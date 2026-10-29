package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOreItem;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registration holder (TC-M2-01 registration architecture).
 *
 * <p>Covers every kind=item and kind=spawner_egg row in
 * docs/content-mapping.csv; the {@code new_id} column is the id contract
 * (docs/architecture/registration.md). Block items are registered here too,
 * referencing {@link ModBlocks} holders — items register after blocks in
 * NeoForge's own registry-event order.</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);

    /** Block item for {@link ModBlocks#BLOCK_CUSTOM_ORE} (TC-M5-05); the
     *  original registered the block item under the block id, which 1.21.1
     *  does the same way. */
    public static final DeferredItem<BlockCustomOreItem> BLOCK_CUSTOM_ORE_ITEM =
            ITEMS.register("blockcustomore",
                    () -> new BlockCustomOreItem(ModBlocks.BLOCK_CUSTOM_ORE.get(), new Item.Properties()));

    /**
     * The Thaumonomicon (TC-M3-03-s): the item that opens the research
     * browser. Registered under the content-mapping.csv id contract
     * {@code thaumcraft:itemthaumonomicon}; the browser is reached through the
     * shared {@code research.book.ThaumonomiconHost} seam, whose only
     * implementation lives in the client package so a dedicated server never
     * loads a screen class.
     */
    public static final DeferredItem<ItemThaumonomicon> THAUMONOMICON =
            ITEMS.register("itemthaumonomicon", () -> new ItemThaumonomicon(new Item.Properties()));

    private ModItems() {
    }
}
