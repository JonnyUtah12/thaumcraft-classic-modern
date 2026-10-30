package com.jonnyutah12.thaumcraftclassic.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu;
import com.jonnyutah12.thaumcraftclassic.menus.HandMirrorMenu;
import com.jonnyutah12.thaumcraftclassic.menus.MagicBoxMenu;
import com.jonnyutah12.thaumcraftclassic.menus.SpaMenu;
import com.jonnyutah12.thaumcraftclassic.menus.TrunkMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-s acceptance for screen coverage: <b>every {@code MenuType} the mod
 * registers is paired with a client screen</b>, each screen has the 1.21.1
 * shape {@code RegisterMenuScreensEvent} demands, and each title falls back to a
 * lang key that actually ships.
 *
 * <p>Why this test exists in this shape. The mod shipped zero screens before
 * TC-M7-04 and the {@code wip-salvage} tree contains no screen sources
 * ({@code git ls-tree -r --name-only wip-salvage | grep -icE "screen|gui"} = 0),
 * so GUI parity here is <i>authoring</i>: the failure mode to guard against is
 * not a mis-ported screen, it is a menu type that opens a blank window. The
 * catalogs are therefore parsed for their {@code MenuType} declarations and
 * every one of them must appear in {@link ModClientScreens}' wiring — a new
 * menu type added without a screen fails this test instead of shipping a blank
 * GUI.</p>
 *
 * <p>Level of proof, stated honestly: this is contract + source-wiring level on
 * a plain JVM. {@code RegisterMenuScreensEvent} cannot be fired for real here —
 * the mod-bus handler resolves each {@code DeferredHolder}, and the unit harness
 * bootstraps the vanilla registries frozen (the reason cr1's fifth assertion was
 * dropped). A live check of the screen map needs a real client; the game-test
 * harness is a dedicated server and never sees the event. Rendered pixels are
 * not proved by anything in this repo's CI.</p>
 */
class MenuTypeScreenCoverageTest {

    /** screen class -> { catalog class, holder field, menu class, registered id }. */
    private static final Map<Class<?>, ScreenUnderTest> SCREENS = new LinkedHashMap<>();
    /** menu class -> registered id, parsed from the catalogs. */
    private static final Map<String, String> EXPECTED_HOLDERS = new LinkedHashMap<>();

    static {
        SCREENS.put(TrunkScreen.class, new ScreenUnderTest("CatalogTrinketsLoot", "GUI_TRUNK",
                TrunkMenu.class, "gui_trunk"));
        SCREENS.put(HandMirrorScreen.class, new ScreenUnderTest("CatalogTrinketsLoot", "GUI_HAND_MIRROR",
                HandMirrorMenu.class, "gui_hand_mirror"));
        SCREENS.put(DeconstructionTableScreen.class, new ScreenUnderTest("CatalogFurniture", "MENU_DECON_TABLE",
                DeconstructionTableMenu.class, "gui_decon_table"));
        SCREENS.put(MagicBoxScreen.class, new ScreenUnderTest("CatalogFurniture", "MENU_MAGIC_BOX",
                MagicBoxMenu.class, "gui_magic_box"));
        SCREENS.put(SpaScreen.class, new ScreenUnderTest("CatalogFurniture", "MENU_SPA",
                SpaMenu.class, "gui_spa"));
        SCREENS.put(ArcaneWorkbenchScreen.class, new ScreenUnderTest("CatalogArcaneWorkbench",
                "MENU_ARCANE_WORKBENCH", ArcaneWorkbenchMenu.class, "gui_arcane_workbench"));

        EXPECTED_HOLDERS.put("CatalogTrinketsLoot.GUI_TRUNK", "gui_trunk");
        EXPECTED_HOLDERS.put("CatalogTrinketsLoot.GUI_HAND_MIRROR", "gui_hand_mirror");
        EXPECTED_HOLDERS.put("CatalogFurniture.MENU_DECON_TABLE", "gui_decon_table");
        EXPECTED_HOLDERS.put("CatalogFurniture.MENU_MAGIC_BOX", "gui_magic_box");
        EXPECTED_HOLDERS.put("CatalogFurniture.MENU_SPA", "gui_spa");
        EXPECTED_HOLDERS.put("CatalogArcaneWorkbench.MENU_ARCANE_WORKBENCH", "gui_arcane_workbench");
    }

