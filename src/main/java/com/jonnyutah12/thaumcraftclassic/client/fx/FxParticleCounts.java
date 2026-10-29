package com.jonnyutah12.thaumcraftclassic.client.fx;

/**
 * The original's particle-count gate (TC-M7-02-s).
 *
 * <p>{@code ClientProxy.particleCount(int base)}
 * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:949-955)
 * is the multiplier every FX sender runs its spawn loop with:
 * the vanilla particle setting {@code 2} (minimal) returns 0, {@code 1}
 * (decreased) returns {@code base * 1}, and {@code 0} (all) returns
 * {@code base * 2} — the original deliberately DOUBLES the base count on
 * "all", so a sparkle burst is 14 particles, not 7.</p>
 *
 * <p>1.21.1 keeps the same three-value setting
 * ({@code Options#particleStatus} / {@code ParticleStatus}, ordinals
 * ALL=0, DECREASED=1, MINIMAL=2), so the gate is the same function over the
 * same ordinal domain and stays unit-testable off the client: the caller reads
 * the ordinal and passes it in.</p>
 */
public final class FxParticleCounts {

    /** Vanilla {@code ParticleStatus.ALL} — the original's {@code == 0} branch. */
    public static final int SETTING_ALL = 0;
    /** Vanilla {@code ParticleStatus.DECREASED} — the original's {@code == 1} branch. */
    public static final int SETTING_DECREASED = 1;
    /** Vanilla {@code ParticleStatus.MINIMAL} — the original's {@code == 2} branch. */
    public static final int SETTING_MINIMAL = 2;

    private FxParticleCounts() {
    }

    /**
     * The original's {@code particleCount(base)} body, verbatim in behaviour:
     * minimal → 0, decreased → base, all → base * 2. Unknown settings take the
     * {@code all} branch exactly like the original's if/else chain does.
     */
    public static int particleCount(final int base, final int particleSetting) {
        if (particleSetting == SETTING_MINIMAL) {
            return 0;
        }
        return particleSetting == SETTING_DECREASED ? base : base * 2;
    }
}
