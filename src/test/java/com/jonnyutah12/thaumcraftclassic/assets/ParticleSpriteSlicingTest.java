package com.jonnyutah12.thaumcraftclassic.assets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the TC-M7-02-s / TC-M7-02-c-s particle sprite contract
 * (build.gradle sliceOriginalParticleSprites,
 * docs/original-asset-inventory.md, docs/asset-mapping.csv).
 *
 * <p>The original ships its FX art in three shapes, and 1.21.1 needs a real
 * sprite per particle frame in each case:</p>
 * <ul>
 *   <li>cells of the 16-column {@code textures/misc/particles.png} grid,
 *       indexed by a flat cell number: sparkle 112..120, bubble 16..17,
 *       the vis-drain mote 128..143 ({@code FXVisSparkle.java:49-53}), the
 *       infusion draw mote 64..67 ({@code FXBoreSparkle.java:54-59}), the
 *       essentia trail mote 25 ({@code FXEssentiaTrail.java:63-66}) and the
 *       arcing wisp 224 ({@code FXWispArcing.java:74-77});</li>
 *   <li>rectangles inside a standalone texture: the wisp's 32x32 cell of
 *       {@code misc/wisp.png} at (0, 224) ({@code FXWisp.java:152-154}) and
 *       the 13 32x8 strips of {@code misc/wispy.png} at y = 24
 *       ({@code FXWispEG.java:60-62});</li>
 *   <li>whole textures bound per frame: the shield runes'
 *       {@code models/hemis1..15} and the ripple set
 *       {@code models/ripple1..15} that both the mob-rune branch and the
 *       sonic pulse bind ({@code FXShieldRunes.java:59-64},
 *       {@code FXSonic.java:57-59}) — one sliced set, two definitions.</li>
 * </ul>
 *
 * <p>This test pins all three links: the committed particle JSONs name the
 * sliced ids in the original's frame order, the sliced files exist, and every
 * sliced file is pixel-identical to the source region it claims — an
 * off-by-one in the cell maths is a red test, not a wrong-looking game.
 * Nothing is committed as art (D-004): the original sheet/texture stays the
 * source of truth and the build slices from it.</p>
 *
 * <p>The pixel comparison needs the reference tree, so it assumption-skips on
 * CI (which has no {@code reference/} checkout), matching the overlay test
 * design in build.gradle. The JSON half runs everywhere.</p>
 */
class ParticleSpriteSlicingTest {

    /** A 1.21.1 sprite id in the particle atlas, as the particle JSON must name it. */
    private static final Pattern SPRITE_ID = Pattern.compile("thaumcraft:particle/[a-z0-9_]+");

    /**
     * Sheet-cell families: particle name -> the cell indices it uses, in the
     * order the FX advances frames. Kept in step with the
     * {@code originalParticleCells} map in build.gradle.
     */
    private static final Map<String, int[]> ORIGINAL_CELLS = Map.of(
            "sparkle", cells(112, 120),
            "bubble", new int[] {16, 17},
            "vis_sparkle", cells(128, 143),
            "bore_sparkle", cells(64, 67),
            "essentia_trail", new int[] {25},
            "wisp_arcing", new int[] {224});

    /**
     * Rectangle families: particle name -> {@code {count, xStep, y, width,
     * height}} with {@code x = frame * xStep}, plus the source texture under
     * the reference {@code textures/} dir. Kept in step with
     * {@code originalParticlePatches} in build.gradle.
     */
    private static final Map<String, int[]> ORIGINAL_RECTS = Map.of(
            "wisp", new int[] {1, 0, 224, 32, 32},
            "wisp_eg", new int[] {13, 32, 24, 32, 8});
    private static final Map<String, String> RECT_SOURCES = Map.of(
            "wisp", "misc/wisp.png",
            "wisp_eg", "misc/wispy.png");

    /**
     * Whole-texture families: sprite-set name -> the source path template
     * (1-based frame index) under the reference {@code textures/} dir. Kept in
     * step with {@code originalParticleFiles} in build.gradle.
     */
    private static final Map<String, String> ORIGINAL_FILES = Map.of(
            "shield_runes", "models/hemis",
            "ripple", "models/ripple");
    private static final Map<String, Integer> ORIGINAL_FILE_COUNTS = Map.of(
            "shield_runes", 15,
            "ripple", 15);

    /**
     * The sprite-id prefix each definition names its frames with; the prefix
     * is the family that owns the art, so the mob runes and the sonic pulse
     * both name {@code ripple_N} (they bind the original's ripple set).
     */
    private static final Map<String, String> SPRITE_PREFIX = Map.of(
            "shield_runes_mob", "ripple",
            "sonic", "ripple");

