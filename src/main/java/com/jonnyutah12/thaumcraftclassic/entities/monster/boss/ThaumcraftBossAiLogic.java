package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;

/**
 * The shared boss rules of {@code thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss}
 * (259 lines), as a Level-free rule class so every figure is pinned by
 * {@code src/test/java/.../entities/monster/boss/ThaumcraftBossAiLogicTest.java} without a world
 * (the family's AiLogic pattern — see {@code EldritchGuardianAiLogic}).
 *
 * <p>Every line citation below is the 1.7.10 decompile at
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityThaumcraftBoss.java}
 * unless the file is named.</p>
 */
public final class ThaumcraftBossAiLogic {

    private ThaumcraftBossAiLogic() {
    }

    // ---------------------------------------------------------------- attributes

    /**
     * {@code :31} — {@code field_70728_aV = 50}: the base experience value every boss
     * carries, before a subclass overwrites it (the Leader sets 40 at {@code EntityCultistLeader:66},
     * the Portal 30 at {@code EntityCultistPortal:39}).
     */
    public static final int EXPERIENCE = 50;

    /**
     * {@code :53} — {@code func_110148_a(field_111266_c).func_111128_a(0.95)}:
     * knockback resistance 0.95. The MCP field is {@code field_111266_c} = knockbackResistance
     * ({@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:297-301}).
     */
    public static final double KNOCKBACK_RESISTANCE = 0.95;

    /**
     * {@code :54} — {@code func_110148_a(field_111265_b).func_111128_a(40.0)}: follow range 40.0.
     * The eldritch golem stub TC-M6-08-s-c landed left this at {@code Monster}'s 16.0, so the boss
     * body carries the parity fix (recorded in the row's note).
     */
    public static final double FOLLOW_RANGE = 40.0;

    // ---------------------------------------------------------------- state indices

    /** {@code :59} — datawatcher index 14, a {@code Short}: the rage timer. */
    public static final int ANGER_WATCHER_INDEX = 14;
    /** {@code :91-97} — the rage timer decays one per tick. */
    public static final int ANGER_RAGE_TICKS = 200;
    /** {@code :91-93} — the spawn-in timer, set by the subclasses (golem 100, warden 150). */
    public static final int SPAWN_TIMER_MIN = 0;

    /** {@code :72-75} — {@code func_110171_b(x, y, z, 24)}: the spawn home radius. */
    public static final int HOME_RADIUS = 24;

    /** {@code :116-118} — every 30 ticks the boss heals 1.0F. */
    public static final int HEAL_PERIOD_TICKS = 30;
    public static final float HEAL_AMOUNT = 1.0F;

    /** {@code :120} — the aggro/retarget pass runs every 20 ticks, with a target. */
    public static final int AGGRO_PERIOD_TICKS = 20;

    /** {@code :132} — {@code !(func_70068_e(newTarget) > 16384.0)}: 128 blocks, squared. */
    public static final double AGGRO_MAX_DISTANCE_SQ = 16384.0;

    /** {@code :130} — the challenger must beat the incumbent by 25 raw ... */
    public static final int AGGRO_MIN_GAIN = 25;
    /** {@code :130} — ... and by 10 % of it. */
    public static final double AGGRO_RATIO = 1.1;

    /** {@code :99} — the client anger puff fires on a 1-in-15 roll. */
    public static final int ANGER_PARTICLE_CHANCE = 15;

    // ---------------------------------------------------------------- rage

    /** {@code :234} — the enrage threshold: a hit over 35.0F. */
    public static final float ENRAGE_THRESHOLD_DAMAGE = 35.0F;
    /** {@code :250} — and every such hit is CLAMPED to 35.0F. */
    public static final float DAMAGE_CLAMP = 35.0F;
    /** {@code :237} — the rage potion runs 200 ticks. */
    public static final int RAGE_POTION_TICKS = 200;
    /** {@code :237} — haste amplifier is {@code (int)(damage / 15.0F)}. */
    public static final float RAGE_HASTE_DIVISOR = 15.0F;
    /** {@code :238-239} — resistance and strength amplifiers are {@code (int)(damage / 40.0F)}. */
    public static final float RAGE_RESIST_DIVISOR = 40.0F;

