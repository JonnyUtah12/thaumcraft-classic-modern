package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.items.ItemResonator;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTube;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeFilter;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeValve;
import com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic;
import com.jonnyutah12.thaumcraftclassic.wands.IWandable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
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
import java.util.ArrayList;
import java.util.List;

/**
 * The essentia tube block (content-mapping.csv {@code thaumcraft:blocktube}) —
 * the whole transport network's host block (TC-M4-07).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockTube}, one block id whose
 * metadata selects the sub-kind and whose {@code createTileEntity(world, meta)}
 * picks the tile. The port keeps the original's metadata 0..7 verbatim on a
 * {@code variant} property (D-001) and picks the tile in
 * {@link #newBlockEntity}:</p>
 * <table>
 *   <caption>blockTube metadata</caption>
 *   <tr><td>0</td><td>Essentia Tube</td><td>{@link TileTube} — ported</td></tr>
 *   <tr><td>1</td><td>Essentia Valve</td><td>{@link TileTubeValve} — ported</td></tr>
 *   <tr><td>2</td><td>Alchemical Centrifuge</td><td>TileCentrifuge — TC-M4-07-sibling</td></tr>
 *   <tr><td>3</td><td>Filtered Essentia Tube</td><td>{@link TileTubeFilter} — ported</td></tr>
 *   <tr><td>4</td><td>Essentia Buffer</td><td>{@link TileTubeBuffer} — ported</td></tr>
 *   <tr><td>5</td><td>Restricted Essentia Tube</td><td>TileTubeRestrict — ported</td></tr>
 *   <tr><td>6</td><td>Directional Essentia Tube</td><td>TileTubeOneway — ported</td></tr>
 *   <tr><td>7</td><td>Essentia Crystallizer</td><td>TileEssentiaCrystalizer — TC-M4-07-sibling</td></tr>
 * </table>
 *
 * <p>Metas 2 and 7 are the two <em>machines</em> the original hosted on this
 * block (the centrifuge and the crystallizer, both essentia-conversion
 * machines, not conduits). They keep their variant in the block so the landed
 * recipes ({@code recipe.190}/{@code 193} centrifuge, {@code recipe.194}
 * crystallizer) and the original ids stay valid, and produce no tile entity
 * until their own card lands — the same treatment
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockJar} gives its
 * brain/node variants.</p>
 *
 * <p>Ported from the block: the material properties (hardness 0.5, resistance
 * 5, metal), the connected pipe outline and collision boxes with the original's
 * own asymmetry (the outline widens y/z to 5/16-11/16 while the collision box
 * uses 6/16-10/16 on every axis, and only a <em>connectable</em> neighbour
 * extends a side), the full-cube body of metas 2/7, the buffer's comparator
 * read, the filtered tube's label handling (sneak to clear and pop the label
 * out, right-click with a jar label to set the filter) and the valve's manual
 * toggle. The original's block destruction drop of the label for a filtered
 * tube rides on {@link #getDrops}.</p>
 *
 * <p>The wand/resonator face toggling is ported here (TC-M4-07-f):
 * {@link #useItemOn} picks up a wand-like click and dispatches it to the tile
 * as {@link IWandable}, which retraces the click into the sub-hits of
 * {@code TileTube.addTraceableCuboids} — flip the aimed face (and the neighbour
 * across it), aim a valve/directional tube on the centre box, cycle a buffer
 * face's choke on a sneak-use. Out of scope, with cards: the original's
 * wand-aware selection outline (its {@code noDoodads} flag, which suppressed
 * the connected outline while a wand was held — 1.21.1 draws the selection
 * shape with an empty {@code CollisionContext}, so there is no held item to
 * test) and the pipe renderer / textures on TC-M7-01, the per-variant item
 * labels on TC-M7-01b (the block's own label is the original's metadata-0 name,
 * "Essentia Tube"), the vent particles and the creak sound on
 * TC-M7-01/TC-M7-05, and the two machine tiles (centrifuge, crystallizer) on
 * <b>TC-M4-07-m</b>.</p>
 */
public class BlockTube extends Block implements EntityBlock, VariantDevice {

    /** The original's eight metadata values (D-001), on one property. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);

    /** Original metadata 0 — {@link TileTube}. */
    public static final int VARIANT_TUBE = 0;
    /** Original metadata 1 — {@link TileTubeValve}. */
    public static final int VARIANT_VALVE = 1;
    /** Original metadata 2 — TileCentrifuge (no tile in this slice). */
    public static final int VARIANT_CENTRIFUGE = 2;
    /** Original metadata 3 — {@link TileTubeFilter}. */
    public static final int VARIANT_FILTER = 3;
    /** Original metadata 4 — {@link TileTubeBuffer}. */
    public static final int VARIANT_BUFFER = 4;
    /** Original metadata 5 — TileTubeRestrict. */
    public static final int VARIANT_RESTRICT = 5;
    /** Original metadata 6 — TileTubeOneway. */
    public static final int VARIANT_ONEWAY = 6;
    /** Original metadata 7 — TileEssentiaCrystalizer (no tile in this slice). */
    public static final int VARIANT_CRYSTALIZER = 7;

