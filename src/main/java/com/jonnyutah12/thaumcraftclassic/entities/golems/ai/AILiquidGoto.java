package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.EnumSet;
import java.util.List;

/**
 * The Decanting golem's "go stand by the fluid I am missing" goal
 * (TC-M6-08-s-b2): the path half of the pair that {@link AILiquidGather}
 * completes, and the goal that sets the golem's watch.
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AILiquidGoto.java}
 * (95 lines). Installed by {@code setupGolem()} for <b>core 5 (Decanting) at
 * priority 3</b> ({@code .../common/entities/golems/EntityGolemBase.java:259}:
 * {@code field_75776_a(3, new AILiquidGoto(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AILiquidGoto.java:29}) → {@code MOVE|JUMP};</li>
 *   <li>{@code canUse} runs on the {@code tickCount % Config.golemDelay <= 0}
 *       cadence ({@code :33}, {@link GolemConfig#GOLEM_DELAY} = 5 in this tree)
 *       and only while the belly has room:
 *       {@code liquidCarried == null || amount <= getFluidCarryLimit() - 1000}
 *       ({@code :34}) — the 1000 mB (one bucket) headroom the original insists on;</li>
 *   <li>the 3×3 search around the fluid for a standable spot is a plain
 *       non-squared {@code distanceTo} comparison and an {@code isEmptyBlock}
 *       test ({@code :44-54});</li>
 *   <li>the watch is set on the golem before the walk starts
 *       ({@code itemWatched = new ItemStack(Item.getItemById(fluid.fluidID), 1, fluid.amount)},
 *       {@code :40-41}) — see the DEVIATION below;</li>
 *   <li>the walk budget is {@code count = 200} ticks ({@code :88}) and the
 *       unstick walk is {@code RandomPositionGenerator(mob, 2, 1)} with
 *       {@code count = 20} ({@code :78-82});</li>
 *   <li>{@code canContinueToUse} is {@code count > 0 && !navigation.isDone()}
 *       ({@code :64}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li><b>The watch item.</b> The original encodes "the fluid I am fetching" as
 *       an {@code ItemStack(itemById(fluidID), 1, amount)} — a 1.7.10 fluid id
 *       used as an item id with the amount in the damage value. 1.21.1 has no
 *       such registry alias (and {@code FluidUtil.BUCKET} is not public), so the
 *       port stores the {@link FluidStack} itself in the shared per-golem watch
 *       ({@link AILiquidGather#watchFluid(EntityGolemBase, FluidStack)}), which
 *       is what {@link AILiquidGather} reads. Same information, no forged item.</li>
 *   <li>{@code RandomPositionGenerator.func_75463_a(mob, 2, 1)} is
 *       {@link LandRandomPos#getPos(net.minecraft.world.entity.PathfinderMob, int, int)}.</li>
 *   <li>The water/lava constants resolve to {@link Fluids#WATER}/{@link Fluids#LAVA};
 *       this goal never compares fluid ids directly, so no mapping is needed.</li>
 * </ul>
 */
public class AILiquidGoto extends GolemGoal {

    /**
     * Original {@code jarX/jarZ} ({@code AILiquidGoto.java:16-17}): the standable
     * spot the 3×3 search picks. The original computes them and — verbatim here —
     * only the walk to the fluid position uses the search's outcome; the pair is
     * kept because the search's cost is the original's behaviour.
     */
    private double jarX;
    private double jarZ;

    /** Original {@code waterX/waterY/waterZ} ({@code :19-21}) — the fluid's position. */
    private double waterX;
    private double waterY;
    private double waterZ;

    /** Original {@code int count = 0} — the walk budget ({@code :23}). */
    private int count = 0;

    /** Original {@code prevX/prevY/prevZ} — the block the walk started in ({@code :24-26}). */
    private int prevX = 0;
    private int prevY = 0;
    private int prevZ = 0;

