package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The pech trade table (TC-M7-04-m3-b-followup): the port of
 * {@code EntityPech.tradeInventory} — {@code monster/EntityPech.java:71}
 * (the field) and its static fill at {@code :746-829} — a per-pech-type list of
 * {@code [amount, ItemStack]} pairs the trade roll pays out of
 * ({@code ContainerPech.java:105} reads it, {@code :150-155} picks a row whose
 * first element equals the rolled amount).
 *
 * <p><b>Transcribed verbatim</b>, row for row, in the original's order, with
 * the line of the decompiled source cited on each row. The original's
 * {@code for (a…)} blocks are expanded (shards {@code :784-786} metas 0-5,
 * crystals {@code :797-799} metas 0-6, candles {@code :810-812} metas 0-14),
 * and its literal duplicates are kept as duplicates — the roll draws
 * uniformly over the list ({@code ContainerPech.java:151}), so the original's
 * twin {@code (5, blockCustomPlant meta 1)} rows at {@code :778,:779} carry
 * twice the weight, and merging them would change the payout distribution.</p>
 *
 * <h2>1.7.10 → 1.21.1 id shape</h2>
 * <ul>
 *   <li><b>Metadata-indexed ids</b> ride the port's shared convention:
 *       an id whose block family carries the {@code variant}
 *       {@link IntegerProperty} gets the placed variant on the vanilla
 *       {@code minecraft:block_state} component
 *       ({@link VariantStateItems#withVariant}); everything else gets
 *       {@code thaumcraft:item_meta} ({@link ModDataComponents#ITEM_META}).
 *       {@link #applyMeta} implements that split, so a payout stack carries its
 *       variant the way the shipped recipes and loot tables put one on.</li>
 *   <li><b>The {@code potionitem} rows</b> ({@code Items.field_151068_bn},
 *       MCP {@code potionitem} — NOT the enchanted book) carry the original
 *       meta verbatim on {@code thaumcraft:item_meta}. Vanilla's
 *       {@code PotionHelper} meta-to-effect decoding lives in the 1.7.10 game,
 *       which is not in this repo's reference tree (it decompiles
 *       {@code thaumcraft/} and {@code baubles/} only), so the port ships the
 *       meta as data and does not invent an effect list (BD-76).</li>
 *   <li><b>The {@code enchanted_book} rows</b>
 *       ({@code Items.field_151134_bR.func_92111_a(new EnchantmentData(x, 1))})
 *       become a {@code minecraft:enchanted_book} carrying
 *       {@code minecraft:stored_enchantments} over one {@link Enchantment} at
 *       level 1. The TC enchantments are data-registry JSON
 *       ({@code data/thaumcraft/enchantment/ench_haste.json},
 *       {@code ench_repair.json} — {@code Config.java:251,255} registers
 *       {@code Config.enchHaste}/{@code enchRepair}); the vanilla ones resolve
 *       through {@code minecraft:power} / {@code flame} / {@code infinity}
 *       ({@code Enchantment.field_77345_t}/{@code :77343_v}/{@code :77342_w},
 *       MCP names).</li>
 *   <li><b>Golden apple meta 1</b> is a separate modern id
 *       ({@code minecraft:enchanted_golden_apple}), the same split
 *       {@code loot/LootRoller.java:41-43} records.</li>
 *   <li><b>The {@code Config.found*Ingot} compat gates</b>
 *       ({@code :751-765} — copper/tin/silver/lead, i.e. "another mod's ore is
 *       present") become the registry gate below: the row ships and is
 *       SKIPPED while its item is unregistered, which is exactly what the
 *       original's flag did when the mod was absent. D-010 (config options not
 *       ported) is the reason the flag itself is gone.</li>
 * </ul>
 *
 * <h2>Registry gating (the {@code neoforge:item_exists} posture, at runtime)</h2>
 * A row is LIVE only when its item resolves in
 * {@link BuiltInRegistries#ITEM} (not {@link Items#AIR}) and, for a book row,
 * its enchantment resolves in the {@code minecraft:enchantment}
 * <b>data</b> registry (the port's enchantments are datapack entries, so
 * {@code BuiltInRegistries.ENCHANTMENT} is the wrong accessor — use the
 * {@link RegistryAccess} lookup). The roll draws only over live rows
 * ({@link #liveRows}), which is what makes the original's unbounded
 * {@code do { … } while (it.get(0) != am);} at
 * {@code ContainerPech.java:150-152} safe to bound: see
 * {@link PechTradeRoll#plan} and BD-74.</p>
 */
public final class PechTradeInventory {

    /**
     * One {@code [amount, ItemStack]} entry: the amount is the payout "price
     * point" the roll matches (NOT a stack size — every original entry is a
     * stack of one), and the stack is {@code (item, meta, enchantment)}:
     * {@code meta < 0} = no metadata, {@code enchantment != null} = an
     * enchanted book carrying that enchantment at level 1.
     */
    public record TradeRow(int amount, ResourceLocation item, int meta, ResourceLocation enchantment) {

        TradeRow(final int amount, final ResourceLocation item, final int meta) {
            this(amount, item, meta, null);
        }

        TradeRow(final int amount, final ResourceLocation item, final ResourceLocation enchantment) {
            this(amount, item, -1, enchantment);
        }

        public boolean enchantedBook() {
            return this.enchantment != null;
        }
    }

    /** The original's static field: {@code EntityPech.java:71}. */
    public static final Map<Integer, List<TradeRow>> TRADE_INVENTORY = tradeInventory();

    /** The pech type's table (the original's {@code tradeInventory.get(pech.getPechType())}). */
    public static List<TradeRow> tableFor(final int pechType) {
        return TRADE_INVENTORY.getOrDefault(pechType, List.of());
    }

    /**
     * The rows of that table whose item (and enchantment) are registered: the
     * draw space of the roll. {@code registries} is the level's, so the
     * enchantment rows follow the data pack that is actually loaded.
     */
    public static List<TradeRow> liveRows(final int pechType, final RegistryAccess registries) {
        final List<TradeRow> live = new ArrayList<>();
        for (final TradeRow row : tableFor(pechType)) {
            if (isLive(row, registries)) {
                live.add(row);
            }
        }
        return live;
    }

    /** The amounts the live rows carry — the set the roll's {@code am} can match. */
    public static java.util.Set<Integer> amountsOf(final List<TradeRow> rows) {
        final java.util.Set<Integer> amounts = new java.util.LinkedHashSet<>();
        for (final TradeRow row : rows) {
            amounts.add(row.amount());
        }
        return amounts;
    }

    /** Is this row's payload resolvable right now? */
    public static boolean isLive(final TradeRow row, final RegistryAccess registries) {
        if (BuiltInRegistries.ITEM.get(row.item()) == Items.AIR) {
            return false;
        }
        return row.enchantment() == null || enchantmentHolder(registries, row.enchantment()).isPresent();
    }

    /**
     * The payout stack: one item of the row, with the original metadata on the
     * port's variant carrier and, for a book row, the stored enchantment at
     * level 1. {@link ItemStack#EMPTY} when the row is not live — the caller
     * must skip the insertion (the original's table was whole by construction,
     * so it had no such case).
     *
     * <p>The original applies its {@code ItemStack.func_77980_a(world, player, 0)}
     * "bonus enchantment" randomizer to the (b)/(c) payouts
     * ({@code ContainerPech.java:145,154}). That method is vanilla 1.7.10,
     * which is not in this repo's reference decompile tree, so the port does
     * not reproduce it: the payout is the plain row stack (BD-76).</p>
     */
    public static ItemStack resolve(final TradeRow row, final RegistryAccess registries) {
        final Item item = BuiltInRegistries.ITEM.get(row.item());
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        Holder<Enchantment> enchantment = null;
        if (row.enchantment() != null) {
            final Optional<Holder.Reference<Enchantment>> found =
                    enchantmentHolder(registries, row.enchantment());
            if (found.isEmpty()) {
                return ItemStack.EMPTY;
            }
            enchantment = found.get();
        }
        final ItemStack stack = new ItemStack(item, 1);                                   // every entry is one item
        if (row.meta() >= 0) {
            applyMeta(stack, row.meta());
        }
        if (enchantment != null) {
            final ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(enchantment, 1);                                                   // EnchantmentData(x, 1)
            stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        }
        return stack;
    }

    /** The data-registry lookup the port's enchantments need (they are datapack
     *  JSON, so the level's {@link RegistryAccess} is the accessor —
     *  {@code BuiltInRegistries.ENCHANTMENT} is the wrong one). */
    private static Optional<Holder.Reference<Enchantment>> enchantmentHolder(
            final RegistryAccess registries, final ResourceLocation id) {
        return registries.registryOrThrow(Registries.ENCHANTMENT).getHolder(id);
    }

    /**
     * The port's metadata convention: a block family with the {@code variant}
     * {@link IntegerProperty} carries the meta as the placed state on
     * {@code minecraft:block_state} (clamped into the property's range, so a
     * meta the shipped block cannot express places its nearest legal variant);
     * every other item carries it on {@code thaumcraft:item_meta}.
     */
    static void applyMeta(final ItemStack stack, final int meta) {
        if (stack.getItem() instanceof final BlockItem blockItem) {
            final Property<?> property =
                    blockItem.getBlock().getStateDefinition().getProperty(VariantStateItems.VARIANT_PROPERTY);
            if (property instanceof final IntegerProperty variant) {
                // 21.1.259's IntegerProperty exposes getPossibleValues() only
                // (no min/max accessors), so the domain is read from it.
                int low = Integer.MAX_VALUE;
                int high = Integer.MIN_VALUE;
                for (final int legal : variant.getPossibleValues()) {
                    low = Math.min(low, legal);
                    high = Math.max(high, legal);
                }
                final BlockState state = blockItem.getBlock().defaultBlockState()
                        .setValue(variant, net.minecraft.util.Mth.clamp(meta, low, high));
                VariantStateItems.withVariant(stack, state);
                return;
            }
        }
        stack.set(ModDataComponents.ITEM_META.get(), meta);
    }

    // ---- the transcription -------------------------------------------------

    private static ResourceLocation tc(final String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    private static ResourceLocation mc(final String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static Map<Integer, List<TradeRow>> tradeInventory() {
        final Map<Integer, List<TradeRow>> tables = new LinkedHashMap<>();

        // ---- type 0, the original's forInv (EntityPech.java:746-779) -------
        final List<TradeRow> forInv = new ArrayList<>();
        forInv.add(new TradeRow(1, tc("itemmanabean"), -1));                    // :747 Mana Bean
        forInv.add(new TradeRow(1, tc("itemnugget"), 16));                      // :748 Native Iron Cluster
        forInv.add(new TradeRow(1, tc("itemnugget"), 31));                      // :749 Native Gold Cluster
        forInv.add(new TradeRow(1, tc("itemnugget"), 21));                      // :750 Native Cinnabar Cluster
        forInv.add(new TradeRow(1, tc("itemnugget"), 17));                      // :752 Native Copper (foundCopperIngot)
        forInv.add(new TradeRow(1, tc("itemnugget"), 18));                      // :756 Native Tin (foundTinIngot)
        forInv.add(new TradeRow(1, tc("itemnugget"), 19));                      // :760 Native Silver (foundSilverIngot)
        forInv.add(new TradeRow(1, tc("itemnugget"), 20));                      // :764 Native Lead (foundLeadIngot)
        forInv.add(new TradeRow(2, mc("blaze_rod"), -1));                       // :767 Items.field_151072_bj
        forInv.add(new TradeRow(2, tc("blockcustomplant"), 0));                 // :768 Greatwood Sapling
        forInv.add(new TradeRow(2, mc("potion"), 8201));                        // :769 potionitem meta 8201
        forInv.add(new TradeRow(2, mc("potion"), 8194));                        // :770 potionitem meta 8194
        forInv.add(new TradeRow(3, mc("experience_bottle"), -1));               // :771 Items.field_151062_by
        forInv.add(new TradeRow(3, tc("itemresource"), 9));                     // :772 Knowledge Fragment
        forInv.add(new TradeRow(3, mc("golden_apple"), 0));                     // :773 golden apple
        forInv.add(new TradeRow(3, mc("potion"), 8265));                        // :774 potionitem meta 8265
        forInv.add(new TradeRow(3, mc("potion"), 8262));                        // :775 potionitem meta 8262
        forInv.add(new TradeRow(5, mc("enchanted_golden_apple"), -1));          // :776 golden apple meta 1
        forInv.add(new TradeRow(4, tc("itempickthaumium"), -1));                // :777 not registered -> gated
        forInv.add(new TradeRow(5, tc("blockcustomplant"), 1));                 // :778 Silverwood Sapling
        forInv.add(new TradeRow(5, tc("blockcustomplant"), 1));                 // :779 the original's twin row
        tables.put(PechAiLogic.TYPE_NORMAL, List.copyOf(forInv));

        // ---- type 1, the original's forMag (EntityPech.java:781-805) -------
        final List<TradeRow> forMag = new ArrayList<>();
        forMag.add(new TradeRow(1, tc("itemmanabean"), -1));                    // :782
        forMag.add(new TradeRow(1, tc("itemshard"), 0));                        // :785 shard meta 0 (air)
        forMag.add(new TradeRow(1, tc("itemshard"), 1));                        // :785 shard meta 1 (fire)
        forMag.add(new TradeRow(1, tc("itemshard"), 2));                        // :785 shard meta 2 (water)
        forMag.add(new TradeRow(1, tc("itemshard"), 3));                        // :785 shard meta 3 (earth)
        forMag.add(new TradeRow(1, tc("itemshard"), 4));                        // :785 shard meta 4 (order)
        forMag.add(new TradeRow(1, tc("itemshard"), 5));                        // :785 shard meta 5 (entropy)
        forMag.add(new TradeRow(1, tc("itemresource"), 9));                     // :788 Knowledge Fragment
        forMag.add(new TradeRow(2, tc("itemresource"), 9));                     // :789 Knowledge Fragment
        forMag.add(new TradeRow(2, mc("potion"), 8193));                        // :790 potionitem meta 8193
        forMag.add(new TradeRow(2, mc("potion"), 8261));                        // :791 potionitem meta 8261
        forMag.add(new TradeRow(3, mc("enchanted_book"), tc("ench_haste")));    // :792 EnchantmentData(enchHaste, 1)
        forMag.add(new TradeRow(3, mc("golden_apple"), 0));                     // :793
        forMag.add(new TradeRow(3, mc("potion"), 8225));                        // :794 potionitem meta 8225
        forMag.add(new TradeRow(3, mc("potion"), 8229));                        // :795 potionitem meta 8229
        forMag.add(new TradeRow(4, tc("blockcrystal"), 0));                     // :798 crystal meta 0 (air)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 1));                     // :798 crystal meta 1 (fire)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 2));                     // :798 crystal meta 2 (water)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 3));                     // :798 crystal meta 3 (earth)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 4));                     // :798 crystal meta 4 (order)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 5));                     // :798 crystal meta 5 (entropy)
        forMag.add(new TradeRow(4, tc("blockcrystal"), 6));                     // :798 crystal meta 6 (mixed)
        forMag.add(new TradeRow(5, mc("enchanted_golden_apple"), -1));          // :801 golden apple meta 1
        forMag.add(new TradeRow(5, mc("enchanted_book"), tc("ench_repair")));   // :802 EnchantmentData(enchRepair, 1)
        forMag.add(new TradeRow(5, tc("focuspouch"), -1));                      // :803 not registered -> gated
        forMag.add(new TradeRow(5, tc("focuspech"), -1));                       // :804 not registered -> gated
        forMag.add(new TradeRow(5, tc("itemamuletvis"), 0));                    // :805 Amulet of Vis Storage meta 0
        tables.put(PechAiLogic.TYPE_WAND, List.copyOf(forMag));

        // ---- type 2, the original's forArc (EntityPech.java:807-828) -------
        final List<TradeRow> forArc = new ArrayList<>();
        forArc.add(new TradeRow(1, tc("itemmanabean"), -1));                    // :808
        forArc.add(new TradeRow(1, tc("blockcandle"), 0));                      // :811 candle meta 0 (white)
        forArc.add(new TradeRow(1, tc("blockcandle"), 1));                      // :811 candle meta 1
        forArc.add(new TradeRow(1, tc("blockcandle"), 2));                      // :811 candle meta 2
        forArc.add(new TradeRow(1, tc("blockcandle"), 3));                      // :811 candle meta 3
        forArc.add(new TradeRow(1, tc("blockcandle"), 4));                      // :811 candle meta 4
        forArc.add(new TradeRow(1, tc("blockcandle"), 5));                      // :811 candle meta 5
        forArc.add(new TradeRow(1, tc("blockcandle"), 6));                      // :811 candle meta 6
        forArc.add(new TradeRow(1, tc("blockcandle"), 7));                      // :811 candle meta 7
        forArc.add(new TradeRow(1, tc("blockcandle"), 8));                      // :811 candle meta 8
        forArc.add(new TradeRow(1, tc("blockcandle"), 9));                      // :811 candle meta 9
        forArc.add(new TradeRow(1, tc("blockcandle"), 10));                     // :811 candle meta 10
        forArc.add(new TradeRow(1, tc("blockcandle"), 11));                     // :811 candle meta 11
        forArc.add(new TradeRow(1, tc("blockcandle"), 12));                     // :811 candle meta 12
        forArc.add(new TradeRow(1, tc("blockcandle"), 13));                     // :811 candle meta 13
        forArc.add(new TradeRow(1, tc("blockcandle"), 14));                     // :811 candle meta 14
        forArc.add(new TradeRow(2, mc("ghast_tear"), -1));                      // :814 Items.field_151073_bk
        forArc.add(new TradeRow(2, mc("potion"), 8194));                        // :815 potionitem meta 8194
        forArc.add(new TradeRow(2, mc("potion"), 8201));                        // :816 potionitem meta 8201
        forArc.add(new TradeRow(2, mc("enchanted_book"), mc("power")));         // :817 Enchantment.field_77345_t
        forArc.add(new TradeRow(3, mc("experience_bottle"), -1));               // :818
        forArc.add(new TradeRow(3, tc("itemresource"), 9));                     // :819
        forArc.add(new TradeRow(3, mc("potion"), 8270));                        // :820 potionitem meta 8270
        forArc.add(new TradeRow(3, mc("potion"), 8225));                        // :821 potionitem meta 8225
        forArc.add(new TradeRow(3, mc("golden_apple"), 0));                     // :822
        forArc.add(new TradeRow(5, mc("enchanted_golden_apple"), -1));          // :823 golden apple meta 1
        forArc.add(new TradeRow(4, tc("itembootsthaumium"), -1));               // :824 Thaumium Boots
        forArc.add(new TradeRow(5, tc("itemringrunic"), 0));                    // :825 Ring meta 0
        forArc.add(new TradeRow(5, mc("enchanted_book"), mc("flame")));         // :826 Enchantment.field_77343_v
        forArc.add(new TradeRow(5, mc("enchanted_book"), mc("infinity")));      // :827 Enchantment.field_77342_w
        tables.put(PechAiLogic.TYPE_BOW, List.copyOf(forArc));

        return Map.copyOf(tables);
    }

    private PechTradeInventory() {
    }
}
