package com.jonnyutah12.thaumcraftclassic.items;

/**
 * The scribing-tools marker (content-mapping row {@code thaumcraft:iteminkwell},
 * original {@code thaumcraft.api.IScribeTools} — an empty interface, verbatim).
 *
 * <p>Why the interface exists in the port: the original used it as a <b>type
 * seam</b>, not as behaviour. {@code ContainerResearchTable} put
 * {@code new SlotLimitedByClass(IScribeTools.class, tile, 0, 14, 10)} in the
 * research table, so the ink slot accepted anything implementing it — the inkwell
 * and the bottle of taint — and {@code ResearchManager.consumeInkFromTable}
 * (ResearchManager.java:220-230) gated ink on the same check. A port that inlines
 * {@code stack.is(id("iteminkwell"))} in the slot would hardwire one item into a
 * slot the original opened to a whole family.</p>
 *
 * <p>The ink-carrying items themselves are not registered yet:
 * {@code thaumcraft:iteminkwell} (Scribing Tools) and
 * {@code thaumcraft:itembottletaint} are {@code not_started} rows of
 * {@code docs/content-mapping.csv}. Until one of them lands and implements this
 * interface, no stack satisfies {@link #is} and the research table's ink slot
 * refuses everything — stated in {@code docs/BEHAVIORAL_DEVIATIONS.md} BD-19, and
 * the probe that pins the seam says so in the gate message. This class carries the
 * seam, not the items.</p>
 */
public interface IScribeTools {

    /** True when {@code stack} is a scribing tool in the original's sense. */
    static boolean is(final net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IScribeTools;
    }
}
