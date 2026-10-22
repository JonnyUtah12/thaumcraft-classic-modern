package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * The fallback provider's player attachment payload: the fixed 5-slot layout
 * from {@link AccessorySlot} (RING 0-1, AMULET 2, BELT 3, CHARM 4).
 *
 * <p>Immutable; mutation helpers return updated copies (same pattern as
 * {@code PlayerWarpData}). The codec is lenient: every slot is optional and
 * unknown fields are ignored, so the payload survives partial writes and
 * future slot additions without corrupting player saves.</p>
 */
public record AccessoryData(List<ItemStack> slots) {

    public static final int SIZE = AccessorySlot.fallbackSlotCount();

    public static final Codec<AccessoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            slotCodec("ring_1").forGetter(d -> d.slots().get(0)),
            slotCodec("ring_2").forGetter(d -> d.slots().get(1)),
            slotCodec("amulet").forGetter(d -> d.slots().get(2)),
            slotCodec("belt").forGetter(d -> d.slots().get(3)),
            slotCodec("charm").forGetter(d -> d.slots().get(4))
    ).apply(instance, (r1, r2, amulet, belt, charm) ->
            new AccessoryData(List.of(r1, r2, amulet, belt, charm))));

    private static com.mojang.serialization.MapCodec<ItemStack> slotCodec(final String name) {
        return ItemStack.OPTIONAL_CODEC.optionalFieldOf(name, ItemStack.EMPTY);
    }

    public static AccessoryData empty() {
        return new AccessoryData(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY));
    }

    public AccessoryData {
        if (slots.size() != SIZE) {
            final List<ItemStack> normalized = new ArrayList<>(slots);
            while (normalized.size() < SIZE) {
                normalized.add(ItemStack.EMPTY);
            }
            slots = List.copyOf(normalized.subList(0, SIZE));
        } else {
            slots = List.copyOf(slots);
        }
    }

    /** Stack at the payload index; EMPTY when out of range or empty. */
    public ItemStack get(final int index) {
        if (index < 0 || index >= SIZE) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slots.get(index);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** Updated copy with the payload index set (EMPTY clears). */
    public AccessoryData with(final int index, final ItemStack stack) {
        if (index < 0 || index >= SIZE) {
            return this;
        }
        final List<ItemStack> next = new ArrayList<>(slots);
        next.set(index, stack == null ? ItemStack.EMPTY : stack);
        return new AccessoryData(next);
    }

    public boolean isEmpty() {
        for (final ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** All non-empty worn stacks in payload order. */
    public List<ItemStack> wornStacks() {
        final List<ItemStack> worn = new ArrayList<>();
        for (final ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                worn.add(stack);
            }
        }
        return worn;
    }
}
