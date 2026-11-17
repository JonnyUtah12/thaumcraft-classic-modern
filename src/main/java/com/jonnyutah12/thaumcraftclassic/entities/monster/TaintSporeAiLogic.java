package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Level-free rules shared by the taint spore and the taint spore swarmer
 * (TC-M6-06-s-f).
 *
 * <p>Every constant and branch below is read straight off the decompiled 1.7.10
 * sources; each citation is {@code File:line} inside
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/}
 * ({@code EntityTaintSpore.java} = 237 lines, {@code EntityTaintSporeSwarmer.java}
 * = 163 lines, the swarmer being a top-level subclass of the spore). The entity
 * classes walk state; the arithmetic and gates live here so the JUnit suite can
 * pin them without a {@code Level}.</p>
 *
 * <p>Preserved quirks, never modernised:</p>
 * <ul>
 *   <li>the size ladder runs on <em>both</em> sides — the original gates no
 *       {@code world.field_72995_K} around it, so the client grows the spore from
 *       its own synced copy too ({@code EntityTaintSpore.java:123-126});</li>
 *   <li>{@code getSporeSize() < 10 && growth++ == 1200} short-circuits, so a
 *       full-grown spore's growth counter freezes at 1200 forever
 *       ({@code EntityTaintSpore.java:123});</li>
 *   <li>NBT stores {@code size - 1} and the read adds 1 back, so an entity loaded
 *       without the tag lands on size 1, not on its constructor size
 *       ({@code EntityTaintSpore.java:41}, {@code :46});</li>
 *   <li>the spawn packet sends the spore size as a float and the client drops it
 *       into {@code displaySize}, never into the size itself
 *       ({@code EntityTaintSpore.java:195-204});</li>
 *   <li>the xp value IS the spore size, rewritten on every size change, so a
 *       full-grown spore pays 10 and a fresh one 2
 *       ({@code EntityTaintSpore.java:54}); the swarmer keeps that write with size
 *       10 ({@code EntityTaintSporeSwarmer.java:29});</li>
 *   <li>the swarmer overrides the setter with a FIXED 1×1 box, so its hitbox never
 *       grows while its swarm budget does ({@code EntityTaintSporeSwarmer.java:25-30});</li>
 *   <li>the burst counter compares the hurt <em>duration</em> with
 *       {@code == 1} ({@code EntityTaintSporeSwarmer.java:72}) — the burst fires
 *       exactly once, on the last flash tick;</li>
 *   <li>the swarm-FX cull removes ONE handle per tick and breaks
 *       ({@code EntityTaintSpore.java:133-138});</li>
 *   <li>a spore standing on a mature spore stalk gets no movement integration at
 *       all, and on any other surface only downward motion survives
 *       ({@code EntityTaintSpore.java:79-92}).</li>
 * </ul>
 */
public final class TaintSporeAiLogic {

    // ------------------------------------------------------------- size / box

    /** {@code EntityTaintSpore.java:31} — the ctor calls {@code setSporeSize(2)}. */
    public static final int SPORE_INITIAL_SIZE = 2;
    /**
     * {@code EntityTaintSpore.java:36} — the data-watcher init
     * ({@code func_75682_a(16, (byte) 1)}), i.e. the synched default is 1.
     */
    public static final int SPORE_SIZE_DATA_INIT = 1;
    /** {@code EntityTaintSpore.java:123} — the size ladder stops at 10. */
    public static final int SPORE_MAX_SIZE = 10;
    /** {@code EntityTaintSpore.java:123} — 1200 ticks (a minute) per size step. */
    public static final int SPORE_GROWTH_TICKS = 1200;
    /** {@code EntityTaintSpore.java:51} — {@code Math.max(0.15F * size, 0.5F)}. */
    public static final float SPORE_BOX_PER_SIZE = 0.15F;
    /** {@code EntityTaintSpore.java:51} — the box floor. */
    public static final float SPORE_BOX_MIN = 0.5F;
    /** {@code EntityTaintSporeSwarmer.java:27} — the swarmer's fixed 1.0×1.0 box. */
    public static final float SWARMER_BOX = 1.0F;
    /** {@code EntityTaintSporeSwarmer.java:21} — the ctor calls {@code setSporeSize(10)}. */
    public static final int SWARMER_INITIAL_SIZE = 10;
    /** {@code EntityTaintSpore.java:67-69} — {@code getEyeHeight() = 0.0F}. */
    public static final float EYE_HEIGHT = 0.0F;
    /** {@code EntityTaintSpore.java:54} — {@code field_70728_aV = size}: xp IS size. */
    public static final int SPORE_SIZE_XP_DIVISOR = 1;

