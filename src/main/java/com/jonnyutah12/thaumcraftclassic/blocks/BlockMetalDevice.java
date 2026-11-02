package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;

/**
 * Metal device (content-mapping.csv {@code thaumcraft:blockmetaldevice}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockMetalDevice} — one block
 * id, metadata 0-14. Physical properties are the original's: metal material,
 * hardness 3.0, resistance 17.0, metal step sound, full cube. Meta map per
 * docs/catalog-triage.md; this card owns the arcane lamps (metas 7, 8, 13),
 * the rest are feature-card tiles via {@link DeviceTileFactory}.</p>
 *
 * <p>Light per the original: meta 3 static 11, meta 7 (arcane lamp) 15. The
 * growth/fertility lamps (8, 13) lit 15 with charges and 8 without
 * ({@code BlockMetalDevice.java:792-815}) — BD-18 ships that half through the
 * {@link #CHARGED} property, which the lamp tiles publish from their running
 * state (see {@link DeviceDynamicState}). Meta 14's vis-relay glow is
 * TC-M5-02's.</p>
 */
public class BlockMetalDevice extends BlockDeviceBase implements DeviceDynamicState {

    public static final int MAX_VARIANT = 14;

    public static final int VARIANT_LAMP = 7;
    public static final int VARIANT_LAMP_GROWTH = 8;
    public static final int VARIANT_LAMP_FERTILITY = 13;

    /** The {@code variant} property this block carries (D-001: original meta range). */
    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    /**
     * BD-18: the lamp face pair the blockstate picks. Only variants 8 and 13
     * read it. The original's swap is the growth/fertility lamps'
     * {@code charges > 0} branch inside its connected-texture hook
     * ({@code BlockMetalDevice.java:117-155}: growth lit =
     * {@code lamp_grow_top} on the caps / {@code lamp_grow_side} on the sides,
     * the {@code lamp_grow_top_off}/{@code lamp_grow_side_off} pair
     * otherwise; fertility = {@code lamp_fert_top}/{@code lamp_fert_side} and
     * the {@code *_off} pair), and the light level follows it
     * ({@code BlockMetalDevice.java:792-815}: 15 charged, 8 uncharged).
     * Variant 7 — the plain arcane lamp — always drew its lit
     * {@code lamp_top}/{@code lamp_side} pair ({@code :122}), so its look is
     * static and SPINE-4 shipped it that way.
     */
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public BlockMetalDevice() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 17.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> switch (state.getValue(VARIANT)) {
                    case 3 -> 11;
                    case VARIANT_LAMP -> 15;
                    // Original getLightValue: 15 while the lamp has charges, 8 without.
                    case VARIANT_LAMP_GROWTH, VARIANT_LAMP_FERTILITY ->
                            state.getValue(CHARGED) ? 15 : 8;
                    default -> 0;
                }));
    }

    /** The {@code variant} and {@code charged} properties; the base cannot add
     * them (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, CHARGED);
    }

    @Override
    public Property<?> dynamicAppearanceProperty() {
        return CHARGED;
    }
}
