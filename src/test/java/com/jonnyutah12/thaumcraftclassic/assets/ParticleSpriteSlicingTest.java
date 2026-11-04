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
 * Guards the TC-M7-02-s / TC-M7-02-b-s particle sprite contract
 * (build.gradle sliceOriginalParticleSprites,
 * docs/original-asset-inventory.md, docs/asset-mapping.csv).
 *
 * <p>The original ships its FX art in two shapes. The particle-cloud FX
 * (TC-M7-02-s) use a 16-column cell grid inside {@code textures/misc/particles.png},
 * indexed by a flat cell number: the sparkle frames are cells 112..120 (the
 * {@code startParticle = 112} / {@code numParticles = 9} pair every
 * {@code ClientProxy.blockSparkle} caller passes to {@code drawGenericParticles},
 * ClientProxy.java:592-606), the crucible bubble is cells 16 (body) and 17 (pop),
 * the golem-boss impact sprite cycles cells 80..95 (FXBeamGolemBoss.java:253-255)
 * and the arc's type-6 glow sparkle takes cells 112..115 (FXSparkle type 6,
 * 4 frames). The beam/bolt family (TC-M7-02-b-s) draws whole sheet tiles
 * instead: {@code misc/beam.png}/{@code beam1-3.png} (FXBeam.java:77-81) and
 * {@code misc/p_large.png}/{@code p_small.png} (FXLightningBolt.java:178-182).
 * 1.21.1 indexes a list of real sprites per particle, so the build slices the
 * cells and copies the tiles. This test pins all three links: the committed
 * particle JSONs name the sliced ids, the sliced files exist, and every sliced
 * file is pixel-identical to the sheet cell / original tile it came from — an
 * off-by-one in the cell maths is a red test, not a wrong-looking game.</p>
 *
 * <p>The pixel comparison needs the reference tree, so it assumption-skips on
 * CI (which has no {@code reference/} checkout), matching the overlay test
 * design in build.gradle. The JSON half runs everywhere.</p>
 */
class ParticleSpriteSlicingTest {

    /** A 1.21.1 sprite id in the particle atlas, as the particle JSON must name it. */
    private static final Pattern SPRITE_ID = Pattern.compile("thaumcraft:particle/[a-z0-9_]+");

    /**
     * A cell-grid particle: the committed JSON name, the sliced sprite base
     * name, and the sheet cells in draw order. Kept in step with the
     * {@code originalParticleCells} map in build.gradle.
     */
    private record CellParticle(String json, String spriteBase, int[] cells) {
    }

    private static final List<CellParticle> CELL_PARTICLES = List.of(
            new CellParticle("sparkle", "sparkle", cells(112, 120)),
            new CellParticle("bubble", "bubble", new int[] {16, 17}),
            new CellParticle("beam_impact", "impact", cells(80, 95)),
            new CellParticle("sparkle_glow", "sparkle_glow", cells(112, 115)),
            // TC-M7-02-c-s-f1b: the EG wisp steps u = age%13/16 at v = 0.1875
            // (FXWispEG.java:59-62) — row 3, columns 0..12 of the sheet grid.
            new CellParticle("wisp_eg", "wisp_eg", cells(48, 60)));

    /**
     * TC-M7-02-c-s-f1b: the wisp and the arcing wisp draw regions of the
     * VANILLA /misc/fx.png sheet (FXWisp.java:148-151, FXWispArcing.java:
     * 74-77), which the reference tree does not ship. They stand in on the
     * sliced Thaumcraft glow frame (BD-18), so their definitions must name
     * exactly that one sprite.
     */
    private static final Map<String, String> SUBSTITUTE_PARTICLES = Map.of(
            "wisp", "sparkle_0",
            "wisp_arcing", "sparkle_0");

    /**
     * A whole-tile particle: the committed JSON name and its sprite base names
     * in draw order. Kept in step with the {@code originalBeamTiles} map in
     * build.gradle.
     */
    private static final Map<String, List<String>> TILE_PARTICLES = Map.of(
            "beam", List.of("beam0", "beam1", "beam2", "beam3"),
            "beam_golem_boss", List.of("beam0", "beam1", "beam2", "beam3"),
            "arc", List.of("beam0"),
            "bolt", List.of("bolt_glow", "bolt_core"));

    /** Sliced tile base name -> original file under textures/misc. */
    private static final Map<String, String> ORIGINAL_TILES = Map.of(
            "beam0", "beam.png",
            "beam1", "beam1.png",
            "beam2", "beam2.png",
            "beam3", "beam3.png",
            "bolt_glow", "p_large.png",
            "bolt_core", "p_small.png");

    private static int[] cells(final int from, final int to) {
        int[] out = new int[to - from + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = from + i;
        }
        return out;
    }

