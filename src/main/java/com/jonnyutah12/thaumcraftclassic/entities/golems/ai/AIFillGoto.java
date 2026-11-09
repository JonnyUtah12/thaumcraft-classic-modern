package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Fill core's "walk to a marked chest that holds what I am missing" goal —
 * the original's {@code thaumcraft.common.entities.ai.inventory.AIFillGoto}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIFillGoto.java},
 * 147 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 4} for core {@code 0} (Fill, {@code EntityGolemBase.java:230}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIFillGoto.java:31}).</li>
 *   <li>Gate: empty hands, {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIFillGoto.java:35}) → {@link GolemConfig#GOLEM_DELAY} = 5, and
 *       {@code hasSomething()}.</li>
 *   <li>The candidate pool is {@code GolemHelper.getContainersWithGoods} probed
 *       per colour from {@code getColorsMatching(itemWatched)}
 *       ({@code AIFillGoto.java:60-70}); the last colour wins, the original's
 *       quirk, kept.</li>
 *   <li>A candidate must be the nearest one, within
 *       {@code distance <= dmod * dmod} with {@code dmod = getRange()}
 *       ({@code AIFillGoto.java:86-87}) — squared — and must not be the home
 *       container block ({@code AIFillGoto.java:87}).</li>
 *   <li>{@code count = 200} ({@code AIFillGoto.java:141}) is the walk budget;
 *       when it runs out while the golem is still in the block it started in,
 *       the goal strolls with {@code RandomPositionGenerator} scale {@code 2},
 *       size {@code 1} and a fresh budget of {@code 20}
 *       ({@code AIFillGoto.java:125-128}).</li>
 *   <li>The path speed is the golem's own movement speed
 *       ({@code getDistanceSqToVecy()}, {@code AIFillGoto.java:128}/{@code :145}) →
 *       {@link EntityGolemBase#getSpeed()}.</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION (BD-21): the Entropy-upgrade arm widened the missing list through
 *       {@code OreDictionary.getOreID}/{@code getOres}
 *       ({@code AIFillGoto.java:39-51}). 1.21.1 has no ore dictionary; this
 *       tree's convention for that seam returns the sample alone
 *       ({@code InventoryMob.fuzzyCandidates}, {@code InventoryMob.java:404}),
 *       so the loop keeps its shape and the list is the plain missing items.</li>
 *   <li>DEVIATION (BD-21): the watched item is a goal-local stand-in for the entity
 *       field {@code theGolem.itemWatched} ({@code AIFillGoto.java:61}); the
 *       tree has no such field. {@code AIFillTake} seeds its own copy from the
 *       same {@code getMissingItems} source.
 *       // SEAM: move the watched stack onto the golem.</li>
 *   <li>DEVIATION: {@code RandomPositionGenerator.findRandomPosition()(mob, 2, 1)} is
 *       1.21.1's {@link LandRandomPos#getPos(net.minecraft.world.entity.PathfinderMob, int, int)},
 *       the "random spot around the mob" generator with the same scale/size.</li>
 * </ul>
 */
public final class AIFillGoto extends GolemGoal {

    /** Original {@code movePosX/Y/Z} ({@code AIFillGoto.java:20-22}). */
    private double movePosX;
    private double movePosY;
    private double movePosZ;

    /** Original {@code dest} ({@code AIFillGoto.java:23}). */
    private BlockPos dest;

    /** Original {@code count} ({@code AIFillGoto.java:24}) — the walk budget. */
    private int count = 0;

    /** Original {@code prevX/Y/Z} ({@code AIFillGoto.java:25-27}). */
    private int prevX;
    private int prevY;
    private int prevZ;

    /** Goal-local stand-in for the original's entity field {@code itemWatched}. */
    private ItemStack watchedItem;

    public AIFillGoto(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIFillGoto.java:31) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIFillGoto.java:34-113}) — picks the
     * nearest marked container that can yield a missing stack, excluding the
     * home container block. Inert when there is no such container.
     */
    @Override
    public boolean canUse() {
        if (this.golem.getCarried() != null
                || this.golem.tickCount % GolemConfig.GOLEM_DELAY != 0
                || !this.golem.hasSomething()) {
            return false;
        }
        final List<ItemStack> missing = GolemHelper.getMissingItems(this.golem);
        if (missing == null || missing.isEmpty()) {
            return false;
        }
        // DEVIATION: with the Entropy upgrade (getUpgradeAmount(5) > 0) the
        // original widened each missing stack through
        // OreDictionary.getOreID/getOres (AIFillGoto.java:39-51). 1.21.1 has no
        // ore dictionary, and this tree's convention for that seam
        // (InventoryMob.fuzzyCandidates, InventoryMob.java:404) resolves an
        // entry to the sample alone, so the widened list equals the plain one.
        final List<ItemStack> missingItems = new ArrayList<>();
        for (final ItemStack stack : missing) {
            missingItems.add(stack.copy());
        }

        List<Container> results = new ArrayList<>();
        for (final ItemStack stack : missingItems) {
            this.watchedItem = stack.copy();
            for (final byte color : this.golem.getColorsMatching(this.watchedItem)) {
                // The last colour's list wins — the original's shape
                // (AIFillGoto.java:63-65).
                results = GolemHelper.getContainersWithGoods(this.level(), this.golem, this.watchedItem, color);
            }
            if (!results.isEmpty()) {
                break;
            }
        }
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
            final double distance = this.golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < range && distance <= dmod * dmod && !pos.equals(homeContainerPos)) {
                range = distance;
                tX = pos.getX();
                tY = pos.getY();
                tZ = pos.getZ();
                this.dest = pos;
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
     * Original {@code hasExecute()} ({@code AIFillGoto.java:115-117}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.golem.getNavigation().isDone();
    }

    /**
     * Original {@code startExecuting()} ({@code AIFillGoto.java:140-146}) —
     * startExecuting: budget, the block the walk started in, then the path.
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
     * Original {@code updateTask()} ({@code AIFillGoto.java:119-133}) — burn the
     * budget; when it runs out and the golem has not moved out of the starting
     * block, stroll to break the stuck path.
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
     * Original {@code resetTask()} ({@code AIFillGoto.java:135-138}) —
     * resetTask clears the destination and the budget.
     */
    @Override
    public void stop() {
        this.dest = null;
        this.count = 0;
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200/20 tick budgets need the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** The watched stack this goal resolved on its last gate pass, for tests. */
    public ItemStack getWatchedItem() {
        return this.watchedItem == null ? null : this.watchedItem.copy();
    }
}
