package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * {@code thaumcraft:golemorb} — original
 * {@code thaumcraft.common.entities.projectile.EntityGolemOrb} (127 lines,
 * content-mapping {@code Thaumcraft:GolemOrb}, kind=entity, mod_index 13,
 * display name "Energy Orb", CSV line 311), {@code extends EntityThrowable
 * implements IEntityAdditionalSpawnData}. Registration
 * {@code ConfigEntities.java:89} — {@code registerModEntity("GolemOrb", id,
 * instance, 64, 20, true)}: tracking 64 blocks, <b>update interval 20</b>,
 * velocity updates on.
 *
 * <p>TC-M6-08-s-c registered this type as a skeleton and named the behaviour
 * half as the golem/monster cards' work (BD-39, "TEMPORARY (deferred
 * behaviour)"). TC-M6-06-s-c4a is the card that fires the orb — the Crimson
 * Cleric's ranged attack ({@code EntityCultistCleric:70}) — so the behaviour
 * half is here, each figure from the decompiled source:</p>
 * <ul>
 *   <li><b>no gravity</b> — {@code func_70185_h() = 0.0F} ({@code :31-33}) →
 *       {@code getDefaultGravity() = 0.0};</li>
 *   <li><b>a lifetime clamp</b> — {@code ticksExisted > (red ? 240 : 160)}
 *       ({@code :84-86});</li>
 *   <li><b>homing</b> — each tick the unit vector toward
 *       {@code (target.x, target.BB.minY + target.height * 0.6, target.z)} is
 *       added to the velocity scaled by 0.2, then every velocity component is
 *       clamped to ±0.25 ({@code :88-105});</li>
 *   <li><b>a hit that hits once</b> — on an entity hit, the owner's
 *       {@code attackDamage} attribute × {@code red ? 1.0F : 0.6F}, from an
 *       indirect-magic source owned by the owner ({@code :51-56});</li>
 *   <li><b>the spawn payload</b> — the target's entity id ({@code -1} when
 *       there is none) then the {@code red} boolean, in that order
 *       ({@code :35-47}), so a client sees the orb home on the right mob;</li>
 *   <li><b>a melee parry</b> — any hit zeroes the velocity and inherits 0.9 of
 *       the attacker's look vector ({@code :113-131});</li>
 *   <li>{@code getPickRadius() = 0.1F} ({@code :69-71}).</li>
 * </ul>
 *
 * <p><b>The {@code red} flag is the constructor's last argument and every
 * caller is pinned.</b> The Crimson Cleric passes <b>true</b>
 * ({@code EntityCultistCleric.java:70}), the eldritch golem passes <b>false</b>
 * ({@code boss/EntityEldritchGolem.java:225}) and the Cultist Leader passes
 * <b>true</b> ({@code boss/EntityCultistLeader.java:191}, card c5). The flag
 * drives the lifetime clamp <i>and</i> the damage factor, so the pair is
 * asserted together in one probe.</p>
 *
 * <p>Deviations, recorded in {@code docs/BEHAVIORAL_DEVIATIONS.md}: the impact
 * burst ({@code :62}, {@code Thaumcraft.proxy.burst(world, x, y, z, 1.0F)}) and
 * the two sounds — {@code thaumcraft:shock} at the impact ({@code :58-61}) and
 * {@code thaumcraft:zap} on a parry ({@code :127}) — are sends owned by the FX
 * lane (docs/fx-events.md) and the audio lane (docs/sound-events.md, D-004):
 * their ids and figures ship pinned as constants and nothing is sent, because
 * firing a missing sound id writes a console warning on every impact. The
 * homing step skips the frame when the distance is 0: the original's
 * normalisation divides by that distance and lands NaN in the velocity, and
 * 1.21.1 cannot encode a NaN position (the pech arrow's
 * {@code Cannot encode empty ItemStack} chunk-save class is the same family) —
 * a platform guard, not a behaviour change. The original overrides no NBT hook,
 * so a saved orb restarts with no target and not red: recorded, not "fixed".</p>
 */
public class EntityGolemOrb extends ThrowableProjectile implements IEntityWithComplexSpawn {

