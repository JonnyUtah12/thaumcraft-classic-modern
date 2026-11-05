package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the research-note container (TC-M3-04-s-iii-b) to the decompiled 1.7.10
 * original instead of to itself: every number and gate order in
 * {@link ResearchNoteMenu} is re-read out of the reference source at test time,
 * so a drift in either tree is caught here.
 *
 * <p>The assertions read the text, not a transcription, and each one names the
 * file it read. The reference tree is a local decompile checkout and is not in
 * the repo, so every assertion in this class assumption-skips when it is
 * absent (the CI {@code build} job has no {@code reference/}) — the skip path is
 * the one {@code ReferenceOrigins} documents, and the game probes
 * ({@code ResearchNoteMenuGameTest}) carry the behaviour into CI.</p>
 *
 * <ul>
 *   <li>the slot layout: {@code ContainerResearchTable.java:19-20,29-36}</li>
 *   <li>the slot gates: {@code SlotLimitedByClass.java:22-24} — a class check
 *   with no metadata and no completeness test</li>
 *   <li>the button arms: {@code ContainerResearchTable.java:39-47}</li>
 *   <li>the write arm's ink gate, its two ink calls and its meta-64 completion
 *   stamp: {@code TileResearchTable.java:122-189}</li>
 *   <li>the copy arm's gates and the fact that it consumes NO ink:
 *   {@code TileResearchTable.java:425-462}</li>
 *   <li>the ink rule itself: {@code ResearchManager.java:220-230}</li>
 * </ul>
 */
class ResearchNoteMenuParityTest {

    private static final String CONTAINER =
            "decompiled/thaumcraft/thaumcraft/common/container/ContainerResearchTable.java";
    private static final String SLOT_LIMIT =
            "decompiled/thaumcraft/thaumcraft/common/container/SlotLimitedByClass.java";
    private static final String TILE =
            "decompiled/thaumcraft/thaumcraft/common/tiles/TileResearchTable.java";
    private static final String MANAGER =
            "decompiled/thaumcraft/thaumcraft/common/lib/research/ResearchManager.java";
    private static final String GUI =
            "decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java";

    // -------------------------------------------------------------- layout

    @Test
    void theTableSlotsSitAtTheOriginalsCoordinates() {
        final String src = ReferenceOrigins.read(CONTAINER);
        final int[] scribe = firstInts(
                "new SlotLimitedByClass\\(IScribeTools\\.class,\\s*\\w+,\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)", src,
                CONTAINER + ":19 (the scribe slot)");
        final int[] note = firstInts(
                "new SlotLimitedByClass\\(ItemResearchNotes\\.class,\\s*\\w+,\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)", src,
                CONTAINER + ":20 (the note slot)");

        assertEquals(0, scribe[0], "the original's scribe slot index");
        assertEquals(ResearchNoteMenu.SCRIBE_SLOT_X, scribe[1],
                CONTAINER + ":19 puts the scribe slot at x " + scribe[1]);
        assertEquals(ResearchNoteMenu.SCRIBE_SLOT_Y, scribe[2],
                CONTAINER + ":19 puts the scribe slot at y " + scribe[2]);
        assertEquals(1, note[0], "the original's note slot index");
        assertEquals(ResearchNoteMenu.NOTE_SLOT_X, note[1],
                CONTAINER + ":20 puts the note slot at x " + note[1]);
        assertEquals(ResearchNoteMenu.NOTE_SLOT_Y, note[2],
                CONTAINER + ":20 puts the note slot at y " + note[2]);
        assertEquals(ResearchNoteMenu.SLOT_SCRIBE, scribe[0], "the port's scribe slot index");
        assertEquals(ResearchNoteMenu.SLOT_NOTE, note[0], "the port's note slot index");
    }

    @Test
    void thePlayerRowsUseTheOriginalsPanelWidth() {
        final String src = ReferenceOrigins.read(CONTAINER);
        final int[] rows = firstInts(
                "new Slot\\(inventoryPlayer, j \\+ i \\* 9 \\+ 9, (\\d+) \\+ j \\* (\\d+), (\\d+) \\+ i \\* (\\d+)\\)",
                src, CONTAINER + ":31 (the three main rows)");
        final int[] hotbar = firstInts(
                "new Slot\\(inventoryPlayer, i, (\\d+) \\+ i \\* (\\d+), (\\d+)\\)",
                src, CONTAINER + ":35 (the hotbar row)");

        // The deconstruction table's rows start at x 8; this panel's start at 48,
        // and a port that copies the neighbouring menu's arithmetic lands its
        // slots under the hex grid.
        assertEquals(ResearchNoteMenu.PLAYER_SLOT_X, rows[0],
                CONTAINER + ":31 starts the player rows at x " + rows[0]);
        assertEquals(ResearchNoteMenu.PLAYER_SLOT_STEP, rows[1], CONTAINER + ":31 column pitch");
        assertEquals(ResearchNoteMenu.PLAYER_ROW_Y, rows[2], CONTAINER + ":31 first row y");
        assertEquals(ResearchNoteMenu.PLAYER_SLOT_STEP, rows[3], CONTAINER + ":31 row pitch");
        assertEquals(ResearchNoteMenu.PLAYER_SLOT_X, hotbar[0], CONTAINER + ":35 hotbar x");
        assertEquals(ResearchNoteMenu.PLAYER_SLOT_STEP, hotbar[1], CONTAINER + ":35 hotbar pitch");
        assertEquals(ResearchNoteMenu.HOTBAR_Y, hotbar[2], CONTAINER + ":35 hotbar y");
    }

