package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacleSmall;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityTaintacleGiant;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The taintacle catalog (TC-M6-06-s-e-ii): the three taintacle ids and one
 * {@link EntityAttributeCreationEvent} supplier each.
 *
 * <p>Ids are the content-mapping.csv {@code new_id} column — the id contract
 * (docs/architecture/registration.md): {@code thaumcraft:taintacle}
 * (row {@code Thaumcraft:Taintacle}, original {@code EntityTaintacle}),
 * {@code thaumcraft:taintacletiny} (row {@code Thaumcraft:TaintacleTiny},
 * display <em>Taint Tendril</em>, original {@code EntityTaintacleSmall}) and
 * {@code thaumcraft:taintaclegiant} (row {@code Thaumcraft:TaintacleGiant},
 * original {@code EntityTaintacleGiant} in the decompiled {@code monster/boss}
 * package). The sibling card owns {@code thaumslime} and the remaining taint
 * mobs; the taint animals and the falling taint are TC-M6-06-s-b's.</p>
 *
 * <p>Every hitbox is the original constructor's {@code func_70105_a}: 0.66×3.0
 * ({@code EntityTaintacle.java:30}), 0.22×1.0
 * ({@code EntityTaintacleSmall.java:14}) and 1.1×6.0
 * ({@code EntityTaintacleGiant.java:24}). The taintacle family's eye height is
 * the {@code func_70053_R} override ({@code EntityTaintacle.java:54-56},
 * 0.25F), which 1.21.1 carries on the dimensions — the entity's
 * {@code getDefaultDimensions} override, not the builder, is where it lives, so
 * the builder takes the plain box.</p>
 *
 * <p>No spawn placements are registered: the originals install none
 * ({@code EntityTaintacle.getCanSpawnHere} is the taint biome plus taint
 * underfoot, the taintlet's is {@code false} at
 * {@code EntityTaintacleSmall.java:34-36}, the giant's is {@code false} at
 * {@code EntityTaintacleGiant.java:80-84}), so a taintacle enters a world
 * through the taint engine, the giant's boss spawn, a spawn egg or a command —
 * never through a biome spawn list.</p>
 */
public final class CatalogTaintacles {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /**
     * {@code thaumcraft:taintacle} — {@code EntityTaintacle.java:30}
     * {@code func_70105_a(0.66F, 3.0F)}; the 0.25F eye height is the
     * {@code func_70053_R} override at :54-56, applied by the entity's own
     * {@code getDefaultDimensions}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacle>> TAINTACLE =
            register("taintacle", EntityTaintacle::new, MobCategory.MONSTER,
                    TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT);

    /** {@code thaumcraft:taintacletiny} (Taint Tendril) — {@code EntityTaintacleSmall.java:14} 0.22×1.0. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleSmall>> TAINTACLE_TINY =
            register("taintacletiny", EntityTaintacleSmall::new, MobCategory.MONSTER,
                    TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT);

    /** {@code thaumcraft:taintaclegiant} — {@code EntityTaintacleGiant.java:24} 1.1×6.0. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleGiant>> TAINTACLE_GIANT =
            register("taintaclegiant", EntityTaintacleGiant::new, MobCategory.MONSTER,
                    TaintacleGiantAiLogic.WIDTH, TaintacleGiantAiLogic.HEIGHT);

    private CatalogTaintacles() {
    }

    /**
     * The builder pair the originals give their constructors. The tracking range
     * stays at 1.21.1's default 8 chunks (8 is also what the taint mobs of
     * TC-M6-06-s-b pass); 1.7.10's {@code ConfigEntities} ranges are per-mob
     * config, and none of the taintacle rows has a content-mapping.csv
     * {@code config} row to code against.
     */
    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            final String name, final EntityType.EntityFactory<T> factory,
            final MobCategory category, final float width, final float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, category)
                .sized(width, height)
                .clientTrackingRange(8)
                .build(name));
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogTaintacles::onAttributeCreation);
    }

    /**
     * The attribute pair each original registers in its {@code createAttributes}
     * / {@code func_70098_a} hook: 50 health / 7 attack
     * ({@code EntityTaintacle.java:60-61}), 8 / 2
     * ({@code EntityTaintacleSmall.java:21-22}) and 125 / 9
     * ({@code EntityTaintacleGiant.java:31-32}). Movement speed is never
     * registered by any of them — a taintacle is rooted
     * ({@code EntityTaintacle.java:98-106}) — so the 1.21.1 default stays.
     */
    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(TAINTACLE.get(), EntityTaintacle.createAttributes().build());
        event.put(TAINTACLE_TINY.get(), EntityTaintacleSmall.createAttributes().build());
        event.put(TAINTACLE_GIANT.get(), EntityTaintacleGiant.createAttributes().build());
    }
}
