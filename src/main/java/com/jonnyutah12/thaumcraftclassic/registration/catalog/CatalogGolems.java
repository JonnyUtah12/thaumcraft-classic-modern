package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemBell;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemDecoration;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemPlacer;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemUpgrade;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    private CatalogGolems() {
    }

    /** Attach both golem holders to the mod bus (single wiring line). */
    public static void init(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CatalogGolems::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(GOLEM.get(), EntityGolemBase.createAttributes().build());
    }
}
