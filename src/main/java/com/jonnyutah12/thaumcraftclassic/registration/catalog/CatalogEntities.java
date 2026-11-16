package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityBrainyZombie;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityFireBat;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityGiantBrainyZombie;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistLeader;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityCultistPortal;
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

    private CatalogEntities() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(WISP.get(), EntityWisp.createAttributes().build());
        event.put(FIREBAT.get(), EntityFireBat.createAttributes().build());
        event.put(BRAINY_ZOMBIE.get(), EntityBrainyZombie.createAttributes().build());
        event.put(GIANT_BRAINY_ZOMBIE.get(), EntityGiantBrainyZombie.createAttributes().build());
        event.put(CULTIST_LEADER.get(), EntityCultistLeader.createAttributes().build());
        event.put(CULTIST_PORTAL.get(), EntityCultistPortal.createAttributes().build());
    }
}
