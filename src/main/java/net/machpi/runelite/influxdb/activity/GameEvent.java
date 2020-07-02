/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2018, PandahRS <https://github.com/PandahRS>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.machpi.runelite.influxdb.activity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


enum LocationType {
    BOSSES,
    CITIES,
    DUNGEONS,
    MINIGAMES,
    RAIDS,
    POI,
    ;
}

@AllArgsConstructor
@Getter
public enum GameEvent {
    IN_GAME("In Game", -3),
    IN_MENU("In Menu", -3),
    PLAYING_DEADMAN("Playing Deadman Mode", -3),
    PLAYING_PVP("Playing in a PVP world", -3),
    WILDERNESS("Wilderness", -2),

    TRAINING_ATTACK(Skill.ATTACK),
    TRAINING_DEFENCE(Skill.DEFENCE),
    TRAINING_STRENGTH(Skill.STRENGTH),
    TRAINING_HITPOINTS(Skill.HITPOINTS, -1),
    TRAINING_SLAYER(Skill.SLAYER, 1),
    TRAINING_RANGED(Skill.RANGED),
    TRAINING_MAGIC(Skill.MAGIC),
    TRAINING_PRAYER(Skill.PRAYER),
    TRAINING_COOKING(Skill.COOKING),
    TRAINING_WOODCUTTING(Skill.WOODCUTTING),
    TRAINING_FLETCHING(Skill.FLETCHING),
    TRAINING_FISHING(Skill.FISHING, 1),
    TRAINING_FIREMAKING(Skill.FIREMAKING),
    TRAINING_CRAFTING(Skill.CRAFTING),
    TRAINING_SMITHING(Skill.SMITHING),
    TRAINING_MINING(Skill.MINING),
    TRAINING_HERBLORE(Skill.HERBLORE),
    TRAINING_AGILITY(Skill.AGILITY),
    TRAINING_THIEVING(Skill.THIEVING),
    TRAINING_FARMING(Skill.FARMING),
    TRAINING_RUNECRAFT(Skill.RUNECRAFT),
    TRAINING_HUNTER(Skill.HUNTER),
    TRAINING_CONSTRUCTION(Skill.CONSTRUCTION),

    // Bosses
    BOSS_ABYSSAL_SIRE("Abyssal Sire", LocationType.BOSSES, 11851, 11850, 12363, 12362),
    BOSS_CERBERUS("Cerberus", LocationType.BOSSES, 4883, 5140, 5395),
    BOSS_COMMANDER_ZILYANA("Commander Zilyana", LocationType.BOSSES, 11602),
    BOSS_DKS("Dagannoth Kings", LocationType.BOSSES, 11588, 11589),
    BOSS_GENERAL_GRAARDOR("General Graardor", LocationType.BOSSES, 11347),
    BOSS_GIANT_MOLE("Giant Mole", LocationType.BOSSES, 6993, 6992),
    BOSS_GROTESQUE_GUARDIANS("Grotesque Guardians", LocationType.BOSSES, 6727),
    BOSS_HYDRA("Alchemical Hydra", LocationType.BOSSES, 5536),
    BOSS_KING_BLACK_DRAGON("King Black Dragon", LocationType.BOSSES), // no region id.. custom logic for kbd/nmz
    BOSS_KQ("Kalphite Queen", LocationType.BOSSES, 13972),
    BOSS_KRAKEN("Kraken", LocationType.BOSSES, 9116),
    BOSS_KREEARRA("Kree'arra", LocationType.BOSSES, 11346),
    BOSS_KRIL_TSUTSAROTH("K'ril Tsutsaroth", LocationType.BOSSES, 11603),
    BOSS_SKOTIZO("Skotizo", LocationType.BOSSES, 6810),
    BOSS_SMOKE_DEVIL("Thermonuclear smoke devil", LocationType.BOSSES, 9363, 9619),
    BOSS_VORKATH("Vorkath", LocationType.BOSSES, 9023),
    BOSS_WINTERTODT("Wintertodt", LocationType.BOSSES, 6462),
    BOSS_ZALCANO("Zalcano", LocationType.BOSSES, 13250),
    BOSS_ZULRAH("Zulrah", LocationType.BOSSES, 9007, 9008),
    BOSS_NIGHTMARE("Nightmare of Ashihama", LocationType.BOSSES, 15515),

