package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * The Alchemy golem's "draw essentia out of the tile at my home face" goal
 * (TC-M6-08-s-b2).
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java}
 * (157 lines). Installed by {@code setupGolem()} for <b>core 6 (Alchemy) at
 * priority 2</b> ({@code .../common/entities/golems/EntityGolemBase.java:263}:
 * {@code field_75776_a(2, new AIEssentiaGather(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEssentiaGather.java:27}) → {@code MOVE|JUMP};</li>
 *   <li>the retry cooldown: {@code canUse} sets {@code delay = now + 1000 ms}
 *       when it accepts a source ({@code :56}) and {@code startExecuting} sets
 *       {@code delay = now + 100 ms} after a take ({@code :153});</li>
 *   <li>the target must be within the <b>squared {@code 6.0}</b> of the block at
 *       {@code home - facing} ({@code :37});</li>
 *   <li><b>the carry clamp</b> — the amount drawn is
 *       {@code Math.min(qq, golem.getCarryLimit() - golem.getEssentiaAmount())}
 *       ({@code AIEssentiaGather.java:136-140}). That is the only bound in the
 *       original golem path: there is <b>no vis drain anywhere</b> in it, and this
 *       port adds none;</li>
 *   <li>the alembic scan walks {@code y + 5 … y + 0} upward and keeps the stack
 *       with the largest amount ({@code :61-79});</li>
 *   <li>the swim sound volume is {@code 0.05F} here, not the {@code 0.2F} the
 *       other fluid goals use ({@code :141-147}).</li>
 * </ul>
 *
 * <h2>Deviations / seams (each is at its site)</h2>
 * <ul>
 *   <li>{@code ForgeDirection.UNKNOWN} has no 1.21.1 counterpart: the "the tile's
 *       own contents" face is passed as {@code null}, which every
 *       {@link IEssentiaTransport} in this tree accepts and reads as its internal
 *       aspect ({@code TileJarFillable.getEssentiaType} ignores the face).</li>
 *   <li>{@code TileAlembic} and {@code TileEssentiaReservoir} are not shipped in
 *       this tree → their arms stay as named seams.</li>
 *   <li>The original's {@code taken == 0} branch clears {@code essentia} and
 *       leaves {@code essentiaAmount} as it was ({@code :149-151}); the port keeps
 *       that quirk through {@link EntityGolemBase#setEssentia(Aspect, int)}.</li>
 *   <li>{@code updateCarried()} has no counterpart yet (the carry display stack is
 *       TC-M6-08-s-c); the payload is read from the golem's own fields.</li>
 * </ul>
 */
public class AIEssentiaGather extends GolemGoal {

    /**
     * Original {@code private long delay = 0L} ({@code AIEssentiaGather.java:21}),
     * a wall-clock millisecond deadline ({@code System.currentTimeMillis()}).
     */
    private long delay = 0L;

    /**
     * Original {@code int start = 0} ({@code AIEssentiaGather.java:22}): the
     * upward offset of the chosen tile above the home container, {@code -1} when
     * the alembic scan found nothing.
     */
    private int start = 0;

    /**
     * The {@code delay} field for the game tests, which must tell a cooldown
     * refusal apart from a gate refusal when they assert "this goal is inert".
     * The original had no accessor; this one is read-only and test-facing, the
     * same seam shape as {@code GolemHelper.clearJarListForTest()}.
     */
    public long getDelayForTest() {
        return this.delay;
    }

    public AIEssentiaGather(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEssentiaGather.java:27) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /** Original {@code func_75250_a} ({@code AIEssentiaGather.java:30-92}). */
    @Override
    public boolean canUse() {
        // Installed only for core 6 (setupGolem's switch) — reproduce that gate.
        if (!coreIs(6)) {
            return false;
        }
        // AIEssentiaGather.java:31 — navigation idle and the cooldown expired.
        if (!(this.golem.getNavigation().isDone() && this.delay <= System.currentTimeMillis())) {
            return false;
        }
        final Direction facing = GolemHelper.directionOf(this.golem.getHomeFacing());
        final BlockPos container = GolemHelper.homeContainerPos(this.golem);
        // AIEssentiaGather.java:37 — squared distance to the container centre, 6.0.
        if (this.golem.distanceToSqr(container.getX() + 0.5F, container.getY() + 0.5F,
                container.getZ() + 0.5F) > 6.0) {
            return false;
        }
        this.start = 0;
        final BlockEntity te = this.level().getBlockEntity(container);
        if (te == null) {
            return false;
        }
        if (te instanceof IEssentiaTransport etrans) {
            // AIEssentiaGather.java:43-59 — a transport (jar / reservoir / anything
            // that can output to the home face) holding essentia the golem may take.
            final boolean reachable = te instanceof TileJarFillable
                    // SEAM: the original's second clause is
                    // `te instanceof TileEssentiaReservoir` (:45) — that tile is not
                    // shipped in this tree (transport branch), so it is absent here.
                    || etrans.canOutputTo(facing);
            if (reachable && etrans.getEssentiaAmount(facing) > 0
                    && essentiaMatches(etrans, facing)) {
                this.delay = System.currentTimeMillis() + 1000L; // :56
                this.start = 0;
                return true;
            }
        } else {
            // AIEssentiaGather.java:60-83 — scan the six tiles above the container for
            // the TileAlembic holding the most of the aspect the golem wants.
            int prevTot = -1;
            this.start = -1;
            for (int a = 5; a >= 0; a--) {
                final BlockEntity above = this.level().getBlockEntity(container.relative(Direction.UP, a));
                if (above == null || !isAlembicTile(above)) {
                    // SEAM: TileAlembic (a tile with a single `aspect` and `amount`)
                    // is not shipped in this tree, so this arm never matches; the
                    // loop, the y+5..y+0 walk and the largest-amount rule are kept so
                    // the arm is complete when the alembic card lands (:64-79).
                    continue;
                }
                final int held = alembicAmount(above);
                if (held <= prevTot) {
                    continue; // the original's `ta.amount > prevTot` (:73)
                }
                this.delay = System.currentTimeMillis() + 1000L; // :74
                this.start = a;                                  // :75
                prevTot = held;                                  // :76
            }
            return this.start >= 0; // :81-83
        }
        // AIEssentiaGather.java:87 — no transport accepted and no alembic found.
        return false;
    }

    /**
     * Original {@code func_75249_e} ({@code AIEssentiaGather.java:94-156}): the
     * whole take happens in {@code startExecuting}; the original has no
     * {@code updateTask} for this goal.
     */
    @Override
    public void start() {
        final Direction homeFacing = GolemHelper.directionOf(this.golem.getHomeFacing());
        final BlockPos container = GolemHelper.homeContainerPos(this.golem);
        final BlockPos target = container.relative(Direction.UP, this.start);
        final BlockEntity te = this.level().getBlockEntity(target);
        if (!(te instanceof IEssentiaTransport ta)) {
            return;
        }
        Direction facing = homeFacing;
        if (te instanceof TileJarFillable) {
            // AIEssentiaGather.java:102-104 — a jar (and an alembic) is always read
            // from the top face.
            facing = Direction.UP;
        }
        // SEAM: the reservoir clause (`facing = ((TileEssentiaReservoir) te).facing`,
        // :106-108) needs the unshipped reservoir tile.

        // AIEssentiaGather.java:111-113 — nothing to take.
        if (ta.getEssentiaAmount(facing) == 0) {
            return;
        }
        if (!ta.canOutputTo(facing) || ta.getEssentiaAmount(facing) <= 0 || !essentiaMatches(ta, facing)) {
            return;
        }
        Aspect aspect = ta.getEssentiaType(facing);
        if (aspect == null) {
            // DEVIATION: ForgeDirection.UNKNOWN has no 1.21.1 Direction; null is the
            // "the tile's own contents" face and every transport in this tree answers
            // it with its internal aspect (:128).
            aspect = ta.getEssentiaType(null);
        }
        int amountInSource = ta.getEssentiaAmount(facing);
        // SEAM: the reservoir clause `qq = ((TileEssentiaReservoir) te).containerContains(a)`
        // (:132-134) needs the unshipped reservoir tile.

        // AIEssentiaGather.java:136 — the carry clamp. min(available, carryLimit - carried).
        final int want = Math.min(amountInSource, this.golem.getCarryLimit() - this.golem.getEssentiaAmount());
        final int taken = ta.takeEssentia(aspect, want, facing);
        if (taken > 0) {
            this.golem.setEssentia(aspect, this.golem.getEssentiaAmount() + taken);
            playSwim(0.05F); // :141-147 — this goal's sound is quieter: 0.05F
            // DEVIATION (BD-20): updateCarried() (:148) publishes the carry payload to the
            // golem's display stack, which this tree has not shipped yet (TC-M6-08-s-c).
        } else {
            // The original clears the aspect and leaves the amount as it was
            // (:149-151) — a quirk the port keeps, not "fixed".
            this.golem.setEssentia(null, this.golem.getEssentiaAmount());
        }
        this.delay = System.currentTimeMillis() + 100L; // :153
    }

    /**
     * The original's essentia-compatibility clause, verbatim at
     * {@code AIEssentiaGather.java:47-55} (and again at {@code :117-125}):
     * free if the golem carries nothing, otherwise its aspect must be unset,
     * equal to the source's face aspect, or equal to the source's internal
     * aspect — and the carry limit must not be reached.
     */
    private boolean essentiaMatches(final IEssentiaTransport transport, final Direction facing) {
        final Aspect carried = this.golem.getEssentia();
        final int carriedAmount = this.golem.getEssentiaAmount();
        if (carriedAmount == 0) {
            return true;
        }
        final boolean compatible = carried == null
                || carried.equals(transport.getEssentiaType(facing))
                || carried.equals(transport.getEssentiaType(null));
        return compatible && carriedAmount < this.golem.getCarryLimit();
    }

    /**
     * SEAM: {@code TileAlembic} (the 1.7.10 distiller tile with a single
     * {@code aspect} and {@code amount}) is not shipped in this tree, so no tile
     * here is one. The predicate keeps the original's
     * {@code te instanceof TileAlembic} test shape.
     */
    private static boolean isAlembicTile(final BlockEntity tile) {
        return false;
    }

    /**
     * SEAM: the original's {@code ta.amount} for the alembic found above
     * ({@code AIEssentiaGather.java:73, 76}). With no {@code TileAlembic} in this
     * tree the value is never read; it names the number the arm needs.
     */
    private static int alembicAmount(final BlockEntity tile) {
        return 0;
    }

    /**
     * The original's {@code world.playSound(golem, "game.neutral.swim", volume,
     * 1.0F + (rand - rand) * 0.3F)} ({@code AIEssentiaGather.java:141-147}).
     *
     * <p>DEVIATION: {@code game.neutral.swim} is {@link SoundEvents#GENERIC_SWIM}
     * in 1.21.1.</p>
     */
    private void playSwim(final float volume) {
        final float pitch = 1.0F + (this.level().getRandom().nextFloat()
                - this.level().getRandom().nextFloat()) * 0.3F;
        this.level().playSound(null, this.golem, SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL, volume, pitch);
    }
}
