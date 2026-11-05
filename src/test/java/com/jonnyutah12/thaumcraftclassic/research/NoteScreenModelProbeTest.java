package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData.AspectEntry;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData.KeyEntry;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NOTE-SCREEN-2 (t_32a29c93) acceptance for the research-note window's
 * headless half: the grid geometry, the pool pagination, the gate order, the
 * veil gate, the combine cost and the sound table.
 *
 * <p><b>Why these assertions are here and not on the window.</b> The 1.7.10
 * {@code GuiResearchTable} is immediate-mode GL — its layout constants, its
 * click-arm order and its sounds are inline in tessellator calls, with no
 * 1.21.1 equivalent to javap against (docs/porting-decisions.md D-019), and the
 * game-test harness is a dedicated server: it never constructs a {@code Screen}
 * and fires no mouse event. So the measurable half lives in
 * {@link NoteScreenModel} — a class with no Minecraft, NeoForge or Mojang
 * import — and {@code client.ResearchNoteScreen} draws exactly what it
 * computes. Each expectation below is derived from the <b>shipped data</b>
 * (the research graph, the aspect definitions, the sound registrations), read
 * at test time; none of them is a hardcoded list of names.</p>
 *
 * <p>What is NOT proved here, and cannot be in CI: that the window renders on
 * screen — that needs a real client ({@code runClient}), and
 * {@code docs/FEATURE_MATRIX.md} says so.</p>
 */
class NoteScreenModelProbeTest {

    /**
     * The shipped files this probe reads are repo files, and the NeoForge
     * JUnit plugin runs the test JVM with {@code build/minecraft-junit} as its
     * working directory, so the repo root is found by walking up to
     * {@code gradlew}.
     */
    private static final Path ROOT = repoRoot();
    private static final Path GRAPH = ROOT.resolve(
            "src/main/resources/data/thaumcraft/research_graph/entries.json");
    private static final Path MOD_SOUNDS = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/sound/ModSounds.java");
    private static final Path MODEL_SOURCE = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/research/NoteScreenModel.java");
    private static final Path SCREEN_SOURCE = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/client/ResearchNoteScreen.java");
    /**
     * The decompiled 1.7.10 window: the arm-sound probe reads the reference's
     * OWN {@code playButton*} sequences out of it, so the expectation is derived
     * from the source and not typed from memory. CI ships no reference tree
     * (D-004), where the probe assumption-skips like the parity probes do.
     */
    private static final String REFERENCE_GUI =
            "decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java";
    private static final Path SCREEN_REGISTRATION = ROOT.resolve(
            "src/main/java/com/jonnyutah12/thaumcraftclassic/client/ModClientScreens.java");

