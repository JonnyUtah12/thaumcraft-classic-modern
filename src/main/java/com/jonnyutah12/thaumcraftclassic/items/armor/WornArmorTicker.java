package com.jonnyutah12.thaumcraftclassic.items.armor;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives {@link WornArmorTick} for the four armor slots — the replacement
 * for the original's player-tick armor loop (see {@link WornArmorTick}).
 * Self-registered on the game event bus, same pattern as the other tick
 * drivers in this port (world/NodeTickDriver, accessory fallback).
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class WornArmorTicker {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
    };

    private WornArmorTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        for (final EquipmentSlot slot : ARMOR_SLOTS) {
            final ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof WornArmorTick worn) {
                worn.onWornTick(stack, player);
            }
        }
    }
}
