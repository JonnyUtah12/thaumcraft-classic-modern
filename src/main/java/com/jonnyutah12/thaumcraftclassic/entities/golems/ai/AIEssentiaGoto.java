package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Alchemy golem's "walk to the jar with room" goal (TC-M6-08-s-b2): the
 * path half of the pair that {@link AIEssentiaEmpty} completes.
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AIEssentiaGoto.java}
 * (77 lines). Installed by {@code setupGolem()} for <b>core 6 (Alchemy) at
 * priority 3</b> ({@code .../common/entities/golems/EntityGolemBase.java:264}:
 * {@code field_75776_a(3, new AIEssentiaGoto(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEssentiaGoto.java:27}) → {@code MOVE|JUMP};</li>
 *   <li>{@code canUse} runs only when {@code tickCount % Config.golemDelay <= 0}
 *       ({@code :31}) — {@code golemDelay = 5} is {@link GolemConfig#GOLEM_DELAY}
 *       in this tree ({@code GolemConfig.java}, pinned from
 *       {@code Config.java:130}) — and only while carrying an aspect with a
 *       non-zero amount ({@code :31}) whose jar {@code GolemHelper.findJarWithRoom}
 *       resolves ({@code :32-38});</li>
 *   <li>{@code canContinueToUse} is {@code count > 0 && !navigation.isDone()}
 *       ({@code :47});</li>
 *   <li>the walk budget is {@code count = 200} ticks
 *       ({@code AIEssentiaGoto.java:71}), and when it runs out with the golem
 *       standing in the same block it started in, a random land pos
 *       {@code (xzDist 2, yDist 1)} is picked and the budget resets to
 *       {@code count = 20} ({@code :60-64});</li>
 *   <li>the speed passed to the navigation is the golem's own
 *       {@code func_70689_ay()} → {@link EntityGolemBase#getSpeed()} ({@code :63, 75}).</li>
 * </ul>
 *
 * <p>The counter is tick-based, so {@link #requiresUpdateEveryTick()} is true:
 * 1.21.1's {@code GoalSelector} throttles {@code tick()} to every third tick
 * unless the goal asks for every tick, and the original's
 * {@code updateTask} ran every tick. The 200/20 budgets are tick counts in the
 * original's terms.</p>
 */
public class AIEssentiaGoto extends GolemGoal {

    /** Original {@code jarX/jarY/jarZ} ({@code AIEssentiaGoto.java:15-17}). */
    private double jarX;
    private double jarY;
    private double jarZ;

    /** Original {@code int count = 0} — the walk budget ({@code AIEssentiaGoto.java:19}). */
    private int count = 0;

    /** Original {@code prevX/prevY/prevZ} — the block the walk started in ({@code :20-22}). */
    private int prevX = 0;
    private int prevY = 0;
    private int prevZ = 0;

    public AIEssentiaGoto(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEssentiaGoto.java:27) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    /**
     * The original's {@code updateTask} ran every tick ({@code AIEssentiaGoto.java:54});
     * 1.21.1 needs the flag to keep the 200/20 tick budgets honest.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Original {@code func_75250_a} ({@code AIEssentiaGoto.java:30-44}). */
    @Override
    public boolean canUse() {
        // Installed only for core 6 (setupGolem's switch) — reproduce that gate.
        if (!coreIs(6)) {
            return false;
        }
        // AIEssentiaGoto.java:31 — the golemDelay modulo gate plus the payload gate.
        if (this.golem.tickCount % GolemConfig.GOLEM_DELAY > 0
                || this.golem.getEssentia() == null || this.golem.getEssentiaAmount() == 0) {
            return false;
        }
        final BlockPos jar = GolemHelper.findJarWithRoom(this.golem);
        if (jar == null) {
            return false;
        }
        this.jarX = jar.getX();
        this.jarY = jar.getY();
        this.jarZ = jar.getZ();
        return true;
    }

    /** Original {@code func_75253_b} ({@code AIEssentiaGoto.java:46-48}). */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.golem.getNavigation().isDone();
    }

    /** Original {@code func_75251_c} ({@code AIEssentiaGoto.java:50-52}): resetTask zeroes the budget. */
    @Override
    public void stop() {
        this.count = 0;
    }

    /** Original {@code func_75249_e} ({@code AIEssentiaGoto.java:70-76}): startExecuting. */
    @Override
    public void start() {
        this.count = 200; // :71
        this.prevX = Mth.floor(this.golem.getX());
        this.prevY = Mth.floor(this.golem.getY());
        this.prevZ = Mth.floor(this.golem.getZ());
        // :75 — walk to the jar the canUse pass resolved, at the golem's own speed.
        this.golem.getNavigation().moveTo(this.jarX, this.jarY, this.jarZ, this.golem.getSpeed());
    }

    /**
     * Original {@code func_75246_d} ({@code AIEssentiaGoto.java:54-68}): burn the
     * budget, and when it runs out with the golem in the block it started in,
     * pick a random land position 2 blocks away (y-range 1) and walk there for
     * 20 more ticks — the original's "unstick" step.
     */
    @Override
    public void tick() {
        this.count--;
        if (this.count == 0
                && this.prevX == Mth.floor(this.golem.getX())
                && this.prevY == Mth.floor(this.golem.getY())
                && this.prevZ == Mth.floor(this.golem.getZ())) {
            // DEVIATION: RandomPositionGenerator.func_75463_a(mob, 2, 1)
            // (AIEssentiaGoto.java:60) is LandRandomPos.getPos(PathfinderMob, 2, 1)
            // in 1.21.1 — the same land-only random-target generator, renamed and
            // moved out of the goal package.
            final Vec3 target = LandRandomPos.getPos(this.golem, 2, 1);
            if (target != null) {
                this.count = 20; // :62
                this.golem.getNavigation().moveTo(target.x, target.y, target.z, this.golem.getSpeed());
            }
        }
        super.tick(); // the original's super.updateTask() (:67)
    }
}
