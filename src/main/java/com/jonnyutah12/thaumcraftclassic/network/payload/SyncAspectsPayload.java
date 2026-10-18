package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncAspects}.
 * <p>Full set of aspects the player has discovered, with discovery amounts.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncAspectsPayload(List<AspectAmount> aspects) implements CustomPacketPayload {

    public static final Type<SyncAspectsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_aspects"));

    public static final StreamCodec<ByteBuf, SyncAspectsPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, AspectAmount, List<AspectAmount>>collection(ArrayList::new, AspectAmount.STREAM_CODEC)
                    .map(SyncAspectsPayload::new, SyncAspectsPayload::aspects);


    /** One discovered-aspect entry: aspect tag plus discovery amount. */
    public record AspectAmount(String tag, int amount) {

        public static final StreamCodec<io.netty.buffer.ByteBuf, AspectAmount> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, AspectAmount::tag,
                        ByteBufCodecs.VAR_INT, AspectAmount::amount,
                        AspectAmount::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
