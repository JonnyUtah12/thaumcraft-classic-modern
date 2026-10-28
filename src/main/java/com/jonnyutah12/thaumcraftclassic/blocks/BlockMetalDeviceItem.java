package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockMetalDevice} (content-mapping.csv row
 * {@code thaumcraft:blockMetalDevice}, item_class=BlockMetalDeviceItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockMetalDevice.0.name=Crucible}, {@code .7.name=Arcane Lamp}, {@code .10.name=Thaumatorium}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>Meta 4 carries no original name key, so that variant keeps the base label — the sparse set is declared in {@link BlockVariantNames}.</p>
 */
public class BlockMetalDeviceItem extends VariantBlockItem {

    public BlockMetalDeviceItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
