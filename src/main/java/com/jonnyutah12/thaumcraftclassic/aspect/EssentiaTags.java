package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

    // ------------------------------------------------- bonus tags (TC-M4-06)

    /**
     * Vanilla enchantment id -> aspect tag merged at the enchantment's level,
     * from the original {@code getBonusTags}'s enchantment-id chain. The srg
     * {@code Enchantment.field_*} constants were decoded through
     * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}; the
     * aspect tags through {@code api/aspects/Aspect.java}. The two Thaumcraft
     * enchantments ({@code Config.enchHaste} -> motus, {@code Config.enchRepair}
     * -> instrumentum) keep their ids here and stay dormant until those
     * enchantments are registered by their own card.
     */
    private static final Map<ResourceLocation, String> ENCHANT_BONUS_ASPECTS = Map.ofEntries(
            Map.entry(mc("aqua_affinity"), "aqua"),
            Map.entry(mc("bane_of_arthropods"), "bestia"),
            Map.entry(mc("blast_protection"), "tutamen"),
            Map.entry(mc("efficiency"), "instrumentum"),
            Map.entry(mc("feather_falling"), "volatus"),
            Map.entry(mc("fire_aspect"), "ignis"),
            Map.entry(mc("fire_protection"), "tutamen"),
            Map.entry(mc("flame"), "ignis"),
            Map.entry(mc("fortune"), "lucrum"),
            Map.entry(mc("infinity"), "fabrico"),
            Map.entry(mc("knockback"), "aer"),
            Map.entry(mc("looting"), "lucrum"),
            Map.entry(mc("power"), "telum"),
            Map.entry(mc("projectile_protection"), "tutamen"),
            Map.entry(mc("protection"), "tutamen"),
            Map.entry(mc("punch"), "aer"),
            Map.entry(mc("respiration"), "aer"),
            Map.entry(mc("sharpness"), "telum"),
            Map.entry(mc("silk_touch"), "permutatio"),
            Map.entry(mc("thorns"), "telum"),
            Map.entry(mc("smite"), "perditio"),
            Map.entry(mc("unbreaking"), "terra"),
            Map.entry(mc("luck_of_the_sea"), "lucrum"),
            Map.entry(mc("lure"), "bestia"),
            Map.entry(tc("haste"), "motus"),
            Map.entry(tc("repair"), "instrumentum"));

    private static ResourceLocation mc(final String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation tc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    /**
     * Port of {@code ThaumcraftCraftingManager.getBonusTags} (TC-M4-06): the
     * carried-aspect bonus of an item being smelted — its own container
     * contents, the source tags, gear-class bonuses and enchantment bonuses,
     * culled. The original's gear checks key off 1.7.10 item classes; the
     * modern equivalents are one-for-one (ItemArmor -> ArmorItem, ItemSword ->
     * SwordItem, ItemPickaxe/ItemTool -> PickaxeItem/DiggerItem, ItemShears ->
     * ShearsItem). The sword bonus reads the stack's total attack damage
     * (base 1 + ADD_VALUE modifiers) because 1.7.10's {@code getDamage()} was
     * that total; the shears bonus keeps the original durability thresholds,
     * which are the same numbers as the modern {@link Tiers} use-counts.
     */
    public static AspectList getBonusTags(final ItemStack stack, final AspectList sourceTags) {
        AspectList tmp = new AspectList();
        final Item item = stack.getItem();
        if (item instanceof IEssentiaContainerItem container) {
            tmp = container.getAspects(stack);
            if (tmp != null && tmp.size() > 0) {
                for (final Aspect tag : tmp.copy().getAspects()) {
                    if (tmp.getAmount(tag) <= 0) {
                        tmp.remove(tag);
                    }
                }
            }
        }
        if (tmp == null) {
            tmp = new AspectList();
        }

        if (sourceTags != null) {
            for (final Aspect tagx : sourceTags.getAspects()) {
                if (tagx != null) {
                    tmp.add(tagx, sourceTags.getAmount(tagx));
                }
            }
        }

        if (item != null) {
            if (item instanceof ArmorItem armor) {
                tmp.merge(getAspectOrSkip("tutamen"), armor.getDefense());
            } else if (item instanceof SwordItem && attackDamageOf(stack) + 1.0F > 0.0F) {
                tmp.merge(getAspectOrSkip("telum"), (int) (attackDamageOf(stack) + 1.0F));
            } else if (item instanceof BowItem) {
                tmp.merge(getAspectOrSkip("telum"), 3).merge(getAspectOrSkip("volatus"), 1);
            } else if (item instanceof PickaxeItem) {
                tmp.merge(getAspectOrSkip("perfodio"), ((TieredItem) item).getTier().getTier() + 1);
            } else if (item instanceof DiggerItem) {
                tmp.merge(getAspectOrSkip("instrumentum"), ((TieredItem) item).getTier().getTier() + 1);
            } else if (item instanceof ShearsItem || item instanceof HoeItem) {
                final int durability = stack.getMaxDamage();
                if (durability <= Tiers.WOOD.getUses()) {
                    tmp.merge(getAspectOrSkip("meto"), 1);
                } else if (durability <= Tiers.STONE.getUses() || durability <= Tiers.GOLD.getUses()) {
                    tmp.merge(getAspectOrSkip("meto"), 2);
                } else if (durability <= Tiers.IRON.getUses()) {
                    tmp.merge(getAspectOrSkip("meto"), 3);
                } else {
                    tmp.merge(getAspectOrSkip("meto"), 4);
                }
            }

            ItemEnchantments ench = null;
            if (item instanceof ItemEnchantedBook) {
                ench = stack.get(DataComponents.STORED_ENCHANTMENTS);
            } else {
                ench = stack.getEnchantments();
            }

            if (ench != null && !ench.isEmpty()) {
                int totalLevels = 0;
                for (final Map.Entry<Holder<Enchantment>, Integer> entry : ench.entrySet()) {
                    final ResourceLocation enchId = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey().value());
                    final String aspectTag = enchId == null ? null : ENCHANT_BONUS_ASPECTS.get(enchId);
                    if (aspectTag != null) {
                        tmp.merge(getAspectOrSkip(aspectTag), entry.getValue());
                    }
                    totalLevels += entry.getValue();
                }
                if (totalLevels > 0) {
                    tmp.merge(getAspectOrSkip("praecantatio"), totalLevels);
                }
            }
        }

        return AspectCombinations.cullTags(tmp);
    }

    /** Total melee attack damage of the stack: base 1 + ADD_VALUE modifiers. */
    private static double attackDamageOf(final ItemStack stack) {
        double damage = 1.0D;
        final ItemAttributeModifiers modifiers =
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (final ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute() == Attributes.ATTACK_DAMAGE
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage += entry.modifier().amount();
            }
        }
        return damage;
    }

    private static Aspect getAspectOrSkip(final String tag) {
        final Aspect aspect = Aspect.getAspect(tag);
        if (aspect == null) {
            LOGGER.warn("bonus tags reference unknown aspect tag {}", tag);
        }
        return aspect;
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
