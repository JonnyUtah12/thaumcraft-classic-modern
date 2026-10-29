package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.util.RandomSource;

/**
 * The wisp family's spawn rules, read off the decompiled sources (TC-M7-02-c-s-f1).
 *
 * <p>Everything in the 1.7.10 wisp constructors is a roll on {@code world.rand}
 * at spawn time: the lifetime, the per-type colour, and the render-distance
 * cull. The constructors run on the client, which is a wall for a game-test
 * server, so this class pulls the rolls out as pure functions over an explicit
 * {@link RandomSource}: {@link TcFxPlans} calls them in the original's draw
 * order and the probes below assert the ranges.</p>
 *
 * <p>Draw order is part of the fidelity claim. Each {@code FXWisp} colour case
 * rolls ONLY the channels the original rolls (case 2 draws one float, case 0
 * three, case 1 two), so reordering the draws changes every later particle in
 * the burst — the cases below mirror
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWisp.java:66-115}
 * line for line.</p>
 */
public final class FxWispRules {

    /** {@code FXWisp.java:41} / {@code FXWispEG.java:36} / {@code FXWispArcing.java:41}: the fancy-graphics render distance. */
    public static final int VISIBLE_DISTANCE_FANCY = 50;
    /** Same lines, the fast-graphics branch: {@code !gameSettings.field_74347_j} (vanilla "graphics: fast"). */
    public static final int VISIBLE_DISTANCE_FAST = 25;

    /** {@code FXWisp.java:39}: {@code (int)(36.0 / (Math.random() * 0.3 + 0.7))}. */
    public static final float WISP_LIFETIME_BASE = 36.0F;
    /** {@code FXWispEG.java:32}: {@code (int)(40.0 / (Math.random() * 0.3 + 0.7))}. */
    public static final float EG_LIFETIME_BASE = 40.0F;
    /** The denominator span of both lifetime rolls: {@code rand * 0.3 + 0.7}. */
    public static final double LIFETIME_JITTER_SPAN = 0.3;
    /** The denominator base of both lifetime rolls. */
    public static final double LIFETIME_JITTER_BASE = 0.7;

    /** {@code FXWisp.java:158-160}: the homing pull per tick toward the target. */
    public static final double SEEK_ACCELERATION = 0.2;
    /** {@code FXWisp.java:161-163}: the homing velocity clamp on every axis. */
    public static final float SEEK_CLAMP = 0.2F;
    /** {@code FXWisp.java:152-154}: per-tick drag while homing. */
    public static final float HOMING_DRAG = 0.985F;
    /** {@code FXWisp.java:165-167}: per-tick drag with no target. */
    public static final float FREE_DRAG = 0.98F;
    /** {@code FXWisp.java:168-171}: extra x/z drag while standing on ground. */
    public static final float GROUND_DRAG = 0.7F;
    /** {@code FXWispEG.java:78-81}: the EG wisp's ground x/z drag. */
    public static final float EG_GROUND_DRAG = 0.85F;
    /** {@code FXWispEG.java:24-26}: the EG wisp's initial x/z gaussian spread. */
    public static final float EG_SPREAD = 0.03F;
    /** {@code FXWispEG.java:25}: the EG wisp's initial fall. */
    public static final double EG_RISE = -0.05;
    /** {@code FXWispEG.java:27}: {@code particleScale *= 0.4F}. */
    public static final float EG_SCALE = 0.4F;
    /** {@code ClientProxy.sourceStreamFX} (ClientProxy.java:755): the arcing wisp's scale. */
    public static final float ARCING_SCALE = 0.1F;

    /** The number of colour branches in {@code FXWisp}'s type switch. */
    public static final int TYPE_COUNT = 8;

    private FxWispRules() {
    }

    /**
     * A wisp's spawn colour plus its blend layer. {@code FXWisp.java:100}
     * (case 5) switches to GL blendmode 771 (additive) — every other branch
     * keeps the alpha layer — which in 1.21.1 is a different particle
     * render-type, so the branch has to ride the colour together with it.
     *
     * @param r        red, 0..1
     * @param g        green, 0..1
     * @param b        blue, 0..1
     * @param additive true when the original sets blendmode 771
     */
    public record WispColor(float r, float g, float b, boolean additive) {
    }

