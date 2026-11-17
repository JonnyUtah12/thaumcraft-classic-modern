package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code dungeonChest} pool the pech's dice roll samples — the rows 1.7.10
 * registers into it, transcribed. TC-M7-04-m3-b-c (BD-80).
 *
 * <p><b>The seam.</b> {@code ContainerPech.java:25} takes
 * {@code ChestGenHooks.getInfo("dungeonChest")} into the container, and the
 * chest branch of the payout loop ({@code am >= 4 && rand.nextBoolean()},
 * {@code :126}) draws from it: {@code :127}
 * {@code WeightedRandomChestContent[] contents = this.chest.getItems(rand)},
 * {@code :131-134}
 * {@code do { wc = contents[rand.nextInt(contents.length)]; cc++; }}
 * {@code while (cc < 50 && (wc.field_76297_b == null || wc.field_76292_a > 5 || wc.field_76296_e > 1))},
 * then {@code :136-139} pays {@code wc.field_76297_b.func_77946_l()} (an item
 * copy) through {@code is.func_77980_a(world, player, 0)} into the pech's 5-cell
 * output grid ({@code this.func_75135_a(is, 1, 5, false)} — slots 1..4, the
 * window's {@code payToOutputRange} in this port), and {@code :141} gives the
 * amount back when the drawn entry has no item. The pool is the whole
 * <em>server</em> {@code dungeonChest} pool — {@code ChestGenHooks} is Forge's
 * loot engine, not the pech's trade table, and the decompiled Thaumcraft tree
 * does not contain it (it decompiles Thaumcraft's own classes), so the pool's
 * contents are only knowable from the rows a mod registers. Thaumcraft's are
 * here.</p>
 *
 * <p><b>What the source registers.</b> {@code Config.java} adds three tiers to
 * {@code dungeonChest}: the {@code commonLoot} trio ({@code :587-589}, added as
 * {@code (is, 1, 3, 5)} at {@code :615}), the {@code uncommonLoot} five
 * ({@code :590-596}, {@code (is, 1, 2, 4)} at {@code :625}) and the
 * {@code rareLoot} fourteen ({@code :597-612}, {@code (is, 1, 1, 1)} at
 * {@code :637}). The {@code amulet} entry at {@code :611} is
 * {@code ConfigItems.itemAmuletVis} meta 0 with vis charges rolled once per
 * game at config init ({@code :519-524}) — one shared stack, so every pech in a
 * world pays the same charges; the port ships the plain stack and BD-80 records
 * it.</p>
 *
 * <p><b>The filter, and why the ctor's argument order does not matter here.</b>
 * The MCP names of the three filtered fields are {@code theItemId}
 * ({@code field_76297_b}), {@code itemWeight} ({@code field_76292_a}) and
 * {@code theMaximumChanceToGenerateItem} ({@code field_76296_e}) —
 * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}. Forge's
 * {@code WeightedRandomChestContent(ItemStack, int, int, int)} is
 * {@code (item, minChances, maxGroup, weight)}, so a registration
 * {@code (is, 1, 3, 5)} has max group count 3 and weight 5. Read the other way
 * — weight third, max fourth — the trio is (3, 5) and the five are (2, 4).
 * The filter's result is <b>the same under both readings</b>: it rejects the
 * trio (max 3, or max 5) and the five (max 2, or max 4) on the
 * {@code max > 1} term, and admits the fourteen, whose last two arguments are
 * {@code (1, 1)}. So {@link #payableByTheBranch} gives the {@code rareLoot}
 * fourteen on either reading, and the port does not have to settle Forge's
 * parameter names to ship the right rows.</p>
 *
 * <p><b>Why not 1.21.1's chest loot table</b> — the card's other option, and
 * the row this card closes. Measured on the target:
 * <ol>
 *   <li><b>No seam.</b> The card named {@code LootTable#getRandomItem(LootParams.Builder,
 *       float luck)}. {@code javap net.minecraft.world.level.storage.loot.LootTable}
 *       on {@code build/moddev/artifacts/neoforge-21.1.259-merged.jar} has no
 *       such method: the public draws are {@code getRandomItemsRaw(LootParams,
 *       Consumer)}, {@code getRandomItems(LootParams, Consumer)},
 *       {@code getRandomItems(LootParams[, long | RandomSource]) →
 *       ObjectArrayList<ItemStack>} and {@code fill(Container, LootParams,
 *       long)}. Every one of them rolls the WHOLE table — no weighted single
 *       pick, no retry, no per-entry acceptance test — so the bounded posture
 *       BD-79 demands of this branch (a capped search, never an unbounded
 *       consumer on a server thread) cannot be expressed over it.</li>
 *   <li><b>The id.</b> On 21.1.259 the vanilla dungeon table is
 *       {@code minecraft:chests/simple_dungeon}
 *       ({@code data/minecraft/loot_table/chests/simple_dungeon.json} in the
 *       client-resources jar); {@code chests/dungeon} is a later version's id.</li>
 *   <li><b>The table fails the branch's own filter.</b> That table is 3 pools of
 *       25 entries with rolls {@code uniform(1,3)}, {@code uniform(1,4)} and
 *       {@code 3}, and entry weights 2, 5, 10, 15 and 20 (read off the JSON:
 *       pool 1 = saddle 20, golden_apple 15, enchanted_golden_apple 2,
 *       music_disc_otherside 2, music_disc_13 15, music_disc_cat 15, name_tag
 *       20, golden_horse_armor 10, iron_horse_armor 15, diamond_horse_armor 5,
 *       book 10). 21 of the 25 entries carry weight &gt; 5, and every pool rolls
 *       more than once, so under {@code :134} — the filter the branch actually
 *       applies — the draw space the table offers is EMPTY: the branch would
 *       burn all 50 draws and take the give-back at {@code :141} on every
 *       {@code am >= 4} roll. Mapping the pool onto the vanilla table does not
 *       port the branch, it deletes it.</li>
 *   <li><b>The content is not the pech's money.</b> {@code EntityPech.java:739-745}
 *       prices exactly six items — mana bean 1, gold_ingot 1, golden_apple 2,
 *       ender_pearl 3, diamond 4, emerald 5 — and {@code getValue}
 *       ({@code :724-737}) falls back to the item's GREED aspect total, which a
 *       vanilla saddle, name tag, music disc, bone or melon seed does not carry:
 *       value 0. The branch's roll consumed 4-5 points of the offer to buy the
 *       draw, so a saddle draw (weight 20 of pool 1's 129) pays the player an
 *       item the pech values at nothing, and the table's 1-4-roll pools pay
 *       multi-item stacks against a branch that pays one copy
 *       ({@code :136-139}).</li>
 * </ol>
 * A hand-written weighted pool over 1.21.1 content would invent content the
 * source never registered, so this card ships the transcribed rows. The 1.7.10
 * pool's VANILLA half — the rows Forge seeds into {@code dungeonChest} itself —
 * is not in {@code reference/} and is not stable across 1.7.10 builds (D-015);
 * BD-80 names that as the remaining half of this deviation.</p>
 *
 * <p><b>Registry-gated (D-007).</b> Four of the fourteen rows
 * ({@code itemswordthaumium}, {@code itempickthaumium}, {@code itemaxethaumium},
 * {@code itemhoethaumium}) name items that are not registered on main, so
 * {@link #liveRows} drops them from the draw space — the same gate
 * {@link PechTradeInventory#liveRows} puts on the trade table's rows (BD-79) —
 * and a gated row can never pay. The draw space is the LIVE rows in the
 * original's registration order, and the sampler keeps the original's shape:
 * uniform draws over it, the acceptance test per draw, the 50-attempt cap.</p>
 */
public final class PechTradeChestPool {

    private PechTradeChestPool() {
    }

    /**
     * One {@code WeightedRandomChestContent} registration: {@code (is, minChances,
     * maxGroup, weight)} in {@code Config.java}'s argument order.
     */
    public record ChestRow(ResourceLocation item, int meta, int minChances, int maxGroup, int weight) {
    }

    /** {@code :134} — {@code wc.field_76292_a > 5} (itemWeight). */
    static final int BRANCH_MAX_WEIGHT = 5;
    /** {@code :134} — {@code wc.field_76296_e > 1} (theMaximumChanceToGenerateItem). */
    static final int BRANCH_MAX_GROUP = 1;

    /**
     * The branch's acceptance test, {@code ContainerPech.java:134}: a drawn entry
     * is rejected when its weight is above 5 or its max group count is above 1
     * (its item being null is the third term; no transcribed row has one).
     */
    public static boolean payableByTheBranch(final ChestRow row) {
        return row.weight() <= BRANCH_MAX_WEIGHT && row.maxGroup() <= BRANCH_MAX_GROUP;
    }

    /**
     * The rows 1.7.10 registers into {@code dungeonChest}, in registration order
     * ({@code commonLoot}, then {@code uncommonLoot}, then {@code rareLoot}),
     * ungated — see {@link #liveRows} for the draw space.
     *
     * <p><b>The draw space and Forge's array.</b> The original samples
     * {@code chest.getItems(rand)} ({@code ContainerPech.java:126}), a
     * {@code ChestGenHooks.HookData} method of Forge 1.7.10 (not a Thaumcraft
     * class, so it is not in {@code reference/}; the board's cites are
     * {@code ChestGenHooks.java:414-417} for the array and {@code :433} for the
     * count roll). That array is built from the registrations with their
     * {@code itemWeight}, so its slots carry the weights: the {@code rareLoot}
     * fourteen each have weight 1 ({@code Config.java:637}), so every accepted
     * draw is uniform over the rows that survive the filter — the same
     * distribution this port draws, which samples the distinct registered rows
     * ({@code contents[rand.nextInt(contents.length)]}, {@code :131}) and stops
     * at the first accepted one. The readings differ only in how many draws are
     * burned before a hit: 49 slots of which 14 are accepted (expected 3.5,
     * P(no hit in 50 draws) ≈ 1e-8) against 22 of which 14 (expected 1.57,
     * ≈ 1e-25). Both end inside the {@code cc < 50} bound for every practical
     * stream and pay the same uniform row, and the shipped
     * {@code pechChestPoolPaysTheLiveRowsUniformly} probe measures the payout
     * side of that claim over 4000 seeded rolls.</p>
     */
    public static List<ChestRow> pool() {
        return POOL;
    }

    /**
     * The rows the branch's filter accepts: the {@code rareLoot} fourteen. The
     * {@code commonLoot} trio and the {@code uncommonLoot} five are registered by
     * the original — they are in the draw space and get drawn — and are rejected
     * by {@code :134} every time one comes up.
     */
    public static List<ChestRow> payable() {
        return PAYABLE;
    }

    /**
     * The draw space: the pool's rows whose item is registered, in pool order.
     * Empty means the branch has no pool at all, which the original cannot reach
     * (its pool always has contents) and which routes the branch to the
     * give-back at {@code :141}.
     */
    public static List<ChestRow> liveRows(final RegistryAccess registries) {
        final List<ChestRow> live = new ArrayList<>();
        for (ChestRow row : POOL) {
            if (!BuiltInRegistries.ITEM.get(row.item()).equals(Items.AIR)) {
                live.add(row);
            }
        }
        return live;
    }

    /**
     * The stack a drawn row pays: {@code wc.field_76297_b.func_77946_l()}
     * ({@code :136-137}) — one item at the row's meta, the pool stack's own
     * count, which is 1 for every row here. The original's
     * {@code is.func_77980_a(world, player, 0)} at {@code :138} (the 1.7.10 stack
     * bonus randomizer, BD-81's not-ported half) is not applied, and the amulet
     * row's vis charges are not invented.
     */
    public static ItemStack resolve(final ChestRow row, final RegistryAccess registries) {
        final Item item = BuiltInRegistries.ITEM.get(row.item());
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = new ItemStack(item, 1);                                       // the pool stack's count
        PechTradeInventory.applyMeta(stack, row.meta());                                      // the row's meta
        return stack;
    }

    private static ChestRow row(final String path, final int meta, final int min, final int max, final int weight) {
        return new ChestRow(ResourceLocation.fromNamespaceAndPath("thaumcraft", path), meta, min, max, weight);
    }

    private static final List<ChestRow> POOL = List.of(
            // commonLoot (Config.java:587-589), registered (1, 3, 5) at :615
            row("itemlootbag", 0, 1, 3, 5),
            row("itemresource", 2, 1, 3, 5),
            row("itemresource", 6, 1, 3, 5),
            // uncommonLoot (Config.java:590-596), registered (1, 2, 4) at :625
            row("itemlootbag", 1, 1, 2, 4),
            row("itembaubleblanks", 0, 1, 2, 4),
            row("itembaubleblanks", 1, 1, 2, 4),
            row("itembaubleblanks", 2, 1, 2, 4),
            row("itemresource", 9, 1, 2, 4),
            // rareLoot (Config.java:597-612), registered (1, 1, 1) at :637; `amulet` at
            // :611 is itemAmuletVis meta 0 with the charges rolled at :519-524
            row("itemlootbag", 2, 1, 1, 1),
            row("itemthaumonomicon", 0, 1, 1, 1),
            row("itemswordthaumium", 0, 1, 1, 1),
            row("itempickthaumium", 0, 1, 1, 1),
            row("itemaxethaumium", 0, 1, 1, 1),
            row("itemhoethaumium", 0, 1, 1, 1),
            row("itemringrunic", 0, 1, 1, 1),
            row("itembaubleblanks", 3, 1, 1, 1),
            row("itembaubleblanks", 4, 1, 1, 1),
            row("itembaubleblanks", 5, 1, 1, 1),
            row("itembaubleblanks", 6, 1, 1, 1),
            row("itembaubleblanks", 7, 1, 1, 1),
            row("itembaubleblanks", 8, 1, 1, 1),
            row("itemamuletvis", 0, 1, 1, 1));

    private static final List<ChestRow> PAYABLE = POOL.stream().filter(PechTradeChestPool::payableByTheBranch).toList();
}
