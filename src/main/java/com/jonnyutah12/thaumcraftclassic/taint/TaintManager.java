package com.jonnyutah12.thaumcraftclassic.taint;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * TC-M5-04-s: the taint spread / conversion / cleansing engine
 * (content-mapping.csv row {@code worldgen.taint_spread_conversion},
 * {@code impl=TaintManager}; design record docs/architecture/taint.md).
 *
 * <p>Port of the original static engine — {@code BlockTaintFibres.spreadFibres
 * / taintBiomeSpread / getAdjacentTaint / isOnlyAdjacentToTaint}, and the
 * {@code updateTick} bodies of {@code BlockTaint} (lines 120-199) and
 * {@code BlockTaintFibres} (lines 129-172) — centralised here so the block
 * classes stay thin and every rule has one home. The roll structures and
 * thresholds live in {@link TaintRules} (pure, unit-tested, parity-pinned to
 * the original source); the target categories in {@link TaintConversion}; the
 * biome write primitive in {@link TaintBiomes}.</p>
 *
 * <p><b>Guarded seams.</b> Content owned by sibling cards is looked up in the
 * live registries at call time, so the engine runs today and needs no edit
 * when the sibling lands: the flux-goo block ({@code thaumcraft:blockfluxgoo},
 * the flux resource row), the taint creatures ({@code thaumcraft:taintspore},
 * {@code thaumcraft:taintswarmer}, {@code thaumcraft:fallingtaint} — TC-M6-06)
 * and the Taint Poison effect ({@code thaumcraft:potion.fluxtaint}, TC-M6-05).
 * While a dependency is absent its branch is inert — for the swarmer that
 * means the original's "remove the crust, spawn a swarmer" branch is skipped
 * (a swarmer can never exist, so the branch cannot fire), and the flux-goo
 * decay targets nothing. Every other rule behaves exactly like the original;
 * {@code TaintGameTest.guardedDependenciesStayInert} proves it.</p>
 *
 * <p>Deviations, all recorded in docs/architecture/taint.md (BD-14): the
 * original's {@code Material}-keyed conversion table becomes tag + block sets;
 * {@code BlockFlower} (1.7.10 tall grass, dead bush, saplings, …) becomes the
 * modern {@code #minecraft:flowers} tag, so the fibre seed accepts a slightly
 * narrower plant set; the biome-restore half keeps the original's
 * taint→plains branch and no-ops on columns the taint never touched.</p>
 */
public final class TaintManager {

    /** Original {@code ConfigBlocks.blockFluxGoo} (the flux resource row's block). */
    public static final ResourceLocation FLUX_GOO_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockfluxgoo");

    /** Original {@code EntityTaintSpore} / {@code EntityTaintSporeSwarmer} (TC-M6-06). */
    public static final ResourceLocation TAINT_SPORE_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintspore");
    public static final ResourceLocation TAINT_SWARMER_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintswarmer");

    /** Original {@code EntityFallingTaint} (TC-M6-06); gravity falls back without it. */
    public static final ResourceLocation FALLING_TAINT_ID = BlockTaint.FALLING_TAINT_ID;

    private TaintManager() {
    }

    // ------------------------------------------------------------- category tests

    /** Original {@code block.getMaterial() == Config.taintMaterial}. */
    public static boolean isTaintBlock(final BlockState state) {
        return state.getBlock() instanceof BlockTaint || state.getBlock() instanceof BlockTaintFibres;
    }

    /** Port of {@code BlockTaintFibres.getAdjacentTaint} (the 6-side count). */
    public static int getAdjacentTaint(final Level level, final BlockPos pos) {
        int count = 0;
        for (final Direction d : Direction.values()) {
            if (isTaintBlock(level.getBlockState(pos.relative(d)))) {
                count++;
            }
        }
        return count;
    }

    /** Port of {@code BlockTaintFibres.isOnlyAdjacentToTaint}: air or taint only. */
    public static boolean isOnlyAdjacentToTaint(final Level level, final BlockPos pos) {
        for (final Direction d : Direction.values()) {
            final BlockState n = level.getBlockState(pos.relative(d));
            if (!n.isAir() && !isTaintBlock(n)) {
                return false;
            }
        }
        return true;
    }

    /** Port of {@code BlockUtils.isAdjacentToSolidBlock}. */
    public static boolean isAdjacentToSolidBlock(final Level level, final BlockPos pos) {
        for (final Direction d : Direction.values()) {
            final BlockPos np = pos.relative(d);
            if (level.getBlockState(np).isFaceSturdy(level, np, d.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- fibre seeding

    /**
     * Port of {@code BlockTaintFibres.spreadFibres} verbatim, including the
     * original's use of the world random (not the tick random) for the ladder
     * draws and its meta ladder: plain fibres 90% of the time; with air above
     * and a solid face below, 9/10 → grass-1, else 10/12 → grass-2, else a
     * spore stalk.
     */
    public static boolean spreadFibres(final ServerLevel level, final BlockPos pos) {
        final BlockState target = level.getBlockState(pos);
        final boolean seedable = TaintRules.canSeedFibres(
                isAdjacentToSolidBlock(level, pos),
                isOnlyAdjacentToTaint(level, pos),
                target.liquid(),
                target.isAir(),
                target.canBeReplaced(),
                target.is(BlockTags.FLOWERS),
                target.is(BlockTags.LEAVES));
        if (!seedable) {
            return false;
        }
        final RandomSource rand = level.getRandom();
        final int meta = TaintRules.fibresMeta(
                rand.nextInt(TaintRules.FIBRES_PLAIN_MOD),
                level.getBlockState(pos.above()).isAir(),
                level.getBlockState(pos.below())
                        .isFaceSturdy(level, pos.below(), Direction.UP),
                rand.nextInt(TaintRules.FIBRES_GRASS1_MOD),
                rand.nextInt(TaintRules.FIBRES_GRASS2_MOD));
        setFibres(level, pos, meta);
        return true;
    }

    public static void setFibres(final ServerLevel level, final BlockPos pos, final int meta) {
        final BlockState old = level.getBlockState(pos);
        final BlockState next = stateOf(FIBRES_ID, meta);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        level.sendBlockUpdated(pos, old, next, Block.UPDATE_ALL);
    }

    public static void setTaint(final ServerLevel level, final BlockPos pos, final int meta) {
        final BlockState old = level.getBlockState(pos);
        final BlockState next = stateOf(TAINT_ID, meta);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        level.sendBlockUpdated(pos, old, next, Block.UPDATE_ALL);
    }

    /** The block id of each taint mega-block (D-001: the original's lowercased ids). */
    public static final ResourceLocation TAINT_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blocktaint");
    public static final ResourceLocation FIBRES_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blocktaintfibres");

    /**
     * The state for one original metadata value: the catalog block's
     * {@code variant} property (the house multi-variant convention,
     * {@link com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems}) set
     * to the meta. Resolved by id, so the engine carries no holder dependency
     * on the catalog class.
     */
    public static BlockState stateOf(final ResourceLocation blockId, final int meta) {
        final Block block = BuiltInRegistries.BLOCK.get(blockId);
        @SuppressWarnings("unchecked")
        final Property<Integer> variant =
                (Property<Integer>) block.getStateDefinition().getProperty(
                        com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems.VARIANT_PROPERTY);
        return variant == null ? block.defaultBlockState() : block.defaultBlockState().setValue(variant, meta);
    }

    // ------------------------------------------------------------- biome spread

    /**
     * Port of {@code BlockTaintFibres.taintBiomeSpread}. The caller reads
     * {@code general/biome_taint_spread} through {@code TcConfig} and passes it
     * in, so the rule stays deterministic-testable (docs/testing.md: seeded
     * tests for random processes).
     */
    public static void taintBiomeSpread(final ServerLevel level, final BlockPos pos,
                                        final RandomSource rand, final int spreadRate) {
        if (spreadRate <= 0) {
            return; // the original's `if (Config.taintSpreadRate > 0)`
        }
        final int xx = rand.nextInt(3) - 1;
        final int zz = rand.nextInt(3) - 1;
        final BlockPos at = pos.offset(xx, 0, zz);
        final Holder<Biome> biome = TaintBiomes.biomeAt(level, at);
        if (TaintRules.spreadsBiome(spreadRate, TaintBiomes.isTaintBiome(biome),
                rand.nextInt(TaintRules.biomeSpreadModulus(spreadRate)), getAdjacentTaint(level, pos))) {
            TaintBiomes.setBiomeAtAndSync(level, at.getX(), at.getZ(), TaintBiomes.taintBiomeHolder(level));
        }
    }

    // ------------------------------------------------------------- tick bodies

    /**
     * Port of {@code BlockTaint.updateTick} (crust / soil / flesh tick),
     * branch for branch: flesh never ticks; biome spread; unsupported-crust
     * gravity; the scan-box seed; the crust→swarmer branch; the
     * surrounded-crust→flux-goo branch; and the outside-the-biome decay rules.
     *
     * <p>The original's {@code BlockTaint.updateTick} calls
     * {@code spreadFibres} and ignores the result — the crust block converts
     * nothing. The crust/soil conversion table lives in the fibres tick
     * ({@link #fibresTick}), exactly as in the original.</p>
     */
    public static void taintBlockTick(final ServerLevel level, final BlockPos pos,
                                      final BlockState state, final RandomSource rand) {
        final int md = state.getValue(BlockTaint.VARIANT);
        if (md == BlockTaint.META_FLESH) {
            return; // original line 122: flesh never ticks
        }
        taintBiomeSpread(level, pos, rand, TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD));

        if (md == BlockTaint.META_CRUST) {
            if (tryToFall(level, pos, pos)) {
                return;
            }
            if (level.getBlockState(pos.above()).isAir()) {
                boolean doIt = true;
                final Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
                for (int a = 0; a < 4 && doIt; a++) {
                    if (!level.getBlockState(pos.offset(dir.getStepX(), -a, dir.getStepZ())).isAir()) {
                        doIt = false;
                    } else if (!(level.getBlockState(pos.offset(0, -a, 0)).getBlock() instanceof BlockTaint)) {
                        doIt = false;
                    }
                }
                if (doIt && tryToFall(level, pos, pos.offset(dir.getStepX(), 0, dir.getStepZ()))) {
                    return;
                }
            }
        }

        final BlockPos off = scanOffset(level, pos, rand);
        if (TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, off))) {
            spreadFibres(level, off); // the original ignores the result here

            if (md == BlockTaint.META_CRUST) {
                if (TaintRules.spawnsSporeSwarmer(rand.nextInt(TaintRules.CRUST_SWARMER_MOD),
                        level.getBlockState(pos.above()).isAir(),
                        TcConfig.getBool(TcConfigCatalog.SPAWN_TAINT_SPORES))) {
                    if (spawnSwarmer(level, pos)) {
                        return;
                    }
                }
                // Original: a crust column walled in by crust turns to flux goo.
                if (surroundedByCrust(level, pos)) {
                    convertToFluxGoo(level, pos);
                }
            }
        } else if (md == BlockTaint.META_CRUST
                && TaintRules.crustDecaysOutsideBiome(rand.nextInt(TaintRules.CRUST_DECAY_MOD))) {
            convertToFluxGoo(level, pos);
        } else if (md == BlockTaint.META_SOIL
                && TaintRules.soilDecaysOutsideBiome(rand.nextInt(TaintRules.SOIL_DECAY_MOD))) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Port of {@code BlockTaintFibres.updateTick} (fibres tick): meta-0 fibres
     * that sit only against taint, and any fibres that leave the biome, die;
     * inside the biome the same tick seeds fibres and applies the conversion
     * table to the scan cell, and drives the spore-stalk ladder. The spore
     * branches sit inside the {@code !spreadFibres} arm, exactly as the
     * original nests them.
     */
    public static void fibresTick(final ServerLevel level, final BlockPos pos,
                                  final BlockState state, final RandomSource rand) {
        final int md = state.getValue(BlockTaintFibres.VARIANT);
        taintBiomeSpread(level, pos, rand, TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD));

        if ((md == BlockTaintFibres.META_FIBRES && isOnlyAdjacentToTaint(level, pos))
                || !TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, pos))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        final BlockPos off = scanOffset(level, pos, rand);
        if (!TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, off))) {
            return;
        }
        if (spreadFibres(level, off)) {
            return;
        }

        final int adjacentTaint = getAdjacentTaint(level, off);
        final BlockState target = level.getBlockState(off);
        if (TaintRules.convertsToCrust(adjacentTaint, TaintConversion.isCrustTarget(target))) {
            setTaint(level, off, BlockTaint.META_CRUST);
        }
        if (TaintRules.convertsToSoil(adjacentTaint, target.isAir(), TaintConversion.isSoilTarget(target))) {
            setTaint(level, off, BlockTaint.META_SOIL);
        }

        if (TaintRules.maturesSporeStalk(md, rand.nextInt(TaintRules.SPORE_STALK_MOD),
                level.getBlockState(pos.above()).isAir(), TcConfig.getBool(TcConfigCatalog.SPAWN_TAINT_SPORES))) {
            setFibres(level, pos, BlockTaintFibres.META_MATURE_SPORE_STALK);
            spawnGuarded(level, TAINT_SPORE_ID, pos.above().getCenter());
        } else if (TaintRules.revertsMatureStalk(md, countEntities(level, TAINT_SPORE_ID,
                boxAbove(pos)))) {
            // The original scans the box above the stalk for a spore entity;
            // while TC-M6-06 has not registered the spore there can never be
            // one, so the revert is the faithful outcome.
            setFibres(level, pos, BlockTaintFibres.META_SPORE_STALK);
        }
    }

    /** The 1×1×1 box directly above {@code pos} (the original's spore scan box). */
    private static AABB boxAbove(final BlockPos pos) {
        final BlockPos above = pos.above();
        return new AABB(above.getX(), above.getY(), above.getZ(),
                above.getX() + 1, above.getY() + 1, above.getZ() + 1);
    }

    /** {@code x + rand.nextInt(3) - 1, y + rand.nextInt(5) - 3, z + rand.nextInt(3) - 1}. */
    private static BlockPos scanOffset(final Level level, final BlockPos pos, final RandomSource rand) {
        return pos.offset(
                TaintRules.scanDelta(rand.nextInt(TaintRules.SCAN_XZ_MOD), TaintRules.SCAN_XZ_MOD, 1),
                TaintRules.scanDelta(rand.nextInt(TaintRules.SCAN_Y_MOD), TaintRules.SCAN_Y_MOD,
                        TaintRules.SCAN_Y_SHIFT),
                TaintRules.scanDelta(rand.nextInt(TaintRules.SCAN_XZ_MOD), TaintRules.SCAN_XZ_MOD, 1));
    }

    /** The original's "crust with crust above and on all four sides" test. */
    private static boolean surroundedByCrust(final Level level, final BlockPos pos) {
        if (!(level.getBlockState(pos.above()).getBlock() instanceof BlockTaint)) {
            return false;
        }
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockState(pos.relative(d)).getBlock() instanceof BlockTaint)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The original's swarmer branch: the scan box (the cell inflated by 16)
     * must hold no swarmer, then the crust is removed and the swarmer spawned
     * with the roots cue. Inert while TC-M6-06 has not registered
     * {@code thaumcraft:taintswarmer} — no swarmer can exist, and the original
     * only ever reaches this branch to create one.
     */
    private static boolean spawnSwarmer(final ServerLevel level, final BlockPos pos) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(TAINT_SWARMER_ID);
        if (type == null) {
            return false;
        }
        final AABB scan = new AABB(pos).inflate(TaintRules.SWARMER_SCAN_INFLATE);
        if (!level.getEntitiesOfClass(Entity.class, scan,
                e -> e.getType() == type).isEmpty()) {
            return false;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (spawnGuarded(level, TAINT_SWARMER_ID, pos.getCenter())) {
            level.playSound(null, pos, ModSounds.ROOTS.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                    TaintRules.ROOTS_VOLUME,
                    TaintRules.ROOTS_PITCH_BASE + level.getRandom().nextFloat() * TaintRules.ROOTS_PITCH_SPREAD);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------- cleansing

    /**
     * TC-M5-04-s-b: the server branch of {@code TileEtherealBloom.update}
     * (TileEtherealBloom.java:16-45), called from the tile every
     * {@code counter % 20 == 0}. One column per roll:
     *
     * <ol><li>a {@code nextInt(8) - nextInt(8)} offset on each horizontal axis
     * (the original draws x first, then z — the order is the seeded
     * sequence);</li>
     * <li>the column's <b>stored</b> biome must be one of the three thaumic
     * biomes the original lists ({@link TaintBiomes#CLEANSABLE_BIOMES}: taint,
     * eerie, magical forest — the original's {@code Config.biomeTaintID},
     * {@code biomeEerieID}, {@code biomeMagicalForestID});</li>
     * <li>the column must be inside the radius gate
     * ({@link TaintRules#bloomDistanceSquared} — the original's squared
     * {@code TileEntity#getDistance}, so the disc is radius ~9, not 81);</li>
     * <li>then the column's biome is replaced by the biome the world generates
     * there, i.e. the biome the taint overwrote (plains when even that answer
     * is a thaumic biome) — {@link #restoreBiome}.</li></ol>
     *
     * <p>Returns true when a column was rewritten, which is what a probe
     * asserts (the bloom is a one-column-per-20-ticks machine: over 400 rolls
     * it cleanses repeatedly inside its disc and never outside it).</p>
     */
    public static boolean etherealBloomCleanse(final ServerLevel level, final BlockPos bloomPos,
                                               final net.minecraft.util.RandomSource random) {
        final int tx = TaintRules.bloomOffset(random.nextInt(TaintRules.BLOOM_SCAN_MOD),
                random.nextInt(TaintRules.BLOOM_SCAN_MOD));
        final int tz = TaintRules.bloomOffset(random.nextInt(TaintRules.BLOOM_SCAN_MOD),
                random.nextInt(TaintRules.BLOOM_SCAN_MOD));
        final int x = bloomPos.getX() + tx;
        final int z = bloomPos.getZ() + tz;
        final Holder<Biome> stored = TaintBiomes.biomeAt(level, new BlockPos(x, bloomPos.getY(), z));
        if (!TaintRules.bloomCleanses(TaintBiomes.isCleansableBiome(stored),
                TaintRules.bloomDistanceSquared(tx, tz))) {
            return false;
        }
        restoreBiome(level, x, bloomPos.getY(), z);
        return true;
    }

    /**
     * TC-M5-04-s-b: the restore-to-original-biome half of the cleansing path,
     * the port's {@code Utils.setBiomeAt(world, x, z, biome)} call from
     * {@code TileEtherealBloom.update}. The biome written is the one the world
     * GENERATES at that column — the original reads it from the chunk manager
     * ({@code getBiomesToGenerateAt}), which in 1.7.10 was a separate generated
     * biome array and in 1.21.1 is the biome recorded at the moment taint
     * overwrote the palette ({@link TaintBiomes#takeGeneratedBiome}, BD-16).
     * Plains is the original's fallback, used when even the generated answer is
     * a thaumic biome (a column that has been taint for so long that nothing
     * recorded a pre-taint biome).
     *
     * <p>Not config-gated: the original's bloom path has no config check, and
     * {@code general/biome_taint_from_flux} gates the flux→taint direction, not
     * the way back.</p>
     */
    public static void restoreBiome(final ServerLevel level, final int x, final int z) {
        restoreBiome(level, x, level.getMinBuildHeight() + 1, z);
    }

    /**
     * The restore rule at an explicit height (the quart the biome is read at).
     * See {@link #restoreBiome(ServerLevel, int, int)} for the rule.
     */
    public static void restoreBiome(final ServerLevel level, final int x, final int y, final int z) {
        final Holder<Biome> generated = TaintBiomes.takeGeneratedBiome(level, x, y, z);
        // Fidelity: the original substitutes plains ONLY when the generated
        // answer is the taint biome (TileEtherealBloom.java:37 —
        // `biome.field_76756_M == ThaumcraftWorldGenerator.biomeTaint.field_76756_M`),
        // never for an Eerie / Magical Forest answer. So this is
        // isTaintBiome, not the three-biome cleansable set used for the gate.
        final Holder<Biome> target = TaintRules.cleansingFallsBackToPlains(
                TaintBiomes.isTaintBiome(generated))
                ? level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)
                : generated;
        TaintBiomes.setBiomeAtAndSync(level, x, z, target);
    }

    /**
     * The neighbour-update death rule of the fibres (original
     * {@code BlockTaintFibres.onNeighborChange}, lines 124-130): a fibres cell
     * whose every neighbour is air or taint is removed.
     */
    public static void fibresNeighborCheck(final ServerLevel level, final BlockPos pos) {
        if (isOnlyAdjacentToTaint(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The original's creative-placement gate ({@code canPlaceBlockAt}): taint biome only. */
    public static boolean canPlaceFibresInBiome(final LevelReader level, final BlockPos pos) {
        return TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, pos));
    }

    // ------------------------------------------------------------- seams

    /**
     * Port of the taint branch of {@code BlockFluxGoo.updateTick}
     * (lines 97-108, gated by {@code general/biome_taint_from_flux}): a goo
     * column of quanta >= 6 with air above, on its 1/50 roll, converts its own
     * column to the taint biome and itself to fibres. The caller is the goo
     * block (the flux resource row's block); quanta and the roll are passed in
     * so the rule is testable.
     */
    public static boolean fluxToTaint(final ServerLevel level, final BlockPos pos,
                                      final int quanta, final int roll) {
        if (!TaintRules.fluxGooConvertsToTaint(quanta, level.getBlockState(pos.above()).isAir(),
                roll, TcConfig.getBool(TcConfigCatalog.BIOME_TAINT_FROM_FLUX))) {
            return false;
        }
        TaintBiomes.setBiomeAtAndSync(level, pos.getX(), pos.getZ(), TaintBiomes.taintBiomeHolder(level));
        setFibres(level, pos, BlockTaintFibres.META_FIBRES);
        return true;
    }

    /**
     * Port of {@code EntityBottleTaint.onImpact}'s world half — the entity
     * itself is TC-M6-06's row and must call this on impact: ten scatter
     * offsets, a coin-flip biome conversion to taint plus fibres on solid
     * ground, and the splash poison on nearby living entities.
     */
    public static void spillBottleTaint(final ServerLevel level, final BlockPos impact,
                                        final RandomSource entityRand) {
        for (final LivingEntity el : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact).inflate(TaintRules.BOTTLE_SPLASH_INFLATE))) {
            if (!(el instanceof ITaintedMob)) {
                applyTaintPoison(el, TaintRules.BOTTLE_POISON_DURATION_TICKS);
            }
        }
        for (int a = 0; a < TaintRules.BOTTLE_SPILL_ATTEMPTS; a++) {
            final int xx = (int) ((entityRand.nextFloat() - entityRand.nextFloat())
                    * TaintRules.BOTTLE_SPILL_SPREAD);
            final int zz = (int) ((entityRand.nextFloat() - entityRand.nextFloat())
                    * TaintRules.BOTTLE_SPILL_SPREAD);
            final BlockPos column = impact.offset(xx, 0, zz);
            if (level.getRandom().nextBoolean() && !TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, column))) {
                TaintBiomes.setBiomeAtAndSync(level, column.getX(), column.getZ(),
                        TaintBiomes.taintBiomeHolder(level));
                final BlockPos below = column.below();
                if (level.getBlockState(below).isSolid()
                        && level.getBlockState(column).canBeReplaced()) {
                    setFibres(level, column, BlockTaintFibres.META_FIBRES);
                }
            }
        }
    }

    /**
     * Applies the Taint Poison effect ({@code potion.fluxtaint}, TC-M6-05's
     * row): false — nothing applied — while the effect is unregistered. The
     * original's potion-immunity guard ({@code func_70662_br}) is subsumed by
     * {@link LivingEntity#addEffect}'s applicability check in 1.21.1.
     */
    public static boolean applyTaintPoison(final Entity entity, final int durationTicks) {
        if (!(entity instanceof LivingEntity living) || entity instanceof ITaintedMob) {
            return false; // taint mobs are healed by taint, never poisoned
        }
        final Holder<MobEffect> effect =
                BuiltInRegistries.MOB_EFFECT.getHolder(TaintBiomes.TAINT_POISON_ID).orElse(null);
        if (effect == null) {
            return false;
        }
        living.addEffect(new MobEffectInstance(effect, durationTicks, 0, false, false));
        return true;
    }

    /**
     * The step/collision poison, verbatim: crust and soil step — player 1/100
     * for 80 ticks, other living 1/20 for 160; fibres collision — player
     * 1/1000 for 80, other living 1/500 for 160. Flesh never poisons (the
     * caller guards it, as the original does).
     */
    public static void stepEffect(final Level level, final Entity entity, final boolean fibres) {
        if (level.isClientSide || !(entity instanceof LivingEntity)) {
            return;
        }
        final boolean player = entity instanceof Player;
        if (level.getRandom().nextInt(TaintRules.stepOdds(player, fibres)) == 0) {
            applyTaintPoison(entity, TaintRules.stepDuration(player));
        }
    }

    /** Places the flux-goo block when it exists; no-op until that row lands. */
    public static boolean convertToFluxGoo(final ServerLevel level, final BlockPos pos) {
        final Block goo = BuiltInRegistries.BLOCK.get(FLUX_GOO_ID);
        if (goo == Blocks.AIR) {
            return false;
        }
        level.setBlock(pos, goo.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    /** Spawns a TC-M6-06 entity once its type is registered; no-op before. */
    public static boolean spawnGuarded(final ServerLevel level, final ResourceLocation entityId,
                                       final net.minecraft.world.phys.Vec3 position) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            return false;
        }
        final Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.setPos(position);
        level.addFreshEntity(entity);
        return true;
    }

    /** How many of {@code entityId} sit inside {@code box}. */
    public static int countEntities(final Level level, final ResourceLocation entityId, final AABB box) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            return 0;
        }
        return level.getEntitiesOfClass(Entity.class, box, e -> e.getType() == type).size();
    }

    // ------------------------------------------------------------- gravity

    /**
     * Port of the original {@code BlockTaint.tryToFall} (lines 258-283): when
     * the cell below the target can be fallen through, the crust becomes the
     * {@code EntityFallingTaint}. While TC-M6-06 has not registered that
     * entity ({@link #FALLING_TAINT_ID}) the state itself moves down one cell:
     * a vanilla gravity block deletes itself on landing, and the original's
     * {@code onFinishFalling} is empty (BlockTaint.java:303) — the crust is
     * not destroyed by falling, and the gametest asserts the TC-M6-06 entity
     * is not spawned today.
     */
    public static boolean tryToFall(final ServerLevel level, final BlockPos pos, final BlockPos target) {
        final BlockState self = level.getBlockState(pos);
        if (!(canFallBelow(level, target.below())
                && TaintRules.canFallAtHeight(target.getY(), level.getMinBuildHeight()))) {
            return false;
        }
        if (spawnGuarded(level, FALLING_TAINT_ID, target.getCenter())) {
            return true;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(target, self, Block.UPDATE_ALL);
        return true;
    }

    /** The original {@code BlockTaint.canFallBelow} support test. */
    public static boolean canFallBelow(final Level level, final BlockPos pos) {
        for (int xx = -TaintRules.FALL_LOG_SCAN_RADIUS; xx <= TaintRules.FALL_LOG_SCAN_RADIUS; xx++) {
            for (int yy = -TaintRules.FALL_LOG_SCAN_RADIUS; yy <= TaintRules.FALL_LOG_SCAN_RADIUS; yy++) {
                for (int zz = -TaintRules.FALL_LOG_SCAN_RADIUS; zz <= TaintRules.FALL_LOG_SCAN_RADIUS; zz++) {
                    if (level.getBlockState(pos.offset(xx, yy, zz)).is(BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }
        final BlockState l = level.getBlockState(pos);
        if (l.isAir()) {
            return true;
        }
        final Block goo = BuiltInRegistries.BLOCK.get(FLUX_GOO_ID);
        if (goo != Blocks.AIR && l.is(goo)) {
            // Original: a goo column of meta >= 4 blocks the fall. The flux
            // row's block is TC-M5-03's and carries its quanta in its own
            // int property; read the first int property it exposes and fall
            // through the test when the row lands without one.
            for (final Property<?> property : l.getBlock().getStateDefinition().getProperties()) {
                if (property instanceof IntegerProperty intProperty) {
                    return !TaintRules.gooBlocksFall(l.getValue(intProperty));
                }
            }
            return false;
        }
        if (l.is(Blocks.SNOW) || l.getBlock() instanceof BlockTaintFibres) {
            return true;
        }
        if (l.canBeReplaced()) {
            return true;
        }
        return l.liquid();
    }

    // ------------------------------------------------------------- probes

    /** Sibling-card presence probes used by the gametest and the seams above. */
    public static boolean fluxGooRegistered() {
        return BuiltInRegistries.BLOCK.containsKey(FLUX_GOO_ID);
    }

    public static boolean entityRegistered(final ResourceLocation entityId) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(entityId);
    }

    public static boolean taintPoisonRegistered() {
        return BuiltInRegistries.MOB_EFFECT.containsKey(TaintBiomes.TAINT_POISON_ID);
    }

    /** The ids this engine reaches for at call time, all owned by sibling cards. */
    public static List<ResourceLocation> guardedDependencies() {
        return List.of(FLUX_GOO_ID, TAINT_SPORE_ID, TAINT_SWARMER_ID, FALLING_TAINT_ID,
                TaintBiomes.TAINT_POISON_ID);
    }

    /** The registry keys this card's own content resolves through (gametest assertion). */
    public static List<ResourceKey<Biome>> ownedBiomes() {
        return List.of(TaintBiomes.TAINT_BIOME);
    }
}
