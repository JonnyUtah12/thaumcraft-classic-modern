package com.jonnyutah12.thaumcraftclassic.runic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Runic shielding pool (TC-M8-02): the port of
 * {@code thaumcraft.common.lib.events.EventHandlerRunic}
 * ({@code common/lib/events/EventHandlerRunic.java}), the system the runic
 * baubles ({@code items/baubles/ItemRingRunic / ItemAmuletRunic /
 * ItemGirdleRunic}) drive and that runic armor (TC-M6-07) will share.
 *
 * <p>Rules preserved term-for-term (values pinned in
 * docs/balance-values.md §1.7, §10.3):</p>
 * <ul>
 * <li>gear scan every 40 player ticks or when {@code isDirty}
 *     (EventHandlerRunic.java:49-50); capacity = sum of
 *     {@code getFinalCharge} over the four armor slots AND the baubles set
 *     (lines 52-84) — armor slots are scanned here so TC-M6-07 armor plugs
 *     in without touching this class;</li>
 * <li>per-unit recharge costing AIR+TERRA {@code shieldCost} centi-vis from
 *     the player's vis containers, interval {@code shieldRecharge -
 *     chargedRings*500} ms on a real-time clock (lines 118-131);</li>
 * <li>damage absorption with the original's truncating charge subtraction
 *     and its four excluded damage sources (starve, drown, outOfWorld,
 *     wither — lines 176-181, SRG-resolved via
 *     reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv; 208-216);</li>
 * <li>on-depletion upgrades: kinetic push burst (radius
 *     {@code 1.5 + n*0.5}, 20 s cooldown, lines 218-229), revitalizing
 *     Regeneration (240 ticks, amplifier = ring count, 20 s cooldown,
 *     lines 231-242), emergency refill ({@code min(max, 8*n)}, 60 s
 *     cooldown, lines 244-253), then {@code rechargeDelay = shieldWait}
 *     (lines 259-261).</li>
 * </ul>
 *
 * <p>State is runtime-only, exactly like the original's HashMaps (never
 * saved; rebuilt from worn gear on the next scan). The original keyed by
 * entity id and never evicted; the port keys by player UUID and evicts on
 * logout and server stop (same semantics, no leak).</p>
 *
 * <p>Deferred to their milestones, cited at the call sites: the
 * {@code PacketRunicCharge} HUD sync and {@code PacketFXShield} hit effect
 * (client work, TC-M7-04/M2-03) and the fortress-helm mask effects
 * (TC-M6-07 armor).</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class RunicShielding {

    /** The original's {@code runicInfo} tuple: {max, charged, kinetic, healing, emergency}. */
    public record GearRow(int max, int charged, int kinetic, int healing, int emergency) {

        public static final GearRow NONE = new GearRow(0, 0, 0, 0, 0);
    }

    /** One contributing stack, already resolved to its final charge and flags. */
    public record RunicGear(int finalCharge, boolean charged, boolean kinetic,
                            boolean healing, boolean emergency) {
    }

    /** Absorption outcome: remaining pool charge and remaining damage. */
    public record Absorption(int chargeLeft, float damageLeft) {
    }

    private static final class ShieldState {
        int charge;
        long nextCycle;
        int lastCharge = -1;
        // The original kept one global rechargeDelay (EventHandlerRunic.java:32)
        // shared by every player; per-player is identical in singleplayer and
        // removes the cross-player coupling (same treatment as the per-player
        // maps the original already had).
        int rechargeDelay;
        final Map<Integer, Long> upgradeCooldown = new HashMap<>();
    }

    private static final Map<UUID, GearRow> ROWS = new ConcurrentHashMap<>();
    private static final Map<UUID, ShieldState> STATES = new ConcurrentHashMap<>();

    /** The original's global {@code isDirty} flag (EventHandlerRunic.java:31). */
    private static volatile boolean dirty = true;

    /** Cooldowns, verbatim from the original (ms): kinetic/healing 20 s, emergency 60 s. */
    public static final long UPGRADE_COOLDOWN_MS = 20_000L;
    public static final long EMERGENCY_COOLDOWN_MS = 60_000L;

    // ------------------------------------------------------------------ pure core

    /**
     * The original's {@code getFinalCharge} (EventHandlerRunic.java:343-354):
     * base capacity plus the {@code RS.HARDEN} hardening bonus (carried here
     * as the {@code thaumcraft:runic_hardening} component; written by the
     * Runic Augmentation flow, TC-M6-07).
     */
    public static int finalCharge(final int baseCharge, final int hardening) {
        return baseCharge + hardening;
    }

    /** Stack-reading wrapper of {@link #finalCharge(int, int)}. */
    public static int finalChargeOf(final IRunicArmor armor, final ItemStack stack) {
        final int hardening = stack.getOrDefault(ModDataComponents.RUNIC_HARDENING.get(), 0);
        return finalCharge(armor.getRunicCharge(stack), hardening);
    }

    /**
     * The original's scan accumulation (EventHandlerRunic.java:52-84): sum
     * every IRunicArmor stack's final charge into {@code max}. The original
     * counted upgrade flags only inside the bauble loop, instanceof-testing
     * the three concrete item classes and switching on their metas (lines
     * 66-80); the port generalizes that through {@link RunicUpgradeFlags}
     * with identical per-item results, so TC-M6-07 armor can contribute
     * flags through the same contract.
     */
    public static GearRow gearRow(final List<RunicGear> gear) {
        int max = 0;
        int charged = 0;
        int kinetic = 0;
        int healing = 0;
        int emergency = 0;
        for (final RunicGear g : gear) {
            max += g.finalCharge();
            if (g.charged()) {
                charged++;
            }
            if (g.kinetic()) {
                kinetic++;
            }
            if (g.healing()) {
                healing++;
            }
            if (g.emergency()) {
                emergency++;
            }
        }
        return new GearRow(max, charged, kinetic, healing, emergency);
    }

    /** Recharge interval, ms: {@code shieldRecharge - charged*500} (line 127). */
    public static int rechargeIntervalMs(final int shieldRecharge, final int chargedCount) {
        return shieldRecharge - chargedCount * 500;
    }

    /**
     * Absorption with the original's truncating charge math (lines 208-215):
     * {@code charge > damage} leaves {@code (int)(charge - damage)} and zero
     * damage; otherwise the damage is reduced by the whole charge and the
     * pool hits zero.
     */
    public static Absorption absorb(final int charge, final float damage) {
        if (charge > damage) {
            return new Absorption((int) (charge - damage), 0.0F);
        }
        return new Absorption(0, damage - charge);
    }

    /** Emergency refill: {@code min(max, 8 * emergencyCount)} (lines 246-248). */
    public static int emergencyRefill(final int max, final int emergencyCount) {
        final int t = 8 * emergencyCount;
        return Math.min(max, t);
    }

    /** Kinetic push radius: {@code 1.5 + kineticCount * 0.5} (line 226). */
    public static float kineticRadius(final int kineticCount) {
        return 1.5F + kineticCount * 0.5F;
    }

    /**
     * The four damage sources the shield never absorbs (lines 176-181):
     * starve, drown, outOfWorld, wither — SRG fields field_76366_f,
     * field_76369_e, field_76380_i, field_82727_n resolved via
     * reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv. Identity-
     * compared against the level's cached sources, as the original compared
     * DamageSource constants.
     */
    public static boolean isBlockedSource(final DamageSource source, final Level level) {
        final var sources = level.damageSources();
        return source == sources.starve()
                || source == sources.drown()
                || source == sources.fellOutOfWorld()
                || source == sources.wither();
    }

    /**
     * The original's {@code WandManager.consumeVisFromInventory}
     * ({@code common/items/wands/WandManager.java:88-111}): worn vis
     * containers first (the original checked {@code ItemAmuletVis} baubles),
     * then the main inventory in reverse slot order ({@code ItemWandCasting}
     * there), each paying all-or-nothing. The port matches any stack carrying
     * a {@code thaumcraft:vis_pool} component — wands (TC-M6-01) and the vis
     * amulet (TC-M8-02) both store charge in it (docs/architecture/vis.md).
     */
    public static boolean payVisFromInventory(final Player player, final AspectList centiCosts) {
        for (final ItemStack worn : AccessoryService.wornStacks(player)) {
            if (worn.has(ModDataComponents.VIS_POOL.get())) {
                final var paid = WandVis.consumeAllVis(WandVis.poolOf(worn), centiCosts);
                if (paid != null) {
                    WandVis.setPool(worn, paid);
                    return true;
                }
            }
        }
        for (int slot = player.getInventory().getContainerSize() - 1; slot >= 0; slot--) {
            final ItemStack stack = player.getInventory().getItem(slot);
            if (stack.has(ModDataComponents.VIS_POOL.get())) {
                final var paid = WandVis.consumeAllVis(WandVis.poolOf(stack), centiCosts);
                if (paid != null) {
                    WandVis.setPool(stack, paid);
                    return true;
                }
            }
        }
        return false;
    }

    /** The original's recharge cost: AIR + EARTH, {@code shieldCost} centi-vis each. */
    public static AspectList rechargeCost() {
        return new AspectList()
                .add(Aspect.getAspect("aer"), TcConfig.getInt(TcConfigCatalog.RUNIC_COST))
                .add(Aspect.getAspect("terra"), TcConfig.getInt(TcConfigCatalog.RUNIC_COST));
    }

    // ------------------------------------------------------------- event wiring

    /** Runic gear calls this from onEquipped/onUnequipped (original: {@code isDirty = true}). */
    public static void markDirty() {
        dirty = true;
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        final Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        tick(player, System.currentTimeMillis());
    }

    /**
     * One player tick of the original's {@code livingTick} (lines 46-136),
     * split out with an injectable clock so gametests can drive the
     * real-time recharge cycle deterministically.
     */
    public static void tick(final Player player, final long nowMs) {
        final UUID id = player.getUUID();
        if (dirty || player.tickCount % 40 == 0) {
            dirty = false;
            final GearRow row = scanGear(player);
            if (row.max() > 0) {
                ROWS.put(id, row);
                final ShieldState state = STATES.computeIfAbsent(id, k -> new ShieldState());
                if (state.charge > row.max()) {
                    state.charge = row.max();
                }
            } else {
                ROWS.remove(id);
                final ShieldState state = STATES.computeIfAbsent(id, k -> new ShieldState());
                state.charge = 0;
            }
        }

        final ShieldState state = STATES.get(id);
        if (state == null) {
            return;
        }
        if (state.rechargeDelay > 0) {
            state.rechargeDelay--;
        } else if (ROWS.containsKey(id)) {
            final GearRow row = ROWS.get(id);
            int charge = state.charge;
            if (charge > row.max()) {
                // Original quirk (lines 122-124): the clamp is local only;
                // the map keeps its value until the next write.
                charge = row.max();
            } else if (charge < row.max()
                    && state.nextCycle < nowMs
                    && payVisFromInventory(player, rechargeCost())) {
                state.nextCycle = nowMs + rechargeIntervalMs(
                        TcConfig.getInt(TcConfigCatalog.RUNIC_RECHARGE_SPEED), row.charged());
                state.charge = ++charge;
            }
            // lastCharge/packet sync (lines 132-135) deferred: client HUD is TC-M7-04.
            state.lastCharge = charge;
        }
    }

    /**
     * Scan the wearer's runic gear: the original read the four armor slots
     * ({@code player.inventory.armorInventory}, lines 55-62) and the baubles
     * inventory (lines 64-83). Armor slots are included here so TC-M6-07
     * runic armor joins the pool through the same {@link IRunicArmor}
     * contract without changes to this class.
     */
    public static GearRow scanGear(final Player player) {
        final List<RunicGear> gear = new ArrayList<>();
        for (final EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            collectRunic(player.getItemBySlot(slot), gear);
        }
        for (final ItemStack worn : AccessoryService.wornStacks(player)) {
            collectRunic(worn, gear);
        }
        return gearRow(gear);
    }

    private static void collectRunic(final ItemStack stack, final List<RunicGear> out) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRunicArmor armor)) {
            return;
        }
        final RunicUpgradeFlags flags = armor instanceof RunicUpgradeFlags f ? f : null;
        out.add(new RunicGear(finalChargeOf(armor, stack),
                flags != null && flags.charged(stack),
                flags != null && flags.kinetic(stack),
                flags != null && flags.healing(stack),
                flags != null && flags.emergency(stack)));
    }

    @SubscribeEvent
    public static void onIncomingDamage(final LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        event.setAmount(hurt(player, event.getSource(), event.getAmount(), System.currentTimeMillis()));
    }

    /**
     * The original's player branch of {@code entityHurt} (lines 174-266)
     * minus the fortress-helm mask effects (TC-M6-07 armor). Returns the
     * damage left after absorption; the event wrapper writes it back.
     */
    public static float hurt(final Player player, final DamageSource source,
                            final float damage, final long nowMs) {
        if (isBlockedSource(source, player.level())) {
            return damage;
        }
        final UUID id = player.getUUID();
        final GearRow row = ROWS.get(id);
        final ShieldState state = STATES.get(id);
        if (row == null || state == null || state.charge <= 0) {
            return damage;
        }

        // FX shield packet (lines 194-205) deferred to client milestones.
        final Absorption result = absorb(state.charge, damage);
        int charge = result.chargeLeft();
        float left = result.damageLeft();

        if (charge <= 0 && row.kinetic() > 0
                && cooldownReady(state, 2, nowMs)) {
            state.upgradeCooldown.put(2, nowMs + UPGRADE_COOLDOWN_MS);
            // Original: world.newExplosion(player, x, y+h/2, z, r, false, false)
            // (EventHandlerRunic.java:224-229) — no fire, no damage, no block
            // damage, push only; causeDamage=false maps to ExplosionInteraction.NONE.
            player.level().explode(null,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(),
                    kineticRadius(row.kinetic()), false,
                    net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        }

        if (charge <= 0 && row.healing() > 0
                && cooldownReady(state, 3, nowMs)) {
            state.upgradeCooldown.put(3, nowMs + UPGRADE_COOLDOWN_MS);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, row.healing()));
            player.level().playSound(null, player, ModSounds.RUNIC_SHIELD_EFFECT.get(),
                    net.minecraft.sounds.SoundSource.MASTER, 1.0F, 1.0F);
        }

        if (charge <= 0 && row.emergency() > 0
                && cooldownReady(state, 4, nowMs)) {
            state.upgradeCooldown.put(4, nowMs + EMERGENCY_COOLDOWN_MS);
            charge = emergencyRefill(row.max(), row.emergency());
            dirty = true;
            player.level().playSound(null, player, ModSounds.RUNIC_SHIELD_CHARGE.get(),
                    net.minecraft.sounds.SoundSource.MASTER, 1.0F, 1.0F);
        }

        if (charge <= 0) {
            state.rechargeDelay = TcConfig.getInt(TcConfigCatalog.RUNIC_RECHARGE_DELAY);
        }

        state.charge = charge;
        return left;
    }

    private static boolean cooldownReady(final ShieldState state, final int key, final long nowMs) {
        return !state.upgradeCooldown.containsKey(key) || state.upgradeCooldown.get(key) < nowMs;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        ROWS.remove(event.getEntity().getUUID());
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        ROWS.clear();
        STATES.clear();
    }

    // Test/game-test accessors: the pool is runtime state the acceptance
    // harness asserts directly (the original exposed the same maps publicly).
    public static GearRow currentRow(final Player player) {
        return ROWS.getOrDefault(player.getUUID(), GearRow.NONE);
    }

    public static int currentCharge(final Player player) {
        final ShieldState state = STATES.get(player.getUUID());
        return state == null ? 0 : state.charge;
    }

    public static void resetForTest() {
        ROWS.clear();
        STATES.clear();
        dirty = true;
    }

    private RunicShielding() {
    }
}
