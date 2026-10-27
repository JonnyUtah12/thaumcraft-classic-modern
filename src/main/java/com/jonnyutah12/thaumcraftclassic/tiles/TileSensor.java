package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Arcane sensor / "arcane ear" (content-mapping.csv {@code thaumcraft:tilesensor},
 * hosted on blockWoodenDevice variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileSensor}. Right-clicking
 * cycles the note pitch and sounds it; the sensor then emits a 10-tick
 * redstone pulse (read by the block's signal hooks). The tone is derived
 * from the material under it (stone/grass/wood/... → instrument).</p>
 *
 * <p>Deferred: the original also listened for note-block events broadcast
 * through a static per-world event list (fed by TC's note-block hook); the
 * 1.21.1 note block has no such hook, so passive listening arrives with the
 * sound/event integration (TC-M7-05). The pulse + pitch structure is here.</p>
 */
public class TileSensor extends BlockEntity implements BlockDeviceBase.TickingDeviceTile {

    public int note = 0;
    public int tone = 0;
    public int redstoneSignal = 0;

    public TileSensor(final BlockPos pos, final BlockState state) {
        this(CatalogDevices.TILE_SENSOR.get(), pos, state);
    }

    public TileSensor(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        if (redstoneSignal > 0) {
            redstoneSignal--;
            if (redstoneSignal == 0) {
                level.neighborChanged(worldPosition,
                        level.getBlockState(worldPosition).getBlock(), worldPosition);
                level.neighborChanged(worldPosition.below(),
                        level.getBlockState(worldPosition.below()).getBlock(), worldPosition);
                level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition),
                        level.getBlockState(worldPosition), 3);
            }
        }
    }

    /** Original {@code updateTone()}: instrument from the block below. */
    public void updateTone() {
        final MapColor below = level.getBlockState(worldPosition.below()).getMapColor(level, worldPosition.below());
        tone = toneForMapColor(below);
    }

    private static int toneForMapColor(final MapColor color) {
        if (color == MapColor.STONE || color == MapColor.METAL) {
            return 1;
        }
        if (color == MapColor.PLANT || color == MapColor.GRASS) {
            return 2;
        }
        if (color == MapColor.WOOD) {
            return 4;
        }
        return 0;
    }

    /** Original {@code changePitch()}. */
    public void changePitch() {
        note = (note + 1) % 25;
        setChanged();
    }

    /**
     * Original {@code triggerNote}: play the instrument sound for the current
     * pitch (the original routed this through the block's playNote hook; the
     * wooden device has no such hook in 1.21.1, so the sound plays here).
     */
    public void triggerNote(final boolean sound) {
        if (!level.getBlockState(worldPosition.above()).isAir()) {
            return;
        }
        if (sound) {
            updateTone();
            final float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            final var event = switch (tone) {
                case 1 -> SoundEvents.NOTE_BLOCK_BASEDRUM.value();
                case 2 -> SoundEvents.NOTE_BLOCK_HAT.value();
                case 3 -> SoundEvents.NOTE_BLOCK_SNARE.value();
                case 4 -> SoundEvents.NOTE_BLOCK_BASS.value();
                default -> SoundEvents.NOTE_BLOCK_HARP.value();
            };
            level.playSound(null, worldPosition, event, SoundSource.RECORDS, 3.0F, pitch);
        }
    }

    /** Start the 10-tick pulse (original set redstoneSignal = 10 on trigger). */
    public void pulse() {
        redstoneSignal = 10;
        level.neighborChanged(worldPosition,
                level.getBlockState(worldPosition).getBlock(), worldPosition);
        level.neighborChanged(worldPosition.below(),
                level.getBlockState(worldPosition.below()).getBlock(), worldPosition);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("note", (byte) note);
        tag.putByte("tone", (byte) tone);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        note = Math.max(0, Math.min(24, tag.getByte("note")));
        tone = tag.getByte("tone");
    }
}
