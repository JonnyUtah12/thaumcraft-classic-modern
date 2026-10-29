package com.jonnyutah12.thaumcraftclassic.assets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the TC-M7-02-s particle sprite contract
 * (build.gradle sliceOriginalParticleSprites,
 * docs/original-asset-inventory.md, docs/asset-mapping.csv).
 *
 * <p>The original ships its FX art as a 16-column cell grid inside
 * {@code textures/misc/particles.png}, indexed by a flat cell number: the
 * sparkle frames are cells 112..120 (the {@code startParticle = 112} /
 * {@code numParticles = 9} pair every {@code ClientProxy.blockSparkle} caller
 * passes to {@code drawGenericParticles}, ClientProxy.java:592-606) and the
 * crucible bubble is cells 16 (body) and 17 (pop), which {@code FXBubble}
 * indexes through its own frame counter. 1.21.1 indexes a list of real sprites
 * per particle, so the build slices those cells out of the sheet. This test
 * pins all three links: the committed particle JSONs name the sliced ids, the
 * sliced files exist, and every sliced file is pixel-identical to the sheet
 * cell the original indexes — an off-by-one in the cell maths is a red test,
 * not a wrong-looking game.</p>
 *
 * <p>The pixel comparison needs the reference tree, so it assumption-skips on
 * CI (which has no {@code reference/} checkout), matching the overlay test
 * design in build.gradle. The JSON half runs everywhere.</p>
 */
class ParticleSpriteSlicingTest {

    /** A 1.21.1 sprite id in the particle atlas, as the particle JSON must name it. */
    private static final Pattern SPRITE_ID = Pattern.compile("thaumcraft:particle/[a-z0-9_]+");

    /**
     * The frames each ported particle uses, in draw order: cell index in the
     * original sheet -> sliced sprite index. Kept in step with the
     * {@code originalParticleCells} map in build.gradle.
     */
    private static final Map<String, int[]> ORIGINAL_CELLS = Map.of(
            "sparkle", cells(112, 120),
            "bubble", new int[] {16, 17});

    private static int[] cells(final int from, final int to) {
        int[] out = new int[to - from + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = from + i;
        }
        return out;
    }

    private static List<String> particleJsonNames() {
        return List.of("sparkle", "bubble");
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
        for (String name : particleJsonNames()) {
            Path json = definition(name);
            assertTrue(Files.isRegularFile(json),
                    name + ": the particle definition must be committed in src/main/resources "
                            + "(the frames are the particle's id list, so the file is not build output): "
                            + json);

            String text = Files.readString(json);
            List<String> textures = texturesOf(text);
            int[] expectedCells = ORIGINAL_CELLS.get(name);
            assertEquals(expectedCells.length, textures.size(),
                    name + ": the frame list must cover every cell the original indexes ("
                            + expectedCells.length + ")");

            for (int frame = 0; frame < expectedCells.length; frame++) {
                String id = textures.get(frame);
                assertTrue(SPRITE_ID.matcher(id).matches(),
                        name + " frame " + frame + ": not a particle-atlas sprite id: " + id);
                assertEquals("thaumcraft:particle/" + name + "_" + frame, id,
                        name + ": frame order must follow the original cell order, cell "
                                + expectedCells[frame]);
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
        Path spritesDir = Path.of(System.getProperty("tc.originalParticleSpritesDir", ""));
        Assumptions.assumeTrue(Files.isRegularFile(sheet),
                "the original FX sheet is only present with the reference tree (CI has none)");
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");

        BufferedImage source = ImageIO.read(sheet.toFile());
        assertEquals(16, source.getWidth() / 16, "the original FX sheet is a 16-column grid");

        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");
        for (Map.Entry<String, int[]> entry : ORIGINAL_CELLS.entrySet()) {
            String name = entry.getKey();
            int[] cells = entry.getValue();
            for (int frame = 0; frame < cells.length; frame++) {
                Path sprite = particleDir.resolve(name + "_" + frame + ".png");
                assertTrue(Files.isRegularFile(sprite),
                        name + " frame " + frame + " was not sliced: " + sprite);

                BufferedImage cell = source.getSubimage((cells[frame] % 16) * 16, (cells[frame] / 16) * 16, 16, 16);
                BufferedImage sliced = ImageIO.read(sprite.toFile());
                assertEquals(16, sliced.getWidth(), name + " frame " + frame + " must be a 16px cell");
                assertEquals(16, sliced.getHeight(), name + " frame " + frame + " must be a 16px cell");
                assertArrayEquals(cell.getRGB(0, 0, 16, 16, null, 0, 16),
                        sliced.getRGB(0, 0, 16, 16, null, 0, 16),
                        name + " frame " + frame + " must be cell " + cells[frame]
                                + " of textures/misc/particles.png (col " + (cells[frame] % 16)
                                + ", row " + (cells[frame] / 16) + ")");
            }
        }
    }

    @Test
    void particleDefinitionsOnlyReferenceSpritesTheBuildSlices() throws IOException {
        Path spritesDir = Path.of(System.getProperty("tc.originalParticleSpritesDir", ""));
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");
        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");

        for (String name : particleJsonNames()) {
            List<String> textures = texturesOf(Files.readString(definition(name)));
            for (String id : textures) {
                // The sprite id is <ns>:<atlas-relative path>; the atlas-relative
                // path starts with "particle/", so it resolves under textures/.
                String path = id.substring("thaumcraft:".length());
                assertTrue(Files.isRegularFile(particleDir.getParent().resolve(path + ".png")),
                        id + ": the particle JSON names a sprite the build does not slice — the "
                                + "particle atlas will log a missing texture");
            }
        }

        // Nothing may be sliced that no definition asks for: an orphan in the
        // particle atlas is dead weight in every jar.
        List<String> orphan = new ArrayList<>();
        try (var files = Files.list(particleDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".png")).forEach(p -> {
                String id = "thaumcraft:particle/"
                        + p.getFileName().toString().replaceAll("\\.png$", "");
                boolean named = particleJsonNames().stream()
                        .anyMatch(name -> texturesOfQuietly(name).contains(id));
                if (!named) {
                    orphan.add(p.getFileName().toString());
                }
            });
        }
        assertTrue(orphan.isEmpty(), "sliced sprites no particle definition names: " + orphan);
    }

    private static List<String> texturesOfQuietly(final String name) {
        try {
            return texturesOf(Files.readString(definition(name)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
