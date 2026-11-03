package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The infernal furnace block (content-mapping.csv
 * {@code thaumcraft:blockarcanefurnace}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockArcaneFurnace}, whose
 * metadata carried the whole 3×3×3 machine: 0 = the base plate, 1-9 = the
 * shell, 10 = the nozzle. All eleven are registered (TC-M4-06-m, t_5eb27d02)
 * as the block's one {@code variant} property — the original had one block id
 * with eleven metadata, and D-001 maps one legacy id to one modern id, so a
 * second block id would be an id the reference does not have:</p>
 * <ul>
 *   <li>{@code variant} 0 = metadata 0, the base plate
 *       ({@link TileArcaneFurnace});</li>
 *   <li>{@code variant} 1 = metadata 10, the nozzle
 *       ({@link com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle});</li>
 *   <li>{@code variant} {@code 2..10} = metadata {@code 1..9}, the nine shell
 *       grid positions, in the order the original's builder assigns them
 *       ({@code WandManager.replaceArcaneFurnace} {@code :454-480} writes
 *       {@code md = step} with {@code step = xx + 3*zz + 1}, {@code xx} fastest
 *       — the same grid the face machine reads back with
 *       {@code (meta-1)%3 + (meta-1)/3*9}).</li>
 * </ul>
 *
 * <p>The grid position is <b>published</b>, not placed: a placed part is the
 * plate, and {@link ArcaneFurnaceStructure} writes the grid into the state from
 * the plate-centred anchor (D-020's publish shape — the original's grid came
 * from the wand builder rewriting every cell's metadata, and the port has no
 * builder). The tile map is the original's: the plate tile for metadata 0, the
 * nozzle tile for the metas the original's {@code createTileEntity}
 * ({@code :355-362}) names — 2, 4, 5, 6, 8 — and no tile at all for the corner
 * metas 1, 3, 7, 9, which is why those publish from the placement and the
 * neighbour hook.</p>
 *
 * <p>Properties are the original's: metal-class material, hardness 10,
 * resistance 500, light 13 for the base and the nozzle only
 * ({@code getLightValue} {@code :179-182} answers super — 0 — for the shell),
 * solid on every face except the plate ({@code isSideSolid} {@code :330-332}).
 * Shapes are the original's bounds: the base plate is a quarter block
 * ({@code :188-191}), the nozzle is a half block against the base it faces
 * ({@code :193-213}), and every shell cell is the full cube
 * ({@code :207-209}). Behavior ported: the base's item suction and burning
 * ({@link TileArcaneFurnace#deviceEntityInside}), and the redstone the
 * original leaked — the original returned weak power on the UP face only,
 * which rides here as a signal of 1 into the block above.</p>
 *
 * <p>Recorded deltas (BD-18 / TC-M4-06-m): the structure check with its
 * {@code restoreBlocks} rollback ({@code :250-264}, called from the plate's
 * block-change hook {@code :267-287}) and the bound blaze
 * ({@code :289-301}) are follow-up cards, so a part that goes missing leaves
 * the shell standing with its last grid position; the original's
 * {@code randomDisplayTick} smoke (client, {@code :343-355}) is TC-M7; the
 * essentia feed stays on the registered nozzle (see
 * {@link com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle}).</p>
 */
public class BlockArcaneFurnace extends Block implements EntityBlock, DeviceDynamicState {

    /**
     * 0 = base plate (original metadata 0), 1 = nozzle (metadata 10),
     * {@code 2..10} = the shell grid positions (metadata {@code 1..9}, i.e.
     * {@code variant = metadata + 1}). One property carries the whole machine
     * because the original was <em>one</em> block id with eleven metadata
     * (D-001).
     */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 10);
    public static final int VARIANT_BASE = 0;
    public static final int VARIANT_NOZZLE = 1;
    /** The first shell variant: metadata 1, the grid's first scan cell. */
    public static final int VARIANT_SHELL_FIRST = 2;
    /** The last shell variant: metadata 9, the grid's last scan cell. */
    public static final int VARIANT_SHELL_LAST = 10;

    /**
     * The vertical-run level the original read out of the blocks above and
     * below: {@code calculateLevel} ({@code BlockArcaneFurnace.java:158-177})
     * answers 9 for the mid of a run, 18 for the top of one and 0 standalone.
     * Published as a state (D-020) because for the metadata-10 nozzle that
     * value picks the side face — {@code level != 9 ? icon[7] : icon[6]}
     * ({@code :104-155}) — and 1.21.1 has no per-frame render hook to recompute
     * it in.
     */
    public static final IntegerProperty STACK = IntegerProperty.create("stack", 0, 2);
    /** The original's 0 (standalone). */
    public static final int STACK_STANDALONE = 0;
    /** The original's 18 (top of a vertical run). */
    public static final int STACK_TOP = 1;
    /** The original's 9 (mid of a vertical run). */
    public static final int STACK_MID = 2;

    /**
     * Which side the machine's other half touches this block: the {@code add}
     * gate of {@code calculateTexture} — {@code add = 3} when
     * {@code BlockUtils.isBlockTouchingOnSide(world, x, y, z, this, 10, side)}
     * finds a metadata-10 nozzle on that face ({@code :61-63},
     * {@code BlockUtils.java:342-379}). The value is the
     * {@link net.minecraft.core.Direction} ordinal; {@link #ATTACHED_NONE} is
     * the original's {@code add == 0}. The ISMR draws its three art quads on
     * the side of the base plate it finds in its west / east / north / else-+Z
     * branch ({@code BlockArcaneFurnaceRenderer.java:26-56}), so this value
     * picks the model geometry.
     */
    public static final IntegerProperty ATTACHED = IntegerProperty.create("attached", 0, 6);
    /** No partner half touching — the original's {@code add == 0}. */
    public static final int ATTACHED_NONE = 6;

    /** Original base bounds: 0.0 → 0.25. */
    private static final VoxelShape BASE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    private static final VoxelShape NOZZLE_WEST = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
    private static final VoxelShape NOZZLE_EAST = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NOZZLE_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    private static final VoxelShape NOZZLE_SOUTH = Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);

    public BlockArcaneFurnace() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .noOcclusion()
                .strength(10.0F, 500.0F)
                // getLightValue (BlockArcaneFurnace.java:179-182): 13 for the
                // base plate and the nozzle; every shell meta answers super,
                // i.e. 0. The nether-brick corners and obsidian edges of the
                // shell are dark — the glow is the lava plate and the nozzle.
                .lightLevel(state -> isGlowing(state) ? 13 : 0));
        // stateDefinition.any() takes the FIRST value of every property, and
        // for an IntegerProperty that is its minimum — STACK_STANDALONE and
        // ATTACHED_DOWN, i.e. a fresh furnace would default to a lit face
        // machine. Name the resting values explicitly (D-020; the same trap bit
        // the parent card's BooleanProperty).
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, VARIANT_BASE)
                .setValue(STACK, STACK_STANDALONE)
                .setValue(ATTACHED, ATTACHED_NONE));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, STACK, ATTACHED);
    }

    /**
     * The original's vertical-run test, transcribed from
     * {@code calculateLevel} ({@code BlockArcaneFurnace.java:158-177}): 9 for
     * the mid of a vertical run of same-metadata parts, 18 for the top of a run
     * (the part above matches and the part below does not), 0 standalone.
     *
     * <p>Both quirks of the original are kept deliberately. It reads the
     * neighbour's metadata and normalises 0 and 10 — the two metadata a furnace
     * part carries — to its own, so a <em>non</em>-furnace neighbour (air reads
     * 0) always passes the metadata test and only the same-block test keeps it
     * out; and it normalises a plate (metadata 0) above a nozzle (metadata 10)
     * to the nozzle's own metadata, so a plate over a nozzle counts as the same
     * run. 1.21.1 has no block metadata, so a non-furnace neighbour contributes
     * 0 here, matching {@code world.getBlockMetadata} on air.</p>
     */
    public static int calculateLevel(final BlockGetter level, final BlockPos pos, final BlockState state) {
        final int meta = originalMetaOf(state);
        final BlockState above = level.getBlockState(pos.above());
        final BlockState below = level.getBlockState(pos.below());
        final int metaAbove = normaliseMeta(originalMetaOf(above), meta);
        final int metaBelow = normaliseMeta(originalMetaOf(below), meta);
        final boolean sameAbove = isFurnacePart(above);
        final boolean sameBelow = isFurnacePart(below);
        if (meta == metaAbove && meta == metaBelow && sameAbove && sameBelow) {
            return 9;
        }
        return meta != metaAbove || !sameAbove || meta == metaBelow && sameBelow ? 0 : 18;
    }

    /** {@code calculateLevel}'s answer as the published {@link #STACK} value. */
    public static int stackFor(final int level) {
        return switch (level) {
            case 9 -> STACK_MID;
            case 18 -> STACK_TOP;
            default -> STACK_STANDALONE;
        };
    }

    /**
     * The original's metadata for a shipped variant: 0 → 0, 1 → 10, and
     * {@code 2..10} → {@code 1..9} (the shell grid positions).
     */
    public static int originalMetaOf(final BlockState state) {
        if (!isFurnacePart(state)) {
            return 0;
        }
        final int variant = state.getValue(VARIANT);
        if (variant == VARIANT_NOZZLE) {
            return 10;
        }
        return variant >= VARIANT_SHELL_FIRST ? variant - 1 : VARIANT_BASE;
    }

    /** The shell variant carrying a grid position (the original's metadata 1-9). */
    public static int variantForShellGrid(final int grid) {
        return grid + 1;
    }

    /** The grid position (the original's metadata 1-9) a shell variant carries. */
    public static int shellGridOf(final BlockState state) {
        return isShell(state) ? state.getValue(VARIANT) - 1 : ArcaneFurnaceStructure.GRID_NONE;
    }

    /** True for the nine shell cells — the 3×3 grid around the plate. */
    public static boolean isShell(final BlockState state) {
        return isFurnacePart(state) && state.getValue(VARIANT) >= VARIANT_SHELL_FIRST;
    }

    /** The two metas the original lights ({@code :179-182}). */
    public static boolean isGlowing(final BlockState state) {
        return isFurnacePart(state) && state.getValue(VARIANT) <= VARIANT_NOZZLE;
    }

    /**
     * The metas the original's {@code createTileEntity} ({@code :355-362})
     * hands the <em>nozzle</em> tile: 2, 4, 5, 6 and 8 — the four edge-middle
     * columns plus the centre column. The corner metas 1, 3, 7 and 9 fall
     * through to {@code super.createTileEntity}, i.e. no tile at all.
     */
    public static boolean isNozzleTileMeta(final BlockState state) {
        if (!isShell(state)) {
            return false;
        }
        final int meta = originalMetaOf(state);
        return meta == 2 || meta == 4 || meta == 5 || meta == 6 || meta == 8;
    }

    private static int normaliseMeta(final int meta, final int own) {
        return meta == 0 || meta == 10 ? own : meta;
    }

    public static boolean isFurnacePart(final BlockState state) {
        return state.is(CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get());
    }

    /** True when {@code state} is the base plate (the machine's centre). */
    public static boolean isPlate(final BlockState state) {
        return isFurnacePart(state) && state.getValue(VARIANT) == VARIANT_BASE;
    }

    /** True when {@code state} is the nozzle (metadata 10, the {@code add} gate's block). */
    public static boolean isNozzle(final BlockState state) {
        return isFurnacePart(state) && state.getValue(VARIANT) == VARIANT_NOZZLE;
    }

    /**
     * The published dynamic-appearance property: the run level, which is the
     * face machine's input that changes the <em>sprite</em> a model names (the
     * nozzle's sides, {@code BlockArcaneFurnace.java:104-155}). {@code attached}
     * changes geometry only — the three art quads move to the side of the plate
     * the ISMR found — so it is gated by the face-table probe
     * ({@code BlockStateCoverageGameTest#arcanefurnaceFaceshiptheoriginalsfacemachine}),
     * which reads the quad placement back out of the shipped JSON, not by the
     * sprite-set rule here.
     */
    @Override
    public Property<?> dynamicAppearanceProperty() {
        return STACK;
    }

    /**
     * Original {@code setBlockBoundsBasedOnState} /
     * {@code addCollisionBoxesToList} ({@code :193-213}): the nozzle is a half
     * block on the side of the base it is attached to (its metadata 10 is
     * paired with a metadata 0 neighbour), the base is the quarter-height
     * plate ({@code :188-191}), and every other meta — the nine shell cells —
     * is the full cube ({@code :207-209}).
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (state.getValue(VARIANT) == VARIANT_BASE) {
            return BASE_SHAPE;
        }
        if (state.getValue(VARIANT) == VARIANT_NOZZLE) {
            if (isBaseAt(level, pos.west())) {
                return NOZZLE_WEST;
            }
            if (isBaseAt(level, pos.east())) {
                return NOZZLE_EAST;
            }
            if (isBaseAt(level, pos.north())) {
                return NOZZLE_NORTH;
            }
            if (isBaseAt(level, pos.south())) {
                return NOZZLE_SOUTH;
            }
        }
        return FULL_SHAPE;
    }

    private static final VoxelShape FULL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static boolean isBaseAt(final BlockGetter level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.is(CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get())
                && state.getValue(VARIANT) == VARIANT_BASE;
    }

    /**
     * Original {@code isSideSolid} ({@code :330-332}): every meta except the
     * base plate is solid — the shell is a full cube you can stand things on.
     * 1.21.1 answers sturdiness from the block support shape
     * ({@code SupportType.FULL} → {@code Block.isFaceFull}), so the rule rides
     * there: the plate keeps its quarter-block support, everything else is the
     * full cube.
     */
    @Override
    protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level,
                                              final BlockPos pos) {
        return originalMetaOf(state) == 0 ? BASE_SHAPE : FULL_SHAPE;
    }

    /**
     * The tile class the original's {@code createTileEntity} ({@code :355-362})
     * names for a state: the nozzle tile for metadata 10 and for the shell metas
     * 2, 4, 5, 6 and 8, nothing for the corner metas 1, 3, 7 and 9 (the
     * blueprint's nether brick), and the furnace tile for the base plate.
     */
    public enum TileKind {
        /** No tile at all — the shell's corner metas. */
        NONE,
        /** {@code TileArcaneFurnaceNozzle}, the original's metas 10, 2, 4, 5, 6, 8. */
        NOZZLE,
        /** {@code TileArcaneFurnace}, the original's metadata 0. */
        PLATE
    }

    public static TileKind tileFor(final BlockState state) {
        if (!isFurnacePart(state)) {
            return TileKind.NONE;
        }
        if (isNozzle(state) || isNozzleTileMeta(state)) {
            return TileKind.NOZZLE;
        }
        return isShell(state) ? TileKind.NONE : TileKind.PLATE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return switch (tileFor(state)) {
            case NOZZLE -> CatalogEssentiaStorage.createFurnaceNozzle(pos, state);
            case PLATE -> CatalogEssentiaStorage.createFurnace(pos, state);
            case NONE -> null;
        };
    }

    /**
     * The grid publish (TC-M4-06-m): a placed part is the plate, and the
     * shell's grid position comes from the plate-centred anchor. The original
     * got its grid from its builder, which rewrote every cell's metadata
     * ({@code WandManager.replaceArcaneFurnace}, {@code :454-480}); the port
     * has no builder, so the state carries the grid and this hook writes it.
     *
     * <p>When the placed part <em>is</em> the anchor, the 26 cells around it
     * are re-published too — the original re-marked that same 3×3×3 for a
     * render update from the plate's neighbour hook
     * ({@code :304-318}, {@code markBlockRangeForRenderUpdate} for every
     * {@code xx,yy,zz} of the 3×3×3) because its faces were recomputed in the
     * render hook; 1.21.1 has no render hook, so the re-mark becomes the state
     * write.</p>
     */
    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos,
                           final BlockState oldState, final boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        ArcaneFurnaceStructure.publish(level, pos);
        if (ArcaneFurnaceStructure.isAnchor(level, pos)) {
            ArcaneFurnaceStructure.publishMachine(level, pos);
        }
    }

    /**
     * The neighbour hook the original used to re-mark the machine
     * ({@code :304-318}). The metas that carry a tile re-publish on their own
     * tick; the corner metas have no tile in the original
     * ({@code createTileEntity}, {@code :355-362}), so this hook is their only
     * path to a face that follows the neighbours.
     */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighborBlock, final BlockPos neighborPos,
                                   final boolean movedByPiston) {
        if (level.isClientSide || !isShell(state) || isNozzleTileMeta(state)) {
            return;
        }
        ArcaneFurnaceStructure.publish(level, pos);
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

    /** Original {@code onEntityCollidedWithBlock} (metadata 0 only). */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (state.getValue(VARIANT) != VARIANT_BASE || level.isClientSide) {
            return;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileArcaneFurnace furnace) {
            furnace.deviceEntityInside(entity);
        }
    }

    /**
     * Original {@code isProvidingWeakPower}: true on the UP face only — a
     * signal of 1 into the block above the furnace.
     */
    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return direction == Direction.UP ? 1 : 0;
    }
}
