package com.jonnyutah12.thaumcraftclassic.blocks;

/**
 * The sixteen candle colours, server-safe (BD-18, t_065502ba).
 *
 * <p>Ground truth, both from the 1.7.10 sources:</p>
 * <ul>
 *   <li>{@code thaumcraft.common.lib.utils.Utils.java:50-52} declares
 *   {@code public static final int[] colors} — the sixteen RGB values, in the
 *   vanilla dye order named by {@code Utils.colorNames} on the lines above it
 *   (White, Orange, Magenta, …, Black);</li>
 *   <li>{@code thaumcraft.common.blocks.BlockCandle.java:76-78} tints the
 *   candle with it, keyed on the block's own metadata —
 *   <pre>
 *   public int func_149720_d(IBlockAccess world, int x, int y, int z) {  // getBlockColor
 *       int md = world.func_72805_g(x, y, z);                           // the metadata
 *       return Utils.colors[md];
 *   }
 *   </pre>
 *   and {@code BlockCandle.java:52-54} ({@code func_149741_i},
 *   {@code colorRepresentative}) returns the same value for the same metadata,
 *   so the flame particles and the breaking effect used it too.</li>
 * </ul>
 *
 * <p><b>The card's citation is off and the port follows the source.</b>
 * t_065502ba's scope line 3 reads {@code BlockCandle.java:177-180} →
 * {@code colour*32+6}; that expression is nowhere in the decompiled tree
 * ({@code grep -rn "32 + 6"} over {@code reference/decompiled} is empty) and
 * the array has 16 entries, so it could not index it. The real hook is
 * {@code Utils.colors[meta]} at the lines quoted above — implemented here, and
 * the correction is a BD-18 cell.</p>
 *
 * <p>Because a 1.7.10 candle's metadata <em>is</em> its dye colour
 * ({@code BlockCandle.java} header: metas 0-15 = the sixteen colours) and the
 * port carries that metadata verbatim as the {@code variant} property
 * ({@link BlockCandle#VARIANT}, 0-15, D-001), the lookup is a direct index —
 * no bit unpacking, no packing the port invented.</p>
 */
public final class DeviceTints {

    /**
     * {@code Utils.colors} ({@code Utils.java:50-52}), in the original's order
     * and decimal form so the parity probe compares it value for value against
     * the reference file.
     */
    public static final int[] COLOURS = {
            15790320, 15435844, 12801229, 6719955,
            14602026, 4312372, 14188952, 4408131,
            10526880, 2651799, 8073150, 2437522,
            5320730, 3887386, 11743532, 1973019,
    };

    /**
     * The candle tint for a variant: {@code Utils.colors[variant]}, clamped
     * into the table (the property is 0-15, so the clamp is a guard, not a
     * mapping).
     */
    public static int colourForVariant(final int variant) {
        return COLOURS[Math.min(Math.max(variant, 0), COLOURS.length - 1)];
    }

    /**
     * The levitator's glow-shell tints, from the original's own ISMR
     * ({@code client/renderers/block/BlockLifterRenderer.java:50-89}).
     *
     * <p><b>The bottom face, not the top.</b> Line 57 sets the colour to
     * {@code BlockCustomOreItem.colors[4]} (40960 = {@code 0x00A000}, the
     * green in that array's 5th slot) and lines 64-66 draw the glow on the
     * block's BOTTOM face at {@code y - 0.01}, gated on the block above being
     * solid. Lines 68-83 then set the colour to {@code 14488063}
     * ({@code 0xDD11FF}) and draw the four side glow faces at a 0.01 inset,
     * each gated on its neighbour being solid. The levitator has no top glow
     * face at all.</p>
     */
    public static final int LIFTER_GLOW_BOTTOM = 0x00A000;
    public static final int LIFTER_GLOW_SIDE = 0xDD11FF; // 14488063, the literal at :68

    private DeviceTints() {
    }
}
