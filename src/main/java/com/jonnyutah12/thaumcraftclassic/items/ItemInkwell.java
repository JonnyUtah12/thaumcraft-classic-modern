package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import net.minecraft.world.item.Item;

/**
 * Scribing Tools (TC-M3-04-s-v) — the port of
 * {@code thaumcraft.common.items.ItemInkwell}
 * (docs/content-mapping.csv row 210, {@code thaumcraft:iteminkwell}; the
 * original class is named for its pre-TC4 "Inkwell" identity, its English name
 * has always been the lang key {@code item.ItemInkwell.name = "Scribing Tools"}).
 *
 * <p>The class exists in this slice for one reason: it is the shipped
 * implementor of {@link IScribeTools}, the marker
 * {@code ResearchManager.consumeInkFromPlayer} reads as the ink source
 * ({@code common/lib/research/ResearchManager.java:204-217}). The
 * Thaumonomicon's note arm and the research table's hex grid both spend ink
 * from it, so the note-completion interaction has a real, obtainable cost only
 * once an implementor exists. The original's other implementors —
 * {@code ItemBottleTaint} ({@code common/items/ItemBottleTaint.java:15}) and,
 * for the table slot renderers, the same two — stay unported.</p>
 *
 * <p>The constructor is the original's shape verbatim
 * ({@code ItemInkwell.java:23-29}): max stack size 1 ({@code field_77777_bU = 1}
 * → {@code stacksTo(1)}), durability 100 ({@code func_77656_e(100)} →
 * {@code durability(100)}), anvil-repairable (the original's
 * {@code canRepair = true}, which 1.21.1 gives every item with a durability
 * component by default), and no subtypes ({@code func_77627_a(false)} =
 * {@code setHasSubtypes}: the flag is {@code true} on the meta-carrying items
 * such as {@code ItemResource} ({@code ItemResource.java:43}) and
 * {@code ItemResearchNotes} ({@code ItemResearchNotes.java:38}), {@code false}
 * on single-visual items like this one).</p>
 *
 * <p>Deferred, with the reasons in docs/BEHAVIORAL_DEVIATIONS.md (BD-19): the
 * {@code onItemUseFirst} table→research-table conversion
 * ({@code ItemInkwell.java:41-89}) belongs to the research-table block slice,
 * there is no acquisition recipe (the port registers no recipes at all — the
 * same gap the note item's KnowFrag recipe filed as a child card), and the
 * {@code thaumcraft:inkwell} sprite is All Rights Reserved and never committed
 * (D-004), so the item has no model yet.</p>
 */
public class ItemInkwell extends Item implements IScribeTools {

    public ItemInkwell(final Properties properties) {
        super(properties);
    }
}
