package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.loot.LootRoller;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Treasure bags (content-mapping.csv {@code thaumcraft:itemlootbag}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemLootBag} — one item, metas
 * 0/1/2 = common/uncommon/rare treasure bags, stack 16. Right-click (server
 * side) spawns {@code 8 + rand(5)} rolls of the matching bag table at the
 * player, plays "thaumcraft:coins" at 0.75/1.0, and consumes one bag. The
 * name color follows the meta (EnumRarity common/uncommon/rare). The
 * original's tooltip appended the generic "tc.lootbag" line; the port uses
 * the per-item lang key {@code item.thaumcraft.itemlootbag.tooltip}.</p>
 *
 * <p>1.21.1 has no item metadata: the rarity tier rides on the shared
 * ITEM_META component (D-001, same pattern as {@link ItemResource}), and the
 * three original name/texture variants map to the {@code .0/.1/.2} name
 * suffixes the same way.</p>
 */
public class ItemLootBag extends Item {

    public ItemLootBag(final Properties properties) {
        super(properties);
    }

    private static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /** Convenience for creative tabs / tests: a bag of one rarity. */
    public static ItemStack withRarity(final Item item, final int rarity) {
        final ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ITEM_META.get(), rarity);
        return stack;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        // Original: StatCollector("tc.lootbag") generic hint line.
        tooltip.add(Component.translatable("item.thaumcraft.itemlootbag.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Original onItemRightClick (server side): q = 8 + rand(5) rolls of
     * Utils.generateLoot(meta) spawned as items at the player, coins sound,
     * bag consumed by one.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            final RandomSource rand = level.getRandom();
            final int rarity = storedMeta(stack);
            final int q = 8 + rand.nextInt(5);
            for (int a = 0; a < q; a++) {
                final ItemStack is = LootRoller.generateLoot(rarity, rand);
                if (!is.isEmpty()) {
                    final ItemEntity item = new ItemEntity(level,
                            player.getX(), player.getY(), player.getZ(), is.copy());
                    level.addFreshEntity(item);
                }
            }
            level.playSound(null, player, ModSounds.COINS.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.75F, 1.0F);
        }
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
