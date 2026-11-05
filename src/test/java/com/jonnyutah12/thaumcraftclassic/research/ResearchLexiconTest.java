package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.tools.ResearchOriginalLang;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probes for the reader half of D-016 option 2 (BOOK-3a follow-up,
 * t_28ce4d49): the rules that decide what a research key displays.
 *
 * <p>No Minecraft type appears in the class under test, so all of it is
 * decidable in a plain JVM: the parse, the merge math, the resolution order,
 * the path contract with the generator, and the D-016 pin that the committed
 * {@code en_us.json} still defines <b>0</b> of the graph's 712 keys — the
 * consumer must not become a reason to commit the strings.</p>
 */
class ResearchLexiconTest {

    private static final String GRAPH_RESOURCE = "/data/thaumcraft/research_graph/entries.json";
    private static final String COMMITTED_LANG_RESOURCE = "/assets/thaumcraft/lang/en_us.json";

    // ------------------------------------------------------------------
    // the reader
    // ------------------------------------------------------------------

    @Test
    void readsExactlyWhatTheGeneratorWrites() {
        // Feed the generator's own writer: the reader is proved against the
        // writer's real bytes, not against a hand-typed approximation.
        Map<String, String> written = new LinkedHashMap<>();
        written.put("tc.research_name.BASICS", "Thaumaturgy 101");
        written.put("tc.research_text.BASICS", "Read the book, learn the plot");
        written.put("tc.research_page.BASICS.1", "<BR>\u00a75Arcane Workbench\u00a70 and the & code");
        written.put("tc.research_page.BASICS.2", "first\nsecond with \"quotes\" and a \\ backslash");

        ResearchLexicon lexicon = ResearchLexicon.fromJson(ResearchOriginalLang.writeJson(written));

        assertEquals(written.keySet(), lexicon.keys());
        assertEquals("Thaumaturgy 101", lexicon.value("tc.research_name.BASICS"));
        // BD-22: the 1.7.10 page markup rides through verbatim — the reader
        // renders nothing, so the bytes a renderer (BOOK-3b) will consume are intact.
        assertEquals("<BR>\u00a75Arcane Workbench\u00a70 and the & code",
                lexicon.value("tc.research_page.BASICS.1"));
        assertEquals("first\nsecond with \"quotes\" and a \\ backslash",
                lexicon.value("tc.research_page.BASICS.2"));
        assertFalse(lexicon.isEmpty());
        assertEquals(4, lexicon.size());
    }

    @Test
    void toleratesEveryShapeABrokenResourceCanHave() {
        // A language resource is user-visible (pack overlays ship one), and the
        // reader runs on the client reload thread: nothing here may throw.
        assertEquals(0, ResearchLexicon.empty().size());
        assertEquals(0, ResearchLexicon.fromJson("").size());
        assertEquals(0, ResearchLexicon.fromJson("   ").size());
        assertEquals(0, ResearchLexicon.fromJson(null).size());
        assertEquals(0, ResearchLexicon.fromJson("{").size(), "truncated JSON");
        assertEquals(0, ResearchLexicon.fromJson("[]").size(), "not an object");
        assertEquals(0, ResearchLexicon.fromJson("null").size());
        assertEquals(0, ResearchLexicon.fromJson("{\"tc.research_name.X\":[1,2]}").size(),
                "a non-primitive value is no entry");
        // primitives that are not strings still read as text, the way Gson does
        assertEquals("1", ResearchLexicon.fromJson("{\"a\":1}").value("a"));
        assertNull(ResearchLexicon.fromJson("{}").value("a"));
    }

