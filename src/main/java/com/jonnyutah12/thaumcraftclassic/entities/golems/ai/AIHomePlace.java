package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.WorldlyContainer;

import java.util.EnumSet;

/**
 * The golem "put my carry into the home chest" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIHomePlace}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIHomePlace.java},
 * 122 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 1} for core {@code 0} (Fill, {@code EntityGolemBase.java:227})
 * and core {@code 2} (Filter, line 243).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIHomePlace.java:20}).</li>
 *   <li>Gate: {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIHomePlace.java:26}) → {@link GolemConfig#GOLEM_DELAY} = 5, and
 *       {@code !(distanceSq(home + 0.5) > 5.0)} ({@code AIHomePlace.java:28}),
 *       squared, ~2.2 blocks.</li>
 *   <li>The gate <em>simulates</em> the insert
 *       ({@code placeItemStackIntoInventory(..., false)},
 *       {@code AIHomePlace.java:43}) and only {@code start} performs it for real
 *       ({@code AIHomePlace.java:97}, {@code doit = true}).</li>
 *   <li>{@code countChest = 5} ({@code AIHomePlace.java:108}), one decrement per
 *       {@code tick} ({@code AIHomePlace.java:77}); the insert face is
 *       {@code ForgeDirection.getOrientation(homeFacing).ordinal()}, i.e. the
 *       home face.</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the real insert is the shipped
 *       {@link EntityGolemBase#actHomeDrop()} (BD-17) in place of the original's
 *       inline {@code InventoryUtils.placeItemStackIntoInventory(..., true)}; it
 *       uses the same home face, sets the carried leftover and starts the action
 *       timer. The gate keeps a local <em>simulated</em> insert because
 *       {@code actHomeDrop} mutates and a {@code canUse} must not.</li>
 *   <li>DEVIATION: the joined double-chest arm ({@code AIHomePlace.java:49-51},
 *       {@code 114-116}) is inert — {@code GolemHelper.getDoubleChest} returns
 *       {@code null} in this tree (BD-16 family). The loop shape is kept.</li>
 *   <li>DEVIATION: the original's {@code InventoryUtils.placeItemStackIntoInventory}
 *       is package-private in this tree ({@code GolemHelper.java:954}), so the
 *       gate carries an identical private copy; {@code simulate} is the inverse
 *       of the original's {@code doit} flag.</li>
 * </ul>
 */
public final class AIHomePlace extends GolemGoal {

    /** Original {@code countChest} ({@code AIHomePlace.java:15}). */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIHomePlace.java:16}). */
    private Container inv;

    public AIHomePlace(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIHomePlace.java:20) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIHomePlace.java:23-61}) — the gate
     * probes the home container with a simulated insert.
     */
    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.golem.getCarried() == null
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
                // simulate = true: the original passed doit = false
                // (AIHomePlace.java:43).
                final ItemStack leftover = this.place(container, this.golem.getCarried(),
                        this.golem.getHomeFacing(), true);
                if (!ItemStack.matches(leftover, this.golem.getCarried())) {
                    return true;
                }
            }
            // DEVIATION: GolemHelper.getDoubleChest is null in 21.1.259
            // (BD-16 family), so the original's joined-chest second pass
            // (AIHomePlace.java:49-51) never runs.
            if (!didRepeat && doubleChest(tile) != null) {
                didRepeat = true;
            } else {
                repeat = false;
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIHomePlace.java:63-65}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.canUse() || this.countChest > 0;
    }

    /**
     * Original {@code startExecuting()} ({@code AIHomePlace.java:81-121}) —
     * startExecuting: the real insert into the home container.
     */
    @Override
    public void start() {
        // DEVIATION: the shipped act method owns the mutation (BD-17). It
        // inserts on the home face, sets the carried leftover, starts the
        // action timer and reports whether anything moved — the original's
        // inline block (AIHomePlace.java:96-111).
        final BlockEntity tile = this.level().getBlockEntity(GolemHelper.homeContainerPos(this.golem));
        if (this.golem.actHomeDrop() && tile instanceof Container container) {
            try {
                if (GolemConfig.GOLEM_CHEST_INTERACT) {
                    container.setChanged();
                }
            } catch (final RuntimeException ignored) {
                // Original: try/catch around the chest-interact call
                // (AIHomePlace.java:101-106).
            }
            this.countChest = 5;
            this.inv = container;
        }
    }

    /**
     * Original {@code updateTask()} ({@code AIHomePlace.java:76-79}).
     */
    @Override
    public void tick() {
        this.countChest--;
    }

    /**
     * Original {@code resetTask()} ({@code AIHomePlace.java:67-74}) —
     * resetTask closes the chest (1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIHomePlace.java:72).
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
     * The goal's copy of the tree's {@code InventoryUtils.placeItemStackIntoInventory}
     * seam ({@code GolemHelper.java:954}, package-private) — the same loop over
     * the face's slots, the same sided checks, and {@code simulate} as the
     * inverse of the original's {@code doit} flag
     * ({@code InventoryUtils.java:30-90}).
     */
    private ItemStack place(final Container container, final ItemStack stack, final int side,
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
     * DEVIATION: the original's joined-double-chest lookup
     * ({@code InventoryUtils.getDoubleChest}, {@code AIHomePlace.java:49}).
     * This tree's copy ({@code GolemHelper.getDoubleChest},
     * {@code GolemHelper.java:1101}) is package-private to
     * {@code entities.golems} and returns {@code null} for a joined chest in
     * 21.1.259 (the BD-16 family), so the goal keeps a local stub and the
     * original's loop shape.
     */
    private static Container doubleChest(final BlockEntity tile) {
        return null;
    }
}
