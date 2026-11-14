package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityInhabitedZombie;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Eldritch guardian / inhabited zombie catalog holder (TC-M6-06-s-c4b-2, card
 * t_0a5d6d53). Per docs/catalog-triage.md rule 3 this card owns its own holder:
 * the entity types, the attribute suppliers, the one wiring line in {@code
 * ModRegistration.init} and the {@link EldritchMobHost} claim for the guardian
 * all live here, so the sibling card (TC-M6-06-s-c4b-1, landed as {@code
 * CatalogEldritchCrab}) and the c5/c6 cards (CultistPortal, spawn eggs, spawn
 * placement) never edit the same file. Ids are the {@code new_id} column of
 * docs/content-mapping.csv (docs/architecture/registration.md).
 *
 * <p>Registered here — two entries of the original's registration block
 * ({@code ConfigEntities.init}), and nothing else:</p>
 * <ul>
 *   <li>{@code thaumcraft:eldritchguardian} — original {@code
 *       EntityEldritchGuardian} (content-mapping {@code
 *       Thaumcraft:EldritchGuardian}, CSV line 323, {@code mod_index} per
 *       {@code ConfigEntities.java:107}, display name "Eldritch Guardian");
 *       {@code registerModEntity(..., 64, 3, true)} — 64 blocks of tracking (=
 *       4 chunks, the family rule pinned at {@code BrainyZombieAiLogicTest:38})
 *       and update interval 3 ({@link EldritchGuardianAiLogic}).</li>
 *   <li>{@code thaumcraft:inhabitedzombie} — original {@code
 *       EntityInhabitedZombie} (content-mapping {@code
 *       Thaumcraft:InhabitedZombie}, CSV line 331, display name "Shambling
 *       Husk"); {@code registerModEntity(..., 64, 3, true)} at {@code
 *       ConfigEntities.java:123}, the zombie triple of the {@code
 *       EntityBrainyZombie} row, so 1.21.1's builder takes 64 / 3 and the
 *       original's own {@code :19} 0.6 x 1.95 frame.</li>
 * </ul>
 *
 * <p>The guardian also claims its {@link EldritchMobHost} id
 * ({@code EldritchMobHost.GUARDIAN}): the Outer Lands' altar/lock structures
 * ({@code TileEldritchAltar}, TC-M6-04f-s) and the warp lane's
 * {@code WarpEvents.spawnGuardian} ({@code :257-280}) hand their summon to that
 * seam, and the seam's javadoc assigns the claim to this card — the warp lane's
 * probe says so in its own words ({@code WarpEventsGameTest:242}). Claiming it
 * makes the structures' and the warp lane's summon live with no change on their
 * side; the census the structures run ({@link EldritchMobHost#countFamily})
 * starts reporting the real count, which is what those tiles' stock checks
 * were written against.</p>
 *
 * <p>No spawn placement and no spawn egg: the guardian's 1.7.10 producers are
 * the eldritch structures, the maze's guardian lane and the warp lane, not a
 * biome-mob-spawn row, and the egg rows ({@code content-mapping.csv:354}/{@code
 * :362}) are card c6's. The renderer is TC-M7-03.</p>
 */
public final class CatalogEldritchGuardian {

    /** Neither mob has a spawn-egg row this slice ships (the egg rows
     *  {@code content-mapping.csv:354}/{@code :362} are card c6's), so no egg
     *  item is invented here — the family's rule. Pinned for the unit test. */
    public static final boolean HAS_SPAWN_EGG_ROW = false;

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:eldritchguardian} — {@code ConfigEntities.java:107}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchGuardian>> ELDRITCH_GUARDIAN =
            ENTITY_TYPES.register("eldritchguardian",
                    () -> EntityEldritchGuardian.buildType(MobCategory.MONSTER));

    /** {@code thaumcraft:inhabitedzombie} — {@code ConfigEntities.java:123}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityInhabitedZombie>> INHABITED_ZOMBIE =
            ENTITY_TYPES.register("inhabitedzombie",
                    () -> EntityInhabitedZombie.buildType(MobCategory.MONSTER));

    private CatalogEldritchGuardian() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogEldritchGuardian::onAttributeCreation);
        // The guardian's summon claim (see the javadoc): the structures' and the
        // warp lane's summon builds the registered type at the cell they picked.
        EldritchMobHost.register(EldritchMobHost.GUARDIAN,
                (level, pos) -> {
                    final EntityEldritchGuardian guardian =
                            new EntityEldritchGuardian(ELDRITCH_GUARDIAN.get(), level);
                    guardian.setPos(pos.getX(), pos.getY(), pos.getZ());
                    return level.addFreshEntity(guardian);
                },
                () -> ELDRITCH_GUARDIAN.isPresent());
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_GUARDIAN.get(), EntityEldritchGuardian.createAttributes().build());
        event.put(INHABITED_ZOMBIE.get(), EntityInhabitedZombie.createAttributes().build());
    }
}
