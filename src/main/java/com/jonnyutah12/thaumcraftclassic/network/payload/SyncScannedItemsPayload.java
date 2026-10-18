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
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncScannedItems}.
 * <p>Full set of object scan keys (Thaumaturgical Registry) the player has completed.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncScannedItemsPayload(List<String> entries) implements CustomPacketPayload {

    public static final Type<SyncScannedItemsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_scanned_items"));

    public static final StreamCodec<ByteBuf, SyncScannedItemsPayload> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                    .map(SyncScannedItemsPayload::new, SyncScannedItemsPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
