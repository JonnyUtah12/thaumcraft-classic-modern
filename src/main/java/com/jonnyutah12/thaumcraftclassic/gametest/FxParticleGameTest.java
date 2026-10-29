package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWispRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime proof for the TC-M7-02-s particle slice, on the game-test server.
 *
 * <p>The original's FX path is client-only (every handler is behind
 * {@code world.field_72995_K}), so a dedicated-server test can never see a
 * rendered particle. This port therefore splits the effect in two: the spawn
 * plan (positions, motion, colour, lifetime — all of it in the decompiled
 * reference tree) is pure code, and only the final
 * {@code Level#addParticle} hand-off is platform-side. These probes pin the
 * plan geometry and the live registry ids, which is what the game-test server
 * can prove; the pixels are covered by the client smoke run recorded in
 * docs/feature-tracking.md.</p>
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FxParticleGameTest {

    private FxParticleGameTest() {
    }

    /** The two ported particle types must be live in the registry. */
    @GameTest(template = "empty")
    public static void portedParticleTypesRegistered(GameTestHelper helper) {
        for (String path : new String[] {"sparkle", "bubble", "wisp", "wisp_eg", "wisp_arcing"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.containsKey(id),
                    "particle type " + id + " is not in the registry");
        }
        // The whole slice: the mod registers exactly the particle types the
        // M7-02 cards scope — 2 (TC-M7-02-s) + 6 beams/bolts (TC-M7-02-b-s)
        // + 3 wisp family (TC-M7-02-c-s-f1).
        long registered = BuiltInRegistries.PARTICLE_TYPE.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .count();
        helper.assertTrue(registered == 11,
                "thaumcraft-namespace particle types must be the 11 ported ones, found " + registered);
        helper.assertTrue(ModParticles.SPARCLE.get().codec() == TcSparkleOptions.MAP_CODEC,
                "sparkle type must decode through TcSparkleOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BUBBLE.get().codec() == TcBubbleOptions.MAP_CODEC,
                "bubble type must decode through TcBubbleOptions.MAP_CODEC");
        helper.succeed();
    }

    /**
     * The sparkle burst's shape, from {@code ClientProxy.blockSparkle}
     * (ClientProxy.java:576-608): 7 particles doubled by the particle setting,
     * jittered inside the block, drifting up, alpha 0.9, colour jittered ±0.2.
     */
    @GameTest(template = "empty")
    public static void sparkleBurstMatchesOriginalGeometry(GameTestHelper helper) {
        BlockPos pos = new BlockPos(10, 60, 10);
        int blue = 0x0000FF;
        List<ParticleSpawn> plan = TcFxPlans.blockSparkle(pos, blue, RandomSource.create(20261028L),
                FxParticleCounts.SETTING_ALL);

        helper.assertTrue(plan.size() == 14,
                "blockSparkle on 'all' must be 7 * 2 = 14 particles, got " + plan.size());

        for (ParticleSpawn spawn : plan) {
            TcSparkleOptions options = (TcSparkleOptions) spawn.options();
            helper.assertTrue(spawn.type() == ModParticles.SPARCLE.get(),
                    "sparkle plan must use the registered sparkle type");
            helper.assertTrue(within(spawn.x(), pos.getX() - 0.1, pos.getX() + 1.1)
                            && within(spawn.y(), pos.getY() - 0.1, pos.getY() + 1.1)
                            && within(spawn.z(), pos.getZ() - 0.1, pos.getZ() + 1.1),
                    "sparkle spawn point must sit in the original's -0.1..1.1 block jitter, got "
                            + spawn.x() + "," + spawn.y() + "," + spawn.z());
            helper.assertTrue(spawn.vx() == 0.0 && spawn.vz() == 0.0
                            && within(spawn.vy(), 0.0, TcFxPlans.SPARKLE_RISE),
                    "the original's sparkle drift is (0, rand*0.02, 0), got "
                            + spawn.vx() + "," + spawn.vy() + "," + spawn.vz());
            helper.assertTrue(options.alpha() == TcFxPlans.SPARKLE_ALPHA,
                    "sparkle alpha must be the sender's 0.9, got " + options.alpha());
            helper.assertTrue(options.age() >= 5 && options.age() < 13,
                    "sparkle age must be 5 + rand(8), got " + options.age());
            helper.assertTrue(options.delay() >= 0 && options.delay() < 10,
                    "sparkle delay must be rand(10), got " + options.delay());
            helper.assertTrue(within(options.scale(), 0.7, 1.1),
                    "sparkle scale must be 0.7 + rand*0.4, got " + options.scale());
            // The sender's colour is pure blue (0,0,1); the jitter is ±0.2, so
            // red/green land in 0..0.2 and blue in 0.8..1.0.
            helper.assertTrue(within(TcFxPlans.red(options.color()), 0.0, 0.21)
                            && within(TcFxPlans.green(options.color()), 0.0, 0.21)
                            && within(TcFxPlans.blue(options.color()), 0.79, 1.0),
                    "sparkle colour must stay within ±0.2 of the sender's (0,0,1), got 0x"
                            + Integer.toHexString(options.color()));
        }

        // The original's loop is deterministic for a given world random; two
        // bursts from the same seed must be identical.
        List<ParticleSpawn> again = TcFxPlans.blockSparkle(pos, blue, RandomSource.create(20261028L),
                FxParticleCounts.SETTING_ALL);
        helper.assertTrue(plan.toString().equals(again.toString()),
                "a sparkle burst must be reproducible from the same random seed");
        helper.succeed();
    }

    /**
     * The original's particle-count gate (ClientProxy.particleCount,
     * ClientProxy.java:949-955): decreased = base, all = base * 2, minimal =
     * nothing at all.
     */
    @GameTest(template = "empty")
    public static void particleSettingGateMatchesOriginal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 70, 3);
        RandomSource rand = RandomSource.create(99L);

        helper.assertTrue(FxParticleCounts.particleCount(7, FxParticleCounts.SETTING_ALL) == 14,
                "'all' must double the sparkle base of 7");
        helper.assertTrue(FxParticleCounts.particleCount(7, FxParticleCounts.SETTING_DECREASED) == 7,
                "'decreased' must keep the base");
        helper.assertTrue(FxParticleCounts.particleCount(7, FxParticleCounts.SETTING_MINIMAL) == 0,
                "'minimal' must suppress the burst");

        helper.assertTrue(TcFxPlans.blockSparkle(pos, 0xFF00FF, rand, FxParticleCounts.SETTING_DECREASED).size() == 7,
                "the sparkle plan must carry the decreased count through");
        helper.assertTrue(TcFxPlans.blockSparkle(pos, 0xFF00FF, rand, FxParticleCounts.SETTING_MINIMAL).isEmpty(),
                "the sparkle plan must be empty on minimal");
        helper.assertTrue(TcFxPlans.blockBubble(pos, 0xFF00FF, rand, FxParticleCounts.SETTING_MINIMAL).isEmpty(),
                "the bubble plan must be empty on minimal");
        helper.succeed();
    }

    /**
     * The bubble burst's shape, from {@code PacketFXBlockBubble.onMessage}
     * (PacketFXBlockBubble.java:44-79): four bubbles per iteration, anchored on
     * the x face, x+1, the z face and z+1, with y jittered on all of them.
     */
    @GameTest(template = "empty")
    public static void bubbleBurstMatchesOriginalAnchors(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 64, 5);
        List<ParticleSpawn> plan = TcFxPlans.blockBubble(pos, 0x00FF00, RandomSource.create(7L),
                FxParticleCounts.SETTING_ALL);

        helper.assertTrue(plan.size() == 8,
                "blockBubble on 'all' must be 1 * 2 loops * 4 bubbles = 8, got " + plan.size());

        helper.assertTrue(plan.get(0).x() == pos.getX(),
                "bubble 1 anchors on the block's own x face");
        helper.assertTrue(plan.get(1).x() == pos.getX() + 1.0,
                "bubble 2 anchors on x + 1");
        helper.assertTrue(plan.get(2).z() == pos.getZ(),
                "bubble 3 anchors on the block's own z face");
        helper.assertTrue(plan.get(3).z() == pos.getZ() + 1.0,
                "bubble 4 anchors on z + 1");

        for (ParticleSpawn spawn : plan) {
            TcBubbleOptions options = (TcBubbleOptions) spawn.options();
            helper.assertTrue(spawn.type() == ModParticles.BUBBLE.get(),
                    "bubble plan must use the registered bubble type");
            helper.assertTrue(within(options.scale(), 0.02, 0.05),
                    "FxBubble scale is 0.1 * (rand*0.3 + 0.2), got " + options.scale());
            helper.assertTrue(options.rise() == TcBubbleOptions.DEFAULT_RISE,
                    "FxBubble's rise is the 0.002 field default, got " + options.rise());
            // age(1) + 2 + 8 / (rand*0.8 + 0.2)  ->  3 + 8 .. 3 + 40
            helper.assertTrue(options.lifetime() >= 11 && options.lifetime() <= 43,
                    "FxBubble lifetime is 1 + 2 + 8/(rand*0.8+0.2), got " + options.lifetime());
            helper.assertTrue(within(spawn.y(), pos.getY(), pos.getY() + 1.0),
                    "the bubble's y is the block corner + rand, got " + spawn.y());
            helper.assertTrue(within(spawn.vx(), -0.02, 0.02) && within(spawn.vy(), 0.0, 0.02)
                            && within(spawn.vz(), -0.02, 0.02),
                    "FxBubble's handed-in motion is zero, so the initial motion is pure ±0.02 jitter");
        }
        helper.succeed();
    }

    /**
     * The magic rainbow colour: {@code PacketFXBlockSparkle} sends -9999 and
     * {@code ClientProxy.blockSparkle} (lines 585-589) replaces it with a
     * 0.33..1.0 white roll per particle.
     */
    @GameTest(template = "empty")
    public static void rainbowSparkleStaysInWhiteBand(GameTestHelper helper) {
        List<ParticleSpawn> plan = TcFxPlans.blockSparkle(new BlockPos(0, 64, 0),
                TcFxPlans.RAINBOW_COLOR, RandomSource.create(4242L), FxParticleCounts.SETTING_ALL);
        helper.assertTrue(plan.size() == 14, "the rainbow burst is the same size as a coloured one");
        for (ParticleSpawn spawn : plan) {
            TcSparkleOptions options = (TcSparkleOptions) spawn.options();
            helper.assertTrue(within(TcFxPlans.red(options.color()), 0.13, 1.0)
                            && within(TcFxPlans.green(options.color()), 0.13, 1.0)
                            && within(TcFxPlans.blue(options.color()), 0.13, 1.0),
                    "a rainbow sparkle stays in the 0.33 base ±0.2 jitter band, got 0x"
                            + Integer.toHexString(options.color()));
        }
        helper.succeed();
    }

    /**
     * The payload handler's entry point runs through the real level API. On the
     * game-test server {@code Level#addParticle} is a no-op by design (the
     * client is the only renderer), so what this proves is that the effect's
     * spawn hand-off is wired to the level it was sent for and returns the
     * burst the original would have produced.
     */
    @GameTest(template = "empty")
    public static void fxSpawnHandoffRunsThroughLevelApi(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int sparkles = TcEffects.blockSparkle(helper.getLevel(), pos, 0xFF6600,
                FxParticleCounts.SETTING_ALL, RandomSource.create(31L));
        int bubbles = TcEffects.blockBubble(helper.getLevel(), pos, 0x0066FF,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(31L));
        helper.assertTrue(sparkles == 14, "the sparkle hand-off must request 14 particles, got " + sparkles);
        helper.assertTrue(bubbles == 4, "the bubble hand-off must request 4 particles, got " + bubbles);
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f1b: the wisp family's wire codecs. The wisp payload carries
     * NINE fields and 1.21.1's {@code StreamCodec.composite} overloads cap at
     * SIX (javap over the mapped {@code StreamCodec}: arities 1..6), so
     * {@code TcWispOptions.STREAM_CODEC} composites a nested six-field core
     * group with the remaining three — the arity limit is the exact defect that
     * made this family fail to compile, and the nesting is where a field can
     * silently land in the wrong slot. Every sample therefore carries values
     * that make a pair swap visible: the two wisp samples are complements in
     * all four boolean slots, and each family is sampled both with a real
     * target id and with the {@link TcWispOptions#NO_TARGET} sentinel (a
     * negative, so it pins the zig-zag varint read too).
     */
    @GameTest(template = "empty")
    public static void wispPayloadCodecsRoundTrip(GameTestHelper helper) {
        assertWireRoundTrip(helper, TcWispOptions.STREAM_CODEC,
                new TcWispOptions(0x112233, 0.75F, 31, 17, true, false, 0.02F, false, true));
        assertWireRoundTrip(helper, TcWispOptions.STREAM_CODEC,
                new TcWispOptions(0x778899, 0.1F, 40, TcWispOptions.NO_TARGET, false, true, 0.0F, true, false));
        assertWireRoundTrip(helper, TcWispEgOptions.STREAM_CODEC,
                new TcWispEgOptions(0x050505, 52, 12));
        assertWireRoundTrip(helper, TcWispEgOptions.STREAM_CODEC,
                new TcWispEgOptions(0x0A0B0C, 36, TcWispOptions.NO_TARGET));
        assertWireRoundTrip(helper, TcWispArcingOptions.STREAM_CODEC,
                new TcWispArcingOptions(0x332211, 0.1F, 36, 1.5, -2.25, 3.0));

        // The registered types must decode through these codecs — the same pin
        // the sparkle/bubble and beam probes use.
        helper.assertTrue(ModParticles.WISP.get().codec() == TcWispOptions.MAP_CODEC,
                "wisp type must decode through TcWispOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.WISP_EG.get().codec() == TcWispEgOptions.MAP_CODEC,
                "wisp_eg type must decode through TcWispEgOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.WISP_ARCING.get().codec() == TcWispArcingOptions.MAP_CODEC,
                "wisp_arcing type must decode through TcWispArcingOptions.MAP_CODEC");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f1c: the wisp family's three senders. These are the
     * original's client-side proxy calls ({@code ClientProxy.wispFX} family,
     * {@code wispFXEG}, {@code sourceStreamFX}) — not packet handlers — so the
     * plan below is what a game-test server can prove: the burst gate, the
     * constructor's roll order, the point-target initial velocity, the homing
     * branch, the cull, and the arcing mote's anchor/delta swap.
     */
    @GameTest(template = "empty")
    public static void wispFamilySendersMatchOriginalGeometry(GameTestHelper helper) {
        final Vec3 spawn = new Vec3(1.0, 64.0, 2.0);
        final Vec3 target = new Vec3(5.0, 68.0, 6.0);

        // ClientProxy.wispFX, the point-target form: particleCount(1) on "all"
        // is 2 motes, each with the constructor's own lifetime roll and the
        // (target - spawn) / lifetime initial velocity (FXWisp.java:39,117-125).
        final List<ParticleSpawn> travelled = TcFxPlans.wisp(spawn, target, 1.0F, 0x00FF00,
                false, TcWispOptions.NO_TARGET, false, TcFxPlans.WISP_GRAVITY, true, false,
                FxParticleCounts.SETTING_ALL, RandomSource.create(20261029L));
        helper.assertTrue(travelled.size() == 2,
                "wispFX on 'all' must be particleCount(1) * 2 = 2 motes, got " + travelled.size());
        for (final ParticleSpawn mote : travelled) {
            helper.assertTrue(mote.type() == ModParticles.WISP.get(),
                    "the wisp plan must use the registered wisp type");
            final TcWispOptions options = (TcWispOptions) mote.options();
            helper.assertTrue(options.lifetime() >= 36 && options.lifetime() <= 51,
                    "wisp lifetime must be 36 / (rand*0.3 + 0.7), got " + options.lifetime());
            helper.assertTrue(!options.homing() && options.targetEntityId() == TcWispOptions.NO_TARGET,
                    "a free wisp must not carry a homing target");
            helper.assertTrue(Math.abs(mote.vx() - (target.x - spawn.x) / options.lifetime()) < 1.0E-9
                            && Math.abs(mote.vy() - (target.y - spawn.y) / options.lifetime()) < 1.0E-9
                            && Math.abs(mote.vz() - (target.z - spawn.z) / options.lifetime()) < 1.0E-9,
                    "the point-target wisp must start at (target - spawn) / lifetime, got "
                            + mote.vx() + "," + mote.vy() + "," + mote.vz());
            helper.assertTrue(TcFxPlans.red(options.color()) > 0.99F
                            && TcFxPlans.green(options.color()) > 0.99F,
                    "the sender's (0,1,0) colour must survive packing, got 0x"
                            + Integer.toHexString(options.color()));
        }

        // The red-channel fix: FXWisp's colour constructor forces a zero red
        // channel to 1.0 (FXWisp.java:25-27), so a (0,0,0) sender is red.
        final List<ParticleSpawn> blackSender = TcFxPlans.wisp(spawn, spawn, 1.0F, 0x000000, false,
                TcWispOptions.NO_TARGET, false, TcFxPlans.WISP_GRAVITY, true, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(11L));
        helper.assertTrue(blackSender.size() == 1,
                "the 'decreased' setting must be particleCount(1) * 1 = 1 mote");
        helper.assertTrue(TcFxPlans.red(((TcWispOptions) blackSender.get(0).options()).color()) > 0.99F,
                "a zero red channel must be forced to 1.0, not left black");
        // The free form: target == spawn means a standstill, and the cull and
        // the minimal setting both gate the spawn away entirely.
        helper.assertTrue(!((TcWispOptions) blackSender.get(0).options()).homing()
                        && blackSender.get(0).vx() == 0.0,
                "the colour constructor's mote starts at a standstill");
        helper.assertTrue(TcFxPlans.wisp(spawn, spawn, 1.0F, 0xFFFFFF, false,
                TcWispOptions.NO_TARGET, false, 0.0F, true, true,
                FxParticleCounts.SETTING_ALL, RandomSource.create(11L)).isEmpty(),
                "a culled wisp must not spawn (the original zeroes its lifetime)");
        helper.assertTrue(TcFxPlans.wisp(spawn, spawn, 1.0F, 0xFFFFFF, false,
                TcWispOptions.NO_TARGET, false, 0.0F, true, false,
                FxParticleCounts.SETTING_MINIMAL, RandomSource.create(11L)).isEmpty(),
                "the 'minimal' setting must gate the whole wisp burst away");

        // The homing form: an entity target switches the mote onto the 0.985
        // drag / 0.2 seek branch and leaves the velocity at zero.
        final List<ParticleSpawn> homing = TcFxPlans.wisp(spawn, target, 0.4F, 0x00FF00, false,
                42, false, 0.0F, true, false, FxParticleCounts.SETTING_ALL,
                RandomSource.create(3L));
        for (final ParticleSpawn mote : homing) {
            final TcWispOptions options = (TcWispOptions) mote.options();
            helper.assertTrue(options.homing() && options.targetEntityId() == 42,
                    "an entity target must set the homing branch and keep the id");
            helper.assertTrue(mote.vx() == 0.0 && mote.vy() == 0.0 && mote.vz() == 0.0,
                    "the entity-target constructor hands in no velocity");
        }

        // The type branch: case 0 is wisp purple (0.75..1 red, 0.25..0.5 green,
        // 0.75..1 blue, alpha layer), case 5 is the additive dark mote
        // (FXWisp.java:66-115), both at the 0.4 the constructor hard-codes.
        final List<ParticleSpawn> purple = TcFxPlans.wispByType(spawn, 0, TcWispOptions.NO_TARGET,
                false, 0.0F, true, false, FxParticleCounts.SETTING_DECREASED,
                RandomSource.create(5L));
        helper.assertTrue(purple.size() == 1, "a typed wisp on 'decreased' must be one mote");
        final TcWispOptions purpleOptions = (TcWispOptions) purple.get(0).options();
        helper.assertTrue(purpleOptions.scale() == TcFxPlans.WISP_TYPE_SCALE
                        && !purpleOptions.additive()
                        && within(TcFxPlans.red(purpleOptions.color()), 0.75, 1.0)
                        && within(TcFxPlans.green(purpleOptions.color()), 0.25, 0.5)
                        && within(TcFxPlans.blue(purpleOptions.color()), 0.75, 1.0),
                "type 0 must be the 0.4-scale wisp-purple mote on the alpha layer, got 0x"
                        + Integer.toHexString(purpleOptions.color()));
        final TcWispOptions darkOptions = (TcWispOptions) TcFxPlans.wispByType(spawn, 5,
                TcWispOptions.NO_TARGET, false, 0.0F, true, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(5L)).get(0).options();
        helper.assertTrue(darkOptions.additive()
                        && TcFxPlans.red(darkOptions.color()) < 0.1F
                        && TcFxPlans.green(darkOptions.color()) < 0.1F
                        && TcFxPlans.blue(darkOptions.color()) < 0.1F,
                "type 5 must be the additive near-black mote (FXWisp.java:100)");

        // ClientProxy.wispFXEG: the gaussian x/z spread, the -0.05 fall and the
        // dark additive colour, all rolled inside the constructor
        // (FXWispEG.java:24-46).
        final List<ParticleSpawn> eg = TcFxPlans.wispEg(spawn, 7, false,
                FxParticleCounts.SETTING_ALL, RandomSource.create(9L));
        helper.assertTrue(eg.size() == 2, "wispFXEG on 'all' must spawn two motes");
        for (final ParticleSpawn mote : eg) {
            helper.assertTrue(mote.type() == ModParticles.WISP_EG.get(),
                    "the EG plan must use the registered wisp_eg type");
            final TcWispEgOptions options = (TcWispEgOptions) mote.options();
            helper.assertTrue(options.lifetime() >= 40 && options.lifetime() <= 57,
                    "EG lifetime must be 40 / (rand*0.3 + 0.7), got " + options.lifetime());
            helper.assertTrue(options.targetEntityId() == 7,
                    "the EG mote must carry the entity it falls onto");
            helper.assertTrue(mote.vy() == FxWispRules.EG_RISE && mote.vy() == -0.05,
                    "the EG mote must fall at -0.05, got " + mote.vy());
            helper.assertTrue(Math.abs(mote.vx()) <= 0.03 * 4 && Math.abs(mote.vz()) <= 0.03 * 4,
                    "the EG x/z spread is a rand*0.03 gaussian, got "
                            + mote.vx() + "," + mote.vz());
            helper.assertTrue(TcFxPlans.red(options.color()) <= 0.05F
                            && TcFxPlans.green(options.color()) <= 0.05F
                            && TcFxPlans.blue(options.color()) <= 0.05F,
                    "the EG colour must be rand*0.05 per channel, got 0x"
                            + Integer.toHexString(options.color()));
        }

        // ClientProxy.sourceStreamFX: the mote parks on the TARGET with the
        // delta to the source in its motion fields (FXWispArcing.java:51-59),
        // at the sender's fixed 0.1 scale and the shared lifetime roll.
        final List<ParticleSpawn> arcing = TcFxPlans.wispArcing(spawn, target, 0x7F3F1F, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(13L));
        helper.assertTrue(arcing.size() == 1, "the arcing wisp on 'decreased' must be one mote");
        final ParticleSpawn arcMote = arcing.get(0);
        final TcWispArcingOptions arcOptions = (TcWispArcingOptions) arcMote.options();
        helper.assertTrue(arcMote.type() == ModParticles.WISP_ARCING.get(),
                "the arcing plan must use the registered wisp_arcing type");
        helper.assertTrue(arcMote.x() == target.x && arcMote.y() == target.y
                        && arcMote.z() == target.z,
                "the arcing mote spawns on the target anchor");
        helper.assertTrue(arcMote.vx() == spawn.x - target.x && arcMote.vy() == spawn.y - target.y
                        && arcMote.vz() == spawn.z - target.z,
                "the arcing payload must carry source - target as its delta");
        helper.assertTrue(arcOptions.dx() == arcMote.vx() && arcOptions.scale() == 0.1F,
                "the delta must reach the payload and the scale must be the sender's 0.1");
        helper.assertTrue(arcOptions.lifetime() >= 36 && arcOptions.lifetime() <= 51,
                "the arcing lifetime is the shared 36 / (rand*0.3 + 0.7) roll");

        // Draw order is part of the fidelity claim: the same seed must give the
        // same burst.
        helper.assertTrue(eg.toString().equals(TcFxPlans.wispEg(spawn, 7, false,
                        FxParticleCounts.SETTING_ALL, RandomSource.create(9L)).toString()),
                "a wisp burst must be reproducible from the same random seed");
        helper.succeed();
    }

    private static <T> void assertWireRoundTrip(final GameTestHelper helper,
            final StreamCodec<RegistryFriendlyByteBuf, T> codec, final T sample) {
        final RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            codec.encode(buffer, sample);
            final T decoded = codec.decode(buffer);
            helper.assertTrue(sample.equals(decoded),
                    "wire round-trip mismatch: " + sample + " != " + decoded);
            helper.assertTrue(buffer.readableBytes() == 0, "trailing bytes for " + sample);
        } finally {
            buffer.release();
        }
    }

    private static boolean within(final double value, final double min, final double max) {
        return value >= min && value <= max;
    }
}
