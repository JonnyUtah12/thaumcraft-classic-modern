package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * TC-M3-02: the port of the original {@code thaumcraft.common.lib.research.ResearchManager}
 * progression core (design record: docs/architecture/research.md).
 *
 * <p>Per-player state lives in the {@link PlayerKnowledgeData} / {@link PlayerWarpData}
 * attachments (TC-M2-02); the entry graph is the data-driven
 * {@link ResearchGraph} (TC-M3-01). The pure methods take those records and return
 * updated copies with no Minecraft access, so the original's rules stay unit-testable;
 * the {@code ServerPlayer} methods are thin glue that persists the records (attachment
 * S2C sync carries the state) and replays the packets the original sent.</p>
 *
 * <p>Parity notes reproduced exactly from the original:</p>
 * <ul>
 *   <li>{@link #doesPlayerHaveRequisites} gates on parents AND parentsHidden, and a
 *       player with an empty completed list never satisfies an entry that has
 *       prerequisites (the original's empty-list quirk).</li>
 *   <li>{@code @}-prefixed keys are note-based unlocks: they bypass the graph-existence
 *       check of {@link #isResearchComplete} and carry no research warp.</li>
 *   <li>{@link #completeResearch} applies the entry's warp as
 *       {@code ResearchManager.completeResearch}: warp&gt;1 splits into
 *       {@code warp - warp/2} permanent + {@code warp/2} sticky; warp 1 is all
 *       permanent; wuss mode suppresses it. Each add updates the eldritch counter to
 *       the new warp total, as {@code Thaumcraft.addWarpToPlayer} did.</li>
 *   <li>The research-point economy of TC4 is the per-player aspect pool ("aspect
 *       research points", the original {@code /thaumcraft aspect} help text):
 *       completing an entry through the Thaumonomicon deducts the entry's tag amounts
 *       from the pool via the original all-or-nothing {@code AspectList.reduce} rule —
 *       a pool below the cost is left untouched, never negative. There is no
 *       per-category point store in 4.2.3.5; the constructor "complexity" (RP cost)
 *       only drives research-note generation (TC-M3-04).</li>
 * </ul>
 */
public final class ResearchManager {

    /** Original wildcard meta ("any metadata") on research item triggers. */
    public static final int META_ANY = 32767;

    private static volatile ResearchGraph graph;

    private ResearchManager() {
    }

    /** The shipped research graph, loaded once from the mod's data resources. */
    public static ResearchGraph graph() {
        ResearchGraph g = graph;
        if (g == null) {
            synchronized (ResearchManager.class) {
                g = graph;
                if (g == null) {
                    g = ResearchGraphLoader.loadDefault();
                    if (!g.problems().isEmpty()) {
                        throw new IllegalStateException(
                                "research graph failed structural validation: " + g.problems());
                    }
                    graph = g;
                }
            }
        }
        return g;
    }

    // ------------------------------------------------------------------ pure rules

    /**
     * Original {@code ResearchManager.isResearchComplete}: an unknown non-{@code @}
     * key is never complete; {@code @}-keys (note unlocks) are checked verbatim.
     */
    public static boolean isResearchComplete(final PlayerKnowledgeData knowledge, final String key) {
        if (!key.startsWith("@") && graph().entry(key) == null) {
            return false;
        }
        return knowledge.researchKeys().contains(key);
    }

    /**
     * Original {@code ResearchManager.doesPlayerHaveRequisites}, quirk included:
     * with a prerequisite list present but an empty completed list the result is
     * false; every parent and hidden parent must be completed.
     */
    public static boolean doesPlayerHaveRequisites(final PlayerKnowledgeData knowledge, final String key) {
        final ResearchEntryData entry = graph().entry(key);
        if (entry == null) {
            return false;
        }
        return requisitionsSatisfied(knowledge, entry.parents())
                && requisitionsSatisfied(knowledge, entry.parentsHidden());
    }

    private static boolean requisitionsSatisfied(final PlayerKnowledgeData knowledge, final List<String> parents) {
        if (parents == null || parents.isEmpty()) {
            return true;
        }
        final var completed = knowledge.research();
        if (completed.isEmpty()) {
            return false;
        }
        final var done = knowledge.researchKeys();
        for (final String parent : parents) {
            if (!done.contains(parent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Original {@code ResearchManager.completeResearchUnsaved}: returns the updated
     * knowledge, or the same instance when the key was already present (identity
     * comparison is the original's boolean).
     */
    public static PlayerKnowledgeData completeResearchUnsaved(final PlayerKnowledgeData knowledge, final String key) {
        return knowledge.withResearchCompleted(key);
    }

    /**
     * Original {@code ResearchManager.completeResearch} warp branch:
     * {@code warp > 1} → {@code [warp - warp/2 permanent, warp/2 sticky]},
     * {@code warp == 1} → all permanent, anything else → nothing.
     */
    public static int[] warpSplit(final int warp) {
        if (warp > 1) {
            final int half = warp / 2;
            return new int[]{warp - half, half};
        }
        if (warp == 1) {
            return new int[]{1, 0};
        }
        return new int[]{0, 0};
    }

    /**
     * Original {@code ResearchManager.findHiddenResearch}: a hidden entry with tags
     * and at least one trigger whose prerequisites are met; the random pick is seeded
     * from the day time exactly as the original ({@code dayTime / 10 / 5}).
     * Returns the original {@code "FAIL"} sentinel when nothing qualifies.
     */
    public static String findHiddenResearch(final PlayerKnowledgeData knowledge, final long dayTime) {
        final List<String> keys = new ArrayList<>();
        for (final ResearchEntryData entry : graph().entries()) {
            final ResearchEntryData.Flags flags = entry.flags();
            if (flags == null || !flags.hidden() || entry.tags() == null || entry.tags().isEmpty()) {
                continue;
            }
            if (isResearchComplete(knowledge, entry.key())) {
                continue;
            }
            if (!doesPlayerHaveRequisites(knowledge, entry.key())) {
                continue;
            }
            final ResearchEntryData.Triggers triggers = entry.triggers();
            final boolean hasTriggers = triggers != null
                    && ((!nullSafe(triggers.items()).isEmpty())
                        || (!nullSafe(triggers.entities()).isEmpty())
                        || (!nullSafe(triggers.aspects()).isEmpty()));
            if (hasTriggers) {
                keys.add(entry.key());
            }
        }
        final Random rand = new Random(dayTime / 10L / 5L);
        return keys.isEmpty() ? "FAIL" : keys.get(rand.nextInt(keys.size()));
    }

    /**
     * Original {@code ResearchManager.findMatchingResearch} (the aspect-click
     * reveal path; the GUI wiring is TC-M3-03). Reads the live
     * {@code research_difficulty} config; see the overload for the pure form.
     */
    public static String findMatchingResearch(final PlayerKnowledgeData knowledge, final String aspectTag,
                                             final Random worldRandom) {
        return findMatchingResearch(knowledge, aspectTag, worldRandom,
                TcConfig.getInt(TcConfigCatalog.RESEARCH_DIFFICULTY));
    }

    /**
     * Original {@code ResearchManager.findMatchingResearch}: entries that are not
     * secondary-under-the-current-difficulty, not hidden/lost/autoUnlock/virtual/stub,
     * not yet complete, requisites met, and tagged with the aspect. The original
     * caches the candidate set; the graph is static so recomputation is equivalent.
     * The difficulty precedence is the original's
     * {@code ri.isSecondary() && Config.researchDifficulty == 0 || Config.researchDifficulty == -1}
     * — in easy mode ({@code -1}) every entry counts as secondary, so the candidate
     * set is empty and the result is null.
     */
    public static String findMatchingResearch(final PlayerKnowledgeData knowledge, final String aspectTag,
                                             final Random worldRandom, final int difficulty) {
        final List<String> keys = new ArrayList<>();
        for (final ResearchEntryData entry : graph().entries()) {
            final ResearchEntryData.Flags flags = entry.flags();
            final boolean secondary = flags != null && flags.secondary() && difficulty == 0 || difficulty == -1;
            if (secondary || flags == null || flags.hidden() || flags.lost()
                    || flags.autoUnlock() || flags.virtual() || flags.stub()) {
                continue;
            }
            if (isResearchComplete(knowledge, entry.key()) || !doesPlayerHaveRequisites(knowledge, entry.key())) {
                continue;
            }
            if (entry.tags() != null && entry.tags().stream()
                    .anyMatch(tag -> tag.aspect().equals(aspectTag) && tag.amount() > 0)) {
                keys.add(entry.key());
            }
        }
        return keys.isEmpty() ? null : keys.get(worldRandom.nextInt(keys.size()));
    }

    /**
     * Original {@code ResearchManager.createClue} candidate scan: hidden or lost
     * entries with tags, not completed (neither plain nor {@code @}-unlocked), whose
     * item/entity/aspect triggers match the clue. Item matching follows the original's
     * {@code InventoryUtils.areItemStacksEqual(trigger, clue, true, true, false)}:
     * damage/meta is ignored (the third {@code true}), so trigger metas are not
     * compared; ore-dictionary equivalence is not modelled yet (TC-M6 maps those to
     * item tags) and trigger NBT is empty in the extracted graph. {@code clueItemId}
     * is a modern registry id; trigger ids are normalized to the frozen id contract
     * (lowercased original path, docs/porting-decisions.md D-001).
     */
    public static List<String> hiddenClueKeys(final PlayerKnowledgeData knowledge, final String clueItemId,
                                             final String clueEntityName, final Collection<String> aspectTags) {
        final List<String> keys = new ArrayList<>();
        for (final ResearchEntryData entry : graph().entries()) {
            final ResearchEntryData.Flags flags = entry.flags();
            if (flags == null || !(flags.lost() || flags.hidden())) {
                continue;
            }
            if (entry.tags() == null || entry.tags().isEmpty()) {
                continue;
            }
            if (isResearchComplete(knowledge, entry.key()) || isResearchComplete(knowledge, "@" + entry.key())) {
                continue;
            }
            final ResearchEntryData.Triggers triggers = entry.triggers();
            if (triggers == null) {
                continue;
            }
            boolean matched = false;
            if (clueItemId != null) {
                for (final ResearchEntryData.ItemRef trigger : nullSafe(triggers.items())) {
                    if (normalizedItemId(trigger.item()).equals(clueItemId)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched && clueEntityName != null) {
                for (final String trigger : nullSafe(triggers.entities())) {
                    if (normalizedEntityName(trigger).equals(clueEntityName)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched && aspectTags != null && !aspectTags.isEmpty()) {
                for (final String trigger : nullSafe(triggers.aspects())) {
                    if (aspectTags.contains(trigger)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                keys.add(entry.key());
            }
        }
        return keys;
    }

    /** Original trigger id normalization: {@code Thaumcraft:FocusFire} → {@code thaumcraft:focusfire}. */
    public static String normalizedItemId(final String originalItem) {
        final int colon = originalItem.indexOf(':');
        if (colon < 0) {
            return originalItem.toLowerCase(Locale.ROOT);
        }
        return originalItem.substring(0, colon).toLowerCase(Locale.ROOT)
                + ":" + originalItem.substring(colon + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Original entity trigger normalization: {@code Thaumcraft.BrainyZombie} →
     * {@code thaumcraft:brainyzombie}; a bare name is a vanilla entity →
     * {@code minecraft:<lower>}.
     */
    public static String normalizedEntityName(final String originalEntity) {
        final int dot = originalEntity.indexOf('.');
        if (dot < 0) {
            return "minecraft:" + originalEntity.toLowerCase(Locale.ROOT);
        }
        return originalEntity.substring(0, dot).toLowerCase(Locale.ROOT)
                + ":" + originalEntity.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------- server mutations

    /**
     * Original {@code ResearchManager.completeResearch}: record the completion, then
     * apply the entry's warp (wuss-gated) exactly as the original split it between
     * permanent and sticky, sending the warp sync/notification packets
     * {@code Thaumcraft.addWarpToPlayer}/{@code addStickyWarpToPlayer} sent and
     * updating the eldritch counter to the new total.
     *
     * <p>Returns whether the key was newly completed. The S2C research sync is the
     * attachment sync (docs/architecture/persistence.md); callers replay the
     * original's per-event {@link ResearchCompletePayload} where the original sent it.</p>
     */
    public static boolean completeResearch(final ServerPlayer player, final String key) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final PlayerKnowledgeData updated = completeResearchUnsaved(knowledge, key);
        if (updated == knowledge) {
            return false;
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, updated);

        final ResearchEntryData entry = graph().entry(key);
        final int warp = entry == null ? 0 : entry.warp();
        if (warp > 0 && !TcConfig.getBool(TcConfigCatalog.WUSS_MODE)) {
            final int[] split = warpSplit(warp);
            if (split[0] > 0) {
                applyWarp(player, split[0], SyncWarpPayload.WARP_PERMANENT);
            }
            if (split[1] > 0) {
                applyWarp(player, split[1], SyncWarpPayload.WARP_STICKY);
            }
        }
        return true;
    }

    /**
     * Original {@code Thaumcraft.addWarpToPlayer(player, amount, false)} /
     * {@code addStickyWarpToPlayer}: add to the chosen counter, sync it, notify the
     * change, and set the eldritch counter to the new warp total.
     */
    static void applyWarp(final ServerPlayer player, final int amount, final int kind) {
        PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        if (kind == SyncWarpPayload.WARP_PERMANENT) {
            warp = warp.addWarpPerm(amount);
        } else if (kind == SyncWarpPayload.WARP_STICKY) {
            warp = warp.addWarpSticky(amount);
        } else {
            warp = warp.addWarpTemp(amount);
        }
        warp = warp.withWarpCounter(warp.total());
        player.setData(ModAttachments.PLAYER_WARP, warp);
        final int value = switch (kind) {
            case SyncWarpPayload.WARP_PERMANENT -> warp.warpPerm();
            case SyncWarpPayload.WARP_STICKY -> warp.warpSticky();
            default -> warp.warpTemp();
        };
        PacketDistributor.sendToPlayer(player, new SyncWarpPayload(value, kind));
        PacketDistributor.sendToPlayer(player, new WarpMessagePayload(amount, kind));
    }

    /**
     * Original {@code EventHandlerEntity.playerLoggedIn} tail: seed a brand-new
     * player's primal aspect pools ({@code 15 + rand(5)} each, the
     * {@code loadPlayerData} "Assigning initial aspects" branch — detected by an
     * empty discovered-aspect map, the state a missing {@code .thaum} file produced)
     * and auto-unlock every entry flagged autoUnlock.
     */
    public static void onPlayerJoined(final ServerPlayer player) {
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.aspectsDiscovered().isEmpty()) {
            final net.minecraft.util.RandomSource random = player.level().getRandom();
            for (final String primal : PRIMAL_ASPECT_TAGS) {
                knowledge = knowledge.withAspectDiscovered(primal, 15 + random.nextInt(5));
            }
            player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            ThaumcraftClassic.LOGGER.info("Assigning initial aspects to {}", player.getName().getString());
        }

        for (final ResearchEntryData entry : graph().entries()) {
            final ResearchEntryData.Flags flags = entry.flags();
            if (flags != null && flags.autoUnlock()) {
                completeResearch(player, entry.key());
            }
        }
    }

    /** The six primal aspect tags seeded on first join (original {@code addDiscoveredPrimalAspects} set). */
    static final List<String> PRIMAL_ASPECT_TAGS =
            List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");

    /**
     * Original {@code ResearchManager.createClue} for an item clue: pick one of the
     * matching hidden entries at random (world random, as the original) and complete
     * its {@code @}-key, sending the original's {@code PacketResearchComplete}.
     */
    public static boolean createClue(final ServerPlayer player, final ItemStack clue,
                                    final Collection<String> aspectTags) {
        final String itemId = BuiltInRegistries.ITEM.getKey(clue.getItem()).toString();
        final List<String> keys = hiddenClueKeys(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                itemId, null, aspectTags);
        return completeRandomClue(player, keys);
    }

    /** Original {@code createClue} for an entity clue (entity type id, normalized form). */
    public static boolean createClue(final ServerPlayer player, final String clueEntityName,
                                    final Collection<String> aspectTags) {
        final List<String> keys = hiddenClueKeys(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                null, clueEntityName, aspectTags);
        return completeRandomClue(player, keys);
    }

    private static boolean completeRandomClue(final ServerPlayer player, final List<String> keys) {
        if (keys.isEmpty()) {
            return false;
        }
        final String key = "@" + keys.get(player.level().getRandom().nextInt(keys.size()));
        PacketDistributor.sendToPlayer(player, new ResearchCompletePayload(key));
        completeResearch(player, key);
        return true;
    }

    /**
     * Original {@code PacketPlayerCompleteToServer.onMessage}: the Thaumonomicon
     * completion request. Already-complete requests are ignored; unmet prerequisites
     * answer the original {@code tc.researcherror} chat; type 0 pays the entry's tag
     * costs out of the aspect pool (all-or-nothing, the original deducted before
     * completing and sent the pool packet either way), completes the entry, then
     * completes requisition-satisfying siblings; type 1 asks for a research note
     * (TC-M3-04 owns note creation).
     */
    public static void handlePlayerCompleteRequest(final PlayerCompleteToServerPayload payload,
                                                  final ServerPlayer player) {
        final String key = payload.key();
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (isResearchComplete(knowledge, key)) {
            return;
        }
        if (!doesPlayerHaveRequisites(knowledge, key)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.researcherror"), false);
            return;
        }

        if (payload.requestType() == PlayerCompleteToServerPayload.REQUEST_NOTE) {
            // TODO(TC-M3-04): createResearchNoteForPlayer — ink/scribe-tool
            // consumption and the hex-grid note item; the note item itself is a
            // TC-M6 catalog row. The reveal rules it feeds (findHiddenResearch,
            // createClue) are implemented here.
            ThaumcraftClassic.LOGGER.debug("research note requested by {} for {}",
                    player.getGameProfile().getName(), key);
            return;
        }

        final ResearchEntryData entry = graph().entry(key);
        PlayerKnowledgeData paid = knowledge;
        if (entry != null && entry.tags() != null) {
            for (final ResearchEntryData.AspectTag tag : entry.tags()) {
                paid = paid.withAspectPoolAdded(tag.aspect(), -tag.amount());
                player.setData(ModAttachments.PLAYER_KNOWLEDGE, paid);
                PacketDistributor.sendToPlayer(player,
                        new AspectPoolPayload(tag.aspect(), -tag.amount(), paid.aspectAmount(tag.aspect())));
            }
        }

        PacketDistributor.sendToPlayer(player, new ResearchCompletePayload(key));
        completeResearch(player, key);

        if (entry != null && entry.siblings() != null) {
            for (final String sibling : entry.siblings()) {
                final PlayerKnowledgeData current = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
                if (!isResearchComplete(current, sibling) && doesPlayerHaveRequisites(current, sibling)) {
                    PacketDistributor.sendToPlayer(player, new ResearchCompletePayload(sibling));
                    completeResearch(player, sibling);
                }
            }
        }

        player.level().playSound(null, player, ModSounds.LEARN.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
    }

    /**
     * Original {@code EventHandlerWorld.onCrafting} warp branch: crafting an item
     * registered with {@code ThaumcraftApi.addWarpToItem} adds sticky warp
     * (wuss-gated). The table below is the complete {@code addWarpToItem} set from
     * the decompiled ConfigResearch.java; every entry is dormant until its item is
     * registered by its content milestone, and non-wildcard metas match plain (meta 0)
     * stacks until item variants are modelled.
     */
    private static final List<ItemWarp> ITEM_WARP = List.of(
            new ItemWarp("thaumcraft:focushellbat", 0, 1),      // ConfigResearch.java:129
            new ItemWarp("thaumcraft:itemcompassstone", 0, 1),  // ConfigResearch.java:865
            new ItemWarp("thaumcraft:blockjar", 1, 1),          // ConfigResearch.java:1414
            new ItemWarp("thaumcraft:itembucketdeath", 0, 1),   // ConfigResearch.java:1647
            new ItemWarp("thaumcraft:itembottletaint", 0, 1),   // ConfigResearch.java:1663
            new ItemWarp("thaumcraft:itemgolemplacer", 4, 1),   // ConfigResearch.java:2178
            new ItemWarp("thaumcraft:focusprimal", 0, 1),       // ConfigResearch.java:3076
            new ItemWarp("thaumcraft:wandrod", 100, 1));        // ConfigResearch.java:3111

    record ItemWarp(String itemId, int meta, int warp) {
    }

    /** Original {@code ThaumcraftApi.getWarp(crafting)} + sticky-warp application. */
    public static void applyCraftingWarp(final ServerPlayer player, final ItemStack crafted) {
        if (TcConfig.getBool(TcConfigCatalog.WUSS_MODE)) {
            return;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        final int stackMeta = 0; // item variants are not modelled yet (TC-M6); metas match plain stacks
        for (final ItemWarp entry : ITEM_WARP) {
            if (entry.itemId().equals(id.toString()) && entry.meta() == stackMeta) {
                applyWarp(player, entry.warp(), SyncWarpPayload.WARP_STICKY);
                return;
            }
        }
    }

    /** The {@code addWarpToItem} table, for tests and the content milestones. */
    public static List<ItemWarp> itemWarpTable() {
        return ITEM_WARP;
    }

    private static <T> List<T> nullSafe(final List<T> list) {
        return list == null ? List.of() : list;
    }
}