    // Cities
    CITY_AL_KHARID("Al Kharid", LocationType.CITIES, 13105, 13106),
    CITY_APE_ATOLL("Ape Atoll", LocationType.CITIES, 10795, 11051, 10974, 11050),
    CITY_ARCEUUS_HOUSE("Arceuus", LocationType.CITIES, 6459, 6715, 6458, 6714),
    CITY_ARDOUGNE("Ardougne", LocationType.CITIES, 10548, 10547, 10292, 10291, 10036, 10035, 9780, 9779),
    CITY_BARBARIAN_VILLAGE("Barbarian Village", LocationType.CITIES, 12341),
    CITY_BANDIT_CAMP("Bandit Camp", LocationType.CITIES, 12591),
    CITY_BEDABIN_CAMP("Bedabin Camp", LocationType.CITIES, 12590),
    CITY_BRIMHAVEN("Brimhaven", LocationType.CITIES, 11057, 11058),
    CITY_BURGH_DE_ROTT("Burgh de Rott", LocationType.CITIES, 13874, 13873, 14130, 14129),
    CITY_BURTHORPE("Burthorpe", LocationType.CITIES, 11319, 11575),
    CITY_CANIFIS("Canifis", LocationType.CITIES, 13878),
    CITY_CATHERBY("Catherby", LocationType.CITIES, 11317, 11318, 11061),
    CITY_CORSAIR_CAVE("Corsair Cove", LocationType.CITIES, 10028, 10284),
    CITY_DARKMEYER("Darkmeyer", LocationType.CITIES, 14388),
    CITY_DORGESH_KAAN("Dorgesh-Kaan", LocationType.CITIES, 10835, 10834),
    CITY_DRAYNOR("Draynor", LocationType.CITIES, 12338),
    CITY_EDGEVILLE("Edgeville", LocationType.CITIES, 12342),
    CITY_ENTRANA("Entrana", LocationType.CITIES, 11060, 11316),
    CITY_FALADOR("Falador", LocationType.CITIES, 11828, 11572, 11571, 11827, 12084),
    CITY_GOBLIN_VILLAGE("Goblin Village", LocationType.CITIES, 11830),
    CITY_GUTANOTH("Gu'Tanoth", LocationType.CITIES, 10031),
    CITY_GWENITH("Gwenith", LocationType.CITIES, 8501, 8757, 9013),
    CITY_HOSIDIUS_HOUSE("Hosidius", LocationType.CITIES, 6713, 6712, 6455, 6711, 6710, 6965, 6966, 7222, 7223, 6967),
    CITY_JATISZO("Jatizso", LocationType.CITIES, 9531),
    CITY_JIGGIG("Jiggig", LocationType.CITIES, 9775),
    CITY_KARAMJA("Karamja", LocationType.CITIES, 11569, 11568, 11567, 11566, 11313, 11312, 11311),
    CITY_KELDAGRIM("Keldagrim", LocationType.CITIES, 11423, 11422, 11679, 11678),
    CITY_LLETYA("Lletya", LocationType.CITIES, 9265),
    CITY_LOVAKENGJ_HOUSE("Lovakengj", LocationType.CITIES, 5692, 5948, 5691, 5947, 6203, 6202, 5690, 5946),
    CITY_LUMBRIDGE("Lumbridge", LocationType.CITIES, 12850),
    CITY_LUNAR_ISLE("Lunar Isle", LocationType.CITIES, 8253, 8252, 8509, 8508),
    CITY_MEIYERDITCH("Meiyerditch", LocationType.CITIES, 14132, 14387, 14386, 14385),
    CITY_MISCELLANIA("Miscellania", LocationType.CITIES, 10044, 10300),
    CITY_MOS_LE_HARMLESS("Mos Le'Harmless", LocationType.CITIES, 14638),
    CITY_MORTTON("Mort'ton", LocationType.CITIES, 13875),
    CITY_MOR_UI_REK("Mor UI Rek", LocationType.CITIES, 9808, 9807, 10064, 10063),
    CITY_MOUNT_KARUULM("Mount Karuulm", LocationType.CITIES, 5179, 4923, 5180),
    CITY_MOUNT_QUIDAMORTEM("Mount Quidamortem", LocationType.CITIES, 4919),
    CITY_NARDAH("Nardah", LocationType.CITIES, 13613),
    CITY_NEITIZNOT("Neitiznot", LocationType.CITIES, 9275),
    CITY_PISCATORIS("Piscatoris", LocationType.CITIES, 9273),
    CITY_POLLNIVNEACH("Pollnivneach", LocationType.CITIES, 13358),
    CITY_PORT_KHAZARD("Port Khazard", LocationType.CITIES, 10545),
    CITY_PORT_PHASMATYS("Port Phasmatys", LocationType.CITIES, 14646),
    CITY_PORT_SARIM("Port Sarim", LocationType.CITIES, 12082),
    CITY_PISCARILIUS_HOUSE("Port Piscarilius", LocationType.CITIES, 6971, 7227, 6970, 7226),
    CITY_PRIFDDINAS("Prifddinas", LocationType.CITIES, 12894, 12895, 13150, 13151),
    CITY_RELLEKKA("Rellekka", LocationType.CITIES, 10553),
    CITY_RIMMINGTON("Rimmington", LocationType.CITIES, 11826, 11570),
    CITY_SEERS_VILLAGE("Seers' Village", LocationType.CITIES, 10806),
    CITY_SHAYZIEN_HOUSE("Shayzien", LocationType.CITIES, 5944, 5943, 6200, 6199, 5688),
    CITY_SHILO_VILLAGE("Shilo Village", LocationType.CITIES, 11310),
    CITY_SOPHANEM("Sophanem", LocationType.CITIES, 13099),
    CITY_TAI_BWO_WANNAI("Tai Bwo Wannai", LocationType.CITIES, 11056, 11055),
    CITY_TAVERLEY("Taverley", LocationType.CITIES, 11574, 11573),
    CITY_TREE_GNOME_STRONGHOLD("Tree Gnome Stronghold", LocationType.CITIES, 9782, 9781),
    CITY_TREE_GNOME_VILLAGE("Tree Gnome Village", LocationType.CITIES, 10033),
    CITY_TROLL_STRONGHOLD("Troll Stronghold", LocationType.CITIES, 11321),
    CITY_TYRAS_CAMP("Tyras Camp", LocationType.CITIES, 8753, 8752),
    CITY_UZER("Uzer", LocationType.CITIES, 13872),
    CITY_VARROCK("Varrock", LocationType.CITIES, 12596, 12597, 12598, 12852, 12853, 12854, 13108, 13109, 13110),
    CITY_WITCHHAVEN("Witchaven", LocationType.CITIES, 10803),
    CITY_WOODCUTTING_GUILD("Woodcutting Guild", LocationType.CITIES, 6454, 6198, 6298),
    CITY_YANILLE("Yanille", LocationType.CITIES, 10288, 10032),
    CITY_ZANARIS("Zanaris", LocationType.CITIES, 9285, 9541, 9540, 9797),
    CITY_ZULANDRA("Zul-Andra", LocationType.CITIES, 8751),

