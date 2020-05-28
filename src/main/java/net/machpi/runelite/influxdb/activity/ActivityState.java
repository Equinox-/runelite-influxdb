package net.machpi.runelite.influxdb.activity;

import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.InfluxDbConfig;
import net.machpi.runelite.influxdb.MeasurementCreator;
import net.machpi.runelite.influxdb.write.InfluxWriter;
import net.machpi.runelite.influxdb.write.Measurement;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final List<EventWithTime> events = new ArrayList<>();
    private final InfluxDbConfig config;
    private final MeasurementCreator measurer;
    private final InfluxWriter writer;
    private State lastState;

    public ActivityState(final InfluxDbConfig config, final InfluxWriter writer, final MeasurementCreator measurer) {
        this.config = config;
        this.writer = writer;
        this.measurer = measurer;
    }

    /**
     * Reset state.
     */
    public void reset() {
        events.clear();
        lastState = null;
    }

    /**
     * Trigger new state update.
     *
     * @param eventType event type
     */
    public void triggerEvent(final GameEvent eventType) {
        if (!config.writeActivity()) return;

        final Optional<EventWithTime> foundEvent = events.stream().filter(e -> e.type == eventType).findFirst();
        EventWithTime event;

        if (foundEvent.isPresent()) {
            event = foundEvent.get();
        } else {
            event = new EventWithTime(eventType, Instant.now());
            events.add(event);
        }

        event.setUpdated(Instant.now());

        if (event.getType().isShouldClear()) {
            events.removeIf(e -> e.getType() != eventType && e.getType().isShouldClear());
        }

        events.sort((a, b) -> ComparisonChain.start()
                .compare(b.getType().getPriority(), a.getType().getPriority())
                .compare(b.getUpdated(), a.getUpdated())
                .result());

        String location = null;
        String skill = null;
        String locationType = null;

        for (EventWithTime e : events) {
            // get the highest priority skill from our event list
            if (skill == null) {
                skill = e.getType().getSkill();
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

        lastState = new State(skill, location, locationType);
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
        final EventWithTime eventWithTime = events.get(0);

        events.removeIf(event -> event.getType().isShouldTimeout() && now.isAfter(event.getUpdated().plus(activityTimeout)));

        // if we've been in the menu for more than the timeout, stop sending updates.
        if (GameEvent.IN_MENU.getLocation().equals(eventWithTime.getType().getLocation()) && now.isAfter(eventWithTime.getStart().plus(activityTimeout))) {
            lastState = null;
        }
    }

    public void write() {
        if (!config.writeActivity() || lastState == null) return;

        Optional<Measurement> m = measurer.createActivityMeasurement(lastState);
        m.ifPresent(writer::submit);
    }
}
