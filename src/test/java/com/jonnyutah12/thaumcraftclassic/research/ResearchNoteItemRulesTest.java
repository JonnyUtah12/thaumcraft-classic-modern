package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-04-s-ii unit guards for {@link ItemResearchNotes}: the meta rules read
 * straight out of the decompiled 1.7.10 item, the tooltip/display-name key
 * shapes against the original lang, and the right-click arm order the platform
 * gives no headless way to exercise (the behaviour itself is probed in-game by
 * {@code ResearchNoteItemGameTest}).
 *
 * <p>Stack construction and registration are out of reach before the game
 * boots, so this class covers the pure rules and pins the source's branch
 * order; {@code ResearchNoteItemGameTest.namesAndRarityFollowTheOriginalMetaThreshold}
 * and {@code ...noteTooltipCarriesTheOriginalLines} run the same tables against
 * real stacks on the game-test server.</p>
 */
class ResearchNoteItemRulesTest {

    private static final String ORIGINAL_ITEM =
            "decompiled/thaumcraft/thaumcraft/common/items/ItemResearchNotes.java";
    private static final String ORIGINAL_LANG = "assets/thaumcraft/lang/en_US.lang";
    private static final String PORT_ITEM =
            "src/main/java/com/jonnyutah12/thaumcraftclassic/items/ItemResearchNotes.java";
    private static final String SHIPPED_LANG = "/assets/thaumcraft/lang/en_us.json";

    /** The original's rarity/name threshold (its 64-meta note/discovery split). */
    private static final Pattern ORIGINAL_FAMILY_THRESHOLD =
            Pattern.compile("func_77960_j\\(\\) < (\\d+)");

    /** The original's blank discovery gate, in both of its two spellings. */
    private static final Pattern ORIGINAL_BLANK_METAS =
            Pattern.compile("func_77960_j\\(\\) == (\\d+) \\|\\| stack\\.func_77960_j\\(\\) == (\\d+)");

    /** The original's warp clamp: the guard that rewrites the level, not its `warp > 0` gate. */
    private static final Pattern ORIGINAL_WARP_CLAMP =
            Pattern.compile("warp > (\\d+)\\) \\{\\s*warp = (\\d+);");

    /** The original's fragment drop: count and meta of the knowledge fragment. */
    private static final Pattern ORIGINAL_FRAGMENTS =
            Pattern.compile("ConfigItems\\.itemResource, (\\d+) \\+ world\\.field_73012_v\\.nextInt\\((\\d+)\\), (\\d+)");

    @Test
    void metaRulesAreReadOutOfTheOriginalSourceAndAgreeWithThePort() {
        final String original = ReferenceOrigins.read(ORIGINAL_ITEM);

        final Matcher threshold = ORIGINAL_FAMILY_THRESHOLD.matcher(original);
        assertTrue(threshold.find(), "the original's meta threshold disappeared: " + ORIGINAL_ITEM);
        final int familyThreshold = Integer.parseInt(threshold.group(1));
        assertEquals(ItemResearchNotes.META_DISCOVERY_FAMILY, familyThreshold,
                "the port's note/discovery threshold drifted from the original's");
        assertTrue(threshold.find(), "the original uses its threshold in both the name and the rarity rule");
        assertEquals(familyThreshold, Integer.parseInt(threshold.group(1)),
                "the original's name and rarity rules split on the same meta");

        final Matcher blanks = ORIGINAL_BLANK_METAS.matcher(original);
        assertTrue(blanks.find(), "the original's blank discovery metas disappeared");
        final int blankA = Integer.parseInt(blanks.group(2));
        final int blankB = Integer.parseInt(blanks.group(1));
        assertTrue(ItemResearchNotes.isBlankDiscoveryMeta(blankA) && ItemResearchNotes.isBlankDiscoveryMeta(blankB),
                "the port lost one of the original's blank discovery metas " + blankA + "/" + blankB);
        assertEquals(ItemResearchNotes.META_BLANK_DISCOVERY_A, Math.min(blankA, blankB));
        assertEquals(ItemResearchNotes.META_BLANK_DISCOVERY_B, Math.max(blankA, blankB));

        final Matcher clamp = ORIGINAL_WARP_CLAMP.matcher(original);
        assertTrue(clamp.find(), "the original's warp clamp disappeared");
        assertEquals(Integer.parseInt(clamp.group(2)), Integer.parseInt(clamp.group(1)),
                "the original clamps the level it tests for");
        assertEquals(ItemResearchNotes.MAX_WARP_LEVEL, Integer.parseInt(clamp.group(1)),
                "the port's warp tooltip clamp drifted from the original's");

        final Matcher fragments = ORIGINAL_FRAGMENTS.matcher(original);
        assertTrue(fragments.find(), "the original's knowledge-fragment drop disappeared");
        assertEquals(7, Integer.parseInt(fragments.group(1)), "the original drops 7 fragments");
        assertEquals(3, Integer.parseInt(fragments.group(2)), "plus rand(3)");
        assertEquals(ItemResource.META_KNOWLEDGE_FRAGMENT, Integer.parseInt(fragments.group(3)),
                "the fragment drop is itemresource meta 9");
    }

