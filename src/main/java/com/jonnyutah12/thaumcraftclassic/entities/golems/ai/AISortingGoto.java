package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Sort core's "walk to the marked chest that already holds what I carry"
 * goal — the original's {@code thaumcraft.common.entities.ai.inventory.AISortingGoto}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AISortingGoto.java},
 * 117 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 3} for core {@code 10} (Sort, {@code EntityGolemBase.java:286}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AISortingGoto.java:30}).</li>
 *   <li>Gate: a carried stack and {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AISortingGoto.java:34}) → {@link GolemConfig#GOLEM_DELAY} = 5.</li>
 *   <li>The pool is {@code GolemHelper.getContainersWithRoom(world, golem,
 *       (byte) -1)} — the {@code -1} wildcard colour, probed with the carried
 *       stack ({@code AISortingGoto.java:35}). A candidate must be within
 *       {@code distance <= dmod * dmod}, {@code dmod = getRange()}
 *       ({@code AISortingGoto.java:54}) — squared — must not be the home
 *       container block ({@code AISortingGoto.java:55}) and must, on one of its
 *       marked faces ({@code -1} colour), <em>already hold</em> the carried
 *       stack ({@code inventoryContains}, {@code AISortingGoto.java:56-59}, with
 *       the golem's {@code checkOreDict}/{@code ignoreDamage}/{@code ignoreNBT}
 *       toggles).</li>
 *   <li>{@code count = 200} ({@code AISortingGoto.java:111}); the stuck-stroll
 *       arm uses {@code RandomPositionGenerator} scale {@code 2} / size
 *       {@code 1} with a fresh budget of {@code 20}
 *       ({@code AISortingGoto.java:95-98}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: {@code RandomPositionGenerator.findRandomPosition()(mob, 2, 1)} →
 *       1.21.1's {@link LandRandomPos#getPos(net.minecraft.world.entity.PathfinderMob, int, int)}.</li>
 *   <li>DEVIATION: {@code inventoryContains} is a local copy of the tree's
 *       package-private seam ({@code GolemHelper.java:1030}).</li>
 *   <li>Inert when there is no target: no carry, no container with room, or none
 *       that already holds the carry, leaves {@code canUse} false and no path is
 *       issued.</li>
 * </ul>
 */
public final class AISortingGoto extends GolemGoal {

    /** Original {@code movePosX/Y/Z} ({@code AISortingGoto.java:19-21}). */
    private double movePosX;
    private double movePosY;
    private double movePosZ;

    /** Original {@code dest} ({@code AISortingGoto.java:22}). */
    private BlockPos dest;

    /** Original {@code count} ({@code AISortingGoto.java:23}) — the walk budget. */
    private int count = 0;

    /** Original {@code prevX/Y/Z} ({@code AISortingGoto.java:24-26}). */
    private int prevX;
    private int prevY;
    private int prevZ;

    public AISortingGoto(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AISortingGoto.java:30) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AISortingGoto.java:33-83}).
     */
    @Override
    public boolean canUse() {
        if (this.golem.getCarried() == null || this.golem.tickCount % GolemConfig.GOLEM_DELAY != 0) {
            return false;
        }
        final List<Container> results =
                GolemHelper.getContainersWithRoom(this.level(), this.golem, (byte) -1);
        if (results.isEmpty()) {
            return false;
        }
        final BlockPos homeContainerPos = GolemHelper.homeContainerPos(this.golem);
        int tX = 0;
        int tY = 0;
        int tZ = 0;
        double range = Double.MAX_VALUE;
        final float dmod = this.golem.getRange();
        for (final Container container : results) {
            if (!(container instanceof BlockEntity tile)) {
                continue;
            }
            final BlockPos pos = tile.getBlockPos();
            final double distance =
                    this.golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < range && distance <= dmod * dmod && !pos.equals(homeContainerPos)) {
                for (final int side : GolemHelper.getMarkedSides(this.golem, tile, (byte) -1)) {
                    if (this.contains(container, this.golem.getCarried(), side)) {
                        tX = pos.getX();
                        tY = pos.getY();
                        tZ = pos.getZ();
                        this.dest = pos;
                        range = distance;
                        break;
                    }
                }
            }
        }
        if (this.dest == null) {
            return false;
        }
        this.movePosX = tX;
        this.movePosY = tY;
        this.movePosZ = tZ;
        return true;
    }

    /**
     * Original {@code hasExecute()} ({@code AISortingGoto.java:85-87}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.golem.getNavigation().isDone();
    }

    /**
     * Original {@code updateTask()} ({@code AISortingGoto.java:89-103}) — burn
     * the budget and stroll when the golem is stuck in its starting block.
     */
    @Override
    public void tick() {
        this.count--;
        if (this.count == 0
                && this.prevX == Mth.floor(this.golem.position().x)
                && this.prevY == Mth.floor(this.golem.position().y)
                && this.prevZ == Mth.floor(this.golem.position().z)) {
            final Vec3 stroll = LandRandomPos.getPos(this.golem, 2, 1);
            if (stroll != null) {
                this.count = 20;
                this.golem.getNavigation().moveTo(stroll.x, stroll.y, stroll.z, this.golem.getSpeed());
            }
        }
    }

    /**
     * Original {@code resetTask()} ({@code AISortingGoto.java:105-108}) —
     * resetTask clears the budget and the destination.
     */
    @Override
    public void stop() {
        this.count = 0;
        this.dest = null;
    }

    /**
     * Original {@code startExecuting()} ({@code AISortingGoto.java:110-116}) —
     * startExecuting: budget, the start block, then the path at the golem's own
     * movement speed.
     */
    @Override
    public void start() {
        this.count = 200;
        final Vec3 here = this.golem.position();
        this.prevX = Mth.floor(here.x);
        this.prevY = Mth.floor(here.y);
        this.prevZ = Mth.floor(here.z);
        this.golem.getNavigation().moveTo(this.movePosX, this.movePosY, this.movePosZ, this.golem.getSpeed());
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200/20 tick budgets need the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The goal's copy of the tree's {@code InventoryUtils.inventoryContains}
     * seam ({@code GolemHelper.java:1030}) with the golem's
     * {@code checkOreDict}/{@code ignoreDamage}/{@code ignoreNBT} toggles
     * ({@code AISortingGoto.java:57-58}).
     */
    private boolean contains(final Container container, final ItemStack sample, final int side) {
        for (final int slot : slotsFor(container, side)) {
            if (this.areItemStacksEqual(container.getItem(slot), sample)) {
                return true;
            }
        }
        return false;
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
}
