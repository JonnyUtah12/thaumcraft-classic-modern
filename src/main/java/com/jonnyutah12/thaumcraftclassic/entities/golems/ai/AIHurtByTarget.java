package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

/**
 * The Guard's retaliation goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/combat/AIHurtByTarget.java},
 * Thaumcraft's copy of 1.7.10's {@code EntityAIHurtByTarget} built on Thaumcraft's own
 * {@code AITarget} base ({@code .../ai/combat/AITarget.java}).
 *
 * <p>Installed in the <b>target</b> task list at priority {@code 1} for the Guard
 * (core 4) — {@code EntityGolemBase.java:253}: {@code new AIHurtByTarget(this, false)}.
 * The original's constructor self-declares priority 1 ({@code AIHurtByTarget.java:15}),
 * matching the installation index.</p>
 *
 * <h2>The dropped boolean, and why the port does not need it</h2>
 * The original's constructor is {@code AIHurtByTarget(EntityCreature, boolean par2)}
 * ({@code :12-16}); {@code par2} is 1.7.10 vanilla {@code EntityAIHurtByTarget}'s
 * "alert the village" flag — when true, {@code startExecuting} walks every entity of the
 * <em>same class</em> inside {@code targetDistance} × 4 blocks up/down and gives the
 * attacker to each of them that has no target yet
 * ({@code AIHurtByTarget.java:30-49}, the same loop 1.7.10's
 * {@code EntityAIHurtByTarget.alertSameType} runs). The golem passes {@code false}
 * ({@code EntityGolemBase.java:253}), so that whole loop is dead code for golems: a golem
 * never recruits other golems. 1.21.1 keeps the same behaviour behind
 * {@code HurtByTargetGoal.setAlertOthers(...)} ({@code HurtByTargetGoal.java:71-75,
 * 87-127}), which the port deliberately never calls, so the flag has nothing to select
 * and the constructor takes only the golem.
 *
 * <h2>What is kept from {@code AITarget}</h2>
 * <ul>
 *   <li>the follow range: the original builds its base with
 *       {@code super(par1EntityLiving, 16.0F, false)} ({@code :13}) and its continuation
 *       test compares the squared distance against that {@code 16.0}
 *       ({@code AITarget.java:40}). 1.21.1 reads the range from
 *       {@code Attributes.FOLLOW_RANGE} (32 for this golem), so {@link #getFollowDistance()}
 *       is overridden back to 16.</li>
 *   <li>no sight requirement: the same constructor passes {@code shouldCheckSight =
 *       false}, so the original's continuation never drops a target for being unseen
 *       ({@code AITarget.java:43-49} is skipped). 1.21.1's {@code HurtByTargetGoal}
 *       hardcodes {@code mustSee = true}, so {@code unseenMemoryTicks} is pushed past any
 *       reachable count — the sight branch can then never fail, which is the original's
 *       behaviour. (With {@code mustSee = false} the original also never used its
 *       {@code 60}-tick unseen memory, {@code AITarget.java:46}.)</li>
 *   <li>the suitability filter: {@code isSuitableTarget(target, false)}
 *       ({@code AITarget.java:65-121}) is ported in {@link #isSuitableTarget}: not self,
 *       alive, {@code canAttackClass} → 1.21.1 {@code canAttackType}, creative players
 *       refused ({@code capabilities.disableDamage} → {@code Player.isCreative()}), the
 *       owner refused by name, and the home-range test
 *       ({@code func_110176_b} → {@link EntityGolemBase#isWithinRange}). The
 *       {@code EntityTameable} arm of that filter ({@code :75-82}) is dead for a golem,
 *       which is not a {@code TamableAnimal}, exactly as it was dead in 1.7.10; the
 *       "can I reach it" arm ({@code :104-116}) is the base class's {@code par4 = false}
 *       ({@code AITarget.java:23-31}), also off, as in 1.7.10's
 *       {@code EntityAIAttackTarget}.</li>
 *   <li>the creative cleanup: {@code resetTask} clears the target when it is a creative
 *       player ({@code :54-61}). Kept, even though 1.7.10's {@code AITarget.resetTask}
 *       cleared the target unconditionally ({@code :61-63}) and 1.21.1's
 *       {@code TargetGoal.stop} does the same — the branch is a no-op in both versions,
 *       and porting it keeps the file readable against the original.</li>
 * </ul>
 *
 * <p>{@code // DEVIATION: the original has no "new attacker" timestamp gate}
 * ({@code AIHurtByTarget.java:18-20} fires on any non-null hurt-by memory; 1.7.10's
 * vanilla {@code EntityAIHurtByTarget} compared a stored timestamp, Thaumcraft's copy
 * dropped it). The port keeps the original: a Guard golem re-acquires the last thing that
 * hurt it, which is why retaliation outlives the sight/range drop-outs of the
 * {@code NearestAttackableTarget} goal. It also means the golem's attack toggles do not
 * gate retaliation — the original never consulted {@code isValidTarget} here, so a golem
 * turns on whatever hit it, animal or player included.</p>
 */
