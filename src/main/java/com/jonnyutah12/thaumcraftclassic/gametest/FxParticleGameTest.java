package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWispRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcPlanarQuad;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcRuneOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.TreeSet;
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
        for (String path : new String[] {"sparkle", "bubble", "wisp", "wisp_eg", "wisp_arcing", "rune"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.containsKey(id),
                    "particle type " + id + " is not in the registry");
        }
        // The whole slice: the mod registers exactly the particle types the
        // M7-02 cards scope — 2 (TC-M7-02-s) + 6 beams/bolts (TC-M7-02-b-s)
        // + 3 wisp family (TC-M7-02-c-s-f1) + 1 rune decal
        // (TC-M7-02-c-s-f3).
        long registered = BuiltInRegistries.PARTICLE_TYPE.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .count();
        helper.assertTrue(registered == 12,
                "thaumcraft-namespace particle types must be the 12 ported ones, found " + registered);
        helper.assertTrue(ModParticles.SPARCLE.get().codec() == TcSparkleOptions.MAP_CODEC,
                "sparkle type must decode through TcSparkleOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BUBBLE.get().codec() == TcBubbleOptions.MAP_CODEC,
                "bubble type must decode through TcBubbleOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.RUNE.get().codec() == TcRuneOptions.MAP_CODEC,
                "rune type must decode through TcRuneOptions.MAP_CODEC");
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

        // ClientProxy.sourceStreamFX: the mote's anchor is the first triple (the
        // point it converges on), it starts at the second, and the reference's
        // motion triple (second - first) rides the payload because the ported
        // particle writes absolute positions (FXWispArcing.java:51-59, 96-103).
        final List<ParticleSpawn> arcing = TcFxPlans.wispArcing(spawn, target, 0x7F3F1F, false,
                FxParticleCounts.SETTING_DECREASED, RandomSource.create(13L));
        helper.assertTrue(arcing.size() == 1, "the arcing wisp on 'decreased' must be one mote");
        final ParticleSpawn arcMote = arcing.get(0);
        final TcWispArcingOptions arcOptions = (TcWispArcingOptions) arcMote.options();
        helper.assertTrue(arcMote.type() == ModParticles.WISP_ARCING.get(),
                "the arcing plan must use the registered wisp_arcing type");
        helper.assertTrue(arcMote.x() == spawn.x && arcMote.y() == spawn.y
                        && arcMote.z() == spawn.z,
                "the arcing mote spawns on its anchor triple, the point it converges on");
        helper.assertTrue(arcMote.vx() == 0.0 && arcMote.vy() == 0.0 && arcMote.vz() == 0.0,
                "the arcing mote re-derives its position every tick, so its spawn velocity is zero");
        helper.assertTrue(arcOptions.dx() == target.x - spawn.x
                        && arcOptions.dy() == target.y - spawn.y
                        && arcOptions.dz() == target.z - spawn.z,
                "the payload must carry the reference's motion triple (start - anchor), got "
                        + arcOptions.dx() + "," + arcOptions.dy() + "," + arcOptions.dz());
        helper.assertTrue(arcOptions.scale() == 0.1F,
                "the sender's arcing scale is the fixed 0.1, got " + arcOptions.scale());
        helper.assertTrue(arcOptions.lifetime() >= 36 && arcOptions.lifetime() <= 51,
                "the arcing lifetime is the shared 36 / (rand*0.3 + 0.7) roll");

        // Draw order is part of the fidelity claim: the same seed must give the
        // same burst.
        helper.assertTrue(eg.toString().equals(TcFxPlans.wispEg(spawn, 7, false,
                        FxParticleCounts.SETTING_ALL, RandomSource.create(9L)).toString()),
                "a wisp burst must be reproducible from the same random seed");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3: the rune decal's spawn shape, line-for-line from
     * {@code FXBlockRunes}'s constructor
     * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBlockRunes.java:16-37)
     * and the {@code ClientProxy.blockRunes} call site (ClientProxy.java:973-978).
     */
    @GameTest(template = "empty")
    public static void runeDecalMatchesOriginalSenderGeometry(GameTestHelper helper) {
        final double x = 10.0;
        final double y = 60.0;
        final double z = 10.0;
        final ParticleSpawn spawn = TcFxPlans.blockRunes(x, y, z, 0.5F, 0.1F, 0.7F, 25, -0.03F,
                RandomSource.create(20261108L));
        final TcRuneOptions options = (TcRuneOptions) spawn.options();

        helper.assertTrue(spawn.type() == ModParticles.RUNE.get(),
                "the rune plan must use the registered rune particle type");
        helper.assertTrue(spawn.x() == 10.5 && spawn.y() == 60.5 && spawn.z() == 10.5,
                "ClientProxy.blockRunes:974 adds the block-centre 0.5 on every axis, got "
                        + spawn.x() + "," + spawn.y() + "," + spawn.z());
        helper.assertTrue(spawn.vx() == 0.0 && spawn.vy() == 0.0 && spawn.vz() == 0.0,
                "FXBlockRunes.java:25 zeroes motionX/motionY/motionZ, got "
                        + spawn.vx() + "," + spawn.vy() + "," + spawn.vz());
        helper.assertTrue(options.lifetime() == 75,
                "FXBlockRunes.java:26 sets maxSize = 3 * dur, so dur 25 is 75 ticks, got "
                        + options.lifetime());
        helper.assertTrue(options.gravity() == -0.03F,
                "the matrix sender's gravity -0.03 must reach the particle through setGravity, got "
                        + options.gravity());
        helper.assertTrue(options.frame() >= 0 && options.frame() < TcFxPlans.RUNE_FRAMES,
                "the runeIndex roll (int)(Math.random()*16 + 224) % 16 is a 0..15 sheet column, got "
                        + options.frame());
        helper.assertTrue(options.yaw() >= 0 && options.yaw() < 360 && options.yaw() % 90 == 0,
                "FXBlockRunes.java:20 draws rotation as nextInt(4)*90, got " + options.yaw());
        helper.assertTrue(within(options.scale(), 0.5, 1.5),
                "FXBlockRunes.java:36 sets particleScale 1.0 + nextGaussian()*0.1, got " + options.scale());
        helper.assertTrue(within(options.offsetU(), 0.0, TcFxPlans.RUNE_OFFSET_X_SPAN),
                "FXBlockRunes.java:34 ofx is rand.nextFloat()*0.2, got " + options.offsetU());
        helper.assertTrue(within(options.offsetV(), TcFxPlans.RUNE_OFFSET_Y_BASE, 0.3),
                "FXBlockRunes.java:35 ofy is -0.3 + rand.nextFloat()*0.6, got " + options.offsetV());
        helper.assertTrue(Math.abs(TcFxPlans.red(options.color()) - 0.5F) < 0.004F
                        && Math.abs(TcFxPlans.green(options.color()) - 0.1F) < 0.004F
                        && Math.abs(TcFxPlans.blue(options.color()) - 0.7F) < 0.004F,
                "the ctor's particleRed/Green/Blue must survive the 1/255 quantisation of the packed "
                        + "colour, got " + TcFxPlans.red(options.color()) + ","
                        + TcFxPlans.green(options.color()) + "," + TcFxPlans.blue(options.color()));

        // The ctor's red-channel quirk (FXBlockRunes.java:16-18): a zero RED is
        // replaced by 1.0 BEFORE the colour is stored, so a black rune is red.
        final TcRuneOptions zeroRed = (TcRuneOptions) TcFxPlans.blockRunes(x, y, z, 0.0F, 0.2F, 0.3F,
                20, 0.0F, RandomSource.create(7L)).options();
        helper.assertTrue(TcFxPlans.red(zeroRed.color()) == 1.0F,
                "FXBlockRunes.java:16-18 replaces a zero red with 1.0, got "
                        + TcFxPlans.red(zeroRed.color()));

        // Draw order is part of the fidelity claim: the ctor's rolls run in
        // statement order, so a seeded plan must reproduce exactly.
        helper.assertTrue(spawn.equals(TcFxPlans.blockRunes(x, y, z, 0.5F, 0.1F, 0.7F, 25, -0.03F,
                        RandomSource.create(20261108L))),
                "a rune plan must be reproducible from the same random seed");

        // The wire shape must survive the network the same way the payload does.
        assertWireRoundTrip(helper, TcRuneOptions.STREAM_CODEC, options);
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3: the rune's render quad. The original's render chain is
     * {@code glRotatef(rotation,0,1,0)} then {@code glRotatef(90,0,0,1)} then
     * {@code glTranslated(ofx, ofy, -0.51)}
     * (FXBlockRunes.java:44-46), which lands the quad on one of four
     * AXIS-ALIGNED walls — no camera basis is involved, so the port keeps the
     * geometry exact (the BD-16 camera-basis adaptation does not apply here).
     */
    @GameTest(template = "empty")
    public static void runeQuadIsAnAxisAlignedWallDecal(GameTestHelper helper) {
        final TcPlanarQuad front = TcPlanarQuad.runeQuad(0, 0.1F, 0.2F, 1.0F);
        assertAxis(helper, front.u(), 0.0, 1.0, 0.0, "yaw 0: the sprite's up axis stays world +Y");
        assertAxis(helper, front.v(), -1.0, 0.0, 0.0, "yaw 0: Rz(90) puts the sprite's right axis on -X");
        assertAxis(helper, front.normal(), 0.0, 0.0, 1.0, "yaw 0: the decal faces +Z");
        assertAxis(helper, front.offset(), -0.2, 0.1, -0.51,
                "the ofx/ofy/-0.51 translate runs through the SAME rotation (FXBlockRunes.java:50)");

        final TcPlanarQuad right = TcPlanarQuad.runeQuad(90, 0.0F, 0.0F, 1.0F);
        assertAxis(helper, right.u(), 0.0, 1.0, 0.0, "yaw 90 keeps the sprite vertical");
        assertAxis(helper, right.v(), 0.0, 0.0, 1.0, "yaw 90: Rz(90) then Ry(90) puts the sprite axis on +Z");
        assertAxis(helper, right.normal(), 1.0, 0.0, 0.0, "yaw 90: the decal faces +X");

        final TcPlanarQuad back = TcPlanarQuad.runeQuad(180, 0.0F, 0.0F, 1.0F);
        assertAxis(helper, back.v(), 1.0, 0.0, 0.0, "yaw 180 mirrors the sprite axis");
        assertAxis(helper, back.normal(), 0.0, 0.0, -1.0, "yaw 180: the decal faces -Z");

        final TcPlanarQuad left = TcPlanarQuad.runeQuad(270, 0.0F, 0.0F, 1.0F);
        assertAxis(helper, left.v(), 0.0, 0.0, -1.0, "yaw 270 mirrors the sprite axis");
        assertAxis(helper, left.normal(), -1.0, 0.0, 0.0, "yaw 270: the decal faces -X");

        // var12 = 0.3F * particleScale (FXBlockRunes.java:55) is the quad's FULL
        // edge: the four vertices sit at +/-0.5*var12 (FXBlockRunes.java:56-59), so
        // the emitted corner — not just a stored extent field — must land there.
        helper.assertTrue(Math.abs(TcPlanarQuad.runeQuad(0, 0.0F, 0.0F, 1.0F).half() - 0.15) < 1e-9,
                "half-extent must be 0.15 * particleScale, got " + front.half());
        final double[] corner = front.vertex(12.5, 61.5, 12.5, -0.5, 0.5);
        helper.assertTrue(within(corner[0], 12.15 - 1e-5, 12.15 + 1e-5)
                        && within(corner[1], 61.35 - 1e-5, 61.35 + 1e-5)
                        && within(corner[2], 11.99 - 1e-5, 11.99 + 1e-5),
                "yaw 0, ofx 0.1, ofy 0.2, scale 1.0: the s=-0.5/t=+0.5 vertex is the offset "
                        + "(-0.2,0.1,-0.51) plus -0.15*u and +0.15*v, got "
                        + corner[0] + "," + corner[1] + "," + corner[2]);
        final double[] scaled = TcPlanarQuad.runeQuad(0, 0.1F, 0.2F, 2.0F)
                .vertex(12.5, 61.5, 12.5, -0.5, 0.5);
        helper.assertTrue(within(scaled[0], 12.0 - 1e-5, 12.0 + 1e-5)
                        && within(scaled[1], 61.3 - 1e-5, 61.3 + 1e-5),
                "particleScale 2.0 doubles var12 to 0.6, so the same vertex moves to "
                        + "(-0.3, -0.3) from the offset, got " + scaled[0] + "," + scaled[1]);
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3: the warding-stone sender. The original's
     * {@code BlockCosmeticSolid.randomDisplayTick} meta-3 body
     * (BlockCosmeticSolid.java:284-343) runs three branches with the counts
     * {@code particleCount(2)} / {@code particleCount(3)} / one victim rune.
     */
    @GameTest(template = "empty")
    public static void wardingStoneRuneBranchesMatchOriginalSender(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(12, 61, 12);

        final List<ParticleSpawn> sky = TcFxPlans.wardingStoneRunes(pos, true, false, -1.0,
                2, FxParticleCounts.SETTING_ALL, RandomSource.create(11L));
        helper.assertTrue(sky.size() == 4,
                "the sky branch runs particleCount(2), which is 2*2 = 4 runes on 'all', got " + sky.size());
        for (ParticleSpawn spawn : sky) {
            final TcRuneOptions o = (TcRuneOptions) spawn.options();
            helper.assertTrue(spawn.x() == 12.5 && spawn.z() == 12.5,
                    "the sender passes the block coords, so the rune centres on the block, got "
                            + spawn.x() + "," + spawn.z());
            helper.assertTrue(within(spawn.y(), 61.0 + 0.7 + 0.5 - 1e-5, 61.0 + 0.7 + 0.5 + 1e-5),
                    "BlockCosmeticSolid.java:295, 311 raises the rune by y + 0.7, got " + spawn.y());
            helper.assertTrue(o.lifetime() == 60, "the sky sender's dur 20 is 3*20 = 60 ticks, got "
                    + o.lifetime());
            helper.assertTrue(o.gravity() == -0.02F, "the sky sender's gravity is -0.02, got " + o.gravity());
            helper.assertTrue(within(TcFxPlans.red(o.color()), 0.2, 0.6)
                            && within(TcFxPlans.green(o.color()), 0.0, 0.3)
                            && within(TcFxPlans.blue(o.color()), 0.8, 1.0),
                    "the sky sender's colour is (0.2+rand*0.4, rand*0.3, 0.8+rand*0.2), got 0x"
                            + Integer.toHexString(o.color()));
        }

        final List<ParticleSpawn> covered = TcFxPlans.wardingStoneRunes(pos, false, true, -1.0,
                3, FxParticleCounts.SETTING_ALL, RandomSource.create(11L));
        helper.assertTrue(covered.size() == 6,
                "the covered branch runs particleCount(3) = 6 runes on 'all', got " + covered.size());
        for (ParticleSpawn spawn : covered) {
            final TcRuneOptions o = (TcRuneOptions) spawn.options();
            helper.assertTrue(o.lifetime() == 72, "the covered sender's dur 24 is 72 ticks, got "
                    + o.lifetime());
            helper.assertTrue(within(TcFxPlans.red(o.color()), 0.9, 1.0)
                            && within(TcFxPlans.green(o.color()), 0.0, 0.3)
                            && within(TcFxPlans.blue(o.color()), 0.0, 0.3),
                    "the covered sender's colour is (0.9+rand*0.1, rand*0.3, rand*0.3), got 0x"
                            + Integer.toHexString(o.color()));
        }

        final List<ParticleSpawn> victim = TcFxPlans.wardingStoneRunes(pos, false, false, 1.8,
                1, FxParticleCounts.SETTING_ALL, RandomSource.create(11L));
        helper.assertTrue(victim.size() == 1,
                "the entity branch breaks after the first living non-player, so exactly 1 rune, got "
                        + victim.size());
        final TcRuneOptions victimOptions = (TcRuneOptions) victim.get(0).options();
        helper.assertTrue(victim.get(0).y() >= 61.0 + 0.6 + 0.5 && victim.get(0).y() <= 61.0 + 0.6 + 1.8 + 0.5,
                "the victim rune rides the entity's eye height: y + 0.6 + rand*max(0.8, eyeHeight), got "
                        + victim.get(0).y());
        helper.assertTrue(victimOptions.gravity() == 0.0F,
                "the victim sender's gravity is 0.0, got " + victimOptions.gravity());
        helper.assertTrue(within(TcFxPlans.red(victimOptions.color()), 0.6, 1.0)
                        && TcFxPlans.green(victimOptions.color()) == 0.0F,
                "the victim sender's colour is (0.6+rand*0.4, 0.0, 0.3+rand*0.7), got 0x"
                        + Integer.toHexString(victimOptions.color()));

        // Minimal particles: the whole branch is gated away by particleCount.
        helper.assertTrue(TcFxPlans.wardingStoneRunes(pos, true, false, -1.0, 2,
                        FxParticleCounts.SETTING_MINIMAL, RandomSource.create(3L)).isEmpty(),
                "the original's particleCount(2) is 0 on the minimal setting, so no rune may spawn");
        helper.succeed();
    }

    /** Asserts a geometry axis vector against an expected unit vector. */
    /**
     * TC-M7-02-c-s-f3-a: the DRAWN-SET rule. One seeded stream cannot show that
     * a range is covered, so this sweeps the plan and asserts the drawn SET.
     * Frame: the original's {@code runeIndex} (FXBlockRunes.java:33) rolls
     * {@code (int)(Math.random()*16 + 224)} = 224..239, and its u leg
     * (FXBlockRunes.java:51-54) folds {@code runeIndex % 16} into u at the FIXED
     * v = 0.375 (line 53) — row 6 of the 16-cell grid, sheet cells 96..111 — so
     * the product's frame draw must cover all 16 cells of that row and nothing
     * outside them. Yaw: FXBlockRunes.java:20 draws {@code nextInt(4)*90}.
     * ofx/ofy: :34/:35.
     */
    @GameTest(template = "empty")
    public static void runeDecalDrawnSetCoversCellsAndAngles(final GameTestHelper helper) {
        final TreeSet<Integer> frames = new TreeSet<>();
        final TreeSet<Integer> yaws = new TreeSet<>();
        double minU = 10.0;
        double maxU = -10.0;
        double minV = 10.0;
        double maxV = -10.0;
        final RandomSource rand = RandomSource.create(20261109L);
        for (int i = 0; i < 8000; i++) {
            final TcRuneOptions o = (TcRuneOptions) TcFxPlans.blockRunes(0.5, 0.5, 0.5,
                    0.0F, 0.0F, 0.0F, 20, 0.0F, rand).options();
            frames.add(o.frame());
            yaws.add(o.yaw());
            minU = Math.min(minU, o.offsetU());
            maxU = Math.max(maxU, o.offsetU());
            minV = Math.min(minV, o.offsetV());
            maxV = Math.max(maxV, o.offsetV());
        }
        helper.assertTrue(frames.size() == TcFxPlans.RUNE_FRAMES
                        && frames.first() == 0 && frames.last() == TcFxPlans.RUNE_FRAMES - 1,
                "the frame draw must cover every cell of the original's fixed row "
                        + "(FXBlockRunes.java:33 rolls 224..239; :51-54 folds roll%16 into u at the "
                        + "fixed v 0.375 = row 6, cells 96..111), got " + frames);
        helper.assertTrue(yaws.size() == 4 && yaws.contains(0) && yaws.contains(90)
                        && yaws.contains(180) && yaws.contains(270),
                "yaw must draw exactly the four right angles (FXBlockRunes.java:20), got " + yaws);
        helper.assertTrue(minU >= 0.0 && maxU < TcFxPlans.RUNE_OFFSET_X_SPAN
                        && minU < 0.002 && maxU > TcFxPlans.RUNE_OFFSET_X_SPAN - 0.003,
                "the ofx draw must fill [0, 0.2) (FXBlockRunes.java:34), got ["
                        + minU + ", " + maxU + "]");
        helper.assertTrue(minV >= TcFxPlans.RUNE_OFFSET_Y_BASE && maxV < 0.3
                        && minV < TcFxPlans.RUNE_OFFSET_Y_BASE + 0.003 && maxV > 0.297,
                "the ofy draw must fill [-0.3, 0.3) (FXBlockRunes.java:35), got ["
                        + minV + ", " + maxV + "]");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3-a: the eldritch-sender branch. The original's
     * {@code BlockEldritch.randomDisplayTick} meta-10 (trap) branch
     * (BlockEldritch.java:295-311) draws an integer cell inside the block
     * (296-298), spawns ONLY where the cell is air (299), raises the rune by
     * y + 0.6 (303), jitters x/z by a raw nextFloat (302, 304), draws the
     * lifetime as 16 + nextInt(4) (309) and hands gravity 0.0F (310).
     */
    @GameTest(template = "empty")
    public static void eldritchTrapRuneCellsMatchOriginalSender(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(31, 61, 31);
        helper.setBlock(pos, ModBlocks.BLOCK_ELDRITCH.get().defaultBlockState()
                .setValue(BlockEldritch.VARIANT, 10));
        final int airCells = airNeighbours(helper, pos);
        helper.assertTrue(airCells >= 1,
                "the probe needs an air neighbour to see the isAirBlock gate, got " + airCells);
        final List<ParticleSpawn> runes = TcFxPlans.eldritchRunes(helper.getLevel(), pos, 10,
                FxParticleCounts.SETTING_ALL, RandomSource.create(19L));
        helper.assertTrue(runes.size() == airCells,
                "one rune per AIR cell only (BlockEldritch.java:299), got " + runes.size()
                        + " runes for " + airCells + " air cells");
        for (ParticleSpawn spawn : runes) {
            final TcRuneOptions o = (TcRuneOptions) spawn.options();
            helper.assertTrue(within(spawn.y(), pos.getY() + 0.6 + 0.5 - 1e-5,
                            pos.getY() + 0.6 + 0.5 + 1e-5),
                    "the trap sender raises the rune by y + 0.6 (BlockEldritch.java:303), got "
                            + spawn.y());
            helper.assertTrue(within(spawn.x(), pos.getX() + 0.5, pos.getX() + 1.5)
                            && within(spawn.z(), pos.getZ() + 0.5, pos.getZ() + 1.5),
                    "the cell roll is an in-block integer cell plus the proxy's 0.5 centring "
                            + "(BlockEldritch.java:296-298, 302, 304), got "
                            + spawn.x() + "," + spawn.z());
            helper.assertTrue(o.gravity() == 0.0F,
                    "the trap sender's gravity is 0.0 (BlockEldritch.java:310), got " + o.gravity());
            helper.assertTrue(o.lifetime() >= 3 * 16 && o.lifetime() <= 3 * 19,
                    "dur = 16 + nextInt(4) (BlockEldritch.java:309) → lifetime 48..57 "
                            + "(FXBlockRunes.java:26), got " + o.lifetime());
            // The ctor replaces a zero RED with 1.0 (FXBlockRunes.java:16-18), so a
            // drawn 0.0 red arrives as 1.0 — the band allows exactly that.
            final float red = TcFxPlans.red(o.color());
            helper.assertTrue((red <= 0.3F || red == 1.0F)
                            && within(TcFxPlans.green(o.color()), 0.0, 0.3)
                            && within(TcFxPlans.blue(o.color()), 0.0, 0.3),
                    "the trap sender's colour is (rand*0.3, rand*0.3, rand*0.3) "
                            + "(BlockEldritch.java:305-307), got 0x" + Integer.toHexString(o.color()));
        }
        helper.assertTrue(TcFxPlans.eldritchRunes(helper.getLevel(), pos, 0,
                        FxParticleCounts.SETTING_ALL, RandomSource.create(19L)).isEmpty(),
                "only meta 10 (ELDRITCH_TRAP) runs the branch (BlockEldritch.java:295)");
        helper.assertTrue(TcFxPlans.eldritchRunes(helper.getLevel(), pos, 10,
                        FxParticleCounts.SETTING_MINIMAL, RandomSource.create(19L)).isEmpty(),
                "particleCount(1) is 0 on the minimal setting, so no trap rune may spawn");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3-a: the infusion-matrix sender.
     * {@code TileInfusionMatrix.doEffects} (TileInfusionMatrix.java:812-832,
     * called from update() behind the client-side gate at line 192) spawns one
     * rune while a run is crafting (816): cell centre x/z (820, 822), y - 2.0
     * (821) — two blocks below the matrix, over the central pedestal —
     * gaussian drift (823-825), dur 25 (826), gravity -0.03 (827). The
     * {@code infuserstart}/{@code infuser} sounds in the same method belong to
     * the sound lane, not to this family.
     */
    @GameTest(template = "empty")
    public static void infusionMatrixRuneMatchesOriginalSender(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(41, 61, 41);
        final List<ParticleSpawn> runes = TcFxPlans.infusionMatrixRunes(pos,
                RandomSource.create(20261109L));
        helper.assertTrue(runes.size() == 1, "one rune per display tick, got " + runes.size());
        final ParticleSpawn spawn = runes.get(0);
        final TcRuneOptions o = (TcRuneOptions) spawn.options();
        helper.assertTrue(within(spawn.x(), 42.0 - 1e-9, 42.0 + 1e-9)
                        && within(spawn.z(), 42.0 - 1e-9, 42.0 + 1e-9),
                "the sender hands the cell centre (x+0.5, z+0.5) and the proxy adds its own 0.5 "
                        + "(TileInfusionMatrix.java:820, 822), got " + spawn.x() + "," + spawn.z());
        helper.assertTrue(spawn.y() == 61.0 - 2.0 + 0.5,
                "TileInfusionMatrix.java:821 spawns at y - 2.0, got " + spawn.y());
        helper.assertTrue(o.lifetime() == 75,
                "dur 25 (TileInfusionMatrix.java:826) is 3*25 = 75 ticks (FXBlockRunes.java:26), got "
                        + o.lifetime());
        helper.assertTrue(o.gravity() == -0.03F,
                "TileInfusionMatrix.java:827 gravity -0.03, got " + o.gravity());
        final float red = TcFxPlans.red(o.color());
        helper.assertTrue((red <= 0.5F || red == 1.0F)
                        && within(TcFxPlans.green(o.color()), 0.15, 0.5)
                        && within(TcFxPlans.blue(o.color()), 0.75, 1.0),
                "the matrix colour is (rand*0.5, 0.15+rand*0.35, 0.75+rand*0.25) "
                        + "(TileInfusionMatrix.java:827-829), got 0x" + Integer.toHexString(o.color()));
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f3-a: the thaumometer scan hook — the effect t_4ebbc3f5
     * deferred to this card ("the original's blockRunes particle effect
     * ItemThaumometer.java L68-79, L91-102, L132-143"). Two senders are ported:
     * the entity scan (68-79: x-0.5 / z-0.5 (71, 73), y + eyeHeight/2 (72),
     * colour (0.2, 0.0, 0.1) (74, 76), dur (int)(eyeHeight*15) (77), gravity
     * +0.03 (78)) and the block scan (91-102 and 132-143: y + 0.25 (95, 136),
     * dur 15 (100, 141), gravity -0.02 (101, 142)). The third site, the NODE
     * tile branch (91-102, {@code tile instanceof INode}), has no counterpart in
     * the port's scan (node milestone), so that site has no sender to hook.
     */
    @GameTest(template = "empty")
    public static void thaumometerScanRunesMatchOriginalSender(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(7, 61, 7);
        final double height = 1.8;
        final List<ParticleSpawn> entity = TcFxPlans.thaumometerScanRunes(pos, height, true,
                RandomSource.create(5L));
        helper.assertTrue(entity.size() == 1, "one rune per valid scan, got " + entity.size());
        final ParticleSpawn e = entity.get(0);
        final TcRuneOptions eo = (TcRuneOptions) e.options();
        helper.assertTrue(within(e.x(), 7.5 - 1e-5, 7.5 + 1e-5)
                        && within(e.z(), 7.5 - 1e-5, 7.5 + 1e-5),
                "ItemThaumometer.java:71, 73 centre the rune on the block (x-0.5, z-0.5), got "
                        + e.x() + "," + e.z());
        helper.assertTrue(within(e.y(), 61.0 + height / 2.0 + 0.5 - 1e-5,
                        61.0 + height / 2.0 + 0.5 + 1e-5),
                "ItemThaumometer.java:72 rides y + eyeHeight/2, got " + e.y());
        helper.assertTrue(eo.lifetime() == 3 * (int) (height * 15.0),
                "ItemThaumometer.java:77 dur = (int)(eyeHeight*15), got " + eo.lifetime());
        helper.assertTrue(eo.gravity() == 0.03F,
                "ItemThaumometer.java:78 gravity +0.03, got " + eo.gravity());
        helper.assertTrue(within(TcFxPlans.blue(eo.color()), 0.3, 0.5),
                "ItemThaumometer.java:76 blue = 0.1 + 0.2, got " + TcFxPlans.blue(eo.color()));

        final List<ParticleSpawn> block = TcFxPlans.thaumometerScanRunes(pos, height, false,
                RandomSource.create(5L));
        helper.assertTrue(block.size() == 1, "one rune per valid block scan, got " + block.size());
        final TcRuneOptions bo = (TcRuneOptions) block.get(0).options();
        helper.assertTrue(block.get(0).y() == 61.0 + 0.25 + 0.5,
                "ItemThaumometer.java:95, 136 raise the block scan by y + 0.25, got "
                        + block.get(0).y());
        helper.assertTrue(bo.lifetime() == 45,
                "ItemThaumometer.java:100, 141 dur 15 is 45 ticks, got " + bo.lifetime());
        helper.assertTrue(bo.gravity() == -0.02F,
                "ItemThaumometer.java:101, 142 gravity -0.02, got " + bo.gravity());
        helper.assertTrue(Math.abs(TcFxPlans.red(bo.color()) - 0.2F) < 0.004F
                        && TcFxPlans.green(bo.color()) == 0.0F
                        && Math.abs(TcFxPlans.blue(bo.color()) - 0.1F) < 0.004F,
                "ItemThaumometer.java:74, 76 hard-code the scan colour (0.2, 0.0, 0.1) with no "
                        + "randomness, got 0x" + Integer.toHexString(bo.color()));
        helper.succeed();
    }

    private static int airNeighbours(final GameTestHelper helper, final BlockPos pos) {
        int air = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (helper.getLevel().getBlockState(pos.relative(new Vec3(dx, dy, dz))).isAir()) {
                        air++;
                    }
                }
            }
        }
        return air;
    }

    private static void assertAxis(final GameTestHelper helper, final double[] axis,
            final double x, final double y, final double z, final String what) {
        helper.assertTrue(Math.abs(axis[0] - x) < 1e-9 && Math.abs(axis[1] - y) < 1e-9
                        && Math.abs(axis[2] - z) < 1e-9,
                what + ": expected (" + x + "," + y + "," + z + "), got ("
                        + axis[0] + "," + axis[1] + "," + axis[2] + ")");
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
