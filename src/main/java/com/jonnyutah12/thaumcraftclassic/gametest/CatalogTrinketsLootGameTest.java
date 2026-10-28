package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.entities.TrunkHost;
import com.jonnyutah12.thaumcraftclassic.items.ItemCompassStone;
import com.jonnyutah12.thaumcraftclassic.items.ItemLootBag;
import com.jonnyutah12.thaumcraftclassic.items.ItemTrunkSpawner;
import com.jonnyutah12.thaumcraftclassic.loot.LootRoller;
import com.jonnyutah12.thaumcraftclassic.menus.HandMirrorMenu;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M6-04e acceptance: trinkets, curios tools and loot containers.
 *
 * <ul>
 *   <li>{@link #lootContainerRowsAreRegistered}: every contract id of this
 *       card (blocks, items, data components, menus) resolves in the live
 *       registries.</li>
 *   <li>{@link #lootContainersRollOriginalTables}: urn/crate drops follow the
 *       original {@code Utils.generateLoot} shape — {@code 1 + rarity +
 *       rand(3)} items from the card's loot tables — silk touch keeps the
 *       block with its rarity, and pick-block preserves the rarity on
 *       ITEM_META.</li>
 *   <li>{@link #treasureBagCarriesOriginalMetaNames}: the single bag id
 *       exposes the original 0/1/2 metas through ITEM_META + per-meta names.</li>
 *   <li>{@link #trunkSpawnerWaitsForTrunkEntity}: with the trunk entity
 *       (TC-M6-06) absent the spawner is a no-op and is not consumed.</li>
 *   <li>{@link #handMirrorMenuLayout}: the hand-mirror menu is the original
 *       1 input slot over the 36-slot player inventory.</li>
 *   <li>{@link #lodestoneAnnouncementsExpire}: the original client-only
 *       announcement body prunes entries older than the original 10 s bound and
 *       clears the active icon, while the server-side {@code inventoryTick}
 *       leaves the announcements untouched (the original's
 *       {@code world.field_72995_K} gate); a seeded roll of each bag table must
 *       resolve to registered items.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogTrinketsLootGameTest {

    private CatalogTrinketsLootGameTest() {
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    @GameTest(template = "empty")
    public static void lootContainerRowsAreRegistered(final GameTestHelper helper) {
        for (final String path : List.of("blocklooturn", "blocklootcrate")) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(path)),
                    "block contract id " + path + " is not registered");
        }
        for (final String path : List.of("blocklooturn", "blocklootcrate", "itemlootbag",
                "itemthaumometer", "itemresonator", "itemcompassstone", "handmirror", "trunkspawner")) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(path)),
                    "item contract id " + path + " is not registered");
        }
        for (final String path : List.of("mirror_link", "trunk_upgrade", "trunk_inventory")) {
            helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id(path)),
                    "data component " + path + " is not registered");
        }
        for (final String path : List.of("gui_hand_mirror", "gui_trunk")) {
            helper.assertTrue(BuiltInRegistries.MENU.containsKey(id(path)),
                    "menu " + path + " is not registered");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lootContainersRollOriginalTables(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final Block urn = CatalogTrinketsLoot.BLOCK_LOOT_URN.get();

        for (final int rarity : List.of(BlockLoot.RARITY_COMMON, BlockLoot.RARITY_UNCOMMON,
                BlockLoot.RARITY_RARE)) {
            final BlockState state = urn.defaultBlockState().setValue(BlockLoot.VARIANT, rarity);
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(1, 1, 1, state);

            final List<ItemStack> drops = Block.getDrops(state, level, pos, null);
            helper.assertTrue(drops.size() >= 1 + rarity && drops.size() <= 3 + rarity,
                    "rarity-" + rarity + " container must drop 1+rarity..3+rarity items, got " + drops.size());
            for (final ItemStack drop : drops) {
                helper.assertTrue(!drop.isEmpty(), "rolled drop must not be empty");
            }
        }

        // Silk touch keeps the container itself with its rarity preserved.
        final BlockState rare = urn.defaultBlockState().setValue(BlockLoot.VARIANT, BlockLoot.RARITY_RARE);
        final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(net.minecraft.core.Holder.direct(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)), 1);
        final List<ItemStack> kept = Block.getDrops(rare,
                level, new BlockPos(1, 1, 1), null, null, tool);
        helper.assertTrue(kept.size() == 1 && kept.get(0).is(urn.asItem())
                        && kept.get(0).getOrDefault(ModDataComponents.ITEM_META.get(), -1) == BlockLoot.RARITY_RARE,
                "silk touch must drop the rare urn itself with ITEM_META preserved, got " + kept);

        // Pick-block preserves the rarity (original kept block metadata).
        final ItemStack picked = rare.getBlock().getCloneItemStack(rare,
                new BlockHitResult(Vec3.atCenterOf(new BlockPos(1, 1, 1)), net.minecraft.core.Direction.UP,
                        new BlockPos(1, 1, 1), false),
                level, new BlockPos(1, 1, 1), null);
        helper.assertTrue(picked.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == BlockLoot.RARITY_RARE,
                "pick-block must preserve the rarity on ITEM_META");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void treasureBagCarriesOriginalMetaNames(final GameTestHelper helper) {
        final ItemLootBag bag = CatalogTrinketsLoot.ITEM_LOOT_BAG.get();
        for (final int meta : List.of(0, 1, 2)) {
            final ItemStack stack = ItemLootBag.withRarity(bag, meta);
            helper.assertTrue(stack.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == meta,
                    "bag meta " + meta + " must be stored on ITEM_META");
            final Component name = bag.getName(stack);
            helper.assertTrue(name.getContents()
                            instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                            && ("item.thaumcraft.itemlootbag." + meta).equals(contents.getKey()),
                    "bag meta " + meta + " must use the per-meta name key");
            // TC-LOC-s: the key must resolve to a shipped en_us.json string —
            // a raw key in the display name is the defect this card removes.
            helper.assertTrue(!name.getString().contains("item.thaumcraft.itemlootbag."),
                    "bag meta " + meta + " renders its raw translation key; en_us.json has no entry for it");
        }
        helper.assertTrue(ItemLootBag.withRarity(bag, 0).getMaxStackSize() == 16,
                "treasure bags stack to 16 (original)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trunkSpawnerWaitsForTrunkEntity(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack stack = new ItemStack(CatalogTrinketsLoot.TRUNK_SPAWNER.get());
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);

        final UseOnContext ctx = new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(new BlockPos(1, 1, 1)), net.minecraft.core.Direction.UP,
                        new BlockPos(1, 1, 1), false));
        final net.minecraft.world.InteractionResult result =
                stack.getItem().useOn(ctx);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS,
                "with the trunk entity (TC-M6-06) unregistered the spawner must be a no-op");
        helper.assertTrue(stack.getCount() == 1, "the spawner must not be consumed when nothing spawned");
        helper.assertTrue(TrunkHost.class.isInterface(), "the TrunkHost seam must exist for TC-M6-06");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void handMirrorMenuLayout(final GameTestHelper helper) {
        final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final HandMirrorMenu menu = new HandMirrorMenu(1, player.getInventory());
        // Original ContainerHandMirror: 1 mirror input slot + 36 player slots.
        helper.assertTrue(menu.slots.size() == 37,
                "hand mirror menu must have 1 input + 36 player slots, got " + menu.slots.size());
        helper.assertTrue(menu.getSlot(0).x == 80 && menu.getSlot(0).y == 24,
                "the mirror input slot must sit at the original (80,24) offset, got "
                        + menu.getSlot(0).x + "," + menu.getSlot(0).y);
        helper.assertTrue(menu.getSlot(0).container != player.getInventory(),
                "slot 0 is the menu's own mirror input container, not the player inventory");
        helper.assertTrue(menu.getSlot(36).x == 152 && menu.getSlot(36).y == 142,
                "the last menu slot is the hotbar row at the original y=142");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lodestoneAnnouncementsExpire(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 3, 3);
        ItemCompassStone.noteSinisterNode(pos, helper.getLevel().dimension());
        helper.assertTrue(!ItemCompassStone.sinisterNodes().isEmpty(),
                "a dark-node announcement must be recorded");
        // Force the entry stale (original pruned announcements older than 10 s).
        final var key = ItemCompassStone.sinisterNodes().keySet().iterator().next();
        ItemCompassStone.sinisterNodes().put(key, System.currentTimeMillis() - 11_000L);
        final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemCompassStone stone =
                (ItemCompassStone) CatalogTrinketsLoot.ITEM_COMPASS_STONE.get();
        final ItemStack stack = new ItemStack(stone);
        // The original ran this body only when world.field_72995_K (client) was
        // set; a game-test server is not client-side, so inventoryTick must
        // leave the announcements untouched.
        stack.getItem().inventoryTick(stack, helper.getLevel(), player, 0, true);
        helper.assertTrue(!ItemCompassStone.sinisterNodes().isEmpty(),
                "the server-side tick must not run the original client-only body");
        // The client body itself: same code, reached directly, must prune past
        // the original 10 s bound and clear the active icon.
        stone.updateAnnouncements(helper.getLevel(), player);
        helper.assertTrue(ItemCompassStone.sinisterNodes().isEmpty(),
                "announcements older than the original 10 s bound must be pruned");
        helper.assertTrue(!stone.isActive(), "no node is in view, the icon must be inactive");
        // The bag tables themselves must resolve: a seeded roll of each rarity
        // reaches at least one registered item.
        final RandomSource seeded = RandomSource.create(0x04eL);
        for (final int rarity : List.of(BlockLoot.RARITY_COMMON, BlockLoot.RARITY_UNCOMMON,
                BlockLoot.RARITY_RARE)) {
            var resolved = 0;
            for (int i = 0; i < 25; i++) {
                if (!LootRoller.generateLoot(rarity, seeded).isEmpty()) {
                    resolved++;
                }
            }
            helper.assertTrue(resolved > 0,
                    "the rarity-" + rarity + " bag table must resolve to registered items, got " + resolved
                            + " of 25 seeded rolls");
        }
        helper.succeed();
    }
}