    // ------------------------------------------------------------- attributes

    /** {@code EntityTaintSpore.java:63} — MAX_HEALTH (1.7.10 {@code field_111267_a}). */
    public static final double HEALTH = 1.0;
    /** {@code EntityTaintSpore.java:64} — ATTACK_DAMAGE (1.7.10 {@code field_111264_e}). */
    public static final double ATTACK = 1.0;
    /** {@code EntityTaintSporeSwarmer.java:35} — MAX_HEALTH 75.0, written after the parent's 1.0. */
    public static final double SWARMER_HEALTH = 75.0;
    /** {@code EntityTaintSporeSwarmer.java:36} — ATTACK_DAMAGE 1.0. */
    public static final double SWARMER_ATTACK = 1.0;

    // ------------------------------------------------------- biome / starve

    /** {@code EntityTaintSpore.java:113} — the off-taint starve cadence. */
    public static final int STARVE_INTERVAL = 20;
    /** {@code EntityTaintSpore.java:116} — {@code DamageSource.field_76366_f} = starve. */
    public static final float STARVE_DAMAGE = 1.0F;

    // ---------------------------------------------------------- client state

    /** {@code EntityTaintSpore.java:130} — the display-size ramp, per tick. */
    public static final float DISPLAY_SIZE_STEP = 0.02F;
    /** {@code EntityTaintSpore.java:140} — spore: swarm FX budget = {@code size / 3} (int division). */
    public static final int SWARM_FX_DIVISOR = 3;
    /** {@code EntityTaintSporeSwarmer.java:67} — swarmer: budget = {@code (500 - counter) / 25}. */
    public static final int SWARMER_FX_DIVISOR = 25;

    // -------------------------------------------------------------- bursting

    /** {@code EntityTaintSpore.java:162} — {@code size / 3 + rand.nextInt(size / 2 + 1)}. */
    public static final int BURST_SIZE_DIVISOR = 3;
    /** {@code EntityTaintSpore.java:162} — the jitter divisor. */
    public static final int BURST_SIZE_HALF_DIVISOR = 2;
    /** {@code EntityTaintSpore.java:161}, {@code EntityTaintSporeSwarmer.java:83} — gore volume. */
    public static final float GORE_VOLUME = 1.0F;
    /** {@code EntityTaintSpore.java:161} — {@code 0.9F + rand.nextFloat() * 0.1F}. */
    public static final float GORE_PITCH_BASE = 0.9F;
    /** {@code EntityTaintSpore.java:161}. */
    public static final float GORE_PITCH_SPREAD = 0.1F;
    /** {@code EntityTaintSpore.java:185} — the client's burst FX: {@code sploosh(50)}. */
    public static final int BURST_SPLLOOSH = 50;
    /** {@code EntityTaintSporeSwarmer.java:41} — {@code sploosh(10)} on every incoming hit. */
    public static final int SWARMER_HURT_SPLLOOSH = 10;
    /** {@code EntityTaintSporeSwarmer.java:99} — {@code sploosh(25)} on the burst event. */
    public static final int SWARMER_EVENT_SPLLOOSH = 25;

    // ------------------------------------------------------- the swarmer loop

