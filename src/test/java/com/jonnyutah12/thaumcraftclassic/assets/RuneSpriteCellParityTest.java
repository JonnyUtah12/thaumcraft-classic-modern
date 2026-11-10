package com.jonnyutah12.thaumcraftclassic.assets;

import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Purity test for the TC-M7-02-c-s-f3-a rune DECAL sprite family: the cells the
 * original's UV math addresses must be exactly the cells this port slices,
 * publishes and ships.
 *
 * <p>{@code ParticleSpriteSlicingTest} proves cell↔pixel equality against the
 * reference sheet when the reference tree is present. CI ships no
 * {@code reference/} tree, so that proof skips there; this test carries the
 * CI-safe half: it folds the original's roll (FXBlockRunes.java:33) through the
 * original's own u/v math (FXBlockRunes.java:51-54) to DERIVE the sheet cells
 * the decal addresses, then pins the shipped strip definition and the slicer's
 * row to those cells. The plan's own DRAWN SET is swept on the game-test server
 * by {@code FxParticleGameTest.runeDecalDrawnSetCoversCellsAndAngles} (a plan
 * resolves {@code ModParticles.RUNE}, so it cannot run in this JVM —
 * {@code Bootstrap.registerBootstraps()} alone is not enough). Together the two
 * tests close the chain: plan frame → strip id → slicer row → sheet cell.</p>
 */
class RuneSpriteCellParityTest {

    /**
     * The original's {@code runeIndex} roll (FXBlockRunes.java:33):
     * {@code (int)(Math.random() * 16.0 + 224.0)} — cells 224..239 of the
     * original's sheet.
     */
    private static final int RAW_ROLL_BASE = 224;
    private static final int RAW_ROLL_SPAN = 16;

    /**
     * The original's v leg (FXBlockRunes.java:53) is the constant 0.375 and its
     * u leg (lines 51-54) is {@code ((runeIndex % 16) + rand) * 0.0624375} with
     * the matching {@code * 0.0624375} inset — the cell inset of a 16-column
     * sheet, the same grid {@code ParticleSpriteSlicingTest} pins at 16 px per
     * cell on the 256×256 fixture sheet.
     */
    private static final int GRID_COLUMNS = 16;
    private static final float UV_V = 0.375F;
    private static final double CELL_INSET = 0.0624375;

    /** Row 6 of the 16-column grid: sheet cells 96..111. */
    private static final int ROW = (int) (UV_V * GRID_COLUMNS);
    private static final int FIRST_CELL = ROW * GRID_COLUMNS;

    /** A swept domain, not one seeded stream (the card's probe rule). */
    private static final int ROLL_SWEEP = 100_000;

    @Test
    void theOriginalsRollFoldsIntoTheRowSixCells() {
        assertEquals(6, ROW, "0.375 * 16 columns is row 6 (FXBlockRunes.java:53)");
        assertEquals(96, FIRST_CELL, "row 6 of a 16-column grid starts at cell 96");

        final Set<Integer> rolls = new TreeSet<>();
        final Set<Integer> cells = new TreeSet<>();
        for (int step = 0; step < ROLL_SWEEP; step++) {
            final double random = (double) step / ROLL_SWEEP;
            final int roll = (int) (random * RAW_ROLL_SPAN + RAW_ROLL_BASE);
            rolls.add(roll);
            // The original's u/v legs, verbatim: u = ((roll % 16) + rand) *
            // 0.0624375 and v = 0.375; the addressed cell is
            // floor(v * 16) * 16 + floor(u * 16).
            for (int sub = 0; sub < 4; sub++) {
                final double u = ((roll % GRID_COLUMNS) + sub / 4.0) * CELL_INSET;
                cells.add((int) (UV_V * GRID_COLUMNS) * GRID_COLUMNS + (int) (u * GRID_COLUMNS));
            }
        }
        assertEquals(range(RAW_ROLL_BASE, RAW_ROLL_BASE + RAW_ROLL_SPAN - 1), rolls,
                "the roll's domain is exactly cells 224..239 (FXBlockRunes.java:33) — a drawn "
                        + "set, not one stream: " + rolls);
        assertEquals(range(FIRST_CELL, FIRST_CELL + GRID_COLUMNS - 1), cells,
                "folding the roll through the original's own u/v math addresses exactly cells "
                        + "96..111 (FXBlockRunes.java:51-54), got " + cells);
    }

