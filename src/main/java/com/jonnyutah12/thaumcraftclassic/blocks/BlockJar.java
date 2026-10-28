package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.items.ItemJarFilled;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.JarStorageLogic;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The essentia jar block (content-mapping.csv {@code thaumcraft:blockjar}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockJar}, one block id with
 * metadata 0 (warded jar) / 1 (void jar) / 2 (node jar) / 3 (node jar with
 * contents). The port keeps the first two on a {@code variant} property
 * (D-001); metadata 2-3 are the visio altar's node jars and stay with their
 * owner card (TC-M6-03). Properties are the original's: glass-like material,
 * hardness 0.3, resistance 0.66, non-opaque; the original's custom
 * {@code CustomStepSound("jar")} has no 1.21.1 {@code SoundType} yet (TC-M7),
 * so GLASS stands in, exactly as for {@link BlockCrystal}.</p>
 *
 * <p>Behavior ported: the label flow (sneak-right-click with a jar label —
 * itemResource meta 13 — sets the jar's aspect filter, consumes one label and
 * pops the jar's own block item out, the original's quirk of handing the jar
 * back so the label survives a placement cycle; the label's carried aspect
 * seeds the jar when it is empty), the sneak-empty-hand dump (empties the
 * jar, and clears the aspect too when no label is set), the label removal
 * (sneak-right-click on the labelled face with a label-free jar → the label
 * pops back out), the 5-tick fill cadence through {@link TileJarFillable}, and
 * the redstone level, which rides as the 1.21.1 comparator read (the original
 * emitted it as weak power from the tile — the jar block has no wire-only
 * signal path in 1.21.1).</p>
 *
 * <p>Out of scope, named for their cards: the node-jar variants (TC-M6-03),
 * the jar brain (TileJarBrain, the mind tile behind metadata 1 — its XP,
 * enchantment bonus and item-devouring loop is TC-M6-05), the jar's liquid
 * fill/glow rendering and spell particles (TC-M7-01), and the light level 11
 * the original gives the node jar only.</p>
 */
public class BlockJar extends Block implements EntityBlock {

    /**
     * The original's four blockJar metadata values on one property (D-001):
     * 0 warded, 1 brain, 2 node, 3 void. This slice has tiles for 0 and 3;
     * the landed {@code recipe.138.arcane_shaped} places variant 3, so the
     * property keeps the original's value range.
     */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    /** Original metadata 0. */
    public static final int VARIANT_WARDED = 0;
    /** Original metadata 1 — TileJarBrain, TC-M6-05 (no tile in this slice). */
    public static final int VARIANT_BRAIN = 1;
    /** Original metadata 2 — TileJarNode, TC-M6-03 (no tile in this slice). */
    public static final int VARIANT_NODE = 2;
    /** Original metadata 3 — the void jar. */
    public static final int VARIANT_VOID = 3;

    /** The original's {@code setBlockBoundsBasedOnState}: 3/16 in, 12/16 high. */
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);

    public BlockJar() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .noOcclusion()
                .strength(0.3F, 0.66F)
                .sound(net.minecraft.world.level.block.SoundType.GLASS));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return switch (state.getValue(VARIANT)) {
            case VARIANT_WARDED -> CatalogEssentiaStorage.createJar(pos, state);
            case VARIANT_VOID -> CatalogEssentiaStorage.createJarVoid(pos, state);
            // The brain jar (TC-M6-05) and the node jar (TC-M6-03) have no
            // tile in this slice: a state placed by an old save keeps its data
            // in the world and drops its item, with no behaviour.
            default -> null;
        };
    }

    /**
     * Original {@code getDrops} for metadata 0/3: a jar with essentia and/or a
     * label drops the filled-jar item with the aspect, amount and label
     * attached (and metadata 3 on the item for a void jar); an empty,
     * unlabelled jar drops its own block item.
     */
    @Override
    public java.util.List<ItemStack> getDrops(final BlockState state,
                                              final net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        final int variant = state.getValue(VARIANT);
        if (variant != VARIANT_WARDED && variant != VARIANT_VOID) {
            return super.getDrops(state, builder);
        }
        final BlockEntity be = builder.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof TileJarFillable jar && (jar.amount > 0 || jar.aspectFilter != null)) {
            return java.util.List.of(ItemJarFilled.dropFor(jar));
        }
        return super.getDrops(state, builder);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof BlockDeviceBase.TickingDeviceTile tile) {
                tile.serverTick();
            }
        };
    }

    /**
     * Original {@code onBlockActivated} branches, in the original's order:
     * label removal, dump, label application.
     */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state,
                                              final Level level, final BlockPos pos, final Player player,
                                              final InteractionHand hand, final BlockHitResult hitResult) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TileJarFillable jar)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        final Direction side = hitResult.getDirection();

        // Sneaking on the labelled face with a filter set: pop the label out.
        if (player.isCrouching() && jar.aspectFilter != null && side.get3DDataValue() == jar.facing) {
            if (!level.isClientSide) {
                jar.aspectFilter = null;
                jar.touch();
                final ItemStack label = new ItemStack(
                        com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources.ITEM_RESOURCE.get(), 1);
                label.set(com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META.get(),
                        ItemResource.META_JAR_LABEL);
                drop(level, pos, label, side);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.2F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // A jar label against an unlabelled jar: filter = the jar's aspect (or
        // the label's own carried aspect when the jar is empty), one label
        // consumed, and the original pops the jar's block item out.
        if (player.isCrouching() && jar.aspectFilter == null
                && ItemResource.storedMeta(stack) == ItemResource.META_JAR_LABEL) {
            final var carried = stack.getItem() instanceof
                    com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem container
                    ? container.getAspects(stack) : null;
            if (jar.amount == 0 && (carried == null || carried.size() == 0)) {
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                if (jar.amount == 0 && carried != null && !carried.getAspects().isEmpty()) {
                    jar.aspect = carried.getAspects().get(0);
                }
                stack.shrink(1);
                drop(level, pos, new ItemStack(state.getBlock()), Direction.UP);
                jar.aspectFilter = jar.aspect;
                jar.touch();
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Original's sneak + empty-hand branch of {@code onBlockActivated}: dump
     * the jar's contents, clearing the aspect as well when there is no label.
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hitResult) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TileJarFillable jar)) {
            return InteractionResult.PASS;
        }
        if (player.isCrouching() && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide) {
                jar.amount = 0;
                if (jar.aspectFilter == null) {
                    jar.aspect = null;
                }
                jar.touch();
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.0F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * Original {@code isProvidingWeakPower} (the same figure the client render
     * used): {@code floor(amount / max * 14) + 1} for a non-empty jar. Carried
     * as the 1.21.1 comparator read.
     */
    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        return be instanceof TileJarFillable jar
                ? JarStorageLogic.signalStrength(jar.amount, jar.maxAmount()) : 0;
    }

    private static void drop(final Level level, final BlockPos pos, final ItemStack stack, final Direction side) {
        if (stack.isEmpty() || level.isClientSide) {
            return;
        }
        final double x = pos.getX() + 0.5D + side.getStepX() / 3.0D;
        final double z = pos.getZ() + 0.5D + side.getStepZ() / 3.0D;
        level.addFreshEntity(new ItemEntity(level, x, pos.getY() + 0.5D, z, stack));
    }
}
