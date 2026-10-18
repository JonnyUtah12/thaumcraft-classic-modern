package com.jonnyutah12.thaumcraftclassic.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jonnyutah12.thaumcraftclassic.config.ConfigOptionType.BOOLEAN;
import static com.jonnyutah12.thaumcraftclassic.config.ConfigOptionType.INTEGER;
import static com.jonnyutah12.thaumcraftclassic.config.ConfigOptionType.STRING;

/**
 * Complete inventory of the original Thaumcraft 4.2.3.5 configuration
 * (thaumcraft/common/config/Config.java, decompiled — 62 options across 8
 * categories, every {@code Configuration.get} call accounted for).
 *
 * <p>Names, types, defaults and comments are transcribed verbatim; the
 * {@code source} field points at the line in the decompiled file. Defaults
 * are the values passed to {@code Configuration.get} — the original
 * re-read every option through syncConfigurable(), so the config-file
 * default, not the Java field initializer, is the effective default
 * (e.g. general.allow_cheat_sheet: field initializer true, config default
 * false — the config default wins and is what is recorded here).</p>
 *
 * <p>Nine options are marked not-ported: they exist only to assign numeric
 * registry ids (biome/enchantment/entity/dimension), a concept the target
 * platform replaced with registry names. They stay in this inventory and in
 * docs/config-reference.md so the parity table remains complete; the
 * reasons are recorded per option.</p>
 *
 * <p>Category order mirrors the original generated Thaumcraft.cfg: the
 * built-in "general" category first, then custom categories in the order
 * the original registered their comments (Config.java L173-182). Within a
 * category, options appear in the original {@code config.get} call order.</p>
 */
public final class TcConfigCatalog {

    public static final String NOT_PORTED_BIOME_ID =
            "Numeric biome ids do not exist on NeoForge 1.21.1; biomes are registry-keyed. The biome weight options are ported.";
    public static final String NOT_PORTED_DIMENSION_ID =
            "Numeric dimension ids do not exist on NeoForge 1.21.1; dimensions are registry-keyed.";
    public static final String NOT_PORTED_ENCHANTMENT_ID =
            "Numeric enchantment ids do not exist on NeoForge 1.21.1; enchantments are registry-keyed.";
    public static final String NOT_PORTED_ENTITY_ID =
            "Numeric entity ids do not exist on NeoForge 1.21.1; entity types are registry-keyed.";

    /** Category comments, verbatim from Config.addCustomCategoryComment (L173-182). */
    public static final Map<String, String> CATEGORY_COMMENTS = map(
            "Enchantments", "Custom enchantments",
            "Monster_Spawning", "Will these mobs spawn",
            "Research", "Various research related things.",
            "World_Generation", "Settings to turn certain world-gen on or off.",
            "World_Regeneration", "If a chunk is encountered that skipped TC worldgen, then the game will attempt to regenerate certain world features if they are set to true. CAUTION: Best used for worlds created before you added this mod, and only if you know what you are doing. Backups are advised.",
            "Biomes", "Biomes and effects",
            "Runic_Shielding", "Runic Shielding");

    /** Category order as the original wrote Thaumcraft.cfg. */
    public static final List<String> CATEGORY_ORDER = List.of(
            "general", "Enchantments", "Monster_Spawning", "Research",
            "World_Generation", "World_Regeneration", "Biomes", "Runic_Shielding");

