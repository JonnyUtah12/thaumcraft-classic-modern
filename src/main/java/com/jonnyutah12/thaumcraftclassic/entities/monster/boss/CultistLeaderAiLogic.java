package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The rules of {@code thaumcraft.common.entities.monster.boss.EntityCultistLeader} (203 lines),
 * Level-free so every figure is pinned by
 * {@code src/test/java/com/jonnyutah12/thaumcraftclassic/entities/monster/boss/CultistLeaderAiLogicTest.java}
 * without a world (the family's AiLogic pattern — {@code CultistAiLogic}, {@code EldritchGuardianAiLogic}).
 *
 * <p>Every line citation below is the 1.7.10 decompile at
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityCultistLeader.java}
 * unless the file is named. The registration row is {@code ConfigEntities.java:115} —
 * {@code registerModEntity(EntityCultistLeader.class, "CultistLeader", 29, instance, 64, 3, true)}.</p>
 */
public final class CultistLeaderAiLogic {

    private CultistLeaderAiLogic() {
    }

    // ---------------------------------------------------------------- registration (:56, :66; ConfigEntities.java:115)

    /** {@code :56} — {@code func_70105_a(0.75F, 2.25F)}: the Praetor's box. */
    public static final float WIDTH = 0.75F;
    public static final float HEIGHT = 2.25F;

    /**
     * {@code ConfigEntities.java:115} — tracking 64 blocks (1.21.1 counts chunks: 4),
     * update interval 3 ticks, velocity updates true. The knight's triple
     * ({@code CultistAiLogic.TRACK_RANGE_CHUNKS}, the same 64/3/true at
     * {@code ConfigEntities.java:111}).
     */
    public static final int TRACK_RANGE_BLOCKS = 64;
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    /** {@code :66} — {@code field_70728_aV = 40}: overwrites the boss base's 50. */
    public static final int EXPERIENCE = 40;

    // ---------------------------------------------------------------- attributes (:70-75)

    /**
     * {@code :72-74} — {@code func_110147_ax}: speed 0.32 ({@code field_111263_d}),
     * maxHealth 125.0 ({@code field_111267_a}), attack 5.0 ({@code field_111264_e}).
     * The {@code super} call carries the boss base's knockback 0.95 and follow 40.0
     * ({@code EntityThaumcraftBoss.java:53-54}).
     */
    public static final double MOVEMENT_SPEED = 0.32;
    public static final double MAX_HEALTH = 125.0;
    public static final double ATTACK_DAMAGE = 5.0;

