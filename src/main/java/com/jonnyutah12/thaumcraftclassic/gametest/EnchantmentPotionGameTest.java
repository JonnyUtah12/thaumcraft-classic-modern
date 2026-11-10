package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.enchantment.EnchantmentBehaviours;
import com.jonnyutah12.thaumcraftclassic.items.ItemBathSalts;
import com.jonnyutah12.thaumcraftclassic.items.ItemSanitySoap;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.potions.PotionBlurredVision;
import com.jonnyutah12.thaumcraftclassic.potions.PotionDeathGaze;
import com.jonnyutah12.thaumcraftclassic.potions.PotionFluxTaint;
import com.jonnyutah12.thaumcraftclassic.potions.PotionInfectiousVisExhaust;
import com.jonnyutah12.thaumcraftclassic.potions.PotionSunScorned;
import com.jonnyutah12.thaumcraftclassic.potions.PotionThaumarhia;
import com.jonnyutah12.thaumcraftclassic.potions.PotionUnnaturalHunger;
import com.jonnyutah12.thaumcraftclassic.potions.PotionVisExhaust;
import com.jonnyutah12.thaumcraftclassic.potions.PotionWarpWard;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModEnchantments;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWarpEffects;
import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.warp.WarpEvents;
import com.jonnyutah12.thaumcraftclassic.warp.WarpLogic;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M6-05-s acceptance: the two enchantments' tick behaviours, the nine
 * potion effects' gates and bodies, and the warp mechanic, against the real
 * server paths. Design record: docs/architecture/enchantments-potions.md.
 *
 * <p>The enchantment data itself (weights, cost tables, applicability tags,
 * unbreaking exclusion) is pinned by {@code EnchantmentDataParityTest}; the
 * warp roll's arithmetic and the whole effect table by {@code WarpLogicTest};
 * the original's thresholds and cadence by {@code WarpParityTest} over
 * {@code reference/decompiled}. This class pins the code halves that need a
 * live server: the haste speed modifier and its gates, the repair
 * vis-payment + mending flow, the effect tick gates, the flux-taint damage
 * body, the infectious-exhaustion spread, the unnatural-hunger exhaustion
 * body, the soap's warp cleansing, the warp-event gates/roll/decay, the death
 * gaze's cone, the aspect-pool grant and the eldritch-research seam.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentPotionGameTest {

    private EnchantmentPotionGameTest() {
    }

    // ------------------------------------------------------------- enchantments

    @GameTest(template = "empty")
    public static void hasteBootsRaiseSpeed(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.ENCHANTMENT_HASTE), 3);
        player.setItemSlot(EquipmentSlot.FEET, boots);

        EnchantmentBehaviours.playerTick(player);

        final var modifier = player.getAttribute(Attributes.MOVEMENT_SPEED)
                .getModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID);
        helper.assertTrue(modifier != null, "haste boots must add the speed modifier");
        helper.assertTrue(Math.abs(modifier.amount() - 3 * 0.015F) < 1.0e-6,
                "modifier must be level * 0.015 (original updateSpeed), got " + modifier.amount());

        // Removing the boots and ticking again must drop the modifier.
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        EnchantmentBehaviours.playerTick(player);
        helper.assertTrue(
                !player.getAttribute(Attributes.MOVEMENT_SPEED)
                        .hasModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID),
                "the modifier must be removed when no boots carry haste");
        helper.succeed();
    }

    /**
     * The original's gates are {@code !capabilities.isFlying}, a feet stack and
     * the movement-speed attribute — NOT the creative mode and NOT the player's
     * velocity ({@code EventHandlerEntity.java:292}). A flying survival player
     * gets no bonus; a standing creative player does.
     */
    @GameTest(template = "empty")
    public static void hasteGatesMatchOriginal(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.ENCHANTMENT_HASTE), 2);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        player.getAbilities().flying = true;

        EnchantmentBehaviours.playerTick(player);
        helper.assertTrue(
                !player.getAttribute(Attributes.MOVEMENT_SPEED)
                        .hasModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID),
                "a flying player must not get the haste bonus (the original gates on isFlying)");

        player.getAbilities().flying = false;
        EnchantmentBehaviours.playerTick(player);
        helper.assertTrue(
                player.getAttribute(Attributes.MOVEMENT_SPEED)
                        .hasModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID),
                "a non-flying player with haste boots must get the bonus");

        // The gate reads the movement-speed attribute, not the velocity: a
        // standing player (getSpeed() == 0) keeps the bonus.
        // 1.21.1 LivingEntity#getSpeed() is the MOVEMENT_SPEED attribute (that is
        // what the bonus rides), not the 1.7.10 motionX/motionZ. The
        // "standing still" precondition is the delta movement.
        helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0e-9,
                "the probe player must not be moving for this assertion to mean "
                        + "anything, got " + player.getDeltaMovement());
        helper.assertTrue(
                player.getAttribute(Attributes.MOVEMENT_SPEED)
                        .hasModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID),
                "a standing player keeps the bonus (the original reads field_70701_bs, not motion)");

        // Sneaking halves it (WarpEvents original: EventHandlerEntity.java:296).
        player.setShiftKeyDown(true);
        EnchantmentBehaviours.playerTick(player);
        final var sneaking = player.getAttribute(Attributes.MOVEMENT_SPEED)
                .getModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID);
        helper.assertTrue(sneaking != null && Math.abs(sneaking.amount() - 2 * 0.015F / 2.0F) < 1.0e-6,
                "the bonus must halve while sneaking, got " + (sneaking == null ? "none" : sneaking.amount()));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repairMendsForVisPayment(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ItemStack tool = new ItemStack(TestProbeContent.PROBE_REPAIRABLE.get());
        tool.setDamageValue(10);
        tool.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.ENCHANTMENT_REPAIR), 2);
        // 10 vis of aqua available; the cost below needs 8 vis (see below).
        tool.set(ModDataComponents.VIS_POOL, new VisPool(Map.of("aqua", 1000)));

        // Object tags: aqua x8 (aqua is primal, so reduceToPrimals is identity).
        // Original price: (int) sqrt(8 * 2) * level(2) = 4 * 2 = 8 vis.
        final AspectList objectTags = new AspectList().add(Aspect.getAspect("aqua"), 8);
        ThaumcraftClassic.LOGGER.info("[TC-M6-05] repair probe: damage={} maxDamage={} tags={} pool={}",
                tool.getDamageValue(), tool.getMaxDamage(), objectTags,
                tool.getOrDefault(ModDataComponents.VIS_POOL, VisPool.EMPTY));
        EnchantmentBehaviours.doRepair(tool, player, objectTags);
        ThaumcraftClassic.LOGGER.info("[TC-M6-05] repair probe: after damage={} pool={}",
                tool.getDamageValue(), tool.getOrDefault(ModDataComponents.VIS_POOL, VisPool.EMPTY));

        helper.assertTrue(tool.getDamageValue() == 8,
                "damage must drop by the enchantment level, got " + tool.getDamageValue());
        final VisPool after = tool.getOrDefault(ModDataComponents.VIS_POOL, VisPool.EMPTY);
        helper.assertTrue(after.getVis(Aspect.getAspect("aqua")) == 200,
                "8 vis (800 centi) must be paid from the container, left "
                        + after.getVis(Aspect.getAspect("aqua")));

        // A second sweep cannot pay (200 centi < 800): all-or-nothing, no
        // partial payment and no further mending.
        EnchantmentBehaviours.doRepair(tool, player, objectTags);
        helper.assertTrue(tool.getDamageValue() == 8,
                "an unaffordable repair must not partially pay or mend");
        helper.succeed();
    }

    /**
     * The enchantment payload through the **live registries**. The salvaged
     * payload named {@code #minecraft:enchantable/footwear} — a tag 1.21.1 does
     * not ship (the boots family is {@code #minecraft:enchantable/foot_armor}) —
     * and shipped an empty {@code repair_applicable} tag, so both enchantments
     * resolved to an EMPTY supported set: no enchanting table, no loot, no
     * anvil. Every probe above applies the enchantment directly with
     * {@code ItemStack#enchant}, which bypasses {@code supported_items}, so
     * they stayed green over an enchantment no player could ever obtain. This
     * probe enters through the shipped resolution path.
     */
    @GameTest(template = "empty")
    public static void enchantmentPayloadsResolveAgainstTheLiveRegistries(final GameTestHelper helper) {
        final var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        final var haste = enchantments.getHolderOrThrow(ModEnchantments.ENCHANTMENT_HASTE);
        final var repair = enchantments.getHolderOrThrow(ModEnchantments.ENCHANTMENT_REPAIR);
        final var unbreaking = enchantments.getHolderOrThrow(Enchantments.UNBREAKING);
        final var mending = enchantments.getHolderOrThrow(Enchantments.MENDING);

        helper.assertTrue(haste.value().isSupportedItem(new ItemStack(Items.DIAMOND_BOOTS)),
                "ench_haste must support boots — the original's armorType 3 gate is boots"
                        + " (fields_1.7.10_mcp_stable_11.csv:4043)");
        helper.assertTrue(haste.value().isSupportedItem(new ItemStack(Items.LEATHER_BOOTS)),
                "the boots family is the tag, not one material");
        helper.assertTrue(!haste.value().isSupportedItem(new ItemStack(Items.DIAMOND_SWORD)),
                "Haste must not support a weapon (the payload's slots are feet)");
        helper.assertTrue(repair.value().isSupportedItem(new ItemStack(Items.DIAMOND_PICKAXE)),
                "the original's Type.all + IRepairable domain covers durable tools");
        helper.assertTrue(repair.value().isSupportedItem(new ItemStack(Items.DIAMOND_BOOTS)),
                "worn gear is in the original's IRepairable domain");
        helper.assertTrue(repair.value().isSupportedItem(new ItemStack(TestProbeContent.PROBE_REPAIRABLE.get())),
                "the probe item must be repairable through the tag, not only by hand");

        // The original's exclusion: EnchantmentRepair.func_77326_a excludes
        // Enchantment.field_77347_r (= unbreaking, fields csv:3951) and nothing
        // else. 1.21.1 measures it as
        // "a.exclusiveSet.contains(b) || b.exclusiveSet.contains(a)"
        // (Enchantment.areCompatible, Enchantment.java:172), and vanilla
        // unbreaking/mending carry no exclusive_set of their own, so naming
        // minecraft:unbreaking in our set is the only way to express it.
        helper.assertTrue(!Enchantment.areCompatible(repair, unbreaking),
                "ench_repair must exclude Unbreaking, as the original's canCombine does");
        helper.assertTrue(Enchantment.areCompatible(repair, mending),
                "the original excludes only Unbreaking — Mending stays compatible");
        helper.assertTrue(Enchantment.areCompatible(haste, unbreaking),
                "the original's Haste excludes nothing");

        // The card's highest-value hunk, measured on the live manager: the two
        // extracted infusion_enchantment rows (recipe.201.infusion_enchantment =
        // Repair, recipe.202 = Haste) name these enchantments.
        // InfusionEnchantmentRecipe decodes the field as a ResourceKey and
        // resolves it against the registry on use, so registering the
        // enchantments is what turns those two shipped rows live.
        int live = 0;
        for (final var row : helper.getLevel().getRecipeManager()
                .getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            if (row.value().enchantmentKey().location().getNamespace()
                    .equals(ThaumcraftClassic.MOD_ID)
                    && row.value().enchantment(helper.getLevel().registryAccess()) != null) {
                live++;
            }
        }
        helper.assertTrue(live == 2,
                "recipe.201/202 must resolve their enchantment now that ench_repair and "
                        + "ench_haste are registered, found " + live);
        helper.succeed();
    }

    /**
     * The taint damage type: the original's
     * {@code DamageSourceThaumcraft.taint.setDamageBypassesArmor().setMagicDamage()}.
     * The salvaged data file rode a 1.7.10-shaped payload
     * ({@code "effects": "magic"}) and 1.21.1's {@code DamageType} codec has no
     * such element (its fields are message_id, scaling, exhaustion, effects,
     * death_message_type — DamageEffects.java), so the registry load died with
     * "Unknown element name:magic" and <b>every game test in the batch</b> went
     * red with it. The payload now uses the codec's own shape, and the
     * original's two flags ride the two minecraft-namespace damage_type tags.
     */
    @GameTest(template = "empty")
    public static void taintDamageTypeCarriesTheOriginalsFlags(final GameTestHelper helper) {
        // Read the tags off the SERVER's damage_type registry. The obvious
        // source.is(DamageTypeTags.X) is a trap here: DamageType#is resolves
        // through builtInRegistryHolder(), and damage_type is a *datapack*
        // registry in 1.21.1, not a built-in one — calling it from the first
        // probe of the batch bootstraps BuiltInRegistries after NeoForge froze
        // it, and the whole batch dies with "Registry is already frozen".
        final var damageTypes = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        final var taint = damageTypes.getHolderOrThrow(DamageSourcesThaumcraft.TAINT);
        helper.assertTrue(carries(damageTypes, DamageTypeTags.BYPASSES_ARMOR, taint),
                "the original's taint bypasses armour (setDamageBypassesArmor) — the flag rides "
                        + "data/minecraft/tags/damage_type/bypasses_armor.json");
        helper.assertTrue(!carries(damageTypes, DamageTypeTags.BYPASSES_EFFECTS, taint)
                        && !carries(damageTypes, DamageTypeTags.BYPASSES_INVULNERABILITY, taint)
                        && !carries(damageTypes, DamageTypeTags.BYPASSES_RESISTANCE, taint),
                "the original's taint is not absolute damage: Resistance, difficulty and "
                        + "invulnerability apply as for any other hit");
        helper.assertTrue(DamageSourcesThaumcraft.taint(helper.getLevel()).getDirectEntity() == null,
                "the source is world damage with no direct attacker, as the original's static "
                        + "DamageSourceThaumcraft.taint");
        helper.succeed();
    }

    /** Is {@code holder} in the named tag of {@code registry}? Null tag = empty tag. */
    private static boolean carries(
            final net.minecraft.core.Registry<net.minecraft.world.damagesource.DamageType> registry,
            final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> tag,
            final net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType> holder) {
        final var named = registry.getTag(tag);
        return named.isPresent() && named.get().contains(holder);
    }

    // ------------------------------------------------------------------ effects

    @GameTest(template = "empty")
    public static void effectTickGatesMatchOriginal(final GameTestHelper helper) {
        // Flux taint: every 40 >> amplifier ticks, every tick at amp >= 6.
        helper.assertTrue(PotionFluxTaint.appliesThisTick(0, 0)
                        && PotionFluxTaint.appliesThisTick(40, 0)
                        && !PotionFluxTaint.appliesThisTick(1, 0)
                        && PotionFluxTaint.appliesThisTick(20, 1)
                        && !PotionFluxTaint.appliesThisTick(19, 1)
                        && !PotionFluxTaint.appliesThisTick(7, 3)
                        && PotionFluxTaint.appliesThisTick(5, 3)
                        && PotionFluxTaint.appliesThisTick(3, 5)
                        && PotionFluxTaint.appliesThisTick(3, 6),
                "flux taint gate must be 40 >> amplifier");
        helper.assertTrue(PotionInfectiousVisExhaust.appliesThisTick(40, 0)
                        && !PotionInfectiousVisExhaust.appliesThisTick(39, 0),
                "infectious vis exhaustion gate must be % 40");
        helper.assertTrue(PotionSunScorned.appliesThisTick(40, 0)
                        && !PotionSunScorned.appliesThisTick(39, 0),
                "sun scorned gate must be % 40");
        helper.assertTrue(PotionThaumarhia.appliesThisTick(20, 0)
                        && !PotionThaumarhia.appliesThisTick(19, 0),
                "thaumarhia gate must be % 20");
        helper.assertTrue(PotionUnnaturalHunger.appliesThisTick(1, 0),
                "unnatural hunger applies every tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluxTaintDamagesNonTaintMobs(final GameTestHelper helper) {
        final Cow cow = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(1, 1, 1)));
        final float before = cow.getHealth();
        new PotionFluxTaint().applyEffectTick(cow, 0);
        helper.assertTrue(cow.getHealth() < before,
                "a non-taint mob out of water must take 1 taint damage");
        helper.assertTrue(cow.getLastDamageSource() != null
                        && cow.getLastDamageSource().is(DamageSourcesThaumcraft.TAINT),
                "the damage must come from the thaumcraft:taint damage type");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void infectiousVisExhaustSpreads(final GameTestHelper helper) {
        final Cow source = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(1, 1, 1)));
        final Cow nearby = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(5, 1, 1)));

        ThaumcraftClassic.LOGGER.info("[TC-M6-05] spread probe: source {} alive={} listed={}; nearby {} "
                        + "alive={} listed={}; queryHits={}",
                source.position(), source.isAlive(), listed(helper, source),
                nearby.position(), nearby.isAlive(), listed(helper, nearby),
                helper.getLevel().getEntitiesOfClass(LivingEntity.class,
                        source.getBoundingBox().inflate(8.0)).size());
        // amp 1: spread infectious at amp 0 to nearby entities.
        new PotionInfectiousVisExhaust().applyEffectTick(source, 1);
        ThaumcraftClassic.LOGGER.info("[TC-M6-05] spread probe: after amp1 hasEffect={} nearEffects={} "
                        + "srcEffects={}",
                nearby.hasEffect(ModMobEffects.POTION_INFVISEXHAUST), nearby.getActiveEffects(),
                source.getActiveEffects());
        helper.assertTrue(nearby.hasEffect(ModMobEffects.POTION_INFVISEXHAUST),
                "amp>0 must spread infectious exhaustion");
        helper.assertTrue(nearby.getEffect(ModMobEffects.POTION_INFVISEXHAUST).getAmplifier() == 0,
                "the spread amplifier must be amplifier - 1");

        // amp 0: convert to plain vis exhaustion on nearby entities.
        new PotionInfectiousVisExhaust().applyEffectTick(nearby, 0);
        helper.assertTrue(source.hasEffect(ModMobEffects.POTION_VISEXHAUST),
                "amp 0 must convert nearby entities to plain vis exhaustion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unnaturalHungerAddsExhaustion(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final float before = player.getFoodData().getExhaustionLevel();
        new PotionUnnaturalHunger().applyEffectTick(player, 1);
        helper.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - before - 0.05F) < 1.0e-5,
                "exhaustion must rise by 0.025 * (amplifier + 1)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soapCleansesWarp(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_WARP,
                PlayerWarpData.empty().withWarpTemp(3).withWarpSticky(5));

        // 30 rolls at chance >= 0.33: temp removal is unconditional, and the
        // probability that no sticky roll ever succeeds is < 1e-6.
        for (int i = 0; i < 30; i++) {
            ItemSanitySoap.applyWarpCleansing(player, helper.getLevel().random);
        }

        final PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(warp.warpTemp() == 0, "all temporary warp must be removed");
        helper.assertTrue(warp.warpSticky() < 5, "sticky warp must be removed by successful rolls");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bathSaltsDespawnQuickly(final GameTestHelper helper) {
        // The registered instance: constructing the Item by hand off the
        // registration path walks BuiltInRegistries after NeoForge froze it
        // ("Registry is already frozen").
        final int lifespan = CatalogWarpEffects.ITEM_BATH_SALTS.get()
                .getEntityLifespan(new ItemStack(Items.SUGAR), helper.getLevel());
        helper.assertTrue(lifespan == 200,
                "dropped bath salts must expire after 200 ticks, got " + lifespan);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allNineEffectsRegistered(final GameTestHelper helper) {
        final var registry = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.MOB_EFFECT);
        for (final String path : new String[]{"potion.fluxtaint", "potion.visexhaust",
                "potion.infvisexhaust", "potion.unhunger", "potion.warpward",
                "potion.deathgaze", "potion.blurred", "potion.sunscorned",
                "potion.thaumarhia"}) {
            helper.assertTrue(
                    registry.containsKey(
                            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path)),
                    "effect " + path + " must be registered");
        }
        // The flag-only effects must resolve through their holders too.
        helper.assertTrue(PotionWarpWard.class.isInstance(ModMobEffects.POTION_WARWARD.value())
                        && ModMobEffects.POTION_DEATHGAZE.value() instanceof PotionDeathGaze
                        && ModMobEffects.POTION_BLURRED.value() instanceof PotionBlurredVision
                        && ModMobEffects.POTION_VISEXHAUST.value() instanceof PotionVisExhaust,
                "effect holders must resolve to the ported classes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void warpItemsAndEnchantmentsRegistered(final GameTestHelper helper) {
        for (final String path : new String[]{"itembathsalts", "itemsanitysoap",
                "itemsanitychecker"}) {
            helper.assertTrue(
                    BuiltInRegistries.ITEM.containsKey(
                            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path)),
                    "item " + path + " must be registered");
        }
        // Enchantments are a datapack registry: the shipped JSON must be in it.
        final var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        helper.assertTrue(enchantments.containsKey(ModEnchantments.ENCHANTMENT_HASTE.location())
                        && enchantments.containsKey(ModEnchantments.ENCHANTMENT_REPAIR.location()),
                "the two enchantment JSON payloads must load into the registry");
        helper.succeed();
    }

    // --------------------------------------------------------------------- warp

    /**
     * The roll's cadence ({@code ticksExisted > 0 && % 2000 == 0}) and the warp
     * ward gate, plus the tail decay ({@code addWarpTemp(-1)},
     * {@code WarpEvents.java:224}) that runs on every event.
     */
    @GameTest(template = "empty")
    public static void warpEventCadenceWardGateAndDecay(final GameTestHelper helper) {
        helper.assertTrue(!WarpEvents.isWarpEventTick(0)
                        && WarpEvents.isWarpEventTick(WarpLogic.WARP_EVENT_INTERVAL_TICKS)
                        && WarpEvents.isWarpEventTick(2 * WarpLogic.WARP_EVENT_INTERVAL_TICKS)
                        && !WarpEvents.isWarpEventTick(1999)
                        && !WarpEvents.isWarpEventTick(100),
                "the warp roll must fire at ticks 2000/4000 and never at 0/1999/100");

        final ServerPlayer player = freshPlayer(helper);
        // warpCounter 0 keeps fires() false, so this probe measures the gates and
        // the decay without a rolled branch.
        player.setData(ModAttachments.PLAYER_WARP, PlayerWarpData.empty().withWarpTemp(7));
        player.tickCount = WarpLogic.WARP_EVENT_INTERVAL_TICKS;

        player.addEffect(new MobEffectInstance(ModMobEffects.POTION_WARWARD, 200));
        WarpEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getData(ModAttachments.PLAYER_WARP).warpTemp() == 7,
                "warp ward must suppress the whole warp event (EventHandlerEntity.java:236)");

        player.removeEffect(ModMobEffects.POTION_WARWARD);
        WarpEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getData(ModAttachments.PLAYER_WARP).warpTemp() == 6,
                "without the ward the event runs and temp warp decays by exactly 1");

        // The cadence gate: the same player, one tick off, must not decay.
        player.tickCount = WarpLogic.WARP_EVENT_INTERVAL_TICKS + 1;
        WarpEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getData(ModAttachments.PLAYER_WARP).warpTemp() == 6,
                "the event must not run between its 2000-tick boundaries");
        helper.succeed();
    }

    /**
     * A seeded world random drives one real warp event; the branch the original
     * table prescribes for that seed must be what the driver applies, with the
     * counter arithmetic and the tail decay of the original.
     */
    @GameTest(template = "empty")
    public static void warpEventAppliesThePinnedTableBranch(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final PlayerWarpData start = PlayerWarpData.empty()
                .withWarpPerm(60).withWarpSticky(40).withWarpTemp(5).withWarpCounter(100);
        player.setData(ModAttachments.PLAYER_WARP, start);

        final RandomSource levelRandom = helper.getLevel().getRandom();
        // Warp total 100, counter 100: fires needs the first draw <= 10, and the
        // second draw picks the branch. Search for the first seed whose branch is
        // the flux (vis exhaustion) one, then replay that seed.
        final int warp = start.total() + WarpEvents.gearWarp(player);
        final int rolled = WarpLogic.rollWarp(warp, 100);
        long chosen = -1L;
        for (long seed = 0; seed < 100_000L; seed++) {
            levelRandom.setSeed(seed);
            if (WarpLogic.fires(warp, 100, levelRandom.nextInt(100))) {
                final WarpLogic.WarpOutcome outcome =
                        WarpLogic.outcome(WarpLogic.rollEff(levelRandom, rolled), rolled);
                if (outcome.kind() == WarpLogic.Kind.VIS_EXHAUST) {
                    chosen = seed;
                    break;
                }
            }
        }
        helper.assertTrue(chosen >= 0, "a seed must produce the flux branch within 100k draws");

        levelRandom.setSeed(chosen);
        WarpEvents.checkWarpEvent(player);

        final MobEffectInstance flux = player.getEffect(ModMobEffects.POTION_VISEXHAUST);
        helper.assertTrue(flux != null, "the rolled branch must apply thaumcraft:potion.visexhaust");
        helper.assertTrue(flux.getDuration() == 5000,
                "the flux duration must be 5000 ticks (WarpEvents.java:73), got " + flux.getDuration());
        helper.assertTrue(flux.getAmplifier() == WarpLogic.effectAmplifier(rolled),
                "the amplifier must be min(3, warp / 15) = "
                        + WarpLogic.effectAmplifier(rolled) + ", got " + flux.getAmplifier());

        final PlayerWarpData after = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(after.warpCounter() == WarpLogic.rollCounter(100),
                "the eldritch counter must drop by max(5, 2*sqrt(counter)) = "
                        + WarpLogic.rollCounter(100) + ", got " + after.warpCounter());
        helper.assertTrue(after.warpTemp() == 4,
                "the tail decay is exactly -1 temp warp, got " + after.warpTemp());
        helper.assertTrue(after.warpPerm() == 60 && after.warpSticky() == 40,
                "a flux branch must not touch the perm or sticky counters");
        helper.succeed();
    }

    /**
     * The death gaze: mobs inside the 0.75-rad cone in front of the player are
     * targeted and withered for 80 ticks; a mob behind the player is untouched
     * ({@code WarpEvents.java:324-347}).
     */
    @GameTest(template = "empty")
    public static void deathGazeWithersMobsInCone(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.addEffect(new MobEffectInstance(ModMobEffects.POTION_DEATHGAZE, 200, 0));

        // yaw 0 looks down +Z: ahead is inside the cone, behind is outside it.
        final Cow ahead = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(0, 1, 4)));
        final Cow behind = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(0, 1, -4)));
        helper.assertTrue(player.getLookAngle().z > 0.9D,
                "the probe player must look along +Z for the cone assertion to mean anything");

        final int gazeRange = WarpLogic.deathGazeRange(
                player.getEffect(ModMobEffects.POTION_DEATHGAZE).getAmplifier());
        ThaumcraftClassic.LOGGER.info("[TC-M6-05] gaze probe: range={} boxHits={} ahead {} alive={} "
                        + "listed={} los={} behind {} alive={} listed={}",
                gazeRange,
                helper.getLevel().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(gazeRange)).size(),
                ahead.position(), ahead.isAlive(), listed(helper, ahead), player.hasLineOfSight(ahead),
                behind.position(), behind.isAlive(), listed(helper, behind));
        WarpEvents.checkDeathGaze(player);
        ThaumcraftClassic.LOGGER.info("[TC-M6-05] gaze probe: aheadEffects={} behindEffects={}",
                ahead.getActiveEffects(), behind.getActiveEffects());

        helper.assertTrue(ahead.hasEffect(MobEffects.WITHER),
                "a mob inside the gaze cone must be withered");
        final MobEffectInstance wither = ahead.getEffect(MobEffects.WITHER);
        helper.assertTrue(wither.getDuration() == WarpLogic.DEATH_GAZE_WITHER_TICKS,
                "the wither must last 80 ticks (WarpEvents.java:344), got " + wither.getDuration());
        helper.assertTrue(ahead.getTarget() == player,
                "the gaze must set the mob's target to the player");
        helper.assertTrue(!behind.hasEffect(MobEffects.WITHER),
                "a mob behind the player must be outside the 0.75 rad cone");
        helper.succeed();
    }

    /**
     * The gaze's range follows the effect amplifier:
     * {@code min(8 + level*3, 24)} with the original's 1-based level, so a
     * level-1 (amplifier 0) gaze reaches 11 blocks and a level-3 gaze reaches
     * 14 — pinned here against a mob at 12 blocks.
     */
    @GameTest(template = "empty")
    public static void deathGazeRangeScalesWithAmplifier(final GameTestHelper helper) {
        helper.assertTrue(WarpLogic.deathGazeRange(0) == 11
                        && WarpLogic.deathGazeRange(1) == 14
                        && WarpLogic.deathGazeRange(5) == 24,
                "range must be min(8 + (amplifier+1)*3, 24)");

        final ServerPlayer player = freshPlayer(helper);
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.addEffect(new MobEffectInstance(ModMobEffects.POTION_DEATHGAZE, 200, 0));
        final Cow far = helper.spawn(EntityType.COW, helper.absolutePos(new BlockPos(0, 1, 12)));

        WarpEvents.checkDeathGaze(player);
        helper.assertTrue(!far.hasEffect(MobEffects.WITHER),
                "a level-1 gaze reaches 11 blocks, so a cow at 12 must be untouched");

        player.removeEffect(ModMobEffects.POTION_DEATHGAZE);
        player.addEffect(new MobEffectInstance(ModMobEffects.POTION_DEATHGAZE, 200, 1));
        WarpEvents.checkDeathGaze(player);
        helper.assertTrue(far.hasEffect(MobEffects.WITHER),
                "a level-2 gaze reaches 14 blocks, so the cow at 12 must be withered");
        helper.succeed();
    }

    /**
     * The original's {@code grantResearch}: {@code 1 + rand(times)} points, each
     * a random primal in the player's aspect pool ({@code WarpEvents.java:236-247}).
     */
    @GameTest(template = "empty")
    public static void grantResearchAddsPrimalAspectPool(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final int before = primalPoolTotal(player);

        WarpEvents.grantResearch(player, 1);
        helper.assertTrue(primalPoolTotal(player) - before == 1,
                "grantResearch(times = 1) adds exactly one primal pool point");

        WarpEvents.grantResearch(player, 5);
        final int gained = primalPoolTotal(player) - before - 1;
        helper.assertTrue(gained >= 1 && gained <= 5,
                "grantResearch(times = 5) adds 1..5 points, got " + gained);
        helper.succeed();
    }

    /**
     * The eldritch-research seam: the 10/25/50 grants name research keys the
     * shipped 201-entry graph does not contain, so they are inert until the
     * eldritch research card lands — and the bath salts the hint points at is
     * registered by this card.
     */
    @GameTest(template = "empty")
    public static void eldritchResearchSeamIsInert(final GameTestHelper helper) {
        // The 201-entry graph ships the bath-salts hint entry
        // (content-mapping row Thaumcraft:BATHSALTS), so the warp>10 grant that
        // names @BATHSALTS is live. The eldritch pair is not shipped (TC-M6-08),
        // so those two grants — and the 10/20 point pools they carry — stay inert.
        helper.assertTrue(WarpEvents.entryExists("@BATHSALTS"),
                "the bath-salts hint entry ships, so its warp grant is live");
        helper.assertTrue(!WarpEvents.entryExists("ELDRITCHMINOR")
                        && !WarpEvents.entryExists("ELDRITCHMAJOR"),
                "the eldritch warp grants stay graph-gated until TC-M6-08 ships those entries");
        helper.assertTrue(ResearchManager.graph().entries().size() == 201,
                "the shipped research graph must keep its 201 entries");
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_WARP,
                PlayerWarpData.empty().withWarpPerm(60).withWarpSticky(0).withWarpCounter(0));
        final int before = primalPoolTotal(player);
        WarpEvents.checkWarpEvent(player);
        helper.assertTrue(primalPoolTotal(player) == before,
                "actualwarp 60 must grant nothing while the eldritch entries are missing");
        helper.succeed();
    }

    /**
     * The gear-warp seam: the roll adds {@code IWarpingGear.getWarp} of worn
     * gear (the original's {@code getWarpFromGear}); no shipped item implements
     * it yet, so a player in plain armour contributes 0 — and the moment runic
     * armour lands, this probe is the row that says so.
     */
    @GameTest(template = "empty")
    public static void gearWarpIsInertUntilRunicArmourLands(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        helper.assertTrue(WarpEvents.gearWarp(player) == 0,
                "no shipped armour implements IWarpingGear, so gear warp must read 0");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static int primalPoolTotal(final ServerPlayer player) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        int total = 0;
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            total += knowledge.aspectAmount(primal.getTag());
        }
        return total;
    }

    /**
     * Ad-hoc ServerPlayer with an embedded connection, same pattern as
     * ResearchProgressionGameTest (warp payloads must be ad-hoc enabled so
     * applyWarp's sync does not throw).
     */
    /** Is this entity reachable through the level's entity list? */
    private static boolean listed(final GameTestHelper helper, final Entity entity) {
        return helper.getLevel()
                .getEntitiesOfClass(entity.getClass(), entity.getBoundingBox().inflate(0.5))
                .contains(entity);
    }

    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "tc-m605-"
                        + UUID.randomUUID().toString().substring(0, 8)), false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(net.neoforged.neoforge.network.payload.SyncAttachmentsPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