    /**
     * The {@code FXWisp(World, x, y, z, scale, int type)} colour switch
     * (FXWisp.java:66-115). {@code type} is the sender's wisp flavour: 0 = wisp
     * purple, 1 = amber, 2/3/4 = the prim, 5 = the additive dark mote, 6 =
     * white, 7 = the fiery (air/fire/water-tinted) roll.
     */
    public static WispColor wispColor(final int type, final RandomSource rand) {
        switch (type) {
            case 0:
                return new WispColor(0.75F + rand.nextFloat() * 0.25F,
                        0.25F + rand.nextFloat() * 0.25F,
                        0.75F + rand.nextFloat() * 0.25F, false);
            case 1:
                return new WispColor(0.5F + rand.nextFloat() * 0.3F,
                        0.5F + rand.nextFloat() * 0.3F, 0.2F, false);
            case 2:
                return new WispColor(0.2F, 0.2F, 0.7F + rand.nextFloat() * 0.3F, false);
            case 3:
                return new WispColor(0.2F, 0.7F + rand.nextFloat() * 0.3F, 0.2F, false);
            case 4:
                return new WispColor(0.7F + rand.nextFloat() * 0.3F, 0.2F, 0.2F, false);
            case 5:
                // The additive branch: near-black on an additive layer.
                return new WispColor(rand.nextFloat() * 0.1F,
                        rand.nextFloat() * 0.1F, rand.nextFloat() * 0.1F, true);
            case 6:
                return new WispColor(0.8F + rand.nextFloat() * 0.2F,
                        0.8F + rand.nextFloat() * 0.2F,
                        0.8F + rand.nextFloat() * 0.2F, false);
            case 7:
            default:
                return new WispColor(0.7F + rand.nextFloat() * 0.3F,
                        0.5F + rand.nextFloat() * 0.2F,
                        0.3F + rand.nextFloat() * 0.1F, false);
        }
    }

    /**
     * The dark roll {@code FXWispEG} makes for itself (FXWispEG.java:44-46):
     * every channel is {@code rand * 0.05}, on the additive layer
     * ({@code blendmode = 771}, line 43).
     */
    public static WispColor wispEgColor(final RandomSource rand) {
        return new WispColor(rand.nextFloat() * 0.05F,
                rand.nextFloat() * 0.05F, rand.nextFloat() * 0.05F, true);
    }

    /** {@code FXWisp.java:39} / {@code FXWispArcing.java:41}: 36 / (rand*0.3 + 0.7), truncated. */
    public static int wispLifetime(final RandomSource rand) {
        return (int) (WISP_LIFETIME_BASE / lifetimeDivisor(rand));
    }

    /** {@code FXWispEG.java:32}: 40 / (rand*0.3 + 0.7), truncated. */
    public static int wispEgLifetime(final RandomSource rand) {
        return (int) (EG_LIFETIME_BASE / lifetimeDivisor(rand));
    }

    private static double lifetimeDivisor(final RandomSource rand) {
        return rand.nextFloat() * LIFETIME_JITTER_SPAN + LIFETIME_JITTER_BASE;
    }

    /**
     * The original's render-distance cull: a wisp older rule is
     * {@code visibleDistance = 50}, dropped to {@code 25} when the client runs
     * on fast graphics, and a wisp spawned beyond it gets
     * {@code particleAge = 0}, i.e. it is culled at spawn
     * (FXWisp.java:40-48, FXWispEG.java:35-41, FXWispArcing.java:40-48).
     *
     * <p>1.21.1's fancy-graphics flag is the same two-value setting
     * ({@code Options#graphicsMode}); the caller reads it and passes the
     * boolean, which keeps this function testable off the client.</p>
     */
    public static int visibleDistance(final boolean fancyGraphics) {
        return fancyGraphics ? VISIBLE_DISTANCE_FANCY : VISIBLE_DISTANCE_FAST;
    }

    /**
     * The cull as the plan applies it: the original zeroes the lifetime past
     * the visible distance, which is the same observable outcome as not
     * spawning the particle at all.
     */
    public static boolean culledAtSpawn(final double x, final double y, final double z,
            final double viewerX, final double viewerY, final double viewerZ,
            final int visibleDistance) {
        final double dx = x - viewerX;
        final double dy = y - viewerY;
        final double dz = z - viewerZ;
        return dx * dx + dy * dy + dz * dz > (double) visibleDistance * visibleDistance;
    }
}
