package com.jonnyutah12.thaumcraftclassic.entities.ai;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Port of the original's {@code EntityAIWatchClosest(this,
 * LivingEntity.class, 8.0F)} row (goalSelector 10 in {@code
 * EntityTaintVillager.func_70610_a()}, line 52; TC-M6-06-s-b). 1.21.1 has
 * {@code LookAtPlayerGoal} for the player-shaped watch rows but no
 * generic "closest living entity" watch goal, so this thin goal fills the
 * row: it scans the same class over the same 8.0F range, keeps the closest
 * candidate (1.7.10 {@code EntityAIWatchClosest.func_75250_a} picks the
 * closest entity of the class through {@code
 * world.selectEntitiesWithinAABB}) and turns the head at the vanilla
 * 30/30 rate.
 */
public class LookAtLivingGoal extends Goal {

    private final Mob mob;
    private final double range;
    private LivingEntity lookedAt;
    private int timeToStart;

    public LookAtLivingGoal(final Mob mob, final double range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.timeToStart > 0) {
            this.timeToStart--;
            return false;
        }
        this.timeToStart = 20 + this.mob.getRandom().nextInt(20); // vanilla look-idle cadence
        this.lookedAt = closestLiving();
        return this.lookedAt != null;
    }

    private LivingEntity closestLiving() {
        final List<LivingEntity> candidates = this.mob.level().getEntitiesOfClass(
                LivingEntity.class, this.mob.getBoundingBox().inflate(this.range),
                candidate -> candidate != this.mob && candidate.isAlive());
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (final LivingEntity candidate : candidates) {
            final double distance = this.mob.distanceToSqr(candidate);
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    @Override
    public boolean canContinueToUse() {
        return this.lookedAt != null && this.lookedAt.isAlive()
                && this.mob.getLookControl().isLookingAtTarget();
    }

    @Override
    public void start() {
        if (this.lookedAt != null) {
            this.mob.getLookControl().setLookAt(this.lookedAt, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        this.lookedAt = null;
    }

    public LivingEntity lookedAt() {
        return this.lookedAt;
    }
}
