package net.machpi.runelite.influxdb;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(InfluxDbConfig.GROUP)
public interface InfluxDbConfig extends Config {
    String GROUP = "influxdb";
    String WRITE_INTERVAL = "writeInterval";

    @ConfigItem(
            keyName = "serverUrl",
            name = "Server URL",
            description = "URL of the influx DB api to write to",
            position = 0
    )
    String getServerUrl();

    @ConfigItem(
            keyName = "serverDatabase",
            name = "Database",
            description = "Database to write to",
            position = 1
    )
    String getDatabase();

    @ConfigItem(
            keyName = "serverUsername",
            name = "Server Username",
            description = "Username to use for authentication",
            position = 2
    )
    default String getServerUsername() {
        return "";
    }

    @ConfigItem(
            keyName = "serverPassword",
            name = "Server Password",
            description = "Password to use for authentication",
            position = 3,
            secret = true
    )
    default String getServerPassword() {
        return "";
    }

    @ConfigItem(
            keyName = "serverRetentionPolicy",
            name = "Server Retention Policy",
            description = "Retention policy to use for storing data",
            position = 4
    )
    default String getServerRetentionPolicy() {
        return "autogen";
    }

    @ConfigItem(
            keyName = "writeXp",
            name = "Submit Experience",
            description = "Submit experience amount",
            position = 5
    )
    default boolean writeXp() {
        return true;
    }

    @ConfigItem(
            keyName = "writeBankValue",
            name = "Submit Bank Value",
            description = "Submit bank and seed vault items",
            position = 6
    )
    default boolean writeBankValue() {
        return true;
    }

    @ConfigItem(
            keyName = "writeSelfLoc",
            name = "Submit Player Location",
            description = "Submit player location",
            position = 7
    )
    default boolean writeSelfLoc() {
        return false;
    }

    @ConfigItem(
            keyName = "writeSelfMeta",
            name = "Submit Player Metadata",
            description = "Submit player combat level, quest points, and other minor stats",
            position = 8
    )
    default boolean writeSelfMeta() {
        return true;
    }

    @ConfigItem(
            keyName = "writeKillCount",
            name = "Submit Kill Count",
            description = "Submits boss kill counts (requires Chat Commands plugin)",
            position = 9
    )
    default boolean writeKillCount() {
        return true;
    }

    @ConfigItem(
            keyName = "writeActivity",
            name = "Submit Activity",
            description = "Submit player activity such as location names, skill training, minigame, raid, or boss",
            position = 10
    )
    default boolean writeActivity() {
        return true;
    }

    @ConfigItem(
            keyName = "writeLoot",
            name = "Submit Loot",
            description = "Submit loot events provided by the Loot Tracker plugin",
            position = 11
    )
    default boolean writeLoot() {
        return false;
    }

    @ConfigItem(
            keyName = WRITE_INTERVAL,
            name = "Recording interval",
            description = "Minimum interval between measurements",
            position = 12
    )
    @Units(Units.SECONDS)
    @Range(min = 5, max = 5 * 60)
    default int writeIntervalSeconds() {
        return 15;
    }

    @ConfigItem(
            keyName = "activityTimeout",
            name = "Activity timeout",
            description = "Configures after how long of not updating activity will be reset (in minutes)",
            position = 13
    )
    @Units(Units.MINUTES)
    default int activityTimeout() {
        return 5;
    }
}
