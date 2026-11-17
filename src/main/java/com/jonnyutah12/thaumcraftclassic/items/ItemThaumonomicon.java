package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.network.payload.OpenThaumonomiconPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Thaumonomicon (BOOK-1 / t_1e6b18cc; content-mapping.csv
 * {@code Thaumcraft:ItemThaumonomicon} → {@code thaumcraft:itemthaumonomicon},
 * original {@code thaumcraft.common.items.relics.ItemThaumonomicon}).
 *
 * <p>This is the book's OPEN PATH: right-clicking the book is what makes the
 * Thaumonomicon reachable in a world, and the original's {@code func_77659_a}
 * (= {@code onItemRightClick}, the shape its
 * {@code ItemStack(ItemStack, World, EntityPlayer)} signature proves) is the
 * only place the browser is opened — its last statement is
 * {@code player.openGui(Thaumcraft.instance, 12, par2World, 0, 0, 0)}
 * ({@code ItemThaumonomicon.java:79}, {@code gui_id = 12} =
 * {@code GUI_THAUMONOMICON}).</p>
 *
 * <p><b>The two sides, as the original splits them.</b> The body is inside
 * {@code if (!par2World.field_72995_K)} ({@code :41}) — server only — and
 * finishes with the research sync ({@code PacketSyncResearch} +
 * {@code PacketSyncAspects}, {@code :69-70}); the {@code else} branch
 * ({@code :72-73}) is client-only and plays {@code thaumcraft:page} at the
 * player at 1.0/1.0. The {@code openGui} call sits after the if/else, so it
 * runs on both sides, and 1.7.10's client-side {@code EntityPlayer.openGui} is
 * an empty method while {@code EntityPlayerMP} sends the GuiOpen packet — i.e.
 * the open is a server-initiated client screen. 1.21.1 has no containerless
 * server-initiated open, so the server arm sends the S2C
 * {@link OpenThaumonomiconPayload} in place of FML's GuiOpen packet
 * (BD-22).</p>
 *
 * <p><b>Research sync arm.</b> The original walks every category's research
 * items and, for each one the player has completed that has siblings, completes
 * every sibling the player does not have yet ({@code :56-64}) — the book hands
 * over the display-linked peers of everything already known. The port walks
 * the shipped graph ({@code ResearchManager.graph()}, 201 entries) through the
 * port's own authority {@link ResearchManager#completeResearch} — the same
 * single entry point every other research writer uses, so the warp of a
 * newly-completed sibling is applied and the attachment sync goes out (the
 * port's replacement for the original's {@code PacketSyncResearch}).</p>
 *
 * <p><b>Deferred, on its own cards (not silently).</b> The cheat sheet:
 * meta 42 is a second icon ({@code thaumonomiconcheat}, {@code :33-40}), an
 * {@code EnumRarity.epic} + "Cheat Sheet" tooltip ({@code :82-84},
 * {@code :92-96}), an extra creative-tab entry behind
 * {@code Config.allowCheatSheet} ({@code :43-48}), and a server arm that
 * completes EVERY research entry and EVERY aspect ({@code :43-55}). Today a
 * meta-42 stack runs the plain arm above: the port ships no per-meta item for
 * this id, {@code Config.allowCheatSheet} is not ported (D-010), and granting
 * all research is a balance change this card has no licence to make. Same for
 * {@code PacketSyncAspects} — the port's aspect pool sync is the attachment
 * sync {@code completeResearch} already performs.</p>
 *
 * <p><b>Ctor shape.</b> The original's ctor is {@code func_77627_a(true)}
 * (full-3D hand pose), {@code func_77656_e(0)} (no durability) and
 * {@code func_77625_d(1)}; the reference tree ships the fields CSV only
 * ({@code reference/mcp-mappings/} has no methods CSV), so those MCP names are
 * not resolvable here and are NOT re-interpreted. 1.21.1 has no full-3D item
 * flag (the hand pose comes from the model) and no durability means
 * {@code stacksTo(1)} is not implied — the original stacks 64, so the port
 * keeps the default. Rarity is registration data in 1.21.1 (no
 * {@code getRarity(ItemStack)} hook), so the book registers
 * {@code Rarity.UNCOMMON} = the original's non-42 branch
 * ({@code func_77613_e}, whose {@code EnumRarity} return proves the hook).</p>
 */
public class ItemThaumonomicon extends Item {

    public ItemThaumonomicon(final Properties properties) {
        super(properties);
    }

    /**
     * Original {@code func_77659_a} ({@code ItemThaumonomicon.java:39-80}):
     * client plays the page sound, server syncs the research and opens the
     * book. The stack is returned unchanged in both arms (the original returns
     * {@code stack}, never consumes it).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // Original :72-73 — the client branch plays "thaumcraft:page"
            // 1.0/1.0 at the player's position (world.playSound with
            // !isRemote = false, i.e. audible to the local player).
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PAGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        openBook(serverPlayer);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * The server arm, split out so a game test can drive it on a mock server
     * player (a game-test server is never client-side, so the client branch
     * above is unreachable there — the {@code field_72995_K} split the original
     * wrote). Runs the research sync, then sends the open payload to this
     * player's client: the 1.21.1 stand-in for
     * {@code PacketSyncResearch} + {@code player.openGui(..., 12, ...)}
     * (BD-22).
     */
    public static void openBook(final ServerPlayer player) {
        completeSiblingsOfCompletedResearch(player);
        PacketDistributor.sendToPlayer(player,
                new OpenThaumonomiconPayload(OpenThaumonomiconPayload.EMPTY_CATEGORY));
    }

    /**
     * Original {@code :56-64}: for every entry of every category that the
     * player has completed and that declares siblings, complete each sibling
     * the player does not have.
     *
     * @return how many research keys this pass newly completed (0 when the
     *         player already knows every sibling — the steady state)
     */
    public static int completeSiblingsOfCompletedResearch(final ServerPlayer player) {
        int completed = 0;
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            final var siblings = entry.siblings();
            if (siblings == null || siblings.isEmpty()) {
                continue; // original: `rix.siblings != null`
            }
            if (!ResearchManager.isResearchComplete(
                    player.getData(ModAttachments.PLAYER_KNOWLEDGE), entry.key())) {
                continue; // original: ResearchManager.isResearchComplete(player, rix.key)
            }
            for (final String sibling : siblings) {
                if (ResearchManager.completeResearch(player, sibling)) {
                    completed++;
                }
            }
        }
        return completed;
    }
}
