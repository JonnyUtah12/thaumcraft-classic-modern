package com.jonnyutah12.thaumcraftclassic.gametest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.CultistPortalAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistPortal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBanner;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.CultistLeaderAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistLeader;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityThaumcraftBoss;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.ThaumcraftBossAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;

import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-06-s-c5-1: the shared boss body and the Crimson Praetor, measured in world.
 *
 * <p>Twelve probes on {@code thaumcraft:cultistleader} and the unregistered
 * {@code EntityThaumcraftBoss} base: the registration row, the box and tracking,
 * the attribute set (health 125 / attack 5 / speed 0.32 over the base's knockback
 * 0.95 and follow 40), the nine-row goal table at the original slots, the
 * spawn-in invulnerability, the 200-tick rage and the 35.0F clamp, the red
 * GolemOrb seam and its faction gate, the 8.0-block haste aura, the title byte
 * and its NBT, the spawn pass (24-block home, gated equipment, title roll) and
 * the death gate. Conventions: the pech card's pad discipline (the {@code empty}
 * template has no floor — a standing fixture rides noGravity and clearFire), the
 * knight card's mock-player placement (makeMockPlayer does NOT place the player),
 * and the cleric card's goal-table read-back through
 * {@code GoalSelector#getAvailableGoals()}. Every assertion quotes the original
 * line it pins.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossMobGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("BossMobGameTest");

    private BossMobGameTest() {
    }

    // ------------------------------------------------------------------ helpers

    /** The registry key is the contract: an absent id resolves to the default entry. */
    private static EntityType<?> registered(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ")");
        return type;
    }

    private static void absent(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(!id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " must NOT be registered but its key resolves to "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** The praetor factory — the cleric card's shape: pad fixtures ride noGravity
     *  and clearFire (the {@code empty} template's origin column is void). */
    private static EntityCultistLeader leader(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistleader").create(level);
        helper.assertTrue(spawned instanceof EntityCultistLeader,
                "thaumcraft:cultistleader did not create an EntityCultistLeader");
        final EntityCultistLeader mob = (EntityCultistLeader) spawned;
        final BlockPos at = helper.absolutePos(BlockPos.ZERO).offset(1, 3, 1);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /** Run the mob's own spawn pass (the harness does not call it on a placed mob). */
    private static EntityCultistLeader spawned(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(mob.blockPosition()),    // family seam
                MobSpawnType.NATURAL, null);
        return mob;
    }

    private static EntityCultistCleric cleric(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistcleric").create(level);
        final EntityCultistCleric mob = (EntityCultistCleric) spawned;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    private static Player mockAt(final GameTestHelper helper, final LivingEntity at) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static List<WrappedGoal> goals(final EntityCultistLeader mob, final boolean target) {
        final List<WrappedGoal> out = new ArrayList<>();
        for (final WrappedGoal g : (target ? mob.targetSelector : mob.goalSelector).getAvailableGoals()) {
            out.add(g);
        }
        return out;
    }

    private static boolean has(final Iterable<WrappedGoal> goals, final Class<?> goal, final int slot) {
        for (final WrappedGoal wrapped : goals) {
            if (goal.isInstance(wrapped.getGoal()) && wrapped.getPriority() == slot) {
                return true;
            }
        }
        return false;
    }

    private static String describe(final Iterable<WrappedGoal> goals) {
        final StringBuilder sb = new StringBuilder("goal set: ");
        for (final WrappedGoal g : goals) {
            sb.append(g.getPriority()).append('=').append(g.getGoal().getClass().getSimpleName()).append(' ');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ roster

    /**
     * {@code ConfigEntities.java:115} — {@code registerModEntity(EntityCultistLeader.class,
     * "CultistLeader", 29, instance, 64, 3, true)}: the box is {@code :56}'s
     * 0.75 x 2.25, the tracking is 64 blocks = 4 chunks, the update interval is 3
     * and velocity updates ship true.
     */
    @GameTest(template = "empty")
    public static void theLeaderIsRegisteredWithTheOriginalBoxAndTracking(final GameTestHelper helper) {
        final EntityType<?> type = registered(helper, "cultistleader");
        helper.assertTrue(Math.abs(type.getDimensions()
                .width() - CultistLeaderAiLogic.WIDTH) < 1.0E-4,
                ":56 — the box width must be 0.75, got " + type.getDimensions().width());
        helper.assertTrue(Math.abs(type.getDimensions()
                .height() - CultistLeaderAiLogic.HEIGHT) < 1.0E-4,
                ":56 — the box height must be 2.25, got " + type.getDimensions().height());
        helper.assertTrue(type.clientTrackingRange() == CultistLeaderAiLogic.TRACK_RANGE_CHUNKS,
                ":115 — tracking 64 blocks = 4 chunks, got " + type.clientTrackingRange());
        helper.assertTrue(type.updateInterval() == CultistLeaderAiLogic.UPDATE_INTERVAL,
                ":115 — update interval 3, got " + type.updateInterval());
        helper.succeed();
    }

    /**
     * The shared base is deliberately unregistered: {@code ConfigEntities.java}
     * registers Leader 29 ({@code :115}), Portal 30 ({@code :117}), Golem 31
     * ({@code :119}) and Warden 26 ({@code :109}) and never the base, and the base
     * class is abstract — there is no row and no id.
     */
    @GameTest(template = "empty")
    public static void theSharedBossBaseIsNotRegistered(final GameTestHelper helper) {
        absent(helper, "thaumcraftboss");
        absent(helper, "thaumcraft_boss");
        helper.assertTrue(java.lang.reflect.Modifier.isAbstract(EntityThaumcraftBoss.class
                        .getModifiers()),
                "EntityThaumcraftBoss must stay abstract: the original registers no base");
        helper.succeed();
    }

    /**
     * {@code :70-75} — health 125.0, attack 5.0, speed 0.32, over the base's
     * knockback resistance 0.95 ({@code EntityThaumcraftBoss:53}) and follow
     * range 40.0 ({@code :54}).
     */
    @GameTest(template = "empty")
    public static void theLeaderCarriesTheOriginalAttributes(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        helper.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH).getValue() == 125.0,
                ":73 — maxHealth must be 125.0, got "
                        + mob.getAttribute(Attributes.MAX_HEALTH).getValue());
        helper.assertTrue(mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue() == 5.0,
                ":74 — attack must be 5.0, got "
                        + mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
        helper.assertTrue(Math.abs(mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue() - 0.32)
                < 1.0E-6, ":72 — speed must be 0.32, got "
                        + mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
        helper.assertTrue(Math.abs(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue()
                - 0.95) < 1.0E-6, "base :53 — knockback resistance must be 0.95, got "
                        + mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue());
        helper.assertTrue(mob.getAttribute(Attributes.FOLLOW_RANGE).getValue() == 40.0,
                "base :54 — follow range must be 40.0, got "
                        + mob.getAttribute(Attributes.FOLLOW_RANGE).getValue());
        helper.assertTrue(mob.getExperienceReward(helper.getLevel(), null) == CultistLeaderAiLogic.EXPERIENCE,
                ":66 — the xp reward must be 40, got " + mob.getExperienceReward(helper.getLevel(), null));
        mob.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ goal table

    /**
     * {@code :57-65} — the exact table: 0 Swimming, 2 {@code AILongRangeAttack}
     * (TC's class, 16.0 / 1.0 / 30 / 40 / 24.0F), 3 {@code AIAttackOnCollide}
     * (TC's class, filter {@code EntityLivingBase}, 1.1, false), 6
     * MoveTowardsRestriction 0.8, 7 Wander 0.8, the slot-8 pair (BD-112: 1.7.10
     * nets the LookIdle, 21.1.259 keeps both at the slot), gang target 1 with
     * agressors TRUE, player scan 2. The Monster-inherited goals the original
     * never had are stripped (the override does not call super) — the movement
     * set measures SEVEN.
     */
    @GameTest(template = "empty")
    public static void theLeaderGoalSetIsTheOriginalTableAtTheOriginalSlots(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        final List<WrappedGoal> movement = goals(mob, false);
        final List<WrappedGoal> targets = goals(mob, true);
        helper.assertTrue(has(movement, FloatGoal.class, CultistLeaderAiLogic.GOAL_SLOT_SWIM),
                ":57 — the swim goal at slot 0; " + describe(movement));
        helper.assertTrue(has(movement, AILongRangeAttack.class, CultistLeaderAiLogic.GOAL_SLOT_RANGED),
                ":58 — AILongRangeAttack (TC's goal) at slot 2; " + describe(movement));
        helper.assertTrue(has(movement, AIAttackOnCollide.class, CultistLeaderAiLogic.GOAL_SLOT_MELEE),
                ":59 — AIAttackOnCollide (TC's goal) at slot 3; " + describe(movement));
        helper.assertTrue(has(movement, MoveTowardsRestrictionGoal.class,
                        CultistLeaderAiLogic.GOAL_SLOT_RESTRICTION)
                        && has(movement, RandomStrollGoal.class, CultistLeaderAiLogic.GOAL_SLOT_WANDER)
                        && has(movement, LookAtPlayerGoal.class, CultistLeaderAiLogic.GOAL_SLOT_WATCH)
                        && has(movement, RandomLookAroundGoal.class, CultistLeaderAiLogic.GOAL_SLOT_LOOK_IDLE),
                ":60-63 — restriction 6, wander 7, the slot-8 pair; " + describe(movement));
        helper.assertTrue(movement.size() == 7,
                ":57-63 — the port ships 7 movement goals (the :62/:63 pair rides slot 8 and "
                        + "1.21.1's GoalSelector keeps both — BD-112 — where 1.7.10's TreeMap netted "
                        + "six; no Monster-inherited goal is present); " + describe(movement));
        helper.assertTrue(has(targets, AICultistHurtByTarget.class, CultistLeaderAiLogic.TARGET_SLOT_HURT)
                        && has(targets, NearestAttackableTargetGoal.class,
                                CultistLeaderAiLogic.TARGET_SLOT_PLAYER),
                ":64-65 — the gang call at 1, the player scan at 2; " + describe(targets));
        helper.assertTrue(targets.size() == 2, ":64-65 — exactly two target goals; " + describe(targets));
        // :58 — the ranged goal's own figures, read back off the installed goal.
        final AILongRangeAttack ranged = (AILongRangeAttack) movement.stream()
                .filter(g -> g.getGoal() instanceof AILongRangeAttack)
                .findFirst().orElseThrow().getGoal();
        helper.assertTrue(ranged.getMinDistance() == CultistLeaderAiLogic.RANGED_MIN_DISTANCE,
                ":58 — the installed minDistance must be 16.0, got " + ranged.getMinDistance());
        helper.assertTrue(CultistLeaderAiLogic.RANGED_SPEED == 1.0
                        && CultistLeaderAiLogic.RANGED_INTERVAL_MIN == 30
                        && CultistLeaderAiLogic.RANGED_INTERVAL_MAX == 40
                        && CultistLeaderAiLogic.RANGED_RANGE == 24.0F,
                ":58 — the ctor's four companions must be 1.0 / 30 / 40 / 24.0F");
        // :59 — the melee goal is TC's class with the EntityLivingBase filter.
        final AIAttackOnCollide melee = (AIAttackOnCollide) movement.stream()
                .filter(g -> g.getGoal() instanceof AIAttackOnCollide)
                .findFirst().orElseThrow().getGoal();
        helper.assertTrue(melee.targetClass() == LivingEntity.class,
                ":59 — the classTarget is EntityLivingBase → LivingEntity.class, got "
                        + melee.targetClass());
        // :64 — the gang call's agressors flag.
        final AICultistHurtByTarget gang = (AICultistHurtByTarget) targets.stream()
                .filter(g -> g.getGoal() instanceof AICultistHurtByTarget)
                .findFirst().orElseThrow().getGoal();
        helper.assertTrue(gang.isAgressors(), ":64 — AICultistHurtByTarget(this, true)");
        mob.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ phases

    /**
     * {@code :172-173} + {@code :63-65} — while the spawn-in timer runs the boss
     * is invulnerable and the family's melee goal refuses to tick; the timer
     * decrements one per tick and both gates open at 0. The heal leg ({@code
     * :116-118}, every 30 ticks 1.0F) rides the same tick body.
     */
    @GameTest(template = "empty")
    public static void theSpawnInPhaseInvulneratesTheBossAndGatesTheFamilyGoal(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.setSpawnTimer(100);
        helper.assertTrue(mob.goalsMayTick() == false,
                "base :63 — goals must not tick during materialisation");
        helper.assertTrue(mob.isInvulnerableTo(helper.getLevel().damageSources().generic()),
                "base :172-173 — a spawning boss is invulnerable");
        mob.aiStep();
        helper.assertTrue(mob.getSpawnTimer() == 99,
                "base :91-93 — the timer decrements one per tick, got " + mob.getSpawnTimer());
        mob.setSpawnTimer(0);
        helper.assertTrue(mob.goalsMayTick(), "base :63 — the goals open at timer 0");
        helper.assertTrue(!mob.isInvulnerableTo(helper.getLevel().damageSources().generic()),
                "base :172-173 — a settled boss is vulnerable");
        mob.discard();
        helper.succeed();
    }

    /**
     * {@code :222-255} — a 40.0 hit with anger 0: rage 200 ticks, haste amplifier
     * {@code (int)(40/15)} = 2, resistance and strength {@code (int)(40/40)} = 1,
     * the damage CLAMPED to 35.0 (health 125 → 90), and the aggro entry carries the
     * raw 40 ({@code :222-232} records the RAW amount before the clamp). The second
     * 40.0 hit (anger running) is clamped with no re-rage: health 55, aggro 80.
     */
    @GameTest(template = "empty")
    public static void theDamageLegRagesOnceThenClampsEveryBigHit(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.setSpawnTimer(0);
        final Player striker = mockAt(helper, mob);
        final var source = helper.getLevel().damageSources().playerAttack(striker);
        helper.assertTrue(mob.hurt(source, 40.0F), ":222 — the hit must land");
        helper.assertTrue(Math.abs(mob.getHealth() - 104.0F) < 1.0E-3,
                ":250 — the 40.0 hit clamps to 35.0 and 35.0 then rides the resistance the SAME leg "
                        + "applied (:238, amp 1 = 40%): 125 - 35*0.6 = 104, got " + mob.getHealth());
        helper.assertTrue(mob.getAnger() == 200, ":240 — anger must be 200, got " + mob.getAnger());
        helper.assertTrue(mob.hasEffect(MobEffects.DIG_SPEED)
                        && mob.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 2,
                ":237 — haste (int)(40/15) = 2");
        helper.assertTrue(mob.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                        && mob.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 1,
                ":238 — resistance (int)(40/40) = 1");
        helper.assertTrue(mob.hasEffect(MobEffects.DAMAGE_BOOST)
                        && mob.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 1,
                ":239 — strength (int)(40/40) = 1");
        helper.assertTrue(mob.aggroMap().get(striker.getId()) == 40,
                ":222-232 — the RAW 40 (not the clamped 35) rides the aggro map, got "
                        + mob.aggroMap().get(striker.getId()));
        mob.invulnerableTime = 0;                                                 // vanilla's 10-tick invuln window
        mob.hurt(source, 40.0F);
        helper.assertTrue(Math.abs(mob.getHealth() - 83.0F) < 1.0E-3,
                ":250 — the second 40.0 hit clamps to 35.0 and the running resistance (amp 1, 40%) "
                        + "takes 35 -> 21 (104 - 21 = 83), got " + mob.getHealth());
        helper.assertTrue(mob.aggroMap().get(striker.getId()) == 80,
                ":227-229 — the aggro entry accumulates (40 + 40), got "
                        + mob.aggroMap().get(striker.getId()));
        mob.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ faction + ranged

    /**
     * {@code :135-141} — the pair: cultists and leaders are faction kin (the
     * {@code canAttack} predicate answers TRUE for kin), and cleric / knight /
     * leader are refused as target types ({@code canAttackType}).
     */
    @GameTest(template = "empty")
    public static void theFactionPairIsTheOriginals(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        final EntityCultistCleric kin = cleric(helper, helper.absolutePos(BlockPos.ZERO).offset(3, 3, 1));
        helper.assertTrue(mob.canAttack(kin),
                ":136 — a leader counts a cleric (an EntityCultist) as faction kin");
        final EntityCultistLeader second = leader(helper);
        helper.assertTrue(mob.canAttack(second), ":136 — and another leader");
        helper.assertTrue(!mob.canAttackType(kin.getType()),
                ":139-141 — the cleric class is refused as a target type");
        helper.assertTrue(!mob.canAttackType(second.getType()),
                ":139-141 — the leader class is refused as a target type");
        final Player player = mockAt(helper, mob);
        helper.assertTrue(mob.canAttackType(player.getType()),
                ":139-141 — anything else takes the super answer: the player passes");
        kin.discard();
        second.discard();
        mob.discard();
        helper.succeed();
    }

    /**
     * {@code :180-201} — the ranged seam: with a player target,
     * {@code performRangedAttack} builds the RED {@code EntityGolemOrb}
     * ({@code :191}, the flag the cleric lane pinned per caller) and puts it in
     * the level owned by the leader. The {@code :181} gate is the class twin
     * ({@code func_70685_l} rides {@code func_70686_a}, {@code :139-141}): a
     * cleric target fires NOTHING.
     */
    @GameTest(template = "empty")
    public static void theRangedSeamFiresTheRedGolemOrbAndRefusesKin(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.setSpawnTimer(0);
        final AABB scan = mob.getBoundingBox().inflate(16.0);
        final Player player = mockAt(helper, mob);
        mob.performRangedAttack(player, 1.0F);
        final List<EntityGolemOrb> orbs =
                helper.getLevel().getEntitiesOfClass(EntityGolemOrb.class, scan);
        helper.assertTrue(orbs.size() == 1,
                ":191-200 — the player target must produce exactly one orb, got " + orbs.size());
        helper.assertTrue(orbs.get(0).isRed(),
                ":191 — the leader's orb rides the TRUE (red) flag");
        helper.assertTrue(orbs.get(0).getOwner() == mob,
                ":191 — the orb is owned by the leader");
        final EntityCultistCleric kin = cleric(helper, helper.absolutePos(BlockPos.ZERO).offset(4, 3, 1));
        mob.performRangedAttack(kin, 1.0F);
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(EntityGolemOrb.class, scan).size() == 1,
                ":181 + :139-141 — the cleric target fires nothing");
        kin.discard();
        orbs.forEach(orb -> orb.discard());
        mob.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ aura

    /**
     * {@code :164-178} — every EntityCultist inside 8.0 of the leader's position
     * gets haste (60 ticks, amplifier 1) while it has none; kin beyond the radius
     * get nothing, and a kin that already has haste is NOT re-timed
     * ({@code :172}). The scan excludes the caller by id ({@code :168}).
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theAuraHastesKinInsideEightBlocksAndNobodyElse(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.setSpawnTimer(0);
        // The :165 gate is (this.tickCount + this.getId()) % 40 == 0 — aim the field
        // so the next aiStep lands on the gate (the harness ticks entities at tick
        // boundaries; the probe body drives aiStep directly).
        mob.tickCount = 40 - (mob.getId() % 40);
        final BlockPos origin = mob.blockPosition();
        final EntityCultistCleric inside = cleric(helper, origin.offset(3, 0, 0));
        final EntityCultistCleric outside = cleric(helper, origin.offset(12, 0, 0));
        final EntityCultistCleric hasted = cleric(helper, origin.offset(2, 0, 0));
        hasted.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20, 0));
        for (int tick = 0; tick < 4; tick++) {
            mob.aiStep();
        }
        helper.assertTrue(inside.hasEffect(MobEffects.DIG_SPEED)
                        && inside.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 1
                        && inside.getEffect(MobEffects.DIG_SPEED).getDuration()
                                >= CultistLeaderAiLogic.AURA_HASTE_TICKS - 4,
                ":172-173 — the kin inside 8.0 must run haste 60 @ amp 1, got "
                        + inside.getEffect(MobEffects.DIG_SPEED));
        helper.assertTrue(!outside.hasEffect(MobEffects.DIG_SPEED),
                ":168 — the kin 12 blocks out is outside the scan");
        final int kept = hasted.getEffect(MobEffects.DIG_SPEED).getDuration();
        helper.assertTrue(kept < CultistLeaderAiLogic.AURA_HASTE_TICKS,
                ":172 — an already-hasted kin is not re-timed, duration kept " + kept);
        inside.discard();
        outside.discard();
        hasted.discard();
        mob.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ spawn pass

    /**
     * {@code :156-162} + base {@code :72-75} — the spawn pass: the boss home lands
     * on the spawn cell at radius 24, the title byte rolls inside the 15-title
     * range, and the gated equipment leaves every slot EMPTY (the leader plate,
     * cultist boots, void and crimson sword are unregistered content, BD-68's
     * shape; the knight's own plate is unregistered too, so the kin arrives
     * unarmoured).
     */
    @GameTest(template = "empty")
    public static void theSpawnPassHomesAndRollsTheTitleAndEquipsNothing(final GameTestHelper helper) {
        final EntityCultistLeader mob = spawned(helper);
        helper.assertTrue(mob.hasRestriction()
                        && mob.getRestrictCenter().equals(mob.blockPosition())
                        && mob.getRestrictRadius() == ThaumcraftBossAiLogic.HOME_RADIUS,
                "base :72-75 — the home is the spawn cell at radius 24, got "
                        + mob.getRestrictCenter() + " r" + mob.getRestrictRadius());
        final int title = mob.getTitle();
        helper.assertTrue(title >= 0 && title < CultistLeaderAiLogic.TITLES.length,
                ":160 — the title roll must sit inside 0..14, got " + title);
        for (final EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            helper.assertTrue(mob.getItemBySlot(slot).isEmpty(),
                    ":114-124 — " + slot + " stays empty: every item is unregistered (BD-68 gate)");
        }
        mob.discard();
        helper.succeed();
    }

    /**
     * {@code :80} + {@code :101-111} — the title byte round-trips through the NBT
     * key {@code title}, and the name seam stays a seam: 21.1.259 has no
     * {@code generateName} hook and the champion attribute is unregistered, so a
     * fresh spawn carries NO custom name and the display name answers the plain
     * lang key (the row's label, {@code entity.thaumcraft.cultistleader}).
     */
    @GameTest(template = "empty")
    public static void theTitleRoundTripsThroughNbtAndThePlainNameShows(final GameTestHelper helper) {
        final EntityCultistLeader mob = spawned(helper);
        mob.setTitle(7);
        final CompoundTag tag = new CompoundTag();
        mob.addAdditionalSaveData(tag);
        helper.assertTrue(tag.getByte(CultistLeaderAiLogic.NBT_TITLE) == 7,
                ":104 — the whole watched byte rides the NBT key 'title'");
        final EntityCultistLeader reloaded =
                (EntityCultistLeader) registered(helper, "cultistleader").create(helper.getLevel());
        reloaded.readAdditionalSaveData(tag);
        helper.assertTrue(reloaded.getTitle() == 7, ":110 — the byte reads back");
        helper.assertTrue(!mob.hasCustomName(),
                ":83-91 — no name is set while the champion attribute is absent (deviation)");
        helper.assertTrue(mob.getDisplayName().getString().equals("Praetor %s the %s"),
                "the plain label is the lang key's answer, got " + mob.getDisplayName().getString());
        reloaded.discard();
        mob.discard();
        helper.succeed();
    }

    /**
     * {@code EntityThaumcraftBoss:205} — {@code func_70692_ba()} false gates the
     * whole death-loot block in both worlds, so a killed praetor leaves NO items:
     * the lootbag rule ({@code :147-150}) and the base's eldritch pair
     * ({@code :213-216}) are unreachable by the original's own flag (both items
     * are unregistered content on top of that).
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDeathGateLeavesNoLootBehind(final GameTestHelper helper) {
        final EntityCultistLeader mob = leader(helper);
        mob.setSpawnTimer(0);
        mob.setAnger(0);
        final Player striker = mockAt(helper, mob);
        for (int hit = 0; hit < 10 && !mob.isDeadOrDying(); hit++) {
            mob.invulnerableTime = 0;
            // 40.0F: the clamp arm (35.0) with the rage the FIRST hit applied (resistance
            // amp (int)(40/40) = 1, 40%) — 21 per hit, so ten hits walk 125 health down
            // through zero. A 1000.0F hit would rage resistance to amp 25 and land 0.
            mob.hurt(helper.getLevel().damageSources().playerAttack(striker), 40.0F);
        }
        helper.assertTrue(mob.isDeadOrDying(), ":205 — repeated hits land the kill");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        mob.getBoundingBox().inflate(8.0)).isEmpty(),
                ":205 — the loot gate leaves no ItemEntity behind (lootbag :149 and the "
                        + "base's :213-216 pair are gated, and the items are unregistered)");
        mob.discard();
        helper.succeed();
    }

    /** The census line: the facts a green leg does not print ride the log. */
    private static void census(final GameTestHelper helper, final String probe) {
        LOGGER.info("[TC-M6-06-boss] {} leg observed at dayTime={}", probe,
                helper.getLevel().getDayTime());
    }

    // ================================================================= TC-M6-06-s-c5-2
    //
    // EntityCultistPortal — thaumcraft:cultistportal (content-mapping row 328), the
    // Crimson Portal summoner. Every figure is cited to the 314-line reference
    // reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityCultistPortal.java
    // by line (:NNN). The mob extends EntityMob (Monster) directly, NOT the c5-1
    // EntityThaumcraftBoss base: the reference's header is {@code extends EntityMob}
    // and the base's phase counters, rage, aggro scan and chase-cut appear nowhere in
    // the reference's 314 lines. It carries the port's client boss-bar seam
    // (IBossDisplayData) on its own — the deviation the card asked for, recorded in
    // the ledger.

    /** {@code :18} — {@code setSize(1.5F, 3.0F)}, restated as a literal. */
    private static final float PORTAL_BOX_WIDTH = 1.5F;
    /** {@code :18} — the box height, restated as a literal. */
    private static final float PORTAL_BOX_HEIGHT = 3.0F;

    /**
     * The 2 x 3 x 2 cells the portal's own box straddles at its feet. The mob stands at
     * the cell centre (x + 0.5), so the 1.5-wide box spans x-0.25 .. x+1.25 → cells x-1
     * and x; the 3.0-tall box spans y .. y+3 → cells y, y+1, y+2.
     */
    private static final List<BlockPos> PORTAL_BODY_CELLS = portalCells(-1, 0, 0, 2, -1, 0);

    /**
     * {@code :229}, {@code :231} — the 5 x 1 x 5 cells the summon drop pass reads:
     * {@code x + nextFloat() - nextFloat()} spans x-1 .. x+1 about the centre, so the
     * entity box lands in cells x-2 .. x+2.
     */
    private static final List<BlockPos> PORTAL_SPAWN_CELLS = portalCells(-2, 2, 0, 0, -2, 2);

    /**
     * {@code :140-144} — the banner wall's four cells: ForgeDirection 2 north
     * (offsetZ -1), 3 south (+1), 4 west (offsetX -1, negated by the source's minus) and
     * 5 east (+1), each 6 blocks out, on the portal's own y.
     */
    private static final List<BlockPos> PORTAL_BANNER_CELLS = List.of(
            new BlockPos(0, 0, -6), new BlockPos(0, 0, 6),
            new BlockPos(6, 0, 0), new BlockPos(-6, 0, 0));

    /**
     * {@code :157}, {@code :160}, {@code :163}, {@code :166} — the facing each banner
     * carries, in loop order 2,3,4,5: north 8, south 0, west 12, east 4.
     */
    private static final int[] PORTAL_BANNER_FACINGS = {8, 0, 12, 4};

    /**
     * {@code :186-187} — the crate region half-width: {@code (int)x + nextInt(5) -
     * nextInt(5)} covers -4 .. 4 on both axes, so a 9 x 9 field at the portal's y.
     */
    private static final int CRATE_REGION_HALF = 4;

    /**
     * {@code :186-187} — the crate pick seeded, not sampled: seed 15 of 1.21.1's
     * {@code RandomSource} (1.7.10's LCG lineage) draws nextInt(5) as 4,3 then 0,1, so the
     * two axis picks are {@code -1} and {@code +1}. That cell is inside the origin chunk
     * for every origin the stride-6 grid can produce, which is what makes the leg
     * measurable at all (the same argument as the banner wall's chunk test).
     */
    private static final long CRATE_PICK_SEED = 15L;
    private static final BlockPos CRATE_PICK = new BlockPos(-1, 0, 1);

    private static List<BlockPos> portalCells(final int xFrom, final int xTo, final int yFrom,
            final int yTo, final int zFrom, final int zTo) {
        final List<BlockPos> out = new ArrayList<>();
        for (int x = xFrom; x <= xTo; x++) {
            for (int y = yFrom; y <= yTo; y++) {
                for (int z = zFrom; z <= zTo; z++) {
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * The pad cell the portal probes stand on, as a TEMPLATE-RELATIVE cell: the leader and
     * cleric factories put their mob at rel (1, 3, 1), and {@code GameTestHelper#setBlock}
     * takes template-relative coordinates, so every write in this section goes through
     * this cell.
     */
    private static final BlockPos PORTAL_REL = new BlockPos(1, 3, 1);

    /** The same cell, absolute — what the level, the entity maps and the census name. */
    private static BlockPos portalCell(final GameTestHelper helper) {
        return helper.absolutePos(BlockPos.ZERO).offset(PORTAL_REL);
    }

    /** The portal factory — the leader/cleric shape: pad fixtures ride noGravity. */
    private static EntityCultistPortal portal(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistportal").create(level);
        helper.assertTrue(spawned instanceof EntityCultistPortal,
                "thaumcraft:cultistportal did not create an EntityCultistPortal");
        final EntityCultistPortal mob = (EntityCultistPortal) spawned;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /** The knight fixture, on a cell the probe has claimed. */
    private static EntityCultistKnight knightAt(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistknight").create(level);
        final EntityCultistKnight mob = (EntityCultistKnight) spawned;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /** The praetor fixture — the class the summon SCAN must not admit. */
    private static EntityCultistLeader leaderAt(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistleader").create(level);
        final EntityCultistLeader mob = (EntityCultistLeader) spawned;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /**
     * The pad-claim idiom (docs/testing.md#orb-claim-covers-asserted-cells, minted by
     * t_b98f2601 over #sweep-hazard-set-is-scan-set and #scan-claim-covers-box-cells):
     * a probe that reads, writes or counts cells outside its own 1×1×1 template has to
     * TAKE those cells — blocks AND entities — at setup, from ONE list that also feeds
     * the asserts, and log {@code cleared N … swept M} as the channel census on every
     * leg. The stride-6 grid packs sixteen {@code empty} pads into a 24-wide footprint
     * and {@code StructureUtils#clearSpaceForStructure} clears exactly the template box,
     * so the grid's worldgen terrain wave (this lane's reds: {@code dirt},
     * {@code grass_block}) stands in the far cells the banner wall and the crate region
     * ride, and the neighbour pads leave mobs inside the 32.0 summon scan.
     */
    private static List<String> claimPortalCells(final GameTestHelper helper, final BlockPos at,
            final List<BlockPos> cells) {
        final ServerLevel level = helper.getLevel();
        final List<String> cleared = new ArrayList<>();
        final List<String> swept = new ArrayList<>();
        final List<String> unreachable = new ArrayList<>();
        for (final BlockPos cell : cells) {
            final BlockPos absolute = at.offset(cell);
            if (!level.isLoaded(absolute)) {
                // 1.21.1's setBlock on a cell whose chunk is not loaded is a SILENT no-op
                // (Level#setBlock asks the chunk source for a FULL chunk and the server
                // answers null), so this claim cannot reach the cell — the probe's own arm
                // names the drop. Census it, skip the write and the sweep.
                // docs/testing.md#pad-write-lands-only-in-a-loaded-chunk
                unreachable.add("(" + absolute.getX() + ", " + absolute.getY() + ", "
                        + absolute.getZ() + ")");
                continue;
            }
            final net.minecraft.world.level.block.state.BlockState state =
                    level.getBlockState(absolute);
            if (!state.isAir()) {
                cleared.add(state + " @ (" + absolute.getX() + ", " + absolute.getY() + ", "
                        + absolute.getZ() + ")");
                helper.setBlock(PORTAL_REL.offset(cell),
                        net.minecraft.world.level.block.Blocks.AIR);
            }
            for (final Entity stray : level.getEntities((Entity) null, new AABB(absolute.getX(),
                    absolute.getY(), absolute.getZ(), absolute.getX() + 1, absolute.getY() + 1,
                    absolute.getZ() + 1), found -> !(found instanceof Player))) {
                swept.add(BuiltInRegistries.ENTITY_TYPE.getKey(stray.getType()).getPath()
                        + " @ (" + absolute.getX() + ", " + absolute.getY() + ", "
                        + absolute.getZ() + ")");
                stray.discard();
            }
        }
        helper.assertTrue(!cells.isEmpty(),
                "[t_51dcbf51] an empty claim list cannot cover the gate's cell set — "
                        + "docs/testing.md#orb-claim-covers-asserted-cells");
        LOGGER.info("[TC-M6-06-boss] [t_51dcbf51] claimed the {} cells this portal probe reads"
                        + " about {}: cleared {} foreign block(s){} swept {} foreign entity(ies){}"
                        + " skipped {} cell(s) in unloaded chunks{}",
                cells.size(), at, cleared.size(), cleared.isEmpty() ? "" : " " + cleared,
                swept.size(), swept.isEmpty() ? "" : " " + swept, unreachable.size(),
                unreachable.isEmpty() ? "" : " " + unreachable);
        return cleared;
    }

    /**
     * Seed the statue's own random. 1.7.10 draws the crate pick from the mob's own
     * {@code rand} ({@code :186-187}), so the probe seeds it instead of sampling; 21.1.259
     * has no {@code Entity#setRandom} (that setter is a later-line platform API), so the
     * leg reaches the platform's {@code Entity#random} field reflectively the way
     * {@code EldritchCrabGameTest:374} reaches {@code NearestAttackableTargetGoal#targetType}.
     */
    private static void seedPortalRandom(final EntityCultistPortal mob, final long seed) {
        try {
            final java.lang.reflect.Field field = Entity.class.getDeclaredField("random");
            field.setAccessible(true);
            field.set(mob, RandomSource.create(seed));
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("cannot seed the statue's random on this platform",
                    e);
        }
    }

    /** The goal census for any portal-family mob (the leader's helper is type-tight). */
    private static List<WrappedGoal> portalGoals(final Mob mob, final boolean target) {
        final List<WrappedGoal> out = new ArrayList<>();
        for (final WrappedGoal g : (target ? mob.targetSelector : mob.goalSelector)
                .getAvailableGoals()) {
            out.add(g);
        }
        return out;
    }

    /**
     * The class-twin gate in the c5-1 card's shape (BD-133's family rule): the strip has
     * to be declared by the portal class itself, so a future {@code super.registerGoals()}
     * — or a parent that starts registering — is a compile-visible change here, not a
     * silent goal.
     */
    private static boolean declaresOwnGoalRegistration() {
        try {
            return EntityCultistPortal.class.getDeclaredMethod("registerGoals")
                    .getDeclaringClass() == EntityCultistPortal.class;
        } catch (final NoSuchMethodException e) {
            return false;
        }
    }

    /** Remove every block this suite's probes may write outside their own template. */
    private static void clearWrittenCells(final GameTestHelper helper,
            final List<BlockPos> cells) {
        for (final BlockPos cell : cells) {
            helper.setBlock(PORTAL_REL.offset(cell),
                    net.minecraft.world.level.block.Blocks.AIR);
        }
    }

    // ---------------------------------------------------------------- registration

    /**
     * {@code :18}, {@code :32} and {@code ConfigEntities.java:117} —
     * {@code registerModEntity(EntityCultistPortal.class, "CultistPortal", 30, instance,
     * 64, 20, false)}: the 1.5 x 3.0 box, the 64-block tracking as 4 chunks, the 20-tick
     * update interval (not the praetor's 3) and velocity updates FALSE — the one member
     * of the family that never syncs velocity, because it never moves ({@code :80-81}).
     */
    @GameTest(template = "empty")
    public static void thePortalIsRegisteredWithTheOriginalBoxAndInterval(
            final GameTestHelper helper) {
        final EntityType<?> type = registered(helper, "cultistportal");
        final BlockPos at = portalCell(helper);
        claimPortalCells(helper, at, PORTAL_BODY_CELLS);
        final EntityCultistPortal mob = portal(helper, at);
        helper.assertTrue(Math.abs(type.getDimensions().width() - PORTAL_BOX_WIDTH) < 1.0E-4,
                ":18 — setSize(1.5F, 3.0F): the width must be 1.5, got "
                        + type.getDimensions().width());
        helper.assertTrue(Math.abs(type.getDimensions().height() - PORTAL_BOX_HEIGHT) < 1.0E-4,
                ":18 — setSize(1.5F, 3.0F): the height must be 3.0, got "
                        + type.getDimensions().height());
        helper.assertTrue(type.clientTrackingRange() == CultistPortalAiLogic.TRACK_RANGE_CHUNKS,
                ":117 — tracking 64 blocks = 4 chunks, got " + type.clientTrackingRange());
        helper.assertTrue(type.updateInterval() == CultistPortalAiLogic.UPDATE_INTERVAL,
                ":117 — update interval 20, got " + type.updateInterval());
        // :117's third registration argument is FALSE (no velocity sync), which 1.21.1
        // exposes only on the client-bound packet path, so the probe measures the figure
        // that flag exists to serve: the statue has no motion to sync (:80-81, measured in
        // the body-figures probe) and 20-tick interval over the praetor's 3.
        helper.assertTrue(type.updateInterval() == 20 && type.updateInterval() != 3,
                ":117 — the update interval is 20, not the praetor's 3, got "
                        + type.updateInterval());
        helper.assertTrue(mob.fireImmune(),
                ":32 — setFireProof(1.0F): the statue is fire immune");
        helper.assertTrue(Math.abs(mob.getEyeHeight(Pose.STANDING)
                - CultistPortalAiLogic.EYE_HEIGHT) < 1.0E-4,
                ":18 — the eye line sits on the feet (eyeHeight 0.0 on the 3.0-tall box),"
                        + " got " + mob.getEyeHeight(Pose.STANDING) + " on a box of "
                        + type.getDimensions().height());
        mob.discard();
        census(helper, "registration");
        helper.succeed();
    }

    // ------------------------------------------------------------------ body figures

    /**
     * {@code :63-65}, {@code :43-45}, {@code :80-81}, {@code :69-71}, {@code :73-75} and
     * {@code :31} — health 500.0, attack 0.0, knockback resistance 1.0, armour 5, xp 30,
     * the 540-tick air supply, an unpushable statue a player can walk into, a {@code travel}
     * that writes nothing, and the port's client boss-bar seam: the reference carries
     * 1.7.10's {@code IBossDisplayData} marker ({@code :31}) for the client's bar, and
     * 21.1.259 has no counterpart of that interface, so the bar ships as a
     * {@code ServerBossEvent} fed from the health fraction and shown to every player
     * tracking the statue — the taintacle giant's landed resolution of the same line
     * ({@code EntityTaintacleGiant:75}, {@code :118-126}), recorded as a deviation.
     */
    @GameTest(template = "empty")
    public static void thePortalCarriesTheOriginalBodyFiguresAndTheBossDisplaySeam(
            final GameTestHelper helper) {
        final BlockPos at = portalCell(helper);
        claimPortalCells(helper, at, PORTAL_BODY_CELLS);
        final EntityCultistPortal mob = portal(helper, at);
        helper.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH).getValue() == 500.0,
                ":63 — maxHealth 500.0, got " + mob.getAttribute(Attributes.MAX_HEALTH).getValue());
        helper.assertTrue(mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue() == 0.0,
                ":64 — attack 0.0 (the statue never strikes; contact is the player's half, :257), got "
                        + mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
        helper.assertTrue(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue() == 1.0,
                ":65 — knockback resistance 1.0, got "
                        + mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue());
        helper.assertTrue(mob.getAttribute(Attributes.ARMOR).getValue() == 5.0,
                ":43-45 — armour 5, got " + mob.getAttribute(Attributes.ARMOR).getValue());
        helper.assertTrue(mob.getArmorValue() == 5,
                ":43-45 — the getter shape the reference used reports 5, got " + mob.getArmorValue());
        helper.assertTrue(mob.getExperienceReward(helper.getLevel(), null) == 30,
                ":66 — the xp reward is 30, got " + mob.getExperienceReward(helper.getLevel(), null));
        helper.assertTrue(mob.getMaxAirSupply() == 540,
                ":43 — maxAir 540 (the 1.7.10 field, 27 seconds), got " + mob.getMaxAirSupply());
        helper.assertTrue(!mob.isPushable(),
                ":73-75 — entityIsPushable = false");
        helper.assertTrue(mob.canBeCollidedWith(),
                ":69-71 — canBeCollidedWith() is true: the statue collides with the player");
        final Vec3 before = mob.position();
        mob.setDeltaMovement(1.0, 0.0, 1.0);
        mob.travel(new Vec3(1.0, 0.0, 1.0));
        helper.assertTrue(mob.position().distanceToSqr(before) < 1.0E-9,
                ":80-81 — the motion body is empty: travel() must not move the statue, moved to "
                        + mob.position());
        helper.assertTrue(EntityCultistPortal.class.getSuperclass() == Monster.class,
                ":14 — the reference header is 'extends EntityMob', so the statue is a plain"
                        + " 1.21.1 Monster, NOT the c5-1 boss base, got "
                        + EntityCultistPortal.class.getSuperclass());
        helper.assertTrue(!EntityThaumcraftBoss.class.isAssignableFrom(EntityCultistPortal.class),
                ":14, :31 — extending EntityThaumcraftBoss would install five goals, a rage"
                        + " rule and a 5x50 health scale the reference never has");
        helper.assertTrue(!Mob.class.isAssignableFrom(EntityThaumcraftBoss.class)
                        || EntityCultistPortal.class.isAssignableFrom(EntityCultistPortal.class),
                "the census below is only a strip gate against the Monster quartet if the"
                        + " mob is a Monster at all");
        helper.assertTrue(mob.getBossEvent() != null, ":31 — the bar exists on the server half");
        helper.assertTrue(Math.abs(mob.getBossEvent().getProgress() - 1.0F) < 1.0E-4,
                ":31 — a full-health statue shows a full bar, got "
                        + mob.getBossEvent().getProgress());
        mob.setHealth(250.0F);
        mob.setStageCounter(400);
        mob.aiStep();
        helper.assertTrue(Math.abs(mob.getBossEvent().getProgress() - 0.5F) < 1.0E-4,
                ":31 — the bar tracks the health fraction on the server half of the tick:"
                        + " 250 of 500 must read 0.5, got " + mob.getBossEvent().getProgress());
        mob.discard();
        census(helper, "body figures");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the empty goals

    /**
     * {@code :47-49} and {@code :83-84} — the statue has NO goals. 1.21.1's
     * {@code Monster#registerGoals} installs the Float / MeleeAttack / RandomStroll /
     * LookAtPlayer / RandomLookAround set and {@code Monster#getTargetSelector} a
     * HurtByTarget goal, so the {@code super} call is deliberately absent (the Pech
     * card's finding, {@code PechAiLogic:317-324}; the knight, cleric and praetor lanes
     * ship the same idiom with their own tables). The census is EMPTY on both selectors
     * at construction and stays empty through ten server ticks and a full {@code aiStep}.
     */
    @GameTest(template = "empty")
    public static void thePortalShipsTheOriginalEmptyGoalSetAfterTheMonsterStrip(
            final GameTestHelper helper) {
        final BlockPos at = portalCell(helper);
        claimPortalCells(helper, at, PORTAL_BODY_CELLS);
        helper.assertTrue(declaresOwnGoalRegistration(),
                ":83-84 — the strip must be declared by EntityCultistPortal itself, not"
                        + " inherited: the census below is only a strip gate if this class"
                        + " owns registerGoals()");
        final EntityCultistPortal mob = portal(helper, at);
        helper.assertTrue(portalGoals(mob, false).isEmpty(),
                ":47-49 — the goal selector must be empty at construction, got "
                        + describe(portalGoals(mob, false)));
        helper.assertTrue(portalGoals(mob, true).isEmpty(),
                ":47-49 — the target selector must be empty at construction, got "
                        + describe(portalGoals(mob, true)));
        for (int tick = 1; tick <= 10; tick++) {
            mob.aiStep();                                          // the server half runs here
        }
        final List<WrappedGoal> goals = portalGoals(mob, false);
        final List<WrappedGoal> targets = portalGoals(mob, true);
        helper.assertTrue(goals.isEmpty(),
                ":83-84 — ten server ticks and an aiStep must install nothing, got "
                        + describe(goals));
        helper.assertTrue(targets.isEmpty(),
                ":83-84 — and no target goal either, got " + describe(targets));
        helper.assertTrue(mob.position().distanceToSqr(
                new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)) < 1.0E-9,
                ":80-81 — the empty set plus the empty motion body leaves the statue on its"
                        + " cell, moved to " + mob.position());
        mob.discard();
        census(helper, "empty goal set");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the summon scan

    /**
     * {@code :214-217} — the scan the machine runs every time it needs a countdown:
     * {@code EntityUtils.getEntitiesInRange(world, x, y, z, this, EntityCultist.class,
     * 32.0)}. The window is a 32.0 cube about the statue, and the admitted class is the
     * landed {@link EntityCultist} base — the knight and the cleric count, the Crimson
     * Praetor does NOT (the c5-1 mob extends {@code EntityThaumcraftBoss}, not
     * {@code EntityCultist}, exactly as the reference's own hierarchy), and neither does
     * any other mob. The scanner is dropped by entity id ({@code :216}); here it is also
     * dropped by class, so the probe reads the size back through the countdown the scan
     * feeds: {@code getTiming()} is the scan size times 20 ({@code :217}).
     */
    @GameTest(template = "empty")
    public static void theSummonScanUsesTheThirtyTwoWindowAndTheCultistBaseOnly(
            final GameTestHelper helper) {
        final BlockPos at = portalCell(helper);
        claimPortalCells(helper, at, PORTAL_BODY_CELLS);
        final EntityCultistPortal mob = portal(helper, at);
        final EntityCultist atEdge = cleric(helper, at.offset(32, 0, 0));
        final EntityCultist pastEdge = cleric(helper, at.offset(33, 0, 0));
        final EntityCultistKnight kin = knightAt(helper, at.offset(0, 0, 31));
        final EntityCultistLeader praetorFar = leaderAt(helper, at.offset(0, 0, 40));
        final EntityCultistLeader praetorNear = leaderAt(helper, at.offset(2, 0, 2));
        final Entity cow = animalAt(helper, EntityType.COW, at.offset(3, 0, 3));
        final java.util.Set<Integer> mine = new java.util.HashSet<>();
        for (final Entity fixture : List.of(atEdge, pastEdge, kin, praetorFar, praetorNear,
                cow)) {
            mine.add(fixture.getId());
        }
        final List<EntityCultist> scan = mob.summonScan();
        final StringBuilder seen = new StringBuilder("scan size " + scan.size() + ":");
        for (final EntityCultist found : scan) {
            seen.append(' ').append(BuiltInRegistries.ENTITY_TYPE.getKey(found.getType()).getPath())
                    .append('@').append(found.blockPosition().toString());
        }
        helper.assertTrue(scan.stream().anyMatch(f -> f.getId() == atEdge.getId()),
                ":215 — a cultist at exactly 32.0 (the boundary cell, centre to centre) is"
                        + " inside the window: " + seen);
        helper.assertTrue(scan.stream().anyMatch(f -> f.getId() == kin.getId()),
                ":215 — the knight 31 blocks out is inside it too: the admitted class is the"
                        + " EntityCultist BASE and the knight extends it: " + seen);
        helper.assertTrue(scan.stream().noneMatch(f -> f.getId() == pastEdge.getId()),
                ":215 — a cultist at 33.0 is outside the 32.0 window: " + seen);
        helper.assertTrue(scan.stream().noneMatch(f -> f.getId() == praetorFar.getId())
                        && scan.stream().noneMatch(f -> f.getId() == praetorNear.getId()),
                ":216 — the admitted class is EntityCultist: the Crimson Praetor is an"
                        + " EntityThaumcraftBoss, so it never counts, near or far: " + seen);
        helper.assertTrue(scan.stream().filter(f -> mine.contains(f.getId())).count() == 2,
                ":215, :216 — of my seven fixtures exactly the 32.0 cleric and the 31-block"
                        + " knight sit in the set (the 33.0 cleric and both praetors do not;"
                        + " the cow is not an EntityCultist, so the scanner's class filter"
                        + " cannot return it): " + seen);
        // The scan window (32.0, a 65-cell cube) is wider than this pad: the stride-6 grid
        // packs a neighbour probe's pad inside it, so its mobs are in the window while the
        // batch runs. The boundary and class rules above are the fidelity claims and are
        // identity-exact; the SIZE is pinned as a delta and as the formula, never as a raw
        // fixture count (docs/testing.md#sweep-hazard-set-is-scan-set).
        helper.assertTrue(mob.getTiming() == 20 * scan.size(),
                ":213, :217 — the countdown's timing term is the scan size times 20: size "
                        + scan.size() + " must give " + (20 * scan.size()) + ", got "
                        + mob.getTiming());
        final int sizeBefore = scan.size();
        final EntityCultist third = cleric(helper, at.offset(-30, 0, 0));
        mine.add(third.getId());
        final int grown = mob.summonScan().size();
        helper.assertTrue(grown == sizeBefore + 1 && mob.getTiming() == 20 * grown,
                ":217 — a third cultist inside the window (30 blocks) moves the size by one"
                        + " and the timing by 20: " + sizeBefore + " -> " + grown + " timing "
                        + mob.getTiming() + " — " + seen);
        for (final Entity fixture : List.of(atEdge, pastEdge, kin, praetorFar, praetorNear, cow,
                third)) {
            fixture.discard();
        }
        mob.discard();
        census(helper, "summon scan");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the summon arms

    /**
     * {@code :220-240} and {@code :243-255} — the two summon arms, driven through the
     * firing seams the pech card's precedent uses ({@code EntityPech:481}): the machine's
     * own firing branch needs a real {@code ServerPlayer} inside 48 blocks
     * ({@code :103}), and the harness's {@code GameTestHelper#makeMockPlayer} is invisible
     * to {@code Level#getNearestPlayer} (that walks the server's player list, not the
     * chunk entity maps), so the probe drives {@code spawnMinions()} / {@code spawnBoss()}
     * — the methods the firing branch calls — and measures what they write in the world.
     *
     * <p>Per draw, one cultist ({@code :222} rolls a knight on {@code nextFloat() > 0.33F},
     * else a cleric) lands at the statue's x/z jittered by {@code nextFloat() -
     * nextFloat()} ({@code :229}, {@code :231} — the interval is -1 .. 1), 0.25 above its
     * feet ({@code :230}), homed on the statue's block with a 32-block radius
     * ({@code :235}). Forty draws put the pick rule on the record: p(knight) = 1 - 0.33 =
     * 0.67, mean 40 × 0.67 = 26.8, sd √(40 × 0.67 × 0.33) = 2.97, so the four-sigma band
     * is 15 .. 38 and the all-one-kind tail is 0.67^40 + 0.33^40 ≈ 1.2e-7. Past stage 12
     * the same arm drowns the statue for 5 + nextInt(5) ({@code :238-239}); at stage 12 it
     * must not.</p>
     */
    @GameTest(template = "empty")
    public static void theSummonArmsDropTheRolledCultistInTheOriginalBox(final GameTestHelper
            helper) {
        final BlockPos at = portalCell(helper);
        final List<BlockPos> claimed = new ArrayList<>(PORTAL_BODY_CELLS);
        claimed.addAll(PORTAL_SPAWN_CELLS);
        claimPortalCells(helper, at, claimed);
        final ServerLevel level = helper.getLevel();
        final EntityCultistPortal mob = portal(helper, at);
        mob.setStage(CultistPortalAiLogic.SELF_DAMAGE_STAGE_MIN);
        final int draws = 40;
        int knights = 0;
        int clerics = 0;
        for (int draw = 0; draw < draws; draw++) {
            final java.util.Set<Integer> before = new java.util.HashSet<>();
            for (final EntityCultist seen : mob.summonScan()) {
                before.add(seen.getId());
            }
            mob.spawnMinions();
            final List<EntityCultist> after = mob.summonScan();
            helper.assertTrue(after.size() == before.size() + 1,
                    ":222-236 — each firing arm adds exactly one cultist to the scan, draw "
                            + draw + " left " + after.size() + " against " + before.size());
            EntityCultist born = null;
            for (final EntityCultist found : after) {
                if (!before.contains(found.getId())) {
                    born = found;
                }
            }
            helper.assertTrue(born != null, ":236 — the summon reached the level, draw " + draw);
            helper.assertTrue(Math.abs(born.getX() - mob.getX()) <= 1.0 + 1.0E-9
                            && Math.abs(born.getZ() - mob.getZ()) <= 1.0 + 1.0E-9,
                    ":229, :231 — the drop is jittered by nextFloat() - nextFloat() (-1 .. 1)"
                            + " on both axes, got dx " + (born.getX() - mob.getX()) + " dz "
                            + (born.getZ() - mob.getZ()));
            helper.assertTrue(Math.abs((born.getY() - mob.getY())
                    - CultistPortalAiLogic.MINION_Y_OFFSET) < 1.0E-9,
                    ":230 — the drop is 0.25 above the statue's feet, got "
                            + (born.getY() - mob.getY()));
            helper.assertTrue(born.hasRestriction()
                            && born.getRestrictRadius() == CultistPortalAiLogic.MINION_HOME_RADIUS
                            && born.getRestrictCenter().equals(new BlockPos((int) mob.getX(),
                                    (int) mob.getY(), (int) mob.getZ())),
                    ":235 — the summon is homed on the statue's (int)-cast position (the"
                            + " reference writes (int)posX/(int)posY/(int)posZ, which truncates"
                            + " toward ZERO: for a mob centred in a cell on the negative half"
                            + " of the axis that is one block higher than blockPosition()'s"
                            + " floor) with a 32-block radius, got restriction="
                            + born.hasRestriction() + " radius=" + born.getRestrictRadius()
                            + " centre=" + born.getRestrictCenter() + " statue="
                            + mob.blockPosition() + " at " + mob.position());
            if (born instanceof EntityCultistKnight) {
                knights++;
            } else if (born instanceof EntityCultistCleric) {
                clerics++;
            } else {
                helper.assertTrue(false, ":223, :225 — the arm spawns a knight or a cleric, got "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(born.getType()));
            }
            born.discard();
        }
        helper.assertTrue(knights + clerics == draws,
                ":222 — every draw landed a minion, got knights " + knights + " clerics " + clerics);
        helper.assertTrue(knights >= 1 && knights <= draws - 1,
                ":222 — over 40 draws both arms must appear (the all-one-kind tail is"
                        + " 0.67^40 + 0.33^40 = 1.2e-7), got knights " + knights);
        final double mean = draws * (1.0 - 0.33);
        final double sd = Math.sqrt(draws * (1.0 - 0.33) * 0.33);
        final int low = (int) Math.ceil(mean - 4.0 * sd);
        final int high = (int) Math.floor(mean + 4.0 * sd);
        helper.assertTrue(knights >= low && knights <= high,
                ":222 — the pick is nextFloat() > 0.33F: 40 draws sit in the four-sigma band"
                        + " " + low + " .. " + high + " (mean 26.8, sd 2.97), got knights "
                        + knights);
        // The upkeep leg past stage 12 (:238-239). The arm fires the reference's own
        // source — DamageSource.drown (field_76380_i: absolute, armour-bypassing) — for
        // 5 + nextInt(5). On 21.1.259 that source is REFUSED by the statue's OWN fire
        // immunity (:38 field_70178_ae = true, the port's .fireImmune()), because the
        // platform's drown type is fire-family and LivingEntity#hurt answers false for
        // IS_FIRE on a fire-immune mob. 1.7.10's drown is not a fire source, so the
        // upkeep lands there: the cost vanishes on the port, recorded as BD-134. The two
        // controls below name the mechanism by measurement — drown DOES damage this
        // platform (a plain armoured-zero cow loses the raw 5.0) and the statue IS
        // damageable (a mob-attack 5.0 goes through its armour 5 at 4.375). The arm's
        // rule constants (the stage gate, the 5 + nextInt(5) roll, the drown id) ride
        // CultistPortalAiLogicTest; the source ships verbatim, not swapped for one the
        // port prefers. The refusal is what this leg pins, so it pins the REFUSED hurt —
        // zero damage from a DROWN-SOURCED hurt. 21.1.259's LivingEntity#hurt RECORDS the
        // source of a hurt it refuses (measured 3/3 arms on t_2f9c86c8: lastSource = the
        // drown source, damage 0.0), and that record is the reference's arm running, so
        // "no source recorded" is NOT part of the ruling: the port keeps hurt(drown).
        mob.setHealth(500.0F);
        mob.setStage(CultistPortalAiLogic.SELF_DAMAGE_STAGE_MIN);
        mob.setStageCounter(400);
        final float full = mob.getHealth();
        mob.spawnMinions();
        final float landed = full - mob.getHealth();
        final net.minecraft.world.damagesource.DamageSource upkeep =
                mob.getLastDamageSource();
        helper.assertTrue(landed == 0.0F && upkeep != null && upkeep.is(DamageTypes.DROWN),
                ":238-239 / BD-134 — the arm fires drown at the statue and 21.1.259 refuses"
                        + " it (the statue's :38 fire immunity over the platform's"
                        + " fire-family drown type), so the upkeep lands NOTHING here while"
                        + " the reference's absolute 5 + nextInt(5) lands on 1.7.10; the"
                        + " refused hurt keeps its source on this platform, which is the"
                        + " port's arm running: got damage " + landed + " lastSource "
                        + upkeep);
        final net.minecraft.world.entity.LivingEntity cow =
                (net.minecraft.world.entity.LivingEntity) animalAt(helper, EntityType.COW,
                        at.offset(4, 0, -4));
        final float cowFull = cow.getHealth();
        cow.hurt(level.damageSources().drown(), 5.0F);
        helper.assertTrue(cow.getHealth() == cowFull - 5.0F,
                "the control — drown damages this platform (a plain cow takes the raw 5.0,"
                        + " armour-bypassing), got " + (cowFull - cow.getHealth()));
        // The mob-attack control rides vanilla's damage cooldown: the REFUSED drown
        // upkeep above arms LivingEntity#invulnerableTime and records the source of the
        // hurt the statue's fire immunity refuses (measured 3/3 arms on t_2f9c86c8), so a
        // 5.0 melee fired on the same tick is refused by the 10-tick window and lands 0.0
        // — the gate legs' "got 0.0" was the window, not the statue. That refusal is the
        // platform's, not the port's, so the leg pins BOTH sides: inside the window the
        // melee lands nothing, cleared (this file's established reset, :373 / :561) the
        // armour-5 statue is damageable.
        // docs/testing.md#follow-up-hurts-clear-the-ten-tick-invuln-window
        final float inWindow = mob.getHealth();
        mob.hurt(level.damageSources().mobAttack(cow), 5.0F);
        helper.assertTrue(inWindow - mob.getHealth() == 0.0F,
                "the control — vanilla's 10-tick window, armed by the refused drown"
                        + " upkeep, refuses the follow-up melee: got "
                        + (inWindow - mob.getHealth()));
        mob.invulnerableTime = 0;                                                 // vanilla's 10-tick invuln window
        final float before = mob.getHealth();
        mob.hurt(level.damageSources().mobAttack(cow), 5.0F);
        helper.assertTrue(mob.getHealth() < before && mob.getLastDamageSource() != null
                        && mob.getLastDamageSource().is(DamageTypes.MOB_ATTACK),
                "the control — the statue is damageable: a non-fire mob-attack 5.0 lands"
                        + " on the armour-5 statue once the window is clear, got "
                        + (before - mob.getHealth()));
        cow.discard();
        mob.setHealth(500.0F);
        mob.setStage(CultistPortalAiLogic.STAGE_BOSS);
        mob.setStageCounter(400);
        mob.spawnMinions();
        helper.assertTrue(mob.getHealth() == 500.0F,
                ":238 — the gate is stage > 12: at stage 12 the arm costs the statue nothing,"
                        + " got health " + mob.getHealth());
        // The boss arm (:243-255): one praetor, same geometry, no upkeep.
        mob.setStage(CultistPortalAiLogic.STAGE_BOSS);
        final AABB near = mob.getBoundingBox().inflate(8.0);
        final int leadersBefore = level.getEntitiesOfClass(EntityCultistLeader.class, near).size();
        mob.spawnBoss();
        final List<EntityCultistLeader> leaders = level.getEntitiesOfClass(EntityCultistLeader.class,
                near);
        helper.assertTrue(leaders.size() == leadersBefore + 1,
                ":244, :253 — the stage-12 arm adds exactly one Crimson Praetor, got "
                        + leaders.size() + " against " + leadersBefore);
        final EntityCultistLeader born = leaders.stream()
                .filter(f -> f.getId() != mob.getId())
                .max(java.util.Comparator.comparingInt(Entity::getId)).orElseThrow();
        helper.assertTrue(Math.abs(born.getX() - mob.getX()) <= 1.0 + 1.0E-9
                        && Math.abs(born.getZ() - mob.getZ()) <= 1.0 + 1.0E-9
                        && Math.abs((born.getY() - mob.getY())
                        - CultistPortalAiLogic.MINION_Y_OFFSET) < 1.0E-9,
                ":246-248 — the praetor drops at the same jittered, 0.25-raised position, got"
                        + " dx " + (born.getX() - mob.getX()) + " dy " + (born.getY() - mob.getY())
                        + " dz " + (born.getZ() - mob.getZ()));
        helper.assertTrue(born.hasRestriction()
                        && born.getRestrictRadius() == CultistPortalAiLogic.MINION_HOME_RADIUS
                        && born.getRestrictCenter().equals(new BlockPos((int) mob.getX(),
                                (int) mob.getY(), (int) mob.getZ())),
                ":251 — the praetor is homed on the statue's (int)-cast position at a"
                        + " 32-block radius, got radius " + born.getRestrictRadius()
                        + " centre=" + born.getRestrictCenter());
        helper.assertTrue(mob.getHealth() == 500.0F,
                ":243-255 — the boss arm carries no upkeep leg, got health " + mob.getHealth());
        born.discard();
        // Every cultist this probe's arms produced (the 40 draws are discarded per draw;
        // the upkeep and stage legs each add one) goes back out: a summon with a 32-block
        // home wandering off the pad is a fixture the neighbour pads inherit.
        for (final EntityCultist leak : level.getEntitiesOfClass(EntityCultist.class,
                mob.getBoundingBox().inflate(34.0))) {
            leak.discard();
        }
        mob.discard();
        census(helper, "summon arms: " + knights + " knights / " + clerics + " clerics of " + draws);
        helper.succeed();
    }

    // ------------------------------------------------------------- the stage machine

    /**
     * {@code :102-204} — the machine, measured on the statue's own ticks. The countdown
     * loses exactly one per tick ({@code :136}); the banner wall writes on the single
     * value 160 in stage 0 ({@code :137}) — 161 must write nothing — and the four cells
     * carry {@code thaumcraft:blockwoodendevice} meta 8 ({@code :143-148}, {@code :153})
     * with the facings 8 / 0 / 12 / 4 ({@code :157}, {@code :160}, {@code :163},
     * {@code :166}); the loot crate writes on a countdown inside (20, 150) that divides
     * by 13 ({@code :185}) — so 144 writes nothing and 143 writes one
     * {@code thaumcraft:blocklootcrate} ({@code :192}) in the 9 x 9 field, off the
     * statue's own column on BOTH axes ({@code :188}). Both write arms read the cell the
     * BUILDER computes — the {@code (int)} CAST of the statue's raw position plus the
     * offset, {@code :144}/{@code :146}/{@code :186} — not the pad cell plus the offset;
     * the statue stands in the cell centre and the cast truncates toward zero, so on a
     * negative axis the two origins differ by one
     * (docs/testing.md#mob-write-cells-ride-the-int-cast). A wall cell in a chunk this
     * game test has not loaded is the platform's silent drop and reads air
     * (docs/testing.md#pad-write-lands-only-in-a-loaded-chunk). The firing arm with no player
     * inside 48 blocks ({@code :103}) re-arms at 30 + nextInt(30) ({@code :133}) and does
     * NOT advance the stage; and the self-heal (+1) runs on every server tick below stage
     * 12 and stops at it ({@code :203-204}).
     */
    @GameTest(template = "empty")
    public static void theStageMachineCountsDownAndGatesTheBannerWallAndCrate(
            final GameTestHelper helper) {
        final BlockPos at = portalCell(helper);
        final List<BlockPos> claimed = new ArrayList<>(PORTAL_BODY_CELLS);
        claimed.addAll(PORTAL_BANNER_CELLS);
        claimPortalCells(helper, at, claimed);
        final ServerLevel level = helper.getLevel();
        final EntityCultistPortal mob = portal(helper, at);
        // :144 / :146 / :186 — the write origin of BOTH arms is the (int) CAST of the
        // statue's raw position, and the port's bannerCell() is that cast plus the
        // direction times 6. The statue stands in the cell CENTRE (at + 0.5 on x and z)
        // and a Java cast truncates toward ZERO, so on the negative half of an axis the
        // cast origin sits one block HIGHER than the pad cell — the same rule the summon
        // arm pins for the minion's home centre at :235. The wall's cells are therefore
        // NOT `at + (0,0,+-6)`: they are the builder's own cells, recomputed here from
        // the same cast, so the claim, the census and the asserts all ride ONE list
        // (docs/testing.md#mob-write-cells-ride-the-int-cast).
        final BlockPos writeOrigin =
                new BlockPos((int) mob.getX(), (int) mob.getY(), (int) mob.getZ());
        final List<BlockPos> wallCells = new ArrayList<>();
        for (int a = CultistPortalAiLogic.BANNER_DIRECTION_FROM;
                a < CultistPortalAiLogic.BANNER_DIRECTION_TO; a++) {                   // :140
            wallCells.add(EntityCultistPortal.bannerCell(writeOrigin, a));              // :141-146
        }
        final List<BlockPos> wallClaim = new ArrayList<>();
        for (final BlockPos cell : wallCells) {
            wallClaim.add(cell.subtract(at));
        }
        claimPortalCells(helper, at, wallClaim);
        try {
            // :136 — one decrement per tick, no fire above zero.
            mob.setStage(0);
            mob.setStageCounter(200);
            for (int tick = 1; tick <= 4; tick++) {
                mob.aiStep();
                helper.assertTrue(mob.getStageCounter() == 200 - tick,
                        ":136 — the countdown loses one per tick, tick " + tick + " left "
                                + mob.getStageCounter());
                helper.assertTrue(mob.getStage() == 0,
                        ":131 — the stage only advances on a firing, tick " + tick + " left "
                                + mob.getStage());
            }
            // :137 — the banner gate's upper edge writes nothing.
            mob.setStage(0);
            mob.setStageCounter(162);
            mob.aiStep();
            helper.assertTrue(mob.getStageCounter() == 161,
                    ":136 — 162 counts down to 161, got " + mob.getStageCounter());
            for (final BlockPos cell : wallCells) {
                if (!level.isLoaded(cell)) {
                    continue; // an unloaded chunk drops the claim's clear as well
                }
                helper.assertTrue(level.getBlockState(cell).isAir(),
                        ":137 — the wall is gated on the single value 160, so 161 wrote a"
                                + " device at " + cell);
            }
            // :137-183 — and 160 writes the wall. Each cell sits 6 blocks out from the
            // (int)-cast origin, which on the stride-6 packing puts SOME of them in chunks
            // this 1x1 template did not force-load: the template force-loads only its OWN
            // intersecting chunks (StructureUtils#forceLoadChunks), and a world.setBlock on
            // a cell whose chunk is not loaded is dropped SILENTLY by 1.21.1 (its
            // Level#setBlock asks the chunk source for a FULL chunk and the server answers
            // null, so the write is never applied). The cell list below is the builder's
            // own, computed from the same (int)-cast origin, so the claim, the census and
            // the asserts ride one list (docs/testing.md#mob-write-cells-ride-the-int-cast).
            // Every wall cell in a LOADED chunk — the origin chunk, or a neighbour
            // template's forced chunk — must carry the device, the meta-8 variant and the
            // banner tile with its facing; every cell in an UNLOADED chunk is the platform's
            // silent drop and is asserted as air, with the chunk and the loaded flag in the
            // message (docs/testing.md#pad-write-lands-only-in-a-loaded-chunk).
            mob.setStage(0);
            mob.setStageCounter(161);
            mob.aiStep();
            helper.assertTrue(mob.getStageCounter() == 160,
                    ":136 — 161 counts down to 160, the gate value, got " + mob.getStageCounter());
            final int originChunkX = SectionPos.blockToSectionCoord(at.getX());
            final int originChunkZ = SectionPos.blockToSectionCoord(at.getZ());
            // The channel census: the cell the probe expects, the cell the PORT computed
            // from the same (int)-cast origin (identical by construction — a drift in the
            // builder's cell expression shows up HERE as a name mismatch), the cell's chunk,
            // whether that chunk is loaded, and the readback.
            final StringBuilder wallCensus = new StringBuilder();
            for (int wall = 0; wall < wallCells.size(); wall++) {
                final BlockPos cell = wallCells.get(wall);
                wallCensus.append(" dir")
                        .append(CultistPortalAiLogic.BANNER_DIRECTION_FROM + wall)
                        .append(' ').append(cell)
                        .append(" port=").append(EntityCultistPortal.bannerCell(writeOrigin,
                                CultistPortalAiLogic.BANNER_DIRECTION_FROM + wall))
                        .append(" chunk=").append(SectionPos.blockToSectionCoord(cell.getX()))
                        .append(',').append(SectionPos.blockToSectionCoord(cell.getZ()))
                        .append(level.isLoaded(cell) ? " loaded=1" : " loaded=0")
                        .append(" read=").append(level.getBlockState(cell));
            }
            LOGGER.info("[TC-M6-06-boss] [t_2f9c86c8] wall census about {} write origin {} mob"
                            + " {} state {} census {}:", at, writeOrigin, mob.position(),
                    mob.blockPosition(), wallCensus);
            int measuredCells = 0;
            for (int wall = 0; wall < wallCells.size(); wall++) {
                final BlockPos cell = wallCells.get(wall);
                final net.minecraft.world.level.block.state.BlockState state =
                        level.getBlockState(cell);
                final int cellChunkX = SectionPos.blockToSectionCoord(cell.getX());
                final int cellChunkZ = SectionPos.blockToSectionCoord(cell.getZ());
                if (!level.isLoaded(cell)) {
                    helper.assertTrue(state.isAir(),
                            ":143-148 — the wall cell " + cell + " sits in chunk (" + cellChunkX
                                    + ", " + cellChunkZ + "), which this game test has not loaded"
                                    + " (the template force-loads only chunk " + originChunkX
                                    + ", " + originChunkZ + "), so 1.21.1 drops the write"
                                    + " silently and the wall leg measures it as air, got "
                                    + state);
                    continue;
                }
                measuredCells++;
                helper.assertTrue(state.is(BuiltInRegistries.BLOCK
                                .get(CultistPortalAiLogic.BLOCK_WOODEN_DEVICE)),
                        ":142-148 — the wall cell " + cell + " (dir "
                                + (CultistPortalAiLogic.BANNER_DIRECTION_FROM + wall) + " from the"
                                + " (int)-cast origin " + writeOrigin + ") carries"
                                + " thaumcraft:blockwoodendevice, got " + state);
                helper.assertTrue(state.getBlock() instanceof VariantDevice device
                                && device.variant(state) == CultistPortalAiLogic.BANNER_VARIANT,
                        ":143-148 — meta 8 (the banner), got " + state);
                helper.assertTrue(level.getBlockEntity(cell) instanceof TileBanner banner
                                && banner.getFacing() == (byte) PORTAL_BANNER_FACINGS[wall],
                        ":153, :" + new int[]{157, 160, 163, 166}[wall] + " — the banner on cell "
                                + cell + " carries facing " + PORTAL_BANNER_FACINGS[wall]
                                + ", got " + level.getBlockEntity(cell));
            }
            helper.assertTrue(measuredCells >= 1,
                    ":143-148 — the harness packing left NO banner cell in a loaded chunk, so"
                            + " the wall is unmeasurable here; the geometry is unit-pinned in"
                            + " CultistPortalAiLogicTest and this probe needs at least one"
                            + " measurable cell (write origin " + writeOrigin + ", cells"
                            + " " + wallCells + ")");
            // :185-199 — the crate region has to be filled for the gate to accept a cell.
            for (int dx = -CRATE_REGION_HALF; dx <= CRATE_REGION_HALF; dx++) {
                for (int dz = -CRATE_REGION_HALF; dz <= CRATE_REGION_HALF; dz++) {
                    helper.setBlock(PORTAL_REL.offset(dx, 0, dz),
                            net.minecraft.world.level.block.Blocks.STONE);
                }
            }
            // :185-199 — the crate leg. The pick is the statue's OWN random (:186-187), so
            // the probe seeds it rather than sampling: seed 15 draws nextInt(5) 4,3 on x
            // and 0,1 on z, i.e. the cell (-1, 0, +1) — a cell that always sits inside the
            // origin chunk, so it is a cell the platform will accept. The gate also needs a
            // NON-AIR target (:188), which the fill below supplies.
            final Block crateBlock = BuiltInRegistries.BLOCK.get(CultistPortalAiLogic.BLOCK_LOOT_CRATE);
            mob.setStage(0);
            mob.setStageCounter(145);
            mob.aiStep();
            helper.assertTrue(countCrates(level, at, crateBlock) == 0,
                    ":185 — the gate needs a countdown divisible by 13, so 144 wrote no crate");
            seedPortalRandom(mob, CRATE_PICK_SEED);
            mob.setStage(0);
            mob.setStageCounter(144);
            mob.aiStep();
            helper.assertTrue(mob.getStageCounter() == 143,
                    ":136, :185 — 144 counts down to 143, the first gate value on the way"
                            + " down from 200, got " + mob.getStageCounter());
            // :186-188 — the crate cell is the SAME (int)-cast origin plus the pick, not
            // the pad cell plus the pick: the source gates the pick on
            // `crateX != (int)this.posX && crateZ != (int)this.posZ`.
            final BlockPos crateCell = writeOrigin.offset(CRATE_PICK);
            helper.assertTrue(crateCell.getX() != writeOrigin.getX()
                            && crateCell.getZ() != writeOrigin.getZ(),
                    ":188 — the pick must leave the statue's column on BOTH axes, got "
                            + CRATE_PICK);
            final int crates = countCrates(level, at, crateBlock);
            helper.assertTrue(crates == 1,
                    ":185-192 — the seeded pick " + CRATE_PICK + " (seed " + CRATE_PICK_SEED
                            + ") with a filled target cell writes exactly one crate in the 9x9"
                            + " field, got " + crates);
            final net.minecraft.world.level.block.state.BlockState crateState =
                    level.getBlockState(crateCell);
            helper.assertTrue(crateState.is(crateBlock),
                    ":186-192 — the crate sits on the cell the seeded pick names, got "
                            + crateState + " at " + crateCell + " (statue " + at + ")");
            final int rarity = crateState.getValue(BlockLoot.VARIANT);
            helper.assertTrue(rarity == 0 || rarity == 1 || rarity == 2,
                    ":190-191 — the crate's rarity is the original's metadata 0, 1 or 2 (the"
                            + " roll rides the LEVEL's random, so the value is a range here,"
                            + " never a fixed number), got " + rarity);
            // :103, :133 — with no player, the firing arm re-arms and does not advance.
            helper.assertTrue(level.getNearestPlayer(mob, CultistPortalAiLogic.PLAYER_SCAN) == null,
                    ":103 — the harness's mock player is invisible to Level#getNearestPlayer"
                            + " (it walks the server player list), so the firing branch's"
                            + " player leg is unreachable here and the counter formulas ride"
                            + " the unit pins; this probe measures the no-player arm");
            mob.setStage(5);
            mob.setStageCounter(0);
            mob.aiStep();
            helper.assertTrue(mob.getStageCounter() >= CultistPortalAiLogic.NO_PLAYER_COUNTER_BASE
                            && mob.getStageCounter() <= CultistPortalAiLogic.NO_PLAYER_COUNTER_BASE
                                    + CultistPortalAiLogic.NO_PLAYER_COUNTER_ROLL - 1,
                    ":133 — with no player the counter re-arms at 30 + nextInt(30) = 30 .. 59,"
                            + " got " + mob.getStageCounter());
            helper.assertTrue(mob.getStage() == 5,
                    ":131 — the stage advances only on a firing with a player, got "
                            + mob.getStage());
            // :203-204 — the self-heal under stage 12, stopped at it.
            mob.setStage(5);
            mob.setStageCounter(400);
            mob.setHealth(100.0F);
            mob.aiStep();
            helper.assertTrue(mob.getHealth() == 101.0F,
                    ":203-204 — under stage 12 the statue heals 1.0 per server tick, got "
                            + mob.getHealth());
            mob.setStage(CultistPortalAiLogic.STAGE_BOSS);
            mob.setStageCounter(400);
            mob.setHealth(100.0F);
            mob.aiStep();
            helper.assertTrue(mob.getHealth() == 100.0F,
                    ":203 — at stage 12 the heal is off, got " + mob.getHealth());
        } finally {
            // The wall and the crate ride cells 6 blocks out — the neighbour pads at
            // stride 6 — so every block this probe writes goes back to air before the
            // leg returns, and the claim list is the list it cleans.
            for (int dx = -CRATE_REGION_HALF; dx <= CRATE_REGION_HALF; dx++) {
                for (int dz = -CRATE_REGION_HALF; dz <= CRATE_REGION_HALF; dz++) {
                    helper.setBlock(PORTAL_REL.offset(dx, 0, dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
            final List<BlockPos> written = new ArrayList<>(wallClaim);
            written.addAll(PORTAL_BANNER_CELLS);
            clearWrittenCells(helper, written);
            mob.discard();
        }
        census(helper, "stage machine");
        helper.succeed();
    }

    /** The crate census in the 9 x 9 field about the statue. */
    private static int countCrates(final ServerLevel level, final BlockPos at,
            final Block crateBlock) {
        int crates = 0;
        for (final BlockPos cell : BlockPos.betweenClosed(at.offset(-CRATE_REGION_HALF, 0,
                -CRATE_REGION_HALF), at.offset(CRATE_REGION_HALF, 0, CRATE_REGION_HALF))) {
            if (level.getBlockState(cell).is(crateBlock)) {
                crates++;
            }
        }
        return crates;
    }

    private static Entity animalAt(final GameTestHelper helper,
            final EntityType<?> type, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final Entity animal = type.create(level);
        animal.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        animal.setNoGravity(true);
        level.addFreshEntity(animal);
        return animal;
    }
}
