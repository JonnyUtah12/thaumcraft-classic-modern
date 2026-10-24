package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Crystal cluster block (content-mapping.csv {@code thaumcraft:blockcrystal}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCrystal} — one block id,
 * metadata 0-5 primal clusters (air/fire/water/earth/order/entropy), 6 mixed,
 * 7 strange crystals; ported verbatim to a {@code variant} property (D-001).
 * Properties are the original's: glass material, hardness 0.7, resistance 1.0,
 * light 7 (original 0.5 × 15), non-opaque. The original's custom
 * {@code CustomStepSound("crystal")} has no 1.21.1 {@code SoundType} yet
 * (TC-M7 sound work) — GLASS stands in.</p>
 *
 * <p>Behavior ported: support-any-face survival (original
 * {@code canBlockStay}), breaking when the stored attachment face loses its
 * support ({@code onNeighborBlockChange} + {@link TileCrystal#getOrientation()}),
 * and the shard drops ({@code getDrops}: six matching shards for a primal,
 * one of each primal for mixed, one balanced shard for strange — silk touch
 * never changed them in the original, and does not here).</p>
 *
 * <p>Variant 7 is a {@code TileEldritchCrystal} host (TC-M6-04f's row) —
 * supplied through {@link DeviceTileFactory}; its "break when the opposite
 * face becomes solid" rule rides with that tile. The original's
 * {@code IInfusionStabiliser} marker (infusion matrix stabilization, TC-M4-04)
 * has no ported interface yet; TC-M4-04 should key off this block id.</p>
 */
public class BlockCrystal extends Block implements VariantDevice, EntityBlock {

    /** Original blockCrystal metadata, verbatim 0-7 (see class javadoc). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);

    public static final int VARIANT_AIR = 0;
    public static final int VARIANT_FIRE = 1;
    public static final int VARIANT_WATER = 2;
    public static final int VARIANT_EARTH = 3;
    public static final int VARIANT_ORDER = 4;
    public static final int VARIANT_ENTROPY = 5;
    public static final int VARIANT_MIXED = 6;
    public static final int VARIANT_STRANGE = 7;

    /** Original drop source: {@code ConfigItems.itemShard} (TC-M6-04a row). */
    private static final ResourceLocation SHARD_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemshard");

    public BlockCrystal() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(0.7F, 1.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 7)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_AIR));
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * Original {@code canBlockStay}: the cluster survives while any face of
     * its cell touches a solid neighbor. The per-attachment-face check
     * ({@code canPlaceAt} keyed on the clicked side) is enforced after
     * placement by {@link #neighborChanged} through the tile's orientation.
     */
    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        for (final Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isFaceSturdy(level, pos.relative(direction),
                    direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Original {@code onNeighborBlockChange} for variants 0-6: when the face
     * recorded in {@link TileCrystal} loses its support, the cluster drops as
     * an item.
     */
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (!level.isClientSide && variant(state) <= VARIANT_MIXED
                && level.getBlockEntity(pos) instanceof TileCrystal tile) {
            final Direction orientation = tile.getOrientation();
            final BlockPos support = pos.relative(orientation.getOpposite());
            if (!level.getBlockState(support).isFaceSturdy(level, support, orientation)) {
                level.destroyBlock(pos, true);
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    /**
     * Original {@code getDrops}: six shards of the cluster's own kind for a
     * primal, one of each primal for the mixed cluster, one balanced shard
     * (meta 6) for strange crystals. The shard item is TC-M6-04a's row; it is
     * looked up by its contract id so this class carries no compile-time
     * dependency on that card's holder.
     */
    @Override
    public void getDrops(final BlockState state, final ServerLevel level, final BlockPos pos,
                         @Nullable final BlockEntity blockEntity, final ItemStack tool) {
        if (!BuiltInRegistries.ITEM.containsKey(SHARD_ID)) {
            super.getDrops(state, level, pos, blockEntity, tool);
            return;
        }
        final int variant = variant(state);
        if (variant <= VARIANT_ENTROPY) {
            for (int t = 0; t < 6; t++) {
                popResource(level, pos, shard(variant));
            }
        } else if (variant == VARIANT_MIXED) {
            for (int t = 0; t < 6; t++) {
                popResource(level, pos, shard(t));
            }
        } else if (variant == VARIANT_STRANGE) {
            popResource(level, pos, shard(VARIANT_STRANGE));
        } else {
            super.getDrops(state, level, pos, blockEntity, tool);
        }
    }

    private static ItemStack shard(final int meta) {
        final ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(SHARD_ID));
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    /**
     * Tile hosts: variants 0-6 carry the orientation tile (this card's
     * {@link TileCrystal}); variant 7's eldritch crystal tile (TC-M6-04f)
     * comes through {@link DeviceTileFactory}.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return variant(state) <= VARIANT_MIXED ? new TileCrystal(pos, state) : DeviceTileFactory.create(state, pos);
    }

    /** Preserve the variant through drops / pick-block (see {@link VariantStateItems}). */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final ServerLevel level,
                                      final BlockPos pos, final ItemStack tool) {
        return VariantStateItems.withVariant(super.getCloneItemStack(state, level, pos, tool), state);
    }
}
