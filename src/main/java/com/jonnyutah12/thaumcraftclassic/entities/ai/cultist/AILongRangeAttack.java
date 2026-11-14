package com.jonnyutah12.thaumcraftclassic.entities.ai.cultist;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;

/**
 * The long-range attack — {@code thaumcraft.common.entities.ai.combat.AILongRangeAttack},
 * goal slot 2 of the Crimson Cleric ({@code EntityCultistCleric:36}):
 * {@code new AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F)}.
 *
 * <p>Original: {@code extends EntityAIArrowAttack} (1.21.1's
 * {@link RangedAttackGoal}) and adds one thing: a minimum range. Its
 * {@code shouldExecute} ({@code :16-37}) takes the parent's verdict, and if the
 * parent says yes it reads the mob's current target, refuses when there is
 * none, <b>clears the target and refuses</b> when the target is dead
 * ({@code :26-29}), and refuses when the squared distance to the target's
 * {@code (x, boundingBox.minY, z)} is under {@code minDistance²}
 * ({@code :30-34}). The squared comparison is the original's own
 * ({@code ra < min * min}), so a 2.0 minimum is 2.0 blocks, not 4.0.</p>
 *
 * <p>The 1.7.10 constructor's arguments map one for one onto
 * {@code RangedAttackGoal(RangedAttackMob, movespeed, attackIntervalMin,
 * timeBetweenAttacks, attackRadius)}: min range is the only addition, so it is
 * the only override. The gate is split out as {@link CultistAiLogic#rangedGate}
 * so the unit test can pin all three arms without a world.</p>
 *
 * <p>The parent's own gates ship untouched: the 20..40-tick attack interval
 * (1.21.1's {@code reducedTickDelay} halves it, the platform's standing rule for
 * goal intervals, recorded with the pech's goal pair), the 24.0F attack radius,
 * the strafe/keep-distance movement at 1.0, and the target-clear on the
 * interval.</p>
 */
public class AILongRangeAttack extends RangedAttackGoal {

    private final RangedAttackMob wielderMob;
    private final double minDistance;

    public AILongRangeAttack(final RangedAttackMob wielder, final double minDistance,
            final double speed, final int intervalMin, final int intervalMax,
            final float radius) {
        super(wielder, speed, intervalMin, intervalMax, radius);
        this.wielderMob = wielder;
        this.minDistance = minDistance;                                                 // :12-13
    }

    /**
     * {@code :16-37} — {@code shouldExecute()}. The dead-target arm clears the
     * mob's target, exactly as the original writes {@code setEntityToAttack(null)}.
     */
    @Override
    public boolean canUse() {
        if (!super.canUse()) {                                                          // :17-18
            return false;
        }
        final Mob wielder = (Mob) this.wielderMob;
        final LivingEntity target = wielder.getTarget();                                // :22
        if (!CultistAiLogic.rangedGate(target != null, target != null && !target.isAlive(),
                distanceSquaredTo(target))) {
            if (target != null && !target.isAlive()) {
                wielder.setTarget(null);                                                // :28
            }
            return false;                                                               // :29/:33
        }
        return true;
    }

    /** {@code :30-31} — squared distance to the target's {@code (x, BB.minY, z)}. */
    private double distanceSquaredTo(final LivingEntity target) {
        final LivingEntity wielder = (LivingEntity) this.wielderMob;
        return wielder.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
    }

    /** The pinned minimum, for probes that read the goal's own figure. */
    public double getMinDistance() {
        return this.minDistance;
    }
}
