package com.jonnyutah12.thaumcraftclassic.entities.golems;

/**
 * A golem marker — the original's {@code thaumcraft.common.entities.golems.Marker}
 * (TC-M6-08-s-b). The marker list is how a golem knows which blocks it may
 * touch: every {@code GolemHelper} query resolves through it, so this is
 * functional state, not decoration.
 *
 * <p>Fields are the original's, all six, with the original types:
 * {@code x}/{@code y}/{@code z} (block coordinates), {@code dim} (dimension
 * id), {@code side} (the face of the marked block the golem interacts with),
 * {@code color} (the marker colour; {@code -1} is the wildcard). The original's
 * constructor takes {@code dim} as an {@code int} and {@code side}/{@code color}
 * as {@code byte} — kept as-is so the disk format in
 * {@link EntityGolemBase#addAdditionalSaveData} stays the original's.</p>
 *
 * <p><b>1.7.10 → 1.21.1.</b> No behavioural change: the class carried no
 * Minecraft dependency in the original either. The golem's {@code dim} check
 * ({@code marker.dim == world.provider.dimensionId}) becomes a comparison
 * against the level's dimension in {@link #isIn(net.minecraft.resources.ResourceKey)};
 * the original's integer dimension ids no longer exist in 1.21.1, so the stored
 * {@code dim} is left as the original's int and compared through
 * {@link #dim()}, with the level side resolved by the caller. See the
 * deviation note on {@link EntityGolemBase#validateMarkers()}.</p>
 */
public final class Marker {

    public int x;
    public int y;
    public int z;
    public int dim;
    public byte side;
    public byte color;

    public Marker(final int x, final int y, final int z, final int dim, final byte side, final byte color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.side = side;
        this.color = color;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public int dim() {
        return this.dim;
    }

    public byte side() {
        return this.side;
    }

    public byte color() {
        return this.color;
    }

    /** Original {@code equals}: every field, colour included. */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Marker marker) {
            return this.x == marker.x
                    && this.y == marker.y
                    && this.z == marker.z
                    && this.dim == marker.dim
                    && this.side == marker.side
                    && this.color == marker.color;
        }
        return false;
    }

    /**
     * Original {@code equalsFuzzy}: position, dimension and side must match;
     * the colour matches when it is equal <em>or</em> when {@code this} colour
     * is {@code -1} (the wildcard). This is the comparison
     * {@code GolemHelper.contained} uses, which is what makes a wildcard-coloured
     * marker (or a {@code -1} query colour) match every marker on that face.
     */
    public boolean equalsFuzzy(final Object obj) {
        if (!(obj instanceof Marker marker)) {
            return false;
        }
        return this.x == marker.x
                && this.y == marker.y
                && this.z == marker.z
                && this.dim == marker.dim
                && this.side == marker.side
                && (this.color == marker.color || this.color == -1);
    }

    /** The original had no {@code hashCode}; kept absent so the two comparison methods above stay the only contract. */
    @Override
    public String toString() {
        return "Marker[" + this.x + "," + this.y + "," + this.z + " dim=" + this.dim
                + " side=" + this.side + " color=" + this.color + "]";
    }
}
