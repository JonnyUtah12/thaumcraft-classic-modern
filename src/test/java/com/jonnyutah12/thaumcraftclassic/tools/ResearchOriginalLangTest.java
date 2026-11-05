package com.jonnyutah12.thaumcraftclassic.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probes for D-016 option 2's generation half (BOOK-3a). No Minecraft types:
 * the class under test is build tooling (the {@code tools} source set), and these
 * probes are what makes its rules executable.
 *
 * <p>Two of them pin measured facts rather than behaviour: the shipped research
 * graph references exactly 712 display keys — every entry {@code nameKey} and
 * {@code textKey} plus every page {@code textKey} (201 name / 201 text / 310
 * page), one of them outside the {@code tc.} namespace — and the committed
 * {@code en_us.json} defines <b>0</b> of them, the D-016 pin BOOK-3a established.</p>
 *
 * <p>The 712th key is the point of t_6799fd3f: {@code VAMPBAT} page 0 carries
 * {@code textKey = "focus.upgrade.vampirebats.text"}, which the original lang
 * defines, and a derivation filtered to the {@code tc.} prefix cannot see it —
 * that row rendered as a raw key on an overlay build. The derivation is
 * therefore structural (graph <em>fields</em>), not a prefix walk.</p>
 */
class ResearchOriginalLangTest {

    private static final String GRAPH_RESOURCE = "/data/thaumcraft/research_graph/entries.json";
    private static final String COMMITTED_LANG_RESOURCE = "/assets/thaumcraft/lang/en_us.json";

    /**
     * {@code VAMPBAT} page 0's {@code textKey} — the graph's only display key
     * outside the {@code tc.} namespace, and the one a prefix-filtered
     * derivation loses (t_6799fd3f). The original lang defines it
     * ({@code en_US.lang}: "This upgrade changes the summoned bats…").
     */
    private static final String VAMPBAT_PAGE_KEY = "focus.upgrade.vampirebats.text";

    /** The 4 keys the original 4.2.3.5 lang never defined (BOOK-3b authors them). */
    private static final List<String> ORIGINAL_LANG_GAPS = List.of(
            "tc.research_name.CAP_iron",
            "tc.research_name.ROD_wood",
            "tc.research_text.CAP_iron",
            "tc.research_text.ROD_wood");

    // ------------------------------------------------------------------
    // the .lang parser
    // ------------------------------------------------------------------

    @Test
    void parsesCrlfCommentsEqualsEscapesAndMarkup() {
        String fixture = "\uFEFF# a comment\r\n"
                + "\r\n"
                + "tc.research_name.X=Plain name\r\n"
                // the value runs to end of line and may contain '='
                + "tc.research_text.X=Holding shift shows numbers = exact values\r\n"
                // Properties-style escapes decode; markup rides through verbatim
                + "tc.research_page.X.1=first line\\nsecond line\\ttabbed \u00e9\r\n"
                + "tc.research_page.X.2=<BR>\u00a75Arcane Workbench\u00a70 and the & code\r\n"
                // a line with no '=' is ignored; a duplicate key takes the last value
                + "not a key=value pair\r\n"
                + "unknown line without an equals sign\r\n"
                + "dup=first\r\n"
                + "dup=last\r\n";

        Map<String, String> lang = ResearchOriginalLang.parseLang(fixture);

        assertEquals("Plain name", lang.get("tc.research_name.X"));
        assertEquals("Holding shift shows numbers = exact values", lang.get("tc.research_text.X"));
        assertEquals("first line\nsecond line\ttabbed \u00e9", lang.get("tc.research_page.X.1"));
        assertEquals("<BR>\u00a75Arcane Workbench\u00a70 and the & code",
                lang.get("tc.research_page.X.2"));
        assertEquals("last", lang.get("dup"));
        assertFalse(lang.containsKey("unknown line without an equals sign"));
        assertEquals(6, lang.size(), () -> "parsed keys: " + lang.keySet());
    }

    @Test
    void keepsValueWhitespaceVerbatim() {
        // Measured on the real file: 17 lines carry a leading space after '=' and
        // 85 a trailing one. The conversion is verbatim either way.
        Map<String, String> lang = ResearchOriginalLang.parseLang("a= leading\r\nb=trailing \r\nc=\r\n");
        assertEquals(" leading", lang.get("a"));
        assertEquals("trailing ", lang.get("b"));
        assertEquals("", lang.get("c"));
    }

    @Test
    void decodesUnicodeEscapesAndUnknownEscapes() {
        Map<String, String> lang = ResearchOriginalLang.parseLang(
                "u=\\u00a7aGreen\\u00a7r\r\nk=\\: not a separator\r\nw=back\\\\slash\r\n");
        assertEquals("\u00a7aGreen\u00a7r", lang.get("u"));
        assertEquals(": not a separator", lang.get("k"));
        assertEquals("back\\slash", lang.get("w"));
    }

