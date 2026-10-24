package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for {@link BlockCosmeticStoneSlab} (content-mapping.csv row
 * {@code thaumcraft:blockCosmeticSlabStone}, item_class=BlockCosmeticStoneSlabItem).
 *
 * <p>The original class existed to list the two stone types in the creative
 * tab (the double-slab item was deliberately hidden); variant-carrying stacks
 * use the vanilla {@code block_state} component (see
 * {@link VariantStateItems}). The creative listing is TC-M6-04 tab territory.</p>
 */
public class BlockCosmeticStoneSlabItem extends BlockItem {

    public BlockCosmeticStoneSlabItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }
}