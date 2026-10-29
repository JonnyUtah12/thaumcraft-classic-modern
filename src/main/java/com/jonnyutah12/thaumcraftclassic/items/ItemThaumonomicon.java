package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Thaumonomicon (content-mapping.csv {@code thaumcraft:itemthaumonomicon},
 * original {@code thaumcraft.common.items.ItemThaumonomicon}) — the item whose
 * use opens the research browser (gui_id 12, {@code GuiResearchBrowser},
 * content-mapping.csv {@code thaumcraft:gui_thaumonomicon}).
 *
 * <p>This class carries no client reference on purpose. In the original the
 * item's {@code onItemRightClick} opened the GUI from an
 * {@code @SideOnly(Side.CLIENT)} branch; the port instead lets the item report
 * a successful use on both sides and has a client-only subscriber
 * ({@code client.ThaumonomiconClientHooks}) open the screen through
 * {@link com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost}.
 * That keeps the class loadable on a dedicated server, whose log the CI
 * {@code runServer} gate scans for mod-attributable errors.</p>
 */
public class ItemThaumonomicon extends Item {

    public ItemThaumonomicon(final Item.Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand hand) {
        // The display is a client concern (ThaumonomiconClientHooks); here we
        // only report that the use was taken, so the client plays the swing.
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
