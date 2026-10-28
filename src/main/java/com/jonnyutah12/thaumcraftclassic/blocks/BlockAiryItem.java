package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockAiry} (content-mapping.csv row
 * {@code thaumcraft:blockAiry}, item_class=BlockAiryItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockAiry.0.name=Aura Node}, {@code .1.name=Nitor}, {@code .5.name=Energized Aura Node}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>Metas 6-9 carry no original name keys, so those variants keep the base label — the sparse set is declared in {@link BlockVariantNames}. {@code blockAiry} is the vis-node host block; TC-M5-02 nodes are record-driven per docs/architecture/node-behavior.md, so an {@code Aura Node} stack is reached through pick-block of a node and {@code /give}.</p>
 */
public class BlockAiryItem extends VariantBlockItem {

    public BlockAiryItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
