package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Empty core's "no chest free, drop my carry at a marked block" goal — the
 * original's {@code thaumcraft.common.entities.ai.inventory.AIEmptyDrop}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIEmptyDrop.java},
 * 80 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 2} for core {@code 1} (Empty, {@code EntityGolemBase.java:235}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEmptyDrop.java:15}).</li>
 *   <li>Gate: a carried stack and an idle navigation
 *       ({@code AIEmptyDrop.java:19}), plus at least one marked non-container
 *       block of a matching colour —
 *       {@code GolemHelper.getMarkedBlocksAdjacentToGolem} whose
 *       {@code ADJACENT_RANGE} is the squared {@code 4.0} (a 2-block radius,
 *       {@code GolemHelper.java:89}).</li>
 *   <li>Colour from {@code getColorsMatching(itemCarried)}
 *       ({@code AIEmptyDrop.java:22}), {@code -1} = the wildcard.</li>
 *   <li>{@code count = 200} ({@code AIEmptyDrop.java:49}), decremented in
 *       {@code tick} ({@code AIEmptyDrop.java:44});
 *       {@code canContinueToUse} re-runs the gate every tick
 *       ({@code AIEmptyDrop.java:37}).</li>
 *   <li>The thrown stack's velocity uses the {@code distance / 3.0} factor with
 *       a flat {@code 0.1} lift ({@code AIEmptyDrop.java:65-67}) and its age is
 *       {@code 10} ({@code AIEmptyDrop.java:68}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION (BD-23): the original compared {@code cc != home} on
 *       {@code ChunkCoordinates} <em>references</em>
 *       ({@code AIEmptyDrop.java:24}, {@code :55}), so it never excluded
 *       anything; the port compares positions ({@link BlockPos#equals}), which is
 *       the branch's stated intent ("not the home block"). The Empty core's
 *       marked-block list never contains the home block in practice, so the
 *       observable behaviour is the same.</li>
 *   <li>DEVIATION: 1.7.10 wrote the item's age directly
 *       ({@code item.field_145804_b = 10}); 1.21.1's {@code ItemEntity.age} has
 *       no setter, so the port uses {@link ItemEntity#setPickUpDelay(int)}, the
 *       field that plays that role today.</li>
 *   <li>DEVIATION (BD-20): the original's {@code theGolem.updateCarried()}
 *       ({@code AIEmptyDrop.java:78}) has no equivalent in this tree.
 *       // SEAM: add the carried-display repaint and call it here.</li>
 *   <li>The original's {@code resetTask()} (resetTask) is empty
 *       ({@code AIEmptyDrop.java:40-41}), so {@code stop} is not overridden.</li>
 * </ul>
 */
public final class AIEmptyDrop extends GolemGoal {

    /** Original {@code count} ({@code AIEmptyDrop.java:11}) — the run budget. */
    private int count = 0;

    public AIEmptyDrop(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEmptyDrop.java:15) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIEmptyDrop.java:18-34}).
     */
    @Override
    public boolean canUse() {
        return dropTarget(this.golem) != null;
    }

    /**
     * Original {@code hasExecute()} ({@code AIEmptyDrop.java:36-38}) — the gate
     * must stay true while the budget lasts.
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && this.canUse();
    }

    /**
     * Original {@code startExecuting()} ({@code AIEmptyDrop.java:48-79}) —
     * startExecuting: budget, then throw the carry at the first marked block.
     */
    @Override
    public void start() {
        this.count = 200;
        final BlockPos target = dropTarget(this.golem);
        if (target != null) {
            final Vec3 here = this.golem.position();
            final double midY = here.y + this.golem.getBbHeight() / 2.0;
            final ItemEntity item = new ItemEntity(this.level(), here.x, midY, here.z,
                    this.golem.getCarried().copy());
            // DEVIATION: the original's `if (item != null)` guard
            // (AIEmptyDrop.java:63) around a constructor result is dropped.
            final Vec3 centre = Vec3.atCenterOf(target);
            final double distance = here.distanceTo(centre);
            item.setDeltaMovement(new Vec3((centre.x - here.x) * (distance / 3.0),
                    0.1 + (centre.y - midY) * (distance / 3.0),
                    (centre.z - here.z) * (distance / 3.0)));
            // DEVIATION: 1.7.10 set the item's age (AIEmptyDrop.java:68); 1.21.1
            // has no age setter, so this is the modern pickup-delay field.
            item.setPickUpDelay(10);
            this.level().addFreshEntity(item);
            this.golem.setCarried(null);
            this.golem.startActionTimer();
        }
        // SEAM: the original's theGolem.updateCarried() (AIEmptyDrop.java:78),
        // which the original ran on every start, drop or not.
    }

    /**
     * Original {@code updateTask()} ({@code AIEmptyDrop.java:43-46}).
     */
    @Override
    public void tick() {
        this.count--;
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200-tick budget needs the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The shared shape of the original's gate loop
     * ({@code AIEmptyDrop.java:22-28} / {@code :53-55}): the first marked
     * non-container block of a colour matching the carry, within the squared
     * {@code ADJACENT_RANGE}, that is not the home block.
     */
    private static BlockPos dropTarget(final EntityGolemBase golem) {
        if (golem.getCarried() == null || !golem.getNavigation().isDone()) {
            return null;
        }
        final BlockPos home = golem.getRestrictCenter();
        for (final byte color : golem.getColorsMatching(golem.getCarried())) {
            for (final BlockPos pos : GolemHelper.getMarkedBlocksAdjacentToGolem(golem.level(), golem, color)) {
                if (!pos.equals(home)) {
                    return pos;
                }
            }
        }
        return null;
    }
}
