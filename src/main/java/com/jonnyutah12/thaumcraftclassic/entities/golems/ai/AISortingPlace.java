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
 * The Sort core's "put goods into the marked chest that already holds them"
 * goal — the original's {@code thaumcraft.common.entities.ai.inventory.AISortingPlace}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AISortingPlace.java},
 * 145 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 1} for core {@code 10} (Sort, {@code EntityGolemBase.java:284}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AISortingPlace.java:25}).</li>
 *   <li>Gate: a carried stack and an idle navigation
 *       ({@code AISortingPlace.java:29}) — no {@code golemDelay} and no
 *       home-distance term.</li>
 *   <li>The pool is the marked containers <em>adjacent</em> to the golem
 *       ({@code AISortingPlace.java:36}; {@code ADJACENT_RANGE} is the squared
 *       {@code 4.0}, a 2-block radius, {@code GolemHelper.java:89}) minus the
 *       home container block ({@code AISortingPlace.java:38}).</li>
 *   <li>The gate queries the marked faces with the {@code (byte) -1} wildcard
 *       colour ({@code AISortingPlace.java:39}) and demands both that the
 *       container can take the carry (a simulated insert changes the stack,
 *       {@code AISortingPlace.java:40-41}) <em>and</em> that it already
 *       <em>holds</em> it ({@code inventoryContains} on the same face,
 *       {@code AISortingPlace.java:42-44}, with the golem's
 *       {@code checkOreDict}/{@code ignoreDamage}/{@code ignoreNBT} toggles).</li>
 *   <li>The real insert in {@code start} uses
 *       {@code getColorsMatching(itemCarried)} colours and their marked sides
 *       ({@code AISortingPlace.java:105-107}), sets {@code countChest = 5}
 *       ({@code AISortingPlace.java:108}, {@code :122}) and breaks once the
 *       window is open ({@code AISortingPlace.java:131-139});
 *       {@code count = 200} ({@code AISortingPlace.java:95}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the mutation is the shipped
 *       {@link EntityGolemBase#actSortingPlace()} (BD-30). The original placed
 *       the <em>carried</em> stack into the recorded container
 *       ({@code AISortingPlace.java:107}, {@code doit = true}); the shipped act
 *       moves goods from the <em>home</em> container into a marked container
 *       that already holds them, bounded by {@code getCarrySpace()}. The goal
 *       keeps the original's destination gate (room + "already holds it" +
 *       marked sides) and the chest bookkeeping.</li>
 *   <li>DEVIATION: the joined double-chest arms
 *       ({@code AISortingPlace.java:52-65}, {@code 115-129}) are inert —
 *       {@code GolemHelper.getDoubleChest} returns {@code null} in this tree
 *       (BD-16 family). The loop shape is kept.</li>
 *   <li>DEVIATION: the gate's {@code placeItemStackIntoInventory} and
 *       {@code inventoryContains} are local copies of the tree's
 *       package-private seam ({@code GolemHelper.java:954},
 *       {@code GolemHelper.java:1030}); {@code simulate} is the inverse of the
 *       original's {@code doit} flag.</li>
 *   <li>DEVIATION: the original's {@code theGolem.updateCarried()}
 *       ({@code AISortingPlace.java:143}) has no equivalent in this tree.
 *       // SEAM: add the carried-display repaint and call it here.</li>
 * </ul>
 */
public final class AISortingPlace extends GolemGoal {

    /** Original {@code countChest} ({@code AISortingPlace.java:16}). */
    private int countChest = 0;

    /** Original {@code inv} ({@code AISortingPlace.java:17}). */
    private Container inv;

    /** Original {@code xx/yy/zz} ({@code AISortingPlace.java:18-20}). */
    private BlockPos dest;

    /** Original {@code count} ({@code AISortingPlace.java:21}) — the run budget. */
    private int count = 0;

    public AISortingPlace(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AISortingPlace.java:25) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AISortingPlace.java:28-73}) — a
     * marked adjacent container (not the home block) that can take the carry on
     * a marked face <em>and</em> already holds it.
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
            for (final int side : GolemHelper.getMarkedSides(this.golem, tile, (byte) -1)) {
                // simulate = true: the original's doit = false
                // (AISortingPlace.java:40).
                final ItemStack leftover = this.place(carried, container, side, true);
                if (!ItemStack.matches(leftover, carried) && this.contains(container, carried, side)) {
                    this.dest = tile.getBlockPos();
                    return true;
                }
            }
            // DEVIATION: the joined double-chest probe
            // (AISortingPlace.java:52-65) is inert in this tree (BD-16 family).
            if (doubleChest(tile) != null) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, (byte) -1)) {
                    final ItemStack leftover = this.place(carried, doubleChest(tile), side, true);
                    if (!ItemStack.matches(leftover, carried) && this.contains(container, carried, side)) {
                        this.dest = tile.getBlockPos();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AISortingPlace.java:75-77}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && (this.canUse() || this.countChest > 0);
    }

    /**
     * Original {@code startExecuting()} ({@code AISortingPlace.java:94-144}) —
     * startExecuting: the sort pass into the recorded container.
     */
    @Override
    public void start() {
        this.count = 200;
        final BlockPos homeContainerPos = GolemHelper.homeContainerPos(this.golem);
        final BlockEntity tile = this.dest == null ? null : this.level().getBlockEntity(this.dest);
        if (tile instanceof Container container && !this.dest.equals(homeContainerPos)) {
            for (final byte color : this.golem.getColorsMatching(this.golem.getCarried())) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                    // DEVIATION: the shipped act method owns the mutation
                    // (BD-30): it moves goods the target already holds out of
                    // the home container, bounded by getCarrySpace(). The
                    // original inserted the carried stack on this side
                    // (AISortingPlace.java:107).
                    if (this.golem.actSortingPlace()) {
                        this.countChest = 5;
                        this.inv = container;
                        break;
                    }
                }
                // DEVIATION: the joined double-chest arm
                // (AISortingPlace.java:115-129) is inert in this tree (BD-16).
                if (doubleChest(tile) != null && this.golem.getCarried() != null) {
                    for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                        final ItemStack probe = this.place(this.golem.getCarried(), doubleChest(tile), side, true);
                        if (!ItemStack.matches(probe, this.golem.getCarried())) {
                            final ItemStack leftover =
                                    this.place(this.golem.getCarried(), doubleChest(tile), side, false);
                            this.golem.setCarried(leftover == null || leftover.isEmpty() ? null : leftover);
                            this.countChest = 5;
                            this.inv = doubleChest(tile);
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
                        // The original's try/catch (AISortingPlace.java:132-137).
                    }
                    break;
                }
            }
        }
        // SEAM: the original's theGolem.updateCarried() (AISortingPlace.java:143).
    }

    /**
     * Original {@code updateTask()} ({@code AISortingPlace.java:88-92}).
     */
    @Override
    public void tick() {
        this.countChest--;
        this.count--;
    }

    /**
     * Original {@code resetTask()} ({@code AISortingPlace.java:79-86}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AISortingPlace.java:84).
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
     * The goal's copy of the tree's {@code InventoryUtils.inventoryContains}
     * seam ({@code GolemHelper.java:1030}), with the golem's
     * {@code checkOreDict}/{@code ignoreDamage}/{@code ignoreNBT} toggles
     * ({@code AISortingPlace.java:42-44}).
     */
    private boolean contains(final Container container, final ItemStack sample, final int side) {
        for (final int slot : slotsFor(container, side)) {
            if (this.areItemStacksEqual(container.getItem(slot), sample)) {
                return true;
            }
        }
        return false;
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

    /** The tree's {@code InventoryUtils.areItemStacksEqual} toggle branch. */
    private boolean areItemStacksEqual(final ItemStack stack, final ItemStack sample) {
        final boolean ignoreDamage = this.golem.ignoreDamage();
        final boolean ignoreNBT = this.golem.ignoreNBT();
        if (stack == null || sample == null || stack.isEmpty() || sample.isEmpty()) {
            return false;
        }
        if (!ignoreNBT) {
            return ItemStack.isSameItemSameComponents(stack, sample);
        }
        if (!ignoreDamage) {
            return ItemStack.isSameItem(stack, sample) && stack.getDamageValue() == sample.getDamageValue();
        }
        return ItemStack.isSameItem(stack, sample);
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
     * {@code null} for a joined double chest in 21.1.259 (BD-16 family); the
     * goal mirrors it with a local stub so the original's loop shape survives.
     */
    private static Container doubleChest(final BlockEntity tile) {
        return null;
    }
}
