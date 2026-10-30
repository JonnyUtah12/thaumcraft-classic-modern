package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * The per-variant display-name domain of the multi-variant mega-blocks
 * (TC-M7-01b / t_f40bbc0d).
 *
 * <p>The original 1.7.10 mega-blocks were one block id with metadata
 * selecting the sub-kind, and each metadata value carried its own name key:
 * {@code tile.blockAiry.0.name=Aura Node},
 * {@code tile.blockAiry.5.name=Energized Aura Node}. 1.21.1 has no per-state
 * block display name, so the port ships the same strings as per-variant keys
 * — {@code block.thaumcraft.blockairy.0}, {@code block.thaumcraft.blockairy.5}
 * — and {@link VariantBlockItem} resolves them from the stack's
 * {@code variant} value (see {@link VariantStateItems}).</p>
 *
 * <p>This class is the single declaration of <em>which</em> (block, meta)
 * pairs are named — the strings themselves live in
 * {@code assets/thaumcraft/lang/en_us.json}. It mirrors the original's key
 * set verbatim (D-001: ids and meta values unchanged), including the meta
 * values the original left unnamed: {@code blockMetalDevice} 4,
 * {@code blockWoodenDevice} 3 and {@code blockAiry} 6-9 have no
 * {@code tile.<id>.<meta>.name} key in the original, so those variants keep
 * the base block label. The two cosmetic slab families are the original's
 * only non-numeric name keys ({@code tile.blockCosmeticSlabStone.arcane.name}
 * / {@code .eldritch.name}, {@code .greatwood} / {@code .silverwood}); their
 * ported keys carry the numeric {@code variant} value like every other family
 * and the legacy key is recorded here so the wording parity test can look the
 * string up.</p>
 *
 * <p>Three audits read this table: {@code LocalizationGameTest} requires an
 * en_us.json entry for every pair the live registries can render and fails on
 * entries that map to no pair, and {@code LocalizationFileTest} compares every
 * declared pair with {@code reference/assets/thaumcraft/lang/en_US.lang} in
 * both directions.</p>
 */
public final class BlockVariantNames {

    /** Key prefix of a per-variant block label: {@code block.thaumcraft.<id>.<meta>}. */
    public static final String KEY_PREFIX = "block." + ThaumcraftClassic.MOD_ID + ".";

    /** block id path -> original-metadata value -> the 1.7.10 lang key it mirrors. */
    private static final Map<String, Map<Integer, String>> LEGACY_KEYS = table();

    /**
     * The per-variant name keys this table declares — the shipped subset of
     * the original's {@code tile.<id>.<meta>.name} set, in port key shape.
     */
    public static Set<String> allKeys() {
        final Set<String> keys = new TreeSet<>();
        LEGACY_KEYS.forEach((path, byMeta) -> byMeta.keySet().forEach(meta -> keys.add(key(path, meta))));
        return keys;
    }

    /** The mega-block ids that carry named variants. */
    public static Set<String> paths() {
        return LEGACY_KEYS.keySet();
    }

    /** The original-metadata values of {@code path} that the original named. */
    public static int[] metasOf(final String path) {
        final Map<Integer, String> byMeta = LEGACY_KEYS.get(path);
        return byMeta == null ? new int[0] : byMeta.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    /** Does the original ship a name for this (block, meta) pair? */
    public static boolean declares(final String path, final int meta) {
        final Map<Integer, String> byMeta = LEGACY_KEYS.get(path);
        return byMeta != null && byMeta.containsKey(meta);
    }

    /** The ported key of a (block, meta) pair, e.g. {@code block.thaumcraft.blockairy.5}. */
    public static String key(final String path, final int meta) {
        return KEY_PREFIX + path + "." + meta;
    }

    /**
     * The 1.7.10 key the ported key mirrors, e.g. {@code tile.blockAiry.5.name}
     * (the slabs keep the original's name-key form:
     * {@code tile.blockCosmeticSlabStone.arcane.name}).
     */
    public static String legacyKey(final String path, final int meta) {
        return LEGACY_KEYS.get(path).get(meta);
    }

    private static Map<String, Map<Integer, String>> table() {
        final Map<String, Map<Integer, String>> table = new LinkedHashMap<>();
        // The device blocks: sparse because the original left those metas unnamed.
        put(table, "blockstonedevice", "blockStoneDevice", range(0, 14));
        put(table, "blockmetaldevice", "blockMetalDevice",
                new int[] {0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});
        put(table, "blockwoodendevice", "blockWoodenDevice", new int[] {0, 1, 2, 4, 5, 6, 7, 8});
        put(table, "blockairy", "blockAiry", new int[] {0, 1, 2, 3, 4, 5, 10, 11, 12});
        put(table, "blocktable", "blockTable", new int[] {0, 1, 14, 15});
        put(table, "blockmirror", "blockMirror", new int[] {0, 6});
        // The decorative families: every state in the property range is named.
        put(table, "blockcosmeticsolid", "blockCosmeticSolid", range(0, 15));
        put(table, "blockcosmeticopaque", "blockCosmeticOpaque", range(0, 2));
        put(table, "blockcrystal", "blockCrystal", range(0, 7));
        put(table, "blockcustomore", "blockCustomOre", range(0, 7));
        put(table, "blockcandle", "blockCandle", range(0, 15));
        // TC-M5-04-s taint: the original named metas 0-2 of blockTaint — the
        // same three its getSubItems lists (BlockTaint.java:49-53) — and 0-4
        // of blockTaintFibres, matching that class' five-state range.
        put(table, "blocktaint", "blockTaint", new int[] {0, 1, 2});
        put(table, "blocktaintfibres", "blockTaintFibres", range(0, 4));
        // The slabs: the original keyed these two families by name, not meta.
        putNamed(table, "blockcosmeticslabstone", "blockCosmeticSlabStone",
                new String[] {"arcane", "eldritch"});
        putNamed(table, "blockcosmeticslabwood", "blockCosmeticSlabWood",
                new String[] {"greatwood", "silverwood"});
        return java.util.Collections.unmodifiableMap(table);
    }

    private static void put(final Map<String, Map<Integer, String>> table, final String path,
                            final String legacyId, final int[] metas) {
        final Map<Integer, String> byMeta = new TreeMap<>();
        for (final int meta : metas) {
            byMeta.put(meta, "tile." + legacyId + "." + meta + ".name");
        }
        table.put(path, java.util.Collections.unmodifiableMap(byMeta));
    }

    private static void putNamed(final Map<String, Map<Integer, String>> table, final String path,
                                 final String legacyId, final String[] legacyKeys) {
        final Map<Integer, String> byMeta = new TreeMap<>();
        for (int meta = 0; meta < legacyKeys.length; meta++) {
            byMeta.put(meta, "tile." + legacyId + "." + legacyKeys[meta] + ".name");
        }
        table.put(path, java.util.Collections.unmodifiableMap(byMeta));
    }

    private static int[] range(final int from, final int to) {
        return IntStream.rangeClosed(from, to).toArray();
    }

    /** The declared (block, meta) pairs of one block id, for the audit failure messages. */
    public static Set<String> declaredKeys(final String path) {
        final Set<String> keys = new LinkedHashSet<>();
        for (final int meta : metasOf(path)) {
            keys.add(key(path, meta));
        }
        return keys;
    }

    private BlockVariantNames() {
    }
}
