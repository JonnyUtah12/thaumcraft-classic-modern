package com.jonnyutah12.thaumcraftclassic.entities;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.entities.monster.FallingTaintAiLogic;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Port of {@code thaumcraft.common.entities.EntityFallingTaint}
 * (TC-M6-06-s-b) — the entity TC-M5-04's {@code TaintManager.tryToFall} seam
 * spawns by id ({@code thaumcraft:fallingtaint}).
 *
 * <p>Faithful gravity body, pinned line by line in
 * {@link FallingTaintAiLogic}: gravity {@code -0.04} (line 81) with the
 * {@code 0.98} drag on ALL THREE motion axes (lines 83-85), the first-tick
 * source-cell check and clear (lines 91-97, {@code fallTime == 1}, cleared
 * at flags 3 via {@code func_147468_f}), the flux-goo float rule (a goo
 * support of quanta >= 4 holds the block up, lines 100-101), the discard
 * rules (100 ticks outside {@code [1, 256]} — the original's hardcoded 256
 * — and 600 ticks total, lines 102-104), the landing conversion (motion x/z
 * *0.7, y *-0.5, lines 106-108; the piston-family refusal, lines 110-112;
 * the gore sound at volume 0.5 with the randomized pitch, lines 113-114;
 * the block placed at the feet at flags 3, line 117; and the
 * {@code BlockTaint} fall hook, lines 118-119).</p>
 *
 * <p>Deviations, documented: 1.7.10 stored the block as a numeric
 * {@code TileID} + {@code Data} pair ({@code writeSpawnData} ships blockId
 * int + metadata byte, lines 58-66) — 1.21.1 has no numeric block ids, so
 * the state round-trips under {@code "BlockState"} via
 * {@code NbtUtils.writeBlockState}, and {@code "Time"} keeps the
 * original's short width (lines 67/73, {@code func_74777_a}/{@code
 * func_74765_d}). The client-side {@code taintLandFX} burst (10 draws on
 * landing or the first tick, lines 121-124) ships with TC-M7-03-s
 * renderers. The spawn-guard path (the taint manager
 * spawns at the target cell through {@code TaintManager.spawnGuarded}, which
 * has no origin cell to hand over) keeps a sentinel origin: the first-tick
 * source check and the source-cell clear are skipped there, and the crust
 * stays in place until the entity lands — the shape the taint card's
 * {@code TaintGameTest} pins. 1.7.10's {@code preventEntitySpawning = true}
 * ({@code field_70156_m}) and its {@code yOffset = height / 2}
 * ({@code field_70129_M}) have no 1.21.1 analogue (spawn prevention is a
 * level query, the eye offset lives on the dimensions) and are dropped.</p>
 */
public class EntityFallingTaint extends Entity {

    private BlockState fallingBlockState = Blocks.AIR.defaultBlockState();
    private int fallTime;
    private int oldX = SENTINEL;
    private int oldY = SENTINEL;
    private int oldZ = SENTINEL;

    private static final int SENTINEL = Integer.MIN_VALUE;

    public EntityFallingTaint(final EntityType<? extends EntityFallingTaint> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(final net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // the original's entity has no synced fields beyond vanilla
    }

    public BlockState getFallingBlockState() {
        return this.fallingBlockState;
    }

    public void setFallingBlockState(final BlockState state) {
        this.fallingBlockState = state;
    }

    /** The original's constructor bookkeeping: remember the source cell. */
    public void setOriginCell(final int x, final int y, final int z) {
        this.oldX = x;
        this.oldY = y;
        this.oldZ = z;
    }

    public boolean hasOriginCell() {
        return this.oldX != SENTINEL;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.fallingBlockState.isAir()) {
            this.discard(); // line 79: a null/air block has nothing to fall
            return;
        }

        this.fallTime++;
        // lines 84-87: gravity, the move, then the drag on all three axes
        final Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, motion.y + FallingTaintAiLogic.GRAVITY, motion.z);
        this.move(MoverType.SELF, this.getDeltaMovement());
        final Vec3 moved = this.getDeltaMovement();
        this.setDeltaMovement(moved.x * FallingTaintAiLogic.DRAG,
                moved.y * FallingTaintAiLogic.DRAG,
                moved.z * FallingTaintAiLogic.DRAG);

