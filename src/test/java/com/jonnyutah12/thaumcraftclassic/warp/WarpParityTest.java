package com.jonnyutah12.thaumcraftclassic.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jonnyutah12.thaumcraftclassic.testsupport.ParityPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-05-s: pins the warp mechanic's numbers to the decompiled 1.7.10
 * original (the same regex-over-source pin style as
 * {@code worldgen/StructureParityTest}): cadence, gates, roll arithmetic,
 * effect durations, amplifiers and the death gaze geometry.
 *
 * <p>The reference-tree reads skip on the code-only CI checkout
 * ({@link ParityPaths#referenceSource}); the shipped-source read and every
 * constant check run everywhere, so the port side of each pin is never
 * silently unmeasured.</p>
 */
class WarpParityTest {

    private static final Path WARP_EVENTS = ParityPaths.referenceSource(
            "thaumcraft/thaumcraft/common/lib/WarpEvents.java");
    private static final Path EVENT_HANDLER_ENTITY = ParityPaths.referenceSource(
            "thaumcraft/thaumcraft/common/lib/events/EventHandlerEntity.java");

    @Test
    void cadenceGatesAndRollArithmeticMatchTheOriginal() throws Exception {
        Assumptions.assumeTrue(Files.exists(WARP_EVENTS), "original source not checked out");
        final String src = Files.readString(WARP_EVENTS);

        // The event cadence and its gates (EventHandlerEntity.livingTick).
        final String entity = Files.readString(EVENT_HANDLER_ENTITY);
        assertTrue(entity.contains("% 2000 == 0"),
                "the original's warp cadence is ticksExisted % 2000 (EventHandlerEntity.java:236)");
        // EventHandlerEntity.java:236 reads
        //   !Config.wuss && player.field_70173_aa > 0 && player.field_70173_aa % 2000 == 0
        //       && !player.func_82165_m(Config.potionWarpWardID)
        // — the ward is the config-held potion id, not a Potion field.
        assertTrue(entity.contains("!Config.wuss"), "wuss mode disables the roll");
        assertTrue(entity.contains("player.field_70173_aa > 0"), "the first tick never rolls");
        assertTrue(entity.contains("Config.potionWarpWardID"), "the warp ward suppresses the event");
        assertTrue(entity.contains("WarpEvents.checkWarpEvent(player)"), "the roll entry point");
        assertTrue(entity.contains("% 10 == 0"),
                "the death gaze runs on its own 10-tick cadence (EventHandlerEntity.java:241)");

        assertTrue(src.contains("warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter)"),
                "the fire gate must be transcribed as written");
        assertTrue(src.contains("Math.min(100, (warp + warp + warpCounter) / 3)"),
                "the roll arithmetic");
        assertTrue(src.contains("(int)(warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0))"),
                "the counter rewrite");
        assertTrue(src.contains("int eff = player.field_70170_p.field_73012_v.nextInt(warp);"),
                "eff is drawn from the world random over the rolled warp");

        // The port's constants must equal the original's literals.
        assertEquals(2000, WarpLogic.WARP_EVENT_INTERVAL_TICKS);
        assertEquals(10, WarpLogic.DEATH_GAZE_INTERVAL_TICKS);
        assertEquals(100, WarpLogic.MAX_ROLL_WARP);
        assertEquals(3, WarpLogic.MAX_EFFECT_AMPLIFIER);
        assertEquals(80, WarpLogic.DEATH_GAZE_WITHER_TICKS);
        assertEquals(8, WarpLogic.DEATH_GAZE_BASE_RANGE);
        assertEquals(3, WarpLogic.DEATH_GAZE_RANGE_PER_LEVEL);
        assertEquals(24, WarpLogic.DEATH_GAZE_MAX_RANGE);
        assertEquals(8, WarpLogic.MAX_GUARDIANS);
        assertEquals(50, WarpLogic.MAX_SPIDERS);
        assertEquals(0.75F, WarpLogic.DEATH_GAZE_APERTURE_RADIANS);

        // The literals the original spells out, in the original's own text.
        for (final String literal : new String[] {"5000", "6000", "1200",
                "Math.min(32000, 10 * warp)", "Math.min(40 * warp, 6000)",
                "Math.min(32000, 5 * warp)", "Math.min(3, warp / 15)",
                "Math.min(8, ", "Math.min(50, warp)", "Math.min(8 + level * 3, 24)",
                "isVisibleTo(0.75F", "Potion.field_82731_v",
                // WarpEvents.java:224 — the temp-warp decay of the method tail. The
                // original keys knowledge by the player uuid (func_70005_c_ =
                // getUniqueID), which is the shipped PlayerWarpData lookup too.
                "Thaumcraft.proxy.getPlayerKnowledge().addWarpTemp(player.func_70005_c_(), -1)"}) {
            assertTrue(src.contains(literal), "the original does not contain " + literal);
        }
        // fields_1.7.10_mcp_stable_11.csv:4576 maps field_82731_v to Potion.wither,
        // so the decompiled death-gaze line is the original's wither application:
        // "func_70690_d(new PotionEffect(Potion.field_82731_v.func_76396_c(), 80))".
        assertTrue(src.contains("new PotionEffect(Potion.field_82731_v.func_76396_c(), 80)"),
                "the death gaze applies wither for 80 ticks");
    }

    @Test
    void theWarpTextKeysAreTheOriginalsVerbatim() throws Exception {
        Assumptions.assumeTrue(Files.exists(WARP_EVENTS), "original source not checked out");
        // Gradle wires the original lang file (build.gradle: tc.originalLangFile).
        final Path originalLangFile = Path.of(System.getProperty("tc.originalLangFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(originalLangFile),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");
        final String src = Files.readString(WARP_EVENTS);
        final String originalLang = Files.readString(originalLangFile);
        // Read the shipped file as JSON: string surgery on a JSON line leaves
        // the closing quote on the last entry of the object, which is the shape
        // this pin used to fail on.
        final var lang = com.google.gson.JsonParser
                .parseString(ParityPaths.lang()).getAsJsonObject();

        int checked = 0;
        for (int n = 1; n <= 15; n++) {
            final String key = "warp.text." + n;
            assertTrue(src.contains('"' + key + '"'), "the original uses " + key);
            assertTrue(lang.has(key), "the port must ship " + key);
            final String original = originalLang.lines()
                    .filter(l -> l.startsWith(key + "="))
                    .map(l -> l.substring(key.length() + 1).replace("\r", "").trim())
                    .findFirst().orElseThrow(() -> new AssertionError(key + " missing from en_US.lang"));
            // Verbatim, byte for byte, including the original's trailing space
            // on warp.text.9 (en_US.lang:31).
            assertEquals(original, lang.get(key).getAsString().trim(), key + " wording must be verbatim");
            checked++;
        }
        assertEquals(15, checked);
    }

    @Test
    void hasteMultiplierMatchesTheOriginal() throws Exception {
        final String port = Files.readString(ParityPaths.sourceFile(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/enchantment/EnchantmentBehaviours.java"));
        // The port side is measured on every build, CI included.
        assertTrue(port.contains("0.015F"), "the haste bonus is 0.015F per level");
        assertTrue(port.contains("MOVEMENT_SPEED"),
                "the bonus rides the movement-speed attribute (field_70701_bs)");
        assertTrue(port.contains("flying"),
                "the gate is capabilities.isFlying, not creative mode");

        Assumptions.assumeTrue(Files.exists(EVENT_HANDLER_ENTITY), "original source not checked out");
        final String entity = Files.readString(EVENT_HANDLER_ENTITY);
        assertTrue(entity.contains("0.015F"), "the original's haste bonus is 0.015F per level");
    }
}
