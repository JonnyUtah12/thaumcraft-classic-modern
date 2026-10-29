package com.jonnyutah12.thaumcraftclassic.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.menus.TrunkMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-s-cr1 acceptance for the FIRST container screen the port authors.
 *
 * <p>What is proved, and at which level — the honest split, because a screen
 * only ever runs inside a real client and the unit harness has no window and
 * no bootstrapped registries:</p>
 * <ol>
 *   <li><b>Contract level (this class, plain JVM):</b> {@link TrunkScreen} is a
 *       {@code AbstractContainerScreen<TrunkMenu>} — the right menu type, the
 *       {@code MenuAccess} pairing, and 1.21.1's screen-constructor shape
 *       {@code (TrunkMenu, Inventory, Component)} that
 *       {@code RegisterMenuScreensEvent}'s constructor argument demands.</li>
 *   <li><b>Registry level (this class, plain JVM):</b> the wired handler is
 *       invoked with a real {@code RegisterMenuScreensEvent} over a live map,
 *       after {@code thaumcraft:gui_trunk} is registered in
 *       {@code BuiltInRegistries.MENU}, and the map ends up holding
 *       <i>that</i> {@code MenuType} instance mapped to a non-null screen
 *       constructor. That map is exactly the map vanilla's
 *       {@code MenuScreens} consults to build a screen from the open-screen
 *       packet, so this is the registration the client uses, not a
 *       look-alike.</li>
 *   <li><b>Wiring level (this class, source-level):</b> the handler is
 *       self-registering on the MOD bus and client-only, so a dedicated server
 *       ({@code runServer}, {@code runGameTestServer}) never loads it, and the
 *       register call pairs the trunk menu holder with {@link TrunkScreen}.</li>
 * </ol>
 *
 * <p>What is NOT proved here: that the GUI renders on screen. That needs a
 * real client (the {@code runClient} path) and is stated as such in
 * {@code docs/FEATURE_MATRIX.md}.</p>
 */
class TrunkScreenRegistrationTest {

