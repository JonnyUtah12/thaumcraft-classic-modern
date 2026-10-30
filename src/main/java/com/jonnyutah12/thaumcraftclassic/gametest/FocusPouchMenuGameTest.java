package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.foci.FocusPouchContents;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocusPouch;
import com.jonnyutah12.thaumcraftclassic.menus.FocusPouchMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M7-04-m3 acceptance probes: the focus pouch's container
 * (content-mapping.csv {@code thaumcraft:gui_focus_pouch}, original gui_id 5 /
 * {@code ContainerFocusPouch} + {@code InventoryFocusPouch}) on the live
 * registries of the game-test server.
 *
 * <p>What they prove: the menu type is registered under the csv's own id; the
 * 18-slot grid is seeded from the pouch's {@code thaumcraft:focus_pouch_contents}
 * component when the container opens (the original seeded from the stack's
 * {@code "Inventory"} NBT list); every pouch slot takes a focus and refuses a
 * stick ({@code InventoryFocusPouch.isItemValidForSlot}) and a nested pouch
 * (TC-M6-02's rule); the slots sit at the original coordinates — 6x3 at
 * {@code (37 + i%6*18, 51 + i/6*18)}, player grid at 151, hotbar at 209; the
 * slot carrying the pouch refuses the cursor, which in 1.21.1 is the one
 * predicate behind the pick-up, shift-click ({@code QUICK_MOVE}) and
 * number-key ({@code SWAP}) branches of {@code AbstractContainerMenu#clicked},
 * so it covers the original's two guards ({@code slotClick}'s number-key
 * branch and {@code transferStackInSlot}'s early return); shift-click moves a
 * focus out of the pouch and never moves a stick in; {@code removed} flushes
 * the grid back to the component (the original's {@code onContainerClosed});
 * and right-clicking the pouch opens the menu, which the server then closes on
 * the tick the pouch leaves the hotbar slot that opened it.</p>
 *
 * <p>That last rule is the deviation. The original polled the hotbar slot from
 * its <em>client</em> draw method ({@code GuiFocusPouch.func_146976_a},
 * lines 36-40) and closed its own window, so the rule was unobservable from a
 * server. Here it is {@link FocusPouchMenu#stillValid(Player)}:
 * {@code ServerPlayer#tickContainer} closes the container on the tick it
 * reports false, and closing fires the writeback, so no window outlives its
 * stack (docs/BEHAVIORAL_DEVIATIONS.md BD-18).</p>
 *
 * <p>Not proved here: pixels. The game-test server is a dedicated server — it
 * never fires {@code RegisterMenuScreensEvent}, and a {@code Screen} cannot
 * even be constructed on it (its {@code Minecraft} instance is null), so the
 * screen's wiring rides the unit-side {@code MenuTypeScreenCoverageTest} and
 * its 175x232 geometry {@code FocusPouchLayoutTest} (BD-17).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusPouchMenuGameTest {

    private FocusPouchMenuGameTest() {
    }

    /** The menu type is registered, and the grid is the original's 18 focus-only slots. */
    @GameTest(template = "empty")
    public static void focusPouchMenuGrid(final GameTestHelper helper) {
        helper.assertTrue(CatalogFoci.GUI_FOCUS_POUCH.get()
                        == BuiltInRegistries.MENU.get(ResourceLocation.parse("thaumcraft:gui_focus_pouch")),
                "the pouch's menu type must be registered as thaumcraft:gui_focus_pouch (gui_id 5)");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().items.set(0, new ItemStack(CatalogFoci.FOCUS_POUCH.get()));
        final ItemStack pouch = player.getInventory().items.get(0);
        final ItemStack[] seed = new ItemStack[ItemFocusPouch.CAPACITY];
        Arrays.fill(seed, ItemStack.EMPTY);
        seed[2] = focus();
        ItemFocusPouch.setInventory(pouch, seed);

        final FocusPouchMenu menu = new FocusPouchMenu(1, player.getInventory());

        // ContainerFocusPouch's own inventory is new ItemStack[18]; the player
        // grid is appended by bindPlayerInventory, so 18 + 27 + 9.
        helper.assertTrue(menu.slots.size() == 54,
                "18 pouch + 27 player + 9 hotbar slots, got " + menu.slots.size());
        int grid = 0;
        for (final Slot slot : menu.slots) {
            if (slot.x >= 37 && slot.x <= 127 && slot.y >= 51 && slot.y <= 87) {
                grid++;
            }
        }
        helper.assertTrue(grid == 18,
                "18 slots fill the original's 6x3 grid box (37,51)..(127,87), got " + grid);

        // InventoryFocusPouch was seeded from the pouch's stored inventory.
        helper.assertTrue(ItemStack.isSameItem(menu.getSlot(2).getItem(),
                        focus()),
                "pouch slot 2 is seeded from thaumcraft:focus_pouch_contents");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "an empty pouch slot stays empty");

        // isItemValidForSlot: ItemFocusBasic only — TC-M6-02's isValidFocus.
        helper.assertTrue(menu.getSlot(0).mayPlace(focus()),
                "a focus is accepted into a pouch slot");
        helper.assertTrue(!menu.getSlot(0).mayPlace(pouch),
                "a nested pouch is refused by the same predicate TC-M6-02 shipped");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)),
                "a stick is refused, as InventoryFocusPouch.isItemValidForSlot refused it");

        // The original's own coordinates (ContainerFocusPouch.java:31-33, 37-47).
        helper.assertTrue(menu.getSlot(5).x == 127 && menu.getSlot(5).y == 51,
                "slot 5 closes row 1 at x = 37 + 5*18 = 127, got "
                        + menu.getSlot(5).x + "," + menu.getSlot(5).y);
        helper.assertTrue(menu.getSlot(6).x == 37 && menu.getSlot(6).y == 69,
                "slot 6 starts row 2 at y = 51 + 18 = 69");
        helper.assertTrue(menu.getSlot(17).x == 127 && menu.getSlot(17).y == 87,
                "slot 17 closes the 6x3 grid at (127, 87)");
        helper.assertTrue(menu.getSlot(18).x == 8 && menu.getSlot(18).y == 151,
                "the player grid starts at (8, 151) — the pouch's own row, not the 84 the 166 px"
                        + " panels use, because GuiFocusPouch's panel is 232 px tall");
        helper.assertTrue(menu.getSlot(45).x == 8 && menu.getSlot(45).y == 209,
                "the hotbar row is at y = 209 (bindPlayerInventory's third loop)");
        helper.succeed();
    }

    /** The carrier-slot fence, the shift-click legs, the writeback and the close rule. */
    @GameTest(template = "empty")
    public static void focusPouchCarrierFenceAndWriteback(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().items.set(0, new ItemStack(CatalogFoci.FOCUS_POUCH.get()));
        final ItemStack pouch = player.getInventory().items.get(0);

        final FocusPouchMenu menu = new FocusPouchMenu(1, player.getInventory());
        final int carrier = menu.carrierSlotIndex();
        helper.assertTrue(carrier == 45,
                "the pouch is hotbar slot 0, i.e. container slot currentItem + 45 — the original's"
                        + " blockSlot (ContainerFocusPouch.slotClick, lines 100-106)");

        // The fence. One predicate covers the original's two guards: 1.21.1's
        // AbstractContainerMenu#clicked gates pick-up, QUICK_MOVE (shift-click)
        // and SWAP (number-key) on Slot#mayPickup.
        helper.assertTrue(!menu.getSlot(carrier).mayPickup(player),
                "the slot carrying the pouch refuses the cursor");
        helper.assertTrue(menu.getSlot(46).mayPickup(player),
                "the neighbouring hotbar slot stays draggable — the fence is one slot, not the row");
        helper.assertTrue(menu.getSlot(18).mayPickup(player),
                "the player grid stays draggable");
        helper.assertTrue(menu.quickMoveStack(player, carrier).isEmpty(),
                "shift-clicking the carrier slot moves nothing (transferStackInSlot returns null"
                        + " for blockSlot)");
        helper.assertTrue(ItemStack.isSameItem(pouch, player.getInventory().items.get(0)),
                "and the pouch is still in the slot afterwards");

        // transferStackInSlot's first leg: a pouch slot merges into the player region.
        menu.getSlot(4).set(focus());
        final ItemStack movedOut = menu.quickMoveStack(player, 4);
        helper.assertTrue(!movedOut.isEmpty() && movedOut.getItem() instanceof ItemFocus,
                "a focus shift-clicks out of the pouch into the player region");
        helper.assertTrue(menu.getSlot(4).getItem().isEmpty(), "and leaves the pouch slot empty");
        helper.assertTrue(contains(player.getInventory().items, focus()),
                "the focus lands in the player inventory");

        // The player-side legs, with the pouch leg refusing a non-focus: a hotbar
        // stack shift-clicks into the main inventory (the original's third leg) and
        // the pouch leg (0..18) rejects it, so it never enters a pouch slot —
        // isItemValidForSlot is what moveItemStackTo consults.
        player.getInventory().items.set(1, new ItemStack(Items.STICK));
        final int stickContainer = FocusPouchMenu.PLAYER_HOTBAR_START + 1;
        final ItemStack cycled = menu.quickMoveStack(player, stickContainer);
        helper.assertTrue(!cycled.isEmpty(),
                "a hotbar slot shift-click moves the stack (the original's third leg)");
        helper.assertTrue(allPouchSlotsEmpty(menu),
                "and it never lands in a pouch slot — isItemValidForSlot gates moveItemStackTo");
        helper.assertTrue(menu.getSlot(stickContainer).getItem().isEmpty(),
                "and the hotbar slot it left is empty");
        final int landed = firstIndex(player.getInventory().items, new ItemStack(Items.STICK));
        helper.assertTrue(landed >= 9,
                "the stick landed in the main inventory, got player slot " + landed);

        // A stick seeded straight into a pouch slot (Slot#set bypasses the filter,
        // as the original's NBT load bypassed it) shift-clicks back out.
        menu.getSlot(1).set(new ItemStack(Items.STICK));
        helper.assertTrue(!menu.quickMoveStack(player, 1).isEmpty(),
                "a pouch slot merges into the player region for whatever it holds");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "and leaves the pouch slot empty");

        // onContainerClosed (ContainerFocusPouch.java:108-121): the grid goes
        // back through ItemFocusPouch.setInventory, server side only.
        menu.getSlot(7).set(focus());
        menu.removed(player);
        final ItemStack[] stored = ItemFocusPouch.getInventory(pouch);
        helper.assertTrue(stored.length == FocusPouchContents.CAPACITY,
                "the component keeps the original's fixed 18-slot ItemStack[] shape");
        helper.assertTrue(ItemStack.isSameItem(stored[7], focus()),
                "removed() flushes the grid back to the pouch stack");
        helper.assertTrue(stored[4].isEmpty(),
                "a focus shift-clicked out of the pouch is not restored to it");

        // GuiFocusPouch.func_146976_a closed the window when that hotbar slot
        // went empty. Here it is the menu's validity (BD-18).
        helper.assertTrue(menu.stillValid(player),
                "the menu is valid while the pouch is the stack in its carrier slot");
        player.getInventory().items.set(0, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player),
                "the menu reports invalid once the pouch leaves the slot, which is what"
                        + " ServerPlayer#tickContainer closes the container on");
        helper.succeed();
    }

    /** Right-click opens the menu (the original's {@code openGui(5)}); the server closes it. */
    @GameTest(template = "empty")
    public static void focusPouchOpensFromRightClickAndServerClosesIt(final GameTestHelper helper) {
        final ServerPlayer player = connectedMockPlayer(helper, "tc-pouch-gui");
        player.getInventory().items.set(0, new ItemStack(CatalogFoci.FOCUS_POUCH.get()));
        final ItemStack pouch = player.getInventory().items.get(0);

        // ItemFocusPouch.onItemRightClick: server side, openGui(player, 5, x, y, z).
        // The position arguments were pure 1.7.10 GUI-channel plumbing —
        // ContainerFocusPouch reads the pouch off getCurrentItem() — so the port
        // needs no payload. The probe drives ItemStack#use(Level, Player, hand) —
        // 1.21.1's public front for the right-click path, which forwards to the
        // item's own use (Item#use itself is protected).
        final InteractionResultHolder<ItemStack> used =
                pouch.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used.getResult() == InteractionResult.CONSUME,
                "right-clicking the pouch consumes the action (the original opened its GUI), got "
                        + used.getResult());
        helper.assertTrue(player.containerMenu instanceof FocusPouchMenu,
                "right-click opens the pouch menu, got " + player.containerMenu.getClass().getSimpleName());
        final FocusPouchMenu menu = (FocusPouchMenu) player.containerMenu;
        helper.assertTrue(menu.carrierSlotIndex() == 45,
                "the opened menu fences the hotbar slot the pouch sat in");
        helper.assertTrue(menu.slots.size() == 54, "the opened menu is bound to the live player");
        helper.assertTrue(menu.pouch() == pouch, "the menu holds the pouch stack itself");

        // The grid is live: a slot filled here reaches the component only on
        // close, exactly as the original wrote its InventoryFocusPouch back in
        // onContainerClosed.
        menu.getSlot(3).set(focus());
        helper.assertTrue(ItemFocusPouch.getInventory(pouch)[3].isEmpty(),
                "an open menu keeps its grid to itself — the component is not rewritten per slot");

        // The close rule, seen from the server: the pouch leaves the slot,
        // stillValid reports false, the container closes and the writeback lands.
        player.getInventory().items.set(0, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player),
                "the menu goes invalid the tick the pouch leaves the hotbar slot");
        player.closeContainer();
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                "and the server-side close returns the player to their own inventory");
        helper.assertTrue(ItemStack.isSameItem(ItemFocusPouch.getInventory(pouch)[3],
                        focus()),
                "the close wrote the grid back to the pouch (BD-18: no window outlives its stack)");
        helper.succeed();
    }

    /** A real focus item — the fire focus, the id the TC-M6-02 probes use. */
    private static ItemStack focus() {
        return new ItemStack(CatalogFoci.item("fire").get());
    }

    /** The first index of a matching stack in the player's item list, or -1. */
    private static int firstIndex(final List<ItemStack> items, final ItemStack want) {
        for (int i = 0; i < items.size(); i++) {
            if (ItemStack.isSameItem(items.get(i), want)) {
                return i;
            }
        }
        return -1;
    }

    /** True if the list holds a matching stack. */
    private static boolean contains(final List<ItemStack> items, final ItemStack want) {
        return firstIndex(items, want) >= 0;
    }

    /** True while every pouch slot is empty. */
    private static boolean allPouchSlotsEmpty(final FocusPouchMenu menu) {
        for (int i = 0; i < FocusPouchMenu.POUCH_SLOTS; i++) {
            if (!menu.getSlot(i).getItem().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * A {@link ServerPlayer} with a packet-collecting connection: opening a
     * menu sends {@code ClientboundOpenScreenPacket} plus the full slot state,
     * so {@code openMenu} needs a {@code connection}, which
     * {@code makeMockPlayer} does not provide. The same shape as
     * {@code AccessoryGameTest.connectedMockPlayer} — packets are queued on an
     * {@link EmbeddedChannel}, never written.
     */
    private static ServerPlayer connectedMockPlayer(final GameTestHelper helper, final String name) {
        final var level = helper.getLevel();
        final var server = level.getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        final ServerPlayer player =
                new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        ChannelAttributes.getOrCreateAdHocChannels(connection).add(SyncAttachmentsPayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
