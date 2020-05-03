package net.machpi.runelite.influxdb.write;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.InfluxDbConfig;
import net.machpi.runelite.influxdb.MeasurementCreator;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import javax.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
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
        String newServerUrl = config.getServerUrl();
        String newServerUser = config.getServerUsername();
        String newServerPass = config.getServerPassword();

        if (!Objects.equals(newServerUrl, serverUrl) || !Objects.equals(newServerUser, serverUser) || !Objects.equals(newServerPass, serverPass)) {
            serverUrl = newServerUrl;
            serverUser = newServerUser;
            serverPass = newServerPass;

            if (cachedServer != null) {
                cachedServer.flush();
                cachedServer.close();
                cachedServer = null;
            }

            if (!StringUtils.isEmpty(serverUrl)) {
                if (StringUtils.isEmpty(serverPass) || StringUtils.isEmpty(serverUser)) {
                    cachedServer = InfluxDBFactory.connect(serverUrl);
                } else {
                    cachedServer = InfluxDBFactory.connect(serverUrl, serverUser, serverPass);
                }
                cachedServer.enableBatch(BatchOptions.DEFAULTS);
            }
        }
        return Optional.ofNullable(cachedServer).map(influx -> {
            String db = config.getDatabase();
            if (StringUtils.isEmpty(db)) {
                return null;
            }
            return influx.setDatabase(db);
        });
    }

    private Writer writer(Series s) {
        return writers.computeIfAbsent(s, series -> {
            if (series.getMeasurement().equals(MeasurementCreator.SERIES_SELF_LOC)) {
                return new Writer(new ThrottledWriter(), SELF_DEDUPE);
            }
            return new Writer(new ThrottledWriter(), FULL_DEDUPE);
        });
    }

    public void flush() {
        writers.forEach((k, v) -> v.flush());
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

        synchronized void flush() {
            terminal.flush();
        }
    }

    private interface TerminalOp {
        Measurement getLastWritten();

        boolean isBlocked();

        void submit(Measurement m);

        void flush();
    }

    private interface FilterOp {
        boolean shouldWrite(Measurement lastWritten, Measurement measurement);
    }

    private class ThrottledWriter implements TerminalOp {
        @Getter
        private volatile Measurement lastWritten;
        private volatile Measurement waitingForWrite;

        @Override
        public boolean isBlocked() {
            return waitingForWrite != null;
        }

        @Override
        public void submit(Measurement m) {
            waitingForWrite = m;
        }

        @Override
        public void flush() {
            Measurement flush = waitingForWrite;
            waitingForWrite = null;
            lastWritten = flush;

            if (flush == null) return;

            getInflux().ifPresent(influxDB -> {
                String pt = flush.toInflux().lineProtocol();
                log.debug("Writing {}", pt);
                influxDB.write(pt);
            });
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
