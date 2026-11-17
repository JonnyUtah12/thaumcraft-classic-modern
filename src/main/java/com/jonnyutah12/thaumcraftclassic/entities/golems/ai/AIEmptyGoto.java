package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Empty core's "walk to a marked chest with room (or a marked block)" goal —
 * the original's {@code thaumcraft.common.entities.ai.inventory.AIEmptyGoto}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIEmptyGoto.java},
 * 127 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 3} for core {@code 1} (Empty, {@code EntityGolemBase.java:236}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEmptyGoto.java:30}).</li>
 *   <li>Gate: a carried stack and {@code tickCount % Config.golemDelay <= 0}
 *       ({@code AIEmptyGoto.java:34}) → {@link GolemConfig#GOLEM_DELAY} = 5.</li>
 *   <li>Container pool: {@code GolemHelper.getContainersWithRoom(world, golem,
 *       color)} probed with the carried stack
 *       ({@code AIEmptyGoto.java:38}); the winner must be the nearest one within
 *       {@code distance <= dmod * dmod}, {@code dmod = getRange()}
 *       ({@code AIEmptyGoto.java:54-55}) — squared — and must not be the home
 *       container block ({@code AIEmptyGoto.java:56}).</li>
 *   <li>Fallback: the golem's own markers whose colour matches
 *       ({@code marker.color == colorx || colorx == -1}) and whose block is
 *       <em>not</em> a container ({@code AIEmptyGoto.java:74-87}).</li>
 *   <li>{@code count = 200} ({@code AIEmptyGoto.java:121}); the stuck-stroll
 *       arm uses {@code RandomPositionGenerator} scale {@code 2} / size
 *       {@code 1} and a fresh budget of {@code 20}
 *       ({@code AIEmptyGoto.java:105-108}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: {@code RandomPositionGenerator.findRandomPosition()(mob, 2, 1)} →
 *       1.21.1's {@link LandRandomPos#getPos(net.minecraft.world.entity.PathfinderMob, int, int)}.</li>
 *   <li>Inert when there is no target: with no carry, no room container and no
 *       matching non-container marker, {@code canUse} returns {@code false} and
 *       no path is issued.</li>
 * </ul>
 */
public final class AIEmptyGoto extends GolemGoal {

    /** Original {@code movePosX/Y/Z} ({@code AIEmptyGoto.java:19-21}). */
    private double movePosX;
    private double movePosY;
    private double movePosZ;

    /** Original {@code dest} ({@code AIEmptyGoto.java:22}). */
    private BlockPos dest;

    /** Original {@code count} ({@code AIEmptyGoto.java:23}) — the walk budget. */
    private int count = 0;

    /** Original {@code prevX/Y/Z} ({@code AIEmptyGoto.java:24-26}). */
    private int prevX;
    private int prevY;
    private int prevZ;

    public AIEmptyGoto(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEmptyGoto.java:30) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIEmptyGoto.java:33-93}).
     */
    @Override
    public boolean canUse() {
        if (this.golem.getCarried() == null || this.golem.tickCount % GolemConfig.GOLEM_DELAY != 0) {
            return false;
        }
        final List<Byte> matchingColors = this.golem.getColorsMatching(this.golem.getCarried());

        for (final byte color : matchingColors) {
            final List<Container> results = GolemHelper.getContainersWithRoom(this.level(), this.golem, color);
            if (results.isEmpty()) {
                continue;
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
                    range = distance;
                    tX = pos.getX();
                    tY = pos.getY();
                    tZ = pos.getZ();
                    this.dest = pos;
                }
            }
            if (this.dest != null) {
                this.movePosX = tX;
                this.movePosY = tY;
                this.movePosZ = tZ;
                return true;
            }
        }

        for (final byte color : matchingColors) {
            // DEVIATION: null-guarded. The original's marker field was never
            // null (its constructor always allocated the list), while this
            // tree's EntityGolemBase.setMarkers(null) is reachable, so an
            // unguarded loop would crash on state the original could not have.
            final java.util.List<Marker> markers = this.golem.getMarkers();
            if (markers == null) {
                break;
            }
            for (final Marker marker : markers) {
                final BlockEntity tile = this.level().getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
                if ((marker.color == color || color == -1) && !(tile instanceof Container)) {
                    this.movePosX = marker.x;
                    this.movePosY = marker.y;
                    this.movePosZ = marker.z;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Original {@code hasExecute()} ({@code AIEmptyGoto.java:95-97}).
     */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.golem.getNavigation().isDone();
    }

    /**
     * Original {@code updateTask()} ({@code AIEmptyGoto.java:99-113}) — burn the
     * budget and stroll when the golem is stuck in its starting block.
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
     * Original {@code resetTask()} ({@code AIEmptyGoto.java:115-118}) —
     * resetTask clears the budget and the destination.
     */
    @Override
    public void stop() {
        this.count = 0;
        this.dest = null;
    }

    /**
     * Original {@code startExecuting()} ({@code AIEmptyGoto.java:120-126}) —
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
}
