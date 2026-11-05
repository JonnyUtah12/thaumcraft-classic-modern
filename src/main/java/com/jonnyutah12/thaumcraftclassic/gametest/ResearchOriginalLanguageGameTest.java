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
 * string. No display string and no key list is hardcoded here.</p>
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

        // The build-time derivation must measure what the runtime graph
        // references, key for key: 712 display keys (201 name + 201 text + 310
        // page), the page count including the one key outside the tc. namespace.
        helper.assertTrue(buildKeys.size() == 712,
                "the build-time derivation over the shipped graph JSON must measure 712 keys, measured "
                        + buildKeys.size());
        final Set<String> notAtRuntime = new TreeSet<>(buildKeys);
        notAtRuntime.removeAll(runtimeKeys);
        helper.assertTrue(notAtRuntime.isEmpty(),
                "the runtime graph must reference every key the generator built the file for, "
                        + "missing " + notAtRuntime);

        if (lexicon.isEmpty()) {
            // A code-only artifact carries 0 of them by design (D-016 option
            // 2) and the merge injects nothing on such a build. This is the
            // only branch the CI game-test legs reach (CI has no reference
            // tree), so it is the branch that was never exercised locally: the
            // first version of this probe asserted a hardcoded entry id here
            // and CI's -Ptc.noCurios leg of 2026-11-05 (run 42099194219) failed
            // with "the graph itself is shipped" because no row of
            // entries.json is named BASICS — the shipped keys are
            // BASICTHAUMATURGY, FOCUSFIRE, … Nothing about the graph was
            // missing: the same line measured 712 referenced keys. The
            // precondition is therefore read from the shipped JSON below, not
            // remembered.
            final com.google.gson.JsonObject graphDocument =
                    com.google.gson.JsonParser.parseString(graphJson).getAsJsonObject();
            final com.google.gson.JsonArray shippedRowsJson = graphDocument.getAsJsonArray("entries");
            final int shippedRows = shippedRowsJson.size();
            final int declaredRows = graphDocument.get("entryCount").getAsInt();
            helper.assertTrue(shippedRows > 0, GRAPH_JSON_ON_CLASSPATH + " ships research rows");
            helper.assertTrue(shippedRows == declaredRows,
                    GRAPH_JSON_ON_CLASSPATH + " must ship the row count it declares, measured "
                            + shippedRows + " rows against a declared entryCount of " + declaredRows);
            helper.assertTrue(graph.entries().size() == shippedRows,
                    "the runtime graph must carry every shipped row, measured " + graph.entries().size()
                            + " against " + shippedRows + " in " + GRAPH_JSON_ON_CLASSPATH);
            final String firstKey = shippedRowsJson.get(0).getAsJsonObject().get("key").getAsString();
            helper.assertTrue(graph.entry(firstKey) != null,
                    "the shipped graph row " + firstKey + " must resolve in the runtime graph");
            helper.assertTrue(!runtimeKeys.isEmpty(),
                    "the runtime graph must reference display keys even on a code-only build");
            LOGGER.info("TC-RESEARCH-LANG probe: {} reaches the runtime with 0 keys — code-only "
                    + "posture, 0 of {} graph keys resolve; graph shipped with {} rows, first key {}",
                    GENERATED, runtimeKeys.size(), shippedRows, firstKey);
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

        // Which lang did this build generate from? Two overlay shapes reach a
        // running game and they are not interchangeable:
        //   - the player's own extracted reference tree (a developer machine),
        //     whose values are the original 1.7.10 strings;
        //   - scripts/make_asset_overlay_fixture.py's fixture (the CI `build`
        //     job), whose 25 values are placeholders that exist to exercise the
        //     lang parser — an '=' inside a value, a \n escape, leading and
        //     trailing spaces, the <BR>/§ markup — and deliberately carry no
        //     original string (D-004).
        // CI run 42102260672 is where the difference showed: this probe's
        // fidelity leg compared "Fixture name with a trailing space " against
        // the graph row "Basic Wand Craft" and failed — correctly, on a build
        // that contains no original string to be faithful to. The shapes are
        // told apart by measurement, not by a counter: every mapped block
        // texture is 1x1 under the fixture and 16x16 under the reference tree
        // (measured over all 242 of them), and the overlay's textures ship into
        // the mod jar, so a running game can read them.
        final int[] overlayTexture = firstMappedOverlayTextureDims();
        helper.assertTrue(overlayTexture != null,
                "a mapped overlay texture must reach the runtime, so this probe can tell the CI "
                        + "fixture from the player's own reference tree; no candidate resolved");
        final boolean ciFixtureOverlay = overlayTexture[0] == 1 && overlayTexture[1] == 1;

        int checked = 0;
        int byteExact = 0;
        int whitespaceOnly = 0;
        int markup = 0;
        final Set<String> explicable = explicableGaps(graph);
        final Set<String> missing = new TreeSet<>(runtimeKeys);
        missing.removeAll(lexicon.keys());

        if (ciFixtureOverlay) {
            // Placeholder values, and a lang that defines 25 of the 712 keys.
            // So neither "generated value == graph row" nor "the unresolved set
            // is exactly the rows with no original string" is a property of this
            // shape — 687 keys are absent because the fixture lang is small,
            // which no graph row describes. What holds in EVERY shape is
            // asserted here: the file never carries a key the generator cannot
            // produce (the rows with no original string, BOOK-3b's). The
            // fidelity + exact-gap-set pins run below, on the shape that has an
            // original string to be faithful to.
            final Set<String> ungeneratable = new TreeSet<>(lexicon.keys());
            ungeneratable.retainAll(explicable);
            helper.assertTrue(ungeneratable.isEmpty(),
                    "the generated file carries keys the generator cannot produce from the graph "
                            + "plus the overlay lang, so they came from somewhere else: "
                            + ungeneratable);
            LOGGER.info("TC-RESEARCH-LANG probe: overlay source is the CI fixture (mapped texture "
                            + "{}x{}); {} generated keys over {} graph keys, {} unresolved (the "
                            + "fixture lang defines 25 of the graph's keys); fidelity against the "
                            + "graph rows not asserted for placeholder values; ungeneratable-key "
                            + "set empty",
                    overlayTexture[0], overlayTexture[1], lexicon.size(), runtimeKeys.size(),
                    missing.size());
        } else {
            // Fidelity against the graph's own rows — same source, so they agree.
            // Measured rule: the graph row is the same original string with
            // surrounding whitespace trimmed (the original lang carries 14 name
            // values with a trailing space), so byte-equality is reported, not
            // required; a difference beyond whitespace is a real defect.
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
                    "the name-fidelity cross-check must cover the graph's named entries, measured "
                            + checked);

            // The closed gap set, derived. Everything the graph references that
            // the generated file cannot serve must be a graph row with no
            // original string (BOOK-3b authors those) — nothing else. t_28ce4d49
            // measured a fifth key in this set, focus.upgrade.vampirebats.text,
            // and blamed the generator's prefix filter; t_6799fd3f removed that
            // filter, so the equality below is the pin that the gap is closed
            // and that no new gap opened. It runs on the shape where the lang is
            // the player's own file.
            helper.assertTrue(explicable.equals(missing),
                    "the graph keys the generated file cannot serve must be exactly the rows with "
                            + "no original string; unexpected " + diff(missing, applicable(missing, explicable))
                            + ", over-explained " + diff(explicable, missing));

            // The key that gap was, named: every display key the graph rows
            // reference outside the tc. namespace (VAMPBAT page 0 is the one) is
            // served by the generated file, and served with the row's own text.
            // Derived from the graph rows, so the pin cannot drift from the data.
            final Map<String, String> outsideNamespace = outsideNamespaceKeys(graph);
            helper.assertTrue(!outsideNamespace.isEmpty(),
                    "the shipped graph is expected to reference at least one display key outside "
                            + "the tc. namespace (VAMPBAT page 0) for this pin to be non-vacuous");
            for (final Map.Entry<String, String> row : outsideNamespace.entrySet()) {
                final String key = row.getKey();
                helper.assertTrue(!missing.contains(key),
                        key + " is referenced by a graph row and defined by the original lang, so "
                                + "the generated file must serve it (this is the raw-key defect "
                                + "t_6799fd3f exists to close)");
                final String value = lexicon.value(key);
                helper.assertTrue(value != null && !value.isBlank(),
                        key + " resolves to blank text in the generated file");
                helper.assertTrue(!key.equals(value), key + " resolves to its own key");
                final String original = row.getValue();
                if (original != null && value != null) {
                    helper.assertTrue(value.strip().equals(original.strip()),
                            key + " resolved to \"" + value + "\", the graph row says \""
                                    + original + "\" (differs beyond whitespace)");
                }
            }
        }

        // A reference-tree build leaves 4 keys unresolved (the rows with no
        // original string); a fixture build leaves 687. Print the set when it is
        // small enough to read, not when it is not.
        final String missingReport = missing.size() <= 16
                ? missing.toString()
                : missing.size() + " keys, first " + missing.iterator().next();
        LOGGER.info("TC-RESEARCH-LANG probe: {} reaches the runtime with {} keys over a {} overlay; "
                        + "runtime graph references {} keys, the build-time derivation {} (delta {}); "
                        + "{} name values cross-checked against the shipped rows ({} byte-exact, {} "
                        + "differing only in surrounding whitespace, {} carrying original markup); "
                        + "{} unresolved — {}",
                GENERATED, lexicon.size(), ciFixtureOverlay ? "CI-fixture" : "reference-tree",
                runtimeKeys.size(), buildKeys.size(), diff(runtimeKeys, buildKeys),
                checked, byteExact, whitespaceOnly, markup, missingReport,
                ciFixtureOverlay
                        ? "the fixture lang defines 25 of the graph's keys, and no key the generator "
                                + "cannot produce leaked into the file"
                        : "every unresolved key is a graph row with no original string");
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

    /**
     * The namespace every 1.7.10 research display key lives in, as measured over
     * the shipped graph (711 of its 712 keys). The generator does <b>not</b>
     * filter on it — this probe uses it only to name the one key that lives
     * outside it, so the pin below cannot go vacuous.
     */
    private static final String TC_NAMESPACE_PREFIX = "tc.";

    /**
     * Mapped block textures used as the overlay-provenance probe. Measured
     * present in both overlay shapes — written by the fixture and by the
     * reference tree — and both ship to
     * {@code assets/thaumcraft/textures/block/}.
     */
    private static final String[] OVERLAY_PROBE_TEXTURES = {
            "al_furnace_top.png", "al_furnace_front_off.png", "adoorbot.png", "adoortop.png",
    };

    // ------------------------------------------------------------------

    /**
     * The PNG header dimensions of the first mapped overlay texture that
     * reaches the runtime. The fixture writes every mapped texture as a
     * transparent 1x1 placeholder (its own CI output: "908 placeholder texture
     * files"); the player's reference tree ships the original sprites — 16x16
     * for every one of the 242 mapped block textures measured. The overlay's
     * textures enter the build through the generated srcDir and ship in the mod
     * jar, so a running game can read them, which makes this a provenance test
     * for the build: it says which lang {@code generateOriginalResearchLang}
     * read, and therefore whether fidelity to the graph's own rows is
     * measurable at all.
     */
    private static int[] firstMappedOverlayTextureDims() {
        for (final String name : OVERLAY_PROBE_TEXTURES) {
            final String path = "/assets/" + ResearchLexicon.NAMESPACE + "/textures/block/" + name;
            try (InputStream in = ResearchOriginalLanguageGameTest.class.getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                final byte[] header = in.readNBytes(24);
                if (header.length >= 24 && header[0] == (byte) 0x89 && header[1] == 'P'
                        && header[2] == 'N' && header[3] == 'G') {
                    return new int[]{bigEndianInt(header, 16), bigEndianInt(header, 20)};
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException("cannot read " + path, unreadable);
            }
        }
        return null;
    }

    private static int bigEndianInt(final byte[] bytes, final int at) {
        return ((bytes[at] & 0xFF) << 24) | ((bytes[at + 1] & 0xFF) << 16)
                | ((bytes[at + 2] & 0xFF) << 8) | (bytes[at + 3] & 0xFF);
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
     * ({@code nameEnUs}/{@code textEnUs} absent — BOOK-3b authors those).
     *
     * <p>This set used to have a second arm, "keys the generator's
     * {@code tc.} prefix filter cannot see", which measured exactly one key
     * (VAMPBAT page 0). t_6799fd3f made the derivation structural, so that arm
     * is gone and the set above is the whole explanation.</p>
     */
    private static Set<String> explicableGaps(final ResearchGraph graph) {
        final Set<String> explicable = new TreeSet<>();
        for (final ResearchEntryData entry : graph.entries()) {
            if (entry.nameEnUs() == null) {
                explicable.add(entry.nameKey());
            }
            if (entry.textEnUs() == null) {
                explicable.add(entry.textKey());
            }
        }
        return explicable;
    }

    /**
     * The display keys the graph rows reference <b>outside</b> the {@code tc.}
     * namespace, mapped to the original string their own row carries. Measured on
     * the shipped graph: one key, {@code focus.upgrade.vampirebats.text} from
     * {@code VAMPBAT} page 0. A derivation filtered to the {@code tc.} prefix
     * cannot see it, which is what made an overlay build render that page as a
     * raw key (t_6799fd3f) — this probe is the pin that it now resolves.
     */
    private static Map<String, String> outsideNamespaceKeys(final ResearchGraph graph) {
        final Map<String, String> keys = new LinkedHashMap<>();
        for (final ResearchEntryData entry : graph.entries()) {
            for (final ResearchPageData page : entry.pages()) {
                final String key = page.textKey();
                if (key != null && !key.startsWith(TC_NAMESPACE_PREFIX)) {
                    keys.put(key, page.textEnUs());
                }
            }
        }
        return keys;
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
