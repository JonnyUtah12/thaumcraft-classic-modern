package com.jonnyutah12.thaumcraftclassic.accessory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M8-01 (D-005) isolation guard: {@code accessory/curios} is the ONLY
 * package allowed to reference {@code top.theillusivec4.curios} types, so
 * the bridge class is class-loaded only on the {@code ModList.isLoaded}
 * branch and a no-Curios runtime never touches a Curios type.
 *
 * <p>Enforced on compiled bytecode (not source): every {@code .class} under
 * the main output outside the bridge package must not contain the internal
 * name {@code top/theillusivec4/curios} anywhere — constant-pool entries
 * (class refs, method descriptors, string constants) all appear as raw UTF-8
 * in the class file, so a byte scan catches every reference form, including
 * ones a source grep would miss (bridged lambdas, synthetic null-checks).</p>
 */
class CuriosBridgeIsolationTest {

    private static final String CURIOS_INTERNAL = "top/theillusivec4/curios";
    private static final String BRIDGE_PACKAGE = "com/jonnyutah12/thaumcraftclassic/accessory/curios/";

    @Test
    void noCoreClassReferencesCuriosTypes() throws IOException {
        final Path root = mainClassesDir();
        final List<String> offenders = new ArrayList<>();
        final List<String> bridgeClasses = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (final Path classFile : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                final String relative = root.relativize(classFile).toString().replace(java.io.File.separatorChar, '/');
                final boolean inBridge = relative.startsWith(BRIDGE_PACKAGE);
                if (inBridge) {
                    bridgeClasses.add(relative);
                    continue;
                }
                if (new String(Files.readAllBytes(classFile), java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains(CURIOS_INTERNAL)) {
                    offenders.add(relative);
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "classes outside accessory/curios reference Curios types: " + offenders);
        // Sanity: the bridge package really exists and holds the bridge.
        assertTrue(bridgeClasses.stream().anyMatch(c -> c.equals(BRIDGE_PACKAGE + "CuriosBridge.class")),
                "CuriosBridge.class missing from compiled output: " + bridgeClasses);
    }

    private static Path mainClassesDir() {
        final String value = System.getProperty("tc.mainClassesDir");
        assertNotNull(value, "tc.mainClassesDir system property not set (see build.gradle test task)");
        final Path dir = Path.of(value);
        assertTrue(Files.isDirectory(dir), "main classes directory not built: " + dir);
        return dir;
    }
}