    // Dungeons
    DUNGEON_ABANDONED_MINE("Abandoned Mine", LocationType.DUNGEONS, 13718, 11079, 11078, 11077, 10823, 10822, 10821),
    DUNGEON_AH_ZA_RHOON("Ah Za Rhoon", LocationType.DUNGEONS, 11666),
    DUNGEON_ANCIENT_CAVERN("Ancient Cavern", LocationType.DUNGEONS, 6483, 6995),
    DUNGEON_APE_ATOLL("Ape Atoll Dungeon", LocationType.DUNGEONS, 11150, 10894),
    DUNGEON_ARDY_SEWERS("Ardougne Sewers", LocationType.DUNGEONS, 10136),
    DUNGEON_ASGARNIAN_ICE_CAVES("Asgarnian Ice Caves", LocationType.DUNGEONS, 12181),
    DUNGEON_BRIMHAVEN("Brimhaven Dungeon", LocationType.DUNGEONS, 10901, 10900, 10899, 10645, 10644, 10643),
    DUNGEON_BRINE_RAT_CAVERN("Brine Rat Cavern", LocationType.DUNGEONS, 10910),
    DUNGEON_CATACOMBS_OF_KOUREND("Catacombs of Kourend", LocationType.DUNGEONS, 6557, 6556, 6813, 6812),
    DUNGEON_CHASM_OF_FIRE("Chasm of Fire", LocationType.DUNGEONS, 5789),
    DUNGEON_CLOCK_TOWER("Clock Tower Basement", LocationType.DUNGEONS, 10390),
    DUNGEON_CORSAIR_COVE("Corsair Cove Dungeon", LocationType.DUNGEONS, 8076, 8332),
    DUNGEON_CRABCLAW_CAVES("Crabclaw Caves", LocationType.DUNGEONS, 6553, 6809),
    DUNGEON_DIGSITE("Digsite Dungeon", LocationType.DUNGEONS, 13465),
    DUNGEON_DORGESHKAAN("Dorgesh-Kaan South Dungeon", LocationType.DUNGEONS, 10833),
    DUNGEON_DORGESHUUN_MINES("Dorgeshuun Mines", LocationType.DUNGEONS, 12950, 13206),
    DUNGEON_DRAYNOR_SEWERS("Draynor Sewers", LocationType.DUNGEONS, 12439, 12438),
    DUNGEON_DWARVEN_MINES("Dwarven Mines", LocationType.DUNGEONS, 12185, 12184, 12183),
    DUNGEON_EAGLES_PEAK("Eagles' Peak Dungeon", LocationType.DUNGEONS, 8013),
    DUNGEON_EDGEVILLE("Edgeville Dungeon", LocationType.DUNGEONS, 12441, 12442, 12443, 12698),
    DUNGEON_ELEMENTAL_WORKSHOP("Elemental Workshop", LocationType.DUNGEONS, 10906, 7760),
    DUNGEON_ENAKHRAS_TEMPLE("Enakhra's Temple", LocationType.DUNGEONS, 12423),
    DUNGEON_ENTRANA("Entrana Dungeon", LocationType.DUNGEONS, 11416),
    DUNGEON_EVIL_CHICKENS_LAIR("Evil Chicken's Lair", LocationType.DUNGEONS, 9796),
    DUNGEON_EXPERIMENT_CAVE("Experiment Cave", LocationType.DUNGEONS, 14235, 13979),
    DUNGEON_FREMENNIK_SLAYER("Fremennik Slayer Dungeon", LocationType.DUNGEONS, 10908, 11164),
    DUNGEON_GOBLIN_CAVE("Goblin Cave", LocationType.DUNGEONS, 10393),
    DUNGEON_GRAND_TREE_TUNNELS("Grand Tree Tunnels", LocationType.DUNGEONS, 9882),
    DUNGEON_HAM("H.A.M Dungeon", LocationType.DUNGEONS, 12694, 10321),
    DUNGEON_IORWERTH("Iorwerth Dungeon", LocationType.DUNGEONS, 12737, 12738, 12993, 12994),
    DUNGEON_JATIZSO_MINES("Jatizso Mines", LocationType.DUNGEONS, 9631),
    DUNGEON_JIGGIG_BURIAL_TOMB("Jiggig Burial Tomb", LocationType.DUNGEONS, 9875, 9874),
    DUNGEON_JOGRE("Jogre Dungeon", LocationType.DUNGEONS, 11412),
    DUNGEON_KARAMJA_VOLCANO("Karamja Volcano", LocationType.DUNGEONS, 11413, 11414),
    DUNGEON_KARUULM("Karuulm Slayer Dungeon", LocationType.DUNGEONS, 5280, 5279, 5023, 5535, 5022, 4766, 4510, 4511, 4767, 4768, 4512),
    DUNGEON_KHARAZI("Khazari Dungeon", LocationType.DUNGEONS, 11153),
    DUNGEON_LIGHTHOUSE("Lighthouse", LocationType.DUNGEONS, 10140),
    DUNGEON_LIZARDMAN_CAVES("Lizardman Caves", LocationType.DUNGEONS, 5275),
    DUNGEON_LUMBRIDGE_SWAMP_CAVES("Lumbridge Swamp Caves", LocationType.DUNGEONS, 12693, 12949),
    DUNGEON_LUNAR_ISLE_MINE("Lunar Isle Mine", LocationType.DUNGEONS, 9377),
    DUNGEON_MISCELLANIA("Miscellania Dungeon", LocationType.DUNGEONS, 10144, 10400),
    DUNGEON_MOGRE_CAMP("Mogre Camp", LocationType.DUNGEONS, 11924),
    DUNGEON_MOS_LE_HARMLESS_CAVES("Mos Le'Harmless Caves", LocationType.DUNGEONS, 14994, 14995, 15251),
    DUNGEON_MOUSE_HOLE("Mouse Hole", LocationType.DUNGEONS, 9046),
    DUNGEON_OBSERVATORY("Observatory Dungeon", LocationType.DUNGEONS, 9362),
    DUNGEON_OGRE_ENCLAVE("Ogre Enclave", LocationType.DUNGEONS, 10387),
    DUNGEON_QUIDAMORTEM_CAVE("Quidamortem Cave", LocationType.DUNGEONS, 4763),
    DUNGEON_RASHILIYIAS_TOMB("Rashiliyta's Tomb", LocationType.DUNGEONS, 11668),
    DUNGEON_SARADOMINSHRINE("Saradomin Shrine (Paterdomus)", LocationType.DUNGEONS, 13722),
    DUNGEON_SHADE_CATACOMBS("Shade Catacombs", LocationType.DUNGEONS, 13975),
    DUNGEON_SHAYZIEN_CRYPTS("Shayzien Crypts", LocationType.DUNGEONS, 6043),
    DUNGEON_SMOKE("Smoke Dungeon", LocationType.DUNGEONS, 12946, 13202),
    DUNGEON_SOPHANEM("Sophanem Dungeon", LocationType.DUNGEONS, 13200),
    DUNGEON_STRONGHOLD_SECURITY("Stronghold of Security", LocationType.DUNGEONS, 7505, 8017, 8530, 9297),
    DUNGEON_TARNS_LAIR("Tarn's Lair", LocationType.DUNGEONS, 12616, 12615),
    DUNGEON_TAVERLEY("Taverley Dungeon", LocationType.DUNGEONS, 11673, 11672, 11929, 11928, 11417),
    DUNGEON_TEMPLE_OF_IKOV("Temple of Ikov", LocationType.DUNGEONS, 10649, 10905, 10650),
    DUNGEON_TEMPLE_OF_MARIMBO("Temple of Marimbo", LocationType.DUNGEONS, 11151),
    DUNGEON_THE_WARRENS("The Warrens", LocationType.DUNGEONS, 7070, 7326),
    DUNGEON_TOLNA("Dungeon of Tolna", LocationType.DUNGEONS, 13209),
    DUNGEON_TOWER_OF_LIFE("Tower of Life Basement", LocationType.DUNGEONS, 12100),
    DUNGEON_TRAHAEARN_MINE("Trahaearn Mine", LocationType.DUNGEONS, 13249),
    DUNGEON_TUNNEL_OF_CHAOS("Tunnel of Chaos", LocationType.DUNGEONS, 12625),
    DUNGEON_UNDERGROUND_PASS("Underground Pass", LocationType.DUNGEONS, 9369, 9370),
    DUNGEON_VARROCKSEWERS("Varrock Sewers", LocationType.DUNGEONS, 12954, 13210),
    DUNGEON_WATER_RAVINE("Water Ravine", LocationType.DUNGEONS, 13461),
    DUNGEON_WATERBIRTH("Waterbirth Dungeon", LocationType.DUNGEONS, 9886, 10142, 7492, 7748),
    DUNGEON_WATERFALL("Waterfall Dungeon", LocationType.DUNGEONS, 10394),
    DUNGEON_WHITE_WOLF_MOUNTAIN_CAVES("White Wolf Mountain Caves", LocationType.DUNGEONS, 11418, 11419, 11675),
    DUNGEON_WITCHAVEN_SHRINE("Witchhaven Shrine Dungeon", LocationType.DUNGEONS, 10903),
    DUNGEON_YANILLE_AGILITY("Yanille Agility Dungeon", LocationType.DUNGEONS, 10388),
    DUNGEON_MOTHERLODE_MINE("Motherlode Mine", LocationType.DUNGEONS, 14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193),
    DUNGEON_NIGHTMARE("Nightmare Dungeon", LocationType.DUNGEONS, 14999, 15000, 15001, 15255, 15256, 15257, 15511, 15512, 15513),

