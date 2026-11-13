package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.List;

/**
 * The golem "my carry is not wanted, trade it at home" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIHomeReplace}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIHomeReplace.java},
 * 126 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 0} — the highest of the task goals — for cores
 * {@code 0} ({@code EntityGolemBase.java:226}), {@code 1} (line 233),
 * {@code 2} (line 240), {@code 8} (line 270) and {@code 10} (line 283).
 * It is the only goal of this family every inventory core installs, which is
 * why it outranks the place/drop/take goals.</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIHomeReplace.java:22} → {@code setFlags(MOVE, LOOK)}).</li>
 *   <li>The home-distance gate is {@code !(distanceSq(home + 0.5) > 5.0)}
 *       ({@code AIHomeReplace.java:30}) — squared, so ~2.2 blocks.</li>
 *   <li>The tick gate is {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIHomeReplace.java:28}) → {@link GolemConfig#GOLEM_DELAY} = 5.</li>
 *   <li>A carry that is <em>on timeout</em> makes the goal fire unconditionally
 *       ({@code AIHomeReplace.java:35-36}) — the original's escape hatch for a
 *       stack the golem has nowhere to put.</li>
 *   <li>The per-core arms: core 1 asks {@code findSomethingEmptyCore}, core 8
 *       {@code findSomethingUseCore}, core 10 {@code findSomethingSortCore},
 *       each negated ({@code AIHomeReplace.java:39-44}); every other core
 *       compares the carry against {@code getItemsNeeded}
 *       ({@code AIHomeReplace.java:46-59}).</li>
 *   <li>{@code countChest = 5} ({@code AIHomeReplace.java:113}) is the chest
 *       bookkeeping window, decremented in {@code tick}
 *       ({@code AIHomeReplace.java:82}).</li>
 * </ul>
 *
 * <h2>Colour semantics</h2>
 * The stack comparison keeps the original's toggle-aware equality
 * ({@code InventoryUtils.areItemStacksEqual(stack, itemCarried, checkOreDict(),
 * ignoreDamage(), ignoreNBT())}, {@code AIHomeReplace.java:50-52}), and the
 * need list it compares against is the core's own
 * ({@code GolemHelper.getItemsNeeded}); the marker colour array
 * ({@code EntityGolemBase.getColors()}, {@code -1} = the wildcard) is what the
 * <em>other</em> goals of this family route through, and this goal leaves the
 * colours untouched exactly as the original does — it only trades the stack.
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: the container mutation is the shipped
 *       {@link EntityGolemBase#actHomeDrop()} (the parent's BD-30 seam) instead
 *       of the original's inline
 *       {@code InventoryUtils.placeItemStackIntoInventory(getCarried(), tile,
 *       facing.ordinal(), true)} ({@code AIHomeReplace.java:102}); the shipped
 *       act places on the home face, which is the same face the original used.</li>
 *   <li>DEVIATION: the original's joined double-chest arm
 *       ({@code AIHomeReplace.java:119-121}) is inert in this tree because
 *       {@code GolemHelper.getDoubleChest} returns {@code null} (BD-16 family).
 *       The loop shape is kept so the branch survives the port.</li>
 *   <li>DEVIATION: the original's {@code InventoryUtils.areItemStacksEqual} is
 *       package-private in this tree
 *       ({@code GolemHelper.areItemStacksEqual}, {@code GolemHelper.java:1046}),
 *       so the goal carries an identical private copy.</li>
 * </ul>
 */
public final class AIHomeReplace extends GolemGoal {

    /** Original {@code countChest} ({@code AIHomeReplace.java:17}), set to 5 on a deposit. */
    private int countChest = 0;

    /** Original {@code inv} ({@code AIHomeReplace.java:18}) — the chest the goal last touched. */
    private Container inv;

