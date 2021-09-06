package net.machpi.runelite.influxdb;

import net.runelite.api.Client;

import java.util.Locale;

// Derived starting from https://github.com/RuneStar/cs2-scripts/blob/9a49f4b47b96c15a05f333d61cdfd469f7a197df/scripts/%5Bproc%2Csummary_sidepanel_draw%5D.cs2
public enum CombatAchievement {
    EASY(12885, 3981),
    MEDIUM(12886, 3982),
    HARD(12887, 3983),
    ELITE(12888, 3984),
    MASTER(12889, 3985),
    GRANDMASTER(12890, 3986);
    private final int varbitId;
    private final int enumId;

    CombatAchievement(int varbitId, int enumId) {
        this.varbitId = varbitId;
        this.enumId = enumId;
    }

    public int getCompleted(Client client) {
        return client.getVarbitValue(varbitId);
    }

    public int getTotal(Client client) {
        return client.getEnum(enumId).size();
    }
}
