package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side aspect knowledge: the discovery/pool rules applied when a scan
 * or a research-table combination reveals an aspect (TC-M4-01). This is the
 * port of {@code ScanManager.checkAndSyncAspectKnowledge} and
 * {@code ScanManager.validScan}, with the per-player state carried by
 * {@link PlayerKnowledgeData} (the port of {@code PlayerKnowledge}).
 *
 * <p>The original arithmetic is reproduced exactly: a first-time discovery
 * sends the discovery packet and adds a +2 bonus; once the pool reaches
 * {@code aspect_total_cap} further gains are square-root compressed, and at
 * 1.25x the cap multi-aspect gains collapse to 1. The research-page unlock
 * the original chains through {@code ResearchManager.completeAspect} belongs
 * to the research-table milestone (TODO below).</p>
 */
public final class AspectKnowledge {

    private AspectKnowledge() {
    }

    /**
     * Original {@code ScanManager.checkAndSyncAspectKnowledge}: returns the
     * amount actually saved (0 = nothing changed), and sends the S2C
     * discovery/pool packets the original sent.
     */
    public static int checkAndSyncAspectKnowledge(final ServerPlayer player,
                                                 final Aspect aspect,
                                                 final int amount) {
        final String tag = aspect.getTag();
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);

        final boolean firstDiscovery = !knowledge.hasAspectDiscovered(tag);
        int save = 0;
        if (firstDiscovery) {
            PacketDistributor.sendToPlayer(player, new AspectDiscoveryPayload(tag));
            // Original captures save at the +2 point, before the cap
            // compression mutates the amount further.
            save = amount + 2;
        }

        final int pool = knowledge.aspectAmount(tag);
        final int cap = TcConfig.getInt(TcConfigCatalog.ASPECT_TOTAL_CAP);
        final int effective = effectiveAmount(pool, amount, firstDiscovery, cap);

        final PlayerKnowledgeData updated = knowledge.withAspectPoolAdded(tag, effective);
        if (updated != knowledge) {
            knowledge = updated;
            player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            PacketDistributor.sendToPlayer(player,
                    new AspectPoolPayload(tag, effective, knowledge.aspectAmount(tag)));
            save = effective;
        }

        // TODO(research-table milestone): ResearchManager.completeAspect —
        // unlock research pages triggered by this aspect once the scan-entry
        // and research-completion lanes exist.
        return save;
    }

    /**
     * The pure arithmetic of the original {@code checkAndSyncAspectKnowledge}
     * amount path, extracted for unit testing: first discovery adds +2; once
     * the pool is at or above {@code cap} the gain is square-root compressed;
     * at 1.25x the cap any multi-point gain collapses to 1.
     */
    static int effectiveAmount(final int pool, final int amount,
                              final boolean firstDiscovery, final int cap) {
        int effective = amount;
        if (firstDiscovery) {
            effective += 2;
        }
        if (pool >= cap) {
            effective = (int) Math.sqrt(effective);
        }
        if (effective > 1 && pool >= cap * 1.25F) {
            effective = 1;
        }
        return effective;
    }

    /**
     * Original {@code ScanManager.validScan}: a scan only completes when its
     * aspect list is non-empty and every compound's parents are already
     * discovered. The original's client-side "discovery error" notification
     * is a GUI concern of the scanning item's milestone.
     */
    public static boolean validScan(final AspectList aspects, final PlayerKnowledgeData knowledge) {
        if (aspects == null || aspects.size() == 0) {
            return false;
        }
        for (final Aspect aspect : aspects.getAspects()) {
            if (aspect != null && !aspect.isPrimal()
                    && !hasDiscoveredParentAspects(knowledge, aspect)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Original {@code PlayerKnowledge.hasDiscoveredParentAspects}: primal
     * aspects pass; a compound requires both declared components discovered.
     * Expressed through the tag-based attachment API (PlayerKnowledgeData
     * deliberately does not depend on the aspect registry type).
     */
    private static boolean hasDiscoveredParentAspects(final PlayerKnowledgeData knowledge,
                                                     final Aspect aspect) {
        if (aspect.isPrimal()) {
            return true;
        }
        final java.util.List<Aspect> components = aspect.getComponents();
        if (components == null) {
            return false;
        }
        return knowledge.hasDiscoveredParentAspectTags(
                components.get(0).getTag(), components.get(1).getTag());
    }

    /**
     * Original {@code ScanManager.completeScan} knowledge tail: for every
     * aspect whose parents are discovered, feed the amount through
     * {@link #checkAndSyncAspectKnowledge} and collect what was gained.
     * {@code scannedByThaumometer} zeroes the amount (discovery only), and
     * the registry prefix "#" adds +1, exactly as the original.
     */
    public static AspectList applyScanKnowledge(final ServerPlayer player,
                                               final AspectList aspects,
                                               final boolean scannedByThaumometer,
                                               final boolean registryPrefix) {
        final AspectList gained = new AspectList();
        for (final Aspect aspect : aspects.getAspects()) {
            final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
            if (!hasDiscoveredParentAspects(knowledge, aspect)) {
                continue;
            }
            int amount = aspects.getAmount(aspect);
            if (scannedByThaumometer) {
                amount = 0;
            }
            if (registryPrefix) {
                amount++;
            }
            final int saved = checkAndSyncAspectKnowledge(player, aspect, amount);
            if (saved > 0) {
                gained.merge(aspect, saved);
            }
        }
        return gained;
    }
}
