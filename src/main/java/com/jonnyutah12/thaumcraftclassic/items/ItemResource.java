package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The original's {@code ItemResource} mega-item (content-mapping.csv row
 * {@code thaumcraft:itemresource}, TC-M6-04a): 19 damage-value variants
 * (alumentum 0, nitor 1, thaumium ingot 2, quicksilver 3, magic tallow 4,
 * zombie brain 5 [defunct], amber 6, enchanted fabric 7, vis filter 8,
 * knowledge fragment 9, mirrored glass 10, tainted goo 11, taint tendril 12,
 * jar label 13, salis mundus 14, primal charm 15, void metal ingot 16,
 * void seed 17, gold coin 18).
 *
 * <p>1.21.1 has no damage-value variants, so the meta travels on the stack in
 * the {@code thaumcraft:item_meta} component (default 0 on this item);
 * {@link #stack(int)} builds a variant stack. Display names follow the
 * original per-meta lang-key shape as {@code item.thaumcraft.itemresource.<meta>}
 * (the original {@code item.ItemResource.<meta>.name} keys map 1:1; the ARR
 * lang conversion is TC-M7-01).</p>
 *
 * <p>Implemented here: the essentia-container contract (jar label, meta 13),
 * the primal charm's max-stack-of-1 rule (original {@code getItemStackLimit})
 * and the knowledge fragment's aspect-pool right-click grant (meta 9, on
 * TC-M2-02 player knowledge).</p>
 *
 * <p>Deferred with their systems, each keyed by meta in the original
 * {@code onUpdate}/{@code onItemRightClick}: nitor placing a light block
 * (meta 1, blockAiry is TC-M6-04c), alumentum throwing (meta 0,
 * EntityAlumentum is TC-M6-06), taint-poisoning the holder (metas 11/12, the
 * potion effect is TC-M6-05), and the primal charm's aspect-orb spawn /
 * FOCUSPRIMAL research trigger (meta 15, entity TC-M6-06 + research
 * completion TC-M4).</p>
 */
public class ItemResource extends Item implements IEssentiaContainerItem {

    /** Original metas 0..18; meta 5 (zombie brain) is defunct but keeps numbering. */
    public static final int META_COUNT = 19;
    public static final int META_ALUMENTUM = 0;
    public static final int META_NITOR = 1;
    public static final int META_THAUMIUM_INGOT = 2;
    public static final int META_QUICKSILVER = 3;
    public static final int META_TALLOW = 4;
    public static final int META_AMBER = 6;
    public static final int META_KNOWLEDGE_FRAGMENT = 9;
    public static final int META_JAR_LABEL = 13;
    public static final int META_SALIS_MUNDUS = 14;
    public static final int META_PRIMAL_CHARM = 15;
    public static final int META_VOID_INGOT = 16;
    public static final int META_GOLD_COIN = 18;

    public ItemResource(final Properties props) {
        super(props);
    }

    /** A stack of one original-metadata variant, original {@code new ItemStack(item, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        if (meta < 0 || meta >= META_COUNT) {
            throw new IllegalArgumentException("ItemResource meta out of range: " + meta);
        }
        final ItemStack stack = new ItemStack(CatalogResources.ITEM_RESOURCE.get());
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        if (meta == META_PRIMAL_CHARM) {
            // Original getItemStackLimit: primal charm stacks never merge.
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        return stack;
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                               final List<Component> tooltip, final TooltipFlag flag) {
        // Original addInformation: aspect names on any stack carrying them
        // (jar label). The original hid undiscovered aspects behind
        // "tc.aspect.unknown"; 1.21.1's appendHoverText has no player
        // context, so the knowledge gate is deferred with the client
        // knowledge mirror (TC-M2-02/M7).
        final AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (final Aspect aspect : aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(aspect.getName() + " x" + aspects.getAmount(aspect)));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Knowledge fragment (meta 9), original {@code onItemRightClick}: right
     * click grants every primal aspect 1-2 points of player aspect pool
     * (TC4 {@code PlayerKnowledge.addAspectPool} + PacketAspectPool — the
     * pool grant and client sync are TC-M2-02's
     * {@link AspectKnowledge#checkAndSyncAspectKnowledge}).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand usedHand) {
        final ItemStack stack = player.getItemInHand(usedHand);
        if (stack == null || storedMeta(stack) != META_KNOWLEDGE_FRAGMENT) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof final ServerPlayer serverPlayer) {
            for (final Aspect aspect : Aspect.getPrimalAspects()) {
                AspectKnowledge.checkAndSyncAspectKnowledge(serverPlayer, aspect, level.getRandom().nextInt(2) + 1);
            }
            // Original: skip the consume when the holder has a debug ability
            // set. 1.21.1 has no instabreak flag (Player.Abilities exposes
            // invulnerable/flying/mayfly/instabuild/mayBuild), so the
            // closest debug-work flag is instabuild.
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
