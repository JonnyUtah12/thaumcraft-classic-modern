package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOre;
import net.neoforged.neoforge.registries.DeferredBlock;
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

    /**
     * TC ore block (TC-M5-05 worldgen prerequisite): cinnabar / infused stone
     * variants 1-6 / amber via the {@code variant} property. See
     * {@link BlockCustomOre} for the metadata-to-property mapping and the
     * drop-table scope note.
     */
    public static final DeferredBlock<BlockCustomOre> BLOCK_CUSTOM_ORE =
            BLOCKS.register("blockcustomore", BlockCustomOre::new);

    private ModBlocks() {
    }
}
