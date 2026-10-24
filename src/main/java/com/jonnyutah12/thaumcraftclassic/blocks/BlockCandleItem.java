package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for {@link BlockCandle} (content-mapping.csv row
 * {@code thaumcraft:blockCandle}, item_class=BlockCandleItem).
 *
 * <p>The original class existed to list all sixteen dye-colour metadata
 * variants in the creative tab; variant-carrying stacks use the vanilla
 * {@code block_state} component (see {@link VariantStateItems}). The
 * creative listing is TC-M6-04 tab territory.</p>
 */
public class BlockCandleItem extends BlockItem {

    public BlockCandleItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }
}