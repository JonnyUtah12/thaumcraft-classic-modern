package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * The essentia tag tables: which aspects an item or entity carries
 * (TC-M4-01). This is the port of the original
 * {@code ThaumcraftApi.objectTags}/{@code scanEntities} stores plus the
 * lookup semantics of {@code ThaumcraftCraftingManager.getObjectTags} and
 * {@code ScanManager.generateEntityAspects}.
 *
 * <p>The data is loaded from {@code data/thaumcraft/essentia/tags.json}
 * (generated 1:1 from the original {@code ConfigAspects.java} by
 * {@code scripts/extract_essentia_tags.py}; format and provenance:
 * docs/aspects.md). Entries keep the original 1.7.10 item ids and metas;
 * {@link #variantMeta(ItemStack)} is the documented seam where a future
 * variant resolver maps modern items onto those (id, meta) pairs.</p>
 */
public final class EssentiaTags {

    private static final Logger LOGGER = ThaumcraftClassic.LOGGER;

    /** One object-tag entry: aspects carried by (item id, meta). */
    public record ItemTag(ResourceLocation item, int meta, boolean complex,
                          String condition, Map<String, Integer> aspects) {
    }

    /** One oredict-name tag entry (original registerObjectTag(String, ...)). */
    public record OreDictTag(String name, boolean complex, String condition,
                             Map<String, Integer> aspects) {
    }

    /** One entity-tag entry, with the optional NBT condition of the original. */
    public record EntityTag(ResourceLocation entity, String originalName,
                            NbtCondition condition, Map<String, Integer> aspects) {
    }

    /** Original {@code ThaumcraftApi.EntityTagsNBT}: name + typed value. */
    public record NbtCondition(String name, String type, Object value) {

        boolean matches(final CompoundTag entityData) {
            if ("byte".equals(type)) {
                return entityData.getByte(name) == ((Byte) value).byteValue();
            }
            if ("string".equals(type)) {
                return entityData.getString(name).equals(value);
            }
            return false;
        }
    }

    private static final Map<ResourceLocation, List<ItemTag>> ITEM_TAGS = new LinkedHashMap<>();
    private static final Map<String, List<OreDictTag>> OREDICT_TAGS = new LinkedHashMap<>();
    private static final List<EntityTag> ENTITY_TAGS = new ArrayList<>();

    private static boolean loaded;

    private EssentiaTags() {
    }

    // ------------------------------------------------------------------ load

    static synchronized void install(final List<ItemTag> items,
                                    final List<OreDictTag> oredict,
                                    final List<EntityTag> entities) {
        ITEM_TAGS.clear();
        OREDICT_TAGS.clear();
        ENTITY_TAGS.clear();
        for (final ItemTag tag : items) {
            ITEM_TAGS.computeIfAbsent(tag.item(), k -> new ArrayList<>()).add(tag);
        }
        for (final OreDictTag tag : oredict) {
            OREDICT_TAGS.computeIfAbsent(tag.name(), k -> new ArrayList<>()).add(tag);
        }
        ENTITY_TAGS.addAll(entities);
        loaded = true;
        LOGGER.info("essentia tags loaded: {} item entries, {} oredict entries, {} entity entries",
                items.size(), oredict.size(), entities.size());
    }

    /** Loads the shipped tables on first use (idempotent). */
    public static synchronized void ensureLoaded() {
        if (!loaded) {
            EssentiaTagDataLoader.loadDefault().ifPresent(data ->
                    install(data.itemTags(), data.oredictTags(), data.entityTags()));
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int itemTagCount() {
        return ITEM_TAGS.values().stream().mapToInt(List::size).sum();
    }

    public static int oredictTagCount() {
        return OREDICT_TAGS.values().stream().mapToInt(List::size).sum();
    }

    public static int entityTagCount() {
        return ENTITY_TAGS.size();
    }

    public static List<ItemTag> itemTagsFor(final ResourceLocation item) {
        return ITEM_TAGS.getOrDefault(item, List.of());
    }

    public static List<OreDictTag> oredictTags(final String name) {
        return OREDICT_TAGS.getOrDefault(name, List.of());
    }

    public static List<EntityTag> entityTags() {
        return List.copyOf(ENTITY_TAGS);
    }

    // --------------------------------------------------------------- lookup

    /**
     * Original {@code ThaumcraftCraftingManager.getObjectTags} fallback chain:
     * exact (item, meta) → wildcard meta 32767 → (meta==32767 only) first of
     * metas 0..15. The original's int[]-meta registrations are expanded to
     * per-meta entries by the extractor, so the range walk is covered by the
     * exact step. Crafting-derived {@code generateTags} and the wand special
     * case belong to the crafting/equipment milestones (TODOs in
     * docs/aspects.md).
     */
    public static AspectList getObjectTags(final ItemStack stack) {
        ensureLoaded();
        if (stack.isEmpty()) {
            return null;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        final int meta = variantMeta(stack);

        ItemTag found = findItemTag(id, meta);
        if (found == null) {
            found = findItemTag(id, 32767);
        }
        if (found == null && meta == 32767) {
            // Original quirk: only a wildcard query walks metas 0..15.
            for (int index = 0; index < 16 && found == null; index++) {
                found = findItemTag(id, index);
            }
        }
        return found == null ? null : toAspectList(found.aspects());
    }

    private static ItemTag findItemTag(final ResourceLocation id, final int meta) {
        for (final ItemTag tag : itemTagsFor(id)) {
            if (tag.meta() == meta) {
                return tag;
            }
        }
        return null;
    }

    /**
     * The 1.7.10 damage/meta of a modern stack. TC's own meta-variant items
     * (nugget/essence/shard/...) register with their original meta split when
     * their content milestone lands; vanilla meta-flattened items stay 0 and
     * their original entries are reachable through the future variant resolver
     * (docs/aspects.md, "id and meta seams").
     */
    private static int variantMeta(final ItemStack stack) {
        return 0;
    }

    /**
     * Original {@code ScanManager.generateEntityAspects}: player special case
     * (MAN 4 + named easter eggs, else three random aspects seeded from the
     * name hash), otherwise the entity-tag table with NBT-condition matching.
     */
    public static AspectList getEntityAspects(final Entity entity) {
        ensureLoaded();
        if (entity instanceof Player player) {
            final String name = player.getGameProfile().getName();
            final AspectList tags = new AspectList();
            addByTag(tags, "humanus", 4);
            if (name.equalsIgnoreCase("azanor")) {
                addByTag(tags, "alienis", 20);
            } else if (name.equalsIgnoreCase("direwolf20")) {
                addByTag(tags, "bestia", 20);
            } else if (name.equalsIgnoreCase("pahimar")) {
                addByTag(tags, "permutatio", 20);
            } else {
                final Random random = new Random(("player_" + name).hashCode());
                final List<Aspect> all = Aspect.values();
                for (int i = 0; i < 3; i++) {
                    tags.add(all.get(random.nextInt(all.size())), 4);
                }
            }
            return tags;
        }

        final ResourceLocation typeId =
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CompoundTag entityData = null; // serialized lazily, only when a condition needs it
        for (final EntityTag tag : ENTITY_TAGS) {
            if (!tag.entity().equals(typeId)) {
                continue;
            }
            if (tag.condition() != null) {
                if (entityData == null) {
                    entityData = entity.saveWithoutId(new CompoundTag());
                }
                if (!tag.condition().matches(entityData)) {
                    continue;
                }
            }
            return toAspectList(tag.aspects());
        }
        return null;
    }

    // ------------------------------------------------------------- helpers

    static AspectList toAspectList(final Map<String, Integer> aspects) {
        final AspectList list = new AspectList();
        for (final Map.Entry<String, Integer> entry : aspects.entrySet()) {
            final Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null) {
                list.add(aspect, entry.getValue());
            } else {
                LOGGER.warn("essentia table references unknown aspect tag {}", entry.getKey());
            }
        }
        return list;
    }

    private static void addByTag(final AspectList list, final String tag, final int amount) {
        final Aspect aspect = Aspect.getAspect(tag);
        if (aspect != null) {
            list.add(aspect, amount);
        }
    }

    /** Lowercase legacy id parse used by the loader. */
    static ResourceLocation parseId(final String raw) {
        final int colon = raw.indexOf(':');
        final String namespace = colon < 0
                ? "minecraft"
                : raw.substring(0, colon).toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(
                namespace, raw.substring(colon + 1).toLowerCase(Locale.ROOT));
    }
}
