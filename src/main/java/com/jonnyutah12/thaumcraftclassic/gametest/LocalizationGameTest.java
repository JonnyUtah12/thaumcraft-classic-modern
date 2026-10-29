package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantBlockItem;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.items.ItemLootBag;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModRegistration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * Localization audit (TC-LOC-s / t_a2a74726): the shipped content must never
 * render as a raw translation key.
 *
 * <p>Two directions, both against the live registries of the gameTestServer
 * (the only place the real id set exists):</p>
 * <ol>
 *   <li>{@link #everyRegisteredNameHasAnEntry}: every registered block, item
 *       (including the per-meta name keys the multi-meta item classes emit
 *       from {@code getName(ItemStack)}), menu, entity type and creative tab
 *       under {@code thaumcraft} has an entry in
 *       {@code assets/thaumcraft/lang/en_us.json}.</li>
 *   <li>{@link #noDeadLangEntries}: the reverse direction — every key in the
 *       file maps to shipped content, i.e. to a live registration, to a
 *       translation key the shipped code passes to
 *       {@code Component.translatable}, or to a documented exception below.</li>
 *   <li>{@link #megaBlockVariantsCarryTheirOwnLabels} (TC-M7-01b): the
 *       multi-variant mega-block families resolve a per-variant key
 *       ({@code block.thaumcraft.<id>.<meta>}, the original's
 *       {@code tile.<id>.<meta>.name}) from the stack's {@code variant} value,
 *       so a variant never renders the family's meta-0 label.</li>
 * </ol>
 *
 * <p>Each run also writes {@code build/lang-audit.txt} (next to
 * {@code docs/content-mapping.csv}'s project root) with the required/missing/
 * dead key sets, so the gap list is inspectable without reading the failure
 * message.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LocalizationGameTest {

    /**
     * Registry ids that are test fixtures, not shipped content: registered
     * only when the run config sets {@code -Dthaumcraft.testprobe=true}, and
     * never in a release build, so they carry no lang entry by design.
     */
    private static final List<String> TEST_FIXTURE_ID_PREFIXES = List.of("test_probe", "test_aspect_source");

    /**
     * Lang keys the shipped code passes to {@code Component.translatable} that
     * are not derived from a registry id. Deliberate exceptions to the
     * no-dead-keys rule; the value provenance for each is in the TC-LOC-s
     * commit note (all preserve the original Thaumcraft 4.2.3.5 wording).
     */
    private static final Map<String, String> CODE_AUTHORED_KEYS = Map.ofEntries(
            // getDisplayName() of the four shipped container block entities —
            // the title the client shows for the open menu.
            Map.entry("container.thaumcraft.chesthungry", "TileChestHungry.getDisplayName"),
            Map.entry("container.thaumcraft.deconstructiontable", "TileDeconstructionTable.getDisplayName"),
            Map.entry("container.thaumcraft.magicbox", "TileMagicBox.getDisplayName"),
            Map.entry("container.thaumcraft.spa", "TileSpa.getDisplayName"),
            // tooltip / action lines
            Map.entry("item.thaumcraft.itemlootbag.tooltip", "ItemLootBag.appendHoverText"),
            Map.entry("item.thaumcraft.trunkspawner.tooltip", "ItemTrunkSpawner.appendHoverText"),
            Map.entry("item.thaumcraft.itemgolemupgrade.0", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.thaumcraft.itemgolemupgrade.1", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.thaumcraft.itemgolemupgrade.2", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.thaumcraft.itemgolemupgrade.3", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.thaumcraft.itemgolemupgrade.4", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.thaumcraft.itemgolemupgrade.5", "ItemTrunkSpawner tooltip (TC-M6-06 trunk upgrades)"),
            Map.entry("item.capacity.text", "ItemAmuletVis capacity line (original key shape kept)"),
            Map.entry("item.ItemAmuletVis.text", "ItemAmuletVis helper line (original key shape kept)"),
            Map.entry("tc.discount", "ItemBaubleBlanks apprentice-ring tooltip"),
            Map.entry("tc.resonator1", "ItemResonator action bar"),
            Map.entry("tc.resonator2", "ItemResonator action bar"),
            Map.entry("tc.resonator3", "ItemResonator untyped-aspect label"),
            Map.entry("tc.handmirrorlinkedto", "ItemHandMirror link message"),
            Map.entry("tc.handmirrorlinked", "ItemHandMirror link message"),
            Map.entry("tc.handmirrorerror", "ItemHandMirror link message"),
            Map.entry("tc.researcherror", "ResearchManager knowledge-gate message"),
            // Eldritch Objects lore (ItemEldritchObject.addInformation, TC4:81-92)
            // and the lock's boss-room chat lines (TileEldritchLock:136/220/300/340).
            Map.entry("item.ItemEldritchObject.text.1", "ItemEldritchObject lore, meta 0"),
            Map.entry("item.ItemEldritchObject.text.2", "ItemEldritchObject lore, meta 1"),
            Map.entry("item.ItemEldritchObject.text.3", "ItemEldritchObject lore, meta 1 second line"),
            Map.entry("item.ItemEldritchObject.text.4", "ItemEldritchObject lore, meta 2"),
            Map.entry("item.ItemEldritchObject.text.5", "ItemEldritchObject lore, meta 3"),
            Map.entry("item.ItemEldritchObject.text.6", "ItemEldritchObject lore, meta 3 second line"),
            Map.entry("tc.boss.crimson", "EldritchBossRoomHost announce (original TileEldritchLock:300)"),
            Map.entry("tc.boss.golem", "EldritchBossRoomHost announce (original TileEldritchLock:220)"),
            Map.entry("tc.boss.taint", "EldritchBossRoomHost announce (original TileEldritchLock:340)"),
            Map.entry("tc.boss.warden", "EldritchBossRoomHost announce (original TileEldritchLock:136)"));

    /**
     * Base name keys of the multi-meta items whose {@code getName} always
     * returns the per-meta key. They are not dead: vanilla reads
     * {@code item.<ns>.<path>} as the description id for tooltips built
     * outside {@code getName} (creative-tab tooltips, {@code /give} feedback),
     * so they stay in the file as the documented fallback.
     */
    private static final Set<String> META_DRIVEN_ITEMS = Set.of(
            "itemresource", "itemshard", "itemnugget", "itemlootbag", "itembaubleblanks",
            "itemamuletvis", "itemamuletrunic", "itemringrunic", "itemgirdlerunic",
            "itemeldritchobject");

    private LocalizationGameTest() {
    }

    // ------------------------------------------------------------------ probes

    @GameTest(template = "empty")
    public static void everyRegisteredNameHasAnEntry(final GameTestHelper helper) {
        final JsonObject lang = loadLang(helper);
        final Set<String> required = requiredKeys(helper);
        final Set<String> missing = new TreeSet<>();
        for (final String key : required) {
            if (!lang.has(key)) {
                missing.add(key);
            }
        }
        writeAudit(helper, required, missing, Set.of());

        ThaumcraftClassic.LOGGER.info("[TC-LOC-s] {} registry-derived keys required, {} missing",
                required.size(), missing.size());
        helper.assertTrue(missing.isEmpty(),
                "en_us.json is missing " + missing.size() + " of " + required.size()
                        + " registry-derived keys: " + preview(missing));
        helper.succeed();
    }

    /**
     * TC-M7-01b / t_f40bbc0d: the mega-block families must name themselves per
     * variant. The original rendered {@code tile.<id>.<meta>.name} for each
     * metadata value; 1.21.1 has no per-state block display name, so
     * {@link com.jonnyutah12.thaumcraftclassic.blocks.VariantBlockItem} reads
     * the stack's {@code variant} value and resolves the per-variant key from
     * {@link BlockVariantNames}. This probe drives the real
     * {@code Item.getName(ItemStack)} path — the one tooltips, {@code /give}
     * feedback and container names go through — for every declared (block,
     * meta) pair, in both variant carriers, and proves the label is the
     * variant's own and not the family's meta-0 label.
     */
    @GameTest(template = "empty")
    public static void megaBlockVariantsCarryTheirOwnLabels(final GameTestHelper helper) {
        final JsonObject lang = loadLang(helper);
        int checked = 0;
        int distinctFromBase = 0;
        for (final String path : BlockVariantNames.paths()) {
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            final Block block = BuiltInRegistries.BLOCK.get(id);
            helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR
                            && BuiltInRegistries.BLOCK.containsKey(id),
                    "block variant family " + id + " is declared in BlockVariantNames but not registered");
            final Item item = BuiltInRegistries.ITEM.get(id);
            helper.assertTrue(item instanceof BlockItem blockItem && blockItem.getBlock() == block,
                    "the same-id item of " + id + " must be a BlockItem of that block, got "
                            + BuiltInRegistries.ITEM.getKey(item));
            helper.assertTrue(item instanceof VariantBlockItem,
                    id + " carries named variants, so its item must be a VariantBlockItem (the per-variant "
                            + "names only resolve through that class)");

            final String baseKey = block.getDescriptionId();
            final String baseLabel = labelOf(lang, baseKey, id.getPath() + " base label");
            for (final int meta : BlockVariantNames.metasOf(path)) {
                final String expected = BlockVariantNames.key(path, meta);
                helper.assertTrue(lang.has(expected) && !lang.get(expected).getAsString().isEmpty(),
                        expected + " is declared for " + BlockVariantNames.legacyKey(path, meta)
                                + " but is not shipped in en_us.json");

                // The two carriers a variant stack ships with: the vanilla
                // block_state component (pick-block, drops, recipe results,
                // /give with components) and the ported ITEM_META component.
                final ItemStack stateStack = new ItemStack(item);
                stateStack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                        new BlockItemStateProperties(
                                java.util.Map.of(VariantStateItems.VARIANT_PROPERTY, Integer.toString(meta))));
                final ItemStack metaStack = new ItemStack(item);
                metaStack.set(ModDataComponents.ITEM_META.get(), meta);
                for (final ItemStack stack : java.util.List.of(stateStack, metaStack)) {
                    final String key = translatableKeyOf(item.getName(stack));
                    helper.assertTrue(expected.equals(key),
                            "meta " + meta + " of " + id + " names itself \"" + key + "\"; the original ships "
                                    + BlockVariantNames.legacyKey(path, meta) + ", ported as " + expected);
                }

                final String label = lang.get(expected).getAsString();
                if (!label.equals(baseLabel)) {
                    // The observed defect: every blockStoneDevice meta read
                    // "Alchemical Furnace" (the meta-0 label), so an Arcane
                    // Bore pedestal looked like a furnace. A variant the
                    // original named differently must not resolve to the base.
                    final String plainKey = translatableKeyOf(item.getName(stateStack));
                    helper.assertTrue(!baseKey.equals(plainKey),
                            "meta " + meta + " of " + id + " is \"" + label + "\" in the original but still "
                                    + "resolves to the family's base key " + baseKey);
                    distinctFromBase++;
                }
                checked++;
            }

            // A stack that carries no variant keeps the plain block key — the
            // documented fallback vanilla uses for component-less stacks.
            helper.assertTrue(baseKey.equals(translatableKeyOf(item.getName(new ItemStack(item)))),
                    id + " must keep the plain block key for a stack with no variant component");
        }
        ThaumcraftClassic.LOGGER.info("[TC-M7-01b] {} declared mega-block variants across {} families "
                        + "resolve their own labels ({} differ from the family's base label)",
                checked, BlockVariantNames.paths().size(), distinctFromBase);
        helper.assertTrue(checked >= 100,
                "expected the declared mega-block families to be probed in full, checked " + checked);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noDeadLangEntries(final GameTestHelper helper) {
        final JsonObject lang = loadLang(helper);
        final Set<String> required = requiredKeys(helper);
        final Set<String> dead = new TreeSet<>();
        for (final String key : lang.keySet()) {
            if (required.contains(key) || CODE_AUTHORED_KEYS.containsKey(key)) {
                continue;
            }
            final String path = key.substring(key.lastIndexOf('.') + 1);
            if (isTestFixture(path)) {
                continue;
            }
            // The base key of a multi-meta item is the documented vanilla
            // fallback (getName returns the per-meta key, which is in
            // `required`): keep the base key in the file.
            final String baseId = metaDrivenBaseId(key);
            if (key.equals("item.thaumcraft." + baseId) && META_DRIVEN_ITEMS.contains(baseId)) {
                continue;
            }
            dead.add(key);
        }
        writeAudit(helper, required, Set.of(), dead);

        helper.assertTrue(dead.isEmpty(),
                "en_us.json carries " + dead.size() + " keys that map to no shipped content "
                        + "(registration, code translatable, or documented exception): " + preview(dead));
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** Every translation key the live registries make reachable, plus the keys the code passes to translatable(). */
    private static Set<String> requiredKeys(final GameTestHelper helper) {
        final Set<String> keys = new LinkedHashSet<>();

        for (final Block block : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (eligible(id)) {
                keys.add(block.getDescriptionId());
            }
        }

        for (final Item item : BuiltInRegistries.ITEM) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!eligible(id)) {
                continue;
            }
            if (item instanceof BlockItem blockItem) {
                // 1.21.1 shows the owning block's key for a block item; the
                // same-id block item contract makes this the shipped label.
                keys.add(blockItem.getBlock().getDescriptionId());
            }
            for (final int meta : metasOf(item)) {
                final ItemStack stack = new ItemStack(item);
                stack.set(ModDataComponents.ITEM_META.get(), meta);
                final String key = translatableKeyOf(item.getName(stack));
                if (key != null) {
                    keys.add(key);
                }
            }
        }

        for (final var menu : BuiltInRegistries.MENU) {
            final ResourceLocation id = BuiltInRegistries.MENU.getKey(menu);
            if (eligible(id)) {
                keys.add("container.thaumcraft." + id.getPath());
            }
        }

        for (final EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (eligible(id)) {
                keys.add("entity.thaumcraft." + id.getPath());
            }
        }

        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            final ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (eligible(id)) {
                keys.add("itemGroup.thaumcraft." + id.getPath());
            }
        }

        keys.addAll(CODE_AUTHORED_KEYS.keySet());

        // TC-M6-04f-s: the fluid types (NeoForge's static registry) label
        // themselves through FluidType.Properties.descriptionId, and the
        // damage types (datapack registry) label their death message as
        // death.attack.<message_id> — both families are shipped content,
        // so the audit walks them like the vanilla static registries above.
        for (final net.neoforged.neoforge.fluids.FluidType fluidType
                : net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES) {
            final ResourceLocation id = net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES.getKey(fluidType);
            if (eligible(id)) {
                keys.add(fluidType.getDescriptionId());
            }
        }
        final net.minecraft.core.Registry<net.minecraft.world.damagesource.DamageType> damageTypes =
                helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (final ResourceLocation id : damageTypes.keySet()) {
            if (eligible(id)) {
                final String msgId = damageTypes.get(id).msgId();
                // DeathMessageTypes appends ".player" when the attacking
                // entity is a player and ".item" when a cause item is
                // present, so all three key shapes are reachable for every
                // shipped type (the originals ship taint[.player],
                // dissolve[.player] and swarm[.item] strings).
                keys.add("death.attack." + msgId);
                keys.add("death.attack." + msgId + ".player");
                keys.add("death.attack." + msgId + ".item");
            }
        }
        return keys;
    }

    /**
     * The original-metadata domain of a shipped item: the class's own
     * {@code META_COUNT} (contiguous metas) or {@code VALID_METAS} (sparse
     * damage values), walking up the class hierarchy the way the item classes
     * declare them. {@link ItemLootBag} keeps its rarity metas on the shared
     * ITEM_META component with no bound constant, so it is named explicitly.
     */
    private static int[] metasOf(final Item item) {
        if (item instanceof BlockItem blockItem) {
            // A multi-variant block item names every state its block can be
            // in (TC-M7-01b): the original-metadata domain is the block's own
            // `variant` property range, not a class constant.
            final net.minecraft.world.level.block.state.properties.Property<?> variant =
                    blockItem.getBlock().getStateDefinition()
                            .getProperty(VariantStateItems.VARIANT_PROPERTY);
            if (variant instanceof IntegerProperty range) {
                return range.getPossibleValues().stream().mapToInt(Integer::intValue).sorted().toArray();
            }
        }
        for (Class<?> c = item.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            final Integer count = staticInt(c, "META_COUNT");
            if (count != null) {
                return IntStream.range(0, count).toArray();
            }
            final Set<?> valid = staticSet(c, "VALID_METAS");
            if (valid != null) {
                return valid.stream().mapToInt(v -> (Integer) v).toArray();
            }
        }
        if (item instanceof ItemLootBag) {
            return new int[] {0, 1, 2};
        }
        return new int[] {0};
    }

    private static Integer staticInt(final Class<?> type, final String name) {
        try {
            final Field field = type.getDeclaredField(name);
            return java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? field.getInt(null) : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Set<?> staticSet(final Class<?> type, final String name) {
        try {
            final Field field = type.getDeclaredField(name);
            return java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && field.get(null) instanceof Set<?> set ? set : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** The shipped string of a key, with the audit failing loudly when it is absent. */
    private static String labelOf(final JsonObject lang, final String key, final String what) {
        final var entry = lang.get(key);
        if (entry == null || !entry.isJsonPrimitive() || entry.getAsString().isEmpty()) {
            throw new AssertionError(what + " has no en_us.json entry: " + key);
        }
        return entry.getAsString();
    }

    /** The key of a pure translatable component, or null when the name is not a lang key. */
    private static String translatableKeyOf(final Component component) {
        return component.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
    }

    /** {@code item.thaumcraft.itemresource.5} -> {@code itemresource}. */
    private static String metaDrivenBaseId(final String key) {
        final String[] parts = key.split("\\.");
        return parts.length >= 3 ? parts[2] : key;
    }

    private static boolean eligible(final ResourceLocation id) {
        return ThaumcraftClassic.MOD_ID.equals(id.getNamespace()) && !isTestFixture(id.getPath());
    }

    private static boolean isTestFixture(final String path) {
        return TEST_FIXTURE_ID_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static String preview(final Set<String> keys) {
        final List<String> list = new ArrayList<>(keys);
        final int shown = Math.min(list.size(), 40);
        return list.subList(0, shown) + (list.size() > shown ? " …(+" + (list.size() - shown) + " more)" : "");
    }

    /** The lang file as shipped in the mod's own resources. */
    private static JsonObject loadLang(final GameTestHelper helper) {
        String text = null;
        try (InputStream in = LocalizationGameTest.class.getResourceAsStream("/assets/"
                + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            if (in != null) {
                text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (text == null) {
            final String csv = System.getProperty("tc.contentMappingFile", "");
            helper.assertTrue(!csv.isEmpty(), "tc.contentMappingFile must be set by the gameTestServer run config");
            final Path file = Path.of(csv).toAbsolutePath().getParent().getParent()
                    .resolve("src/main/resources/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json");
            helper.assertTrue(Files.isRegularFile(file), "en_us.json not found on the classpath or at " + file);
            try {
                text = Files.readString(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        final var parsed = JsonParser.parseString(text);
        helper.assertTrue(parsed.isJsonObject(), "en_us.json must be a JSON object");
        return parsed.getAsJsonObject();
    }

    /** Diagnostic artifact: build/lang-audit.txt with the full key sets. */
    private static void writeAudit(final GameTestHelper helper, final Set<String> required,
                                   final Set<String> missing, final Set<String> dead) {
        final String csv = System.getProperty("tc.contentMappingFile", "");
        if (csv.isEmpty()) {
            return;
        }
        final Path audit = Path.of(csv).toAbsolutePath().getParent().getParent().resolve("build/lang-audit.txt");
        try {
            Files.createDirectories(audit.getParent());
            final StringBuilder sb = new StringBuilder();
            sb.append("# TC-LOC-s lang audit (generated by LocalizationGameTest; not a build input)\n");
            sb.append("# required=").append(required.size())
                    .append(" missing=").append(missing.size())
                    .append(" dead=").append(dead.size())
                    .append(" probeEnabled=").append(Boolean.getBoolean(ModRegistration.TEST_PROBE_PROPERTY))
                    .append('\n');
            sb.append("\n[required]\n");
            required.forEach(k -> sb.append(k).append('\n'));
            sb.append("\n[missing]\n");
            missing.forEach(k -> sb.append(k).append('\n'));
            sb.append("\n[dead]\n");
            dead.forEach(k -> sb.append(k).append('\n'));
            Files.writeString(audit, sb.toString());
        } catch (IOException e) {
            ThaumcraftClassic.LOGGER.warn("[TC-LOC-s] could not write the lang audit file", e);
        }
    }

}
