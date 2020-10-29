package net.machpi.runelite.influxdb.write;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.InfluxDbConfig;
import net.machpi.runelite.influxdb.MeasurementCreator;
import net.runelite.api.coords.WorldPoint;
import net.runelite.http.api.loottracker.LootRecord;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class InfluxWriter {
    private final InfluxDbConfig config;
    private final ConcurrentMap<Series, Writer> writers = new ConcurrentHashMap<>();

    @Inject
    public InfluxWriter(InfluxDbConfig config) {
        this.config = config;
    }

    public void submit(Measurement m) {
        writer(m.getSeries()).submit(m);
    }

    public boolean isBlocked(Series s) {
        return writer(s).isBlocked();
    }

    private InfluxDB cachedServer = null;
    private String serverUrl, serverUser, serverPass;

    private synchronized Optional<InfluxDB> getInflux() {
        if (StringUtils.isEmpty(config.getDatabase())) {
            return Optional.empty();
        }

        String newServerUrl = config.getServerUrl();
        String newServerUser = config.getServerUsername();
        String newServerPass = config.getServerPassword();

        if (!Objects.equals(newServerUrl, serverUrl)
                || !Objects.equals(newServerUser, serverUser)
                || !Objects.equals(newServerPass, serverPass)
                || cachedServer == null) {
            if (cachedServer != null) {
                cachedServer.close();
                cachedServer = null;
                serverUrl = null;
                serverUser = null;
                serverPass = null;
            }

            if (!StringUtils.isEmpty(serverUrl)) {
                if (StringUtils.isEmpty(serverPass) || StringUtils.isEmpty(serverUser)) {
                    cachedServer = InfluxDBFactory.connect(serverUrl);
                } else {
                    cachedServer = InfluxDBFactory.connect(serverUrl, serverUser, serverPass);
                }
            }
            serverUrl = newServerUrl;
            serverUser = newServerUser;
            serverPass = newServerPass;
        }
        return Optional.ofNullable(cachedServer);
    }

    private Writer writer(Series s) {
        return writers.computeIfAbsent(s, series -> {
            if (series.getMeasurement().equals(MeasurementCreator.SERIES_SELF_LOC)) {
                return new Writer(new ThrottledWriter(), SELF_DEDUPE);
            } else if (series.getMeasurement().equals(MeasurementCreator.SERIES_ACTIVITY)
                    || series.getMeasurement().equals(MeasurementCreator.SERIES_LOOT)) {
                return new Writer(new AlwaysWriter(), (a, b) -> true);
            }
            return new Writer(new ThrottledWriter(), FULL_DEDUPE);
        });
    }

    public void flush() {
        BatchPoints.Builder batch = BatchPoints.database(config.getDatabase())
                .retentionPolicy(config.getServerRetentionPolicy())
                .consistency(InfluxDB.ConsistencyLevel.ONE);
        writers.forEach((k, v) -> v.flush(batch));

        getInflux().ifPresent(influxDB -> {
            BatchPoints built = batch.build();
            if (!built.getPoints().isEmpty()) {
                influxDB.write(built);
                log.debug("Writing {}", built.lineProtocol());
            }
        });
    }

    private static class Writer {
        private final TerminalOp terminal;
        private final FilterOp[] filters;

        private Writer(TerminalOp terminal, FilterOp... filters) {
            this.terminal = terminal;
            this.filters = filters;
        }

        boolean isBlocked() {
            return terminal.isBlocked();
        }

        synchronized void submit(Measurement m) {
            Measurement prev = terminal.getLastWritten();
            for (FilterOp e : filters) {
                if (!e.shouldWrite(prev, m)) {
                    return;
                }
            }
            terminal.submit(m);
        }

        synchronized void flush(BatchPoints.Builder output) {
            terminal.flush(output);
        }
    }

    private interface TerminalOp {
        Measurement getLastWritten();

        boolean isBlocked();

        void submit(Measurement m);

        void flush(BatchPoints.Builder output);
    }

    private interface FilterOp {
        boolean shouldWrite(Measurement lastWritten, Measurement measurement);
    }

    private static class ThrottledWriter implements TerminalOp {
        @Getter
        private volatile Measurement lastWritten;
        private final AtomicReference<Measurement> waitingForWrite = new AtomicReference<>();

        @Override
        public boolean isBlocked() {
            return waitingForWrite.get() != null;
        }

        @Override
        public void submit(Measurement m) {
            waitingForWrite.set(m);
        }

        @Override
        public void flush(BatchPoints.Builder output) {
            Measurement flush = waitingForWrite.getAndSet(null);
            lastWritten = flush;
            if (flush != null)
                flush.toInflux().ifPresent(output::point);
        }
    }

    private static class AlwaysWriter implements TerminalOp {
        private final ArrayDeque<Measurement> queued = new ArrayDeque<>();

        @Override
        public synchronized Measurement getLastWritten() {
            return queued.isEmpty() ? null : queued.peekLast();
        }

        @Override
        public boolean isBlocked() {
            return false;
        }

        @Override
        public synchronized void submit(Measurement m) {
            queued.add(m);
        }

        @Override
        public synchronized void flush(BatchPoints.Builder output) {
            while (!queued.isEmpty()) {
                queued.removeFirst().toInflux().ifPresent(output::point);
            }
        }
    }

    private static final FilterOp FULL_DEDUPE = (prev, b) -> prev == null || !prev.getNumericValues().equals(b.getNumericValues())
            || !prev.getStringValues().equals(b.getStringValues());

    private static final FilterOp SELF_DEDUPE = (prev, curr) -> {
        if (prev == null)
            return true;
        if (!prev.getStringValues().equals(curr.getStringValues()))
            return true;
        for (String posKey : MeasurementCreator.SELF_POS_KEYS) {
            Number p = prev.getNumericValues().get(posKey);
            Number c = curr.getNumericValues().get(posKey);
            if (p == null || c == null)
                return true;
            if (Math.abs(p.doubleValue() - c.doubleValue()) > 5)
                return true;
        }
        return !Objects.equals(
                Maps.filterKeys(prev.getNumericValues(), x -> !MeasurementCreator.SELF_POS_KEYS.contains(x)),
                Maps.filterKeys(curr.getNumericValues(), x -> !MeasurementCreator.SELF_POS_KEYS.contains(x)));
    };
}
