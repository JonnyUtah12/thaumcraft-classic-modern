package com.jonnyutah12.thaumcraftclassic.entities.golems;

/**
 * The golem numbers the original read out of {@code thaumcraft.common.config.Config}
 * (TC-M6-08-s-b). The port has no config file seam yet, so these ship as
 * constants pinned to the original's own defaults and clamped the way the
 * original's {@code Config.load} clamps them
 * ({@code Config.java:424-478}):
 *
 * <ul>
 *   <li>{@link #GOLEM_DELAY} = {@code golemDelay = 5} — ticks a golem waits
 *       between checking for tasks ({@code Config.java:130}); clamped to a
 *       minimum of 1 ({@code Config.java:427-429}).</li>
 *   <li>{@link #GOLEM_IGNORE_DELAY} = {@code golemIgnoreDelay = 10000} —
 *       milliseconds a golem ignores an item after failing to place or use it
 *       ({@code Config.java:131}); clamped to a minimum of 1000
 *       ({@code Config.java:434-436}).</li>
 *   <li>{@link #GOLEM_LINK_QUALITY} = {@code golemLinkQuality = 16} — the fx
 *       quality of the golem-to-marker line ({@code Config.java:132});
 *       below 4 the original deactivates the effect entirely
 *       ({@code Config.java:441-443}).</li>
 *   <li>{@link #GOLEM_CHEST_INTERACT} = {@code golemChestInteract = true} —
 *       whether the golem plays the chest open animation
 *       ({@code Config.java:122}).</li>
 * </ul>
 */
public final class GolemConfig {

    /** Original {@code Config.golemDelay}, default 5, minimum 1. */
    public static final int GOLEM_DELAY = 5;

    /** Original {@code Config.golemIgnoreDelay}, default 10000 ms, minimum 1000. */
    public static final int GOLEM_IGNORE_DELAY = 10_000;

    /** Original {@code Config.golemLinkQuality}, default 16; below 4 disables the beam. */
    public static final int GOLEM_LINK_QUALITY = 16;

    /** Original {@code Config.golemChestInteract}, default true. */
    public static final boolean GOLEM_CHEST_INTERACT = true;

    private GolemConfig() {
    }
}
