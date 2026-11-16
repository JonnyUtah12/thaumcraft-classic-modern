package com.jonnyutah12.thaumcraftclassic.entities.ai.boss;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityThaumcraftBoss;

/**
 * {@code thaumcraft.common.entities.ai.combat.AIAttackOnCollide} (138 lines). The Thaumcraft class
 * is vanilla 1.7.10's {@code EntityAIAttackOnCollide} copied and given ONE extra field: the
 * {@code classTarget} filter of {@code :24-28} and {@code :36-38}
 * ({@code classTarget.isAssignableFrom(target.getClass())}), plus the goal-flag call
 * {@code func_75248_a(3)} at {@code :33}. Everything else — the 4 + nextInt(7) re-path cadence
 * ({@code :46}), the 10-tick attack cooldown ({@code :131-134}), the {@code failedPathFindingPenalty}
 * growth of 10 ({@code :113}/{@code :118}), the {@code d0 > 1024 → +10}/{@code d0 > 256 → +5}
 * distance penalties ({@code :122-125}) and the {@code +15} re-path failure penalty ({@code :128}) —
 * is the vanilla body, and 1.21.1 ships it as {@link MeleeAttackGoal}.
 *
 * <p>So the port is the vanilla goal plus the two deltas the Thaumcraft class actually adds:
 * the target-class admission and the flag set (MOVE|LOOK, the original's {@code 3}), and it is
 * honest about it: no copied vanilla body that could drift out of step with the platform.</p>
 *
 * <p>The third gate is the family's: the boss's {@code EntityThaumcraftBoss:63-65} rule "the goals
 * tick only while the spawn-in timer is idle" has no 1.21.1 seam (the goal-tick pass
 * {@code Mob#serverAiStep} is {@code final}), so it rides every Thaumcraft goal's {@code canUse}.</p>
 */
public class AIAttackOnCollide extends MeleeAttackGoal {

    private final Class<? extends LivingEntity> classTarget;
    private final Goal ownerGate;

    /**
     * {@code :24-28} — {@code (EntityCreature, Class, double speed, boolean longMemory)}: the class
     * form delegates to the class-less one and stores the filter.
     */
    public AIAttackOnCollide(final PathfinderMob attacker, final Class<? extends LivingEntity> classTarget,
            final double speedTowardsTarget, final boolean longMemory) {
        super(attacker, speedTowardsTarget, longMemory);
        this.classTarget = classTarget;
        this.ownerGate = null;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));                          // :33 flags 3
    }

    public AIAttackOnCollide(final PathfinderMob attacker, final double speedTowardsTarget,
            final boolean longMemory) {
        this(attacker, LivingEntity.class, speedTowardsTarget, longMemory);
    }

    /** {@code :36-38} — the {@code classTarget} admission, on top of vanilla's own gate. */
    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        if (this.classTarget != null && this.mob.getTarget() != null
                && !this.classTarget.isAssignableFrom(this.mob.getTarget().getClass())) {
            return false;
        }
        return bossGoalsMayTick();
    }

    private boolean bossGoalsMayTick() {
        return !(this.mob instanceof EntityThaumcraftBoss boss) || boss.goalsMayTick();
    }

    /** The filter, for the probe that pins the goal table. */
    public Class<? extends LivingEntity> targetClass() {
        return this.classTarget;
    }
}
