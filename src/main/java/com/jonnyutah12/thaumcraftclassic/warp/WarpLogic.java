package com.jonnyutah12.thaumcraftclassic.warp;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * The warp-event roll, arithmetic and effect table (TC-M6-05-s).
 *
 * <p>Original: {@code thaumcraft/common/lib/WarpEvents.java} lines 39-225
 * ({@code checkWarpEvent}) and 324-347 ({@code checkDeathGaze}). This class is
 * the arithmetic of those two methods with every world, packet, chat, entity
 * and capability read stripped out, so the whole table is unit-testable
 * ({@code warp/WarpLogicTest}); {@link WarpEvents} is the thin driver that
 * performs the reads and applies the outcomes.</p>
 *
 * <h2>Cadence and gates</h2>
 * The original calls the roll from {@code EventHandlerEntity.livingTick}
 * ({@code EventHandlerEntity.java:236}): server side,
 * {@code !Config.wuss && ticksExisted > 0 && ticksExisted % 2000 == 0 &&
 * !hasEffect(warpWard)}; the death gaze runs from the same tick method at
 * {@code ticksExisted % 10 == 0} while the effect is active
 * ({@code EventHandlerEntity.java:241}), with no wuss gate. The wuss switch is
 * {@code TcConfigCatalog.WUSS_MODE} and the ward is
 * {@code ModMobEffects.POTION_WARWARD} in this port.</p>
 *
 * <h2>The roll</h2>
 * The original gates on {@code warpCounter > 0 && warp > 0 && rand(100) <=
 * sqrt(warpCounter)} where {@code warp} is the warp total <em>plus gear warp</em>
 * ({@code WarpEvents.java:44-47}); a fired event rewrites
 * {@code warp = min(100, (warp*3 + warpCounter)/3)} and
 * {@code warpCounter -= max(5, sqrt(warpCounter)*2)}
 * ({@code WarpEvents.java:49-52}), then draws {@code eff = rand(warp)}
 * (line 53) and reads the table below. The temporary-warp decay is
 * {@code addWarpTemp(-1)} in the method tail — it runs on every event, fired or
 * not ({@code WarpEvents.java:224-225}); there is no sticky decay in this
 * method other than the {@code eff == 76} relief branch (line 171).</p>
 *
 * <p>The original's fortress-helmet mask bonus ({@code WarpEvents.java:55-60}:
 * {@code eff -= 2 + rand(4)} for an enchanted mask) is armour content and stays
 * on TC-M8; the port passes the unadjusted {@code eff}.</p>
 */
public final class WarpLogic {

    /** Original {@code player.field_70173_aa % 2000 == 0} (EventHandlerEntity.java:236). */
    public static final int WARP_EVENT_INTERVAL_TICKS = 2000;

    /** Original {@code player.field_70173_aa % 10 == 0} (EventHandlerEntity.java:241). */
    public static final int DEATH_GAZE_INTERVAL_TICKS = 10;

    /** Original {@code Math.min(100, ...)} cap on the roll's warp (WarpEvents.java:49). */
    public static final int MAX_ROLL_WARP = 100;

    /** Original tail: {@code addWarpTemp(player, -1)} (WarpEvents.java:224). */
    public static final int TEMP_WARP_DECAY_PER_EVENT = 1;

    /** Original {@code Math.min(3, warp / 15)} amplifier in every effect branch. */
    public static final int EFFECT_LEVEL_DIVISOR = 15;

    /** Original {@code Math.min(3, ...)} amplifier cap. */
    public static final int MAX_EFFECT_AMPLIFIER = 3;

    /** Original {@code Math.min(8 + level * 3, 24)} (WarpEvents.java:328). */
    public static final int DEATH_GAZE_BASE_RANGE = 8;
    public static final int DEATH_GAZE_RANGE_PER_LEVEL = 3;
    public static final int DEATH_GAZE_MAX_RANGE = 24;

    /** Original {@code EntityUtils.isVisibleTo(0.75F, ...)} aperture (WarpEvents.java:332). */
    public static final float DEATH_GAZE_APERTURE_RADIANS = 0.75F;

    /** Original {@code new PotionEffect(Potion.wither..., 80)} (WarpEvents.java:344). */
    public static final int DEATH_GAZE_WITHER_TICKS = 80;

    /** Original {@code Math.min(8, guardian)} (WarpEvents.java:230). */
    public static final int MAX_GUARDIANS = 8;

    /** Original {@code Math.min(50, warp)} spider count (WarpEvents.java:281). */
    public static final int MAX_SPIDERS = 50;