    /**
     * The rage verdict of {@code :234-251}. The gate is the ORIGINAL's pair
     * ({@code damage > 35.0F} and, for the effects, {@code getAnger() == 0}); the clamp is
     * unconditional inside the {@code damage > 35.0F} arm, so a re-rage hit is still capped.
     *
     * <p>1.7.10's {@code Potion.field_76428_l} = haste, {@code field_76420_g} = resistance,
     * {@code field_76424_c} = strength (resolved from the port's own potion names).</p>
     */
    public record Rage(boolean enrages, int hasteAmplifier, int resistanceAmplifier,
            int strengthAmplifier, float damageAfterClamp) {

        public static final Rage NONE = new Rage(false, 0, 0, 0, 0.0F);
    }

    public static Rage rageFor(final float damage, final int anger) {
        if (damage <= ENRAGE_THRESHOLD_DAMAGE) {                                        // :234
            return new Rage(false, 0, 0, 0, damage);
        }
        if (anger != 0) {                                                               // :235
            return new Rage(false, 0, 0, 0, DAMAGE_CLAMP);                              // :250
        }
        return new Rage(true,
                (int) (damage / RAGE_HASTE_DIVISOR),                                    // :237
                (int) (damage / RAGE_RESIST_DIVISOR),                                   // :238
                (int) (damage / RAGE_RESIST_DIVISOR),                                   // :239
                DAMAGE_CLAMP);                                                          // :250
    }

    // ---------------------------------------------------------------- aggro map

    /**
     * The aggro bookkeeping of {@code :222-232}: the raw damage is recorded per attacker
     * entity id, summed with any previous entry ({@code (int)damage + previous}).
     */
    public static int aggroEntry(final Map<Integer, Integer> aggro, final int attackerId,
            final float damage) {
        int ad = (int) damage;                                                          // :226
        final Integer previous = aggro.get(attackerId);
        if (previous != null) {
            ad += previous;                                                             // :227-229
        }
        return ad;                                                                      // :231
    }

    /**
     * The retarget pass of {@code :121-150}. The returned scan is what the entity applies.
     *
     * <p>The original's own quirks are transcribed verbatim, not "fixed":</p>
     * <ul>
     *   <li>{@code :131} resolves {@code world.func_73045_a(hei)} — the CURRENT incumbent id,
     *       not the candidate {@code ei} — so the liveness/distance test is against the
     *       incumbent while the score comes from the candidate;</li>
     *   <li>{@code :134} sets {@code ld = ei}, the entity id, not the score;</li>
     *   <li>{@code :135-137} counts {@code players} only inside the arm that switches the
     *       incumbent, which is why a boss with no challenger keeps its old buff count.</li>
     * </ul>
     */
    public record AggroScan(int targetId, int players, List<Integer> dropIds, boolean switched) {
    }

    public static AggroScan scanAggro(final Map<Integer, Integer> aggro, final int currentId,
            final int currentScore, final IntFunction<net.minecraft.world.entity.Entity> byId,
            final ToDoubleFunction<net.minecraft.world.entity.Entity> distanceSquared) {
        final List<Integer> dropIds = new ArrayList<>();
        int players = 0;
        int hei = currentId;
        int ad = currentScore;
        int ld = ad;                                                                    // :125
        boolean haveCandidate = false;                                                  // :126 newTarget != null

        for (final Map.Entry<Integer, Integer> entry : aggro.entrySet()) {              // :128
            final int ei = entry.getKey();
            final int ca = entry.getValue();
            if (ca > ad + AGGRO_MIN_GAIN && ca > ad * AGGRO_RATIO && ca > ld) {         // :130
                final net.minecraft.world.entity.Entity newTarget = byId.apply(hei);    // :131 — hei, verbatim
                if (newTarget != null && !newTarget.isRemoved()
                        && distanceSquared.applyAsDouble(newTarget) <= AGGRO_MAX_DISTANCE_SQ) { // :132
                    hei = ei;                                                           // :133
                    ld = ei;                                                            // :134 — verbatim
                    if (newTarget instanceof net.minecraft.world.entity.player.Player) { // :135
                        players++;                                                      // :136
                    }
                    haveCandidate = true;
                } else {
                    dropIds.add(ei);                                                    // :139
                }
            }
        }

        boolean switched = false;
        if (haveCandidate && hei != currentId) {                                        // :148
            switched = true;
        }
        return new AggroScan(hei, players, dropIds, switched);
    }

