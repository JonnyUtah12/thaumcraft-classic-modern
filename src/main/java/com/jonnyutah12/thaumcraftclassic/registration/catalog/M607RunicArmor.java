package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemCultistBoots;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemCultistLeaderArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemCultistPlateArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemCultistRobeArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemFortressArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemRobeArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemThaumiumArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemVoidArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemVoidRobeArmor;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeReveal;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/**
 * Thaumic/runic armor registration (TC-M6-07): the 27 armor item rows
 * docs/content-mapping.csv:214-217, 236-238, 265-267, 269-272, 278-280,
 * 284-293, ids exactly per the {@code new_id} column
 * (docs/architecture/registration.md, D-001 lowercasing), plus the four
 * custom armor materials as registry entries under
 * {@code minecraft:armor_material}.
 *
 * <p>Material values are pinned to {@code ThaumcraftApi}
 * ({@code api/ThaumcraftApi.java:37-41}): defense arrays are the original
 * {@code {helmet, chestplate, leggings, boots}} reduction arrays verbatim;
 * the EnumHelper durability multiplier moves to the item side
 * ({@code Item.Properties#durability}, see the item classes). The
 * original's {@code armorMatVoidFortress} is defined but never used —
 * {@code ItemVoidRobeArmor} pieces are registered with {@code armorMatVoid}
 * ({@code common/config/ConfigItems.java:509-513}) — so no fourth material
 * variant is registered for it. Repair ingredients are empty until the
 * thaumium/void ingots land with TC-M6-04a (the original repaired through
 * the {@code IRepairable} registry); the crimson cult sets use the vanilla
 * {@code ArmorMaterials.IRON} holder exactly as the original passed
 * {@code ArmorMaterial.IRON} to those constructors
 * ({@code ConfigItems.java:521-533}).</p>
 *
 * <p>Set membership follows the original exactly: no robe helm, no fortress
 * boots, no void-fortress boots, no cultist-robe/plate boots (the single
 * cultist boots item is its own row).</p>
 */
