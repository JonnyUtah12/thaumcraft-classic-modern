package com.jonnyutah12.thaumcraftclassic.player;

/**
 * Every constant and rule of the warp-event mob-spawn seam, kept
 * {@code Level}-free so the unit test can pin them without a world.
 *
 * <p>Original: {@code thaumcraft.common.lib.WarpEvents}, 368 lines (read whole,
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/WarpEvents.java}),
 * plus its call site {@code common/lib/events/EventHandlerEntity.java:235-237}.
 * Ledger: the warp half of BD-75 (mind spider spawn seams), closed by t_1a83f0db.</p>
 *
 * <p>The cadence: the original drives {@code checkWarpEvent} from
 * {@code EventHandlerEntity.java:235-237} — server side, every 2000th player
 * tick, gated off by {@code Config.wuss} and the Warp Ward potion effect. The
 * gate roll, the warp formula, the counter decay, the whole event ladder, the
 * mob counts and the spawn-scan arithmetic are all pinned here.</p>
 */
public final class WarpEventsLogic {

    // ---- cadence (EventHandlerEntity.java:235-237) ----
    /** {@code player.field_70173_aa % 2000 == 0} — one event check per 2000 ticks. */
    public static final int TICK_PERIOD = 2000;

    /** {@code field_70173_aa > 0 && field_70173_aa % 2000 == 0} (:235). */
    public static boolean tickFires(final int ticksExisted) {
        return ticksExisted > 0 && ticksExisted % TICK_PERIOD == 0;
    }

    // ---- gate (WarpEvents.java:45-49) ----
    /** {@code :45} — {@code worldObj.rand.nextInt(100)}. */
    public static final int ROLL_BOUND = 100;

    /**
     * {@code :46} — {@code warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter)}.
     * The comparison is int-vs-double (the sqrt widens the counter).
     */
    public static boolean gatePasses(final int warp, final int warpCounter, final int roll) {
        return warpCounter > 0 && warp > 0 && roll <= Math.sqrt(warpCounter);
    }

    /** {@code :47} — {@code warp = Math.min(100, (warp + warp + warpCounter) / 3)} (int division). */
    public static int eventWarp(final int warp, final int warpCounter) {
        return Math.min(100, (warp + warp + warpCounter) / 3);
    }

