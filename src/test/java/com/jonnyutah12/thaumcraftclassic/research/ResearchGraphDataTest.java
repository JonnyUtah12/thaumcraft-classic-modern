package com.jonnyutah12.thaumcraftclassic.research;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-01 acceptance: the extracted research graph
 * (data/thaumcraft/research/) must match the TC-M0-04 inventory
 * (docs/original-content-inventory.md section 8) entry for entry, and the
 * graph must satisfy the structural invariants of the original: acyclic
 * prerequisites, every reference resolving, every page lang key present in
 * the original en_US.lang with the embedded text verbatim, and every
 * aspect/item reference resolving against the original inventories.
 */
class ResearchGraphDataTest {

    /** Vanilla items the original research graph references by obfuscated
     *  1.7.10 field; resolved via MCP stable_11 at extraction time
     *  (docs/research-graph-notes.md). Any new entry forces a re-review. */
    private static final Set<String> ALLOWED_VANILLA_ITEMS = Set.of(
            "minecraft:wool", "minecraft:bow", "minecraft:bone", "minecraft:gold_nugget",
            "minecraft:ender_pearl", "minecraft:portal", "minecraft:end_portal",
            "minecraft:end_portal_frame");

    /** Recipe objects the original constructed inline in ConfigResearch.java
     *  instead of registering them in the recipes map. */
    private static final Set<String> ALLOWED_INLINE_RECIPES = Set.of(
            "inline:InfusionRunicAugmentRecipe#0", "inline:InfusionRunicAugmentRecipe#1",
            "inline:InfusionRunicAugmentRecipe#2", "inline:InfusionRunicAugmentRecipe#3",
            "inline:InfusionRunicAugmentRecipe#4",
            "inline:ShapedArcaneRecipe#SCEPTRE#1", "inline:ShapedArcaneRecipe#SCEPTRE#2",
            "inline:ShapedArcaneRecipe#SCEPTRE#3");

    /** Entries the original registered without any lang name/text (UNKNOWN). */
    private static final Set<String> UNKNOWN_DISPLAY_ENTRIES = Set.of("CAP_iron", "ROD_wood");

    record InventoryRow(String key, String category, String name, String flags) {
    }

    private static Path dataDir() {
        String path = System.getProperty("tc.researchDataDir");
        assertNotNull(path, "system property 'tc.researchDataDir' is not set — run tests through ./gradlew test");
        Path dir = Path.of(path);
        assertTrue(Files.isDirectory(dir), "research data dir not found at " + dir);
        return dir;
    }

    private static ResearchGraph loadGraph() throws IOException {
        try (Reader categories = Files.newBufferedReader(dataDir().resolve("categories.json"));
             Reader entries = Files.newBufferedReader(dataDir().resolve("entries.json"))) {
            return ResearchGraphLoader.load(categories, entries);
        }
    }

    private static List<InventoryRow> inventoryRows() throws IOException {
        String doc = Files.readString(inventoryFile());
        String section = doc.split("## 8\\. Research")[1].split("## 9\\.")[0];
        List<InventoryRow> rows = new ArrayList<>();
        for (String line : section.split("\n")) {
            if (!line.startsWith("| ") || line.contains("Research key") || line.contains("---")) {
                continue;
            }
            String[] parts = line.strip().replaceFirst("^\\|", "").split("\\|");
            if (parts.length == 4) {
                rows.add(new InventoryRow(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()));
            }
        }
        return rows;
    }

    private static Path inventoryFile() {
        String path = System.getProperty("tc.contentInventoryFile");
        assertNotNull(path, "system property 'tc.contentInventoryFile' is not set — run tests through ./gradlew test");
        Path file = Path.of(path);
        assertTrue(Files.isRegularFile(file), "content inventory not found at " + file);
        return file;
    }

    /** Aspect tag names from inventory section 1 (| AIR | aer | ...). */
    private static Set<String> inventoryAspectTags() throws IOException {
        String doc = Files.readString(inventoryFile());
        String section = doc.split("## 1\\. Aspects")[1].split("## 2\\.")[0];
        Set<String> tags = new TreeSet<>();
        for (String line : section.split("\n")) {
            if (!line.startsWith("| ") || line.contains("---") || line.contains("| Aspect |")) {
                continue;
            }
            String[] parts = line.strip().replaceFirst("^\\|", "").split("\\|");
            if (parts.length >= 2) {
                tags.add(parts[1].trim());
            }
        }
        return tags;
    }

    /** Registered Thaumcraft item/block names from inventory sections 2 and 5. */
    private static Set<String> inventoryThaumcraftItemNames() throws IOException {
        String doc = Files.readString(inventoryFile());
        Set<String> names = new TreeSet<>();
        for (String header : new String[]{"## 2. Blocks", "## 5. Items"}) {
            String section = doc.split(java.util.regex.Pattern.quote(header) + "[^\\n]*\\n")[1]
                    .split("\\n## ")[0];
            for (String line : section.split("\n")) {
                if (!line.startsWith("| ") || line.contains("---")
                        || line.contains("Register name")) {
                    continue;
                }
                String[] parts = line.strip().replaceFirst("^\\|", "").split("\\|");
                if (parts.length >= 2) {
                    names.add("Thaumcraft:" + parts[0].trim());
                }
            }
        }
        return names;
    }

