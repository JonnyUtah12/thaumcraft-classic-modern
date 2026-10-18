package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of the server's player knowledge, populated by the S2C
 * sync payloads in {@link ModNetwork}. This is the client copy the original
 * built on the fly inside its {@code @SideOnly(Side.CLIENT)} handlers (the
 * client half of {@code PlayerKnowledge} plus {@code GuiResearchBrowser
 * .completedResearch}); the authoritative store lives server-side and arrives
 * with the persistence milestone (TC-M2-02), which will back this mirror.
 *
 * <p>Client-only: never loaded on a dedicated server (only client handlers
 * reference it).</p>
 */
public final class ClientNetworkData {

    private static final Set<String> COMPLETED_RESEARCH = new LinkedHashSet<>();
    private static final Set<String> SCANNED_ITEMS = new LinkedHashSet<>();
    private static final Set<String> SCANNED_ENTITIES = new LinkedHashSet<>();
    private static final Set<String> SCANNED_PHENOMENA = new LinkedHashSet<>();
    private static final Map<String, Integer> DISCOVERED_ASPECTS = new HashMap<>();
    private static final Map<String, Integer> ASPECT_POOLS = new HashMap<>();
    private static final Map<Integer, int[]> RUNIC_CHARGES = new HashMap<>();
    private static final Map<Integer, Integer> WARP = new HashMap<>();

    private ClientNetworkData() {
    }

    /** Drop everything: sent before a full re-sync and on client login. */
    public static void wipe() {
        COMPLETED_RESEARCH.clear();
        SCANNED_ITEMS.clear();
        SCANNED_ENTITIES.clear();
        SCANNED_PHENOMENA.clear();
        DISCOVERED_ASPECTS.clear();
        ASPECT_POOLS.clear();
        RUNIC_CHARGES.clear();
        WARP.clear();
    }

    public static void replaceResearch(final Iterable<String> entries) {
        COMPLETED_RESEARCH.clear();
        for (String entry : entries) {
            COMPLETED_RESEARCH.add(entry);
        }
    }

    public static void addCompletedResearch(final String key) {
        COMPLETED_RESEARCH.add(key);
    }

    public static Set<String> completedResearch() {
        return Set.copyOf(COMPLETED_RESEARCH);
    }

    public static void replaceScannedItems(final Iterable<String> entries) {
        SCANNED_ITEMS.clear();
        for (String entry : entries) {
            SCANNED_ITEMS.add(entry);
        }
    }

    public static Set<String> scannedItems() {
        return Set.copyOf(SCANNED_ITEMS);
    }

    public static void replaceScannedEntities(final Iterable<String> entries) {
        SCANNED_ENTITIES.clear();
        for (String entry : entries) {
            SCANNED_ENTITIES.add(entry);
        }
    }

    public static Set<String> scannedEntities() {
        return Set.copyOf(SCANNED_ENTITIES);
    }

    public static void replaceScannedPhenomena(final Iterable<String> entries) {
        SCANNED_PHENOMENA.clear();
        for (String entry : entries) {
            SCANNED_PHENOMENA.add(entry);
        }
    }

    public static Set<String> scannedPhenomena() {
        return Set.copyOf(SCANNED_PHENOMENA);
    }

    public static void replaceDiscoveredAspects(final Map<String, Integer> amounts) {
        DISCOVERED_ASPECTS.clear();
        DISCOVERED_ASPECTS.putAll(amounts);
    }

    public static void addDiscoveredAspect(final String tag, final int amount) {
        DISCOVERED_ASPECTS.merge(tag, amount, Integer::sum);
    }

    public static Map<String, Integer> discoveredAspects() {
        return Map.copyOf(DISCOVERED_ASPECTS);
    }

    public static void setAspectPool(final String tag, final int total) {
        ASPECT_POOLS.put(tag, total);
    }

    public static Map<String, Integer> aspectPools() {
        return Map.copyOf(ASPECT_POOLS);
    }

    public static void setWarp(final int warpKind, final int value) {
        WARP.put(warpKind, value);
    }

    public static int warp(final int warpKind) {
        return WARP.getOrDefault(warpKind, 0);
    }

    public static int warpPermanent() {
        return warp(SyncWarpPayload.WARP_PERMANENT);
    }

    public static void setRunicCharge(final int playerId, final int charge, final int maxCharge) {
        RUNIC_CHARGES.put(playerId, new int[] {charge, maxCharge});
    }

    public static int[] runicCharge(final int playerId) {
        return RUNIC_CHARGES.get(playerId);
    }

    /** Snapshot helper for diagnostics and tests. */
    public static Set<String> knownAspectTags() {
        return new HashSet<>(new ArrayList<>(DISCOVERED_ASPECTS.keySet()));
    }
}
