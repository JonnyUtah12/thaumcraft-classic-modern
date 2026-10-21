package com.jonnyutah12.thaumcraftclassic.visnet;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared plumbing for vis-network members (TC-M4-02): the port of the
 * non-abstract body of {@code thaumcraft.api.visnet.TileVisNode}
 * ({@code api/visnet/TileVisNode.java}). Concrete members (vis node sources,
 * relays, devices — owned by the M5/M6 feature cards) extend this and supply
 * location/range/source-ness; sources additionally hold and drain their own
 * aspect store.
 *
 * <p>Non-source {@link #consumeVis} delegates up the parent chain
 * (TileVisNode.java:31-40): a device never holds vis, it borrows from the
 * source it is attached to. {@link #networkTick()} is the original's
 * {@code onUpdate} pass (TileVisNode.java:117-152): every 40 ticks (or on
 * demand via {@code nodeRefresh}) it validates child edges, drops the subtree
 * on any break, registers sources once, and re-attaches orphaned members.</p>
 */
public abstract class VisNodeBase implements IVisNode {

    private IVisNode parent;
    private final List<IVisNode> children = new ArrayList<>();
    private boolean nodeRefresh;
    private int nodeCounter;
    private boolean nodeRegistered;

    @Override
    public byte getAttunement() {
        return -1;
    }

    /**
     * Conduit/device default: consume through the parent edge
     * (original TileVisNode.java:31-40). Sources override this to drain their
     * own store.
     */
    @Override
    public int consumeVis(final Aspect aspect, final int vis) {
        if (VisNetHandler.isNodeValid(getParent())) {
            final int out = getParent().consumeVis(aspect, vis);
            if (out > 0) {
                triggerConsumeEffect(aspect);
            }
            return out;
        }
        return 0;
    }

    @Override
    public IVisNode getParent() {
        return parent;
    }

    @Override
    public void setParent(final IVisNode parent) {
        this.parent = parent;
    }

    @Override
    public List<IVisNode> getChildren() {
        return children;
    }

    @Override
    public boolean isNodeRefresh() {
        return nodeRefresh;
    }

    @Override
    public void setNodeRefresh(final boolean refresh) {
        this.nodeRefresh = refresh;
    }

    /** Detach this node and its whole subtree (original {@code removeThisNode},
     * TileVisNode.java:42-64). */
    @Override
    public void removeThisNode() {
        for (final IVisNode child : new ArrayList<>(children)) {
            if (child != null) {
                child.removeThisNode();
            }
        }
        children.clear();
        if (VisNetHandler.isNodeValid(parent)) {
            parent.setNodeRefresh(true);
        }
        setParent(null);
        parentChanged();
        if (isSource()) {
            VisNetHandler.removeSource(getNetworkDimension(), this);
        }
        nodeRegistered = false;
    }

    /** Original {@code getRootSource} (TileVisNode.java:71-75). */
    @Override
    public IVisNode getRootSource() {
        if (VisNetHandler.isNodeValid(getParent())) {
            return getParent().getRootSource();
        }
        return isSource() ? this : null;
    }

    @Override
    public void parentChanged() {
        // Original no-op hook; subclasses may react to (re)attachment.
    }

    @Override
    public void triggerConsumeEffect(Aspect aspect) {
        // Original no-op hook; subclasses may fire visuals.
    }

    /**
     * The original's {@code onUpdate} network pass (TileVisNode.java:117-152),
     * driven once per game tick by the backing tile. The original also marked
     * the block for update on refresh ({@code world.markBlockForUpdate}) —
     * that is the tile layer's job here.
     */
    public void networkTick() {
        if (nodeCounter++ % 40 == 0 || nodeRefresh) {
            if (!nodeRefresh && !children.isEmpty()) {
                for (final IVisNode child : children) {
                    if (!VisNetHandler.isNodeValid(child) || !VisNetHandler.canNodeBeSeen(this, child)) {
                        nodeRefresh = true;
                        break;
                    }
                }
            }

            if (nodeRefresh) {
                for (final IVisNode child : children) {
                    if (child != null) {
                        child.setNodeRefresh(true);
                    }
                }
                children.clear();
                parent = null;
            }

            if (isSource() && !nodeRegistered) {
                VisNetHandler.addSource(getNetworkDimension(), this);
                nodeRegistered = true;
            } else if (!isSource() && !VisNetHandler.isNodeValid(parent)) {
                setParent(VisNetHandler.addNode(getNetworkDimension(), this));
                nodeRefresh = true;
            }

            if (nodeRefresh) {
                parentChanged();
            }

            nodeRefresh = false;
        }
    }
}