    /** Every committed particle definition, in registration order. */
    private static final List<String> PARTICLE_JSONS = List.of(
            "sparkle", "bubble",
            "wisp", "wisp_eg", "wisp_arcing",
            "vis_sparkle", "bore_sparkle", "essentia_trail",
            "shield_runes", "shield_runes_mob", "sonic");

    private static int[] cells(final int from, final int to) {
        int[] out = new int[to - from + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = from + i;
        }
        return out;
    }

    private static String prefixOf(final String name) {
        return SPRITE_PREFIX.getOrDefault(name, name);
    }

    /** The frames of one definition: the sprite-id prefix, and the frame count. */
    private static int frameCountOf(final String name) {
        String prefix = prefixOf(name);
        if (ORIGINAL_CELLS.containsKey(name)) {
            return ORIGINAL_CELLS.get(name).length;
        }
        if (ORIGINAL_RECTS.containsKey(name)) {
            return ORIGINAL_RECTS.get(name)[0];
        }
        return ORIGINAL_FILE_COUNTS.get(prefix);
    }

    /**
     * The committed particle definitions. The test JVM does not run in the
     * project directory (NeoForge wires a run directory for tests), so the
     * path arrives as a build-wired system property — the same rule the other
     * asset tests follow.
     */
    private static Path definitionDir() {
        String wired = System.getProperty("tc.particleDefinitionDir", "");
        assertTrue(!wired.isEmpty(),
                "tc.particleDefinitionDir is not wired — run tests through ./gradlew test");
        return Path.of(wired);
    }

    private static Path definition(final String name) {
        return definitionDir().resolve(name + ".json");
    }

    @Test
    void everyPortedParticleDefinitionNamesItsSlicedFramesInOrder() throws IOException {
        for (String name : PARTICLE_JSONS) {
            Path json = definition(name);
            assertTrue(Files.isRegularFile(json),
                    name + ": the particle definition must be committed in src/main/resources "
                            + "(the frames are the particle's id list, so the file is not build output): "
                            + json);

            String text = Files.readString(json);
            List<String> textures = texturesOf(text);
            int expectedFrames = frameCountOf(name);
            assertEquals(expectedFrames, textures.size(),
                    name + ": the frame list must cover every frame the original indexes ("
                            + expectedFrames + ")");

            String prefix = prefixOf(name);
            for (int frame = 0; frame < expectedFrames; frame++) {
                String id = textures.get(frame);
                assertTrue(SPRITE_ID.matcher(id).matches(),
                        name + " frame " + frame + ": not a particle-atlas sprite id: " + id);
                assertEquals("thaumcraft:particle/" + prefix + "_" + frame, id,
                        name + ": frame order must follow the original frame order");
            }
        }
    }

    private static List<String> texturesOf(final String json) {
        com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
        List<String> out = new ArrayList<>();
        parsed.getAsJsonObject().getAsJsonArray("textures").forEach(entry -> out.add(entry.getAsString()));
        return out;
    }

    @Test
    void slicedSpritesAreTheOriginalSheetCells() throws IOException {
        Path sheet = Path.of(System.getProperty("tc.originalParticleSheet", ""));
        Path spritesDir = spritesDir();
        Assumptions.assumeTrue(Files.isRegularFile(sheet),
                "the original FX sheet is only present with the reference tree (CI has none)");
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");

        BufferedImage source = ImageIO.read(sheet.toFile());
        assertEquals(16, source.getWidth() / 16, "the original FX sheet is a 16-column grid");

        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");
        for (Map.Entry<String, int[]> entry : ORIGINAL_CELLS.entrySet()) {
            String name = entry.getKey();
            int[] cellList = entry.getValue();
            for (int frame = 0; frame < cellList.length; frame++) {
                int cell = cellList[frame];
                Path sprite = particleDir.resolve(name + "_" + frame + ".png");
                assertTrue(Files.isRegularFile(sprite),
                        name + " frame " + frame + " was not sliced: " + sprite);

                BufferedImage region = source.getSubimage((cell % 16) * 16, (cell / 16) * 16, 16, 16);
                assertPixels(region, ImageIO.read(sprite.toFile()),
                        name + " frame " + frame + " must be cell " + cell
                                + " of textures/misc/particles.png (col " + (cell % 16)
                                + ", row " + (cell / 16) + ")");
            }
        }
    }

