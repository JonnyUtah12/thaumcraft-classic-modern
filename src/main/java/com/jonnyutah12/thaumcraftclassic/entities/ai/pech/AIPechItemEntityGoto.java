package com.jonnyutah12.thaumcraftclassic.entities.ai.pech;

import java.util.EnumSet;
import java.util.List;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The Pech's item-retrieval goal — a line-by-line port of
 * {@code thaumcraft.common.entities.ai.pech.AIPechItemEntityGoto} (122 lines),
 * installed at goal slot 3 ({@code EntityPech.java:97}).
 *
 * <p>Every figure is on {@link PechAiLogic}, cited to the original's line: the
 * 16-block scan box ({@code :52}), the {@code Config.golemDelay} cadence
 * ({@code :62}, {@code Config.java:130} = 5), the failed-path penalty
 * ({@code :119}, {@code :127}, {@code :130}), the 1.5 speed factor
 * ({@code :111}, {@code :120}), the 1.5 squared-blocks pickup radius
 * ({@code :135}), the {@code "PechDrop"} owner exclusion ({@code :78}) and the
 * pop sound ({@code :146-154}).</p>
 *
 * <p>Two 1.21.1 seams are recorded, not invented:</p>
 * <ul>
 *   <li>the owner <b>name</b> is gone from {@code ItemEntity} (1.21.1's
 *       {@code getOwner()} returns an {@code Entity}), so the marker rides the
 *       vanilla {@code minecraft:entity_data} component under the original's
 *       key; the site that wrote it ({@code ContainerPech.java:211},
 *       {@code setOwner("PechDrop")}) is the container/menu lane this card
 *       leaves out of scope, so the guard is live code over a tag nothing
 *       writes today — the pech's own death drops ({@code :326}
 *       {@code entityDropItem}) never carried the tag either, so the guard's
 *       behaviour is unchanged for them;</li>
 *   <li>1.7.10's {@code PathNavigate.func_75494_a(Entity)} (the entity-path
 *       helper, whose vanilla body passes the 16-block range) is
 *       {@code createPath(Entity, 16)}.</li>
 * </ul>
 */
public class AIPechItemEntityGoto extends Goal {

    /** {@code :76-77} — the original reads the item entity's free-form entity
     *  data; 1.21.1's slot for that is the vanilla {@code entity_data}
     *  component. */
    static final String OWNER_KEY = "Owner";

    private final EntityPech pech;
    private Entity targetEntity;
    private int count;
    private int failedPathFindingPenalty;

