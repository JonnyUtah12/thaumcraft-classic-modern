package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockCosmeticSolid} (content-mapping.csv row
 * {@code thaumcraft:blockCosmeticSolid}, item_class=BlockCosmeticSolidItem).
 *
 * <p>The original class existed to list the fourteen visible variants (metas 10 and 13 were deliberately hidden) in the creative tab;
 * variant-carrying stacks use the vanilla {@code block_state} component (see
 * {@link VariantStateItems}) and the creative listing is TC-M7-01 tab
 * territory. The per-variant display names — the original's
 * {@code tile.<id>.<meta>.name} strings — arrive with TC-M7-01b through
 * {@link VariantBlockItem}.</p>
 */
public class BlockCosmeticSolidItem extends VariantBlockItem {

    public BlockCosmeticSolidItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
