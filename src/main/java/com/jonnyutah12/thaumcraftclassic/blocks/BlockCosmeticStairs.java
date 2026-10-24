package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * TC cosmetic stairs (content-mapping.csv rows
 * {@code thaumcraft:blockstairsarcanestone|greatwood|silverwood|eldritch}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCosmeticStairs} — four
 * separately registered {@code BlockStairs} instances, each bound to a base
 * block + metadata: arcane stone stairs ← blockCosmeticSolid meta 7, eldritch
 * (ancient stone) stairs ← blockCosmeticSolid meta 11, greatwood/silverwood
 * stairs ← blockWoodenDevice metas 6/7 (planks, TC-M6-04c's row). The base
 * reference only drives the model and the explosion resistance in 1.21.1, so
 * the resistance is pinned here (the original's stairs carried their base
 * material's resistance: rock 30.0 / wood 5.0) and the base state for the
 * wooden stairs resolves through the registry by contract id — with an oak
 * planks stand-in while TC-M6-04c's block is not yet registered (the value
 * only feeds the block codec; behavior is unaffected).</p>
 *
 * <p>Hardness is the original's: the TC stairs never called
 * {@code setBlockHardness}, so they break instantly (0.0F) — ported verbatim
 * rather than "fixed" to vanilla stair values.</p>
 */
public class BlockCosmeticStairs extends StairBlock {

    /** TC-M6-04c's shared device block, referenced by contract id only. */
    private static final ResourceLocation WOODEN_DEVICE_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockwoodendevice");

    private final float resistance;

    public BlockCosmeticStairs(final Supplier<BlockState> baseState, final float resistance) {
        super(baseState, Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.0F, resistance)
                .sound(SoundType.STONE));
        this.resistance = resistance;
    }

    /**
     * {@link StairBlock} delegates resistance to the base block instance; the
     * base may be a registry stand-in (see class javadoc), so answer from the
     * value pinned in the properties instead.
     */
    @Override
    public float getExplosionResistance() {
        return this.resistance;
    }

    /**
     * The base state for the greatwood/silverwood stairs: blockWoodenDevice
     * plank variants (TC-M6-04c), resolved by contract id at construction.
     */
    public static Supplier<BlockState> woodenPlanks(final int variant) {
        return () -> {
            final Block base = BuiltInRegistries.BLOCK.get(WOODEN_DEVICE_ID);
            if (base instanceof VariantDevice
                    && base.getStateDefinition().getProperty(VariantStateItems.VARIANT_PROPERTY)
                            instanceof IntegerProperty property) {
                return base.defaultBlockState().setValue(property, variant);
            }
            return Blocks.OAK_PLANKS.defaultBlockState();
        };
    }
}
