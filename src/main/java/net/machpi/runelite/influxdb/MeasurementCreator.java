package net.machpi.runelite.influxdb;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import net.machpi.runelite.influxdb.activity.ActivityState;
import net.machpi.runelite.influxdb.activity.GameEvent;
import net.machpi.runelite.influxdb.write.Measurement;
import net.machpi.runelite.influxdb.write.Series;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Experience;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Singleton
public class MeasurementCreator {
    public static final String SERIES_INVENTORY = "rs_inventory";
    public static final String SERIES_SKILL = "rs_skill";
    public static final String SERIES_SELF = "rs_self";
    public static final String SERIES_ACHIEVEMENTS = "rs_achivements";
    public static final String SERIES_KILL_COUNT = "rs_killcount";
    public static final String SERIES_SELF_LOC = "rs_self_loc";
    public static final String SERIES_ACTIVITY = "rs_activity";
    public static final String SERIES_LOOT = "rs_loot";
    public static final String SERIES_SKILLING_ITEMS = "rs_skilling_items";
    public static final String SELF_KEY_X = "locX";
    public static final String SELF_KEY_Y = "locY";
    public static final Set<String> SELF_POS_KEYS = ImmutableSet.of(SELF_KEY_X, SELF_KEY_Y);

    private final Client client;
    private final ItemManager itemManager;
    private final ConfigManager configManager;

    @Inject
    public MeasurementCreator(Client client, ItemManager itemManager, ConfigManager configManager) {
        this.client = client;
        this.itemManager = itemManager;
        this.configManager = configManager;
    }

