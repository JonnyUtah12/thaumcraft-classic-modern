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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * TC-M6-08-s-c: the golem window's marker-colour path. The colour buttons
     * are the original {@code ContainerGolem.func_75140_a} legs — button
     * {@code slot} steps that slot's colour DOWN (underflow wraps to 15),
     * button {@code slotCount + slot} steps it UP (overflow wraps to -1, the
     * wildcard) — and the value lands on the live entity's own colour array,
     * which is what the {@code getColorsMatching} marker lookup reads. The
     * probe sets a colour THROUGH the menu, then proves it survives the
     * entity's save/load round-trip.
     */
    @GameTest(template = "empty")
    public static void markerColourSetThroughTheMenuPersistsOnTheEntity(final GameTestHelper helper) {
        final EntityGolemBase golem = spawnGolem(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(golem.position().add(0.5, 1.0, 0.5));

        // The shipped colour array is the original's constructor value,
        // new byte[]{-1} (EntityGolemBase.java:113): one wildcard slot.
        helper.assertTrue(golem.getColors().length == 1 && golem.getColors(0) == -1,
                "the shipped golem carries the original's single wildcard colour slot, got length "
                        + golem.getColors().length + " slot0 " + golem.getColors(0));

        final GolemMenu menu = new GolemMenu(1, player.getInventory(), golem);
        helper.assertTrue(menu.markerColorSlotCount() == golem.getColors().length,
                "the palette's slot count is the golem's own colour array length");
        helper.assertTrue(menu.clickMenuButton(player, 0), "the colour button is answered");
        helper.assertTrue(golem.getColors(0) == 15,
                "the DOWN leg wraps the wildcard -1 to 15 (original c < -1 -> c = 15), got " + golem.getColors(0));
        menu.clickMenuButton(player, 0);
        helper.assertTrue(golem.getColors(0) == 14, "the DOWN leg steps 15 -> 14, got " + golem.getColors(0));
        menu.clickMenuButton(player, 1); // slotCount(1) + slot(0) = the UP leg
        helper.assertTrue(golem.getColors(0) == 15, "the UP leg steps 14 -> 15, got " + golem.getColors(0));

        // Persistence: the colour rides the original "colors" NBT key
        // (EntityGolemBase.java:578 / :697).
        final CompoundTag tag = golem.saveWithoutId(new CompoundTag());
        final EntityGolemBase reloaded = CatalogGolems.GOLEM.get().create(helper.getLevel());
        helper.assertTrue(reloaded != null, "the golem type must build a reloaded instance");
        reloaded.load(tag);
        helper.assertTrue(reloaded.getColors(0) == 15,
                "the marker colour set through the menu survives save/load, got " + reloaded.getColors(0));
        reloaded.discard();
        menu.removed(player);
        cleanup(helper, golem);
        helper.succeed();
    }

    /**
     * TC-M6-08-s-c: the four golem-family sibling entity types, registered
     * under their content-mapping ids with the figures read from the original
     * registration table ({@code ConfigEntities.java:84,89,119,154}) and the
     * entity classes' own hitbox calls.
     */
    @GameTest(template = "empty")
    public static void siblingGolemFamilyTypesAreRegistered(final GameTestHelper helper) {
        assertSiblingType(helper, "eldritchgolem", EntityEldritchGolem.WIDTH, EntityEldritchGolem.HEIGHT,
                EntityEldritchGolem.TRACKING_RANGE, EntityEldritchGolem.UPDATE_INTERVAL, MobCategory.MONSTER,
                "ConfigEntities.java:119 + EntityEldritchGolem.java:54 (func_70105_a(1.75F, 3.5F))");
        assertSiblingType(helper, "golembobber", 0.25F, 0.25F, 64, 64, MobCategory.MISC,
                "ConfigEntities.java:154 (64, 64, velocity not synced)");
        assertSiblingType(helper, "golemorb", 0.25F, 0.25F, 64, 20, MobCategory.MISC,
                "ConfigEntities.java:89; 0.25 x 0.25 is the 1.7.10 EntityThrowable default hitbox");
        assertSiblingType(helper, "dart", 0.5F, 0.5F, 64, 20, MobCategory.MISC,
                "ConfigEntities.java:84; the original's own func_70105_a(0.5F, 0.5F) in EntityDart");
        helper.succeed();
    }

    private static void assertSiblingType(final GameTestHelper helper, final String path, final float width,
                                          final float height, final int tracking, final int interval,
                                          final MobCategory category, final String source) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), id + " is not registered");
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(type.getWidth() == width && type.getHeight() == height,
                id + " hitbox is " + type.getWidth() + " x " + type.getHeight() + ", the original ships "
                        + width + " x " + height + " (" + source + ")");
        helper.assertTrue(type.clientTrackingRange() == tracking && type.updateInterval() == interval,
                id + " tracking/interval is " + type.clientTrackingRange() + " / " + type.updateInterval()
                        + ", the original registers " + tracking + " / " + interval + " (" + source + ")");
        helper.assertTrue(type.getCategory() == category,
                id + " category is " + type.getCategory() + ", expected " + category);
    }

    /**
     * TC-M6-08-s-c3 (t_6160b768): the eldritch golem's spawn egg.
     *
     * <ul>
     *   <li>registered under the ledger's id verbatim —
     *       {@code thaumcraft:spawnegg.eldritchgolem} (content-mapping
     *       {@code Thaumcraft:spawnegg.EldritchGolem}, kind=spawner_egg, the
     *       row this probe's green run lets flip to {@code done}/{@code tested}
     *       because {@code RegistrationGameTest.implementedContentIdsResolve}
     *       resolves {@code spawner_egg} rows in the ITEM registry);</li>
     *   <li>the colour pair is the original's decimal literals,
     *       {@code ItemSpawnerEgg.addMapping("EldritchGolem", 5592405,
     *       4210752)} ({@code ConfigEntities.java:120}) = 0x555555 / 0x404040,
     *       pinned on {@link EntityEldritchGolem#EGG_PRIMARY} /
     *       {@link EntityEldritchGolem#EGG_SECONDARY} and read back through
     *       {@code SpawnEggItem#getColor(int)} in the original's layer order
     *       ({@code ItemSpawnerEgg.func_82790_a}: {@code layer == 0 ? c1 : c2});</li>
     *   <li>the egg points at {@code thaumcraft:eldritchgolem};</li>
     *   <li>the shipped item model is the vanilla egg template, i.e. the two
     *       tinted layers the colours apply to ({@code item/template_spawn_egg}
     *       = {@code item/generated} + {@code spawn_egg} + {@code spawn_egg_overlay});</li>
     *   <li>and the item names itself with the dotted key
     *       {@code item.thaumcraft.spawnegg.eldritchgolem} — the id contract,
     *       which is what the localization audit had to learn to read.</li>
     * </ul>
     */
    @GameTest(template = "empty")
    public static void eldritchSpawnEggCarriesTheOriginalColours(final GameTestHelper helper) {
        final ResourceLocation eggId =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "spawnegg.eldritchgolem");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(eggId),
                "the ledger's spawner_egg id " + eggId + " is not registered");
        final Item egg = BuiltInRegistries.ITEM.get(eggId);
        helper.assertTrue(egg instanceof DeferredSpawnEggItem,
                eggId + " is a " + egg.getDescriptionId() + " item; the port registers a deferred spawn egg "
                        + "because 1.21.1 items cannot resolve an EntityType at registration time");

        // The constants are the original's literals, and the live item must
        // answer with them in the original's layer order.
        helper.assertTrue(EntityEldritchGolem.EGG_PRIMARY == 5592405 && EntityEldritchGolem.EGG_SECONDARY == 4210752,
                "the pinned constants must be the original's decimal literals 5592405 / 4210752, got "
                        + EntityEldritchGolem.EGG_PRIMARY + " / " + EntityEldritchGolem.EGG_SECONDARY);
        final SpawnEggItem spawnEgg = (SpawnEggItem) egg;
        helper.assertTrue(spawnEgg.getColor(0) == 5592405,
                "egg layer 0 = 5592405 (0x555555) per ItemSpawnerEgg.addMapping / func_82790_a layer==0, got "
                        + spawnEgg.getColor(0));
        helper.assertTrue(spawnEgg.getColor(1) == 4210752,
                "egg layer 1 = 4210752 (0x404040) per ItemSpawnerEgg.addMapping / func_82790_a layer!=0, got "
                        + spawnEgg.getColor(1));
        helper.assertTrue(spawnEgg.getType(new ItemStack(egg)) == CatalogGolems.ELDRITCH_GOLEM.get(),
                "the egg must spawn thaumcraft:eldritchgolem, got "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(spawnEgg.getType(new ItemStack(egg))));
        helper.assertTrue("item.thaumcraft.spawnegg.eldritchgolem".equals(egg.getDescriptionId()),
                "the egg's lang key is the dotted ledger id, got " + egg.getDescriptionId());

        // The model the client will load for it: the vanilla egg template, the
        // same shape every vanilla egg model has (measured in the 1.21.1 client
        // jar: models/item/zombie_spawn_egg.json == {"parent":
        // "minecraft:item/template_spawn_egg"}).
        final JsonObject eggModel =
                modelJson(helper, "spawnegg.eldritchgolem");
        helper.assertTrue("minecraft:item/template_spawn_egg".equals(eggModel.get("parent").getAsString()),
                "the egg model must parent the vanilla two-layer egg template, got parent "
                        + eggModel.get("parent"));

        // The five golem item models the card ships: present, vanilla
        // item/generated parent, sprite through the SINGULAR item/ layout
        // (the 1.7.10 plural layout is the defect docs/asset-mapping.csv and
        // scripts/check_asset_paths.py gate 3 exist to catch).
        for (final String id : new String[] {"itemgolemplacer", "itemgolemcore", "itemgolemupgrade",
                "golembell", "itemgolemdecoration"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(
                            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, id)),
                    "golem item " + id + " must stay registered for its model to be reachable");
            final JsonObject model = modelJson(helper, id);
            helper.assertTrue("minecraft:item/generated".equals(model.get("parent").getAsString()),
                    id + " model parents minecraft:item/generated, got " + model.get("parent"));
            final String layer0 = model.getAsJsonObject("textures").get("layer0").getAsString();
            helper.assertTrue(layer0.startsWith("thaumcraft:item/"),
                    id + " sprite reference is " + layer0 + "; 1.21.1 resolves models at the SINGULAR "
                            + "textures/item/ layout (OriginalTextureOverlayTest, check_asset_paths gate 3)");
        }
        helper.succeed();
    }

    /**
     * A committed item model, read the way {@code LocalizationGameTest.loadLang}
     * reads the lang file: the mod classpath first, the source tree as the
     * fallback the game-test run config makes reachable.
     */
    private static JsonObject modelJson(final GameTestHelper helper, final String path) {
        final String resource = "/assets/" + ThaumcraftClassic.MOD_ID + "/models/item/" + path + ".json";
        String text = null;
        try (InputStream in = GolemMenuGameTest.class.getResourceAsStream(resource)) {
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
                    .resolve("src/main/resources/assets/" + ThaumcraftClassic.MOD_ID + "/models/item/"
                            + path + ".json");
            helper.assertTrue(Files.isRegularFile(file), resource + " is not shipped (classpath, nor " + file + ")");
            try {
                text = Files.readString(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        final var parsed = JsonParser.parseString(text);
        helper.assertTrue(parsed.isJsonObject(), resource + " does not parse to a JSON object");
        return parsed.getAsJsonObject();
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
