package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockCustomOre} (content-mapping.csv row
 * {@code thaumcraft:blockCustomOre}, item_class=BlockCustomOreItem).
 *
 * <p>The original class existed to list all eight metadata variants in the
 * creative tab (fillCreativeTab, metas 0-7). The creative tab itself is
 * TC-M6-04 territory; this subclass keeps the class contract from the
 * content mapping and is where that variant listing will land.</p>
 */
public class BlockCustomOreItem extends BlockItem {

    /**
     * Per-variant overlay tints, original {@code BlockCustomOreItem.colors}
     * verbatim (index = variant; index 0 unused/white). Applied to the
     * animated infused-ore overlay by {@link OreBlockColors}, matching the
     * original BlockCustomOreRenderer's per-metadata glColor3f.
     */
    public static final int[] COLORS = {16777215, 16777086, 16727041, 37119, 40960, 15650047, 5592439};

    public BlockCustomOreItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
