package com.jonnyutah12.thaumcraftclassic.fluids;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * The death fluid's block (content-mapping.csv {@code thaumcraft:fluiddeath}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockFluidDeath} — every entity
 * inside the fluid takes {@link EldritchRealmValues#DEATH_FLUID_DAMAGE} points
 * of the armour-bypassing magic {@code damage.thaumcraft.dissolve} damage type
 * (the original calls {@code DamageSourceThaumcraft.dissolve} from
 * {@code onEntityCollidedWithBlock} at BlockFluidDeath:24). Living and item
 * entities alike: the fluid dissolves what it touches, which is why the Outer
 * Lands' taint flows kill the things that wander into them.</p>
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
            entity.hurt(DamageSourcesThaumcraft.dissolve(serverLevel), EldritchRealmValues.DEATH_FLUID_DAMAGE);
        }
    }
}
