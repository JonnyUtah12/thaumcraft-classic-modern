package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockCosmeticStoneSlab} (content-mapping.csv row
 * {@code thaumcraft:blockCosmeticSlabStone}, item_class=BlockCosmeticStoneSlabItem).
 *
 * <p>The original class existed to list the two stone types (the double-slab item was deliberately hidden) in the creative tab;
 * variant-carrying stacks use the vanilla {@code block_state} component (see
 * {@link VariantStateItems}) and the creative listing is TC-M7-01 tab
 * territory. The per-variant display names — the original's
 * {@code tile.<id>.<meta>.name} strings — arrive with TC-M7-01b through
 * {@link VariantBlockItem}.</p>
 */
public class BlockCosmeticStoneSlabItem extends VariantBlockItem {

    public BlockCosmeticStoneSlabItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
