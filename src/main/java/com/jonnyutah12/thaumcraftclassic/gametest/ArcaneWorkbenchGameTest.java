package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.recipes.M403RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M403ArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * TC-M4-03 acceptance: the arcane workbench end-to-end.
 *
 * <p>Pins the behaviours the card's acceptance criteria name: the block/BE/
 * menu/recipe-type registration, the research gate (locked until ALUMENTUM is
 * known), the vis payment (exact centi-vis, all-or-nothing, on take), the
 * insufficient-vis blocked result, vanilla-recipe priority over an overlapping
 * arcane recipe (with the original's pay-anyway quirk), the wand-only slot,
 * and grid persistence across GUI close/reopen (the tile owns the matrix).</p>
 *
 * <p>Uses the test-only recipes {@code test_arcane_workbench[_shapeless|
 * _overlap].json} (shaped single-slot, shapeless, and one overlapping the
 * vanilla iron-block recipe) and the test-only vis container item
 * {@code test_probe_vis_container} — both gated to the gameTestServer.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneWorkbenchGameTest {

    private static final String GATE_RESEARCH = "ALUMENTUM";

    private ArcaneWorkbenchGameTest() {
    }

    @GameTest(template = "empty")
    public static void registrationAndRecipeDataPresent(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(
                        id("blocktable")), "blocktable not registered");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(
                        id("tilearcaneworkbench")), "tilearcaneworkbench not registered");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(
                        id("gui_arcane_workbench")), "gui_arcane_workbench menu not registered");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(
                        id("arcane_workbench")), "arcane_workbench recipe type not registered");
        helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(
                        id("arcane_workbench")), "arcane_workbench serializer not registered");

        // The 109 shipped instances (TC-M4-04 inventory) plus the 3 test
        // recipes must all decode through the real datapack path.
        final var loaded = helper.getLevel().getRecipeManager()
                .recipes(M403RecipeSerializers.ARCANE_WORKBENCH_TYPE.get());
        helper.assertTrue(loaded.size() == 112,
                "expected 109 shipped + 3 test arcane recipes, got " + loaded.size());
        helper.assertTrue(helper.getLevel().getRecipeManager().byId(id("test_arcane_workbench")) != null
                        && helper.getLevel().getRecipeManager().byId(id("test_arcane_workbench_shapeless")) != null
                        && helper.getLevel().getRecipeManager().byId(id("test_arcane_workbench_overlap")) != null,
                "test arcane recipes did not load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchGateThenVisPayment(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final TileArcaneWorkbench tile = workbenchAt(helper, BlockPos.ZERO);

        final ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), tile);
        tile.setItem(0, new ItemStack(Items.IRON_INGOT));
        menu.slotsChanged(tile);

        // Gate: ALUMENTUM unknown → no result, overlay reports the lock.
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "a gated recipe must not produce a result before the research is known");
        final ArcaneWorkbenchMenu.Overlay locked = menu.overlay();
        helper.assertTrue(locked.recipe() != null && locked.researchLocked(),
                "the overlay must report the research lock");

        // No wand yet: even with the research known, no result (the original
        // requires an ItemWandCasting in slot 10 to show an arcane result).
        grant(player, GATE_RESEARCH);
        menu.slotsChanged(tile);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "without a wand the arcane result must stay empty");

        // Funded wand: result appears, cost renders as 200+200 centi-vis.
        final ItemStack wand = fundedWand(Map.of("ignis", 400, "aqua", 400));
        tile.setItem(TileArcaneWorkbench.SLOT_WAND, wand);
        menu.slotsChanged(tile);
        final ArcaneWorkbenchMenu.Overlay open = menu.overlay();
        helper.assertTrue(open.recipe() != null && !open.researchLocked() && open.payable(),
                "with research + funded wand the recipe must be payable");
        helper.assertTrue(open.effectiveCost().getAmount(Aspect.getAspect("ignis")) == 200
                        && open.effectiveCost().getAmount(Aspect.getAspect("aqua")) == 200,
                "vis price must scale to centi-vis (2 vis → 200c)");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(),
                        new ItemStack(Items.GLASS)),
                "result must be the recipe output");

        // Take: grid consumed, pool debited exactly (400/400 → 200/200).
        takeResult(menu, player);
        helper.assertTrue(tile.getItem(0).isEmpty(), "the grid ingredient must be consumed");
        final VisPool after = WandVis.poolOf(tile.getItem(TileArcaneWorkbench.SLOT_WAND));
        helper.assertTrue(after.getVis(Aspect.getAspect("ignis")) == 200
                        && after.getVis(Aspect.getAspect("aqua")) == 200,
                "payment must be exactly the effective cost: got ignis="
                        + after.getVis(Aspect.getAspect("ignis")) + " aqua="
                        + after.getVis(Aspect.getAspect("aqua")));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shapelessCraftAndVanillaPriority(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        grant(player, GATE_RESEARCH);
        final TileArcaneWorkbench tile = workbenchAt(helper, BlockPos.ZERO);
        tile.setItem(TileArcaneWorkbench.SLOT_WAND, fundedWand(
                Map.of("ordo", 300, "aer", 300)));
        final ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), tile);

        // Shapeless: 2 coal → glass, 1 vis ordo (100c).
        tile.setItem(0, new ItemStack(Items.COAL));
        tile.setItem(1, new ItemStack(Items.COAL));
        menu.slotsChanged(tile);
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(),
                        new ItemStack(Items.GLASS)),
                "the shapeless arcane recipe must match");
        takeResult(menu, player);
        helper.assertTrue(WandVis.poolOf(tile.getItem(TileArcaneWorkbench.SLOT_WAND))
                        .getVis(Aspect.getAspect("ordo")) == 200,
                "shapeless payment must be 100c of ordo");
        helper.assertTrue(tile.getItem(0).isEmpty() && tile.getItem(1).isEmpty(),
                "shapeless ingredients must be consumed");

        // Overlap: the 3x3 iron grid matches BOTH the vanilla iron-block
        // recipe and the test arcane recipe. Vanilla wins the result (the
        // original checks CraftingManager first), but the original still paid
        // vis for any matching arcane recipe on take — quirk pinned.
        for (int i = 0; i < 9; i++) {
            tile.setItem(i, new ItemStack(Items.IRON_INGOT));
        }
        menu.slotsChanged(tile);
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(),
                        new ItemStack(Items.IRON_BLOCK)),
                "a vanilla match must win over an overlapping arcane recipe");
        takeResult(menu, player);
        helper.assertTrue(WandVis.poolOf(tile.getItem(TileArcaneWorkbench.SLOT_WAND))
                        .getVis(Aspect.getAspect("aer")) == 200,
                "the original pays the overlapping arcane recipe's vis anyway");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void insufficientVisBlocksAndGridPersists(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        grant(player, GATE_RESEARCH);
        final TileArcaneWorkbench tile = workbenchAt(helper, BlockPos.ZERO);

        // Underfunded wand: 100c ignis + 100c aqua < 200+200 → no result,
        // overlay reports unpayable (the original's grayed-slot state).
        tile.setItem(TileArcaneWorkbench.SLOT_WAND, fundedWand(Map.of("ignis", 100, "aqua", 100)));
        tile.setItem(0, new ItemStack(Items.IRON_INGOT));
        final ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), tile);
        menu.slotsChanged(tile);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "an unaffordable arcane recipe must not produce a result");
        final ArcaneWorkbenchMenu.Overlay overlay = menu.overlay();
        helper.assertTrue(overlay.recipe() != null && !overlay.researchLocked() && !overlay.payable(),
                "the overlay must report the unpayable state");

        // Wand slot rules (original SlotLimitedByWand): the vis container is
        // accepted, a plain item is not.
        helper.assertTrue(menu.getSlot(TileArcaneWorkbench.SLOT_WAND)
                        .mayPlace(fundedWand(Map.of())),
                "the wand slot must accept vis containers");
        helper.assertTrue(!menu.getSlot(TileArcaneWorkbench.SLOT_WAND)
                        .mayPlace(new ItemStack(Items.IRON_INGOT)),
                "the wand slot must reject non-wand items");

        // The grid lives in the tile: a fresh menu (GUI reopen) sees it intact.
        final ArcaneWorkbenchMenu reopened = new ArcaneWorkbenchMenu(2, player.getInventory(), tile);
        helper.assertTrue(ItemStack.isSameItemSameComponents(reopened.getSlot(2).getItem(),
                        new ItemStack(Items.IRON_INGOT)),
                "the grid must persist across GUI close/reopen");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static TileArcaneWorkbench workbenchAt(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, M403ArcaneWorkbench.BLOCK_TABLE.get()
                .withVariant(com.jonnyutah12.thaumcraftclassic.blocks.BlockTable
                        .VARIANT_ARCANE_WORKBENCH));
        return helper.getBlockEntity(pos, TileArcaneWorkbench.class);
    }

    private static ItemStack fundedWand(final Map<String, Integer> centiVis) {
        final ItemStack wand = new ItemStack(TestProbeContent.PROBE_VIS_CONTAINER.get());
        WandVis.setPool(wand, new VisPool(Map.copyOf(centiVis)));
        return wand;
    }

    private static void grant(final ServerPlayer player, final String key) {
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                player.getData(ModAttachments.PLAYER_KNOWLEDGE).withResearchCompleted(key));
    }

    /** Simulates the client's take click on the result slot. */
    private static void takeResult(final ArcaneWorkbenchMenu menu, final ServerPlayer player) {
        final ItemStack result = menu.getSlot(0).getItem().copy();
        menu.getSlot(0).set(ItemStack.EMPTY);
        player.getInventory().add(result);
        menu.getSlot(0).onTake(player, result);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    /** Same connection-wired player as ResearchProgressionGameTest (the menu
     *  sends container packets through it). */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-arcane-"
                        + UUID.randomUUID().toString().substring(0, 8)), false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

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
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
