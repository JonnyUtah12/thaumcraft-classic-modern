package com.jonnyutah12.thaumcraftclassic.player;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.api.IWarpingGear;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogMindSpider;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The warp-event seam: the mind spider's real spawn source and the eldritch
 * mist FX (TC-M6-06-s-c3-b's spawn half / t_1a83f0db, closing BD-75's warp
 * side). Port of {@code thaumcraft.common.lib.WarpEvents} — the event entry
 * {@code checkWarpEvent} ({@code :32-226}), the spider swarm
 * {@code suddenlySpiders} ({@code :282-322}) and the mist {@code spawnMist}
 * ({@code :229-242}) — driven from the player tick exactly as the original
 * drives it ({@code EventHandlerEntity.java:235-237}).
 *
 * <p>All constants and rules live on {@link WarpEventsLogic} (unit-pinned);
 * this class is the Level-touching wiring. What ships wired: the gate, the
 * warp formula, the counter decay, the {@code PacketMiscEvent(0)} FX
 * ({@code :60} → {@link MiscEventPayload#EVENT_WARP}), the spider arms
 * ({@code :158}/{@code :195}), the mist arm ({@code :106}/{@code :162}/
 * {@code :197} → {@link MiscEventPayload#EVENT_MIST} + the guardian seam),
 * the sticky relief ({@code :169-176}) and the tail temp decay
 * ({@code :224-225}).</p>
 *
 * <p>Documented inert arms (BD-75 records them): the potion arms (no
 * thaumcraft MobEffect is registered — the TC-M4-03 lane), the research-grant
 * arms (the grant lane, {@code :203-221} / {@code grantResearch :244-261}),
 * and the chat-only arms (their lang keys ship with the potions lane). The
 * guardians run through {@link EldritchMobHost} — the guardian is not
 * registered, so each summon is the host's documented no-op, the same pattern
 * as {@code TileEldritchCrabSpawner}. The fortress helm discount ({@code :51-58})
 * needs ItemFortressArmor (TC-M6-07) — no shipped stack is fortress armour,
 * so {@link #fortressMaskActive} is statically false; the discount rule itself
 * is wired and unit-pinned. The Warp Ward gate ({@code EventHandlerEntity.java:235})
 * is likewise inert: the effect is unregistered.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class WarpEvents {

    /**
     * {@code EventHandlerEntity.java:235-237} — server side, every 2000th
     * tick, off while {@code Config.wuss} is on and while a Warp Ward is
     * active. {@code player.field_70173_aa} is the {@code tickCount}.
     */
    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        final Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (TcConfig.getBool(TcConfigCatalog.WUSS_MODE)) {
            return;
        }
        if (!WarpEventsLogic.tickFires(serverPlayer.tickCount)) {
            return;
        }
        if (warpWardActive(serverPlayer)) {
            return;
        }
        checkWarpEvent(serverPlayer, serverPlayer.level().getRandom()); // worldObj.rand = field_73012_v
    }

    /**
     * The original's public entry ({@code WarpEvents.java:32}); the random is a
     * parameter so the game-test probes can script the draw order ({@code :45},
     * {@code :50} and the per-attempt scan draws) — the production call site
     * passes the level's random, exactly {@code player.field_70170_p.field_73012_v}.
     */
    public static void checkWarpEvent(final ServerPlayer player, final RandomSource random) {
        PlayerWarpData data = player.getData(ModAttachments.PLAYER_WARP);
        // :34-36 — warp = total (perm+temp+sticky) + gear warp. `actualwarp`
        // (perm+sticky, :35-36) only feeds the research-grant block (:203-221),
        // which is the documented inert arm.
        int warp = data.total() + getWarpFromGear(player);
        int warpCounter = data.warpCounter();                                      // :37

        final int roll = random.nextInt(WarpEventsLogic.ROLL_BOUND);              // :45
        if (WarpEventsLogic.gatePasses(warp, warpCounter, roll)) {                // :46
            warp = WarpEventsLogic.eventWarp(warp, warpCounter);                  // :47
            data = data.withWarpCounter(WarpEventsLogic.decayedCounter(warpCounter)); // :48
            player.setData(ModAttachments.PLAYER_WARP, data);                     // :49
            int eff = random.nextInt(warp);                                       // :50
            if (fortressMaskActive(player.getItemBySlot(EquipmentSlot.HEAD))) {   // :51-56
                eff -= WarpEventsLogic.fortressHelmDiscount(random.nextInt(4));   // :57-58
            }
            // :60 — the warp-event screen FX, to the client hooks lane
            PacketDistributor.sendToPlayer(player, new MiscEventPayload(MiscEventPayload.EVENT_WARP));
            final WarpEventsLogic.Arm arm = WarpEventsLogic.armFor(eff);          // :61-199
            switch (arm) {
                case SPIDERS_ILLUSION -> suddenlySpiders(player, warp, false, random); // :158
                case SPIDERS_REAL -> suddenlySpiders(player, warp, true, random);      // :195
                case MIST_ONE, MIST_W30, MIST_W15 ->
                        spawnMist(player, WarpEventsLogic.guardianCount(arm, warp));  // :106/:162/:197
                case STICKY_RELIEF -> {                                              // :169-176
                    if (data.warpSticky() > 0) {                                      // :170
                        final PlayerWarpData relieved =
                                data.addWarpSticky(WarpEventsLogic.STICKY_RELIEF_DELTA); // :172
                        player.setData(ModAttachments.PLAYER_WARP, relieved);
                        PacketDistributor.sendToPlayer(player, new SyncWarpPayload(
                                relieved.warpSticky(), SyncWarpPayload.WARP_STICKY));  // :173
                        PacketDistributor.sendToPlayer(player, new WarpMessagePayload(
                                WarpEventsLogic.STICKY_RELIEF_DELTA,
                                SyncWarpPayload.WARP_STICKY));                        // :174
                    }
                    chat(player, WarpEventsLogic.KEY_RELIEF);                         // :175
                }
                default -> {
                    // Inert arms (potion / research-grant / chat-only) — see
                    // the class javadoc; WarpEventsLogic.ships() is the list.
                }
            }
        }

        // :224-225 — the tail runs on every check, gate passed or not.
        final PlayerWarpData tail =
                player.getData(ModAttachments.PLAYER_WARP).addWarpTemp(WarpEventsLogic.TAIL_TEMP_DECAY);
        player.setData(ModAttachments.PLAYER_WARP, tail);
        PacketDistributor.sendToPlayer(player,
                new SyncWarpPayload(tail.warpTemp(), SyncWarpPayload.WARP_TEMPORARY));
    }

    /**
     * {@code WarpEvents.suddenlySpiders} ({@code :282-322}):
     * {@code min(50, warp)} spiders, each placed by a 50-attempt offset scan
     * around the player, each targeted at the player (attack target +
     * revenge target, {@code :314-315}); illusion spiders ({@code real ==
     * false}) carry the viewer and {@code harmless = true} ({@code :316-319}),
     * the chat line at {@code :321} closes the swarm.
     */
    private static void suddenlySpiders(final ServerPlayer player, final int warp,
                                        final boolean real, final RandomSource random) {
        final ServerLevel level = (ServerLevel) player.level();                    // :281 worldObj cast
        final int spawns = WarpEventsLogic.spiderSpawns(warp);                     // :283
        for (int a = 0; a < spawns; a++) {                                         // :284
            final EntityMindSpider spider =
                    new EntityMindSpider(CatalogMindSpider.MIND_SPIDER.get(), level);
            final int i = Mth.floor(player.getX());                                // :287
            final int j = Mth.floor(player.getY());                                // :288
            final int k = Mth.floor(player.getZ());                                // :289
            boolean success = false;
            for (int l = 0; l < WarpEventsLogic.SCAN_ATTEMPTS; l++) {              // :291
                final int i1 = i + scanOffset(random) * scanSign(random);          // :292-293
                final int j1 = j + scanOffset(random) * scanSign(random);          // :294-295
                final int k1 = k + scanOffset(random) * scanSign(random);          // :296-297
                if (cellFits(level, i1, j1 - 1, k1)) {                             // :298 World.func_147466_a
                    spider.moveTo(i1, j1, k1, spider.getYRot(), spider.getXRot());  // :299 setPosition
                    final AABB box = spider.getBoundingBox();
                    if (level.noCollision(box)                                   // :300 func_72855_b
                            && level.getEntities(spider, box).isEmpty()            // :301 func_72945_a
                            && level.getEntities((Entity) null, box, e -> !(e instanceof Player)).isEmpty()) {
                        // :302 — !world.func_72953_d(bb): the box must carry no
                        // colliding non-player entity (the original's checkNoEntity
                        // -colliding term; players are the excluded witness).
                        success = true;                                            // :303
                        break;
                    }
                }
            }
            if (success) {
                // :314-315 setAttackTarget + setRevengeTarget. 1.21.1 removed
                // the revenge-target field from LivingEntity (measured on the
                // decompiled jar: no setRevengeTarget symbol); Mob#setTarget is
                // the single surviving seam. The swarm is NoAi by construction
                // (BD-74, the original's :90-92 switch), so this is the
                // target-on-AI-activation shape, not a live bite.
                spider.setTarget(player);                                          // :314 func_70784_b
                if (!real) {                                                       // :316
                    spider.setViewer(player.getScoreboardName());                  // :317 setViewer(player.getCommandSenderName())
                    spider.setHarmless(true);                                      // :318
                }
                level.addFreshEntity(spider);                                      // :320 spawnEntityInWorld
            }
        }
        chat(player, WarpEventsLogic.KEY_SPIDERS);                                 // :321 warp.text.7
    }

    /**
     * {@code WarpEvents.spawnMist} ({@code :229-242}): the mist FX, then up to
     * {@code min(8, guardian)} eldritch guardians, then the chat line.
     */
    private static void spawnMist(final ServerPlayer player, final int guardian) {
        PacketDistributor.sendToPlayer(player, new MiscEventPayload(MiscEventPayload.EVENT_MIST)); // :230 PacketMiscEvent(1)
        if (guardian > 0) {                                                        // :231
            for (int a = 0; a < guardian; a++) {                                   // :233-235
                summonGuardian(player);                                            // :234
            }
        }
        chat(player, WarpEventsLogic.KEY_MIST);                                    // :241 warp.text.6
    }

    /**
     * {@code WarpEvents.spawnGuardian} ({@code :264-280}): an
     * {@code EntityEldritchGuardian} placed by a 50-attempt offset scan and
     * set on the player. The guardian is not registered, so the summon runs
     * through {@link EldritchMobHost} — the documented no-op until the
     * guardian card claims it, the same seam {@code TileEldritchCrabSpawner}
     * and {@code TileEldritchAltar} use; when it lands, the host's spawner
     * owns the {@code :264-280} scan (the scan needs the registered entity
     * type to build the box, so it cannot precede registration).
     */
    private static void summonGuardian(final ServerPlayer player) {
        EldritchMobHost.spawn(EldritchMobHost.GUARDIAN, (ServerLevel) player.level(),
                BlockPos.containing(player.getX(), player.getY(), player.getZ()));
    }

    /**
     * {@code WarpEvents.getWarpFromGear} ({@code :353-367}): warp from the
     * main-hand stack, the four armor slots and the baubles slots (the port's
     * accessory worn set, TC-M8-01), each through {@link IWarpingGear}
     * ({@code EventHandlerRunic.getFinalWarp}, {@code :356-363}).
     */
    static int getWarpFromGear(final Player player) {
        int w = 0;
        final ItemStack mainHand = player.getMainHandItem();                       // :354 func_71045_bC()
        if (mainHand.getItem() instanceof IWarpingGear gear) {
            w += gear.getWarp(mainHand, player);
        }
        for (final EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) { // :355-358
            final ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof IWarpingGear gear) {
                w += gear.getWarp(armor, player);
            }
        }
        for (final ItemStack worn : AccessoryService.wornStacks(player)) {         // :359-363 BaublesApi slots
            if (!worn.isEmpty() && worn.getItem() instanceof IWarpingGear gear) {
                w += gear.getWarp(worn, player);
            }
        }
        return w;
    }

    /**
     * The {@code :51-56} helm condition: {@code ItemFortressArmor} with the
     * {@code mask} NBT tag == 0. Fortress armour ships on TC-M6-07; no
     * shipped stack's item implements it, so this is false for every shipped
     * stack and the {@code :57-58} discount never fires. Kept as a named
     * predicate so the wired discount flips with the armour.
     */
    static boolean fortressMaskActive(final ItemStack helm) {
        return false;
    }

    /**
     * The {@code :235} Warp Ward gate ({@code !player.isPotionActive(
     * Config.potionWarpWardID)}). The Warp Ward effect (bath-salts brew) is
     * not registered — the TC-M4-03 potion lane — so no player carries it and
     * the guard is inert, named here so the wiring flips with the effect.
     */
    static boolean warpWardActive(final ServerPlayer player) {
        return false;
    }

    /** {@code :248/:292} — {@code MathHelper.func_76136_a(rand, 7, 24)} (inclusive). */
    private static int scanOffset(final RandomSource random) {
        return random.nextIntBetweenInclusive(WarpEventsLogic.OFFSET_MIN, WarpEventsLogic.OFFSET_MAX);
    }

    /** {@code :248/:292} — the sign term, {@code MathHelper.func_76136_a(rand, -1, 1)}. */
    private static int scanSign(final RandomSource random) {
        return random.nextIntBetweenInclusive(WarpEventsLogic.SIGN_MIN, WarpEventsLogic.SIGN_MAX);
    }

    /**
     * {@code World.func_147466_a(world, x, y, z)} — the 1×1×1 cell at the
     * coordinates fits: no block collides with it and no entity is inside it
     * (the original composes {@code func_72855_b} (blocks) with
     * {@code func_72953_d} (entities) over the unit box).
     */
    private static boolean cellFits(final Level level, final int x, final int y, final int z) {
        final AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
        return level.noCollision(cell) && level.getEntities((Player) null, cell).isEmpty();
    }

    /**
     * The original's chat lines: {@code func_145747_a(new ChatComponentText(
     * "§5§o" + translateToLocal(key)))} — dark purple italic into chat
     * (never the action bar).
     */
    private static void chat(final ServerPlayer player, final String key) {
        player.displayClientMessage(
                Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                false);
    }

    private WarpEvents() {
    }
}
