package com.jonnyutah12.thaumcraftclassic.taint;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * TC-M5-04-s: the taint conversion table — which target blocks the original
 * converted, and into what (content-mapping.csv row
 * {@code worldgen.taint_spread_conversion}, the rule that
 * {@code BlockTaintFibres.updateTick} applies at its lines 158-171).
 *
 * <p>The original keyed the table on 1.7.10 {@code Material} values:</p>
 * <ul>
 *   <li>crust (meta 0): {@code Utils.isWoodLog} (canSustainLeaves ∪ oredict
 *       logWood) ∪ {@code Material.gourd} ∪ {@code Material.cactus};</li>
 *   <li>soil (meta 1): {@code Material.sand} ∪ {@code Material.ground} ∪
 *       {@code Material.grass} ∪ {@code Material.clay}, never air.</li>
 * </ul>
 *
 * <p>1.21.1 has no {@code Material}, so each 1.7.10 category becomes a tag
 * where a vanilla tag matches it exactly plus an explicit block list for the
 * members that have no tag:</p>
 * <ul>
 *   <li>{@code isWoodLog} ≡ {@code #minecraft:logs} (the oredict logWood set
 *       is the modern logs tag, D-012 tag mapping);</li>
 *   <li>{@code Material.gourd} = the pumpkin/melon family and their stems,
 *       {@code Material.cactus} = cactus: no vanilla tag covers them, so they
 *       are listed ({@link #CRUST_BLOCKS});</li>
 *   <li>{@code Material.ground} / {@code Material.grass} ≡ the members of
 *       {@code #minecraft:dirt} that existed in 1.7.10 (dirt, grass_block,
 *       mycelium, podzos) — the tag also carries the blocks added after 1.7.10
 *       (coarse_dirt, rooted_dirt, mud, muddy_mangrove_roots), which are
 *       modern instances of the same ground category (the deviation note in
 *       docs/architecture/taint.md, BD-14);</li>
 *   <li>{@code Material.sand} = sand + red sand, {@code Material.clay} = clay
 *       block; {@code BlockFarmland} is {@code Material.ground} in the
 *       original, and 1.21.1 puts farmland in the dirt tag.</li>
 * </ul>
 *
 * <p>The tag membership itself cannot be asserted in the unit suite (the unit
 * JVM has no loaded tags — {@code Blocks.OAK_LOG.is(BlockTags.LOGS)} is false
 * there), so the sets are exposed for composition tests
 * ({@code TaintConversionTableTest}) and the live tag semantics are proven in
 * the world by {@code TaintGameTest.taintFibresConvertNeighboursByThreshold}.</p>
 */
public final class TaintConversion {

    /** 1.7.10 {@code Utils.isWoodLog} ≡ this tag. */
    public static final TagKey<Block> CRUST_TAG = BlockTags.LOGS;
    /** 1.7.10 {@code Material.ground} + {@code Material.grass} ≡ this tag. */
    public static final TagKey<Block> SOIL_TAG = BlockTags.DIRT;

    /** {@code Material.gourd} (pumpkins, melons, their stems) + {@code Material.cactus}. */
    public static final List<Block> CRUST_BLOCKS = List.of(
            Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN, Blocks.MELON,
            Blocks.PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM,
            Blocks.ATTACHED_MELON_STEM, Blocks.CACTUS);

    /** {@code Material.sand} + {@code Material.clay}; ground/grass ride the dirt tag. */
    public static final List<Block> SOIL_BLOCKS = List.of(
            Blocks.SAND, Blocks.RED_SAND, Blocks.CLAY);

    private TaintConversion() {
    }

    /** Crust target: a log, a gourd, or a cactus (original line 162). */
    public static boolean isCrustTarget(final BlockState state) {
        return state.is(CRUST_TAG) || isOneOf(state, CRUST_BLOCKS);
    }

    /** Soil target: sand, clay, or the dirt-tag family (original lines 166-167). */
    public static boolean isSoilTarget(final BlockState state) {
        return state.is(SOIL_TAG) || isOneOf(state, SOIL_BLOCKS);
    }

    /** {@code BlockState#is} has no collection overload (1.21.1): test the list. */
    private static boolean isOneOf(final BlockState state, final List<Block> blocks) {
        for (final Block block : blocks) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }
}
