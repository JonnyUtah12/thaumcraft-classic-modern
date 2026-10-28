package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageImages;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageImages.PageImage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TC-M7-01-s-a follow-up (kanban t_7463bc4c): the Thaumonomicon page render
 * path (TC-M3-03/04 surface) must draw every shipped {@code <IMG>} marker at
 * the texture location the 1.21.1 jar publishes, while the verbatim
 * {@code en_US.lang} page text keeps the 1.7.10 path.
 *
 * <p>The book screen is client-side and the game test server is headless, so
 * the probe drives the renderer's exact inputs: it loads the shipped graph
 * through {@link ResearchGraphLoader#loadDefault()} — the same call the
 * server-side research core makes — and walks every page through
 * {@link ResearchPageData#images()}, the seam the page renderer blits. The
 * missing-texture fallback fires exactly when the location the seam hands the
 * drawer names no published byte, so the probe verifies that for every
 * shipped marker: on a dev game test server (overlay generated) each resolved
 * location resolves to the asset bytes on the mod classpath — the same bytes
 * the client jar ships. CI builds without the reference tree, so there the
 * probe asserts the resolution shape and logs the skipped byte check,
 * matching the assumption pattern of {@code OriginalTextureOverlayTest}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchPageImagesGameTest {

    /** 1.21.1 texture directories the published layout carries. */
    private static final Set<String> PUBLISHED_DIRS =
            Set.of("block", "item", "gui", "misc", "models", "aspects", "foci");

    private ResearchPageImagesGameTest() {
    }

    @GameTest(template = "empty")
    public static void researchimglocationsresolvetopublishedtextures(final GameTestHelper helper) {
        final ResearchGraph graph = ResearchGraphLoader.loadDefault();

        final List<PageImage> images = new ArrayList<>();
        int legacyMarkersInText = 0;
        for (final ResearchEntryData entry : graph.entries()) {
            for (final ResearchPageData page : entry.pages()) {
                final String text = page.textEnUs();
                if (text != null) {
                    // The data keeps the 1.7.10 plural path byte-verbatim —
                    // the seam resolves it, it never rewrites the page.
                    legacyMarkersInText += count(text, "<IMG>thaumcraft:textures/items/")
                            + count(text, "<IMG>thaumcraft:textures/blocks/");
                }
                images.addAll(page.images());
            }
        }

        helper.assertTrue(images.size() == 29,
                "the shipped graph carries 29 <IMG> markers, parsed " + images.size());
        helper.assertTrue(legacyMarkersInText == 11,
                "the 11 plural 1.7.10 markers must stay verbatim in the page text, found "
                        + legacyMarkersInText);

        final List<String> problems = new ArrayList<>();
        final Set<String> pluralDraws = new LinkedHashSet<>();
        for (final PageImage image : images) {
            final String path = image.location().getPath();
            final String[] parts = path.split("/");
            if (!image.location().getNamespace().equals("thaumcraft")
                    || parts.length < 3 || !parts[0].equals("textures")
                    || !PUBLISHED_DIRS.contains(parts[1])) {
                problems.add(image.location() + " — not a published-layout texture location");
            }
            if (parts.length >= 2 && (parts[1].equals("blocks") || parts[1].equals("items"))) {
                pluralDraws.add(image.location().toString());
            }
        }
        helper.assertTrue(problems.isEmpty(),
                images.size() + " resolved <IMG> locations, " + problems.size()
                        + " problems (first 5): " + problems.subList(0, Math.min(5, problems.size())));
        helper.assertTrue(pluralDraws.isEmpty(),
                "the renderer seam handed the drawer 1.7.10 plural paths the jar does not "
                        + "publish (missing-texture draw, first 5 of " + pluralDraws.size() + "): "
                        + pluralDraws.stream().limit(5).toList());

        // The bytes a missing-texture fallback would miss: every resolved
        // location must exist on the mod classpath. The dev game test server
        // carries the overlay output (build/resources/main), i.e. the same
        // assets tree the client resolves; a CI build ships no reference tree,
        // so there the byte check is logged as skipped.
        final ClassLoader loader = ResearchPageImagesGameTest.class.getClassLoader();
        final boolean publishedLayoutShipped =
                loader.getResource("assets/thaumcraft/textures/block/amberore.png") != null;
        if (publishedLayoutShipped) {
            final List<String> missingBytes = new ArrayList<>();
            for (final PageImage image : images) {
                final ResourceLocation location = image.location();
                if (loader.getResource("assets/" + location.getNamespace() + "/"
                        + location.getPath()) == null) {
                    missingBytes.add(location.toString());
                }
            }
            helper.assertTrue(missingBytes.isEmpty(),
                    "resolved <IMG> locations with no published texture byte (the page would "
                            + "render the missing-texture texture, first 5 of " + missingBytes.size() + "): "
                            + missingBytes.subList(0, Math.min(5, missingBytes.size())));
            ThaumcraftClassic.LOGGER.info(
                    "ResearchPageImagesGameTest: {} resolved <IMG> locations, all published "
                            + "on the mod classpath (11 plural markers renamed at resolution)",
                    images.size());
        } else {
            ThaumcraftClassic.LOGGER.info(
                    "ResearchPageImagesGameTest: {} resolved <IMG> locations validated "
                            + "against the published layout shape; asset bytes not shipped "
                            + "in this build (no reference tree) — byte check skipped",
                    images.size());
        }

        helper.succeed();
    }

    private static int count(String haystack, String needle) {
        int total = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            total++;
        }
        return total;
    }
}
