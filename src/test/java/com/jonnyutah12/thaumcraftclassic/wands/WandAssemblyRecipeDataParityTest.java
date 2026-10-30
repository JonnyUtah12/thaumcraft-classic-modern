package com.jonnyutah12.thaumcraftclassic.wands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-01-s-b data parity: the shipped wand/sceptre assembly rows against the
 * original's two dynamic {@code IArcaneRecipe}s.
 *
 * <p>Source of truth: {@code thaumcraft.common.lib.crafting.ArcaneWandRecipe} /
 * {@code ArcaneSceptreRecipe} of the decompiled tree in
 * {@code reference/decompiled} (layouts from the {@code matches}/
 * {@code getCraftingResult} bodies, prices from {@code getAspects}: every
 * primal at {@code capCraftCost * rodCraftCost}, the sceptre at
 * {@code (int)(capCraftCost * rodCraftCost * 1.5F)}), and the cap/rod numbers
 * from {@code ConfigItems.initializeWandComponents} + {@code postInit}
 * ({@code common/config/ConfigItems.java:287-322}) — the same literals
 * {@code WandComponentsParityTest} pins the Java tables against.</p>
 *
 * <p>What this pins that the game probes cannot: the file <em>set</em>
 * (107 wand + 108 sceptre, with the wood rod + iron cap wand row the original
 * refuses absent and its sceptre row present) and each row's literal payload.
 * The live-decode half runs in {@code gametest/WandAssemblyGameTest}.</p>
 */
class WandAssemblyRecipeDataParityTest {

