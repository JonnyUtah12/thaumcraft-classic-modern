package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockMirror} (content-mapping.csv row
 * {@code thaumcraft:blockMirror}, item_class=BlockMirrorItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockMirror.0.name=Magic Mirror}, {@code .6.name=Essentia Mirror}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>The original named metas 0 and 6 (its {@code tile.blockMirror.name} base key is what the plain item rendered), so the other ten variants keep the base label.</p>
 */
public class BlockMirrorItem extends VariantBlockItem {

    public BlockMirrorItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
