package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTube;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCentrifuge;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEssentiaCrystalizer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTube;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeFilter;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeValve;
import com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M4-07-s evidence: the essentia transport network in the world — the tube
 * family on its original block, its suction figures measured off real placed
 * tiles, essentia handed down a real line from a source into a jar, the filter
 * label flow, the loot, and the buffer's pool.
 *
 * <ol>
 *   <li>{@link #tubeNetworkRegistersItsOwnContent} — the block, its same-id
 *       block item, the six conduit block entities, the original metadata 0..7
 *       on the {@code variant} property, the (variant -> tile class) mapping
 *       (all eight, including the centrifuge and the crystallizer, which
 *       landed with their own card TC-M4-07-m) and the content-mapping rows
 *       this card flipped.</li>
 *   <li>{@link #aBufferFeedsAJarThroughTheTubeLine} — the end-to-end flow in a
 *       real world: an essentia buffer, two plain tubes and a warded jar, ticked
 *       by the blocks' own tickers, with the total conserved.</li>
 *   <li>{@link #theTubeSuctionRulesHoldInTheWorld} — the suction figures of the
 *       four conduit kinds measured on placed tiles: 32−1 for a plain tube,
 *       32/2 for a restricted tube, the directional tube's facing gate, and the
 *       closed valve's refusal to advertise anything.</li>
 *   <li>{@link #theFilteredTubeTakesItsAspectFromALabel} — the label
 *       interaction through the block's own path, the suction type it then
 *       advertises, and the label handed back on a sneak-use.</li>
 *   <li>{@link #tubesDropThemselvesWithTheirVariant} — the shipped loot table
 *       keeps the original metadata on the drop, and a labelled filtered tube
 *       drops its label too.</li>
 *   <li>{@link #theBufferPoolsAnyAspectAndSignalsItsFill} — the buffer's pool
 *       (any mix, one unit at a time, eight max), its bellows-driven suction
 *       and its comparator read.</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EssentiaTransportGameTest {

    private EssentiaTransportGameTest() {
    }

    private static Aspect aer() {
        return Aspect.getAspect("aer");
    }

    private static Aspect ignis() {
        return Aspect.getAspect("ignis");
    }

    /** Local structure coordinates -> world coordinates. */
    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> T castBe(final BlockEntity be, final Class<T> type) {
        return type.isInstance(be) ? (T) be : null;
    }

    private static <T extends BlockEntity> T tubeAt(final GameTestHelper helper, final int x, final int y, final int z,
                                                    final Class<T> type) {
        return castBe(helper.getLevel().getBlockEntity(at(helper, x, y, z)), type);
    }

    private static TileJarFillable jarAt(final GameTestHelper helper, final int x, final int y, final int z) {
        return tubeAt(helper, x, y, z, TileJarFillable.class);
    }

    private static List<ItemStack> dropsAround(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
                .map(ItemEntity::getItem).toList();
    }

    // --- registration and the id contract ---------------------------------

    @GameTest(template = "empty")
    public static void tubeNetworkRegistersItsOwnContent(final GameTestHelper helper) {
        final ResourceLocation blockId = ResourceLocation.parse("thaumcraft:blocktube");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(blockId),
                "content-mapping.csv row blocktube must resolve as a block");
        helper.assertTrue(BuiltInRegistries.BLOCK.get(blockId) == CatalogEssentiaTransport.BLOCK_TUBE.get(),
                "and be the holder's own block");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(blockId),
                "blocktube must have its same-id block item");
        helper.assertTrue(BuiltInRegistries.ITEM.get(blockId) == CatalogEssentiaTransport.ITEM_TUBE.get(),
                "and be the holder's own block item, not another card's");

        // The original's metadata 0..7, carried verbatim (D-001).
        final BlockState base = CatalogEssentiaTransport.BLOCK_TUBE.get().defaultBlockState();
        final var variantProperty = base.getBlock().getStateDefinition()
                .getProperty(VariantStateItems.VARIANT_PROPERTY);
        helper.assertTrue(variantProperty instanceof IntegerProperty range
                        && range.getPossibleValues().size() == 8
                        && range.getPossibleValues().contains(BlockTube.VARIANT_TUBE)
                        && range.getPossibleValues().contains(BlockTube.VARIANT_CRYSTALIZER),
                "blockTube's original metadata 0..7 must be the variant property's whole range");
        helper.assertTrue(CatalogEssentiaTransport.BLOCK_TUBE.get().variant(base) == 0,
                "the default state is the plain tube (original metadata 0)");

        // The eight variants the original's createTileEntity picked: their ids,
        // their binding to the tube block, and the (variant -> tile) mapping.
        // Metas 2 and 7 are the two machines (the centrifuge and the
        // crystallizer): they landed with TC-M4-07-m, so this probe — which
        // used to assert those two variants place NO tile — now asserts they
        // place their own machine tile. The machines' conversion rules are
        // proven by EssentiaMachinesGameTest.
        final List<Class<?>> expected = java.util.Arrays.asList(TileTube.class, TileTubeValve.class,
                TileCentrifuge.class,
                TileTubeFilter.class, TileTubeBuffer.class,
                com.jonnyutah12.thaumcraftclassic.tiles.TileTubeRestrict.class,
                com.jonnyutah12.thaumcraftclassic.tiles.TileTubeOneway.class,
                TileEssentiaCrystalizer.class);
        for (final String id : List.of("tiletube", "tiletubevalve", "tiletubefilter",
                "tiletubebuffer", "tiletuberestrict", "tiletubeoneway",
                "tilecentrifuge", "tileessentiacrystalizer")) {
            helper.assertTrue(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("thaumcraft:" + id)),
                    id + " must be registered as a block entity");
        }
        for (int variant = 0; variant < 8; variant++) {
            final BlockEntity created = CatalogEssentiaTransport.BLOCK_TUBE.get()
                    .newBlockEntity(BlockPos.ZERO, CatalogEssentiaTransport.state(variant));
            final Class<?> want = expected.get(variant);
            helper.assertTrue(created != null && want.isInstance(created),
                    "blockTube variant " + variant + " must place " + want.getSimpleName()
                            + ", got " + created);
        }

        final TileTube tube = castBe(CatalogEssentiaTransport.createTube(BlockPos.ZERO,
                CatalogEssentiaTransport.state(0)), TileTube.class);
        helper.assertTrue(tube != null && tube.isConnectable(Direction.UP) && tube.isConnectable(Direction.DOWN),
                "a fresh tube is open on every face (the original's openSides default)");
        helper.assertTrue(tube.getMinimumSuction() == 0 && !tube.renderExtendedTube(),
                "and reports the original's minimum suction 0");
        helper.succeed();
    }

    // --- the line that actually moves essentia ----------------------------

    /**
     * The end-to-end flow: an essentia buffer (which, unlike a jar, advertises
     * minimum suction 0 — see {@code TubeTransportLogicTest}) feeds a chain of
     * plain tubes, and the warded jar at the end of the line pulls the essentia
     * out of the last tube at its own 32.
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void aBufferFeedsAJarThroughTheTubeLine(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
        helper.setBlock(1, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(2, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(2, 1, 1, CatalogEssentiaStorage.jarState(false));

        final TileTubeBuffer buffer = tubeAt(helper, 1, 1, 1, TileTubeBuffer.class);
        final TileTube first = tubeAt(helper, 1, 2, 1, TileTube.class);
        final TileTube second = tubeAt(helper, 2, 2, 1, TileTube.class);
        final TileJarFillable jar = jarAt(helper, 2, 1, 1);
        helper.assertTrue(buffer != null && first != null && second != null && jar != null,
                "the buffer, the two tubes and the jar must all have placed their tiles");

        buffer.aspects.add(aer(), 8);
        helper.assertTrue(buffer.getEssentiaAmount(Direction.UP) == 8,
                "the source buffer starts with 8 units of aer");

        helper.runAtTickTime(190L, () -> {
            helper.assertTrue(jar.amount > 0,
                    "the jar at the end of the line must have received essentia, got " + jar.amount);
            helper.assertTrue(jar.aspect != null && "aer".equals(jar.aspect.getTag()),
                    "and it must be the aspect the line was carrying, got " + jar.aspect);
            final int total = jar.amount + buffer.aspects.visSize()
                    + first.essentiaAmount + second.essentiaAmount;
            helper.assertTrue(total == 8,
                    "no essentia may be created or destroyed in transit: total = " + total
                            + " (jar " + jar.amount + ", buffer " + buffer.aspects.visSize()
                            + ", tubes " + first.essentiaAmount + "+" + second.essentiaAmount + ")");
            helper.assertTrue(buffer.aspects.visSize() < 8,
                    "and the source must actually have been drained, got " + buffer.aspects.visSize());
            helper.succeed();
        });
    }

    // --- the suction figures, measured on placed tiles --------------------

    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void theTubeSuctionRulesHoldInTheWorld(final GameTestHelper helper) {
        // Four lines, each a jar with one conduit above it. The jar advertises
        // 32 while it has room, so every figure below is the original's.
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(1, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));

        helper.setBlock(5, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(5, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_RESTRICT));

        helper.setBlock(9, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(9, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_ONEWAY));

        helper.setBlock(13, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(13, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_VALVE));

        final TileTube plain = tubeAt(helper, 1, 2, 1, TileTube.class);
        final TileTube restrict = tubeAt(helper, 5, 2, 1, TileTube.class);
        final TileTube oneway = tubeAt(helper, 9, 2, 1, TileTube.class);
        final TileTubeValve valve = tubeAt(helper, 13, 2, 1, TileTubeValve.class);
        helper.assertTrue(plain != null && restrict != null && oneway != null && valve != null,
                "all four conduits must have placed their tiles");

        // The directional tube reads only the face opposite its facing, and its
        // facing starts on the original's ForgeDirection.NORTH default, so at
        // first it sees nothing from the jar below.
        oneway.facing = Direction.NORTH;

        // A third of a second of real ticks: the suction pass runs every 2 ticks.
        helper.runAtTickTime(20L, () -> {
            helper.assertTrue(plain.getSuctionAmount(Direction.UP) == 31,
                    "TileTube.calculateSuction takes the jar's 32 and subtracts one, got "
                            + plain.getSuctionAmount(Direction.UP));
            helper.assertTrue(plain.getSuctionType(Direction.UP) == null,
                    "an unlabelled jar advertises no suction type, so a plain tube stays untyped");
            helper.assertTrue(restrict.getSuctionAmount(Direction.UP) == 16,
                    "TileTubeRestrict passes restrict = true: 32 / 2, got "
                            + restrict.getSuctionAmount(Direction.UP));
            helper.assertTrue(oneway.getSuctionAmount(Direction.UP) == 0,
                    "TileTubeOneway only reads the face opposite its facing; facing NORTH ignores the jar below,"
                            + " got " + oneway.getSuctionAmount(Direction.UP));
            helper.assertTrue(valve.getSuctionAmount(Direction.UP) == 31,
                    "an open valve passes the suction through, got " + valve.getSuctionAmount(Direction.UP));
            // The valve's own geometric gate: it is never connectable across its facing.
            helper.assertTrue(!valve.isConnectable(Direction.NORTH),
                    "a valve is not connectable across the face it gates");
            helper.assertTrue(valve.isConnectable(Direction.UP),
                    "but its other faces still are");

            // Flip the directional tube to look at the jar, and close the valve.
            oneway.facing = Direction.UP;
            valve.allowFlow = false;
        });

        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(oneway.getSuctionAmount(Direction.UP) == 31,
                    "with facing = UP the directional tube reads the jar below (DOWN.getOpposite() == UP), got "
                            + oneway.getSuctionAmount(Direction.UP));
            helper.assertTrue(valve.getSuctionAmount(Direction.UP) == 0,
                    "a closed valve refuses setSuction, so it advertises nothing and the line is dead, got "
                            + valve.getSuctionAmount(Direction.UP));
            helper.succeed();
        });
    }

    // --- the filter's label ------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void theFilteredTubeTakesItsAspectFromALabel(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(1, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_FILTER));
        final TileTubeFilter filter = tubeAt(helper, 1, 2, 1, TileTubeFilter.class);
        helper.assertTrue(filter != null, "blockTube variant 3 must place a TileTubeFilter");
        helper.assertTrue(filter.aspectFilter == null, "and it starts unfiltered");

        final BlockPos pos = at(helper, 1, 2, 1);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        final BlockState state = helper.getLevel().getBlockState(pos);

        // The original's itemResource metadata 13 carries its aspect on the stack.
        final ItemStack label = ItemResource.stack(ItemResource.META_JAR_LABEL);
        label.set(ModDataComponents.ESSENTIA_CONTAINER.get(), EssentiaContainer.of(aer(), 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, label);
        final ItemInteractionResult applied = state.useItemOn(label, helper.getLevel(), player,
                InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(applied.consumesAction(),
                "labelling a filtered tube consumes the interaction, got " + applied);
        helper.assertTrue(filter.aspectFilter != null && "aer".equals(filter.aspectFilter.getTag()),
                "the filter takes the label's aspect, got " + filter.aspectFilter);
        helper.assertTrue(label.isEmpty(), "and the label is spent, got " + label.getCount());

        // With the filter set, the tube advertises it as its suction type and
        // keeps reading the jar below (32 -> 31) — so a line asking for aer now
        // has a typed suction to match against.
        helper.runAtTickTime(120L, () -> {
            helper.assertTrue(TubeTransportLogic.same(filter.suctionType, aer()),
                    "a filtered tube advertises its own aspect as the suction type, got " + filter.suctionType);
            helper.assertTrue(filter.getSuctionAmount(Direction.UP) == 31,
                    "and still draws from the jar's 32, got " + filter.getSuctionAmount(Direction.UP));

            // Sneaking on the labelled tube pops the label back out (the
            // original's clear branch) and leaves the tube unfiltered.
            // 1.21.1's Entity.isCrouching() reads the CROUCHING pose, not the
            // shift key flag, so the mock player must be posed.
            player.setShiftKeyDown(true);
            player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
            player.setPos(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
            final net.minecraft.world.InteractionResult cleared = state.useWithoutItem(
                    helper.getLevel(), player, hit);
            helper.assertTrue(cleared.consumesAction(),
                    "clearing a labelled tube consumes the interaction, got " + cleared);
            helper.assertTrue(filter.aspectFilter == null, "the filter is cleared");
            final boolean labelBack = dropsAround(helper, pos).stream()
                    .anyMatch(stack -> stack.is(CatalogEssentiaStorage.ITEM_ESSENCE.get())
                            || ItemResource.storedMeta(stack) == ItemResource.META_JAR_LABEL);
            helper.assertTrue(labelBack,
                    "and the label is handed back to the world, got " + dropsAround(helper, pos));
            helper.succeed();
        });
    }

    // --- drops -------------------------------------------------------------

    @GameTest(template = "empty_24")
    public static void tubesDropThemselvesWithTheirVariant(final GameTestHelper helper) {
        // A plain tube and a buffer, broken for real, through the shipped loot
        // table (1.21.1 has no implicit block self-drop).
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.getLevel().destroyBlock(at(helper, 1, 1, 1), true, null);
        final List<ItemStack> plain = dropsAround(helper, at(helper, 1, 1, 1));
        helper.assertTrue(plain.size() == 1 && plain.get(0).is(CatalogEssentiaTransport.ITEM_TUBE.get()),
                "a broken tube drops the tube block item, got " + plain);
        helper.assertTrue(carriedVariant(plain.get(0)) == BlockTube.VARIANT_TUBE,
                "and the drop keeps the original metadata (variant 0), got " + carriedVariant(plain.get(0)));

        helper.setBlock(4, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
        helper.getLevel().destroyBlock(at(helper, 4, 1, 1), true, null);
        final List<ItemStack> buffer = dropsAround(helper, at(helper, 4, 1, 1));
        helper.assertTrue(buffer.size() == 1 && carriedVariant(buffer.get(0)) == BlockTube.VARIANT_BUFFER,
                "a broken buffer drops a buffer variant, not a plain tube, got " + buffer);

        // A labelled filtered tube drops the block AND its label (the original's
        // breakBlock).
        helper.setBlock(7, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_FILTER));
        final TileTubeFilter filter = tubeAt(helper, 7, 1, 1, TileTubeFilter.class);
        helper.assertTrue(filter != null, "the filtered tube must have placed its tile");
        filter.aspectFilter = ignis();
        helper.getLevel().destroyBlock(at(helper, 7, 1, 1), true, null);
        final List<ItemStack> labelled = dropsAround(helper, at(helper, 7, 1, 1));
        helper.assertTrue(labelled.stream().anyMatch(stack -> carriedVariant(stack) == BlockTube.VARIANT_FILTER),
                "the filtered tube drops its own variant, got " + labelled);
        helper.assertTrue(labelled.stream().anyMatch(
                        stack -> stack.is(com.jonnyutah12.thaumcraftclassic.registration.catalog
                                .CatalogResources.ITEM_RESOURCE.get())
                                && ItemResource.storedMeta(stack) == ItemResource.META_JAR_LABEL),
                "and the label comes back out of a labelled tube, got " + labelled);
        helper.succeed();
    }

    /** The original metadata a dropped stack carries, or -1 when it carries none. */
    private static int carriedVariant(final ItemStack stack) {
        final BlockItemStateProperties component =
                stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        final String carried = component == null
                ? null : component.properties().get(VariantStateItems.VARIANT_PROPERTY);
        if (carried != null) {
            return Integer.parseInt(carried);
        }
        final Integer meta = stack.get(ModDataComponents.ITEM_META.get());
        return meta == null ? -1 : meta;
    }

    // --- the buffer --------------------------------------------------------

    @GameTest(template = "empty_24")
    public static void theBufferPoolsAnyAspectAndSignalsItsFill(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
        final TileTubeBuffer buffer = tubeAt(helper, 1, 1, 1, TileTubeBuffer.class);
        helper.assertTrue(buffer != null, "blockTube variant 4 must place a TileTubeBuffer");
        final BlockState state = helper.getLevel().getBlockState(at(helper, 1, 1, 1));

        helper.assertTrue(buffer.addToContainer(aer(), 1) == 0 && buffer.addToContainer(ignis(), 1) == 0,
                "a buffer takes one unit at a time — and a mix, unlike a jar");
        helper.assertTrue(buffer.getEssentiaAmount(Direction.UP) == 2,
                "and holds them together, got " + buffer.getEssentiaAmount(Direction.UP));
        helper.assertTrue(buffer.addToContainer(aer(), 3) == 3,
                "a multi-unit add is refused outright (the original only ever moves one unit), got "
                        + buffer.addToContainer(aer(), 3));

        // The pool's own pressure: 1 with no bellows, floor(vis / 8 * 14) + 1 on the wire.
        helper.assertTrue(buffer.getSuctionAmount(Direction.UP) == 1,
                "an unbellowed buffer advertises the original's idle 1, got "
                        + buffer.getSuctionAmount(Direction.UP));
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at(helper, 1, 1, 1)) == 4,
                "two of eight units read floor(2 / 8 * 14) + 1 = 4, got "
                        + state.getAnalogOutputSignal(helper.getLevel(), at(helper, 1, 1, 1)));
        buffer.addToContainer(aer(), 1);
        buffer.addToContainer(aer(), 1);
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), at(helper, 1, 1, 1)) == 8,
                "half full reads 8, got "
                        + state.getAnalogOutputSignal(helper.getLevel(), at(helper, 1, 1, 1)));

        // A bellows pointing at the buffer multiplies its suction by 32.
        helper.setBlock(1, 2, 1, ((com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase)
                com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices.BLOCK_WOODEN_DEVICE.get())
                .withVariant(0));
        final com.jonnyutah12.thaumcraftclassic.tiles.TileBellows bellows =
                tubeAt(helper, 1, 2, 1, com.jonnyutah12.thaumcraftclassic.tiles.TileBellows.class);
        helper.assertTrue(bellows != null, "blockwoodendevice variant 0 must place the bellows");
        bellows.orientation = Direction.DOWN;
        buffer.getBellows();
        helper.assertTrue(buffer.bellows == 1, "the buffer must count the bellows pointing at it, got "
                + buffer.bellows);
        helper.assertTrue(buffer.getSuctionAmount(Direction.UP) == 32,
                "one bellows: 1 * 32, got " + buffer.getSuctionAmount(Direction.UP));

        // The per-face choke the original cycles through on a sneak-use.
        buffer.chokedSides[Direction.UP.ordinal()] = TubeTransportLogic.CHOKE_OFF;
        helper.assertTrue(buffer.getSuctionAmount(Direction.UP) == 0,
                "choke 2 shuts that face off");
        buffer.chokedSides[Direction.UP.ordinal()] = TubeTransportLogic.CHOKE_RESTRICTED;
        helper.assertTrue(buffer.getSuctionAmount(Direction.UP) == 1,
                "choke 1 ignores the bellows and falls back to 1");
        helper.succeed();
    }
}