    /** {@code :48} — {@code warpCounter = (int)(warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0))};
     *  the cast truncates toward zero (the counter is positive inside the gate). */
    public static int decayedCounter(final int warpCounter) {
        return (int) (warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0));
    }

    // ---- fortress helm discount (WarpEvents.java:51-58) ----
    /**
     * {@code :57} — with a Fortress helm wearing its {@code mask == 0} NBT,
     * {@code eff -= 2 + rand.nextInt(4)}. The discount term is
     * {@code 2 + nextInt(4)} ∈ [2, 5]; the helm item itself ships on TC-M6-07.
     */
    public static int fortressHelmDiscount(final int roll3) {
        return 2 + roll3;
    }

    /** {@code :50} — {@code eff = worldObj.rand.nextInt(warp)}; the warp event roll. */
    public static final int EVENT_ROLL_BOUND_MIN = 1; // warp > 0 is the gate (:46)

    // ---- the event ladder (WarpEvents.java:61-199) ----
    /**
     * One branch of the original's ladder. {@code SHIPS} records which branches
     * the live seam ({@link WarpEvents}) wires today: the mob arms, the mist FX
     * and the sticky relief. The potion arms need MobEffects no card has
     * registered; the research arms (GRANT_RESEARCH/GRANT_RESEARCH_TEN,
     * {@code :62-65}, {@code :190-194}) feed the research grant lane
     * (grantResearch {@code :241-255}: random primal aspect pools);
     * the chat-only arms ship keys the lang file does not carry yet.
     */
    public enum Arm {
        NONE,
        GRANT_RESEARCH,       // :62-65  eff 1..4   grantResearch(1) + warp.text.3
        CHAT_11,              // :66-67  eff 9..12
        VISEXHAUST,           // :68-78  eff 13..16 5000 t, amp min(3, warp/15)
        THAUMARHIA,           // :80-89  eff 17..20 min(32000, 10*warp)
        UNHUNGER_5000,        // :91-102 eff 21..24 5000 t, amp min(3, warp/15)
        CHAT_12,              // :104    eff 25..28
        MIST_ONE,             // :106    eff 29..32 spawnMist(warp, 1)
        BLURRED,              // :109-113 eff 33..36 min(32000, 10*warp)
        SUNSCORNED,           // :114-123 eff 37..40 5000 t, amp min(3, warp/15)
        STARVATION,           // :126-131 eff 41..44 Potion.starvation 1200 t
        INF_VISEXHAUST,       // :133-142 eff 45..48 6000 t, amp min(3, warp/15)
        WEAKNESS,             // :144-145 eff 49..52 min(40*warp, 6000)
        DEATHGAZE,            // :147-156 eff 53..56 6000 t, amp min(3, warp/15)
        SPIDERS_ILLUSION,     // :158    eff 57..60 suddenlySpiders(warp, false)
        CHAT_13,              // :160    eff 61..64
        MIST_W30,             // :162    eff 65..68 spawnMist(warp, warp / 30)
        SLOWNESS,             // :165-168 eff 69..72 min(32000, 5*warp)
        STICKY_RELIEF,        // :169-176 eff == 76  addWarpSticky(-1) + sync + warp.text.14
        UNHUNGER_6000,        // :177-189 eff 73..75 and 77..80, 6000 t
        GRANT_RESEARCH_TEN,   // :190-194 eff 81..84 grantResearch(warp / 10) + warp.text.3
        SPIDERS_REAL,         // :195    eff 89..92 suddenlySpiders(warp, true)
        MIST_W15,             // :197    eff 93..   spawnMist(warp, warp / 15)
    }

    /** Arms the live seam wires (the mob spawns, the mist FX, the sticky relief). */
    public static boolean ships(final Arm arm) {
        return arm == Arm.SPIDERS_ILLUSION || arm == Arm.SPIDERS_REAL
                || arm == Arm.MIST_ONE || arm == Arm.MIST_W30 || arm == Arm.MIST_W15
                || arm == Arm.STICKY_RELIEF;
    }

    /**
     * The ladder of {@code :61-199}, branch for branch. The original's chain is
     * {@code if (eff > 0) { if (eff <= 4) … else if (eff > 8) { … } }}; eff 5..8
     * and 85..88 fall through every branch (FX0 at {@code :60} and the tail at
     * {@code :224-225} still run), and eff 73..75 land in the {@code eff <= 80}
     * branch because {@code eff == 76} is an equality arm, not a range.
     */
    public static Arm armFor(final int eff) {
        if (eff <= 0) {
            return Arm.NONE;                                       // :61 (eff > 0 gate)
        }
        if (eff <= 4) {
            return Arm.GRANT_RESEARCH;                             // :62
        }
        if (eff <= 8) {
            return Arm.NONE;                                       // no branch (:65 eff > 8 gate)
        }
        if (eff <= 12) {
            return Arm.CHAT_11;                                    // :66
        }
        if (eff <= 16) {
            return Arm.VISEXHAUST;                                 // :69
        }
        if (eff <= 20) {
            return Arm.THAUMARHIA;                                 // :80
        }
        if (eff <= 24) {
            return Arm.UNHUNGER_5000;                              // :91
        }
        if (eff <= 28) {
            return Arm.CHAT_12;                                    // :104
        }
        if (eff <= 32) {
            return Arm.MIST_ONE;                                   // :106
        }
        if (eff <= 36) {
            return Arm.BLURRED;                                    // :109
        }
        if (eff <= 40) {
            return Arm.SUNSCORNED;                                 // :114
        }
        if (eff <= 44) {
            return Arm.STARVATION;                                 // :126
        }
        if (eff <= 48) {
            return Arm.INF_VISEXHAUST;                             // :133
        }
        if (eff <= 52) {
            return Arm.WEAKNESS;                                   // :144
        }
        if (eff <= 56) {
            return Arm.DEATHGAZE;                                  // :147
        }
        if (eff <= 60) {
            return Arm.SPIDERS_ILLUSION;                           // :158
        }
        if (eff <= 64) {
            return Arm.CHAT_13;                                    // :160
        }
        if (eff <= 68) {
            return Arm.MIST_W30;                                   // :162
        }
        if (eff <= 72) {
            return Arm.SLOWNESS;                                   // :165
        }
        if (eff == 76) {
            return Arm.STICKY_RELIEF;                              // :169 (equality arm — 73..75 fall past it)
        }
        if (eff <= 80) {
            return Arm.UNHUNGER_6000;                              // :177 (carries eff 73..75 and 77..80)
        }
        if (eff <= 84) {
            return Arm.GRANT_RESEARCH_TEN;                         // :189
        }
        if (eff <= 88) {
            return Arm.NONE;                                       // :193 (eff > 88 gate)
        }
        if (eff <= 92) {
            return Arm.SPIDERS_REAL;                               // :195
        }
        return Arm.MIST_W15;                                       // :197
    }

    /** The guardian count the arm asks for: 1 ({@code :106}), warp/30 ({@code :162}), warp/15 ({@code :197}). */
    public static int guardianRoll(final Arm arm, final int warp) {
        return switch (arm) {
            case MIST_ONE -> 1;
            case MIST_W30 -> warp / 30;
            case MIST_W15 -> warp / 15;
            default -> 0;
        };
    }

    /** {@code :231} — {@code guardian = Math.min(8, guardian)} (only when {@code > 0}, :230). */
    public static int guardianCount(final Arm arm, final int warp) {
        final int rolled = guardianRoll(arm, warp);
        return rolled > 0 ? Math.min(8, rolled) : 0;
    }

    // ---- spider spawn count (WarpEvents.java:282) ----
    /** {@code :282} — {@code int spawns = Math.min(50, warp)} (inside {@code suddenlySpiders}). */
    public static final int MAX_SPAWNS = 50;

    public static int spiderSpawns(final int warp) {
        return Math.min(MAX_SPAWNS, warp);
    }

    // ---- spawn-scan arithmetic (WarpEvents.java:257-280 spawnGuardian, :281-322 suddenlySpiders) ----
    /** {@code :263/:291} — {@code for (int l = 0; l < 50; l++)} — attempts per mob. */
    public static final int SCAN_ATTEMPTS = 50;

    /** {@code :264-266/:292-297} — {@code MathHelper.func_76136_a(rand, 7, 24)} per axis. */
    public static final int OFFSET_MIN = 7;
    public static final int OFFSET_MAX = 24;

    /** {@code :264-266/:292-297} — the sign term, {@code func_76136_a(rand, -1, 1)}. */
    public static final int SIGN_MIN = -1;
    public static final int SIGN_MAX = 1;

    // ---- tail (WarpEvents.java:224-225) ----
    /** {@code :224} — {@code addWarpTemp(name, -1)} on every check, gate passed or not. */
    public static final int TAIL_TEMP_DECAY = -1;

    /** The sticky-relief delta ({@code :172} — {@code addWarpSticky(name, -1)}), guarded by sticky > 0. */
    public static final int STICKY_RELIEF_DELTA = -1;

    // ---- chat keys (the original's StatCollector keys, shipped by the wired arms) ----
    /** {@code :238} — the mist line, {@code warp.text.6}. */
    public static final String KEY_MIST = "warp.text.6";
    /** {@code :321} — the spider line, {@code warp.text.7}. */
    public static final String KEY_SPIDERS = "warp.text.7";
    /** {@code :176} — the sticky relief line, {@code warp.text.14}. */
    public static final String KEY_RELIEF = "warp.text.14";

    private WarpEventsLogic() {
    }
}
