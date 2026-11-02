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
 * The Sort core's "load up on goods from the home chest before I sort" goal —
 * the original's {@code thaumcraft.common.entities.ai.inventory.AIHomeTakeSorting}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIHomeTakeSorting.java},
 * 155 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 4} for core {@code 10} (Sort, {@code EntityGolemBase.java:287}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIHomeTakeSorting.java:23}).</li>
 *   <li>Gate: empty hands, {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIHomeTakeSorting.java:29}) → {@link GolemConfig#GOLEM_DELAY} = 5,
 *       navigation idle, and {@code !(distanceSq(home + 0.5) > 5.0)}
 *       ({@code AIHomeTakeSorting.java:31}) — squared, ~2.2 blocks.</li>
 *   <li>The need list is {@code GolemHelper.getItemsNeeded(golem,
 *       getUpgradeAmount(5) > 0)} and the list must be non-empty
 *       ({@code AIHomeTakeSorting.java:46-47}); each probe amount is
 *       {@code getCarrySpace()} ({@code AIHomeTakeSorting.java:50}) and the gate
 *       <em>simulates</em> the extraction
 *       ({@code extractStack(..., false)}, {@code AIHomeTakeSorting.java:51-60}).</li>
 *   <li>Only {@code start} extracts for real
 *       ({@code extractStack(..., true)}, {@code AIHomeTakeSorting.java:122-124}),
 *       on the home face ({@code facing.ordinal()}, line 123), sets
 *       {@code countChest = 5} ({@code AIHomeTakeSorting.java:135}) and breaks
 *       the loop once the hands are full ({@code :142-144}).</li>
 *   <li>{@code count = 200} ({@code AIHomeTakeSorting.java:101}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the real extraction is the shipped
 *       {@link EntityGolemBase#actHomeTake()} (BD-17) — the home container, the
 *       home face, the need list, bounded by {@code getCarrySpace()}; it fills
 *       the carry and the golem inventory. The gate keeps the original's local
 *       simulated extraction.</li>
 *   <li>DEVIATION: the joined double-chest arms
 *       ({@code AIHomeTakeSorting.java:67-69}, {@code 147-149}) are inert —
 *       {@code GolemHelper.getDoubleChest} returns {@code null} in this tree
 *       (BD-16 family). The loop shape is kept.</li>
 *   <li>DEVIATION: {@code extractStack} is a local copy of the tree's
 *       package-private seam ({@code GolemHelper.java:1000}); {@code simulate}
 *       is the inverse of the original's {@code doit} flag.</li>
 * </ul>
 */
public final class AIHomeTakeSorting extends GolemGoal {

    /** Original {@code countChest} ({@code AIHomeTakeSorting.java:17}). */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIHomeTakeSorting.java:18}). */
    private Container inv;

    /** Original {@code count} ({@code AIHomeTakeSorting.java:19}) — the run budget. */
    private int count = 0;

    public AIHomeTakeSorting(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIHomeTakeSorting.java:23) — MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIHomeTakeSorting.java:26-79}) —
     * probes the home container with a simulated extraction of each needed
     * stack, at the carry-space bound.
     */
    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.golem.getCarried() != null
                || this.golem.tickCount % GolemConfig.GOLEM_DELAY != 0
                || !this.golem.getNavigation().isDone()
                || this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 5.0) {
            return false;
        }
        final BlockPos containerPos = GolemHelper.homeContainerPos(this.golem);
        final BlockEntity tile = this.level().getBlockEntity(containerPos);
        boolean repeat = true;
        boolean didRepeat = false;
        while (repeat) {
            if (didRepeat) {
                repeat = false;
            }
            if (tile instanceof Container container) {
                final List<ItemStack> neededList =
                        GolemHelper.getItemsNeeded(this.golem, this.golem.getUpgradeAmount(5) > 0);
                if (neededList != null && !neededList.isEmpty()) {
                    for (final ItemStack stack : neededList) {
                        final ItemStack needed = stack.copy();
                        needed.setCount(this.golem.getCarrySpace());
                        // simulate = true: the original's doit = false
                        // (AIHomeTakeSorting.java:51-60).
                        if (this.extract(container, needed, this.golem.getHomeFacing(), true) != null) {
                            return true;
                        }
                    }
                }
            }
            // DEVIATION: GolemHelper.getDoubleChest is null in 21.1.259
            // (BD-16 family), so the original's joined-chest second pass
            // (AIHomeTakeSorting.java:67-69) never runs.
            if (!didRepeat && doubleChest(tile) != null) {
                didRepeat = true;
            } else {
                repeat = false;
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIHomeTakeSorting.java:81-83}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && (this.canUse() || this.countChest > 0);
    }

    /**
     * Original {@code startExecuting()} ({@code AIHomeTakeSorting.java:100-154}) —
     * startExecuting: the real take from the home container.
     */
    @Override
    public void start() {
        this.count = 200;
        final BlockPos containerPos = GolemHelper.homeContainerPos(this.golem);
        final BlockEntity tile = this.level().getBlockEntity(containerPos);
        boolean repeat = true;
        boolean didRepeat = false;
        while (repeat) {
            if (didRepeat) {
                repeat = false;
            }
            if (tile instanceof Container container) {
                final List<ItemStack> neededList =
                        GolemHelper.getItemsNeeded(this.golem, this.golem.getUpgradeAmount(5) > 0);
                if (neededList != null && !neededList.isEmpty()) {
                    // DEVIATION: the shipped act method owns the mutation
                    // (BD-17) — home container, home face, need list, bounded by
                    // getCarrySpace(); the original extracted the first matching
                    // stack inline (AIHomeTakeSorting.java:122-124).
                    if (this.golem.actHomeTake()) {
                        try {
                            if (GolemConfig.GOLEM_CHEST_INTERACT) {
                                container.setChanged();
                            }
                        } catch (final RuntimeException ignored) {
                            // The original's try/catch (AIHomeTakeSorting.java:128-133).
                        }
                        this.countChest = 5;
                        this.inv = container;
                    }
                }
                if (this.golem.getCarried() != null) {
                    break;
                }
            }
            // DEVIATION: the joined double-chest arm
            // (AIHomeTakeSorting.java:147-149) is inert in this tree (BD-16).
            if (!didRepeat && doubleChest(tile) != null) {
                didRepeat = true;
            } else {
                repeat = false;
            }
        }
    }

    /**
     * Original {@code updateTask()} ({@code AIHomeTakeSorting.java:94-98}).
     */
    @Override
    public void tick() {
        this.countChest--;
        this.count--;
    }

    /**
     * Original {@code resetTask()} ({@code AIHomeTakeSorting.java:85-92}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIHomeTakeSorting.java:90).
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
     * ({@code GolemHelper.java:1000}) with the toggle-aware equality
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
