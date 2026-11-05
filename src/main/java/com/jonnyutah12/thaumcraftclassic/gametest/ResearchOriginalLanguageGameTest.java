package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchLexicon;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.tools.ResearchOriginalLang;
import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The runtime half of the D-016 option 2 consumer (BOOK-3a follow-up,
 * t_28ce4d49): the generated research lang must be reachable from a running
 * game through the route the client merge reads, and it must resolve the graph
 * the server ships.
 *
 * <p>The merge itself is client-only ({@code client/ResearchOriginalLanguage},
 * {@code Dist.CLIENT}) and this harness is a dedicated server that never
 * constructs a {@code Screen}, so these probes name no client class — the
 * bytecode scan in
 * {@code ResearchOriginalLanguageTest.noServerSideClassNamesTheClientMerge}
 * enforces that. What is decidable headlessly is the input side: the shipped
 * file, the shipped graph, the keys each one covers, and the committed lang.</p>
 *
 * <p><b>Two routes, measured.</b> The probes read the lang resources through
 * the classloader route (the route NeoForge's own
 * {@code I18nManager.loadTranslations} uses). The {@code ResourceManager}
 * route is tried first, exactly as the client merge does it, and the fact that
 * it returns an empty stack for {@code thaumcraft:lang/en_us.json} on this
 * harness is itself a measured result — it is why the merge has a classloader
 * fallback, and probe two prints which route supplied the file.</p>
 *
 * <p>Every expectation is <b>derived</b> from the shipped data: the key sets
 * come from {@link ResearchManager#graph()} and from the graph JSON through
 * {@link ResearchLexicon#graphKeys(String)}; the set of keys the generated
 * file cannot serve is derived from the graph rows that carry no original
 * string plus the keys the generator's own
 * {@link ResearchOriginalLang#GRAPH_KEY_PREFIX} filter cannot see. No display
 * string and no key list is hardcoded here.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchOriginalLanguageGameTest {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** {@code thaumcraft:lang/research_original.json} — what BOOK-3a generates. */
    private static final ResourceLocation GENERATED =
            ResourceLocation.fromNamespaceAndPath(ResearchLexicon.NAMESPACE,
                    ResearchLexicon.GENERATED_RESOURCE_PATH);
    private static final String GENERATED_ON_CLASSPATH = "/" + ResearchLexicon.GENERATED_ASSET_PATH;
    private static final String COMMITTED_LANG_ON_CLASSPATH =
            "/" + ResearchLexicon.ASSETS_PREFIX + "lang/en_us.json";
    private static final String GRAPH_JSON_ON_CLASSPATH =
            "/data/" + ThaumcraftClassic.MOD_ID + "/research_graph/entries.json";
    /** {@code thaumcraft:lang/en_us.json} as a resource location. */
    private static final ResourceLocation COMMITTED_LANG_LOCATION =
            ResourceLocation.fromNamespaceAndPath(ResearchLexicon.NAMESPACE, "lang/en_us.json");

    /**
     * The generated file reaches a running game, and every string in it
     * resolves the shipped graph.
     */
    @GameTest(template = "empty")
    public static void generatedResearchLangResolvesTheShippedGraph(final GameTestHelper helper) {
        final ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        final ResearchLexicon lexicon = readGenerated(resources);
        final ResearchGraph graph = ResearchManager.graph();
        final Set<String> runtimeKeys = graphKeys(graph);
        final String graphJson = readClasspathText(GRAPH_JSON_ON_CLASSPATH);
        helper.assertTrue(graphJson != null, GRAPH_JSON_ON_CLASSPATH + " is on the runtime classpath");
        final Set<String> buildKeys = ResearchLexicon.graphKeys(graphJson);

        // The build-time derivation is the 711 every doc cites; the runtime
        // graph must cover it, key for key.
        helper.assertTrue(buildKeys.size() == 711,
                "the build-time derivation over the shipped graph JSON must measure 711 keys, measured "
                        + buildKeys.size());
        final Set<String> notAtRuntime = new TreeSet<>(buildKeys);
        notAtRuntime.removeAll(runtimeKeys);
        helper.assertTrue(notAtRuntime.isEmpty(),
                "the runtime graph must reference every key the generator built the file for, "
                        + "missing " + notAtRuntime);

        if (lexicon.isEmpty()) {
            // A code-only artifact carries 0 of them by design (D-016 option
            // 2) and the merge injects nothing on such a build.
            LOGGER.info("TC-RESEARCH-LANG probe: {} reaches the runtime with 0 keys — code-only "
                    + "posture, 0 of {} graph keys resolve", GENERATED, runtimeKeys.size());
            helper.assertTrue(graph.entry("BASICS") != null, "the graph itself is shipped");
            helper.succeed();
            return;
        }

        // A generated lang that invents keys is a leak, not a consumer.
        final Set<String> invented = new TreeSet<>(lexicon.keys());
        invented.removeAll(runtimeKeys);
        helper.assertTrue(invented.isEmpty(),
                "the generated research lang carries keys the shipped graph does not reference: "
                        + invented);

        // The raw-key defect this card exists to close, measured per key.
        for (final String key : lexicon.keys()) {
            final String value = lexicon.value(key);
            helper.assertTrue(value != null && !value.isBlank(), key + " resolves to blank text");
            helper.assertTrue(!key.equals(value), key + " resolves to its own key");
        }

        // Fidelity against the graph's own rows — same source, so they agree.
        // Measured rule: the graph row is the same original string with
        // surrounding whitespace trimmed (the original lang carries 14 name
        // values with a trailing space), so byte-equality is reported, not
        // required; a difference beyond whitespace is a real defect.
        int checked = 0;
        int byteExact = 0;
        int whitespaceOnly = 0;
        int markup = 0;
        for (final ResearchEntryData entry : graph.entries()) {
            final String original = entry.nameEnUs();
            if (original == null) {
                continue;
            }
            final String resolved = lexicon.value(entry.nameKey());
            if (resolved == null) {
                continue;
            }
            if (resolved.equals(original)) {
                byteExact++;
            } else if (resolved.strip().equals(original.strip())) {
                whitespaceOnly++;
            } else {
                helper.assertTrue(false, entry.nameKey() + " resolved to \"" + resolved
                        + "\", the graph row says \"" + original + "\" (differs beyond whitespace)");
            }
            checked++;
            if (original.indexOf('\u00a7') >= 0 || original.indexOf('<') >= 0) {
                markup++;
            }
        }
        helper.assertTrue(checked >= 150,
                "the name-fidelity cross-check must cover the graph's named entries, measured " + checked);

        // The closed gap set, derived. Everything the graph references that the
        // generated file cannot serve must be explicable from the shipped data:
        // a graph row with no original string (BOOK-3b authors those), or a key
        // the generator's GRAPH_KEY_PREFIX filter cannot see (measured here).
        final Set<String> explicable = explicableGaps(graph, runtimeKeys);
        final Set<String> missing = new TreeSet<>(runtimeKeys);
        missing.removeAll(lexicon.keys());
        helper.assertTrue(explicable.equals(missing),
                "the graph keys the generated file cannot serve must be exactly the rows with no "
                        + "original string plus the keys the generator's prefix filter cannot see; "
                        + "unexpected " + diff(missing, applicable(missing, explicable))
                        + ", over-explained " + diff(explicable, missing));

        LOGGER.info("TC-RESEARCH-LANG probe: {} reaches the runtime with {} keys; runtime graph "
                        + "references {} keys, the build-time derivation {} (delta {}); {} name values "
                        + "cross-checked against the shipped rows ({} byte-exact, {} differing only in "
                        + "surrounding whitespace, {} carrying original markup); unresolved set {} = "
                        + "no-original-string + prefix-invisible keys",
                GENERATED, lexicon.size(), runtimeKeys.size(), buildKeys.size(),
                diff(runtimeKeys, buildKeys), checked, byteExact, whitespaceOnly, markup, missing);
        helper.succeed();
    }

    /**
     * D-016 at runtime: the committed {@code en_us.json} the running game
     * reads defines none of the keys the generated file is the only carrier
     * of. Non-vacuous on every leg, code-only included.
     */
    @GameTest(template = "empty")
    public static void committedLangCarriesNoneOfTheGraphKeys(final GameTestHelper helper) {
        final ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        final int stackSize = resources.getResourceStack(COMMITTED_LANG_LOCATION).size();

        final String text = readClasspathText(COMMITTED_LANG_ON_CLASSPATH);
        helper.assertTrue(text != null, "the committed lang is shipped: " + COMMITTED_LANG_ON_CLASSPATH);
        final Map<String, String> committed = ResearchOriginalLang.parseFlatJson(text);
        final Set<String> runtimeKeys = graphKeys(ResearchManager.graph());

        final Set<String> overlap = new TreeSet<>(committed.keySet());
        overlap.retainAll(runtimeKeys);

        LOGGER.info("TC-RESEARCH-LANG probe: committed lang reaches the runtime through the "
                        + "classloader with {} keys (ResourceManager stack size {}), {} of them graph keys "
                        + "over the {} the runtime graph references",
                committed.size(), stackSize, overlap.size(), runtimeKeys.size());
        helper.assertTrue(overlap.isEmpty(),
                "D-016 option 2: the committed lang must define 0 of the research keys the graph "
                        + "references, found " + overlap);
        helper.succeed();
    }

    // ------------------------------------------------------------------

    /**
     * The merge's own read order: the {@code ResourceManager} stack first (the
     * client route, where pack overlays apply), then the classloader route (the
     * route {@code I18nManager} uses, and the only one a dedicated server has
     * for lang files). Which one supplied is logged.
     */
    private static ResearchLexicon readGenerated(final ResourceManager resources) {
        Map<String, String> read = new LinkedHashMap<>();
        for (final Resource resource : resources.getResourceStack(GENERATED)) {
            try (InputStream in = resource.open()) {
                read = ResearchLexicon.merged(read, ResearchLexicon.fromStream(in).values());
            } catch (IOException broken) {
                LOGGER.warn("Skipped {} from pack {} ({})", GENERATED, resource.sourcePackId(),
                        broken.toString());
            }
        }
        if (!read.isEmpty()) {
            LOGGER.info("TC-RESEARCH-LANG: {} keys from the ResourceManager stack", read.size());
            return ResearchLexicon.of(read);
        }
        final ResearchLexicon shipped = ResearchLexicon.fromClasspath(ResearchLexicon.GENERATED_ASSET_PATH);
        LOGGER.info("TC-RESEARCH-LANG: ResourceManager stack empty for {}, classloader route carries "
                + "{} keys", GENERATED, shipped.size());
        return shipped;
    }

    /** Every display key the shipped graph references, as the records expose them. */
    private static Set<String> graphKeys(final ResearchGraph graph) {
        final Set<String> keys = new LinkedHashSet<>();
        for (final ResearchEntryData entry : graph.entries()) {
            if (entry.nameKey() != null) {
                keys.add(entry.nameKey());
            }
            if (entry.textKey() != null) {
                keys.add(entry.textKey());
            }
            for (final ResearchPageData page : entry.pages()) {
                if (page.textKey() != null) {
                    keys.add(page.textKey());
                }
            }
        }
        return keys;
    }

    /**
     * The keys the generated file is allowed not to carry, derived from the
     * shipped data: every key of a graph row that names no original string
     * ({@code nameEnUs}/{@code textEnUs} absent — BOOK-3b authors those), plus
     * every key the generator's {@code GRAPH_KEY_PREFIX} filter cannot see.
     */
    private static Set<String> explicableGaps(final ResearchGraph graph, final Set<String> runtimeKeys) {
        final Set<String> explicable = new TreeSet<>();
        for (final ResearchEntryData entry : graph.entries()) {
            if (entry.nameEnUs() == null) {
                explicable.add(entry.nameKey());
            }
            if (entry.textEnUs() == null) {
                explicable.add(entry.textKey());
            }
        }
        for (final String key : runtimeKeys) {
            if (!key.startsWith(ResearchOriginalLang.GRAPH_KEY_PREFIX)) {
                explicable.add(key);
            }
        }
        return explicable;
    }

    private static Set<String> applicable(final Set<String> missing, final Set<String> explicable) {
        final Set<String> both = new TreeSet<>(missing);
        both.retainAll(explicable);
        return both;
    }

    private static Set<String> diff(final Set<String> from, final Set<String> other) {
        final Set<String> out = new TreeSet<>(from);
        out.removeAll(other);
        return out;
    }

    /** The classpath text at {@code path}, or {@code null} when it is not shipped. */
    private static String readClasspathText(final String path) {
        try (InputStream in = ResearchOriginalLanguageGameTest.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException broken) {
            throw new UncheckedIOException(broken);
        }
    }
}
