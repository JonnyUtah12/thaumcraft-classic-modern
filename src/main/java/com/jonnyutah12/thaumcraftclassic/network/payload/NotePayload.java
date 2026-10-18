package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C + C2S payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketNote}.
 * <p>Sensor/note-block value relay. note == NOTE_QUERY (-1) is a client request; the server answers with the tile's current value to nearby players.</p>
<p>The original used one class registered on both sides with a -1 sentinel; the same shape is kept via a bidirectional registration.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record NotePayload(BlockPos pos, int note) implements CustomPacketPayload {

    public static final Type<NotePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "note"));

    public static final StreamCodec<ByteBuf, NotePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NotePayload::pos,
            ByteBufCodecs.VAR_INT, NotePayload::note,
            NotePayload::new);
    /** Sentinel note value: the client is asking for the tile's current value. */
    public static final int NOTE_QUERY = -1;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