    private record ScreenUnderTest(String catalog, String holder, Class<?> menu, String id) {
    }

    // ------------------------------------------------- contract level

    @Test
    void everyScreenIsAContainerScreenForItsOwnMenu() {
        for (final Map.Entry<Class<?>, ScreenUnderTest> entry : SCREENS.entrySet()) {
            final Class<?> screen = entry.getKey();
            final ScreenUnderTest expected = entry.getValue();
            assertEquals(AbstractContainerScreen.class, screen.getSuperclass(),
                    screen.getSimpleName() + " must extend 1.21.1's AbstractContainerScreen");
            final Type superType = screen.getGenericSuperclass();
            assertTrue(superType instanceof ParameterizedType,
                    screen.getSimpleName() + "'s base must be parameterized");
            assertEquals(expected.menu(), ((ParameterizedType) superType).getActualTypeArguments()[0],
                    screen.getSimpleName() + " must be parameterized on " + expected.menu().getSimpleName());
            assertTrue(MenuAccess.class.isAssignableFrom(screen),
                    screen.getSimpleName() + " must supply the MenuAccess pairing the event requires");
            assertTrue(Screen.class.isAssignableFrom(screen), screen.getSimpleName() + " must be a Screen");
            assertTrue(AbstractContainerMenu.class.isAssignableFrom(expected.menu()),
                    expected.menu().getSimpleName() + " must be a menu type");
        }
    }

    @Test
    void everyScreenHasThe1211ScreenConstructorShape() throws Exception {
        for (final Map.Entry<Class<?>, ScreenUnderTest> entry : SCREENS.entrySet()) {
            final Class<?> screen = entry.getKey();
            final Constructor<?> ctor = screen.getConstructor(entry.getValue().menu(), Inventory.class,
                    Component.class);
            assertNotNull(ctor, "1.21.1 builds a container screen as (menu, inventory, title-from-the-packet): "
                    + screen.getSimpleName());
        }
    }

    @Test
    void everyTitleKeyIsTheShippedMenuKey() throws Exception {
        for (final Map.Entry<Class<?>, ScreenUnderTest> entry : SCREENS.entrySet()) {
            final Class<?> screen = entry.getKey();
            final String id = entry.getValue().id();
            final String key = "container.thaumcraft." + id;
            final java.lang.reflect.Field titleKey = screen.getDeclaredField("TITLE_KEY");
            titleKey.setAccessible(true);
            assertEquals(key, titleKey.get(null),
                    screen.getSimpleName() + " must fall back to the key its own menu type ships");
            assertTrue(langKeyIsShipped(key), key + " must ship in en_us.json (the screen title fallback)");
        }
    }

    // --------------------------------------------------- coverage level

    @Test
    void everyMenuTypeDeclaredInTheCatalogsIsWiredToAScreen() throws Exception {
        final String wiring = readSource("src/main/java/com/jonnyutah12/thaumcraftclassic/client/ModClientScreens.java");
        for (final Map.Entry<Class<?>, ScreenUnderTest> entry : SCREENS.entrySet()) {
            final ScreenUnderTest expected = entry.getValue();
            assertTrue(wiring.contains("event.register(" + expected.catalog() + "." + expected.holder()
                            + ".get(), " + entry.getKey().getSimpleName() + "::new)"),
                    "a registered menu type with no screen registration: "
                            + expected.catalog() + "." + expected.holder());
        }
        assertEquals(6, SCREENS.size(), "the six gui rows whose menus exist today");
    }

