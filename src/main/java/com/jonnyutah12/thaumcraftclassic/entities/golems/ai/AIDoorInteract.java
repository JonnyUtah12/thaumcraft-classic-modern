package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/**
 * The door-walking base of {@link AIOpenDoor} — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/misc/AIDoorInteract.java},
 * Thaumcraft's copy of 1.7.10's {@code EntityAIDoorInteract}. It is the base class, not
 * a goal of its own: the original declares it {@code abstract} (line 12) and
 * {@code setupGolem()} installs only {@code AIOpenDoor}
 * ({@code EntityGolemBase.java:293}), so this file ships package-private.
 *
 * <h2>Why port the pair instead of extending 1.21.1's {@code OpenDoorGoal}</h2>
 * {@code OpenDoorGoal}/{@code DoorInteractGoal} reproduce the wooden-door half, and
 * their shape is identical to Thaumcraft's copy ({@code forgetTime = 20}, the
 * {@code closeDoor && forgetTime > 0} continuation, the dot-product "I have passed the
 * doorway" test, the 200/100 tick budget). They do <b>not</b> handle fence gates, and the
 * original does ({@code AIOpenDoor.java:31-41, 49-55}: open the gate, turn it to face the
 * golem when it faces the other way, and play the gate sound). Porting the pair keeps the
 * fence-gate arm and lets every constant stay quoted to the original.
 *
 * <h2>Pinned numbers (original lines)</h2>
 * <ul>
 *   <li>the trigger: the golem must have collided horizontally
 *       ({@code field_70123_F} → {@code Entity.horizontalCollision}, {@code :28}), and
 *       there must be a live path with door-opening navigation enabled
 *       ({@code :31-33});</li>
 *   <li>the node scan: the first {@code min(currentPathIndex + 2, nodeCount)} nodes
 *       ({@code :34}), each accepted within a squared XZ distance of {@code 2.25}
 *       ({@code :39});</li>
 *   <li>the fallback: when the scan finds nothing, the block at the golem's own height is
 *       checked ({@code :48-56}). {@code // DEVIATION: 1.21.1's vanilla goal uses the block
 *       above the mob} ({@code DoorInteractGoal.java:71}); the port keeps the original's
 *       feet-level read, because 1.7.10's path points and the mob's {@code posY} both sat
 *       at the door's lower half. For the node scan that same convention change means the
 *       doorway is {@code node.y + 1} — 1.21.1's {@code Node.y} is the block under the
 *       walkable space, and vanilla's door goal adds the same 1
 *       ({@code DoorInteractGoal.java:62}).</li>
 *   <li>the budgets: {@code count = 200} when the goal arms, {@code count = 100} on
 *       {@code start}, decremented every tick, and the goal lives while
 *       {@code count > 0} and the golem has not passed the doorway
 *       ({@code :42, 63-68, 74-81}).</li>
 * </ul>
 *
 * <p>{@code // DEVIATION: the usable-door set.} The original accepts exactly two blocks,
 * {@code Blocks.wooden_door} ({@code field_150466_ao}) and
 * {@code Blocks.fence_gate} ({@code field_150396_be}), and returns "air" — its own
 * not-a-door sentinel — for everything else ({@code findUsableDoor}, {@code :84-87}),
 * which the caller compares against {@code Blocks.air} ({@code field_150350_a}). 1.21.1
 * split the wooden door into six blocks, so the port reads the set as
 * {@code DoorBlock.isWoodenDoor(state)} (the six wooden doors, i.e. exactly what vanilla's
 * own door goal accepts) and the {@code fence_gates} tag. Iron doors stay refused, as in
 * the original.</p>
 */
abstract class AIDoorInteract extends GolemGoal {

    /** The original's {@code entityPosX/Y/Z} — the doorway the goal is working on ({@code :14-16}). */
    protected int entityPosX;
    protected int entityPosY;
    protected int entityPosZ;

    /**
     * The original's {@code targetDoor} — a {@code Block}, with air as its "nothing here"
     * sentinel ({@code :17, 41, 56}). 1.21.1 has no metadata-carrying {@code Block}, so the
     * port carries the {@link BlockState} it read and the sentinel stays
     * {@code Blocks.AIR}.
     */
    protected BlockState targetDoor;

    /** The original's {@code hasStoppedDoorInteraction} / {@code entityPositionX/Z} / {@code count} ({@code :18-21}). */
    private boolean hasStoppedDoorInteraction;
    private float entityPositionX;
    private float entityPositionZ;
    private int count;

    protected AIDoorInteract(final EntityGolemBase golem) {
        super(golem);
        // The original sets no mutex bits, and neither does 1.21.1's DoorInteractGoal: the
        // goal reads the path and flips a block, it does not steer the mob.
        this.count = 0;
    }

