package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Random;

/**
 * Research Notes (TC-M3-04-s-ii; content-mapping.csv
 * {@code thaumcraft:itemresearchnotes}, original
 * {@code thaumcraft.common.items.ItemResearchNotes}).
 *
 * <p>The port is line-for-line against the decompiled 4.2.3.35-era class
 * ({@code common/items/ItemResearchNotes.java}) plus the two
 * {@code ResearchManager} members its right-click drives
 * ({@code findHiddenResearch}, {@code createNote} — the latter ported as
 * {@link NotePuzzle#generate} by TC-M3-04-s):</p>
 *
 * <ul>
 *   <li>Unstackable, no durability, rarity rare — the constructor's
 *       {@code setMaxStackSize(1)} / {@code setMaxDamage(0)} and
 *       {@code getRarity} for metas below 64 (the epic discovery family is
 *       {@link #META_DISCOVERY_FAMILY} and above).</li>
 *   <li>A completed note whose research is not yet known finishes that
 *       research (and every sibling whose requisites the completion satisfies),
 *       consumes the note and plays {@code thaumcraft:learn} at 0.75/1.0. An
 *       unmet-requisite note answers the original {@code tc.researcherror} chat
 *       and is NOT consumed; a note that is not complete, or whose research is
 *       already known, falls through to the branch below, exactly as the
 *       original's {@code else if} chain does.</li>
 *   <li>A blank discovery note (original metas 24 and 42 — meta 42 is what
 *       {@code ConfigRecipes.java:3863} crafts from nine knowledge fragments)
 *       rolls {@code findHiddenResearch}: {@code FAIL} consumes the note and
 *       throws 7-9 knowledge fragments ({@code itemresource} meta 9) at the
 *       player with {@code thaumcraft:erase}; a hit resets the stack to meta 0
 *       and stamps it with a freshly generated puzzle note, with
 *       {@code thaumcraft:write}.</li>
 *   <li>The per-meta display name ({@code getItemDisplayName}: the note key
 *       below meta 64, the discovery key at/above it) and the tooltip — the
 *       blank-note lines, the note's research name/hover text, and the
 *       {@code tc.forbidden} warp line clamped at level 5.</li>
 * </ul>
 *
 * <h2>Port shape</h2>
 *
 * The original's damage-value metas ride the {@code thaumcraft:item_meta}
 * component like every other multi-meta family, and the note state is the
 * {@code thaumcraft:research_note} component (the original's
 * {@code ResearchNoteData} NBT, {@code ResearchManager.getData}). The
 * {@code thaumcraft:item_meta} codec is bounded at 63, so the discovery family
 * (metas 64+, i.e. {@code item.discovery.name} + epic rarity + the discovery
 * icons) has no stack carrier in the port; {@link #discoveryFamilyStack} is the
 * documented seam and the rule itself is ported verbatim, so a widened meta
 * carrier changes nothing else. The shipped original never crafts such a stack
 * (its only note recipe is the meta-42 blank). See
 * docs/BEHAVIORAL_DEVIATIONS.md.
 *
 * <p>The original's per-meta icons (note/discovery plus the overlay pass), the
 * {@code hasEffect} glint and the note's {@code color} tint are
 * {@code @SideOnly(Side.CLIENT)} rendering and stay with the item-models card
 * (the {@code discovery}/{@code discoveryoverlay} textures are
 * All Rights Reserved, D-004).</p>
 *
 * <p>The note's hex-grid SCREEN (the {@code GuiResearchTable} interaction that
 * places aspects and sets {@code complete}) is out of scope for this card, so a
 * completed note is reachable only from the note-creation path plus the
 * completion path pinned by {@code ResearchNoteItemGameTest}.</p>
 */
public class ItemResearchNotes extends Item {

    /**
     * The original's blank discovery-note metas — the two values the item's
     * right-click chain checks ({@code ItemResearchNotes.java:95}) and the two
     * the original hands out (meta 42 from {@code ConfigRecipes.java:3863}).
     */
    public static final int META_BLANK_DISCOVERY_A = 24;
    public static final int META_BLANK_DISCOVERY_B = 42;

    /**
     * The original's family threshold: below it a stack is a research note
     * ({@code item.researchnotes.name}, {@code EnumRarity.rare}, the note
     * icons), at/above it a discovery ({@code item.discovery.name},
     * {@code EnumRarity.epic}, the discovery icons).
     */
    public static final int META_DISCOVERY_FAMILY = 64;

    /** The tooltip lines the original shows on a blank discovery note. */
    public static final String KEY_BLANK_UNKNOWN_1 = "item.researchnotes.unknown.1";
    public static final String KEY_BLANK_UNKNOWN_2 = "item.researchnotes.unknown.2";

