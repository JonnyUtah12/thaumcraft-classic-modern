package com.jonnyutah12.thaumcraftclassic.entities.monster;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import javax.annotation.Nullable;

/**
 * Every constant and rule of the Pech, kept {@code Level}-free so the unit test
 * can pin them without a world.
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityPech}, 830 lines,
 * {@code extends EntityMob implements IRangedAttackMob}. Ledger row
 * {@code docs/content-mapping.csv:321} ({@code Thaumcraft:Pech} →
 * {@code thaumcraft:pech}), registration
 * {@code common/config/ConfigEntities.java:103-104}.</p>
 *
 * <p>Every figure below cites the decompiled line it came from
 * ({@code EntityPech.java:LINE}). The three cross-referenced 1.7.10 SRG names
 * resolve against {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}
 * (the methods CSV is not vendored, so vanilla method hooks are resolved by
 * behaviour and named in the entity's javadoc).</p>
 */
public final class PechAiLogic {

    // ---- registration (ConfigEntities.java:103-104) ----
    /** {@code registerModEntity("Pech", EntityPech.class, "Pech", 12,
     *  Thaumcraft.instance, 64, 3, true)}. */
    public static final int MOD_INDEX = 23;
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** 1.21.1's {@code EntityType.Builder#clientTrackingRange} counts CHUNKS,
     *  so the original's 64 blocks ({@code ConfigEntities.java:103}) become 4 —
     *  the family rule the c2 slice pinned at {@code BrainyZombieAiLogicTest:38}
     *  ("64 blocks is 4"). */
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    // ---- geometry, xp, armour ----
    /** {@code :86} — {@code func_70105_a(0.6F, 1.8F)}: the pech is the tallest
     *  common mob in the mod at 1.8, and its width is the vanilla humanoid 0.6
     *  (the "1.2 wide" figure is not in the source). */
    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 1.8F;
    /** {@code :211-213} — {@code func_70047_e() = height * 0.66F} = 1.188. */
    public static final float EYE_HEIGHT_FACTOR = 0.66F;
    public static double eyeHeight() {
        return HEIGHT * EYE_HEIGHT_FACTOR;
    }

    /** {@code :254-259} — maxHealth 30.0, attackDamage 6.0, movementSpeed 0.5
     *  ({@code field_111267_a}/{@code field_111264_e}/{@code field_111263_d}). */
    public static final double MAX_HEALTH = 30.0;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double MOVEMENT_SPEED = 0.5;

    /** {@code :813} — {@code getExperiencePoints = 5}. */
    public static final int XP = 5;

    /** {@code :507-510} — {@code func_70658_aO} (getTotalArmorValue) is an
     *  OVERRIDE: {@code super.getTotalArmorValue() + 2}, capped at 20. It runs on
     *  every armour read, so a wild pech with no armour equipped stands at 2. */
    public static final int ARMOR_BONUS = 2;
    public static final int ARMOR_CAP = 20;
    public static int totalArmorValue(final int parentValue) {
        return Math.min(parentValue + ARMOR_BONUS, ARMOR_CAP);
    }

    /** {@code :250-252} — {@code func_70650_aV() = true} (the 1.7.10 boss flag).
     *  1.21.1 has no {@code Mob#isBoss()}: the boss health bar is a
     *  {@code ServerBossEvent} drawn by the renderer/HUD lane, so the flag ships
     *  as a pinned constant and the bar is recorded in the row's
     *  {@code known_differences} (TC-M7-03-s). */
    public static final boolean IS_BOSS = true;

    /** {@code :110} — {@code equipmentDropChances[0] = 0.2F} (the mainhand slot,
     *  {@code field_82174_bp} = equipmentDropChances). */
    public static final float EQUIP_DROP_CHANCE_MAINHAND = 0.2F;
    /** {@code :172} — {@code equipmentDropChances[0] = 0.1F} for a
     *  wand-carrying pech. */
    public static final float EQUIP_DROP_CHANCE_WAND = 0.1F;

