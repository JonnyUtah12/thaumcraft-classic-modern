package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ported {@code thaumcraft.common.entities.ai.misc.AIReturnHome} from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIReturnHome.java}
 * (84 lines) — TC-M6-08-s-b2.
 *
 * <p>The original is installed by {@code EntityGolemBase.setupGolem()} at
 * <b>priority 6</b> in the base goal list every core gets (original
 * {@code EntityGolemBase.java:217-296}). Pinned constants, all from the
 * original file: {@code setFlags(3)} → {@code MOVE|LOOK}
 * ({@code AIReturnHome.java:19}); the home-radius gate — activation needs
 * squared distance from the home cell centre {@code >= 3.0}, the same gate
 * keeps the goal continuing ({@code AIReturnHome.java:31, 42-44}); the
 * per-attempt budget {@code count = 20} ({@code AIReturnHome.java:73, 60});
 * the stuck test — budget spent at the same block cell as at {@code start()}
 * ({@code AIReturnHome.java:52-56}); the stuck escape: a random target
 * {@code findRandomTarget(golem, 2, 1)} ({@code AIReturnHome.java:57-59});
 * the pathing-delay ladder: failed {@code moveTo} parks the goal for
 * {@code pathingDelayInc} ticks and grows the gap by 5 up to 50, a success
 * resets it to 5 ({@code AIReturnHome.java:61-68, 74-81}).</p>
 *
 * <p>The goal is the walk half of the golem's leash; the 48² home-range
 * teleport that yanks a far-away golem is the entity's own
 * {@code EntityGolemBase.tick()} branch (original
 * {@code EntityGolemBase.java:402-421}, ported there), not this goal.</p>
 *
 * <h2>1.7.10 → 1.21.1</h2>
 * <p>{@code RandomPositionGenerator.findRandomTarget(theGolem, 2, 1)}
 * ({@code AIReturnHome.java:57}) is {@link DefaultRandomPos#getPos} in
 * 1.21.1 — the same "land" generator the successor vanilla goals use. The
 * speed argument {@code func_70689_ay()} maps to {@code golem.getSpeed()},
 * the established mapping in this tree.</p>
 */
public class AIReturnHome extends GolemGoal {

    /** Original {@code movePosX/Y/Z} — the home-centre target set by canUse. */
    private double movePosX;
    private double movePosY;
    private double movePosZ;

    /** Original {@code pathingDelay}/{@code pathingDelayInc} — the failure ladder. */
    private int pathingDelay = 0;
    private int pathingDelayInc = 5;

    /** Original {@code count} — the 20-tick per-attempt budget. */
    private int count = 0;

    /** Original {@code prevX/Y/Z} — the cell at the current attempt's start. */
    private int prevX = 0;
    private int prevY = 0;
    private int prevZ = 0;

    public AIReturnHome(final EntityGolemBase golem) {
        super(golem);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.pathingDelay > 0) {
            this.pathingDelay--;
        }

        if (this.pathingDelay <= 0
                && !(this.golem.distanceToSqr(home.getX() + 0.5F, home.getY() + 0.5F, home.getZ() + 0.5F) < 3.0)) {
            this.movePosX = home.getX() + 0.5;
            this.movePosY = home.getY() + 0.5;
            this.movePosZ = home.getZ() + 0.5;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        final BlockPos home = this.golem.getRestrictCenter();
        return this.pathingDelay <= 0
                && this.count > 0
                && !this.golem.getNavigation().isDone()
                && this.golem.distanceToSqr(home.getX() + 0.5F, home.getY() + 0.5F, home.getZ() + 0.5F) >= 3.0;
    }

    @Override
    public void stop() {
        // Original func_75251_c is empty (AIReturnHome.java:47-48).
    }

    @Override
    public void tick() {
        this.count--;
        if (this.count == 0
                && this.prevX == Mth.floor(this.golem.getX())
                && this.prevY == Mth.floor(this.golem.getY())
                && this.prevZ == Mth.floor(this.golem.getZ())) {
            final Vec3 var2 = DefaultRandomPos.getPos(this.golem, 2, 1);
            if (var2 != null) {
                this.count = 20;
                final boolean path = this.golem.getNavigation()
                        .moveTo(var2.x + 0.5, var2.y + 0.5, var2.z + 0.5, this.golem.getSpeed());
                if (!path) {
                    this.pathingDelay = this.pathingDelayInc;
                    if (this.pathingDelayInc < 50) {
                        this.pathingDelayInc += 5;
                    }
                } else {
                    this.pathingDelayInc = 5;
                }
            }
        }

        super.tick();
    }

    @Override
    public void start() {
        this.count = 20;
        this.prevX = Mth.floor(this.golem.getX());
        this.prevY = Mth.floor(this.golem.getY());
        this.prevZ = Mth.floor(this.golem.getZ());
        final boolean path = this.golem.getNavigation()
                .moveTo(this.movePosX, this.movePosY, this.movePosZ, this.golem.getSpeed());
        if (!path) {
            this.pathingDelay = this.pathingDelayInc;
            if (this.pathingDelayInc < 50) {
                this.pathingDelayInc += 5;
            }
        } else {
            this.pathingDelayInc = 5;
        }
    }
}
