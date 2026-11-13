package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item of the essentia tube block (content-mapping.csv
 * {@code item_class=BlockTubeItem}).
 *
 * <p>The original {@code BlockTubeItem} existed to name the eight metadata
 * values ({@code tile.blockTube.<meta>.name}); it subclasses the shared
 * {@link VariantBlockItem}, which is where the port keeps the meta-carrying
 * stack convention (the vanilla {@code block_state} component, see
 * {@link VariantStateItems}) and the per-variant label lookup through
 * {@link BlockVariantNames}.</p>
 *
 * <p>Block-tube's eight names are <em>not</em> declared in
 * {@link BlockVariantNames} yet: that table and its audits (the
 * {@code LocalizationGameTest} per-variant probe and the en_US wording parity
 * test) are the per-variant-label card's (TC-M7-01b), so a tube stack reads the
 * block's own label today — the original's metadata-0 name, "Essentia Tube" —
 * exactly as the void jar does until that card names its family.</p>
 */
public class BlockTubeItem extends VariantBlockItem {

    public BlockTubeItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
