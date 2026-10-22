package com.jonnyutah12.thaumcraftclassic.accessory.curios;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TC-M8-01: the Curios adapter's negative paths — the parts of the mapping
 * that hold without a live Curios runtime or a Player instance (TC4 baubles
 * are player-only, so a non-player wearer must fail validation and receive
 * no callbacks). The positive paths (equip/tick/unequip driving
 * {@link AccessoryItem} exactly once through Curios' update loop) need a
 * ticking player and are covered by AccessoryGameTest in the Curios run.
 *
 * <p>The behavior fake implements {@link AccessoryItem} directly (no
 * {@code Item} construction — the unit-test item registry is frozen); the
 * adapter drives the behavior interface, not the item class.</p>
 */
class AccessoryCurioItemTest {

    private static final class Counting implements AccessoryItem {
        final AtomicInteger equipped = new AtomicInteger();
        final AtomicInteger unequipped = new AtomicInteger();
        final AtomicInteger wornTicks = new AtomicInteger();

        @Override
        public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
            return Set.of(AccessorySlot.RING);
        }

        @Override
        public void onEquipped(final ItemStack stack, final Player player) {
            equipped.incrementAndGet();
        }

        @Override
        public void onUnequipped(final ItemStack stack, final Player player) {
            unequipped.incrementAndGet();
        }

        @Override
        public void onWornTick(final ItemStack stack, final Player player) {
            wornTicks.incrementAndGet();
        }
    }

    @Test
    void nonPlayerWearerFailsValidationAndGetsNoCallbacks() {
        final Counting behavior = new Counting();
        final AccessoryCurioItem adapter = new AccessoryCurioItem(behavior);
        final ItemStack stack = ItemStack.EMPTY;
        final SlotContext context = new SlotContext(AccessorySlot.RING.curiosSlotId(), null, 0, false, true);

        assertFalse(adapter.canEquip(context, stack), "non-player wearer accepted");
        assertFalse(adapter.canUnequip(context, stack));
        adapter.onEquip(context, stack, ItemStack.EMPTY);
        adapter.onUnequip(context, stack, ItemStack.EMPTY);
        adapter.curioTick(context, stack);
        assertEquals(0, behavior.equipped.get());
        assertEquals(0, behavior.unequipped.get());
        assertEquals(0, behavior.wornTicks.get());
    }

    @Test
    void unknownSlotIdFailsValidation() {
        final Counting behavior = new Counting();
        final AccessoryCurioItem adapter = new AccessoryCurioItem(behavior);
        final SlotContext context = new SlotContext("someothermod:satchel", null, 0, false, true);
        assertFalse(adapter.canEquip(context, ItemStack.EMPTY));
    }
}