    // ---- general (25 options, syncConfigurable() order) ---------------------
    public static final ConfigOption ALLOW_MIRRORS = p("general", "allow_mirrors", BOOLEAN, true,
            "Setting this to false will disable arcane mirror research and crafting recipes.", "Config.java:403");
    public static final ConfigOption COLOR_BLIND = p("general", "color_blind", BOOLEAN, false,
            "Setting this to true will make certain colors higher contrast or darker to prevent them from being 'invisible' to color blind people.", "Config.java:406");
    public static final ConfigOption SHADERS = p("general", "shaders", BOOLEAN, true,
            "This setting will disable certain thaumcraft shaders for those who experience FPS drops.", "Config.java:409");
    public static final ConfigOption CROOKED = p("general", "crooked", BOOLEAN, true,
            "Hate crooked labels, kittens, puppies and all things awesome? If yes, set this to false.", "Config.java:412");
    public static final ConfigOption HARD_MODE_NODES = p("general", "hard_mode_nodes", BOOLEAN, true,
            "Negative nodes like hungry, tainted or dark nodes will have additional, much nastier, effects.", "Config.java:415");
    public static final ConfigOption WUSS_MODE = p("general", "wuss_mode", BOOLEAN, false,
            "Setting this to true disables Warp and similar mechanics. You wuss.", "Config.java:418");
    public static final ConfigOption WAND_DIAL_BOTTOM = p("general", "wand_dial_bottom", BOOLEAN, false,
            "Set to true to have the wand dial display in the bottom left instead of the top left.", "Config.java:421");
    public static final ConfigOption GOLEM_DELAY = p("general", "golem_delay", INTEGER, 5,
            "How many ticks a golem waits between checking for tasks. Setting it higher will save server ticks, but will make the golems slower to react.", "Config.java:424");
    public static final ConfigOption GOLEM_IGNORE_DELAY = p("general", "golem_ignore_delay", INTEGER, 10000,
            "How many milliseconds a golem will ignore an item after it has failed to find a destination or use for it. Min value 1000", "Config.java:431");
    public static final ConfigOption GOLEM_LINK_QUALITY = p("general", "golem_link_quality", INTEGER, 16,
            "The fx quality of the line connecting golems to marked blocks. Setting this below 4 deactives the effect entirely.", "Config.java:438");
    public static final ConfigOption NOTIFICATION_DELAY = p("general", "notification_delay", INTEGER, 5000,
            "Determines how fast notifications scroll downwards.", "Config.java:445");
    public static final ConfigOption NOTIFICATION_MAX = p("general", "notification_max", INTEGER, 15,
            "The maximum amount of notifications that are displayed onscreen.", "Config.java:448");
    public static final ConfigOption NODE_RARITY = p("general", "node_rarity", INTEGER, 36,
            "How rare nodes are in the world. The number means there will be (on average) one node per N chunks.", "Config.java:451");
    public static final ConfigOption SPECIAL_NODE_RARITY = p("general", "special_node_rarity", INTEGER, 18,
            "The chance of a node being special (pure, dark, unstable, etc.). The number means roughly 1 in N nodes will be special, so setting the number to 5 will mean 1 in 5 nodes may be special.", "Config.java:454");
    public static final ConfigOption DISPLAY_ASPECTS = p("general", "display_aspects", BOOLEAN, false,
            "Item aspects are hidden by default and pressing shift reveals them. Changing this setting to 'true' will reverse this behaviour and always display aspects unless shift is pressed.", "Config.java:461");
    public static final ConfigOption ALLOW_CHEAT_SHEET = p("general", "allow_cheat_sheet", BOOLEAN, false,
            "Enables a version of the Thauminomicon in creative mode that grants you all the research when you first use it.", "Config.java:464");
    public static final ConfigOption ALLOW_WARDED_STONE = p("general", "allow_warded_stone", BOOLEAN, true,
            "If set to false, warded stone, doors and glass will just be cosmetic in nature and not have its hardened properties (everyone will be able to break it with equal ease).", "Config.java:467");
    public static final ConfigOption THAUMCRAFT_VILLAGER_ID = np("general", "thaumcraft_villager_id", INTEGER, 190,
            "Thaumcraft wizard villager id", "Config.java:470", NOT_PORTED_ENTITY_ID);
    public static final ConfigOption THAUMCRAFT_BANKER_ID = np("general", "thaumcraft_banker_id", INTEGER, 191,
            "Thaumcraft banker villager id", "Config.java:473", NOT_PORTED_ENTITY_ID);
    public static final ConfigOption GOLEM_CHEST_INTERACT = p("general", "golem_chest_interact", BOOLEAN, true,
            "If set to true golems will attempt to play the chest opening animations and sounds whenever they interact with them.", "Config.java:476");
    public static final ConfigOption PORTABLEHOLE_BLACKLIST = p("general", "portablehole_blacklist", STRING, "iron_door",
            "This is a comma-delimited list of any block names the portable hole is not allowed to pass through.", "Config.java:479");
    public static final ConfigOption BLUE_MAGICAL_FOREST = p("general", "blue_magical_forest", BOOLEAN, false,
            "Set this to true to get the old blue magical forest back.", "Config.java:494");
    public static final ConfigOption BIOME_TAINT_FROM_FLUX = p("general", "biome_taint_from_flux", BOOLEAN, true,
            "Can Taint be caused by flux effects.", "Config.java:497");
    public static final ConfigOption BIOME_TAINT_SPREAD = p("general", "biome_taint_spread", INTEGER, 200,
            "The chance per block update (1 in n) of the Taint biome spreading. Setting it to 0 prevents the spread of Taint biomes.", "Config.java:500");
    public static final ConfigOption GLOWING_TAINT = p("general", "glowing_taint", BOOLEAN, true,
            "Setting this to false will remove the glowing purple nodules from taint fibres. This might prevent crashes some people experience and improve performance.", "Config.java:503");

