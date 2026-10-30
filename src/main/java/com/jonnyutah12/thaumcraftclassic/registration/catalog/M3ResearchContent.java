package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M3-04-s-ii research content holder: the research note item
 * ({@code thaumcraft:itemresearchnotes}, docs/content-mapping.csv row 209).
 *
 * <p>Per the catalog-triage ownership rule (docs/catalog-triage.md rule 3) this
 * card owns one holder class under {@code registration/catalog/} and appends
 * exactly one wiring line to {@code ModRegistration.init}; the id is the row's
 * {@code new_id} column, guarded by ContentMappingIdContractTest (unit) and
 * RegistrationGameTest (in-game).</p>
 *
 * <p>The properties carry the original constructor shape
 * ({@code common/items/ItemResearchNotes.java:37-40}): {@code setMaxStackSize(1)}
 * → {@code stacksTo(1)} (unstackable), {@code setMaxDamage(0)} → no durability
 * component (a plain item has none), and {@code setHasSubtypes(true)} → the
 * meta-carrying convention, i.e. the original damage value lives in the
 * {@code thaumcraft:item_meta} component (docs/architecture/registration.md).
 * {@code rarity(RARE)} is the original's {@code getRarity} for the note metas
 * ({@code < 64}); the epic discovery family is applied per stack by
 * {@link ItemResearchNotes}.</p>
 */
public final class M3ResearchContent {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);

    /** Attach this card's holder to the mod bus (single wiring line). */
    public static void init(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /**
     * thaumcraft:itemresearchnotes — research notes and discoveries
     * (original {@code thaumcraft.common.items.ItemResearchNotes}, registered
     * as {@code ItemResearchNotes} in {@code ConfigItems.java:365-366}).
     */
    public static final DeferredItem<ItemResearchNotes> ITEM_RESEARCH_NOTES =
            ITEMS.register("itemresearchnotes", () -> new ItemResearchNotes(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)
                            .component(ModDataComponents.ITEM_META.get(), 0)));

    private M3ResearchContent() {
    }
}
