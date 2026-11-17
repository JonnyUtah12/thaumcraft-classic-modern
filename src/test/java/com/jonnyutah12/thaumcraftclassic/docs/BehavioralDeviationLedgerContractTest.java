package com.jonnyutah12.thaumcraftclassic.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Contract for {@code docs/BEHAVIORAL_DEVIATIONS.md} (the BD ledger) — t_9a2c9f91.
 *
 * <p>The ledger diverged across live heads: the same file carried 14-30 rows on
 * different heads while {@code main} carried 45, and three different rows
 * claimed {@code BD-19} on three sides of three landings. Resolving that file
 * with a wholesale "take ours"/"take theirs" during a merge deletes rows
 * silently — a row that was never edited carries no conflict marker, so the
 * loss lands green (the same channel that reverted 43 rows of
 * {@code docs/content-mapping.csv}).
 *
 * <p>These probes make the reconcile rule machine-checked, with {@code main} as
 * the incumbent id space:
 *
 * <ul>
 *   <li>the row count never drops below the incumbent floor, and the two ways
 *       of counting rows — the {@code ^| BD-} line count and a strict parse of
 *       the id cell — agree, so a row cannot hide from tooling in a polluted
 *       id cell;</li>
 *   <li>every incumbent row survives with its id and its System cell verbatim:
 *       this is the anti-revert assertion, the one a "take theirs" violates;</li>
 *   <li>ids are unique, zero-padded and contiguous from {@code BD-01} — the
 *       ledger has no slack, so a gap is a deleted row and a duplicate is a
 *       collision;</li>
 *   <li>every {@code BD-NN} cited anywhere in the tree resolves to exactly one
 *       row (0 rows = a dangling citation, 2 = the multi-occupant defect);</li>
 *   <li>every row carries a status token that the file itself declares in its
 *       "Status vocabulary" paragraph — the file's own vocabulary is the law,
 *       so a row cannot invent a classification the ledger does not define;</li>
 *   <li>the generated {@code docs/bd-id-map.csv} old→new mapping resolves: every
 *       mapped new id exists as exactly one row, every (head, old id) pair is
 *       unique, and every appended row names its source head and the reconcile
 *       that allocated its id.</li>
 * </ul>
 *
 * <p>Pure JDK, no Minecraft classpath: the ledger is plain Markdown, so this is
 * a pure-logic assertion and belongs in {@code ./gradlew test} (CI's
 * {@code build} job), not in the game-test harness.
 */
class BehavioralDeviationLedgerContractTest {

    /** The id count on `main` at the reconcile base 7933db8, before this card appended anything. */
    private static final int INCUMBENT_FLOOR = 45;

    /** Rows at the reconcile base plus the ids this card allocated: the count floor for every future landing. */
    private static final int LEDGER_FLOOR = 64;

    /** Reconcile tag an appended row must carry in its last cell. */
    private static final String RECONCILE_TAG = "t_9a2c9f91";

    private static final Pattern ID_CELL = Pattern.compile("^\\|\\s*BD-(\\d{2,})\\s*\\|");
    private static final Pattern ID_CELL_PAD = Pattern.compile("^\\|\\s*BD-\\d{2,3}\\s*\\|");
    private static final Pattern LOOSE_ROW = Pattern.compile("^\\|\\s*BD-");
    private static final Pattern BD_REF = Pattern.compile("\\bBD-(\\d{2,})\\b");
    private static final Pattern VOCAB = Pattern.compile("\\*\\*([A-Za-z][A-Za-z /]*?)\\*\\*");

    /**
     * The 45 incumbent rows, frozen as id -> first 48 chars of the System cell,
     * measured on `main` at 7933db8 (BD-13's entry is its repaired form: the
     * parenthetical that sat in its id cell moves to the System cell, which is
     * the only edit this card makes to an incumbent row). A landing that takes
     * one side of a merge wholesale trips this: a deleted id, a renumbered id,
     * or a row replaced by the other side's text.
     */
    private static final Map<String, String> INCUMBENT = incumbent();