    /** The warp block of the tooltip: the {@code tc.forbidden} line + its levels. */
    public static final String KEY_FORBIDDEN = "tc.forbidden";
    public static final String KEY_FORBIDDEN_LEVEL_PREFIX = "tc.forbidden.level.";
    /** The original clamps the displayed warp level at 5. */
    public static final int MAX_WARP_LEVEL = 5;

    public ItemResearchNotes(final Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------- pure meta rules

    /** The stack's original damage value; the component is the port's carrier. */
    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * Original {@code getItemDisplayName}/{@code getRarity}/{@code getIcon}
     * family test: the metas below 64 are notes, 64 and above are discoveries.
     */
    public static boolean isDiscoveryFamily(final int meta) {
        return meta >= META_DISCOVERY_FAMILY;
    }

    /** The suffix the original's meta-64 rule appends to the family name key. */
    public static String nameKeySuffixForMeta(final int meta) {
        return isDiscoveryFamily(meta) ? ".discovery" : "";
    }

    /**
     * Original {@code getRarity}: rare for notes, epic for discoveries.
     * {@code Item.Properties.rarity} carries the note family's rare as the
     * registered default; the epic family is a per-stack component.
     */
    public static Rarity rarityForMeta(final int meta) {
        return isDiscoveryFamily(meta) ? Rarity.EPIC : Rarity.RARE;
    }

    /** The original's blank discovery note (the right-click branch's gate). */
    public static boolean isBlankDiscoveryMeta(final int meta) {
        return meta == META_BLANK_DISCOVERY_A || meta == META_BLANK_DISCOVERY_B;
    }

    // ---------------------------------------------------------- stack builders

    /** The original's {@code new ItemStack(itemResearchNotes, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get());
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        final Rarity rarity = rarityForMeta(meta);
        if (rarity != rarityForMeta(0)) {
            // 1.21.1 moved per-stack rarity to a data component; the registered
            // default is the note family's rare, so only the epic family needs
            // an override (the MetaBaubleItem.metaStack shape).
            stack.set(DataComponents.RARITY, rarity);
        }
        return stack;
    }

    /** A blank discovery note — the original's meta 24/42 reward/craft shape. */
    public static ItemStack blankDiscoveryNote(final int meta) {
        if (!isBlankDiscoveryMeta(meta)) {
            throw new IllegalArgumentException("not a blank discovery meta: " + meta);
        }
        return stack(meta);
    }

    /**
     * The note {@code ResearchManager.createNote} returns: meta 0 carrying the
     * generated puzzle state.
     */
    public static ItemStack puzzleNote(final ResearchNoteData data) {
        final ItemStack stack = stack(0);
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), data);
        return stack;
    }

    /**
     * The discovery family (original metas 64+). Documented seam only:
     * {@code thaumcraft:item_meta} is bounded at 63, so nothing in the shipped
     * port can carry such a stack; the name/rarity/icon rule is ported so the
     * family works the moment the carrier is widened.
     */
    public static ItemStack discoveryFamilyStack(final int meta) {
        if (!isDiscoveryFamily(meta)) {
            throw new IllegalArgumentException("not a discovery-family meta: " + meta);
        }
        return stack(meta);
    }

    // ------------------------------------------------------------------ rules

    @Override
    public Component getName(final ItemStack stack) {
        // Original getItemDisplayName (ItemResearchNotes.java:118-121): one label
        // for every note meta, a second one for the discovery family. Built on
        // the registered id so the id contract (content-mapping.csv) is the only
        // source of the key, per the TC-M7-01 naming convention; the note family
        // keeps the plain key, which is also vanilla's fallback.
        return Component.translatable(baseDescriptionId() + nameKeySuffixForMeta(storedMeta(stack)));
    }

    /** The registered id's plain description key. */
    private String baseDescriptionId() {
        final var id = BuiltInRegistries.ITEM.getKey(this);
        return "item." + id.getNamespace() + "." + id.getPath();
    }

    /**
     * Original {@code onItemRightClick} ({@code ItemResearchNotes.java:56-99}),
     * whose whole body sits behind {@code !world.isClientSide}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand usedHand) {
        final ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            // The original's body is server-only; the client just swings.
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        final PlayerKnowledgeData knowledge = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final ResearchNoteData note = stack.get(ModDataComponents.RESEARCH_NOTE.get());

        // Branch 1: getData(stack) != null && isComplete() && !isResearchComplete(key)
        if (note != null && note.complete() && !ResearchManager.isResearchComplete(knowledge, note.key())) {
            if (!ResearchManager.doesPlayerHaveRequisites(knowledge, note.key())) {
                serverPlayer.displayClientMessage(Component.translatable("tc.researcherror"), false);
                return InteractionResultHolder.pass(stack);
            }
            completeWithSiblings(serverPlayer, note.key());
            stack.shrink(1);
            level.playSound(null, serverPlayer, ModSounds.LEARN.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            return InteractionResultHolder.consume(stack);
        }

        // Branch 2: the blank discovery note (meta 24 / 42)
        final int meta = storedMeta(stack);
        if (isBlankDiscoveryMeta(meta)) {
            final String key = ResearchManager.findHiddenResearch(knowledge, level.getGameTime());
            if ("FAIL".equals(key)) {
                stack.shrink(1);
                final ItemStack fragments =
                        ItemResource.stack(ItemResource.META_KNOWLEDGE_FRAGMENT);
                fragments.setCount(7 + level.getRandom().nextInt(3));
                final ItemEntity drop = new ItemEntity(level, player.getX(),
                        player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), fragments);
                level.addFreshEntity(drop);
                level.playSound(null, serverPlayer, ModSounds.ERASE.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            } else {
                final ResearchEntryData entry = ResearchManager.graph().entry(key);
                final ResearchNoteData generated = NotePuzzle.generate(entry, noteRandom(level));
                if (generated == null) {
                    // The original dereferences the null createNote result and
                    // crashes; an uncraftable entry is not a note. Reachable
                    // only for a hidden entry whose tags all amount to <= 0,
                    // which the shipped graph has none of.
                    return InteractionResultHolder.pass(stack);
                }
                stack.set(ModDataComponents.ITEM_META.get(), 0);
                stack.set(ModDataComponents.RESEARCH_NOTE.get(), generated);
                level.playSound(null, serverPlayer, ModSounds.WRITE.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            }
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * The original's {@code ResearchManager.createNote} draws from
     * {@code world.rand} (a {@code java.util.Random} in 1.7.10). 1.21.1's world
     * random is a {@code RandomSource}, which the ported generator cannot
     * consume, so one draw from the world stream seeds the generator's own
     * stream: notes stay per-world reproducible and distinct from each other,
     * and the generation rule (radius, ring placement, blank removal) is
     * unchanged. See docs/BEHAVIORAL_DEVIATIONS.md.
     */
    private static Random noteRandom(final Level level) {
        return new Random(level.getRandom().nextLong());
    }

    /**
     * The original's completion block: send {@code PacketResearchComplete} then
     * {@code completeResearch}, for the note's key and then for every sibling
     * that is not already complete and whose requisitions the new knowledge
     * satisfies.
     */
    private static void completeWithSiblings(final ServerPlayer player, final String key) {
        PacketDistributor.sendToPlayer(player, new ResearchCompletePayload(key));
        ResearchManager.completeResearch(player, key);

        final ResearchEntryData entry = ResearchManager.graph().entry(key);
        if (entry == null || entry.siblings() == null) {
            return;
        }
        for (final String sibling : entry.siblings()) {
            final PlayerKnowledgeData current = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
            if (!ResearchManager.isResearchComplete(current, sibling)
                    && ResearchManager.doesPlayerHaveRequisites(current, sibling)) {
                PacketDistributor.sendToPlayer(player, new ResearchCompletePayload(sibling));
                ResearchManager.completeResearch(player, sibling);
            }
        }
    }

    // ---------------------------------------------------------------- tooltip

    /**
     * Original {@code addInformation}: the two blank-note lines, then the
     * note's research name and hover text, then the warp line.
     *
     * <p>The research name/text keys ({@code tc.research_name.*} /
     * {@code tc.research_text.*}) are the localization card's, exactly as in
     * the Thaumonomicon screen (BD-17), so a label renders as its key until
     * that card ships them.</p>
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final int meta = storedMeta(stack);
        if (isBlankDiscoveryMeta(meta)) {
            tooltip.add(Component.translatable(KEY_BLANK_UNKNOWN_1).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable(KEY_BLANK_UNKNOWN_2).withStyle(ChatFormatting.BLUE));
        }

        final ResearchNoteData note = stack.get(ModDataComponents.RESEARCH_NOTE.get());
        if (note == null || note.key() == null) {
            return;
        }
        final ResearchEntryData entry = ResearchManager.graph().entry(note.key());
        if (entry == null) {
            return;
        }
        tooltip.add(Component.translatable(entry.nameKey()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(entry.textKey()).withStyle(ChatFormatting.ITALIC));

        int warp = entry.warp();
        if (warp > 0) {
            if (warp > MAX_WARP_LEVEL) {
                warp = MAX_WARP_LEVEL;
            }
            // Original: EnumChatFormatting.DARK_PURPLE + tc.forbidden with its
            // %n placeholder replaced by tc.forbidden.level.<warp>. The
            // original's §l§5 markup is carried as formatting (1.21.1 renders
            // section codes literally), and the placeholder is a %s argument.
            tooltip.add(Component.translatable(KEY_FORBIDDEN,
                            Component.translatable(KEY_FORBIDDEN_LEVEL_PREFIX + warp))
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        }
    }
}