    // ---- pech type (the mainhand item decides the attack) ----
    public static final int TYPE_NORMAL = 0;   // melee
    public static final int TYPE_WAND = 1;     // EntityPechBlast
    public static final int TYPE_BOW = 2;      // EntityArrow

    /** {@code :170-178} — the type lookup, and {@code :440-462} the goal
     *  selection, both key off {@code getPechType()}. */
    public static int pechTypeFor(final boolean wand, final boolean bow) {
        if (wand) {
            return TYPE_WAND;
        }
        if (bow) {
            return TYPE_BOW;
        }
        return TYPE_NORMAL;
    }

    /** {@code :170} — {@code mainhand.getItem() == ConfigItems.itemWandCasting}.
     *  ItemWandCasting is content-mapping row 170 ({@code Thaumcraft:WandCasting},
     *  {@code not_started}), so the
     *  lookup resolves to no item today; the shape is kept so the wand lane
     *  closes it without touching this class. */
    public static final ResourceLocation WAND_ITEM =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandcasting");
    /** {@code :166-168} — the focus that rides the wand
     *  ({@code ConfigItems.itemFocusPech}, row 181 {@code Thaumcraft:FocusPech},
     *  {@code not_started}). */
    public static final ResourceLocation FOCUS_PECH_ITEM =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focuspech");
    /** {@code :175} — {@code Items.field_151031_f} = {@code bow} (ported). */
    public static final ResourceLocation BOW_ITEM = ResourceLocation.withDefaultNamespace("bow");

    // ---- data values (1.7.10 watcher indices; 1.21.1 keys them, not indices) ----
    public static final int DATA_INDEX_TYPE = 13;    // :221 Byte(0)
    public static final int DATA_INDEX_ANGER = 14;   // :222 Short(0)
    public static final int DATA_INDEX_TAMED = 16;   // :223 Byte(0)
    public static final int LOOT_SLOTS = 9;          // :60

    // ---- anger, mumble, sound cadence ----
    /** {@code :498} — {@code setAnger(400 + nextInt(400))} when the pech is
     *  angered. */
    public static final int ANGER_BASE = 400;
    public static final int ANGER_JITTER = 400;
    public static int rollAnger(final int nextInt) {
        return ANGER_BASE + nextInt;
    }
    /** {@code :555} — the growl cadence: every 100 anger ticks. */
    public static final int CHARGE_CADENCE = 100;
    /** {@code :420-422} — {@code func_70627_aG() = 120} (the ambient interval). */
    public static final int AMBIENT_SOUND_INTERVAL = 120;
    /** {@code :424-426} — {@code func_70599_aP() = 0.4F} (the voice pitch hook). */
    public static final float VOICE_PITCH = 0.4F;
    /** {@code :540} — {@code mumble *= 0.75F} every tick (the client decay). */
    public static final float MUMBLE_DECAY = 0.75F;
    /** {@code :552-563} — the per-tick mumble roll: {@code nextInt(100)} gates
     *  the trade chat, {@code nextInt(15)} the angry particles, {@code nextInt(25)}
     *  the tamed hearts. */
    public static final int MUMBLE_ROLL = 100;
    public static final int ANGRY_PARTICLE_ROLL = 15;
    public static final int TAMED_PARTICLE_ROLL = 25;
    public static final int PARTICLE_COUNT = 5;
    public static final float PARTICLE_SIGMA = 0.02F;

    // ---- entity events (:352-397 handleStatusEvent, :500, :563) ----
    public static final byte EVENT_MUMBLE = 16;         // :563 / :355 (mumble = PI)
    public static final byte EVENT_MUMBLE_TRADE = 17;   // :563 / :358 (mumble = 2*PI)
    public static final byte EVENT_TAMED = 18;          // :641 (5 happy particles)
    public static final byte EVENT_ANGRY = 19;          // :500 (5 angry particles)

