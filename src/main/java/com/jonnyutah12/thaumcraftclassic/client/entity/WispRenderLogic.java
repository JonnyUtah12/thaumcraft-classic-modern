package com.jonnyutah12.thaumcraftclassic.client.entity;

/**
 * World-free, graphics-free geometry and animation maths for the wisp renderer
 * (TC-M7-03-s — the elemental slice's renderer).
 *
 * <p>Original: {@code thaumcraft.client.renderers.entity.RenderWisp}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderWisp.java}).
 * It is not a model renderer at all: it draws <b>two camera-facing quads</b>,
 * additively blended, at the entity position raised by 0.45 blocks —
 * a body pass sampled from the 4x4 animated sheet
 * {@code textures/misc/wisp.png} and tinted with the wisp's aspect colour, and
 * a glow shell pass sampled from the 16x16 animated atlas
 * {@code textures/misc/particles.png} at row 5 and scaled by a sine pulse.
 * {@code field_76989_e = 0.0F} in the constructor means <b>no</b> shadow.</p>
 *
 * <p>Every figure below is a transcription of that class, so the animation and
 * geometry can be pinned by plain unit tests with no client, no GL context and
 * no window (the same split TC-M6-06-s used for {@code WispAiLogic}, and
 * TC-M7-02-s for the particle frame lists). The renderer itself
 * ({@link EntityWispRenderer}) only turns these values into vertices.</p>
 *
 * <h2>Source figures</h2>
 * <ul>
 *   <li>{@code size1 = UtilsFX.getTextureSize("textures/misc/wisp.png", 64)} —
 *       the sheet is 256x256 and is divided by 64, so {@code size1 = 4} cells
 *       per row and {@code size4 = size1 * 4 = 16}: a <b>4x4 grid, 16 frames</b>,
 *       advanced by {@code field_70173_aa % 16} (the entity tick count).</li>
 *   <li>Cell inset {@code size1 - 0.01F} — a 0.01-cell bleed inset at each
 *       frame edge, reproduced verbatim.</li>
 *   <li>Body tint {@code color.getRed()/255F, getGreen()/255F, getBlue()/255F}
 *       with the aspect colour, or {@code 1.0F, getGreen()/300F,
 *       getBlue()/300F} while {@code field_70737_aN > 0} (the hurt flash).</li>
 *   <li>Glow shell: atlas column {@code qq = tick % 16}, row 5 (i.e.
 *       {@code 5/16 .. 6/16}), {@code f10 = 0.4F + sin((tick + pticks) / 10F) *
 *       0.1F}. This is {@code ParticleEngine.particleTexture} — the original's
 *       own atlas, not the sliced particle sprites of TC-M7-02-s.</li>
 * </ul>
 */
public final class WispRenderLogic {

    // ------------------------------------------------------------- body sheet

    /** {@code size1}: 256px sheet / 64 = 4 cells per row. */
    public static final int SHEET_CELLS_PER_ROW = 4;

    /** {@code size4 == size1 * size1}: the sheet is measured in 16 units. */
    public static final int SHEET_UNITS = SHEET_CELLS_PER_ROW * SHEET_CELLS_PER_ROW;

    /** 4x4 grid -> 16 animation frames. */
    public static final int FRAME_COUNT = SHEET_UNITS;

    /** {@code size1 - 0.01F}: the original's per-frame bleed inset. */
    public static final float FRAME_INSET = 0.01F;

    // --------------------------------------------------------------- shell

    /** The particle atlas is a 16x16 cell grid ({@code qq / 16.0F}). */
    public static final int SHELL_ATLAS_CELLS = 16;

    /** {@code x2 = 5.0F / size8} — the shell always samples atlas row 5. */
    public static final int SHELL_ATLAS_ROW = 5;

    /** The original's {@code f10} for the body pass. */
    public static final float BODY_SCALE = 1.0F;

    /** {@code f12 = (float)y + 0.45F} — the billboard centre above the feet. */
    public static final float VERTICAL_OFFSET = 0.45F;

    /** {@code f10 = 0.4F + var11} base. */
    public static final float SHELL_BASE_SCALE = 0.4F;

    /** {@code var11 = sin((tick + pticks) / 10.0F) * 0.1F}. */
    public static final float SHELL_PULSE_AMPLITUDE = 0.1F;

    /** {@code / 10.0F} — the pulse period divisor. */
    public static final float SHELL_PULSE_PERIOD = 10.0F;

    /** Peak split of the hurt flash: {@code green = color.getGreen() / 300.0F}. */
    public static final float HURT_FLASH_DIVISOR = 300.0F;

    /** Full-channel divisor of the normal body tint. */
    public static final float CHANNEL_DIVISOR = 255.0F;

    /** Fully opaque, in both passes (the original always passes alpha 1.0F). */
    public static final float ALPHA = 1.0F;

