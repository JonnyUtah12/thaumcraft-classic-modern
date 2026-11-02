package com.jonnyutah12.thaumcraftclassic.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BD-18 (t_065502ba): the candle/levitator tints are the original's values,
 * the shipped models carry the faces to tint, and the client handlers that
 * supply the colour are wired where NeoForge 21.1 will actually call them.
 *
 * <h2>Why the wiring is asserted at the source level</h2>
 * {@code RegisterColorHandlersEvent} fires on the <b>mod</b> bus in a real
 * client. The game-test harness is a dedicated server and the unit harness has
 * no client, so neither can observe the event — the same split
 * {@code TrunkScreenRegistrationTest} records for screens. What is checked
 * here is therefore (a) the colour data, (b) the model's tinted faces, and
 * (c) the wiring text, including the bus: FML's {@code @EventBusSubscriber}
 * defaults to the <b>game</b> bus, so a subscriber that omits {@code bus =}
 * never runs and the block silently draws untinted. That is a real defect this
 * card found and fixed in {@link OreBlockColors}, and the last test below is
 * what keeps it from coming back.
 *
 * <p>What is NOT proved here: that a candle renders green in an item frame on
 * screen. That needs {@code runClient}, and {@code docs/FEATURE_MATRIX.md}
 * says so.</p>
 */
class DeviceTintRegistrationTest {

    /** {@code Utils.colors} ({@code Utils.java:50-52}) — pinned, original order. */
    private static final int[] REFERENCE_COLOURS = {
            15790320, 15435844, 12801229, 6719955,
            14602026, 4312372, 14188952, 4408131,
            10526880, 2651799, 8073150, 2437522,
            5320730, 3887386, 11743532, 1973019,
    };

    // --------------------------------------------------------- colour data

    @Test
    void candleColourTableIsTheOriginalsValues() {
        assertEquals(REFERENCE_COLOURS.length, DeviceTints.COLOURS.length,
                "Utils.java:50-52 declares 16 colours");
        for (int i = 0; i < REFERENCE_COLOURS.length; i++) {
            assertEquals(REFERENCE_COLOURS[i], DeviceTints.COLOURS[i],
                    "colour " + i + " drifted from the original table (white…black, Utils.colorNames)");
        }
    }

    @Test
    void everyCandleVariantTintsToItsOwnColour() {
        // BlockCandle.java:76-78: getBlockColor returns Utils.colors[metadata],
        // and the port's variant property carries that metadata verbatim (D-001).
        final Set<Integer> distinct = new LinkedHashSet<>();
        for (int variant = 0; variant < 16; variant++) {
            assertEquals(REFERENCE_COLOURS[variant], DeviceTints.colourForVariant(variant),
                    "variant " + variant + " must tint to the original's colour for that metadata");
            distinct.add(DeviceTints.colourForVariant(variant));
        }
        assertEquals(16, distinct.size(),
                "the tint is per-variant: a table with duplicates would render two colours alike");
        assertEquals(REFERENCE_COLOURS[15], DeviceTints.colourForVariant(99),
                "an out-of-range variant must clamp, not read past the table");
    }

    @Test
    void levitatorGlowTintsAreTheOriginals() {
        // BlockLifterRenderer.java:57 tints the BOTTOM glow face
        // BlockCustomOreItem.colors[4] = 40960; lines 68-83 tint the four side
        // glow faces 14488063 (0xDDDDDD). There is no top glow face.
        assertEquals(40960, DeviceTints.LIFTER_GLOW_BOTTOM,
                "BlockLifterRenderer.java:57 tints the bottom glow face BlockCustomOreItem.colors[4] = 40960");
        assertEquals(14488063, DeviceTints.LIFTER_GLOW_SIDE,
                "BlockLifterRenderer.java:68-83 tint the side glow faces 14488063 (0xDDDDDD)");
    }

    // ---------------------------------------------------- model tinted faces

