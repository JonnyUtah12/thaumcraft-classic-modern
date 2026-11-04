package com.jonnyutah12.thaumcraftclassic.fluids;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Pure fluid (content-mapping.csv {@code thaumcraft:fluidpure}) — the clean
 * water of the Outer Lands, flowing from the eldritch structures.
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockFluidPure} on a
 * {@code BlockFluidClassic} (an infinite, freely flowing fluid) with no damage
 * of its own — it is the <em>clean</em> counterpart of death fluid, and the
 * place players bathe in the Outer Lands (the spa). The bubbles are TC-M7-02.</p>
 *
 * <p>The modern shape is NeoForge's {@link SimpleFlowingFluid} with a
 * water-like source, registered under the original's id; the fluid type
 * carries the original's water behaviour (players swim, it extinguishes fire).
 * The block and the bucket are the original's {@code fluidpure} block and
 * {@code ItemBucketPure}, registered in the catalog card.</p>
 */
public class FluidPure extends BaseFlowingFluid {

    public FluidPure(final BaseFlowingFluid.Properties properties) {
        super(properties);
    }

    @Override
    public int getAmount(final net.minecraft.world.level.material.FluidState state) {
        return 4;
    }

    @Override
    public boolean isSource(final net.minecraft.world.level.material.FluidState state) {
        return true;
    }

    /** The flowing half of the pure fluid (the original's {@code fluidpure} meta-1..7). */
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(final BaseFlowingFluid.Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(final net.minecraft.world.level.material.FluidState state) {
            return 2;
        }

        @Override
        public boolean isSource(final net.minecraft.world.level.material.FluidState state) {
            return false;
        }
    }
}
