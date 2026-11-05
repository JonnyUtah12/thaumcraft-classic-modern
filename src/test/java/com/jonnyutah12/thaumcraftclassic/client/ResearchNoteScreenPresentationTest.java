package com.jonnyutah12.thaumcraftclassic.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The research-note screen's seams (NOTE-SCREEN-2, closes BD-20): the client
 * draws from the model, and the client's only grid messages are the original's
 * write packet and the original's copy button.
 *
 * <p>These are the two claims the card pins that no runtime probe can make on
 * its own: that the screen never sends a completion claim ({@code
 * ResearchManager.completeResearch} + {@code ResearchCompletePayload} stay the
 * only completer, per D-019), and that the drawing takes its geometry from
 * {@code NoteGridView} rather than re-deriving its own numbers, so the grid
 * drawn and the grid hit-tested cannot drift apart.</p>
 */
class ResearchNoteScreenPresentationTest {

    private static final String SCREEN = "client/ResearchNoteScreen.java";
    private static final String MODEL = "research/NoteGridView.java";

    /** The numbers the model owns; the screen must not re-derive any of them. */
    private static final List<String> MODEL_GEOMETRY = List.of(
            "169", "83", "157", "84", "148", "255", "167", "184", "150", "94", "121", "51", "37", "148");

    @Test
    void theScreenSendsTheOriginalsWritePacketAndNeverACompletion() throws Exception {
        final String source = code(SCREEN);
        assertTrue(source.contains("AspectPlaceToServerPayload"),
                "the screen no longer sends the original's thaumcraft:aspect_place_to_server");
        assertTrue(source.contains("PacketDistributor.sendToServer"),
                "the screen is not sending its grid arms through the mod network");
        for (final String forbidden : List.of("ResearchCompletePayload", "PlayerCompleteToServerPayload",
                "completeResearch", "ResearchManager", "ScannedToServerPayload")) {
            assertFalse(source.contains(forbidden),
                    "the screen names " + forbidden + ": a client that can claim completion reopens BD-20's write arm");
        }
        assertTrue(source.contains("NoteGridView.requestAt("),
                "the screen no longer asks the model which arm a click is");
        assertTrue(source.contains("request.writes()") && source.contains("request.erases()"),
                "one of the original's two grid arms (write on a blank cell, erase on a typed one) went missing");
        // A client read of the synced slot is how the grid gets its note; the
        // client may not write the container: that is the server's write arm.
        for (final String mutation : List.of(".set(", "setItem(", "spendCopies", "quickMoveStack", "safeInsert",
                "placeItemBack", "setData(")) {
            assertFalse(source.contains(mutation),
                    "the screen mutates the container through " + mutation + ": the server's arms own the state");
        }
    }

    @Test
    void theScreenSendsTheOriginalsCopyButton() throws Exception {
        final String source = code(SCREEN);
        assertTrue(source.contains("handleInventoryButtonClick"),
                "the copy plate no longer rides a menu button: the original sends one (GuiResearchTable.java:688)");
        assertTrue(source.contains("ResearchNoteMenu.BUTTON_DUPLICATE"),
                "the copy plate no longer sends the menu's copy button id");
        assertTrue(source.contains("handleInventoryButtonClick(this.menu.containerId"),
                "the copy plate no longer rides the menu's button, so the server's copy arm never runs");
    }

    @Test
    void theScreenDrawsTheWholeGridFromTheModel() throws Exception {
        final String source = code(SCREEN);
        for (final String seam : List.of("NoteGridView.cells(", "NoteGridView.chain(", "NoteGridView.tray(",
                "NoteGridView.requestAt(", "NoteGridView.inPlate(", "NoteGridView.inTray(",
                "NoteGridView.WINDOW_SPAN")) {
            assertTrue(source.contains(seam), "the screen stopped drawing through " + seam);
        }
        assertTrue(source.contains("for (Cell cell : NoteGridView.cells("),
                "the screen no longer draws one pass over every note cell");
        final String stripped = stripComments(source);
        for (final String number : MODEL_GEOMETRY) {
            assertFalse(Pattern.compile("(?<![\\w.])" + number + "(?![\\w.])").matcher(stripped).find(),
                    "the screen re-derives the geometry number " + number
                            + ": the grid drawn and the grid hit-tested can now drift apart");
        }
    }

    @Test
    void theScreensInkAndCopyTextStayTheOriginalsKeys() throws Exception {
        final String source = code(SCREEN);
        for (final String key : List.of("tile.researchtable.noink.0", "tile.researchtable.noink.1", "tc.research.copy")) {
            assertTrue(source.contains("\"" + key + "\""), "the screen dropped the original's key " + key);
        }
    }

    @Test
    void theGridModelIsCommonCode() throws Exception {
        final String source = code(MODEL);
        for (final String client : List.of("net.minecraft.client", "OnlyIn", "Minecraft.getInstance", "GuiGraphics")) {
            assertFalse(source.contains(client),
                    "the grid model names " + client + ": the write arm's arithmetic would stop shipping to a server");
        }
        final Path model = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic").resolve(MODEL);
        assertTrue(model.startsWith(repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/research")),
                "the grid model left the research package and its common-code contract");
    }

    // ------------------------------------------------------------------ helpers

    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");
        return csv.getParent().getParent();
    }

    private static String source(final String relative) throws Exception {
        final Path file = repoRoot().resolve("src/main/java/com/jonnyutah12/thaumcraftclassic").resolve(relative);
        assertTrue(Files.isRegularFile(file), "expected the source at " + file);
        return Files.readString(file);
    }

    /** The source with its comments removed, so prose references to numbers do not fail the scan. */
    private static String code(final String relative) throws Exception {
        return stripComments(source(relative));
    }

    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }
}
