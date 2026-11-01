package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.EntityFallingTaint;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintChicken;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCow;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCreeper;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSheep;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpore;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSporeSwarmer;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSwarm;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintVillager;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacleSmall;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityThaumicSlime;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityTaintacleGiant;
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
 * spawn-guard seam pins {@code fallingtaint}, {@code taintspore} and
 * {@code taintswarmer} (the latter two flip on with TC-M6-06-s-e, which is
 * when {@code TaintManager} / {@code BlockTaint} stop falling back to their
 * documented no-entity path). Hitboxes and attributes quote the decompiled
 * constructors — see the per-class AiLogic files and their unit tests.</p>
 *
 * <p>TC-M6-06-s-e added the monster slice: {@code thaumslime}, the taintacle
 * family ({@code taintacle}, {@code taintacletiny}, {@code taintaclegiant})
 * and the spore/swarm chain ({@code taintspore}, {@code taintswarmer},
 * {@code taintswarm}). None of them installs goal selectors — see
 * {@link com.jonnyutah12.thaumcraftclassic.entities.monster.GoallessTaintMonster}
 * — and none of them is in a biome spawn list: the originals install no
 * spawn placements, so the taint engine and the boss fight are their only
 * entry points.</p>
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

    // ------------------------------------------------------------- TC-M6-06-s-e

    /**
     * {@code thaumcraft:thaumslime} — {@code EntityThaumicSlime} ctor line 84
     * boxes the slime as {@code 0.25*sqrt(size)+0.25} on BOTH axes and the
     * ctor (lines 31-34) starts at {@code 1 << rand(3)}, i.e. size 1 → the
     * 0.5 x 0.5 spawning box. The size ladder re-calls {@code setSize} on
     * every spawn, load and merge, so the builder size is only the entry size.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityThaumicSlime>> THAUMIC_SLIME =
            register("thaumslime", EntityThaumicSlime::new, MobCategory.MONSTER, 0.5F, 0.5F);

    /**
     * {@code thaumcraft:taintacle} — {@code EntityTaintacle.java:30}
     * {@code func_70105_a(0.66F, 3.0F)}; the eye height is the
     * {@code func_70053_R} override at :54-56 (0.25F), carried on the
     * dimensions by the entity's {@code getDefaultDimensions} override.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacle>> TAINTACLE =
            register("taintacle", EntityTaintacle::new, MobCategory.MONSTER,
                    TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT);

    /** {@code thaumcraft:taintacletiny} — {@code EntityTaintacleSmall.java:14} 0.22 x 1.0. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleSmall>> TAINTACLE_TINY =
            register("taintacletiny", EntityTaintacleSmall::new, MobCategory.MONSTER,
                    TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT);

    /** {@code thaumcraft:taintaclegiant} — {@code EntityTaintacleGiant.java:24} 1.1 x 6.0. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleGiant>> TAINTACLE_GIANT =
            register("taintaclegiant", EntityTaintacleGiant::new, MobCategory.MONSTER,
                    com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic.WIDTH,
                    com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic.HEIGHT);

    /**
     * {@code thaumcraft:taintspore} — the ctor's {@code setSporeSize(2)}
     * (EntityTaintSpore.java:31) boxes it at {@code max(0.15*2, 0.5)} = 0.5
     * (:51); the size ladder grows that box to 1.5 at size 10 through
     * {@code getDefaultDimensions}, so this is the spawning size only.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSpore>> TAINT_SPORE =
            register("taintspore", EntityTaintSpore::new, MobCategory.MONSTER, 0.5F, 0.5F);

    /**
     * {@code thaumcraft:taintswarmer} — the swarmer's own {@code setSporeSize}
     * copy (EntityTaintSporeSwarmer.java:27) pins a FIXED 1.0 x 1.0 box,
     * smaller than a size-10 spore's 1.5; the size ladder moves the swarm-FX
     * budget and the xp, never the hitbox.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSporeSwarmer>> TAINT_SWARMER =
            register("taintswarmer", EntityTaintSporeSwarmer::new, MobCategory.MONSTER, 1.0F, 1.0F);

    /**
     * {@code thaumcraft:taintswarm} — 2.0 x 2.0. The tracking range is in
     * CHUNKS here, and {@code ConfigEntities.java:137} registered 64 BLOCKS
     * for this mob, so the argument is 4, not 64.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSwarm>> TAINT_SWARM =
            register("taintswarm", EntityTaintSwarm::new, MobCategory.MONSTER, 2.0F, 2.0F, 4);

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
        // TC-M6-06-s-e: the monster slice. The thaumslime's health attribute IS
        // its size (EntityThaumicSlime.java:86), rewritten by setSlimeSize on
        // every spawn, load and merge, so the builder value is a placeholder.
        event.put(THAUMIC_SLIME.get(), EntityThaumicSlime.createAttributes().build());
        event.put(TAINTACLE.get(), EntityTaintacle.createAttributes().build());
        event.put(TAINTACLE_TINY.get(), EntityTaintacleSmall.createAttributes().build());
        event.put(TAINTACLE_GIANT.get(), EntityTaintacleGiant.createAttributes().build());
        event.put(TAINT_SPORE.get(), EntityTaintSpore.createAttributes().build());
        event.put(TAINT_SWARMER.get(), EntityTaintSporeSwarmer.createSwarmerAttributes().build());
        event.put(TAINT_SWARM.get(), EntityTaintSwarm.createAttributes().build());
    }
}
