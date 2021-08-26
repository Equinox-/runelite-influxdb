package net.machpi.runelite.influxdb;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jogamp.common.util.IntLongHashMap;
import lombok.extern.slf4j.Slf4j;
import net.machpi.runelite.influxdb.write.InfluxWriter;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

import java.util.Set;

@Slf4j
@Singleton
public final class SkillingItemTracker {
    private static final Set<Integer> IGNORED_ITEMS = ImmutableSet.of(ItemID.COINS_995,
            // ignore waterskins since they can be used on the same tick when doing activities in the desert
            ItemID.WATERSKIN0, ItemID.WATERSKIN1, ItemID.WATERSKIN2, ItemID.WATERSKIN3, ItemID.WATERSKIN4,
            // ignore cooking utensils
            ItemID.PIE_DISH, ItemID.CAKE_TIN, ItemID.POT, ItemID.BOWL, ItemID.SERVERY_DISH,
            // ignore not-interesting items received while doing birdhouse runs
            ItemID.CLOCKWORK, ItemID.FEATHER);
    private static final Set<Skill> VALID_SKILLS = ImmutableSet.of(Skill.RUNECRAFT, Skill.CRAFTING, Skill.MINING,
            Skill.SMITHING, Skill.FIREMAKING, Skill.COOKING, Skill.WOODCUTTING, Skill.HERBLORE, Skill.HUNTER,
            Skill.FLETCHING, Skill.FARMING, Skill.FISHING);

    private final Client client;
    private final ItemManager itemManager;
    private final MeasurementCreator measurementCreator;
    private final InfluxWriter writer;

    private IntLongHashMap prevInventoryItems = new IntLongHashMap();
    private IntLongHashMap currInventoryItems = new IntLongHashMap();

    private int trackingDataForTick = -1;
    private final IntLongHashMap currTickAddedItems = new IntLongHashMap();
    private final Multiset<Skill> currTickXp = HashMultiset.create();

    @Inject
    public SkillingItemTracker(Client client, ItemManager itemManager, MeasurementCreator measurementCreator,
                               InfluxWriter writer) {
        this.client = client;
        this.itemManager = itemManager;
        this.measurementCreator = measurementCreator;
        this.writer = writer;
        this.prevInventoryItems.setKeyNotFoundValue(0);
        this.currInventoryItems.setKeyNotFoundValue(0);
        this.currTickAddedItems.setKeyNotFoundValue(0);
    }

    public void onInventoryChanges(ItemContainer container) {
        for (int i = 0; i < container.size(); i++) {
            Item curr = container.getItem(i);
            if (curr == null) {
                continue;
            }
            int id = itemManager.canonicalize(curr.getId());
            if (IGNORED_ITEMS.contains(id)) {
                continue;
            }
            currInventoryItems.put(id, currInventoryItems.get(id) + curr.getQuantity());
        }

        // Figure out what was added:
        for (IntLongHashMap.Entry curr : currInventoryItems) {
            long prevCount = prevInventoryItems.get(curr.key);
            long newItems = curr.value - prevCount;
            if (newItems > 0) {
                onItemAdded(curr.key, newItems);
            }
        }

        IntLongHashMap tmp = prevInventoryItems;
        prevInventoryItems = currInventoryItems;
        currInventoryItems = tmp;
        tmp.clear();
    }

    public void onXpGained(Skill skill, int xp) {
        if (VALID_SKILLS.contains(skill)) {
            flushIfNeeded();
            currTickXp.add(skill, xp);
        }
    }

    private void onItemAdded(int id, long count) {
        flushIfNeeded();
        currTickAddedItems.put(id, currTickAddedItems.get(id) + count);
    }

    public void flushIfNeeded() {
        int tick = client.getTickCount();
        if (trackingDataForTick == tick) {
            return;
        }
        if (currTickXp.entrySet().size() == 1) {
            Multiset.Entry<Skill> skill = Iterables.getOnlyElement(currTickXp.entrySet());
            float weightedXp = skill.getCount() / (float) currTickAddedItems.size();
            for (IntLongHashMap.Entry item : currTickAddedItems) {
                ItemComposition composition = itemManager.getItemComposition(item.key);
                writer.submit(measurementCreator.createSkillingItemMeasurement(
                        skill.getElement(), skill.getCount(), weightedXp, composition, item.value));
            }
        }
        currTickAddedItems.clear();
        currTickXp.clear();
        trackingDataForTick = tick;
    }
}
