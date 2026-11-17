package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Level-free rules for the taint swarm (TC-M6-06-s-e).
 *
 * <p>Every constant and rule below is read straight off the decompiled 1.7.10
 * {@code thaumcraft.common.entities.monster.EntityTaintSwarm}; the {@code :NNN}
 * citation in each javadoc line is that file's line number in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintSwarm.java}
 * (262 lines, {@code extends EntityMob implements ITaintedMob}, line 26). The
 * entity class walks state; the arithmetic and gates live here so the JUnit
 * tests pin them without a {@code Level}.</p>
 *
 * <p>Preserved quirks, never modernised:</p>
 * <ul>
 *   <li>the two target-seeking horizontal lerps are <b>different literals</b>:
 *       x steers with {@code 0.025000000149011613} ({@code :162}) and z with
 *       {@code 0.02500000001490116} ({@code :164});</li>
 *   <li>the flight-target pick casts the position with an {@code (int)} cast
 *       ({@code :142-144}), i.e. truncation toward zero, not floor;</li>
 *   <li>the client FX list prunes at most ONE entry per tick — the scan
 *       {@code break}s after the first null/dead entry ({@code :106-110});</li>
 *   <li>the melee reach is a bare {@code 3.0F} and the swing cooldown is
 *       {@code 10 + rand(5)} ({@code :200}, {@code :207});</li>
 *   <li>the swing saves and restores the target's motion around the hit, which
 *       undoes the vanilla knockback ({@code :208-218});</li>
 *   <li>{@code damBonus} is read from NBT as a <b>byte</b> and written back as
 *       {@code (byte)} ({@code :231}, {@code :237}) — a bonus over 127 wraps;</li>
 *   <li>the drop offset is the 1.7.10 {@code EntityLivingBase.height} field
 *       halved ({@code :259}), not the bounding-box height;</li>
 *   <li>the swarm declares an ambient sample of {@code ""} — it never loops a
 *       sound — while its hurt ({@code :62-64}) and death ({@code :66-68}) samples
 *       are both {@code thaumcraft:swarmattack} ({@code :58-68});</li>
 * </ul>
 */
public final class TaintSwarmAiLogic {

    // ------------------------------------------------------------- body/attrs

    /** {@code :33} — {@code setSize(2.0F, 2.0F)} in the ctor. */
    public static final float WIDTH = 2.0F;
    /** {@code :33}. */
    public static final float HEIGHT = 2.0F;
    /** {@code :50-52} — {@code getEyeHeight(float) = 1.0F}. */
    public static final float EYE_HEIGHT = 1.0F;
    /** {@code :76} — MAX_HEALTH. */
    public static final double HEALTH = 30.0;
    /** {@code :77} — ATTACK_DAMAGE is {@code 2 + damBonus}. */
    public static final double ATTACK_BASE = 2.0;
    /** {@code :28} — {@code public int damBonus = 0}. */
    public static final int DAM_BONUS_START = 0;
    /** {@code :54-56} — {@code getSoundVolume() = 0.1F}. */
    public static final float SOUND_VOLUME = 0.1F;

    /**
     * {@code :41-44} — the client-only {@code getBrightness(float)} override
     * returns {@code 15728880}, i.e. {@code 0xF000F0}: full sky <em>and</em>
     * full block light in the packed lightmap, so the swarm always renders
     * fully lit. 1.21.1 has no entity-side lightmap override, so the value is
     * pinned for the render layer and the probe (see the card's
     * left_out_of_scope list).
     */
    public static final int FULL_BRIGHTNESS_LIGHTMAP = 15728880;

    // ----------------------------------------------------------- synched flags

    /** {@code :36-39} — the flag byte lives at synched-data index 16. */
    public static final int DATA_FLAGS_INDEX = 16;
    /** {@code :81}/{@code :87} — the summoned bit is value 2 inside that byte. */
    public static final int SUMMONED_BIT = 2;
    /** {@code :89} — clearing the flag writes {@code flags & -4} (bits 0 AND 1). */
    public static final int FLAG_CLEAR_MASK = -4;

    // ---------------------------------------------------------------- targets

