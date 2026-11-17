package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import com.jonnyutah12.thaumcraftclassic.network.payload.OpenThaumonomiconPayload;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BOOK-1 (t_1e6b18cc): the Thaumonomicon open path, pinned in the plain JVM.
 *
 * <p><b>Why a unit test and not only a game test.</b> The harness the build
 * gates on — {@code runGameTestServer} — boots a <em>dedicated server</em>:
 * {@code Minecraft.getInstance()} is null, no screen is ever constructed,
 * {@code RegisterMenuScreensEvent} never fires, and no key event ever reaches a
 * window. So the screen half of this card — a class that exists purely to be
 * shown — is unobservable there, exactly as {@code TrunkScreenRegistrationTest}
 * documents for the one screen pair that works today. The structural contract
 * below is therefore asserted against the PORT's classes (D-018: the 1.7.10 GUI
 * is immediate-mode GL with no 1.21.1 equivalent to javap against), and the
 * honest limit — the pixels need the ATM10 client — is stated in
 * docs/FEATURE_MATRIX.md rather than claimed.</p>
 *
 * <p>Every assertion names the original source line that makes it the right
 * contract, in {@code reference/decompiled/thaumcraft/…}.</p>
 */
class ThaumonomiconOpenPathContractTest {

    /** The gui row's id: docs/content-mapping.csv row {@code Thaumcraft:GUI_THAUMONOMICON}. */
    private static final String GUI_ROW_PATH = "gui_thaumonomicon";

    // ------------------------------------------------ structural: the screen

    @Test
    void screenExtendsScreenAndNotAContainerScreen() {
        // GuiResearchBrowser.java:41 `public class GuiResearchBrowser extends GuiScreen`
        // — no Container, no slots. The port keeps that shape: the menu route
        // would invent a container the original never had (BD-22).
        assertEquals(Screen.class, ThaumonomiconScreen.class.getSuperclass(),
                "the book screen must extend Screen directly, like GuiResearchBrowser extends GuiScreen");
        assertFalse(AbstractContainerScreen.class.isAssignableFrom(ThaumonomiconScreen.class),
                "the book screen must NOT be an AbstractContainerScreen: the original has no Container");
        assertNotNull(ThaumonomiconScreen.class.getAnnotation(OnlyIn.class),
                "the screen is client-only, so a dedicated server never loads it");
        assertEquals(Dist.CLIENT, ThaumonomiconScreen.class.getAnnotation(OnlyIn.class).value());

        final Constructor<?> ctor = constructorWith(ThaumonomiconScreen.class, String.class);
        assertNotNull(ctor, "the screen opens from the payload's category: ctor(String)");
    }

    @Test
    void screenKeepsTheVanillaCloseAndPauseDefaultsOfTheOriginal() {
        // GuiResearchBrowser overrides no key handler (no keyTyped, no
        // mouseClicked for closing) and no doesGuiPauseGame, so vanilla governs
        // it: ESC closes, and the screen is a pause screen in singleplayer.
        // 1.21.1's equivalents are shouldCloseOnEsc()/isPauseScreen() — the
        // fidelity is in NOT overriding them.
        assertFalse(declares(ThaumonomiconScreen.class, "shouldCloseOnEsc"),
                "the original overrides no close-on-esc: ESC closes the book (GuiScreen default)");
        assertFalse(declares(ThaumonomiconScreen.class, "isPauseScreen"),
                "the original overrides no doesGuiPauseGame: the book pauses in singleplayer");
        assertFalse(declares(ThaumonomiconScreen.class, "keyPressed"),
                "the original has no key handler at all: vanilla's ESC path is the close path");
        // The close itself is the vanilla GuiScreen act (setScreen(null)); in
        // 1.21.1 the screen is removed and `removed()` is the hook the original
        // used for its own close bookkeeping (onGuiClosed, GuiResearchBrowser:116-121).
        assertNotNull(method(ThaumonomiconScreen.class, "removed"),
                "removed() is the 1.21.1 stand-in for the original's onGuiClosed");
    }