    // Minigames
    MG_BARBARIAN_ASSAULT("Barbarian Assault", LocationType.MINIGAMES, 10332),
    MG_BARROWS("Barrows", LocationType.MINIGAMES, 14131, 14231),
    MG_BLAST_FURNACE("Blast Furnace", LocationType.MINIGAMES, 7757),
    MG_BRIMHAVEN_AGILITY_ARENA("Brimhaven Agility Arena", LocationType.MINIGAMES, 11157),
    MG_BURTHORPE_GAMES_ROOM("Burthorpe Games Room", LocationType.MINIGAMES, 8781),
    MG_CASTLE_WARS("Castle Wars", LocationType.MINIGAMES, 9520),
    MG_CLAN_WARS("Clan Wars", LocationType.MINIGAMES, 13135, 13134, 13133, 13131, 13130, 13387, 13386),
    MG_DUEL_ARENA("Duel Arena", LocationType.MINIGAMES, 13362),
    MG_FISHING_TRAWLER("Fishing Trawler", LocationType.MINIGAMES, 7499),
    MG_GAUNTLET("Gauntlet", LocationType.MINIGAMES, 12995),
    MG_INFERNO("The Inferno", LocationType.MINIGAMES, 9043),
    MG_LAST_MAN_STANDING("Last Man Standing", LocationType.MINIGAMES, 13660, 13659, 13658, 13916, 13915, 13914),
    MG_HALLOWED_SEPULCHRE("Hallowed Sepulchre", LocationType.MINIGAMES, 8797, 9051, 9052, 9053, 9054, 9309, 9563, 9565, 9821, 10074, 10075, 10077),
    MG_MAGE_TRAINING_ARENA("Mage Training Arena", LocationType.MINIGAMES, 13462, 13463),
    MG_NIGHTMARE_ZONE("Nightmare Zone", LocationType.MINIGAMES, 9033),
    MG_PEST_CONTROL("Pest Control", LocationType.MINIGAMES, 10536),
    MG_PYRAMID_PLUNDER("Pyramid Plunder", LocationType.MINIGAMES, 7749),
    MG_ROGUES_DEN("Rogues' Den", LocationType.MINIGAMES, 11855, 11854, 12111, 12110),
    MG_SORCERESS_GARDEN("Sorceress's Garden", LocationType.MINIGAMES, 11605),
    MG_TEMPLE_TREKKING("Temple Trekking", LocationType.MINIGAMES, 8014, 8270, 8256, 8782, 9038, 9294, 9550, 9806),
    MG_TITHE_FARM("Tithe Farm", LocationType.MINIGAMES, 6968),
    MG_TROUBLE_BREWING("Trouble Brewing", LocationType.MINIGAMES, 15150),
    MG_TZHAAR_FIGHT_CAVES("Tzhaar Fight Caves", LocationType.MINIGAMES, 9551),
    MG_TZHAAR_FIGHT_PITS("Tzhaar Fight Pits", LocationType.MINIGAMES, 9552),
    MG_VOLCANIC_MINE("Volcanic Mine", LocationType.MINIGAMES, 15263, 15262),

