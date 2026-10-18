package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketItemKeyToServer}.
 * <p>The client pressed an item-bound key (golem bell reset, wand misc toggle, elemental shovel orientation).</p>
<p>playerid+dimension dropped: the sender identity is the payload context player.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record ItemKeyToServerPayload(int key) implements CustomPacketPayload {

    public static final Type<ItemKeyToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "item_key_to_server"));

    public static final StreamCodec<ByteBuf, ItemKeyToServerPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT
                    .map(ItemKeyToServerPayload::new, ItemKeyToServerPayload::key);
    /** Key kinds, matching the original PacketItemKeyToServer key byte. */
    public static final int KEY_GOLEM_BELL_RESET = 0;
    public static final int KEY_WAND_MISC_TOGGLE = 1;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
