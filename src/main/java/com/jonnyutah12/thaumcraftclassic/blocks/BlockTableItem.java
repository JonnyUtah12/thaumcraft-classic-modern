package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockTable} (content-mapping.csv row
 * {@code thaumcraft:blockTable}, item_class=BlockTableItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockTable.0.name=Table}, {@code .14.name=Deconstruction Table}, {@code .15.name=Arcane Worktable}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>The original named metas 0, 1, 14 and 15 only (its {@code tile.blockTable.research.name} key has no metadata of its own), so the other twelve variants keep the base label.</p>
 */
public class BlockTableItem extends VariantBlockItem {

    public BlockTableItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
