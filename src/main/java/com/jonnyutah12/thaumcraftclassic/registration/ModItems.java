package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
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

    private ModItems() {
    }
}
