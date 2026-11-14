package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/**
 * {@code thaumcraft:golemorb} — original
 * {@code thaumcraft.common.entities.projectile.EntityGolemOrb}
 * (content-mapping {@code Thaumcraft:GolemOrb}, kind=entity, mod_index 13,
 * display name "Energy Orb"). TC-M6-08-s-c.
 *
 * <p><b>Pinned from source</b> ({@code EntityGolemOrb.java}): the original
 * {@code extends EntityThrowable} and overrides {@code func_70185_h()}
 * ({@code getGravityVelocity}) to {@code 0.0F} — a gravity-free homing orb;
 * registration {@code registerModEntity(EntityGolemOrb.class, "GolemOrb", id,
 * instance, 64, 20, true)} ({@code ConfigEntities.java:89}) — tracking 64,
 * interval 20, velocity synced. Its lifetime clamps are
 * {@code age > (red ? 240 : 160)}, its homing step is 0.2 per tick toward
 * {@code target.y + height*0.6} with each velocity component clamped to
 * ±0.25, and its hit damage is the owner's attack damage × {@code red ? 1.0
 * : 0.6}. Those figures are the entity's, not this card's, and are recorded
 * here as the port's spec; the behaviour half (firing, target selection) is
 * golem behaviour (TC-M6-08-s-b2).</p>
 *
 * <p>The {@code red} flag and the target id are the original's
 * {@code IEntityAdditionalSpawnData} payload ({@code writeSpawnData}:
 * {@code int id} then {@code boolean red}), carried on the port as plain
 * fields; the entity is registered with velocity sync so a client sees the
 * orb move, and the extra spawn data is not reproduced (recorded in
 * docs/BEHAVIORAL_DEVIATIONS.md).</p>
 */
public class EntityGolemOrb extends ThrowableProjectile {

    /** Original {@code func_70185_h()} — the orb falls at 0 gravity. */
    public static final float GRAVITY = 0.0F;
    /** Original lifetime clamp for the plain (not red) orb. */
    public static final int LIFETIME = 160;
    /** Original lifetime clamp for the red (doubled-damage) orb. */
    public static final int LIFETIME_RED = 240;
    /** Original per-tick homing acceleration. */
    public static final double HOMING = 0.2;
    /** Original clamp on every velocity component while homing. */
    public static final double HOMING_CLAMP = 0.25;

    /** Original {@code public boolean red} — the doubled-damage variant. */
    private boolean red;

    public EntityGolemOrb(final EntityType<? extends EntityGolemOrb> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(final net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // The original's synched state is the EntityThrowable body: nothing of
        // its own. The red flag and the target id travelled in the spawn
        // payload (writeSpawnData), not in synched data — see the class note.
    }

    public boolean isRed() {
        return this.red;
    }

    public void setRed(final boolean red) {
        this.red = red;
    }
}
