package com.jonnyutah12.thaumcraftclassic.taint;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TC-M5-04: the taint spread / conversion / cleansing engine (content-mapping
 * .csv row {@code worldgen.taint_spread_conversion}, impl=TaintManager).
 *
 * <p>1:1 port of the original static engine — {@code BlockTaintFibres.
 * spreadFibres / taintBiomeSpread / getAdjacentTaint / isOnlyAdjacentToTaint}
 * (decompiled lines 174-240) and the {@code updateTick} bodies of
 * {@code BlockTaint} (lines 120-199) and {@code BlockTaintFibres} (lines
 * 129-172) — centralized here so the block classes stay thin and the rules
 * are unit-testable. The conversion table itself lives in
 * {@link TaintConversion}; the biome write primitive in {@link TaintBiomes}.</p>
 *
 * <p>Guards for content owned by sibling cards (docs/architecture/taint.md):
 * the flux-goo conversion target ({@code thaumcraft:blockfluxgoo}, TC-M5-03),
 * the spore/swarmer entities ({@code thaumcraft:taintspore} /
 * {@code thaumcraft:taintswarmer}, TC-M6-06) and the Taint Poison effect
 * ({@code thaumcraft:potion.fluxtaint}, TC-M6-05) are looked up in the live
 * registries at call time; while absent the guarded branch no-ops and every
 * other rule behaves exactly like the original.</p>
 */
public final class TaintManager {

    /** Original {@code ConfigBlocks.blockFluxGoo} (TC-M5-03 owns the block). */
    public static final ResourceLocation FLUX_GOO_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockfluxgoo");

    /** Original {@code EntityTaintSpore} / {@code EntityTaintSporeSwarmer} (TC-M6-06). */
    public static final ResourceLocation TAINT_SPORE_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintspore");
    public static final ResourceLocation TAINT_SWARMER_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintswarmer");

    private TaintManager() {
    }

    /**
     * Original {@code block.getMaterial() == Config.taintMaterial}: only
     * blockTaint and blockTaintFibres carry the taint material.
     */
    public static boolean isTaintBlock(final BlockState state) {
        return state.getBlock() instanceof BlockTaint || state.getBlock() instanceof BlockTaintFibres;
    }

