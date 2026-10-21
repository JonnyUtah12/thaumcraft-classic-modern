package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModAspects;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One Thaumcraft aspect: the registered object behind the
 * {@code thaumcraft:aspect} registry (TC-M4-01).
 *
 * <p>Field set and behaviour mirror the original
 * {@code thaumcraft.api.aspects.Aspect} exactly: tag, display color, chat
 * color code, blend mode, image path and the two-component compound rule.
 * Components are declared as tags in {@link AspectDefinitions} and resolved
 * lazily through the registry, which removes the original's static-init
 * ordering constraint while keeping {@link #getComponents()} identity-stable
 * per registry object (the original compared components with {@code ==}).</p>
 *
 * <p>Registry objects are singletons per id, so identity equality (the
 * original's semantics) is intentional: no equals/hashCode override.</p>
 */
public final class Aspect {

    private final String tag;
    private final int color;
    private final String chatcolor;
    private final int blend;
    private final List<String> componentTags;
    private volatile List<Aspect> resolvedComponents;

    public Aspect(final AspectDefinitions.Definition definition) {
        this.tag = definition.tag();
        this.color = definition.color();
        this.chatcolor = definition.chatcolor();
        this.blend = definition.blend();
        this.componentTags = definition.components();
    }

    public String getTag() {
        return tag;
    }

    public int getColor() {
        return color;
    }

    /** Legacy chat color code (single hex digit); null for compounds, as in the original. */
    public String getChatcolor() {
        return chatcolor;
    }

    public int getBlend() {
        return blend;
    }

    /** Component tags in declaration order; empty for primals. */
    public List<String> getComponentTags() {
        return componentTags;
    }

    /** Original semantics: primal iff the component list is not exactly two entries. */
    public boolean isPrimal() {
        return componentTags.size() != 2;
    }

    /**
     * The two component aspects, or {@code null} for primals (the original
     * returns null, and callers such as {@code hasDiscoveredParentAspects}
     * branch on that).
     */
    public List<Aspect> getComponents() {
        if (isPrimal()) {
            return null;
        }
        List<Aspect> resolved = resolvedComponents;
        if (resolved == null) {
            final Registry<Aspect> registry = ModAspects.registry();
            if (registry == null) {
                throw new IllegalStateException("aspect registry not available yet for " + tag);
            }
            final List<Aspect> components = new ArrayList<>(2);
            for (final String componentTag : componentTags) {
                final Aspect component = registry.get(ModAspects.aspectId(componentTag));
                if (component == null) {
                    throw new IllegalStateException("component aspect " + componentTag + " missing for " + tag);
                }
                components.add(component);
            }
            resolved = List.copyOf(components);
            resolvedComponents = resolved;
        }
        return resolved;
    }

    /** Original: {@code WordUtils.capitalizeFully(tag)} — tags are single lowercase words. */
    public String getName() {
        return tag.substring(0, 1).toUpperCase(Locale.ROOT) + tag.substring(1);
    }

    /** Original image path: {@code thaumcraft:textures/aspects/<tag>.png}. */
    public ResourceLocation getImage() {
        return ResourceLocation.fromNamespaceAndPath(
                ThaumcraftClassic.MOD_ID, "textures/aspects/" + tag.toLowerCase(Locale.ROOT) + ".png");
    }

    // ------------------------------------------------------- static access

    /** Original {@code Aspect.getAspect(tag)}: null for unknown tags. */
    public static Aspect getAspect(final String tag) {
        final Registry<Aspect> registry = ModAspects.registry();
        return registry == null ? null : registry.get(ModAspects.aspectId(tag));
    }

    /** All registered aspects in declaration order (original {@code Aspect.aspects.values()}). */
    public static List<Aspect> values() {
        final Registry<Aspect> registry = ModAspects.registry();
        if (registry == null) {
            return List.of();
        }
        final List<Aspect> out = new ArrayList<>(AspectDefinitions.all().size());
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            final Aspect aspect = registry.get(ModAspects.aspectId(definition.tag()));
            if (aspect != null) {
                out.add(aspect);
            }
        }
        return out;
    }

    public static List<Aspect> getPrimalAspects() {
        final List<Aspect> primal = new ArrayList<>();
        for (final Aspect aspect : values()) {
            if (aspect.isPrimal()) {
                primal.add(aspect);
            }
        }
        return primal;
    }

    public static List<Aspect> getCompoundAspects() {
        final List<Aspect> compound = new ArrayList<>();
        for (final Aspect aspect : values()) {
            if (!aspect.isPrimal()) {
                compound.add(aspect);
            }
        }
        return compound;
    }

    @Override
    public String toString() {
        return tag;
    }
}
