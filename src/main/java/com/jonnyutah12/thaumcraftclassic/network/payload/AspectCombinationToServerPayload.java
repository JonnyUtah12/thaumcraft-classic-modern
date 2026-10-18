package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer}.
 * <p>The client combined two aspects in the research table GUI.</p>
<p>The sending player is taken from the payload context instead of id+dimension.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record AspectCombinationToServerPayload(BlockPos tablePos, String aspect1, String aspect2, boolean bonus1, boolean bonus2) implements CustomPacketPayload {

    public static final Type<AspectCombinationToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "aspect_combination_to_server"));

    public static final StreamCodec<ByteBuf, AspectCombinationToServerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AspectCombinationToServerPayload::tablePos,
            ByteBufCodecs.STRING_UTF8, AspectCombinationToServerPayload::aspect1,
            ByteBufCodecs.STRING_UTF8, AspectCombinationToServerPayload::aspect2,
            ByteBufCodecs.BOOL, AspectCombinationToServerPayload::bonus1,
            ByteBufCodecs.BOOL, AspectCombinationToServerPayload::bonus2,
            AspectCombinationToServerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
