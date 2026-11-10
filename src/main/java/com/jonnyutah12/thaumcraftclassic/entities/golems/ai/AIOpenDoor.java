package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported {@code thaumcraft.common.entities.ai.misc.AIOpenDoor} from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIOpenDoor.java}
 * (58 lines) — TC-M6-08-s-b2. Thaumcraft's copy of the 1.7.10 vanilla
 * {@code EntityAIOpenDoor}, re-typed to the golem.
 *
 * <p>The original is installed by {@code EntityGolemBase.setupGolem()} at
 * <b>priority 5</b> with {@code closeDoors = true} in the base goal list every
 * core gets (original {@code EntityGolemBase.java:217-296}). Pinned constants,
 * all from the original file: the open-forget timer {@code 20} ticks
 * ({@code AIOpenDoor.java:23}); continue gate {@code closeDoor &&
 * ticksSinceDoorOpen > 0 && super} ({@code AIOpenDoor.java:18-20}); the tick
 * decrements the forget timer then the base count ({@code
 * AIOpenDoor.java:54-57}); stop closes the door only when {@code closeDoor}
 * ({@code AIOpenDoor.java:41-52}) — this is the "closes it behind you"
 * behaviour. Note the original's {@code start()} deliberately does NOT call
 * {@code super.start()}: the base {@code count = 100} and the
 * {@code entityPositionX/Z} capture never run for this subclass
 * ({@code AIOpenDoor.java:21-39}); the port reproduces that quirk.</p>
 *
 * <h2>1.7.10 → 1.21.1</h2>
 * <p>DEVIATION: the wooden-door branch calls
 * {@code BlockDoor.openDoor/closeDoor} ({@code AIOpenDoor.java:24-25, 43-44});
 * 1.21.1's {@link DoorBlock#setOpen} is the same open/close (state flip +
 * sound). The iron-door branch of the original is the inherited vanilla fence
 * gate code running on an iron door: it reads the fence-gate open bit (meta
 * bit 4 — a bit the iron door ignores), writes {@code meta | 4} with update
 * flag 3 (an iron-door no-op), and plays record 1003, the block
 * open/close sound ({@code AIOpenDoor.java:26-38, 45-51}). The fence gate
 * itself is unreachable: {@code findUsableDoor} filters fence gates out
 * ({@code AIDoorInteract.java:74-76}). The port keeps the visible net effect
 * — the open sound at the iron door, state untouched — and closes nothing on
 * stop (the original's close branch is gated on that same ignored bit 4, so
 * it runs for nothing).</p>
 */
public class AIOpenDoor extends AIDoorInteract {

    /** Original {@code field_75361_i} — close the door again when the goal stops. */
    private final boolean closeDoor;

    /** Original {@code field_75360_j} — ticks the door stays open after {@code start()}. */
    private int ticksSinceDoorOpen = 0;

    public AIOpenDoor(final EntityGolemBase golem, final boolean closeDoor) {
        super(golem);
        this.closeDoor = closeDoor;
    }

    @Override
    public boolean canContinueToUse() {
        return this.closeDoor && this.ticksSinceDoorOpen > 0 && super.canContinueToUse();
    }

    @Override
    public void start() {
        // Original quirk (AIOpenDoor.java:21-39): no super.start() — the base
        // count/entityPosition capture is skipped, count stays at the canUse 200.
        this.ticksSinceDoorOpen = 20;
        final BlockPos pos = this.targetDoorPos();
        if (DoorBlock.isWoodenDoor(this.targetDoor)) {
            // Original BlockDoor.openDoor(..., true) (AIOpenDoor.java:24-25).
            ((DoorBlock) this.targetDoor.getBlock())
                    .setOpen(this.golem, this.level(), this.targetDoor, pos, true);
        } else if (this.targetDoor.is(Blocks.IRON_DOOR)) {
            // DEVIATION: the original's fence-gate meta write on the iron door
            // (bit 4, ignored by BlockDoor) is a state no-op in 1.7.10 too; the
            // only observable effect is record 1003, the block open/close note
            // (the wooden door's open sound in 1.21.1 terms).
            this.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void stop() {
        if (this.closeDoor) {
            final BlockPos pos = this.targetDoorPos();
            if (DoorBlock.isWoodenDoor(this.targetDoor)) {
                // Original BlockDoor.openDoor(..., false) (AIOpenDoor.java:43-44).
                ((DoorBlock) this.targetDoor.getBlock())
                        .setOpen(this.golem, this.level(), this.targetDoor, pos, false);
            }
            // Iron door: the original's close branch (AIOpenDoor.java:45-51) is
            // gated on the fence-gate open bit (meta bit 4) the iron door never
            // sets — it runs for nothing. Same no-op here.
        }
    }

    @Override
    public void tick() {
        this.ticksSinceDoorOpen--;
        super.tick();
    }
}
