package net.machpi.runelite.influxdb.write;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.influxdb.dto.Point;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Value
@Builder
public class Measurement {
    Series series;

    @Builder.Default
    long time = System.currentTimeMillis();

    @Singular
    Map<@NonNull String, @NonNull String> stringValues;

    @Singular
    Map<@NonNull String, @NonNull Number> numericValues;

    // influx accepts Map<String, Object>, where object is String | Number
    @SuppressWarnings("unchecked")
    Optional<Point> toInflux() {
        if (getStringValues().isEmpty() && getNumericValues().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Point.measurement(series.getMeasurement())
                .tag(series.getTags())
                .time(time, TimeUnit.MILLISECONDS)
                .fields((Map) getStringValues())
                .fields((Map) getNumericValues())
                .build());
    }
}
