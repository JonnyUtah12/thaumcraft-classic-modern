package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The golem's door goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIOpenDoor.java},
 * the only goal installed from Thaumcraft's door pair.
 *
 * <p>Installed at melee priority {@code 5} in the common tail that
 * {@code setupGolem()} runs for every core ({@code EntityGolemBase.java:293}):
 * {@code new AIOpenDoor(this, true)} — i.e. the golem also closes the door behind it.
 * The original declares no priority of its own, so 5 is purely the installation index.
 * See {@link AIDoorInteract} for why the pair is ported rather than extending 1.21.1's
 * {@code OpenDoorGoal} (fence gates).</p>
 *
 * <h2>Pinned numbers and behaviour (original lines)</h2>
 * <ul>
 *   <li>{@code closeDoor} + {@code forgetTime = 20}: the goal lives while
 *       {@code closeDoor && forgetTime > 0 && super.canContinue} ({@code :21-23}), the
 *       counter starts at 20 ({@code :27}) and burns one per tick ({@code :61}) —
 *       identical to 1.21.1's {@code OpenDoorGoal} ({@code OpenDoorGoal.java:12, 22-24,
 *       43-45, 54}).</li>
 *   <li>the wooden door: the original calls {@code BlockDoor.openCloseDoor(world, x, y, z,
 *       true)} ({@code :29}, {@code :48}) — 1.21.1's {@code DoorBlock.setOpen}, which
 *       finds the door's other half, plays the door sound and fires the block event, so
 *       no hand-rolled sound is needed on this branch (the original plays none here
 *       either).</li>
 *   <li>the fence gate ({@code :30-41, 49-55}): the metadata arithmetic is
 *       {@code meta | 4} = open, {@code meta & -5} = close, and
 *       {@code BlockFenceGate.func_149896_b(meta)} = "is open". The orientation fix
 *       ({@code :33-37}) reads the golem's yaw as
 *       {@code floor(rotationYaw * 4 / 360 + 0.5) & 3}, i.e. one of the four horizontal
 *       directions, and turns the gate to face the golem when it currently faces the
 *       other way. 1.21.1's equivalent pair is {@code Direction.fromYRot(yaw)} and
 *       {@code Direction.getOpposite()} (the {@code (n + 2) \% 4} is the 1.7.10 data-value
 *       form of "opposite"), and the {@code | 4}/{@code & -5} bits become the
 *       {@link FenceGateBlock#OPEN} property.</li>
 *   <li>the sound: 1.7.10's {@code world.playSound((EntityPlayer) null, x, y, z, 1003, 0)}
 *       — sound-event id 1003 is {@code Blocks.fence_gate}'s own sound — becomes the
 *       block's own {@code openSound}/{@code closeSound} at {@code SoundSource.BLOCKS}.</li>
 * </ul>
 */
public final class AIOpenDoor extends AIDoorInteract {

    /** The original's {@code field_75361_i} ({@code :11}) — the constructor's {@code par2}, {@code true} from the golem. */
    private final boolean closeDoor;

    /** The original's {@code field_75360_j} ({@code :12}) — {@code forgetTime}, 20 ticks, one per tick. */
    private int forgetTime;

    public AIOpenDoor(final EntityGolemBase golem, final boolean closeDoor) {
        super(golem);
        this.closeDoor = closeDoor;
    }

    /** Original {@code func_75253_b()} ({@code :21-23}): a one-way golem lets go of the door at once. */
    @Override
    public boolean canContinueToUse() {
        return this.closeDoor && this.forgetTime > 0 && super.canContinueToUse();
    }

    /** Original {@code func_75249_e()} ({@code :26-43}): open the door, or open (and re-face) the gate. */
    @Override
    public void start() {
        this.forgetTime = 20;
        final BlockPos pos = this.doorPos();
        // DEVIATION: the original branches on the cached targetDoor Block identity and passes that
        // Block plus the world metadata; 1.21.1 carries metadata inside BlockState, so the port
        // re-reads the state at the doorway and branches on the block type.
        final BlockState state = this.level().getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(this.golem, this.level(), state, pos, true);
            return;
        }
        if (state.getBlock() instanceof FenceGateBlock gate && !state.getValue(FenceGateBlock.OPEN)) {
            BlockState open = state.setValue(FenceGateBlock.OPEN, Boolean.TRUE);
            // Original :33-37: turn the gate toward the golem when it faces the other way; the
            // 1.7.10 (n + 2) % 4 data-value test is Direction.getOpposite(), and the meta | 4 bit
            // that opens the gate is the OPEN property.
            final Direction golemFacing = Direction.fromYRot(this.golem.getYRot());
            if (open.hasProperty(HorizontalDirectionalBlock.FACING)
                    && open.getValue(HorizontalDirectionalBlock.FACING) == golemFacing.getOpposite()) {
                open = open.setValue(HorizontalDirectionalBlock.FACING, golemFacing);
            }
            this.level().setBlock(pos, open, 3);
            this.level().playSound(null, pos, gate.openSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Original {@code func_75251_c()} ({@code :45-57}): the same two branches, closing. */
    @Override
    public void stop() {
        if (!this.closeDoor) {
            return;
        }
        final BlockPos pos = this.doorPos();
        final BlockState state = this.level().getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(this.golem, this.level(), state, pos, false);
            return;
        }
        if (state.getBlock() instanceof FenceGateBlock gate && state.getValue(FenceGateBlock.OPEN)) {
            this.level().setBlock(pos, state.setValue(FenceGateBlock.OPEN, Boolean.FALSE), 3);
            this.level().playSound(null, pos, gate.closeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Original {@code func_75246_d()} ({@code :60-63}): burn the forget counter, then run the base tick. */
    @Override
    public void tick() {
        this.forgetTime--;
        super.tick();
    }
}