    /** {@code :224} — {@code double var1 = 12.0}, the target search range. */
    public static final double TARGET_RANGE = 12.0;
    /** {@code :200} — the melee reach is a bare {@code par2 < 3.0F}. */
    public static final float MELEE_REACH = 3.0F;
    /** {@code :207} — swing cooldown base, ticks. */
    public static final int ATTACK_COOLDOWN_BASE = 10;
    /** {@code :207} — plus {@code rand.nextInt(5)}. */
    public static final int ATTACK_COOLDOWN_JITTER = 5;
    /** {@code :204} — {@code EntityUtils.setRecentlyHit(target, 100)}. */
    public static final int RECENTLY_HIT_TICKS = 100;
    /** {@code :212} — {@code Potion.weakness} for 100 ticks. */
    public static final int WEAKNESS_TICKS = 100;
    /** {@code :212} — amplifier 0. */
    public static final int WEAKNESS_AMPLIFIER = 0;
    /** {@code :219} — the attack sample plays at volume 0.3. */
    public static final float ATTACK_SOUND_VOLUME = 0.3F;
    /** {@code :219} — pitch {@code 0.9F + world.rand.nextFloat() * 0.2F}. */
    public static final float ATTACK_SOUND_PITCH_BASE = 0.9F;
    /** {@code :219}. */
    public static final float ATTACK_SOUND_PITCH_SPREAD = 0.2F;

    // ------------------------------------------------------------- movement

    /** {@code :103} — {@code motionY *= 0.6F} after {@code super.onUpdate()}. */
    public static final double Y_DRAG = 0.6F;
    /** {@code :122} — a summoned swarm with no target takes 5.0F of generic damage. */
    public static final float SUMMON_SELF_DAMAGE = 5.0F;

    /** {@code :129} — a flight target below y 1 is dropped. */
    public static final int FLIGHT_MIN_Y = 1;
    /** {@code :130-131} — a flight target above {@code world.getHeight(x,z) + 8} is dropped. */
    public static final int FLIGHT_HEIGHT_BONUS = 8;
    /** {@code :139} — re-pick cadence: {@code rand.nextInt(30) == 0}. */
    public static final int FLIGHT_REPICK_ROLL = 30;
    /** {@code :140} — arrival radius, squared ({@code < 4.0F}, i.e. 2 blocks). */
    public static final float FLIGHT_ARRIVAL_DIST_SQR = 4.0F;
    /** {@code :142}/{@code :144} — x/z jitter rolls are {@code nextInt(7) - nextInt(7)}. */
    public static final int FLIGHT_ROLL_XZ = 7;
    /** {@code :143} — the y roll is {@code nextInt(6) - 2}. */
    public static final int FLIGHT_ROLL_Y = 6;
    /** {@code :143} — minus this, so the target sits 2..3 blocks under the swarm. */
    public static final int FLIGHT_Y_DROP = 2;

    /** {@code :151}/{@code :162}/{@code :164} — the horizontal steer speed, blocks/tick. */
    public static final double STEER_XZ_SPEED = 0.5;
    /** {@code :152}/{@code :163} — the vertical steer speed, blocks/tick (a float literal). */
    public static final float STEER_Y_SPEED = 0.7F;
    /** {@code :151}/{@code :153} — idle (no-target) horizontal lerp. */
    public static final double STEER_IDLE_XZ_LERP = 0.015000000014901161;
    /** {@code :152}/{@code :163} — the vertical lerp is the same in both branches. */
    public static final float STEER_Y_LERP = 0.1F;
    /** {@code :162} — with a target, x lerps at this (note the trailing digits). */
    public static final double STEER_TARGET_X_LERP = 0.025000000149011613;
    /** {@code :164} — with a target, z lerps at THIS, which is NOT the x literal. */
    public static final double STEER_TARGET_Z_LERP = 0.02500000001490116;
    /** {@code :156}/{@code :167} — {@code field_70701_bs = 0.1F} (moveForward). */
    public static final float MOVE_FORWARD_INPUT = 0.1F;
    /** {@code :154}/{@code :165} — the yaw is the motion vector's bearing minus 90°. */
    public static final double YAW_BEARING_OFFSET = 90.0;

    // ------------------------------------------------------------------ spawn

    /** {@code :245} — {@code byte var5 = 7}, the light roll bound. */
    public static final int SPAWN_LIGHT_ROLL_BOUND = 7;

    // ------------------------------------------------------------- client FX

