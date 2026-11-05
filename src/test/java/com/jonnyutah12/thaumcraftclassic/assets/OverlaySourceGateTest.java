package com.jonnyutah12.thaumcraftclassic.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the gate that keeps the overlay CONTENT-INVENTORY assertions honest
 * (kanban t_70da8abe).
 *
 * <p>The hazard: {@code -Ptc.referenceAssets=build/ci-fixture-assets} points the
 * overlay at the placeholder tree CI builds from
 * {@code docs/asset-mapping.csv}. That tree satisfies the presence assumptions
 * the inventory tests used, so the reference-tree counts (66 sound events, 111
 * oggs, 262 files under {@code textures/block/}) ran against 2 events and 256
 * placeholder PNGs and failed — while {@code ./gradlew build} stayed green
 * because CI never runs that shape. Presence separates "no overlay" from
 * "overlay"; only the source tree's own identity separates "reference tree"
 * from "fixture".</p>
 *
 * <p>These probes pin the decision table, the fail-safe direction, and the
 * generator's self-declaration: they go RED if the fixture stops declaring
 * itself, if a marker stops being read, or if a fixture is ever treated as a
 * reference tree (and the reverse — an unmarked tree must never silence the
 * reference assertions).</p>
 */
class OverlaySourceGateTest {

    /** The stand-in for each test's own "no overlay at all" reason. */
    private static final String PRESENCE_REASON = "original-asset overlay not generated";

    @TempDir
    Path dir;

    /** A tree with the overlay layout but no identity card is a reference tree: assertions run. */
    @Test
    void anUnmarkedTreeRunsTheContentInventoryAssertions() throws IOException {
        Path reference = referenceShapedTree("reference");

        assertTrue(OverlaySourceGate.contentInventorySkipReason(reference, true, PRESENCE_REASON).isEmpty(),
                "a tree that declares nothing is a reference tree — the inventory assertions "
                        + "must run there, so an unmarked tree may never produce a skip reason");
    }

    /** The fixture skips the inventory assertions, with a reason that explains the skip. */
    @Test
    void theCiFixtureSkipsThemAndNamesTheTreeItSkipped() throws IOException {
        Path fixture = referenceShapedTree("fixture");
        writeMarker(fixture, "{\"placeholder\": true}");

        Optional<String> reason =
                OverlaySourceGate.contentInventorySkipReason(fixture, true, PRESENCE_REASON);

        assertTrue(reason.isPresent(),
                "the fixture's placeholder bytes cannot carry the reference tree's inventory, "
                        + "so a fixture run must skip the content-inventory assertions");
        for (String mustName : List.of(fixture.toString(), OverlaySourceGate.FIXTURE_MARKER,
                "make_asset_overlay_fixture.py", "reference")) {
            assertTrue(reason.get().contains(mustName),
                    "a skip reason a human has to act on must name the tree, the marker, the "
                            + "generator and the shape that runs the assertions — missing: "
                            + mustName + " in: " + reason.get());
        }
    }

    /** The mechanism this card is about: overlaid content is not evidence of a reference tree. */
    @Test
    void overlaidContentNeverOutranksTheSourceTreesDeclaration() throws IOException {
        Path fixture = referenceShapedTree("fixture");
        writeMarker(fixture, "{\"placeholder\": true}");
        // The overlay output is full-shaped: sounds.json and textures/block both there.
        Path output = referenceShapedTree("overlay-output");

        Optional<String> fromFixtureSource =
                OverlaySourceGate.contentInventorySkipReason(fixture, true, PRESENCE_REASON);
        Optional<String> fromReferenceSource =
                OverlaySourceGate.contentInventorySkipReason(output, true, PRESENCE_REASON);

        assertTrue(fromFixtureSource.isPresent(),
                "the gate reads the source tree: a fixture source skips the inventory assertions "
                        + "even when the overlaid output looks complete");
        assertTrue(fromReferenceSource.isEmpty(),
                "the same shaped output with an unmarked source is a reference tree — its "
                        + "assertions must keep running");
    }

    /** No overlay at all keeps the long-standing presence skip text, verbatim. */
    @Test
    void noOverlayAtAllKeepsTheExistingPresenceReason() {
        Optional<String> reason =
                OverlaySourceGate.contentInventorySkipReason(dir.resolve("absent"), false, PRESENCE_REASON);

        assertEquals(Optional.of(PRESENCE_REASON), reason,
                "CI's plain `build` skips these tests with its own reason text; the fixture arm "
                        + "must not move that message");
    }

    /** Fail-safe: an unset source dir, a marker that lies, and a marker that cannot parse all run the assertions. */
    @Test
    void everyUntrustworthySignalLeavesTheAssertionsRunning() throws IOException {
        Path tree = referenceShapedTree("plain");

        assertTrue(OverlaySourceGate.contentInventorySkipReason(null, true, PRESENCE_REASON).isEmpty(),
                "no wired source dir (tests run outside gradle) must not silence the assertions");

        Path lying = referenceShapedTree("declares-itself-real");
        writeMarker(lying, "{\"placeholder\": false}");
        assertTrue(OverlaySourceGate.contentInventorySkipReason(lying, true, PRESENCE_REASON).isEmpty(),
                "a tree that declares itself real is treated as real");

        Path broken = referenceShapedTree("broken-marker");
        writeMarker(broken, "{ not json");
        assertTrue(OverlaySourceGate.contentInventorySkipReason(broken, true, PRESENCE_REASON).isEmpty(),
                "a marker that does not parse declares nothing: the assertions stay running");

        assertTrue(OverlaySourceGate.contentInventorySkipReason(tree, true, PRESENCE_REASON).isEmpty());
    }

