package com.jonnyutah12.thaumcraftclassic.taint;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The taint query seam the taint MONSTERS ask the taint ENGINE through
 * (TC-M6-06-s-e-ii, the taintacle family).
 *
 * <p>Why this exists: the taintacle family's original AI is written against
 * taint world state — "is the block under me taint", "is this the taint biome",
 * "spawn this entity only where taint allows", "convert this ground to taint
 * fibres". That world state is the TC-M5-04-s taint engine, and it is NOT on
 * this branch: its probes are still on
 * {@code wt/taint} / {@code wt/taint-monsters} and carry the documented
 * 12-of-16-cell disc defect, so this card must not link against it. Every
 * taintacle rule is therefore expressed against this seam, and the slice that
 * lands the taint engine installs an implementation with
 * {@link #install(Provider)} — one adapter method per query, each one a
 * straight call to the engine's own API:</p>
 *
 * <pre>{@code
 * TaintHooks.install(new TaintHooks.Provider() {
 *     public boolean isTaintBlock(BlockState s)            { return TaintManager.isTaintBlock(s); }
 *     public boolean isTaintGroundMeta(BlockState s)       { return isFibres(s, BlockTaintFibres.META_FIBRES)
 *                                                                   || isTaint(s, BlockTaint.META_SOIL); }
 *     public boolean isTaintBiome(LevelAccessor l, BlockPos p) { return TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(l, p)); }
 *     public void spawnGuarded(ServerLevel l, ResourceLocation id, Vec3 at) { TaintManager.spawnGuarded(l, id, at); }
 *     public void convertToTaintFibres(ServerLevel l, BlockPos at, int meta) { ... setBiomeAtAndSync + setBlock ... }
 * });
 * }</pre>
 *
 * <p>Until someone installs a provider, the active provider is
 * {@link #NO_TAINT}: every query answers "no taint here" and every write is a
 * no-op. That is not a stub of the original — it is a faithful reading of the
 * world this branch actually ships, which has no taint blocks and no taint
 * biome at all. Consequences, all of them the original's own rules applied to a
 * taint-free world: the taintacle's spawn gate ({@code EntityTaintacle.java:34-51})
 * refuses every cell, the off-taint starve ({@code :116-121}) burns one heart
 * every 20 ticks, and the taintlet burst ({@code :209-238}) returns before it
 * spawns anything. A mob summoned by command still runs the same hand-rolled
 * AI, and the world probes in {@code TaintacleGameTest} prove the seam is
 * consulted by installing a stub provider and watching the answers flip.</p>
 */
public final class TaintHooks {

    /**
     * The queries the taintacle family needs. Each one mirrors a method the
     * taint engine already exposes so the install adapter is a one-liner.
     */
    public interface Provider {

        /**
         * Is this block any taint surface? The original's test is
         * {@code EntityTaintacle.java:216-218}
         * ({@code BlockUtils.isBlockTaint} on the target cell and the cell below).
         */
        boolean isTaintBlock(BlockState state);

        /**
         * The spawn floor: taint fibres meta 0 (Fibrous Taint) or taint meta 1
         * (Tainted Soil) — {@code EntityTaintacle.java:46-50}. Meta semantics
         * belong to the engine's block classes, which is why this is a query and
         * not a constant.
         */
        boolean isTaintGroundMeta(BlockState state);

        /**
         * Is the biome at this column the taint biome?
         * {@code EntityTaintacle.java:39} ({@code world.getBiomeGenForCoords}
         * compared against {@code ThaumcraftWorldGenerator.biomeTaint}).
         */
        boolean isTaintBiome(LevelAccessor level, BlockPos pos);

        /**
         * Spawn {@code entityId} at {@code at} through the engine's spawn guard
         * ({@code EntityTaintacle.java:221-229}). The guard is the engine's, so
         * the taintlet inherits whatever placement rules taint ships.
         */
        void spawnGuarded(ServerLevel level, ResourceLocation entityId, Vec3 at);

        /**
         * The eldritch-lands branch of the burst: write the taint biome at this
         * column and place taint fibres with {@code fibresMeta}
         * ({@code EntityTaintacle.java:231-236} — {@code Utils.setBiomeAt} plus
         * {@code world.setBlock} with {@code Block.UPDATE_ALL}).
         */
        void convertToTaintFibres(ServerLevel level, BlockPos at, int fibresMeta);
    }

    /**
     * The default: a world with no taint in it, which is exactly the world this
     * branch ships (docs/content-mapping.csv rows {@code thaumcraft:blocktaint}
     * and {@code thaumcraft:blocktaintfibres} are both {@code not_started}).
     */
    public static final Provider NO_TAINT = new Provider() {
        @Override
        public boolean isTaintBlock(final BlockState state) {
            return false;
        }

        @Override
        public boolean isTaintGroundMeta(final BlockState state) {
            return false;
        }

        @Override
        public boolean isTaintBiome(final LevelAccessor level, final BlockPos pos) {
            return false;
        }

        @Override
        public void spawnGuarded(final ServerLevel level, final ResourceLocation entityId, final Vec3 at) {
            // No engine, no spawn. The original spawns a taintlet only on taint
            // ground in the taint biome, and this world has neither.
        }

        @Override
        public void convertToTaintFibres(final ServerLevel level, final BlockPos at, final int fibresMeta) {
            // No engine, no biome write, no fibres placement.
        }

        @Override
        public String toString() {
            return "TaintHooks.NO_TAINT";
        }
    };

    private static volatile Provider provider = NO_TAINT;

    private TaintHooks() {
    }

    /** Install the engine's answers; the taint slice calls this from its holder's {@code init}. */
    public static void install(final Provider next) {
        provider = Objects.requireNonNull(next, "provider");
    }

    /** Restore {@link #NO_TAINT} — the reset half of a probe that installed a stub. */
    public static Provider reset() {
        final Provider previous = provider;
        provider = NO_TAINT;
        return previous;
    }

    /** The provider answers right now (probes assert on this so the seam is never silently inert). */
    public static Provider current() {
        return provider;
    }

    public static boolean isTaintBlock(final BlockState state) {
        return provider.isTaintBlock(state);
    }

    public static boolean isTaintGroundMeta(final BlockState state) {
        return provider.isTaintGroundMeta(state);
    }

    public static boolean isTaintBiome(final LevelAccessor level, final BlockPos pos) {
        return provider.isTaintBiome(level, pos);
    }

    public static void spawnGuarded(final ServerLevel level, final ResourceLocation entityId, final Vec3 at) {
        provider.spawnGuarded(level, entityId, at);
    }

    public static void convertToTaintFibres(final ServerLevel level, final BlockPos at, final int fibresMeta) {
        provider.convertToTaintFibres(level, at, fibresMeta);
    }
}
