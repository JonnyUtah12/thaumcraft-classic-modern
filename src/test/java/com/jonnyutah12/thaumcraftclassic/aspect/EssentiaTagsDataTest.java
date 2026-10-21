package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-01 acceptance: the item/entity essentia tag tables are data-driven
 * and complete per the TC-M0-04 inventory.
 *
 * <p>The shipped {@code data/thaumcraft/essentia/tags.json} is generated
 * 1:1 from the original {@code ConfigAspects.java} by
 * {@code scripts/extract_essentia_tags.py}. This test pins the frozen
 * entry counts, spot-checks entries against the original registrations,
 * cross-checks every non-vanilla target id against the content-mapping id
 * contract (docs/content-mapping.csv), and verifies the loader's format
 * contract rejects broken files.</p>
 */
class EssentiaTagsDataTest {

    /** Frozen counts from the original ConfigAspects.java (TC-M0-04 baseline):
     *  287 item entries (incl. the dyes/wisp/potion loop expansions),
     *  56 ore-dict entries, 116 entity registrations. */
    private static final int EXPECTED_ITEM_TAGS = 287;
    private static final int EXPECTED_OREDICT_TAGS = 56;
    private static final int EXPECTED_ENTITY_TAGS = 116;

    private static EssentiaTagDataLoader.Tables loadShipped() {
        return EssentiaTagDataLoader.loadResource(EssentiaTagDataLoader.RESOURCE);
    }

    @Test
    void shippedTablesMatchTheOriginalEntryCounts() {
        final EssentiaTagDataLoader.Tables tables = loadShipped();
        assertEquals(EXPECTED_ITEM_TAGS, tables.itemTags().size());
        assertEquals(EXPECTED_OREDICT_TAGS, tables.oredictTags().size());
        assertEquals(EXPECTED_ENTITY_TAGS, tables.entityTags().size());
    }

    @Test
    void everyReferencedAspectTagIsValid() {
        final Set<String> valid = new HashSet<>();
        for (final AspectDefinitions.Definition def : AspectDefinitions.all()) {
            valid.add(def.tag());
        }
        assertEquals(48, valid.size());
        final EssentiaTagDataLoader.Tables tables = loadShipped();
        for (final EssentiaTags.ItemTag tag : tables.itemTags()) {
            assertTrue(valid.containsAll(tag.aspects().keySet()), "item " + tag.item() + " has unknown aspect");
            // Empty lists exist in the original (e.g. itemEssence meta 1 and
            // itemCrystalEssence are registered with an empty AspectList,
            // sandstone meta 32767 with the complex flag); they are faithful,
            // not corruption.
        }
        for (final EssentiaTags.OreDictTag tag : tables.oredictTags()) {
            assertTrue(valid.containsAll(tag.aspects().keySet()), "oredict " + tag.name() + " has unknown aspect");
        }
        for (final EssentiaTags.EntityTag tag : tables.entityTags()) {
            assertTrue(valid.containsAll(tag.aspects().keySet()), "entity " + tag.entity() + " has unknown aspect");
        }
    }

    @Test
    void everyAspectTagIsReachableThroughTheTables() {
        // Completeness in the other direction: the original ConfigAspects
        // references every one of the 48 aspects somewhere.
        final Set<String> referenced = new HashSet<>();
        final EssentiaTagDataLoader.Tables tables = loadShipped();
        tables.itemTags().forEach(t -> referenced.addAll(t.aspects().keySet()));
        tables.oredictTags().forEach(t -> referenced.addAll(t.aspects().keySet()));
        tables.entityTags().forEach(t -> referenced.addAll(t.aspects().keySet()));
        final Set<String> all = new HashSet<>();
        AspectDefinitions.all().forEach(d -> all.add(d.tag()));
        assertEquals(all, referenced, "every original aspect appears in at least one essentia entry");
    }

