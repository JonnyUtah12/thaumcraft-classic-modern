package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Research Notes (TC-M3-04; content-mapping.csv
 * {@code thaumcraft:itemresearchnotes}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemResearchNotes}. Unstackable,
 * rarity rare (epic for the blank discovery metas), and its right-click does
 * the two note actions: a completed note finishes its research (plus eligible
 * siblings) and is consumed; a blank discovery note (original metas 24/42)
 * either fails — dropping 7-9 knowledge fragments (itemresource meta 9) — or
 * becomes a fresh puzzle note for a hidden research.</p>
 *
 * <p>Deviations (docs/porting-decisions.md conventions): the original's
 * damage-value metas live in the {@code thaumcraft:item_meta} component like
 * the other multi-meta families; the note state itself is the
 * {@code thaumcraft:research_note} component (see {@link ResearchNoteData})
 * instead of raw NBT. The original's per-meta icons (note vs discovery, plus
 * the glint overlay) are a client-render concern deferred with item models.</p>
 */
public class ItemResearchNotes extends Item {

    /** Original blank discovery-note metas (24 and 42; the pair the original checked). */
    public static final int META_DISCOVERY_A = 24;
    public static final int META_DISCOVERY_B = 42;

    public ItemResearchNotes(final Properties properties) {
        super(properties);
    }

    /** A blank discovery note, as the original reward rows hand out (meta 24/42). */
    public static ItemStack discoveryNote(final int meta) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get());
        stack.set(ModDataComponents.ITEM_META, meta);
        return stack;
    }

    private static int metaOf(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META, 0);
    }

    /** Original {@code getItemDisplayName}: notes under meta 64, discoveries above. */
    @Override
    public Component getName(final ItemStack stack) {
        return metaOf(stack) < 64
                ? Component.translatable("item.researchnotes.name")
                : Component.translatable("item.discovery.name");
    }

    @Override
    public Rarity getRarity(final ItemStack stack) {
        return metaOf(stack) < 64 ? Rarity.RARE : Rarity.EPIC;
    }

    /**
     * Original {@code onItemRightClick} (server branch). The completion path
     * mirrors {@code ResearchManager.completeResearch} + the sibling loop; the
     * discovery path mirrors {@code findHiddenResearch} +
     * {@code createNote}.
     */
    @Override
    public InteractionResult use(final ItemStack stack, final Level level, final net.minecraft.world.entity.LivingEntity user,
            final InteractionHand hand) {
        if (!(user instanceof ServerPlayer serverPlayer) || level.isClientSide) {
            return InteractionResult.PASS;
        }
        final PlayerKnowledgeData knowledge = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final ResearchNoteData note = stack.get(ModDataComponents.RESEARCH_NOTE);

        if (note != null && note.complete() && !note.key().isEmpty()
                && !ResearchManager.isResearchComplete(knowledge, note.key())) {
            if (!ResearchManager.doesPlayerHaveRequisites(knowledge, note.key())) {
                serverPlayer.displayClientMessage(Component.translatable("tc.researcherror"), false);
                return InteractionResult.FAIL;
            }
            ResearchManager.completeResearch(serverPlayer, note.key());
            final ResearchEntryData entry = ResearchManager.graph().entry(note.key());
            if (entry != null && entry.siblings() != null) {
                final PlayerKnowledgeData after = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
                for (final String sibling : entry.siblings()) {
                    if (!ResearchManager.isResearchComplete(after, sibling)
                            && ResearchManager.doesPlayerHaveRequisites(after, sibling)) {
                        ResearchManager.completeResearch(serverPlayer, sibling);
                    }
                }
            }
            stack.shrink(1);
            level.playSound(null, serverPlayer, ModSounds.LEARN.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        final int meta = metaOf(stack);
        if (meta == META_DISCOVERY_A || meta == META_DISCOVERY_B) {
            final String key = ResearchManager.findHiddenResearch(knowledge, level.getDayTime());
            if ("FAIL".equals(key)) {
                stack.shrink(1);
                final ItemStack fragments = CatalogResources.stack(9);
                fragments.setCount(7 + level.random.nextInt(3));
                final ItemEntity drop = new ItemEntity(level,
                        serverPlayer.getX(), serverPlayer.getY() + serverPlayer.getEyeHeight() / 2.0F, serverPlayer.getZ(),
                        fragments);
                level.addFreshEntity(drop);
                level.playSound(null, serverPlayer, ModSounds.ERASE.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            } else {
                final ResearchEntryData entry = ResearchManager.graph().entry(key);
                final ResearchNoteData generated = NotePuzzle.generate(entry, level.random);
                if (generated == null) {
                    return InteractionResult.PASS;
                }
                stack.set(ModDataComponents.ITEM_META, 0);
                stack.set(ModDataComponents.RESEARCH_NOTE, generated);
                level.playSound(null, serverPlayer, ModSounds.WRITE.value(), SoundSource.PLAYERS, 0.75F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
            final List<Component> tooltip, final TooltipFlag flag) {
        final int meta = metaOf(stack);
        if (meta == META_DISCOVERY_A || meta == META_DISCOVERY_B) {
            tooltip.add(Component.translatable("item.researchnotes.unknown.1").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.researchnotes.unknown.2").withStyle(ChatFormatting.BLUE));
        }

        final ResearchNoteData note = stack.get(ModDataComponents.RESEARCH_NOTE);
        if (note != null && !note.key().isEmpty()) {
            final ResearchEntryData entry = ResearchManager.graph().entry(note.key());
            if (entry != null) {
                tooltip.add(Component.translatable(entry.nameKey()).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(entry.textKey()).withStyle(ChatFormatting.ITALIC));
                int warp = entry.warp();
                if (warp > 0) {
                    if (warp > 5) {
                        warp = 5;
                    }
                    // Original: tc.forbidden with its %n placeholder replaced by
                    // tc.research.level.<warp> — the lang string carries the
                    // placeholder, so the substitution happens on the resolved text.
                    final String label = Component.translatable("tc.forbidden").getString();
                    final String levelText = Component.translatable("tc.forbidden.level." + warp).getString();
                    tooltip.add(Component.literal(label.replace("%n", levelText)).withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
    }
}
