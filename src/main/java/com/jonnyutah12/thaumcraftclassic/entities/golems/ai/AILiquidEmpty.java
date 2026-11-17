package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.EnumSet;
import java.util.List;

/**
 * The Decanting golem's "pour my belly into the home container" goal
 * (TC-M6-08-s-b2).
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AILiquidEmpty.java}
 * (74 lines). Installed by {@code setupGolem()} for <b>core 5 (Decanting) at
 * priority 1</b> ({@code .../common/entities/golems/EntityGolemBase.java:257}:
 * {@code field_75776_a(1, new AILiquidEmpty(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AILiquidEmpty.java:25}) → {@code MOVE|JUMP};</li>
 *   <li>{@code canUse} requires an idle navigation, a carried fluid with a
 *       non-zero amount, and the golem within the <b>squared {@code 5.0}</b> of
 *       its <em>home</em> centre ({@code :30-33}) — the 2.2-block radius that
 *       {@code AILiquidGather}/{@code AILiquidGoto} then widen to via the
 *       {@code (byte) -1} marker queries; the comparison is squared, kept as is;</li>
 *   <li>the goal fires only for a fluid the home container is <em>missing</em>
 *       ({@code :35-43}) — {@code fluid.isFluidEqual(liquidCarried)};</li>
 *   <li>{@code canContinueToUse} is constant false: the act is the whole goal
 *       ({@code :51-53});</li>
 *   <li>the sound plays only above {@code 200} mB, at volume
 *       {@code Math.min(0.2F, 0.2F * amount / getFluidCarryLimit())}
 *       ({@code :70-78}).</li>
 * </ul>
 *
 * <h2>Port shape</h2>
 * 1.7.10's {@code TileEntity → IFluidHandler} cast is NeoForge's block
 * capability on the container face, the same mapping {@link GolemHelper}
 * already uses for {@link GolemHelper#homeContainerPos(EntityGolemBase)} —
 * {@code DEVIATION} at the site. The golem's belly is a
 * {@link FluidStack} ({@link EntityGolemBase#getFluidCarried()}); the original
 * mutated {@code liquidCarried.amount} the same way and nulled the field at
 * zero, which the port reproduces exactly.
 */
public class AILiquidEmpty extends GolemGoal {

    public AILiquidEmpty(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AILiquidEmpty.java:25) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /** Original {@code func_75250_a} ({@code AILiquidEmpty.java:28-49}). */
    @Override
    public boolean canUse() {
        // Installed only for core 5 (setupGolem's switch) — reproduce that gate.
        if (!coreIs(5)) {
            return false;
        }
        final FluidStack carried = this.golem.getFluidCarried();
        // :30 — navigation idle, belly non-empty.
        if (!(this.golem.getNavigation().isDone() && carried != null && carried.getAmount() != 0)) {
            return false;
        }
        // :32-33 — squared distance to the home centre, gate 5.0. The home is the
        // golem's restrict centre (the original's func_110172_bL()).
        final BlockPos home = this.golem.getRestrictCenter();
        if (this.golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 5.0) {
            return false;
        }
        // :35-45 — the home container must be missing exactly the fluid carried.
        final List<FluidStack> fluids = GolemHelper.getMissingLiquids(this.golem);
        if (fluids == null) {
            return false;
        }
        for (final FluidStack fluid : fluids) {
            if (fluid.isFluidEqual(carried)) {
                return true;
            }
        }
        return false;
    }

    /** Original {@code func_75253_b} → constant false ({@code AILiquidEmpty.java:51-53}). */
    @Override
    public boolean canContinueToUse() {
        return false;
    }

    /** Original {@code func_75249_e} ({@code AILiquidEmpty.java:55-73}): startExecuting. */
    @Override
    public void start() {
        final Direction facing = GolemHelper.directionOf(this.golem.getHomeFacing());
        final BlockPos container = GolemHelper.homeContainerPos(this.golem);
        // DEVIATION: the original's `world.getTileEntity(pos) instanceof IFluidHandler`
        // (:58-59). 1.21.1 tiles reach their tank through the NeoForge block
        // capability, which is how GolemHelper resolves the same handler
        // (GolemHelper.java:1243-1246).
        // SEAM: this tree registers no block fluid-handler capability, so a *mod*
        // tile's tank (the crucible's 1000 mB water tank, for one) is invisible
        // here; vanilla cauldrons — NeoForge's own registration — do resolve, and
        // that is the handler the probe fills.
        final IFluidHandler handler = this.level().getCapability(Capabilities.FluidHandler.BLOCK, container,
                facing);
        final FluidStack carried = this.golem.getFluidCarried();
        if (handler == null || carried == null) {
            return;
        }
        final int amt = handler.fill(carried, IFluidHandler.FluidAction.EXECUTE); // :64
        carried.setAmount(carried.getAmount() - amt); // the original's `liquidCarried.amount -= amt` (:65)
        if (carried.getAmount() <= 0) {
            this.golem.setFluidCarried(null); // :66-69
        }
        if (amt > 200) {
            // :70-78 — volume min(0.2, 0.2 * amount / carryLimit), pitch jitter.
            final float volume = Math.min(0.2F, 0.2F * amt / this.golem.getFluidCarryLimit());
            final float pitch = 1.0F + (this.level().getRandom().nextFloat()
                    - this.level().getRandom().nextFloat()) * 0.3F;
            this.level().playSound(null, this.golem, SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL, volume, pitch);
        }
        // DEVIATION: updateCarried() (:79) publishes the carry payload to the
        // golem's display stack — not shipped yet (TC-M6-08-s-c).
        this.golem.setFluidCarried(carried.getAmount() <= 0 ? null : carried);
        // The original's itemWatched = null (:81) — see AILiquidGather's watch store.
        AILiquidGather.watchFluid(this.golem, null);
        // The original's tile.markDirtyForTheClient() (:82-83): the capability
        // handler has no single tile, so the block entity behind the position is
        // marked when there is one.
        final BlockEntity tile = this.level().getBlockEntity(container);
        if (tile != null) {
            tile.setChanged();
        }
    }
}
