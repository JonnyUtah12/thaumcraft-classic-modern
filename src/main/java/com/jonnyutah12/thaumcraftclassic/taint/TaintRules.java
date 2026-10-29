package com.jonnyutah12.thaumcraftclassic.taint;

/**
 * TC-M5-04-s: the taint spread / conversion / cleansing rule table, as pure
 * arithmetic (content-mapping.csv row {@code worldgen.taint_spread_conversion},
 * impl=TaintManager).
 *
 * <p>Every threshold below is the original 1.7.10 number, transcribed from the
 * decompiled sources and cited at its constant. The rule needs no world: the
 * caller (in {@link TaintManager}) reads the world facts — the adjacent-taint
 * count, the biome of a column, the target's category — and hands them in as
 * arguments. That split is what makes the thresholds unit-testable
 * (docs/testing.md: the unit suite has the Minecraft classpath for objects but
 * NO loaded tags, so a rule that reads a tag can only be proven in a
 * gametest), and it is what keeps the block classes thin.</p>
 *
 * <p>{@code TaintRulesParityTest} parses the original
 * {@code BlockTaint.java} / {@code BlockTaintFibres.java} /
 * {@code BlockFluxGoo.java} out of the local reference tree and asserts each
 * constant against the number written in the original source, so a threshold
 * cannot drift without a test failure.</p>
 */
public final class TaintRules {

    // --- conversion table (BlockTaintFibres.java:158-171) -------------------

    /** {@code adjacentTaint >= 2} for a crust target (log / gourd / cactus). */
    public static final int CRUST_ADJACENT_MIN = 2;
    /** {@code adjacentTaint >= 3} for a soil target (sand / ground / grass / clay). */
    public static final int SOIL_ADJACENT_MIN = 3;
    /** The original's {@code bi != Blocks.air} guard on the soil rule. */
    public static final boolean SOIL_TARGET_MUST_NOT_BE_AIR = true;

    // --- fibre meta ladder (BlockTaintFibres.java:196-208) ------------------

    /** {@code world.rand.nextInt(10) != 0} → plain fibres. */
    public static final int FIBRES_PLAIN_MOD = 10;
    /** {@code world.rand.nextInt(10) < 9} → tainted grass 1 (90% of the rest). */
    public static final int FIBRES_GRASS1_MOD = 10;
    public static final int FIBRES_GRASS1_LIMIT = 9;
    /** {@code world.rand.nextInt(12) < 10} → tainted grass 2. */
    public static final int FIBRES_GRASS2_MOD = 12;
    public static final int FIBRES_GRASS2_LIMIT = 10;
    /** The ladder's last rung: a spore stalk. */
    public static final int FIBRES_SPORE_STALK_META = 3;

    // --- biome spread (BlockTaintFibres.java:222-233) ----------------------

    /** {@code rand.nextInt(Config.taintSpreadRate * 5) == 0}. */
    public static final int BIOME_SPREAD_RATE_MULTIPLIER = 5;
    /** {@code getAdjacentTaint(...) >= 2} in the same rule. */
    public static final int BIOME_SPREAD_ADJACENT_MIN = 2;

    // --- the tick scan box (BlockTaint.java:157-159, BlockTaintFibres.java:153-155) ---

    /** {@code x + rand.nextInt(3) - 1} / {@code z + rand.nextInt(3) - 1}. */
    public static final int SCAN_XZ_MOD = 3;
    /** {@code y + rand.nextInt(5) - 3}: the scan reaches 3 below and 1 above. */
    public static final int SCAN_Y_MOD = 5;
    public static final int SCAN_Y_SHIFT = 3;

    // --- decay outside the taint biome (BlockTaint.java:192-197) -----------

    /** Crust → flux goo: {@code rand.nextInt(20) == 0}. */
    public static final int CRUST_DECAY_MOD = 20;
    /** Soil → dirt: {@code rand.nextInt(10) == 0}. */
    public static final int SOIL_DECAY_MOD = 10;

    // --- spore content, guarded on TC-M6-06's entities ---------------------

