package com.jonnyutah12.thaumcraftclassic.casting;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The equal-trade swapper's queue seam (TC-M6-03-p): the original's
 * {@code ServerTickEventsFML.addSwapper(world, x, y, z, block, meta, target
 * =pickedStack, life, player, hotbarSlot)} ({@code ItemFocusTrade.java:141,
 * 189}) hands the swap to the server-tick queue, where the block is exchanged
 * and each swap pays its own vis ({@code ServerTickEventsFML.tickBlockSwap},
 * lines 210-310).
 *
 * <p>That queue is the equal-trade machinery card's content (the swapper is
 * an entity-less tick queue, not one of the seven projectile entity rows).
 * This is the seam the {@code swap} performer codes against: the machinery
 * card calls {@link #register} at init, and the performer only enqueues once
 * a bridge exists — until then the cast is refunded and the swapper is
 * simply not reachable, the card-wide rule for unlanded content.</p>
 *
 * <p>The performer applies the original's guards before enqueueing (air,
 * unbreakable, and picked-block-equals-target all skip,
 * {@code ServerTickEventsFML.java:180-190}); the bridge performs the
 * cascade a {@code lifespan > 0} entry gets and the per-swap charge.</p>
 */
public interface SwapperBridge {

    /**
     * Queues one swap: exchange the block at {@code pos} for
     * {@code picked}, cascading {@code lifespan} hops outward (the original's
     * non-architect {@code 3 + enlarge}, the architect path's {@code 0}).
     * {@code hotbarSlot} is the wand slot the queue charges.
     */
    void queue(ServerLevel level, BlockPos pos, BlockState source, ItemStack picked,
               int lifespan, Player player, int hotbarSlot);

    /** The registered bridge, if the equal-trade machinery has landed. */
    @Nullable
    static SwapperBridge get() {
        return Holder.BRIDGE;
    }

    /** Registers the machinery's bridge (idempotent re-registration allowed). */
    static void register(final SwapperBridge bridge) {
        Holder.BRIDGE = bridge;
    }

    /** The mutable holder; split out so the interface keeps no instance state. */
    final class Holder {
        @Nullable
        static volatile SwapperBridge BRIDGE;

        private Holder() {
        }
    }
}
