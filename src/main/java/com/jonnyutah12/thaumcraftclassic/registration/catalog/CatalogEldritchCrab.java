package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Eldritch crab catalog holder (TC-M6-06-s-c4b-1, card t_23520cdc): the crab
 * and the orb it fires. Per docs/catalog-triage.md rule 3 this card owns its
 * own holder — the entity types, the attribute supplier and the one wiring line
 * in {@code ModRegistration.init} all live here, so the sibling card
 * (TC-M6-06-s-c4b-2: Eldritch Guardian, Inhabited Zombie) and the c5/c6 cards
 * (CultistPortal, spawn eggs, spawn placement) never edit the same file. Ids
 * are the {@code new_id} column of docs/content-mapping.csv
 * (docs/architecture/registration.md).
 *
 * <p>Registered here — two entries of the original's registration block
 * ({@code ConfigEntities.init}), and nothing else:</p>
 * <ul>
 *   <li>{@code thaumcraft:eldritchcrab} — original {@code EntityEldritchCrab}
 *       (content-mapping {@code Thaumcraft:EldritchCrab}, CSV line 330,
 *       mod_index 32, "Eldritch Crab"); {@code registerModEntity(..., 64, 3,
 *       true)} at {@code ConfigEntities.java:121} — 64 blocks of tracking (= 4
 *       chunks, the family rule pinned at {@code BrainyZombieAiLogicTest:38}),
 *       update interval 3, velocity updates on, the mob triple of the cultists
 *       ({@code :111}/ {@code :113}).</li>
 *   <li>{@code thaumcraft:eldritchorb} — original {@code EntityEldritchOrb}
 *       (content-mapping {@code Thaumcraft:EldritchOrb}, CSV line 309,
 *       mod_index 11, "Eldritch Orb"); {@code registerModEntity(..., 64, 20,
 *       true)} at {@code ConfigEntities.java:87} — the projectile triple of the
 *       Energy Orb ({@code :89}) and the Pech blast ({@code :86}), so 1.21.1's
 *       builder takes 64 / 20 and the 0.25 x 0.25 {@code EntityThrowable}
 *       default box (the original sets no box of its own).</li>
 * </ul>
 *
 * <p>Only the crab needs an {@link EntityAttributeCreationEvent} supplier: the
 * orb is a {@code ThrowableProjectile}, not a {@code LivingEntity}, so 1.21.1
 * asks for no attributes and registering one for it is impossible.</p>
 *
 * <p>No spawn placement and no spawn egg: the crab's 1.7.10 producer is the
 * Outer Lands' crab spawner ({@code tile.EldritchSpawner}, shipped by
 * TC-M6-04f-s as {@code thaumcraft:blockEldritch}'s spawner variant) and the
 * maze's guardian lane, not a biome-mob-spawn row — the original's spawn-list
 * entry for the crab is absent. The egg row
 * ({@code ItemSpawnerEgg.addMapping("EldritchCrab", 5592405, 5570560)},
 * {@code ConfigEntities.java:122}) is card c6's, and the renderer is TC-M7-03.</p>
 */
public final class CatalogEldritchCrab {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:eldritchcrab} — {@code ConfigEntities.java:121}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchCrab>> ELDRITCH_CRAB =
            ENTITY_TYPES.register("eldritchcrab",
                    () -> EntityEldritchCrab.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:eldritchorb} — {@code ConfigEntities.java:87}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchOrb>> ELDRITCH_ORB =
            ENTITY_TYPES.register("eldritchorb", () -> EntityType.Builder
                    .<EntityEldritchOrb>of(EntityEldritchOrb::new, MobCategory.MISC)
                    .sized(EntityEldritchOrb.WIDTH, EntityEldritchOrb.HEIGHT)
                    .clientTrackingRange(EntityEldritchOrb.TRACK_RANGE_CHUNKS)
                    .updateInterval(EntityEldritchOrb.UPDATE_INTERVAL)
                    .build("eldritchorb"));

    private CatalogEldritchCrab() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogEldritchCrab::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_CRAB.get(), EntityEldritchCrab.createAttributes().build());
    }
}
