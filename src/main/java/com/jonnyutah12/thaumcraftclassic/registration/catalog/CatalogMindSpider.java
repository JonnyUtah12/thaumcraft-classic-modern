package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
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


    // --- spawner eggs (TC-M6-06-s-c6b) -------------------------------
    /**
     * The egg items of this holder (TC-M6-06-s-c6b). 1.21.1 registers ONE item
     * per mob, so the original's single meta-indexed {@code ItemSpawnerEgg}
     * becomes one {@code DeferredSpawnEggItem} per mob under the ledger's
     * dotted {@code new_id} (BD-40: the dot is part of the id, and the
     * localization audit resolves the lang key by longest inventoried id
     * prefix).
     */
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);

    /**
     * {@code thaumcraft:spawnegg.mindspider} — the Mind Spider's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("MindSpider", 11184810, 4210752)} (ConfigEntities.java:106). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.mindspider.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.mindspider}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_MIND_SPIDER =
            ITEMS.register("spawnegg.mindspider", () -> new DeferredSpawnEggItem(
                    MIND_SPIDER::get, 11184810, 4210752, new Item.Properties()));


    private CatalogMindSpider() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(CatalogMindSpider::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(MIND_SPIDER.get(), EntityMindSpider.createAttributes().build());
    }
}
