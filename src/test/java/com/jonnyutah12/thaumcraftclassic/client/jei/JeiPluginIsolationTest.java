package com.jonnyutah12.thaumcraftclassic.client.jei;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPINE-4 (t_aebd14cf) isolation + dependency-shape guard for the optional JEI
 * recipe browser (D-011: the browser is an integration convenience, not port
 * content).
 *
 * <p>Three contracts, all of them the reason the card is allowed to exist at
 * all:</p>
 * <ol>
 *   <li><b>No core class references a JEI type.</b> {@code client/jei} is the
 *       only package allowed to mention {@code mezz/jei}. JEI discovers
 *       {@link ThaumcraftJeiPlugin} through {@code @JeiPlugin}; with no JEI
 *       installed nothing loads that package, so the port's load path is the
 *       same as before this card. Enforced on compiled bytecode, not source:
 *       constant-pool entries (class refs, descriptors, string constants) all
 *       appear as raw UTF-8 in a {@code .class} file, so a byte scan catches
 *       every reference form including bridged lambdas and synthetic
 *       null-checks — the same technique as
 *       {@code CuriosBridgeIsolationTest}, whose shape this copies.</li>
 *   <li><b>JEI is compile-time and runtime-optional; its jar is never bundled
 *       and never a published dependency.</b> Asserted by reading
 *       {@code build.gradle} itself: every {@code mezz.jei} dependency line is
 *       {@code compileOnly} (permanent) or a guarded {@code localRuntime}
 *       (development only). A {@code mezz.jei} line in the {@code api},
 *       {@code implementation}, {@code runtimeOnly} or {@code annotationProcessor}
 *       configuration would put a third-party jar on the published runtime path
 *       (D-016 licensing rule, recorded with the Curios precedent).</li>
 *   <li><b>The manifest declares JEI optional and client-side</b>, pinned to
 *       the exact build the target instance ships — read from
 *       gradle.properties through the {@code tc.jeiVersion} system property so
 *       the pin stays single-source.</li>
 * </ol>
 *
 * <p>What is NOT proved here, and is not provable in this harness: the
 * categories drawing on screen (the game-test harness is a dedicated server and
 * never runs JEI's client plugin discovery). The data half — every live shipped
 * row builds a renderable display, including rows that resolve to no item — is
 * proved by {@code JeiRecipeCategoryGameTest} and {@code RecipeDisplayToleranceTest}.</p>
 */
class JeiPluginIsolationTest {

    private static final String JEI_INTERNAL = "mezz/jei";
    private static final String JEI_PACKAGE = "com/jonnyutah12/thaumcraftclassic/client/jei/";

    // ------------------------------------------------ 1. bytecode isolation

    @Test
    void noCoreClassReferencesJeiTypes() throws IOException {
        final Path root = mainClassesDir();
        final List<String> offenders = new ArrayList<>();
        final List<String> pluginClasses = new ArrayList<>();
        final List<String> pluginClassesWithJei = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (final Path classFile : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                final String relative =
                        root.relativize(classFile).toString().replace(java.io.File.separatorChar, '/');
                final String text = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
                final boolean inPlugin = relative.startsWith(JEI_PACKAGE);
                if (inPlugin) {
                    pluginClasses.add(relative);
                    if (text.contains(JEI_INTERNAL)) {
                        pluginClassesWithJei.add(relative);
                    }
                    continue;
                }
                if (text.contains(JEI_INTERNAL)) {
                    offenders.add(relative);
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "classes outside client/jei reference JEI types: " + offenders
                        + " — the core mod would then fail to load on a machine with no JEI jar");
        // Sanity: the plugin is really in the compiled output and really annotated.
        assertTrue(pluginClasses.contains(JEI_PACKAGE + "ThaumcraftJeiPlugin.class"),
                "ThaumcraftJeiPlugin.class missing from compiled output: " + pluginClasses);
        // Exactly the six JEI-facing classes reference the browser; JeiRecipeSource is the
        // Minecraft-only seam the plugin reads its rows through, which is why the plugin is
        // testable with no browser on the classpath.
        assertEquals(java.util.Set.of(
                        "ThaumcraftJeiPlugin.class", "ThaumcraftRecipeCategory.class",
                        "ArcaneWorkbenchCategory.class", "CrucibleCategory.class",
                        "InfusionCategory.class", "InfusionEnchantmentCategory.class"),
                pluginClassesWithJei.stream().map(c -> c.substring(JEI_PACKAGE.length()))
                        .collect(java.util.stream.Collectors.toSet()),
                "the set of JEI-facing classes in the plugin package changed: " + pluginClassesWithJei);
        final String plugin = new String(
                Files.readAllBytes(root.resolve(JEI_PACKAGE.replace('/', java.io.File.separatorChar)
                        + "ThaumcraftJeiPlugin.class")), StandardCharsets.ISO_8859_1);
        assertTrue(plugin.contains("Lmezz/jei/api/JeiPlugin;"),
                "ThaumcraftJeiPlugin carries no @JeiPlugin annotation in its bytecode — JEI would"
                        + " never discover it and the four categories would not exist");
    }

    // ------------------------------------------------- 2. dependency shape

    @Test
    void jeiIsCompileOnlyAndLocalRuntimeOnly() throws IOException {
        final Path gradle = buildFile();
        final List<String> jeiLines = new ArrayList<>();
        for (final String line : Files.readAllLines(gradle, StandardCharsets.UTF_8)) {
            final String stripped = line.strip();
            if (stripped.startsWith("//")) {
                continue; // prose in a comment is not a dependency declaration
            }
            if (stripped.contains("mezz.jei")) {
                jeiLines.add(stripped);
            }
        }
        assertTrue(jeiLines.size() >= 4,
                "expected the JEI api (compileOnly + testImplementation) and the dev jar"
                        + " (localRuntime) in build.gradle, found " + jeiLines);
        final List<String> bundled = new ArrayList<>();
        int compileOnly = 0;
        int localRuntime = 0;
        int testImplementation = 0;
        for (final String line : jeiLines) {
            if (line.startsWith("compileOnly")) {
                compileOnly++;
            } else if (line.startsWith("localRuntime")) {
                localRuntime++;
            } else if (line.startsWith("testImplementation")) {
                testImplementation++;
            } else {
                bundled.add(line);
            }
        }
        assertTrue(bundled.isEmpty(),
                "JEI must never be a published/runtime dependency (D-016: third-party jar, never"
                        + " redistributed) — offending lines: " + bundled);
        assertTrue(compileOnly >= 2,
                "the plugin must compile against both JEI api jars (common-api + neoforge-api),"
                        + " found " + compileOnly);
        assertTrue(localRuntime >= 1,
                "the JEI dev jar must be a localRuntime artifact so the development client runs"
                        + " the plugin; -Ptc.noJei removes it to exercise the no-JEI load path");
        assertTrue(testImplementation >= 2,
                "the guards must compile against the JEI api on the test classpath in BOTH legs"
                        + " (the -Ptc.noJei run keeps them), found " + testImplementation);
        // The dev jar is the only JEI artifact behind the property guard, and the
        // guards stay unconditional: that is what makes the two legs comparable.
        final String text = Files.readString(gradle, StandardCharsets.UTF_8);
        assertTrue(text.contains("if (!project.hasProperty('tc.noJei')) {"),
                "the JEI dev jar must sit behind the -Ptc.noJei guard, like Curios's -Ptc.noCurios");
        assertTrue(text.indexOf("testImplementation \"mezz.jei") > text.indexOf("tc.noJei"),
                "the JEI test-classpath lines must be outside the -Ptc.noJei guard so the guards"
                        + " run on both legs");
    }

    @Test
    void versionPinIsSingleSource() throws IOException {
        final Properties props = new Properties();
        try (var in = Files.newInputStream(buildFile().getParent().resolve("gradle.properties"))) {
            props.load(in);
        }
        final String pinned = props.getProperty("jei_version");
        assertNotNull(pinned, "gradle.properties carries no jei_version");
        assertEquals(property("tc.jeiVersion"), pinned,
                "the tc.jeiVersion system property must be gradle.properties' jei_version");
        // The pin is the build the target instance ships; a bare "19" or a
        // range would compile against an API the runtime does not have.
        assertTrue(pinned.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"),
                "jei_version must be the exact 4-part build the target instance ships, got " + pinned);
    }

    // ---------------------------------------------------- 3. manifest shape

    @Test
    void manifestDeclaresJeiOptionalOnTheClient() throws IOException {
        final Path file = Path.of(property("tc.generatedModsToml"));
        assertTrue(Files.isRegularFile(file), "generated mods.toml not built: " + file);
        final TomlParseResult toml = Toml.parse(file);
        assertTrue(toml.errors().isEmpty(), "generated mods.toml has parse errors: " + toml.errors());
        final TomlArray deps = toml.getArray("dependencies." + property("tc.modId"));
        assertNotNull(deps, "the generated mods.toml declares no dependencies");
        for (int i = 0; i < deps.size(); i++) {
            final TomlTable dep = deps.getTable(i);
            if (!"jei".equals(dep.getString("modId"))) {
                continue;
            }
            assertEquals("optional", dep.getString("type"),
                    "JEI must be declared optional: a required dependency would stop the mod"
                            + " loading on an install without a recipe browser");
            assertEquals("CLIENT", dep.getString("side"),
                    "JEI is a client browser; declaring it BOTH would gate a dedicated server");
            assertEquals("[" + property("tc.jeiVersion") + ",)", dep.getString("versionRange"),
                    "the version floor must be the exact build the target instance ships");
            return;
        }
        throw new AssertionError("the generated mods.toml declares no jei dependency — the"
                + " optional integration is undeclared, so FML cannot order AFTER JEI");
    }

    @Test
    void noJeiLegIsWiredAndReported() {
        // The -Ptc.noJei leg must be observable from the test task, so a docs
        // claim about "both legs" is auditable in the build log.
        final String flag = property("tc.jeiLocalRuntime");
        assertTrue("true".equals(flag) || "false".equals(flag),
                "tc.jeiLocalRuntime must be the boolean 'is the dev jar on localRuntime'," + " got " + flag);
    }

    // ---------------------------------------------------------------- helpers

    private static Path mainClassesDir() {
        final String value = System.getProperty("tc.mainClassesDir");
        assertNotNull(value, "tc.mainClassesDir system property not set (see build.gradle test task)");
        final Path dir = Path.of(value);
        assertTrue(Files.isDirectory(dir), "main classes directory not built: " + dir);
        return dir;
    }

    private static Path buildFile() {
        final String value = System.getProperty("tc.buildGradleFile");
        assertNotNull(value, "tc.buildGradleFile system property not set (see build.gradle test task)");
        final Path file = Path.of(value);
        assertTrue(Files.isRegularFile(file), "build.gradle not found at " + file);
        return file;
    }

    private static String property(final String name) {
        final String value = System.getProperty(name);
        assertNotNull(value, "system property '" + name + "' is not set — run tests through ./gradlew test");
        return value;
    }
}
