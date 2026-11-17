package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockArcPayload;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBanner;

import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * The Crimson Portal — {@code thaumcraft:cultistportal} (content-mapping
 * {@code Thaumcraft:CultistPortal}, mod_index 30), {@code thaumcraft.common.entities.monster.boss.EntityCultistPortal}
 * (313 lines, {@code extends EntityMob implements IBossDisplayData}, {@code :31}).
 * Registration {@code ConfigEntities.java:117} — {@code (64, 20, false)}.
 *
 * <p>The summoner of the boss band: a 1.5 x 3.0, fire-immune, 500-health,
 * armour-5, eyeless statue that never moves, never swings, installs no goal
 * selector, accepts no potion effect and makes no footsteps. Its entire behaviour
 * is the stage machine of {@code :99-209}, fully on the rule class
 * {@link CultistPortalAiLogic}: a 200-tick countdown that walls the room with four
 * banners at 160, seeds loot crates on every 13th tick in (20, 150), self-heals 1 HP
 * per tick below stage 12, and on reaching zero fires the pulse status, summons a
 * knight/cleric wave out of the cultists within 32 blocks (or one Crimson Praetor at
 * stage 12) and raises the stage by one — with no wrap, so past stage 12 the summoner
 * starts drowning itself.</p>
 *
 * <p><b>It is NOT a {@link EntityThaumcraftBoss} and never was.</b> The reference
 * extends {@code EntityMob} directly: it has no enrage, no aggro bookkeeping, no
 * player-count scaling, no home restriction, no shared boss tick body and no
 * {@code IBossDisplayData} sibling behaviour — the marker it implements is
 * decoration for the client's boss bar ({@code :31}). Recording the choice here
 * (D-137) is the point: a port that "helpfully" extended the boss base would install
 * five goals, a rage rule and a 5x50 health scale the reference does not have.</p>
 *
 * <p><b>The goal set is empty by construction.</b> 1.21.1's {@code Monster} installs
 * the Float/Melee/Wander/Look quartet plus a hurt-target goal through
 * {@code registerGoals}; this override does not call {@code super.registerGoals()}
 * (the Pech card's finding, {@code PechAiLogic:317-324}), so the goal set measures
 * empty — which is the reference's own state, because {@code :83-84}
 * ({@code func_70626_be}, updateEntityActionState) is an EMPTY override: 1.7.10's
 * {@code EntityLiving#onUpdate} ticks the goal selector from the very method this
 * class empties.</p>
 */
public class EntityCultistPortal extends Monster {

    /** {@code :32} — {@code int stage = 0}. */
    private int stage = CultistPortalAiLogic.STAGE_INITIAL;
    /** {@code :33} — {@code int stagecounter = 200}. */
    private int stageCounter = CultistPortalAiLogic.COUNTER_INITIAL;
    /** {@code :34} — {@code public int pulse = 0}; the client-side pulse countdown. */
    private int pulse = CultistPortalAiLogic.PULSE_INITIAL;

    /**
     * The {@code IBossDisplayData} seam ({@code :31}): 21.1.259 has no counterpart of
     * the marker at all (the interface is not in the deobf source; the taintacle
     * giant's resolution of the same line, {@code EntityTaintacleGiant:75},
     * {@code :118-126}), so the bar is a {@link ServerBossEvent}
     * fed from the health fraction and shown to every player tracking the statue.
     * The name resolves the lang key
     * {@link CultistPortalAiLogic#BOSS_BAR_NAME_KEY}; the RED colour and the PROGRESS
     * overlay are port-chosen, as the giant's are — 1.7.10's boss bar has no colour
     * and no overlay (D-137).
     */
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    protected EntityCultistPortal(final EntityType<? extends Monster> type, final Level level) {
        super(type, level);
        this.xpReward = CultistPortalAiLogic.XP_REWARD;                                   // :39 field_70728_aV = 30
    }

    /**
     * {@code ConfigEntities.java:117} — {@code addMob(EntityCultistPortal.class,
     * "EntityCultistPortal", "Crimson Portal", 64, 20, false, …)}: tracking range
     * 64 blocks, update interval 20, velocity updates false. The {@code setSize}
     * pair of {@code :40} and the zero eye height of {@code :68-70} ride the
     * builder, as the family's shape rule requires (D-031, D-137), and
     * {@code :38}'s fire immunity rides {@code .fireImmune()} (the firebat card's
     * precedent, {@code EntityFireBat.java:141}).
     */
    public static EntityType<EntityCultistPortal> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityCultistPortal>of(EntityCultistPortal::new, category)
                .sized(CultistPortalAiLogic.WIDTH, CultistPortalAiLogic.HEIGHT)           // :40 (1.5F, 3.0F)
                .eyeHeight(CultistPortalAiLogic.EYE_HEIGHT)                               // :68-70 (func_70053_R = 0.0F)
                .fireImmune()                                                             // :38 (field_70178_ae = true)
                .clientTrackingRange(CultistPortalAiLogic.TRACK_RANGE_CHUNKS)             // :117, 64 blocks = 4 chunks
                .updateInterval(CultistPortalAiLogic.UPDATE_INTERVAL)                     // :117, 20
                .setShouldReceiveVelocityUpdates(CultistPortalAiLogic.VELOCITY_UPDATES)   // :117, false
                .build("cultistportal");
    }

    /**
     * {@code :61-66} ({@code func_110147_ax}) plus {@code :43-45}
     * ({@code func_70658_aO} = armour 5, whose 1.21.1 holder is the
     * {@code minecraft:armor} attribute — the Pech card's resolution,
     * {@code EntityPech:213-216}).
     *
     * <p>The reference sets NO movement speed, so the attribute keeps 1.21.1's
     * {@code Monster} default. That is unobservable here: {@code travel} is a no-op
     * ({@code :80-81}) and the goal set is empty, so nothing ever writes a velocity
     * this attribute could scale.</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CultistPortalAiLogic.MAX_HEALTH)              // :63 500.0
                .add(Attributes.ATTACK_DAMAGE, CultistPortalAiLogic.ATTACK_DAMAGE)        // :64 0.0
                .add(Attributes.KNOCKBACK_RESISTANCE, CultistPortalAiLogic.KNOCKBACK_RESISTANCE) // :65 1.0
                .add(Attributes.ARMOR, CultistPortalAiLogic.ARMOR);                       // :43-45 armour 5
    }

    // ================================================== the empty goal selector

    /**
     * The empty goal selector — the reference registers nothing, and {@code :83-84}
     * empties the method that would tick it. 1.21.1's {@code Monster#registerGoals}
     * installs the Float/Melee/Wander/Look quartet plus a hurt-target goal, so the
     * {@code super} call is deliberately absent (the Pech card's finding,
     * {@code PechAiLogic:317-324}; the knight/cleric/leader lanes ship the same
     * idiom with their own goal tables).
     */
    @Override
    protected void registerGoals() {
        // :47-49 (entityInit writes only super) and :83-84 (empty
        // updateEntityActionState): the portal has no goals at all.
    }

    // ============================================================ the stage machine

    /**
     * {@code :99-209} — the whole tick. {@code :100} super, {@code :101} the
     * server-side gate, the firing branch at {@code :102-133}, the countdown branch
     * at {@code :135-201}, the self-heal at {@code :203-204} (inside the server
     * gate, outside both branches — it runs on every server tick) and the pulse
     * decrement at {@code :208-209} on BOTH sides.
     *
     * <p>1.7.10's {@code func_70071_h_} is 1.21.1's {@code LivingEntity#aiStep}:
     * the family's split rule (docs/deviations/entity-builders-and-the-boss-band.md)
     * puts the server half behind {@code isEffectiveAi()} and keeps the shared half
     * outside it, which is what the reference's brace nesting says.</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();                                                                   // :100
        if (this.isEffectiveAi()) {                                                       // :101
            // The IBossDisplayData seam (:31) — no reference line. The giant's
            // placement rule: the bar tracks the health fraction on the server half
            // of the tick (EntityTaintacleGiant:227-230).
            this.bossEvent.setName(this.getDisplayName());
            this.bossEvent.setProgress(CultistPortalAiLogic.bossHealthProgress(
                    this.getHealth(), this.getMaxHealth()));
            if (CultistPortalAiLogic.fires(this.stageCounter)) {                          // :102
                final Player player = this.level().getNearestPlayer(this,
                        CultistPortalAiLogic.PLAYER_SCAN);                                // :103 (48.0)
                if (player != null) {
                    this.level().broadcastEntityEvent(this, CultistPortalAiLogic.STATUS_PULSE); // :104 (byte 16)
                    final CultistPortalAiLogic.Summon action =
                            CultistPortalAiLogic.actionFor(this.stage);                   // :105
                    if (action == CultistPortalAiLogic.Summon.BOSS) {
                        this.stageCounter = CultistPortalAiLogic.bossCounterFor(this.getTiming(),
                                this.random.nextInt(CultistPortalAiLogic.BOSS_COUNTER_ROLL)); // :127
                        this.spawnBoss();                                                 // :128
                    } else if (CultistPortalAiLogic.isShortCounterStage(this.stage)) {
                        this.stageCounter = CultistPortalAiLogic.shortCounterFor(this.stage,
                                this.random.nextInt(
                                        CultistPortalAiLogic.SHORT_COUNTER_SPREAD - this.stage)); // :111
                        this.spawnMinions();                                              // :112
                    } else {
                        final int timing = this.getTiming();                              // :122
                        this.stageCounter = CultistPortalAiLogic.timedCounterFor(timing,
                                this.random.nextInt(
                                        CultistPortalAiLogic.timedCounterRollSpread(timing))); // :123
                        this.spawnMinions();                                              // :124
                    }
                    this.stage = CultistPortalAiLogic.stageAfterFiring(this.stage);       // :131 (no wrap, no cap)
                } else {
                    this.stageCounter = CultistPortalAiLogic.noPlayerCounterFor(
                            this.random.nextInt(CultistPortalAiLogic.NO_PLAYER_COUNTER_ROLL)); // :133
                }
            } else {
                this.stageCounter--;                                                      // :136
                if (CultistPortalAiLogic.bannerGate(this.stageCounter, this.stage)) {     // :137
                    this.level().broadcastEntityEvent(this, CultistPortalAiLogic.STATUS_PULSE); // :138
                    this.spawnBanners();                                                  // :140-183
                }
                if (CultistPortalAiLogic.crateGate(this.stageCounter, this.stage)) {      // :185
                    this.spawnCrate();                                                    // :186-199
                }
            }
            if (CultistPortalAiLogic.healsForStage(this.stage)) {                         // :203
                this.heal(CultistPortalAiLogic.HEAL_AMOUNT);                              // :204
            }
        }
        if (this.pulse > 0) {                                                             // :208
            this.pulse--;                                                                 // :209
        }
    }

    /**
     * {@code :213-218} — {@code getTiming()}: the cultist scan's size, times 20. The
     * scan is {@code EntityUtils.getEntitiesInRange(world, x, y, z, this,
     * EntityCultist.class, 32.0)} — a 32.0 cube about the portal's position with
     * {@code this} filtered out by entity id ({@code EntityUtils.java:221-233}),
     * which 1.21.1's {@code Level#getEntitiesOfClass(Class, AABB, Entity)} does in
     * one call.
     */
    public int getTiming() {
        return CultistPortalAiLogic.timingFor(this.summonScan().size());                   // :217
    }

    /**
     * {@code :214-216} — the scan itself, public so a GameTest can measure the
     * radius and the class filter directly (the card's "scan radius" probe): the
     * box is the portal's position expanded 32.0 on every axis, and the admitted
     * class is the landed {@link EntityCultist} base, so the knight, the cleric and
     * the Praetor all count.
     */
    public java.util.List<EntityCultist> summonScan() {
        final AABB box = new AABB(this.getX(), this.getY(), this.getZ(),
                this.getX(), this.getY(), this.getZ())
                .inflate(CultistPortalAiLogic.SUMMON_SCAN_RADIUS,
                        CultistPortalAiLogic.SUMMON_SCAN_RADIUS,
                        CultistPortalAiLogic.SUMMON_SCAN_RADIUS);                         // :215 (func_72314_b(32.0,32.0,32.0))
        // :214-216 — EntityUtils.getEntitiesInRange(…, EntityCultist.class, 32.0) filters
        // by CLASS and drops the scanner by entity id (EntityUtils.java:221-233); 1.21.1's
        // third argument is a predicate, so the id filter is the predicate below.
        return this.level().getEntitiesOfClass(EntityCultist.class, box,
                e -> e.getId() != this.getId());                                          // :216 (entityId != this.entityId)
    }

    // =============================================================== the summons

    /**
     * {@code :220-240} — {@code spawnMinions()}: one knight ({@code :223}, on
     * {@code nextFloat() > 0.33F}) or one cleric ({@code :225}), dropped at the
     * portal's position jittered ±1 on x and z ({@code :229}, {@code :231} — the
     * two {@code nextFloat()} draws subtract, so the offset is -1..1) and 0.25 above
     * its feet ({@code :230}), initialised ({@code :233}), made to play its living
     * sound ({@code :234}), homed on the portal's block with a 32-block radius
     * ({@code :235}, the {@code EntityCultist#setHomePosition} pair the cleric lane
     * landed), added to the level ({@code :236}) and set to play the wandfail track
     * ({@code :237}). Past stage 12 it then drowns the portal for
     * {@code 5 + nextInt(5)} ({@code :238-239}) — the summoner's upkeep cost.
     */
    public void spawnMinions() {
        final EntityCultist cultist;
        if (CultistPortalAiLogic.summonsKnight(this.random.nextFloat())) {                // :222
            cultist = (EntityCultist) spawnEntity(CultistPortalAiLogic.CULTIST_KNIGHT);   // :223
        } else {
            cultist = (EntityCultist) spawnEntity(CultistPortalAiLogic.CULTIST_CLERIC);   // :225
        }
        if (cultist == null) {
            return;                                                                       // BD-68's shape: an unregistered type costs the summon
        }
        cultist.setPos(this.getX() + this.random.nextFloat() - this.random.nextFloat(),   // :229
                this.getY() + CultistPortalAiLogic.MINION_Y_OFFSET,                       // :230 (+0.25)
                this.getZ() + this.random.nextFloat() - this.random.nextFloat());         // :231
        if (this.level() instanceof ServerLevel serverLevel) {                            // :233 (func_110161_a(null))
            cultist.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(this.blockPosition()),             // family seam
                    MobSpawnType.MOB_SUMMONED, null);
        }
        cultist.playAmbientSound();                                                       // :234 (func_70656_aK)
        cultist.setHomePosition(new BlockPos((int) this.getX(), (int) this.getY(),
                (int) this.getZ()), CultistPortalAiLogic.MINION_HOME_RADIUS);             // :235 (32)
        this.level().addFreshEntity(cultist);                                             // :236 (func_72838_d)
        cultist.playSound(ModSounds.WANDFAIL.get(), CultistPortalAiLogic.CONTACT_VOLUME,
                1.0F);                                                                    // :237
        if (CultistPortalAiLogic.damagesSelf(this.stage)) {                               // :238 (stage > 12)
            this.hurt(this.level().damageSources().drown(),
                    CultistPortalAiLogic.selfDamageFor(
                            this.random.nextInt(CultistPortalAiLogic.SELF_DAMAGE_ROLL))); // :239
        }
    }

    /**
     * {@code :243-255} — {@code spawnBoss()}: one {@link EntityCultistLeader} (the
     * c5-1 mob) at the same jittered, 0.25-raised position ({@code :245-249}),
     * initialised ({@code :250}), homed on the portal ({@code :251}), made to play
     * its living sound ({@code :252} — the order is the reverse of the minion arm,
     * which plays the sound BEFORE the home write), added ({@code :253}) and set to
     * play wandfail ({@code :254}). No self-damage on this arm.
     */
    public void spawnBoss() {
        // :244 — new EntityCultistLeader(world). The Praetor is NOT an EntityCultist
        // (the c5-1 base is EntityThaumcraftBoss), so this arm spawns through the
        // registry by id and the cast is the reference's own type.
        final Entity spawned = spawnEntity(CultistPortalAiLogic.CULTIST_LEADER);          // :244
        if (!(spawned instanceof EntityCultistLeader leader)) {
            return;                                                                       // BD-68's shape
        }
        leader.setPos(this.getX() + this.random.nextFloat() - this.random.nextFloat(),    // :246
                this.getY() + CultistPortalAiLogic.MINION_Y_OFFSET,                       // :247 (+0.25)
                this.getZ() + this.random.nextFloat() - this.random.nextFloat());         // :248
        if (this.level() instanceof ServerLevel serverLevel) {                            // :250
            leader.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(this.blockPosition()),             // family seam
                    MobSpawnType.MOB_SUMMONED, null);
        }
        // :251 — the Praetor's home is the portal at radius 32. 1.7.10's
        // EntityCultist#setHomePosition is not on the port's leader (its base is the
        // boss one), and the base writes the very same pair through Mob#restrictTo
        // (EntityThaumcraftBoss:119), so the seam is restrictTo.
        leader.restrictTo(new BlockPos((int) this.getX(), (int) this.getY(),
                (int) this.getZ()), CultistPortalAiLogic.MINION_HOME_RADIUS);             // :251 (32)
        leader.playAmbientSound();                                                        // :252
        this.level().addFreshEntity(leader);                                              // :253
        leader.playSound(ModSounds.WANDFAIL.get(), CultistPortalAiLogic.CONTACT_VOLUME,
                1.0F);                                                                    // :254
    }

    /**
     * The summon carrier: 1.7.10's {@code new EntityCultistKnight(world)} trio
     * becomes the registered type's factory, so the summon rides the SAME landed
     * classes the knight/cleric/leader lanes shipped and no id is invented. A type
     * missing from the registry yields null and the arm costs the summon (the BD-68
     * runtime-lookup shape) instead of a crash.
     */
    @Nullable
    private Entity spawnEntity(final net.minecraft.resources.ResourceLocation id) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;                                                                  // :222-225 — the ctor is a direct new
        }
        return BuiltInRegistries.ENTITY_TYPE.get(id).create(this.level());
    }

    // ======================================================== the banner wall

    /**
     * {@code :140-183} — the four banners. The loop walks {@code ForgeDirection}
     * 2..5 (north, south, west, east), so the cell is {@code (int)x - offsetX*6} /
     * {@code (int)z + offsetZ*6} ({@code :143}, {@code :144}): north lands at z-6, south
     * at z+6, west at x+6 (the minus on x flips the -1 offset) and east at x-6, all three
     * coordinates read as the {@code (int)} CAST of the statue's raw position — the mob
     * stands in the cell CENTRE and the cast truncates toward zero, so this origin is one
     * block HIGHER than {@code blockPosition()} on the negative half of an axis
     * (docs/testing.md#mob-write-cells-ride-the-int-cast). Each
     * cell gets {@code blockWoodenDevice} meta 8 with update flags 3, unconditionally
     * — the reference has NO pre-check on the target cell — and the tile's facing is
     * written only if the cell's tile entity is a {@code TileBanner} ({@code :153}),
     * which is also what gates the arc FX packet ({@code :170-179}) and the
     * wandfail track ({@code :180}). A cell whose chunk is not loaded is dropped
     * SILENTLY by 1.21.1's {@code Level#setBlock} (docs/testing.md#pad-write-lands-only-in-a-loaded-chunk).
     */
    public void spawnBanners() {
        final Block block = BuiltInRegistries.BLOCK.get(CultistPortalAiLogic.BLOCK_WOODEN_DEVICE);
        if (!(block instanceof BlockDeviceBase device)) {
            return;                                                                       // BD-68's shape: an unregistered target costs the leg
        }
        // :143-148 — every write cell of this arm rides the (int) CAST of the statue's OWN
        // raw position ((int)posX, (int)posY, (int)posZ). The origin is not optional: a
        // loop that computed the offsets about (0,0,0) would stamp the wall at the world
        // origin and leave the statue's own pad untouched (measured on t_2f9c86c8: the
        // probe read Block{minecraft:air} in its own wall cell on 3/3 arms while the
        // builder wrote 0,0+-6). docs/testing.md#mob-write-cells-ride-the-int-cast
        final BlockPos origin = new BlockPos((int) this.getX(),                    // :143 ((int)this.posX)
                (int) this.getY(),                                                // :143 ((int)this.posY)
                (int) this.getZ());                                               // :144 ((int)this.posZ)
        for (int a = CultistPortalAiLogic.BANNER_DIRECTION_FROM;
                a < CultistPortalAiLogic.BANNER_DIRECTION_TO; a++) {                      // :140
            final BlockPos cell = bannerCell(origin, a);                                  // :141-144
            this.level().setBlock(cell, device.withVariant(CultistPortalAiLogic.BANNER_VARIANT),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);                       // :143-148 (setBlock(block, meta 8, flags 3))
            if (this.level().getBlockEntity(cell) instanceof TileBanner banner) {         // :152-153
                banner.setFacing((byte) CultistPortalAiLogic.bannerFacing(a));            // :169
                sendBlockArc(cell);                                                       // :170-179
                this.playSound(ModSounds.WANDFAIL.get(), CultistPortalAiLogic.CONTACT_VOLUME,
                        1.0F);                                                            // :180
            }
        }
    }

    /**
     * {@code :141-144} — the banner cell for one loop index. The reference writes
     * {@code (int)x - dir.offsetX * 6} and {@code (int)z + dir.offsetZ * 6} on the
     * {@code ForgeDirection} of index {@code a} (2 north, 3 south, 4 west, 5 east),
     * with the portal's own y.
     */
    public static BlockPos bannerCell(final int directionIndex) {
        return bannerCell(new BlockPos(0, 0, 0), directionIndex);
    }

    /**
     * The same cell relative to a given origin: the WRITE arm passes the {@code (int)}
     * CAST of the statue's raw position ({@code :143-144}), and a probe passes the same
     * cast, so the claim, the census and the asserts ride ONE list
     * (docs/testing.md#mob-write-cells-ride-the-int-cast). The 1-arg overload above is the
     * unit geometry seam — it pins the offset shape at the origin, not the write site.
     */
    public static BlockPos bannerCell(final BlockPos origin, final int directionIndex) {
        final int offsetX = CultistPortalAiLogic.bannerOffsetX(directionIndex);           // :141
        final int offsetZ = CultistPortalAiLogic.bannerOffsetZ(directionIndex);           // :144
        return origin.offset(-offsetX * CultistPortalAiLogic.BANNER_OFFSET,
                0, offsetZ * CultistPortalAiLogic.BANNER_OFFSET);                         // :143, :144
    }

    // ============================================================ the loot crate

    /**
     * {@code :186-199} — one loot crate per gate. The cell is
     * {@code (int)x + nextInt(5) - nextInt(5)} on x and the same on z (two draws
     * each, so the offset is -4..4) and the portal's own y ({@code :186-187}); the
     * cell must differ from the portal's column on BOTH axes and must not be air
     * ({@code :188}). The crate's rarity is drawn from the LEVEL's random, not the
     * entity's ({@code :190}), mapped by {@code :191} to the original's metadata
     * 0/1/2 (common/uncommon/rare), which the port's crate block carries verbatim in
     * its {@code variant} property (D-001). Then the pulse status ({@code :189}), the
     * arc FX ({@code :193-197}) and the wandfail track ({@code :198}).
     */
    public void spawnCrate() {
        final int cellX = (int) this.getX() + this.random.nextInt(CultistPortalAiLogic.CRATE_SPREAD)
                - this.random.nextInt(CultistPortalAiLogic.CRATE_SPREAD);                 // :186
        final int cellZ = (int) this.getZ() + this.random.nextInt(CultistPortalAiLogic.CRATE_SPREAD)
                - this.random.nextInt(CultistPortalAiLogic.CRATE_SPREAD);                 // :187
        final BlockPos cell = new BlockPos(cellX, (int) this.getY(), cellZ);              // :192 ((int)y)
        if (!CultistPortalAiLogic.crateCellValid(cellX, (int) this.getX(), cellZ,
                (int) this.getZ(), !this.level().isEmptyBlock(cell))) {                   // :188
            return;
        }
        this.level().broadcastEntityEvent(this, CultistPortalAiLogic.STATUS_PULSE);       // :189
        final int meta = CultistPortalAiLogic.crateMetaFor(this.level().getRandom().nextFloat()); // :190-191
        final Block crate = BuiltInRegistries.BLOCK.get(CultistPortalAiLogic.BLOCK_LOOT_CRATE);
        if (!(crate instanceof BlockLoot loot)) {
            return;                                                                       // BD-68's shape
        }
        this.level().setBlock(cell, loot.defaultBlockState().setValue(BlockLoot.VARIANT, meta),
                Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);                           // :192 (setBlock(block, md, 3))
        sendBlockArc(cell);                                                               // :193-197
        this.playSound(ModSounds.WANDFAIL.get(), CultistPortalAiLogic.CONTACT_VOLUME,
                1.0F);                                                                    // :198
    }

    /**
     * {@code :170-179}, {@code :193-197} — the arc FX broadcast:
     * {@code PacketHandler.sendToAllAround(new PacketFXBlockArc(x, y, z, entityId),
     * new TargetPoint(dimension, x, y, z, 32.0))}. The payload is the landed
     * {@link FXBlockArcPayload} (the arc particle family landed with
     * {@code PacketFXBlockArc}) and the broadcast rides the tracking players of the
     * target chunk — 1.21.1's twin of "everyone near this block" (the infusion
     * matrix's precedent, {@code TileInfusionMatrix.java:316-319}). Deviation,
     * recorded: the reference's {@code TargetPoint} radius is 32 blocks, the chunk
     * tracking reach is 64, so the FX can reach clients the original never reached.
     */
    private void sendBlockArc(final BlockPos cell) {
        if (this.level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(cell),
                    new FXBlockArcPayload(cell, this.getId()));
        }
    }

    // ================================================================ touch, die

    /**
     * {@code :257-260} — {@code func_70100_b_(EntityPlayer)} (collideWithPlayer):
     * inside squared distance 3.0, the PLAYER takes 8.0 mob-attack damage, and the
     * zap track plays only on the arm of a landed hit ({@code :259}), with the
     * original's jittered pitch {@code (rand - rand) * 0.1F + 1.0F}. 1.21.1's seam
     * is {@link Entity#playerTouch(Player)} — the landed resolution of the same
     * 1.7.10 hook ({@code EntityThaumicSlime.java:410-416}).
     */
    @Override
    public void playerTouch(final Player player) {
        if (CultistPortalAiLogic.contactHits(this.distanceToSqr(player))) {                // :258
            if (player.hurt(this.level().damageSources().mobAttack(this),
                    CultistPortalAiLogic.CONTACT_DAMAGE)) {                               // :258 (causeMobDamage, 8.0F)
                this.playSound(ModSounds.ZAP.get(), CultistPortalAiLogic.CONTACT_VOLUME,
                        CultistPortalAiLogic.contactPitchFor(this.random.nextFloat(),
                                this.random.nextFloat()));                                // :259
            }
        }
    }

    /**
     * {@code :307-313} — {@code func_70645_a} (onDeathUpdate): on the server the
     * world gets {@code newExplosion(this, x, y, z, 2.0F, false, false)} (no fire,
     * no block breaking, entity damage on) and THEN the parent's death runs
     * ({@code :312}), so the explosion is the portal's, not the drops'. 1.21.1's
     * seam is {@code LivingEntity#die(DamageSource)};
     * {@code Level#explode(source, x, y, z, radius, fire, interaction)} with
     * {@code ExplosionInteraction.NONE} is the exact twin of the
     * {@code (isFlaming=false, isBreaking=false)} pair.
     */
    @Override
    public void die(final DamageSource source) {
        if (!this.level().isClientSide) {                                                 // :308
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    CultistPortalAiLogic.DEATH_EXPLOSION_POWER,
                    CultistPortalAiLogic.DEATH_EXPLOSION_FIRE,
                    Level.ExplosionInteraction.NONE);                                     // :309 (2.0F, false, false)
        }
        super.die(source);                                                                // :312
    }

    /**
     * {@code :287-289} — {@code func_70628_a} (entityDropAllLoot): the ONLY custom
     * drop is eldritch object meta 3, thrown by
     * {@code EntityUtils.entityDropSpecialItem(this, stack, field_70131_O / 2.0F)} —
     * i.e. at {@code y + maxHealth/2} = 250 blocks up ({@code EntityUtils.java:356-358}:
     * dead centre in x/z, motionY 0.1, a 10-tick pickup delay). 1.21.1 has no
     * {@code EntitySpecialItem} (unported) so the drop rides
     * {@code Mob#spawnAtLocation(ItemStack, float)}, which keeps the 10-tick delay
     * ({@code setDefaultPickUpDelay}) and the lift, and jitters x/z by the offset —
     * recorded as a deviation (D-137). The item is registered
     * ({@code CatalogEldritchRealm.java:196}), so the gate is BD-68's shape, not a
     * stand-in.
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        if (BuiltInRegistries.ITEM.containsKey(CultistPortalAiLogic.ITEM_ELDRITCH_OBJECT)) { // :288
            final ItemStack object = new ItemStack(
                    BuiltInRegistries.ITEM.get(CultistPortalAiLogic.ITEM_ELDRITCH_OBJECT));
            object.setDamageValue(CultistPortalAiLogic.ELDRITCH_OBJECT_META);             // :288 (meta 3)
            this.spawnAtLocation(object,
                    (float) CultistPortalAiLogic.dropLiftFor(this.getMaxHealth()));       // :288 (field_70131_O / 2.0F)
        }
    }

    // ============================================================ pinned figures

    /** {@code :263-265} — {@code func_70599_aP() = 0.75F} (getSoundVolume). */
    @Override
    protected float getSoundVolume() {
        return CultistPortalAiLogic.SOUND_VOLUME;
    }

    /**
     * {@code :267-269} — {@code func_70627_aG() = 540} (getMaxAir), the cleric
     * lane's shape ({@code EntityCultistCleric:443-445}).
     */
    @Override
    public int getMaxAirSupply() {
        return CultistPortalAiLogic.MAX_AIR;
    }

    /** {@code :271-273} — {@code func_70639_aQ() = "thaumcraft:monolith"}. */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.MONOLITH.get();
    }

    /** {@code :275-277} — {@code func_70621_aR() = "thaumcraft:zap"}. */
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.ZAP.get();
    }

    /** {@code :279-281} — {@code func_70673_aS() = "thaumcraft:shock"}. */
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.SHOCK.get();
    }

    /** {@code :72-74} — {@code func_70067_L() = true} (canBeCollidedWith). */
    @Override
    public boolean canBeCollidedWith() {
        return CultistPortalAiLogic.CAN_BE_COLLIDED_WITH;
    }

    /** {@code :76-78} — {@code func_70104_M() = false} (isPushable). */
    @Override
    public boolean isPushable() {
        return CultistPortalAiLogic.PUSHABLE;
    }

    /**
     * {@code :80-81} — {@code func_70091_d(dx, dy, dz)} is an EMPTY override: the
     * portal never moves, not by gravity, not by a push, not by a path. The taint
     * family resolved the same override to {@code travel(Vec3)}
     * ({@code EntityTaintacle.java:50-52}); the family's arms zero the axes and keep
     * super, and this one calls nothing — a faithful empty body. With no goals and no
     * travel, the position is fixed for the entity's whole life.
     */
    @Override
    public void travel(final Vec3 travelVector) {
        // :80-81 — deliberately empty. No super: the portal has no motion at all.
    }

    /**
     * {@code :83-84} — {@code func_70626_be()} (updateEntityActionState) is an EMPTY
     * override, the taint-swarm card's resolution of the same method
     * ({@code EntityTaintSwarm.java:54}). 1.21.1's twin is
     * {@code Mob#customServerAiStep}; the reference's empty body is why the portal
     * has no goal ticking, no vanilla look/idle bookkeeping and no target re-read.
     * The empty body is the load-bearing fact of the mob, so the probe asserts it.
     */
    @Override
    protected void customServerAiStep() {
        // :83-84 — deliberately empty. No super.
    }

    /**
     * {@code :301-302} — {@code func_70690_d(PotionEffect)} (onPotionEffectApplied) is
     * an EMPTY override, and it is a VISUAL hook: 1.7.10's
     * {@code EntityLiving#onPotionEffectApplied} draws the purple swirl when an effect
     * is applied, which is the method the reference empties. It is NOT a potion
     * immunity — the portal's only immunity is fire ({@code :38}).
     *
     * <p>21.1.259 has no counterpart: 1.21.1 draws effect particles from the effect
     * map inside {@code LivingEntity}, the write path
     * ({@code LivingEntity#addEffect(MobEffectInstance)}) is {@code public final}
     * (the crab card's finding, {@code EldritchCrabAiLogic:407-411}) and 1.21.1 has no
     * applicability gate to override. So the port ships NO override here and pins the
     * fact as data ({@link CultistPortalAiLogic#DRAWS_EFFECT_PARTICLES}) — the
     * deviation is that a portal the reference would have kept particle-free draws the
     * swirl if anything ever applies an effect to it. Nothing does: the statue has no
     * goals, no movement and no weapon, and the band's own effects target kin.</p>
     */
    public boolean drawsEffectParticles() {
        return CultistPortalAiLogic.DRAWS_EFFECT_PARTICLES;
    }

    /**
     * {@code :304-305} — {@code func_70069_a(float)} is an EMPTY override: the
     * portal is silent underfoot. The firebat card resolved the same override to
     * {@code playStepSound} ({@code EntityFireBat.java:592-597}).
     */
    @Override
    protected void playStepSound(final BlockPos pos, final BlockState state) {
        // :304-305 — deliberately silent.
    }

    /**
     * {@code :292-299} — the CLIENT half of {@code func_70103_a}
     * (handleStatusEvent): status 16 sets the pulse to 10 and plays the living
     * sound; every other byte goes to the parent. 1.21.1 renamed the receiver to
     * {@code Entity#handleEntityEvent(byte)} ({@code EntityPech.java:672-677}). The
     * body is split into {@link #onPulseStatus(byte)} so the game-test server can
     * drive the client half directly (docs/deviations rule: a client half of a
     * server tick must be an extracted method).
     */
    @Override
    public void handleEntityEvent(final byte id) {
        if (onPulseStatus(id)) {                                                          // :293-295
            return;
        }
        super.handleEntityEvent(id);                                                      // :297
    }

    /**
     * {@code :293-295} — the pulse arm of the status handler: on byte 16 the pulse
     * counter goes to 10 and the ambient track plays. Returns whether the byte was
     * the portal's own, so the caller can forward the rest.
     */
    public boolean onPulseStatus(final byte id) {
        if (id != CultistPortalAiLogic.STATUS_PULSE) {
            return false;                                                                 // :296-297
        }
        this.pulse = CultistPortalAiLogic.PULSE_TICKS;                                    // :294
        this.playAmbientSound();                                                          // :295
        return true;
    }

    // ==================================================================== data

    /**
     * {@code :51-54} — the ONLY key written: {@code tag.putInt("stage", stage)}. The
     * countdown ({@code :33}) and the pulse ({@code :34}) are NOT saved, so a
     * loaded portal keeps its stage and restarts its countdown at 200.
     */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(CultistPortalAiLogic.TAG_STAGE, this.stage);                           // :53
    }

    /** {@code :56-59} — {@code this.stage = tag.getInt("stage")}, no default. */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stage = tag.getInt(CultistPortalAiLogic.TAG_STAGE);                          // :58
    }

    // =================================================================== seams

    /** {@code :32} — the stage; the machine's only saved state. */
    public int getStage() {
        return this.stage;
    }

    /** {@code :115}, {@code :125}, {@code :131} — the stage writes. Public for the probe's setup. */
    public void setStage(final int stage) {
        this.stage = stage;
    }

    /** {@code :33} — the countdown. */
    public int getStageCounter() {
        return this.stageCounter;
    }

    /** {@code :111}, {@code :123}, {@code :127}, {@code :133}, {@code :136} — the countdown writes. */
    public void setStageCounter(final int stageCounter) {
        this.stageCounter = stageCounter;
    }

    /** {@code :34} — the client-side pulse countdown, the boss band's shared seam. */
    public int getPulse() {
        return this.pulse;
    }

    /** {@code :43-45} — the armour figure, exposed as the reference's getter shape. */
    public int getArmorValue() {
        return (int) CultistPortalAiLogic.ARMOR;
    }

    /** The boss-bar handle, exposed for the game-test card (the giant's shape). */
    public ServerBossEvent getBossEvent() {
        return this.bossEvent;
    }

    /** {@code :31} — show the bar to every player tracking the statue. */
    @Override
    public void startSeenByPlayer(final ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    /** {@code :31} — and take it away when they stop tracking it. */
    @Override
    public void stopSeenByPlayer(final ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /**
     * {@code :36-41} — the constructor ran {@code onInitialSpawn} for the mob in
     * 1.7.10's spawn path; the portal sets no gear, no group data and no flags
     * there, so the reference has no override to carry and 1.21.1's inherited
     * {@code Mob#finalizeSpawn} (difficulty scaling, the patroller/raid branches)
     * runs as it does for every other mob in the band. Pinned as data (D-137); the
     * summon arms call it explicitly at {@code :233}/{@code :250}.
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            @Nullable final SpawnGroupData data) {
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

}