    @Test
    void spotChecksMatchTheOriginalConfigAspectsRegistrations() {
        final EssentiaTagDataLoader.Tables tables = loadShipped();

        // registerObjectTag("stone", EARTH 2) / ("cobblestone", EARTH 1 + ENTROPY 1)
        assertEquals(Map.of("terra", 2), oredict(tables, "stone").aspects());
        assertEquals(Map.of("terra", 1, "perditio", 1), oredict(tables, "cobblestone").aspects());
        // registerObjectTag("logWood", TREE 4)
        assertEquals(Map.of("arbor", 4), oredict(tables, "logWood").aspects());
        // Blocks.dirt wildcard EARTH 2; podzol (meta 2) EARTH 1 + PLANT 1
        assertEquals(Map.of("terra", 2), item(tables, "minecraft:dirt", 32767).aspects());
        assertEquals(Map.of("terra", 1, "herba", 1), item(tables, "minecraft:dirt", 2).aspects());

        // registerEntityTag("Cow", BEAST 3 + EARTH 3)
        assertEquals(Map.of("bestia", 3, "terra", 3),
                entity(tables, "minecraft:cow", null, null).aspects());
        // Wither-skeleton variant: SkeletonType==1 -> UNDEAD 4 + MAN 1 + FIRE 2
        final EssentiaTags.EntityTag witherVariant =
                entity(tables, "minecraft:skeleton", "SkeletonType", (byte) 1);
        assertEquals(Map.of("exanimis", 4, "humanus", 1, "ignis", 2), witherVariant.aspects());
        assertEquals("byte", witherVariant.condition().type());
        assertEquals((byte) 1, witherVariant.condition().value());
        // Wisp loop expanded to per-type entries: Type=="aer" -> AER 2 + MAGIC 1 + AER 1
        final EssentiaTags.EntityTag wisp = entity(tables, "thaumcraft:wisp", "Type", "aer");
        assertEquals(Map.of("aer", 3, "praecantatio", 1), wisp.aspects());
        assertEquals("string", wisp.condition().type());
        assertEquals("aer", wisp.condition().value());
    }

    @Test
    void nbtConditionMatchingFollowsTheOriginalByteAndStringRules() {
        final EssentiaTags.NbtCondition byteCondition =
                new EssentiaTags.NbtCondition("SkeletonType", "byte", (byte) 1);
        final CompoundTag wither = new CompoundTag();
        wither.putByte("SkeletonType", (byte) 1);
        assertTrue(byteCondition.matches(wither));
        final CompoundTag skeleton = new CompoundTag();
        skeleton.putByte("SkeletonType", (byte) 0);
        assertFalse(byteCondition.matches(skeleton));
        assertFalse(byteCondition.matches(new CompoundTag()));

        final EssentiaTags.NbtCondition stringCondition =
                new EssentiaTags.NbtCondition("Type", "string", "aer");
        final CompoundTag wisp = new CompoundTag();
        wisp.putString("Type", "aer");
        assertTrue(stringCondition.matches(wisp));
        wisp.putString("Type", "ignis");
        assertFalse(stringCondition.matches(wisp));
    }

    @Test
    void nonVanillaTargetsResolveAgainstTheContentMappingIdContract() throws Exception {
        // Every thaumcraft-namespace target in the tables must correspond to a
        // TC-M0-04 inventory row (original ids keep their path; namespace and
        // path are matched case-insensitively against the CSV's original_id).
        final Set<String> csvIds = new HashSet<>();
        final Pattern row = Pattern.compile("^([^,]+),");
        final java.nio.file.Path csv = java.nio.file.Path.of(requiredProperty("tc.contentMappingFile"));
        for (final String line : java.nio.file.Files.readAllLines(csv)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("original_id,")) {
                continue;
            }
            final Matcher rowMatcher = row.matcher(line);
            final String id = rowMatcher.find() ? rowMatcher.group(1) : "";
            if (!id.isBlank()) {
                final int colon = id.indexOf(':');
                final String ns = colon < 0 ? "thaumcraft" : id.substring(0, colon).toLowerCase();
                final String path = (colon < 0 ? id : id.substring(colon + 1)).toLowerCase();
                csvIds.add(ns + ":" + path);
            }
        }
        assertFalse(csvIds.isEmpty(), "content-mapping CSV had no ids");

