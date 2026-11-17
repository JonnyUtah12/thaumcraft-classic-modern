package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.book.BookNoteRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-03-s / BOOK-1 / BOOK-2 / BOOK-4 / TC-M3-04-s-v / INTEGRATE-BOOK-LANES:
 * the book screen's wiring is asserted <b>statically</b>, and there is exactly
 * <b>one</b> book screen.
 *
 * <p><b>Why static.</b> D-019, rule 1: a GUI port asserts against the port's
 * own classes, not against a render harness. Three things can be measured
 * without a client and they are exactly the things the card names: the id the
 * open path hangs off, the fact that the open path is the payload rather than a
 * menu type (BD-22) and <em>its uniqueness</em>, and the fact that the note arm
 * travels on the one completion payload. Everything about the window itself —
 * tabs, rows, the detail panel, the hover hint, the popup — is measured in the
 * live window by {@code ThaumonomiconBookGameTest}, and the note/purchase
 * affordance per state by {@code ResearchNoteProgressionGameTest}.</p>
 *
 * <p><b>One screen.</b> Two lanes each built a Thaumonomicon: this one (tabs +
 * entry listing + per-entry detail panel) and the research lane's node browser.
 * The merge kept this screen and deleted the node browser and its
 * {@code ThaumonomiconClientHooks} open seam, so the assertions below read the
 * merged tree: a second book screen, a second open seam, a
 * {@code thaumcraft:gui_thaumonomicon} menu type, a client-side
 * {@code ItemResearchNotes.completeWithSiblings} call, a second completion path
 * from the window, or an affordance rule re-implemented in the window all fail
 * this test.</p>
 */
class ThaumonomiconScreenRegistrationTest {

    private static final Path ROOT = repoRoot();

    private static final Path CLIENT_PACKAGE =
            ROOT.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/client");

    private static final Path SCREEN_SOURCE =
            CLIENT_PACKAGE.resolve("ThaumonomiconScreen.java");

    private static final Path ITEM_SOURCE = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/items/ItemThaumonomicon.java");

