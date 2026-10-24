package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane growth lamp (content-mapping.csv {@code thaumcraft:tilearanelampgrowth},
 * blockMetalDevice variant 7).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneLampGrowth}. Every
 * 20 ticks it looks for a grass block or growing crop within 8 blocks and,
 * while one is in range, burns one tick of that block's essentia cost
 * (from the TC-M5-05 block essentia data) to grow it — the same growth step
 * bone meal performs, at the essentia price.</p>
 */
public class TileArcaneLampGrowth extends TileArcaneLamp {

    public TileArcaneLampGrowth(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ARCANE_LAMP_GROWTH.get(), pos, state);
    }

    @Override
    protected void updateUse() {
        if (use <= 0 && target() != null) {
            use = 1;
        }
    }

    @Override
    protected AspectList target() {
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    final BlockPos at = worldPosition.offset(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (state.getBlock() instanceof BonemealableBlock growable
                            && growable.isRandomlyTicking(state)) {
                        final AspectList tags = EssentiaTags.getObjectTags(
                                new net.minecraft.world.item.ItemStack(state.getBlock()));
                        if (tags.size() > 0) {
                            return tags;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void consumeEssentia(final AspectList list) {
        super.consumeEssentia(list);
        if (use == 0) {
            return;
        }
        // apply the growth step to the first bonemealable target in range
        for (int x = -8; x <= 8 && use > 0; x++) {
            for (int y = -8; y <= 8 && use > 0; y++) {
                for (int z = -8; z <= 8 && use > 0; z++) {
                    final BlockPos at = worldPosition.offset(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (state.getBlock() instanceof BonemealableBlock growable
                            && growable.isRandomlyTicking(state)) {
                        growable.randomTick(level, at, state, level.random);
                        use = 1; // keep the lamp armed while a target exists
                        return;
                    }
                }
            }
        }
    }
}
