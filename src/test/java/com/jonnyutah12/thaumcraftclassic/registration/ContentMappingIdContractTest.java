package com.jonnyutah12.thaumcraftclassic.registration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the id contract in docs/content-mapping.csv (TC-M2-01):
 *
 * <ul>
 *   <li>every {@code new_id} is a legal 1.21.1 resource location under the
 *       {@code thaumcraft} namespace (charset mirrors vanilla
 *       ResourceLocation: path {@code [a-z0-9/._-]+});</li>
 *   <li>every {@code new_id} is exactly {@code "thaumcraft:" +
 *       lower(original id path)} — lowercasing is the only permitted
 *       transformation (D-001 addendum), so the mapping is mechanically
 *       auditable and cannot drift silently;</li>
 *   <li>ids are unique per kind except for the documented shared-id groups
 *       (wand rod/staff pairs, multi-use oredict names, multi-rule mob
 *       spawns, and the wand_trigger pseudo-id);</li>
 *   <li>the kind vocabulary and implementation_status vocabulary are the
 *       documented ones (docs/architecture/registration.md).</li>
 * </ul>
 *
 * <p>Pure JDK + the shared {@link ContentMappingCsv} reader: no Minecraft
 * classpath needed. The in-game half (ids resolve to live objects) is
 * RegistrationGameTest.</p>
 */
class ContentMappingIdContractTest {

    private static final Pattern LEGAL_ID = Pattern.compile("^thaumcraft:[a-z0-9/._-]+$");

    private static final Set<String> KINDS = Set.of(
            "aspect", "block", "fluid", "tile_entity", "item", "entity", "spawner_egg",
            "mob_spawn", "wand_component", "research_category", "research", "recipe",
            "wand_trigger", "config_option", "gui", "enchantment", "potion", "biome",
            "dimension", "node_type", "node_modifier", "worldgen_feature", "oredict");

    private static final Set<String> STATUSES = Set.of("not_started", "in_progress", "done", "deferred");

    /**
     * (kind, new_id) groups that legitimately share one id because the
     * original registered several entries under it (or, for wand_trigger,
     * because the original "id" is a registration call signature). Each group
     * resolves to a single registered object (or is reference-only).
     */
    private static final Set<String> SHARED_ID_ALLOWLIST = Set.of(
            "wand_component|thaumcraft:greatwood",
            "wand_component|thaumcraft:silverwood",
            "wand_component|thaumcraft:obsidian",
            "wand_component|thaumcraft:blaze",
            "wand_component|thaumcraft:ice",
            "wand_component|thaumcraft:quartz",
            "wand_component|thaumcraft:bone",
            "wand_component|thaumcraft:reed",
            "oredict|thaumcraft:oredict.logwood",
            "oredict|thaumcraft:oredict.plankwood",
            "oredict|thaumcraft:oredict.slabwood",
            "oredict|thaumcraft:oredict.treesapling",
            "mob_spawn|thaumcraft:monster.entityfirebat",
            "wand_trigger|thaumcraft:wandtrigger");

    private static String property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value,
                "system property '" + name + "' is not set — run tests through ./gradlew test, not a bare runner");
        return value;
    }

    private static List<ContentMappingCsv.Row> rows() throws IOException {
        Path file = Path.of(property("tc.contentMappingFile"));
        assertTrue(Files.isRegularFile(file),
                "content-mapping.csv not found at " + file + " — the test task must pass tc.contentMappingFile");
        return ContentMappingCsv.read(file);
    }

    @Test
    void everyNewIdIsALegalThaumcraftResourceLocation() throws IOException {
        List<ContentMappingCsv.Row> rows = rows();
        // 1049 rows: the 1048 of the frozen inventory plus the gui_crucible
        // row TC-M7-04-m1 added (the FEATURE_MATRIX gui_crucible label had no
        // csv row; the row records the measured reference finding - the
        // 1.7.10 crucible ships no GUI - and CrucibleGuiParityTest pins it).
        assertEquals(1049, rows.size(),
                "content-mapping.csv row count changed — the id contract is frozen; update this count only with an inventory re-run (TC-M0-04)");

        List<String> problems = new ArrayList<>();
        for (ContentMappingCsv.Row row : rows) {
            if (!KINDS.contains(row.kind())) {
                problems.add("unknown kind '" + row.kind() + "' for " + row.originalId());
            }
            if (!STATUSES.contains(row.implementationStatus())) {
                problems.add("unknown implementation_status '" + row.implementationStatus() + "' for " + row.newId());
            }
            if (!LEGAL_ID.matcher(row.newId()).matches()) {
                problems.add("new_id is not a legal thaumcraft resource location: '" + row.newId() + "' (" + row.originalId() + ")");
            }
            if (!row.originalId().contains(":") || row.originalId().substring(row.originalId().indexOf(':') + 1).isEmpty()) {
                problems.add("original_id has no path: " + row.originalId());
            }
        }
        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems.stream().limit(25).toList()));
    }

    @Test
    void newIdIsExactlyTheLowercasedOriginalPath() throws IOException {
        List<String> problems = new ArrayList<>();
        for (ContentMappingCsv.Row row : rows()) {
            String originalPath = row.originalId().substring(row.originalId().indexOf(':') + 1);
            String expected = "thaumcraft:" + originalPath.toLowerCase(Locale.ROOT);
            if (row.kind().equals("wand_trigger")) {
                // Original "id" is a registerWandBlockTrigger call signature (commas,
                // quotes) — not a resource location; rows share the pseudo-id.
                if (!row.newId().equals("thaumcraft:wandtrigger")) {
                    problems.add("wand_trigger row must carry the shared pseudo-id: " + row.originalId());
                }
                continue;
            }
            if (!row.newId().equals(expected)) {
                problems.add("new_id drifted from the D-001 rule: " + row.originalId()
                        + " -> " + row.newId() + " (expected " + expected + ")");
            }
        }
        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems.stream().limit(25).toList()));
    }

    @Test
    void sharedIdsAreOnlyTheDocumentedGroups() throws IOException {
        Set<String> seen = new HashSet<>();
        Set<String> shared = new HashSet<>();
        for (ContentMappingCsv.Row row : rows()) {
            String key = row.kind() + "|" + row.newId();
            if (!seen.add(key)) {
                shared.add(key);
            }
        }
        Set<String> unexpected = new HashSet<>(shared);
        unexpected.removeAll(SHARED_ID_ALLOWLIST);
        assertTrue(unexpected.isEmpty(),
                "new (kind, new_id) collisions outside the documented shared-id groups: " + unexpected);
    }
}
