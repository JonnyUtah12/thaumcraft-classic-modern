package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EldritchGuardianAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityInhabitedZombie;
import com.jonnyutah12.thaumcraftclassic.entities.monster.InhabitedZombieAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Eldritch Guardian and the Inhabited Zombie
 * (TC-M6-06-s-c4b-2, content-mapping {@code Thaumcraft:EldritchGuardian} (CSV
 * line 323) mod_index 25 registration {@code ConfigEntities.java:107}, and
 * {@code Thaumcraft:InhabitedZombie} (CSV line 331) mod_index 33 registration
 * {@code ConfigEntities.java:123}).
 *
 * <p>Every figure reaches the mob through the live registry and the vanilla
 * {@code Mob} / {@code LivingEntity} / {@code Projectile} API, so a probe goes
 * RED on behaviour rather than on a missing symbol. Each literal is the
 * original's own and quotes its decompiled line in {@code
 * thaumcraft.common.entities.monster.EntityEldritchGuardian} (310 lines) and
 * {@code thaumcraft.common.entities.monster.EntityInhabitedZombie} (115
 * lines); the world-free rules of the same lines are pinned by {@code
 * EldritchGuardianAiLogicTest} and {@code InhabitedZombieAiLogicTest}.</p>
 *
 * <p>The pad fixtures follow the family's findings: an "empty" template has no
 * floor, so every spawned mob rides {@code setNoGravity(true)} + {@code
 * clearFire()} (the pech card); mob age ({@code tickCount}) is walked by
 * {@code ServerLevel#tickNonPassenger}, so age-gated seams ride {@code
 * runAfterDelay}, never a manual tick loop (the c4b-1 card); and the goals of
 * an {@code setNoAi(true)} mob are driven by hand through {@code Goal#start()}
 * + {@code Goal#tick()}, which is the only way the ranged attack's cooldown
 * walks without a shared-tick dependency.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EldritchMobGameTest {

    private EldritchMobGameTest() {
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

    private static BlockPos pad(final GameTestHelper helper) {
        return helper.absolutePos(BlockPos.ZERO).offset(1, 3, 1);
    }

    private static <T extends Mob> T spawn(final GameTestHelper helper, final String path,
            final Class<T> kind, final int dx, final int dz) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, path).create(level);
        helper.assertTrue(kind.isInstance(spawned),
                "thaumcraft:" + path + " did not create a " + kind.getSimpleName() + " but "
                        + (spawned == null ? "null" : spawned.getClass().getName()));
        final T mob = kind.cast(spawned);
        final BlockPos at = pad(helper).offset(dx, 0, dz);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    private static EntityEldritchGuardian guardian(final GameTestHelper helper,
            final int dx, final int dz) {
        return spawn(helper, "eldritchguardian", EntityEldritchGuardian.class, dx, dz);
    }

    private static EntityInhabitedZombie zombie(final GameTestHelper helper,
            final int dx, final int dz) {
        return spawn(helper, "inhabitedzombie", EntityInhabitedZombie.class, dx, dz);
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

    private static boolean contains(final Iterable<WrappedGoal> goals, final Class<?> goal) {
        for (final WrappedGoal wrapped : goals) {
            if (goal.isInstance(wrapped.getGoal())) {
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

    // ------------------------------------------------------------------ roster

    /**
     * {@code ConfigEntities.java:107} / {@code :123} — both triples are 64 / 3
     * (4 chunks / 3 ticks), the boxes are the guardian's {@code
     * func_70105_a(0.8F, 2.25F)} of {@code :80} with the {@code func_70047_e}
     * eye height of {@code :247-249} and the inherited vanilla zombie box of
     * the 115-line class (BD-120), and neither builder calls {@code
     * fireImmune()} (the guardian's {@code :159-161} getter answers false).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void thePairIsRegisteredWithTheOriginalBoxesAndTracking(final GameTestHelper helper) {
        final EntityType<?> guardian = registered(helper, "eldritchguardian");
        helper.assertTrue(Math.abs(guardian.getDimensions().width() - EldritchGuardianAiLogic.WIDTH) < 1.0E-4F
                        && Math.abs(guardian.getDimensions().height() - EldritchGuardianAiLogic.HEIGHT) < 1.0E-4F,
                ":80 — the guardian's hitbox must be 0.8 x 2.25, got "
                        + guardian.getDimensions().width() + " x " + guardian.getDimensions().height());
        helper.assertTrue(Math.abs(guardian.getDimensions().eyeHeight()
                        - EldritchGuardianAiLogic.EYE_HEIGHT) < 1.0E-4F,
                ":247-249 — the guardian's eye height must be 2.1, got "
                        + guardian.getDimensions().eyeHeight());
        helper.assertTrue(guardian.getCategory() == MobCategory.MONSTER,
                "the guardian is not a MONSTER-category mob but " + guardian.getCategory());
        helper.assertTrue(guardian.clientTrackingRange() == EldritchGuardianAiLogic.TRACK_RANGE_CHUNKS
                        && guardian.updateInterval() == EldritchGuardianAiLogic.UPDATE_INTERVAL,
                ":107 — the guardian's triple must be 64 blocks (4 chunks) / 3 ticks, got "
                        + guardian.clientTrackingRange() + " chunks / " + guardian.updateInterval() + " ticks");
        helper.assertTrue(guardian.fireImmune() == false,
                ":159-161 — the guardian is not fire-immune on either engine");
        helper.assertTrue(CatalogEldritchGuardian.ELDRITCH_GUARDIAN.get() == guardian,
                "the catalog's ELDRITCH_GUARDIAN holder is not the registered type");
        helper.assertTrue(CatalogEldritchGuardian.HAS_SPAWN_EGG_ROW == false,
                ":113 — the guardian's egg is the guardian row's egg (mod_index 56), not a row");

        final EntityType<?> zombie = registered(helper, "inhabitedzombie");
        helper.assertTrue(Math.abs(zombie.getDimensions().width() - InhabitedZombieAiLogic.WIDTH) < 1.0E-4F
                        && Math.abs(zombie.getDimensions().height() - InhabitedZombieAiLogic.HEIGHT) < 1.0E-4F,
                "the 115 lines set no size, so the box is the vanilla zombie's 0.6 x 1.95, got "
                        + zombie.getDimensions().width() + " x " + zombie.getDimensions().height());
        helper.assertTrue(Math.abs(zombie.getDimensions().eyeHeight()
                        - InhabitedZombieAiLogic.EYE_HEIGHT) < 1.0E-4F,
                "the eye height is the vanilla zombie's 1.74, got " + zombie.getDimensions().eyeHeight());
        helper.assertTrue(zombie.getCategory() == MobCategory.MONSTER,
                "the inhabited zombie is not a MONSTER-category mob but " + zombie.getCategory());
        helper.assertTrue(zombie.clientTrackingRange() == InhabitedZombieAiLogic.TRACK_RANGE_CHUNKS
                        && zombie.updateInterval() == InhabitedZombieAiLogic.UPDATE_INTERVAL,
                ":123 — the zombie's triple must be 64 blocks (4 chunks) / 3 ticks, got "
                        + zombie.clientTrackingRange() + " chunks / " + zombie.updateInterval() + " ticks");
        helper.assertTrue(CatalogEldritchGuardian.INHABITED_ZOMBIE.get() == zombie,
                "the catalog's INHABITED_ZOMBIE holder is not the registered type");

        // The host seam: the guardian row now resolves, so the warp lane's
        // mist band summons the real guardian (the live summon itself is
        // pinned by WarpEventsGameTest's mist-band probe). The inhabited
        // zombie is NOT a structure summon: EldritchMobHost's roster is
        // exactly the original's seven tile summons — GUARDIAN / CULTIST /
        // CLERIC / KNIGHT / BOBO / CRAB / FIRE_MOTH (EldritchRealmValuesParityTest
        // pins the ids) — and 1.7.10's EntityInhabitedZombie arrives from the
        // night-spawn path, never from an eldritch tile. So the zombie's claim
        // is the registry + catalog holders above, and the host seam must stay
        // UNCLAIMED for it: wiring it there would move the structures'
        // countFamily census and has to re-open this probe.
        helper.assertTrue(EldritchMobHost.isAvailable(EldritchMobHost.GUARDIAN),
                "the guardian row still reports not_started to the host");
        helper.assertTrue(EldritchMobHost.isAvailable("inhabitedzombie") == false
                        && EldritchMobHost.spawn("inhabitedzombie", helper.getLevel(), pad(helper)) == false,
                "the host seam claims thaumcraft:inhabitedzombie, which none of the original's "
                        + "eldritch structures ever summon (roster: guardian/cultist/cleric/"
                        + "knight/bobo/crab/firemoth)");

        // The slice's append-only tail: the roster landed before it is intact.
        registered(helper, "eldritchcrab");
        registered(helper, "eldritchorb");
        registered(helper, "cultistcleric");
        registered(helper, "cultistknight");
    }

    // ------------------------------------------------------------- goal sets

    /**
     * {@code :55/:56/:57/:58/:59/:60/:61} + the target goals of {@code
     * :62/:63/:64} — every slot quoted from the constructor. The look goals
     * share priority 8 on 1.7.10 (a GoalSelector map keyed on the slot keeps
     * one); 1.21.1 keys on the goal, so the shipped set carries both, the
     * second at 9 (BD-118, recorded as a known difference).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianCarriesTheOriginalGoalAndTargetSet(final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        final List<WrappedGoal> goals = goals(mob, false);
        final List<WrappedGoal> targets = goals(mob, true);
        helper.assertTrue(has(goals, FloatGoal.class, EldritchGuardianAiLogic.GOAL_SLOT_SWIM),
                ":55 — no swimming goal at slot 0. " + describe(goals));
        helper.assertTrue(has(goals, AILongRangeAttack.class, EldritchGuardianAiLogic.GOAL_SLOT_RANGED),
                ":56 — no ranged-attack goal at slot 2. " + describe(goals));
        helper.assertTrue(has(goals, MeleeAttackGoal.class, EldritchGuardianAiLogic.GOAL_SLOT_MELEE),
                ":57 — no melee-attack goal at slot 3. " + describe(goals));
        helper.assertTrue(has(goals, MoveTowardsRestrictionGoal.class,
                        EldritchGuardianAiLogic.GOAL_SLOT_RESTRICTION),
                ":58 — no home-restriction goal at slot 5. " + describe(goals));
        helper.assertTrue(has(goals, RandomStrollGoal.class, EldritchGuardianAiLogic.GOAL_SLOT_WANDER),
                ":59 — no stroll goal at slot 7. " + describe(goals));
        helper.assertTrue(has(goals, LookAtPlayerGoal.class, EldritchGuardianAiLogic.GOAL_SLOT_WATCH),
                ":60 — no look-at-player goal at slot 8. " + describe(goals));
        helper.assertTrue(has(goals, RandomLookAroundGoal.class,
                        EldritchGuardianAiLogic.GOAL_SLOT_LOOK_IDLE_PORT),
                ":61 — no random-look goal (the original's slot 8, shipped at 9). " + describe(goals));
        helper.assertTrue(goals.size() == 7,
                "the constructor declares seven goals, got " + goals.size() + ". " + describe(goals));
        helper.assertTrue(has(targets, HurtByTargetGoal.class, EldritchGuardianAiLogic.TARGET_SLOT_HURT),
                ":62 — no hurt-by-target goal at target-slot 1. " + describe(targets));
        helper.assertTrue(contains(targets, NearestAttackableTargetGoal.class),
                ":63/:64 — the player and cultist target goals are missing. " + describe(targets));
        helper.assertTrue(targets.size() == 3,
                ":62/:63/:64 declare three target goals, got " + targets.size() + ". " + describe(targets));
    }

    /**
     * The goal reconciliation the card asks for: the shipped set is written out
     * as the original's net set — the Zombie's own movement goals at the slots
     * 1.21.1's parent uses, plus {@code :29}'s hurt goal (alert-same-type) and
     * {@code :30}'s cultist goal — and the vanilla extras the 1.7.10 slot
     * writes evicted (the villager, iron golem and turtle goals, the villager
     * attack, the door goals, the sun goal and the item goals) are NOT
     * inherited. The census is the assertion: 5 goals, 3 targets.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theZombieKeepsTheZombieSetAndStripsTheVanillaExtras(final GameTestHelper helper) {
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        final List<WrappedGoal> goals = goals(mob, false);
        final List<WrappedGoal> targets = goals(mob, true);
        helper.assertTrue(has(goals, ZombieAttackGoal.class, InhabitedZombieAiLogic.GOAL_SLOT_MELEE),
                "the Zombie melee goal at slot 2 is missing. " + describe(goals));
        helper.assertTrue(has(goals, MoveThroughVillageGoal.class, InhabitedZombieAiLogic.GOAL_SLOT_VILLAGE),
                "the village-patrol goal at slot 6 is missing. " + describe(goals));
        helper.assertTrue(has(goals, WaterAvoidingRandomStrollGoal.class,
                        InhabitedZombieAiLogic.GOAL_SLOT_WANDER),
                "the stroll goal at slot 7 is missing. " + describe(goals));
        helper.assertTrue(has(goals, LookAtPlayerGoal.class, InhabitedZombieAiLogic.GOAL_SLOT_WATCH),
                "the look-at-player goal at slot 8 is missing. " + describe(goals));
        helper.assertTrue(has(goals, RandomLookAroundGoal.class, InhabitedZombieAiLogic.GOAL_SLOT_LOOK_IDLE),
                "the random-look goal at slot 8 is missing. " + describe(goals));
        helper.assertTrue(goals.size() == InhabitedZombieAiLogic.SHIPPED_GOAL_COUNT,
                "the shipped goal census is " + InhabitedZombieAiLogic.SHIPPED_GOAL_COUNT + ", got "
                        + goals.size() + ". " + describe(goals));
        helper.assertTrue(targets.size() == InhabitedZombieAiLogic.SHIPPED_TARGET_COUNT,
                "the shipped target census is " + InhabitedZombieAiLogic.SHIPPED_TARGET_COUNT + ", got "
                        + targets.size() + ". " + describe(targets));
        helper.assertTrue(has(targets, HurtByTargetGoal.class, InhabitedZombieAiLogic.TARGET_SLOT_HURT),
                ":29 — the hurt-by-target goal is missing at slot 1. " + describe(targets));
        helper.assertTrue(has(targets, NearestAttackableTargetGoal.class,
                        InhabitedZombieAiLogic.TARGET_SLOT_PLAYER),
                "the player target goal at slot 2 is missing. " + describe(targets));
        helper.assertTrue(has(targets, NearestAttackableTargetGoal.class,
                        InhabitedZombieAiLogic.TARGET_SLOT_CULTIST),
                ":30 — the cultist target goal at slot 3 is missing. " + describe(targets));
        helper.assertTrue(mob.canPickUpLoot() == false,
                ":31-38 — canPickUpLoot must be off (the pick-up strip's twin)");
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)) < 1.0E-9,
                ":24 — the reinforcement attribute is pinned to 0.0 (BD-120)");
    }

    // ------------------------------------------------------------ attributes

    /**
     * {@code :69-75} + {@code :84-86} + {@code :155-157} + {@code :242-244} on
     * the live registry: health 50, follow 40, speed 0.28, damage 7, armour 4
     * (the runtime getter of {@code :84-86} rides the ARMOR base, the crab
     * slice's mapping of the same getter), max air 500, no loot-picking, and
     * the boss flag pinned as the original's flag — 1.21.1 has no {@code
     * Mob#isBoss()}, boss bars are {@code ServerBossEvent}s and the bar belongs
     * to the family's boss-bar lane (the cultist card's finding, BD-91).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianShipsTheOriginalAttributesAndFlags(final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) - 50.0) < 1.0E-4,
                ":71 — max health must be 50, got " + mob.getAttributeBaseValue(Attributes.MAX_HEALTH));
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE) - 40.0) < 1.0E-4,
                ":72 — follow range must be 40, got " + mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE));
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - 0.28) < 1.0E-4,
                ":73 — movement speed must be 0.28, got "
                        + mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) - 7.0) < 1.0E-4,
                ":74 — attack damage must be 7, got " + mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.ARMOR) - 4.0) < 1.0E-4,
                ":84-86 — the armour getter's 4 rides the ARMOR base, got "
                        + mob.getAttributeBaseValue(Attributes.ARMOR));
        helper.assertTrue(mob.getMaxAirSupply() == 500,
                ":155-157 — the max air must be 500, got " + mob.getMaxAirSupply());
        helper.assertTrue(mob.canPickUpLoot() == false, ":242-244 — canPickUpLoot must be false");
        helper.assertTrue(EldritchGuardianAiLogic.IS_BOSS,
                ":169-171 — isAI() = true is pinned as the flag (no 1.21.1 seam)");
        helper.assertTrue(Math.abs(mob.getEyeHeight()) > 0.0F, "the eye height is live on the mob");
    }

    // ------------------------------------------------- heal, realm and undying

    /**
     * {@code :214-216} — the spawn heal is half the max (25 of 50), applied on
     * the mob's first tick, and {@code :177-183} regenerates only inside the
     * Outer Lands, so on the overworld pad the health must stay at the
     * spawn-heal value across two full regen cadences (BD-117; the realm leg is
     * the realm lane's pad).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianSpawnsAtHalfHealthAndDoesNotRegenOutsideTheRealm(
            final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(mob.getHealth() - 25.0F) < 1.0E-3,
                    ":214-216 — the spawn heal is (int) maxHealth / 2 = 25, got " + mob.getHealth());
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(Math.abs(mob.getHealth() - 25.0F) < 1.0E-3,
                        ":177-183 — outside the Outer Lands the guardian must not regenerate; after "
                                + "62 ticks it reads " + mob.getHealth());
                helper.succeed();
            });
        });
    }

    /**
     * {@code :155-157} + {@code :303-305} + {@code :159-161} — the air is
     * clamped above zero (the undying seam that pairs with the health guard,
     * BD-117) and fire takes hold: the guardian is not fire-immune on 1.7.10 or
     * 1.21.1.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianNeverDrownsAndAlwaysBurns(final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        mob.setAirSupply(-20);
        // 1.21.1's seam for 1.7.10's func_70015_d(seconds) is NeoForge's
        // Entity#igniteForSeconds (the same seam the guardian's own hit uses at
        // :139); the read stays getRemainingFireTicks.
        mob.igniteForSeconds(10.0F);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(mob.getAirSupply() >= 1,
                    ":303-305 — the air supply must stay above zero, got " + mob.getAirSupply());
            helper.assertTrue(mob.getRemainingFireTicks() > 0,
                    ":159-161 — the guardian burns (no immunity on either engine), fire ticks "
                            + mob.getRemainingFireTicks());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ home

    /**
     * {@code :193-196} / {@code :202-204} + the {@code :223-235} write
     * condition — a guardian with a wanted home saves HomeX/Y/Z as Ints and
     * HomeD as the distance, and a fresh instance reads them back through the
     * same keys (BD-118: the writes are Ints, the read is an Int + an Int
     * distance, and 1.21.1's {@code Mob} home is a {@code BlockPos} + float
     * radius).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianSavesAndReadsItsHome(final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        final BlockPos home = pad(helper).offset(4, 0, 4);
        mob.setHomePosition(home, 16);
        mob.setHealth(5.0F);
        helper.runAfterDelay(3, () -> {
            final CompoundTag tag = new CompoundTag();
            mob.saveWithoutId(tag);
            helper.assertTrue(tag.contains("HomeX") && tag.contains("HomeY")
                            && tag.contains("HomeZ") && tag.contains("HomeD"),
                    ":193-196 — the save must carry HomeX/Y/Z/D, the tag is " + tag);
            helper.assertTrue(tag.getInt("HomeX") == home.getX()
                            && tag.getInt("HomeY") == home.getY() && tag.getInt("HomeZ") == home.getZ(),
                    ":193-195 — the home ints must be the wanted position, got "
                            + tag.getInt("HomeX") + "," + tag.getInt("HomeY") + "," + tag.getInt("HomeZ"));

            final Entity loaded = EntityType.create(tag, helper.getLevel()).orElse(null);
            helper.assertTrue(loaded instanceof EntityEldritchGuardian,
                    "the guardian's save does not read back as a guardian but "
                            + (loaded == null ? "null" : loaded.getClass().getName()));
            final EntityEldritchGuardian reloaded = (EntityEldritchGuardian) loaded;
            helper.assertTrue(home.equals(reloaded.getHomePosition()) && reloaded.getHomeDist() == 16,
                    ":202-204 — the home must round-trip, got " + reloaded.getHomePosition()
                            + " / " + reloaded.getHomeDist());
            helper.succeed();
        });
    }

    // ----------------------------------------------------------- the orb

    /**
     * {@code :266-285} — the ranged attack on an in-range target fires
     * {@code EntityEldritchOrb} (the c4b-1 class, reused, not forked) from the
     * arm muzzle toward the target. The mob rides {@code setNoAi(true)} and the
     * goal is driven by hand through {@code start()} + {@code tick()}, so the
     * cooldown walk (the goal's 20..40-tick interval of {@code :56}) needs no
     * shared-tick dependency and the melee goal can't close the distance.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianFiresItsOrbAtItsTarget(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        final Zombie victim = new Zombie(EntityType.ZOMBIE, level);
        final BlockPos at = pad(helper).offset(10, 0, 0);
        victim.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90.0F, 0.0F);
        victim.setNoGravity(true);
        victim.clearFire();
        victim.setNoAi(true);
        level.addFreshEntity(victim);

        Goal ranged = null;
        for (final WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof AILongRangeAttack) {
                ranged = goal.getGoal();
            }
        }
        helper.assertTrue(ranged != null, ":56 — the ranged goal is not on the mob");
        mob.setNoAi(true);
        mob.setTarget(victim);
        final int before = level.getEntitiesOfClass(EntityEldritchOrb.class,
                mob.getBoundingBox().inflate(48)).size();
        ranged.start();
        for (int tick = 0; tick < 120; tick++) {
            ranged.tick();
        }
        final List<EntityEldritchOrb> orbs =
                level.getEntitiesOfClass(EntityEldritchOrb.class, mob.getBoundingBox().inflate(48));
        helper.assertTrue(orbs.size() > before,
                ":273 — the attack must fire its orb within the goal's 20..40 interval, found "
                        + (orbs.size() - before));
        final EntityEldritchOrb orb = orbs.get(orbs.size() - 1);
        helper.assertTrue(orb.position().distanceTo(mob.position()) < 24.0,
                ":273 — the orb leaves from the guardian's chest, got " + orb.position()
                        + " against " + mob.position());
        helper.succeed();
    }

    // ------------------------------------------------------- the zombie death

    /**
     * {@code :57-91} — a kill a player claims spawns exactly one {@code
     * EntityEldritchCrab} (the c4b-1 class, reused) with its helm flag set, at
     * the zombie's feet-at-eye-height, and throws the vanilla XP split of the
     * zombie's five points, which on 1.7.10's table is 3 + 2 (BD-121).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void everyClaimedZombieDeathSpawnsACrabAndTheFivePointSplit(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        final Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = pad(helper);
        killer.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        mob.hurt(level.damageSources().playerAttack(killer), 1000.0F);
        helper.runAfterDelay(2, () -> {
            final List<EntityEldritchCrab> crabs =
                    level.getEntitiesOfClass(EntityEldritchCrab.class, mob.getBoundingBox().inflate(8));
            helper.assertTrue(crabs.size() == 1,
                    ":57-62 — every death spawns exactly one crab, found " + crabs.size());
            helper.assertTrue(crabs.get(0).hasShell(),
                    ":61 — the crab spawns with its helm flag set");
            final List<ExperienceOrb> orbs =
                    level.getEntitiesOfClass(ExperienceOrb.class, mob.getBoundingBox().inflate(8));
            final List<Integer> values = new ArrayList<>();
            for (final ExperienceOrb orb : orbs) {
                values.add(orb.value);
            }
            values.sort(Integer::compareTo);
            helper.assertTrue(values.equals(List.of(2, 3)),
                    ":64-70 — the five points split as 3 + 2 on 1.7.10's table, got " + values);
            helper.succeed();
        });
    }

    /**
     * {@code :63} — the XP loop's guard is {@code (recentlyHit > 0 ||
     * isPlayer) && canDropLoot && doMobLoot}, so a kill nobody claims (a
     * generic source) throws no experience at all.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void anUnclaimedZombieDeathThrowsNoExperience(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        mob.hurt(level.damageSources().genericKill(), 1000.0F);
        helper.runAfterDelay(2, () -> {
            final List<ExperienceOrb> orbs =
                    level.getEntitiesOfClass(ExperienceOrb.class, mob.getBoundingBox().inflate(8));
            helper.assertTrue(orbs.isEmpty(),
                    ":63 — a generic kill must throw no orbs, found " + orbs.size());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------- equipment

    /**
     * {@code :37-45} with the BD-68 gate — the cultist plate ids are not in the
     * live registry (the armour is {@code not_started}), so the equipment write
     * is gated: a forced roll of 1.0 (every arm passes, including the helmet's
     * unconditional {@code :38}) equips NOTHING and throws no crash, which is
     * only true while the registry gate holds.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void thePlatesStayOffWhileTheArmourIsNotLanded(final GameTestHelper helper) {
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(InhabitedZombieAiLogic.HELM_PLATE),
                "the cultist plate items are not landed (BD-68) — this probe's premise is that gate");
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        mob.equipPlates(RandomSource.create(), 1.0F);
        helper.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR),
                ":38 — the helmet plate is not registered, so nothing may be equipped");
        helper.assertTrue(mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.AIR),
                ":40 — the chest plate is not registered, so nothing may be equipped");
        helper.assertTrue(mob.getItemBySlot(EquipmentSlot.LEGS).is(Items.AIR),
                ":44 — the leg plate is not registered, so nothing may be equipped");
        helper.succeed();
    }

    // ------------------------------------------------------------ spawn gate

    /**
     * {@code :251-261} / {@code :101-113} — the box expands 32 on x/z and 16 on
     * y and every probed cell refuses the spawn on an {@code IEldritchMob}: a
     * guardian sharing the pad with a crab refuses, and the zombie 40 blocks
     * away passes.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianRefusesAnEldritchNeighbour(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        final EntityInhabitedZombie lonely = zombie(helper, 40, 0);
        helper.assertTrue(lonely.checkSpawnRules(level, MobSpawnType.NATURAL),
                ":101-113 — an isolated inhabited zombie passes its own gate");
        helper.runAfterDelay(2, () -> {
            final EntityEldritchCrab neighbour =
                    spawn(helper, "eldritchcrab", EntityEldritchCrab.class, 2, 0);
            helper.assertTrue(neighbour.isAlive(), "the fixture crab must stay alive for the probe");
            helper.assertTrue(!mob.checkSpawnRules(level, MobSpawnType.NATURAL),
                    ":251-261 — a guardian within 32/16 of an IEldritchMob must refuse");
            helper.assertTrue(lonely.checkSpawnRules(level, MobSpawnType.NATURAL),
                    ":101-113 — the zombie 40 blocks away is outside the 32/16 box and passes");
            helper.succeed();
        });
    }

    // --------------------------------------------------------- reinforcements

    /**
     * BD-120 — the vanilla reinforcement pass runs on the {@code
     * SPAWN_REINFORCEMENTS_CHANCE} attribute, which the 1.7.10 class pins to
     * 0.0; 1.21.1's {@code Zombie} randomizes it at spawn, and the port's empty
     * {@code randomizeReinforcementsChance} keeps the pin. The contrast leg is
     * the vanilla zombie's own non-zero base.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theInhabitedZombieCarriesNoReinforcementPressure(final GameTestHelper helper) {
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        final Zombie vanilla = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)) < 1.0E-9,
                "the port pins SPAWN_REINFORCEMENTS_CHANCE to 0.0, got "
                        + mob.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
        helper.assertTrue(vanilla.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) > 0.0,
                "the contrast leg: the vanilla zombie keeps its reinforcement pressure");
        helper.succeed();
    }
}
