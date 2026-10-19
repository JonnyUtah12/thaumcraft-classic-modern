package com.jonnyutah12.thaumcraftclassic.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player warp state: permanent, temporary and sticky warp, the eldritch warp
 * counter, and the runic (shielding) charge. Field names and NBT tag names are the
 * exact originals from Thaumcraft 4.2.3.5 {@code ResearchManager.savePlayerData}
 * ({@code Thaumcraft.eldritch*}, {@code Thaumcraft.shielding}) and the clamping
 * semantics match {@code PlayerKnowledge.addWarp*} ({@code Math.max(0, ...)}); see
 * docs/architecture/persistence.md.
 *
 * <p>Carried as a NeoForge data attachment (see {@link ModAttachments}) with
 * {@code copyOnDeath} and automatic S2C sync. The codec is lenient: every field is
 * optional (defaults to 0) and unknown fields are ignored.</p>
 *
 * <p>Immutable: mutation helpers return updated copies.</p>
 */
public record PlayerWarpData(int warpPerm, int warpTemp, int warpSticky, int warpCounter, int runicCharge) {

    // Original TC4 save tags (ResearchManager.savePlayerData).
    public static final String TAG_WARP_PERM = "Thaumcraft.eldritch";
    public static final String TAG_WARP_TEMP = "Thaumcraft.eldritch.temp";
    public static final String TAG_WARP_STICKY = "Thaumcraft.eldritch.sticky";
    public static final String TAG_WARP_COUNTER = "Thaumcraft.eldritch.counter";
    public static final String TAG_RUNIC_CHARGE = "Thaumcraft.shielding";

    public static final Codec<PlayerWarpData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(TAG_WARP_PERM, 0).forGetter(PlayerWarpData::warpPerm),
            Codec.INT.optionalFieldOf(TAG_WARP_TEMP, 0).forGetter(PlayerWarpData::warpTemp),
            Codec.INT.optionalFieldOf(TAG_WARP_STICKY, 0).forGetter(PlayerWarpData::warpSticky),
            Codec.INT.optionalFieldOf(TAG_WARP_COUNTER, 0).forGetter(PlayerWarpData::warpCounter),
            Codec.INT.optionalFieldOf(TAG_RUNIC_CHARGE, 0).forGetter(PlayerWarpData::runicCharge)
    ).apply(instance, PlayerWarpData::new));

    public static PlayerWarpData empty() {
        return new PlayerWarpData(0, 0, 0, 0, 0);
    }

    /** TC4 parity: {@code PlayerKnowledge.getWarpTotal} = perm + temp + sticky. */
    public int total() {
        return warpPerm + warpTemp + warpSticky;
    }

    // TC4 parity: addWarp* clamps the result at 0, never the delta.
    public PlayerWarpData addWarpPerm(final int amount) {
        return withWarpPerm(Math.max(0, warpPerm + amount));
    }

    public PlayerWarpData addWarpTemp(final int amount) {
        return withWarpTemp(Math.max(0, warpTemp + amount));
    }

    public PlayerWarpData addWarpSticky(final int amount) {
        return withWarpSticky(Math.max(0, warpSticky + amount));
    }

    public PlayerWarpData withWarpPerm(final int value) {
        return new PlayerWarpData(Math.max(0, value), warpTemp, warpSticky, warpCounter, runicCharge);
    }

    public PlayerWarpData withWarpTemp(final int value) {
        return new PlayerWarpData(warpPerm, Math.max(0, value), warpSticky, warpCounter, runicCharge);
    }

    public PlayerWarpData withWarpSticky(final int value) {
        return new PlayerWarpData(warpPerm, warpTemp, Math.max(0, value), warpCounter, runicCharge);
    }

    public PlayerWarpData withWarpCounter(final int value) {
        return new PlayerWarpData(warpPerm, warpTemp, warpSticky, value, runicCharge);
    }

    public PlayerWarpData withRunicCharge(final int value) {
        return new PlayerWarpData(warpPerm, warpTemp, warpSticky, warpCounter, value);
    }
}
