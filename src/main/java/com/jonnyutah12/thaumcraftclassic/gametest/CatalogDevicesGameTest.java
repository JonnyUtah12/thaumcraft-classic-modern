package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAiry;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockWoodenDevice;
import com.jonnyutah12.thaumcraftclassic.tiles.TileNitor;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSensor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-04c-s evidence: the card's "registry + block-entity tick smoke test" bar.
 *
 * <ol>
 *   <li>{@link #deviceBlocksCarryTheirOriginalMetaRanges} — the four shared
 *       device blocks resolve in the BLOCK and ITEM registries, are placeable,
 *       and expose the original's meta range verbatim as the as-number
 *       {@code variant} property (D-001: 15 stone / 15 metal / 9 wooden / 13
 *       airy) with a value-for-value round-trip; the fourteen content-mapping.csv
 *       rows this card flipped to done (4 block + 10 tile) keep resolving,
 *       the tile ids through {@code minecraft:block_entity_type}.</li>
 *   <li>{@link #deviceTilesResolveOnlyForTheirOwningMetas} — the
 *       (block, variant) → tile suppliers this card registers through
 *       {@code DeviceTileFactory} really produce their tile entity, and the
 *       metas this card deliberately left out (the essentia-transport furnace
 *       machines and the node/warding hosts) produce none — the same shape as
 *       the original's {@code createTileEntity} returning null. The one
 *       furniture meta this card probed as hostless (blockWoodenDevice 8, the
 *       banner) was filled in by TC-M6-04d through the same seam, so the probe
 *       now asserts the tile resolves instead.</li>
 *   <li>{@link #standaloneMachinesTickThroughTheirBlock} — the original's
 *       {@code canUpdate()} contract: the nitor and the arcane ear tick through
 *       the device block's shared ticker (the marker interface is what selects
 *       them), the pressure plate does not, and the observable state changes
 *       (the redstone arm counts down, the Fire-starved nitor burns out).</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogDevicesGameTest {

    private CatalogDevicesGameTest() {
    }

    /** The ids this card owns: four device blocks plus the ten tile entities. */
    private static List<String> deviceBlockIds() {
        return List.of("blockstonedevice", "blockmetaldevice", "blockwoodendevice", "blockairy");
    }

    private static List<String> deviceTileIds() {
        return List.of("tilebellows", "tilesensor", "tilearcanepressureplate",
                "tilearcanebore", "tilearcaneborebase",
                "tilearcanelamp", "tilearcanelampgrowth", "tilearcanelampfertility",
                "tileowned", "tilenitor");
    }

    private static List<ContentMappingCsv.Row> readContract(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        try {
            return ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One device family: registered, placeable, and its meta range round-trips. */
    private static void checkFamily(final GameTestHelper helper, final String id,
                                   final BlockDeviceBase device, final int originalMetaCount) {
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, id);
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), id + " is not registered in the block registry");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key),
                id + " has no block item under the same contract id");
        final IntegerProperty variant = device.variantProperty();
        helper.assertTrue(variant.getPossibleValues().size() == originalMetaCount,
                id + " must expose the original's " + originalMetaCount + " meta values, got "
                        + variant.getPossibleValues().size());

        final BlockState base = device.defaultBlockState();
        helper.assertTrue(base.getValue(variant) == 0,
                id + "'s default state must carry the original's default meta 0");
        for (int value = 0; value < originalMetaCount; value++) {
            final BlockState state = base.setValue(variant, value);
            helper.assertTrue(state.getValue(variant) == value,
                    id + " variant " + value + " did not round-trip through the block state");
            helper.setBlock(new BlockPos(1, 1, 1), state);
            helper.assertBlockPresent(device, 1, 1, 1);
            helper.assertBlockProperty(new BlockPos(1, 1, 1), variant, value);
        }
    }

    @GameTest(template = "empty")
    public static void deviceBlocksCarryTheirOriginalMetaRanges(final GameTestHelper helper) {
        // Meta counts are the originals' metadata ranges (docs/content-mapping.csv
        // names column): stone 0-14, metal 0-14, wooden 0-8, airy 0-12.
        checkFamily(helper, "blockstonedevice", (BlockDeviceBase) CatalogDevices.BLOCK_STONE_DEVICE.get(),
                BlockStoneDevice.MAX_VARIANT + 1);
        checkFamily(helper, "blockmetaldevice", (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get(),
                BlockMetalDevice.MAX_VARIANT + 1);
        checkFamily(helper, "blockwoodendevice", (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(),
                BlockWoodenDevice.MAX_VARIANT + 1);
        checkFamily(helper, "blockairy", (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get(),
                BlockAiry.MAX_VARIANT + 1);

        // The rows this card flipped to done must keep resolving (id stability).
        var flippedBlocks = 0;
        var flippedTiles = 0;
        for (final ContentMappingCsv.Row row : readContract(helper)) {
            final String namespace = ThaumcraftClassic.MOD_ID + ":";
            if (!row.newId().startsWith(namespace)) {
                continue;
            }
            final String name = row.newId().substring(namespace.length());
            final boolean ownedBlock = deviceBlockIds().contains(name);
            final boolean ownedTile = deviceTileIds().contains(name);
            if (!ownedBlock && !ownedTile) {
                continue;
            }
            helper.assertTrue(row.implementationStatus().equals("done") && row.testStatus().equals("tested"),
                    "card-owned row " + row.newId() + " must be marked done/tested in this commit");
            if (ownedBlock) {
                flippedBlocks++;
                helper.assertTrue(BuiltInRegistries.BLOCK
                        .containsKey(ResourceLocation.parse(row.newId())),
                        "csv row " + row.newId() + " is marked done but does not resolve");
            } else {
                flippedTiles++;
                helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE
                        .containsKey(ResourceLocation.parse(row.newId())),
                        "csv row " + row.newId() + " is marked done but its tile entity is not registered");
            }
        }
        helper.assertTrue(flippedBlocks == deviceBlockIds().size(),
                "expected " + deviceBlockIds().size() + " device block rows in the contract, got "
                        + flippedBlocks);
        helper.assertTrue(flippedTiles == deviceTileIds().size(),
                "expected " + deviceTileIds().size() + " device tile rows in the contract, got " + flippedTiles);
        helper.succeed();
    }

    /**
     * The block entity this card's supplier produces for one (block, variant).
     * The lookup goes through the world so the test proves the placed block
     * really hosts its tile, not just that the factory has an entry.
     */
    private static void expectTile(final GameTestHelper helper, final BlockDeviceBase device,
                                  final int variant, final BlockEntityType<?> expectedType,
                                  final BlockPos pos, final String label) {
        helper.setBlock(pos, device.withVariant(variant));
        final BlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity != null, label + " has no block entity at variant " + variant);
        helper.assertTrue(entity.getType() == expectedType,
                label + " at variant " + variant + " created the wrong tile entity");
    }

    /**
     * A meta this card does not own: the device block's own creation hook
     * {@code newBlockEntity} returns none — the same shape as the original's
     * {@code createTileEntity} returning null. (Checked on the block, not in the
     * world: placing a hostless variant leaves nothing to look up.)
     */
    private static void expectNoTile(final GameTestHelper helper, final BlockDeviceBase device,
                                    final int variant, final BlockPos pos) {
        final BlockState state = device.withVariant(variant);
        helper.setBlock(pos, state);
        final BlockEntity entity = device.newBlockEntity(pos, state);
        helper.assertTrue(entity == null,
                "a meta outside this card's scope must not create a block entity: " + variant);
    }

    /**
     * One fresh spot per probe: {@code setBlock} over an existing block entity
     * leaves that entity in place, so probing the same coordinate twice would
     * read the previous probe's tile.
     */
    @GameTest(template = "empty")
    public static void deviceTilesResolveOnlyForTheirOwningMetas(final GameTestHelper helper) {
        final var base = new BlockPos(1, 1, 1);
        final var step = 2;
        // This card's standalone wooden devices and their supplier table.
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 0,
                CatalogDevices.TILE_BELLOWS.get(), base, "arcane bellows");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 1,
                CatalogDevices.TILE_SENSOR.get(), base.offset(0, step, 0), "arcane ear");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 2,
                CatalogDevices.TILE_ARCANE_PRESSURE_PLATE.get(), base.offset(0, step * 2, 0),
                "arcane pressure plate");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 3,
                CatalogDevices.TILE_ARCANE_PRESSURE_PLATE.get(), base.offset(0, step * 3, 0),
                "arcane pressure plate (raised)");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 4,
                CatalogDevices.TILE_ARCANE_BORE_BASE.get(), base.offset(0, step * 4, 0),
                "arcane bore base");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get(), 5,
                CatalogDevices.TILE_ARCANE_BORE.get(), base.offset(0, step * 5, 0), "arcane bore");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get(),
                BlockMetalDevice.VARIANT_LAMP, CatalogDevices.TILE_ARCANE_LAMP.get(),
                base.offset(0, step * 6, 0), "arcane lamp");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get(),
                BlockMetalDevice.VARIANT_LAMP_GROWTH, CatalogDevices.TILE_ARCANE_LAMP_GROWTH.get(),
                base.offset(0, step * 7, 0), "lamp of growth");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get(),
                BlockMetalDevice.VARIANT_LAMP_FERTILITY, CatalogDevices.TILE_ARCANE_LAMP_FERTILITY.get(),
                base.offset(0, step * 8, 0), "lamp of fertility");
        expectTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get(),
                BlockAiry.VARIANT_NITOR, CatalogDevices.TILE_NITOR.get(),
                base.offset(0, step * 9, 0), "nitor");

        // Metas with no supplier here: the essentia-transport furnace (TC-M4-07),
        // the metal crucible/alembic metas, and the airy node metas, which are
        // record-driven (TC-M5-03).
        expectNoTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_STONE_DEVICE.get(),
                BlockStoneDevice.VARIANT_ALCHEMY_FURNACE, base.offset(0, step * 10, 0));
        expectNoTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get(), 1,
                base.offset(0, step * 11, 0));
        expectNoTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get(),
                BlockAiry.VARIANT_NODE, base.offset(0, step * 13, 0));
        expectNoTile(helper, (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get(),
                BlockAiry.VARIANT_NODE_ENERGIZED, base.offset(0, step * 14, 0));
        // blockWoodenDevice 8 (the hanging banner) is no longer hostless: TC-M6-04d
        // owns it and registers its tile through this same DeviceTileFactory seam.
        // The tile *type* is asserted in CatalogFurnitureGameTest; here the parent
        // card only asserts that the seam still resolves once a sibling card fills
        // it in, so this probe flips from expectNoTile to a presence check.
        final BlockPos bannerPos = base.offset(0, step * 12, 0);
        final BlockState bannerState = ((BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get())
                .withVariant(BlockWoodenDevice.VARIANT_BANNER);
        helper.setBlock(bannerPos, bannerState);
        helper.assertTrue(helper.getBlockEntity(bannerPos) != null,
                "blockWoodenDevice " + BlockWoodenDevice.VARIANT_BANNER
                        + " is owned by TC-M6-04d and must resolve to its tile through this seam");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void standaloneMachinesTickThroughTheirBlock(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = new BlockPos(1, 1, 1);
        final BlockDeviceBase airy = (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get();
        final BlockState nitorState = airy.withVariant(BlockAiry.VARIANT_NITOR);
        helper.setBlock(pos, nitorState);

        final TileNitor nitor = helper.getBlockEntity(pos);
        helper.assertTrue(nitor != null, "the nitor host has no block entity");
        helper.assertTrue(nitor.getType() == CatalogDevices.TILE_NITOR.get(),
                "the nitor host created the wrong tile entity");

        // The shared ticker the device block hands back is what drives the tile:
        // only tiles that opt into the ticking marker interface are selected, so
        // this is the port-side of the original's canUpdate() contract.
        final BlockEntityTicker<TileNitor> nitorTicker =
                airy.getTicker(level, nitorState, CatalogDevices.TILE_NITOR.get());
        helper.assertTrue(nitorTicker != null, "the nitor host has no ticker");

        // Original onBlockActivated: arms a 10-tick redstone pulse and the burn.
        nitor.activate();
        helper.assertTrue(nitor.redstoneSignal == 10, "arming the nitor must set a 10-tick pulse");
        nitorTicker.tick(level, pos, nitorState, nitor);
        helper.assertTrue(nitor.redstoneSignal == 9,
                "one world tick through the shared ticker must count the pulse down, got "
                        + nitor.redstoneSignal);
        // Original serverTick: the burn needs Fire; with no provider it goes out.
        helper.assertTrue(nitor.use == 0,
                "a Fire-starved nitor must burn out on its first tick, got use=" + nitor.use);

        // The arcane ear (blockWoodenDevice meta 1) ticks the same way.
        final BlockDeviceBase wooden = (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get();
        final BlockState sensorState = wooden.withVariant(1);
        helper.setBlock(new BlockPos(1, 3, 1), sensorState);
        final TileSensor sensor = helper.getBlockEntity(new BlockPos(1, 3, 1));
        helper.assertTrue(sensor != null, "the arcane ear host has no block entity");
        final BlockEntityTicker<TileSensor> sensorTicker =
                wooden.getTicker(level, sensorState, CatalogDevices.TILE_SENSOR.get());
        helper.assertTrue(sensorTicker != null, "the arcane ear host has no ticker");
        sensor.redstoneSignal = 3;
        sensorTicker.tick(level, new BlockPos(1, 3, 1), sensorState, sensor);
        sensorTicker.tick(level, new BlockPos(1, 3, 1), sensorState, sensor);
        helper.assertTrue(sensor.redstoneSignal == 1,
                "two ticks must count the pulse down to 1, got " + sensor.redstoneSignal);

        // canUpdate()=false in the original: the pressure plate has a tile entity
        // but never opts into the ticking marker interface.
        final BlockState plateState = wooden.withVariant(2);
        helper.setBlock(new BlockPos(1, 5, 1), plateState);
        final BlockEntity plate = helper.getBlockEntity(new BlockPos(1, 5, 1));
        helper.assertTrue(plate != null, "the pressure plate host has no block entity");
        helper.assertTrue(!(plate instanceof BlockDeviceBase.TickingDeviceTile),
                "the pressure plate must not tick (the original's canUpdate() is false)");
        helper.succeed();
    }
}
