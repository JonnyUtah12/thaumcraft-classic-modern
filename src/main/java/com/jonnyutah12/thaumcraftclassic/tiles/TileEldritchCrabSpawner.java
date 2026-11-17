package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crusted Opening — the eldritch crab spawner (content-mapping.csv
 * {@code thaumcraft:tileeldritchcrabspawner}, hosted on blockEldritch variant 9).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchCrabSpawner}. The
 * spawner runs a countdown from 150 that refills to 50 + rand(50) when it
 * underflows. When the count hits exactly 15, and a player is within 16 cells
 * and the box does not already hold more than 5 crabs, it plays the fizz and
 * (on the client) vents. At 0 it refills to 150 + rand(100), fires the crab out
 * of its facing side with a little gore, and the count restarts. A redstone
 * pulse makes it vent for 20 ticks. The crab census is an
 * {@code IEldritchMob}-typed box query — a player's pet crab never blocks the
 * spawner.</p>
 *
 * <p>Deferred: the crab itself is TC-M6-06 and its summon runs through
 * {@link EldritchMobHost} (nothing is registered today, so the count, the
 * sounds, the vent bookkeeping and the census all run exactly as the original
 * and the crab appears when its card lands); the vent particles and the 2-cell
 * render box are TC-M7-02.</p>
 */
public class TileEldritchCrabSpawner extends BlockEntity
        implements BlockDeviceBase.TickingDeviceTile, BlockDeviceBase.DeviceNeighborTile {

    public int count = EldritchRealmValues.CRAB_START_COUNT;
    public int ticks = 0;
    public int venting = 0;
    private byte facing = 0;

    public TileEldritchCrabSpawner(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_CRAB_SPAWNER.get(), pos, state);
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

    /**
     * Original {@code isActivated()}: a player within 16 cells of the spawner.
     */
    public boolean isActivated() {
        return level != null && level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, EldritchRealmValues.CRAB_PLAYER_RANGE, null) != null;
    }

    /** Original {@code maxEntitiesReached()}: more than 5 crabs in the 32-box. */
    public boolean maxEntitiesReached() {
        return EldritchMobHost.countFamily(EldritchMobHost.CRAB, (ServerLevel) level, worldPosition,
                EldritchRealmValues.CRAB_BOX, EldritchRealmValues.CRAB_BOX) > EldritchRealmValues.CRAB_ENTITY_CAP;
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (ticks == 0) {
            ticks = level.getRandom().nextInt(500);
        }
        ticks++;
        count--;
        if (count < 0) {
            count = EldritchRealmValues.CRAB_REFILL_MIN + level.getRandom().nextInt(EldritchRealmValues.CRAB_REFILL_SPAN);
            return;
        }
        final boolean ready = isActivated() && !maxEntitiesReached();
        if (count == EldritchRealmValues.CRAB_FIZZ_COUNT && ready) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            venting = 20;
        }
        if (count <= 0 && ready) {
            count = EldritchRealmValues.CRAB_RESET_MIN + level.getRandom().nextInt(EldritchRealmValues.CRAB_RESET_SPAN);
            spawnCrab();
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    ModSounds.GORE.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        if (venting > 0) {
            venting--;
        }
    }

    /** Original {@code onNeighborBlockChange}: a redstone pulse makes it vent. */
    @Override
    public void deviceNeighborChanged() {
        if (level != null && !level.isClientSide && level.hasNeighborSignal(worldPosition)) {
            venting = 20;
        }
    }

    /** Original {@code spawnCrab()}: fired along the facing side with velocity. */
    private void spawnCrab() {
        final Direction dir = Direction.from3DDataValue(facing);
        EldritchMobHost.spawn(EldritchMobHost.CRAB, (ServerLevel) level, worldPosition.relative(dir));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", facing);
        tag.putInt("count", count);
        tag.putInt("ticks", ticks);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.getByte("facing");
        count = tag.contains("count") ? tag.getInt("count") : EldritchRealmValues.CRAB_START_COUNT;
        ticks = tag.getInt("ticks");
    }
}
