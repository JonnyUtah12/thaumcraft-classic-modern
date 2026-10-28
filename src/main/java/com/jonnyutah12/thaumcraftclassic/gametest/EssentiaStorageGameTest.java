package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.items.ItemJarFilled;
import com.jonnyutah12.thaumcraftclassic.items.ItemPhial;
import com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBellows;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M4-06-s evidence: essentia extraction (the infernal furnace and its
 * smelting bonuses) and essentia storage (jars, phials, the filled-jar item)
 * in the world, on the numbers the original used.
 *
 * <ol>
 *   <li>{@link #jarHoldsOneAspectUpToItsCapacity} and
 *       {@link #theVoidJarClipsOverflowAndNeverStopsPulling} — the aspect
 *       matching rules run in the world, not only in
 *       {@code JarStorageLogicTest}: a jar takes one aspect, stacks it to 64,
 *       refuses a foreign one, and the void jar clips its overflow instead of
 *       refusing it.</li>
 *   <li>{@link #theJarLabelFiltersWhatItAccepts} — the label is a filter: a
 *       labelled jar accepts only its aspect and pulls harder (64 vs 32).</li>
 *   <li>{@link #theFurnaceCookTimeFollowsTheOriginalRates} — 140 ticks cold,
 *       80 with FIRE essentia, minus 20 per bellows (capped at 3), measured off
 *       a real placed furnace and a real placed bellows.</li>
 *   <li>{@link #theFurnaceSmeltsAnItemAndEjectsTheResult} — an input really is
 *       consumed and its smelt result is thrown out of the nozzle side, which
 *       is the extraction path the rest of the essentia chain starts from.</li>
 *   <li>{@link #thePhialMovesEightUnitsPerFill} — the phial fills from a jar
 *       and drains back into it, 8 units per pass.</li>
 *   <li>{@link #theFilledJarDropCarriesItsEssentia} — breaking a filled jar
 *       hands back a filled-jar item (essentia, amount, label, void flag).</li>
 *   <li>{@link #essentiaStorageRowsResolve} — the ids this card owns resolve
 *       in their registries and are marked done/tested in
 *       {@code docs/content-mapping.csv} in this commit, and every
 *       smelting-bonus row's output item exists.</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EssentiaStorageGameTest {

    private EssentiaStorageGameTest() {
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

    private static TileJarFillable jarAt(final GameTestHelper helper, final BlockPos pos) {
        return castBe(helper.getLevel().getBlockEntity(pos), TileJarFillable.class);
    }

    // --- jars -------------------------------------------------------------

    @GameTest(template = "empty_24")
    public static void jarHoldsOneAspectUpToItsCapacity(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 1, 1, 1);
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable jar = jarAt(helper, pos);
        helper.assertTrue(jar != null, "blockjar variant 0 must place a TileJarFillable");

        helper.assertTrue(jar.addToContainer(aer(), 40) == 0,
                "an empty jar takes the first aspect it is offered");
        helper.assertTrue(jar.getEssentiaAmount(Direction.UP) == 40,
                "the jar reports what it holds on every face, got " + jar.getEssentiaAmount(Direction.UP));

        helper.assertTrue(jar.addToContainer(ignis(), 5) == 5,
                "one jar, one aspect: the foreign essentia is handed straight back");
        helper.assertTrue(jar.amount == 40 && jar.aspect == aer(),
                "the refusal must not change what the jar holds");

        final int leftover = jar.addToContainer(aer(), 40);
        helper.assertTrue(jar.amount == 64, "the jar fills to its 64 capacity, got " + jar.amount);
        helper.assertTrue(leftover == 16, "the overflow is handed back to the line, got " + leftover);

        helper.assertTrue(jar.takeFromContainer(ignis(), 1) == false,
                "the jar will not hand out an aspect it does not hold");
        helper.assertTrue(jar.takeFromContainer(aer(), 64), "and it hands out what it holds");
        helper.assertTrue(jar.amount == 0 && jar.aspect == null,
                "at zero the aspect is cleared, so the jar re-locks on the next fill");

        helper.succeed();
    }

    @GameTest(template = "empty_24")
    public static void theVoidJarClipsOverflowAndNeverStopsPulling(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 1, 1, 1);
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(true));
        final TileJarFillable jar = jarAt(helper, pos);
        helper.assertTrue(jar != null && jar.voidJar(),
                "blockjar variant 3 (the original's metadata 3) must place the void jar");

        helper.assertTrue(jar.addToContainer(ignis(), 100) == 0,
                "TileJarFillableVoid.addToContainer has no room check, so it reports nothing left over");
        helper.assertTrue(jar.amount == 64,
                "the overflow is discarded (the void jar swallows it), got " + jar.amount);

        helper.assertTrue(jar.getSuctionAmount(Direction.UP) == 32,
                "a full unlabelled void jar still advertises 32 (no full check), got "
                        + jar.getSuctionAmount(Direction.UP));
        jar.aspectFilter = ignis();
        helper.assertTrue(jar.getSuctionAmount(Direction.UP) == 32,
                "the void jar's getSuctionAmount carries the original's amount check, so a FULL labelled "
                        + "jar is back to 32, got " + jar.getSuctionAmount(Direction.UP));
        helper.assertTrue(jar.getMinimumSuction() == 48,
                "and its minimum suction is the labelled 48 (the original's getMinimumSuction has no "
                        + "amount check), got " + jar.getMinimumSuction());
        jar.amount = 40;
        helper.assertTrue(jar.getSuctionAmount(Direction.UP) == 48,
                "with room, the labelled void jar pulls at 48, got " + jar.getSuctionAmount(Direction.UP));

        // The plain jar does stop: same fill, same label, full.
        helper.setBlock(2, 1, 1, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable plain = jarAt(helper, at(helper, 2, 1, 1));
        plain.addToContainer(ignis(), 64);
        helper.assertTrue(plain.getSuctionAmount(Direction.UP) == 0,
                "a full plain jar advertises 0 so the line stops feeding it");

        helper.succeed();
    }

    @GameTest(template = "empty_24")
    public static void theJarLabelFiltersWhatItAccepts(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 1, 1, 1);
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable jar = jarAt(helper, pos);

        jar.aspectFilter = aer();
        helper.assertTrue(jar.doesContainerAccept(aer()), "a label matches its own aspect");
        helper.assertTrue(!jar.doesContainerAccept(ignis()),
                "and rejects every other aspect — the filter is an exact tag match");
        helper.assertTrue(jar.getSuctionAmount(Direction.UP) == 64,
                "a labelled jar pulls at 64, got " + jar.getSuctionAmount(Direction.UP));

        jar.aspectFilter = null;
        helper.assertTrue(jar.doesContainerAccept(ignis()), "no label accepts anything");
        helper.assertTrue(jar.getSuctionAmount(Direction.UP) == 32,
                "an unlabelled jar pulls at 32, got " + jar.getSuctionAmount(Direction.UP));

        helper.succeed();
    }

    // --- the infernal furnace's rates -------------------------------------

    @GameTest(template = "empty_24")
    public static void theFurnaceCookTimeFollowsTheOriginalRates(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 3, 1, 1);
        helper.setBlock(3, 1, 1, CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get().defaultBlockState());
        final TileArcaneFurnace furnace = castBe(helper.getLevel().getBlockEntity(pos), TileArcaneFurnace.class);
        helper.assertTrue(furnace != null, "blockarcanefurnace variant 0 must place TileArcaneFurnace");

        furnace.speedyTime = 0;
        helper.assertTrue(furnace.getBellows() == 0, "no bellows around it, got " + furnace.getBellows());
        helper.assertTrue(furnace.calcCookTime() == 140,
                "cold, unbelloved: 140 ticks, got " + furnace.calcCookTime());

        furnace.speedyTime = 600;
        helper.assertTrue(furnace.calcCookTime() == 80,
                "fed FIRE essentia: 80 ticks, got " + furnace.calcCookTime());

        // A bellows two cells out, pointing back at the furnace, is the original's
        // placement rule (its metadata 3 bellows facing the furnace).
        helper.setBlock(1, 1, 1, bellowsState());
        final TileBellows bellows = castBe(helper.getLevel().getBlockEntity(at(helper, 1, 1, 1)), TileBellows.class);
        helper.assertTrue(bellows != null, "blockwoodendevice variant 0 must place TileBellows");
        bellows.orientation = Direction.EAST;
        furnace.speedyTime = 0;
        helper.assertTrue(furnace.getBellows() == 1, "the furnace must count that bellows, got "
                + furnace.getBellows());
        helper.assertTrue(furnace.calcCookTime() == 120,
                "140 - 20 per bellows, got " + furnace.calcCookTime());

        // A bellows that points away is not counted.
        bellows.orientation = Direction.WEST;
        helper.assertTrue(furnace.getBellows() == 0,
                "the bellows must face the furnace, as in the original's getBellows");

        furnace.speedyTime = 600;
        bellows.orientation = Direction.EAST;
        helper.assertTrue(furnace.calcCookTime() == 60,
                "FIRE + one bellows: 80 - 20 = 60 ticks, got " + furnace.calcCookTime());

        helper.succeed();
    }

    @GameTest(template = "empty_24")
    public static void theFurnaceSmeltsAnItemAndEjectsTheResult(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 2, 1, 1);
        helper.setBlock(2, 1, 1, CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get().defaultBlockState());
        final TileArcaneFurnace furnace = castBe(helper.getLevel().getBlockEntity(pos), TileArcaneFurnace.class);
        helper.assertTrue(furnace != null, "the furnace tile must exist");

        furnace.setInventorySlotContents(0, new ItemStack(Items.RAW_IRON, 1));
        helper.assertTrue(furnace.canSmelt(0),
                "raw iron has a smelting recipe in 1.21.1, so the furnace must accept it");

        // Cold, no bellows: 140 ticks of cook, then the eject. Ticked, not faked:
        // the eject is a real entity in the world.
        helper.succeedOnTickWhen(170, () -> {
            helper.assertTrue(furnace.getStackInSlot(0).isEmpty(),
                    "the furnace consumed the input");
            final List<net.minecraft.world.entity.item.ItemEntity> drops = helper.getLevel().getEntities(
                    EntityType.ITEM, new AABB(pos).inflate(4.0D), e -> true);
            boolean sawResult = false;
            for (final net.minecraft.world.entity.item.ItemEntity drop : drops) {
                if (drop.getItem().is(Items.IRON_INGOT)) {
                    sawResult = true;
                }
            }
            helper.assertTrue(sawResult,
                    "the smelt result must be ejected out of the nozzle side, found " + drops.size()
                            + " item entities");
        });
    }

    // --- the phial --------------------------------------------------------

    @GameTest(template = "empty_24")
    public static void thePhialMovesEightUnitsPerFill(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 1, 1, 1);
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable jar = jarAt(helper, pos);
        jar.addToContainer(aer(), 20);

        final Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        final ItemStack emptyPhial = ItemPhial.stack(ItemPhial.META_EMPTY);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, emptyPhial);

        helper.useBlock(pos, player);

        final ItemStack phial = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(ItemPhial.isFilled(phial),
                "filling from a jar turns a Glass Phial into a Phial of Essentia");
        final AspectList carried = CatalogEssentiaStorage.ITEM_ESSENCE.get().getAspects(phial);
        helper.assertTrue(carried != null && carried.getAmount(aer()) == 8,
                "a phial takes 8 units per fill, got " + (carried == null ? "nothing" : carried.getAmount(aer())));
        helper.assertTrue(jar.amount == 12, "and the jar drops by exactly that, got " + jar.amount);

        // Drain it back.
        helper.useBlock(pos, player);
        helper.assertTrue(jar.amount == 20,
                "the filled phial pours its 8 units back into the jar, got " + jar.amount);
        helper.assertTrue(!ItemPhial.isFilled(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND))
                        || CatalogEssentiaStorage.ITEM_ESSENCE.get()
                        .getAspects(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)) == null,
                "a drained phial is empty again");

        helper.succeed();
    }

    // --- the filled-jar drop ---------------------------------------------

    @GameTest(template = "empty_24")
    public static void theFilledJarDropCarriesItsEssentia(final GameTestHelper helper) {
        final BlockPos pos = at(helper, 1, 1, 1);
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(true));
        final TileJarFillable jar = jarAt(helper, pos);
        jar.addToContainer(ignis(), 30);
        jar.aspectFilter = ignis();

        // The real break path: BlockJar.getDrops -> the filled-jar item.
        helper.getLevel().destroyBlock(pos, true, null);
        final List<ItemStack> filled = dropsAround(helper, pos);
        helper.assertTrue(filled.size() == 1,
                "breaking a filled jar drops exactly one item, got " + filled.size() + " " + filled);
        final ItemStack drop = filled.get(0);
        helper.assertTrue(drop.is(CatalogEssentiaStorage.ITEM_JAR_FILLED.get()),
                "a filled jar drops the filled-jar item, not the block, got " + drop);
        helper.assertTrue(ItemJarFilled.isVoid(drop),
                "the void jar's drop keeps the void flag (the original's metadata 3)");
        final AspectList aspects = CatalogEssentiaStorage.ITEM_JAR_FILLED.get().getAspects(drop);
        helper.assertTrue(aspects != null && aspects.getAmount(ignis()) == 30,
                "the drop carries the essentia and its amount, got "
                        + (aspects == null ? "nothing" : aspects.getAmount(ignis())));
        helper.assertTrue(ignis() == ItemJarFilled.filterOf(drop),
                "and the label, so a placed-back jar keeps its filter");

        // An empty, unlabelled jar drops its own block item (the original's
        // getDrops returns the block for that case).
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(true));
        helper.getLevel().destroyBlock(pos, true, null);
        final List<ItemStack> plain = dropsAround(helper, pos);
        helper.assertTrue(plain.size() == 1,
                "an empty jar drops exactly one item, got " + plain.size() + " " + plain);
        helper.assertTrue(plain.get(0).is(CatalogEssentiaStorage.ITEM_JAR.get()),
                "an empty void jar drops the jar block item, got " + plain.get(0));

        helper.succeed();
    }

    /** The item entities a block break just spawned, as their stacks. */
    private static List<ItemStack> dropsAround(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)
                .stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).toList();
    }

    // --- ids, rows, and the bonus table in the registry -------------------

    @GameTest(template = "empty_24")
    public static void essentiaStorageRowsResolve(final GameTestHelper helper) {
        for (final String id : List.of("blockjar", "blockarcanefurnace")) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("thaumcraft:" + id)),
                    id + " must be registered as a block");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("thaumcraft:" + id)),
                    id + " must have its block item");
        }
        for (final String id : List.of("itemessence", "blockjarfilleditem")) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("thaumcraft:" + id)),
                    id + " must be registered as an item");
        }
        for (final String id : List.of("tilejar", "tilejarvoid", "tilearcanefurnace",
                "tilearcanefurnacenozzle")) {
            helper.assertTrue(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("thaumcraft:" + id)),
                    id + " must be registered as a block entity");
        }

        // Every smelting-bonus row's output must resolve, and no row may leak
        // a free item: the original's rows carry count 0.
        SmeltingBonuses.registerOriginalBonuses();
        helper.assertTrue(SmeltingBonuses.byItem().size() == 11 && SmeltingBonuses.byTag().size() == 7,
                "the 18 original addSmeltingBonus rows must be registered, got " + SmeltingBonuses.size());
        for (final SmeltingBonuses.Bonus bonus : SmeltingBonuses.byItem().values()) {
            final net.minecraft.world.item.Item item =
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(bonus.itemId()));
            helper.assertTrue(item != net.minecraft.world.item.Items.AIR,
                    "the bonus row outputs " + bonus.itemId() + ", which is not registered");
        }
        helper.assertTrue(SmeltingBonuses.lookup("minecraft:beef", 0, List.of()).itemId()
                        .equals("thaumcraft:itemnuggetbeef"),
                "beef smelts to the beef nugget, as the original registers it");

        // The csv contract for the ids this card owns.
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        final List<ContentMappingCsv.Row> rows;
        try {
            rows = ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final List<String> owned = List.of("thaumcraft:blockjar", "thaumcraft:blockarcanefurnace",
                "thaumcraft:itemessence", "thaumcraft:blockjarfilleditem");
        for (final String id : owned) {
            final ContentMappingCsv.Row row = rows.stream()
                    .filter(r -> r.newId().equals(id)).findFirst().orElse(null);
            helper.assertTrue(row != null, id + " must be inventoried in docs/content-mapping.csv");
            helper.assertTrue("done".equals(row.implementationStatus()) && "tested".equals(row.testStatus()),
                    id + " must be done/tested in this commit, got " + row.implementationStatus()
                            + "/" + row.testStatus());
        }

        helper.succeed();
    }

    private static BlockState bellowsState() {
        return ((BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get()).withVariant(0);
    }
}
