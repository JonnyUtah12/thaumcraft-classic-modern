package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

/**
 * The Guard's "find something to hit" goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java},
 * Thaumcraft's copy of 1.7.10's {@code EntityAINearestAttackableTarget} whose filter
 * is the golem's own {@code isValidTarget}.
 *
 * <p>Installed in the <b>target</b> task list at priority {@code 2} for the Guard
 * (core 4) with {@code (this, 0, true)} — {@code EntityGolemBase.java:254}, i.e.
 * {@code randomInterval = 0} (no sampling gate) and {@code mustSee = true}. The
 * original's constructor self-declares priority 3 ({@code AINearestAttackableTarget.java:30}),
 * which 1.7.10's {@code EntityAITasks.addTask} overwrote with the installation index;
 * the target-priority that matters is 2.</p>
 *
 * <h2>1.7.10 → 1.21.1 mapping</h2>
 * <ul>
 *   <li>the search box: the original inflates the golem's bounding box by
 *       {@code targetDistance = getRange()} horizontally and by {@code 4.0} vertically
 *       ({@code AINearestAttackableTarget.java:34, 41}) — that is exactly 1.21.1's
 *       {@code NearestAttackableTargetGoal.getTargetSearchArea(range)}
 *       ({@code inflate(range, 4.0, range)}), so the override below feeds it the golem
 *       range instead of the {@code FOLLOW_RANGE} attribute, and rebuilds
 *       {@code targetConditions} with the same range;</li>
 *   <li>the nearest-first order: the original sorts with
 *       {@code AINearestAttackableTargetSorter} (distance-squared ascending,
 *       {@code AINearestAttackableTargetSorter.java:16-20}); 1.21.1's
 *       {@code getNearestEntity(…, targetConditions, …)} is the same selection, so the
 *       inherited {@code findTarget} is kept;</li>
 *   <li>the filter: {@code theGolem.isValidTarget(entity)} (line 47) →
 *       {@link GolemTargeting#isValidTarget}, plugged in as the conditions' selector, so
 *       the attack toggles ({@code canAttackHostiles/Animals/Players/Creepers}) and the
 *       type-4 upgrade gate stay in force;</li>
 *   <li>the sampling gate: {@code targetChance > 0 && random.nextInt(targetChance) != 0}
 *       (line 35) is the inherited {@code randomInterval} check
 *       ({@code NearestAttackableTargetGoal.java:52-55}); the ctor passes it through
 *       ({@code reducedTickDelay(0) = 0}, i.e. no gate, as in the original);</li>
 *   <li>continuation: the original inherits 1.7.10's {@code EntityAITarget}
 *       continuation — target alive, within the {@code followRange} attribute, and the
 *       sight memory of {@code 60} unseen ticks when {@code shouldCheckSight} is set
 *       ({@code AITarget.java:34-53}, the same body 1.7.10's {@code EntityAITarget} has).
 *       1.21.1's {@code TargetGoal.canContinueToUse} is the same test with
 *       {@code unseenMemoryTicks = 60} and the {@code FOLLOW_RANGE} attribute, so it is
 *       inherited unchanged.</li>
 * </ul>
 *
 * <p>{@code // DEVIATION: the original applied the sight flag only while continuing,
 * never while searching} ({@code AINearestAttackableTarget.java:33-55} has no sight
 * test; 1.7.10's {@code EntityAITarget} applied it in {@code continueExecuting}).
 * 1.21.1's {@code TargetingConditions.forCombat()} line-of-sight test runs during
 * {@code findTarget}, so a Guard golem with {@code mustSee = true} now needs line of
 * sight <em>before</em> it acquires a target, one tick earlier than the original.</p>
 */
public class AINearestAttackableTarget extends NearestAttackableTargetGoal<LivingEntity> {

    /** The golem — the original's {@code theGolem} ({@code AINearestAttackableTarget.java:12}). */
    protected final EntityGolemBase golem;

    public AINearestAttackableTarget(final EntityGolemBase golem, final int randomInterval, final boolean mustSee) {
        // (mob, class, randomInterval, mustSee, mustReach=false, selector) — the original's
        // super(par1, par5, par6) with par6 = false (AINearestAttackableTarget.java:24).
        super(golem, LivingEntity.class, randomInterval, mustSee, false, null);
        this.golem = golem;
        // The original measures the search in getRange(), not the follow-range attribute
        // (AINearestAttackableTarget.java:34), and the ctor built targetConditions from
        // getFollowDistance(); rebuild it with the golem range and the golem's own filter.
        // DEVIATION: forCombat() runs the sight test during the search; the original applied it
        // only while continuing, so mustSee now bites one tick earlier than it did in 1.7.10.
        this.targetConditions = TargetingConditions.forCombat()
                .range(this.golem.getRange())
                .selector(candidate -> GolemTargeting.isValidTarget(this.golem, candidate));
    }

    /** Original {@code func_75250_a()}: the search box is the golem range inflated 4 blocks up/down. */
    @Override
    protected AABB getTargetSearchArea(final double range) {
        return this.golem.getBoundingBox().inflate(this.golem.getRange(), 4.0, this.golem.getRange());
    }
}
