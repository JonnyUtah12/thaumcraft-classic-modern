package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
 * growth/fertility lamps (8, 13) lit 15 with charges and 8 without — the
 * charge-driven half is TE-dynamic and deferred (noted in the handoff); the
 * static 8 base is applied. Meta 14's vis-relay glow is TC-M5-02's.</p>
 */
public class BlockMetalDevice extends BlockDeviceBase {

    public static final int MAX_VARIANT = 14;

    public static final int VARIANT_LAMP = 7;
    public static final int VARIANT_LAMP_GROWTH = 8;
    public static final int VARIANT_LAMP_FERTILITY = 13;

    public BlockMetalDevice() {
        super(MAX_VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 17.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> switch (state.getValue(variantProperty())) {
                    case 3 -> 11;
                    case VARIANT_LAMP -> 15;
                    case VARIANT_LAMP_GROWTH, VARIANT_LAMP_FERTILITY -> 8;
                    default -> 0;
                }));
    }
}
