package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for {@link BlockCosmeticWoodSlab} (content-mapping.csv row
 * {@code thaumcraft:blockCosmeticSlabWood}, item_class=BlockCosmeticWoodSlabItem).
 *
 * <p>The original class existed to list the two wood types in the creative
 * tab (the double-slab item was deliberately hidden); variant-carrying stacks
 * use the vanilla {@code block_state} component (see
 * {@link VariantStateItems}). The creative listing is TC-M6-04 tab territory.</p>
 */
public class BlockCosmeticWoodSlabItem extends BlockItem {

    public BlockCosmeticWoodSlabItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }
}