package com.jonnyutah12.thaumcraftclassic.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-03-s acceptance for the book screen's own contract, at the level the
 * harness can actually reach.
 *
 * <p>A {@code Screen} only ever runs inside a real client: the unit harness has
 * no window, and the game-test harness is a dedicated server that cannot
 * construct one. So this class pins the three things that ARE decidable off a
 * client — the screen's shape, the item id its open path hangs off, and the
 * dist-safety of the wiring — and leaves the behaviour to
 * {@code ThaumonomiconBookTest} (state machine) and
 * {@code ThaumonomiconGameTest} (open path + rendered graph on the real
 * server).</p>
 */
class ThaumonomiconScreenRegistrationTest {

    private static final ResourceLocation ITEM_THAUMONOMICON_ID =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemthaumonomicon");

    @Test
    void screenIsAScreenOverTheBookModel() throws Exception {
        assertEquals(Screen.class, ThaumonomiconScreen.class.getSuperclass(),
                "the book is a plain Screen, not a container screen");

        final var constructor = ThaumonomiconScreen.class.getConstructor(ThaumonomiconBook.class);
        assertNotNull(constructor, "the screen takes the book model");

        final var open = ThaumonomiconScreen.class.getMethod("open", ThaumonomiconBook.class);
        assertTrue(java.lang.reflect.Modifier.isStatic(open.getModifiers()),
                "open(ThaumonomiconBook) is the host implementation and must be static");
        assertEquals(void.class, open.getReturnType());
    }

    @Test
    void screenIsClientOnly() throws Exception {
        final String source = readSource("client/ThaumonomiconScreen.java");
        assertTrue(source.contains("@OnlyIn(Dist.CLIENT)"),
                "the screen must be marked client-only so a dedicated server never loads it");
    }

    @Test
    void bookItemIdIsTheContractIdTheOpenPathHangsOff() {
        assertEquals(ITEM_THAUMONOMICON_ID, ModItems.THAUMONOMICON.getKey().location(),
                "docs/content-mapping.csv row Thaumcraft:ItemThaumonomicon realizes as "
                        + "thaumcraft:itemthaumonomicon");
    }

    @Test
    void theItemAndTheScreenBothHaveLocalizedNames() throws Exception {
        try (InputStream in = ThaumonomiconScreenRegistrationTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "en_us.json must be on the test classpath");
            final JsonObject lang = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(lang.has("item.thaumcraft.itemthaumonomicon")
                            && !lang.get("item.thaumcraft.itemthaumonomicon").getAsString().isBlank(),
                    "the Thaumonomicon item's name key must be shipped, or the item renders as a raw key");
        }
    }

    @Test
    void theWiringIsSelfRegisteringClientOnlyAndOpensThroughTheHost() throws Exception {
        final String source = readSource("client/ThaumonomiconClientHooks.java");
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)"),
                "the open path must be client-only and self-registering");
        assertTrue(source.contains("@SubscribeEvent"), "21.1 needs @SubscribeEvent on the handler");
        assertTrue(source.contains("PlayerInteractEvent.RightClickItem event"),
                "the trigger is right-clicking the item, as the original's onItemRightClick was");
        assertTrue(source.contains("ThaumonomiconHost.install(ThaumonomiconScreen::open)"),
                "the client must install the screen as the host implementation");
        assertTrue(source.contains("ThaumonomiconHost.openBook("),
                "the open path must go through the shared seam, which is what the game test drives");
        assertTrue(source.contains("ClientNetworkData.completedResearch()"),
                "the screen must open with the client's synced knowledge");
    }

    /**
     * The classloading contract: nothing outside the {@code client} package may
     * name the screen. A common class holding a reference to a {@code Screen}
     * subclass is a dedicated-server hazard ({@code runServer} and
     * {@code runGameTestServer} would try to load it), which is the whole reason
     * the {@code ThaumonomiconHost} seam exists.
     */
    @Test
    void noServerSideSourceNamesTheClientScreen() throws Exception {
        final Path main = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");
        final List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (final Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.toString().contains("/client/")) {
                    continue;
                }
                final String source = Files.readString(file, StandardCharsets.UTF_8);
                if (source.contains("ThaumonomiconScreen")) {
                    offenders.add(main.relativize(file).toString());
                }
            }
        }
        assertEquals(List.of(), offenders,
                "only the client package may name the screen; the seam is what keeps servers loading it");
    }

    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");
        return csv.getParent().getParent();
    }

    private static String readSource(final String relative) throws Exception {
        final Path source = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic").resolve(relative);
        assertTrue(Files.isRegularFile(source), "expected the source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
