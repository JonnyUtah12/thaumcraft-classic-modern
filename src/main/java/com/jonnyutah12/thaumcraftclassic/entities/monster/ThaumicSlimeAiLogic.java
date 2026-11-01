package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Level-free rules of {@code thaumcraft.common.entities.monster.EntityThaumicSlime}
 * (TC-M6-06-s-e). The original is an {@code EntityMob} (NOT 1.7.10
 * {@code EntitySlime}) that installs no goal selectors at all: its jump, its
 * spit, its merge and its split are hand-rolled in {@code func_70071_h_} /
 * {@code func_70626_be} / {@code func_70106_y}. Every constant and branch here
 * quotes the decompiled file and line it came from
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityThaumicSlime.java});
 * {@code ThaumicSlimeAiLogicTest} re-states the same (line, value) pairs, so a
 * drift is a red test.
 */
public final class ThaumicSlimeAiLogic {

    private ThaumicSlimeAiLogic() {
    }

    // ------------------------------------------------------------- construction

    /**
     * Ctor line 31: {@code int i = 1 << this.rand.nextInt(3)} — a freshly
     * spawned thaumic slime is size 1, 2 or 4, never 3.
     */
    public static int initialSize(final int nextIntOfThree) {
        return 1 << nextIntOfThree;
    }

    /** Ctor line 32: {@code this.yOffset = 0.0F} (the slime has no eye offset). */
    public static final float Y_OFFSET = 0.0F;

    /** Ctor line 33: {@code slimeJumpDelay = rand.nextInt(20) + 10}. */
    public static int initialJumpDelay(final int nextIntOfTwenty) {
        return nextIntOfTwenty + 10;
    }

    /** Ctor line 26: {@code int launched = 10} — the "freshly thrown" countdown. */
    public static final int LAUNCHED_START = 10;

    /** Ctor line 27: {@code int spitCounter = 100}. */
    public static final int SPIT_COUNTER_START = 100;

    /** Line 197: the spit counter resets to 101, not 100. */
    public static final int SPIT_COUNTER_RESET = 101;

    // ------------------------------------------------------------------- size

    /**
     * {@code setSlimeSize} lines 83-84: {@code float ss = sqrt(size)} and
     * {@code func_70105_a(0.25F * ss + 0.25F, 0.25F * ss + 0.25F)} — width and
     * height are the SAME expression.
     */
    public static float sizeToBox(final int size) {
        return 0.25F * (float) Math.sqrt(size) + 0.25F;
    }

    /** {@code setSlimeSize} line 86: the max-health attribute IS the size. */
    public static double sizeToMaxHealth(final int size) {
        return size;
    }

    /** {@code setSlimeSize} line 88: {@code experienceValue = (int) sqrt(size)}. */
    public static int sizeToExperience(final int size) {
        return (int) Math.sqrt(size);
    }

    /** {@code getSlimeSize} / {@code getAttackStrength} (lines 91-97): both are the raw size. */
    public static int attackStrength(final int size) {
        return size;
    }

    /** Save line 101: the NBT {@code "Size"} is written as {@code size - 1}. */
    public static int sizeToNbt(final int size) {
        return size - 1;
    }

    /** Load line 106: the NBT {@code "Size"} is read back as {@code nbt + 1}. */
    public static int nbtToSize(final int nbt) {
        return nbt + 1;
    }

    // ------------------------------------------------------------------ sound

    /**
     * Lines 113-115 / 288-294: the jump, ambient and hurt sounds are all
     * {@code "mob.slime." + (size > 3 ? "big" : "small")}.
     */
    public static String slimeSound(final int size) {
        return "mob.slime." + (size > 3 ? "big" : "small");
    }

    /** Line 306: {@code getSoundVolume} is {@code 0.1F * sqrt(size)}. */
    public static float soundVolume(final int size) {
        return 0.1F * (float) Math.sqrt(size);
    }

