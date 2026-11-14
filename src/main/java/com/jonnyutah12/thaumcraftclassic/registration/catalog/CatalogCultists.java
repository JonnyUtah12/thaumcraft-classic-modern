package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Cultist catalog holder (TC-M6-06-s-c4a-1, card t_c59f137b): the Crimson Knight,
 * the first of the four cultists the original {@code
 * thaumcraft.common.entities.monster} package registers.
 *
 * <p>Per docs/catalog-triage.md rule 3 this card owns its own holder: the
 * entity types, the attribute suppliers and the one wiring line in
 * {@code ModRegistration.init} all live here, so the sibling card
 * (TC-M6-06-s-c4b: crab, guardian, inhabited zombie, EldritchOrb), the c5 card
 * (CultistLeader, CultistPortal) and the c6 card (spawn eggs, spawn placement)
 * never edit the same file. Ids are the {@code new_id} column of
 * docs/content-mapping.csv (docs/architecture/registration.md).</p>
 *
 * <p>Registered here — two entries of the original's registration block
 * ({@code ConfigEntities.init}), and nothing else:</p>
 * <ul>
 *   <li>{@code thaumcraft:cultistknight} — {@code EntityCultistKnight}
 *       (content-mapping {@code Thaumcraft:CultistKnight}, CSV line 325,
 *       mod_index 27, "Crimson Knight"); {@code registerModEntity(..., 64, 3,
 *       true)} at {@code ConfigEntities.java:111}.</li>
 *   <li>{@code thaumcraft:cultistcleric} — {@code EntityCultistCleric}
 *       (content-mapping {@code Thaumcraft:CultistCleric}, CSV line 326,
 *       mod_index 28, "Crimson Cleric"); {@code registerModEntity(..., 64, 3,
 *       true)} at {@code ConfigEntities.java:113} — the same tracking 64 /
 *       interval 3 / velocity-updates triple as the knight, so both ride
 *       {@link CultistAiLogic}'s pinned registration constants.</li>
 * </ul>
 *
 * <p>The Cleric arrived from the D-012 split: t_5c5fa1d2 died twice at the
 * wall-clock cap, its authored WIP was sliced at the c4a-1/c4a-2 seam
 * ({@code t_c59f137b} shipped base + knight, {@code t_6160444c} ships the
 * Cleric + GolemOrb half), and the two name/id carriers that seam needed —
 * the Cleric's class NAME in {@code EntityCultist:105-107}'s exclusion and the
 * gang call's cleric-by-registry-id arm (BD-90) — were swapped back to the
 * original's class forms in this commit.</p>
 *
 * <p><b>The base is deliberately absent.</b> {@code EntityCultist} has no
 * {@code registerModEntity} line in the original, no mod index and no ledger
 * row: it is the named goal target of the crab ({@code EntityEldritchCrab:44}),
 * the guardian ({@code EntityEldritchGuardian:64}), the inhabited zombie
 * ({@code EntityInhabitedZombie:30}) and the warden
 * ({@code EntityEldritchWarden:79}), and it lands as the shared unregistered
 * parent — see its javadoc. {@code thaumcraft:cultistleader} and
 * {@code thaumcraft:cultistportal} are card c5's.</p>
 *
 * <p>Attributes ship through {@link EntityAttributeCreationEvent} because
 * 1.21.1 requires a supplier for every living entity type before it can be
 * placed in a level. No spawn placement and no spawn egg: the original's
 * biome-mob-spawn list is empty for cultists (they come from the ritual and the
 * boss structures), and the egg rows are the c6 card's.</p>
 */
public final class CatalogCultists {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:cultistknight} — original {@code EntityCultistKnight}
     *  (content-mapping {@code Thaumcraft:CultistKnight}, CSV line 325,
     *  mod_index 27). {@code ConfigEntities.java:111}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistKnight>> CULTIST_KNIGHT =
            ENTITY_TYPES.register("cultistknight",
                    () -> EntityCultistKnight.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:cultistcleric} — original {@code EntityCultistCleric}
     *  (content-mapping {@code Thaumcraft:CultistCleric}, CSV line 326,
     *  mod_index 28). {@code ConfigEntities.java:113} — {@code
     *  registerModEntity(EntityCultistCleric.class, "CultistCleric", 28,
     *  instance, 64, 3, true)}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistCleric>> CULTIST_CLERIC =
            ENTITY_TYPES.register("cultistcleric",
                    () -> EntityCultistCleric.buildType(MobCategory.MONSTER));

    private CatalogCultists() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogCultists::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(CULTIST_KNIGHT.get(), EntityCultistKnight.createAttributes().build());
        event.put(CULTIST_CLERIC.get(), EntityCultistCleric.createAttributes().build());
    }

    /**
     * The faction test of {@code EntityCultist:105-107} in the target's
     * {@code EntityType} form: the original excludes the Cleric, the Leader and
     * the Knight from its own kind's target selectors, and the Leader's id is
     * the Leader's id is carried by name because the class is card c5's. A cultist type is anything
     * under {@code EntityCultist} in the live registry, so the knight on this card,
     * the Cleric on t_6160444c and the Leader on c5 all read true with no
     * cross-card edit.
     */
    public static boolean isCultistType(final EntityType<?> type) {
        if (type == null) {
            return false;
        }
        final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && CultistAiLogic.isCultistTypeId(id);
    }

    /** The registry check, split out so the rule stays a pure function on the
     *  AiLogic (and the unit test can pin the id set world-free). */
    public static boolean isRegistered(final ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }
}