    @Test
    void theMetaThresholdSplitsNameAndRarityExactlyOnce() {
        final int split = ItemResearchNotes.META_DISCOVERY_FAMILY;
        for (int meta = 0; meta <= 256; meta++) {
            final boolean discovery = ItemResearchNotes.isDiscoveryFamily(meta);
            assertEquals(meta >= split, discovery, "family gate at meta " + meta);
            assertEquals(discovery ? ".discovery" : "", ItemResearchNotes.nameKeySuffixForMeta(meta),
                    "name suffix at meta " + meta);
            assertEquals(discovery
                            ? net.minecraft.world.item.Rarity.EPIC
                            : net.minecraft.world.item.Rarity.RARE,
                    ItemResearchNotes.rarityForMeta(meta), "rarity at meta " + meta);
        }
        assertFalse(ItemResearchNotes.isBlankDiscoveryMeta(split),
                "the family threshold is not itself a blank note");
        assertFalse(ItemResearchNotes.isBlankDiscoveryMeta(-1));
    }

    @Test
    void stackBuildersGuardTheirOwnFamilies() {
        // The builders are the port's documented seams for metas the
        // item_meta carrier cannot hold, so they must refuse foreign metas
        // rather than produce a stack the original has no rule for.
        assertThrows(IllegalArgumentException.class, () -> ItemResearchNotes.blankDiscoveryNote(7));
        assertThrows(IllegalArgumentException.class, () -> ItemResearchNotes.blankDiscoveryNote(64));
        assertThrows(IllegalArgumentException.class, () -> ItemResearchNotes.discoveryFamilyStack(63));
        assertThrows(IllegalArgumentException.class, () -> ItemResearchNotes.discoveryFamilyStack(-1));
    }

    @Test
    void tooltipKeysCarryTheOriginalStringsAndTheWarpPlaceholderIsPortable() {
        final JsonObject shipped = shippedLang();
        final String originalLang = ReferenceOrigins.read(ORIGINAL_LANG);

        // The two blank-note lines and the warp block keep the original key
        // shapes; the labels must be the original's strings, formatting codes
        // stripped (1.21.1 renders section codes literally, so the port carries
        // them as component styling instead — see docs/BEHAVIORAL_DEVIATIONS.md).
        for (String key : new String[] {"item.researchnotes.unknown.1", "item.researchnotes.unknown.2"}) {
            assertEquals(originalValue(originalLang, key), shipped.get(key).getAsString(),
                    key + " drifted from the original lang");
        }
        for (int level = 1; level <= ItemResearchNotes.MAX_WARP_LEVEL; level++) {
            final String key = ItemResearchNotes.KEY_FORBIDDEN_LEVEL_PREFIX + level;
            assertEquals(originalValue(originalLang, key), shipped.get(key).getAsString(),
                    key + " drifted from the original lang");
        }

        final String originalForbidden = originalValue(originalLang, ItemResearchNotes.KEY_FORBIDDEN);
        assertTrue(originalForbidden.contains("%n"), "the original's warp line carries its %n placeholder");
        final String forbidden = shipped.get(ItemResearchNotes.KEY_FORBIDDEN).getAsString();
        assertTrue(forbidden.contains("%s"), "the port's warp line lost the level placeholder: " + forbidden);
        assertFalse(forbidden.contains("%n"), "1.21.1's formatter has no %n: " + forbidden);
        assertFalse(forbidden.contains("\u00a7"), "the shipped file must not carry section codes: " + forbidden);
        assertEquals(originalForbidden.replace("%n", "%s").replace("\u00a70", ""), forbidden,
                "the warp line drifted from the original's wording");

        // The display-name labels: the original's two keys, under the port's
        // registry-derived key (TC-M7-01 naming convention).
        assertEquals(originalValue(originalLang, "item.researchnotes.name"),
                shipped.get("item.thaumcraft.itemresearchnotes").getAsString(),
                "the note label drifted from the original's");
        assertEquals(originalValue(originalLang, "item.discovery.name"),
                shipped.get("item.thaumcraft.itemresearchnotes.discovery").getAsString(),
                "the discovery label drifted from the original's");
    }

