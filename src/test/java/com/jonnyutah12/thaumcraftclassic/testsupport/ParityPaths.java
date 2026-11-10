package com.jonnyutah12.thaumcraftclassic.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path resolution for the unit tests that read shipped resources and, on a
 * developer machine, the decompiled 1.7.10 original (TC-M6-05-s).
 *
 * <p>Two ways a plain relative path goes wrong in this repo, both of which the
 * new parity tests hit first: the Gradle test JVM runs with the NeoForge
 * <b>run directory</b> as its working directory, not the repository root, so
 * {@code Path.of("src/main/resources/...")} throws
 * {@code NoSuchFileException}; and {@code Path.of("../reference/...")}
 * resolves to {@code <repo>/reference} — a path that never exists — so a test
 * guarded by {@code Assumptions.assumeTrue(Files.exists(...))} reports itself
 * green while checking nothing (that is the shape of the long-standing
 * {@code worldgen/StructureParityTest} skip). So: the repo root is found by
 * walking up from the working directory until the shipped lang file sits under
 * it, the reference tree is that root's sibling, and anything that ships
 * inside the mod is read from the classpath, which needs no path at all.</p>
 */
public final class ParityPaths {

    /** Marker file: the root that owns it is the repository root. */
    private static final String ROOT_MARKER =
            "src/main/resources/assets/thaumcraft/lang/en_us.json";

    private static Path repoRoot;

    private ParityPaths() {
    }

    /** The repository root, found by walking up from the test working directory. */
    public static synchronized Path repoRoot() {
        if (repoRoot == null) {
            Path cursor = Path.of("").toAbsolutePath();
            for (int depth = 0; depth < 6 && cursor != null; depth++, cursor = cursor.getParent()) {
                if (Files.isRegularFile(cursor.resolve(ROOT_MARKER))) {
                    repoRoot = cursor;
                    break;
                }
            }
            assertNotNull(repoRoot,
                    "no ancestor of the test working directory carries " + ROOT_MARKER);
        }
        return repoRoot;
    }

    /**
     * A shipped main-source file, e.g. {@code src/main/java/.../WarpLogic.java}.
     * Asserts the file is there: a moved class must fail loudly, not skip.
     */
    public static Path sourceFile(final String relative) {
        final Path file = repoRoot().resolve(relative);
        assertTrue(Files.isRegularFile(file),
                "the shipped source " + relative + " is not at " + file);
        return file;
    }

    /**
     * The decompiled original tree root ({@code ../reference/decompiled} on a
     * developer machine). Not asserted: the CI checkout has no reference tree,
     * and a test that reads it must skip (see {@link #referenceSource}).
     */
    public static Path referenceDecompiledRoot() {
        return repoRoot().getParent().resolve("reference/decompiled");
    }

    /**
     * A file under the reference tree, e.g.
     * {@code thaumcraft/thaumcraft/common/lib/WarpEvents.java}. Returns the
     * resolved path whether or not it exists so the caller can
     * {@code Assumptions.assumeTrue(Files.exists(...), ...)} — that is how the
     * code-only CI build is expected to treat these pins.
     */
    public static Path referenceSource(final String relative) {
        return referenceDecompiledRoot().resolve(relative);
    }

    /**
     * A file that ships inside the mod, read from the classpath:
     * {@code assets/thaumcraft/lang/en_us.json},
     * {@code data/thaumcraft/enchantment/ench_haste.json}, ... — the
     * {@code src/main/resources} tree is on the test classpath, so this works
     * on CI and locally with no path wiring.
     */
    public static String classpathResource(final String path) {
        try (InputStream in = ParityPaths.class.getResourceAsStream("/" + path)) {
            assertNotNull(in, path + " is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The shipped lang file. */
    public static String lang() {
        return classpathResource("assets/thaumcraft/lang/en_us.json");
    }
}
