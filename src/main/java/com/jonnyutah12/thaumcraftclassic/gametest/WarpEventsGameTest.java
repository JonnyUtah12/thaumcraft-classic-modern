package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.player.WarpEvents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogMindSpider;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The warp-events mob spawn seam, in the world (t_1a83f0db, closing BD-75's
 * warp half). The original spawns the mind spider from the warp events
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/lib/WarpEvents.java:281-315})
 * — these probes fire the event entry with the warp state set on a player and
 * assert the resulting entities, not a log line.
 *
 * <p>The plain state, asserted below: the mind spider DOES spawn in a shipped
 * world today, through the warp-event seam (this class), and through nothing
 * else — no shipped biome carries a spawn entry naming it (counted by
 * {@code MindSpiderMobGameTest.thePassageSpawnerSeamResolvesAndNoShippedBio
 * meSpawnsTheMindSpider}, which these probes keep at zero), and the Outer
 * Lands passage routing that places the spawner block is
 * {@code t_cc6182ca}'s lane.</p>
 *
 * <p>Every probe scripts the level random ({@link #Scripted}): the gate roll
 * ({@code :45}), the event roll ({@code :50}) and the 6 draws per scan
 * attempt ({@code :292-297}) are consumed in the original's exact order.
 * Spider slots are scripted 2 blocks apart (offsets 7, 9, 11, … on two axes)
 * so the swarm's boxes cannot overlap each other — the original's scan
 * rejects an occupied box ({@code :300-302}). Per spider the script carries
 * THREE attempts: its slot at y=1, then y=8 and y=15 above the same column
 * (the scan offset is 7..24, so those are the near landings), and slots stay
 * 2 apart so swarm boxes never touch — a pad that
 * shares cells with a sibling probe's entities (the batch packs by the
 * biggest template, the standing gate hazard {@code t_44055375}) costs a
 * spider its scripted FIRST attempt and never a random draw. The probes
 * assert the arithmetic: every spider lands on one of its scripted cells and
 * the unconsumed queue equals the scripted draws minus 6 per spent attempt —
 * a port that draws more, fewer, or in a different sequence than the
 * original fails.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WarpEventsGameTest {

    /** The probe pad: 24×24 air, player scripted at (1, 1, 1) inside it. */
    private static final String TEMPLATE = "empty_24";
    // eventWarp(perm 90 + sticky 30, counter 30) = (120+120+30)/3 = 90 (:47)
    private static final int COUNTER = 30;
    private static final int DECAYED = 19;   // (int)(30 - max(5, sqrt(30)*2)) = 19 — WarpEvents.java:48

    /**
     * eff 57 ∈ the illusion band (57..60, {@code :158}):
     * {@code suddenlySpiders(warp, false)} spawns min(50, warp) = 50 spiders
     * ({@code :283}), each targeted at the player ({@code :314-315}) and
     * marked {@code harmless = true} with the player as viewer
     * ({@code :316-319}). Asserts the 50 new entities (delta over the ambient
     * census), their harmless/viewer/target state, the counter decay
     * ({@code :48-49}) and the tail temp decay ({@code :224}).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theIllusionBandSpawnsTheWarpCappedHarmlessViewerBoundSwarm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 5, 30, COUNTER);

        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = scriptedSwarm(57);
        WarpEvents.checkWarpEvent(player, random);

        final List<EntityMindSpider> spawned = spawnedSince(baseline, padSpiders(helper, level));
        helper.assertTrue(spawned.size() == 50,
                "the illusion band (eff 57, WarpEvents.java:158) produced " + spawned.size()
                        + " new mind spiders, not the original's min(50, warp=90) = 50 (:283)");
        for (final EntityMindSpider spider : spawned) {
            helper.assertTrue(spider.isHarmless(),
                    "an illusion spider (real == false, :316-318) is not harmless — it would bite the player");
            helper.assertTrue(player.getScoreboardName().equals(spider.getViewer()),
                    "an illusion spider's viewer is \"" + spider.getViewer() + "\", not the warping player ("
                            + ":317 setViewer(player.getCommandSenderName()))");
            helper.assertTrue(spider.getTarget() == player,
                    "a warp spider does not target the warping player (:314 setAttackTarget)");
        }
        final PlayerWarpData after = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(after.warpCounter() == DECAYED,
                "the counter after one firing pass is " + after.warpCounter() + ", not the decayed "
                        + DECAYED + " (:48-49)");
        helper.assertTrue(after.warpTemp() == 4,
                "the tail (WarpEvents.java:224) did not decay temp warp 5 -> 4");
        assertQueueArithmetic(helper, random, spawned);
        helper.succeed();
    }

    /**
     * The draw-order contract, robust to batch contention: every spider lands
     * on one of its three scripted cells ({@link #landingAttempt}) and the
     * unconsumed queue equals the 900 scan draws minus 6 per spent attempt.
     * A port that draws per attempt in the original's shape passes whatever
     * cells the batch happens to share; any other draw shape fails.
     */
    private static void assertQueueArithmetic(final GameTestHelper helper, final Scripted random,
                                              final List<EntityMindSpider> spawned) {
        final BlockPos o = helper.absolutePos(BlockPos.ZERO);
        int spent = 0;
        for (final EntityMindSpider spider : spawned) {
            final int idx = landingAttempt(helper, spider);
            helper.assertTrue(idx >= 0,
                    "a warp spider landed off-script at rel (" + ((int) Math.floor(spider.getX()) - o.getX())
                            + ", " + ((int) Math.floor(spider.getY()) - o.getY()) + ", "
                            + ((int) Math.floor(spider.getZ()) - o.getZ()) + ") — the scan drew cells the "
                            + "scripted order never offers (x=8+2(a%8), z=8+2(a/8), y=1,8,15)");
            spent += idx + 1;
        }
        helper.assertTrue(random.unconsumed() == 900 - 6 * spent,
                "the event consumed " + (2 + 6 * spent) + " scripted draws by landing cells (" + spent
                        + " attempts x 6 scan draws) but the queue clock says " + random.consumed
                        + " (left " + random.unconsumed() + " of the 902 scripted) — the "
                        + "original spends 6 draws per scan attempt (:292-297) in this exact order "
                        + "(:45, :50)");
    }

    /**
     * eff 89 ∈ the real band (89..92, {@code :195}): the same
     * {@code suddenlySpiders} call with {@code real == true} — the spiders are
     * NOT harmless and carry no viewer ({@code :316} skips the marking).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theRealBandSpawnsTheWarpCappedHostileSwarm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 0, 30, COUNTER);

        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = scriptedSwarm(89);
        WarpEvents.checkWarpEvent(player, random);

        final List<EntityMindSpider> spawned = spawnedSince(baseline, padSpiders(helper, level));
        helper.assertTrue(spawned.size() == 50,
                "the real band (eff 89, WarpEvents.java:195) produced " + spawned.size()
                        + " new mind spiders, not the original's min(50, warp=90) = 50 (:283)");
        for (final EntityMindSpider spider : spawned) {
            helper.assertTrue(!spider.isHarmless(),
                    "a real warp spider (real == true, :316) must not be harmless");
            helper.assertTrue(spider.getViewer().isEmpty(),
                    "a real warp spider carries viewer \"" + spider.getViewer() + "\" — only illusions "
                            + "are marked (:317)");
            helper.assertTrue(spider.getTarget() == player,
                    "a warp spider does not target the warping player (:314 setAttackTarget)");
        }
        assertQueueArithmetic(helper, random, spawned);
        helper.succeed();
    }

    /**
     * A warp total without a counter never fires (gate {@code :46},
     * {@code warpCounter > 0}): no spider, and the event roll ({@code :50})
     * is never drawn (the script carries exactly the gate roll, and a second
     * draw would leave it unconsumed). The tail ({@code :224-225}) still runs
     * on a non-firing pass.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theGateRefusesWithoutACounterWhileTheTempDecayTailStillRuns(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 3, 30, 0);

        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = new Scripted().offer(0); // gate roll only (:45)
        WarpEvents.checkWarpEvent(player, random);

        helper.assertTrue(spawnedSince(baseline, padSpiders(helper, level)).isEmpty(),
                "a player with warp and NO counter spawned spiders — the gate (:46) must refuse");
        final PlayerWarpData after = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(after.warpCounter() == 0,
                "the refused pass changed the counter to " + after.warpCounter() + " (:49 only runs inside the gate)");
        helper.assertTrue(after.warpTemp() == 2,
                "the tail (WarpEvents.java:224) must decay temp 3 -> 2 even on a refused pass");
        helper.assertTrue(random.isExhausted(),
                "the refused pass left the scripted queue unconsumed — it never drew the gate roll (:45)");
        helper.succeed();
    }

    /**
     * eff 30 ∈ the mist band (29..32, {@code :106}): {@code spawnMist(warp, 1)}
     * ({@code :229-242}) sends the mist FX, summons min(8, 1) = 1 eldritch
     * guardian and chats warp.text.6. The guardian is not registered, so the
     * summon runs as the {@link EldritchMobHost} documented no-op (the same
     * seam {@code TileEldritchCrabSpawner} uses): the probe asserts the seam
     * ran without producing an entity, the pass decayed the counter, and NO
     * spider spawned (the mist arm is not the spider arm).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theMistBandFiresTheFxAndTheGuardianSeamAsDocumentedNoOps(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 0, 30, COUNTER);

        helper.assertTrue(!EldritchMobHost.isAvailable(EldritchMobHost.GUARDIAN),
                "this probe narrates the guardian as unregistered; if it became available, re-measure the summon");
        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = scriptedGate(30);
        WarpEvents.checkWarpEvent(player, random);

        helper.assertTrue(spawnedSince(baseline, padSpiders(helper, level)).isEmpty(),
                "the mist arm (:106) spawned spiders — only the spider arms (:158/:195) call suddenlySpiders");
        helper.assertTrue(countGuardians(helper, level) == 0,
                "a thaumcraft:eldritchguardian entity exists — the unregistered guardian must summon as the "
                        + "EldritchMobHost no-op, and its registered card must own the :264-280 scan");
        final PlayerWarpData after = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(after.warpCounter() == DECAYED,
                "the mist pass did not decay the counter to " + DECAYED + " (:48-49)");
        helper.assertTrue(random.isExhausted(),
                "the mist pass left " + random.unconsumed() + " scripted draws unused — the guardian scan "
                        + "draws (:264-280) belong to the registered guardian's own card");
        helper.succeed();
    }

    /**
     * eff 76 (the equality arm, {@code :169-176}): one sticky warp is shed
     * ({@code :172}) with the sticky sync + warp-message payloads
     * ({@code :173-174}), the counter decays, no spider spawns.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theStickyReliefArmShedsOneStickyWarp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 0, 30, COUNTER);

        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = scriptedGate(76);
        WarpEvents.checkWarpEvent(player, random);

        helper.assertTrue(spawnedSince(baseline, padSpiders(helper, level)).isEmpty(),
                "the relief arm (:169-176) spawned spiders");
        final PlayerWarpData after = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(after.warpSticky() == 29,
                "the relief pass left sticky warp at " + after.warpSticky() + ", not 30 - 1 (:172)");
        helper.assertTrue(after.warpCounter() == DECAYED,
                "the relief pass did not decay the counter to " + DECAYED + " (:48-49)");
        helper.assertTrue(random.isExhausted(),
                "the relief pass left " + random.unconsumed() + " scripted draws unused beyond roll+eff");
        helper.succeed();
    }

    /**
     * The scan respects the original's occupancy rules: an entity parked in a
     * scripted slot is rejected by the entity-free checks ({@code :301-302}),
     * a solid block in the cell BELOW a candidate kills it at {@code :298}
     * ({@code World.func_147466_a}), and the spiders whose scripted FIRST
     * cells are blocked (slots 0, 1, 2 — witnesses at (8,1,8)/(10,1,8), stone
     * under (12,1,8)) spend their scripted attempts ABOVE and never land in a
     * blocked cell; all 50 land. Batch contention can block other slots the
     * same way, and the arithmetic assert absorbs that (each extra attempt
     * costs exactly 6 scripted draws).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void theScanRejectsOccupiedAndFlooredCellsAndSpendsItsSecondAttempt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = warpPlayer(helper, 90, 0, 30, COUNTER);
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);

        for (final BlockPos cell : new BlockPos[] {origin.offset(8, 1, 8), origin.offset(10, 1, 8)}) {
            final EntityMindSpider witness = new EntityMindSpider(CatalogMindSpider.MIND_SPIDER.get(), level);
            // The witness sits at the scripted landing spot itself, not the
            // cell centre: the spider lands at the integer coordinates, and a
            // narrow box at the centre (+0.5) would not intersect it.
            witness.moveTo(cell.getX(), cell.getY(), cell.getZ(), 0.0F, 0.0F);
            witness.setNoGravity(true);
            level.addFreshEntity(witness);
        }
        helper.setBlock(new BlockPos(12, 0, 8), Blocks.STONE);

        final List<EntityMindSpider> baseline = padSpiders(helper, level);
        final Scripted random = scriptedSwarm(57);
        WarpEvents.checkWarpEvent(player, random);

        final List<EntityMindSpider> spawned = spawnedSince(baseline, padSpiders(helper, level));
        helper.assertTrue(spawned.size() == 50,
                "the blocked scan produced " + spawned.size() + " spiders, not 50 — the scan spends its "
                        + "50 attempts per spider (:291), a blocked scripted slot costs an attempt, not a spider");
        for (final EntityMindSpider spider : spawned) {
            final int idx = landingAttempt(helper, spider);
            final int slot = landingSlot(helper, spider);
            helper.assertTrue(idx >= 0,
                    "a spider landed off-script in the blocked-scan probe");
            helper.assertTrue(!(slot <= 2 && idx == 0),
                    "a spider landed in a blocked scripted slot (slot " + slot + ", attempt 0) — the "
                            + ":298 floor check (stone under (12,1,8)) or the :301-302 occupancy check "
                            + "(witnesses at (8,1,8) and (10,1,8)) is not rejecting");
        }
        assertQueueArithmetic(helper, random, spawned);
        helper.succeed();
    }

    // ------------------------------------------------------------------ plumbing

    /** The gate roll (0, always inside sqrt(30) = 5.477, {@code :45-46}) + the scripted event roll. */
    private static Scripted scriptedGate(final int eff) {
        return new Scripted().offer(0).offer(eff);
    }

    /**
     * The 50-spider scan script, contention-hardened: per spider a, THREE
     * scripted attempts — A1 at (8+2·(a%8), 1, 8+2·(a/8)), A2 one block
     * above, A3 two above (x and z identical; slots 2 apart so swarm boxes
     * never touch). 6 draws per attempt ({@code :292-297}), gate + event roll
     * first ({@code :45, :50}): 902 draws. The batch packs pads by the
     * biggest template (hazard {@code t_44055375}), so a spider whose cell is
     * occupied spends the scripted attempt for it, never a random draw; the
     * probes assert the landing cells and the queue arithmetic.
     */
    private static Scripted scriptedSwarm(final int eff) {
        final Scripted random = scriptedGate(eff);
        for (int a = 0; a < 50; a++) {
            final int x = 2 * (a % 8);
            final int z = 2 * (a / 8);
            random.offer(x, 2, 0, 1, z, 2);          // A1: y = 1 + 7·0
            random.offer(x, 2, 0, 2, z, 2);          // A2: y = 1 + 7·1 = 8
            random.offer(x, 2, 7, 2, z, 2);          // A3: y = 1 + 14·1 = 15
        }
        return random;
    }

    /**
     * The scripted attempt index a spider landed on: 0 (A1, y=1), 1 (A2,
     * y=8 = 1+7), 2 (A3, y=15 = 1+14), or -1 for any cell the script never
     * offered (the scan offset is 7..24, so y=2 is NOT reachable).
     */
    private static int landingAttempt(final GameTestHelper helper, final EntityMindSpider spider) {
        final BlockPos o = helper.absolutePos(BlockPos.ZERO);
        final int rx = (int) Math.floor(spider.getX()) - o.getX();
        final int ry = (int) Math.floor(spider.getY()) - o.getY();
        final int rz = (int) Math.floor(spider.getZ()) - o.getZ();
        if (rx < 8 || rx > 22 || ((rx - 8) & 1) != 0) {
            return -1;
        }
        if (rz < 8 || rz > 20 || ((rz - 8) & 1) != 0) {
            return -1;
        }
        final int a = (rx - 8) / 2 + 8 * ((rz - 8) / 2);
        final int idx = ry == 1 ? 0 : ry == 8 ? 1 : ry == 15 ? 2 : -1;
        if (a > 49 || idx < 0) {
            return -1;
        }
        // the (x, z) grid point must belong to spider a: x = 8+2(a%8), z = 8+2(a/8)
        if (rx != 8 + 2 * (a % 8) || rz != 8 + 2 * (a / 8)) {
            return -1;
        }
        return idx;
    }

    /** The spider's scripted slot id a (0..49), valid only when {@link #landingAttempt} >= 0. */
    private static int landingSlot(final GameTestHelper helper, final EntityMindSpider spider) {
        final BlockPos o = helper.absolutePos(BlockPos.ZERO);
        return ((int) Math.floor(spider.getX()) - o.getX() - 8) / 2
                + 8 * (((int) Math.floor(spider.getZ()) - o.getZ() - 8) / 2);
    }

    /**
     * A warp-carrying real {@link ServerPlayer} at pad cell (1, 1, 1) — inside
     * the 24³ air template, so every scripted slot (x 8..22, z 8..19) is
     * template air. The harness mock player is not a ServerPlayer (its own
     * comment in {@code OuterLandsGameTest}), and the warp seam sends payloads
     * to the player, so this builds a player with a connection carrying our
     * payload channels — the {@code ThaumonomiconNoteRequestGameTest.freshPlayer}
     * idiom.
     */
    private static ServerPlayer warpPlayer(final GameTestHelper helper, final int perm, final int temp,
                                           final int sticky, final int counter) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-warpevent-" + UUID.randomUUID().toString().substring(0, 8)), false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(SyncAttachmentsPayload.TYPE.id());   // setData(PLAYER_WARP) syncs attachments
        adHoc.add(MiscEventPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);

        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 1.5, origin.getY() + 1.0, origin.getZ() + 1.5);
        player.setHealth(20.0F);
        player.setNoGravity(true);
        player.setData(ModAttachments.PLAYER_WARP, new PlayerWarpData(perm, temp, sticky, counter, 0));
        return player;
    }

    private static List<EntityMindSpider> padSpiders(final GameTestHelper helper, final ServerLevel level) {
        return padEntities(helper, level, e -> e instanceof EntityMindSpider).stream()
                .map(e -> (EntityMindSpider) e)
                .toList();
    }

    /** The census delta by identity (Entity has no equals override) — contamination-safe baseline rule. */
    private static List<EntityMindSpider> spawnedSince(final List<EntityMindSpider> baseline,
                                                       final List<EntityMindSpider> census) {
        return census.stream().filter(e -> !baseline.contains(e)).toList();
    }

    private static int countGuardians(final GameTestHelper helper, final ServerLevel level) {
        return padEntities(helper, level, e -> {
            final ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            return ThaumcraftClassic.MOD_ID.equals(key.getNamespace())
                    && key.getPath().equals(EldritchMobHost.GUARDIAN);
        }).size();
    }

    private static List<Entity> padEntities(final GameTestHelper helper, final ServerLevel level,
                                            final Predicate<Entity> filter) {
        final BlockPos o = helper.absolutePos(BlockPos.ZERO);
        return level.getEntitiesOfClass(Entity.class,
                new AABB(o.getX() - 1, o.getY() - 3, o.getZ() - 1, o.getX() + 25, o.getY() + 27, o.getZ() + 25),
                filter);
    }

    /**
     * A {@link RandomSource} that replays a scripted queue through
     * {@code nextInt(bound)} — the only draw family the warp event uses
     * ({@code :45}, {@code :50}, and {@code nextIntBetweenInclusive} over the
     * scan draws). A draw outside the queue fails (the port draws more than
     * the original) and an unconsumed queue fails at the assert (the port
     * draws fewer, in a different order, with a different bound).
     */
    private static final class Scripted implements RandomSource {

        private final Deque<Integer> draws = new ArrayDeque<>();
        int consumed;

        Scripted offer(final int... values) {
            for (final int v : values) {
                draws.add(v);
            }
            return this;
        }

        @Override
        public int nextInt(final int bound) {
            final Integer v = draws.poll();
            if (v == null) {
                throw new IllegalStateException("the warp event drew beyond the scripted queue (bound " + bound
                        + ") — the port draws more times than WarpEvents.java does");
            }
            if (v >= bound) {
                throw new IllegalStateException("scripted draw " + v + " exceeds bound " + bound
                        + " — the port rolled with the wrong bound");
            }
            consumed++;
            return v;
        }

        boolean isExhausted() {
            return draws.isEmpty();
        }

        int unconsumed() {
            return draws.size();
        }

        private IllegalStateException unused() {
            return new IllegalStateException("the warp event must draw nextInt(bound) only — the original "
                    + "spends no other random family here");
        }

        @Override
        public RandomSource fork() {
            throw unused();
        }

        @Override
        public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional() {
            throw unused();
        }

        @Override
        public void setSeed(final long seed) {
            throw unused();
        }

        @Override
        public int nextInt() {
            throw unused();
        }

        @Override
        public long nextLong() {
            throw unused();
        }

        @Override
        public boolean nextBoolean() {
            throw unused();
        }

        @Override
        public float nextFloat() {
            throw unused();
        }

        @Override
        public double nextDouble() {
            throw unused();
        }

        @Override
        public double nextGaussian() {
            throw unused();
        }
    }
}
