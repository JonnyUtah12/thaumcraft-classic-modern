package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogPech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M7-04-m3-b probes: the pech trade window (gui_id 1 /
 * {@code thaumcraft:gui_pech}). The menu is a live menu type with the original's
 * 5-slot layout at the original's coordinates, the {@code SlotOutput} insertion
 * rule, the dice predicate from {@code GuiPech.java:38-39} synced over a
 * data slot, the original's shift-click ranges, the close-drop stamped with the
 * {@code "PechDrop"} owner, the tamed-only open gate and validity, and — for the
 * unported loot-table trade roll — silence: no invented contents (BD-73).
 *
 * <p>What these probes do <b>not</b> claim: rendered pixels (the game-test
 * server is a dedicated server and never fires {@code RegisterMenuScreensEvent}
 * — the 175x232 window, the (67,24) 25x25 dice rect and the geometry are pinned
 * by {@code PechLayoutTest} + {@code PechGuiParityTest}), and no trade roll
 * (the {@code ChestGenHooks} engine is D-007 / BD-73).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PechMenuGameTest {

    private PechMenuGameTest() {
    }

    private static EntityPech tamedPech(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final EntityPech pech = CatalogPech.PECH.get().create(level);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        pech.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(pech);                                                          // the mob lane's idiom
        pech.setTamed(true);
        return pech;
    }

    private static Player fakePlayer(final GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.CREATIVE);                                       // the golem lane's idiom
    }

    /** The menu type is {@code thaumcraft:gui_pech} (the content-mapping gui path, D-001). */
    @GameTest(template = "empty")
    public static void pechMenuTypeIsRegistered(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gui_pech");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(id),
                "thaumcraft:gui_pech must be registered (content-mapping.csv Thaumcraft:GUI_PECH)");
        helper.assertTrue(BuiltInRegistries.MENU.get(id) == CatalogPech.GUI_PECH.get(),
                "thaumcraft:gui_pech resolves to the catalog holder");
        try {
            PechMenu.class.getConstructor(int.class, Inventory.class, RegistryFriendlyByteBuf.class);
        } catch (final NoSuchMethodException e) {
            helper.fail("the client (int, Inventory, RegistryFriendlyByteBuf) constructor is required: " + e);
        }
        helper.succeed();
    }

    /** {@code ContainerPech.java:33,37,43,48} — 41 slots at the original coordinates. */
    @GameTest(template = "empty")
    public static void pechSlotsSitAtTheOriginalCoordinates(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(menu.slots.size() == 41, "5 pech slots + 27 + 9 = 41, got " + menu.slots.size());
        assertSlotAt(helper, menu.slots.get(0), ContainerLayout.PECH_OFFER_X, ContainerLayout.PECH_OFFER_Y);      // :33
        assertSlotAt(helper, menu.slots.get(1), 106, 20);                                                          // :37
        assertSlotAt(helper, menu.slots.get(2), 124, 20);
        assertSlotAt(helper, menu.slots.get(3), 106, 38);
        assertSlotAt(helper, menu.slots.get(4), 124, 38);
        assertSlotAt(helper, menu.slots.get(5), 8, 84);                                                            // :43
        assertSlotAt(helper, menu.slots.get(31), 152, 120);
        assertSlotAt(helper, menu.slots.get(32), 8, 142);                                                          // :48
        assertSlotAt(helper, menu.slots.get(40), 152, 142);
        helper.succeed();
    }

    /** {@code SlotOutput.java} — the 2x2 grid refuses insertion; the offer accepts it. */
    @GameTest(template = "empty")
    public static void pechOutputGridRefusesInsertion(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.DIAMOND)),
                "the offer slot accepts items (ContainerPech.java:33)");
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(!menu.slots.get(a).mayPlace(new ItemStack(Items.DIAMOND)),
                    "output slot " + a + " refuses insertion (SlotOutput insertion rule)");
        }
        helper.succeed();
    }

    /** {@code GuiPech.java:38-39} — the dice predicate, and its data-slot sync. */
    @GameTest(template = "empty")
    public static void pechDicePredicateFollowsTheOriginal(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(!menu.diceReady(), "an empty window shows no dice");
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND));
        helper.assertTrue(menu.diceReady(), "a valued offer with an empty grid lights the dice (GuiPech.java:38-39)");
        helper.assertTrue(menu.diceDataValue() == 1, "the data slot carries the bit");
        menu.slots.get(1).set(new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(!menu.diceReady(), "a full output cell hides the dice");
        helper.assertTrue(menu.diceDataValue() == 0, "the data slot follows");
        menu.slots.get(1).set(ItemStack.EMPTY);
        menu.slots.get(0).set(new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(!menu.diceReady(), "an unvalued offer shows no dice (EntityPech.java:707-722)");
        helper.succeed();
    }

    /** {@code ContainerPech.java:54-64} — 0..4 to the player region, 5..40 to the offer only. */
    @GameTest(template = "empty")
    public static void pechShiftClickUsesTheOriginalRanges(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, 3));
        menu.quickMoveStack(player, 0);
        helper.assertTrue(menu.getItems().get(0).isEmpty(), "the offer shifts out of the pech slots (:54-58)");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.DIAMOND, 3)),
                "the offer lands in the player region");
        player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 2));
        menu.quickMoveStack(player, 5);                                                  // menu slot 5 = inv slot 9
        helper.assertTrue(ItemStack.isSameItem(menu.getItems().get(0), new ItemStack(Items.EMERALD, 2)),
                "a player slot shifts into the offer slot (:59-64)");
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(menu.getItems().get(a).isEmpty(), "the output grid is never a shift destination");
        }
        helper.succeed();
    }

    /** {@code ContainerPech.java:199-205} — close drops slots 0..4 owned by {@code "PechDrop"}. */
    @GameTest(template = "empty")
    public static void pechCloseDropsStampedWithPechDrop(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        pech.trading = true;
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        for (int a = 0; a < 5; a++) {
            menu.slots.get(a).set(new ItemStack(Items.DIAMOND, a + 1));
        }
        menu.removed(player);
        helper.assertTrue(!pech.trading, "closing marks the pech not trading (:204)");
        // The drop lands at the DROPPING player - a mock player is not parked in the test
        // cell (take-2 measured 0 hits for a test-cell box), so search around the player.
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(8.0));
        int stamped = 0;
        for (final ItemEntity drop : drops) {
            final var data = drop.getItem().get(DataComponents.ENTITY_DATA);
            if (data == null) {
                continue;
            }
            final CompoundTag tag = data.copyTag();
            if (tag.contains("PechDropOwner", net.minecraft.nbt.Tag.TAG_STRING)
                    && "PechDrop".equals(tag.getString("PechDropOwner"))) {
                stamped++;
            }
        }
        helper.assertTrue(stamped == 5, "all five slots drop with the PechDrop owner (:201), got " + stamped);
        helper.succeed();
    }

    /** {@code EntityPech.java:695-698} — the open gate: server-side, tamed, not sneaking, no name tag. */
    @GameTest(template = "empty")
    public static void pechOpensOnlyUnderTheOriginalGate(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final EntityPech wild = tamedPech(helper);
        wild.setTamed(false);
        helper.assertTrue(!wild.openTradeGui(player), "an untamed pech opens no window (:696)");
        final EntityPech pech = tamedPech(helper);
        player.setShiftKeyDown(true);
        helper.assertTrue(!pech.openTradeGui(player), "a sneaking player opens no window (:695)");
        player.setShiftKeyDown(false);
        player.getInventory().setItem(0, new ItemStack(Items.NAME_TAG));
        helper.assertTrue(!pech.openTradeGui(player), "the name-tag stand-in opens no window (:695, BD-73)");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(pech.openTradeGui(player), "a tamed pech opens the window (:697)");
        // NOT asserted: the opened container on the mock player. The golem lane measured this
        // harness wall (harness note at the bottom of GolemMenuGameTest.java): the patched
        // ServerPlayer#openMenu payload path against the mock player's unnegotiated
        // EmbeddedChannel is not a provable leg in both arms; the gate booleans above are.
        helper.succeed();
    }

    /** {@code ContainerPech.java:245-247} — the window lives while the pech is tamed. */
    @GameTest(template = "empty")
    public static void pechMenuValidatesTamedOnly(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        helper.assertTrue(menu.stillValid(player), "a tamed pech keeps the window (:245-247)");
        pech.setTamed(false);
        helper.assertTrue(!menu.stillValid(player), "an untamed pech closes it");
        helper.succeed();
    }

    /** BD-73 — the dice click invents nothing: no roll, no contents, no consumption. */
    @GameTest(template = "empty")
    public static void pechDiceClickInventsNothing(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, 4));
        helper.assertTrue(!menu.clickMenuButton(player, PechMenu.DICE_BUTTON),
                "the roll is the unported ChestGenHooks engine — no button is accepted (BD-73)");
        helper.assertTrue(menu.getItems().get(0).getCount() == 4, "the offer is not consumed");
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(menu.getItems().get(a).isEmpty(), "no contents are invented into the output grid");
        }
        helper.succeed();
    }

    private static void assertSlotAt(final GameTestHelper helper, final Slot slot, final int x, final int y) {
        helper.assertTrue(slot.x == x && slot.y == y,
                "slot at (" + slot.x + "," + slot.y + ") expected (" + x + "," + y + ")");
    }
}
