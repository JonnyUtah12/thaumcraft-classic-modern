package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
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

        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Catalog cards (TC-M6-04 split, docs/catalog-triage.md rule 3): each
        // card owns a holder under registration/catalog/ and appends exactly
        // one line here. TC-M6-04a: resource/shard/nugget items, food,
        // essences, mana pod block + tile.
        CatalogResources.init(modEventBus);

        // Modded registries (TC-M4-01): the aspect registry is created via
        // NewRegistryEvent and its 48 holders attach here, same constructor-
        // time rule as the chain above (docs/aspects.md).
        ModAspects.register(modEventBus);

        // Catalog card holders (TC-M6-04*, catalog-triage pattern): each card
        // owns one holder under registration/catalog with a single wiring
        // line here.
        // TC-M6-04c: device blocks (stone/metal/wooden/airy devices, arcane
        // and alchemical furnaces) + their tiles. Wired by TC-M4-04 because
        // the runic matrix lives on blockStoneDevice variant 2; the card
        // itself was abandoned before this wiring line existed.
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlocks.register(modEventBus);
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities.register(modEventBus);
        // TC-M4-04: infusion altar (runic matrix, infusion pillars, recipe
        // types/serializers) + the blockStoneDevice 1/2/3/4 tile bindings.
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M404Infusion.register(modEventBus);
        // TC-M4-05: crucible (tilecrucible BE + blockMetalDevice 0 binding +
        // crucible recipe type/serializer).
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M405Crucible.register(modEventBus);
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture.register(modEventBus);
        // TC-M3-04: research table (inkwell + research notes + the
        // blocktable 2-5 research-table tile bindings + the table menu).
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent.register(modEventBus);
        // TC-M6-04e: trinkets/curios tools (thaumometer, resonator, lodestone,
        // hand mirror, trunk spawner), loot containers (urn/crate), treasure
        // bags, and the hand-mirror/trunk menus.
        com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot.register(modEventBus);
        // TC-M6-04b: cosmetic block family (opaque/solid/crystal, stairs,
        // slabs, candle) + block items + the crystal orientation tile.
        CatalogDecorativeBlocks.init(modEventBus);
        // TC-M6-05: warp-management items (bath salts, sanity soap, sanity
        // checker). The card's potion effects register through ModMobEffects
        // above; its enchantments are datapack payloads (ModEnchantments).
        com.jonnyutah12.thaumcraftclassic.registration.catalog.M605WarpEffects.register(modEventBus);
    }

    private ModRegistration() {
    }
}
