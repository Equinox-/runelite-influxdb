package net.machpi.runelite.influxdb;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.activity.ActivityState;
import net.machpi.runelite.influxdb.activity.GameEvent;
import net.machpi.runelite.influxdb.write.InfluxWriter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.task.Schedule;
import net.runelite.client.util.ExecutorServiceExceptionLogger;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(
        name = "InfluxDB",
        description = "Saves statistics to InfluxDB",
        tags = {"experience", "levels", "stats", "activity", "tracker"}
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

    @Inject
    private ActivityState activityState;

    /**
     * Don't use a shared executor because we don't want to block any game threads.
     */
    private final ScheduledExecutorService executor = new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
    private final EnumMap<Skill, Integer> previousStatXp = new EnumMap<>(Skill.class);
    private GameState prevGameState;

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (measurer.isInLastManStanding())
            return;
        if (statChanged.getXp() == 0 || client.getGameState() != GameState.LOGGED_IN)
            return;
        final Integer previous = previousStatXp.put(statChanged.getSkill(), statChanged.getXp());
        if (previous == null || previous == statChanged.getXp())
            return;
        previousStatXp.put(statChanged.getSkill(), statChanged.getXp());

        if (config.writeXp()) {
            measurer.createXpMeasurement(statChanged.getSkill()).ifPresent(writer::submit);
            if (statChanged.getSkill() != Skill.OVERALL) {
                measurer.createXpMeasurement(Skill.OVERALL).ifPresent(writer::submit);
            }
        }

        if (config.writeActivity()) {
            final GameEvent gameEvent = GameEvent.fromSkill(statChanged.getSkill());
            if (gameEvent != null) {
                activityState.triggerEvent(gameEvent);
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState prev = prevGameState;
        prevGameState = event.getGameState();

        switch (event.getGameState()) {
            case LOGIN_SCREEN:
                checkForGameStateUpdate();
                break;
            case LOGGING_IN:
                previousStatXp.clear();
                break;
            case LOGGED_IN:
                if (prev == GameState.LOGGING_IN) {
                    measureInitialState();
                    checkForGameStateUpdate();
                }
                break;
        }

        checkForAreaUpdate();
    }

    private void measureInitialState() {
        if (config.writeXp() && !measurer.isInLastManStanding()) {
            for (Skill s : Skill.values()) {
                measurer.createXpMeasurement(s).ifPresent(writer::submit);
            }
        }
        if (config.writeKillCount()) {
            String group = MeasurementCreator.KILL_COUNT_CFG_GROUP + client.getUsername().toLowerCase() + ".";
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
        Item[] items = container.getItems();
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
        observeKillCountConfig(changed.getGroup(), changed.getKey());
    }

    private void observeKillCountConfig(String group, String key) {
        // Piggyback on the chat commands plugin to record kill count to avoid
        // duplicating the complex logic to keep up to date on kill counts
        if (!config.writeKillCount())
            return;
        if (!group.equals(MeasurementCreator.KILL_COUNT_CFG_GROUP)
                && !group.equals(MeasurementCreator.PERSONAL_BEST_CFG_GROUP))
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

    @Subscribe
    public void onLootReceived(LootReceived event) {
        if (config.writeLoot()) {
            measurer.createLootMeasurement(event).ifPresent(writer::submit);
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        final GameEvent gameEvent = GameEvent.fromVarbit(client);
        if (gameEvent != null) {
            activityState.triggerEvent(gameEvent);
        }
    }

    private void checkForAreaUpdate() {
        if (client.getLocalPlayer() == null) {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        final int regionId = WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()).getRegionID();
        if (regionId == 0) {
            return;
        }

        final EnumSet<WorldType> worldType = client.getWorldType();
        GameEvent gameEvent = GameEvent.fromRegion(regionId);

        Widget wildyWidget = client.getWidget(WidgetInfo.PVP_WILDERNESS_LEVEL);
        if (GameEvent.MG_NIGHTMARE_ZONE == gameEvent && localPlayer.getWorldLocation().getPlane() == 0) {
            // NMZ uses the same region ID as KBD. KBD is always on plane 0 and NMZ is always above plane 0
            gameEvent = GameEvent.BOSS_KING_BLACK_DRAGON;
        } else if (wildyWidget != null && !wildyWidget.isHidden() && !"".equals(wildyWidget.getText())) {
            gameEvent = GameEvent.WILDERNESS;
        } else if (worldType.contains(WorldType.DEADMAN)) {
            gameEvent = GameEvent.PLAYING_DEADMAN;
        } else if (WorldType.isPvpWorld(worldType)) {
            gameEvent = GameEvent.PLAYING_PVP;
        } else if (gameEvent == null) {
            gameEvent = GameEvent.IN_GAME;
        }

        activityState.triggerEvent(gameEvent);
    }

    private void checkForGameStateUpdate() {
        // Game state update does also full reset of state
        activityState.reset();
        activityState.triggerEvent(client.getGameState() == GameState.LOGGED_IN
                ? GameEvent.IN_GAME
                : GameEvent.IN_MENU);
    }

    /**
     * send an activity heartbeat once every 50 seconds.
     */
    @Schedule(period = 50, unit = ChronoUnit.SECONDS)
    public void updateActivity() {
        activityState.checkForTimeout();
        activityState.measure().ifPresent(writer::submit);
    }

    @Override
    protected void startUp() {
        rescheduleFlush();
        if (client.getGameState() == GameState.LOGGED_IN) {
            measureInitialState();
        }

        checkForGameStateUpdate();
        checkForAreaUpdate();
    }

    @Override
    protected void shutDown() {
        updateActivity(); // get the final activity before shutting down
        flush();
        unscheduleFlush();
    }
}