    /** {@code :112} — {@code Thaumcraft.proxy.particleCount(25)}'s argument. */
    public static final int FX_BUDGET_REQUEST = 25;
    /** {@code :112} — the floor of that budget: {@code Math.max(count, 10)}. */
    public static final int FX_MIN_COUNT = 10;
    /** {@code :113} — {@code swarmParticleFX(world, this, 0.22F, 15.0F, 0.08F)}. */
    public static final float FX_PARTICLE_SCALE = 0.22F;
    /** {@code :113}. */
    public static final float FX_VIEW_RANGE = 15.0F;
    /** {@code :113}. */
    public static final float FX_SPEED = 0.08F;

    // ------------------------------------------------------------------ loot

    /** {@code :253-255} — {@code getDropItem() = ConfigItems.itemResource}. */
    public static final String DROP_ITEM = "itemResource";
    /** {@code :259} — itemResource meta 11: Tainted Goo. */
    public static final int META_TAINTED_GOO = 11;
    /**
     * {@code :259} — the drop offset is {@code this.field_70131_O / 2.0F}; MCP names
     * that field {@code height} ("how high this entity is considered to be"), and
     * 1.7.10's {@code EntityLivingBase} initialises it to 0.1F. The swarm never
     * writes it, so the offset is 0.05 — NOT the bounding-box height (2.0/2 = 1.0).
     */
    public static final float DROP_OFFSET_HEIGHT_FIELD = 0.1F;

    // ------------------------------------------------------------------- nbt

    /** {@code :230}/{@code :236} — the flag byte round-trips under {@code "Flags"}. */
    public static final String NBT_FLAGS = "Flags";
    /** {@code :231}/{@code :237} — the damage bonus round-trips as a byte. */
    public static final String NBT_DAM_BONUS = "damBonus";

    /** 1.7.10's registration ({@code ConfigEntities.java:137}): 64 blocks, update 3. */
    public static final int TRACKING_RANGE_BLOCKS = 64;
    /** 64 blocks is 4 chunks, which is what 1.21.1's builder takes. */
    public static final int TRACKING_RANGE_CHUNKS = TRACKING_RANGE_BLOCKS / 16;

    private TaintSwarmAiLogic() {
    }

    // ------------------------------------------------------------- attributes

    /** {@code :77} — {@code 2 + this.damBonus}. */
    public static double attackDamage(final int damBonus) {
        return ATTACK_BASE + damBonus;
    }

    /**
     * The drop offset the original hands to {@code entityDropItem}:
     * {@code field_70131_O / 2.0F} with the untouched 1.7.10 default 0.1F ({@code :259}).
     */
    public static float dropOffset() {
        return DROP_OFFSET_HEIGHT_FIELD / 2.0F;
    }

    /** {@code :258} — the drop fires on a straight coin flip, looting level ignored. */
    public static boolean dropsGoo(final boolean coinFlip) {
        return coinFlip;
    }

    // ------------------------------------------------------------------ flags

    /** {@code :80-82} — {@code (data16 & 2) != 0}. */
    public static boolean isSummoned(final byte flags) {
        return (flags & SUMMONED_BIT) != 0;
    }

    /**
     * {@code :84-91} — set writes {@code flags | 2}; clear writes
     * {@code flags & -4}, which also wipes bit 0. Verbatim.
     */
    public static byte flagsWithSummoned(final byte flags, final boolean summoned) {
        return summoned ? (byte) (flags | SUMMONED_BIT) : (byte) (flags & FLAG_CLEAR_MASK);
    }

    /**
     * {@code :121-123} — the generic 5.0F self-hit runs only while the swarm is
     * summoned AND has no target, i.e. inside the {@code entityToAttack == null}
     * branch.
     */
    public static boolean summonSelfDamageApplies(final boolean summoned, final boolean hasTarget) {
        return summoned && !hasTarget;
    }

    /** {@code :194-196} — {@code isInvulnerable()} swallows every hit. */
    public static boolean hurtBlocked(final boolean invulnerable) {
        return invulnerable;
    }

    // --------------------------------------------------------------- targeting

    /** {@code :223-226} — the target provider searches only for an unsummoned swarm. */
    public static boolean providerSearches(final boolean summoned) {
        return !summoned;
    }

    /** {@code :171-173} — a creative player is dropped as a target. */
    public static boolean clearsTarget(final boolean targetIsCreativePlayer) {
        return targetIsCreativePlayer;
    }

    // ---------------------------------------------------------------- flight

