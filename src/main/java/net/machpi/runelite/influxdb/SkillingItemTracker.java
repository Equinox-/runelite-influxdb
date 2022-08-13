package net.machpi.runelite.influxdb;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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

    private Multiset<Integer> prevInventoryItems = HashMultiset.create();
    private Multiset<Integer> currInventoryItems = HashMultiset.create();

    private int trackingDataForTick = -1;
    private final Multiset<Integer> currTickAddedItems = HashMultiset.create();
    private final Multiset<Skill> currTickXp = HashMultiset.create();

    @Inject
    public SkillingItemTracker(Client client, ItemManager itemManager, MeasurementCreator measurementCreator,
                               InfluxWriter writer) {
        this.client = client;
        this.itemManager = itemManager;
        this.measurementCreator = measurementCreator;
        this.writer = writer;
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
            currInventoryItems.add(id, curr.getQuantity());
        }

        // Figure out what was added:
        for (Multiset.Entry<Integer> curr : currInventoryItems.entrySet()) {
            int prevCount = prevInventoryItems.count(curr.getElement());
            int newItems = curr.getCount() - prevCount;
            if (newItems > 0) {
                onItemAdded(curr.getElement(), newItems);
            }
        }

        Multiset<Integer> tmp = prevInventoryItems;
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

    private void onItemAdded(int id, int count) {
        flushIfNeeded();
        currTickAddedItems.add(id, count);
    }

    public void flushIfNeeded() {
        int tick = client.getTickCount();
        if (trackingDataForTick == tick) {
            return;
        }
        if (currTickXp.entrySet().size() == 1) {
            Multiset.Entry<Skill> skill = Iterables.getOnlyElement(currTickXp.entrySet());
            float weightedXp = skill.getCount() / (float) currTickAddedItems.size();
            for (Multiset.Entry<Integer> item : currTickAddedItems.entrySet()) {
                ItemComposition composition = itemManager.getItemComposition(item.getElement());
                writer.submit(measurementCreator.createSkillingItemMeasurement(
                        skill.getElement(), skill.getCount(), weightedXp, composition, item.getCount()));
            }
        }
        currTickAddedItems.clear();
        currTickXp.clear();
        trackingDataForTick = tick;
    }
}
