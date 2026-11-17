package com.jonnyutah12.thaumcraftclassic.fluids;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Death fluid (content-mapping.csv {@code thaumcraft:fluiddeath}) — the
 * dissolving flow of the Outer Lands (taint's living counterpart).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockFluidDeath} on a
 * {@code BlockFluidFinite} with 4 quanta per block — a thick, short-ranged
 * flow, not water — and every entity that touches it takes 2 points of
 * {@code damage.thaumcraft.dissolve} (the original's
 * {@code DamageSourceThaumcraft.dissolve}, armour-bypassing magic damage).</p>
 *
 * <p>The modern shape is NeoForge's {@link SimpleFlowingFluid}; the dissolve
 * damage lives on the block (see {@link BlockFluidDeath}), where the original
 * applied it. Registered under the original's id.</p>
 */
public class FluidDeath extends BaseFlowingFluid {

    public FluidDeath(final BaseFlowingFluid.Properties properties) {
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

    /** The flowing half of the death fluid. */
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
