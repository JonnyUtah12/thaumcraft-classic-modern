package com.jonnyutah12.thaumcraftclassic.casting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The excavation focus's in-flight dig (TC-M6-03-p): the original kept
 * {@code PlayerProperties} state — the excavate block {@code x}/{@code y}/
 * {@code z} and the accumulated {@code breakProgress} — on the player while
 * the wand was channelled ({@code ItemFocusExcavation.java:170-188}: read the
 * stored coordinates, zero the progress when the pointed block changes,
 * accumulate against the block's hardness).
 *
 * <p>The port keeps the same meaning on the wand stack, the item that owns
 * the channel (component {@code thaumcraft:wand_dig_state}): the coordinates
 * are the block the progress accumulated against, and a cast at a different
 * block starts a fresh dig.</p>
 *
 * @param pos      the block the progress accumulated against
 * @param progress the accumulated break progress, compared to the block's
 *                 hardness ({@code bc >= bi.getBlock().getHardness(...)})
 */
public record DigState(BlockPos pos, float progress) {

    public static final Codec<DigState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(DigState::pos),
            Codec.FLOAT.fieldOf("progress").forGetter(DigState::progress))
            .apply(instance, DigState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DigState> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DigState::pos,
                    ByteBufCodecs.FLOAT, DigState::progress,
                    DigState::new);

    /** The same block, as the original compared {@code x}/{@code y}/{@code z}. */
    public boolean isAt(final BlockPos other) {
        return pos.equals(other);
    }
}
