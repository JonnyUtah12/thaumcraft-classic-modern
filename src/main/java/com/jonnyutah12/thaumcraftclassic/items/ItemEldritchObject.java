package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Eldritch Objects (content-mapping.csv {@code thaumcraft:itemeldritchobject})
 * — one item, five faces of the Outer Lands: the Eldritch Eye, Crimson Rites,
 * the Runed Tablet, the Primordial Pearl and the Eldritch Obelisk Placer
 * (metas 0-4 of the original's {@code ItemEldritchObject}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemEldritchObject} —
 * stack size 1, {@code setMaxDamage(0)} (no durability bar), per-metadata
 * name ({@code item.ItemEldritchObject.<meta>.name} →
 * {@code item.thaumcraft.itemeldritchobject.<meta>}), per-metadata lore
 * ({@code item.ItemEldritchObject.text.1..6}), per-metadata rarity
 * (2 = rare, 3 = epic, rest uncommon — 1.21.1 rarity is a data component with
 * no per-meta hook, so the item ships the original's uncommon default and the
 * two elevated metas are left to the research/tab card), and one behaviour:
 * the placer (meta 4) on the top face of a block, with six empty cells above
 * it, builds the obelisk column — altar at y+1, obelisk at y+3, obelisk tops
 * at y+4 through y+7 — and is consumed. Meta 3 (the Pearl) on a node is the
 * Primordial Edge ritual; the node rework is TC-M5-02's surface, so that
 * half stays with the follow-up card.</p>
 *
 * <p>Registered as one item under the original's id; the Eye and the Runed
 * Tablet are the keys of the Outer Lands structures — the altar and the lock
 * tiles read them through {@link #isEldritchObject(ItemStack)} and
 * {@link #storedMeta(ItemStack)}; their loot sources are the boss rooms and
 * structures that TC-M6-04f-m places.</p>
 */
public class ItemEldritchObject extends Item {

    /** The original's metadata domain: five creative-tab entries, metas 0-4. */
    public static final int META_COUNT = EldritchRealmValues.OBJECT_COUNT;

    public static final int DURABILITY = EldritchRealmValues.ELDRITCH_OBJECT_DURABILITY;

    public ItemEldritchObject() {
        // Original: setMaxStackSize(1) + setMaxDamage(0) (no durability bar) +
        // getRarity default uncommon (meta 2 rare / meta 3 epic have no
        // per-meta component hook in 21.1).
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** Is this the Eldritch Objects item (the original's {@code isItemEqual})? */
    public static boolean isEldritchObject(final ItemStack stack) {
        return stack.is(CatalogEldritchRealm.ITEM_ELDRITCH_OBJECT.get());
    }

    /** The metadata the original stored in the damage value; the port in the ITEM_META component. */
    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    public static void setStoredMeta(final ItemStack stack, final int meta) {
        stack.set(ModDataComponents.ITEM_META.get(), meta);
    }

    /**
     * The original's {@code getUnlocalizedName(ItemStack)} —
     * {@code super.getName() + "." + meta} — so every metadata resolves its
     * own name key {@code item.thaumcraft.itemeldritchobject.<meta>}
     * (the original's {@code item.ItemEldritchObject.<meta>.name}); a meta
     * outside the original's five keeps the family key.
     */
    @Override
    public String getDescriptionId(final ItemStack stack) {
        final int meta = storedMeta(stack);
        return meta < META_COUNT
                ? "item." + com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID + ".itemeldritchobject." + meta
                : getDescriptionId();
    }

    /**
     * The original's {@code addInformation} lore, in the original's colours:
     * the Eye one purple line, Crimson Rites two (purple + blue), the Tablet
     * and the Pearl two purple lines each, and the Placer the italic creative
     * notice (the original's literal {@code §oCreative Mode Only}, rendered
     * as the italic style — section codes ship literally in 1.21.1).
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        switch (storedMeta(stack)) {
            case EldritchRealmValues.OBJECT_EYE -> tooltip.add(
                    Component.translatable("item.ItemEldritchObject.text.1").withStyle(ChatFormatting.DARK_PURPLE));
            case EldritchRealmValues.OBJECT_CRIMSON -> {
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.2")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.3")
                        .withStyle(ChatFormatting.DARK_BLUE));
            }
            case EldritchRealmValues.OBJECT_TABLET -> tooltip.add(
                    Component.translatable("item.ItemEldritchObject.text.4").withStyle(ChatFormatting.DARK_PURPLE));
            case EldritchRealmValues.OBJECT_PEARL -> {
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.5")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.6")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            case EldritchRealmValues.OBJECT_PLACER -> tooltip.add(
                    Component.literal("Creative Mode Only").withStyle(ChatFormatting.ITALIC));
            default -> {
            }
        }
    }

    /**
     * The original's {@code onItemUseFirst} (placer, top face, six empty cells
     * above): raise the obelisk column and consume the placer.
     */
    @Override
    public net.minecraft.world.InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getItemInHand();
        if (storedMeta(stack) != EldritchRealmValues.OBJECT_PLACER || context.getClickedPos() == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        for (int a = 1; a <= EldritchRealmValues.PLACER_CLEAR_HEIGHT; a++) {
            if (!level.getBlockState(pos.above(a)).isAir()) {
                return net.minecraft.world.InteractionResult.PASS;
            }
        }
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        final BlockState base = CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState();
        for (int i = 0; i < EldritchRealmValues.PLACER_COLUMN_Y.length; i++) {
            level.setBlock(pos.above(EldritchRealmValues.PLACER_COLUMN_Y[i]),
                    base.setValue(com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch.VARIANT,
                            EldritchRealmValues.PLACER_COLUMN_VARIANT[i]), 3);
        }
        stack.shrink(1);
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}
