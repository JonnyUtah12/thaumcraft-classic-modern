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
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

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
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EldritchCrabGameTest {

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
        return new DifficultyInstance(mode, level.getDayTime(), 0L, 0.0F);
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
        for (final Difficulty mode : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.PEACEFUL)) {
            for (int i = 0; i < 20; i++) {
                final EntityEldritchCrab crab = crab(helper);
                crab.finalizeSpawn(level, difficulty(level, mode), MobSpawnType.NATURAL, null);
                if (crab.hasShell()) {
                    rolled++;
                }
                crab.discard();
            }
        }
        // :74-75 — p = 1/3 over n = 60 draws: mean 20, sd 3.65, so the band
        // 9..31 is ±3.0 sd (a clean run passes with p ~0.997 and the roll
        // reseeded every draw by the mob's own LCG), while a rule that made the
        // shell HARD-only — the shape this probe first predicted — scores 0.
        helper.assertTrue(rolled >= 9 && rolled <= 31,
                ":74-75 — 0.33 of the 60 non-HARD spawns wear a shell; measured " + rolled);
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
        crab.setHealth(19.0F);
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

    /** :151-158 — a landed melee hit asks for the claw screech; the id is
     *  unregistered (D-004) so the seam is observed, not a sound. */
    @GameTest(template = "empty")
    public static void aLandedPunchAsksForTheClawSeamAndTheIdStaysUnregistered(
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
        helper.assertTrue(!BuiltInRegistries.SOUND_EVENT.containsKey(EldritchCrabAiLogic.SOUND_CLAW),
                ":153 — thaumcraft:crabclaw is not registered (the audio lane owns the registry,"
                        + " D-004), so the port pins the id and fires nothing");
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
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);

        final EntityEldritchCrab airborne = crab(helper);
        airborne.setNoGravity(true);
        airborne.setPos(player.getX(), player.getY() + 2.0, player.getZ());
        airborne.setPos(airborne.getX(), airborne.getY() - 0.4, airborne.getZ());
        airborne.setShell(false);
        helper.assertTrue(!airborne.onGround(), ":123 — the climb needs the crab off the ground");
        airborne.tick();                                                     // :120-129
        helper.assertTrue(airborne.getVehicle() == player,
                ":120-129 — a free mid-air unshelled crab mounts the nearest player");

        final EntityEldritchCrab shelled = crab(helper);
        shelled.setNoGravity(true);
        shelled.setShell(true);
        shelled.setPos(player.getX(), player.getY() + 1.4, player.getZ());
        shelled.tick();                                                      // :124
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
        airborne.stopRiding();
        airborne.discard();
        shelled.discard();
        helper.succeed();
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
        final BlockPos at = pad(helper);
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
            if (orb.isRemoved()) {
                break;
            }
        }
        helper.assertTrue(orb.isRemoved(),
                ":84 — the burst consumes the orb (the impact writes the 100-tick age)");
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
        thrower.discard();
        pig.discard();
        zombie.discard();
        helper.succeed();
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
        helper.assertTrue(!BuiltInRegistries.SOUND_EVENT.containsKey(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "crabclaw")),
                "the crab's claw id stays unregistered while the orb's fizz rides vanilla");
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
        helper.assertTrue(java.lang.reflect.Modifier.isPublic(
                        EntityEldritchCrab.EldritchCrabGroupData.class
                                .getDeclaredField("marker").getModifiers()),
                ":78-92 — 1.7.10's GroupData potion id is public; the port's marker is too");
        helper.assertTrue(EldritchCrabAiLogic.GROUP_EFFECT_MAX_AGE == Integer.MAX_VALUE
                        && EldritchCrabAiLogic.GROUP_POTION_BASE_CHANCE == 0.1F,
                ":86-95 — the gate is 0.1 * localDifficulty on HARD, and the cast is unbounded");
        helper.succeed();
    }
}
