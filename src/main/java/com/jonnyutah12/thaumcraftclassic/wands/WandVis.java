package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Wand/vis-container pool semantics (TC-M4-02): the port of the vis storage
 * surface of {@code ItemWandCasting} ({@code common/items/wands/
 * ItemWandCasting.java}), expressed as pure operations over {@link VisPool}
 * so the rules are unit-testable without item classes (those arrive with
 * TC-M6-01/TC-M8-02; design record: docs/architecture/vis.md).
 *
 * <p>All amounts are centi-vis unless the name says otherwise. The
 * all-or-nothing payment rule of the original is preserved exactly:
 * {@code consumeAllVis} checks every aspect before subtracting any
 * (ItemWandCasting.java:325-347), and {@code addVis} clamps to capacity and
 * reports the leftover (ItemWandCasting.java:352-363).</p>
 */
public final class WandVis {

    /** Centi-vis per vis unit (the original's {@code * 100}, ItemWandCasting.java:314,355). */
    public static final int CENTI_VIS_PER_VIS = 100;

    /**
     * Stored capacity per aspect in centi-vis:
     * {@code maxVis = rod.capacity × (isSceptre ? 150 : 100)}
     * (original {@code getMaxVis}, ItemWandCasting.java:82-84; rod capacity
     * table in docs/balance-values.md §2.2). A staff rod without the sceptre
     * flag still uses ×100 — the flag, not the rod class, drives the bonus.
     */
    public static int maxVis(final int rodCapacity, final boolean sceptre) {
        return rodCapacity * (sceptre ? 150 : 100);
    }

    /** Result of a deposit: the new pool plus what did not fit. */
    public record AddResult(VisPool pool, int leftover) {
    }

    /** Current charge of one aspect (original {@code getVis}, ItemWandCasting.java:238-244). */
    public static int getVis(final VisPool pool, final Aspect aspect) {
        return pool.getVis(aspect);
    }

    /**
     * Every primal aspect's charge, zero entries included — the original's
     * {@code getAllVis} (ItemWandCasting.java:206-218), which consumers use to
     * enumerate the full primal row.
     */
    public static AspectList getAllVis(final VisPool pool) {
        final AspectList out = new AspectList();
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            out.merge(aspect, pool.getVis(aspect));
        }
        return out;
    }

    /**
     * Primal aspects that can still accept vis (original {@code getAspectsWithRoom},
     * ItemWandCasting.java:220-229; marker amount 1 preserved).
     */
    public static AspectList getAspectsWithRoom(final VisPool pool, final int maxVis) {
        final AspectList out = new AspectList();
        final AspectList cur = getAllVis(pool);
        for (final Aspect aspect : cur.getAspects()) {
            if (cur.getAmount(aspect) < maxVis) {
                out.add(aspect, 1);
            }
        }
        return out;
    }

    /**
     * Deposit real vis (whole units). Non-primals are refused (leftover 0, pool
     * unchanged — original ItemWandCasting.java:353). Clamps to {@code maxVis};
     * the leftover is reported back in real vis (original returns
     * {@code leftover / 100}, ItemWandCasting.java:354-362).
     */
    public static AddResult addVis(final VisPool pool, final Aspect aspect, final int amount, final int maxVis) {
        if (!aspect.isPrimal()) {
            return new AddResult(pool, 0);
        }
        final AddResult centi = addRealVis(pool, aspect, amount * CENTI_VIS_PER_VIS, maxVis);
        return new AddResult(centi.pool(), centi.leftover() / CENTI_VIS_PER_VIS);
    }

    /**
     * Deposit centi-vis directly (original {@code addRealVis},
     * ItemWandCasting.java:365-375; leftover in centi-vis).
     */
    public static AddResult addRealVis(final VisPool pool, final Aspect aspect, final int centiAmount, final int maxVis) {
        if (!aspect.isPrimal()) {
            return new AddResult(pool, 0);
        }
        final int storeAmount = getVis(pool, aspect) + centiAmount;
        final int leftover = Math.max(storeAmount - maxVis, 0);
        return new AddResult(withVis(pool, aspect, Math.min(storeAmount, maxVis)), leftover);
    }

    /**
     * Withdraw one aspect, all-or-nothing (original {@code consumeVis} after its
     * modifier step, ItemWandCasting.java:305-312 — cost application itself is
     * {@link VisCostRules}). Returns the new pool, or {@code null} when the
     * charge is insufficient (the original's {@code false} return).
     */
    public static VisPool consumeVis(final VisPool pool, final Aspect aspect, final int centiAmount) {
        final int current = getVis(pool, aspect);
        if (current < centiAmount) {
            return null;
        }
        return withVis(pool, aspect, current - centiAmount);
    }

    /**
     * Withdraw many aspects, all-or-nothing: every aspect must cover its cost
     * before any is subtracted (original {@code consumeAllVis} check loop,
     * ItemWandCasting.java:331-337). Returns the new pool, or {@code null}.
     */
    public static VisPool consumeAllVis(final VisPool pool, final AspectList centiCosts) {
        if (centiCosts == null || centiCosts.size() == 0) {
            return null;
        }
        for (final Aspect aspect : centiCosts.getAspects()) {
            if (getVis(pool, aspect) < centiCosts.getAmount(aspect)) {
                return null;
            }
        }
        VisPool result = pool;
        for (final Aspect aspect : centiCosts.getAspects()) {
            result = withVis(result, aspect, getVis(result, aspect) - centiCosts.getAmount(aspect));
        }
        return result;
    }

    /**
     * Crafting payment: recipe costs are whole vis, storage is centi-vis, so the
     * original scaled by 100 before paying ({@code consumeAllVisCrafting},
     * ItemWandCasting.java:314-322). Returns the new pool, or {@code null}.
     */
    public static VisPool consumeAllVisCrafting(final VisPool pool, final AspectList realCosts) {
        if (realCosts == null || realCosts.size() == 0) {
            return null;
        }
        return consumeAllVis(pool, scaleToCenti(realCosts));
    }

    /**
     * The original's whole-vis → centi-vis scaling step
     * ({@code cost = aspects.getAmount(aspect) * 100},
     * ItemWandCasting.java:316-320), used by both crafting payment paths.
     */
    public static AspectList scaleToCenti(final AspectList realCosts) {
        final AspectList centi = new AspectList();
        for (final Aspect aspect : realCosts.getAspects()) {
            centi.add(aspect, realCosts.getAmount(aspect) * CENTI_VIS_PER_VIS);
        }
        return centi;
    }

    // ------------------------------------------------------------- stack glue

    /** Pool on a stack; absent component reads as empty (original: no NBT keys = 0 vis). */
    public static VisPool poolOf(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VIS_POOL.get(), VisPool.EMPTY);
    }

    /** Write the pool back; an empty pool removes the component entirely. */
    public static void setPool(final ItemStack stack, final VisPool pool) {
        if (pool == null || pool.isEmpty()) {
            stack.remove(ModDataComponents.VIS_POOL.get());
        } else {
            stack.set(ModDataComponents.VIS_POOL.get(), pool);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Immutable "set one aspect's charge" — the original's {@code storeVis}. */
    public static VisPool withVis(final VisPool pool, final Aspect aspect, final int centiVis) {
        final Map<String, Integer> next = new LinkedHashMap<>(pool.centiVis());
        next.put(aspect.getTag(), centiVis);
        return new VisPool(next);
    }

    private WandVis() {
    }
}
