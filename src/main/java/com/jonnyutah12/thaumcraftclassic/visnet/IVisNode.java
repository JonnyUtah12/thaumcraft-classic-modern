package com.jonnyutah12.thaumcraftclassic.visnet;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Vis-network container contract (TC-M4-02): the port of the public surface of
 * {@code thaumcraft.api.visnet.TileVisNode} ({@code api/visnet/TileVisNode.java}),
 * expressed as an interface so the network model
 * ({@link VisNetHandler}, node→conduit→device) is testable without tile
 * entities. The tile implementations arrive with the feature cards that own
 * them (vis relay / energized node: TC-M5-02/M6; devices such as the wand
 * pedestal: TC-M6-04d) and extend {@link VisNodeBase}, which carries the
 * shared parent/child plumbing. Design record: docs/architecture/vis.md.
 *
 * <p>Parity deviation: the original held parent/child edges as
 * {@code WeakReference}s so unloaded/GC'd tiles dropped out implicitly. The
 * port uses explicit edges plus {@link #isRemoved()} — tile invalidation is
 * deterministic on the target platform, and GC-based liveness is not. The
 * original's chunk-loaded guard during traversal is preserved through
 * {@link #isChunkLoaded()}.</p>
 */
public interface IVisNode {

    /** Block position of this node (original {@code getLocation()}). */
    BlockPos getLocation();

    /** Network dimension this node registers into (original: the tile's world). */
    ResourceKey<Level> getNetworkDimension();

    /** Link range in blocks (original {@code getRange()}; relay/energized node: 8,
     * docs/balance-values.md §6.3). */
    int getRange();

    /** Whether this node holds vis itself (vis node source) or only relays (conduit/device). */
    boolean isSource();

    /** Attunement filter; -1 matches everything (original {@code getAttunement()}). */
    default byte getAttunement() {
        return -1;
    }

    /**
     * Consume up to {@code vis} centi-vis of one aspect from this node's
     * network position. Sources drain their own store; conduits/devices
     * delegate to their parent ({@link VisNodeBase}'s default, original
     * TileVisNode.java:31-40). Returns the amount actually taken.
     */
    int consumeVis(Aspect aspect, int vis);

    IVisNode getParent();

    void setParent(IVisNode parent);

    /** Live child edge list (the handler appends here on attach). */
    List<IVisNode> getChildren();

    boolean isNodeRefresh();

    void setNodeRefresh(boolean refresh);

    /** False once the backing tile is gone (original {@code isInvalid()} check in {@code isNodeValid}). */
    boolean isRemoved();

    /** Whether this node's chunk is currently loaded (original traversal guard). */
    boolean isChunkLoaded();

    /** Detach this node and its whole subtree from the network (original {@code removeThisNode()}). */
    void removeThisNode();

    /** Walk parent edges to the owning source, or null (original {@code getRootSource()}). */
    IVisNode getRootSource();

    /** Hook fired when the parent edge changes (original no-op hook). */
    void parentChanged();

    /** Hook fired when vis actually flowed through this node (original {@code triggerConsumeEffect}). */
    void triggerConsumeEffect(Aspect aspect);
}
