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
            description = "URL of the influx DB api to write to"
    )
    String getServerUrl();

    @ConfigItem(
            keyName = "serverDatabase",
            name = "Database",
            description = "Database to write to"
    )
    String getDatabase();

    @ConfigItem(
            keyName = "serverUsername",
            name = "Server Username",
            description = "Username to use for authentiation"
    )
    default String getServerUsername() {
        return "";
    }

    @ConfigItem(
            keyName = "serverPassword",
            name = "Server Password",
            description = "Password to use for authentiation"
    )
    default String getServerPassword() {
        return "";
    }

    @ConfigItem(
            keyName = "serverRetentionPolicy",
            name = "Server Retention Policy",
            description = "Retention policy to use for storing data"
    )
    default String getServerRetentionPolicy() {
        return "autogen";
    }

    @ConfigItem(
            keyName = "writeXp",
            name = "Submit Experience",
            description = "Submit experience amount"
    )
    default boolean writeXp() {
        return true;
    }

    @ConfigItem(
            keyName = "writeBankValue",
            name = "Submit Bank Value",
            description = "Submit bank and seed vault items"
    )
    default boolean writeBankValue() {
        return true;
    }

    @ConfigItem(
            keyName = "writeSelfLoc",
            name = "Submit Player Location",
            description = "Submit player location"
    )
    default boolean writeSelfLoc() {
        return false;
    }

    @ConfigItem(
            keyName = "writeSelfMeta",
            name = "Submit Player Metadata",
            description = "Submit player combat level, quest points, and other minor stats"
    )
    default boolean writeSelfMeta() {
        return true;
    }

    @ConfigItem(
            keyName = "writeKillCount",
            name = "Submit Kill Count",
            description = "Submits boss kill counts (requires Chat Commands plugin)"
    )
    default boolean writeKillCount() {
        return true;
    }

    @ConfigItem(
            keyName = WRITE_INTERVAL,
            name = "Recording interval",
            description = "Minimum interval between measurements"
    )
    @Units(Units.SECONDS)
    @Range(min = 5, max = 5 * 60)
    default int writeIntervalSeconds() {
        return 15;
    }
}