    // Raids
    RAIDS_CHAMBERS_OF_XERIC("Chambers of Xeric", LocationType.RAIDS, Varbits.IN_RAID),
    RAIDS_THEATRE_OF_BLOOD("Theatre of Blood", LocationType.RAIDS, Varbits.THEATRE_OF_BLOOD),

    POI_FISHING_GUILD("Fishing Guild", LocationType.POI, 10293),
    POI_OTTOS_GROTTO("Otto's Grotto", LocationType.POI, 10038),
    POI_PLAYER_OWNED_HOUSE("Player Owned House", LocationType.POI, 7769),
    ;

    private static final Map<Integer, GameEvent> FROM_REGION;
    private static final List<GameEvent> FROM_VARBITS;
    private static final EnumMap<Skill, GameEvent> FROM_SKILL = new EnumMap<Skill, GameEvent>(Skill.class);

    static {
        ImmutableMap.Builder<Integer, GameEvent> regionMapBuilder = new ImmutableMap.Builder<>();
        ImmutableList.Builder<GameEvent> fromVarbitsBuilder = ImmutableList.builder();
        for (GameEvent gameEvent : GameEvent.values()) {
            if (gameEvent.getVarbits() != null) {
                fromVarbitsBuilder.add(gameEvent);
                continue;
            }

            if (gameEvent.getSkill() != null) {
                FROM_SKILL.put(gameEvent.getSkill(), gameEvent);
                continue;
            }

            if (gameEvent.getRegionIds() == null) {
                continue;
            }


            for (int region : gameEvent.getRegionIds()) {
                regionMapBuilder.put(region, gameEvent);
            }
        }
        FROM_REGION = regionMapBuilder.build();
        FROM_VARBITS = fromVarbitsBuilder.build();
    }