        final BlockPos feet = BlockPos.containing(this.getX(), this.getBoundingBox().minY, this.getZ());

        // lines 91-98: the FIRST tick verifies the source cell, then clears it
        if (this.fallTime == FallingTaintAiLogic.FIRST_TICK && this.hasOriginCell()) {
            final BlockPos origin = new BlockPos(this.oldX, this.oldY, this.oldZ);
            if (!this.level().getBlockState(origin).is(this.fallingBlockState.getBlock())) {
                this.discard();
                return;
            }
            // func_147468_f = setBlockToAir (line 97; the 1.7.10 helper
            // applies flags 3)
            this.level().setBlock(origin, Blocks.AIR.defaultBlockState(),
                    FallingTaintAiLogic.PLACE_BLOCK_FLAGS);
            this.oldX = SENTINEL;
            this.oldY = SENTINEL;
            this.oldZ = SENTINEL;
        }

        final BlockPos below = feet.below();
        final boolean held = this.onGround() || FallingTaintAiLogic.heldByFluxGoo(
                this.level().getBlockState(below), TaintRules.FALL_BLOCKING_GOO_QUANTA);
        if (!held) {
            // lines 100-102: the falling branch, and its two discard rules
            if (FallingTaintAiLogic.discardByHeight(this.fallTime, feet.getY())
                    || this.fallTime > FallingTaintAiLogic.MAX_FALL_TICKS) {
                this.discard();
            }
            return;
        }

        // lines 106-108: landing motion
        final Vec3 landing = this.getDeltaMovement();
        this.setDeltaMovement(landing.x * FallingTaintAiLogic.LAND_XZ,
                landing.y * FallingTaintAiLogic.LAND_Y,
                landing.z * FallingTaintAiLogic.LAND_XZ);

        final BlockState target = this.level().getBlockState(feet);
        if (FallingTaintAiLogic.isPistonFamily(target)) {
            return; // line 109-111: pistons refuse the landing
        }
        this.playSound(ModSounds.GORE.get(), FallingTaintAiLogic.LAND_SOUND_VOLUME,
                FallingTaintAiLogic.landSoundPitch(this.getRandom().nextFloat(),
                        this.getRandom().nextFloat()));
        this.discard();
        // lines 113-119: canPlace + BlockTaint.canFallBelow + the block set,
        // then the placed block's fall hook
        if (!TaintManager.canFallBelow(this.level(), below)) {
            return;
        }
        if (!FallingTaintAiLogic.canPlaceInto(target,
                target.getBlock() instanceof BlockTaintFibres)) {
            return;
        }
        if (this.level().setBlock(feet, this.fallingBlockState,
                FallingTaintAiLogic.PLACE_BLOCK_FLAGS)
                && this.fallingBlockState.getBlock() instanceof BlockTaint) {
            // the original's onFinishFalling hook (BlockTaint.java:303) is
            // EMPTY in the reference tree, so nothing to call here
            com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.LOGGER.debug(
                    "falling taint placed at {}", feet);
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        // "Time" is a SHORT (line 67, func_74777_a); TileID/Data pair
        // documented in the class doc
        tag.putShort("Time", (short) this.fallTime);
        tag.put("BlockState", NbtUtils.writeBlockState(this.fallingBlockState));
        tag.putInt("OldX", this.oldX);
        tag.putInt("OldY", this.oldY);
        tag.putInt("OldZ", this.oldZ);
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        this.fallTime = tag.getShort("Time");
        this.fallingBlockState = NbtUtils.readBlockState(
                this.level().holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                tag.getCompound("BlockState"));
        this.oldX = tag.getInt("OldX");
        this.oldY = tag.getInt("OldY");
        this.oldZ = tag.getInt("OldZ");
    }

    public int fallTime() {
        return this.fallTime;
    }

    public void setFallTime(final int time) {
        this.fallTime = time;
    }
}