    public AIPechItemEntityGoto(final EntityPech pech) {
        this.pech = pech;
        // :58 — func_75248_a(3): 1.7.10 mutex bit 1 = MOVE, bit 2 = LOOK
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.pech.tickCount % PechAiLogic.GOLEM_DELAY > 0) {
            return false;                                                       // :62
        }
        if (--this.count > 0) {
            return false;                                                       // :64
        }
        double range = Double.MAX_VALUE;
        this.targetEntity = null;
        final List<Entity> targets = this.pech.level().getEntities(this.pech,
                this.pech.getBoundingBox().inflate(PechAiLogic.PICKUP_TARGET_RANGE,
                        PechAiLogic.PICKUP_TARGET_RANGE, PechAiLogic.PICKUP_TARGET_RANGE),
                entity -> true);                                                // :68-70
        if (targets.isEmpty()) {
            return false;                                                       // :71-72
        }
        for (final Entity e : targets) {
            if (e instanceof ItemEntity item && this.pech.canPickup(item.getItem())) {   // :75
                final String username = ownerName(item);                        // :76-77
                if (username == null
                        || !PechAiLogic.isPechDrop(username)) {                 // :78
                    final double distance = e.distanceToSqr(this.pech.getX(),
                            this.pech.getY(), this.pech.getZ());               // :79
                    if (distance < range
                            && distance <= PechAiLogic.PICKUP_TARGET_RANGE
                                    * PechAiLogic.PICKUP_TARGET_RANGE) {        // :80
                        range = distance;
                        this.targetEntity = e;                                  // :81-82
                    }
                }
            }
        }
        return this.targetEntity != null;                                       // :88
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetEntity != null                                        // :94
                && !this.targetEntity.isRemoved()                               // :97
                && !this.pech.getNavigation().isDone()                          // :99
                && this.targetEntity.distanceToSqr(this.pech)
                        < PechAiLogic.PICKUP_TARGET_RANGE
                                * PechAiLogic.PICKUP_TARGET_RANGE;              // :99
    }

    @Override
    public void stop() {
        this.targetEntity = null;                                               // :104
    }

    @Override
    public void start() {
        final PathNavigation nav = this.pech.getNavigation();
        final Path path = nav.createPath(this.targetEntity, 16);                // :110
        nav.moveTo(path, movementSpeed() * PechAiLogic.PICKUP_SPEED_FACTOR);    // :111
        this.count = 0;                                                         // :113
    }

    @Override
    public void tick() {
        this.pech.getLookControl().setLookAt(this.targetEntity,
                PechAiLogic.PICKUP_LOOK_YAW, PechAiLogic.PICKUP_LOOK_PITCH);     // :117
        final PathNavigation nav = this.pech.getNavigation();
        if (nav.isDone() && --this.count <= 0) {                                // :118
            this.count = this.failedPathFindingPenalty + PechAiLogic.PENALTY_BASE
                    + this.pech.getRandom().nextInt(PechAiLogic.PENALTY_BASE);   // :119
            nav.moveTo(this.targetEntity,
                    movementSpeed() * PechAiLogic.PICKUP_SPEED_FACTOR);          // :120
            if (nav.getPath() != null) {                                        // :121
                // :122 — 1.7.10's Path#getEndPos(): the last node's centre.
                // 21.1.259's Path exposes getEndNode() (Path.java:42) and Node
                // carries integer block x/y/z (Node.java:10-12), so the centre is
                // built with the vanilla node-centre convention (x+0.5, y, z+0.5).
                final net.minecraft.world.level.pathfinder.Node end = nav.getPath().getEndNode();
                final var finalPathPoint = end == null ? null
                        : new net.minecraft.world.phys.Vec3(end.x + 0.5, end.y, end.z + 0.5);
                if (finalPathPoint != null
                        && this.targetEntity.distanceToSqr(finalPathPoint.x,
                                finalPathPoint.y, finalPathPoint.z)
                                < PechAiLogic.PENALTY_END_DISTANCE_SQ) {          // :124
                    this.failedPathFindingPenalty = 0;                          // :125
                } else {
                    this.failedPathFindingPenalty += PechAiLogic.PENALTY_FAIL;  // :127
                }
            } else {
                this.failedPathFindingPenalty += PechAiLogic.PENALTY_FAIL;      // :130
            }
        }

        final double distance = this.pech.distanceToSqr(this.targetEntity.getX(),
                this.targetEntity.getBoundingBox().minY, this.targetEntity.getZ()); // :134
        if (distance <= PechAiLogic.PICKUP_DISTANCE_SQ) {                       // :135
            this.count = 0;                                                     // :136
            final ItemEntity item = (ItemEntity) this.targetEntity;
            final int am = item.getItem().getCount();                           // :137
            final ItemStack is = this.pech.pickupItem(item.getItem());          // :138
            if (is != null && is.getCount() > 0) {
                item.setItem(is);                                               // :140
            } else {
                this.targetEntity.discard();                                    // :142
            }
            if (is == null || is.getCount() != am) {                            // :145
                // :146-154 — world.playSound(target, "random.pop", 0.2F,
                // (rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F
                final float pitch = (this.pech.getRandom().nextFloat()
                        - this.pech.getRandom().nextFloat()) * 0.7F + 1.0F;
                this.pech.level().playSound(null, this.targetEntity, SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS, 0.2F, pitch * 2.0F);
            }
        }
    }

    /** {@code :111}/{@code :120} — {@code getAttributeInstance(movementSpeed)
     *  .getAttributeValue()} ({@code field_111263_d} = movementSpeed). */
    private double movementSpeed() {
        final var instance = this.pech.getAttribute(Attributes.MOVEMENT_SPEED);
        return instance == null ? 0.0 : instance.getValue();
    }

    /** {@code :76-77} — the original's {@code getEntityData().getString("Owner")}
     *  shape: read the vanilla {@code entity_data} component, no tag → null. */
    private static String ownerName(final ItemEntity item) {
        final var data = item.getItem().get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
        if (data == null) {
            return null;
        }
        final CompoundTag tag = data.copyTag();                                       // CustomData#copyTag (21.1.259 has no copy())
        return tag.contains(OWNER_KEY, Tag.TAG_STRING) ? tag.getString(OWNER_KEY) : null;  // CompoundTag#isString is gone
    }
}