    /** The recipe dir handle build.gradle wires into the test task. */
    private static Path recipeDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path).resolve("thaumcraft").resolve("recipe");
        assertTrue(Files.isDirectory(dir), "recipe data dir not found at " + dir);
        return dir;
    }

    // tag -> {craftCost, meta, itemId} — ConfigItems.java:287-290 + :314-322.
    private static final Map<String, int[]> CAPS = new TreeMap<>();
    private static final Map<String, String> CAP_ITEMS = new TreeMap<>();
    // tag -> {craftCost, meta} ; itemId is the third entry of the array pair below.
    private static final Map<String, int[]> RODS = new TreeMap<>();
    private static final Map<String, String> ROD_ITEMS = new TreeMap<>();

    static {
        // WAND_CAP_IRON/GOLD/THAUMIUM/VOID + postInit COPPER/SILVER.
        cap("iron", 1, 0);
        cap("gold", 3, 1);
        cap("thaumium", 6, 2);
        cap("void", 9, 7);
        cap("copper", 2, 3);
        cap("silver", 4, 4);

        // WAND_ROD_* (ConfigItems.java:291-300) — the wood rod is a plain stick.
        rod("wood", 1, 0, "minecraft:stick");
        rod("greatwood", 3, 0, "thaumcraft:wandrod");
        rod("obsidian", 6, 1, "thaumcraft:wandrod");
        rod("blaze", 6, 6, "thaumcraft:wandrod");
        rod("ice", 6, 3, "thaumcraft:wandrod");
        rod("quartz", 6, 4, "thaumcraft:wandrod");
        rod("bone", 6, 7, "thaumcraft:wandrod");
        rod("reed", 6, 5, "thaumcraft:wandrod");
        rod("silverwood", 9, 2, "thaumcraft:wandrod");
        // STAFF_ROD_* (ConfigItems.java:301-310) — staff cores are rods, and the
        // original's ArcaneWandRecipe walks WandRod.rods.values(), so they are
        // legal wand cores too.
        rod("greatwood_staff", 8, 50, "thaumcraft:wandrod");
        rod("obsidian_staff", 14, 51, "thaumcraft:wandrod");
        rod("blaze_staff", 14, 56, "thaumcraft:wandrod");
        rod("ice_staff", 14, 53, "thaumcraft:wandrod");
        rod("quartz_staff", 14, 54, "thaumcraft:wandrod");
        rod("bone_staff", 14, 57, "thaumcraft:wandrod");
        rod("reed_staff", 14, 55, "thaumcraft:wandrod");
        rod("silverwood_staff", 24, 52, "thaumcraft:wandrod");
        rod("primal_staff", 32, 100, "thaumcraft:wandrod");
    }

    private static void cap(final String tag, final int cost, final int meta) {
        CAPS.put(tag, new int[]{cost, meta});
        CAP_ITEMS.put(tag, "thaumcraft:wandcap");
    }

    private static void rod(final String tag, final int cost, final int meta, final String item) {
        RODS.put(tag, new int[]{cost, meta});
        ROD_ITEMS.put(tag, item);
    }

    /** The six primal aspects {@code Aspect.getPrimalAspects()} hands the original's price loop. */
    private static final List<String> PRIMALS =
            List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");

    private static final List<String> WAND_PATTERN = List.of("  C", " R ", "C  ");
    private static final List<String> SCEPTRE_PATTERN = List.of("  C", "CR ", "FC ");

    // ------------------------------------------------------------------ the file set

    @Test
    void theCombinationSpaceIsExactlyTheOriginals() throws IOException {
        final Set<String> wand = files("wand.");
        final Set<String> sceptre = files("sceptre.");

        assertEquals(107, wand.size(), "6 caps x 18 rods = 108, minus the wood+iron refusal");
        assertEquals(108, sceptre.size(), "the sceptre recipe has no cap/rod refusal");

        for (final String capTag : CAPS.keySet()) {
            for (final String rodTag : RODS.keySet()) {
                final String stem = capTag + "_" + rodTag + ".arcane_shaped.json";
                final boolean refused = rodTag.equals("wood") && capTag.equals("iron");
                // ArcaneWandRecipe.getCraftingResult: (!br.equals("wood") || !bc.equals("iron"))
                assertEquals(!refused, wand.contains("wand." + stem),
                        "wood rod + iron cap is the one combo the wand recipe refuses");
                assertTrue(sceptre.contains("sceptre." + stem),
                        "ArcaneSceptreRecipe refuses no combo, so " + stem + " must ship");
            }
        }
    }

    // ------------------------------------------------------------------ row payloads

    @Test
    void everyWandRowCarriesTheOriginalLayoutPriceAndGate() throws IOException {
        for (final String capTag : CAPS.keySet()) {
            for (final String rodTag : RODS.keySet()) {
                if (rodTag.equals("wood") && capTag.equals("iron")) {
                    continue;
                }
                final int cost = CAPS.get(capTag)[0] * RODS.get(rodTag)[0];
                assertRow(read("wand." + capTag + "_" + rodTag + ".arcane_shaped.json"),
                        capTag, rodTag, false, cost);
            }
        }
    }

    @Test
    void everySceptreRowCarriesTheOriginalLayoutPriceAndGate() throws IOException {
        for (final String capTag : CAPS.keySet()) {
            for (final String rodTag : RODS.keySet()) {
                // ArcaneSceptreRecipe: (int)(cc * cr * 1.5F)
                final int cost = (int) (CAPS.get(capTag)[0] * RODS.get(rodTag)[0] * 1.5F);
                assertRow(read("sceptre." + capTag + "_" + rodTag + ".arcane_shaped.json"),
                        capTag, rodTag, true, cost);
            }
        }
    }

    private void assertRow(final JsonObject row, final String capTag, final String rodTag,
                           final boolean sceptre, final int cost) {
        final String name = (sceptre ? "sceptre." : "wand.") + capTag + "_" + rodTag;

        assertEquals("thaumcraft:arcane_workbench", row.get("type").getAsString(),
                name + " rides the TC-M4-03-s arcane workbench type");
        assertFalse(row.has("research"),
                name + ": the dynamic original carries no single research key; the gate is research_all");

        // The dual cap+rod gate (ArcaneWandRecipe.checkMatch) + SCEPTRE for the sceptre.
        final List<String> wanted = new ArrayList<>(List.of("CAP_" + capTag, "ROD_" + rodTag));
        if (sceptre) {
            wanted.add("SCEPTRE"); // ItemWandCasting.RESEARCH_SCEPTRE
        }
        assertEquals(wanted, strings(row.getAsJsonArray("research_all")), name + " research gate");

        assertPattern(row, sceptre, name);
        assertKey(row, capTag, rodTag, sceptre, name);

        // ArcaneWandRecipe.getAspects: every primal aspect at capCost * rodCost.
        final JsonArray aspects = row.getAsJsonArray("aspects");
        assertEquals(PRIMALS.size(), aspects.size(), name + " prices each of the 6 primals");
        final Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < aspects.size(); i++) {
            final JsonObject entry = aspects.get(i).getAsJsonObject();
            final String tag = entry.get("aspect").getAsString();
            assertTrue(PRIMALS.contains(tag), name + " prices the non-primal aspect " + tag);
            assertEquals(cost, entry.get("amount").getAsInt(), name + " price of " + tag);
            seen.add(tag);
        }
        assertEquals(new LinkedHashSet<>(PRIMALS), seen, name + " must price every primal");

        // The result: the wand with cap+rod identity, the sceptre marker, and the
        // original's damage value (D-001: carried in thaumcraft:item_meta).
        final JsonObject result = row.getAsJsonObject("result");
        assertEquals("thaumcraft:wandcasting", result.get("id").getAsString(), name + " result id");
        assertEquals(1, result.get("count").getAsInt(), name + " result count");
        final JsonObject components = result.getAsJsonObject("components");
        assertEquals(capTag, components.get("thaumcraft:wand_cap").getAsString(), name + " cap tag");
        assertEquals(rodTag, components.get("thaumcraft:wand_rod").getAsString(), name + " rod tag");
        assertEquals(cost, components.get("thaumcraft:item_meta").getAsInt(), name + " damage value");
        assertTrue(cost <= 1023, name + " damage value must fit the item_meta codec (0..1023)");
        assertEquals(sceptre, components.has("thaumcraft:wand_sceptre"),
                name + ": only the sceptre row carries the wand_sceptre marker");
        if (sceptre) {
            assertTrue(components.get("thaumcraft:wand_sceptre").getAsBoolean(),
                    name + " carries the presence-tested marker as true");
        }

        // Every item the row names is gated, so an unported id skips the row
        // instead of failing the whole data pack.
        final Set<String> named = new LinkedHashSet<>();
        named.add("thaumcraft:wandcasting");
        named.add(CAP_ITEMS.get(capTag));
        named.add(ROD_ITEMS.get(rodTag));
        if (sceptre) {
            named.add("thaumcraft:itemresource");
        }
        final Set<String> gated = new LinkedHashSet<>();
        for (final String guardItem : guards(row)) {
            gated.add(guardItem);
        }
        assertEquals(named, gated, name + " must gate every item it names");
    }

    private static List<String> guards(final JsonObject row) {
        final List<String> out = new ArrayList<>();
        for (final var element : row.getAsJsonArray("neoforge:conditions")) {
            final JsonObject guard = element.getAsJsonObject();
            assertEquals("neoforge:item_exists", guard.get("type").getAsString(),
                    "only the item-exists guard is decodable by the smoke gate");
            out.add(guard.get("item").getAsString());
        }
        return out;
    }

    private static void assertPattern(final JsonObject row, final boolean sceptre, final String name) {
        final List<String> pattern = strings(row.getAsJsonArray("pattern"));
        assertEquals(sceptre ? SCEPTRE_PATTERN : WAND_PATTERN, pattern, name + " layout");
        assertEquals(3, pattern.size(), name + " is a 3x3 grid recipe");
        for (final String line : pattern) {
            assertEquals(3, line.length(), name + " pattern rows are 3 wide (grid slots 0..8)");
        }
    }

    private static void assertKey(final JsonObject row, final String capTag, final String rodTag,
                                  final boolean sceptre, final String name) {
        final JsonObject key = row.getAsJsonObject("key");
        assertComponent(key.getAsJsonObject("C"), CAP_ITEMS.get(capTag), CAPS.get(capTag)[1],
                name + " cap ingredient");
        assertComponent(key.getAsJsonObject("R"), ROD_ITEMS.get(rodTag), RODS.get(rodTag)[1],
                name + " rod ingredient");
        if (sceptre) {
            // new ItemStack(ConfigItems.itemResource, 1, 15) — the primal charm.
            assertComponent(key.getAsJsonObject("F"), "thaumcraft:itemresource", 15,
                    name + " primal charm ingredient");
            assertEquals(3, key.size(), name + " uses exactly cap, rod and charm");
        } else {
            assertEquals(2, key.size(), name + " uses exactly cap and rod");
        }
    }

    private static void assertComponent(final JsonObject ingredient, final String item,
                                        final int meta, final String label) {
        assertNotNull(ingredient, label + " is missing");
        // The bare {"neoforge:components": {...}} form is malformed JSON for the
        // loader; every meta-carrying row must name its ingredient type.
        assertEquals("thaumcraft:wand_component", ingredient.get("type").getAsString(),
                label + " must name the port's checkItemEquals ingredient type");
        assertEquals(item, ingredient.get("item").getAsString(), label + " item");
        assertEquals(meta, ingredient.get("meta").getAsInt(), label + " original damage value");
    }

    // ------------------------------------------------------------------ helpers

    private static Set<String> files(final String prefix) throws IOException {
        try (Stream<Path> s = Files.list(recipeDir())) {
            final Set<String> out = new LinkedHashSet<>();
            s.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(prefix))
                    .forEach(out::add);
            return out;
        }
    }

    private static JsonObject read(final String name) throws IOException {
        final Path path = recipeDir().resolve(name);
        assertTrue(Files.isRegularFile(path), "assembly row " + name + " is not shipped");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<String> strings(final JsonArray array) {
        final List<String> out = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            out.add(array.get(i).getAsString());
        }
        return out;
    }
}