    // ----------------------------------------------------------------- gates

    @Test
    void theSlotGateIsAClassCheckAndNothingElse() {
        final String gate = ReferenceOrigins.read(SLOT_LIMIT);
        final String body = methodBody(gate, "func_75214_a");
        assertTrue(body.contains("isAssignableFrom"),
                "SlotLimitedByClass.isItemValid is not a class check: " + body);
        assertFalse(body.contains("func_77960_j"),
                "the original's slot gate reads metadata, so the port's gate needs it too: " + body);

        final String port = portSource();
        final String noteGate = between(port, "new Slot(this.container, SLOT_NOTE", "addDataSlots");
        assertTrue(noteGate.contains("instanceof ItemResearchNotes"),
                "the port's note slot is not a class check");
        assertFalse(noteGate.contains("META_DISCOVERY_FAMILY") || noteGate.contains("complete()"),
                "the port's note slot gates metadata or completion, which the original's slot does not: "
                        + "the meta gate is TileResearchTable.java:133 (the write arm)");
    }

    @Test
    void theButtonArmsAreExactlyTheOnesTheOriginalHas() {
        final String src = ReferenceOrigins.read(CONTAINER);
        final String arm = methodBody(src, "func_75140_a");
        final Set<Integer> ids = new LinkedHashSet<>();
        final Matcher m = Pattern.compile("button == (\\d+)").matcher(arm);
        while (m.find()) {
            ids.add(Integer.parseInt(m.group(1)));
        }
        assertEquals(Set.of(ResearchNoteMenu.BUTTON_KEEP_OPEN, ResearchNoteMenu.BUTTON_DUPLICATE), ids,
                CONTAINER + ":39-47 has arms for " + ids);
        assertTrue(arm.contains("tileEntity.duplicate"),
                CONTAINER + ":44 routes button 5 to TileResearchTable.duplicate");

        final String port = portSource();
        final String portArm = between(port, "public boolean clickMenuButton", "// ------------------------------------------------------- container data");
        assertTrue(portArm.contains("BUTTON_KEEP_OPEN") && portArm.contains("BUTTON_DUPLICATE"),
                "the port's button arms do not cover the original's two ids");
        assertFalse(Pattern.compile("id == \\d+").matcher(portArm).find(),
                "the port added a button id the original does not have");
    }

    // ------------------------------------------------------------- the arms

    @Test
    void theWriteArmIsInkGatedTwiceAndStampsTheCompletedMeta() {
        final String src = ReferenceOrigins.read(TILE);
        final String arm = methodBody(src, "placeAspect");
        assertEquals(2, count(arm, "consumeInkFromTable"),
                "the original's write arm calls consumeInkFromTable twice (the gate and the spend, :127,:176)");
        assertTrue(arm.contains("func_77960_j() < 64"),
                "the original's write arm does not gate metadata 64: " + arm.substring(0, 40));
        assertTrue(arm.contains("func_77964_b(64)"),
                "the original's completion does not stamp meta 64 (:178)");
        assertTrue(arm.contains("checkResearchCompletion"),
                "the original's completion check is not in the write arm");

        final String port = portSource();
        final String portArm = between(port, "public void placeAspect", "private PlayerKnowledgeData knowledge()");
        assertEquals(2, count(portArm, "consumeInk(") + count(portArm, "hasInk()"),
                "the port's write arm must gate ink and then spend it");
        assertTrue(portArm.contains("META_DISCOVERY_FAMILY"),
                "the port's write arm does not refuse the completed family");
        assertTrue(between(port, "public static void applyCompletion", "private PlayerKnowledgeData knowledge()")
                        .contains("META_DISCOVERY_FAMILY"),
                "the port's completion arm does not stamp the original's meta 64 (TileResearchTable.java:178)");
        assertEquals(ItemResearchNotes.META_DISCOVERY_FAMILY, 64, "the original's stamp is the literal 64");
    }

