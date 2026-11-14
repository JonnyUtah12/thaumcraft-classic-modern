package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The pech dice roll (TC-M7-04-m3-b-followup, pool by TC-M7-04-m3-b-c): the port
 * of {@code ContainerPech.generateContents()} —
 * {@code common/entities/ContainerPech.java:83-158} — split into the part that
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
 * ({@code :98-102}); the trade table is read once per click
 * ({@code :104}); then the <b>payout loop</b> ({@code :106-155}) draws
 * {@code am = min(5, max((value + 1) / 2, rand.nextInt(value) + 1))}, subtracts
 * it from the value, and pays from one of three sources: the pech's loot pack
 * ({@code :109-125}, gated on {@code am == 1}, a coin flip and
 * {@code hasStuffInPack()}), a {@code ChestGenHooks} "dungeonChest" draw
 * ({@code :126-142}, gated on {@code am >= 4} and a coin flip), or the
 * {@code EntityPech.tradeInventory} row whose first element equals {@code am}
 * ({@code :143-153}). The window is then marked dirty at {@code :156}.</p>
 *
 * <h2>Draw-order fidelity</h2>
 * Every random draw below happens in the original's order and only where the
 * original drew, because the short-circuit structure of its {@code &&} chains
 * is observable: {@code am == 1 && rand.nextBoolean() && hasStuffInPack()}
 * ({@code :109}) flips the coin <b>even when the pack is empty</b>, and
 * {@code am >= 4 && rand.nextBoolean()} ({@code :126}) flips it only at
 * {@code am >= 4}. A seeded run therefore has to reproduce the draws, not only
 * the results — which is what the probes measure.
 *
 * <h2>Deviations (BD-74, BD-79, BD-80, BD-81)</h2>
 * <ul>
 *   <li>The original's table search
 *       {@code do { it = pos.get(rand.nextInt(pos.size())); } while (it.get(0) != am);}
 *       ({@code :146-148}) has NO bound. With the registry-gated table
 *       ({@link PechTradeInventory#liveRows}) an {@code am} no live row carries
 *       is permanent, and that loop <b>hangs the server thread inside the click
 *       handler</b>. The search is capped at {@link #TABLE_SEARCH_CAP} draws; on
 *       exhaustion the amount goes back to the value (the original's own
 *       give-back shape at {@code :141}) and the roll stops. The loop overall is
 *       capped at {@link #ROLL_ITERATION_CAP} iterations for the same reason:
 *       the original's give-back at {@code :141} re-arms the {@code while
 *       (value > 0)} with no bound.</li>
 *   <li>The {@code ChestGenHooks} branch (b) ships <b>its own pool</b> (BD-80):
 *       the rows 1.7.10 registers into {@code dungeonChest}, transcribed on
 *       {@link PechTradeChestPool} and registry-gated. The pool the original
 *       draws from is the whole server pool — Forge seeds vanilla rows into it
 *       and the 1.7.10 vanilla half is not in {@code reference/} and is not
 *       stable across 1.7.10 builds (D-015) — so the vanilla half of the draw
 *       space is the open remainder. The 1.21.1 chest loot table is not
 *       substituted for it: 21.1.259's {@code LootTable} has no single-draw
 *       weighted seam (its public draws roll the whole table into a
 *       {@code Consumer} / {@code ObjectArrayList}), the id the card named
 *       ({@code CHESTS_DUNGEON}) is a later version's (21.1.259 has
 *       {@code minecraft:chests/simple_dungeon}), and that table's pools roll
 *       1-4 items of weight 2-20, i.e. everything the branch's own filter at
 *       {@code :134} rejects. {@link #drawChestRow} keeps the original's shape:
 *       a uniform draw per attempt over the live rows, the acceptance test
 *       {@code :134} applies, and the original's own 50-attempt bound
 *       ({@link #CHEST_SEARCH_CAP}, {@code cc < 50}); a pool with no live row
 *       routes the branch to the give-back at {@code :141}.</li>
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
    /** {@code :134} — the chest draw accepts a row at weight &le; 5 and max group &le; 1. */
    public static final int CHEST_MAX_WEIGHT = PechTradeChestPool.BRANCH_MAX_WEIGHT;
    public static final int CHEST_MAX_GROUP = PechTradeChestPool.BRANCH_MAX_GROUP;
    /** BD-74 — the bounded form of the original's unbounded table search ({@code :150-152}). */
    public static final int TABLE_SEARCH_CAP = CHEST_SEARCH_CAP;
    /** BD-74 — the bound on the whole {@code while (value > 0)} loop ({@code :123}). */
    public static final int ROLL_ITERATION_CAP = 256;

    /** One payout step, in the order the original inserted it. */
    public sealed interface Outcome permits FromPack, FromTable, FromChest {
    }

    /** {@code :110-124} — one item out of the pech's loot pack, at this pack slot. */
    public record FromPack(int packSlot) implements Outcome {
    }

    /** {@code :150-155} — the trade-table row the search matched. */
    public record FromTable(PechTradeInventory.TradeRow row) implements Outcome {
    }

    /**
     * {@code :131-139} — the {@code dungeonChest} row the sampler accepted, paid
     * as {@code wc.theItem.copy()}: one item at the row's meta
     * ({@link PechTradeChestPool}, BD-80).
     */
    public record FromChest(PechTradeChestPool.ChestRow row) implements Outcome {
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

        public int chestPayouts() {
            return (int) this.outcomes.stream().filter(o -> o instanceof FromChest).count();
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
     * The payout loop ({@code :123-157}) over the LIVE rows of the pech type's
     * table, the LIVE {@code dungeonChest} draw space and the pech's non-empty
     * loot-pack slots. {@code liveRows} is the gated trade table (registry-gated);
     * {@code chestPool} is {@link PechTradeChestPool#liveRows} — the pool rows
     * whose items are registered, in the original's registration order;
     * {@code packSlots} is the answer of {@code hasStuffInPack()}
     * ({@code :73-81}) as the indices it found.
     */
    public static Plan plan(final int value, final RandomSource rand, final List<PechTradeInventory.TradeRow> liveRows,
            final List<PechTradeChestPool.ChestRow> chestPool, final int[] packSlots) {
        final List<Outcome> outcomes = new ArrayList<>();
        int remaining = value;
        int iterations = 0;
        boolean exhausted = false;
        while (remaining > 0 && iterations < ROLL_ITERATION_CAP) {                          // :106
            iterations++;
            final int am = amountFor(remaining, rand.nextInt(remaining));                   // :107
            remaining -= am;                                                                // :108
            if (am == PACK_BRANCH_AMOUNT && rand.nextBoolean() && packSlots.length > 0) {    // :109
                outcomes.add(new FromPack(packSlotFor(packSlots, rand)));                    // :110-125
                continue;
            }
            if (am >= CHEST_BRANCH_MIN_AMOUNT && rand.nextBoolean()) {                       // :126 (b)
                final PechTradeChestPool.ChestRow drawn = drawChestRow(chestPool, rand);     // :127-134
                if (drawn == null) {
                    remaining += am;                                                         // :141 give-back
                    continue;
                }
                outcomes.add(new FromChest(drawn));                                          // :136-139
                continue;
            }
            if (liveRows.isEmpty()) {                                                        // :144-148 (c), no draw space
                remaining += am;                                                             // :141 shape (BD-74)
                exhausted = true;                                                            // BD-74
                break;
            }
            PechTradeInventory.TradeRow matched = null;
            for (int attempt = 0; attempt < TABLE_SEARCH_CAP; attempt++) {                   // :146-148, BD-74
                final PechTradeInventory.TradeRow row = liveRows.get(rand.nextInt(liveRows.size()));
                if (row.amount() == am) {                                                    // :148
                    matched = row;
                    break;
                }
            }
            if (matched == null) {
                remaining += am;                                                             // :141 shape
                exhausted = true;                                                            // BD-74
                break;
            }
            outcomes.add(new FromTable(matched));                                            // :150-152
        }
        if (remaining > 0) {
            exhausted = true;                                                                // BD-74 (loop cap)
        }
        return new Plan(List.copyOf(outcomes), remaining, iterations, exhausted);
    }

    /**
     * {@code :148-151} — the original's chest draw:
     * {@code do { wc = contents[rand.nextInt(contents.length)]; cc++; }}
     * {@code while (cc < 50 && (wc.itemWeight > 5 || wc.maxGroupCount > 1))}. It
     * returns the accepted row, or {@code null} when the pool has no draw space
     * (the original's {@code contents} is never empty, so {@code null} is the
     * give-back case only for the gated pool — an unregistered pool) or when all
     * 50 draws were rejected.
     */
    static PechTradeChestPool.ChestRow drawChestRow(final List<PechTradeChestPool.ChestRow> chestPool,
            final RandomSource rand) {
        if (chestPool.isEmpty()) {
            return null;
        }
        for (int attempt = 1; attempt <= CHEST_SEARCH_CAP; attempt++) {                      // :148-151
            final PechTradeChestPool.ChestRow drawn = chestPool.get(rand.nextInt(chestPool.size()));
            if (PechTradeChestPool.payableByTheBranch(drawn)) {                              // :149
                return drawn;
            }
        }
        return null;
    }

    /**
     * The payout loop with NO {@code dungeonChest} draw space — the state of the
     * port before BD-80, kept for the probes that pin the empty-pool arm
     * (branch (b) then takes the original's give-back at {@code :155}).
     */
    public static Plan plan(final int value, final RandomSource rand, final List<PechTradeInventory.TradeRow> liveRows,
            final int[] packSlots) {
        return plan(value, rand, liveRows, List.of(), packSlots);
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
