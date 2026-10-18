package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketFocusChangeToServer}.
 * <p>The client changed the wand focus in the wand GUI.</p>
<p>playerid+dimension dropped: the sender identity is the payload context player.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FocusChangeToServerPayload(String focus) implements CustomPacketPayload {

    public static final Type<FocusChangeToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "focus_change_to_server"));

    public static final StreamCodec<ByteBuf, FocusChangeToServerPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8
                    .map(FocusChangeToServerPayload::new, FocusChangeToServerPayload::focus);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
