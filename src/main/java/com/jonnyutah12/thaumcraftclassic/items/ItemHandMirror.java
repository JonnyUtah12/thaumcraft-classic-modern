package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.items.component.HandMirrorLink;
import com.jonnyutah12.thaumcraftclassic.menus.HandMirrorMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirror;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Magic Hand Mirror (content-mapping.csv {@code thaumcraft:handmirror}).
 *
 * <p>Original: {@code thaumcraft.common.items.relics.ItemHandMirror}. Right-click
 * on a magic mirror block links the mirror item to it (the original wrote
 * linkX/Y/Z/linkDim/dimname onto the item NBT, played "thaumcraft:jar" at
 * 1.0/2.0, printed "Link established." and closed any open container).
 * Right-click with a linked mirror opens the hand-mirror GUI (original gui_id
 * 16, {@code ContainerHandMirror}): a single input slot whose contents are
 * teleported out of the linked mirror via {@link #transport}. A link whose
 * destination is no longer a mirror breaks itself with "thaumcraft:zap" and
 * the error message — on open and on transport.</p>
 *
 * <p>1.21.1 realization: the link is the {@code thaumcraft:mirror_link} data
 * component ({@link HandMirrorLink}); the GUI is {@link HandMirrorMenu}
 * (menu id {@code gui_hand_mirror}, the CSV gui row's realization). The
 * destination-side item spawn reuses {@link TileMirror#spawnItem} — the same
 * offset/velocity/pickup-delay formula the original's {@code transport}
 * computed inline. The original's {@code markBlockForUpdate} on the
 * destination is reduced to {@code setChanged} (no TC block-update channel in
 * 1.21.1).</p>
 */
public class ItemHandMirror extends Item {

    public ItemHandMirror(final Properties properties) {
        super(properties);
    }

    /**
     * Original onItemUseFirst: link to a clicked magic mirror (server side).
     * The client half only swung the arm, which vanilla block interaction
     * already does.
     */
    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof TileMirror)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        stack.set(CatalogTrinketsLoot.MIRROR_LINK.get(),
                new HandMirrorLink(pos.immutable(), level.dimension(),
                        HandMirrorLink.dimNameFor(level.dimension())));
        level.playSound(null, pos, ModSounds.JAR.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
        if (player != null) {
            player.displayClientMessage(linkedMessage(), false);
            player.closeContainer();
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Original onItemRightClick (server side): with a link, validate the
     * destination; a broken destination clears the link with zap + error, a
     * valid one opens the hand-mirror GUI. Unlinked: nothing.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        final HandMirrorLink link = stack.get(CatalogTrinketsLoot.MIRROR_LINK.get());
        if (link == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level.getServer().getLevel(link.dimension()) instanceof ServerLevel targetWorld)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(targetWorld.getBlockEntity(link.pos()) instanceof TileMirror)) {
            breakLink(stack, level, player);
            return InteractionResultHolder.pass(stack);
        }
        player.openMenu(new SimpleMenuProvider(
                (menuId, inv, p) -> new HandMirrorMenu(menuId, inv), stack.getHoverName()));
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Original {@code ItemHandMirror.transport}: spawn the stack on the far
     * side of the linked mirror, facing-offset formula included; a missing
     * destination breaks the link (zap + error) and returns false.
     */
    public static boolean transport(final ItemStack mirror, final ItemStack items,
                                   final Player player, final Level worldObj) {
        final HandMirrorLink link = mirror.get(CatalogTrinketsLoot.MIRROR_LINK.get());
        if (link == null) {
            return false;
        }
        if (!(worldObj.getServer().getLevel(link.dimension()) instanceof ServerLevel targetWorld)) {
            return false;
        }
        if (targetWorld.getBlockEntity(link.pos()) instanceof TileMirror tm) {
            tm.spawnItem(items.copy());
            tm.setChanged();
            worldObj.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.1F, 1.0F);
            return true;
        }
        breakLink(mirror, worldObj, player);
        return false;
    }

    /** Original link-break path: clear NBT, zap 1.0/0.8, error message. */
    private static void breakLink(final ItemStack mirror, final Level level, final Player player) {
        mirror.remove(CatalogTrinketsLoot.MIRROR_LINK.get());
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                ModSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        if (player != null) {
            player.displayClientMessage(Component.translatable("tc.handmirrorerror")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
    }

    private static Component linkedMessage() {
        return Component.translatable("tc.handmirrorlinked")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
    }

    /** Original tooltip: "Linked to x,y,z in dimname" while a link exists. */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final HandMirrorLink link = stack.get(CatalogTrinketsLoot.MIRROR_LINK.get());
        if (link != null) {
            tooltip.add(Component.translatable("tc.handmirrorlinkedto")
                    .append(" " + link.pos().getX() + "," + link.pos().getY() + "," + link.pos().getZ()
                            + " in " + link.dimName()));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Convenience for tests: is this stack linked? */
    public static boolean isLinked(final ItemStack stack) {
        return stack.has(CatalogTrinketsLoot.MIRROR_LINK.get());
    }
}
