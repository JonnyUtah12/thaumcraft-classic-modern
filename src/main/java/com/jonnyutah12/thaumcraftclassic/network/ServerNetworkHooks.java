package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * Server-side action sinks for C2S payloads. The systems that own these
 * actions (research table GUI state, wand/equipment behavior, sensor tiles)
 * arrive with their feature milestones; until then each remaining hook is a
 * stub that logs at debug level and carries a TODO naming the milestone that
 * implements the real behavior. The payload plumbing around them is final —
 * only these bodies change.
 *
 * <p>The scan and aspect-combination sinks are live as of TC-M4-01: they run
 * the ported {@code ScanManager}/{@code PacketAspectCombinationToServer}
 * semantics (docs/aspects.md). The original's {@code ResearchManager.
 * scheduleSave} has no port counterpart because attachment state persists with
 * the player save automatically.</p>
 */
public final class ServerNetworkHooks {

    private static final Logger LOGGER = ThaumcraftClassic.LOGGER;

    private ServerNetworkHooks() {
    }

    /**
     * TC-M4-01: port of {@code ScanManager.completeScan} — generate the
     * scanned target's aspects, gate on {@code validScan}, record the scan,
     * and feed the aspects through the discovery/pool rules. The original's
     * {@code completeScannedObject/Entity/Phenomena} research-page completion
     * is a TODO for the research-table milestone (the graph has no scan
     * entries yet); {@code getBonusTags} (crafting-derived bonus aspects) is
     * a TODO for the crafting milestone; NODE phenomena need the node system.
     */
    static void completeScan(final ScannedToServerPayload payload, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final String prefix = payload.prefix();
        AspectList aspects = null;
        String scanKey = null;
        boolean entityScan = false;
        boolean phenomenaScan = false;

        switch (payload.scanType()) {
            case 1 -> {
                final Item item = BuiltInRegistries.ITEM.get(payload.scannedId());
                if (item == Items.AIR) {
                    return;
                }
                // TODO(crafting milestone): getBonusTags for crafted items.
                aspects = EssentiaTags.getObjectTags(new ItemStack(item));
                scanKey = prefix + payload.scannedId()
                        + (payload.data() != 0 ? ":" + payload.data() : "");
            }
            case 2 -> {
                final Entity target = serverPlayer.level().getEntity(payload.targetEntityId());
                if (target == null) {
                    return;
                }
                if (target instanceof ItemEntity itemEntity) {
                    final ItemStack stack = itemEntity.getItem().copy();
                    stack.setCount(1);
                    aspects = EssentiaTags.getObjectTags(stack);
                    scanKey = prefix
                            + BuiltInRegistries.ITEM.getKey(stack.getItem());
                } else {
                    aspects = EssentiaTags.getEntityAspects(target);
                    scanKey = prefix
                            + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                    entityScan = true;
                }
            }
            case 3 -> {
                if (payload.phenomena() != null && payload.phenomena().startsWith("NODE")) {
                    // TODO(node milestone): generateNodeAspects for elemental nodes.
                    LOGGER.debug("node scan from {}: {}", serverPlayer.getGameProfile().getName(),
                            payload.phenomena());
                    return;
                }
                scanKey = prefix + payload.phenomena();
                phenomenaScan = true;
            }
            default -> {
                return;
            }
        }

        PlayerKnowledgeData knowledge = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (!AspectKnowledge.validScan(aspects, knowledge)) {
            return;
        }

        // The original records the scan through ResearchManager.completeScanned*;
        // the knowledge record itself is this milestone's part. Scan-key format is
        // prefix + modern id (docs/aspects.md); legacy hash import belongs to the
        // scan-entry lane of the research-table milestone.
        // TODO(research-table milestone): completeScanned* page unlocks.
        knowledge = phenomenaScan ? knowledge.withScannedPhenomena(scanKey)
                : entityScan ? knowledge.withScannedEntity(scanKey)
                : knowledge.withScannedObject(scanKey);
        serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);

        // Original: scannedByThaumometer = prefix "#" && the "@" scan already
        // exists — a registry scan of an already-thaumometered target grants 1.
        final String otherKey = "@" + scanKey.substring(Math.min(prefix.length(), scanKey.length()));
        final boolean scannedByThaumometer = "#".equals(prefix)
                && (phenomenaScan ? knowledge.scannedPhenomenaKeys().contains(otherKey)
                : entityScan ? knowledge.scannedEntityKeys().contains(otherKey)
                : knowledge.scannedObjectKeys().contains(otherKey));

