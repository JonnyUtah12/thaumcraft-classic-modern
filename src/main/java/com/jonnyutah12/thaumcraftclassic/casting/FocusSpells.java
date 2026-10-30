package com.jonnyutah12.thaumcraftclassic.casting;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The ten focus spell bodies (TC-M6-03-s): the port of every
 * {@code onFocusRightClick}/{@code onUsingFocusTick} the original focus classes
 * declared, wired into the TC-M6-02 casting seam by
 * {@link #registerAll()}.
 *
 * <p>Each body is the same three steps the original performed, in its order:</p>
 * <ol>
 *   <li><b>the combination</b> — {@code FocusCombinations.resolve} runs the
 *       original's {@code isUpgradedWith(...) ? … : …} chain, which is also what
 *       priced the cast ({@code ItemFocus.getVisCost} prices through the same
 *       table, so the price paid and the body are the same decision)</li>
 *   <li><b>the original's guards</b> — the moving-object hit the wand resolved
 *       in {@code ItemWandCasting.func_77659_a} for the architecture and
 *       portable-hole foci (a focus with no pointed block did nothing, and
 *       nothing was charged: {@code ItemFocusWarding.java:89},
 *       {@code ItemFocusTrade.java:99}, {@code ItemFocusPortableHole.java:85}),
 *       and the hellbat's pointed living target within 32 blocks
 *       ({@code ItemFocusHellbat.java:87-104}; the original also required the
 *       target's class to be {@code EntityFireBat} when the wand's own GUI path
 *       called it, an artefact of the summon search)</li>
 *   <li><b>the effect</b> — {@link FocusSpellSpecs} computes exactly what the
 *       original built and {@link SpellEffects} performs it</li>
 * </ol>
 *
 * <h2>What "no effect" means here</h2>
 * A body returns false when nothing happened, and {@link WandCasting} refunds
 * the vis it had just charged — the port's own rule (the original paid first
 * and spawned second, so a failed spawn still cost vis). The projectiles the
 * foci use are the entity cards' content, so until those land a projectile
 * focus reports no effect and costs nothing; the shock focus's bolt is plain
 * damage plus the original's chain, so it is implemented and casts for real
 * today. {@link SpellEffects} lists the seams.</p>
 *
 * <h2>The channel (per-tick) foci</h2>
 * {@code fire} (base and beam), {@code shock} (base and chain) and
 * {@code excavation} ran their bodies in {@code onUsingFocusTick} while the use
 * button was held: the right-click started an unbounded use
 * ({@code p.func_71008_a(itemstack, Integer.MAX_VALUE)}) and every tick paid and
 * fired. Those combinations are marked
 * {@link FocusCombinations.Combination#channeled()}; the right-click reports
 * {@link WandCasting.Status#CHANNELED} and {@code ItemWandCasting.onUseTick}
 * drives {@link WandCasting#channelTick}, which pays and fires one tick the same
 * way this body does.
 */
public final class FocusSpells {

    /** {@code ItemFocusPortableHole.java:100}: {@code int maxdis = 33 + enlarge * 8;}. */
    public static final int PORTABLE_HOLE_MAX_DISTANCE = 33;
    public static final int PORTABLE_HOLE_ENLARGE_STEP = 8;
    /** {@code ItemFocusPortableHole.java:141}: {@code short dur = (short)(120 + 60 * di);}. */
    public static final int PORTABLE_HOLE_DURATION = 120;
    public static final int PORTABLE_HOLE_EXTEND_STEP = 60;
    /** {@code ItemFocusHellbat.java:87}: {@code EntityUtils.getPointedEntity(player.level, player, 32.0, EntityFireBat.class)}. */
    public static final double HELLBAT_TARGET_RANGE = 32.0D;

    private static boolean registered;

    /** Registers the ten bodies on the TC-M6-02 seam; called once from registration wiring. */
    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        for (final Focus focus : Focuses.all()) {
            FocusBehaviors.register(focus.tag(), FocusSpells::cast);
        }
    }

    /** The chain the right-click path uses; the ten bodies are registered by {@link #registerAll()}. */
    /**
     * One focus body. Returns false when the cast did nothing — no combination,
     * the original's own guard failed, or the effect has no implementation yet —
     * so {@link WandCasting} refunds it.
     */
    static boolean cast(final CastContext context) {
        final String tag = context.focus().tag();
        final FocusUpgradeList upgrades = ItemFocus.upgrades(context.focusStack());
        final FocusCombinations.Combination combination = FocusCombinations.resolve(tag, upgrades);
        if (combination == null) {
            return false;
        }
        final ItemStack wandStack = context.wandStack();
        final ItemWandCasting wand = context.wand();
        final int potency = ItemWandCasting.focusPotency(wandStack);
        final int enlarge = ItemWandCasting.focusEnlarge(wandStack);
        final int extend = ItemWandCasting.focusExtend(wandStack);
        final int treasure = ItemWandCasting.focusTreasure(wandStack);

        int distance = 0;
        int side = 1;
        if ("portablehole".equals(tag)) {
            final BlockHitResult hit = CastTargets.pointedBlock(context.level(), context.player());
            if (hit == null) {
                return false;
            }
            side = hit.getDirection().get3DDataValue();
            distance = CastTargets.holeDistance(context.level(), hit.getBlockPos(), hit.getDirection(),
                    PORTABLE_HOLE_MAX_DISTANCE + enlarge * PORTABLE_HOLE_ENLARGE_STEP);
            // The original priced the whole cast at (distance + 1) times the base cost and
            // paid it all-or-nothing (ItemFocusPortableHole.java:133-139). The port pays the
            // base first (the seam prices before the body), so the remaining `distance`
            // multiples are charged here; if the wand cannot cover them the body reports no
            // effect and the base is refunded — the same net spend as the original's.
            if (distance > 0 && !wand.consumeAllVis(wandStack, context.player(),
                    multiplied(context.baseCost(), distance), true, false)) {
                return false;
            }
        } else if ("warding".equals(tag)) {
            final BlockHitResult hit = CastTargets.pointedBlock(context.level(), context.player());
            if (hit == null || !CastTargets.wardeable(context.level(), hit.getBlockPos())) {
                return false;
            }
        } else if ("trade".equals(tag)) {
            if (CastTargets.pointedBlock(context.level(), context.player()) == null) {
                return false;
            }
        } else if ("hellbat".equals(tag)) {
            if (!hasSummonTarget(context)) {
                return false;
            }
        }

        final FocusSpellSpecs.SpellInput input = new FocusSpellSpecs.SpellInput(upgrades, potency, enlarge,
                extend, treasure, distance, side);
        final SpellEffect effect = FocusSpellSpecs.spec(tag, combination, input);
        return effect != null && SpellEffects.perform(context, effect);
    }

    /**
     * {@code ItemFocusHellbat.java:99-104}: the summon needs a pointed living
     * entity, and the original refused a player target on a non-PVP server
     * ({@code pointedEntity instanceof EntityPlayer && !MinecraftServer.isPVPEnabled()}).
     */
    private static boolean hasSummonTarget(final CastContext context) {
        final Entity pointed = CastTargets.pointedEntity(context.level(), context.player(),
                HELLBAT_TARGET_RANGE, 1.1F);
        if (!(pointed instanceof LivingEntity) || pointed == context.player()) {
            return false;
        }
        if (pointed instanceof Player && context.level() instanceof ServerLevel serverLevel
                && serverLevel.getServer() != null && !serverLevel.getServer().isPvpAllowed()) {
            return false;
        }
        return true;
    }

    /**
     * One aspect price times {@code factor} — the original's
     * {@code c.merge(a, c.getAmount(a) * distance)} over the base cost
     * ({@code ItemFocusPortableHole.java:135-137}). The cap's consumption
     * modifier is applied by {@code consumeAllVis}, as for every other cast.
     */
    private static AspectList multiplied(final AspectList cost, final int factor) {
        final AspectList out = new AspectList();
        for (final Aspect aspect : cost.getAspects()) {
            out.add(aspect, cost.getAmount(aspect) * factor);
        }
        return out;
    }

    /** The twenty-one upgrade ids, re-exported so a docs/test scan has one place to read them. */
    public static java.util.List<String> upgradeTags() {
        return FocusUpgrades.all().stream().map(type -> type.tag()).toList();
    }

    private FocusSpells() {
    }
}
