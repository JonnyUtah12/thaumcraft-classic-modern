package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The smelting-bonus table (TC-M4-06-s): the extra drops the arcane furnace
 * throws off when it smelts a specific input.
 *
 * <p>Original: {@code ThaumcraftApi.addSmeltingBonus} /
 * {@code ThaumcraftApi.getSmeltingBonus} over a
 * {@code HashMap<Object, ItemStack>} keyed on {@code List<Item, meta>} (with
 * {@code 32767} as the any-meta wildcard) and on ore-dictionary name strings,
 * populated by {@code ConfigRecipes}. The rows are the 18 original calls,
 * verbatim; the lookup order is the original's too — exact item+meta, then
 * item+wildcard, then the ore-dictionary keys.</p>
 *
 * <p>1.21.1 translation: the stored value keeps the original's
 * <b>count 0</b> semantics (the bonus is the roll's <em>result</em>, not one
 * free copy), the damage values ride on the {@code thaumcraft:item_meta}
 * component like every other meta-carrying item in this port (D-001), and the
 * ore-dictionary names become item tags in the {@code c} namespace the repo
 * already uses ({@code c:ores/gold} for {@code oreGold}, etc.). A tag that no
 * pack defines simply never matches, exactly like an ore-dictionary name
 * nothing is registered under.</p>
 *
 * <p>Keys are item id strings, not {@link Item} references, so the table can
 * be filled at mod-construction time and resolved against the registry at
 * smelt time; that also keeps SmeltingBonusesTest able to pin every row
 * without a booted game.</p>
 */
public final class SmeltingBonuses {

    /** The original's any-metadata key. */
    public static final int WILDCARD_META = 32767;

    /** One row's output: an item id, an original damage value, count 0. */
    public record Bonus(String itemId, int meta) {
    }

    /** A table key: item id + original damage value. */
    public record Key(String itemId, int meta) {
    }

    private static final Map<Key, Bonus> BY_ITEM = new LinkedHashMap<>();
    private static final Map<String, Bonus> BY_TAG = new LinkedHashMap<>();

    private SmeltingBonuses() {
    }

    public static void register(final String inItemId, final int inMeta,
                                final String outItemId, final int outMeta) {
        BY_ITEM.put(new Key(inItemId, inMeta), new Bonus(outItemId, outMeta));
    }

    /** The original's ore-dictionary-keyed row, as an item tag id. */
    public static void registerTag(final String tagId, final String outItemId, final int outMeta) {
        BY_TAG.put(tagId, new Bonus(outItemId, outMeta));
    }

    public static void clear() {
        BY_ITEM.clear();
        BY_TAG.clear();
    }

    public static int size() {
        return BY_ITEM.size() + BY_TAG.size();
    }

    public static Map<Key, Bonus> byItem() {
        return Collections.unmodifiableMap(BY_ITEM);
    }

    public static Map<String, Bonus> byTag() {
        return Collections.unmodifiableMap(BY_TAG);
    }

    /**
     * Original {@code getSmeltingBonus}: exact item+meta, then the item's
     * wildcard row, then the input's ore-dictionary (tag) names.
     *
     * @param tags the tag ids the input item belongs to (namespace:path form)
     */
    @Nullable
    public static Bonus lookup(final String inItemId, final int inMeta, final Collection<String> tags) {
        final Bonus exact = BY_ITEM.get(new Key(inItemId, inMeta));
        if (exact != null) {
            return exact;
        }
        final Bonus wildcard = BY_ITEM.get(new Key(inItemId, WILDCARD_META));
        if (wildcard != null) {
            return wildcard;
        }
        for (final String tag : tags) {
            final Bonus byTag = BY_TAG.get(tag);
            if (byTag != null) {
                return byTag;
            }
        }
        return null;
    }

    /** The registry-resolving form the furnace tile calls. */
    @Nullable
    public static Bonus getBonus(final ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(input.getItem());
        final int meta = input.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
        return lookup(id.toString(), meta, tagIds(input.getItem()));
    }

    private static Set<String> tagIds(final Item item) {
        return item.builtInRegistryHolder().tags()
                .map(tag -> tag.location().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * The original's bonus roll, verbatim: with no bellows the bonus drops at
     * all only on {@code nextInt(4) == 0}; with <i>n</i> bellows each of the
     * n rolls adds one at {@code nextFloat() < 0.44}. The base count is 0, as
     * the table stores {@code new ItemStack(out, 0, meta)}.
     */
    public static int rollCount(final int bellows, final RandomSource random) {
        int count = 0;
        if (bellows <= 0) {
            if (random.nextInt(4) == 0) {
                count++;
            }
            return count;
        }
        for (int a = 0; a < bellows; a++) {
            if (random.nextFloat() < 0.44F) {
                count++;
            }
        }
        return count;
    }

    /** Resolve a row's stack, or an empty stack when the roll gave nothing. */
    public static ItemStack rollStack(@Nullable final Bonus bonus, final int bellows, final RandomSource random) {
        if (bonus == null) {
            return ItemStack.EMPTY;
        }
        final int count = rollCount(bellows, random);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        final Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(bonus.itemId()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = new ItemStack(item, count);
        if (bonus.meta() != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), bonus.meta());
        }
        return stack;
    }

    /**
     * The 18 rows of the original {@code ConfigRecipes} block, verbatim
     * (ids: {@code minecraft:gold_nugget} for {@code Items.field_151074_bl},
     * {@code thaumcraft:itemnugget} for {@code ConfigItems.itemNugget},
     * {@code minecraft:chicken}/{@code beef}/{@code porkchop}/{@code fish} for
     * the raw-food inputs and the matching
     * {@code thaumcraft:itemnugget{chicken,beef,pork,fish}} outputs).
     */
    public static void registerOriginalBonuses() {
        // Ore-dictionary (tag) keyed rows.
        registerTag("c:ores/gold", "minecraft:gold_nugget", 0);
        registerTag("c:ores/iron", "thaumcraft:itemnugget", 0);
        registerTag("c:ores/cinnabar", "thaumcraft:itemnugget", 5);
        registerTag("c:ores/copper", "thaumcraft:itemnugget", 1);
        registerTag("c:ores/tin", "thaumcraft:itemnugget", 2);
        registerTag("c:ores/silver", "thaumcraft:itemnugget", 3);
        registerTag("c:ores/lead", "thaumcraft:itemnugget", 4);

        // TC's own nugget metas (quicksilver/chaining/gossamer metal forms)
        // smelt into the plain metal nuggets, gold nugget for meta 31.
        register("thaumcraft:itemnugget", 31, "minecraft:gold_nugget", 0);
        register("thaumcraft:itemnugget", 16, "thaumcraft:itemnugget", 0);
        register("thaumcraft:itemnugget", 21, "thaumcraft:itemnugget", 5);
        register("thaumcraft:itemnugget", 17, "thaumcraft:itemnugget", 1);
        register("thaumcraft:itemnugget", 18, "thaumcraft:itemnugget", 2);
        register("thaumcraft:itemnugget", 19, "thaumcraft:itemnugget", 3);
        register("thaumcraft:itemnugget", 20, "thaumcraft:itemnugget", 4);

        // Raw meat smelts into its nugget form; the fish row is any-meta.
        register("minecraft:chicken", 0, "thaumcraft:itemnuggetchicken", 0);
        register("minecraft:beef", 0, "thaumcraft:itemnuggetbeef", 0);
        register("minecraft:porkchop", 0, "thaumcraft:itemnuggetpork", 0);
        register("minecraft:cod", WILDCARD_META, "thaumcraft:itemnuggetfish", 0);
    }
}