    /** Original {@code BlockRenderer.W5}/{@code W11}: the outline's 5/16 and 11/16. */
    private static final double W5 = 0.3125D;
    private static final double W11 = 0.6875D;
    /** Original {@code BlockRenderer.W6}/{@code W10}: the pipe's 6/16 and 10/16. */
    private static final double W6 = 0.375D;
    private static final double W10 = 0.625D;

    public BlockTube() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .noOcclusion()
                .strength(0.5F, 5.0F)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_TUBE));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    /** True for the two machine variants the original hosted here (no tile yet). */
    private static boolean isMachine(final int variant) {
        return variant == VARIANT_CENTRIFUGE || variant == VARIANT_CRYSTALIZER;
    }

    // --- shapes (the original's two, deliberately different, boxes) --------

    /**
     * Original {@code getSelectedBoundingBoxFromPool}: the connected pipe
     * outline for metas 0/1/3/5/6, the 4/16-12/16 column for the centrifuge
     * (meta 2), the full cube for the crystallizer (meta 7), and the
     * 4/16-12/16 cube for the buffer (meta 4).
     *
     * <p>The original also skipped the connected outline while the player held
     * a wand or a resonator (its {@code noDoodads} flag, which went with the
     * wand interaction). The interaction itself is ported (TC-M4-07-f, see
     * {@link #useItemOn}); the flag's outline change stays client-side because
     * 1.21.1 asks for the selection shape with an empty {@code CollisionContext}
     * — there is no held item to test — so the port always draws the connected
     * outline.</p>
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return switch (state.getValue(VARIANT)) {
            case VARIANT_CENTRIFUGE -> Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
            case VARIANT_CRYSTALIZER -> Shapes.block();
            case VARIANT_BUFFER -> Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);
            default -> pipe(level, pos, true);
        };
    }

    /**
     * Original {@code addCollisionBoxesToList}: the connected pipe box (all six
     * axes at W6/W10 — note this is <em>not</em> the outline's box) for metas
     * 0/1/3/4/5/6 and the full cube for metas 2 and 7.
     */
    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return isMachine(state.getValue(VARIANT)) ? Shapes.block() : pipe(level, pos, false);
    }

    /**
     * The original's per-side extension: a side is drawn out to the block edge
     * only when {@link EssentiaTransportHelper#getConnectableTile} finds a
     * neighbour connectable back toward this cell.
     *
     * @param outline the selection outline's box uses W5/W11 on y and z while
     *                the collision box uses W6/W10 on every axis (the original
     *                did exactly this)
     */
    private static VoxelShape pipe(final BlockGetter getter, final BlockPos pos, final boolean outline) {
        double minX = W6;
        double maxX = W10;
        double minY = outline ? W5 : W6;
        double maxY = outline ? W11 : W10;
        double minZ = outline ? W5 : W6;
        double maxZ = outline ? W11 : W10;
        if (getter instanceof Level level) {
            for (final Direction side : Direction.values()) {
                if (EssentiaTransportHelper.getConnectableTile(level, pos, side) == null) {
                    continue;
                }
                switch (side) {
                    case DOWN -> minY = 0.0D;
                    case UP -> maxY = 1.0D;
                    case NORTH -> minZ = 0.0D;
                    case SOUTH -> maxZ = 1.0D;
                    case WEST -> minX = 0.0D;
                    case EAST -> maxX = 1.0D;
                }
            }
        }
        return Block.box(minX * 16.0D, minY * 16.0D, minZ * 16.0D,
                maxX * 16.0D, maxY * 16.0D, maxZ * 16.0D);
    }

    // --- tile wiring -------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return switch (state.getValue(VARIANT)) {
            case VARIANT_TUBE -> CatalogEssentiaTransport.createTube(pos, state);
            case VARIANT_VALVE -> CatalogEssentiaTransport.createValve(pos, state);
            case VARIANT_FILTER -> CatalogEssentiaTransport.createFilter(pos, state);
            case VARIANT_BUFFER -> CatalogEssentiaTransport.createBuffer(pos, state);
            case VARIANT_RESTRICT -> CatalogEssentiaTransport.createRestrict(pos, state);
            case VARIANT_ONEWAY -> CatalogEssentiaTransport.createOneway(pos, state);
            // The centrifuge (meta 2) and the crystallizer (meta 7) are machines,
            // not conduits: TC-M4-07-m. A state placed by an old save keeps its
            // data in the world and drops its item, with no behaviour.
            default -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide) {
            // The original's client tick only drove the vent particles
            // (TC-M7-01); the transport pass is server-side.
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof TileTubeBuffer buffer) {
                buffer.serverTick();
            } else if (be instanceof TileTube tube) {
                tube.serverTick();
            }
        };
    }

    @Override
    public ItemStack getCloneItemStack(final net.minecraft.world.level.LevelReader level, final BlockPos pos,
                                       final BlockState state) {
        return VariantStateItems.withVariant(super.getCloneItemStack(level, pos, state), state);
    }

    /**
     * Original {@code breakBlock}: a filtered tube drops its label alongside the
     * block (the block itself comes from the shipped loot table, since 1.21.1
     * has no implicit block self-drop — the {@code blockjar} precedent).
     */
    @Override
    public List<ItemStack> getDrops(final BlockState state,
                                    final net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (state.getValue(VARIANT) == VARIANT_FILTER) {
            final BlockEntity be = builder.getOptionalParameter(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
            if (be instanceof TileTubeFilter filter && filter.aspectFilter != null) {
                drops.add(jarLabel());
            }
        }
        return drops;
    }

    /**
     * Original {@code hasComparatorInputOverride}: true for the whole family,
     * with only the buffer answering above zero.
     */
    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    /** Original {@code getComparatorInputOverride}: the buffer's pool level. */
    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        return be instanceof TileTubeBuffer buffer
                ? TubeTransportLogic.bufferSignalStrength(buffer.aspects.visSize()) : 0;
    }

    // --- interactions (the original's onBlockActivated) --------------------

    /**
     * Original {@code onBlockActivated}, metadata 1 and 3 branches, <em>plus</em>
     * the wand branch (TC-M4-07-f).
     *
     * <p>The wand branch: in the original, {@code ItemWandCasting.onItemUseFirst}
     * asked the block and then the block entity as {@code IWandable} and consumed
     * the click when the answer was {@code >= 0}, so a wand right-click on any
     * tube face reached {@code TileTube.onWandRightClick}. 1.21.1 hands the held
     * stack to {@code useItemOn} for <em>any</em> item, which is where the port
     * picks the click up and dispatches it — the wand item lives on another
     * branch, so the block is the seam, exactly as the tile family's own
     * "the player is working on the pipe" guard already is. It runs ahead of the
     * valve and filter branches: the original's metadata guards excluded a
     * wand-like stack, so a wand toggles a <em>face</em> and never flips a valve
     * or moves a label.</p>
     *
     * <p>The valve branch mirrors the original's guard: a wand, a resonator or
     * the tube's own item in hand means "the player is working on the pipe, not
     * flipping the valve", so the use falls through to the item. The original
     * compared the held item against the block's item; a
     * {@link VariantBlockItem} of this block is the port's equivalent.</p>
     */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hitResult) {
        final int variant = state.getValue(VARIANT);
        if (isWandItem(stack) && level.getBlockEntity(pos) instanceof IWandable wandable) {
            return wandInteraction(wandable, stack, state, level, pos, player, hitResult);
        }
        if (variant == VARIANT_VALVE && isWandLike(stack)) {
            // NOT PASS_TO_DEFAULT_BLOCK_INTERACTION: that would let the
            // engine call useWithoutItem next and flip the valve anyway.
            return ItemInteractionResult.FAIL;
        }
        return switch (variant) {
            case VARIANT_VALVE -> toggleValve(level, pos, player);
            case VARIANT_FILTER -> filterInteraction(stack, state, level, pos, player, hitResult);
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /**
     * The original's {@code ItemWandCasting.onItemUseFirst} dispatch
     * (TC-M4-07-f): hand the click to the {@link IWandable} tile and treat an
     * answer of {@code >= 0} as handled.
     *
     * <p>One deliberate divergence: the wand item returned {@code ret == 1}, so
     * with a tube answering {@code 0} the original let the wand's own
     * right-click run as well — in 1.7.10 that was a side effect of
     * {@code onItemUseFirst}, and in 1.21.1 letting the click continue means
     * firing an equipped focus and spending vis on every face toggle. The port
     * consumes the click instead (the tile's own {@code onWandRightClick}
     * drawing/holding hooks, which the original left empty, stay unwired for
     * the same reason).</p>
     */
    private static ItemInteractionResult wandInteraction(final IWandable wandable, final ItemStack stack,
                                                         final BlockState state, final Level level,
                                                         final BlockPos pos, final Player player,
                                                         final BlockHitResult hitResult) {
        final Direction side = hitResult.getDirection();
        final int md = state.getValue(VARIANT);
        wandable.onWandRightClick(level, stack, player, pos, side, md);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The empty-hand half of the original's {@code onBlockActivated}: both the
     * valve toggle and the filter's label removal are reachable with an empty
     * hand, so they are handled here too (1.21.1 splits the two paths).
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hitResult) {
        final int variant = state.getValue(VARIANT);
        if (variant == VARIANT_VALVE) {
            if (isWandLike(player.getMainHandItem())) {
                return InteractionResult.PASS;
            }
            final ItemInteractionResult result = toggleValve(level, pos, player);
            return result.consumesAction() ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (variant == VARIANT_FILTER
                && player.isCrouching()
                && level.getBlockEntity(pos) instanceof TileTubeFilter filter
                && filter.aspectFilter != null) {
            if (!level.isClientSide) {
                filter.aspectFilter = null;
                filter.touch();
                drop(level, pos, jarLabel(), hitResult.getDirection());
            }
            level.playSound(null, pos, ModSounds.PAGE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * Original metadata-1 branch: flip {@code allowFlow}, sound the squeak, and
     * mark the block for a client update. The original's wand branch (the face
     * toggle) is ported in {@link #useItemOn} (TC-M4-07-f) and runs <em>ahead</em>
     * of this one, so a wand in hand aims the pipe rather than flipping the valve.
     */
    private static ItemInteractionResult toggleValve(final Level level, final BlockPos pos, final Player player) {
        if (level.getBlockEntity(pos) instanceof TileTubeValve valve) {
            if (!level.isClientSide) {
                valve.allowFlow = !valve.allowFlow;
                valve.touch();
            }
            level.playSound(null, pos, ModSounds.SQUEEK.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                    0.7F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            player.swing(InteractionHand.MAIN_HAND);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Original metadata-3 branch, in the original's order: sneaking on a
     * labelled tube clears the filter and pops the label out; a jar label held
     * against an unfiltered tube sets the filter from the label's own aspect and
     * consumes it.
     */
    private static ItemInteractionResult filterInteraction(final ItemStack stack, final BlockState state,
                                                           final Level level, final BlockPos pos,
                                                           final Player player, final BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof TileTubeFilter filter)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isCrouching() && filter.aspectFilter != null) {
            if (!level.isClientSide) {
                filter.aspectFilter = null;
                filter.touch();
                drop(level, pos, jarLabel(), hitResult.getDirection());
            }
            level.playSound(null, pos, ModSounds.PAGE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (filter.aspectFilter == null && ItemResource.storedMeta(stack) == ItemResource.META_JAR_LABEL) {
            final var carried = stack.getItem() instanceof IEssentiaContainerItem container
                    ? container.getAspects(stack) : null;
            if (carried == null || carried.getAspects().isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                final Aspect filterAspect = carried.getAspects().get(0);
                filter.aspectFilter = filterAspect;
                filter.touch();
                stack.shrink(1);
            }
            level.playSound(null, pos, ModSounds.PAGE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** The original's "the player is working on the pipe" guard for the valve. */
    private static boolean isWandLike(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // The original tested instanceof ItemWandCasting; the port's seam for
        // "this stack holds vis, i.e. it is a wand" is the TC-M4-02 VIS_POOL
        // component (the same test TileArcaneWorkbench uses), because the wand
        // item itself lands with its own card.
        return isWandItem(stack)
                || stack.getItem() instanceof VariantBlockItem item && item.getBlock() instanceof BlockTube;
    }

    /**
     * The stacks that carry the original's wand dispatch (TC-M4-07-f):
     * {@code ItemWandCasting} and {@code ItemResonator} are the two items whose
     * {@code onItemUseFirst} asked the tile as {@code IWandable}. The tube's own
     * item is deliberately <em>not</em> here — it only ever appeared in
     * {@code BlockTube.onBlockActivated}'s "working on the pipe" guard, and in
     * 1.7.10 it never reached a tile's {@code onWandRightClick}.
     */
    private static boolean isWandItem(final ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getOrDefault(ModDataComponents.VIS_POOL.get(), null) != null
                || stack.getItem() instanceof ItemResonator);
    }

    /** The original's {@code new ItemStack(ConfigItems.itemResource, 1, 13)}. */
    private static ItemStack jarLabel() {
        final ItemStack label = new ItemStack(CatalogResources.ITEM_RESOURCE.get(), 1);
        label.set(ModDataComponents.ITEM_META.get(), ItemResource.META_JAR_LABEL);
        return label;
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
