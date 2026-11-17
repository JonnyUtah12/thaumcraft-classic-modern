package com.jonnyutah12.thaumcraftclassic.registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spawner-egg and mob-spawn parity units of TC-M6-06-s-c6b (landed by
 * t_55822ab5): the 15 roster eggs, the golem egg of TC-M6-08-s-c3 as the control,
 * and the five biome spawn entries, measured against the ORIGINAL's literals.
 *
 * <p>Every expected value here is the original 1.7.10 literal, transcribed from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/config/ConfigEntities.java}
 * with the line quoted in the assertion message — never a port constant, which is
 * the mutation surface (docs/testing.md; the tautology trap). The last arm
 * re-reads that decompiled file and pins this table against it, and
 * assumption-skips on a tree with no {@code reference/} checkout (CI), so the
 * shipped-side arms above stay live there.</p>
 *
 * <p>The shipped side is read as shipped bytes: the {@code ITEMS.register} lines of
 * {@code registration/catalog/} for the egg items (1.21.1 needs the
 * {@code EntityType} at construction, so {@code DeferredSpawnEggItem} takes the
 * supplier and the two colours as literals — the colour pair is what the client
 * tints the egg with, {@code SpawnEggItem#getColor(0)} = background,
 * {@code getColor(1)} = highlight, the order {@code ItemSpawnerEgg.func_82790_a}
 * answers in as {@code layer == 0 ? color1 : color2}), the built resource tree for
 * the model JSONs and {@code en_us.json}, and {@code docs/content-mapping.csv} for
 * the ledger's {@code egg_colors=} notes and the {@code mob_spawn} figures.</p>
 *
 * <p>The five spawn entries ship as {@code neoforge:biome_modifier} data, one file
 * per original {@code EntityRegistry.addSpawn} call
 * ({@code ConfigEntities.java:161-187}) — SEVEN files, because 1.21.1's
 * {@code Biome.LIST_CODEC} is {@code RegistryCodecs.homogeneousList}: ONE holder set
 * per file, so the two multi-biome entries (the Angry Zombie's monster-list-bearing
 * set, the Halloween all-biome entry) ship as overworld + Nether PAIRS (BD-148,
 * measured: a JSON list of tag strings is a registry-load failure). The {@code :181}
 * Halloween entry rides the port's own {@code thaumcraft:halloween_spawns} type on
 * BOTH halves because 1.21.1's registered datapack conditions carry no date
 * (BD-147). The in-world half of that — the loaded instances, the decoded figures,
 * the biome sets, the calendar rule and the MONSTER category — is measured by
 * {@code MonsterSpawnListGameTest} (3 probes) and the 15 eggs by
 * {@code SpawnEggGameTest} (2 probes); this class pins the shipped files, the
 * catalog sources, the models, the lang keys and the ledger.</p>
 */
class SpawnerEggParityTest {

    /** One original {@code ItemSpawnerEgg.addMapping} line. */
    private record OriginalEgg(String name, String path, int background, int highlight, int line,
            String entitySupplier) {
    }

    /**
     * The 15 roster eggs of TC-M6-06-s-c6b, plus {@code EldritchGolem} — the row
     * TC-M6-08-s-c3 already shipped, kept as the control arm. Transcribed from
     * {@code ConfigEntities.java} (the decompiled original), name / id path /
     * color1 / color2 / line, and the entity holder constant the registration
     * resolves the type through.
     */
    private static final List<OriginalEgg> ORIGINAL_EGGS = List.of(
            new OriginalEgg("BrainyZombie", "brainyzombie", 16761087, 32768, 96, "BRAINY_ZOMBIE"),
            new OriginalEgg("GiantBrainyZombie", "giantbrainyzombie", 16761087, 16384, 98, "GIANT_BRAINY_ZOMBIE"),
            new OriginalEgg("Wisp", "wisp", 16761087, 16777215, 100, "WISP"),
            new OriginalEgg("Firebat", "firebat", 16761087, 12582912, 102, "FIREBAT"),
            new OriginalEgg("Pech", "pech", 16761087, 4194368, 104, "PECH"),
            new OriginalEgg("MindSpider", "mindspider", 11184810, 4210752, 106, "MIND_SPIDER"),
            new OriginalEgg("EldritchGuardian", "eldritchguardian", 2236962, 4210752, 108, "ELDRITCH_GUARDIAN"),
            new OriginalEgg("EldritchWarden", "eldritchwarden", 5579298, 4210752, 110, "ELDRITCH_WARDEN"),
            new OriginalEgg("CultistKnight", "cultistknight", 16732245, 128, 112, "CULTIST_KNIGHT"),
            new OriginalEgg("CultistCleric", "cultistcleric", 16732245, 8388608, 114, "CULTIST_CLERIC"),
            new OriginalEgg("CultistLeader", "cultistleader", 16732245, 5263440, 116, "CULTIST_LEADER"),
            new OriginalEgg("CultistPortal", "cultistportal", 16732245, 16732415, 118, "CULTIST_PORTAL"),
            new OriginalEgg("EldritchGolem", "eldritchgolem", 5592405, 4210752, 120, "ELDRITCH_GOLEM"),
            new OriginalEgg("EldritchCrab", "eldritchcrab", 5592405, 5570560, 122, "ELDRITCH_CRAB"),
            new OriginalEgg("InhabitedZombie", "inhabitedzombie", 5601109, 5570560, 124, "INHABITED_ZOMBIE"),
            new OriginalEgg("ThaumSlime", "thaumslime", 16761087, 16744703, 126, "THAUMIC_SLIME"));

    /** The 15 eggs this card registers (the golem row is c3's, kept as the control). */
    private static final List<OriginalEgg> ROSTER_EGGS = ORIGINAL_EGGS.stream()
            .filter(e -> !e.path().equals("eldritchgolem"))
            .toList();

    /**
     * One original {@code EntityRegistry.addSpawn} call of {@code initEntitySpawns}.
     * {@code files} is the shipped file group of that call: one file for a
     * single-holder-set entry, the overworld + Nether PAIR for a multi-biome entry
     * (1.21.1's codec takes one holder set per file — BD-148).
     */
    private record OriginalSpawn(String label, String mobId, int weight, int min, int max,
            List<String> files, List<String> biomes, String cite) {
    }

    private static final List<OriginalSpawn> ORIGINAL_SPAWNS = List.of(
            new OriginalSpawn("brainyzombie 10/1-1", "brainyzombie", 10, 1, 1,
                    List.of("tc_brainy_zombie_spawns", "tc_brainy_zombie_nether_spawns"),
                    List.of("#minecraft:is_overworld", "#minecraft:is_nether"),
                    "ConfigEntities.java:161-166 (every biome carrying a non-empty monster list — overworld + Nether, the End's list is empty; the set ships as a PAIR, one holder set per file — BD-148)"),
            new OriginalSpawn("pech 10/1-1 MAGICAL", "pech", 10, 1, 1,
                    List.of("tc_pech_spawns"),
                    List.of("#neoforge:is_magical"),
                    "ConfigEntities.java:168-174 (Type.MAGICAL, weight 10, pack 1-1; the shipped tag carries no biome on 1.21.1, the entry selects nothing — BD-146)"),
            new OriginalSpawn("firebat 10/1-2 NETHER", "firebat", 10, 1, 2,
                    List.of("tc_firebat_nether_spawns"),
                    List.of("#minecraft:is_nether"),
                    "ConfigEntities.java:176-177 (Type.NETHER, weight 10, pack 1-2)"),
            new OriginalSpawn("firebat 5/1-2 Halloween", "firebat", 5, 1, 2,
                    List.of("tc_firebat_halloween_spawns", "tc_firebat_halloween_nether_spawns"),
                    List.of("#minecraft:is_overworld", "#minecraft:is_nether"),
                    "ConfigEntities.java:178-181 (the calendar-gated all-biome entry, weight 5, pack 1-2; the gate rides the port's own thaumcraft:halloween_spawns type on BOTH halves — BD-147; the set ships as a PAIR — BD-148)"),
            new OriginalSpawn("wisp 5/1-1 NETHER", "wisp", 5, 1, 1,
                    List.of("tc_wisp_spawns"),
                    List.of("#minecraft:is_nether"),
                    "ConfigEntities.java:185-187 (Type.NETHER, weight 5, pack 1-1)"));

    // The `ITEMS.register("spawnegg.<path>", () -> new DeferredSpawnEggItem(<CONST>::get, <bg>, <fg>, new Item.Properties()))` shape.
    private static final Pattern EGG_REG = Pattern.compile(
            "ITEMS\\.register\\(\"(spawnegg\\.[a-z0-9]+)\",\\s*\\(\\)\\s*->\\s*new DeferredSpawnEggItem\\("
                    + "\\s*([A-Z_]+)::get,\\s*([\\w.]+),\\s*([\\w.]+),\\s*new Item\\.Properties\\(\\)\\)\\);",
            Pattern.DOTALL);

    private static final Pattern ADD_MAPPING = Pattern.compile(
            "ItemSpawnerEgg\\.addMapping\\(\"([^\"]+)\",\\s*(\\d+),\\s*(\\d+)\\);");

    private static final Pattern INT_CONST = Pattern.compile(
            "static final int (\\w+)\\s*=\\s*(0x[0-9a-fA-F]+|\\d+)\\s*;");

    // -------------------------------------------------------------------------------------------
    // the egg items, measured as shipped
    // -------------------------------------------------------------------------------------------

    /**
     * Every roster egg registers under the ledger's dotted id with the original's
     * colour pair, in the original's layer order, over its own mob type.
     */
    @Test
    void theFifteenEggRegistrationsCarryTheOriginalColourPair() throws IOException {
        final Map<String, ShippedEgg> shipped = shippedEggRegistrations();
        final List<String> problems = new ArrayList<>();
        for (final OriginalEgg egg : ORIGINAL_EGGS) {
            final String id = "spawnegg." + egg.path();
            final ShippedEgg found = shipped.get(id);
            if (found == null) {
                problems.add(id + " is not registered by any registration/catalog holder");
                continue;
            }
            if (found.background() != egg.background() || found.highlight() != egg.highlight()) {
                problems.add(id + " is " + found.background() + "/" + found.highlight()
                        + " but ItemSpawnerEgg.addMapping(\"" + egg.name() + "\", "
                        + egg.background() + ", " + egg.highlight() + ") is ConfigEntities.java:"
                        + egg.line());
            }
            if (!found.entity().equals(egg.entitySupplier())) {
                problems.add(id + " resolves its type through " + found.entity()
                        + "::get, not " + egg.entitySupplier() + "::get");
            }
        }
        assertTrue(problems.isEmpty(), "spawner-egg colour/id parity against ConfigEntities.java: " + problems);
        assertEquals(16, shipped.size(), "the catalog registers " + shipped.size()
                + " spawn eggs; this class pins 16 (the 15 roster eggs + the golem control), the roster's own rows ride their mob cards");
    }

    /**
     * The registered id is the ledger row's {@code new_id} verbatim, dot included
     * (BD-40 / D-001 addendum: {@code new_id == "thaumcraft:" + lower(original id
     * path)}), and the ledger's egg inventory and the registration surface are the
     * same set — no egg ships without a row and no row ships unregistered.
     */
    @Test
    void everyLedgerEggRowRegistersUnderItsOwnDottedId() throws IOException {
        final Map<String, ShippedEgg> shipped = shippedEggRegistrations();
        final Set<String> ledgerIds = new LinkedHashSet<>();
        for (final String[] row : contentMappingRows()) {
            if (row[2].equals("spawner_egg")) {
                ledgerIds.add(row[1]);
            }
        }
        final Set<String> registeredIds = new LinkedHashSet<>();
        shipped.forEach((id, e) -> registeredIds.add("thaumcraft:" + id));
        final List<String> registeredOnly = new ArrayList<>(registeredIds);
        registeredOnly.removeAll(ledgerIds);
        final List<String> missingRow = new ArrayList<>();
        for (final OriginalEgg egg : ROSTER_EGGS) {
            final String id = "thaumcraft:spawnegg." + egg.path();
            if (!ledgerIds.contains(id)) {
                missingRow.add(id);
            }
            if (!registeredIds.contains(id)) {
                missingRow.add(id + " (no registration)");
            }
        }
        assertTrue(registeredOnly.isEmpty(),
                "an egg is registered under an id the ledger's spawner_egg rows do not inventory: " + registeredOnly);
        assertTrue(missingRow.isEmpty(),
                "a roster egg has no ledger spawner_egg row (D-001: the ledger's dotted id IS the registered id): "
                        + missingRow);
    }

    /**
     * 1.21.1 picks ONE model per item id and the vanilla egg model is the two-layer
     * template with no own textures — overriding a layer drops the tinted overlay.
     */
    @Test
    void everyEggModelIsTheVanillaSpawnEggTemplateAlone() {
        final List<String> problems = new ArrayList<>();
        for (final OriginalEgg egg : ORIGINAL_EGGS) {
            final String name = "spawnegg." + egg.path() + ".json";
            final String text = readAsset("models/item/" + name);
            assertNotNull(text, "models/item/" + name + " is not shipped — the model file's name IS the registered id");
            final JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            final Set<String> members = new LinkedHashSet<>(json.keySet());
            if (!members.equals(Set.of("parent")) || !json.get("parent").getAsString()
                    .equals("minecraft:item/template_spawn_egg")) {
                problems.add(name + " is " + members + " parent="
                        + (json.has("parent") ? json.get("parent").getAsString() : "<none>")
                        + "; the vanilla egg model is exactly parent=minecraft:item/template_spawn_egg"
                        + " with no own textures (measured: models/item/zombie_spawn_egg.json)");
            }
        }
        assertTrue(problems.isEmpty(), "egg model contract: " + problems);
    }

    /**
     * Every egg is named by the key its own id spells,
     * {@code item.thaumcraft.spawnegg.<path>}, which the localization audit
     * resolves back to the inventoried dotted id by longest dot-boundary prefix
     * (BD-40) — a key that does not carry the id's own path is a dead key.
     */
    @Test
    void everyEggIsNamedByItsOwnLangKey() {
        final String text = readAsset("lang/en_us.json");
        assertNotNull(text, "assets/thaumcraft/lang/en_us.json is not shipped");
        final JsonObject lang = JsonParser.parseString(text).getAsJsonObject();
        final List<String> problems = new ArrayList<>();
        for (final OriginalEgg egg : ORIGINAL_EGGS) {
            final String key = "item.thaumcraft.spawnegg." + egg.path();
            if (!lang.has(key)) {
                problems.add(key + " is missing from en_us.json");
            } else if (!lang.get(key).getAsString().endsWith(" Spawn Egg")) {
                problems.add(key + " reads \"" + lang.get(key).getAsString()
                        + "\", not a \"<name> Spawn Egg\" label");
            }
        }
        assertTrue(problems.isEmpty(), "egg lang keys: " + problems);
    }

    /**
     * The ledger's own {@code egg_colors=} note column carries the same pair the
     * original passed to {@code addMapping}, as six-digit hex — the row is what a
     * reader trusts when the code is not open.
     */
    @Test
    void theLedgerEggRowsCarryTheOriginalColourPair() throws IOException {
        final List<String> problems = new ArrayList<>();
        int rows = 0;
        for (final String[] row : contentMappingRows()) {
            if (!row[2].equals("spawner_egg")) {
                continue;
            }
            for (final OriginalEgg egg : ORIGINAL_EGGS) {
                if (!row[1].equals("thaumcraft:spawnegg." + egg.path())) {
                    continue;
                }
                rows++;
                final String want = String.format("egg_colors=%06X/%06X", egg.background(), egg.highlight());
                final String note = row[7];
                if (!note.contains(want)) {
                    problems.add(row[0] + " notes \"" + note + "\", expected to carry " + want
                            + " (addMapping(\"" + egg.name() + "\", " + egg.background() + ", "
                            + egg.highlight() + ") at ConfigEntities.java:" + egg.line() + ")");
                }
            }
        }
        assertTrue(problems.isEmpty(), "ledger egg_colors parity: " + problems);
        assertEquals(16, rows, "the ledger carries " + rows + " of the 16 pinned spawner_egg rows");
    }

    // -------------------------------------------------------------------------------------------
    // the mob_spawn entries, measured as shipped
    // -------------------------------------------------------------------------------------------

    /**
     * The seven shipped biome-modifier files carry the original's mob, weight and
     * pack size, and the union of each call's file group is the original's biome
     * set — every file carries exactly ONE holder set, which is why the two
     * multi-biome calls ship as overworld + Nether pairs (BD-148, measured:
     * {@code Biome.LIST_CODEC} is {@code RegistryCodecs.homogeneousList} and a JSON
     * list of tag strings is a registry-load failure). The calendar gate is the
     * port's own type on both Halloween halves because 1.21.1's registered datapack
     * conditions (and/false/item_exists/mod_loaded/not/or/tag_empty/true) carry no
     * date (BD-147).
     */
    @Test
    void theFiveShippedSpawnEntriesCarryTheOriginalFigures() throws IOException {
        final Path dir = shippedDataDir("neoforge/biome_modifier");
        final List<String> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.map(p -> p.getFileName().toString()).filter(n -> n.endsWith("_spawns.json")).sorted()
                    .forEach(files::add);
        }
        final List<String> expected = ORIGINAL_SPAWNS.stream()
                .flatMap(s -> s.files().stream().map(f -> f + ".json")).sorted().toList();
        assertEquals(expected, files,
                "the shipped spawn-modifier set is " + files + "; the original's initEntitySpawns has "
                        + ORIGINAL_SPAWNS.size() + " addSpawn calls (ConfigEntities.java:161-187) and 1.21.1 "
                        + "ships the two multi-biome entries as overworld + Nether pairs (BD-148)");
        final List<String> problems = new ArrayList<>();
        for (final OriginalSpawn spawn : ORIGINAL_SPAWNS) {
            final List<String> union = new ArrayList<>();
            for (final String name : spawn.files()) {
                final Path file = dir.resolve(name + ".json");
                if (!Files.exists(file)) {
                    problems.add(name + ".json is not shipped");
                    continue;
                }
                final JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                final String wantType = name.contains("halloween")
                        ? "thaumcraft:halloween_spawns"
                        : "neoforge:add_spawns";
                if (!json.get("type").getAsString().equals(wantType)) {
                    problems.add(name + " is type " + json.get("type").getAsString() + ", not " + wantType);
                }
                final List<String> fileBiomes = new ArrayList<>();
                if (json.get("biomes").isJsonArray()) {
                    json.get("biomes").getAsJsonArray().forEach(b -> fileBiomes.add(b.getAsString()));
                } else {
                    fileBiomes.add(json.get("biomes").getAsString());
                }
                assertEquals(1, fileBiomes.size(), spawn.cite() + " — " + name + " carries " + fileBiomes
                        + "; 1.21.1's Biome.LIST_CODEC takes ONE holder set per file, which is why the "
                        + "multi-biome entries ship as pairs (BD-148)");
                union.addAll(fileBiomes);
                final JsonArray spawners = json.has("spawners")
                        ? json.getAsJsonArray("spawners")
                        : json.getAsJsonArray("spawns");
                assertEquals(1, spawners.size(), spawn.cite() + " adds exactly one entry; " + name
                        + " carries " + spawners.size());
                final JsonObject entry = spawners.get(0).getAsJsonObject();
                assertEquals("thaumcraft:" + spawn.mobId(), entry.get("type").getAsString(),
                        spawn.cite() + " — the mob of " + name);
                assertEquals(spawn.weight(), entry.get("weight").getAsInt(),
                        spawn.cite() + " — the weight of " + name);
                assertEquals(spawn.min(), entry.get("minCount").getAsInt(),
                        spawn.cite() + " — the min pack size of " + name);
                assertEquals(spawn.max(), entry.get("maxCount").getAsInt(),
                        spawn.cite() + " — the max pack size of " + name);
            }
            assertEquals(spawn.biomes(), union, spawn.cite() + " — the shipped set of " + spawn.label()
                    + " is the union of its files, overworld first");
        }
        assertTrue(problems.isEmpty(), "spawn-entry figures against ConfigEntities.java:161-187: " + problems);
    }

    /**
     * The ledger's five {@code mob_spawn} rows — the duplicate FireBat row is the
     * two {@code addSpawn} calls, nether + Halloween — carry the figures the shipped
     * files carry.
     */
    @Test
    void theLedgerMobSpawnRowsMatchTheShippedSpawnEntries() throws IOException {
        final List<String[]> rows = new ArrayList<>();
        for (final String[] row : contentMappingRows()) {
            if (row[2].equals("mob_spawn") && row[0].startsWith("Thaumcraft:monster.Entity")
                    && List.of("EntityBrainyZombie", "EntityPech", "EntityFireBat", "EntityWisp")
                            .contains(row[3])) {
                rows.add(row);
            }
        }
        assertEquals(5, rows.size(), "the ledger carries " + rows.size()
                + " mob_spawn rows for the five roster mobs; the original has 5 addSpawn calls");
        final List<String> problems = new ArrayList<>();
        for (final String[] row : rows) {
            final String mob = switch (row[3]) {
                case "EntityBrainyZombie" -> "brainyzombie";
                case "EntityPech" -> "pech";
                case "EntityFireBat" -> "firebat";
                case "EntityWisp" -> "wisp";
                default -> row[3];
            };
            boolean matched = false;
            for (final OriginalSpawn spawn : ORIGINAL_SPAWNS) {
                if (!spawn.mobId().equals(mob)) {
                    continue;
                }
                final String expected = "weight=" + spawn.weight() + " min=" + spawn.min()
                        + " max=" + spawn.max() + " type=monster";
                // The landed rows carry the figure first, then the shipping/provenance prose.
                if (row[7].equals(expected) || row[7].startsWith(expected + ";")) {
                    matched = true;
                }
            }
            if (!matched) {
                problems.add(row[0] + " notes \"" + row[7] + "\", which is not the shipped figure of any "
                        + mob + " addSpawn call (ConfigEntities.java:161-187 — brainy 10/1-1, pech 10/1-1,"
                        + " firebat 10/1-2 nether and 5/1-2 Halloween, wisp 5/1-1)");
            }
        }
        // The two FireBat rows are the pair of calls: weight 10/1-2 (nether) and 5/1-2 (Halloween).
        final Set<String> firebatNotes = new LinkedHashSet<>();
        rows.stream().filter(r -> r[3].equals("EntityFireBat"))
                .forEach(r -> firebatNotes.add(r[7].split(";", 2)[0].strip()));
        assertEquals(Set.of("weight=10 min=1 max=2 type=monster", "weight=5 min=1 max=2 type=monster"),
                firebatNotes, "the two FireBat mob_spawn rows are the two addSpawn calls"
                        + " (ConfigEntities.java:177 nether, :181 the calendar-gated all-biome entry)");
        assertTrue(problems.isEmpty(), "ledger mob_spawn parity: " + problems);
    }

    // -------------------------------------------------------------------------------------------
    // the decompile, when the tree is checked out
    // -------------------------------------------------------------------------------------------

    /**
     * The pinned table IS the decompiled original: every {@code addMapping} line,
     * its literals and its line number. Assumption-skips on a tree with no
     * {@code reference/} checkout (CI ships a code-only jar and has no decompile),
     * so the shipped-side arms above stay live there.
     */
    @Test
    void thePinnedTableIsTheDecompiledOriginal() throws IOException {
        final Path config = referenceConfigEntities();
        Assumptions.assumeTrue(config != null && Files.exists(config),
                "the reference/ decompile tree is not checked out next to this repo —"
                        + " the pinned addMapping table is unverified against the source on this tree");
        final List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
        final Map<String, int[]> table = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            final Matcher m = ADD_MAPPING.matcher(lines.get(i));
            if (m.find()) {
                table.put(m.group(1), new int[] {Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), i + 1});
            }
        }
        final List<String> problems = new ArrayList<>();
        for (final OriginalEgg egg : ORIGINAL_EGGS) {
            final int[] hit = table.get(egg.name());
            if (hit == null) {
                problems.add("ItemSpawnerEgg.addMapping(\"" + egg.name() + "\", ...) is not in the decompile");
                continue;
            }
            if (hit[0] != egg.background() || hit[1] != egg.highlight()) {
                problems.add(egg.name() + " is " + hit[0] + "/" + hit[1] + " in the decompile, pinned "
                        + egg.background() + "/" + egg.highlight());
            }
            if (hit[2] != egg.line()) {
                problems.add(egg.name() + " is cited at ConfigEntities.java:" + egg.line()
                        + ", the decompile line is :" + hit[2]);
            }
        }
        assertTrue(problems.isEmpty(), "the pinned addMapping table vs the decompile: " + problems);
    }

    // -------------------------------------------------------------------------------------------
    // plumbing
    // -------------------------------------------------------------------------------------------

    /** One shipped egg registration read out of the catalog sources. */
    private record ShippedEgg(String id, String entity, int background, int highlight, String file) {
    }

    private static Map<String, ShippedEgg> shippedEggRegistrations() throws IOException {
        final Path catalog = repoRoot().resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog");
        assertTrue(Files.isDirectory(catalog), "registration/catalog is not under the wired repo root: " + catalog);
        final Map<String, Integer> colourConstants = colourConstants();
        final Map<String, ShippedEgg> shipped = new LinkedHashMap<>();
        final List<String> duplicates = new ArrayList<>();
        final List<Path> sources;
        try (var stream = Files.list(catalog)) {
            sources = stream.filter(p -> p.getFileName().toString().endsWith(".java")).sorted().toList();
        }
        for (final Path file : sources) {
            final String text = Files.readString(file, StandardCharsets.UTF_8);
            final Matcher m = EGG_REG.matcher(text);
            while (m.find()) {
                final int bg = colourValue(m.group(3), colourConstants, file.getFileName().toString());
                final int fg = colourValue(m.group(4), colourConstants, file.getFileName().toString());
                final ShippedEgg egg = new ShippedEgg(m.group(1), m.group(2), bg, fg,
                        file.getFileName().toString());
                if (shipped.put(egg.id(), egg) != null) {
                    duplicates.add(egg.id());
                }
            }
        }
        assertTrue(duplicates.isEmpty(), "an egg id is registered twice: " + duplicates);
        return shipped;
    }

    /** The {@code EGG_PRIMARY}/{@code EGG_SECONDARY}-style colour constants of the tree. */
    private static Map<String, Integer> colourConstants() throws IOException {
        final Path src = repoRoot().resolve("src/main/java");
        final Map<String, Integer> values = new LinkedHashMap<>();
        final List<String> conflicts = new ArrayList<>();
        final List<Path> files;
        try (var walk = Files.walk(src)) {
            files = walk.filter(p -> p.getFileName().toString().endsWith(".java")).sorted().toList();
        }
        for (final Path file : files) {
            final Matcher m = INT_CONST.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (m.find()) {
                final String token = m.group(1);
                if (!token.startsWith("EGG_")) {
                    continue;
                }
                final int v = m.group(2).startsWith("0x")
                        ? Integer.parseInt(m.group(2).substring(2), 16)
                        : Integer.parseInt(m.group(2));
                final Integer prior = values.put(token, v);
                if (prior != null && prior != v) {
                    conflicts.add(token + " = " + prior + " and " + v);
                }
            }
        }
        assertTrue(conflicts.isEmpty(), "an egg colour constant is declared twice with different values: "
                + conflicts);
        return values;
    }

    private static int colourValue(final String token, final Map<String, Integer> constants, final String fromFile) {
        if (token.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(token);
        }
        final String name = token.substring(token.lastIndexOf('.') + 1);
        final Integer v = constants.get(name);
        assertNotNull(v, "the egg colour " + token + " of " + fromFile + " resolves to no int constant of the tree");
        return v;
    }

    private static List<String[]> contentMappingRows() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(csv.getFileName().toString().equals("content-mapping.csv"),
                "the build must wire tc.contentMappingFile — run the suite through ./gradlew test (got " + csv + ")");
        final List<String[]> rows = new ArrayList<>();
        for (final String line : Files.readAllLines(csv, StandardCharsets.UTF_8)) {
            final String text = line.strip();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            final String[] fields = splitCsvLine(text);
            assertEquals(8, fields.length, "content-mapping row parses to " + fields.length + " fields: " + text);
            rows.add(fields);
        }
        return rows;
    }

    /** A quote-aware split, because display columns legitimately carry quoted commas. */
    private static String[] splitCsvLine(final String line) {
        final List<String> fields = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static Path repoRoot() {
        final String prop = System.getProperty("tc.repoRootDir");
        assertNotNull(prop, "tc.repoRootDir is not wired — run the suite through ./gradlew test");
        final Path root = Path.of(prop);
        assertTrue(Files.isDirectory(root.resolve("src/main/java")),
                "tc.repoRootDir does not name a source tree: " + root);
        return root;
    }

    /** The built asset tree (the shipped bytes): models, lang. */
    private static Path modAssetsDir() {
        final String prop = System.getProperty("tc.modAssetsDir");
        assertNotNull(prop, "tc.modAssetsDir is not wired — run the suite through ./gradlew test");
        final Path dir = Path.of(prop);
        assertTrue(Files.isDirectory(dir), "tc.modAssetsDir does not name a built asset tree: " + dir);
        return dir;
    }

    /** The shipped datapack payload tree. */
    private static Path shippedDataDir(final String underNamespace) {
        final String prop = System.getProperty("tc.modDataDir");
        assertNotNull(prop, "tc.modDataDir is not wired — run the suite through ./gradlew test");
        final Path dir = Path.of(prop).resolve("thaumcraft").resolve(underNamespace);
        assertTrue(Files.isDirectory(dir), "the shipped data tree has no " + dir);
        return dir;
    }

    private static String readAsset(String relative) {
        final Path file = modAssetsDir().resolve("thaumcraft").resolve(relative);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The decompiled {@code ConfigEntities.java}, resolved by walking UP from the
     * repo root: a {@code .worktrees/<task>} checkout sits below the repo, so the
     * bare {@code ../reference} idiom of {@code worldgen/StructureParityTest} looks
     * inside {@code .worktrees/} and skips silently there (docs/catalog-slice).
     */
    private static Path referenceConfigEntities() {
        Path dir = repoRoot().getParent();
        final String tail = "reference/decompiled/thaumcraft/thaumcraft/common/config/ConfigEntities.java";
        for (int up = 0; dir != null && up < 6; up++, dir = dir.getParent()) {
            final Path candidate = dir.resolve(tail);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