    /** What a warp event does. The names carry the original's branch. */
    public enum Kind {
        /** No branch matched (eff 0, the 5-8 gap, eff 85-88). */
        NONE,
        /** {@code eff <= 4}: one primal aspect point, {@code warp.text.3}. */
        RESEARCH,
        /** {@code eff <= 12} / {@code <= 28} / {@code <= 64}: chat only. */
        CHAT,
        /** {@code eff <= 16}: {@code thaumcraft:potion.visexhaust}. */
        VIS_EXHAUST,
        /** {@code eff <= 20}: {@code thaumcraft:potion.thaumarhia}. */
        THAUMARHIA,
        /** {@code eff <= 24} / {@code <= 80}: {@code thaumcraft:potion.unhunger}. */
        UNNATURAL_HUNGER,
        /** {@code eff <= 32} / {@code <= 68} / {@code > 92}: the mist. */
        MIST,
        /** {@code eff <= 36}: {@code thaumcraft:potion.blurred}. */
        BLURRED_VISION,
        /** {@code eff <= 40}: {@code thaumcraft:potion.sunscorned}. */
        SUN_SCORNED,
        /** {@code eff <= 44}: vanilla {@code minecraft:dig_slowdown}. */
        DIG_SLOWDOWN,
        /** {@code eff <= 48}: {@code thaumcraft:potion.infvisexhaust}. */
        INFECTIOUS_VIS_EXHAUST,
        /** {@code eff <= 52}: vanilla {@code minecraft:night_vision}. */
        NIGHT_VISION,
        /** {@code eff <= 56}: {@code thaumcraft:potion.deathgaze}. */
        DEATH_GAZE,
        /** {@code eff <= 60}: illusion spiders; {@code eff <= 92}: real ones. */
        SPIDERS,
        /** {@code eff == 76}: one sticky warp back off. */
        STICKY_RELIEF,
        /** {@code eff <= 72}: vanilla {@code minecraft:blindness}. */
        BLINDNESS
    }

    /**
     * One branch's outcome. {@code langKey} is the original's
     * {@code StatCollector func_74838_a("warp.text.N")} key (null for the two
     * silent branches, blurred vision and blindness). {@code times} is the
     * {@code grantResearch} argument; {@code guardians}/{@code spiders} are the
     * spawn counts the original computed.
     */
    public record WarpOutcome(Kind kind, String langKey, int duration, int amplifier, int times, int guardians) {

        public static final WarpOutcome NONE = new WarpOutcome(Kind.NONE, null, 0, 0, 0, 0);
    }

    private WarpLogic() {
    }

    /**
     * The original's roll gate ({@code WarpEvents.java:44-47}):
     * {@code warpCounter > 0 && warp > 0 && rand(100) <= sqrt(warpCounter)}.
     *
     * @param warp        warp total <b>plus</b> gear warp
     * @param warpCounter the eldritch counter
     * @param roll        a 0-99 draw
     */
    public static boolean fires(final int warp, final int warpCounter, final int roll) {
        return warpCounter > 0 && warp > 0 && roll <= Math.sqrt(warpCounter);
    }

    /** Original {@code Math.min(100, (warp + warp + warpCounter) / 3)} (line 49). */
    public static int rollWarp(final int warp, final int warpCounter) {
        return Math.min(MAX_ROLL_WARP, (warp + warp + warpCounter) / 3);
    }

    /**
     * Original {@code (int)(warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0))}
     * (line 50) — the counter is a float cast, so the subtraction is truncated.
     */
    public static int rollCounter(final int warpCounter) {
        return (int) (warpCounter - Math.max(5.0D, Math.sqrt(warpCounter) * 2.0D));
    }

    /** Original {@code player.world.rand.nextInt(warp)} (line 53); 0 when warp is 0. */
    public static int rollEff(final RandomSource random, final int warp) {
        return warp <= 0 ? 0 : random.nextInt(warp);
    }

    /**
     * Original amplifier {@code Math.min(3, warp / 15)} (every effect branch).
     * The original stores amplifier+1 as its potion "level"; 1.21.1
     * {@link net.minecraft.world.effect.MobEffectInstance} takes the 0-based
     * amplifier directly, so the value maps 1:1.
     */
    public static int effectAmplifier(final int warp) {
        return Math.min(MAX_EFFECT_AMPLIFIER, warp / EFFECT_LEVEL_DIVISOR);
    }

