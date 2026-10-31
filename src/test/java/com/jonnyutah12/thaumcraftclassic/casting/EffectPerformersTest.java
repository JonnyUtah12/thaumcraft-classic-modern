package com.jonnyutah12.thaumcraftclassic.casting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-03-p acceptance: the performer registration surface and the pure
 * geometry the ward/swap handlers walk — the original's flood bounds and the
 * area limit per pointed face ({@code ItemFocusWarding.checkNeighbours}
 * axis mapping, {@code ItemFocusTrade.CollectBlocks}).
 *
 * <p>The registry-dependent behaviours (entity gates, warding, the dig, the
 * hole's refund) are world-side and pinned live by
 * {@code gametest/FociPerformersGameTest}; this suite pins the parts that
 * are pure functions of positions, so a broken walk fails in milliseconds.</p>
 */
class EffectPerformersTest {

    /** Every effect key the card registers gets a real performer. */
    @Test
    void everyEffectKeyHasAPerformer() {
        FocusSpells.registerAll();
        EffectPerformers.registerAll();
        assertEquals(11, EffectPerformers.KEYS.size());
        for (final String key : EffectPerformers.KEYS) {
            assertTrue(SpellEffects.isRegistered(key), "no performer registered for " + key);
        }
        // The bolt is SpellEffects' native dispatch arm (not a registry key),
        // implemented by the parent card; the eleven content keys are here.
        // The projectile keys are the content-mapping entity ids, not prose.
        for (final String key : List.of(SpellEffect.Projectile.EMBER, SpellEffect.Projectile.EXPLOSIVE_ORB,
                SpellEffect.Projectile.SHOCK_ORB, SpellEffect.Projectile.FROST_SHARD,
                SpellEffect.Projectile.PECH_BLAST, SpellEffect.Projectile.PRIMAL_ORB,
                SpellEffect.Projectile.FIRE_BAT)) {
            assertTrue(key.startsWith("thaumcraft:"), key);
        }
    }

    /** GUI area 1 (extent 0) walks the pointed block alone, on every face. */
    @Test
    void aZeroExtentAreaIsTheCentreAlone() {
        final BlockPos centre = new BlockPos(3, 4, 5);
        for (final Direction side : Direction.values()) {
            final List<BlockPos> walked = EffectPerformers.flood(centre, side, 0, pos -> true);
            assertEquals(List.of(centre), walked, "area 1 on side " + side);
        }
    }

    /** Area 3 (extent 1) is the 3x3x3 cube around the centre — all accepted. */
    @Test
    void anAreaWalksTheBoundedCube() {
        final BlockPos centre = BlockPos.ZERO;
        final List<BlockPos> walked = EffectPerformers.flood(centre, Direction.UP, 1, pos -> true);
        assertEquals(27, walked.size());
        assertEquals(27, new HashSet<>(walked).size(), "no position collected twice");
        assertTrue(EffectPerformers.withinLimit(centre, new BlockPos(1, 1, 1), Direction.UP, 1));
        assertFalse(EffectPerformers.withinLimit(centre, new BlockPos(2, 0, 0), Direction.UP, 1));
    }

    /** A wall of refused positions caps the walk — the original's cut-off. */
    @Test
    void theWalkStopsAtRefusedPositions() {
        final BlockPos centre = BlockPos.ZERO;
        // Refuse everything except the centre's +X chain of two.
        final List<BlockPos> walked = EffectPerformers.flood(centre, Direction.NORTH, 3,
                pos -> pos.equals(centre)
                        || pos.equals(centre.relative(Direction.EAST))
                        || pos.equals(centre.relative(Direction.EAST).relative(Direction.EAST)));
        assertEquals(3, walked.size());
        assertTrue(walked.contains(centre.relative(Direction.EAST).relative(Direction.EAST)));
    }

    /** The walk terminates: accepting the whole world cannot loop forever. */
    @Test
    void theWalkIsFiniteOverAnOpenVolume() {
        final List<BlockPos> walked = EffectPerformers.flood(new BlockPos(10, 10, 10), Direction.EAST, 2,
                pos -> true);
        assertEquals(125, walked.size());
    }

    /** The dig state tracks the pointed block: the original zeroes bc per target. */
    @Test
    void theDigStateKeyedOnTheBlock() {
        final DigState dig = new DigState(new BlockPos(1, 2, 3), 0.25F);
        assertTrue(dig.isAt(new BlockPos(1, 2, 3)));
        assertFalse(dig.isAt(new BlockPos(1, 2, 4)));
    }

    /**
     * The original's soft set is {@code Material.rock}, {@code grass},
     * {@code ground} and {@code sand} ({@code ItemFocusExcavation.java:179-183});
     * the port proxies it with the break-sound group, so a block outside those
     * four families digs at the slow rate.
     */
    @Test
    void theSoftMaterialSetIsTheOriginalsFourFamilies() {
        final BlockState stone = Blocks.STONE.defaultBlockState();
        final BlockState grassBlock = Blocks.GRASS_BLOCK.defaultBlockState();
        final BlockState dirt = Blocks.DIRT.defaultBlockState();
        final BlockState sand = Blocks.SAND.defaultBlockState();
        assertTrue(EffectPerformers.isSoftMaterial(stone), "Material.rock");
        assertTrue(EffectPerformers.isSoftMaterial(grassBlock), "Material.grass");
        assertTrue(EffectPerformers.isSoftMaterial(dirt), "Material.ground");
        assertTrue(EffectPerformers.isSoftMaterial(sand), "Material.sand");
        assertFalse(EffectPerformers.isSoftMaterial(Blocks.OAK_PLANKS.defaultBlockState()), "Material.wood");
        assertFalse(EffectPerformers.isSoftMaterial(Blocks.GLASS.defaultBlockState()), "Material.glass");
    }

    /**
     * The rates are the original's three ({@code :175-186}): the slow
     * {@code 0.05F + pot * 0.1F}, the soft-set {@code 0.25F + pot * 0.25F}, and
     * obsidian at triple whatever rate its material earns.
     */
    @Test
    void theDigSpeedIsTheOriginalsThreeRates() {
        final SpellEffect.Excavate spec = new SpellEffect.Excavate(0.05F, 0.25F, false, false, 0, 0);
        final BlockPos pos = BlockPos.ZERO;
        assertEquals(0.25F, EffectPerformers.digSpeed(Blocks.DIRT.defaultBlockState(), spec, null, pos), 1e-6F,
                "soft dirt digs at softSpeed");
        assertEquals(0.05F, EffectPerformers.digSpeed(Blocks.OAK_PLANKS.defaultBlockState(), spec, null, pos), 1e-6F,
                "a material outside the soft set digs at speed");
        assertEquals(0.75F, EffectPerformers.digSpeed(Blocks.OBSIDIAN.defaultBlockState(), spec, null, pos), 1e-6F,
                "obsidian is the soft rate times three (its material is rock, :184-186)");
    }
}