    @Test
    void theRightClickArmsKeepTheOriginalsOrderAndGuards() {
        final String port = read(ReferenceOrigins.projectFile(PORT_ITEM));

        // Branch 1 (complete note) is tested before branch 2 (blank discovery),
        // and its requisite gate returns before anything is completed.
        final int completeGate = port.indexOf("note.complete() && !ResearchManager.isResearchComplete");
        final int requisitesGate = port.indexOf("doesPlayerHaveRequisites", completeGate);
        // The arm's own gate, not the stack builder's family guard.
        final int blankGate = port.indexOf("if (isBlankDiscoveryMeta(meta))");
        final int completion = port.indexOf("completeWithSiblings(serverPlayer, note.key())");
        assertTrue(completeGate >= 0, "the complete-note arm is missing");
        assertTrue(requisitesGate > completeGate && requisitesGate < completion,
                "the hasRequisites gate no longer returns before the research is completed");
        assertTrue(blankGate > completion, "the blank discovery arm runs before the complete-note arm");

        // The note is consumed in the completion arm and in the reveal's FAIL
        // arm; the success arm spends the blank in place (the original stamps
        // the fresh puzzle state onto the same stack, meta back to 0).
        assertEquals(2, count(port, "stack.shrink(1)"),
                "the consumption points drifted from the original's two");

        // FAIL arm: fragments after the reveal fails; success arm: the generated
        // note after the entry is looked up, and meta back to 0.
        final int fail = port.indexOf("\"FAIL\".equals(key)");
        assertTrue(fail > 0, "the reveal's FAIL sentinel is missing");
        assertTrue(port.indexOf("7 + level.getRandom().nextInt(3)") > fail,
                "the FAIL arm no longer drops 7 + rand(3) fragments");
        assertTrue(port.indexOf("META_KNOWLEDGE_FRAGMENT") > fail,
                "the FAIL arm no longer drops the knowledge fragment meta");
        assertTrue(port.indexOf("findHiddenResearch") < port.indexOf("NotePuzzle.generate"),
                "the success arm generates a note without asking the reveal rules for a key");
        assertTrue(port.indexOf("stack.set(ModDataComponents.ITEM_META.get(), 0)")
                        > port.indexOf("NotePuzzle.generate"),
                "the revealed note is not reset to the original's meta 0");
    }

    @Test
    void theDisplayNameIsDerivedFromTheRegisteredIdNotAHardcodedLegacyKey() {
        final String port = read(ReferenceOrigins.projectFile(PORT_ITEM));
        final String original = ReferenceOrigins.read(ORIGINAL_ITEM);

        // TC-M7-01: the key comes from the registered id, with the original's
        // meta-64 suffix rule on top.
        assertTrue(port.contains("BuiltInRegistries.ITEM.getKey(this)"),
                "the display name is no longer derived from the registered id");
        assertTrue(port.contains("nameKeySuffixForMeta(storedMeta(stack))"),
                "the display name lost the original's meta-64 family suffix rule");
        assertFalse(port.contains("\"item.researchnotes.name\""),
                "the port hardcodes the 1.7.10 label key; the id contract owns it");
        assertFalse(port.contains("\"item.discovery.name\""),
                "the port hardcodes the 1.7.10 discovery key; the id contract owns it");

        // The original really did use those two keys, so the mapping is honest.
        assertTrue(original.contains("\"item.researchnotes.name\"")
                        && original.contains("\"item.discovery.name\""),
                "the original's two-name rule is not the one the port mirrors");
    }

    // ---- helpers ------------------------------------------------------------

    private static JsonObject shippedLang() {
        try (InputStream in = ResearchNoteItemRulesTest.class.getResourceAsStream(SHIPPED_LANG)) {
            assertTrue(in != null, "the shipped lang is not on the classpath: " + SHIPPED_LANG);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The original {@code key=value} line, with 1.7.10 section codes stripped. */
    private static String originalValue(final String lang, final String key) {
        for (final String line : lang.split("\r?\n")) {
            final int equals = line.indexOf('=');
            if (equals > 0 && line.substring(0, equals).trim().equals(key)) {
                return stripCodes(line.substring(equals + 1)).trim();
            }
        }
        throw new AssertionError("the original lang has no " + key);
    }

    private static String stripCodes(final String value) {
        return value.replace("\u00a7l", "").replace("\u00a75", "").replace("\u00a7n", "")
                .replace("\u00a7r", "").replace("\u00a70", "");
    }

    private static String read(final Path path) {
        assertTrue(path != null, "the port source is not reachable from this working tree");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int count(final String haystack, final String needle) {
        int total = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            total++;
        }
        return total;
    }
}
