package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.items.armor.ArmorSetRules;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemGoggles;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemRobeArmor;
import com.jonnyutah12.thaumcraftclassic.items.armor.ItemThaumiumArmor;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemRingRunic;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeReveal;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArmor;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogBaubles;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.runic.RunicShielding;
import com.jonnyutah12.thaumcraftclassic.wands.WornGearDiscounts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-07-s acceptance: the thaumic armour set and its equipment effects in
 * the live registry and on a live player.
 *
 * <p>Every probe pins one behaviour of the original
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/items/armor/} and
 * {@code api/ThaumcraftApi.java}) against the merged seams, so an integration
 * that drops a piece, a table, an interface, a tooltip key, a discount chain
 * participant or the reveal gate fails here:</p>
 * <ol>
 *   <li>the eight ids and the two armour materials resolve in the live
 *   registries with the original defence, durability and enchantability
 *   numbers, the robe layer dyeable and the goggles at their own 350;</li>
 *   <li>the thaumium set defends 2/6/5/2, is uncommon, and anvil-repairs with
 *   the thaumium ingot — and only with meta 2 of {@code itemResource};</li>
 *   <li>the robe (2/2/1) and the goggles (5) sum through the live
 *   {@code wands/WornGearDiscounts} chain the wand cast reads, and the
 *   original's {@code tc.visdiscount} tooltip line rides along;</li>
 *   <li>the armour slots join {@code runic/RunicShielding}'s scan and
 *   contribute exactly zero charge next to a runic ring;</li>
 *   <li>the goggles reveal nodes through the {@code IRevealer} contract —
 *   surviving {@code NodeReveal.clear()}, which a registry-map-only wiring
 *   would not;</li>
 *   <li>the robe colour round-trips on the {@code thaumcraft:armor_color}
 *   component with the original's 6961280 default.</li>
 * </ol>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmorEquipmentGameTest {

    private static final EquipmentSlot[] ARMOUR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /**
     * The original's armour table, in slot order:
     * {@code armorMatThaumium = addArmorMaterial("THAUMIUM", 25, {2, 6, 5, 2}, 25)}
     * ({@code api/ThaumcraftApi.java:37}) with 1.7.10's per-slot durability
     * factors {11, 16, 15, 13}.
     */
    private static final int[] THAUMIUM_DEFENCE = {2, 6, 5, 2};
    private static final int[] DYE_FACTORS = {11, 16, 15, 13};

    /**
     * Probe 1: registry presence, and the shipped materials read back from the
     * live {@code minecraft:armor_material} registry with the original's
     * numbers — the table {@code ArmorMaterialParityTest} pins to the original
     * source, pinned here to what the game actually loaded.
     */
    @GameTest(template = "empty")
    public static void thaumicArmorIdsAndMaterialsResolve(final GameTestHelper helper) {
        final Item goggles = CatalogArmor.ITEM_GOGGLES.get();
        final Item[] thaumium = {
            CatalogArmor.ITEM_HELMET_THAUMIUM.get(), CatalogArmor.ITEM_CHESTPLATE_THAUMIUM.get(),
            CatalogArmor.ITEM_LEGGINGS_THAUMIUM.get(), CatalogArmor.ITEM_BOOTS_THAUMIUM.get() };
        final Item[] robe = {
            CatalogArmor.ITEM_CHESTPLATE_ROBE.get(), CatalogArmor.ITEM_LEGGINGS_ROBE.get(),
            CatalogArmor.ITEM_BOOTS_ROBE.get() };
        for (final Item piece : thaumium) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(piece)),
                    "thaumium piece " + BuiltInRegistries.ITEM.getKey(piece) + " is not registered");
        }
        for (final Item piece : robe) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(piece)),
                    "robe piece " + BuiltInRegistries.ITEM.getKey(piece) + " is not registered");
        }
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(goggles)),
                "goggles are not registered");
        // The csv owns these ids; pin that the names the game serves are the mapped ones.
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(goggles)
                        .equals(ResourceLocation.parse("thaumcraft:itemgoggles")),
                "goggles id is " + BuiltInRegistries.ITEM.getKey(goggles));

        final ArmorMaterial mat = BuiltInRegistries.ARMOR_MATERIAL.get(ResourceLocation.parse("thaumcraft:thaumium"));
        final ArmorMaterial special =
                BuiltInRegistries.ARMOR_MATERIAL.get(ResourceLocation.parse("thaumcraft:special"));
        helper.assertTrue(mat != null && special != null,
                "thaumcraft:thaumium / thaumcraft:special must be registered armour materials");

        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            final ArmorItem piece = (ArmorItem) thaumium[slot];
            helper.assertTrue(piece.getDefense() == THAUMIUM_DEFENCE[slot],
                    "thaumium " + piece.getType() + " defence " + piece.getDefense()
                            + " != original " + THAUMIUM_DEFENCE[slot]);
            helper.assertTrue(mat.getDefense(piece.getType()) == THAUMIUM_DEFENCE[slot],
                    "live thaumium material defence for " + piece.getType() + " is wrong");
            helper.assertTrue(mat.enchantmentValue() == ArmorSetRules.THAUMIUM_ENCHANTABILITY,
                    "thaumium enchantability " + mat.enchantmentValue() + " != original 25");
            helper.assertTrue(new ItemStack(piece).getMaxDamage()
                            == ArmorSetRules.THAUMIUM_DURABILITY_FACTOR * DYE_FACTORS[slot],
                    "thaumium " + piece.getType() + " durability " + new ItemStack(piece).getMaxDamage()
                            + " != 25 x " + DYE_FACTORS[slot]);
            helper.assertTrue(piece.getToughness() == 0.0F && mat.toughness() == 0.0F,
                    "1.7.10 has no armour toughness; thaumium must carry none");
        }

        // Robe: the SPECIAL material (1/3/2/1), and its layer is the dyeable one.
        helper.assertTrue(((ArmorItem) robe[0]).getDefense() == 3
                        && ((ArmorItem) robe[1]).getDefense() == 2
                        && ((ArmorItem) robe[2]).getDefense() == 1,
                "robe defence must be the original armorMatSpecial chest/legs/boots 3/2/1");
        helper.assertTrue(special.enchantmentValue() == ArmorSetRules.SPECIAL_ENCHANTABILITY,
                "special enchantability " + special.enchantmentValue() + " != original 25");
        helper.assertTrue(special.layers().get(0).dyeable(),
                "the robe is the dyed family: its material layer must be dyeable");
        helper.assertTrue(!mat.layers().get(0).dyeable(),
                "thaumium armour is not dyeable in the original");
        // Robe durability rides the same 25 factor (ItemRobeArmor has no override).
        helper.assertTrue(new ItemStack(robe[0]).getMaxDamage() == 25 * 16
                        && new ItemStack(robe[1]).getMaxDamage() == 25 * 15
                        && new ItemStack(robe[2]).getMaxDamage() == 25 * 13,
                "robe durability must be 25 x {16, 15, 13}");

        // Goggles: the original overrides the material durability with 350.
        helper.assertTrue(new ItemStack(goggles).getMaxDamage() == ArmorSetRules.GOGGLES_MAX_DAMAGE,
                "goggles max damage " + new ItemStack(goggles).getMaxDamage() + " != original 350");
        helper.assertTrue(((ArmorItem) goggles).getDefense() == 1,
                "goggles sit on armorMatSpecial, whose helmet defence is the original 1");

        // Rarity is a data component in 1.21.1 (the original's EnumRarity rows).
        helper.assertTrue(new ItemStack(goggles).getRarity() == Rarity.RARE,
                "goggles rarity must be rare (ItemGoggles.java:59-60)");
        helper.assertTrue(new ItemStack(thaumium[0]).getRarity() == Rarity.UNCOMMON
                        && new ItemStack(robe[0]).getRarity() == Rarity.UNCOMMON,
                "thaumium and robe armour are uncommon (ItemThaumiumArmor.java:58-59, ItemRobeArmor.java:119-120)");

        helper.succeed();
    }

    /**
     * Probe 2: the thaumium set's own rules — anvil repair is the thaumium
     * ingot and precisely that {@code itemResource} meta, the runic charge is
     * the original's 0, and the pieces occupy their four equipment slots.
     */
    @GameTest(template = "empty")
    public static void thaumiumSetRepairsWithItsOwnIngotAndCarriesZeroRunicCharge(
            final GameTestHelper helper) {
        final ItemThaumiumArmor chest = CatalogArmor.ITEM_CHESTPLATE_THAUMIUM.get();
        final ItemStack chestStack = new ItemStack(chest);
        final Item thaumiumIngot = CatalogResources.ITEM_RESOURCE.get().stack(
                ArmorSetRules.REPAIR_META_THAUMIUM_INGOT).getItem();
        helper.assertTrue(thaumiumIngot == CatalogResources.ITEM_RESOURCE.get(),
                "the thaumium ingot is itemResource meta 2");
        helper.assertTrue(chest.isValidRepairItem(chestStack,
                        ItemResource.stack(ArmorSetRules.REPAIR_META_THAUMIUM_INGOT)),
                "thaumium armour must anvil-repair with the thaumium ingot (ItemThaumiumArmor.java:62-63)");
        helper.assertTrue(!chest.isValidRepairItem(chestStack,
                        ItemResource.stack(ArmorSetRules.REPAIR_META_ENCHANTED_FABRIC)),
                "the same item at meta 7 (enchanted fabric) must NOT repair thaumium armour");
        helper.assertTrue(!chest.isValidRepairItem(chestStack, new ItemStack(Items.IRON_INGOT)),
                "thaumium armour does not repair with iron");
        helper.assertTrue(chest.getRunicCharge(chestStack) == ArmorSetRules.RUNIC_CHARGE_UNUPGRADED,
                "un-upgraded thaumium armour has runic charge 0 (ItemThaumiumArmor.java:29-30)");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            player.setItemSlot(ARMOUR_SLOTS[slot], new ItemStack(thaumiumPiece(slot)));
            helper.assertTrue(player.getItemBySlot(ARMOUR_SLOTS[slot]).getItem() == thaumiumPiece(slot),
                    "thaumium " + ARMOUR_SLOTS[slot] + " did not equip in that slot");
        }
        helper.succeed();
    }

    /**
     * Probe 3: the equipment effect the set exists for — the vis discount the
     * wand cast reads. Robe chest/legs/boots 2/2/1 (the original's
     * {@code armorType == 3 ? 1 : 2}) and goggles 5, summed by the live
     * {@code WornGearDiscounts.gearPercentTotal}; thaumium armour, which never
     * implemented {@code IVisDiscountGear}, adds nothing. The original's
     * {@code tc.visdiscount} tooltip line rides along.
     */
    @GameTest(template = "empty")
    public static void robeAndGogglesFeedTheLiveWandDiscountChain(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player robeOnly = helper.makeMockPlayer(GameType.SURVIVAL);
        robeOnly.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CatalogArmor.ITEM_CHESTPLATE_ROBE.get()));
        robeOnly.setItemSlot(EquipmentSlot.LEGS, new ItemStack(CatalogArmor.ITEM_LEGGINGS_ROBE.get()));
        robeOnly.setItemSlot(EquipmentSlot.FEET, new ItemStack(CatalogArmor.ITEM_BOOTS_ROBE.get()));
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(robeOnly, null) == 5,
                "full robe must discount 2 + 2 + 1 = 5 percent, got "
                        + WornGearDiscounts.gearPercentTotal(robeOnly, null));

        final Player robed = helper.makeMockPlayer(GameType.SURVIVAL);
        robed.setItemSlot(EquipmentSlot.HEAD, new ItemStack(CatalogArmor.ITEM_GOGGLES.get()));
        robed.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CatalogArmor.ITEM_CHESTPLATE_ROBE.get()));
        robed.setItemSlot(EquipmentSlot.LEGS, new ItemStack(CatalogArmor.ITEM_LEGGINGS_ROBE.get()));
        robed.setItemSlot(EquipmentSlot.FEET, new ItemStack(CatalogArmor.ITEM_BOOTS_ROBE.get()));
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(robed, null) == 10,
                "goggles 5 + robe 5 = 10 percent, got " + WornGearDiscounts.gearPercentTotal(robed, null));

        // Thaumium armour is not IVisDiscountGear in the original: it discounts 0.
        final Player armoured = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            armoured.setItemSlot(ARMOUR_SLOTS[slot], new ItemStack(thaumiumPiece(slot)));
        }
        helper.assertTrue(!(CatalogArmor.ITEM_CHESTPLATE_THAUMIUM.get()
                        instanceof com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear),
                "thaumium armour must not implement IVisDiscountGear (the original does not)");
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(armoured, null) == 0,
                "a full thaumium set discounts nothing (got "
                        + WornGearDiscounts.gearPercentTotal(armoured, null) + ")");
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(
                        helper.makeMockPlayer(GameType.SURVIVAL), null) == 0,
                "bare armour slots discount nothing");

        // The tooltip line, through the live component stack (untranslated here:
        // the game test server has no lang file loaded, so the key is literal).
        final List<ItemStack> stacks = List.of(
                new ItemStack(CatalogArmor.ITEM_CHESTPLATE_ROBE.get()),
                new ItemStack(CatalogArmor.ITEM_BOOTS_ROBE.get()),
                new ItemStack(CatalogArmor.ITEM_GOGGLES.get()));
        final String[] expected = {"2%", "1%", "5%"};
        for (int i = 0; i < stacks.size(); i++) {
            final List<Item> seen = new ArrayList<>();
            final List<net.minecraft.network.chat.Component> lines = stacks.get(i)
                    .getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
            final String joined = lines.toString();
            helper.assertTrue(joined.contains("tc.visdiscount") && joined.contains(expected[i]),
                    "stack " + stacks.get(i).getItem() + " tooltip must carry the original"
                            + " tc.visdiscount line with " + expected[i] + ": " + joined);
            seen.add(stacks.get(i).getItem());
        }
        helper.succeed();
    }

    /**
     * Probe 4: the armour slots join the runic shielding scan (the original's
     * {@code EventHandlerRunic} walks {@code armorSlot} 0..3 plus the bauble
     * slots) and contribute exactly 0 charge, so a runic ring's 5 stays 5 and
     * a full thaumium set on its own opens no shielding pool.
     */
    @GameTest(template = "empty")
    public static void armourSlotsJoinTheRunicScanWithZeroCharge(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player armoured = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            armoured.setItemSlot(ARMOUR_SLOTS[slot], new ItemStack(thaumiumPiece(slot)));
        }
        helper.assertTrue(armoured.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof IRunicArmor,
                "the shipped chestpiece must implement IRunicArmor (the original's does)");
        helper.assertTrue(RunicShielding.scanGear(armoured).max() == 0,
                "four un-upgraded thaumium pieces (charge 0 each) open no shield pool, got "
                        + RunicShielding.scanGear(armoured).max());

        final Player geared = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < ARMOUR_SLOTS.length; slot++) {
            geared.setItemSlot(ARMOUR_SLOTS[slot], new ItemStack(thaumiumPiece(slot)));
        }
        geared.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CatalogArmor.ITEM_CHESTPLATE_ROBE.get()));
        geared.setItemSlot(EquipmentSlot.FEET, new ItemStack(CatalogArmor.ITEM_BOOTS_ROBE.get()));
        helper.assertTrue(AccessoryService.provider().equip(geared, AccessorySlot.RING, 0,
                        CatalogBaubles.ITEM_RING_RUNIC.get().stack(ItemRingRunic.META_GREATER)),
                "greater runic ring equip failed");
        helper.assertTrue(RunicShielding.scanGear(geared).max() == 5,
                "the ring's 5 must survive the armour scan (zero-charge pieces must not dilute it), got "
                        + RunicShielding.scanGear(geared).max());
        helper.succeed();
    }

    /**
     * Probe 5: the goggles' signature effect through the ported
     * {@code IRevealer} contract — the original's head-slot rule
     * ({@code TileNodeRenderer.java:187-189}) — and it survives
     * {@code NodeReveal.clear()}, because the interface is the source of truth,
     * not a registry map that a server stop can clear.
     */
    @GameTest(template = "empty")
    public static void gogglesRevealNodesThroughTheIRevealerContract(final GameTestHelper helper) {
        final ItemStack goggles = new ItemStack(CatalogArmor.ITEM_GOGGLES.get());
        helper.assertTrue(goggles.getItem() instanceof ItemGoggles, "goggles item type");
        final ItemGoggles item = (ItemGoggles) goggles.getItem();
        helper.assertTrue(item.showNodes(goggles, null),
                "the original's ItemGoggles.showNodes returns true (ItemGoggles.java:73-74)");
        helper.assertTrue(item.showIngamePopups(goggles, null),
                "the original's ItemGoggles.showIngamePopups returns true (ItemGoggles.java:78-79)");
        helper.assertTrue(NodeReveal.canSeeNodes(ItemStack.EMPTY, goggles, null),
                "goggles in the head slot must reveal nodes");
        helper.assertTrue(!NodeReveal.canSeeNodes(ItemStack.EMPTY,
                        new ItemStack(CatalogArmor.ITEM_CHESTPLATE_THAUMIUM.get()), null),
                "a thaumium helm reveals nothing (the original's ItemThaumiumArmor is not IRevealer)");
        helper.assertTrue(!NodeReveal.canSeeNodes(ItemStack.EMPTY, ItemStack.EMPTY, null),
                "a bare head reveals nothing");
        NodeReveal.clear();
        helper.assertTrue(NodeReveal.canSeeNodes(ItemStack.EMPTY, goggles, null),
                "the reveal rides the IRevealer contract, so NodeReveal.clear() must not drop it");
        helper.succeed();
    }

    /**
     * Probe 6: the robe's colour, on the port's {@code thaumcraft:armor_color}
     * component, with the original's 6961280 default and the cauldron scrub's
     * clear returning to it ({@code ItemRobeArmor.getColor}/{@code setColor}/
     * {@code clearColor}, lines 74-107).
     */
    @GameTest(template = "empty")
    public static void robeColourRoundTripsOnTheArmorColorComponent(final GameTestHelper helper) {
        final ItemRobeArmor robe = CatalogArmor.ITEM_CHESTPLATE_ROBE.get();
        final ItemStack stack = new ItemStack(robe);
        helper.assertTrue(robe.getColor(stack) == ArmorSetRules.ROBE_DEFAULT_COLOR,
                "an undyed robe is the original's 6961280 (0x6A3880), got " + robe.getColor(stack));
        helper.assertTrue(ArmorSetRules.ROBE_DEFAULT_COLOR == 6961280,
                "the default must be the original's decimal 6961280");
        robe.setColor(stack, 0x123456);
        helper.assertTrue(robe.getColor(stack) == 0x123456, "setColor must round-trip");
        robe.clearColor(stack);
        helper.assertTrue(robe.getColor(stack) == ArmorSetRules.ROBE_DEFAULT_COLOR,
                "clearColor must fall back to the original default");
        helper.succeed();
    }

    private static Item thaumiumPiece(final int slot) {
        return switch (slot) {
            case 0 -> CatalogArmor.ITEM_HELMET_THAUMIUM.get();
            case 1 -> CatalogArmor.ITEM_CHESTPLATE_THAUMIUM.get();
            case 2 -> CatalogArmor.ITEM_LEGGINGS_THAUMIUM.get();
            default -> CatalogArmor.ITEM_BOOTS_THAUMIUM.get();
        };
    }

    private ArmorEquipmentGameTest() {
    }
}
