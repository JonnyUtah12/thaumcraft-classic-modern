package com.jonnyutah12.thaumcraftclassic.registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-04a oredict guard: every oredict row this catalog card owns
 * (docs/catalog-triage.md row list) is flipped to done and resolves to an
 * item tag whose id is the row's new_id and whose single member is the
 * family item named by the row's {@code stack=} note. The original's
 * meta-precise OreDictionary registrations become item-level tags plus
 * {@code neoforge:data_component} ingredients at use sites
 * (docs/architecture/registration.md), so the tag membership check is
 * family-level and the meta precision is guarded by the item round-trip
 * probes (CatalogResourcesGameTest).
 */
final class CatalogOredictTagsTest {

    private static final Pattern STACK_CLASS = Pattern.compile("stack=(\\w+)");
    private static final List<String> OWNED_CLASSES = List.of("ItemResource", "ItemShard", "ItemNugget");

    private static Path csvFile() {
        return Path.of(System.getProperty("tc.contentMappingFile", ""));
    }

    private static Path dataDir() {
        return Path.of(System.getProperty("tc.modDataDir", ""));
    }

    @Test
    void ownedOredictRowsResolveToItemTags() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(csvFile()), "tc.contentMappingFile not set");
        Assumptions.assumeTrue(Files.isDirectory(dataDir()), "tc.modDataDir not set");

        final List<ContentMappingCsv.Row> rows = ContentMappingCsv.read(csvFile());
        int checked = 0;
        for (final ContentMappingCsv.Row row : rows) {
            if (!"oredict".equals(row.kind())) {
                continue;
            }
            final Matcher matcher = STACK_CLASS.matcher(row.notes());
            if (!matcher.find() || !OWNED_CLASSES.contains(matcher.group(1))) {
                continue; // owned by another catalog card
            }
            final String ownerClass = matcher.group(1);

            assertEquals("done", row.implementationStatus(),
                    "TC-M6-04a owns oredict row " + row.originalId() + " and must mark it done");

            // new_id is thaumcraft:oredict.<name>; the tag file mirrors the id path.
            final String[] parts = row.newId().split(":", 2);
            assertEquals("thaumcraft", parts[0], "tag namespace must be the mod id: " + row.newId());
            final Path tagFile = dataDir().resolve(parts[0]).resolve("tags/item").resolve(parts[1] + ".json");
            assertTrue(Files.isRegularFile(tagFile),
                    "oredict row " + row.newId() + " has no tag file at " + tagFile);

            final JsonObject tag = JsonParser.parseString(Files.readString(tagFile)).getAsJsonObject();
            assertTrue(tag.get("required").getAsBoolean(), "tag " + row.newId() + " must be required");
            final JsonArray values = tag.getAsJsonArray("values");
            assertEquals(1, values.size(),
                    "tag " + row.newId() + " must contain exactly its owner family item");
            final String expectedOwner =
                    "thaumcraft:" + ownerClass.toLowerCase(java.util.Locale.ROOT);
            assertEquals(expectedOwner, values.get(0).getAsString(),
                    "tag " + row.newId() + " must contain the family item from its stack= note");
            checked++;
        }
        // The TC-M6-04a row list in docs/catalog-triage.md pins this count:
        // 6 shards + 9 nuggets + 7 clusters + ingotThaumium/ingotVoid +
        // quicksilver + gemAmber.
        assertEquals(26, checked, "expected exactly 26 TC-M6-04a oredict rows");
    }
}