    private static final ResourceLocation GUI_TRUNK_ID =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "gui_trunk");

    // ---------------------------------------------------- contract level

    @Test
    void screenExtendsAbstractContainerScreenForTheTrunkMenu() {
        assertEquals(AbstractContainerScreen.class, TrunkScreen.getSuperclass(),
                "the screen must extend 1.21.1's AbstractContainerScreen");
        final Type superType = TrunkScreen.getGenericSuperclass();
        assertTrue(superType instanceof ParameterizedType, "the base must be parameterized");
        assertEquals(TrunkMenu.class, ((ParameterizedType) superType).getActualTypeArguments()[0],
                "the screen must be parameterized on TrunkMenu, not on another menu");
        assertTrue(MenuAccess.class.isAssignableFrom(TrunkScreen.class),
                "AbstractContainerScreen must supply the MenuAccess<TrunkMenu> pairing "
                        + "RegisterMenuScreensEvent.register requires");
    }

    @Test
    void screenHasThe1211ScreenConstructorShape() throws Exception {
        final var constructor = TrunkScreen.getConstructor(TrunkMenu.class, Inventory.class, Component.class);
        assertNotNull(constructor,
                "1.21.1 builds a container screen as (menu, inventory, title-from-the-open-screen-packet)");
        // The title is the menu's display name; the shipped menu key is the
        // fallback, and it is the same key the localization audit requires for
        // the registered menu type — so the audit sees no extra key.
        assertTrue(TrunkScreen.TITLE_KEY.equals("container.thaumcraft." + GUI_TRUNK_ID.getPath()));
        assertTrue(menuLangKeyIsShipped(TrunkScreen.TITLE_KEY),
                TrunkScreen.TITLE_KEY + " must ship in en_us.json (the screen title fallback)");
    }

    // ---------------------------------------------------- registry level

    @Test
    void registerScreensEventMapsTheTrunkMenuTypeToAScreenConstructor() throws Exception {
        // The unit harness has no mod loader, so the menu registry is empty.
        // Register the trunk id for real, which is what makes
        // CatalogTrinketsLoot.GUI_TRUNK.get() resolvable for the handler.
        final MenuType<TrunkMenu> trunkMenuType =
                new MenuType<>((id, inv) -> null, FeatureFlags.VANILLA_SET);
        registerIntoBuiltIn(BuiltInRegistries.MENU, GUI_TRUNK_ID, trunkMenuType);
        assertSame(trunkMenuType, CatalogTrinketsLoot.GUI_TRUNK.get(),
                "the menu holder must resolve to the menu type registered at " + GUI_TRUNK_ID);

        final Map<MenuType<?>, ?> registeredScreens = new HashMap<>();
        ModClientScreens.onRegisterScreens(newRegisterMenuScreensEvent(registeredScreens));

        assertEquals(1, registeredScreens.size(),
                "exactly one screen is wired today (TC-M7-04-s-cr1 is one screen)");
        assertSame(trunkMenuType, registeredScreens.keySet().iterator().next(),
                "the event map must be keyed by the trunk MenuType instance");
        assertNotNull(registeredScreens.get(trunkMenuType),
                "the trunk MenuType must map to a screen constructor");
    }

    // ---------------------------------------------------- wiring level

    @Test
    void handlerIsSelfRegisteringOnTheModBusClientOnlyAndPairsTrunkWithTrunkScreen() throws Exception {
        final String source = readModClientScreensSource();
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID,"),
                "the screen wiring must be self-registering for this mod");
        assertTrue(source.contains("bus = EventBusSubscriber.Bus.MOD"),
                "RegisterMenuScreensEvent is a mod-bus event in NeoForge 21.1");
        assertTrue(source.contains("value = Dist.CLIENT"),
                "the wiring is client-only, so a dedicated server never loads the screen class");
        assertTrue(source.contains("@SubscribeEvent"),
                "21.1 needs @SubscribeEvent on the handler");
        assertTrue(source.contains("RegisterMenuScreensEvent event"),
                "the handler must take RegisterMenuScreensEvent");
        assertTrue(source.contains("event.register(CatalogTrinketsLoot.GUI_TRUNK.get(), TrunkScreen::new)"),
                "the register call must pair the trunk menu holder with TrunkScreen");
    }

    @Test
    void trunkMenuIdIsTheContractIdTheScreenBindsTo() {
        assertEquals(GUI_TRUNK_ID, CatalogTrinketsLoot.GUI_TRUNK.getKey().location(),
                "docs/content-mapping.csv row Thaumcraft:GUI_TRUNK realizes as thaumcraft:gui_trunk");
    }

    // ---------------------------------------------------- harness helpers

    /** Fire the vanilla register call over a live map (the ctor is internal). */
    private static RegisterMenuScreensEvent newRegisterMenuScreensEvent(final Map<MenuType<?>, ?> target)
            throws Exception {
        final var ctor = RegisterMenuScreensEvent.class.getDeclaredConstructor(Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(target);
    }

    /** Registry.register(Registry, ResourceLocation, T) is package-private in 1.21.1. */
    @SuppressWarnings("unchecked")
    private static <T> void registerIntoBuiltIn(final Registry<T> registry, final ResourceLocation id,
                                                final T value) throws Exception {
        final Method register = Registry.class.getDeclaredMethod(
                "register", Registry.class, ResourceLocation.class, Object.class);
        register.setAccessible(true);
        register.invoke(null, registry, id, value);
    }

    private static boolean menuLangKeyIsShipped(final String key) throws Exception {
        try (InputStream in = TrunkScreenRegistrationTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            final JsonObject lang = JsonParser.parseReader(
                    new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return lang.has(key) && !lang.get(key).getAsString().isBlank();
        }
    }

    /** The repo root, off the content-mapping path the build passes to tests. */
    private static String readModClientScreensSource() throws Exception {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path repoRoot = csv.getParent().getParent();
        final Path source = repoRoot.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/client/ModClientScreens.java");
        assertTrue(Files.isRegularFile(source), "expected the wiring source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
