package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOre;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * SPINE-1 probes: the {@code blockcustomore} drop tables.
 *
 * <p>Original contract, {@code reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockCustomOre.java}
 * getDrops (lines 100-113) and getExpDrop (lines 115-120), verbatim:</p>
 * <ul>
 *   <li>meta 0 (cinnabar) drops the ore block itself, exactly 1 — it is
 *       furnace-smelted to quicksilver, so the ore has to survive the pick.</li>
 *   <li>metas 1-6 (infused stone) drop {@code itemShard} meta {@code md-1};
 *       {@code q = 1 + rand.nextInt(2 + fortune)} separate stacks of 1 — the
 *       loop in getDrops adds q single-item stacks, not one stack of q.</li>
 *   <li>meta 7 (amber bearing stone) drops {@code itemResource} meta 6 (Amber),
 *       count {@code 1 + rand.nextInt(fortune + 1)}.</li>
 *   <li>exp: metas 1-6 → MathHelper.getRandomIntegerInRange(rand, 0, 3);
 *       meta 7 → rand(1, 4) inclusive; meta 0 → the vanilla default (0).</li>
 * </ul>
 *
 * <p>Silk touch: the original getDrops has NO silk branch, and 1.7.10 vanilla
 * never reaches it under silk touch — {@code Block.harvestBlock} (same shape
 * in the CraftBukkit 1.8 mirror, {@code Block.a(Level,EntityHuman,BlockPos,int)}
 * and in the 1.7.10 decompiles: silk + canSilkHarvest → drop
 * the block item, bypassing getDrops) short-circuits to the block item, and
 * {@code BlockCustomOre.canSilkHarvest} returns true
 * ({@code BlockCustomOre.java:73-75}) with {@code damageDropped(meta) = meta}
 * ({@code func_149692_a}, line 77-79). So the silk break must drop the ore
 * block itself, variant preserved — this probe pins that.</p>
 *
 * <p>Dispatch path: the probes break real placed blocks through the shipped
 * vanilla paths — {@code Level.destroyBlock(pos, true, entity)} (drops
 * computed by the loot table with an empty tool) and
 * {@code Block.playerDestroy(level, player, pos, state, null, tool)} — the
 * hook {@code ServerPlayerGameMode.destroyBlock} calls with the player's real
 * tool, so fortune/silk-touch read the enchanted pickaxe exactly as in play.
 * No internal methods.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OreDropGameTest {

    private OreDropGameTest() {
    }

    private static final int SAMPLES_BASE = 25;
    private static final int SAMPLES_FORTUNE = 60;

    private static BlockState oreState(final int variant) {
        return ModBlocks.BLOCK_CUSTOM_ORE.get().defaultBlockState().setValue(BlockCustomOre.VARIANT, variant);
    }

    /**
     * The only item ids the shipped {@code blockcustomore} loot table can
     * emit. Game-test structures share one level and pack by template size, so
     * a neighbouring probe's cell can sit within the sweep box and its own
     * dropped stacks (measured: a vanilla {@code stone_button}) show up in the
     * box. The drop under test is identified by identity, not by "whatever is
     * in the box" — a probe that asserts on the raw box contents is a
     * flake that moves every time the batch grows.
     */
    private static boolean isOreDrop(final ItemStack stack) {
        return stack.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get())
                || stack.is(CatalogResources.ITEM_SHARD.get())
                || stack.is(CatalogResources.ITEM_RESOURCE.get());
    }

    /** The item stacks the break just spawned near pos, as stacks. */
    private static List<ItemStack> dropsAround(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        final List<ItemStack> all = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)
                .stream().map(ItemEntity::getItem).toList();
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
        helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).forEach(ExperienceOrb::discard);
        final List<ItemStack> foreign = all.stream().filter(stack -> !isOreDrop(stack)).toList();
        if (!foreign.isEmpty()) {
            ThaumcraftClassic.LOGGER.info("[SPINE-1] {} foreign stack(s) in the drop box at {} — a neighbouring "
                    + "structure in the shared game-test level, not this block's drop: {}",
                    foreign.size(), pos, foreign);
        }
        return all.stream().filter(OreDropGameTest::isOreDrop).toList();
    }

    /** Sums the experience value of the orbs the break just spawned. */
    private static int experienceAround(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        final int total = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box)
                .stream().map(ExperienceOrb::getValue).mapToInt(Integer::intValue).sum();
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
        helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).forEach(ExperienceOrb::discard);
        return total;
    }


    /** The stacks this probe's own break produced: those carrying the item it owns.
     *
     *  The scan box is inflate(2.0) around the broken cell and the harness packs probe
     *  structures by the biggest template in the batch, so a neighbouring pad's cells can
     *  sit inside that box. With the eldritch templates in the batch the cinnabar probe's
     *  box overlapped a grass cell and its wheat_seeds drop answered the ore contract
     *  ("got 2 stacks [1 minecraft:wheat_seeds, 1 thaumcraft:blockcustomore]"). The
     *  contract is about the ore, so the assertion runs on the owned subset and the
     *  foreign stacks are NAMED in the failure message rather than counted. A real
     *  regression still bites: a duplicated loot pool grows the owned subset, and a drop
     *  that changed item or stopped happening empties it. */
    private static List<ItemStack> ownedBy(final List<ItemStack> stacks, final Item owner) {
        return stacks.stream().filter(stack -> stack.is(owner)).toList();
    }

    private static List<ItemStack> foreignTo(final List<ItemStack> stacks, final Item owner) {
        return stacks.stream().filter(stack -> !stack.is(owner)).toList();
    }

    /** The shipped player-break dispatch: ServerPlayerGameMode.destroyBlock calls exactly this. */
    private static void playerBreak(final GameTestHelper helper, final BlockPos pos, final ItemStack tool) {
        final BlockState state = helper.getLevel().getBlockState(pos);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        state.getBlock().playerDestroy(helper.getLevel(), player, pos, state, null, tool);
    }

    private static ItemStack enchantedPickaxe(final GameTestHelper helper,
                                               final net.minecraft.resources.ResourceKey<
                                                       net.minecraft.world.item.enchantment.Enchantment> key,
                                               final int level) {
        final ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        if (key != null) {
            pickaxe.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(key), level);
        }
        return pickaxe;
    }

    /** The meta carried on a shard/resource drop (the original damage value). */
    private static int metaOf(final ItemStack stack) {
        final Integer meta = stack.get(ModDataComponents.ITEM_META.get());
        return meta == null ? 0 : meta;
    }

    // ---------------------------------------------------------------- 0

    @GameTest(template = "empty_24")
    public static void cinnabardropsitselfexactlyone(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int sample = 0; sample < 3; sample++) {
            helper.setBlock(1, 1, 1, oreState(BlockCustomOre.VARIANT_CINNABAR));
            helper.getLevel().destroyBlock(pos, true, null);
            final List<ItemStack> drops = dropsAround(helper, pos);
            final List<ItemStack> ore = ownedBy(drops, ModItems.BLOCK_CUSTOM_ORE_ITEM.get());
            helper.assertTrue(ore.size() == 1,
                    "cinnabar ore drops exactly the block, got " + ore.size() + " ore stacks " + ore
                            + "; foreign stacks in the scan box (neighbouring pad): "
                            + foreignTo(drops, ModItems.BLOCK_CUSTOM_ORE_ITEM.get()));
            final ItemStack drop = ore.get(0);
            helper.assertTrue(drop.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get()),
                    "cinnabar ore drops the ore block itself (it is furnace-smelted to quicksilver), got " + drop);
            helper.assertTrue(drop.getCount() == 1, "cinnabar drop is exactly 1, got " + drop.getCount());
            helper.assertTrue(metaOf(drop) == 0, "the cinnabar drop carries meta 0, got " + metaOf(drop));
        }
        helper.succeed();
    }

    // ------------------------------------------------------------ 1..6

    @GameTest(template = "empty_24")
    public static void infusedstonedropsitsprimalshards(final GameTestHelper helper) {
        for (int variant = BlockCustomOre.VARIANT_INFUSED_MIN; variant <= BlockCustomOre.VARIANT_INFUSED_MAX;
             variant++) {
            final int x = 1 + 3 * variant;
            final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
            int observedMin = Integer.MAX_VALUE;
            int observedMax = 0;
            for (int sample = 0; sample < SAMPLES_BASE; sample++) {
                helper.setBlock(x, 1, 1, oreState(variant));
                helper.getLevel().destroyBlock(pos, true, null);
                final List<ItemStack> drops = dropsAround(helper, pos);
                final List<ItemStack> shards = ownedBy(drops, CatalogResources.ITEM_SHARD.get());
                final int count = shards.size();
                observedMin = Math.min(observedMin, count);
                observedMax = Math.max(observedMax, count);
                helper.assertTrue(count >= 1 && count <= 2,
                        "infused stone v" + variant + " drops 1..2 shard stacks (1 + rand.nextInt(2 + 0)), got "
                                + count + " shard stacks " + shards + "; foreign stacks in the scan box "
                                + "(neighbouring pad): " + foreignTo(drops, CatalogResources.ITEM_SHARD.get()));
                for (final ItemStack drop : shards) {
                    helper.assertTrue(drop.is(CatalogResources.ITEM_SHARD.get()),
                            "infused stone v" + variant + " drops itemshard, got " + drop);
                    helper.assertTrue(metaOf(drop) == variant - 1,
                            "infused stone v" + variant + " drops shard meta " + (variant - 1)
                                    + " (original getDrops: meta md-1), got " + metaOf(drop));
                    helper.assertTrue(drop.getCount() == 1,
                            "the original adds q single-item stacks (loop over q), not one stack of q, got " + drop);
                }
            }
            helper.assertTrue(observedMin == 1 && observedMax == 2,
                    "infused stone v" + variant + " must hit both ends of 1..2 across " + SAMPLES_BASE
                            + " breaks, observed " + observedMin + ".." + observedMax);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- 7

    @GameTest(template = "empty_24")
    public static void amberbearingdropsamberexactlyone(final GameTestHelper helper) {
        final int x = 22;
        final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
        for (int sample = 0; sample < SAMPLES_BASE; sample++) {
            helper.setBlock(x, 1, 1, oreState(BlockCustomOre.VARIANT_AMBER));
            helper.getLevel().destroyBlock(pos, true, null);
            final List<ItemStack> drops = dropsAround(helper, pos);
            final List<ItemStack> amber = ownedBy(drops, CatalogResources.ITEM_RESOURCE.get());
            helper.assertTrue(amber.size() == 1,
                    "amber bearing stone drops exactly one resource stack, got " + amber.size()
                            + " resource stacks " + amber + "; foreign stacks in the scan box (neighbouring pad): "
                            + foreignTo(drops, CatalogResources.ITEM_RESOURCE.get()));
            final ItemStack drop = amber.get(0);
            helper.assertTrue(drop.is(CatalogResources.ITEM_RESOURCE.get()),
                    "amber bearing stone drops itemresource, got " + drop);
            helper.assertTrue(metaOf(drop) == 6,
                    "amber is itemresource meta 6 (original: new ItemStack(ConfigItems.itemResource, ..., 6)), got "
                            + metaOf(drop));
            // 1 + rand.nextInt(0 + 1) is 1 every time at fortune 0 — assert it exactly.
            helper.assertTrue(drop.getCount() == 1,
                    "1 + rand.nextInt(fortune + 1) is exactly 1 at fortune 0, got " + drop.getCount());
        }
        helper.succeed();
    }

    // --------------------------------------------------------- silk touch

    @GameTest(template = "empty_24")
    public static void silktouchdropstheblockwithitsvariant(final GameTestHelper helper) {
        final ItemStack silk = enchantedPickaxe(helper, Enchantments.SILK_TOUCH, 1);
        for (int variant = 0; variant <= 7; variant++) {
            final int x = 1 + 3 * variant;
            final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
            helper.setBlock(x, 1, 1, oreState(variant));
            playerBreak(helper, pos, silk);
            final List<ItemStack> drops = dropsAround(helper, pos);
            final List<ItemStack> ore = ownedBy(drops, ModItems.BLOCK_CUSTOM_ORE_ITEM.get());
            helper.assertTrue(ore.size() == 1,
                    "a silk-touched v" + variant + " ore drops exactly one stack, got " + ore.size()
                            + " ore stacks " + ore + "; foreign stacks in the scan box (neighbouring pad): "
                            + foreignTo(drops, ModItems.BLOCK_CUSTOM_ORE_ITEM.get()));
            final ItemStack drop = ore.get(0);
            helper.assertTrue(drop.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get()),
                    "silk touch drops the ore block itself (1.7.10 harvestBlock bypasses getDrops — no silk branch "
                            + "there), got " + drop);
            final net.minecraft.world.item.component.BlockItemStateProperties blockState =
                    drop.get(DataComponents.BLOCK_STATE);
            helper.assertTrue(blockState != null,
                    "the silk drop carries the block_state component, got " + drop);
            helper.assertTrue(blockState != null
                            && java.util.Objects.equals(blockState.get(BlockCustomOre.VARIANT), variant),
                    "the silk drop preserves variant " + variant + ", got "
                            + drop.get(DataComponents.BLOCK_STATE));
        }
        helper.succeed();
    }

    // ------------------------------------------------------------ fortune

    @GameTest(template = "empty_24")
    public static void fortunescalestheoriginalcurves(final GameTestHelper helper) {
        final ItemStack fortune3 = enchantedPickaxe(helper, Enchantments.FORTUNE, 3);
        for (int variant = BlockCustomOre.VARIANT_INFUSED_MIN; variant <= BlockCustomOre.VARIANT_INFUSED_MAX;
             variant++) {
            final int x = 1 + 3 * variant;
            final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
            int observedMin = Integer.MAX_VALUE;
            int observedMax = 0;
            for (int sample = 0; sample < SAMPLES_FORTUNE; sample++) {
                helper.setBlock(x, 1, 1, oreState(variant));
                playerBreak(helper, pos, fortune3);
                final List<ItemStack> drops = dropsAround(helper, pos);
                final List<ItemStack> shards = ownedBy(drops, CatalogResources.ITEM_SHARD.get());
                final int count = shards.size();
                observedMin = Math.min(observedMin, count);
                observedMax = Math.max(observedMax, count);
                helper.assertTrue(count >= 1 && count <= 5,
                        "fortune III infused stone v" + variant + " drops 1..5 shards (1 + rand.nextInt(2 + 3)), got "
                                + count + " shard stacks " + shards + "; foreign stacks in the scan box "
                                + "(neighbouring pad): " + foreignTo(drops, CatalogResources.ITEM_SHARD.get()));
                for (final ItemStack drop : shards) {
                    helper.assertTrue(drop.is(CatalogResources.ITEM_SHARD.get())
                                    && metaOf(drop) == variant - 1 && drop.getCount() == 1,
                            "every fortune drop is one size-1 shard stack of meta " + (variant - 1)
                                    + ", got " + drop);
                }
            }
            helper.assertTrue(observedMin == 1 && observedMax == 5,
                    "fortune III infused stone v" + variant + " must hit both ends of 1..5 across "
                            + SAMPLES_FORTUNE + " breaks, observed " + observedMin + ".." + observedMax);
        }

        final int amberX = 22;
        final BlockPos amberPos = helper.absolutePos(new BlockPos(amberX, 1, 1));
        int amberMin = Integer.MAX_VALUE;
        int amberMax = 0;
        for (int sample = 0; sample < SAMPLES_FORTUNE; sample++) {
            helper.setBlock(amberX, 1, 1, oreState(BlockCustomOre.VARIANT_AMBER));
            playerBreak(helper, amberPos, fortune3);
            final List<ItemStack> drops = dropsAround(helper, amberPos);
            final List<ItemStack> amber = ownedBy(drops, CatalogResources.ITEM_RESOURCE.get());
            helper.assertTrue(amber.size() == 1,
                    "amber drops one resource stack, got " + amber.size() + " resource stacks " + amber
                            + "; foreign stacks in the scan box (neighbouring pad): "
                            + foreignTo(drops, CatalogResources.ITEM_RESOURCE.get()));
            final ItemStack drop = amber.isEmpty() ? ItemStack.EMPTY : amber.get(0);
            final int count = drop.getCount();
            amberMin = Math.min(amberMin, count);
            amberMax = Math.max(amberMax, count);
            helper.assertTrue(count >= 1 && count <= 4,
                    "fortune III amber drops 1..4 (1 + rand.nextInt(3 + 1)), got " + count);
            helper.assertTrue(drop.is(CatalogResources.ITEM_RESOURCE.get()) && metaOf(drop) == 6,
                    "fortune amber stays itemresource meta 6, got " + drop);
        }
        helper.assertTrue(amberMin == 1 && amberMax == 4,
                "fortune III amber must hit both ends of 1..4 across " + SAMPLES_FORTUNE
                        + " breaks, observed " + amberMin + ".." + amberMax);
        helper.succeed();
    }

    // ------------------------------------------------------------- exp

    @GameTest(template = "empty_24")
    public static void expdropmatchestheoriginalcurve(final GameTestHelper helper) {
        final ItemStack pickaxe = enchantedPickaxe(helper, null, 0);
        for (int variant = BlockCustomOre.VARIANT_INFUSED_MIN; variant <= BlockCustomOre.VARIANT_INFUSED_MAX;
             variant++) {
            final int x = 1 + 3 * variant;
            final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
            int observedMin = Integer.MAX_VALUE;
            int observedMax = 0;
            long total = 0;
            for (int sample = 0; sample < SAMPLES_FORTUNE; sample++) {
                helper.setBlock(x, 1, 1, oreState(variant));
                playerBreak(helper, pos, pickaxe);
                // experienceAround sweeps the item drops too — do NOT sweep
                // dropsAround first, it discards the orbs before the scan.
                final int exp = experienceAround(helper, pos);
                observedMin = Math.min(observedMin, exp);
                observedMax = Math.max(observedMax, exp);
                total += exp;
                helper.assertTrue(exp >= 0 && exp <= 3,
                        "infused stone v" + variant + " exp is MathHelper.rand(0,3), got " + exp);
            }
            helper.assertTrue(observedMin <= 1 && observedMax >= 2,
                    "infused stone v" + variant + " exp must spread over 0..3, observed "
                            + observedMin + ".." + observedMax);
            helper.assertTrue(total * 10 >= SAMPLES_FORTUNE * 9 && total * 10 <= SAMPLES_FORTUNE * 21,
                    "infused stone v" + variant + " exp mean is ~1.5 over " + SAMPLES_FORTUNE
                            + " breaks, got total " + total);
        }

        final int amberX = 22;
        final BlockPos amberPos = helper.absolutePos(new BlockPos(amberX, 1, 1));
        int amberMin = Integer.MAX_VALUE;
        int amberMax = 0;
        for (int sample = 0; sample < SAMPLES_FORTUNE; sample++) {
            helper.setBlock(amberX, 1, 1, oreState(BlockCustomOre.VARIANT_AMBER));
            playerBreak(helper, amberPos, pickaxe);
            final int exp = experienceAround(helper, amberPos);
            amberMin = Math.min(amberMin, exp);
            amberMax = Math.max(amberMax, exp);
            helper.assertTrue(exp >= 1 && exp <= 4,
                    "amber exp is MathHelper.rand(1,4), got " + exp);
        }
        helper.assertTrue(amberMin == 1 && amberMax == 4,
                "amber exp must hit both ends of 1..4 across " + SAMPLES_FORTUNE
                        + " breaks, observed " + amberMin + ".." + amberMax);

        final BlockPos cinnabarPos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int sample = 0; sample < 20; sample++) {
            helper.setBlock(1, 1, 1, oreState(BlockCustomOre.VARIANT_CINNABAR));
            playerBreak(helper, cinnabarPos, pickaxe);
            final int exp = experienceAround(helper, cinnabarPos);
            helper.assertTrue(exp == 0, "cinnabar exp is the vanilla default (0), got " + exp);
        }
        helper.succeed();
    }
}
