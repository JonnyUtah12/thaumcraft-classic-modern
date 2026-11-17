package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The click feedback of the wand/resonator tube configuration (TC-M4-07-f),
 * shared by the tube family — the original's tiles each repeated the same two
 * {@code playSound} calls, so the port puts them in one place.
 *
 * <p>Original: {@code thaumcraft:tool} at {@code 0.5F} /
 * {@code 0.9F + rand·0.2F} for a face toggle and for aiming (both from
 * {@code TileTube}, {@code TileTubeValve} and {@code TileTubeBuffer}
 * {@code .onWandRightClick}), and {@code thaumcraft:squeek} at {@code 0.6F} /
 * {@code 1.1F + world.rand·0.2F} for the buffer's choke cycle — the buffer's
 * pitch rolls on the <em>world</em> random, the tool click on the
 * <em>player's</em>, which is why the two read from different sources. The
 * original's wand click also cued the player on every branch (the
 * {@code func_71038_i} call in front of each sub-hit branch, whose MCP name is
 * not recoverable from the local mapping exports — only its call sites are);
 * 1.21.1 has no server-side call for that cue, so the port plays the swing
 * <em>animation</em> here ({@link Player#swing(InteractionHand)}), matching
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockTube}'s valve toggle
 * (SQUEEK + swing).</p>
 */
public final class WandInteractionFeedback {

    /** The face-toggle / aiming click: {@code thaumcraft:tool}, 0.5 loud, 0.9 + rand·0.2. */
    public static void toolClick(final Level level, final BlockPos pos, final Player player) {
        final float pitch = TubeTransportLogic.SUBHIT_TOOL_PITCH_BASE
                + level.getRandom().nextFloat() * 0.2F;
        if (level.isClientSide) {
            // The original ran the wand dispatch on the client and played the
            // world sound record there; the port plays it once, on the side
            // that can hear it, so the server broadcast below is the pair of
            // that local play.
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModSounds.TOOL.get(), SoundSource.BLOCKS, TubeTransportLogic.SUBHIT_TOOL_VOLUME,
                    pitch, false);
        } else {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModSounds.TOOL.get(), SoundSource.BLOCKS, TubeTransportLogic.SUBHIT_TOOL_VOLUME, pitch);
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** The buffer's choke cycle: {@code thaumcraft:squeek}, 0.6 loud, 1.1 + world rand·0.2. */
    public static void squeak(final Level level, final BlockPos pos, final Player player) {
        final float pitch = TubeTransportLogic.SUBHIT_SQUEAK_PITCH_BASE
                + level.getRandom().nextFloat() * 0.2F;
        if (level.isClientSide) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModSounds.SQUEEK.get(), SoundSource.BLOCKS, TubeTransportLogic.SUBHIT_SQUEAK_VOLUME,
                    pitch, false);
        } else {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModSounds.SQUEEK.get(), SoundSource.BLOCKS, TubeTransportLogic.SUBHIT_SQUEAK_VOLUME, pitch);
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    private WandInteractionFeedback() {
    }
}
