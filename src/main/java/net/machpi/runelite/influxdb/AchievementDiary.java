package net.machpi.runelite.influxdb;

import net.runelite.api.Client;

// Derived starting from https://github.com/RuneStar/cs2-scripts/blob/9a49f4b47b96c15a05f333d61cdfd469f7a197df/scripts/%5Bproc%2Csummary_sidepanel_draw%5D.cs2
public enum AchievementDiary {
    KARAMJA(2423, 6288, 6289, 6290),
    ARDOUGNE(6291, 6292, 6293, 6294),
    FALADOR(6299, 6300, 6301, 6302),
    FREMENNIK(6303, 6304, 6305, 6306),
    KANDARIN(6307, 6308, 6309, 6310),
    DESERT(6295, 6296, 6297, 6298),
    LUMBRIDGE(6311, 6312, 6313, 6314),
    MORYTANIA(6315, 6316, 6317, 6318),
    VARROCK(6319, 6320, 6321, 6322),
    WILDERNESS(6323, 6324, 6325, 6326),
    WESTERN(6327, 6328, 6329, 6330),
    KOUREND(7933, 7934, 7935, 7936);

    private final int varbitEasy;
    private final int varbitMedium;
    private final int varbitHard;
    private final int varbitElite;

    AchievementDiary(int varbitEasy, int varbitMedium, int varbitHard, int varbitElite) {
        this.varbitEasy = varbitEasy;
        this.varbitMedium = varbitMedium;
        this.varbitHard = varbitHard;
        this.varbitElite = varbitElite;
    }

    public int getEasy(Client client) {
        return client.getVarbitValue(varbitEasy);
    }

    public int getMedium(Client client) {
        return client.getVarbitValue(varbitMedium);
    }

    public int getHard(Client client) {
        return client.getVarbitValue(varbitHard);
    }

    public int getElite(Client client) {
        return client.getVarbitValue(varbitElite);
    }

    public int getTotal(Client client) {
        return getEasy(client) + getMedium(client) + getHard(client) + getElite(client);
    }
}
