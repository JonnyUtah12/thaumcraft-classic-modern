package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.MenuType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The research-note screen wiring is real (TC-M3-04-s-iii), in the shape
 * {@code TrunkScreenRegistrationTest} established for the trunk: a registered
 * {@code MenuType} must have a screen mapping (the screen-coverage gate), and
 * the menu contract is useless on screen without one.
 *
 * <p>The registry-level live-map assertion the trunk test documents (and
 * dropped: the unit harness bootstraps the vanilla registries frozen, so a
 * {@code BuiltInRegistries.MENU} injection throws) is not repeated here; the
 * pairing this card owns is asserted at the contract level (the screen's shape)
 * and at the wiring level (the {@code RegisterMenuScreensEvent} handler pairs
 * the note menu holder with the note screen, client-only on the mod bus).</p>
 */
class ResearchNoteScreenRegistrationTest {

    private static final ResourceLocation GUI_NOTE_ID =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "gui_research_note");

    @Test
    void screenExtendsAbstractContainerScreenForTheNoteMenu() {
        assertEquals(AbstractContainerScreen.class, ResearchNoteScreen.class.getSuperclass(),
                "the screen must extend 1.21.1's AbstractContainerScreen");
        final Type superType = ResearchNoteScreen.class.getGenericSuperclass();
        assertTrue(superType instanceof ParameterizedType, "the base must be parameterized");
        assertEquals(ResearchNoteMenu.class, ((ParameterizedType) superType).getActualTypeArguments()[0],
                "the screen must be parameterized on ResearchNoteMenu, not on another menu");
        assertTrue(MenuAccess.class.isAssignableFrom(ResearchNoteScreen.class),
                "AbstractContainerScreen must supply the MenuAccess<ResearchNoteMenu> pairing "
                        + "RegisterMenuScreensEvent.register requires");
    }

    @Test
    void screenHasThe1211ScreenConstructorShape() throws Exception {
        final var constructor = ResearchNoteScreen.class.getConstructor(
                ResearchNoteMenu.class, Inventory.class, Component.class);
        assertNotNull(constructor,
                "1.21.1 builds a container screen as (menu, inventory, title-from-the-open-screen-packet)");
        assertTrue(ResearchNoteScreen.TITLE_KEY.equals("container.thaumcraft." + GUI_NOTE_ID.getPath()),
                "the title key must be the registered menu id's key");
        assertTrue(menuLangKeyIsShipped(ResearchNoteScreen.TITLE_KEY),
                ResearchNoteScreen.TITLE_KEY + " must ship in en_us.json (the screen title fallback)");
        for (final String key : new String[] {ResearchNoteScreen.NO_INK_LINE_0,
                ResearchNoteScreen.NO_INK_LINE_1, ResearchNoteScreen.COPY_HEADER}) {
            assertTrue(menuLangKeyIsShipped(key), key + " must ship (the panel draws it)");
        }
    }

    @Test
    void noteMenuIdIsTheContractIdTheScreenBindsTo() {
        assertEquals(GUI_NOTE_ID, M3ResearchContent.GUI_RESEARCH_NOTE.getKey().location(),
                "docs/content-mapping.csv row Thaumcraft:GUI_RESEARCH_TABLE realizes as "
                        + "thaumcraft:gui_research_note (the flipped new_id, BD-20)");
    }

    @Test
    void handlerPairsTheNoteMenuWithTheNoteScreenClientOnlyOnTheModBus() throws Exception {
        final String source = readModClientScreensSource();
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID,"),
                "the screen wiring must be self-registering for this mod");
        assertTrue(source.contains("bus = EventBusSubscriber.Bus.MOD"),
                "RegisterMenuScreensEvent is a mod-bus event in NeoForge 21.1");
        assertTrue(source.contains("value = Dist.CLIENT"),
                "the wiring is client-only, so a dedicated server never loads the screen class");
        assertTrue(source.contains("event.register(M3ResearchContent.GUI_RESEARCH_NOTE.get(), "
                        + "ResearchNoteScreen::new)"),
                "the register call must pair the note menu holder with ResearchNoteScreen");
    }

    // ---------------------------------------------------- harness helpers

    private static boolean menuLangKeyIsShipped(final String key) throws Exception {
        try (InputStream in = ResearchNoteScreenRegistrationTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            final JsonObject lang = JsonParser.parseReader(
                    new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return lang.has(key) && !lang.get(key).getAsString().isBlank();
        }
    }

    private static String readModClientScreensSource() throws Exception {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path repoRoot = csv.getParent().getParent();
        final Path source = repoRoot.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/client/ModClientScreens.java");
        assertTrue(Files.isRegularFile(source), "expected the wiring source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
