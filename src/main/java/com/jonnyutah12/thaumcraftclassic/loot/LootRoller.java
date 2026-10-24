package com.jonnyutah12.thaumcraftclassic.loot;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Treasure-bag loot tables (content-mapping.csv {@code thaumcraft:itemlootbag}
 * and the break-drops of {@code thaumcraft:blocklooturn} /
 * {@code thaumcraft:blocklootcrate}).
 *
 * <p>Original: {@code ThaumcraftApi.addLootBagItem} registrations in
 * {@code Config.initLoot()} feeding {@code WeightedRandomLoot.lootBagCommon/
 * Uncommon/Rare}, consumed by {@code Utils.generateLoot(int rarity, Random)}.
 * The table below is that registration list transcribed verbatim (weights,
 * counts, rarities), with 1.7.10 → 1.21.1 id/meta translation:</p>
 * <ul>
 *   <li>TC items use the modern lowercase ids from content-mapping.csv; the
 *       original item metadata rides on the shared ITEM_META component.</li>
 *   <li>Vanilla {@code Items.field_*} fields resolve through the MCP mapping
 *       (nether_star, diamond, emerald, gold_ingot, ender_pearl,
 *       experience_bottle, golden_apple, book, potionitem).</li>
 *   <li>Golden apple meta 1 (enchanted) is a separate modern item id.</li>
 *   <li>TC items owned by sibling catalog cards (bauble blanks, vis amulet,
 *       runic ring, eldritch object, itemresource) are resolved lazily by id
 *       at roll time; while unregistered their entries are skipped, exactly
 *       as they were absent from the registry in the original when mods were
 *       missing. Once the owning card registers the id the entry activates
 *       with no change here.</li>
 * </ul>
 *
 * <p>The original's potion block (loop over metas {@code i | 8192} /
 * {@code i | 16384} guarded by {@code PotionHelper.getPotionEffects}) never
 * produced entries in 1.7.10 — vanilla's effect table returns empty for any
 * meta with the 0x2000/0x4000 bits set, so every lookup failed and the block
 * was dead code. The port realizes the table the loop was written to build:
 * the ten effect ids that exist in 1.7.10 (speed, slowness, poison,
 * regeneration, strength, instant health, instant damage, night vision,
 * weakness, saturation) in base/extended/upgraded tiers with the loop's
 * weights 1/2/3, using modern Potion instances with the original durations
 * (3600 / 9600 / 1800@amp1; instant effects duration 0). This deviation is
 * recorded in the itemlootbag CSV row.</p>
 */
public final class LootRoller {

    /** One addLootBagItem registration. */
    private record BagEntry(ResourceLocation item, int count, int meta, int weight, Potion potion) {
        BagEntry(ResourceLocation item, int count, int meta, int weight) {
            this(item, count, meta, weight, null);
        }
    }

    private static final List<BagEntry> BAG_COMMON = new ArrayList<>();
    private static final List<BagEntry> BAG_UNCOMMON = new ArrayList<>();
    private static final List<BagEntry> BAG_RARE = new ArrayList<>();

