package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * The live behaviour of one vis node — the data-driven port of the original
 * {@code TileNode} update loop (reference/decompiled
 * {@code thaumcraft/common/tiles/TileNode.java}, TC 4.2.3.5).
 *
 * <p>The original node is a tile entity ticking inside its chunk. This port has no
 * node block entity (generation stores records only, see
 * {@code worldgen/NodeGeneration}), so the behaviour runs against the
 * {@link WorldMagicData.Node} record: {@code world/NodeTickDriver} materialises a
 * runtime per node in a loaded tracked chunk, ticks it in the same order the
 * original {@code updateEntity} does, and writes the state back through
 * {@link WorldMagicData#replaceNode}. That is the same shape as the sibling
 * TC-M5-03 flux engine ({@code world/FluxTickHandler}): data-driven, no blocks.
 *
 * <p>Everything the original persists is carried by the record: the aspect
 * container ({@code aspects}), the per-aspect vis base ({@code aspectsBase}) and
 * the offline catch-up anchor ({@code lastActive}). The transient tick counters
 * ({@code count}, {@code regeneration}, {@code wait}, {@code catchUp},
 * {@code nodeLock}) are deliberately NOT persisted, exactly as in the original —
 * they restart when a chunk loads (TileNode L56-58, L946-962).
 *
 * <p>Parity deviations, each pinned by tests:
 * <ul>
 *   <li>Block gates ({@code blockAiry} at the node, {@code blockStoneDevice}
 *       meta 9/10 below it, {@code blockMagicalLog}) become {@link Context}
 *       probes; the driver answers them from block tags so M6 device blocks can
 *       opt in without touching this class.</li>
 *   <li>Biome painting (taint/eerie/magical forest) and the dimension/biome
 *       blacklists are {@link Context} probes; unregistered biomes read as
 *       "nothing to paint", so the behaviour is inert until the biome cards land.</li>
 *   <li>The {@code Config.hardNode} entity effects (hungry-node damage/kill→scan
 *       feed, entity pull, giant brainsy zombie spawn, taint fibre spread) and the
 *       unstable-node aspect orb spawn are entity-system seams: the calls are kept
 *       as {@link Context} hooks but the driver no-ops them until the M6 entity
 *       cards provide the entities.</li>
 *   <li>{@code takeRandomPrimalFromSource} on an empty primal list returns null
 *       where the original would throw {@code IllegalArgumentException} from
 *       {@code nextInt(0)} — same observable outcome (no orb), no crash.</li>
 *   <li>The pre-4.2 legacy {@code nodeVisBase} short fallback in read (TileNode
 *       L260-266) is not ported: the port has no such saves.</li>
 * </ul>
 */
public final class NodeRuntime implements INode {

    // ------------------------------------------------------------- original constants

    /** Original regeneration periods in game ticks (TileNode L271-283, L661-674). */
    public static final int REGEN_BASE = 600;
    public static final int REGEN_BRIGHT = 400;
    public static final int REGEN_PALE = 900;
    public static final int REGEN_FADING = 0;

    /** Stabilizer lock multipliers on the regen period (TileNode L676-682). */
    public static final int LOCK1_REGEN_MULTIPLIER = 2;
    public static final int LOCK2_REGEN_MULTIPLIER = 20;

    /** Offline catch-up window: 75 ms of wall time per regen tick (TileNode L286, L688). */
    public static final long CATCHUP_MS_PER_REGEN_TICK = 75L;

    /** Death-spiral check period (TileNode L707). */
    public static final int DEATH_SPIRAL_TICKS = 1200;

    /** Stability / taint-conversion check period (TileNode L792, L804). */
    public static final int STABILITY_TICKS = 100;

    /** Taint painting / dark / pure / hungry check period (TileNode L774, L837, L864, L913). */
    public static final int BIOME_EFFECT_TICKS = 50;

    /** Lock re-probe period (TileNode L947). */
    public static final int LOCK_PROBE_TICKS = 50;

    /** Discharge attempt periods per modifier (TileNode L585). */
    public static final int DISCHARGE_INTERVAL_DEFAULT = 2;
    public static final int DISCHARGE_INTERVAL_SHINY = 1;
    public static final int DISCHARGE_INTERVAL_PALE = 3;

    /** Hungry-node block-break reach: squared distance from node centre (TileNode L925). */
    public static final double HUNGRY_REACH_SQUARED = 256.0;

    /** Breakable hardness window for the hungry node (TileNode L931-933). */
    public static final float HUNGRY_MIN_HARDNESS = 0.0F;
    public static final float HUNGRY_MAX_HARDNESS = 5.0F;

    /**
     * World-facing probes the behaviour needs. All defaults are inert so unit
     * tests only script the seam a given test exercises.
     */
    public interface Context {

        /** The runtime at that exact position, or null (original loaded-TE lookup, TileNode L600-608). */
        NodeRuntime neighborAt(int x, int y, int z);

        /** 0 none, 1 basic stabilizer, 2 advanced stabilizer below (x,y,z) (TileNode L949-957). */
        default int stabilizerAt(int x, int y, int z) {
            return 0;
        }

        default boolean isTaintBiomeAt(int x, int z) {
            return false;
        }

        /** False when no taint biome is registered — painting is then skipped entirely. */
        default boolean hasTaintBiome() {
            return false;
        }

        default void paintTaintBiome(int x, int y, int z) {
        }

        default boolean isEerieBiomeAt(int x, int z) {
            return false;
        }

        default boolean hasEerieBiome() {
            return false;
        }

        default void paintEerieBiome(int x, int y, int z) {
        }

        default boolean isMagicalForestBiomeAt(int x, int z) {
            return false;
        }

        default boolean hasMagicalForestBiome() {
            return false;
        }

        default void paintMagicalForestBiome(int x, int y, int z) {
        }

        /**
         * Combined dimension/biome blacklist gate the original applies before dark
         * and pure biome painting (TileNode L831-835, L855-861): overworld-like
         * dimensions with the node's biome not blacklisted.
         */
        default boolean biomePaintingAllowed() {
            return true;
        }

        /** True when the block at (x,y,z) is a magical log under a pure node (TileNode L846). */
        default boolean isMagicalLogAt(int x, int y, int z) {
            return false;
        }

        /** Top non-air block y in the column (original world.getHeight, TileNode L919-921). */
        default int surfaceHeight(int x, int z) {
            return 0;
        }

        default boolean isAir(int x, int y, int z) {
            return true;
        }

        /** Block hardness, negative for unbreakable (TileNode L931). */
        default float blockHardness(int x, int y, int z) {
            return -1.0F;
        }

        /**
         * Ray from the node centre to the target block centre ignoring the source
         * block; returns the hit block position or null (TileNode L923-925).
         */
        default int[] rayTraceHit(int x0, int y0, int z0, int x1, int y1, int z1) {
            return null;
        }

        default void breakBlock(int x, int y, int z) {
        }

        /** Unstable-node primal orb spawn hook (TileNode L812-818); no-op until M6 entities. */
        default void spawnAspectOrb(int x, int y, int z, Aspect aspect, int amount) {
        }
    }

    // -------------------------------------------------------------------- identity

    private final int x;
    private final int y;
    private final int z;
    private final int intensity;

    private NodeType type;
    private NodeModifier modifier;

    // ------------------------------------------------- persisted node state (record)

    /** Original {@code aspects} container (TileNode L51). */
    private final AspectList aspects = new AspectList();
    /** Original {@code aspectsBase} vis base (TileNode L52). */
    private final AspectList aspectsBase = new AspectList();
    /** Original {@code lastActive} wall-clock anchor (TileNode L60). */
    private long lastActive;

    // ------------------------------------------------ transient tick state (not persisted)

    /** Original {@code count} — ticks since the chunk loaded (TileNode L56, L111). */
    private int count;
    /** Original {@code regeneration} period in ticks; -1 = recompute (TileNode L57). */
    private int regeneration = -1;
    /** Original {@code wait} — discharge-induced production pause (TileNode L58, L760-762). */
    private int wait;
    /** Original {@code catchUp} — offline production owed (TileNode L59, L285-289). */
    private boolean catchUp;
    /** Original {@code nodeLock} from the stabilizer below (TileNode L61, L946-962). */
    private byte nodeLock;

    /** Set when record state changed and must be written back (original markDirty). */
    private boolean dirty;
    /** Set when the node died and its record must be removed (original tile removal). */
    private boolean dead;

    private NodeRuntime(final int x, final int y, final int z, final int intensity) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.intensity = intensity;
    }

    // ------------------------------------------------------------------ construction

    /**
     * Materialises the runtime from a stored record. {@code resolver} maps aspect
     * tags to {@link Aspect} instances — the driver passes {@code Aspect::getAspect};
     * unit tests pass a definition-table resolver (same seam as
     * {@code visnet/AmbientVis}), since the aspect registry is not bootstrapped
     * outside a game.
     *
     * <p>Reproduces the original read path (TileNode L247-289): a missing
     * {@code aspectsBase} copies the container (setAspects copy, L171-174), and
     * the catch-up flag is set from the persisted {@code lastActive} using the
     * modifier-only regen period — the stabilizer multiplier is applied later, on
     * the first lock probe, exactly as the original's load-then-checkLock order.
     */
    public static NodeRuntime fromRecord(final WorldMagicData.Node record,
            final Function<String, Aspect> resolver, final LongSupplier clock) {
        final NodeRuntime node = new NodeRuntime(record.x(), record.y(), record.z(), record.intensity());
        node.type = NodeType.fromId(record.type());
        node.modifier = NodeModifier.fromId(record.modifier());
        addAll(node.aspects, record.aspects(), resolver);
        if (record.aspectsBase() == null) {
            node.aspectsBase.add(node.aspects);
        } else {
            addAll(node.aspectsBase, record.aspectsBase(), resolver);
        }
        node.lastActive = record.lastActive();
        node.regeneration = regenerationFor(node.modifier);
        final long inc = (long) node.regeneration * CATCHUP_MS_PER_REGEN_TICK;
        if (node.regeneration > 0 && node.lastActive > 0L && clock.getAsLong() > node.lastActive + inc) {
            node.catchUp = true;
        }
        return node;
    }

    private static void addAll(final AspectList target, final Map<String, Integer> source,
            final Function<String, Aspect> resolver) {
        for (final Map.Entry<String, Integer> entry : source.entrySet()) {
            final Aspect aspect = resolver.apply(entry.getKey());
            if (aspect != null) {
                target.add(aspect, entry.getValue());
            }
        }
    }

    /** Writes the runtime state back as a record (original writeToNBT counterpart). */
    public WorldMagicData.Node toRecord() {
        return new WorldMagicData.Node(x, y, z, type.id(), modifier == null ? null : modifier.id(),
                intensity, toMap(aspects), toMap(aspectsBase), lastActive);
    }

    private static Map<String, Integer> toMap(final AspectList list) {
        final Map<String, Integer> map = new LinkedHashMap<>();
        for (final Aspect aspect : list.getAspects()) {
            map.put(aspect.getTag(), list.getAmount(aspect));
        }
        return map;
    }

    // ------------------------------------------------------------------------ ticking

    /**
     * One game tick, in the original {@code updateEntity} server order (TileNode
     * L105-124): count++, checkLock, discharge, recharge, taint, stability, dark,
     * pure, hungry. The client-only branches (particle FX, dark-node compass
     * painting) and the {@code Config.hardNode} entity effects are not run here;
     * see the class parity notes.
     *
     * @return true when any state changed (the original's {@code change} flag)
     */
    public boolean tick(final Context context, final Random random, final LongSupplier clock) {
        count++;
        checkLock(context);
        boolean change = false;
        change |= handleDischarge(context, random);
        change |= handleRecharge(context, random, clock);
        change |= handleTaintNode(context, random);
        change |= handleNodeStability(context, random);
        change |= handleDarkNode(context, random);
        change |= handlePureNode(context, random);
        change |= handleHungryNode(context, random);
        if (change) {
            dirty = true;
        }
        return change;
    }

    /** Original {@code checkLock} (TileNode L946-962); the blockAiry gate is implied by record identity. */
    void checkLock(final Context context) {
        if ((count <= 1 || count % LOCK_PROBE_TICKS == 0) && y > 0) {
            final byte oldLock = nodeLock;
            nodeLock = (byte) context.stabilizerAt(x, y - 1, z);
            if (oldLock != nodeLock) {
                regeneration = -1;
            }
        }
    }

    /** Original {@code handleDischarge} (TileNode L579-658). */
    boolean handleDischarge(final Context context, final Random random) {
        if (nodeLock == 1 || modifier == NodeModifier.FADING) {
            return false;
        }
        final boolean shiny = type == NodeType.HUNGRY || modifier == NodeModifier.BRIGHT;
        final int inc = modifier == null ? DISCHARGE_INTERVAL_DEFAULT
                : (shiny ? DISCHARGE_INTERVAL_SHINY : (modifier == NodeModifier.PALE ? DISCHARGE_INTERVAL_PALE : DISCHARGE_INTERVAL_DEFAULT));
        if (count % inc != 0) {
            return false;
        }
        final int dx = random.nextInt(5) - random.nextInt(5);
        final int dy = random.nextInt(5) - random.nextInt(5);
        final int dz = random.nextInt(5) - random.nextInt(5);
        if (modifier == NodeModifier.PALE && random.nextBoolean()) {
            return false;
        }
        if (dx == 0 && dy == 0 && dz == 0) {
            return false;
        }
        final NodeRuntime nd = context.neighborAt(x + dx, y + dy, z + dz);
        if (nd == null || nd.nodeLock > 0) {
            return false;
        }
        final int ndavg = (nd.getAspects().visSize() + nd.getAspectsBase().visSize()) / 2;
        final int thisavg = (getAspects().visSize() + getAspectsBase().visSize()) / 2;
        if (ndavg >= thisavg || nd.getAspects().size() <= 0) {
            return false;
        }
        final Aspect a = nd.getAspects().getAspects().get(random.nextInt(nd.getAspects().size()));
        boolean u = false;
        if (getAspects().getAmount(a) < getNodeVisBase(a) && nd.takeVis(a, 1)) {
            addToContainer(a, 1);
            u = true;
        } else if (nd.takeVis(a, 1)) {
            if (random.nextInt(1 + (int) (getNodeVisBase(a) / (shiny ? 1.5 : 1.0))) == 0) {
                aspectsBase.add(a, 1);
                if (modifier == NodeModifier.PALE && random.nextInt(100) == 0) {
                    modifier = null;
                    regeneration = -1;
                }
                if (random.nextInt(3) == 0) {
                    nd.setNodeVisBase(a, (short) (nd.getNodeVisBase(a) - 1));
                }
            }
            u = true;
        }
        if (u) {
            nd.wait = nd.regeneration / 2;
            nd.dirty = true;
            return true;
        }
        return false;
    }

    /** Original {@code handleRecharge} (TileNode L660-771). */
    boolean handleRecharge(final Context context, final Random random, final LongSupplier clock) {
        if (regeneration < 0) {
            regeneration = regenerationFor(modifier);
            if (nodeLock == 1) {
                regeneration *= LOCK1_REGEN_MULTIPLIER;
            }
            if (nodeLock == 2) {
                regeneration *= LOCK2_REGEN_MULTIPLIER;
            }
        }

        if (catchUp) {
            catchUp = false;
            final long ct = clock.getAsLong();
            final long inc = (long) regeneration * CATCHUP_MS_PER_REGEN_TICK;
            final int amt = inc > 0 ? (int) ((ct - lastActive) / inc) : 0;
            if (amt > 0) {
                for (int a = 0; a < Math.min(amt, aspectsBase.visSize()); a++) {
                    final AspectList al = productionCandidates();
                    if (al.size() > 0) {
                        addToContainer(al.getAspects().get(random.nextInt(al.size())), 1);
                    }
                }
            }
        }

        if (count % DEATH_SPIRAL_TICKS == 0) {
            for (final Aspect aspectx : getAspects().getAspects()) {
                if (getAspects().getAmount(aspectx) <= 0) {
                    setNodeVisBase(aspectx, (short) (getNodeVisBase(aspectx) - 1));
                    if (random.nextInt(20) == 0 || getNodeVisBase(aspectx) <= 0) {
                        getAspects().remove(aspectx);
                        if (random.nextInt(5) == 0) {
                            if (modifier == NodeModifier.BRIGHT) {
                                modifier = null;
                            } else if (modifier == null) {
                                modifier = NodeModifier.PALE;
                            }
                            if (modifier == NodeModifier.PALE && random.nextInt(5) == 0) {
                                modifier = NodeModifier.FADING;
                            }
                        }
                        nodeChange();
                        break;
                    }
                    nodeChange();
                }
            }
            if (getAspects().size() <= 0) {
                // Original removes the tile and its blockAiry/magicalLog block
                // (TileNode L749-758); the port drops the record instead.
                dead = true;
            }
        }

        if (wait > 0) {
            wait--;
        }

        if (regeneration > 0 && wait == 0 && count % regeneration == 0) {
            lastActive = clock.getAsLong();
            final AspectList al = productionCandidates();
            if (al.size() > 0) {
                addToContainer(al.getAspects().get(random.nextInt(al.size())), 1);
                return true;
            }
        }
        return false;
    }

    /** The original's candidate list: existing entries below their vis base (TileNode L765-769). */
    private AspectList productionCandidates() {
        final AspectList al = new AspectList();
        for (final Aspect aspect : getAspects().getAspects()) {
            if (getAspects().getAmount(aspect) < getNodeVisBase(aspect)) {
                al.add(aspect, 1);
            }
        }
        return al;
    }

    private static int regenerationFor(final NodeModifier modifier) {
        if (modifier == null) {
            return REGEN_BASE;
        }
        switch (modifier) {
            case BRIGHT:
                return REGEN_BRIGHT;
            case PALE:
                return REGEN_PALE;
            case FADING:
            default:
                return REGEN_FADING;
        }
    }

    /** Original {@code handleTaintNode} (TileNode L773-801); fibre spread deferred to M5-04. */
    boolean handleTaintNode(final Context context, final Random random) {
        if (type == NodeType.TAINTED) {
            if (count % BIOME_EFFECT_TICKS == 0 && context.hasTaintBiome()) {
                final int tx = x + random.nextInt(8) - random.nextInt(8);
                final int tz = z + random.nextInt(8) - random.nextInt(8);
                if (!context.isTaintBiomeAt(tx, tz)) {
                    context.paintTaintBiome(tx, y, tz);
                }
            }
            return false;
        }
        if (type != NodeType.PURE && count % STABILITY_TICKS == 0
                && context.hasTaintBiome() && context.isTaintBiomeAt(x, z)
                && random.nextInt(500) == 0) {
            type = NodeType.TAINTED;
            nodeChange();
            return true;
        }
        return false;
    }

    /** Original {@code handleNodeStability} (TileNode L803-828). */
    boolean handleNodeStability(final Context context, final Random random) {
        if (count % STABILITY_TICKS != 0) {
            return false;
        }
        boolean change = false;
        if (type == NodeType.UNSTABLE && random.nextBoolean()) {
            if (nodeLock == 0) {
                final Aspect aspect = takeRandomPrimalFromSource(random);
                if (aspect != null) {
                    context.spawnAspectOrb(x, y, z, aspect, 1);
                    change = true;
                }
            } else if (random.nextInt(10000 / nodeLock) == 42) {
                type = NodeType.NORMAL;
                change = true;
            }
        }
        if (modifier == NodeModifier.FADING && nodeLock > 0
                && random.nextInt(12500 / nodeLock) == 69) {
            modifier = NodeModifier.PALE;
            change = true;
        }
        return change;
    }

    /** Original {@code handleDarkNode} (TileNode L854-910); GBZ spawn deferred to M6. */
    boolean handleDarkNode(final Context context, final Random random) {
        if (!context.biomePaintingAllowed() || type != NodeType.DARK || count % BIOME_EFFECT_TICKS != 0) {
            return false;
        }
        final int tx = x + random.nextInt(12) - random.nextInt(12);
        final int tz = z + random.nextInt(12) - random.nextInt(12);
        if (context.hasEerieBiome() && !context.isEerieBiomeAt(tx, tz)) {
            context.paintEerieBiome(tx, y, tz);
        }
        return false;
    }

    /** Original {@code handlePureNode} (TileNode L830-852). */
    boolean handlePureNode(final Context context, final Random random) {
        if (!context.biomePaintingAllowed() || type != NodeType.PURE || count % BIOME_EFFECT_TICKS != 0) {
            return false;
        }
        final int tx = x + random.nextInt(8) - random.nextInt(8);
        final int tz = z + random.nextInt(8) - random.nextInt(8);
        if (!context.hasMagicalForestBiome() || context.isMagicalForestBiomeAt(tx, tz)) {
            return false;
        }
        if (context.isTaintBiomeAt(tx, tz)) {
            context.paintMagicalForestBiome(tx, y, tz);
        } else if (context.isMagicalLogAt(x, y, z)) {
            context.paintMagicalForestBiome(tx, y, tz);
        }
        return false;
    }

    /** Original {@code handleHungryNodeSecond} (TileNode L912-940). */
    boolean handleHungryNode(final Context context, final Random random) {
        if (type != NodeType.HUNGRY || count % BIOME_EFFECT_TICKS != 0) {
            return false;
        }
        int tx = x + random.nextInt(16) - random.nextInt(16);
        int ty = y + random.nextInt(16) - random.nextInt(16);
        final int tz = z + random.nextInt(16) - random.nextInt(16);
        final int surface = context.surfaceHeight(tx, tz);
        if (ty > surface) {
            ty = surface;
        }
        final int[] hit = context.rayTraceHit(x, y, z, tx, ty, tz);
        if (hit == null || getDistanceTo(hit[0], hit[1], hit[2]) >= HUNGRY_REACH_SQUARED) {
            return false;
        }
        if (context.isAir(hit[0], hit[1], hit[2])) {
            return false;
        }
        final float hardness = context.blockHardness(hit[0], hit[1], hit[2]);
        if (hardness >= HUNGRY_MIN_HARDNESS && hardness < HUNGRY_MAX_HARDNESS) {
            context.breakBlock(hit[0], hit[1], hit[2]);
        }
        return false;
    }

    // ------------------------------------------------------------- container surface

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    /** Original {@code getAspectsBase} (TileNode L164-167). */
    public AspectList getAspectsBase() {
        return aspectsBase;
    }

    /**
     * Original {@code addToContainer} (TileNode L177-182): adds up to
     * {@code amount} without exceeding the vis base; returns the amount that did
     * not fit.
     */
    public int addToContainer(final Aspect aspect, final int amount) {
        int left = amount + aspects.getAmount(aspect) - aspectsBase.getAmount(aspect);
        left = Math.max(left, 0);
        aspects.add(aspect, amount - left);
        return left;
    }

    /**
     * Original {@code takeFromContainer} (TileNode L184-187): all-or-nothing.
     * Every original caller marks the tile dirty after a successful take, so the
     * dirty flag is set here.
     */
    public boolean takeVis(final Aspect aspect, final int amount) {
        final boolean taken = aspects.reduce(aspect, amount);
        if (taken) {
            dirty = true;
        }
        return taken;
    }

    /** {@link INode} surface (void); prefer {@link #takeVis} for the original's boolean. */
    @Override
    public void takeFromContainer(final Aspect aspect, final int amount) {
        takeVis(aspect, amount);
    }

    /**
     * Original {@code takeRandomPrimalFromSource} (TileNode L189-193): removes one
     * of a random primal the node holds, drawn from the world RNG the caller is
     * ticking with. Returns null when the node holds none (the original would
     * throw; same observable outcome).
     */
    public Aspect takeRandomPrimalFromSource(final Random random) {
        final List<Aspect> primals = aspects.getPrimalAspects();
        if (primals.isEmpty()) {
            return null;
        }
        final Aspect asp = primals.get(random.nextInt(primals.size()));
        return asp != null && aspects.reduce(asp, 1) ? asp : null;
    }

    /** Convenience overload using the scratch random; the tick loop passes its own. */
    public Aspect takeRandomPrimalFromSource() {
        return takeRandomPrimalFromSource(TICK_RANDOM);
    }

    /** Scratch random for the no-RNG-parameter convenience surface; the tick loop always passes one. */
    private static final Random TICK_RANDOM = new Random();

    /**
     * Original {@code chooseRandomFilteredFromSource} (TileNode L195-211): picks a
     * node aspect that the filter (amount &gt; 0 = "the wand has room") accepts;
     * {@code preserve} keeps at least one of every aspect in the node.
     */
    public Aspect chooseRandomFilteredFromSource(final AspectList filter, final boolean preserve,
            final Random random) {
        final int min = preserve ? 1 : 0;
        final List<Aspect> validAspects = new ArrayList<>();
        for (final Aspect prim : aspects.getAspects()) {
            if (filter.getAmount(prim) > 0 && aspects.getAmount(prim) > min) {
                validAspects.add(prim);
            }
        }
        if (validAspects.isEmpty()) {
            return null;
        }
        final Aspect asp = validAspects.get(random.nextInt(validAspects.size()));
        return asp != null && aspects.getAmount(asp) > min ? asp : null;
    }

    /** Convenience overload matching the original's world-rand call (TileNode L209). */
    public Aspect chooseRandomFilteredFromSource(final AspectList filter, final boolean preserve) {
        return chooseRandomFilteredFromSource(filter, preserve, TICK_RANDOM);
    }

    // ------------------------------------------------------------------ node identity

    public NodeType getNodeType() {
        return type;
    }

    public void setNodeType(final NodeType nodeType) {
        this.type = nodeType;
        this.dirty = true;
    }

    public NodeModifier getNodeModifier() {
        return modifier;
    }

    public void setNodeModifier(final NodeModifier nodeModifier) {
        this.modifier = nodeModifier;
        this.dirty = true;
    }

    /** Original {@code getNodeVisBase} (TileNode L233-235). */
    public int getNodeVisBase(final Aspect aspect) {
        return aspectsBase.getAmount(aspect);
    }

    /** Original {@code setNodeVisBase} (TileNode L237-245): merge up, reduce down. */
    public void setNodeVisBase(final Aspect aspect, final short nodeVisBase) {
        if (aspectsBase.getAmount(aspect) < nodeVisBase) {
            aspectsBase.merge(aspect, nodeVisBase);
        } else {
            aspectsBase.reduce(aspect, aspectsBase.getAmount(aspect) - nodeVisBase);
        }
        this.dirty = true;
    }

    /** Original {@code nodeChange} (TileNode L130-134): force regen recompute. */
    public void nodeChange() {
        this.regeneration = -1;
        this.dirty = true;
    }

    /** Original {@code getLock} (TileNode L942-944). */
    public byte getLock() {
        return nodeLock;
    }

    /** Original {@code getDistanceTo} — squared distance from the node centre (TileNode L139-145). */
    public double getDistanceTo(final double px, final double py, final double pz) {
        final double dx = x + 0.5 - px;
        final double dy = y + 0.5 - py;
        final double dz = z + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    // --------------------------------------------------------------- driver surface

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    /** True once the node died (empty container at a death-spiral check). */
    public boolean isDead() {
        return dead;
    }

    // ------------------------------------------------------------- test observation

    int count() {
        return count;
    }

    void setCount(final int value) {
        count = value;
    }

    void setRegeneration(final int value) {
        regeneration = value;
    }

    void setWait(final int value) {
        wait = value;
    }

    void setCatchUp(final boolean value) {
        catchUp = value;
    }

    int regeneration() {
        return regeneration;
    }

    int getWait() {
        return wait;
    }

    boolean catchUp() {
        return catchUp;
    }
}
