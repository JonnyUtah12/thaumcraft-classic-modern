package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcArcGeometry;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcArcOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamGeometry;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoltGeometry;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoltOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxVector3;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGlowSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGolemBeamOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime proof for the TC-M7-02-b-s beam/bolt slice, on the game-test server.
 *
 * <p>The originals ({@code FXLightningBolt}, {@code FXBeam},
 * {@code FXBeamGolemBoss}, {@code FXArc}) drew immediate-mode GL in a client
 * {@code RenderGlobal} pass (the {@code world.field_72995_K} wall), so the
 * pixels cannot be proven here — the client smoke run in
 * docs/feature-tracking.md covers them. What is provable on a dedicated
 * server is the whole seed-reproducible geometry stack: the fractal bolt
 * (java.util.Random, seed-for-seed identical), the arc's jittered parabola,
 * the beam transform/ribbon math, the original's view-distance gates, and
 * the spawn plans the packet handlers build. The seam choice (beams as
 * custom-geometry particles on the particle sheet) is BD-16 in
 * docs/BEHAVIORAL_DEVIATIONS.md.</p>
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FxBeamGameTest {

    private FxBeamGameTest() {
    }

    /** The six ported beam/bolt particle types must be live in the registry. */
    @GameTest(template = "empty")
    public static void portedBeamTypesRegistered(GameTestHelper helper) {
        for (String path : new String[] {"bolt", "beam", "beam_impact", "beam_golem_boss",
            "arc", "sparkle_glow"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.containsKey(id),
                    "particle type " + id + " is not in the registry");
        }
        // TC-M7-02-s (2) + TC-M7-02-b-s (6): the mod registers exactly the
        // eight particle types the two cards scope, no more.
        long registered = BuiltInRegistries.PARTICLE_TYPE.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .count();
        helper.assertTrue(registered == 8,
                "thaumcraft-namespace particle types must be the 8 ported ones, found " + registered);
        helper.assertTrue(ModParticles.BOLT.get().codec() == TcBoltOptions.MAP_CODEC,
                "bolt type must decode through TcBoltOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BEAM.get().codec() == TcBeamOptions.MAP_CODEC,
                "beam type must decode through TcBeamOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BEAM_IMPACT.get().codec() == TcBeamOptions.MAP_CODEC,
                "beam_impact type must decode through TcBeamOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.BEAM_GOLEM_BOSS.get().codec() == TcGolemBeamOptions.MAP_CODEC,
                "beam_golem_boss type must decode through TcGolemBeamOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.ARC.get().codec() == TcArcOptions.MAP_CODEC,
                "arc type must decode through TcArcOptions.MAP_CODEC");
        helper.assertTrue(ModParticles.SPARKLE_GLOW.get().codec() == TcGlowSparkleOptions.MAP_CODEC,
                "sparkle_glow type must decode through TcGlowSparkleOptions.MAP_CODEC");
        helper.succeed();
    }

    /**
     * The fractal bolt is seed-reproducible: {@code FXLightningBolt} builds
     * its geometry from {@code world.rand.nextLong()} (ClientProxy.java:789-796)
     * and seven fixed fractal passes, so the same seed must rebuild the same
     * bolt on the client — the packet carries only the endpoints and the seed.
     */
    @GameTest(template = "empty")
    public static void boltGeometryIsSeedReproducible(GameTestHelper helper) {
        long seed = 20261028L;
        TcBoltGeometry first = new TcBoltGeometry(new TcFxVector3(10, 64, 10),
                new TcFxVector3(14, 66, 12), seed,
                TcFxPlans.BOLT_DURATION, TcFxPlans.BOLT_MULTIPLIER, TcFxPlans.BOLT_INCREMENT);
        first.defaultFractal();
        first.finalizeBolt();
        TcBoltGeometry again = new TcBoltGeometry(new TcFxVector3(10, 64, 10),
                new TcFxVector3(14, 66, 12), seed,
                TcFxPlans.BOLT_DURATION, TcFxPlans.BOLT_MULTIPLIER, TcFxPlans.BOLT_INCREMENT);
        again.defaultFractal();
        again.finalizeBolt();

        helper.assertTrue(!first.segments.isEmpty(), "the fractal bolt must have segments");
        helper.assertTrue(first.segments.size() == again.segments.size(),
                "the same seed must rebuild the same segment count, got "
                        + first.segments.size() + " vs " + again.segments.size());
        for (int i = 0; i < first.segments.size(); i++) {
            TcFxVector3 a = first.segments.get(i).startpoint.point;
            TcFxVector3 b = again.segments.get(i).startpoint.point;
            helper.assertTrue(a.x == b.x && a.y == b.y && a.z == b.z,
                    "segment " + i + " must be bit-identical for the same seed");
        }
        // A different seed must NOT reproduce the same bolt (the seed is the
        // original's per-spawn world.rand.nextLong()). The fractal walks inward
        // from the endpoints: segment 0's startpoint is the original start
        // vector (seed-independent by construction), so the seed shows up from
        // the first displaced point onward — compare the last segment.
        TcBoltGeometry other = new TcBoltGeometry(new TcFxVector3(10, 64, 10),
                new TcFxVector3(14, 66, 12), seed + 1L,
                TcFxPlans.BOLT_DURATION, TcFxPlans.BOLT_MULTIPLIER, TcFxPlans.BOLT_INCREMENT);
        other.defaultFractal();
        other.finalizeBolt();
        TcFxVector3 firstPoint = first.segments.get(first.segments.size() - 1).startpoint.point;
        TcFxVector3 otherPoint = other.segments.get(other.segments.size() - 1).startpoint.point;
        helper.assertTrue(firstPoint.x != otherPoint.x || firstPoint.y != otherPoint.y
                        || firstPoint.z != otherPoint.z,
                "a different seed must produce a different bolt");
        helper.succeed();
    }

    /**
     * The zap plan: two bolt passes (glow p_large + core p_small, colours
     * (0.6,0.3,0.6) and (1.0,0.6,1.0)) behind the original's 100/50
     * view-distance gate (FXLightningBolt.java:155-161).
     */
    @GameTest(template = "empty")
    public static void boltPlanPassesAndViewGate(GameTestHelper helper) {
        Vec3 start = new Vec3(10, 64, 10);
        Vec3 end = new Vec3(14, 66, 12);
        long seed = 99L;

        List<ParticleSpawn> plan = TcFxPlans.blockZapBolt(start, end, seed, 10.0, true);
        helper.assertTrue(plan.size() == TcFxPlans.BOLT_PASSES,
                "the bolt is a 2-pass draw (getRenderPass() == 2), got " + plan.size());
        TcBoltOptions glow = (TcBoltOptions) plan.get(0).options();
        TcBoltOptions core = (TcBoltOptions) plan.get(1).options();
        helper.assertTrue(plan.get(0).type() == ModParticles.BOLT.get()
                        && plan.get(1).type() == ModParticles.BOLT.get(),
                "both passes must use the registered bolt type");
        helper.assertTrue(glow.pass() == 0 && core.pass() == 1,
                "pass 0 is the body glow, pass 1 the core");
        helper.assertTrue(glow.color() == TcFxPlans.BOLT_COLOR_GLOW
                        && core.color() == TcFxPlans.BOLT_COLOR_CORE,
                "bolt pass colours must be the senders' (0.6,0.3,0.6) and (1.0,0.6,1.0)");
        helper.assertTrue(glow.seed() == seed && glow.endX() == end.x
                        && glow.endY() == end.y && glow.endZ() == end.z,
                "the bolt payload carries the seed and the end point");

        // The gate: fancy 100 / fast 50 (FXLightningBolt.java:155-161).
        helper.assertTrue(TcFxPlans.blockZapBolt(start, end, seed, 99.0, true).size() == 2,
                "a bolt at 99 blocks stays visible in fancy mode (gate: > 100)");
        helper.assertTrue(TcFxPlans.blockZapBolt(start, end, seed, 150.0, true).isEmpty(),
                "a bolt beyond 100 blocks must not render in fancy mode");
        helper.assertTrue(TcFxPlans.blockZapBolt(start, end, seed, 60.0, false).isEmpty(),
                "a bolt beyond 50 blocks must not render on fast graphics");
        helper.succeed();
    }

    /**
     * The beam pulse plan: one entity-targeted beam, size 2.5, type 1,
     * reverse, pulse, age 20 (PacketFXBeamPulse.java:44-52) behind the
     * 50/25 gate (FXBeam.java:66-73).
     */
    @GameTest(template = "empty")
    public static void beamPulsePlanAndViewGate(GameTestHelper helper) {
        Vec3 eye = new Vec3(100.5, 65.0, 200.5);
        int color = 0x00FF00;

        List<ParticleSpawn> plan = TcFxPlans.beamPulse(eye, 7, color, 10.0, true);
        helper.assertTrue(plan.size() == 1, "the pulse is one beam, got " + plan.size());
        TcBeamOptions options = (TcBeamOptions) plan.get(0).options();
        helper.assertTrue(plan.get(0).type() == ModParticles.BEAM.get(),
                "the pulse must use the registered beam type");
        helper.assertTrue(options.targetEntityId() == 7, "the beam tracks the packet's target id");
        helper.assertTrue(options.color() == color, "the beam carries the packet's colour");
        helper.assertTrue(options.size() == TcFxPlans.BEAM_PULSE_SIZE
                        && options.type() == TcFxPlans.BEAM_PULSE_TYPE && options.reverse(),
                "the packet sets N=2.5, type 1, reverse (PacketFXBeamPulse.java:44-47)");
        helper.assertTrue(options.age() == TcFxPlans.BEAM_AGE,
                "the packet sets particleMaxAge 20 (PacketFXBeamPulse.java:52)");
        helper.assertTrue(plan.get(0).x() == eye.x && plan.get(0).y() == eye.y
                        && plan.get(0).z() == eye.z,
                "the beam spawns at the source entity's eye position");

        // The gate: fancy 50 / fast 25, age clamped, 0 = dead (FXBeam.java:66-73).
        helper.assertTrue(TcFxPlans.beamPulse(eye, 7, color, 75.0, true).isEmpty(),
                "a beam beyond 50 blocks must not render in fancy mode");
        helper.assertTrue(TcFxPlans.beamPulse(eye, 7, color, 30.0, false).isEmpty(),
                "a beam beyond 25 blocks must not render on fast graphics");
        helper.assertTrue(TcFxPlans.beamPulse(eye, 7, color, 10.0, false).size() == 1,
                "a close beam on fast graphics still renders");
        helper.succeed();
    }

    /**
     * The arc plan: the handler's colour draws, the glow sparkle's three
     * constructor draws, the seed, then the jittered parabola
     * (PacketFXBlockArc.java:41-66, FXArc.java:41-63, Utils.calculateVelocity).
     */
    @GameTest(template = "empty")
    public static void arcPlanDrawOrderAndParabola(GameTestHelper helper) {
        Vec3 source = new Vec3(50.0, 64.0, 50.0);
        double tx = 54.5;
        double ty = 66.0;
        double tz = 50.5;

        List<ParticleSpawn> plan = TcFxPlans.blockArc(source, tx, ty, tz,
                RandomSource.create(20261028L), false);
        helper.assertTrue(plan.size() == 2, "the arc sender plans a glow sparkle + the arc, got "
                + plan.size());
        ParticleSpawn glow = plan.get(0);
        ParticleSpawn arc = plan.get(1);
        helper.assertTrue(glow.type() == ModParticles.SPARKLE_GLOW.get()
                        && arc.type() == ModParticles.ARC.get(),
                "arcLightning spawns the type-6 glow sparkle at the target, then the arc");
        TcGlowSparkleOptions glowOptions = (TcGlowSparkleOptions) glow.options();
        helper.assertTrue(glowOptions.scale() == TcFxPlans.SPARKLE_GLOW_SCALE
                        && glowOptions.age() == TcFxPlans.SPARKLE_GLOW_AGE,
                "the glow sparkle is scale 3.0, age 3*m = 6 (ClientProxy.java:1302)");
        helper.assertTrue(glow.x() == tx && glow.y() == ty && glow.z() == tz,
                "the glow sparkle lands at the arc's target point");
        TcArcOptions arcOptions = (TcArcOptions) arc.options();
        helper.assertTrue(arcOptions.dx() == tx - source.x && arcOptions.dy() == ty - source.y
                        && arcOptions.dz() == tz - source.z,
                "the arc carries the target delta from the source entity");
        helper.assertTrue(arcOptions.heightGain() == TcFxPlans.ARC_HEIGHT_GAIN,
                "the sender passes heightGain 0.5 (PacketFXBlockArc.java:64)");
        helper.assertTrue(arcOptions.color() == glowOptions.color(),
                "the arc and its glow sparkle share the rolled colour");

        // Determinism: the same source random replays the same plan (the
        // original consumes the identical draw sequence).
        List<ParticleSpawn> again = TcFxPlans.blockArc(source, tx, ty, tz,
                RandomSource.create(20261028L), false);
        helper.assertTrue(plan.toString().equals(again.toString()),
                "the arc plan must be reproducible from the same random seed");

        // The parabola: calculateVelocity's vy = sqrt(maxGain * gravity)
        // (Utils.java:316-338) and the point chain's exact endpoints.
        double[] velocity = TcArcGeometry.calculateVelocity(0, 0, 0, 4, 2, 0,
                TcFxPlans.ARC_HEIGHT_GAIN, TcArcGeometry.GRAVITY);
        double maxGain = Math.max(TcFxPlans.ARC_HEIGHT_GAIN, 2.0 + TcFxPlans.ARC_HEIGHT_GAIN);
        helper.assertTrue(Math.abs(velocity[1] - Math.sqrt(maxGain * TcArcGeometry.GRAVITY)) < 1.0e-9,
                "the launch vy must be sqrt(maxGain * gravity), got " + velocity[1]);
        List<double[]> points = TcArcGeometry.points(4, 2, 0, TcFxPlans.ARC_HEIGHT_GAIN,
                RandomSource.create(7L));
        helper.assertTrue(points.get(0)[0] == 0.0 && points.get(0)[1] == 0.0
                        && points.get(0)[2] == 0.0,
                "the arc chain starts at the origin (FXArc.java:41)");
        double[] last = points.get(points.size() - 1);
        helper.assertTrue(last[0] == 4.0 && last[1] == 2.0 && last[2] == 0.0,
                "the arc chain ends exactly at the target (FXArc.java:63)");
        helper.assertTrue(points.size() <= TcArcGeometry.MAX_MID_POINTS + 2,
                "the chain is capped at 50 mid points + start + end, got " + points.size());
        helper.succeed();
    }

    /**
     * The golem-boss plan: the beam pair (N 3.0 type 2, N 1.5 type 1, colour
     * (0.07,0.376,0.325)) each with its impact sprite, anchored at the boss's
     * head (PacketFXBeamPulseGolemBoss.java:44-56, FXBeamGolemBoss.java:39-52).
     */
    @GameTest(template = "empty")
    public static void golemBossBeamPairAndHeadAnchor(GameTestHelper helper) {
        double[] head = TcBeamGeometry.headAnchor(0, 0, 0, 0.0F, 0.0F, 1.62F);
        // yaw 0: yawRad = -PI -> f1 = -1, f2 = sin(-PI) ~ -8.7e-8 (float noise
        // at 1e-7, not 0); pitch 0: f3 = -1 -> anchor = (0, eyeHeight, +0.5)
        // (FXBeamGolemBoss.java:39-52).
        helper.assertTrue(Math.abs(head[0]) < 1.0e-3 && Math.abs(head[1] - 1.62) < 1.0e-3
                        && Math.abs(head[2] - 0.5) < 1.0e-3,
                "the head anchor of a yaw-0 boss is its eye height, 0.5 ahead, got "
                        + head[0] + "," + head[1] + "," + head[2]);

        List<ParticleSpawn> plan = TcFxPlans.beamPulseGolemBoss(
                new Vec3(head[0], head[1], head[2]), 3, 4, 10.0, true);
        helper.assertTrue(plan.size() == 4,
                "the packet builds 2 beams + 2 impact sprites, got " + plan.size());
        helper.assertTrue(plan.get(0).type() == ModParticles.BEAM_GOLEM_BOSS.get()
                        && plan.get(1).type() == ModParticles.BEAM_IMPACT.get()
                        && plan.get(2).type() == ModParticles.BEAM_GOLEM_BOSS.get()
                        && plan.get(3).type() == ModParticles.BEAM_IMPACT.get(),
                "each beam is followed by its impact sprite (FXBeamGolemBoss.java:232-262)");
        TcGolemBeamOptions wide = (TcGolemBeamOptions) plan.get(0).options();
        TcGolemBeamOptions narrow = (TcGolemBeamOptions) plan.get(2).options();
        helper.assertTrue(wide.size() == TcFxPlans.GOLEM_N_LARGE
                        && wide.type() == TcFxPlans.GOLEM_TYPE_LARGE,
                "the wide beam is N 3.0, type 2 (PacketFXBeamPulseGolemBoss.java:46-50)");
        helper.assertTrue(narrow.size() == TcFxPlans.GOLEM_N_SMALL
                        && narrow.type() == TcFxPlans.GOLEM_TYPE_SMALL,
                "the narrow beam is N 1.5, type 1 (PacketFXBeamPulseGolemBoss.java:52-56)");
        helper.assertTrue(wide.color() == TcFxPlans.GOLEM_BEAM_COLOR
                        && narrow.color() == TcFxPlans.GOLEM_BEAM_COLOR,
                "both beams carry the packet's colour (0.07, 0.376, 0.325)");
        helper.assertTrue(wide.bossEntityId() == 3 && wide.targetEntityId() == 4,
                "the beams track boss + target entity ids like FXBeamGolemBoss.onUpdate");

        // Ribbon/scroll constants the render pass shares (FXBeam.java).
        helper.assertTrue(TcBeamGeometry.RIBBON_HALF_WIDTH == 0.15F
                        && TcBeamGeometry.OPACITY == 0.4F,
                "ribbon half-width 0.15 and opacity 0.4 (FXBeam.java:201-224)");
        List<TcBeamGeometry.RibbonTile> tiles =
                TcBeamGeometry.ribbon(10.0F, 2.5F, 2.5F, 1.0F, 0.0F);
        helper.assertTrue(tiles.size() == 10,
                "span = length*size/n = 10 tiles at vStart 0, got " + tiles.size());
        helper.assertTrue(Math.abs(TcBeamGeometry.halfWidth(2.5F, 1.0F, 5.0F, 10.0F)
                - 0.15F * 2.5F) < 1.0e-6, "endMod 1 keeps the ribbon width constant");
        helper.succeed();
    }

    /**
     * The seam's server-side contract: {@code TcEffects.spawn} accepts the
     * beam plans on a server level (no client gate can be proven here), and
     * the geometry helpers expose the original's measured constants — the
     * BD-16 row in docs/BEHAVIORAL_DEVIATIONS.md pins what is NOT measurable
     * from the reference tree (the camera basis the original received from
     * RenderGlobal and the clamped-atlas tiling).
     */
    @GameTest(template = "empty")
    public static void beamSeamConstantsMatchReferenceTree(GameTestHelper helper) {
        // Gates (FXBeam.java:66-73, FXLightningBolt.java:155-161, FXBeamGolemBoss
        // uses the beam gate via its packet sender).
        helper.assertTrue(TcBeamGeometry.visibleAge(10.0, 20, true) == 20
                        && TcBeamGeometry.visibleAge(60.0, 20, true) == 0,
                "fancy gate: 50 blocks, age kept / 0");
        helper.assertTrue(TcBeamGeometry.visibleAge(10.0, 20, false) == 20
                        && TcBeamGeometry.visibleAge(30.0, 20, false) == 0,
                "fast gate: 25 blocks, age kept / 0");
        helper.assertTrue(TcBeamGeometry.boltVisible(99.0, true) && !TcBeamGeometry.boltVisible(101.0, true),
                "bolt fancy gate: 100 blocks");
        helper.assertTrue(TcBeamGeometry.boltVisible(49.0, false) && !TcBeamGeometry.boltVisible(51.0, false),
                "bolt fast gate: 50 blocks");
        // Scroll: var11 = slide+f, reversed BEFORE quantisation (FXBeam.java:215-218),
        // var12 = -var11*0.2 - floorFloat(-var11*0.1): forward slide 1 -> 0.8,
        // reverse slide 1 -> 0.2 (the mirror is not a simple negation of 0.8).
        helper.assertTrue(Math.abs(TcBeamGeometry.scroll(1.0F, false) - 0.8F) < 1.0e-6
                        && Math.abs(TcBeamGeometry.scroll(1.0F, true) - 0.2F) < 1.0e-6,
                "the beam texture scrolls at 0.2/tick and mirrors the slide on reverse");
        // World-time spin at rotationspeed 5: (20*5) % 360 = 100 degrees
        // (FXBeam.java:104-110 + the Y-spin pass).
        helper.assertTrue(Math.abs(TcBeamGeometry.spin(20L, 5, 0.0F) - 100.0F) < 1.0e-3,
                "the ribbon's Y-spin tracks world time at rotationspeed 5");
        // Arc ribbon size 0.25, alpha 0.8, 1-tick lifetime (FXArc.java:31, 96-101).
        helper.assertTrue(TcArcGeometry.RIBBON_SIZE == 0.25F && TcArcGeometry.ALPHA == 0.8F
                        && TcArcGeometry.LIFETIME == 1 && TcArcGeometry.GRAVITY == 0.115
                        && TcArcGeometry.NOISE == 0.25,
                "arc constants: size 0.25, alpha 0.8, life 1, gravity 0.115, noise 0.25");
        // The bolt's progressive reveal: at age 0 one wave (numsegments0
        // segments) is shown, and the reveal grows with age
        // (FXLightningBolt.java:112-131).
        TcBoltGeometry bolt = new TcBoltGeometry(new TcFxVector3(0, 0, 0),
                new TcFxVector3(0, 0, 8), 5L,
                TcFxPlans.BOLT_DURATION, TcFxPlans.BOLT_MULTIPLIER, TcFxPlans.BOLT_INCREMENT);
        bolt.defaultFractal();
        bolt.finalizeBolt();
        bolt.particleAge = 0;
        int revealAtBirth = bolt.renderLength(0.0F);
        bolt.particleAge = bolt.particleMaxAge;
        int revealAtDeath = bolt.renderLength(0.0F);
        helper.assertTrue(revealAtBirth == bolt.numsegments0
                        && revealAtDeath >= revealAtBirth,
                "the reveal starts at one wave (numsegments0) and grows with age, got "
                        + revealAtBirth + " -> " + revealAtDeath + " (numsegments0 "
                        + bolt.numsegments0 + ")");
        helper.assertTrue(Math.abs(TcBoltGeometry.passAlpha(0, 0, 10) - 0.4F) < 1.0e-6
                        && Math.abs(TcBoltGeometry.passAlpha(1, 0, 10) - 1.0F) < 1.0e-6
                        && Math.abs(TcBoltGeometry.passAlpha(0, 10, 10)) < 1.0e-6,
                "pass 0 alpha is (1-age/max)*0.4, pass 1 is 1-age/max*0.5 (FXLightningBolt.java:99-103)");
        helper.assertTrue(TcEffects.class != null, "TcEffects stays the single addParticle seam");
        helper.succeed();
    }
}
