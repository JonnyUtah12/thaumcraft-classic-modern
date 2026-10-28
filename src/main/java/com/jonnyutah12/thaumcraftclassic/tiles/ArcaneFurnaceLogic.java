package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.util.RandomSource;

/**
 * The arcane furnace's extraction numbers (TC-M4-06-s), as pure functions.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneFurnace}
 * ({@code calcCookTime}, {@code getBellows}, {@code ejectItem}) and
 * {@code TileArcaneFurnaceNozzle} ({@code updateEntity}, {@code drawEssentia},
 * {@code getSuctionAmount}). They are the "extraction rates" TC-M4-06-s is
 * specified to carry, so they live here, away from a {@code Level}, and
 * ArcaneFurnaceLogicTest pins each one value for value with a seeded
 * {@link RandomSource} (the {@code JarStorageLogic}/{@code CrucibleStateMachine}
 * pattern).</p>
 *
 * <h2>The rates, verbatim from 4.2.3.5</h2>
 * <ul>
 *   <li><b>cook time</b> = {@code (speedyTime > 0 ? 80 : 140) - 20 * bellows},
 *       ticking <em>down</em> to 0, recomputed when a new item starts;
 *       bellows is capped at 3, so the range is 20 ticks (3 bellows + fire
 *       vis) to 140 ticks (nothing);</li>
 *   <li><b>fire vis</b> — the furnace drains up to 5 FIRE vis from the area
 *       whenever {@code speedyTime} has run out, and the drained amount
 *       <em>is</em> speedyTime, so a charged furnace cooks at the 80-tick
 *       rate for that many ticks; one drain unit decays per tick;</li>
 *   <li><b>the nozzle</b> pulls 1 FIRE essentia out of the connectable in
 *       front of it every 5 ticks, only while {@code speedyTime < 60}, and
 *       each successful draw adds 600 to speedyTime; it advertises suction 128
 *       while {@code speedyTime < 40} and 0 above that, so a full furnace
 *       stops starving the tube line;</li>
 *   <li><b>bonus drops</b> roll per {@link com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses};
 *       bellows==0 is a 1-in-4 single drop, bellows==n is n rolls at 0.44
 *       each — an expected 1.32 items with 3 bellows;</li>
 *   <li><b>experience</b> follows the original furnace rule: the result's xp
 *       figure of 0 gives nothing, a sub-1.0 figure rounds up on
 *       {@code random < count * xp - floor(count * xp)}, anything else pays
 *       {@code count * xp} orbs.</li>
 * </ul>
 */
public final class ArcaneFurnaceLogic {

    /** Original {@code furnaceItemStacks.length}. */
    public static final int SLOTS = 32;

    /** Original {@code getInventoryStackLimit}. */
    public static final int STACK_LIMIT = 64;

    /** Original {@code calcCookTime} base, unboosted / boosted by fire vis. */
    public static final int COOK_TIME_SLOW = 140;
    public static final int COOK_TIME_FAST = 80;

    /** Original: each bellows shaves 20 ticks. */
    public static final int COOK_TIME_PER_BELLOWS = 20;

    /** Original {@code getBellows} cap. */
    public static final int MAX_BELLOWS = 3;

    /** Original {@code VisNetHandler.drainVis(world, pos, Aspect.FIRE, 5)}. */
    public static final int VIS_DRAIN_REQUEST = 5;

    /** Original: speedyTime decays by one per tick while cooking. */
    public static final int SPEEDY_DECAY = 1;

    /** Original nozzle cadence: {@code ++drawDelay % 5 == 0}. */
    public static final int NOZZLE_DRAW_CADENCE = 5;

    /** Original: one FIRE essentia per successful nozzle draw. */
    public static final int NOZZLE_DRAW_AMOUNT = 1;

    /** Original: a successful draw adds 600 to the furnace's speedyTime. */
    public static final int NOZZLE_SPEEDY_BONUS = 600;

    /** Original: the nozzle draws only while the furnace is under this. */
    public static final int NOZZLE_DRAW_LIMIT = 60;

    /** Original: suction 128 under this speedyTime, 0 above. */
    public static final int NOZZLE_SUCTION_LIMIT = 40;
    public static final int NOZZLE_SUCTION = 128;

    /** Per-bellows bonus roll chance (original {@code nextFloat() < 0.44F}). */
    public static final float BELLOWS_BONUS_CHANCE = 0.44F;

    /** Cook time for the current state (original {@code calcCookTime}). */
    public static int cookTime(final int speedyTime, final int bellows) {
        return (speedyTime > 0 ? COOK_TIME_FAST : COOK_TIME_SLOW) - COOK_TIME_PER_BELLOWS * bellows(bellows);
    }

    /** The original's {@code Math.min(3, bellows)} cap. */
    public static int bellows(final int raw) {
        return Math.min(MAX_BELLOWS, raw);
    }

    /** True on the tick the nozzle tries to draw (original {@code ++drawDelay % 5 == 0}). */
    public static boolean nozzleDrawTick(final int drawDelayAfterIncrement) {
        return drawDelayAfterIncrement % NOZZLE_DRAW_CADENCE == 0;
    }

    /** Original {@code drawEssentia} gate: the furnace is not already saturated. */
    public static boolean nozzleCanDraw(final int speedyTime) {
        return speedyTime < NOZZLE_DRAW_LIMIT;
    }

    /** Original {@code getSuctionAmount}: 128 while starved, 0 once fed. */
    public static int nozzleSuction(final int speedyTime) {
        return speedyTime < NOZZLE_SUCTION_LIMIT ? NOZZLE_SUCTION : 0;
    }

    /** Original: a successful draw pushes speedyTime up by 600. */
    public static int speedyAfterDraw(final int speedyTime) {
        return speedyTime + NOZZLE_SPEEDY_BONUS;
    }

    /**
     * Original {@code ejectItem} xp block: {@code count * xp} with the
     * sub-1.0 fractional rounding, and nothing at all when the result carries
     * 0 xp.
     */
    public static int experience(final int resultCount, final float xp, final RandomSource random) {
        int out = resultCount;
        if (xp == 0.0F) {
            return 0;
        }
        if (xp < 1.0F) {
            final int floor = (int) Math.floor(out * xp);
            if (floor < (int) Math.ceil(out * xp) && random.nextFloat() < out * xp - floor) {
                out = floor + 1;
            } else {
                out = floor;
            }
            return out;
        }
        return out;
    }

    /**
     * The eject offset the original computes from the nozzle direction:
     * 1.2 blocks out in the facing axis, 0.5 centred on the other axis, at
     * 0.4 height.
     */
    public static double ejectOffsetAxis(final int facing) {
        return 0.5D + facing * 1.2D;
    }

    /** Original eject velocity: 0.13 along the facing axis, ±0.03 jitter on the other. */
    public static double ejectVelocity(final int facing, final RandomSource random) {
        return facing == 0
                ? (random.nextFloat() - random.nextFloat()) * 0.03D
                : facing * 0.13D;
    }

    private ArcaneFurnaceLogic() {
    }
}
