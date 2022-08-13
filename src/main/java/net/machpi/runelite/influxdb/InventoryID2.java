package net.machpi.runelite.influxdb;

public enum InventoryID2 {
    /**
     * Reward from fishing trawler
     */
    FISHING_TRAWLER_REWARD(0),
    /**
     * The trade inventory.
     */
    TRADE(90),
    /**
     * The other trade inventory.
     */
    TRADEOTHER(90 | 0x8000),
    /**
     * Standard player inventory.
     */
    INVENTORY(93),
    /**
     * Equipment inventory.
     */
    EQUIPMENT(94),
    /**
     * Bank inventory.
     */
    BANK(95),
    /**
     * A puzzle box inventory.
     */
    PUZZLE_BOX(140),
    /**
     * Barrows reward chest inventory.
     */
    BARROWS_REWARD(141),
    /**
     * Monkey madness puzzle box inventory.
     */
    MONKEY_MADNESS_PUZZLE_BOX(221),
    /**
     * Drift net fishing reward
     */
    DRIFT_NET_FISHING_REWARD(307),
    /**
     * Kingdom Of Miscellania reward inventory.
     */
    KINGDOM_OF_MISCELLANIA(390),
    /**
     * Chambers of Xeric chest inventory.
     */
    CHAMBERS_OF_XERIC_CHEST(581),
    /**
     * Theater of Blood reward chest inventory (Raids 2)
     */
    THEATRE_OF_BLOOD_CHEST(612),
    /**
     * Inventory representing contents of the collection log
     */
    COLLECTION_LOG(620),
    /**
     * Seed vault located inside the Farming Guild
     */
    SEED_VAULT(626),
    ;

    private final int id;

    InventoryID2(int id) {
        this.id = id;
    }

    /**
     * Gets the raw inventory type ID.
     *
     * @return inventory type
     */
    public int getId() {
        return id;
    }
}