    // ---- Enchantments (2 options, initialize() order) ------------------------
    public static final ConfigOption ENCH_HASTE = np("Enchantments", "ench_haste", INTEGER, 150,
            "", "Config.java:250", NOT_PORTED_ENCHANTMENT_ID);
    public static final ConfigOption ENCH_REPAIR = np("Enchantments", "ench_repair", INTEGER, 151,
            "", "Config.java:254", NOT_PORTED_ENCHANTMENT_ID);

    // ---- Monster_Spawning (8 options) ----------------------------------------
    public static final ConfigOption SPAWN_ANGRY_ZOMBIES = p("Monster_Spawning", "spawn_angry_zombies", BOOLEAN, true,
            "", "Config.java:393");
    public static final ConfigOption SPAWN_FIRE_BATS = p("Monster_Spawning", "spawn_fire_bats", BOOLEAN, true,
            "", "Config.java:394");
    public static final ConfigOption SPAWN_WISPS = p("Monster_Spawning", "spawn_wisps", BOOLEAN, true,
            "", "Config.java:395");
    public static final ConfigOption SPAWN_TAINTACLES = p("Monster_Spawning", "spawn_taintacles", BOOLEAN, true,
            "", "Config.java:396");
    public static final ConfigOption SPAWN_TAINT_SPORES = p("Monster_Spawning", "spawn_taint_spores", BOOLEAN, true,
            "", "Config.java:397");
    public static final ConfigOption SPAWN_PECHS = p("Monster_Spawning", "spawn_pechs", BOOLEAN, true,
            "", "Config.java:398");
    public static final ConfigOption SPAWN_ELDERCREATURES = p("Monster_Spawning", "spawn_eldercreatures", BOOLEAN, true,
            "", "Config.java:399");
    public static final ConfigOption CHAMPION_MOBS = p("Monster_Spawning", "champion_mobs", BOOLEAN, true,
            "Setting this to false will disable spawning champion mobs. Even when false they will still have a greatly reduced chance of spawning in certain dangerous places.", "Config.java:400");

    // ---- Research (2 options) -------------------------------------------------
    public static final ConfigOption RESEARCH_DIFFICULTY = p("Research", "research_difficulty", INTEGER, 0,
            "0 = normal, -1 = easy (all research items are directly purchased with RP), 1 = Hard (all research items need to be solved via the research table)", "Config.java:387");
    public static final ConfigOption ASPECT_TOTAL_CAP = p("Research", "aspect_total_cap", INTEGER, 100,
            "The total amount of RP you can have in your pool per aspect before the scanning soft cap kicks in.", "Config.java:390");

