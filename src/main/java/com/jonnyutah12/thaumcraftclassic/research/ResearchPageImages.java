package com.jonnyutah12.thaumcraftclassic.research;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The draw-time seam for the {@code <IMG>} texture markers embedded in
 * research page text (kanban t_7463bc4c, follow-up to TC-M7-01-s-a).
 *
 * <p>The original Thaumonomicon drew inline images straight from the page
 * text: {@code ResearchPage.renderPage} splits the text on
 * {@code <IMG>...</IMG>} and parses the payload
 * {@code namespace:path:u1:v1:u2:v2:scale} — texture corners in pixels and a
 * UV scale (1/16 = 0.0625 for a 16px icon, 1 for a full-size blit). The 29
 * shipped markers live in research page <em>text</em>, which is byte-verbatim
 * to the original {@code en_US.lang}
 * (ResearchGraphDataTest.langStringsAreVerbatimFromTheOriginalFile), so the
 * 11 of them naming the 1.7.10 plural directories ({@code textures/items/})
 * stay as authored — the data is never rewritten.</p>
 *
 * <p>The 1.21.1 jar publishes those texture bytes at the singular layout
 * (1.14 dropped the plural names; docs/original-asset-inventory.md,
 * "Published layout (1.21.1)"; {@code overlayOriginalAssets} renames at copy
 * time). This class applies the same rename at resolution time, so the page
 * renderer binds the sprite the jar ships and every other texture directory —
 * {@code gui}, {@code misc}, {@code models}, {@code aspects}, {@code foci} —
 * resolves unchanged, exactly as the {@code misc} atlases
 * ({@code textures/misc/research1..5.png}) are bound by literal path in the
 * original.</p>
 *
 * <p>The TC-M3-03 book screen draws a page by iterating
 * {@link ResearchPageData#images()} and blitting {@link PageImage#location()}
 * at the parsed rectangle; it must never bind the raw string's location,
 * which the jar does not publish.</p>
 */
public final class ResearchPageImages {

    /** The original marker: {@code <IMG>namespace:path:u1:v1:u2:v2:scale</IMG>}. */
    private static final Pattern MARKER = Pattern.compile("<IMG>([^<]+)</IMG>");

    /**
     * 1.7.10 texture directory -> 1.21.1 texture directory. The exact table
     * {@code overlayOriginalAssets} uses at copy time (build.gradle
     * {@code textureDirectoryRenames}); every other directory keeps its name.
     */
    private static final Map<String, String> LEGACY_DIR_RENAMES = Map.of(
            "blocks", "block",
            "items", "item");

    /**
     * One inline page image: the texture location resolved to the published
     * layout, plus the original blit rectangle (pixel corners of the region
     * inside the texture) and UV scale.
     */
    public record PageImage(ResourceLocation location, int x1, int y1, int x2, int y2, float scale) {
    }

    /**
     * Every inline image in a research page text, in document order. The text
     * is the verbatim {@code en_US.lang} string; parsing it is side-effect
     * free, which is what lets the data stay byte-identical while the draw
     * resolves against the published layout.
     *
     * @throws IllegalArgumentException on a malformed marker (the shipped
     *                                  graph parses clean; a hand-edit that
     *                                  breaks the six-field payload must fail
     *                                  loudly, not render garbage)
     */
    public static List<PageImage> parse(String pageText) {
        if (pageText == null) {
            return List.of();
        }
        List<PageImage> images = new ArrayList<>();
        Matcher matcher = MARKER.matcher(pageText);
        while (matcher.find()) {
            images.add(parsePayload(matcher.group(1)));
        }
        return List.copyOf(images);
    }

    /**
     * The published-layout location of a texture location as a 1.7.10 marker
     * writes it: under {@code textures/}, the plural directories 1.14 dropped
     * rename to the singular directories 1.21.1 ships; every other path
     * resolves unchanged.
     */
    public static ResourceLocation publishedLocation(ResourceLocation location) {
        String[] parts = location.getPath().split("/");
        if (parts.length >= 3 && parts[0].equals("textures")) {
            String renamed = LEGACY_DIR_RENAMES.get(parts[1]);
            if (renamed != null) {
                parts[1] = renamed;
                return ResourceLocation.fromNamespaceAndPath(
                        location.getNamespace(), String.join("/", parts));
            }
        }
        return location;
    }

    private static PageImage parsePayload(String payload) {
        String[] parts = payload.split(":");
        if (parts.length != 7) {
            throw new IllegalArgumentException("malformed <IMG> payload (expected "
                    + "namespace:path:u1:v1:u2:v2:scale): " + payload);
        }
        ResourceLocation raw = ResourceLocation.parse(parts[0] + ":" + parts[1]);
        try {
            return new PageImage(
                    publishedLocation(raw),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Float.parseFloat(parts[6]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("malformed <IMG> rectangle/scale: " + payload, e);
        }
    }

    private ResearchPageImages() {
    }
}
