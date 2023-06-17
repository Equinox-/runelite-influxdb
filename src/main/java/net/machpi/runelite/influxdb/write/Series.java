package net.machpi.runelite.influxdb.write;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class Series {
    String measurement;

    @Singular
    Map<@NonNull String, @NonNull String> tags;
}
