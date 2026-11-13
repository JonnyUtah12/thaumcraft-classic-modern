package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolid;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritchNothing;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCrabSpawner;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The shared toolbox of the Outer Lands maze generators — the port of the
 * original {@code thaumcraft.common.lib.world.dim.GenCommon}, reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/world/dim/GenCommon.java}
 * (415 lines), cited below as "ref". {@code Gen2x2}, {@code GenPortal},
 * {@code GenBossRoom} and {@code GenPassage} all extend it exactly as the
 * original generation classes extend the original GenCommon.
 *
 * <p><b>Numbers verbatim.</b> The b-codes {@code BEDROCK = 1} … {@code CRUST = 21}
 * and {@code BEDROCK_REPL = 99} (ref L20-34), the 11x11 {@link #PAT_CONNECT}
 * matrix (ref L35-47), the whole {@code placeBlock} code table (ref L55-259:
 * every {@code nextInt}/{@code nextFloat}/{@code nextBoolean} draw, its bound,
 * its branch and the {@code break label208} exits that skip the crust tail),
 * the obelisk stack (ref L261-267), the three decoration queues and their
 * {@code processDecorations} drain (ref L17-19, L269-323: the
 * {@code nextFloat()} urn-rarity roll and its 0.025F/0.1F cut-points, the
 * {@code nextInt(3)}/{@code nextInt(8)} eldritch-meta roll, the
 * {@code nextInt(12)} crystal roll), {@code isBedrockShowing} (ref L325-338),
 * the four-way {@code generateConnections} with its three-nested-conditional
 * loop bounds (ref L340-414) and the placement flag table
 * {@code block != eldritchNothing && block != bedrock && block != air ? 3 : 0}
 * (ref L254-257) are all carried unchanged.</p>
 *
 * <h2>1.21.1 adaptation</h2>
 * <ul>
 *   <li><b>No block metadata.</b> A 1.21.1 block state has no metadata int.
 *   The original metas ride on the shipped blocks' {@code variant}
 *   {@code IntegerProperty}, whose domain is the original meta domain
 *   (BlockCosmeticSolid 0-15 — metas 11-15 are the ancient-stone family used
 *   by the maze, BlockEldritch 0-10, BlockAiry 0-12, BlockCrystal 0-7, the
 *   stone slab 0-1, blockEldritchNothing 0-1); see
 *   {@link #stateFor(Block, int)}. Metas outside a property domain (the chest
 *   loot index 66, the urn rarity 0-2, the spawner type) have no block-state
 *   consumer: they are carried verbatim on the {@link Decoration} record / the
 *   maze queues and their consumer is the tile card that reads them (the
 *   shipped item-side analogue is the {@code item_meta} data component,
 *   {@code registration/ModDataComponents}).</li>
 *   <li><b>No block-entity factory casts.</b> {@code world.getTileEntity}
 *   casts become typed {@code level.getBlockEntity(pos)} pattern-matches
 *   ({@code TileCrystal} — whose {@code orientation} short (ForgeDirection
 *   ordinal) becomes the shipped {@code TileCrystal.setOrientation(Direction)},
 *   {@code TileEldritchCrabSpawner} — whose facing byte keeps the original
 *   ForgeDirection ordinal bytes via {@link #forgeOrdinal(Direction)}, the
 *   shipped wire format, see {@code GenBossRoom}).</li>
 *   <li><b>World random.</b> {@code world.field_73012_v} (ref L68, 73, 74, 87,
 *   240, 243, 246) is {@code Level.random} — the maze path resolves the
 *   {@code LevelWriter} to the {@code ServerLevel} once per entry and draws
 *   from the level's own random, as the original did.</li>
 *   <li><b>ForgeDirection ordinals.</b> The codes 3-6 switch on
 *   {@code dir.ordinal()} in the 1.7.10 encoding (UNKNOWN 0, DOWN 1, UP 2,
 *   NORTH 3, SOUTH 4, WEST 5, EAST 6) while 1.21.1 {@code Direction} orders
 *   DOWN 0, UP 1, NORTH 2, SOUTH 3, WEST 4, EAST 5; the codes that read the
 *   direction by <i>name</i> (10, 11) are unaffected. All ordinal reads and the
 *   crab facing byte go through {@link #forgeOrdinal(Direction)} so the
 *   original stair metas (ref L92-145) and facing bytes (ref L312) survive.
 *   An unknown ordinal falls to the original {@code default: break label208}
 *   branch — meta stays 0 — verbatim.</li>
 *   <li><b>Stairs have no metas.</b> The eldritch stair metas 0-7 of the
 *   table (ref L91-145) map to the shipped {@code StairBlock} state: meta&3 →
 *   FACING (0 east, 1 west, 2 south, 3 north — the original
 *   {@code BlockStairs.blockInside} orientation table) and meta&4 →
 *   {@code HalfBlock.TOP} (the original's upside-down bit).</li>
 *   <li><b>Block-item placement.</b> {@code BlockItem.setBlockViaItem} and
 *   {@code ItemActionContext} are 1.21.2+ API and do not exist on
 *   Minecraft 1.21.1 (the shipped code uses neither), so the task's
 *   setBlockViaItem path falls back, for every block, to the documented
 *   direct {@code level.setBlock} with the update flags the original passes
 *   ({@code flag ? 3 : 2}, ref: the 4-arg {@code setBlock} calls pass 3).</li>
 *   <li><b>Queues.</b> The original's three {@code ArrayList<ChunkCoordinates>}
 *   (ref L17-19) become {@code List<BlockPos>} and drain in the original
 *   order (urns, common deco, crab spawners, then {@code clear()}),
 *   {@code processDecorations} being the shipped {@code MazeHandler} flush.
 *   The toolbox decoration queue of the 1.9-generation callers
 *   ({@code decoratedBlocks = List<Object[]> (world, pos, block, meta, type)})
 *   becomes {@link Decoration} + {@link #DECORATED_BLOCKS}, drained after the
 *   maze queues, entries removed as dispatched.</li>
 *   <li><b>Class is not final.</b> {@code Gen2x2}/{@code GenPortal}/
 *   {@code GenBossRoom}/{@code GenPassage} extend it as the originals do, so
 *   no private constructor / final class is possible here.</li>
 * </ul>
 */
public class GenCommon {

    /**
     * One queued toolbox decoration — the original's
     * {@code decoratedBlocks} {@code Object[]}
     * {@code (World world, BlockPos pos, Block block, int meta, int type)}.
     */
    public record Decoration(ServerLevel level, BlockPos pos, Block block, int meta, int type) {
    }

    /** The toolbox decoration queue; {@link #processDecorations(ServerLevel)} drains it. */
    public static final List<Decoration> DECORATED_BLOCKS = new CopyOnWriteArrayList<>();

    /**
     * The original's {@code decoCommon} queue (ref L17): wall cells that may
     * grow crusted/glyphed/trap stone (and a crystal) in
     * {@link #processDecorations}.
     */
    public static final List<BlockPos> decoCommon = new CopyOnWriteArrayList<>();

    /** The original's {@code crabSpawner} queue (ref L18). */
    public static final List<BlockPos> crabSpawner = new CopyOnWriteArrayList<>();

    /** The original's {@code decoUrn} queue (ref L19). */
    public static final List<BlockPos> decoUrn = new CopyOnWriteArrayList<>();

    /** The original's {@code BEDROCK} code (ref L20). */
    static final int BEDROCK = 1;
    /** The original's {@code BEDROCK_REPL} code (ref L21). */
    static final int BEDROCK_REPL = 99;
    /** The original's {@code STONE} code (ref L22). */
    static final int STONE = 2;
    /** The original's {@code VOID} code (ref L23). */
    static final int VOID = 8;
    /** The original's {@code AIR_REPL} code (ref L24). */
    static final int AIR_REPL = 9;
    /** The original's {@code STAIR_DIRECTIONAL} code (ref L25). */
    static final int STAIR_DIRECTIONAL = 10;
    /** The original's {@code STAIR_DIRECTIONAL_INV} code (ref L26). */
    static final int STAIR_DIRECTIONAL_INV = 11;
    /** The original's {@code SLAB} code (ref L27; no {@code placeBlock} case for it, ref L61-235). */
    static final int SLAB = 12;
    /** The original's {@code DOOR_BLOCK} code (ref L28). */
    static final int DOOR_BLOCK = 15;
    /** The original's {@code DOOR_LOCK} code (ref L29). */
    static final int DOOR_LOCK = 16;
    /** The original's {@code VOID_DOOR} code (ref L30). */
    static final int VOID_DOOR = 17;
    /** The original's {@code ROCK} code (ref L31). */
    static final int ROCK = 18;
    /** The original's {@code STONE_NOSPAWN} code (ref L32). */
    static final int STONE_NOSPAWN = 19;
    /** The original's {@code STONE_TRAPPED} code (ref L33). */
    static final int STONE_TRAPPED = 20;
    /** The original's {@code CRUST} code (ref L34) — its effect is the tail at ref L237-251. */
    static final int CRUST = 21;

    /** The original's 11x11 wall pattern (ref L35-47), verbatim. */
    public static final int[][] PAT_CONNECT = new int[][]{
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
            {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
            {1, 8, 2, 5, 9, 9, 9, 6, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
            {1, 8, 2, 3, 9, 9, 9, 4, 2, 8, 1},
            {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
            {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}
    };

    /** Decoration type 0 — tree/log (the 1.9-generation toolbox switch). */
    public static final int DECORATION_TREE = 0;
    /** Decoration type 1 — eldritch spawner (the 1.9-generation toolbox switch). */
    public static final int DECORATION_SPAWNER = 1;
    /** Decoration type 2 — stairs. */
    public static final int DECORATION_STAIRS = 2;
    /** Decoration type 3 — loot urn. */
    public static final int DECORATION_LOOT = 3;
    /** Decoration type 4 — mob spawner. */
    public static final int DECORATION_MOB_SPAWNER = 4;
    /** Decoration type 5 — pillar. */
    public static final int DECORATION_PILLAR = 5;
    /** Decoration type 6 — chest. */
    public static final int DECORATION_CHEST = 6;
    /** Decoration type 7 — gold pan. */
    public static final int DECORATION_GOLDPAN = 7;
    /** Decoration type 8 — arcane. */
    public static final int DECORATION_ARCANE = 8;
    /** Decoration type 9 — taint foliage. */
    public static final int DECORATION_TAINT = 9;
    /** Decoration type 10 — fire. */
    public static final int DECORATION_FIRE = 10;
    /** Decoration type 11 — crystal. */
    public static final int DECORATION_CRYSTAL = 11;

    /**
     * The original's eldritch spawner entity id (the 1.9-generation spawner
     * types, e.g. {@code addSpawner}). These {@code EntityEldritch*} ids are
     * NOT registered by the shipped repo (TC-M6-06 owns the eldritch mobs),
     * so they resolve to nothing and the spawner falls back to the original's
     * vanilla {@code "Silverfish"} id ({@link #FALLBACK_SPAWNER_ID}) — inert
     * until TC-M6-06.
     */
    public static final String SPAWNER_ID_ELDRITCH_GUARDIAN = "EntityEldritchGuardian";

    /** The original's vanilla spawner fallback id (the generic spawner case). */
    public static final String FALLBACK_SPAWNER_ID = "Silverfish";

    /**
     * The chest loot id the original's {@code TileThaumcraftChest.setLoot(66)}
     * names — {@code "ThaumVault"}, kept verbatim. No shipped loot table with
     * this id exists yet and no {@code TileThaumcraftChest} is shipped, so the
     * loot reference is inert until its card lands; the placement itself is
     * the vanilla chest + house loot-table pattern
     * ({@code worldgen/StructureGen.fillDungeonChest}).
     */
    public static final String CHEST_LOOT_THAUM_VAULT = "ThaumVault";

    /** The golden-head item id of the gold-pan decoration; not registered yet (no shipped item row). */
    public static final String GOLDEN_HEAD_ID = "thaumcraft:goldenhead";

    // ------------------------------------------------------------------ state mapping

    /**
     * The meta → BlockState mapping (see the class javadoc): the shipped
     * blocks' {@code variant} {@code IntegerProperty} carries the original
     * meta verbatim when the meta is inside the property domain; the eldritch
     * stair metas 0-7 map onto {@code StairBlock} FACING/HALF; every other
     * meta yields the block's default state (the meta rides on the
     * decoration/queue for its tile consumer — class (c) in the javadoc).
     *
     * @param block the block to state
     * @param meta  the original 1.7.10 metadata value
     * @return the state to set
     */
    public static BlockState stateFor(final Block block, final int meta) {
        final BlockState base = block.defaultBlockState();
        if (block instanceof BlockDeviceBase device) {
            final IntegerProperty property = device.variantProperty();
            if (property.getPossibleValues().contains(meta)) {
                return device.withVariant(meta);
            }
            return base;
        }
        if (block instanceof BlockCosmeticSolid) {
            return setVariant(base, BlockCosmeticSolid.VARIANT, meta);
        }
        if (block instanceof BlockEldritch) {
            return setVariant(base, BlockEldritch.VARIANT, meta);
        }
        if (block instanceof BlockCrystal) {
            return setVariant(base, BlockCrystal.VARIANT, meta);
        }
        if (block instanceof BlockCosmeticStoneSlab) {
            return setVariant(base, BlockCosmeticStoneSlab.VARIANT, meta);
        }
        if (block instanceof BlockEldritchNothing) {
            return setVariant(base, BlockEldritchNothing.VARIANT, meta);
        }
        if (base.hasProperty(StairBlock.FACING)) {
            return stairState(block, meta);
        }
        return base;
    }

    /** {@code setValue} guarded against the property domain (out-of-domain metas fall to default). */
    private static BlockState setVariant(final BlockState base, final IntegerProperty property, final int value) {
        if (!property.getPossibleValues().contains(value)) {
            return base;
        }
        return base.setValue(property, value);
    }

    /**
     * The original's stair meta 0-7 → the shipped {@code StairBlock} state:
     * meta&3 → FACING (0 east, 1 west, 2 south, 3 north) and meta&4 →
     * {@code HalfBlock.TOP} (the original's upside-down bit).
     */
    public static BlockState stairState(final Block block, final int meta) {
        final Direction facing = switch (meta & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.WEST;
            case 2 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
        final BlockState base = block.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        if (base.hasProperty(StairBlock.WATERLOGGED)) {
            return base.setValue(StairBlock.WATERLOGGED, false);
        }
        return base;
    }

    /**
     * The 1.7.10 ForgeDirection ordinal of a 1.21.1 {@code Direction}
     * (UNKNOWN 0, DOWN 1, UP 2, NORTH 3, SOUTH 4, WEST 5, EAST 6) — the
     * encoding the original's stair codes switch on (ref L92-145) and the
     * crab-spawner facing byte (ref L312).
     */
    public static int forgeOrdinal(final Direction direction) {
        return switch (direction) {
            case DOWN -> 1;
            case UP -> 2;
            case NORTH -> 3;
            case SOUTH -> 4;
            case WEST -> 5;
            case EAST -> 6;
        };
    }

    // ------------------------------------------------------------------ block setting

    /**
     * The original's {@code setBlock(x, y, z, block, meta, flags)} toolbox
     * entry. The task's setBlockViaItem route needs
     * {@code BlockItem.setBlockViaItem}/{@code ItemActionContext}, which are
     * 1.21.2+ API absent on Minecraft 1.21.1 (no shipped code references them),
     * so the documented fallback — a direct {@code level.setBlock} of the
     * meta-mapped state — is used for every block. The {@code flag} is the
     * original's notify flag: update flags {@code 3} (update + notify) when
     * set, {@code 2} (no notify) when not.
     *
     * @param pos   the cell
     * @param block the block
     * @param level the level writer
     * @param flag  the notify flag
     * @param meta  the original metadata value (see {@link #stateFor})
     */
    public static void setBlock(final BlockPos pos, final Block block, final LevelWriter level,
                                final boolean flag, final int meta) {
        level.setBlock(pos, stateFor(block, meta), flag ? 3 : 2);
    }

    /**
     * The original's conditional replace (the 1.9-generation toolbox):
     * the cell adopts {@code block}/{@code meta} only when it currently holds
     * {@code from} (meta-agnostic, {@code state.is}) whose meta equals
     * {@code fromMeta}, and only while fewer than {@code maxCount} copies of
     * {@code block} already sit within the 16-cell placement region
     * (the original's {@code countBlocks} window). {@code priority} is the
     * original's unused pass-through; {@code -1} bounds mean "no limit".
     */
    public static void setBlock(final BlockPos pos, final Block block, final LevelWriter level, final int meta,
                                final Block from, final int fromMeta, final int priority, final int maxCount) {
        final LevelReader reader = level instanceof LevelReader r ? r : null;
        if (reader == null) {
            return;
        }
        final BlockState current = reader.getBlockState(pos);
        if (!current.is(from)) {
            return;
        }
        if (fromMeta >= 0 && variantOf(current) != fromMeta) {
            return;
        }
        if (maxCount >= 0 && countBlocks(reader, pos, block) >= maxCount) {
            return;
        }
        setBlock(pos, block, level, true, meta);
    }

    /**
     * The original's count-capped set: place only while fewer than
     * {@code count} copies of {@code block} sit within the 16-cell region.
     */
    public static void setBlock(final BlockPos pos, final Block block, final LevelWriter level, final boolean flag,
                                final int meta, final int count) {
        if (countBlocks(level instanceof LevelReader rl ? rl : null, pos, block) >= count) {
            return;
        }
        setBlock(pos, block, level, flag, meta);
    }

    /** The original's direct set with update flags 3 (update + notify). */
    public static void setBlockAndUpdate(final BlockPos pos, final Block block, final LevelWriter level,
                                         final int meta) {
        level.setBlock(pos, stateFor(block, meta), 3);
    }

    /**
     * The toolbox placement entry (the 1.9-generation
     * {@code placeBlock(pos, block, world, flag, meta)}): the original's
     * set-with-meta; the {@code flag} rides the update flags exactly as in
     * {@link #setBlock(BlockPos, Block, LevelWriter, boolean, int)}.
     */
    public static void placeBlock(final BlockPos pos, final Block block, final ServerLevel level,
                                  final boolean flag, final int meta) {
        setBlock(pos, block, level, flag, meta);
    }

    /** The original-meta equivalent of a state (0 for blocks without a variant property). */
    private static int variantOf(final BlockState state) {
        if (state.getBlock() instanceof com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice device) {
            return device.variant(state);
        }
        return 0;
    }

    /** The original's {@code countBlocks}: copies of {@code block} within a 16-cell cube. */
    private static int countBlocks(final LevelReader level, final BlockPos pos, final Block block) {
        int count = 0;
        for (int x = -16; x <= 16; x++) {
            for (int y = -16; y <= 16; y++) {
                for (int z = -16; z <= 16; z++) {
                    if (level.getBlockState(pos.offset(x, y, z)).is(block)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ side checks

    /**
     * The original's {@code BlockUtils.countExposedSides} toolbox shape: sides
     * whose state is not {@code block}. The 1.7.10 maze form counted air
     * sides; 1.9.4 — the form the task pins — compares the block
     * meta-agnostically, ported per the rule as {@code state.is(block)}.
     */
    public static int countExposedSides(final LevelReader level, final BlockPos pos, final Block block) {
        int c = 0;
        for (final Direction dir : Direction.values()) {
            if (!level.getBlockState(pos.relative(dir)).is(block)) {
                c++;
            }
        }
        return c;
    }

    /**
     * The {@code MazeGenPatcher}/{@code BlockUtils} helper (the 1.9-generation
     * form; the shipped maze flush uses it meta-agnostically per task rule 5):
     * at least {@code count} of the six sides — excluding {@code ignore} —
     * carry {@code block}.
     */
    public static boolean isBlockAdjacentToAtleast(final LevelReader level, final Block block, final BlockPos pos,
                                                   final int count, final Direction ignore) {
        int c = 0;
        for (final Direction dir : Direction.values()) {
            if (dir != ignore && level.getBlockState(pos.relative(dir)).is(block)) {
                c++;
            }
        }
        return c >= count;
    }

    /**
     * The original's {@code isBedrockShowing} (ref L325-338): some side is
     * see-through ({@code !isOpaqueCube} → {@code !canOcclude}) while the cell
     * behind it is bedrock or blockEldritchNothing.
     */
    static boolean isBedrockShowing(final LevelReader level, final BlockPos pos) {
        for (final Direction dir : Direction.values()) {
            if (!level.getBlockState(pos.relative(dir)).canOcclude()
                    && (level.getBlockState(pos.relative(dir.getOpposite())).is(Blocks.BEDROCK)
                    || level.getBlockState(pos.relative(dir.getOpposite()))
                    .is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The toolbox side checks (the 1.9-generation
     * {@code checkSurroundingBlockForConnections}): per side in the order
     * NORTH, EAST, SOUTH, WEST — is the neighbour {@code block}
     * (meta-agnostic per task rule 5).
     */
    public static boolean[] checkSurroundingBlockForConnections(final BlockPos pos, final Block block,
                                                                final LevelWriter level, final int meta) {
        final LevelReader reader = level instanceof LevelReader r ? r : null;
        final boolean[] b = new boolean[4];
        for (int i = 0; i < 4; i++) {
            b[i] = reader.getBlockState(pos.relative(Direction.from2DDataValue(i))).is(block);
        }
        return b;
    }

    /**
     * The toolbox corner check (the 1.9-generation {@code checkForConnections}
     * seam the maze patcher used to orient stair runs): a side counts as
     * connected when the neighbour is {@code block} (meta-agnostic), and the
     * two corner cells flanking that side are read for the run's corner
     * fill. Order NORTH, EAST, SOUTH, WEST; the corner flags ride in
     * {@code b[4..7]} for the caller's run logic (toolbox shape).
     */
    public static boolean[] checkForConnections(final BlockPos pos, final Block block, final LevelWriter level,
                                                final int meta) {
        final LevelReader reader = level instanceof LevelReader r ? r : null;
        final boolean[] b = new boolean[8];
        for (int i = 0; i < 4; i++) {
            final Direction dir = Direction.from2DDataValue(i);
            final Direction cw = Direction.from2DDataValue((i + 1) & 3);
            final Direction ccw = Direction.from2DDataValue((i + 3) & 3);
            b[i] = reader.getBlockState(pos.relative(dir)).is(block);
            b[4 + i] = reader.getBlockState(pos.relative(dir).relative(cw)).is(block)
                    && reader.getBlockState(pos.relative(cw)).is(block);
            b[4 + i] |= reader.getBlockState(pos.relative(dir).relative(ccw)).is(block)
                    && reader.getBlockState(pos.relative(ccw)).is(block);
        }
        return b;
    }

    /**
     * The toolbox connection pass (the 1.9-generation
     * {@code generateConnections(pos, block, world, flag, meta)}): set the
     * block, then re-set every matching same-block neighbour so their
     * connection logic (stairs, walls) re-solves against the new cell — the
     * original's effect through 1.21.1's block-update plumbing.
     */
    public static void generateConnections(final BlockPos pos, final Block block, final LevelWriter level,
                                           final boolean flag, final int meta) {
        setBlock(pos, block, level, flag, meta);
        final LevelReader reader = level instanceof LevelReader r ? r : null;
        for (final Direction dir : Direction.values()) {
            final BlockPos side = pos.relative(dir);
            if (reader.getBlockState(side).is(block)) {
                // Re-set the neighbour with its own state: the set itself is
                // the update trigger the original's re-place provided.
                level.setBlock(side, reader.getBlockState(side), flag ? 3 : 2);
            }
        }
    }

    // ------------------------------------------------------------------ code dispatcher (ref L49-259)

    /** The original's {@code placeBlock(world, x, y, z, code, cell)} (ref L49-51). */
    public static void placeBlock(final ServerLevel world, final BlockPos pos, final int b, final Cell cell) {
        placeBlock(world, pos, b, Direction.UP, cell);
    }

    /**
     * The original's code dispatcher (ref L55-259), table verbatim.
     * {@code ForgeDirection.UNKNOWN} (the non-directional entry, ref L50) is
     * the {@code Direction.UP} ordinal 2 branch of the codes 3-6 switch per
     * the shipped callers' encoding note — the original's UNKNOWN ordinal is
     * 0 and falls to the {@code default: break label208} branch (meta 0);
     * the callers pass a real {@code Direction} by name for every code that
     * reads one ({@code GenPassage} L494-512, {@code GenPortal} L290-298), so
     * the UNKNOWN case never carries a stair.
     */
    public static void placeBlock(final ServerLevel world, final BlockPos pos, final int b, final Direction dir,
                                  final Cell cell) {
        Block block = null;
        int meta = 0;

        switch (b) {
            case BEDROCK: // ref L62-66
                if (world.isEmptyBlock(pos)) {
                    block = Blocks.BEDROCK;
                }
                break;
            case STONE: // ref L67-85
                if (cell.feature == 7 && world.random.nextInt(3) == 0) {
                    break;
                }
                if (!world.getBlockState(pos).is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    if (world.random.nextInt(25) == 0) {
                        final boolean crab = cell.feature == 7 || world.random.nextInt(50) == 0;
                        if ((!crab || cell.feature != 0) && (!crab || cell.feature != 7)) {
                            decoCommon.add(pos);
                        } else {
                            crabSpawner.add(pos);
                        }
                    }
                    block = CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get();
                    meta = BlockCosmeticSolid.VARIANT_ANCIENT_STONE;
                }
                break;
            case 3: // ref L86-102
                if (world.random.nextFloat() < 0.005F) {
                    decoUrn.add(pos);
                }
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (forgeOrdinal(dir)) {
                    case 2, 3 -> 1;
                    case 4, 5 -> 3;
                    default -> 0;
                };
                break;
            case 4: // ref L103-119
                if (world.random.nextFloat() < 0.005F) {
                    decoUrn.add(pos);
                }
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (forgeOrdinal(dir)) {
                    case 2, 3 -> 0;
                    case 4, 5 -> 2;
                    default -> 0;
                };
                break;
            case 5: // ref L120-132
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (forgeOrdinal(dir)) {
                    case 2, 3 -> 5;
                    case 4, 5 -> 7;
                    default -> 0;
                };
                break;
            case 6: // ref L133-145
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (forgeOrdinal(dir)) {
                    case 2, 3 -> 4;
                    case 4, 5 -> 6;
                    default -> 0;
                };
                break;
            case 7: // ref L146-149
                block = CatalogEldritchRealm.BLOCK_ELDRITCH.get();
                meta = EldritchRealmValues.ELDRITCH_CRUSTED;
                break;
            case VOID: // ref L150-152
                block = CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get();
                break;
            case AIR_REPL: // ref L153-158
                block = Blocks.AIR;
                decoCommon.remove(pos);
                crabSpawner.remove(pos);
                decoUrn.remove(pos);
                break;
            case STAIR_DIRECTIONAL: // ref L159-175
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (dir) {
                    case NORTH -> 3;
                    case SOUTH -> 2;
                    case EAST -> 0;
                    case WEST -> 1;
                    default -> 0;
                };
                break;
            case STAIR_DIRECTIONAL_INV: // ref L176-192
                block = CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get();
                meta = switch (dir) {
                    case NORTH -> 7;
                    case SOUTH -> 6;
                    case EAST -> 4;
                    case WEST -> 5;
                    default -> 0;
                };
                break;
            case DOOR_BLOCK: // ref L193-199
                block = CatalogEldritchRealm.BLOCK_ELDRITCH.get();
                meta = EldritchRealmValues.ELDRITCH_GATE;
                decoCommon.remove(pos);
                crabSpawner.remove(pos);
                decoUrn.remove(pos);
                break;
            case DOOR_LOCK: // ref L200-206
                block = CatalogEldritchRealm.BLOCK_ELDRITCH.get();
                meta = EldritchRealmValues.ELDRITCH_LOCK;
                decoCommon.remove(pos);
                crabSpawner.remove(pos);
                decoUrn.remove(pos);
                break;
            case VOID_DOOR: // ref L207-210
                block = CatalogDevices.BLOCK_AIRY.get();
                meta = 12;
                break;
            case ROCK: // ref L211-216
                if (!world.getBlockState(pos).is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    block = CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get();
                    meta = BlockCosmeticSolid.VARIANT_ANCIENT_ROCK;
                }
                break;
            case STONE_NOSPAWN: // ref L217-222
                if (!world.getBlockState(pos).is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    block = CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get();
                    meta = BlockCosmeticSolid.VARIANT_ANCIENT_STONE_2;
                }
                break;
            case STONE_TRAPPED: // ref L223-228
                if (!world.getBlockState(pos).is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    block = CatalogEldritchRealm.BLOCK_ELDRITCH.get();
                    meta = EldritchRealmValues.ELDRITCH_TRAP;
                }
                break;
            case CRUST: // ref L229-230 — the case's whole effect is the tail below (ref L237-251)
                if (!world.getBlockState(pos).is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    block = CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get();
                    meta = BlockCosmeticSolid.VARIANT_CRUSTED_STONE;
                    if (world.random.nextInt(25) == 0) { // ref L240-242
                        block = CatalogEldritchRealm.BLOCK_ELDRITCH.get();
                        meta = EldritchRealmValues.ELDRITCH_CRUSTED;
                    } else if (world.random.nextInt(25) == 0) { // ref L243-250
                        final boolean crab = cell.feature == 7
                                || (cell.feature == 12 && world.random.nextBoolean()
                                ? true : world.random.nextInt(25) == 0);
                        if (crab && cell.feature == 0 || crab && cell.feature == 7 || crab && cell.feature == 12) {
                            crabSpawner.add(pos);
                        }
                    }
                }
                break;
            case BEDROCK_REPL: // ref L231-232
                block = Blocks.BEDROCK;
                break;
            default: // ref L233-234 — nothing placed
                break;
        }

        if (block != null) {
            // The original's flag table (ref L254-257): eldritchNothing,
            // bedrock and air set with flags 0; everything else with 3.
            final int flags = block != CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get()
                    && block != Blocks.BEDROCK && block != Blocks.AIR ? 3 : 0;
            world.setBlock(pos, stateFor(block, meta), flags);
        }
    }

    // ------------------------------------------------------------------ obelisk (ref L261-267)

    /** The original's obelisk stack: blockEldritch meta 1, then meta 2 four high (ref L262-266). */
    public static void genObelisk(final ServerLevel world, final BlockPos pos) {
        genObelisk(world, pos.getX(), pos.getY(), pos.getZ());
    }

    /** The original's {@code genObelisk(World, x, y, z)} (ref L261-267), update flags 3. */
    public static void genObelisk(final ServerLevel world, final int x, final int y, final int z) {
        world.setBlock(new BlockPos(x, y, z),
                stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK), 3);
        world.setBlock(new BlockPos(x, y + 1, z),
                stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK_TOP), 3);
        world.setBlock(new BlockPos(x, y + 2, z),
                stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK_TOP), 3);
        world.setBlock(new BlockPos(x, y + 3, z),
                stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK_TOP), 3);
        world.setBlock(new BlockPos(x, y + 4, z),
                stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), EldritchRealmValues.ELDRITCH_OBELISK_TOP), 3);
    }

    // ------------------------------------------------------------------ decoration drain (ref L269-323)

    /**
     * The maze flush (the shipped {@code MazeHandler} calls it): the
     * original's three queue loops in the original order (ref L270-322), the
     * world-random draws verbatim, then the toolbox queue drain (types
     * 0..11, the 1.9-generation switch the toolbox callers compile against),
     * every dispatched entry removed.
     */
    public static void processDecorations(final ServerLevel world) {
        // Ref L270-277: urns — a pedestal under an air cell, then the urn on
        // the rarity roll. The rarity (meta 2 rare <0.025F, meta 1 normal
        // <0.1F, meta 0 plain) has no shipped urn-tile consumer (class (c));
        // the BlockLoot URN ships plain and the rarity rides the javadoc for
        // the loot card that reads it.
        for (final BlockPos cc : decoUrn) {
            if (world.isEmptyBlock(cc.above())) {
                world.setBlock(cc, stateFor(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(),
                        BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL), 3);
                final float rr = world.random.nextFloat();
                final int meta = rr < 0.025F ? 2 : (rr < 0.1F ? 1 : 0);
                world.setBlock(cc.above(),
                        stateFor(CatalogTrinketsLoot.BLOCK_LOOT_URN.get(), meta), 3);
            }
        }

        // Ref L279-301: common wall deco — crusted/glyphed/trap roll, and a
        // crystal on the meta-4 nextInt(12) follow-up.
        for (final BlockPos ccx : decoCommon) {
            final int exp = countExposedSides(world, ccx, world.getBlockState(ccx).getBlock());
            if (exp > 0
                    && (exp == 1 || !isBedrockShowing(world, ccx))
                    && !isBlockAdjacentToAtleast(world, CatalogEldritchRealm.BLOCK_ELDRITCH.get(), ccx, 1, null)) {
                final int meta = world.random.nextInt(3) != 0
                        ? EldritchRealmValues.ELDRITCH_CRUSTED
                        : (world.random.nextInt(8) != 0
                        ? EldritchRealmValues.ELDRITCH_GLYPHED : EldritchRealmValues.ELDRITCH_TRAP);
                world.setBlock(ccx, stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), meta), 3);
                if (meta == EldritchRealmValues.ELDRITCH_CRUSTED && world.random.nextInt(12) == 0) {
                    for (final Direction dir : Direction.values()) {
                        final BlockPos side = ccx.relative(dir);
                        if (world.isEmptyBlock(side)) {
                            world.setBlock(side, stateFor(CatalogDecorativeBlocks.BLOCK_CRYSTAL.get(),
                                    BlockCrystal.VARIANT_STRANGE), 3);
                            if (world.getBlockEntity(side) instanceof TileCrystal crystal) {
                                crystal.setOrientation(dir);
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Ref L303-318: crab spawners — eldritch meta 9 and the facing byte of
        // the first see-through side (the shipped TileEldritchCrabSpawner
        // facing wire format is the ForgeDirection ordinal, so the original
        // (byte)dir.ordinal() cast survives via forgeOrdinal).
        for (final BlockPos ccxx : crabSpawner) {
            final int exp = countExposedSides(world, ccxx, world.getBlockState(ccxx).getBlock());
            if (exp == 1
                    && !isBlockAdjacentToAtleast(world, CatalogEldritchRealm.BLOCK_ELDRITCH.get(), ccxx, 1, null)) {
                world.setBlock(ccxx, stateFor(CatalogEldritchRealm.BLOCK_ELDRITCH.get(),
                        EldritchRealmValues.ELDRITCH_CRAB_SPAWNER), 3);
                if (world.getBlockEntity(ccxx) instanceof TileEldritchCrabSpawner spawner) {
                    for (final Direction dir : Direction.values()) {
                        if (world.isEmptyBlock(ccxx.relative(dir))) {
                            spawner.setFacing((byte) forgeOrdinal(dir));
                            break;
                        }
                    }
                }
            }
        }

        // Ref L320-322: the original's clear.
        decoCommon.clear();
        crabSpawner.clear();
        decoUrn.clear();

        // The toolbox queue: the 1.9-generation decoration switch, entries
        // removed as dispatched.
        for (final Decoration decoration : DECORATED_BLOCKS) {
            dispatchDecoration(decoration);
            DECORATED_BLOCKS.remove(decoration);
        }
    }

    /** The decoration switch body (types 0..11). */
    private static void dispatchDecoration(final Decoration decoration) {
        final ServerLevel world = decoration.level();
        final BlockPos pos = decoration.pos();
        final Block block = decoration.block();
        final int meta = decoration.meta();
        switch (decoration.type()) {
            case DECORATION_TREE -> setBlockAndUpdate(pos, block, world, meta);
            case DECORATION_SPAWNER -> {
                // The eldritch spawner variant has no shipped block state
                // (BlockEldritch carries 11 variants, no spawner), so the
                // original's spawner tile rides on the vanilla mob spawner:
                // the guardian id is kept verbatim in SPAWNER_ID_ELDRITCH_GUARDIAN
                // and is inert until TC-M6-06 registers it; the original's
                // vanilla fallback carries the spawner.
                setBlockAndUpdate(pos, Blocks.SPAWNER, world, 0);
                applySpawnerId(world, pos, SPAWNER_ID_ELDRITCH_GUARDIAN);
            }
            case DECORATION_STAIRS -> setBlockAndUpdate(pos, block, world, meta);
            case DECORATION_LOOT -> {
                setBlockAndUpdate(pos, CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(),
                        world, BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL);
                setBlockAndUpdate(pos.above(), CatalogTrinketsLoot.BLOCK_LOOT_URN.get(), world, meta);
            }
            case DECORATION_MOB_SPAWNER -> {
                setBlockAndUpdate(pos, Blocks.SPAWNER, world, 0);
                applySpawnerId(world, pos, FALLBACK_SPAWNER_ID);
            }
            case DECORATION_PILLAR -> {
                setBlockAndUpdate(pos, block, world, meta);
                if (meta >= 3 && meta <= 6
                        && world.getBlockEntity(pos) instanceof TileEldritchCrabSpawner spawner) {
                    spawner.setFacing((byte) meta);
                }
            }
            case DECORATION_CHEST -> {
                setBlockAndUpdate(pos, Blocks.CHEST, world, 0);
                applyChestLoot(world, pos);
            }
            case DECORATION_GOLDPAN -> {
                // The golden-head item ships on its own card (no shipped item
                // row for "goldenhead"); the pan stays inert until it lands.
                ThaumcraftClassic.LOGGER.debug("[TC-M6-04f] gold pan decoration at {} skipped: the "
                        + "golden-head item ({}) is not registered yet", pos, GOLDEN_HEAD_ID);
            }
            case DECORATION_ARCANE ->
                // The original decorates with a Thaumcraft potion ItemStack;
                // no potion catalog is shipped (registration/catalog has no
                // CatalogPotions), so the arcane decoration is inert until
                // the potion ships.
                    ThaumcraftClassic.LOGGER.debug("[TC-M6-04f] arcane decoration at {} skipped: the "
                            + "Thaumcraft potion item is not registered yet", pos);
            case DECORATION_TAINT ->
                // blockTaintFoliage ships on the taint card (no shipped row);
                // the draw-free decoration is skipped, matching the shipped
                // taint-fibres pattern in GenPassage.
                    ThaumcraftClassic.LOGGER.debug("[TC-M6-04f] taint foliage decoration at {} skipped: the "
                            + "taint foliage block is not registered yet", pos);
            case DECORATION_FIRE -> setBlockAndUpdate(pos, Blocks.FIRE, world, 0);
            case DECORATION_CRYSTAL -> {
                setBlockAndUpdate(pos, CatalogDecorativeBlocks.BLOCK_CRYSTAL.get(), world, meta);
                if (world.getBlockEntity(pos) instanceof TileCrystal crystal) {
                    crystal.setOrientation(Direction.UP);
                }
            }
            default -> ThaumcraftClassic.LOGGER.debug("[TC-M6-04f] unknown decoration type {} at {}",
                    decoration.type(), pos);
        }
    }

    /** The house chest-loot pattern (worldgen/StructureGen.fillDungeonChest): loot table + seed. */
    static void applyChestLoot(final ServerLevel world, final BlockPos pos) {
        final ChestBlockEntity chest = world.getBlockEntity(pos, BlockEntityType.CHEST).orElse(null);
        if (chest == null) {
            return;
        }
        // setLoot 66 → the ThaumVault loot id verbatim (CHEST_LOOT_THAUM_VAULT);
        // the registry path is the lowercase form (ResourceLocation path rule)
        // and the table ships with its card, so the reference is inert — the
        // chest stays empty until then, mirroring the unresolvable-loot
        // behaviour of the shipped pattern.
        chest.setLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                        CHEST_LOOT_THAUM_VAULT.toLowerCase())));
        chest.setLootTableSeed(world.random.nextLong());
    }

    /**
     * The spawner id apply: {@code SpawnerBlockEntity.setEntityId} with ids
     * the shipped repo registers. The 1.7.10-era ids resolve in the
     * {@code thaumcraft} namespace (lowercased — registry paths are
     * lowercase) then the vanilla namespace; unresolvable ids (the eldritch
     * mobs pre-TC-M6-06) fall back to the original's vanilla
     * {@code "Silverfish"} id.
     */
    static void applySpawnerId(final ServerLevel world, final BlockPos pos, final String originalId) {
        final SpawnerBlockEntity spawner = world.getBlockEntity(pos, BlockEntityType.MOB_SPAWNER).orElse(null);
        if (spawner == null) {
            return;
        }
        EntityType<?> type = resolveEntity(originalId);
        if (type == null) {
            type = resolveEntity(FALLBACK_SPAWNER_ID);
        }
        if (type != null) {
            spawner.setEntityId(type, world.random);
        }
    }

    /** A 1.7.10-era entity name → the shipped {@code EntityType} (thaumcraft, then vanilla namespace). */
    private static EntityType<?> resolveEntity(final String name) {
        final String path = name.toLowerCase();
        var tc = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(
                ThaumcraftClassic.MOD_ID, path));
        if (tc.isPresent()) {
            return tc.get();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.withDefaultNamespace(path)).orElse(null);
    }

    // ------------------------------------------------------------------ maze connections (ref L340-414)

    /**
     * The original's four-way connection pass (ref L340-414): the tunnel
     * stubs of a cell's open sides, drawing no random of its own (the
     * {@code placeBlock} draws ride the world random, as the original
     * documents at ref L68-87/L240-246).
     */
    public static void generateConnections(final ServerLevel world, final Random random, final int cx, final int cz,
                                           final int y, final Cell cell, final int depth, final boolean justthetip) {
        final int x = cx * 16;
        final int z = cz * 16;

        if (cell.north) {
            for (int d = 0; d <= depth; d++) {
                for (int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                     w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                     w++) {
                    for (int h = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                         h < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                         h++) {
                        if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                            placeBlock(world, new BlockPos(x + 3 + w, y + 10 - h, z + d),
                                    PAT_CONNECT[h][w], Direction.NORTH, cell);
                        }
                    }
                }
            }
        }

        if (cell.south) {
            for (int d = 0; d <= depth; d++) {
                for (int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                     w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                     w++) {
                    for (int hx = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                         hx < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                         hx++) {
                        if (d != depth || !justthetip || PAT_CONNECT[hx][w] != 8) {
                            placeBlock(world, new BlockPos(x + 3 + w, y + 10 - hx, z + 16 - d),
                                    PAT_CONNECT[hx][w], Direction.SOUTH, cell);
                        }
                    }
                }
            }
        }

        if (cell.east) {
            for (int d = 0; d <= depth; d++) {
                for (int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                     w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                     w++) {
                    for (int hxx = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                         hxx < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                         hxx++) {
                        if (d != depth || !justthetip || PAT_CONNECT[hxx][w] != 8) {
                            placeBlock(world, new BlockPos(x + 16 - d, y + 10 - hxx, z + 3 + w),
                                    PAT_CONNECT[hxx][w], Direction.EAST, cell);
                        }
                    }
                }
            }
        }

        if (cell.west) {
            for (int d = 0; d <= depth; d++) {
                for (int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                     w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                     w++) {
                    for (int hxxx = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0);
                         hxxx < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11));
                         hxxx++) {
                        if (d != depth || !justthetip || PAT_CONNECT[hxxx][w] != 8) {
                            placeBlock(world, new BlockPos(x + d, y + 10 - hxxx, z + 3 + w),
                                    PAT_CONNECT[hxxx][w], Direction.WEST, cell);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ decoration toolbox seams

    /**
     * Toolbox seam (the 1.9-generation {@code decorateTree}): queue a tree/log
     * cell for {@link #processDecorations}; no random is drawn here — the
     * callers roll, as the original's queue model demands.
     */
    public static void decorateTree(final BlockPos pos, final Random random, final Block block, final int meta,
                                    final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_TREE));
    }

    /**
     * Toolbox seam (the 1.9-generation {@code decorateCrystal}): queue a crystal
     * of a random aspect (the original draws one {@code nextInt(6)} for the
     * element crystal, meta 1-6; {@code Random} order: one draw) and orient it
     * UP — the maze's own crystals ride {@link #decoCommon} verbatim.
     */
    public static void decorateCrystal(final BlockPos pos, final Random random, final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, CatalogDecorativeBlocks.BLOCK_CRYSTAL.get(),
                random.nextInt(6) + 1, DECORATION_CRYSTAL));
    }

    /**
     * Toolbox seam (the 1.9-generation {@code decorateEnderman}): the original
     * spawns an enderman on an air cell over obsidian on a 1-in-10 roll
     * (reconstructed; one {@code nextInt(10)} draw). The enderman is a vanilla
     * entity the shipped repo registers nothing for — vanilla ids need no
     * registration.
     */
    public static void decorateEnderman(final BlockPos pos, final Random random, final ServerLevel level) {
        if (!level.isEmptyBlock(pos) || random.nextInt(10) != 0) {
            return;
        }
        final EnderMan enderman = new EnderMan(EntityType.ENDERMAN, level);
        enderman.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(enderman);
    }

    /** Toolbox seam (the 1.9-generation {@code decorateSpawner}): queue the eldritch spawner (meta kept verbatim; consumer is the spawner tile card). */
    public static void decorateSpawner(final BlockPos pos, final Random random, final Block block, final int meta,
                                       final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_SPAWNER));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateMobSpawner}): queue a vanilla mob spawner. */
    public static void decorateMobSpawner(final BlockPos pos, final Random random, final Block block, final int meta,
                                          final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_MOB_SPAWNER));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateBlockLoot}): queue an urn (the rarity meta is class (c), consumed by the loot card). */
    public static void decorateBlockLoot(final BlockPos pos, final Random random, final Block block, final int meta,
                                         final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_LOOT));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateBlockPillar}): queue a pillar; metas 3-6 drive the crab-spawner facing byte. */
    public static void decorateBlockPillar(final BlockPos pos, final Random random, final Block block,
                                           final int meta, final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_PILLAR));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateBlockChest}): queue a chest (loot 66 = ThaumVault, see {@link #CHEST_LOOT_THAUM_VAULT}). */
    public static void decorateBlockChest(final BlockPos pos, final Random random, final Block block, final int meta,
                                          final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_CHEST));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateGoldPan}): queue a gold pan (golden head, inert until the item ships). */
    public static void decorateGoldPan(final BlockPos pos, final Random random, final Block block, final int meta,
                                       final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_GOLDPAN));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateArcane}): queue an arcane decoration (potion item, inert until it ships). */
    public static void decorateArcane(final BlockPos pos, final Random random, final Block block, final int meta,
                                      final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_ARCANE));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateTaintFoliage}): queue taint foliage (block inert until the taint card). */
    public static void decorateTaintFoliage(final BlockPos pos, final Random random, final Block block,
                                            final int meta, final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_TAINT));
    }

    /** Toolbox seam (the 1.9-generation {@code decorateFire}): queue a fire cell (vanilla {@code BlockFire} stand-in, see the switch). */
    public static void decorateFire(final BlockPos pos, final Random random, final Block block, final int meta,
                                    final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_FIRE));
    }

    /**
     * The stairs decoration overloads (the 1.9-generation {@code decorateStairs}):
     * queue the stair cell (the meta rides the original stair-orientation
     * domain 0-7, mapped by {@link #stairState}). The 1.7.10 reference has no
     * {@code decorateStairs} — its stairs ride the {@code placeBlock} codes
     * 3-6/10-11 — these are the toolbox seams the 1.9-generation callers
     * compile against.
     */
    public static void decorateStairs(final BlockPos pos, final Random random, final Block block, final int meta,
                                      final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_STAIRS));
    }

    /** The stairs-decoration overload without a random (the caller rolled). */
    public static void decorateStairs(final BlockPos pos, final Block block, final int meta, final ServerLevel level) {
        DECORATED_BLOCKS.add(new Decoration(level, pos, block, meta, DECORATION_STAIRS));
    }

    /** The stairs-decoration overload on a level writer (the meta-mapped direct set). */
    public static void decorateStairs(final BlockPos pos, final Block block, final int meta, final LevelWriter level) {
        setBlockAndUpdate(pos, block, level, meta);
    }
}
