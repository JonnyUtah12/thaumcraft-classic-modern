package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Level-free rules of the taint creeper (TC-M6-06-s-b). Numbers pinned
 * from {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/
 * monster/EntityTaintCreeper.java} and {@code ai/combat/AICreeperSwell.java}.
 * The swell state machine matches vanilla 1.7.10's creeper; 1.21.1's
 * {@code Creeper} keeps the same shape (swell progress, fuse, explosion).
 * The entity here extends the taint animal base (Monster), NOT 1.21.1's
 * {@code Creeper} — the TC creeper extends {@code EntityMob} in 1.7.10,
 * so the port pins these rules on a plain mob.
 */
public final class TaintCreeperAiLogic {

    /**
     * The fuse cap: the TC creeper's own {@code fuseTime} field
     * (declaration line 36, init 30; loaded from the "Fuse" NBT short,
     * lines 93-95). The tick body caps the progress at it (lines 133-134:
     * {@code >= 30 -> = 30 -> explode()}).
     */
    public static final int FUSE_TICKS = 30;

    /**
     * The death-animation fuse hook ({@code func_70069_a}, lines 70-77):
     * during the death animation the fuse accelerates by {@code par1 *
     * 1.5F} truncated to int and is capped at {@code fuseTime - 5} (lines
     * 75-76) — 5 is the headroom that keeps a dying creeper below the
     * blast threshold.
     */
    public static final int DEATH_FUSE_HEADROOM = 5;
    public static final double HEALTH = 30.0D;
    public static final double ATTACK = 2.0D;
    public static final double MOVEMENT_SPEED = 0.25D;

    /**
     * {@code EntityTaintCreeper.func_70071_h_} (lines 122-140): the swell
     * state drives the fuse: with {@code getCreeperState() > 0} and
     * {@code timeSinceIgnited == 0} the fuse sound fires (line 125,
     * {@code "random.fuse", 1.0F, 0.5F}); the fuse accumulates by the
     * state each tick, floors at {@code 0} (lines 128-131) and caps at
     * {@code 30} (lines 133-134, {@code timeSinceIgnited = 30}).
     */
    public static int nextFuse(final int timeSinceIgnited, final int creeperState,
            final int fuseCap) {
        final int next = timeSinceIgnited + creeperState;
        return Math.min(fuseCap, Math.max(0, next));
    }

    public static boolean explodedThisTick(final int fuse, final int fuseCap) {
        return fuse >= fuseCap;
    }

    /**
     * {@code func_70069_a} (lines 70-77): the death-animation fuse step,
     * {@code (int)(timeSinceIgnited + par1 * 1.5F)} — par1 is the
     * deathTime delta (1.0 per tick), so the fuse gains +1/tick truncated.
     * The caller applies the {@code fuseTime - 5} cap (lines 75-76).
     */
    public static int deathFuseStep(final int timeSinceIgnited, final float deathTimeDelta) {
        return (int) (timeSinceIgnited + deathTimeDelta * 1.5F);
    }

    /**
     * {@code func_82143_as} (lines 65-67): {@code getTarget() == null ? 3 :
     * 3 + (int)(getMaxHealth() - 1.0F)} — 32 at health 30.0.
     */
    public static int experiencePoints(final boolean hasTarget, final double maxHealth) {
        return hasTarget ? 3 + (int) (maxHealth - 1.0F) : 3;
    }

    /** Line 125: the fuse sound is played at pitch {@code 0.5F}. */
    public static final float FUSE_SOUND_PITCH = 0.5F;

    /**
     * {@code getCreeperFlashIntensity(float)} (line 192): the swell
     * animation divides by {@code 28.0F} — a client render helper, deferred
     * to the renderer row (TC-M7-03-s).
     */
    public static final float FLASH_INTENSITY_DIVISOR = 28.0F;

