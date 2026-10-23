package com.jonnyutah12.thaumcraftclassic.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Biome aura / dominant-aspect table (TC-M5-01). Port of the original
 * {@code Config.registerBiomes()} table ({@code common/config/Config.java:837-867})
 * plus {@code BiomeHandler.getBiomeAura}/{@code getRandomBiomeTag}
 * ({@code common/lib/world/biomes/BiomeHandler.java}).
 *
 * <p>The original keyed the table by BiomeDictionary {@code Type}; on NeoForge
 * 1.21.1 the equivalent is the {@code c:} convention biome-tag set (the
 * successor of the Forge biome dictionary — {@code c:is_forest} ≡ FOREST, etc.).
 * Every one of the 31 registered types maps 1:1 onto a {@code c:} tag; the
 * mapping is pinned below and in docs/architecture/aura-nodes.md. Aura values,
 * dominant aspects and the average-over-matched-types rule (fallback 100) are
 * transcribed verbatim from the table in docs/balance-values.md §6.1.</p>
 *
 * <p>Deviation from the original: an unregistered dictionary type made
 * {@code getBiomeAura} throw internally and fall back to 100 for the whole
 * biome; here only registered tags are ever considered, so unregistered tags
 * are simply ignored. For vanilla biomes (all of whose dictionary types were
 * registered by the original) the two behaviours agree.</p>
 */
public final class BiomeAura {

    /**
     * One table row: dictionary type name, the biome tag that stands in for it,
     * aura level, dominant aspect tag (null = none), and the greatwood tree
     * support flags (consumed by the tree milestone, original
     * {@code BiomeHandler.getBiomeSupportsGreatwood}).
     */
    public record Entry(String dictionaryType, TagKey<Biome> tag, int aura, String aspectTag,
            boolean greatwood, float greatwoodChance) {
    }

    /** The original fallback aura for biomes with no usable dictionary data (BiomeHandler L32). */
    public static final int FALLBACK_AURA = 100;

