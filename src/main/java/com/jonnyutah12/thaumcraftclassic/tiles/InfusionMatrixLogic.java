package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * MC-independent core of the runic matrix (TC-M4-04): the altar scan
 * ({@code getSurroundings}) and the instability math from
 * {@code thaumcraft.common.tiles.TileInfusionMatrix}, expressed against a
 * probe so they are unit-testable without a Level (docs/testing.md).
 *
 * <p>Geometry (original): the matrix block sits two above the central arcane
 * pedestal; a valid altar also needs four {@link TileInfusionPillar}s at the
 * corners of that pedestal's layer. Pedestals are scanned in a 25x16x25 box
 * (x/z ±12, y −5..+10 relative to the matrix, matrix column excluded); the
 * first pedestal found per (x,z) column counts. Stabilisers are snow layers or
 * blocks implementing {@link IInfusionStabiliser} (original: snow layer,
 * BlockCandle, BlockCrystal).</p>
 *
 * <p>Symmetry (original, unchanged): +2 per pedestal, +1 more if it holds an
 * item, −2 for the point-mirrored pedestal (−1 more if both hold items);
 * stabilisers add +0.1 each and their mirror −0.2, truncated into the integer
 * at the end. Instability at craft start is {@code symmetry + recipe
 * instability} (can be negative — the original never clamps at start), each
 * increment caps at 25, and an event fires when {@code nextInt(500) <=
 * instability} (docs/balance-values.md §5).</p>
 */
public final class InfusionMatrixLogic {

    /** Block-side hook: original {@code thaumcraft.api.crafting.IInfusionStabiliser}. */
    public interface IInfusionStabiliser {
        boolean canStabiliseInfusion(net.minecraft.world.level.Level level, BlockPos pos);
    }

    /** World queries the scan needs, so the logic runs against fakes in tests. */
    public interface WorldProbe {
        boolean isPedestal(BlockPos pos);

        boolean pedestalHasItem(BlockPos pos);

        boolean isPillar(BlockPos pos);

        boolean isStabiliser(BlockPos pos);
    }

    public record Scan(List<BlockPos> pedestals, int symmetry) {
    }

    /** Altar shape check — original {@code validLocation}. */
    public static boolean validLocation(final BlockPos matrix, final WorldProbe probe) {
        final BlockPos centre = matrix.below(2);
        if (!probe.isPedestal(centre)) {
            return false;
        }
        return probe.isPillar(centre.east().south()) && probe.isPillar(centre.east().north())
                && probe.isPillar(centre.west().north()) && probe.isPillar(centre.west().south());
    }

    /** Faithful port of {@code getSurroundings}. */
    public static Scan scan(final BlockPos matrix, final WorldProbe probe) {
        final List<BlockPos> pedestals = new ArrayList<>();
        final List<BlockPos> stabilisers = new ArrayList<>();
        for (int xx = -12; xx <= 12; xx++) {
            for (int zz = -12; zz <= 12; zz++) {
                boolean skip = false;
                for (int yy = -5; yy <= 10; yy++) {
                    if (xx == 0 && zz == 0) {
                        continue;
                    }
                    final BlockPos pos = matrix.offset(xx, -yy, zz);
                    if (!skip && yy > 0 && Math.abs(xx) <= 8 && Math.abs(zz) <= 8 && probe.isPedestal(pos)) {
                        pedestals.add(pos);
                        skip = true;
                    } else if (probe.isStabiliser(pos)) {
                        stabilisers.add(pos);
                    }
                }
            }
        }

        int symmetry = 0;
        for (final BlockPos ped : pedestals) {
            boolean items = false;
            if (probe.pedestalHasItem(ped)) {
                symmetry++;
                items = true;
            }
            symmetry += 2;
            final BlockPos mirror = new BlockPos(2 * matrix.getX() - ped.getX(), ped.getY(),
                    2 * matrix.getZ() - ped.getZ());
            if (probe.isPedestal(mirror)) {
                symmetry -= 2;
                if (probe.pedestalHasItem(mirror) && items) {
                    symmetry--;
                }
            }
        }

        float sym = 0.0F;
        for (final BlockPos st : stabilisers) {
            sym += 0.1F;
            final BlockPos mirror = new BlockPos(2 * matrix.getX() - st.getX(), st.getY(),
                    2 * matrix.getZ() - st.getZ());
            if (probe.isStabiliser(mirror)) {
                sym -= 0.2F;
            }
        }
        return new Scan(pedestals, (int) (symmetry + sym));
    }

    // ------------------------------------------------------- instability math

    /** Original: {@code instability = symmetry + recipeInstability} (no clamp). */
    public static int craftStartInstability(final int symmetry, final int recipeInstability) {
        return symmetry + recipeInstability;
    }

    /** Original: every increment is capped at 25. */
    public static int cappedIncrement(final int instability) {
        return Math.min(25, instability + 1);
    }

    /** Original: {@code instability > 0 && world.random.nextInt(500) <= instability}. */
    public static boolean eventFires(final int instability, final int roll500) {
        return instability > 0 && roll500 <= instability;
    }

    /** Event ids for the original's {@code nextInt(21)} switch. */
    public static final int EV_EJECT = 0;
    public static final int EV_ZAP = 1;
    public static final int EV_HARM = 2;
    public static final int EV_LIGHTNING = 3;
    public static final int EV_WARP = 4;

    /** Maps the 21-sided roll to an event; eject variants keep the original type index. */
    public static int eventForRoll(final int roll21) {
        switch (roll21) {
            case 0: case 2: case 10: case 13: return EV_EJECT;      // type 0
            case 1: case 11: return EV_EJECT | (2 << 8);            // type 2
            case 3: case 8: case 14: return EV_ZAP;                 // single zap
            case 4: case 15: return EV_EJECT | (5 << 8);            // type 5
            case 5: case 16: return EV_HARM;                        // single harm
            case 6: case 17: return EV_EJECT | (1 << 8);            // type 1
            case 7: return EV_EJECT | (4 << 8);                     // type 4
            case 9: return EV_LIGHTNING;
            case 12: return EV_ZAP | (1 << 8);                      // all-zap
            case 18: return EV_HARM | (1 << 8);                     // all-harm
            case 19: return EV_EJECT | (3 << 8);                    // type 3
            case 20: return EV_WARP;
            default: throw new IllegalArgumentException("roll out of range: " + roll21);
        }
    }

    /** Eject type carried in the high byte of {@link #eventForRoll} results. */
    public static int ejectType(final int eventId) {
        return (eventId >> 8) & 0xFF;
    }

    /** True when the low byte marks the "all targets" variants. */
    public static boolean affectsAll(final int eventId) {
        return (eventId >> 8) == 1;
    }

    private InfusionMatrixLogic() {
    }
}
