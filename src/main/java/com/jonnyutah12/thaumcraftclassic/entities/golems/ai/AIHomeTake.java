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
 * The "take what I need out of the home chest" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIHomeTake}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIHomeTake.java},
 * 137 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 4} for core {@code 1} (Empty, {@code EntityGolemBase.java:237})
 * and at priority {@code 4} for core {@code 8} (the Planner/"use" core, line
 * 271).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIHomeTake.java:22}).</li>
 *   <li>Gate: empty hands, {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIHomeTake.java:28}) → {@link GolemConfig#GOLEM_DELAY} = 5,
 *       navigation idle, and {@code !(distanceSq(home + 0.5) > 5.0)}
 *       ({@code AIHomeTake.java:30}) — squared, ~2.2 blocks.</li>
 *   <li>The need list is {@code GolemHelper.getItemsNeeded(golem,
 *       getUpgradeAmount(5) > 0)} ({@code AIHomeTake.java:45}); each candidate
 *       must pass {@code GolemHelper.validTargetForItem}
 *       ({@code AIHomeTake.java:72}) and the probe amount is
 *       {@code getCarrySpace()} ({@code AIHomeTake.java:74}).</li>
 *   <li>The take face is the home face
 *       ({@code facing.ordinal()}, {@code AIHomeTake.java:78}).</li>
 *   <li>On a hit: {@code countChest = 5} ({@code AIHomeTake.java:94}) and
 *       {@code canContinueToUse} is only that window
 *       ({@code AIHomeTake.java:117-119}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the mutation goes through the shipped
 *       {@link EntityGolemBase#actHomeTake()} (BD-30), which drains the home
 *       container's need list into the carry and the golem inventory, bounded by
 *       {@code getCarrySpace()} — the same face, the same bound. The gate keeps
 *       a local simulated extraction so the need-list/valid-target/colour
 *       semantics stay the original's.</li>
 *   <li>SEAM: when the need list is {@code null} the original drained the home
 *       chest through {@code GolemHelper.getFirstItemUsingTimeout}
 *       ({@code AIHomeTake.java:46-68}) — the "take the first item that is not
 *       timing out" arm. That helper is not in this tree, so the port declines
 *       (returns {@code false}) for a null need list.
 *       // SEAM: port getFirstItemUsingTimeout and this arm lights up.</li>
 *   <li>DEVIATION: the joined double-chest arm
 *       ({@code AIHomeTake.java:103-105}) is inert —
 *       {@code GolemHelper.getDoubleChest} returns {@code null} in this tree
 *       (BD-16 family). The loop shape is kept.</li>
 *   <li>The original's {@code startExecuting()} (startExecuting) is empty
 *       ({@code AIHomeTake.java:135-136}), so {@code start} is not overridden.</li>
 * </ul>
 */
public final class AIHomeTake extends GolemGoal {

    /** Original {@code countChest} ({@code AIHomeTake.java:17}) — the window. */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIHomeTake.java:18}). */
    private Container inv;

    public AIHomeTake(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIHomeTake.java:22) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIHomeTake.java:25-115}) — the gate
     * probes the home container for a needed stack the golem can carry; the
     * shipped act method does the taking.
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
                if (neededList == null) {
                    // SEAM: the original's getFirstItemUsingTimeout drain arm
                    // (AIHomeTake.java:46-68) has no equivalent in this tree.
                    return false;
                }
                for (final ItemStack stack : neededList) {
                    if (!GolemHelper.validTargetForItem(this.golem, stack)) {
                        continue;
                    }
                    final ItemStack needed = stack.copy();
                    needed.setCount(this.golem.getCarrySpace());
                    // simulate: the original's gate probes (the original
                    // extracted with doit = true right there,
                    // AIHomeTake.java:75-83).
                    if (this.extract(container, needed, this.golem.getHomeFacing(), true) == null) {
                        continue;
                    }
                    // DEVIATION: the shipped act method owns the mutation
                    // (BD-30): home face, carry-space bound.
                    if (!this.golem.actHomeTake()) {
                        return false;
                    }
                    try {
                        if (GolemConfig.GOLEM_CHEST_INTERACT) {
                            container.setChanged();
                        }
                    } catch (final RuntimeException ignored) {
                        // The original's try/catch (AIHomeTake.java:87-92).
                    }
                    this.countChest = 5;
                    this.inv = container;
                    return true;
                }
            }
            // DEVIATION: GolemHelper.getDoubleChest is null in 21.1.259
            // (BD-16 family), so the original's joined-chest second pass
            // (AIHomeTake.java:103-105) never runs.
            if (!didRepeat && doubleChest(tile) != null) {
                didRepeat = true;
            } else {
                repeat = false;
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIHomeTake.java:117-119}) — only the
     * chest window keeps this goal running.
     */
    @Override
    public boolean canContinueToUse() {
        return this.countChest > 0;
    }

    /**
     * Original {@code updateTask()} ({@code AIHomeTake.java:130-133}).
     */
    @Override
    public void tick() {
        this.countChest--;
    }

    /**
     * Original {@code resetTask()} ({@code AIHomeTake.java:121-128}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIHomeTake.java:126).
        }
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 5-tick chest window needs the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The goal's copy of the tree's {@code InventoryUtils.extractStack} seam
     * ({@code GolemHelper.java:1000}) with the toggle-aware equality
     * ({@code GolemHelper.java:1046}); {@code simulate} is the inverse of the
     * original's {@code doit} flag ({@code AIHomeTake.java:75-83}).
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
