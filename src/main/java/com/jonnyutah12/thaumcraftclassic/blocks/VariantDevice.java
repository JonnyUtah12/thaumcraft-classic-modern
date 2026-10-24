package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Implemented by the shared multi-variant device blocks (TC-M6-04c).
 *
 * <p>The original 1.7.10 blocks stored the sub-kind in block metadata; 1.21.1
 * has no block metadata, so each device block carries the original meta value
 * verbatim in an {@code IntegerProperty} named {@code variant} (same
 * convention as {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOre},
 * D-001: ids and meta values unchanged, only the storage mechanism). This
 * accessor lets shared code (the tile factory, the variant block item) read
 * the original-meta equivalent without knowing each block's property range.</p>
 */
public interface VariantDevice {

    /** The original metadata value carried by this state (0-26 range). */
    int variant(BlockState state);
}
