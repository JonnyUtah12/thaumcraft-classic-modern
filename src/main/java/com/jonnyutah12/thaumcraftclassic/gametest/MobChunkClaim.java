package com.jonnyutah12.thaumcraftclassic.gametest;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import org.slf4j.Logger;

/**
 * The chunk claim for probes that read <b>mob ticks</b> (t_f9fe6e81,
 * docs/testing.md#mob-tick-claim-covers-mob-chunk).
 *
 * <p><b>Why a claim is needed at all.</b> Every probe in this suite runs on
 * {@code template = "empty"}, a 1×1×1 box, so the harness's
 * {@code StructureUtils#forceLoadChunks} (framework {@code :165-170}) force-loads
 * {@code boundingBox.intersectingChunks()} = the PAD CORNER's chunk and nothing else, and
 * {@code ServerLevel#tickEntities} ({@code :390-395, 411}) ticks an entity only in chunks that
 * reach {@code inEntityTickingRange(chunkPosition)} — ticket level
 * {@code ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING)} = 31 ({@code ChunkLevel:47-49
 * isEntityTicking(level) = level <= 31}). A fixture mob is armed at {@code pad(1, 3, 1)}, one
 * block off the corner, so in every grid slot whose corner lands at x mod 16 = 15 / z mod 16 = 15
 * the mob stands in a chunk that carries no ticket — the 4-neighbour of the corner's chunk
 * inherits the level and ticks, the DIAGONAL neighbour does not — and the mob runs ZERO entity
 * ticks for its whole life. Measured on the trace legs of the tree the lane lands on: green legs
 * run {@code mobTick} 1:1 with {@code gameTime} and read {@code entTick=true forcedHere=true},
 * the red legs read {@code mobTick=0} from the first sample for the probe's whole 400-tick life
 * with the mob alive, in level, unremoved. The walkback arm of
 * {@code EldritchMobGameTest.theGuardianBreathesForFiveHundredTicksAndBurns} reads the un-walked
 * air — the lane's {@code got -20}.</p>
 *
 * <p><b>The claim is the harness's own lever on the mob's chunk</b> —
 * {@link ServerLevel#setChunkForced(int, int, boolean)}, the exact call
 * {@code StructureUtils#forceLoadChunks} makes for the template box, i.e.
 * {@code ForcedChunkManager.addForcedChunk} → {@code TicketType.FORCED_TICKET} at level 31 plus
 * the level's {@code ForcedChunksSavedData} entry. It is deliberately NOT a plain
 * {@code DistanceManager.addTicket(TicketType.FORCED, …)}: on a chunk that is not loaded yet
 * that ticket is queued as pending and the level map keeps {@code MAX_TICKET_LEVEL} for the chunk
 * until it arrives, so a probe that reads mob ticks 20 ticks after setup measures
 * {@code entTick=false} on the freshly claimed chunk — measured on {@code fix1_leg_4} of this
 * card (armed=true, entTick=false, 0 of 20 ticks). The forced-set entry is cleared for the whole
 * level by the harness at every batch end ({@code GameTestRunner#runBatch}, {@code :100-103}), so
 * a claim taken at setup cannot outlive the batch that took it, and it makes the level's
 * {@code !players.isEmpty() || ForcedChunkManager.hasForcedChunks(level)} gate at
 * {@code ServerLevel:390} read true — the 300-tick {@code emptyTime} wall of
 * {@code ServerLevel#tickEntities} ({@code :390-395}) cannot freeze the tick loop for a batch
 * that carries mob probes.</p>
 *
 * <p>The claim is keyed on the mob's spawn chunk, so it covers the mob while it stands there;
 * a probe whose gates read ticks after the mob moves has to widen the set itself and say so, the
 * way {@code docs/testing.md#scan-claim-covers-box-cells} requires of a cell claim.
 * {@code -Dtc.noMobChunkClaim} (gradle {@code -Ptc.mobChunkClaimOff}) drops the claim for a
 * measurement leg — the teeth arm of the plant pair in
 * {@code EldritchMobGameTest.theGuardianArmedOneChunkOffThePadCornerStillTicks}.</p>
 */
