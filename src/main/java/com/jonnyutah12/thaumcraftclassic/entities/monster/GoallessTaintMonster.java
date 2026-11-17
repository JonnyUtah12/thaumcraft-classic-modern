package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * TC-M6-06-s-e: the shared base of the taint monsters whose behaviour the
 * original hand-rolls (thaumslime, the taintacle family, the spore/swarmer and
 * the taint swarm).
 *
 * <p>All seven are 1.7.10 {@code EntityMob} subclasses, and every one of them
 * installs <em>no goal selectors at all</em>: the decompiled sources contain no
 * {@code goalSelector}/{@code targetSelector} calls, because the jump, the
 * target search, the attack and the steering are written by hand in
 * {@code func_70071_h_} (onUpdate) and {@code func_70626_be}
 * (updateEntityActionState). 1.21.1's {@link Monster#registerGoals()} is the
 * opposite — it installs a look-at-player row, a random-look row and a
 * {@code NearestAttackableTargetGoal} on the player — so the port overrides the
 * method and deliberately does NOT call super. The goal selectors stay empty,
 * which is what {@code TaintMonsterGameTest} asserts for every mob in this
 * slice.</p>
 *
 * <p>The hook mapping the slice uses throughout (the same convention as
 * {@link EntityTaintedAnimalBase} from TC-M6-06-s-b):</p>
 * <ul>
 *   <li>1.7.10 {@code func_70071_h_} / {@code onUpdate} → {@link #tick()}, with
 *       the {@code world.field_72995_K} (isRemote) halves split into a client
 *       and a server body so a game-test server can prove both;</li>
 *   <li>1.7.10 {@code func_70626_be} / updateEntityActionState →
 *       {@link #customServerAiStep()}, which is the 1.21.1 seam that runs after
 *       the goal selectors and before the move/look/jump controls, exactly like
 *       the original's slot inside {@code onLivingUpdate}; the original's
 *       {@code func_70623_bb()} (onEntityUpdate: potion ticks, attack/hurt
 *       counters) is plain 1.21.1 plumbing inside {@code LivingEntity.aiStep}.</li>
 * </ul>
 */
public abstract class GoallessTaintMonster extends Monster implements ITaintedMob {

    protected GoallessTaintMonster(final EntityType<? extends Monster> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty — see the javadoc. The originals install no goals;
        // 1.21.1's Monster does, so NOT calling super is the faithful port.
    }

    /**
     * The original's {@code func_70685_l(entity)} target gate (e.g.
     * EntityTaintacle.java:144, EntityThaumicSlime.java:276): a target may be
     * attacked when it is a living, non-dead, non-invulnerable entity of a type
     * this mob is allowed to attack. 1.21.1 splits that check across
     * {@code Mob#canAttackType} plus the alive/invulnerable probes, so the
     * slices express the original gate once, here.
     */
    protected boolean canAttackTarget(final net.minecraft.world.entity.LivingEntity target) {
        return target != null
                && target != this
                && target.isAlive()
                && !target.isInvulnerableTo(this.level().damageSources().generic())
                && this.canAttackType(target.getType());
    }
}
