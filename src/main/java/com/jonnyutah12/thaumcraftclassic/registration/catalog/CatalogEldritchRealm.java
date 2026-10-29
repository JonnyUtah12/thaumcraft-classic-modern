package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritchNothing;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantBlockItem;
import com.jonnyutah12.thaumcraftclassic.fluids.BlockFluidDeath;
import com.jonnyutah12.thaumcraftclassic.fluids.BlockFluidPure;
import com.jonnyutah12.thaumcraftclassic.fluids.FluidDeath;
import com.jonnyutah12.thaumcraftclassic.fluids.FluidPure;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;
import com.jonnyutah12.thaumcraftclassic.registration.ModBlockEntities;
import com.jonnyutah12.thaumcraftclassic.registration.ModBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchAltar;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCap;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCrabSpawner;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchLock;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchNothing;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchObelisk;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * TC-M6-04f-s catalog card: the Eldritch realm's <em>content</em> — the
 * outer-lands blocks, structures' parts, fluids and items, plus the damage
 * types they deal (data/thaumcraft/damage_type). The dimension, portal and
 * structure placement stay on the follow-up card (TC-M6-04f-m); the maze
 * generation shipped in f252c58.
 *
 * <p>Rows: {@code thaumcraft:blockeldritch} (11 variants),
 * {@code thaumcraft:blockeldritchnothing} (2 variants),
 * {@code thaumcraft:fluidpure}/{@code thaumcraft:fluiddeath} (+ their
 * flowing halves, blocks and buckets),
 * {@code thaumcraft:itemeldritchobject} (8 metas),
 * {@code thaumcraft:tileeldritchaltar}/{@code obelisk}/{@code cap}/
 * {@code nothing}/{@code lock}/{@code crabspawner},
 * {@code thaumcraft:damagetype.taint} and {@code thaumcraft:damagetype.dissolve}
 * (the two damage types the original registers; their JSONs live under
 * {@code data/thaumcraft/damage_type/}).</p>
 *
 * <p>Every id is the CSV {@code new_id}; the tile entities bind per
 * (block, variant) through {@link DeviceTileFactory} exactly where the
 * original bound them (altar 0, obelisk 1, cap 3, lock 8, crab spawner 9,
 * nothing 1). The variant names and localization are in
 * {@link BlockVariantNames}; the numeric parity contract — blast resistance,
 * hardness, the experience table, the tick cadences, the trap rules, the lock
 * fuse — is in {@link EldritchRealmValues}.</p>
 */
