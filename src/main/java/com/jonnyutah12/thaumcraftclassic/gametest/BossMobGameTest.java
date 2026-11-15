package com.jonnyutah12.thaumcraftclassic.gametest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
}
