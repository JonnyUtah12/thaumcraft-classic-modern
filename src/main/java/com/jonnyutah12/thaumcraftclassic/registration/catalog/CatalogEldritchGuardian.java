package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityInhabitedZombie;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
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

    /** Both egg rows shipped with TC-M6-06-s-c6b: the guardian's
     *  {@code thaumcraft:spawnegg.eldritchguardian} (2236962 / 4210752,
     *  {@code ConfigEntities.java:108}) and the Shambling Husk's
     *  {@code thaumcraft:spawnegg.inhabitedzombie} (5601109 / 5570560,
     *  {@code :124}), registered here as {@code SPAWN_EGG_ELDRITCH_GUARDIAN} +
     *  {@code SPAWN_EGG_INHABITED_ZOMBIE}. Pinned for {@code EldritchMobGameTest}
     *  and {@code SpawnEggGameTest}. */
    public static final boolean HAS_SPAWN_EGG_ROW = true;

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
     * {@code thaumcraft:spawnegg.eldritchguardian} — the Eldritch Guardian's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("EldritchGuardian", 2236962, 4210752)} (ConfigEntities.java:108). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.eldritchguardian.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.eldritchguardian}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_ELDRITCH_GUARDIAN =
            ITEMS.register("spawnegg.eldritchguardian", () -> new DeferredSpawnEggItem(
                    ELDRITCH_GUARDIAN::get, 2236962, 4210752, new Item.Properties()));




    /**
     * {@code thaumcraft:spawnegg.inhabitedzombie} — the Shambling Husk's spawner egg
     * (content-mapping {@code Thaumcraft:spawnegg.*}, TC-M6-06-s-c6b). The
     * original mapped it as one entry of the meta-indexed {@code ItemSpawnerEgg}:
     * {@code addMapping("InhabitedZombie", 5601109, 5570560)} (ConfigEntities.java:124). Layer 0 takes
     * the first colour and layer 1 the second ({@code ItemSpawnerEgg#func_82790_a}),
     * the order {@code SpawnEggItem#getColor(int)} answers in, so the pair goes
     * in as background / highlight. The id keeps the ledger's dot verbatim; its
     * model is {@code models/item/spawnegg.inhabitedzombie.json} (the vanilla egg
     * template) and its label is the lang key
     * {@code item.thaumcraft.spawnegg.inhabitedzombie}.
     */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG_INHABITED_ZOMBIE =
            ITEMS.register("spawnegg.inhabitedzombie", () -> new DeferredSpawnEggItem(
                    INHABITED_ZOMBIE::get, 5601109, 5570560, new Item.Properties()));


    private CatalogEldritchGuardian() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
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
                () -> ELDRITCH_GUARDIAN.isBound());
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_GUARDIAN.get(), EntityEldritchGuardian.createAttributes().build());
        event.put(INHABITED_ZOMBIE.get(), EntityInhabitedZombie.createAttributes().build());
    }
}