public final class AIHurtByTarget extends HurtByTargetGoal {

    /**
     * The conditions 1.21.1's own {@code HurtByTargetGoal} uses
     * ({@code HurtByTargetGoal.java:20}, {@code forCombat().ignoreLineOfSight()
     * .ignoreInvisibilityTesting()}) — the original's base is 1.7.10's
     * {@code EntityAIHurtByTarget}, which is the same code.
     */
    private static final TargetingConditions HURT_BY_TARGETING =
            TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    /** The golem — the original's {@code taskOwner} / {@code entityPathNavigate} ({@code :10}). */
    private final EntityGolemBase golem;

    public AIHurtByTarget(final EntityGolemBase golem) {
        super(golem);
        this.golem = golem;
        // The original's super(creature, 16.0F, false): shouldCheckSight = false
        // (AIHurtByTarget.java:13). HurtByTargetGoal hardcodes mustSee = true, so make the
        // unseen-memory branch unreachable-by-count instead of editing the flag.
        this.setUnseenMemoryTicks(Integer.MAX_VALUE);
    }

    /**
     * Original {@code func_75250_a()} ({@code :18-20}):
     * {@code isSuitableTarget(taskOwner.getAITarget(), false)} — 1.21.1's hurt-by memory is
     * {@code Mob.getLastHurtByMob()} (1.7.10's {@code func_70643_av()}).
     */
    @Override
    public boolean canUse() {
        // DEVIATION: no "new attacker" timestamp gate — the original's copy of 1.7.10's
        // EntityAIHurtByTarget dropped it, so any non-null hurt-by memory fires the goal.
        final LivingEntity attacker = this.golem.getLastHurtByMob();
        if (!this.isSuitableTarget(attacker)) {
            return false;
        }
        return this.canAttack(attacker, HURT_BY_TARGETING);
    }

    /**
     * Original {@code func_75249_e()} ({@code :27-52}): set the attacker as the target,
     * skip the (dead, {@code false}) alert-others loop, then run the base start.
     */
    @Override
    public void start() {
        super.start();
        // HurtByTargetGoal.start() raises unseenMemoryTicks to 300 (HurtByTargetGoal.java:80);
        // the original passes shouldCheckSight = false, so keep that branch unreachable.
        this.setUnseenMemoryTicks(Integer.MAX_VALUE);
    }

    /**
     * Original {@code func_75253_b()} ({@code :22-25}): the goal lives while the golem
     * still remembers an attacker. 1.21.1's inherited continuation is the stricter
     * {@code TargetGoal} test (alive, within {@link #getFollowDistance()}, and — with the
     * unseen memory disabled above — sight-independent), so the memory check runs on top
     * of it and the goal then stops exactly where the original's stops.
     */
    @Override
    public boolean canContinueToUse() {
        return this.golem.getLastHurtByMob() != null && super.canContinueToUse();
    }

    /** Original {@code func_75251_c()} ({@code :54-61}): a creative player is never kept as a target. */
    @Override
    public void stop() {
        final LivingEntity target = this.golem.getTarget();
        if (target instanceof Player player && player.isCreative()) {
            this.golem.setTarget(null);
        }
        super.stop();
    }

    /**
     * The original's follow range: {@code 16.0} from the constructor's
     * {@code super(par1EntityLiving, 16.0F, false)} ({@code :13}), used by the
     * continuation test at {@code AITarget.java:40}.
     */
    @Override
    protected double getFollowDistance() {
        return 16.0;
    }

    /**
     * Thaumcraft's {@code AITarget.isSuitableTarget(EntityLivingBase, boolean par2)} with
     * {@code par2 = false} ({@code AITarget.java:65-121}): the {@code par2} flag is the
     * "attack creative players too" switch, and the golem calls it with {@code false}.
     */
    private boolean isSuitableTarget(final LivingEntity target) {
        if (target == null || target == this.golem) {
            return false;
        }
        if (!target.isAlive()) {
            return false;
        }
        // Original :72 — func_70686_a(Class) is 1.21.1's canAttackType(EntityType).
        if (!this.golem.canAttackType(target.getType())) {
            return false;
        }
        // Original :84 — a creative player (capabilities.disableDamage) is not a target.
        if (target instanceof Player player && player.isCreative()) {
            return false;
        }
        // Original :88-91 — the golem's owner is never a target.
        if (target instanceof Player player && player.getGameProfile().getName().equals(this.golem.getOwnerName())) {
            return false;
        }
        // Original :94-99 — func_110176_b(floor(x), floor(y), floor(z)) → the home-range test.
        return this.golem.isWithinRange(target.blockPosition());
    }
}