    /**
     * {@code :125-134} — the flight target is abandoned when the target block is
     * not air, sits below y 1, sits above {@code world.getHeight(x,z) + 8}, or
     * its column is not the taint biome (1.7.10 compares
     * {@code world.getBiomeGenForCoords(x,z).biomeID != Config.biomeTaintID}).
     */
    public static boolean flightTargetIsStale(final boolean targetBlockIsAir, final int targetY,
            final int heightLimit, final boolean inTaintBiome) {
        return !targetBlockIsAir
                || targetY < FLIGHT_MIN_Y
                || targetY > heightLimit
                || !inTaintBiome;
    }

    /** {@code :130-131} — {@code world.getHeight(x, z) + 8}. */
    public static int flightHeightLimit(final int heightmapY) {
        return heightmapY + FLIGHT_HEIGHT_BONUS;
    }

    /**
     * {@code :138-140} — the re-pick test. The caller evaluates the first
     * disjunct ({@code currentFlightTarget == null}) itself: Java's
     * {@code ||} short-circuits, so the original does NOT consume a
     * {@code rand.nextInt(30)} on a tick that has no target. That ordering is
     * part of the RNG stream and is preserved by keeping the roll in the caller.
     */
    public static boolean wantsNewFlightTarget(final int repickRoll, final double distanceSquared) {
        return repickRoll == 0 || distanceSquared < FLIGHT_ARRIVAL_DIST_SQR;
    }

    /**
     * {@code :142-144} — the new target. The rolls arrive in source order:
     * x {@code nextInt(7)}, x {@code nextInt(7)}, y {@code nextInt(6)},
     * z {@code nextInt(7)}, z {@code nextInt(7)}.
     */
    public static BlockPos pickFlightTarget(final BlockPos self, final int xRollA, final int xRollB,
            final int yRoll, final int zRollA, final int zRollB) {
        return new BlockPos(self.getX() + xRollA - xRollB,
                self.getY() + yRoll - FLIGHT_Y_DROP,
                self.getZ() + zRollA - zRollB);
    }

    /**
     * {@code :142} — the original casts the entity position with {@code (int)},
     * i.e. truncation toward zero: −3.5 becomes −3, not −4. Preserved.
     */
    public static int truncate(final double coordinate) {
        return (int) coordinate;
    }

