package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

/**
 * The golem's melee goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/combat/AIGolemAttackOnCollide.java},
 * Thaumcraft's trimmed copy of 1.7.10's {@code EntityAIAttackOnCollide}.
 *
 * <p>Installed at melee priority {@code 3} for the Guard (core 4) and the Butcher
 * (core 9) — {@code EntityGolemBase.java:252} and {@code :279}
 * ({@code this.tasks.addTask(3, new AIGolemAttackOnCollide(this))}). The original's
 * constructor also self-declares priority 3 ({@code AIGolemAttackOnCollide.java:20},
 * {@code func_75248_a(3)}), so installation index and self-declared priority agree.</p>
 *
 * <h2>Why this is a {@link GolemGoal} and not a {@code MeleeAttackGoal} subclass</h2>
 * Three of the original's numbers cannot be reproduced by overriding 1.21.1's
 * {@code MeleeAttackGoal}, whose fields are private and final:
 * <ul>
 *   <li>speed: the original path-follows at the golem's <em>live</em> speed,
 *       {@code func_70689_ay()} ({@code AIGolemAttackOnCollide.java:42, 55});
 *       {@code MeleeAttackGoal.speedModifier} is fixed at construction, and the
 *       golem's speed changes with the pause flag, the material, the "B"/"P"
 *       decorations and water;</li>
 *   <li>attack cooldown: the original reloads to {@code getAttackSpeed()} =
 *       {@code 20 - (advanced ? 2 : 0)} ({@code AIGolemAttackOnCollide.java:63},
 *       {@code EntityGolemBase.java:1263}); {@code MeleeAttackGoal} hardcodes
 *       {@code adjustedTickDelay(20)} = 10 ({@code MeleeAttackGoal.java:31, 130-133})
 *       and exposes no setter;</li>
 *   <li>re-path delay: the original's counter is {@code 4 + random.nextInt(7)}
 *       ({@code :54}) on a per-tick clock, while {@code MeleeAttackGoal}
 *       throttles {@code canUse} to every 20 game ticks
 *       ({@code MeleeAttackGoal.java:33, 41-45}) — the golem would ignore a fresh
 *       target for up to a second.</li>
 * </ul>
 * The attack range test is the original's own
 * ({@code width * 2 * width * 2 + 1} squared blocks against the target's
 * {@code (x, boundingBox.minY, z)}, {@code :59-62}), not 1.21.1's
 * {@code Mob.isWithinMeleeAttackRange}, and the damage is not hardcoded: it comes
 * from the {@code ATTACK_DAMAGE} attribute through {@code doHurtTarget}
 * (the original's {@code func_70652_k} = {@code attackEntityAsMob}, which reads
 * {@code SharedMonsterAttributes.attackDamage}; the port sets that attribute in
 * {@code EntityGolemBase.applyMaterialTraits()}).
 */
public final class AIGolemAttackOnCollide extends GolemGoal {

    /** The goal the golem is chasing — the original's {@code entityTarget} ({@code :12}). */
    private LivingEntity entityTarget;

    /** The path computed by {@link #canUse()} — the original's {@code entityPathEntity} ({@code :14}). */
    private Path entityPathEntity;

    /** The original's {@code attackTick} ({@code :13}); it attacks when this hits 0. */
    private int attackTick;

    /** The original's {@code counter} ({@code :15}) — the re-path delay, 4 + rand(7). */
    private int counter;

    public AIGolemAttackOnCollide(final EntityGolemBase golem) {
        super(golem);
        // 1.7.10 had no mutex bits; MOVE+LOOK is 1.21.1's MeleeAttackGoal flag set
        // (MeleeAttackGoal.java:37), which is what this goal does — path and look.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Original {@code func_75250_a()} ({@code :23-35}): a target must exist, must pass
     * the golem's own {@code isValidTarget} — and if it does not, the goal <em>clears</em>
     * the target (line 28) — and a path to it must exist.
     */
    @Override
    public boolean canUse() {
        final LivingEntity target = this.golem.getTarget();
        if (target == null) {
            return false;
        }
        if (!GolemTargeting.isValidTarget(this.golem, target)) {
            this.golem.setTarget(null);
            return false;
        }
        this.entityTarget = target;
        this.entityPathEntity = this.golem.getNavigation().createPath(this.entityTarget, 0);
        return this.entityPathEntity != null;
    }

    /** Original {@code func_75253_b()} ({@code :37-39}): re-run {@code shouldExecute} and stop when the path is finished. */
    @Override
    public boolean canContinueToUse() {
        return this.canUse() && !this.golem.getNavigation().isDone();
    }

    /** Original {@code func_75249_e()} ({@code :41-44}): follow the path at the golem's live speed, reset the counter. */
    @Override
    public void start() {
        this.golem.getNavigation().moveTo(this.entityPathEntity, this.golem.getSpeed());
        this.counter = 0;
    }

    /** Original {@code func_75251_c()} ({@code :46-49}): drop the target reference and stop navigating. */
    @Override
    public void stop() {
        this.entityTarget = null;
        this.golem.getNavigation().stop();
    }

    /** 1.7.10 updates every running task each tick, so the original's tick counters are per-tick numbers. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Original {@code updateTask} ({@code func_75246_d()}, {@code :51-72}): look at the
     * target, re-path every {@code 4 + rand(7)} ticks while line of sight holds, then
     * attack when inside the width-derived range and the cooldown has run out.
     */
    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.entityTarget, 30.0F, 30.0F);
        if (this.golem.getSensing().hasLineOfSight(this.entityTarget) && --this.counter <= 0) {
            this.counter = 4 + this.golem.getRandom().nextInt(7);
            this.golem.getNavigation().moveTo(this.entityTarget, this.golem.getSpeed());
        }

        this.attackTick = Math.max(this.attackTick - 1, 0);
        final double width = this.entityTarget.getBbWidth() * 2.0F;
        final double attackRange = width * width + 1.0;
        if (this.golem.distanceToSqr(this.entityTarget.getX(), this.entityTarget.getBoundingBox().minY,
                this.entityTarget.getZ()) <= attackRange && this.attackTick <= 0) {
            this.attackTick = this.attackSpeed();
            final ItemStack held = this.golem.getCarried();
            if (held != null && !held.isEmpty()) {
                this.golem.swing(InteractionHand.MAIN_HAND);
            } else {
                this.golem.startActionTimer();
            }
            this.golem.doHurtTarget(this.entityTarget);
        }
    }

    /**
     * Original {@code EntityGolemBase.getAttackSpeed()} ({@code EntityGolemBase.java:1263}):
     * {@code 20 - (this.advanced ? 2 : 0)} ticks.
     */
    private int attackSpeed() {
        return 20 - (this.golem.isAdvanced() ? 2 : 0);
    }
}
