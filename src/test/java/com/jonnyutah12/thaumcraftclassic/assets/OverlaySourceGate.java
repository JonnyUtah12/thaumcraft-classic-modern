package com.jonnyutah12.thaumcraftclassic.assets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Decides whether the original-asset overlay <em>content-inventory</em>
 * assertions may run, from the tree the overlay was built <b>from</b>.
 *
 * <p>The overlay has two sources with the same layout (build.gradle:131-142):
 * the developer's reference tree ({@code ../reference/assets}, All Rights
 * Reserved, never committed — D-004) and the placeholder tree
 * {@code scripts/make_asset_overlay_fixture.py} generates in CI from
 * {@code docs/asset-mapping.csv} and points at with
 * {@code -Ptc.referenceAssets=build/ci-fixture-assets}. Both satisfy a
 * presence check — each carries {@code thaumcraft/sounds.json} and
 * {@code textures/} — so a presence assumption separates "no overlay" (CI's
 * plain {@code build}: the assertions skip) from "overlay", and nothing else.
 * Pointed at the fixture it therefore let the reference-tree counts run against
 * placeholder bytes and fail (kanban t_70da8abe: 66 sound events expected vs
 * the fixture's 2; 262 files under {@code textures/block/} vs 256).</p>
 *
 * <p>The fix is the tree's own identity card: the fixture writes
 * {@code .ci-fixture.json} at its root declaring {@code placeholder: true} and
 * the inventory it really carries. This gate reads that marker out of the
 * overlay source dir wired by build.gradle
 * ({@code tc.originalAssetsSourceDir}), so the decision comes from the tree
 * being measured, not from a path convention a caller has to remember. A real
 * reference tree carries no marker, so its assertions keep running
 * — nothing about them is weakened; the path-shape tests
 * (every model sprite resolves, structured research locations are published
 * paths) keep running on the fixture too, because that is the half a
 * placeholder tree can prove.</p>
 *
 * <p>The fail-safe direction is "run the assertions": an unset property, a
 * missing marker or a marker that does not parse all leave the content
 * assertions in play. Only a tree that <em>declares</em> itself a placeholder
 * can silence them.</p>
 */
public final class OverlaySourceGate {

    /** build.gradle's resolved {@code originalAssetsDir}: reference tree, fixture, or absent. */
    public static final String SOURCE_DIR_PROPERTY = "tc.originalAssetsSourceDir";

    /** The fixture's self-declaration, written by scripts/make_asset_overlay_fixture.py. */
    public static final String FIXTURE_MARKER = ".ci-fixture.json";

    private OverlaySourceGate() {
    }

    /** The overlay source tree the suite was run against; null when nothing is wired. */
    public static Path sourceDir() {
        String wired = System.getProperty(SOURCE_DIR_PROPERTY, "");
        return wired.isEmpty() ? null : Path.of(wired);
    }

    /** True when the overlay source declares itself the CI placeholder fixture. */
    public static boolean sourceIsCiFixture() {
        return declaresPlaceholder(sourceDir());
    }

    /** True when {@code sourceDir/.ci-fixture.json} exists and says {@code placeholder: true}. */
    public static boolean declaresPlaceholder(Path sourceDir) {
        if (sourceDir == null) {
            return false;
        }
        Path marker = sourceDir.resolve(FIXTURE_MARKER);
        if (!Files.isRegularFile(marker)) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
            return root.has("placeholder") && root.get("placeholder").getAsBoolean();
        } catch (RuntimeException | java.io.IOException malformed) {
            // A marker that does not parse declares nothing: keep the assertions running.
            return false;
        }
    }

    /**
     * The gate the four content-inventory tests share
     * ({@code OriginalSoundOverlayTest.generatedOverlaySoundsJsonIsPlatformValid},
     * {@code SoundRegistryParityTest.generatedOverlaySoundDefinitionsAreComplete},
     * {@code SoundRegistryParityTest.csvNewAssetPathsExistInOverlay},
     * {@code OriginalTextureOverlayTest.overlaidTexturesLandOnTheModernPaths}).
     *
     * @param overlayContentPresent the presence condition the test measured before
     *                              (its generated {@code sounds.json} / overlaid directory)
     * @param presenceSkipReason    that test's own reason for "no overlay at all", kept
     *                              verbatim so the long-standing CI skip text does not move
     */
    public static void requireReferenceTreeForContentInventory(boolean overlayContentPresent,
                                                               String presenceSkipReason) {
        Optional<String> skip = contentInventorySkipReason(sourceDir(), overlayContentPresent,
                presenceSkipReason);
        Assumptions.assumeTrue(skip.isEmpty(), skip.orElse(""));
    }

    /**
     * The pure decision behind {@link #requireReferenceTreeForContentInventory}:
     * empty means the reference-tree assertions run. Fixture first, presence
     * second — with the fixture overlaid, presence is true, so the fixture arm
     * is what a fixture run reports.
     */
    public static Optional<String> contentInventorySkipReason(Path sourceDir,
                                                              boolean overlayContentPresent,
                                                              String presenceSkipReason) {
        if (declaresPlaceholder(sourceDir)) {
            return Optional.of(fixtureSkipReason(sourceDir));
        }
        if (!overlayContentPresent) {
            return Optional.of(presenceSkipReason);
        }
        return Optional.empty();
    }

    /** Names the tree, the marker, the fixture's own declared inventory, and how to get the assertions back. */
    public static String fixtureSkipReason(Path sourceDir) {
        return "overlay source " + sourceDir + " declares itself a placeholder fixture ("
                + sourceDir.resolve(FIXTURE_MARKER) + ", written by "
                + "scripts/make_asset_overlay_fixture.py from docs/asset-mapping.csv"
                + declaredInventory(sourceDir) + "): the reference-tree counts — 66 sound events over "
                + "111 oggs, 940 textures (docs/original-asset-inventory.md) — are not that tree's "
                + "contract, so the content-inventory assertions skip. They run when the overlay "
                + "source is the reference tree, i.e. ./gradlew build with no "
                + "-Ptc.referenceAssets.";
    }

    /** The fixture's declared numbers, read out of its marker; empty when it declares none. */
    private static String declaredInventory(Path sourceDir) {
        try {
            JsonObject declares = JsonParser.parseString(Files.readString(
                            sourceDir.resolve(FIXTURE_MARKER)))
                    .getAsJsonObject().getAsJsonObject("declares");
            if (declares == null) {
                return "";
            }
            return ", declaring " + declares.get("textureFiles").getAsInt() + " placeholder textures, "
                    + declares.getAsJsonArray("soundEvents").size() + " sound events over "
                    + declares.getAsJsonArray("soundFiles").size() + " ogg files";
        } catch (RuntimeException | java.io.IOException absent) {
            return "";
        }
    }
}