    /**
     * {@code :140} — {@code ChunkCoordinates.func_71569_e(x, y, z)}: the plain
     * integer-component distance, with no {@code +0.5} centre offset.
     */
    public static double flightTargetDistanceSquared(final BlockPos target, final int selfX,
            final int selfY, final int selfZ) {
        final double dx = target.getX() - selfX;
        final double dy = target.getY() - selfY;
        final double dz = target.getZ() - selfZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** {@code :148} — idle x delta: {@code target.x + 0.5 - posX}. */
    public static double idleDeltaX(final int targetX, final double selfX) {
        return targetX + 0.5 - selfX;
    }

    /** {@code :149} — idle y delta: {@code target.y + 0.1 - posY} (0.1, not 0.5). */
    public static double idleDeltaY(final int targetY, final double selfY) {
        return targetY + 0.1 - selfY;
    }

    /** {@code :150} — idle z delta: {@code target.z + 0.5 - posZ}. */
    public static double idleDeltaZ(final int targetZ, final double selfZ) {
        return targetZ + 0.5 - selfZ;
    }

    /** {@code :159} — with a target, the x delta is a bare position difference. */
    public static double targetDeltaX(final double targetX, final double selfX) {
        return targetX - selfX;
    }

    /** {@code :160} — the y delta aims at {@code target.posY + target.getEyeHeight()}. */
    public static double targetDeltaY(final double targetY, final double targetEyeHeight,
            final double selfY) {
        return targetY + targetEyeHeight - selfY;
    }

    /** {@code :161} — with a target, the z delta is a bare position difference. */
    public static double targetDeltaZ(final double targetZ, final double selfZ) {
        return targetZ - selfZ;
    }

    /**
     * {@code :151}/{@code :162}/{@code :164} —
     * {@code motion += (signum(delta) * 0.5 - motion) * lerp}. Pass the branch's
     * own lerp: {@link #STEER_IDLE_XZ_LERP}, {@link #STEER_TARGET_X_LERP} or
     * {@link #STEER_TARGET_Z_LERP}.
     */
    public static double steerHorizontal(final double currentMotion, final double delta,
            final double lerp) {
        return currentMotion + (Math.signum(delta) * STEER_XZ_SPEED - currentMotion) * lerp;
    }

    /** {@code :152}/{@code :163} — {@code motion += (signum(delta) * 0.7 - motion) * 0.1}. */
    public static double steerVertical(final double currentMotion, final double delta) {
        return currentMotion + (Math.signum(delta) * STEER_Y_SPEED - currentMotion) * STEER_Y_LERP;
    }

    /** {@code :103} — the swarm's own y drag, applied after {@code super.onUpdate()}. */
    public static double dragVertical(final double motionY) {
        return motionY * Y_DRAG;
    }

    /**
     * {@code :154-157}/{@code :165-168} — the yaw step. The bearing comes from the
     * motion the steer calls just wrote, so the caller passes the NEW
     * motionX/motionZ: {@code (float)(atan2(motionZ, motionX) * 180.0 / PI) - 90.0F}
     * — the bearing is rounded to FLOAT before the −90 (the original's
     * {@code float var7}), then {@code var8 = wrap(var7 - rotationYaw)} is added to
     * the yaw.
     */
    public static float yawStep(final double motionX, final double motionZ, final float currentYaw) {
        final float bearing = (float) (Math.atan2(motionZ, motionX) * 180.0 / Math.PI) - 90.0F;
        return Mth.wrapDegrees(bearing - currentYaw);
    }

    // ----------------------------------------------------------------- melee

    /**
     * {@code :199-202} — {@code attackTime <= 0 && distance < 3.0F} and the
     * target's box overlaps this box vertically
     * ({@code target.maxY > self.minY && target.minY < self.maxY}).
     */
    public static boolean canMelee(final int attackTime, final float distance,
            final boolean verticalOverlap) {
        return attackTime <= 0
                && distance < MELEE_REACH
                && verticalOverlap;
    }

    /** {@code :207} — {@code 10 + rand.nextInt(5)} → 10..14. */
    public static int meleeCooldown(final RandomSource random) {
        return ATTACK_COOLDOWN_BASE + random.nextInt(ATTACK_COOLDOWN_JITTER);
    }

    /** {@code :219} — {@code 0.9F + world.rand.nextFloat() * 0.2F} (the WORLD's rng). */
    public static float attackSoundPitch(final float worldRandom) {
        return ATTACK_SOUND_PITCH_BASE + worldRandom * ATTACK_SOUND_PITCH_SPREAD;
    }

    // ------------------------------------------------------------------ spawn

    /**
     * {@code :245-246} — {@code light > rand.nextInt(7) ? false : super}. The
     * roll is 0..6, so any light above 6 fails on the smallest roll and light 0
     * always passes.
     */
    public static boolean lightAllowsSpawn(final int light, final int lightRoll) {
        return !(light > lightRoll);
    }

    // -------------------------------------------------------------- client FX

    /**
     * {@code :106-110} — the FX list scan removes ONE entry per tick: it
     * {@code break}s at the first null-or-dead entry, so a list with several dead
     * entries takes one tick per entry.
     *
     * @param nullOrDead one slot per tracked FX entity; true means null or dead
     * @return the index to remove, or -1 when the list is clean
     */
    public static int firstRemovableFxIndex(final boolean[] nullOrDead) {
        for (int a = 0; a < nullOrDead.length; a++) {
            if (nullOrDead[a]) {
                return a;
            }
        }
        return -1;
    }

    /** {@code :112} — the FX budget: {@code Math.max(particleCount(25), 10)}. */
    public static int fxCountTarget(final int particleCount) {
        return Math.max(particleCount, FX_MIN_COUNT);
    }

    /** {@code :112} — a new FX spawns whenever the list is under that budget. */
    public static boolean spawnsFx(final int swarmListSize, final int particleCount) {
        return swarmListSize < fxCountTarget(particleCount);
    }

    // -------------------------------------------------------------------- nbt

    /**
     * {@code :231} — {@code damBonus = tag.getByte("damBonus")}: the byte is
     * widened to int, so a stored 200 reads back as −56.
     */
    public static int damBonusFromNbt(final byte stored) {
        return stored;
    }

    /** {@code :237} — {@code tag.setByte("damBonus", (byte) damBonus)}. */
    public static byte damBonusToNbt(final int damBonus) {
        return (byte) damBonus;
    }
}