    public AIHomeReplace(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIHomeReplace.java:22) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIHomeReplace.java:25-66}).
     */
    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        final ItemStack carried = this.golem.getCarried();
        if (carried == null
                || this.golem.tickCount % GolemConfig.GOLEM_DELAY != 0
                || !this.golem.getNavigation().isDone()
                || this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 5.0) {
            return false;
        }
        // The original's escape hatch: a carried stack the golem has already
        // failed to place is dropped at home regardless of the core arms
        // (AIHomeReplace.java:35-36).
        if (GolemHelper.isOnTimeOut(this.golem, carried)) {
            return true;
        }
        switch (this.golem.getCore()) {
            case 1:
                return !GolemHelper.findSomethingEmptyCore(this.golem, carried);
            case 8:
                return !GolemHelper.findSomethingUseCore(this.golem, carried);
            case 10:
                return !GolemHelper.findSomethingSortCore(this.golem, carried);
            default:
                final List<ItemStack> neededList =
                        GolemHelper.getItemsNeeded(this.golem, this.golem.getUpgradeAmount(5) > 0);
                if (neededList == null || neededList.isEmpty()) {
                    return false;
                }
                for (final ItemStack stack : neededList) {
                    if (this.matches(stack, carried)) {
                        return false;
                    }
                }
                return true;
        }
    }

    /**
     * Original {@code hasExecute()} ({@code AIHomeReplace.java:68-70}) — the
     * goal keeps running while the chest window is open, even when the gate
     * went false.
     */
    @Override
    public boolean canContinueToUse() {
        return this.canUse() || this.countChest > 0;
    }

    /**
     * Original {@code startExecuting()} ({@code AIHomeReplace.java:86-126}) —
     * startExecuting, the half that trades the carried stack at the home
     * container.
     */
    @Override
    public void start() {
        final BlockPos containerPos = GolemHelper.homeContainerPos(this.golem);
        final BlockEntity tile = this.level().getBlockEntity(containerPos);
        boolean repeat = true;
        boolean didRepeat = false;
        while (repeat) {
            if (didRepeat) {
                repeat = false;
            }
            if (tile instanceof Container container) {
                // DEVIATION: the shipped act method owns the container mutation
                // (BD-30); it places on the home face, the face the original
                // passed as facing.ordinal() (AIHomeReplace.java:102).
                if (this.golem.actHomeDrop()) {
                    this.markDirty(container);
                    this.countChest = 5;
                    this.inv = container;
                    break;
                }
            }
            // DEVIATION: GolemHelper.getDoubleChest is null in 21.1.259 (BD-16
            // family), so the original's joined-chest second pass
            // (AIHomeReplace.java:119-121) never runs.
            if (!didRepeat && doubleChest(tile) != null) {
                didRepeat = true;
            } else {
                repeat = false;
            }
        }
    }

    /**
     * Original {@code updateTask()} ({@code AIHomeReplace.java:81-84}) — the
     * chest window ticks down.
     */
    @Override
    public void tick() {
        this.countChest--;
    }

    /**
     * Original {@code resetTask()} ({@code AIHomeReplace.java:72-79}) —
     * resetTask closes the chest it left open (the original's
     * {@code IInventory.onUpdate()()}, 1.21.1's {@code Container.stopOpen}).
     */
    @Override
    public void stop() {
        try {
            if (this.inv != null && GolemConfig.GOLEM_CHEST_INTERACT) {
                this.inv.stopOpen(null);
            }
        } catch (final RuntimeException ignored) {
            // The original swallowed everything here (AIHomeReplace.java:77).
        }
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 5-tick chest window only survives if 1.21.1's GoalSelector does the
     * same.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** The golem last touched a container; keep it dirty the way the original marked it. */
    private void markDirty(final Container container) {
        try {
            if (GolemConfig.GOLEM_CHEST_INTERACT) {
                container.setChanged();
            }
        } catch (final RuntimeException ignored) {
            // Original: the chest-interact block is wrapped in a try/catch
            // (AIHomeReplace.java:106-111).
        }
    }

    /**
     * The goal's copy of {@code InventoryUtils.areItemStacksEqual(stack1,
     * stack2, checkOreDict(), ignoreDamage(), ignoreNBT())}
     * ({@code AIHomeReplace.java:50-52}); the tree's shared copy
     * ({@code GolemHelper.java:1046}) is package-private. The {@code oredict}
     * toggle is carried by the signature for shape; 1.21.1 has no ore
     * dictionary, and this tree's seam treats it as item identity
     * ({@code GolemHelper.java:1046-1059}).
     */
    private boolean matches(final ItemStack stack, final ItemStack carried) {
        final boolean oredict = this.golem.checkOreDict();
        final boolean ignoreDamage = this.golem.ignoreDamage();
        final boolean ignoreNBT = this.golem.ignoreNBT();
        if (stack == null || carried == null || stack.isEmpty() || carried.isEmpty()) {
            return false;
        }
        if (!ignoreNBT) {
            return ItemStack.isSameItemSameComponents(stack, carried);
        }
        if (!ignoreDamage) {
            return ItemStack.isSameItem(stack, carried) && stack.getDamageValue() == carried.getDamageValue();
        }
        return ItemStack.isSameItem(stack, carried);
    }

    /**
     * DEVIATION: the original's joined-double-chest lookup
     * ({@code InventoryUtils.getDoubleChest}). This tree's copy
     * ({@code GolemHelper.getDoubleChest}, {@code GolemHelper.java:1101}) is
     * package-private to {@code entities.golems} and returns {@code null} for a
     * joined chest in 21.1.259 (the BD-16 family: 1.21.1 resolves the joined
     * half through the block's own container lookup), so the goal keeps a local
     * stub and the original's loop shape.
     */
    private static Container doubleChest(final BlockEntity tile) {
        return null;
    }
}