    @Test
    void theCategoryMemoryIsTheOriginalsStaticField() {
        // GuiResearchBrowser.java:63 `private static String selectedCategory` —
        // the reason a reopen lands on the page the player left. The port keeps
        // one static field and writes it in removed().
        try {
            final Field field = ThaumonomiconScreen.class.getDeclaredField("lastCategory");
            assertTrue(Modifier.isStatic(field.getModifiers()),
                    "the category memory is a static in the original (:63) and must stay static");
            assertEquals(String.class, field.getType());
        } catch (NoSuchFieldException e) {
            org.junit.jupiter.api.Assertions.fail("ThaumonomiconScreen must keep a static category memory", e);
        }
        final Method resolve = declaredMethod(ThaumonomiconScreen.class, "resolveCategory", String.class);
        assertTrue(Modifier.isStatic(resolve.getModifiers()),
                "the empty-category default (original :96-99) must be reachable without a window");
        assertEquals(String.class, resolve.getReturnType());
    }

    @Test
    void titleKeyIsTheCardKeyAndShipsInLang() throws Exception {
        // The card's key. A containerless screen gets no title from the server
        // (a menu's title rides the open-screen packet), so the screen
        // translates this key itself.
        assertEquals("container.thaumcraft.thaumonomicon", ThaumonomiconScreen.TITLE_KEY);
        final JsonObject lang = langFile();
        assertTrue(lang.has(ThaumonomiconScreen.TITLE_KEY),
                ThaumonomiconScreen.TITLE_KEY + " must ship in en_us.json (the card's key)");
        // D-016: original display strings commit verbatim. The book's own name
        // string in the original lang is `item.ItemThaumonomicon.name` =
        // "Thaumonomicon" (reference/assets/thaumcraft/lang/en_US.lang).
        assertEquals("Thaumonomicon", lang.get(ThaumonomiconScreen.TITLE_KEY).getAsString());
        assertTrue(lang.has("item.thaumcraft.itemthaumonomicon"),
                "the book item's id-derived label key must ship (SPINE-2 searchable)");
        assertEquals("Thaumonomicon", lang.get("item.thaumcraft.itemthaumonomicon").getAsString());
    }

    // ------------------------------------------------ the open path: payload, not menu

    @Test
    void openPathIsAPayloadAndNotAMenuOpen() throws Exception {
        // ItemThaumonomicon.java:79 `player.openGui(Thaumcraft.instance, 12, world, 0, 0, 0)`
        // is a server-initiated client screen. 1.21.1's only server-initiated
        // open is Player#openMenu, which needs a container — the browser has
        // none, so the port sends a payload (BD-22).
        final String item = readSource("items/ItemThaumonomicon.java");
        assertTrue(item.contains("PacketDistributor.sendToPlayer(player"),
                "the server arm must send the open to the player's client, like EntityPlayerMP.openGui");
        assertTrue(item.contains("new OpenThaumonomiconPayload"),
                "the open must ride the OpenThaumonomiconPayload");
        assertFalse(item.contains("openMenu"),
                "the book must not open a menu: the original's browser is a containerless GuiScreen");

        final String catalog = readSource("registration/catalog/CatalogResearchBook.java");
        assertFalse(catalog.contains("Registries.MENU"),
                "no menu type may be registered for the containerless browser (BD-22)");
        assertTrue(catalog.contains("\"itemthaumonomicon\""),
                "the item registers under the ledger's id thaumcraft:itemthaumonomicon");

        // The book screen is payload-opened, so it must NOT be paired with a
        // menu screen factory — ModClientScreens stays the trunk-only file.
        final String screens = readSource("client/ModClientScreens.java");
        assertFalse(screens.contains("Thaumonomicon"),
                "the book screen is opened by the payload handler, not by RegisterMenuScreensEvent");
    }