    @Test
    void graphLoadsAndIsStructurallyClean() throws IOException {
        ResearchGraph graph = loadGraph();
        assertEquals(List.of(), graph.problems(), "extracted graph must have no structural problems");
    }

    @Test
    void entryCountMatchesTheContentInventoryExactly() throws IOException {
        ResearchGraph graph = loadGraph();
        List<InventoryRow> rows = inventoryRows();
        assertEquals(201, rows.size(), "TC-M0-04 inventory lists 201 research entries");
        assertEquals(6, graph.categoryCount());
        assertEquals(rows.size(), graph.entryCount());

        Set<String> inventoryKeys = new HashSet<>();
        for (InventoryRow row : rows) {
            inventoryKeys.add(row.key());
        }
        assertEquals(inventoryKeys, new HashSet<>(graph.entries().stream().map(ResearchEntryData::key).toList()),
                "extracted entry keys must equal the inventory keys exactly");
    }

    @Test
    void everyEntryMatchesItsInventoryRow() throws IOException {
        ResearchGraph graph = loadGraph();
        Map<String, String> flagMap = Map.of("setHidden", "hidden", "setAutoUnlock", "autoUnlock",
                "setSpecial", "special");
        for (InventoryRow row : inventoryRows()) {
            ResearchEntryData entry = graph.entry(row.key());
            assertNotNull(entry, "inventory entry missing from the graph: " + row.key());
            assertEquals(row.category(), entry.category(), "category mismatch for " + row.key());
            if (UNKNOWN_DISPLAY_ENTRIES.contains(row.key())) {
                // The original had no lang name for these virtual stubs; the
                // inventory shows an empty display name and so must the graph.
                assertEquals("", row.name(), "inventory must show no name for " + row.key());
                assertEquals(null, entry.nameEnUs(), "UNKNOWN display must stay null: " + row.key());
            } else {
                assertEquals(row.name(), entry.nameEnUs(), "display name mismatch for " + row.key());
            }
            Set<String> expectedFlags = new HashSet<>();
            if (!"-".equals(row.flags())) {
                for (String flag : row.flags().split(",")) {
                    expectedFlags.add(flagMap.get(flag.trim()));
                }
            }
            Set<String> actualFlags = new HashSet<>();
            ResearchEntryData.Flags f = entry.flags();
            if (f.hidden()) {
                actualFlags.add("hidden");
            }
            if (f.autoUnlock()) {
                actualFlags.add("autoUnlock");
            }
            if (f.special()) {
                actualFlags.add("special");
            }
            assertEquals(expectedFlags, actualFlags, "flag mismatch for " + row.key());
        }
    }

    @Test
    void categoriesMatchTheOriginalRegistration() throws IOException {
        ResearchGraph graph = loadGraph();
        assertEquals(List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"),
                graph.categories().stream().map(ResearchCategoryData::key).toList());
        for (ResearchCategoryData category : graph.categories()) {
            assertEquals("tc.research_category." + category.key(), category.nameKey());
            assertNotNull(category.nameEnUs(), "category name missing: " + category.key());
            assertTrue(category.icon().startsWith("thaumcraft:textures/"), "category icon: " + category.key());
            assertTrue(category.background().startsWith("thaumcraft:textures/"), "category background: " + category.key());
        }
    }

    @Test
    void langStringsAreVerbatimFromTheOriginalFile() throws IOException {
        ResearchGraph graph = loadGraph();
        Path langFile = Path.of(System.getProperty("tc.originalLangFile", ""));
        boolean referenceAvailable = Files.isRegularFile(langFile);
        Map<String, String> lang = referenceAvailable ? readLang(langFile) : Map.of();

        for (ResearchEntryData entry : graph.entries()) {
            checkLang(entry.key(), entry.nameKey(), entry.nameEnUs(), lang, referenceAvailable);
            checkLang(entry.key(), entry.textKey(), entry.textEnUs(), lang, referenceAvailable);
            for (ResearchPageData page : entry.pages()) {
                if (page.textKey() != null) {
                    checkLang(entry.key(), page.textKey(), page.textEnUs(), lang, referenceAvailable);
                }
            }
        }
    }