public final class MobChunkClaim {

    /** System property that drops the claim; set only by a measurement leg. */
    public static final String OFF_PROPERTY = "tc.noMobChunkClaim";

    /** ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) — 31 on 21.1.259. */
    private static final int ENTITY_TICKING_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);

    private static final Logger LOGGER = LogUtils.getLogger();

    private MobChunkClaim() {
    }

    /** True while the claim is armed for this JVM (read per call, so a leg can flip it). */
    public static boolean armed() {
        return !Boolean.getBoolean(OFF_PROPERTY);
    }

    /** The ticket level the claim needs (for a probe's own census line). */
    public static int entityTickingLevel() {
        return ENTITY_TICKING_LEVEL;
    }

    /** Claim the chunk the mob stands in. Returns that chunk. */
    public static ChunkPos claimMobChunk(final ServerLevel level, final Entity mob) {
        return claim(level, mob.blockPosition());
    }

    /** Claim the chunk that contains {@code at}. Idempotent: the forced set is keyed on the chunk. */
    public static ChunkPos claim(final ServerLevel level, final BlockPos at) {
        return claim(level, new ChunkPos(at), at);
    }

    /** Claim {@code pos}'s chunk — the arm set for a probe that follows a mob's live chunk. */
    public static ChunkPos claim(final ServerLevel level, final ChunkPos pos) {
        return claim(level, pos, pos);
    }

    private static ChunkPos claim(final ServerLevel level, final ChunkPos pos, final Object where) {
        if (!armed()) {
            LOGGER.info("[t_f9fe6e81] mob-chunk claim SKIPPED by -D{} — chunk ({}, {}) of {} keeps"
                            + " whatever the harness gave it: entTick={} forcedHere={}"
                            + " (the harness forces only the pad corner's chunk,"
                            + " docs/testing.md#mob-tick-claim-covers-mob-chunk)",
                    OFF_PROPERTY, pos.x, pos.z, where, entTicking(level, pos), forcedHere(level, pos));
            return pos;
        }
        level.setChunkForced(pos.x, pos.z, true);
        LOGGER.info("[t_f9fe6e81] forced mob chunk ({}, {}) of {} — entTick={} forcedHere={}"
                        + " (docs/testing.md#mob-tick-claim-covers-mob-chunk)",
                pos.x, pos.z, where, entTicking(level, pos), forcedHere(level, pos));
        return pos;
    }

    /**
     * Does this chunk currently reach the entity-ticking range the level's tick loop reads?
     * The census a mob-tick probe has to carry: a frozen per-tick read with {@code entTick=false}
     * is the claim missing, not the product.
     */
    public static boolean entTicking(final ServerLevel level, final ChunkPos pos) {
        return level.getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(pos.toLong());
    }

    /** Is this chunk in the level's forced set (the harness's own set, cleared at batch end)? */
    public static boolean forcedHere(final ServerLevel level, final ChunkPos pos) {
        return level.getForcedChunks().contains(pos.toLong());
    }

    /**
     * Is the chunk LOADED? This is the half a force does NOT give you for free: the ticket census
     * ({@link #entTicking}, {@link #forcedHere}) reads true the moment the force lands, and the
     * FULL load that the force requests has to go through the batch's shared load queue. The
     * level's entity tick loop visits the entities of LOADED chunks only, so a mob on a
     * ticketed-but-unloaded chunk runs ZERO ticks while every ticket census reads healthy —
     * measured {@code fix5_leg_3}: 285 ticks, {@code entTick=true forcedHere=true}, mob alive,
     * unremoved, in the level's entity list, 0 entity ticks in 57 samples. Any probe that gates on
     * mob ticks on a cold chunk has to carry this read next to the ticket reads.
     */
    public static boolean loaded(final ServerLevel level, final ChunkPos pos) {
        return level.hasChunk(pos.x, pos.z);
    }

    /** The chunk's status for the census — {@code "absent"} until the load arrives. */
    public static String status(final ServerLevel level, final ChunkPos pos) {
        final ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        return chunk == null ? "absent" : String.valueOf(chunk.getPersistedStatus());
    }
}
