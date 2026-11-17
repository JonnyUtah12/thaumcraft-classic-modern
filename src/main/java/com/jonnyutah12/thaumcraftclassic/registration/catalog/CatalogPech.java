package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityPechBlast;
import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Pech catalog (TC-M6-06-s-c3-a): the pech monster and its blast projectile.
 *
 * <p>Per docs/catalog-triage.md rule 3 this card owns its own holder: the
 * entity types, the attribute supplier and the wiring line in
 * {@code ModRegistration.init} all live here, so the sibling cards (Mind
 * Spider TC-M6-06-s-c3-b, taint mobs, golems) never edit the same file.</p>
 *
 * <p>Registered here:</p>
 * <ul>
 *   <li>{@code thaumcraft:pechblast} — original {@code EntityPechBlast}
 *       (content-mapping {@code Thaumcraft:PechBlast}, row 277, mod_index 10): the pech
 *       wand's projectile. Not a living entity, so it needs no attribute
 *       supplier. Registration args {@code (64, 20, true)} —
 *       {@code ConfigEntities.java:86}.</li>
 *   <li>{@code thaumcraft:pech} — original {@code EntityPech}
 *       (content-mapping {@code Thaumcraft:Pech}, row 290, mod_index 23): the
 *       0.6 x 1.8 box ({@code EntityPech.java:92}, the 1.7.10 source), the 0.66 eye-height factor
 *       ({@code :211-213}), health 30.0 / attack 6.0 / speed 0.5
 *       ({@code :254-259}) and the +2 armour over the mob base
 *       ({@code :507-514}). Registration args {@code (64, 3, true)} —
 *       {@code ConfigEntities.java:103-104}.</li>
 * </ul>
 *
 * <p>Attributes ship through {@link EntityAttributeCreationEvent} because
 * 1.21.1 requires every living entity type to have a supplier before it can be
 * placed in a level; that is the only mod-bus listener this holder needs.
 * The mind spider's registration and attributes are the sibling card's
 * (TC-M6-06-s-c3-b) and are deliberately absent here.</p>
 */
public final class CatalogPech {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:pechblast} — original {@code EntityPechBlast}
     *  (content-mapping {@code Thaumcraft:PechBlast}, row 277, mod_index 10). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPechBlast>> PECH_BLAST =
            ENTITY_TYPES.register("pechblast",
                    () -> EntityPechBlast.buildType(MobCategory.MISC));

    /** {@code thaumcraft:pech} — original {@code EntityPech}
     *  (content-mapping {@code Thaumcraft:Pech}, row 290, mod_index 23). */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPech>> PECH =
            ENTITY_TYPES.register("pech",
                    () -> EntityPech.buildType(MobCategory.MONSTER));

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    /** GUI_PECH (original gui_id 1, {@code ContainerPech}; content-mapping
     *  {@code Thaumcraft:GUI_PECH}). Entity-bound: the open-menu extra buffer
     *  carries the pech's entity id — the original's {@code openGui(Thaumcraft, 1,
     *  world, entityId, 0, 0)} ({@code EntityPech.java:697}) passes the id as the
     *  x argument and 0 for y/z, so the id is the only payload (see
     *  {@link com.jonnyutah12.thaumcraftclassic.menus.PechMenu}). */
    public static final DeferredHolder<MenuType<?>, MenuType<PechMenu>> GUI_PECH =
            MENUS.register("gui_pech",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                            (menuId, inv, extraData) -> new PechMenu(menuId, inv, extraData)));

    private CatalogPech() {
    }

    public static void init(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(CatalogPech::onAttributeCreation);
    }

    private static void onAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(PECH.get(), EntityPech.createAttributes().build());
    }
}