    /** Original {@code func_70185_h()} — the orb falls at 0 gravity. */
    public static final float GRAVITY = 0.0F;
    /** Original lifetime clamp for the plain (not red) orb ({@code :85}). */
    public static final int LIFETIME = 160;
    /** Original lifetime clamp for the red (doubled-damage) orb ({@code :85}). */
    public static final int LIFETIME_RED = 240;
    /** Original per-tick homing acceleration ({@code :96} — {@code d13 = 0.2}). */
    public static final double HOMING = 0.2;
    /** Original clamp on every velocity component while homing ({@code :102-104}). */
    public static final double HOMING_CLAMP = 0.25;
    /** {@code :92} — the aim point is the target's {@code BB.minY + height * 0.6}. */
    public static final double HOMING_HEIGHT_FACTOR = 0.6;
    /** {@code :126} — the parry inherits 0.9 of the attacker's look vector. */
    public static final double PARRY_LOOK_FACTOR = 0.9;
    /** {@code :69-71} — {@code func_70053_R() = 0.1F} (the crosshair reach). */
    public static final float PICK_RADIUS = 0.1F;
    /** {@code :56} — the damage factors of the {@code red} flag. */
    public static final float DAMAGE_RED = 1.0F;
    public static final float DAMAGE_PLAIN = 0.6F;
    /** {@code :58-61} — the impact sound's id, volume and pitch spread. */
    public static final ResourceLocation SOUND_IMPACT =
            ResourceLocation.parse("thaumcraft:shock");
    public static final float SOUND_VOLUME = 1.0F;
    public static final float SOUND_PITCH_SPREAD = 0.2F;
    /** {@code :127} — the parry sound's id. */
    public static final ResourceLocation SOUND_PARRY =
            ResourceLocation.parse("thaumcraft:zap");
    /** {@code :62} — {@code Thaumcraft.proxy.burst(world, x, y, z, 1.0F)}: the
     *  FX lane's send, pinned with its scale. */
    public static final float BURST_SCALE = 1.0F;

    /** {@code :14} — {@code int targetID}: the spawn payload's carrier. */
    private int targetID = -1;
    /** {@code :15} — {@code EntityLivingBase target}. */
    @Nullable
    private LivingEntity target;
    /** {@code :16} — {@code public boolean red} — the doubled-damage variant. */
    private boolean red;

    // The (type, level) form takes the EXACT EntityType<EntityGolemOrb>:
    // EntityType.Builder.of(factory, category) infers its type variable from the
    // ctor reference, and a `? extends EntityGolemOrb` parameter makes javac
    // infer T = Entity and reject the builder.
    public EntityGolemOrb(final EntityType<EntityGolemOrb> type, final Level level) {
        super(type, level);
    }

    /**
     * {@code :22-26} — the thrower constructor the cleric uses:
     * {@code new EntityGolemOrb(world, this, target, red)}. 1.7.10's
     * {@code EntityThrowable(World, EntityLivingBase)} is 1.21.1's
     * {@code ThrowableProjectile(EntityType, LivingEntity, Level)}: it places the
     * projectile at the owner's {@code eyeY - 0.1} and records the owner, so the
     * caller's {@code +motion/2} nudge lands on the same start as the original's.
     */
    public EntityGolemOrb(final EntityType<EntityGolemOrb> type, final Level level,
            final LivingEntity owner, @Nullable final LivingEntity target, final boolean red) {
        super(type, owner, level);
        this.target = target;
        this.targetID = target == null ? -1 : target.getId();
        this.red = red;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        // The original's synched state is the EntityThrowable body: nothing of
        // its own. The red flag and the target id travelled in the spawn
        // payload (writeSpawnData, :35-47) — the entity is registered with
        // velocity sync (updateInterval 20, velocity updates true) so a client
        // sees the orb move, and IEntityWithComplexSpawn carries the rest.
    }

    /** {@code :31-33} — the orb is gravity-free. */
    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** {@code :69-71}. */
    @Override
    public float getPickRadius() {
        return PICK_RADIUS;
    }

    public boolean isRed() {
        return this.red;
    }

    public void setRed(final boolean red) {
        this.red = red;
    }

    @Nullable
    public LivingEntity getOrbTarget() {
        return this.target;
    }

    public void setOrbTarget(@Nullable final LivingEntity target) {
        this.target = target;
        this.targetID = target == null ? -1 : target.getId();
    }

    /** The spawn payload's target id, pinned for probes. */
    public int getTargetID() {
        return this.targetID;
    }