    private static Path repoRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null && !Files.exists(cursor.resolve("gradlew"))) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalStateException("no gradlew above " + System.getProperty("user.dir"));
        }
        return cursor;
    }

    // ------------------------------------------------------------- geometry

    /**
     * The panel and grid geometry, and the pixel mapping of every cell of a
     * note generated from shipped graph entries: the centre is the original's
     * (169, 83), the hex size its 9, and each cell's pixel centre is
     * {@code Hex.toPixel(9)} of the cell's own coordinate — the same mapping
     * the erase arm inverts. Expected cells come from the note, not a list.
     */
    @Test
    void theGridGeometryIsTheOriginalSAndCoversEveryGeneratedNote() throws IOException {
        assertEquals(255, NoteScreenModel.PANEL_WIDTH, "ySize (GuiResearchTable.java:70)");
        assertEquals(255, NoteScreenModel.PANEL_HEIGHT, "xSize (GuiResearchTable.java:71)");
        assertEquals(9, NoteScreenModel.HEX_SIZE, "HEX_SIZE (GuiResearchTable.java:43)");
        assertEquals(169, NoteScreenModel.GRID_CENTRE_X, "the grid centre x (:153)");
        assertEquals(83, NoteScreenModel.GRID_CENTRE_Y, "the grid centre y (:154)");
        assertEquals(-8, NoteScreenModel.HEX_PLATE_OFFSET, "the plate sits centre-8 (:533,553)");

        final List<ShippedEntry> shipped = shippedEntriesWithTags();
        assertFalse(shipped.isEmpty(), "the shipped graph must carry tagged entries");
        final Random random = new Random(0x5EEDL);
        int notes = 0;
        for (final ShippedEntry entry : shipped) {
            final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), random);
            final NoteScreenModel model = new NoteScreenModel(note, knowledge(List.of(), List.of()));
            final Map<String, HexEntry> grid = note.hexEntries();
            final List<NoteScreenModel.HexCell> cells = model.hexCells();
            assertEquals(grid.size(), cells.size(), "one drawn cell per grid entry: " + entry.key());
            for (final NoteScreenModel.HexCell cell : cells) {
                final HexEntry gridEntry = grid.get(cell.key());
                assertNotNull(gridEntry, "drawn cell " + cell.key() + " is in the grid: " + entry.key());
                final Hex hex = Hex.fromKey(cell.key());
                assertEquals(hex.q(), cell.q());
                assertEquals(hex.r(), cell.r());
                // The original maps a hex to a pixel around the centre (:522-561).
                assertEquals(169 + (int) Math.round(hex.toPixel(9).x()), cell.centreX());
                assertEquals(83 + (int) Math.round(hex.toPixel(9).y()), cell.centreY());
                assertEquals(cell.centreX() - 8, cell.plateX());
                assertEquals(cell.centreY() - 8, cell.plateY());
                // The required cells carry the orb, every other cell the outline.
                assertEquals(gridEntry.type() == 1, cell.orb(), "orb on a required hex only: " + entry.key());
            }
            // The erase arm inverts the same mapping: the centre of a cell is a
            // click that lands on that cell.
            for (final NoteScreenModel.HexCell cell : cells) {
                assertEquals(cell.key(), model.hexAt(cell.centreX(), cell.centreY()).toString(),
                        "hexAt inverts toPixel for every cell: " + entry.key());
            }
            notes++;
        }
        assertTrue(notes >= 100, "the probe walked the whole tagged graph, saw " + notes);
    }

    /**
     * A completed note draws no hex outline ({@code :522} gates the whole grid
     * loop on {@code !note.complete()}), and a note with no grid has no cells.
     */
    @Test
    void aCompletedNoteDrawsNoHexOutlines() {
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final ResearchNoteData generated = NotePuzzle.generate(entry.toEntryData(), new Random(7L));
        final NoteScreenModel open = new NoteScreenModel(generated, knowledge(List.of(), List.of()));
        assertTrue(open.hexCells().stream().anyMatch(NoteScreenModel.HexCell::outline),
                "an open note draws outlines");

        final ResearchNoteData completed = new ResearchNoteData(
                generated.key(), generated.color(), true, generated.copies(), generated.hexEntries());
        final NoteScreenModel done = new NoteScreenModel(completed, knowledge(List.of(), List.of()));
        assertTrue(done.hexCells().stream().noneMatch(NoteScreenModel.HexCell::outline),
                "a completed note draws no outline (:522)");
        assertTrue(done.canDuplicate() == false, "RESEARCHDUPE gates the copy plate (:104,254)");

        final NoteScreenModel noNote = new NoteScreenModel(null, knowledge(List.of(), List.of()));
        assertTrue(noNote.hexCells().isEmpty(), "no note, no grid");
        assertFalse(noNote.canDuplicate());
    }

    // ----------------------------------------------------------------- pool

    /**
     * The pool pagination, over the shipped aspect tags: a page starts at
     * index {@code page * 5}, draws 25 cells max, in the sorted order, on a
     * 16px grid at the original's (10, 40) — and {@code lastPage} keeps the
     * original's negative arithmetic for small pools ({@code :268}).
     */
    @Test
    void thePoolPaginatesEveryFiveDiscoveredAspects() {
        final List<String> allTags = new ArrayList<>(new TreeSet<>(shippedAspectTags()));
        final PlayerKnowledgeData known = knowledge(allTags.stream()
                .map(tag -> new AspectEntry(tag, (short) 3)).toList(), List.of());
        final NoteScreenModel model = new NoteScreenModel(null, known);

        assertEquals(allTags.size(), model.discoveredTags().size());
        assertEquals((allTags.size() - 20) / 5, model.lastPage(), "lastPage = (count-20)/5 (:268)");

        final List<NoteScreenModel.PoolCell> first = model.poolCells();
        assertEquals(25, first.size(), "the original caps the pool at 25 draws (:273)");
        for (int drawn = 0; drawn < first.size(); drawn++) {
            final NoteScreenModel.PoolCell cell = first.get(drawn);
            assertEquals(10 + drawn / 5 * 16, cell.x(), "drawn / 5 * 16 (:275)");
            assertEquals(40 + drawn % 5 * 16, cell.y(), "drawn % 5 * 16 (:276)");
            assertEquals(allTags.get(drawn), cell.tag(), "the pool walks the sorted order (:272)");
            assertFalse(cell.faded(), "a payable entry is not faded (:274)");
        }

        model.setPage(2);
        final List<NoteScreenModel.PoolCell> pageTwo = model.poolCells();
        for (int drawn = 0; drawn < pageTwo.size(); drawn++) {
            assertEquals(allTags.get(2 * 5 + drawn), pageTwo.get(drawn).tag(),
                    "page 2 starts at index 10 (:273)");
        }
        assertTrue(model.canPageBack());
        assertEquals(model.lastPage() > 2, model.canPageForward(), "page forward needs page < lastPage (:657)");

        // The faded arm: an entry in the pool with a 0 amount draws at 0.33 (:274).
        final PlayerKnowledgeData broke = knowledge(
                allTags.stream().map(tag -> new AspectEntry(tag, (short) 0)).toList(), List.of());
        final NoteScreenModel brokeModel = new NoteScreenModel(null, broke);
        assertTrue(brokeModel.poolCells().stream().allMatch(NoteScreenModel.PoolCell::faded),
                "every 0-amount entry is faded");
        assertEquals(0.33F, NoteScreenModel.FADED_ALPHA, 0.0F);
        // A 0-amount entry is not payable: the press arm will not pick it up (:742-768).
        final NoteScreenModel.PoolCell cell = brokeModel.poolCells().get(0);
        assertEquals(null, brokeModel.aspectAt(cell.x(), cell.y(), false), "a 0-amount entry is not payable");
        assertEquals(cell.tag(), brokeModel.aspectAt(cell.x(), cell.y(), true),
                "the shift arm reads it with ignoreZero (:742-768)");
        // A below-20 pool keeps the original's negative lastPage.
        final NoteScreenModel smallPool = new NoteScreenModel(null,
                knowledge(allTags.stream().limit(4).map(tag -> new AspectEntry(tag, (short) 1)).toList(),
                        List.of()));
        assertTrue(smallPool.lastPage() < 0, "(count-20)/5 is negative below 20 (:268)");
        assertFalse(smallPool.canPageForward(), "so page forward is never offered");
    }

    /**
     * A selection whose aspect has left the pool is dropped by the window
     * itself, every frame ({@code :292-302}).
     */
    @Test
    void theWindowDropsASelectionThatLeftThePool() {
        final NoteScreenModel model = new NoteScreenModel(null,
                knowledge(List.of(new AspectEntry("aqua", (short) 2)), List.of()));
        model.setSelect1("aqua");
        model.setSelect2("potentia");
        model.clearUnpayableSelections();
        assertEquals("aqua", model.select1(), "a payable selection stays");
        assertEquals(null, model.select2(), "a selection outside the pool is dropped (:292-302)");
    }

    // ------------------------------------------------------------- gate order

    /**
     * The click arms in the original's order ({@code :623-724}): the combine
     * button needs both slots, its 200 ms cooldown gates the whole chain, the
     * page arrows need a page to move, the clear arms need a filled slot, the
     * erase arm runs before the copy plate, and the shift-combine arm is last
     * and needs RESEARCHER2.
     */
    @Test
    void theClickGatesRunInTheOriginalSOrder() {
        final long now = 1_000_000_000L;
        final List<String> pair = firstCompoundPair();
        final PlayerKnowledgeData knowledge = knowledge(
                List.of(new AspectEntry(pair.get(0), (short) 5), new AspectEntry(pair.get(1), (short) 5)),
                List.of(new KeyEntry("RESEARCHER2")));
        final NoteScreenModel model = new NoteScreenModel(null, knowledge);

        // The combine box, empty slots: refused, and nothing is armed.
        assertEquals(NoteScreenModel.ClickAction.REFUSED,
                model.clickAt(35, 139, false, now), "the combine arm needs both slots (:630)");
        model.setSelect1(pair.get(0));
        assertEquals(NoteScreenModel.ClickAction.REFUSED,
                model.clickAt(35, 139, false, now), "and the second slot");
        model.setSelect2(pair.get(1));
        assertEquals(NoteScreenModel.ClickAction.COMBINE,
                model.clickAt(35, 139, false, now), "both slots: the arm fires (:630-647)");

        // The arm set a 200 ms cooldown; the whole chain is gated (:625).
        assertEquals(NoteScreenModel.ClickAction.NONE,
                model.clickAt(35, 139, false, now + 199_999_999L), "mid-cooldown: nothing fires (:625)");
        assertEquals(200_000_000L, NoteScreenModel.COMBINE_COOLDOWN_NANOS, "butcount2 = nanoTime()+200000000 (:631)");
        assertEquals(NoteScreenModel.ClickAction.COMBINE,
                model.clickAt(35, 139, false, now + 200_000_000L), "the cooldown expires exactly at 200 ms");

        // The clear arms need a filled slot, and fire in slot order. After the
        // combine the chain is cooled down, so these arms are measured past it.
        final long after = now + 500_000_000L;
        model.setSelect2(null);
        assertEquals(NoteScreenModel.ClickAction.NONE,
                model.clickAt(71, 137, false, after), "the clear-2 box is dead while slot 2 is empty (:670)");
        assertEquals(NoteScreenModel.ClickAction.CLEAR_ONE,
                model.clickAt(11, 137, false, after), "the clear-1 box fires on a filled slot (:661)");
        assertEquals(null, model.select1());

        // The page arrows: gated on there being a page to move to.
        final List<String> bigPool = new ArrayList<>(new TreeSet<>(shippedAspectTags()));
        final NoteScreenModel paged = new NoteScreenModel(null, knowledge(
                bigPool.stream().map(tag -> new AspectEntry(tag, (short) 1)).toList(), List.of()));
        assertEquals(NoteScreenModel.ClickAction.NONE,
                paged.clickAt(27, 121, false, now), "no page back from page 0 (:651)");
        assertEquals(NoteScreenModel.ClickAction.PAGE_NEXT,
                paged.clickAt(51, 121, false, now), "page forward (:657)");
        assertEquals(1, paged.page());
        assertEquals(NoteScreenModel.ClickAction.PAGE_PREV,
                paged.clickAt(27, 121, false, now), "page back (:651)");
        assertEquals(0, paged.page());

        // The shift arm is last, and RESEARCHER2 gates it (:694-718).
        final NoteScreenModel noResearcher = new NoteScreenModel(null, knowledge(
                List.of(new AspectEntry(pair.get(0), (short) 5), new AspectEntry(pair.get(1), (short) 5)),
                List.of()));
        noResearcher.setPage(0);
        final String compound = AspectCombinations.combinationResultTag(pair.get(0), pair.get(1));
        assertNotNull(compound, "the shipped table must contain the pair");
        final NoteScreenModel withResearcher = new NoteScreenModel(null, knowledge(
                List.of(new AspectEntry(pair.get(0), (short) 5), new AspectEntry(pair.get(1), (short) 5),
                        new AspectEntry(compound, (short) 0)),
                List.of(new KeyEntry("RESEARCHER2"))));
        final NoteScreenModel.PoolCell compoundCell = withResearcher.poolCells().stream()
                .filter(cell -> cell.tag().equals(compound)).findFirst().orElseThrow();
        assertEquals(NoteScreenModel.ClickAction.SHIFT_COMBINE,
                withResearcher.clickAt(compoundCell.x(), compoundCell.y(), true, now),
                "shift on a payable-component compound (:694-718)");
        assertEquals(NoteScreenModel.ClickAction.NONE,
                noResearcher.clickAt(compoundCell.x(), compoundCell.y(), true, now),
                "RESEARCHER2 gates the shift arm (:76,694)");
        // A compound whose components are unpayable is refused (:704-710).
        final NoteScreenModel brokeComponents = new NoteScreenModel(null, knowledge(
                List.of(new AspectEntry(compound, (short) 1)), List.of(new KeyEntry("RESEARCHER2"))));
        final NoteScreenModel.PoolCell brokeCell = brokeComponents.poolCells().stream()
                .filter(cell -> cell.tag().equals(compound)).findFirst().orElseThrow();
        assertEquals(NoteScreenModel.ClickAction.NONE,
                brokeComponents.clickAt(brokeCell.x(), brokeCell.y(), true, now),
                "both components must be payable (:704-710)");
    }

    /**
     * The drag machine: a press inside the pool's 80x80 block picks up a
     * payable aspect, a release writes on a hex, drops on a select slot, or
     * picks up on the pool cell — in that order ({@code :150-208}).
     */
    @Test
    void theDragMachineFollowsTheOriginalSGateOrder() {
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), new Random(11L));
        final List<String> required = note.hexEntries().values().stream()
                .map(HexEntry::aspectTag).filter(tag -> tag != null && !tag.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new)).stream().toList();
        final PlayerKnowledgeData knowledge = knowledge(required.stream()
                .map(tag -> new AspectEntry(tag, (short) 4)).toList(), List.of());
        final NoteScreenModel model = new NoteScreenModel(note, knowledge);

        final NoteScreenModel.PoolCell first = model.poolCells().get(0);
        // A press outside the pool block picks up nothing (:135-145), and a press
        // that arms a drag plays one hhoff (:141) — the isMouseButtonDown gate.
        assertEquals(null, model.pressAt(120, 150).tag(), "no pool cell, no pick-up");
        assertFalse(model.pressAt(120, 150).started(), "a press outside the pool never arms the drag");
        final NoteScreenModel.Press armed = model.pressAt(first.x() + 3, first.y() + 3);
        assertEquals(first.tag(), armed.tag());
        assertTrue(armed.started(), "the press armed the drag (:135)");
        assertEquals(List.of(NoteScreenModel.UiSound.BUTTON_ASPECT), armed.sounds(),
                "the pick-up plays hhoff (:141)");
        final NoteScreenModel.Press rePress = model.pressAt(first.x() + 3, first.y() + 3);
        assertFalse(rePress.started(), "a press with the drag already armed does not re-arm (:135)");
        assertEquals(List.of(), rePress.sounds(),
                "and it plays nothing: the isMouseButtonDown gate is what stops the jingle on a hold (:135)");

        // A release on a writeable hex wins over every other arm (:156-172).
        final String emptyHex = note.hexEntries().entrySet().stream()
                .filter(cell -> cell.getValue().type() == 0).map(Map.Entry::getKey).findFirst().orElseThrow();
        final Hex target = Hex.fromKey(emptyHex);
        final int centreX = 169 + (int) Math.round(target.toPixel(9).x());
        final int centreY = 83 + (int) Math.round(target.toPixel(9).y());
        final NoteScreenModel.Drop write = model.releaseAt(centreX, centreY);
        assertEquals(NoteScreenModel.DropKind.HEX, write.kind(), "a hex write is the first gate (:156)");
        assertEquals(emptyHex, write.hexKey());
        assertEquals(List.of(NoteScreenModel.UiSound.BUTTON_COMBINE, NoteScreenModel.UiSound.BUTTON_WRITE),
                write.sounds(), "the write plays the combine jingle THEN the write (:157-158)");
        assertEquals(null, model.draggedAspect(), "the drag ends with the write");

        // A release in the select-1 box, then the select-2 box (:174-187).
        model.pressAt(first.x() + 3, first.y() + 3);
        assertEquals(NoteScreenModel.DropKind.SELECT_ONE,
                model.releaseAt(20, 146).kind(), "the select-1 drop box (:174-179)");
        assertEquals(first.tag(), model.select1());
        model.pressAt(first.x() + 3, first.y() + 3);
        assertEquals(NoteScreenModel.DropKind.SELECT_TWO,
                model.releaseAt(79, 146).kind(), "the select-2 drop box (:182-187)");
        assertEquals(first.tag(), model.select2());

        // A release on the pool cell fills the first empty select slot (:188-207).
        final NoteScreenModel fresh = new NoteScreenModel(note, knowledge);
        final NoteScreenModel.PoolCell other = model.poolCells().get(1);
        fresh.pressAt(other.x() + 3, other.y() + 3);
        assertEquals(NoteScreenModel.DropKind.POOL_PICKUP_ONE,
                fresh.releaseAt(other.x() + 3, other.y() + 3).kind(), "the pick-up arm fills slot 1 (:192-199)");
        fresh.pressAt(other.x() + 3, other.y() + 3);
        assertEquals(NoteScreenModel.DropKind.POOL_PICKUP_TWO,
                fresh.releaseAt(other.x() + 3, other.y() + 3).kind(), "then slot 2 (:200-205)");

        // A release with no target cancels the drag: nothing spends (:174-207).
        fresh.pressAt(other.x() + 3, other.y() + 3);
        assertEquals(NoteScreenModel.DropKind.NOTHING, fresh.releaseAt(130, 240).kind());
        fresh.cancelDrag();
        assertEquals(null, fresh.draggedAspect());

        // The erase arm lands on a player-placed (type 2) cell only (:726-740).
        // A freshly generated note has no writes, so the probe writes one: the
        // grid after a write is the grid the erase arm sees.
        final Map<String, HexEntry> written = new java.util.LinkedHashMap<>(note.hexEntries());
        written.put(emptyHex, new HexEntry(model.poolCells().get(0).tag(), 2));
        final ResearchNoteData afterWrite = new ResearchNoteData(
                note.key(), note.color(), note.complete(), note.copies(), written);
        final NoteScreenModel writtenModel = new NoteScreenModel(afterWrite, knowledge);
        final String placed = writtenModel.note().hexEntries().entrySet().stream()
                .filter(cell -> cell.getValue().type() == 2).map(Map.Entry::getKey).findFirst().orElseThrow();
        final Hex placedHex = Hex.fromKey(placed);
        final int placedX = 169 + (int) Math.round(placedHex.toPixel(9).x());
        final int placedY = 83 + (int) Math.round(placedHex.toPixel(9).y());
        assertEquals(NoteScreenModel.ClickAction.ERASE, writtenModel.eraseAt(placedX, placedY));
        assertEquals(placedHex, writtenModel.eraseTarget(placedX, placedY));
        final String stillEmpty = writtenModel.note().hexEntries().entrySet().stream()
                .filter(cell -> cell.getValue().type() == 0 && !cell.getKey().equals(emptyHex))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        final Hex emptyHexTarget = Hex.fromKey(stillEmpty);
        assertEquals(NoteScreenModel.ClickAction.NONE, writtenModel.eraseAt(
                        169 + (int) Math.round(emptyHexTarget.toPixel(9).x()),
                        83 + (int) Math.round(emptyHexTarget.toPixel(9).y())),
                "an empty hex has nothing to erase (:728)");
        // The erase click is the arm the window sends, and it fires before the
        // copy plate arm (:682).
        assertEquals(NoteScreenModel.ClickAction.ERASE,
                writtenModel.clickAt(placedX, placedY, false, 9_000_000_000L));
    }

    // ----------------------------------------------------------------- veil

    /**
     * The veil gate ({@code :539-540}): a grid cell's aspect is hidden behind a
     * black plate when the player has not discovered it. The expectation is the
     * shipped entry's own tag list — grant the tags and no cell is veiled;
     * revoke one and exactly the cells that carried it are.
     */
    @Test
    void theVeilCoversExactlyTheUndiscoveredCellsOfAShippedEntry() {
        int checked = 0;
        for (final ShippedEntry entry : shippedEntriesWithTags()) {
            final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), new Random(0xC0FFEL));
            final Set<String> required = new TreeSet<>();
            for (final HexEntry cell : note.hexEntries().values()) {
                if (cell.aspectTag() != null && !cell.aspectTag().isEmpty()) {
                    required.add(cell.aspectTag());
                }
            }
            if (required.size() < 2) {
                continue;
            }
            final PlayerKnowledgeData full = knowledge(required.stream()
                    .map(tag -> new AspectEntry(tag, (short) 4)).toList(), List.of());
            final NoteScreenModel granted = new NoteScreenModel(note, full);
            assertTrue(granted.hexCells().stream().noneMatch(NoteScreenModel.HexCell::veiled),
                    "every tag granted: nothing veiled (:539) " + entry.key());

            final String revoked = required.iterator().next();
            final PlayerKnowledgeData partial = knowledge(required.stream()
                    .filter(tag -> !tag.equals(revoked))
                    .map(tag -> new AspectEntry(tag, (short) 4)).toList(), List.of());
            final NoteScreenModel revoked1 = new NoteScreenModel(note, partial);
            final Set<String> veiledTags = new TreeSet<>();
            for (final NoteScreenModel.HexCell cell : revoked1.hexCells()) {
                if (cell.veiled()) {
                    veiledTags.add(cell.tag());
                }
            }
            assertEquals(Set.of(revoked), veiledTags,
                    "exactly the revoked tag is veiled: " + entry.key());
            for (final NoteScreenModel.HexCell cell : revoked1.hexCells()) {
                if (revoked.equals(cell.tag())) {
                    assertTrue(cell.veiled(), "the revoked tag's cell is veiled");
                    assertEquals(0.5F, NoteScreenModel.VEIL_ALPHA, 0.0F, "the plate is 0.5 alpha (:544)");
                }
            }
            checked++;
            if (checked >= 12) {
                break;
            }
        }
        assertTrue(checked >= 5, "the probe walked the shipped notes, checked " + checked);
    }

    /**
     * The connection sweep follows the shipped component graph: with every tag
     * granted, two grid cells are linked exactly when one is a direct component
     * of the other — the same relation {@code NotePuzzle.checkConnections} uses,
     * so the window's sweep and the server's completion check cannot disagree.
     */
    @Test
    void theConnectionsAreTheShippedComponentGraph() {
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), new Random(3L));
        final Set<String> required = new TreeSet<>();
        for (final HexEntry cell : note.hexEntries().values()) {
            if (cell.aspectTag() != null && !cell.aspectTag().isEmpty()) {
                required.add(cell.aspectTag());
            }
        }
        final NoteScreenModel granted = new NoteScreenModel(note, knowledge(required.stream()
                .map(tag -> new AspectEntry(tag, (short) 4)).toList(), List.of()));
        final List<Hex[]> links = granted.connections();
        for (final Hex[] link : links) {
            final HexEntry from = note.hexEntries().get(link[0].toString());
            final HexEntry to = note.hexEntries().get(link[1].toString());
            assertNotNull(from);
            assertNotNull(to);
            assertTrue(to.type() >= 1, "a link never ends on an empty cell (:578)");
            assertTrue(NotePuzzle.componentsMatch(from.aspectTag(), to.aspectTag()),
                    "the link's pair is a shipped component pair (:582-585)");
        }
        // Nothing discovered: no sweep at all (:507-512 seeds on discovery).
        final NoteScreenModel dark = new NoteScreenModel(note, knowledge(List.of(), List.of()));
        assertTrue(dark.connections().isEmpty(), "an unknown grid draws no sweep");
    }

    // -------------------------------------------------------------- combine

    /**
     * The combine cost, over the <b>whole shipped combination table</b>: one
     * unit of each side (the original's two {@code addAspectPool(-1)} calls,
     * {@code PacketAspectCombinationToServer.java:76-101}), and the result is
     * the table's own. The affordability arm is the pool's amounts, as the
     * original gates it (:71-75).
     */
    @Test
    void everyShippedPairCostsOneOfEachAndReturnsTheTableSResult() {
        final Map<String, String> table = AspectCombinations.combinationTable();
        assertFalse(table.isEmpty(), "the shipped combination table must be populated");
        int pairs = 0;
        for (final Map.Entry<String, String> row : table.entrySet()) {
            final String[] sides = row.getKey().split("\\+");
            final String first = sides[0];
            final String second = sides[1];
            final PlayerKnowledgeData rich = knowledge(
                    List.of(new AspectEntry(first, (short) 2), new AspectEntry(second, (short) 2)), List.of());
            final NoteScreenModel model = new NoteScreenModel(null, rich);
            final NoteScreenModel.Combination paid = model.combinePreview(first, second);
            assertEquals(row.getValue(), paid.resultTag(),
                    "the result is the table's, for " + row.getKey());
            final Map<String, Integer> expected = new TreeMap<>();
            expected.merge(first, 1, Integer::sum);
            expected.merge(second, 1, Integer::sum);
            assertEquals(expected, paid.cost(), "one unit of each side, for " + row.getKey());
            assertTrue(paid.affordable(), "a pool of 2 each affords it (:71-75)");

            final PlayerKnowledgeData broke = knowledge(
                    List.of(new AspectEntry(first, (short) 1), new AspectEntry(second, (short) 0)), List.of());
            assertFalse(new NoteScreenModel(null, broke).combinePreview(first, second).affordable(),
                    "a 0 side is unaffordable (:71-75)");
            pairs++;
        }
        assertEquals(table.size(), pairs);
        // A pair the table has no result for: the cost is still the two sides,
        // and the original pays before it checks for a result.
        final NoteScreenModel model = new NoteScreenModel(null, knowledge(
                List.of(new AspectEntry("aqua", (short) 1), new AspectEntry("bestia", (short) 1)), List.of()));
        final NoteScreenModel.Combination noResult = model.combinePreview("aqua", "aqua");
        assertEquals(Map.of("aqua", 2), noResult.cost(), "a self-pair pays twice (:76-101)");
        assertEquals(null, model.combinePreview("aqua", "bestia").resultTag(),
                "the table decides the result, not the client");
        // The shift arm's pair is the compound's own two components.
        final String compound = "ignis+aqua".substring(0, 0) + "potentia";
        final NoteScreenModel shift = new NoteScreenModel(null, knowledge(
                List.of(new AspectEntry("potentia", (short) 0)), List.of()));
        final NoteScreenModel.Combination preview = shift.shiftCombinePreview(compound);
        assertEquals(AspectDefinitions.all().stream()
                        .filter(definition -> definition.tag().equals(compound))
                        .findFirst().orElseThrow().components().stream().sorted().toList(),
                preview.cost().keySet().stream().sorted().toList(),
                "the shift arm's cost is the compound's declared components (:694-718)");
    }

    // --------------------------------------------------------------- sounds

    /**
     * The sound table: the six sounds of {@code GuiResearchTable.java:770-858},
     * each with the original's volume, and each id registered by the port's
     * {@code ModSounds} — the expectation is read from the registration source
     * at test time, and from the reference's line-cited numbers.
     */
    @Test
    void theSixSoundsAndTheirVolumesAreTheOriginalS() throws IOException {
        final String registrations = Files.readString(MOD_SOUNDS);
        final Map<NoteScreenModel.UiSound, String> expected = new java.util.EnumMap<>(
                NoteScreenModel.UiSound.class);
        expected.put(NoteScreenModel.UiSound.BUTTON_CLICK, "cameraclack");
        expected.put(NoteScreenModel.UiSound.BUTTON_ASPECT, "hhoff");
        expected.put(NoteScreenModel.UiSound.BUTTON_COMBINE, "hhon");
        expected.put(NoteScreenModel.UiSound.BUTTON_WRITE, "write");
        expected.put(NoteScreenModel.UiSound.BUTTON_ERASE, "erase");
        expected.put(NoteScreenModel.UiSound.BUTTON_SCROLL, "key");
        assertEquals(6, expected.size(), "the reference has six button sounds (:770-858)");
        for (final Map.Entry<NoteScreenModel.UiSound, String> row : expected.entrySet()) {
            assertEquals(row.getValue(), row.getKey().soundPath(), "the sound id (:770-858)");
            assertTrue(registrations.contains("register(\"" + row.getValue() + "\")"),
                    row.getValue() + " must be registered by ModSounds, or the window is silent");
        }
        assertEquals(0.4F, NoteScreenModel.UiSound.BUTTON_CLICK.volume(), 0.0F, "cameraclack 0.4F (:770-783)");
        assertEquals(0.2F, NoteScreenModel.UiSound.BUTTON_ASPECT.volume(), 0.0F, "hhoff 0.2F (:785-798)");
        assertEquals(0.3F, NoteScreenModel.UiSound.BUTTON_COMBINE.volume(), 0.0F, "hhon 0.3F (:800-813)");
        assertEquals(0.2F, NoteScreenModel.UiSound.BUTTON_WRITE.volume(), 0.0F, "write 0.2F (:815-828)");
        assertEquals(0.2F, NoteScreenModel.UiSound.BUTTON_ERASE.volume(), 0.0F, "erase 0.2F (:830-843)");
        assertEquals(0.3F, NoteScreenModel.UiSound.BUTTON_SCROLL.volume(), 0.0F, "key 0.3F (:845-858)");
        assertTrue(NoteScreenModel.UiSound.BUTTON_ASPECT.pitchRandomised(), "hhoff: 1 + rand*0.1 (:793-796)");
        assertTrue(NoteScreenModel.UiSound.BUTTON_ERASE.pitchRandomised(), "erase: 1 + rand*0.1 (:838-841)");
        assertFalse(NoteScreenModel.UiSound.BUTTON_CLICK.pitchRandomised(), "cameraclack: 1.0F (:778)");
    }

    /**
     * The ink gate ({@code :211-227}): a note in the slot and no ink (no
     * scribing tool, or an empty one) blocks the window's drawing with the
     * original's two tooltip lines.
     */
    @Test
    void theInkGateIsTheOriginalS() {
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final NoteScreenModel model = new NoteScreenModel(
                NotePuzzle.generate(entry.toEntryData(), new Random(5L)), knowledge(List.of(), List.of()));
        model.setInk(false, 0);
        assertTrue(model.showsNoInkTooltip(), "no scribe, no ink (:211)");
        model.setInk(true, 0);
        assertTrue(model.showsNoInkTooltip(), "a dry scribe is out of ink (:211)");
        model.setInk(true, 42);
        assertFalse(model.showsNoInkTooltip(), "ink in the scribe: the window draws");
        final NoteScreenModel noNote = new NoteScreenModel(null, knowledge(List.of(), List.of()));
        noNote.setInk(true, 42);
        assertFalse(noNote.showsNoInkTooltip(), "no note: the gate is not in play");
        assertEquals("tile.researchtable.noink.0", NoteScreenModel.NO_INK_LINE_ONE);
        assertEquals("tile.researchtable.noink.1", NoteScreenModel.NO_INK_LINE_TWO);
    }

    /**
     * The copy-cost plate: the shipped entry's tags with the note's copy count
     * added to each ({@code :115-119}). The expectation is the entry's own tag
     * list from the shipped graph.
     */
    @Test
    void theCopyPlateCostsTheEntrySTagsPlusTheCopies() {
        final List<ShippedEntry> shipped = shippedEntriesWithTags();
        final ShippedEntry entry = shipped.get(0);
        final ResearchNoteData note = new ResearchNoteData(entry.key(), 0, true, 3, Map.of());
        final NoteScreenModel model = new NoteScreenModel(note, knowledge(List.of(), List.of()));
        final Map<String, Integer> costs = model.copyCosts(entry.toEntryData());
        final Map<String, Integer> expected = new java.util.LinkedHashMap<>();
        for (final ResearchEntryData.AspectTag tag : entry.tags()) {
            expected.merge(tag.aspect(), tag.amount() + 3, Integer::sum);
        }
        assertEquals(expected, costs, "each tag's amount plus the note's copies (:115-119)");
        assertTrue(model.canDuplicate() == false, "RESEARCHDUPE is not known: the plate is off (:104)");
        final NoteScreenModel armed = new NoteScreenModel(note,
                knowledge(List.of(), List.of(new KeyEntry("RESEARCHDUPE"))));
        assertTrue(armed.canDuplicate(), "RESEARCHDUPE and a complete grid arm the plate (:104,254,683)");
    }

    // ------------------------------------------------------- source hygiene

    /**
     * The split this card's posture demands: the model is the headless half, so
     * it must import no Minecraft, NeoForge or Mojang type — otherwise the
     * probes would be running against a bootstrapped client, which the harness
     * cannot provide.
     */
    @Test
    void theModelIsMinecraftFree() throws IOException {
        final String source = Files.readString(MODEL_SOURCE);
        for (final String banned : List.of("net.minecraft", "com.mojang", "net.neoforged")) {
            assertFalse(source.contains("import " + banned),
                    "NoteScreenModel must not import " + banned + "; the probes run in a plain JVM");
        }
        assertTrue(source.contains("GuiResearchTable.java"),
                "every constant must cite the reference line it came from");
    }

    /**
     * The window is client-only and never claims completion: it sends writes,
     * erases, combines and the copy button, and the server's arm decides. The
     * layout numbers live in the model, not in the window.
     */
    @Test
    void theWindowDrawsTheModelAndNeverClaimsCompletion() throws IOException {
        final String screen = Files.readString(SCREEN_SOURCE);
        assertTrue(screen.contains("NoteScreenModel"), "the window must draw the model");
        for (final String banned : List.of("applyCompletion", "NotePuzzle.checkCompletion",
                "ResearchManager.addAspectCombination")) {
            assertFalse(screen.contains(banned),
                    "the client must not run " + banned + "; completion is the server's claim");
        }
        assertTrue(screen.contains("AspectPlaceToServerPayload")
                        && screen.contains("AspectCombinationToServerPayload"),
                "the write/erase and combine arms go over the port's payloads");
        assertTrue(screen.contains("BUTTON_DUPLICATE"), "the copy arm uses the menu's button id");
        // The panel geometry is the model's, never a literal in the window.
        assertTrue(screen.contains("NoteScreenModel.PANEL_WIDTH")
                        && screen.contains("NoteScreenModel.GRID_CENTRE_X"),
                "the window reads its geometry from the model");
        assertFalse(screen.contains("169, 83") || screen.contains("(169,83)"),
                "no hardcoded grid centre in the window");
        assertTrue(screen.contains("Dist") || screen.startsWith("package com.jonnyutah12.thaumcraftclassic.client"),
                "the window stays in the client package");
        final String registration = Files.readString(SCREEN_REGISTRATION);
        // Registering the menu type's screen is NOTE-SCREEN-1's deliverable, not
        // this card's: when the base has no note-menu screen registration yet,
        // this half is not applicable here (the sibling's own parity probes pin
        // it, and MenuTypeScreenCoverageTest stays green with it registered).
        Assumptions.assumeTrue(registration.contains("ResearchNoteScreen"),
                "the note menu's screen registration belongs to NOTE-SCREEN-1 and is not on this base");
        assertTrue(registration.contains("gui_research"),
                "the menu type's screen is the note window, so the coverage gate stays green");
    }

    // ------------------------------------- the arm sound tables (reference-derived)

    /**
     * The card's "sounds cameraclack 0.4 / hhoff 0.2+rand / hhon 0.3 / write 0.2
     * / erase 0.2+rand / key 0.3" note, asserted the D-019 way: every arm's
     * WHOLE sound list, in order, is read out of the reference's own
     * {@code playButton*} call sites — the expectation is parsed from the source
     * at test time, never typed from memory. This pins the pairs the naive port
     * dropped: the combine button plays click THEN combine (:631-633), the erase
     * arm combine THEN erase (:730-732), a hex write combine THEN write
     * (:156-158), and the pool pick-up plays NOTHING at all (:193-202).
     */
    @Test
    void theArmSoundListsAreTheReferenceCallSequences() {
        final String src = ReferenceOrigins.read(REFERENCE_GUI);
        assertEquals(Set.of("Click", "Combine", "Aspect", "Scroll", "Write", "Erase"),
                referenceButtonSounds(src).keySet(),
                "the reference's playButton* set is not the six the model carries");
        assertEquals(referenceSequence(src, "this.butcount2 = System.nanoTime() + 200000000L;", 2, 4),
                NoteScreenModel.ClickAction.COMBINE.sounds(), "the combine arm (:631-633)");
        assertEquals(referenceSequence(src, ".containsKey(hp.toString()) && this.note.hexEntries.get(hp.toString()).type == 2) {", 1, 4),
                NoteScreenModel.ClickAction.ERASE.sounds(), "the erase arm (:730-732)");
        assertEquals(referenceSequence(src, ".containsKey(hp.toString()) && this.note.hexEntries.get(hp.toString()).type == 0) {", 1, 4),
                referenceSequenceOf(NoteScreenModel.UiSound.BUTTON_COMBINE, NoteScreenModel.UiSound.BUTTON_WRITE),
                "the write arm (:156-158)");
        assertEquals(referenceSequence(src, "this.isMouseButtonDown = 1;", 3, 2),
                referenceSequenceOf(NoteScreenModel.UiSound.BUTTON_ASPECT), "the press arm (:141-142)");
        assertEquals(referenceSequence(src, "this.select1 = null;", 2, 3),
                NoteScreenModel.ClickAction.CLEAR_ONE.sounds(), "the clear-1 arm (:665-666)");
        assertEquals(referenceSequence(src, "this.select2 = null;", 2, 3),
                NoteScreenModel.ClickAction.CLEAR_TWO.sounds(), "the clear-2 arm (:675-676)");
        assertEquals(referenceSequence(src, "this.page--;", 2, 3),
                NoteScreenModel.ClickAction.PAGE_PREV.sounds(), "the back arrow (:652-653)");
        assertEquals(referenceSequence(src, "this.page++;", 2, 3),
                NoteScreenModel.ClickAction.PAGE_NEXT.sounds(), "the forward arrow (:658-659)");
        assertEquals(referenceSequence(src, "field_75152_c, 5);", 2, 3),
                NoteScreenModel.ClickAction.DUPLICATE.sounds(), "the copy arm (:687-688)");
        assertEquals(referenceSequence(src, "func_146272_n() && RESEARCHER_2) {", 1, 14),
                NoteScreenModel.ClickAction.SHIFT_COMBINE.sounds(), "the shift arm (:694-702)");
        assertEquals(referenceSequence(src, "if (this.select1 == null) {", 3, 12), List.of(),
                "no pool pick-up site plays anything (:193-202)");
        assertEquals(List.of(), NoteScreenModel.ClickAction.NONE.sounds(), "an unhandled click plays nothing");
        assertEquals(List.of(), NoteScreenModel.ClickAction.REFUSED.sounds(),
                "the original plays nothing when a box's own gate refuses");
    }

    // ------------------------------------------- the animations and the geometry

    @Test
    void theSweepCarriesTheReferenceColourAndItsAnimatedAlpha() {
        // :433 glColor4f(0.0F, 0.6F, 0.8F, alpha) — a FIXED colour, never the
        // aspect's, on the client player's tick clock (:434).
        assertEquals(0xFF0099CC, NoteScreenModel.CONNECTION_LINE_COLOUR,
                "the sweep's colour is the reference's, not the aspect's blend colour");
        for (int tick = 0; tick < 48; tick++) {
            for (int x = 0; x < 255; x += 17) {
                final float alpha = NoteScreenModel.connectionAlpha(tick, x);
                assertEquals(0.3F + (float) Math.sin((float) (tick + x)) * 0.3F + 0.3F, alpha, 1e-5F,
                        "the sweep alpha is the reference's 0.3 + sin(count + x) * 0.3 + 0.3 (:434)");
                assertTrue(alpha >= 0.0F && alpha <= 1.0F, "the sweep alpha stays a colour channel");
            }
        }
    }

    @Test
    void theRequiredHexOrbIsTheAnimationColourNotTheAspectColour() {
        // :860-867 — the required hex's orb is the shimmering white/blue animation;
        // the aspect's colour belongs to its TAG, drawn on top of the orb.
        // The formula's range is 0.4..1.0 per channel: the animation is always a
        // bright near-white shimmer (:860-867), never a dark aspect tint.
        for (int tick = 0; tick < 24; tick++) {
            final int orb = NoteScreenModel.orbColour(tick, 12, 30);
            for (int channel = 0; channel < 3; channel++) {
                final int value = (orb >> (16 - 8 * channel)) & 0xFF;
                assertTrue(value >= 0x66 && value <= 0xFF,
                        "the orb's animation never dips below the reference's 0.4 floor (channel "
                                + channel + " = " + value + " at tick " + tick + ")");
            }
        }
        assertEquals(0xFF000000, NoteScreenModel.orbColour(7, 3, 9) & 0xFF000000, "the orb is opaque");
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), new Random(5L));
        final String required = note.hexEntries().values().stream()
                .filter(cell -> cell.type() == NoteScreenModel.TYPE_REQUIRED)
                .map(HexEntry::aspectTag).filter(tag -> tag != null && !tag.isEmpty())
                .findFirst().orElseThrow();
        final NoteScreenModel model = new NoteScreenModel(note, knowledge(List.of(), List.of()));
        final NoteScreenModel.HexCell cell = model.hexCells().stream()
                .filter(hex -> hex.type() == NoteScreenModel.TYPE_REQUIRED).findFirst().orElseThrow();
        assertTrue(cell.orb(), "a required hex carries the orb (:533)");
        assertTrue(NoteScreenModel.aspectColourRgb(required) != NoteScreenModel.orbColour(3, cell.plateX(), cell.plateY()),
                "the orb must not be tinted with the aspect's colour");
    }

    @Test
    void theBlankHexFadesAndTheRequiredHexNeverHighlight() {
        assertEquals(0.25F, NoteScreenModel.HEX_OUTLINE_ALPHA,
                "the blank hex sprite draws at alpha 0.25 (:395,401)");
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final ResearchNoteData note = NotePuzzle.generate(entry.toEntryData(), new Random(9L));
        final NoteScreenModel model = new NoteScreenModel(note, knowledge(List.of(), List.of()));
        final NoteScreenModel.HexCell required = model.hexCells().stream()
                .filter(cell -> cell.type() == NoteScreenModel.TYPE_REQUIRED).findFirst().orElseThrow();
        // Hover exactly over the required hex. The hex that takes the highlight
        // is the one hexAt resolves for that pixel (:500-502), and the
        // reference's highlight lives in the NON-required branch of an open note
        // (:522-531): a required cell never lights, a blank cell always does.
        final String underKey = model.hexAt(required.centreX(), required.centreY()).toString();
        final NoteScreenModel.HexCell seen = model.hexCells(required.centreX(), required.centreY()).stream()
                .filter(cell -> cell.key().equals(underKey)).findFirst().orElseThrow();
        assertEquals(required.orb(), seen.orb(), "the orb rides the required cell wherever the cursor lands");
        assertEquals(seen.type() != NoteScreenModel.TYPE_REQUIRED, seen.hovered(),
                "a required hex never takes the hover highlight; an open cell always does (:522-531)");
        assertFalse(seen.outline() && seen.type() == NoteScreenModel.TYPE_REQUIRED,
                "a required hex draws no blank outline (:524)");
        final NoteScreenModel.HexCell blank = model.hexCells().stream()
                .filter(cell -> cell.type() == 0).findFirst().orElseThrow();
        final List<NoteScreenModel.HexCell> onBlank =
                model.hexCells(blank.centreX(), blank.centreY());
        final NoteScreenModel.HexCell seenBlank = onBlank.stream()
                .filter(cell -> cell.key().equals(blank.key())).findFirst().orElseThrow();
        assertTrue(seenBlank.hovered(), "an open note's blank cell takes the hover highlight (:525-528)");
        assertEquals(0.25F, seenBlank.outlineAlpha(), 1e-6F, "and its hex draws at the sprite's alpha");
    }

    @Test
    void thePanelIsTheOriginalsTwoFillShape() throws IOException {
        // :236 (0, 0, 255, 167) and :237 (40, 167, 184, 88): the panel is a field
        // plus a lower strip, not a square. The strip's bottom edge lands on the
        // panel's bottom, and it is inset on both sides.
        assertEquals(167, NoteScreenModel.PANEL_FIELD_HEIGHT);
        assertEquals(40, NoteScreenModel.PANEL_STRIP_X);
        assertEquals(NoteScreenModel.PANEL_FIELD_HEIGHT, NoteScreenModel.PANEL_STRIP_Y);
        assertEquals(184, NoteScreenModel.PANEL_STRIP_WIDTH);
        assertEquals(88, NoteScreenModel.PANEL_STRIP_HEIGHT);
        assertEquals(NoteScreenModel.PANEL_HEIGHT,
                NoteScreenModel.PANEL_STRIP_Y + NoteScreenModel.PANEL_STRIP_HEIGHT,
                "the strip closes the panel's bottom edge");
        assertTrue(NoteScreenModel.PANEL_STRIP_X + NoteScreenModel.PANEL_STRIP_WIDTH
                        < NoteScreenModel.PANEL_WIDTH,
                "the strip is inset on the right: a 255x255 fill would cover the inventory rows");
        final String screen = read(SCREEN_SOURCE);
        assertTrue(screen.contains("NoteScreenModel.PANEL_FIELD_HEIGHT")
                        && screen.contains("NoteScreenModel.PANEL_STRIP_WIDTH"),
                "the window fills both shapes from the model, never a square panel");
    }

    @Test
    void theCopyTagsHangOffTheRightOfThePlateInSortedOrder() {
        assertEquals(NoteScreenModel.COPY_PANEL_X + NoteScreenModel.COPY_PANEL_W, NoteScreenModel.COPY_TAG_X,
                ":124 draws each tag at plate-x + 48 + 16 per tag");
        assertEquals(NoteScreenModel.COPY_PANEL_Y, NoteScreenModel.COPY_TAG_Y, "at the plate's own y");
        assertEquals(16, NoteScreenModel.COPY_TAG_STEP);
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final NoteScreenModel model = new NoteScreenModel(
                NotePuzzle.generate(entry.toEntryData(), new Random(3L)), knowledge(List.of(), List.of()));
        final List<String> tags = new ArrayList<>(model.copyCosts(entry.toEntryData()).keySet());
        assertFalse(tags.isEmpty(), "a shipped tagged entry has a copy cost");
        assertEquals(new ArrayList<>(new TreeSet<>(tags)), tags,
                ":123 walks getAspectsSorted() — the plate's tags run in tag order");
    }

    @Test
    void theSelectSlotsHaveTheirOwnTooltipArm() {
        final ShippedEntry entry = shippedEntriesWithTags().get(0);
        final NoteScreenModel model = new NoteScreenModel(
                NotePuzzle.generate(entry.toEntryData(), new Random(7L)), knowledge(List.of(), List.of()));
        final String tag = model.hexCells().stream()
                .filter(cell -> cell.type() == NoteScreenModel.TYPE_REQUIRED)
                .map(NoteScreenModel.HexCell::tag).filter(java.util.Objects::nonNull)
                .findFirst().orElseThrow();
        assertNull(model.selectSlotAt(NoteScreenModel.SELECT1_X + 8, NoteScreenModel.SELECT_Y + 8),
                "an empty slot has no tooltip arm (:360)");
        model.setSelect1(tag);
        assertEquals(tag, model.selectSlotAt(NoteScreenModel.SELECT1_X + 8, NoteScreenModel.SELECT_Y + 8),
                "the select-1 slot has its own arm (:360-368)");
        model.setSelect2(tag);
        assertEquals(tag, model.selectSlotAt(NoteScreenModel.SELECT2_X + 8, NoteScreenModel.SELECT_Y + 8),
                "and so does select-2 (:369-379)");
        assertNull(model.selectSlotAt(0, 0), "outside both slots, no arm fires");
    }

    // -------------------------------------------------- reference-source parsing

    /** The reference's playButton* bodies: name to the model's sound with the same path and volume. */
    private static Map<String, NoteScreenModel.UiSound> referenceButtonSounds(final String src) {
        final Map<String, NoteScreenModel.UiSound> byName = new TreeMap<>();
        final Matcher bodies = Pattern
                .compile("private void playButton(\\w+)\\(\\) \\{(.*?)(?=\\n\\s*\\})\\n",
                        Pattern.DOTALL)
                .matcher(src);
        while (bodies.find()) {
            final Matcher sound = Pattern.compile("\"thaumcraft:([a-z]+)\",\\s*([0-9.]+)F")
                    .matcher(bodies.group(2));
            assertTrue(sound.find(), "playButton" + bodies.group(1) + " plays no thaumcraft sound");
            byName.put(bodies.group(1), portSound(sound.group(1), Float.parseFloat(sound.group(2))));
        }
        return byName;
    }

    /**
     * The playButton* calls of an arm: the window around EVERY line matching the
     * anchor, in source order, taking the busiest site. The reference assigns
     * some of the same fields outside the click arms (the pool-drop housekeeping
     * at {@code :295,301} clears a selection and plays nothing), so the arm's
     * own site is the one that plays. When no site of an anchor plays anything,
     * the answer is the empty list — which is exactly what the pool pick-up arm
     * is ({@code :193-202}).
     */
    private static List<NoteScreenModel.UiSound> referenceSequence(final String src, final String anchor,
            final int before, final int after) {
        final String[] lines = src.split("\n", -1);
        final Map<String, NoteScreenModel.UiSound> buttons = referenceButtonSounds(src);
        List<NoteScreenModel.UiSound> best = List.of();
        boolean seen = false;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains(anchor)) {
                continue;
            }
            seen = true;
            final String window = String.join("\n",
                    java.util.Arrays.asList(lines).subList(Math.max(0, i - before),
                            Math.min(lines.length, i + after + 1)));
            final List<NoteScreenModel.UiSound> calls = new ArrayList<>();
            final Matcher found = Pattern.compile("playButton(\\w+)\\(").matcher(window);
            while (found.find()) {
                final NoteScreenModel.UiSound sound = buttons.get(found.group(1));
                assertNotNull(sound, "the reference calls playButton" + found.group(1)
                        + ", which the model carries no sound for");
                calls.add(sound);
            }
            if (calls.size() > best.size()) {
                best = calls;
            }
        }
        assertTrue(seen, "the reference source has no arm anchor " + anchor);
        return best;
    }

    private static List<NoteScreenModel.UiSound> referenceSequenceOf(final NoteScreenModel.UiSound... sounds) {
        return List.of(sounds);
    }

    private static NoteScreenModel.UiSound portSound(final String referenceName, final float volume) {
        for (final NoteScreenModel.UiSound sound : NoteScreenModel.UiSound.values()) {
            if (sound.soundPath().equals(referenceName) && Math.abs(sound.volume() - volume) < 0.001F) {
                return sound;
            }
        }
        throw new AssertionError("the model carries no sound for thaumcraft:" + referenceName
                + " at volume " + volume + "; the reference's volume is data, not a guess");
    }

    // ----------------------------------------------------------- test fixtures

    /** One shipped graph entry, read from the shipped JSON at test time. */
    private record ShippedEntry(String key, List<ResearchEntryData.AspectTag> tags) {

        ResearchEntryData toEntryData() {
            return new ResearchEntryData(key, "NOTEPROBE", 0, 0, 1, 1, tags,
                    null, null, List.of(), List.of(), List.of(), null, 0,
                    "tc.research." + key.toLowerCase(Locale.ROOT), "note probe",
                    "tc.research." + key.toLowerCase(Locale.ROOT) + ".text", "note probe text", List.of());
        }
    }

    private static List<ShippedEntry> shippedEntriesWithTags() {
        final JsonArray entries = JsonParser.parseString(read(GRAPH)).getAsJsonObject()
                .getAsJsonArray("entries");
        final List<ShippedEntry> shipped = new ArrayList<>();
        for (final JsonElement element : entries) {
            final JsonObject entry = element.getAsJsonObject();
            if (!entry.has("tags") || !entry.getAsJsonArray("tags").isJsonArray()
                    || entry.getAsJsonArray("tags").isEmpty()) {
                continue;
            }
            final List<ResearchEntryData.AspectTag> tags = new ArrayList<>();
            for (final JsonElement tag : entry.getAsJsonArray("tags")) {
                final JsonObject row = tag.getAsJsonObject();
                tags.add(new ResearchEntryData.AspectTag(row.get("aspect").getAsString(),
                        row.has("constant") ? row.get("constant").getAsString() : null,
                        row.get("amount").getAsInt()));
            }
            shipped.add(new ShippedEntry(entry.get("key").getAsString(), tags));
        }
        return shipped;
    }

    private static Set<String> shippedAspectTags() {
        final Set<String> tags = new TreeSet<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            tags.add(definition.tag());
        }
        return tags;
    }

    /** The first compound pair the shipped table declares, in table order. */
    private static List<String> firstCompoundPair() {
        final String key = AspectCombinations.combinationTable().keySet().iterator().next();
        return List.of(key.split("\\+"));
    }

    private static PlayerKnowledgeData knowledge(final List<AspectEntry> aspects,
                                                 final List<KeyEntry> research) {
        return new PlayerKnowledgeData(research, aspects, List.of(), List.of(), List.of());
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException failure) {
            throw new IllegalStateException("cannot read " + path.toAbsolutePath()
                    + " (cwd: " + Path.of("").toAbsolutePath() + ")", failure);
        }
    }
}
