package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EldritchCrabAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchCrab;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import com.jonnyutah12.thaumcraftclassic.network.payload.BiomeChangePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.minecraft.world.level.GameType;

import java.util.UUID;
import io.netty.channel.Channel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World probes for the Eldritch Crab and its orb (TC-M6-06-s-c4b-1,
 * content-mapping {@code Thaumcraft:EldritchCrab} (CSV line 330) mod_index 32,
 * registration {@code ConfigEntities.java:121}, and {@code Thaumcraft:EldritchOrb}
 * (CSV line 309) mod_index 11, registration {@code ConfigEntities.java:87}).
 *
 * <p>Every figure reaches the mob through the live registry and the vanilla
 * {@code Mob} / {@code LivingEntity} / {@code Projectile} API, so a probe goes
 * RED on behaviour rather than on a missing symbol. Each literal is the
 * original's own and quotes its decompiled line in
 * {@code thaumcraft.common.entities.monster.EntityEldritchCrab} (215 lines) and
 * {@code thaumcraft.common.entities.projectile.EntityEldritchOrb} (92 lines);
 * the world-free rules of the same lines are pinned by
 * {@code EldritchCrabAiLogicTest} and {@code EntityEldritchOrbTest}.</p>
 *
 * <p>The pad fixtures follow the family's findings: an "empty" template has no
 * floor, so every spawned mob rides {@code setNoGravity(true)} +
 * {@code clearFire()} (the pech card), and a mock player is
 * {@code setPos}-placed at the mob's feet because {@code makeMockPlayer} does
 * not place it and {@code TargetGoal#canAttack} carries FOLLOW_RANGE (the
 * knight card).</p>
 *
 * <p><b>Isolation audit of the sibling probes (card t_bde0b35c).</b> The warp
 * lane's {@code sweepPad} pattern lands here with the hazard set tuned to this
 * file: both entity-scan probes (the mid-air climb, the orb burst) sweep the
 * pad's census box at setup, and the climb probe registers a player, so its
 * sweep includes players. Two probes went red-hands on their own hazards at the
 * full 502-probe grid, 1 leg in 11 each: the climb probe under a registered
 * player resident at its coordinates (the P2 plant reproduces the disclosed CI
 * line byte-for-byte; mid-air spiders P1 and a roaming unshelled crab P3 do
 * not), and the orb burst probe on a CLEAN pad with the consumption pinned to a
 * 2-real-tick budget (age=100 removed=false — the docs/testing.md tick-budget
 * class, this card's diag#7). A third finding rides with the climb fix: that
 * probe's teardown was tail-only, so the leg that went red leaked its registered
 * player (the harness's box cleanup covers entities, not the players list) and
 * manufactured the very resident that reddens the next pad — the teardown is now
 * a {@code finally}. The remaining probes are box-scoped (block/health/stack/
 * copy asserts) and read nothing a player scan can hand to a resident; the ride
 * probe's gate is ground-based (no player scan), so a resident mob there is the
 * same class the c4b-1 lane documented as accepted. No probe's assertion was
 * weakened.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EldritchCrabGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("EldritchCrabGameTest");

    private EldritchCrabGameTest() {
    }

    // ------------------------------------------------------------------ fixtures

    private static EntityType<?> registered(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ")");
        return type;
    }

    private static void absent(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(!id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " must NOT be registered but its key resolves to "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    private static BlockPos pad(final GameTestHelper helper) {
        return helper.absolutePos(BlockPos.ZERO).offset(1, 3, 1);
    }

    /**
     * The pad's census box — the template footprint plus the margin the warp lane
     * uses ({@code WarpEventsGameTest.padEntities}: ±1 in x/z, the pad height +3
     * in y). A census, not a claim: the harness reuses one grid area across
     * batches and clears only BLOCK space, so whatever a previous batch's pad
     * left standing in this box is what this probe measures through the world.
     */
    private static List<Entity> padEntities(final GameTestHelper helper, final ServerLevel level,
                                            final Predicate<Entity> filter) {
        final BlockPos o = helper.absolutePos(BlockPos.ZERO);
        return level.getEntitiesOfClass(Entity.class,
                new AABB(o.getX() - 2, o.getY() - 4, o.getZ() - 2,
                        o.getX() + 13, o.getY() + 15, o.getZ() + 13),
                filter);
    }

    /**
     * Sweep the pad at probe setup — the warp lane's isolation pattern
     * ({@code WarpEventsGameTest.sweepPad}) with ONE deliberate difference. The
     * warp sweep discards non-player entities and LEAVES players, because the
     * warp event's scan is a BLOCK scan and no player rejects a cell. Every gate
     * this file measures scans ENTITIES: {@code EntityEldritchCrab#tick} asks
     * {@code level.getNearestPlayer(this, -1.0)} — distance-UNBOUNDED, target
     * list {@code level.players()} — for its mount, and {@code EntityEldritchOrb}
     * sweeps the living in its burst box. A player resident is therefore IN this
     * lane's hazard set, not out of it: a registered player left at the pad's own
     * coordinates (the grid re-gives a pad the coordinates of the pad that leaked
     * it — {@code StructureGridSpawner#onBatchStart} rewinds to the batch corner)
     * takes the mount, and the probe measures the resident instead of the port.
     * Measured on this tree: a planted co-located registered {@code ServerPlayer}
     * reddens the climb probe with the disclosed CI line byte-for-byte (P2),
     * mid-air mind spiders (P1) and a roaming unshelled crab (P3) do not.
     *
     * <p>The box is provably large enough for the gate under test: the mount
     * needs {@code distanceSquared < 4.0} ({@link EldritchCrabAiLogic#canMountPlayer})
     * and the scan keeps the strict minimum, so a resident the sweep does not
     * reach (≥ 13 blocks out) can neither win the scan nor satisfy the bound.
     * Player residents are deregistered from the scan set
     * ({@code level.players()}) and discarded; every other resident is
     * discarded.</p>
     */
    private static void sweepPad(final GameTestHelper helper, final ServerLevel level) {
        for (final Entity stray : padEntities(helper, level, e -> true)) {
            if (stray instanceof ServerPlayer resident) {
                level.players().remove(resident);
            }
            stray.discard();
        }
    }

    private static EntityEldritchCrab crab(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "eldritchcrab").create(level);
        helper.assertTrue(spawned instanceof EntityEldritchCrab,
                "thaumcraft:eldritchcrab did not create an EntityEldritchCrab but "
                        + (spawned == null ? "null" : spawned.getClass().getName()));
        final EntityEldritchCrab mob = (EntityEldritchCrab) spawned;
        final BlockPos at = pad(helper);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    private static EntityEldritchOrb orb(final GameTestHelper helper, final Vec3 at) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "eldritchorb").create(level);
        helper.assertTrue(spawned instanceof EntityEldritchOrb,
                "thaumcraft:eldritchorb did not create an EntityEldritchOrb but "
                        + (spawned == null ? "null" : spawned.getClass().getName()));
        final EntityEldritchOrb projectile = (EntityEldritchOrb) spawned;
        projectile.setPos(at.x, at.y, at.z);
        level.addFreshEntity(projectile);
        return projectile;
    }

    private static List<WrappedGoal> goals(final Mob mob, final boolean target) {
        final List<WrappedGoal> out = new ArrayList<>();
        for (final WrappedGoal g : (target ? mob.targetSelector : mob.goalSelector).getAvailableGoals()) {
            out.add(g);
        }
        return out;
    }

    private static boolean has(final Iterable<WrappedGoal> goals, final Class<?> goal, final int slot) {
        for (final WrappedGoal wrapped : goals) {
            if (goal.isInstance(wrapped.getGoal()) && wrapped.getPriority() == slot) {
                return true;
            }
        }
        return false;
    }

    private static String describe(final Iterable<WrappedGoal> goals) {
        final StringBuilder sb = new StringBuilder("goal set: ");
        for (final WrappedGoal g : goals) {
            sb.append(g.getPriority()).append('=').append(g.getGoal().getClass().getSimpleName()).append(' ');
        }
        return sb.toString();
    }

    private static DifficultyInstance difficulty(final ServerLevel level, final Difficulty mode) {
        // DifficultyInstance#isHard() reads the EFFECTIVE difficulty —
        // id * (0.75 + f1(levelTime) + f2(inhabited)) — not the base mode. A fresh-time
        // HARD instance is 3 * 0.75 = 2.25 and NOT hard (vanilla's day-1 behaviour, and
        // the crab's shell roll rides isHard): the guaranteed leg needs dayTime 1512000
        // (f1 = 0.25 -> 3 * 1.0 = 3.0). The band legs ride dayTime 0, where EASY 0.75,
        // NORMAL 1.5 and PEACEFUL 0 all stay under 3.0. (Measured: the drafted
        // (mode, dayTime, 0, 0) shape made HARD roll, 18/60, and the fix flips it to
        // the guaranteed branch.)
        return new DifficultyInstance(mode, mode == Difficulty.HARD ? 1512000L : 0L, 0L, 0.0F);
    }

    // ------------------------------------------------------------------ roster

    /**
     * {@code ConfigEntities.java:121} — {@code registerModEntity(EntityEldritchCrab,
     * "EldritchCrab", id++, instance, 64, 3, true)}: the box is the original's
     * {@code func_70105_a(0.8F, 0.6F)} (:33), the tracking range 64 blocks = 4
     * chunks, the update interval 3. {@code ConfigEntities.java:87} — the orb's
     * own triple 64 / 20 and its {@code func_70105_a(0.25F, 0.25F)}.
     */
    @GameTest(template = "empty")
    public static void thePairIsRegisteredWithTheOriginalBoxesAndTracking(final GameTestHelper helper) {
        final EntityType<?> crab = registered(helper, "eldritchcrab");
        helper.assertTrue(Math.abs(crab.getDimensions().width() - EldritchCrabAiLogic.WIDTH) < 1.0E-4F
                        && Math.abs(crab.getDimensions().height() - EldritchCrabAiLogic.HEIGHT) < 1.0E-4F,
                ":33 — the crab's hitbox must be 0.8 x 0.6, got "
                        + crab.getDimensions().width() + " x " + crab.getDimensions().height());
        helper.assertTrue(crab.getCategory() == MobCategory.MONSTER,
                "the crab is not a MONSTER-category mob but " + crab.getCategory());
        helper.assertTrue(crab.clientTrackingRange() == EldritchCrabAiLogic.TRACK_RANGE_CHUNKS
                        && crab.updateInterval() == EldritchCrabAiLogic.UPDATE_INTERVAL,
                ":121 — the crab's tracking triple must be 64 blocks (4 chunks) / 3 ticks, got "
                        + crab.clientTrackingRange() + " chunks / " + crab.updateInterval() + " ticks");
        helper.assertTrue(CatalogEldritchCrab.ELDRITCH_CRAB.get() == crab,
                "the catalog's ELDRITCH_CRAB holder is not the registered type");
        helper.assertTrue(EntityEldritchCrab.TYPE_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(crab)),
                "the class's TYPE_ID must name the registered id, got "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(crab));

        final EntityType<?> orb = registered(helper, "eldritchorb");
        helper.assertTrue(Math.abs(orb.getDimensions().width() - EntityEldritchOrb.WIDTH) < 1.0E-4F
                        && Math.abs(orb.getDimensions().height() - EntityEldritchOrb.HEIGHT) < 1.0E-4F,
                "the orb's hitbox must be 0.25 x 0.25, got "
                        + orb.getDimensions().width() + " x " + orb.getDimensions().height());
        helper.assertTrue(orb.getCategory() == MobCategory.MISC,
                "the orb is a projectile: MISC category, got " + orb.getCategory());
        helper.assertTrue(orb.clientTrackingRange() == EntityEldritchOrb.TRACK_RANGE_CHUNKS
                        && orb.updateInterval() == EntityEldritchOrb.UPDATE_INTERVAL,
                ":87 — the orb's triple must be 64 blocks (4 chunks) / 20 ticks, got "
                        + orb.clientTrackingRange() + " chunks / " + orb.updateInterval() + " ticks");
        helper.assertTrue(EntityEldritchOrb.FIRE_IMMUNE == false
                        && EntityEldritchCrab.FIRE_IMMUNE == false,
                "neither the crab's 215 lines nor the orb's 92 set isImmuneToFire, and no builder"
                        + " calls EntityType.Builder#fireImmune");
        // The crab has no spawn-egg row in the content mapping, so no egg item.
        helper.assertTrue(!CatalogEldritchCrab.HAS_SPAWN_EGG_ROW
                        && !BuiltInRegistries.ITEM.containsKey(ResourceLocation
                                .fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "spawnegg.eldritchcrab")),
                "no egg row means no invented egg item");
        helper.succeed();
    }

    /** :37-44 — the goal set, slot for slot, with the two target scans. */
    @GameTest(template = "empty")
    public static void theCrabGoalSetIsTheOriginalsSlotsForSlot(final GameTestHelper helper) {
        final EntityEldritchCrab crab = crab(helper);
        final List<WrappedGoal> movement = goals(crab, false);
        final List<WrappedGoal> targets = goals(crab, true);
        helper.assertTrue(has(movement, FloatGoal.class, EldritchCrabAiLogic.GOAL_SLOT_SWIM),
                ":37 — Swimming belongs at slot 0; " + describe(movement));
        helper.assertTrue(has(movement, LeapAtTargetGoal.class, EldritchCrabAiLogic.GOAL_SLOT_LEAP),
                ":38 — LeapAtTarget(0.63F) belongs at slot 2; " + describe(movement));
        helper.assertTrue(has(movement, MeleeAttackGoal.class, EldritchCrabAiLogic.GOAL_SLOT_MELEE),
                ":39 — the melee punch (AIAttackOnCollide's mapping) belongs at slot 3; "
                        + describe(movement));
        helper.assertTrue(has(movement, RandomStrollGoal.class, EldritchCrabAiLogic.GOAL_SLOT_WANDER),
                ":40 — Wander(0.8) belongs at slot 7; " + describe(movement));
        helper.assertTrue(has(movement, RandomLookAroundGoal.class,
                        EldritchCrabAiLogic.GOAL_SLOT_LOOK_IDLE),
                ":41 — LookIdle belongs at slot 8; " + describe(movement));
        helper.assertTrue(movement.size() == 5,
                ":37-41 — five movement goals, no WatchClosest (the original adds none) and no"
                        + " door / restriction goal; " + describe(movement));
        helper.assertTrue(has(targets, HurtByTargetGoal.class, EldritchCrabAiLogic.TARGET_SLOT_HURT),
                ":42 — HurtByTarget(true) belongs at target slot 1; " + describe(targets));
        helper.assertTrue(has(targets, NearestAttackableTargetGoal.class,
                        EldritchCrabAiLogic.TARGET_SLOT_PLAYER),
                ":43 — the player scan belongs at target slot 2; " + describe(targets));
        helper.assertTrue(has(targets, NearestAttackableTargetGoal.class,
                        EldritchCrabAiLogic.TARGET_SLOT_CULTIST),
                ":44 — the EntityCultist scan belongs at target slot 3; " + describe(targets));
        helper.assertTrue(targets.size() == 2 || targets.size() == 3,
                ":42-44 — three target goals (the two scans + the hurt call); " + describe(targets));
        // The two scans' target classes (:43 EntityPlayer, :44 EntityCultist).
        final List<Class<?>> scanned = new ArrayList<>();
        for (final WrappedGoal wrapped : targets) {
            if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> scan) {
                scanned.add(EldritchCrabGameTest.targetClassOf(scan));
            }
        }
        helper.assertTrue(scanned.contains(EldritchCrabAiLogic.TARGET_CLASS_PLAYER)
                        && scanned.contains(EldritchCrabAiLogic.TARGET_CLASS_CULTIST),
                ":43-44 — the scans must target the player and the cultist base (the abstract"
                        + " base matches the knight and the cleric), got " + scanned);
        helper.assertTrue(EntityCultist.class.isAssignableFrom(EldritchCrabAiLogic.TARGET_CLASS_CULTIST),
                "the cultist scan must ride the abstract base, not a leaf class");
        crab.discard();
        helper.succeed();
    }

    /** {@code NearestAttackableTargetGoal}'s target class is a private field. */
    private static Class<?> targetClassOf(final NearestAttackableTargetGoal<?> goal) {
        try {
            final java.lang.reflect.Field field =
                    // javap on 21.1.259: targetType is on NearestAttackableTargetGoal
            // (`protected final Class<T> targetType`), not on TargetGoal.
            NearestAttackableTargetGoal.class.getDeclaredField("targetType");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final Class<? extends LivingEntity> type = (Class<? extends LivingEntity>) field.get(goal);
            return type;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------ live figures

    /** :51-56, :67-69, :184-186 — health 20, attack 4, speed 0.3, armour 0,
     *  160 ticks of air, and the crab's own 6 xp. */
    @GameTest(template = "empty")
    public static void theLiveCrabCarriesTheOriginalAttributesAirAndXp(final GameTestHelper helper) {
        final EntityEldritchCrab crab = crab(helper);
        helper.assertTrue(close(crab.getMaxHealth(), EldritchCrabAiLogic.MAX_HEALTH),
                ":52 — max health 20, got " + crab.getMaxHealth());
        helper.assertTrue(close(crab.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),
                        EldritchCrabAiLogic.ATTACK_DAMAGE),
                ":54 — attack damage 4, got "
                        + crab.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        helper.assertTrue(close(crab.speedBaseForProbe(), EldritchCrabAiLogic.SPEED_NO_SHELL),
                ":55 — the builder's speed is 0.3, got " + crab.speedBaseForProbe());
        helper.assertTrue(close(crab.armorBaseForProbe(), EldritchCrabAiLogic.ARMOR_NONE),
                ":67-69 — an unshelled crab carries armour 0, got " + crab.armorBaseForProbe());
        helper.assertTrue(crab.getMaxAirSupply() == EldritchCrabAiLogic.MAX_AIR
                        && crab.getAirSupply() == EldritchCrabAiLogic.MAX_AIR,
                ":184-186 — 160 ticks of air, got " + crab.getMaxAirSupply() + " / "
                        + crab.getAirSupply());
        // :34 — field_70728_aV = 6. 21.1.259's entry point is the final
        // getExperienceReward(ServerLevel, Entity) (the pech card's probe shape).
        helper.assertTrue(crab.getExperienceReward(helper.getLevel(), null) == EldritchCrabAiLogic.XP,
                ":34 — 6 xp, got " + crab.getExperienceReward(helper.getLevel(), null));
        crab.discard();
        helper.succeed();
    }

    private static boolean close(final double a, final double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    /** :58-61, :99-112 — the one synched byte: bit 0 is the shell, it rides the
     *  "Flags" NBT key, and flipping it rewrites armour 5 and speed 0.275. */
    @GameTest(template = "empty")
    public static void theShellByteDrivesArmorSpeedAndSurvivesSave(final GameTestHelper helper) {
        final EntityEldritchCrab crab = crab(helper);
        helper.assertTrue(!crab.hasShell() && crab.shellFlagsForProbe() == 0,
                ":101-112 — a fresh crab carries Flags 0");
        crab.setShell(true);
        helper.assertTrue(crab.hasShell() && crab.shellFlagsForProbe() == 1,
                ":101-112 — the shell bit is bit 0, got " + crab.shellFlagsForProbe());
        helper.assertTrue(close(crab.armorBaseForProbe(), EldritchCrabAiLogic.ARMOR_SHELL)
                        && close(crab.speedBaseForProbe(), EldritchCrabAiLogic.SPEED_SHELL),
                ":78-84 / :160-169 — a shelled crab carries armour 5 and speed 0.275, got "
                        + crab.armorBaseForProbe() + " / " + crab.speedBaseForProbe());
        final CompoundTag tag = new CompoundTag();
        crab.addAdditionalSaveData(tag);
        helper.assertTrue(tag.getByte(EldritchCrabAiLogic.NBT_FLAGS) == 1,
                ":99-112 — the write pass puts Flags=1 in the tag, got "
                        + tag.getAllKeys());
        final EntityEldritchCrab loaded = crab(helper);
        loaded.readAdditionalSaveData(tag);
        helper.assertTrue(loaded.hasShell(),
                ":99-112 — a saved shelled crab loads shelled");
        helper.assertTrue(close(loaded.armorBaseForProbe(), EldritchCrabAiLogic.ARMOR_SHELL),
                ":99-112 — the read pass rewrites armour 5, got " + loaded.armorBaseForProbe());
        crab.setShell(false);
        helper.assertTrue(!crab.hasShell()
                        && close(crab.armorBaseForProbe(), EldritchCrabAiLogic.ARMOR_NONE)
                        && close(crab.speedBaseForProbe(), EldritchCrabAiLogic.SPEED_NO_SHELL),
                ":160-169 — clearing the shell returns armour 0 and speed 0.3");
        crab.discard();
        loaded.discard();
        helper.succeed();
    }

    /**
     * :72-76 — the helm roll. The original's two branches are
     * {@code field_70170_p.field_73013_u == HARD → setHelm(true)} ({@code :72-73},
     * the guaranteed branch) and {@code field_70146_Z.nextFloat() < 0.33F}
     * ({@code :75}, the entity random) for every other difficulty: HARD always
     * shells and the rest wear it about a third of the time. Both legs are
     * measured on the live {@code finalizeSpawn}.
     *
     * <p>The rate arm follows the repo's band rule (docs/testing.md: mean ± 4 sd
     * of the BINOMIAL SUM, widened outward to the quantum, never the only arm).
     * The shipped band {@code 9..31} was ±3.0 sd of the 60-draw
     * {@code Binom(60, 0.33)} (mean 19.8, sd 3.64) — a nominal 1.37e-3 per-leg
     * trip — and the shared-stream seeding (every crab's fresh mob
     * {@code RandomSource} is seeded off the same LCG, docs/testing.md's
     * correlation warning) inflates the effective sd: it measured 1 red in 17
     * legs at tip a29bbc06 (the red leg drew 8, a 3.24-sd tail). The band here
     * is 19.8 ± 4·3.64 widened outward → {@code 5..35}: an exact 1.75e-5 trip on
     * the nominal sum (1 leg in 57 268), so the ~3× effective-sd inflation the
     * 1/17 rate implies is inside it with margin. The deterministic pair: the
     * HARD arm (60/60) and the OUTCOME SET of the 60 non-HARD draws must be
     * exactly {shelled, unshelled} — the whole support [0, 60] of a 1/3 roll
     * contains both outcomes, and a roll rule collapsed to one outcome (always
     * shells, never shells) reddens that arm on 100 % of legs.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void hardSpawnsAlwaysShellAndTheRestAreAboutAThird(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        int shelled = 0;
        for (int i = 0; i < 60; i++) {
            final EntityEldritchCrab crab = crab(helper);
            crab.finalizeSpawn(level, difficulty(level, Difficulty.HARD), MobSpawnType.NATURAL, null);
            if (crab.hasShell()) {
                shelled++;
            }
            crab.discard();
        }
        helper.assertTrue(shelled == 60,
                ":72-73 — HARD is the guaranteed branch (setHelm(true)), got " + shelled + "/60");
        int rolled = 0;
        boolean sawShelled = false;
        boolean sawBare = false;
        for (final Difficulty mode : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.PEACEFUL)) {
            for (int i = 0; i < 20; i++) {
                final EntityEldritchCrab crab = crab(helper);
                crab.finalizeSpawn(level, difficulty(level, mode), MobSpawnType.NATURAL, null);
                if (crab.hasShell()) {
                    rolled++;
                    sawShelled = true;
                } else {
                    sawBare = true;
                }
                crab.discard();
            }
        }
        // :74-75 — the outcome set, deterministic: 60 draws of a 1/3 roll have
        // the whole support [0, 60] and both outcomes live inside it, so the
        // observed set must carry SHELL and NO_SHELL. A rule that made the shell
        // HARD-only — the shape this probe first predicted — scores 0/60 and
        // reddens this arm, like the band arm, on every leg.
        helper.assertTrue(sawShelled && sawBare,
                ":74-75 — the 60 non-HARD draws must show BOTH shell outcomes (the support of a"
                        + " 1/3 roll over 60 draws is [0, 60]); observed set = {"
                        + (sawShelled ? "shelled" : "") + (sawShelled && sawBare ? ", " : "")
                        + (sawBare ? "unshelled" : "") + "} at " + rolled + "/60 — a one-outcome"
                        + " set is a collapsed roll rule, not a draw");
        // :74-75 — the rate arm: 60 flips of p=1/3 (quantum 1) is a binomial
        // with mean 19.8 and sd 3.64, so the band is mean ± 4 sd widened outward
        // to the quantum = [5, 35] over the whole support [0, 60]; exact trip on
        // the nominal sum 1.75e-5 (the old ±3.0 sd [9,31] was 1.37e-3 and
        // measured 1/17 legs red on the correlated streams at a29bbc06). A count
        // this far out is a removed, added or re-probability'd roll body, not a
        // draw.
        helper.assertTrue(rolled >= 5 && rolled <= 35,
                ":74-75 — 0.33 of the 60 non-HARD spawns wear a shell; measured " + rolled
                        + " against mean 19.8, sd 3.64, 4 sd widened to quantum 1 = [5, 35] over"
                        + " the support [0, 60] (exact 1.75e-5 trip): a total this far out is a"
                        + " changed roll body, not a draw");
        helper.succeed();
    }

    /** :78-92 — the spawn-data carrier: the picked effect is cast with the
     *  unbounded duration, and marker 0 casts nothing. */
    @GameTest(template = "empty")
    public static void theGroupDataCarrierCastsThePickedEffectForever(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final DifficultyInstance hard = difficulty(level, Difficulty.HARD);
        for (int marker = EldritchCrabAiLogic.GROUP_EFFECT_SPEED;
                marker <= EldritchCrabAiLogic.GROUP_EFFECT_INVISIBILITY; marker++) {
            final EntityEldritchCrab crab = crab(helper);
            final EntityEldritchCrab.EldritchCrabGroupData data =
                    new EntityEldritchCrab.EldritchCrabGroupData();
            data.marker = marker;
            crab.finalizeSpawn(level, hard, MobSpawnType.NATURAL, data);
            final MobEffectInstance cast = crab.getEffect(EntityEldritchCrab.groupEffectOf(marker));
            helper.assertTrue(cast != null,
                    ":87-90 — marker " + marker + " must cast its table effect on the crab");
            helper.assertTrue(cast.getDuration() == EldritchCrabAiLogic.GROUP_EFFECT_MAX_AGE,
                    ":90 — the cast's duration is the original's unbounded Integer.MAX_VALUE, got "
                            + cast.getDuration());
            crab.discard();
        }
        final EntityEldritchCrab none = crab(helper);
        final EntityEldritchCrab.EldritchCrabGroupData data =
                new EntityEldritchCrab.EldritchCrabGroupData();
        data.marker = EldritchCrabAiLogic.GROUP_EFFECT_NONE;
        none.finalizeSpawn(level, hard, MobSpawnType.NATURAL, data);
        helper.assertTrue(none.getActiveEffects().isEmpty(),
                ":88 — the > 0 gate: marker 0 casts nothing, got " + none.getActiveEffects());
        none.discard();
        helper.succeed();
    }

    /** :160-169 — the shell breaks at half health; the chest plate it drops is
     *  unregistered content, so nothing is invented. */
    @GameTest(template = "empty")
    public static void halfHealthBreaksTheShellAndTheUnregisteredPlateIsNotInvented(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchCrab crab = crab(helper);
        crab.setShell(true);
        // Measured: the shelled crab's 5.0 armour scales an 8.0 hit to 7.68, so from
        // 19.0 the hit lands at 11.32 — ABOVE the 10.0 half-health break line, and the
        // drafted 19.0 fixture never armed the break. 17.0 lands at ~9.3, below half.
        crab.setHealth(17.0F);
        crab.hurt(level.damageSources().mobAttack(null), 8.0F);          // :160-162
        helper.assertTrue(crab.getHealth() <= 11.0F,
                "the probe needs the crab at or below half health, got " + crab.getHealth());
        helper.assertTrue(!crab.hasShell(),
                ":160-169 — damage at half health clears the shell");
        helper.assertTrue(close(crab.armorBaseForProbe(), EldritchCrabAiLogic.ARMOR_NONE)
                        && close(crab.speedBaseForProbe(), EldritchCrabAiLogic.SPEED_NO_SHELL),
                ":160-169 — the break restores armour 0 and speed 0.3");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(EldritchCrabAiLogic.CHEST_CULTIST_PLATE),
                ":164 — itemChestCultistPlate stays unregistered (its content-mapping row is"
                        + " not_started), so the drop is registry-gated");
        crab.setShell(true);
        crab.setHealth(crab.getMaxHealth());
        crab.hurt(level.damageSources().mobAttack(null), 1.0F);
        helper.assertTrue(crab.hasShell(),
                ":160-169 — a hit above half health keeps the shell");
        crab.discard();
        helper.succeed();
    }

    /** :116-118 — the first 20 ticks of life take no fall damage. */
    @GameTest(template = "empty")
    public static void fallDistanceIsClearedInTheFirstTwentyTicks(final GameTestHelper helper) {
        final EntityEldritchCrab crab = crab(helper);
        crab.fallDistance = 40.0F;
        crab.tick();
        helper.assertTrue(crab.fallDistance == 0.0F,
                ":116-118 — inside the window the crab's fall distance is zeroed, got "
                        + crab.fallDistance);
        helper.assertTrue(close(crab.getHealth(), crab.getMaxHealth()),
                ":116-118 — and it takes no fall damage, got " + crab.getHealth());
        helper.assertTrue(EldritchCrabAiLogic.FALL_IMMUNE_AGE_TICKS == 20,
                ":116-118 — the window is 20 ticks");
        crab.discard();
        helper.succeed();
    }

    /** :208-210 and :160-166 — poison never applies; the group-effect cast
     *  does, and the self-cast marker rule rides the crab's predicate. */
    @GameTest(template = "empty")
    public static void poisonNeverAppliesToTheCrab(final GameTestHelper helper) {
        final EntityEldritchCrab crab = crab(helper);
        helper.assertTrue(!crab.isPotionApplicable(new MobEffectInstance(MobEffects.POISON, 200, 0)),
                ":208-210 — func_70687_e: poison never applies to the crab");
        helper.assertTrue(crab.isPotionApplicable(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0)),
                ":208-210 — the immunity is poison-only; an ordinary effect still applies");
        helper.assertTrue(crab.isPotionApplicable(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0)),
                ":208-210 — the group-effect table's own effects stay applicable");
        // The in-world half is NOT probed on purpose: 21.1.259 has no mob-side
        // applicability hook — LivingEntity#addEffect(MobEffectInstance) is
        // `public final` (verified with javap on the compiled NeoForge jar) and
        // the universal jar ships no potion-applicability event (only
        // PotionBrewEvent/PlayerBrewedPotionEvent under event/brewing) — so the
        // rule rides the crab's plain predicate and the missing immunity is the
        // ledger row. A probe here would test the platform, not the port.
        helper.assertTrue(EldritchCrabAiLogic.POISON_IMMUNE,
                ":208-210 — the rule ships as data, the platform hook is absent (BD row)");
        crab.discard();
        helper.succeed();
    }

    /** :151-158 — a landed melee hit asks for the claw screech. The id the
     *  port pins is the original's {@code thaumcraft:crabclaw}; the audio lane
     *  shipped that id on main, so on this tree the seam fires a REGISTERED
     *  sound — the drafted probe pinned the pre-audio-lane world where the id
     *  was absent, and that premise is now stale by design. */
    @GameTest(template = "empty")
    public static void aLandedPunchAsksForTheClawSeamAndTheIdResolvesRegistered(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchCrab crab = crab(helper);
        final Pig pig = (Pig) EntityType.PIG.create(level);
        final BlockPos at = pad(helper);
        pig.moveTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        helper.assertTrue(!crab.wasClawFxRequested(), ":151 — the seam starts unfired");
        crab.doHurtTarget(pig);                                              // :152
        helper.assertTrue(crab.wasClawFxRequested(),
                ":151-158 — a landed hit runs the claw-sound site");
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(EldritchCrabAiLogic.SOUND_CLAW),
                ":153 — the audio lane shipped thaumcraft:crabclaw on main, so the id the port"
                        + " pins resolves to a registered sound on this tree");
        crab.discard();
        pig.discard();
        helper.succeed();
    }

    /** :120-137 — the mid-air climb onto an unridden player, the shelled crab
     *  that never climbs, and the 20% punch dismount band. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anUnshelledCrabInMidAirClimbsOnAPlayerAndPunchesHimOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = pad(helper);
        // The gate under test scans the PLAYER list, so the sweep box includes
        // players: a registered resident at these coordinates wins the
        // getNearestPlayer scan and the probe measures the resident, not the port.
        sweepPad(helper, level);
        // GameTestHelper#makeMockPlayer returns a helper-internal ANONYMOUS Player
        // (GameTestHelper$1) on 21.1.259 — not a ServerPlayer, invisible to the
        // players-list scan Level#getNearestPlayer runs, and a ClassCastException to
        // any cast that tries to register it. The shipped mechanism for a player the
        // LEVEL sees is the warp-events construction (WarpEventsGameTest.warpPlayer):
        // a real ServerPlayer over the login cookie with an EmbeddedChannel connection.
        final MinecraftServer server = level.getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-crabclimb-" + UUID.randomUUID().toString().substring(0, 8)), false);
        final ServerPlayer player =
                new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        // A ServerPlayer on a bare EmbeddedChannel dies on the FIRST payload its
        // tick sends (measured: the swarmer lane's crash 'Payload thaumcraft:
        // biome_change may not be sent to the client!' — a registered ServerPlayer
        // ticks, its biome-change/warp/attachment sends hit the channel check, the
        // throw rides the shared level tick and fails NEIGHBOUR probes). The
        // WarpEvents rule: declare every payload the tick can put on the wire as
        // an ad-hoc channel on THIS connection.
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(SyncAttachmentsPayload.TYPE.id());
        adHoc.add(BiomeChangePayload.TYPE.id());
        adHoc.add(MiscEventPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);        // harness kit (family rule)
        }
        player.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        player.setHealth(20.0F);
        player.setNoGravity(true);
        player.setInvulnerable(true);   // the ride's punches must not kill the fixture
        // ServerLevel#players() is the live player list Level#getNearestPlayer scans
        // (addPlayer is private on 21.1.259) — joining it is the registration.
        level.players().add(player);

        final List<EntityEldritchCrab> fixtures = new ArrayList<>();
        try {
            final EntityEldritchCrab airborne = crab(helper);
            fixtures.add(airborne);
            airborne.setNoGravity(true);
            airborne.setPos(player.getX(), player.getY() + 2.0, player.getZ());
            airborne.setPos(airborne.getX(), airborne.getY() - 0.4, airborne.getZ());
            airborne.setShell(false);
            helper.assertTrue(!airborne.onGround(), ":123 — the climb needs the crab off the ground");
            // The mount gate AND the punch (:131-137) fire on the SAME scripted
            // tick: a fresh crab has invulnerableTime 0, so the punch block passes
            // its cooldown check on the mount tick, and dismountsAfterPunch (0.2)
            // throws the crab off before the mount assert. Measured on the parent
            // tip (8 trace legs, full grid, noCurios arm): EVERY leg ivt=10..19
            // after the tick (the punch set its cooldown on the mount tick); 2 red
            // legs t1[veh=null psg=0 ivt=12 bdCd=60 rise=+0.20] — bdCd=60 is set
            // only by the dismount path (Entity#removePassenger) and the +0.20 rise
            // is the dismount hop (6/6 green legs rise=0.00 bdCd=0); the pre-tick
            // arms all trace TRUE (gate, canRide, canAddPassenger, canMountEntity)
            // — Entity#startRiding never rejects. The red census signature
            // yDelta=1.80 / distSqr=3.24 is the design 1.60 plus the 0.20 hop,
            // unreachable in a never-mounted state (1.60 / 2.56). The arm under
            // test is :120-129, so arm the crab at its MAXIMUM punch cooldown
            // (LivingEntity#aiStep decrements it once inside this tick and the
            // punch block skips on the mount tick) and let the mount reach the
            // assert; the punch cadence and the 0.2 dismount band are each
            // measured by their own arms below. Mutation teeth: flipping
            // PUNCH_DISMOUNT_CHANCE 0.2 -> 1.0 reddens the pre-fix probe every
            // leg naming :120-129, and leaves this probe green.
            airborne.invulnerableTime =
                    EldritchCrabAiLogic.PUNCH_COOLDOWN_BASE + EldritchCrabAiLogic.PUNCH_COOLDOWN_JITTER;
            airborne.tick();                                                 // :120-129
            helper.assertTrue(airborne.getVehicle() == player,
                    ":120-129 — a free mid-air unshelled crab mounts the nearest player");

            final EntityEldritchCrab shelled = crab(helper);
            fixtures.add(shelled);
            shelled.setNoGravity(true);
            shelled.setShell(true);
            shelled.setPos(player.getX(), player.getY() + 1.4, player.getZ());
            shelled.tick();                                                  // :124
            helper.assertTrue(shelled.getVehicle() == null,
                    ":124 — a shelled crab never climbs");

            // :131-137 — the punch on the ride: the mount takes the hit and 0.2 of
            // punches throw the rider, so the dismount band is measured on the rule.
            final int mounted = player.getPassengers().size();
            helper.assertTrue(mounted == 1, ":128 — the rider sits on the player, got " + mounted);
            int dismounts = 0;
            for (int i = 0; i < 400; i++) {
                if (EldritchCrabAiLogic.dismountsAfterPunch(((float) i % 100F) / 100F)) {
                    dismounts++;
                }
            }
            helper.assertTrue(dismounts == 80,
                    ":137 — 0.2 of punches dismount; the 0..1 sweep gives " + dismounts);
            int cooled = 0;
            int max = 0;
            for (int roll = 0; roll < EldritchCrabAiLogic.PUNCH_COOLDOWN_JITTER; roll++) {
                final int cooldown = EldritchCrabAiLogic.punchCooldownFor(roll);
                cooled += cooldown;
                max = Math.max(max, cooldown);
            }
            helper.assertTrue(max == 19 && cooled == 145,
                    ":131-133 — the cooldown is 10 + nextInt(10) (10..19, mean 14.5), got max "
                            + max + " mean " + (cooled / 10.0));
            helper.succeed();
        } finally {
            // The teardown runs on the RED path too. `level.players().add(player)`
            // is a LEVEL-WIDE registration: it is in the scan set of every later
            // probe's getNearestPlayer for the rest of the run, and this fixture
            // is a bare-connection ServerPlayer whose first payload send throws
            // ('Payload thaumcraft:biome_change may not be sent to the client!' —
            // measured on the c4b-1 card, where it rode the shared level tick and
            // killed the NEIGHBOUR swarmer probe). A probe that throws an assert
            // mid-body reaches no tail code, so the tail-only teardown leaked a
            // registered player exactly on the leg that goes red: the resident the
            // next pad's player scan finds (the P2 plant reproduces that red
            // byte-for-byte). fixtures ride in a list so the crabs are discarded
            // from the red path as well.
            for (final EntityEldritchCrab fixture : fixtures) {
                fixture.stopRiding();
                fixture.discard();
            }
            level.players().remove(player);
            player.discard();
        }
    }

    /**
     * :63-87 — the orb's burst. Everything living that is not undead inside the
     * orb's box inflated by 2.0 ({@code :65}) takes the thrown damage
     * ({@code :70-74}) and the weakness cast ({@code :76-79}); the undead walk
     * out of it ({@code :69}); the burst consumes the orb ({@code :84}); and the
     * thrower is not in the swept set at all — the original's {@code :65} sweep
     * is {@code world.getEntitiesWithinAABB(thrower, box)}, whose first argument
     * is the entity the sweep SKIPS, so a crab never takes its own orb's burst
     * while a neighbour standing in exactly the same spot does.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theOrbBurstStrikesTheLivingAndSparesTheUndead(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The pad cell is this probe's OWN cell, not the (1,3,1) cell the lane's
        // box-only probes use: `empty` is 1×1×1, so the harness force-loads exactly
        // the one chunk the cell corner sits in (StructureUtils#prepareTestStructure
        // -> forceLoadChunks) and a FORCED ticket entity-ticks that chunk alone
        // (ChunkMap#FORCED_TICKET_LEVEL = ChunkLevel.byStatus(ENTITY_TICKING), no
        // radius). This probe reads an ENTITY clock (the orb's age), and a fixture a
        // block past the cell corner can land on a neighbour chunk nothing ticks —
        // then the age the burst writes (:84) never walks past the clamp (:27-29) and
        // the consumption arm starves (docs/testing.md:123 — fix the CELL, not the
        // assertion). The swept box is anchored at the cell corner (`padEntities`),
        // so the census box, the burst box (the orb's box inflated 2.0) and every
        // offset below are unchanged: the pad shifts by (-1,-2,-1) inside the same box.
        final BlockPos at = helper.absolutePos(BlockPos.ZERO);
        // The burst sweeps entities in its box and the pad census is shared world
        // state; a grid resident in the box is contamination (docs/testing.md).
        sweepPad(helper, level);
        final EntityEldritchCrab thrower = crab(helper);
        thrower.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);

        final Pig pig = (Pig) EntityType.PIG.create(level);
        pig.moveTo(at.getX() + 1.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        final Zombie zombie = (Zombie) EntityType.ZOMBIE.create(level);
        zombie.moveTo(at.getX() + 1.5, at.getY() + 0.5, at.getZ() + 1.5, 0.0F, 0.0F);
        zombie.setNoGravity(true);
        zombie.clearFire();   // the pad lights mobs (family finding) — the burst probe is about the burst
        zombie.setPersistenceRequired();
        level.addFreshEntity(zombie);

        final EntityEldritchOrb orb = orb(helper,
                new Vec3(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5));
        orb.setOwner(thrower);                       // :73 the thrower's damage
        helper.assertTrue(close(orb.getDefaultGravityForProbe(), EntityEldritchOrb.GRAVITY),
                "the orb floats: gravity 0.0, got " + orb.getDefaultGravityForProbe());
        helper.assertTrue(orb.getType().getDimensions().width() == EntityEldritchOrb.WIDTH,
                "the registry's box is the orb's 0.25");

        final float pigHealth = pig.getHealth();
        final float zombieHealth = zombie.getHealth();
        final float throwerHealth = thrower.getHealth();
        orb.setDeltaMovement(new Vec3(1.0, -0.5, 0.0));
        for (int tick = 0; tick < 25; tick++) {
            orb.tick();                                                        // :63-87 impact
            if (pig.getEffect(MobEffects.WEAKNESS) != null) {
                break;   // the burst has fired (measured: the impact lands on the first tick)
            }
        }
        helper.assertTrue(pig.getEffect(MobEffects.WEAKNESS) != null,
                ":63-87 — the manual drive reached the impact");
        helper.assertTrue(pig.getEffect(MobEffects.WEAKNESS) != null,
                ":76-79 — the living target takes the weakness cast (160 ticks, amplifier 0)");
        helper.assertTrue(pig.getEffect(MobEffects.WEAKNESS).getDuration()
                        == EntityEldritchOrb.WEAKNESS_TICKS
                        && pig.getEffect(MobEffects.WEAKNESS).getAmplifier()
                                == EntityEldritchOrb.WEAKNESS_AMPLIFIER,
                ":76-79 — 160 ticks / amplifier 0, got "
                        + pig.getEffect(MobEffects.WEAKNESS).getDuration() + " / "
                        + pig.getEffect(MobEffects.WEAKNESS).getAmplifier());
        helper.assertTrue(pig.getHealth() < pigHealth,
                ":70-74 — the burst damages the living: " + pigHealth + " -> " + pig.getHealth());
        // :70-74 — the source. The original hands attackEntityFrom
        // DamageSource.func_76354_b(this, thrower): 21.1.259's thrown factory is
        // DamageSources#thrown (javap), whose shape is measured here — direct
        // entity the orb, owning entity the thrower, a projectile type and NOT
        // the magic type (the pair that separates thrown from indirectMagic).
        final DamageSource struck = pig.getLastDamageSource();
        helper.assertTrue(struck != null && struck.getDirectEntity() == orb
                        && struck.getEntity() == thrower,
                ":72 — the source is the orb, owned by the thrower, got " + struck);
        helper.assertTrue(struck != null && struck.is(DamageTypeTags.IS_PROJECTILE)
                        && struck.is(DamageTypes.THROWN)
                        && !struck.is(DamageTypes.MAGIC)
                        && !struck.is(DamageTypes.INDIRECT_MAGIC),
                ":72 — a thrown source, not an indirect-magic one: "
                        + (struck == null ? "null" : struck.getMsgId() + " " + struck.getDirectEntity()));
        // :65 — the swept set excludes the THROWER (its first argument), so the
        // crab that owns the orb stands in the burst box and takes nothing,
        // while the pig in the same box takes it. A sweep that excludes the orb
        // instead (the drafted shape) damages the thrower and reads red here.
        helper.assertTrue(thrower.getHealth() >= throwerHealth,
                ":65 — the thrower is out of its own burst, "
                        + throwerHealth + " -> " + thrower.getHealth());
        helper.assertTrue(zombie.getEffect(MobEffects.WEAKNESS) == null,
                ":69 — the undead are exempt: no weakness on the zombie");
        helper.assertTrue(zombie.getHealth() >= zombieHealth,
                ":69 — the undead are exempt: the zombie takes no burst damage, "
                        + zombieHealth + " -> " + zombie.getHealth());
        // :84 — the consumption, OBSERVED ON THE ORB'S OWN CLOCK. The impact writes
        // the 100-tick age (:84) and :27-29 discards once the age is PAST it, and the
        // age moves only on the SERVER's entity tick (ServerLevel#tickNonPassenger is
        // tickCount++ then tick()), which the server runs only for an entity whose
        // chunk is in the entity-ticking range (ServerLevel#tick ->
        // ChunkMap#inEntityTickingRange). The harness vouches exactly the chunks the
        // template box covers (StructureUtils#prepareTestStructure -> forceLoadChunks;
        // a forced chunk entity-ticks at exactly ChunkMap#FORCED_TICKET_LEVEL =
        // ChunkLevel.byStatus(ENTITY_TICKING) and the FORCED ticket carries no
        // radius), and this probe's pad cell sits a block PAST its own cell corner —
        // so when the grid draw leaves the pad's chunk unvouched (a row's last column
        // or a batch's last row landing on a chunk boundary; GameTestServer#startTests
        // draws the origin from Level#getRandom), the orb's clock never moves while
        // the probe's own clock (GameTestInfo#tickInternal = level.getGameTime() -
        // startTick) runs on to the 300-tick timeout. Measured: the CI line this card
        // opens (main 046f3d32, run 43634031237, build job — age=100 removed=false)
        // and the parent card's clean-pad diag#7 (1 leg in 11).
        // The arm watches the orb's clock, not the test clock. The removal assert IS
        // the poll: in 1.21.1 a succeedWhen criterion that RETURNS consumes the event
        // and runs thenSucceed, so an early return is a false green — the assert keeps
        // throwing until the removal lands and the harness retries it each tick. The
        // clock is driven with the entity tick the server owes (tickCount++ then
        // tick(), the body of ServerLevel#tickNonPassenger) ONLY on the probe ticks
        // where the server left it still — see driveEntityClock. On a vouched pad
        // nothing is driven and the server's own tick ends the arm, so the entity's
        // clock stays the reference and no outcome is pinned to N real ticks
        // (docs/testing.md). The census line names vouched/driven per leg, so a vouch
        // loss shows in the log instead of being inferred from a red, and the failure
        // message carries the same two facts. Everything the burst WRITES is asserted
        // synchronously above, as before.
        final boolean vouched = level.isPositionEntityTicking(orb.blockPosition());
        final AtomicInteger driven = driveEntityClock(helper, orb, 100);
        final AtomicInteger polled = new AtomicInteger();
        helper.succeedWhen(() -> {
            final int age = orb.tickCount;
            if (polled.incrementAndGet() == 1) {
                final BlockPos corner = helper.absolutePos(BlockPos.ZERO);
                LOGGER.info("[TC-M6-06-orb] consumption poll starts: pad chunk vouched "
                        + "(entity-ticking)={} ticksDrivenSoFar={} age={} removed={} orbChunk={} "
                        + "corner={} cornerMod16={},{} probeTick={}",
                        vouched, driven.get(), age, orb.isRemoved(), orb.chunkPosition(), corner,
                        corner.getX() & 15, corner.getZ() & 15, helper.getTick());
            }
            helper.assertTrue(orb.isRemoved(),
                    ":84 — the burst consumes the orb (the impact writes the 100-tick age and "
                            + ":27-29 discards once the ENTITY clock walks past it; that clock "
                            + "moves only in the server's entity pass, which runs only for an "
                            + "entity in an entity-ticking chunk, so this arm drives the owed "
                            + "tick itself while the pad chunk is left unvouched), age=" + age
                            + " removed=" + orb.isRemoved() + " polls=" + polled.get()
                            + " padChunkVouched=" + vouched
                            + " ticksDrivenByProbe=" + driven.get());
            LOGGER.info("[TC-M6-06-orb] consumption seen on the orb's own clock: pad chunk "
                    + "vouched (entity-ticking)={} ticksDrivenByProbe={} age={} polls={} probeTick={}",
                    vouched, driven.get(), age, polled.get(), helper.getTick());
            thrower.discard();
            pig.discard();
            zombie.discard();
        });
    }


    /**
     * Drive an entity's clock from the probe's own ticks and count the driven ticks.
     *
     * <p>The server ticks an entity only in an entity-ticking chunk
     * (ServerLevel#tick -> ChunkMap#inEntityTickingRange) and the harness vouches only
     * the chunks the probe's template box covers
     * (StructureUtils#prepareTestStructure -> forceLoadChunks), so a probe standing on
     * an unvouched chunk has no server ticks of that entity to observe at all, while
     * its own clock (GameTestInfo#tickInternal = level.getGameTime() - startTick) keeps
     * running. Each step spends ONE probe tick and drives the entity with the entity
     * tick the server owes ({@code tickCount++} then {@code tick()}, the body of
     * {@code ServerLevel#tickNonPassenger}) ONLY if the clock did not move during that
     * tick — so on a vouched pad nothing is ever driven (the count stays 0) and the
     * entity's own clock is the reference, which is what docs/testing.md asks for. It
     * is not a real-tick budget: the arm ends on the removal, and the chain ends by
     * itself once the entity is gone.</p>
     */
    private static AtomicInteger driveEntityClock(final GameTestHelper helper,
            final Entity entity, final int maxProbeTicks) {
        final AtomicInteger driven = new AtomicInteger();
        final int[] steps = {0};
        final Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (entity.isRemoved() || steps[0] >= maxProbeTicks) {
                return;
            }
            final int seen = entity.tickCount;
            try {
                helper.runAfterDelay(1, () -> {
                    if (!entity.isRemoved() && entity.tickCount == seen) {
                        entity.tickCount++;   // the server's entity tick, by hand
                        entity.tick();
                        driven.incrementAndGet();
                    }
                    steps[0]++;
                    step[0].run();
                });
            } catch (IllegalStateException probeIsDone) {
                // The probe is finished and refuses further timed actions, so the drive
                // chain ends with it; a completed probe has nothing left to observe.
                return;
            }
        };
        step[0].run();
        return driven;
    }

    /** :83 / :89-91 — the impact fizz's figures and the pickup radius live on
     *  the orb's own class (the 92 lines have no launch sound at all), and the
     *  burst's client event id is the original's 16 ({@code :85}). */
    @GameTest(template = "empty")
    public static void theOrbCarriesItsOwnSoundAndEventFigures(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchOrb orb = orb(helper,
                new Vec3(pad(helper).getX() + 0.5, pad(helper).getY() + 1.0, pad(helper).getZ() + 0.5));
        helper.assertTrue(orb.getPickRadius() == EntityEldritchOrb.PICK_RADIUS,
                ":89-91 — func_70053_R() is 0.1F, got " + orb.getPickRadius());
        helper.assertTrue(EntityEldritchOrb.BURST_EVENT == (byte) 16
                        && EntityEldritchOrb.LIFETIME_TICKS == 100
                        && EntityEldritchOrb.AOE_INFLATE == 2.0,
                ":85 / :30-31 / :65 — the event id is 16, the age 100, the AABB inflate 2.0");
        // :83 — the pitch spread is symmetric: 2.6F + (r - r) * 0.8F spans
        // 2.2..3.0, so the pinned spread must be the full 0.8F and a one-sided
        // 2.6..3.4 rewrite of the original's (r - r) is caught by the pair.
        helper.assertTrue(EntityEldritchOrb.SOUND_PITCH_BASE == 2.6F
                        && EntityEldritchOrb.SOUND_PITCH_SPREAD == 0.8F
                        && EntityEldritchOrb.SOUND_VOLUME == 0.5F,
                ":83 — random.fizz at 0.5F, 2.6F + (r - r) * 0.8F");
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "crabclaw")),
                "the crab's claw id is registered (the audio lane shipped it on main) while the"
                        + " orb's impact fizz rides the vanilla random.fizz figures");
        orb.discard();
        helper.succeed();
    }

    /** :94 — the crab's spawn pass rides the vanilla spider pair for the
     *  group-data shape: the port ships its own carrier because 21.1.259's
     *  spider carrier keeps its effect {@code protected} with no accessor. */
    @GameTest(template = "empty")
    public static void theCarrierIsThePortsOwnBecauseTheVanillaOneIsOpaque(final GameTestHelper helper) {
        helper.assertTrue(EntityEldritchCrab.EldritchCrabGroupData.class
                        .getDeclaredFields().length == 1,
                ":78-92 — the carrier is the marker alone, got "
                        + EntityEldritchCrab.EldritchCrabGroupData.class.getDeclaredFields().length);
        int markerMods;
        try {
            markerMods = EntityEldritchCrab.EldritchCrabGroupData.class
                    .getDeclaredField("marker").getModifiers();
        } catch (NoSuchFieldException absent) {
            helper.fail(":78-92 — the carrier must expose its marker field: " + absent);
            return;
        }
        helper.assertTrue(java.lang.reflect.Modifier.isPublic(markerMods),
                ":78-92 — 1.7.10's GroupData potion id is public; the port's marker is too");
        helper.assertTrue(EldritchCrabAiLogic.GROUP_EFFECT_MAX_AGE == Integer.MAX_VALUE
                        && EldritchCrabAiLogic.GROUP_POTION_BASE_CHANCE == 0.1F,
                ":86-95 — the gate is 0.1 * localDifficulty on HARD, and the cast is unbounded");
        helper.succeed();
    }
}
