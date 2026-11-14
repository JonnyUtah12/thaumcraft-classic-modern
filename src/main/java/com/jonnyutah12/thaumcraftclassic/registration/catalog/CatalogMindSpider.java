package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mind Spider catalog (TC-M6-06-s-c3-b): the one entity type of this card.
 *
 * <p>Per docs/catalog-triage.md rule 3 this card owns its own holder, exactly
 * as the sibling {@code CatalogPech} does: the entity type, the attribute
 * supplier and the one wiring line in {@code ModRegistration.init} all live
 * here, so no two c3 cards edit the same file.</p>
 *
 * <p>Registered here: {@code thaumcraft:mindspider} — original
 * {@code EntityMindSpider} (content-mapping {@code Thaumcraft:MindSpider},
 * line 322, mod_index 24, {@code ConfigEntities.java:105-106}): tracking 64
 * blocks (= 4 chunks), update interval 3, velocity updates ON. The 0.3 x 0.3
 * box is the class's own {@code func_70105_a(0.3F, 0.3F)}
 * ({@code EntityMindSpider.java:18}) and the experience value 1 is
 * {@code :19}; health 1.0 / attack 1.0 are {@code :26-30}. Every figure rides
 * on {@code MindSpiderAiLogic} and is pinned both world-free
 * ({@code MindSpiderAiLogicTest}) and in the world
 * ({@code MindSpiderMobGameTest}).</p>
 *
 * <p>The 0.3 render scale ({@code spiderScaleAmount()}, {@code :67-70}) and this
 * mob's client renderer ship on TC-M7-03-r (kanban {@code t_32366a27}), which
 * pairs the mob with its renderer under {@code client/entity/}; the
 * {@code thaumcraft:spawnegg.mindspider} egg row (the ledger's
 * {@code Thaumcraft:spawnegg.MindSpider}) is the spawn-egg card's, so no item
 * lands here. The mind spider owns no spawn placement of its own — the original
 * has no spawn-list entry and no {@code checkSpawnRules}: it appears from the
 * warp event ({@code WarpEvents.java:281-315}, not ported) and from the Outer
 * Lands passage's mob spawner, whose id seam is
 * {@code world/outerlands/gen/GenPassage} and resolves through the registry
 * from this commit on.</p>
 */
public final class CatalogMindSpider {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:mindspider} — original {@code EntityMindSpider}
     *  (content-mapping {@code Thaumcraft:MindSpider}, line 322, mod_index 24). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityMindSpider>> MIND_SPIDER =
            ENTITY_TYPES.register("mindspider",
                    () -> EntityMindSpider.buildType(MobCategory.MONSTER));

    private CatalogMindSpider() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogMindSpider::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(MIND_SPIDER.get(), EntityMindSpider.createAttributes().build());
    }
}
