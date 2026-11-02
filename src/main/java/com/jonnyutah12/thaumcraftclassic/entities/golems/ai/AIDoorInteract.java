package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Ported {@code thaumcraft.common.entities.ai.misc.AIDoorInteract} from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIDoorInteract.java}
 * (77 lines) — TC-M6-08-s-b2. This is Thaumcraft's own copy of the 1.7.10
 * vanilla {@code EntityAIGetDoor}, re-typed to the golem; the abstract base of
 * {@link AIOpenDoor}.
 *
 * <p>Pinned constants, all from the original file: the path-node scan
 * {@code min(currentPathIndex + 2, pathSize)} ({@code AIDoorInteract.java:29});
 * the door-proximity gate {@code distanceSq(nodeX, entityY, nodeZ) <= 2.25}
 * ({@code AIDoorInteract.java:35}); the budget {@code count = 200} on
 * activation ({@code AIDoorInteract.java:38, 45}); {@code count = 100} in
 * {@code start()} ({@code AIDoorInteract.java:52}); the pass-through test —
 * the dot product of the door-relative offsets, negative once the entity has
 * crossed the door centre ({@code AIDoorInteract.java:59-67}).
 * {@code count > 0 && !hasStoppedDoorInteraction} is the continue gate
 * ({@code AIDoorInteract.java:49-51}).</p>
 *
 * <p>The installed-priority contract: the original adds the concrete subclass
 * {@code AIOpenDoor} at priority 5 in {@code setupGolem()}'s base goal list
 * (original {@code EntityGolemBase.java:217-296}); this base itself is never
 * installed (it is abstract in the original too).</p>
 *
 * <h2>1.7.10 → 1.21.1</h2>
 * <p>DEVIATION (BD-19): the gate {@code theEntity.field_70123_F}
 * ({@code AIDoorInteract.java:24}) is the 1.7.10 {@code PathfinderMob} boolean
 * that mirrored "this mob's navigator may open doors"; 1.21.1 keeps that flag
 * only on {@link GroundPathNavigation#canOpenDoors()}, so the port reads the
 * navigator (the same flag the original's inner
 * {@code var1.func_75507_c()} gate, {@code AIDoorInteract.java:27}, already
 * reads). Behaviour is identical for the golem, whose constructor turns the
 * navigator flag on.</p>
 *
 * <p>DEVIATION (BD-19): {@code findUsableDoor} whitelists the 1.7.10 door ids
 * wooden door (id 64, a {@code BlockDoor}) and iron door (id 71, a
 * {@code BlockIronDoor extends BlockDoor}) and returns air for everything
 * else, including fence gates ({@code AIDoorInteract.java:73-76}). 1.21.1
 * gives every wood its own door class, all under {@link DoorBlock}; the port
 * whitelists {@link DoorBlock#isWoodenDoor(Level, BlockPos)} and the iron
 * door, the exact id set.</p>
 */
public abstract class AIDoorInteract extends GolemGoal {

    /** The original's {@code entityPosX/Y/Z} — the door cell found on the path. */
    protected int entityPosX;
    protected int entityPosY;
    protected int entityPosZ;

    /** Original {@code targetDoor} (a {@code Block}); the port keeps the whole state. */
    protected BlockState targetDoor = Blocks.AIR.defaultBlockState();

    /** Original {@code hasStoppedDoorInteraction} — true once the entity crossed the door. */
    private boolean hasStoppedDoorInteraction;

    /** Original {@code entityPositionX/Z} — the door-relative offset captured at {@code start()}. */
    private float entityPositionX;
    private float entityPositionZ;

    /** Original {@code count} — the continue budget. */
    int count = 0;

    protected AIDoorInteract(final EntityGolemBase golem) {
        super(golem);
    }

    @Override
    public boolean canUse() {
        // DEVIATION (see class javadoc): field_70123_F becomes canOpenDoors().
        if (!(this.golem.getNavigation() instanceof GroundPathNavigation navigator)
                || !navigator.canOpenDoors()) {
            return false;
        }
        final Path path = navigator.getPath();
        if (path != null && !path.isDone() && navigator.canOpenDoors()) {
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
                final Node node = path.getNode(i);
                this.entityPosX = node.x;
                this.entityPosY = node.y;
                this.entityPosZ = node.z;
                if (this.golem.distanceToSqr(this.entityPosX, this.golem.getY(), this.entityPosZ) <= 2.25) {
                    this.targetDoor = this.findUsableDoor(this.entityPosX, this.entityPosY, this.entityPosZ);
                    if (!this.targetDoor.isAir()) {
                        this.count = 200;
                        return true;
                    }
                }
            }

            this.entityPosX = Mth.floor(this.golem.getX());
            this.entityPosY = Mth.floor(this.golem.getY());
            this.entityPosZ = Mth.floor(this.golem.getZ());
            this.targetDoor = this.findUsableDoor(this.entityPosX, this.entityPosY, this.entityPosZ);
            if (!this.targetDoor.isAir()) {
                this.count = 200;
            }
            return !this.targetDoor.isAir();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.hasStoppedDoorInteraction;
    }

    @Override
    public void start() {
        this.count = 100;
        this.hasStoppedDoorInteraction = false;
        this.entityPositionX = (float) (this.entityPosX + 0.5F - this.golem.getX());
        this.entityPositionZ = (float) (this.entityPosZ + 0.5F - this.golem.getZ());
    }

    @Override
    public void tick() {
        this.count--;
        final float var1 = (float) (this.entityPosX + 0.5F - this.golem.getX());
        final float var2 = (float) (this.entityPosZ + 0.5F - this.golem.getZ());
        final float var3 = this.entityPositionX * var1 + this.entityPositionZ * var2;
        if (var3 < 0.0F) {
            this.hasStoppedDoorInteraction = true;
        }
    }

    /** The door cell the current {@code targetDoor} sits at. */
    protected final BlockPos targetDoorPos() {
        return new BlockPos(this.entityPosX, this.entityPosY, this.entityPosZ);
    }

    /**
     * The original's {@code findUsableDoor} ({@code AIDoorInteract.java:73-76}):
     * the block at the cell, but air unless it is the wooden door
     * ({@code Blocks.field_150466_ao}) or the iron door
     * ({@code Blocks.field_150396_be}) — fence gates included in the air sweep.
     */
    private BlockState findUsableDoor(final int x, final int y, final int z) {
        final BlockPos pos = new BlockPos(x, y, z);
        final BlockState state = this.level().getBlockState(pos);
        if (DoorBlock.isWoodenDoor(state) || state.is(Blocks.IRON_DOOR)) {
            return state;
        }
        return Blocks.AIR.defaultBlockState();
    }
}
