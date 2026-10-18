package com.jonnyutah12.thaumcraftclassic.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M2-04 acceptance: the option-by-option table in docs/config-reference.md
 * must match the code-side inventory ({@link TcConfigCatalog}, which the
 * NeoForge spec is built from) row for row — name, type, default, status,
 * notes, original comment and source line. The table is the human-facing
 * record of the original Thaumcraft 4.2.3.5 config; the catalog is the
 * machine-facing one; this test is what keeps them identical.
 */
class ConfigReferenceTableTest {

    private static final Pattern SECTION = Pattern.compile("^## (\\S+) \\(\\d+ options\\)\\s*$");

    record Row(String category, String option, String type, String defaultValue, String status, String notes,
            String comment, String source) {
    }

    private static Path docFile() {
        String path = System.getProperty("tc.configReferenceFile");
        assertNotNull(path, "system property 'tc.configReferenceFile' is not set — run tests through ./gradlew test");
        Path file = Path.of(path);
        assertTrue(Files.isRegularFile(file), "docs/config-reference.md not found at " + file);
        return file;
    }

    private static List<Row> parseTable() throws IOException {
        List<Row> rows = new ArrayList<>();
        String category = null;
        for (String line : Files.readAllLines(docFile())) {
            Matcher section = SECTION.matcher(line);
            if (section.matches()) {
                category = section.group(1);
                continue;
            }
            if (!line.startsWith("| ")) {
                continue;
            }
            String[] cells = line.split("\\|", -1);
            // split on leading/trailing pipes yields empty first/last cells
            List<String> body = new ArrayList<>();
            for (int i = 1; i < cells.length - 1; i++) {
                body.add(cells[i].trim());
            }
            if (body.size() != 7) {
                throw new AssertionError("table row must have exactly 7 cells: " + line);
            }
            if (body.get(0).equals("Option") || body.get(0).startsWith("---")) {
                continue; // header / separator
            }
            assertNotNull(category, "table row outside a category section: " + line);
            rows.add(new Row(category, body.get(0), body.get(1), body.get(2), body.get(3), body.get(4),
                    body.get(5), body.get(6)));
        }
        return rows;
    }

    @Test
    void tableMatchesCatalogRowForRow() throws IOException {
        List<Row> table = parseTable();
        List<ConfigOption> catalog = TcConfigCatalog.OPTIONS;

        assertEquals(catalog.size(), table.size(),
                "docs/config-reference.md must contain exactly one row per catalogued option");

        for (int i = 0; i < catalog.size(); i++) {
            ConfigOption expected = catalog.get(i);
            Row actual = table.get(i);
            String where = "row " + (i + 1);
            assertEquals(expected.category(), actual.category(), where + ": category section mismatch");
            assertEquals(expected.name(), actual.option(), where + ": option name mismatch");
            assertEquals(expected.type().docLabel(), actual.type(), where + ": type mismatch for " + expected.path());
            assertEquals(expected.defaultLabel(), actual.defaultValue(),
                    where + ": default mismatch for " + expected.path());
            assertEquals(expected.ported() ? "ported" : "not ported", actual.status(),
                    where + ": status mismatch for " + expected.path());
            assertEquals(expected.notPortedReason() == null ? "" : expected.notPortedReason(), actual.notes(),
                    where + ": notes mismatch for " + expected.path());
            assertEquals(expected.comment(), actual.comment(), where + ": comment mismatch for " + expected.path());
            assertEquals(expected.source(), actual.source(), where + ": source mismatch for " + expected.path());
        }
    }

    @Test
    void countsMatchTheOriginalConfigClass() throws IOException {
        // Thaumcraft-1.7.10-4.2.3.5 Config.java contains exactly 62
        // Configuration.get calls; 9 of them only assign numeric registry ids
        // that cannot exist on NeoForge 1.21.1.
        List<Row> table = parseTable();
        assertEquals(62, table.size(), "total option count must match the original config class");
        assertEquals(53, table.stream().filter(r -> r.status().equals("ported")).count(), "ported option count");
        assertEquals(9, table.stream().filter(r -> r.status().equals("not ported")).count(), "not-ported option count");
    }

    @Test
    void categoryCommentsAreRecordedVerbatim() throws IOException {
        String doc = Files.readString(docFile());
        for (var entry : TcConfigCatalog.CATEGORY_COMMENTS.entrySet()) {
            assertTrue(doc.contains(entry.getValue()),
                    "category comment for " + entry.getKey() + " must appear verbatim in the docs");
        }
    }
}
