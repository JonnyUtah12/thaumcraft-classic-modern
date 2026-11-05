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
 * graph references exactly 711 {@code tc.*} keys (201 name / 201 text / 309
 * page), and the committed {@code en_us.json} defines <b>0</b> of them — the
 * D-016 pin this card exists to establish.</p>
 */
class ResearchOriginalLangTest {

    private static final String GRAPH_RESOURCE = "/data/thaumcraft/research_graph/entries.json";
    private static final String COMMITTED_LANG_RESOURCE = "/assets/thaumcraft/lang/en_us.json";

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
    void derivesSevenHundredElevenKeysFromTheShippedGraph() throws IOException {
        Set<String> keys = ResearchOriginalLang.collectGraphKeys(readResource(GRAPH_RESOURCE));

        assertEquals(711, keys.size(), "graph-referenced tc.* keys");
        assertEquals(201, count(keys, "tc.research_name."));
        assertEquals(201, count(keys, "tc.research_text."));
        assertEquals(309, count(keys, "tc.research_page."));

        // the selector is a plain JSON walk, not a regex: cross-check it against
        // Gson on the real 402 KB graph.
        assertEquals(gsonTcStrings(readResource(GRAPH_RESOURCE)), keys);
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
        assertEquals(707, ResearchOriginalLang.selectGraphKeys(graphKeys, lang).size());
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
        // copy under src/main/resources would make the code-only jar carry the 711
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

    /** Every {@code tc.*} string, collected with Gson instead of the tool's reader. */
    private static Set<String> gsonTcStrings(String json) {
        Set<String> keys = new TreeSet<>();
        collect(JsonParser.parseString(json), keys);
        return keys;
    }

    private static void collect(JsonElement element, Set<String> keys) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collect(entry.getValue(), keys);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collect(child, keys);
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith(ResearchOriginalLang.GRAPH_KEY_PREFIX)) {
                keys.add(value);
            }
        }
    }
}
