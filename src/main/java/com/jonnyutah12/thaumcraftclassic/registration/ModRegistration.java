package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogBaubles;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWandRecipes;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import net.neoforged.bus.api.IEventBus;

/**
 * Central wiring for every registration holder in the mod (TC-M2-01).
 *
 * <p>All {@code DeferredRegister}s must be attached to the mod event bus from
 * the mod constructor, before any {@code RegisterEvent} fires — attaching one
 * later throws "DeferredRegister ... registered too late". This class is the
 * single ordering point; the mod constructor calls {@link #init(IEventBus)}
 * once.</p>
 *
 * <h2>Ordering</h2>
 * NeoForge drives the actual per-registry order itself (GameData fires
 * {@code RegisterEvent} per registry: data components, then vanilla order —
 * blocks before items before block entities — then modded registries). The
 * order below mirrors that dependency chain for readability and safety:
 * <ol>
 *   <li>data components — items reference component types at construction;</li>
 *   <li>blocks;</li>
 *   <li>items — block items reference block holders;</li>
 *   <li>block entities — bound to block holders;</li>
 *   <li>entities, mob effects, particles — independent leaf registries;</li>
 *   <li>creative tabs — reference item/block holders.</li>
 * </ol>
 * Holders owned by other milestones (player attachments TC-M2-02, sound
 * events TC-M7-05) are registered from the mod constructor around this call;
 * they are independent of the block/item chain and must be registered exactly
 * once — do not duplicate them here.
 *
 * <p>{@link ModEnchantments} is deliberately absent: enchantments are a
 * datapack registry in 1.21.1 and are not bus-registered (see its javadoc).
 * Custom registries for aspects, research, wand components and node
 * types/modifiers are created by their feature milestones via
 * {@code NewRegistryEvent} + their own DeferredRegister, following this same
 * constructor-time rule; their registry keys are pinned in
 * docs/architecture/registration.md.</p>
 *
 * <p>The id contract for everything registered is the {@code new_id} column of
 * docs/content-mapping.csv, guarded by ContentMappingIdContractTest (unit) and
 * RegistrationGameTest (in-game). See docs/architecture/registration.md.</p>
 */
public final class ModRegistration {

    /** Set by the gameTestServer run config; enables {@link TestProbeContent}. */
    public static final String TEST_PROBE_PROPERTY = "thaumcraft.testprobe";

