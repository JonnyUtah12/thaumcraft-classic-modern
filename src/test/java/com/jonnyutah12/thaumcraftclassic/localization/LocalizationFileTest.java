package com.jonnyutah12.thaumcraftclassic.localization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit guards for the shipped localization file (TC-LOC-s / t_a2a74726).
 *
 * <p>The live-registry half of the contract — every registered item/block/menu
 * has an entry, and no entry is dead — runs in-game
 * ({@code LocalizationGameTest}), because the real id set only exists on the
 * game-test server. This class covers what is decidable from the resources
 * alone: the file parses and is populated, the keys follow the 1.21.1
 * conventions, every registry-derived key names content that
 * {@code docs/content-mapping.csv} inventories, the translation keys the
 * shipped code passes to {@code Component.translatable} are all present, and —
 * on a build that has the reference tree — every per-meta name preserves the
 * original Thaumcraft 4.2.3.5 wording.</p>
 */
class LocalizationFileTest {

    /** 1.21.1 key conventions for the content this mod ships. */
    private static final List<String> CURRENT_PREFIXES = List.of(
            "item.thaumcraft.", "block.thaumcraft.", "container.thaumcraft.",
            "entity.thaumcraft.", "itemGroup.thaumcraft.");

    /**
     * Keys the shipped code uses with the original 1.7.10 key shape, kept
     * verbatim so the original strings apply unchanged
     * (ItemAmuletVis:206/209, ItemBaubleBlanks:142, ItemResonator:66-72,
     * ItemHandMirror:142-158, ResearchManager:460).
     */
    private static final List<String> LEGACY_KEYS = List.of(
            "item.ItemAmuletVis.text", "item.capacity.text",
            "tc.discount", "tc.resonator1", "tc.resonator2", "tc.resonator3",
            "tc.handmirrorlinkedto", "tc.handmirrorlinked", "tc.handmirrorerror",
            "tc.researcherror");

    /** Registry-derived key shape: lower-case id plus an optional meta/tooltip suffix. */
    private static final Pattern KEY_SHAPE =
            Pattern.compile("(?:item|block|entity|itemGroup)\\.thaumcraft\\.[a-z0-9_]+(?:\\.[a-z0-9]+)?");

    /** Every translation key the shipped code passes to Component.translatable that is not registry-derived. */
    private static final List<String> CODE_AUTHORED_KEYS = List.of(
            "container.thaumcraft.chesthungry", "container.thaumcraft.deconstructiontable",
            "container.thaumcraft.magicbox", "container.thaumcraft.spa",
            "item.thaumcraft.itemlootbag.tooltip", "item.thaumcraft.trunkspawner.tooltip",
            "item.thaumcraft.itemgolemupgrade.0", "item.thaumcraft.itemgolemupgrade.1",
            "item.thaumcraft.itemgolemupgrade.2", "item.thaumcraft.itemgolemupgrade.3",
            "item.thaumcraft.itemgolemupgrade.4", "item.thaumcraft.itemgolemupgrade.5",
            "item.capacity.text", "item.ItemAmuletVis.text",
            "tc.discount", "tc.resonator1", "tc.resonator2", "tc.resonator3",
            "tc.handmirrorlinkedto", "tc.handmirrorlinked", "tc.handmirrorerror", "tc.researcherror");

    /**
     * The shipped multi-meta items and the original-metadata domain their
     * {@code getName(ItemStack)} walks (mirrors the {@code META_COUNT} /
     * {@code VALID_METAS} / ITEM_META rarity contract of each class; nuggets
     * use the original's sparse damage values 0-7, 16-21, 31).
     */
    private static final Map<String, int[]> META_DRIVEN = metaDriven();

    private static Map<String, int[]> metaDriven() {
        final Map<String, int[]> metas = new java.util.LinkedHashMap<>();
        metas.put("itemresource", range(0, 18));
        metas.put("itemshard", range(0, 6));
        metas.put("itemnugget", concat(range(0, 7), concat(range(16, 21), new int[] {31})));
        metas.put("itemlootbag", range(0, 2));
        metas.put("itembaubleblanks", range(0, 3));
        metas.put("itemamuletvis", range(0, 1));
        metas.put("itemamuletrunic", range(0, 1));
        metas.put("itemringrunic", range(0, 3));
        metas.put("itemgirdlerunic", range(0, 1));
        return java.util.Collections.unmodifiableMap(metas);
    }

