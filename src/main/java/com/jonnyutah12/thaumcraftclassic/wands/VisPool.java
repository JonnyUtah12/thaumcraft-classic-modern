package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Vis charge carried by a stack (TC-M4-02): the payload of the
 * {@code thaumcraft:vis_pool} data component (design record:
 * docs/architecture/vis.md).
 *
 * <p>Replaces the original's per-aspect {@code NBTTagInt} keys written directly
 * on the wand/amulet stack tag by {@code ItemWandCasting.storeVis}/{@code getVis}
 * ({@code common/items/wands/ItemWandCasting.java:238-249}). Values are
 * centi-vis — 1/100 of one vis unit, the original's internal integer unit
 * (docs/balance-values.md convention, ItemWandCasting.java:314). Keys are
 * aspect tags; only primal aspects ever hold charge (the original's
 * {@code addVis} refuses non-primals, ItemWandCasting.java:353).</p>
 *
 * <p>Capacity is deliberately NOT part of this payload: the original derived it
 * from the rod component on every read ({@code getMaxVis},
 * ItemWandCasting.java:82-84); {@link WandVis#maxVis} ports that math and the
 * rod/cap components arrive with TC-M6-01. Entries at zero are dropped on
 * construction — the original stored a literal 0 after full consumption, but
 * every read path ({@code getVis}) treats absent and 0 identically.</p>
 */
public record VisPool(Map<String, Integer> centiVis) {

    public static final VisPool EMPTY = new VisPool(Map.of());

    public static final Codec<VisPool> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .xmap(VisPool::new, VisPool::centiVis);

    public static final StreamCodec<ByteBuf, VisPool> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public VisPool {
        final Map<String, Integer> normalized = new TreeMap<>();
        centiVis.forEach((tag, amount) -> {
            if (amount != null && amount != 0) {
                normalized.put(tag, amount);
            }
        });
        centiVis = normalized.isEmpty() ? Map.of() : Collections.unmodifiableMap(normalized);
    }

    /** Current charge of an aspect in centi-vis (original {@code getVis}: absent reads 0). */
    public int getVis(final Aspect aspect) {
        return centiVis.getOrDefault(aspect.getTag(), 0);
    }

    public boolean isEmpty() {
        return centiVis.isEmpty();
    }
}