    @Test
    void readsFromAStreamAsFromAString() throws IOException {
        String json = ResearchOriginalLang.writeJson(Map.of("tc.research_name.X", "X name"));
        ResearchLexicon fromStream =
                ResearchLexicon.fromStream(new java.io.ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals("X name", fromStream.value("tc.research_name.X"));
        assertEquals(1, fromStream.size());
    }

    // ------------------------------------------------------------------
    // the path contract with the generator — the reader can drift from the
    // writer silently, and every gate stays green when it does
    // ------------------------------------------------------------------

    @Test
    void readsThePathTheGeneratorWrites() {
        assertEquals(ResearchOriginalLang.GENERATED_RESOURCE_PATH, ResearchLexicon.GENERATED_ASSET_PATH,
                "the reader must read exactly the file BOOK-3a writes");
        assertEquals("assets/thaumcraft/lang/research_original.json", ResearchLexicon.GENERATED_ASSET_PATH);
        assertEquals("thaumcraft", ResearchLexicon.NAMESPACE);
        assertEquals("lang/research_original.json", ResearchLexicon.GENERATED_RESOURCE_PATH,
                "the ResourceLocation form drops the assets/<ns>/ prefix");
        assertEquals(ResearchLexicon.GENERATED_ASSET_PATH,
                ResearchLexicon.ASSETS_PREFIX + ResearchLexicon.GENERATED_RESOURCE_PATH);
    }

    // ------------------------------------------------------------------
    // the merge and the resolution order
    // ------------------------------------------------------------------

    @Test
    void mergeKeepsEveryLiveKeyAndLetsTheOriginalOverlayIt() {
        Map<String, String> live = new LinkedHashMap<>();
        live.put("item.thaumcraft.itemthaumonomicon", "Thaumonomicon");
        live.put("tc.research_name.SHARED", "committed label");
        Map<String, String> generated = new LinkedHashMap<>();
        generated.put("tc.research_name.SHARED", "the original label");
        generated.put("tc.research_text.SHARED", "the original hover text");

        Map<String, String> merged = ResearchLexicon.merged(live, generated);

        assertEquals(3, merged.size());
        assertEquals("the original label", merged.get("tc.research_name.SHARED"),
                "the generated original wins on a collision");
        assertEquals("the original hover text", merged.get("tc.research_text.SHARED"));
        assertEquals("Thaumonomicon", merged.get("item.thaumcraft.itemthaumonomicon"),
                "no live key is dropped by the merge");
        assertEquals(2, live.size(), "the base map is not mutated by the merge");
        // The map the client renders from is frozen.
        assertThrowsOnPut(merged);
    }

    @Test
    void displayAnswersTheOriginalThenTheCommittedEntryThenTheKey() {
        Map<String, String> generated = new LinkedHashMap<>();
        generated.put("tc.research_name.BASICS", "Thaumaturgy 101");
        ResearchLexicon lexicon = ResearchLexicon.of(generated);

        // 1. an overlay build: the player's own original string.
        assertEquals("Thaumaturgy 101", lexicon.display("tc.research_name.BASICS", "tc.research_name.BASICS"));
        // even if the live language also answers something for that key
        assertEquals("Thaumaturgy 101", lexicon.display("tc.research_name.BASICS", "a committed label"));
        // 2. a key the generated file has not (the 4 BOOK-3b authors): the
        //    committed entry the live language carries.
        assertEquals("Iron Cap", lexicon.display("tc.research_name.CAP_iron", "Iron Cap"));
        // 3. a key nobody defines (every research key in a code-only build):
        //    the key itself, the intended D-016 outcome.
        assertEquals("tc.research_name.CAP_iron",
                lexicon.display("tc.research_name.CAP_iron", "tc.research_name.CAP_iron"));
        assertEquals("tc.research_name.NO_SUCH", lexicon.display("tc.research_name.NO_SUCH", null));
        // and the code-only lexicon has no opinion at all
        assertEquals("tc.research_name.BASICS",
                ResearchLexicon.empty().display("tc.research_name.BASICS", "tc.research_name.BASICS"));
    }

    // ------------------------------------------------------------------
    // D-016: the consumer must not become a reason to commit the strings
    // ------------------------------------------------------------------

    @Test
    void committedLangKeepsZeroOfTheGraphKeys() throws IOException {
        Set<String> graphKeys = ResearchLexicon.graphKeys(readResource(GRAPH_RESOURCE));
        assertEquals(712, graphKeys.size(),
                "the pin is measured against the graph's real key set");
        Map<String, String> committed =
                ResearchOriginalLang.parseFlatJson(readResource(COMMITTED_LANG_RESOURCE));

        Set<String> overlap = new TreeSet<>(graphKeys);
        overlap.retainAll(committed.keySet());
        assertEquals(Set.of(), overlap,
                "D-016 option 2: the committed lang must keep 0 of the 712 research keys "
                        + "so the generated file stays the only carrier of the original strings");
    }

    @Test
    void theFileItReadsIsNotCommitted() {
        // If the generated file sat in src/main/resources, this reader would be
        // satisfied by a committed copy of the original strings — the exact
        // thing D-016 option 2 forbids, and the code-only artifact would carry
        // 708 of them.
        String assetsDir = System.getProperty("tc.modAssetsDir");
        assertNotNull(assetsDir, "system property 'tc.modAssetsDir' is not set — run tests through ./gradlew test");
        assertFalse(Files.exists(Path.of(assetsDir, "thaumcraft/lang/research_original.json")),
                "the research lang is a build output; it must not be committed");
    }

    @Test
    void theGeneratedFileOnTheRuntimeClasspathIsGraphKeysOnly() throws IOException {
        // The reader's input is the generated resource. In a tree that has the
        // reference lang the build generates it, and the subset contract below
        // is what stops a generated file from inventing keys; a code-only
        // build ships no such entry at all (the D-016 posture), so that build
        // skips this probe — the same shape as the pre-existing
        // reference-absence probes. The classloader route is the one the
        // dedicated server has, and the client merge falls back to it.
        try (InputStream in = ResearchLexiconTest.class
                .getResourceAsStream("/" + ResearchLexicon.GENERATED_ASSET_PATH)) {
            org.junit.jupiter.api.Assumptions.assumeTrue(in != null,
                    "no generated research lang on this classpath — a code-only build ships 0 of the graph keys");
        }
        final ResearchLexicon lexicon = ResearchLexicon.fromClasspath(ResearchLexicon.GENERATED_ASSET_PATH);
        final Set<String> graphKeys = ResearchLexicon.graphKeys(readResource(GRAPH_RESOURCE));
        assertTrue(lexicon.size() > 0, "the classpath route reads the bytes that are there");
        Set<String> invented = new HashSet<>(lexicon.keys());
        invented.removeAll(graphKeys);
        assertEquals(Set.of(), invented, "the generated file may only carry graph-referenced keys");
        for (String key : lexicon.keys()) {
            String value = lexicon.value(key);
            assertTrue(value != null && !value.isBlank(), key + " resolves to blank text");
            assertFalse(key.equals(value), key + " resolves to its own key");
        }
        // The route is tolerant: an unreadable path is an empty lexicon, not a throw.
        assertEquals(0, ResearchLexicon.fromClasspath("assets/thaumcraft/lang/no_such_file.json").size());
    }

    // ------------------------------------------------------------------

    private static void assertThrowsOnPut(Map<String, String> map) {
        try {
            map.put("x", "y");
            throw new AssertionError("the merged language map must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = ResearchLexiconTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
