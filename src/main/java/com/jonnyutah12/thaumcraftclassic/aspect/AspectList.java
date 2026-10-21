package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An ordered multiset of aspects with amounts — the port of the original
 * {@code thaumcraft.api.aspects.AspectList} (MIT-licensed API, see
 * docs/licensing-and-attribution.md). Every operation keeps the original's
 * exact arithmetic: {@code add} accumulates, {@code merge} keeps the maximum,
 * {@code reduce} only succeeds when the full amount is present, {@code remove}
 * deletes at zero-or-below.
 *
 * <p>Iteration order is insertion order (the original's LinkedHashMap), which
 * several original consumers rely on. NBT format is unchanged from the
 * original: an {@code Aspects} list of {@code {key: tag, amount: int}}
 * records, so legacy TC save data reads unchanged.</p>
 */
public final class AspectList {

    public static final String NBT_TAG = "Aspects";

    private final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    public AspectList() {
    }

    public AspectList copy() {
        final AspectList out = new AspectList();
        for (final Map.Entry<Aspect, Integer> entry : aspects.entrySet()) {
            out.add(entry.getKey(), entry.getValue());
        }
        return out;
    }

    /** Number of distinct aspects (original {@code size()}). */
    public int size() {
        return aspects.size();
    }

    /** Total vis value: sum of all amounts (original {@code visSize()}). */
    public int visSize() {
        int total = 0;
        for (final int amount : aspects.values()) {
            total += amount;
        }
        return total;
    }

    public List<Aspect> getAspects() {
        return new ArrayList<>(aspects.keySet());
    }

    public List<Aspect> getPrimalAspects() {
        final AspectList t = new AspectList();
        for (final Aspect aspect : aspects.keySet()) {
            if (aspect.isPrimal()) {
                t.add(aspect, 1);
            }
        }
        return t.getAspects();
    }

    public List<Aspect> getAspectsSorted() {
        final List<Aspect> out = getAspects();
        out.sort(Comparator.comparing(Aspect::getTag));
        return out;
    }

    public List<Aspect> getAspectsSortedAmount() {
        final List<Aspect> out = getAspects();
        // Original bubble sort: strictly-greater swaps on positive amounts only,
        // stable for equal amounts — reproduced with a stable descending sort
        // restricted to positive entries, which yields the same order.
        final List<Aspect> positive = out.stream().filter(a -> getAmount(a) > 0).toList();
        final List<Aspect> sorted = new ArrayList<>(positive);
        sorted.sort((a, b) -> Integer.compare(getAmount(b), getAmount(a)));
        out.removeAll(positive);
        out.addAll(0, sorted);
        return out;
    }

    public int getAmount(final Aspect aspect) {
        return aspects.getOrDefault(aspect, 0);
    }

    /** Original: accumulates when the aspect is already present. */
    public AspectList add(final Aspect aspect, final int amount) {
        if (aspect == null) {
            return this;
        }
        aspects.put(aspect, getAmount(aspect) + amount);
        return this;
    }

    public AspectList add(final AspectList other) {
        for (final Aspect aspect : other.getAspects()) {
            add(aspect, other.getAmount(aspect));
        }
        return this;
    }

    /** Original: keeps the higher of old and new amount. */
    public AspectList merge(final Aspect aspect, final int amount) {
        if (aspect == null) {
            return this;
        }
        aspects.put(aspect, Math.max(getAmount(aspect), amount));
        return this;
    }

    public AspectList merge(final AspectList other) {
        for (final Aspect aspect : other.getAspects()) {
            merge(aspect, other.getAmount(aspect));
        }
        return this;
    }

    /** Original: subtracts only when the full amount is available. */
    public boolean reduce(final Aspect aspect, final int amount) {
        if (getAmount(aspect) >= amount) {
            aspects.put(aspect, getAmount(aspect) - amount);
            return true;
        }
        return false;
    }

    /** Original: subtracts; deletes the entry at zero-or-below. */
    public AspectList remove(final Aspect aspect, final int amount) {
        final int remaining = getAmount(aspect) - amount;
        if (remaining <= 0) {
            aspects.remove(aspect);
        } else {
            aspects.put(aspect, remaining);
        }
        return this;
    }

    public AspectList remove(final Aspect aspect) {
        aspects.remove(aspect);
        return this;
    }

    // ------------------------------------------------------------------ NBT

    /** Original format: {@code {Aspects: [{key: tag, amount: int}, ...]}}. */
    public void readFromNBT(final CompoundTag tag) {
        readFromNBT(tag, NBT_TAG);
    }

    public void readFromNBT(final CompoundTag tag, final String label) {
        aspects.clear();
        if (!tag.contains(label, Tag.TAG_LIST)) {
            return;
        }
        final ListTag list = tag.getList(label, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            if (entry.contains("key")) {
                final Aspect aspect = Aspect.getAspect(entry.getString("key"));
                if (aspect != null) {
                    add(aspect, entry.getInt("amount"));
                }
            }
        }
    }

    public void writeToNBT(final CompoundTag tag) {
        writeToNBT(tag, NBT_TAG);
    }

    public void writeToNBT(final CompoundTag tag, final String label) {
        final ListTag list = new ListTag();
        for (final Aspect aspect : getAspects()) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", getAmount(aspect));
            list.add(entry);
        }
        tag.put(label, list);
    }

    // -------------------------------------------------------------- identity

    @Override
    public boolean equals(final Object other) {
        return other instanceof AspectList otherList && aspects.equals(otherList.aspects);
    }

    @Override
    public int hashCode() {
        return aspects.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AspectList[");
        for (final Map.Entry<Aspect, Integer> entry : aspects.entrySet()) {
            sb.append(entry.getKey().getTag()).append('=').append(entry.getValue()).append(' ');
        }
        return sb.toString().trim() + "]";
    }
}