    /**
     * The whole {@code if (eff > 0)} chain of {@code WarpEvents.java:62-207},
     * in order, with the original's thresholds. {@code eff == 76} is its own
     * branch, so 73-75 fall through to the {@code eff <= 80} unnatural-hunger
     * branch exactly as the original's if-chain does.
     */
    public static WarpOutcome outcome(final int eff, final int warp) {
        if (eff <= 0) {
            return WarpOutcome.NONE;
        }
        if (eff <= 4) {
            return new WarpOutcome(Kind.RESEARCH, "warp.text.3", 0, 0, 1, 0);
        }
        if (eff <= 8) {
            return WarpOutcome.NONE;
        }
        if (eff <= 12) {
            return new WarpOutcome(Kind.CHAT, "warp.text.11", 0, 0, 0, 0);
        }
        if (eff <= 16) {
            return new WarpOutcome(Kind.VIS_EXHAUST, "warp.text.1", 5000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 20) {
            return new WarpOutcome(Kind.THAUMARHIA, "warp.text.15", Math.min(32000, 10 * warp), 0, 0, 0);
        }
        if (eff <= 24) {
            return new WarpOutcome(Kind.UNNATURAL_HUNGER, "warp.text.2", 5000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 28) {
            return new WarpOutcome(Kind.CHAT, "warp.text.12", 0, 0, 0, 0);
        }
        if (eff <= 32) {
            return new WarpOutcome(Kind.MIST, "warp.text.6", 0, 0, 0, 1);
        }
        if (eff <= 36) {
            return new WarpOutcome(Kind.BLURRED_VISION, null, Math.min(32000, 10 * warp), 0, 0, 0);
        }
        if (eff <= 40) {
            return new WarpOutcome(Kind.SUN_SCORNED, "warp.text.5", 5000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 44) {
            return new WarpOutcome(Kind.DIG_SLOWDOWN, "warp.text.9", 1200, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 48) {
            return new WarpOutcome(Kind.INFECTIOUS_VIS_EXHAUST, "warp.text.1", 6000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 52) {
            return new WarpOutcome(Kind.NIGHT_VISION, "warp.text.10", Math.min(40 * warp, 6000), 0, 0, 0);
        }
        if (eff <= 56) {
            return new WarpOutcome(Kind.DEATH_GAZE, "warp.text.4", 6000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 60) {
            return new WarpOutcome(Kind.SPIDERS, "warp.text.7", 0, 0, 0, 0);
        }
        if (eff <= 64) {
            return new WarpOutcome(Kind.CHAT, "warp.text.13", 0, 0, 0, 0);
        }
        if (eff <= 68) {
            return new WarpOutcome(Kind.MIST, "warp.text.6", 0, 0, 0, warp / 30);
        }
        if (eff <= 72) {
            return new WarpOutcome(Kind.BLINDNESS, null, Math.min(32000, 5 * warp), 0, 0, 0);
        }
        if (eff == 76) {
            return new WarpOutcome(Kind.STICKY_RELIEF, "warp.text.14", 0, 0, 0, 0);
        }
        if (eff <= 80) {
            return new WarpOutcome(Kind.UNNATURAL_HUNGER, "warp.text.2", 6000, effectAmplifier(warp), 0, 0);
        }
        if (eff <= 84) {
            return new WarpOutcome(Kind.RESEARCH, "warp.text.3", 0, 0, warp / 10, 0);
        }
        if (eff <= 88) {
            return WarpOutcome.NONE;
        }
        if (eff <= 92) {
            return new WarpOutcome(Kind.SPIDERS, "warp.text.7", 0, 0, 0, -1);
        }
        return new WarpOutcome(Kind.MIST, "warp.text.6", 0, 0, 0, warp / 15);
    }

    /**
     * Original {@code addWarpTemp(player, -1)} in the method tail
     * ({@code WarpEvents.java:224}). The original does not clamp, and the
     * ported {@code PlayerWarpData} does not either, so a 0-temp player goes to
     * -1 exactly as the original's {@code .thaum} file recorded it.
     */
    public static int tempWarpAfterEvent(final int warpTemp) {
        return warpTemp - TEMP_WARP_DECAY_PER_EVENT;
    }

    /**
     * Original {@code Math.min(8 + level * 3, 24)} ({@code WarpEvents.java:328}),
     * where the original's {@code level} is
     * {@code PotionEffect.func_76458_c()} — the 1-based potion level, one above
     * the 1.21.0 amplifier. The port adds the 1 back, so amplifier 0 gives the
     * original's level-1 range of 11.
     */
    public static int deathGazeRange(final int amplifier) {
        return Math.min(DEATH_GAZE_BASE_RANGE + (amplifier + 1) * DEATH_GAZE_RANGE_PER_LEVEL,
                DEATH_GAZE_MAX_RANGE);
    }

    /** Original {@code Math.min(8, guardian)} ({@code WarpEvents.java:230}). */
    public static int guardianCount(final int guardians) {
        return Math.min(MAX_GUARDIANS, Math.max(0, guardians));
    }

    /** Original {@code Math.min(50, warp)} ({@code WarpEvents.java:281}). */
    public static int spiderCount(final int warp) {
        return Math.min(MAX_SPIDERS, warp);
    }

    /**
     * The original's {@code EntityUtils.isVisibleTo(0.75F, player, entity,
     * range)} ({@code WarpEvents.java:332}): the target lies inside a cone of
     * {@code aperture} radians about the viewer's look vector, within
     * {@code range}. Same cone arithmetic as the shipped
     * {@code items/ItemCompassStone} scan (its {@code lyingInCone}), expressed
     * here for the player's look vector.
     */
    public static boolean coneVisible(final Vec3 apex, final Vec3 look, final Vec3 target,
                                      final double aperture, final double range) {
        final Vec3 axis = look;
        final Vec3 apexToTarget = target.subtract(apex);
        final double apexLen = apexToTarget.length();
        final double axisLen = axis.length();
        if (apexLen == 0.0D || axisLen == 0.0D) {
            return false;
        }
        final double projection = apexToTarget.dot(axis) / axisLen;
        final boolean insideInfiniteCone = apexToTarget.dot(axis) / apexLen / axisLen
                > Math.cos(aperture / 2.0D);
        return insideInfiniteCone && projection < range && projection >= 0.0D;
    }

    /** The original's {@code MathHelper.floor_double} of the player position. */
    public static int floor(final double value) {
        return Mth.floor(value);
    }
}