    public static void init(final IEventBus modEventBus) {
        if (Boolean.getBoolean(TEST_PROBE_PROPERTY)) {
            TestProbeContent.init();
        }

        // Feature content holders (append-only): each registers into the
        // shared ModItems holder; class-init trigger only, no new bus wiring.
        CatalogBaubles.init(); // TC-M8-02 bauble items
        CatalogWands.init(modEventBus); // TC-M6-01 wand items + thaumcraft:wand_component registry

        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Catalog cards (TC-M6-04 split, docs/catalog-triage.md rule 3): each
        // card owns one holder class under registration/catalog/ with its own
        // DeferredRegisters and appends exactly one wiring line here.
        // TC-M6-04a: resource/shard/nugget mega-items, food (meat nuggets,
        // triple meat treat, mana bean), wisp/crystal essences, mana pod
        // block + tile entity.
        CatalogResources.init(modEventBus);

        // TC-M6-04b: decorative/cosmetic blocks — blockCosmeticOpaque /
        // blockCosmeticSolid / blockCrystal (+ TileCrystal), the four cosmetic
        // stairs, the wood/stone slabs (plus their legacy double-slab ids), the
        // tallow candle, and their variant-carrying block items.
        CatalogDecorativeBlocks.init(modEventBus);

        // TC-M6-04c-s: the shared multi-variant device blocks (blockStoneDevice /
        // blockMetalDevice / blockWoodenDevice / blockAiry) with their block items,
        // this card's standalone device tiles (bellows, sensor, arcane pressure
        // plate, bore + bore base, arcane lamps, nitor, the shared ownership base)
        // and the (block, variant) -> tile suppliers for those metas.
        CatalogDevices.init(modEventBus);

        // TC-M6-04d-s: furniture/storage/display — the table family (blockTable
        // variants incl. the deconstruction table), hungry chest storage with its
        // persisted container inventory, magic box, mirrors (tilemirror/tilemirroressentia),
        // arcane door + key, levitator, and the tiles hosted on sibling device
        // blocks (banner on blockWoodenDevice 8, item grate on blockMetalDevice
        // 5/6, pedestal on blockCosmeticSolid 3/15, wand pedestal + spa on
        // blockStoneDevice 9/11, warding stone/fence), the decon-table/magic-box/spa
        // menus and the specialitemgrate entity; suppliers go through
        // DeviceTileFactory. Research-table tiles/workbench and portable-hole
        // style rows stay on their cards.
        CatalogFurniture.init(modEventBus);

        // TC-M6-04e-s: trinkets / curios tools / loot containers — the old urn
        // and abandoned crate (blockLootUrn / blockLootCrate: rarity carried as
        // the shared ITEM_META component, break-drops from the original Config.
        // initLoot bag tables in loot/LootRoller), treasure bags, the thaumom-
        // eter, essentia resonator, sinister lodestone, magic hand mirror and
        // the traveling-trunk spawner with its hand-mirror and trunk menus
        // (the trunk entity itself stays on TC-M6-06).
        CatalogTrinketsLoot.init(modEventBus);

        // TC-M4-03-s: arcane workbench core — the recipe type + serializer pair
        // the 109 data files under data/thaumcraft/recipe/ resolve through, the
        // workbench block/item + its TileArcaneWorkbench block entity, and vis
        // payment routed through TC-M4-02's cost chain. The GUI (gui_arcane_workbench)
        // stays on TC-M7-04.
        CatalogArcaneWorkbench.init(modEventBus);

        // TC-M4-04-s: infusion altar core — the runic matrix + infusion pillar
        // block entities hosted on blockStoneDevice (its variants 1-4, the
        // arcane pedestal / matrix / two pillar orientations), the (block,
        // variant) -> tile bindings, and the recipe type + serializer pair the
        // 87 data files under data/thaumcraft/recipe/ resolve through. The
        // matrix's client-side particles/runes stay on TC-M7-01 and the essentia
        // jar that feeds it on TC-M4-06.
        CatalogInfusion.init(modEventBus);

        // TC-M4-05-s: crucible (alchemy) core — the tilecrucible block entity
        // on its blockMetalDevice variant-0 host, and the recipe type +
        // serializer pair the 54 data files under data/thaumcraft/recipe/
        // resolve through. The essentia jar that refills it stays on TC-M4-06.
        CatalogCrucible.init(modEventBus);

        // TC-M4-06-s: essentia extraction + storage — the infernal furnace
        // (base + nozzle) and its smelting-bonus table, the warded/void jars
        // with their jar tiles, the glass phial and the filled-jar item. The
        // tube network that moves essentia between them stays on TC-M4-07.
        CatalogEssentiaStorage.init(modEventBus);

        // TC-M6-01-s-b: the wand/sceptre assembly recipes' ingredient type —
        // the JSON form of ArcaneWandRecipe.checkItemEquals (item + original
        // damage value) that the 107 wand + 108 sceptre rows under
        // data/thaumcraft/recipe/ name. The rows themselves ride the
        // CatalogArcaneWorkbench type/serializer pair above.
        CatalogWandRecipes.init(modEventBus);

        // Modded registries (TC-M4-01): the aspect registry is created via
        // NewRegistryEvent and its 48 holders attach here, same constructor-
        // time rule as the chain above (docs/aspects.md).
        ModAspects.register(modEventBus);
    }

    private ModRegistration() {
    }
}