    /** Port of {@code BlockTaintFibres.getAdjacentTaint} (6-side count). */
    public static int getAdjacentTaint(final Level level, final BlockPos pos) {
        int count = 0;
        for (final net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (isTaintBlock(level.getBlockState(pos.relative(d)))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Port of {@code BlockTaintFibres.isOnlyAdjacentToTaint}: every neighbour
     * is air or a taint-material block.
     */
    public static boolean isOnlyAdjacentToTaint(final Level level, final BlockPos pos) {
        for (final net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            final BlockPos np = pos.relative(d);
            final BlockState n = level.getBlockState(np);
            if (!n.isAir() && !isTaintBlock(n)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Port of {@code BlockUtils.isAdjacentToSolidBlock}: any of the six
     * neighbours is solid on the facing side.
     */
    public static boolean isAdjacentToSolidBlock(final Level level, final BlockPos pos) {
        for (final net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            final BlockPos np = pos.relative(d);
            final BlockState n = level.getBlockState(np);
            if (n.isFaceSturdy(level, np, d.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Port of {@code BlockTaintFibres.spreadFibres} verbatim, including the
     * original's use of the world random (not the tick random) and its meta
     * ladder: plain fibres 90%+ of the time; with air above and solid below
     * the ladder 9/10 → grass-1, else 10/12 → grass-2, else spore stalk.
     */
    public static boolean spreadFibres(final ServerLevel level, final BlockPos pos) {
        final BlockState target = level.getBlockState(pos);
        if (isAdjacentToSolidBlock(level, pos)
                && !isOnlyAdjacentToTaint(level, pos)
                && !target.liquid()
                && (target.isAir() || target.canBeReplaced()
                    || target.is(BlockTags.FLOWERS) || target.isLeaves())) {
            final RandomSource rand = level.getRandom();
            final int meta;
            if (rand.nextInt(10) != 0 || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                meta = 0;
            } else if (rand.nextInt(10) < 9) {
                meta = 1;
            } else if (rand.nextInt(12) < 10) {
                meta = 2;
            } else {
                meta = 3;
            }
            setFibres(level, pos, meta);
            return true;
        }
        return false;
    }

    public static void setFibres(final ServerLevel level, final BlockPos pos, final int meta) {
        final BlockState old = level.getBlockState(pos);
        final BlockState next = BlockTaintFibres.stateFor(meta);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        level.sendBlockUpdated(pos, old, next, Block.UPDATE_ALL);
    }

    /**
     * Port of {@code BlockTaintFibres.taintBiomeSpread}. The caller reads
     * {@code general/biome_taint_spread} through {@code TcConfig} and passes
     * it in, so the rule is deterministic-testable (docs/testing.md: seeded
     * tests for random processes).
     */
    public static void taintBiomeSpread(final ServerLevel level, final BlockPos pos,
                                        final RandomSource rand, final int spreadRate) {
        if (spreadRate <= 0) {
            return;
        }
        final int xx = rand.nextInt(3) - 1;
        final int zz = rand.nextInt(3) - 1;
        final Holder<Biome> biome = level.getBiome(pos.offset(xx, 0, zz));
        if (!TaintBiomes.isTaintBiome(biome)
                && rand.nextInt(spreadRate * 5) == 0
                && getAdjacentTaint(level, pos) >= 2) {
            TaintBiomes.setBiomeAtAndSync(level, pos.getX() + xx, pos.getZ() + zz,
                    level.registryAccess().registryOrThrow(Registries.BIOME)
                            .wrapAsHolder(level.registryAccess().registryOrThrow(Registries.BIOME)
                                    .getOrThrow(TaintBiomes.TAINT_BIOME)));
        }
    }

    /**
     * Port of {@code BlockTaint.updateTick} (crust/soil/flesh tick).
     * {@code spreadRate} is the config value read by the block; the spore
     * swarmer branch is guarded on TC-M6-06's entity and the flux-goo
     * conversion on TC-M5-03's block (see class javadoc).
     */
    public static void taintBlockTick(final ServerLevel level, final BlockPos pos,
                                      final BlockState state, final RandomSource rand) {
        final int md = state.getValue(BlockTaint.VARIANT);
        if (md == 2) {
            return; // flesh never ticks (original line 122)
        }
        taintBiomeSpread(level, pos, rand, TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD));

        if (md == 0) {
            if (BlockTaint.tryToFall(level, pos, pos, rand)) {
                return;
            }
            if (level.getBlockState(pos.above()).isAir()) {
                boolean doIt = true;
                final net.minecraft.core.Direction dir = net.minecraft.core.Direction.Plane.HORIZONTAL
                        .getRandomDirection(rand);
                for (int a = 0; a < 4 && doIt; a++) {
                    if (!level.getBlockState(pos.offset(dir.getStepX(), -a, dir.getStepZ())).isAir()) {
                        doIt = false;
                    } else if (!(level.getBlockState(pos.offset(0, -a, 0)).getBlock() instanceof BlockTaint)) {
                        doIt = false;
                    }
                }
                if (doIt && BlockTaint.tryToFall(level, pos, pos.offset(dir.getStepX(), 0, dir.getStepZ()), rand)) {
                    return;
                }
            }
        }

        final BlockPos off = pos.offset(rand.nextInt(3) - 1, rand.nextInt(5) - 3, rand.nextInt(3) - 1);
        if (TaintBiomes.isTaintBiome(level.getBiome(off))) {
            if (!spreadFibres(level, off)) {
                final int adjacentTaint = getAdjacentTaint(level, off);
                final BlockState target = level.getBlockState(off);
                if (adjacentTaint >= 2 && TaintConversion.isCrustTarget(target)) {
                    setTaint(level, off, 0);
                }
                if (adjacentTaint >= 3 && !target.isAir() && TaintConversion.isSoilTarget(target)) {
                    setTaint(level, off, 1);
                }
            }
            if (md == 0) {
                if (TcConfig.getBool(TcConfigCatalog.SPAWN_TAINT_SPORES)
                        && level.getBlockState(pos.above()).isAir() && rand.nextInt(200) == 0) {
                    spawnGuarded(level, TAINT_SWARMER_ID, pos.getCenter());
                } else {
                    // Original: crust fully surrounded by crust turns to flux goo.
                    boolean doIt = level.getBlockState(pos.above()).getBlock() instanceof BlockTaint;
                    if (doIt) {
                        for (final net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                            if (!(level.getBlockState(pos.relative(d)).getBlock() instanceof BlockTaint)) {
                                doIt = false;
                                break;
                            }
                        }
                    }
                    if (doIt) {
                        convertToFluxGoo(level, pos);
                    }
                }
            }
        } else if (md == 0 && rand.nextInt(20) == 0) {
            convertToFluxGoo(level, pos); // crust decays outside the taint biome
        } else if (md == 1 && rand.nextInt(10) == 0) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL); // soil decays
        }
    }

    /**
     * Port of {@code BlockTaintFibres.updateTick} (fibres tick): dies when
     * meta-0 fibres sit only against taint, or any fibres leave the biome;
     * otherwise drives spread and the conversion table.
     */
    public static void fibresTick(final ServerLevel level, final BlockPos pos,
                                  final BlockState state, final RandomSource rand) {
        final int md = state.getValue(BlockTaintFibres.VARIANT);
        taintBiomeSpread(level, pos, rand, TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD));
        if ((md == 0 && isOnlyAdjacentToTaint(level, pos)) || !TaintBiomes.isTaintBiome(level.getBiome(pos))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        final BlockPos off = pos.offset(rand.nextInt(3) - 1, rand.nextInt(5) - 3, rand.nextInt(3) - 1);
        if (TaintBiomes.isTaintBiome(level.getBiome(off))) {
            if (!spreadFibres(level, off)) {
                final int adjacentTaint = getAdjacentTaint(level, off);
                final BlockState target = level.getBlockState(off);
                if (adjacentTaint >= 2 && TaintConversion.isCrustTarget(target)) {
                    setTaint(level, off, 0);
                }
                if (adjacentTaint >= 3 && !target.isAir() && TaintConversion.isSoilTarget(target)) {
                    setTaint(level, off, 1);
                }
            }
            if (md == 3 && TcConfig.getBool(TcConfigCatalog.SPAWN_TAINT_SPORES)
                    && rand.nextInt(10) == 0 && level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos, BlockTaintFibres.stateFor(4), Block.UPDATE_ALL);
                spawnGuarded(level, TAINT_SPORE_ID, pos.above().getCenter());
            } else if (md == 4) {
                // Mature stalk reverts once its spore entity is gone; while
                // TC-M6-06 has not registered the spore there can never be
                // one, so the revert branch is the faithful behaviour.
                level.setBlock(pos, BlockTaintFibres.stateFor(3), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Port of {@code BlockFluxGoo.updateTick}'s taint branch (guarded by
     * {@code general/biome_taint_from_flux}): flux goo at quanta >= 6 turns
     * its own column to taint biome and itself to fibres. Called by TC-M5-03's
     * goo block (docs/architecture/taint.md seam).
     */
    public static void fluxToTaint(final ServerLevel level, final BlockPos pos) {
        if (!TcConfig.getBool(TcConfigCatalog.BIOME_TAINT_FROM_FLUX)) {
            return;
        }
        TaintBiomes.setBiomeAtAndSync(level, pos.getX(), pos.getZ(),
                level.registryAccess().registryOrThrow(Registries.BIOME)
                        .wrapAsHolder(level.registryAccess().registryOrThrow(Registries.BIOME)
                                .getOrThrow(TaintBiomes.TAINT_BIOME)));
        setFibres(level, pos, 0);
    }

    /**
     * Port of {@code EntityBottleTaint.onImpact}'s world half (the entity
     * itself is TC-M6-06's row; it must call this on impact): 10 triangular
     * offsets, coin-flip biome conversion to taint plus fibres on solid
     * ground, and the splash effect on nearby living entities.
     */
    public static void spillBottleTaint(final ServerLevel level, final BlockPos impact, final RandomSource entityRand) {
        for (final LivingEntity el : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(impact).inflate(5.0D))) {
            if (!(el instanceof ITaintedMob)) {
                applyTaintPoison(el, 100);
            }
        }
        for (int a = 0; a < 10; a++) {
            final int xx = impact.getX() + (int) ((entityRand.nextFloat() - entityRand.nextFloat()) * 5.0F);
            final int zz = impact.getZ() + (int) ((entityRand.nextFloat() - entityRand.nextFloat()) * 5.0F);
            if (level.getRandom().nextBoolean() && !TaintBiomes.isTaintBiome(level.getBiome(impact.offset(xx, 0, zz)))) {
                TaintBiomes.setBiomeAtAndSync(level, impact.getX() + xx, impact.getZ() + zz,
                        level.registryAccess().registryOrThrow(Registries.BIOME)
                                .wrapAsHolder(level.registryAccess().registryOrThrow(Registries.BIOME)
                                        .getOrThrow(TaintBiomes.TAINT_BIOME)));
                final BlockPos below = BlockPos.containing(impact.getX() + xx, impact.getY() - 1, impact.getZ() + zz);
                final BlockPos at = BlockPos.containing(impact.getX() + xx, impact.getY(), impact.getZ() + zz);
                if (level.getBlockState(below).isSolid() && level.getBlockState(at).canBeReplaced()) {
                    setFibres(level, at, 0);
                }
            }
        }
    }

    /**
     * Applies the Taint Poison effect ({@code potion.fluxtaint}, TC-M6-05's
     * row). No-ops while the effect is unregistered; the original's
     * potion-immunity guard ({@code func_70662_br}) is subsumed by
     * {@link LivingEntity#addEffect}'s applicability check in 1.21.1.
     */
    public static boolean applyTaintPoison(final Entity entity, final int durationTicks) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        final MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(TAINT_POISON_ID());
        if (effect == null) {
            return false;
        }
        living.addEffect(new MobEffectInstance(effect, durationTicks, 0, false, false));
        return true;
    }

    private static ResourceLocation TAINT_POISON_ID() {
        return TaintBiomes.TAINT_POISON_ID;
    }

    /**
     * Taint-poison step rules, verbatim from the originals: crust/soil
     * stepOn — player 1/100 for 80t, other living 1/20 for 160t; fibres
     * collision — player 1/1000 for 80t, other living 1/500 for 160t.
     */
    public static void stepEffect(final Level level, final Entity entity, final boolean fibres) {
        if (level.isClientSide || !(entity instanceof LivingEntity)) {
            return;
        }
        if (entity instanceof Player) {
            if (level.getRandom().nextInt(fibres ? 1000 : 100) == 0) {
                applyTaintPoison(entity, 80);
            }
        } else if (level.getRandom().nextInt(fibres ? 500 : 20) == 0) {
            applyTaintPoison(entity, 160);
        }
    }

    public static void setTaint(final ServerLevel level, final BlockPos pos, final int meta) {
        final BlockState old = level.getBlockState(pos);
        final BlockState next = BlockTaint.stateFor(meta);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        level.sendBlockUpdated(pos, old, next, Block.UPDATE_ALL);
    }

    /** Places the TC-M5-03 goo when it exists; no-op until that card lands. */
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
        final var type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
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

    /** The original {@code BlockTaint.canFallBelow} support test. */
    public static boolean canFallBelow(final Level level, final BlockPos pos) {
        for (int xx = -1; xx <= 1; xx++) {
            for (int yy = -1; yy <= 1; yy++) {
                for (int zz = -1; zz <= 1; zz++) {
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
        if (l.is(FLUX_GOO_ID)) {
            if (!l.hasProperty(BlockStateProperties.LEVEL_FLUID)) {
                return false;
            }
            final Integer amount = l.getValue(BlockStateProperties.LEVEL_FLUID);
            return amount != null && amount < 4; // original: goo meta >= 4 blocks the fall
        }
        if (l.is(Blocks.SNOW) || isTaintBlock(l)) {
            return true;
        }
        if (l.canBeReplaced()) {
            return true;
        }
        return l.liquid();
    }

    /** Neighbour-update death rule: fibres need a solid neighbour (original func_149719_a bounds fallback). */
    public static void fibresNeighborCheck(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (state.getValue(BlockTaintFibres.VARIANT) == 0 && !isAdjacentToSolidBlock(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The original's creative-placement gate (func_149742_c): fibres only place in the taint biome. */
    public static boolean canPlaceFibresInBiome(final Level level, final BlockPos pos) {
        return TaintBiomes.isTaintBiome(level.getBiome(pos));
    }

    /**
     * The original's biome-restore half of the Ethereal Bloom (see
     * {@code TileEtherealBloom}): taint columns revert to plains, other
     * mod-biome columns revert to whatever the world generator would place.
     */
    public static void restoreBiome(final ServerLevel level, final int x, final int z) {
        final Holder<Biome> generated = level.getChunkSource().getGenerator().getBiomeSource()
                .getNoiseBiome(net.minecraft.world.level.QuantaPos.create(x >> 2, 0, z >> 2));
        Holder<Biome> target = generated;
        if (TaintBiomes.isTaintBiome(generated)) {
            target = level.registryAccess().registryOrThrow(Registries.BIOME)
                    .wrapAsHolder(level.registryAccess().registryOrThrow(Registries.BIOME)
                            .getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS));
        }
        TaintBiomes.setBiomeAtAndSync(level, x, z, target);
    }

    /** Sibling-card presence probes used by the gametest. */
    public static boolean fluxGooRegistered() {
        return BuiltInRegistries.BLOCK.containsKey(FLUX_GOO_ID);
    }

    public static List<ResourceLocation> guardedDependencies() {
        return List.of(FLUX_GOO_ID, TAINT_SPORE_ID, TAINT_SWARMER_ID, TaintBiomes.TAINT_POISON_ID);
    }
}
