package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryProvider;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemAmuletRunic;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemAmuletVis;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemBaubleBlanks;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemGirdleHover;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemGirdleRunic;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemRingRunic;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogBaubles;
import com.jonnyutah12.thaumcraftclassic.runic.RunicShielding;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.WornGearDiscounts;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M8-02 acceptance: the six bauble items and the runic shielding pool in
 * the live world, under BOTH runtime configurations (Curios and the
 * fallback provider — CI runs the suite twice, see AccessoryGameTest).
 *
 * <p>Each test pins one original behaviour from {@code common/items/baubles/}
 * or {@code common/lib/events/EventHandlerRunic.java}; the pure tables are
 * unit-tested in BaubleRulesTest / RunicShieldingRulesTest — here the
 * wiring: slot routing through the active provider, the shield scan/recharge/
 * absorption cycle, the vis amulet transfer and research gate, the
 * apprentice-ring discount aggregation, and the hover girdle fall step.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaubleGameTest {

    private BaubleGameTest() {
    }

    private static AccessoryProvider provider() {
        return AccessoryService.provider();
    }

    // ------------------------------------------------- runic shielding pool

    @GameTest(template = "empty")
    public static void runicBaublesBuildShieldPoolWithOriginalCapacities(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Ring meta1 (5) + charged ring meta2 (4, charged flag) in the two ring
        // slots, amulet meta0 (8), girdle meta0 (10): EventHandlerRunic 52-84.
        helper.assertTrue(provider().equip(player, AccessorySlot.RING, 0,
                        CatalogBaubles.ITEM_RING_RUNIC.get().stack(ItemRingRunic.META_GREATER)),
                "greater ring equip failed");
        helper.assertTrue(provider().equip(player, AccessorySlot.RING, 1,
                        CatalogBaubles.ITEM_RING_RUNIC.get().stack(ItemRingRunic.META_CHARGED)),
                "charged ring equip failed");
        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_AVATAR)),
                "runic amulet equip failed");
        helper.assertTrue(provider().equip(player, AccessorySlot.BELT, 0,
                        CatalogBaubles.ITEM_GIRDLE_RUNIC.get().stack(ItemGirdleRunic.META_VIS)),
                "runic girdle equip failed");

        RunicShielding.markDirty();
        RunicShielding.tick(player, System.currentTimeMillis());

        final var row = RunicShielding.currentRow(player);
        helper.assertTrue(row.max() == 5 + 4 + 8 + 10,
                "shield capacity " + row.max() + " (expected 27)");
        helper.assertTrue(row.charged() == 1, "charged flag count " + row.charged());
        helper.assertTrue(row.kinetic() == 0 && row.healing() == 0 && row.emergency() == 0,
                "unexpected upgrade flags");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hardeningComponentAddsToCapacity(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        final ItemStack ring = CatalogBaubles.ITEM_RING_RUNIC.get().stack(ItemRingRunic.META_PROTECTION);
        ring.set(ModDataComponents.RUNIC_HARDENING.get(), 3);
        helper.assertTrue(provider().equip(player, AccessorySlot.RING, 0, ring), "ring equip failed");

        RunicShielding.markDirty();
        RunicShielding.tick(player, System.currentTimeMillis());
        // getFinalCharge (EventHandlerRunic.java:343-354): base 1 + harden 3.
        helper.assertTrue(RunicShielding.currentRow(player).max() == 4,
                "hardened capacity " + RunicShielding.currentRow(player).max() + " (expected 4)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shieldRechargePaysCentiVisFromInventory(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_AVATAR)),
                "runic amulet equip failed");

        // A vis amulet in the main inventory pays AIR+EARTH runic_cost each
        // unit (WandManager.consumeVisFromInventory, WandManager.java:88-111).
        final ItemStack visAmulet = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_LESSER);
        var pool = WandVis.poolOf(visAmulet);
        pool = WandVis.withVis(pool, Aspect.getAspect("aer"), 1_000);
        pool = WandVis.withVis(pool, Aspect.getAspect("terra"), 1_000);
        WandVis.setPool(visAmulet, pool);
        player.getInventory().setItem(0, visAmulet);

        final long now = System.currentTimeMillis();
        RunicShielding.markDirty();
        RunicShielding.tick(player, now); // scan
        RunicShielding.tick(player, now); // first unit (nextCycle starts at 0)

        helper.assertTrue(RunicShielding.currentCharge(player) == 1,
                "charge after first recharge " + RunicShielding.currentCharge(player));
        final var paid = WandVis.poolOf(player.getInventory().getItem(0));
        final int cost = TcConfig.getInt(TcConfigCatalog.RUNIC_COST);
        helper.assertTrue(paid.getVis(Aspect.getAspect("aer")) == 1_000 - cost
                        && paid.getVis(Aspect.getAspect("terra")) == 1_000 - cost,
                "recharge did not pay runic_cost centi-vis of aer+terra");

        // Second unit must wait the interval (nextCycle gate, line 125-127).
        RunicShielding.tick(player, now);
        helper.assertTrue(RunicShielding.currentCharge(player) == 1,
                "recharged twice in one interval");
        RunicShielding.tick(player, now + 2_000_000_000L);
        helper.assertTrue(RunicShielding.currentCharge(player) == 2,
                "recharge did not resume after the interval");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void absorptionDepletesPoolAndFiresUpgrades(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Amulet meta1 (7, emergency) + ring meta3 (4, healing): on depletion
        // the pool refills to min(max, 8) and Regeneration lands (231-253).
        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_EMERGENCY)),
                "emergency amulet equip failed");
        helper.assertTrue(provider().equip(player, AccessorySlot.RING, 0,
                        CatalogBaubles.ITEM_RING_RUNIC.get().stack(ItemRingRunic.META_REVITALIZING)),
                "revitalizing ring equip failed");

        final long now = System.currentTimeMillis();
        RunicShielding.markDirty();
        RunicShielding.tick(player, now);
        RunicShielding.tick(player, now); // charge 1 (no vis needed: pay fails? no —)

        // No vis containers: recharge cannot pay; drive the pool via a second
        // gear set instead — simpler: assert absorption of the 1 charge we have.
        int charge = RunicShielding.currentCharge(player);
        helper.assertTrue(charge == 0,
                "recharge without any vis container should fail, got " + charge);

        // Give the pool a unit by paying from a vis amulet, then absorb.
        final ItemStack visAmulet = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_LESSER);
        var pool = WandVis.withVis(WandVis.poolOf(visAmulet), Aspect.getAspect("aer"), 1_000);
        pool = WandVis.withVis(pool, Aspect.getAspect("terra"), 1_000);
        WandVis.setPool(visAmulet, pool);
        player.getInventory().setItem(0, visAmulet);
        RunicShielding.tick(player, now + 2_000_000_000L);
        helper.assertTrue(RunicShielding.currentCharge(player) == 1, "pool did not charge");

        final float left = RunicShielding.hurt(player,
                player.level().damageSources().generic(), 5.0F, now);
        helper.assertTrue(Math.abs(left - 4.0F) < 1e-4,
                "damage after absorbing 1 charge: " + left + " (expected 4.0)");
        helper.assertTrue(RunicShielding.currentCharge(player) == 8,
                "emergency refill wrong: " + RunicShielding.currentCharge(player) + " (expected min(11,8)=8)");
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                "revitalizing ring did not apply Regeneration");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockedDamageSourcesBypassTheShield(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_AVATAR)),
                "runic amulet equip failed");

        final long now = System.currentTimeMillis();
        RunicShielding.markDirty();
        RunicShielding.tick(player, now);
        final var visAmulet = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_LESSER);
        var pool = WandVis.withVis(WandVis.poolOf(visAmulet), Aspect.getAspect("aer"), 1_000);
        pool = WandVis.withVis(pool, Aspect.getAspect("terra"), 1_000);
        WandVis.setPool(visAmulet, pool);
        player.getInventory().setItem(0, visAmulet);
        RunicShielding.tick(player, now + 2_000_000_000L);
        helper.assertTrue(RunicShielding.currentCharge(player) == 1, "pool did not charge");

        // EventHandlerRunic.java:176-181 — wither never touches the pool.
        final float left = RunicShielding.hurt(player,
                player.level().damageSources().wither(), 3.0F, now);
        helper.assertTrue(Math.abs(left - 3.0F) < 1e-4, "wither damage was absorbed");
        helper.assertTrue(RunicShielding.currentCharge(player) == 1, "wither drained the pool");
        helper.succeed();
    }

    // ------------------------------------------------------------- lifecycle

    /**
     * Acceptance: equip → effect active → unequip → effect gone. The pool
     * exists while the gear is worn and is gone on the next scan after the
     * provider removes it (the original's onUnequipped isDirty=true,
     * ItemAmuletRunic.java:71-73 + EventHandlerRunic.java:96-100).
     */
    @GameTest(template = "empty")
    public static void unequippingRunicGearEmptiesThePool(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_AVATAR)),
                "runic amulet equip failed");

        final long now = System.currentTimeMillis();
        RunicShielding.markDirty();
        RunicShielding.tick(player, now);
        helper.assertTrue(RunicShielding.currentRow(player).max() == 8,
                "pool not built while gear worn: " + RunicShielding.currentRow(player).max());

        provider().unequip(player, AccessorySlot.AMULET, 0);
        RunicShielding.tick(player, now);
        helper.assertTrue(RunicShielding.currentRow(player).max() == 0,
                "pool survived unequip: " + RunicShielding.currentRow(player).max());
        helper.assertTrue(RunicShielding.currentCharge(player) == 0,
                "charge survived unequip: " + RunicShielding.currentCharge(player));
        helper.succeed();
    }

    /**
     * Acceptance: persistence across relog. The worn set and the vis amulet's
     * stored charge survive the save round-trip; the shield pool is runtime
     * state the original never saved either (EventHandlerRunic.java:30-32)
     * and rebuilds from the worn gear on the first scan after reconnect.
     */
    @GameTest(template = "empty")
    public static void baublesAndEffectsPersistAcrossRelog(final GameTestHelper helper) {
        RunicShielding.resetForTest();
        final Player original = helper.makeMockPlayer(GameType.SURVIVAL);

        final ItemStack visAmulet = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_LESSER);
        var pool = WandVis.withVis(WandVis.poolOf(visAmulet), Aspect.getAspect("aer"), 1_000);
        pool = WandVis.withVis(pool, Aspect.getAspect("terra"), 1_000);
        WandVis.setPool(visAmulet, pool);
        original.getInventory().setItem(0, visAmulet);
        helper.assertTrue(provider().equip(original, AccessorySlot.AMULET, 0,
                        CatalogBaubles.ITEM_AMULET_RUNIC.get().stack(ItemAmuletRunic.META_AVATAR)),
                "runic amulet equip failed");

        final CompoundTag saved = original.saveWithoutId(new CompoundTag());
        final Player reconnected = helper.makeMockPlayer(GameType.SURVIVAL);
        reconnected.load(saved);

        boolean stillWorn = false;
        for (final ItemStack worn : AccessoryService.wornStacks(reconnected)) {
            if (worn.is(CatalogBaubles.ITEM_AMULET_RUNIC.get())) {
                stillWorn = true;
            }
        }
        helper.assertTrue(stillWorn, "runic amulet not worn after relog");

        final var carried = WandVis.poolOf(reconnected.getInventory().getItem(0));
        helper.assertTrue(carried.getVis(Aspect.getAspect("aer")) == 1_000
                        && carried.getVis(Aspect.getAspect("terra")) == 1_000,
                "vis amulet charge lost across relog");

        RunicShielding.markDirty();
        RunicShielding.tick(reconnected, System.currentTimeMillis());
        helper.assertTrue(RunicShielding.currentRow(reconnected).max() == 8,
                "shield pool did not rebuild after relog: "
                        + RunicShielding.currentRow(reconnected).max());
        helper.succeed();
    }

    // ------------------------------------------------------------- vis amulet

    @GameTest(template = "empty")
    public static void visAmuletTransfersToHeldContainerEveryFifthTick(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        final ItemStack worn = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_LESSER);
        WandVis.setPool(worn, WandVis.withVis(WandVis.poolOf(worn), Aspect.getAspect("aer"), 100));
        helper.assertTrue(provider().equip(player, AccessorySlot.AMULET, 0, worn), "vis amulet equip failed");

        // The held container: another vis amulet standing in for the wand —
        // the original duck-typed ItemWandCasting; the port uses the shared
        // VisContainerItem surface (ItemAmuletVis.onWornTick, lines 74-92).
        final ItemStack held = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_GREATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);

        CatalogBaubles.ITEM_AMULET_VIS.get().onWornTick(worn, player);

        final var heldPool = WandVis.poolOf(player.getItemInHand(InteractionHand.MAIN_HAND));
        final var wornPool = WandVis.poolOf(worn);
        helper.assertTrue(heldPool.getVis(Aspect.getAspect("aer")) == 5,
                "transfer moved " + heldPool.getVis(Aspect.getAspect("aer")) + " (expected 5)");
        helper.assertTrue(wornPool.getVis(Aspect.getAspect("aer")) == 95,
                "source not debited: " + wornPool.getVis(Aspect.getAspect("aer")));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void greaterVisAmuletGatedOnResearch(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack greater = CatalogBaubles.ITEM_AMULET_VIS.get().stack(ItemAmuletVis.META_GREATER);

        // ItemAmuletVis.canEquip (line 197): VISAMULET research required.
        helper.assertTrue(!provider().canEquip(player, AccessorySlot.AMULET, 0, greater),
                "greater vis amulet equipped without VISAMULET research");

        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                player.getData(ModAttachments.PLAYER_KNOWLEDGE)
                        .withResearchCompleted(ItemAmuletVis.RESEARCH_KEY));
        helper.assertTrue(provider().canEquip(player, AccessorySlot.AMULET, 0, greater),
                "greater vis amulet rejected after VISAMULET research");
        helper.succeed();
    }

    // ----------------------------------------------------------- blanks + discount

    @GameTest(template = "empty")
    public static void blankSlotRoutingIsPerStackUnderActiveProvider(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemBaubleBlanks blanks = CatalogBaubles.ITEM_BAUBLE_BLANKS.get();

        helper.assertTrue(provider().canEquip(player, AccessorySlot.AMULET, 0, blanks.stack(0)),
                "amulet blank rejected");
        helper.assertTrue(!provider().canEquip(player, AccessorySlot.AMULET, 0, blanks.stack(1)),
                "ring blank accepted in the amulet slot");
        helper.assertTrue(provider().canEquip(player, AccessorySlot.RING, 0, blanks.stack(1)),
                "ring blank rejected");
        helper.assertTrue(provider().canEquip(player, AccessorySlot.BELT, 0, blanks.stack(2)),
                "belt blank rejected");
        helper.assertTrue(provider().canEquip(player, AccessorySlot.RING, 0, blanks.stack(5)),
                "apprentice ring rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void apprenticeRingDiscountAggregatesFromWornGear(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(provider().equip(player, AccessorySlot.RING, 0,
                        CatalogBaubles.ITEM_BAUBLE_BLANKS.get().stack(3)),
                "apprentice ring (aer) equip failed");

        final Aspect aer = Aspect.getAspect("aer");
        final Aspect ignis = Aspect.getAspect("ignis");
        // WandManager.getTotalVisDiscount (49-84): 1% for the matching primal.
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(player, aer) == 1,
                "aer discount total " + WornGearDiscounts.gearPercentTotal(player, aer));
        helper.assertTrue(WornGearDiscounts.gearPercentTotal(player, ignis) == 0,
                "non-matching primal got a discount");
        helper.succeed();
    }

    // ----------------------------------------------------------- hover girdle

    @GameTest(template = "empty")
    public static void hoverGirdleReducesFallDistanceWhileWorn(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack girdle = CatalogBaubles.ITEM_GIRDLE_HOVER.get().stack(0);
        helper.assertTrue(provider().equip(player, AccessorySlot.BELT, 0, girdle), "hover girdle equip failed");

        player.fallDistance = 1.0F;
        CatalogBaubles.ITEM_GIRDLE_HOVER.get().onWornTick(girdle, player);
        helper.assertTrue(Math.abs(player.fallDistance - 0.67F) < 1e-4,
                "fall distance after worn tick: " + player.fallDistance);
        helper.succeed();
    }

    // ------------------------------------------------------------- display parity

    @GameTest(template = "empty")
    public static void variantStacksCarryOriginalRarityAndMeta(final GameTestHelper helper) {
        final ItemRingRunic ring = CatalogBaubles.ITEM_RING_RUNIC.get();
        final ItemStack lesser = ring.stack(ItemRingRunic.META_PROTECTION);
        final ItemStack charged = ring.stack(ItemRingRunic.META_CHARGED);

        helper.assertTrue(lesser.has(ModDataComponents.ITEM_META.get()) == false
                        || storedMeta(lesser) == 0,
                "meta 0 must not carry a non-zero meta component");
        helper.assertTrue(storedMeta(charged) == 2, "charged ring meta component wrong");
        // func_77613_e parity: meta0 uncommon (registered default), upgrades rare.
        helper.assertTrue(lesser.getRarity() == Rarity.UNCOMMON,
                "lesser ring rarity " + lesser.getRarity());
        helper.assertTrue(charged.getRarity() == Rarity.RARE,
                "charged ring rarity " + charged.getRarity());
        helper.succeed();
    }

    private static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }
}