    /** The leader's attribute pass: the boss base's pair plus the leader's three. */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(
                        net.minecraft.world.entity.monster.Monster.createMonsterAttributes())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)                         // :72
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)                                 // :73
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);                          // :74
    }

    // ---------------------------------------------------------------- goals (:57-65)

    public static final int GOAL_SLOT_SWIM = 0;                                         // :57
    public static final int GOAL_SLOT_RANGED = 2;                                       // :58
    public static final int GOAL_SLOT_MELEE = 3;                                        // :59
    public static final int GOAL_SLOT_RESTRICTION = 6;                                  // :60
    public static final int GOAL_SLOT_WANDER = 7;                                       // :61
    public static final int GOAL_SLOT_WATCH = 8;                                        // :62
    public static final int GOAL_SLOT_LOOK_IDLE = 8;                                    // :63
    public static final int TARGET_SLOT_HURT = 1;                                       // :64
    public static final int TARGET_SLOT_PLAYER = 2;                                     // :65

    /**
     * {@code :58} — {@code AILongRangeAttack(this, 16.0, 1.0, 30, 40, 24.0F)}. The goal
     * class is Thaumcraft's own ({@code ai/combat/AILongRangeAttack.java}), ported
     * by the cleric lane as {@code ai/cultist/AILongRangeAttack} — not vanilla's
     * {@code RangedAttackGoal}.
     */
    public static final double RANGED_MIN_DISTANCE = 16.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 30;
    public static final int RANGED_INTERVAL_MAX = 40;
    public static final float RANGED_RANGE = 24.0F;

    /**
     * {@code :59} — {@code AIAttackOnCollide(this, EntityLivingBase.class, 1.1, false)}:
     * Thaumcraft's melee goal ({@code ai/combat/AIAttackOnCollide.java}) with the
     * {@code classTarget} admission; 1.21.1's {@code EntityLivingBase} twin is
     * {@code LivingEntity.class}, and the shipped port is the vanilla
     * {@code MeleeAttackGoal} plus the class filter (see that class's javadoc).
     */
    public static final double MELEE_SPEED = 1.1;
    public static final boolean MELEE_LONG_MEMORY = false;

    /** {@code :60-61} — {@code EntityAIMoveTowardsRestriction(0.8)}, {@code EntityAIWander(0.8)}. */
    public static final double RESTRICTION_SPEED = 0.8;
    public static final double WANDER_SPEED = 0.8;

    /**
     * {@code :62} — {@code EntityAIWatchClosest(this, EntityPlayer.class, 8.0F)}. The
     * 1.21.1 twin {@code LookAtPlayerGoal} takes the family's watch probability
     * ({@code CultistAiLogic.WATCH_PLAYER_PROBABILITY} = 0.02F) — the shipped pair,
     * as the knight installed at {@code Knight:32}.
     */
    public static final float WATCH_PLAYER_RANGE = 8.0F;
    public static final float WATCH_PLAYER_PROBABILITY =
            com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic
                    .WATCH_PLAYER_PROBABILITY;

    /** {@code :64} — {@code AICultistHurtByTarget(this, true)}: the {@code true} is the
     *  agressors/calls-for-help flag the family ships ({@code CultistAiLogic.IS_AGRESSORS}). */
    public static final boolean HURT_IS_AGRESSORS = true;

    /** {@code :65} — {@code EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true)}:
     *  the {@code 0} is the sort type and {@code true} the must-see flag; 1.21.1's
     *  {@code NearestAttackableTargetGoal} carries the must-see flag (the knight's
     *  shipping form at {@code Knight:35}), and 1.7.10's hard-wired 10-tick scan
     *  interval is the target goal's own default. */
    public static final int TARGET_SORT_TYPE = 0;
    public static final boolean TARGET_MUST_SEE = true;

    // ---------------------------------------------------------------- titles (:36-52, :80, :83-99, :101-111)

    /**
     * {@code :36-52} — the fifteen praetor titles, verbatim, in the original's order.
     */
    public static final String[] TITLES = {
        "Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse",
        "Dodorol", "Ebardo", "Ferrando", "Fertus", "Guillen", "Larpe", "Obano", "Zelipe",
    };

    /** {@code :80} — {@code func_70096_w().addObject(16, (byte) 0)}: the watched title byte. */
    public static final int TITLE_WATCHER_INDEX = 16;
    /** {@code :104} / {@code :110} — the NBT key: the whole watched byte is written. */
    public static final String NBT_TITLE = "title";

    /** {@code :160} — {@code setTitle(rand.nextInt(titles.length))}: the spawn roll. */
    public static int titleFor(final int roll) {
        return Math.floorMod(roll, TITLES.length);
    }

    /**
     * {@code :83-91} — {@code generateName}: reads the {@code tc.mobmod} champion
     * attribute and, when its value is {@code >= 0}, formats
     * {@code entity.Thaumcraft.CultistLeader.name} (the row's display column
     * {@code Praetor %s the %d}) with the title and the champion mod name.
     *
     * <p>The champion attribute ({@code EntityUtils.CHAMPION_MOD}) is NOT registered
     * by the port and 21.1.259 has no {@code generateName} seam on {@code Mob}
     * (javap: no such method), so no name is ever set on spawn and the entity shows
     * the plain registry name — the row's {@code entity.thaumcraft.cultistleader}
     * lang key, "Praetor". Recorded deviation; the rule ships pinned here.</p>
     */
    public static boolean championNameApplies(final int championAttributeValue) {
        return championAttributeValue >= 0;                                             // :86
    }

    /** {@code :88} — {@code String.format(lang, getTitle(), ChampionModifier.mods[t].getModNameLocalized())}. */
    public static String nameFor(final int title, final String championModName) {
        return String.format("Praetor %s the %s", TITLES[titleFor(title)], championModName);
    }

    // ---------------------------------------------------------------- equipment (:113-132)

    /**
     * {@code :115-118} — helm / chest / legs = the cultist leader plate, boots = the
     * cultist boots; {@code :119-123} — weapon {@code itemSwordVoid} on EASY, else
     * {@code itemSwordCrimson}. Every one of these items is unregistered content
     * (their ledger rows are {@code not_started}), so the pass ships gated: each
     * branch lights up when its item lands, with no invented stand-in — the knight
     * lane's BD-68 shape.
     */
    public static final ResourceLocation HELMET_LEADER_PLATE =
            ResourceLocation.parse("thaumcraft:helmcultistleaderplate");
    public static final ResourceLocation CHEST_LEADER_PLATE =
            ResourceLocation.parse("thaumcraft:chestcultistleaderplate");
    public static final ResourceLocation LEGS_LEADER_PLATE =
            ResourceLocation.parse("thaumcraft:legscultistleaderplate");
    public static final ResourceLocation BOOTS_CULTIST =
            ResourceLocation.parse("thaumcraft:bootscultist");
    public static final ResourceLocation SWORD_VOID =
            ResourceLocation.parse("thaumcraft:swordvoid");
    public static final ResourceLocation SWORD_CRIMSON =
            ResourceLocation.parse("thaumcraft:swordcrimson");

    /**
     * The leader's plate set ({@code :115-118}) in 1.7.10's slot ordinals
     * ({@code func_70062_b}: 0 mainhand, 1 boots, 2 legs, 3 chest, 4 helmet — the
     * family's {@code CultistAiLogic.SLOT_*} layout).
     */
    public static Map<Integer, ResourceLocation> leaderPlate() {
        final Map<Integer, ResourceLocation> plate = new LinkedHashMap<>();
        plate.put(com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic.SLOT_HELMET,
                HELMET_LEADER_PLATE);                                                   // :115
        plate.put(com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic.SLOT_CHEST,
                CHEST_LEADER_PLATE);                                                    // :116
        plate.put(com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic.SLOT_LEGS,
                LEGS_LEADER_PLATE);                                                     // :117
        plate.put(com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic.SLOT_BOOTS,
                BOOTS_CULTIST);                                                         // :118
        return plate;
    }

    /** {@code :119-123} — {@code itemSwordVoid} on EASY, {@code itemSwordCrimson} on anything else. */
    public static ResourceLocation weaponFor(final boolean easy) {
        return easy ? SWORD_VOID : SWORD_CRIMSON;
    }

    /**
     * {@code :126-132} — {@code func_82162_bC}: with a held item
     * ({@code func_70694_bm() != null}), on {@code rand.nextFloat() < 0.5F * light}
     * ({@code :129}, the light at the mob's position — 1.7.10's
     * {@code func_147462_b}, the same normalized neighbour light as the knight lane's
     * raw-brightness read), the weapon is enchanted at
     * {@code (int)(7.0F + light * rand.nextInt(22))} ({@code :130}). The one-enchantment
     * shape is the family's BD-93 reading: 1.21.1 ships
     * {@code EnchantmentHelper.enchantItem} over the mob-spawn tag set.
     */
    public static final float ENCHANT_LIGHT_FACTOR = 0.5F;
    public static final float ENCHANT_BASE_LEVEL = 7.0F;
    public static final int ENCHANT_LEVEL_ROLL = 22;

    public static boolean enchantFires(final float roll, final float light) {
        return roll < ENCHANT_LIGHT_FACTOR * light;                                     // :129
    }

    public static int enchantLevel(final float light, final int nextInt) {
        return (int) (ENCHANT_BASE_LEVEL + light * nextInt);                            // :130
    }

    // ---------------------------------------------------------------- faction (:134-141)

    /**
     * {@code :134-137} — {@code func_142014_c(EntityLivingBase)}: the leader counts
     * cultists (knight and cleric ride that class) and other leaders as faction
     * enemies, verbatim — the expression alone, with no vanilla fall-through.
     */
    public static boolean isEnemyFaction(final Object target) {
        return target instanceof com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist
                || target instanceof EntityCultistLeader;                               // :136
    }

    /**
     * {@code :139-141} — {@code func_70686_a(Class)}: cleric, leader and knight
     * classes are refused outright; anything else takes 1.7.10's
     * {@code EntityLivingBase} answer (true).
     */
    public static boolean isTargetableClass(final Class<?> clazz) {
        return !(com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric.class
                        .isAssignableFrom(clazz)
                || EntityCultistLeader.class.isAssignableFrom(clazz)
                || com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight.class
                        .isAssignableFrom(clazz))
                && !com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist.class
                        .isAssignableFrom(clazz);                                       // :140
    }

    // ---------------------------------------------------------------- drops (:143-154, and the base's gate)

    /**
     * {@code :143-145} — {@code func_146068_u()} returns {@code Item.getIdFromItem(0)}:
     * air. 1.7.10's consumer is the renderer's hand-item hook (TC-M7-03-s is the
     * renderer lane), so nothing ships and the row records the seam.
     */

    /**
     * {@code :147-150} — {@code func_70628_a(boolean, int)} ({@code dropCustomDeathLoot}):
     * the lootbag, meta 2, at 1.5F. {@code itemLootbag} is unregistered content
     * ({@code not_started}) AND the boss base's {@code func_70692_ba()} false
     * ({@code EntityThaumcraftBoss:205}) gates the whole death-loot block in both
     * worlds — the drop is unreachable by the original's own flag, so the rule
     * ships pinned and the gate ships as the original wrote it.
     */
    public static final ResourceLocation LOOTBAG =
            ResourceLocation.parse("thaumcraft:itemlootbag");
    public static final int LOOTBAG_META = 2;
    public static final float LOOTBAG_CHANCE = 1.5F;

    // ---------------------------------------------------------------- aura (:164-178)

    /** {@code :168} — {@code EntityUtils.getEntitiesInRange(..., EntityCultist.class, 8.0)}. */
    public static final double AURA_RADIUS = 8.0;
    /** {@code :172-173} — haste ({@code Potion.field_76428_l}) 60 ticks, amplifier 1. */
    public static final int AURA_HASTE_TICKS = 60;
    public static final int AURA_HASTE_AMPLIFIER = 1;

    /** {@code :172} — {@code !cultist.func_82165_m(haste)}: the aura fires only when absent. */
    public static boolean auraApplies(final boolean targetAlreadyHasted) {
        return !targetAlreadyHasted;                                                    // :172
    }

    // ---------------------------------------------------------------- ranged (:180-201)

    /** {@code :191} — {@code new EntityGolemOrb(world, this, target, true)}: the red flag. */
    public static final boolean ORB_IS_RED = true;

    /**
     * {@code :192-193} — the spawn nudge, verbatim: {@code x += motionX / 2} and
     * {@code z += motionY / 2} ({@code field_70159_w} then {@code field_70179_y}).
     * The z component rides the Y motion, the original's own pairing (the orb's
     * motion is zero at construction — {@code EntityThrowable} sets none — so the
     * nudge is a no-op, exactly as in 1.7.10, and ships as written).
     */
    public static final double ORB_NUDGE_FACTOR = 2.0;

    /** {@code :188-189} / {@code :195-197} — the aim vector, 30/30 look roll. */
    public static final int ORB_LOOK_ROLL = 30;

    /** {@code :198} — {@code func_70186_c(d0, d1 + 2.0, d2, 0.66F, 3.0F)}. */
    public static final double ORB_AIM_LIFT = 2.0;
    public static final float ORB_SPEED = 0.66F;
    public static final float ORB_SPREAD = 3.0F;

    /** {@code :199} — {@code playSound("thaumcraft:egattack", 1.0F, 1.0F + rand*0.1F)}. */
    public static final float ORB_SOUND_VOLUME = 1.0F;
    public static final float ORB_SOUND_PITCH_BASE = 1.0F;
    public static final float ORB_SOUND_PITCH_JITTER = 0.1F;
}
