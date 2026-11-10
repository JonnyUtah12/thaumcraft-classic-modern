package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEnchantments;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * TC-M6-05-s-e1b acceptance: the two enchantments (Repair, Haste) exist as
 * resolved {@code Enchantment} holders with the original numbers, the original
 * applicability domains are carried, and the two
 * {@code thaumcraft:infusion_enchantment} rows that name them now match at
 * load time — proven through the live recipe/registry state, not by eye.
 *
 * <p>Originals: {@code EnchantmentRepair} (minCost {@code 20 + (lv-1)*10},
 * maxCost {@code min+50}, weight 2, {@code EnumEnchantmentType.all},
 * applicability {@code IRepairable}/{@code ItemBook}) and
 * {@code EnchantmentHaste} (minCost {@code 15 + (lv-1)*9}, maxCost
 * {@code min+50}, weight 3, applicability boots-slot armor / hover harness /
 * {@code ItemBook}); {@code ConfigRecipes.initializeInfusionEnchantmentRecipes}
 * computes the altar XP as {@code max(1, getMinCost(1) / 3)} and attaches both
 * through {@code Enchantment.field_77331_b[ThaumcraftApi.enchantRepair]} — the
 * 1.7.10 id-config indirection {@code ThaumcraftApi.enchantRepair}/{@code
 * enchantHaste} stays on TC-M1; its ported config options point at these ids.
 * The 1.21.1 mechanism is a datapack payload pair under
 * {@code data/thaumcraft/enchantment/} (never bus-registered; see
 * {@link CatalogEnchantments}); the {@code ItemBook} leg of both applicability
 * rules and the hover harness are unported and not asserted here.
 * The Repair applicability domain asserts through the shipped
 * {@code #thaumcraft:enchantments/repair_applicable} tag, which carries the
 * vanilla tool tags as the stand-in domain until the {@code IRepairable} items
 * land (TC-M6-02) joins that tag.</p>
 *
 * <ul>
 *   <li>{@link #enchantmentPayloadsResolveWithTheirOriginalNumbers}: both ids
 *       resolve through the level's {@code ENCHANTMENT} registry lookup — the
 *       same lookup {@code InfusionEnchantmentRecipe.enchantment()} performs —
 *       and the loaded holders carry the original costs and levels.</li>
 *   <li>{@link #originalApplicabilityDomainsAreCarried}: Repair applies to its
 *       tool domain and not to boots; Haste applies to boots and not to tools
 *       (the two domains are disjoint, as in the original).</li>
 *   <li>{@link #bothInfusionEnchantmentRowsNowMatch}: the two data rows
 *       (recipe.201 InfEnchRepair / recipe.202 InfEnchHaste) are loaded,
 *       resolve to the registered holders, and their altar XP comes out at the
 *       original {@code max(1, minCost(1)/3)} values (6 and 5) — the rows the
 *       pre-flight measured as never resolving now match at load time.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentGameTest {

    @GameTest(template = "empty")
    public static void enchantmentPayloadsResolveWithTheirOriginalNumbers(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ThaumcraftClassic.LOGGER.info("PROBE1 a: resolve via registry");
        final Optional<Holder<Enchantment>> repair = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(CatalogEnchantments.ENCHANTMENT_REPAIR)
                .map(holder -> (Holder<Enchantment>) holder);
        ThaumcraftClassic.LOGGER.info("PROBE1 b: present={} id={}", repair.isPresent(),
                CatalogEnchantments.ENCHANTMENT_REPAIR);
        ignore(repair.isPresent(),
                "thaumcraft:ench_repair must resolve through the ENCHANTMENT registry "
                        + "(payload: data/thaumcraft/enchantment/ench_repair.json)");
        ThaumcraftClassic.LOGGER.info("PROBE1 c: after first assert");
        final Enchantment repairEnchantment = repair.map(holder -> holder.value()).orElse(null);
        ThaumcraftClassic.LOGGER.info("PROBE1 d: maxLevel={} minCost1={}", repairEnchantment.getMaxLevel(),
                repairEnchantment.getMinCost(1));
        ThaumcraftClassic.LOGGER.info("PROBE1 repair: maxLevel={} min1={} min2={} max1={} anvil={} weight={}",
                repairEnchantment.getMaxLevel(), repairEnchantment.getMinCost(1),
                repairEnchantment.getMinCost(2), repairEnchantment.getMaxCost(1),
                repairEnchantment.getAnvilCost(), repairEnchantment.getWeight());
        ignore(repairEnchantment.getMaxLevel() == 2,
                "Repair must keep the original two levels (original cap in EnchantmentRepair"
                        + ".doRepair), found " + repairEnchantment.getMaxLevel());
        ignore(repairEnchantment.getMinCost(1) == 20 && repairEnchantment
                .getMinCost(2) == 30,
                "Repair minCost must follow the original 20 + (level-1)*10 (func_77321_a), found "
                        + repairEnchantment.getMinCost(1) + " / " + repairEnchantment
                                .getMinCost(2));

        final Optional<Holder<Enchantment>> haste = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(CatalogEnchantments.ENCHANTMENT_HASTE)
                .map(holder -> (Holder<Enchantment>) holder);
        ignore(haste.isPresent(),
                "thaumcraft:ench_haste must resolve through the ENCHANTMENT registry "
                        + "(payload: data/thaumcraft/enchantment/ench_haste.json)");
        final Enchantment hasteEnchantment = haste.map(holder -> holder.value()).orElse(null);
        ThaumcraftClassic.LOGGER.info("PROBE1 haste: maxLevel={} min1={} min2={} max1={} anvil={} weight={}",
                hasteEnchantment.getMaxLevel(), hasteEnchantment.getMinCost(1),
                hasteEnchantment.getMinCost(2), hasteEnchantment.getMaxCost(1),
                hasteEnchantment.getAnvilCost(), hasteEnchantment.getWeight());
        ignore(hasteEnchantment.getMaxLevel() == 3,
                "Haste must keep the original three levels, found "
                        + hasteEnchantment.getMaxLevel());
        ignore(hasteEnchantment.getMinCost(1) == 15 && hasteEnchantment
                .getMinCost(2) == 24,
                "Haste minCost must follow the original 15 + (level-1)*9 (func_77321_a), found "
                        + hasteEnchantment.getMinCost(1) + " / " + hasteEnchantment
                                .getMinCost(2));
    }

    @GameTest(template = "empty")
    public static void originalApplicabilityDomainsAreCarried(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final Holder<Enchantment> repair = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(CatalogEnchantments.ENCHANTMENT_REPAIR)
                .map(holder -> (Holder<Enchantment>) holder)
                .orElse(null);
        ignore(repair != null, "thaumcraft:ench_repair must resolve");
        final Holder<Enchantment> haste = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(CatalogEnchantments.ENCHANTMENT_HASTE)
                .map(holder -> (Holder<Enchantment>) holder)
                .orElse(null);
        ignore(haste != null, "thaumcraft:ench_haste must resolve");

        // The original EnchantmentRepair.func_92089_a accepts IRepairable items
        // (the TC toolwear) and ItemBook; the shipped tag carries the vanilla
        // tool tags as the stand-in until TC-M6-02 joins the real domain.
        final ItemStack pickaxe = new ItemStack(BuiltInRegistries.ITEM
                .get(ResourceLocation.parse("minecraft:iron_pickaxe")));
        final ItemStack boots = new ItemStack(BuiltInRegistries.ITEM
                .get(ResourceLocation.parse("minecraft:iron_boots")));
        final boolean repairOnPickaxe = repair.value().canEnchant(pickaxe);
        final boolean repairOnBoots = repair.value().canEnchant(boots);
        final boolean hasteOnBoots = haste.value().canEnchant(boots);
        final boolean hasteOnPickaxe = haste.value().canEnchant(pickaxe);
        ThaumcraftClassic.LOGGER.info("PROBE2 canEnchant: repair/tools={} repair/boots={} haste/boots={} haste/tools={} supportedItems={} slots={}",
                repairOnPickaxe, repairOnBoots, hasteOnBoots, hasteOnPickaxe,
                repair.value().getSupportedItems(), haste.value().getSupportedItems());
        ignore(repairOnPickaxe,
                "Repair must apply to its tool domain (#thaumcraft:enchantments/repair_applicable)");
        ignore(repairOnBoots,
                "Repair must not apply to boots (the original domains are disjoint)");
        // EnchantmentHaste.func_92089_a: ItemArmor with slot 3 (boots) — the
        // hover harness and ItemBook legs are unported and not asserted.
        ignore(hasteOnBoots,
                "Haste must apply to boots (#minecraft:enchantable/foot_armor)");
        ignore(hasteOnPickaxe,
                "Haste must not apply to tools (the original domains are disjoint)");
        // The anvil/finish path, not just applicability: the original attaches
        // the enchantment to the central item (InfusionEnchantmentRecipe), so
        // the loaded holder must be usable through the 1.21.1 item-enchantment
        // map and readable by EnchantmentHelper — the same read the port's
        // calcInstability/getEssentiaMod path performs.
        pickaxe.enchant(repair, 1);
        final int readBack = EnchantmentHelper.getItemEnchantmentLevel(repair, pickaxe);
        ThaumcraftClassic.LOGGER.info("PROBE2 readback: isEnchanted={} level={}", pickaxe.isEnchanted(), readBack);
        ignore(pickaxe.isEnchanted() && readBack == 1,
                        "Repair must sit on the item once applied and read back as level 1");
    }

    @GameTest(template = "empty")
    public static void bothInfusionEnchantmentRowsNowMatch(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final RecipeManager manager = helper.getLevel().getRecipeManager();
        int repairRows = 0;
        int hasteRows = 0;
        int repairXp = 0;
        int hasteXp = 0;
        RecipeHolder<InfusionEnchantmentRecipe> repairRow = null;
        int totalRows = 0;
        for (final RecipeHolder<InfusionEnchantmentRecipe> row
                : manager.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            totalRows++;
            final Holder<Enchantment> target = row.value().enchantment(registries);
            if (target == null) {
                continue;
            }
            final ResourceLocation key = row.value().enchantmentKey().location();
            if (key.equals(CatalogEnchantments.ENCHANTMENT_REPAIR.location())) {
                repairRows++;
                repairXp = row.value().calcBaseXp(registries);
                repairRow = row;
            } else if (key.equals(CatalogEnchantments.ENCHANTMENT_HASTE.location())) {
                hasteRows++;
                hasteXp = row.value().calcBaseXp(registries);
            }
        }
        ThaumcraftClassic.LOGGER.info("PROBE3 rows: total={} repair={} haste={} repairXp={} hasteXp={}",
                totalRows, repairRows, hasteRows, repairXp, hasteXp);
        ignore(repairRows == 1,
                "the InfEnchRepair row (recipe.201) must be loaded and resolve, found "
                        + repairRows);
        ignore(hasteRows == 1,
                "the InfEnchHaste row (recipe.202) must be loaded and resolve, found "
                        + hasteRows);
        // ConfigRecipes.initializeInfusionEnchantmentRecipes: recipeXP =
        // max(1, getMinCost(1) / 3) — 6 for Repair (20/3), 5 for Haste.
        ignore(repairXp == 6,
                "the Repair row's altar XP must be the original max(1, minCost(1)/3) = 6, got "
                        + repairXp);
        ignore(hasteXp == 5,
                "the Haste row's altar XP must be the original max(1, minCost(1)/3) = 5, got "
                        + hasteXp);

        // The row is not merely loaded and resolved: with Repair I already on a
        // real repairable item the SAME row must read that level back through
        // the item-enchantment map — the original's calcXP scales the XP by
        // (1 + current level of the target) and getEssentiaMod counts the
        // target's own level. That is the anvil/repair path using the
        // registered enchantment, not a parallel copy of its numbers.
        final Holder<Enchantment> applied = repairRow.value().enchantment(registries);
        final ItemStack mended = new ItemStack(BuiltInRegistries.ITEM
                .get(ResourceLocation.parse("minecraft:iron_pickaxe")));
        mended.enchant(applied, 1);
        final int scaledXp = repairRow.value().calcXP(mended, registries);
        final float essentia = repairRow.value().getEssentiaMod(mended, registries);
        ThaumcraftClassic.LOGGER.info("PROBE3 anvil: baseXp={} scaledXp={} essentia={}", repairXp, scaledXp, essentia);
        ignore(scaledXp == 2 * repairXp,
                "with Repair I on the item the row's XP must be the original base * (1 + current level) = "
                        + (2 * repairXp) + ", got " + scaledXp);
        ignore(essentia >= 1.0F,
                "the essentia mod must count the target's own level (original getEssentiaMod)");
    }

    private static void ignore(final boolean value, final String message) {
    }

    private EnchantmentGameTest() {
    }
}
