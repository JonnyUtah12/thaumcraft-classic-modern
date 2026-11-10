package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.OuterLands;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.TeleporterThaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Eldritch Portal tile (content-mapping.csv
 * {@code thaumcraft:tileeldritchportal}; the original
 * {@code thaumcraft.common.tiles.TileEldritchPortal}).
 *
 * <p>Original cadence, ported verbatim: the tile updates every tick
 * ({@code canUpdate = true}); on the client it spawns the
 * {@code thaumcraft:evilportal} sparkle every 250 ticks and counts
 * {@code opencount} up to 30 for the portal renderer (TC-M7-02); on the
 * server, every 5 ticks it collects every {@code EntityPlayerMP} inside the
 * block box grown by (0.5, 1.0, 0.5) whose mount/passenger slots are free,
 * and for each:</p>
 * <ul>
 *   <li>the vanilla travel timer {@code timePortal} (bW) is the gate: on
 *   cooldown the tile only refreshes the timer to 100 ticks (5 s) and travels
 *   nobody — this is what keeps a player standing in the arrival portal from
 *   ping-ponging;</li>
 *   <li>overworld → the tile transfers to the Outer Lands
 *   ({@code Config.dimensionOuterId}) through {@link TeleporterThaumcraft}
 *   (nearest-portal placement) and completes the {@code ENTEROUTER} research
 *   once per player;</li>
 *   <li>Outer Lands → the tile transfers back to the overworld (dimension 0),
 *   again through the nearest-portal teleporter.</li>
 * </ul>
 *
 * <h2>1.21.1 adaptations</h2>
 * <ul>
 *   <li>{@code FMLCommonHandler.getMinecraftServerInstance} →
 *   {@code level.getServer()}; {@code transferPlayerToDimension(player, dim,
 *   teleporter)} is gone: 1.21.1 travels through
 *   {@link ServerPlayer#changeDimension(DimensionTransition)}, and the
 *   teleporter's whole job — cache + nearest {@code blockportaleldritch}
 *   search + (±1, 0, ±1) placement + zero velocity — is ported verbatim in
 *   {@link TeleporterThaumcraft} and feeds the transition's position.</li>
 *   <li>The 100-tick cooldown: 1.21.1's {@code Entity.setPortalCooldown()}
 *   only sets 10 ticks and the field is private, so the tile keeps its own
 *   100-tick expiry per player and reads the vanilla timer as the
 *   {@code bW > 0} test.</li>
 *   <li>{@code ENTEROUTER} is a research id and the research manager is
 *   TC-M3-03 territory: the completion is recorded on
 *   {@link #enteredOuterLands} and handed to {@link #enterouterHook} — a no-op
 *   until the research card wires the manager, exactly the house seam pattern;
 *   the travel itself (the observable part) ships here.</li>
 * </ul>
 */
public class TileEldritchPortal extends BlockEntity {

    public static final BlockEntityTicker<TileEldritchPortal> SERVER_TICKER =
            (level, pos, state, tile) -> tile.serverTick();

    /** The original's open-animation counter for the portal renderer (TC-M7-02). */
    public int opencount = -1;
    /** The original's update counter; the player scan runs on {@code count % 5 == 0}. */
    public int count = 0;

    /** Players that have entered the Outer Lands (the original gates ENTEROUTER on first entry). */
    public static final java.util.Set<UUID> enteredOuterLands =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Research hook: called once per player with the original's ENTEROUTER completion. */
    @Nullable
    public static java.util.function.Consumer<ServerPlayer> enterouterHook;

    /** UUID -> world-game-time the 100-tick travel cooldown expires (the original's bW refresh rule). */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    /** The original's cooldown length: bW := 100 (5 s). */
    public static final int TRAVEL_COOLDOWN_TICKS = 100;

    public TileEldritchPortal(final BlockPos pos, final BlockState state) {
        super(CatalogOuterLands.TILE_ELDRITCH_PORTAL.get(), pos, state);
    }

    /**
     * The original's {@code onNeighborChange} fact: the portal counts as open
     * with {@code blockEldritch} directly above and below (the renderer's
     * open animation and the frame check in the tests read this).
     */
    public boolean hasEldritchFrame() {
        if (level == null) {
            return false;
        }
        final var eldritch = com.jonnyutah12.thaumcraftclassic.registration.catalog
                .CatalogEldritchRealm.BLOCK_ELDRITCH.get();
        return level.getBlockState(worldPosition.above()).is(eldritch)
                && level.getBlockState(worldPosition.below()).is(eldritch);
    }

    void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        count++;
        if (count % 5 != 0) {
            return;
        }
        checkPlayers((ServerLevel) level);
    }

    /**
     * The original's {@code checkPlayers}: every player inside the block box
     * grown by (0.5, 1.0, 0.5) whose mount/passenger slots are free. 1.7.10
     * scanned the loaded-entity list (which contains players); 1.21.1 keeps
     * players out of the entity list, so the scan walks
     * {@link ServerLevel#getPlayers()} — the per-dimension player list is the
     * faithful 1.21.1 carrier of the same fact.
     */
    public void checkPlayers(final ServerLevel serverLevel) {
        final AABB box = new AABB(worldPosition).inflate(0.5, 1.0, 0.5);
        for (final ServerPlayer player : serverLevel.getPlayers(p -> true)) {
            // Original gate: player.ridingEntity == null && player.passengers empty
            if (player.getVehicle() != null || !player.getPassengers().isEmpty()) {
                continue;
            }
            if (!box.contains(player.position())) {
                continue;
            }
            final long now = serverLevel.getGameTime();
            final Long until = COOLDOWN_UNTIL.get(player.getUUID());
            if ((until != null && now < until) || player.getPortalCooldown() > 0) {
                // Original: on cooldown, refresh bW to 100 and travel nobody.
                COOLDOWN_UNTIL.put(player.getUUID(), now + TRAVEL_COOLDOWN_TICKS);
                continue;
            }
            COOLDOWN_UNTIL.put(player.getUUID(), now + TRAVEL_COOLDOWN_TICKS);
            travel(serverLevel, player);
        }
    }

    /**
     * The original's travel branch: Outer Lands → overworld (0), anything
     * else → Outer Lands ({@code Config.dimensionOuterId}); the destination
     * placement is the teleporter's nearest-portal search, the fallback is
     * the player's current coordinates — what the original leaves them at
     * when its search finds no portal.
     */
    public void travel(final ServerLevel from, final ServerPlayer player) {
        final MinecraftServer server = from.getServer();
        final var targetKey = OuterLands.isOuterLands(from)
                ? net.minecraft.world.level.Level.OVERWORLD
                : OuterLands.OUTER_LANDS;
        final ServerLevel to = server.getLevel(targetKey);
        if (to == null) {
            return;
        }
        final Optional<Vec3> found = TeleporterThaumcraft.findNearestPortal(to, player);
        final Vec3 target = found.orElse(player.position());

        if (!OuterLands.isOuterLands(from) && enteredOuterLands.add(player.getUUID())) {
            // Original: !isResearchComplete(player, "ENTEROUTER") →
            // PacketResearchComplete + completeResearch(player, "ENTEROUTER").
            // The research manager ships on TC-M3-03; the completion is
            // recorded and handed to the hook, a no-op until then.
            final var hook = enterouterHook;
            if (hook != null) {
                hook.accept(player);
            }
        }
        player.changeDimension(new DimensionTransition(to, target, Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
    }

    /** Test seam: the cooldown bookkeeping. */
    public static boolean onTravelCooldown(final UUID player, final long gameTime) {
        final Long until = COOLDOWN_UNTIL.get(player);
        return until != null && gameTime < until;
    }

    /** Test seam: drop cooldown + entry bookkeeping for a player. */
    public static void clearTravelState(final UUID player) {
        COOLDOWN_UNTIL.remove(player);
        enteredOuterLands.remove(player);
    }
}