    // ---------------------------------------------------------------- player scaling

    /** {@code :5 x 50.0 additive max health} modifiers, ids from {@code EntityUtils.java:43-49}. */
    public static final AttributeModifier[] HPBUFF = {
        buff("health_buff_1", 50.0),                                                    // EntityUtils:44
        buff("health_buff_2", 50.0),                                                    // EntityUtils:45
        buff("health_buff_3", 50.0),                                                    // EntityUtils:46
        buff("health_buff_4", 50.0),                                                    // EntityUtils:47
        buff("health_buff_5", 50.0),                                                    // EntityUtils:48
    };

    /** {@code 5 x 0.5 additive attack damage} modifiers, ids from {@code EntityUtils.java:50-56}. */
    public static final AttributeModifier[] DMGBUFF = {
        buff("damage_buff_1", 0.5),                                                     // EntityUtils:51
        buff("damage_buff_2", 0.5),                                                     // EntityUtils:52
        buff("damage_buff_3", 0.5),                                                     // EntityUtils:53
        buff("damage_buff_4", 0.5),                                                     // EntityUtils:54
        buff("damage_buff_5", 0.5),                                                     // EntityUtils:55
    };

    /**
     * The 1.7.10 modifiers are keyed by a fixed {@code UUID} ({@code EntityUtils.java:43-56});
     * 1.21.1's {@link AttributeModifier} record keys by {@link ResourceLocation}
     * ({@code AttributeModifier(ResourceLocation, double, Operation)}), so the original's
     * ten UUIDs become the ten ids below, name-for-name, and the pair of arrays is the same
     * 5 additive modifiers. The health/damage attributes are the 1.21.1 holders of
     * {@code field_111267_a} / {@code field_111264_e}.
     */
    private static AttributeModifier buff(final String path, final double amount) {
        return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path),
                amount, AttributeModifier.Operation.ADD_VALUE);                         // op 0 = additive
    }

    /** {@code :161} — the buff count is {@code Math.min(5, players - 1)}. */
    public static int buffCount(final int players) {
        return Math.min(5, players - 1);
    }

    /** {@code :166-167} — the current health is rescaled by the max-health ratio. */
    public static float rescaledHealth(final double oldMax, final double newMax, final float health) {
        final double mm = newMax / oldMax;
        return (float) (health * mm);
    }

    // ---------------------------------------------------------------- hooks

    /** {@code :172-173} — invulnerable while the spawn-in timer runs. */
    public static boolean invulnerableForSpawnTimer(final int spawnTimer) {
        return spawnTimer > 0;
    }

    /** {@code :180} — {@code func_70104_M() = super && !isInvulnerable()}: pushable only when vulnerable. */
    public static boolean pushableForVulnerable(final boolean vulnerable) {
        return vulnerable;
    }

    /** {@code :99} — the client anger-puff roll (the {@code field_72995_K} half is the caller's). */
    public static boolean angerParticleFires(final int roll) {
        return roll == 0;
    }

    /** {@code :191} — {@code func_98052_bS()} false: bosses pick up nothing. */
    public static final boolean CAN_PICK_UP_LOOT = false;

    /** {@code :205} — {@code func_70692_ba()} false: bosses drop no death loot. */
    public static final boolean DAPS_LOOT = false;

    /** The attributes the base's {@code createAttributes} adds on top of {@code Monster}'s. */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder
            onMonsterAttributes(
            final net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder builder) {
        return builder
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)             // :53
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);                            // :54
    }
}
