package net.machpi.runelite.influxdb.write;

import net.machpi.runelite.influxdb.InfluxDbPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InfluxDbPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InfluxDbPlugin.class);
		RuneLite.main(args);
	}
}