    /** Line 314: {@code makesSoundOnJump()} is {@code size > 3}. */
    public static boolean makesSoundOnJump(final int size) {
        return size > 3;
    }

    /** The size threshold inside {@code slimeSound}: {@code size > 3} → "big". */
    public static final int BIG_SOUND_SIZE = 3;

    /**
     * Line 204: the spit plays the original's {@code thaumcraft:gore} sample —
     * shipped by the port as {@code thaumcraft:tentacle}? No: the port's sound
     * registry carries {@code thaumcraft:gore} from TC-M6-06-s-b, so the id is
     * used as-is. The 1.7.10 sample name is kept as a string here so this class
     * stays free of Minecraft registry types (the JUnit tests run in a plain JVM).
     */
    public static final String SPIT_SOUND_ID = "thaumcraft:gore";

    /**
     * The jump impulse: 1.7.10 {@code EntityLiving.onLivingUpdate}'s
     * {@code isJumping} branch builds
     * {@code normalize(motionX, 0, motionZ) * 0.4 + (0, 0.42, 0)}. In 1.21.1
     * {@code JumpControl} only raises the flag, so the entity writes the
     * original vector itself (vanilla 1.21.1 {@code Slime.java:353} does the
     * same for its own jump).
     */
    public static final double JUMP_XZ_SCALE = 0.4;
    public static final double JUMP_Y_IMPULSE = 0.42;

    /** Line 318: {@code makesSoundOnLand()} is {@code size > 5}. */
    public static boolean makesSoundOnLand(final int size) {
        return size > 5;
    }

    // -------------------------------------------------------------- peaceful

    /**
     * Line 118: {@code !world.isRemote && world.getWorldInfo().getDifficulty()
     * == 0 && getSlimeSize() > 0} → {@code setEntityDead()} — the slime erases
     * itself on peaceful difficulty (difficulty id 0), server side only.
     */
    public static boolean selfDestructsOnPeaceful(final int size) {
        return size > 0;
    }

    // ---------------------------------------------------------------- squish

    /** Line 122: {@code squishFactor += (squishAmount - squishFactor) * 0.5F}. */
    public static final float SQUISH_LERP = 0.5F;

    /** Lines 122-123: {@code squishPrevFactor = squishFactor} (the render pair). */
    public static float squishStep(final float prev, final float amount) {
        return prev + (amount - prev) * SQUISH_LERP;
    }

    /** Line 149: landing sets the squish amount to {@code -0.5F}. */
    public static final float SQUISH_ON_LAND = -0.5F;

    /** Line 151: leaving the ground sets it to {@code 1.0F}. */
    public static final float SQUISH_ON_JUMP = 1.0F;

    /** Line 242: {@code func_70808_l()} decays the squish amount by 0.6. */
    public static final float SQUISH_DECAY = 0.6F;

    // ------------------------------------------------------- client-only FX

    /**
     * Lines 127-134 (client half, {@code world.isRemote}): while
     * {@code launched > 0} the client draws {@code i * (launched + 1)} slime
     * jump particles, {@code i = (int) sqrt(size)}. The game-test server is
     * never client-side, so the burst lives in this extracted function and the
     * probe asserts BOTH halves of the {@code world.field_72995_K} gate.
     */
    public static int launchedParticleCount(final int size, final int launched) {
        return (int) Math.sqrt(size) * (launched + 1);
    }

    /** Line 138: the landing burst draws {@code i * 8} particles. */
    public static int landingParticleCount(final int size) {
        return (int) Math.sqrt(size) * 8;
    }

    /**
     * Lines 155-158 (client only): every tick the client re-sizes the entity
     * box to {@code 0.6F * sqrt(size)} — the render pulse. Server side keeps
     * the {@code setSlimeSize} box, so this factor is client-only by design.
     */
    public static final float CLIENT_BOX_FACTOR = 0.6F;

