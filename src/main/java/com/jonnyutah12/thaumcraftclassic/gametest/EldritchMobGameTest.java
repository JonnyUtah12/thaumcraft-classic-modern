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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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

    /**
     * 21.1.259 {@code ExperienceOrb.getExperienceValue} ({@code :326-340}) —
     * the splitter's table, used by the death-XP probe's invariant (the 1.7.10
     * side of that getter is a Minecraft class and is not in {@code
     * reference/}, so its buckets are not measurable here).
     */
    private static final List<Integer> PLATFORM_XP_TABLE =
            List.of(2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1);

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
        helper.succeed();
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
                ":62/:63/:64 declare three target goals, got " + targets.size() + ". "
                        + describe(targets));
        helper.succeed();
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
        helper.succeed();
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
        helper.succeed();
    }

    // ------------------------------------------------- heal, realm and undying

    /**
     * {@code :208-217} — {@code onInitialSpawn} heals half the max ({@code
     * (int) getMaxHealth / 2} = 25 of 50, {@code :211-213}) INSIDE the realm
     * gate ({@code field_73011_w.field_76574_g == Config.dimensionOuterId}), and
     * {@code :219-227} regenerates under the same gate. So the full spawn pass
     * ({@code finalizeSpawn}, the path that applies the attributes) on the
     * OVERWORLD pad leaves the guardian at max health, {@link
     * EntityEldritchGuardian#healAtSpawn()} outside the realm writes nothing,
     * and the health may only fall across two regen cadences. The +25 heal is
     * an Outer-Lands leg — the realm lane's pad (BD-117); the 25 is pinned on
     * the world-free seam from the original's own arithmetic.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theGuardianTakesNoSpawnHealOutsideTheRealmAndNeverRegens(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.NATURAL, null);
        helper.assertTrue(Math.abs(mob.getHealth() - 50.0F) < 1.0E-3,
                ":208-217 — the heal is inside the realm gate, so the full spawn pass on the "
                        + "overworld pad leaves max health (50), got " + mob.getHealth());
        mob.setHealth(10.0F);
        mob.healAtSpawn();
        helper.assertTrue(Math.abs(mob.getHealth() - 10.0F) < 1.0E-3,
                ":208-213 — healAtSpawn() outside the Outer Lands heals nothing, got " + mob.getHealth());
        helper.assertTrue(EldritchGuardianAiLogic.spawnHeal(50.0) == 25,
                ":211-213 — the realm-side heal is (int) maxHealth / 2 = 25 of 50, got "
                        + EldritchGuardianAiLogic.spawnHeal(50.0));
        final float atSpawn = mob.getHealth();
        mob.clearFire();
        helper.runAfterDelay(62, () -> {
            helper.assertTrue(mob.getHealth() <= atSpawn,
                    ":219-227 — outside the Outer Lands the guardian must not regenerate; it read "
                            + atSpawn + " and after 62 ticks reads " + mob.getHealth());
            helper.succeed();
        });
    }

    /**
     * {@code :155-157} — {@code func_70627_aG()} returns 500, so the breath is
     * 500 TICKS, not infinite: there is no air clamp anywhere in the class
     * (the {@code :303-305} pair the first run pinned is the {@code
     * setHealth(getMaxHealth())} at {@code :304} and {@code setAirSupply(0)} at
     * {@code :305}, an initial-state write, not a per-tick clamp). On 21.1.259
     * the platform drains 1/tick with the eye in water ({@code
     * LivingEntity.java:436}, {@code decreaseAirSupply} :584-592 = {@code air -
     * 1} with no OXYGEN_BONUS attribute) and restores +4/tick out of it ({@code
     * :455-457}, {@code increaseAirSupply} :596-597 = {@code min(air + 4, max)})
     * — that restore is exactly the -20 → -4 the first run measured — and the
     * eye-in-water run reaches the drown trigger at -20 ({@code :437-448},
     * {@code hurt(drown(), 2.0F)}) on a 500-breath guardian. {@code :92-94} —
     * {@code func_98052_bS()} returns false, so fire takes hold: the type flag
     * is false and an ignite survives on the dry pad.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theGuardianBreathesForFiveHundredTicksAndBurns(final GameTestHelper helper) {
        final EntityEldritchGuardian mob = guardian(helper, 0, 0);
        helper.assertTrue(mob.getMaxAirSupply() == 500,
                ":155-157 — func_70627_aG() = 500 is the whole breath, got " + mob.getMaxAirSupply());
        helper.assertTrue(mob.getType().fireImmune() == false,
                ":92-94 — func_98052_bS() = false: the registered type carries no fire immunity");
        // 1.21.1's seam for 1.7.10's func_70015_d(seconds) is NeoForge's
        // Entity#igniteForSeconds (the same seam the guardian's own hit uses at
        // :139); the read stays getRemainingFireTicks.
        mob.setAirSupply(-20);
        mob.igniteForSeconds(10.0F);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(mob.getAirSupply() == -20 + 16,
                    "LivingEntity:455-457/596-597 — out of water the air walks back at +4/tick with no "
                            + "clamp above zero (the -20 → -4 the first run measured), got "
                            + mob.getAirSupply());
            helper.assertTrue(mob.getRemainingFireTicks() > 0,
                    ":92-94 — the guardian burns (no immunity on the type, and the fire survives on "
                            + "the dry pad), fire ticks " + mob.getRemainingFireTicks());

            // The drain legs: a 3 x 5 x 3 water cube over the mob's cell (the
            // FloatGoal of :55 drifts the mob a block or two, so the column is
            // wider than the entity box; the pad has no floor, so the probe
            // lays its own water and takes it back down).
            //
            // GameTestHelper#setBlock(BlockPos, BlockState) applies absolutePos
            // to its argument ITSELF (framework GameTestHelper.java:345-347 =
            // getLevel().setBlock(this.absolutePos(pos), state, 3)), so the
            // position handed to it is TEMPLATE-RELATIVE. Handing it the mob's
            // absolute blockPosition() — as this probe did — puts every block
            // twice the grid offset away from the pad (measured red: the air
            // never drained, 500 stayed 500, and the mob showed no buoyancy
            // because no water existed at its cell). Convert once, then work in
            // relative space.
            final BlockPos cell = mob.blockPosition();
            final BlockPos rel = helper.relativePos(cell);
            final BlockPos eyeCell = BlockPos.containing(mob.getEyePosition());
            for (int dy = -1; dy <= 3; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        helper.setBlock(rel.offset(dx, dy, dz), Blocks.WATER.defaultBlockState());
                    }
                }
            }
            helper.assertTrue(
                    helper.getLevel().getFluidState(eyeCell).is(net.minecraft.tags.FluidTags.WATER),
                    "the probe's own water must reach the mob's eye cell " + eyeCell.toShortString()
                            + " (feet " + cell.toShortString() + ", relative base "
                            + rel.toShortString() + "): the state there is "
                            + helper.getLevel().getBlockState(eyeCell));
            mob.setAirSupply(mob.getMaxAirSupply());
            helper.runAfterDelay(10, () -> {
                final boolean eyeInWater = mob.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
                helper.assertTrue(mob.getAirSupply() == 500 - 10,
                        ":155-157 — with the eye in water the air drains 1/tick (LivingEntity:436/584), "
                                + "so a full 500 reads 490 after 10 ticks (eye in water: " + eyeInWater
                                + ", at " + mob.blockPosition() + " from " + cell + "), got "
                                + mob.getAirSupply());
                mob.setAirSupply(-19);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(mob.getHealth() < 50.0F,
                            ":155-157 — the breath runs out: the drown trigger (LivingEntity:437-448) "
                                    + "hit a 500-breath guardian, health " + mob.getHealth()
                                    + " (eye in water: " + mob.isEyeInFluid(
                                            net.minecraft.tags.FluidTags.WATER) + ")");
                    for (int dy = -1; dy <= 3; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                helper.setBlock(rel.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                    mob.setAirSupply(300);
                    helper.runAfterDelay(4, () -> {
                        helper.assertTrue(mob.getAirSupply() == 300 + 16,
                                "LivingEntity:455-457 — out of water again the air walks +4/tick, got "
                                        + mob.getAirSupply());
                        helper.succeed();
                    });
                });
            });
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

            // 1.21.1's EntityType#create(TagInput, …) reads the entity id out of
            // the tag, and saveWithoutId deliberately does not write it (the
            // caller adds it), so the probe supplies the registry key.
            tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
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
        // :16-37 — shouldExecute() is the goal's precondition (live target,
        // squared distance in range); start() (:30-31) reads the target
        // straight, so driving start() past a closed canUse gate is a
        // null-target crash, not a probe. Pin the gate open, then drive it.
        helper.assertTrue(ranged.canUse(),
                ":16-37 — canUse must open for a live in-range target with the mob's own target set "
                        + "before the goal is driven by hand");
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
     * the zombie's feet-at-eye-height, and runs the XP loop of {@code :63-71}:
     * the guard, then {@code int i = func_70693_a(player); while (i > 0) { int
     * j = EntityXPOrb.func_70527_a(i); i -= j; addEntity(new EntityXPOrb(…, j))
     * }} — the WHOLE reward thrown, each orb carrying the splitter's own
     * bucket. The splitter is a Minecraft class ({@code EntityXPOrb
     * func_70527_a}) and 1.7.10's decompiled Minecraft sources are not in
     * {@code reference/}, so the bucket SIZES are the 21.1.259 platform table
     * ({@code ExperienceOrb.java:326-340}: 2477, 1237, 617, 307, 149, 73, 37,
     * 17, 7, then 3 / 1) and the loop's invariants are the original's: every
     * orb carries a table value, no orb carries zero, the values sum to the
     * reward, and the split is deterministic. The reward on the live engine is
     * the platform's 5 for the zombie type — the reference's per-plate bonus is
     * gated on registered plate ids (BD-68) — so 5 walks 3, then 2, then 1:
     * the multiset {1, 1, 3}, compared order-insensitively (the orbs are thrown
     * at one point and the census reads them in level order) (BD-121).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void everyClaimedZombieDeathSpawnsACrabAndThePlatformOrbSplit(
            final GameTestHelper helper) {
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
            helper.assertTrue(values.equals(List.of(1, 1, 3)),
                    ":63-71 — the whole 5-point reward is thrown through the platform splitter "
                            + "(ExperienceOrb.java:326-340: 5 -> 3, 2 -> 1, 1 -> 1), got " + values);
            helper.assertTrue(values.stream().mapToInt(Integer::intValue).sum() == 5,
                    ":64-69 — the loop subtracts each bucket until i is 0, so the orbs must sum to the "
                            + "reward, got " + values);
            helper.assertTrue(values.stream().allMatch(v -> v > 0),
                    ":66-69 — no orb is thrown with a zero value (the splitter's buckets are all "
                            + "positive), got " + values);
            helper.assertTrue(values.stream().allMatch(PLATFORM_XP_TABLE::contains),
                    ":66 — every orb value is a bucket of the platform's XP table, got " + values);
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
     * {@code EntityEldritchGuardian.java:251-261} and {@code
     * EntityInhabitedZombie.java:104-113} — the gate is the mob's OWN unit box
     * expanded 32 x 16 x 32 ({@code func_72314_b} = expand, both sides) and the
     * census is the mob's OWN class ({@code getEntitiesWithinAABB(
     * EntityEldritchGuardian.class, …)} / {@code EntityInhabitedZombie.class}),
     * composed as {@code ents.size() > 0 ? false : super.getCanSpawnHere()}. An
     * {@code IEldritchMob} of ANOTHER family (the c4b-1 crab) is not in that
     * census and never refuses; the first run's "every IEldritchMob refuses"
     * leg was an invented contract and is re-pinned here.
     *
     * <p>The harness packs pads ~50 blocks apart in x and ~30 in z, so a fixed
     * "isolated mob passes" leg reads a neighbouring probe's mob through a
     * 32-block box (the shape of the first run's red). Every leg here is
     * therefore either the census conditional itself (the reference's rule,
     * evaluated on the ambient census, with the census quoted in the message)
     * or a delta the rule predicts when the probe's own neighbour lands inside
     * the box.</p>
     *
     * <p>Measured at this tip, and pinned as written: the two census shapes are
     * NOT the same in the shipped code, and the guardian's is the faithful one.
     * 1.7.10's {@code func_72872_a(MyClass.class, bb)} passes NO entity to
     * exclude, so the mob's own body is inside its own expanded box and counts
     * ({@code EntityEldritchGuardian.java:253-256}); the port mirrors that with
     * {@code world.getEntitiesOfClass(EntityEldritchGuardian.class, box)} at
     * {@code EntityEldritchGuardian.java:460}, so a live guardian's verdict is
     * ALWAYS false — the gate can never open for it, which is why the original
     * spawns guardians through events, never naturally. The zombie's port
     * ({@code EntityInhabitedZombie.java:359-366}) walks
     * {@code level.getEntities(this, box)} and skips {@code this}, so the
     * zombie's gate DOES open and close on the census. That asymmetry against
     * {@code EntityInhabitedZombie.java:104-113} is BD-123; the probe pins each
     * shipped shape on its own census, never one shared helper.</p>
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theEldritchSpawnGateCensusIsPerKindAndTheGuardiansCountsItself(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityEldritchGuardian guardianMob = guardian(helper, 0, 0);
        final EntityInhabitedZombie zombieMob = zombie(helper, 6, 0);
        helper.runAfterDelay(2, () -> {
            final int guardianKind = kindInGateInclusive(level, guardianMob,
                    EntityEldritchGuardian.class);
            final int zombieKind = kindInGate(level, zombieMob, EntityInhabitedZombie.class);
            helper.assertTrue(guardianMob.checkSpawnRules(level, MobSpawnType.NATURAL)
                            == (guardianKind == 0),
                    ":253-260 — the guardian's verdict is its own class in the 32/16/32 box, and that "
                            + "census counts the mob itself (1.7.10 func_72872_a(Class, AABB) excludes "
                            + "no entity; the port ships getEntitiesOfClass at :460), so a live "
                            + "guardian refuses (census "
                            + guardianKind + ", verdict " + guardianMob.checkSpawnRules(level,
                                    MobSpawnType.NATURAL) + ", box " + gateBox(guardianMob) + ")");
            helper.assertTrue(guardianKind >= 1,
                    ":253-256 — the guardian is inside its own expanded box, so the shipped census is "
                            + "at least 1 (census " + guardianKind + ", box " + gateBox(guardianMob) + ")");
            helper.assertTrue(zombieMob.checkSpawnRules(level, MobSpawnType.NATURAL)
                            == (zombieKind == 0),
                    ":105-113 — the zombie's verdict is its own class in the 32/16/32 box (census "
                            + zombieKind + ", verdict " + zombieMob.checkSpawnRules(level,
                                    MobSpawnType.NATURAL) + ", box " + gateBox(zombieMob) + ")");

            // An IEldritchMob of the c4b-1 family, 2 blocks from the guardian:
            // :253-256 scans EntityEldritchGuardian.class, so it is invisible
            // to the census and the verdict must not move.
            final EntityEldritchCrab crab =
                    spawn(helper, "eldritchcrab", EntityEldritchCrab.class, 2, 0);
            helper.assertTrue(crab.isAlive(), "the fixture crab must stay alive for the probe");
            helper.assertTrue(!(crab instanceof com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob),
                    "the invented contract's premise is not in the original: EntityEldritchCrab declares "
                            + "no interface (its class header), IEldritchMob rides the guardian "
                            + "(reference :47) and the golem — so 'every IEldritchMob refuses' never "
                            + "applied to a crab in any reading of :251-261");
            helper.runAfterDelay(2, () -> {
                final int withCrab = kindInGateInclusive(level, guardianMob,
                        EntityEldritchGuardian.class);
                helper.assertTrue(withCrab == guardianKind,
                        ":253-256 — the census is the mob's OWN class: a crab is EntityEldritchCrab, so "
                                + "it never enters the guardian's census (census " + guardianKind
                                + " before the crab, " + withCrab + " with it in the box)");
                helper.assertTrue(guardianMob.checkSpawnRules(level, MobSpawnType.NATURAL)
                                == (withCrab == 0),
                        ":253-260 — a crab (IEldritchMob, not EntityEldritchGuardian) inside the box "
                                + "does not move the guardian's verdict; census " + withCrab);
                // The delta: a same-kind neighbour 2 blocks away is inside both
                // boxes and refuses, and the verdict returns when it is removed.
                final EntityInhabitedZombie neighbour = zombie(helper, 8, 0);
                helper.assertTrue(neighbour.isAlive(), "the fixture zombie must stay alive");
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(kindInGate(level, zombieMob, EntityInhabitedZombie.class) >= 1,
                            ":105-108 — the fixture zombie at 8,0 is inside the 32/16/32 box of 6,0");
                    helper.assertTrue(!zombieMob.checkSpawnRules(level, MobSpawnType.NATURAL),
                            ":113 — a same-kind neighbour inside the box refuses the spawn (census "
                                    + kindInGate(level, zombieMob, EntityInhabitedZombie.class) + ")");
                    neighbour.discard();
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(zombieMob.checkSpawnRules(level, MobSpawnType.NATURAL)
                                        == (kindInGate(level, zombieMob,
                                                EntityInhabitedZombie.class) == 0),
                                ":113 — the verdict is the census: with the neighbour gone the gate "
                                        + "opens again (census " + kindInGate(level, zombieMob,
                                                EntityInhabitedZombie.class) + ")");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The reference's 32/16/32 box around the mob's own box ({@code :255-258}). */
    private static AABB gateBox(final Mob mob) {
        return mob.getBoundingBox().inflate(
                EldritchGuardianAiLogic.SPAWN_BOX_XZ_EXPAND,
                EldritchGuardianAiLogic.SPAWN_BOX_Y_EXPAND,
                EldritchGuardianAiLogic.SPAWN_BOX_XZ_EXPAND);
    }

    /**
     * The mob's own class in the gate box, SELF EXCLUDED — the shape the
     * zombie's port ships ({@code EntityInhabitedZombie.java:359-366},
     * {@code level.getEntities(this, box)} + {@code entity != this}).
     */
    private static <T extends Mob> int kindInGate(final Level level, final T mob,
            final Class<T> kind) {
        int n = 0;
        for (final Entity other : level.getEntities((Entity) null, gateBox(mob),
                e -> kind.isInstance(e) && e != mob)) {
            n++;
        }
        return n;
    }

    /**
     * The same box, SELF INCLUDED — the shape 1.7.10's census has
     * ({@code func_72872_a(MyClass.class, bb)} excludes no entity,
     * guardian {@code :253-256}) and the shape the guardian's port ships
     * ({@code EntityEldritchGuardian.java:460}, {@code getEntitiesOfClass}).
     * A live mob is always >= 1 here, so its gate never opens (BD-123).
     */
    private static <T extends Mob> int kindInGateInclusive(final Level level, final T mob,
            final Class<T> kind) {
        int n = 0;
        for (final Entity other : level.getEntities((Entity) null, gateBox(mob),
                e -> kind.isInstance(e))) {
            n++;
        }
        return n;
    }

    // --------------------------------------------------------- reinforcements

    /**
     * BD-120 — {@code :24} pins the reinforcement attribute ({@code
     * field_110186_bp}) to 0.0 and {@code :36-48} returns the passed spawn data
     * WITHOUT calling {@code super.onInitialSpawn}, so the vanilla pass that
     * randomizes it never runs on this mob. Both legs are driven through the
     * spawn pass that applies the attribute (1.21.1's seam is {@code
     * Mob#finalizeSpawn} → {@code Zombie#finalizeSpawn} → {@code
     * handleAttributes} → {@code randomizeReinforcementsChance}, {@code
     * Zombie.java:552-553} = {@code setBaseValue(random.nextDouble() * 0.1F)})
     * — a bare constructed zombie reads 0.0 on both engines and proves nothing.
     * The port's override is empty ({@code EntityInhabitedZombie.java:169}), so
     * the reference's 0.0 survives the pass; the vanilla zombie's base does not.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theInhabitedZombieCarriesNoReinforcementPressure(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityInhabitedZombie mob = zombie(helper, 0, 0);
        final Zombie vanilla = new Zombie(EntityType.ZOMBIE, level);
        vanilla.moveTo(mob.position(), 0.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.NATURAL, null);
        vanilla.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.NATURAL, null);
        helper.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)) < 1.0E-9,
                ":24 + :36-48 — the full spawn pass leaves the reinforcement attribute at the "
                        + "reference's 0.0 (the port's randomizeReinforcementsChance is empty), got "
                        + mob.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
        helper.assertTrue(vanilla.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) > 0.0,
                "the contrast leg through the same spawn pass: Zombie.java:552-553 randomizes the "
                        + "vanilla base to nextDouble() * 0.1, got "
                        + vanilla.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
        helper.succeed();
    }
}