    /** build.gradle wires the property the gate reads — an unwired gate silently asserts nothing. */
    @Test
    void gradleWiresTheOverlaySourceDir() {
        String wired = System.getProperty(OverlaySourceGate.SOURCE_DIR_PROPERTY);
        assertTrue(wired != null && !wired.isEmpty(),
                OverlaySourceGate.SOURCE_DIR_PROPERTY + " not wired — run tests through ./gradlew test; "
                        + "the content-inventory gate has to read the tree the overlay was built from");
    }

    /**
     * The generator's half of the contract: the fixture it writes declares
     * itself, and declares numbers that match the tree on disk — so the skip
     * reason quotes measured facts, not prose. RED if the marker is dropped or
     * its counts drift from the tree.
     */
    @Test
    void theGeneratorDeclaresItsFixtureWithTheCountsItReallyShips() throws IOException {
        Path csv = Path.of(System.getProperty("tc.assetMappingFile", "docs/asset-mapping.csv"));
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(csv),
                "asset-mapping.csv not wired — run tests through ./gradlew test");
        // The project root comes off the wired CSV path, not the working dir:
        // gradle runs this suite with a working dir that is not the project dir.
        Path script = csv.getParent().getParent().resolve("scripts/make_asset_overlay_fixture.py");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(script),
                "fixture generator missing at " + script);

        Path out = dir.resolve("fixture");
        Process process = new ProcessBuilder("python3", script.toString(), out.toString(),
                "--asset-mapping", csv.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = waitForInt(process);
        assertEquals(0, exit, "fixture generator failed: " + output);

        Path marker = out.resolve(OverlaySourceGate.FIXTURE_MARKER);
        assertTrue(Files.isRegularFile(marker),
                "the fixture must declare its own identity at " + marker
                        + "; the overlay content-inventory tests gate on it");
        assertTrue(OverlaySourceGate.declaresPlaceholder(out),
                "the marker must say placeholder: true — that is what the gate reads");

        JsonObject declares = JsonParser.parseString(Files.readString(marker))
                .getAsJsonObject().getAsJsonObject("declares");
        Path textures = out.resolve("thaumcraft/textures");
        Map<String, Integer> measuredPerDir = new TreeMap<>();
        Set<String> measuredSounds = new HashSet<>();
        try (Stream<Path> walk = Files.walk(textures)) {
            walk.filter(Files::isRegularFile).forEach(p -> measuredPerDir.merge(
                    textures.relativize(p).toString().replace('\\', '/').split("/")[0], 1, Integer::sum));
        }
        try (Stream<Path> walk = Files.walk(out.resolve("thaumcraft/sounds"))) {
            walk.filter(Files::isRegularFile).forEach(p -> measuredSounds.add(
                    out.resolve("thaumcraft").relativize(p).toString().replace('\\', '/')));
        }

        int measuredTotal = measuredPerDir.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(measuredTotal, declares.get("textureFiles").getAsInt(),
                "the fixture declares " + declares.get("textureFiles") + " texture files, the tree "
                        + "on disk has " + measuredTotal);
        JsonObject declaredDirs = declares.getAsJsonObject("textureFilesByLegacyDir");
        Set<String> declaredDirNames = new TreeSet<>();
        for (Map.Entry<String, JsonElement> declared : declaredDirs.entrySet()) {
            declaredDirNames.add(declared.getKey());
        }
        assertEquals(measuredPerDir.keySet(), declaredDirNames, "declared texture directories");
        for (Map.Entry<String, Integer> measured : measuredPerDir.entrySet()) {
            assertEquals(measured.getValue().intValue(), declaredDirs.get(measured.getKey()).getAsInt(),
                    "declared file count for textures/" + measured.getKey() + "/");
        }
        Set<String> declaredSounds = new HashSet<>();
        declares.getAsJsonArray("soundFiles").forEach(e -> declaredSounds.add(e.getAsString()));
        assertEquals(measuredSounds, declaredSounds, "the fixture's declared sound files");
        assertEquals(declaredSounds.size(), declares.getAsJsonArray("soundEvents").size(),
                "the fixture declares one sound event per placeholder ogg it writes");

        // The tree it produced is gated as a fixture, with the counts in the reason.
        Optional<String> reason =
                OverlaySourceGate.contentInventorySkipReason(out, true, PRESENCE_REASON);
        assertTrue(reason.isPresent() && reason.get().contains(String.valueOf(measuredTotal)),
                "the skip reason must quote the fixture's own measured inventory: " + reason);
    }

    // --- helpers -------------------------------------------------------------

    /** A tree with the overlay layout: thaumcraft/sounds.json + textures/block + sounds. */
    private Path referenceShapedTree(String name) throws IOException {
        Path root = dir.resolve(name);
        Files.createDirectories(root.resolve("thaumcraft/textures/block"));
        Files.createDirectories(root.resolve("thaumcraft/sounds"));
        Files.writeString(root.resolve("thaumcraft/sounds.json"), "{\"firebat\": {\"sounds\": []}}");
        Files.writeString(root.resolve("thaumcraft/textures/block/amberore.png"), "png");
        Files.writeString(root.resolve("thaumcraft/sounds/alembicknock.ogg"), "ogg");
        return root;
    }

    private static void writeMarker(Path fixtureRoot, String json) throws IOException {
        Files.writeString(fixtureRoot.resolve(OverlaySourceGate.FIXTURE_MARKER), json);
    }

    /** Process.waitFor with the interrupt guarded, so a hung generator fails the test, not the suite. */
    private static int waitForInt(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running " + process, e);
        }
    }
}
