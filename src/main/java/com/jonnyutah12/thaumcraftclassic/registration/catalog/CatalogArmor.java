package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.items.armor.ArmorSetRules;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemGoggles;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemRobeArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemThaumiumArmor;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Thaumic armour registration (TC-M6-07-s): the eight kind=item rows
 * docs/content-mapping.csv:213-217 and 236-238 ({@code ItemGoggles}, the four
 * thaumium pieces, the three robe pieces), plus the two armour materials the
 * original registers for them.
 *
 * <h2>Materials are real registry entries</h2>
 * {@code minecraft:armor_material} is a built-in registry in 1.21.1
 * ({@code BuiltInRegistries.ARMOR_MATERIAL = registerSimple(...)}), not the
 * datapack registry it becomes in 1.21.4, so the two 1.7.10
 * {@code EnumHelper.addArmorMaterial} rows become two
 * {@code DeferredRegister} entries here. NeoForge orders the registry events so
 * ARMOR_MATERIAL fires before ITEM ("ArmorItem depends on armor materials at
 * construction time", {@code GameData:130}), so {@code ArmorItem} constructors
 * can take these holders directly.
 *
 * <p>The layer asset ids point at {@code textures/models/armor/<id>_layer_1.png}
 * (1.21.1's layout); the textures themselves are TC-M7-01's, so a live
 * {@code /give} shows the missing-model pattern until that card lands — the
 * behaviour, the defence table and the durability do not depend on them.</p>
 *
 * <p>Out of scope, each its own card: the boots of the traveller and the
 * thaumostatic harness (+ its container) TC-M6-07b, the fortress and void
 * families (+ their runic upgrade flags) TC-M6-07b, the crimson cult armour
 * (it belongs to the cultist entities, TC-M6-06), the robe dye recipe and the
 * cauldron colour scrub TC-M6-07b, armour models and textures TC-M7-01.</p>
 */
public final class CatalogArmor {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumcraftClassic.MOD_ID);

    /**
     * {@code ThaumcraftApi.armorMatThaumium} =
     * {@code addArmorMaterial("THAUMIUM", 25, {2, 6, 5, 2}, 25)}
     * ({@code api/ThaumcraftApi.java:37}).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> THAUMIUM =
            ARMOR_MATERIALS.register("thaumium", () -> new ArmorMaterial(
                    ArmorSetRules.defenseMap(ArmorSetRules.THAUMIUM_DEFENSE_HELMET,
                            ArmorSetRules.THAUMIUM_DEFENSE_CHESTPLATE,
                            ArmorSetRules.THAUMIUM_DEFENSE_LEGGINGS,
                            ArmorSetRules.THAUMIUM_DEFENSE_BOOTS),
                    ArmorSetRules.THAUMIUM_ENCHANTABILITY,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ItemResource.stack(
                            ArmorSetRules.REPAIR_META_THAUMIUM_INGOT)),
                    List.of(new ArmorMaterial.Layer(layerAsset("thaumium"))),
                    0.0F, 0.0F));

    /**
     * {@code ThaumcraftApi.armorMatSpecial} =
     * {@code addArmorMaterial("SPECIAL", 25, {1, 3, 2, 1}, 25)}
     * ({@code api/ThaumcraftApi.java:38}) — the material behind both the robe
     * and the goggles. Dyeable layer: the original's robe is the dyed family
     * ({@code getColor}/{@code setColor}/{@code clearColor}, and the goggles
     * ignore the tint, exactly as vanilla reads one flag per material layer).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPECIAL =
            ARMOR_MATERIALS.register("special", () -> new ArmorMaterial(
                    ArmorSetRules.defenseMap(ArmorSetRules.SPECIAL_DEFENSE_HELMET,
                            ArmorSetRules.SPECIAL_DEFENSE_CHESTPLATE,
                            ArmorSetRules.SPECIAL_DEFENSE_LEGGINGS,
                            ArmorSetRules.SPECIAL_DEFENSE_BOOTS),
                    ArmorSetRules.SPECIAL_ENCHANTABILITY,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ItemResource.stack(
                            ArmorSetRules.REPAIR_META_ENCHANTED_FABRIC)),
                    List.of(new ArmorMaterial.Layer(layerAsset("special"), "", true)),
                    0.0F, 0.0F));

    // ------------------------------------------------------------- thaumium set

    public static final DeferredItem<ItemThaumiumArmor> ITEM_HELMET_THAUMIUM =
            ModItems.ITEMS.register("itemhelmetthaumium", () -> new ItemThaumiumArmor(
                    THAUMIUM, ArmorItem.Type.HELMET,
                    armorProps(ArmorItem.Type.HELMET, ArmorSetRules.THAUMIUM_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ItemThaumiumArmor> ITEM_CHESTPLATE_THAUMIUM =
            ModItems.ITEMS.register("itemchestplatethaumium", () -> new ItemThaumiumArmor(
                    THAUMIUM, ArmorItem.Type.CHESTPLATE,
                    armorProps(ArmorItem.Type.CHESTPLATE, ArmorSetRules.THAUMIUM_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ItemThaumiumArmor> ITEM_LEGGINGS_THAUMIUM =
            ModItems.ITEMS.register("itemleggingsthaumium", () -> new ItemThaumiumArmor(
                    THAUMIUM, ArmorItem.Type.LEGGINGS,
                    armorProps(ArmorItem.Type.LEGGINGS, ArmorSetRules.THAUMIUM_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ItemThaumiumArmor> ITEM_BOOTS_THAUMIUM =
            ModItems.ITEMS.register("itembootsthaumium", () -> new ItemThaumiumArmor(
                    THAUMIUM, ArmorItem.Type.BOOTS,
                    armorProps(ArmorItem.Type.BOOTS, ArmorSetRules.THAUMIUM_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    // -------------------------------------------------------------- robe pieces

    public static final DeferredItem<ItemRobeArmor> ITEM_CHESTPLATE_ROBE =
            ModItems.ITEMS.register("itemchestplaterobe", () -> new ItemRobeArmor(
                    SPECIAL, ArmorItem.Type.CHESTPLATE,
                    armorProps(ArmorItem.Type.CHESTPLATE, ArmorSetRules.SPECIAL_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ItemRobeArmor> ITEM_LEGGINGS_ROBE =
            ModItems.ITEMS.register("itemleggingsrobe", () -> new ItemRobeArmor(
                    SPECIAL, ArmorItem.Type.LEGGINGS,
                    armorProps(ArmorItem.Type.LEGGINGS, ArmorSetRules.SPECIAL_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ItemRobeArmor> ITEM_BOOTS_ROBE =
            ModItems.ITEMS.register("itembootsrobe", () -> new ItemRobeArmor(
                    SPECIAL, ArmorItem.Type.BOOTS,
                    armorProps(ArmorItem.Type.BOOTS, ArmorSetRules.SPECIAL_DURABILITY_FACTOR)
                            .rarity(Rarity.UNCOMMON)));

    // ------------------------------------------------------------------ goggles

    /**
     * {@code ItemGoggles} overrides the material's per-slot durability with
     * {@code setMaxDamage(350)} (constructor, {@code ItemGoggles.java:29-31}), so
     * this piece carries 350 and not {@code 25 * 11 = 275}.
     */
    public static final DeferredItem<ItemGoggles> ITEM_GOGGLES =
            ModItems.ITEMS.register("itemgoggles", () -> new ItemGoggles(
                    SPECIAL, ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(ArmorSetRules.GOGGLES_MAX_DAMAGE)
                            .rarity(Rarity.RARE)));

    /**
     * Armour is unstackable and every piece takes the 1.7.10
     * {@code durabilityFactor * {11,16,15,13}[slot]} damage budget. Not
     * {@code setNoRepair}: the original's armour repairs in an anvil, per the
     * per-class repair item.
     */
    private static Item.Properties armorProps(final ArmorItem.Type type, final int durabilityFactor) {
        return new Item.Properties()
                .stacksTo(1)
                .durability(ArmorSetRules.maxDamage(type, durabilityFactor));
    }

    private static ResourceLocation layerAsset(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    /** Forces class initialization (entry registration) from {@code ModRegistration}. */
    public static void init(final IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }

    private CatalogArmor() {
    }
}
