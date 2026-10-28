package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;

/**
 * Block item for {@link BlockStoneDevice} (content-mapping.csv row
 * {@code thaumcraft:blockStoneDevice}, item_class=BlockStoneDeviceItem).
 *
 * <p>The original registered an {@code ItemBlock} per device block whose only
 * logic was the creative-tab meta listing; the sub-kind names came from the
 * {@code tile.<id>.<meta>.name} keys (e.g. {@code tile.blockStoneDevice.0.name=Alchemical Furnace}, {@code .1.name=Arcane Pedestal}, {@code .12.name=Arcane Spa}). TC-M7-01b restores
 * those names through {@link VariantBlockItem}, which reads the stack's
 * {@code variant} value; the creative-tab listing stays on TC-M7-01.</p>
 *
 * <p>The original's metas 3/4 (infusion pillars) drop {@code blockCosmeticSolid} — {@link BlockStoneDevice#getDrops} keeps that, so a pillar stack names the cosmetic block, as in the original.</p>
 */
public class BlockStoneDeviceItem extends VariantBlockItem {

    public BlockStoneDeviceItem(final Block block, final Properties properties) {
        super(block, properties);
    }
}