    // ------------------------------------------------------------------
    // the graph-key selector
    // ------------------------------------------------------------------

    @Test
    void derivesEveryDisplayKeyTheGraphRowsReference() throws IOException {
        Set<String> keys = ResearchOriginalLang.collectGraphKeys(readResource(GRAPH_RESOURCE));

        // 201 name + 201 text + 310 page = 712, the set the runtime graph exposes.
        // Of the 310 page keys, 309 are tc.research_page.* and 1 is not.
        assertEquals(712, keys.size(), "display keys the graph rows reference");
        assertEquals(201, count(keys, "tc.research_name."));
        assertEquals(201, count(keys, "tc.research_text."));
        assertEquals(309, count(keys, "tc.research_page."));
        assertEquals(1, count(keys, "focus.upgrade."),
                "the graph's one display key outside the tc. namespace");
        assertTrue(keys.contains(VAMPBAT_PAGE_KEY),
                "VAMPBAT page 0's textKey is a display key the generator must collect: "
                        + keys.stream().filter(k -> !k.startsWith("tc.")).toList());

        // the selector is a plain JSON walk over the graph's display-key FIELDS,
        // not a regex and not a prefix filter: cross-check it against Gson.
        assertEquals(gsonDisplayKeys(readResource(GRAPH_RESOURCE)), keys);
    }

    /**
     * The shape of the rule, on a graph small enough to read: collect the
     * {@code nameKey}/{@code textKey} of every row and the {@code textKey} of
     * every page — whatever namespace they live in — and collect nothing that
     * merely <em>looks</em> like a key.
     */
    @Test
    void collectsDisplayKeysStructurallyNotByPrefix() {
        String graph = "{\"formatVersion\": 1, \"entryCount\": 2, \"entries\": [\n"
                + "  {\"key\": \"A\", \"nameKey\": \"tc.research_name.A\",\n"
                + "   \"textKey\": \"tc.research_text.A\",\n"
                + "   \"icon\": {\"kind\": \"texture\", \"texture\": \"tc.not.a.display.key\"},\n"
                + "   \"parents\": [\"tc.research_text.GONE\"],\n"
                + "   \"pages\": [{\"type\": \"text\", \"textKey\": \"" + VAMPBAT_PAGE_KEY + "\"},\n"
                + "             {\"type\": \"image\", \"textKey\": \"tc.research_page.A.2\"},\n"
                + "             {\"type\": \"aspects\"}]},\n"
                + "  {\"key\": \"B\", \"nameKey\": \"focus.upgrade.bat.name\",\n"
                + "   \"textKey\": \"tc.research_text.B\", \"pages\": []}\n"
                + "]}";

        assertEquals(Set.of(
                        "tc.research_name.A", "tc.research_text.A",
                        VAMPBAT_PAGE_KEY, "tc.research_page.A.2",
                        "focus.upgrade.bat.name", "tc.research_text.B"),
                ResearchOriginalLang.collectGraphKeys(graph));
    }

    /**
     * D-016 option 2's output contract: the generated object carries a value for
     * <b>every</b> graph-referenced key the original lang defines, the
     * non-{@code tc.} one included, and nothing for the keys it never defined.
     */
    @Test
    void emitsAValueForEveryGraphKeyTheOriginalLangDefines() throws IOException {
        Set<String> graphKeys = ResearchOriginalLang.collectGraphKeys(readResource(GRAPH_RESOURCE));
        Map<String, String> lang = new LinkedHashMap<>();
        for (String key : graphKeys) {
            if (!ORIGINAL_LANG_GAPS.contains(key)) {
                lang.put(key, "value of " + key);
            }
        }

        Map<String, String> generated = ResearchOriginalLang.selectGraphKeys(graphKeys, lang);

        assertEquals(708, generated.size(), "712 graph keys minus the 4 the lang never defines");
        assertTrue(generated.containsKey(VAMPBAT_PAGE_KEY),
                "the generator's output must name the key the prefix filter used to lose");
        assertEquals("value of " + VAMPBAT_PAGE_KEY, generated.get(VAMPBAT_PAGE_KEY));
        // every key the graph references and the lang defines, and only those
        Set<String> expected = new TreeSet<>(graphKeys);
        expected.removeAll(ORIGINAL_LANG_GAPS);
        assertEquals(expected, new TreeSet<>(generated.keySet()));
        // the writer round-trips the 708 including the non-tc. key
        assertEquals(generated,
                ResearchOriginalLang.parseFlatJson(ResearchOriginalLang.writeJson(generated)));
    }

