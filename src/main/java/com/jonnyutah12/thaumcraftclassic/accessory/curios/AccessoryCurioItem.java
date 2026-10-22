package com.jonnyutah12.thaumcraftclassic.accessory.curios;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Adapter from our provider-independent {@link AccessoryItem} contract to
 * Curios' {@code ICurioItem}, attached via {@code CuriosApi.registerCurio}
 * (never implemented on item classes — that is what keeps core items free of
 * Curios types).
 *
 * <p>Mapping (Curios 9.5.1 signatures verified via javap against the ATM10
 * jar): {@code canEquip/canUnequip} → the item-side vetoes,
 * {@code onEquip/onUnequip} → {@code onEquipped/onUnequipped},
 * {@code curioTick} → {@code onWornTick}. TC4 baubles are player-only, so a
 * non-player wearer fails validation and gets no callbacks.</p>
 */
public final class AccessoryCurioItem implements ICurioItem {

    private final AccessoryItem behavior;

    public AccessoryCurioItem(final AccessoryItem behavior) {
        this.behavior = behavior;
    }

    @Override
    public boolean canEquip(final SlotContext context, final ItemStack stack) {
        final Player wearer = playerOf(context);
        if (wearer == null) {
            return false;
        }
        final var slot = com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot
                .byCuriosSlotId(context.identifier());
        if (slot == null || !com.jonnyutah12.thaumcraftclassic.accessory.AccessoryRules.slotAccepts(slot, stack)) {
            return false;
        }
        return behavior.canEquip(stack, wearer);
    }

    @Override
    public boolean canUnequip(final SlotContext context, final ItemStack stack) {
        final Player wearer = playerOf(context);
        return wearer != null && behavior.canUnequip(stack, wearer);
    }

    @Override
    public void onEquip(final SlotContext context, final ItemStack stack, final ItemStack previous) {
        final Player wearer = playerOf(context);
        if (wearer != null) {
            behavior.onEquipped(stack, wearer);
        }
    }

    @Override
    public void onUnequip(final SlotContext context, final ItemStack stack, final ItemStack previous) {
        final Player wearer = playerOf(context);
        if (wearer != null) {
            behavior.onUnequipped(stack, wearer);
        }
    }

    @Override
    public void curioTick(final SlotContext context, final ItemStack stack) {
        final Player wearer = playerOf(context);
        if (wearer != null) {
            behavior.onWornTick(stack, wearer);
        }
    }

    private static Player playerOf(final SlotContext context) {
        final LivingEntity entity = context.entity();
        return entity instanceof Player player ? player : null;
    }
}
