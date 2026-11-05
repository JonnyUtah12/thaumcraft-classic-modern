package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.ResearchLexicon;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probes for the client merge (BOOK-3a follow-up, t_28ce4d49).
 *
 * <p>The unit-test source set runs on the Minecraft/NeoForge classpath, so the
 * merge is proved against the <b>real</b> language API: {@code Language.inject}
 * and {@code TranslatableContents}, the two pieces a research screen's
 * {@code Component.translatable(entry.nameKey())} actually runs through. No
 * {@code Screen} is constructed (the lane rule: the measurable half lives
 * outside the screen), and every probe restores the live language it touched,
 * because {@code Language.instance} is one static for the whole JVM.</p>
 */
class ResearchOriginalLanguageTest {

    private static final String NAME_KEY = "tc.research_name.BASICS";
    private static final String TEXT_KEY = "tc.research_text.BASICS";
    private static final String PAGE_KEY = "tc.research_page.BASICS.1";
    private static final String NAME_VALUE = "Thaumaturgy 101";
    private static final String COMMITTED_KEY = "item.thaumcraft.itemthaumonomicon";

    /** The client class in internal-name form, for the dist-safety scan. */
    private static final String CLIENT_CLASS_INTERNAL =
            "com/jonnyutah12/thaumcraftclassic/client/ResearchOriginalLanguage";
    private static final String CLIENT_PACKAGE_INTERNAL = "com/jonnyutah12/thaumcraftclassic/client/";

    // ------------------------------------------------------------------

