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
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncScannedPhenomena}.
 * <p>Full set of phenomenon scan keys the player has completed.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncScannedPhenomenaPayload(List<String> entries) implements CustomPacketPayload {

    public static final Type<SyncScannedPhenomenaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_scanned_phenomena"));

    public static final StreamCodec<ByteBuf, SyncScannedPhenomenaPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                    .map(SyncScannedPhenomenaPayload::new, SyncScannedPhenomenaPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