    public static float clientBox(final int size) {
        return CLIENT_BOX_FACTOR * (float) Math.sqrt(size);
    }

    // ------------------------------------------------------------------ merge

    /** Line 168: the merge scan box is the entity point inflated 16 / 8 / 16. */
    public static final double MERGE_SCAN_XZ = 16.0;
    public static final double MERGE_SCAN_Y = 8.0;

    /**
     * Lines 173-176: a merge candidate must be a DIFFERENT entity, have existed
     * for more than 100 ticks, and be smaller than 100.
     */
    public static boolean isMergableCandidate(final int otherId, final int myId,
            final int otherTicksExisted, final int otherSize) {
        return otherId != myId && otherTicksExisted > 100 && otherSize < 100;
    }

    /**
     * The lines 173-181 quirk, kept verbatim: {@code distance} is reassigned
     * for EVERY scanned slime (line 180, outside the {@code if}), so the
     * "closest" test at line 176 compares against the distance of the
     * PREVIOUSLY scanned entity, not the best-so-far. A faithful port must
     * update the running distance unconditionally.
     */
    public static void mergeScanVisit(final ScanState state, final int id, final int myId,
            final int ticksExisted, final int size, final double distanceSq) {
        if (isMergableCandidate(id, myId, ticksExisted, size) && distanceSq < state.distance) {
            state.closestId = id;
        }
        state.distance = distanceSq;
    }

    /** The scan accumulator, so the quirk above is testable. */
    public static final class ScanState {
        public double distance = Double.MAX_VALUE;
        public int closestId = -1;

        public void reset(final double initial) {
            this.distance = initial;
            this.closestId = -1;
        }
    }

    /**
     * Line 211: the merge fires when the distance is below the sum of the two
     * widths; line 212 caps the merged slime at 100.
     */
    public static boolean mergesAt(final double distance, final float myWidth, final float theirWidth) {
        return distance < myWidth + theirWidth;
    }

    public static int mergedSize(final int mySize, final int theirSize) {
        return Math.min(100, mySize + theirSize);
    }

    // ------------------------------------------------------------------- spit

    /** Line 189: the player search range is 16.0 blocks. */
    public static final double TARGET_SEARCH_RANGE = 16.0;

    /** Line 195: {@code setEntityAttackTarget(player, 10.0F, 20.0F)}. */
    public static final float AGGRO_RANGE = 10.0F;
    public static final float DEAGGRO_RANGE = 20.0F;

    /**
     * Line 196: the spit fires when the player is FARTHER than 4.0 blocks, the
     * spit counter has run down, and this slime is bigger than 3.
     */
    public static boolean spits(final float distanceToPlayer, final int spitCounter, final int size) {
        return distanceToPlayer > 4.0F && spitCounter <= 0 && size > 3;
    }

    /** Line 191-193: the counter only decrements while a player is in range. */
    public static int tickSpitCounter(final int spitCounter) {
        return spitCounter > 0 ? spitCounter - 1 : spitCounter;
    }

    /**
     * The flying-slime constructor (lines 40-53): the launch point is the
     * midpoint of the source AABB, the aim point adds {@code height / 3} and a
     * {@code distance * 0.2F} lead, and the whole thing is skipped when the
     * horizontal distance is below {@code 1.0E-7}.
     */
    public static final double THROW_MIN_DISTANCE = 1.0E-7;
    public static final double THROW_LEAD_FACTOR = 0.2;
    public static final float THROW_AIM_HEIGHT_DIVISOR = 3.0F;

    /** Lines 52-53: {@code setThrowableHeading(dx, dy + lead, dz, 1.5F, 1.0F)}. */
    public static final float THROW_VELOCITY = 1.5F;
    public static final float THROW_INACCURACY = 1.0F;

