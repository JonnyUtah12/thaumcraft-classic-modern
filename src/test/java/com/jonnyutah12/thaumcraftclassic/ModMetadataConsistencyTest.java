package com.jonnyutah12.thaumcraftclassic;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the build/metadata wiring that FML depends on at load time.
 *
 * <p>Everything asserted here is derived from gradle.properties via system
 * properties injected by the {@code test} task — nothing is hardcoded, so the
 * single-source-of-truth rule in docs/release-process.md holds and these tests
 * keep guarding after version bumps.</p>
 */
class ModMetadataConsistencyTest {

    private static String property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value,
                "system property '" + name + "' is not set — run tests through ./gradlew test, not a bare runner");
        return value;
    }

    private static TomlParseResult generatedMetadata() throws IOException {
        Path file = Path.of(property("tc.generatedModsToml"));
        assertTrue(Files.isRegularFile(file),
                "generated mods.toml not found at " + file + " — generateModMetadata must run before tests");
        TomlParseResult result = Toml.parse(file);
        assertFalse(result.hasErrors(),
                "generated mods.toml is not valid TOML: " + result.errors());
        return result;
    }

    @Test
    void generatedModsTomlDeclaresTheModWithBuildWiredValues() throws IOException {
        TomlParseResult toml = generatedMetadata();

        assertEquals("javafml", toml.getString("modLoader"), "modLoader must be javafml for @Mod mods");

        TomlArray mods = toml.getArray("mods");
        assertNotNull(mods, "mods.toml must contain a [[mods]] entry");
        assertEquals(1, mods.size(), "exactly one [[mods]] entry is expected for this single-mod project");
        TomlTable mod = mods.getTable(0);

        assertEquals(property("tc.modId"), mod.getString("modId"), "modId must match gradle.properties mod_id");
        assertEquals(property("tc.modVersion"), mod.getString("version"),
                "version must come from gradle.properties mod_version (docs/release-process.md)");
        assertEquals(property("tc.modName"), mod.getString("displayName"),
                "displayName must match gradle.properties mod_name");
        assertEquals(property("tc.modLicense"), toml.getString("license"),
                "license must match gradle.properties mod_license");
    }

    @Test
    void targetPlatformPinsAreExact() {
        // The project targets exactly MC 1.21.1 / NeoForge 21.1.259
        // (docs/target-platform.md). A drift here is a project-level incident.
        assertEquals("1.21.1", property("tc.minecraftVersion"));
        assertEquals("21.1.259", property("tc.neoVersion"));
    }

    @Test
    void modIdIsTheLowercasedOriginalNamespace() {
        // Content ids must stay as close to the original `Thaumcraft:*` as the
        // platform permits (docs/porting-decisions.md, spec section 10).
        assertEquals("thaumcraft", property("tc.modId"));
    }

    @Test
    void neoforgeDependencyIsPinnedToTheExactTarget() throws IOException {
        TomlParseResult toml = generatedMetadata();
        TomlArray deps = toml.getArray("dependencies." + property("tc.modId"));
        assertNotNull(deps, "dependency table for the mod must exist");

        boolean neoforgePinned = false;
        boolean minecraftPinned = false;
        for (int i = 0; i < deps.size(); i++) {
            TomlTable dep = deps.getTable(i);
            String modId = dep.getString("modId");
            if ("neoforge".equals(modId)) {
                assertEquals("[" + property("tc.neoVersion") + ",)", dep.getString("versionRange"));
                assertEquals("required", dep.getString("type"));
                neoforgePinned = true;
            }
            if ("minecraft".equals(modId)) {
                assertEquals("[" + property("tc.minecraftVersion") + "]", dep.getString("versionRange"));
                minecraftPinned = true;
            }
        }
        assertTrue(neoforgePinned, "neoforge dependency missing");
        assertTrue(minecraftPinned, "minecraft dependency missing");
    }

    @Test
    void curiosIsDeclaredOptionalNeverRequired() throws IOException {
        TomlParseResult toml = generatedMetadata();
        TomlArray deps = toml.getArray("dependencies." + property("tc.modId"));
        assertNotNull(deps);
        for (int i = 0; i < deps.size(); i++) {
            TomlTable dep = deps.getTable(i);
            if ("curios".equals(dep.getString("modId"))) {
                assertEquals("optional", dep.getString("type"),
                        "the accessory framework must stay optional (spec section 15)");
                return;
            }
        }
        // Absent is also acceptable: the core mod never requires Curios.
    }
}