    // ------------------------------------------------------------ value types

    /**
     * A UV rectangle in texture space. {@code u0/v0} are the frame's origin
     * corner, {@code u1/v1} its far corner (already inset by
     * {@link #FRAME_INSET} of a cell).
     */
    public record Uv(float u0, float u1, float v0, float v1) {
    }

    /**
     * One billboard corner in the camera-facing local plane: {@code x} runs to
     * the camera's right, {@code y} up, with {@code u}/{@code v} the UV the
     * original assigned to it.
     */
    public record Corner(float x, float y, float u, float v) {
    }

    private WispRenderLogic() {
    }

    // ------------------------------------------------------------- animation

    /** {@code entity.field_70173_aa % 16}: the frame index for a tick count. */
    public static int frameIndex(final int tickCount) {
        return Math.floorMod(tickCount, FRAME_COUNT);
    }

    /** The body-sheet UVs for one frame index. */
    public static Uv frameUv(final int frame) {
        final int f = Math.floorMod(frame, FRAME_COUNT);
        final int col = f % SHEET_CELLS_PER_ROW;
        final int row = f / SHEET_CELLS_PER_ROW;
        final float cell = SHEET_CELLS_PER_ROW;
        return new Uv(
                col * cell / SHEET_UNITS,
                (col * cell + cell - FRAME_INSET) / SHEET_UNITS,
                row * cell / SHEET_UNITS,
                (row * cell + cell - FRAME_INSET) / SHEET_UNITS);
    }

    /**
     * The glow-shell UVs for one frame index: column {@code qq} of the 16-column
     * atlas, and the fixed row {@link #SHELL_ATLAS_ROW} (the original's
     * {@code x2 = 5.0F / size8, x3 = 6.0F / size8}).
     */
    public static Uv shellUv(final int frame) {
        final int qq = Math.floorMod(frame, SHELL_ATLAS_CELLS);
        return new Uv(
                qq / (float) SHELL_ATLAS_CELLS,
                (qq + 1) / (float) SHELL_ATLAS_CELLS,
                SHELL_ATLAS_ROW / (float) SHELL_ATLAS_CELLS,
                (SHELL_ATLAS_ROW + 1) / (float) SHELL_ATLAS_CELLS);
    }

    /** {@code f10 = 0.4F + sin((tick + pticks) / 10.0F) * 0.1F}. */
    public static float shellScale(final int tickCount, final float partialTick) {
        return SHELL_BASE_SCALE
                + (float) Math.sin((tickCount + partialTick) / SHELL_PULSE_PERIOD) * SHELL_PULSE_AMPLITUDE;
    }

    // ---------------------------------------------------------------- colour

    /**
     * The body tint, as 0..1 RGB plus the original's constant alpha 1.0.
     *
     * @param argb     the aspect colour ({@code Aspect#getColor()}), or 0 for
     *                 "no aspect resolved" — the original then keeps
     *                 {@code new Color(0)}, i.e. black
     * @param hurtTime {@code field_70737_aN}: when above zero the original
     *                 flashes the body red by driving green and blue to
     *                 {@code channel / 300}
     */
    public static float[] bodyColour(final int argb, final int hurtTime) {
        final float red = ((argb >> 16) & 0xFF) / CHANNEL_DIVISOR;
        final float green = ((argb >> 8) & 0xFF) / CHANNEL_DIVISOR;
        final float blue = (argb & 0xFF) / CHANNEL_DIVISOR;
        if (hurtTime > 0) {
            return new float[] { 1.0F, green * CHANNEL_DIVISOR / HURT_FLASH_DIVISOR,
                    blue * CHANNEL_DIVISOR / HURT_FLASH_DIVISOR, ALPHA };
        }
        return new float[] { red, green, blue, ALPHA };
    }

    /** The glow shell's constant white (the original's {@code 1,1,1,1}). */
    public static float[] shellColour() {
        return new float[] { 1.0F, 1.0F, 1.0F, ALPHA };
    }

    // -------------------------------------------------------------- geometry

    /**
     * The four billboard corners in the original's emission order
     * ({@code func_78374_a} call order in {@code renderEntityAt}): the
     * {@code -right,-up} corner carries {@code (u1, v1)}, {@code -right,+up}
     * {@code (u1, v0)}, {@code +right,+up} {@code (u0, v0)} and
     * {@code +right,-up} {@code (u0, v1)}. Both passes use this same mapping;
     * only the UV rect and the scale differ.
     */
    public static Corner[] quad(final Uv uv) {
        return new Corner[] {
                new Corner(-1.0F, -1.0F, uv.u1(), uv.v1()),
                new Corner(-1.0F, 1.0F, uv.u1(), uv.v0()),
                new Corner(1.0F, 1.0F, uv.u0(), uv.v0()),
                new Corner(1.0F, -1.0F, uv.u0(), uv.v1()),
        };
    }
}
