package com.jonnyutah12.thaumcraftclassic.casting;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocusPouch;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The wand casting entry point (TC-M6-02): focus resolution, the TC-M4-02 cost
 * chain, vis payment and the delegate call into the per-focus seam.
 *
 * <p>The original spread this over three places:
 * {@code ItemWandCasting.func_77659_a} resolved the moving-object hit and
 * delegated to the focus ({@code ItemWandCasting.java:526-560}),
 * {@code ItemFocusBasic.onFocusRightClick} was the focus's own body, and each
 * body called {@code wand.consumeAllVis(..., !world.isRemote, false)} itself
 * ({@code ItemFocusFire.java:85,117}). The port centralises the middle two
 * steps here so every focus shares one payment path and the per-focus body
 * cannot get the charging wrong (see {@link FocusBehavior}).</p>
 *
 * <h2>Order of operations</h2>
 * <ol>
 *   <li>client side → nothing happens (the original gated every effect and
 *       every write on {@code !world.isRemote}); the item-use path passes
 *       through</li>
 *   <li>no focus → {@link Status#NO_FOCUS}, no vis touched</li>
 *   <li>no registered behaviour → {@link Status#UNSUPPORTED}, no vis touched
 *       (an unported focus must not burn charge — per-focus behaviour is
 *       TC-M6-03-s)</li>
 *   <li>price the focus, then dry-run the payment →
 *       {@link Status#UNPAID} when the wand cannot cover it, no vis touched
 *       (the original's check-then-pay loop, {@code ItemWandCasting.java:324-350})</li>
 *   <li>pay, then call the behaviour; a behaviour that reports no effect has
 *       its payment refunded → {@link Status#NO_EFFECT}</li>
 * </ol>
 *
 * <h2>Focus selection</h2>
 * The original stored the selected focus <em>on the wand</em> and only ever
 * changed it from the wand GUI. 1.21.1's wand GUI is TC-M7-04, and this slice
 * has no GUI, so {@link #selectedFocus} falls back after the wand's own focus:
 * the offhand, then the first focus in a focus pouch the player carries. Those
 * two fallbacks are a documented port extension
 * (docs/content-mapping.csv, the {@code thaumcraft:focuspouch} row's
 * {@code known_differences}) that keeps the pouch's contents reachable until
 * the GUI lands; the wand's own component stays authoritative.
 */
public final class WandCasting {

    /** What happened to a cast attempt. */
    public enum Status {
        /** Client side — the original's {@code world.isRemote} gate. */
        CLIENT,
        /** The wand carries no focus (and no fallback focus was found). */
        NO_FOCUS,
        /** The focus has no registered spell body yet. */
        UNSUPPORTED,
        /**
         * The focus's combination runs per tick (the original's channel, TC-M6-03-s):
         * the right-click started the use and nothing has been paid yet — each tick
         * pays for itself through {@link #channelTick}.
         */
        CHANNELED,
        /** The wand cannot cover the focus's price. */
        UNPAID,
        /** Vis was paid and the behaviour reported an effect. */
        CAST,
        /** Vis was paid, the behaviour did nothing, and the payment was refunded. */
        NO_EFFECT
    }

    /**
     * The outcome of a cast attempt.
     *
     * @param status the outcome
     * @param focus  the resolved focus, or null
     * @param cost   the price after the TC-M4-02 chain, or an empty list
     * @param paid   what was actually deducted (empty when nothing was paid)
     */
    public record CastResult(Status status, Focus focus, AspectList cost, AspectList paid) {

        /** Whether the cast had an effect (vis stands paid). */
        public boolean cast() {
            return status == Status.CAST;
        }

        /** Whether the attempt left the wand untouched. */
        public boolean untouched() {
            return paid.size() == 0;
        }

        static CastResult of(final Status status) {
            return new CastResult(status, null, new AspectList(), new AspectList());
        }
    }

    // --------------------------------------------------------------- selection

    /**
     * The focus the wand would cast: the wand's own focus component first (the
     * original's {@code getFocusItem}), then — as a documented port extension —
     * a focus in the caster's offhand, then the first focus in a carried focus
     * pouch.
     */
    public static ItemStack selectedFocus(final ItemStack wandStack, final Player player) {
        final ItemStack onWand = ItemWandCasting.focusItem(wandStack);
        if (ItemFocus.focusOf(onWand) != null) {
            return onWand;
        }
        if (player == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack offhand = player.getOffhandItem();
        if (ItemFocus.focusOf(offhand) != null) {
            return offhand;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemStack carried = player.getInventory().getItem(slot);
            if (carried.getItem() instanceof ItemFocusPouch) {
                final ItemStack stored = ItemFocusPouch.firstFocus(carried);
                if (!stored.isEmpty()) {
                    return stored;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------- cast

    /**
     * Attempts one cast of {@code wandStack}'s selected focus.
     *
     * <p>Nothing is mutated on the client, without a focus, without a
     * registered behaviour, or when the wand cannot pay — see the class
     * javadoc for the ordering and {@link Status} for each outcome. A focus
     * whose resolved <em>combination</em> is a per-tick one reports
     * {@link Status#CHANNELED} instead: the caller starts the channel and
     * {@link #channelTick} pays and fires each tick.</p>
     */
    public static CastResult cast(final ItemWandCasting wand, final ItemStack wandStack,
                                  final Player player, final Level level, final InteractionHand hand) {
        if (level.isClientSide()) {
            return CastResult.of(Status.CLIENT);
        }
        final ItemStack focusStack = selectedFocus(wandStack, player);
        final Focus focus = ItemFocus.focusOf(focusStack);
        if (focus == null) {
            return CastResult.of(Status.NO_FOCUS);
        }
        final FocusBehavior behavior = FocusBehaviors.get(focus.tag());
        if (behavior == null) {
            return CastResult.of(Status.UNSUPPORTED);
        }
        if (FocusCombinations.channeled(focus.tag(), ItemFocus.upgrades(focusStack))) {
            return CastResult.of(Status.CHANNELED);
        }
        return fire(wand, wandStack, player, level, hand, focusStack, focus, behavior);
    }

    /**
     * One tick of a per-tick focus's channel — the original's
     * {@code onUsingFocusTick} ({@code ItemFocusFire.java:104-137},
     * {@code ItemFocusShock.java:136-239},
     * {@code ItemFocusExcavation.java:109-227}).
     *
     * <p>The original's tick body checked affordability (a dry run), stopped the
     * use when the wand could not pay ({@code p.func_71034_by()}), then paid and
     * fired. The port does the same through {@link #fire}, with its own rule on
     * top: a tick that produced no effect is refunded, so a channel never bills
     * for a focus whose content has not landed.</p>
     *
     * @return {@link Status#CAST} for a tick that fired (the channel continues),
     *         {@link Status#UNPAID} when the wand cannot pay (the caller stops the
     *         channel), {@link Status#NO_EFFECT} when nothing happened (also stops)
     */
    public static CastResult channelTick(final ItemWandCasting wand, final ItemStack wandStack,
                                         final Player player, final Level level, final InteractionHand hand) {
        if (level.isClientSide()) {
            return CastResult.of(Status.CLIENT);
        }
        final ItemStack focusStack = selectedFocus(wandStack, player);
        final Focus focus = ItemFocus.focusOf(focusStack);
        if (focus == null) {
            return CastResult.of(Status.NO_FOCUS);
        }
        final FocusBehavior behavior = FocusBehaviors.get(focus.tag());
        if (behavior == null) {
            return CastResult.of(Status.UNSUPPORTED);
        }
        if (!FocusCombinations.channeled(focus.tag(), ItemFocus.upgrades(focusStack))) {
            return CastResult.of(Status.NO_EFFECT);
        }
        return fire(wand, wandStack, player, level, hand, focusStack, focus, behavior);
    }

    /** Price, pay, fire, refund on a no-op — the one path both entries share. */
    private static CastResult fire(final ItemWandCasting wand, final ItemStack wandStack, final Player player,
                                   final Level level, final InteractionHand hand, final ItemStack focusStack,
                                   final Focus focus, final FocusBehavior behavior) {
        final AspectList baseCost = ((ItemFocus) focusStack.getItem()).getVisCost(focusStack);
        final AspectList cost = wand.effectiveCost(wandStack, player, baseCost, false);
        if (!ITEM_DRY_RUN.canPay(wand, wandStack, player, baseCost)) {
            return new CastResult(Status.UNPAID, focus, cost, new AspectList());
        }
        if (!wand.consumeAllVis(wandStack, player, baseCost, true, false)) {
            // Unreachable while the dry run above holds, but never spend on a disagreement.
            return new CastResult(Status.UNPAID, focus, cost, new AspectList());
        }

        final CastContext context = new CastContext(player, level, hand, wandStack, wand, focus,
                focusStack, baseCost, cost);
        if (!behavior.onCast(context)) {
            refund(wand, wandStack, cost);
            return new CastResult(Status.NO_EFFECT, focus, cost, new AspectList());
        }
        return new CastResult(Status.CAST, focus, cost, cost.copy());
    }

    /** The same affordability test the payment uses, without touching the wand. */
    private interface DryRun {
        boolean canPay(ItemWandCasting wand, ItemStack wandStack, Player player, AspectList baseCost);
    }

    private static final DryRun ITEM_DRY_RUN =
            (wand, wandStack, player, baseCost) -> wand.consumeAllVis(wandStack, player, baseCost, false, false);

    /**
     * Puts an already-deducted cost back, exactly — {@code storeVis} writes the
     * pool absolutely (the original's semantics), so this restores the precise
     * pre-cast charge without re-running the modifier chain. Public for the
     * performers (TC-M6-03-p): a handler that charged beyond the seam's base
     * (the hole's distance tax, an excavation progress tick) refunds the
     * excess through this same primitive.
     */
    public static void refund(final ItemWandCasting wand, final ItemStack wandStack, final AspectList cost) {
        for (final Aspect aspect : cost.getAspects()) {
            final int restored = wand.getVis(wandStack, aspect) + cost.getAmount(aspect);
            wand.storeVis(wandStack, aspect, restored);
        }
    }

    private WandCasting() {
    }
}
