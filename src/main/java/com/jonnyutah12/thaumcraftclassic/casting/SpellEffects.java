package com.jonnyutah12.thaumcraftclassic.casting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Where a {@link SpellEffect} is performed (TC-M6-03-s).
 *
 * <p>The original focus bodies did the work inline: they constructed the
 * projectile and added it to the world, or walked the architect area and placed
 * blocks. Those bodies span five milestones' worth of content — the seven focus
 * projectiles ({@code thaumcraft:ember}, {@code frostshard}, {@code shockorb},
 * {@code explosiveorb}, {@code primalorb}, {@code pechblast}, {@code firebat} —
 * are the entity cards' (their content-mapping rows are {@code not_started}),
 * {@code blockWarded}+{@code TileWarded} the warding card's, the swapper the
 * equal-trade card's, {@code blockHole}+{@code TileHole} the portable-hole
 * card's.</p>
 *
 * <p>So the port splits the body in two. A behaviour <em>decides</em> the exact
 * effect the original built (its {@link SpellEffect}, with every parameter the
 * original set) and hands it here; this class performs it through a registry
 * keyed by {@link SpellEffect#key()}. The one effect that is not content —
 * {@link SpellEffect.Bolt}, the shock focus's lightning — is implemented here,
 * because it is plain damage plus the original's chain, and that makes the
 * shock focus a real, playable behaviour today.</p>
 *
 * <p>A key with no handler changes nothing and returns {@code false}; the
 * casting seam then refunds the vis (see {@link WandCasting}), so a focus whose
 * content has not landed never burns a wand's charge. Handlers are registered
 * by the cards that bring the content, and by game tests for the duration of a
 * probe (the parent's {@code FocusBehaviors} test pattern).</p>
 */
public final class SpellEffects {

    /** Performs one effect; true when the world actually changed. */
    @FunctionalInterface
    public interface Handler {
        boolean perform(CastContext context, SpellEffect effect);
    }

    private static final Map<String, Handler> HANDLERS = new LinkedHashMap<>();

    /** Registers (or replaces) the handler for an effect key. */
    public static void register(final String key, final Handler handler) {
        if (key == null || handler == null) {
            throw new IllegalArgumentException("an effect needs a key and a handler");
        }
        HANDLERS.put(key, handler);
    }

    /** Removes a handler — the test-side unregister in a {@code finally}. */
    public static void unregister(final String key) {
        HANDLERS.remove(key);
    }

    /** Whether an effect key can be performed today. */
    public static boolean isRegistered(final String key) {
        return HANDLERS.containsKey(key);
    }

    /** The keys with a handler. */
    public static List<String> registeredKeys() {
        return Collections.unmodifiableList(new ArrayList<>(HANDLERS.keySet()));
    }

    /**
     * Performs {@code effect} for {@code context}; false when nothing could be
     * done (no handler for the effect's key, or the handler declined), which the
     * casting seam turns into a refund.
     */
    public static boolean perform(final CastContext context, final SpellEffect effect) {
        if (context == null || effect == null) {
            return false;
        }
        if (effect instanceof SpellEffect.Bolt bolt) {
            return bolt(context, bolt);
        }
        final Handler handler = HANDLERS.get(effect.key());
        return handler != null && handler.perform(context, effect);
    }

    // ------------------------------------------------------------- the bolt

    /**
     * The original's bolt, server half
     * ({@code ItemFocusShock.doLightningBolt}, {@code ItemFocusShock.java:191-236}):
     * damage the pointed entity ({@code (chain > 0 ? 6 : 4) + potency}, the
     * caller's {@link SpellEffect.Bolt#damage()}), then if
     * {@code chainlightning} is applied jump to the nearest not-yet-hit living
     * entity within 8 blocks, {@code 4 + potency} per jump, up to
     * {@code 2 * chain level + 2 * enlarge level} jumps.
     *
     * <p>The original also skipped players unless the server's PVP flag was on
     * ({@code MinecraftServer.func_71276_C().func_71219_W()}); the port asks the
     * {@link ServerLevel}'s server the same question. The FX bolt
     * ({@code shootLightning}, and the client half of this method) is TC-M7-02's;
     * the port's server body is the damage, exactly as the original's
     * {@code !world.isRemote} branch was.</p>
     */
    private static boolean bolt(final CastContext context, final SpellEffect.Bolt bolt) {
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final Player player = context.player();
        final Entity pointed = CastTargets.pointedEntity(level, player, bolt.range(), bolt.padding());
        if (!(pointed instanceof LivingEntity target) || !attackable(level, target, player)) {
            // The original's bolt found nothing to hit: no damage, but the tick
            // was still paid for (the cast happened).
            return false;
        }
        target.hurt(level.damageSources().playerAttack(player), bolt.damage());
        if (bolt.jumps() <= 0) {
            return true;
        }
        final List<Entity> hit = new ArrayList<>();
        hit.add(target);
        LivingEntity centre = target;
        int remaining = bolt.jumps();
        while (remaining-- > 0) {
            final LivingEntity closest = nearestChainTarget(level, player, centre, hit);
            if (closest == null) {
                break;
            }
            hit.add(closest);
            closest.hurt(level.damageSources().playerAttack(player), bolt.jumpDamage());
            centre = closest;
        }
        return true;
    }

    /** The original's 8.0 block nearest-target search ({@code ItemFocusShock.java:206-220}). */
    private static LivingEntity nearestChainTarget(final ServerLevel level, final Player player,
                                                   final LivingEntity centre, final List<Entity> hit) {
        final List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(centre.blockPosition()).inflate(8.0D),
                candidate -> !hit.contains(candidate) && attackable(level, candidate, player));
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (final LivingEntity candidate : candidates) {
            final double distance = candidate.distanceToSqr(centre);
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    /** {@code !(pointedEntity instanceof EntityPlayer) || server.isPVPEnabled()}. */
    private static boolean attackable(final ServerLevel level, final LivingEntity target, final Player player) {
        if (target == player || !target.isAlive()) {
            return false;
        }
        return !(target instanceof Player) || level.getServer() == null
                || level.getServer().isPvpAllowed();
    }

    private SpellEffects() {
    }
}