        AspectKnowledge.applyScanKnowledge(serverPlayer, aspects,
                scannedByThaumometer, "#".equals(prefix));
    }

    /**
     * TC-M4-01: port of {@code PacketAspectCombinationToServer.onMessage}.
     * The research-table bonus-aspect branch (consuming a bonus copy from
     * {@code TileResearchTable.bonusAspects}) is a TODO for the research-table
     * milestone; the pool arithmetic and discovery rules are exact.
     */
    static void combineAspects(final AspectCombinationToServerPayload payload, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final Aspect aspect1 = Aspect.getAspect(payload.aspect1());
        final Aspect aspect2 = Aspect.getAspect(payload.aspect2());
        if (aspect1 == null || aspect2 == null) {
            return;
        }
        final Aspect combo = AspectCombinations.getCombinationResult(aspect1, aspect2);

        PlayerKnowledgeData knowledge = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final int pool1 = knowledge.aspectAmount(aspect1.getTag());
        final int pool2 = knowledge.aspectAmount(aspect2.getTag());
        if (!((pool1 > 0 || payload.bonus1()) && (pool2 > 0 || payload.bonus2()))) {
            return;
        }

        if (pool1 > 0 || !payload.bonus1()) {
            knowledge = knowledge.withAspectPoolAdded(aspect1.getTag(), -1);
            serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            PacketDistributor.sendToPlayer(serverPlayer, new AspectPoolPayload(
                    aspect1.getTag(), 0, knowledge.aspectAmount(aspect1.getTag())));
        }
        // TODO(research-table milestone): else consume TileResearchTable.bonusAspects.

        if (pool2 > 0 || !payload.bonus2()) {
            knowledge = knowledge.withAspectPoolAdded(aspect2.getTag(), -1);
            serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            PacketDistributor.sendToPlayer(serverPlayer, new AspectPoolPayload(
                    aspect2.getTag(), 0, knowledge.aspectAmount(aspect2.getTag())));
        }
        // TODO(research-table milestone): else consume TileResearchTable.bonusAspects.

        if (combo != null) {
            AspectKnowledge.checkAndSyncAspectKnowledge(serverPlayer, combo, 1);
        }
    }

    /** TODO(M4 research table): place aspect on the research table grid, per original PacketAspectPlaceToServer. */
    static void placeAspect(final AspectPlaceToServerPayload payload, final Player player) {
        LOGGER.debug("aspect place from {} at {} ({},{}): {}", player.getGameProfile().getName(),
                payload.tablePos(), payload.q(), payload.r(), payload.aspectTag().orElse("(none)"));
    }

    /**
     * TC-M3-02: the Thaumonomicon completion request, per original
     * {@code PacketPlayerCompleteToServer.onMessage} — the research manager owns
     * the semantics (prerequisite gate, aspect-pool payment, completion, siblings).
     */
    static void completeResearchRequest(final PlayerCompleteToServerPayload payload, final Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchManager.handlePlayerCompleteRequest(payload, serverPlayer);
        }
    }

    /** TODO(M3 equipment/hover): hover flight toggle, per original PacketFlyToServer. */
    static void setHover(final boolean hover, final Player player) {
        LOGGER.debug("hover toggle from {}: {}", player.getGameProfile().getName(), hover);
    }

    /** TODO(M3 equipment/wands): wand focus change, per original PacketFocusChangeToServer. */
    static void changeFocus(final String focus, final Player player) {
        LOGGER.debug("focus change from {}: {}", player.getGameProfile().getName(), focus);
    }

    /** TODO(M3 equipment): item-bound key actions, per original PacketItemKeyToServer. */
    static void itemKey(final int key, final Player player) {
        LOGGER.debug("item key from {}: {}", player.getGameProfile().getName(), key);
    }

    /** TODO(M5 tiles/sensors): answer a sensor note query, per original PacketNote server side. */
    static void answerNoteQuery(final NotePayload payload, final Player player) {
        LOGGER.debug("note query from {} at {}", player.getGameProfile().getName(), payload.pos());
    }
}
