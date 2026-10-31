package com.jonnyutah12.thaumcraftclassic.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The portable hole block's carrier seam (TC-M6-03-p): the original's static
 * {@code TileHole.createHole(world, pos, side, length, ticks)}
 * ({@code ItemFocusPortableHole.java:169-205}), called with the hole's
 * length ({@code distance + 1}) and duration in ticks
 * ({@code ItemFocusPortableHole.java:142-146}).
 *
 * <p>{@code blockHole}/{@code TileHole} are the portable-hole machinery
 * card's content; the performer finds the block by registry id
 * ({@code thaumcraft:blockhole}, the original's {@code GameRegistry
 * .findBlock}) and opens through this seam. The seam owns the original's
 * placement sequence: the guard loop over the distance (air, unbreakable,
 * warded and other hole blocks refused — {@code :176-196}), clearing the
 * column to air, placing the hole block facing {@code side}, and starting the
 * timer. It returns false when a guard refuses, which is the original's
 * silent no-op ({@code return true;}).</p>
 */
public interface HoleCarrier {

    /**
     * Opens a hole of {@code length} blocks at {@code pos} facing
     * {@code side}, lasting {@code durationTicks}; false when the original's
     * guards refuse it (the caller refunds what it charged).
     */
    boolean openHole(ServerLevel level, BlockPos pos, Direction side, int length, int durationTicks);
}
