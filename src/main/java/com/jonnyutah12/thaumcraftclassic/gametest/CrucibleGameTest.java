package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaHandler;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.tiles.CrucibleStateMachine;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrucible;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TC-M4-05-p acceptance: the crucible (alchemy) core is on main — this probe
 * proves what the core card left unproven, namely that the crucible
 * {@em behaves}. The state machine math itself is pinned by the unit suite
 * (docs/testing.md, {@code CrucibleStateMachineTest}); these three probes run
 * it in a real world against the real registries and the shipped data.
 *
 * <ul>
 *   <li>{@link #crucibleCoreIsRegistered}: every contract id of the crucible
 *       resolves — the {@code tilecrucible} block entity, the crucible recipe
 *       type + serializer pair, and the {@code blockmetaldevice} variant-0
 *       host that hands out the tile (the original's
 *       {@code BlockMetalDevice.createTileEntity} metadata 0, and no other
 *       meta) — plus a shipped data row decoded twice over: once by the live
 *       {@link RecipeManager} through the registered serializer, once through
 *       {@link CatalogCrucible#CRUCIBLE_SERIALIZER}'s codec directly, with its
 *       component-carrying catalyst and meta-carrying result intact (D-001,
 *       D-006, D-015).</li>
 *   <li>{@link #entropicCycleCraftsTheBalancedShard}: one whole cycle in the
 *       world. A water bucket fills the 1000 mB tank through the block's
 *       item-use seam; a lava block below drives the heat step across the
 *       151 ignition edge; a thrown essentia-charged jar feeds the vis pool
 *       through the merged container + object-tag path
 *       ({@link TileCrucible#absorbedTags}); a thrown meta-0 shard then
 *       matches the research-gated data row, and the machine pays the matched
 *       recipe's cost out of the pool exactly, drains {@code CRAFT_DRAIN}
 *       water and ejects the result stamped so it is never re-absorbed. The
 *       essentia draw runs through the merged {@link EssentiaHandler}: a
 *       source in reach is drained through it, and the crucible itself is
 *       never a source (the original's {@code takeFromContainer} returns
 *       false).</li>
 *   <li>{@link #everyUngatedCrucibleRowIsLoaded}: the data invariant — every
 *       {@code *.crucible*} row under {@code data/thaumcraft/recipe/} is
 *       enumerated through the server {@code ResourceManager}, its
 *       {@code neoforge:conditions} gate is evaluated against the live
 *       registries, every ungated row must be present in the live
 *       {@link RecipeManager} under its own id, and every
 *       component-carrying catalyst must use the registered
 *       {@code neoforge:components} Ingredient form (D-015). The resolved
 *       count is logged; today every shipped row carries an
 *       {@code neoforge:item_exists} gate, and the rows whose gate is
 *       satisfied are the ones that must land.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrucibleGameTest {

    private CrucibleGameTest() {
    }

    // --- helpers -------------------------------------------------------------

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private static Aspect aspect(final String tag) {
        final Aspect found = Aspect.getAspect(tag);
        if (found == null) {
            throw new AssertionError("aspect " + tag + " is not registered");
        }
        return found;
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    /** The crucible: blockMetalDevice variant 0 at {@code pos}, lava below to burn. */
    private static TileCrucible crucibleAt(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.LAVA);
        helper.setBlock(pos, CatalogDevices.BLOCK_METAL_DEVICE.get().withVariant(0));
        final BlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity instanceof TileCrucible,
                "blockMetalDevice variant 0 must create the crucible tile, got " + entity);
        return (TileCrucible) entity;
    }

    /**
     * A dummy thrower carrying the given research keys, with the same net shape
     * the sibling probes use ({@code InfusionAltarGameTest.freshPlayer},
     * {@code AspectSystemGameTest.freshPlayer}): the craft leg posts
     * {@code PlayerEvent.ItemCraftedEvent}, and the research listeners answer
     * with payloads, so the ad-hoc channels declare what that path can send.
     *
     * <p>The tile resolves a thrown item's owner by name through
     * {@code server.getPlayerList().getPlayerByName(username)} — the port of
     * the original's {@code EventHandlerEntity} throw registry — so the
     * thrower has to be listed the way a real thrower is.
     * {@code PlayerList.getPlayers()} hands out an unmodifiable view and the
     * only other door is {@code placeNewPlayer}, which needs a live login
     * handshake, so the probe inserts its thrower into the server's own list
     * for the probe's duration and removes it again in a {@code finally}.</p>
     */
    private static ServerPlayer throwerWithResearch(final GameTestHelper helper,
                                                    final List<String> keys) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "tc-crucible"), false);
        final ServerPlayer thrower = new ServerPlayer(server, helper.getLevel(),
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
        adHoc.add(ResearchCompletePayload.TYPE.id());
        thrower.connection =
                new ServerGamePacketListenerImpl(server, connection, thrower, cookie);

        PlayerKnowledgeData knowledge = thrower.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final String key : keys) {
            knowledge = knowledge.withResearchCompleted(key);
        }
        thrower.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        setPlayerListed(server, thrower, true);
        return thrower;
    }

    @SuppressWarnings("unchecked")
    private static void setPlayerListed(final MinecraftServer server,
                                        final ServerPlayer thrower, final boolean listed) {
        try {
            final Field field = PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            final List<ServerPlayer> players = (List<ServerPlayer>) field.get(server.getPlayerList());
            if (listed) {
                players.add(thrower);
            } else {
                players.remove(thrower);
            }
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("the probe cannot list its thrower with the server: "
                    + failure, failure);
        }
    }

    /**
     * An item entity hovering in the crucible mouth (the tile's own eject
     * point), tagged with its thrower. {@code at} is the crucible's
     * {@code worldPosition} — the gametest structure sits far from the world
     * origin, so anything read by the tile itself has to be placed in world
     * coordinates, not in the helper's local ones.
     */
    private static ItemEntity thrown(final GameTestHelper helper, final BlockPos at,
                                     final ItemStack stack, final ServerPlayer thrower) {
        final ItemEntity entity = new ItemEntity(helper.getLevel(),
                at.getX() + 0.5, at.getY() + 0.71, at.getZ() + 0.5, stack.copy());
        if (thrower != null) {
            entity.getPersistentData().putString(TileCrucible.THROWER_TAG,
                    thrower.getGameProfile().getName());
        }
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    /** The tile's own selection rule over the loaded rows (original findMatchingCrucibleRecipe). */
    private static RecipeHolder<CrucibleRecipe> bestMatch(final GameTestHelper helper,
                                                           final ItemStack catalyst,
                                                           final AspectList pool,
                                                           final ServerPlayer thrower) {
        return CrucibleStateMachine.findBestMatch(
                helper.getLevel().getRecipeManager()
                        .getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get()),
                holder -> holder.value().aspectKeyCount(),
                holder -> holder.value().researchUnlocked(thrower)
                        && holder.value().matches(
                                new CrucibleRecipe.CrucibleInput(catalyst, pool, thrower),
                                helper.getLevel()));
    }

    // --- probes --------------------------------------------------------------

    @GameTest(template = "empty")
    public static void crucibleCoreIsRegistered(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id("blockmetaldevice")),
                "the crucible's host block blockmetaldevice is not registered");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id("tilecrucible")),
                "the crucible block entity id tilecrucible is not registered");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(id("crucible")),
                "the crucible recipe type is not registered");
        helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id("crucible")),
                "the crucible recipe serializer is not registered");

        // The host hands out the tile only for the crucible variant (the
        // original's createTileEntity switch: meta 0 crucible, 7/8/13 lamps).
        // A lamp on its own cell: setBlock keeps the block entity of an
        // existing same-block cell, so the two variants need two cells.
        helper.setBlock(new BlockPos(1, 1, 2),
                CatalogDevices.BLOCK_METAL_DEVICE.get().withVariant(7));
        helper.assertTrue(!(helper.getBlockEntity(new BlockPos(1, 1, 2)) instanceof TileCrucible),
                "variant 7 (arcane lamp) must not hand out a crucible tile");
        final TileCrucible crucible = crucibleAt(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(CatalogCrucible.TILE_CRUCIBLE.get() == crucible.getType(),
                "the crucible tile must be the registered tilecrucible type");

        final ServerLevel level = helper.getLevel();
        final RecipeManager recipes = level.getRecipeManager();

        // 1. A shipped row reached the live manager, i.e. it decoded through
        //    the registered serializer at load with its payload intact.
        final var row = recipes.byKey(id("recipe.0.crucible.0"));
        helper.assertTrue(row.isPresent(),
                "recipe.0.crucible.0 did not resolve through the crucible serializer");
        helper.assertTrue(row.get().value() instanceof CrucibleRecipe,
                "recipe.0.crucible.0 did not decode as a CrucibleRecipe, got "
                        + row.get().value().getClass().getName());
        final CrucibleRecipe balanced = (CrucibleRecipe) row.get().value();
        helper.assertTrue("CRUCIBLE".equals(balanced.research()),
                "the data row's research gate must survive the codec, got " + balanced.research());
        helper.assertTrue(balanced.aspectKeyCount() == 5,
                "the BalancedShard row keeps the five primals of its loop instance, got "
                        + balanced.aspectKeyCount());

        // D-015: the catalyst is a components-carrying Ingredient — it matches
        // the meta-0 shard and refuses a different meta of the same item.
        final ItemStack shardMeta0 = new ItemStack(item("itemshard"));
        shardMeta0.set(ModDataComponents.ITEM_META.get(), 0);
        helper.assertTrue(balanced.catalyst().test(shardMeta0),
                "the component-carrying catalyst ingredient must match a meta-0 shard");
        final ItemStack shardMeta6 = new ItemStack(item("itemshard"));
        shardMeta6.set(ModDataComponents.ITEM_META.get(), 6);
        helper.assertTrue(!balanced.catalyst().test(shardMeta6),
                "a components-carrying ingredient must not match every meta of the family");

        // D-006: the result rides the thaumcraft:item_meta component.
        final ItemStack output = balanced.getResultItem(level.registryAccess());
        helper.assertTrue(output.is(item("itemshard"))
                        && output.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == 6,
                "the balanced-shard result must ride the thaumcraft:item_meta component (D-006), got "
                        + output);

        // 2. The same shipped bytes decode through the serializer's codec on
        //    their own, so the pair (not a vanilla path) is what reads them.
        final var resources = level.getServer().getResourceManager();
        final ResourceLocation shipped = id("recipe/recipe.0.crucible.0.json");
        final JsonObject json;
        try (var reader = resources.getResource(shipped).orElseThrow().openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (final IOException failure) {
            helper.fail("unreadable crucible recipe resource " + shipped + ": " + failure);
            return;
        }
        helper.assertTrue("thaumcraft:crucible".equals(json.get("type").getAsString()),
                "the crucible row must name the thaumcraft:crucible type");
        final var decoded = CatalogCrucible.CRUCIBLE_SERIALIZER.get().codec().codec()
                .parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), json)
                .result();
        helper.assertTrue(decoded.isPresent(),
                "the crucible serializer's codec must decode its own shipped row: "
                        + json.get("type").getAsString());
        helper.assertTrue(decoded.get().research().equals(balanced.research())
                        && decoded.get().aspectKeyCount() == balanced.aspectKeyCount(),
                "the codec decode must agree with the loaded instance");

        int loaded = 0;
        for (final RecipeHolder<CrucibleRecipe> holder
                : recipes.getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get())) {
            loaded++;
        }
        helper.assertTrue(loaded >= 10,
                "at least 10 crucible data rows must resolve through the live RecipeManager, got "
                        + loaded);
        ThaumcraftClassic.LOGGER.info("[TC-M4-05-p] crucible registration: type + serializer + "
                + "tilecrucible resolve; {} crucible rows decoded through the live RecipeManager",
                loaded);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void entropicCycleCraftsTheBalancedShard(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = new BlockPos(1, 1, 1);
        final TileCrucible crucible = crucibleAt(helper, pos);
        // The tile's own world position: the tile reads its neighbourhood, the
        // entity search box and the essentia scan all live in world space.
        final BlockPos at = crucible.getBlockPos();
        final ServerPlayer thrower = throwerWithResearch(helper, List.of("CRUCIBLE"));
        try {
            // 1. Water in, through the block's held-item seam (the original's
            //    onBlockActivated bucket branch).
            thrower.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
            crucible.useWithItem(level, at, thrower, InteractionHand.MAIN_HAND);
            helper.assertTrue(crucible.tank().getFluidAmount() == CrucibleStateMachine.TANK_CAPACITY,
                    "a water bucket must fill the 1000 mB tank, got "
                            + crucible.tank().getFluidAmount());

            // 2. Ignition: the lava below drives the heat step (1/tick, no bellows).
            int ticks = 0;
            int prev = 0;
            boolean ignited = false;
            while (!crucible.isBoiling() && ticks < 400) {
                prev = crucible.heat();
                crucible.serverTick();
                ticks++;
                if (CrucibleStateMachine.clientIgnitionNudge(prev, crucible.heat())) {
                    ignited = true;
                }
            }
            helper.assertTrue(crucible.isBoiling(),
                    "the crucible must cross the boiling point with a heat source below, heat="
                            + crucible.heat() + " after " + ticks + " ticks");
            helper.assertTrue(crucible.heat() >= CrucibleStateMachine.IGNITION_EDGE,
                    "the ignition edge is heat " + CrucibleStateMachine.IGNITION_EDGE + ", got "
                            + crucible.heat());
            helper.assertTrue(ignited, "the 150 -> 151 step must be the ignition edge");

            // 3. Absorption: a thrown jar that carries essentia in its
            //    container feeds the vis pool through the merged aspect path.
            final List<String> primals =
                    List.of("ignis", "aqua", "terra", "ordo", "perditio");
            final AspectList charge = new AspectList();
            for (final String tag : primals) {
                charge.add(aspect(tag), 2);
            }
            final ItemStack jar = new ItemStack(item("itemresource"));
            jar.set(ModDataComponents.ESSENTIA_CONTAINER.get(),
                    EssentiaContainer.fromAspectList(charge));
            crucible.attemptSmelt(thrown(helper, at, jar, null));
            for (final String tag : primals) {
                helper.assertTrue(crucible.pool().getAmount(aspect(tag)) >= 2,
                        "the thrown essentia jar must feed the vis pool through the merged "
                                + "aspect data: " + tag + " = "
                                + crucible.pool().getAmount(aspect(tag)) + ", pool="
                                + crucible.pool());
            }

            // 4. The craft: the meta-0 shard catalyst, gated on CRUCIBLE.
            final ItemStack catalyst = new ItemStack(item("itemshard"));
            catalyst.set(ModDataComponents.ITEM_META.get(), 0);
            final RecipeHolder<CrucibleRecipe> expected =
                    bestMatch(helper, catalyst, crucible.pool(), thrower);
            helper.assertTrue(expected != null,
                    "the loaded rows must contain a crucible recipe for a meta-0 shard with "
                            + "CRUCIBLE researched and the seeded pool");
            final AspectList cost = expected.value().aspects();
            final int[] before = new int[cost.getAspects().size()];
            for (int i = 0; i < before.length; i++) {
                before[i] = crucible.pool().getAmount(cost.getAspects().get(i));
            }
            final int water = crucible.tank().getFluidAmount();

            crucible.attemptSmelt(thrown(helper, at, catalyst, thrower));

            for (int i = 0; i < before.length; i++) {
                final Aspect tag = cost.getAspects().get(i);
                helper.assertTrue(crucible.pool().getAmount(tag) == before[i] - cost.getAmount(tag),
                        "the craft must subtract exactly the recipe's cost from the pool: "
                                + tag.getTag() + " " + before[i] + " -> "
                                + crucible.pool().getAmount(tag) + " (cost "
                                + cost.getAmount(tag) + ")");
            }
            helper.assertTrue(water - crucible.tank().getFluidAmount()
                            == CrucibleStateMachine.CRAFT_DRAIN,
                    "the craft drains " + CrucibleStateMachine.CRAFT_DRAIN + " water, got "
                            + (water - crucible.tank().getFluidAmount()));

            final ItemStack wanted = expected.value().getResultItem(level.registryAccess());
            final int meta = wanted.getOrDefault(ModDataComponents.ITEM_META.get(), -1);
            final List<ItemEntity> ejected = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(at.getX() - 2, at.getY() - 2, at.getZ() - 2,
                            at.getX() + 3, at.getY() + 3, at.getZ() + 3));
            boolean sawResult = false;
            for (final ItemEntity drop : ejected) {
                if (drop.getItem().is(wanted.getItem())
                        && drop.getItem().getOrDefault(ModDataComponents.ITEM_META.get(), -1) == meta) {
                    sawResult = true;
                    helper.assertTrue(drop.getPersistentData().getBoolean(TileCrucible.EJECT_TAG),
                            "an ejected crucible result must be stamped so it is never re-absorbed");
                }
            }
            helper.assertTrue(sawResult,
                    "the crucible must eject the crafted result ("
                            + BuiltInRegistries.ITEM.getKey(wanted.getItem()).getPath()
                            + " meta " + meta + ")");

            // 5. The merged essentia handler: the crucible is never a source
            //    (the original's container drain surface returns false), and a
            //    source in reach is drawn through the handler, not the pool.
            final Aspect ignis = aspect("ignis");
            EssentiaHandler.clearAllSources();
            helper.assertTrue(!EssentiaHandler.drainEssentia(level, at, ignis, 1),
                    "the crucible must never be drained as an essentia source (the original's "
                            + "TileCrucible.takeFromContainer returns false)");
            final int poolBefore = crucible.pool().getAmount(ignis);
            final BlockPos sourceLocal = new BlockPos(2, 1, 1);
            final BlockPos sourcePos = helper.absolutePos(sourceLocal);
            helper.setBlock(sourceLocal, TestProbeContent.PROBE_ASPECT_SOURCE_BLOCK.get());
            final TestProbeContent.ProbeAspectSource source =
                    (TestProbeContent.ProbeAspectSource) helper.getBlockEntity(sourceLocal);
            source.seed(ignis, 5);
            EssentiaHandler.refreshSources(at);
            helper.assertTrue(EssentiaHandler.drainEssentia(level, at, ignis, 1),
                    "the merged EssentiaHandler must draw from a source in reach of the crucible");
            helper.assertTrue(source.amount(ignis) == 4,
                    "the draw must come out of the source tile, got " + source.amount(ignis));
            helper.assertTrue(crucible.pool().getAmount(ignis) == poolBefore,
                    "the handler draw must not touch the crucible's own pool");
            EssentiaHandler.clearAllSources();

            ThaumcraftClassic.LOGGER.info("[TC-M4-05-p] crucible cycle: {} ticks to ignition, "
                            + "pool {} vis after absorption, crafted {} meta {} (research {}, row {})",
                    ticks, crucible.pool().visSize(),
                    BuiltInRegistries.ITEM.getKey(wanted.getItem()).getPath(), meta,
                    expected.value().research(), expected.id());
            helper.succeed();
        } finally {
            setPlayerListed(level.getServer(), thrower, false);
        }
    }

    @GameTest(template = "empty")
    public static void everyUngatedCrucibleRowIsLoaded(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final var resources = level.getServer().getResourceManager();
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        final TileCrucible crucible = crucibleAt(helper, new BlockPos(1, 1, 1));
        final ServerPlayer thrower = throwerWithResearch(helper, List.of("INFUSION", "CRUCIBLE"));
        try {
            recordUngatedRows(helper, level, resources, manager, crucible, thrower);
        } finally {
            setPlayerListed(level.getServer(), thrower, false);
        }
    }

    private static void recordUngatedRows(final GameTestHelper helper, final Level level,
            final net.minecraft.server.packs.resources.ResourceManager resources,
            final RecipeManager manager, final TileCrucible crucible,
            final ServerPlayer thrower) {

        final List<String> ungated = new ArrayList<>();
        final java.util.Map<String, JsonObject> rows = new java.util.LinkedHashMap<>();
        final List<String> missing = new ArrayList<>();
        final List<String> badIngredientForm = new ArrayList<>();
        int shipped = 0;
        int gated = 0;
        int componentCatalysts = 0;

        for (final var entry : resources.listResources("recipe", path ->
                path.getNamespace().equals(ThaumcraftClassic.MOD_ID)
                        && path.getPath().contains(".crucible")).entrySet()) {
            final String path = entry.getKey().getPath();
            final JsonObject shippedJson;
            // listResources keeps the namespace-local path with its extension;
            // data/thaumcraft/recipe/<name>.json loads under thaumcraft:<name>.
            final String file = path.startsWith("recipe/")
                    ? path.substring("recipe/".length()) : path;
            final String rowId = file.endsWith(".json")
                    ? file.substring(0, file.length() - ".json".length()) : file;
            shipped++;
            try (var reader = entry.getValue().openAsReader()) {
                shippedJson = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (final IOException failure) {
                helper.fail("unreadable crucible recipe resource " + entry.getKey() + ": "
                        + failure);
                return;
            }
            helper.assertTrue("thaumcraft:crucible".equals(shippedJson.get("type").getAsString()),
                    "the crucible row " + rowId + " must name the thaumcraft:crucible type");

            // D-015: a components-carrying catalyst is the registered
            // neoforge:components Ingredient, with items as a list.
            final JsonObject catalyst = shippedJson.getAsJsonObject("catalyst");
            helper.assertTrue(catalyst != null, "the crucible row " + rowId + " has no catalyst");
            if (catalyst.has("components") || catalyst.has("neoforge:components")) {
                componentCatalysts++;
                if (!"neoforge:components".equals(optString(catalyst, "type"))
                        || !catalyst.has("items") || !catalyst.get("items").isJsonArray()) {
                    badIngredientForm.add(rowId);
                }
            }

            boolean rowGated = false;
            final JsonElement conditions = shippedJson.get("neoforge:conditions");
            if (conditions != null && conditions.isJsonArray()) {
                for (final var condition : conditions.getAsJsonArray()) {
                    final JsonObject guard = condition.getAsJsonObject();
                    final String type = guard.get("type").getAsString();
                    if ("neoforge:item_exists".equals(type)) {
                        if (!BuiltInRegistries.ITEM.containsKey(
                                ResourceLocation.parse(guard.get("item").getAsString()))) {
                            rowGated = true; // the row is legitimately skipped at load
                        }
                    } else if ("neoforge:not".equals(type)) {
                        final JsonObject inner = guard.getAsJsonObject("value");
                        if ("neoforge:tag_empty".equals(inner.get("type").getAsString())
                                && tagEmpty(helper, inner.get("tag").getAsString())) {
                            rowGated = true; // the original's Config.foundXIngot stand-in
                        }
                    } else {
                        rowGated = true; // a condition this probe cannot evaluate
                    }
                }
            }
            if (rowGated) {
                gated++;
                continue;
            }
            ungated.add(rowId);
            rows.put(rowId, shippedJson);
            if (manager.byKey(id(rowId)).isEmpty()) {
                missing.add(rowId);
            }
        }

        helper.assertTrue(shipped >= 48,
                "the crucible data ships 48 recipe rows (54 instances), got " + shipped
                        + " resource files");
        helper.assertTrue(badIngredientForm.isEmpty(),
                "rows using the malformed bare neoforge:components ingredient (D-015): "
                        + String.join(", ", badIngredientForm));
        helper.assertTrue(missing.isEmpty(),
                "every ungated crucible row must be present in the live RecipeManager "
                        + "(a row that fails to decode is dropped in silence — check the "
                        + "server log's RecipeManager parse error), missing: "
                        + String.join(", ", missing));
        helper.assertTrue(ungated.size() >= 10,
                "at least 10 crucible rows must pass their item_exists gate, got "
                        + ungated.size());

        // The research source gate. The pool is filled first — the original's
        // CrucibleRecipe.matches reads the pool before the research gate, so an
        // empty pool would make the assertions below true for the wrong reason —
        // and the thrower deliberately knows only INFUSION and CRUCIBLE, not
        // ALCHEMY, the source of most of the shipped rows.
        final AspectList pool = crucible.pool();
        final AspectList charge = new AspectList();
        for (final Aspect aspect : Aspect.values()) {
            charge.add(aspect, 64);
        }
        for (int jar = 0; jar < 2; jar++) {
            final ItemStack fuel = new ItemStack(item("itemresource"));
            fuel.set(ModDataComponents.ESSENTIA_CONTAINER.get(),
                    EssentiaContainer.fromAspectList(charge));
            crucible.attemptSmelt(thrown(helper, crucible.getBlockPos(), fuel, thrower));
        }
        helper.assertTrue(pool.visSize() > 0, "the seeded jars must fill the vis pool, got " + pool);
        final PlayerKnowledgeData knowledge = thrower.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final String known : new String[] {"INFUSION", "CRUCIBLE"}) {
            helper.assertTrue(ResearchManager.isResearchComplete(knowledge, known),
                    "the probe thrower must know " + known + " for the source gate");
        }
        final var knownRow = manager.byKey(id("recipe.0.crucible.0"));
        helper.assertTrue(knownRow.isPresent() && knownRow.get().value() instanceof CrucibleRecipe,
                "the balanced-shard row must be live for the research gate below");
        final CrucibleRecipe known = (CrucibleRecipe) knownRow.get().value();
        helper.assertTrue("CRUCIBLE".equals(known.research()),
                "the balanced-shard row is gated on CRUCIBLE research, got " + known.research());
        helper.assertTrue(known.researchUnlocked(thrower),
                "a thrower who knows CRUCIBLE must be able to unlock that row");
        helper.assertTrue(!known.researchUnlocked(null),
                "the original's CrucibleRecipe.matches returns false with no player at all");

        int researchGated = 0;
        int payableWhileGated = 0;
        for (final String rowId : ungated) {
            final JsonObject row = rows.get(rowId);
            final CrucibleRecipe parsed = CatalogCrucible.CRUCIBLE_SERIALIZER.get().codec().codec()
                    .parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), row)
                    .result().orElse(null);
            if (parsed == null) {
                continue; // already proven absent by the live-registry assertion
            }
            if (parsed.researchUnlocked(thrower)) {
                continue; // the gate is open for this thrower
            }
            researchGated++;
            final AspectList cost = parsed.aspects();
            boolean payable = true;
            for (final Aspect aspect : cost.getAspects()) {
                payable &= pool.getAmount(aspect) >= cost.getAmount(aspect);
            }
            if (payable) {
                payableWhileGated++;
            }
            final ItemStack catalyst = catalystStack(row.getAsJsonObject("catalyst"));
            helper.assertTrue(!catalyst.isEmpty(),
                    "the catalyst of " + rowId + " names no item this probe can throw: "
                            + row.getAsJsonObject("catalyst"));
            helper.assertTrue(bestMatch(helper, catalyst, pool, thrower) == null,
                    "a crucible row the thrower has not unlocked (" + parsed.research()
                            + ") must not match the recipe list: " + rowId);
        }
        helper.assertTrue(researchGated >= 1,
                "at least one live crucible row must be gated on research this thrower does not "
                        + "know, otherwise the source gate above proves nothing (pool " + pool + ")");
        helper.assertTrue(payableWhileGated >= 1,
                "at least one research-gated row must be payable by the seeded pool, otherwise the "
                        + "gate assertion above proves nothing (" + researchGated
                        + " gated rows, pool " + pool + ")");

        ThaumcraftClassic.LOGGER.info("[TC-M4-05-p] crucible data: {} row files shipped, "
                        + "{} pass their gate and are live in the RecipeManager, {} gated out, "
                        + "{} resolve in total, {} use the registered neoforge:components "
                        + "ingredient form, {} of the live rows are gated on research this "
                        + "thrower does not know ({} of those payable by the seeded pool)",
                shipped, ungated.size() - missing.size(), gated,
                manager.getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get()).size(),
                componentCatalysts, researchGated, payableWhileGated);
        helper.succeed();
    }

    // --- probe-local readers -------------------------------------------------

    private static String optString(final JsonObject json, final String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }

    /**
     * A stack the row's shipped catalyst form would accept. Bare ingredients
     * name one {@code item}; the registered {@code neoforge:components} form
     * (D-015) names an {@code items} list, and its component predicate is
     * evaluated by the live {@link Ingredient}, not re-derived here — the
     * {@code thaumcraft:item_meta} default (D-006) makes a bare stack meta 0,
     * so a row whose catalyst pins another meta simply does not match the
     * stack this returns, which is the safe direction for the probe's
     * negative ("this row must not match") assertion.
     */
    private static ItemStack catalystStack(final JsonObject catalyst) {
        String id = optString(catalyst, "item");
        if (id == null && catalyst.has("items") && catalyst.get("items").isJsonArray()
                && !catalyst.getAsJsonArray("items").isEmpty()) {
            id = catalyst.getAsJsonArray("items").get(0).getAsString();
        }
        if (id == null) {
            return ItemStack.EMPTY;
        }
        final Item found = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return found == null ? ItemStack.EMPTY : new ItemStack(found);
    }

    /** True when an item tag has no members in the live (datapack) tag set. */
    private static boolean tagEmpty(final GameTestHelper helper, final String tag) {
        final var held = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ITEM)
                .get(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
        return held.isEmpty() || held.get().size() == 0;
    }
}