package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.core.Direction;

/**
 * Essentia transport contract (original {@code thaumcraft.api.aspects
 * .IEssentiaTransport}).
 *
 * <p>Shared API between essentia consumers (arcane furnace/alchemy nozzles,
 * lamps, bore base — TC-M6-04c) and essentia providers (tubes, jars —
 * TC-M4-06/TC-M4-07). The suction model is unchanged from the original:
 * a consumer advertises a suction type/amount per face; a provider pushes
 * essentia toward faces whose suction it exceeds. Ported to {@link Direction}
 * (D-001: ids and semantics unchanged, only the platform types move).</p>
 */
public interface IEssentiaTransport {

    boolean isConnectable(Direction face);

    boolean canInputFrom(Direction face);

    boolean canOutputTo(Direction face);

    void setSuction(Aspect aspect, int amount);

    Aspect getSuctionType(Direction face);

    int getSuctionAmount(Direction face);

    int getMinimumSuction();

    Aspect getEssentiaType(Direction face);

    int getEssentiaAmount(Direction face);

    /** Absorb {@code amount} of {@code aspect} from this side; returns what was taken. */
    int takeEssentia(Aspect aspect, int amount, Direction face);

    /** Push {@code amount} of {@code aspect} into this side; returns what was added. */
    int addEssentia(Aspect aspect, int amount, Direction face);

    /** Whether the extended tube renderer should draw into this tile's cell. */
    boolean renderExtendedTube();
}
