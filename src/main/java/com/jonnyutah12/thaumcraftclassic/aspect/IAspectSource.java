package com.jonnyutah12.thaumcraftclassic.aspect;

/**
 * A block entity that can supply essentia to consumers that cannot reach the
 * container directly (content-mapping.csv consumer rows: tilemirroressentia).
 *
 * <p>Original: {@code thaumcraft.api.aspects.IAspectSource} — a marker over
 * {@code IAspectContainer}. The port has no standalone IAspectContainer; the
 * container surface here mirrors {@link IEssentiaTransport}'s read/consume
 * methods so providers (essentia jars, TC-M4-06) can implement both.</p>
 */
public interface IAspectSource {

    /** Total essentia available for the aspect pool (original getAspectPoolSize). */
    int getAspectPoolSize();

    /** Whether the source holds at least one unit of this aspect (original doesContain). */
    boolean doesContain(Aspect aspect);

    /** Consume up to {@code amount} of the aspect; return what was taken (original useAspect). */
    int useAspect(Aspect aspect, int amount);
}
