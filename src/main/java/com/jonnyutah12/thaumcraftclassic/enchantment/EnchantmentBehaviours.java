package com.jonnyutah12.thaumcraftclassic.enchantment;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.IRepairable;
import com.jonnyutah12.thaumcraftclassic.api.IRepairableExtended;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModEnchantments;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerTickEvent;

/**
 * Behaviour handlers for the two ported enchantments (TC-M6-05).
 *
 * <p>Original: {@code EventHandlerEntity.doRepair} (Repair) and
 * {@code EventHandlerEntity.updateSpeed} (Haste), both driven from
 * {@code EventHandlerEntity.livingTick}. The enchantment data itself is
 * datapack JSON (data/thaumcraft/enchantment/); only these tick behaviours
 * need code, exactly like the original's static handler.</p>
 *
 * <p>Port notes:</p>
 * <ul>
 *   <li>Haste: the original added a movement-input bonus
 *       ({@code addMovementInput(0, 1, bonus)}), which multiplies the speed
 *       attribute by {@code 1 + bonus} for that tick. 1.21.1 keeps movement
 *       input boolean on the server, so the equivalent is a transient
 *       multiplicative {@code MOVEMENT_SPEED} modifier of the same magnitude
 *       — the effective walking speed is identical. The original's gates are
 *       kept: not creative, boots slot non-empty, speed attribute &gt; 0;
 *       the bonus halves while sneaking and while in water
 *       ({@code Entity.isInWater}, original {@code func_70090_H}).</li>
 *   <li>Repair: the original's per-item creative gate used
 *       {@code capabilities.disableDamage}; the port uses the 1.21.1
 *       equivalent {@code abilities.invulnerable}. The original also skipped
 *       the hover harness in the main-inventory loop; that item is not
 *       ported yet (TC-M8-02) and will be excluded when it lands.</li>
 *   <li>Vis payment: the original paid from {@code ItemAmuletVis} in bauble
 *       slots first, then the main inventory in reverse for
 *       {@code ItemWandCasting}; the port uses the {@link IVisContainerItem}
 *       abstraction over the same slots, paying from the stack's
 *       {@code vis_pool} component all-or-nothing (the original's
 *       {@code consumeAllVis}, costs scaled ×100 to centi-vis).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class EnchantmentBehaviours {

    /** Original: {@code player.tickCount % 40 == 0} repair sweep. */
    static final int REPAIR_INTERVAL_TICKS = 40;

    /** Original: {@code if (level > 2) level = 2;} inside doRepair. */
    static final int REPAIR_LEVEL_CAP = 2;

    /** Original: {@code float bonus = haste * 0.015F;} in updateSpeed. */
    static final float HASTE_BONUS_PER_LEVEL = 0.015F;

    public static final ResourceLocation HASTE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "enchantment_haste");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private EnchantmentBehaviours() {
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerTick(player);
        }
    }

    /** The original's livingTick body for players; public for the gametest. */
    public static void playerTick(final ServerPlayer player) {
        if (player.tickCount % REPAIR_INTERVAL_TICKS == 0) {
            repairSweep(player);
        }
        updateSpeed(player);
    }

    // ------------------------------------------------------------------ repair

    private static void repairSweep(final ServerPlayer player) {
        if (player.getAbilities().invulnerable) {
            return;
        }
        for (final ItemStack stack : player.getInventory().items()) {
            if (stack.getDamageValue() > 0 && stack.getItem() instanceof IRepairable) {
                doRepair(stack, player);
            }
        }
        for (final EquipmentSlot slot : ARMOR_SLOTS) {
            final ItemStack stack = player.getItemBySlot(slot);
            if (stack.getDamageValue() > 0 && stack.getItem() instanceof IRepairable) {
                doRepair(stack, player);
            }
        }
    }

    /** Original {@code EventHandlerEntity.doRepair(is, player)} with the table lookup. */
    public static void doRepair(final ItemStack stack, final ServerPlayer player) {
        doRepair(stack, player, EssentiaTags.getObjectTags(stack));
    }

    /**
     * The original's doRepair body; the object-tag lookup is a parameter so
     * the gametest can drive the full vis-payment + mending flow for a probe
     * item that is not in the shipped essentia tables.
     */
    public static void doRepair(final ItemStack stack, final ServerPlayer player,
                                final AspectList objectTags) {
        final int rawLevel = enchantmentLevel(player, ModEnchantments.ENCHANTMENT_REPAIR, stack);
        if (rawLevel <= 0) {
            return;
        }
        final int level = Math.min(rawLevel, REPAIR_LEVEL_CAP);
        if (objectTags == null || objectTags.size() == 0) {
            return;
        }
        final AspectList cost = AspectCombinations.reduceToPrimals(objectTags);
        final AspectList finalCost = new AspectList();
        for (final Aspect aspect : cost.getAspects()) {
            if (aspect != null) {
                finalCost.add(aspect, repairCostAmount(cost.getAmount(aspect), level));
            }
        }
        if (stack.getItem() instanceof IRepairableExtended extended) {
            if (extended.doRepair(stack, player, level) && consumeVisFromInventory(player, finalCost)) {
                mend(stack, player, level);
            }
        } else if (consumeVisFromInventory(player, finalCost)) {
            mend(stack, player, level);
        }
    }

    /**
     * Original per-aspect vis price: {@code (int) Math.sqrt(amount * 2) * level}
     * (doRepair line 201). Pure so the unit test can pin it.
     */
    public static int repairCostAmount(final int aspectAmount, final int level) {
        return (int) Math.sqrt(aspectAmount * 2) * level;
    }

    private static void mend(final ItemStack stack, final ServerPlayer player, final int level) {
        // Original: is.damageItem(-level, player) — a negative damage amount
        // reduces the damage value; hurtAndBreak carries the same rule.
        if (player.level() instanceof ServerLevel serverLevel) {
            stack.hurtAndBreak(-level, serverLevel, player, item -> {
            });
        }
    }

    /**
     * Original {@code WandManager.consumeVisFromInventory}: accessory slots
     * first (the original's ItemAmuletVis baubles), then the main inventory
     * in reverse (the original's ItemWandCasting loop); the first container
     * that can pay the whole cost wins.
     */
    public static boolean consumeVisFromInventory(final ServerPlayer player, final AspectList cost) {
        for (final ItemStack stack : AccessoryService.wornStacks(player)) {
            if (payVisFrom(stack, cost)) {
                return true;
            }
        }
        final List<ItemStack> main = player.getInventory().items();
        for (int i = main.size() - 1; i >= 0; i--) {
            if (payVisFrom(main.get(i), cost)) {
                return true;
            }
        }
        return false;
    }

    private static boolean payVisFrom(final ItemStack stack, final AspectList cost) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IVisContainerItem)) {
            return false;
        }
        final VisPool pool = stack.getOrDefault(ModDataComponents.VIS_POOL, VisPool.EMPTY);
        final AspectList centiCost = new AspectList();
        for (final Aspect aspect : cost.getAspects()) {
            centiCost.add(aspect, cost.getAmount(aspect) * WandVis.CENTI_VIS_PER_VIS);
        }
        final VisPool paid = WandVis.consumeAllVis(pool, centiCost);
        if (paid == null) {
            return false;
        }
        stack.set(ModDataComponents.VIS_POOL, paid);
        return true;
    }

    // ------------------------------------------------------------------- haste

    private static void updateSpeed(final ServerPlayer player) {
        final ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        float bonus = 0.0F;
        if (!player.isCreative() && !boots.isEmpty() && player.getSpeed() > 0.0F) {
            final int haste = enchantmentLevel(player, ModEnchantments.ENCHANTMENT_HASTE, boots);
            if (haste > 0) {
                bonus = haste * HASTE_BONUS_PER_LEVEL;
                if (player.isCrouching()) {
                    bonus /= 2.0F;
                }
                if (player.isInWater()) {
                    bonus /= 2.0F;
                }
            }
        }
        final AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (bonus > 0.0F) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    HASTE_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(HASTE_MODIFIER_ID);
        }
    }

    // ----------------------------------------------------------------- lookup

    private static int enchantmentLevel(final ServerPlayer player, final ResourceKey<Enchantment> key,
                                        final ItemStack stack) {
        return player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }
}