    /** {@code EntityTaintSporeSwarmer.java:17}, {@code :55} — the swarm cadence. */
    public static final int SWARMER_SPAWN_INTERVAL = 500;
    /** {@code EntityTaintSporeSwarmer.java:54} — {@code getClosestPlayerToEntity(this, 16.0)}. */
    public static final double SWARMER_PLAYER_SCAN = 16.0;
    /** {@code EntityTaintSporeSwarmer.java:72} — the burst fires on the hurt duration exactly 1. */
    public static final int SWARMER_HURT_EVENT_TICK = 1;
    /** {@code EntityTaintSporeSwarmer.java:91}/{@code :97} — the entity event id of a swarm burst. */
    public static final byte SWARMER_BURST_EVENT = 6;
    /** {@code EntityTaintSporeSwarmer.java:87} — the swarm spawns half a block above the swarmer. */
    public static final double SWARM_SPAWN_Y_OFFSET = 0.5;

    // -------------------------------------------------------------- the stalk

    /**
     * {@code EntityTaintSpore.java:180} — the burst rewrites the mature stalk to
     * meta 3. Must equal {@code BlockTaintFibres.META_SPORE_STALK} (pinned by the
     * unit test).
     */
    public static final int FIBRES_META_SPORE_STALK = 3;
    /**
     * {@code EntityTaintSpore.java:89}, {@code :148}, {@code :179} — the stalk the
     * spore roots in. Must equal {@code BlockTaintFibres.META_MATURE_SPORE_STALK}
     * (pinned by the unit test).
     */
    public static final int FIBRES_META_MATURE_SPORE_STALK = 4;

    // ------------------------------------------------------------ renderer-side

    /**
     * {@code EntityTaintSpore.java:97-99} — {@code isInRangeToRenderDist(dist) :
     * dist < 4096.0}. 1.7.10 hands the RENDER DISTANCE to the gate; 1.21.1 moved
     * that decision into the renderer / tracking range, so this rule ships pinned
     * and the 64-block cap is the parent's catalog tracking-range note.
     */
    public static final double RENDER_DISTANCE_SQ = 4096.0;
    /** {@code EntityTaintSpore.java:103} — the client overlay color (full-bright ARGB). */
    public static final int OVERLAY_COLOR = 15728880;
    /** {@code EntityTaintSpore.java:107} — the spore's {@code getBrightness} is a flat 1.0. */
    public static final float SPORE_BRIGHTNESS = 1.0F;
    /**
     * {@code EntityTaintSporeSwarmer.java:127}, {@code :140} — the light sample
     * sits {@code (aabb.height) * 0.66} above {@code posY - eyeHeight}.
     */
    public static final float BRIGHTNESS_SAMPLE_FRACTION = 0.66F;

    // ---------------------------------------------------------------- sounds

    /** {@code EntityTaintSpore.java:206-208} — {@code getSoundVolume() = 0.1F}. */
    public static final float SOUND_VOLUME = 0.1F;
    /** {@code EntityTaintSpore.java:210-212}, {@code EntityTaintSporeSwarmer.java:111-114}. */
    public static final int AMBIENT_SOUND_INTERVAL = 200;

    // ----------------------------------------------------------------- drops

    /** itemResource meta 11 — tainted goo ({@code EntityTaintSpore.java:232}). */
    public static final int META_TAINTED_GOO = 11;
    /** itemResource meta 12 — tainted fungus ({@code EntityTaintSpore.java:234}). */
    public static final int META_TAINTED_FUNGUS = 12;
    /**
     * {@code EntityTaintSporeSwarmer.java:155} — {@code for (a = 0; a <= 1; a++)}:
     * the swarmer rolls the coin flip twice, so it can drop two goo, two fungus,
     * or one of each.
     */
    public static final int SWARMER_DROP_ROLLS = 2;

    private TaintSporeAiLogic() {
    }

    // ------------------------------------------------------------------ rules

    /**
     * {@code EntityTaintSpore.java:51} — the box is
     * {@code Math.max(0.15F * size, 0.5F)} in BOTH axes (size 2 → 0.5, size 10 → 1.5).
     */
    public static float sporeBoxSize(final int sporeSize) {
        return Math.max(SPORE_BOX_PER_SIZE * sporeSize, SPORE_BOX_MIN);
    }

