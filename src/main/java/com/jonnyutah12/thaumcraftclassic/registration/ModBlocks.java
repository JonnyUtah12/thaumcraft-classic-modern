package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registration holder (TC-M2-01 registration architecture).
 *
 * <p>Every block from docs/content-mapping.csv (kind=block) is registered here
 * with its canonical id — the {@code new_id} column is the id contract
 * (docs/architecture/registration.md, D-001 addendum). Content milestones
 * (M4-M6) add the entries; ids are frozen once shipped.</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);

    private ModBlocks() {
    }
}
