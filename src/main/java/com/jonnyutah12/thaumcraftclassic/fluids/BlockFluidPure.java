package com.jonnyutah12.thaumcraftclassic.fluids;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * The pure fluid's block (content-mapping.csv {@code thaumcraft:fluidpure}).
 *
 * <p>Original: the {@code BlockFluidClassic} behind {@code fluidpure} — clean,
 * freely flowing water with no damage; players swim in it and it extinguishes
 * fire (the fluid type carries both). The original's random tick (meta 15
 * spreads the fluid sideways) is the finite-fluid bookkeeping of the legacy
 * fluid model; the modern flowing fluid spreads on its own.</p>
 */
public class BlockFluidPure extends LiquidBlock {

    public BlockFluidPure(final net.minecraft.world.level.material.FlowingFluid fluid, final Properties properties) {
        super(fluid, properties);
    }
}
