package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.wands.VisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;

/**
 * Vis amulet (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemAmuletVis}
 * ({@code common/items/baubles/ItemAmuletVis.java}).
 *
 * <p>Original metas preserved: 0 = Vis Amulet (capacity 2500 centi-vis =
 * 25 vis), 1 = Greater Vis Amulet (capacity 25000 = 250 vis; the original
 * gates equipping it on research "VISAMULET", line 197). Charge storage uses
 * the shared {@code thaumcraft:vis_pool} component (docs/architecture/vis.md)
 * instead of the original's per-aspect NBTTagInt keys — same centi-vis
 * amounts, same per-aspect keys.</p>
 *
 * <p>Worn-tick wand transfer (lines 74-92): every 5 ticks, up to 5 centi-vis
 * per aspect flows from the amulet into the main-hand vis container, capped
 * by the wand's remaining room and the amulet's stored charge — the original
 * duck-typed on {@code ItemWandCasting}; the port uses the shared
 * {@link VisContainerItem} surface so TC-M6-01's wand joins unchanged. The
 * vis-relay refill branch (lines 94-110) is deferred to TC-M5-02
 * ({@code TileVisRelay}).</p>
 *
 * <p>{@code getRunicCharge} = 0, as in the original (line 210).</p>
 */
public class ItemAmuletVis extends MetaBaubleItem implements IRunicArmor, VisContainerItem {

    public static final int META_LESSER = 0;
    public static final int META_GREATER = 1;
    public static final int META_COUNT = 2;

    /** Research gate for the greater amulet (original: "VISAMULET", line 197). */
    public static final String RESEARCH_KEY = "VISAMULET";

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#######.##");

    public ItemAmuletVis(final Properties props) {
        super(props);
    }

    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** The original's {@code getMaxVis} (line 131): 25000 for meta 1, else 2500 (centi-vis). */
    public static int maxVisForMeta(final int meta) {
        return meta == META_GREATER ? 25_000 : 2_500;
    }

    /**
     * Per-transfer cap of the original's worn-tick loop (lines 82-86):
     * {@code min(5, room, stored)} centi-vis.
     */
    public static int transferAmount(final int room, final int stored) {
        return Math.min(Math.min(5, room), stored);
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.AMULET);
    }

    @Override
    public boolean canEquip(final ItemStack stack, final Player player) {
        // Original (line 197): meta 1 requires research VISAMULET.
        return storedMeta(stack) != META_GREATER
                || ResearchManager.isResearchComplete(
                        player.getData(ModAttachments.PLAYER_KNOWLEDGE), RESEARCH_KEY);
    }

    @Override
    public void onWornTick(final ItemStack stack, final Player wearer) {
        if (wearer.level().isClientSide || wearer.tickCount % 5 != 0) {
            return;
        }
        final ItemStack held = wearer.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof VisContainerItem wand) {
            final AspectList al = wand.getAspectsWithRoom(held);
            for (final Aspect aspect : al.getAspects()) {
                if (aspect != null && getVis(stack, aspect) > 0) {
                    final int amt = transferAmount(
                            wand.getMaxVis(held) - wand.getVis(held, aspect),
                            getVis(stack, aspect));
                    storeVis(stack, aspect, getVis(stack, aspect) - amt);
                    wand.storeVis(held, aspect, wand.getVis(held, aspect) + amt);
                }
            }
        }
        // Vis-relay refill branch (original lines 94-110) deferred to TC-M5-02
        // (TileVisRelay.nearbyPlayers): same transferAmount cap applies there.
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    // ------------------------------------------------------- VisContainerItem

    @Override
    public int getMaxVis(final ItemStack stack) {
        return maxVisForMeta(storedMeta(stack));
    }

    @Override
    public int getVis(final ItemStack stack, final Aspect aspect) {
        return WandVis.poolOf(stack).getVis(aspect);
    }

    @Override
    public void storeVis(final ItemStack stack, final Aspect aspect, final int amount) {
        // Original writes the NBTTagInt absolutely, zero included (line 141).
        WandVis.setPool(stack, WandVis.withVis(WandVis.poolOf(stack), aspect, amount));
    }

    @Override
    public AspectList getAspectsWithRoom(final ItemStack stack) {
        return WandVis.getAspectsWithRoom(WandVis.poolOf(stack), getMaxVis(stack));
    }

    @Override
    public AspectList getAllVis(final ItemStack stack) {
        // Original merges every primal into the list, zero included (lines 158-169).
        final AspectList out = new AspectList();
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            out.add(aspect, getVis(stack, aspect));
        }
        return out;
    }

    @Override
    public boolean consumeAllVis(final ItemStack stack, final Player player,
                                final AspectList centiCosts, final boolean doit,
                                final boolean crafting) {
        final VisPool paid = WandVis.consumeAllVis(WandVis.poolOf(stack), centiCosts);
        if (paid == null) {
            return false;
        }
        if (doit) {
            WandVis.setPool(stack, paid);
        }
        return true;
    }

    @Override
    public int addVis(final ItemStack stack, final Aspect aspect, final int amount,
                      final boolean doit) {
        final var result = WandVis.addVis(WandVis.poolOf(stack), aspect, amount, getMaxVis(stack));
        if (doit) {
            WandVis.setPool(stack, result.pool());
        }
        return result.leftover();
    }

    @Override
    public int addRealVis(final ItemStack stack, final Aspect aspect, final int centiAmount,
                          final boolean doit) {
        final var result = WandVis.addRealVis(WandVis.poolOf(stack), aspect, centiAmount,
                getMaxVis(stack));
        if (doit) {
            WandVis.setPool(stack, result.pool());
        }
        return result.leftover();
    }

    // ---------------------------------------------------------------- display

    @Override
    public Component getName(final ItemStack stack) {
        return perMetaName(stack);
    }

    @Override
    protected Rarity rarityForMeta(final int meta) {
        // Original: uncommon for meta 0 (the registered default), rare for
        // meta 1 (ItemAmuletVis.java:51).
        return meta == META_LESSER ? null : Rarity.RARE;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                               final List<Component> tooltip, final TooltipFlag flag) {
        // Original addInformation (lines 112-129): helper text for the lesser
        // amulet, capacity line, then one line per stored primal aspect.
        if (storedMeta(stack) == META_LESSER) {
            tooltip.add(Component.translatable("item.ItemAmuletVis.text")
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("item.capacity.text")
                .append(" " + (getMaxVis(stack) / 100))
                .withStyle(ChatFormatting.GOLD));
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            if (WandVis.poolOf(stack).centiVis().containsKey(aspect.getTag())) {
                // Original: " §<chatcolor><name>§r x <amount>" (line 126); the
                // legacy codes become real component styles in the port.
                final int centi = getVis(stack, aspect);
                final ChatFormatting color = ChatFormatting.getByCode(aspect.getChatcolor().charAt(0));
                tooltip.add(Component.literal(" ")
                        .append(Component.literal(aspect.getName())
                                .withStyle(color == null ? ChatFormatting.WHITE : color))
                        .append(Component.literal(" x " + AMOUNT_FORMAT.format(centi / 100.0F))
                                .withStyle(ChatFormatting.WHITE)));
            }
        }
    }
}
