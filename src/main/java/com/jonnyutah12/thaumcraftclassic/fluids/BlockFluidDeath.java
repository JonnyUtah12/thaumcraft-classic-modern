package com.jonnyutah12.thaumcraftclassic.fluids;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The death fluid's block (content-mapping.csv {@code thaumcraft:fluiddeath}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockFluidDeath} — every living
 * entity inside the fluid takes {@code world.getBlockMetadata(x, y, z) + 1}
 * points of {@code damage.thaumcraft.dissolve} (the original calls
 * {@code DamageSourceThaumcraft.dissolve} from {@code func_149670_a},
 * BlockFluidDeath.java:42 — 1 at the source block, up to 8 at the shallowest
 * flowing step). Living and item entities alike: the fluid dissolves what it
 * touches, which is why the Outer Lands' taint flows kill the things that
 * wander into them.</p>
 *
 * <p>The dissolve type's armour-ignoring bypass is not applied here. It is the
 * {@code minecraft:bypasses_armor} damage-type tag that carries it (the
 * 1.21.1 replacement for the original's {@code setDamageBypassesArmor()}), and
 * this emitter only supplies the amount (BD-20).</p>
 *
 * <p>The original's {@code PlayerSuffocateEvent} hook (players do not drown in
 * the fluid) is a legacy event with no 1.21.1 equivalent; in modern
 * behaviour, suffocation only applies to water, so the fluid never drowns a
 * player by construction. The slimy bubbles are TC-M7-02.</p>
 */
public class BlockFluidDeath extends LiquidBlock {

    public BlockFluidDeath(final net.minecraft.world.level.material.FlowingFluid fluid, final Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.hurt(DamageSourcesThaumcraft.dissolve(serverLevel),
                    EldritchRealmValues.dissolveDamage(fluidDepth(state)));
        }
    }

    /**
     * The depth of the fluid column of one cell — the 1.21.1 reading of the
     * 1.7.10 {@code world.getBlockMetadata(x, y, z)} the original added 1 to
     * (BD-20): 0 at the source block, then 1..7 down the flowing steps.
     *
     * <p>It is read from the cell's {@link FluidState}. The fluid's own
     * {@code getAmount} is not the level here: this port's
     * {@link com.jonnyutah12.thaumcraftclassic.fluids.FluidDeath} answers it
     * with the original's quanta (4 source / 2 flowing) rather than the
     * vanilla {@code LEVEL}, so the flowing half is read from the fluid
     * state's own {@link FlowingFluid#LEVEL} property. {@code LiquidBlock}
     * builds that as {@code getFlowing(8 - blockLevel)} (LiquidBlock:64-71),
     * i.e. the inverse of the block's {@code level} — hence {@code 8 - level}
     * below. The source's fluid state carries no {@code LEVEL} (the source
     * type defines only {@code FALLING}), hence the
     * {@link FluidState#isSource()} guard. Falling cells answer 0, exactly as
     * the original's full column (metadata 0) did.</p>
     */
    private static int fluidDepth(final BlockState state) {
        final FluidState fluid = state.getFluidState();
        return fluid.isSource() ? 0 : 8 - Math.min(fluid.getValue(FlowingFluid.LEVEL), 8);
    }
}
