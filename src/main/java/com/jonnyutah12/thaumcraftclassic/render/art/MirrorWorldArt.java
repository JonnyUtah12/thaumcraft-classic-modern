package com.jonnyutah12.thaumcraftclassic.render.art;

import net.minecraft.resources.ResourceLocation;

/**
 * The mirror's pane and portal as a data plan (BD-18, t_6563ce50, D-021).
 *
 * <p>1.7.10 drew a mirror in its tile-entity renderer,
 * {@code thaumcraft.client.renderers.tile.TileMirrorRenderer}, keyed on the
 * tile's link fields ({@code TileMirror.linked}/{@code linkX}/{@code linkY}/
 * {@code linkZ}/{@code linkDim}/{@code instability}):</p>
 *
 * <ul>
 *   <li>the dispatch ({@code :575-657}): orientation is {@code meta % 6}; a
 *   linked mirror (link target present, {@code :577, 594-603}) draws the
 *   16-layer ender-portal planes (tunnel + particlefield, {@code :38-200})
 *   and the {@code mirrorpanetrans.png} pane quad at offset
 *   {@code 0.02F + instability-jitter} ({@code :619-621}); an unlinked mirror
 *   draws the {@code mirrorpane.png} quad at the same offset ({@code :627-629}).
 *   The jitter is {@code random * instability / 10000} ({@code :578-582}) —
 *   live tile data ({@code instability} is synced, {@code TileMirror.java:253}).</li>
 *   <li>the frame icon quad ({@code :634-650}) is already shipped as the
 *   static per-variant blockstate models (SPINE-4), so it is not in this plan.</li>
 * </ul>
 *
 * <p>The 16 GL-texgen parallax layers cannot survive as-is (fixed-function
 * texgen has no 1.21.1 equivalent); the renderer draws the two real published
 * textures — {@code misc/tunnel} (alpha-blended, {@code blendFunc(770,771)},
 * {@code :54}) and {@code misc/particlefield} (additive,
 * {@code blendFunc(1,1)}, {@code :60}) — as quads at the same inset
 * ({@code p = 0.1875F}, {@code :41}) with the same fullbright floor, and BD-18 records the
 * parallax delta. The plan is the tile data: linked, jitter, sprites.</p>
 *
 * <p>Server-safe: the plan takes plain values and returns sprite ids.</p>
 */
public final class MirrorWorldArt {

    /** The unlinked pane, published {@code textures/block/mirrorpane.png}. */
    public static final ResourceLocation PANE_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "block/mirrorpane");

    /** The linked pane, published {@code textures/block/mirrorpanetrans.png}. */
    public static final ResourceLocation PANE_TRANSPARENT_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "block/mirrorpanetrans");

    /** Portal layer 1, published {@code textures/misc/tunnel.png} (TileMirrorRenderer.java:27). */
    public static final ResourceLocation TUNNEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "misc/tunnel");

    /** Portal layer 2, published {@code textures/misc/particlefield.png} (TileMirrorRenderer.java:28). */
    public static final ResourceLocation FIELD_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "misc/particlefield");

    /** The pane quad's standoff from the cell face, {@code :620, :628}. */
    public static final float PANE_OFFSET = 0.02F;

    /** The portal quad's inset from the cell edges, {@code p = 0.1875F} ({@code :41}). */
    public static final float PORTAL_INSET = 0.1875F;

    /** The pane swap + the quads a linked mirror draws in front of it. */
    public record Plan(ResourceLocation paneSprite, ResourceLocation tunnelSprite,
                       ResourceLocation fieldSprite, float paneOffset) {
    }

    /**
     * linked: {@code TileMirror.isLinkValidSimple()} (the renderer's own
     * gate, {@code :577, 594}); jitter: {@code random * instability / 10000}
     * ({@code :578-582}) added to the 0.02 standoff.
     */
    public static Plan plan(final boolean linked, final float instabilityJitter) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The jitter from live data: {@code instability / 10000F} scaled by a
     * render-time random in {@code [0,1)} (the original draws a fresh
     * {@code nextFloat} per frame, {@code :582}).
     */
    public static float jitter(final int instability, final float randomUnit) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    private MirrorWorldArt() {
    }
}
