package com.jonnyutah12.thaumcraftclassic.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static java.util.Map.entry;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;

/**
 * BD-18 slice 4 (t_6563ce50): the TERs are wired where NeoForge 21.1 will
 * actually call them, and the card's citations are checked against
 * {@code reference/decompiled/} — the parent card's lesson: five citations in
 * its own body did not exist, and this card's body adds more.
 *
 * <h2>Why the wiring is asserted at the source level</h2>
 * {@code EntityRenderersEvent.RegisterRenderers} fires on the <b>mod</b> bus
 * in a real client ({@code net.neoforged.neoforge.client.event.
 * EntityRenderersEvent$RegisterRenderers}). The game-test harness is a
 * dedicated server and the unit harness has no client, so neither can
 * observe the event — the same split {@code DeviceTintRegistrationTest}
 * records for colour handlers. What is checked: the subscriber file, the
 * {@code bus = EventBusSubscriber.Bus.MOD} attribute (FML's default is the
 * game bus, where the event never fires — the defect that made
 * {@code OreBlockColors} silently dead), {@code value = Dist.CLIENT} (the
 * renderers are client-only classes; loading them on the gametest server is
 * the classloading hazard {@code ModClientScreens} records), and that every
 * tile type with a scene in this slice is registered.
 *
 * <h2>The citations, verified</h2>
 * Every {@code file:line} this slice's code cites must exist and say what is
 * claimed, and the card's phantom citations are pinned absent:
 * {@code BlockSpa.java} (the spa is stone-device meta 12),
 * {@code BlockArmourersStandRenderer.java} and
 * {@code BlockEntityDummyRenderer.java} (there is no armourer/armature
 * renderer in 4.2.3.5 — {@code grep -rn -i armourer reference/decompiled/
 * client} is empty, and armature is not a registered block in the port), and
 * {@code BlockMirror.java:48-50} (that region registers the mirrorframe /
 * mirrorframe2 icons — not "models/pillar + models/door").
 * Tests skip (assumption) when the reference tree is not on disk.
 */
class DeviceRenderRegistrationTest {

    private static final Path SUBSCRIBER = Path.of(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/client/render/DeviceBlockEntityRenderers.java");

    // ---------------------------------------------------------------- wiring

    @Test
    void terSubscriberExists() {
        assertTrue(Files.isRegularFile(SUBSCRIBER),
                "the TER subscriber file must ship (D-021: jar, crystal, mirror, spa scenes are TERs): "
                        + SUBSCRIBER);
    }

    @Test
    void terSubscriberDeclaresModBusAndClientDist() throws IOException {
        assumeTrue(Files.isRegularFile(SUBSCRIBER), "subscriber missing (RED at the parent tip)");
        final String source = Files.readString(SUBSCRIBER, StandardCharsets.UTF_8);
        assertTrue(source.contains("bus = EventBusSubscriber.Bus.MOD"),
                "RegisterRenderers fires on the MOD bus; the FML default game bus is where "
                        + "OreBlockColors sat dead — the bus must be declared");
        assertTrue(source.contains("value = Dist.CLIENT") || source.contains("value=Dist.CLIENT"),
                "the renderers are client classes; the subscriber must be CLIENT-dist so the "
                        + "dedicated server never loads them");
        assertTrue(source.contains("EntityRenderersEvent.RegisterRenderers"),
                "the registration event is EntityRenderersEvent.RegisterRenderers in 21.1");
    }

    @Test
    void everySceneTileTypeIsRegistered() throws IOException {
        assumeTrue(Files.isRegularFile(SUBSCRIBER), "subscriber missing (RED at the parent tip)");
        final String source = Files.readString(SUBSCRIBER, StandardCharsets.UTF_8);
        for (final String tile : List.of("TILE_JAR", "TILE_JAR_VOID", "TILE_CRYSTAL",
                "TILE_MIRROR", "TILE_MIRROR_ESSENTIA", "TILE_SPA")) {
            assertTrue(source.contains(tile + ".get()"),
                    tile + " carries world art in this slice and must be registered");
        }
    }

    @Test
    void atlasShipsTheTerOnlySprites() throws IOException {
        final Path atlas = Path.of("src/main/resources/assets/thaumcraft/atlases/blocks.json");
        assumeTrue(Files.isRegularFile(SUBSCRIBER), "subscriber missing (RED at the parent tip)");
        assertTrue(Files.isRegularFile(atlas),
                "TER-only sprites (label, crystal, aspect icons) need explicit atlas entries — "
                        + "nothing model-references them");
        final String json = Files.readString(atlas, StandardCharsets.UTF_8);
        assertTrue(json.contains("models/label"), "the jar label sprite must be in the atlas");
        assertTrue(json.contains("models/crystal"), "the crystal shard sprite must be in the atlas");
        assertTrue(json.contains("aspects"), "the aspect tag icons must be stitched for jar labels");
    }

    // ------------------------------------------------------------ citations

    @Test
    void jarRendererCitationsExistAsClaimed() throws IOException {
        final Path file = reference("client/renderers/tile/TileJarRenderer.java");
        assumeTrue(Files.isRegularFile(file), "reference tree not on disk");
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(109).contains("0.625F"),
                "TileJarRenderer.java:110 is the height formula: " + lines.get(109));
        assertTrue(lines.get(64).contains("hashCode()"),
                "TileJarRenderer.java:65 is the label tilt seed: " + lines.get(64));
        assertTrue(lines.get(71).contains("label.png"),
                "TileJarRenderer.java:72 draws the label texture: " + lines.get(71));
    }

