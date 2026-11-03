package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * A device block whose <b>appearance</b> depends on live tile data (BD-18,
 * t_065502ba).
 *
 * <h2>Why the look is a block state property</h2>
 * The 1.7.10 originals picked these faces from the tile inside their
 * connected-texture hook, which has no 1.21.1 equivalent — a blockstate JSON
 * maps state&nbsp;→&nbsp;model and cannot read a block entity. The port
 * therefore mirrors the original's <em>observable condition</em> into one
 * block state property per block, and the tile writes it. That keeps the whole
 * appearance in the shipped JSON (a model per state, sprites from
 * {@code docs/asset-mapping.csv}, D-004/D-005) and leaves no renderer to port:
 * <ul>
 *   <li>{@link BlockMetalDevice#CHARGED} — the growth/fertility lamp faces
 *     swap to their lit sprites while the lamp runs
 *     ({@code BlockMetalDevice.java:117-155}: {@code charges > 0} picks
 *     {@code lamp_grow_top}/{@code lamp_grow_side} and
 *     {@code lamp_fert_top}/{@code lamp_fert_side}, the {@code *_off} pair
 *     otherwise), and the light level follows
 *     ({@code BlockMetalDevice.java:792-815}: 15 charged, 8 uncharged);</li>
 *   <li>{@link BlockLifter#POWERED} — the levitator draws its
 *     {@code animatedglow} shell while it is pushing, i.e. unpowered
 *     ({@code BlockLifterRenderer.java:50-89}: the bottom + four side glow
 *     faces, with the full-bright {@code bb = 180} while
 *     {@code !tile.gettingPower()}).</li>
 *   <li>{@link BlockStoneDevice#BURNING} — the alchemical furnace swaps its
 *     whole side ring to {@code al_furnace_front_on} while the tile burns
 *     ({@code BlockStoneDevice.java:165-171}, icons registered at
 *     {@code :76-77}), lights 12 with the same flag ({@code :216-220}) and
 *     spawns its fire-box particles ({@code :194-212}); the furnace's tile is
 *     content-mapping row {@code thaumcraft:tilealchemyfurnace}
 *     (not_started), so nothing writes {@code burning=true} until that tile
 *     lands, and the state-side contract ships first (BD-18 slice 2b).</li>
 * </ul>
 *
 * <h2>The condition the tile reports</h2>
 * The original's growth/fertility lamps count <em>vis charges</em>
 * ({@code TileArcaneLampGrowth.charges}, {@code TileArcaneLampFertility.charges});
 * the port's lamp tiles run off essentia and have no vis charge field
 * (the vis drain is documented as deferred in {@link TileArcaneLamp}), so the
 * port's live analogue of {@code charges > 0} is {@code use > 0} — the lamp is
 * armed and burning. That mapping is a BD-18 cell, not a silent substitution.
 *
 * <p>Implementing blocks are enumerated by the coverage probe through the live
 * registry, so a block that gains a dynamic look is gated without touching the
 * test's expected set.</p>
 */
public interface DeviceDynamicState {

    /** The property whose value the tile drives to change the block's look. */
    Property<?> dynamicAppearanceProperty();

    /**
     * Publish {@code value} on the state at {@code pos} when it differs, so the
     * client re-renders and the blockstate picks the matching model.
     *
     * <p>{@link Block#UPDATE_CLIENTS} only: the neighbours do not react to a
     * pure appearance change, and writing the state of the <em>same</em> block
     * keeps the block entity (its inventory and fields) — {@code LevelChunk#setBlockState}
     * runs the remove/place hooks only when the block changes, which is the
     * vanilla pattern the lit furnace/copper bulbs use. Light changes are
     * re-checked by that same call.</p>
     */
    static <T extends Comparable<T>> void sync(final BlockEntity tile, final Property<T> property, final T value) {
        final Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        final BlockPos pos = tile.getBlockPos();
        final BlockState state = level.getBlockState(pos);
        if (!state.is(tile.getBlockState().getBlock()) || !state.hasProperty(property)
                || state.getValue(property).equals(value)) {
            return;
        }
        level.setBlock(pos, state.setValue(property, value), Block.UPDATE_CLIENTS);
    }
}
