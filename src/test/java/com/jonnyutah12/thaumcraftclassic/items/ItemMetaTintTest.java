package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemBaubleBlanks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BD-17 (t_f04c44fb) — the item tint table, asserted as pure maths.
 *
 * <p>The colour a client paints is a client fact no headless gate sees; the
 * numbers the client will ask for are not — they are the original's
 * {@code Item.getColorFromItemStack} arithmetic, kept in
 * {@link ItemMetaTints#color(String, int, Aspect, int, long)} where a unit
 * test can drive it without a registry, a stack, or a client. These
 * assertions are the mutation-proof pair for the tint half of the card:
 * change one number in the table and a test goes red.</p>
 */
class ItemMetaTintTest {

    private static final int WHITE = 0xFFFFFF;

    @Test
    void shardMetasTakeTheOreColourArrayShiftedByOne() {
        // ItemShard.getColorFromItemStack: md == 6 ? super : colors[md + 1].
        // Literals, not a read of the table's own array: this is the half that
        // catches a transcription slip in the array itself.
        final int[] expected = {0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};
        for (int meta = 0; meta <= 5; meta++) {
            assertEquals(expected[meta], ItemMetaTints.color("itemshard", meta, null, 0, 0L),
                    "shard meta " + meta);
        }
        assertEquals(WHITE, ItemMetaTints.color("itemshard", 6, null, 0, 0L),
                "the balanced shard (md 6) is the original's untinted meta");
    }

    @Test
    void oreColourArrayIsTheOriginalsSevenLiterals() {
        // BlockCustomOreItem.colors at reference common/blocks/BlockCustomOreItem.java:8,
        // written there in decimal: {16777215, 16777086, 16727041, 37119, 40960,
        // 15650047, 5592439}.
        assertEquals(java.util.List.of(0xFFFFFF, 0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577),
                java.util.Arrays.stream(ItemMetaTints.ORE_COLORS).boxed().toList(),
                "the shards' colour source is the original's array, value for value");
    }

    @Test
    void apprenticeRingsWearTheirPrimalAndTheBlanksWearNothing() {
        // ItemBaubleBlanks.getColorFromItemStack (reference :88-92): md 3-8 take
        // Aspect.getPrimalAspects().get(md - 3).getColor(), every pass; the
        // amulet/ring/belt blanks (0-2) fall through to super, i.e. untinted.
        for (int meta = ItemBaubleBlanks.META_APPRENTICE_FIRST; meta <= ItemBaubleBlanks.META_APPRENTICE_LAST; meta++) {
            final Aspect primal = ItemBaubleBlanks.apprenticeAspect(meta);
            assertTrue(primal != null, "apprentice ring meta " + meta + " maps to a primal");
            assertTrue(primal.isPrimal(), "meta " + meta + " maps to " + primal.getName() + ", a primal");
            for (final int layer : new int[] {0, 1}) {
                assertEquals(primal.getColor(), ItemMetaTints.color("itembaubleblanks", meta, null, layer, 0L),
                        "apprentice ring meta " + meta + " layer " + layer);
            }
        }
        for (final int blank : new int[] {ItemBaubleBlanks.META_AMULET, ItemBaubleBlanks.META_RING,
                ItemBaubleBlanks.META_BELT}) {
            assertEquals(WHITE, ItemMetaTints.color("itembaubleblanks", blank, null, 0, 0L),
                    "blank meta " + blank);
        }
        final Aspect primus = ItemBaubleBlanks.apprenticeAspect(ItemBaubleBlanks.META_APPRENTICE_FIRST);
        final Aspect secundus = ItemBaubleBlanks.apprenticeAspect(ItemBaubleBlanks.META_APPRENTICE_FIRST + 1);
        assertTrue(primus != null && secundus != null && primus != secundus,
                "the six apprentice metas map to six distinct primals");
    }

    @Test
    void essenceTintsTheOverlayLayerWithTheAspectAndNeverTheGlass() {
        final Aspect ignis = Aspect.getAspect("ignis");
        assertEquals(WHITE, ItemMetaTints.color("itemessence", 1, ignis, 0, 0L),
                "pass 0 is the phial glass — the original never tints it");
        assertEquals(ignis.getColor(), ItemMetaTints.color("itemessence", 1, ignis, 1, 0L));
        assertEquals(WHITE, ItemMetaTints.color("itemessence", 0, ignis, 1, 0L),
                "an unfilled phial (md 0) has no aspect band");
    }

    @Test
    void jarLabelOverlayCarriesTheAspectBand() {
        // ItemResource.getColorFromItemStack: pass 1 of md 13 (label_over) only
        final Aspect aqua = Aspect.getAspect("aqua");
        assertEquals(WHITE, ItemMetaTints.color("itemresource", 13, aqua, 0, 0L));
        assertEquals(aqua.getColor(), ItemMetaTints.color("itemresource", 13, aqua, 1, 0L));
        assertEquals(WHITE, ItemMetaTints.color("itemresource", 0, aqua, 1, 0L));
    }

    @Test
    void unassignedEssenceCyclesTheKnownAspectsEveryHalfSecond() {
        final List<Aspect> all = Aspect.values();
        assertTrue(all.size() > 1, "the original cycles over Aspect.aspects.values()");
        final Aspect first = all.get(0);
        final Aspect second = all.get(1);
        assertEquals(first.getColor(), ItemMetaTints.color("itemwispessence", 0, null, 0, 0L));
        assertEquals(second.getColor(),
                ItemMetaTints.color("itemwispessence", 0, null, 0, ItemMetaTints.CYCLE_MS));
        // displayAspects is a fixed array: the cycle wraps, it never runs off it.
        assertEquals(first.getColor(),
                ItemMetaTints.color("itemmanabean", 0, null, 0,
                        ItemMetaTints.CYCLE_MS * all.size()));
        final Aspect terra = Aspect.getAspect("terra");
        assertEquals(terra.getColor(), ItemMetaTints.color("itemcrystalessence", 0, terra, 0, 0L),
                "an assigned container wins over the cycle");
    }

    @Test
    void everythingElseIsUntinted() {
        assertEquals(WHITE, ItemMetaTints.color("itemcurocpearl", 3, null, 0, 0L));
        assertEquals(WHITE, ItemMetaTints.color("blockcosmeticopaque", 2, null, 1, 0L));
    }

    @Test
    void theTintedSetIsTheMeasuredSet() {
        // The complete set of Item.getColorFromItemStack overrides among the
        // shipped ids (grep of func_82790_a over reference/decompiled's
        // common/items/ and common/items/baubles/). The original also colours
        // ItemSpawnerEgg, ItemResearchNotes and the taint-fibre items — none of
        // those is shipped registered content on this branch, so they are not
        // rows here; probe 3 fails the moment one becomes shipped un-tabled.
        assertEquals(java.util.Set.of("itemshard", "itemessence", "itemresource", "itembaubleblanks",
                        "itemcrystalessence", "itemwispessence", "itemmanabean"),
                ItemMetaTints.tintedIds());
    }
}
