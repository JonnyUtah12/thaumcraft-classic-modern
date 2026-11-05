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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The runtime half of the D-016 option 2 consumer (BOOK-3a follow-up,
 * t_28ce4d49): the generated research lang must be reachable through the
 * shipped resource pipeline, and it must resolve the graph the server ships.
 *
 * <p>The merge itself is client-only ({@code client/ResearchOriginalLanguage},
 * {@code Dist.CLIENT}) and the game-test harness is a dedicated server that
 * never constructs a {@code Screen}, so these probes deliberately name no
 * client class — the dist-safety scan in
 * {@code ResearchOriginalLanguageTest.noServerSideClassNamesTheClientMerge}
 * enforces that on bytecode. What is decidable headlessly is the input side,
 * through the same {@code ResourceManager} route the client listener uses:
 * the resource is there, it parses, every key it carries is graph-referenced,
 * every value is real text, and each value equals the graph's own
 * {@code nameEnUs} where the graph names the entry. The committed
 * {@code en_us.json} is pinned at 0 overlap with the graph keys
 <i>at runtime</i> (the unit twin pins it at build time).</p>

 * <p>A code-only build ships no generated file (that is the point of D-016),
 * so probe one takes the absence branch there and asserts the D-016 posture
 * instead. Both branches log the measured counts into
 * {@code run/logs/latest.log} so the leg states which one it proved.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchOriginalLanguageGameTest {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation GENERATED =
            ResourceLocation.fromNamespaceAndPath(ResearchLexicon.NAMESPACE,
                    ResearchLexicon.GENERATED_RESOURCE_PATH);
    private static final ResourceLocation COMMITTED =
            ResourceLocation.fromNamespaceAndPath(ResearchLexicon.NAMESPACE, "lang/en_us.json");

    /**
     * The generated resource reaches the game's own {@code ResourceManager}
     * (the pipeline the client merge reads), and every string in it resolves
     * the shipped graph.
     */
    @GameTest(template = "empty")
    public static void generatedResearchLangResolvesTheShippedGraph(final GameTestHelper helper) {
        final ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        final ResearchLexicon lexicon = readLexicon(resources, GENERATED);
        final ResearchGraph graph = ResearchManager.graph();
        final Set<String> graphKeys = graphKeys(graph);

        helper.assertTrue(graphKeys.size() == 711,
                "the shipped graph must reference 711 display keys, measured " + graphKeys.size());

        if (lexicon.isEmpty()) {
            // A code-only artifact carries 0 of the 711 by design (D-016
            // option 2), and the merge injects nothing on such a build.
            LOGGER.info("tc research lang probe: {} absent from the runtime resources — code-only "
                    + "posture, 0 of {} graph keys resolve", GENERATED, graphKeys.size());
            helper.assertTrue(graph.entry("BASICS") != null, "the graph itself is shipped");
            helper.succeed();
            return;
        }

        // Every key the file carries must be graph-referenced: a generated
        // lang that invents keys is a leak, not a consumer.
        final Set<String> invented = new TreeSet<>(lexicon.keys());
        invented.removeAll(graphKeys);
        helper.assertTrue(invented.isEmpty(),
                "the generated research lang carries non-graph keys: " + invented);

        // Every key resolves to real text — this is the raw-key defect the
        // card exists to close, measured per key, over the shipped file.
        for (final String key : lexicon.keys()) {
            final String value = lexicon.value(key);
            helper.assertTrue(value != null && !value.isBlank(), key + " resolves to blank text");
            helper.assertTrue(!key.equals(value), key + " resolves to its own key");
        }

        // Fidelity: the graph row and the generated file come from the same
        // original lang, so they must agree. Derived from the shipped data —
        // no display string is hardcoded in this probe.
        int checked = 0;
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
            helper.assertTrue(resolved.equals(original),
                    entry.nameKey() + " resolved to \"" + resolved + "\", the graph row says \""
                            + original + "\"");
            checked++;
            if (original.indexOf('\u00a7') >= 0 || original.indexOf('<') >= 0) {
                markup++;
            }
        }
        helper.assertTrue(checked >= 150,
                "name-fidelity cross-check must cover the named entries, measured " + checked);

        LOGGER.info("tc research lang probe: {} keys merged from {}, {} name values match the "
                        + "shipped graph rows ({} of them carry markup), {} graph keys in the graph",
                lexicon.size(), GENERATED, checked, markup, graphKeys.size());
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
        final List<Resource> stack = resources.getResourceStack(COMMITTED);
        helper.assertTrue(!stack.isEmpty(), "the committed lang is shipped: " + COMMITTED);

        Map<String, String> committed = Map.of();
        for (final Resource resource : stack) {
            try (InputStream in = resource.open()) {
                committed = ResearchLexicon.merged(committed, ResearchOriginalLang
                        .parseFlatJson(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException broken) {
                throw new UncheckedIOException(broken);
            }
        }
        final Set<String> graphKeys = graphKeys(ResearchManager.graph());
        final Set<String> overlap = new TreeSet<>(committed.keySet());
        overlap.retainAll(graphKeys);

        LOGGER.info("tc research lang probe: committed {} carries {} keys, {} of them graph keys",
                COMMITTED, committed.size(), overlap.size());
        helper.assertTrue(overlap.isEmpty(),
                "D-016 option 2: the committed lang must define 0 of the 711 research keys, found "
                        + overlap);
        helper.succeed();
    }

    // ------------------------------------------------------------------

    /** The generated file merged over every pack that ships it, in pack order. */
    private static ResearchLexicon readLexicon(final ResourceManager resources, final ResourceLocation path) {
        Map<String, String> read = new java.util.LinkedHashMap<>();
        for (final Resource resource : resources.getResourceStack(path)) {
            try (InputStream in = resource.open()) {
                read = ResearchLexicon.merged(read, ResearchLexicon.fromStream(in).values());
            } catch (IOException broken) {
                LOGGER.warn("Skipped {} from pack {} ({})", path, resource.sourcePackId(), broken.toString());
            }
        }
        return ResearchLexicon.of(read);
    }

    /** Every display key the shipped graph references — derived, never listed. */
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
}