        final EssentiaTagDataLoader.Tables tables = loadShipped();
        for (final EssentiaTags.ItemTag tag : tables.itemTags()) {
            requireKnown(csvIds, tag.item());
        }
        for (final EssentiaTags.EntityTag tag : tables.entityTags()) {
            requireKnown(csvIds, tag.entity());
        }
    }

    private static void requireKnown(Set<String> csvIds, ResourceLocation id) {
        assertTrue("minecraft".equals(id.getNamespace()) || csvIds.contains(id.toString()),
                id + " is not covered by the content-mapping inventory");
    }

    @Test
    void loaderRejectsBrokenFormatContracts() {
        final String good = """
                {"formatVersion":1,"counts":{"aspects":48,"itemTags":1,"oredictTags":0,"entityTags":0},
                 "itemTags":[{"target":{"kind":"item","id":"stone","meta":0},"aspects":{"aer":1}}],
                 "entityTags":[]}""";
        assertNotNull(EssentiaTagDataLoader.load(new StringReader(good)));

        // Unsupported format version.
        assertThrows(IllegalStateException.class, () -> EssentiaTagDataLoader.load(
                new StringReader(good.replace("\"formatVersion\":1", "\"formatVersion\":2"))));
        // Unknown aspect tag.
        assertThrows(IllegalStateException.class, () -> EssentiaTagDataLoader.load(
                new StringReader(good.replace("\"aer\"", "\"nonsense\""))));
        // Declared counts must match the embedded arrays.
        assertThrows(IllegalStateException.class, () -> EssentiaTagDataLoader.load(
                new StringReader(good.replace("\"itemTags\":1", "\"itemTags\":2"))));
        // Unknown target kind.
        assertThrows(IllegalStateException.class, () -> EssentiaTagDataLoader.load(
                new StringReader(good.replace("\"kind\":\"item\"", "\"kind\":\"block\""))));
    }

    @Test
    void legacyIdParsingMatchesThePortContract() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("minecraft", "stone"),
                EssentiaTags.parseId("stone"));
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemnugget"),
                EssentiaTags.parseId("Thaumcraft:itemNugget"));
    }

    @Test
    void installPopulatesTheRuntimeStores() {
        final EssentiaTagDataLoader.Tables tables = loadShipped();
        EssentiaTags.install(tables.itemTags(), tables.oredictTags(), tables.entityTags());
        assertTrue(EssentiaTags.isLoaded());
        assertEquals(EXPECTED_ITEM_TAGS, EssentiaTags.itemTagCount());
        assertEquals(EXPECTED_OREDICT_TAGS, EssentiaTags.oredictTagCount());
        assertEquals(EXPECTED_ENTITY_TAGS, EssentiaTags.entityTagCount());
        assertEquals(List.of(), EssentiaTags.itemTagsFor(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "definitely_not_registered")));
        assertFalse(EssentiaTags.oredictTags("oreRedstone").isEmpty());
    }

    // ------------------------------------------------------------------ util

    private static EssentiaTags.OreDictTag oredict(EssentiaTagDataLoader.Tables tables, String name) {
        final List<EssentiaTags.OreDictTag> found = tables.oredictTags().stream()
                .filter(t -> t.name().equals(name)).toList();
        assertEquals(1, found.size(), "expected exactly one oredict entry for " + name);
        return found.get(0);
    }

    private static EssentiaTags.ItemTag item(EssentiaTagDataLoader.Tables tables, String id, int meta) {
        final ResourceLocation rl = ResourceLocation.parse(id);
        final List<EssentiaTags.ItemTag> found = tables.itemTags().stream()
                .filter(t -> t.item().equals(rl) && t.meta() == meta).toList();
        assertEquals(1, found.size(), "expected exactly one item entry for " + id + " meta " + meta);
        return found.get(0);
    }

    private static EssentiaTags.EntityTag entity(EssentiaTagDataLoader.Tables tables, String id,
                                                 String nbtName, Object nbtValue) {
        final ResourceLocation rl = ResourceLocation.parse(id);
        final List<EssentiaTags.EntityTag> found = tables.entityTags().stream()
                .filter(t -> t.entity().equals(rl)
                        && (nbtName == null
                        ? t.condition() == null
                        : t.condition() != null && t.condition().name().equals(nbtName)
                        && java.util.Objects.equals(t.condition().value(), nbtValue)))
                .toList();
        assertEquals(1, found.size(), "expected exactly one entity entry for " + id
                + (nbtName == null ? " (unconditional)" : " with NBT " + nbtName + "=" + nbtValue));
        return found.get(0);
    }

    private static String requiredProperty(String name) {
        final String value = System.getProperty(name);
        assertNotNull(value, "system property '" + name + "' is not set — run tests through ./gradlew test");
        return value;
    }
}
