package com.jonnyutah12.thaumcraftclassic.player;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-player Thaumcraft knowledge: completed research, discovered aspects, and scan
 * records. Field names and NBT tag names are the exact originals from Thaumcraft
 * 4.2.3.5 {@code ResearchManager.saveResearchNBT/saveAspectNBT/saveScannedNBT}
 * (design record: docs/architecture/persistence.md), so a future importer for legacy
 * {@code .thaum} player files can reuse this codec unchanged.
 *
 * <p>Carried as a NeoForge data attachment (see {@link ModAttachments}) with
 * {@code copyOnDeath} and automatic S2C sync. The codec is lenient: every field is
 * optional (defaults to empty) and unknown fields are ignored, which is the
 * forward-compatibility contract for this milestone.</p>
 *
 * <p>Immutable: mutation helpers return updated copies.</p>
 */
public record PlayerKnowledgeData(
        List<KeyEntry> research,
        List<AspectEntry> aspectsDiscovered,
        List<KeyEntry> scannedObjects,
        List<KeyEntry> scannedEntities,
        List<KeyEntry> scannedPhenomena) {

    // Original TC4 save tags (ResearchManager constants).
    public static final String TAG_RESEARCH = "THAUMCRAFT.RESEARCH";
    public static final String TAG_ASPECTS = "THAUMCRAFT.ASPECTS";
    public static final String TAG_SCANNED_OBJECTS = "THAUMCRAFT.SCAN.OBJECTS";
    public static final String TAG_SCANNED_ENTITIES = "THAUMCRAFT.SCAN.ENTITIES";
    public static final String TAG_SCANNED_PHENOMENA = "THAUMCRAFT.SCAN.PHENOMENA";

    public static final Codec<PlayerKnowledgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KeyEntry.CODEC.listOf().optionalFieldOf(TAG_RESEARCH, List.of()).forGetter(PlayerKnowledgeData::research),
            AspectEntry.CODEC.listOf().optionalFieldOf(TAG_ASPECTS, List.of()).forGetter(PlayerKnowledgeData::aspectsDiscovered),
            KeyEntry.CODEC.listOf().optionalFieldOf(TAG_SCANNED_OBJECTS, List.of()).forGetter(PlayerKnowledgeData::scannedObjects),
            KeyEntry.CODEC.listOf().optionalFieldOf(TAG_SCANNED_ENTITIES, List.of()).forGetter(PlayerKnowledgeData::scannedEntities),
            KeyEntry.CODEC.listOf().optionalFieldOf(TAG_SCANNED_PHENOMENA, List.of()).forGetter(PlayerKnowledgeData::scannedPhenomena)
    ).apply(instance, PlayerKnowledgeData::new));

    public PlayerKnowledgeData {
        research = research == null ? List.of() : List.copyOf(research);
        aspectsDiscovered = aspectsDiscovered == null ? List.of() : List.copyOf(aspectsDiscovered);
        scannedObjects = scannedObjects == null ? List.of() : List.copyOf(scannedObjects);
        scannedEntities = scannedEntities == null ? List.of() : List.copyOf(scannedEntities);
        scannedPhenomena = scannedPhenomena == null ? List.of() : List.copyOf(scannedPhenomena);
    }

    public static PlayerKnowledgeData empty() {
        return new PlayerKnowledgeData(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** Original entry shape: {@code {key: "..."}}, matching TC4's NBTTagList records. */
    public record KeyEntry(String key) {
        public static final Codec<KeyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(KeyEntry::key)
        ).apply(instance, KeyEntry::new));
    }

    /** Original entry shape: {@code {key: aspectTag, amount: short}} (TC4 writes shorts). */
    public record AspectEntry(String key, short amount) {
        public static final Codec<AspectEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(AspectEntry::key),
                Codec.SHORT.fieldOf("amount").forGetter(AspectEntry::amount)
        ).apply(instance, AspectEntry::new));
    }

    // ------------------------------------------------------------ query views

    public Set<String> researchKeys() {
        return keysToSet(research);
    }

    public boolean knowsResearch(final String key) {
        return researchKeys().contains(key);
    }

    public Set<String> scannedObjectKeys() {
        return keysToSet(scannedObjects);
    }

    public Set<String> scannedEntityKeys() {
        return keysToSet(scannedEntities);
    }

    public Set<String> scannedPhenomenaKeys() {
        return keysToSet(scannedPhenomena);
    }

    public int aspectAmount(final String aspectTag) {
        for (final AspectEntry entry : aspectsDiscovered) {
            if (entry.key().equals(aspectTag)) {
                return entry.amount();
            }
        }
        return 0;
    }

    /**
     * TC4 parity: {@code PlayerKnowledge.hasDiscoveredAspect} is key presence
     * in the discovered map — a discovered aspect can legitimately hold pool
     * amount 0 (the crucible's "not fully discovered" check keys off that).
     * The original also accepts the lowercased tag form.
     */
    public boolean hasAspectDiscovered(final String aspectTag) {
        return aspectAmount(aspectTag) != 0 || hasAspectKey(aspectTag)
                || hasAspectKey(aspectTag.toLowerCase(Locale.ROOT));
    }

    private boolean hasAspectKey(final String aspectTag) {
        for (final AspectEntry entry : aspectsDiscovered) {
            if (entry.key().equals(aspectTag)) {
                return true;
            }
        }
        return false;
    }

    /** TC4 parity: {@code PlayerKnowledge.hasDiscoveredParentAspects}. */
    public boolean hasDiscoveredParentAspects(final Aspect aspect) {
        if (aspect.isPrimal()) {
            return true;
        }
        final List<Aspect> components = aspect.getComponents();
        if (components == null) {
            return false;
        }
        return hasAspectDiscovered(components.get(0).getTag())
                && hasAspectDiscovered(components.get(1).getTag());
    }

    // -------------------------------------------------- functional mutators

    public PlayerKnowledgeData withResearchCompleted(final String key) {
        if (knowsResearch(key)) {
            return this;
        }
        final ArrayList<KeyEntry> next = new ArrayList<>(research);
        next.add(new KeyEntry(key));
        return new PlayerKnowledgeData(next, aspectsDiscovered, scannedObjects, scannedEntities, scannedPhenomena);
    }

    /**
     * TC4 parity: {@code PlayerKnowledge.addAspectPool} on the single
     * discovered/pool map. Amount 0 never changes anything; a negative amount
     * only succeeds while the pool is positive (reduce), and a reduced-to-zero
     * aspect stays in the map — discovery is never undone by pool changes.
     * Returns {@code this} when nothing changed, so callers can detect the
     * original's boolean return by identity.
     */
    public PlayerKnowledgeData withAspectPoolAdded(final String aspectTag, final int amount) {
        if (aspectTag == null || aspectTag.isEmpty() || amount == 0) {
            return this;
        }
        final int current = aspectAmount(aspectTag);
        if (amount < 0 && current <= 0) {
            return this;
        }
        final int merged = current + amount;
        final short clamped = (short) Math.clamp(merged, Short.MIN_VALUE, Short.MAX_VALUE);
        final ArrayList<AspectEntry> next = new ArrayList<>();
        for (final AspectEntry entry : aspectsDiscovered) {
            if (!entry.key().equals(aspectTag)) {
                next.add(entry);
            }
        }
        next.add(new AspectEntry(aspectTag, clamped));
        return new PlayerKnowledgeData(research, next, scannedObjects, scannedEntities, scannedPhenomena);
    }

    /**
     * Accumulates a pool amount (TC4 {@code PlayerKnowledge.addAspectPool}
     * positive branch); the entry stays present at amount 0 so discovery and
     * pool stay distinguishable, as in the original.
     */
    public PlayerKnowledgeData withAspectDiscovered(final String aspectTag, final int amount) {
        final int merged = aspectAmount(aspectTag) + amount;
        final short clamped = (short) Math.clamp(merged, Short.MIN_VALUE, Short.MAX_VALUE);
        final ArrayList<AspectEntry> next = new ArrayList<>();
        for (final AspectEntry entry : aspectsDiscovered) {
            if (!entry.key().equals(aspectTag)) {
                next.add(entry);
            }
        }
        next.add(new AspectEntry(aspectTag, clamped));
        return new PlayerKnowledgeData(research, next, scannedObjects, scannedEntities, scannedPhenomena);
    }

    public PlayerKnowledgeData withScannedObject(final String key) {
        if (scannedObjectKeys().contains(key)) {
            return this;
        }
        final ArrayList<KeyEntry> next = new ArrayList<>(scannedObjects);
        next.add(new KeyEntry(key));
        return new PlayerKnowledgeData(research, aspectsDiscovered, next, scannedEntities, scannedPhenomena);
    }

    public PlayerKnowledgeData withScannedEntity(final String key) {
        if (scannedEntityKeys().contains(key)) {
            return this;
        }
        final ArrayList<KeyEntry> next = new ArrayList<>(scannedEntities);
        next.add(new KeyEntry(key));
        return new PlayerKnowledgeData(research, aspectsDiscovered, scannedObjects, next, scannedPhenomena);
    }

    public PlayerKnowledgeData withScannedPhenomena(final String key) {
        if (scannedPhenomenaKeys().contains(key)) {
            return this;
        }
        final ArrayList<KeyEntry> next = new ArrayList<>(scannedPhenomena);
        next.add(new KeyEntry(key));
        return new PlayerKnowledgeData(research, aspectsDiscovered, scannedObjects, scannedEntities, next);
    }

    private static Set<String> keysToSet(final List<KeyEntry> entries) {
        final Set<String> keys = new LinkedHashSet<>();
        for (final KeyEntry entry : entries) {
            keys.add(entry.key());
        }
        return keys;
    }
}
