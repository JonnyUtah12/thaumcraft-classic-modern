package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchBossRoomHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMazeHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Ancient Locking Mechanism (content-mapping.csv
 * {@code thaumcraft:tileeldritchlock}, hosted on blockEldritch variant 8).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchLock}. The lock
 * starts disarmed ({@code count == -1}); inserting a Runed Tablet (the
 * original's meta-8 activation, ported to the item-use seam) sets the fuse to
 * 0, consumes the tablet, marks the tile and plays the runic charge chime.
 * Every tick the lock counts up, plays the pump sound on every fifth tick, and
 * on {@code count > 100} runs its boss spawn: the ice chime, the per-level boss
 * counter, the {@code tc.boss.*} announcement, the room build and the sparkle
 * refresh of every aura node within ±2 cells. The lock's {@code facing} (the
 * side the tablet goes on) is saved and drives the original's face-specific
 * icon.</p>
 *
 * <p>The room build itself is handed to {@link EldritchBossRoomHost} — see the
 * provenance note there. The 3.25-cell render box, the client-side sparkles and
 * the per-face icon selection are TC-M7-01/02.</p>
 */
public class TileEldritchLock extends BlockEntity
        implements BlockDeviceBase.TickingDeviceTile, BlockDeviceBase.DeviceItemUseTile {

    /** The original's {@code count}: -1 disarmed, 0..100 the fuse, 101 the spawn. */
    public int count = -1;
    private byte facing = 0;

    public TileEldritchLock(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_LOCK.get(), pos, state);
    }

    public byte getFacing() {
        return facing;
    }

    public void setFacing(final byte face) {
        facing = face;
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        setChanged();
    }

    public boolean isArmed() {
        return count >= 0;
    }

    /** The original's meta-8 activation: insert the Runed Tablet while disarmed. */
    @Override
    public ItemInteractionResult useWithItem(final ServerLevel level, final BlockPos pos,
                                            final net.minecraft.server.level.ServerPlayer player,
                                            final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (count >= 0 || !ItemEldritchObject.isEldritchObject(stack)
                || ItemEldritchObject.storedMeta(stack) != EldritchRealmValues.OBJECT_TABLET) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        count = EldritchRealmValues.LOCK_ARMED_COUNT;
        stack.shrink(1);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                ModSounds.RUNIC_SHIELD_CHARGE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide || count < 0) {
            return;
        }
        count++;
        if (count % EldritchRealmValues.LOCK_SOUND_PERIOD_TICKS == 0) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    ModSounds.PUMP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (count > EldritchRealmValues.LOCK_BOSS_DELAY_TICKS) {
            doBossSpawn();
        }
    }

    /**
     * The original's {@code doBossSpawn()} minus the room build (see
     * {@link EldritchBossRoomHost}): chime, boss counter, announcement, room
     * hand-off, and the node sparkle refresh.
     */
    private void doBossSpawn() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                ModSounds.ICE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

        // The original picks the boss room's chunk and exit from the maze map
        // of the 5x5 chunk neighbourhood; TC-M6-04f-m answers "no maze" today,
        // which leaves the lock's own chunk and exit 0 — the same fallback the
        // original takes on a world with no maze.
        final int exitFeature = 0;
        final int bossCount = EldritchBossRoomHost.advanceBossCount(serverLevel);
        final String boss = EldritchBossRoomHost.bossOf(bossCount);
        EldritchBossRoomHost.announce(serverLevel, worldPosition, boss);
        EldritchBossRoomHost.buildRoom(serverLevel, worldPosition, exitFeature);

        refreshAiryNeighbours(serverLevel);
        setChanged();
    }

    /**
     * The original's sparkle pass: every {@code blockAiry} within ±2 cells gets
     * the sparkle packet and a block update (the aura nodes above the boss
     * room light up when the boss wakes).
     */
    private void refreshAiryNeighbours(final ServerLevel level) {
        final var airy = com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices.BLOCK_AIRY.get();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                for (int c = -2; c <= 2; c++) {
                    final BlockPos pos = worldPosition.offset(a, b, c);
                    if (level.getBlockState(pos).is(airy)) {
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", facing);
        tag.putShort("count", (short) count);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.getByte("facing");
        count = tag.getShort("count");
    }
}
