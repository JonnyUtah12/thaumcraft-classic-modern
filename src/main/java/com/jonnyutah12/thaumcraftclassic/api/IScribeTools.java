package com.jonnyutah12.thaumcraftclassic.api;

/**
 * Scribing-tool marker (port of the original {@code thaumcraft.api.IScribeTools},
 * 1.7.10 API — an empty marker, reproduced as one). The research-table scribe
 * slot accepts only stacks whose item implements it
 * ({@code ContainerResearchTable} {@code SlotLimitedByClass(IScribeTools.class)}
 * / {@code TileResearchTable.isItemValidForSlot} case 0), and the ink gate
 * checks the same marker while consuming the vanilla durability component the
 * way the original consumed the damage value
 * ({@code damage < maxDamage}, +1 per grid write —
 * {@code ResearchManager.consumeInkFromTable}, ported as
 * {@code ResearchNoteMenu.consumeInkFromTable}).
 *
 * <p>No shipped item implements this marker yet — the scribe-tools item is its
 * own catalog row (thaumcraft:itemscribetools); the research-note screen's
 * scribe slot is gated against it from day one, so the gate is provable with a
 * synthetic scribe in tests.</p>
 */
public interface IScribeTools {
}
