package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityBrainyZombie;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityFireBat;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityGiantBrainyZombie;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistLeader;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistPortal;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityEldritchWarden;
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

    /** {@code thaumcraft:firebat} — original {@code EntityFireBat}
     * (content-mapping {@code Thaumcraft:Firebat}, mod_index 22): the
     * 0.5 x 0.9 hitbox and the fire immunity ({@code field_70178_ae = true},
     * {@code EntityFireBat.java:32}) come from the original; health 5.0 and
     * attack 1.0 are its {@code func_110147_ax} figures (:75-76), with the
     * devil 15.0 / summoned {@code (devil ? 3 : 2) + damBonus} applied at
     * runtime by the entity. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFireBat>> FIREBAT =
            ENTITY_TYPES.register("firebat",
                    () -> EntityFireBat.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:brainyzombie} — original {@code EntityBrainyZombie}
     *  (content-mapping {@code Thaumcraft:BrainyZombie}, mod_index 19): the
     *  vanilla zombie box (the 54-line class never resizes itself), health 25.0
     *  and attack 5.0 from {@code func_110147_ax:16-17}, the reinforcement
     *  attribute pinned at 0.0 ({@code :18}), and the armour value 2 + 3 from
     *  {@code func_70658_aO:34-41}. Registration args {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:95}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBrainyZombie>> BRAINY_ZOMBIE =
            ENTITY_TYPES.register("brainyzombie",
                    () -> EntityBrainyZombie.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:giantbrainyzombie} — original
     *  {@code EntityGiantBrainyZombie} (content-mapping
     *  {@code Thaumcraft:GiantBrainyZombie}, mod_index 20): the angry zombie's
     *  subclass, so it inherits its attributes and goal shape and overrides
     *  health to 60.0 / attack to 7.0 ({@code :52-53}), carries experience 15
     *  ({@code :14}) and a 0.6 x 1.8 base box that scales by 1.2 + anger
     *  ({@code :15-16}, {@code :24}). Registration args {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:97}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGiantBrainyZombie>> GIANT_BRAINY_ZOMBIE =
            ENTITY_TYPES.register("giantbrainyzombie",
                    () -> EntityGiantBrainyZombie.buildFuriousType(MobCategory.MONSTER));

    /** {@code thaumcraft:cultistleader} — original
     *  {@code monster/boss/EntityCultistLeader} (content-mapping
     *  {@code Thaumcraft:CultistLeader}, mod_index 29): the boss family's first
     *  registered member, riding the shared unregistered
     *  {@code EntityThaumcraftBoss} base. Registration {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:115}; the box 0.75 x 2.25 ({@code :56}) and the
     *  125.0 / 5.0 / 0.32 attributes over the base's knockback 0.95 and follow 40.0
     *  are the original's own figures ({@code CultistLeaderAiLogic}). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistLeader>> CULTIST_LEADER =
            ENTITY_TYPES.register("cultistleader",
                    () -> EntityCultistLeader.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:cultistportal} — original
     *  {@code monster/boss/EntityCultistPortal} (content-mapping
     *  {@code Thaumcraft:CultistPortal}, mod_index 30): the Crimson Portal, the
     *  boss band's summoner. It is NOT on the {@code EntityThaumcraftBoss} base —
     *  the reference extends {@code EntityMob} directly and only implements the
     *  {@code IBossDisplayData} marker ({@code :31}), so the port's class extends
     *  {@code Monster} and carries the bar as a {@code ServerBossEvent} (D-137).
     *  Registration {@code (64, 20, false)} — {@code ConfigEntities.java:117}; the
     *  box 1.5 x 3.0 ({@code :40}), the zero eye height ({@code :68-70}), the fire
     *  immunity ({@code :38}) and the 500.0 / 0.0 / knockback 1.0 / armour 5
     *  figures ({@code :63-65}, {@code :43-45}) are the original's own
     *  ({@code CultistPortalAiLogic}). The goal set is empty by construction:
     *  {@code :83-84} empties the method 1.7.10 ticks the selector from. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistPortal>> CULTIST_PORTAL =
            ENTITY_TYPES.register("cultistportal",
                    () -> EntityCultistPortal.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:eldritchwarden} — original
     *  {@code monster/boss/EntityEldritchWarden} (content-mapping
     *  {@code Thaumcraft:EldritchWarden}, mod_index 26): the boss family's last
     *  registered member, riding the shared {@code EntityThaumcraftBoss} base and
     *  implementing the family's {@code IRangedAttackMob} twin and the
     *  {@code IEldritchMob} marker ({@code :43}). Registration {@code (64, 3,
     *  true)} — {@code ConfigEntities.java:109}; the box 1.5 x 3.5 ({@code :80}),
     *  the eye height 3.1 ({@code :336-338}), the 200.0 / 0.33 / 10.0 attributes
     *  ({@code :104-106}), the armour 4 over the base's pair ({@code :127-129})
     *  and the 500 experience over the base's 50 ({@code :415-417}) are the
     *  original's own ({@code WardenAiLogic}). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchWarden>> ELDRITCH_WARDEN =
            ENTITY_TYPES.register("eldritchwarden",
                    () -> EntityEldritchWarden.buildType(MobCategory.MONSTER));


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
     * {@code thaumcraft:spawnegg.brainyzombie} — the Angry Zombie's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("BrainyZombie", 16761087, 32768)} (ConfigEntities.java:96). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.brainyzombie.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.brainyzombie}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_BRAINY_ZOMBIE =
            ITEMS.register("spawnegg.brainyzombie", () -> new DeferredSpawnEggItem(
                    BRAINY_ZOMBIE::get, 16761087, 32768, new Item.Properties()));

    /**
     * {@code thaumcraft:spawnegg.giantbrainyzombie} — the Furious Zombie's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("GiantBrainyZombie", 16761087, 16384)} (ConfigEntities.java:98). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.giantbrainyzombie.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.giantbrainyzombie}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_GIANT_BRAINY_ZOMBIE =
            ITEMS.register("spawnegg.giantbrainyzombie", () -> new DeferredSpawnEggItem(
                    GIANT_BRAINY_ZOMBIE::get, 16761087, 16384, new Item.Properties()));

    /**
     * {@code thaumcraft:spawnegg.wisp} — the Wisp's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("Wisp", 16761087, 16777215)} (ConfigEntities.java:100). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.wisp.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.wisp}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_WISP =
            ITEMS.register("spawnegg.wisp", () -> new DeferredSpawnEggItem(
                    WISP::get, 16761087, 16777215, new Item.Properties()));

    /**
     * {@code thaumcraft:spawnegg.firebat} — the Firebat's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("Firebat", 16761087, 12582912)} (ConfigEntities.java:102). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.firebat.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.firebat}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_FIREBAT =
            ITEMS.register("spawnegg.firebat", () -> new DeferredSpawnEggItem(
                    FIREBAT::get, 16761087, 12582912, new Item.Properties()));

    /**
     * {@code thaumcraft:spawnegg.eldritchwarden} — the Eldritch Warden's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("EldritchWarden", 5579298, 4210752)} (ConfigEntities.java:110). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.eldritchwarden.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.eldritchwarden}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_ELDRITCH_WARDEN =
            ITEMS.register("spawnegg.eldritchwarden", () -> new DeferredSpawnEggItem(
                    ELDRITCH_WARDEN::get, 5579298, 4210752, new Item.Properties()));


    private CatalogEntities() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(CatalogEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(WISP.get(), EntityWisp.createAttributes().build());
        event.put(FIREBAT.get(), EntityFireBat.createAttributes().build());
        event.put(BRAINY_ZOMBIE.get(), EntityBrainyZombie.createAttributes().build());
        event.put(GIANT_BRAINY_ZOMBIE.get(), EntityGiantBrainyZombie.createAttributes().build());
        event.put(CULTIST_LEADER.get(), EntityCultistLeader.createAttributes().build());
        event.put(CULTIST_PORTAL.get(), EntityCultistPortal.createAttributes().build());
        event.put(ELDRITCH_WARDEN.get(), EntityEldritchWarden.createAttributes().build());
    }
}
