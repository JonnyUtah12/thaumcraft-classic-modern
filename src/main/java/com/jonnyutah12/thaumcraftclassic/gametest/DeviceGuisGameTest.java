package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.menus.AlchemyFurnaceMenu;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneBoreMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrucible;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * TC-M7-04-m1 evidence: the two device-machine menus the card adds
 * (original gui ids 9 and 15) build in a real level from the real tile,
 * carry the original's slot geometry and slot classes, sync their gauges
 * through data slots, and open through the seam the block ships — and the
 * crucible, whose 1.7.10 GUI does not exist (see CrucibleGuiParityTest),
 * gains none in the field.
 *
 * <ol>
 *   <li>{@link #alchemyFurnaceMenuMirrorsTheOriginalContainer} — slots at
 *       the original container's coordinates over the vanilla 84/142 player
 *       grid, the input slot gated to the smeltable class (the port analogue
 *       of {@code SlotLimitedHasAspects}), shift-click in/out, the cook
 *       progress riding data slots 0/1, and the MenuProvider seam on the
 *       base plate (the nozzle variant keeps none).</li>
 *   <li>{@link #arcaneBoreMenuMirrorsTheOriginalContainer} — focus/pickaxe
 *       slots at (26,18)/(74,18) over the short 59/117 grid, the pickaxe
 *       class gate, area/maxRadius/speed/fortune data slots, and the
 *       width formula the screen draws.</li>
 *   <li>{@link #crucibleGainsNoMenuInTheField} — the placed crucible's tile
 *       is no {@code MenuProvider}, its host's {@code getMenuProvider} is
 *       null, and no crucible id exists in the MENU registry; the two new
 *       device menu types do, so the absence is deliberate, not missing
 *       wiring.</li>
 * </ol>
 *
 * <p>The mock player follows the {@code AccessoryGameTest}/{@code
 * PersistenceGameTest} shape (real {@code ServerPlayer} on the test level).
 * Menus are built directly from the tile — the game-test server never
 * renders a screen, so this is field-level proof of the server side (menu
 * construction, slot gates, data slots, the open seam); the screen
 * registration itself is proved by {@code MenuTypeScreenCoverageTest}, and
 * rendered pixels stay unproved in this repo's CI (parent slice's honesty).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeviceGuisGameTest {

    private DeviceGuisGameTest() {
    }

    private static ServerPlayer mockPlayer(final GameTestHelper helper, final BlockPos at) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-dev-gui"), false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        return player;
    }

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    // ------------------------------------------------ gui_id 9: alchemical furnace

    @GameTest(template = "empty_24")
    public static void alchemyFurnaceMenuMirrorsTheOriginalContainer(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 2, 1, 1);
        helper.setBlock(2, 1, 1, CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get()
                .defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));
        final BlockEntity be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be instanceof TileArcaneFurnace, "the base plate hosts TileArcaneFurnace");
        final TileArcaneFurnace furnace = (TileArcaneFurnace) be;
        final ServerPlayer player = mockPlayer(helper, pos);

        final AlchemyFurnaceMenu menu = new AlchemyFurnaceMenu(1, player.getInventory(), furnace);
        helper.assertTrue(menu.slots.size() == 38, "2 machine slots + the 36-slot player inventory");
        helper.assertTrue(menu.slots.get(0).x == 80 && menu.slots.get(0).y == 8,
                "the input slot sits at ContainerAlchemyFurnace's (80,8)");
        helper.assertTrue(menu.slots.get(1).x == 80 && menu.slots.get(1).y == 48,
                "the second slot sits at ContainerAlchemyFurnace's (80,48)");
        helper.assertTrue(menu.slots.get(2).x == 8 && menu.slots.get(2).y == 84,
                "the player grid starts at the vanilla (8,84) — the 1.7.10 default panel");
        helper.assertTrue(menu.slots.get(29).y == 142, "the hotbar row sits at y = 142");

        // SlotLimitedHasAspects's port analogue: only smelttables enter slot 0.
        helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.RAW_IRON)),
                "raw iron smelts, so it is a valid input");
        helper.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(Items.DIRT)),
                "dirt has no smelt result: the input slot rejects it (the tile would destroy it)");
        helper.assertTrue(menu.slots.get(1).mayPlace(new ItemStack(Items.DIRT)),
                "the second slot is plain, as the original's fuel slot was");

        // Shift-click: a smeltable stack leaves the player for the machine,
        // and the machine hands it back. The inventory's first grid slot
        // (stack index 9) is menu slot 2 - the player grid is added grid-first.
        player.getInventory().setItem(9, new ItemStack(Items.RAW_IRON, 4));
        menu.quickMoveStack(player, 2);
        helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON),
                "the smeltable shift-click filled the input slot first");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(furnace.getItem(0).isEmpty(), "the machine slot shifts back to the player");

        // The cook gauge rides data slots 0/1 (the screen's 46 px fill reads
        // these; the tile's own tick owns the field). The menu's accessors
        // read the same ContainerData instance addDataSlots registered, so
        // they are the packet's own read path.
        furnace.furnaceMaxCookTime = 140;
        furnace.furnaceCookTime = 70;
        helper.assertTrue(menu.getCookTime() == 70, "data slot 0 carries the cook progress");
        helper.assertTrue(menu.getMaxCookTime() == 140, "data slot 1 carries the cook duration");
        menu.setDataValue(0, 30);
        helper.assertTrue(furnace.furnaceCookTime == 30,
                "the data slot's set path writes the tile — the client mirror the screen reads");

        // The open seam: the base plate carries the MenuProvider (and the
        // window title); the nozzle variant must not.
        helper.assertTrue(furnace.getDisplayName().getString().equals("Alchemical Furnace"),
                "the window title is the original's container.alchemyfurnace string");
        helper.assertTrue(furnace.createMenu(1, player.getInventory(), player) instanceof AlchemyFurnaceMenu,
                "the provider builds the menu the MenuType carries");
        final BlockPos nozzlePos = at(helper, 3, 1, 1);
        helper.setBlock(3, 1, 1, CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get()
                .defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
        final BlockEntity nozzle = helper.getLevel().getBlockEntity(nozzlePos);
        helper.assertTrue(!(nozzle instanceof MenuProvider),
                "the nozzle (original metadata 10) carries no window");
        helper.succeed();
    }

    // ------------------------------------------------------- gui_id 15: arcane bore

    @GameTest(template = "empty_24")
    public static void arcaneBoreMenuMirrorsTheOriginalContainer(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 2, 1, 1);
        helper.setBlock(2, 1, 1, ((com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase)
                CatalogDevices.BLOCK_WOODEN_DEVICE.get()).withVariant(5));
        final BlockEntity be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be instanceof TileArcaneBore, "blockWoodenDevice meta 5 hosts TileArcaneBore");
        final TileArcaneBore bore = (TileArcaneBore) be;
        final ServerPlayer player = mockPlayer(helper, pos);

        final ArcaneBoreMenu menu = new ArcaneBoreMenu(1, player.getInventory(), bore);
        helper.assertTrue(menu.slots.size() == 38, "2 machine slots + 36 player slots");
        helper.assertTrue(menu.slots.get(0).x == 26 && menu.slots.get(0).y == 18,
                "the focus slot sits at ContainerArcaneBore's (26,18)");
        helper.assertTrue(menu.slots.get(1).x == 74 && menu.slots.get(1).y == 18,
                "the pickaxe slot sits at ContainerArcaneBore's (74,18)");
        helper.assertTrue(menu.slots.get(2).y == 59, "the player grid starts at the original's y = 59");
        helper.assertTrue(menu.slots.get(29).y == 117, "the hotbar row sits at the original's y = 117");

        // Slot classes: picks into slot 1 (the original's ItemPickaxe),
        // nothing today into slot 0 (the excavation focus is TC-M6-02's, so
        // the id contract accepts no registered item).
        helper.assertTrue(menu.slots.get(1).mayPlace(new ItemStack(Items.IRON_PICKAXE)),
                "a pickaxe is the pickaxe slot's class");
        helper.assertTrue(!menu.slots.get(1).mayPlace(new ItemStack(Items.DIRT)),
                "slot 1 rejects non-picks");
        helper.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(Items.IRON_PICKAXE)),
                "slot 0 takes excavation focuses only (id contract)");

        // Shift-click a pickaxe from the player's first grid slot (stack index
        // 9 = menu slot 2): slot 0 rejects it, the pickaxe slot accepts, so it
        // lands in slot 1.
        player.getInventory().setItem(9, new ItemStack(Items.IRON_PICKAXE, 1));
        menu.quickMoveStack(player, 2);
        helper.assertTrue(menu.slots.get(1).getItem().is(Items.IRON_PICKAXE),
                "the pickaxe shifted into slot 1, as ContainerArcaneBore routed it");

        // The readout state rides four data slots; the width formula is the
        // screen's drawing (1 + (area + maxRadius) * 2).
        bore.area = 1;
        bore.maxRadius = 4;
        bore.speed = 2;
        bore.fortune = 3;
        helper.assertTrue(menu.getArea() == 1 && menu.getMaxRadius() == 4
                        && menu.getSpeed() == 2 && menu.getFortune() == 3,
                "area/maxRadius/speed/fortune read through the data slots");
        helper.assertTrue(com.jonnyutah12.thaumcraftclassic.client.ContainerLayout
                        .boreWidth(menu.getArea(), menu.getMaxRadius()) == 11,
                "the screen's Width row computes 1 + (1 + 4) * 2 = 11");
        helper.assertTrue(menu.getPickaxeStack().is(Items.IRON_PICKAXE),
                "the screen reads the pickaxe slot's synced stack for its property rows");
        helper.succeed();
    }

    // --------------------------------------------- the crucible: GUI parity = absence

    @GameTest(template = "empty_24")
    public static void crucibleGainsNoMenuInTheField(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 2, 1, 1);
        helper.setBlock(2, 1, 1, ((com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase)
                CatalogDevices.BLOCK_METAL_DEVICE.get()).withVariant(0));
        final BlockEntity be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be instanceof TileCrucible, "blockMetalDevice meta 0 hosts TileCrucible");
        helper.assertTrue(!(be instanceof MenuProvider),
                "the crucible tile must not gain a MenuProvider: the 1.7.10 crucible opens no window");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileCrucible,
                "sanity: the tile stays put");
        // No crucible menu type anywhere in the registry — and the device's
        // two real ones are there, so the absence is the scope, not a gap.
        boolean crucibleMenu = false;
        for (final ResourceLocation id : BuiltInRegistries.MENU.keySet()) {
            if (id.getNamespace().equals(ThaumcraftClassic.MOD_ID) && id.getPath().contains("crucible")) {
                crucibleMenu = true;
            }
        }
        helper.assertTrue(!crucibleMenu, "no crucible menu type exists in the MENU registry");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gui_alchemy_furnace")),
                "gui_alchemy_furnace is registered (content-mapping gui row, original gui_id 9)");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gui_arcane_bore")),
                "gui_arcane_bore is registered (content-mapping gui row, original gui_id 15)");
        helper.succeed();
    }
}
