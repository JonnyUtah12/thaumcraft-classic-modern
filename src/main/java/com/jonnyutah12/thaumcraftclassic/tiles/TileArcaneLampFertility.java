package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Arcane fertility lamp (content-mapping.csv {@code thaumcraft:tilearanelampfertility},
 * blockMetalDevice variant 8).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneLampFertility}. Keeps
 * farmland within 6 blocks fully moist, burning one unit of each primal
 * aspect per tick (the original's fixed five-aspect cost — not data-driven).</p>
 */
public class TileArcaneLampFertility extends TileArcaneLamp {

    public TileArcaneLampFertility(final BlockPos pos, final BlockState state) {
        super(CatalogDevices.TILE_ARCANE_LAMP_FERTILITY.get(), pos, state);
    }

    @Override
    protected void updateUse() {
        if (use <= 0 && findFarmland() != null) {
            use = 1;
        }
    }

    @Override
    protected AspectList target() {
        final AspectList cost = new AspectList();
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            cost.add(primal, 1);
        }
        return findFarmland() == null ? null : cost;
    }

    @Override
    protected void consumeEssentia(final AspectList list) {
        super.consumeEssentia(list);
        if (use == 0) {
            return;
        }
        final BlockPos farmland = findFarmland();
        if (farmland != null) {
            final BlockState state = level.getBlockState(farmland);
            if (state.hasProperty(BlockStateProperties.MOISTURE)
                    && state.getValue(BlockStateProperties.MOISTURE) < 7) {
                level.setBlock(farmland, state.setValue(BlockStateProperties.MOISTURE, 7), 2);
            }
            use = 1; // keep the lamp armed while farmland is in range
        }
    }

    private BlockPos findFarmland() {
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    final BlockPos at = worldPosition.offset(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (state.hasProperty(BlockStateProperties.MOISTURE)
                            && state.getValue(BlockStateProperties.MOISTURE) < 7) {
                        return at;
                    }
                }
            }
        }
        return null;
    }
}