    /** Crust → spore swarmer: {@code random.nextInt(200) == 0} (BlockTaint.java:171). */
    public static final int CRUST_SWARMER_MOD = 200;
    /** Swarmer scan box: {@code AABB(x,y,z,x+1,y+1,z+1).expand(16,16,16)} (BlockTaint.java:173-176). */
    public static final double SWARMER_SCAN_INFLATE = 16.0D;
    /** Spore stalk → mature stalk: {@code random.nextInt(10) == 0} (BlockTaintFibres.java:172). */
    public static final int SPORE_STALK_MOD = 10;

    // --- step / collision poison (BlockTaint.java:257-266, BlockTaintFibres.java:262-270) ---

    /** Player on crust/soil: {@code world.rand.nextInt(100) == 0}. */
    public static final int STEP_PLAYER_CRUST_MOD = 100;
    /** Other living entity on crust/soil: {@code nextInt(20) == 0}. */
    public static final int STEP_MOB_CRUST_MOD = 20;
    /** Player on fibres: {@code nextInt(1000) == 0}. */
    public static final int STEP_PLAYER_FIBRES_MOD = 1000;
    /** Other living entity on fibres: {@code nextInt(500) == 0}. */
    public static final int STEP_MOB_FIBRES_MOD = 500;
    /** {@code new PotionEffect(potionTaintPoisonID, 80, 0, false)} for a player. */
    public static final int STEP_PLAYER_DURATION_TICKS = 80;
    /** 160 ticks for every other living entity. */
    public static final int STEP_MOB_DURATION_TICKS = 160;

    // --- flux goo → taint (BlockFluxGoo.java:97-108), TC-M5-03's seam -------

    /** The branch is gated on {@code meta >= 6}. */
    public static final int FLUX_TAINT_MIN_QUANTA = 6;
    /** {@code Config.taintFromFlux && rand.nextInt(50) == 0}. */
    public static final int FLUX_TAINT_MOD = 50;
    /** The thaumic-slime branch that takes precedence: {@code rand.nextInt(25) == 0} (meta>=6). */
    public static final int FLUX_SLIME_LARGE_MOD = 25;

    // --- falling taint (BlockTaint.java:211-235) ---------------------------

    /** {@code l == blockFluxGoo && md >= 4} blocks a fall. */
    public static final int FALL_BLOCKING_GOO_QUANTA = 4;
    /** The log scan is the 3x3x3 box around the landing spot. */
    public static final int FALL_LOG_SCAN_RADIUS = 1;
    /**
     * {@code y2 >= 0} in the original — in 1.7.10 y=0 IS the floor of the
     * world (bedrock), so the guard exists to stop a falling crust from leaving
     * it. 1.21.1's floor is the level's build limit, so the port compares
     * against that: the guard's meaning ("the target cell stays inside the
     * world") is what carries over, not the literal 0, which in a 1.21.1
     * overworld (-64..320) would disable the fall for the bottom 64 layers.
     * Deviation, recorded in docs/architecture/taint.md (BD-14): in a
     * 1.7.10-shaped world (min build height 0) the port refuses a fall into
     * the bottom layer (y=0) where the original allowed it — one cell, bedrock.
     */
    public static final int FALL_MIN_Y = 0;

    /**
     * The fall-height guard above, evaluated against a real level: the target
     * cell must be inside the world, i.e. strictly above its bottom layer.
     * {@code minBuildHeight} is what 1.7.10's literal 0 meant there (y=0 was
     * the bottom layer), so the number does not carry over — the check does.
     */
    public static boolean canFallAtHeight(final int targetY, final int minBuildHeight) {
        return targetY > minBuildHeight;
    }

    // --- bottled taint impact (EntityBottleTaint.java:36-58), TC-M6-06's seam ---

    /** Ten scatter offsets per impact. */
    public static final int BOTTLE_SPILL_ATTEMPTS = 10;
    /** {@code (rand.nextFloat() - rand.nextFloat()) * 5.0F} per axis. */
    public static final float BOTTLE_SPILL_SPREAD = 5.0F;
    /** Splash entity box: the impact AABB inflated by 5. */
    public static final double BOTTLE_SPLASH_INFLATE = 5.0D;
    /** {@code applyTaintPoison(el, 100)} on every non-tainted mob in the splash. */
    public static final int BOTTLE_POISON_DURATION_TICKS = 100;

