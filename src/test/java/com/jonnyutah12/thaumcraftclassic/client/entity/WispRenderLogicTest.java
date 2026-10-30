package com.jonnyutah12.thaumcraftclassic.client.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the wisp renderer's animation and geometry maths (TC-M7-03-s).
 *
 * <p>Every expected figure is transcribed from
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderWisp.java}
 * (line references in each assertion) rather than from this port's own code, so
 * a transcription slip in {@link WispRenderLogic} fails here. No client, no GL
 * context, no window: the maths is world-free by construction.</p>
 */
class WispRenderLogicTest {

    // ------------------------------------------------------------- animation

    /** {@code int i = entity.field_70173_aa % 16} (RenderWisp.java:48,79). */
    @Test
    void aWispCyclesThroughSixteenFramesWrappingOnTheTickCount() {
        assertEquals(16, WispRenderLogic.FRAME_COUNT, "4x4 grid of the 256px sheet divided by 64");
        assertEquals(0, WispRenderLogic.frameIndex(0));
        assertEquals(7, WispRenderLogic.frameIndex(7));
        assertEquals(15, WispRenderLogic.frameIndex(15));
        assertEquals(0, WispRenderLogic.frameIndex(16), "tick 16 wraps back to frame 0");
        assertEquals(3, WispRenderLogic.frameIndex(35));
        // The original's `%` is Java's on ints, so a negative tick mirrors the
        // wrap rather than going out of range; the port keeps it in range.
        assertEquals(15, WispRenderLogic.frameIndex(-1));
    }

    // ----------------------------------------------------------- body sheet

    /**
     * {@code size1 = getTextureSize("textures/misc/wisp.png", 64) = 256/64 = 4};
     * {@code size4 = size1 * 4 = 16}; the frame rect is
     * {@code (i%4*size1 + {0, size1-0.01}) / size4} on both axes
     * (RenderWisp.java:49-56).
     */
    @Test
    void bodyFrameUvsWalkTheFourByFourSheetWithTheOriginalInset() {
        final WispRenderLogic.Uv first = WispRenderLogic.frameUv(0);
        assertEquals(0.0F, first.u0(), 1.0e-6F);
        assertEquals(3.99F / 16.0F, first.u1(), 1.0e-6F, "size1 - 0.01F inset at the frame edge");
        assertEquals(0.0F, first.v0(), 1.0e-6F);
        assertEquals(3.99F / 16.0F, first.v1(), 1.0e-6F);

        // Frame 5 = column 1, row 1.
        final WispRenderLogic.Uv mid = WispRenderLogic.frameUv(5);
        assertEquals(4.0F / 16.0F, mid.u0(), 1.0e-6F);
        assertEquals(7.99F / 16.0F, mid.u1(), 1.0e-6F);
        assertEquals(4.0F / 16.0F, mid.v0(), 1.0e-6F);
        assertEquals(7.99F / 16.0F, mid.v1(), 1.0e-6F);

        // Last frame = column 3, row 3, and stays inside the sheet.
        final WispRenderLogic.Uv last = WispRenderLogic.frameUv(15);
        assertEquals(12.0F / 16.0F, last.u0(), 1.0e-6F);
        assertEquals(15.99F / 16.0F, last.u1(), 1.0e-6F);
        assertEquals(12.0F / 16.0F, last.v0(), 1.0e-6F);
        assertEquals(15.99F / 16.0F, last.v1(), 1.0e-6F);
        assertTrue(last.u1() < 1.0F && last.v1() < 1.0F, "the inset keeps every frame off the sheet border");

        // Out-of-range indices wrap, they never index a fifth cell.
        assertEquals(first, WispRenderLogic.frameUv(16));
        assertEquals(WispRenderLogic.frameUv(3), WispRenderLogic.frameUv(-13));
    }

    // ---------------------------------------------------------- glow shell

    /**
     * {@code qq = tick % 16; x0 = qq/16; x1 = (qq+1)/16; x2 = 5/16;
     * x3 = 6/16} (RenderWisp.java:79-84) — the shell always samples atlas row 5.
     */
    @Test
    void shellUvsWalkTheAtlasColumnsAtTheFixedRowFive() {
        final WispRenderLogic.Uv first = WispRenderLogic.shellUv(0);
        assertEquals(0.0F, first.u0(), 1.0e-6F);
        assertEquals(1.0F / 16.0F, first.u1(), 1.0e-6F);
        assertEquals(5.0F / 16.0F, first.v0(), 1.0e-6F, "x2 = 5.0F / size8");
        assertEquals(6.0F / 16.0F, first.v1(), 1.0e-6F, "x3 = 6.0F / size8");

        final WispRenderLogic.Uv second = WispRenderLogic.shellUv(17);
        assertEquals(1.0F / 16.0F, second.u0(), 1.0e-6F, "17 % 16 = 1");
        assertEquals(2.0F / 16.0F, second.u1(), 1.0e-6F);

        // The row never changes, whatever the frame.
        for (int frame = 0; frame < WispRenderLogic.FRAME_COUNT; frame++) {
            assertEquals(5.0F / 16.0F, WispRenderLogic.shellUv(frame).v0(), 1.0e-6F);
            assertEquals(6.0F / 16.0F, WispRenderLogic.shellUv(frame).v1(), 1.0e-6F);
        }
    }

