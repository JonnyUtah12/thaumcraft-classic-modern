package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBobber;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityDart;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemBell;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemDecoration;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemPlacer;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemUpgrade;
import com.jonnyutah12.thaumcraftclassic.menus.GolemMenu;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Golem catalog holder (TC-M6-08-s): the five golem items and the golem entity
 * type of the original {@code thaumcraft.common.entities.golems} package.
 *
 * <p>Per the catalog-triage ownership rules this card registers its content in
 * its own holder (this file) and adds exactly one wiring line in
 * {@code ModRegistration.init}; ids are the {@code new_id} column of
 * docs/content-mapping.csv (id contract, docs/architecture/registration.md).</p>
 *
 * <p>Registered here:</p>
 * <ul>
 *   <li>{@code thaumcraft:itemgolemplacer} — {@code ItemGolemPlacer}, 8
 *       materials on the {@code thaumcraft:item_meta} component;</li>
 *   <li>{@code thaumcraft:itemgolemcore} — {@code ItemGolemCore}, metas 0..11
 *       plus the blank core at 100;</li>
 *   <li>{@code thaumcraft:itemgolemupgrade} — {@code ItemGolemUpgrade}, metas
 *       0..5 (Air..Entropy);</li>
 *   <li>{@code thaumcraft:golembell} — {@code ItemGolemBell}, stack size 1;</li>
 *   <li>{@code thaumcraft:itemgolemdecoration} — {@code ItemGolemDecoration},
 *       metas 0..7;</li>
 *   <li>{@code thaumcraft:golem} — {@code EntityGolemBase}, the original's
 *       registration
 *       ({@code ConfigEntities.init}: {@code registerModEntity(EntityGolemBase.class,
 *       "Golem", id, instance, 64, 3, true)}) with its 0.4 × 0.95 hitbox.</li>
 *   <li>{@code thaumcraft:gui_golem} — the {@link GolemMenu} menu type for the
 *       golem configuration window (content-mapping {@code Thaumcraft:GUI_GOLEM},
 *       gui_id 0, TC-M7-04-m3-a).</li>
 * </ul>
 *
 * <p>Attributes ship through {@link EntityAttributeCreationEvent} because
 * 1.21.1 requires a supplier for every living entity type before it can be
 * placed in a level. No spawn placement is registered: the original has no
 * biome spawn list for golems — they exist only by being placed from a placer
 * item.</p>
 */
