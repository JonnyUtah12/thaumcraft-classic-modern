package com.jonnyutah12.thaumcraftclassic.localization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit guards for the shipped localization file (TC-LOC-s / t_a2a74726).
 *
 * <p>The live-registry half of the contract — every registered item/block/menu
 * has an entry, and no entry is dead — runs in-game
 * ({@code LocalizationGameTest}), because the real id set only exists on the
 * game-test server. This class covers what is decidable from the resources
 * alone: the file parses and is populated, the keys follow the 1.21.1
 * conventions, every registry-derived key names content that
 * {@code docs/content-mapping.csv} inventories, the translation keys the
 * shipped code passes to {@code Component.translatable} are all present, and —
 * on a build that has the reference tree — every per-meta name preserves the
 * original Thaumcraft 4.2.3.5 wording.</p>
 */
class LocalizationFileTest {

    /** 1.21.1 key conventions for the content this mod ships. */
    private static final List<String> CURRENT_PREFIXES = List.of(
            "item.thaumcraft.", "block.thaumcraft.", "container.thaumcraft.",
            "entity.thaumcraft.", "itemGroup.thaumcraft.",
            // 1.21.1 enchantments are datapack entries labelled by their own
            // key shape (TC-M6-05-s-e1c: ench_repair, ench_haste).
            "enchantment.thaumcraft.",
            "death.attack.thaumcraft.", "fluid.thaumcraft.",
            // TC-M6-04f-m: the vanilla biome description id shape
            // "biome.<namespace>.<path>" (the Eldritch biome).
            "biome.thaumcraft.");

    /**
     * Keys the shipped code uses with the original 1.7.10 key shape, kept
     * verbatim so the original strings apply unchanged
     * (ItemAmuletVis:206/209, ItemBaubleBlanks:142, ItemResonator:66-72,
     * ItemHandMirror:142-158, ResearchManager:460, ItemEldritchObject:81-92,
     * TileEldritchLock:136/220/300/340,
     * ItemHandMirror:142-158, ResearchManager:460,
     * EntityTaintacleGiant:122 — {@code StatCollector.translateToLocal(
     * "tc.boss.enrage")} in the enrage announcement, en_US.lang:761).
     */
    private static final List<String> LEGACY_KEYS = List.of(
            "item.ItemAmuletVis.text", "item.capacity.text",
            "tc.discount", "tc.resonator1", "tc.resonator2", "tc.resonator3",
            "tc.handmirrorlinkedto", "tc.handmirrorlinked", "tc.handmirrorerror",
            "tc.researcherror",
            // TC-M6-07-s: the robe/goggles tooltip line, the original's own key
            // (ItemRobeArmor.java:132-133, ItemGoggles.java:40-42).
            "tc.visdiscount",
            "item.ItemEldritchObject.text.1", "item.ItemEldritchObject.text.2",
            "item.ItemEldritchObject.text.3", "item.ItemEldritchObject.text.4",
            "item.ItemEldritchObject.text.5", "item.ItemEldritchObject.text.6",
            "tc.boss.crimson", "tc.boss.golem", "tc.boss.taint", "tc.boss.warden",
            // TC-M6-06-s-e: the giant taintacle's enrage announcement, the original's
            // own key (EntityTaintacleGiant.java:122 via
            // StatCollector.translateToLocal("tc.boss.enrage"), en_US.lang:761).
            "tc.boss.enrage",
            // TC-M3-04-s-ii: the research note's blank-note tooltip lines and
            // its warp block, kept in the original 1.7.10 key shape.
            "item.researchnotes.unknown.1", "item.researchnotes.unknown.2",
            "tc.forbidden", "tc.forbidden.level.1", "tc.forbidden.level.2",
            "tc.forbidden.level.3", "tc.forbidden.level.4", "tc.forbidden.level.5",
            // TC-M3-04-s-v: the Thaumonomicon node-click hint lines, the note
            // popup and the requisition failure line, in the original 1.7.10
            // key shape (GuiResearchBrowser.java:614-618, :656-658, :683-727,
            // :186-191, PacketPlayerCompleteToServer.java:52-92), with the
            // original en_US.lang wording.
            "tc.research.getprim", "tc.research.hasnote", "tc.research.popup",
            "tc.research.purchase", "tc.research.short", "tc.research.shortprim",
            "tc.researchmissing",
            // TC-M3-04-s-iii: the research-note screen's ink tooltip lines and
            // its copy-cost header (GuiResearchTable:172-183, :327), each string
            // verbatim from the original en_US.lang.
            "tile.researchtable.noink.0", "tile.researchtable.noink.1", "tc.research.copy",
            // TC-M6-08-s: the original tc.adv label (ConfigItems' advanced-golem
            // tooltip line), kept verbatim.
            "tc.adv",
            // TC-M3-04-s-iv-b: the Thaumonomicon crafting-page station labels, kept in
            // the original key shape (GuiResearchRecipe:821 / :888, the key printed centred
            // above the ingredient grid of a crafting page).
            "recipe.type.workbench", "recipe.type.workbenchshapeless");