    private static int[] range(final int from, final int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).toArray();
    }

    private static int[] concat(final int[] a, final int[] b) {
        return java.util.stream.IntStream.concat(java.util.stream.IntStream.of(a), java.util.stream.IntStream.of(b))
                .toArray();
    }

    private static JsonObject lang() {
        try (InputStream in = LocalizationFileTest.class.getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void langFileParsesAndIsFullyPopulated() {
        final JsonObject lang = lang();
        assertTrue(lang.size() >= 120,
                "expected one entry per shipped key (120+), got " + lang.size());
        for (final String key : lang.keySet()) {
            assertTrue(lang.get(key).isJsonPrimitive() && lang.get(key).getAsString().length() > 0,
                    key + " has an empty value");
        }
    }

    @Test
    void keysFollowThe1211KeyConventions() {
        for (final String key : lang().keySet()) {
            final boolean current = CURRENT_PREFIXES.stream().anyMatch(key::startsWith);
            final boolean legacy = LEGACY_KEYS.contains(key);
            assertTrue(current || legacy,
                    key + " is neither a 1.21.1 current-shape key nor a documented original-shape key");
            if (key.startsWith("item.thaumcraft.") || key.startsWith("block.thaumcraft.")
                    || key.startsWith("entity.thaumcraft.")) {
                assertTrue(KEY_SHAPE.matcher(key).matches(), key + " is not a lower-case id key");
            }
        }
    }

    @Test
    void valuesCarryNoLegacyMarkup() {
        final JsonObject lang = lang();
        for (final String key : lang.keySet()) {
            final String value = lang.get(key).getAsString();
            assertTrue(value.indexOf('\u00a7') < 0,
                    key + " keeps 1.7-era section-sign colour codes; 1.21.1 renders them literally");
            assertTrue(!value.contains("%TYPE"),
                    key + " keeps the original %TYPE token; 1.21.1 formats with %s");
            assertTrue(!value.contains("<BR>"),
                    key + " keeps original BBCode-style markup");
            final String withoutPlaceholders = value.replaceAll("%(?:\\d+\\$)?[sdc]", "");
            assertTrue(!withoutPlaceholders.contains("%"),
                    key + " has an unsupported format placeholder in: " + value);
        }
    }

    @Test
    void everyCodeTranslatedKeyIsShipped() {
        final JsonObject lang = lang();
        final List<String> missing = CODE_AUTHORED_KEYS.stream().filter(k -> !lang.has(k)).sorted().toList();
        assertEquals(List.of(), missing, "Component.translatable keys the shipped code uses are not in en_us.json");
    }

    @Test
    void everyRegistryKeyNamesInventoriedContent() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");

        final java.util.Set<String> inventory = new java.util.HashSet<>();
        for (final com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.Row row
                : com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.read(csv)) {
            final int colon = row.newId().indexOf(':');
            inventory.add(colon > 0 ? row.newId().substring(colon + 1) : row.newId());
        }

        final JsonObject lang = lang();
        final List<String> orphans = lang.keySet().stream()
                .filter(key -> key.startsWith("item.thaumcraft.") || key.startsWith("block.thaumcraft.")
                        || key.startsWith("container.thaumcraft.") || key.startsWith("entity.thaumcraft."))
                .filter(key -> !CODE_AUTHORED_KEYS.contains(key))
                .map(key -> key.split("\\.")[2])
                .filter(id -> !inventory.contains(id))
                .distinct()
                .sorted()
                .toList();
        assertEquals(List.of(), orphans,
                "en_us.json keys name ids that docs/content-mapping.csv does not inventory");
    }

    @Test
    void everyMultiMetaItemHasItsFullMetaSet() {
        final JsonObject lang = lang();
        META_DRIVEN.forEach((id, metas) -> {
            for (final int meta : metas) {
                final String key = "item.thaumcraft." + id + "." + meta;
                assertTrue(lang.has(key) && lang.get(key).isJsonPrimitive()
                                && lang.get(key).getAsString().length() > 0,
                        key + " is missing; " + id + " getName() emits it for that original metadata");
            }
        });
        assertTrue(lang.get("item.thaumcraft.itembaubleblanks.3").getAsString().contains("%s"),
                "the apprentice-ring name formats the aspect name as an argument");
    }

    @Test
    void perMetaNamesPreserveTheOriginalWording() throws IOException {
        final Path original = Path.of(System.getProperty("tc.originalLangFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(original),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");

        final Map<String, String> legacy = new LinkedHashMap<>();
        for (final String line : Files.readAllLines(original, StandardCharsets.UTF_8)) {
            final String trimmed = line.replace("\r", "");
            final int equals = trimmed.indexOf('=');
            if (equals > 0 && !trimmed.startsWith("#")) {
                legacy.put(trimmed.substring(0, equals).trim(), normalize(trimmed.substring(equals + 1)));
            }
        }

        final JsonObject lang = lang();
        final Map<String, String> originalClass = Map.of(
                "itemresource", "ItemResource", "itemshard", "ItemShard", "itemnugget", "ItemNugget",
                "itemlootbag", "ItemLootBag", "itembaubleblanks", "ItemBaubleBlanks",
                "itemamuletvis", "ItemAmuletVis", "itemamuletrunic", "ItemAmuletRunic",
                "itemringrunic", "ItemRingRunic", "itemgirdlerunic", "ItemGirdleRunic");

        int checked = 0;
        final List<String> mismatches = new java.util.ArrayList<>();
        for (final String key : lang.keySet()) {
            final String[] parts = key.split("\\.");
            if (parts.length != 4 || !parts[0].equals("item")
                    || !parts[3].matches("[0-9]+") || !originalClass.containsKey(parts[2])) {
                continue;
            }
            final String legacyKey = "item." + originalClass.get(parts[2]) + "." + parts[3] + ".name";
            final String expected = legacy.get(legacyKey);
            assertNotNull(expected, "the port ships " + key + " with no original " + legacyKey + " to preserve");
            if (!expected.equals(normalize(lang.get(key).getAsString()))) {
                mismatches.add(key + ": expected \"" + expected + "\" from " + legacyKey
                        + ", got \"" + normalize(lang.get(key).getAsString()) + "\"");
            }
            checked++;
        }
        assertEquals(List.of(), mismatches, "per-meta names must preserve the original en_US.lang wording");
        assertTrue(checked >= 55, "expected the nine multi-meta families to be checked, got " + checked);
    }

    /** The normalization the port applies to original 1.7 lang strings. */
    private static String normalize(final String value) {
        return value.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "")
                .replace("%TYPE", "%s")
                .trim();
    }
}