    /**
     * Original {@code func_75250_a()} ({@code :27-61}): only while the golem is pressed
     * against something, scan the front of the current path for a door within
     * {@code 2.25} squared blocks, and fall back to the block under the golem.
     */
    @Override
    public boolean canUse() {
        if (!this.golem.horizontalCollision) {
            return false;
        }
        final GroundPathNavigation navigation =
                this.golem.getNavigation() instanceof GroundPathNavigation ground ? ground : null;
        if (navigation == null) {
            return false;
        }
        final Path path = navigation.getPath();
        if (path == null || path.isDone() || !navigation.canOpenDoors()) {
            return false;
        }

        for (int index = 0; index < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); index++) {
            final Node node = path.getNode(index);
            // DEVIATION: the doorway block is node.y + 1, not node.y. 1.7.10's PathPoint.y sat
            // at the walkable height (the door's lower half), while 1.21.1's Node.y is the block
            // under it, so the +1 reproduces the original's target block. 1.21.1's own
            // DoorInteractGoal.java:62 uses the same node.y + 1.
            this.entityPosX = node.x;
            this.entityPosY = node.y + 1;
            this.entityPosZ = node.z;
            if (this.golem.distanceToSqr(this.entityPosX, this.golem.getY(), this.entityPosZ) <= 2.25) {
                this.targetDoor = this.findUsableDoor(this.entityPosX, this.entityPosY, this.entityPosZ);
                if (this.targetDoor != null && !this.targetDoor.isAir()) {
                    this.count = 200;
                    return true;
                }
            }
        }

        // DEVIATION: the fallback reads the block at the golem's own height (the original's
        // func_76128_c(posY), :48-50); 1.21.1's vanilla goal reads blockPosition().above()
        // (DoorInteractGoal.java:71). At the feet level that is the door's lower half, which is
        // the block the original opened, so the original's choice is kept.
        this.entityPosX = net.minecraft.util.Mth.floor(this.golem.getX());
        this.entityPosY = net.minecraft.util.Mth.floor(this.golem.getY());
        this.entityPosZ = net.minecraft.util.Mth.floor(this.golem.getZ());
        this.targetDoor = this.findUsableDoor(this.entityPosX, this.entityPosY, this.entityPosZ);
        if (this.targetDoor != null && !this.targetDoor.isAir()) {
            this.count = 200;
        }
        return this.targetDoor != null && !this.targetDoor.isAir();
    }

    /** Original {@code func_75253_b()} ({@code :63-65}): live while the tick budget runs and the golem has not passed through. */
    @Override
    public boolean canContinueToUse() {
        return this.count > 0 && !this.hasStoppedDoorInteraction;
    }

    /** Original {@code func_75249_e()} ({@code :67-72}): budget 100, remember which way the golem was walking. */
    @Override
    public void start() {
        this.count = 100;
        this.hasStoppedDoorInteraction = false;
        this.entityPositionX = (float) (this.entityPosX + 0.5F - this.golem.getX());
        this.entityPositionZ = (float) (this.entityPosZ + 0.5F - this.golem.getZ());
    }

    /**
     * Original {@code func_75246_d()} ({@code :74-82}): burn a tick, and stop caring about
     * the door as soon as the golem's dot product against the doorway turns negative —
     * it has walked past it.
     */
    @Override
    public void tick() {
        this.count--;
        final float dx = (float) (this.entityPosX + 0.5F - this.golem.getX());
        final float dz = (float) (this.entityPosZ + 0.5F - this.golem.getZ());
        final float dot = this.entityPositionX * dx + this.entityPositionZ * dz;
        if (dot < 0.0F) {
            this.hasStoppedDoorInteraction = true;
        }
    }

    /** 1.7.10 runs every live task every tick, so the {@code count} budget is a tick count. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * The original's {@code findUsableDoor(int, int, int)} ({@code :84-87}): the block is
     * usable when it is the wooden door or the fence gate; anything else comes back as the
     * air sentinel. See the class javadoc for the 1.21.1 block-set mapping.
     */
    private BlockState findUsableDoor(final int x, final int y, final int z) {
        // DEVIATION: the original names two blocks (wooden_door, fence_gate); 1.21.1 split the
        // wooden door into six, so the set is isWoodenDoor (the six vanilla doors) + fence_gates.
        final BlockState state = this.level().getBlockState(new BlockPos(x, y, z));
        if (DoorBlock.isWoodenDoor(state) || state.is(BlockTags.FENCE_GATES)) {
            return state;
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    /** The doorway position the subclass flips ({@code entityPosX/Y/Z} in the original). */
    protected final BlockPos doorPos() {
        return new BlockPos(this.entityPosX, this.entityPosY, this.entityPosZ);
    }
}