    @Test
    void catalogSourcesDeclareExactlyTheSixScreensMenus() throws Exception {
        final Map<String, String> declared = new LinkedHashMap<>();
        collectDeclaredMenuTypes("src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog/CatalogTrinketsLoot.java",
                "CatalogTrinketsLoot", declared);
        collectDeclaredMenuTypes("src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog/CatalogFurniture.java",
                "CatalogFurniture", declared);
        collectDeclaredMenuTypes("src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog/CatalogArcaneWorkbench.java",
                "CatalogArcaneWorkbench", declared);
        assertEquals(EXPECTED_HOLDERS, declared,
                "the catalogs' MenuType declarations and this test's expectations must agree; a new menu type "
                        + "needs a screen and an entry here");
    }

    @Test
    void everyScreenIsRegisteredFromTheClientOnlyModBusHandler() throws Exception {
        final String wiring = readSource("src/main/java/com/jonnyutah12/thaumcraftclassic/client/ModClientScreens.java");
        assertTrue(wiring.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID,"),
                "the screen wiring must be self-registering for this mod");
        assertTrue(wiring.contains("bus = EventBusSubscriber.Bus.MOD"),
                "RegisterMenuScreensEvent is a mod-bus event in NeoForge 21.1");
        assertTrue(wiring.contains("value = Dist.CLIENT"),
                "the wiring is client-only, so a dedicated server never loads a screen class");
        assertTrue(wiring.contains("@SubscribeEvent"), "21.1 needs @SubscribeEvent on the handler");
        assertTrue(wiring.contains("RegisterMenuScreensEvent event"),
                "the handler must take RegisterMenuScreensEvent");
    }

    @Test
    void everyScreenClassIsClientOnly() throws Exception {
        for (final Class<?> screen : SCREENS.keySet()) {
            final String source = readSource("src/main/java/com/jonnyutah12/thaumcraftclassic/client/"
                    + screen.getSimpleName() + ".java");
            assertTrue(source.contains("@OnlyIn(Dist.CLIENT)"),
                    screen.getSimpleName() + " must be annotated client-only");
            assertTrue(source.contains("import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;"),
                    screen.getSimpleName() + " must extend the 1.21.1 container screen");
        }
    }

    // ---------------------------------------------------- menu buttons

    @Test
    void theScreensSendTheOriginalMenuButtonIds() throws Exception {
        // Original: GuiDeconstructionTable's aspect box and GuiSpa's mix box both
        // call handleInventoryButtonClick(containerId, 1).
        assertEquals(1, DeconstructionTableScreen.BUTTON_COLLECT_ASPECT);
        assertEquals(1, SpaScreen.BUTTON_TOGGLE_MIX);
        for (final Class<?> screen : new Class<?>[] {DeconstructionTableScreen.class, SpaScreen.class}) {
            final String source = readSource("src/main/java/com/jonnyutah12/thaumcraftclassic/client/"
                    + screen.getSimpleName() + ".java");
            assertTrue(source.contains("handleInventoryButtonClick(this.menu.containerId,"),
                    screen.getSimpleName() + " must submit the original's menu button");
        }
    }

    // ---------------------------------------------------- harness helpers

    private static void collectDeclaredMenuTypes(final String relativePath, final String simpleCatalogName,
                                                 final Map<String, String> into) throws Exception {
        final String source = readSource(relativePath);
        final Matcher matcher = Pattern.compile(
                        "DeferredHolder<MenuType<\\?>, MenuType<([\\w]+)>>\\s+([A-Z_0-9]+)\\s*=\\s*"
                                + "MENUS\\.register\\(\\s*\"([a-z_0-9]+)\"")
                .matcher(source);
        while (matcher.find()) {
            into.put(simpleCatalogName + "." + matcher.group(2), matcher.group(3));
        }
    }

    private static boolean langKeyIsShipped(final String key) throws Exception {
        try (InputStream in = MenuTypeScreenCoverageTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            final JsonObject lang = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return lang.has(key) && !lang.get(key).getAsString().isBlank();
        }
    }

    /** The repo root, off the content-mapping path the build passes to tests. */
    private static String readSource(final String relativePath) throws Exception {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path repoRoot = csv.getParent().getParent();
        final Path source = repoRoot.resolve(relativePath);
        assertTrue(Files.isRegularFile(source), "expected " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
