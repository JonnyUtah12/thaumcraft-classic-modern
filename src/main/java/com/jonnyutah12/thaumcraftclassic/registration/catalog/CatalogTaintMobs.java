package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.EntityFallingTaint;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintChicken;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCow;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCreeper;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSheep;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintVillager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Taint mob catalog (TC-M6-06-s-b): the taint animals, the taint spider and
 * the falling-taint entity — the ids the TC-M5-04 taint engine looks up at
 * call time ({@code TaintManager.spawnGuarded}, {@code
 * BlockTaint.FALLING_TAINT_ID}).
 *
 * <p>Every id is the content-mapping.csv {@code new_id} for the row; the
 * spawn-guard seam pins {@code fallingtaint}. Hitboxes and attributes quote
 * the decompiled constructors — see the per-class AiLogic files and their
 * unit tests.</p>
 *
 * <p>Still unregistered, and therefore still inert: the taintacle family
 * ({@code taintacle}, {@code taintacletiny}, {@code taintaclegiant}), the
 * spore/swarm chain ({@code taintspore}, {@code taintswarmer},
 * {@code taintswarm}) and {@code thaumslime} — they are the follow-up cards
 * split out of this one (see the card handoff). {@code TaintManager} resolves
 * those ids at call time and falls back to its documented no-entity path.</p>
 */
public final class CatalogTaintMobs {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:fallingtaint} (TC-M5-04's gravity seam). */
    public static final ResourceLocation FALLING_TAINT_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fallingtaint");

    /**
     * {@code EntityFallingTaint} ctor line 38: {@code func_70105_a(0.98F,
     * 0.98F)}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFallingTaint>> FALLING_TAINT =
            register("fallingtaint", EntityFallingTaint::new, MobCategory.MISC, 0.98F, 0.98F, 3);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintChicken>> TAINTED_CHICKEN =
            register("taintedchicken", EntityTaintChicken::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.CHICKEN.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.CHICKEN.height());

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintCow>> TAINTED_COW =
            register("taintedcow", EntityTaintCow::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.COW.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.COW.height());

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintCow.Pig>> TAINTED_PIG =
            register("taintedpig", EntityTaintCow.Pig::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.PIG.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.PIG.height());

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSheep>> TAINTED_SHEEP =
            register("taintedsheep", EntityTaintSheep::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.SHEEP.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.SHEEP.height());

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintCreeper>> TAINTED_CREEPER =
            register("taintedcreeper", EntityTaintCreeper::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.CREEPER.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.CREEPER.height());

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintVillager>> TAINTED_VILLAGER =
            register("taintedvillager", EntityTaintVillager::new, MobCategory.MONSTER,
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.VILLAGER.width(),
                    (float) com.jonnyutah12.thaumcraftclassic.entities.monster
                            .TaintedAnimalAiLogic.VILLAGER.height());

    /**
     * {@code EntityTaintSpider} ctor line 17: 0.4x0.3; the {@code
     * func_70053_R} override (line 41-43) is the 0.1 eye height, carried on
     * the builder in 1.21.1.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSpider>> TAINT_SPIDER =
            registerWithEyeHeight("taintspider", EntityTaintSpider::new, MobCategory.MONSTER,
                    0.4F, 0.3F, 0.1F);

    private CatalogTaintMobs() {
    }

    private static <T extends net.minecraft.world.entity.Entity>
            DeferredHolder<EntityType<?>, EntityType<T>> register(
                    final String name, final EntityType.EntityFactory<T> factory,
                    final MobCategory category, final float width, final float height) {
        return register(name, factory, category, width, height, 8);
    }

    private static <T extends net.minecraft.world.entity.Entity>
            DeferredHolder<EntityType<?>, EntityType<T>> register(
                    final String name, final EntityType.EntityFactory<T> factory,
                    final MobCategory category, final float width, final float height,
                    final int trackingRange) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, category)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .build(name));
    }

    /**
     * 1.7.10 expressed the eye height as the {@code func_70053_R} override;
     * 1.21.1 carries it on the dimensions, so a mob whose original overrides
     * it passes the value to the builder.
     */
    private static <T extends net.minecraft.world.entity.Entity>
            DeferredHolder<EntityType<?>, EntityType<T>> registerWithEyeHeight(
                    final String name, final EntityType.EntityFactory<T> factory,
                    final MobCategory category, final float width, final float height,
                    final float eyeHeight) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, category)
                .sized(width, height)
                .eyeHeight(eyeHeight)
                .clientTrackingRange(8)
                .build(name));
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogTaintMobs::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(TAINTED_CHICKEN.get(), EntityTaintChicken.createAttributes().build());
        event.put(TAINTED_COW.get(), EntityTaintCow.createAttributes().build());
        event.put(TAINTED_PIG.get(), EntityTaintCow.Pig.createPigAttributes().build());
        event.put(TAINTED_SHEEP.get(), EntityTaintSheep.createAttributes().build());
        event.put(TAINTED_CREEPER.get(), EntityTaintCreeper.createAttributes().build());
        event.put(TAINTED_VILLAGER.get(), EntityTaintVillager.createAttributes().build());
        event.put(TAINT_SPIDER.get(), EntityTaintSpider.createAttributes().build());
    }
}
