package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
import com.jonnyutah12.thaumcraftclassic.research.book.BookDetailPanelLayout;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageLayout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BOOK-4 (t_341117b8): the per-entry detail panel, pinned structurally in the
 * plain JVM.
 *
 * <p>The pixels live in {@code ThaumonomiconScreen}, and {@code runGameTestServer}
 * is a dedicated server that never constructs a {@code Screen} (D-019 rule 4:
 * "TESTED" on a GUI row means the structure is measured, not that the render
 * was observed). What CAN be measured is the shape: the detail's data half lives
 * in a class that imports no Minecraft type (rule 2), the window draws the
 * lines that class computes and reads its unlock state out of the synced
 * server store (not a copy of its own), and the panel's chrome text lives in
 * the model so the window cannot invent research copy.</p>
 *
 * <p>The content of the panel — the edges, the tags, the state — is gated
 * against the shipped data in {@code ResearchDetailModelTest} and measured on
 * the live graph in {@code ThaumonomiconBookGameTest}.</p>
 */
class ThaumonomiconDetailPanelContractTest {

    private static final String DETAIL_MODEL_SOURCE = "research/ResearchDetailModel.java";
    private static final String SCREEN_SOURCE = "client/ThaumonomiconScreen.java";

    /** D-019 rule 2: the measurable half of the detail lives outside the Screen. */
    @Test
    void theDetailModelImportsNoMinecraftType() throws Exception {
        final String source = stripComments(readSource(DETAIL_MODEL_SOURCE));
        final List<String> offenders = new ArrayList<>();
        for (final String line : source.split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("import ")
                    && !trimmed.startsWith("import java.")
                    && !trimmed.startsWith("import com.jonnyutah12.thaumcraftclassic.")) {
                offenders.add(trimmed);
            }
        }
        assertTrue(offenders.isEmpty(),
                "the detail model must import no Minecraft/NeoForge/Mojang type, or the game probes lose the"
                        + " seam that lets them build the panel on a dedicated server (D-019 rule 2): " + offenders);
        assertTrue(source.contains("import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;"),
                "the panel reads the player's knowledge — the server's research store, not a copy");
        assertTrue(source.contains("ResearchManager.isResearchComplete")
                        && source.contains("ResearchManager.doesPlayerHaveRequisites"),
                "the unlock state must be ResearchManager's own pair of authorities (TC-M3-01/02), not a rule"
                        + " this card invents");
    }

    /** The window draws the model's lines and reads the synced store; it writes no research copy. */
    @Test
    void theWindowDrawsTheModelsLinesAndReadsTheServerStore() throws Exception {
        final String source = stripComments(readSource(SCREEN_SOURCE));
        assertTrue(source.contains("displayLines()"),
                "the panel must draw the lines the model computed, not text it composes itself");
        assertTrue(source.contains("ModAttachments.PLAYER_KNOWLEDGE"),
                "the panel's unlock state must come from the synced PLAYER_KNOWLEDGE attachment — the store the"
                        + " server gates completions on (no new store on this card)");
        assertTrue(source.contains("ResearchDetailModel"),
                "the screen wires the detail model (the class this card ships)");
        assertTrue(source.contains("selectEntry"),
                "a related-research row opens that entry (the original's node click, GuiResearchBrowser.java:711)");
        // No invented chrome: the panel's labels are the model's (and the
        // original's quoted strings), never literals in the window.
        for (final String invented : List.of("Hidden requirement:", "Requires:", "Related:",
                "Missing required research!", "Essentia required:", "Forbidden knowledge")) {
            assertFalse(source.contains("\"" + invented),
                    "\"" + invented + "\" belongs to the model (and to the original's en_US.lang, quoted under"
                            + " D-016), not to the window: the window adds no research copy");
        }
    }

    /** The screen wires the panel: the accessors, the click record, the mouse handler. */
    @Test
    void theScreenWiresTheDetailPanelStructurally() throws Exception {
        final Method details = ThaumonomiconScreen.class.getDeclaredMethod("details");
        assertSame(ResearchDetailModel.class, details.getReturnType(),
                "the screen exposes the detail resolver it draws");
        final Method selected = ThaumonomiconScreen.class.getDeclaredMethod("selectedDetail");
        assertTrue(ResearchDetailModel.Detail.class.isAssignableFrom(selected.getReturnType()),
                "the panel's content is the model's Detail record (null with nothing selected)");
        final Method knowledge = ThaumonomiconScreen.class.getDeclaredMethod("playerKnowledge");
        assertTrue(knowledge.getReturnType() == com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData.class
                        && java.lang.reflect.Modifier.isStatic(knowledge.getModifiers()),
                "the panel reads the player's knowledge through the attachment accessor");
        assertNotNull(ThaumonomiconScreen.class.getDeclaredMethod("mouseClicked", double.class, double.class, int.class),
                "the link rows need the click handler (the original's func_73864_a)");
        boolean hitRecord = false;
        for (final Class<?> nested : ThaumonomiconScreen.class.getDeclaredClasses()) {
            if (nested.isRecord() && "DetailHit".equals(nested.getSimpleName())
                    && java.util.Arrays.stream(nested.getRecordComponents())
                    .anyMatch(component -> "key".equals(component.getName()))) {
                hitRecord = true;
            }
        }
        assertTrue(hitRecord,
                "the panel records the link rows it drew as click targets (the window keeps no listing of its own)") ;
    }

    /** The panel's words are the original's, verbatim (D-016), with the source line on record. */
    @Test
    void thePanelWordsAreTheOriginalsOwnStrings() {
        assertEquals("Missing required research!", ResearchDetailModel.STATE_LOCKED_TEXT,
                "tc.researchmissing (reference/assets/thaumcraft/lang/en_US.lang:731)");
        assertEquals("You do not have the required aspects to purchase this",
                ResearchDetailModel.ASPECTS_SHORT_TEXT,
                "tc.research.short (en_US.lang:748)");
        assertEquals("Forbidden knowledge (%n)", ResearchDetailModel.FORBIDDEN_TEMPLATE,
                "tc.forbidden (en_US.lang:722), § codes dropped and %n filled by the panel");
        assertEquals("parents|parentsHidden|siblings",
                String.join("|", ResearchDetailModel.linkKinds()),
                "the link kinds are the graph's own field names, in the order the panel draws them");
    }

    // ------------------------------ TC-M3-04-s-iv-b-2: the panel's recipe body (BD-64)

    /**
     * The panel splits its own box: a region at the bottom for the row's picture,
     * the kept text rows above it, and every number is a function of the box the
     * screen hands it (the split tracks the text pitch, so a font change moves it).
     *
     * <p>Fed the shipped panel — {@code DETAIL_WIDTH} wide, from
     * {@code ROWS_TOP - 2} to {@code PANE_HEIGHT - 4}, at {@code DETAIL_PITCH} —
     * the region is 100x146, the original's 104x156 box fits it scaled down
     * (never up), and the KNOWFRAG 3x3 plus its result and label land inside the
     * plate. The items stay native size and each offset rounds on its own
     * (BD-17's delta, recorded in {@code RecipePageLayout}), which puts the last
     * row's 18 px slot square one pixel past the fitted box: inside the plate's
     * inset and inside the pane, so nothing is clipped.</p>
     */
    @Test
    void thePanelReservesARegionAtItsBottomForTheRecipeBody() {
        final int pitch = ThaumonomiconScreen.DETAIL_PITCH;
        final int right = ThaumonomiconScreen.DETAIL_LEFT + ThaumonomiconScreen.DETAIL_WIDTH;
        final BookDetailPanelLayout panel = new BookDetailPanelLayout(
                ThaumonomiconScreen.DETAIL_LEFT, ThaumonomiconScreen.ROWS_TOP - 2,
                ThaumonomiconScreen.DETAIL_WIDTH, ThaumonomiconScreen.PANE_HEIGHT - 4, pitch);

        assertEquals(panel.top() + BookDetailPanelLayout.TEXT_TOP_PAD
                        + BookDetailPanelLayout.TEXT_ROWS_KEPT * pitch + BookDetailPanelLayout.BODY_GAP,
                panel.bodyTop(), "the body starts under the kept text rows");
        assertTrue(panel.bodyTop() - panel.top() >= BookDetailPanelLayout.TEXT_ROWS_KEPT * pitch,
                "the kept rows are above the body, not under it");
        assertEquals(panel.bottom(), panel.textBottom(), "with no body the text keeps the whole panel");
        assertEquals(panel.bodyTop(), panel.textBottomWithBody(),
                "with a body the text stops where the body starts");
        assertTrue(panel.bodyFits(), "the shipped panel must have room for the block, got a "
                + panel.bodyHeight() + " px region for a "
                + panel.bodyLayout().size(RecipePageLayout.BLOCK_HEIGHT) + " px block");

        final RecipePageLayout layout = panel.bodyLayout();
        assertTrue(layout.scale() > RecipePageLayout.MIN_SCALE && layout.scale() <= 1.0F,
                "the block fits the panel scaled down from the original's box, never enlarged, got "
                        + layout.scale() + " for a " + RecipePageLayout.BLOCK_WIDTH + "x"
                        + RecipePageLayout.BLOCK_HEIGHT + " block in " + panel.width() + "x"
                        + panel.bodyHeight());
        assertEquals(Math.round(RecipePageLayout.BLOCK_WIDTH * layout.scale()),
                panel.bodyLayout().size(RecipePageLayout.BLOCK_WIDTH), "the frame is the scaled box");
        assertTrue(layout.x(0) >= panel.left()
                        && layout.x(0) + layout.size(RecipePageLayout.BLOCK_WIDTH) <= right + 1,
                "the block's frame is centred in the panel column: " + layout.x(0) + " + "
                        + layout.size(RecipePageLayout.BLOCK_WIDTH) + " in " + panel.left() + " .. " + right);
        assertTrue(layout.y(0) >= panel.bodyTop()
                        && layout.y(0) + layout.size(RecipePageLayout.BLOCK_HEIGHT) <= panel.bottom(),
                "the block's frame is inside the region it was given");

        for (int col = 0; col < RecipePageLayout.MAX_GRID; col++) {
            assertTrue(layout.cellX(col) >= panel.left()
                            && layout.cellX(col) + RecipePageLayout.ITEM <= right,
                    "grid column " + col + "'s item draws outside the panel column");
            assertTrue(layout.slotX(col) >= panel.left() - 1
                            && layout.slotX(col) + layout.slotSize() <= right + 1,
                    "grid column " + col + "'s slot square draws outside the panel column");
        }
        for (int row = 0; row < RecipePageLayout.MAX_GRID; row++) {
            assertTrue(layout.cellY(row) >= panel.bodyTop()
                            && layout.cellY(row) + RecipePageLayout.ITEM <= panel.bottom() + 1,
                    "grid row " + row + "'s item draws outside the region");
            assertTrue(layout.slotY(row) >= panel.bodyTop() - 1
                            && layout.slotY(row) + layout.slotSize() <= ThaumonomiconScreen.PANE_HEIGHT,
                    "grid row " + row + "'s slot square draws outside the pane");
        }
        assertTrue(layout.resultX() >= panel.left()
                        && layout.resultX() + RecipePageLayout.ITEM <= right
                        && layout.resultY() >= panel.bodyTop()
                        && layout.resultY() + RecipePageLayout.ITEM <= panel.bottom(),
                "the result beside the grid draws outside the panel");
        assertTrue(layout.headerX(40) >= panel.left() && layout.headerX(40) + 40 <= right,
                "the station label, centred on the original's centre line, draws outside the panel");
    }

    /**
     * The panel with no room for a picture shows its text body: the block is never
     * scaled below {@link RecipePageLayout#MIN_SCALE}, so the region gives, and the
     * text column is the whole panel again — the same shape as an entry with no
     * linked row and a row the live {@code RecipeManager} does not carry.
     */
    @Test
    void thePanelWithNoRoomForTheBlockKeepsItsTextBody() {
        final int pitch = ThaumonomiconScreen.DETAIL_PITCH;
        final BookDetailPanelLayout cramped = new BookDetailPanelLayout(
                0, 0, panelWidth() / 5,
                BookDetailPanelLayout.TEXT_TOP_PAD + BookDetailPanelLayout.TEXT_ROWS_KEPT * pitch, pitch);
        assertTrue(cramped.bodyHeight() < 0,
                "the kept text already reaches the panel's bottom, got " + cramped.bodyHeight());
        assertEquals(RecipePageLayout.MIN_SCALE, cramped.bodyScale(), 0.0F,
                "the fit floors at MIN_SCALE: a picture is never scaled into unreadability");
        assertTrue(!cramped.bodyFits(), "no room for the block: the panel must draw its text body");
        assertTrue(cramped.bodyLayout().size(RecipePageLayout.BLOCK_HEIGHT) > cramped.bodyHeight(),
                "the block at its floor is taller than the region, which is why the region gives");
        assertEquals(cramped.bottom(), cramped.textBottom(), "the text keeps the whole panel");
    }

    /**
     * The window draws the body the panel reserves, and it is the only place a
     * recipe type is touched: the server-safe half — the detail record, the page
     * state, the panel geometry, the join — carries the row's <b>id</b> and imports
     * no Minecraft type, so the game probes build the panel on a dedicated server
     * (D-019 rule 2) and the picture can only come from the client's live
     * {@code RecipeManager}.
     */
    @Test
    void theWindowResolvesTheRowIdTheServerSafeHalfCarries() throws Exception {
        final String source = stripComments(readSource(SCREEN_SOURCE));
        assertTrue(source.contains("BookDetailPanelLayout"),
                "the window splits the panel through the record that owns the geometry");
        assertTrue(source.contains("textBottomWithBody()") && source.contains("bodyFits()"),
                "the text stops at the body, and the record decides when there is no room for one");
        assertTrue(source.contains("renderRecipeBody"), "the window draws the row's picture");
        assertTrue(source.contains("RecipePageDisplay.resolve(") && source.contains("getRecipeManager()"),
                "the picture is read out of the live RecipeManager, in the window");
        assertTrue(source.contains("detail.recipeId()"),
                "the window asks the detail for the row id; it composes no link of its own");
        assertTrue(source.contains("SLOT_SPRITE") && source.contains("renderTooltip"),
                "vanilla slot framing and the original's ingredient tooltip (BD-17, GuiResearchRecipe.java:807-818, 866-884)");
        assertFalse(source.contains("net.minecraft.world.item.crafting"),
                "the window touches no recipe type: RecipePageDisplay is the seam");
        for (final String serverSafe : List.of("research/ResearchDetailModel.java",
                "research/book/BookPageView.java", "research/book/BookDetailPanelLayout.java",
                "research/book/ResearchRecipeLinks.java")) {
            final String stripped = stripComments(readSource(serverSafe));
            assertFalse(stripped.contains("net.minecraft"),
                    serverSafe + " is the measurable half of the page body and must import no Minecraft"
                            + " type — it carries the row's id, never a recipe (D-019 rule 2)");
        }
        assertTrue(java.util.Arrays.stream(ResearchDetailModel.Detail.class.getRecordComponents())
                        .anyMatch(component -> "recipeId".equals(component.getName())
                                && component.getType() == String.class),
                "the detail carries the row's id as a String, so the page state stays id-only");
    }

    /** The panel's column, as the shipped screen sizes it. */
    private static int panelWidth() {
        return ThaumonomiconScreen.DETAIL_WIDTH;
    }

    /** The comments stripped, so the scans above target the executable code. */
    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    /** The source of a shipped class, read from the repo the harness runs in. */
    private static String readSource(final String relativeUnderModPackage) throws Exception {
        final String suffix = "src/main/java/com/jonnyutah12/thaumcraftclassic/" + relativeUnderModPackage;
        Path source = Path.of(System.getProperty("user.dir")).toAbsolutePath().resolve(suffix);
        Path walk = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && !Files.isRegularFile(source); i++) {
            source = walk.resolve(suffix);
            walk = walk.getParent();
        }
        assertTrue(Files.isRegularFile(source), "expected the source at " + source);
        return Files.readString(source, java.nio.charset.StandardCharsets.UTF_8);
    }
}
