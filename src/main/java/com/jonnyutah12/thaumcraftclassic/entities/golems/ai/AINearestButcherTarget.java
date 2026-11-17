package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

/**
 * The Butcher's cull goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/combat/AINearestButcherTarget.java},
 * Thaumcraft's herd-aware twin of {@link AINearestAttackableTarget}.
 *
 * <p>Installed in the <b>target</b> task list at priority {@code 1} for the Butcher
 * (core 9) with {@code (this, 0, true)} — {@code EntityGolemBase.java:280}:
 * {@code randomInterval = 0} (no sampling gate) and {@code mustSee = true}. The
 * original's constructor self-declares priority 3
 * ({@code AINearestButcherTarget.java:31}), overwritten by the installation index.</p>
 *
 * <h2>The two things that make it not the Guard's goal</h2>
 * <ul>
 *   <li>it orders candidates by <b>age</b>, not distance —
 *       {@code AIOldestAttackableTargetSorter.java:16-19} compares
 *       {@code Entity.field_70173_aa} ({@code ticksExisted}) <em>descending</em>, so the
 *       oldest animal of the pen is culled first. 1.21.1's equivalent field is
 *       {@code Entity.tickCount} (public, and the {@code Entity} tick counter the same
 *       field feeds); the comparator below is the original's, verbatim. The
 *       {@code target} field is then handed to {@code mob.setTarget(...)} by the
 *       inherited {@code start()} (the original's {@code func_75249_e()},
 *       {@code :75-78}).</li>
 *   <li>it only commits to a target when the herd is big enough: after the first valid
 *       candidate, it re-queries the world for entities <b>of that candidate's own
 *       class</b> in the same box and counts the valid ones, and returns true only when
 *       that count is {@code > 2} ({@code :50-67}) — a Butcher golem will not spend
 *       itself on a single animal, it needs at least three of the kind in range.</li>
 * </ul>
 *
 * <p>The animal restriction is not in this class: it is the core-9 arm of the golem's
 * own filter ({@code EntityGolemBase.java:1216-1226} →
 * {@link GolemTargeting#isValidTarget}), which accepts animals (1.21.1
 * {@code AgeableMob}: the {@code Animal} tree), refuses hostiles, tamed animals,
 * golems and animals in love. So the search stays over {@code LivingEntity}, exactly as
 * the original searched {@code EntityLivingBase} (line 41), and the filter is what makes
 * it a butcher.</p>
 *
 * <p>{@code // DEVIATION: the herd count is counted over the whole box, including the
 * candidate itself} — the original's second query ({@code :50-54}) re-runs the same
 * {@code getEntitiesWithinAABB(target.getClass(), …)} that already contains the chosen
 * target, so "more than two" means three or more valid animals of that species in the
 * box. The port keeps that arithmetic, including the {@code 4.0} vertical inflation of
 * the first box on the second query (the original reuses {@code targetDistance},
 * {@code :53}).</p>
 */
public final class AINearestButcherTarget extends NearestAttackableTargetGoal<LivingEntity> {

    /** The golem — the original's {@code theGolem} ({@code AINearestButcherTarget.java:13}). */
    private final EntityGolemBase golem;

    /** The original's {@code targetDistance}, re-read from {@code getRange()} every attempt (line 35). */
    private double targetDistance;

    public AINearestButcherTarget(final EntityGolemBase golem, final int randomInterval, final boolean mustSee) {
        // (mob, class, randomInterval, mustSee, mustReach=false, selector) — the original's
        // super(par1, par5, par6) with par6 = false (AINearestButcherTarget.java:25).
        super(golem, LivingEntity.class, randomInterval, mustSee, false, null);
        this.golem = golem;
    }

    /**
     * Original {@code func_75250_a()} ({@code :34-73}): sample, then walk the
     * oldest-first candidate list and accept the first valid candidate whose own species
     * has more than two valid members in the box.
     */
    @Override
    public boolean canUse() {
        this.targetDistance = this.golem.getRange();
        // The original's sampling gate (:36) — with the installed randomInterval of 0 it
        // never gates, and 1.21.1's reducedTickDelay(0) is 0, so the inherited field
        // reproduces it.
        if (this.randomInterval > 0 && this.golem.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        }

        final AABB box = this.getTargetSearchArea(this.targetDistance);
        final List<LivingEntity> candidates = this.golem.level().getEntitiesOfClass(LivingEntity.class, box);
        // AIOldestAttackableTargetSorter: oldest (largest ticksExisted) first.
        candidates.sort((left, right) -> Integer.compare(right.tickCount, left.tickCount));

        for (final LivingEntity candidate : candidates) {
            if (!GolemTargeting.isValidTarget(this.golem, candidate)) {
                continue;
            }
            // DEVIATION: the herd count runs over the whole box and includes the candidate
            // itself, so "more than two" is three or more valid animals of that species. The
            // original's two queries also pass its IEntitySelector (:41-42, 53-55); the
            // installed goal is built as (golem, 0, true), which resolves to a null
            // selector (:25), so there is nothing to filter here.
            int count = 0;
            for (final LivingEntity sameSpecies :
                    this.golem.level().getEntitiesOfClass(candidate.getClass(), box)) {
                if (GolemTargeting.isValidTarget(this.golem, sameSpecies)) {
                    count++;
                }
            }
            if (count > 2) {
                this.target = candidate;
                return true;
            }
        }
        return false;
    }

    /** The original's box: golem range horizontally, 4.0 vertically ({@code :41-42}). */
    @Override
    protected AABB getTargetSearchArea(final double range) {
        return this.golem.getBoundingBox().inflate(this.targetDistance, 4.0, this.targetDistance);
    }
}
