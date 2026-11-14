package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeChestPool;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeInventory;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeRoll;
import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogPech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TC-M7-04-m3-b-followup: the pech dice roll in the world
 * ({@code ContainerPech.java:83-159} through {@code PechMenu.generateContents}).
 *
 * <p>The arithmetic — the exact value→payout sequence, the draw order, the
 * bounds — is pinned draw-by-draw by {@code PechTradeRollTest} over a scripted
 * {@link RandomSource}. These probes own what only a live server can show: the
 * seeded roll runs through the real menu, the real registries and the real
 * pech — the grid carries the rows, the offer loses exactly one item, the scam
 * untames the pech and plays its cue, the loot pack decrements, and the bounded
 * table search (BD-74) terminates on a gated amount instead of hanging the
 * server thread.</p>
 *
 * <h2>How a probe drives the roll deterministically</h2>
 * The original draws on the WORLD's random
 * ({@code this.theWorld.field_73012_v} at {@code :92,98-102,107,110,118,138,142,151}
 * — NOT the pech's), and 21.1.259's {@code Level.random} is a public final
 * {@link RandomSource} field: it has {@code setSeed(long)} and no setter, so a
 * probe cannot substitute a scripted stream. It therefore seeds the level's own
 * stream and mirrors it with an independent clone
 * ({@code RandomSource.create(sameSeed)}), replays the roll on the clone, and
 * compares the window against that replay. The clone is the expectation; the
 * menu's application of it is the measured half.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PechTradeRollGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("PechTradeRollGameTest");

    /** The probe offer: a 7-item diamond stack (so the one-item consumption is visible). */
    private static final int OFFER_COUNT = 7;

    /** A stack of the probe offer. */
    private static ItemStack offerStack() {
        return new ItemStack(Items.DIAMOND, OFFER_COUNT);
    }

    /**
     * What the pech pays for the probe offer: the valued table
     * ({@code EntityPech.java:724-745}, ported at {@code PechAiLogic.VALUED_ITEMS})
     * gives {@code minecraft:diamond} 4, so the roll runs on value 4. The probes
     * read it from the shipped table rather than hardcoding it.
     */
    private static int offerValue(final GameTestHelper helper) {
        final EntityPech pech = CatalogPech.PECH.get().create(helper.getLevel());
        final int value = pech.getValue(offerStack());
        pech.discard();
        helper.assertTrue(value > 0, "the probe offer is valued (EntityPech.java:724-745), got " + value);
        return value;
    }

    private PechTradeRollGameTest() {
    }

    private static EntityPech tamedPech(final GameTestHelper helper, final int pechType) {
        final Level level = helper.getLevel();
        final EntityPech pech = CatalogPech.PECH.get().create(level);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        pech.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(pech);
        pech.setTamed(true);
        pech.setPechType(pechType);                                                            // :167-178
        return pech;
    }

    private static Player fakePlayer(final GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.CREATIVE);
    }

    /** One roll: the seeded window, the replay of the same stream, and what the grid shows. */
    private record Roll(PechMenu menu, PechTradeRoll.Plan expected, boolean scam, int value,
                        List<ItemStack> grid) {
    }

    /**
     * Fill the offer, seed the level's random, click the dice, and read the grid —
     * with the expectation replayed on a clone of the seeded stream.
     */
    private static Roll rollWithSeed(final GameTestHelper helper, final EntityPech pech, final int seed) {
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, OFFER_COUNT));
        final Level level = helper.getLevel();
        final int value = pech.getValue(menu.getItems().get(0));
        final List<PechTradeInventory.TradeRow> live = menu.tradeTable();
        final int[] pack = PechTradeRoll.packSlotsOf(pech.lootSlots());

        final RandomSource clone = RandomSource.create(seed);
        final boolean scam = PechTradeRoll.scams(clone.nextInt(100), value);                   // :92
        final int jittered = PechTradeRoll.jitteredValue(value, clone);                        // :98-102
        final PechTradeRoll.Plan expected =
                PechTradeRoll.plan(jittered, clone, live, menu.chestPool(), pack);             // :106-155

        level.random.setSeed(seed);
        helper.assertTrue(menu.clickMenuButton(player, PechMenu.DICE_BUTTON),
                "the dice button is accepted while the gate stands (ContainerPech.java:64-70)");

        final List<ItemStack> grid = new ArrayList<>();
        for (int a = 1; a < 5; a++) {
            final ItemStack stack = menu.getItems().get(a);
            if (!stack.isEmpty()) {
                grid.add(stack.copy());
            }
        }
        return new Roll(menu, expected, scam, value, grid);
    }

    /** The grid's multiset by item id (payouts of the same row merge into one stack). */
    private static Map<String, Integer> tally(final List<ItemStack> stacks) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final ItemStack stack : stacks) {
            counts.merge(builtIn(stack), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static String builtIn(final ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** The multiset the plan's outcomes imply for the grid. */
    private static Map<String, Integer> expectedTally(final PechTradeRoll.Plan plan,
                                                       final EntityPech pech, final Level level) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final PechTradeRoll.Outcome outcome : plan.outcomes()) {
            final String key;
            final int count;
            if (outcome instanceof PechTradeRoll.FromPack fromPack) {
                // :118-120 - the pack pays ONE of the slot's item
                key = builtIn(pech.lootSlots()[fromPack.packSlot()]);
                count = 1;
            } else if (outcome instanceof PechTradeRoll.FromChest fromChest) {
                // :136-139 - the pool pays the row's stack (one item, its meta)
                final ItemStack payout =
                        PechTradeChestPool.resolve(fromChest.row(), level.registryAccess());
                key = builtIn(payout);
                count = payout.getCount();
            } else {
                final ItemStack payout = PechTradeInventory.resolve(
                        ((PechTradeRoll.FromTable) outcome).row(), level.registryAccess());
                key = builtIn(payout);
                count = payout.getCount();
            }
            counts.merge(key, count, Integer::sum);
        }
        return counts;
    }

    /**
     * The seeded roll: the grid carries exactly the rows the same seeded stream
     * produced on the clone, and the offer loses exactly ONE item ({@code :158})
     * whatever was paid.
     */
    @GameTest(template = "empty")
    public static void pechRollPaysTheTradeTableSeeded(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final int value = offerValue(helper);
        helper.assertTrue(value == 4,
                "the valued table gives minecraft:diamond 4 (EntityPech.java:724-745), got " + value);
        final Roll roll = rollWithSeed(helper, pech, 20261113);
        helper.assertTrue(roll.value() == value,
                "the roll runs on the table's value, got " + roll.value() + " want " + value);

        final Map<String, Integer> shown = tally(roll.grid());
        final Map<String, Integer> wanted = expectedTally(roll.expected(), pech, helper.getLevel());
        LOGGER.info("pech seeded roll: value={} scam={} payouts={} leftover={} grid={} expected={}",
                roll.value(), roll.scam(), roll.expected().outcomes().size(), roll.expected().leftover(),
                shown, wanted);
        helper.assertTrue(!roll.grid().isEmpty(), "the roll pays the window (ContainerPech.java:153-155)");
        helper.assertTrue(shown.equals(wanted),
                "the grid carries exactly the rows the seeded stream drew, got " + shown + " want " + wanted);
        int total = 0;
        for (final ItemStack stack : roll.grid()) {
            total += stack.getCount();
        }
        helper.assertTrue(total == roll.expected().outcomes().size(),
                "each payout step inserts ONE item (:119-124,:153-155), got " + total
                        + " for " + roll.expected().outcomes().size() + " steps");
        helper.assertTrue(roll.menu().getItems().get(0).getCount() == OFFER_COUNT - 1,
                "the offer loses exactly one item (:158), got " + roll.menu().getItems().get(0).getCount());
        helper.assertTrue(pech.isTamed() == !roll.scam(),
                "the tamed state follows the scam arm of the same stream (:92-96), scam=" + roll.scam());
        helper.succeed();
    }

    /**
     * The scam arm ({@code :92-96}): {@code nextInt(100) <= value / 2} untames the
     * pech, sets {@code updateAINextTick} and plays {@code thaumcraft:pech_trade}
     * at 0.4F/1.0F. The safe arm does none of that. The seeds are picked by
     * reading the first draw of each stream, so both arms are pinned.
     */
    @GameTest(template = "empty")
    public static void pechScamRollUntamesThePechAndPlaysTheCue(final GameTestHelper helper) {
        final int value = offerValue(helper);
        int scamSeed = -1;
        int safeSeed = -1;
        for (int seed = 0; seed < 1024 && (scamSeed < 0 || safeSeed < 0); seed++) {
            final boolean scammed = PechTradeRoll.scams(RandomSource.create(seed).nextInt(100), value);
            if (scammed && scamSeed < 0) {
                scamSeed = seed;
            }
            if (!scammed && safeSeed < 0) {
                safeSeed = seed;
            }
        }
        helper.assertTrue(scamSeed >= 0 && safeSeed >= 0,
                "the seed scan found both scam arms (scam=" + scamSeed + " safe=" + safeSeed + ")");

        final EntityPech scammed = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final Roll scamRoll = rollWithSeed(helper, scammed, scamSeed);
        helper.assertTrue(scamRoll.scam(), "the chosen seed's first draw is a scam");
        helper.assertTrue(!scammed.isTamed(), "the scam untames the pech (:94)");
        helper.assertTrue(scammed.lastTradeCue() == 1,
                "the scam plays thaumcraft:pech_trade once, 0.4F/1.0F (:95), got " + scammed.lastTradeCue());
        helper.assertTrue(scammed.lastAiUpdateFlag() == 1 && scammed.aiUpdateFlagged(),
                "the scam sets updateAINextTick (:95, the :72 field)");
        helper.assertTrue(scamRoll.menu().getItems().get(0).getCount() == OFFER_COUNT - 1,
                "the scammed trade still consumes the offer item (:158)");

        final EntityPech safe = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final Roll safeRoll = rollWithSeed(helper, safe, safeSeed);
        helper.assertTrue(!safeRoll.scam(), "the safe seed's first draw is over value / 2");
        helper.assertTrue(safe.isTamed(), "the safe arm keeps the pech tamed");
        helper.assertTrue(safe.lastTradeCue() == 0, "the safe arm plays no trade cue, got " + safe.lastTradeCue());
        helper.assertTrue(safe.lastAiUpdateFlag() == 0, "the safe arm flags no AI update");
        helper.assertTrue(safeRoll.menu().getItems().get(0).getCount() == OFFER_COUNT - 1,
                "the safe arm consumes the offer item (:158)");
        helper.succeed();
    }

    /**
     * The {@code :90} gate: an unvalued offer is untouched — the click is refused,
     * the offer keeps every item, the grid stays empty, the pech is untouched.
     */
    @GameTest(template = "empty")
    public static void pechUnvaluedOfferIsUntouched(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.COBBLESTONE, 12));
        helper.assertTrue(!menu.diceReady(), "cobblestone is not valued (EntityPech.java:707-722)");
        helper.assertTrue(!menu.clickMenuButton(player, PechMenu.DICE_BUTTON),
                "the roll gate refuses an unvalued offer (:90)");
        helper.assertTrue(menu.getItems().get(0).getCount() == 12,
                "an unvalued offer keeps every item (:158 is never reached), got "
                        + menu.getItems().get(0).getCount());
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(menu.getItems().get(a).isEmpty(), "no contents appear in cell " + a);
        }
        helper.assertTrue(pech.lastTradeCue() == 0 && pech.lastAiUpdateFlag() == 0 && pech.isTamed(),
                "the pech is untouched");
        helper.succeed();
    }

    /**
     * The {@code :85-89} gate: while ANY output cell is full the roll is refused —
     * the client never draws the dice there ({@code GuiPech.java:34-39}) and the
     * server re-checks it.
     */
    @GameTest(template = "empty")
    public static void pechRollRefusedWhileTheGridIsNotClear(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, OFFER_COUNT));
        helper.assertTrue(menu.diceReady(), "a valued offer with a clear grid lights the dice");
        menu.slots.get(3).set(new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(!menu.diceReady(), "one full cell hides the dice (:89)");
        helper.assertTrue(!menu.clickMenuButton(player, PechMenu.DICE_BUTTON),
                "the roll gate refuses while any cell is full (:85-89)");
        helper.assertTrue(menu.getItems().get(0).getCount() == OFFER_COUNT,
                "the offer is not consumed while the gate is closed, got "
                        + menu.getItems().get(0).getCount());
        helper.assertTrue(menu.getItems().get(3).getCount() == 5, "the cell's own stack is untouched");
        helper.succeed();
    }

    /**
     * Branch (a) ({@code :110-124}): at {@code am == 1} with the coin true and a
     * non-empty pack, one item is copied out of the pech's loot pack into the
     * window and the pack slot is decremented — the pech pays with what it
     * carries, not only out of the table.
     */
    @GameTest(template = "empty")
    public static void pechLootPackPaysAndDecrements(final GameTestHelper helper) {
        int packSeed = -1;
        final EntityPech scout = tamedPech(helper, PechAiLogic.TYPE_WAND);
        scout.setPechType(PechAiLogic.TYPE_WAND);
        // the pack's stack is cobblestone: NO pech trade table pays cobblestone
        // (PechTradeInventory has no cobblestone row in any of the three types), so a
        // cobblestone in the grid can only have come out of the pech's loot pack
        scout.setLootSlot(3, new ItemStack(Items.COBBLESTONE, 2));
        final List<PechTradeInventory.TradeRow> live =
                PechTradeInventory.liveRows(PechAiLogic.TYPE_WAND, helper.getLevel().registryAccess());
        final int value = scout.getValue(offerStack());
        final int[] pack = PechTradeRoll.packSlotsOf(scout.lootSlots());
        helper.assertTrue(pack.length == 1 && pack[0] == 3,
                "one non-empty loot slot is read as slot 3 (:73-81), got " + java.util.Arrays.toString(pack));
        for (int seed = 0; seed < 4096 && packSeed < 0; seed++) {
            // mirror the real draw order on the clone: scam, jitter, payout loop
            final RandomSource clone = RandomSource.create(seed);
            PechTradeRoll.scams(clone.nextInt(100), value);                                    // :92
            final int jittered = PechTradeRoll.jitteredValue(value, clone);                    // :98-102
            if (PechTradeRoll.plan(jittered, clone, live, pack).packPayouts() == 1) {          // :106-157
                packSeed = seed;
            }
        }
        helper.assertTrue(packSeed >= 0, "a seeded stream pays exactly one item from the loot pack");

        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_WAND);
        pech.setLootSlot(3, new ItemStack(Items.COBBLESTONE, 2));
        final Roll roll = rollWithSeed(helper, pech, packSeed);
        final Map<String, Integer> shown = tally(roll.grid());
        LOGGER.info("pech loot pack roll: seed={} payouts={} grid={} pack slot 3={}",
                packSeed, roll.expected().outcomes().size(), shown, pech.lootSlots()[3].getCount());
        helper.assertTrue(shown.getOrDefault("cobblestone", 0) == 1,
                "exactly one cobblestone copied out of the pack into a free cell (:117-120), got " + shown);
        helper.assertTrue(pech.lootSlots()[3].getCount() == 1,
                "the pack slot decrements by one (:121-124), got " + pech.lootSlots()[3].getCount());
        helper.assertTrue(shown.equals(expectedTally(roll.expected(), pech, helper.getLevel())),
                "the grid matches the replayed stream, got " + shown);
        helper.succeed();
    }

    /**
     * BD-74 in the world: the melee pech's table has NO live amount-4 row
     * ({@code itemPickThaumium} is unregistered), so a roll that draws
     * {@code am == 4} can never match. The original's
     * {@code do { … } while (it.get(0) != am);} ({@code :150-152}) spins on that
     * forever, inside the click handler, on the server thread. The bounded search
     * returns the amount and stops: this probe completes, the window shows what
     * the plan says (nothing, in this shape), and the offer still loses its one
     * item.
     */
    @GameTest(template = "empty")
    public static void pechBoundedTableSearchTerminatesOnAGatedAmount(final GameTestHelper helper) {
        final List<PechTradeInventory.TradeRow> live =
                PechTradeInventory.liveRows(PechAiLogic.TYPE_NORMAL, helper.getLevel().registryAccess());
        helper.assertTrue(!PechTradeInventory.amountsOf(live).contains(4),
                "the melee pech's live table carries no amount 4 (itemPickthaumium is gated), got "
                        + PechTradeInventory.amountsOf(live));

        final int value = offerValue(helper);
        int exhaustSeed = -1;
        for (int seed = 0; seed < 4096 && exhaustSeed < 0; seed++) {
            final RandomSource clone = RandomSource.create(seed);
            PechTradeRoll.scams(clone.nextInt(100), value);                                    // :92
            final int jittered = PechTradeRoll.jitteredValue(value, clone);                    // :98-102
            final PechTradeRoll.Plan plan = PechTradeRoll.plan(jittered, clone, live, new int[0]);
            if (plan.exhausted() && plan.outcomes().isEmpty()) {
                exhaustSeed = seed;
            }
        }
        helper.assertTrue(exhaustSeed >= 0, "a seeded stream draws an unmatchable amount and exhausts the search");

        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_NORMAL);
        final Roll roll = rollWithSeed(helper, pech, exhaustSeed);
        LOGGER.info("pech bounded-search roll: seed={} iterations={} leftover={} outcomes={} grid={}",
                exhaustSeed, roll.expected().iterations(), roll.expected().leftover(),
                roll.expected().outcomes().size(), tally(roll.grid()));
        helper.assertTrue(roll.expected().exhausted(), "the bounded search is the arm that ended the roll");
        helper.assertTrue(roll.grid().isEmpty(),
                "nothing is paid for an unmatchable amount, got " + tally(roll.grid()));
        helper.assertTrue(roll.menu().getItems().get(0).getCount() == OFFER_COUNT - 1,
                "the click still consumes the offer item (:158)");
        helper.succeed();
    }

    /**
     * The trade table ({@code EntityPech.java:746-829}) as shipped: every live
     * row's item resolves, the gated rows are excluded, and the enchanted-book
     * rows carry their enchantment — the TC ones from the port's
     * <b>data-registry</b> JSON, the vanilla ones from the vanilla registry. The
     * counts are logged so the CI log carries the evidence.
     */
    @GameTest(template = "empty")
    public static void pechTradeTableIsRegistryGatedAndBooksResolve(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final Map<String, Integer> vanillaSeen = new LinkedHashMap<>();
        for (final int type : new int[]{PechAiLogic.TYPE_NORMAL, PechAiLogic.TYPE_WAND, PechAiLogic.TYPE_BOW}) {
            final List<PechTradeInventory.TradeRow> table = PechTradeInventory.tableFor(type);
            final List<PechTradeInventory.TradeRow> live = PechTradeInventory.liveRows(type, level.registryAccess());
            LOGGER.info("pech trade table type={}: rows={} live={} gated={} amounts={}",
                    type, table.size(), live.size(), table.size() - live.size(),
                    PechTradeInventory.amountsOf(live));
            helper.assertTrue(!live.isEmpty(), "type " + type + " has live rows");
            helper.assertTrue(PechTradeInventory.amountsOf(live).containsAll(List.of(1, 2, 3, 5)),
                    "type " + type + " pays at 1, 2, 3 and 5, got " + PechTradeInventory.amountsOf(live));
            for (final PechTradeInventory.TradeRow row : live) {
                final ItemStack payout = PechTradeInventory.resolve(row, level.registryAccess());
                helper.assertTrue(!payout.isEmpty(), "a live row resolves to a stack: " + row);
                helper.assertTrue(payout.getCount() == 1, "every table entry is one item: " + row);
                if (row.meta() >= 0) {
                    final Integer carried = com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems
                            .variantOf(payout);
                    helper.assertTrue(carried != null,
                            "the original metadata rides the port's variant carrier (block_state variant for a "
                                    + "block family, thaumcraft:item_meta for an item): " + row);
                    if (!(payout.getItem() instanceof net.minecraft.world.item.BlockItem)) {
                        helper.assertTrue(carried == row.meta(), "the meta rides verbatim on item_meta: " + row);
                    }
                }
                if (row.item().getNamespace().equals("minecraft")) {
                    vanillaSeen.merge(row.item().getPath(), 1, Integer::sum);
                }
            }
            helper.assertTrue(table.size() > live.size() || type != PechAiLogic.TYPE_NORMAL,
                    "the melee table ships the gated itemPickthaumium row (EntityPech.java:777) and gates it out,"
                            + " rows=" + table.size() + " live=" + live.size());
        }

        // thaumcraft:ench_haste (data registry, data/thaumcraft/enchantment/ench_haste.json)
        final PechTradeInventory.TradeRow haste = bookRow(PechAiLogic.TYPE_WAND, "ench_haste", level);
        final ItemStack hasteBook = PechTradeInventory.resolve(haste, level.registryAccess());
        helper.assertTrue(hasteBook.is(Items.ENCHANTED_BOOK), "the book row ships an enchanted book (:792)");
        final ItemEnchantments stored = hasteBook.get(DataComponents.STORED_ENCHANTMENTS);
        helper.assertTrue(stored != null, "the payout carries minecraft:stored_enchantments");
        helper.assertTrue(stored.getLevel(level.registryAccess().registryOrThrow(
                        net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "ench_haste"))
                .get()) == 1,
                "EnchantmentData(Config.enchHaste, 1) is level 1 (:792, Config.java:251)");

        // minecraft:power (Enchantment.field_77345_t)
        final PechTradeInventory.TradeRow power = bookRow(PechAiLogic.TYPE_BOW, "power", level);
        helper.assertTrue(PechTradeInventory.resolve(power, level.registryAccess())
                        .get(DataComponents.STORED_ENCHANTMENTS) != null,
                "the vanilla power book carries its enchantment (:817)");

        // the golden-apple split (EntityPech.java:776 / :801 / :823)
        helper.assertTrue(PechTradeInventory.tableFor(PechAiLogic.TYPE_NORMAL).stream()
                        .anyMatch(row -> row.item().getPath().equals("enchanted_golden_apple")),
                "golden apple meta 1 becomes the separate modern item id (:776)");
        LOGGER.info("pech table vanilla payouts seen: {}", vanillaSeen);
        helper.assertTrue(vanillaSeen.keySet().containsAll(
                        List.of("blaze_rod", "potion", "experience_bottle", "golden_apple",
                                "enchanted_golden_apple", "enchanted_book")),
                "the vanilla payouts of :767-:827 resolve live (no content-mapping row exists for vanilla"
                        + " content in this ledger schema), got " + vanillaSeen);
        helper.succeed();
    }

    private static PechTradeInventory.TradeRow bookRow(final int type, final String enchantmentPath,
            final Level level) {
        for (final PechTradeInventory.TradeRow row : PechTradeInventory.liveRows(type, level.registryAccess())) {
            if (row.enchantedBook() && row.enchantment().getPath().equals(enchantmentPath)) {
                return row;
            }
        }
        throw new AssertionError("type " + type + " ships no live book row for " + enchantmentPath);
    }

    /**
     * BD-80 (TC-M7-04-m3-b-c): the {@code dungeonChest} pool as shipped. The 22
     * rows 1.7.10 registers into the hook ({@code Config.java:615,625,637}), the
     * branch's own acceptance filter ({@code :134}), the registry gate on ids this
     * build does not have, and the row metas on the port's component carrier. The
     * last claim is the one that justifies the pool at all: rows the branch can
     * pay exist that no pech trade table carries, so the dice roll that pays them
     * is not a second route to the same loot.
     */
    @GameTest(template = "empty")
    public static void pechChestPoolShipsTheTranscribedRowsAndGatesTheUnregisteredOnes(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        helper.assertTrue(PechTradeChestPool.pool().size() == 22,
                "3 common + 5 uncommon + 14 rare = 22 registrations (Config.java:615,625,637), got "
                        + PechTradeChestPool.pool().size());
        helper.assertTrue(PechTradeChestPool.payable().size() == 14,
                "the branch's filter admits the rareLoot fourteen (:134 weight > 5 || max group > 1), got "
                        + PechTradeChestPool.payable().size());
        for (final PechTradeChestPool.ChestRow row : PechTradeChestPool.pool()) {
            helper.assertTrue(row.minChances() == 1, "every registration's second argument is 1 (addItem, :615,625,637)");
            helper.assertTrue(row.item().getNamespace().equals("thaumcraft"),
                    "the pool names 1.7.10's ids only, no invented content: " + row.item());
        }

        final List<PechTradeChestPool.ChestRow> live = PechTradeChestPool.liveRows(level.registryAccess());
        final List<String> gated = new ArrayList<>();
        for (final PechTradeChestPool.ChestRow row : PechTradeChestPool.pool()) {
            final boolean registered = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(row.item());
            helper.assertTrue(live.contains(row) == registered,
                    "the pool's rows are gated on the live registry alone: " + row.item());
            if (!registered) {
                gated.add(row.item().getPath() + "#" + row.meta());
            }
        }
        helper.assertTrue(!gated.isEmpty(), "the gate has work to do — the pool names ids this build lacks");

        // every live row resolves to a real stack: one item (ChestGenHooks.addItem
        // isStack=false, :403-412) at the row's meta on the port's carrier.
        for (final PechTradeChestPool.ChestRow row : live) {
            final ItemStack payout = PechTradeChestPool.resolve(row, level.registryAccess());
            helper.assertTrue(!payout.isEmpty(), "the live row " + row.item() + " resolves");
            helper.assertTrue(payout.getCount() == 1, "one item per drawn row (:136-137)");
            // the port's metadata convention (PechTradeInventory.applyMeta): every
            // non-block item carries its meta on thaumcraft:item_meta; every pool row
            // is an item, not a block family, so the carrier is the whole story.
            helper.assertTrue(payout.getOrDefault(
                            com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META.get(), 0)
                            .intValue() == row.meta(),
                    "the drawn row's meta rides thaumcraft:item_meta: " + row.item() + "#" + row.meta());
        }

        // the discriminator: rows the branch pays that the pech's trade tables do not carry
        final List<PechTradeChestPool.ChestRow> payableLive = live.stream()
                .filter(PechTradeChestPool::payableByTheBranch).toList();
        helper.assertTrue(!payableLive.isEmpty(), "the branch pays real registered content");
        final List<String> chestOnly = List.of("itemlootbag#2", "itembaubleblanks#3", "itemthaumonomicon#0");
        for (final String key : chestOnly) {
            final PechTradeChestPool.ChestRow row = payableLive.stream()
                    .filter(r -> (r.item().getPath() + "#" + r.meta()).equals(key)).findFirst().orElse(null);
            helper.assertTrue(row != null, key + " is a payable, registered pool row (Config.java:637)");
            for (final int type : List.of(PechAiLogic.TYPE_NORMAL, PechAiLogic.TYPE_WAND, PechAiLogic.TYPE_BOW)) {
                helper.assertTrue(PechTradeInventory.liveRows(type, level.registryAccess()).stream()
                                .noneMatch(trade -> trade.item().equals(row.item())),
                        key + " is in NO pech trade table (type " + type + "): the chest branch is the only"
                                + " route to it, so the roll is not a second route to the table's loot (BD-80)");
            }
        }
        LOGGER.info("pech chest pool: rows={} payable={} live={} gated={} chest-only-rows={} ids-in-no-table={}",
                PechTradeChestPool.pool().size(), PechTradeChestPool.payable().size(), live.size(), gated, chestOnly);
        helper.succeed();
    }

    /**
     * BD-80: the dice roll pays the pool. Seeds are swept for a stream whose
     * replay draws the branch ({@code am >= 4 && nextBoolean()}, {@code :128}) and
     * lands on a row no trade table carries; the seeded menu then runs that
     * stream and the window is compared with the replay, item for item.
     */
    @GameTest(template = "empty")
    public static void pechChestBranchPaysThePoolIntoTheWindow(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper, PechAiLogic.TYPE_WAND);
        final int value = offerValue(helper);
        final List<PechTradeInventory.TradeRow> live =
                PechTradeInventory.liveRows(PechAiLogic.TYPE_WAND, helper.getLevel().registryAccess());
        final List<PechTradeChestPool.ChestRow> chest =
                PechTradeChestPool.liveRows(helper.getLevel().registryAccess());

        int seed = -1;
        for (int s = 0; s < 8192 && seed < 0; s++) {
            final RandomSource clone = RandomSource.create(s);
            PechTradeRoll.scams(clone.nextInt(100), value);                                   // :92
            final int jittered = PechTradeRoll.jitteredValue(value, clone);                   // :98-102
            final PechTradeRoll.Plan plan = PechTradeRoll.plan(jittered, clone, live, chest, new int[0]);
            for (final PechTradeRoll.Outcome outcome : plan.outcomes()) {
                if (outcome instanceof PechTradeRoll.FromChest fromChest
                        && fromChest.row().item().getPath().equals("itemlootbag")
                        && fromChest.row().meta() == 2) {
                    seed = s;                                                                 // the Rare Treasure: no table carries it
                    break;
                }
            }
        }
        helper.assertTrue(seed >= 0, "a seeded stream draws the branch and pays a pool row the table lacks");

        final Roll roll = rollWithSeed(helper, pech, seed);
        final Map<String, Integer> shown = tally(roll.grid());
        final Map<String, Integer> wanted = expectedTally(roll.expected(), pech, helper.getLevel());
        LOGGER.info("pech chest-branch roll: seed={} payouts={} chest={} grid={} expected={}",
                seed, roll.expected().outcomes().size(), roll.expected().chestPayouts(), shown, wanted);
        helper.assertTrue(roll.expected().chestPayouts() >= 1, "the replay draws the chest branch");
        helper.assertTrue(shown.equals(wanted),
                "the window carries the pool row the seeded stream drew, got " + shown + " want " + wanted);
        helper.assertTrue(shown.containsKey("itemlootbag"),
                "the window shows the pool's Rare Treasure, got " + shown);
        helper.assertTrue(roll.menu().getItems().get(0).getCount() == OFFER_COUNT - 1,
                "the click consumes one offer item (:156)");
        helper.succeed();
    }

    /**
     * BD-80 measured: the pool draw over seeded streams. 1.7.10 draws the whole
     * pool uniformly and throws away what the branch's filter rejects
     * ({@code :131-134}), so among the rows that survive — the live payable
     * rows — every one is paid with the same probability, per-row (path + meta),
     * and no unpayable or unregistered row is ever paid.
     */
    @GameTest(template = "empty")
    public static void pechChestPoolPaysTheLiveRowsUniformly(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final List<PechTradeChestPool.ChestRow> live = PechTradeChestPool.liveRows(level.registryAccess());
        final List<PechTradeChestPool.ChestRow> payableLive =
                live.stream().filter(PechTradeChestPool::payableByTheBranch).toList();
        final List<PechTradeInventory.TradeRow> table =
                PechTradeInventory.liveRows(PechAiLogic.TYPE_WAND, level.registryAccess());
        final int value = offerValue(helper);
        final int rolls = 4000;

        final Map<String, Integer> paid = new LinkedHashMap<>();
        int chestPayouts = 0;
        int tablePayouts = 0;
        for (int s = 0; s < rolls; s++) {
            final RandomSource clone = RandomSource.create(s);
            PechTradeRoll.scams(clone.nextInt(100), value);                                   // :92
            final int jittered = PechTradeRoll.jitteredValue(value, clone);                   // :98-102
            final PechTradeRoll.Plan plan = PechTradeRoll.plan(jittered, clone, table, live, new int[0]);
            chestPayouts += plan.chestPayouts();
            tablePayouts += plan.tablePayouts();
            for (final PechTradeRoll.Outcome outcome : plan.outcomes()) {
                if (outcome instanceof PechTradeRoll.FromChest fromChest) {
                    final PechTradeChestPool.ChestRow row = fromChest.row();
                    paid.merge(row.item().getPath() + "#" + row.meta(), 1, Integer::sum);
                    helper.assertTrue(payableLive.contains(row),
                            "the sampler never pays a row the branch rejects or the registry lacks: " + row);
                }
            }
        }
        helper.assertTrue(chestPayouts > 0, "the branch fires over " + rolls + " seeded rolls");
        final double mean = chestPayouts / (double) payableLive.size();
        final double sigma = Math.sqrt(chestPayouts * (1.0 / payableLive.size()) * (1.0 - 1.0 / payableLive.size()));
        for (final PechTradeChestPool.ChestRow row : payableLive) {
            final int count = paid.getOrDefault(row.item().getPath() + "#" + row.meta(), 0);
            helper.assertTrue(count > 0,
                    "the live payable row " + row.item() + "#" + row.meta() + " is paid at least once over "
                            + chestPayouts + " chest payouts");
            helper.assertTrue(Math.abs(count - mean) <= 6.0 * sigma,
                    "the pool draw is uniform per row: " + row.item() + "#" + row.meta() + " paid " + count
                            + ", mean " + mean + ", 6 sigma " + (6.0 * sigma));
        }
        helper.assertTrue(paid.size() == payableLive.size(),
                "exactly the live payable rows paid, got " + paid.size() + " of " + payableLive.size());
        LOGGER.info("pech chest pool distribution over {} rolls: chest payouts={} table payouts={}"
                        + " rows={} mean/row={} 6sigma={} paid={}",
                rolls, chestPayouts, tablePayouts, payableLive.size(), String.format("%.1f", mean),
                String.format("%.1f", 6.0 * sigma), paid);
        helper.succeed();
    }
}