    // ---- World_Generation (7 options) -----------------------------------------
    public static final ConfigOption GENERATE_AURA_NODES = p("World_Generation", "generate_aura_nodes", BOOLEAN, true,
            "", "Config.java:368");
    public static final ConfigOption GENERATE_STRUCTURES = p("World_Generation", "generate_structures", BOOLEAN, true,
            "", "Config.java:369");
    public static final ConfigOption GENERATE_CINNIBAR_ORE = p("World_Generation", "generate_cinnibar_ore", BOOLEAN, true,
            "", "Config.java:370");
    public static final ConfigOption GENERATE_AMBER_ORE = p("World_Generation", "generate_amber_ore", BOOLEAN, true,
            "", "Config.java:371");
    public static final ConfigOption GENERATE_INFUSED_STONE = p("World_Generation", "generate_infused_stone", BOOLEAN, true,
            "", "Config.java:372");
    public static final ConfigOption GENERATE_TREES = p("World_Generation", "generate_trees", BOOLEAN, true,
            "", "Config.java:373");
    public static final ConfigOption GENERATE_TAINT = p("World_Generation", "generate_taint", BOOLEAN, true,
            "Can taint biomes generate at worldgen", "Config.java:374");

    // ---- World_Regeneration (8 options) ----------------------------------------
    public static final ConfigOption REGEN_KEY = p("World_Regeneration", "regen_key", STRING, "DEFAULT",
            "This key is used to keep track of which chunk have been generated/regenerated. Changing it will cause the regeneration code to run again, so only change it if you want it to happen. Useful to regen only one world feature at a time.", "Config.java:377");
    public static final ConfigOption REGEN_AURA_NODES = p("World_Regeneration", "aura_nodes", BOOLEAN, false,
            "", "Config.java:380");
    public static final ConfigOption REGEN_STRUCTURES = p("World_Regeneration", "structures", BOOLEAN, false,
            "", "Config.java:381");
    public static final ConfigOption REGEN_CINNIBAR_ORE = p("World_Regeneration", "cinnibar_ore", BOOLEAN, false,
            "", "Config.java:382");
    public static final ConfigOption REGEN_AMBER_ORE = p("World_Regeneration", "amber_ore", BOOLEAN, false,
            "", "Config.java:383");
    public static final ConfigOption REGEN_INFUSED_STONE = p("World_Regeneration", "infused_stone", BOOLEAN, false,
            "", "Config.java:384");
    public static final ConfigOption REGEN_TREES = p("World_Regeneration", "trees", BOOLEAN, false,
            "", "Config.java:385");
    public static final ConfigOption REGEN_TAINT = p("World_Regeneration", "taint", BOOLEAN, false,
            "", "Config.java:386");

    // ---- Biomes (7 options, initialize() order) --------------------------------
    public static final ConfigOption TAINT_BIOME_WEIGHT = p("Biomes", "taint_biome_weight", INTEGER, 2,
            "higher values increases number of taint biomes. If you are using biome addon mods you probably want to increase this weight quite a bit", "Config.java:185");
    public static final ConfigOption BIOME_TAINT = np("Biomes", "biome_taint", INTEGER, 192,
            "Taint biome id", "Config.java:188", NOT_PORTED_BIOME_ID);
    public static final ConfigOption MAGICAL_FOREST_BIOME_WEIGHT = p("Biomes", "magical_forest_biome_weight", INTEGER, 5,
            "higher values increases number of magical forest biomes. If you are using biome addon mods you probably want to increase this weight quite a bit", "Config.java:202");
    public static final ConfigOption BIOME_MAGICAL_FOREST = np("Biomes", "biome_magical_forest", INTEGER, 193,
            "Magical Forest biome id", "Config.java:205", NOT_PORTED_BIOME_ID);
    public static final ConfigOption BIOME_EERIE = np("Biomes", "biome_eerie", INTEGER, 194,
            "Eerie biome id", "Config.java:219", NOT_PORTED_BIOME_ID);
    public static final ConfigOption BIOME_ELDRITCH = np("Biomes", "biome_eldritch", INTEGER, 195,
            "Eldritch Lands biome id", "Config.java:233", NOT_PORTED_BIOME_ID);
    public static final ConfigOption OUTER_LANDS_DIM = np("Biomes", "outer_lands_dim", INTEGER, -42,
            "", "Config.java:247", NOT_PORTED_DIMENSION_ID);