    /**
     * Registry-derived key shape: lower-case id plus up to two optional
     * suffixes — one meta/variant segment ({@code .0}, {@code .void}) and one
     * tooltip segment ({@code .desc}, TC-M6-08-s's golem upgrade
     * description lines, the ported {@code item.ItemGolemUpgrade.<meta>.desc}).
     */
    private static final Pattern KEY_SHAPE =
            Pattern.compile("(?:item|block|entity|itemGroup)\\.thaumcraft\\.[a-z0-9_]+(?:\\.[a-z0-9]+){0,2}");

    /** Every translation key the shipped code passes to Component.translatable that is not registry-derived. */
    private static final List<String> CODE_AUTHORED_KEYS = List.of(
            "container.thaumcraft.chesthungry", "container.thaumcraft.deconstructiontable",
            "container.thaumcraft.magicbox", "container.thaumcraft.spa",
            // BOOK-1 (t_1e6b18cc): the Thaumonomicon browser is containerless
            // (GuiResearchBrowser extends GuiScreen), so no registered menu
            // type derives a container.* key for it. ThaumonomiconScreen.TITLE_KEY
            // is the title the screen translates itself (BD-22).
            "container.thaumcraft.thaumonomicon",
            // TC-M3-04-s-iii: the research-note screen's MenuType title
            // ("Research Table", the original tile's inventory name).
            "container.thaumcraft.gui_research_note",
            "item.thaumcraft.itemlootbag.tooltip", "item.thaumcraft.trunkspawner.tooltip",
            "item.thaumcraft.itemgolemupgrade.0", "item.thaumcraft.itemgolemupgrade.1",
            "item.thaumcraft.itemgolemupgrade.2", "item.thaumcraft.itemgolemupgrade.3",
            "item.thaumcraft.itemgolemupgrade.4", "item.thaumcraft.itemgolemupgrade.5",
            "item.thaumcraft.itemgolemupgrade.0.desc", "item.thaumcraft.itemgolemupgrade.1.desc",
            "item.thaumcraft.itemgolemupgrade.2.desc", "item.thaumcraft.itemgolemupgrade.3.desc",
            "item.thaumcraft.itemgolemupgrade.4.desc", "item.thaumcraft.itemgolemupgrade.5.desc",
            "tc.adv",
            "item.capacity.text", "item.ItemAmuletVis.text",
            "item.thaumcraft.itemresearchnotes.discovery",
            "item.researchnotes.unknown.1", "item.researchnotes.unknown.2",
            "tc.forbidden", "tc.forbidden.level.1", "tc.forbidden.level.2",
            "tc.forbidden.level.3", "tc.forbidden.level.4", "tc.forbidden.level.5",
            "tc.discount", "tc.resonator1", "tc.resonator2", "tc.resonator3",
            "tc.handmirrorlinkedto", "tc.handmirrorlinked", "tc.handmirrorerror", "tc.researcherror",
            "tc.visdiscount",
            "item.ItemEldritchObject.text.1", "item.ItemEldritchObject.text.2",
            "item.ItemEldritchObject.text.3", "item.ItemEldritchObject.text.4",
            "item.ItemEldritchObject.text.5", "item.ItemEldritchObject.text.6",
            "tc.boss.crimson", "tc.boss.golem", "tc.boss.taint", "tc.boss.warden",
            "tc.research.getprim", "tc.research.hasnote", "tc.research.popup", "tc.research.purchase",
            "tc.research.short", "tc.research.shortprim", "tc.researchmissing",
            "tc.adv",
            // TC-M6-01: the wand display name composes one translatable per part (the
            // original item.Wand.<cap>.cap / .<rod>.rod / .{wand,staff,sceptre}.obj keys,
            // collapsed to the port's one-suffix key shape) plus the item.Wand.name format.
            "item.thaumcraft.wandcasting.iron",
            "item.thaumcraft.wandcasting.gold",
            "item.thaumcraft.wandcasting.copper",
            "item.thaumcraft.wandcasting.silver",
            "item.thaumcraft.wandcasting.thaumium",
            "item.thaumcraft.wandcasting.void",
            "item.thaumcraft.wandcasting.wood",
            "item.thaumcraft.wandcasting.greatwood",
            "item.thaumcraft.wandcasting.silverwood",
            "item.thaumcraft.wandcasting.obsidian",
            "item.thaumcraft.wandcasting.blaze",
            "item.thaumcraft.wandcasting.ice",
            "item.thaumcraft.wandcasting.bone",
            "item.thaumcraft.wandcasting.reed",
            "item.thaumcraft.wandcasting.quartz",
            "item.thaumcraft.wandcasting.primal",
            "item.thaumcraft.wandcasting.wand",
            "item.thaumcraft.wandcasting.sceptre",
            "item.thaumcraft.wandcasting.staff",
            "tc.boss.enrage",
            // TC-M3-04-s-iv-b: the Thaumonomicon crafting-page station labels, read by
            // RecipePageDisplay.station().labelKey() (original recipe.type.* keys).
            "recipe.type.workbench", "recipe.type.workbenchshapeless",
            // The arcane workbench window's own labels (TC-M7-04-m2): the vis
            // notice the original hardcoded as "Insufficient vis"
            // (GuiArcaneWorkbench.java:105) and the wand-slot tooltip the port
            // added where the original drew an unlabelled slot. Neither names a
            // content id, so they are code-authored, not registry keys.
            "container.thaumcraft.insufficient_vis", "container.thaumcraft.wand_slot_hint");

