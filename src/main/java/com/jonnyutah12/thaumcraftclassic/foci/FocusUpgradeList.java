package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The upgrades applied to one focus stack (TC-M6-03-s): the original
 * {@code ItemFocusBasic} NBT {@code "upgrade"} {@code NBTTagList} of
 * {@code {id: short}} compounds
 * ({@code reference/decompiled/…/api/wands/ItemFocusBasic.java:129-207}).
 *
 * <p>The original was a fixed {@code short[5]} — one slot per rank, {@code -1}
 * for an empty slot — that {@code getAppliedUpgrades} filled from the tag list
 * and {@code setFocusUpgradeTagList} wrote back <em>whole</em>, including the
 * {@code -1} entries. The port keeps exactly that shape: {@link #ranks()} is
 * always {@link #RANKS} entries, {@link #EMPTY_RANK} means "no upgrade in that
 * rank", and the id is the {@link FocusUpgradeType#id()} the original's
 * {@code types[]} array indexed.</p>
 *
 * <p>Rank rules are the original's and stay in this record so they are
 * testable without a stack: {@code applyUpgrade} accepted a type only when
 * <em>its own rank's slot was empty</em> and the rank was 1..5
 * ({@code ItemFocusBasic.java:143-152}) — it never looked at the type's
 * applicability, which {@code canApplyUpgrade} answered separately
 * ({@link FocusUpgradeTrees}).</p>
 *
 * @param ranks the five rank slots, {@link #EMPTY_RANK} where nothing is applied
 */
public record FocusUpgradeList(List<Integer> ranks) {

    /** The original's {@code short[5]} ({@code ItemFocusBasic.java:130}). */
    public static final int RANKS = 5;

    /** The original's empty-slot marker ({@code new short[]{-1, -1, -1, -1, -1}}). */
    public static final int EMPTY_RANK = -1;

    /** A focus with no upgrades. */
    public static final FocusUpgradeList NONE = new FocusUpgradeList(List.of(-1, -1, -1, -1, -1));

    /** Persisted shape — the original's int-per-rank list, empty ranks included. */
    public static final Codec<FocusUpgradeList> CODEC =
            Codec.INT.listOf().xmap(FocusUpgradeList::of, FocusUpgradeList::ranks);

    /** Network shape, so the client sees the upgrade tooltip the server stores. */
    public static final StreamCodec<ByteBuf, FocusUpgradeList> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())
                    .map(FocusUpgradeList::of, FocusUpgradeList::ranks);

    public FocusUpgradeList {
        if (ranks.size() != RANKS) {
            throw new IllegalArgumentException("a focus has " + RANKS + " upgrade ranks, got " + ranks.size());
        }
        for (final Integer rank : ranks) {
            if (rank == null || rank < EMPTY_RANK) {
                throw new IllegalArgumentException("an upgrade rank is an id or " + EMPTY_RANK + ", got " + rank);
            }
        }
        ranks = List.copyOf(ranks);
    }

    /**
     * The original's {@code getAppliedUpgrades} pad/truncate rule: the first
     * {@link #RANKS} entries of {@code ids} land in rank order and the rest of
     * the {@code short[5]} stays {@link #EMPTY_RANK}.
     */
    public static FocusUpgradeList of(final List<Integer> ids) {
        final List<Integer> padded = new ArrayList<>(RANKS);
        for (int i = 0; i < RANKS; i++) {
            padded.add(i < ids.size() && ids.get(i) != null ? ids.get(i) : EMPTY_RANK);
        }
        return new FocusUpgradeList(padded);
    }

    /** {@link #of(List)} from varargs, i.e. the original's literal {@code short[]}. */
    public static FocusUpgradeList of(final int... ids) {
        final List<Integer> list = new ArrayList<>(RANKS);
        for (final int id : ids) {
            list.add(id);
        }
        return of(list);
    }

    /** A list with {@code type} in {@code rank} (1-based); fails when the slot is taken. */
    public static FocusUpgradeList single(final FocusUpgradeType type, final int rank) {
        return NONE.withUpgrade(type, rank)
                .orElseThrow(() -> new IllegalArgumentException("rank " + rank + " of a fresh focus must be free"));
    }

    /** The applied ids in rank order — the original's non-{@code -1} entries, {@code getAppliedUpgrades}. */
    public List<Integer> applied() {
        final List<Integer> out = new ArrayList<>(RANKS);
        for (final Integer rank : ranks) {
            if (rank != EMPTY_RANK) {
                out.add(rank);
            }
        }
        return List.copyOf(out);
    }

    /** The types applied, in rank order — resolved through the shipped upgrade table. */
    public List<FocusUpgradeType> appliedTypes() {
        final List<FocusUpgradeType> out = new ArrayList<>(RANKS);
        for (final Integer rank : ranks) {
            final FocusUpgradeType type = FocusUpgrades.byId(rank);
            if (type != null) {
                out.add(type);
            }
        }
        return List.copyOf(out);
    }

    /** Original {@code getUpgradeLevel}: how many ranks hold {@code type}. */
    public int level(final FocusUpgradeType type) {
        if (type == null) {
            return 0;
        }
        int level = 0;
        for (final Integer rank : ranks) {
            if (rank == type.id()) {
                level++;
            }
        }
        return level;
    }

    /** Original {@code isUpgradedWith}. */
    public boolean isUpgradedWith(final FocusUpgradeType type) {
        return level(type) > 0;
    }

    /** Whether {@code rank} (1-based) is empty — the slot {@code applyUpgrade} demanded. */
    public boolean rankFree(final int rank) {
        return rank >= 1 && rank <= RANKS && ranks.get(rank - 1) == EMPTY_RANK;
    }

    /** The type in {@code rank} (1-based), or null. */
    public FocusUpgradeType at(final int rank) {
        if (rank < 1 || rank > RANKS) {
            return null;
        }
        return FocusUpgrades.byId(ranks.get(rank - 1));
    }

    /**
     * Original {@code applyUpgrade} ({@code ItemFocusBasic.java:143-152}): the
     * upgrade lands in its own rank's slot, and only when that slot is empty and
     * the rank is 1..5. Empty when the original would have returned false.
     */
    public Optional<FocusUpgradeList> withUpgrade(final FocusUpgradeType type, final int rank) {
        if (type == null || !rankFree(rank)) {
            return Optional.empty();
        }
        final List<Integer> out = new ArrayList<>(ranks);
        out.set(rank - 1, type.id());
        return Optional.of(new FocusUpgradeList(out));
    }

    /** This list with {@code rank} cleared — the inverse of {@link #withUpgrade}. */
    public FocusUpgradeList cleared(final int rank) {
        if (rank < 1 || rank > RANKS) {
            throw new IllegalArgumentException("no such focus rank: " + rank);
        }
        final List<Integer> out = new ArrayList<>(ranks);
        out.set(rank - 1, EMPTY_RANK);
        return new FocusUpgradeList(out);
    }

    /** The upgrade ids the original's tooltip listed, in rank order. */
    public List<Integer> tooltipOrder() {
        return applied();
    }
}