    // ---- Runic_Shielding (3 options) -------------------------------------------
    public static final ConfigOption RUNIC_RECHARGE_SPEED = p("Runic_Shielding", "runic_recharge_speed", INTEGER, 2000,
            "How many milliseconds pass between runic shielding recharge ticks. Lower values equals faster recharge. Minimum of 500.", "Config.java:506");
    public static final ConfigOption RUNIC_RECHARGE_DELAY = p("Runic_Shielding", "runic_recharge_delay", INTEGER, 80,
            "How many game ticks pass after a shield has been reduced to zero before it can start recharging again. Minimum of 0.", "Config.java:509");
    public static final ConfigOption RUNIC_COST = p("Runic_Shielding", "runic_cost", INTEGER, 50,
            "How much aer and terra centi-vis (0.01 vis) it costs to reacharge a single unit of shielding. Minimum of 0.", "Config.java:512");

    /** Every original option, in category order (docs/config-reference.md order). */
    public static final List<ConfigOption> OPTIONS = List.of(
            ALLOW_MIRRORS, COLOR_BLIND, SHADERS, CROOKED, HARD_MODE_NODES, WUSS_MODE,
            WAND_DIAL_BOTTOM, GOLEM_DELAY, GOLEM_IGNORE_DELAY, GOLEM_LINK_QUALITY,
            NOTIFICATION_DELAY, NOTIFICATION_MAX, NODE_RARITY, SPECIAL_NODE_RARITY,
            DISPLAY_ASPECTS, ALLOW_CHEAT_SHEET, ALLOW_WARDED_STONE, THAUMCRAFT_VILLAGER_ID,
            THAUMCRAFT_BANKER_ID, GOLEM_CHEST_INTERACT, PORTABLEHOLE_BLACKLIST,
            BLUE_MAGICAL_FOREST, BIOME_TAINT_FROM_FLUX, BIOME_TAINT_SPREAD, GLOWING_TAINT,
            ENCH_HASTE, ENCH_REPAIR,
            SPAWN_ANGRY_ZOMBIES, SPAWN_FIRE_BATS, SPAWN_WISPS, SPAWN_TAINTACLES,
            SPAWN_TAINT_SPORES, SPAWN_PECHS, SPAWN_ELDERCREATURES, CHAMPION_MOBS,
            RESEARCH_DIFFICULTY, ASPECT_TOTAL_CAP,
            GENERATE_AURA_NODES, GENERATE_STRUCTURES, GENERATE_CINNIBAR_ORE,
            GENERATE_AMBER_ORE, GENERATE_INFUSED_STONE, GENERATE_TREES, GENERATE_TAINT,
            REGEN_KEY, REGEN_AURA_NODES, REGEN_STRUCTURES, REGEN_CINNIBAR_ORE,
            REGEN_AMBER_ORE, REGEN_INFUSED_STONE, REGEN_TREES, REGEN_TAINT,
            TAINT_BIOME_WEIGHT, BIOME_TAINT, MAGICAL_FOREST_BIOME_WEIGHT,
            BIOME_MAGICAL_FOREST, BIOME_EERIE, BIOME_ELDRITCH, OUTER_LANDS_DIM,
            RUNIC_RECHARGE_SPEED, RUNIC_RECHARGE_DELAY, RUNIC_COST);

    private TcConfigCatalog() {
    }

    public static List<ConfigOption> optionsIn(String category) {
        return OPTIONS.stream().filter(o -> o.category().equals(category)).toList();
    }

    public static List<ConfigOption> portedOptions() {
        return OPTIONS.stream().filter(ConfigOption::ported).toList();
    }

    private static ConfigOption p(String category, String name, ConfigOptionType type, Object defaultValue,
            String comment, String source) {
        return new ConfigOption(category, name, type, defaultValue, comment, source, true, null);
    }

    private static ConfigOption np(String category, String name, ConfigOptionType type, Object defaultValue,
            String comment, String source, String reason) {
        return new ConfigOption(category, name, type, defaultValue, comment, source, false, reason);
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put(kv[i], kv[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(out);
    }
}
