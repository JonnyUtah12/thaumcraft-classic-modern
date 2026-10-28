package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Warding stone fence post (content-mapping.csv {@code thaumcraft:tilewardingstonefence},
 * hosted on the fence block under blockCosmeticSolid variant 4 — TC-M6-04b).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileWardingStoneFence}. Self-
 * removes when no warding stone (blockCosmeticSolid variant 3) supports it
 * within two blocks below.</p>
 */
public class TileWardingStoneFence extends BlockEntity implements com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    int count = 0;

    public TileWardingStoneFence(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_WARDING_STONE_FENCE.get(), pos, state);
    }

    public void serverTick() {
        if (this.count == 0) {
            this.count = this.level.random.nextInt(100);
        }
        if (++this.count % 100 == 0
                && !isWardingStone(this.worldPosition.below())
                && !isWardingStone(this.worldPosition.below(2))) {
            this.level.removeBlock(this.worldPosition, false);
        }
    }

    private boolean isWardingStone(final BlockPos pos) {
        final BlockState state = this.level.getBlockState(pos);
        final net.minecraft.world.level.block.Block cosmetic =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        ResourceLocation.fromNamespaceAndPath(
                                com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockcosmeticsolid"));
        return state.getBlock() == cosmetic
                && cosmetic instanceof VariantDevice device && device.variant(state) == 3;
    }
}