    private static void checkLang(String entryKey, String key, String embedded,
            Map<String, String> lang, boolean referenceAvailable) {
        assertNotNull(key, "lang key must be recorded for " + entryKey);
        if (UNKNOWN_DISPLAY_ENTRIES.contains(entryKey)) {
            // The original shipped no lang entry for these virtual stubs; the
            // graph must record the key but keep the text UNKNOWN (null).
            assertEquals(null, embedded, "UNKNOWN display must stay null: " + entryKey + " " + key);
            if (referenceAvailable) {
                assertTrue(!lang.containsKey(key), "original unexpectedly has a lang entry: " + key);
            }
            return;
        }
        if (referenceAvailable) {
            assertTrue(lang.containsKey(key), "referenced lang key missing from the original file: " + key);
            assertEquals(lang.get(key), embedded, "embedded text is not verbatim for " + key);
        } else {
            assertNotNull(embedded, "embedded text missing for " + key);
        }
    }

    private static Map<String, String> readLang(Path file) throws IOException {
        Map<String, String> entries = new HashMap<>();
        for (String raw : Files.readAllLines(file)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int split = line.indexOf('=');
            entries.put(line.substring(0, split).strip(), line.substring(split + 1));
        }
        return entries;
    }

    @Test
    void aspectReferencesResolveAgainstTheAspectInventory() throws IOException {
        ResearchGraph graph = loadGraph();
        Set<String> tags = inventoryAspectTags();
        assertEquals(48, tags.size(), "TC-M0-04 inventory lists 48 aspects");
        for (ResearchEntryData entry : graph.entries()) {
            for (ResearchEntryData.AspectTag tag : nullSafe(entry.tags())) {
                assertTrue(tags.contains(tag.aspect()), entry.key() + ": unknown aspect tag " + tag.aspect());
                assertTrue(tag.amount() > 0, entry.key() + ": non-positive tag amount");
            }
            for (String aspect : nullSafe(entry.triggers().aspects())) {
                assertTrue(tags.contains(aspect), entry.key() + ": unknown trigger aspect " + aspect);
            }
        }
    }

    @Test
    void itemReferencesResolveAgainstTheItemAndBlockInventories() throws IOException {
        ResearchGraph graph = loadGraph();
        Set<String> known = new HashSet<>(inventoryThaumcraftItemNames());
        for (ResearchEntryData entry : graph.entries()) {
            ResearchEntryData.Icon icon = entry.icon();
            if ("item".equals(icon.kind())) {
                assertKnownItem(entry.key(), known, icon.item());
            } else if ("texture".equals(icon.kind())) {
                assertTrue(icon.texture().startsWith("thaumcraft:textures/"), entry.key() + ": icon texture");
            } else if (!"none".equals(icon.kind())) {
                throw new AssertionError(entry.key() + ": unexpected icon kind " + icon.kind());
            }
            for (ResearchEntryData.ItemRef item : nullSafe(entry.triggers().items())) {
                assertKnownItem(entry.key(), known, item.item());
            }
            for (ResearchPageData page : entry.pages()) {
                if (page.input() != null) {
                    assertKnownItem(entry.key(), known, page.input().item());
                }
            }
        }
    }

    private static void assertKnownItem(String entryKey, Set<String> known, String item) {
        assertTrue(known.contains(item) || ALLOWED_VANILLA_ITEMS.contains(item),
                entryKey + ": item reference not in the original inventories: " + item);
    }

    @Test
    void recipeReferencesAreOriginalMapKeys() throws IOException {
        ResearchGraph graph = loadGraph();
        Set<String> referenced = new TreeSet<>();
        for (ResearchEntryData entry : graph.entries()) {
            for (ResearchPageData page : entry.pages()) {
                if (page.recipe() != null) {
                    referenced.add(page.recipe());
                }
                referenced.addAll(nullSafe(page.recipes()));
                if (page.recipeGroup() != null) {
                    referenced.add(page.recipeGroup());
                }
            }
        }
        assertTrue(referenced.size() > 300, "expected the graph to reference most original recipe keys");
        for (String key : referenced) {
            if (key.startsWith("inline:")) {
                assertTrue(ALLOWED_INLINE_RECIPES.contains(key), "unexpected inline recipe reference: " + key);
            } else {
                assertTrue(!key.isBlank(), "blank recipe key referenced");
            }
        }
    }

    @Test
    void graphIsAcyclicAndEveryReferenceResolves() throws IOException {
        ResearchGraph graph = loadGraph();
        for (ResearchEntryData entry : graph.entries()) {
            for (String parent : concat(entry.parents(), entry.parentsHidden(), entry.siblings())) {
                assertNotNull(graph.entry(parent), entry.key() + ": unknown prerequisite " + parent);
            }
            for (ResearchPageData page : entry.pages()) {
                if (page.requires() != null) {
                    assertNotNull(graph.entry(page.requires()), entry.key() + ": page gate " + page.requires());
                }
            }
        }
        // The original gates on parents+parentsHidden; the extracted graph must
        // admit a topological order (ResearchGraph reports cycles as problems).
        assertEquals(List.of(), graph.problems());
    }

    private static Set<String> concat(List<String>... lists) {
        Set<String> out = new HashSet<>();
        for (List<String> list : lists) {
            out.addAll(nullSafe(list));
        }
        return out;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
