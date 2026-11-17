package com.jonnyutah12.thaumcraftclassic.items.golems;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Golem upgrade (content-mapping.csv {@code thaumcraft:itemgolemupgrade}
 * "Golem Upgrade: Air / Earth / Fire / Water / Order / Entropy") — the
 * original's {@code thaumcraft.common.entities.golems.ItemGolemUpgrade}.
 *
 * <p>The original's {@code icon[6]} array is the whole domain: metas 0..5 are
 * Air, Earth, Fire, Water, Order, Entropy, in that primal order (the order of
 * {@code item.ItemGolemUpgrade.<meta>.name}). {@code func_77667_c} names the
 * stack {@code <item>.<meta>}, so the port keeps the per-meta name key
 * {@code item.thaumcraft.itemgolemupgrade.<meta>}, and {@code func_77624_a}
 * adds the meta's own description line
 * ({@code item.ItemGolemUpgrade.<meta>.desc}), shipped as
 * {@code item.thaumcraft.itemgolemupgrade.<meta>.desc}.</p>
 *
 * <p>Rarity is the original's unconditional {@code EnumRarity.uncommon}. The
 * upgrade <em>effects</em> — the type indexes {@code getUpgradeAmount(0..5)}
 * reads (0 speed, 1 strength/damage, 2 carrying, 3 senses/range, 4
 * organisation) — belong to the runtime card TC-M6-08-s-b.</p>
 */
public class ItemGolemUpgrade extends Item {

    /** The original's six damage values — {@code icon[6]}, one per primal. */
    public static final int META_COUNT = 6;

    public ItemGolemUpgrade(final Properties properties) {
        super(properties);
    }

    /** Original {@code func_77667_c}: {@code <item>.<meta>}. */
    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /** The original's {@code item.ItemGolemUpgrade.<meta>.desc} key, ported. */
    public static String descriptionKey(final int meta) {
        return "item.thaumcraft.itemgolemupgrade." + meta + ".desc";
    }

    /** Original {@code func_77624_a}: the upgrade's own description line. */
    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable(descriptionKey(storedMeta(stack))));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
