package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smelting bonus registry — the 1.21.1 realization of the original
 * {@code ThaumcraftApi.addSmeltingBonus}/{@code getSmeltingBonus} pair
 * (content-mapping.csv {@code recipe.225..242.smelting_bonus}, populated by
 * the original's {@code ConfigRecipes} block at lines 4065-4082).
 *
 * <p>The arcane furnace (and only the arcane furnace — the original had no
 * other consumer) consults this when ejecting a smelt result: the bonus
 * stack starts at count 0 and the furnace's bellows logic grows it, exactly
 * as {@code TileArcaneFurnace.ejectItem}.</p>
 *
 * <h2>Key model</h2>
 * The original keyed entries by {@code (item, damage)} pairs and by raw
 * OreDictionary name strings, with a {@code damage=32767} wildcard. Here:
 * {@code (item, thaumcraft:item_meta)} pairs (the port's meta carrier,
 * {@link ModDataComponents#ITEM_META}) with a 32767 wildcard, and ore-dict
 * names as the {@code thaumcraft:oredict.*} item tags the catalog uses
 * (docs/catalog-triage.md oredict rule). Lookup order matches the original:
 * exact pair, wildcard pair, ore-dict tag.</p>
 */
public final class SmeltingBonuses {

    /** The original's wildcard damage value. */
    private static final int WILDCARD = 32767;

    private record ItemKey(Item item, int meta) {
    }

    private static final Map<ItemKey, ItemStack> BY_ITEM = new LinkedHashMap<>();
    private static final Map<TagKey<Item>, ItemStack> BY_TAG = new LinkedHashMap<>();

    /** Original {@code addSmeltingBonus(ItemStack in, ItemStack out)}. */
    public static void register(final ItemStack in, final ItemStack out) {
        BY_ITEM.put(new ItemKey(in.getItem(), metaOf(in)), bonusOf(out));
    }

    /**
     * Original {@code addSmeltingBonus(String oreDictName, ItemStack out)};
     * the name maps to the catalog's {@code thaumcraft:oredict.<name>} tag.
     */
    public static void registerOredict(final String oreDictName, final ItemStack out) {
        BY_TAG.put(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
                "thaumcraft", "oredict." + oreDictName.toLowerCase(java.util.Locale.ROOT))),
                bonusOf(out));
    }

    /**
     * Original {@code getSmeltingBonus(ItemStack in)}: exact pair, then the
     * 32767 wildcard, then the ore-dict name. Returns a count-0 copy (the
     * furnace grows it), or {@link ItemStack#EMPTY} when nothing matches.
     */
    public static ItemStack getSmeltingBonus(final ItemStack in) {
        if (in.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final int meta = metaOf(in);
        ItemStack out = BY_ITEM.get(new ItemKey(in.getItem(), meta));
        if (out == null) {
            out = BY_ITEM.get(new ItemKey(in.getItem(), WILDCARD));
        }
        if (out == null) {
            for (final Map.Entry<TagKey<Item>, ItemStack> entry : BY_TAG.entrySet()) {
                if (in.is(entry.getKey())) {
                    out = entry.getValue();
                    break;
                }
            }
        }
        return out == null ? ItemStack.EMPTY : out.copy();
    }

    private static ItemStack bonusOf(final ItemStack out) {
        // Original: new ItemStack(out.getItem(), 0, out.meta) — count 0.
        final ItemStack bonus = new ItemStack(out.getItem());
        bonus.setCount(0);
        final int meta = metaOf(out);
        if (meta != 0) {
            bonus.set(ModDataComponents.ITEM_META.get(), meta);
        }
        return bonus;
    }

    private static int metaOf(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * The original {@code ConfigRecipes} bonus block (CSV rows 225-242),
     * verbatim. Runs at common setup so every item holder is resolved.
     */
    public static void registerOriginalBonuses() {
        final Item nugget = item("itemnugget");
        // 225-231: ore-dict keyed.
        registerOredict("oreGold", new ItemStack(Items.GOLD_NUGGET));
        registerOredict("oreIron", nuggetStack(nugget, 0));
        registerOredict("oreCinnabar", nuggetStack(nugget, 5));
        registerOredict("oreCopper", nuggetStack(nugget, 1));
        registerOredict("oreTin", nuggetStack(nugget, 2));
        registerOredict("oreSilver", nuggetStack(nugget, 3));
        registerOredict("oreLead", nuggetStack(nugget, 4));
        // 232-238: nugget-item keyed (the original's "shard nugget" metas).
        register(nuggetStack(nugget, 31), new ItemStack(Items.GOLD_NUGGET));
        register(nuggetStack(nugget, 16), nuggetStack(nugget, 0));
        register(nuggetStack(nugget, 21), nuggetStack(nugget, 5));
        register(nuggetStack(nugget, 17), nuggetStack(nugget, 1));
        register(nuggetStack(nugget, 18), nuggetStack(nugget, 2));
        register(nuggetStack(nugget, 19), nuggetStack(nugget, 3));
        register(nuggetStack(nugget, 20), nuggetStack(nugget, 4));
        // 239-242: meat/fish. The original keyed fish with damage 32767
        // (any fish item); the wildcard covers cod/salmon/tropical/pufferfish.
        register(new ItemStack(Items.CHICKEN), item("itemnuggetchicken"));
        register(new ItemStack(Items.BEEF), item("itemnuggetbeef"));
        register(new ItemStack(Items.PORKCHOP), item("itemnuggetpork"));
        register(wildcard(Items.COD), item("itemnuggetfish"));
        register(wildcard(Items.SALMON), item("itemnuggetfish"));
        register(wildcard(Items.TROPICAL_FISH), item("itemnuggetfish"));
        register(wildcard(Items.PUFFERFISH), item("itemnuggetfish"));
    }

    private static ItemStack wildcard(final Item item) {
        final ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ITEM_META.get(), WILDCARD);
        return stack;
    }

    private static ItemStack nuggetStack(final Item nugget, final int meta) {
        final ItemStack stack = new ItemStack(nugget);
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }

    private SmeltingBonuses() {
    }
}
