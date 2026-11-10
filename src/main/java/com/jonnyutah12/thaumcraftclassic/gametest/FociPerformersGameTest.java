package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.casting.CastTargets;
import com.jonnyutah12.thaumcraftclassic.casting.EffectPerformers;
import com.jonnyutah12.thaumcraftclassic.casting.FocusSpells;
import com.jonnyutah12.thaumcraftclassic.casting.SpellEffect;
import com.jonnyutah12.thaumcraftclassic.casting.SpellEffects;
import com.jonnyutah12.thaumcraftclassic.casting.WandCasting;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-03-p probes: the effect performers as the player meets them.
 *
 * <p>The seven projectiles, {@code blockWarded}, {@code blockHole} and the
 * swapper queue are partner cards' content (TC-M6-06, warding, portable
 * hole, equal trade). Each probe pins the card-wide rule in world: a cast
 * whose content has not landed refuses <em>before</em> any world change and
 * charges nothing — not one centi-vis, and not the portable hole's
 * per-distance tax the spell body charges up front. The excavation focus is
 * the one performer that is pure machinery, so its probe digs for real on the
 * original's own billing: every channeled tick pays, a tick on a new target
 * records zero progress, and the tick that reaches hardness pays a second
 * time, breaks the block and pops its drops.</p>
 *
 * <p>Where a probe is gated on content that may land later, the gate is the
 * assertion: the moment the entity/block/queue registers, the gate flips to
 * the performed-effect path, and this probe says so. The parent's
 * {@code FociBehaviorsGameTest} cleans a probe recorder out of the seam at
 * the end of its run, so every probe here re-registers the real performers
 * first.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public class FociPerformersGameTest {

    /** All eleven performer keys, plus the parent's native bolt, are live. */
    @GameTest(template = "empty")
    public static void everyEffectKeyIsPerformedByThisCard(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll(); // self-heal the seam for probe order
        for (final String key : EffectPerformers.KEYS) {
            helper.assertTrue(SpellEffects.isRegistered(key), "no performer for " + key);
        }
        sweepPlayers(helper);
        helper.succeed();
    }

    /**
     * The fire focus's base combination is the per-tick ember path
     * ({@code ItemFocusFire.java:104-137}), and {@code thaumcraft:ember} is
     * TC-M6-06's entity row: while it is unlanded, a paid tick reports no
     * effect and the wand's vis stands exactly where it was.
     */
    @GameTest(template = "empty")
    public static void projectileFociRefundWhileTheirEntitiesAreUnlanded(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll(); // self-heal the seam for probe order
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("fire").get());
        ItemWandCasting.setFocus(wand, focus);
        final AspectList cost = fund(wandItem, wand, focus);
        final int before = totalVis(wandItem, wand, cost);

        final boolean landed = BuiltInRegistries.ENTITY_TYPE
                .containsKey(ResourceLocation.parse(SpellEffect.Projectile.EMBER));
        helper.assertTrue(wandItem.getVis(wand, cost.getAspects().get(0)) >= 1000,
                "the probe funds the wand before the first paid tick");
        final WandCasting.CastResult start =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(start.status() == WandCasting.Status.CHANNELED,
                "the fire focus opens its ember channel, got " + start.status());
        helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                "opening the channel charges nothing, " + before + " -> " + totalVis(wandItem, wand, cost));

        final WandCasting.CastResult tick =
                WandCasting.channelTick(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        if (landed) {
            // TC-M6-06 has landed: the embers spawn and the tick is billed.
            helper.assertTrue(tick.status() == WandCasting.Status.CAST,
                    "with the ember entity landed the channel fires, got " + tick.status());
        } else {
            helper.assertTrue(tick.status() == WandCasting.Status.NO_EFFECT,
                    "an unlanded projectile must report no effect, got " + tick.status());
            helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                    "the refund must restore the whole tick, " + before + " -> " + totalVis(wandItem, wand, cost));
        }
        sweepPlayers(helper);
        helper.succeed();
    }

    /**
     * The warding cast against plain ground, with no {@code blockWarded}:
     * the handler refuses before touching the world, and the vis stands.
     */
    @GameTest(template = "empty")
    public static void wardingRefusesBeforeChangingTheWorld(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll();
        final BlockPos wallPos = new BlockPos(1, 3, 2);
        helper.setBlock(wallPos, Blocks.STONE.defaultBlockState());
        final Player player = aimAt(helper, 1, 0.0F);

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("warding").get());
        ItemWandCasting.setFocus(wand, focus);
        final AspectList cost = fund(wandItem, wand, focus);
        final int before = totalVis(wandItem, wand, cost);

        final boolean landed = BuiltInRegistries.BLOCK
                .containsKey(EffectPerformers.WARDED_BLOCK);
        final WandCasting.CastResult result =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        if (landed) {
            helper.assertTrue(result.status() == WandCasting.Status.CAST,
                    "with blockWarded landed the ward lands, got " + result.status());
        } else {
            helper.assertTrue(result.status() == WandCasting.Status.NO_EFFECT,
                    "the warding gate must refuse, got " + result.status());
            helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                    "a refused ward charges nothing, " + before + " -> " + totalVis(wandItem, wand, cost));
            helper.assertTrue(helper.getBlockState(wallPos).is(Blocks.STONE),
                    "the refused ward must not change the world");
        }
        sweepPlayers(helper);
        helper.succeed();
    }

    /**
     * The portable hole scan prices the cast up front (base + base x the
     * walked distance); with no {@code blockHole} the whole cast — tax
     * included — comes back and the wall stands. A wand funded only for the
     * base is refused, which is the proof that the scan's tax is really
     * demanded on top of the base.
     */
    @GameTest(template = "empty")
    public static void portableHoleRefundsTheDistanceTax(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll();
        final BlockPos wallPos = new BlockPos(1, 3, 2);
        helper.setBlock(wallPos, Blocks.STONE.defaultBlockState());
        final Player player = aimAt(helper, 1, 0.0F);

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("portablehole").get());
        ItemWandCasting.setFocus(wand, focus);
        final AspectList cost = fund(wandItem, wand, focus);
        final AspectList effective =
                wandItem.effectiveCost(wand, player, cost, false);

        // The body's own scan: the number of blocks it walks, which is what
        // prices the cast (ItemFocusPortableHole.java:101-137).
        final BlockHitResult hit = CastTargets.pointedBlock(helper.getLevel(), player);
        helper.assertTrue(hit != null, "the probe must point at the wall");
        final int distance = CastTargets.holeDistance(helper.getLevel(), hit.getBlockPos(),
                hit.getDirection(), FocusSpells.PORTABLE_HOLE_MAX_DISTANCE);
        helper.assertTrue(distance >= 1, "the scan walks past the pointed block, got distance "
                + distance + " — a zero-distance cast would not prove the tax");

        final boolean landed = BuiltInRegistries.BLOCK.containsKey(EffectPerformers.HOLE_BLOCK);
        if (!landed) {
            // Funded only for the base: the tax is not affordable, so the cast
            // is refused and the base is put straight back.
            for (final Aspect aspect : cost.getAspects()) {
                wandItem.storeVis(wand, aspect, effective.getAmount(aspect));
            }
            final int baseOnly = totalVis(wandItem, wand, cost);
            final WandCasting.CastResult shortCast =
                    WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
            helper.assertTrue(shortCast.status() == WandCasting.Status.NO_EFFECT,
                    "a hole the wand cannot pay for is refused, got " + shortCast.status());
            helper.assertTrue(totalVis(wandItem, wand, cost) == baseOnly,
                    "an unaffordable hole spends nothing, " + baseOnly + " -> " + totalVis(wandItem, wand, cost));
        }

        // Funded for base + tax: the body charges the whole price, the
        // performer refuses, and the refund must be the WHOLE cast.
        for (final Aspect aspect : cost.getAspects()) {
            wandItem.storeVis(wand, aspect, (distance + 1) * effective.getAmount(aspect));
        }
        final int before = totalVis(wandItem, wand, cost);
        final WandCasting.CastResult result =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        if (landed) {
            helper.assertTrue(result.status() == WandCasting.Status.CAST,
                    "with blockHole landed the hole opens, got " + result.status());
        } else {
            helper.assertTrue(result.status() == WandCasting.Status.NO_EFFECT,
                    "the hole gate must refuse, got " + result.status());
            // The body charged base + distance*base; the seam refunded base,
            // the performer must refund the tax — so the pool is whole again.
            helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                    "the refused hole must refund the whole cast including its "
                            + distance + "-block tax, " + before + " -> " + totalVis(wandItem, wand, cost));
            helper.assertTrue(helper.getBlockState(wallPos).is(Blocks.STONE),
                    "the refused hole must not change the world");
        }
        sweepPlayers(helper);
        helper.succeed();
    }

    /**
     * The equal-trade focus: a sneak-pick stores the pointed block on the
     * focus for free (the original's {@code "picked"} path), and a swap cast
     * with no queue registered charges nothing.
     */
    @GameTest(template = "empty")
    public static void equalTradePicksForFreeAndSwapsOnlyThroughTheQueue(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll();
        final BlockPos wallPos = new BlockPos(1, 3, 2);
        helper.setBlock(wallPos, Blocks.STONE.defaultBlockState());
        final Player player = aimAt(helper, 1, 0.0F);

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("trade").get());
        ItemWandCasting.setFocus(wand, focus);
        final AspectList cost = fund(wandItem, wand, focus);
        final int before = totalVis(wandItem, wand, cost);

        // Sneak-pick: performed, and the original charged it nothing
        // (ItemFocusTrade.java:93-95). The pick lands on the focus the wand
        // carries; the port rebuilds that stack from the tag on every cast, so
        // the pick rides the wand's snapshot (ItemWandCasting.setFocusPicked).
        player.setShiftKeyDown(true);
        final WandCasting.CastResult pick =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        final ItemStack carried = ItemWandCasting.focusItem(wand);
        helper.assertTrue(ItemFocus.getPickedBlock(carried).is(Items.STONE),
                "the sneak-pick stores the pointed block on the focus the wand carries, got "
                        + ItemFocus.getPickedBlock(carried) + " (cast status " + pick.status() + ")");
        helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                "the pick is free, " + before + " -> " + totalVis(wandItem, wand, cost));

        // Swap: with no swapper queue landed, refuse, refund, change nothing.
        final boolean queueLanded =
                com.jonnyutah12.thaumcraftclassic.casting.SwapperBridge.get() != null;
        final WandCasting.CastResult swap =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        if (!queueLanded) {
            helper.assertTrue(swap.status() == WandCasting.Status.NO_EFFECT,
                    "a swap with no queue must report no effect, got " + swap.status());
            helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                    "the refused swap charges nothing, " + before + " -> " + totalVis(wandItem, wand, cost));
            helper.assertTrue(helper.getBlockState(wallPos).is(Blocks.STONE),
                    "the refused swap must not change the world");
        }
        sweepPlayers(helper);
        helper.succeed();
    }

    /**
     * The dig for real, on the original's own billing: every channeled tick
     * pays ({@code ItemFocusExcavation.java:111} is the tick body's first
     * statement), a tick that points at a new block only records it
     * ({@code :214-219} zeroes the progress and adds no speed), and the tick
     * that reaches hardness pays a SECOND time and breaks the block with its
     * drops ({@code :205}).
     */
    @GameTest(template = "empty")
    public static void excavationPaysEveryTickAndBreaksOnTheCompletingOne(final GameTestHelper helper) {
        sweepPlayers(helper);
        EffectPerformers.registerAll();
        final BlockPos digPos = new BlockPos(1, 3, 2);
        helper.setBlock(digPos, Blocks.DIRT.defaultBlockState());
        final Player player = aimAt(helper, 1, 0.0F);

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("excavation").get());
        ItemWandCasting.setFocus(wand, focus);
        final AspectList cost = fund(wandItem, wand, focus);
        final int before = totalVis(wandItem, wand, cost);

        final WandCasting.CastResult start =
                WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(start.status() == WandCasting.Status.CHANNELED,
                "excavation opens a channel, got " + start.status());
        helper.assertTrue(totalVis(wandItem, wand, cost) == before,
                "starting the channel must not charge, got " + totalVis(wandItem, wand, cost));
        final int tickCost = wandItem.effectiveCost(wand, player, cost, false)
                .getAmount(cost.getAspects().get(0));
        helper.assertTrue(tickCost > 0, "the dig is priced, the probe needs the price");

        // Tick 1 — a new target: recorded, no progress added (the original's
        // lastX/lastY/lastZ branch), and the tick itself is charged.
        final WandCasting.CastResult tick1 =
                WandCasting.channelTick(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(tick1.status() == WandCasting.Status.CAST,
                "the channel continues while digging, got " + tick1.status());
        helper.assertTrue(helper.getBlockState(digPos).is(Blocks.DIRT),
                "the first tick must not break the block");
        helper.assertTrue(totalVis(wandItem, wand, cost) == before - tickCost,
                "every channeled tick pays its cost, " + before + " -> "
                        + totalVis(wandItem, wand, cost) + " (cost " + tickCost + ")");
        com.jonnyutah12.thaumcraftclassic.casting.DigState progress =
                wand.get(ModDataComponents.WAND_DIG_STATE.get());
        helper.assertTrue(progress != null && progress.isAt(helper.absolutePos(digPos)),
                "the dig state tracks the pointed block");
        helper.assertTrue(progress != null && progress.progress() == 0.0F,
                "a new target records zero progress, got "
                        + (progress == null ? "no dig state" : progress.progress()));

        // Tick 2 — soft dirt adds its 0.25 of 0.5 hardness: progress, block intact.
        final WandCasting.CastResult tick2 =
                WandCasting.channelTick(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(tick2.status() == WandCasting.Status.CAST,
                "the progress tick fires, got " + tick2.status());
        helper.assertTrue(helper.getBlockState(digPos).is(Blocks.DIRT),
                "one progress tick must not break the block");
        helper.assertTrue(totalVis(wandItem, wand, cost) == before - 2 * tickCost,
                "the progress tick pays its cost too, " + before + " -> " + totalVis(wandItem, wand, cost));
        progress = wand.get(ModDataComponents.WAND_DIG_STATE.get());
        helper.assertTrue(progress != null && progress.progress() == 0.25F,
                "soft dirt accumulates softSpeed per tick, got "
                        + (progress == null ? "no dig state" : progress.progress()));

        // Tick 3 — 0.5 >= hardness: the block breaks, drops, and the tick pays
        // twice (the tick's own cost plus the original's completion charge).
        final WandCasting.CastResult tick3 =
                WandCasting.channelTick(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
        helper.assertTrue(tick3.status() == WandCasting.Status.CAST,
                "the completing tick fires, got " + tick3.status());
        helper.assertTrue(helper.getBlockState(digPos).isAir(),
                "the completed dig breaks the block");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(helper.absolutePos(digPos))).isEmpty(),
                "the broken block pops its drops");
        helper.assertTrue(totalVis(wandItem, wand, cost) == before - 4 * tickCost,
                "the completing tick pays its tick cost plus the dig cost, " + before + " -> "
                        + totalVis(wandItem, wand, cost) + " (cost " + tickCost + ")");
        helper.assertTrue(wand.get(ModDataComponents.WAND_DIG_STATE.get()) == null,
                "a completed dig clears the dig state");
        sweepPlayers(helper);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Discard the harness players this probe class creates.
     *
     * <p>{@code makeMockPlayer} leaves its player in the shared level, and the
     * batch packs every probe into one grid, so a leaked player is the nearest
     * living entity for a neighbour probe. That is not cosmetic: the shock
     * focus's bolt refuses a player target on a non-PVP server
     * ({@code ItemFocusShock.java:191-236}, the parent's port of it), so a probe
     * that aims at a cow reads NO_EFFECT because of what a sibling left behind.
     * The sweep is scoped to this probe's own cell (16 blocks; the batch packs
     * at 31, so this reaches this probe's players and no neighbour's), never the
     * whole level.</p>
     */
    private static void sweepPlayers(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final AABB cell = new AABB(origin).inflate(16.0D);
        for (final Player stray : helper.getLevel().getEntities(EntityType.PLAYER, cell, entity -> true)) {
            stray.discard();
        }
    }


    /**
     * A mock survival player one cell back with their feet in the cell at
     * local {@code y + 1}, looking along +Z at the pitch. Eye height is 1.62,
     * so the eye sits inside the cell at local {@code y + 2}: a block placed
     * at local {@code (1, 3, 2)} is 3.5 blocks straight ahead at eye level —
     * inside the 4.5 default block-reach — and its north face is what
     * {@code pointedBlock} returns. Pitch 0 pins the crosshair on that block;
     * a probe wanting a top-face target asks for a pitch.
     */
    private static Player aimAt(final GameTestHelper helper, final int x, final float pitch) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(base.getX() - 1 + x + 0.5, base.getY() + 1.0, base.getZ() - 1.5);
        player.setYRot(0.0F);
        player.setXRot(pitch);
        return player;
    }

    /** Fund every aspect the combination prices, and hand the cost back. */
    private static AspectList fund(final ItemWandCasting wandItem, final ItemStack wand,
                                   final ItemStack focus) {
        final AspectList cost = focus.getItem() instanceof final ItemFocus item
                ? item.getVisCost(focus) : new AspectList();
        for (final Aspect aspect : cost.getAspects()) {
            wandItem.addRealVis(wand, aspect, 2000, true);
        }
        return cost;
    }

    /** The wand's whole charge over the cost's aspects — what a cast must restore. */
    private static int totalVis(final ItemWandCasting wandItem, final ItemStack wand,
                                final AspectList cost) {
        int total = 0;
        for (final Aspect aspect : cost.getAspects()) {
            total += wandItem.getVis(wand, aspect);
        }
        return total;
    }
}
