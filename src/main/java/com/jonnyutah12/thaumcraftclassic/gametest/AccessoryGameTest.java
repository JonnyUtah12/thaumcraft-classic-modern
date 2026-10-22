package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryAttachments;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryData;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryProvider;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.accessory.FallbackProvider;
import com.jonnyutah12.thaumcraftclassic.accessory.WornAccessory;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * TC-M8-01 acceptance: the accessory provider contract under BOTH runtime
 * configurations. CI runs this suite twice — once with Curios on the
 * runtime classpath (default) and once with {@code -Ptc.noCurios}
 * (game-test-no-curios job, .github/workflows/build.yml). The suite
 * branches on the active provider: the shared contract (selection,
 * validation, worn queries, persistence, death drops) must hold identically;
 * provider-specific internals (fallback tick/lifecycle, Curios slot
 * registration) are asserted where they belong.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccessoryGameTest {

    private AccessoryGameTest() {
    }

    @GameTest(template = "empty")
    public static void providerSelectionMatchesEnvironment(final GameTestHelper helper) {
        helper.assertTrue(AccessoryService.isInitialized(), "accessory layer not initialized");
        final boolean curiosLoaded = net.neoforged.fml.ModList.get().isLoaded("curios");
        final AccessoryProvider provider = AccessoryService.provider();
        if (curiosLoaded) {
            helper.assertTrue("curios".equals(provider.id()),
                    "Curios is loaded but provider is: " + provider.id());
        } else {
            helper.assertTrue("fallback".equals(provider.id()),
                    "Curios is absent but provider is: " + provider.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipWornQueryUnequipContract(final GameTestHelper helper) {
        TestProbeContent.ProbeAccessory.resetCounters();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final AccessoryProvider provider = AccessoryService.provider();

        final ItemStack ring1 = new ItemStack(TestProbeContent.PROBE_RING.get());
        final ItemStack ring2 = new ItemStack(TestProbeContent.PROBE_RING.get());
        final ItemStack amulet = new ItemStack(TestProbeContent.PROBE_AMULET.get());

        helper.assertTrue(provider.equip(player, AccessorySlot.RING, 0, ring1), "ring 1 equip failed");
        helper.assertTrue(provider.equip(player, AccessorySlot.RING, 1, ring2), "ring 2 equip failed");
        helper.assertTrue(provider.equip(player, AccessorySlot.AMULET, 0, amulet), "amulet equip failed");

        // Capacity: a third ring has no valid slot.
        helper.assertTrue(!provider.canEquip(player, AccessorySlot.RING, 2, new ItemStack(TestProbeContent.PROBE_RING.get())),
                "third ring accepted (RING capacity is 2)");
        // Slot routing: a ring in the amulet slot is rejected.
        helper.assertTrue(!provider.canEquip(player, AccessorySlot.AMULET, 0, new ItemStack(TestProbeContent.PROBE_RING.get())),
                "ring accepted in AMULET slot");
        // Item-side veto: the veto ring is rejected in its own slot type.
        helper.assertTrue(!provider.equip(player, AccessorySlot.RING, 0, new ItemStack(TestProbeContent.PROBE_VETO_RING.get())),
                "canEquip veto not respected");

        final var worn = provider.worn(player);
        helper.assertTrue(worn.size() == 3, "worn count wrong: " + worn.size());
        helper.assertTrue(provider.worn(player).stream()
                        .filter(w -> w.slot() == AccessorySlot.RING).count() == 2,
                "both rings not reported worn");
        helper.assertTrue(AccessoryService.isWorn(player, TestProbeContent.PROBE_AMULET.get()),
                "amulet not reported worn");

        final ItemStack removed = provider.unequip(player, AccessorySlot.RING, 0);
        helper.assertTrue(removed.is(TestProbeContent.PROBE_RING.get()), "unequip did not return the ring");
        helper.assertTrue(provider.worn(player).size() == 2, "worn count after unequip wrong");
        helper.assertTrue(!AccessoryService.worn(player).stream().anyMatch(w -> w.slot() == AccessorySlot.RING && w.index() == 0),
                "unequipped slot still reported worn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallbackLifecycleAndWornTickExactlyOnce(final GameTestHelper helper) {
        if (!(AccessoryService.provider() instanceof FallbackProvider fallback)) {
            helper.succeed(); // Curios drives these natively; asserted via the adapter unit test.
            return;
        }
        TestProbeContent.ProbeAccessory.resetCounters();
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.assertTrue(fallback.equip(player, AccessorySlot.RING, 0, new ItemStack(TestProbeContent.PROBE_RING.get())),
                "probe ring equip failed");
        helper.assertTrue(TestProbeContent.ProbeAccessory.EQUIPPED.get() == 1,
                "onEquipped fired " + TestProbeContent.ProbeAccessory.EQUIPPED.get() + " time(s), expected exactly 1");

        fallback.tickWorn(player);
        fallback.tickWorn(player);
        helper.assertTrue(TestProbeContent.ProbeAccessory.WORN_TICKS.get() == 2,
                "onWornTick fired " + TestProbeContent.ProbeAccessory.WORN_TICKS.get() + " times for 2 ticks, expected exactly 2");

        fallback.unequip(player, AccessorySlot.RING, 0);
        helper.assertTrue(TestProbeContent.ProbeAccessory.UNEQUIPPED.get() == 1,
                "onUnequipped fired " + TestProbeContent.ProbeAccessory.UNEQUIPPED.get() + " time(s), expected exactly 1");
        fallback.tickWorn(player);
        helper.assertTrue(TestProbeContent.ProbeAccessory.WORN_TICKS.get() == 2,
                "onWornTick fired after unequip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curiosSlotTypesRegisteredForPlayers(final GameTestHelper helper) {
        if (!"curios".equals(AccessoryService.provider().id())) {
            helper.succeed(); // no-Curios run: nothing to assert
            return;
        }
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // The probe lives in CuriosBridge so this class stays free of Curios
        // types (CuriosBridgeIsolationTest guards exactly that).
        for (final AccessorySlot slot : AccessorySlot.values()) {
            final int size = com.jonnyutah12.thaumcraftclassic.accessory.curios.CuriosBridge
                    .registeredSlotSize(player, slot);
            helper.assertTrue(size == slot.capacity(),
                    "Curios slot " + slot.curiosSlotId() + " size " + size + " (expected " + slot.capacity() + ")");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void accessoriesSurviveSaveAndLoad(final GameTestHelper helper) {
        final Player original = helper.makeMockPlayer(GameType.SURVIVAL);
        final AccessoryData data = AccessoryData.empty()
                .with(AccessorySlot.RING.fallbackIndex(0), new ItemStack(TestProbeContent.PROBE_RING.get()))
                .with(AccessorySlot.AMULET.fallbackIndex(0), new ItemStack(TestProbeContent.PROBE_AMULET.get()));
        original.setData(AccessoryAttachments.ACCESSORIES, data);

        // The disconnect→reconnect path: player NBT carries the attachment.
        final CompoundTag saved = original.saveWithoutId(new CompoundTag());
        final Player reconnected = helper.makeMockPlayer(GameType.SURVIVAL);
        reconnected.load(saved);

        final AccessoryData loaded = reconnected.getData(AccessoryAttachments.ACCESSORIES);
        helper.assertTrue(loaded.get(AccessorySlot.RING.fallbackIndex(0)).is(TestProbeContent.PROBE_RING.get()),
                "worn ring lost across save/load");
        helper.assertTrue(loaded.get(AccessorySlot.AMULET.fallbackIndex(0)).is(TestProbeContent.PROBE_AMULET.get()),
                "worn amulet lost across save/load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void keepInventoryDeathKeepsAccessories(final GameTestHelper helper) {
        if (!"fallback".equals(AccessoryService.provider().id())) {
            helper.succeed(); // Curios owns death handling natively (ATM10 battery covers it)
            return;
        }
        final var level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());

        final ServerPlayer original = connectedMockPlayer(helper, "tc-acc-keep-original");
        original.setData(AccessoryAttachments.ACCESSORIES, AccessoryData.empty()
                .with(AccessorySlot.RING.fallbackIndex(0), new ItemStack(TestProbeContent.PROBE_RING.get())));

        final ServerPlayer respawned = connectedMockPlayer(helper, "tc-acc-keep-respawn");
        respawned.restoreFrom(original, false);

        helper.assertTrue(respawned.getData(AccessoryAttachments.ACCESSORIES)
                        .get(AccessorySlot.RING.fallbackIndex(0)).is(TestProbeContent.PROBE_RING.get()),
                "accessories not carried across death with keepInventory=true");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathDropsAccessoriesWithoutKeepInventory(final GameTestHelper helper) {
        final var level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());

        final ServerPlayer player = connectedMockPlayer(helper, "tc-acc-drop");
        final AccessoryProvider provider = AccessoryService.provider();
        helper.assertTrue(provider.equip(player, AccessorySlot.RING, 0, new ItemStack(TestProbeContent.PROBE_RING.get())),
                "equipping before death failed");

        // The real death path: die() → dropAllDeathLoot → LivingDropsEvent.
        // Fallback drops come from AccessoryLifecycle; under Curios they are
        // Curios' own drop handling (integration assertion).
        player.die(level.damageSources().genericKill());

        final boolean dropped = level.getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(4)).stream()
                .anyMatch(item -> item.getItem().is(TestProbeContent.PROBE_RING.get()));
        helper.assertTrue(dropped, "worn accessory did not drop on death with keepInventory=false");

        if ("fallback".equals(AccessoryService.provider().id())) {
            helper.assertTrue(player.getData(AccessoryAttachments.ACCESSORIES).isEmpty(),
                    "attachment not cleared after death drop");
            final ServerPlayer respawned = connectedMockPlayer(helper, "tc-acc-drop-respawn");
            respawned.restoreFrom(player, false);
            helper.assertTrue(respawned.getData(AccessoryAttachments.ACCESSORIES).isEmpty(),
                    "clone kept accessories that were already dropped at death");
        }
        helper.succeed();
    }

    /**
     * ServerPlayer with a dummy net handler, same pattern as
     * {@code PersistenceGameTest}: real respawns always have a connection
     * before setData/restoreFrom, and attachment sync sends through it. The
     * Connection's channel is an EmbeddedChannel so NeoForge's send guard
     * (NetworkRegistry.checkPacket → ChannelAttributes) can inspect channel
     * attributes; packets are queued, never written.
     */
    private static ServerPlayer connectedMockPlayer(final GameTestHelper helper, final String name) {
        final var level = helper.getLevel();
        final var server = level.getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        final ServerPlayer player =
                new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        final io.netty.channel.Channel nettyChannel = new io.netty.channel.embedded.EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public io.netty.channel.Channel channel() {
                return nettyChannel;
            }
        };
        net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(
                connection, net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateAdHocChannels(connection)
                .add(net.neoforged.neoforge.network.payload.SyncAttachmentsPayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