    @Test
    void reportsTheFourKeysTheOriginalLangNeverDefined() throws IOException {
        Set<String> graphKeys = ResearchOriginalLang.collectGraphKeys(readResource(GRAPH_RESOURCE));
        // A synthesized lang: every graph key except the 4 the original never had.
        Map<String, String> lang = new LinkedHashMap<>();
        for (String key : graphKeys) {
            if (!ORIGINAL_LANG_GAPS.contains(key)) {
                lang.put(key, "value of " + key);
            }
        }

        // the tool reports the gaps in key order
        assertEquals(ORIGINAL_LANG_GAPS, ResearchOriginalLang.missingGraphKeys(graphKeys, lang));
        assertEquals(708, ResearchOriginalLang.selectGraphKeys(graphKeys, lang).size());
    }

    // ------------------------------------------------------------------
    // the writer
    // ------------------------------------------------------------------

    @Test
    void writesSortedValidJsonWithMarkupIntact() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("tc.research_page.X.2", "<BR>\u00a75Arcane Workbench\u00a70 and the & code");
        values.put("tc.research_name.X", "Plain \"quoted\" name");
        values.put("tc.research_text.X", "first\nsecond");
        values.put("a.key", "with a backslash \\ and a \u00e9");

        String json = ResearchOriginalLang.writeJson(values);

        assertFalse(json.startsWith("\uFEFF"), "no BOM");
        assertEquals("{\n"
                + "  \"a.key\": \"with a backslash \\\\ and a \u00e9\",\n"
                + "  \"tc.research_name.X\": \"Plain \\\"quoted\\\" name\",\n"
                + "  \"tc.research_page.X.2\": \"<BR>\u00a75Arcane Workbench\u00a70 and the & code\",\n"
                + "  \"tc.research_text.X\": \"first\\nsecond\"\n"
                + "}\n", json);
        // valid JSON by an independent reader, with the keys sorted
        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(new TreeSet<>(values.keySet()), new TreeSet<>(parsed.keySet()));
        // and the tool's own reader round-trips it
        assertEquals(values, ResearchOriginalLang.parseFlatJson(json));
    }

    @Test
    void writesAnEmptyObjectRatherThanNothing() {
        assertEquals("{}\n", ResearchOriginalLang.writeJson(Map.of()));
        assertEquals(Map.of(), ResearchOriginalLang.parseFlatJson("{}\n"));
    }

    // ------------------------------------------------------------------
    // D-016: nothing original is committed
    // ------------------------------------------------------------------

    @Test
    void committedLangDefinesNoneOfTheGraphKeys() throws IOException {
        Set<String> graphKeys = ResearchOriginalLang.collectGraphKeys(readResource(GRAPH_RESOURCE));
        Map<String, String> committed =
                ResearchOriginalLang.parseFlatJson(readResource(COMMITTED_LANG_RESOURCE));

        assertTrue(committed.size() > 300, "the committed lang is the port-authored key list");
        Set<String> overlap = new TreeSet<>(graphKeys);
        overlap.retainAll(committed.keySet());
        assertEquals(Set.of(), overlap,
                "D-016 option 2: no graph-referenced research string is committed");
    }

    @Test
    void theGeneratedFileIsNotCommitted() {
        // It is a build output (build/generated/resources/research-lang/...), so a
        // copy under src/main/resources would make the code-only jar carry the 712
        // again. The source tree, not the build output, is what is asserted here.
        String assetsDir = System.getProperty("tc.modAssetsDir");
        assertNotNull(assetsDir, "system property 'tc.modAssetsDir' is not set — run tests through ./gradlew test");
        Path committed = Path.of(assetsDir, "thaumcraft/lang/research_original.json");
        assertFalse(Files.exists(committed),
                committed + " must not be committed (D-016 option 2 generates it at build time)");
    }

    // ------------------------------------------------------------------

    private static int count(Set<String> keys, String prefix) {
        return (int) keys.stream().filter(key -> key.startsWith(prefix)).count();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = ResearchOriginalLangTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Every display key the graph rows reference — entry {@code nameKey} /
     * {@code textKey} and every page {@code textKey} — collected with Gson
     * instead of the tool's reader. Structural on both sides: no prefix filter.
     */
    private static Set<String> gsonDisplayKeys(String json) {
        Set<String> keys = new TreeSet<>();
        JsonElement document = JsonParser.parseString(json);
        for (JsonElement row : document.getAsJsonObject().getAsJsonArray("entries")) {
            JsonObject entry = row.getAsJsonObject();
            addDisplayKey(keys, entry, "nameKey");
            addDisplayKey(keys, entry, "textKey");
            JsonElement pages = entry.get("pages");
            if (pages != null && pages.isJsonArray()) {
                for (JsonElement page : pages.getAsJsonArray()) {
                    if (page.isJsonObject()) {
                        addDisplayKey(keys, page.getAsJsonObject(), "textKey");
                    }
                }
            }
        }
        return keys;
    }

    private static void addDisplayKey(Set<String> keys, JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && !value.getAsString().isBlank()) {
            keys.add(value.getAsString());
        }
    }
}
