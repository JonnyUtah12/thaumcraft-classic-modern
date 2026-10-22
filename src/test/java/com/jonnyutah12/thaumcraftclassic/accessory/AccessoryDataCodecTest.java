package com.jonnyutah12.thaumcraftclassic.accessory;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M8-01: the fallback attachment payload codec. Round-trips worn stacks
 * through JSON (the same Codec path the attachment serializer and the S2C
 * sync use), tolerates missing slots (lenient, every field optional), and
 * normalizes wrong-size payloads instead of throwing — a corrupt or
 * future-version payload must never brick a player save.
 */
class AccessoryDataCodecTest {

    @Test
    void emptyRoundTrips() {
        assertRoundTrip(AccessoryData.empty());
        assertTrue(AccessoryData.empty().isEmpty());
    }

    @Test
    void wornStacksRoundTrip() {
        final AccessoryData data = AccessoryData.empty()
                .with(0, new ItemStack(net.minecraft.world.item.Items.CLOCK))
                .with(2, new ItemStack(net.minecraft.world.item.Items.ENDER_EYE))
                .with(4, new ItemStack(net.minecraft.world.item.Items.COMPASS));
        final AccessoryData back = assertRoundTrip(data);
        assertEquals(3, back.wornStacks().size());
        assertTrue(back.get(0).is(net.minecraft.world.item.Items.CLOCK));
        assertTrue(back.get(4).is(net.minecraft.world.item.Items.COMPASS));
    }

    @Test
    void partialPayloadDecodesWithMissingSlotsEmpty() {
        final AccessoryData source = AccessoryData.empty()
                .with(3, new ItemStack(net.minecraft.world.item.Items.BELL));
        final var encoded = AccessoryData.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .resultOrPartial(err -> { throw new IllegalStateException(err); })
                .orElseThrow().getAsJsonObject();

        // Keep only the "belt" field: every other field is absent, which the
        // lenient codec must decode as empty rather than fail.
        final JsonObject partial = new JsonObject();
        partial.add("belt", encoded.get("belt"));
        final AccessoryData decoded = AccessoryData.CODEC.decode(JsonOps.INSTANCE, partial)
                .resultOrPartial(err -> { throw new IllegalStateException(err); })
                .orElseThrow().getFirst();
        assertEquals(1, decoded.wornStacks().size());
        assertTrue(decoded.get(3).is(net.minecraft.world.item.Items.BELL));
    }

    @Test
    void wrongSizePayloadIsNormalizedNotThrown() {
        final AccessoryData shortOne = new AccessoryData(List.of(ItemStack.EMPTY));
        assertEquals(AccessoryData.SIZE, shortOne.slots().size());
        final AccessoryData longOne = new AccessoryData(java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> ItemStack.EMPTY).toList());
        assertEquals(AccessoryData.SIZE, longOne.slots().size());
    }

    private static AccessoryData assertRoundTrip(final AccessoryData data) {
        final var encoded = AccessoryData.CODEC.encodeStart(JsonOps.INSTANCE, data)
                .resultOrPartial(err -> { throw new IllegalStateException(err); })
                .orElseThrow();
        return AccessoryData.CODEC.decode(JsonOps.INSTANCE, encoded)
                .resultOrPartial(err -> { throw new IllegalStateException(err); })
                .orElseThrow().getFirst();
    }
}
