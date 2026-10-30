package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.casting.FocusSpellSpecs;
import com.jonnyutah12.thaumcraftclassic.casting.FocusSpells;
import com.jonnyutah12.thaumcraftclassic.casting.SpellEffect;
import com.jonnyutah12.thaumcraftclassic.casting.SpellEffects;
import com.jonnyutah12.thaumcraftclassic.casting.WandCasting;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * TC-M6-03-s acceptance probes: the per-focus spell bodies on the live server —
 * the ten tags registered with an effect for every combination, the
 * combination's price billed through the TC-M4-02 chain and the original's
 * effect requested with the original's numbers, the per-tick channel, and one
 * effect that is genuinely performed (the shock focus's bolt damages and chains).
 *
 * <p>The probes drive {@link WandCasting} — the parent slice's entry point —
 * rather than any new path, and register their own effect handlers for the
 * duration of a probe ({@code SpellEffects.register} then {@code unregister} in a
 * {@code finally}), exactly as the parent's {@code FociGameTest} does for
 * behaviours. A focus whose effect handler is absent must report
 * {@link WandCasting.Status#NO_EFFECT} and leave the wand untouched, which is
 * the port's rule that unimplemented content never burns charge.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FociBehaviorsGameTest {

    private FociBehaviorsGameTest() {
    }

    /** The ten bodies are registered at mod init and every combination has its ported effect. */
    @GameTest(template = "empty")
    public static void everyFocusIsRegisteredWithItsCombinations(final GameTestHelper helper) {
        int combinations = 0;
        for (final String tag : Focuses.tags()) {
            helper.assertTrue(FocusCombinations.variants(tag).stream().anyMatch(
                            combination -> FocusSpellSpecs.has(tag, combination.id())),
                    tag + " has no ported effect");
            final List<FocusCombinations.Combination> variants = FocusCombinations.variants(tag);
            helper.assertTrue(!variants.isEmpty(), tag + " has no combination");
            for (final FocusCombinations.Combination combination : variants) {
                helper.assertTrue(FocusSpellSpecs.has(tag, combination.id()),
                        tag + "/" + combination.id() + " is priced but not ported");
                combinations++;
            }
        }
        helper.assertTrue(combinations == 23, "the ten foci offer 23 combinations, got " + combinations);
        helper.assertTrue(FocusSpells.upgradeTags().size() == 21,
                "the original declares 21 focus upgrades, got " + FocusSpells.upgradeTags().size());
        // The upgrade table is the id space a focus stack serializes.
        for (int id = 0; id <= 20; id++) {
            helper.assertTrue(FocusUpgrades.byId(id) != null, "upgrade id " + id + " is missing");
        }
        ThaumcraftClassic.LOGGER.info("[TC-M6-03-s] {} focus combinations have a ported effect", combinations);
        helper.succeed();
    }

    /**
     * An upgraded focus is priced by the original's combination and its effect is
     * requested with the original's numbers: the fire focus with {@code fireball}
     * bills ignis 66 + perditio 33 through the iron cap's 1.1F modifier and asks
     * for one explosive orb of strength {@code 1.0F + potency * 0.4F}.
     */
    @GameTest(template = "empty")
    public static void anUpgradedCombinationBillsAndFiresAsTheOriginalDid(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("fire").get());
        // The ball combination lives at rank 3 of the fire focus's tree.
        helper.assertTrue(ItemFocus.upgrades(focus).withUpgrade(FocusUpgrades.FIREBALL, 3).isPresent(),
                "fireball must be applicable at rank 3");
        ItemFocus.setUpgrades(focus, FocusUpgradeList.single(FocusUpgrades.FIREBALL, 3));
        ItemWandCasting.setFocus(wand, focus);
        // The wand stores the focus's tag, so this also proves the upgrades are snapshotted with it.
        helper.assertTrue(ItemFocus.upgrades(ItemWandCasting.focusItem(wand)).isUpgradedWith(FocusUpgrades.FIREBALL),
                "the wand's rebuilt focus must keep its applied upgrades");
        helper.assertTrue(ItemFocus.focusOf(focus) == Focuses.byTag("fire"),
                "the focus stack must still carry its table entry");

        final Aspect ignis = Aspect.getAspect("ignis");
        final Aspect perditio = Aspect.getAspect("perditio");
        // Centi-vis amounts, both inside the wood rod's 2500 capacity (WandVis.addVis clamps).
        wandItem.addRealVis(wand, ignis, 2000, true);
        wandItem.addRealVis(wand, perditio, 2000, true);
        helper.assertTrue(wandItem.getVis(wand, ignis) == 2000 && wandItem.getVis(wand, perditio) == 2000,
                "the probe's wand must be funded to 2000 centi-vis per aspect");

        // 1. Without a handler for the orb the cast must report no effect and refund everything.
        WandCasting.CastResult result = WandCasting.cast(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(result.status() == WandCasting.Status.NO_EFFECT,
                "an unported projectile must report NO_EFFECT, got " + result.status());
        helper.assertTrue(result.untouched(), "a no-effect cast reports nothing paid");
        helper.assertTrue(wandItem.getVis(wand, ignis) == 2000 && wandItem.getVis(wand, perditio) == 2000,
                "the refund must restore both aspects exactly");

        // 2. With a recorder the cast succeeds, at the combination's price and with the
        //    original's projectile parameters.
        final List<SpellEffect> seen = new ArrayList<>();
        try {
            SpellEffects.register(SpellEffect.Projectile.EXPLOSIVE_ORB, (context, effect) -> {
                seen.add(effect);
                return true;
            });
            result = WandCasting.cast(wandItem, wand, player, helper.getLevel(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result.status() == WandCasting.Status.CAST,
                    "a supported, affordable cast must succeed, got " + result.status());
            helper.assertTrue(result.cost().getAmount(ignis) == 72,
                    "ignis 66 through the iron cap's 1.1F is 72 centi-vis, got " + result.cost().getAmount(ignis));
            helper.assertTrue(result.cost().getAmount(perditio) == 36,
                    "perditio 33 through the iron cap's 1.1F is 36 centi-vis, got "
                            + result.cost().getAmount(perditio));
            helper.assertTrue(wandItem.getVis(wand, ignis) == 2000 - 72
                            && wandItem.getVis(wand, perditio) == 2000 - 36,
                    "both aspects of the combination must be debited");
            helper.assertTrue(seen.size() == 1, "the fireball combination fires one orb, got " + seen.size());
            final SpellEffect.Projectile orb = (SpellEffect.Projectile) seen.get(0);
            helper.assertTrue(orb.count() == 1 && orb.strength() == 1.0F,
                    "an unupgraded fireball is one orb of strength 1.0F, got " + orb);
            helper.assertTrue(!orb.onFire(), "alchemist's fire is not applied");
        } finally {
            SpellEffects.unregister(SpellEffect.Projectile.EXPLOSIVE_ORB);
        }
        helper.assertTrue(!SpellEffects.isRegistered(SpellEffect.Projectile.EXPLOSIVE_ORB),
                "the probe must clean up its handler");
        helper.succeed();
    }

    /**
     * The per-tick channel: an unupgraded fire focus starts one on the right-click
     * (nothing paid), and each tick pays the base price and asks for
     * {@code 2 + potency} embers with the original's damage and scatter.
     */
    @GameTest(template = "empty")
    public static void theChannelPaysPerTickAndFiresTheOriginalStream(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        ItemWandCasting.setFocus(wand, new ItemStack(CatalogFoci.item("fire").get()));
        final Aspect ignis = Aspect.getAspect("ignis");
        wandItem.addRealVis(wand, ignis, 2000, true);

        // The uncombined fire focus is a channel (ItemFocusFire.onUsingFocusTick).
        final WandCasting.CastResult started = WandCasting.cast(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(started.status() == WandCasting.Status.CHANNELED,
                "an uncombined fire focus starts a channel, got " + started.status());
        helper.assertTrue(wandItem.getVis(wand, ignis) == 2000,
                "starting the channel must not charge, got " + wandItem.getVis(wand, ignis));

        final List<SpellEffect> seen = new ArrayList<>();
        try {
            SpellEffects.register(SpellEffect.Projectile.EMBER, (context, effect) -> {
                seen.add(effect);
                return true;
            });
            final WandCasting.CastResult tick = WandCasting.channelTick(wandItem, wand, player, helper.getLevel(),
                    InteractionHand.MAIN_HAND);
            helper.assertTrue(tick.status() == WandCasting.Status.CAST,
                    "a tick whose effect lands must report CAST, got " + tick.status());
            helper.assertTrue(wandItem.getVis(wand, ignis) == 2000 - 11,
                    "each tick bills the base price through the cap modifier (11), got "
                            + wandItem.getVis(wand, ignis));
            helper.assertTrue(seen.size() == 1, "one ember stream per tick");
            final SpellEffect.Projectile stream = (SpellEffect.Projectile) seen.get(0);
            helper.assertTrue(stream.count() == 2, "2 + potency(0) embers per tick, got " + stream.count());
            helper.assertTrue(stream.damage() == 2.0F && stream.scatter() == 15.0F && stream.duration() == 20,
                    "the uncombined stream is damage 2.0F, scatter 15.0F, 20 ticks: " + stream);
        } finally {
            SpellEffects.unregister(SpellEffect.Projectile.EMBER);
        }

        // Without the handler the next tick is refunded and reports no effect (the channel stops).
        final WandCasting.CastResult stopped = WandCasting.channelTick(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(stopped.status() == WandCasting.Status.NO_EFFECT,
                "a tick with no effect must report NO_EFFECT, got " + stopped.status());
        helper.assertTrue(wandItem.getVis(wand, ignis) == 2000 - 11,
                "the refused tick must leave the charge alone, got " + wandItem.getVis(wand, ignis));
        helper.succeed();
    }

    /**
     * The one effect the port performs itself: the shock focus's bolt. An
     * uncombined shock focus damages the pointed cow for {@code 4 + potency}, and
     * with {@code chainlightning} applied it also jumps {@code 2 + 2 * enlarge}
     * times, {@code 4 + potency} per jump, to the nearest other living entity
     * within 8 blocks.
     */
    @GameTest(template = "empty")
    public static void theShockBoltDamagesAndChains(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        player.setYRot(0.0F);
        player.setXRot(0.0F);

        final Cow target = helper.spawn(EntityType.COW, new BlockPos(1, 1, 5));
        target.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 4.5);
        final Cow bystander = helper.spawn(EntityType.COW, new BlockPos(4, 1, 6));
        bystander.setPos(base.getX() + 2.0, base.getY() + 1.0, base.getZ() + 4.5);
        final float targetStart = target.getHealth();
        final float bystanderStart = bystander.getHealth();

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        final ItemStack focus = new ItemStack(CatalogFoci.item("shock").get());
        ItemFocus.setUpgrades(focus, FocusUpgradeList.single(FocusUpgrades.CHAINLIGHTNING, 3));
        ItemWandCasting.setFocus(wand, focus);
        final Aspect aer = Aspect.getAspect("aer");
        final Aspect aqua = Aspect.getAspect("aqua");
        // The chain combination prices aer 40 *and* aqua 10, so both aspects must be funded
        // (an unfunded aspect makes the all-or-nothing payment report UNPAID).
        wandItem.addRealVis(wand, aer, 2000, true);
        wandItem.addRealVis(wand, aqua, 500, true);
        final WandCasting.CastResult result = WandCasting.channelTick(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(result.status() == WandCasting.Status.CAST,
                "the bolt must be performed by the port, got " + result.status());
        helper.assertTrue(result.cost().getAmount(aer) == 44,
                "chainlightning prices aer 40 (44 with the iron cap), got " + result.cost().getAmount(aer));
        helper.assertTrue(target.getHealth() == targetStart - 6.0F,
                "the pointed cow takes 6 + potency(0) = 6.0F, got " + (targetStart - target.getHealth()));
        helper.assertTrue(bystander.getHealth() == bystanderStart - 4.0F,
                "the chain jump deals 4 + potency(0) = 4.0F, got " + (bystanderStart - bystander.getHealth()));
        helper.succeed();
    }

    /** The uncombined shock focus still fires, for 4 + potency, and never bills for nothing. */
    @GameTest(template = "empty")
    public static void theUncombinedBoltStillDamages(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        final Cow target = helper.spawn(EntityType.COW, new BlockPos(1, 1, 5));
        target.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 4.5);
        final float start = target.getHealth();

        final ItemWandCasting wandItem = CatalogWands.WAND_CASTING.get();
        final ItemStack wand = ItemWandCasting.stack(WandComponents.cap("iron"), WandComponents.rod("wood"));
        ItemWandCasting.setFocus(wand, new ItemStack(CatalogFoci.item("shock").get()));
        final Aspect aer = Aspect.getAspect("aer");
        wandItem.addRealVis(wand, aer, 2000, true);

        final WandCasting.CastResult result = WandCasting.channelTick(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(result.status() == WandCasting.Status.CAST, "the plain bolt fires, got " + result.status());
        helper.assertTrue(target.getHealth() == start - 4.0F,
                "an uncombined bolt deals 4 + potency(0), got " + (start - target.getHealth()));

        // A wand that cannot pay reports UNPAID and spends nothing.
        wandItem.storeVis(wand, aer, 1);
        final WandCasting.CastResult unpaid = WandCasting.channelTick(wandItem, wand, player, helper.getLevel(),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(unpaid.status() == WandCasting.Status.UNPAID,
                "a wand that cannot pay reports UNPAID, got " + unpaid.status());
        helper.assertTrue(wandItem.getVis(wand, aer) == 1, "an unpaid tick spends nothing");
        helper.succeed();
    }

    /** An unused import guard: the upgrade type is part of the shipped surface. */
    @SuppressWarnings("unused")
    private static FocusUpgradeType upgrade(final String tag) {
        return FocusUpgrades.byTag(tag);
    }
}
