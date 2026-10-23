package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TC-M8-02 acceptance: the pure rule tables of the six bauble item classes
 * against the original classes in {@code common/items/baubles/} — slot
 * routing (getBaubleType), per-meta runic charge tables (getRunicCharge),
 * the apprentice-ring discount rule (getVisDiscount), vis amulet capacities
 * (getMaxVis), the worn-tick transfer cap, and the hover girdle fall step.
 * Item instances cannot be constructed in the unit-test JVM (frozen
 * registry), so these exercise the static tables the classes delegate to.
 */
class BaubleRulesTest {

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    private static final Aspect AER = aspect("aer");
    private static final Aspect IGNIS = aspect("ignis");

    // ------------------------------------------- ItemBaubleBlanks.getBaubleType

    @Test
    void blankSlotRoutingMatchesOriginalSwitch() {
        assertEquals(Set.of(AccessorySlot.AMULET), ItemBaubleBlanks.slotsForMeta(0));
        assertEquals(Set.of(AccessorySlot.RING), ItemBaubleBlanks.slotsForMeta(1));
        assertEquals(Set.of(AccessorySlot.BELT), ItemBaubleBlanks.slotsForMeta(2));
        for (int meta = 3; meta <= 8; meta++) {
            assertEquals(Set.of(AccessorySlot.RING), ItemBaubleBlanks.slotsForMeta(meta),
                    "apprentice meta " + meta + " must route to RING");
        }
    }

    // ---------------------------------- ItemBaubleBlanks primal indexing (85-102)

    @Test
    void apprenticeMetasIndexPrimalsInDeclarationOrder() {
        // Original: Aspect.getPrimalAspects().get(meta - 3) — declaration
        // order aer, terra, ignis, aqua, ordo, perditio (api Aspect 17-22).
        assertEquals("aer", ItemBaubleBlanks.apprenticeTag(3));
        assertEquals("terra", ItemBaubleBlanks.apprenticeTag(4));
        assertEquals("ignis", ItemBaubleBlanks.apprenticeTag(5));
        assertEquals("aqua", ItemBaubleBlanks.apprenticeTag(6));
        assertEquals("ordo", ItemBaubleBlanks.apprenticeTag(7));
        assertEquals("perditio", ItemBaubleBlanks.apprenticeTag(8));
        assertNull(ItemBaubleBlanks.apprenticeTag(0));
        assertNull(ItemBaubleBlanks.apprenticeTag(2));
        assertNull(ItemBaubleBlanks.apprenticeTag(9));
    }

    @Test
    void apprenticeDiscountIsOnePercentForOwnAspectOnly() {
        assertEquals(1, ItemBaubleBlanks.visDiscountPercent(3, AER));
        assertEquals(0, ItemBaubleBlanks.visDiscountPercent(3, IGNIS));
        assertEquals(0, ItemBaubleBlanks.visDiscountPercent(5, AER));
        assertEquals(0, ItemBaubleBlanks.visDiscountPercent(0, AER));
        assertEquals(0, ItemBaubleBlanks.visDiscountPercent(1, null));
    }

    // ------------------------------------------- runic charge tables (per class)

    @Test
    void ringChargeTableMatchesOriginalTernary() {
        // ItemRingRunic.java:76 — meta0 1, meta1 5, else 4.
        assertEquals(1, ItemRingRunic.chargeForMeta(0));
        assertEquals(5, ItemRingRunic.chargeForMeta(1));
        assertEquals(4, ItemRingRunic.chargeForMeta(2));
        assertEquals(4, ItemRingRunic.chargeForMeta(3));
    }

    @Test
    void amuletChargeTableMatchesOriginalTernary() {
        // ItemAmuletRunic.java:55 — meta0 8, meta1 7.
        assertEquals(8, ItemAmuletRunic.chargeForMeta(0));
        assertEquals(7, ItemAmuletRunic.chargeForMeta(1));
    }

    @Test
    void girdleChargeTableMatchesOriginalTernary() {
        // ItemGirdleRunic.java:60 — meta0 10, meta1 9.
        assertEquals(10, ItemGirdleRunic.chargeForMeta(0));
        assertEquals(9, ItemGirdleRunic.chargeForMeta(1));
    }

    // ------------------------------------------------ ItemAmuletVis.getMaxVis

    @Test
    void visAmuletCapacitiesMatchOriginal() {
        // ItemAmuletVis.java:131 — 25000 centi-vis for meta1, else 2500.
        assertEquals(2_500, ItemAmuletVis.maxVisForMeta(0));
        assertEquals(25_000, ItemAmuletVis.maxVisForMeta(1));
    }

    // --------------------------------------- ItemAmuletVis.onWornTick transfer

    @Test
    void transferCapIsFiveCentiVisCappedByRoomAndStored() {
        // ItemAmuletVis.java:82-86 — amt = min(5, room, stored).
        assertEquals(5, ItemAmuletVis.transferAmount(10, 10));
        assertEquals(3, ItemAmuletVis.transferAmount(3, 10));
        assertEquals(2, ItemAmuletVis.transferAmount(10, 2));
        assertEquals(0, ItemAmuletVis.transferAmount(0, 10));
        assertEquals(0, ItemAmuletVis.transferAmount(10, 0));
    }

    // ------------------------------------------------ ItemGirdleHover.onWornTick

    @Test
    void hoverGirdleSubtractsThirdOfABlockOnlyFromPositiveFall() {
        // ItemGirdleHover.java:56-58 — if (fallDistance > 0) fallDistance -= 0.33.
        assertEquals(0.67F, ItemGirdleHover.nextFallDistance(1.0F), 1e-6);
        // The original lets the value go negative when the fall is short.
        assertEquals(-0.13F, ItemGirdleHover.nextFallDistance(0.2F), 1e-6);
        assertEquals(0.0F, ItemGirdleHover.nextFallDistance(0.0F));
    }
}