    /**
     * {@code EntityTaintSpore.java:54} — {@code field_70728_aV = par1}: the xp
     * reward tracks the size.
     */
    public static int xpForSize(final int sporeSize) {
        return sporeSize / SPORE_SIZE_XP_DIVISOR;
    }

    /**
     * {@code EntityTaintSpore.java:89}, {@code :148}, {@code :179} — the cell below
     * the spore is the stalk: it must BE taint fibres and carry meta 4.
     */
    public static boolean isMatureSporeStalk(final boolean fibresBlock, final int variant) {
        return fibresBlock && variant == FIBRES_META_MATURE_SPORE_STALK;
    }

    /**
     * {@code EntityTaintSpore.java:79-92} — the movement hook: x and z are thrown
     * away and any upward motion is clamped to 0, so the spore only ever falls.
     */
    public static double travelY(final double dy) {
        return dy > 0.0 ? 0.0 : dy;
    }

    /**
     * {@code EntityTaintSpore.java:112-117} — one starve heart every 20 ticks while
     * the biome under the spore is not the taint biome (server side only).
     */
    public static boolean starves(final int tickCount, final boolean inTaintBiome) {
        return !inTaintBiome && tickCount % STARVE_INTERVAL == 0;
    }

    /**
     * {@code EntityTaintSpore.java:123} — the size step fires when
     * {@code size < 10} AND the PRE-increment growth counter reads 1200.
     */
    public static boolean grows(final int sporeSize, final int growth) {
        return sporeSize < SPORE_MAX_SIZE && growth == SPORE_GROWTH_TICKS;
    }

    /**
     * {@code EntityTaintSpore.java:123} — the {@code growth++} lives inside the
     * short-circuited condition: below the cap the counter climbs by one per tick,
     * at the cap it stops moving entirely.
     */
    public static int nextGrowth(final int sporeSize, final int growth) {
        return sporeSize < SPORE_MAX_SIZE ? growth + 1 : growth;
    }

    /**
     * {@code EntityTaintSpore.java:129-131} — the client ramps the display size by
     * 0.02 a tick until it catches the real size, then holds.
     */
    public static float nextDisplaySize(final float displaySize, final int sporeSize) {
        return displaySize < sporeSize ? displaySize + DISPLAY_SIZE_STEP : displaySize;
    }

    /** {@code EntityTaintSpore.java:140} — {@code swarm.size() < size / 3} (int division). */
    public static boolean sporeWantsSwarmFx(final int swarmFxCount, final int sporeSize) {
        return swarmFxCount < sporeSize / SWARM_FX_DIVISOR;
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:67} —
     * {@code swarm.size() < (500 - spawnCounter) / 25}: the swarm grows through the
     * cadence window.
     */
    public static boolean swarmerWantsSwarmFx(final int swarmFxCount, final int spawnCounter) {
        return swarmFxCount < (SWARMER_SPAWN_INTERVAL - spawnCounter) / SWARMER_FX_DIVISOR;
    }

    /**
     * {@code EntityTaintSpore.java:133-138} — the cull walks the list, removes the
     * FIRST dead handle and breaks: at most one handle dies per tick.
     *
     * @param deadHandlePresent the list holds at least one null / dead handle
     */
    public static int swarmFxAfterCull(final int swarmFxCount, final boolean deadHandlePresent) {
        return deadHandlePresent && swarmFxCount > 0 ? swarmFxCount - 1 : swarmFxCount;
    }

    /**
     * {@code EntityTaintSpore.java:148-152} — off a mature stalk the spore bursts
     * every tick; on one, it bursts while the hurt flash is running
     * ({@code field_70725_aQ > 0} — the hurt DURATION, 1.21.1 {@code hurtDuration}).
     */
    public static boolean sporeBursts(final boolean onMatureStalk, final int hurtDuration) {
        return !onMatureStalk || hurtDuration > 0;
    }

