package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxParticleCounts;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoltPlan;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoreSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaSourceMap;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSonicOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcVisSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime proof for the TC-M7-02-c-s slice on the game-test server: the wisp
 * family, the dig / vis-drain / essentia-source / infusion-source senders, and
 * the entity-space FX (shield runes, sonic pulse, zap bolt plan).
 *
 * <p>Same seam as {@link FxParticleGameTest}: the original's whole FX path sits
 * behind {@code world.field_72995_K} (client-only), so the spawn plan —
 * positions, motion, colour, lifetime, entity target, frame count — is the
 * part a dedicated server can prove, and it is transcribed from the decompiled
 * reference with the original random-draw order (a seeded
 * {@link RandomSource} reproduces a burst exactly, which these probes assert).
 * The pixels need a human look and are recorded in docs/feature-tracking.md.</p>
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FxSenderGameTest {

    private FxSenderGameTest() {
    }

    /** {@code ClientProxy.wispFX}/{@code wispFX2}/{@code wispFX3}/{@code wispFX4}. */
    @GameTest(template = "empty")
    public static void wispPlansMatchOriginalConstructors(GameTestHelper helper) {
        final RandomSource rand = RandomSource.create(20261028L);

        // wispFX (ClientProxy.java:700-703): gravity 0.02, colour given
        // straight, and FXWisp.java:32-34's `red == 0 -> 1` rule.
        final List<ParticleSpawn> plain = TcFxPlans.wispFx(1.5, 2.5, 3.5, 0.6F, 0.0F, 0.5F, 0.25F, rand);
        helper.assertTrue(plain.size() == 1, "wispFX spawns exactly one wisp");
        final TcWispOptions plainOptions = (TcWispOptions) plain.get(0).options();
        helper.assertTrue(plain.get(0).type() == ModParticles.WISP.get(), "plain wisp type");
        helper.assertTrue(plain.get(0).x() == 1.5 && plain.get(0).y() == 2.5 && plain.get(0).z() == 3.5,
                "wispFX spawns where the sender says");
        helper.assertTrue(TcFxPlans.red(plainOptions.color()) == 1.0F,
                "FXWisp's red == 0 rule must lift the red channel to 1, got " + plainOptions.color());
        helper.assertTrue(Math.abs(TcFxPlans.green(plainOptions.color()) - 0.5F) < 1.0F / 255.0F
                        && Math.abs(TcFxPlans.blue(plainOptions.color()) - 0.25F) < 1.0F / 255.0F,
                "the other channels pass through untouched (to the 8-bit packing)");
        helper.assertTrue(plainOptions.scale() == 0.6F, "scale passthrough");
        helper.assertTrue(plainOptions.gravity() == TcFxPlans.WISP_GRAVITY, "wispFX gravity 0.02");
        helper.assertTrue(plainOptions.lifetime() >= 36 && plainOptions.lifetime() <= 51,
                "lifetime is (int)(36 / (rand*0.3 + 0.7)) in 36..51, got " + plainOptions.lifetime());
        helper.assertTrue(plainOptions.targetEntityId() == TcWispOptions.NO_TARGET
                        && !plainOptions.additive() && !plainOptions.clip(),
                "a plain wisp has no target, no additive blend, and no noClip flag "
                        + "(the plain wispFX passes no field_70145_X, so it keeps block collision)");

        // wispFX2 (ClientProxy.java:705-710): type 5 is the additive smoke.
        final List<ParticleSpawn> typed = TcFxPlans.wispFx2(4.0, 5.0, 6.0, 0.9F, 5, true, false, 0.0, rand);
        final TcWispOptions typedOptions = (TcWispOptions) typed.get(0).options();
        helper.assertTrue(typedOptions.additive(), "type 5 flips the blend to additive (FXWisp.java:96)");
        helper.assertTrue(!typedOptions.clip(), "clip comes from the caller");
        helper.assertTrue(TcFxPlans.red(typedOptions.color()) <= 0.1F
                        && TcFxPlans.green(typedOptions.color()) <= 0.1F
                        && TcFxPlans.blue(typedOptions.color()) <= 0.1F,
                "type 5 rolls each channel at rand*0.1 (FXWisp.java:88-92)");
        helper.assertTrue(typedOptions.scale() == 0.9F && typedOptions.gravity() == 0.0,
                "size and gravity come from the caller");

        // wispFX3 (ClientProxy.java:719-724): mot = (target - spawn) / lifetime.
        final List<ParticleSpawn> travelling = TcFxPlans.wispFx3(0.0, 0.0, 0.0, 4.0, 3.0, 2.0,
                0.5F, 2, false, 0.03, rand);
        final TcWispOptions travel = (TcWispOptions) travelling.get(0).options();
        helper.assertTrue(travel.motX() == (4.0 - 0.0) / travel.lifetime()
                        && travel.motY() == (3.0 - 0.0) / travel.lifetime()
                        && travel.motZ() == (2.0 - 0.0) / travel.lifetime(),
                "the mote must cover the gap in exactly its lifetime (FXWisp.java:112-118)");
        helper.assertTrue(travelling.get(0).vx() == travel.motX()
                        && travelling.get(0).vy() == travel.motY()
                        && travelling.get(0).vz() == travel.motZ(),
                "the plan's motion is the wisp's motion");
        helper.assertTrue(TcFxPlans.red(travel.color()) == 0.2F && TcFxPlans.blue(travel.color()) >= 0.7F,
                "type 2 fixes red/green at 0.2 and rolls blue at 0.7 + rand*0.3");

        // wispFX4 (ClientProxy.java:726-731): entity-tracking, fixed scale 0.4.
        final List<ParticleSpawn> tracking = TcFxPlans.wispFx4(9.0, 9.0, 9.0, 17, 6, true, 0.0, rand);
        final TcWispOptions tracked = (TcWispOptions) tracking.get(0).options();
        helper.assertTrue(tracked.scale() == TcFxPlans.WISP_TARGET_SCALE, "wispFX4 scale is 0.4");
        helper.assertTrue(tracked.targetEntityId() == 17, "the wisp must ride entity 17");
        helper.assertTrue(!tracked.additive(), "only type 5 is additive");
        helper.succeed();
    }

    /** The {@code FXWisp(world, x, y, z, scale, type)} colour table (FXWisp.java:64-111). */
    @GameTest(template = "empty")
    public static void wispTypeColourTableMatches(GameTestHelper helper) {
        final int type0 = TcFxPlans.wispTypeColor(0, RandomSource.create(11L));
        helper.assertTrue(within(TcFxPlans.red(type0), 0.75, 1.0)
                        && within(TcFxPlans.green(type0), 0.25, 0.5)
                        && within(TcFxPlans.blue(type0), 0.75, 1.0),
                "type 0 (vis wisp): r 0.75+, g 0.25..0.5, b 0.75+");
        final int type1 = TcFxPlans.wispTypeColor(1, RandomSource.create(11L));
        helper.assertTrue(within(TcFxPlans.red(type1), 0.5, 0.8)
                        && within(TcFxPlans.green(type1), 0.5, 0.8)
                        && TcFxPlans.blue(type1) == 0.2F,
                "type 1 is amber with a fixed blue");
        final int type3 = TcFxPlans.wispTypeColor(3, RandomSource.create(11L));
        helper.assertTrue(TcFxPlans.red(type3) == 0.2F && TcFxPlans.blue(type3) == 0.2F
                        && within(TcFxPlans.green(type3), 0.7, 1.0),
                "type 3 is green with fixed red/blue");
        final int type7 = TcFxPlans.wispTypeColor(7, RandomSource.create(11L));
        helper.assertTrue(within(TcFxPlans.red(type7), 0.7, 1.0)
                        && within(TcFxPlans.green(type7), 0.5, 0.7)
                        && within(TcFxPlans.blue(type7), 0.3, 0.4),
                "type 7 is the earth-brown roll");
        final int unknown = TcFxPlans.wispTypeColor(9, RandomSource.create(11L));
        helper.assertTrue(TcFxPlans.red(unknown) == 1.0F && TcFxPlans.green(unknown) == 0.0F
                        && TcFxPlans.blue(unknown) == 0.0F,
                "an unknown type keeps the constructor's (1, 0, 0)");
        helper.assertTrue(TcFxPlans.wispTypeColor(4, RandomSource.create(4242L))
                        == TcFxPlans.wispTypeColor(4, RandomSource.create(4242L)),
                "the same seed must roll the same colour — the draw order is the fidelity claim");
        helper.assertTrue(TcFxPlans.wispTypeAdditive(5) && !TcFxPlans.wispTypeAdditive(0),
                "blendmode 771 belongs to type 5 only");
        helper.succeed();
    }

    /** {@code ClientProxy.wispFXEG} (ClientProxy.java:712-716, FXWispEG.java:15-38). */
    @GameTest(template = "empty")
    public static void wispEgBurstMatchesOriginal(GameTestHelper helper) {
        final RandomSource rand = RandomSource.create(99L);
        helper.assertTrue(TcFxPlans.wispFxEg(0.5, 64.5, 0.5, 5, FxParticleCounts.SETTING_ALL, rand).size() == 2,
                "particleCount(1) doubles at the ALL setting");
        helper.assertTrue(TcFxPlans.wispFxEg(0.5, 64.5, 0.5, 5, FxParticleCounts.SETTING_DECREASED, rand).size() == 1,
                "particleCount(1) is 1 at DECREASED");
        helper.assertTrue(TcFxPlans.wispFxEg(0.5, 64.5, 0.5, 5, FxParticleCounts.SETTING_MINIMAL, rand).isEmpty(),
                "particleCount(1) is 0 at MINIMAL");

        final List<ParticleSpawn> burst =
                TcFxPlans.wispFxEg(0.5, 64.5, 0.5, 5, FxParticleCounts.SETTING_DECREASED, rand);
        final TcWispEgOptions options = (TcWispEgOptions) burst.get(0).options();
        helper.assertTrue(burst.get(0).type() == ModParticles.WISP_EG.get(), "the eg wisp type");
        helper.assertTrue(options.targetEntityId() == 5, "FXWispEG rides the target");
        helper.assertTrue(options.lifetime() >= 40 && options.lifetime() <= 57,
                "FXWispEG lifetime is (int)(40 / (rand*0.3 + 0.7)), got " + options.lifetime());
        helper.assertTrue(TcFxPlans.red(options.color()) <= 0.05F
                        && TcFxPlans.green(options.color()) <= 0.05F
                        && TcFxPlans.blue(options.color()) <= 0.05F,
                "the colour rolls rand*0.05 per channel (FXWispEG.java:36-38)");
        helper.assertTrue(burst.get(0).vy() == TcFxPlans.WISP_EG_FALL, "the wisp falls at -0.05");
        helper.succeed();
    }

    /** {@code ClientProxy.sourceStreamFX} → {@code FXWispArcing} (ClientProxy.java:777-781). */
    @GameTest(template = "empty")
    public static void arcingStreamFliesFromTargetToSource(GameTestHelper helper) {
        final List<ParticleSpawn> plan =
                TcFxPlans.sourceStream(10.0, 20.0, 30.0, 1.0, 2.0, 3.0, 0x00FF00, RandomSource.create(7L));
        helper.assertTrue(plan.size() == 1, "one stream mote per packet");
        final ParticleSpawn spawn = plan.get(0);
        final TcWispArcingOptions options = (TcWispArcingOptions) spawn.options();
        helper.assertTrue(spawn.type() == ModParticles.WISP_ARCING.get(), "the arcing wisp type");
        helper.assertTrue(spawn.x() == 1.0 && spawn.y() == 2.0 && spawn.z() == 3.0,
                "FXWispArcing spawns at the target argument and flies to the start argument");
        helper.assertTrue(options.deltaX() == 9.0 && options.deltaY() == 18.0 && options.deltaZ() == 27.0,
                "motion is start - target (FXWispArcing.java:44-46)");
        helper.assertTrue(options.color() == 0x00FF00, "the colour comes from the tag colour");
        helper.assertTrue(options.scale() == 0.1F, "the caller's 0.1 scale");
        helper.assertTrue(options.lifetime() >= 36 && options.lifetime() <= 51, "the ctor lifetime roll");
        helper.succeed();
    }

    /** {@code PacketFXVisDrain.onMessage} (PacketFXVisDrain.java:59-73). */
    @GameTest(template = "empty")
    public static void visDrainMoteHomesAtConsumer(GameTestHelper helper) {
        final List<ParticleSpawn> plan =
                TcFxPlans.visDrain(20.0, 64.0, 30.0, 10.0, 60.0, 15.0, 0x33CC99, RandomSource.create(3L));
        helper.assertTrue(plan.size() == 1, "one vis mote per packet");
        final ParticleSpawn spawn = plan.get(0);
        final TcVisSparkleOptions options = (TcVisSparkleOptions) spawn.options();
        helper.assertTrue(spawn.type() == ModParticles.VIS_SPARKLE.get(), "the vis mote type");
        helper.assertTrue(within(spawn.x(), 20.4, 20.6) && within(spawn.y(), 64.4, 64.6)
                        && within(spawn.z(), 30.4, 30.6),
                "the mote spawns at the source corner + 0.4..0.6 (line 63-65)");
        helper.assertTrue(within(options.targetX(), 10.0, 11.0) && within(options.targetY(), 60.0, 61.0)
                        && within(options.targetZ(), 15.0, 16.0),
                "it homes on the consumer block + a unit roll (line 66-68)");
        helper.assertTrue(options.color() == 0x33CC99, "the aspect colour passes through");
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
                    "the block branch of PacketFXBlockDig spawns the vanilla block crumb");
            helper.assertTrue(spawn.options() instanceof BlockParticleOption option
                            && option.getState() == state,
                    "the crumb must carry the packet's block state");
            helper.assertTrue(within(spawn.x(), 11.0, 12.0) && within(spawn.y(), 10.0, 11.0)
                            && within(spawn.z(), 10.0, 11.0),
                    "crumbs spawn inside the dig-face block (dx + rand)");
        }

        final ItemStack stack = new ItemStack(Items.DIRT);
        final List<ParticleSpawn> items = TcFxPlans.blockDig(target, digFrom, null, stack,
                RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED);
        helper.assertTrue(items.get(0).type() == ParticleTypes.ITEM
                        && items.get(0).options() instanceof ItemParticleOption option
                        && option.getItem().is(Items.DIRT),
                "the item branch (PacketFXBlockDig.java:99-128) spawns the vanilla item crumb");
        helper.assertTrue(signature(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                        RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED))
                        .equals(signature(TcFxPlans.blockDig(target, digFrom, state, ItemStack.EMPTY,
                                RandomSource.create(5L), FxParticleCounts.SETTING_DECREASED))),
                "the same seed must reproduce the burst exactly (the draw order is the fidelity claim)");
        helper.succeed();
    }

    /**
     * The {@code EssentiaHandler.sourceFX} tick loop
     * (ClientTickEventsFML.java:218-261 → ClientProxy.essentiaTrailFx).
     */
    @GameTest(template = "empty")
    public static void essentiaTrailTickMatchesClientTickLoop(GameTestHelper helper) {
        final List<ParticleSpawn> steady =
                TcFxPlans.essentiaTrailTick(20, 64, 30, 10, 60, 15, 0xFF0000, 15, 100, false,
                        RandomSource.create(1L));
        helper.assertTrue(steady.size() == 1, "one trail mote per entry per tick");
        final ParticleSpawn spawn = steady.get(0);
        final TcEssentiaTrailOptions options = (TcEssentiaTrailOptions) spawn.options();
        helper.assertTrue(spawn.type() == ModParticles.ESSENTIA_TRAIL.get(), "the trail type");
        helper.assertTrue(spawn.x() == 20.5 && spawn.y() == 64.5 && spawn.z() == 30.5,
                "the trail starts at the source block centre");
        helper.assertTrue(options.targetX() == 10.5 && options.targetY() == 60.5 && options.targetZ() == 15.5,
                "and flies to the consumer block centre");
        helper.assertTrue(options.count() == 100, "the burst count is the client tick count");
        helper.assertTrue(Math.abs(options.scale() - wobble(100)) < 1.0E-4,
                "outside the tail window the scale is the client tick count's sine wobble around 1");
        helper.assertTrue(options.lifetime() >= 1, "lifetime (int)(dist*30) based, min 1");
        helper.assertTrue(TcFxPlans.red(options.color()) <= 1.0F
                        && TcFxPlans.red(options.color()) >= 0.8F
                        && TcFxPlans.green(options.color()) == 0.0F,
                "the colour jitters each channel down by at most 0.2 of itself (FXEssentiaTrail.java:63-68)");

        final TcEssentiaTrailOptions tail = (TcEssentiaTrailOptions) TcFxPlans
                .essentiaTrailTick(20, 64, 30, 10, 60, 15, 0xFF0000, 3, 100, false, RandomSource.create(1L))
                .get(0).options();
        helper.assertTrue(tail.count() == 98, "the last 5 ticks subtract (5 - ticks) from the count");
        helper.assertTrue(Math.abs(tail.scale() - wobble(98) * 9.0F / 25.0F) < 1.0E-4,
                "the tail shrinks the scale by ticks²/25 on its own (reduced) count (ClientTickEventsFML.java:244-246)");

        final TcEssentiaTrailOptions matrix = (TcEssentiaTrailOptions) TcFxPlans
                .essentiaTrailTick(20, 64, 30, 10, 60, 15, 0xFF0000, 15, 100, true, RandomSource.create(1L))
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

        final List<ParticleSpawn> itemCrumbs = TcFxPlans.infusionPedestalTick(12, 64, 18, 20, 66, 20,
                null, pedestalStack, FxParticleCounts.SETTING_ALL, RandomSource.create(0L));
        for (final ParticleSpawn spawn : itemCrumbs) {
            if (spawn.type() == ParticleTypes.ITEM) {
                helper.assertTrue(spawn.options() instanceof ItemParticleOption option
                                && option.getItem().is(Items.IRON_BLOCK),
                        "an item-only pedestal stack draws item crumbs (drawInfusionParticles1)");
                helper.assertTrue(within(spawn.y(), 65.23, 65.43), "the item crumbs sit at sy + 1.23..1.43");
            }
        }

        final List<ParticleSpawn> playerDraws = TcFxPlans.infusionPlayerTick(20, 66, 20, 21.5, 64.0, 21.5,
                0.6, 1.8, FxParticleCounts.SETTING_DECREASED, RandomSource.create(3L));
        helper.assertTrue(playerDraws.size() == 2, "particleCount(2) motes for the player branch");
        for (final ParticleSpawn spawn : playerDraws) {
            final TcBoreSparkleOptions options = (TcBoreSparkleOptions) spawn.options();
            helper.assertTrue(spawn.type() == ModParticles.BORE_SPARKLE.get(), "the player branch draws bore motes");
            helper.assertTrue(options.scale() == 0.3F, "setParticleScale(0.3F) fixes the quad scale");
            helper.assertTrue(within(spawn.x(), 20.9, 22.1) && within(spawn.y(), 64.0, 65.8)
                            && within(spawn.z(), 20.9, 22.1),
                    "the motes spawn on the player's AABB (TileInfusionMatrix.java:873-878)");
            helper.assertTrue(options.targetX() == 20.5 && options.targetY() == 65.5 && options.targetZ() == 20.5,
                    "and fly to the matrix head");
        }
        helper.succeed();
    }

    /** The entity-space FX: runes and pulses ride their entity owner. */
    @GameTest(template = "empty")
    public static void entitySpaceFxRideTheirEntities(GameTestHelper helper) {
        final List<ParticleSpawn> runes = TcFxPlans.shieldRune(3, 12.5F, -30.0F, false);
        helper.assertTrue(runes.size() == 1, "PacketFXShield adds exactly one rune per call");
        final ParticleSpawn rune = runes.get(0);
        final TcShieldOptions runeOptions = (TcShieldOptions) rune.options();
        helper.assertTrue(rune.type() == ModParticles.SHIELD_RUNES.get(), "the player rune type");
        helper.assertTrue(runeOptions.targetEntityId() == 3, "the rune rides entity 3");
        helper.assertTrue(runeOptions.yaw() == 12.5F && runeOptions.pitch() == -30.0F,
                "the aim is the packet's yaw/pitch (PacketFXShield.java:63-72)");
        helper.assertTrue(runeOptions.lifetime() == 8, "the ctor lifetime is the literal 8");
        helper.assertTrue(!runeOptions.mob(), "a player shield uses the hemis frames");

        final TcShieldOptions mobRune = (TcShieldOptions) TcFxPlans.shieldRune(3, 0.0F, 90.0F, true).get(0).options();
        helper.assertTrue(mobRune.mob(), "the mob branch flags itself");
        helper.assertTrue(TcFxPlans.shieldRune(3, 0.0F, 90.0F, true).get(0).type()
                        == ModParticles.SHIELD_RUNES_MOB.get(),
                "the mob branch uses its own type so it can bind the ripple frames");

        final List<ParticleSpawn> pulses = TcFxPlans.sonicPulse(4);
        helper.assertTrue(pulses.size() == 1, "PacketFXSonic adds one pulse");
        final TcSonicOptions pulse = (TcSonicOptions) pulses.get(0).options();
        helper.assertTrue(pulses.get(0).type() == ModParticles.SONIC.get(), "the sonic pulse type");
        helper.assertTrue(pulse.targetEntityId() == 4, "the pulse rides entity 4");
        helper.assertTrue(pulse.lifetime() == 10, "the ctor lifetime is the literal 10");
        helper.succeed();
    }

    /** {@code PacketFXZap} / {@code PacketFXWispZap} → {@code proxy.bolt} geometry. */
    @GameTest(template = "empty")
    public static void zapBoltPlanCarriesReferenceGeometry(GameTestHelper helper) {
        final TcBoltPlan plan = TcFxPlans.boltPlan(1.5, 64.5, 2.5, 9.5, 70.5, 8.5, -123456789L);
        helper.assertTrue(plan.fromX() == 1.5 && plan.fromY() == 64.5 && plan.fromZ() == 2.5
                        && plan.toX() == 9.5 && plan.toY() == 70.5 && plan.toZ() == 8.5,
                "the endpoints are the two entities' AABB centres (PacketFXZap.java:46-52)");
        helper.assertTrue(plan.seed() == -123456789L, "the seed is the world random's nextLong()");
        helper.assertTrue(plan.segments() == 6 && plan.diameter() == 0.5F && plan.lifetime() == 8,
                "the ctor args are (world.rand.nextLong(), 6, 0.5F, 8)");
        helper.assertTrue(plan.type() == 2 && plan.width() == 0.125F,
                "setType(2) and setWidth(0.125F) before finalizeBolt()");
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
            motes += TcEssentiaSourceMap.drain(pos -> false, 100 + tickNumber, RandomSource.create(2L)).size();
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

    /** {@code PacketFXEssentiaSource} / {@code PacketFXInfusionSource} reach the map through the hook. */
    @GameTest(template = "empty")
    public static void particleSettingGateCoversTheNewSenders(GameTestHelper helper) {
        helper.assertTrue(FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_ALL) == 40
                        && FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_DECREASED) == 20
                        && FxParticleCounts.particleCount(20, FxParticleCounts.SETTING_MINIMAL) == 0,
                "the dig burst runs the original's particleCount(20) gate");
        helper.assertTrue(FxParticleCounts.particleCount(2, FxParticleCounts.SETTING_ALL) == 4
                        && FxParticleCounts.particleCount(2, FxParticleCounts.SETTING_MINIMAL) == 0,
                "the infusion draws run particleCount(2)");
        helper.assertTrue(FxParticleCounts.particleCount(1, FxParticleCounts.SETTING_DECREASED) == 1,
                "the eg wisp runs particleCount(1)");
        helper.succeed();
    }

    private static boolean within(final double value, final double min, final double max) {
        return value >= min && value <= max;
    }

    /** The trail's sine wobble around 1 for a given client tick count (FXEssentiaTrail.java:71-73). */
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
