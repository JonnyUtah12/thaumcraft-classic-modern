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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * The Alchemy golem's "go empty my essentia" goal (TC-M6-08-s-b2): the goal
 * that hands the carried essentia to the nearest jar that has room.
 *
 * <p>Original: {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/fluid/AIEssentiaEmpty.java}
 * (119 lines). Installed by {@code setupGolem()} for <b>core 6 (Alchemy) at
 * priority 1</b> ({@code .../common/entities/golems/EntityGolemBase.java:262}:
 * {@code field_70714_bg.func_75776_a(1, new AIEssentiaEmpty(this))}).</p>
 *
 * <h2>Numbers pinned from the original</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIEssentiaEmpty.java:25}) → {@code MOVE|JUMP}
 *       in 1.21.1's {@link Goal.Flag} vocabulary;</li>
 *   <li>{@code canUse} requires the navigation to be idle, a carried aspect and a
 *       non-zero amount ({@code AIEssentiaEmpty.java:30}), a jar found by
 *       {@code GolemHelper.findJarWithRoom} ({@code :31}) and the jar within the
 *       <b>squared {@code 4.0}</b> of the golem ({@code :34}) — a 2-block radius,
 *       the same squared-comparison quirk as {@link GolemHelper#ADJACENT_RANGE};</li>
 *   <li>{@code canContinueToUse} is constant {@code false}: the whole act happens
 *       in {@code startExecuting} ({@code :47-49, 51});</li>
 *   <li>the jar arm is {@code essentiaAmount = jar.addToContainer(essentia,
 *       essentiaAmount)} and clears the aspect when the remainder is 0
 *       ({@code :55-58});</li>
 *   <li>the swim sound is volume {@code 0.2F} with pitch
 *       {@code 1.0F + (rand - rand) * 0.3F} ({@code :60-63}).</li>
 * </ul>
 *
 * <h2>Port shape</h2>
 * The container mutation runs through the shipped
 * {@link EntityGolemBase#actEssentiaEmpty()} (parent deviation {@code BD-17}):
 * it re-resolves the same jar with {@link GolemHelper#findJarWithRoom} and applies
 * {@link TileJarFillable#addToContainer}, which returns exactly the remainder the
 * original assigned to {@code essentiaAmount}. The goal keeps the original's
 * gating, its squared-4.0 proximity test, its sound and its block update.
 *
 * <p><b>Declared deviations/seams</b> are at each site: the
 * {@code TileEssentiaReservoir} arm, the generic {@code IEssentiaTransport}
 * suction arm, {@code updateCarried()}, and the 1.7.10 swim sound id.</p>
 */
public class AIEssentiaEmpty extends GolemGoal {

    /** Original {@code jarX/jarY/jarZ} ({@code AIEssentiaEmpty.java:16-18}). */
    private int jarX;
    private int jarY;
    private int jarZ;

    public AIEssentiaEmpty(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIEssentiaEmpty.java:25) = MOVE | JUMP.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    /** Original {@code func_75250_a} ({@code AIEssentiaEmpty.java:28-45}). */
    @Override
    public boolean canUse() {
        // The goal is installed only for core 6 (setupGolem's switch); the guard
        // reproduces that installation gate, so a core-less golem does nothing.
        if (!coreIs(6)) {
            return false;
        }
        // AIEssentiaEmpty.java:30 — navigation idle, carrying an aspect, amount non-zero.
        if (!(this.golem.getNavigation().isDone() && this.golem.getEssentia() != null
                && this.golem.getEssentiaAmount() != 0)) {
            return false;
        }
        final BlockPos jar = GolemHelper.findJarWithRoom(this.golem);
        if (jar == null) {
            return false;
        }
        // AIEssentiaEmpty.java:34 — squared distance to the jar centre, compared
        // against 4.0 (a 2-block radius). Preserved, not "fixed".
        if (this.golem.distanceToSqr(jar.getX() + 0.5, jar.getY() + 0.5, jar.getZ() + 0.5) > 4.0) {
            return false;
        }
        this.jarX = jar.getX();
        this.jarY = jar.getY();
        this.jarZ = jar.getZ();
        return true;
    }

    /** Original {@code func_75253_b} → constant false ({@code AIEssentiaEmpty.java:47-49}). */
    @Override
    public boolean canContinueToUse() {
        return false;
    }

    /** Original {@code func_75249_e} ({@code AIEssentiaEmpty.java:51-118}). */
    @Override
    public void start() {
        final BlockPos pos = new BlockPos(this.jarX, this.jarY, this.jarZ);
        final BlockEntity tile = this.level().getBlockEntity(pos);
        if (tile == null) {
            return;
        }
        if (tile instanceof TileJarFillable) {
            // The jar arm (AIEssentiaEmpty.java:53-65): addToContainer returns the
            // remainder, which becomes the golem's essentiaAmount; actEssentiaEmpty
            // is the shipped act method that does exactly that (BD-17).
            if (this.golem.actEssentiaEmpty()) {
                playSwim(0.2F);
                // DEVIATION (BD-20): the original's updateCarried() (AIEssentiaEmpty.java:64)
                // publishes the carry payload to the golem's display/renders; this
                // tree has no display stack yet (TC-M6-08-s-c), and the payload is
                // read straight from getEssentia()/getEssentiaAmount().
                // TileJarFillable.addToContainer → touch() already does the
                // original's markDirtyForTheClient (AIEssentiaEmpty.java:65).
            }
        } else if (isReservoirTile(tile)) {
            // SEAM: the reservoir arm (AIEssentiaEmpty.java:66-87) needs
            // TileEssentiaReservoir — a tile with a single `facing`, getSuctionAmount/
            // getSuctionType on that face and containerContains(aspect). That tile is
            // not shipped in this tree (it belongs to the transport branch), so the
            // arm stays as this unreachable-by-construction branch.
        } else if (tile instanceof IEssentiaTransport transport) {
            emptyIntoTransport(tile, transport);
        }
    }

    /**
     * The original's generic transport arm ({@code AIEssentiaEmpty.java:88-117}):
     * for every marked face of the tile that can input, advertises suction and
     * either has no suction type or wants exactly the carried aspect, push the
     * essentia in and keep only what came back.
     *
     * <p><b>SEAM in this tree:</b> {@link GolemHelper#findJarWithRoom} only ever
     * resolves jars (its javadoc says so; the tube/reservoir targeting lives with
     * the transport branch), so {@link #start()} can never hand this method a
     * non-jar tile. The arm is ported so the behaviour is complete once the
     * transport card lands, not re-invented then.</p>
     */
    private void emptyIntoTransport(final BlockEntity tile, final IEssentiaTransport transport) {
        final Aspect carried = this.golem.getEssentia();
        for (final int side : GolemHelper.getMarkedSides(this.golem, tile, (byte) -1)) {
            final Direction face = GolemHelper.directionOf(side);
            final Aspect wanted = transport.getSuctionType(face);
            if (transport.canInputFrom(face) && transport.getSuctionAmount(face) > 0
                    && (wanted == null || wanted.equals(carried))) {
                final int added = transport.addEssentia(carried, this.golem.getEssentiaAmount(), face);
                if (added > 0) {
                    final int left = this.golem.getEssentiaAmount() - added;
                    this.golem.setEssentia(left == 0 ? null : carried, left);
                    playSwim(0.2F);
                    // DEVIATION: updateCarried() — see the jar arm above.
                    return; // the original breaks out of the side loop (AIEssentiaEmpty.java:113)
                }
            }
        }
    }

    /**
     * SEAM: {@code TileEssentiaReservoir} is not shipped in this tree, so no tile
     * here can be one; the check is the honest shape of the original's
     * {@code tile instanceof TileEssentiaReservoir} test.
     */
    private static boolean isReservoirTile(final BlockEntity tile) {
        return false;
    }

    /**
     * The original's {@code world.playSound(golem, "game.neutral.swim", volume,
     * 1.0F + (rand - rand) * 0.3F)} ({@code AIEssentiaEmpty.java:60-63}).
     *
     * <p>DEVIATION: the 1.7.10 sound id {@code game.neutral.swim} is
     * {@link SoundEvents#GENERIC_SWIM} in 1.21.1; the pitch jitter formula is the
     * original's own (two draws from the same random, subtracted).</p>
     */
    private void playSwim(final float volume) {
        final float pitch = 1.0F + (this.level().getRandom().nextFloat()
                - this.level().getRandom().nextFloat()) * 0.3F;
        this.level().playSound(null, this.golem, SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL, volume, pitch);
    }
}
