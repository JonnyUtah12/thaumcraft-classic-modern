package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockWoodenDevice} (content-mapping.csv row
 * {@code thaumcraft:blockWoodenDevice}, item_class=BlockWoodenDeviceItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockWoodenDevice.0.name=Arcane Bellows}, {@code .4.name=Arcane Bore Base}, {@code .5.name=Arcane Bore}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>Meta 3 carries no original name key, so that variant keeps the base label — the sparse set is declared in {@link BlockVariantNames}.</p>
 */
public class BlockWoodenDeviceItem extends VariantBlockItem {

    public BlockWoodenDeviceItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
