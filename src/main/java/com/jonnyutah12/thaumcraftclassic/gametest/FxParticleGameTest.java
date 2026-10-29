package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoreSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSonicOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcVisSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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

    /** Every ported particle type must be live in the registry. */
    @GameTest(template = "empty")
    public static void portedParticleTypesRegistered(GameTestHelper helper) {
        for (String path : new String[] {"sparkle", "bubble",
                "wisp", "wisp_eg", "wisp_arcing",
                "vis_sparkle", "bore_sparkle", "essentia_trail",
                "shield_runes", "shield_runes_mob", "sonic"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.containsKey(id),
                    "particle type " + id + " is not in the registry");
        }
        // The whole slice (TC-M7-02-s + TC-M7-02-c-s): the mod registers exactly
        // these particle types, no more (on the pre-slice tree this was 0).
        long registered = BuiltInRegistries.PARTICLE_TYPE.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .count();
        helper.assertTrue(registered == 11,
                "thaumcraft-namespace particle types must be the 11 ported ones, found " + registered);
        helper.assertTrue(ModParticles.SPARCLE.get().codec() == TcSparkleOptions.MAP_CODEC,
                "sparkle type must decode through TcSparkleOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BUBBLE.get().codec() == TcBubbleOptions.MAP_CODEC,
                "bubble type must decode through TcBubbleOptions.MAP_CODEC");
        // The codec identity check is the wire contract: a packet that ships a
        // wisp payload must decode back into the same record the plan built.
        helper.assertTrue(ModParticles.WISP.get().codec() == TcWispOptions.MAP_CODEC
                        && ModParticles.WISP_EG.get().codec() == TcWispEgOptions.MAP_CODEC
                        && ModParticles.WISP_ARCING.get().codec() == TcWispArcingOptions.MAP_CODEC
                        && ModParticles.VIS_SPARKLE.get().codec() == TcVisSparkleOptions.MAP_CODEC
                        && ModParticles.BORE_SPARKLE.get().codec() == TcBoreSparkleOptions.MAP_CODEC
                        && ModParticles.ESSENTIA_TRAIL.get().codec() == TcEssentiaTrailOptions.MAP_CODEC
                        && ModParticles.SHIELD_RUNES.get().codec() == TcShieldOptions.MAP_CODEC
                        && ModParticles.SHIELD_RUNES_MOB.get().codec() == TcShieldOptions.MAP_CODEC
                        && ModParticles.SONIC.get().codec() == TcSonicOptions.MAP_CODEC,
                "every ported type must decode through its own options record");
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

    private static boolean within(final double value, final double min, final double max) {
        return value >= min && value <= max;
    }
}
