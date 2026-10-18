package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketMiscEvent}.
 * <p>One-shot client effect trigger (warp vignette, long/short mist).</p>
<p>short kind widened to int (varint).</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record MiscEventPayload(int eventKind) implements CustomPacketPayload {

    public static final Type<MiscEventPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "misc_event"));

    public static final StreamCodec<ByteBuf, MiscEventPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT
                    .map(MiscEventPayload::new, MiscEventPayload::eventKind);
    /** Event kinds, matching the original PacketMiscEvent constants. */
    public static final int EVENT_WARP = 0;
    public static final int EVENT_MIST = 1;
    public static final int EVENT_MIST_SHORT = 2;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
