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
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncResearch}.
 * <p>Full set of research keys the player has completed.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncResearchPayload(List<String> entries) implements CustomPacketPayload {

    public static final Type<SyncResearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_research"));

    public static final StreamCodec<ByteBuf, SyncResearchPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                    .map(SyncResearchPayload::new, SyncResearchPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
