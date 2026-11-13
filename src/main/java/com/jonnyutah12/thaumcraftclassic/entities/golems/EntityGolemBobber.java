package com.jonnyutah12.thaumcraftclassic.entities.golems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * {@code thaumcraft:golembobber} — original
 * {@code thaumcraft.common.entities.golems.EntityGolemBobber}
 * (content-mapping {@code Thaumcraft:GolemBobber}, kind=entity, mod_index 49,
 * display name "Golem Fishing Bobber"). TC-M6-08-s-c.
 *
 * <p><b>Pinned from source</b> ({@code EntityGolemBobber.java}): the original
 * {@code extends Entity implements IEntityAdditionalSpawnData} with the hitbox
 * {@code func_70105_a(0.25F, 0.25F)} in both constructors ({@code :49} and
 * {@code :58}) and the registration
 * {@code registerModEntity(EntityGolemBobber.class, "GolemBobber", id,
 * instance, 64, 64, false)} ({@code ConfigEntities.java:154}) — tracking 64,
 * update interval 64, no velocity sync (the bobber tells its own position
 * through the spawn payload).</p>
 *
 * <p>The bobber's whole body is the fishing-core state machine (bite timers,
 * the {@code hookedEntity}/{@code golem} links, the line FX) and belongs to
 * golem behaviour (TC-M6-08-s-b2); this class ships the entity type, its
 * hitbox and the persistence seams so the type is real and registered.</p>
 */
public class EntityGolemBobber extends Entity {

    /**
     * Original {@code public EntityGolemBase fisher = null}
     * ({@code reference/.../common/entities/golems/EntityGolemBobber.java:25}).
     * A transient, unsaved, unsynched pointer, set at the throw site — the
     * bobber's renderer draws its line to it, gated by the {@code fisher != null}
     * check of {@code RenderGolemBobber.java:45}. The throw site itself is golem
     * behaviour (TC-M6-08-s-b2, t_03c5d088), so the renderer draws only the ball
     * until that card wires it.
     */
    private EntityGolemBase fisher;

    public EntityGolemBobber(final EntityType<? extends EntityGolemBobber> type, final Level level) {
        super(type, level);
    }

    /** The original's public {@code fisher} field, as an accessor pair. */
    public EntityGolemBase getFisher() {
        return this.fisher;
    }

    public void setFisher(final EntityGolemBase fisher) {
        this.fisher = fisher;
    }

    @Override
    protected void defineSynchedData(final net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // 1.7.10 wrote the bobber's state to the spawn payload
        // (writeSpawnData/readSpawnData), not to synched data; see the class
        // note. Nothing to declare.
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        // Bobber state (bite timer, hooked entity) is TC-M6-08-s-b2.
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        // Bobber state (bite timer, hooked entity) is TC-M6-08-s-b2.
    }
}