    /**
     * {@code setThrowableHeading} lines 57-73, verbatim: normalise, add
     * {@code rand.nextGaussian() * 0.0075F * inaccuracy} per axis, scale by the
     * velocity, and set yaw/pitch from the resulting vector.
     */
    public static double throwAxis(final double axis, final double gaussian, final float inaccuracy,
            final float velocity) {
        return axis * velocity;
    }

    public static double throwJitter(final double gaussian, final float inaccuracy) {
        return gaussian * 0.0075F * inaccuracy;
    }

    public static final float THROW_GAUSSIAN_SCALE = 0.0075F;

    // ------------------------------------------------------------------- jump

    /** Line 246: {@code getJumpDelay() = rand.nextInt(16) + 8}. */
    public static int jumpDelay(final int nextIntOfSixteen) {
        return nextIntOfSixteen + 8;
    }

    /** Line 221: with a target in range the delay is divided by 3. */
    public static final int JUMP_DELAY_TARGET_DIVISOR = 3;

    /** Line 231: {@code moveStrafing = 1.0F - rand.nextFloat() * 2.0F}. */
    public static float jumpStrafe(final float nextFloat) {
        return 1.0F - nextFloat * 2.0F;
    }

    /** Line 232: {@code moveForward = 1.0 * sqrt(size)}. */
    public static float jumpForward(final int size) {
        return (float) (1.0 * Math.sqrt(size));
    }

    /** Lines 235-237: not jumping clears both inputs while on the ground. */
    public static boolean jumpInputsZeroed(final boolean onGround) {
        return onGround;
    }

    // --------------------------------------------------------------- splitting

    /** Line 254: the split count is {@code (int) sqrt(size)}. */
    public static int splitCount(final int size) {
        return (int) Math.sqrt(size);
    }

    /**
     * Line 255: the split only happens server side, when {@code i > 1}, and
     * when health is at or below 0.
     */
    public static boolean splitsOnDeath(final int size, final float health) {
        return splitCount(size) > 1 && health <= 0.0F;
    }

    /** Lines 257-258: the child offsets, verbatim (integer division on k / 2). */
    public static float splitOffsetX(final int k, final int i) {
        return (k % 2 - 0.5F) * i / 4.0F;
    }

    public static float splitOffsetZ(final int k, final int i) {
        return (k / 2 - 0.5F) * i / 4.0F;
    }

    /** Line 260: every child is size 1; line 261 spawns them 0.5 above. */
    public static final int CHILD_SIZE = 1;
    public static final double CHILD_SPAWN_Y_OFFSET = 0.5;

    // ------------------------------------------------------------------ drop

    /**
     * Lines 300-304: a slime smaller than 3 drops meta 11 (Tainted Goo) with
     * {@code rand.nextInt(3) == 0}.
     */
    public static boolean dropsTaintedGoo(final int size, final int nextIntOfThree) {
        return size < 3 && nextIntOfThree == 0;
    }

    /** Lines 296-298: the "item in hand" slot is the goo only below size 3. */
    public static boolean carriesGooItem(final int size) {
        return size < 3;
    }

    /** The {@code ConfigItems.itemResource} metadata the slime drops. */
    public static final int TAINTED_GOO_META = 11;

    // ------------------------------------------------------- player collision

    /** Lines 269-282: the reach index, including the launched-size-2 quirk. */
    public static int playerReachIndex(final int size, final int launched) {
        int i = (int) Math.max(1.0, Math.sqrt(size));
        if (launched > 0 && i == 2) {
            i = 3;
        }
        return i;
    }

    /** Line 277: the touch gate is {@code distanceSq < 0.8*i * 0.8*i}. */
    public static boolean canTouchPlayer(final int reachIndex, final double distanceSq) {
        return distanceSq < 0.8 * reachIndex * 0.8 * reachIndex;
    }

    /** Line 285: {@code canDamagePlayer()} is {@code size > 0}. */
    public static boolean canDamagePlayer(final int size) {
        return size > 0;
    }
}
