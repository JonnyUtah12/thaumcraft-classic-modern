package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.ResearchPageImages.PageImage;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-01-s-a follow-up (kanban t_7463bc4c): the research page renderer must
 * draw {@code <IMG>} texture locations at the PUBLISHED 1.21.1 layout while the
 * page text stays byte-verbatim to the original {@code en_US.lang}.
 *
 * <p>The original marker format (ResearchPage.java, TC 4.2.3.5) is
 * {@code <IMG>namespace:path:u1:v1:u2:v2:scale</IMG>}; the 29 shipped markers
 * sit in research page text, 11 of them naming the 1.7.10 plural directory
 * {@code textures/items/}. The overlay publishes those bytes at
 * {@code textures/item/} (docs/original-asset-inventory.md, "Published layout
 * (1.21.1)"), and the page text may not be rewritten
 * (ResearchGraphDataTest.langStringsAreVerbatimFromTheOriginalFile), so the
 * rename must happen in the draw-time resolver — {@link ResearchPageImages}.</p>
 */
class ResearchPageImagesTest {

    /** 1.21.1 texture directories the published layout carries (measured, per
     *  docs/original-asset-inventory.md): blocks/items were renamed, all kept. */
    private static final Set<String> PUBLISHED_DIRS =
            Set.of("block", "item", "gui", "misc", "models", "aspects", "foci");

    /** The 1.7.10 directories 1.21.1 dropped (1.14). */
    private static final Set<String> LEGACY_DIRS = Set.of("blocks", "items");

    /** A shipped marker, copied byte-for-byte from entries.json. */
    private static final String AMBER_MARKER =
            "<IMG>thaumcraft:textures/items/amber.png:0:0:255:255:0.0625</IMG>";

    /** A shipped misc marker: a kept directory, unchanged by the rename. */
    private static final String RESEARCH5_MARKER =
            "<IMG>thaumcraft:textures/misc/research5.png:128:136:112:120:.75</IMG>";

    /**
     * The renderer seam renames the plural directory the 1.7.10 marker names to
     * the singular directory the jar publishes, keeps every other directory,
     * and parses the original six-field payload (u1:v1:u2:v2:scale).
     */
    @Test
    void imgMarkersResolveToThePublishedTextureLayout() {
        List<PageImage> images = ResearchPageImages.parse(
                "Things and stuff " + AMBER_MARKER + " " + RESEARCH5_MARKER + " tail");

        assertEquals(2, images.size());

        PageImage amber = images.get(0);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/item/amber.png"),
                amber.location(),
                "a <IMG> marker naming textures/items/ must resolve to the published textures/item/");
        assertEquals(0, amber.x1());
        assertEquals(0, amber.y1());
        assertEquals(255, amber.x2());
        assertEquals(255, amber.y2());
        assertEquals(0.0625f, amber.scale(), 0.0f);

        PageImage research5 = images.get(1);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/research5.png"),
                research5.location(), "textures/misc/ is kept by the published layout");
        assertEquals(128, research5.x1());
        assertEquals(136, research5.y1());
        assertEquals(112, research5.x2());
        assertEquals(120, research5.y2());
        assertEquals(0.75f, research5.scale(), 0.0f);
    }

    /**
     * The rename table is exactly the overlay's: blocks→block, items→item,
     * everything under textures/ else (and every non-texture path) untouched.
     */
    @Test
    void theDirectoryRenameMatchesTheOverlayTable() {
        assertEquals("textures/block/amberore.png",
                ResearchPageImages.publishedLocation(
                        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/amberore.png"))
                        .getPath());
        assertEquals("textures/item/amber.png",
                ResearchPageImages.publishedLocation(
                        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/amber.png"))
                        .getPath());
        for (String kept : List.of("gui", "misc", "models", "aspects", "foci")) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    "thaumcraft", "textures/" + kept + "/sample.png");
            assertEquals(location, ResearchPageImages.publishedLocation(location),
                    "textures/" + kept + "/ keeps its name (docs/original-asset-inventory.md)");
        }
        assertEquals(ResourceLocation.parse("thaumcraft:notextures/items/sample.png"),
                ResearchPageImages.publishedLocation(
                        ResourceLocation.parse("thaumcraft:notextures/items/sample.png")),
                "only paths under textures/ rename");
    }

    /**
     * End to end over the shipped graph: every one of the 29 markers parses,
     * the 11 plural markers all resolve into the published layout, and each
     * resolved location names a texture byte the build publishes (assumption-
     * guarded like OriginalTextureOverlayTest: CI has no reference tree, so CI
     * asserts the resolution and a local build asserts the bytes).
     */
    @Test
    void everyShippedImgLocationResolvesToAPublishedTexture() throws IOException {
        Path research = researchDataDir();
        String raw = Files.readString(research.resolve("entries.json"));

        List<PageImage> images = ResearchPageImages.parse(raw);
        assertEquals(29, count(raw, "<IMG>"), "entries.json carries 29 <IMG> markers");
        assertEquals(29, count(raw, "</IMG>"), "every <IMG> marker is closed");
        assertEquals(29, images.size(), "every shipped <IMG> marker parses");

        Path textures = overlayTexturesDir();
        List<String> unresolved = new ArrayList<>();
        for (PageImage image : images) {
            ResourceLocation resolved = image.location();
            String dir = resolved.getPath().split("/")[0];
            String tail = resolved.getPath().substring(dir.length() + 1);
            if (!resolved.getNamespace().equals("thaumcraft")) {
                unresolved.add(resolved + " — only thaumcraft markers are shipped");
            } else if (!dir.equals("textures")) {
                unresolved.add(resolved + " — a research <IMG> names a texture path");
            } else {
                String textureDir = tail.split("/")[0];
                if (!PUBLISHED_DIRS.contains(textureDir)) {
                    unresolved.add(resolved + " — not a published texture directory");
                } else if (!Files.isRegularFile(textures.resolve(tail))) {
                    unresolved.add(resolved + " — the published layout has no such byte");
                }
            }
        }
        assertTrue(unresolved.isEmpty(),
                images.size() + " <IMG> locations, unresolved: " + unresolved);
    }

    /**
     * The seam resolves; it never rewrites. The committed page text must keep
     * exactly the 11 plural 1.7.10 markers byte-for-byte (the lang-verbatim
     * guard in ResearchGraphDataTest pins the strings against en_US.lang;
     * this pins the marker census the follow-up card was filed around).
     */
    @Test
    void theDataStaysVerbatimWithTheLegacyPaths() throws IOException {
        String raw = Files.readString(researchDataDir().resolve("entries.json"));

        assertEquals(11, count(raw, "<IMG>thaumcraft:textures/items/"),
                "the 11 1.7.10 plural <IMG> markers stay byte-verbatim in the data");
        assertEquals(0, count(raw, "<IMG>thaumcraft:textures/blocks/"),
                "no blocks/ marker is shipped; the 11 plural markers are all items/");
        assertEquals(18, count(raw, "<IMG>thaumcraft:textures/misc/"),
                "the other 18 markers name the kept misc directory");
        assertTrue(raw.contains(AMBER_MARKER),
                "the amber marker must survive in the raw text byte-for-byte: " + AMBER_MARKER);

        // Resolution is a pure function of the string, applied at draw time.
        List<PageImage> images = ResearchPageImages.parse(AMBER_MARKER);
        assertEquals(1, images.size());
        assertTrue(images.get(0).location().getPath().contains("/item/"),
                "resolution names the singular directory; the data keeps the plural one");
    }

    private static Path researchDataDir() {
        String path = System.getProperty("tc.researchDataDir");
        assertNotNull(path, "system property 'tc.researchDataDir' is not set — run tests through ./gradlew test");
        Path dir = Path.of(path);
        assertTrue(Files.isDirectory(dir), "research data dir not found at " + dir);
        return dir;
    }

    private static Path overlayTexturesDir() {
        Path overlayDir = Path.of(System.getProperty("tc.originalAssetsOverlayDir", ""));
        Path textures = overlayDir.resolve("thaumcraft/textures");
        Assumptions.assumeTrue(Files.isDirectory(textures),
                "original-asset overlay not generated (no ../reference tree) — "
                        + "CI asserts the resolution shape only");
        return textures;
    }

    private static int count(String haystack, String needle) {
        int total = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            total++;
        }
        return total;
    }
}
