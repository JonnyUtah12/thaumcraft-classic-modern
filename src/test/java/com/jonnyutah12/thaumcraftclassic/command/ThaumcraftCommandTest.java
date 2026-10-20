package com.jonnyutah12.thaumcraftclassic.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TC-M3-02: the /thaumcraft aspect command's embedded tag set must stay pinned
 * to the original's 48 aspects (docs/original-content-inventory.md §1, derived
 * from api/aspects/Aspect.java static finals). The aspect registry itself is
 * TC-M4-01's; this guard keeps the command's fallback set honest until then.
 */
class ThaumcraftCommandTest {

    @Test
    void aspectTagsMatchTheOriginalInventory() throws IOException {
        final String file = System.getProperty("tc.contentInventoryFile");
        assertNotNull(file, "system property 'tc.contentInventoryFile' is not set — run tests through ./gradlew test");

        final List<String> documented = new ArrayList<>();
        boolean inSection = false;
        for (final String line : Files.readAllLines(Path.of(file))) {
            if (line.startsWith("## 1. Aspects")) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("## ")) {
                break;
            }
            if (!inSection || !line.startsWith("|")) {
                continue;
            }
            final String[] cells = line.split("\\|");
            if (cells.length < 3) {
                continue;
            }
            final String tag = cells[2].trim();
            if (tag.isEmpty() || tag.equals("Tag") || tag.equals("---")) {
                continue;
            }
            documented.add(tag);
        }

        assertEquals(48, documented.size(), "inventory §1 must list all 48 aspects");
        assertEquals(documented, ThaumcraftCommand.ASPECT_TAGS,
                "command aspect tag set drifted from the original inventory");
    }
}
