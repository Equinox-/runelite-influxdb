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

import javax.inject.Inject;
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
    private InfluxWriter writer;

    @Inject
    private InfluxDbConfig config;

    @Inject
    private Client client;

    @Inject
    private MeasurementCreator measurer;

    @Inject
    private ScheduledExecutorService executor;

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
        if (event.getGameState() == GameState.LOGGED_IN && config.writeXp()) {
            for (Skill s : Skill.values()) {
                writer.submit(measurer.createXpMeasurement(s));
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
                unscheduleFlush();
                scheduleFlush();
            }
        }
        if (!config.writeKillCount())
            return;
        // Piggyback on the chat commands plugin to record kill count to avoid
        // duplicating the complex logic to keep up to date on kill counts
        if (!changed.getGroup().startsWith("killcount.") || changed.getNewValue() == null)
            return;
        if (!changed.getGroup().equals("killcount." + client.getUsername().toLowerCase()))
            return;
        try {
            String boss = changed.getKey();
            int kc = Integer.parseInt(changed.getNewValue());
            writer.submit(measurer.createKillCountMeasurement(boss, kc));
        } catch (NumberFormatException ex) {
            log.debug("Failed to parse KC for boss {} value {}",
                    changed.getKey(),
                    changed.getNewValue(),
                    ex);
        }
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

    private synchronized void scheduleFlush() {
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
        scheduleFlush();
    }

    @Override
    protected void shutDown() {
        unscheduleFlush();
    }
}
