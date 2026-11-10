package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * The golem's leash — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIReturnHome.java},
 * Thaumcraft's stall-aware twin of 1.7.10's {@code EntityAIFollowParent} (it reuses that
 * class's random-wander-when-stuck generator, not vanilla's {@code EntityAIHomeAndAnger}).
 *
 * <p>Installed at melee priority {@code 6} in the common tail of
 * {@code setupGolem()} ({@code EntityGolemBase.java:294}), so it is the lowest-priority
 * goal a golem runs — it only steers when nothing else wants the navigation. The
 * original's constructor self-declares priority 3 ({@code AIReturnHome.java:24}), which
 * the installation index 6 overwrote.</p>
 *
 * <h2>The home</h2>
 * 1.7.10 reads {@code func_110172_bL()} (the {@code ChunkCoordinates} home). The port
 * reads {@link EntityGolemBase#getRestrictCenter()} / {@link EntityGolemBase#getRestrictRadius()},
 * which is what the placer sets with {@code restrictTo(center, HOME_RANGE)}; the
 * distance test is against the centre, exactly as the original's
 * {@code distanceSq(home.x + 0.5, home.y + 0.5, home.z + 0.5) < 3.0} ({@code :33, 48}).
 *
 * <h2>Pinned numbers (original lines)</h2>
 * <ul>
 *   <li>the "already home" radius: squared distance {@code 3.0} from the centre block's
 *       middle ({@code :33, 48}) — a squared distance, so about 1.73 blocks;</li>
 *   <li>the pathing delay: {@code pathingDelay = 5} on a failed path, growing by
 *       {@code pathingDelayInc += 5} up to {@code 50}, reset to 5 on a success
 *       ({@code :29-30, 67-72, 86-92}) — a golem that cannot walk home backs off for
 *       longer each time instead of flooding the path finder; and the delay is burned one
 *       per {@code canUse} call, i.e. once per tick while the goal is idle
 *       ({@code :29-31}), which is how 1.7.10's per-tick task scheduling worked;</li>
 *   <li>the stall detector: {@code count = 20} ({@code :62, 81}), counted down per tick
 *       ({@code :55}); when it reaches 0 <b>and</b> the golem is still in the block it
 *       occupied when the goal started ({@code :56-59}, 1.7.10's
 *       {@code EntityAIFollowParent} shape), it gives the golem a random nearby push with
 *       {@code RandomPositionGenerator.findRandomTarget(golem, 2, 1)} →
 *       {@code DefaultRandomPos.getPos(golem, 2, 1)} and re-arms the counter for 20 more
 *       ticks; a failed path to that spot feeds the same escalating delay.</li>
 * </ul>
 *
 * <p>{@code // DEVIATION: this port does not teleport}. 1.7.10's {@code EntityGolemBase}
 * teleported the gole to home from inside its own entity tick (the 48² check at
 * {@code reference/.../golems/EntityGolemBase.java:349-355}), and that is where the
 * teleport lives in this tree too — {@code EntityGolemBase.tick()} (lines 1100-1115)
 * teleports at a squared distance of {@code 48² = 2304}. This goal only navigates, so the
 * two never fight: the goal walks the golem home out to the restrict radius
 * ({@code HOME_RANGE = 32}), and the entity's own teleport only fires at 48 blocks, which
 * is outside the range the goal is even asked to cover.</p>
 */
public final class AIReturnHome extends GolemGoal {

    /** The original's squared "already home" distance ({@code :33, 48}). */
    private static final double HOME_SQUARED = 3.0;

    /** The original's stall budget in ticks ({@code :62, 81}). */
    private static final int STALL_TICKS = 20;

    /** The original's {@code pathingDelayInc} start, step and cap ({@code :16, 68-69, 88-89}). */
    private static final int DELAY_STEP = 5;
    private static final int DELAY_CAP = 50;

    /** The original's {@code movePosX/Y/Z} ({@code :12-14}) — where {@code start()} paths to. */
    private double movePosX;
    private double movePosY;
    private double movePosZ;

    /** The original's {@code pathingDelay}, {@code pathingDelayInc}, {@code count} and stall reference ({@code :15-20}). */
    private int pathingDelay;
    private int pathingDelayInc = DELAY_STEP;
    private int count;
    private int prevX;
    private int prevY;
    private int prevZ;

    public AIReturnHome(final EntityGolemBase golem) {
        super(golem);
        // 1.7.10 set no mutex bits; MOVE is 1.21.1's MoveToBlockGoal flag set
        // (MoveToBlockGoal.java:31), the goal this one resembles.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Original {@code func_75250_a()} ({@code :27-41}): burn the delay, and want to walk
     * home when the delay is spent and the golem is outside the home radius.
     */
    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.pathingDelay > 0) {
            this.pathingDelay--;
        }
        if (this.pathingDelay > 0) {
            return false;
        }
        if (this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) < HOME_SQUARED) {
            return false;
        }
        this.movePosX = home.getX() + 0.5;
        this.movePosY = home.getY() + 0.5;
        this.movePosZ = home.getZ() + 0.5;
        return true;
    }

    /** Original {@code func_75253_b()} ({@code :43-49}): live while the delay is spent, the budget runs, the path lives and the golem is still outside the home radius. */
    @Override
    public boolean canContinueToUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        return this.pathingDelay <= 0
                && this.count > 0
                && !this.golem.getNavigation().isDone()
                && this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) >= HOME_SQUARED;
    }

    /** Original {@code func_75249_e()} ({@code :80-94}): arm the budget, remember the standing block, path home at the golem's live speed. */
    @Override
    public void start() {
        // DEVIATION: navigation only, never a teleport. 1.7.10 teleported the golem home from
        // its entity tick (48 squared blocks), and this tree does the same in EntityGolemBase.tick().
        this.count = STALL_TICKS;
        this.prevX = Mth.floor(this.golem.getX());
        this.prevY = Mth.floor(this.golem.getY());
        this.prevZ = Mth.floor(this.golem.getZ());
        final boolean path = this.golem.getNavigation()
                .moveTo(this.movePosX, this.movePosY, this.movePosZ, this.golem.getSpeed());
        this.applyPathingDelay(path);
    }

    /** Original {@code func_75251_c()} ({@code :51-52}): empty — the original stops nothing, so neither does this. */
    @Override
    public void stop() {
    }

    /** 1.7.10 runs every live task every tick, so the counters are tick counts. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Original {@code func_75246_d()} ({@code :54-78}): burn the budget, and when it runs
     * out with the golem still in the block it started in, path it to a random spot two
     * blocks over ({@code findRandomTarget(golem, 2, 1)}) to work the stall loose.
     */
    @Override
    public void tick() {
        this.count--;
        if (this.count != 0) {
            return;
        }
        if (this.prevX != Mth.floor(this.golem.getX())
                || this.prevY != Mth.floor(this.golem.getY())
                || this.prevZ != Mth.floor(this.golem.getZ())) {
            return;
        }
        final Vec3 wander = DefaultRandomPos.getPos(this.golem, 2, 1);
        if (wander == null) {
            return;
        }
        this.count = STALL_TICKS;
        final boolean path = this.golem.getNavigation()
                .moveTo(wander.x + 0.5, wander.y + 0.5, wander.z + 0.5, this.golem.getSpeed());
        this.applyPathingDelay(path);
    }

    /** The original's escalating back-off, verbatim ({@code :66-73, 86-93}). */
    private void applyPathingDelay(final boolean pathFound) {
        if (pathFound) {
            this.pathingDelayInc = DELAY_STEP;
            return;
        }
        this.pathingDelay = this.pathingDelayInc;
        if (this.pathingDelayInc < DELAY_CAP) {
            this.pathingDelayInc += DELAY_STEP;
        }
    }
}
