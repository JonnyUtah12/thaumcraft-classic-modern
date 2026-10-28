package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * MC-independent core of the crucible (TC-M4-05): the entropic heat /
 * ignition state machine, the essentia decay step, the overflow step and the
 * per-unit throw loop from {@code thaumcraft.common.tiles.TileCrucible},
 * expressed against plain state + a {@code RandomSource} so the original's
 * transitions are unit-testable without a Level (docs/testing.md; same
 * split as {@link InfusionMatrixLogic}).
 *
 * <p>All constants and branch orders are the original's, verbatim
 * (reference/decompiled {@code TileCrucible.func_145845_h} and
 * {@code attemptSmelt}): heat climbs {@code 1 + bellows*2} per tick while a
 * heat source sits below and the tank holds fluid, capped at 200; it decays
 * by 1 per tick otherwise or with an empty tank. The boiling state is
 * {@code heat > 150}; the original forces a block re-render exactly when
 * heat crosses 151 upward or lands on 149 downward (the client picks the
 * boiling visuals off the tile's heat, so those are the only sync points).
 * The tick counter starts at −100; every successful craft resets it to −250
 * and every absorption to −150, delaying the decay step. The decay step
 * (counter &gt; 100 &amp;&amp; heat &gt; 150) drains 2 water, removes one
 * aspect and — for compounds — splits it into one of its two components
 * (coin flip); a primal instead spills flux. Overflow (pool &gt; 100 vis,
 * every 5th tick) removes one random aspect and spills.</p>
 */
public final class CrucibleStateMachine {

    /** Original field caps/thresholds. */
    public static final int MAX_HEAT = 200;
    /** Boiling is {@code heat > 150}; the upward sync edge is 151. */
    public static final int IGNITION_EDGE = 151;
    /** The downward sync edge: heat decaying to exactly 149 re-renders. */
    public static final int EXTINGUISH_EDGE = 149;
    /** Original {@code maxTags} — above this the pool bleeds off every 5 ticks. */
    public static final int MAX_TAGS = 100;
    public static final int TANK_CAPACITY = 1000;
    /** Water drained per decay step / per craft (original drain(2) / drain(50)). */
    public static final int DECAY_DRAIN = 2;
    public static final int CRAFT_DRAIN = 50;
    /** Original counter seeds: fresh −100, post-absorb −150, post-craft −250. */
    public static final long COUNTER_FRESH = -100L;
    public static final long COUNTER_ABSORBED = -150L;
    public static final long COUNTER_CRAFTED = -250L;

    private short heat = 0;
    private long counter = COUNTER_FRESH;
    private AspectList aspects = new AspectList();

    // ------------------------------------------------------------------ state

    public int heat() {
        return this.heat;
    }

    public void setHeat(final int heat) {
        this.heat = (short) heat;
    }

    public long counter() {
        return this.counter;
    }

    public void setCounter(final long counter) {
        this.counter = counter;
    }

    public AspectList aspects() {
        return this.aspects;
    }

    public void setAspects(final AspectList aspects) {
        this.aspects = aspects;
    }

    /** Original {@code tagAmount()}: total vis currently in the pool. */
    public int tagAmount() {
        int tt = 0;
        for (final Aspect tag : this.aspects.getAspects()) {
            tt += this.aspects.getAmount(tag);
        }
        return tt;
    }

    // ------------------------------------------------------- heat / ignition

    /**
     * Result of one heat step: the new heat plus the two edges that force a
     * block re-render in the original (upward crossing of 151, landing on 149).
     */
    public record HeatStep(int newHeat, boolean ignited, boolean extinguished) {
    }

    /**
     * Faithful port of the tank/heat branch of {@code func_145845_h}
     * (server side). {@code heatSourceBelow} is the original's
     * lava/fire/blockAiry-meta-1 (nitor) test on the block below.
     */
    public HeatStep heatStep(final boolean hasFluid, final boolean heatSourceBelow, final int bellows) {
        final int prevheat = this.heat;
        int h = this.heat;
        boolean ignited = false;
        boolean extinguished = false;
        if (!hasFluid) {
            if (h > 0) {
                h--;
            }
        } else if (heatSourceBelow) {
            if (h < MAX_HEAT) {
                h = h + 1 + bellows * 2;
                if (prevheat < IGNITION_EDGE && h >= IGNITION_EDGE) {
                    ignited = true;
                }
            }
        } else if (h > 0) {
            h--;
            if (h == EXTINGUISH_EDGE) {
                extinguished = true;
            }
        }
        this.heat = (short) h;
        return new HeatStep(h, ignited, extinguished);
    }

    /**
     * Original client-side quirk: on the client the heat is nudged one extra
     * when it crosses 151 upward (the client never runs the heat simulation,
     * it only mirrors the synced value; kept for transition parity).
     */
    public static boolean clientIgnitionNudge(final int prevheat, final int heat) {
        return prevheat < IGNITION_EDGE && heat >= IGNITION_EDGE;
    }

    // ------------------------------------------------------------- overflow

    /** Original: {@code tagAmount() > 100 && counter % 5 == 0}. */
    public boolean overflowDue() {
        return tagAmount() > MAX_TAGS && this.counter % 5L == 0L;
    }

    /**
     * Original {@code takeRandomFromSource}: remove one unit of a random
     * aspect (any aspect, primal or not) and return it.
     */
    @Nullable
    public Aspect takeRandomFromSource(final RandomSource rand) {
        if (this.aspects.size() <= 0) {
            return null;
        }
        final List<Aspect> tags = this.aspects.getAspects();
        final Aspect tag = tags.get(rand.nextInt(tags.size()));
        this.aspects.remove(tag, 1);
        return tag;
    }

    // ---------------------------------------------------------------- decay

    /** Original: {@code counter > 100 && heat > 150}. */
    public boolean decayDue() {
        return this.counter > 100L && this.heat > 150;
    }

    public enum DecayOutcome {
        /** Counter reset, pool empty: nothing was removed. */
        NOTHING,
        /** A primal was consumed: the original spills flux instead of splitting. */
        PRIMAL_SPILLED,
        /** A compound was consumed and split into one component. */
        SPLIT
    }

    /**
     * Faithful port of the decay body. The original picks a random aspect
     * index and — only if that pick is primal — re-rolls once more with a
     * fresh roll (the second pick may also be primal; that quirk is kept).
     * The caller drains {@link #DECAY_DRAIN} water and spills on
     * {@link DecayOutcome#PRIMAL_SPILLED}.
     */
    public DecayOutcome decayStep(final RandomSource rand) {
        this.counter = 0L;
        if (tagAmount() <= 0) {
            return DecayOutcome.NOTHING;
        }
        final List<Aspect> tags = this.aspects.getAspects();
        Aspect a = tags.get(rand.nextInt(tags.size()));
        if (a.isPrimal()) {
            a = tags.get(rand.nextInt(tags.size()));
        }
        this.aspects.remove(a, 1);
        if (!a.isPrimal()) {
            final List<Aspect> components = a.getComponents();
            final Aspect component = rand.nextBoolean() ? components.get(0) : components.get(1);
            this.aspects.add(component, 1);
            return DecayOutcome.SPLIT;
        }
        return DecayOutcome.PRIMAL_SPILLED;
    }

    // ------------------------------------------------------- recipe matching

    /**
     * Original {@code ThaumcraftCraftingManager.findMatchingCrucibleRecipe}
     * selection: among candidates that pass {@code matches} (which includes
     * the research gate and the catalyst + pool-suberset test), the one with
     * the most distinct aspect tags wins; strict {@code >} keeps the first
     * found on ties.
     */
    @Nullable
    public static <T> T findBestMatch(final List<T> candidates, final ToIntFunction<T> aspectKeyCount,
                                      final Predicate<T> matches) {
        int highest = 0;
        int index = -1;
        for (int a = 0; a < candidates.size(); a++) {
            final T candidate = candidates.get(a);
            if (matches.test(candidate)) {
                final int result = aspectKeyCount.applyAsInt(candidate);
                if (result > highest) {
                    highest = result;
                    index = a;
                }
            }
        }
        return index < 0 ? null : candidates.get(index);
    }

    // ---------------------------------------------------------- throw loop

    /**
     * Per-unit hooks the tile provides: {@link #tryMatch} returns the chosen
     * recipe (original findMatchingCrucibleRecipe) or null; {@link #itemTags}
     * returns the item's object + bonus tags (original getObjectTags +
     * getBonusTags); {@link #craft} applies the cost, drains
     * {@link #CRAFT_DRAIN} water and ejects the output; {@link #absorb}
     * adds the tags to the pool.
     */
    public interface SmeltHooks {
        /** Re-checked per unit: the original tests the live tank every pass. */
        boolean hasFluid();

        @Nullable
        Object tryMatch();

        @Nullable
        AspectList itemTags();

        void craft(Object match);
    }

    /**
     * Remaining stack size after the pass plus the original's two post-pass
     * flags: {@code bubble} (something absorbed → bubble sound + re-render)
     * and {@code event} (something crafted → re-render); {@code bounced}
     * marks the early return for a tagless item.
     */
    public record SmeltResult(int remaining, boolean bubble, boolean event, boolean bounced) {
    }

    /**
     * Faithful port of the {@code attemptSmelt} unit loop, including the
     * original's loop/counter interaction: the loop variable climbs while the
     * (mutated) stack size shrinks, so a stack of N processes ceil(N/2)
     * units per collision and the entity keeps the rest.
     */
    public SmeltResult attemptSmelt(final int stacksize, final SmeltHooks hooks) {
        boolean bubble = false;
        boolean event = false;
        int remaining = stacksize;

        for (int a = 0; a < remaining; a++) {
            final Object match = hooks.tryMatch();
            if (match != null && hooks.hasFluid()) {
                hooks.craft(match);
                event = true;
                remaining--;
                this.counter = COUNTER_CRAFTED;
            } else {
                AspectList ot = hooks.itemTags();
                if (ot == null || ot.size() == 0) {
                    return new SmeltResult(remaining, bubble, event, true);
                }
                this.aspects.add(ot);
                bubble = true;
                remaining--;
                this.counter = COUNTER_ABSORBED;
            }
        }
        return new SmeltResult(remaining, bubble, event, false);
    }

    // ------------------------------------------------------------- remnants

    /**
     * Original {@code spillRemnants}: empties tank and pool; the caller
     * spills once per {@code visSize()/2} and re-renders. Returns the number
     * of spill attempts the original would make.
     */
    public int clearRemnants() {
        final int spills = this.aspects.visSize() / 2;
        this.aspects = new AspectList();
        return spills;
    }

    // ------------------------------------------------------- derived values

    /**
     * Faithful port of {@code getFluidHeight} (rendering helper, kept pure):
     * base height from the fluid fraction, raised toward 1 by the vis load.
     */
    public float getFluidHeight(final int fluidAmount) {
        final float base = 0.3F + 0.5F * ((float) fluidAmount / TANK_CAPACITY);
        float out = base + tagAmount() / 100.0F * (1.0F - base);
        if (out > 1.0F) {
            out = 1.001F;
        }
        if (out == 1.0F) {
            out = 0.9999F;
        }
        return out;
    }

    /**
     * Original {@code BlockMetalDevice.isProvidingComparatorPower} for the
     * crucible: {@code floor(visSize/100 * 14) + (visSize > 0 ? 1 : 0)}.
     */
    public static int comparatorSignal(final int visSize) {
        return net.minecraft.util.Mth.floor(visSize / 100.0F * 14.0F) + (visSize > 0 ? 1 : 0);
    }

    /** Original {@code spill()}'s 1-in-4 gate. */
    public static boolean spillGate(final RandomSource rand) {
        return rand.nextInt(4) == 0;
    }
}
