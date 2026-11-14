package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCultists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Crimson Knight (TC-M6-06-s-c4a-1, content-mapping
 * {@code Thaumcraft:CultistKnight} (CSV line 325) mod_index 27, registration
 * {@code ConfigEntities.java:111}). The parent {@code EntityCultist} is
 * deliberately <b>unregistered</b> — the original registers Knight / Cleric /
 * Leader and no base — so the base is probed through its subclasses and through
 * the live registry, never by spawning it.
 *
 * <p>Every figure reaches the mob through the <b>live registry</b> and the
 * vanilla {@code Mob} / {@code LivingEntity} API, so the class goes RED on
 * behaviour rather than on a missing symbol. Each literal is the original's own
 * and quotes its decompiled line in
 * {@code thaumcraft.common.entities.monster.EntityCultistKnight} (74 lines) and
 * {@code EntityCultist} (110 lines); the same figures live as named constants on
 * the port's Level-free rule class {@link CultistAiLogic}, pinned world-free by
 * {@code CultistAiLogicTest}.</p>
 *
 * <p>The Cleric half of this family (its goals, its ritual byte, its orb) is
 * card {@code t_6160444c} and is not in this tree, so the probes here assert the
 * shipped roster: {@code thaumcraft:cultistknight} present,
 * {@code thaumcraft:cultistcleric} and {@code thaumcraft:cultist} absent.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CultistKnightGameTest {

    private CultistKnightGameTest() {
    }

    /** Look up an id in the live registry. The <b>key</b> is the contract: an
     *  absent id resolves to the registry default entry, so comparing the key —
     *  not a null check — is what proves registration. */
    private static EntityType<?> registered(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ")");
        return type;
    }

    /** Prove an id is NOT in the registry, naming what the id resolves to. */
    private static void absent(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(!id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " must NOT be registered but its key resolves to "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    private static EntityCultistKnight knight(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistknight").create(level);
        helper.assertTrue(spawned instanceof EntityCultistKnight,
                "thaumcraft:cultistknight did not create an EntityCultistKnight but "
                        + (spawned == null ? "null" : spawned.getClass().getName()));
        final EntityCultistKnight mob = (EntityCultistKnight) spawned;
        final var at = helper.absolutePos(net.minecraft.core.BlockPos.ZERO).offset(1, 3, 1);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        // Harness fixture, not ported behaviour: an "empty" pad has no floor, so
        // a WALKING mob falls out of the structure's ticking area (the pech card's
        // finding: 12 mob ticks across 65 probe ticks) and the pad sets it alight.
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /** {@code GoalSelector#getAvailableGoals()} is a guava Multimap (no
     *  {@code stream()}), so every goal probe reads a plain list copy. */
    private static List<WrappedGoal> goals(final Mob mob, final boolean target) {
        final List<WrappedGoal> out = new ArrayList<>();
        for (final WrappedGoal g : (target ? mob.targetSelector : mob.goalSelector).getAvailableGoals()) {
            out.add(g);
        }
        return out;
    }

    private static boolean has(final Iterable<WrappedGoal> goals, final Class<?> goal, final int slot) {
        for (final WrappedGoal wrapped : goals) {
            if (wrapped.getGoal().getClass() == goal && wrapped.getPriority() == slot) {
                return true;
            }
        }
        return false;
    }

    private static long count(final Iterable<WrappedGoal> goals, final Class<?> goal) {
        long n = 0;
        for (final WrappedGoal wrapped : goals) {
            if (wrapped.getGoal().getClass() == goal) {
                n++;
            }
        }
        return n;
    }

    private static String describe(final Iterable<WrappedGoal> goals) {
        final StringBuilder sb = new StringBuilder("goal set: ");
        for (final WrappedGoal g : goals) {
            sb.append(g.getPriority()).append('=').append(g.getGoal().getClass().getSimpleName()).append(' ');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ the roster

    /**
     * {@code ConfigEntities.java:111} — {@code registerModEntity(EntityCultistKnight.class,
     * "CultistKnight", 27, instance, 64, 3, true)}: the box is the base's
     * {@code func_70105_a(0.6F, 1.8F)} ({@code EntityCultistKnight:20}), the
     * tracking range is 64 blocks = 4 chunks (1.21.1 counts chunks — the family
     * rule pinned at {@code BrainyZombieAiLogicTest:38}) and the update interval
     * is 3 ticks.
     */
    @GameTest(template = "empty")
    public static void knightIsRegisteredWithTheOriginalBoxAndTracking(final GameTestHelper helper) {
        final EntityType<?> type = registered(helper, "cultistknight");
        helper.assertTrue(Math.abs(type.getDimensions().width() - CultistAiLogic.WIDTH) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - CultistAiLogic.HEIGHT) < 1.0E-4F,
                "the knight's hitbox is not the original's 0.6 x 1.8 (EntityCultistKnight.java:20) but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(type.getCategory() == MobCategory.MONSTER,
                "the knight is not a MONSTER-category mob (the family rule for a hostile mob) but "
                        + type.getCategory());
        helper.assertTrue(type.clientTrackingRange() == CultistAiLogic.TRACK_RANGE_CHUNKS
                        && type.updateInterval() == CultistAiLogic.UPDATE_INTERVAL,
                "the knight's tracking triple is not 64 blocks / 3 ticks (ConfigEntities.java:111) but "
                        + type.clientTrackingRange() + " chunks / " + type.updateInterval() + " ticks");
        helper.assertTrue(CatalogCultists.CULTIST_KNIGHT.get() == type,
                "the catalog's CULTIST_KNIGHT holder is not the registered type");
        helper.succeed();
    }

    /**
     * THE REGISTRATION ROSTER, MEASURED. The original's {@code
     * ConfigEntities.init} registers CultistKnight (27, {@code :111}),
     * CultistCleric (28, {@code :113}) and CultistLeader (29, {@code :115}) — and
     * <b>no {@code EntityCultist} base</b>. This card ships the knight alone, so
     * on this tree the shipped thaumcraft cultist ids are exactly
     * {@code [cultistknight]}: the base stays unregistered (the design the card
     * records, {@link CultistAiLogic#BASE_IS_REGISTERED}) and the Cleric is the
     * child card's (t_6160444c). The probe asserts all three halves so the day
     * the Cleric lands this says so by name.
     */
    @GameTest(template = "empty")
    public static void theBaseIsUnregisteredAndTheKnightIsTheOnlyCultistId(final GameTestHelper helper) {
        helper.assertTrue(CultistAiLogic.BASE_IS_REGISTERED == false,
                "the card's own statement is that the base is unregistered; the constant says "
                        + CultistAiLogic.BASE_IS_REGISTERED);
        // The base has no id at all.
        absent(helper, "cultist");
        absent(helper, "cultistcleric");
        // The base is abstract, so it cannot even be instantiated as a spawnable mob.
        helper.assertTrue(java.lang.reflect.Modifier.isAbstract(EntityCultist.class.getModifiers()),
                "EntityCultist must stay abstract: the original registers no base, so an "
                        + "instantiable base invites a registration row");
        // The shipped roster, read from the live registry — an exact set, so an
        // extra id names itself here.
        final List<String> shipped = new ArrayList<>();
        for (final EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (CultistAiLogic.CULTIST_TYPE_PATHS.contains(id.getPath()) && "thaumcraft".equals(id.getNamespace())) {
                shipped.add(id.getPath());
            }
        }
        helper.assertTrue(shipped.equals(List.of("cultistknight")),
                "the shipped cultist ids must be exactly [cultistknight] on this card — found " + shipped
                        + " (Cleric = t_6160444c, Leader = card c5)");
        helper.succeed();
    }

    // ---------------------------------------------------------------- attributes

    /**
     * {@code EntityCultist:26-28} — the base sets exactly three attributes over
     * the monster baseline: follow range 32, move speed 0.3, attack damage 4.0.
     * {@code EntityCultistKnight:41} adds max health 36.0 and nothing else, so
     * the knight's armour is the monster baseline (2.0) — the original's knight
     * adds no armour, unlike the Cleric's {@code :51}.
     */
    @GameTest(template = "empty")
    public static void knightCarriesTheBaseAttributesAndTheKnightHealth(final GameTestHelper helper) {
        final EntityCultistKnight knight = knight(helper);
        helper.assertTrue(knight.getAttributeValue(Attributes.MAX_HEALTH) == CultistAiLogic.KNIGHT_MAX_HEALTH,
                "the knight's health is not the original's 36.0 (EntityCultistKnight.java:41) but "
                        + knight.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(knight.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == CultistAiLogic.FOLLOW_RANGE,
                ":26 — follow range must be 32.0, got "
                        + knight.getAttributeBaseValue(Attributes.FOLLOW_RANGE));
        helper.assertTrue(Math.abs(knight.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                        - CultistAiLogic.MOVEMENT_SPEED) < 1.0E-6,
                ":27 — move speed must be 0.3, got " + knight.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(knight.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == CultistAiLogic.ATTACK_DAMAGE,
                ":28 — attack damage must be 4.0, got "
                        + knight.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        // The knight's armour is the PLATFORM monster baseline, measured, not a
        // remembered figure: 21.1.259's Monster.createMonsterAttributes() (target
        // jar, decompiled) is Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE)
        // — no ARMOR term — so the living baseline (LivingEntity.createLivingAttributes,
        // ARMOR at its registered default) is what :41 leaves standing, and :41 adds
        // only MAX_HEALTH. 1.7.10's mob armour figure is a vanilla EntityMob baseline
        // the mod-only reference tree cannot cite, and the knight's real defence is
        // the plate of :46-49, unregistered on this lane (BD-68). Recorded as BD-95.
        final double armourBaseline = net.minecraft.world.entity.monster.Monster
                .createMonsterAttributes().build().getBaseValue(Attributes.ARMOR);
        helper.assertTrue(knight.getAttributeBaseValue(Attributes.ARMOR) == armourBaseline,
                "the knight adds no armour (:41 adds only MAX_HEALTH), so its armour must equal the "
                        + "platform monster baseline " + armourBaseline + "; got "
                        + knight.getAttributeBaseValue(Attributes.ARMOR));
        // The attribute builder adds exactly three to the baseline: a count probe,
        // because a fourth value would mean the port invented a stat.
        helper.assertTrue(CultistAiLogic.FOLLOW_RANGE == 32.0
                        && CultistAiLogic.MOVEMENT_SPEED == 0.3
                        && CultistAiLogic.ATTACK_DAMAGE == 4.0,
                "the base's three attribute constants are not the original's 32.0 / 0.3 / 4.0");
        knight.discard();
        helper.succeed();
    }

    // -------------------------------------------------------------------- the goals

    /**
     * {@code EntityCultistKnight:25-35} — {@code func_70622_b}, slot for slot:
     *
     * <pre>
     *   0 EntityAISwimming                    → FloatGoal
     *   3 AIAttackOnCollide(EntityLivingBase) → MeleeAttackGoal (1.0, false)
     *   4 EntityAIRestrictOpenDoor            → OpenDoorGoal(this, false)   (BD: no twin)
     *   5 EntityAIOpenDoor(this, true)        → OpenDoorGoal(this, true)
     *   6 EntityAIMoveTowardsRestriction(0.8) → MoveTowardsRestrictionGoal
     *   7 EntityAIWander(0.8)                 → RandomStrollGoal
     *   8 EntityAIWatchClosest(Player, 8.0F)  → LookAtPlayerGoal (0.02)
     *   8 EntityAILookIdle                    → RandomLookAroundGoal
     *   target 1 AICultistHurtByTarget(this, true)
     *   target 2 EntityAINearestAttackableTarget(Player, 0, true)
     * </pre>
     *
     * <p>The goal selector is <b>exactly</b> those eight and the target selector
     * exactly those two: the altar-ward goal ({@code AIAltarFocus}, priority 7,
     * cleric slot 1) is the Cleric's goal and is not installed on the knight, so
     * its absence is part of the contract.</p>
     */
    @GameTest(template = "empty")
    public static void knightGoalSetIsTheOriginalSlots(final GameTestHelper helper) {
        final EntityCultistKnight knight = knight(helper);
        final List<WrappedGoal> goals = goals(knight, false);
        helper.assertTrue(has(goals, FloatGoal.class, CultistAiLogic.GOAL_SLOT_SWIM),
                "no swim goal at slot 0 (EntityCultistKnight.java:26 EntityAISwimming) — " + describe(goals));
        helper.assertTrue(has(goals, MeleeAttackGoal.class, CultistAiLogic.GOAL_SLOT_MELEE),
                ":27 — no melee goal at slot 3 (AIAttackOnCollide, 1.0, false) — " + describe(goals));
        helper.assertTrue(has(goals, OpenDoorGoal.class, CultistAiLogic.GOAL_SLOT_DOOR_RESTRICT),
                ":28 — no restrict-open-door stand-in at slot 4 — " + describe(goals));
        helper.assertTrue(has(goals, OpenDoorGoal.class, CultistAiLogic.GOAL_SLOT_DOOR_OPEN),
                ":29 — no open-door goal at slot 5 — " + describe(goals));
        helper.assertTrue(count(goals, OpenDoorGoal.class) == 2,
                "the door pair of :28-:29 is not both installed — " + describe(goals));
        helper.assertTrue(has(goals, MoveTowardsRestrictionGoal.class, CultistAiLogic.GOAL_SLOT_RESTRICTION),
                ":30 — no move-towards-restriction goal at slot 6 — " + describe(goals));
        helper.assertTrue(has(goals, RandomStrollGoal.class, CultistAiLogic.GOAL_SLOT_WANDER),
                ":31 — no wander goal at slot 7 — " + describe(goals));
        helper.assertTrue(has(goals, LookAtPlayerGoal.class, CultistAiLogic.GOAL_SLOT_WATCH),
                ":32 — no watch-closest goal at slot 8 — " + describe(goals));
        helper.assertTrue(has(goals, RandomLookAroundGoal.class, CultistAiLogic.GOAL_SLOT_LOOK_IDLE),
                ":33 — no look-idle goal at slot 8 — " + describe(goals));
        helper.assertTrue(goals.size() == 8,
                "the knight's goal selector is not the original's eight goals of :26-:33 but "
                        + goals.size() + " — " + describe(goals));

        final List<WrappedGoal> targets = goals(knight, true);
        helper.assertTrue(targets.size() == 2,
                "the target selector is not the original's pair of :34-:35 but " + targets.size()
                        + " — " + describe(targets));
        helper.assertTrue(has(targets, AICultistHurtByTarget.class, CultistAiLogic.TARGET_SLOT_HURT),
                ":34 — the family hurt-by-target goal is missing from target slot 1 — " + describe(targets));
        // The original's AICultistHurtByTarget extends EntityAIAttackEntity's
        // family, not vanilla's; 1.21.1's twin is HurtByTargetGoal, so the
        // parent-class chain is the assertion that the port kept the shape.
        helper.assertTrue(targets.stream().anyMatch(g -> g.getGoal() instanceof HurtByTargetGoal),
                "the hurt goal is not 1.21.1's HurtByTargetGoal twin — " + describe(targets));
        helper.assertTrue(has(targets, NearestAttackableTargetGoal.class, CultistAiLogic.TARGET_SLOT_PLAYER),
                ":35 — no nearest-player target goal at target slot 2 — " + describe(targets));
        helper.succeed();
    }

    /**
     * The attackability contract, both directions.
     *
     * <p>{@code EntityCultist:101-103} — {@code func_142014_c(EntityLivingBase)}
     * returns true for a cultist, so the gang's aggro-sharing works;
     * {@code :105-107} — {@code func_70686_a(Class)} returns false for the
     * Cleric, the Leader and the Knight, so the target selectors never pick their
     * own kind (the exclusion reads the registry id — BD-87's carrier, since the
     * Cleric and Leader classes are not in this tree).</p>
     *
     * <p>And the mob is damageable through the vanilla damage path: a player hit
     * moves its health, which is what every "attackable" claim in the ledger
     * means in a headless harness.</p>
     */
    @GameTest(template = "empty")
    public static void knightAdmitsTheFactionAndIsAttackable(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistKnight a = knight(helper);
        final EntityCultistKnight b = knight(helper);
        // :101-103 — canAttack admits a faction member.
        helper.assertTrue(a.canAttack(b),
                ":101-103 — a cultist must be able to attack a cultist (the gang call depends on it)");
        helper.assertTrue(a.canAttack(new Zombie(EntityType.ZOMBIE, level)),
                "a cultist must still attack a vanilla monster");
        // :105-107 — the selectors skip their own kind.
        helper.assertTrue(!a.canAttackType(registered(helper, "cultistknight")),
                ":105-107 — canAttackType must reject the Knight's own type");
        helper.assertTrue(CultistAiLogic.isCultistTypeId(EntityCultistKnight.TYPE_ID)
                        && CultistAiLogic.isCultistTypeId(
                                ResourceLocation.parse("thaumcraft:cultistcleric"))
                        && CultistAiLogic.isCultistTypeId(
                                ResourceLocation.parse("thaumcraft:cultistleader"))
                        && !CultistAiLogic.isCultistTypeId(
                                ResourceLocation.withDefaultNamespace("zombie")),
                "the :105-107 exclusion set must be the three registered cultists, carried by id "
                        + "(BD-87) — " + CultistAiLogic.CULTIST_TYPE_PATHS);
        helper.assertTrue(!a.isCultistType(EntityType.ZOMBIE),
                "a zombie must not read as a cultist type");
        // Damage lands: the vanilla damage path is what the ledger's "attackable"
        // row means, and the pad's on-fire state is excluded by clearFire().
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // The house pattern (the pech card): makeMockPlayer does NOT place the
        // player, and every distance gate on the damage/goal path uses its position.
        player.setPos(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        a.clearFire();
        final float before = a.getHealth();
        a.hurt(level.damageSources().playerAttack(player), 4.0F);
        helper.assertTrue(a.getHealth() < before,
                "a player hit did not damage the knight (health " + before + " -> " + a.getHealth() + ")");

        // :35 — the live arm is NOT measurable in this harness, and the probe says
        // so rather than passing vacuously. 1.21.1's NearestAttackableTargetGoal#findTarget
        // (target jar, decompiled :58-75) routes a Player-typed goal to
        // `level.getNearestPlayer(conditions, mob, x, eyeY, z)` (:74), and
        // ServerLevel#getNearestPlayer walks the ServerPlayer list. The game-test
        // server has no connected player and GameTestHelper.makeMockPlayer never
        // joins one — the fleet's Outer Lands row records the same wall for the
        // portal's travel leg (BD-97) — so the scan below is EMPTY and the goal can
        // never arm here, whatever the mob does. What the harness CAN measure is
        // asserted; the arm itself is pinned at rule and goal-shape level, and
        // CultistAiLogicTest pins the selection rule world-free.
        final List<Player> inLevel = level.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(a.position(), a.position())
                        .inflate(CultistAiLogic.FOLLOW_RANGE));
        helper.assertTrue(inLevel.isEmpty(),
                "PROBE SHAPE: the harness now has a level-resident player, so the :35 arm became "
                        + "measurable — move this probe to measure the acquisition (BD-97)");
        helper.assertTrue(a.canAttack(player),
                ":101-103 — the knight must be attackable-targetable by the player the :35 goal hunts, "
                        + "or the goal's own TargetGoal#canAttack leg would refuse it");
        helper.assertTrue(a.getAttributeValue(Attributes.FOLLOW_RANGE) == CultistAiLogic.FOLLOW_RANGE,
                ":32-33 — the :35 goal selects inside the follow range (the ctor builds "
                        + "`forCombat().range(getFollowDistance())`, decompiled :42), so 32.0 must stand; got "
                        + a.getAttributeValue(Attributes.FOLLOW_RANGE));
        helper.assertTrue(goals(a, true).stream()
                        .anyMatch(g -> g.getGoal() instanceof NearestAttackableTargetGoal),
                ":35 — the player target goal must sit in the goal selector (its presence is the "
                        + "measurable half of this line on this harness)");
        a.discard();
        b.discard();
        helper.succeed();
    }

    /**
     * The gang call, live in the world. {@code AICultistHurtByTarget:29-43} — a
     * hurt cultist re-shares the attacker with the faction; {@code :35-43} the
     * help box is the unit cube at its feet expanded by the follow range (32) on
     * x/z and 10.0 on y; {@code :44-50} every cultist in the box that is not the
     * hurt one and has <b>no target of its own</b> takes the attacker. The Knight
     * passes {@code agressors = true} ({@code EntityCultistKnight:34}), so the
     * attacker also lands in the hurt mob's own table.
     *
     * <p>The Cleric's {@code :48-53} ritual arm is pinned on the registry id
     * while the class is on t_6160444c (BD-87), so on this tree every shipped
     * cultist takes the {@code :49-50} arm verbatim — which is what this probe
     * measures.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void theHurtGangCallSharesTheAttackerWithEveryIdleCultistInTheBox(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistKnight hurt = knight(helper);
        final EntityCultistKnight gang = knight(helper);
        final EntityCultistKnight busy = knight(helper);
        final Zombie other = new Zombie(EntityType.ZOMBIE, level);
        other.moveTo(busy.getX(), busy.getY(), busy.getZ() + 6.5, 0.0F, 0.0F);
        other.setNoGravity(true);
        level.addFreshEntity(other);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // The house pattern (the pech card): makeMockPlayer does NOT place the player,
        // and the goal's suitability test runs inside TargetGoal#canAttack, whose
        // conditions carry the mob's FOLLOW RANGE (the target jar's decompiled
        // HurtByTargetGoal:42 builds `forCombat().range(getFollowDistance())`), so an
        // unplaced mock player sits 44 blocks away in the template's void and the goal
        // never arms. The probe puts the attacker at the hurt knight's feet, where the
        // range leg is inert. What the range leg never explained is the other gate:
        // the platform's `universal_anger` clause (BD-98) refused a PLAYER attacker
        // outright, which is exactly the attacker this call exists for.
        player.setPos(hurt.getX() + 0.5, hurt.getY(), hurt.getZ() + 0.5);
        // :45-47 — a cultist that already has a target does not join.
        busy.setTarget(other);
        helper.assertTrue(busy.getTarget() == other,
                "PROBE BROKEN: the harness could not give the busy knight a target, so the :45-47"
                        + " arm is unmeasured");
        final double range = CultistAiLogic.FOLLOW_RANGE;
        helper.assertTrue(hurt.position().distanceToSqr(gang.position()) < range * range,
                "the probe expects its gang inside the follow-range box (range " + range + ")");
        hurt.clearFire();
        // Harness timing, not behaviour. `LivingEntity#setLastHurtByMob` stamps the
        // hit with the MOBS OWN tickCount, and a freshly spawned mob is at 0 — which
        // is exactly the goal's initial `timestamp`. Vanilla's gate (`stamp ==
        // timestamp` → already consumed; the original's own mob-tick gate at :20)
        // then reads the hit as consumed for the whole probe and no target is ever
        // shared. Real play never hurts a mob that has never ticked, so the probe
        // fires the hit once the mob's own clock is live.
        final Runnable fireTheHit = () -> {
            hurt.clearFire();
            hurt.hurt(level.damageSources().playerAttack(player), 3.0F);               // :29
            helper.assertTrue(hurt.getLastHurtByMob() != null
                            && hurt.getLastHurtByMobTimestamp() > 0,
                    "the probe's hit must land on a live mob clock, or the goal's :20 gate reads it"
                            + " as already consumed (lastHurtByMob = " + hurt.getLastHurtByMob()
                            + ", timestamp = " + hurt.getLastHurtByMobTimestamp()
                            + ", mob tickCount = " + hurt.tickCount + ")");
        };
        if (hurt.tickCount <= 0) {
            helper.runAfterDelay(20, fireTheHit);
        } else {
            fireTheHit.run();
        }
        // The gang call lives in the goal's start(), and GoalSelector only reaches
        // start() from the mob's OWN AI cadence — the Mind Spider card's finding
        // (driving mob.tick() in a loop inside one probe tick exercises the engine
        // once at most; TargetGoal throttles on level().getGameTime()), so this
        // probe polls the level's ticks and latches.
        helper.succeedWhen(() -> {
            hurt.clearFire();
            gang.clearFire();
            busy.clearFire();
            helper.assertTrue(gang.getTarget() == player,
                    ":44-50 — the idle cultist in the help box has not taken the attacker as its target"
                            + " (target = " + gang.getTarget() + ", hurt tickCount = " + hurt.tickCount
                            + ", lastHurtByMob = " + hurt.getLastHurtByMob() + ", probeTick = "
                            + helper.getTick() + ")");
            helper.assertTrue(busy.getTarget() == other,
                    ":45-47 — the cultist with its own target was re-aimed at the attacker");
            // The agressors flag of :34 — the hurt knight records the attacker in
            // its own table (the goal's start(), not the gang loop).
            helper.assertTrue(hurt.getTarget() == player,
                    ":34 (agressors=true) — the hurt knight did not take the attacker (target = "
                            + hurt.getTarget() + ")");
            hurt.discard();
            gang.discard();
            busy.setTarget(null);
            busy.discard();
            other.discard();
        });
    }

    // ------------------------------------------------------------------ equipment

    /**
     * {@code EntityCultistKnight:45-64} — {@code func_70634_aV} reached through
     * 1.21.1's {@code Mob#populateDefaultEquipmentSlots} in the {@code
     * finalizeSpawn} pass.
     *
     * <p>The plate ({@code :46-49}) is unconditional — and every plate id is a
     * Thaumcraft item that is not registered on this tree, so the slots stay
     * EMPTY (BD-68's shape: the port never invents an item for an unregistered
     * id). The weapon roll is the live part: non-elite knights
     * ({@code rand() >= 0.01F}, {@code :50}) take the <b>iron sword</b>
     * ({@code :62}, the vanilla stand-in for the unregistered {@code
     * thaumcraft:questsword}); the elite branches' void / thaumium swords
     * ({@code :52} / {@code :56}) are unregistered too, so no draw may produce
     * an invented weapon.</p>
     *
     * <p>The probe drives the pass 100 times: with the elite chance at 0.01 and
     * the plate ids unregistered, "every draw lands the iron sword" is the
     * measured shipped behaviour, and the rule constants are asserted separately
     * so the elite shape is not lost with the unregistered items.</p>
     */
    @GameTest(template = "empty")
    public static void theSpawnPassHandsOutTheIronSwordAndSkipsTheUnregisteredPlate(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final var difficulty = new net.minecraft.world.DifficultyInstance(
                level.getDifficulty(), level.getDayTime(), 0L, 0.0F);
        int swords = 0;
        int elite = 0;
        for (int i = 0; i < 100; i++) {
            final EntityCultistKnight knight = knight(helper);
            knight.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);       // :45
            final ItemStack mainhand = knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            final ResourceLocation id = mainhand.isEmpty() ? null
                    : BuiltInRegistries.ITEM.getKey(mainhand.getItem());
            if (mainhand.is(Items.IRON_SWORD)) {
                swords++;                                                              // :62
            } else if (mainhand.isEmpty()) {
                elite++;                                        // :50-59 — every elite sword id of
                                                                 // :52/:53/:56 is unregistered, so an
                                                                 // elite draw leaves the hand empty
                                                                 // (BD-68), it may not invent one.
            } else {
                helper.assertTrue(false,
                        "the spawn pass handed the knight " + id + "; the shipped sword is the iron"
                                + " stand-in of the unregistered thaumcraft:questsword (:62), and the"
                                + " elite void / thaumium swords of :52/:56 are unregistered too, so"
                                + " no draw may produce anything else");
            }
            helper.assertTrue(knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                            && knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()
                            && knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).isEmpty()
                            && knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty(),
                    ":46-49 — every plate id is unregistered, so the armour slots must stay empty (BD-68)");
            knight.discard();
        }
        // :50 — off HARD the elite chance is 0.01, so 100 draws carry mean 1.0 elite
        // draws (sd 1.0) and 0.99^100 = 36.6% of runs carry none. "Every draw is an
        // iron sword" was therefore an RNG cliff that reddened 63 runs in 100 — the
        // gates are the ones the arithmetic supports: iron carries the draw set
        // (floor 90), the elite set stays inside a ~7 sd band (ceiling 8), and the
        // else-branch above fails the probe the day a weapon gets invented.
        helper.assertTrue(swords + elite == 100, "the spawn pass ran " + (swords + elite) + " of 100 times");
        helper.assertTrue(swords >= 90 && elite <= 8,
                "100 spawn passes: " + swords + " iron (:62), " + elite + " elite-empty (:50-59) — the"
                        + " original's 0.01 elite chance puts the elite count at mean 1.0 sd 1.0, so 8"
                        + " is a ~7 sd ceiling and 90 is the matching iron floor");
        // The elite shape, pinned as data because its items are not in this tree.
        helper.assertTrue(CultistAiLogic.knightIsElite(0.049F, true) && !CultistAiLogic.knightIsElite(0.05F, true)
                        && CultistAiLogic.knightIsElite(0.009F, false) && !CultistAiLogic.knightIsElite(0.01F, false),
                ":50 — the elite chance must be 0.05 on HARD and 0.01 elsewhere, as a strict <");
        helper.assertTrue(CultistAiLogic.knightCarriesVoidSword(0) && !CultistAiLogic.knightCarriesVoidSword(1),
                ":51-52 — nextInt(5) roll 0 takes the void sword, anything else the thaumium");
        helper.assertTrue(CultistAiLogic.knightPlate().size() == 4
                        && CultistAiLogic.knightPlate().values().stream()
                                .allMatch(id -> !BuiltInRegistries.ITEM.containsKey(id)),
                ":46-49 — the knight's plate table is the four unregistered cultist-plate ids — "
                        + CultistAiLogic.knightPlate());
        helper.succeed();
    }

    /**
     * {@code EntityCultistKnight:66-72} — {@code func_70640_b(DifficultyInstance,
     * Random)}: with a held item, on {@code rand() < 0.25F * light} where
     * <b>light is the light at the knight's feet</b> ({@code :68},
     * {@code world.func_147462_b(x, y, z)} = {@code getLightFromNeighbors}), the
     * weapon is enchanted at {@code (int)(5.0F + light * rand.nextInt(18))}
     * ({@code :70}).
     *
     * <p>The probe drives 120 spawn passes on the pad, where the measured raw
     * brightness with the 10-darkening allowance is bright enough that the gate
     * passes, and asserts the enchantment lands at least once. The rule
     * constants carry the shape the pad cannot exercise: the gate is a strict
     * {@code <} against {@code 0.25 * light}, and light 0 never enchants.</p>
     */
    @GameTest(template = "empty")
    public static void theWeaponEnchantmentIsGatedOnTheLightAtTheKnightsFeet(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final var difficulty = new net.minecraft.world.DifficultyInstance(
                level.getDifficulty(), level.getDayTime(), 0L, 0.0F);
        // The measured pad light, printed so the gate's behaviour here is auditable.
        final EntityCultistKnight probe = knight(helper);
        probe.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);
        final int light = level.getRawBrightness(probe.blockPosition(), 10);
        ThaumcraftClassic.LOGGER.info("[TC-CULTIST-s] pad light at the knight's feet = {}", light);
        helper.assertTrue(light > 0,
                "the probe expects a lit pad (the gate needs non-zero light to be exercised); got " + light);
        int enchanted = 0;
        int weapons = 0;
        int elite = 0;
        for (int i = 0; i < 120; i++) {
            final EntityCultistKnight knight = knight(helper);
            knight.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);       // :66-72
            final ItemStack weapon = knight.getItemBySlot(
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            if (weapon.isEmpty()) {
                // :50-59 — an elite draw's sword ids (:52 swordvoid, :56 swordthaumium)
                // are unregistered content, so the hand is EMPTY (BD-68) and the
                // original's own :67 `getHeldItem() != null` gate skips the enchant.
                // Asserting a weapon on every draw was an RNG cliff: 0.01 off HARD means
                // ~1.2 elite draws in 120 and 70 runs in 100 carry at least one.
                elite++;
            } else {
                weapons++;
                if (weapon.isEnchanted()) {
                    enchanted++;
                }
            }
            knight.discard();
        }
        helper.assertTrue(weapons >= 100 && elite <= 12,
                ":50 — off HARD the elite chance is 0.01: mean 1.2 empty hands in 120 (sd 1.1), so 12 is"
                        + " a ~10 sd ceiling; got " + weapons + " weapon draws and " + elite + " elite-empty");
        helper.assertTrue(enchanted > 0,
                "120 spawn passes on a lit pad (light " + light + ") handed " + weapons
                        + " weapons and enchanted " + enchanted + " of them — the :69-70 leg is not live"
                        + " (gate rand() < 0.25 * light, level (int)(5.0 + light * nextInt(18)))");
        // The shape the pad cannot reach, pinned as data.
        helper.assertTrue(CultistAiLogic.knightEnchantsWeapon(0.249F, 1.0F)
                        && !CultistAiLogic.knightEnchantsWeapon(0.25F, 1.0F)
                        && !CultistAiLogic.knightEnchantsWeapon(0.0F, 0.0F),
                ":69 — the gate is rand() < 0.25F * light: light 1.0 must pass at 0.249, fail at 0.25,"
                        + " and light 0 must never pass");
        helper.assertTrue(CultistAiLogic.knightEnchantLevel(1.0F, 0) == 5
                        && CultistAiLogic.knightEnchantLevel(1.0F, 17) == 22
                        && CultistAiLogic.knightEnchantLevel(4.0F, 0) == 5,
                ":70 — the level figure is (int)(5.0F + light * nextInt(18)) — got "
                        + CultistAiLogic.knightEnchantLevel(1.0F, 17) + " for light 1.0 / roll 17");
        probe.discard();
        helper.succeed();
    }

    // ----------------------------------------------------------------- the drops

    /**
     * {@code EntityCultist:47-58} — {@code func_70106_b}/{@code dropSubsequent
     * ServerDeath}: on {@code nextInt(10)} the knight drops a
     * {@code thaumcraft:itemlootbag} meta 9, and on HARD, meta 17
     * ({@code :52}); a creative-instant-kill gift drops meta 18 at the
     * {@code 3 + looting} baseline ({@code :54}, {@code :60-62}). The drop is a
     * real world drop because {@code itemlootbag} IS registered (unlike the
     * plate ids), so the probe drives the roll seam in the world and counts the
     * drops.
     */
    @GameTest(template = "empty")
    public static void theDeathRollDropsTheLootBagMetaOfTheOriginal(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistKnight knight = knight(helper);
        helper.assertTrue(CultistAiLogic.DROP_ROLL_MODULO == 10
                        && CultistAiLogic.DROP_META_FIRST == 9
                        && CultistAiLogic.DROP_META_SECOND == 17
                        && CultistAiLogic.DROP_META_THIRD == 18
                        && CultistAiLogic.DROP_THIRD_BASE == 3,
                ":48-55 — the roll table is nextInt(10) → meta 9, HARD → 17, the 3+looting gift → 18");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(CultistAiLogic.DROP_ITEM),
                CultistAiLogic.DROP_ITEM + " must be registered for the :50 drop to be a real drop");
        final var box = new net.minecraft.world.phys.AABB(
                knight.position(), knight.position()).inflate(8.0);
        // The pad is shared with the batch's other probes, so the counts below are
        // DELTAS of a baseline scan, and stacks are counted by COUNT not by entity:
        // 160 drops of one item merge into 64-stacks, so an entity count under-reads.
        final int[] before = lootBagMetas(level, box);
        for (int i = 0; i < 400; i++) {
            knight.rollCultistDrops(level, CultistAiLogic.DROP_LOOTING_BASELINE);              // :48-55
        }
        final int[] after = lootBagMetas(level, box);
        final int m9 = after[0] - before[0];
        final int m17 = after[1] - before[1];
        final int m18 = after[2] - before[2];
        final int other = after[3] - before[3];
        final int drops = m9 + m17 + m18 + other;
        for (final net.minecraft.world.entity.item.ItemEntity item
                : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            item.discard();
        }
        // The roll table is ONE nextInt(10) read top-down: r == 0 → meta 9 (:50),
        // r <= 1 → meta 17 (:52), r <= 3 + looting → meta 18 (:54), else nothing.
        // At the shipped looting baseline (0) that is 10% / 10% / 20%, i.e. means
        // 40 / 40 / 80 in 400 rolls (sd 6.0 / 6.0 / 8.0). The bands below are ~4 sd
        // wide, so a table change — a merged branch, a shifted meta, a re-scaled
        // roll — reddens the probe, and nothing is asserted that the arithmetic
        // does not support.
        helper.assertTrue(other == 0,
                other + " drops carry a meta outside {" + CultistAiLogic.DROP_META_FIRST + ", "
                        + CultistAiLogic.DROP_META_SECOND + ", " + CultistAiLogic.DROP_META_THIRD
                        + "} — the roll table of :50/:52/:54 has no fourth meta");
        helper.assertTrue(m9 >= 16 && m9 <= 64,
                "meta " + CultistAiLogic.DROP_META_FIRST + " fired " + m9 + " times in 400 rolls — the"
                        + " original's `r == 0` branch is 10% (mean 40, sd 6, so 16..64 is a ~4 sd band)");
        helper.assertTrue(m17 >= 16 && m17 <= 64,
                "meta " + CultistAiLogic.DROP_META_SECOND + " fired " + m17 + " times in 400 rolls — the"
                        + " original's `r <= 1` branch is 10% (mean 40, sd 6, so 16..64 is a ~4 sd band)");
        helper.assertTrue(m18 >= 48 && m18 <= 112,
                "meta " + CultistAiLogic.DROP_META_THIRD + " fired " + m18 + " times in 400 rolls — the"
                        + " original's `r <= 3 + looting` branch at looting " + CultistAiLogic.DROP_THIRD_BASE
                        + " is 20% (mean 80, sd 8, so 48..112 is a ~4 sd band)");
        helper.assertTrue(drops >= 120 && drops <= 200,
                "the :48-54 roll produced " + drops + " drops in 400 rolls — the table's total is 40%"
                        + " (mean 160, sd 9.8, so 120..200 is a ~4 sd band)");
        knight.discard();
        helper.succeed();
    }

    /**
     * Count the pad's {@code thaumcraft:itemresource} drops, bucketed by the port's
     * {@code thaumcraft:item_meta} component (the carrier of 1.7.10's damage value):
     * {@code {meta 9, meta 17, meta 18, anything else}}, summed by stack COUNT because
     * drops of one meta merge into 64-stacks.
     */
    private static int[] lootBagMetas(final net.minecraft.server.level.ServerLevel level,
            final net.minecraft.world.phys.AABB box) {
        final net.minecraft.world.item.Item drop =
                BuiltInRegistries.ITEM.get(CultistAiLogic.DROP_ITEM);
        final int[] out = new int[4];
        for (final net.minecraft.world.entity.item.ItemEntity item
                : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (!item.getItem().is(drop)) {
                continue;
            }
            final Integer meta = item.getItem().get(
                    com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META);
            if (java.util.Objects.equals(meta, CultistAiLogic.DROP_META_FIRST)) {
                out[0] += item.getItem().getCount();
            } else if (java.util.Objects.equals(meta, CultistAiLogic.DROP_META_SECOND)) {
                out[1] += item.getItem().getCount();
            } else if (java.util.Objects.equals(meta, CultistAiLogic.DROP_META_THIRD)) {
                out[2] += item.getItem().getCount();
            } else {
                out[3] += item.getItem().getCount();
            }
        }
        return out;
    }
}
