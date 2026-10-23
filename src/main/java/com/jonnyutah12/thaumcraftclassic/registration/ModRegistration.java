package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogBaubles;
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

        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Modded registries (TC-M4-01): the aspect registry is created via
        // NewRegistryEvent and its 48 holders attach here, same constructor-
        // time rule as the chain above (docs/aspects.md).
        ModAspects.register(modEventBus);
    }

    private ModRegistration() {
    }
}
