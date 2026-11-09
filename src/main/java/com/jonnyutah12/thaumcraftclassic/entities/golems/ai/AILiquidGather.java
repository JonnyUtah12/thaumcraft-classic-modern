package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Decanting golem's "pump a fluid into my belly" goal (TC-M6-08-s-b2): the
 * 339-line gather goal — find a source, navigate, then fill the carried
 * {@link FluidStack} from a marked fluid handler, a marked fluid block, or (with
 * the Focal Wall upgrade, type {@code 5}) the connected fluid body behind the
 * marked block, nearest-valid-source-first.
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AILiquidGather.java}
 * (339 lines). Installed by {@code setupGolem()} for <b>core 5 (Decanting) at
 * priority 2</b> ({@code .../common/entities/golems/EntityGolemBase.java:258}:
 * {@code field_75776_a(2, new AILiquidGather(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AILiquidGather.java:39}) → {@code MOVE|JUMP};</li>
 *   <li>the work happens once the counter reaches {@code count >= 10} ticks
 *       ({@code :111}), and {@code canContinueToUse} gives up at
 *       {@code count >= 20} ({@code :92}); {@code isInterruptable} is false
 *       ({@code :95-97}) — the original's only goal family to opt out;</li>
 *   <li>the drain request is always {@code max - camt} ({@code :63, 130}) with
 *       {@code max = getFluidCarryLimit()} and {@code camt} the amount already
 *       carried;</li>
 *   <li>the sound threshold is {@code amount > 200} mB, volume
 *       {@code 0.2F * amount / max} ({@code :138-146});</li>
 *   <li>a fluid block yields {@code 1000} mB ({@code FluidStack(FLUID, 1000)},
 *       {@code :217}) and the watch clears at {@code >= getFluidCarryLimit()}
 *       for the handler arm ({@code :149}) and at
 *       {@code > getFluidCarryLimit() - 1000} for the block arms ({@code :204, 233});</li>
 *   <li>the Focal Wall arm ({@code getUpgradeAmount(5) > 0}, {@code :164-182})
 *       builds a flood-fill queue over the connected body with
 *       {@code pumpDist = getRange()²} ({@code :268}), sorted with
 *       {@code Collections.reverseOrder()} ({@code :273}) so the <b>farthest</b>
 *       valid source is taken first — the original drains a pool inward from its
 *       far edge. Kept exactly, not "fixed".</li>
 * </ul>
 *
 * <h2>The watch ({@code itemWatched})</h2>
 * The original parks "the fluid I set out to fetch" on the golem
 * ({@code EntityGolemBase.itemWatched}) and every liquid goal reads or clears it.
 * This tree's {@code EntityGolemBase} has no such field and this card may not
 * edit it, so the three liquid goals share {@link #WATCHED_FLUID} — a
 * per-golem map keyed on entity identity, cleared in {@link #stop()} exactly
 * where the original clears the field. DEVIATION (storage site only).
 *
 * <h2>Deviations / seams (each is at its site)</h2>
 * <ul>
 *   <li>1.7.10's {@code IFluidBlock} ({@code canDrain}/{@code drain}) has no
 *       1.21.1 type: the port reads the level's {@link FluidState}, a
 *       drainable fluid block is a <b>source</b> state of that fluid, one block
 *       is {@value FluidType#BUCKET_VOLUME} mB, and the drain removes the
 *       block.</li>
 *   <li>1.7.10's {@code world.getBlockMetadata(x,y,z) == 0} (a source) becomes
 *       {@code FluidState#isSource()}; the {@code Blocks.blockWater → Blocks.water}
 *       flowing-block aliasing collapses, since 1.21.1 has one fluid type with a
 *       source flag.</li>
 *   <li>{@code IFluidHandler} tiles become the NeoForge block capability, the
 *       same mapping {@link GolemHelper} uses.</li>
 *   <li>{@code updateCarried()} has no counterpart yet (TC-M6-08-s-c).</li>
 *   <li>The original's unused fields are named where they were dropped:
 *       {@code waterX/waterY/waterZ} ({@code :25-27}) stay as declared fields,
 *       {@code markerOrientation} ({@code :28}) is declared and never read in the
 *       original, so it is not carried over; the original's
 *       {@code cX/cY/cZ} home-container locals in {@code canUse}/{@code tick}
 *       ({@code :48-51, 112-116}) are computed and never read, so the port omits
 *       the arithmetic. {@code theWorld} ({@code :29, 38}) is the goal's captured
 *       level, which {@code GolemGoal} does the same way.</li>
 * </ul>
 */
public class AILiquidGather extends GolemGoal {

    /**
     * DEVIATION (storage site): the original's
     * {@code EntityGolemBase.itemWatched} — "the fluid this golem set out to
     * fetch", cleared when the belly is full or the goal resets. The field does
     * not exist in this tree's golem and this card may not add it, so the three
     * liquid goals ({@link AILiquidGather}, {@link AILiquidGoto},
     * {@link AILiquidEmpty}) share this per-golem map. Keyed on entity identity
     * (the golem has no {@code hashCode} override) in a {@link WeakHashMap} so a
     * despawned golem releases its entry.
     */
    private static final Map<EntityGolemBase, FluidStack> WATCHED_FLUID = new WeakHashMap<>();

    /**
     * The golem's current watch, or {@code null} (the original's
     * {@code EntityGolemBase.itemWatched}, which was a public field — this
     * accessor is its faithful analogue).
     */
    public static FluidStack watchedFluid(final EntityGolemBase golem) {
        return WATCHED_FLUID.get(golem);
    }

    /** Set the golem's watch (the original's {@code golem.itemWatched = stack}). */
    public static void watchFluid(final EntityGolemBase golem, final FluidStack target) {
        if (target == null) {
            WATCHED_FLUID.remove(golem);
        } else {
            WATCHED_FLUID.put(golem, target);
        }
    }

    /** The watch list, for the runtime and the tests. */
    public static int watchedGolemCount() {
        return WATCHED_FLUID.size();
    }

    /** Original {@code waterX/waterY/waterZ} ({@code AILiquidGather.java:25-27}): declared by the original and never read. */
    private int waterX;
    private int waterY;
    private int waterZ;

    /** Original {@code private float pumpDist} — the flood-fill bound, set to {@code range²} ({@code :30, 268}). */
    private float pumpDist = 0.0F;

    /** Original {@code int count = 0} — the tick counter ({@code :31}). */
    int count = 0;

    /** Original {@code queue}: per marked block, the connected sources, farthest first ({@code :32, 273-274}). */
    private final Map<BlockPos, List<SourceBlock>> queue = new HashMap<>();

    /** Original {@code cache}: the visited set of the flood fill ({@code :33, 279-283}). */
    private final List<BlockPos> cache = new ArrayList<>();

    /** Original {@code origin}: the marked block the current flood fill started at ({@code :34, 270}). */
    private BlockPos origin = null;

    public AILiquidGather(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AILiquidGather.java:39) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /** The counter is tick-based ({@code count >= 10} / {@code count < 20}), so 1.21.1 must call {@code tick()} every tick. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Original {@code func_75250_a} ({@code AILiquidGather.java:42-89}). */
    @Override
    public boolean canUse() {
        // Installed only for core 5 (setupGolem's switch) — reproduce that gate.
        if (!coreIs(5)) {
            return false;
        }
        final List<FluidStack> fluids = GolemHelper.getMissingLiquids(this.golem);
        if (fluids == null) {
            return false;
        }
        // :46 — a set-out-to-fetch fluid, something the home container wants, and an idle navigation.
        if (watchedFluid(this.golem) == null || fluids.isEmpty()
                || !this.golem.getNavigation().isDone()) {
            return false;
        }
        final int camt = carriedAmount();
        final int max = this.golem.getFluidCarryLimit();

        for (final FluidStack fluid : fluids) {
            // :60-68 — a marked fluid handler adjacent to the golem that can yield it.
            for (final Marker marker : GolemHelper.getMarkedFluidHandlersAdjacentToGolem(fluid, this.level(),
                    this.golem)) {
                final IFluidHandler handler = handlerAt(marker);
                if (handler == null) {
                    continue;
                }
                final FluidStack fs = handler.drain(new FluidStack(fluid.getFluid(), max - camt),
                        IFluidHandler.FluidAction.SIMULATE);
                if (fs != null && fs.getAmount() > 0) {
                    return true;
                }
            }
            // :70-82 — a marked non-container block that is the fluid itself.
            for (final BlockPos loc : GolemHelper.getMarkedBlocksAdjacentToGolem(this.level(), this.golem,
                    (byte) -1)) {
                // DEVIATION: `FluidRegistry.getFluid(fluid.fluidID).getBlock() == bi`
                // (:72) reads the block's fluid in 1.21.1 — a fluid block is a
                // FluidState, and fluid ids are registry entries.
                if (!isFluidBlockAt(fluid, loc)) {
                    continue;
                }
                if (isDrainableFluidBlock(loc)) {
                    // :73-76 — the IFluidBlock arm: simulate the drain, accept when it
                    // fits in the remaining carry.
                    final FluidStack fs = drainFluidBlock(loc, IFluidHandler.FluidAction.SIMULATE);
                    return fs != null && fs.getAmount() <= max - camt;
                }
                if (fluid.getFluid() == Fluids.WATER || fluid.getFluid() == Fluids.LAVA) {
                    // :78-79 — the 1.7.10 rule for vanilla fluids: metadata 0 is a
                    // source block. DEVIATION: the metadata test becomes isSource().
                    return isSourceAt(loc);
                }
            }
        }
        return false;
    }

    /** Original {@code func_75253_b} ({@code AILiquidGather.java:91-93}). */
    @Override
    public boolean canContinueToUse() {
        return this.count < 20 && watchedFluid(this.golem) != null;
    }

    /** Original {@code func_75252_g} → false ({@code AILiquidGather.java:95-97}). */
    @Override
    public boolean isInterruptable() {
        return false;
    }

    /** Original {@code func_75249_e} ({@code AILiquidGather.java:99-101}): the counter restarts. */
    @Override
    public void start() {
        this.count = 0;
    }

    /** Original {@code func_75251_c} ({@code AILiquidGather.java:103-107}). */
    @Override
    public void stop() {
        this.count = 0;
        watchFluid(this.golem, null); // the original's `theGolem.itemWatched = null`
        super.stop();
    }

    /**
     * Original {@code func_75246_d} ({@code AILiquidGather.java:109-245}): the
     * ten-tick wind-up, then the fill pass — marked handlers first, then marked
     * blocks, with the Focal Wall queue redirecting the block arm into the
     * connected body.
     */
    @Override
    public void tick() {
        this.count++;
        if (this.count < 10) { // :111
            return;
        }
        final int camt = carriedAmount();
        final int max = this.golem.getFluidCarryLimit();
        final List<FluidStack> fluids = GolemHelper.getMissingLiquids(this.golem);
        if (fluids == null) {
            return;
        }
        for (final FluidStack fluidstack : fluids) {
            // :126-156 — the handler arm: drain for real, merge, sound, full check.
            for (final Marker marker : GolemHelper.getMarkedFluidHandlersAdjacentToGolem(fluidstack,
                    this.level(), this.golem)) {
                final IFluidHandler handler = handlerAt(marker);
                if (handler == null) {
                    continue;
                }
                final FluidStack fs = handler.drain(new FluidStack(fluidstack.getFluid(), max - camt),
                        IFluidHandler.FluidAction.EXECUTE);
                if (fs == null || fs.getAmount() <= 0) {
                    continue;
                }
                addFluidToCarried(fs);
                if (fs.getAmount() > 200) {
                    playSwim(0.2F * fs.getAmount() / max); // :138-146
                }
                // DEVIATION: updateCarried() (:148) — see AIEssentiaEmpty's note.
                if (carriedAmount() >= this.golem.getFluidCarryLimit()) {
                    watchFluid(this.golem, null); // :149-151
                }
                this.count = 0;
            }

            // :158-241 — the block arm.
            for (final BlockPos marked : GolemHelper.getMarkedBlocksAdjacentToGolem(this.level(), this.golem,
                    (byte) -1)) {
                int i = marked.getX();
                int j = marked.getY();
                int k = marked.getZ();

                if (this.golem.getUpgradeAmount(5) > 0) {
                    // :164-182 — the Focal Wall arm: rebuild the connected queue for
                    // this marked block, then pop entries until a valid source is
                    // found and work on that position instead.
                    if (!this.queue.containsKey(marked) || this.queue.get(marked).isEmpty()) {
                        rebuildQueue(marked, fluidstack.getFluid());
                    }
                    if (this.queue.containsKey(marked) && !this.queue.get(marked).isEmpty()) {
                        final List<SourceBlock> t = new ArrayList<>(this.queue.get(marked));
                        BlockPos current;
                        do {
                            current = t.get(0).loc;
                            i = current.getX();
                            j = current.getY();
                            k = current.getZ();
                            t.remove(0);
                        } while (!t.isEmpty() && !validFluidBlock(fluidstack.getFluid(), current));
                        this.queue.put(marked, t);
                    }
                }

                final BlockPos loc = new BlockPos(i, j, k);
                if (!isFluidBlockAt(fluidstack, loc)) { // :184
                    continue;
                }
                if (isDrainableFluidBlock(loc)) {
                    // :185-209 — drain the fluid block itself, once the simulated
                    // amount fits the remaining carry.
                    final FluidStack fs = drainFluidBlock(loc, IFluidHandler.FluidAction.SIMULATE);
                    if (fs != null && fs.getAmount() <= max - camt) {
                        removeFluidBlock(loc); // the original's drain(..., true) + markBlockForUpdate
                        addFluidToCarried(fs);
                        playSwim(0.2F); // :196-202
                        // DEVIATION: updateCarried() (:203) — see AIEssentiaEmpty's note.
                        if (carriedAmount() > this.golem.getFluidCarryLimit() - 1000) {
                            watchFluid(this.golem, null); // :204-206
                        }
                        this.count = 0;
                    }
                } else if (fluidstack.getFluid() == Fluids.WATER || fluidstack.getFluid() == Fluids.LAVA) {
                    // :210-239 — the 1.7.10 vanilla-fluid rule: a metadata-0 (source)
                    // water/lava block yields a full bucket and is NOT removed. Kept
                    // as the original has it, including that it is unreachable for
                    // vanilla fluids in practice: the arm above already accepts a
                    // source, so this arm can only be reached by a non-source state,
                    // which fails the isSource test below.
                    if (isSourceAt(loc)) { // :213-216
                        final FluidStack fs = new FluidStack(fluidstack.getFluid(), FluidType.BUCKET_VOLUME); // :217
                        addFluidToCarried(fs);
                        markForUpdate(loc); // :224
                        playSwim(0.2F); // :225-231
                        // DEVIATION: updateCarried() (:232) — see AIEssentiaEmpty's note.
                        if (carriedAmount() > this.golem.getFluidCarryLimit() - 1000) {
                            watchFluid(this.golem, null); // :233-235
                        }
                        this.count = 0;
                    }
                }
            }
        }
    }

    /**
     * Original {@code validFluidBlock(Fluid, x, y, z)} ({@code AILiquidGather.java:247-265}):
     * the block must be the fluid, and either drainable (a source, whose
     * simulated drain is non-null) or a water/lava source block (metadata 0).
     */
    private boolean validFluidBlock(final Fluid fluid, final BlockPos pos) {
        final FluidState state = this.level().getFluidState(pos);
        if (state.isEmpty() || state.getType() != fluid) {
            return false;
        }
        if (isDrainableFluidBlock(pos)
                && drainFluidBlock(pos, IFluidHandler.FluidAction.SIMULATE) != null) {
            return true;
        }
        return (fluid == Fluids.WATER || fluid == Fluids.LAVA) && state.isSource();
    }

    /**
     * Original {@code rebuildQueue(loc, fluid)} ({@code AILiquidGather.java:267-275}):
     * bound {@code pumpDist = getRange()²}, flood fill from the marked block,
     * sort farthest-first, store per marked block.
     */
    private void rebuildQueue(final BlockPos loc, final Fluid fluid) {
        this.pumpDist = this.golem.getRange() * this.golem.getRange(); // :268
        this.cache.clear();
        this.origin = loc;
        final List<SourceBlock> sources = new ArrayList<>();
        getConnectedFluidBlocks(loc.getX(), loc.getY(), loc.getZ(), fluid, sources);
        Collections.sort(sources, Collections.reverseOrder()); // :273 — farthest first
        this.queue.put(loc, sources);
    }

    /**
     * Original {@code getConnectedFluidBlocks} ({@code AILiquidGather.java:277-319}):
     * the 26-neighbour flood fill, bounded by {@link #pumpDist} and the
     * {@link #cache} visited set, recording every valid source it touches. The
     * original swallows every exception out of the recursion ({@code :317-318});
     * the port keeps that, since the fill walks unloaded chunks.
     */
    private void getConnectedFluidBlocks(final int x, final int y, final int z, final Fluid fluid,
                                         final List<SourceBlock> sources) {
        try {
            final BlockPos start = new BlockPos(x, y, z);
            if (this.cache.contains(start)) { // :279-281
                return;
            }
            this.cache.add(start); // :283

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    for (int c = -1; c <= 1; c++) {
                        if (a == 0 && b == 0 && c == 0) {
                            continue;
                        }
                        final int xx = x + a;
                        final int yy = y + b;
                        final int zz = z + c;
                        final BlockPos cc = new BlockPos(xx, yy, zz);
                        // DEVIATION: the original's func_82371_e(Vec3) returned a
                        // float; 1.21.1's BlockPos#distSqr returns a double, so the
                        // value is narrowed back to keep the original's precision
                        // against the float pumpDist (:293-294).
                        final float dist = (float) this.origin.distSqr(cc); // :293 (squared, as the original used it)
                        if (dist > this.pumpDist) { // :294
                            continue;
                        }
                        // DEVIATION: the original maps the flowing blocks
                        // (Blocks.field_150356_k → field_150353_l, field_150358_i →
                        // field_150355_j, :296-302) onto their source blocks; 1.21.1
                        // has one fluid type for both and flags the source on the
                        // state, so the aliasing collapses into this fluid test.
                        final FluidState state = this.level().getFluidState(cc);
                        if (!state.isEmpty() && state.getType() == fluid) { // :304-305
                            if (validFluidBlock(fluid, cc)) {
                                sources.add(new SourceBlock(cc, dist)); // :307
                            }
                            getConnectedFluidBlocks(xx, yy, zz, fluid, sources); // :310
                        }
                    }
                }
            }
        } catch (final Exception ignored) {
            // The original's catch-all (:317-318) — kept verbatim.
        }
    }

    /** The amount the golem is carrying, 0 when it carries nothing ({@code :53-55, 117-120}). */
    private int carriedAmount() {
        final FluidStack carried = this.golem.getFluidCarried();
        return carried == null ? 0 : carried.getAmount();
    }

    /**
     * The original's merge ({@code :132-136, 189-193, 218-222}): add the drained
     * amount to the carried stack, or adopt a copy of it. Kept verbatim,
     * including that it adds the amount even if the two stacks hold <em>different</em>
     * fluids — the original never checks, and the marker filters make a mismatch
     * a rarity.
     */
    private void addFluidToCarried(final FluidStack drained) {
        final FluidStack carried = this.golem.getFluidCarried();
        if (carried != null) {
            carried.setAmount(carried.getAmount() + drained.getAmount());
        } else {
            this.golem.setFluidCarried(drained.copy());
        }
    }

    /**
     * Resolve the marked block's fluid handler. The original tested
     * {@code tile instanceof IFluidHandler} ({@code :62, 128}); the port resolves
     * NeoForge's block capability on the marker's face, the mapping
     * {@link GolemHelper} uses throughout.
     */
    private IFluidHandler handlerAt(final Marker marker) {
        final BlockEntity tile = this.level().getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
        if (tile == null) {
            return null;
        }
        // SEAM: this tree registers no block fluid-handler capability anywhere (no
        // RegisterCapabilitiesEvent for a fluid tank), so on today's shipped blocks
        // this resolves to null and the handler arm — the original's
        // `te instanceof IFluidHandler` drain (:62-67, :128-136) — cannot fire. It
        // is live for any mod tile that registers the capability, e.g. a tank.
        return this.level().getCapability(Capabilities.FluidHandler.BLOCK, tile.getBlockPos(),
                GolemHelper.directionOf(marker.side));
    }

    /** {@code FluidRegistry.getFluid(id).getBlock() == bi} ({@code :72, 184}) in 1.21.1 terms. */
    private boolean isFluidBlockAt(final FluidStack fluid, final BlockPos pos) {
        final FluidState state = this.level().getFluidState(pos);
        return !state.isEmpty() && state.getType() == fluid.getFluid();
    }

    /**
     * DEVIATION: 1.7.10's {@code IFluidBlock.canDrain(world,x,y,z)} — for every
     * shipped implementation that is "the block is a full source" — is
     * {@code FluidState#isSource()} on the state at the position in 1.21.1.
     */
    private boolean isDrainableFluidBlock(final BlockPos pos) {
        return isSourceAt(pos);
    }

    /** {@code FluidState#isSource()} for the state at the position. */
    private boolean isSourceAt(final BlockPos pos) {
        final FluidState state = this.level().getFluidState(pos);
        return !state.isEmpty() && state.isSource();
    }

    /**
     * DEVIATION: {@code ((IFluidBlock) bi).drain(world,x,y,z, simulate)}
     * ({@code :74, 186, 253}). 1.21.1 has no {@code IFluidBlock}; a full source
     * block is one bucket, and the simulate arm must not touch the level, so the
     * port reports {@link FluidType#BUCKET_VOLUME} without removing anything.
     */
    private FluidStack drainFluidBlock(final BlockPos pos, final IFluidHandler.FluidAction action) {
        final FluidState state = this.level().getFluidState(pos);
        if (state.isEmpty() || !state.isSource()) {
            return null;
        }
        return new FluidStack(state.getType(), FluidType.BUCKET_VOLUME);
    }

    /**
     * The "drain for real" half of the original's
     * {@code ((IFluidBlock) bi).drain(world,x,y,z, true)} followed by
     * {@code world.markBlockForUpdate} ({@code :188, 195}).
     */
    private void removeFluidBlock(final BlockPos pos) {
        final BlockState state = this.level().getBlockState(pos);
        this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        markForUpdate(pos, state);
    }

    /** {@code world.markBlockForUpdate(x,y,z)} ({@code :224}). */
    private void markForUpdate(final BlockPos pos) {
        markForUpdate(pos, this.level().getBlockState(pos));
    }

    /** {@code world.markBlockForUpdate(x,y,z)} with the state the caller already read. */
    private void markForUpdate(final BlockPos pos, final BlockState state) {
        this.level().sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    /**
     * The original's {@code world.playSound(golem, "game.neutral.swim", volume,
     * 1.0F + (rand - rand) * 0.3F)} ({@code :196-202}). DEVIATION: the id is
     * {@link SoundEvents#GENERIC_SWIM} in 1.21.1.
     */
    private void playSwim(final float volume) {
        final float pitch = 1.0F + (this.level().getRandom().nextFloat()
                - this.level().getRandom().nextFloat()) * 0.3F;
        this.level().playSound(null, this.golem, SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL, volume, pitch);
    }

    /**
     * Original {@code AILiquidGather.SourceBlock} ({@code :321-338}): a source
     * position with its squared distance from the marked block, ordered so that
     * {@code Collections.reverseOrder()} puts the <em>farthest</em> first — the
     * comparison is the original's, verbatim.
     */
    private static final class SourceBlock implements Comparable<SourceBlock> {

        /** Original {@code ChunkCoordinates loc} ({@code :322}). */
        private final BlockPos loc;

        /** Original {@code float dist} ({@code :323}) — squared, as {@code func_82371_e} returns. */
        private final float dist;

        private SourceBlock(final BlockPos loc, final float dist) {
            this.loc = loc;
            this.dist = dist;
        }

        /** Original {@code compareTo} ({@code :330-332}) — ascending by distance. */
        @Override
        public int compareTo(final SourceBlock target) {
            return target.dist < this.dist ? 1 : (target.dist > this.dist ? -1 : 0);
        }
    }
}