    // --- cleansing: the Ethereal Bloom (TileEtherealBloom.java:11-46) -------
    // TC-M5-04-s-b. The original's bloom is a TileEntity that updates every
    // game tick; its counter is seeded once and the cleansing roll runs on
    // `counter % 20 == 0`. The scan offset is `nextInt(8) - nextInt(8)` on each
    // horizontal axis (so -7..7, i.e. a 15×15 box), and the target column must
    // lie inside `getDistanceTo(...) <= 81.0` — TileEntity#getDistanceTo is the
    // SQUARED distance, so the gate is a radius of 9, not 81.

    /** {@code counter = world.rand.nextInt(100)} when the counter is 0. */
    public static final int BLOOM_PHASE_MOD = 100;
    /** The cleansing roll runs on {@code counter % 20 == 0}. */
    public static final int BLOOM_PERIOD = 20;
    /** {@code rand.nextInt(8) - rand.nextInt(8)} per horizontal axis. */
    public static final int BLOOM_SCAN_MOD = 8;
    /** {@code getDistance(x + tx + 0.5, ty, z + tz + 0.5) <= 81.0} (squared). */
    public static final double BLOOM_RADIUS_SQUARED = 81.0D;
    /** The bloom's client roots cue: {@code playSound("thaumcraft:roots", 1.0F, 0.6F)}. */
    public static final float BLOOM_ROOTS_VOLUME = 1.0F;
    public static final float BLOOM_ROOTS_PITCH = 0.6F;

    // --- flux → taint thresholds, the rest of BlockFluxGoo.updateTick -------
    // TC-M5-04-s-b pins the whole goo tick so the thresholds exist in one place
    // for the flux row (TC-M5-03) and the slime entity (TC-M6-06) to call.

    /** The thaumic-slime branch below the taint gate: {@code meta >= 2 && meta < 6}. */
    public static final int FLUX_SLIME_SMALL_MIN_QUANTA = 2;
    public static final int FLUX_SLIME_SMALL_MAX_QUANTA = 5;
    /** Size-1 slime: {@code rand.nextInt(25) == 0}. */
    public static final int FLUX_SLIME_SMALL_MOD = 25;
    /** Goo decay: {@code rand.nextInt(30) == 0} → one quanta less (meta 0 → removed). */
    public static final int FLUX_DECAY_MOD = 30;
    /** A decayed goo column vents gas above on {@code rand.nextBoolean()}. */
    public static final int FLUX_GAS_ABOVE_CHANCES = 2;

    // --- aura: taint's contribution to the biome aura (TC-M5-04-s-b) --------

    /**
     * {@code baura = (int)(baura * 1.5F)} for a non-PURE node generated in a
     * taint biome, and ×1.5 again when the coin flip makes it TAINTED
     * (ThaumcraftWorldGenerator.java:272-279).
     */
    public static final float TAINT_AURA_MULTIPLIER = 1.5F;
    /**
     * {@code TileNode.handleTaintNode}: the taint biome repainted on
     * {@code count % 50 == 0} in a {@code nextInt(8) - nextInt(8)} box; a
     * non-PURE node standing in a taint biome flips to TAINTED on
     * {@code nextInt(500) == 0} every {@code count % 100 == 0}
     * (TileNode.java:773-801).
     */
    public static final int NODE_TAINT_PAINT_PERIOD = 50;
    public static final int NODE_STABILITY_PERIOD = 100;
    public static final int NODE_TAINT_SCAN_MOD = 8;
    public static final int NODE_TAINT_FLIP_MOD = 500;

    // --- sounds and client FX (original volumes, verbatim) ------------------

    /** {@code playSoundEffect(..., "thaumcraft:roots", 0.1F, 0.9F + rand*0.2F)}. */
    public static final float ROOTS_VOLUME = 0.1F;
    public static final float ROOTS_PITCH_BASE = 0.9F;
    public static final float ROOTS_PITCH_SPREAD = 0.2F;
    /** The crust droplet FX: {@code random.nextInt(10) == 0} (BlockTaint.java:277-283). */
    public static final int DROPLET_FX_MOD = 10;

    private TaintRules() {
    }

    // ---------------------------------------------------------------- rules

