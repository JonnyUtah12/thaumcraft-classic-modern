package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaHandler;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.M404RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M404Infusion;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * TC-M4-04 acceptance: the infusion altar end-to-end on the live server
 * (design record: docs/architecture/infusion.md).
 *
 * <p>{@link #fullInfusionCraftConsumesPedestalsAndCenter} builds the real
 * altar shape (runic matrix two above the central arcane pedestal, four
 * infusion pillars at the corners — {@code validLocation}), places a center
 * item plus a point-mirrored pair of component pedestals (symmetry 0, so no
 * instability events fire and the run is deterministic), injects a synthetic
 * infusion recipe into the live {@link RecipeManager}, activates the matrix
 * through the empty-hand use seam (the original wand-click path), and waits
 * for the block-entity ticker to run the full craft cycle: essentia drains
 * from a nearby {@code IAspectSource}, each pedestal component burns its
 * 5-tick FX countdown and is consumed, and the center item is replaced by the
 * recipe output.</p>
 *
 * <p>The 63 original crafting + 24 enchantment recipes shipped as data are
 * guarded structurally by {@code InfusionRecipeDataTest}; their ingredients
 * reference catalog items owned by cards that have not landed yet, so the
 * runtime path is exercised here with test-probe items instead.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfusionAltarGameTest {

    private InfusionAltarGameTest() {
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void fullInfusionCraftConsumesPedestalsAndCenter(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockStoneDevice device = M404Infusion.BLOCK_STONE_DEVICE.get();

        // --- altar geometry (original validLocation): matrix at y+2 above the
        // central arcane pedestal (blockStoneDevice variant 1), pillars of
        // variant 3 at the four corners of the pedestal layer.
        final BlockPos centre = helper.absolutePos(new BlockPos(3, 1, 3));
        final BlockPos matrixPos = centre.above(2);
        level.setBlock(centre, device.withVariant(1), 3);
        level.setBlock(matrixPos, device.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX), 3);
        for (final BlockPos corner : List.of(
                new BlockPos(2, 1, 2), new BlockPos(2, 1, 4), new BlockPos(4, 1, 2), new BlockPos(4, 1, 4))) {
            level.setBlock(helper.absolutePos(corner), device.withVariant(BlockStoneDevice.VARIANT_PILLAR_A), 3);
        }
        // Two component pedestals as a point-mirrored pair: symmetry sums to 0,
        // recipe instability is 0, so no failure events can fire mid-craft.
        final BlockPos pedA = helper.absolutePos(new BlockPos(5, 1, 3));
        final BlockPos pedB = helper.absolutePos(new BlockPos(1, 1, 3));
        level.setBlock(pedA, device.withVariant(1), 3);
        level.setBlock(pedB, device.withVariant(1), 3);

        // --- essentia source inside the matrix's 12-block drain range
        EssentiaHandler.clearCache();
        final BlockPos sourcePos = helper.absolutePos(new BlockPos(3, 1, 6));
        level.setBlock(sourcePos, TestProbeContent.PROBE_ESSENTIA_SOURCE.get().defaultBlockState(), 3);
        final TestProbeContent.ProbeEssentiaSource source =
                (TestProbeContent.ProbeEssentiaSource) level.getBlockEntity(sourcePos);
        helper.assertTrue(source != null, "essentia source block entity missing");
        final Aspect aer = Aspect.getAspect("aer");
        helper.assertTrue(aer != null, "aer aspect not registered");
        source.addAspect(aer, 10);

        // --- synthetic recipe (test scaffolding; see class doc)
        final ItemStack centreItem = new ItemStack(TestProbeContent.PROBE_ITEM.get());
        final ItemStack componentItem = new ItemStack(TestProbeContent.PROBE_COMPONENT.get());
        final ItemStack outputItem = new ItemStack(TestProbeContent.PROBE_OUTPUT.get(), 2);
        final AspectList aspects = new AspectList();
        aspects.add(aer, 2);
        final InfusionCraftingRecipe recipe = new InfusionCraftingRecipe(
                "TC-M4-04-gametest", "", 0, aspects,
                Ingredient.of(centreItem),
                List.of(Ingredient.of(componentItem), Ingredient.of(componentItem)),
                outputItem, null, null);
        injectRecipe(level, new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gametest_infusion_probe"),
                recipe));

        // --- load the altar
        ((TilePedestal) level.getBlockEntity(centre)).setItem(0, centreItem.copy());
        ((TilePedestal) level.getBlockEntity(pedA)).setItem(0, componentItem.copy());
        ((TilePedestal) level.getBlockEntity(pedB)).setItem(0, componentItem.copy());

        // --- activate, then start the craft (original onWandRightClick seam)
        final TileInfusionMatrix matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
        helper.assertTrue(matrix != null, "matrix tile missing");
        final ServerPlayer player = freshPlayer(helper, "tc-infusion-");
        matrix.useWithoutItem(level, matrixPos, player);
        helper.assertTrue(matrix.active, "matrix did not activate on first use (validLocation broken?)");
        matrix.useWithoutItem(level, matrixPos, player);
        helper.assertTrue(matrix.crafting, "craft did not start with a matching altar");

        // --- the ticker runs the cycle: 2 essentia drains, then each component
        // pedestal counts down (itemCount 5..0) and is cleared, then the center
        // is replaced. ~14 cycles at countDelay 10 → ~140 ticks.
        helper.succeedWhen(() -> {
            helper.assertTrue(!matrix.crafting, "craft never finished");
            helper.assertTrue(matrix.instability == 0, "unexpected instability: " + matrix.instability);
            final ItemStack result = ((TilePedestal) level.getBlockEntity(centre)).getItem(0);
            helper.assertTrue(result.is(outputItem.getItem()) && result.getCount() == 2,
                    "center pedestal does not hold the recipe output: " + result);
            helper.assertTrue(((TilePedestal) level.getBlockEntity(pedA)).getItem(0).isEmpty(),
                    "component pedestal A was not consumed");
            helper.assertTrue(((TilePedestal) level.getBlockEntity(pedB)).getItem(0).isEmpty(),
                    "component pedestal B was not consumed");
            helper.assertTrue(source.getAspectPoolSize() == 8,
                    "essentia drain wrong: expected 10-2 left, got " + source.getAspectPoolSize());
        });
    }

    /**
     * Injects a recipe holder into the live RecipeManager's byType multimap.
     * The recipe JSONs for the original catalog reference items owned by
     * unlanded cards, so the runtime path needs a recipe built from probe
     * items; replacing the multimap (not mutating) keeps the manager's
     * invariants intact and the injection scoped to this test level.
     */
    @SuppressWarnings("unchecked")
    private static void injectRecipe(final ServerLevel level, final RecipeHolder<?> holder) {
        try {
            final RecipeManager manager = level.getRecipeManager();
            final Field byType = RecipeManager.class.getDeclaredField("byType");
            byType.setAccessible(true);
            final Multimap<RecipeType<?>, RecipeHolder<?>> current =
                    (Multimap<RecipeType<?>, RecipeHolder<?>>) byType.get(manager);
            final Multimap<RecipeType<?>, RecipeHolder<?>> next = ArrayListMultimap.create(current);
            next.put(M404RecipeSerializers.INFUSION_TYPE.get(), holder);
            byType.set(manager, next);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot inject gametest infusion recipe", e);
        }
    }

    /** Same bare-ServerPlayer construction as ResearchProgressionGameTest. */
    private static ServerPlayer freshPlayer(final GameTestHelper helper, final String namePrefix) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), namePrefix + UUID.randomUUID().toString().substring(0, 8)), false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
