package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWispRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoreSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaSourceMap;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime proof of the FX <em>sender</em> wirings on the game-test server
 * (TC-M7-02-c-s): the wisp family's plan constructors, the typed-wisp colour
 * table, the dig / essentia-source / infusion-source senders, the shield runes
 * and the client-tick drain map.
 *
 * <p>Same seam as {@link FxParticleGameTest}: the original's whole FX path sits
 * behind {@code world.field_72995_K} (client-only), so the spawn plan —
 * positions, motion, colour, lifetime, entity target, frame count — is the part
 * a dedicated server can prove, and it is transcribed from the decompiled
 * reference with the original random-draw order (a seeded {@link RandomSource}
 * reproduces a burst exactly, which these probes assert). The pixels need a
 * human look and are recorded in docs/feature-tracking.md.</p>
 *
 * <h2>Ported onto main's API (card t_982f53d5)</h2>
 * This is the wisps head's ({@code origin/wt/m7-fx-wisps} 535d816) sender suite,
 * retargeted to the names main's FX lane landed: {@code wispFx}/{@code wispFx3}
 * → {@link TcFxPlans#wisp}, {@code wispFx2}/{@code wispFx4} →
 * {@link TcFxPlans#wisp} (entity target) and {@link TcFxPlans#wispByType},
 * {@code wispFxEg} → {@link TcFxPlans#wispEg}, {@code sourceStreamFX} →
 * {@link TcFxPlans#wispArcing}, {@code wispTypeColor}/{@code wispTypeAdditive}/
 * {@code wispLifetime} → {@link FxWispRules#wispColor} (main consolidated the
 * colour table out of the plan) and {@code shieldRune} →
 * {@link TcFxPlans#shieldRunes}, which takes a {@code Level} and reads the aim
 * off the real source/target entities.
 *
 * <h2>Deferred subjects, with the sender each one needs</h2>
 * Three of the head's twelve subjects have no landed trigger path and are
 * deliberately NOT ported; each is named here with the sender that owns it, so
 * the next card can pick it up:
 * <ul>
 *   <li>{@code visDrainMoteHomesAtConsumer} — owner {@code PacketFXVisDrain}
 *       (PacketFXVisDrain.java:59-73 → {@code ClientProxy.visFx}). The
 *       {@code vis_sparkle} type is registered and {@code TcVisSparkleOptions}
 *       is landed, but no sender on main feeds the plan: {@code PacketFXVisDrain}
 *       has no landed counterpart in {@code network}, so a probe would pin a
 *       plan nobody calls.</li>
 *   <li>{@code sonicPulseRidesItsEntity} — owner {@code PacketFXSonic}
 *       (PacketFXSonic.java:36 → {@code FXSonic} at the target's eye). The
 *       {@code sonic} type is registered with its 15 ripple frames (BD-46) and
 *       {@code TcSonicOptions} is landed, but the packet itself has no landed
 *       counterpart, so there is no trigger site to pin (the same open seam the
 *       -f5 row records for the five untriggered families).</li>
 *   <li>{@code zapBoltPlanCarriesReferenceGeometry} — owner {@code PacketFXZap} /
 *       {@code PacketFXWispZap} → {@code ClientProxy.bolt}. SUPERSEDED: the head
 *       pinned a 12-field {@code TcBoltPlan} record, main's bolt path is
 *       {@link TcFxPlans#blockZapBolt} with {@code TcBoltGeometry} /
 *       {@code TcBoltOptions}, and that geometry is already pinned by
 *       {@link FxBeamGameTest} (the segment-bit-identical fractal bolt for a
 *       seed, the 2-pass glow/core plan and its colours). Landing {@code TcBoltPlan}
 *       would be dead code, so the subject is recorded as superseded, not ported.</li>
 * </ul>
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FxSenderGameTest {

    private FxSenderGameTest() {
    }

    /**
     * The {@code FXWisp} constructors' spawn plan: the colour triple
     * (ClientProxy.wispFX, ClientProxy.java:700-706) with {@code FXWisp.java:25-27}'s
     * {@code red == 0 → 1} rule, the point-target velocity
     * ({@code FXWisp.java:105-115}: {@code (target - spawn) / lifetime}), the
     * typed branch (the 0.4 the entity-target constructor hard-codes,
     * {@code FXWisp.java:112-115}) and the entity-tracking ride.
     */
    @GameTest(template = "empty")
    public static void wispPlansMatchOriginalConstructors(GameTestHelper helper) {
        final RandomSource rand = RandomSource.create(20261028L);
        final Vec3 spawn = new Vec3(1.5, 2.5, 3.5);

        // The colour constructor: the sender's triple, the red-lift, the
        // caller's scale and the 0.02 gravity ClientProxy.wispFX documents.
        final List<ParticleSpawn> plain = TcFxPlans.wisp(spawn, spawn, 0.6F,
                TcFxPlans.rgb(0.0F, 0.5F, 0.25F), false, TcWispOptions.NO_TARGET, false,
                0.02F, false, false, FxParticleCounts.SETTING_DECREASED, rand);
        helper.assertTrue(plain.size() == 1, "wispFX is particleCount(1) — one wisp");
        final TcWispOptions plainOptions = (TcWispOptions) plain.get(0).options();
        helper.assertTrue(plain.get(0).type() == ModParticles.WISP.get(), "the wisp type");
        helper.assertTrue(plain.get(0).x() == 1.5 && plain.get(0).y() == 2.5 && plain.get(0).z() == 3.5,
                "the wisp spawns where the sender says");
        helper.assertTrue(TcFxPlans.red(plainOptions.color()) == 1.0F,
                "FXWisp's red == 0 rule must lift the red channel to 1, got 0x"
                        + Integer.toHexString(plainOptions.color()));
        helper.assertTrue(Math.abs(TcFxPlans.green(plainOptions.color()) - 0.5F) < 1.0F / 255.0F
                        && Math.abs(TcFxPlans.blue(plainOptions.color()) - 0.25F) < 1.0F / 255.0F,
                "the other channels pass through untouched (to the 8-bit packing), got 0x"
                        + Integer.toHexString(plainOptions.color()));
        helper.assertTrue(plainOptions.scale() == 0.6F, "the caller's scale passes through");
        helper.assertTrue(plainOptions.gravity() == 0.02F, "wispFX's gravity is the sender's 0.02");
        helper.assertTrue(plainOptions.lifetime() >= 36 && plainOptions.lifetime() <= 51,
                "lifetime is (int)(36 / (rand*0.3 + 0.7)) = 36..51 (FXWisp.java:39), got "
                        + plainOptions.lifetime());
        helper.assertTrue(plainOptions.targetEntityId() == TcWispOptions.NO_TARGET
                        && !plainOptions.additive() && !plainOptions.clip(),
                "a plain wisp has no target, no additive blend and no noClip flag");

        // The point-target form: the mote must cover the gap in its own lifetime
        // (FXWisp.java:107-113, guarded by `particleMaxAge > 0`).
        final Vec3 target = new Vec3(4.0, 3.0, 2.0);
        final List<ParticleSpawn> travelling = TcFxPlans.wisp(new Vec3(0.0, 0.0, 0.0), target,
                0.5F, TcFxPlans.rgb(0.2F, 0.2F, 0.9F), false, TcWispOptions.NO_TARGET, false,
                0.03F, false, false, FxParticleCounts.SETTING_DECREASED, rand);
        final TcWispOptions travel = (TcWispOptions) travelling.get(0).options();
        helper.assertTrue(travelling.get(0).vx() == (target.x - 0.0) / travel.lifetime()
                        && travelling.get(0).vy() == (target.y - 0.0) / travel.lifetime()
                        && travelling.get(0).vz() == (target.z - 0.0) / travel.lifetime(),
                "the mote must cover the gap in exactly its lifetime, got "
                        + travelling.get(0).vx() + "," + travelling.get(0).vy() + ","
                        + travelling.get(0).vz() + " over " + travel.lifetime());
        helper.assertTrue(!travel.homing(), "a point target is not an entity target");

        // The typed branch: ClientProxy.wispFX2 with type 5 — the additive
        // smoke (FXWisp.java:84-90, blendmode 771) at the caller's size.
        final List<ParticleSpawn> typed = TcFxPlans.wispByType(new Vec3(4.0, 5.0, 6.0), 5,
                TcWispOptions.NO_TARGET, false, 0.0F, true, false,
                FxParticleCounts.SETTING_DECREASED, rand);
        final TcWispOptions typedOptions = (TcWispOptions) typed.get(0).options();
        helper.assertTrue(typedOptions.additive(), "type 5 flips the blend to additive (FXWisp.java:84)");
        helper.assertTrue(typedOptions.clip() && !typedOptions.shrink(),
                "clip and shrink ride the caller's flags, not the type branch");
        helper.assertTrue(TcFxPlans.red(typedOptions.color()) <= 0.1F
                        && TcFxPlans.green(typedOptions.color()) <= 0.1F
                        && TcFxPlans.blue(typedOptions.color()) <= 0.1F,
                "type 5 rolls each channel at rand*0.1 (FXWisp.java:86-89), got 0x"
                        + Integer.toHexString(typedOptions.color()));
        helper.assertTrue(typedOptions.scale() == 0.4F && typedOptions.gravity() == 0.0F,
                "the typed branch runs at the constructor's 0.4 and the caller's gravity");

        // The entity-target form (ClientProxy.wispFX4, FXWisp.java:112-118):
        // the mote rides the entity id and starts with no velocity.
        final List<ParticleSpawn> tracking = TcFxPlans.wisp(new Vec3(9.0, 9.0, 9.0),
                new Vec3(9.0, 9.0, 9.0), 0.4F, 0x00FF00, false, 17, true, 0.0F, false, false,
                FxParticleCounts.SETTING_DECREASED, rand);
        final TcWispOptions tracked = (TcWispOptions) tracking.get(0).options();
        helper.assertTrue(tracked.scale() == 0.4F, "the entity-target constructor scales at 0.4");
        helper.assertTrue(tracked.targetEntityId() == 17, "the wisp must ride entity 17");
        helper.assertTrue(tracked.homing() && tracking.get(0).vx() == 0.0
                        && tracking.get(0).vy() == 0.0 && tracking.get(0).vz() == 0.0,
                "the entity-target constructor hands in no velocity (FXWisp.java:112-115)");
        helper.assertTrue(!tracked.additive(), "only type 5 is additive");

        // Draw order is the fidelity claim: the same seed through the same plan
        // must roll the same mote.
        final List<ParticleSpawn> firstRoll = TcFxPlans.wispByType(spawn, 4,
                TcWispOptions.NO_TARGET, false, 0.0F, true, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(4242L));
        final List<ParticleSpawn> secondRoll = TcFxPlans.wispByType(spawn, 4,
                TcWispOptions.NO_TARGET, false, 0.0F, true, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(4242L));
        helper.assertTrue(signature(firstRoll).equals(signature(secondRoll))
                        && ((TcWispOptions) firstRoll.get(0).options()).color()
                        == ((TcWispOptions) secondRoll.get(0).options()).color(),
                "the same seed must roll the same wisp — the draw order is the fidelity claim");
        helper.succeed();
    }

    /**
     * The {@code FXWisp(world, x, y, z, scale, type)} colour switch, line for
     * line from FXWisp.java:64-115, through the table main consolidated into
     * {@link FxWispRules#wispColor}.
     *
     * <p>The original's switch has <em>no</em> {@code default:} case, so an
     * unknown type keeps the delegated constructor's lifted {@code (1, 0, 0)}
     * (FXWisp.java:23-27, 64). The port's table is total: it maps an unknown
     * type to the case-7 fiery roll. No landed sender passes a type outside
     * 0..7 — every reference call site of {@code wispFX2/3/4} hands a literal
     * in that range (15 sites, all literals) and the port's
     * {@code ClientEffectHooks.wispFxType} has no caller yet — so the
     * difference is unobservable and no deviation row is filed; the arm is
     * pinned here so a future packet-carried type cannot change it silently.</p>
     */
    @GameTest(template = "empty")
    public static void wispTypeColourTableMatches(GameTestHelper helper) {
        final FxWispRules.WispColor type0 = FxWispRules.wispColor(0, RandomSource.create(11L));
        helper.assertTrue(within(type0.r(), 0.75, 1.0) && within(type0.g(), 0.25, 0.5)
                        && within(type0.b(), 0.75, 1.0) && !type0.additive(),
                "type 0 (wisp purple): r 0.75+, g 0.25..0.5, b 0.75+ on the alpha layer");
        final FxWispRules.WispColor type1 = FxWispRules.wispColor(1, RandomSource.create(11L));
        helper.assertTrue(within(type1.r(), 0.5, 0.8) && within(type1.g(), 0.5, 0.8)
                        && type1.b() == 0.2F && !type1.additive(),
                "type 1 is amber (0.5 + rand*0.3 on red and green) with blue fixed at 0.2");
        final FxWispRules.WispColor type2 = FxWispRules.wispColor(2, RandomSource.create(11L));
        helper.assertTrue(type2.r() == 0.2F && type2.g() == 0.2F && within(type2.b(), 0.7, 1.0),
                "type 2 fixes red and green at 0.2 and rolls blue at 0.7 + rand*0.3");
        final FxWispRules.WispColor type3 = FxWispRules.wispColor(3, RandomSource.create(11L));
        helper.assertTrue(type3.r() == 0.2F && type3.b() == 0.2F && within(type3.g(), 0.7, 1.0),
                "type 3 is green with red and blue fixed at 0.2");
        final FxWispRules.WispColor type4 = FxWispRules.wispColor(4, RandomSource.create(11L));
        helper.assertTrue(type4.g() == 0.2F && type4.b() == 0.2F && within(type4.r(), 0.7, 1.0),
                "type 4 is the prim red: 0.7 + rand*0.3 on red, green and blue fixed at 0.2");
        final FxWispRules.WispColor type5 = FxWispRules.wispColor(5, RandomSource.create(11L));
        helper.assertTrue(type5.r() <= 0.1F && type5.g() <= 0.1F && type5.b() <= 0.1F
                        && type5.additive(),
                "type 5 is the near-black additive mote (blendmode 771, FXWisp.java:84)");
        final FxWispRules.WispColor type6 = FxWispRules.wispColor(6, RandomSource.create(11L));
        helper.assertTrue(within(type6.r(), 0.8, 1.0) && within(type6.g(), 0.8, 1.0)
                        && within(type6.b(), 0.8, 1.0) && !type6.additive(),
                "type 6 is white: every channel 0.8 + rand*0.2");
        final FxWispRules.WispColor type7 = FxWispRules.wispColor(7, RandomSource.create(11L));
        helper.assertTrue(within(type7.r(), 0.7, 1.0) && within(type7.g(), 0.5, 0.7)
                        && within(type7.b(), 0.3, 0.4) && !type7.additive(),
                "type 7 is the fiery roll");

        final FxWispRules.WispColor unknown = FxWispRules.wispColor(9, RandomSource.create(11L));
        helper.assertTrue(unknown.r() == type7.r() && unknown.g() == type7.g()
                        && unknown.b() == type7.b() && !unknown.additive(),
                "the port's table is total: an unknown type takes the case-7 roll (the "
                        + "original's switch has no default; no sender passes a type outside 0..7)");

        helper.assertTrue(FxWispRules.wispColor(4, RandomSource.create(4242L))
                        .equals(FxWispRules.wispColor(4, RandomSource.create(4242L))),
                "the same seed must roll the same colour — the draw order is the fidelity claim");
        helper.assertTrue(!FxWispRules.wispColor(0, RandomSource.create(11L)).additive()
                        && FxWispRules.wispColor(5, RandomSource.create(11L)).additive(),
                "blendmode 771 belongs to type 5 only (FXWisp.java:84)");

        // The lifetime law the same constructors roll: 36/(rand*0.3+0.7) for the
        // wisp, 40/(rand*0.3+0.7) for the EG mote (FXWisp.java:39, FXWispEG.java:32).
        for (long seed = 0L; seed < 24L; seed++) {
            final int wispLife = FxWispRules.wispLifetime(RandomSource.create(seed));
            helper.assertTrue(wispLife >= 36 && wispLife <= 51,
                    "the wisp lifetime law spans 36..51, got " + wispLife);
            final int egLife = FxWispRules.wispEgLifetime(RandomSource.create(seed));
            helper.assertTrue(egLife >= 40 && egLife <= 57,
                    "the EG wisp lifetime law spans 40..57, got " + egLife);
        }
        helper.succeed();
    }

    /**
     * {@code ClientProxy.wispFXEG} (ClientProxy.java:712-716) through
     * {@link TcFxPlans#wispEg} and {@code FXWispEG.java:24-46}: the ctor's draw
     * order is gaussian pair, lifetime roll, colour rolls, and the mote falls at
     * -0.05.
     */
    @GameTest(template = "empty")
    public static void wispEgBurstMatchesOriginal(GameTestHelper helper) {
        final RandomSource rand = RandomSource.create(99L);
        helper.assertTrue(TcFxPlans.wispEg(new Vec3(0.5, 64.5, 0.5), 5, false,
                        FxParticleCounts.SETTING_ALL, rand).size() == 2,
                "particleCount(1) doubles at the ALL setting");
        helper.assertTrue(TcFxPlans.wispEg(new Vec3(0.5, 64.5, 0.5), 5, false,
                        FxParticleCounts.SETTING_DECREASED, rand).size() == 1,
                "particleCount(1) is 1 at DECREASED");
        helper.assertTrue(TcFxPlans.wispEg(new Vec3(0.5, 64.5, 0.5), 5, false,
                        FxParticleCounts.SETTING_MINIMAL, rand).isEmpty(),
                "particleCount(1) is 0 at MINIMAL");
        helper.assertTrue(TcFxPlans.wispEg(new Vec3(0.5, 64.5, 0.5), 5, true,
                        FxParticleCounts.SETTING_ALL, rand).isEmpty(),
                "past the sender's visible distance the ctor zeroes the lifetime, so no mote spawns");

        final List<ParticleSpawn> burst = TcFxPlans.wispEg(new Vec3(0.5, 64.5, 0.5), 5, false,
                FxParticleCounts.SETTING_DECREASED, rand);
        final TcWispEgOptions options = (TcWispEgOptions) burst.get(0).options();
        helper.assertTrue(burst.get(0).type() == ModParticles.WISP_EG.get(), "the EG wisp type");
        helper.assertTrue(options.targetEntityId() == 5, "FXWispEG rides the target entity");
        helper.assertTrue(options.lifetime() >= 40 && options.lifetime() <= 57,
                "FXWispEG's lifetime is (int)(40 / (rand*0.3 + 0.7)), got " + options.lifetime());
        helper.assertTrue(TcFxPlans.red(options.color()) <= 0.05F
                        && TcFxPlans.green(options.color()) <= 0.05F
                        && TcFxPlans.blue(options.color()) <= 0.05F,
                "the colour rolls rand*0.05 per channel (FXWispEG.java:44-46), got 0x"
                        + Integer.toHexString(options.color()));
        helper.assertTrue(burst.get(0).vy() == -0.05, "the EG wisp falls at -0.05 (FXWispEG.java:25)");
        helper.assertTrue(Math.abs(burst.get(0).vx()) < 0.3 && Math.abs(burst.get(0).vz()) < 0.3,
                "the ctor spreads the mote with a 0.03-sigma gaussian pair (FXWispEG.java:24-26)");
        helper.succeed();
    }

    /**
     * {@code ClientProxy.sourceStreamFX} (ClientProxy.java:777-781) through
     * {@link TcFxPlans#wispArcing}: {@code FXWispArcing}'s 11-argument ctor keeps
     * the FIRST triple as the anchor, moves the mote to the second, and puts
     * {@code second - first} in its motion (FXWispArcing.java:51-59), so the
     * stream mote rides the target and converges on the source.
     */
    @GameTest(template = "empty")
    public static void arcingStreamFliesFromTargetToSource(GameTestHelper helper) {
        final List<ParticleSpawn> plan = TcFxPlans.wispArcing(new Vec3(1.0, 2.0, 3.0),
                new Vec3(10.0, 20.0, 30.0), 0x00FF00, false, FxParticleCounts.SETTING_ALL,
                RandomSource.create(7L));
        helper.assertTrue(plan.size() == 2,
                "the arcing sender runs the wisp family's particleCount(1) gate, which doubles at "
                        + "the ALL setting (ClientProxy.java:949-955), got " + plan.size());
        helper.assertTrue(TcFxPlans.wispArcing(new Vec3(1.0, 2.0, 3.0), new Vec3(10.0, 20.0, 30.0),
                        0x00FF00, false, FxParticleCounts.SETTING_DECREASED, RandomSource.create(7L)).size() == 1,
                "particleCount(1) is 1 at DECREASED");
        helper.assertTrue(TcFxPlans.wispArcing(new Vec3(1.0, 2.0, 3.0), new Vec3(10.0, 20.0, 30.0),
                        0x00FF00, false, FxParticleCounts.SETTING_MINIMAL, RandomSource.create(7L)).isEmpty(),
                "particleCount(1) is 0 at MINIMAL");
        final ParticleSpawn spawn = plan.get(0);
        final TcWispArcingOptions options = (TcWispArcingOptions) spawn.options();
        helper.assertTrue(spawn.type() == ModParticles.WISP_ARCING.get(), "the arcing wisp type");
        helper.assertTrue(spawn.x() == 1.0 && spawn.y() == 2.0 && spawn.z() == 3.0,
                "the mote spawns at the target triple and flies to the source triple");
        helper.assertTrue(options.dx() == 9.0 && options.dy() == 18.0 && options.dz() == 27.0,
                "the delta is start - anchor (FXWispArcing.java:56-58), got "
                        + options.dx() + "," + options.dy() + "," + options.dz());
        helper.assertTrue(spawn.vx() == 0.0 && spawn.vy() == 0.0 && spawn.vz() == 0.0,
                "the delta rides the payload, not the spawn velocity");
        for (final ParticleSpawn mote : plan) {
            final TcWispArcingOptions moteOptions = (TcWispArcingOptions) mote.options();
            helper.assertTrue(mote.x() == 1.0 && mote.y() == 2.0 && mote.z() == 3.0
                            && moteOptions.dx() == 9.0 && moteOptions.dy() == 18.0 && moteOptions.dz() == 27.0,
                    "every mote of the burst flies the same anchor-to-start line");
            helper.assertTrue(moteOptions.lifetime() >= 36 && moteOptions.lifetime() <= 51,
                    "each mote rolls its own 36..51 lifetime, got " + moteOptions.lifetime());
        }
        helper.assertTrue(options.scale() == 0.1F, "sourceStreamFX's scale is the literal 0.1");
        helper.assertTrue(options.lifetime() >= 36 && options.lifetime() <= 51,
                "the ctor rolls 36 / (rand*0.3 + 0.7), got " + options.lifetime());
        helper.assertTrue(TcFxPlans.red(options.color()) == 1.0F
                        && TcFxPlans.green(options.color()) == 1.0F
                        && TcFxPlans.blue(options.color()) == 0.0F,
                "the tag colour passes through FXWispArcing.java:22-24's red-lift, got 0x"
                        + Integer.toHexString(options.color()));
        helper.assertTrue(TcFxPlans.wispArcing(new Vec3(1.0, 2.0, 3.0), new Vec3(10.0, 20.0, 30.0),
                0x00FF00, true, FxParticleCounts.SETTING_ALL, RandomSource.create(7L)).isEmpty(),
                "past the sender's visible distance the ctor zeroes the lifetime, so no mote spawns");
        helper.succeed();
    }

    /** {@code PacketFXBlockDig.onMessage} (PacketFXBlockDig.java:64-131). */
    @GameTest(template = "empty")
    public static void blockDigCrumbsRideBlockAndItemParticles(GameTestHelper helper) {
        final BlockPos target = new BlockPos(10, 10, 10);
        final BlockPos digFrom = new BlockPos(11, 10, 10);
        final BlockState state = Blocks.STONE.defaultBlockState();

        final List<ParticleSpawn> all = TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                RandomSource.create(5L), FxParticleCounts.SETTING_ALL);
        helper.assertTrue(all.size() == 40, "particleCount(20) doubles at ALL, got " + all.size());
        helper.assertTrue(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                        RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED).size() == 20,
                "particleCount(20) is 20 at DECREASED");
        helper.assertTrue(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                        RandomSource.create(5L), FxParticleCounts.SETTING_MINIMAL).isEmpty(),
                "particleCount(20) is 0 at MINIMAL");

        for (final ParticleSpawn spawn : all) {
            helper.assertTrue(spawn.type() == ParticleTypes.BLOCK,
                    "the block branch of PacketFXBlockDig spawns the vanilla block crumb (BD-47)");
            helper.assertTrue(spawn.options() instanceof BlockParticleOption option
                            && option.getState() == state,
                    "the crumb must carry the packet's block state");
            helper.assertTrue(within(spawn.x(), 11.0, 12.0) && within(spawn.y(), 10.0, 11.0)
                            && within(spawn.z(), 10.0, 11.0),
                    "crumbs spawn inside the dig-face block (dx + rand)");
        }

        // A non-block stack is the packet's item branch (PacketFXBlockDig.java:99-128
        // takes ItemParticleOption for anything that is not a block item, which is the
        // essentia-dust case the sender is built for).
        final ItemStack stack = new ItemStack(Items.GUNPOWDER);
        final List<ParticleSpawn> items = TcFxPlans.blockDig(target, digFrom, null, stack,
                RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED);
        helper.assertTrue(items.size() == 20, "the item branch runs the same particleCount(20)");
        helper.assertTrue(items.get(0).type() == ParticleTypes.ITEM
                        && items.get(0).options() instanceof ItemParticleOption option
                        && option.getItem().is(Items.GUNPOWDER),
                "the item branch (PacketFXBlockDig.java:99-128) spawns the vanilla item crumb");
        helper.assertTrue(items.stream().allMatch(spawn -> spawn.type() == ParticleTypes.ITEM),
                "every mote of a non-block stack's burst is an item crumb");
        helper.assertTrue(signature(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                        RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED))
                        .equals(signature(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                                RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED))),
                "the same seed must reproduce the burst exactly (the draw order is the fidelity claim)");
        helper.succeed();
    }

    /**
     * The {@code EssentiaHandler.sourceFX} tick loop
     * (ClientTickEventsFML.java:218-261 → ClientProxy.essentiaTrailFx:783-786)
     * through {@link TcFxPlans#essentiaTrailTick}.
     */
    @GameTest(template = "empty")
    public static void essentiaTrailTickMatchesClientTickLoop(GameTestHelper helper) {
        final List<ParticleSpawn> steady = TcFxPlans.essentiaTrailTick(20, 64, 30, 10, 60, 15,
                0xFF0000, 15, 100, false, RandomSource.create(1L));
        helper.assertTrue(steady.size() == 1, "one trail mote per entry per tick");
        final ParticleSpawn spawn = steady.get(0);
        final TcEssentiaTrailOptions options = (TcEssentiaTrailOptions) spawn.options();
        helper.assertTrue(spawn.type() == ModParticles.ESSENTIA_TRAIL.get(), "the trail type");
        helper.assertTrue(spawn.x() == 20.5 && spawn.y() == 64.5 && spawn.z() == 30.5,
                "the trail starts at the source block centre");
        helper.assertTrue(options.targetX() == 10.5 && options.targetY() == 60.5 && options.targetZ() == 15.5,
                "and flies to the consumer block centre");
        helper.assertTrue(options.count() == 100, "the burst count is the client tick count");
        helper.assertTrue(Math.abs(options.scale() - wobble(100)) < 1.0E-3,
                "outside the tail window the scale is the sine wobble around 1 (the port's "
                        + "Mth.sin table quantises the angle, so the band is 1e-3), got "
                        + options.scale() + " want " + wobble(100));
        helper.assertTrue(options.lifetime() >= 1, "lifetime is (int)(dist*30) based, min 1");
        helper.assertTrue(within(TcFxPlans.red(options.color()), 0.8, 1.0)
                        && TcFxPlans.green(options.color()) == 0.0F
                        && TcFxPlans.blue(options.color()) == 0.0F,
                "the colour jitters each channel down by at most 0.2 of itself "
                        + "(FXEssentiaTrail.java:63-68), got 0x" + Integer.toHexString(options.color()));

        final TcEssentiaTrailOptions tail = (TcEssentiaTrailOptions) TcFxPlans
                .essentiaTrailTick(20, 64, 30, 10, 60, 15, 0xFF0000, 3, 100, false,
                        RandomSource.create(1L))
                .get(0).options();
        helper.assertTrue(tail.count() == 98,
                "the last 5 ticks subtract (5 - ticks) from the count (ClientTickEventsFML.java:244)");
        helper.assertTrue(Math.abs(tail.scale() - wobble(98) * 9.0F / 25.0F) < 1.0E-3,
                "the tail shrinks the scale by ticks²/25 on its own (reduced) count "
                        + "(ClientTickEventsFML.java:244-246), got " + tail.scale());

        final TcEssentiaTrailOptions matrix = (TcEssentiaTrailOptions) TcFxPlans
                .essentiaTrailTick(20, 64, 30, 10, 60, 15, 0xFF0000, 15, 100, true,
                        RandomSource.create(1L))
                .get(0).options();
        helper.assertTrue(matrix.targetY() == 59.5,
                "a matrix consumer pulls the target down a block (the original's y - 1)");
        helper.succeed();
    }

    /** The infusion matrix branches (TileInfusionMatrix.java:864-947). */
    @GameTest(template = "empty")
    public static void infusionTicksMatchMatrixBranches(GameTestHelper helper) {
        final BlockState pedestalState = Blocks.IRON_BLOCK.defaultBlockState();
        final ItemStack pedestalStack = new ItemStack(Items.IRON_BLOCK);
        int sparkleSeeds = 0;
        int crumbSeeds = 0;
        for (long seed = 0L; seed < 64L; seed++) {
            final List<ParticleSpawn> plan = TcFxPlans.infusionPedestalTick(12, 64, 18, 20, 66, 20,
                    pedestalState, pedestalStack, FxParticleCounts.SETTING_DECREASED,
                    RandomSource.create(seed));
            if (plan.size() == 1) {
                sparkleSeeds++;
                final ParticleSpawn spawn = plan.get(0);
                final TcBoreSparkleOptions options = (TcBoreSparkleOptions) spawn.options();
                helper.assertTrue(spawn.type() == ModParticles.BORE_SPARKLE.get(),
                        "the third chance draws one bore sparkle (drawInfusionParticles3)");
                helper.assertTrue(options.scale() == 1.0F, "that sparkle keeps scale 1");
                helper.assertTrue(options.targetX() == 20.5 && options.targetY() == 65.5
                                && options.targetZ() == 20.5,
                        "it flies to the matrix head (mx + 0.5, my - 0.5, mz + 0.5)");
                helper.assertTrue(within(spawn.y(), 65.0, 66.0), "it spawns on the pedestal top (sy + rand + 1)");
            } else if (plan.size() == 2) {
                crumbSeeds++;
                helper.assertTrue(plan.get(0).type() == ParticleTypes.BLOCK
                                && plan.get(0).options() instanceof BlockParticleOption option
                                && option.getState() == pedestalState,
                        "the other draws are the pedestal stack's block crumbs (drawInfusionParticles2)");
            } else {
                helper.fail("unexpected pedestal plan size " + plan.size() + " for seed " + seed);
            }
        }
        helper.assertTrue(sparkleSeeds > 0 && crumbSeeds > 0,
                "the pedestal branch must hit both sides of the third chance, got "
                        + sparkleSeeds + " sparkles / " + crumbSeeds + " crumb bursts");

        // The item branch is the `state == null` leg (drawInfusionParticles1), and the
        // third chance can pre-empt the whole draw, so sweep seeds and pin the motes
        // that do come out.
        final ItemStack dustStack = new ItemStack(Items.GUNPOWDER);
        int itemCrumbs = 0;
        for (long seed = 0L; seed < 64L; seed++) {
            for (final ParticleSpawn spawn : TcFxPlans.infusionPedestalTick(12, 64, 18, 20, 66, 20,
                    null, dustStack, FxParticleCounts.SETTING_ALL, RandomSource.create(seed))) {
                if (spawn.type() == ModParticles.BORE_SPARKLE.get()) {
                    continue; // the third chance's own draw
                }
                itemCrumbs++;
                helper.assertTrue(spawn.type() == ParticleTypes.ITEM,
                        "a pedestal stack with no block state draws item crumbs, not block crumbs "
                                + "(drawInfusionParticles1, seed " + seed + ")");
                helper.assertTrue(spawn.options() instanceof ItemParticleOption option
                                && option.getItem().is(Items.GUNPOWDER),
                        "the crumb carries the pedestal stack (drawInfusionParticles1)");
                helper.assertTrue(within(spawn.x(), 12.4, 12.6) && within(spawn.y(), 65.23, 65.43)
                                && within(spawn.z(), 18.4, 18.6),
                        "the item crumbs sit at sx + 0.4..0.6, sy + 1.23..1.43, sz + 0.4..0.6");
            }
        }
        helper.assertTrue(itemCrumbs > 0, "the pedestal's item branch must draw at least one crumb");

        final List<ParticleSpawn> playerDraws = TcFxPlans.infusionPlayerTick(20, 66, 20, 21.5, 64.0,
                21.5, 0.6, 1.8, FxParticleCounts.SETTING_DECREASED, RandomSource.create(3L));
        helper.assertTrue(playerDraws.size() == 2, "particleCount(2) motes for the player branch");
        for (final ParticleSpawn spawn : playerDraws) {
            final TcBoreSparkleOptions options = (TcBoreSparkleOptions) spawn.options();
            helper.assertTrue(spawn.type() == ModParticles.BORE_SPARKLE.get(), "the player branch draws bore motes");
            helper.assertTrue(options.scale() == 0.3F, "setParticleScale(0.3F) fixes the quad scale (BD-46)");
            helper.assertTrue(within(spawn.x(), 20.9, 22.1) && within(spawn.y(), 64.0, 65.8)
                            && within(spawn.z(), 20.9, 22.1),
                    "the motes spawn on the player's AABB (TileInfusionMatrix.java:873-878)");
            helper.assertTrue(options.targetX() == 20.5 && options.targetY() == 65.5 && options.targetZ() == 20.5,
                    "and fly to the matrix head");
        }
        helper.succeed();
    }

    /**
     * The shield runes: {@code PacketFXShield.onMessage} (PacketFXShield.java:38-79)
     * through {@link TcFxPlans#shieldRunes}, which reads the aim off the real
     * source and target entities. The runes ride the SOURCE; the target only aims
     * them; a mob source that is not a cultist takes the ripple-frame type
     * (FXShieldRunes.java:61) and the {@code mob} flag.
     *
     * <p>The expected yaw/pitch are literals: the packet's own aim math
     * ({@code yaw = atan2(dz, dx)*180/PI - 90}, {@code pitch =
     * -atan2(dy, horizontal)*180/PI}) evaluated on the geometry this probe
     * builds, never read back off the port's helper. A source 4 blocks -x of the
     * target at the same height is yaw 90 / pitch 0; a target 4 out and 3 up is
     * yaw 90 / pitch atan(3/4) = 36.86989765.</p>
     */
    @GameTest(template = "empty")
    public static void shieldRunesRideTheirSourceEntity(GameTestHelper helper) {
        // MEASURED (FxParticleGameTest:600-604): helper.spawn TRANSLATES its
        // position by the template origin, so the cells stay template-RELATIVE.
        // spawnWithNoFreeWill keeps the cow at the cell it is placed in (NoAI,
        // so no wander and no gravity step) and out of its neighbours' boxes.
        final Entity mob = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 3, 1));
        final Entity level0 = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 3, 1));
        final Entity raised = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 6, 1));
        final Entity stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(9, 3, 1));
        final RandomSource rand = RandomSource.create(4L);

        helper.assertTrue(TcFxPlans.shieldRunes(helper.getLevel(), 999999, level0.getId(),
                        false, rand).isEmpty(),
                "no source entity, no runes (PacketFXShield.java:39-40)");

        final List<ParticleSpawn> aimed = TcFxPlans.shieldRunes(helper.getLevel(), mob.getId(),
                level0.getId(), false, rand);
        helper.assertTrue(aimed.size() == 1, "PacketFXShield adds exactly one rune per aimed call");
        final ParticleSpawn rune = aimed.get(0);
        final TcShieldOptions runeOptions = (TcShieldOptions) rune.options();
        helper.assertTrue(rune.type() == ModParticles.SHIELD_RUNES_MOB.get(),
                "a mob source rides the ripple-frame type (FXShieldRunes.java:61-66)");
        helper.assertTrue(runeOptions.mob(), "the mob branch flags itself");
        helper.assertTrue(runeOptions.sourceEntityId() == mob.getId(),
                "the rune carries the source id it orbits");
        helper.assertTrue(runeOptions.yaw() == 90.0F,
                "a target due +x aims yaw 90 (atan2(0,-4) = 180, -90), got " + runeOptions.yaw());
        helper.assertTrue(runeOptions.pitch() == 0.0F,
                "equal AABB centre heights aim pitch 0, got " + runeOptions.pitch());
        helper.assertTrue(rune.x() == mob.getX() && rune.z() == mob.getZ(),
                "the rune rides the source's x/z (FXShieldRunes.java:39-41)");
        helper.assertTrue(within(rune.y(), mob.getBoundingBox().minY, mob.getBoundingBox().maxY),
                "the rune rides the source's AABB centre height");
        helper.assertTrue(Math.abs(runeOptions.scale() - 0.36F) < 1.0E-5F,
                "the scale is glScaled(0.4 * width) = 0.4 * 0.9 for a cow (FXShieldRunes.java:74), got "
                        + runeOptions.scale());
        helper.assertTrue(runeOptions.lifetime() >= 8 && runeOptions.lifetime() <= 11,
                "the ctor rolls age + rand.nextInt(age/2) = 8..11 (FXShieldRunes.java:31), got "
                        + runeOptions.lifetime());

        final TcShieldOptions pitched = (TcShieldOptions) TcFxPlans.shieldRunes(helper.getLevel(),
                mob.getId(), raised.getId(), false, rand).get(0).options();
        helper.assertTrue(pitched.yaw() == 90.0F, "the raised target keeps yaw 90, got " + pitched.yaw());
        helper.assertTrue(Math.abs(pitched.pitch() - 36.86989765F) < 1.0E-3F,
                "a target 4 out and 3 up aims atan(3/4) up = 36.86989765 (PacketFXShield.java:47-53), got "
                        + pitched.pitch());

        final List<ParticleSpawn> cultist = TcFxPlans.shieldRunes(helper.getLevel(), mob.getId(),
                level0.getId(), true, rand);
        helper.assertTrue(cultist.size() == 1, "the cultist exclusion is the same one rune");
        helper.assertTrue(cultist.get(0).type() == ModParticles.SHIELD_RUNES.get()
                        && !((TcShieldOptions) cultist.get(0).options()).mob(),
                "an EntityCultist is excluded from the mob branch, so it keeps the hemis frames "
                        + "and the base type (FXShieldRunes.java:61)");

        final List<ParticleSpawn> flat = TcFxPlans.shieldRunes(helper.getLevel(), stand.getId(),
                level0.getId(), false, rand);
        helper.assertTrue(flat.size() == 1, "a non-mob source with a real target is one rune");
        helper.assertTrue(flat.get(0).type() == ModParticles.SHIELD_RUNES.get()
                        && !((TcShieldOptions) flat.get(0).options()).mob(),
                "an armor stand is not a Mob: the runes keep the hemis frames and the base type");
        helper.assertTrue(Math.abs(((TcShieldOptions) flat.get(0).options()).scale() - 0.2F) < 1.0E-5F,
                "the scale is 0.4 * 0.5 for an armor stand's width, got "
                        + ((TcShieldOptions) flat.get(0).options()).scale());
        helper.succeed();
    }

    /**
     * The essentia drain map: {@code PacketFXEssentiaSource.onMessage}
     * (PacketFXEssentiaSource.java:52-68) plus the client tick drain
     * (ClientTickEventsFML.java:218-261), proved through the client-free
     * {@link TcEssentiaSourceMap} the client subscriber feeds.
     */
    @GameTest(template = "empty")
    public static void essentiaSourceMapMatchesPacketSemantics(GameTestHelper helper) {
        TcEssentiaSourceMap.reset();
        final BlockPos consumer = new BlockPos(20, 64, 30);
        final BlockPos offset = new BlockPos(0, 1, 0);
        final BlockPos source = consumer.subtract(offset);

        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 0, "the map starts empty");
        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0xFF0000);
        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 1, "one entry per packet key");
        helper.assertTrue(TcEssentiaSourceMap.ticksAt(consumer, source, 0xFF0000) == 15,
                "a landing packet sets the 15-tick budget (PacketFXEssentiaSource.java:66)");

        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0xFF0000);
        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 1,
                "the key is pos:source:colour, so a repeat refreshes the same entry");

        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0x00FF00);
        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 2, "a different colour is a different entry");
        helper.assertTrue(TcEssentiaSourceMap.order().size() == 2
                        && !TcEssentiaSourceMap.order().get(0).equals(TcEssentiaSourceMap.order().get(1)),
                "the map is insertion-ordered, which is what the original's remove-and-put gives");
        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0xFF0000);
        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 2
                        && TcEssentiaSourceMap.order().get(1).endsWith(":16711680"),
                "refreshing moves the entry to the back of the drain order");

        final List<ParticleSpawn> tick = TcEssentiaSourceMap.drain(pos -> false, 100, RandomSource.create(1L));
        helper.assertTrue(tick.size() == 2, "one trail mote per live entry per tick");
        helper.assertTrue(tick.get(0).type() == ModParticles.ESSENTIA_TRAIL.get(), "the trail type");
        helper.assertTrue(TcEssentiaSourceMap.ticksAt(consumer, source, 0x00FF00) == 14
                        && TcEssentiaSourceMap.ticksAt(consumer, source, 0xFF0000) == 14,
                "the tick decrements each entry's budget");

        // The 15-tick budget is the whole life of an entry: 15 drains spawn, the
        // 16th drops it (the original's `if (sf.ticks <= 0) remove`).
        TcEssentiaSourceMap.reset();
        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0xFF0000);
        int motes = 0;
        for (int tickNumber = 0; tickNumber < 16; tickNumber++) {
            motes += TcEssentiaSourceMap.drain(pos -> false, 100 + tickNumber,
                    RandomSource.create(2L)).size();
        }
        helper.assertTrue(motes == 15, "an entry drains exactly its 15 ticks, got " + motes);
        helper.assertTrue(TcEssentiaSourceMap.entryCount() == 0, "the drained entry is dropped");

        TcEssentiaSourceMap.onEssentiaSource(consumer, offset, 0x00FF00);
        final TcEssentiaTrailOptions matrix = (TcEssentiaTrailOptions) TcEssentiaSourceMap
                .drain(pos -> true, 100, RandomSource.create(1L)).get(0).options();
        helper.assertTrue(matrix.targetY() == 63.5,
                "a matrix consumer pulls the target down a block (y - 1 + 0.5)");
        TcEssentiaSourceMap.reset();
        helper.succeed();
    }

    /**
     * The {@code ClientProxy.particleCount} gate (ClientProxy.java:949-955) as
     * each sender here uses it: dig 20, infusion 2, the EG mote and the wisp 1.
     */
    @GameTest(template = "empty")
    public static void particleSettingGateCoversTheSenderSenders(GameTestHelper helper) {
        helper.assertTrue(FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_ALL) == 40
                        && FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_DECREASED) == 20
                        && FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_MINIMAL) == 0,
                "the dig burst runs the original's particleCount(20) gate");
        helper.assertTrue(FxParticleCounts.particleCount(2, FxParticleCounts.SETTING_ALL) == 4
                        && FxParticleCounts.particleCount(2, FxParticleCounts.SETTING_MINIMAL) == 0,
                "the infusion draws run particleCount(2)");
        helper.assertTrue(FxParticleCounts.particleCount(1, FxParticleCounts.SETTING_DECREASED) == 1,
                "the EG wisp runs particleCount(1)");
        helper.assertTrue(TcFxPlans.wisp(new Vec3(0.0, 0.0, 0.0), new Vec3(0.0, 0.0, 0.0), 0.4F,
                        0x00FF00, false, TcWispOptions.NO_TARGET, false, 0.0F, false, false,
                        FxParticleCounts.SETTING_ALL, RandomSource.create(3L)).size()
                        == FxParticleCounts.particleCount(1, FxParticleCounts.SETTING_ALL),
                "the wisp sender gates on particleCount(1) (the wispFX family's base count)");
        helper.succeed();
    }

    private static boolean within(final double value, final double min, final double max) {
        return value >= min && value <= max;
    }

    /**
     * The trail's sine wobble around 1 for a client tick count, stated as the
     * original writes it ({@code Math.sin(count / 2.0F) * 0.1F + 1.0F},
     * FXEssentiaTrail.java:71-73). The port evaluates it through 1.21.1's
     * quantised {@code Mth.sin} table, which is why the probes compare inside
     * 1e-3 and not to the bit.
     */
    private static float wobble(final int count) {
        return (float) (Math.sin(count / 2.0F) * 0.1F + 1.0F);
    }

    /** A structural signature of a plan: {@code ParticleOptions} has no value-based toString. */
    private static String signature(final List<ParticleSpawn> plan) {
        final StringBuilder out = new StringBuilder();
        for (final ParticleSpawn spawn : plan) {
            out.append(spawn.type()).append('@')
                    .append(spawn.x()).append(',').append(spawn.y()).append(',').append(spawn.z())
                    .append(',').append(spawn.vx()).append(',').append(spawn.vy()).append(',').append(spawn.vz())
                    .append(';');
        }
        return out.toString();
    }
}
