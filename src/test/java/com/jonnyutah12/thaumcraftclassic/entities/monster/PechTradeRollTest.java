package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-m3-b-followup: the pech dice roll arithmetic
 * ({@code ContainerPech.java:83-159} via {@link PechTradeRoll}) over a
 * <b>scripted</b> {@link RandomSource}, so every draw is named and the
 * value→payout sequence is exact.
 *
 * <p>The scripted stream is the point: the original's short-circuits make its
 * draw ORDER observable ({@code :110} flips the coin at {@code am == 1} even
 * with an empty pack; {@code :138} flips it only at {@code am >= 4}), so each
 * case below lists the draws it expects to consume, in order. A roll that draws
 * somewhere else fails on the leftover-draws assert, not on the payout count.</p>
 */
class PechTradeRollTest {

    // ---- the scripted stream ------------------------------------------------

    /** A {@link RandomSource} that answers from a list and records every draw. */
    private static final class Scripted implements RandomSource {
        private final java.util.List<Integer> ints;
        private final java.util.List<Boolean> bools;
        final java.util.List<String> log = new java.util.ArrayList<>();
        private int intAt;
        private int boolAt;

        Scripted(final List<Integer> ints, final List<Boolean> bools) {
            this.ints = ints;
            this.bools = bools;
        }

        @Override
        public int nextInt(final int bound) {
            this.log.add("nextInt(" + bound + ")=" + this.ints.get(this.intAt));
            return this.ints.get(this.intAt++);
        }

        @Override
        public boolean nextBoolean() {
            this.log.add("nextBoolean()=" + this.bools.get(this.boolAt));
            return this.bools.get(this.boolAt++);
        }

        int unusedInts() {
            return this.ints.size() - this.intAt;
        }

        int unusedBools() {
            return this.bools.size() - this.boolAt;
        }

        @Override
        public int nextInt() {
            throw new UnsupportedOperationException("the roll draws bounded ints");
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("the roll draws bounded ints");
        }

        @Override
        public float nextFloat() {
            throw new UnsupportedOperationException("the roll draws bounded ints");
        }

        @Override
        public double nextDouble() {
            throw new UnsupportedOperationException("the roll draws bounded ints");
        }

        @Override
        public double nextGaussian() {
            throw new UnsupportedOperationException("the roll draws bounded ints");
        }

        @Override
        public void setSeed(final long seed) {
            // the scripted stream is order-driven, not seed-driven
        }

        @Override
        public RandomSource fork() {
            return this;
        }

        @Override
        public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional() {
            return null;
        }
    }

    // ---- the rows: a minimal stand-in for a filtered table ------------------