public final class CatalogEldritchRealm {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ThaumcraftClassic.MOD_ID);

    // ------------------------------------------------------------------
    // Variants
    // ------------------------------------------------------------------

    /** blockEldritchNothing: 0 = shell, 1 = hollow (nothing). */
    public static final List<String> NOTHING_VARIANTS = List.of("shell", "hollow");

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    /** Block Eldritch (content-mapping.csv {@code thaumcraft:blockeldritch}). */
    public static final DeferredHolder<Block, BlockEldritch> BLOCK_ELDRITCH =
            BLOCKS.register("blockeldritch", () -> new BlockEldritch());

    /** Block Eldritch Nothing (content-mapping.csv {@code thaumcraft:blockeldritchnothing}). */
    public static final DeferredHolder<Block, BlockEldritchNothing> BLOCK_ELDRITCH_NOTHING =
            BLOCKS.register("blockeldritchnothing", BlockEldritchNothing::new);

    // ------------------------------------------------------------------
    // Fluids (pure = clean water; death = the dissolving flow)
    // ------------------------------------------------------------------

    public static final DeferredHolder<FluidType, FluidType> FLUID_TYPE_PURE =
            FLUID_TYPES.register("fluidpure", () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.thaumcraft.fluidpure")
                            .motionScale(1.0)
                            .fallDistanceModifier(0.0F)
                            .canExtinguish(true)
                            .canSwim(true)
                            .canDrown(false)
                            .density(1000)
                            .viscosity(1000)
                            .temperature(20)));

    public static final DeferredHolder<FluidType, FluidType> FLUID_TYPE_DEATH =
            FLUID_TYPES.register("fluiddeath", () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.thaumcraft.fluiddeath")
                            .motionScale(0.5)
                            .fallDistanceModifier(0.0F)
                            .canExtinguish(false)
                            .canSwim(false)
                            .canDrown(false)
                            .density(3000)
                            .viscosity(6000)
                            .temperature(0)));

    // The fluid quartets are mutually referencing (fluid <-> type, block,
    // bucket); javac rejects simple-name forward/self references inside a
    // static initializer, so every lambda references the field by its
    // class-qualified name. All five suppliers run at RegisterEvent time —
    // after every DeferredHolder in this class exists.

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FluidPure> FLUID_PURE =
            FLUIDS.register("fluidpure", () -> new FluidPure(new BaseFlowingFluid.Properties(
                    () -> CatalogEldritchRealm.FLUID_TYPE_PURE.get(), () -> CatalogEldritchRealm.FLUID_PURE.get(),
                    () -> CatalogEldritchRealm.FLUID_PURE_FLOWING.get())
                    .block(() -> CatalogEldritchRealm.BLOCK_FLUID_PURE.get())
                    .bucket(() -> CatalogEldritchRealm.BUCKET_PURE.get())));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FluidPure.Flowing> FLUID_PURE_FLOWING =
            FLUIDS.register("fluidpure_flowings", () -> new FluidPure.Flowing(new BaseFlowingFluid.Properties(
                    () -> CatalogEldritchRealm.FLUID_TYPE_PURE.get(), () -> CatalogEldritchRealm.FLUID_PURE.get(),
                    () -> CatalogEldritchRealm.FLUID_PURE_FLOWING.get())
                    .block(() -> CatalogEldritchRealm.BLOCK_FLUID_PURE.get())
                    .bucket(() -> CatalogEldritchRealm.BUCKET_PURE.get())));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FluidDeath> FLUID_DEATH =
            FLUIDS.register("fluiddeath", () -> new FluidDeath(new BaseFlowingFluid.Properties(
                    () -> CatalogEldritchRealm.FLUID_TYPE_DEATH.get(), () -> CatalogEldritchRealm.FLUID_DEATH.get(),
                    () -> CatalogEldritchRealm.FLUID_DEATH_FLOWING.get())
                    .block(() -> CatalogEldritchRealm.BLOCK_FLUID_DEATH.get())
                    .bucket(() -> CatalogEldritchRealm.BUCKET_DEATH.get())));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FluidDeath.Flowing> FLUID_DEATH_FLOWING =
            FLUIDS.register("fluiddeath_flowings", () -> new FluidDeath.Flowing(new BaseFlowingFluid.Properties(
                    () -> CatalogEldritchRealm.FLUID_TYPE_DEATH.get(), () -> CatalogEldritchRealm.FLUID_DEATH.get(),
                    () -> CatalogEldritchRealm.FLUID_DEATH_FLOWING.get())
                    .block(() -> CatalogEldritchRealm.BLOCK_FLUID_DEATH.get())
                    .bucket(() -> CatalogEldritchRealm.BUCKET_DEATH.get())));

    public static final DeferredHolder<Block, BlockFluidPure> BLOCK_FLUID_PURE =
            BLOCKS.register("blockfluidpure", () -> new BlockFluidPure(CatalogEldritchRealm.FLUID_PURE.get(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollission().strength(100.0F, 100.0F)
                            .pushReaction(PushReaction.DESTROY).noLootTable().liquid().replaceable()));

    public static final DeferredHolder<Block, BlockFluidDeath> BLOCK_FLUID_DEATH =
            BLOCKS.register("blockfluiddeath", () -> new BlockFluidDeath(CatalogEldritchRealm.FLUID_DEATH.get(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission().strength(100.0F, 100.0F)
                            .pushReaction(PushReaction.DESTROY).noLootTable().liquid().replaceable()));

    // 1.21.1 BucketItem(Fluid, Properties): the empty bucket is returned by
    // the bucket's own pickup path (BucketItem.use replaces the stack); the
    // 1.7.10 setContainerItem is the recipe's minecraft:recipe_containers
    // component in 1.21.1, and no crafting recipe for these buckets ships in
    // this card, so there is no crafting remainder to wire.
    public static final DeferredHolder<Item, BucketItem> BUCKET_PURE =
            ITEMS.register("itembucketpure", () -> new BucketItem(CatalogEldritchRealm.FLUID_PURE.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, BucketItem> BUCKET_DEATH =
            ITEMS.register("itembucketdeath", () -> new BucketItem(CatalogEldritchRealm.FLUID_DEATH.get(),
                    new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------
    // Item
    // ------------------------------------------------------------------

    /** Eldritch Objects (content-mapping.csv {@code thaumcraft:itemeldritchobject}). */
    public static final DeferredHolder<Item, ItemEldritchObject> ITEM_ELDRITCH_OBJECT =
            ITEMS.register("itemeldritchobject", ItemEldritchObject::new);

    // ------------------------------------------------------------------
    // Block items (variant-carrying, the house mega-item pattern)
    // ------------------------------------------------------------------

    public static final DeferredHolder<Item, BlockItem> BLOCK_ELDRITCH_ITEM =
            ITEMS.register("blockeldritch", () -> new VariantBlockItem(BLOCK_ELDRITCH.get(),
                    new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> BLOCK_ELDRITCH_NOTHING_ITEM =
            ITEMS.register("blockeldritchnothing", () -> new VariantBlockItem(BLOCK_ELDRITCH_NOTHING.get(),
                    new Item.Properties()));

    // ------------------------------------------------------------------
    // Block entities, bound per (block, variant) as the original bound them
    // ------------------------------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchAltar>> TILE_ELDRITCH_ALTAR =
            BLOCK_ENTITY_TYPES.register("tileeldritchaltar",
                    () -> BlockEntityType.Builder.of(TileEldritchAltar::new,
                            BLOCK_ELDRITCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchObelisk>> TILE_ELDRITCH_OBELISK =
            BLOCK_ENTITY_TYPES.register("tileeldritchobelisk",
                    () -> BlockEntityType.Builder.of(TileEldritchObelisk::new,
                            BLOCK_ELDRITCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchCap>> TILE_ELDRITCH_CAP =
            BLOCK_ENTITY_TYPES.register("tileeldritchcap",
                    () -> BlockEntityType.Builder.of(TileEldritchCap::new,
                            BLOCK_ELDRITCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchNothing>> TILE_ELDRITCH_NOTHING =
            BLOCK_ENTITY_TYPES.register("tileeldritchnothing",
                    () -> BlockEntityType.Builder.of(TileEldritchNothing::new,
                            BLOCK_ELDRITCH_NOTHING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchLock>> TILE_ELDRITCH_LOCK =
            BLOCK_ENTITY_TYPES.register("tileeldritchlock",
                    () -> BlockEntityType.Builder.of(TileEldritchLock::new,
                            BLOCK_ELDRITCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEldritchCrabSpawner>> TILE_ELDRITCH_CRAB_SPAWNER =
            BLOCK_ENTITY_TYPES.register("tileeldritchcrabspawner",
                    () -> BlockEntityType.Builder.of(TileEldritchCrabSpawner::new,
                            BLOCK_ELDRITCH.get()).build(null));

    public static void init(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        bus.addListener((final net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                registerTiles();
            }
        });
    }

    /**
     * The (block, variant) -&gt; tile bindings of the original's
     * {@code BlockEldritch.createTile} / {@code BlockEldritchNothing.createTile}:
     * altar 0, obelisk 1, cap 3, lock 8, crab spawner 9, nothing 1.
     */
    public static void registerTiles() {
        DeviceTileFactory.register(BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_ALTAR, TileEldritchAltar::new);
        DeviceTileFactory.register(BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK, TileEldritchObelisk::new);
        DeviceTileFactory.register(BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_CAP, TileEldritchCap::new);
        DeviceTileFactory.register(BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_LOCK, TileEldritchLock::new);
        DeviceTileFactory.register(BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_CRAB_SPAWNER, TileEldritchCrabSpawner::new);
        DeviceTileFactory.register(BLOCK_ELDRITCH_NOTHING.get(), EldritchRealmValues.NOTHING_VARIANT_EXPOSED, TileEldritchNothing::new);
        com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.LOGGER.info(
                "[TC-M6-04f-s] eldritch (block, variant) -> tile suppliers wired: altar 0, obelisk 1, cap 3, lock 8, crab spawner 9 on blockeldritch; nothing 1 on blockeldritchnothing");
    }

    // Shapes ride the blockstate JSONs (docs/architecture/assets.md): the
    // variant property maps to the original's per-meta models with the
    // textures; the nothing family is a plain cube.

    private CatalogEldritchRealm() {
    }
}
