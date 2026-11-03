package com.jonnyutah12.thaumcraftclassic.render.art;

import net.minecraft.resources.ResourceLocation;

/**
 * The spa's water as a data plan (BD-18, t_6563ce50, D-021).
 *
 * <p><b>The card's citation is false and the port follows the source.</b>
 * t_6563ce50 cites {@code BlockSpa.java:69-71} for a {@code water_still}
 * quad. There is no {@code BlockSpa.java} anywhere in
 * {@code reference/decompiled} (the spa is {@code BlockStoneDevice}
 * metadata 12, whose render hook {@code BlockStoneDeviceRenderer.java:240-251}
 * draws a full cube — pedestal bottom, {@code spa_top}, four
 * {@code spa_side} faces — and no water at all), and
 * {@code grep -rn water_still reference/decompiled --include=*.java} is
 * empty. In 4.2.3.5 the spa's water is tank state only: a 5000 mB tank on
 * {@code TileSpa} shown in {@code GuiSpa}; the full cube is opaque, so the
 * original had no visible water.</p>
 *
 * <p>The card's intent — "spa water, driven by the tile" — is therefore
 * <b>new art</b>, recorded in BD-18: a {@code minecraft:block/water_still}
 * surface on the spa's top face (vanilla's own sprite; no CSV row, the CSV
 * governs {@code thaumcraft:} sprites, and the reference has no water
 * texture to map), inset 1/16 from the cell edges, shown while the tank
 * holds anything and scaled across the face by the fill fraction
 * ({@code tile.getTankAmount() / 5000}, {@code TileSpa.java:39, 65-71}).
 * The tank is tile data that survives save/load ({@code saveAdditional}
 * writes {@code FluidName}/{@code Amount}, {@code tiles/TileSpa.java:333-334} (port sync keys)),
 * so the art is observable, save-safe, and testable without a client.</p>
 *
 * <p>Server-safe: plain int in, boolean + fraction out.</p>
 */
public final class SpaWorldArt {

    /** Vanilla's own water surface sprite — the vanilla atlas, never the CSV. */
    public static final ResourceLocation WATER_SPRITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");

    /** {@code TileSpa.TANK_CAPACITY} ({@code TileSpa.java:39}). */
    public static final int TANK_CAPACITY = 5000;

    /** The 1/16 inset from the cell edges (the top face is the block's rim). */
    public static final float WATER_INSET = 0.0625F;

    /** visible: tank not empty; uScale: fill fraction across the face. */
    public record Plan(boolean visible, float uScale) {
    }

    /**
     * The tank read: {@code amount > 0} shows water; the fill fraction is
     * {@code amount / 5000} ({@code TileSpa.getTankAmount()}).
     */
    public static Plan plan(final int tankAmount, final int capacity) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    private SpaWorldArt() {
    }
}