    /**
     * The fibre meta ladder, verbatim: the cell needs a solid neighbour, must
     * not sit only against taint, must not be liquid, and must be air,
     * replaceable, a flower, or leaves (original lines 191-195).
     */
    public static boolean canSeedFibres(final boolean adjacentToSolid, final boolean onlyAdjacentToTaint,
                                        final boolean liquid, final boolean air,
                                        final boolean canBeReplaced, final boolean flower, final boolean leaves) {
        return adjacentToSolid && !onlyAdjacentToTaint && !liquid
                && (air || canBeReplaced || flower || leaves);
    }

    /**
     * The meta the original writes for the seeded cell. {@code r0} is
     * {@code nextInt(FIBRES_PLAIN_MOD)}, {@code r1}
     * {@code nextInt(FIBRES_GRASS1_MOD)} and {@code r2}
     * {@code nextInt(FIBRES_GRASS2_MOD)} — the original draws them in this
     * order and only draws the later ones when the earlier rung missed.
     */
    public static int fibresMeta(final int r0, final boolean airAbove, final boolean solidBelow,
                                 final int r1, final int r2) {
        if (r0 != 0 || !airAbove || !solidBelow) {
            return 0;
        }
        if (r1 < FIBRES_GRASS1_LIMIT) {
            return 1;
        }
        if (r2 < FIBRES_GRASS2_LIMIT) {
            return 2;
        }
        return FIBRES_SPORE_STALK_META;
    }

    /** {@code adjacentTaint >= 2} plus the crust category (log / gourd / cactus). */
    public static boolean convertsToCrust(final int adjacentTaint, final boolean crustTarget) {
        return adjacentTaint >= CRUST_ADJACENT_MIN && crustTarget;
    }

    /** {@code adjacentTaint >= 3} plus non-air plus the soil category. */
    public static boolean convertsToSoil(final int adjacentTaint, final boolean targetIsAir,
                                         final boolean soilTarget) {
        return adjacentTaint >= SOIL_ADJACENT_MIN && !(SOIL_TARGET_MUST_NOT_BE_AIR && targetIsAir)
                && soilTarget;
    }

    /** The roll modulus of the biome-spread rule for a config rate. */
    public static int biomeSpreadModulus(final int spreadRate) {
        return spreadRate * BIOME_SPREAD_RATE_MULTIPLIER;
    }

    /**
     * The biome-spread rule: the target column must not already be taint, the
     * {@code nextInt(rate*5)} roll must hit, and the source cell needs
     * {@code >= 2} adjacent taint. A rate of 0 disables the rule entirely
     * (the original's {@code if (Config.taintSpreadRate > 0)}).
     */
    public static boolean spreadsBiome(final int spreadRate, final boolean targetIsTaint,
                                       final int rateRoll, final int adjacentTaint) {
        return spreadRate > 0 && !targetIsTaint && rateRoll == 0
                && adjacentTaint >= BIOME_SPREAD_ADJACENT_MIN;
    }

    /** Crust outside the taint biome decays to flux goo on {@code nextInt(20) == 0}. */
    public static boolean crustDecaysOutsideBiome(final int roll) {
        return roll == 0;
    }

    /** Soil outside the taint biome decays to dirt on {@code nextInt(10) == 0}. */
    public static boolean soilDecaysOutsideBiome(final int roll) {
        return roll == 0;
    }

    /** The step-poison odds for the surface walked on. */
    public static int stepOdds(final boolean player, final boolean fibres) {
        if (fibres) {
            return player ? STEP_PLAYER_FIBRES_MOD : STEP_MOB_FIBRES_MOD;
        }
        return player ? STEP_PLAYER_CRUST_MOD : STEP_MOB_CRUST_MOD;
    }

    /** The step-poison duration: 80 ticks for a player, 160 for anything else. */
    public static int stepDuration(final boolean player) {
        return player ? STEP_PLAYER_DURATION_TICKS : STEP_MOB_DURATION_TICKS;
    }

    /** One axis of the tick scan box: {@code rand.nextInt(mod) - shift}. */
    public static int scanDelta(final int roll, final int mod, final int shift) {
        return roll - shift;
    }

    /** {@code rand.nextInt(CRUST_SWARMER_MOD) == 0} with air above the crust. */
    public static boolean spawnsSporeSwarmer(final int roll, final boolean airAbove, final boolean configOn) {
        return configOn && airAbove && roll == 0;
    }

