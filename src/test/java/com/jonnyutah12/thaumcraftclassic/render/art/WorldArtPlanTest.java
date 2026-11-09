package com.jonnyutah12.thaumcraftclassic.render.art;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BD-18 slice 4 (t_6563ce50): the world art plans are the original's
 * arithmetic, and every {@code thaumcraft:} sprite they name is a published
 * {@code docs/asset-mapping.csv} row (D-005 / the published-sprite contract).
 *
 * <p>These probes pin the formulas the game tests exercise against live
 * tiles, here at exact values: the jar's height ({@code 0.625F},
 * {@code TileJarRenderer.java:110}), label yaw map and crooked tilt
 * ({@code :55-65}); the crystal's tint table
 * ({@code TileCrystalRenderer.java:74-76, 89-92}) and the seeded shard
 * sequence at a fixed cell, with the supporting-face placement table
 * ({@code :20-39}); the mirror's pane swap and standoff
 * ({@code TileMirrorRenderer.java:617-629}); the spa's tank read (the port's
 * {@code tiles/TileSpa.java:39, 65-71}). The game-test class is
 * the mutation-proof surface; this class pins the arithmetic so a formula
 * drift names itself.</p>
 *
 * <p>The sprite scan reads the shipped source (unit tests run from the
 * project dir): every quoted {@code "namespace/path"} constant in
 * {@code render/art/*.java} must be a published row — matched against either
 * CSV column, because the {@code textures/blocks/} rows of the reference
 * publish {@code textures/block/} targets in 1.21.1. {@code minecraft:}
 * sprites are exempt (the CSV governs the reference's assets, and vanilla's
 * water sprite is not one). Model ids are out of scope here: models are
 * authored port files (D-004) and the asset-path contract walks them.</p>
 */
class WorldArtPlanTest {

    // ----------------------------------------------------------------- jar

    @Test
    void jarFluidHeightIsTheOriginalsFormula() {
        // TileJarRenderer.java:110 — (float) amount / maxAmount * 0.625F.
        assertEquals(0.625F, JarWorldArt.fluidHeight(500, 500), 1.0E-5F,
                "a full jar is 0.625 blocks tall (amount == maxAmount, :110)");
        assertEquals(0.1875F, JarWorldArt.fluidHeight(150, 500), 1.0E-5F,
                "150 units is 0.1875 blocks");
        assertEquals(0.0F, JarWorldArt.fluidHeight(0, 500), 1.0E-5F,
                "an empty jar draws no fluid");
        assertEquals(JarWorldArt.FILL_PER_UNIT, 0.625F, "the published formula constant is 0.625F (:110)");
    }

    @Test
    void jarLabelFrameYawIsTheFacingMap() {
        // TileJarRenderer.java:55-61.
        assertEquals(180, JarWorldArt.facingRotation(3), "facing 3 rotates the label frame 180");
        assertEquals(270, JarWorldArt.facingRotation(4), "facing 4 rotates 270");
        assertEquals(90, JarWorldArt.facingRotation(5), "facing 5 rotates 90");
        assertEquals(0, JarWorldArt.facingRotation(2), "facing 2 (the port's default) is the base");
        assertEquals(0, JarWorldArt.facingRotation(0), "down/up facings keep the base");
    }

    @Test
    void jarLabelTiltCarriesTagHashPositionAndFacing() {
        // TileJarRenderer.java:65 — (tag.hashCode() + xCoord + facing) % 4 - 2.
        final int hash = "aqua".hashCode();
        final float expected = (hash + 7 + 4) % 4 - 2;
        assertEquals(expected, JarWorldArt.tiltDeg("aqua", 7, 4), 1.0E-5F,
                "the tilt is (hash + x + facing) % 4 - 2 degrees, always in [-2, 1]");
        for (final int x : new int[]{-13, 0, 4, 91}) {
            for (final int facing : new int[]{2, 3, 4, 5}) {
                final float tilt = JarWorldArt.tiltDeg("ignis", x, facing);
                assertTrue(tilt >= -2.0F && tilt <= 1.0F,
                        "tilt must sit in [-2, 1] for every cell, got " + tilt);
            }
        }
    }

    @Test
    void jarFluidLightIsThe200Floor() {
        // TileJarRenderer.java:118-120: max(200, blockLight(0-15)) is always
        // 200; 200/255 of 1.21.1's 0-15 channel is 11.
        assertEquals(11, JarWorldArt.FLUID_LIGHT);
    }

    // -------------------------------------------------------------- crystal

    @Test
    void crystalShardsAreDeterministicForOneCell() {
        final var a = CrystalWorldArt.shards(2, new net.minecraft.core.BlockPos(10, 64, -3));
        final var b = CrystalWorldArt.shards(2, new net.minecraft.core.BlockPos(10, 64, -3));
        final var c = CrystalWorldArt.shards(2, new net.minecraft.core.BlockPos(11, 64, -3));
        assertEquals(6, a.size(), "main + a = 1..5 (TileCrystalRenderer.java:81-101)");
        assertEquals(a, b, "same seed, same shards — the seed is metadata + x + y * z (:78)");
        assertFalse(a.equals(c), "the seed carries x: the next cell over draws a different cluster");
        final var d = CrystalWorldArt.shards(3, new net.minecraft.core.BlockPos(10, 64, -3));
        assertFalse(a.equals(d), "the seed carries metadata");
        // y * z: cells sharing x but not y*z must differ when y*z differs.
        final var e = CrystalWorldArt.shards(2, new net.minecraft.core.BlockPos(10, 65, -2));
        assertFalse(a.equals(e), "the seed is y * z, not y + z: (64, -3) and (65, -2) differ");
    }

    @Test
    void crystalShardAnglesRespectTheOriginalsRanges() {
        final var shards = CrystalWorldArt.shards(1, new net.minecraft.core.BlockPos(3, 5, 9));
        final var main = shards.get(0);
        assertTrue(main.yawDeg() >= -5.0F && main.yawDeg() <= 5.0F,
                "the main shard yaw is (nextFloat - nextFloat) * 5 (:83), got " + main.yawDeg());
        assertTrue(main.scaleX() >= 0.15F * 1.1F && main.scaleX() < 0.225F * 1.1F
                        && main.scaleY() >= 0.5F * 1.1F && main.scaleY() < 0.6F * 1.1F,
                "the main shard scales (0.15+r*0.075, 0.5+r*0.1) * 1.1 (:56, :88)");
        for (int a = 1; a < 6; a++) {
            final var shard = shards.get(a);
            assertTrue(shard.yawDeg() >= 72 * a && shard.yawDeg() < 72 * a + 36,
                    "shard " + a + " yaw is nextInt(36) + 72*a (integer degrees, :98), got " + shard.yawDeg());
            assertEquals(Math.rint(shard.yawDeg()), shard.yawDeg(), 1.0E-4F, "yaw stays integral (:98)");
            assertTrue(shard.pitchDeg() >= 15 && shard.pitchDeg() < 30);
        }
    }

    @Test
    void crystalTintsAreTheColorsTable() {
        // TileCrystalRenderer.java:74-76 (main) and :89-92 (mixed shards).
        final int[] colors = com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOreItem.COLORS;
        for (int variant = 0; variant <= 5; variant++) {
            assertEquals(colors[variant + 1], CrystalWorldArt.tintFor(variant),
                    "variant " + variant + " tints colors[" + (variant + 1) + "] (:76)");
        }
        assertEquals(colors[5], CrystalWorldArt.tintFor(6), "the mixed cluster mains on colors[5] (:74)");
        for (int a = 1; a <= 4; a++) {
            assertEquals(colors[a], CrystalWorldArt.mixedShardTint(a), "mixed shard a tints colors[a] (:95)");
        }
        assertEquals(colors[6], CrystalWorldArt.mixedShardTint(5), "mixed shard 5 tints colors[6] (:95)");
    }

    @Test
    void crystalOrientationPlacesMatchTheTranslationTable() {
        // TileCrystalRenderer.java:20-39 (translateFromOrientation): orientation
        // 0 = DOWN, 1 = UP, 2 = NORTH, 3 = SOUTH, 4 = WEST, 5 = EAST.
        final float[] down = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.DOWN);
        assertEquals(0.5F, down[0], 1.0E-5F);
        assertEquals(1.3F, down[1], 1.0E-5F);
        assertEquals(180.0F, down[3], 1.0E-5F, "orientation 0 flips the model 180 about X (:23)");
        final float[] up = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.UP);
        assertEquals(-0.3F, up[1], 1.0E-5F, "orientation 1 sits at y - 0.3 (:25)");
        final float[] south = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.SOUTH);
        assertEquals(-0.3F, south[2], 1.0E-5F, "orientation 3 sits at z - 0.3, rotated 90 about X (:30-31)");
        assertEquals(90.0F, south[3], 1.0E-5F, "orientation 3 rotates 90 about X (:31)");
        final float[] north = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.NORTH);
        assertEquals(1.3F, north[2], 1.0E-5F, "orientation 2 sits at z + 1.3, rotated -90 about X (:27-28)");
        assertEquals(-90.0F, north[3], 1.0E-5F, "orientation 2 rotates -90 about X (:28)");
        final float[] west = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.WEST);
        assertEquals(1.3F, west[0], 1.0E-5F, "orientation 4 sits at x + 1.3 (:33)");
        assertEquals(90.0F, west[5], 1.0E-5F, "orientation 4 rotates 90 about Z (:34)");
        final float[] east = CrystalWorldArt.orientationPlacement(net.minecraft.core.Direction.EAST);
        assertEquals(-0.3F, east[0], 1.0E-5F, "orientation 5 sits at x - 0.3 (:36)");
        assertEquals(-90.0F, east[5], 1.0E-5F, "orientation 5 rotates -90 about Z (:37)");
    }

    // --------------------------------------------------------------- mirror

    @Test
    void mirrorPaneSwapsWithTheLink() {
        // TileMirrorRenderer.java:617-629: the linked branch draws the
        // mirrorpanetrans quad (:621), the unlinked branch the mirrorpane quad
        // (:629), both at the 0.02F + jitter standoff (:620, :628).
        final var linked = MirrorWorldArt.plan(true, 0.0F);
        final var unlinked = MirrorWorldArt.plan(false, 0.0F);
        assertEquals(MirrorWorldArt.PANE_TRANSPARENT_SPRITE, linked.paneSprite(),
                "linked draws mirrorpanetrans (:621)");
        assertEquals(MirrorWorldArt.PANE_SPRITE, unlinked.paneSprite(), "unlinked draws mirrorpane (:629)");
        assertEquals(0.02F, unlinked.paneOffset(), 1.0E-5F, "the pane standoff is 0.02F (:628)");
        assertEquals(0.02F + MirrorWorldArt.jitter(50, 1.0F),
                MirrorWorldArt.plan(true, MirrorWorldArt.jitter(50, 1.0F)).paneOffset(), 1.0E-5F,
                "a live link's pane rides the instability jitter (:620 standoff, :578-581 jitter)");
        assertEquals(MirrorWorldArt.TUNNEL_SPRITE, linked.tunnelSprite());
        assertEquals(MirrorWorldArt.FIELD_SPRITE, linked.fieldSprite());
    }

    @Test
    void mirrorJitterIsInstabilityOverTenThousand() {
        // TileMirrorRenderer.java:578-581: random * instability / 10000.
        assertEquals(0.005F, MirrorWorldArt.jitter(50, 1.0F), 1.0E-6F);
        assertEquals(0.0F, MirrorWorldArt.jitter(0, 1.0F), 1.0E-6F, "no instability, no jitter");
        assertTrue(MirrorWorldArt.jitter(9000, 0.3F) < 9000 / 10000.0F, "the random scales it down");
    }

    // ------------------------------------------------------------------ spa

    @Test
    void spaWaterFollowsTheTank() {
        // TileSpa.java:39 (capacity 5000), :65-71 (the getters).
        assertFalse(SpaWorldArt.plan(0, SpaWorldArt.TANK_CAPACITY).visible(), "empty tank, no water");
        assertTrue(SpaWorldArt.plan(1, SpaWorldArt.TANK_CAPACITY).visible(), "1 mB is visible");
        assertEquals(0.3F, SpaWorldArt.plan(1500, SpaWorldArt.TANK_CAPACITY).uScale(), 1.0E-5F,
                "fill fraction across the face");
        assertEquals(1.0F, SpaWorldArt.plan(5000, SpaWorldArt.TANK_CAPACITY).uScale(), 1.0E-5F);
        assertEquals(5000, SpaWorldArt.TANK_CAPACITY, "TileSpa.TANK_CAPACITY");
    }

    // ------------------------------------------------------------- armature

    /**
     * The card's armature scope names
     * {@code BlockArmourersStandRenderer} /
     * {@code BlockEntityDummyRenderer.java:52-70}; the reference tree contains
     * neither, so there is no scene to port: no TER, no plan class, and the
     * registration source must not invent one.
     */
    @Test
    void armatureDeclinesBecauseTheReferenceHasNoRenderer() throws IOException {
        final Path root = findRepoRoot();
        final String registration = Files.readString(
                root.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/client/render/"
                        + "DeviceBlockEntityRenderers.java"),
                StandardCharsets.UTF_8);
        final List<String> registrations = registration.lines()
                .filter(line -> line.contains("registerBlockEntityRenderer"))
                .toList();
        assertTrue(registrations.stream().noneMatch(line -> {
            final String l = line.toLowerCase();
            return l.contains("armature") || l.contains("armour") || l.contains("dummy")
                    || l.contains("mannequin");
        }), "the registration source must register no armature scene: the reference has no "
                + "armourer/dummy renderer to port (the decline lives in its javadoc, never in a "
                + "registration); registrations = " + registrations);
        final Path artDir = root.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/render/art");
        try (final var files = Files.list(artDir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().toLowerCase().contains("armature")
                            || p.getFileName().toString().toLowerCase().contains("dummy")),
                    "the declined armature scene must not grow a plan class");
        }
        final Path reference = referenceTreeRoot();
        assumeTrue(reference != null, "reference tree not on disk");
        try (final var files = Files.walk(reference)) {
            final List<String> renderers = files.map(p -> p.getFileName().toString())
                    .filter(name -> name.contains("Renderer"))
                    .filter(name -> name.contains("Armour") || name.contains("Dummy")
                            || name.contains("Mannequin"))
                    .sorted()
                    .toList();
            assertTrue(renderers.isEmpty(), "no armourer/dummy/mannequin RENDERER exists in the "
                    + "reference (the tree's only Dummy-named files are ContainerDummy, "
                    + "ChampionModDummy and api/internal/DummyInternalMethodHandler — none renders "
                    + "anything), so the card's armature scope has nothing to port; found " + renderers);
        }
    }

    /** The decompiled reference, found by walking up from the project dir. */
    private static Path referenceTreeRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            final Path candidate = dir.resolve("reference/decompiled/thaumcraft/thaumcraft");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    // -------------------------------------------------- published sprites

    /** Sprites the plans name; every {@code thaumcraft:} one must be a CSV row. */
    @Test
    void everyThaumcraftSpriteNamedByTheArtIsAPublishedRow() throws IOException {
        final Path csv = findRepoRoot().resolve("docs/asset-mapping.csv");
        assumeTrue(Files.isRegularFile(csv), "docs/asset-mapping.csv missing — running outside the repo");
        final List<String> rows = Files.readAllLines(csv, StandardCharsets.UTF_8);

        final Path artDir = findRepoRoot()
                .resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/render/art");
        final Pattern quoted = Pattern.compile("\"([a-z0-9_]+/[a-z0-9_]+)\"");
        int checked = 0;
        try (final var files = Files.list(artDir)) {
            for (final Path source : files.toList()) {
                final String text = Files.readString(source, StandardCharsets.UTF_8);
                final Matcher matcher = quoted.matcher(text);
                while (matcher.find()) {
                    final String path = matcher.group(1);
                    // Only sprite-shaped constants (namespace-qualified paths).
                    if (!text.contains("\"" + path + "\"")) {
                        continue;
                    }
                    // Vanilla sprites (minecraft: namespace) live in the vanilla
                    // atlas; the published-sprite contract governs mod sprites.
                    if (text.contains("\"minecraft\", \"" + path + "\"")) {
                        continue;
                    }
                    final String target = "textures/" + path + ".png";
                    // The mod's sprites are the CSV's *target* column in 1.21.1
                    // layout (a textures/blocks/ source row publishes a
                    // textures/block/ target), so match either column.
                    final boolean published = rows.stream().anyMatch(
                            row -> row.startsWith(target + ",") || row.contains("," + target + ","));
                    assertTrue(published, source.getFileName() + " names the sprite path " + path
                            + " which has no published row " + target + " (D-005 / published-sprite contract)");
                    checked++;
                }
            }
        }
        assertTrue(checked >= 6,
                "the scan must find the sprite constants (label, crystal, tunnel, field, pane, "
                        + "panetrans, animatedglow); checked " + checked);
    }

    // ---------------------------------------------------------------- plumbing

    static Path findRepoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src/main/java"))) {
            dir = dir.getParent();
        }
        assumeTrue(dir != null, "no repo root found from the test working dir");
        return dir;
    }
}
