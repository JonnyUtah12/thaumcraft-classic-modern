package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.List;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Game-event wiring of the accessory layer (TC-M8-01): worn-tick driving,
 * right-click equip/unequip, and keepInventory-aware death handling — all
 * fallback-provider only. Under the Curios bridge these behaviors are native
 * (curioTick, equip-from-use, CurioDropsEvent/DropRulesEvent) and this class
 * stays inert, which is what keeps the exactly-once lifecycle contract true
 * under both providers.
 *
 * <p>Self-registering via {@code @EventBusSubscriber} like the network event
 * handlers; no constructor wiring needed.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class AccessoryLifecycle {

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        final Player player = event.getEntity();
        if (player.level().isClientSide || !fallbackActive()) {
            return;
        }
        AccessoryService.provider().tickWorn(player);
    }

    /**
     * Right-click equip/unequip without Curios (the pattern the Baubles spec
     * endorses as item behavior): right-clicking an accessory toggles it —
     * if a copy is already worn it is returned to the hand, otherwise it is
     * equipped into the first valid empty slot.
     */
    @SubscribeEvent
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        final Player player = event.getEntity();
        if (player.level().isClientSide || !fallbackActive()) {
            return;
        }
        final ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof AccessoryItem)) {
            return;
        }
        final FallbackProvider fallback = (FallbackProvider) AccessoryService.provider();

        // Toggle: a worn copy of the same item returns to the hand.
        for (final WornAccessory worn : fallback.worn(player)) {
            if (worn.stack().is(held.getItem())) {
                final ItemStack removed = fallback.unequip(player, worn.slot(), worn.index());
                if (removed.isEmpty()) {
                    return; // canUnequip veto
                }
                held.shrink(1);
                if (held.isEmpty()) {
                    player.setItemInHand(event.getHand(), removed);
                } else {
                    player.getInventory().add(removed);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
        }

        final SlotRef target = fallback.firstFreeSlot(player, held);
        if (target == null) {
            return;
        }
        final ItemStack toEquip = held.copyWithCount(1);
        if (fallback.equip(player, target.slot(), target.localIndex(), toEquip)) {
            held.shrink(1);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Death drops (fallback): with keepInventory off, worn accessories drop
     * with the rest of the inventory and the attachment is cleared. No
     * {@code onUnequipped} callback — {@code InventoryBaubles} drops without
     * lifecycle callbacks on death. With keepInventory on, nothing drops;
     * {@link #onPlayerClone} carries the attachment to the respawned player.
     */
    @SubscribeEvent
    public static void onLivingDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || !fallbackActive()) {
            return;
        }
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        final List<ItemStack> worn = player.getData(AccessoryAttachments.ACCESSORIES).wornStacks();
        if (worn.isEmpty()) {
            return;
        }
        for (final ItemStack stack : worn) {
            final ItemEntity drop = new ItemEntity(player.level(),
                    player.getX(), player.getEyeY(), player.getZ(), stack.copy());
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
        player.setData(AccessoryAttachments.ACCESSORIES, AccessoryData.empty());
    }

    /**
     * Respawn copy (fallback): keepInventory on → accessories carry over;
     * keepInventory off → they were already dropped at death
     * ({@link #onLivingDrops}), so the clone starts empty. A non-death clone
     * (end exit) always carries them, matching vanilla inventory behavior.
     */
    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (!fallbackActive()) {
            return;
        }
        final boolean keep = !event.isWasDeath()
                || event.getOriginal().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keep) {
            event.getEntity().setData(AccessoryAttachments.ACCESSORIES,
                    event.getOriginal().getData(AccessoryAttachments.ACCESSORIES));
        }
    }

    private static boolean fallbackActive() {
        return AccessoryService.isInitialized()
                && AccessoryService.provider() instanceof FallbackProvider;
    }

    private AccessoryLifecycle() {
    }
}