    /** {@code md == 3 && rand.nextInt(SPORE_STALK_MOD) == 0 && air above}. */
    public static boolean maturesSporeStalk(final int meta, final int roll, final boolean airAbove,
                                            final boolean configOn) {
        return meta == FIBRES_SPORE_STALK_META && configOn && roll == 0 && airAbove;
    }

    /** A mature stalk reverts once no spore entity is left above it. */
    public static boolean revertsMatureStalk(final int meta, final int sporeEntitiesAbove) {
        return meta == 4 && sporeEntitiesAbove <= 0;
    }

    /** The flux-goo → taint branch: quanta, air above, config, and the 1/50 roll. */
    public static boolean fluxGooConvertsToTaint(final int quanta, final boolean airAbove,
                                                 final int roll, final boolean configOn) {
        return quanta >= FLUX_TAINT_MIN_QUANTA && airAbove && configOn && roll == 0;
    }

    /** A goo column of quanta >= 4 is solid enough to stop falling crust. */
    public static boolean gooBlocksFall(final int quanta) {
        return quanta >= FALL_BLOCKING_GOO_QUANTA;
    }

    // ---------------------------------------------------------- cleansing rules

    /**
     * Does the bloom's update run its cleansing roll this tick? The original
     * seeds {@code counter} with {@code nextInt(100)} when it reads 0, then
     * increments, then tests {@code counter % 20 == 0} — so the phase is the
     * seed, and the roll is one draw per {@link #BLOOM_PERIOD} ticks.
     */
    public static boolean bloomCleansesThisTick(final long counterAfterIncrement) {
        return counterAfterIncrement % BLOOM_PERIOD == 0;
    }

    /**
     * The bloom's gate (TileEtherealBloom.java:26-31): the column it picked
     * must be a biome taint can occupy — the original lists the Tainted Land,
     * Eerie and Magical Forest ids — and must sit inside the radius. The
     * distance is the original's squared {@code TileEntity#getDistanceTo}, so
     * the caller passes the square and the gate is {@link #BLOOM_RADIUS_SQUARED}.
     */
    public static boolean bloomCleanses(final boolean targetIsCleansableBiome,
                                        final double distanceSquared) {
        return targetIsCleansableBiome && distanceSquared <= BLOOM_RADIUS_SQUARED;
    }

    /**
     * One axis of the bloom's scan offset: {@code rand.nextInt(8) -
     * rand.nextInt(8)}, i.e. -7..7, so the bloom reaches a 15×15 box around
     * itself and the radius gate trims it to a disc.
     */
    public static int bloomOffset(final int rollA, final int rollB) {
        return rollA - rollB;
    }

    /**
     * The distance term of the bloom's radius gate, transcribed from the
     * original's call
     * {@code getDistance(x + tx + 0.5, y, z + tz + 0.5)} — the {@code double}
     * overload of {@code TileEntity#getDistance}, which measures from the
     * tile's own centre {@code (x + 0.5, y + 0.5, z + 0.5)} and returns the
     * SQUARE of the distance. The deltas are therefore {@code (-tx, 0.5, -tz)}
     * (the y delta is 0.5 because the original passes the tile's own {@code y}
     * un-offset), so:
     *
     * <pre>d² = tx² + 0.5² + tz²</pre>
     *
     * <p>With the ±7 scan box that admits offsets up to |tx| = 8 (the box
     * tops out at 7), trims the corners ((7,7) → 98.25 &gt; 81), and makes the
     * cleansed area a disc of radius ~9 — not the 81-block radius the literal
     * constant suggests, and not a square. Feeds {@link #bloomCleanses}.</p>
     */
    public static double bloomDistanceSquared(final int tx, final int tz) {
        return (double) tx * tx + 0.25D + (double) tz * tz;
    }

    /**
     * The biome the cleansed column goes back to (TileEtherealBloom.java:32-41).
     * The original asks the <em>chunk manager</em> — {@code
     * WorldChunkManager#getBiomesToGenerateAt} — for the biome that GENERATES
     * at that column, i.e. the biome the world would have placed there, and
     * substitutes plains when even the generator says taint. So "restore the
     * original biome" is not a stored record: it is the generator's own answer,
     * and the plains write is only the fallback for a taint-plumbed world.
     *
     * @param generatedIsTaint whether the generator's biome is the taint biome
     * @return true — write plains; false — write the generated biome itself
     */
    public static boolean cleansingFallsBackToPlains(final boolean generatedIsTaint) {
        return generatedIsTaint;
    }

