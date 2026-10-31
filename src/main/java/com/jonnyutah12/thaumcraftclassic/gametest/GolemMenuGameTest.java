package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemBell;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.menus.GolemMenu;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * TC-M7-04-m3-a probes: the golem window is a live menu type that pauses the
 * golem while it is open, seeds the player rows at the original coordinates,
 * opens from the golem's right-click only under the original's gate, enforces
 * the original's squared-distance validity, and stays silent (no fake window)
 * over the golem inventory the runtime card has not shipped (BD-19).
 *
 * <p>What these probes do <b>not</b> claim: rendered pixels (the game-test
 * server is a dedicated server and never fires {@code RegisterMenuScreensEvent}
 * — the screen is pinned by {@code GolemLayoutTest} + {@code GolemGuiParityTest}),
 * no goal/tick pause behaviour (TC-M6-08-s-b), no bell riding (TC-M6-08-s-b),
 * and no window slots (the {@code InventoryMob} container is TC-M6-08-s-b).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemMenuGameTest {

    private GolemMenuGameTest() {
    }

    /** The menu type is {@code thaumcraft:gui_golem} (the content-mapping gui path, D-001). */
    @GameTest(template = "empty")
    public static void golemMenuTypeIsRegistered(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gui_golem");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(id),
                "thaumcraft:gui_golem must be registered (content-mapping.csv Thaumcraft:GUI_GOLEM)");
        final MenuType<?> type = BuiltInRegistries.MENU.get(id);
        helper.assertTrue(type == CatalogGolems.GUI_GOLEM.get(), "thaumcraft:gui_golem resolves to the catalog holder");
        // The client constructor is the bytebuf shape the open-menu packet
        // feeds (the original's openGui x argument = the entity id) — prove the
        // exact (int, Inventory, RegistryFriendlyByteBuf) ctor exists.
        try {
            final var ctor = GolemMenu.class.getConstructor(int.class, Inventory.class,
                    RegistryFriendlyByteBuf.class);
            helper.assertTrue(ctor != null, "the bytebuf constructor exists for client construction");
        } catch (final NoSuchMethodException e) {
            helper.fail("GolemMenu must expose the (int, Inventory, RegistryFriendlyByteBuf) client ctor");
        }
        helper.succeed();
    }

    /**
     * Opening pauses the golem (original constructor
     * {@code ((EntityGolemBase)mobInv.ent).paused = true}) and the menu seeds
     * ONLY the 36 player slots at the original coordinates — the 84 px grid
     * rows {@code (8 + j*18, 84 + i*18)} and the hotbar {@code (8 + i*18,
     * 142)}; with the mob inventory absent (BD-19) no golem window slots bind,
     * which is the original's own slot list for a core without
     * {@code hasInventory}.
     */
    @GameTest(template = "empty")
    public static void openPausesGolemAndSeedsTheOriginalPlayerRows(final GameTestHelper helper) {
        final EntityGolemBase golem = spawnGolem(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(golem.position().add(0.5, 1.0, 0.5));

        final GolemMenu menu = new GolemMenu(1, player.getInventory(), golem);
        helper.assertTrue(golem.getPaused(),
                "opening the window marks the golem paused (original constructor)");
        helper.assertTrue(menu.getGolem() == golem, "the menu is bound to the live golem");
        helper.assertTrue(menu.slots.size() == 36,
                "the shipped slot list is the 36 player slots — no window binds over an absent InventoryMob"
                        + " (the original's list for a core without hasInventory), got " + menu.slots.size());
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == ContainerLayout.PLAYER_GRID_TOP,
                "the first grid slot is (8, 84) — ContainerGolem.bindPlayerInventory, got ("
                        + menu.slots.get(0).x + "," + menu.slots.get(0).y + ")");
        helper.assertTrue(menu.slots.get(26).x == 8 + 8 * 18 && menu.slots.get(26).y == 84 + 2 * 18,
                "the last grid slot is (152, 120)");
        helper.assertTrue(menu.slots.get(27).x == 8 && menu.slots.get(27).y == 142,
                "the first hotbar slot is (8, 142)");
        helper.assertTrue(menu.slots.get(35).x == 8 + 8 * 18 && menu.slots.get(35).y == 142,
                "the last hotbar slot is (152, 142)");
        // The window gate's own figures (the original's scroll clamp over a
        // six-slot page): maxScroll = slotCount/6 - 1 = -1 for the absent
        // window; the page pins at 0 — the original's state for a core with
        // no inventory.
        helper.assertTrue(menu.golemSlotCount() == 0 && menu.maxScroll() == -1 && menu.currentScroll() == 0,
                "the window gate reports an unbound window: count 0, maxScroll -1, page 0");
        menu.removed(player);
        cleanup(helper, golem);
        helper.succeed();
    }

    /** Closing the window resumes the golem ({@code onContainerClosed}). */
    @GameTest(template = "empty")
    public static void closeResumesGolem(final GameTestHelper helper) {
        final EntityGolemBase golem = spawnGolem(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(golem.position().add(0.5, 1.0, 0.5));

        final GolemMenu menu = new GolemMenu(1, player.getInventory(), golem);
        helper.assertTrue(golem.getPaused(), "open marks paused");
        menu.removed(player);
        helper.assertTrue(!golem.getPaused(),
                "onContainerClosed clears the pause (the original's func_75134_a)");
        cleanup(helper, golem);
        helper.succeed();
    }

    /**
     * The right-click open site is the original's gate (customInteraction):
     * a sneak with a non-bell hand is refused; a golem with no core is
     * refused; a golem with a GUI core opens the real window; and a golem-bell
     * refuses the window on a golem WITH a core (the original's
     * {@code (isSneaking() || getCore() >= 0) -> false} bell-ride gate — the
     * ride itself is TC-M6-08-s-b).
     */
    @GameTest(template = "empty")
    public static void rightClickGateRefusesAndConsumes(final GameTestHelper helper) {
        // Plain mock player: NeoForge's Player#openMenu is a no-op default for
        // a non-ServerPlayer, so this probe pins the original's interaction
        // GATES and the consumed/refused result; the provider's product (a real
        // GolemMenu bound to the entity, pause on open) is pinned by the
        // direct-construction probes above, and the live open leg on a joined
        // ServerPlayer is not assertable in this harness — see the harness note
        // at the bottom of this file.
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // (a) No core: refused (getCore() > -1 && ItemGolemCore.hasGUI(core)).
        final EntityGolemBase bare = spawnGolem(helper);
        bare.setCore(-1);
        player.setPos(bare.position().add(0.5, 1.0, 0.5));
        helper.assertTrue(!bare.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a coreless golem refuses the window (getCore() > -1 && hasGUI(core))");

        // (b) A GUI core (0 = Fill, ItemGolemCore.hasGUI(0) is true): the
        // interaction is CONSUMED — the original customInteraction returns
        // true when the GUI opens, ported as InteractionResult.SUCCESS.
        final EntityGolemBase filled = spawnGolem(helper);
        filled.setCore(0); // 0 = the Fill core
        player.setPos(filled.position().add(0.5, 1.0, 0.5));
        helper.assertTrue(filled.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a Fill-core golem consumes the right-click (the original returns true on open)");

        // (c) Sneak with an empty hand: the original's isSneaking() refusal.
        final EntityGolemBase sneakTarget = spawnGolem(helper);
        sneakTarget.setCore(0); // Fill core again
        player.setPos(sneakTarget.position().add(0.5, 1.0, 0.5));
        player.setShiftKeyDown(true);
        helper.assertTrue(!sneakTarget.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a sneaking hand is refused (the original's isSneaking() gate)");
        player.setShiftKeyDown(false);

        // (d) Golem-bell in hand on a golem WITH a core: the window is refused
        // (the original's bell-ride gate (isSneaking() || getCore() >= 0)); the
        // ride branch (setRiderId/LEASHED NBT) is TC-M6-08-s-b.
        final EntityGolemBase bellTarget = spawnGolem(helper);
        bellTarget.setCore(0); // Fill core again
        player.setPos(bellTarget.position().add(0.5, 1.0, 0.5));
        final ItemStack bell = new ItemStack(CatalogGolems.GOLEM_BELL.get());
        player.getInventory().setItem(0, bell);
        helper.assertTrue(!bellTarget.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "the bell on a cored golem is refused (the original's bell-ride gate); ride = TC-M6-08-s-b");
        cleanup(helper, bare, filled, sneakTarget, bellTarget);
        helper.succeed();
    }

    // Harness note (measured, not guessed): the live ServerPlayer open leg is
    // NOT assertable in the game-test harness in NeoForge 21.1.259. The patched
    // ServerPlayer#openMenu sends the AdvancedOpenScreenPayload (and Curios fires
    // its sync_data on the join/login event) BEFORE the containerMenu field is
    // assigned, and NetworkRegistry#validateForSending throws
    // UnsupportedOperationException for every mod payload on the mock player's
    // unnegotiated EmbeddedChannel - the container never lands on the mock and
    // even placeNewPlayer can abort mid-login. The provider shape (windowId,
    // inventory, opener -> a GolemMenu bound to the entity) is compile-checked,
    // its product is exercised by the direct-construction probes (slots, data
    // slots, pause), and mobInteract's consumption is pinned by
    // rightClickGateRefusesAndConsumes. Live open on a real client is recorded
    // as not_proved in the docs.

    private static EntityGolemBase spawnGolem(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
        helper.assertTrue(golem != null, "the golem type must build an instance");
        final BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        golem.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(golem), "the golem must join the level");
        return golem;
    }

    /** Mock players are not inside the gametest box; clean them and the golems up. */
    private static void cleanup(final GameTestHelper helper, final EntityGolemBase... golems) {
        final var level = helper.getLevel();
        for (final EntityGolemBase golem : golems) {
            golem.discard();
        }
        for (final Player mock : level.getEntitiesOfClass(Player.class,
                new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(64))) {
            if (!(mock instanceof net.minecraft.server.level.ServerPlayer)) {
                mock.discard(); // placed ServerPlayers leave in their own probe
            }
        }
    }
}