    @Test
    void slicedSpritesAreTheOriginalTextureRects() throws IOException {
        Path sourceRoot = Path.of(System.getProperty("tc.originalParticleSourceDir", ""));
        Path spritesDir = spritesDir();
        Assumptions.assumeTrue(Files.isDirectory(sourceRoot),
                "the reference textures/ dir is only present with the reference tree (CI has none)");
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");

        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");
        for (Map.Entry<String, int[]> entry : ORIGINAL_RECTS.entrySet()) {
            String name = entry.getKey();
            int[] spec = entry.getValue();
            int count = spec[0];
            int xStep = spec[1];
            int y = spec[2];
            int width = spec[3];
            int height = spec[4];
            Path sourcePath = sourceRoot.resolve(RECT_SOURCES.get(name));
            Assumptions.assumeTrue(Files.isRegularFile(sourcePath),
                    sourcePath + " is missing from the reference tree");
            BufferedImage source = ImageIO.read(sourcePath.toFile());

            for (int frame = 0; frame < count; frame++) {
                Path sprite = particleDir.resolve(name + "_" + frame + ".png");
                assertTrue(Files.isRegularFile(sprite),
                        name + " frame " + frame + " was not sliced: " + sprite);
                int x = frame * xStep;
                BufferedImage region = source.getSubimage(x, y, width, height);
                assertPixels(region, ImageIO.read(sprite.toFile()),
                        name + " frame " + frame + " must be the " + width + "x" + height
                                + " rect at (" + x + ", " + y + ") of " + RECT_SOURCES.get(name));
            }
        }
    }

    @Test
    void slicedSpritesAreTheOriginalWholeTextures() throws IOException {
        Path sourceRoot = Path.of(System.getProperty("tc.originalParticleSourceDir", ""));
        Path spritesDir = spritesDir();
        Assumptions.assumeTrue(Files.isDirectory(sourceRoot),
                "the reference textures/ dir is only present with the reference tree (CI has none)");
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");

        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");
        for (Map.Entry<String, String> entry : ORIGINAL_FILES.entrySet()) {
            String family = entry.getKey();
            String template = entry.getValue();
            int count = ORIGINAL_FILE_COUNTS.get(family);
            for (int frame = 0; frame < count; frame++) {
                Path sourcePath = sourceRoot.resolve(template + (frame + 1) + ".png");
                Assumptions.assumeTrue(Files.isRegularFile(sourcePath),
                        sourcePath + " is missing from the reference tree");
                Path sprite = particleDir.resolve(family + "_" + frame + ".png");
                assertTrue(Files.isRegularFile(sprite),
                        family + " frame " + frame + " was not copied: " + sprite);
                assertPixels(ImageIO.read(sourcePath.toFile()), ImageIO.read(sprite.toFile()),
                        family + " frame " + frame + " must be the original "
                                + template + (frame + 1) + ".png");
            }
        }
    }

    private static void assertPixels(final BufferedImage expected, final BufferedImage actual,
            final String message) {
        assertEquals(expected.getWidth(), actual.getWidth(), message + " (width)");
        assertEquals(expected.getHeight(), actual.getHeight(), message + " (height)");
        int[] a = expected.getRGB(0, 0, expected.getWidth(), expected.getHeight(), null, 0,
                expected.getWidth());
        int[] b = actual.getRGB(0, 0, actual.getWidth(), actual.getHeight(), null, 0,
                actual.getWidth());
        assertArrayEquals(a, b, message);
    }

    private static Path spritesDir() {
        return Path.of(System.getProperty("tc.originalParticleSpritesDir", ""));
    }

    @Test
    void particleDefinitionsOnlyReferenceSpritesTheBuildSlices() throws IOException {
        Path spritesDir = spritesDir();
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");
        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");

        Map<String, List<String>> framesByName = new LinkedHashMap<>();
        for (String name : PARTICLE_JSONS) {
            framesByName.put(name, texturesOf(Files.readString(definition(name))));
        }

        for (Map.Entry<String, List<String>> entry : framesByName.entrySet()) {
            for (String id : entry.getValue()) {
                // The sprite id is <ns>:<atlas-relative path>; the atlas-relative
                // path starts with "particle/", so it resolves under textures/.
                String path = id.substring("thaumcraft:".length());
                assertTrue(Files.isRegularFile(particleDir.getParent().resolve(path + ".png")),
                        entry.getKey() + ": the particle JSON names a sprite the build does not "
                                + "slice — the particle atlas will log a missing texture: " + id);
            }
        }

        // Nothing may be sliced that no definition asks for: an orphan in the
        // particle atlas is dead weight in every jar.
        Set<String> named = new LinkedHashSet<>();
        framesByName.values().forEach(named::addAll);
        List<String> orphan = new ArrayList<>();
        try (var files = Files.list(particleDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".png")).forEach(p -> {
                String id = "thaumcraft:particle/"
                        + p.getFileName().toString().replaceAll("\\.png$", "");
                if (!named.contains(id)) {
                    orphan.add(p.getFileName().toString());
                }
            });
        }
        assertTrue(orphan.isEmpty(), "sliced sprites no particle definition names: " + orphan);
    }
}
