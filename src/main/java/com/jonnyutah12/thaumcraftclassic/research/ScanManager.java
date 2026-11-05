package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedPhenomenaPayload;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeLookup;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.world.NodeTickDriver;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The scan state machine (THAUM-1 remainder). Port of
 * {@code thaumcraft.common.lib.research.ScanManager} (reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/research/ScanManager.java}),
 * scoped to the <b>scanType-3 phenomena / NODE branch</b>:
 * {@code ItemThaumometer.doScan} L88-107 builds
 * {@code new ScanResult((byte)3, 0, 0, null, "NODE" + ((INode)tile).getId())}
 * and {@code ScanManager.completeScan} L435-441 turns that string into node
 * aspects, records the phenomenon and feeds the aspect pool.
 *
 * <p>Scan types 1 (block/item) and 2 (entity) stay with their own cards: the
 * client's raycast already builds them in
 * {@code items/ItemThaumometer#doScan} and the server's completion of those
 * types is the registry-hash milestone, so
 * {@code network/ServerNetworkHooks#completeScan} keeps its TODO for them.</p>
 *
 * <h2>Node identity</h2>
 * The original's phenomenon key is {@code "NODE" + TileNode#getId()}, and
 * {@code TileNode.generateId} (L69-88) builds that id as
 * {@code dimension:x:y:z}. This port has no node block entity — generation
 * stores {@link WorldMagicData.Node} records (TC-M5-01) — so the id is built
 * from the record's own coordinates, and the dimension part is the modern
 * {@link ResourceLocation} form ({@code minecraft:overworld}) rather than the
 * original's numeric dimension id. The {@code :}-separated four-part shape is
 * kept verbatim, so {@link #parseNodeKey} reads the last three parts as
 * {@code x}/{@code y}/{@code z} and everything before them as the dimension.
 * Full key: {@code NODEminecraft:overworld:100:64:-200}.</p>
 *
 * <h2>Server-authoritative (the split)</h2>
 * The original runs the whole scan on the client: {@code doScan} is called
 * from {@code onItemRightClick}/{@code onUsingTick} under
 * {@code world.isRemote}, and it reads the node through
 * {@code world.getTileEntity(pos)}. That is impossible here — the port's node
 * state lives in {@link WorldMagicData}, which is {@code ServerLevel}-only, and
 * no node block exists to reach the client. The phenomena branch is therefore
 * split: {@link #scanNode} (the detection half, the original's type-3
 * {@code doScan}) and {@link #completeScan} (the completion half) both run on
 * the server, and the client's type-3 send is the remaining lane, listed in
 * docs/FEATURE_MATRIX.md.</p>
 *
 * <h2>Research feed</h2>
 * The original's {@code completeScan} leaves {@code clue} null for type 3
 * (L391, L435-441 — only types 1 and 2 assign it), so a node scan never calls
 * {@code ResearchManager.createClue}. Its only research feed is the aspect
 * tail ({@code checkAndSyncAspectKnowledge} → {@code completeAspect}); the
 * page-unlock step of that chain is the aspect lane's own TODO, so this class
 * feeds the pool and stops there. The shipped research graph carries item,
 * entity and aspect triggers only — there is no phenomena trigger to satisfy.
 */
public final class ScanManager {

    /** The original's thaumometer prefix (ItemThaumometer L67, L81, L182). */
    public static final String PREFIX_THAUMOMETER = "@";
    /** The original's registry-table prefix (ScanManager L390). */
    public static final String PREFIX_REGISTRY = "#";
    /** The only phenomena kind the original emits: {@code "NODE" + id}. */
    public static final String PHENOMENON_NODE = "NODE";

    /** ScanResult type 3 — the phenomena branch (ScanResult L12, ItemThaumometer L89). */
    public static final int SCAN_TYPE_PHENOMENA = 3;

    private ScanManager() {
    }

    /** A node phenomenon key's payload: which dimension, which block. */
    public record NodeKey(ResourceLocation dimension, BlockPos pos) {
    }

    // ------------------------------------------------------------ key shape

    /**
     * The original's phenomenon string: {@code "NODE" + TileNode#getId()}
     * (ItemThaumometer L89), where the id is {@code dimension:x:y:z}
     * (TileNode L75).
     */
    public static String nodePhenomenon(final ResourceKey<Level> dimension, final BlockPos pos) {
        return PHENOMENON_NODE + nodeKey(dimension, pos);
    }

    /** The bare node id (the original's {@code TileNode#getId()}). */
    public static String nodeKey(final ResourceKey<Level> dimension, final BlockPos pos) {
        return dimension.location() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    /**
     * Read a node id back out. The original goes the other way through
     * {@code TileNode.locations} (ScanManager L254); the port derives the
     * location from the id itself, so the last three {@code :}-separated parts
     * are x, y and z and the remainder is the dimension id. Malformed ids
     * (including the original's numeric-dimension ids from a pre-port save)
     * read as absent.
     */
    public static Optional<NodeKey> parseNodeKey(final String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return Optional.empty();
        }
        final int zSep = nodeId.lastIndexOf(':');
        final int ySep = zSep < 0 ? -1 : nodeId.lastIndexOf(':', zSep - 1);
        final int xSep = ySep < 0 ? -1 : nodeId.lastIndexOf(':', ySep - 1);
        if (xSep < 0) {
            return Optional.empty();
        }
        final ResourceLocation id = ResourceLocation.tryParse(nodeId.substring(0, xSep));
        if (id == null) {
            return Optional.empty();
        }
        try {
            final int x = Integer.parseInt(nodeId.substring(xSep + 1, ySep));
            final int y = Integer.parseInt(nodeId.substring(ySep + 1, zSep));
            final int z = Integer.parseInt(nodeId.substring(zSep + 1));
            return Optional.of(new NodeKey(id, new BlockPos(x, y, z)));
        } catch (final NumberFormatException notCoordinates) {
            return Optional.empty();
        }
    }

    /** {@code scan.phenomena.replace("NODE", "")} (ScanManager L436). */
    public static String stripPhenomenonPrefix(final String phenomena) {
        return phenomena == null ? "" : phenomena.replace(PHENOMENON_NODE, "");
    }

    // ---------------------------------------------------- phenomena detection

    /**
     * The original's type-3 {@code doScan} branch (ItemThaumometer L87-107):
     * the pointed block is a node → the phenomenon key. The original asks for
     * a tile implementing {@code INode}; the port's nodes are store records,
     * so the record is the test. Air and non-node blocks return empty, exactly
     * as the original returns null there.
     */
    public static Optional<String> scanNode(final ServerLevel level, final BlockPos pos) {
        return NodeLookup.findNodeAt(level, pos).map(node -> nodePhenomenon(level.dimension(), pos));
    }

    // ---------------------------------------------------------- node aspects

    /**
     * The original's {@code generateNodeAspects} (ScanManager L252-292): every
     * container aspect enters at {@code max(4, amount / 10)}, then the node
     * type adds its own signature. An empty result reads as null (L291), which
     * is what makes {@code validScan} refuse the scan.
     */
    public static AspectList nodeAspects(final AspectList container, final NodeType type,
                                         final Function<String, Aspect> resolver) {
        final AspectList tags = new AspectList();
        for (final Aspect aspect : container.getAspectsSorted()) {
            tags.merge(aspect, Math.max(4, container.getAmount(aspect) / 10));
        }
        // The original's Aspect.* constants, by their registry tags:
        // ENTROPY is the primal tagged "perditio" (Aspect.java L22), HUNGER is
        // "fames", TAINT "vitium", HEAL "sano", ORDER "ordo", DEATH "mortuus",
        // DARKNESS "tenebrae". merge() vs add() is kept exactly as the original
        // writes it (L269-286): merge keeps the higher of old/new, add
        // accumulates, so the distinction shows only when the container
        // already carries the type's own aspect.
        switch (type) {
            case UNSTABLE -> merge(tags, resolver, "perditio", 4);
            case HUNGRY -> merge(tags, resolver, "fames", 4);
            case TAINTED -> merge(tags, resolver, "vitium", 4);
            case PURE -> {
                merge(tags, resolver, "sano", 2);
                add(tags, resolver, "ordo", 2);
            }
            case DARK -> {
                merge(tags, resolver, "mortuus", 2);
                add(tags, resolver, "tenebrae", 2);
            }
            case NORMAL -> {
            }
        }
        return tags.size() > 0 ? tags : null;
    }

    private static void merge(final AspectList tags, final Function<String, Aspect> resolver,
                              final String tag, final int amount) {
        final Aspect aspect = resolver.apply(tag);
        if (aspect != null) {
            tags.merge(aspect, amount);
        }
    }

    private static void add(final AspectList tags, final Function<String, Aspect> resolver,
                            final String tag, final int amount) {
        final Aspect aspect = resolver.apply(tag);
        if (aspect != null) {
            tags.add(aspect, amount);
        }
    }

    /**
     * The record-keyed form, resolver-injected so the arithmetic is unit
     * testable without the aspect registry (the same seam
     * {@code NodeRuntime#fromRecord} uses).
     */
    public static AspectList nodeAspects(final WorldMagicData.Node record,
                                         final Function<String, Aspect> resolver) {
        final AspectList container = new AspectList();
        for (final var entry : record.aspects().entrySet()) {
            final Aspect aspect = resolver.apply(entry.getKey());
            if (aspect != null) {
                container.add(aspect, entry.getValue());
            }
        }
        return nodeAspects(container, NodeType.fromId(record.type()), resolver);
    }

    /**
     * The live node at the phenomenon key. The original's lookup walks
     * {@code TileNode.locations} (L254-262) and bails when the recorded
     * dimension is not the scanning world's; the port checks the key's own
     * dimension against {@code level}, then reads the freshest container
     * available: the tick driver's live runtime when the node is tracked
     * (the tile-equivalent state), else the stored record.
     */
    public static AspectList nodeAspects(final ServerLevel level, final String phenomena) {
        if (phenomena == null || !phenomena.startsWith(PHENOMENON_NODE)) {
            return null;
        }
        final Optional<NodeKey> ref = parseNodeKey(stripPhenomenonPrefix(phenomena));
        if (ref.isEmpty() || !ref.get().dimension().equals(level.dimension().location())) {
            return null;
        }
        final BlockPos pos = ref.get().pos();
        final var runtime = NodeTickDriver.runtime(level, pos);
        if (runtime != null) {
            return nodeAspects(runtime.getAspects(), runtime.getNodeType(), Aspect::getAspect);
        }
        return NodeLookup.findNodeAt(level, pos)
                .map(record -> nodeAspects(record, Aspect::getAspect))
                .orElse(null);
    }

    // ------------------------------------------------------------- scan gate

    /**
     * The original's {@code isValidScanTarget} type-3 clause (ScanManager
     * L328-333): the phenomenon is refused once the player's scanned-phenomena
     * list already carries {@code prefix + phenomena}.
     */
    public static boolean isValidScanTarget(final PlayerKnowledgeData knowledge, final String prefix,
                                            final String phenomena) {
        if (knowledge == null || phenomena == null) {
            return false;
        }
        return !knowledge.scannedPhenomenaKeys().contains(prefix + phenomena);
    }

    /**
     * The original's {@code hasBeenScanned} type-3 clause (ScanManager
     * L376-381): the phenomenon counts as scanned under either prefix.
     */
    public static boolean hasBeenScanned(final PlayerKnowledgeData knowledge, final String phenomena) {
        if (knowledge == null || phenomena == null) {
            return false;
        }
        final var keys = knowledge.scannedPhenomenaKeys();
        return keys.contains(PREFIX_THAUMOMETER + phenomena) || keys.contains(PREFIX_REGISTRY + phenomena);
    }

    // -------------------------------------------------------------- completion

    /**
     * The original's {@code ScanManager.completeScan} type-3 branch
     * (ScanManager L386-469) — the server half of a scan request:
     * <ol>
     *   <li>the phenomenon must be a node key resolvable in this dimension;</li>
     *   <li>{@code hasBeenScanned} under either prefix refuses a repeat (L390's
     *       {@code "@"}/{@code "#"} pairing);</li>
     *   <li>{@code validScan} requires a non-empty aspect list whose compound
     *       parents are already discovered (L404, L437);</li>
     *   <li>the key is stored as {@code prefix + phenomena} (L438) and the full
     *       set is re-synced, the port's stand-in for
     *       {@code PacketSyncScannedPhenomena};</li>
     *   <li>the aspect tail runs (L443-462) through
     *       {@link AspectKnowledge#applyScanKnowledge}, so the discovery +2,
     *       the pool cap compression and the sync packets are the shared
     *       implementation, not a copy.</li>
     * </ol>
     * Types 1 and 2 return false with the {@code ServerNetworkHooks} TODO
     * intact; the phenomena payload never carries them.
     */
    public static boolean completeScan(final ScannedToServerPayload payload, final ServerPlayer player) {
        if (payload.scanType() != SCAN_TYPE_PHENOMENA) {
            return false;
        }
        final String phenomena = payload.phenomena();
        if (phenomena == null || !phenomena.startsWith(PHENOMENON_NODE)) {
            return false;
        }
        final PlayerKnowledgeData current = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (current == null || hasBeenScanned(current, phenomena)) {
            return false;
        }
        final ServerLevel level = player.serverLevel();
        final AspectList aspects = nodeAspects(level, phenomena);
        if (!AspectKnowledge.validScan(aspects, current)) {
            return false;
        }
        final String prefix = payload.prefix() == null || payload.prefix().isEmpty()
                ? PREFIX_THAUMOMETER
                : payload.prefix();

        // L438: completeScannedPhenomena(player, prefix + phenomena).
        final PlayerKnowledgeData recorded = current.withScannedPhenomena(prefix + phenomena);
        if (recorded != current) {
            player.setData(ModAttachments.PLAYER_KNOWLEDGE, recorded);
            PacketDistributor.sendToPlayer(player,
                    new SyncScannedPhenomenaPayload(new ArrayList<>(recorded.scannedPhenomenaKeys())));
        }

        // L443-462: the knowledge tail. The thaumometer prefix is not the
        // registry, so scannedByThaumometer is false for "@": the aspect
        // amounts flow at face value (L390 makes it true only for "#").
        AspectKnowledge.applyScanKnowledge(player, aspects, false, PREFIX_REGISTRY.equals(prefix));
        return true;
    }
}
