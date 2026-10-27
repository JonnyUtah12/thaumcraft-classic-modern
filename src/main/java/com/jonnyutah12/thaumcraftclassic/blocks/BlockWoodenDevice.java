package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * Wooden device (content-mapping.csv {@code thaumcraft:blockwoodendevice}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockWoodenDevice} — one block
 * id, metadata 0-8. Physical properties are the original's: wood material,
 * hardness 2.5, resistance 10.0, wood step sound. Meta map per
 * docs/catalog-triage.md: 0 bellows, 1 sensor (arcane ear), 2-3 arcane
 * pressure plate, 4 bore base, 5 bore, 6 greatwood planks, 7 silverwood
 * planks, 8 banner (TileBanner is TC-M6-04d's, supplied through
 * {@link DeviceTileFactory}).</p>
 *
 * <p>Flammability per the original: the plank metas (6, 7) are flammable
 * (20/5), everything else is not — implemented via the {@code neoforge:flammable}
 * block property on the plank variants is not state-aware in 1.21.1, so the
 * fire behaviour is carried by {@link #isFlammablePlank} and wired into the
 * fire tick helper (see {@code BlockDeviceFire}); until that lands the
 * planks are not fire-spreadable (noted in the handoff).</p>
 */
public class BlockWoodenDevice extends BlockDeviceBase {

    public static final int MAX_VARIANT = 8;

    public static final int VARIANT_PLANKS_GREATWOOD = 6;
    public static final int VARIANT_PLANKS_SILVERWOOD = 7;
    public static final int VARIANT_BANNER = 8;

    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    public BlockWoodenDevice() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 10.0F)
                .sound(SoundType.WOOD));
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /** Original getFlammability/getFireSpreadSpeed: only the plank metas burn. */
    public boolean isFlammablePlank(final net.minecraft.world.level.block.state.BlockState state) {
        final int variant = variant(state);
        return variant == VARIANT_PLANKS_GREATWOOD || variant == VARIANT_PLANKS_SILVERWOOD;
    }
}
