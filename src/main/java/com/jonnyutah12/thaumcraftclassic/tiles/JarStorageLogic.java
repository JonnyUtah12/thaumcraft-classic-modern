package com.jonnyutah12.thaumcraftclassic.tiles;

/**
 * The essentia jar's aspect-matching and stacking rules (TC-M4-06-s), as pure
 * functions over aspect <em>tags</em> and ints.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileJarFillable} (and its
 * {@code TileJarFillableVoid} override) — {@code addToContainer},
 * {@code takeFromContainer}, {@code doesContainerAccept}, the suction numbers
 * a tube reads, and the redstone level the block emits. The rules live here,
 * detached from a {@code Level}, so JarStorageLogicTest pins them value for
 * value without a world (the {@code InfusionMatrixLogic}/{@code CrucibleStateMachine}
 * pattern). {@link TileJarFillable} is the thin world-facing shell that applies
 * the returned results and saves them.</p>
 *
 * <h2>The rules, verbatim from 4.2.3.5</h2>
 * <ul>
 *   <li>a jar holds <b>one</b> aspect only: an add is writable when
 *       {@code (amount < maxAmount && incoming == held) || amount == 0} —
 *       the original's precedence is exactly that (the empty jar is the only
 *       way an aspect ever changes), and the void jar replaces the
 *       room-required term with "no gate at all" (it clips the overflow
 *       instead of refusing it);</li>
 *   <li>the label filter is an <em>acceptance</em> gate, not a content gate:
 *       {@code doesContainerAccept} is {@code tag.equals(filter)} when a
 *       filter is set and true otherwise, and an unfiltered jar never blocks a
 *       mismatched aspect at the accept step — the single-aspect rule in
 *       {@code addToContainer} is what refuses it;</li>
 *   <li>taking requires the exact aspect and enough of it; the aspect is
 *       cleared only when the amount reaches 0;</li>
 *   <li>suction: a plain jar advertises 32 (64 when labelled) while it has
 *       room and 0 when full, with the same figure as its minimum suction; a
 *       void jar advertises 32 (48 when labelled) and never goes to 0
 *       ({@code getSuctionAmount} has no full check there);</li>
 *   <li>the block's signal is {@code floor(amount / maxAmount * 14) + 1} for
 *       any non-empty jar, so an empty jar gives 0 and a full one 15.</li>
 * </ul>
 */
public final class JarStorageLogic {

    /** Original {@code maxAmount} of TileJarFillable (both jar kinds). */
    public static final int MAX_AMOUNT = 64;

    /** Original {@code ++count % 5 == 0}: the jar pulls one unit every 5 ticks. */
    public static final int FILL_CADENCE_TICKS = 5;

    /** One unit of essentia: a jar pulls 1 per cadence tick from the tube above. */
    public static final int FILL_PER_CYCLE = 1;

    /** A phial moves 8 units per fill/drain (original ItemEssence.onItemUseFirst). */
    public static final int PHIAL_TRANSFER = 8;

    /** The outcome of an add, so the shell can apply it and persist it. */
    public record Add(int leftover, String aspectTag, int amount) {
    }

    /** The outcome of a take, so the shell can apply it and persist it. */
    public record Take(boolean taken, String aspectTag, int amount) {
    }

    /**
     * Plain jar: {@code if (amount < max && incoming == held || amount == 0)}
     * — the original's exact precedence — then {@code added = min(requested,
     * max - amount)}.
     */
    public static Add addToContainer(final String heldTag, final int amount,
                                     final int maxAmount, final String incomingTag,
                                     final int requested) {
        return addToContainer(heldTag, amount, maxAmount, incomingTag, requested, false);
    }

    /**
     * @param voidJar the {@code TileJarFillableVoid} override: same-aspect-or-empty
     *                with no room check, the overflow clipped to {@code maxAmount}.
     */
    public static Add addToContainer(final String heldTag, final int amount,
                                     final int maxAmount, final String incomingTag,
                                     final int requested, final boolean voidJar) {
        if (requested == 0) {
            return new Add(0, heldTag, amount);
        }
        // A null aspect is not storable; the original's reference comparison
        // let `tt == null && aspect == null` through and counted it, which is
        // unreachable from any ported caller.
        if (incomingTag == null) {
            return new Add(requested, heldTag, amount);
        }
        if (voidJar) {
            if (incomingTag.equals(heldTag) || amount == 0) {
                int next = amount + requested;
                if (next > maxAmount) {
                    next = maxAmount;
                }
                return new Add(0, incomingTag, next);
            }
            return new Add(requested, heldTag, amount);
        }
        final boolean writable = (amount < maxAmount && incomingTag.equals(heldTag)) || amount == 0;
        if (!writable) {
            return new Add(requested, heldTag, amount);
        }
        final int added = Math.min(requested, maxAmount - amount);
        return new Add(requested - added, incomingTag, amount + added);
    }

    /** Plain jar: exact aspect and enough of it, clearing the aspect at 0. */
    public static Take takeFromContainer(final String heldTag, final int amount,
                                         final String takenTag, final int requested) {
        if (requested > 0 && amount >= requested && takenTag != null && takenTag.equals(heldTag)) {
            final int next = amount - requested;
            return next <= 0 ? new Take(true, null, 0) : new Take(true, heldTag, next);
        }
        return new Take(false, heldTag, amount);
    }

    /**
     * The label filter gate (original {@code doesContainerAccept}): a filter
     * matches exactly, no filter accepts anything.
     */
    public static boolean doesContainerAccept(final String filterTag, final String proposedTag) {
        return filterTag == null || filterTag.equals(proposedTag);
    }

    /** Original {@code doesContainerContainAmount}. */
    public static boolean doesContainerContainAmount(final String heldTag, final int amount,
                                                     final String queriedTag, final int requested) {
        return amount >= requested && queriedTag != null && queriedTag.equals(heldTag);
    }

    /** Original {@code getSuctionAmount} for a plain jar. */
    public static int suctionAmount(final boolean hasFilter, final int amount, final int maxAmount) {
        return amount < maxAmount ? (hasFilter ? 64 : 32) : 0;
    }

    /** Original {@code getSuctionAmount} for a void jar (no full check). */
    public static int voidSuctionAmount(final boolean hasFilter, final int amount, final int maxAmount) {
        return hasFilter && amount < maxAmount ? 48 : 32;
    }

    /** Original {@code getMinimumSuction} for a plain jar. */
    public static int minimumSuction(final boolean hasFilter) {
        return hasFilter ? 64 : 32;
    }

    /** Original {@code getMinimumSuction} for a void jar. */
    public static int voidMinimumSuction(final boolean hasFilter) {
        return hasFilter ? 48 : 32;
    }

    /**
     * Original {@code isProvidingWeakPower}: {@code floor(r * 14) + (amount > 0 ? 1 : 0)}
     * with {@code r = amount / maxAmount}. Carried as the 1.21.1 comparator
     * read (see {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockJar}).
     */
    public static int signalStrength(final int amount, final int maxAmount) {
        if (maxAmount <= 0) {
            return 0;
        }
        final float ratio = (float) amount / (float) maxAmount;
        return (int) Math.floor(ratio * 14.0F) + (amount > 0 ? 1 : 0);
    }

    /** True on the tick the jar pulls from the transport above it. */
    public static boolean fillTick(final int tickCounter) {
        return tickCounter % FILL_CADENCE_TICKS == 0;
    }

    private JarStorageLogic() {
    }
}