    private static List<String> particleJsonNames() {
        List<String> names = new ArrayList<>();
        CELL_PARTICLES.forEach(p -> names.add(p.json()));
        names.addAll(SUBSTITUTE_PARTICLES.keySet().stream().sorted().toList());
        names.addAll(TILE_PARTICLES.keySet().stream().sorted().toList());
        return names;
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
        for (CellParticle particle : CELL_PARTICLES) {
            Path json = definition(particle.json());
            assertTrue(Files.isRegularFile(json),
                    particle.json() + ": the particle definition must be committed in "
                            + "src/main/resources (the frames are the particle's id list, so the "
                            + "file is not build output): " + json);

            List<String> textures = texturesOf(Files.readString(json));
            assertEquals(particle.cells().length, textures.size(),
                    particle.json() + ": the frame list must cover every cell the original "
                            + "indexes (" + particle.cells().length + ")");

            for (int frame = 0; frame < particle.cells().length; frame++) {
                String id = textures.get(frame);
                assertTrue(SPRITE_ID.matcher(id).matches(),
                        particle.json() + " frame " + frame + ": not a particle-atlas sprite id: " + id);
                assertEquals("thaumcraft:particle/" + particle.spriteBase() + "_" + frame, id,
                        particle.json() + ": frame order must follow the original cell order, cell "
                                + particle.cells()[frame]);
            }
        }

        for (Map.Entry<String, String> entry : SUBSTITUTE_PARTICLES.entrySet()) {
            List<String> textures = texturesOf(Files.readString(definition(entry.getKey())));
            assertEquals(1, textures.size(),
                    entry.getKey() + ": the fx.png stand-in is one frame (BD-18)");
            assertEquals("thaumcraft:particle/" + entry.getValue(), textures.get(0),
                    entry.getKey() + ": the definition must name the glow frame it stands in on");
        }

        for (Map.Entry<String, List<String>> entry : TILE_PARTICLES.entrySet()) {
            List<String> textures = texturesOf(Files.readString(definition(entry.getKey())));
            assertEquals(entry.getValue().size(), textures.size(),
                    entry.getKey() + ": the frame list must cover every sheet tile the original "
                            + "binds (" + entry.getValue().size() + ")");
            for (int frame = 0; frame < entry.getValue().size(); frame++) {
                String id = textures.get(frame);
                assertTrue(SPRITE_ID.matcher(id).matches(),
                        entry.getKey() + " frame " + frame + ": not a particle-atlas sprite id: " + id);
                assertEquals("thaumcraft:particle/" + entry.getValue().get(frame), id,
                        entry.getKey() + ": the tile draw order must follow the original "
                                + "(FXBeam.java:77-81, FXLightningBolt.java:178-182)");
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
        Path miscDir = Path.of(System.getProperty("tc.originalMiscTexturesDir", ""));
        Assumptions.assumeTrue(Files.isRegularFile(sheet),
                "the original FX sheet is only present with the reference tree (CI has none)");
        Assumptions.assumeTrue(Files.isDirectory(spritesDir),
                "sliceOriginalParticleSprites has not run — run ./gradlew processResources first");

        BufferedImage source = ImageIO.read(sheet.toFile());
        assertEquals(16, source.getWidth() / 16, "the original FX sheet is a 16-column grid");

        Path particleDir = spritesDir.resolve("assets/thaumcraft/textures/particle");
        for (CellParticle particle : CELL_PARTICLES) {
            int[] sheetCells = particle.cells();
            for (int frame = 0; frame < sheetCells.length; frame++) {
                Path sprite = particleDir.resolve(particle.spriteBase() + "_" + frame + ".png");
                assertTrue(Files.isRegularFile(sprite),
                        particle.spriteBase() + " frame " + frame + " was not sliced: " + sprite);

                BufferedImage cell = source.getSubimage((sheetCells[frame] % 16) * 16,
                        (sheetCells[frame] / 16) * 16, 16, 16);
                BufferedImage sliced = ImageIO.read(sprite.toFile());
                assertEquals(16, sliced.getWidth(), particle.spriteBase() + " frame " + frame + " must be a 16px cell");
                assertEquals(16, sliced.getHeight(), particle.spriteBase() + " frame " + frame + " must be a 16px cell");
                assertArrayEquals(cell.getRGB(0, 0, 16, 16, null, 0, 16),
                        sliced.getRGB(0, 0, 16, 16, null, 0, 16),
                        particle.spriteBase() + " frame " + frame + " must be cell " + sheetCells[frame]
                                + " of textures/misc/particles.png (col " + (sheetCells[frame] % 16)
                                + ", row " + (sheetCells[frame] / 16) + ")");
            }
        }

        Assumptions.assumeTrue(Files.isDirectory(miscDir),
                "the original misc textures are only present with the reference tree (CI has none)");
        for (Map.Entry<String, String> entry : ORIGINAL_TILES.entrySet()) {
            Path original = miscDir.resolve(entry.getValue());
            assertTrue(Files.isRegularFile(original), "the original tile is missing: " + original);
            Path sprite = particleDir.resolve(entry.getKey() + ".png");
            assertTrue(Files.isRegularFile(sprite),
                    entry.getKey() + ": the beam/bolt tile was not copied: " + sprite);
            BufferedImage sourceTile = ImageIO.read(original.toFile());
            BufferedImage sliced = ImageIO.read(sprite.toFile());
            assertEquals(sourceTile.getWidth(), sliced.getWidth(), entry.getKey() + " keeps the tile width");
            assertEquals(sourceTile.getHeight(), sliced.getHeight(), entry.getKey() + " keeps the tile height");
            int[] src = sourceTile.getRGB(0, 0, sourceTile.getWidth(), sourceTile.getHeight(), null, 0,
                    sourceTile.getWidth());
            int[] dst = sliced.getRGB(0, 0, sourceTile.getWidth(), sourceTile.getHeight(), null, 0,
                    sourceTile.getWidth());
            assertArrayEquals(src, dst,
                    entry.getKey() + " must be a pixel-identical copy of textures/misc/" + entry.getValue()
                            + " (the original binds it whole, no crop)");
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
