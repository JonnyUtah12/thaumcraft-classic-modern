package com.jonnyutah12.thaumcraftclassic.casting;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

/**
 * The block-entity face of a warded block (TC-M6-03-p): the warding focus's
 * {@code TileWarded} fields ({@code ItemFocusWarding.java:98-105}:
 * {@code tw.block}/{@code tw.blockMd}/{@code tw.light}/{@code tw.owner} —
 * the captured block, its metadata, its light level and the owner's name hash)
 * and the read-back the un-ward performs
 * ({@code ItemFocusWarding.java:135-145}: {@code tw2.block} +
 * {@code tw2.blockMd}).
 *
 * <p>{@code blockWarded}/{@code TileWarded} are the warding machinery card's
 * content; this is the seam the ward performer codes against. The block is
 * found by registry id ({@code thaumcraft:blockwarded}) — the original's
 * {@code GameRegistry.findBlock("Thaumcraft", "blockWarded")} — and the
 * performer only treats it as warded when its block entity implements this
 * interface.</p>
 *
 * <p>The original keyed ownership on {@code player.getCommandSenderName()
 * .hashCode()}; the port hashes {@code player.getGameProfile().getName()} —
 * the same stable per-player identity. Owner checks happen in the performer,
 * not here.</p>
 */
public interface WardedSurface {

    /** Records the block this ward replaced, as the original stored it. */
    void setCaptured(BlockState captured, int light, int ownerHash);

    /** The captured block ({@code tw.block} + {@code tw.blockMd}), if any. */
    @Nullable
    BlockState captured();

    /** The owner hash recorded at ward time ({@code tw.owner}). */
    int ownerHash();
}
