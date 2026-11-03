package com.jonnyutah12.thaumcraftclassic.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.Items;
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
        // glow faces 14488063 (0xDD11FF). There is no top glow face.
        assertEquals(40960, DeviceTints.LIFTER_GLOW_BOTTOM,
                "BlockLifterRenderer.java:57 tints the bottom glow face BlockCustomOreItem.colors[4] = 40960");
        assertEquals(14488063, DeviceTints.LIFTER_GLOW_SIDE,
                "BlockLifterRenderer.java:68-83 tint the side glow faces 14488063 (0xDD11FF)");
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
        // (0xDD11FF, 68-83), each side gated on its neighbour being solid.
        // There is NO top glow face — assert the model does not invent one.
        final JsonObject model = model("blocklifter_lifter_glow");
        final JsonArray elements = require(model, "elements");
        assertTrue(elements.size() >= 2,
                "the unpowered model is the levitator cube plus the animatedglow shell");

        final JsonObject base = elements.get(0).getAsJsonObject();
        final JsonObject baseFaces = base.getAsJsonObject("faces");
        assertEquals("thaumcraft:block/liftertop", sprite(model, baseFaces.getAsJsonObject("up")),
                "BlockLifter.java:36 the levitator's top sprite is liftertop");
        assertEquals("thaumcraft:block/lifterside", sprite(model, baseFaces.getAsJsonObject("north")),
                "BlockLifter.java:38 the levitator's side sprite is lifterside");
        assertEquals("thaumcraft:block/arcaneearbottom",
                sprite(model, baseFaces.getAsJsonObject("down")),
                "BlockLifter.java:37 the levitator's bottom sprite is arcaneearbottom");

        final JsonObject glowFaces = new JsonObject();
        for (int i = 1; i < elements.size(); i++) {
            for (final Map.Entry<String, JsonElement> face
                    : elements.get(i).getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                if ("thaumcraft:block/animatedglow".equals(sprite(model, face.getValue().getAsJsonObject()))) {
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
                    "the " + side + " glow is the 0xDD11FF face (BlockLifterRenderer.java:68) -> tintindex 1");
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
        // A unit test runs with the registries frozen: constructing an Item here
        // throws, so the probe exercises the component read on a vanilla stack —
        // the variant carrier is item-agnostic, and the game test covers the
        // shipped candle's own stack end to end.
        final ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("variant", "5")));
        final Integer carried = VariantStateItems.variantOf(stack);
        assertEquals(5, carried, "the variant must read back out of the minecraft:block_state component");
        assertEquals(REFERENCE_COLOURS[5], DeviceTints.colourForVariant(carried),
                "a lime candle in an item frame must draw the lime tint");

        final ItemStack plain = new ItemStack(Items.STONE);
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
            // Only the ANNOTATION line counts: a javadoc mention of the
            // annotation is not a subscription, and scanning prose reported a
            // documented subscriber as an offender.
            final String annotation = java.util.Arrays.stream(text.split("\n"))
                    .map(String::strip)
                    .filter(line -> line.startsWith("@EventBusSubscriber"))
                    .findFirst()
                    .orElse("");
            assertTrue(!annotation.isEmpty(),
                    source + " registers colour handlers with no EventBusSubscriber annotation line at all");
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

    // ------------------------------ BD-18 slice 2b (t_45b89dd2): the lit furnace ring

    @Test
    void burningFurnaceBlockstatePinsEveryKeyAndSwapsTheFrontRing() {
        // BlockStoneDevice.java:153-177 (getConnectedIcon): meta 0 (the alchemical
        // furnace, :155) answers iconFurnace[3] = al_furnace_front_on (:77) on every
        // side while the tile isBurning() (:165-171), and iconFurnace[2] =
        // al_furnace_front_off (:76; the plain getIcon side answer at :101-103)
        // otherwise. metas 1/5/8/12 delegate to the super answer (:172-174) — no
        // other stone device swaps.
        final JsonObject variants = asset("blockstates/blockstonedevice.json").getAsJsonObject("variants");

        for (final Map.Entry<String, JsonElement> entry : variants.entrySet()) {
            assertTrue(entry.getKey().contains("burning="),
                    "blockstonedevice blockstate key '" + entry.getKey() + "' leaves burning a wildcard — one model"
                            + " would be pinned for both the burning and the unburning furnace"
                            + " (BlockStoneDevice.java:153-177 swaps per value)");
        }

        final String unlitRef = blockstateModel(variants, "variant=0,burning=false");
        final String litRef = blockstateModel(variants, "variant=0,burning=true");
        assertFalse(litRef.equals(unlitRef),
                "the burning furnace must name a model of its own — BlockStoneDevice.java:165-171 answers a"
                        + " different sprite while the tile burns");

        final JsonObject unlit = model(unlitRef.substring(unlitRef.indexOf(':') + 1));
        final JsonObject lit = model(litRef.substring(litRef.indexOf(':') + 1));
        for (final String side : new String[]{"north", "south", "east", "west"}) {
            assertEquals("thaumcraft:block/al_furnace_front_off", texture(unlit, side),
                    "BlockStoneDevice.java:76 registers al_furnace_front_off and :101-103 answers it for the"
                            + " furnace's sides — the unburning model must name it for " + side);
            assertEquals("thaumcraft:block/al_furnace_front_on", texture(lit, side),
                    "BlockStoneDevice.java:77 registers al_furnace_front_on and :165-171 answers it while the"
                            + " tile burns — the burning model must name it for " + side);
        }
        assertEquals("thaumcraft:block/al_furnace_top", texture(unlit, "up"),
                "BlockStoneDevice.java:75/:98: the furnace top is al_furnace_top when unlit");
        assertEquals("thaumcraft:block/al_furnace_top", texture(lit, "up"),
                "the top-face swap is the VIS branch (:157-163 answers al_furnace_top_filled while"
                        + " TileAlchemyFurnace.vis > 0) — a tile-driven condition the port's furnace tile"
                        + " (content-mapping thaumcraft:tilealchemyfurnace, not_started) has not shipped, so"
                        + " the burning model must not invent a top change");

        for (int v = 1; v <= 14; v++) {
            assertEquals(blockstateModel(variants, "variant=" + v + ",burning=false"),
                    blockstateModel(variants, "variant=" + v + ",burning=true"),
                    "the original's swap is gated on meta 0 (:155) and metas 1/5/8/12 delegate to the super"
                            + " answer (:172-174) — stone device meta " + v + " never swaps its faces");
        }
    }

    @Test
    void burningFurnaceFireParticlesFollowTheState() throws Exception {
        // BlockStoneDevice.java:194-212 (randomDisplayTick): while the tile is
        // burning, smoke + flame spawn at the four rim offsets around the top
        // (f1 = y + 0.2 + r*5/16, f3 = 0.52, f4 = r*0.5 - 0.25 — eight particles,
        // smoke and flame at each of the four positions).
        final String source = readSource("blocks/BlockStoneDevice.java");
        assertTrue(source.contains("animateTick"),
                "BlockStoneDevice.java:194-212 is the original's randomDisplayTick; the port's animateTick"
                        + " override is where that particle box lives");
        assertTrue(source.contains("Particles.FLAME") && source.contains("Particles.SMOKE"),
                "the original spawned 'smoke' and 'flame' at each rim position (:203-210)");
        assertTrue(source.contains("getValue(BURNING)"),
                "the box must be gated on the published burning state — the tile that knows it is burning"
                        + " is content-mapping thaumcraft:tilealchemyfurnace, not_started");
    }

    // ------------------------------------------------------------- helpers

    /** A blockstate variant key's model reference. */
    private static String blockstateModel(final JsonObject variants, final String key) {
        final JsonObject entry = variants.getAsJsonObject(key);
        assertNotNull(entry, "the blockstate ships no key '" + key + "'");
        return entry.get("model").getAsString();
    }

    /** A cube-parent model's texture-map entry for a face, {@code #alias} resolved. */
    private static String texture(final JsonObject model, final String face) {
        final JsonObject textures = model.getAsJsonObject("textures");
        assertNotNull(textures, "the model declares no textures map");
        String value = textures.get(face).getAsString();
        while (value.startsWith("#")) {
            final String key = value.substring(1);
            assertTrue(textures.has(key), "the model references #" + key + " but declares no such entry");
            value = textures.get(key).getAsString();
        }
        return value;
    }

    private static JsonObject asset(final String path) {
        try (InputStream in = DeviceTintRegistrationTest.class
                .getResourceAsStream("/assets/thaumcraft/" + path)) {
            assertNotNull(in, path + " must be on the test classpath");
            return JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A face's sprite, with a {@code #key} reference resolved through the model's own {@code textures}. */
    private static String sprite(final JsonObject model, final JsonObject face) {
        String texture = face.get("texture").getAsString();
        while (texture.startsWith("#")) {
            final String key = texture.substring(1);
            assertTrue(model.has("textures") && model.getAsJsonObject("textures").has(key),
                    "the model references #" + key + " but declares no such texture entry");
            texture = model.getAsJsonObject("textures").get(key).getAsString();
        }
        return texture;
    }

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
