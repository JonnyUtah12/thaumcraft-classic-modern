package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meta-to-tile factory for the shared multi-variant device blocks
 * (TC-M6-04c; docs/catalog-triage.md shared multi-meta block map).
 *
 * <p>The original BlockStoneDevice/BlockMetalDevice/BlockWoodenDevice/
 * BlockAiry chose their TileEntity in {@code createTileEntity(world, meta)}.
 * In 1.21.1 the block entity types are bound at registration and the block
 * picks the instance in {@code newBlockEntity}; the device blocks are owned by
 * TC-M6-04c and their tiles are owned by many feature cards, so the mapping
 * lives here: the owning feature card registers its supplier for
 * (block, variant) from its own registration holder, and the device block
 * consults this table. Feature cards never edit the device block class
 * (docs/catalog-triage.md hotspot rule).</p>
 *
 * <p>Suppliers are registered during mod construction (constructor-time rule,
 * docs/architecture/registration.md) — before any chunk loads — so the plain
 * concurrent map is only a defensive measure against load-order surprises.</p>
 */
public final class DeviceTileFactory {

    /** A block-entity supplier keyed by (block, variant). */
    @FunctionalInterface
    public interface Supplier {
        @Nullable BlockEntity create(BlockPos pos, BlockState state);
    }

    private record Key(Block block, int variant) {
    }

    private static final Map<Key, Supplier> SUPPLIERS = new ConcurrentHashMap<>();

    /**
     * Register the tile supplier for one (block, variant) pair. Called by the
     * feature card that owns the tile, from its own registration holder.
     */
    public static void register(final Block block, final int variant, final Supplier supplier) {
        final Supplier previous = SUPPLIERS.put(new Key(block, variant), supplier);
        if (previous != null) {
            throw new IllegalStateException("Duplicate device tile supplier for "
                    + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block)
                    + " variant " + variant);
        }
    }

    /** Create the block entity for this state, or null when the variant has none. */
    @Nullable
    public static BlockEntity create(final BlockState state, final BlockPos pos) {
        if (!(state.getBlock() instanceof VariantDevice device)) {
            return null;
        }
        final Supplier supplier = SUPPLIERS.get(new Key(state.getBlock(), device.variant(state)));
        return supplier == null ? null : supplier.create(pos, state);
    }

    private DeviceTileFactory() {
    }
}
