package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The taint spore swarmer (TC-M6-06-s-f, content-mapping.csv
 * {@code thaumcraft:taintswarmer}).
 *
 * <p>Port of {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintSporeSwarmer.java}
 * (163 lines) — a top-level subclass of {@link EntityTaintSpore} in the original
 * ({@code extends EntityTaintSpore}, {@code :16}), so it ships as its own
 * top-level class here. Every number and branch is quoted from that file; the
 * arithmetic lives in {@link TaintSporeAiLogic} beside the spore's.</p>
 *
 * <p>What the subclass changes, verbatim: a fixed 1×1 box in its own
 * {@code setSporeSize} copy ({@code :25-30}), health 75 / attack 1 written on top of
 * the parent's 1 / 1 ({@code :32-37}), {@code sploosh(10)} on every incoming hit
 * ({@code :39-45}), the 500-tick swarm cadence with a 16-block player gate
 * ({@code :47-75}), NO player interaction at all ({@code :77-79}), the swarm burst
 * plus entity event 6 ({@code :81-109}), ambient {@code thaumcraft:roots}
 * ({@code :116-119}), a sampled light/overlay pair ({@code :121-146}) and TWO
 * independent loot coin flips ({@code :153-162}). Everything else is the spore's,
 * including the size-10 start ({@code :21}), which also means the inherited
 * {@code field_70728_aV = size} write leaves it worth 10 xp.</p>
 *
 * <p>1.7.10 → 1.21.1 hook map: {@code func_70097_a} (attackEntityFrom) →
 * {@link #hurt(DamageSource, float)}; {@code func_70103_a(byte)}
 * (handleEntityEvent) → {@link #handleEntityEvent(byte)};
 * {@code world.func_72960_a(this, (byte) 6)} → {@link Level#broadcastEntityEvent};
 * {@code world.func_72856_b(this, 16.0)} (getClosestPlayerToEntity) →
 * {@link Level#getNearestPlayer(net.minecraft.world.entity.Entity, double)};
 * {@code world.func_72956_a} → {@link Level#playSound(Player, net.minecraft.world.entity.Entity, SoundEvent, SoundSource, float, float)}.
 * The {@code func_145771_j(x, y, z)} call at {@code :49} is the 1.7.10
 * self-position re-assert of the {@code setPosition} family (it re-writes the
 * position and the last-tick copies, killing the interpolation jitter); the 1.21.1
 * equivalent is {@link net.minecraft.world.entity.Entity#setPos(double, double, double)}
 * with the entity's own coordinates.</p>
 *
 * <p>Client-only, no seam: {@code func_70070_b} (getBrightness, {@code :121-133}) and
 * {@code func_70013_c} (getBrightness-for-color, {@code :135-146}) — the light
 * sampling moved into the renderer in 1.21.1 — plus
 * {@code Thaumcraft.proxy.swarmParticleFX / splooshFX} ({@code :68}, {@code :41},
 * {@code :99}) and the {@code EntityFX.setDead()} sweep of {@code :101-103}. The
 * sampling RULE ships pinned ({@link TaintSporeAiLogic#brightnessSampleY},
 * {@link TaintSporeAiLogic#swarmerBrightness},
 * {@link TaintSporeAiLogic#swarmerOverlayLightmap}); the effects are the client-FX
 * card's (see this card's left_out_of_scope).</p>
 */
public class EntityTaintSporeSwarmer extends EntityTaintSpore {

    /**
     * The 1.7.10 spawn id of the creature the burst releases
     * ({@code EntityTaintSporeSwarmer.java:86}, {@code EntityTaintSwarm}). Referenced
     * as a constant here and resolved from the live registry at call time — the
     * {@link TaintManager#spawnGuarded} shape, inert while the id is unregistered and
     * needing no edit from this card when it lands. NOT added to the entity catalog
     * (the catalog row is the parent's).
     */
    public static final ResourceLocation TAINT_SWARM_ID =
            ResourceLocation.fromNamespaceAndPath(
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "taintswarm");

    /** {@code :17} — {@code int spawnCounter = 500} (a private field in the original). */
    private int spawnCounter = TaintSporeAiLogic.SWARMER_SPAWN_INTERVAL;

    public EntityTaintSporeSwarmer(final EntityType<? extends EntityTaintSporeSwarmer> type,
                                   final Level level) {
        super(type, level);
        // :21 — setSporeSize(10). The PARENT ctor already ran setSporeSize(2) through
        // this override (dynamic dispatch), exactly as in the original; the box stays
        // 1.0 both times.
        this.setSporeSize(TaintSporeAiLogic.SWARMER_INITIAL_SIZE);
    }

    /**
     * :32-37 — {@code super.func_110147_ax()} first (the spore's health 1.0 / attack
     * 1.0), then MAX_HEALTH 75.0 and ATTACK_DAMAGE 1.0 overwrite them.
     */
    public static AttributeSupplier.Builder createSwarmerAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintSporeAiLogic.SWARMER_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintSporeAiLogic.SWARMER_ATTACK);
    }

    /**
     * :24-30 — the swarmer's OWN copy of the size setter: the same watcher write,
     * the same self-position write and the same {@code field_70728_aV = size} xp
     * write, and a FIXED 1.0×1.0 box. The size ladder still moves the swarm budget
     * and the xp, and never touches the hitbox.
     */
    @Override
    public void setSporeSize(final int sporeSize) {
        this.setSporeSizeData(sporeSize);                            // :26
        this.sporeBox = TaintSporeAiLogic.SWARMER_BOX;               // :27
        this.refreshDimensions();                                    // (the 1.21.1 half of :27)
        this.setPos(this.getX(), this.getY(), this.getZ());           // :28
        this.xpReward = TaintSporeAiLogic.xpForSize(sporeSize);       // :29
    }

    /**
     * :39-45 — the client plays {@code sploosh(10)} on EVERY incoming hit before
     * handing the hit to the parent. The client half is split out so a game-test
     * server proves the server path (1.21.1 only calls {@code hurt} server-side).
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        this.swarmerHurtClientFx(this.level().isClientSide);
        return super.hurt(source, amount);
    }

    /** :40-42 — the client half of {@code attackEntityFrom}. */
    protected void swarmerHurtClientFx(final boolean clientSide) {
        if (clientSide) {
            this.sploosh(TaintSporeAiLogic.SWARMER_HURT_SPLLOOSH);
        }
    }

    /**
     * :47-75 — the swarmer replaces the spore's whole update: the self-position
     * re-assert, the 500-tick cadence gated on a player within 16 blocks, the
     * client's swarm-FX budget on its own curve, and the hurt-flash burst on
     * {@code hurtDuration == 1}. The spore's growth ladder and its stalk check are
     * gone entirely — a swarmer never grows and never roots.
     */
    @Override
    protected void sporeOnUpdate() {
        this.setPos(this.getX(), this.getY(), this.getZ());   // :49 — func_145771_j
        this.spawnCounter = TaintSporeAiLogic.swarmerNextSpawnCounter(this.spawnCounter);   // :50-52
        if (TaintSporeAiLogic.swarmerBursts(this.spawnCounter, this.playerWithinScan())) {  // :54-57
            this.spawnCounter = TaintSporeAiLogic.SWARMER_SPAWN_INTERVAL;
            this.swarmBurst(1);
        }
        if (this.level().isClientSide) {                                                  // :59-70
            this.cullSwarmFx();
            if (TaintSporeAiLogic.swarmerWantsSwarmFx(this.swarmFx.size(), this.spawnCounter)) {
                this.addSwarmFx();
            }
        }
        if (TaintSporeAiLogic.swarmerBurstsOnHurt(this.hurtDuration)) {                   // :72-74
            this.swarmBurst(1);
        }
    }

    /** :54 — {@code world.getClosestPlayerToEntity(this, 16.0) != null}. */
    private boolean playerWithinScan() {
        final Player player = this.level().getNearestPlayer(this, TaintSporeAiLogic.SWARMER_PLAYER_SCAN);
        return player != null;
    }

    /** :77-79 — the swarmer overrides onPlayerInteract with an EMPTY body: no burst. */
    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /**
     * :81-93 — the swarm burst. Server only: the gore cue, {@code amt} taint swarms
     * half a block above the swarmer's feet, then entity event 6 so the clients
     * re-arm and pop their own FX.
     */
    protected void swarmBurst(final int amt) {
        if (this.level().isClientSide) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // :83 — world.playSoundAtEntity(this, "thaumcraft:gore", 1.0F,
        // 0.9F + world.rand.nextFloat() * 0.1F)
        serverLevel.playSound(null, this, ModSounds.GORE.get(), SoundSource.NEUTRAL,
                TaintSporeAiLogic.GORE_VOLUME,
                TaintSporeAiLogic.gorePitch(serverLevel.getRandom().nextFloat()));
        for (int a = 0; a < amt; a++) {
            // :86-88 — the swarm spawns at (x, y + 0.5, z) with a random yaw;
            // spawnGuarded sets the position, and the id stays registry-resolved.
            TaintManager.spawnGuarded(serverLevel, TAINT_SWARM_ID,
                    new Vec3(this.getX(), this.getY() + TaintSporeAiLogic.SWARM_SPAWN_Y_OFFSET,
                            this.getZ()));
        }
        serverLevel.broadcastEntityEvent(this, TaintSporeAiLogic.SWARMER_BURST_EVENT);   // :91
    }

    /**
     * :95-109 — event 6: the clients re-arm the cadence, pop {@code sploosh(25)} and
     * kill their swarm handles; every other event goes to the parent. The original
     * marks the method {@code @SideOnly(Side.CLIENT)}; 1.21.1 dispatches entity
     * events to the receiving side, which is the same audience.
     */
    @Override
    public void handleEntityEvent(final byte id) {
        if (id == TaintSporeAiLogic.SWARMER_BURST_EVENT) {
            this.spawnCounter = TaintSporeAiLogic.SWARMER_SPAWN_INTERVAL;   // :98
            this.sploosh(TaintSporeAiLogic.SWARMER_EVENT_SPLLOOSH);         // :99
            this.clearSwarmFx();                                            // :101-105
        } else {
            super.handleEntityEvent(id);                                    // :107
        }
    }

    /**
     * :101-105 — {@code for (a...) ((Entity) swarm.get(a)).setDead(); swarm.clear();}.
     * The handles are {@code EntityFX} objects and the port has no proxy FX seam, so
     * only the list clear is reachable; the kill is the client-FX card's.
     */
    protected void clearSwarmFx() {
        for (int a = 0; a < this.swarmFx.size(); a++) {
            // ((Entity) swarm.get(a)).setDead() — no FX seam
        }
        this.swarmFx.clear();
    }

    /** :111-114 — ambient sound interval 200 (the same value the parent answers). */
    @Override
    public int getAmbientSoundInterval() {
        return TaintSporeAiLogic.AMBIENT_SOUND_INTERVAL;
    }

    /** :116-119 — ambient {@code thaumcraft:roots} (the parent's is {@code thaumcraft:swarm}). */
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOTS.get();
    }

    /**
     * :148-150 — getDropItemId stays itemResource (inherited).
     * :153-162 — dropRareDrop rolls the goo/fungus coin flip TWICE
     * ({@code for (a = 0; a <= 1; a++)}), each roll independent, so a swarmer can
     * drop two of the same; the offset is the 1.0 box height / 2.
     */
    @Override
    protected void dropFromLootTable(final DamageSource source, final boolean attackedRecently) {
        for (int a = 0; a < TaintSporeAiLogic.SWARMER_DROP_ROLLS; a++) {
            final ItemStack drop = ItemResource.stack(TaintSporeAiLogic.rollsGoo(this.random)
                    ? TaintSporeAiLogic.META_TAINTED_GOO : TaintSporeAiLogic.META_TAINTED_FUNGUS);
            this.spawnAtLocation(drop, TaintSporeAiLogic.dropOffset(this.getBbHeight()));   // :157/:159
        }
        super.dropFromLootTable(source, attackedRecently);                                   // :161
    }

    /** The swarm cadence counter (1.7.10 {@code spawnCounter}, {@code :17}), for probes. */
    public int getSpawnCounter() {
        return this.spawnCounter;
    }

    /** For probes: force the cadence counter. */
    public void setSpawnCounter(final int ticks) {
        this.spawnCounter = ticks;
    }
}
