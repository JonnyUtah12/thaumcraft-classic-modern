package com.jonnyutah12.thaumcraftclassic.research;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates the decompiled 1.7.10 tree ({@code ../reference}) for the tests that
 * pin the port to the original source.
 *
 * <p>The bare {@code ../reference} idiom the older parity tests use silently
 * skips inside a {@code .worktrees/<task>} checkout, which sits one level
 * deeper than the project dir. This helper walks up instead, so the parity
 * assertions actually run there.</p>
 */
final class ReferenceOrigins {

    private static final int MAX_DEPTH = 4;

    private ReferenceOrigins() {
    }

    /**
     * The original tree's file, relative to the repository root — the common
     * case being {@code reference/decompiled/...}. Returns null when not found.
     */
    static Path projectFile(String relativeToRepoRoot) {
        Path base = Path.of(".").toAbsolutePath().normalize();
        for (int up = 0; up <= MAX_DEPTH; up++) {
            Path candidate = base.resolve(relativeToRepoRoot);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            base = base.getParent();
            if (base == null) {
                break;
            }
        }
        return null;
    }

    /** The reference file, or null when the decompiled tree is not checked out. */
    static Path find(String relative) {
        return projectFile("reference/" + relative);
    }

    /** The reference file's text, skipping the test when it is absent. */
    static String read(String relative) {
        Path path = find(relative);
        Assumptions.assumeTrue(path != null, "decompiled original not checked out: " + relative);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
