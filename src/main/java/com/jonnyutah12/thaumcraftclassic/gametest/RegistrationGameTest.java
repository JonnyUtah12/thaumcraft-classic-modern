package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.ModRegistration;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registration + id-stability harness (TC-M2-01). Three guarantees:
 *
 * <ol>
 *   <li>{@link #registriesInitialize}: every holder's target registry exists
 *       at runtime and the probe block/item are registered through the real
 *       ModBlocks/ModItems holders under their exact contract ids — proof the
 *       constructor-time wiring in {@link ModRegistration} runs before any
 *       RegisterEvent (no "DeferredRegister registered too late").</li>
 *   <li>{@link #probeRoundTripsThroughSaveData}: the probe block state and
 *       item stack survive NBT save round-trips, i.e. their ids resolve
 *       through the save-data path (block state "Name", item stack "id").</li>
 *   <li>{@link #implementedContentIdsResolve}: the forward harness over
 *       docs/content-mapping.csv — every row whose implementation_status is
 *       {@code done} must resolve to a live registered object in its target
 *       registry. Rows stay {@code not_started} until their content milestone
 *       lands; the same test then enforces resolution forever (id stability).</li>
 * </ol>
 *
 * <p>Requires the {@code gameTestServer} run configuration (build.gradle),
 * which sets {@code -Dthaumcraft.testprobe=true} and
 * {@code -Dtc.contentMappingFile=<abs path to the CSV>}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistrationGameTest {

    /** kind -> vanilla/builtin registry the id must resolve in. */
    private static final Map<String, ResourceKey<? extends Registry<?>>> BUILTIN_KINDS = Map.of(
            "block", Registries.BLOCK,
            "item", Registries.ITEM,
            "spawner_egg", Registries.ITEM,
            "entity", Registries.ENTITY_TYPE,
            "fluid", Registries.FLUID,
            "potion", Registries.MOB_EFFECT);

    /** kind -> datapack (dynamic) registry, resolved via the level's lookup provider. */
    private static final Map<String, ResourceKey<? extends Registry<?>>> DATAPACK_KINDS = Map.of(
            "enchantment", Registries.ENCHANTMENT,
            "biome", Registries.BIOME);

    /**
     * kind -> modded registry created by the owning milestone via
     * NewRegistryEvent; registry keys are pinned here and in
     * docs/architecture/registration.md so the contract is unambiguous.
     */
    private static final Map<String, ResourceKey<? extends Registry<?>>> CUSTOM_KINDS = Map.of(
            "aspect", registryKey("aspect"),
            "research_category", registryKey("research_category"),
            "research", registryKey("research"),
            "wand_component", registryKey("wand_component"),
            "node_type", registryKey("node_type"),
            "node_modifier", registryKey("node_modifier"));

    /**
     * kind -> inventory-only rows: not standalone registered objects in 1.21.1
     * (config keys, GUI ids, oredict→tag names, spawn rules, dimension ids,
     * worldgen features, wand triggers, tile entities keyed by their block,
     * recipe instances). Documented in docs/architecture/registration.md.
     */
    private static final List<String> REFERENCE_KINDS = List.of(
            "config_option", "gui", "oredict", "mob_spawn", "dimension",
            "worldgen_feature", "wand_trigger", "tile_entity", "recipe");

    private RegistrationGameTest() {
    }

    private static ResourceKey<Registry<Object>> registryKey(final String path) {
        return ResourceKey.createRegistryKey(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    @GameTest(template = "empty")
    public static void registriesInitialize(final GameTestHelper helper) {
        helper.assertTrue(Boolean.getBoolean(ModRegistration.TEST_PROBE_PROPERTY),
                "gameTestServer run config must set -D" + ModRegistration.TEST_PROBE_PROPERTY + "=true");

        for (final ResourceKey<? extends Registry<?>> key : List.of(
                Registries.DATA_COMPONENT_TYPE, Registries.BLOCK, Registries.ITEM,
                Registries.BLOCK_ENTITY_TYPE, Registries.ENTITY_TYPE, Registries.MOB_EFFECT,
                Registries.PARTICLE_TYPE, Registries.CREATIVE_MODE_TAB)) {
            helper.assertTrue(BuiltInRegistries.REGISTRY.containsKey(key.location()),
                    "registry " + key.location() + " is not available at runtime");
        }

        final ResourceLocation probeBlockId =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "test_probe_block");
        final ResourceLocation probeItemId =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "test_probe_item");
        final Block probeBlock = TestProbeContent.PROBE_BLOCK.get();
        final var probeItem = TestProbeContent.PROBE_ITEM.get();

        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(probeBlockId)
                        && BuiltInRegistries.BLOCK.get(probeBlockId) == probeBlock,
                "probe block is not registered under its contract id " + probeBlockId);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(probeItemId)
                        && BuiltInRegistries.ITEM.get(probeItemId) == probeItem,
                "probe item is not registered under its contract id " + probeItemId);

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void probeRoundTripsThroughSaveData(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        // Block state: NBT round-trip (the save-data path stores the registry id).
        final BlockState state = TestProbeContent.PROBE_BLOCK.get().defaultBlockState();
        final CompoundTag blockNbt = NbtUtils.writeBlockState(state);
        helper.assertTrue("thaumcraft:test_probe_block".equals(blockNbt.getString("Name")),
                "block state NBT does not carry the contract id, got: " + blockNbt.getString("Name"));
        final BlockState restored =
                NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), blockNbt);
        helper.assertTrue(restored == state, "block state did not survive the NBT round-trip");

        // In-world placement read-back through the level.
        helper.setBlock(1, 1, 1, state);
        helper.assertBlockPresent(TestProbeContent.PROBE_BLOCK.get(), 1, 1, 1);

        // Item stack: save() -> parseOptional() round-trip (the player-save path).
        final ItemStack stack = new ItemStack(TestProbeContent.PROBE_ITEM.get(), 3);
        final CompoundTag itemNbt = (CompoundTag) stack.save(registries);
        helper.assertTrue("thaumcraft:test_probe_item".equals(itemNbt.getString("id")),
                "item stack NBT does not carry the contract id, got: " + itemNbt.getString("id"));
        final ItemStack back = ItemStack.parseOptional(registries, itemNbt);
        helper.assertTrue(!back.isEmpty() && back.is(stack.getItem()) && back.getCount() == 3,
                "item stack did not survive the save round-trip");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void implementedContentIdsResolve(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");

        final List<ContentMappingCsv.Row> rows;
        try {
            rows = ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        int resolved = 0;
        int referenceOnly = 0;
        int pending = 0;
        for (final ContentMappingCsv.Row row : rows) {
            if (!"done".equals(row.implementationStatus())) {
                pending++;
                continue;
            }
            if (REFERENCE_KINDS.contains(row.kind())) {
                referenceOnly++;
                continue;
            }
            final ResourceLocation id = ResourceLocation.parse(row.newId());
            final String what = row.kind() + " " + row.newId();
            if (BUILTIN_KINDS.containsKey(row.kind())) {
                final Registry<?> registry = BuiltInRegistries.REGISTRY.get(BUILTIN_KINDS.get(row.kind()).location());
                helper.assertTrue(registry != null && registry.containsKey(id),
                        what + " is marked done but is not registered");
                resolved++;
            } else if (CUSTOM_KINDS.containsKey(row.kind())) {
                final Registry<?> registry = BuiltInRegistries.REGISTRY.get(CUSTOM_KINDS.get(row.kind()).location());
                helper.assertTrue(registry != null,
                        what + " is marked done but its modded registry " + CUSTOM_KINDS.get(row.kind()).location()
                                + " does not exist (create it via NewRegistryEvent, see docs/architecture/registration.md)");
                helper.assertTrue(registry.containsKey(id), what + " is marked done but is not registered");
                resolved++;
            } else if (DATAPACK_KINDS.containsKey(row.kind())) {
                final ResourceKey<? extends Registry<?>> dataKey = DATAPACK_KINDS.get(row.kind());
                boolean found = false;
                @SuppressWarnings({"unchecked", "rawtypes"})
                final Optional<? extends HolderLookup.RegistryLookup<?>> lookup =
                        registries.lookup((ResourceKey) dataKey);
                if (lookup.isPresent()) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final net.minecraft.core.HolderGetter rawGetter = lookup.get();
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final ResourceKey entryKey = ResourceKey.create((ResourceKey) dataKey, id);
                    found = rawGetter.get(entryKey).isPresent();
                }
                helper.assertTrue(found,
                        what + " is marked done but is not present in the datapack registry (provide its JSON data file)");
                resolved++;
            } else {
                helper.assertTrue(false,
                        what + " is marked done but has no harness mapping — add it to RegistrationGameTest or REFERENCE_KINDS");
            }
        }

        ThaumcraftClassic.LOGGER.info(
                "[TC-M2-01] id-stability harness: {} rows total, {} resolved to live registrations, "
                        + "{} reference-only kinds, {} pending content",
                rows.size(), resolved, referenceOnly, pending);
        helper.succeed();
    }
}
