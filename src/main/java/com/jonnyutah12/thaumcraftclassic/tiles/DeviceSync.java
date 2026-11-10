package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared sync helper for the essentia devices (TC-M4-06). The original's
 * tiles call {@code worldObj.markBlockRangeForRenderUpdate(...)} plus
 * {@code markDirty()} after every state change so the client re-renders the
 * essentia fill; the modern equivalent is {@code sendBlockUpdated}, which
 * both re-syncs the block entity and triggers the client render update.
 */
final class DeviceSync {

    private DeviceSync() {
    }

    static void sync(final BlockEntity be) {
        be.setChanged();
        final var level = be.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }
}
