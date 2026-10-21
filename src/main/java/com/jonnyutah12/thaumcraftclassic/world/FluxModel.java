package com.jonnyutah12.thaumcraftclassic.world;

/**
 * Pure flux (vis-pollution) math for TC-M5-03 (design record:
 * docs/architecture/flux.md). No Minecraft types — unit-testable without the
 * game classpath, per docs/testing.md.
 *
 * <p><b>Parity finding.</b> Thaumcraft 4.2.3.5 has no per-chunk flux map and
 * no flux event handlers in {@code common/lib/events} (verified by class scan
 * of the original jar: no {@code WorldMagicData}/{@code FluxAge}/{@code addFlux}
 * exist). Flux in the original is block state: {@code BlockFluxGoo} (ground)
 * and {@code BlockFluxGas} (air) with the block metadata used as a flux level
 * 0..8 ({@code getQuanta()==8}, {@code BlockFluxGoo.java:47-49}; meta-8
 * placements at {@code BlockAiry.java:347-349}, {@code BlockTaint.java:188-193}).
 * The per-chunk {@code fluxAir}/{@code fluxGround} scalars in
 * {@link WorldMagicData} (TC-M2-02) are the modern equivalent the milestone
 * plan needs; this class defines their semantics, with every constant derived
 * from the original's block-level rules (TC-M0-06, docs/balance-values.md §8)
 * or explicitly marked as a port design constant.</p>
 *
 * <p><b>Unit.</b> Flux quanta per chunk, air and ground tracked separately —
 * the chunk-scale analogue of the original's goo/gas split. A flux block of
 * metadata m contributes {@code max(1, m)} quanta (a meta-0 placement is a
 * trace of flux but still flux; meta 7/8 = a full column = 8 quanta). A
 * "column" holds up to {@link #COLUMN_CAP} quanta.</p>
 */
public final class FluxModel {

    private FluxModel() {
    }

    /** Deterministic random seam so unit tests pin every probabilistic rule. */
    public interface Rand {
        /** Uniform int in [0, bound) — mirrors {@code Random.nextInt(int)}. */
        int nextInt(int bound);

        /** Coin flip — mirrors {@code Random.nextBoolean()}. */
        boolean nextBoolean();
    }

    /** java.util.Random adapter. */
    public static Rand randOf(final java.util.Random random) {
        return new Rand() {
            @Override
            public int nextInt(final int bound) {
                return random.nextInt(bound);
            }

            @Override
            public boolean nextBoolean() {
                return random.nextBoolean();
            }
        };
    }

    // ------------------------------------------------------------------ scale

    /**
     * Quanta a single flux column can hold: the original's fluid quantum count
     * ({@code BlockFluidFinite.quantaPerBlock == 8}, surfaced as
     * {@code BlockFluxGoo.getQuanta()}, {@code BlockFluxGoo.java:47-49}).
     */
    public static final int COLUMN_CAP = 8;

    /** Highest flux level the original's effect thresholds ever test (meta 8). */
    public static final int MAX_LEVEL = 8;

    /** Quanta contributed by an original flux-block placement of metadata m. */
    public static int quantaOfMeta(final int meta) {
        return Math.max(1, meta);
    }

    /** Representative block level for effect thresholds: capped at meta 8. */
    public static int level(final int flux) {
        return Math.min(MAX_LEVEL, Math.max(0, flux));
    }

    /** Number of flux columns a chunk's worth of quanta represents. */
    public static int columns(final int flux) {
        return (Math.max(0, flux) + COLUMN_CAP - 1) / COLUMN_CAP;
    }

    // ------------------------------------------------------------- generation

    /** Which medium a flux event pollutes (goo = ground, gas = air). */
    public enum Medium {
        AIR,
        GROUND,
        /** Medium chosen by the event's own rule (coin flip / vertical side). */
        CONTEXT
    }

