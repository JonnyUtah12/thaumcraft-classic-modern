package com.jonnyutah12.thaumcraftclassic.entities.ai.boss;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

/**
 * TC's own melee goal — {@code thaumcraft.common.entities.ai.combat.AIAttackOnCollide}
 * (126 lines), the goal the boss family installs: the CultistLeader at {@code :59},
 * the EldritchWarden at {@code :72} and the EldritchGolem at {@code :47} (all three
 * with {@code (this, EntityLivingBase.class, 1.1, false)}).
 *
 * <p>Read against 1.21.1: the class is a verbatim copy of 1.7.10's vanilla
 * {@code EntityAIAttackOnCollide} — the same path-recompute cadence
 * ({@code 4 + rand(7)}, {@code :46-47}), the same path-finding penalties
 * (+10 {@code :123}/{@code :125}, +5/+10 for the 256/1024 distance bands
 * {@code :129-132}, +15 on a failed move {@code :135}), the same reach test
 * {@code d1 = width*2*width + targetWidth} ({@code :62}) and the same 10-tick
 * attack gate ({@code :141-147}). 1.21.1's vanilla {@link MeleeAttackGoal} is that
 * port, so the ONLY delta TC adds over vanilla is the {@code classTarget} filter at
 * {@code :45} — which vanilla's goal has no slot for. Hence: extend the vanilla
 * goal and add the filter, so every path/attack figure stays vanilla-faithful and
 * the class filter is the ported part.</p>
 *
 * <p>The family's goal reconciliation (the row's known_differences): the bosses'
 * melee ({@code :59}/{@code :72}/{@code :47}), long-range ({@code :58}/{@code :71},
 * {@code AILongRangeAttack} — c4's class, reused), and the Leader's gang-call hurt
 * target ({@code :64}, {@code AICultistHurtByTarget}) are Thaumcraft's own, while
 * the swim, restriction, wander, watch, idle, hurt-target and nearest-target lines
 * ({@code EntityAISwimming}, {@code EntityAIMoveTowardsRestriction}, {@code
 * EntityAIWander}, {@code EntityAIWatchClosest}, {@code EntityAILookIdle}, {@code
 * EntityAIHurtByTarget}, {@code EntityAINearestAttackableTarget}) are vanilla in the
 * original and stay vanilla here. The golem's melee is the PLAIN {@code
 * AIAttackOnCollide}, not {@code AIGolemAttackOnCollide} — that one belongs to the
 * golem mob slice (TC-M6-08), not to this boss family. 1.21.1's {@code Monster}
 * constructor adds no goal of its own, so no inherited goal needs stripping.</p>
 */
public class AIBossAttackOnCollide extends MeleeAttackGoal {

    /** {@code :18} — the assignable class filter {@code :45} applies to the live target. */
    private final Class<? extends LivingEntity> classTarget;

    /**
     * {@code :26-29} → {@code :31-36}: the 4-arg form stores the filter and delegates;
     * the 3-arg form is {@code (attacker, speedTowardsTarget, longMemory)} and sets the
     * {@code func_75248_a(3)} flag pair (move + look), which {@code MeleeAttackGoal}
     * sets identically.
     */
    public AIBossAttackOnCollide(final PathfinderMob attacker,
            final Class<? extends LivingEntity> classTarget,
            final double speedTowardsTarget, final boolean longMemory) {
        super(attacker, speedTowardsTarget, longMemory);
        this.classTarget = classTarget;
    }

    /** {@code :45} — a target outside the filter never runs the goal. */
    @Override
    public boolean canUse() {
        if (this.classTarget != null) {
            final LivingEntity target = this.mob.getTarget();
            if (target == null || !this.classTarget.isAssignableFrom(target.getClass())) {
                return false;
            }
        }
        return super.canUse();
    }

    public Class<? extends LivingEntity> classTarget() {
        return this.classTarget;
    }

    /** The vanilla siblings the bosses' goal table names (so probes can assert types). */
    public static WaterAvoidingRandomStrollGoal vanillaWander(final PathfinderMob mob, final double speed) {
        return new WaterAvoidingRandomStrollGoal(mob, speed);                           // EntityAIWander
    }

    public static <T extends LivingEntity> NearestAttackableTargetGoal<T> vanillaNearest(
            final Mob mob, final Class<T> targetClass, final int interval, final boolean mustSee) {
        return new NearestAttackableTargetGoal<>(mob, targetClass, interval, mustSee);   // EntityAINearestAttackableTarget
    }

    public static HurtByTargetGoal vanillaHurt(final Mob mob, final boolean isAuxTarget) {
        // :77/:52 — 1.7.10's (mob, isAuxTarget) pair: the aux flag has no 1.21.1 ctor
        // slot (it fed the 1.7.10 target-assignment path), so the flag is recorded here.
        return new HurtByTargetGoal(mob);
    }
}