    private static PechTradeInventory.TradeRow row(final int amount, final String path) {
        return new PechTradeInventory.TradeRow(amount,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", path), -1, null);
    }

    /** Amounts 1..5, one row each: the shape the roll matches on. */
    private static List<PechTradeInventory.TradeRow> ladder() {
        return List.of(row(1, "a1"), row(2, "a2"), row(3, "a3"), row(4, "a4"), row(5, "a5"));
    }

    // ---- :107 the amount formula -------------------------------------------

    @Test
    @DisplayName("am = min(5, max((value + 1) / 2, rand.nextInt(value) + 1)) (:107)")
    void amountFormula() {
        // (value, draw) -> am, each pair read off the formula's own branches
        assertEquals(1, PechTradeRoll.amountFor(1, 0), "value 1 can only draw 0, am 1");
        assertEquals(1, PechTradeRoll.amountFor(2, 0), "max(1, 1) = 1");
        assertEquals(2, PechTradeRoll.amountFor(2, 1), "max(1, 2) = 2");
        assertEquals(4, PechTradeRoll.amountFor(7, 0), "the (value + 1) / 2 floor kicks in: max(4, 1)");
        assertEquals(5, PechTradeRoll.amountFor(7, 6), "max(4, 7) = 7, capped to 5");
        assertEquals(5, PechTradeRoll.amountFor(40, 39), "a 40-value roll pays 5 at a time");
        assertEquals(3, PechTradeRoll.amountFor(6, 1), "max(3, 2) = 3");
    }

    // ---- :92 the scam boundary ---------------------------------------------

    @Test
    @DisplayName("the scam is nextInt(100) <= value / 2 with INTEGER division (:92)")
    void scamBoundary() {
        assertTrue(PechTradeRoll.scams(2, 5), "5 / 2 = 2, so a draw of 2 tares the pech");
        assertFalse(PechTradeRoll.scams(3, 5), "3 > 2 keeps it tamed");
        assertTrue(PechTradeRoll.scams(3, 7), "7 / 2 = 3");
        assertFalse(PechTradeRoll.scams(4, 7), "the boundary is inclusive, 4 is over");
        assertTrue(PechTradeRoll.scams(0, 1), "value 1 tares only on a draw of 0");
        assertFalse(PechTradeRoll.scams(1, 1), "1 > 0");
        assertTrue(PechTradeRoll.scams(0, 0), "value 0 tares on 0 (0 <= 0)");
        assertTrue(PechTradeRoll.scams(99, 200), "value 200 tares on any draw of 0..99");
    }

    // ---- :98-102 the jitter -------------------------------------------------

    @Test
    @DisplayName("nextInt(5) == 0 adds nextInt(3); the coin then subtracts nextInt(3); else no change (:98-102)")
    void jitterDrawOrder() {
        final Scripted plus = new Scripted(List.of(0, 2), List.of());
        assertEquals(9, PechTradeRoll.jitteredValue(7, plus), "mode draw 0 -> +2");
        assertEquals(List.of("nextInt(5)=0", "nextInt(3)=2"), plus.log, "two draws, in that order");

        final Scripted minus = new Scripted(List.of(4, 1), List.of(true));
        assertEquals(6, PechTradeRoll.jitteredValue(7, minus), "mode 4, coin true -> -1");
        assertEquals(List.of("nextInt(5)=4", "nextBoolean()=true", "nextInt(3)=1"), minus.log);

        final Scripted none = new Scripted(List.of(4), List.of(false));
        assertEquals(7, PechTradeRoll.jitteredValue(7, none), "mode 4, coin false -> untouched");
        assertEquals(List.of("nextInt(5)=4", "nextBoolean()=false"), none.log, "no third draw");
    }

    // ---- :106-157 the payout loop ------------------------------------------

    @Test
    @DisplayName("value 7 pays 4, then 2, then 1 from the table: the exact sequence (:106-155)")
    void exactSequenceValue7() {
        final Scripted stream = new Scripted(
                // it1: nextInt(7)=0 -> am 4; it2: nextInt(3)=0 -> am 2; it3: nextInt(1)=0 -> am 1
                List.of(0, 3, 0, 1, 0, 0),
                // it1 coin (am 4 -> chest arm) false; it2: no coin (am 2); it3: pack coin false
                List.of(false, false));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(7, stream, ladder(), new int[0]);

        assertEquals(List.of("a4", "a2", "a1"), rowsOf(plan), "the payout sequence, in insertion order");
        assertEquals(0, plan.leftover(), "4 + 2 + 1 spends the value");
        assertEquals(3, plan.iterations());
        assertFalse(plan.exhausted(), "the roll ended on its own condition");
        assertEquals(0, stream.unusedInts(), "every scripted int was consumed");
        assertEquals(0, stream.unusedBools(), "every scripted coin was consumed");
    }

    @Test
    @DisplayName("the pack branch: am == 1, coin true, pack non-empty -> one item from that slot (:110-124)")
    void packBranch() {
        // it1: nextInt(7)=0 -> am 4, chest coin false -> table row 3 (a4)
        // it2: nextInt(3)=0 -> am 2 -> table row 1 (a2)
        // it3: nextInt(1)=0 -> am 1, PACK coin true, pack index nextInt(2)=1 -> slot 5
        final Scripted stream = new Scripted(List.of(0, 3, 0, 1, 0, 1), List.of(false, true));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(7, stream, ladder(), new int[]{3, 5});

        assertEquals(List.of("a4", "a2", "PACK@5"), rowsOf(plan), "the pack item pays from slot 5");
        assertEquals(1, plan.packPayouts());
        assertEquals(2, plan.tablePayouts());
        assertEquals(0, stream.unusedInts());
        assertEquals(0, stream.unusedBools());
    }

    @Test
    @DisplayName("the pack coin flips even with an EMPTY pack, and the roll falls through (:110)")
    void packCoinFlipsOnEmptyPack() {
        // am == 1 with an empty pack: the coin is drawn (the original's && order),
        // hasStuffInPack() is false, am < 4 so no chest coin, the table pays.
        final Scripted stream = new Scripted(List.of(0, 0), List.of(true));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(1, stream, ladder(), new int[0]);
        assertEquals(List.of("a1"), rowsOf(plan));
        assertEquals(List.of("nextInt(1)=0", "nextBoolean()=true", "nextInt(5)=0"), stream.log,
                "the coin sits between the amount draw and the table draw (the table draw is bounded by the table size)");
        assertEquals(0, stream.unusedInts(), "the table draw consumed the last scripted int");
    }

    @Test
    @DisplayName("branch (b) with no pool (the pre-BD-80 state): the gate, the coin, the give-back (:126, :141)")
    void chestBranchTakesTheFailureArm() {
        // it1: nextInt(7)=6 -> am 5, chest coin TRUE -> value back to 7
        // it2: nextInt(7)=0 -> am 4, chest coin false -> table row 3 (a4)
        // it3: nextInt(3)=0 -> am 2 -> table row 1 (a2); it4: nextInt(1)=0 -> am 1 -> table row 0 (a1)
        final Scripted stream = new Scripted(List.of(6, 0, 3, 0, 1, 0, 0), List.of(true, false, false));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(7, stream, ladder(), new int[0]);

        assertEquals(List.of("a4", "a2", "a1"), rowsOf(plan), "no dungeon item, the table pays after the give-back");
        assertEquals(0, plan.leftover());
        assertEquals(4, plan.iterations(), "the dropped branch costs one iteration");
        assertEquals(0, stream.unusedInts(), "the scripted stream is exactly the original's draw order");
    }

    @Test
    @DisplayName("the table search is BOUNDED: an unmatchable amount returns the value and stops (BD-74)")
    void boundedTableSearch() {
        // The table carries only amount 1. The roll draws am=4 and the search
        // spends its whole budget on rows that do not match. The original's
        // do/while (:150-152) would spin here forever.
        final java.util.List<Integer> ints = new java.util.ArrayList<>();
        ints.add(0);                                            // nextInt(7) -> am 4
        for (int a = 0; a < PechTradeRoll.TABLE_SEARCH_CAP; a++) {
            ints.add(0);                                        // every search draw hits the amount-1 row
        }
        final Scripted stream = new Scripted(ints, List.of(false));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(7, stream, List.of(row(1, "a1")), new int[0]);

        assertTrue(plan.outcomes().isEmpty(), "nothing is paid out");
        assertEquals(7, plan.leftover(), "the amount goes back to the value (:148 shape)");
        assertTrue(plan.exhausted(), "the bound, not the value, ended the roll");
        assertEquals(PechTradeRoll.TABLE_SEARCH_CAP + 2, stream.log.size(),
                "the amount draw, the chest coin, then exactly TABLE_SEARCH_CAP table draws, then stop");
        assertEquals(0, stream.unusedInts(), "the bound consumed the scripted draws and stopped the search");
    }

    @Test
    @DisplayName("an empty table pays nothing, returns the value and stops (the gated-table case)")
    void emptyTable() {
        final Scripted stream = new Scripted(List.of(0), List.of(false));
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(7, stream, List.of(), new int[0]);
        assertTrue(plan.outcomes().isEmpty());
        assertEquals(7, plan.leftover());
        assertTrue(plan.exhausted());
    }

    @Test
    @DisplayName("the whole loop is bounded: a stream that keeps taking the dropped (b) arm terminates (BD-74)")
    void loopIsBounded() {
        // value 8, every amount draw yields 5, every chest coin is TRUE: the value is
        // returned every iteration, so the original's while (value > 0) never ends.
        final java.util.List<Integer> ints = new java.util.ArrayList<>();
        final java.util.List<Boolean> bools = new java.util.ArrayList<>();
        for (int a = 0; a < PechTradeRoll.ROLL_ITERATION_CAP + 8; a++) {
            ints.add(4);
            bools.add(true);
        }
        final Scripted stream = new Scripted(ints, bools);
        final PechTradeRoll.Plan plan = PechTradeRoll.plan(8, stream, ladder(), new int[0]);
        assertEquals(PechTradeRoll.ROLL_ITERATION_CAP, plan.iterations(), "the loop stops at the cap");
        assertTrue(plan.exhausted());
        assertTrue(plan.outcomes().isEmpty(), "every iteration took the dropped (b) arm");
    }

    // ---- :73-81 hasStuffInPack as a domain ---------------------------------

    @Test
    @DisplayName("packSlotsOf reads the non-empty loot slots in index order (:73-81)")
    void packSlotsDomain() {
        final net.minecraft.world.item.ItemStack[] loot = new net.minecraft.world.item.ItemStack[9];
        for (int a = 0; a < 9; a++) {
            loot[a] = net.minecraft.world.item.ItemStack.EMPTY;
        }
        assertEquals(0, PechTradeRoll.packSlotsOf(loot).length, "an all-EMPTY pack is the original's false");
        loot[2] = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD);
        loot[7] = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3);
        java.util.Arrays.fill(loot, 4, 5, net.minecraft.world.item.ItemStack.EMPTY);
        assertEquals(java.util.List.of(2, 7), java.util.Arrays.stream(PechTradeRoll.packSlotsOf(loot))
                .boxed().toList(), "the 1.21.1 EMPTY slot is the original's null slot");
    }

    // ---- the dungeonChest pool (BD-80, TC-M7-04-m3-b-c) --------------------

    /** A pool row by its {@code (min, max, weight)} registration triple. */
    private static PechTradeChestPool.ChestRow chestRow(final String path, final int min, final int max,
            final int weight) {
        return new PechTradeChestPool.ChestRow(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", path), 0, min, max, weight);
    }

    /** The rare row the branch accepts, and the common row it rejects. */
    private static final PechTradeChestPool.ChestRow RARE = chestRow("rare", 1, 1, 1);
    private static final PechTradeChestPool.ChestRow COMMON = chestRow("common", 1, 3, 5);

    @Test
    @DisplayName("branch (b) with a live pool pays the drawn row and consumes one draw (:126-139)")
    void chestBranchPaysTheDrawnRow() {
        // it1: nextInt(7)=6 -> am 5; the branch's coin TRUE; nextInt(2)=0 -> the rare row
        // it2: nextInt(2)=1 -> am 2; the table search draws nextInt(5)=1 -> the amount-2 row
        final Scripted stream = new Scripted(List.of(6, 0, 1, 1), List.of(true));
        final PechTradeRoll.Plan plan =
                PechTradeRoll.plan(7, stream, ladder(), List.of(RARE, COMMON), new int[0]);

        assertEquals(List.of("CHEST@rare#0", "a2"), rowsOf(plan),
                "the accepted row pays, not a table row; the rest of the value pays through the table");
        assertEquals(1, plan.chestPayouts());
        assertEquals(1, plan.tablePayouts());
        assertEquals(0, plan.leftover(), "the chest payout consumes the amount (:108, no give-back)");
        assertEquals(2, plan.iterations());
        assertEquals(List.of("nextInt(7)=6", "nextBoolean()=true", "nextInt(2)=0", "nextInt(2)=1", "nextInt(5)=1"),
                stream.log, "amount draw, the branch's coin, ONE pool draw, then the table's search — "
                        + "the sampler stops at the first accepted entry and never enters the table search");
        assertEquals(0, stream.unusedInts());
        assertEquals(0, stream.unusedBools());
    }

    @Test
    @DisplayName("the sampler rejects every unpayable draw and stops at the original's cc < 50 bound (:131-134, :141)")
    void chestSamplerIsBoundedByTheOriginalsOwnCap() {
        // A pool of one unpayable row: every draw is rejected, so the sampler burns
        // exactly CHEST_SEARCH_CAP draws and the branch takes the give-back.
        final java.util.List<Integer> ints = new java.util.ArrayList<>();
        final java.util.List<Boolean> bools = new java.util.ArrayList<>();
        ints.add(0);                                                      // nextInt(7)=0 -> am 4
        bools.add(true);                                                  // the branch's coin
        for (int a = 0; a < PechTradeRoll.CHEST_SEARCH_CAP; a++) {
            ints.add(0);                                                  // nextInt(1)=0, every draw
        }
        // the give-back re-arms the loop; cap the scripted stream so the test ends
        for (int a = 1; a < PechTradeRoll.ROLL_ITERATION_CAP; a++) {
            ints.add(0);
            bools.add(true);
            for (int b = 0; b < PechTradeRoll.CHEST_SEARCH_CAP; b++) {
                ints.add(0);
            }
        }
        final Scripted stream = new Scripted(ints, bools);
        final PechTradeRoll.Plan plan =
                PechTradeRoll.plan(7, stream, ladder(), List.of(COMMON), new int[0]);

        assertTrue(plan.outcomes().isEmpty(), "an unpayable pool pays nothing");
        assertEquals(7, plan.leftover(), "every iteration gave the amount back (:141)");
        assertTrue(plan.exhausted(), "the loop bound, not the value, ended the roll (BD-74)");
        assertEquals(PechTradeRoll.ROLL_ITERATION_CAP, plan.iterations());
        final List<String> firstIteration = stream.log.subList(0, 2 + PechTradeRoll.CHEST_SEARCH_CAP);
        assertEquals(PechTradeRoll.CHEST_SEARCH_CAP,
                firstIteration.stream().filter(s -> s.startsWith("nextInt(1)")).count(),
                "exactly the original's 50 draws, no 51st");
        assertEquals("nextInt(7)=0", firstIteration.get(0));
        assertEquals("nextBoolean()=true", firstIteration.get(1));
    }

    @Test
    @DisplayName("the filter is the original's field test: weight > 5 or max group > 1 rejects (:134)")
    void chestFilterIsTheFieldTest() {
        assertTrue(PechTradeChestPool.payableByTheBranch(chestRow("rare", 1, 1, 1)), "(1, 1, 1) — rareLoot (:637)");
        assertTrue(PechTradeChestPool.payableByTheBranch(chestRow("edge", 1, 1, 5)), "weight 5 is not > 5");
        assertFalse(PechTradeChestPool.payableByTheBranch(chestRow("common", 1, 3, 5)), "max 3 — commonLoot (:615)");
        assertFalse(PechTradeChestPool.payableByTheBranch(chestRow("uncommon", 1, 2, 4)), "max 2 — uncommonLoot (:625)");
        assertFalse(PechTradeChestPool.payableByTheBranch(chestRow("heavy", 1, 1, 6)), "weight 6 is > 5");
        assertFalse(PechTradeChestPool.payableByTheBranch(chestRow("stacking", 1, 2, 1)), "max 2 is > 1");
    }

    @Test
    @DisplayName("the pool is the 22 rows 1.7.10 registers and the filter admits exactly the rare fourteen (Config.java:587-637)")
    void poolIsTheTranscribedRows() {
        assertEquals(22, PechTradeChestPool.pool().size(), "3 common + 5 uncommon + 14 rare");
        assertEquals(14, PechTradeChestPool.payable().size(), "the rareLoot rows");
        for (PechTradeChestPool.ChestRow row : PechTradeChestPool.pool()) {
            assertTrue(row.item().getNamespace().equals("thaumcraft"), "no invented namespace: " + row.item());
            assertEquals(1, row.minChances(), "every registration's second argument is 1 (:615, :625, :637)");
            // The reading of the ctor's third/fourth arguments (max,weight) vs (weight,max)
            // does not change which rows pass: assert the invariance the class doc claims.
            final PechTradeChestPool.ChestRow swapped =
                    chestRow(row.item().getPath(), row.minChances(), row.weight(), row.maxGroup());
            assertEquals(PechTradeChestPool.payableByTheBranch(row), PechTradeChestPool.payableByTheBranch(swapped),
                    "both readings of (" + row.maxGroup() + ", " + row.weight() + ") classify the row the same way");
        }
        assertEquals(3, PechTradeChestPool.pool().stream().filter(r -> r.maxGroup() == 3).count(),
                "the commonLoot trio carries (1, 3, 5)");
        assertEquals(5, PechTradeChestPool.pool().stream().filter(r -> r.maxGroup() == 2 && r.weight() == 4).count(),
                "the uncommonLoot five carry (1, 2, 4)");
        assertEquals(14, PechTradeChestPool.pool().stream().filter(r -> r.maxGroup() == 1 && r.weight() == 1).count(),
                "the rareLoot fourteen carry (1, 1, 1)");
    }

    private static List<String> rowsOf(final PechTradeRoll.Plan plan) {
        return plan.outcomes().stream().map(outcome -> {
            if (outcome instanceof PechTradeRoll.FromPack fromPack) {
                return "PACK@" + fromPack.packSlot();
            }
            if (outcome instanceof PechTradeRoll.FromChest fromChest) {
                return "CHEST@" + fromChest.row().item().getPath() + "#" + fromChest.row().meta();
            }
            return ((PechTradeRoll.FromTable) outcome).row().item().getPath();
        }).toList();
    }
}