    /** {@code :84-105} — {@code func_70071_h_()}. */
    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > (this.red ? LIFETIME_RED : LIFETIME)) {                    // :85-86
            this.discard();
        }
        homeOntoTarget();                                                               // :88-105
    }

    /**
     * {@code :88-105} — the homing step: the unit vector to the target's
     * {@code (x, BB.minY + height*0.6, z)} times 0.2 added to the velocity, then
     * each component clamped to ±0.25.
     *
     * <p>The {@code d == 0} guard is the platform guard named in the class
     * javadoc. The target is also dropped when it dies — 1.7.10 keeps a dead
     * target and keeps homing on its corpse, which in 1.21.1 means homing on an
     * entity that is leaving the level; the removal is the target lifecycle, not
     * the orb's, and the original's {@code func_70071_h_} reads a target that is
     * still in the world.</p>
     */
    private void homeOntoTarget() {
        if (this.target == null) {
            return;
        }
        final double d = this.distanceTo(this.target);                                  // :90
        if (d == 0.0 || Double.isNaN(d)) {
            return;
        }
        final double dx = (this.target.getX() - this.getX()) / d;                       // :91/:97
        final double dy = (this.target.getBoundingBox().minY
                + this.target.getBbHeight() * HOMING_HEIGHT_FACTOR
                - this.getY()) / d;                                                     // :92/:98
        final double dz = (this.target.getZ() - this.getZ()) / d;                       // :93/:99
        final Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(                                                          // :100-104
                Mth.clamp((float) (motion.x() + dx * HOMING), (float) -HOMING_CLAMP, (float) HOMING_CLAMP),
                Mth.clamp((float) (motion.y() + dy * HOMING), (float) -HOMING_CLAMP, (float) HOMING_CLAMP),
                Mth.clamp((float) (motion.z() + dz * HOMING), (float) -HOMING_CLAMP, (float) HOMING_CLAMP));
    }

    /**
     * {@code :49-64} — {@code func_70184_a(MovingObjectPosition)}: server-side,
     * with an owner, on an entity hit, damage the struck entity with the owner's
     * attack damage × {@code red ? 1.0F : 0.6F} from an indirect-magic source;
     * then the impact send (the pinned sound and burst) and the orb is gone. A
     * block hit damages nothing and ends the orb the same way.
     *
     * <p>1.21.1's {@code Projectile#onHit} routes an entity hit through
     * {@code onHitEntity}, whose generic 4.0 damage (gated on
     * {@code doMobGriefing}) is not the original's figure, so the whole hook is
     * overridden and {@code onHitEntity} is not chained: the orb's damage is
     * exactly the owner's attack damage times the flag's factor.</p>
     */
    @Override
    protected void onHit(final HitResult hit) {
        if (!this.level().isClientSide && this.getOwner() != null
                && hit.getType() == HitResult.Type.ENTITY) {                            // :51
            final Entity struck = ((EntityHitResult) hit).getEntity();
            final DamageSource source = this.level().damageSources()
                    .indirectMagic(this, this.getOwner());                              // :53
            struck.hurt(source, orbDamage());                                           // :52-56
        }
        // :58-62 — the impact playSound (SOUND_IMPACT, SOUND_VOLUME, pitch
        // 1.0F + (r - r) * 0.2F) and Thaumcraft.proxy.burst (BURST_SCALE):
        // unregistered sends, pinned and not fired (see the class javadoc).
        this.discard();                                                                 // :63
    }

    /** {@code :52-56} — the owner's attack-damage attribute, times the flag. */
    public float orbDamage() {
        final Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity living)) {
            return 0.0F;
        }
        return (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (this.red ? DAMAGE_RED : DAMAGE_PLAIN);
    }

    /**
     * {@code :113-131} — {@code func_70097_a(DamageSource, float)}: an
     * invulnerable orb takes nothing; any hit zeroes the velocity, and when the
     * source has an owning entity the orb inherits 0.9 of that entity's look
     * vector (with the {@code thaumcraft:zap} send, pinned and not fired) and the
     * hit counts. With no owning entity the hit does not count.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (this.isInvulnerableTo(source)) {                                            // :114
            return false;
        }
        this.setDeltaMovement(Vec3.ZERO);                                               // :116
        final Entity attacker = source.getEntity();                                     // :117
        if (attacker == null) {
            return false;                                                               // :130
        }
        final Vec3 look = attacker.getLookAngle();                                      // :119
        if (look != null) {                                                             // :120
            this.setDeltaMovement(look.scale(PARRY_LOOK_FACTOR));                       // :121-126
            // :127 — playSound("thaumcraft:zap", 1.0F, 1.0F + (r - r) * 0.2F):
            // the audio lane's event, pinned as SOUND_PARRY, not fired.
        }
        return true;                                                                    // :129
    }

    /**
     * {@code :35-40} — the spawn payload: the target id ({@code -1} when absent)
     * then the red flag, in that order. 1.7.10's
     * {@code IEntityAdditionalSpawnData} is NeoForge's
     * {@code IEntityWithComplexSpawn}; the payload's fields and order are pinned
     * for card c5, whose Leader fires this same orb.
     */
    @Override
    public void writeSpawnData(final RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.target == null ? -1 : this.target.getId());
        buffer.writeBoolean(this.red);
    }

    /** {@code :42-47} — the payload back: the id resolves through the level
     *  ({@code world.getEntity(id)}; the original wraps the lookup in a
     *  try/catch because 1.7.10's throws on a missing id — 1.21.1's answers
     *  null), then the flag. */
    @Override
    public void readSpawnData(final RegistryFriendlyByteBuf buffer) {
        final int id = buffer.readInt();
        this.targetID = id;
        if (id >= 0) {
            final Entity entity = this.level().getEntity(id);
            if (entity instanceof LivingEntity living) {
                this.target = living;
            }
        }
        this.red = buffer.readBoolean();
    }
}
