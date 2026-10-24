package com.jonnyutah12.thaumcraftclassic.taint;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TC-M5-04: the exact original taint conversion table (content-mapping.csv
 * row {@code worldgen.taint_spread_conversion}, impl=TaintManager).
 *
 * <p>The original keyed the table on 1.7.10 {@code Material} values inside
 * {@code BlockTaintFibres.updateTick} (decompiled lines 158-171):</p>
 * <ul>
 *   <li>{@code adjacentTaint >= 2 && (Utils.isWoodLog || material == gourd
 *       || material == cactus)} → {@code blockTaint} meta 0 (crust).
 *       MCP: {@code field_151572_C=gourd}, {@code field_151570_A=cactus}.</li>
 *   <li>{@code adjacentTaint >= 3 && block != air && (material == sand ||
 *       ground || grass || clay)} → {@code blockTaint} meta 1 (soil).
 *       MCP: {@code field_151595_p=sand}, {@code field_151578_c=ground},
 *       {@code field_151577_b=grass}, {@code field_151571_B=clay}.</li>
 * </ul>
 *
 * <p>1.21.1 has no {@code Material}; the categories are reproduced as block
 * sets. Vanilla tags are used where they exactly match a 1.7.10 material
 * ({@code #minecraft:logs} ≡ {@code isWoodLog} (canSustainLeaves ∪ oredict
 * logWood); {@code #minecraft:dirt} ≡ the ground+grass material members that
 * exist in both versions). Blocks added after 1.7.10 whose material is
 * semantically ground/grass (coarse_dirt, rooted_dirt, mud,
 * muddy_mangrove_roots, dirt_path) are included — they are the modern
 * instances of the same original categories (docs/architecture/taint.md).</p>
 */
public final class TaintConversion {

    private TaintConversion() {
    }

    /**
     * Original crust targets: {@code Utils.isWoodLog} (canSustainLeaves or
     * oredict logWood) or Material.gourd or Material.cactus.
     */
    public static boolean isCrustTarget(final BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.CARVED_PUMPKIN)
                || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.MELON)
                || state.is(Blocks.PUMPKIN_STEM)
                || state.is(Blocks.MELON_STEM)
                || state.is(Blocks.ATTACHED_PUMPKIN_STEM)
                || state.is(Blocks.ATTACHED_MELON_STEM)
                || state.is(Blocks.CACTUS);
    }

    /**
     * Original soil targets: Material.sand, Material.ground, Material.grass
     * or Material.clay, and never air (the caller checks air separately,
     * exactly like the original's {@code bi != Blocks.air} guard).
     */
    public static boolean isSoilTarget(final BlockState state) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH)
                || state.is(BlockTags.DIRT); // dirt, grass_block, mycelium, podzol, coarse_dirt, rooted_dirt, mud, muddy_mangrove_roots
    }
}