    /**
     * The shipped multi-meta items and the original-metadata domain their
     * {@code getName(ItemStack)} walks (mirrors the {@code META_COUNT} /
     * {@code VALID_METAS} / ITEM_META rarity contract of each class; nuggets
     * use the original's sparse damage values 0-7, 16-21, 31).
     */
    private static final Map<String, int[]> META_DRIVEN = metaDriven();

    private static Map<String, int[]> metaDriven() {
        final Map<String, int[]> metas = new java.util.LinkedHashMap<>();
        metas.put("itemresource", range(0, 18));
        metas.put("itemshard", range(0, 6));
        metas.put("itemnugget", concat(range(0, 7), concat(range(16, 21), new int[] {31})));
        metas.put("itemlootbag", range(0, 2));
        metas.put("itembaubleblanks", range(0, 3));
        metas.put("itemamuletvis", range(0, 1));
        metas.put("itemamuletrunic", range(0, 1));
        metas.put("itemringrunic", range(0, 3));
        metas.put("itemgirdlerunic", range(0, 1));
        metas.put("itemeldritchobject", range(0, 4));        // TC-M6-08-s golem families: the placer's eight materials, the twelve
        // cores plus the blank core at 100, the six upgrades, the eight
        // accessories.
        metas.put("itemgolemplacer", range(0, 7));
        metas.put("itemgolemcore", concat(range(0, 11), new int[] {100}));
        metas.put("itemgolemupgrade", range(0, 5));
        metas.put("itemgolemdecoration", range(0, 7));
        return java.util.Collections.unmodifiableMap(metas);
    }

