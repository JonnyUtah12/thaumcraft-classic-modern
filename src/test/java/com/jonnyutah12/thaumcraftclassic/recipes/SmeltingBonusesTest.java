package com.jonnyutah12.thaumcraftclassic.recipes;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-06-s evidence: the smelting-bonus <b>extraction rates</b>, pinned row
 * for row against the original.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java}
 * plus the {@code ConfigRecipes} block that calls
 * {@code ThaumcraftApi.addSmeltingBonus(String, ItemStack)} /
 * {@code addSmeltingBonus(Item, int, ItemStack)}. The original stores
 * {@code new ItemStack(out, 0, damage)} — <b>count 0</b>, i.e. the bonus is the
 * roll's result, not a free copy — and looks up in the order exact item+meta →
 * item + 32767 (any meta) → ore-dictionary name.</p>
 *
 * <p>The table is process-wide static state (the original's is a static
 * HashMap), so each test fills it from empty; {@link #theOriginalEighteenRows()}
 * is the row-by-row transcription that carries the "from the original" claim.</p>
 */
class SmeltingBonusesTest {

    @BeforeEach
    void fill() {
        SmeltingBonuses.clear();
        SmeltingBonuses.registerOriginalBonuses();
    }

    @AfterEach
    void empty() {
        SmeltingBonuses.clear();
    }

    @Test
    void theOriginalEighteenRows() {
        assertEquals(18, SmeltingBonuses.size(), "18 addSmeltingBonus calls in the original ConfigRecipes");
        assertEquals(7, SmeltingBonuses.byTag().size(), "seven of them are keyed on an ore-dictionary name");
        assertEquals(11, SmeltingBonuses.byItem().size(), "eleven are keyed on an item and a damage value");

        // Ore-dictionary rows: the ore yields its metal nugget.
        assertTag("c:ores/gold", "minecraft:gold_nugget", 0);
        assertTag("c:ores/iron", "thaumcraft:itemnugget", 0);
        assertTag("c:ores/cinnabar", "thaumcraft:itemnugget", 5);
        assertTag("c:ores/copper", "thaumcraft:itemnugget", 1);
        assertTag("c:ores/tin", "thaumcraft:itemnugget", 2);
        assertTag("c:ores/silver", "thaumcraft:itemnugget", 3);
        assertTag("c:ores/lead", "thaumcraft:itemnugget", 4);

        // TC's own nugget metas: quicksilver/chaining/gossamer metal smelt down
        // to the plain metal, the gold nugget for meta 31.
        assertItem("thaumcraft:itemnugget", 31, "minecraft:gold_nugget", 0);
        assertItem("thaumcraft:itemnugget", 16, "thaumcraft:itemnugget", 0);
        assertItem("thaumcraft:itemnugget", 21, "thaumcraft:itemnugget", 5);
        assertItem("thaumcraft:itemnugget", 17, "thaumcraft:itemnugget", 1);
        assertItem("thaumcraft:itemnugget", 18, "thaumcraft:itemnugget", 2);
        assertItem("thaumcraft:itemnugget", 19, "thaumcraft:itemnugget", 3);
        assertItem("thaumcraft:itemnugget", 20, "thaumcraft:itemnugget", 4);

        // Raw meat and fish smelt into their nugget forms; the fish row is
        // any-meta (the original registers it with the 32767 wildcard).
        assertItem("minecraft:chicken", 0, "thaumcraft:itemnuggetchicken", 0);
        assertItem("minecraft:beef", 0, "thaumcraft:itemnuggetbeef", 0);
        assertItem("minecraft:porkchop", 0, "thaumcraft:itemnuggetpork", 0);
        assertItem("minecraft:cod", SmeltingBonuses.WILDCARD_META, "thaumcraft:itemnuggetfish", 0);
    }

    private static void assertTag(final String tag, final String outItem, final int outMeta) {
        final SmeltingBonuses.Bonus bonus = SmeltingBonuses.lookup("some.mod:ore", 0, List.of(tag));
        assertNotNull(bonus, "the original registers a bonus for " + tag);
        assertEquals(outItem, bonus.itemId(), tag + " smelts to " + outItem);
        assertEquals(outMeta, bonus.meta(), tag + " yields damage value " + outMeta);
    }

    private static void assertItem(final String inItem, final int inMeta,
                                   final String outItem, final int outMeta) {
        final SmeltingBonuses.Bonus bonus = SmeltingBonuses.lookup(inItem, inMeta, List.of());
        assertNotNull(bonus, inItem + ":" + inMeta + " has no bonus row");
        assertEquals(outItem, bonus.itemId(), inItem + ":" + inMeta + " smelts to " + outItem);
        assertEquals(outMeta, bonus.meta());
    }

    @Test
    void lookupOrderIsExactItemThenAnyMetaThenTheOreName() {
        SmeltingBonuses.register("thaumcraft:test_ore", 3, "minecraft:gold_nugget", 0);
        SmeltingBonuses.register("thaumcraft:test_ore", SmeltingBonuses.WILDCARD_META, "minecraft:iron_nugget", 0);
        SmeltingBonuses.registerTag("c:ores/test", "minecraft:diamond", 0);

        assertEquals("minecraft:gold_nugget",
                SmeltingBonuses.lookup("thaumcraft:test_ore", 3, List.of("c:ores/test")).itemId(),
                "the exact item+meta row wins over the wildcard and the ore name");
        assertEquals("minecraft:iron_nugget",
                SmeltingBonuses.lookup("thaumcraft:test_ore", 9, List.of("c:ores/test")).itemId(),
                "any other meta falls through to the 32767 wildcard row");
        assertEquals("minecraft:diamond",
                SmeltingBonuses.lookup("thaumcraft:other_ore", 0, List.of("c:ores/test")).itemId(),
                "an item with no row of its own is matched by its ore-dictionary (tag) names");
        assertNull(SmeltingBonuses.lookup("thaumcraft:unregistered", 0, List.of("c:ores/nothing")));
    }

    @Test
    void anInputWithNoRowGivesNoBonus() {
        assertNull(SmeltingBonuses.lookup("minecraft:cobblestone", 0, List.of("minecraft:is_slab")),
                "the original returns null for an input it has no row for");
    }

    // --- the roll: how much the bonus is ----------------------------------

    @Test
    void withNoBellowsTheBonusDropsAQuarterOfTheTimeAndNeverMoreThanOne() {
        final RandomSource random = RandomSource.create(1234L);
        int ones = 0;
        final int rolls = 8000;
        for (int i = 0; i < rolls; i++) {
            final int count = SmeltingBonuses.rollCount(0, random);
            assertTrue(count == 0 || count == 1,
                    "with no bellows the original rolls nextInt(4) == 0 for a single item, got " + count);
            ones += count;
        }
        final double rate = ones / (double) rolls;
        assertTrue(rate > 0.21 && rate < 0.29,
                "the no-bellows drop rate is 1 in 4, measured " + rate);
    }

    @Test
    void eachBellowsAddsOneItemAtFortyFourPercent() {
        final RandomSource random = RandomSource.create(987654321L);
        final int rolls = 8000;
        long total = 0;
        for (int i = 0; i < rolls; i++) {
            final int count = SmeltingBonuses.rollCount(3, random);
            assertTrue(count >= 0 && count <= 3,
                    "three bellows make three rolls of nextFloat() < 0.44, got " + count);
            total += count;
        }
        final double mean = total / (double) rolls;
        assertTrue(mean > 1.18 && mean < 1.46,
                "the expected yield is 3 * 0.44 = 1.32 items per smelt, measured " + mean);
    }

    @Test
    void moreBellowsExtractMore() {
        final RandomSource random = RandomSource.create(555L);
        final int rolls = 6000;
        double bare = 0;
        double fed = 0;
        for (int i = 0; i < rolls; i++) {
            bare += SmeltingBonuses.rollCount(0, random);
            fed += SmeltingBonuses.rollCount(3, random);
        }
        assertTrue(fed / rolls > bare / rolls,
                "a bellows-fed furnace extracts more: bare " + bare / rolls + " vs fed " + fed / rolls);
    }

    @Test
    void rowsAreStoredWithTheOriginalsZeroCount() {
        final Map<SmeltingBonuses.Key, SmeltingBonuses.Bonus> byItem = SmeltingBonuses.byItem();
        for (final SmeltingBonuses.Bonus bonus : byItem.values()) {
            assertNotNull(bonus.itemId());
        }
        // The original stores `new ItemStack(out, 0, damage)`; the port keeps
        // the row as (item, damage) and gets the count from rollCount, so a
        // row can never smuggle a free item in.
        assertEquals("thaumcraft:itemnuggetchicken",
                byItem.get(new SmeltingBonuses.Key("minecraft:chicken", 0)).itemId());
    }
}
