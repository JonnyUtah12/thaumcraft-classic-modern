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
import java.util.List;

/**
 * The Fill core's "fetch the item I am watching from a marked chest" goal — the
 * original's {@code thaumcraft.common.entities.ai.inventory.AIFillTake}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIFillTake.java},
 * 108 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 3} for core {@code 0} (Fill, {@code EntityGolemBase.java:229}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIFillTake.java:22}).</li>
 *   <li>Gate: empty hands, a watched item, navigation idle and
 *       {@code hasSomething()} ({@code AIFillTake.java:26-29}) — no
 *       {@code golemDelay} term here.</li>
 *   <li>The candidate pool is the marked containers <em>adjacent</em> to the
 *       golem — {@code GolemHelper.getMarkedContainersAdjacentToGolem}
 *       ({@code AIFillTake.java:36}), whose {@code ADJACENT_RANGE} is the
 *       squared {@code 4.0} (a 2-block radius, {@code GolemHelper.java:89}) —
 *       minus the home container block ({@code AIFillTake.java:38}).</li>
 *   <li>The marker colours come from
 *       {@code getColorsMatching(itemWatched)} and the faces from
 *       {@code getMarkedSides(golem, tile, color)}
 *       ({@code AIFillTake.java:39-40}); the {@code -1} wildcard colour matches
 *       every marker.</li>
 *   <li>The wanted amount is {@code getCarrySpace()} when toggle 0 is set,
 *       otherwise {@code min(itemWatched.count, getCarrySpace())}
 *       ({@code AIFillTake.java:42-44}).</li>
 *   <li>On a hit: {@code countChest = 5}, {@code count = 200}
 *       ({@code AIFillTake.java:70-71}) and the watched item is cleared
 *       ({@code AIFillTake.java:72}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION (BD-34): the original's watched item lives on the entity
 *       ({@code theGolem.itemWatched}, written by
 *       {@code AIFillGoto.ground()}, {@code AIFillGoto.java:61}). This tree
 *       has no such field, so the goal keeps a local copy and seeds it from
 *       {@code GolemHelper.getMissingItems} — the same source the goto reads.
 *       // SEAM: move the watched stack onto the golem and this seed goes away.</li>
 *   <li>DEVIATION: the extraction is the shipped
 *       {@link EntityGolemBase#actHomeTake()} (BD-30) — the original pulled from
 *       the <em>adjacent marked</em> container inside {@code ground()}
 *       ({@code AIFillTake.java:45-47}, {@code doit = true}); the shipped act
 *       drains the <em>home</em> container's need list. The gate keeps a local
 *       simulated extraction so the marker/colour/side/amount semantics stay
 *       the original's; when the shipped act cannot act the goal declines.</li>
 *   <li>DEVIATION: the joined double-chest arm ({@code AIFillTake.java:48-58})
 *       is inert — {@code GolemHelper.getDoubleChest} returns {@code null} in
 *       this tree (BD-16 family). The loop shape is kept.</li>
 *   <li>DEVIATION: the original's {@code theGolem.updateCarried()}
 *       ({@code AIFillTake.java:73}) has no equivalent in this tree.
 *       // SEAM: add the carried-display repaint and call it here.</li>
 * </ul>
 */
public final class AIFillTake extends GolemGoal {

    /** Original {@code countChest} ({@code AIFillTake.java:16}). */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIFillTake.java:17}). */
    private Container inv;

    /** Original {@code count} ({@code AIFillTake.java:18}) — the run budget. */
    private int count = 0;

    /**
     * Stand-in for the original's entity field {@code theGolem.itemWatched}
     * ({@code AIFillTake.java:27}); see the class javadoc.
     */
    private ItemStack watchedItem;

    public AIFillTake(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIFillTake.java:22) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIFillTake.java:25-85}).
     */
    @Override
    public boolean canUse() {
        if (this.golem.getCarried() != null
                || !this.golem.getNavigation().isDone()
                || !this.golem.hasSomething()) {
            return false;
        }
        if (this.watchedItem == null) {
            // DEVIATION/SEAM: seed the watched stack from the goto's own source.
            final List<ItemStack> missing = GolemHelper.getMissingItems(this.golem);
            if (missing == null || missing.isEmpty()) {
                return false;
            }
            this.watchedItem = missing.get(0).copy();
        }
        final BlockPos homeContainerPos = GolemHelper.homeContainerPos(this.golem);
        for (final Container container : GolemHelper.getMarkedContainersAdjacentToGolem(this.level(), this.golem)) {
            if (!(container instanceof BlockEntity tile)) {
                continue;
            }
            if (tile.getBlockPos().equals(homeContainerPos)) {
                continue;
            }
            for (final byte color : this.golem.getColorsMatching(this.watchedItem)) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, color)) {
                    final ItemStack target = this.watchedItem.copy();
                    target.setCount(this.golem.getToggles()[0]
                            ? this.golem.getCarrySpace()
                            : Math.min(target.getCount(), this.golem.getCarrySpace()));
                    // simulate: the original's gate probes, then acts
                    // (AIFillTake.java:45-47 with doit = true).
                    ItemStack result = this.extract(container, target, side, true);
                    if (result == null && doubleChest(tile) != null) {
                        result = this.extract(doubleChest(tile), target, side, true);
                    }
                    if (result == null) {
                        continue;
                    }
                    // DEVIATION: the shipped act method owns the mutation
                    // (BD-30); it drains the home container's need list.
                    if (!this.golem.actHomeTake()) {
                        return false;
                    }
                    try {
                        if (GolemConfig.GOLEM_CHEST_INTERACT) {
                            this.inv.setChanged();
                        }
                    } catch (final RuntimeException ignored) {
                        // The original's try/catch around the chest-interact
                        // call, which is a null dereference on the first pass
                        // (AIFillTake.java:63-68).
                    }
                    this.countChest = 5;
                    this.count = 200;
                    this.watchedItem = null;
                    // SEAM: the original's theGolem.updateCarried() (AIFillTake.java:73).
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIFillTake.java:87-89}) — keeps
     * running while walking, and for the chest window afterwards.
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && (!this.golem.getNavigation().isDone() || this.countChest > 0);
    }

    /**
     * Original {@code updateTask()} ({@code AIFillTake.java:91-95}).
     */
    @Override
    public void tick() {
        this.count--;
        this.countChest--;
    }

    /**
     * Original {@code resetTask()} ({@code AIFillTake.java:97-104}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}). The
     * original's {@code startExecuting()} (startExecuting) is empty
     * ({@code AIFillTake.java:106-107}), so {@code start} is not overridden.
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIFillTake.java:102).
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
     * The goal's copy of the tree's {@code InventoryUtils.extractStack} seam
     * ({@code GolemHelper.java:1000}, package-private), including the
     * {@code areItemStacksEqual} toggle branch
     * ({@code GolemHelper.java:1046}); {@code simulate} is the inverse of the
     * original's {@code doit} flag.
     */
    private ItemStack extract(final Container container, final ItemStack sample, final int side,
                              final boolean simulate) {
        if (sample == null) {
            return null;
        }
        for (final int slot : slotsFor(container, side)) {
            final ItemStack found = container.getItem(slot);
            if (found.isEmpty() || !this.areItemStacksEqual(found, sample)) {
                continue;
            }
            final int amount = Math.min(found.getCount(), sample.getCount());
            if (amount <= 0) {
                continue;
            }
            final ItemStack extracted = found.copy();
            extracted.setCount(amount);
            if (!simulate) {
                found.shrink(amount);
                if (found.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
                container.setChanged();
            }
            return extracted;
        }
        return null;
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