    // ---- spawn rules (:189-209) ----
    /** {@code :195} — the count box is {@code entityBoundingBox.expand(16.0)}. */
    public static final double SPAWN_BOX_INFLATE = 16.0;
    /** {@code :207} — {@code countPechs() < 4}. */
    public static final int MAX_PECHS_IN_BOX = 4;
    /** {@code :202-206} — overworld only ({@code dimension != 0} kills the
     *  magical-biome gate). */
    public static final boolean OVERWORLD_ONLY = true;
    /** {@code :208} — the exclusion ids, taken from the ledger's biome rows
     *  ({@code thaumcraft:biome.biomegentaint} row 961, {@code done};
     *  {@code ...biomegenmagicalforest} row 962 and {@code ...biomegeneerie}
     *  row 963, both {@code not_started}): the gate is live code over ids that
     *  resolve to no biome today. */
    public static final ResourceLocation BIOME_TAINT =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "biome.biomegentaint");
    public static final ResourceLocation BIOME_MAGICAL_FOREST =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "biome.biomegenmagicalforest");
    public static final ResourceLocation BIOME_EERIE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "biome.biomegeneerie");
    /** {@code :189-191} — {@code BiomeDictionary.getType(biome) == MAGICAL} →
     *  1.21.1's seam is NeoForge's biome tag {@code neoforge:is_magical}
     *  ({@code Tags.Biomes.IS_MAGICAL}, the BiomeDictionary-type port). */
    public static boolean isMagicBiomeTag(final boolean carriesIsMagical) {
        return carriesIsMagical;
    }
    /** {@code :201-205} — the three named exclusions. */
    public static boolean isExcludedBiome(final boolean taint, final boolean magicalForest,
            final boolean eerie) {
        return !(taint || magicalForest || eerie);
    }

    // ---- target lookup (:440-442, :548-551) ----
    /** {@code :441} — {@code getAnger() == 0 ? null : super.func_70782_k()}.
     *  The 1.7.10 {@code EntityMob} body of that hook is the nearest
     *  attackable player inside 16x16. */
    public static boolean hasTargetPermission(final int anger) {
        return anger > 0;
    }
    public static final double PARENT_TARGET_RANGE_XZ = 16.0;
    public static final double PARENT_TARGET_RANGE_Y = 16.0;

    // ---- goal table (:60-67 fields, :95-108 registration, :444-462 swap) ----
    /** slot : goal (all at the original slots). */
    public static final int GOAL_SLOT_SWIM = 0;             // :95
    public static final int GOAL_SLOT_TRADE = 1;            // :96
    public static final int GOAL_SLOT_ITEM_PICKUP = 3;      // :97
    public static final int GOAL_SLOT_DOOR = 5;             // :98 OpenDoor(this,true)
    public static final int GOAL_SLOT_RESTRICTION = 6;      // :99 MoveTowardsRestriction(0.5)
    public static final int GOAL_SLOT_VILLAGE = 6;          // :100 MoveThroughVillage(1.0,false)
    public static final int GOAL_SLOT_WANDER = 9;           // :101 Wander(0.6)
    public static final int GOAL_SLOT_WATCH_PLAYER = 9;     // :102 WatchClosest2(Player,3.0,1.0)
    public static final int GOAL_SLOT_WATCH = 10;           // :103 WatchClosest(Living,8.0)
    public static final int GOAL_SLOT_LOOK_IDLE = 11;       // :104
    public static final int TARGET_SLOT_HURT = 1;           // :105 HurtByTarget(this,false)
    public static final int COMBAT_SLOT_MELEE = 2;          // :449
    public static final int COMBAT_SLOT_ARROW = 2;          // :452
    public static final int COMBAT_SLOT_BLAST = 2;          // :453
    public static final int COMBAT_SLOT_AVOID = 4;          // :460

    public static final float WAND_SPEED = 0.6F;            // :64
    public static final int WAND_MIN_TICKS = 20;            // :64
    public static final int WAND_MAX_RANGE = 50;            // :64
    public static final float WAND_RANGE = 15.0F;           // :64
    public static final float BLAST_SPEED = 0.6F;           // :65
    public static final int BLAST_MIN_TICKS = 20;           // :65
    public static final int BLAST_MAX_RANGE = 30;           // :65
    public static final float BLAST_RANGE = 15.0F;          // :65
    public static final double MELEE_SPEED = 0.6;           // :63 (the same 0.6 the arrow goals carry)
    public static final float AVOID_RANGE = 8.0F;           // :67
    public static final double AVOID_SPEED_CLOSE = 0.5;     // :67
    public static final double AVOID_SPEED_FAR = 0.6;       // :67
    public static final double RESTRICTION_SPEED = 0.5;     // :99
    public static final double VILLAGE_SPEED = 1.0;         // :100
    public static final double WANDER_SPEED = 0.6;          // :101
    public static final float WATCH_RANGE = 8.0F;           // :103
    public static final float WATCH_PLAYER_RANGE = 3.0F;    // :102
    public static final float WATCH_PLAYER_SPEED = 1.0F;    // :102

    // ---- arrow attack (:464-479) ----
    public static final double ARROW_SPEED = 1.6;                    // :466
    public static final double ARROW_INACCURACY_BASE = 14;           // :466
    public static final double ARROW_INACCURACY_PER_DIFFICULTY = 4;  // :466
    public static final double ARROW_DAMAGE_BASE = 2.0;              // :468
    public static final double ARROW_DAMAGE_GAUSS = 0.25;            // :468
    public static final double ARROW_DAMAGE_PER_DIFFICULTY = 0.11;   // :468
    public static final double ARROW_POWER_PER_LEVEL = 0.5;          // :470 / 1.21.1 Power
    public static final int BOW_SOUND_VOLUME = 1;                    // :476
    public static final double BOW_SOUND_PITCH_BASE = 1.0;           // :476
    public static final double BOW_SOUND_PITCH_SPREAD = 0.4;         // :476

    /** {@code :472} — the punch: {@code if (rand.nextInt(3) == 0)} gives the
     *  arrow knockback strength 1. 21.1.259's {@code AbstractArrow} exposes NO
     *  knockback-strength setter (its knockback term is read from the arming
     *  stack's {@code minecraft:knockback} component, and a mob-fired arrow has
     *  no arming stack), so the rule ships here as data and the unit test pins
     *  the roll; the entity has no live seam for the value. */
    /** :469-470 (Power) and :472-473 (Punch) ride the WEAPON in 1.21.1:
     *  {@code AbstractArrow} applies them at impact from its
     *  {@code firedFromWeapon} association ({@code AbstractArrow.java:450 —
     *  EnchantmentHelper.modifyKnockback(serverLevel, this.firedFromWeapon,
     *  ...)}), and 21.1.259 exposes NO knockback-strength setter on the arrow.
     *  The pech therefore fires with its MAINHAND ITEM as {@code
     *  firedFromWeapon} (see {@code EntityPech.performRangedAttack}), which is
     *  the 1.21.1 seam for both legs; the original's explicit {@code damage +=
     *  i*0.5 + 0.5} term is vanilla 1.21.1's own Power formula (+0.5 per level,
     *  +0.5), so no arithmetic is duplicated here. */
    public static final double ARROW_POWER_FLAT = 0.5;    // :470 == 1.21.1 Power's flat term

    public static double arrowInaccuracy(final int difficulty) {
        return ARROW_INACCURACY_BASE - difficulty * ARROW_INACCURACY_PER_DIFFICULTY;
    }

    public static double arrowDamage(final double distanceFactor, final double gaussian,
            final float difficulty) {
        return distanceFactor * ARROW_DAMAGE_BASE + gaussian * ARROW_DAMAGE_GAUSS
                + difficulty * ARROW_DAMAGE_PER_DIFFICULTY;
    }

    public static double arrowPowerBonus(final int powerLevel) {
        return powerLevel > 0 ? powerLevel * ARROW_POWER_PER_LEVEL + ARROW_POWER_FLAT : 0.0;
    }

    public static double bowSoundPitch(final double nextFloat) {
        return BOW_SOUND_PITCH_BASE / (nextFloat * BOW_SOUND_PITCH_SPREAD + 0.8);
    }

    // ---- blast attack (:480-489) + EntityPechBlast ----
    public static final float BLAST_MOTION = 1.5F;                   // :485
    public static final float BLAST_SPREAD = 4.0F;                   // :485
    public static final double BLAST_AIM_Y_OFFSET = 1.500000023841858; // :482
    public static final double BLAST_ARC_FACTOR = 0.1;               // :484
    public static final float BLAST_SMOKE_CHANCE = 0.1F;             // :481
    public static final float BLAST_SOUND_VOLUME = 2.0F;             // EntityPechBlast.java:128
    public static final int BLAST_DAMAGE_PLAYER = 3;                 // :113
    public static final int BLAST_DAMAGE_PLAYER_SMOKE = 4;           // :113
    public static final int BLAST_DAMAGE_OTHER = 2;                  // :115
    public static final int BLAST_DAMAGE_OTHER_SMOKE = 3;            // :115
    public static final int BLAST_POISON_TICKS = 80;                 // :117
    public static final int BLAST_POISON_AMPLIFIER = 0;              // :117
    public static final int BLAST_POISON_AMPLIFIER_SMOKE = 1;        // :117
    public static final int BLAST_SMOKE_PUFFS_SPAWN = 20;            // :159 (event 10)
    public static final int BLAST_SMOKE_PUFFS_IMPACT = 10;           // :107
    public static final float BLAST_BOX_SMALL = 0.5F;                // :167
    public static final float BLAST_BOX_BIG = 1.0F;                  // :167

    public static double blastAimY(final double targetY, final double targetEyeHeight,
            final double y) {
        return targetY + targetEyeHeight - BLAST_AIM_Y_OFFSET - y;
    }

    public static double blastArc(final double horizontal) {
        return horizontal * BLAST_ARC_FACTOR;
    }

    public static float blastSmokeRoll() {
        return BLAST_SMOKE_CHANCE;
    }

    public static int blastDamagePlayer(final boolean smoke) {
        return smoke ? BLAST_DAMAGE_PLAYER_SMOKE : BLAST_DAMAGE_PLAYER;
    }

    public static int blastDamageOther(final boolean smoke) {
        return smoke ? BLAST_DAMAGE_OTHER_SMOKE : BLAST_DAMAGE_OTHER;
    }

    public static int blastPoisonAmplifier(final boolean smoke) {
        return smoke ? BLAST_POISON_AMPLIFIER_SMOKE : BLAST_POISON_AMPLIFIER;
    }

    // ---- equipment table (:120-164, the 20-side roll) ----
    /** {@code :122-163} — {@code nextInt(20)}: 0 and 12 want the wand (+focus),
     *  1 a stone sword, 2/4/10/11/13 a bow, 3 a stone axe, 5 an iron sword,
     *  6 an iron axe, 7 a fishing rod, 8 a stone pickaxe, 9 an iron pickaxe.
     *  {@code null} = the unported wand branch (see BD-47). */
    public static ResourceLocation equipmentFor(final int roll) {
        switch (roll) {
            case 0:
            case 12:
                return null;                       // :123-146 wand + ItemFocusPech
            case 1:
                return ResourceLocation.withDefaultNamespace("stone_sword");    // :148
            case 2:
            case 4:
            case 10:
            case 11:
            case 13:
                return ResourceLocation.withDefaultNamespace("bow");            // :150
            case 3:
                return ResourceLocation.withDefaultNamespace("stone_axe");      // :152
            case 5:
                return ResourceLocation.withDefaultNamespace("iron_sword");     // :154
            case 6:
                return ResourceLocation.withDefaultNamespace("iron_axe");       // :156
            case 7:
                return ResourceLocation.withDefaultNamespace("fishing_rod");    // :158
            case 8:
                return ResourceLocation.withDefaultNamespace("stone_pickaxe");  // :160
            case 9:
                return ResourceLocation.withDefaultNamespace("iron_pickaxe");   // :162
            default:
                return null;
        }
    }
    /** {@code :126-143} — the wand's vis charges when it spawns in a magical
     *  biome: EARTH/AIR/WATER 2+nextInt(6), FIRE/AIR/ORDER nextInt(4). */
    public static final int WAND_VIS_BASE = 2;
    public static final int WAND_VIS_JITTER = 6;
    public static final int WAND_VIS_SMALL_JITTER = 4;

    // ---- drops (:324-350) ----
    /** {@code :333} — every loot slot drops at 0.88. */
    public static final float LOOT_DROP_CHANCE = 0.88F;
    /** {@code :334} — {@code func_70030_a(x, y + 1.5, z)}: a flat 1.5-block
     *  offset, neither the box height nor the width. */
    public static final double LOOT_DROP_OFFSET = 1.5;
    /** {@code :340} — each of the {@code 1 + looting} beans is gated by its own
     *  {@code rand.nextBoolean()} and each carries ONE random PRIMAL aspect
     *  ({@code Aspect.getPrimalAspects().get(nextInt(6))}), not the pech's own
     *  aspect list. */
    public static final int BEAN_ROLL_BOUND = 2;            // :340 nextBoolean
    public static final int PRIMAL_ASPECT_COUNT = 6;        // :340
    /** {@code :341} — {@code nextInt(10) < 1 + looting} → itemResource meta 18. */
    public static final int MANA_BEAN_ROLL = 10;
    public static final int MANA_BEAN_LOOTING_ADD = 1;
    public static final int MANA_BEAN_META = 0;
    /** {@code :344} — the itemResource meta. */
    public static final int RESOURCE_DROP_META = 18;
    /** {@code :348-350} — {@code func_70085_c(int)} (dropAllLoot) replaces the
     *  parent's equipment drop with itemResource meta 9. */
    public static final int DROP_ALL_LOOT_META = 9;

    public static boolean dropsResource(final int nextInt, final int looting) {
        return nextInt < MANA_BEAN_LOOTING_ADD + looting;
    }

    public static int manaBeanCount(final int looting) {
        return MANA_BEAN_LOOTING_ADD + looting;
    }

    // ---- pickup / taming (:612-692) ----
    /** {@code :641} — {@code nextInt(10) < getValue(item)} tames the pech. */
    public static final int TAME_ROLL_BOUND = 10;   // :623, :641
    public static boolean tames(final int nextInt, final int value) {
        return nextInt < value;
    }
    /** {@code :707-722} — an item is valued when it is in the table, or when its
     *  object tags carry {@code Aspect.GREED}. 1.7.10's GREED aspect is the
     *  port's {@code lucrum} (id + colour 15121988 + parents humanus/fames match
     *  {@code AspectDefinitions.java:70}). */
    public static final String GREED_ASPECT_ID = "lucrum";
    /** {@code :735} — the aspect-derived value is capped at 32. */
    public static final int GREED_VALUE_CAP = 32;
    /** {@code :740-745} — the valued table: mana bean 1, gold ingot 2, golden
     *  apple 2, ender pearl 3, diamond 4, emerald 5. */
    public static final Map<ResourceLocation, Integer> VALUED_ITEMS = valuedItems();

    private static Map<ResourceLocation, Integer> valuedItems() {
        final Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
        map.put(ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemmanabean"), 1);  // :740
        map.put(ResourceLocation.withDefaultNamespace("gold_ingot"), 2);                  // :741
        map.put(ResourceLocation.withDefaultNamespace("golden_apple"), 2);                // :742
        map.put(ResourceLocation.withDefaultNamespace("ender_pearl"), 3);                 // :743
        map.put(ResourceLocation.withDefaultNamespace("diamond"), 4);                     // :744
        map.put(ResourceLocation.withDefaultNamespace("emerald"), 5);                     // :745
        return Map.copyOf(map);
    }

    public static int tableValue(final ResourceLocation item) {
        return VALUED_ITEMS.getOrDefault(item, -1);
    }

    public static int aspectValue(final int greedAmount) {
        return Math.min(GREED_VALUE_CAP, greedAmount);
    }

    // ---- the item-pickup goal (AIPechItemEntityGoto, 122 lines) ----
    public static final float PICKUP_TARGET_RANGE = 16.0F;   // :52
    public static final int PICKUP_MUTEX_BITS = 3;           // :58
    /** {@code :62} — {@code ticksExisted % Config.golemDelay > 0} gates the scan;
     *  {@code Config.java:130} — {@code golemDelay = 5}. */
    public static final int GOLEM_DELAY = 5;
    public static final int PENALTY_BASE = 4;                // :119
    public static final int PENALTY_FAIL = 10;               // :127, :130
    public static final double PENALTY_END_DISTANCE_SQ = 1.0; // :124
    public static final double PICKUP_DISTANCE_SQ = 1.5;     // :135
    public static final double PICKUP_SPEED_FACTOR = 1.5;    // :111, :120
    /** {@code :78} — the pech never chases an item owned by {@code "PechDrop"}. */
    public static final String PECH_DROP_OWNER = "PechDrop";
    /** {@code :117} — {@code lookAt(target, 30.0F, 30.0F)}. */
    public static final float PICKUP_LOOK_YAW = 30.0F;
    public static final float PICKUP_LOOK_PITCH = 30.0F;

    // ---- the trade goal (AIPechTradePlayer, 35 lines) ----
    public static final int TRADE_MUTEX_BITS = 5;            // :11

    // ---- the blast goal's own range (EntityPech.java:65) ----
    /** {@code :65} — the blast {@code EntityAIArrowAttack} is the same goal with
     *  the range 30 (the arrow goal's is 50) and the same 0.6 / 20 / 15.0F. */
    public static final int BLAST_GOAL_MAX_RANGE = 30;

    // ---- the trade goal's gate (AIPechTradePlayer.java:15-24) ----
    /** The 1.7.10 {@code field_70133_I} = {@code velocityChanged} proxy: a mob
     *  whose delta movement is under this is "not being moved this tick". */
    public static final double STANDING_STILL_EPSILON_SQ = 1.0E-6;
    /** {@code :24} — {@code velocityChanged ? false : trading}. */
    public static boolean canStartTrade(final boolean standingStill, final boolean trading) {
        return standingStill && trading;
    }

    /** {@code :612-630} — {@code canPickup}: a valued item is always worth
     *  picking while wild; otherwise the pech needs a free slot (an emptied slot
     *  is nulled first, {@code :617-620}) or a stack that merges into an
     *  existing slot without passing that slot's max size. */
    public static boolean canPickup(final boolean tamed, final boolean valued,
            final boolean freeSlot, final boolean merges, final boolean fits) {
        if (!tamed && valued) {
            return true;                                            // :615
        }
        return freeSlot || (merges && fits);                        // :621-628
    }

    /** {@code :631-654} — the merge transfer: the whole stack when it fits,
     *  otherwise exactly the room left in the slot. */
    public static int mergeTransfer(final int heldCount, final int heldMax, final int want) {
        return want + heldCount <= heldMax ? want : Math.min(want, heldMax - heldCount);
    }

    /** {@code :640} — the {@code nextBoolean()} gate on each mana bean drop. */
    public static boolean beanDropped(final boolean coinFlip) {
        return coinFlip;
    }

    /** {@code :713-714, :729-730} — merge the tag-table aspects with the stack's
     *  own carried essentia (1.7.10's {@code getObjectTags} + {@code getBonusTags}
     *  pair). Null-safe: a null base starts from the empty list, a null bonus
     *  leaves the base untouched, and a pair of nulls yields {@code null} so the
     *  caller's {@code al != null} test behaves as the original's.
     *  {@code AspectList#add(AspectList)} is the port's merge primitive. */
    @Nullable
    public static AspectList mergeAspectLists(@Nullable final AspectList base,
            @Nullable final AspectList bonus) {
        if (base == null && bonus == null) {
            return null;
        }
        final AspectList out = base == null ? new AspectList() : base.copy();
        if (bonus != null) {
            out.add(bonus);
        }
        return out;
    }

    /** {@code :78} — {@code !owner.equals("PechDrop")}: the pech never chases an
     *  item it dropped itself (the owner tag the drop code writes). */
    public static boolean isPechDrop(final String owner) {
        return PECH_DROP_OWNER.equals(owner);
    }

    private PechAiLogic() {
    }
}
