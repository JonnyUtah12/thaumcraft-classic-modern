package com.jonnyutah12.thaumcraftclassic.api;

/**
 * Marker for an item that carries scribing ink (TC-M3-04-s-v).
 *
 * <p>Port of {@code thaumcraft.api.IScribeTools} — in the original it is an
 * empty marker interface ({@code api/IScribeTools.java}, one line) and the only
 * thing that reads it is {@code ResearchManager.consumeInkFromPlayer}
 * ({@code common/lib/research/ResearchManager.java:204-217}): the first stack in
 * the player's main inventory whose item implements it, with durability left on
 * it, is the ink source, and spending ink is one durability use of that stack
 * ({@code :210}). The original's implementors are the scribing tools item
 * {@code ItemInkwell} ({@code common/items/ItemInkwell.java:19}) and
 * {@code ItemBottleTaint}.</p>
 *
 * <p>This slice ships the first implementor: {@code thaumcraft:iteminkwell}
 * (Scribing Tools, {@code items/ItemInkwell}), so the note arm has a real ink
 * source and the content-mapping row for the item flips with it. The original's
 * other implementor, {@code ItemBottleTaint}
 * ({@code thaumcraft:itembottletaint}, a {@code not_started} catalog row),
 * stays unported. No acquisition recipe ships here — the port registers the
 * inkwell through {@code M3ResearchContent}, the recipe cards own recipes — so
 * until one lands the item is obtainable through creative/give only. See
 * docs/BEHAVIORAL_DEVIATIONS.md (BD-19).</p>
 */
public interface IScribeTools {
}
