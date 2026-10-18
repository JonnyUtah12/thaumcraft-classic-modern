package com.jonnyutah12.thaumcraftclassic.registration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for docs/content-mapping.csv — the id contract for all mod content
 * (docs/architecture/registration.md). Comment lines (starting with '#') are
 * skipped; the first remaining line is the header.
 *
 * <p>Deliberately dependency-free (no Minecraft, no third-party CSV): the same
 * reader serves the unit test (src/test) and the in-game registration harness
 * (RegistrationGameTest), so both see identical parsing.</p>
 */
public final class ContentMappingCsv {

    public record Row(String originalId, String newId, String kind, String displayName,
                     String implementationStatus, String testStatus, String knownDifferences, String notes) {
    }

    public static List<Row> read(final Path file) throws IOException {
        final List<Row> rows = new ArrayList<>();
        boolean headerSeen = false;
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final List<String> fields = parseCsvLine(line);
            if (!headerSeen) {
                if (!"original_id".equals(fields.get(0))) {
                    throw new IOException("unexpected content-mapping.csv header: " + line);
                }
                headerSeen = true;
                continue;
            }
            if (fields.size() != 8) {
                throw new IOException("expected 8 fields, got " + fields.size() + ": " + line);
            }
            rows.add(new Row(fields.get(0), fields.get(1), fields.get(2), fields.get(3),
                    fields.get(4), fields.get(5), fields.get(6), fields.get(7)));
        }
        if (!headerSeen) {
            throw new IOException("content-mapping.csv has no header row: " + file);
        }
        return rows;
    }

    /** Minimal RFC-4180 line parser: quoted fields, doubled-quote escapes. */
    static List<String> parseCsvLine(final String line) {
        final List<String> out = new ArrayList<>();
        final StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        out.add(field.toString());
        return out;
    }

    private ContentMappingCsv() {
    }
}
