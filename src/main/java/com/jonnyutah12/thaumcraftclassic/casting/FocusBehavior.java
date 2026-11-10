package com.jonnyutah12.thaumcraftclassic.casting;

/**
 * One focus's spell body (TC-M6-03-s) behind the TC-M6-02 casting seam.
 *
 * <p>The original spelled each focus as an override of
 * {@code ItemFocusBasic.onFocusRightClick(ItemStack wandstack, World world,
 * EntityPlayer player, MovingObjectPosition mop)} (see
 * {@code common/items/wands/foci/ItemFocusFire.java:82-102}): it took the vis
 * payment as a precondition and then spawned the effect. The port inverts the
 * ownership — {@link WandCasting} resolves the focus, checks and pays the vis
 * through the TC-M4-02 chain, <em>then</em> calls the behaviour — so every
 * focus shares one payment path and a behaviour cannot forget to charge or
 * double-charge.</p>
 *
 * <p>The contract is deliberately two-valued: a behaviour that returns
 * {@code false} did nothing, and {@link WandCasting} refunds the payment it
 * made before the call. That keeps the original's "no effect, no cost" outcome
 * reachable without a behaviour having to reason about vis at all.</p>
 */
@FunctionalInterface
public interface FocusBehavior {

    /**
     * Perform the focus's effect. Vis for {@link CastContext#cost()} has already
     * been deducted from the wand.
     *
     * @return true when the cast had an effect (the payment stands); false when
     *         nothing happened (the payment is refunded)
     */
    boolean onCast(CastContext context);
}
