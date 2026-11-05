package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
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
