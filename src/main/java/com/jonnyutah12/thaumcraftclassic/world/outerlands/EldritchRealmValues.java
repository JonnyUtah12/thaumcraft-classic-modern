package com.jonnyutah12.thaumcraftclassic.world.outerlands;

/**
 * The numeric contract of the Outer Lands catalog slice (TC-M6-04f-s).
 *
 * <p>Every number here is read straight off the 1.7.10 source this card ports
 * ({@code thaumcraft.common.blocks.BlockEldritch},
 * {@code BlockEldritchNothing}, {@code BlockFluidPure},
 * {@code BlockFluidDeath}, {@code thaumcraft.common.tiles.TileEldritch*} and
 * {@code thaumcraft.common.items.ItemEldritchObject}). Keeping them in one
 * dependency-free class means the shipped block, the block entities and the
 * unit parity test all read the same table — a number cannot drift between
 * the code and its evidence.</p>
 */
public final class EldritchRealmValues {

    // ------------------------------------------------------------- blockEldritch

    /** Original {@code BlockEldritch} metadata domain: 0-10 (11 states). */
    public static final int ELDRITCH_VARIANT_COUNT = 11;
    public static final int ELDRITCH_ALTAR = 0;
    public static final int ELDRITCH_OBELISK = 1;
    public static final int ELDRITCH_OBELISK_TOP = 2;
    public static final int ELDRITCH_CAP = 3;
    public static final int ELDRITCH_CRUSTED = 4;
    public static final int ELDRITCH_GLYPHED = 5;
    public static final int ELDRITCH_DOORWAY = 6;
    public static final int ELDRITCH_GATE = 7;
    public static final int ELDRITCH_LOCK = 8;
    public static final int ELDRITCH_CRAB_SPAWNER = 9;
    public static final int ELDRITCH_TRAP = 10;

    /**
     * The original's BlockEldritch material: hardness 20, blast resistance
     * 1200; the experience table caps at MAX_XP 3000 (the spawner scales with
     * its count) and MAX_STACK_XP 1500 (the altar with its charge).
     */
    public static final float ELDRITCH_HARDNESS = 20.0F;
    public static final float ELDRITCH_RESISTANCE = 1200.0F;
    public static final int ELDRITCH_TICK_PERIOD = 20;
    public static final int ELDRITCH_MAX_XP = 3000;
    public static final int ELDRITCH_MAX_STACK_XP = 1500;
    public static final int ELDRITCH_OBELISK_XP = 1000;
    public static final int ELDRITCH_OBELISK_TOP_XP = 0;
    public static final int ELDRITCH_CAP_XP = 500;

    /**
     * Original {@code getLightValue}: 4/5/7 glow 12, 6/8 glow 5, 9 glows 4,
     * 10 is dark, everything else glows 8.
     */
    public static int light(final int variant) {
        if (variant == ELDRITCH_CRUSTED || variant == ELDRITCH_GLYPHED || variant == ELDRITCH_GATE) {
            return 12;
        }
        if (variant == ELDRITCH_DOORWAY || variant == ELDRITCH_LOCK) {
            return 5;
        }
        if (variant == ELDRITCH_CRAB_SPAWNER) {
            return 4;
        }
        return variant == ELDRITCH_TRAP ? 0 : 8;
    }

    /** The unbreakable marker — the original returned hardness -1. */
    public static final float UNBREAKABLE = -1.0F;

    /**
     * Original {@code getBlockHardness}: base 20000, 4/5 → 2.0, 6 → 4.0,
     * 7/8 → -1 (unbreakable), 9/10 → 15.0.
     */
    public static float hardness(final int variant) {
        if (variant == ELDRITCH_CRUSTED || variant == ELDRITCH_GLYPHED) {
            return 2.0F;
        }
        if (variant == ELDRITCH_DOORWAY) {
            return 4.0F;
        }
        if (variant == ELDRITCH_GATE || variant == ELDRITCH_LOCK) {
            return UNBREAKABLE;
        }
        if (variant == ELDRITCH_CRAB_SPAWNER || variant == ELDRITCH_TRAP) {
            return 15.0F;
        }
        return 20000.0F;
    }

    /**
     * Original {@code getExplosionResistance}: base 50, 4/5/9/10 → 30,
     * 6 → 100, 7/8 → {@code Float.MAX_VALUE}.
     */
    public static float resistance(final int variant) {
        if (variant == ELDRITCH_CRUSTED || variant == ELDRITCH_GLYPHED
                || variant == ELDRITCH_CRAB_SPAWNER || variant == ELDRITCH_TRAP) {
            return 30.0F;
        }
        if (variant == ELDRITCH_DOORWAY) {
            return 100.0F;
        }
        if (variant == ELDRITCH_GATE || variant == ELDRITCH_LOCK) {
            return Float.MAX_VALUE;
        }
        return 50.0F;
    }

