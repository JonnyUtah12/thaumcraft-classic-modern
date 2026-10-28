package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaHandler;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionPillar;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.tiles.InfusionMatrixLogic;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * TC-M4-04-s acceptance: the infusion altar core — the runic matrix and its
 * pillars registered on the shared stone device block, the altar shape and the
 * rune scan, the instability math, and one craft driven to completion
 * (essentia drained through {@link EssentiaHandler} from a test aspect source,
 * component items claimed from the pedestals the scan found, and the result
 * written back onto the central pedestal).
 *
 * <ul>
 *   <li>{@link #altarCoreResolvesOnItsHostBlock}: the two tile-entity ids this
 *       card owns ({@code tileinfusionstone} — the original registers the
 *       {@code TileInfusionMatrix} class under that string, D-001 — and
 *       {@code tileinfusionpillar}) resolve through the (block, variant)
 *       bindings this card registers on blockStoneDevice (metas 1-4), and the
 *       recipe type + serializer pair the data files under
 *       {@code data/thaumcraft/recipe/} name is in both recipe registries.</li>
 *   <li>{@link #altarShapeNeedsEveryPillar}: {@code validLocation} (original
 *       TileInfusionMatrix.java:229) accepts only the complete altar — the
 *       arcane pedestal two rows below the matrix plus a pillar at each corner
 *       of that pedestal's layer — and rejects the three shapes that are one
 *       part short.</li>
 *   <li>{@link #runeScanCountsPedestalsAndStabilisers}: the original
 *       {@code getSurroundings} symmetry rule (+2 per pedestal, +1 when it
 *       holds an item, −2 for the point-mirrored pedestal) and the snow-layer
 *       stabiliser rule (+0.1 each, −0.2 for a mirrored pair, truncated into
 *       the integer at the end) produce exactly the expected values against a
 *       real world, including twelve lone snow layers pushing 6 up to 7.</li>
 *   <li>{@link #infusionCraftRunsToCompletion}: a player use activates and
 *       then starts a craft; the matched recipe's essentia cost is drained
 *       pedestal-first from the probe source, its component items are consumed
 *       over the original's five-cycle animation, and the run ends with the
 *       result on the central pedestal, the pedestals empty and
 *       {@code crafting}/{@code instability} cleared.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfusionAltarGameTest {

    private InfusionAltarGameTest() {
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    // --- helpers ---------------------------------------------------------------

    private static final BlockStoneDevice STONE =
            (BlockStoneDevice) BuiltInRegistries.BLOCK.get(id("blockstonedevice"));

    private static final BlockPos ALTAR = new BlockPos(1, 4, 1);

    private static final List<BlockPos> ALTAR_CORNERS = List.of(
            new BlockPos(0, 2, 0), new BlockPos(2, 2, 0), new BlockPos(0, 2, 2),
            new BlockPos(2, 2, 2));

    /** Matrix at (1,4,1): pedestal two below plus all four corner pillars. */
    private static TileInfusionMatrix placeAltar(final GameTestHelper helper) {
        helper.setBlock(ALTAR, STONE.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX));
        helper.setBlock(ALTAR.below(2), STONE.withVariant(1));
        for (final BlockPos corner : ALTAR_CORNERS) {
            helper.setBlock(corner, STONE.withVariant(BlockStoneDevice.VARIANT_PILLAR_A));
        }
        final BlockEntity entity = helper.getBlockEntity(ALTAR);
        helper.assertTrue(entity instanceof TileInfusionMatrix,
                "the runic matrix block must create its matrix entity");
        return (TileInfusionMatrix) entity;
    }

    /** An arcane pedestal (blockStoneDevice meta 1) plus the item it carries. */
    private static TilePedestal pedestalWith(final GameTestHelper helper,
                                            final BlockPos at, final ItemStack item) {
        helper.setBlock(at, STONE.withVariant(1));
        final BlockEntity entity = helper.getBlockEntity(at);
        helper.assertTrue(entity instanceof TilePedestal,
                "pedestal host at " + at + " has no pedestal entity");
        final TilePedestal pedestal = (TilePedestal) entity;
        pedestal.setItem(0, item);
        return pedestal;
    }

    private static TilePedestal centreOf(final GameTestHelper helper, final BlockPos centre) {
        final BlockEntity centreBe = helper.getBlockEntity(centre);
        helper.assertTrue(centreBe instanceof TilePedestal,
                "the altar centre two rows below the matrix must be a pedestal");
        return (TilePedestal) centreBe;
    }

    private static final Aspect aspect(final String tag) {
        final Aspect found = Aspect.getAspect(tag);
        assert found != null : "unknown aspect tag " + tag;
        return found;
    }

    /** The test-only essentia jar: a probe aspect source seeded for the craft. */
    private static TestProbeContent.ProbeAspectSource seedAspectSource(
            final GameTestHelper helper, final BlockPos at, final String tag, final int amount) {
        helper.setBlock(at, TestProbeContent.PROBE_ASPECT_SOURCE_BLOCK.get());
        final BlockEntity entity = helper.getBlockEntity(at);
        helper.assertTrue(entity instanceof TestProbeContent.ProbeAspectSource,
                "the aspect-source probe has no block entity");
        final TestProbeContent.ProbeAspectSource source =
                (TestProbeContent.ProbeAspectSource) entity;
        source.seed(aspect(tag), amount);
        return source;
    }

    private static void snowLayer(final GameTestHelper helper, final BlockPos at) {
        // The original's stabiliser is a snow layer on the ground: 1.21.1 marks
        // snow layers as REQUIRES_SUPPORT, so a floating layer is dropped when
        // its column is re-checked. The host block keeps the cell stable.
        helper.setBlock(at.below(1), Blocks.STONE);
        helper.setBlock(at, Blocks.SNOW);
        // GameTestHelper.getBlockState applies the test-area offset; helper.getLevel()
        // reads raw world coordinates, which is a different cell (the same reason
        // every other test in this package uses helper.getBlockEntity, e.g.
        // gametest/CatalogDevicesGameTest.java:176).
        helper.assertTrue(helper.getBlockState(at).is(Blocks.SNOW),
                "the snow layer at " + at + " must be the original's stabiliser block");
    }

    /** The matrix's own serverTick, driven directly (as CatalogDevicesGameTest does). */
    private static void tickMatrix(final GameTestHelper helper, final BlockPos at,
                                  final TileInfusionMatrix matrix, final int times) {
        final BlockState state = helper.getBlockState(at);
        final BlockEntityTicker<TileInfusionMatrix> ticker =
                STONE.getTicker(helper.getLevel(), state,
                        CatalogInfusion.TILE_INFUSION_MATRIX.get());
        helper.assertTrue(ticker != null, "the runic matrix has no world ticker");
        for (int tick = 0; tick < times; tick++) {
            ticker.tick(helper.getLevel(), at, state, matrix);
        }
    }

    /**
     * A ServerPlayer with a dummy net handler — same shape as
     * AspectSystemGameTest.java:252 / ResearchProgressionGameTest: the
     * craft path reports through packets, so the player needs a connection
     * whose ad-hoc channels declare the payloads that path can send.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(
                        java.util.UUID.randomUUID(), "tc-infusion-player"), false);
        final ServerPlayer player =
                new ServerPlayer(server, (ServerLevel) helper.getLevel(),
                        cookie.gameProfile(), cookie.clientInformation());

        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(SyncAttachmentsPayload.TYPE.id());
        adHoc.add(AspectDiscoveryPayload.TYPE.id());
        adHoc.add(AspectPoolPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }

    // --- probes ----------------------------------------------------------------

    @GameTest(template = "empty")
    public static void altarCoreResolvesOnItsHostBlock(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id("blockstonedevice")),
                "the stone device host block is not registered");
        for (final String path : List.of("tileinfusionstone", "tileinfusionpillar")) {
            helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id(path)),
                    "tile entity " + path + " is not registered");
        }
        for (final String path : List.of("infusion", "infusion_enchantment")) {
            helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(id(path)),
                    "recipe type " + path + " is not registered");
            helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id(path)),
                    "recipe serializer " + path + " is not registered");
        }

        // The (block, variant) -> tile bindings: meta 1 is the arcane pedestal
        // the altar reads, 2 the matrix, 3/4 the two pillar orientations.
        final TileInfusionMatrix matrix = placeAltar(helper);
        helper.assertTrue(helper.getBlockEntity(new BlockPos(1, 2, 1)) instanceof TilePedestal,
                "blockStoneDevice meta 1 must resolve to the arcane pedestal");
        helper.assertTrue(matrix.getType() == CatalogInfusion.TILE_INFUSION_MATRIX.get(),
                "the matrix host created the wrong tile entity");
        for (final BlockPos corner : ALTAR_CORNERS) {
            helper.assertTrue(helper.getBlockEntity(corner) instanceof TileInfusionPillar,
                    "corner " + corner + " has no infusion pillar entity");
        }
        // The second pillar orientation is a distinct binding of the same tile.
        final BlockPos other = new BlockPos(6, 4, 6);
        helper.setBlock(other, STONE.withVariant(BlockStoneDevice.VARIANT_PILLAR_B));
        helper.assertTrue(helper.getBlockEntity(other) instanceof TileInfusionPillar,
                "blockStoneDevice meta 4 must resolve to the infusion pillar");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void altarShapeNeedsEveryPillar(final GameTestHelper helper) {
        final TileInfusionMatrix complete = placeAltar(helper);
        helper.assertTrue(complete.validLocation(),
                "the complete altar (pedestal two below plus four corner pillars) "
                        + "must be a valid runic matrix location");

        // One pillar short: the original's validLocation needs all four corners.
        final BlockPos at = new BlockPos(10, 4, 1);
        helper.setBlock(at, STONE.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX));
        helper.setBlock(at.below(2), STONE.withVariant(1));
        for (final BlockPos corner : List.of(
                new BlockPos(9, 2, 0), new BlockPos(11, 2, 0), new BlockPos(9, 2, 2))) {
            helper.setBlock(corner, STONE.withVariant(BlockStoneDevice.VARIANT_PILLAR_A));
        }
        final BlockEntity missing = helper.getBlockEntity(at);
        helper.assertTrue(missing instanceof TileInfusionMatrix,
                "the incomplete matrix host has no matrix entity");
        helper.assertTrue(!((TileInfusionMatrix) missing).validLocation(),
                "an altar missing one corner pillar must not be a valid location");

        // Wrong vertical spacing: the pedestal must be exactly two rows below.
        final BlockPos offBy = new BlockPos(1, 4, 10);
        helper.setBlock(offBy, STONE.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX));
        helper.setBlock(offBy.below(1), STONE.withVariant(1));
        for (final BlockPos corner : List.of(
                new BlockPos(0, 3, 9), new BlockPos(2, 3, 9), new BlockPos(0, 3, 11),
                new BlockPos(2, 3, 11))) {
            helper.setBlock(corner, STONE.withVariant(BlockStoneDevice.VARIANT_PILLAR_A));
        }
        final BlockEntity spaced = helper.getBlockEntity(offBy);
        helper.assertTrue(spaced instanceof TileInfusionMatrix,
                "the spaced matrix host has no matrix entity");
        helper.assertTrue(!((TileInfusionMatrix) spaced).validLocation(),
                "a pedestal one row below the matrix must not be a valid location");

        // No pedestal at all under a otherwise complete pillar square.
        final BlockPos bare = new BlockPos(10, 4, 10);
        helper.setBlock(bare, STONE.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX));
        for (final BlockPos corner : List.of(
                new BlockPos(9, 2, 9), new BlockPos(11, 2, 9), new BlockPos(9, 2, 11),
                new BlockPos(11, 2, 11))) {
            helper.setBlock(corner, STONE.withVariant(BlockStoneDevice.VARIANT_PILLAR_A));
        }
        final BlockEntity noCentre = helper.getBlockEntity(bare);
        helper.assertTrue(noCentre instanceof TileInfusionMatrix,
                "the centreless matrix host has no matrix entity");
        helper.assertTrue(!((TileInfusionMatrix) noCentre).validLocation(),
                "a matrix with no arcane pedestal below must not be a valid location");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void runeScanCountsPedestalsAndStabilisers(
            final GameTestHelper helper) {
        final TileInfusionMatrix matrix = placeAltar(helper);
        final ItemStack ingot = new ItemStack(Items.GOLD_INGOT);
        final ItemStack pane = new ItemStack(net.minecraft.world.item.Items.GLASS_PANE);

        // Two component pedestals, neither with a point-mirrored twin: +3 each
        // (+2 for the pedestal, +1 because it holds an item).
        pedestalWith(helper, new BlockPos(5, 2, 1), ingot);
        pedestalWith(helper, new BlockPos(1, 2, 6), pane);

        matrix.checkSurroundings = true;
        tickMatrix(helper, ALTAR, matrix, 1);
        helper.assertTrue(matrix.symmetry == 6,
                "two unmirrored pedestals holding items must score +3 each, got "
                        + matrix.symmetry);

        // Twelve lone snow layers: the original adds +0.1 per stabiliser and
        // truncates the float total, so 6.2 reads back as 7.
        for (final int x : List.of(5, 6, 7)) {
            for (final int z : List.of(3, 4, 5)) {
                snowLayer(helper, new BlockPos(x, 2, z));
            }
            snowLayer(helper, new BlockPos(x, 2, 7));
        }
        matrix.checkSurroundings = true;
        tickMatrix(helper, ALTAR, matrix, 1);
        helper.assertTrue(matrix.symmetry == 7,
                "twelve lone snow layers must add 1.2 to the symmetry total, got "
                        + matrix.symmetry);

        helper.succeed();
    }

    /**
     * The 87 extracted rows must be either loaded or legitimately skipped:
     * a row whose items all exist has no unmet gate and therefore MUST be in
     * the live RecipeManager. A row that fails to parse is dropped with a
     * 'Parsing error loading recipe' ERROR (the shape the first cut of
     * scripts/extract_infusion_recipes.py produced, and the reason every row
     * naming an unported item now carries a neoforge:item_exists condition).
     * The check is computed from the shipped JSON, so it needs no pinned
     * counts: it stays valid as the unported items land.
     */
    private static void assertEveryLoadableRowLoaded(final GameTestHelper helper) {
        final var resources = helper.getLevel().getServer().getResourceManager();
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        final java.util.Set<String> loaded = new java.util.HashSet<>();
        for (final RecipeHolder<InfusionCraftingRecipe> row
                : manager.getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get())) {
            loaded.add(row.value().originalKey());
        }
        for (final RecipeHolder<InfusionEnchantmentRecipe> row
                : manager.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            loaded.add(row.value().originalKey());
        }

        int checked = 0;
        for (final var entry : resources.listResources("recipe", path ->
                path.getNamespace().equals(ThaumcraftClassic.MOD_ID)
                        && path.getPath().contains(".infusion")).entrySet()) {
            final com.google.gson.JsonObject json;
            try (var reader = entry.getValue().openAsReader()) {
                json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            } catch (final java.io.IOException failure) {
                helper.fail("unreadable recipe resource " + entry.getKey() + ": " + failure);
                return;
            }
            final java.util.Set<String> items = new java.util.HashSet<>();
            for (final String key : List.of("input", "components", "output")) {
                if (json.has(key)) {
                    collectItemIds(json.get(key), items);
                }
            }
            boolean gated = false;
            for (final var condition : json.getAsJsonArray("neoforge:conditions")) {
                final var entryJson = condition.getAsJsonObject();
                if (!"neoforge:item_exists".equals(entryJson.get("type").getAsString())) {
                    gated = true; // a Config gate this test cannot evaluate
                    continue;
                }
                if (!BuiltInRegistries.ITEM.containsKey(
                        ResourceLocation.parse(entryJson.get("item").getAsString()))) {
                    gated = true; // the row is legitimately skipped at load
                }
            }
            if (!gated && json.has("enchantment")
                    && !helper.getLevel().registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .containsKey(ResourceLocation.parse(json.get("enchantment").getAsString()))) {
                // An unported enchantment is NOT a gate: the recipe ships as
                // data and must still be loaded (it simply never matches).
                helper.assertTrue(loaded.contains(json.get("original_key").getAsString()),
                        "the row " + entry.getKey() + " names an unported enchantment, so it "
                                + "must load and never match, but the RecipeManager dropped it");
            }
            if (gated) {
                continue;
            }
            final String key = json.get("original_key").getAsString();
            helper.assertTrue(loaded.contains(key),
                    "the data row " + entry.getKey() + " (" + key + ") has every item it "
                            + "names, so it must be loaded, but the RecipeManager has no "
                            + "such infusion recipe");
            checked++;
        }
        ThaumcraftClassic.LOGGER.info(
                "[TC-M4-04] infusion data: {} rows resolve into the live RecipeManager "
                        + "({} crafting + {} enchantment); {} of 87 rows carry an unmet "
                        + "neoforge:item_exists / config gate",
                loaded.size(),
                manager.getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get()).size(),
                manager.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get()).size(),
                87 - loaded.size());
        helper.assertTrue(checked >= 30,
                "only " + checked + " infusion rows were fully resolvable — the data set "
                        + "looks truncated");
    }

    /** Every item id an ingredient/output object names. */
    private static void collectItemIds(final com.google.gson.JsonElement node,
                                      final java.util.Set<String> items) {
        if (node == null || node.isJsonNull()) {
            return;
        }
        if (node.isJsonArray()) {
            for (final var element : node.getAsJsonArray()) {
                collectItemIds(element, items);
            }
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        final com.google.gson.JsonObject object = node.getAsJsonObject();
        if (object.has("item")) {
            items.add(object.get("item").getAsString());
        }
        if (object.has("id")) {
            items.add(object.get("id").getAsString());
        }
        if (object.has("items")) {
            final var value = object.get("items");
            if (value.isJsonArray()) {
                for (final var element : value.getAsJsonArray()) {
                    items.add(element.getAsString());
                }
            } else {
                items.add(value.getAsString());
            }
        }
        for (final String key : List.of("input", "components", "output")) {
            if (object.has(key)) {
                collectItemIds(object.get(key), items);
            }
        }
    }

    @GameTest(template = "empty")
    public static void infusionCraftRunsToCompletion(final GameTestHelper helper) {
        final TileInfusionMatrix matrix = placeAltar(helper);
        final ServerPlayer player = freshPlayer(helper);

        // Mirror of data row recipe.105's shape (central item + one component
        // per pedestal); built in code because the extracted rows use the
        // neoforge:components ingredient form, which needs the component-ingredient
        // IngredientType card, and their items belong to cards that have not
        // registered them yet.
        final AspectList cost = new AspectList();
        cost.add(aspect("potentia"), 2);
        final InfusionCraftingRecipe recipe = new InfusionCraftingRecipe(
                "InfusionAltarProbe", "", -8, cost,
                Ingredient.of(Items.GOLD_INGOT),
                List.of(Ingredient.of(net.minecraft.world.item.Items.GLASS_PANE)),
                new ItemStack(Items.GOLD_BLOCK), null, null);

        // The matrix matches recipes through the level's RecipeManager (the
        // original's ThaumcraftCraftingManager), so the probe's recipe has to
        // be in it. The data rows are already there — this one is built here
        // because the extracted rows use the neoforge:components ingredient
        // form, which needs the component-ingredient IngredientType card.
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        final java.util.List<RecipeHolder<?>> loaded =
                new java.util.ArrayList<>(manager.getOrderedRecipes());
        loaded.add(new RecipeHolder(id("infusionaltarprobe"), recipe));
        manager.replaceRecipes(loaded);

        // The extracted data rows reach the same manager: recipe.80.infusion is
        // the one row whose ingredients are all registered today.
        final java.util.List<RecipeHolder<InfusionCraftingRecipe>> dataRows =
                manager.getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get());
        helper.assertFalse(dataRows.isEmpty(),
                "no thaumcraft:infusion recipe is loaded");
        // The two rows naming the Thaumcraft enchantments (TC-M6-05) must LOAD
        // and resolve to no holder: a row for unported content ships as data and
        // never matches, instead of failing the data pack with a parse error the
        // dedicated-server smoke gate counts as mod-attributable.
        int unregistered = 0;
        for (final RecipeHolder<InfusionEnchantmentRecipe> row
                : manager.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            if (row.value().enchantmentKey().location().getNamespace()
                    .equals(ThaumcraftClassic.MOD_ID)
                    && row.value().enchantment(helper.getLevel().registryAccess()) == null) {
                unregistered++;
            }
        }
        helper.assertTrue(unregistered == 2,
                "the two rows naming thaumcraft:ench_repair/ench_haste (TC-M6-05) must be "
                        + "loaded with no resolvable holder, found " + unregistered);
        assertEveryLoadableRowLoaded(helper);
        RecipeHolder<InfusionCraftingRecipe> mirrorHand = null;
        for (final RecipeHolder<InfusionCraftingRecipe> row : dataRows) {
            if (row.value().originalKey().equals("MirrorHand")) {
                mirrorHand = row;
            }
        }
        helper.assertTrue(mirrorHand != null, "the extracted MirrorHand row did not register");
        helper.assertTrue("MIRRORHAND".equals(mirrorHand.value().research()),
                "the data row's research gate must survive the codec");
        helper.assertTrue(mirrorHand.value().instability() == 5,
                "the data row's instability must survive the codec, got "
                        + mirrorHand.value().instability());
        helper.assertTrue(mirrorHand.value().components().size() == 3,
                "the data row's component count must survive the codec, got "
                        + mirrorHand.value().components().size());
        helper.assertTrue(mirrorHand.value().aspects().getAmount(aspect("instrumentum")) == 16,
                "the data row's essentia cost must survive the codec");
        helper.assertTrue(mirrorHand.value().concreteOutput().is(
                BuiltInRegistries.ITEM.get(id("handmirror"))),
                "the data row's output item must survive the codec");

        centreOf(helper, ALTAR.below(2)).setItem(0, new ItemStack(Items.GOLD_INGOT));
        final TilePedestal componentPedestal =
                pedestalWith(helper, new BlockPos(5, 2, 1), new ItemStack(net.minecraft.world.item.Items.GLASS_PANE));
        final TestProbeContent.ProbeAspectSource source =
                seedAspectSource(helper, new BlockPos(3, 2, 1), "potentia", 5);

        // The original's onWandRightClick: first use arms the matrix, second
        // use starts a craft against whatever the pedestals hold.
        matrix.useWithoutItem((ServerLevel) helper.getLevel(), ALTAR, player);
        helper.assertTrue(matrix.active, "a valid idle matrix must activate on use");
        matrix.useWithoutItem((ServerLevel) helper.getLevel(), ALTAR, player);
        helper.assertTrue(matrix.crafting,
                "a second use with matching pedestals must start the craft");
        helper.assertTrue(!recipe.matches(
                new InfusionCraftingRecipe.InfusionInput(ItemStack.EMPTY, List.of(), player),
                helper.getLevel()),
                "an empty altar must never match");

        // The recipe's cost is paid out of the probe source over the original's
        // cycle cadence (countDelay is 10 ticks, the component animation five).
        int cycles = 0;
        while (matrix.crafting && cycles < 300) {
            tickMatrix(helper, ALTAR, matrix, 1);
            cycles++;
        }
        helper.assertTrue(!matrix.crafting,
                "the craft must finish inside 300 ticks, it is still crafting");

        // Both essentia units were taken from the seeded source, the component
        // pedestal is empty, and the central pedestal now holds the result.
        helper.assertTrue(TestProbeContent.ProbeAspectSource.drainedTotal() == 2,
                "the recipe's two potentia must have been drained, got "
                        + TestProbeContent.ProbeAspectSource.drainedTotal());
        helper.assertTrue(componentPedestal.getItem(0).isEmpty(),
                "the component item must have been consumed");
        final ItemStack result = centreOf(helper, ALTAR.below(2)).getItem(0);
        helper.assertTrue(result.is(Items.GOLD_BLOCK) && !result.isEmpty(),
                "the central pedestal must hold the crafted result");
        helper.assertTrue(matrix.instability == 0,
                "a clean craft ends with instability cleared, got " + matrix.instability);
        helper.succeed();
    }
}