    private static Map<String, String> incumbent() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("BD-01", "Content ids");
        m.put("BD-02", "Sound event ids");
        m.put("BD-03", "Wand-trigger \"ids\"");
        m.put("BD-04", "Item/player state storage");
        m.put("BD-05", "Research data");
        m.put("BD-06", "Accessory inventory GUI");
        m.put("BD-07", "Config numeric-id options");
        m.put("BD-08", "Vis storage");
        m.put("BD-09", "sounds.json");
        m.put("BD-10", "Research hidden-parent quirk");
        m.put("BD-11", "Persistence codecs");
        m.put("BD-12", "Ore world-gen");
        m.put("BD-13", "Thaumonomicon GUI (see D-019 for the GU");
        m.put("BD-14", "Interaction layer (workbench/infusion/crucible/e");
        m.put("BD-15", "Client FX motion model (TC-M7-02-s)");
        m.put("BD-16", "World-rendered beam/bolt seam (TC-M7-02-b-s)");
        m.put("BD-17", "4.2.3.5 gives meta-carrying items and blocks a *");
        m.put("BD-18", "Taint (spread/conversion, Tainted Land biome) — ");
        m.put("BD-19", "Wisp FX family art + homing NaN guard (TC-M7-02-");
        m.put("BD-20", "Eldritch damage types (taint / dissolve / tentac");
        m.put("BD-21", "Firebat death drop and the 1.7.10 `isNoAi` hook ");
        m.put("BD-22", "Thaumonomicon open path (BOOK-1 / t_1e6b18cc)");
        m.put("BD-23", "Thaumonomicon presentation (TC-M3-03-s)");
        m.put("BD-24", "Research note item (TC-M3-04-s-ii)");
        m.put("BD-25", "Thaumonomicon note request + Scribing Tools (TC-");
        m.put("BD-26", "Research note menu (TC-M3-04-s-iii)");
        m.put("BD-27", "Research note screen (TC-M3-04-s-iii / NOTE-SCRE");
        m.put("BD-28", "Furious zombie resize and anger persistence (TC-");
        m.put("BD-29", "The pair's zombie-brain drop is gated on an unre");
        m.put("BD-30", "Golem home deposit / gather deposit (TC-M6-08-s-");
        m.put("BD-31", "Golem goal installation (TC-M6-08-s-b2)");
        m.put("BD-32", "Golem door interaction (TC-M6-08-s-b2)");
        m.put("BD-33", "Golem carried-display repaint `updateCarried()` ");
        m.put("BD-34", "OreDictionary widening + the watched item (TC-M6");
        m.put("BD-35", "Marked double-chest join (TC-M6-08-s-b, cited by");
        m.put("BD-36", "Home-container exclusion in the Empty drop (TC-M");
        m.put("BD-37", "Container screen backgrounds and client-visible ");
        m.put("BD-38", "Golem configuration GUI (TC-M7-04-m3-a)");
        m.put("BD-39", "Golem-family sibling entities and the eldritch s");
        m.put("BD-40", "Localization audit vs the ledger id contract (sp");
        m.put("BD-41", "Golem decoration layer render state (TC-M6-08-s-");
        m.put("BD-42", "Golem bobber fishing line (TC-M6-08-s-c2 / t_647");
        m.put("BD-43", "Eldritch golem renderer state (TC-M6-08-s-c2 / t");
        m.put("BD-44", "Golem core-icon and upgrade-strip sprites (TC-M6");
        m.put("BD-45", "Golem bucket overlay (TC-M6-08-s-c2 / t_647f672e");
        return m;
    }

    // ---------- plumbing ----------

    private static Path root() {
        String prop = System.getProperty("tc.repoRootDir");
        assumeTrue(prop != null && Files.isDirectory(Path.of(prop)),
                "tc.repoRootDir is not wired — run the suite through ./gradlew test");
        return Path.of(prop);
    }

    private static Path ledgerFile() {
        return root().resolve("docs/BEHAVIORAL_DEVIATIONS.md");
    }

    private static List<String> ledgerLines() {
        try {
            return Files.readAllLines(ledgerFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + ledgerFile(), e);
        }
    }

    /** A ledger row: the id cell, the remaining cells, and the raw line. */
    private record Row(String id, List<String> cells, String raw) {}

    /** Rows whose id cell parses as exactly `BD-NN`. */
    private static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (String line : ledgerLines()) {
            Matcher m = ID_CELL.matcher(line);
            if (!m.find()) {
                continue;
            }
            List<String> all = new ArrayList<>(List.of(line.split("\\|", -1)));
            all.remove(0);                                   // text before the first pipe
            if (!all.isEmpty()) {
                all.remove(all.size() - 1);                  // text after the last pipe
            }
            List<String> cells = new ArrayList<>();
            for (String c : all) {
                cells.add(c.strip());
            }
            out.add(new Row("BD-" + m.group(1), cells, line));
        }
        return out;
    }

    private static String collapse(String s) {
        return s.replaceAll("\\s+", " ").strip();
    }

    /** Files whose text may cite a BD id: docs, sources, scripts, the two root prose files. */
    private static List<Path> citableFiles() throws IOException {
        Path r = root();
        List<Path> files = new ArrayList<>();
        for (String dir : List.of("docs", "src", "scripts")) {
            Path p = r.resolve(dir);
            if (!Files.isDirectory(p)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(p)) {
                s.filter(Files::isRegularFile)
                        .filter(f -> {
                            String n = f.getFileName().toString();
                            return n.endsWith(".md") || n.endsWith(".csv") || n.endsWith(".java")
                                    || n.endsWith(".py") || n.endsWith(".sh") || n.endsWith(".json")
                                    || n.endsWith(".txt");
                        })
                        .forEach(files::add);
            }
        }
        for (String f : List.of("README.md", "CHANGELOG.md")) {
            if (Files.isRegularFile(r.resolve(f))) {
                files.add(r.resolve(f));
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    // ---------- the contract ----------

    /**
     * The count never decreases, and the two counting methods agree: the loose
     * `^| BD-` line count every card quotes, the strict parse is what tooling
     * sees. A row that hides in the gap between them (a polluted id cell) is
     * invisible to a merge audit and survives a "take theirs" as a phantom.
     */
    @Test
    void ledgerRowCountNeverDecreasesAndBothCountingMethodsAgree() {
        List<String> lines = ledgerLines();
        long loose = lines.stream().filter(l -> LOOSE_ROW.matcher(l).find()).count();
        int strict = rows().size();
        assertTrue(loose >= LEDGER_FLOOR,
                "ledger row count dropped to " + loose + " (loose) / " + strict + " (strict); the "
                        + "incumbent floor is " + INCUMBENT_FLOOR + " rows from main plus the ids the "
                        + "reconcile allocated (" + LEDGER_FLOOR + " total). A merge deleted rows.");
        assertEquals(loose, strict,
                "the ledger has rows the strict parse cannot see: loose count " + loose
                        + " != strict count " + strict + " — a row's id cell is not exactly `BD-NN`,"
                        + " so merge audits and generators silently skip it");
        assertTrue(strict >= INCUMBENT_FLOOR,
                "strict row parse found " + strict + " rows, below the incumbent floor "
                        + INCUMBENT_FLOOR);
    }

    /**
     * Every incumbent row survives verbatim: same id, same System cell. This is
     * the assertion that makes `main` the incumbent — a landing that takes its
     * branch's version of the ledger wholesale trips here on the ids its base
     * never carried.
     */
    @Test
    void incumbentRowsSurviveWithTheirIdsAndSystemCellsVerbatim() {
        Map<String, String> byId = new HashMap<>();
        for (Row r : rows()) {
            byId.putIfAbsent(r.id(), r.cells().size() > 1 ? collapse(r.cells().get(1)) : "");
        }
        List<String> missing = new ArrayList<>();
        List<String> rewritten = new ArrayList<>();
        for (Map.Entry<String, String> e : INCUMBENT.entrySet()) {
            String sys = byId.get(e.getKey());
            if (sys == null) {
                missing.add(e.getKey() + " (" + e.getValue() + ")");
            } else if (!sys.startsWith(e.getValue())) {
                rewritten.add(e.getKey() + ": expected prefix [" + e.getValue() + "] got ["
                        + sys.substring(0, Math.min(sys.length(), 64)) + "]");
            }
        }
        assertTrue(missing.isEmpty(),
                (INCUMBENT_FLOOR - missing.size()) + "/" + INCUMBENT_FLOOR + " incumbent BD rows "
                        + "survive; DELETED (a merge took one side wholesale): " + missing);
        assertTrue(rewritten.isEmpty(),
                "incumbent BD rows were REWRITTEN by a landing (the row a merge should have kept is "
                        + "main's): " + rewritten);
    }

    /** Ids are unique, zero-padded, and contiguous from BD-01: the id space has no slack. */
    @Test
    void bdIdsAreUniqueZeroPaddedAndContiguous() {
        List<Row> all = rows();
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<String> pad = new ArrayList<>();
        for (Row r : all) {
            seen.merge(r.id(), 1, Integer::sum);
            if (!ID_CELL_PAD.matcher(r.raw()).find()) {
                pad.add(r.raw().substring(0, Math.min(40, r.raw().length())));
            }
        }
        List<String> dupes = new ArrayList<>();
        for (Map.Entry<String, Integer> e : seen.entrySet()) {
            if (e.getValue() > 1) {
                dupes.add(e.getKey() + " x" + e.getValue());
            }
        }
        assertTrue(dupes.isEmpty(), "duplicate BD ids in one ledger: " + dupes);
        assertTrue(pad.isEmpty(), "id cell not zero-padded `BD-NN`: " + pad);
        List<Integer> nums = all.stream().map(r -> Integer.parseInt(r.id().substring(3))).toList();
        for (int i = 0; i < nums.size(); i++) {
            assertEquals(i + 1, nums.get(i),
                    "BD ids are not contiguous in file order at position " + i + " (ids: " + nums + ")");
        }
    }

    /**
     * Every BD id cited anywhere in the tree resolves to exactly one row. 0 rows
     * = a dangling citation (a renumbered row nobody re-derived); 2 rows = the
     * multi-occupant defect the reconcile exists to end.
     */
    @Test
    void everyBdCitationResolvesToExactlyOneRow() throws IOException {
        Set<String> ids = new java.util.HashSet<>();
        for (Row r : rows()) {
            ids.add(r.id());
        }
        Map<String, List<String>> dangling = new LinkedHashMap<>();
        for (Path f : citableFiles()) {
            int lineNo = 0;
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                lineNo++;
                Matcher m = BD_REF.matcher(line);
                while (m.find()) {
                    String ref = m.group(0);
                    if (!ids.contains(ref)) {
                        dangling.computeIfAbsent(ref, k -> new ArrayList<>())
                                .add(root().relativize(f) + ":" + lineNo);
                    }
                }
            }
        }
        assertTrue(dangling.isEmpty(),
                (dangling.size()) + " cited BD ids resolve to 0 rows (dangling after a renumber, "
                        + "citing docs not re-derived): "
                        + dangling.entrySet().stream().limit(12)
                                .map(e -> e.getKey() + " at " + String.join(", ", e.getValue().stream()
                                        .limit(3).toList())).toList());
    }

    /**
     * Every row carries a status token the file declares in its own "Status
     * vocabulary" paragraph. The paragraph is the law, so a row cannot invent a
     * classification the ledger does not define, and the vocabulary cannot drift
     * out of sync with the rows.
     */
    @Test
    void everyRowCarriesADeclaredStatusToken() {
        List<String> lines = ledgerLines();
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Status vocabulary:")) {
                start = i;
                break;
            }
        }
        assertTrue(start >= 0, "the ledger has no `Status vocabulary:` paragraph to declare its classes");
        StringBuilder para = new StringBuilder();
        for (int i = start; i < lines.size() && !lines.get(i).isBlank(); i++) {
            para.append(lines.get(i)).append(' ');
        }
        List<String> vocab = new ArrayList<>();
        Matcher v = VOCAB.matcher(para);
        while (v.find()) {
            vocab.add(v.group(1).strip());
        }
        assertFalse(vocab.isEmpty(), "the `Status vocabulary:` paragraph declares no **token** classes");
        List<String> unclassified = new ArrayList<>();
        for (Row r : rows()) {
            String body = collapse(String.join(" ", r.cells().subList(1, r.cells().size())));
            final String bodyL = body.toLowerCase();
            boolean hit = vocab.stream().anyMatch(t -> bodyL.contains(t.toLowerCase()));
            if (!hit) {
                unclassified.add(r.id() + " (declared: " + vocab + "; row tail: "
                        + body.substring(Math.max(0, body.length() - 90)) + ")");
            }
        }
        assertTrue(unclassified.isEmpty(),
                unclassified.size() + " rows carry no status token the ledger declares: " + unclassified);
    }

    /**
     * The generated old→new map resolves: every mapped new id is a real row,
     * every (head, old id) pair is unique, and every appended row names the head
     * it was renumbered from.
     */
    @Test
    void generatedIdMapResolvesAndEveryAppendedRowDeclaresItsSource() throws IOException {
        Path map = root().resolve("docs/bd-id-map.csv");
        assertTrue(Files.isRegularFile(map),
                "docs/bd-id-map.csv is missing: the old→new mapping the pending landings must cite "
                        + "is generated by scripts/bd_ledger.py emit");
        List<String> lines = Files.readAllLines(map, StandardCharsets.UTF_8);
        assertEquals("head,head_sha,old_id,new_id,card,canonical,system", lines.get(0),
                "bd-id-map.csv header drifted; landings parse it by column name");
        Map<String, String> byId = new LinkedHashMap<>();
        for (Row r : rows()) {
            byId.put(r.id(), collapse(String.join(" ", r.cells())));
        }
        Map<String, Integer> pairs = new LinkedHashMap<>();
        List<String> deadTargets = new ArrayList<>();
        List<String> rowsWithoutProvenance = new ArrayList<>();
        int data = 0;
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split(",", -1);
            data++;
            String key = f[0] + "|" + f[2];
            pairs.merge(key, 1, Integer::sum);
            if (!byId.containsKey(f[3])) {
                deadTargets.add(key + " -> " + f[3]);
            }
            String body = byId.getOrDefault(f[3], "");
            if (!body.contains(RECONCILE_TAG) || !body.contains(f[0])) {
                rowsWithoutProvenance.add(f[3] + " (from " + key + ")");
            }
        }
        assertTrue(data > 0, "bd-id-map.csv carries no mapping rows");
        List<String> dupes = pairs.entrySet().stream().filter(e -> e.getValue() > 1)
                .map(e -> e.getKey() + " x" + e.getValue()).toList();
        assertTrue(dupes.isEmpty(), "one (head, old id) pair maps to two new ids: " + dupes);
        assertTrue(deadTargets.isEmpty(),
                "mapped new ids do not exist as rows (the map was not regenerated after a renumber): "
                        + deadTargets);
        assertTrue(rowsWithoutProvenance.isEmpty(),
                "appended rows name no source head / reconcile card: " + rowsWithoutProvenance);
    }

    /**
     * Appended ids start exactly where the incumbent space ends: the reconcile
     * appends, it never fills a gap, so an incoming row can never steal a main id.
     */
    @Test
    void appendedIdsStartWhereTheIncumbentSpaceEnds() {
        for (Row r : rows()) {
            int n = Integer.parseInt(r.id().substring(3));
            if (n > INCUMBENT_FLOOR) {
                assertTrue(r.cells().get(r.cells().size() - 1).contains(RECONCILE_TAG),
                        r.id() + " is above the incumbent floor and must carry the reconcile "
                                + "provenance in its last cell");
            } else {
                assertFalse(r.cells().get(r.cells().size() - 1).contains(RECONCILE_TAG),
                        r.id() + " is an incumbent id and must not be tagged as a reconcile append");
            }
        }
    }
}
