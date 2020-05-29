package net.machpi.runelite.influxdb;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.write.InfluxWriter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ExecutorServiceExceptionLogger;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(
        name = "InfluxDB",
        description = "Saves statistics to InfluxDB",
        tags = {"experience", "levels", "stats"}
)
@Slf4j
public class InfluxDbPlugin extends Plugin {
    private ScheduledFuture<?> flushTask;

    @Provides
    InfluxDbConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(InfluxDbConfig.class);
    }

    @Inject
    private ConfigManager configManager;

    @Inject
    private InfluxWriter writer;

    @Inject
    private InfluxDbConfig config;

    @Inject
    private Client client;

    @Inject
    private MeasurementCreator measurer;

    /**
     * Don't use a shared executor because we don't want to block any game threads.
     */
    private final ScheduledExecutorService executor = new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (!config.writeXp()) {
            return;
        }

        writer.submit(measurer.createXpMeasurement(statChanged.getSkill()));
        if (statChanged.getSkill() != Skill.OVERALL) {
            writer.submit(measurer.createXpMeasurement(Skill.OVERALL));
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            return;
        measureInitialState();
    }

    private void measureInitialState() {
        if (config.writeXp()) {
            for (Skill s : Skill.values()) {
                writer.submit(measurer.createXpMeasurement(s));
            }
        }
        if (config.writeKillCount()) {
            String group = MeasurementCreator.KILL_COUNT_CFG_PREFIX + client.getUsername().toLowerCase() + ".";
            for (String groupAndKey : configManager.getConfigurationKeys(group)) {
                String boss = groupAndKey.substring(group.length());
                measurer.createKillCountMeasurement(boss).ifPresent(writer::submit);
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!config.writeBankValue()) return;
        ItemContainer container = event.getItemContainer();
        if (container == null)
            return;
        Item[] items = container.getItems();
        if (items == null)
            return;

        InventoryID id = null;
        for (InventoryID val : InventoryID.values()) {
            if (val.getId() == event.getContainerId()) {
                id = val;
                break;
            }
        }
        if (id != InventoryID.BANK && id != InventoryID.SEED_VAULT)
            return;
        if (writer.isBlocked(measurer.createItemSeries(id, MeasurementCreator.InvValueType.HA)))
            return;
        measurer.createItemMeasurements(id, items).forEach(writer::submit);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (config.writeSelfLoc())
            writer.submit(measurer.createSelfLocMeasurement());
        if (config.writeSelfMeta())
            writer.submit(measurer.createSelfMeasurement());
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged changed) {
        if (InfluxDbConfig.GROUP.equals(changed.getGroup())) {
            failureBackoff = 0;
            if (InfluxDbConfig.WRITE_INTERVAL.equals(changed.getKey())) {
                rescheduleFlush();
            }
        }
        observeKillCountConfig(changed.getGroup(), changed.getKey(), changed.getNewValue());
    }

    private void observeKillCountConfig(String group, String key, String value) {
        // Piggyback on the chat commands plugin to record kill count to avoid
        // duplicating the complex logic to keep up to date on kill counts
        if (!config.writeKillCount())
            return;
        String user = client.getUsername().toLowerCase();
        if (!group.equals(MeasurementCreator.KILL_COUNT_CFG_PREFIX + user)
                && !group.equals(MeasurementCreator.PERSONAL_BEST_CFG_PREFIX + user))
            return;
        measurer.createKillCountMeasurement(key).ifPresent(writer::submit);
    }

    private int failures = 0;
    private int failureBackoff = 0;
    public void flush() {
        if (failureBackoff > 0) {
            failureBackoff--;
            return;
        }
        try {
            writer.flush();
            failures = 0;
        } catch (RuntimeException ex) {
            failures++;
            log.error("Failed to write to influxDB " + failures + " times", ex);
            failureBackoff = Math.min(32, failures * failures);
        }
    }

    private synchronized void rescheduleFlush() {
        unscheduleFlush();
        this.flushTask = executor.scheduleWithFixedDelay(this::flush, config.writeIntervalSeconds(), config.writeIntervalSeconds(), TimeUnit.SECONDS);
    }

    private synchronized void unscheduleFlush() {
        if (flushTask != null) {
            flushTask.cancel(false);
            flushTask = null;
        }
    }

    @Override
    protected void startUp() {
        rescheduleFlush();
        if (client.getGameState() == GameState.LOGGED_IN) {
            measureInitialState();
        }
    }

    @Override
    protected void shutDown() {
        unscheduleFlush();
    }
}