public final class M607RunicArmor {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumcraftClassic.MOD_ID);

    /** {@code ThaumcraftApi.armorMatThaumium}: iron defense, enchantability 25. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> THAUMIUM =
            ARMOR_MATERIALS.register("thaumium",
                    () -> material("thaumium", new java.util.EnumMap<>(java.util.Map.of(
                            Type.HELMET, 2, Type.CHESTPLATE, 6, Type.LEGGINGS, 5, Type.BOOTS, 2)), 25, false));

    /** {@code ThaumcraftApi.armorMatSpecial}: the robe material, defense 1/3/2/1. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPECIAL =
            ARMOR_MATERIALS.register("special",
                    () -> material("special", new java.util.EnumMap<>(java.util.Map.of(
                            Type.HELMET, 1, Type.CHESTPLATE, 3, Type.LEGGINGS, 2, Type.BOOTS, 1)), 25, true));

    /** {@code ThaumcraftApi.armorMatThaumiumFortress}: defense 3/7/6/3. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FORTRESS =
            ARMOR_MATERIALS.register("fortress",
                    () -> material("fortress", new java.util.EnumMap<>(java.util.Map.of(
                            Type.HELMET, 3, Type.CHESTPLATE, 7, Type.LEGGINGS, 6, Type.BOOTS, 3)), 25, false));

    /** {@code ThaumcraftApi.armorMatVoid}: defense 3/7/6/3, enchantability 10. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VOID =
            ARMOR_MATERIALS.register("void",
                    () -> material("void", new java.util.EnumMap<>(java.util.Map.of(
                            Type.HELMET, 3, Type.CHESTPLATE, 7, Type.LEGGINGS, 6, Type.BOOTS, 3)), 10, false));

    // ---- Thaumium set (rows 214-217) ------------------------------------------
    public static final DeferredItem<ItemThaumiumArmor> ITEM_HELMET_THAUMIUM =
            ModItems.ITEMS.register("itemhelmetthaumium",
                    () -> new ItemThaumiumArmor(THAUMIUM, Type.HELMET));
    public static final DeferredItem<ItemThaumiumArmor> ITEM_CHESTPLATE_THAUMIUM =
            ModItems.ITEMS.register("itemchestplatethaumium",
                    () -> new ItemThaumiumArmor(THAUMIUM, Type.CHESTPLATE));
    public static final DeferredItem<ItemThaumiumArmor> ITEM_LEGGINGS_THAUMIUM =
            ModItems.ITEMS.register("itemleggingsthaumium",
                    () -> new ItemThaumiumArmor(THAUMIUM, Type.LEGGINGS));
    public static final DeferredItem<ItemThaumiumArmor> ITEM_BOOTS_THAUMIUM =
            ModItems.ITEMS.register("itembootsthaumium",
                    () -> new ItemThaumiumArmor(THAUMIUM, Type.BOOTS));

    // ---- Thaumaturge's robe set (rows 236-238; no helm, ConfigItems:419-423) --
    public static final DeferredItem<ItemRobeArmor> ITEM_CHESTPLATE_ROBE =
            ModItems.ITEMS.register("itemchestplaterobe",
                    () -> new ItemRobeArmor(SPECIAL, Type.CHESTPLATE));
    public static final DeferredItem<ItemRobeArmor> ITEM_LEGGINGS_ROBE =
            ModItems.ITEMS.register("itemleggingsrobe",
                    () -> new ItemRobeArmor(SPECIAL, Type.LEGGINGS));
    public static final DeferredItem<ItemRobeArmor> ITEM_BOOTS_ROBE =
            ModItems.ITEMS.register("itembootsrobe",
                    () -> new ItemRobeArmor(SPECIAL, Type.BOOTS));

    // ---- Fortress set (rows 265-267; no boots, ConfigItems:483-487) -----------
    public static final DeferredItem<ItemFortressArmor> ITEM_HELMET_FORTRESS =
            ModItems.ITEMS.register("itemhelmetfortress",
                    () -> new ItemFortressArmor(FORTRESS, Type.HELMET));
    public static final DeferredItem<ItemFortressArmor> ITEM_CHESTPLATE_FORTRESS =
            ModItems.ITEMS.register("itemchestplatefortress",
                    () -> new ItemFortressArmor(FORTRESS, Type.CHESTPLATE));
    public static final DeferredItem<ItemFortressArmor> ITEM_LEGGINGS_FORTRESS =
            ModItems.ITEMS.register("itemleggingsfortress",
                    () -> new ItemFortressArmor(FORTRESS, Type.LEGGINGS));

    // ---- Void set (rows 269-272) ----------------------------------------------
    public static final DeferredItem<ItemVoidArmor> ITEM_HELMET_VOID =
            ModItems.ITEMS.register("itemhelmetvoid",
                    () -> new ItemVoidArmor(VOID, Type.HELMET));
    public static final DeferredItem<ItemVoidArmor> ITEM_CHESTPLATE_VOID =
            ModItems.ITEMS.register("itemchestplatevoid",
                    () -> new ItemVoidArmor(VOID, Type.CHESTPLATE));
    public static final DeferredItem<ItemVoidArmor> ITEM_LEGGINGS_VOID =
            ModItems.ITEMS.register("itemleggingsvoid",
                    () -> new ItemVoidArmor(VOID, Type.LEGGINGS));
    public static final DeferredItem<ItemVoidArmor> ITEM_BOOTS_VOID =
            ModItems.ITEMS.register("itembootsvoid",
                    () -> new ItemVoidArmor(VOID, Type.BOOTS));

    // ---- Void-fortress set (rows 278-280; void material, no boots) -----------
    public static final DeferredItem<ItemVoidRobeArmor> ITEM_HELMET_VOIDFORTRESS =
            ModItems.ITEMS.register("itemhelmetvoidfortress",
                    () -> new ItemVoidRobeArmor(VOID, Type.HELMET));
    public static final DeferredItem<ItemVoidRobeArmor> ITEM_CHESTPLATE_VOIDFORTRESS =
            ModItems.ITEMS.register("itemchestplatevoidfortress",
                    () -> new ItemVoidRobeArmor(VOID, Type.CHESTPLATE));
    public static final DeferredItem<ItemVoidRobeArmor> ITEM_LEGGINGS_VOIDFORTRESS =
            ModItems.ITEMS.register("itemleggingsvoidfortress",
                    () -> new ItemVoidRobeArmor(VOID, Type.LEGGINGS));

    // ---- Crimson cult sets (rows 284-293; vanilla iron material) --------------
    public static final DeferredItem<ItemCultistRobeArmor> ITEM_HELMET_CULTISTROBE =
            ModItems.ITEMS.register("itemhelmetcultistrobe",
                    () -> new ItemCultistRobeArmor(ArmorMaterials.IRON, Type.HELMET));
    public static final DeferredItem<ItemCultistRobeArmor> ITEM_CHESTPLATE_CULTISTROBE =
            ModItems.ITEMS.register("itemchestplatecultistrobe",
                    () -> new ItemCultistRobeArmor(ArmorMaterials.IRON, Type.CHESTPLATE));
    public static final DeferredItem<ItemCultistRobeArmor> ITEM_LEGGINGS_CULTISTROBE =
            ModItems.ITEMS.register("itemleggingscultistrobe",
                    () -> new ItemCultistRobeArmor(ArmorMaterials.IRON, Type.LEGGINGS));
    public static final DeferredItem<ItemCultistBoots> ITEM_BOOTS_CULTIST =
            ModItems.ITEMS.register("itembootscultist",
                    () -> new ItemCultistBoots(ArmorMaterials.IRON, Type.BOOTS));
    public static final DeferredItem<ItemCultistPlateArmor> ITEM_HELMET_CULTISTPLATE =
            ModItems.ITEMS.register("itemhelmetcultistplate",
                    () -> new ItemCultistPlateArmor(ArmorMaterials.IRON, Type.HELMET));
    public static final DeferredItem<ItemCultistPlateArmor> ITEM_CHESTPLATE_CULTISTPLATE =
            ModItems.ITEMS.register("itemchestplatecultistplate",
                    () -> new ItemCultistPlateArmor(ArmorMaterials.IRON, Type.CHESTPLATE));
    public static final DeferredItem<ItemCultistPlateArmor> ITEM_LEGGINGS_CULTISTPLATE =
            ModItems.ITEMS.register("itemleggingscultistplate",
                    () -> new ItemCultistPlateArmor(ArmorMaterials.IRON, Type.LEGGINGS));

    // ---- Crimson praetor set (rows 291-293; fortress material) ---------------
    public static final DeferredItem<ItemCultistLeaderArmor> ITEM_HELMET_CULTISTLEADERPLATE =
            ModItems.ITEMS.register("itemhelmetcultistleaderplate",
                    () -> new ItemCultistLeaderArmor(FORTRESS, Type.HELMET));
    public static final DeferredItem<ItemCultistLeaderArmor> ITEM_CHESTPLATE_CULTISTLEADERPLATE =
            ModItems.ITEMS.register("itemchestplatecultistleaderplate",
                    () -> new ItemCultistLeaderArmor(FORTRESS, Type.CHESTPLATE));
    public static final DeferredItem<ItemCultistLeaderArmor> ITEM_LEGGINGS_CULTISTLEADERPLATE =
            ModItems.ITEMS.register("itemleggingscultistleaderplate",
                    () -> new ItemCultistLeaderArmor(FORTRESS, Type.LEGGINGS));

    /**
     * The original's EnumHelper equip sound default (generic armor equip);
     * toughness/knockback are the EnumHelper 4-arg defaults (0). Layer
     * textures follow the original's ArmorMaterial name fallback
     * ({@code textures/models/armor/<name>_layer_<1|2>}); the art itself
     * ships in the ARR overlay (D-004). The robe material is dyeable
     * (original {@code IDyeableArmorItem}).
     */
    private static ArmorMaterial material(final String name,
                                         final java.util.Map<Type, Integer> defense,
                                         final int enchantability, final boolean dyeable) {
        return new ArmorMaterial(defense, enchantability,
                net.minecraft.core.Holder.direct(SoundEvents.ARMOR_EQUIP_GENERIC),
                () -> Ingredient.EMPTY,
                java.util.List.of(new ArmorMaterial.Layer(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                                "textures/models/armor/" + name), "", dyeable)),
                0.0F, 0.0F);
    }

    /**
     * Registers the holder's material registry with the mod bus and wires the
     * goggle revealers (TC4 {@code IRevealer} on the fortress and
     * void-fortress helms — {@code ItemFortressArmor.showNodes} =
     * hasGoggles, {@code ItemVoidRobeArmor.showNodes} = true; the renderer
     * only consults the helmet slot). Item entries live in the shared
     * {@code ModItems} holder and need no bus wiring of their own.
     */
    public static void init(final IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            NodeReveal.registerRevealer(ITEM_HELMET_FORTRESS.get(),
                    (stack, wearer) -> ItemFortressArmor.hasGoggles(stack));
            NodeReveal.registerRevealer(ITEM_HELMET_VOIDFORTRESS.get(), (stack, wearer) -> true);
        }));
    }

    private M607RunicArmor() {
    }
}
