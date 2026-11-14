package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import javax.annotation.Nullable;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code thaumcraft:eldritchorb} — original
 * {@code thaumcraft.common.entities.projectile.EntityEldritchOrb} (92 lines,
 * content-mapping {@code Thaumcraft:EldritchOrb}, kind=entity, mod_index 11,
 * display name "Eldritch Orb", CSV row 275 at this card's base),
 * {@code extends EntityThrowable}. Registration
 * {@code thaumcraft/common/config/ConfigEntities.java:87} — {@code
 * registerModEntity(EntityEldritchOrb.class, "EldritchOrb", id++, instance, 64,
 * 20, true)}: tracking 64 blocks, update interval 20, velocity updates on —
 * the same projectile triple as the Energy Orb ({@code :89}) and the Pech blast
 * ({@code :86}).
 *
 * <p>TC-M6-06-s-c4b-1 (card t_23520cdc) registers and drives this type: it is
 * the Eldritch Crab's projectile (the crab's swarm call site is
 * {@code EntityEldritchCrab} in the same family, and the Outer Lands' crab
 * spawner is the shipped producer). Every figure below is from the decompiled
 * source:</p>
 * <ul>
 *   <li><b>no gravity</b> — {@code func_70185_h() = 0.0F} ({@code :24-26}) →
 *       {@code getDefaultGravity() = 0.0};</li>
 *   <li><b>a 100-tick lifetime</b> — {@code func_70071_h_} discards on
 *       {@code ticksExisted > 100} ({@code :28-33}), and an impact writes
 *       {@code ticksExisted = 100} ({@code :84}) so the orb dies on the tick
 *       after it bursts;</li>
 *   <li><b>a burst on ANY impact</b> — {@code func_70184_a} ({@code :63-87})
 *       checks neither hit type nor a struck entity: server-side with a thrower
 *       ({@code :64}), it sweeps the entities inside <b>the orb's own box
 *       inflated by 2.0</b> ({@code :65}, {@code world.getEntitiesWithinAABB(
 *       thrower, box)} — the swept set EXCLUDES THE THROWER, so the crab that
 *       threw the orb never takes its own burst) and hits every living,
 *       non-undead one ({@code :69}) for the thrower's attack-damage attribute
 *       × {@code 0.666F} from the thrown-damage source
 *       {@code DamageSource.func_76354_b(this, thrower)} ({@code :70-74}), then
 *       applies {@code Potion.field_76437_t} = {@code weakness} for 160 ticks at
 *       amplifier 0 ({@code :76-79}, mapping: {@code
 *       reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:3659}
 *       {@code field_76437_t,weakness});</li>
 *   <li><b>the fizz and the burst packet</b> — {@code random.fizz} at volume
 *       0.5F and pitch {@code 2.6F + (r - r) * 0.8F} ({@code :83}) — a
 *       symmetric pitch spread, not a one-sided one — played AFTER the sweep,
 *       at the impact site, then {@code world.func_72960_a(this, (byte)16)}
 *       ({@code :85}); the client half of that event ({@code :35-61}) is 30
 *       wispFX3 sprays, pinned here and sent by the FX lane (TC-M7-01), never
 *       by this class. The original has NO launch sound.</li>
 *   <li><b>a 0.1F crosshair reach</b> — {@code func_70053_R() = 0.1F}
 *       ({@code :89-91}) → {@code getPickRadius()}.</li>
 * </ul>
 *
 * <p><b>21.1.259's damage-source carrier</b> is
 * {@code DamageSources#thrown(Entity, Entity)} (javap on the compiled
 * NeoForge jar: {@code public DamageSource thrown(Entity, Entity)}), the
 * platform's thrown-damage factory — NOT {@code indirectMagic}, which is a
 * different damage type with its own death messages and its own
 * {@code isMagic} answer. 1.7.10's MCP <em>name</em> for
 * {@code func_76354_b} is not verifiable in this tree (the shipped mapping CSV
 * carries fields only: {@code ls reference/mcp-mappings} = {@code
 * fields_1.7.10_mcp_stable_11.csv}), so the seam cites the call site and the
 * platform method it maps to, and the pair is measured on the pad
 * measured inside {@code EldritchCrabGameTest
 * .theOrbBurstStrikesTheLivingAndSparesTheUndead}, which reads the struck
 * target's {@code getLastDamageSource()} and asserts the direct entity is the
 * orb, the owning entity is the thrower, and the type is a projectile's and not
 * the magic type).</p>
 *
 * <p><b>The {@code try/catch (Exception)} of {@code :76-79} is kept as the same
 * guard</b> around the effect send — the original swallows a failed
 * {@code addPotion}, so a target that rejects the effect still took the damage.
 * 1.21.1's {@code addEffect} answers false instead of throwing, so the catch is
 * a formality; it ships because the source shape is the contract.</p>
 *
 * <p><b>What this type does not do:</b> no homing (the Energy Orb's
 * homing step is {@code EntityGolemOrb:88-105}, not here), no damage cap, no
 * pierce and no block-break — {@code func_70184_a} is the whole impact story,
 * and the original's {@code onHit} chain never runs (this class does not call
 * {@code super.onHit}), so 1.21.1's generic {@code onHitEntity} damage of 4.0
 * is NOT added on top.</p>
 */