public final class CatalogGolems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    // --- items -----------------------------------------------------------------

    /**
     * {@code thaumcraft:itemgolemplacer} — original {@code ItemGolemPlacer}
     * ("Golem"). Stack size 1 ({@code func_77625_d(1)}); meta 0..7 is the
     * material, defaulting to STRAW exactly as the original's
     * {@code new ItemStack(item)} (damage 0) does.
     */
    public static final DeferredItem<ItemGolemPlacer> ITEM_GOLEM_PLACER =
            ITEMS.register("itemgolemplacer", () -> new ItemGolemPlacer(metaProps().stacksTo(1)));

    /** {@code thaumcraft:itemgolemcore} — original {@code ItemGolemCore}, stack 64, metas 0..11 + 100. */
    public static final DeferredItem<ItemGolemCore> ITEM_GOLEM_CORE =
            ITEMS.register("itemgolemcore", () -> new ItemGolemCore(metaProps()));

    /** {@code thaumcraft:itemgolemupgrade} — original {@code ItemGolemUpgrade}, metas 0..5. */
    public static final DeferredItem<ItemGolemUpgrade> ITEM_GOLEM_UPGRADE =
            ITEMS.register("itemgolemupgrade", () -> new ItemGolemUpgrade(metaProps()));

    /** {@code thaumcraft:golembell} — original {@code ItemGolemBell}, stack size 1 ({@code func_77625_d(1)}). */
    public static final DeferredItem<ItemGolemBell> GOLEM_BELL =
            ITEMS.register("golembell", () -> new ItemGolemBell(new Item.Properties().stacksTo(1)));

    /** {@code thaumcraft:itemgolemdecoration} — original {@code ItemGolemDecoration}, metas 0..7. */
    public static final DeferredItem<ItemGolemDecoration> ITEM_GOLEM_DECORATION =
            ITEMS.register("itemgolemdecoration", () -> new ItemGolemDecoration(metaProps()));

    /**
     * The original's meta-carrying items were plain stack-64 items whose damage
     * value selected the variant; the default {@code item_meta} of 0 reproduces
     * the original's damageless {@code new ItemStack(item)} form
     * (docs/architecture/registration.md ITEM_META convention).
     */
    private static Item.Properties metaProps() {
        return new Item.Properties().component(ModDataComponents.ITEM_META.get(), 0);
    }

    // --- entity types ----------------------------------------------------------

    /**
     * {@code thaumcraft:golem} — original {@code EntityGolemBase}
     * (content-mapping {@code Thaumcraft:Golem}, kind=entity, mod_index 17).
     *
     * <p>Hitbox 0.4 × 0.95, tracking range 64 and update interval 3 are the
     * original's own figures ({@code func_70105_a(0.4F, 0.95F)};
     * {@code registerModEntity(..., 64, 3, true)}). {@link MobCategory#MISC} is
     * the category 1.21.1 uses for the vanilla golem family (iron/snow golem),
     * and matches the original registering no natural spawn entry for golems at
     * all.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGolemBase>> GOLEM =
            ENTITY_TYPES.register("golem", () -> EntityType.Builder
                    .of(EntityGolemBase::new, MobCategory.MISC)
                    .sized(EntityGolemBase.WIDTH, EntityGolemBase.HEIGHT)
                    .clientTrackingRange(EntityGolemBase.TRACKING_RANGE)
                    .updateInterval(EntityGolemBase.UPDATE_INTERVAL)
                    .build("golem"));

    /**
     * {@code thaumcraft:eldritchgolem} — original {@code EntityEldritchGolem}
     * (content-mapping {@code Thaumcraft:EldritchGolem}, mod_index 31). Hitbox
     * 1.75 x 3.5 and tracking 64 / interval 3 are the original's own figures
     * ({@code EntityEldritchGolem.java:54};
     * {@code ConfigEntities.java:119}); category MONSTER is the 1.21.1 home of
     * the 1.7.10 hostile-mob base. No spawn placement: the original registers
     * none for it (it is summoned/boss-spawned).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchGolem>> ELDRITCH_GOLEM =
            ENTITY_TYPES.register("eldritchgolem", () -> EntityType.Builder
                    .of(EntityEldritchGolem::new, MobCategory.MONSTER)
                    .sized(EntityEldritchGolem.WIDTH, EntityEldritchGolem.HEIGHT)
                    .clientTrackingRange(EntityEldritchGolem.TRACKING_RANGE)
                    .updateInterval(EntityEldritchGolem.UPDATE_INTERVAL)
                    .build("eldritchgolem"));

    /**
     * {@code thaumcraft:golembobber} — original {@code EntityGolemBobber}
     * (content-mapping {@code Thaumcraft:GolemBobber}, mod_index 49). Hitbox
     * 0.25 x 0.25 and tracking 64 / interval 64 from the original
     * ({@code EntityGolemBobber.java:49};
     * {@code ConfigEntities.java:154}, third argument false = no velocity
     * sync — the bobber carried its state in the spawn payload).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGolemBobber>> GOLEM_BOBBER =
            ENTITY_TYPES.register("golembobber", () -> EntityType.Builder
                    .of(EntityGolemBobber::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(64)
                    .noSave()
                    .build("golembobber"));

    /**
     * {@code thaumcraft:golemorb} — original {@code EntityGolemOrb}
     * (content-mapping {@code Thaumcraft:GolemOrb}, mod_index 13). The
     * original {@code extends EntityThrowable}, whose 1.7.10 hitbox is the
     * 0.25 x 0.25 projectile default; tracking 64 / interval 20 / velocity
     * synced ({@code ConfigEntities.java:89}, third argument true).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGolemOrb>> GOLEM_ORB =
            ENTITY_TYPES.register("golemorb", () -> EntityType.Builder
                    .<EntityGolemOrb>of(EntityGolemOrb::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("golemorb"));

    /**
     * {@code thaumcraft:dart} — original {@code EntityDart}
     * (content-mapping {@code Thaumcraft:Dart}, mod_index 8). Hitbox
     * 0.5 x 0.5 from the original's own {@code func_70105_a(0.5F, 0.5F)};
     * tracking 64 / interval 20 / no velocity sync
     * ({@code ConfigEntities.java:84}).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityDart>> DART =
            ENTITY_TYPES.register("dart", () -> EntityType.Builder
                    .<EntityDart>of(EntityDart::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .noSave()
                    .build("dart"));

    // --- menus (gui rows) ------------------------------------------------------

    /**
     * GUI_GOLEM (content-mapping.csv {@code Thaumcraft:GUI_GOLEM}, original
     * gui_id 0 / {@code ContainerGolem} + {@code GuiGolem}, TC-M7-04-m3-a).
     * Entity-bound: the open-menu extra buffer carries the golem's entity id
     * (the original's openGui x argument) and {@link GolemMenu}'s client
     * constructor resolves it. The id is the content-mapping gui path (D-001);
     * opened from {@link EntityGolemBase#mobInteract}.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<GolemMenu>> GUI_GOLEM =
            MENUS.register("gui_golem", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    (menuId, inv, extraData) -> new GolemMenu(menuId, inv, extraData)));

    private CatalogGolems() {
    }

    /** Attach the golem holders to the mod bus (single wiring line). */
    public static void init(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(CatalogGolems::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(GOLEM.get(), EntityGolemBase.createAttributes().build());
        event.put(ELDRITCH_GOLEM.get(), EntityEldritchGolem.createAttributes().build());
    }
}
