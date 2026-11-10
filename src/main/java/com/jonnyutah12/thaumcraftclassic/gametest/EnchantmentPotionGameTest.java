package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.enchantment.EnchantmentBehaviours;
import com.jonnyutah12.thaumcraftclassic.items.ItemBathSalts;
import com.jonnyutah12.thaumcraftclassic.items.ItemSanitySoap;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
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
import com.jonnyutah12.thaumcraftclassic.registration.ModAttachmentsBridge;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModEnchantments;
import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M6-05 acceptance: the two enchantments' tick behaviours and the nine
 * potion effects' gates/bodies against the real server paths. Design record:
 * docs/architecture/enchantments-potions.md.
 *
 * <p>The enchantment data itself (weights, cost tables, applicability tags,
 * unbreaking exclusion) is pinned by the unit-side JSON parity test; this
 * class pins the code halves: the haste speed modifier, the repair
 * vis-payment + mending flow, the effect tick gates, the flux-taint damage
 * body, the infectious-exhaustion spread, the unnatural-hunger exhaustion
 * body and the soap's warp cleansing.</p>
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
        player.setItemBySlot(EquipmentSlot.FEET, boots);

        EnchantmentBehaviours.playerTick(player);

        final var modifier = player.getAttribute(Attributes.MOVEMENT_SPEED)
                .getModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID);
        helper.assertTrue(modifier != null, "haste boots must add the speed modifier");
        helper.assertTrue(Math.abs(modifier.amount() - 3 * 0.015F) < 1.0e-6,
                "modifier must be level * 0.015 (original updateSpeed), got " + modifier.amount());

        // Removing the boots and ticking again must drop the modifier.
        player.setItemBySlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        EnchantmentBehaviours.playerTick(player);
        helper.assertTrue(
                !player.getAttribute(Attributes.MOVEMENT_SPEED)
                        .hasModifier(EnchantmentBehaviours.HASTE_MODIFIER_ID),
                "the modifier must be removed when no boots carry haste");
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
        EnchantmentBehaviours.doRepair(tool, player, objectTags);

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

    // ------------------------------------------------------------------ effects

    @GameTest(template = "empty")
    public static void effectTickGatesMatchOriginal(final GameTestHelper helper) {
        // Flux taint: every 40 >> amplifier ticks, every tick at amp >= 6.
        helper.assertTrue(PotionFluxTaint.appliesThisTick(0, 0)
                        && PotionFluxTaint.appliesThisTick(40, 0)
                        && !PotionFluxTaint.appliesThisTick(1, 0)
                        && PotionFluxTaint.appliesThisTick(20, 1)
                        && !PotionFluxTaint.appliesThisTick(19, 1)
                        && PotionFluxTaint.appliesThisTick(7, 3)
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
        final Cow cow = helper.spawnEntity(net.minecraft.world.entity.EntityType.COW,
                helper.absolutePos(helper.relativeBlockPos(new net.minecraft.core.BlockPos(1, 1, 1))));
        final float before = cow.getHealth();
        new PotionFluxTaint().applyEffectTick(cow, 0);
        helper.assertTrue(cow.getHealth() < before,
                "a non-taint mob out of water must take 1 taint damage");
        helper.assertTrue(cow.getLastDamageSource() != null
                        && cow.getLastDamageSource().is(
                                com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft.TAINT),
                "the damage must come from the thaumcraft:taint damage type");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void infectiousVisExhaustSpreads(final GameTestHelper helper) {
        final Cow source = helper.spawnEntity(net.minecraft.world.entity.EntityType.COW,
                helper.absolutePos(helper.relativeBlockPos(new net.minecraft.core.BlockPos(1, 1, 1))));
        final Cow nearby = helper.spawnEntity(net.minecraft.world.entity.EntityType.COW,
                helper.absolutePos(helper.relativeBlockPos(new net.minecraft.core.BlockPos(5, 1, 1))));

        // amp 1: spread infectious at amp 0 to nearby entities.
        new PotionInfectiousVisExhaust().applyEffectTick(source, 1);
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
        final float before = player.getFoodData().getExhaustion();
        new PotionUnnaturalHunger().applyEffectTick(player, 1);
        helper.assertTrue(Math.abs(player.getFoodData().getExhaustion() - before - 0.05F) < 1.0e-5,
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
        final int lifespan = new ItemBathSalts()
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
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    ThaumcraftClassic.MOD_ID, path)),
                    "effect " + path + " must be registered");
        }
        // The flag-only effects must resolve through their holders too.
        helper.assertTrue(PotionWarpWard.class.isInstance(
                        ModMobEffects.POTION_WARWARD.value())
                        && ModMobEffects.POTION_DEATHGAZE.value() instanceof PotionDeathGaze
                        && ModMobEffects.POTION_BLURRED.value() instanceof PotionBlurredVision
                        && ModMobEffects.POTION_VISEXHAUST.value() instanceof PotionVisExhaust,
                "effect holders must resolve to the ported classes");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Ad-hoc ServerPlayer with an embedded connection, same pattern as
     * ResearchProgressionGameTest (warp payloads must be ad-hoc enabled so
     * applyWarp's sync does not throw).
     */
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