    @Test
    void mergedLanguageOverlaysTheGeneratedMapAndInheritsTheBehaviour() {
        Map<String, String> live = new LinkedHashMap<>();
        live.put(COMMITTED_KEY, "Thaumonomicon");
        live.put(NAME_KEY, "a committed label");
        Component componentForm = Component.literal("component-form translation");
        Language base = fakeLanguage(live, true, "component.key", componentForm);

        Map<String, String> generated = new LinkedHashMap<>();
        generated.put(NAME_KEY, NAME_VALUE);
        generated.put(PAGE_KEY, "<BR>\u00a75Arcane Workbench\u00a70");

        Language merged = ResearchOriginalLanguage.mergedLanguage(base, generated);

        // the generated original answers, and wins on a collision with the committed file
        assertEquals(NAME_VALUE, merged.getOrDefault(NAME_KEY));
        assertEquals("<BR>\u00a75Arcane Workbench\u00a70", merged.getOrDefault(PAGE_KEY));
        // every live key survives the merge: no item/block/container label is lost
        assertEquals("Thaumonomicon", merged.getOrDefault(COMMITTED_KEY));
        assertEquals("tc.research_text.BASICS", merged.getOrDefault(TEXT_KEY),
                "an undefined key keeps the language's own answer (the key itself)");
        assertTrue(merged.has(NAME_KEY));
        assertTrue(!merged.has("tc.research_name.NO_SUCH"));
        // the behavioural half is delegated, not replaced — this is why an RTL
        // locale keeps its bidi reordering and its flag after the merge.
        assertTrue(merged.isDefaultRightToLeft(), "the RTL flag is inherited from the base");
        assertSame(base.getVisualOrder(FormattedText.of("x")), merged.getVisualOrder(FormattedText.of("x")),
                "getVisualOrder delegates to the base (a ClientLanguage bidi-reorders)");
        assertSame(componentForm, merged.getComponent("component.key"));
        assertNull(merged.getComponent("component.other"));
        // the merged map carries both halves, and the base is not mutated
        assertEquals(3, merged.getLanguageData().size());
        assertEquals(2, live.size());
        try {
            merged.getLanguageData().put("x", "y");
            throw new AssertionError("the merged language map must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    void injectingTheMergedLanguageMakesTranslatableComponentsResolve() {
        // The end of the chain a book screen uses: a translatable component
        // resolves through Language.getInstance(), and re-decomposes when that
        // instance's identity changes. This is the proof the raw
        // tc.research_* keys stop rendering as keys.
        final Language before = Language.getInstance();
        try {
            assertTrue(!before.has(NAME_KEY),
                    "the live language must not define the research key (D-016: nothing committed)");
            assertEquals(NAME_KEY, Component.translatable(NAME_KEY).getString(),
                    "unmerged: the component renders its own key");

            Language merged = ResearchOriginalLanguage.mergedLanguage(before, Map.of(NAME_KEY, NAME_VALUE));
            Language.inject(merged);

            assertSame(merged, Language.getInstance());
            assertEquals(NAME_VALUE, Component.translatable(NAME_KEY).getString());
            assertEquals(NAME_VALUE, merged.getOrDefault(NAME_KEY));
            // what the live language already answered still answers
            List<String> liveKeys = new ArrayList<>(before.getLanguageData().keySet());
            if (!liveKeys.isEmpty()) {
                String probe = liveKeys.get(0);
                assertEquals(before.getOrDefault(probe), Language.getInstance().getOrDefault(probe),
                        "the merge preserves the live language's answer for " + probe);
            }
        } finally {
            Language.inject(before);
        }
        assertSame(before, Language.getInstance(), "the live language is restored for the rest of the suite");
    }

    @Test
    void aSeededLexiconMergesIntoTheLiveLanguageAndIsIdempotent() {
        final Language before = Language.getInstance();
        try {
            // A live language in the shape a client build has: the committed
            // lang keys, no research keys.
            Map<String, String> live = new LinkedHashMap<>();
            live.put(COMMITTED_KEY, "Thaumonomicon");
            Language.inject(fakeLanguage(live, false, null, null));

            Map<String, String> generated = new LinkedHashMap<>();
            generated.put(NAME_KEY, NAME_VALUE);
            generated.put(TEXT_KEY, "Read the book, learn the plot");
            ResearchOriginalLanguage.installLexicon(ResearchLexicon.of(generated));

            Language merged = ResearchOriginalLanguage.applyMerge();
            assertNotNull(merged, "a non-empty lexicon merges");
            assertSame(merged, Language.getInstance(), "the merged instance is the live language");
            assertNull(ResearchOriginalLanguage.applyMerge(), "idempotent: nothing applied twice");
            assertEquals(NAME_VALUE, ResearchOriginalLanguage.display(NAME_KEY));
            assertEquals("Read the book, learn the plot", ResearchOriginalLanguage.display(TEXT_KEY));
            // the committed file's own keys keep answering through the same language
            assertEquals("Thaumonomicon", Language.getInstance().getOrDefault(COMMITTED_KEY));
            // a key nobody defines answers its own key (BOOK-3b has not authored it)
            assertEquals("tc.research_name.CAP_iron", ResearchOriginalLanguage.display("tc.research_name.CAP_iron"));
            // and the display component is the resolved text, not a key
            assertEquals(NAME_VALUE, ResearchOriginalLanguage.displayComponent(NAME_KEY).getString());
        } finally {
            ResearchOriginalLanguage.installLexicon(ResearchLexicon.empty());
            Language.inject(before);
        }
    }

    @Test
    void anEmptyLexiconMergesNothing() {
        // The code-only build: the generated resource is absent, so the merge
        // must leave the client language exactly vanilla's. D-016 says no
        // original string ships; it says nothing about rewriting the language.
        final Language before = Language.getInstance();
        try {
            Map<String, String> live = new LinkedHashMap<>();
            live.put(COMMITTED_KEY, "Thaumonomicon");
            Language base = fakeLanguage(live, false, null, null);
            Language.inject(base);
            ResearchOriginalLanguage.installLexicon(ResearchLexicon.empty());

            assertNull(ResearchOriginalLanguage.applyMerge(), "nothing to merge, nothing injected");
            assertSame(base, Language.getInstance(), "the live language is left exactly as it was");
            assertEquals("tc.research_name.BASICS", ResearchOriginalLanguage.display(NAME_KEY),
                    "a code-only build renders the raw key — the intended D-016 outcome");
            assertEquals("Thaumonomicon", ResearchOriginalLanguage.display(COMMITTED_KEY),
                    "the committed port-authored keys still resolve");
        } finally {
            ResearchOriginalLanguage.installLexicon(ResearchLexicon.empty());
            Language.inject(before);
        }
    }

    // ------------------------------------------------------------------
    // dist safety: a dedicated server must never load the merge
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("removal") // EventBusSubscriber.bus()/Bus are marked for removal; the tree registers on them
    void bothSubscribersAreClientOnly() {
        EventBusSubscriber outer = ResearchOriginalLanguage.class.getAnnotation(EventBusSubscriber.class);
        assertNotNull(outer, "the merge class must be an EventBusSubscriber");
        assertEquals(EventBusSubscriber.Bus.MOD, outer.bus(),
                "the reload-listener registration is a mod-bus event");
        assertEquals(1, outer.value().length);
        assertEquals(Dist.CLIENT, outer.value()[0], "the merge class is client-only");

        EventBusSubscriber guard =
                ResearchOriginalLanguage.TickGuard.class.getAnnotation(EventBusSubscriber.class);
        assertNotNull(guard, "the tick guard must be an EventBusSubscriber");
        assertEquals(EventBusSubscriber.Bus.GAME, guard.bus(), "the tick guard runs on the game bus");
        assertEquals(1, guard.value().length);
        assertEquals(Dist.CLIENT, guard.value()[0], "the tick guard is client-only");
    }

    @Test
    void noServerSideClassNamesTheClientMerge() throws IOException {
        // The lane's dist-safety rule, on bytecode: nothing outside client/ may
        // reference the merge, so runServer / runGameTestServer never load a
        // client-only type. The gametest probe reads the resource through the
        // ResourceManager and ResearchLexicon only.
        final String value = System.getProperty("tc.mainClassesDir");
        assertNotNull(value, "tc.mainClassesDir system property not set (see build.gradle test task)");
        final Path root = Path.of(value);
        assertTrue(Files.isDirectory(root), "main classes directory not built: " + root);

        final List<String> offenders = new ArrayList<>();
        final List<String> found = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (final Path classFile : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                final String relative =
                        root.relativize(classFile).toString().replace(java.io.File.separatorChar, '/');
                if (new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1)
                        .contains(CLIENT_CLASS_INTERNAL)) {
                    found.add(relative);
                    if (!relative.startsWith(CLIENT_PACKAGE_INTERNAL)) {
                        offenders.add(relative);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "classes outside client/ reference the client research-lang merge: " + offenders);
        assertTrue(found.contains(CLIENT_PACKAGE_INTERNAL + "ResearchOriginalLanguage.class"),
                "the merge class is missing from the compiled output: " + found);
        assertTrue(found.stream().anyMatch(c -> c.contains("TickGuard")),
                "the tick guard is missing from the compiled output: " + found);
    }

    // ------------------------------------------------------------------

    /** A stand-in for a {@code ClientLanguage}: a map, a flag, and sentinels. */
    private static Language fakeLanguage(Map<String, String> map, boolean rightToLeft,
                                         String componentKey, Component component) {
        final FormattedCharSequence sentinel = sink -> true;
        return new Language() {
            @Override
            public String getOrDefault(String key, String defaultValue) {
                return map.getOrDefault(key, defaultValue);
            }

            @Override
            public boolean has(String id) {
                return map.containsKey(id);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return rightToLeft;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return sentinel;
            }

            @Override
            public Map<String, String> getLanguageData() {
                return Map.copyOf(map);
            }

            @Override
            public Component getComponent(String key) {
                return component != null && key.equals(componentKey) ? component : null;
            }
        };
    }
}