    /**
     * Original {@code getExpDrop}: 5 and 10 drop 1-4 experience, 9 drops 6-10,
     * every other variant drops none.
     */
    public static int xpMin(final int variant) {
        return variant == ELDRITCH_GLYPHED || variant == ELDRITCH_TRAP ? 1 : (variant == ELDRITCH_CRAB_SPAWNER ? 6 : 0);
    }

    /** Upper bound of {@link #xpMin(int)}'s original {@code MathHelper.getRandomInRange} roll. */
    public static int xpMax(final int variant) {
        return variant == ELDRITCH_GLYPHED || variant == ELDRITCH_TRAP ? 4 : (variant == ELDRITCH_CRAB_SPAWNER ? 10 : 0);
    }

    /**
     * Original {@code hasTileEntity}: only metas 0, 1, 3, 8, 9 and 10 carry a
     * tile entity; every other variant creates none (the original's
     * {@code createTileEntity} returned null).
     */
    public static boolean hasTile(final int variant) {
        return variant == ELDRITCH_ALTAR || variant == ELDRITCH_OBELISK || variant == ELDRITCH_CAP
                || variant == ELDRITCH_LOCK || variant == ELDRITCH_CRAB_SPAWNER || variant == ELDRITCH_TRAP;
    }

    /**
     * Original {@code getItemDropped}/{@code getDrops}: only the crusted stone
     * (4) drops itself and the glyphed stone (5) drops a glyph ({@code
     * itemResource} meta 9); every other variant drops nothing, and
     * {@code damageDropped} maps 2 → 1 so an obelisk top drops an obelisk.
     */
    public static boolean dropsItself(final int variant) {
        return variant == ELDRITCH_CRUSTED;
    }

    /** Meta of {@code thaumcraft:itemresource} the glyphed stone (5) drops. */
    public static final int GLYPH_RESOURCE_META = 9;

    /** {@code damageDropped(meta)}: meta 2 drops as meta 1. */
    public static int droppedVariant(final int variant) {
        return variant == ELDRITCH_OBELISK_TOP ? ELDRITCH_OBELISK : variant;
    }

    /**
     * Original {@code onNeighborChange}: a variant below 4 refreshes every
     * variant-below-4 neighbour in a 7×5×7 box and plays the rumble.
     */
    public static final int NEIGHBOUR_SCAN_RADIUS_XZ = 3;
    public static final int NEIGHBOUR_SCAN_RADIUS_Y = 2;
    /** The original's {@code meta < 4} gate for that scan. */
    public static final int NEIGHBOUR_SCAN_MAX_VARIANT = 3;

    // --------------------------------------------------- TileEldritch (cadences)

    /** Original {@code TileEldritchAltar}: the eye socket takes 4 eyes. */
    public static final int ALTAR_MAX_EYES = 4;
    /** Two eyes armed the guardian spawner (original {@code getEyes() >= 2}). */
    public static final int ALTAR_SPAWNER_EYES = 2;
    /** Original altar {@code onUpdate}: first wave at 80 ticks, then every 40. */
    public static final int ALTAR_FIRST_WAVE_TICKS = 80;
    public static final int ALTAR_WAVE_PERIOD_TICKS = 40;
    /** Original {@code spawnClerics}: the four corners of the altar, ±2 cells. */
    public static final int ALTAR_CLERIC_OFFSET = 2;
    /** Original {@code spawnClerics}: more than 2 successes arms the guard waves. */
    public static final int ALTAR_CLERIC_SUCCESS = 3;
    /** Original {@code spawnGuards}: below 8 cultists in the 24×16×24 box. */
    public static final int ALTAR_GUARD_CAP = 8;
    /** The 24×16×24 census box of {@code spawnGuards} (half-extents). */
    public static final int ALTAR_GUARD_BOX_XZ = 24;
    public static final int ALTAR_GUARD_BOX_Y = 16;

    /** Original {@code TileEldritchObelisk}: every 20 ticks, 6-cell radius. */
    public static final int OBELISK_TICK_PERIOD = 20;
    public static final double OBELISK_RANGE = 6.0;
    /** Original slowness + weakness: 40 ticks, amplifier 0, ambient. */
    public static final int OBELISK_EFFECT_TICKS = 40;
    public static final int OBELISK_EFFECT_AMPLIFIER = 0;

    /** Original {@code TileEldritchLock}: the tablet starts the 100-tick fuse. */
    public static final int LOCK_ARMED_COUNT = 0;
    public static final int LOCK_BOSS_DELAY_TICKS = 100;
    /** Original lock sound every 5 ticks while the fuse runs. */
    public static final int LOCK_SOUND_PERIOD_TICKS = 5;

