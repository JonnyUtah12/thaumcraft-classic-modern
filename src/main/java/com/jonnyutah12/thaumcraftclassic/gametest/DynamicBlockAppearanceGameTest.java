package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCandle;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLifter;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTints;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLamp;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLampGrowth;
import com.jonnyutah12.thaumcraftclassic.tiles.TileLifter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BD-18 (t_065502ba): the appearance a static blockstate cannot express is
 * driven by <b>live tile data</b>, and the tile publishes it.
 *
 * <p>SPINE-4 shipped the state→model mapping; this probe pins the half that
 * mapping cannot compute — the value of the property the lit/active models key
 * on. 1.7.10 read that value inside the connected-texture hook
 * ({@code BlockMetalDevice.java:117-155} chose {@code lamp_grow_top}/
 * {@code lamp_grow_side} while {@code TileArcaneLampGrowth.charges > 0}, and
 * {@code BlockLifterRenderer.java:50-89} drew the {@code animatedglow} shell
 * bright while {@code !TileLifter.gettingPower()}). 1.21.1 has no such hook, so
 * the port mirrors the original's condition into a block state property, and
 * these tests assert the tile writes it, the block's light follows with it, the
 * block entity survives the state write, and the candle's 16 tints stay
 * addressable from the state and from the stack an item frame shows.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DynamicBlockAppearanceGameTest {

    private DynamicBlockAppearanceGameTest() {
    }

    /**
     * The growth lamp's face pair swaps on the live condition. The original
     * armed on vis charges; the port's lamps run on essentia, so the live
     * condition is {@code use > 0} (armed and burning) — BD-18 records that
     * mapping rather than inventing a charge field.
     */
    @GameTest(template = "empty")
    public static void litLampFaceFollowsTheTile(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 3, 1);
        final BlockState lamp = CatalogDevices.BLOCK_METAL_DEVICE.get().defaultBlockState()
                .setValue(variantProperty(CatalogDevices.BLOCK_METAL_DEVICE.get()),
                        BlockMetalDevice.VARIANT_LAMP_GROWTH);
        helper.setBlock(pos, lamp);

        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof TileArcaneLampGrowth
                        && blockEntity.getType() == CatalogDevices.TILE_ARCANE_LAMP_GROWTH.get(),
                "metal device variant 8 must place the growth-lamp tile the lit swap reads, got " + blockEntity
                        + " (placed " + stateAt(helper, pos) + ", hasBlockEntity="
                        + stateAt(helper, pos).hasBlockEntity() + ")");
        final TileArcaneLamp tile = (TileArcaneLamp) blockEntity;
        helper.assertTrue(!stateAt(helper, pos).getValue(BlockMetalDevice.CHARGED),
                "a freshly placed growth lamp must sit in its unlit state (lamp_grow_top_off/lamp_grow_side_off)");
        helper.assertTrue(stateAt(helper, pos).getLightEmission() == 8,
                "BlockMetalDevice.getLightValue: an uncharged growth lamp lit 8, got "
                        + stateAt(helper, pos).getLightEmission());

        tile.use = 1;
        tile.publishCharged();
        helper.assertTrue(stateAt(helper, pos).getValue(BlockMetalDevice.CHARGED),
                "an armed lamp must publish charged=true so the blockstate picks the lit lamp_grow_top/lamp_grow_side model");
        helper.assertTrue(stateAt(helper, pos).getLightEmission() == 15,
                "BlockMetalDevice.getLightValue: a charged growth lamp lit 15, got "
                        + stateAt(helper, pos).getLightEmission());
        helper.assertTrue(helper.getBlockEntity(pos) == tile,
                "publishing the appearance property replaced the block state — the lamp's block entity "
                        + "(its essentia inventory) must survive the write");

        // The wiring: serverTick() publishes, so the state follows the tile with
        // no renderer and no per-frame query.
        tile.use = 0;
        tile.serverTick();
        helper.assertTrue(!stateAt(helper, pos).getValue(BlockMetalDevice.CHARGED),
                "serverTick() must publish the tile's current condition: disarming the lamp returns it to the unlit model");
        helper.assertTrue(stateAt(helper, pos).getLightEmission() == 8,
                "the light level follows the published property back down to the uncharged 8, got "
                        + stateAt(helper, pos).getLightEmission());
        helper.succeed();
    }

    /**
     * The levitator's glow shell swaps on the tile's power query: unpowered is
     * the state that pushes and glows
     * ({@code BlockLifterRenderer.java:50-89}: the glow faces, with
     * {@code bb = 180} when {@code !tile.gettingPower()}).
     */
    @GameTest(template = "empty")
    public static void levitatorGlowFollowsPower(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, CatalogFurniture.BLOCK_LIFTER.get().defaultBlockState());

        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof TileLifter
                        && blockEntity.getType() == CatalogFurniture.TILE_LIFTER.get(),
                "the levitator must place the tile the power query reads, got " + blockEntity
                        + " (placed " + stateAt(helper, pos) + ", hasBlockEntity="
                        + stateAt(helper, pos).hasBlockEntity() + ")");
        final TileLifter tile = (TileLifter) blockEntity;
        tile.serverTick();
        helper.assertTrue(!stateAt(helper, pos).getValue(BlockLifter.POWERED),
                "an unpowered levitator is the glowing state (powered=false -> the animatedglow shell model)");

        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        tile.serverTick();
        helper.assertTrue(stateAt(helper, pos).getValue(BlockLifter.POWERED),
                "a powered levitator must publish powered=true so the blockstate drops the glow shell (it is not pushing)");
        helper.assertTrue(helper.getBlockEntity(pos) == tile,
                "the levitator's block entity (its lift column) must survive the appearance write");

        helper.setBlock(pos.east(), Blocks.AIR.defaultBlockState());
        tile.serverTick();
        helper.assertTrue(!stateAt(helper, pos).getValue(BlockLifter.POWERED),
                "losing the signal returns the levitator to its glowing state");
        helper.succeed();
    }

    /**
     * The candle's colour is <b>state-addressable</b> in the port (BD-18 cell
     * 3). The original tinted the whole block from its metadata
     * ({@code BlockCandle.java:76-78} → {@code Utils.colors[md]}, and the same
     * value for the particles at {@code :52-54}); the port carries that
     * metadata verbatim as the {@code variant} property (D-001), so the tint a
     * {@code BlockColor} handler will return is a pure function of the state —
     * and the tint an item frame will show is the same function of the stack
     * the block drops, which is the only form that reaches a render path with
     * no {@code Level}.
     *
     * <p>Asserted here: all sixteen registered states yield sixteen
     * <em>distinct</em> colours (a tint that collapsed several variants onto
     * one RGB value is the failure mode), and a placed candle's drop carries
     * the variant back out so the item-side handler can resolve the same
     * colour. What is NOT proved: the pixels — that needs {@code runClient}.</p>
     */
    @GameTest(template = "gametest_pad_32")
    public static void candleTintIsAddressableFromTheStateAndTheStack(final GameTestHelper helper) {
        final BlockCandle candle = CatalogDecorativeBlocks.BLOCK_CANDLE.get();
        final IntegerProperty variant = variantProperty(candle);

        final Set<Integer> tints = new LinkedHashSet<>();
        for (final BlockState state : candle.getStateDefinition().getPossibleStates()) {
            tints.add(DeviceTints.colourForVariant(state.getValue(variant)));
        }
        ThaumcraftClassic.LOGGER.info("[BD-18] candle tint: {} registered variants, {} distinct colours",
                candle.getStateDefinition().getPossibleStates().size(), tints.size());
        helper.assertTrue(tints.size() == 16,
                "Utils.java:50-52 declares 16 colours and BlockCandle.java:76-78 indexes them by metadata, so"
                        + " the 16 shipped variants must resolve 16 distinct tints — got " + tints.size());

        // A placed candle's drop must carry the variant, or the item frame /
        // inventory draws the white placeholder: the state is gone at that point.
        for (final int colourIndex : new int[]{0, 5, 12, 15}) {
            final BlockPos pos = new BlockPos(1 + colourIndex, 1, 1);
            helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
            helper.setBlock(pos, candle.defaultBlockState().setValue(variant, colourIndex));
            final BlockState placed = stateAt(helper, pos);
            helper.assertTrue(placed.getValue(variant) == colourIndex,
                    "the shipped candle must keep its variant in the world state (the tint source), got "
                            + placed);
            final ItemStack drop = candle.getCloneItemStack(helper.getLevel(), helper.absolutePos(pos), placed);
            final Integer carried = VariantStateItems.variantOf(drop);
            helper.assertTrue(carried != null && carried == colourIndex,
                    "a variant-" + colourIndex + " candle's stack must carry its variant (the item frame has"
                            + " no Level to read); the item tint resolves to "
                            + (carried == null ? "the uncoloured placeholder" : DeviceTints.colourForVariant(carried))
                            + ", expected " + DeviceTints.colourForVariant(colourIndex));
            helper.assertTrue(DeviceTints.colourForVariant(carried) == DeviceTints.colourForVariant(colourIndex),
                    "the item-side tint for variant " + colourIndex + " must equal the world tint");
        }
        helper.succeed();
    }

    /**
     * BD-18 slice 2b (t_45b89dd2): the alchemical furnace's lit face ring follows a
     * published {@code burning} state. The original swapped the whole side ring from
     * the tile's burning flag inside its connected-texture hook
     * ({@code BlockStoneDevice.java:153-177}: {@code :165-171} answers
     * {@code iconFurnace[3] = al_furnace_front_on} (registered at {@code :77}) while
     * {@code TileAlchemyFurnace.isBurning()}, {@code iconFurnace[2] =
     * al_furnace_front_off} (registered at {@code :76}, and the plain
     * {@code getIcon} side answer at {@code :101-103}) otherwise), and the same flag
     * lit the block to 12 ({@code :216-220}). The port does not ship the furnace's
     * tile yet (content-mapping row {@code thaumcraft:tilealchemyfurnace},
     * not_started), so the machine card publishes the flag through
     * {@link DeviceDynamicState#sync} when it lands; this probe pins the state-side
     * contract that publication will drive: the default is the unlit value, the
     * light follows the flag, and the swap is the furnace's own (meta 2's static 10
     * is untouched, {@code :221-223}).
     */
    @GameTest(template = "empty")
    public static void burningFurnaceFacesFollowTheBurningState(final GameTestHelper helper) {
        final BlockStoneDevice block = CatalogDevices.BLOCK_STONE_DEVICE.get();
        final IntegerProperty variant = variantProperty(block);
        final var burning = block.getStateDefinition().getProperty("burning");
        helper.assertTrue(burning instanceof BooleanProperty,
                "the alchemical furnace must publish a boolean 'burning' state property — "
                        + "BlockStoneDevice.java:165-171 swaps the front-face ring on the tile's burning flag and "
                        + ":216-220 lights 12 with it; a blockstate model can only follow a published state (D-020), "
                        + "got properties " + block.getStateDefinition().getProperties());
        final BooleanProperty fire = (BooleanProperty) burning;

        final BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, block.withVariant(BlockStoneDevice.VARIANT_ALCHEMY_FURNACE));
        final BlockState placed = stateAt(helper, pos);
        helper.assertTrue(placed.getValue(variant) == BlockStoneDevice.VARIANT_ALCHEMY_FURNACE,
                "the alchemical furnace is stone device meta 0 (BlockStoneDevice.java:155), got " + placed);
        helper.assertTrue(!placed.getValue(fire),
                "a freshly placed alchemical furnace is unlit (BlockStoneDevice.java:165-171 answers "
                        + "al_furnace_front_off for a furnace that is not burning): 1.21.1 orders a "
                        + "BooleanProperty true-first, so the registered default must pin burning=false explicitly");
        helper.assertTrue(placed.getLightEmission() == 0,
                "an unburning alchemical furnace emits no light (BlockStoneDevice.java:214-226 falls through to "
                        + "the super answer), got " + placed.getLightEmission());

        helper.setBlock(pos, placed.setValue(fire, true));
        final BlockState lit = stateAt(helper, pos);
        helper.assertTrue(lit.getValue(fire),
                "the burning value must survive placement so the blockstate picks the al_furnace_front_on model "
                        + "(the icon is registered at BlockStoneDevice.java:77 and answered at :165-171), got " + lit);
        helper.assertTrue(lit.getLightEmission() == 12,
                "BlockStoneDevice.java:216-220: meta 0 with a burning tile lit 12, got " + lit.getLightEmission());
        helper.assertTrue(!lit.hasBlockEntity(),
                "the furnace's tile — the publisher of this flag — is content-mapping row "
                        + "thaumcraft:tilealchemyfurnace (not_started); the shipped state contract must not "
                        + "depend on the machine tile");

        // The flag belongs to the furnace alone: the runic matrix keeps its static
        // 10 for both burning values (BlockStoneDevice.java:221-223), and the
        // original's face swap is gated on meta 0 (:155) with metas 1/5/8/12
        // delegating to the super answer (:172-174).
        helper.setBlock(pos, block.withVariant(BlockStoneDevice.VARIANT_RUNIC_MATRIX).setValue(fire, true));
        helper.assertTrue(stateAt(helper, pos).getLightEmission() == 10,
                "BlockStoneDevice.java:221-223: meta 2 lit a static 10 — the burning flag is decoration there, "
                        + "got " + stateAt(helper, pos).getLightEmission());
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** The shipped state at a position (the write must land on the world state). */
    private static BlockState stateAt(final GameTestHelper helper, final BlockPos pos) {
        // GameTestHelper.setBlock/getBlockEntity take positions RELATIVE to the
        // test structure; Level.getBlockState does not translate, so reading
        // through the raw level needs the absolute position or the probe reads
        // the world under the test island.
        return helper.getLevel().getBlockState(helper.absolutePos(pos));
    }

    /**
     * The device's {@code variant} property, read from the live state
     * definition rather than a private constant — the property a later card
     * renames fails the probe instead of compiling against a stale name.
     */
    private static IntegerProperty variantProperty(final net.minecraft.world.level.block.Block block) {
        final var property = block.getStateDefinition().getProperty("variant");
        if (property instanceof IntegerProperty integer) {
            return integer;
        }
        throw new IllegalStateException("the shipped metal device registers no integer 'variant' property: "
                + block.getStateDefinition().getProperties());
    }
}
