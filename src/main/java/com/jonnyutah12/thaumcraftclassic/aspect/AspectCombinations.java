package com.jonnyutah12.thaumcraftclassic.aspect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The aspect combination rules (TC-M4-01) — the port of the original
 * {@code ResearchManager.getCombinationResult}/{@code reduceToPrimals} pair.
 * (The card body's "AspectHelper" is a TC5-era name; in Thaumcraft 4.2.3.5
 * these rules live in ResearchManager — verified against the decompiled
 * source, see docs/aspects.md.)
 *
 * <p>The original rule is exact: a compound is the result of the pair formed
 * by its two declared components, in either order; every other pair has no
 * result. The pure table below is derived from {@link AspectDefinitions} and
 * is the same data the in-game lookup walks, so unit tests and the docs
 * table cannot drift from runtime behaviour.</p>
 */
public final class AspectCombinations {

    private AspectCombinations() {
    }

    /** Original {@code ResearchManager.getCombinationResult}: identity match on the component pair. */
    public static Aspect getCombinationResult(final Aspect aspect1, final Aspect aspect2) {
        if (aspect1 == null || aspect2 == null) {
            return null;
        }
        for (final Aspect aspect : Aspect.values()) {
            final List<Aspect> components = aspect.getComponents();
            if (components != null
                    && ((components.get(0) == aspect1 && components.get(1) == aspect2)
                    || (components.get(0) == aspect2 && components.get(1) == aspect1))) {
                return aspect;
            }
        }
        return null;
    }

    /**
     * The exhaustive pair → result table over the 48 definitions, Minecraft-free.
     * Key: the two tags joined with "+" in declaration order of the result's
     * component list. Result: the compound's tag. Pairs absent from the table
     * have no result (original returns null).
     */
    public static Map<String, String> combinationTable() {
        final Map<String, String> table = new LinkedHashMap<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            if (definition.primal()) {
                continue;
            }
            final String first = definition.components().get(0);
            final String second = definition.components().get(1);
            table.put(first + "+" + second, definition.tag());
            table.put(second + "+" + first, definition.tag());
        }
        return table;
    }

    /**
     * Pure-table lookup over tags (no registry needed): null when the pair has
     * no result, exactly like the original.
     */
    public static String combinationResultTag(final String tag1, final String tag2) {
        return combinationTable().get(tag1 + "+" + tag2);
    }

    /** Original {@code ResearchManager.reduceToPrimals(al)}. */
    public static AspectList reduceToPrimals(final AspectList list) {
        return reduceToPrimals(list, false);
    }

    /**
     * Original {@code ResearchManager.reduceToPrimals(al, merge)}: every
     * compound contributes its two components (recursively reduced); with
     * {@code merge} the contributions merge by maximum instead of adding.
     */
    public static AspectList reduceToPrimals(final AspectList list, final boolean merge) {
        final AspectList out = new AspectList();
        for (final Aspect aspect : list.getAspects()) {
            if (aspect == null) {
                continue;
            }
            if (aspect.isPrimal()) {
                if (merge) {
                    out.merge(aspect, list.getAmount(aspect));
                } else {
                    out.add(aspect, list.getAmount(aspect));
                }
            } else {
                final AspectList send = new AspectList();
                send.add(aspect.getComponents().get(0), list.getAmount(aspect));
                send.add(aspect.getComponents().get(1), list.getAmount(aspect));
                final AspectList reduced = reduceToPrimals(send, merge);
                for (final Aspect component : reduced.getAspects()) {
                    if (merge) {
                        out.merge(component, reduced.getAmount(component));
                    } else {
                        out.add(component, reduced.getAmount(component));
                    }
                }
            }
        }
        return out;
    }

    /**
     * Pure-table variant of {@link #reduceToPrimals} over tags (no registry
     * needed): same recursive decomposition and add/merge arithmetic, for
     * Minecraft-free tests and the docs table.
     */
    public static Map<String, Integer> reduceToPrimalsTags(final Map<String, Integer> amounts,
                                                           final boolean merge) {
        final Map<String, AspectDefinitions.Definition> byTag = new LinkedHashMap<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            byTag.put(definition.tag(), definition);
        }
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (final Map.Entry<String, Integer> entry : amounts.entrySet()) {
            reduceTag(byTag, out, entry.getKey(), entry.getValue(), merge);
        }
        return out;
    }

    private static void reduceTag(final Map<String, AspectDefinitions.Definition> byTag,
                                 final Map<String, Integer> out, final String tag,
                                 final int amount, final boolean merge) {
        final AspectDefinitions.Definition definition = byTag.get(tag);
        if (definition == null) {
            return;
        }
        if (definition.primal()) {
            if (merge) {
                out.merge(tag, amount, Math::max);
            } else {
                out.merge(tag, amount, Integer::sum);
            }
        } else {
            for (final String componentTag : definition.components()) {
                reduceTag(byTag, out, componentTag, amount, merge);
            }
        }
    }

    /**
     * Original {@code ThaumcraftApiHelper.cullTags}: trim to 6 aspects by
     * weighted value, dropping the lowest each round. Weights: primal ×0.9;
     * per non-primal component ×1.1; per non-primal grandchild ×1.05.
     */
    public static AspectList cullTags(final AspectList list) {
        final AspectList temp = list.copy();

        while (temp.size() > 6) {
            Aspect lowest = null;
            float low = 32767.0F;

            for (final Aspect tag : temp.getAspects()) {
                if (tag == null) {
                    continue;
                }
                float value = temp.getAmount(tag);
                if (tag.isPrimal()) {
                    value *= 0.9F;
                } else {
                    final List<Aspect> components = tag.getComponents();
                    for (final Aspect component : components) {
                        if (!component.isPrimal()) {
                            value *= 1.1F;
                            for (final Aspect grandchild : component.getComponents()) {
                                if (!grandchild.isPrimal()) {
                                    value *= 1.05F;
                                }
                            }
                        }
                    }
                }

                if (value < low) {
                    low = value;
                    lowest = tag;
                }
            }

            temp.remove(lowest);
        }

        return temp;
    }

    /**
     * Pure-definition variant of {@link #cullTags} for Minecraft-free tests:
     * same weighting math over tag names and the definition graph.
     */
    public static List<String> cullTagOrder(final Map<String, Integer> amounts) {
        final Map<String, AspectDefinitions.Definition> byTag = new LinkedHashMap<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            byTag.put(definition.tag(), definition);
        }
        final List<String> kept = new ArrayList<>(amounts.keySet());
        while (kept.size() > 6) {
            String lowest = null;
            float low = 32767.0F;
            for (final String tag : kept) {
                float value = amounts.get(tag);
                final AspectDefinitions.Definition definition = byTag.get(tag);
                if (definition.primal()) {
                    value *= 0.9F;
                } else {
                    for (final String componentTag : definition.components()) {
                        final AspectDefinitions.Definition component = byTag.get(componentTag);
                        if (!component.primal()) {
                            value *= 1.1F;
                            for (final String grandchildTag : component.components()) {
                                if (!byTag.get(grandchildTag).primal()) {
                                    value *= 1.05F;
                                }
                            }
                        }
                    }
                }
                if (value < low) {
                    low = value;
                    lowest = tag;
                }
            }
            kept.remove(lowest);
        }
        return kept;
    }
}