    private static int[] range(final int from, final int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).toArray();
    }

    private static int[] concat(final int[] a, final int[] b) {
        return java.util.stream.IntStream.concat(java.util.stream.IntStream.of(a), java.util.stream.IntStream.of(b))
                .toArray();
    }

    private static JsonObject lang() {
        try (InputStream in = LocalizationFileTest.class.getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void langFileParsesAndIsFullyPopulated() {
        final JsonObject lang = lang();
        assertTrue(lang.size() >= 120,
                "expected one entry per shipped key (120+), got " + lang.size());
        for (final String key : lang.keySet()) {
            assertTrue(lang.get(key).isJsonPrimitive() && lang.get(key).getAsString().length() > 0,
                    key + " has an empty value");
        }
    }

    @Test
    void keysFollowThe1211KeyConventions() {
        for (final String key : lang().keySet()) {
            final boolean current = CURRENT_PREFIXES.stream().anyMatch(key::startsWith);
            final boolean legacy = LEGACY_KEYS.contains(key);
            assertTrue(current || legacy,
                    key + " is neither a 1.21.1 current-shape key nor a documented original-shape key");
            if (key.startsWith("item.thaumcraft.") || key.startsWith("block.thaumcraft.")
                    || key.startsWith("entity.thaumcraft.")) {
                assertTrue(KEY_SHAPE.matcher(key).matches(), key + " is not a lower-case id key");
            }
        }
    }

    @Test
    void valuesCarryNoLegacyMarkup() {
        final JsonObject lang = lang();
        for (final String key : lang.keySet()) {
            final String value = lang.get(key).getAsString();
            assertTrue(value.indexOf('\u00a7') < 0,
                    key + " keeps 1.7-era section-sign colour codes; 1.21.1 renders them literally");
            assertTrue(!value.contains("%TYPE"),
                    key + " keeps the original %TYPE token; 1.21.1 formats with %s");
            assertTrue(!value.contains("<BR>"),
                    key + " keeps original BBCode-style markup");
            final String withoutPlaceholders = value.replaceAll("%(?:\\d+\\$)?[sdc]", "");
            assertTrue(!withoutPlaceholders.contains("%"),
                    key + " has an unsupported format placeholder in: " + value);
        }
    }

    @Test
    void everyCodeTranslatedKeyIsShipped() {
        final JsonObject lang = lang();
        final List<String> missing = CODE_AUTHORED_KEYS.stream().filter(k -> !lang.has(k)).sorted().toList();
        assertEquals(List.of(), missing, "Component.translatable keys the shipped code uses are not in en_us.json");
    }

    @Test
    void everyRegistryKeyNamesInventoriedContent() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");

        final java.util.Set<String> inventory = new java.util.HashSet<>();
        for (final com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.Row row
                : com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.read(csv)) {
            final int colon = row.newId().indexOf(':');
            inventory.add(colon > 0 ? row.newId().substring(colon + 1) : row.newId());
        }

        final JsonObject lang = lang();
        final List<String> orphans = lang.keySet().stream()
                .filter(key -> key.startsWith("item.thaumcraft.") || key.startsWith("block.thaumcraft.")
                        || key.startsWith("container.thaumcraft.") || key.startsWith("entity.thaumcraft."))
                .filter(key -> !CODE_AUTHORED_KEYS.contains(key))
                .filter(key -> !inventoriedById(key, inventory))
                .map(key -> key.split("\\.")[2])
                .distinct()
                .sorted()
                .toList();
        assertEquals(List.of(), orphans,
                "en_us.json keys name ids that docs/content-mapping.csv does not inventory");
    }

    /**
     * Does a registry-derived key name content the ledger inventories?
     *
     * <p>The naive reading — the key's third segment is the id — is only right
     * for ids whose path has no dot. The ledger's own id contract is the
     * narrower rule: {@code new_id} is exactly {@code "thaumcraft:" +
     * lower(original id path)} ({@code ContentMappingIdContractTest}, D-001
     * addendum) and that path charset is {@code [a-z0-9/._-]+}, so a
     * spawner_egg row inventories the WHOLE dotted path —
     * {@code thaumcraft:spawnegg.eldritchgolem} for the original
     * {@code spawnegg.EldritchGolem} — and the item's 1.21.1 lang key is
     * {@code item.thaumcraft.<that whole path>}. The key shape audit
     * ({@link #keysFollowThe1211KeyConventions}) already permits a trailing
     * segment: the per-meta suffix ({@code .3}) and the tooltip suffix
     * ({@code .desc}) are the same syntax. So a key belongs to the inventoried
     * id that is its longest dot-boundary prefix: {@code spawnegg.eldritchgolem}
     * resolves for {@code item.thaumcraft.spawnegg.eldritchgolem}, and
     * {@code itemgolemplacer} resolves for {@code item.thaumcraft.itemgolemplacer.3}.
     * Nothing is widened for a key whose id is simply absent: the whole
     * remainder and every one of its dotted prefixes must be checked, and all
     * of them miss.</p>
     */
    private static boolean inventoriedById(final String key, final java.util.Set<String> inventory) {
        final String[] parts = key.split("\\.");
        if (parts.length < 3) {
            return false;
        }
        final StringBuilder id = new StringBuilder(parts[2]);
        if (inventory.contains(id.toString())) {
            return true;
        }
        for (int i = 3; i < parts.length; i++) {
            id.append('.').append(parts[i]);
            if (inventory.contains(id.toString())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void everyMultiMetaItemHasItsFullMetaSet() {
        final JsonObject lang = lang();
        META_DRIVEN.forEach((id, metas) -> {
            for (final int meta : metas) {
                final String key = "item.thaumcraft." + id + "." + meta;
                assertTrue(lang.has(key) && lang.get(key).isJsonPrimitive()
                                && lang.get(key).getAsString().length() > 0,
                        key + " is missing; " + id + " getName() emits it for that original metadata");
            }
        });
        assertTrue(lang.get("item.thaumcraft.itembaubleblanks.3").getAsString().contains("%s"),
                "the apprentice-ring name formats the aspect name as an argument");
    }

    @Test
    void perMetaNamesPreserveTheOriginalWording() throws IOException {
        final Path original = Path.of(System.getProperty("tc.originalLangFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(original),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");

        final Map<String, String> legacy = new LinkedHashMap<>();
        for (final String line : Files.readAllLines(original, StandardCharsets.UTF_8)) {
            final String trimmed = line.replace("\r", "");
            final int equals = trimmed.indexOf('=');
            if (equals > 0 && !trimmed.startsWith("#")) {
                legacy.put(trimmed.substring(0, equals).trim(), normalize(trimmed.substring(equals + 1)));
            }
        }

        final JsonObject lang = lang();
        // TC-M6-08-s added the four golem families: their per-meta names are the
        // original item.ItemGolem<X>.<meta>.name strings (ItemGolemDecoration's
        // stack name is its own per-meta name; the original's "Accessory: "
        // prefix rides the tooltip instead).
        final Map<String, String> originalClass = Map.ofEntries(
                Map.entry("itemresource", "ItemResource"),
                Map.entry("itemshard", "ItemShard"),
                Map.entry("itemnugget", "ItemNugget"),
                Map.entry("itemlootbag", "ItemLootBag"),
                Map.entry("itembaubleblanks", "ItemBaubleBlanks"),
                Map.entry("itemamuletvis", "ItemAmuletVis"),
                Map.entry("itemamuletrunic", "ItemAmuletRunic"),
                Map.entry("itemringrunic", "ItemRingRunic"),
                Map.entry("itemgirdlerunic", "ItemGirdleRunic"),
                Map.entry("itemeldritchobject", "ItemEldritchObject"),
                Map.entry("itemgolemplacer", "ItemGolemPlacer"),
                Map.entry("itemgolemcore", "ItemGolemCore"),
                Map.entry("itemgolemupgrade", "ItemGolemUpgrade"),
                Map.entry("itemgolemdecoration", "ItemGolemDecoration"));

        int checked = 0;
        final List<String> mismatches = new java.util.ArrayList<>();
        for (final String key : lang.keySet()) {
            final String[] parts = key.split("\\.");
            if (parts.length != 4 || !parts[0].equals("item")
                    || !parts[3].matches("[0-9]+") || !originalClass.containsKey(parts[2])) {
                continue;
            }
            final String legacyKey = "item." + originalClass.get(parts[2]) + "." + parts[3] + ".name";
            final String expected = legacy.get(legacyKey);
            assertNotNull(expected, "the port ships " + key + " with no original " + legacyKey + " to preserve");
            if (!expected.equals(normalize(lang.get(key).getAsString()))) {
                mismatches.add(key + ": expected \"" + expected + "\" from " + legacyKey
                        + ", got \"" + normalize(lang.get(key).getAsString()) + "\"");
            }
            checked++;
        }
        assertEquals(List.of(), mismatches, "per-meta names must preserve the original en_US.lang wording");
        assertTrue(checked >= 55, "expected the nine multi-meta families to be checked, got " + checked);
    }


    /**
     * TC-M7-01b / t_f40bbc0d: every (block, meta) pair the port declares in
     * {@code BlockVariantNames} — the original's per-metadata
     * {@code tile.<id>.<meta>.name} set, sparse metas included — ships a
     * {@code block.thaumcraft.<id>.<meta>} entry, because
     * {@code VariantBlockItem.getName} emits that key for a variant stack.
     */
    @Test
    void everyMultiMetaBlockHasItsFullVariantSet() {
        final JsonObject lang = lang();
        final List<String> missing = new java.util.ArrayList<>();
        int checked = 0;
        for (final String path : com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.paths()) {
            for (final int meta : com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.metasOf(path)) {
                final String key = com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.key(path, meta);
                if (!lang.has(key) || !lang.get(key).isJsonPrimitive() || lang.get(key).getAsString().isEmpty()) {
                    missing.add(key + " (original " + com.jonnyutah12.thaumcraftclassic.blocks
                            .BlockVariantNames.legacyKey(path, meta) + ")");
                }
                checked++;
            }
        }
        assertEquals(List.of(), missing, "per-variant block names the port resolves are not shipped in en_us.json");
        assertTrue(checked >= 100, "expected the 13 mega-block families to be checked in full, got " + checked);
    }

    /**
     * The wording parity for those keys, in both directions, on a build that
     * has the reference tree: every declared pair preserves the original
     * string, every shipped per-variant block key is declared, and the
     * original's {@code tile.<id>.<meta>.name} set for the shipped families
     * has no name the port leaves unshipped.
     */
    @Test
    void perVariantBlockNamesPreserveTheOriginalWording() throws IOException {
        final Path original = Path.of(System.getProperty("tc.originalLangFile", ""));
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(original),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");

        final Map<String, String> legacy = new LinkedHashMap<>();
        for (final String line : Files.readAllLines(original, StandardCharsets.UTF_8)) {
            final String trimmed = line.replace("\r", "");
            final int equals = trimmed.indexOf('=');
            if (equals > 0 && !trimmed.startsWith("#")) {
                legacy.put(trimmed.substring(0, equals).trim(), normalize(trimmed.substring(equals + 1)));
            }
        }

        final Map<String, String> declaredByLegacy = new java.util.LinkedHashMap<>();
        for (final String path : com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.paths()) {
            for (final int meta : com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.metasOf(path)) {
                declaredByLegacy.put(com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames
                                .legacyKey(path, meta),
                        com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames.key(path, meta));
            }
        }
        // The original ids the port ships variants of, e.g. `blockAiry`.
        final java.util.Set<String> shippedLegacyIds = declaredByLegacy.keySet().stream()
                .map(key -> key.substring("tile.".length(), key.lastIndexOf('.')))
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

        final JsonObject lang = lang();
        final List<String> problems = new java.util.ArrayList<>();

        // 1. declared -> original string, verbatim after normalization.
        declaredByLegacy.forEach((legacyKey, portKey) -> {
            final String expected = legacy.get(legacyKey);
            if (expected == null) {
                problems.add(portKey + " is declared with no original " + legacyKey + " to preserve");
                return;
            }
            if (!lang.has(portKey)) {
                problems.add(portKey + " is not shipped; the original names it " + legacyKey + "=" + expected);
                return;
            }
            final String shipped = normalize(lang.get(portKey).getAsString());
            if (!expected.equals(shipped)) {
                problems.add(portKey + ": expected \"" + expected + "\" from " + legacyKey
                        + ", got \"" + shipped + "\"");
            }
        });

        // 2. original -> declared: the shipped families keep every original
        //    per-meta name, so a name the port drops shows up here.
        final java.util.regex.Pattern perMeta =
                java.util.regex.Pattern.compile("tile\\.([A-Za-z0-9_]+)\\..+\\.name");
        for (final String legacyKey : legacy.keySet()) {
            final java.util.regex.Matcher matcher = perMeta.matcher(legacyKey);
            if (!matcher.matches() || !shippedLegacyIds.contains(matcher.group(1))) {
                continue;
            }
            if (!declaredByLegacy.containsKey(legacyKey)) {
                problems.add("the original ships " + legacyKey + "=" + legacy.get(legacyKey)
                        + " for a mega-block the port names, but BlockVariantNames does not declare it");
            }
        }

        // 3. shipped -> declared: no stray per-variant block key.
        for (final String key : lang.keySet()) {
            if (key.startsWith("block.thaumcraft.") && key.matches(".*\\.\\d+$")
                    && !declaredByLegacy.containsValue(key)) {
                problems.add(key + " is a per-variant block key that BlockVariantNames does not declare");
            }
        }

        assertEquals(List.of(), problems,
                "per-variant block names must mirror the original en_US.lang set verbatim");
        assertTrue(declaredByLegacy.size() >= 100,
                "expected the mega-block families to declare their full name set, got "
                        + declaredByLegacy.size());
    }

    /** The normalization the port applies to original 1.7 lang strings. */
    private static String normalize(final String value) {
        return value.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "")
                .replace("%TYPE", "%s")
                .trim();
    }
}