    private static final Path CLIENT_HANDLER_SOURCE = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/network/"
                    + "ClientPayloadHandlers.java");

    private static final Path SERVER_HANDLER_SOURCE = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/network/ServerNetworkHooks.java");

    private static final Path EN_US =
            ROOT.resolve("src/main/resources/assets/thaumcraft/lang/en_us.json");

    /**
     * BOOK-1: {@code thaumcraft:itemthaumonomicon} is the contract id, and after
     * the merge the <b>catalog</b> is the single registry of it. The BOOK lane
     * registers every item from {@code catalog/items.json}; the research lane
     * hand-picked the same id in {@code ModItems.THAUMONOMICON} (TC-M3-03-s), so
     * the merge deleted that declaration — a second registration of a registry
     * id boots the game red. Exactly one source file may register the id.
     */
    @Test
    void theBookItemIdIsTheContractIdAndTheCatalogIsTheOnlyRegistrationSite()
            throws IOException {
        assertTrue(Files.readString(ROOT.resolve("docs/content-mapping.csv"))
                        .contains("thaumcraft:itemthaumonomicon"),
                "the content map must keep the book item's row: the id is a contract");

        final Path root = ROOT.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");
        final List<String> registrationSites;
        try (var paths = Files.walk(root)) {
            registrationSites = paths
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("register(\"itemthaumonomicon\"");
                        } catch (IOException | UncheckedIOException e) {
                            return false;
                        }
                    })
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(List.of("registration/catalog/CatalogResearchBook.java"), registrationSites,
                "one registration site for the book item: the catalog lane's, not the "
                        + "research lane's ModItems hand-pick (BD-13/BD-17 de-duplicated)");
    }

    /**
     * BOOK-1: the open path is the containerless payload, and there is exactly
     * one of it. The right-click arm and the {@code /thaumcraft thaumonomicon}
     * command both call {@code ItemThaumonomicon.openBook}, which sends
     * {@code ClientPayloads.OPEN_BOOK}; the server's handler
     * ({@code ClientPayloadToServer.openBookFromPayload}) is the only code that
     * may open the screen, and it opens it through the screen's own
     * {@code openFromServer}. No {@code thaumcraft:gui_thaumonomicon}
     * {@code MenuType}, no {@code openScreen} ({@code :99-102}) — BD-22 on the
     * BOOK lane — and no client-side hook seam, which is what the research lane
     * had and what the merge deleted.
     */
    @Test
    void theOpenPathIsThePayloadAndThereIsExactlyOne() throws IOException {
        final String item = Files.readString(ITEM_SOURCE);
        assertTrue(item.contains("OpenThaumonomiconPayload"),
                "the item's click sends thaumcraft:open_thaumonomicon (BOOK-1)");
        final String clientHandler = Files.readString(CLIENT_HANDLER_SOURCE);
        assertTrue(clientHandler.contains("ThaumonomiconScreen.openFromServer"),
                "the payload handler opens the screen through its entry point");
        assertFalse(clientHandler.contains("gui_thaumonomicon")
                        || clientHandler.contains("IMenuTypeExtension"),
                "no menu type on the open path (BD-22)");

        assertFalse(Files.exists(CLIENT_PACKAGE.resolve("ThaumonomiconClientHooks.java")),
                "the research lane's client open seam is gone: the payload handler "
                        + "is the only code that opens the book");
        assertFalse(Files.readString(ROOT.resolve(
                        "src/main/java/com/jonnyutah12/thaumcraftclassic/registration/ModItems.java"))
                        .contains("THAUMONOMICON"),
                "the research lane's hand-picked book registration is gone");

        final String screen = Files.readString(SCREEN_SOURCE);
        assertTrue(screen.contains("public static void openFromServer(final String category)"),
                "the screen exposes one entry point for the payload, keyed by category "
                        + "(the original's GuiResearchBrowser(String) category argument)");
        assertTrue(screen.contains("extends Screen"),
                "the book screen extends the vanilla Screen, as the original's did "
                        + "(GuiResearchBrowser extends GuiScreen, :36)");
        assertTrue(screen.contains("public ThaumonomiconScreen(final String category)"),
                "the screen is built from a category key — the original's constructor "
                        + "argument (GuiResearchBrowser.java:161)");
    }

    /**
     * INTEGRATE-BOOK-LANES: one book screen, so exactly one client window class
     * in the client package names the Thaumonomicon, and the research lane's
     * node-browser screen is not in the tree.
     */
    @Test
    void thereIsExactlyOneBookScreenClass() throws IOException {
        final List<String> bookScreens;
        try (var paths = Files.walk(CLIENT_PACKAGE, 1)) {
            bookScreens = paths
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase().contains("thaumonomicon"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(List.of("ThaumonomiconScreen.java"), bookScreens,
                "the merged tree has ONE Thaumonomicon window (Decision 1): the BOOK "
                        + "lane's tabs/listing/detail screen, not a second node browser");
    }

    /**
     * TC-M3-04-s-v: the book's note/purchase arm travels on the single
     * completion path. The window sends the {@code PlayerCompleteToServerPayload}
     * the note item already drives research through — type 1 for a note, type 0
     * for an aspect purchase — and completes nothing itself. The server handler
     * for that payload is the only completion authority
     * ({@code ResearchManager.handlePlayerCompleteRequest}, BD-13), so the
     * merge adds no second completion path (Decision 2).
     */
    @Test
    void theNoteArmTravelsOnTheSingleCompletionPath() throws IOException {
        final String screen = Files.readString(SCREEN_SOURCE);
        assertTrue(screen.contains("PlayerCompleteToServerPayload.REQUEST_NOTE"),
                "the note arm asks the server for a puzzle note on the payload's "
                        + "REQUEST_NOTE arm (the original's :713-716)");
        assertTrue(screen.contains("PlayerCompleteToServerPayload.REQUEST_COMPLETE"),
                "the purchase arm asks for an aspect completion on the payload's "
                        + "REQUEST_COMPLETE arm (the original's :728-753)");
        assertFalse(screen.contains("completeResearch("),
                "the window never completes research: the completion authority stays "
                        + "ResearchManager (Decision 2)");
        assertFalse(screen.contains("ItemResearchNotes"),
                "no client-side ItemResearchNotes reach-through (D-019 rule 2)");

        final String serverHandler = Files.readString(SERVER_HANDLER_SOURCE);
        assertTrue(serverHandler.contains("handlePlayerCompleteRequest"),
                "the payload's server leg dispatches through the one completion authority");
        final String payloadClass = Files.readString(ROOT.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/network/payload/"
                        + "PlayerCompleteToServerPayload.java"));
        assertTrue(payloadClass.contains("REQUEST_NOTE = 1"),
                "the payload's note arm stays the single documented type 1");
    }

    /**
     * D-019 rule 1: the affordance <em>rule</em> lives in the model, not in the
     * window. The window asks {@code ResearchDetailModel.affordance}, which
     * delegates to {@code BookNoteRequest.decide}; a second copy of the
     * five-state rule inside the window would fail this and the research lane's
     * own rule tests.
     */
    @Test
    void theAffordanceRuleLivesInTheModelNotTheWindow() throws IOException {
        final String screen = Files.readString(SCREEN_SOURCE);
        assertTrue(screen.contains("BookNoteRequest"),
                "the window draws BookNoteRequest's answer");
        assertTrue(screen.contains("this.details.affordance")
                        || screen.contains("details.affordance")
                        || screen.contains("requestFor"),
                "the window asks the model for the affordance");
        assertFalse(screen.contains("static BookNoteRequest.Request decide"),
                "the window does not re-implement the decision");

        final String model = Files.readString(ROOT.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/research/"
                        + "ResearchDetailModel.java"));
        assertTrue(model.contains("BookNoteRequest.decide"),
                "the model delegates to the shipped rule (BookNoteRequest, TC-M3-04-s-v)");
        assertNotNull(BookNoteRequest.Arm.values());
    }

    /**
     * D-019, the rule this whole file is built on: a client screen is
     * client-painted. The BOOK lane enforces it repo-wide in
     * {@code ArchitectureTest} and this is the same assertion scoped to the
     * lane's own contract, so the merge cannot quietly move the screen onto the
     * server side.
     */
    @Test
    void noServerSideSourceNamesTheClientScreen() throws IOException {
        final Path root = ROOT.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");
        final List<String> offenders;
        try (var paths = Files.walk(root)) {
            offenders = paths
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/client/"))
                    .filter(p -> {
                        try {
                            // Javadoc that NAMES the screen is not a class-loading
                            // hazard; code that names it is. Strip comments first.
                            return stripComments(Files.readString(p)).contains("ThaumonomiconScreen");
                        } catch (IOException | UncheckedIOException e) {
                            return false;
                        }
                    })
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(List.of("gametest/LocalizationGameTest.java",
                        "network/ClientPayloadHandlers.java"),
                offenders,
                "only the client payload handler names the book screen (it runs on the "
                        + "client) and the localization inventory, which names it as the "
                        + "owner of its title key; no server-painted class may name it");
    }

    /**
     * BOOK-1/BOOK-3: the item and the screen both have names. An untranslated
     * key on the item is what made the book invisible in the hand, and the
     * window's title is a caller-supplied string — the lang key is what every
     * caller uses, so a missing key is a window titled {@code
     * container.thaumcraft.thaumonomicon}.
     */
    @Test
    void theItemAndTheScreenBothHaveLocalizedNames() throws IOException {
        final String lang = Files.readString(EN_US);
        assertTrue(lang.contains("\"item.thaumcraft.itemthaumonomicon\":"),
                "the book needs a name: 7 of the 8 catalog rows BOOK-3 added had no key");
        assertTrue(lang.contains("\"container.thaumcraft.thaumonomicon\":"),
                "the book screen's title is a caller-supplied lang key (the "
                        + "containerless route has no menu to name it)");
    }

    /**
     * The repo root. Gradle runs the test task with the module directory as its
     * working directory, so a relative {@code src/...} path is not the repo;
     * every source-reading test in this repo resolves through the
     * {@code tc.contentMappingFile} system property, which the build sets to
     * {@code <root>/docs/content-mapping.csv}.
     */
    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        if (!Files.isRegularFile(csv)) {
            throw new AssertionError("content-mapping.csv not wired - run tests through ./gradlew test");
        }
        return csv.getParent().getParent();
    }

    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    private static int count(final String haystack, final String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }
}