    @Test
    void theCommittedStripNamesTheSlicedRowInOrder() throws IOException {
        final Path dir = definitionDir();
        final Path definition = dir.resolve("rune.json");
        assumeTrue(Files.isRegularFile(definition),
                "the particle definition dir is not on this run's layout: " + dir);
        final String json = Files.readString(definition, StandardCharsets.UTF_8);
        final Matcher textures = Pattern.compile("\"textures\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
                .matcher(json);
        assertTrue(textures.find(), "rune.json must carry a textures array, got: " + json);
        final Matcher ids = Pattern.compile(
                "\"" + Pattern.quote("thaumcraft:particle/rune_") + "(\\d+)\"")
                .matcher(textures.group(1));
        final List<Integer> frames = new ArrayList<>();
        while (ids.find()) {
            frames.add(Integer.parseInt(ids.group(1)));
        }
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), frames,
                "rune.json must name the 16 sliced frames in cell order — frame i is sheet cell "
                        + FIRST_CELL + "+i, the row the slicer publishes (build.gradle 'rune' row) "
                        + "and the row the plan draws (FXBlockRunes.java:33, 51-54)");
        assertEquals(16, TcFxPlans.RUNE_FRAMES,
                "16 frames is the original's 16-cell roll (FXBlockRunes.java:33)");
    }

    @Test
    void theSlicerRowPublishesExactlyTheCellsTheMathDerives() throws IOException {
        final Path gradle = repoFile("build.gradle");
        assumeTrue(Files.isRegularFile(gradle),
                "build.gradle is not shipped to the test runtime: " + gradle.toAbsolutePath());
        final String text = Files.readString(gradle, StandardCharsets.UTF_8);
        final Matcher row = Pattern
                .compile("'rune'\\s*:\\s*\\(\\s*(\\d+)\\.\\.(\\d+)\\s*\\)\\s*\\.toList\\(\\)")
                .matcher(text);
        assertTrue(row.find(),
                "the slicer task must publish a 'rune' row in originalParticleCells "
                        + "(docs/particles.md §4.1) — the fixture overlay's slicer check cannot "
                        + "pass without it");
        assertEquals(FIRST_CELL, Integer.parseInt(row.group(1)),
                "the rune row's first cell must be the cell the original's UV math derives "
                        + "(FXBlockRunes.java:51-54 → " + FIRST_CELL + "), got " + row.group(1));
        assertEquals(FIRST_CELL + GRID_COLUMNS - 1, Integer.parseInt(row.group(2)),
                "the rune row must end at the last cell of the original's row, got " + row.group(2));
        assertEquals(0.999, GRID_COLUMNS * CELL_INSET, 1e-9,
                "the sheet grid is 16 columns BECAUSE the original's uv inset is 0.999/16 "
                        + "(FXBlockRunes.java:52, 54) — the same 16 px cell ParticleSpriteSlicingTest "
                        + "pins on the 256x256 fixture sheet");
    }

    /**
     * The senders are the family: a plan nobody calls is a stand-in, so every
     * sender source must route its display tick through the plan this card owns.
     */
    @Test
    void everySenderRoutesThroughItsRunePlan() throws IOException {
        // The original's warding stone has three mutually exclusive branches, and
        // each sends through the one family (BlockCosmeticSolid.java:285-344).
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/blocks/BlockCosmeticSolid.java",
                "TcFxPlans.wardingStoneRunes(", 3, "public void animateTick(");
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/blocks/BlockEldritch.java",
                "TcFxPlans.eldritchTrapRunes(", 1, "public void animateTick(");
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/tiles/TileInfusionMatrix.java",
                "TcFxPlans.matrixRunes(", 1, "public void deviceDisplayTick(");
        // Two senders in the original's doScan: the entity scan (68-79) and the
        // block scan (91-102, 132-143). The node-tile branch (88-107, tile
        // instanceof INode) has no counterpart in the port's scan, so there is
        // nothing there to hook (node milestone).
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/items/ItemThaumometer.java",
                "TcFxPlans.scanEntityRunes(", 1, "private PendingScan doScan(");
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/items/ItemThaumometer.java",
                "TcFxPlans.scanBlockRunes(", 1, "private PendingScan doScan(");
        // The gates that pick the sender, from the same sources.
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/blocks/BlockEldritch.java",
                "EldritchRealmValues.ELDRITCH_TRAP", 2, "public void animateTick(");
        assertCalls("src/main/java/com/jonnyutah12/thaumcraftclassic/tiles/TileInfusionMatrix.java",
                "if (this.crafting) {", 1, "public void deviceDisplayTick(");
    }

    private static void assertCalls(final String relative, final String call, final int expected,
                                    final String enclosingMethod) throws IOException {
        final Path source = repoFile(relative);
        assumeTrue(Files.isRegularFile(source),
                "source tree is not shipped to the test runtime: " + source.toAbsolutePath());
        final String text = Files.readString(source, StandardCharsets.UTF_8);
        assertTrue(text.contains(enclosingMethod),
                relative + " must carry " + enclosingMethod + " — the seam that replaces the "
                        + "original's client display tick");
        int seen = 0;
        for (int at = text.indexOf(call); at >= 0; at = text.indexOf(call, at + 1)) {
            seen++;
        }
        assertEquals(expected, seen,
                relative + " must call " + call + " " + expected + " time(s) — every sender branch "
                        + "of the original needs its port-side call site");
    }

    /**
     * A repo file from the test JVM's working directory. NeoGradle runs unit
     * tests with the working dir under {@code build/minecraft-junit}, so the
     * repo-relative candidates must also walk up — the same reason
     * {@code ParticleSpriteSlicingTest} probes {@code ../reference}.
     */
    private static Path repoFile(final String relative) {
        for (final String candidate : List.of(relative, "../../" + relative,
                "../../../../" + relative)) {
            final Path path = Paths.get(candidate);
            if (Files.isRegularFile(path) || Files.isDirectory(path)) {
                return path;
            }
        }
        return Paths.get(relative);
    }

    private static Set<Integer> range(final int from, final int to) {
        final Set<Integer> cells = new TreeSet<>();
        for (int i = from; i <= to; i++) {
            cells.add(i);
        }
        return cells;
    }

    /**
     * The wired particle-definition dir (build.gradle passes it for the
     * overlay-asset test runs); same fallbacks as ParticleSpriteSlicingTest.
     */
    private static Path definitionDir() {
        final String wired = System.getProperty("tc.particleDefinitionDir");
        if (wired != null) {
            return Paths.get(wired);
        }
        final Path direct = repoFile("src/main/resources/assets/thaumcraft/particles");
        return Files.isDirectory(direct) ? direct
                : Paths.get("src/main/resources/assets/minecraft/particles");
    }
}
