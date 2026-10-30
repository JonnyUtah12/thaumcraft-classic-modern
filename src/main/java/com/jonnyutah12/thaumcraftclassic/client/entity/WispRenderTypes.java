package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The two pass render types of the wisp billboard (TC-M7-03-s).
 *
 * <p>The original renders with immediate-mode GL: {@code glEnable(GL_BLEND)},
 * {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} — <b>additive</b> — and
 * {@code glDepthMask(false)} for both passes, with depth testing left on
 * ({@code RenderWisp.java:43-46, 73-77}). Three of those four are mapped
 * straight onto 1.21.1 render state shards:</p>
 * <ul>
 *   <li>{@code GL_SRC_ALPHA, GL_ONE} -> {@code ADDITIVE_TRANSPARENCY};</li>
 *   <li>{@code glDepthMask(false)} -> {@code COLOR_WRITE} (write colour, not
 *       depth);</li>
 *   <li>{@code glDisable(GL_CULL_FACE)} was never called by the original, but
 *       the quad is emitted in one winding only, so {@code NO_CULL} preserves
 *       the "either face is drawn" behaviour of the original's raw quads.</li>
 * </ul>
 *
 * <p>The vertex format is {@code POSITION_COLOR_TEX_LIGHTMAP}: a textured,
 * coloured quad with a fixed lightmap coordinate — exactly what
 * {@code Tessellator#func_78382_b/func_78369_a/func_78374_a} produced after the
 * original's {@code func_78380_c(240)}.</p>
 */
final class WispRenderTypes {

    /**
     * {@code func_78380_c(240)}: the original pins the lightmap coordinate to
     * the raw 1.7.10 value 240 for both passes, so a wisp is drawn at its own
     * full brightness and ignores the world light level. 240 (0xF0) is the
     * 1.7.10 maximum light value, and 1.21.1's {@code LightTexture.FULL_BRIGHT}
     * is that same maximum for both sky and block light; the renderer uses
     * {@code FULL_BRIGHT} rather than the bare {@code 240} literal because
     * 1.21.1 packs the lightmap as two 16-bit halves. Recorded as a documented
     * approximation in {@code docs/BEHAVIORAL_DEVIATIONS.md}.</p>
     */
    static final int LIGHT = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

    private WispRenderTypes() {
    }

    /** The textured, additively blended quad used by both wisp passes. */
    static RenderType billboard(final String name, final ResourceLocation texture) {
        return RenderType.create(
                name,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    /**
     * Body pass: {@code textures/misc/wisp.png}, the 4x4 animated sheet the
     * original binds with {@code UtilsFX.bindTexture}.
     */
    static final class Body {
        static final RenderType TYPE =
                billboard("thaumcraft_wisp_body", EntityWispRenderer.WISP_TEXTURE);

        private Body() {
        }
    }

    /**
     * Glow shell pass: {@code ParticleEngine.particleTexture}
     * ({@code thaumcraft:textures/misc/particles.png}) — the original's own
     * atlas, addressed as a raw animated texture rather than the build-time
     * sliced sprites TC-M7-02-s committed for the particle system.
     */
    static final class Shell {
        static final RenderType TYPE =
                billboard("thaumcraft_wisp_shell", EntityWispRenderer.SHELL_TEXTURE);

        private Shell() {
        }
    }
}