    @Test
    void itemKeepsTheOriginalsTwoSidedArms() throws Exception {
        // ItemThaumonomicon.java:41 server body, :72-73 client branch, and the
        // stack is returned, never consumed (:79 falls out with `return stack`).
        final String item = readSource("items/ItemThaumonomicon.java");
        assertTrue(item.contains("if (level.isClientSide)"),
                "the original splits the arms on world.field_72995_K (:41, :72)");
        assertTrue(item.contains("ModSounds.PAGE.get()"),
                "the client branch plays thaumcraft:page at the player (:72-73)");
        assertTrue(item.contains("sidedSuccess"),
                "the client arm must not consume the stack (the original returns it)");
        assertTrue(item.contains("completeSiblingsOfCompletedResearch(player)"),
                "the server arm must run the research sync before the open (:56-64)");
    }

    @Test
    void payloadCarriesTheCategoryAndRoundTripsByteExactly() {
        // The open's only state. Empty = "the last category read, else the
        // first of the graph" (GuiResearchBrowser.java:96-99).
        for (final String category : new String[] {"BASICS", "ARCANE", OpenThaumonomiconPayload.EMPTY_CATEGORY}) {
            final ByteBuf buf = Unpooled.buffer();
            OpenThaumonomiconPayload.STREAM_CODEC.encode(buf, new OpenThaumonomiconPayload(category));
            final OpenThaumonomiconPayload decoded = OpenThaumonomiconPayload.STREAM_CODEC.decode(buf);
            assertEquals(category, decoded.category(), "payload category did not round-trip");
            assertEquals("thaumcraft:open_thaumonomicon", OpenThaumonomiconPayload.TYPE.id().toString(),
                    "the payload id is the channel contract both sides must agree on");
        }
    }

    // ------------------------------------------------------------- plumbing

    private static Constructor<?> constructorWith(final Class<?> type, final Class<?>... params) {
        try {
            return type.getConstructor(params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method method(final Class<?> type, final String name, final Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            org.junit.jupiter.api.Assertions.fail(type.getSimpleName() + " must declare " + name, e);
            return null;
        }
    }

    /** Package-private members count: the port keeps its helpers encapsulated. */
    private static Method declaredMethod(final Class<?> type, final String name, final Class<?>... params) {
        try {
            return type.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException e) {
            org.junit.jupiter.api.Assertions.fail(type.getSimpleName() + " must declare " + name, e);
            return null;
        }
    }

    private static boolean declares(final Class<?> type, final String name) {
        for (final Method candidate : type.getDeclaredMethods()) {
            if (candidate.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject langFile() throws Exception {
        try (Reader reader = new InputStreamReader(
                ThaumonomiconOpenPathContractTest.class.getResourceAsStream(
                        "/assets/thaumcraft/lang/en_us.json"),
                StandardCharsets.UTF_8)) {
            assertNotNull(reader, "the shipped lang file must be on the test classpath");
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * The source of a shipped class, read from the repo the harness runs in —
     * the wiring a JVM-only harness cannot execute is pinned at source level,
     * the same way {@code TrunkScreenRegistrationTest} pins the trunk pairing.
     */
    private static String readSource(final String relativeUnderModPackage) throws Exception {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        Path root = csv.getParent() == null ? Path.of(System.getProperty("user.dir")) : csv.getParent().getParent();
        final String suffix = "src/main/java/com/jonnyutah12/thaumcraftclassic/" + relativeUnderModPackage;
        Path source = root.resolve(suffix);
        if (!Files.isRegularFile(source)) {
            // Fallback: walk up from the working directory (the harness's cwd).
            Path walk = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            for (int i = 0; i < 6 && !Files.isRegularFile(source); i++) {
                source = walk.resolve(suffix);
                walk = walk.getParent();
            }
        }
        assertTrue(Files.isRegularFile(source), "expected the source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
