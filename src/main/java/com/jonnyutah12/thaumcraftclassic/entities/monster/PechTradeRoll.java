package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The pech dice roll (TC-M7-04-m3-b-followup): the port of
 * {@code ContainerPech.generateContents()} —
 * {@code common/entities/ContainerPech.java:83-159} — split into the part that
 * is pure arithmetic over a supplied {@link RandomSource} (this class) and the
 * part that touches the window, the pech and the registries
 * ({@code menus/PechMenu}).
 *
 * <p>The original, line for line: the value comes from
 * {@code pech.getValue(offer)} ({@code :91}); the <b>scam roll</b>
 * {@code rand.nextInt(100) <= value / 2} untames the pech and plays
 * {@code thaumcraft:pech_trade} at 0.4F/1.0F ({@code :92-96}); the
 * <b>jitter</b> adds {@code nextInt(3)} when {@code nextInt(5) == 0},
 * otherwise subtracts {@code nextInt(3)} on a {@code nextBoolean()}
 * ({@code :98-102}); then the <b>payout loop</b> ({@code :106-157}) draws
 * {@code am = min(5, max((value + 1) / 2, rand.nextInt(value) + 1))}, subtracts
 * it from the value, and pays from one of three sources: the pech's loot pack
 * ({@code :110-124}, gated on {@code am == 1}, a coin flip and
 * {@code hasStuffInPack()}), a {@code ChestGenHooks} "dungeonChest" draw
 * ({@code :138-149}, gated on {@code am >= 4} and a coin flip), or the
 * {@code EntityPech.tradeInventory} row whose first element equals {@code am}
 * ({@code :150-155}). The offer loses exactly one item at {@code :158}.</p>
 *
 * <h2>Draw-order fidelity</h2>
 * Every random draw below happens in the original's order and only where the
 * original drew, because the short-circuit structure of its {@code &&} chains
 * is observable: {@code am == 1 && rand.nextBoolean() && hasStuffInPack()}
 * ({@code :110}) flips the coin <b>even when the pack is empty</b>, and
 * {@code am >= 4 && rand.nextBoolean()} ({@code :138}) flips it only at
 * {@code am >= 4}. A seeded run therefore has to reproduce the draws, not only
 * the results — which is what the probes measure.
 *
 * <h2>Deviations (BD-74/BD-75)</h2>
 * <ul>
 *   <li>The original's table search
 *       {@code do { it = pos.get(rand.nextInt(pos.size())); } while (it.get(0) != am);}
 *       ({@code :150-152}) has NO bound. With the registry-gated table
 *       ({@link PechTradeInventory#liveRows}) an {@code am} no live row carries
 *       is permanent, and that loop <b>hangs the server thread inside the click
 *       handler</b>. The search is capped at {@link #TABLE_SEARCH_CAP} draws; on
 *       exhaustion the amount goes back to the value (the original's own
 *       give-back shape at {@code :148}) and the roll stops. The loop overall is
 *       capped at {@link #ROLL_ITERATION_CAP} iterations for the same reason:
 *       the original's give-back at {@code :148} re-arms the {@code while
 *       (value > 0)} with no bound.</li>
 *   <li>The {@code ChestGenHooks} branch (b) is <b>dropped</b>:
 *       {@code common/crafting/ChestGenHooks.java} (1304 lines) is the
 *       unported loot-engine lane, its {@code dungeonChest} pool is Forge's
 *       (not a vanilla loot table this port registers), and 21.1.259 has no
 *       single-draw seam on {@code LootTable} — the compiled jar exposes only
 *       {@code getRandomItems(LootParams[, RandomSource])}, not the
 *       {@code getRandomItem(LootParams.Builder)} of later versions. The
 *       branch keeps its gate ({@code am >= 4} + coin flip, so the draw order
 *       is intact) and takes the original's <b>failure arm</b>: the amount is
 *       returned to the value and the loop continues ({@code :147-149}), so the
 *       payout comes from the trade table on the next iteration instead of a
 *       dungeon item.</li>
 * </ul>
 */
public final class PechTradeRoll {

    /** {@code :92} — {@code rand.nextInt(100)}. */
    public static final int SCAM_ROLL_BOUND = 100;
    /** {@code :98} — {@code rand.nextInt(5) == 0} selects the +jitter. */
    public static final int JITTER_MODE_BOUND = 5;
    /** {@code :99,101} — both jitter arms draw {@code rand.nextInt(3)}. */
    public static final int JITTER_AMOUNT_BOUND = 3;
    /** {@code :107} — {@code Math.min(5, …)} caps one payout step. */
    public static final int PAYOUT_MAX_AMOUNT = 5;
    /** {@code :110} — the loot-pack branch runs at {@code am == 1}. */
    public static final int PACK_BRANCH_AMOUNT = 1;
    /** {@code :138} — the chest branch runs at {@code am >= 4}. */
    public static final int CHEST_BRANCH_MIN_AMOUNT = 4;
    /** {@code :141} — the original's own {@code cc < 50} bound on its chest draw. */
    public static final int CHEST_SEARCH_CAP = 50;
    /** BD-74 — the bounded form of the original's unbounded table search ({@code :150-152}). */
    public static final int TABLE_SEARCH_CAP = CHEST_SEARCH_CAP;
    /** BD-74 — the bound on the whole {@code while (value > 0)} loop ({@code :106}). */
    public static final int ROLL_ITERATION_CAP = 256;

    /** One payout step, in the order the original inserted it. */
    public sealed interface Outcome permits FromPack, FromTable {
    }

    /** {@code :110-124} — one item out of the pech's loot pack, at this pack slot. */
    public record FromPack(int packSlot) implements Outcome {
    }

    /** {@code :150-155} — the trade-table row the search matched. */
    public record FromTable(PechTradeInventory.TradeRow row) implements Outcome {
    }

    /**
     * The roll's result: the payouts in insertion order, the value left
     * unspent, the iterations used, and the {@code exhausted} flag — true when
     * the bounded search (BD-74) ended the roll early.
     */
    public record Plan(List<Outcome> outcomes, int leftover, int iterations, boolean exhausted) {

        public int tablePayouts() {
            return (int) this.outcomes.stream().filter(o -> o instanceof FromTable).count();
        }

        public int packPayouts() {
            return (int) this.outcomes.stream().filter(o -> o instanceof FromPack).count();
        }
    }

    /**
     * {@code :92-96} — the scam: {@code nextInt(100) <= value / 2}. Integer
     * division, so a value of 5 tares the pech on a draw of 2 or less, and a
     * value of 1 only on a draw of 0.
     */
    public static boolean scams(final int roll, final int value) {
        return roll <= value / 2;                                                          // :92
    }

    /** {@code :98-102} — the jitter, drawing in the original's order. */
    public static int jitteredValue(final int value, final RandomSource rand) {
        if (rand.nextInt(JITTER_MODE_BOUND) == 0) {                                        // :98
            return value + rand.nextInt(JITTER_AMOUNT_BOUND);                              // :99
        }
        if (rand.nextBoolean()) {                                                          // :100
            return value - rand.nextInt(JITTER_AMOUNT_BOUND);                              // :101
        }
        return value;
    }

    /**
     * {@code :107} — {@code am = min(5, max((value + 1) / 2, rand.nextInt(value) + 1))}.
     * The {@code draw} argument is the {@code rand.nextInt(value)} the caller drew
     * (the draw is unconditional, so its cost must stay visible).
     */
    public static int amountFor(final int value, final int draw) {
        return Math.min(PAYOUT_MAX_AMOUNT, Math.max((value + 1) / 2, draw + 1));            // :107
    }

    /** {@code :118} — the pack slot picked from the non-empty slots. */
    public static int packSlotFor(final int[] packSlots, final RandomSource rand) {
        return packSlots[rand.nextInt(packSlots.length)];                                   // :118
    }

    /**
     * The payout loop ({@code :106-157}) over the LIVE rows of the pech type's
     * table and the pech's non-empty loot-pack slots. {@code liveRows} is the
     * gated draw space (registry-gated); {@code packSlots} is the answer of
     * {@code hasStuffInPack()} ({@code :73-81}) as the indices it found.
     */
    public static Plan plan(final int value, final RandomSource rand, final List<PechTradeInventory.TradeRow> liveRows,
            final int[] packSlots) {
        final List<Outcome> outcomes = new ArrayList<>();
        int remaining = value;
        int iterations = 0;
        boolean exhausted = false;
        while (remaining > 0 && iterations < ROLL_ITERATION_CAP) {                          // :106
            iterations++;
            final int am = amountFor(remaining, rand.nextInt(remaining));                   // :107
            remaining -= am;                                                                // :108
            if (am == PACK_BRANCH_AMOUNT && rand.nextBoolean() && packSlots.length > 0) {    // :110
                outcomes.add(new FromPack(packSlotFor(packSlots, rand)));                    // :112-124
                continue;
            }
            if (am >= CHEST_BRANCH_MIN_AMOUNT && rand.nextBoolean()) {                       // :138 (b)
                remaining += am;                                                             // :148, BD-75
                continue;
            }
            if (liveRows.isEmpty()) {                                                        // :150 (c), no draw space
                remaining += am;                                                             // :148 give-back shape
                exhausted = true;                                                            // BD-74
                break;
            }
            PechTradeInventory.TradeRow matched = null;
            for (int attempt = 0; attempt < TABLE_SEARCH_CAP; attempt++) {                   // :150-152, BD-74
                final PechTradeInventory.TradeRow row = liveRows.get(rand.nextInt(liveRows.size()));
                if (row.amount() == am) {                                                    // :152
                    matched = row;
                    break;
                }
            }
            if (matched == null) {
                remaining += am;                                                             // :148
                exhausted = true;                                                            // BD-74
                break;
            }
            outcomes.add(new FromTable(matched));                                            // :153-155
        }
        if (remaining > 0) {
            exhausted = true;                                                                // BD-74 (loop cap)
        }
        return new Plan(List.copyOf(outcomes), remaining, iterations, exhausted);
    }

    /**
     * The indices of the pech's non-empty loot-pack slots — {@code hasStuffInPack()}
     * ({@code ContainerPech.java:73-81}) read as a domain: the original's
     * {@code loot} list at {@code :112-117} collects exactly these, and an empty
     * array is its {@code false}.
     */
    public static int[] packSlotsOf(final net.minecraft.world.item.ItemStack[] loot) {
        final int[] slots = new int[loot.length];
        int used = 0;
        for (int a = 0; a < loot.length; a++) {                                              // :113-117
            if (loot[a] != null && !loot[a].isEmpty() && loot[a].getCount() > 0) {
                slots[used++] = a;
            }
        }
        return java.util.Arrays.copyOf(slots, used);
    }

    private PechTradeRoll() {
    }
}