    /**
     * The bloom only rewrites a column whose stored biome is one of the three
     * thaumic biomes it lists (see {@link #bloomCleanses}); a column that holds
     * any other biome is left exactly as it is.
     */
    public static boolean cleansingSkipsColumn(final boolean storedIsCleansableBiome) {
        return !storedIsCleansableBiome;
    }

    // -------------------------------------------------- flux goo tick (the rest)

    /**
     * The thaumic-slime branch under the taint gate: {@code meta >= 2 && meta <
     * 6 && air above && rand.nextInt(25) == 0} spawns a size-1 slime
     * (BlockFluxGoo.java:88-96). The slime itself is TC-M6-06's row; the
     * threshold is pinned here.
     */
    public static boolean fluxGooSpawnsSmallSlime(final int quanta, final boolean airAbove,
                                                  final int roll) {
        return quanta >= FLUX_SLIME_SMALL_MIN_QUANTA && quanta <= FLUX_SLIME_SMALL_MAX_QUANTA
                && airAbove && roll == 0;
    }

    /** The size-2 slime above the taint gate: {@code meta >= 6 && nextInt(25) == 0}. */
    public static boolean fluxGooSpawnsLargeSlime(final int quanta, final boolean airAbove,
                                                  final int roll) {
        return quanta >= FLUX_TAINT_MIN_QUANTA && airAbove && roll == 0;
    }

    /**
     * The goo's own decay: {@code rand.nextInt(30) == 0} removes the last
     * quanta, and a surviving column vents flux gas above on a coin flip
     * (BlockFluxGoo.java:110-118).
     */
    public static boolean fluxGooDecays(final int roll) {
        return roll == 0;
    }

    // ------------------------------------------------- aura / vis interaction

    /**
     * The biome-aura value the node generator uses in a taint biome: {@code
     * (int)(baura * 1.5F)} for a non-PURE node, and ×1.5 again once the coin
     * flip has made the node TAINTED (ThaumcraftWorldGenerator.java:272-279).
     * The float cast truncates, exactly as the original's {@code (int)} does:
     * aura 100 → 150, 150 → 225 (not 225.5 rounded).
     */
    public static int taintBiomeAura(final int biomeAura) {
        return (int) (biomeAura * TAINT_AURA_MULTIPLIER);
    }

    /**
     * The node rule that spreads the taint biome: {@code count % 50 == 0} and a
     * ±7 scan box (TileNode.java:775-783). Gated on the taint biome being
     * registered: {@code BiomeAura.hasTaintBiome()} is the port's stand-in for
     * the original's identity compare against {@code biomeTaint}, which was
     * never null in a world that had Thaumcraft loaded.
     */
    public static boolean nodePaintsTaintBiome(final long count, final boolean taintBiomeRegistered) {
        return taintBiomeRegistered && count % NODE_TAINT_PAINT_PERIOD == 0;
    }

    /**
     * The node rule that takes taint over: a non-PURE node standing in a taint
     * biome flips to TAINTED on {@code nextInt(500) == 0}, checked every
     * {@code count % 100 == 0} (TileNode.java:792-798).
     */
    public static boolean nodeBecomesTainted(final long count, final boolean pure,
                                             final boolean taintBiomeRegistered,
                                             final boolean standingInTaintBiome, final int roll) {
        return !pure && taintBiomeRegistered && standingInTaintBiome
                && count % NODE_STABILITY_PERIOD == 0 && roll == 0;
    }

    /**
     * One axis of the taint node's scan offset: {@code rand.nextInt(8) -
     * rand.nextInt(8)} (TileNode.java:778-779), i.e. -7..7. The caller draws
     * the two rolls in the original's order (x pair, then z pair) so a seeded
     * world reproduces the original's sequence.
     */
    public static int nodeTaintScanOffset(final int rollA, final int rollB) {
        return rollA - rollB;
    }
}
