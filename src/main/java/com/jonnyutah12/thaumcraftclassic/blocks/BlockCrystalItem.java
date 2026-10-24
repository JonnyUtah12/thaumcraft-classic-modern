package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for {@link BlockCrystal} (content-mapping.csv row
 * {@code thaumcraft:blockCrystal}, item_class=BlockCrystalItem).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCrystalItem} — a plain
 * meta-carrying ItemBlock whose only logic was {@code placeBlockAt}: after a
 * successful placement it stored the clicked side into
 * {@code TileCrystal.orientation} (wrapped in a try/catch because the tile
 * may not exist yet). Ported the same way on top of {@code place}: the
 * clicked face is the support face, and {@link Direction} ordinals match the
 * original ForgeDirection ordinals (see {@link TileCrystal}).</p>
 */
public class BlockCrystalItem extends BlockItem {

    public BlockCrystalItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        final InteractionResult result = super.place(context);
        if (result == InteractionResult.SUCCESS
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileCrystal tile) {
            tile.setOrientation(context.getClickedFace());
        }
        return result;
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }
}