    @Test
    void candleModelFacesAreTinted() {
        final JsonObject model = model("blockcandle_stem");
        int faces = 0;
        for (final JsonElement element : require(model, "elements")) {
            for (final Map.Entry<String, JsonElement> face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                assertEquals(0, face.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                        "candle face " + face.getKey() + " must carry tintindex 0 — "
                                + "BlockCandle.java:76-78 tinted the whole block, so every face needs the colour");
                faces++;
            }
        }
        assertEquals(6, faces, "the candle stem is a cube: six tinted faces");
    }

    @Test
    void levitatorGlowShellIsTintedWithTheOriginalsIndexes() {
        // BlockLifterRenderer.java:50-89 composes the levitator cube with a
        // glow shell: the bottom face (colours[4], 57-66) and the four sides
        // (0xDDDDDD, 68-83), each side gated on its neighbour being solid.
        // There is NO top glow face — assert the model does not invent one.
        final JsonObject model = model("blocklifter_lifter_glow");
        final JsonArray elements = require(model, "elements");
        assertTrue(elements.size() >= 2,
                "the unpowered model is the levitator cube plus the animatedglow shell");

        final JsonObject base = elements.get(0).getAsJsonObject();
        final JsonObject baseFaces = base.getAsJsonObject("faces");
        assertEquals("thaumcraft:block/liftertop", baseFaces.getAsJsonObject("up").get("texture").getAsString(),
                "BlockLifter.java:36 the levitator's top sprite is liftertop");
        assertEquals("thaumcraft:block/lifterside", baseFaces.getAsJsonObject("north").get("texture").getAsString(),
                "BlockLifter.java:38 the levitator's side sprite is lifterside");
        assertEquals("thaumcraft:block/arcaneearbottom",
                baseFaces.getAsJsonObject("down").get("texture").getAsString(),
                "BlockLifter.java:37 the levitator's bottom sprite is arcaneearbottom");

        final JsonObject glowFaces = new JsonObject();
        for (int i = 1; i < elements.size(); i++) {
            for (final Map.Entry<String, JsonElement> face
                    : elements.get(i).getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                if ("thaumcraft:block/animatedglow".equals(face.getValue().getAsJsonObject()
                        .get("texture").getAsString())) {
                    glowFaces.add(face.getKey(), face.getValue());
                }
            }
        }
        assertEquals(java.util.List.of("down", "east", "north", "south", "west"),
                new java.util.TreeSet<>(glowFaces.keySet()).stream().sorted().toList(),
                "BlockLifterRenderer.java:64-83 draws exactly five glow faces (bottom + four sides); a sixth"
                        + " would be a top glow the original never drew");
        assertEquals(0, glowFaces.getAsJsonObject("down").get("tintindex").getAsInt(),
                "the bottom glow is the colours[4] face (BlockLifterRenderer.java:57) -> tintindex 0");
        for (final String side : new String[]{"north", "south", "west", "east"}) {
            final JsonObject face = glowFaces.getAsJsonObject(side);
            assertEquals(1, face.get("tintindex").getAsInt(),
                    "the " + side + " glow is the 0xDDDDDD face (BlockLifterRenderer.java:68) -> tintindex 1");
            assertFalse(face.get("shade").getAsBoolean(),
                    "the glow draws unshaded, matching the original's full-bright lightmap (bb = 180, line 60)");
            assertEquals(side, face.get("cullface").getAsString(),
                    "the original gates each side glow on its neighbour being solid (lines 69-83);"
                            + " cullface is the JSON spelling of that gate");
        }
    }

    // ------------------------------------------------------- item-frame path