    public AILiquidGoto(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AILiquidGoto.java:29) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /** The counters are tick-based (200/20 budgets), so 1.21.1 must call {@code tick()} every tick. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Original {@code func_75250_a} ({@code AILiquidGoto.java:32-61}). */
    @Override
    public boolean canUse() {
        // Installed only for core 5 (setupGolem's switch) — reproduce that gate.
        if (!coreIs(5)) {
            return false;
        }
        final FluidStack carried = this.golem.getFluidCarried();
        // :33-34 — the golemDelay cadence, and one bucket of headroom in the belly.
        if (this.golem.tickCount % GolemConfig.GOLEM_DELAY > 0
                || (carried != null && carried.getAmount() > this.golem.getFluidCarryLimit() - 1000)) {
            return false;
        }
        final List<FluidStack> fluids = GolemHelper.getMissingLiquids(this.golem);
        if (fluids == null) {
            return false;
        }
        for (final FluidStack fluid : fluids) {
            final BlockPos source = GolemHelper.findPossibleLiquid(fluid, this.golem);
            if (source == null) {
                continue;
            }
            // :40-41 — the golem's watch is the fluid it set out to fetch.
            // DEVIATION: stored as the FluidStack, not an ItemStack(itemById(fluidID))
            // — see the class javadoc.
            AILiquidGather.watchFluid(this.golem,
                    new FluidStack(fluid.getFluid(), fluid.getAmount()));
            this.waterX = source.getX();
            this.waterY = source.getY();
            this.waterZ = source.getZ();

            // :43-56 — look for a standable block in the 3×3 around the fluid,
            // closest to the golem. Plain distanceTo (not squared), as the original has it.
            double dd = this.golem.position().distanceTo(new Vec3(source.getX(), source.getY(), source.getZ()));
            for (int xx = -1; xx <= 1; xx++) {
                for (int zz = -1; zz <= 1; zz++) {
                    final double dd2 = this.golem.position().distanceTo(
                            new Vec3(source.getX() + xx, this.waterY, source.getZ() + zz));
                    if (dd2 < dd
                            && this.level().isEmptyBlock(new BlockPos(source.getX() + xx,
                            (int) this.waterY, source.getZ() + zz))) {
                        this.jarX = source.getX() + xx;
                        this.jarZ = source.getZ() + zz;
                        dd = dd2;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** Original {@code func_75253_b} ({@code AILiquidGoto.java:63-65}). */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.golem.getNavigation().isDone();
    }

    /** Original {@code func_75251_c} ({@code AILiquidGoto.java:67-69}). */
    @Override
    public void stop() {
        this.count = 0;
    }

    /** Original {@code func_75249_e} ({@code AILiquidGoto.java:87-93}): walk to the fluid. */
    @Override
    public void start() {
        this.count = 200; // :88
        this.prevX = Mth.floor(this.golem.getX());
        this.prevY = Mth.floor(this.golem.getY());
        this.prevZ = Mth.floor(this.golem.getZ());
        // :92 — the walk goes to the fluid's own position, at the golem's speed.
        this.golem.getNavigation().moveTo(this.waterX, this.waterY, this.waterZ, this.golem.getSpeed());
    }

    /**
     * Original {@code func_75246_d} ({@code AILiquidGoto.java:72-85}): burn the
     * budget, and when it runs out with the golem in the block it started in,
     * pick a random land pos 2 blocks away (y-range 1) and walk there for 20
     * more ticks — the original's "unstick" step.
     */
    @Override
    public void tick() {
        this.count--;
        if (this.count == 0
                && this.prevX == Mth.floor(this.golem.getX())
                && this.prevY == Mth.floor(this.golem.getY())
                && this.prevZ == Mth.floor(this.golem.getZ())) {
            final Vec3 target = LandRandomPos.getPos(this.golem, 2, 1); // :78 — see the DEVIATION above
            if (target != null) {
                this.count = 20; // :80
                this.golem.getNavigation().moveTo(target.x, target.y, target.z, this.golem.getSpeed());
            }
        }
        super.tick(); // the original's super.updateTask() (:84)
    }
}
