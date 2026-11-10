package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityGiantBrainyZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the Angry / Furious Zombie pair (TC-M6-06-s-c2,
 * content-mapping {@code Thaumcraft:BrainyZombie} mod_index 19 and
 * {@code Thaumcraft:GiantBrainyZombie} mod_index 20).
 *
 * <p>Like the firebat probes, every figure here reaches the mob through the
 * <b>live registry</b> and the vanilla {@code Mob} / {@code LivingEntity} API,
 * so this class compiles against a tree that contains no brainy-zombie class at
 * all and goes RED on behaviour rather than on a missing symbol. Each literal
 * is the original's own and quotes its decompiled line:
 * {@code thaumcraft.common.entities.monster.EntityBrainyZombie} (54 lines) and
 * {@code EntityGiantBrainyZombie} (90 lines). The same figures live as named
 * constants on the port's Level-free rule class, pinned world-free by
 * {@code BrainyZombieAiLogicTest}.</p>
 *
 * <p>The pair's whole surface: registration and hitbox, the attribute figures
 * the originals set in {@code func_110147_ax}, the goal set (the two goals the
 * originals install themselves, and the 1.21.1-only goals they never had), the
 * giant's anger data value with its damage bump, its decay, its attack-damage
 * rewrite and its hitbox scale, and the death-drop roll structure.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZombieMobGameTest {

    private ZombieMobGameTest() {
    }

    /** Look up a mob id in the live registry. The <b>key</b> is the contract:
     *  an absent id resolves to the registry default entry, so comparing the
     *  key — not a null check — is what proves registration. */
    private static EntityType<?> registered(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ")");
        return type;
    }

    /** The column a placed mob stands in: the structure block's OWN (x, z), not
     *  an offset cell. The harness vouches exactly the chunks the template's
     *  bounding box intersects — {@code StructureUtils#prepareTestStructure}
     *  calls {@code forceLoadChunks} on that box, and {@code GameTestInfo#tick}
     *  will not start a probe until every one of those chunks answers
     *  {@code isPositionEntityTicking}. {@code "empty"} is 1×1×1, so that set is
     *  ONE chunk: a cell one block off the origin lands in a neighbour the
     *  harness never vouched whenever the structure block falls on a chunk's
     *  last row or column, and WHICH draw that is moves with the batch because
     *  {@code GameTestServer#startTests} draws the whole grid's origin from the
     *  level RNG ({@code TEST_POSITION_RANGE = 14999992}). Measured on this tree
     *  (284-test run, {@code run/logs}): a mob in the vouched column ticks 1:1
     *  with the probe ({@code tickCount == } probe tick from probe tick 1); a mob
     *  one chunk outside it is never handed to {@code ServerLevel#tickNonPassenger}
     *  and its counter stays 0 for the whole probe. A probe that reads anything
     *  the LEVEL ticks therefore has to place its mob where the LEVEL runs. */
    private static final BlockPos MOB_CELL = new BlockPos(0, 3, 0);

    private static Mob spawn(final GameTestHelper helper, final String path) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, path).create(level);
        helper.assertTrue(spawned instanceof Mob,
                "thaumcraft:" + path + " did not create a Mob instance");
        final Mob mob = (Mob) spawned;
        final BlockPos at = helper.absolutePos(MOB_CELL);              // see MOB_CELL
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        // Harness fixture, not ported behaviour: an "empty" pad has no floor, so
        // a WALKING mob falls out of the structure's ticking area and the server
        // stops ticking it (measured on this pad: 12 mob ticks across 65 probe
        // ticks, all of them before the fall). The flying mobs of the sibling
        // cards never notice; a zombie does. Gravity off keeps it in the box so
        // the goal selectors and the per-tick body run at the server's cadence —
        // and keeps it in the chunk MOB_CELL vouches (see MOB_CELL).
        mob.setNoGravity(true);
        level.addFreshEntity(mob);
        return mob;
    }

    /** An entity built from the registry, loaded from a save compound, and
     *  placed in the level — the path an existing save takes. */
    private static Mob spawnLoaded(final GameTestHelper helper, final String path,
            final CompoundTag saved) {
        final ServerLevel level = helper.getLevel();
        final Mob mob = (Mob) registered(helper, path).create(level);
        final BlockPos at = helper.absolutePos(MOB_CELL);              // see MOB_CELL
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.readAdditionalSaveData(saved);
        mob.setNoGravity(true);                                  // see spawn()
        level.addFreshEntity(mob);
        return mob;
    }

    /** The tick-list precondition, stated where it can be read: the mob's chunk
     *  is one of the chunks the harness force-loaded for this structure box, so
     *  the LEVEL has to be running it before the probe body ran at all. A
     *  harness/layout regression therefore reports HERE, by name, instead of
     *  arriving as a misread rate downstream. */
    private static void assertMobCellIsTicking(final GameTestHelper helper, final Mob mob,
            final String who) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final net.minecraft.world.level.ChunkPos cell = mob.chunkPosition();
        final int originChunkX = origin.getX() >> 4;
        final int originChunkZ = origin.getZ() >> 4;
        helper.assertTrue(level.isPositionEntityTicking(mob.blockPosition()),
                who + " stands in chunk " + cell.x + "," + cell.z + " (structure origin "
                        + origin.toShortString() + " is chunk " + originChunkX + "," + originChunkZ
                        + ", align16 " + (origin.getX() & 15) + "," + (origin.getZ() & 15)
                        + "), and that chunk is not entity-ticking: the harness force-loads and "
                        + "vouches only the chunks the template box intersects, so the mob's cell "
                        + "has left the vouched footprint (MOB_CELL / the template's size)");
    }

    // ------------------------------------------------------- registration

    @GameTest(template = "empty")
    public static void angryZombieIsRegisteredWithTheVanillaZombieBox(final GameTestHelper helper) {
        // EntityBrainyZombie never touches its size (no func_70105_a in the
        // 54-line class), so the box is the 1.7.10 zombie parent's: 0.6 x 1.95,
        // the same dimensions 1.21.1's own EntityType.ZOMBIE carries.
        final EntityType<?> type = registered(helper, "brainyzombie");
        final float zw = EntityType.ZOMBIE.getDimensions().width();
        final float zh = EntityType.ZOMBIE.getDimensions().height();
        helper.assertTrue(Math.abs(type.getDimensions().width() - zw) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - zh) < 1.0E-4F,
                "the angry zombie's hitbox is not the vanilla zombie box " + zw + " x " + zh
                        + " but " + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(Math.abs(zw - 0.6F) < 1.0E-4F && Math.abs(zh - 1.95F) < 1.0E-4F,
                "the vanilla zombie box the angry zombie inherits is not 0.6 x 1.95");
        // MobCategory.MONSTER — registerModEntity under the monster family
        // (ConfigEntities.java:95).
        helper.assertTrue(type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER,
                "thaumcraft:brainyzombie is not registered in the MONSTER category (:95)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void angryZombieCarriesTheOriginalAttributes(final GameTestHelper helper) {
        final Mob zombie = spawn(helper, "brainyzombie");
        // func_110147_ax:16 — max health 25.0
        helper.assertTrue(zombie.getAttributeValue(Attributes.MAX_HEALTH) == 25.0,
                "the angry zombie's health is not the original 25.0 but "
                        + zombie.getAttributeValue(Attributes.MAX_HEALTH) + " (EntityBrainyZombie.java:16)");
        // func_110147_ax:17 — attack damage 5.0
        helper.assertTrue(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5.0,
                "the angry zombie's attack damage is not the original 5.0 but "
                        + zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) + " (:17)");
        // func_70658_aO():35-38 — the zombie's armour value plus 3, capped at 20.
        // 1.21.1's armour value is the ARMOR attribute (the vanilla zombie base
        // is 2.0, Zombie.createAttributes:66), so the port ships 2 + 3 = 5.
        helper.assertTrue(zombie.getAttributeBaseValue(Attributes.ARMOR) == 5.0,
                "the angry zombie's armour value is not the zombie base 2.0 plus the original's +3 "
                        + "(:35-38) but " + zombie.getAttributeBaseValue(Attributes.ARMOR));
        // func_110147_ax:18 — the 1.7.10 zombie spawn-strength attribute at 0.0:
        // an angry zombie never calls for reinforcements. 1.21.1's attribute of
        // that name is SPAWN_REINFORCEMENTS_CHANCE, consumed by Zombie#hurt
        // (Zombie.java:296-298, HARD difficulty only).
        helper.assertTrue(zombie.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) == 0.0,
                "the angry zombie's reinforcement chance base is not the original 0.0 (:18) but "
                        + zombie.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
        // The pair sets no follow range and no movement speed: both stay on the
        // 1.7.10 zombie parent's figures, which 1.21.1 supplies as 35.0 / 0.23
        // (Zombie.createAttributes:63-64).
        helper.assertTrue(zombie.getAttributeValue(Attributes.FOLLOW_RANGE) == 35.0,
                "the angry zombie's follow range is not the parent's 35.0 (the pair never writes it)");
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.23) < 1.0E-4,
                "the angry zombie's move speed is not the parent's 0.23 (the pair never writes it)");
        helper.succeed();
    }

    // ------------------------------------------------------------- goals

    @GameTest(template = "empty")
    public static void angryZombieInstallsTheOriginalGoalSet(final GameTestHelper helper) {
        final Mob zombie = spawn(helper, "brainyzombie");
        // The 54-line class installs exactly one goal:
        // field_70715_bh.func_75776_a(1, new EntityAIHurtByTarget(this, false))
        // — field_70715_bh is targetTasks (fields CSV), slot 1, and the false
        // is the 1.7.10 "not passive" flag: it counter-attacks and alerts
        // nobody, which is 1.21.1's plain HurtByTargetGoal(this).
        final WrappedGoal hurt = zombie.targetSelector.getAvailableGoals().stream()
                .filter(g -> g.getGoal().getClass() == HurtByTargetGoal.class)
                .findFirst().orElse(null);
        helper.assertTrue(hurt != null,
                "the angry zombie has no plain HurtByTargetGoal — the original installs "
                        + "EntityAIHurtByTarget(this, false) at target slot 1 (:23)");
        helper.assertTrue(hurt.getPriority() == 1,
                "the angry zombie's hurt-by-target goal is not at target slot 1 (:23)");
        // The goal set is the 1.7.10 zombie parent's shape, not 1.21.1's: no
        // village patrol, no turtle-egg attack, no turtle target.
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .noneMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal),
                "the port kept 1.21.1's MoveThroughVillageGoal; 1.7.10 had no village patrol goal");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .noneMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.BreakDoorGoal),
                "the port kept 1.21.1's BreakDoorGoal; 1.7.10 had no door-breaking goal");
        // The melee / wander / look trio, at the parent's slots.
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal() instanceof MeleeAttackGoal
                                && g.getPriority() == 2),
                "the angry zombie has no melee-attack goal at the zombie parent's slot 2 "
                        + "(Zombie.java:110 — the 1.7.10 EntityAIAttackOnCollide analogue)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == WaterAvoidingRandomStrollGoal.class
                                && g.getPriority() == 7),
                "the angry zombie has no wander goal at goal slot 7 (the 1.7.10 EntityAIWander slot)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == LookAtPlayerGoal.class
                                && g.getPriority() == 8),
                "the angry zombie has no watch-closest goal at the parent's slot 8 "
                        + "(Zombie.java:104 — the 1.7.10 EntityAIWatchClosest analogue)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .noneMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.RemoveBlockGoal),
                "the port kept a turtle-egg raid goal (RemoveBlockGoal family, Zombie.java:103); "
                        + "1.7.10 had no turtle eggs");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == RandomLookAroundGoal.class
                                && g.getPriority() == 8),
                "the angry zombie has no look-idle goal at the parent's slot 8 "
                        + "(Zombie.java:105 — the 1.7.10 EntityAILookIdle analogue)");
        // Target set: the player / villager / iron-golem trio of the zombie
        // parent, plus the original's own hurt-by-target at slot 1.
        helper.assertTrue(zombie.targetSelector.getAvailableGoals().size() == 4,
                "the angry zombie's target selector is not the 4-goal 1.7.10 zombie set "
                        + "(1 hurt + player + villager + golem) but "
                        + zombie.targetSelector.getAvailableGoals().size() + " goals");
        long nearest = zombie.targetSelector.getAvailableGoals().stream()
                .filter(g -> g.getGoal() instanceof NearestAttackableTargetGoal<?>)
                .count();
        helper.assertTrue(nearest == 3,
                "the angry zombie must target exactly 3 nearest-target families "
                        + "(player / villager / iron golem) but has " + nearest);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void angryZombieTargetsTheNearestPlayer(final GameTestHelper helper) {
        final Mob zombie = spawn(helper, "brainyzombie");
        final ServerLevel level = helper.getLevel();
        // The target goal of the 1.7.10 zombie parent (the
        // EntityAINearestAttackableTarget trio: player / villager / iron golem,
        // which 1.21.1 installs at Zombie.java:114-116) must be live: a zombie
        // that never acquires a target is a zombie whose AI was stripped too
        // far. The scan walks the LEVEL's entities, so the target has to be a
        // real level entity — GameTestHelper#makeMockPlayer hands back a ServerPlayer
        // that was never added (measured: 0 players found in the pad), and a
        // vanilla target goal can never see it. A villager is the parent's own
        // target family, needs no player, and the gametest server runs at
        // NORMAL difficulty, so the monster-target gate is open.
        final Villager villager = new Villager(net.minecraft.world.entity.EntityType.VILLAGER, level);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        villager.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(villager);
        // The target is acquired by the LEVEL — the goal runs from the mob's own AI
        // tick — so this leg is a delta contract: poll the outcome, do not schedule it.
        // The old form parked the assert at getTick() + 40, which made the harness's
        // scheduler part of the port's contract; the 400-tick budget below is now a
        // watchdog only, and at the watchdog the failure that surfaces IS this
        // condition's message (GameTestSequence#tickAndFailIfNotComplete), so the
        // diagnosis survives the conversion. docs/testing.md, "assert the OUTCOME".
        helper.succeedWhen(() -> {
            // Stated first, so a layout/harness change reports the precondition by
            // name and not as a target that never arrived.
            assertMobCellIsTicking(helper, zombie, "the angry zombie");
            helper.assertTrue(zombie.getTarget() == villager,
                    "the angry zombie did not acquire the villager 3 blocks away as its target; the "
                            + "parent's nearest-target goal is not installed (target="
                            + zombie.getTarget() + ", tickCount=" + zombie.tickCount
                            + ", probeTick=" + helper.getTick()
                            + ", difficulty=" + level.getDifficulty() + ", villagers in the pad="
                            + level.getEntitiesOfClass(Villager.class, mobBox(helper)).size() + ")");
            villager.discard();
            zombie.discard();
        });
    }

    // ------------------------------------------------------- the giant

    @GameTest(template = "empty")
    public static void furiousZombieIsRegisteredAtItsOwnBaseSize(final GameTestHelper helper) {
        // func_70636_d():24 — the giant sizes itself from the 0.6 x 1.8 base,
        // multiplied by 1.2 + anger. So the registered base is 0.6 x 1.8 and a
        // fresh entity (anger 1.0, :32) stands at 0.6 x 2.2 = 1.32 wide and
        // 1.8 x 2.2 = 3.96 tall (ctor :16 multiplies the same 1.2 + anger).
        final EntityType<?> type = registered(helper, "giantbrainyzombie");
        helper.assertTrue(Math.abs(type.getDimensions().width() - 0.6F) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - 1.8F) < 1.0E-4F,
                "the furious zombie's base hitbox is not the original 0.6 x 1.8 (:24) but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        final Mob giant = spawn(helper, "giantbrainyzombie");
        final var dims = giant.getDimensions(giant.getPose());
        helper.assertTrue(Math.abs(dims.width() - 1.32F) < 1.0E-3F
                        && Math.abs(dims.height() - 3.96F) < 1.0E-3F,
                "a fresh furious zombie is not at the anger-1.0 size 1.32 x 3.96 (:16, :24, :32) but "
                        + dims.width() + " x " + dims.height());
        // 1.21.1 carries an entity's size factor as the generic SCALE attribute
        // (LivingEntity#getScale:528-530, applied to the dimensions at :3250 and
        // refreshed in aiStep at :2440-2443). The original's anger factor is that
        // same number, so the port ships it as the attribute base.
        helper.assertTrue(Math.abs(giant.getAttributeBaseValue(Attributes.SCALE) - 2.2) < 1.0E-3,
                "the furious zombie's scale attribute base is not the original 1.2 + 1.0 anger factor "
                        + "(:15-16, :32) but " + giant.getAttributeBaseValue(Attributes.SCALE));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void furiousZombieCarriesTheOriginalAttributesAndXp(final GameTestHelper helper) {
        final Mob giant = spawn(helper, "giantbrainyzombie");
        // func_110147_ax:52-53 — health 60.0, attack 7.0
        helper.assertTrue(giant.getAttributeValue(Attributes.MAX_HEALTH) == 60.0,
                "the furious zombie's health is not the original 60.0 (EntityGiantBrainyZombie.java:52) but "
                        + giant.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 7.0,
                "the furious zombie's attack base is not the original 7.0 (:53, :27) but "
                        + giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        // It inherits the angry zombie's armour value (2 + 3, EntityBrainyZombie.java:35-38)
        // and its 0.0 reinforcement chance (:18).
        helper.assertTrue(giant.getAttributeBaseValue(Attributes.ARMOR) == 5.0,
                "the furious zombie must inherit the angry zombie's armour value 5.0 (:35-38)");
        helper.assertTrue(giant.getAttributeBaseValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) == 0.0,
                "the furious zombie must inherit the angry zombie's 0.0 reinforcement chance (:18)");
        // :14 — field_70728_aV (experienceValue) = 15, the 1.21.1 xpReward
        // (Monster.java:32 sets the monster default 5; the pair overrides it).
        helper.assertTrue(giant.getExperienceReward(helper.getLevel(), null) == 15,
                "the furious zombie's experience value is not the original 15 (:14) but "
                        + giant.getExperienceReward(helper.getLevel(), null));
        // :17 — the giant's own goal: field_70714_bg (tasks) slot 2,
        // EntityAILeapAtTarget(this, 0.4F)
        final WrappedGoal leap = giant.goalSelector.getAvailableGoals().stream()
                .filter(g -> g.getGoal().getClass() == LeapAtTargetGoal.class)
                .findFirst().orElse(null);
        helper.assertTrue(leap != null,
                "the furious zombie installs no LeapAtTargetGoal (:17)");
        helper.assertTrue(leap.getPriority() == 2,
                "the furious zombie's leap goal is not at goal slot 2 (:17)");
        // It also keeps the angry zombie's target set (the pair is a subclass).
        helper.assertTrue(giant.targetSelector.getAvailableGoals().size() == 4,
                "the furious zombie must carry the angry zombie's 4-goal target set");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void furiousZombieAngerGrowsOnDamageAndRewritesItsDamage(final GameTestHelper helper) {
        final Mob giant = spawn(helper, "giantbrainyzombie");
        final ServerLevel level = helper.getLevel();
        // :44-47 — every attackEntityFrom raises the anger by 0.1 to the 2.0 cap
        // (:45), and it happens BEFORE super.attackEntityFrom, so a killing blow
        // raises it too.
        for (int i = 0; i < 20; i++) {
            giant.hurt(level.damageSources().generic(), 0.1F);
            if (giant.isDeadOrDying()) {
                giant.setHealth(60.0F);
            }
        }
        giant.clearFire();      // the pad sets mobs alight; a fire hurt re-caps the anger
        final CompoundTag hit = giant.saveWithoutId(new CompoundTag());
        helper.assertTrue(hit.contains("Anger"),
                "the furious zombie does not persist its anger value (:30-41)");
        final float angerAfter = hit.getFloat("Anger");
        helper.assertTrue(Math.abs(angerAfter - 2.0F) < 1.0E-4F,
                "after 20 hits the furious zombie's anger is not the original cap 2.0 (:45) but "
                        + angerAfter);
        // :24 and :27 ride the per-tick update. The probe FEEDS the mob its own
        // tick() — probe ticks are not a valid clock for a per-tick rate on an
        // empty pad (see spawn()) — so the mob advances by exactly the number of
        // ticks the math below assumes. The pair's own figures (1.2 + anger,
        // 7.0 + (anger - 1) * 5.0) are pinned exactly by BrainyZombieAiLogicTest;
        // here the contract is that the live box and the live attack base TRACK
        // that value (the failure this probe guards is the box sitting at the
        // anger-1.0 size 1.32 x 3.96).
        // This leg is SELF-CONTAINED: the mob's own tick() is fed by the leg below, so
        // the measurement does not read the harness's clock at all. The old form parked
        // it at getTick() + 3, which put a scheduling delay inside the contract for no
        // measurement benefit and left the batch one more way to redden the probe. Run
        // it inline: no probe tick gates it now (docs/testing.md, "assert the OUTCOME").
        final Runnable angerDelta = () -> {
            // The clock is the mob's per-tick body, driven here, and the
            // measurement is self-contained IN THIS LEG: the server ticks the
            // mobs of a ticking area from ServerLevel#tickNonPassenger (which
            // increments the mob's tickCount at :772 and calls tick() at :777),
            // and it is single-threaded, so no server tick interleaves a probe
            // leg. Anger at the top of the leg, N driven ticks, anger at the
            // bottom: the delta is exactly N x 0.002 by construction.
            final float from = giant.saveWithoutId(new CompoundTag()).getFloat("Anger");
            helper.assertTrue(from <= 2.0F && from > 1.9F,
                    "the anger after the 20 hits is off the 2.0 cap band (:45) and cannot anchor a "
                            + "decay measurement: anger " + from + " at tickCount " + giant.tickCount
                            + " hp " + giant.getHealth());
            for (int i = 0; i < 3; i++) {
                giant.clearFire();          // the pad sets mobs alight; see the decay probe
                giant.tick();
            }
            final float now = giant.saveWithoutId(new CompoundTag()).getFloat("Anger");
            // 0.002 is the ORIGINAL's literal (:23), not the port's constant —
            // see the decay probe for why the expectation may not read the
            // mutation surface.
            helper.assertTrue(Math.abs(now - (from - 3 * 0.002F))
                            < 1.0E-4F,
                    "3 driven mob ticks did not decay the anger by exactly 3 x 0.002 (:22-23): " + from
                            + " -> " + now);

            final double wantDamage = 7.0 + (now - 1.0F) * 5.0;                       // :27
            helper.assertTrue(Math.abs(giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                            - wantDamage) < 1.0E-6,
                    "the furious zombie's attack base is not 7.0 + (anger - 1) * 5.0 (:27) at anger "
                            + now + ": got " + giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                            + " want " + wantDamage);
            final double wantScale = 1.2 + now;                                       // :16, :24
            final var dims = giant.getDimensions(giant.getPose());
            helper.assertTrue(Math.abs(dims.width() - 0.6F * wantScale) < 1.0E-3F
                            && Math.abs(dims.height() - 1.8F * wantScale) < 1.0E-3F,
                    "the hitbox does not track the anger (:24): box " + dims.width() + " x "
                            + dims.height() + " at anger " + now + " wants "
                            + 0.6 * wantScale + " x " + 1.8 * wantScale
                            + " (the unscaled angry-zombie box would be 0.6 x 1.95, the "
                            + "anger-1.0 size 1.32 x 3.96)");
            giant.discard();
        };
        // The tick-list precondition, stated before the leg that depends on the pad's
        // layout, so a harness change reports the precondition by name.
        assertMobCellIsTicking(helper, giant, "the furious zombie");
        angerDelta.run();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void furiousZombieAngerDecaysAndTheBoxShrinksBack(final GameTestHelper helper) {
        // :30-33 — the anger data value (watcher index 20) starts at 1.0; a save
        // that carries 2.0 decays at 0.002 while it is above 1.0 (:22-23),
        // resizing the box from (1.2 + anger) as it goes (:24).
        //
        // The rate is measured per TICK OF THE MOB, not per probe tick. Two
        // facts about the harness make that measurable (both measured on this
        // pad): a WALKING mob on an "empty" pad falls out of the structure's
        // ticking area and the server stops ticking it (12 mob ticks across 65
        // probe ticks), so the probes keep the mob in the box with no gravity;
        // and the mob's own tickCount is incremented by ServerLevel#tickNonPassenger
        // (:772), not by Entity#tick, so a rate over driven ticks must be
        // measured INSIDE one leg, where no server tick can interleave.
        //
        // For the server to hand the mob that increment at all, the mob has to
        // stand in a chunk the harness vouches — see MOB_CELL. That is why the
        // mob is placed on the structure block's own column and why the leg
        // below asserts the cell is entity-ticking BEFORE it reads the counter:
        // a layout/harness change then reports the precondition by name instead
        // of arriving as a decay rate that cannot be measured.
        final CompoundTag saved = new CompoundTag();
        saved.putFloat("Anger", 2.0F);
        final EntityGiantBrainyZombie giant = (EntityGiantBrainyZombie)
                spawnLoaded(helper, "giantbrainyzombie", saved);
        helper.assertTrue(Math.abs(giant.getAnger() - 2.0F) < 1.0E-4F,
                "a save's Anger value did not load onto the furious zombie (:39-41)");
        // The ctor sized the box from the anger the entity was CONSTRUCTED with
        // (1.0) and 1.7.10 reads the save after construction, so the first
        // per-tick update is what resizes it to (1.2 + 2.0).
        // The expectations below are the ORIGINAL's literals
        // (reference/decompiled/.../EntityGiantBrainyZombie.java:22 floor, :23
        // decay), NOT the port's constants. An arm that reads its expected value
        // off BrainyZombieAiLogic.ANGER_DECAY_PER_TICK / ANGER_FLOOR stays GREEN
        // when those constants are mutated, because the mutation surface becomes
        // the expectation (the tautology trap). The port's constants are pinned
        // to the same literals by BrainyZombieAiLogicTest, which is where a
        // changed port value has to be caught.
        final float decayPerTick = 0.002F;                  // :23
        final float angerFloor = 1.0F;                      // :22
        final float[] base = new float[4];
        // The leg needs ONE server tick of the mob — the tick that resizes the loaded
        // fury's box to (1.2 + anger) — and that tick is the LEVEL's work, not the
        // probe's. The old form asserted at getTick() + 5, i.e. it GUESSED when the
        // level would get there: that is the shape that went red 2/2 in the
        // with-Curios CI job as "tickCount 0" while the same sha was green locally.
        // Poll for the tick instead. timeoutTicks = 600 below is a watchdog, and a
        // watchdog failure surfaces this condition's own message
        // (GameTestSequence#tickAndFailIfNotComplete), so the diagnosis survives.
        // Measured on this pad (283-test batch): the mob's tickCount tracks the probe
        // 1:1 from probe tick 1, so the leg opens one tick in and the watchdog carries
        // ~600x margin over a healthy run.
        helper.succeedWhen(() -> {
            assertMobCellIsTicking(helper, giant, "the furious zombie");
            base[0] = giant.getAnger();
            base[1] = giant.tickCount;
            helper.assertTrue(base[1] > 0.0F,
                    "the furious zombie is not in the server's entity tick list (tickCount 0): "
                            + "ServerLevel#tickNonPassenger increments the mob's counter (:772) before "
                            + "calling tick() (:777), so a zero counter means the mob sits outside the "
                            + "structure's ticking area and no per-tick rate can be read from it "
                            + "| mobChunk=" + giant.chunkPosition().x + "," + giant.chunkPosition().z
                            + " originChunk=" + (helper.absolutePos(BlockPos.ZERO).getX() >> 4) + ","
                            + (helper.absolutePos(BlockPos.ZERO).getZ() >> 4)
                            + " originAlign16=" + (helper.absolutePos(BlockPos.ZERO).getX() & 15)
                            + "," + (helper.absolutePos(BlockPos.ZERO).getZ() & 15)
                            + " cellEntityTicking=" + helper.getLevel()
                                    .isPositionEntityTicking(giant.blockPosition())
                            + " | polled " + helper.getTick() + " probe ticks, mob tickCount "
                            + giant.tickCount);

            final var settled = giant.getDimensions(giant.getPose());
            base[2] = settled.width();
            base[3] = settled.height();
            helper.assertTrue(Math.abs(base[2] - 0.6F * (1.2F + base[0])) < 1.0E-3F
                            && Math.abs(base[3] - 1.8F * (1.2F + base[0])) < 1.0E-3F,
                    "the loaded fury did not resize its box to (1.2 + anger) on its first update "
                            + "(:24): " + base[2] + " x " + base[3] + " at anger " + base[0]);
            // :23 is a PER-MOB-TICK rate, so the probe drives the mob's OWN
            // tick() and measures the delta on the mob's counter: probe ticks
            // are not a valid clock on this pad (see spawn()) and 0.002 across
            // a handful of server ticks is under the float noise. 250 driven
            // ticks move 2.0 to ~1.5 — measurable, and clear of the 1.0 floor
            // (:22) at which the decay stops.
            // The pad's terrain is NOT neutral: a mob in the cell a walking mob
            // gets catches the world's fire (measured: DamageSource (onFire),
            // health 60 -> 58), and every hurt raises the anger to the 2.0 cap
            // (:44-46), which poisons any rate read from it. The probe clears the
            // fire so the anger ledger is decay-only, then asserts the step on
            // EVERY driven tick — 250 x 0.002, each one pinned, with the pad's
            // own interference named in the message if it comes back.
            final int drive = 250;
            for (int i = 0; i < drive; i++) {
                final float before = giant.getAnger();
                giant.clearFire();
                giant.tick();
                final float after = giant.getAnger();
                helper.assertTrue(Math.abs(after - (before - decayPerTick))
                                < 1.0E-4F,
                        "driven mob tick " + i + " did not decay the anger by exactly 0.002 (:22-23): "
                                + before + " -> " + after + " (hp " + giant.getHealth() + " src "
                                + giant.getLastDamageSource() + " tickCount " + giant.tickCount + ")");
            }
            helper.assertTrue(!giant.isRemoved(),
                    "the driven furious zombie left the level mid-probe (health "
                            + giant.getHealth() + ")");
            final float anger = giant.getAnger();
            helper.assertTrue(anger < base[0] && anger > angerFloor,
                    "the anger did not decay from " + base[0] + " over " + drive + " driven mob ticks "
                            + "(:22-23): " + anger + " | server tickCount " + giant.tickCount);
            // The driven count IS the tick count of this leg: the server cannot
            // interleave a mob tick inside a probe leg (single-threaded), and
            // tick() does not touch the mob's tickCount — the server owns it
            // (ServerLevel#tickNonPassenger:772).
            final float want = base[0] - decayPerTick * drive;                       // :23

            helper.assertTrue(Math.abs(anger - want) < 1.0E-3F,
                    "the anger decay is not 0.002 per tick (:23): " + anger + " after " + drive
                            + " driven mob ticks from " + base[0] + " wants " + want);
            final var dims = giant.getDimensions(giant.getPose());
            helper.assertTrue(dims.width() < base[2] && dims.height() < base[3],
                    "the shrinking anger did not shrink the box (:24): " + base[2] + " x " + base[3]
                            + " -> " + dims.width() + " x " + dims.height());
            helper.assertTrue(Math.abs(dims.width() - 0.6F * (1.2F + anger)) < 1.0E-3F
                            && Math.abs(dims.height() - 1.8F * (1.2F + anger)) < 1.0E-3F,
                    "the box is not (1.2 + anger) times the 0.6 x 1.8 base (:24): "
                            + dims.width() + " x " + dims.height() + " at anger " + anger);
            // :27 — the every-tick attack rewrite tracks the SAME decayed value.
            helper.assertTrue(Math.abs(giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                            - (7.0 + (anger - 1.0F) * 5.0)) < 1.0E-6,
                    "the attack base is not 7.0 + (anger - 1) * 5.0 (:27) after " + drive
                            + " driven mob ticks: " + giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                            + " at anger " + anger);
            giant.discard();
        });
    }

    // ------------------------------------------------------------- drops

    /** How long the drop probes wait after the kills before they read the pad.
     *  The scan is the point where the neighbourhood matters (see DeathDrops), so
     *  it runs a few ticks AFTER the kills: by then every probe in the batch has
     *  had its own tick 0 and its foreign stacks are in the box, exactly as they
     *  are in a real run. The verdicts themselves read the owned subset captured
     *  at the kill, so the delay cannot move a count. */
    private static final int SCAN_DELAY_TICKS = 5;

    /** The stacks this probe's own kills left in the pad — the owned subset.
     *
     *  <p>Ownership is captured <b>at the kill</b>. The server thread is
     *  single-threaded and a {@code mob.hurt} call is not re-entered, so the item
     *  entities that enter the pad between the two scans wrapped around one kill
     *  are exactly that death's drops: no neighbouring probe, no block break, no
     *  loot roll can run inside the call.</p>
     *
     *  <p><b>Item identity cannot carry ownership here.</b> The mutation surface of
     *  this probe IS the drop table, so the stack a defect produces — a replaced
     *  {@code dropAllLoot} arm, a flipped 6-flip loop — is the probe's OWN stack,
     *  and a filter keyed on the legal items is blind to precisely that. (The house
     *  pattern in {@code OreDropGameTest.ownedBy} filters by item because there the
     *  contract names one item; here the contract IS the item set.)</p>
     *
     *  <p><b>Position cannot carry it either.</b> The batch packs structures on a
     *  walk ({@code StructureGridSpawner} advances {@code xsize + 5} in x and
     *  {@code zsize + 6} in z per placed structure; {@code TestGenerator} restarts
     *  the walk every 40 tests — measured numbers in
     *  {@link #aForeignStackInAPadIsNamedAndNotCounted}), so a 1×1×1 {@code empty}
     *  neighbour sits 6 away in x <b>and</b> z from this pad while {@link #mobBox}
     *  spans ±12, and a 24³ neighbour's cell edge reaches to 5 blocks from it: a
     *  neighbour's ejected stack lands on this probe's own kill column. Measured on
     *  CI run 42573819914 (attempt 1, sha 1430745):
     *  {@code furiouszombiedeathdropsdoubletherollsandthestack} failed at grid
     *  origin 12969321, -60, -6539840 with {@code a furious zombie dropped
     *  minecraft:stone_button}. No drop body of the pair can produce a stone
     *  button ({@code grep -rn stone_button src/main/java} → 1 hit, in
     *  {@code data/thaumcraft/essentia/tags.json}; no vanilla structure NBT and no
     *  vanilla loot table contains one, so it is not terrain loot — the project's
     *  only {@code stone_button} is the ported essentia entry {@code itemTags[195]},
     *  {@code machina 1}, which a neighbouring machine probe can eject), so the scan
     *  was reading the neighbourhood. {@code GameTestServer#startTests} draws the
     *  grid origin from the level RNG, which is why the arm was green in run
     *  42573817477 and red in 42573819914 at the identical sha.</p>
     *
     *  <p>Foreign stacks therefore stay in the report — the failure message names
     *  them — and are never counted into the verdict. Every kill in the pair puts a
     *  stone button through the shipped block-break route inside the pad on the kill
     *  tick ({@link #breakForeignBlockInPad}), and
     *  {@link #aForeignStackInAPadIsNamedAndNotCounted} keeps that shape as a standing
     *  probe with a self-check, so the coupling is exercised on every run instead of
     *  on the draws that happen to collide.</p> */
    private static final class DeathDrops {

        private final java.util.List<ItemStack> owned = new java.util.ArrayList<>();
        private final java.util.Set<java.util.UUID> ownedIds = new java.util.HashSet<>();

        /** The total items in the owned stacks carrying this item. */
        private int countOf(final Item item) {
            int total = 0;
            for (final ItemStack stack : this.owned) {
                if (stack.is(item)) {
                    total += stack.getCount();
                }
            }
            return total;
        }
    }

    /** Kill {@code kills} fresh mobs and collect what their deaths left, attributed
     *  to this probe by the scans wrapped around each kill — see {@link DeathDrops}.
     *  A mob built from the registry carries no equipment and has no entity loot
     *  table, so the only drops are the ones the original's bodies produce. */
    private static DeathDrops killAndCollectDeathDrops(final GameTestHelper helper,
            final String path, final int kills) {
        final ServerLevel level = helper.getLevel();
        final Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        killer.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        final DeathDrops drops = new DeathDrops();
        for (int i = 0; i < kills; i++) {
            final Mob mob = (Mob) registered(helper, path).create(level);
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(mob);
            final java.util.Set<java.util.UUID> ambient = padDropIds(helper);
            mob.hurt(level.damageSources().playerAttack(killer), 1000.0F);
            helper.assertTrue(mob.isDeadOrDying(), "the probe's kill of thaumcraft:" + path
                    + " (death " + i + ") did not kill the mob, so the drop roll never ran");
            for (final ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
                if (!ambient.contains(drop.getUUID())) {
                    drops.ownedIds.add(drop.getUUID());
                    drops.owned.add(drop.getItem().copy());
                }
            }
            // Foreign stack in the pad, on the kill tick, after the capture: the
            // arm's foreign-stack fixture (see breakForeignBlockInPad).
            breakForeignBlockInPad(helper, i);
        }
        killer.discard();
        return drops;
    }

    /** The identity arm, run on the owned subset. Foreign stacks in the pad are
     *  NAMED in the failure message and never counted — see {@link DeathDrops}. */
    private static void assertOwnedDeathDropsAre(final GameTestHelper helper,
            final DeathDrops drops, final java.util.function.Predicate<ItemStack> legal,
            final String who, final String bodies) {
        final java.util.List<ItemStack> outside = new java.util.ArrayList<>();
        for (final ItemStack stack : drops.owned) {
            if (!legal.test(stack)) {
                outside.add(stack);
            }
        }
        final java.util.List<ItemStack> foreign = new java.util.ArrayList<>();
        for (final ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
            if (!drops.ownedIds.contains(drop.getUUID())) {
                foreign.add(drop.getItem());
            }
        }
        helper.assertTrue(!drops.owned.isEmpty(),
                who + "'s kills left no drop in the pad at all, so the drop contract (" + bodies
                        + ") was never exercised");
        helper.assertTrue(outside.isEmpty(),
                who + " dropped " + describeStacks(outside) + ", outside the pair's drop bodies: "
                        + bodies + " — the pad also held " + describeStacks(foreign)
                        + " from neighbours (the batch walks structures 6 apart in x and z for a"
                        + " 1x1x1 template, 29 for a 24^3 one, and mobBox spans +-12, so their"
                        + " stacks reach this kill column): named, not counted");
    }

    /** "2x minecraft:rotten_flesh, 1x thaumcraft:itemresource" — grouped by
     *  registered id, so a pad full of a neighbour's 240 drops fits one line. */
    private static String describeStacks(final java.util.List<ItemStack> stacks) {
        final java.util.Map<String, int[]> counts = new java.util.LinkedHashMap<>();
        for (final ItemStack stack : stacks) {
            final String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            counts.computeIfAbsent(key, k -> new int[1])[0] += stack.getCount();
        }
        final StringBuilder out = new StringBuilder();
        for (final java.util.Map.Entry<String, int[]> entry : counts.entrySet()) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(entry.getValue()[0]).append("x ").append(entry.getKey());
        }
        return "[" + out + "]";
    }

    /** The item entities standing in the pad right now, by identity. */
    private static java.util.Set<java.util.UUID> padDropIds(final GameTestHelper helper) {
        final java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
        for (final ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
            ids.add(drop.getUUID());
        }
        return ids;
    }

    private static net.minecraft.world.phys.AABB mobBox(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        return new net.minecraft.world.phys.AABB(at).inflate(12, 12, 12);
    }

    /** A stone button placed in the pad and broken through the shipped block-break
     *  route, on the kill tick, AFTER that kill's drops were captured.
     *
     *  <p>It reproduces in the probe's own box the one shape that reddened CI: a
     *  foreign stack produced by the block-break/loot path, standing in the scan box
     *  when the pad is read. It goes in the kill column's own box because that is
     *  where the coupling lives and because the batch order fixes every position:
     *  a probe cannot be packed "next to" the drop probes at a chosen distance, so
     *  the same-tick foreign stack is produced where the verdict is made. The
     *  measured layout is in {@link #aForeignStackInAPadIsNamedAndNotCounted}.</p> */
    private static void breakForeignBlockInPad(final GameTestHelper helper, final int index) {
        final BlockPos rel = new BlockPos(3 + index, 6 + index, 4);
        helper.setBlock(rel.getX(), rel.getY(), rel.getZ(),
                net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState());
        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(rel), true, null),
                "the fixture's stone button at " + rel.toShortString() + " did not break, so the"
                        + " pad has no foreign stack on the kill tick and the foreign-stack arm"
                        + " is untested");
    }

    /** The standing exercise of the pad contract, in one box: a kill, the shipped
     *  block-break route's foreign stack, and the scan over both.
     *
     *  <p>It is the CI shape (run 42573819914: {@code a furious zombie dropped
     *  minecraft:stone_button}) with the verdict on the owned subset. The self-check
     *  keeps it honest: if the fixture's break stops leaving a stack in the box — the
     *  block-break API, the floor, the scan box — this probe says so by name instead
     *  of rotting into a no-op.</p>
     *
     *  <p>Why the foreign stack is broken in the pad rather than arranged in a
     *  neighbouring cell: the batch order fixes positions, and the measured layout of
     *  this batch (run 09Nov2026 02:32, 283 tests, grid origin 6015265, -59, -1188336;
     *  the same shape measured at 2011583, -60, -4860956) is a walk —
     *  {@code StructureGridSpawner} advances the next slot by {@code xsize + 5} in x
     *  AND {@code zsize + 6} in z per placed structure, and {@code TestGenerator}
     *  restarts the walk at the batch corner every {@code maxPerBatch = 40} tests. The
     *  templates are {@code thaumcraft:empty} 1×1×1, {@code empty_24} 24³ and
     *  {@code gametest_pad_32} 32×1×32, so consecutive probes in the batch order sat
     *  (+29, +68) and (0, +249) from one another — out of reach of a 25³ box, while a
     *  1×1×1 neighbour is 6 away in both axes (inside it) and a 24³ neighbour's cell
     *  edge reaches to 5 blocks from the pad. The reach that reddened CI is the wide
     *  templates'; the arrangement that reproduces it on every draw is the one in the
     *  pad. The layout is not stable across runs, so nothing here keys on a
     *  position.</p> */
    @GameTest(template = "empty")
    public static void aForeignStackInAPadIsNamedAndNotCounted(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final DeathDrops drops = killAndCollectDeathDrops(helper, "giantbrainyzombie", 1);
        helper.runAfterDelay(SCAN_DELAY_TICKS, () -> {
            long buttons = 0;
            for (final ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
                if (drop.getItem().is(Items.STONE_BUTTON)) {
                    buttons++;
                }
            }
            helper.assertTrue(buttons >= 1,
                    "the pad held " + buttons + " stone buttons; the fixture's shipped-route break"
                            + " left no foreign stack in mobBox, so the foreign-stack arm of the"
                            + " drop probes is no longer exercised (check destroyBlock's drop flag"
                            + " and that the pad's floor still catches drops)");
            // The verdict the drop probes make, made here over the same box: the owned
            // subset names the kill's drops, the stone button is not counted into any
            // total, and the count arm reads the kill alone.
            helper.assertTrue(drops.countOf(Items.STONE_BUTTON) == 0,
                    "a foreign stone button was counted into this probe's drop totals — the"
                            + " owned-subset attribution (DeathDrops) leaked it");
            final int flesh = drops.countOf(Items.ROTTEN_FLESH);
            helper.assertTrue(flesh <= 24,
                    "one furious-zombie death left " + flesh + " rotten flesh; the original rolls 12"
                            + " coin flips of 2 each (:58-66), so 24 is the cap of that support — a"
                            + " higher total is a doubled loop or a doubled stack");
            assertOwnedDeathDropsAre(helper, drops,
                    stack -> stack.is(Items.ROTTEN_FLESH)
                            || stack.is(Items.CARROT)
                            || stack.is(Items.POTATO)
                            || ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemresource")
                                    .equals(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                    "this probe",
                    "rotten flesh (:58-66) and the 4-way dropAllLoot replacement "
                            + "thaumium / carrot / potato / amber (:76-88)");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void angryZombieDeathDropsFollowTheOriginalRolls(final GameTestHelper helper) {
        // :44-46 — three rolls, each coin flip drops 1 rotten flesh
        // (Items.field_151078_bh = rotten_flesh, fields CSV), so 10 kills
        // sum 30 flips: mean 15, sd 2.74, and the arm takes 4 sd, [4, 26]. The
        // band is a rate guard (the identity arm is the contract's teeth), so it is
        // set wide enough that no honest draw trips it.
        // The brain branch (:50-51) names thaumcraft:itemzombiebrain, which this
        // port has not registered, so it must never appear (the gate).
        final DeathDrops drops = killAndCollectDeathDrops(helper, "brainyzombie", 10);
        helper.runAfterDelay(SCAN_DELAY_TICKS, () -> {
            final int flesh = drops.countOf(Items.ROTTEN_FLESH);
            helper.assertTrue(flesh >= 4 && flesh <= 26,
                    "10 angry-zombie deaths left " + flesh + " rotten flesh; the original rolls 3 coin "
                            + "flips of 1 each per death (:44-46), so the rate must sit near 15 "
                            + "(4sd of the 30-flip sum is 4-26; doubling the rolls lands on 30)");
            assertOwnedDeathDropsAre(helper, drops,
                    stack -> stack.is(Items.ROTTEN_FLESH),
                    "an angry zombie",
                    "rotten flesh (:44-46) and the (unregistered) zombie brain (:50-51)");
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(
                            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemzombiebrain")),
                    "thaumcraft:itemzombiebrain is registered; the brain drop branch is c-card work, "
                            + "and the ledger row (content-mapping line 260) is still not_started");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void furiousZombieDeathDropsDoubleTheRollsAndTheStack(final GameTestHelper helper) {
        // :58-66 — TWO loops of SIX coin flips, each true drop leaving 2 rotten
        // flesh: 12 flips x 2 = 24 per death, so 10 deaths sum 120 flips of 2:
        // mean 120, sd 10.95, and the arm takes 4 sd, [76, 164]. Halving the loops
        // (1 loop) or the stack (1 per flip) lands on 60, well outside. The brain
        // branch (:70-71) is velocity 2.0 and the same unregistered item, so it is
        // gated out exactly as for the angry zombie.
        final DeathDrops drops = killAndCollectDeathDrops(helper, "giantbrainyzombie", 10);
        helper.runAfterDelay(SCAN_DELAY_TICKS, () -> {
            final int flesh = drops.countOf(Items.ROTTEN_FLESH);
            helper.assertTrue(flesh >= 76 && flesh <= 164,
                    "10 furious-zombie deaths left " + flesh + " rotten flesh; the original rolls 2 loops "
                            + "of 6 coin flips dropping 2 each (:58-66), so the rate must sit near 120 "
                            + "(4sd of the sum is 76-164; dropping one loop, or one per flip, lands on 60)");
            final ResourceLocation itemResource =
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemresource");
            // The furious zombie has TWO drop bodies: func_70628_a (:57-73) and the
            // func_70600_l (:75-89) replacement of vanilla dropAllLoot, which picks
            // one of thaumium / carrot / potato / amber per death on the entity RNG.
            assertOwnedDeathDropsAre(helper, drops,
                    stack -> stack.is(Items.ROTTEN_FLESH)
                            || stack.is(Items.CARROT)
                            || stack.is(Items.POTATO)
                            || itemResource.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                    "a furious zombie",
                    "rotten flesh (:58-66) and the 4-way dropAllLoot replacement "
                            + "thaumium / carrot / potato / amber (:76-88)");
            helper.succeed();
        });
    }

    // ----------------------------------------------------- zombie parent

    @GameTest(template = "empty")
    public static void thePairAreZombiesOfTheVanillaFamily(final GameTestHelper helper) {
        // Both originals extend EntityZombie (EntityBrainyZombie.java:13, and
        // EntityGiantBrainyZombie.java:11 extends EntityBrainyZombie), so the
        // port's pair must answer to the 1.21.1 zombie family: the family gate
        // is what keeps zombie piglin aggro, the Undead symbology and the
        // cure-villager behaviour of the parent.
        final EntityType<?> brainy = registered(helper, "brainyzombie");
        final EntityType<?> giant = registered(helper, "giantbrainyzombie");
        final ServerLevel level = helper.getLevel();
        final Mob a = (Mob) brainy.create(level);
        final Mob b = (Mob) giant.create(level);
        helper.assertTrue(a instanceof Zombie,
                "thaumcraft:brainyzombie is not a 1.21.1 Zombie (EntityBrainyZombie.java:13)");
        helper.assertTrue(b instanceof Zombie,
                "thaumcraft:giantbrainyzombie is not a 1.21.1 Zombie (EntityGiantBrainyZombie.java:11)");
        helper.assertTrue(a.getType() != b.getType(),
                "the pair must be two registered entity types, not one");
        helper.succeed();
    }
}
