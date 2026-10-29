package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMazeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eldritch altar (content-mapping.csv {@code thaumcraft:tileeldritchaltar},
 * hosted on blockEldritch variant 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchAltar}. Inserting an
 * Eldritch Eye into the altar (the original's {@code BlockEldritch
 * .onBlockActivated} meta-0 branch, ported to the device block's item-use
 * seam) raises the eye count to a maximum of four; from the third eye the
 * altar arms its spawner with spawn type 1 (guardian) and asks the maze
 * generator for a dungeon near the altar. Every 40 ticks after the first 80 it
 * runs its wave: spawn type 0 summons four Crimson Clerics on the altar's
 * corners and, once three of them landed, switches to knight waves; spawn
 * type 1 summons the Eldritch Guardian. A knight wave with no cleric left
 * within the 24×16×24 box disarms the spawner.</p>
 *
 * <p>The summons themselves run through {@link EldritchMobHost}: the Crimson
 * Cult family is TC-M6-06 and an unregistered {@code EntityType} cannot be
 * spawned in 1.21.1, so until that card registers its spawners the counters,
 * flags, sounds, maze request and the {@code IEldritchMob} census behave
 * exactly as the original and the mob appears when the family lands. The
 * original's render bounding box (a 1-cell box) and the client-side sparkles
 * are TC-M7-02.</p>
 */
public class TileEldritchAltar extends BlockEntity
        implements BlockDeviceBase.TickingDeviceTile, BlockDeviceBase.DeviceItemUseTile {

    private boolean spawner = false;
    private boolean open = false;
    private boolean spawnedClerics = false;
    private byte spawnType = 0;
    private byte eyes = 0;
    private int counter = 0;

    public TileEldritchAltar(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_ALTAR.get(), pos, state);
    }

    public boolean isSpawner() {
        return spawner;
    }

    public void setSpawner(final boolean spawner) {
        this.spawner = spawner;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(final boolean open) {
        this.open = open;
    }

    public byte getSpawnType() {
        return spawnType;
    }

    public byte getEyes() {
        return eyes;
    }

    public int counter() {
        return counter;
    }

    /** The original's meta-0 activation: insert an Eldritch Eye. */
    @Override
    public ItemInteractionResult useWithItem(final ServerLevel level, final BlockPos pos,
                                            final ServerPlayer player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!ItemEldritchObject.isEldritchObject(stack) || ItemEldritchObject.storedMeta(stack) != EldritchRealmValues.OBJECT_EYE
                || eyes >= EldritchRealmValues.ALTAR_MAX_EYES) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isCreative()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (eyes >= EldritchRealmValues.ALTAR_SPAWNER_EYES) {
            spawner = true;
            spawnType = 1;
        }
        eyes++;
        checkForMaze();
        stack.shrink(1);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                ModSounds.CRYSTAL.value(), SoundSource.BLOCKS, 0.2F, 1.0F);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide || !spawner) {
            counter++;
            return;
        }
        counter++;
        if (counter < EldritchRealmValues.ALTAR_FIRST_WAVE_TICKS
                || counter % EldritchRealmValues.ALTAR_WAVE_PERIOD_TICKS != 0) {
            return;
        }
        if (spawnType == 0) {
            if (!spawnedClerics) {
                spawnClerics();
            } else {
                spawnGuards();
            }
        } else {
            spawnGuardian();
        }
    }

    /**
     * Original {@code checkForMaze()}: ask for a maze of 15 + 2·rand(8) cells
     * per side covering the altar's chunk. The maze generation request is
     * recorded on {@link EldritchMazeHost} (the maze generator shipped as a
     * pure class in f252c58; the per-world handler is TC-M6-04f-m), so the
     * answer is "no maze in range" until that handler lands.
     */
    public boolean checkForMaze() {
        final long seed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        final int w = EldritchMazeHost.requestSize(seed, worldPosition.getX());
        final int h = EldritchMazeHost.requestSize(seed, worldPosition.getZ());
        return EldritchMazeHost.request(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, w, h);
    }

    /** Original {@code spawnClerics()}: the four corners, ±2 cells. */
    private void spawnClerics() {
        int success = 0;
        for (int a = 0; a < 4; a++) {
            final int xx = a < 2 ? -EldritchRealmValues.ALTAR_CLERIC_OFFSET : EldritchRealmValues.ALTAR_CLERIC_OFFSET;
            final int zz = a % 2 == 0 ? -EldritchRealmValues.ALTAR_CLERIC_OFFSET : EldritchRealmValues.ALTAR_CLERIC_OFFSET;
            if (EldritchMobHost.spawn(EldritchMobHost.CLERIC, (ServerLevel) level, worldPosition.offset(xx, 0, zz))) {
                success++;
            }
        }
        if (success > EldritchRealmValues.ALTAR_CLERIC_SUCCESS - 1) {
            spawnedClerics = true;
            setChanged();
        }
    }

    /** Original {@code spawnGuards()}: knight waves, off when the clerics are gone. */
    private void spawnGuards() {
        if (EldritchMobHost.countFamily(EldritchMobHost.CLERIC, (ServerLevel) level, worldPosition,
                EldritchRealmValues.ALTAR_GUARD_BOX_XZ, EldritchRealmValues.ALTAR_GUARD_BOX_Y) < 1) {
            spawner = false;
            return;
        }
        if (EldritchMobHost.countFamily(EldritchMobHost.CULTIST, (ServerLevel) level, worldPosition,
                EldritchRealmValues.ALTAR_GUARD_BOX_XZ, EldritchRealmValues.ALTAR_GUARD_BOX_Y)
                >= EldritchRealmValues.ALTAR_GUARD_CAP) {
            return;
        }
        EldritchMobHost.spawn(EldritchMobHost.KNIGHT, (ServerLevel) level, worldPosition);
    }

    /** Original {@code spawnGuardian()}. */
    private void spawnGuardian() {
        EldritchMobHost.spawn(EldritchMobHost.GUARDIAN, (ServerLevel) level, worldPosition);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("eyes", eyes);
        tag.putBoolean("open", open);
        tag.putBoolean("spawnedClerics", spawnedClerics);
        tag.putBoolean("spawner", spawner);
        tag.putByte("spawntype", spawnType);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        eyes = tag.getByte("eyes");
        open = tag.getBoolean("open");
        spawnedClerics = tag.getBoolean("spawnedClerics");
        spawner = tag.getBoolean("spawner");
        spawnType = tag.getByte("spawntype");
    }
}
