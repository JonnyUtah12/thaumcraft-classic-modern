package com.jonnyutah12.thaumcraftclassic.warp;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryService;
import com.jonnyutah12.thaumcraftclassic.api.IWarpingGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The warp mechanic's driver (TC-M6-05-s): the periodic warp event and the
 * death-gaze leg.
 *
 * <p>Original: {@code thaumcraft/common/lib/WarpEvents.java}
 * ({@code checkWarpEvent} lines 39-225, {@code checkDeathGaze} lines 324-347),
 * called from {@code EventHandlerEntity.livingTick}
 * ({@code common/lib/events/EventHandlerEntity.java:235-243}). Every number
 * and threshold lives in {@link WarpLogic}, which is the pure half of those two
 * methods; this class only performs the reads (warp attachment, config, world
 * random, gear) and the writes (effects, chat, warp counters, aspect pool).</p>
 *
 * <h2>What the port does not do (each row is a card of its own)</h2>
 * <ul>
 *   <li><b>Spawns.</b> {@code spawnMist} / {@code spawnGuardian} /
 *       {@code suddenlySpiders} ({@code WarpEvents.java:226-323}) spawn the
 *       eldritch guardian and the mind spider — entity content, TC-M6-06. The
 *       mist/spider outcomes still run their chat line and (for the mist) the
 *       original's client FX packet slot, so the event is legible; the counts
 *       the original computed are carried in
 *       {@link WarpLogic.WarpOutcome#guardians()} and asserted by the unit
 *       test, so the spawn cards have the arithmetic pinned.</li>
 *   <li><b>Client FX + sound.</b> {@code PacketMiscEvent((short)0)} (the warp
 *       event's screen flash) and {@code (short)1} (the mist), plus the
 *       {@code FxWarp}/{@code bloodpoof}/{@code sanity} particles, are
 *       TC-M7-02 / TC-M7-04; the port has no FX to send.</li>
 *   <li><b>Fortress-mask relief.</b> {@code WarpEvents.java:55-60} subtracts
 *       {@code 2 + rand(4)} from the roll for an enchanted fortress mask —
 *       armour content, TC-M8.</li>
 *   <li><b>Eldritch research grants.</b> The {@code actualwarp} 10/25/50 grants
 *       ({@code WarpEvents.java:209-222}) name {@code @BATHSALTS},
 *       {@code ELDRITCHMINOR} and {@code ELDRITCHMAJOR}; the shipped
 *       201-entry research graph has no such entries (verified against
 *       {@code data/thaumcraft/research_graph/entries.json}), so each grant is
 *       graph-gated and inert here — the {@code @BATHSALTS} hint chat is gated
 *       the same way, matching the original's "only if the research is not
 *       known" gate. The bath-salts item this hint points at is registered by
 *       this card ({@code items/ItemBathSalts}).</li>
 * </ul>
 *
 * <p>The warp counters are the {@code player/PlayerWarpData} attachment
 * (docs/architecture/persistence.md): a counter write syncs with
 * {@link SyncWarpPayload} where the original sent {@code PacketSyncWarp}, and
 * warp gains go through {@link ResearchManager#applyWarp}, which is the
 * original's {@code addWarpToPlayer}/{@code addStickyWarpToPlayer}
 * (counter update + sync + {@code WarpMessagePayload}).</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class WarpEvents {

    private WarpEvents() {
    }

    /**
     * Original {@code EventHandlerEntity.livingTick} ({@code :235-243}):
     * server side only, the warp roll behind the wuss + warp-ward gates, the
     * death gaze on its own 10-tick cadence with no wuss gate.
     */
    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        final Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final int tick = player.tickCount;
        if (isWarpEventTick(tick)
                && !TcConfig.getBool(TcConfigCatalog.WUSS_MODE)
                && !player.hasEffect(ModMobEffects.POTION_WARWARD)) {
            checkWarpEvent(serverPlayer);
        }
        if (tick % WarpLogic.DEATH_GAZE_INTERVAL_TICKS == 0
                && player.hasEffect(ModMobEffects.POTION_DEATHGAZE)) {
            checkDeathGaze(serverPlayer);
        }
    }

    /**
     * The original's {@code player.ticksExisted > 0 && % 2000 == 0}
     * ({@code EventHandlerEntity.java:236}).
     */
    public static boolean isWarpEventTick(final int tickCount) {
        return tickCount > 0 && tickCount % WarpLogic.WARP_EVENT_INTERVAL_TICKS == 0;
    }

    /**
     * Original {@code WarpEvents.checkWarpEvent}. Package-visible so the game
     * probe can drive one event directly instead of ticking 2000 times.
     */
    public static void checkWarpEvent(final ServerPlayer player) {
        final PlayerWarpData warpData = player.getData(ModAttachments.PLAYER_WARP);
        // WarpEvents.java:40-43: the roll uses warp total + gear warp; the
        // research hints use perm + sticky ("actualwarp"), which gear does NOT
        // feed — keep the two values separate exactly as the original does.
        final int warp = warpData.total() + gearWarp(player);
        final int actualWarp = warpData.warpPerm() + warpData.warpSticky();
        int warpCounter = warpData.warpCounter();
        // The original draws from the world random (world.rand, :44).
        final RandomSource random = player.level().getRandom();

        if (WarpLogic.fires(warp, warpCounter, random.nextInt(100))) {
            final int rolled = WarpLogic.rollWarp(warp, warpCounter);
            warpCounter = WarpLogic.rollCounter(warpCounter);
            player.setData(ModAttachments.PLAYER_WARP, warpData.withWarpCounter(warpCounter));
            applyOutcome(player, WarpLogic.outcome(WarpLogic.rollEff(random, rolled), rolled), rolled);
            grantEldritchResearch(player, actualWarp);
        }

        // WarpEvents.java:224-225: the temp-warp decay runs on every call,
        // fired or not, and syncs the temporary counter afterwards.
        final PlayerWarpData decayed = player.getData(ModAttachments.PLAYER_WARP)
                .withWarpTemp(WarpLogic.tempWarpAfterEvent(player.getData(ModAttachments.PLAYER_WARP).warpTemp()));
        player.setData(ModAttachments.PLAYER_WARP, decayed);
        PacketDistributor.sendToPlayer(player,
                new SyncWarpPayload(decayed.warpTemp(), SyncWarpPayload.WARP_TEMPORARY));
    }

    /**
     * Applies one table branch. The effect durations/amplifiers come from
     * {@link WarpLogic#outcome}; {@code visibleParticles} is the original's
     * 4th {@code PotionEffect} argument (true everywhere except the infectious
     * flux branch, {@code WarpEvents.java:150}).
     */
    private static void applyOutcome(final ServerPlayer player, final WarpLogic.WarpOutcome outcome,
                                     final int warp) {
        switch (outcome.kind()) {
            case NONE -> {
            }
            case RESEARCH -> {
                grantResearch(player, outcome.times());
                chat(player, outcome.langKey());
            }
            case CHAT -> chat(player, outcome.langKey());
            case VIS_EXHAUST -> effect(player, ModMobEffects.POTION_VISEXHAUST,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case THAUMARHIA -> effect(player, ModMobEffects.POTION_THAUMARHIA,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case UNNATURAL_HUNGER -> effect(player, ModMobEffects.POTION_UNHUNGER,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case SUN_SCORNED -> effect(player, ModMobEffects.POTION_SUNSCORNED,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case INFECTIOUS_VIS_EXHAUST -> effect(player, ModMobEffects.POTION_INFVISEXHAUST,
                    outcome.duration(), outcome.amplifier(), false, outcome.langKey());
            case DEATH_GAZE -> effect(player, ModMobEffects.POTION_DEATHGAZE,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case BLURRED_VISION -> effect(player, ModMobEffects.POTION_BLURRED,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case DIG_SLOWDOWN -> effect(player, MobEffects.DIG_SLOWDOWN,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case NIGHT_VISION -> effect(player, MobEffects.NIGHT_VISION,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case BLINDNESS -> effect(player, MobEffects.BLINDNESS,
                    outcome.duration(), outcome.amplifier(), true, outcome.langKey());
            case STICKY_RELIEF -> {
                // WarpEvents.java:169-175: one sticky warp back off, with the
                // original's own sync + notification pair, then the chat line.
                if (player.getData(ModAttachments.PLAYER_WARP).warpSticky() > 0) {
                    ResearchManager.applyWarp(player, -1, SyncWarpPayload.WARP_STICKY);
                }
                chat(player, outcome.langKey());
            }
            case MIST, SPIDERS -> {
                // The chat lines of spawnMist (:233) and suddenlySpiders
                // (:320); the spawns and the mist FX packet are TC-M6-06 /
                // TC-M7-02 (see the class javadoc).
                chat(player, outcome.langKey());
            }
        }
    }

    /** Adding one effect, then the branch's chat line. */
    private static void effect(final ServerPlayer player, final Holder<net.minecraft.world.effect.MobEffect> effect,
                               final int duration, final int amplifier, final boolean visibleParticles,
                               final String langKey) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, visibleParticles));
        if (langKey != null) {
            chat(player, langKey);
        }
    }

    /**
     * Original {@code player.addChatMessage(new ChatComponentText("§5§o" +
     * StatCollector.translate(key)))} — dark-purple italic translated text,
     * overlay=false (the original's chat box, not the action bar).
     */
    private static void chat(final ServerPlayer player, final String langKey) {
        if (langKey == null) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(langKey).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                false);
    }

    /**
     * Original {@code WarpEvents.grantResearch} ({@code :236-247}):
     * {@code 1 + rand(times)} points, each one a random primal aspect in the
     * player's pool. The original synced each point with
     * {@code PacketAspectPool} and called {@code ResearchManager.scheduleSave};
     * the pool here is the {@code PLAYER_KNOWLEDGE} attachment, whose sync is
     * the attachment sync (docs/architecture/persistence.md), and the
     * attachment is the save (no scheduled file write).
     */
    public static void grantResearch(final ServerPlayer player, final int times) {
        final RandomSource random = player.level().getRandom();
        final List<Aspect> primals = Aspect.getPrimalAspects();
        if (primals.isEmpty()) {
            return;
        }
        // The original's rand.nextInt(times) would throw for times <= 0; the
        // table never reaches it (eff 84 needs warp > 84, so warp/10 >= 8), and
        // the floor of 1 keeps the port from crashing on a hand-built call.
        final int amount = 1 + random.nextInt(Math.max(1, times));
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (int a = 0; a < amount; a++) {
            final Aspect primal = primals.get(random.nextInt(primals.size()));
            knowledge = knowledge.withAspectPoolAdded(primal.getTag(), 1);
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    /**
     * The three {@code actualwarp} thresholds of {@code WarpEvents.java:209-222}.
     * Each grant names a research key the shipped graph does not contain, so
     * the graph gate keeps them inert until the eldritch research lands
     * (TC-M6-08); the shape of the gate — chat the hint, complete the
     * research, grant the pool — is the original's.
     */
    private static void grantEldritchResearch(final ServerPlayer player, final int actualWarp) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (actualWarp > 10
                && entryExists("@BATHSALTS")
                && !ResearchManager.isResearchComplete(knowledge, "BATHSALTS")
                && !ResearchManager.isResearchComplete(knowledge, "@BATHSALTS")) {
            chat(player, "warp.text.8");
            ResearchManager.completeResearch(player, "@BATHSALTS");
        }
        if (actualWarp > 25 && entryExists("ELDRITCHMINOR")
                && !ResearchManager.isResearchComplete(knowledge, "ELDRITCHMINOR")) {
            grantResearch(player, 10);
            ResearchManager.completeResearch(player, "ELDRITCHMINOR");
        }
        if (actualWarp > 50 && entryExists("ELDRITCHMAJOR")
                && !ResearchManager.isResearchComplete(knowledge, "ELDRITCHMAJOR")) {
            grantResearch(player, 20);
            ResearchManager.completeResearch(player, "ELDRITCHMAJOR");
        }
    }

    /** Whether a research key ships in the graph (the gate above's registry guard). */
    public static boolean entryExists(final String key) {
        return ResearchManager.graph().entry(key) != null;
    }

    /**
     * Original {@code WarpEvents.getWarpFromGear} ({@code :349-361}): the
     * {@code IWarpingGear.getWarp} of the worn gear. The original walked the
     * elytra slot, the four armour slots and the four baubles slots through
     * {@code EventHandlerRunic.getFinalWarp}; the port sums the armour slots
     * and the accessory slots (curios when present, the vanilla slots
     * otherwise — {@code accessory/AccessoryService}). No shipped item
     * implements {@link IWarpingGear} yet, so this reads 0.
     */
    public static int gearWarp(final Player player) {
        int w = 0;
        for (final ItemStack stack : player.getInventory().armor) {
            w += stackWarp(stack, player);
        }
        for (final ItemStack stack : AccessoryService.wornStacks(player)) {
            w += stackWarp(stack, player);
        }
        return w;
    }

    private static int stackWarp(final ItemStack stack, final Player player) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof IWarpingGear gear) {
            return gear.getWarp(stack, player);
        }
        return 0;
    }

    /**
     * Original {@code WarpEvents.checkDeathGaze} ({@code :324-347}): while the
     * deadly-gaze effect is on the player, every living mob in the range cone
     * that is not already withered is targeted and withered for
     * {@value WarpLogic#DEATH_GAZE_WITHER_TICKS} ticks.
     *
     * <p>Two gates of the original have no ported counterpart and are documented
     * deviations: the {@code MinecraftServer.func_71219_W()} server switch that
     * lets the gaze take player targets (the port excludes players — see
     * docs/architecture/enchantments-potions.md), and the original's
     * {@code player.func_70685_l(entity)} proximity gate, which maps to
     * {@link Entity#canSee(Entity)} line-of-sight here.</p>
     */
    public static void checkDeathGaze(final ServerPlayer player) {
        final MobEffectInstance gaze = player.getEffect(ModMobEffects.POTION_DEATHGAZE);
        if (gaze == null) {
            return;
        }
        final int range = WarpLogic.deathGazeRange(gaze.getAmplifier());
        final AABB box = player.getBoundingBox().inflate(range);
        for (final Entity entity : player.level().getEntities(player, box, Entity::isAlive)) {
            if (!(entity instanceof LivingEntity mob) || mob == player) {
                continue;
            }
            if (mob instanceof Player) {
                continue;
            }
            if (mob.hasEffect(MobEffects.WITHER) || !player.hasLineOfSight(entity)) {
                continue;
            }
            if (!WarpLogic.coneVisible(player.position(), player.getLookAngle(), entity.position(),
                    WarpLogic.DEATH_GAZE_APERTURE_RADIANS, range)) {
                continue;
            }
            // WarpEvents.java:336-343: target the player, then the wither hit.
            if (mob instanceof Mob creature) {
                creature.setTarget(player);
            }
            mob.addEffect(new MobEffectInstance(MobEffects.WITHER, WarpLogic.DEATH_GAZE_WITHER_TICKS));
        }
    }
}