    @Nullable
    private String location;

    @Nullable
    private Skill skill;

    private final int priority;
    private boolean shouldClear;
    private boolean shouldTimeout;

    @Nullable
    private LocationType locationType;

    @Nullable
    private Varbits varbits;

    @Nullable
    private int[] regionIds;

    GameEvent(Skill skill) {
        this(skill, 0);
    }

    GameEvent(Skill skill, int priority) {
        this.skill = skill;
        this.priority = priority;
        this.shouldTimeout = true;
    }

    GameEvent(String areaName, LocationType locationType, int... regionIds) {
        this.location = areaName;
        this.priority = -2;
        this.locationType = locationType;
        this.regionIds = regionIds;
        this.shouldClear = true;
    }

    GameEvent(String state, int priority) {
        this.location = state;
        this.priority = priority;
        this.shouldClear = true;
    }

    GameEvent(String areaName, LocationType locationType, Varbits varbits) {
        this.location = areaName;
        this.priority = -2;
        this.locationType = locationType;
        this.varbits = varbits;
        this.shouldClear = true;
    }

    public static GameEvent fromSkill(final Skill skill) {
        return FROM_SKILL.get(skill);
    }

    public static GameEvent fromRegion(final int regionId) {
        return FROM_REGION.get(regionId);
    }

    public static GameEvent fromVarbit(final Client client) {
        for (GameEvent fromVarbit : FROM_VARBITS) {
            if (client.getVar(fromVarbit.getVarbits()) != 0) {
                return fromVarbit;
            }
        }

        return null;
    }
}
