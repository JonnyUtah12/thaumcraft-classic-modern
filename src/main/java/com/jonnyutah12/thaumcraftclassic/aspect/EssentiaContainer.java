package com.jonnyutah12.thaumcraftclassic.aspect;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Typed payload for the {@code thaumcraft:essentia_container} data component
 * (TC-M6-04a). Replaces the original's {@code "Aspects"} ItemStack NBT
 * subcompound written by {@code AspectList.writeToNBT}/{@code readFromNBT}
 * (IEssentiaContainerItem implementations in
 * ItemWispEssence/ItemCrystalEssence/ItemManaBean/ItemResource).
 *
 * <p>Keys are aspect tags, values are amounts. The map is kept sorted by tag
 * exactly like the original's backing {@code TreeMap}, so
 * {@code getAspects()[0]}-style first-aspect reads (tooltip tint, bean
 * pool reward) stay deterministic across the port.</p>
 */
public record EssentiaContainer(Map<String, Integer> amounts) {

    public static final Codec<EssentiaContainer> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 65535))
                    .xmap(EssentiaContainer::new, EssentiaContainer::amounts);

    public static final StreamCodec<ByteBuf, EssentiaContainer> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(
                            LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
                    .map(EssentiaContainer::new, EssentiaContainer::amounts);

    public EssentiaContainer {
        amounts = amounts.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(amounts));
    }

    /** One-aspect container, the original's {@code new AspectList().add(a, n)}. */
    public static EssentiaContainer of(final Aspect aspect, final int amount) {
        return new EssentiaContainer(Map.of(aspect.getTag(), amount));
    }

    public static EssentiaContainer fromAspectList(final AspectList list) {
        final Map<String, Integer> map = new TreeMap<>();
        for (final Aspect aspect : list.getAspects()) {
            map.put(aspect.getTag(), list.getAmount(aspect));
        }
        return new EssentiaContainer(map);
    }

    public AspectList toAspectList() {
        final AspectList list = new AspectList();
        for (final Map.Entry<String, Integer> entry : amounts.entrySet()) {
            final Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null) {
                list.add(aspect, entry.getValue());
            }
        }
        return list;
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    public int getAmount(final String tag) {
        return amounts.getOrDefault(tag, 0);
    }
}
