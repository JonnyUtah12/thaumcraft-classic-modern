package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity-type catalog (TC-M6-06-s): the elemental mob family.
 *
 * <p>Holds the third and last entity-type registration surface in the tree
 * next to {@code ModEntities} (the TC-M2-01 architecture holder, still empty)
 * and the per-card catalog classes: each card owns one holder under
 * {@code registration/catalog/} with its own {@code DeferredRegister}s and
 * appends exactly one wiring line in {@code ModRegistration.init}
 * (docs/catalog-triage.md rule 3).</p>
 *
 * <p>Registered here: {@code thaumcraft:wisp} — the original's
 * {@code EntityWisp}, an {@code EntityFlying implements IMob} registered by
 * {@code ConfigEntities.init} under the name {@code "Wisp"}. The 0.9 x 0.9
 * hitbox, the {@code MONSTER} category and the health/attack attributes are the
 * original's own figures (see {@code WispAiLogic}).</p>
 *
 * <p>Attributes ship through {@link EntityAttributeCreationEvent} because
 * 1.21.1 requires every living entity type to have a supplier before it can be
 * placed in a level; that is the only mod-bus listener this holder needs.</p>
 */
public final class CatalogEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:wisp} — original {@code EntityWisp} (content-mapping
     * {@code Thaumcraft:Wisp}, mod_index 21). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityWisp>> WISP =
            ENTITY_TYPES.register("wisp", () -> EntityType.Builder
                    .of(EntityWisp::new, MobCategory.MONSTER)
                    .sized(com.jonnyutah12.thaumcraftclassic.entities.monster.WispAiLogic.WIDTH,
                            com.jonnyutah12.thaumcraftclassic.entities.monster.WispAiLogic.HEIGHT)
                    .clientTrackingRange(8)
                    .build("wisp"));

    private CatalogEntities() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(WISP.get(), EntityWisp.createAttributes().build());
    }
}