    @Test
    void theCopyArmHasNoInkGateAndRequiresTheCompletedMeta() {
        final String src = ReferenceOrigins.read(TILE);
        final String arm = methodBody(src, "duplicate");
        assertFalse(arm.contains("consumeInk"),
                "the original's copy arm consumes ink — a port that gates it on ink is inventing a gate");
        assertTrue(arm.contains("func_77960_j() == 64"),
                "the original's copy arm does not require meta 64 (:434)");
        assertTrue(arm.contains("field_151121_aF") && arm.contains("field_151100_aR"),
                "the original's copy arm does not require a book and a coal (:435-436)");
        assertTrue(arm.contains(".copies"), "the original's copy arm does not bump the copy count");
        assertTrue(arm.contains("field_77994_a++"),
                "the original's copy arm does not grow the stack in the slot (:459)");

        final String port = portSource();
        final String portArm = between(port, "public void duplicate()",
                "/**\n     * Original {@code ContainerResearchTable.java:39-47}");
        assertFalse(portArm.contains("consumeInk(") || portArm.contains("hasInk("),
                "the port put an ink gate on the copy arm, which the original does not have");
        assertTrue(portArm.contains("META_DISCOVERY_FAMILY"),
                "the port's copy arm does not require the completed meta");
        assertTrue(portArm.contains("countItem(Items.BOOK)") && portArm.contains("countItem(Items.COAL)"),
                "the port's copy arm does not require a book and a coal");
        assertTrue(portArm.contains("setCount(noteStack.getCount() + 1)"),
                "the port's copy arm does not grow the note in the slot (the original's stackSize++)");

        // The plate's gate is the GUI's: note + RESEARCHDUPE + complete, and no ink.
        final String gui = ReferenceOrigins.read(GUI);
        assertTrue(gui.contains("RESEARCHDUPE && this.note.isComplete()"),
                "the original's copy plate is not gated on RESEARCHDUPE + complete (:104,:254)");
        assertTrue(between(port, "public boolean canDuplicate()", "public Map<String, Integer> copyCosts()")
                        .contains("RESEARCHDUPE"),
                "the port's plate gate does not test RESEARCHDUPE");
    }

    @Test
    void theInkRuleIsTheOriginalsDamageComparison() {
        final String src = ReferenceOrigins.read(MANAGER);
        final String arm = methodBody(src, "consumeInkFromTable");
        assertTrue(arm.contains("instanceof IScribeTools"), "the ink gate is not the marker check (:221)");
        assertTrue(arm.contains("func_77960_j() < func_77958_k()"),
                "the ink gate is not damage < maxDamage (:221)");
        assertTrue(arm.contains("func_77960_j() + 1"), "the committed write does not spend one point (:223)");

        final String port = portSource();
        final String portArm = between(port, "public static boolean consumeInk(",
                "/** The ink gate alone, with no spend");
        assertTrue(portArm.contains("instanceof IScribeTools"), "the port's ink gate is not the marker check");
        assertTrue(portArm.contains("getDamageValue() >= getMaxDamage()"),
                "the port's ink gate is not the inverse of damage < maxDamage");
        assertTrue(portArm.contains("getDamageValue() + 1"), "the port's commit does not spend one point");
    }

    // -------------------------------------------------------------- helpers

    /** The three ints of the first match, in group order. */
    private static int[] firstInts(final String regex, final String src, final String citation) {
        final Matcher m = Pattern.compile(regex).matcher(src);
        assertTrue(m.find(), "the decompiled text no longer matches " + citation);
        final int[] out = new int[m.groupCount()];
        for (int i = 0; i < out.length; i++) {
            out[i] = Integer.parseInt(m.group(i + 1));
        }
        return out;
    }

    /** A decompiled method's text, from its signature to its closing brace at 3-space indent. */
    private static String methodBody(final String src, final String name) {
        final Matcher m = Pattern.compile("(?s)public [^{]*\\b" + Pattern.quote(name) + "\\b[^{]*\\{.*?\n   }")
                .matcher(src);
        assertTrue(m.find(), "the decompiled text has no method named " + name);
        return m.group();
    }

    private static String between(final String src, final String from, final String to) {
        final int a = src.indexOf(from);
        assertTrue(a >= 0, "the port's source no longer contains: " + from);
        final int b = src.indexOf(to, a);
        assertTrue(b > a, "the port's source no longer contains: " + to);
        return src.substring(a, b);
    }

    private static int count(final String haystack, final String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    /** The shipped menu's source, resolved off the content-mapping path the build passes to tests. */
    private static String portSource() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path source = csv.getParent().getParent().resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/menus/ResearchNoteMenu.java");
        assertTrue(java.nio.file.Files.isRegularFile(source), "expected the menu source at " + source);
        try {
            return java.nio.file.Files.readString(source);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