public class EntityEldritchOrb extends ThrowableProjectile {

    /** The registered id, pinned so a probe can name it from the class. */
    public static final ResourceLocation TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "eldritchorb");

    /** {@code ConfigEntities.java:87} — {@code registerModEntity(..., 64, 20, true)}. */
    /** The original's 92 lines set no {@code isImmuneToFire}, and a 1.21.1
     *  projectile needs none; pinned with the {@code build} pass that calls no
     *  {@code EntityType.Builder#fireImmune()}. */
    public static final boolean FIRE_IMMUNE = false;

    public static final int MOD_INDEX = 11;
    // registerModEntity(..., 64, 20, true) — ConfigEntities.java:87. The
    // original's tracking range is 64 BLOCKS; 1.21.1's clientTrackingRange
    // counts CHUNKS, so the builder gets 4 (the family's rule,
    // BrainyZombieAiLogicTest:38 — a raw 64 would track a kilometre).
    public static final int TRACK_RANGE_BLOCKS = 64;
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 20;
    /** 1.7.10's {@code EntityThrowable} default box — the original sets none:
     *  grepped, the 92 lines call no {@code func_70105_a} (setSize), so the
     *  vanilla projectile box (0.25 × 0.25) is what the original has. Pinned by
     *  {@link #SIZE_FROM_VANILLA_CTOR} so a unit test can name the source. */
    public static final boolean SIZE_FROM_VANILLA_CTOR = true;
    public static final float WIDTH = 0.25F;
    public static final float HEIGHT = 0.25F;

    /** {@code :24-26} — {@code func_70185_h() = 0.0F}: the orb floats. */
    public static final double GRAVITY = 0.0;
    /** {@code :30-31} and {@code :84} — the lifetime, and the value an impact writes. */
    public static final int LIFETIME_TICKS = 100;
    /** {@code :65} — {@code func_72314_b(2.0, 2.0, 2.0)} on the ORB's own box. */
    public static final double AOE_INFLATE = 2.0;
    /** {@code :73} — the thrower's {@code field_111264_e} (attack damage) × this. */
    public static final float DAMAGE_MULTIPLIER = 0.666F;
    /** {@code :77} — {@code Potion.field_76437_t} = weakness, 160 ticks, amp 0. */
    public static final int WEAKNESS_TICKS = 160;
    public static final int WEAKNESS_AMPLIFIER = 0;
    /** {@code :83} — {@code random.fizz} = the platform's extinguish event,
     *  played at the impact site after the sweep (there is no launch sound). */
    public static final float SOUND_VOLUME = 0.5F;
    public static final float SOUND_PITCH_BASE = 2.6F;
    public static final float SOUND_PITCH_SPREAD = 0.8F;
    /** {@code :85} — the burst packet's event id ({@code (byte)16}), read by the
     *  client half at {@code :36}. */
    public static final byte BURST_EVENT = 16;
    /** {@code :38-56} — the client spray, pinned for the FX lane: 30 wispFX3,
     *  jitter {@code (r - r) * 0.3F}, offset × 8.0F, size 0.3F, type 5, fade 0.02F. */
    public static final int BURST_PARTICLES = 30;
    public static final float BURST_JITTER = 0.3F;
    public static final float BURST_OFFSET_SCALE = 8.0F;
    public static final float BURST_SIZE = 0.3F;
    public static final int BURST_TYPE = 5;
    public static final float BURST_FADE = 0.02F;
    /** {@code :89-91} — {@code func_70053_R() = 0.1F}. */
    public static final float PICK_RADIUS = 0.1F;

    /**
     * {@code :68-71} — the burst's damage: the thrower's attack-damage attribute
     * times {@link #DAMAGE_MULTIPLIER}. Split out (and world-free) so a unit
     * probe can pin the factor without a Level.
     */
    public static float burstDamage(final double throwerAttackDamage) {
        return (float) (throwerAttackDamage * DAMAGE_MULTIPLIER);
    }

    /**
     * {@code :69} — who the burst strikes: an {@code EntityLivingBase} that is
     * NOT {@code func_70662_br()} (undead). Undead are exempt; players, animals
     * and the crab's own kind are not — the original filters on the living flag
     * only, and a crab standing in the burst of ANOTHER crab's orb takes it (the
     * thrower of the orb in question is out of the swept set, {@code :65}).
     * Kept as a pure predicate over the two flags the platform exposes.
     */
    public static boolean burstStrikes(final boolean living, final boolean undead) {
        return living && !undead;
    }

    // The (type, level) form takes the EXACT EntityType<EntityEldritchOrb>:
    // EntityType.Builder.of(factory, category) infers its type variable from the
    // ctor reference, and a `? extends EntityEldritchOrb` parameter makes javac
    // infer T = Entity and reject the builder.
    public EntityEldritchOrb(final EntityType<EntityEldritchOrb> type, final Level level) {
        super(type, level);
    }

    /**
     * {@code :17-19} — {@code EntityEldritchOrb(World, EntityLivingBase)}: 1.7.10's
     * thrower constructor is 1.21.1's {@code
     * ThrowableProjectile(EntityType, LivingEntity, Level)}; it records the owner
     * (the original's {@code func_85052_h()}) and places the orb at the owner's
     * {@code eyeY - 0.1}.
     */
    public EntityEldritchOrb(final EntityType<EntityEldritchOrb> type, final Level level,
            final LivingEntity thrower) {
        super(type, thrower, level);
    }

    /**
     * The original's whole synched state is the {@code EntityThrowable} body
     * ({@code field_70173_aa} and the thrower), so the crab's orb defines no
     * watcher of its own — 1.21.1's {@code Entity} leaves {@code
     * defineSynchedData} abstract and the {@code Projectile} chain adds no
     * entry, so the empty builder is the port of "nothing" (the shipped
     * precedent is {@code EntityGolemOrb:150-153}).
     */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        // :14-19 — no watcher of its own.
    }

    /** {@code :24-26} — the orb floats: gravity 0.0. */
    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** Probe handle on the protected platform getter. */
    public double getDefaultGravityForProbe() {
        return this.getDefaultGravity();
    }

    /** {@code :89-91} — the crosshair reach. */
    @Override
    public float getPickRadius() {
        return PICK_RADIUS;
    }

    /** {@code :28-33} — {@code func_70071_h_()}: the 100-tick lifetime clamp. */
    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFETIME_TICKS) {                                          // :27-29
            this.discard();
        }
    }

    /**
     * {@code :35-61} — {@code func_70103_a(byte)}: event 16 is the burst packet
     * and its client half is the 30-spray wispFX3 cloud; every other event rides
     * the platform's own handling.
     *
     * <p>The particle sends are the FX lane (TC-M7-01): this port ships no
     * wispFX3 sender, so the client branch is pinned as the constants above
     * ({@link #BURST_PARTICLES}, {@link #BURST_JITTER}, {@link
     * #BURST_OFFSET_SCALE}, {@link #BURST_SIZE}, {@link #BURST_TYPE}, {@link
     * #BURST_FADE}) and the event's server-side half — which is what the burst
     * IS for gameplay — runs in full here.</p>
     */
    @Override
    public void handleEntityEvent(final byte id) {
        if (id == BURST_EVENT) {
            if (this.level().isClientSide) {
                // :38-56 — 30 × wispFX3(pos + jitter, pos + jitter*8.0F, 0.3F, 5,
                // true, 0.02F): pinned, sent by the FX lane, never from here.
                return;
            }
            return;
        }
        super.handleEntityEvent(id);                                                    // :58-60
    }

    /**
     * {@code :63-87} — {@code func_70184_a(MovingObjectPosition)}: the burst. The
     * hit type is never read, so a wall, a floor and a mob all trigger it, and
     * the swept box is the <b>orb's</b> box inflated by 2.0 ({@code :65}) — the
     * area around where the orb died, not around the thrower (the Pech blast's
     * owner-centred box is that lane's documented deviation, not the family's).
     * The swept set excludes the THROWER (the original's {@code
     * func_72839_b(thrower, box)} first argument), and the orb itself drops out
     * of the loop on the living test.
     */
    @Override
    protected void onHit(final HitResult hit) {
        if (this.level().isClientSide || this.getOwner() == null) {                     // :64
            this.discard();
            return;
        }
        final Entity thrower = this.getOwner();
        // :65 — func_72839_b(thrower, box): the FIRST argument is the entity the
        // sweep skips, so the thrower is out of its own burst (the orb itself is
        // not skipped; it drops out on the living test below).
        for (final Entity struck : this.level().getEntities(thrower,
                this.getBoundingBox().inflate(AOE_INFLATE, AOE_INFLATE, AOE_INFLATE))) {  // :65
            // :69 — `entity1 instanceof EntityLivingBase && !func_70662_br()`: the
            // LIVING test, not a Mob test — the original sweeps players, animals and
            // golems too, so the port rides LivingEntity (1.21.1's Player is not a
            // Mob) — and skips the undead (1.21.1's UNDEAD type tag).
            if (!(struck instanceof LivingEntity living)) {                            // :69
                continue;
            }
            if (!burstStrikes(true, living.getType().is(EntityTypeTags.UNDEAD))) { // :69
                continue;
            }
            // :70-74 — DamageSource.func_76354_b(this, thrower): 21.1.259's
            // thrown-damage factory is DamageSources#thrown (javap), not
            // indirectMagic.
            final DamageSource source = this.level().damageSources()
                    .thrown(this, thrower);                                             // :72
            living.hurt(source, burstDamage(thrower instanceof LivingEntity owner
                    ? owner.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0));  // :73
            try {                                                                       // :76-79
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        WEAKNESS_TICKS, WEAKNESS_AMPLIFIER));                           // :77
            } catch (final RuntimeException ignored) {
                // :78-79 — the original's silent catch (Exception).
            }
        }
        // :83 — playSound("random.fizz", 0.5F, 2.6F + (r - r) * 0.8F): a vanilla
        // event, so the send fires (the TC-registered sends are the audio lane's),
        // at the impact site, after the sweep, with the symmetric pitch spread.
        final float pitch = SOUND_PITCH_BASE
                + (this.random.nextFloat() - this.random.nextFloat()) * SOUND_PITCH_SPREAD;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL,
                SOUND_VOLUME, pitch);
        this.tickCount = LIFETIME_TICKS;                                                // :84
        this.level().broadcastEntityEvent(this, BURST_EVENT);                           // :85
    }

    /** {@code :65} — the swept box, split out so a probe can pin the inflation. */
    public net.minecraft.world.phys.AABB burstBox() {
        return this.getBoundingBox().inflate(AOE_INFLATE, AOE_INFLATE, AOE_INFLATE);
    }

    /** The thrower, typed for probes ({@code func_85052_h()}). */
    @Nullable
    public LivingEntity throwerForProbe() {
        final Entity owner = this.getOwner();
        return owner instanceof LivingEntity living ? living : null;
    }

    /** The launch vector, for the probe that pins "no gravity" over one tick. */
    public Vec3 velocityForProbe() {
        return this.getDeltaMovement();
    }
}