    @Test
    void itemTintFollowsTheStacksVariantCarrier() {
        // An item frame renders the block item, and the block item is coloured
        // from its variant carrier — the same read DeviceBlockColors performs.
        final ItemStack stack = new ItemStack(new Item(new Item.Properties()));
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("variant", "5")));
        final Integer carried = VariantStateItems.variantOf(stack);
        assertEquals(5, carried, "the variant must read back out of the minecraft:block_state component");
        assertEquals(REFERENCE_COLOURS[5], DeviceTints.colourForVariant(carried),
                "a lime candle in an item frame must draw the lime tint");

        final ItemStack plain = new ItemStack(new Item(new Item.Properties()));
        assertNull(VariantStateItems.variantOf(plain),
                "a stack carrying no variant resolves to no tint source (the handler draws white)");
    }

    // ------------------------------------------------------------- wiring

    @Test
    void colourHandlersAreWiredOnTheModBusForTheShippedBlocks() throws Exception {
        final String source = readSource("blocks/DeviceBlockColors.java");
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, "
                        + "bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)"),
                "RegisterColorHandlersEvent is a mod-bus event in NeoForge 21.1 and the subscriber "
                        + "must say so; FML's default is the game bus, where it never fires");
        assertTrue(source.contains("@SubscribeEvent"), "21.1 needs @SubscribeEvent on the handler");
        assertTrue(source.contains("RegisterColorHandlersEvent.Block event")
                        && source.contains("CatalogDecorativeBlocks.BLOCK_CANDLE.get()"),
                "the candle block colour must register for the shipped candle block");
        assertTrue(source.contains("RegisterColorHandlersEvent.Block event")
                        && source.contains("CatalogFurniture.BLOCK_LIFTER.get()"),
                "the levitator glow colour must register for the shipped levitator block");
        assertTrue(source.contains("RegisterColorHandlersEvent.Item event")
                        && source.contains("CatalogDecorativeBlocks.ITEM_CANDLE.get()"),
                "the candle item colour must register, or the item frame / hand / inventory draw untinted");
    }

    @Test
    void everyColourHandlerSubscriberDeclaresTheModBus() throws Exception {
        // The defect this card found: OreBlockColors subscribed on FML's default
        // (game) bus, so the infused-ore overlay drew untinted in game. Scan every
        // colour-handler subscriber in the port; none may rely on the default.
        final Path root = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");
        final Set<Path> sources;
        try (var walk = Files.walk(root)) {
            sources = new LinkedHashSet<>(walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java")).toList());
        }
        int subscribers = 0;
        final StringBuilder offenders = new StringBuilder();
        for (final Path source : sources) {
            final String text = Files.readString(source, StandardCharsets.UTF_8);
            if (!text.contains("RegisterColorHandlersEvent")) {
                continue;
            }
            subscribers++;
            final int at = text.indexOf("@EventBusSubscriber");
            assertTrue(at >= 0, source + " registers colour handlers with no @EventBusSubscriber at all");
            final String annotation = text.substring(at, text.indexOf('\n', at));
            if (!annotation.contains("bus = EventBusSubscriber.Bus.MOD")) {
                offenders.append("\n").append(source.getFileName())
                        .append(" [").append(annotation.trim()).append(']');
            }
        }
        assertTrue(subscribers >= 2,
                "expected the candle/lifter and infused-ore colour subscribers; found " + subscribers
                        + " — this probe can never fail if the scan matches nothing");
        assertEquals("", offenders.toString(),
                "colour-handler subscribers on the default (game) bus never fire, so their tint is "
                        + "silently missing in game:" + offenders);
    }

    // ------------------------------------------------------------- helpers

    private static JsonObject model(final String name) {
        try (InputStream in = DeviceTintRegistrationTest.class
                .getResourceAsStream("/assets/thaumcraft/models/block/" + name + ".json")) {
            assertNotNull(in, name + ".json must be on the test classpath");
            return JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonArray require(final JsonObject model, final String key) {
        final JsonArray array = model.getAsJsonArray(key);
        assertNotNull(array, "the model must declare \"" + key + "\"");
        assertTrue(array.size() > 0, "the model's \"" + key + "\" is empty — no geometry to tint");
        return array;
    }

    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path root = csv.getParent() == null ? Path.of(".") : csv.getParent().getParent();
        assertTrue(Files.isDirectory(root), "expected the repo root at " + root);
        return root;
    }

    private static String readSource(final String relative) throws Exception {
        final Path source = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/" + relative);
        assertTrue(Files.isRegularFile(source), "expected the wiring source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
