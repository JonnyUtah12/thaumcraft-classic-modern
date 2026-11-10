package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritchPortal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchPortal;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.MazeHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-04f-m: the Outer Lands slice registration — the eldritch portal
 * block + tile (content-mapping.csv {@code thaumcraft:blockportaleldritch} /
 * {@code thaumcraft:tileeldritchportal}) and the maze/boss-room handler
 * install behind the shipped seams.
 *
 * <p>The idiom is the shipped one (CatalogEldritchRealm): a
 * {@link DeferredRegister} per registry, wired once from
 * {@link com.jonnyutah12.thaumcraftclassic.registration.ModRegistration}.
 * The dimension, its type and the biome ship as data JSONs
 * ({@code data/thaumcraft/dimension/dimension.-42.json},
 * {@code data/thaumcraft/dimension_type/outerlands.json},
 * {@code data/thaumcraft/worldgen/biome/biomegeneldritch.json}) — 1.21.1
 * registries built from data need no code registration.</p>
 */
public final class CatalogOuterLands {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID);

    /** Original {@code LanguageRegistry.addName(blockPortalEldritch, "Eldritch Portal")}. */
    public static final DeferredBlock<BlockEldritchPortal> BLOCK_ELDRITCH_PORTAL =
            BLOCKS.register("blockportaleldritch", BlockEldritchPortal::new);

    /** The portal tile: the original's per-5-tick player scan + travel. */
    @SuppressWarnings("deprecation")
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>,
            BlockEntityType<TileEldritchPortal>> TILE_ELDRITCH_PORTAL =
            BLOCK_ENTITY_TYPES.register("tileeldritchportal",
                    () -> BlockEntityType.Builder.of(TileEldritchPortal::new, BLOCK_ELDRITCH_PORTAL.get())
                            .build(null));

    private CatalogOuterLands() {
    }

    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // The handler install is idempotent (two static seam setters) and runs
        // once per client/server process, exactly where the shipped slices do
        // their install work.
        MazeHandler.install();
    }
}
