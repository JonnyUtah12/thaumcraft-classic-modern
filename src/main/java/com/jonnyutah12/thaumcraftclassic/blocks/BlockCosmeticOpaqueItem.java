package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for {@link BlockCosmeticOpaque} (content-mapping.csv row
 * {@code thaumcraft:blockCosmeticOpaque}, item_class=BlockCosmeticOpaqueItem).
 *
 * <p>The original class existed to list the three metadata variants in the
 * creative tab; variant-carrying stacks use the vanilla {@code block_state}
 * component (see {@link VariantStateItems}). The creative-tab listing itself
 * is TC-M6-04 tab territory.</p>
 */
public class BlockCosmeticOpaqueItem extends BlockItem {

    public BlockCosmeticOpaqueItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }
}