    /**
     * Magic-use event kinds. The amounts are the quanta of flux block the
     * original places at each site; the call-site probability gates (e.g. the
     * crucible's 1/4 spill roll) stay with the caller so the model stays pure.
     */
    public enum FluxSource {
        /**
         * Port design constant — TC 4.2.3.5 generates no flux from wand
         * casting (verified: no flux references in ItemWandCasting; flux sites
         * are device/tile/block events only). The milestone plan requires
         * casting to pollute, so the port adds 1 quantum per vis unit
         * consumed, mirroring the crucible's one-spill-one-quantum scale.
         * Medium: air (the "vis exhaust" the original's gas carries).
         */
        SPELL_CAST(Medium.AIR, 1),
        /**
         * Crucible spill: one meta-0 flux block, gas or goo 50/50
         * ({@code TileCrucible.java:196-204}); the 1/4-per-operation gate
         * ({@code TileCrucible.java:197}) stays at the call site.
         */
        CRUCIBLE_SPILL(Medium.CONTEXT, quantaOfMeta(0)),
        /**
         * Infusion crash ejects a meta-7 goo column over the pedestal for
         * failure types 1/3, gas for types 2/4
         * ({@code TileInfusionMatrix.java:639-642}).
         */
        INFUSION_FAILURE_GROUND(Medium.GROUND, quantaOfMeta(7)),
        INFUSION_FAILURE_AIR(Medium.AIR, quantaOfMeta(7)),
        /**
         * Reservoir overflow places meta-8 goo/gas
         * ({@code BlockEssentiaReservoir.java:99-101}).
         */
        RESERVOIR_OVERFLOW_GROUND(Medium.GROUND, quantaOfMeta(8)),
        RESERVOIR_OVERFLOW_AIR(Medium.AIR, quantaOfMeta(8)),
        /**
         * Node-converter / airy detonation scatters up to 50 meta-8 placements
         * in a ±8 box, goo below the origin and gas above
         * ({@code BlockAiry.explodify}, {@code BlockAiry.java:336-353}). The
         * per-placement amount is 8; the caller iterates placements (they
         * cross chunk borders) — see {@link #explosionPlacements()}.
         */
        NODE_CONVERTER_EXPLOSION(Medium.CONTEXT, quantaOfMeta(8)),
        /**
         * Taint blocks decay into a full goo column
         * ({@code BlockTaint.java:188-193}, meta = {@code getQuanta()}).
         */
        TAINT_DECAY(Medium.GROUND, quantaOfMeta(8)),
        /**
         * Thaumhia places a meta-0 goo block
         * ({@code PotionThaumarhia.java:44}).
         */
        THAUMARHIA(Medium.GROUND, quantaOfMeta(0));

        /**
         * Port design constant: the original's 50/50 gas/goo choice for
         * context-dependent sites ({@code TileCrucible.java:199-202}).
         */
        static final int COIN_FLIP_BOUND = 2;

        /** Explodify attempt count, original {@code BlockAiry.java:341}. */
        public static final int EXPLOSION_PLACEMENTS = 50;

        private final Medium medium;
        private final int quanta;

        FluxSource(final Medium medium, final int quanta) {
            this.medium = medium;
            this.quanta = quanta;
        }

        public Medium medium() {
            return medium;
        }

        public int quantaPerUnit() {
            return quanta;
        }
    }

    /** Flux added to a chunk's air/ground scalars by one event. */
    public record FluxDelta(int air, int ground) {
        public FluxDelta plus(final FluxDelta other) {
            return new FluxDelta(air + other.air, ground + other.ground);
        }
    }

    /**
     * Quanta added by one occurrence of {@code source}. {@code units} scales
     * per-unit sources (SPELL_CAST: vis consumed); other sources use 1.
     * CONTEXT sources resolve their medium with {@code rand} (the original's
     * coin flips); explosion attribution is per placement — call with units=1
     * once per placement position.
     */
    public static FluxDelta generation(final FluxSource source, final int units, final Rand rand) {
        final int amount = Math.max(0, units) * source.quantaPerUnit();
        return switch (source.medium()) {
            case AIR -> new FluxDelta(amount, 0);
            case GROUND -> new FluxDelta(0, amount);
            case CONTEXT -> rand.nextBoolean() ? new FluxDelta(amount, 0) : new FluxDelta(0, amount);
        };
    }

    /** Explodify placement count (the ±8 box and side rule stay with the caller). */
    public static int explosionPlacements() {
        return FluxSource.EXPLOSION_PLACEMENTS;
    }

    // ------------------------------------------------------------------- decay

    /**
     * Game ticks between flux-age passes (decay + diffusion), per chunk.
     * Derived from the original's per-column decay rate: a goo block loses one
     * metadata level with probability 1/30 per random tick
     * ({@code BlockFluxGoo.java:104-112}; TC-M0-06 §8), and 1.7.10 random-ticks
     * 3 blocks per 4096-block chunk section per game tick
     * ({@code randomTickSpeed == 3}), i.e. 3/4096 random ticks per block per
     * game tick. Expected game ticks per level lost = 30 × 4096 / 3 = 40,960.
     */
    public static final long DECAY_PASS_TICKS = 40_960L;

    /**
     * Ground/air flux after one age pass: every column loses one level
     * (the original decays each goo block independently, so the chunk-scale
     * rate is one level per column per pass). Clamped at 0.
     */
    public static int decayStep(final int flux) {
        return Math.max(0, flux - columns(flux));
    }