    /** Original {@code TileEldritchCrabSpawner}: counts, cap and cadence. */
    public static final int CRAB_START_COUNT = 150;
    public static final int CRAB_FIZZ_COUNT = 15;
    public static final int CRAB_REFILL_MIN = 50;
    public static final int CRAB_REFILL_SPAN = 50;
    public static final int CRAB_RESET_MIN = 150;
    public static final int CRAB_RESET_SPAN = 100;
    /** Original {@code maxEntitiesReached}: more than 5 crabs in the 32-box. */
    /** The spawner's crab census box half-width (the original's +-16 box). */
    public static final int CRAB_BOX = 16;

    public static final int CRAB_ENTITY_CAP = 5;
    /** Original {@code isActivated}: a player within 16 cells. */
    public static final double CRAB_PLAYER_RANGE = 16.0;

    /** Original {@code TileEldritchTrap}: 20 ticks, then 10 + rand(25). */
    /** The trap's per-trap damage cooldown (500 ticks). */
    public static final int TRAP_COOLDOWN_TICKS = 500;

    public static final int TRAP_START_COUNT = 20;
    public static final int TRAP_RESET_MIN = 10;
    public static final int TRAP_RESET_SPAN = 25;
    /** Original trap: nearest player within 3 cells, 2 magic damage. */
    public static final double TRAP_PLAYER_RANGE = 3.0;
    public static final float TRAP_DAMAGE = 2.0F;
    /** Original trap: 50 % chance of 1 + rand(2) warp. */
    public static final int TRAP_WARP_MIN = 1;
    public static final int TRAP_WARP_SPAN = 2;

    /** Original {@code BlockEldritchNothing}: 8 void damage, age gate 20 ticks. */
    /** BlockEldritchNothing material: hardness 0.2, resistance 6,000,000. */
    public static final float NOTHING_HARDNESS = 0.2F;
    public static final float NOTHING_RESISTANCE = 6_000_000.0F;

    public static final float NOTHING_DAMAGE = 8.0F;
    public static final int NOTHING_AGE_GATE_TICKS = 20;
    /** Original exposure toggle: meta 0 ↔ 1 (1 carries the tile). */
    public static final int NOTHING_VARIANT_HIDDEN = 0;
    public static final int NOTHING_VARIANT_EXPOSED = 1;
    public static final int NOTHING_VARIANT_COUNT = 2;

    // ------------------------------------------------------------- fluids

    /**
     * Original {@code BlockFluidPure.onEntityCollidedWithBlock}: the source
     * block grants Warp Ward for {@code min(32000, 200000 / sqrt(warp))}
     * ticks (divisor floored at 1). Pure function of the player's permanent
     * warp, so the unit test pins the curve exactly.
     */
    public static int warpWardDuration(final int warpPerm) {
        int div = 1;
        if (warpPerm > 0) {
            div = (int) Math.sqrt(warpPerm);
            if (div < 1) {
                div = 1;
            }
        }
        return Math.min(32000, 200000 / div);
    }

    /**
     * Original {@code BlockFluidDeath.onEntityCollidedWithBlock}:
     * {@code DamageSourceThaumcraft.dissolve} for {@code meta + 1} damage —
     * the depth of the fluid column, 1 at the source down to 1..4 with the
     * original's four quanta.
     */
    public static float dissolveDamage(final int depth) {
        return depth + 1.0F;
    }

    /** Original {@code BlockFluidDeath.setQuantaPerBlock(4)}. */
    /** BlockFluidDeath: 2.0 dissolve damage per entity contact. */
    public static final float DEATH_FLUID_DAMAGE = 2.0F;

    public static final int DEATH_QUANTA = 4;

    // ------------------------------------------------- ItemEldritchObject

    /** Original metas 0-4: eye, crimson rites, runed tablet, pearl, placer. */
    /** ItemEldritchObject: the original's {@code setMaxDamage(0)} — no durability bar. */
    public static final int ELDRITCH_OBJECT_DURABILITY = 0;

    public static final int OBJECT_COUNT = 5;
    public static final int OBJECT_EYE = 0;
    public static final int OBJECT_CRIMSON = 1;
    public static final int OBJECT_TABLET = 2;
    public static final int OBJECT_PEARL = 3;
    public static final int OBJECT_PLACER = 4;

    /**
     * Original {@code ItemEldritchObject.onItemUseFirst} (placer, meta 4, top
     * face): the six cells above the clicked face must be empty, then the
     * column is altar at y+1, obelisk at y+3 and obelisk tops at y+4/5/6/7.
     */
    public static final int PLACER_CLEAR_HEIGHT = 6;
    public static final int[] PLACER_COLUMN_Y = {1, 3, 4, 5, 6, 7};
    public static final int[] PLACER_COLUMN_VARIANT = {ELDRITCH_ALTAR, ELDRITCH_OBELISK,
            ELDRITCH_OBELISK_TOP, ELDRITCH_OBELISK_TOP, ELDRITCH_OBELISK_TOP, ELDRITCH_OBELISK_TOP};

    private EldritchRealmValues() {
    }
}