    private static TagKey<Biome> cTag(final String name) {
        return TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("c", name));
    }

    /**
     * The 31 rows of {@code Config.registerBiomes}, in the original registration
     * order, with aura levels and dominant aspect tags verbatim. The MAGICAL row
     * carries no aspect tag in the original (null) — kept as null.
     */
    public static final List<Entry> TABLE = List.of(
            new Entry("WATER", cTag("is_aquatic"), 100, "aqua", false, 0.0F),
            new Entry("OCEAN", cTag("is_ocean"), 120, "aqua", false, 0.0F),
            new Entry("RIVER", cTag("is_river"), 100, "aqua", false, 0.0F),
            new Entry("WET", cTag("is_wet"), 80, "aqua", false, 0.0F),
            new Entry("HOT", cTag("is_hot"), 100, "ignis", false, 0.0F),
            new Entry("DESERT", cTag("is_desert"), 100, "ignis", false, 0.0F),
            new Entry("NETHER", cTag("is_nether"), 120, "ignis", false, 0.0F),
            new Entry("MESA", cTag("is_badlands"), 80, "ignis", false, 0.0F),
            new Entry("DENSE", cTag("is_dense_vegetation"), 100, "ordo", false, 0.0F),
            new Entry("SNOWY", cTag("is_snowy"), 80, "ordo", false, 0.0F),
            new Entry("COLD", cTag("is_cold"), 80, "ordo", false, 0.0F),
            new Entry("FROZEN", cTag("is_icy"), 100, "ordo", false, 0.0F),
            new Entry("MUSHROOM", cTag("is_mushroom"), 140, "ordo", false, 0.0F),
            new Entry("CONIFEROUS", cTag("is_taiga"), 100, "terra", true, 0.2F),
            new Entry("FOREST", cTag("is_forest"), 120, "terra", true, 1.0F),
            new Entry("SANDY", cTag("is_sandy"), 80, "terra", false, 0.0F),
            new Entry("BEACH", cTag("is_beach"), 80, "terra", false, 0.0F),
            new Entry("SAVANNA", cTag("is_savanna"), 80, "aer", true, 0.2F),
            new Entry("MOUNTAIN", cTag("is_mountain"), 100, "aer", false, 0.0F),
            new Entry("HILLS", cTag("is_hill"), 120, "aer", false, 0.0F),
            new Entry("PLAINS", cTag("is_plains"), 80, "aer", true, 0.2F),
            new Entry("DRY", cTag("is_dry"), 80, "perditio", false, 0.0F),
            new Entry("SPARSE", cTag("is_sparse_vegetation"), 80, "perditio", false, 0.0F),
            new Entry("SWAMP", cTag("is_swamp"), 120, "perditio", true, 0.2F),
            new Entry("WASTELAND", cTag("is_wasteland"), 80, "perditio", false, 0.0F),
            new Entry("JUNGLE", cTag("is_jungle"), 100, "herba", false, 0.0F),
            new Entry("LUSH", cTag("is_lush"), 100, "herba", true, 0.5F),
            new Entry("MAGICAL", cTag("is_magical"), 100, null, true, 1.0F),
            new Entry("END", cTag("is_end"), 80, "vacuos", false, 0.0F),
            new Entry("SPOOKY", cTag("is_spooky"), 80, "spiritus", false, 0.0F),
            new Entry("DEAD", cTag("is_dead"), 50, "mortuus", false, 0.0F));

    /**
     * Biomes treated as Taint biomes by the node rules (original: identity
     * compare against {@code ThaumcraftWorldGenerator.biomeTaint}). The Taint
     * biome itself is registered by the taint/catalog milestones (TC-M5-04 /
     * TC-M6-04f); they add its key here. Empty until then — the multiplier and
     * TAINTED-type roll simply never fire, which is what an unmodded world
     * without Taint biomes did.
     */
    private static final Set<ResourceKey<Biome>> TAINT_BIOMES = ConcurrentHashMap.newKeySet();

    public static void registerTaintBiome(final ResourceKey<Biome> biome) {
        TAINT_BIOMES.add(biome);
    }

    /** True once a taint biome is registered — TC-M5-02 node painting gate. */
    public static boolean hasTaintBiome() {
        return !TAINT_BIOMES.isEmpty();
    }

    /**
     * The taint biome to paint (TC-M5-02: the original had exactly one
     * {@code ThaumcraftWorldGenerator.biomeTaint}; with several registered the
     * first wins). Null when none is registered.
     */
    public static ResourceKey<Biome> firstTaintBiome() {
        return TAINT_BIOMES.isEmpty() ? null : TAINT_BIOMES.iterator().next();
    }

    public static boolean isTaintBiome(final Holder<Biome> biome) {
        return biome.unwrapKey().map(TAINT_BIOMES::contains).orElse(false);
    }

    private BiomeAura() {
    }

    /** All table rows whose tag the biome carries — the modern {@code BiomeDictionary.getTypesForBiome} ∩ registered types. */
    public static List<Entry> matching(final Holder<Biome> biome) {
        final List<Entry> out = new ArrayList<>();
        for (final Entry entry : TABLE) {
            if (biome.is(entry.tag())) {
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * Original {@code BiomeHandler.getBiomeAura}: the integer average of the
     * aura levels of all matched types; {@link #FALLBACK_AURA} when nothing
     * matches (the original's exception fallback).
     */
    public static int getBiomeAura(final Holder<Biome> biome) {
        return averageAura(matching(biome));
    }

    /** Pure form of the average rule — unit-testable without a level. */
    public static int averageAura(final List<Entry> matched) {
        if (matched.isEmpty()) {
            return FALLBACK_AURA;
        }
        int average = 0;
        for (final Entry entry : matched) {
            average += entry.aura();
        }
        return average / matched.size();
    }

    /**
     * Original {@code BiomeHandler.getRandomBiomeTag}: a uniform random matched
     * row's dominant aspect tag, or null (MAGICAL carries none, and an
     * unmatched biome returns null).
     */
    public static String randomBiomeTag(final Holder<Biome> biome, final Random random) {
        return pickRandomTag(matching(biome), random);
    }

    /** Pure form of the pick rule. */
    public static String pickRandomTag(final List<Entry> matched, final Random random) {
        if (matched.isEmpty()) {
            return null;
        }
        return matched.get(random.nextInt(matched.size())).aspectTag();
    }

    /**
     * Original {@code BiomeHandler.getBiomeSupportsGreatwood}: the chance from
     * the first matched row that supports greatwood, else 0. Kept here so the
     * tree milestone reads the same table; unused by node generation.
     */
    public static float supportsGreatwood(final Holder<Biome> biome) {
        for (final Entry entry : matching(biome)) {
            if (entry.greatwood()) {
                return entry.greatwoodChance();
            }
        }
        return 0.0F;
    }
}
