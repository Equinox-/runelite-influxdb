package net.machpi.runelite.influxdb.activity;

import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.InfluxDbConfig;
import net.machpi.runelite.influxdb.MeasurementCreator;
import net.machpi.runelite.influxdb.write.InfluxWriter;
import net.machpi.runelite.influxdb.write.Measurement;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Optional;
import java.util.TreeSet;

@Slf4j
public class ActivityState {

    @Data
    private static class EventWithTime {
        private final GameEvent type;
        private final Instant start;
        private Instant updated;
    }

    @Data
    public static class State {
        private final String skill;
        private final String location;
        private final String locationType;
    }

    private final InfluxDbConfig config;
    private final MeasurementCreator measurer;

    private final EnumMap<GameEvent, EventWithTime> latestEvents = new EnumMap<>(GameEvent.class);
    private final TreeSet<EventWithTime> events;


    @Inject
    public ActivityState(final InfluxDbConfig config, final MeasurementCreator measurer) {
        this.config = config;
        this.measurer = measurer;
        this.events = new TreeSet<>((a, b) -> ComparisonChain.start()
                .compare(b.getType().getPriority(), a.getType().getPriority())
                .compare(b.getUpdated(), a.getUpdated())
                .result()
        );
    }

    /**
     * Reset state.
     */
    public void reset() {
        events.clear();
        latestEvents.clear();
    }

    /**
     * Trigger new state update.
     *
     * @param eventType event type
     */
    public void triggerEvent(final GameEvent eventType) {
        if (!config.writeActivity()) return;

        EventWithTime event = latestEvents.get(eventType);
        if (event == null) {
            event = new EventWithTime(eventType, Instant.now());
            latestEvents.put(eventType, event);
        } else {
            events.remove(event);
        }
        event.setUpdated(Instant.now());
        events.add(event);

        if (event.getType().isShouldClear()) {
            events.removeIf(e -> {
                boolean remove = e.getType() != eventType && e.getType().isShouldClear();
                if (remove) latestEvents.remove(e.getType());
                return remove;
            });
        }
    }

    public State getState() {
        if (events.isEmpty() || !config.writeActivity()) return null;

        final Duration activityTimeout = Duration.ofMinutes(config.activityTimeout());
        final Instant now = Instant.now();
        final EventWithTime eventWithTime = events.first();

        // if we've been in the menu for more than the timeout, stop sending updates.
        if (GameEvent.IN_MENU.getLocation().equals(eventWithTime.getType().getLocation()) && now.isAfter(eventWithTime.getStart().plus(activityTimeout))) {
            return null;
        }

        String location = null;
        String skill = null;
        String locationType = null;

        for (EventWithTime e : events) {
            // get the highest priority skill from our event list
            if (skill == null && e.getType().getSkill() != null) {
                skill = e.getType().getSkill().name();
            }

            // get the highest priority or latest area from our event list
            if (location == null) {
                location = e.getType().getLocation();
            }

            if (locationType == null && e.getType().getLocationType() != null) {
                locationType = e.getType().getLocationType().name();
            }

            if (skill != null && location != null && locationType != null) {
                break;
            }
        }

        return new State(skill, location, locationType);
    }

    /**
     * Check for current state timeout and act upon it.
     */
    public void checkForTimeout() {
        if (events.isEmpty() || !config.writeActivity()) {
            return;
        }

        final Duration activityTimeout = Duration.ofMinutes(config.activityTimeout());
        final Instant now = Instant.now();

        events.removeIf(event -> {
            boolean remove = event.getType().isShouldTimeout() && now.isAfter(event.getUpdated().plus(activityTimeout));
            if (remove) latestEvents.remove(event.getType());
            return remove;
        });
    }

    public Optional<Measurement> measure() {
        if (!config.writeActivity()) return Optional.empty();

        State state = getState();
        if (state == null)
            return Optional.empty();

        return measurer.createActivityMeasurement(state);
    }
}