    /** {@code EntityTaintSporeSwarmer.java:72-74} — the burst fires on hurt duration 1, exactly once. */
    public static boolean swarmerBurstsOnHurt(final int hurtDuration) {
        return hurtDuration == SWARMER_HURT_EVENT_TICK;
    }

    /** {@code EntityTaintSpore.java:162} — {@code size / 3 + roll}, {@code roll} in {@code [0, size / 2]}. */
    public static int burstCount(final int sporeSize, final int roll) {
        return sporeSize / BURST_SIZE_DIVISOR + roll;
    }

    /** {@code EntityTaintSpore.java:162} — the {@code rand.nextInt} bound, {@code size / 2 + 1}. */
    public static int burstRollBound(final int sporeSize) {
        return sporeSize / BURST_SIZE_HALF_DIVISOR + 1;
    }

    /** {@code EntityTaintSpore.java:161} — the gore pitch {@code 0.9F + roll * 0.1F}. */
    public static float gorePitch(final float roll) {
        return GORE_PITCH_BASE + roll * GORE_PITCH_SPREAD;
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:50-52} — {@code if (spawnCounter > 0)
     * spawnCounter--}: the counter is clamped at 0 and never goes negative.
     */
    public static int swarmerNextSpawnCounter(final int spawnCounter) {
        return spawnCounter > 0 ? spawnCounter - 1 : spawnCounter;
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:54-57} — a burst needs the counter spent
     * AND a player inside the 16-block scan; the counter re-arms to 500.
     */
    public static boolean swarmerBursts(final int spawnCounter, final boolean playerWithinScan) {
        return spawnCounter <= 0 && playerWithinScan;
    }

    /**
     * {@code EntityTaintSpore.java:97-99} — the render gate: 1.7.10 passes the
     * SQUARED distance, so the cap is 64 blocks. Renderer-side in 1.21.1
     * (see this card's left_out_of_scope); the rule ships pinned.
     */
    public static boolean isWithinRenderDistance(final double distanceSquared) {
        return distanceSquared < RENDER_DISTANCE_SQ;
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:128}, {@code :141} — the light sample row:
     * {@code floor(posY - eyeHeight + (aabb.height) * 0.66)}.
     */
    public static int brightnessSampleY(final double posY, final float eyeHeight, final float bbHeight) {
        return Mth.floor(posY - eyeHeight + bbHeight * BRIGHTNESS_SAMPLE_FRACTION);
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:136-146} — the swarmer samples the world
     * light at {@link #brightnessSampleY} inside its own column, and reports 0 when
     * the column at y = 0 does not see the sky. Renderer-side in 1.21.1; the rule
     * ships pinned ({@code maxLocalLight} is {@code Level#getMaxLocalRawBrightness}).
     */
    public static float swarmerBrightness(final int maxLocalLight, final boolean columnSeesSky) {
        return columnSeesSky ? maxLocalLight / 15.0F : 0.0F;
    }

    /**
     * {@code EntityTaintSporeSwarmer.java:121-133} — the overlay lightmap, same
     * column gate and the same 0 fallback (the parent's flat 15728880 is
     * {@link #OVERLAY_COLOR}). Renderer-side in 1.21.1; the rule ships pinned.
     */
    public static int swarmerOverlayLightmap(final int lightmap, final boolean columnSeesSky) {
        return columnSeesSky ? lightmap : 0;
    }

    /**
     * {@code EntityTaintSpore.java:231-235} — the death drop is a coin flip between
     * tainted goo (meta 11) and tainted fungus (meta 12).
     *
     * @return true for goo, false for fungus
     */
    public static boolean rollsGoo(final RandomSource random) {
        return random.nextBoolean();
    }

    /**
     * {@code EntityTaintSpore.java:232} — the drop offset is
     * {@code field_70131_O / 2.0F}: 1.7.10's {@code field_70131_O} is the entity
     * HEIGHT set by {@code setSize}, not the max health, so a size-10 spore drops
     * from 5 blocks up and a swarmer (1.0 box) from 0.5.
     */
    public static float dropOffset(final float bbHeight) {
        return bbHeight / 2.0F;
    }
}