    /**
     * {@code explode()} (method lines 133-171; the blast call is line 136):
     * the explosion radius is {@code 1.5F} ({@code func_72876_a(this, x, y
     * + height/2, z, 1.5F, false)}) — the {@code explosionRadius} field is
     * NEVER read by the blast, a preserved quirk; the 1.7.10 6-argument
     * overload always destroys blocks (no mobGriefing rule), so the port's
     * Level.ExplosionInteraction is MOB, passed unconditionally.
     */
    public static final float EXPLOSION_RADIUS = 1.5F;
    /** {@code explosionRadius} (field, line 37, init 3): round-trips through the
     *  "ExplosionRadius" NBT byte (lines 87, 98) and is never read by the blast. */
    public static final float LOADED_RADIUS_FIELD = 3.0F;
    /** Line 87: {@code par1NBTTagCompound.func_74774_a("ExplosionRadius", ...)}. */
    public static final String TAG_EXPLOSION_RADIUS = "ExplosionRadius";
    /** Line 86: {@code par1NBTTagCompound.func_74777_a("Fuse", (short) this.fuseTime)}. */
    public static final String TAG_FUSE = "Fuse";
    /** Line 83: {@code par1NBTTagCompound.func_74757_a("powered", true)}. */
    public static final String TAG_POWERED = "powered";

    /**
     * {@code explode()} (lines 143-152): every LivingEntity inside the box
     * that is the entity's own POSITION inflated by {@code 6.0} on all
     * three axes (a zero-size AABB at the position, {@code func_72330_a(x,
     * y, z, x, y, z).func_72314_b(6.0, 6.0, 6.0)}) that is NOT a taint
     * creature and NOT wet gets the taint poison effect for {@code 100}
     * ticks at amplifier {@code 0}.
     */
    public static final double POISON_BOX_INFLATE = 6.0D;
    public static final int POISON_DURATION = 100;
    public static final int POISON_AMPLIFIER = 0;

    public static boolean poisonsTarget(final boolean tainted, final boolean wet) {
        return !tainted && !wet;
    }

    /**
     * {@code explode()} (lines 156-168): the taint write-up scans {@code 10}
     * offsets in {@code [-5, 5)} on x/z — the offset is TWO independent
     * {@code nextFloat()} draws ({@code (int)((nextFloat() - nextFloat()) *
     * 5.0F)}), not a single scaled draw — and the 50% coin
     * ({@code nextBoolean()}) plus the biome test are evaluated INSIDE the
     * loop, per attempt. A winning attempt writes the taint biome and, when
     * the cell below is a solid render block and the cell itself is
     * replaceable, places fibrous taint meta {@code 0}.
     */
    public static final int BIAS_ATTEMPTS = 10;
    public static final double BIAS_OFFSET = 5.0D;
    public static final int FIBRES_PLACED_META = 0;

    /** {@code (int)((rollA - rollB) * 5.0F)} — two independent draws (line 159). */
    public static int biasOffset(final float rollA, final float rollB) {
        return (int) ((rollA - rollB) * (float) BIAS_OFFSET);
    }

    /** Line 158: {@code nextBoolean()} gates the write-up, per attempt. */
    public static boolean biasPlacementRoll(final boolean coin) {
        return coin;
    }

    /**
     * {@code AICreeperSwell.func_75250_a} (canUse): engages when the swell
     * state is already above 0, or a target exists within squared distance
     * {@code 9.0}; {@code func_75246_d} (update): the state resets to -1
     * when the target is gone, the squared distance exceeds {@code 49.0},
     * or the target is no longer visible; otherwise the state is 1.
     */
    public static boolean swellEngage(final int creeperState, final boolean hasTarget,
            final double squaredDistance) {
        return creeperState > 0 || (hasTarget && squaredDistance < 9.0D);
    }

    public static int swellUpdateState(final boolean hasTarget, final double squaredDistance,
            final boolean canSeeTarget) {
        if (!hasTarget || squaredDistance > 49.0D || !canSeeTarget) {
            return -1;
        }
        return 1;
    }

    private TaintCreeperAiLogic() {
    }
}
