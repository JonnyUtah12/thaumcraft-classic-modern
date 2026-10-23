package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import java.util.Random;

/**
 * Wand right-click on a vis node: the tap math of the original
 * {@code TileNode.onWandRightClick(World, ItemStack, EntityPlayer)} server branch
 * (reference TileNode L385-441), expressed against the TC-M4-02 vis pool instead
 * of the original wand item NBT.
 *
 * <p>Research keys {@code NODETAPPER1}/{@code NODETAPPER2}/{@code NODEPRESERVE}
 * ship in the research graph (data/thaumcraft/research/entries.json);
 * {@link #tapAmount(PlayerKnowledgeData)} reads 1 and
 * {@link #preserveFor(PlayerKnowledgeData, boolean, boolean, boolean)} reads
 * false for an unresearched player, and grow with completed entries. The boolean
 * overloads below are the pure rules the research wrapper feeds.
 */
public final class NodeWandTap {

    /** Original research keys gating the tap (TileNode L392-401). */
    public static final String RESEARCH_NODE_TAPPER_1 = "NODETAPPER1";
    public static final String RESEARCH_NODE_TAPPER_2 = "NODETAPPER2";
    public static final String RESEARCH_NODE_PRESERVE = "NODEPRESERVE";

    private NodeWandTap() {
    }

    /** Outcome of one tap: the pool to store back, the aspect drawn, and counts. */
    public record Result(VisPool pool, Aspect aspect, int taken, int leftover) {
    }

    /** Original tap size: 1 + one per tapper research (TileNode L390-397). */
    public static int tapAmount(final boolean nodeTapper1, final boolean nodeTapper2) {
        int tap = 1;
        if (nodeTapper1) {
            tap++;
        }
        if (nodeTapper2) {
            tap++;
        }
        return tap;
    }

    /** Research-backed tap size: 1 + one per completed NODETAPPER entry. */
    public static int tapAmount(final PlayerKnowledgeData knowledge) {
        return tapAmount(ResearchManager.isResearchComplete(knowledge, RESEARCH_NODE_TAPPER_1),
                ResearchManager.isResearchComplete(knowledge, RESEARCH_NODE_TAPPER_2));
    }

    /**
     * Original preserve gate (TileNode L398-401): not sneaking, research done,
     * and neither a wooden rod nor an iron cap (the cheap wand never preserves).
     */
    public static boolean preserveEligible(final boolean sneaking, final boolean nodePreserveResearch,
            final boolean rodIsWood, final boolean capIsIron) {
        return !sneaking && nodePreserveResearch && !rodIsWood && !capIsIron;
    }

    /** Research-backed preserve; false until the NODEPRESERVE entry exists. */
    public static boolean preserveFor(final PlayerKnowledgeData knowledge, final boolean sneaking,
            final boolean rodIsWood, final boolean capIsIron) {
        return preserveEligible(sneaking, ResearchManager.isResearchComplete(knowledge, RESEARCH_NODE_PRESERVE),
                rodIsWood, capIsIron);
    }

    /**
     * One tap, faithful to TileNode L403-427: pick an aspect the wand has room
     * for, clamp the tap to what the node holds (preserve keeps one), add vis to
     * the pool, and drain the node only by the amount actually stored. A full
     * pool takes nothing from the node.
     *
     * <p>The original builds the room filter over every primal
     * ({@code getAspectsWithRoom}, ItemWandCasting L222-231, marker amount 1)
     * and {@code chooseRandomFilteredFromSource} intersects it with the node's
     * container (TileNode L195-211). Intersecting first yields the same
     * candidate set and keeps the tap free of the static aspect registry — the
     * same seam {@code world/AmbientVis} uses so unit tests need no game
     * bootstrap.
     *
     * @param tap    vis per tap in whole vis (see {@link #tapAmount})
     * @param maxVis the wand's stored capacity in centi-vis (see {@link WandVis#maxVis})
     */
    public static Result tap(final NodeRuntime node, final VisPool pool, final int maxVis,
            final int tap, final boolean preserve, final Random random) {
        final AspectList room = new AspectList();
        for (final Aspect candidate : node.getAspects().getPrimalAspects()) {
            if (WandVis.getVis(pool, candidate) < maxVis) {
                room.add(candidate, 1);
            }
        }
        final Aspect aspect = node.chooseRandomFilteredFromSource(room, preserve, random);
        if (aspect == null) {
            return new Result(pool, null, 0, tap);
        }
        int amount = tap;
        final int held = node.getAspects().getAmount(aspect);
        if (amount > held) {
            amount = held;
        }
        if (preserve && amount == held) {
            amount--;
        }
        if (amount <= 0) {
            return new Result(pool, aspect, 0, amount);
        }
        final WandVis.AddResult add = WandVis.addVis(pool, aspect, amount, maxVis);
        final int leftover = add.leftover();
        if (leftover < amount) {
            final int taken = amount - leftover;
            node.takeVis(aspect, taken);
            return new Result(add.pool(), aspect, taken, leftover);
        }
        return new Result(pool, aspect, 0, leftover);
    }
}
