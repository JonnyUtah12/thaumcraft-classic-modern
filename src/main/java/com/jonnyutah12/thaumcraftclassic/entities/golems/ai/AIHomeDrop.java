package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The golem "no home chest, throw my carry on the ground" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIHomeDrop}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIHomeDrop.java},
 * 84 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 2} for core {@code 0} (Fill, {@code EntityGolemBase.java:228}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIHomeDrop.java:20}).</li>
 *   <li>Gate: carried stack, navigation idle, and
 *       {@code !(distanceSq(home + 0.5) > 5.0)} ({@code AIHomeDrop.java:25-27}) —
 *       squared, ~2.2 blocks. There is <em>no</em> {@code golemDelay} term here,
 *       unlike the other goals of this family.</li>
 *   <li>The gate additionally requires that the block at the home container pos
 *       is <em>not</em> a container ({@code AIHomeDrop.java:33}): this goal is
 *       the no-chest fallback.</li>
 *   <li>{@code count = 200} ({@code AIHomeDrop.java:59}) is the run budget,
 *       decremented with {@code countChest} in {@code tick}
 *       ({@code AIHomeDrop.java:53-54}); {@code countChest} is only ever
 *       decremented, never set — the original's quirk, kept.</li>
 *   <li>The thrown stack's velocity is aimed at the container centre with a
 *       {@code distance / 3.0} factor and a flat {@code 0.1} lift
 *       ({@code AIHomeDrop.java:73-76}); its age is set to {@code 10}
 *       ({@code AIHomeDrop.java:77}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: 1.7.10 wrote the item's <em>age</em> directly
 *       ({@code item.field_145804_b = 10}, {@code AIHomeDrop.java:77}) so a
 *       player could pick it up at once; 1.21.1's {@code ItemEntity.age} has no
 *       setter, so the port uses the field that plays that role today,
 *       {@link ItemEntity#setPickUpDelay(int)} (1.7.10's
 *       {@code delayBeforePickup}). The goal's own
 *       {@code getAge() < 5} gate ({@code AIItemPickup.java:49}) is unchanged.</li>
 *   <li>DEVIATION: the original's {@code theGolem.updateCarried()}
 *       ({@code AIHomeDrop.java:81}) repaints the carried-stack display; this
 *       tree has no such method.
 *       // SEAM: add the golem's carried-display repaint and call it here.</li>
 *   <li>DEVIATION: the deposit branch. The shipped
 *       {@link EntityGolemBase#actHomeDrop()} (BD-30) is used when the home pos
 *       <em>is</em> a container, which the original's gate excludes
 *       ({@code AIHomeDrop.java:33}) — that arm is a safety net and never runs
 *       behind the original gate.</li>
 * </ul>
 */
public final class AIHomeDrop extends GolemGoal {

    /**
     * Original {@code countChest} ({@code AIHomeDrop.java:14}) — the original
     * only ever decrements it, so it gates through {@code > 0} once and stays
     * negative. Kept as-is.
     */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIHomeDrop.java:15}). */
    private Container inv;

    /** Original {@code count} ({@code AIHomeDrop.java:16}) — the run budget. */
    private int count = 0;

    public AIHomeDrop(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIHomeDrop.java:20) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIHomeDrop.java:23-37}).
     */
    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.golem.getCarried() == null
                || !this.golem.getNavigation().isDone()
                || this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 5.0) {
            return false;
        }
        final BlockEntity tile = this.level().getBlockEntity(GolemHelper.homeContainerPos(this.golem));
        // The original's fallback gate: only when there is no container at the
        // home block (AIHomeDrop.java:33).
        return !(tile instanceof Container);
    }

    /**
     * Original {@code hasExecute()} ({@code AIHomeDrop.java:39-41}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && (this.canUse() || this.countChest > 0);
    }

    /**
     * Original {@code startExecuting()} ({@code AIHomeDrop.java:58-83}) —
     * startExecuting: budget, then hand the stack over.
     */
    @Override
    public void start() {
        this.count = 200;
        // DEVIATION: the shipped act method (BD-30) when the home block is a
        // container. The original's gate rules that case out, so behind a
        // faithful gate this arm is inert.
        if (this.golem.actHomeDrop()) {
            final BlockEntity tile = this.level().getBlockEntity(GolemHelper.homeContainerPos(this.golem));
            if (tile instanceof Container container) {
                this.inv = container;
            }
            this.countChest = 5;
            return;
        }
        final BlockPos containerPos = GolemHelper.homeContainerPos(this.golem);
        final Vec3 here = this.golem.position();
        final double midY = here.y + this.golem.getBbHeight() / 2.0;
        final ItemEntity item = new ItemEntity(this.level(), here.x, midY, here.z, this.golem.getCarried().copy());
        // DEVIATION: the original wrapped this in `if (item != null)`
        // (AIHomeDrop.java:72); 1.21.1's constructor cannot return null.
        final Vec3 centre = Vec3.atCenterOf(containerPos);
        final double distance = here.distanceTo(centre);
        item.setDeltaMovement(new Vec3((centre.x - here.x) * (distance / 3.0),
                0.1 + (centre.y - midY) * (distance / 3.0),
                (centre.z - here.z) * (distance / 3.0)));
        // DEVIATION: 1.7.10 set the item's age (AIHomeDrop.java:77); 1.21.1 has
        // no age setter, so this is the modern pickup-delay field.
        item.setPickUpDelay(10);
        this.level().addFreshEntity(item);
        this.golem.setCarried(null);
        this.golem.startActionTimer();
        // SEAM: the original's theGolem.updateCarried() (AIHomeDrop.java:81).
    }

    /**
     * Original {@code updateTask()} ({@code AIHomeDrop.java:52-56}).
     */
    @Override
    public void tick() {
        this.countChest--;
        this.count--;
    }

    /**
     * Original {@code resetTask()} ({@code AIHomeDrop.java:43-50}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIHomeDrop.java:48).
        }
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200-tick budget needs the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
