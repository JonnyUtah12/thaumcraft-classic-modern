package com.jonnyutah12.thaumcraftclassic.visnet;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-02 acceptance: the vis network container model (node→conduit→device)
 * — a port of {@code api/visnet/VisNetHandler} + {@code TileVisNode}. Covers
 * source registration, range-limited attachment, attunement matching, the
 * original's root-preference distance quirk (VisNetHandler.java:66-72),
 * drain through the parent chain, the drainer-nearest-node selection
 * (calculateNearbyNodes), traversal guards (line of sight, chunk-loaded),
 * removal, the self-registration tick pass, and drainer-cache invalidation.
 */
class VisNetHandlerTest {

    private static final ResourceKey<Level> DIM = Level.OVERWORLD;
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    private static final Aspect AER = new Aspect(AspectDefinitions.all().stream()
            .filter(def -> def.tag().equals("aer")).findFirst().orElseThrow());

    /** A source: holds its own aspect store and drains it. */
    private static class SourceNode extends VisNodeBase {
        private final BlockPos pos;
        private final int range;
        private final byte attunement;
        private final AspectList store = new AspectList();
        private boolean removed;

        SourceNode(final BlockPos pos, final int range) {
            this(pos, range, (byte) -1);
        }

        SourceNode(final BlockPos pos, final int range, final byte attunement) {
            this.pos = pos;
            this.range = range;
            this.attunement = attunement;
        }

        @Override public BlockPos getLocation() { return pos; }
        @Override public ResourceKey<Level> getNetworkDimension() { return DIM; }
        @Override public int getRange() { return range; }
        @Override public boolean isSource() { return true; }
        @Override public byte getAttunement() { return attunement; }
        @Override public boolean isRemoved() { return removed; }
        @Override public boolean isChunkLoaded() { return true; }

        void setRemoved(final boolean removed) {
            this.removed = removed;
        }

        @Override
        public int consumeVis(final Aspect aspect, final int vis) {
            final int taken = Math.min(store.getAmount(aspect), vis);
            if (taken > 0) {
                store.reduce(aspect, taken);
                triggerConsumeEffect(aspect);
            }
            return taken;
        }
    }

    /** A conduit/device: no store; consumes through the parent (VisNodeBase default). */
    private static class RelayNode extends VisNodeBase {
        private final BlockPos pos;
        private final int range;
        private final byte attunement;
        private boolean chunkLoaded = true;

        RelayNode(final BlockPos pos, final int range) {
            this(pos, range, (byte) -1);
        }

        RelayNode(final BlockPos pos, final int range, final byte attunement) {
            this.pos = pos;
            this.range = range;
            this.attunement = attunement;
        }

        @Override public BlockPos getLocation() { return pos; }
        @Override public ResourceKey<Level> getNetworkDimension() { return DIM; }
        @Override public int getRange() { return range; }
        @Override public boolean isSource() { return false; }
        @Override public byte getAttunement() { return attunement; }
        @Override public boolean isRemoved() { return false; }
        @Override public boolean isChunkLoaded() { return chunkLoaded; }
    }

    /** Attach a non-source through the handler, wiring the parent edge exactly
     * as the original caller (TileVisNode update) does. */
    private static IVisNode attach(final IVisNode node) {
        final IVisNode parent = VisNetHandler.addNode(DIM, node);
        node.setParent(parent);
        return parent;
    }

    @BeforeEach
    void resetNetwork() {
        VisNetHandler.clearNetworks();
        VisNetHandler.setLineOfSight(null);
    }