    /**
     * Air quanta emitted while ground flux decayed: each decay event has a 50%
     * chance to place flux gas above ({@code BlockFluxGoo.java:108-111}).
     * {@code decayedColumns} is the number of decay events that pass had.
     */
    public static int groundDecayAirEmission(final int decayedColumns, final Rand rand) {
        int emitted = 0;
        for (int i = 0; i < decayedColumns; i++) {
            if (rand.nextBoolean()) {
                emitted++;
            }
        }
        return emitted;
    }

    // -------------------------------------------------------------- diffusion

    /**
     * Quanta one chunk passes to each of its four cardinal neighbours per age
     * pass while it holds more than one full column. Port adaptation: the
     * original has no chunk-level diffusion — flux gas/goo spread by fluid
     * flow inside the world ({@code BlockFluidFinite} flow, densityDir) — this
     * is the chunk-scale analogue of that spreading.
     */
    public static int diffusionOut(final int flux) {
        return flux > COLUMN_CAP ? 1 : 0;
    }

    // ------------------------------------------------------- effect thresholds

    /** Thaumic slime spawn roll from a goo block ({@code BlockFluxGoo.java:88-99}). */
    public record SlimeSpawn(boolean occurred, int size) {
        public static final SlimeSpawn NONE = new SlimeSpawn(false, 0);
    }

    /**
     * meta 2..5: 1/25 → size-1 slime; meta ≥ 6: 1/25 → size-2 slime
     * ({@code BlockFluxGoo.java:88-99}; TC-M0-06 §8). Requires air above —
     * a block-level check the caller performs.
     */
    public static SlimeSpawn slimeSpawnRoll(final int level, final Rand rand) {
        if (level < 2) {
            return SlimeSpawn.NONE;
        }
        if (rand.nextInt(25) != 0) {
            return SlimeSpawn.NONE;
        }
        return new SlimeSpawn(true, level >= 6 ? 2 : 1);
    }

    /**
     * Taint conversion roll: meta ≥ 6 and {@code biome_taint_from_flux}
     * (default true, {@code Config.java:88,497-499}): 1/50 → biome→Taint +
     * taint fibres ({@code BlockFluxGoo.java:100-103}; TC-M0-06 §8). The
     * conversion itself is the taint milestone's action (TC-M5-04).
     */
    public static boolean taintConversionRoll(final int level, final boolean taintFromFlux, final Rand rand) {
        return level >= 6 && taintFromFlux && rand.nextInt(50) == 0;
    }

    /** A potion-effect application an effect rule produced. */
    public record EffectSpec(String effectId, int durationTicks, int amplifier, boolean noCuratives) {
        /** TC's Vis Exhaustion (registered by the potion milestone, TC-M6-05). */
        public static final String VIS_EXHAUST = "vis_exhaust";
        /** Vanilla nausea ({@code Potion.field_76431_k} in the original). */
        public static final String NAUSEA = "nausea";
    }

    /**
     * Flux gas contact: 1/10 per contact on a living, non-tainted,
     * non-affected entity → 50/50 Vis Exhaustion 1200 ticks (amplifier
     * meta/3, curatives cleared) or Nausea 80+meta×20 ticks; either way the
     * gas block loses one level ({@code BlockFluxGas.java:59-77}; TC-M0-06
     * §8). The caller performs the entity-state gates and the level
     * consumption.
     */
    public static EffectSpec gasContactRoll(final int level, final Rand rand) {
        if (rand.nextInt(10) != 0) {
            return null;
        }
        if (rand.nextBoolean()) {
            return new EffectSpec(EffectSpec.VIS_EXHAUST, 1200, level / 3, true);
        }
        return new EffectSpec(EffectSpec.NAUSEA, 80 + level * 20, 0, false);
    }

    /**
     * Flux goo contact: Vis Exhaustion 600 ticks, amplifier meta/3, curatives
     * cleared ({@code BlockFluxGoo.java:63-65}; TC-M0-06 §8). Applied on
     * every contact (no roll); the movement-slowdown factor is block-level.
     */
    public static EffectSpec gooContactEffect(final int level) {
        return new EffectSpec(EffectSpec.VIS_EXHAUST, 600, level / 3, true);
    }

    /**
     * Thaumic slime growth from standing in goo: grows by one size while
     * {@code slimeSize < meta}, consuming one level ({@code
     * BlockFluxGoo.java:55-61}). Returns true when the slime grew.
     */
    public static boolean slimeGrowthRoll(final int level, final int slimeSize, final Rand rand) {
        return slimeSize < level && rand.nextBoolean();
    }
}
