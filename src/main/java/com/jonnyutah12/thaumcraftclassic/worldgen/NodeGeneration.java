package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeModifier;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vis node generation (TC-M5-01). 1:1 port of the original
 * {@code ThaumcraftWorldGenerator.createRandomNodeAt} (L296-403),
 * {@code createNodeAt} (L405-418), {@code generateWildNodes} (L185-215) and
 * {@code Utils.getFirstUncoveredY} ({@code common/lib/utils/Utils.java:94-101}).
 *
 * <p>Split for testability: {@link #planNode} is the pure aspect/type/modifier
 * roll (no Minecraft types beyond a {@link Random}) and consumes randomness in
 * exactly the original's order; {@link #generateWildNodes} /
 * {@link #createRandomNodeAt} are the world-facing placement. The node record
 * is stored through {@code WorldMagicData} — the original's blockairy block +
 * {@code TileNode} materialisation is TC-M5-02's job. The original's
 * silverwood/eerie tree nodes, the scattered-feature (magic mushroom) node and
 * the per-structure nodes ride the tree/structure milestones; the
 * {@code createRandomNodeAt} flag signature is kept so those can call it
 * unchanged.</p>
 *
 * <p>Dimension blacklist parity: the original kept a per-dimension blacklist fed
 * by network messages (other mods opting out). {@link #addDimBlacklist} keeps
 * that API surface keyed by dimension key; the surface path skips a blacklisted
 * dimension (the original's {@code blacklist == 0 || == 2} gate), the Nether
 * path ignores it, exactly as {@code generateSurface}/{@code generateNether}
 * do.</p>
 *
 * <p>Deliberate deviations, both documented: the per-chunk RNG is seeded from
 * world seed + chunk coords (the original used the unsalted {@code world.rand},
 * making placement load-order dependent), and {@code nextInt} bounds are floored
 * at 1 where the original would throw on a degenerate config (e.g.
 * {@code node_rarity=0} or an aura below 2) — unreachable with real table
 * values.</p>
 */
public final class NodeGeneration {

    /** Aspect tags used by the original's type/environment merges (Aspect.* constants → tags). */
    static final String T_FAMES = "fames";      // Aspect.HUNGER
    static final String T_LUXRUM = "luxrum";    // Aspect.GREED
    static final String T_VICTUS = "victus";    // Aspect.LIFE
    static final String T_ORDO = "ordo";        // Aspect.ORDER
    static final String T_MORTUUS = "mortuus";  // Aspect.DEATH
    static final String T_EXANIMIS = "exanimis";// Aspect.UNDEAD
    static final String T_PERDITIO = "perditio";// Aspect.ENTROPY
    static final String T_TENEBRAE = "tenebrae";// Aspect.DARKNESS
    static final String T_AQUA = "aqua";        // Aspect.WATER
    static final String T_IGNIS = "ignis";      // Aspect.FIRE
    static final String T_TERRA = "terra";      // Aspect.EARTH
    static final String T_HERBA = "herba";      // Aspect.PLANT

    /**
     * Environment block counts around a node position — the original's 11×11×11
     * scan (L364-390). The merge thresholds are the original's: water&gt;100,
     * lava&gt;100, stone&gt;500, foliage&gt;100.
     */
    public record EnvCounts(int water, int lava, int stone, int foliage) {
        public static final EnvCounts NONE = new EnvCounts(0, 0, 0, 0);

        public boolean hasWater() {
            return water > 100;
        }

        public boolean hasLava() {
            return lava > 100;
        }

        public boolean hasStone() {
            return stone > 500;
        }

        public boolean hasFoliage() {
            return foliage > 100;
        }
    }

    /** The result of one pure node roll — what gets stored as a WorldMagicData.Node. */
    public record PlannedNode(NodeType type, NodeModifier modifier, int intensity, Map<String, Integer> aspects) {
    }

    private static final Set<ResourceKey<Level>> DIM_BLACKLIST = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceKey<Level>> OUTER_LANDS_DIMS = ConcurrentHashMap.newKeySet();

    private NodeGeneration() {
    }

    /** Original {@code ThaumcraftWorldGenerator.addDimBalcklist} (spelling in the original). */
    public static void addDimBlacklist(final ResourceKey<Level> dimension) {
        DIM_BLACKLIST.add(dimension);
    }

    public static boolean isDimBlacklisted(final ResourceKey<Level> dimension) {
        return DIM_BLACKLIST.contains(dimension);
    }

    /**
     * Dimensions that host the Outer Lands maze instead of nodes (original:
     * {@code dimensionId == Config.dimensionOuterId} → maze only). Populated by
     * the Outer Lands milestone (TC-M6-04f); empty until then.
     */
    public static void registerOuterLandsDimension(final ResourceKey<Level> dimension) {
        OUTER_LANDS_DIMS.add(dimension);
    }

    public static boolean isOuterLandsDimension(final ResourceKey<Level> dimension) {
        return OUTER_LANDS_DIMS.contains(dimension);
    }

    // ------------------------------------------------------------------
    // Pure core: the node roll
    // ------------------------------------------------------------------

    /**
     * Port of the body of {@code createRandomNodeAt} after the biome lookup,
     * consuming randomness in the original's exact order:
     *
     * <ol><li>type roll: silverwood→PURE / eerie→DARK, else 1-in-
     * {@code specialNodeRarity} then a 10-way pick (DARK 3/10, UNSTABLE 3/10,
     * PURE 3/10, HUNGRY 1/10);</li>
     * <li>modifier roll: 1-in-{@code specialNodeRarity/2} then 3-way
     * (BRIGHT/PALE/FADING);</li>
     * <li>taint: non-PURE nodes in a taint biome get baura ×1.5, then a coin
     * flip to TAINTED with another ×1.5;</li>
     * <li>silverwood/small: baura ÷4;</li>
     * <li>value = nextInt(baura/2) + baura/2;</li>
     * <li>biome tag pick (uniform over matched rows);</li>
     * <li>base aspects: biome tag ×2, else complex ×1 + basic ×1;</li>
     * <li>three coin flips, each optionally merging one pool aspect (complex on
     * a 1-in-{@code specialNodeRarity} roll, else basic);</li>
     * <li>type merges (HUNGRY: fames 2 + coin luxrum 1; PURE: coin victus 2 /
     * ordo 2; DARK: four independent coins mortuus/exanimis/perditio/tenebrae
     * ×1; TAINTED: none);</li>
     * <li>environment merges at the original thresholds (lava also merges
     * terra);</li>
     * <li>spread: tag-sorted aspects get 50+25r when holding exactly 2 else
     * 25+50r, then each merges its floor'd share of value.</li></ol>
     *
     * @param biomeAura raw {@code BiomeHandler.getBiomeAura} value
     * @param matched   {@code BiomeAura.matching} rows for the biome (the tag
     *                  pick consumes one draw here, as the original's
     *                  {@code getRandomBiomeTag} does)
     */
    public static PlannedNode planNode(
            final Random random,
            final int biomeAura,
            final List<BiomeAura.Entry> matched,
            final boolean inTaintBiome,
            final boolean silverwood,
            final boolean eerie,
            final boolean small,
            final int specialNodeRarity,
            final EnvCounts counts,
            final List<String> basicPool,
            final List<String> complexPool) {
        // 1. Type roll (original L296-330).
        NodeType type = NodeType.NORMAL;
        if (silverwood) {
            type = NodeType.PURE;
        } else if (eerie) {
            type = NodeType.DARK;
        } else if (random.nextInt(Math.max(1, specialNodeRarity)) == 0) {
            switch (random.nextInt(10)) {
                case 0:
                case 1:
                case 2:
                    type = NodeType.DARK;
                    break;
                case 3:
                case 4:
                case 5:
                    type = NodeType.UNSTABLE;
                    break;
                case 6:
                case 7:
                case 8:
                    type = NodeType.PURE;
                    break;
                default:
                    type = NodeType.HUNGRY;
            }
        }

        // 2. Modifier roll (original L332-346).
        NodeModifier modifier = null;
        if (random.nextInt(Math.max(1, specialNodeRarity / 2)) == 0) {
            switch (random.nextInt(3)) {
                case 0:
                    modifier = NodeModifier.BRIGHT;
                    break;
                case 1:
                    modifier = NodeModifier.PALE;
                    break;
                default:
                    modifier = NodeModifier.FADING;
            }
        }

        // 3. Taint multiplier (original L348-355) — the rule is
        // TaintRules.taintBiomeAura (the truncating (int)(baura * 1.5F)).
        int baura = biomeAura;
        if (type != NodeType.PURE && inTaintBiome) {
            baura = TaintRules.taintBiomeAura(baura);
            if (random.nextBoolean()) {
                type = NodeType.TAINTED;
                baura = TaintRules.taintBiomeAura(baura);
            }
        }

        // 4. silverwood/small divisor (original L357-359).
        if (silverwood || small) {
            baura /= 4;
        }

        // 5. Vis budget (original L361).
        final int value = random.nextInt(Math.max(1, baura / 2)) + baura / 2;

        // 6. Biome tag pick (original L362 / BiomeHandler.getRandomBiomeTag).
        final String biomeTag = BiomeAura.pickRandomTag(matched, random);

        // 7. Base aspects (original L363-371).
        final List<String> aspects = new ArrayList<>();
        final List<Integer> amounts = new ArrayList<>();
        if (biomeTag != null) {
            addAspect(aspects, amounts, biomeTag, 2);
        } else {
            addAspect(aspects, amounts, pickPool(complexPool, random), 1);
            addAspect(aspects, amounts, pickPool(basicPool, random), 1);
        }

        // 8. Three coin flips (original L373-385).
        for (int a = 0; a < 3; a++) {
            if (random.nextBoolean()) {
                if (random.nextInt(Math.max(1, specialNodeRarity)) == 0) {
                    addAspect(aspects, amounts, pickPool(complexPool, random), 1);
                } else {
                    addAspect(aspects, amounts, pickPool(basicPool, random), 1);
                }
            }
        }

        // 9. Type merges (original L387-410).
        switch (type) {
            case HUNGRY:
                addAspect(aspects, amounts, T_FAMES, 2);
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_LUXRUM, 1);
                }
                break;
            case PURE:
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_VICTUS, 2);
                } else {
                    addAspect(aspects, amounts, T_ORDO, 2);
                }
                break;
            case DARK:
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_MORTUUS, 1);
                }
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_EXANIMIS, 1);
                }
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_PERDITIO, 1);
                }
                if (random.nextBoolean()) {
                    addAspect(aspects, amounts, T_TENEBRAE, 1);
                }
                break;
            default:
                break;
        }

        // 10. Environment merges at the original thresholds (L392-410).
        if (counts.hasWater()) {
            addAspect(aspects, amounts, T_AQUA, 1);
        }
        if (counts.hasLava()) {
            addAspect(aspects, amounts, T_IGNIS, 1);
            addAspect(aspects, amounts, T_TERRA, 1);
        }
        if (counts.hasStone()) {
            addAspect(aspects, amounts, T_TERRA, 1);
        }
        if (counts.hasFoliage()) {
            addAspect(aspects, amounts, T_HERBA, 1);
        }

        // 11. Spread the budget over the aspects (original L412-425). The
        // original iterates getAspectsSorted() — tag-sorted — twice per
        // aspect; the order fixes which draw lands on which aspect, so the
        // sort is part of the contract.
        final List<Integer> order = sortedByTag(aspects);
        final int size = order.size();
        final int[] spread = new int[size];
        float total = 0.0F;
        for (int ax = 0; ax < size; ax++) {
            if (amounts.get(order.get(ax)) == 2) {
                spread[ax] = 50 + random.nextInt(25);
            } else {
                spread[ax] = 25 + random.nextInt(50);
            }
            total += spread[ax];
        }
        for (int ax = 0; ax < size; ax++) {
            final int idx = order.get(ax);
            amounts.set(idx, amounts.get(idx) + (int) (spread[ax] / total * value));
        }

        final Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (final int idx : order) {
            out.put(aspects.get(idx), amounts.get(idx));
        }
        return new PlannedNode(type, modifier, value, Map.copyOf(out));
    }

    /** Indices of {@code tags} sorted by tag string — the original's getAspectsSorted order. */
    private static List<Integer> sortedByTag(final List<String> tags) {
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> tags.get(a).compareTo(tags.get(b)));
        return order;
    }

    private static String pickPool(final List<String> pool, final Random random) {
        return pool.get(random.nextInt(pool.size()));
    }

    private static void addAspect(final List<String> aspects, final List<Integer> amounts, final String tag, final int amount) {
        final int idx = aspects.indexOf(tag);
        if (idx >= 0) {
            amounts.set(idx, amounts.get(idx) + amount);
        } else {
            aspects.add(tag);
            amounts.add(amount);
        }
    }

    // ------------------------------------------------------------------
    // World-facing placement
    // ------------------------------------------------------------------

    /**
     * Port of {@code createRandomNodeAt}: biome lookup → aura + taint check →
     * environment scan → {@link #planNode} → store the node record. The flag
     * signature mirrors the original so the tree/structure milestones can call
     * it unchanged.
     */
    public static void createRandomNodeAt(
            final ServerLevel level,
            final int x,
            final int y,
            final int z,
            final Random random,
            final boolean silverwood,
            final boolean eerie,
            final boolean small) {
        final Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(new BlockPos(x, y, z));
        final int baura = BiomeAura.getBiomeAura(biome);
        final EnvCounts counts = scanEnvironment(level, new BlockPos(x, y, z));
        final PlannedNode plan = planNode(random, baura, BiomeAura.matching(biome),
                BiomeAura.isTaintBiome(biome), silverwood, eerie, small,
                Math.max(3, com.jonnyutah12.thaumcraftclassic.config.TcConfig.getInt(
                        com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog.SPECIAL_NODE_RARITY)),
                counts, basicAspectPool(), complexAspectPool());
        createNodeAt(level, x, y, z, plan);
    }

    /**
     * Store a rolled node (the original's {@code createNodeAt}: blockairy
     * placement + TileNode fields — the block entity half is TC-M5-02).
     */
    public static void createNodeAt(final ServerLevel level, final int x, final int y, final int z, final PlannedNode plan) {
        final WorldMagicData data = WorldMagicData.get(level);
        data.addNode(new ChunkPos(new BlockPos(x, 0, z)), new WorldMagicData.Node(
                x, y, z, plan.type().id(),
                plan.modifier() == null ? null : plan.modifier().id(),
                plan.intensity(), plan.aspects()));
    }

    /**
     * Port of {@code generateWildNodes}: rarity gate, surface scan, the +0..3
     * vertical jitter with its replaceability check, then the node roll.
     *
     * @param nodeRarity {@code World_Generation.node_rarity} (0 disables — the
     *                   original would throw there; guarding is a documented deviation)
     * @param auraGen    true when this chunk already got its one wild-node attempt
     *                   (the original's per-chunk {@code auraGen} flag, set by the
     *                   scattered-feature hook)
     * @return whether a node was placed
     */
    public static boolean generateWildNodes(
            final ServerLevel level,
            final ChunkPos chunkPos,
            final Random random,
            final int nodeRarity,
            final boolean auraGen) {
        if (nodeRarity <= 0 || auraGen || random.nextInt(nodeRarity) != 0) {
            return false;
        }
        final int x = chunkPos.getMinBlockX() + random.nextInt(16);
        final int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        int q = firstUncoveredY(level, x, z);
        if (q < 2) {
            // Original: dimension.getActualHeight() + jitter + uncoveredY. Dead
            // in both versions — firstUncoveredY starts at 5 and only climbs.
            q = level.getMaxBuildHeight() + random.nextInt(64) - 32 + firstUncoveredY(level, x, z);
        }
        if (q < 2) {
            q = 32 + random.nextInt(64);
        }
        if (level.isEmptyBlock(new BlockPos(x, q + 1, z))) {
            q++;
        }
        final int p = random.nextInt(4);
        final BlockPos at = new BlockPos(x, q + p, z);
        final BlockState block = level.getBlockState(at);
        if (level.isEmptyBlock(at) || block.canBeReplaced()) {
            q += p;
        }
        if (q > level.getMaxBuildHeight()) {
            return false;
        }
        createRandomNodeAt(level, x, q, z, random, false, false, false);
        return true;
    }

    /**
     * Port of {@code Utils.getFirstUncoveredY}: walk up from y=5 (the original's
     * absolute start, kept as-is) while the block above is not air.
     */
    static int firstUncoveredY(final LevelReader level, final int x, final int z) {
        int y = 5;
        while (y < level.getMaxBuildHeight() && !level.isEmptyBlock(new BlockPos(x, y + 1, z))) {
            y++;
        }
        return y;
    }

    /**
     * Port of the original's 11×11×11 scan (L364-390): water, lava and stone
     * blocks are counted directly; "foliage" is anything the original's
     * {@code isFoliage} matched — leaves, grass/fern and flowers (the original
     * additionally matched modded custom-foliage blocks, which have no
     * vanilla-side equivalent). Out-of-world reads count as nothing, matching
     * the original's try/catch skip.
     */
    static EnvCounts scanEnvironment(final LevelReader level, final BlockPos center) {
        int water = 0;
        int lava = 0;
        int stone = 0;
        int foliage = 0;
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    final BlockPos pos = center.offset(x, y, z);
                    if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    final BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.WATER)) {
                        water++;
                    } else if (state.is(Blocks.LAVA)) {
                        lava++;
                    } else if (state.is(Blocks.STONE)) {
                        stone++;
                    }
                    if (isFoliage(state)) {
                        foliage++;
                    }
                }
            }
        }
        return new EnvCounts(water, lava, stone, foliage);
    }

    private static boolean isFoliage(final BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN);
    }

    /**
     * Original {@code basicAspects}: every aspect whose constructor passed no
     * components — exactly the six primals. Vacuos has components (AIR +
     * ENTROPY, {@code api/aspects/Aspect.java:23}) and lands in the complex
     * pool, as in the original's {@code getComponents() != null} split.
     */
    static List<String> basicAspectPool() {
        final List<String> pool = new ArrayList<>();
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            pool.add(aspect.getTag());
        }
        return pool;
    }

    /** Original {@code complexAspects}: every aspect with components. */
    static List<String> complexAspectPool() {
        final List<String> pool = new ArrayList<>();
        for (final Aspect aspect : Aspect.getCompoundAspects()) {
            pool.add(aspect.getTag());
        }
        return pool;
    }
}