    @Test
    void deviceAttachesToSourceWithinRangeAndDrainsThroughIt() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 16);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);

        final RelayNode device = new RelayNode(new BlockPos(6, 64, 0), 8); // distSq 36 <= 64
        assertSame(source, attach(device));

        // Drain at the device position: flows up the parent edge to the source store.
        final int drained = VisNetHandler.drainVis(DIM, device.getLocation(), AER, 400);
        assertEquals(400, drained);
        assertEquals(600, source.store.getAmount(AER));
        assertSame(source, device.getRootSource());
    }

    @Test
    void nodeOutOfEveryRangeStaysUnattachedAndDrainsNothing() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 16);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);

        final RelayNode far = new RelayNode(new BlockPos(40, 64, 0), 8); // distSq 1600 > 64
        assertNull(attach(far));
        assertEquals(0, VisNetHandler.drainVis(DIM, far.getLocation(), AER, 100));
    }

    @Test
    void chainDeviceConduitSourceDrainsEndToEnd() {
        // node -> conduit -> device: the device borrows through the conduit.
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        source.store.add(AER, 500);
        VisNetHandler.addSource(DIM, source);

        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16); // distSq 100 <= 256
        assertSame(source, attach(relay));

        final RelayNode pedestal = new RelayNode(new BlockPos(16, 64, 0), 8); // distSq to relay 36 <= 64
        assertSame(relay, attach(pedestal));

        final int drained = VisNetHandler.drainVis(DIM, pedestal.getLocation(), AER, 300);
        assertEquals(300, drained);
        assertEquals(200, source.store.getAmount(AER));
    }

    @Test
    void attunementMismatchBlocksTheLinkButMinusOneMatchesAnything() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 16, (byte) 5);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);

        final RelayNode mismatched = new RelayNode(new BlockPos(4, 64, 0), 8, (byte) 2);
        assertNull(attach(mismatched));

        final RelayNode wildcard = new RelayNode(new BlockPos(4, 64, 1), 8, (byte) -1);
        assertSame(source, attach(wildcard));
    }

    @Test
    void rootIsPreferredOverItsOwnNearerDescendantTheOriginalQuirk() {
        // Original keys roots at r - targetRange*2 and descendants at raw r
        // (VisNetHandler.java:66-72): mixed units, deliberately root-biased.
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        VisNetHandler.addSource(DIM, source);

        final RelayNode nearRoot = new RelayNode(new BlockPos(1, 64, 0), 16);
        assertSame(source, attach(nearRoot)); // distSq 1, key 1 - 32 = -31

        // Target at (10,64,0), range 16: root distSq 100 -> key 100 - 32 = 68;
        // descendant distSq 81 -> raw key 81. 68 < 81: the root wins even
        // though the descendant is one block closer. Pinned quirk.
        final RelayNode target = new RelayNode(new BlockPos(10, 64, 0), 16);
        assertSame(source, attach(target));
    }

    @Test
    void nearerDescendantWinsWhenTheRootIsOutOfTheTargetsRange() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);
        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16);
        assertSame(source, attach(relay));

        // Target at (18,64,0), range 16: root distSq 324 > 256 (out of the
        // target's range), relay distSq 64 -> the relay wins.
        final RelayNode target = new RelayNode(new BlockPos(18, 64, 0), 16);
        assertSame(relay, attach(target));
    }

    @Test
    void drainPicksTheNearestEligibleMemberPerTree() {
        // Source (range 32) with a relay child; the drainer sits next to the
        // relay: calculateNearbyNodes must choose the relay (judged by its own
        // range, VisNetHandler.java:144-158), and the drain still reaches the
        // source through it.
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);
        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16);
        assertSame(source, attach(relay));

        final int drained = VisNetHandler.drainVis(DIM, new BlockPos(12, 64, 0), AER, 100);
        assertEquals(100, drained);
        assertEquals(900, source.store.getAmount(AER));
    }

    @Test
    void lineOfSightHookBlocksAttachmentWhenInstalled() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 16);
        VisNetHandler.addSource(DIM, source);

        VisNetHandler.setLineOfSight((from, to) -> false);
        final RelayNode blocked = new RelayNode(new BlockPos(4, 64, 0), 8);
        assertNull(attach(blocked));

        VisNetHandler.setLineOfSight(null);
        assertSame(source, attach(blocked));
    }

    @Test
    void unloadedChildrenAreSkippedByDrainTraversal() {
        // Source range 11: the drainer at (12,64,0) is outside the source's
        // own range (distSq 144 > 121) and only reachable through the relay.
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 11);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);
        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16); // distSq 100 <= 256
        assertSame(source, attach(relay));

        relay.chunkLoaded = false;
        assertEquals(0, VisNetHandler.drainVis(DIM, new BlockPos(12, 64, 0), AER, 100));

        // Fresh drainer position (still outside the source's own range) so the
        // per-position drainer cache from the previous call can't mask the flip.
        relay.chunkLoaded = true;
        assertEquals(100, VisNetHandler.drainVis(DIM, new BlockPos(13, 64, 0), AER, 100));
    }

    @Test
    void removingASourceDetachesTheNetworkAndStopsDrain() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);
        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16);
        assertSame(source, attach(relay));
        assertEquals(100, VisNetHandler.drainVis(DIM, relay.getLocation(), AER, 100));

        source.setRemoved(true);
        source.removeThisNode();

        assertTrue(relay.getChildren().isEmpty());
        assertNull(relay.getParent());
        assertEquals(0, VisNetHandler.drainVis(DIM, relay.getLocation(), AER, 100));
    }

    @Test
    void networkTickRegistersSourcesAndReattachesOrphans() {
        // The TileVisNode update pass (TileVisNode.java:117-152): sources
        // self-register once, members without a valid parent re-attach.
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 32);
        source.store.add(AER, 1000);
        final RelayNode relay = new RelayNode(new BlockPos(10, 64, 0), 16);

        for (int tick = 0; tick < 41; tick++) {
            source.networkTick();
            relay.networkTick();
        }
        assertNotNull(relay.getParent());
        assertSame(source, relay.getRootSource());
        assertEquals(50, VisNetHandler.drainVis(DIM, relay.getLocation(), AER, 50));
    }

    @Test
    void topologyChangesInvalidateTheDrainerCache() {
        // First drain caches "nothing nearby" for this position; registering a
        // new source must clear that cache (VisNetHandler.java:47).
        final SourceNode far = new SourceNode(new BlockPos(100, 64, 0), 4);
        VisNetHandler.addSource(DIM, far);
        final BlockPos pedestal = new BlockPos(12, 64, 0);
        assertEquals(0, VisNetHandler.drainVis(DIM, pedestal, AER, 100));

        final SourceNode near = new SourceNode(new BlockPos(0, 64, 0), 16);
        near.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, near); // clears the stale entry

        assertEquals(100, VisNetHandler.drainVis(DIM, pedestal, AER, 100));
    }

    @Test
    void separateDimensionsDoNotShareNetworks() {
        final SourceNode source = new SourceNode(new BlockPos(0, 64, 0), 16);
        source.store.add(AER, 1000);
        VisNetHandler.addSource(DIM, source);

        final RelayNode nether = new RelayNode(new BlockPos(1, 64, 0), 8) {
            @Override public ResourceKey<Level> getNetworkDimension() { return NETHER; }
        };
        assertNull(VisNetHandler.addNode(NETHER, nether));
        assertEquals(0, VisNetHandler.drainVis(NETHER, nether.getLocation(), AER, 100));
    }
}
