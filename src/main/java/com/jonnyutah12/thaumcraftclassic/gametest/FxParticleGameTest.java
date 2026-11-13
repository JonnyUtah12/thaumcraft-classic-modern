package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticOpaque;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxShieldRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWardRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWispRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWardHitEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWardOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    /**
     * Every particle id the M7-02 cards register, in one list shared by both
     * registry probes: 2 (TC-M7-02-s) + 6 beams/bolts (TC-M7-02-b-s) + 3 wisp
     * family (TC-M7-02-c-s-f1) + 11 shared families (TC-M7-02-c-s-f5: scorch,
     * smoke_spiral, smoke_trail, swarm, vent, bore_sparkle, vis_sparkle,
     * essentia_trail, sonic, drop, burst).
     *
     * <p>The two registry probes each carried their own hardcoded count pin
     * (first 8, then 11), so every slice that registered types had to remember
     * to bump two literals in two files, and a slice that registered a type
     * whose id nobody listed stayed invisible. The set is the contract now:
     * {@link #assertParticleRegistryIsExact} names the missing ids and the extra
     * ones, so an unlisted registration is red and a deleted definition is red.
     */
    public static final Set<String> PORTED_PARTICLE_PATHS = Set.of(
            "sparkle", "bubble",
            "bolt", "beam", "beam_impact", "beam_golem_boss", "arc", "sparkle_glow",
            "wisp", "wisp_eg", "wisp_arcing",
            "scorch", "smoke_spiral", "smoke_trail", "swarm", "vent",
            "bore_sparkle", "vis_sparkle", "essentia_trail", "sonic", "drop", "burst",
            "ward", "shield_runes", "shield_runes_mob");

    /** The thaumcraft particle registry must be exactly {@link #PORTED_PARTICLE_PATHS}. */
    static void assertParticleRegistryIsExact(GameTestHelper helper) {
        Set<ResourceLocation> registered = new TreeSet<>();
        BuiltInRegistries.PARTICLE_TYPE.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .forEach(key -> registered.add(key.key().location()));
        Set<ResourceLocation> expected = new TreeSet<>();
        for (String path : PORTED_PARTICLE_PATHS) {
            expected.add(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
        }
        Set<ResourceLocation> missing = new TreeSet<>(expected);
        missing.removeAll(registered);
        Set<ResourceLocation> extra = new TreeSet<>(registered);
        extra.removeAll(expected);
        helper.assertTrue(missing.isEmpty() && extra.isEmpty(),
                "thaumcraft particle types must be exactly the " + expected.size() + " ported ids"
                        + "; missing=" + missing + " extra=" + extra
                        + " (registered=" + registered.size() + ")");
    }

    /** Every ported particle type must be live in the registry, and nothing else. */
    @GameTest(template = "empty")
    public static void portedParticleTypesRegistered(GameTestHelper helper) {
        for (String path : PORTED_PARTICLE_PATHS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.containsKey(id),
                    "particle type " + id + " is not in the registry");
        }
        assertParticleRegistryIsExact(helper);
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
     * TC-M7-02-c-s-f4: the ward hexagram's plan. The original fires ONE
     * {@code FXBlockWard} per hit face ({@code BlockCosmeticOpaque
     * .addHitEffects}, line 68-82), riding the face normal by 0.505 with the
     * constructor's jitter zeroed along that normal, and it is static
     * (FXBlockWard.java:57-59). Every face must show that geometry, so the
     * probe walks all six.
     */
    @GameTest(template = "empty")
    public static void wardHexagramPlanRidesTheFaceNormal(GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        for (final Direction side : Direction.values()) {
            final List<ParticleSpawn> plan =
                    TcFxPlans.wardBlock(pos, side, 0.25, 0.5, 0.75, RandomSource.create(11L));
            helper.assertTrue(plan.size() == 1,
                    "one hexagram per hit face, got " + plan.size() + " for " + side);
            final ParticleSpawn spawn = plan.get(0);
            helper.assertTrue(spawn.type() == ModParticles.WARD.get(),
                    "the ward plan must use the registered ward type");
            helper.assertTrue(spawn.vx() == 0.0 && spawn.vy() == 0.0 && spawn.vz() == 0.0,
                    side + ": the hexagram is static (FXBlockWard.java:57-59)");
            final double ex = pos.getX() + 0.5 + side.getStepX() * FxWardRules.FACE_OFFSET;
            final double ey = pos.getY() + 0.5 + side.getStepY() * FxWardRules.FACE_OFFSET;
            final double ez = pos.getZ() + 0.5 + side.getStepZ() * FxWardRules.FACE_OFFSET;
            final double offX = spawn.x() - ex;
            final double offY = spawn.y() - ey;
            final double offZ = spawn.z() - ez;
            final double along = offX * side.getStepX() + offY * side.getStepY() + offZ * side.getStepZ();
            helper.assertTrue(along == 0.0,
                    side + ": the jitter must not move the quad off the face normal, got " + along);
            // MEASURED (this card, run 3, after my 0.1 bound went red at
            // 0.3925…): the jitter is per component, not in-plane length.
            // FXBlockWard.java:33-35 turns each hit offset f into
            // clamp(f - 0.6 + rand*0.2, -0.4, 0.4), rand in [0,1), and the
            // constructor zeroes only the component along the normal
            // (lines 36-46). The band endpoints below ARE that expression at
            // roll 0 and roll 1, so the assertion pins the measured rule.
            final double[] hit = {0.25, 0.5, 0.75};
            final double[] off = {offX, offY, offZ};
            for (final Direction.Axis axis : Direction.Axis.values()) {
                if (FxWardRules.zeroesAxis(side, axis)) {
                    helper.assertTrue(off[axis.ordinal()] == 0.0,
                            side + ": the " + axis + " jitter lies on the normal and must be "
                                    + "exactly 0 (FXBlockWard.java:36-46), got " + off[axis.ordinal()]);
                } else {
                    final double lo = FxWardRules.jitter(hit[axis.ordinal()], 0.0);
                    final double hi = FxWardRules.jitter(hit[axis.ordinal()], 1.0);
                    helper.assertTrue(off[axis.ordinal()] >= lo && off[axis.ordinal()] < hi,
                            side + ": the " + axis + " jitter lies in [" + lo + ", " + hi
                                    + ") = clamp(f - 0.6 + rand*0.2, -0.4, 0.4) of f = "
                                    + hit[axis.ordinal()] + ", got " + off[axis.ordinal()]);
                }
            }
            if (spawn.options() instanceof TcWardOptions options) {
                helper.assertTrue(options.side() == side.get3DDataValue(),
                        "the draw-side must travel on the wire, got " + options.side());
                helper.assertTrue(options.rotation() >= 0.0F && options.rotation() < 360.0F,
                        "the face roll is nextInt(360), got " + options.rotation());
                helper.assertTrue(within(options.scale(), 0.4, 2.4),
                        "the scale is 1.4 + gaussian*0.3 (FXBlockWard.java:24-25), inside the "
                                + "1.4 +- 1.0 band the wisp probe pins, got " + options.scale());
                helper.assertTrue(options.lifetime() >= 12 && options.lifetime() <= 16,
                        "the lifetime is 12 + rand(5) ticks (FXBlockWard.java:24), got "
                                + options.lifetime());
            } else {
                helper.fail("the ward plan must carry TcWardOptions");
            }
        }
        helper.assertTrue(TcFxPlans.wardBlock(pos, Direction.UP, 0.25, 0.5, 0.75,
                        RandomSource.create(5L)).toString()
                .equals(TcFxPlans.wardBlock(pos, Direction.UP, 0.25, 0.5, 0.75,
                        RandomSource.create(5L)).toString()),
                "a ward hexagram must be reproducible from the same random seed");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f4: the ward gate on a real block state — warded glass
     * (metadata 2) fires and returns true (which suppresses vanilla's digging
     * particles); the amber metas return false and fall through
     * (BlockCosmeticOpaque.java:74-81). The particle hand-off itself is a
     * documented game-test-server no-op (see the class note), so what this
     * probe can prove is the branch, which is the fidelity claim.
     */
    @GameTest(template = "empty")
    public static void wardHexagramFiresForWardedGlassOnly(GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        // The landed block's variant property is 0..2 (BlockCosmeticOpaque.java:49),
        // so the original's amber metas are variants 0 and 1 here and 2 is warded
        // glass — the landed ids, not the original's metadata numbering.
        final BlockState base = CatalogDecorativeBlocks.BLOCK_COSMETIC_OPAQUE.get()
                .defaultBlockState();
        final BlockState warded =
                base.setValue(BlockCosmeticOpaque.VARIANT, BlockCosmeticOpaque.VARIANT_WARDED_GLASS);
        final BlockState amber =
                base.setValue(BlockCosmeticOpaque.VARIANT, BlockCosmeticOpaque.VARIANT_AMBER_BRICKS);
        final BlockHitResult hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        helper.assertTrue(TcWardHitEffects.addHitEffects(warded, helper.getLevel(), hit, null),
                "warded glass (meta 2) must fire the ward and swallow vanilla's particles");
        helper.assertTrue(!TcWardHitEffects.addHitEffects(amber, helper.getLevel(), hit, null),
                "the amber meta must fall through to vanilla (the else return false)");
        helper.succeed();
    }

    /**
     * TC-M7-02-c-s-f4: the orbiting runes follow the packet's sentinels
     * (PacketFXShield.java:38-79). The runes ride the SOURCE entity; the target
     * only aims them; {@code -1} is two runes (pitch 90 then 270 in the
     * handler's order); a target id that is gone aims down; a non-mob source
     * takes the flat frame and the non-mob particle type (the mob form swaps
     * the 1..15 ripple frames — BD-45).
     */
    @GameTest(template = "empty")
    public static void shieldRunePlanFollowsThePacketSentinels(GameTestHelper helper) {
        // MEASURED (this card, run 3): helper.spawn(EntityType, BlockPos)
        // TRANSLATES its position by the template origin — the cell stays
        // template-RELATIVE (the note at GolemAiInstallationGameTest:205-211).
        // Passing absolutePos put the cow out of the structure and the plan
        // looked up a null entity: the -1 sentinel then produced 0 runes.
        final Entity cow = helper.spawn(EntityType.COW, new BlockPos(1, 3, 1));
        final Entity tnt = helper.spawn(EntityType.TNT, new BlockPos(5, 3, 1));
        final RandomSource rand = RandomSource.create(4L);

        helper.assertTrue(TcFxPlans.shieldRunes(helper.getLevel(), 999999,
                        FxShieldRules.TARGET_ANY, false, rand).isEmpty(),
                "no source entity, no runes (PacketFXShield.java:39-40)");

        final List<ParticleSpawn> bothPitches = TcFxPlans.shieldRunes(helper.getLevel(),
                cow.getId(), FxShieldRules.TARGET_ANY, false, rand);
        helper.assertTrue(bothPitches.size() == 2,
                "the -1 sentinel is two runes, got " + bothPitches.size());
        for (final ParticleSpawn spawn : bothPitches) {
            helper.assertTrue(spawn.type() == ModParticles.SHIELD_RUNES_MOB.get(),
                    "a mob source must ride the ripple-frame type (BD-45)");
            if (spawn.options() instanceof TcShieldOptions options) {
                helper.assertTrue(options.mob(), "the mob branch must set the mob flag");
                helper.assertTrue(options.yaw() == FxShieldRules.SENTINEL_YAW,
                        "a sentinel target gives yaw 180 (PacketFXShield.java:45), got "
                                + options.yaw());
                helper.assertTrue(options.sourceEntityId() == cow.getId(),
                        "the rune must carry the source id it orbits (BD-46)");
                helper.assertTrue(options.scale() == (float) (cow.getBbWidth() * FxShieldRules.SCALE_FROM_WIDTH),
                        "the scale is 0.4 * the source width, got " + options.scale());
                helper.assertTrue(options.lifetime() >= 8 && options.lifetime() < 16,
                        "the rune lifetime is 8 + rand*(8/2), got " + options.lifetime());
            } else {
                helper.fail("the shield plan must carry TcShieldOptions");
            }
            helper.assertTrue(within(spawn.x(), cow.getX() - 0.5, cow.getX() + 0.5)
                            && within(spawn.z(), cow.getZ() - 0.5, cow.getZ() + 0.5),
                    "the rune rides the source's x/z (FXShieldRunes.java:39-41)");
            helper.assertTrue(within(spawn.y(), cow.getBoundingBox().minY,
                            cow.getBoundingBox().maxY),
                    "the rune rides the source's AABB centre height");
        }
        if (bothPitches.size() == 2
                && bothPitches.get(0).options() instanceof TcShieldOptions first
                && bothPitches.get(1).options() instanceof TcShieldOptions second) {
            helper.assertTrue(first.pitch() == FxShieldRules.SENTINEL_PITCH_DOWN
                            && second.pitch() == FxShieldRules.SENTINEL_PITCH_UP,
                    "the -1 branch is 270 then 90, the handler's own order "
                            + "(PacketFXShield.java:63-65), got " + first.pitch() + "," + second.pitch());
        }

        final List<ParticleSpawn> gone = TcFxPlans.shieldRunes(helper.getLevel(),
                cow.getId(), 999998, false, rand);
        helper.assertTrue(gone.size() == 1, "a gone target is one rune, got " + gone.size());
        if (gone.size() == 1 && gone.get(0).options() instanceof TcShieldOptions goneOptions) {
            helper.assertTrue(goneOptions.pitch() == FxShieldRules.SENTINEL_PITCH_DOWN
                            && goneOptions.yaw() == FxShieldRules.SENTINEL_YAW,
                    "a gone target aims straight down (PacketFXShield.java:55-58)");
        }

        final List<ParticleSpawn> aimed = TcFxPlans.shieldRunes(helper.getLevel(),
                cow.getId(), tnt.getId(), false, rand);
        helper.assertTrue(aimed.size() == 1, "a real target is one rune, got " + aimed.size());
        if (aimed.size() == 1 && aimed.get(0).options() instanceof TcShieldOptions aimedOptions) {
            final double dx = cow.getX() - tnt.getX();
            final double dz = cow.getZ() - tnt.getZ();
            final double dy = (cow.getBoundingBox().minY + cow.getBoundingBox().maxY) / 2.0
                    - (tnt.getBoundingBox().minY + tnt.getBoundingBox().maxY) / 2.0;
            helper.assertTrue(Math.abs(aimedOptions.yaw()
                            - FxShieldRules.yawTo(dx, dz)) < 1.0e-4F,
                    "the aim yaw must be the source-target delta (BD-16), got "
                            + aimedOptions.yaw() + " want " + FxShieldRules.yawTo(dx, dz));
            helper.assertTrue(Math.abs(aimedOptions.pitch()
                            - FxShieldRules.pitchTo(dy, FxShieldRules.horizontal(dx, dz))) < 1.0e-4F,
                    "the aim pitch must be the source-target delta, got " + aimedOptions.pitch());
        }

        final List<ParticleSpawn> flat = TcFxPlans.shieldRunes(helper.getLevel(),
                tnt.getId(), FxShieldRules.TARGET_ANY, false, rand);
        helper.assertTrue(flat.size() == 2, "the -1 sentinel is two runes for a mob too, got "
                + flat.size());
        for (final ParticleSpawn spawn : flat) {
            helper.assertTrue(spawn.type() == ModParticles.SHIELD_RUNES.get(),
                    "primed tnt is not a Mob: the runes take the flat frame and the base type");
            if (spawn.options() instanceof TcShieldOptions options) {
                helper.assertTrue(!options.mob(), "the non-mob branch must clear the mob flag");
            }
        }
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
