package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.jonnyutah12.thaumcraftclassic.core.Refs;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.CultistLeaderAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistLeader;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistPortal;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityEldritchWarden;

/**
 * TC-M6-06-s-c5 — the boss family's entity types: the Praetor, the Crimson Portal
 * and the Eldritch Warden over {@code entities/monster/boss/}. The registration
 * arguments are the original's own ({@code ConfigEntities.java}): the Leader
 * {@code :115} (64/3/true), the Portal {@code :117} (64/<b>20</b>/false), the
 * Warden {@code :109} (64/3/true). The fourth boss, {@code thaumcraft:eldritchgolem}
 * ({@code :119} 64/3/true), stays registered where it has lived since TC-M6-08-s-c
 * — {@link CatalogGolems#ELDRITCH_GOLEM} — and only its CLASS is repointed to the
 * boss body by this card (one registration for one id).
 *
 * <p>The 1.7.10 {@code velocityReduceOnEntitites} flag has no 1.21.1
 * {@code EntityType.Builder} analogue: the Portal's {@code false} is carried by its
 * body (no goals, no gravity, never pushed), which is what the flag protected.</p>
 *
 * <p>No spawn placements: the original registers none for these (the boss/spawn
 * pass summons them). The shared {@code EntityThaumcraftBoss} base carries no id
 * of its own ({@code EntityThaumcraftBoss} has no {@code registerModEntity} line)
 * and is registered nowhere.</p>
 */
public final class CatalogBosses {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.entities(Registries.ENTITY_TYPE, Refs.MODID);

    private static final Map<EntityType<?>, AttributeSupplier.Builder> ATTRIBUTES =
            new LinkedHashMap<>();

    /** {@code ConfigEntities.java:115} — {@code Thaumcraft:CultistLeader}, 64/3. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistLeader>> CULTIST_LEADER =
            register("cultistleader", EntityCultistLeader::new, MobCategory.MONSTER,
                    EntityCultistLeader.WIDTH, EntityCultistLeader.HEIGHT,
                    EntityCultistLeader.TRACKING_RANGE, EntityCultistLeader.UPDATE_INTERVAL,
                    CultistLeaderAiLogic.createAttributes());

    /** {@code ConfigEntities.java:117} — {@code Thaumcraft:CultistPortal}, 64/20. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistPortal>> CULTIST_PORTAL =
            register("cultistportal", EntityCultistPortal::new, MobCategory.MONSTER,
                    EntityCultistPortal.WIDTH, EntityCultistPortal.HEIGHT,
                    EntityCultistPortal.TRACKING_RANGE, EntityCultistPortal.UPDATE_INTERVAL,
                    EntityCultistPortal.createAttributes());

    /** {@code ConfigEntities.java:109} — {@code Thaumcraft:EldritchWarden}, 64/3. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchWarden>> ELDRITCH_WARDEN =
            register("eldritchwarden", EntityEldritchWarden::new, MobCategory.MONSTER,
                    EntityEldritchWarden.WIDTH, EntityEldritchWarden.HEIGHT,
                    EntityEldritchWarden.TRACKING_RANGE, EntityEldritchWarden.UPDATE_INTERVAL,
                    EntityEldritchWarden.createAttributes());

    private CatalogBosses() {
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            final String name, final EntityType.EntityFactory<T> factory, final MobCategory category,
            final float width, final float height, final int tracking, final int interval,
            final AttributeSupplier.Builder attributes) {
        final DeferredHolder<EntityType<?>, EntityType<T>> type = ENTITY_TYPES.register(name,
                () -> EntityType.Builder.of(factory, category)
                        .sized(width, height)
                        .clientTrackingRange(tracking)
                        .updateInterval(interval)
                        .build(name));
        // The builder runs at registration, so the type instance is resolvable now.
        ATTRIBUTES.put(type.get(), attributes);
        return type;
    }

    public static void init(final IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        ATTRIBUTES.forEach((type, builder) -> event.put(type, builder.build()));
    }
}