    @Test
    void crystalRendererCitationsExistAsClaimed() throws IOException {
        final Path file = reference("client/renderers/tile/TileCrystalRenderer.java");
        assumeTrue(Files.isRegularFile(file), "reference tree not on disk");
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(79).contains("field_145848_d * te.field_145849_e"),
                "TileCrystalRenderer.java:80 seeds metadata + x + y * z: " + lines.get(79));
        assertTrue(lines.get(97).contains("72 * a"),
                "TileCrystalRenderer.java:98 is the shard yaw: " + lines.get(97));
    }

    @Test
    void mirrorRendererCitationsExistAsClaimed() throws IOException {
        final Path file = reference("client/renderers/tile/TileMirrorRenderer.java");
        assumeTrue(Files.isRegularFile(file), "reference tree not on disk");
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(26).contains("tunnel.png"),
                "TileMirrorRenderer.java:27 binds the tunnel layer: " + lines.get(26));
        assertTrue(lines.get(27).contains("particlefield.png"),
                "TileMirrorRenderer.java:28 binds the particlefield layer: " + lines.get(27));
        assertTrue(lines.get(620).contains("mirrorpanetrans.png"),
                "TileMirrorRenderer.java:621 is the linked pane: " + lines.get(620));
        assertTrue(lines.get(628).contains("mirrorpane.png"),
                "TileMirrorRenderer.java:629 is the unlinked pane: " + lines.get(628));
    }

    @Test
    void tableRendererCitationIsTheWholeDraw() throws IOException {
        final Path file = reference("client/renderers/tile/TileTableRenderer.java");
        assumeTrue(Files.isRegularFile(file), "reference tree not on disk");
        final String source = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(source.contains("md < 6"),
                "TileTableRenderer.java:22 gates the cloth to metas < 6");
        assertTrue(source.contains("md == 1"),
                "TileTableRenderer.java:27 rotates the pair-placed cloth 90 degrees");
        assertTrue(source.contains("table.png"),
                "TileTableRenderer.java:24 binds the published models/table sprite");
    }

    @Test
    void spaIsStoneDeviceMeta12AndDrawsNoWater() throws IOException {
        final Path file = reference("client/renderers/block/BlockStoneDeviceRenderer.java");
        assumeTrue(Files.isRegularFile(file), "reference tree not on disk");
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(239).contains("metadata == 12"),
                "BlockStoneDeviceRenderer.java:240 is the spa branch: " + lines.get(239));
        final String source = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(source.contains("water"), "the spa branch draws no water — spa water is new art (BD-18)");
        assertTrue(Files.isRegularFile(reference("common/tiles/TileSpa.java")),
                "the tank lives on TileSpa (common/tiles/TileSpa.java)");
    }

    // ------------------------------------------------------ phantom citations

    @Test
    void theCardsPhantomCitationsArePinnedAbsent() throws IOException {
        final Path root = referenceRoot();
        assumeTrue(root != null && Files.isDirectory(root), "reference tree not on disk");

        try (final Stream<Path> all = Files.walk(root)) {
            final List<String> names = all.map(p -> p.getFileName().toString()).toList();
            assertFalse(names.contains("BlockSpa.java"),
                    "the card cites BlockSpa.java:69-71 — the file does not exist in reference/decompiled");
            assertFalse(names.contains("BlockArmourersStandRenderer.java"),
                    "the card cites BlockArmourersStandRenderer — 4.2.3.5 has no armourer stand renderer");
            assertFalse(names.contains("BlockEntityDummyRenderer.java"),
                    "the card cites BlockEntityDummyRenderer.java:52-70 — the tile renderers are named "
                            + "TileDummy*Renderer in 4.2.3.5; no file matches the citation");
        }

        final Path mirror = reference("common/blocks/BlockMirror.java");
        final List<String> lines = Files.readAllLines(mirror, StandardCharsets.UTF_8);
        // Card: "models/pillar + models/door at BlockMirror.java:48-50".
        final String region = String.join("\n", lines.subList(42, 52));
        assertFalse(region.contains("pillar") || region.contains("door"),
                "BlockMirror.java:43-51 registers mirrorframe/mirrorframe2, not models/pillar + "
                        + "models/door — the card's citation is phantom");
        assertTrue(region.contains("mirrorframe"), "the real citation: mirrorframe icons, :44-45");
    }

    @Test
    void tableclothModelRotatesThePairPlacement() throws IOException {
        final Path state = Path.of("src/main/resources/assets/thaumcraft/blockstates/blocktable.json");
        assumeTrue(Files.isRegularFile(state), "blocktable blockstate not shipped yet (RED at parent tip)");
        final String json = Files.readString(state, StandardCharsets.UTF_8);
        for (int variant = 0; variant <= 15; variant++) {
            assertTrue(json.contains("\"variant=" + variant + "\""),
                    "blocktable variant " + variant + " needs a blockstate key");
        }
        assertTrue(json.contains("\"y\": 90"),
                "the pair-placed table rotates 90 degrees (TileTableRenderer.java:27)");
    }

    // ---------------------------------------------------------------- plumbing

    private static Path referenceRoot() {
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

    private static Path reference(final String relative) {
        final Path root = referenceRoot();
        return root == null ? Path.of("reference-missing", relative) : root.resolve(relative);
    }
}