    /** {@code f10 = 0.4F + sin((tick + pticks) / 10.0F) * 0.1F} (RenderWisp.java:85-86). */
    @Test
    void shellScalePulsesBetweenTheOriginalBounds() {
        assertEquals(0.4F, WispRenderLogic.shellScale(0, 0.0F), 1.0e-6F);
        assertEquals(0.4F + (float) Math.sin(1.0D) * 0.1F, WispRenderLogic.shellScale(10, 0.0F), 1.0e-6F,
                "sin((tick + pticks) / 10), not sin(tick / 10)");
        assertEquals(0.4F + (float) Math.sin(1.0D) * 0.1F, WispRenderLogic.shellScale(5, 5.0F), 1.0e-6F,
                "partial ticks advance the pulse");
        assertEquals(0.4F,
                WispRenderLogic.shellScale(0, (float) (2.0D * Math.PI * WispRenderLogic.SHELL_PULSE_PERIOD)),
                1.0e-5F, "one full period is 2*pi*10 = 62.83 ticks, so sin is back at 0");

        // Bounds: 0.3 .. 0.5 over a full period.
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int tick = 0; tick < 1000; tick++) {
            final float scale = WispRenderLogic.shellScale(tick, 0.0F);
            min = Math.min(min, scale);
            max = Math.max(max, scale);
        }
        assertEquals(0.3F, min, 1.0e-3F);
        assertEquals(0.5F, max, 1.0e-3F);
    }

    // -------------------------------------------------------------- colour

    /**
     * Body tint {@code color.getRed()/255F, ...} and the hurt flash
     * {@code 1.0F, color.getGreen()/300F, color.getBlue()/300F}
     * (RenderWisp.java:59-63).
     */
    @Test
    void bodyTintIsTheAspectColourAndFlashesRedWhileHurt() {
        final float[] tint = WispRenderLogic.bodyColour(0x00336699, 0);
        assertEquals(0x33 / 255.0F, tint[0], 1.0e-6F);
        assertEquals(0x66 / 255.0F, tint[1], 1.0e-6F);
        assertEquals(0x99 / 255.0F, tint[2], 1.0e-6F);
        assertEquals(1.0F, tint[3], 1.0e-6F, "the original always writes alpha 1.0F");

        final float[] flash = WispRenderLogic.bodyColour(0x00336699, 1);
        assertEquals(1.0F, flash[0], 1.0e-6F, "red is driven to full while field_70737_aN > 0");
        assertEquals(0x66 / 300.0F, flash[1], 1.0e-6F);
        assertEquals(0x99 / 300.0F, flash[2], 1.0e-6F);

        // No aspect resolved -> the original keeps `new Color(0)`: black.
        final float[] none = WispRenderLogic.bodyColour(0, 0);
        assertEquals(0.0F, none[0], 1.0e-6F);
        assertEquals(0.0F, none[1], 1.0e-6F);
        assertEquals(0.0F, none[2], 1.0e-6F);

        final float[] shell = WispRenderLogic.shellColour();
        assertEquals(1.0F, shell[0], 1.0e-6F);
        assertEquals(1.0F, shell[3], 1.0e-6F);
    }

    // ------------------------------------------------------------ geometry

    /**
     * The four corners in the original's emission order
     * (RenderWisp.java:65-68) with its UV assignment: the two {@code -right}
     * corners take {@code u1}, the two {@code +right} corners {@code u0}; the
     * upper corners take {@code v0} and the lower ones {@code v1}.
     */
    @Test
    void quadIsTheOriginalFourCornersFacingTheCameraBasis() {
        final WispRenderLogic.Uv uv = new WispRenderLogic.Uv(0.1F, 0.2F, 0.3F, 0.4F);
        final WispRenderLogic.Corner[] corners = WispRenderLogic.quad(uv);
        assertEquals(4, corners.length, "one quad per pass");

        assertEquals(new WispRenderLogic.Corner(-1.0F, -1.0F, 0.2F, 0.4F), corners[0]);
        assertEquals(new WispRenderLogic.Corner(-1.0F, 1.0F, 0.2F, 0.3F), corners[1]);
        assertEquals(new WispRenderLogic.Corner(1.0F, 1.0F, 0.1F, 0.3F), corners[2]);
        assertEquals(new WispRenderLogic.Corner(1.0F, -1.0F, 0.1F, 0.4F), corners[3]);

        // A unit quad: the camera basis is applied by the pose, the corners are
        // always +/-1 before the f10 scale.
        for (final WispRenderLogic.Corner corner : corners) {
            assertEquals(1.0F, Math.abs(corner.x()), 1.0e-6F);
            assertEquals(1.0F, Math.abs(corner.y()), 1.0e-6F);
        }
    }

    /** The billboard sits 0.45 above the feet and the body pass is unscaled. */
    @Test
    void billboardOffsetsMatchTheOriginal() {
        assertEquals(0.45F, WispRenderLogic.VERTICAL_OFFSET, 1.0e-6F, "f12 = (float)y + 0.45F");
        assertEquals(1.0F, WispRenderLogic.BODY_SCALE, 1.0e-6F, "f10 = 1.0F for the body pass");
        assertFalse(WispRenderLogic.BODY_SCALE == WispRenderLogic.SHELL_BASE_SCALE,
                "the two passes use different scales; they are not interchangeable");
    }
}
