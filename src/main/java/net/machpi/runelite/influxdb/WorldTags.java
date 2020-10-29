package net.machpi.runelite.influxdb;

import net.runelite.api.WorldType;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public final class WorldTags {
    private WorldTags() {
    }

    private static final String TAG_VARIANT = "variant";
    private static final String TAG_LEAGUE = "league";

    public static Map<String, String> tagsForWorld(EnumSet<WorldType> worldTypeData) {
        Variant variant = null;
        League league = null;
        if (worldTypeData.contains(WorldType.LEAGUE)) {
            variant = Variant.LEAGUES;
            league = League.findCurrentLeague();
        } else if (worldTypeData.contains(WorldType.DEADMAN) || worldTypeData.contains(WorldType.DEADMAN_TOURNAMENT)) {
            variant = Variant.DEADMAN;
        } else if (worldTypeData.contains(WorldType.TOURNAMENT)) {
            variant = Variant.TOURNAMENT;
        } else if (worldTypeData.contains(WorldType.LAST_MAN_STANDING)) {
            variant = Variant.LAST_MAN_STANDING;
        }
        Map<String, String> results = new HashMap<>();
        if (variant != null)
            results.put(TAG_VARIANT, variant.name().toLowerCase());
        if (league != null)
            results.put(TAG_LEAGUE, league.name().toLowerCase());
        return results;
    }

    public enum League {
        TRAILBLAZER(Instant.parse("2020-10-28T00:00:00.00Z"), Instant.parse("2021-01-07T00:00:00.00Z")),
        UNKNOWN(null, null);

        private final Instant start;
        private final Instant end;

        League(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        @Nonnull
        public static League findCurrentLeague() {
            Instant now = Instant.now();
            for (League league : League.values()) {
                if ((league.start == null || league.start.isBefore(now)) && (league.end == null || league.end.isAfter(now))) {
                    return league;
                }
            }
            return UNKNOWN;
        }
    }

    public enum Variant {
        LEAGUES,
        DEADMAN,
        TOURNAMENT,
        LAST_MAN_STANDING
    }
}
