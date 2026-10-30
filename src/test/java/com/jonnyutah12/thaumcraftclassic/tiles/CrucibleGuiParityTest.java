package com.jonnyutah12.thaumcraftclassic.tiles;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The crucible has no GUI — in 1.7.10 and in the port (TC-M7-04-m1 scope
 * resolution).
 *
 * <p>The original: {@code thaumcraft.client.gui} ships no {@code GuiCrucible}
 * (the reference {@code client/gui/} has 18 Gui* classes — furnace, bore,
 * workbench, spa, trunk, decon, box, golem, pech, mirror, harness, pouch,
 * research table, thaumatorium, research browser/popup/recipe — and none is
 * the crucible). There is no {@code BlockCrucible} in the reference tree
 * either: the crucible is {@code blockMetalDevice} meta 0 — the reference
 * {@code BlockMetalDevice.createTileEntity} returns {@code new TileCrucible()}
 * at line 356 — and that block's only {@code openGui} sites are metas 10/11
 * opening the Thaumatorium ({@code gui_id} 3, BlockMetalDevice.java:717/725).
 * The reference {@code TileCrucible} never calls {@code openGui}, and both
 * proxies ({@code ClientProxy}/{@code CommonProxy}
 * {@code getClientGuiElement}/{@code getServerGuiElement}) return null for
 * every id they do not handle; no crucible id is in their switches. The
 * crucible is a direct-manipulation block: right-click dissolves, shifts,
 * dyes — no window. That is why the content-mapping {@code gui_crucible}
 * row cannot ship a menu type: there is nothing to be at parity with.</p>
 *
 * <p>So this test guards the absence in the port, the same shape the
 * reference exhibits: the tile is not a {@code MenuProvider}, no catalog
 * registers a crucible menu type, and the content-mapping row stays
 * un-implemented by design (its note says so).</p>
 */
class CrucibleGuiParityTest {

    @Test
    void tileCrucibleIsNotAMenuProvider() {
        assertFalse(net.minecraft.world.MenuProvider.class.isAssignableFrom(TileCrucible.class),
                "the crucible tile must not become a MenuProvider: the 1.7.10 crucible opens no window");
        assertFalse(TileCrucible.class.isAssignableFrom(TileArcaneFurnace.class),
                "sanity: the furnace body and the crucible are distinct tiles");
    }

    @Test
    void crucibleHasNoRightClickGuiPath() {
        // The crucible rides blockMetalDevice meta 0 (the reference hosts it
        // there too); the port's host block runs the original's
        // direct-manipulation branches (dissolve/dye/scries) and gains no
        // open path.
        final String source = readSource(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/blocks/BlockMetalDevice.java");
        assertFalse(source.contains("openMenu"),
                "the crucible host block must not open a menu (the original never did for meta 0)");
        assertFalse(source.contains("MenuProvider"),
                "the crucible host block must not wire a menu provider");
        assertFalse(source.contains("openGui"),
                "the crucible host block has no gui-open site at all");
        final String tile = readSource(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/tiles/TileCrucible.java");
        assertFalse(tile.contains("MenuProvider"),
                "the crucible tile must not implement MenuProvider");
    }

    @Test
    void noCatalogRegistersACrucibleMenuType() throws IOException {
        final Path catalogDir = repoRoot()
                .resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog");
        try (var files = Files.list(catalogDir)) {
            for (final Path file : files.toList()) {
                final String source = Files.readString(file, StandardCharsets.UTF_8);
                for (final String line : source.split("\n")) {
                    if (line.contains("MENUS.register(\"") && line.toLowerCase().contains("crucible")) {
                        throw new AssertionError("a crucible menu type appeared: " + file.getFileName()
                                + " — the 1.7.10 crucible has no GUI to pair it with");
                    }
                }
            }
        }
    }

    @Test
    void theContentMappingCrucibleRowStaysOpenByDesign() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(csv.getFileName().toString().equals("content-mapping.csv"),
                "the build must pass tc.contentMappingFile: " + csv);
        final List<String> rows;
        try {
            rows = Files.readAllLines(csv, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final String row = rows.stream()
                .filter(line -> line.contains("thaumcraft:gui_crucible,"))
                .findFirst()
                .orElse(null);
        assertNotNull(row, "docs/content-mapping.csv must keep the gui_crucible row");
        assertTrue(row.contains("no GUI"), "the gui_crucible row's note must state the reference finding: " + row);
    }

    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path root = csv.getParent().getParent();
        assertTrue(Files.isDirectory(root), "repo root off the content-mapping path: " + root);
        return root;
    }

    private static String readSource(final String relativePath) {
        final Path source = repoRoot().resolve(relativePath);
        assertTrue(Files.isRegularFile(source), "expected " + source);
        try {
            return Files.readString(source, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