    private static ResourceLocation tc(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    static {
        // --- original Config.initLoot(), verbatim order -----------------------
        add(tc("itemresource"), 1, 18, 2500, 0);
        add(tc("itemresource"), 2, 18, 2250, 1);
        add(tc("itemresource"), 3, 18, 2000, 2);
        add(tc("itemeldritchobject"), 1, 3, 1, 2);
        add(mc("nether_star"), 1, -1, 1, 2);
        add(mc("diamond"), 1, -1, 10, 0);
        add(mc("diamond"), 2, -1, 50, 1, 2);
        add(mc("emerald"), 1, -1, 15, 0);
        add(mc("emerald"), 2, -1, 75, 1, 2);
        add(mc("gold_ingot"), 1, -1, 100, 0, 1, 2);
        add(mc("ender_pearl"), 1, -1, 100, 0, 1, 2);
        add(tc("itemresource"), 1, 9, 25, 0, 1, 2);
        add(tc("itembaubleblanks"), 1, 0, 10, 0);
        add(tc("itembaubleblanks"), 1, 1, 10, 0);
        add(tc("itembaubleblanks"), 1, 2, 10, 0);
        for (int a = 3; a <= 8; a++) {
            add(tc("itembaubleblanks"), 1, a, 5, 1);
            add(tc("itembaubleblanks"), 1, a, 7, 2);
        }
        // Original: a copy of the vis amulet with random primal vis added; the
        // vis charge lives on the amulet's own component (owned by its card),
        // so the table carries the plain amulet item.
        add(tc("itemamuletvis"), 1, 0, 6, 1, 2);
        add(tc("itemringrunic"), 1, 0, 5, 1, 2);
        add(mc("experience_bottle"), 1, -1, 5, 0);
        add(mc("experience_bottle"), 1, -1, 10, 1);
        add(mc("experience_bottle"), 1, -1, 20, 2);
        add(mc("enchanted_golden_apple"), 1, -1, 1, 0);
        add(mc("enchanted_golden_apple"), 1, -1, 2, 1);
        add(mc("enchanted_golden_apple"), 1, -1, 3, 2);
        add(mc("golden_apple"), 1, -1, 3, 0);
        add(mc("golden_apple"), 1, -1, 6, 1);
        add(mc("golden_apple"), 1, -1, 9, 2);
        add(mc("book"), 1, -1, 10, 0, 1, 2);

        // --- potion block (see class javadoc: dead in the original) ----------
        // effect id -> modern effect, for the ten ids valid in 1.7.10
        final Holder<MobEffect>[] effects = new Holder[] {
                null,                          // 0 water
                MobEffects.MOVEMENT_SPEED,     // 1
                MobEffects.MOVEMENT_SLOWDOWN,  // 2
                null,                          // 3 jump boost (removed in 1.7)
                MobEffects.POISON,             // 4
                MobEffects.REGENERATION,       // 5
                MobEffects.DAMAGE_BOOST,       // 6
                MobEffects.HEAL,               // 7 instant
                MobEffects.HARM,               // 8 instant
                MobEffects.NIGHT_VISION,       // 9
                MobEffects.WEAKNESS,           // 10
                null, null, null,              // 11-13 not in 1.7.10
                MobEffects.SATURATION,         // 14
        };
        for (Holder<MobEffect> effect : effects) {
            if (effect == null) {
                continue;
            }
            boolean instant = effect.value() instanceof InstantenousMobEffect;
            if (instant) {
                addPotion(new Potion(new MobEffectInstance(effect, 0, 0)), 1, 0, 1, 2);
                addPotion(new Potion(new MobEffectInstance(effect, 0, 1)), 3, 0, 1, 2);
            } else {
                addPotion(new Potion(new MobEffectInstance(effect, 3600, 0)), 1, 0, 1, 2);
                addPotion(new Potion(new MobEffectInstance(effect, 9600, 0)), 2, 0, 1, 2);
                addPotion(new Potion(new MobEffectInstance(effect, 1800, 1)), 3, 0, 1, 2);
            }
        }
    }

    private LootRoller() {
    }

    private static void add(ResourceLocation item, int count, int meta, int weight, int... bagTypes) {
        for (int rarity : bagTypes) {
            bag(rarity).add(new BagEntry(item, count, meta, weight));
        }
    }

    private static void addPotion(Potion potion, int weight, int... bagTypes) {
        for (int rarity : bagTypes) {
            bag(rarity).add(new BagEntry(mc("potion"), 1, -1, weight, potion));
        }
    }
    private static List<BagEntry> bag(int rarity) {
        return switch (rarity) {
            case 1 -> BAG_UNCOMMON;
            case 2 -> BAG_RARE;
            default -> BAG_COMMON;
        };
    }

    /**
     * Original {@code Utils.generateLoot(int rarity, Random rand)}: weighted
     * pick from the matching bag table; an entry whose item is not registered
     * (sibling card not landed) is skipped and re-rolled, bounded, then EMPTY.
     */
    public static ItemStack generateLoot(int rarity, RandomSource rand) {
        List<BagEntry> table = bag(rarity);
        for (int attempt = 0; attempt < 16; attempt++) {
            BagEntry entry = pickWeighted(table, rand);
            if (entry == null) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.get(entry.item);
            if (item == Items.AIR) {
                continue; // owning card not registered yet
            }
            ItemStack stack = new ItemStack(item, entry.count);
            if (entry.meta >= 0) {
                stack.set(ModDataComponents.ITEM_META.get(), entry.meta);
            }
            if (entry.potion != null) {
                stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new PotionContents(Holder.direct(entry.potion)));
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Original WeightedRandom.getRandomItem: total-weight roll, cumulative scan. */
    private static BagEntry pickWeighted(List<BagEntry> table, RandomSource rand) {
        int total = 0;
        for (BagEntry entry : table) {
            total += entry.weight;
        }
        if (total <= 0) {
            return null;
        }
        int roll = rand.nextInt(total);
        for (BagEntry entry : table) {
            roll -= entry.weight;
            if (roll < 0) {
                return entry;
            }
        }
        return null;
    }
}