    public boolean isInLastManStanding() {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }
        final int regionId = WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()).getRegionID();
        return GameEvent.MG_LAST_MAN_STANDING.equals(GameEvent.fromRegion(regionId));
    }

    private Series.SeriesBuilder createSeries() {
        Series.SeriesBuilder builder = Series.builder();
        builder.tag("user", client.getUsername());
        builder.tags(WorldTags.tagsForWorld(client.getWorldType()));
        builder.tag("profile", configManager.getRSProfileKey());
        builder.tag("worldType", RuneScapeProfileType.getCurrent(client).name());
        return builder;
    }

    public Series createXpSeries(Skill skill) {
        return createSeries().measurement(SERIES_SKILL).tag("skill", skill.name()).build();
    }

    public Optional<Measurement> createXpMeasurement(Skill skill) {
        long xp = skill == Skill.OVERALL ? client.getOverallExperience() : client.getSkillExperience(skill);
        if (xp == 0) {
            return Optional.empty();
        }
        int virtualLevel;
        int realLevel;
        if (skill == Skill.OVERALL) {
            virtualLevel = Arrays.stream(Skill.values())
                    .filter(x -> x != Skill.OVERALL)
                    .mapToInt(x -> Experience.getLevelForXp(client.getSkillExperience(x)))
                    .sum();
            realLevel = client.getTotalLevel();
        } else {
            virtualLevel = Experience.getLevelForXp((int) xp);
            realLevel = client.getRealSkillLevel(skill);
        }
        return Optional.of(Measurement.builder()
                .series(createXpSeries(skill))
                .numericValue("xp", xp)
                .numericValue("realLevel", realLevel)
                .numericValue("virtualLevel", virtualLevel)
                .build());
    }

    public Series createItemSeries(InventoryID inventory, InvValueType type) {
        return createSeries().measurement(SERIES_INVENTORY)
                .tag("inventory", inventory.name())
                .tag("type", type.name())
                .build();
    }

    private static String itemToKey(ItemComposition composition) {
        return composition.getName() + "@" + composition.getId();
    }

    private static void addToMap(Map<String, Long> map, String key, long value) {
        map.compute(key, (_key, oldValue) -> (oldValue != null ? oldValue : 0) + value);
    }

    public Stream<Measurement> createItemMeasurements(InventoryID inventoryID, Item[] items) {
        Map<String, Long> gePrice = new HashMap<>(items.length / 2);
        Map<String, Long> haPrice = new HashMap<>(items.length / 2);
        Map<String, Long> count = new HashMap<>(items.length / 2);

        long totalGe = 0, totalAlch = 0;
        long otherGe = 0, otherAlch = 0;
        for (Item item : items) {
            long ge, alch;
            if (item.getId() < 0 || item.getQuantity() <= 0 || item.getId() == ItemID.BANK_FILLER)
                continue;
            int canonId = itemManager.canonicalize(item.getId());
            ItemComposition data = itemManager.getItemComposition(canonId);
            switch (canonId) {
                case ItemID.COINS_995:
                    ge = item.getQuantity();
                    alch = item.getQuantity();
                    break;
                case ItemID.PLATINUM_TOKEN:
                    ge = item.getQuantity() * 1000L;
                    alch = item.getQuantity() * 1000L;
                    break;
                default:
                    final long storePrice = data.getPrice();
                    final long alchPrice = (long) (storePrice * Constants.HIGH_ALCHEMY_MULTIPLIER);
                    alch = alchPrice * item.getQuantity();
                    ge = (long) itemManager.getItemPrice(canonId) * item.getQuantity();
                    break;
            }
            totalGe += ge;
            totalAlch += alch;
            boolean highValue = ge > THRESHOLD || alch > THRESHOLD;
            if (highValue) {
                String key = itemToKey(data);
                addToMap(gePrice, key, ge);
                addToMap(haPrice, key, alch);
                addToMap(count, key, item.getQuantity());
            } else {
                otherGe += ge;
                otherAlch += alch;
            }
        }


        return Stream.of(Measurement.builder().series(createItemSeries(inventoryID, InvValueType.GE))
                        .numericValues(gePrice)
                        .numericValue("total", totalGe)
                        .numericValue("other", otherGe)
                        .build(),
                Measurement.builder().series(createItemSeries(inventoryID, InvValueType.HA))
                        .numericValues(haPrice)
                        .numericValue("total", totalAlch)
                        .numericValue("other", otherAlch)
                        .build(),
                Measurement.builder().series(createItemSeries(inventoryID, InvValueType.COUNT))
                        .numericValues(count)
                        .build());
    }

    public Series createSelfLocSeries() {
        return createSeries().measurement(SERIES_SELF_LOC).build();
    }

    public Measurement createSelfLocMeasurement() {
        Player local = client.getLocalPlayer();
        WorldPoint location = WorldPoint.fromLocalInstance(client, local.getLocalLocation());
        return Measurement.builder()
                .series(createSelfLocSeries())
                .numericValue(SELF_KEY_X, location.getX())
                .numericValue(SELF_KEY_Y, location.getY())
                .numericValue("plane", location.getPlane())
                .numericValue("instance", client.isInInstancedRegion() ? 1 : 0)
                .build();
    }

    public Series createSelfSeries() {
        return createSeries().measurement(SERIES_SELF).build();
    }

    private static final int VARBIT_LEAGUE_TASKS = 10046;
    private static final int VARP_LEAGUE_POINTS = 2614;
    private static final int VARP_COLLECTION_LOG_ACHIEVED = 2943;
    private static final int VARP_COLLECTION_LOG_TOTAL = 2944;

    public Measurement createSelfMeasurement() {
        Player local = client.getLocalPlayer();
        Measurement.MeasurementBuilder builder = Measurement.builder()
                .series(createSelfSeries())
                .numericValue("combat", Experience.getCombatLevelPrecise(
                        client.getRealSkillLevel(Skill.ATTACK),
                        client.getRealSkillLevel(Skill.STRENGTH),
                        client.getRealSkillLevel(Skill.DEFENCE),
                        client.getRealSkillLevel(Skill.HITPOINTS),
                        client.getRealSkillLevel(Skill.MAGIC),
                        client.getRealSkillLevel(Skill.RANGED),
                        client.getRealSkillLevel(Skill.PRAYER)
                ))
                .numericValue("questPoints", client.getVar(VarPlayer.QUEST_POINTS))
                .numericValue("skulled", local.getSkullIcon() != null ? 1 : 0)
                .stringValue("name", MoreObjects.firstNonNull(local.getName(), "none"))
                .stringValue("overhead", local.getOverheadIcon() != null ? local.getOverheadIcon().name() : "NONE");
        if (client.getWorldType().contains(WorldType.SEASONAL) && WorldTags.League.findCurrentLeague() != null) {
            int tasksComplete = client.getVarbitValue(VARBIT_LEAGUE_TASKS);
            int leaguePoints = client.getVarpValue(VARP_LEAGUE_POINTS);
            builder.numericValue("leagueTasksComplete", tasksComplete)
                    .numericValue("leaguePoints", leaguePoints);
        }
        loadProfileConfig("slayer", "initialAmount", Integer::parseInt)
                .ifPresent(amount -> builder.numericValue("slayerTaskAmount", amount));
        loadProfileConfig("slayer", "amount", Integer::parseInt)
                .ifPresent(amount -> builder.numericValue("slayerTaskRemaining", amount));
        loadProfileConfig("slayer", "taskName", MeasurementCreator::toTitleCase)
                .ifPresent(taskName -> {
                    String taskLoc = loadProfileConfig("slayer", "taskLocation", MeasurementCreator::toTitleCase)
                            .orElse("Any Location");
                    builder.stringValue("slayerTaskName", taskName)
                            .stringValue("slayerTaskLoc", taskLoc);
                });
        loadProfileConfig("slayer", "points", Integer::parseInt)
                .ifPresent(amount -> builder.numericValue("slayerPoints", amount));
        loadProfileConfig("slayer", "streak", Integer::parseInt)
                .ifPresent(streak -> builder.numericValue("slayerTaskStreak", streak));
        int collectionLogAchieved = client.getVarpValue(VARP_COLLECTION_LOG_ACHIEVED);

        // Don't record if these are zero -- the varbits may not be initialized
        if (collectionLogAchieved > 0) {
            builder.numericValue("collectionLogAchieved", collectionLogAchieved);
        }
        int achievementDiaryAchieved = Arrays.stream(AchievementDiary.values())
                .mapToInt(d -> d.getTotal(client))
                .sum();
        if (achievementDiaryAchieved > 0) {
            builder.numericValue("achievementDiaryAchieved", achievementDiaryAchieved);
        }
        int combatAchievementsAchieved = Arrays.stream(CombatAchievement.values())
                .mapToInt(d -> d.getCompleted(client))
                .sum();
        if (combatAchievementsAchieved > 0) {
            builder.numericValue("combatAchievementsAchieved", combatAchievementsAchieved);
        }
        return builder.build();
    }

    public Series createAchievementSeries(String achievementGroup, String achievementTier) {
        return createSeries()
                .measurement(SERIES_ACHIEVEMENTS)
                .tag("group", achievementGroup)
                .tag("tier", achievementTier)
                .build();
    }

    private Optional<Measurement> createAchievementMeasurementInternal(String group, String tier, int count, int total) {
        if (count == 0 || total == 0) {
            return Optional.empty();
        }
        Measurement.MeasurementBuilder builder = Measurement.builder()
                .series(createAchievementSeries(group, tier))
                .numericValue("count", count);
        if (total > 0) {
            builder.numericValue("total", total);
        }
        return Optional.of(builder.build());
    }

    public void createAchievementMeasurements(Consumer<Measurement> target) {
        for (AchievementDiary diary : AchievementDiary.values()) {
            createAchievementMeasurementInternal(diary.name(), "EASY", diary.getEasy(client), -1)
                    .ifPresent(target);
            createAchievementMeasurementInternal(diary.name(), "MEDIUM", diary.getMedium(client), -1)
                    .ifPresent(target);
            createAchievementMeasurementInternal(diary.name(), "HARD", diary.getHard(client), -1)
                    .ifPresent(target);
            createAchievementMeasurementInternal(diary.name(), "ELITE", diary.getElite(client), -1)
                    .ifPresent(target);
        }
        for (CombatAchievement tier : CombatAchievement.values()) {
            createAchievementMeasurementInternal("COMBAT", tier.name(), tier.getCompleted(client), tier.getTotal(client))
                    .ifPresent(target);
        }
    }

    private static final Pattern SENTENCE_CASE_PATTERN = Pattern.compile("( |^)([a-z])");

    private static String toTitleCase(String str) {
        Matcher matcher = SENTENCE_CASE_PATTERN.matcher(str);
        if (!matcher.find()) {
            return str;
        }
        StringBuffer dest = new StringBuffer(str.length());
        do {
            matcher.appendReplacement(dest, matcher.group(1) + matcher.group(2).toUpperCase());
        } while (matcher.find());
        matcher.appendTail(dest);
        return dest.toString();
    }

    private <T> Optional<T> loadProfileConfig(String group, String key, Function<String, T> parser) {
        String value = configManager.getRSProfileConfiguration(group, key);
        if (Strings.isNullOrEmpty(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.apply(value));
        } catch (RuntimeException err) {
            return Optional.empty();
        }
    }

    public Series createKillCountSeries(String boss) {
        return createSeries().measurement(SERIES_KILL_COUNT)
                .tag("boss", boss).build();
    }

    static final String KILL_COUNT_CFG_GROUP = "killcount";
    static final String PERSONAL_BEST_CFG_GROUP = "personalbest";

    public Optional<Measurement> createKillCountMeasurement(String bossMixed) {
        // Piggyback off of chat commands plugin
        String boss = bossMixed.toLowerCase();
        Integer killCount = configManager.getRSProfileConfiguration(KILL_COUNT_CFG_GROUP, boss, int.class);
        if (killCount == null)
            return Optional.empty();
        Double personalBest = configManager.getRSProfileConfiguration(PERSONAL_BEST_CFG_GROUP, boss, double.class);
        Measurement.MeasurementBuilder measurement = Measurement.builder()
                .series(createKillCountSeries(boss))
                .numericValue("kc", killCount);
        if (personalBest != null) {
            measurement.numericValue("pb", personalBest.intValue());
            measurement.numericValue("pb_float", personalBest.floatValue());
        }
        return Optional.of(measurement.build());
    }

    enum InvValueType {
        GE,
        HA,
        COUNT
    }

    private static final int THRESHOLD = 50_000;

    public Optional<Series> createActivitySeries() {
        Series series = createSeries().measurement(SERIES_ACTIVITY).build();
        if (Strings.isNullOrEmpty(series.getTags().getOrDefault("user", null)))
            return Optional.empty();
        return Optional.of(series);
    }

    public Optional<Measurement> createActivityMeasurement(ActivityState.State lastState) {
        Optional<Series> series = createActivitySeries();
        if (!series.isPresent()) {
            return Optional.empty();
        }
        Measurement.MeasurementBuilder mb = Measurement.builder().series(series.get());
        if (!Strings.isNullOrEmpty(lastState.getSkill())) {
            mb.stringValue("skill", lastState.getSkill());
        }
        if (!Strings.isNullOrEmpty(lastState.getLocationType())) {
            mb.stringValue("type", lastState.getLocationType());
        }
        if (!Strings.isNullOrEmpty(lastState.getLocation())) {
            mb.stringValue("location", lastState.getLocation());
        }
        Measurement measure = mb.build();
        if (measure.getStringValues().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(measure);
    }

    public Series createLootSeries(LootRecordType type, String source, int combatLevel) {
        return createSeries()
                .measurement(SERIES_LOOT)
                .tag("type", type.name())
                .tag("source", source)
                .tag("combat", Integer.toString(combatLevel))
                .build();
    }

    public Optional<Measurement> createLootMeasurement(LootReceived event) {
        Measurement.MeasurementBuilder measurement = Measurement.builder().series(createLootSeries(event.getType(), event.getName(), event.getCombatLevel()));
        Optional<WorldPoint> worldPoint = event.getItems().stream()
                .filter(Objects::nonNull)
                .map(stack -> WorldPoint.fromLocalInstance(client, stack.getLocation()))
                .findAny();
        if (!worldPoint.isPresent()) {
            return Optional.empty();
        }

        WorldPoint location = worldPoint.get();
        measurement.numericValue(SELF_KEY_X, location.getX())
                .numericValue(SELF_KEY_Y, location.getY())
                .numericValue("plane", location.getPlane())
                .numericValue("killcount", 1);

        Multiset<String> counts = HashMultiset.create(event.getItems().size());
        for (ItemStack stack : event.getItems()) {
            if (stack.getQuantity() <= 0) {
                continue;
            }
            int canonId = itemManager.canonicalize(stack.getId());
            ItemComposition data = itemManager.getItemComposition(canonId);
            counts.add(itemToKey(data), stack.getQuantity());
        }
        if (counts.isEmpty()) {
            return Optional.empty();
        }
        for (Multiset.Entry<String> count : counts.entrySet()) {
            measurement.numericValue(count.getElement(), count.getCount());
        }
        return Optional.of(measurement.build());
    }

    public Series createSkillingItemSeries(Skill skill, ItemComposition item) {
        return createSeries()
                .measurement(SERIES_SKILLING_ITEMS)
                .tag("skill", skill.name())
                .tag("item", itemToKey(item))
                .build();
    }

    public Measurement createSkillingItemMeasurement(Skill skill, int xp,
                                                     float weightedXp,
                                                     ItemComposition item,
                                                     long count) {
        return Measurement.builder().series(createSkillingItemSeries(skill, item))
                .numericValue("xp", xp)
                .numericValue("weightedXp", weightedXp)
                .numericValue("itemCount", count)
                .numericValue("actionCount", 1)
                .build();
    }
}
