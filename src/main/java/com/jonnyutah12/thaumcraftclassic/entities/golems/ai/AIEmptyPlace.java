package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * The Empty core's "dump my carry into a marked chest" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIEmptyPlace}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIEmptyPlace.java},
 * 141 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 1} for core {@code 1} (Empty, {@code EntityGolemBase.java:234}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEmptyPlace.java:25}).</li>
 *   <li>Gate: a carried stack and an idle navigation only
 *       ({@code AIEmptyPlace.java:29}) — no {@code golemDelay} and no
 *       home-distance term.</li>
 *   <li>The pool is the marked containers <em>adjacent</em> to the golem
 *       ({@code AIEmptyPlace.java:36}; {@code ADJACENT_RANGE} is the squared
 *       {@code 4.0}, a 2-block radius, {@code GolemHelper.java:89}), minus the
 *       home container block ({@code AIEmptyPlace.java:38}).</li>
 *   <li>Colour comes from {@code getColorsMatching(itemCarried)} and the faces
 *       from {@code getMarkedSides(golem, tile, color)}
 *       ({@code AIEmptyPlace.java:39-40}); {@code -1} is the wildcard colour.</li>
 *   <li>The gate <em>simulates</em> the insert
 *       ({@code placeItemStackIntoInventory(..., false)},
 *       {@code AIEmptyPlace.java:41}) and records the winning block; only
 *       {@code start} inserts for real ({@code AIEmptyPlace.java:103},
 *       {@code doit = true}).</li>
 *   <li>On a real insert: {@code countChest = 5}
 *       ({@code AIEmptyPlace.java:104}) and the loop breaks once the chest
 *       window is open ({@code AIEmptyPlace.java:127-135}); {@code count = 200}
 *       ({@code AIEmptyPlace.java:91}) is the run budget.</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the target container is a marked chest, not the home chest,
 *       so no shipped act method covers it; the goal mutates the container
 *       through its own copy of the tree's
 *       {@code InventoryUtils.placeItemStackIntoInventory} seam
 *       ({@code GolemHelper.java:954}, package-private).</li>
 *   <li>DEVIATION: the joined double-chest arms
 *       ({@code AIEmptyPlace.java:50-60}, {@code 111-125}) are inert —
 *       {@code GolemHelper.getDoubleChest} returns {@code null} in this tree
 *       (BD-22). The loop shape is kept.</li>
 *   <li>DEVIATION: the original assigns {@code itemCarried = null} when the
 *       stack is fully placed; 1.21.1's carry is an {@code ItemStack} that goes
 *       empty, so the port stores {@code null} for an emptied carry
 *       ({@link EntityGolemBase#setCarried}).</li>
 *   <li>DEVIATION (BD-20): the original's {@code theGolem.updateCarried()}
 *       ({@code AIEmptyPlace.java:139}) has no equivalent in this tree.
 *       // SEAM: add the carried-display repaint and call it here.</li>
 * </ul>
 */
public final class AIEmptyPlace extends GolemGoal {

    /** Original {@code countChest} ({@code AIEmptyPlace.java:16}). */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIEmptyPlace.java:17}). */
    private Container inv;

    /** Original {@code xx/yy/zz} ({@code AIEmptyPlace.java:18-20}). */
    private BlockPos dest;

    /** Original {@code count} ({@code AIEmptyPlace.java:21}) — the run budget. */
    private int count = 0;

    public AIEmptyPlace(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEmptyPlace.java:25) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIEmptyPlace.java:28-69}) — finds a
     * marked adjacent container (not the home block) that can take the carry on
     * a marked face, and records it.
     */
    @Override
    public boolean canUse() {
        final ItemStack carried = this.golem.getCarried();
        if (carried == null || !this.golem.getNavigation().isDone()) {
            return false;
        }
        final BlockPos homeContainerPos = GolemHelper.homeContainerPos(this.golem);
        for (final Container container : GolemHelper.getMarkedContainersAdjacentToGolem(this.level(), this.golem)) {
            if (!(container instanceof BlockEntity tile)) {
                continue;
            }
            if (tile.getBlockPos().equals(homeContainerPos)) {
                continue;
            }
            for (final byte color : this.golem.getColorsMatching(carried)) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                    // simulate = true: the original's doit = false
                    // (AIEmptyPlace.java:41).
                    final ItemStack leftover = this.place(carried, container, side, true);
                    if (!ItemStack.matches(leftover, carried)) {
                        this.dest = tile.getBlockPos();
                        return true;
                    }
                }
                // DEVIATION: the joined double-chest probe
                // (AIEmptyPlace.java:50-60) is inert in this tree (BD-16
                // family); the loop shape is kept.
                if (doubleChest(tile) != null) {
                    for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                        final ItemStack leftover = this.place(carried, doubleChest(tile), side, true);
                        // The original accepts the target when the simulated
                        // insert CHANGES it (!areItemStacksEqual,
                        // AIEmptyPlace.java:53-58).
                        if (!ItemStack.matches(leftover, carried)) {
                            this.dest = tile.getBlockPos();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIEmptyPlace.java:71-73}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && (this.canUse() || this.countChest > 0);
    }

    /**
     * Original {@code startExecuting()} ({@code AIEmptyPlace.java:90-140}) —
     * startExecuting: the real insert into the recorded container, on its marked
     * faces, in the golem's matching colours.
     */
    @Override
    public void start() {
        this.count = 200;
        final BlockPos homeContainerPos = GolemHelper.homeContainerPos(this.golem);
        final BlockEntity tile = this.dest == null ? null : this.level().getBlockEntity(this.dest);
        if (tile instanceof Container container && !this.dest.equals(homeContainerPos)) {
            for (final byte color : this.golem.getColorsMatching(this.golem.getCarried())) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                    // DEVIATION: this tree's placeItemStackIntoInventory
                    // (GolemHelper.java:954) is package-private, so the goal
                    // carries its own copy; simulate = false is the original's
                    // doit = true (AIEmptyPlace.java:103).
                    final ItemStack leftover = this.place(this.golem.getCarried(), container, side, false);
                    this.golem.setCarried(leftover == null || leftover.isEmpty() ? null : leftover);
                    this.countChest = 5;
                    this.inv = container;
                    if (this.golem.getCarried() == null) {
                        break;
                    }
                }
                // DEVIATION: the joined double-chest arm
                // (AIEmptyPlace.java:111-125) is inert in this tree (BD-22).
                if (doubleChest(tile) != null && this.golem.getCarried() != null) {
                    for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                        final Container joined = doubleChest(tile);
                        final ItemStack probe = this.place(this.golem.getCarried(), joined, side, true);
                        if (!ItemStack.matches(probe, this.golem.getCarried())) {
                            final ItemStack leftover = this.place(this.golem.getCarried(), joined, side, false);
                            this.golem.setCarried(leftover == null || leftover.isEmpty() ? null : leftover);
                            this.countChest = 5;
                            this.inv = joined;
                            if (this.golem.getCarried() == null) {
                                break;
                            }
                        }
                    }
                }
                if (this.countChest == 5) {
                    try {
                        if (GolemConfig.GOLEM_CHEST_INTERACT) {
                            container.setChanged();
                        }
                    } catch (final RuntimeException ignored) {
                        // The original's try/catch (AIEmptyPlace.java:128-133).
                    }
                    break;
                }
            }
        }
        // SEAM: the original's theGolem.updateCarried() (AIEmptyPlace.java:139).
    }

    /**
     * Original {@code updateTask()} ({@code AIEmptyPlace.java:84-88}).
     */
    @Override
    public void tick() {
        this.countChest--;
        this.count--;
    }

    /**
     * Original {@code resetTask()} ({@code AIEmptyPlace.java:75-82}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIEmptyPlace.java:80).
        }
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200/5 tick counts need the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The goal's copy of the tree's {@code InventoryUtils.placeItemStackIntoInventory}
     * seam ({@code GolemHelper.java:954}); {@code simulate} is the inverse of
     * the original's {@code doit} flag.
     */
    private ItemStack place(final ItemStack stack, final Container container, final int side,
                            final boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        final ItemStack remaining = stack.copy();
        for (final int slot : slotsFor(container, side)) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!sidedCanPlace(container, slot, remaining, side)) {
                continue;
            }
            final ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                final int move = Math.min(remaining.getCount(), container.getMaxStackSize());
                if (!simulate) {
                    final ItemStack placed = remaining.copy();
                    placed.setCount(move);
                    container.setItem(slot, placed);
                    container.setChanged();
                }
                remaining.shrink(move);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                final int space = Math.min(container.getMaxStackSize(), existing.getMaxStackSize())
                        - existing.getCount();
                if (space > 0) {
                    final int move = Math.min(space, remaining.getCount());
                    if (!simulate) {
                        existing.setCount(existing.getCount() + move);
                        container.setChanged();
                    }
                    remaining.shrink(move);
                }
            }
        }
        return remaining;
    }

    private static int[] slotsFor(final Container container, final int side) {
        if (container instanceof WorldlyContainer sided && side >= 0) {
            return sided.getSlotsForFace(GolemHelper.directionOf(side));
        }
        final int[] slots = new int[container.getContainerSize()];
        for (int a = 0; a < slots.length; a++) {
            slots[a] = a;
        }
        return slots;
    }

    private static boolean sidedCanPlace(final Container container, final int slot, final ItemStack stack,
                                         final int side) {
        return !(container instanceof WorldlyContainer sided && side >= 0)
                || sided.canPlaceItemThroughFace(slot, stack, GolemHelper.directionOf(side));
    }

    /**
     * DEVIATION: this tree's {@code GolemHelper.getDoubleChest}
     * ({@code GolemHelper.java:1101}) is package-private and returns
     * {@code null} for a joined double chest in 21.1.259 (BD-22); the
     * goal mirrors it with a local stub so the original's loop shape survives.
     */
    private static Container doubleChest(final BlockEntity tile) {
        return null;
    }
}
