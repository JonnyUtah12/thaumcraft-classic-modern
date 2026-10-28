package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockJar;
import com.jonnyutah12.thaumcraftclassic.items.ItemJarFilled;
import com.jonnyutah12.thaumcraftclassic.items.ItemPhial;
import com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillableVoid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-06-s: essentia extraction and storage — the infernal furnace
 * (base + nozzle), the warded and void jars, the glass phial, the filled-jar
 * item, and the smelting-bonus table.
 *
 * <p>One {@code Catalog<Name>} unit per the registration architecture
 * (docs/architecture/registration.md): its own {@code DeferredRegister}s, a
 * single {@code init(IEventBus)} line in
 * {@link com.jonnyutah12.thaumcraftclassic.registration.ModRegistration}, and
 * the ids exactly as docs/content-mapping.csv lists them — {@code blockjar},
 * {@code blockarcanefurnace}, {@code tilejar}, {@code tilejarvoid},
 * {@code tilearcanefurnace}, {@code tilearcanefurnacenozzle},
 * {@code itemessence}, {@code blockjarfilleditem}.</p>
 *
 * <p>Metadata mapping (D-001): the jar keeps the original's four blockJar
 * metadata values on a {@code variant} property — 0 warded, 1 brain, 2 node,
 * 3 void — because the landed arcane recipes already address them
 * ({@code recipe.138.arcane_shaped} places {@code variant} 3, the void jar).
 * Only 0 and 3 have tiles in this slice; 1 (TileJarBrain, TC-M6-05) and 2
 * (TileJarNode, TC-M6-03) stay with their cards. The furnace maps the
 * original's metadata 0 to {@code variant} 0 and metadata 10 (nozzle) to
 * {@code variant} 1.</p>
 *
 * <p>The smelting-bonus rows (content-mapping.csv
 * {@code recipe.225..242.smelting_bonus}) are registered here as the
 * original registers them at load: {@link SmeltingBonuses#registerOriginalBonuses()}.</p>
 */
public final class CatalogEssentiaStorage {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    // --- blocks ------------------------------------------------------------

    /** thaumcraft:blockjar — warded jar (0) / void jar (3). */
    public static final DeferredBlock<BlockJar> BLOCK_JAR = BLOCKS.register("blockjar", BlockJar::new);

    /** thaumcraft:blockarcanefurnace — base (0) / nozzle (1). */
    public static final DeferredBlock<BlockArcaneFurnace> BLOCK_ARCANE_FURNACE =
            BLOCKS.register("blockarcanefurnace", BlockArcaneFurnace::new);

    // --- items -------------------------------------------------------------

    public static final DeferredItem<BlockItem> ITEM_JAR =
            ITEMS.register("blockjar", () -> new BlockItem(BLOCK_JAR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_ARCANE_FURNACE =
            ITEMS.register("blockarcanefurnace",
                    () -> new BlockItem(BLOCK_ARCANE_FURNACE.get(), new Item.Properties()));

    /** thaumcraft:itemessence — Glass Phial (meta 0) / Phial of Essentia (meta 1). */
    public static final DeferredItem<ItemPhial> ITEM_ESSENCE =
            ITEMS.register("itemessence", () -> new ItemPhial(new Item.Properties()));

    /** thaumcraft:blockjarfilleditem — Jar of Essentia. */
    public static final DeferredItem<ItemJarFilled> ITEM_JAR_FILLED =
            ITEMS.register("blockjarfilleditem", () -> new ItemJarFilled(new Item.Properties()));

    // --- block entities ----------------------------------------------------

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarFillable>> TILE_JAR =
            BLOCK_ENTITY_TYPES.register("tilejar",
                    () -> BlockEntityType.Builder.of(TileJarFillable::new, BLOCK_JAR.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarFillableVoid>> TILE_JAR_VOID =
            BLOCK_ENTITY_TYPES.register("tilejarvoid",
                    () -> BlockEntityType.Builder.of(TileJarFillableVoid::new, BLOCK_JAR.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneFurnace>> TILE_ARCANE_FURNACE =
            BLOCK_ENTITY_TYPES.register("tilearcanefurnace",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnace::new, BLOCK_ARCANE_FURNACE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneFurnaceNozzle>>
            TILE_ARCANE_FURNACE_NOZZLE =
            BLOCK_ENTITY_TYPES.register("tilearcanefurnacenozzle",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnaceNozzle::new, BLOCK_ARCANE_FURNACE.get())
                            .build(null));

    private CatalogEssentiaStorage() {
    }

    public static void init(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        // The original registers its smelting-bonus rows at load, from
        // ConfigRecipes; this is the mod's load-time equivalent.
        SmeltingBonuses.registerOriginalBonuses();
    }

    // --- tile factories (the blocks' newBlockEntity seam) ------------------

    public static BlockEntity createJar(final BlockPos pos, final BlockState state) {
        return new TileJarFillable(pos, state);
    }

    public static BlockEntity createJarVoid(final BlockPos pos, final BlockState state) {
        return new TileJarFillableVoid(pos, state);
    }

    public static BlockEntity createFurnace(final BlockPos pos, final BlockState state) {
        return new TileArcaneFurnace(pos, state);
    }

    public static BlockEntity createFurnaceNozzle(final BlockPos pos, final BlockState state) {
        return new TileArcaneFurnaceNozzle(pos, state);
    }

    /** The jar state for an item's original metadata (0 warded, 3 void). */
    public static BlockState jarState(final boolean voidJar) {
        return BLOCK_JAR.get().defaultBlockState()
                .setValue(BlockJar.VARIANT, voidJar ? BlockJar.VARIANT_VOID : BlockJar.VARIANT_WARDED);
    }

    public static boolean isVoidJar(final BlockState state) {
        return state.is(BLOCK_JAR.get()) && state.getValue(BlockJar.VARIANT) == BlockJar.VARIANT_VOID;
    }
}
