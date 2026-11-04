package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    private static Mob spawn(final GameTestHelper helper, final String path) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, path).create(level);
        helper.assertTrue(spawned instanceof Mob,
                "thaumcraft:" + path + " did not create a Mob instance");
        final Mob mob = (Mob) spawned;
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(mob);
        return mob;
    }

    /** An entity built from the registry, loaded from a save compound, and
     *  placed in the level — the path an existing save takes. */
    private static Mob spawnLoaded(final GameTestHelper helper, final String path,
            final CompoundTag saved) {
        final ServerLevel level = helper.getLevel();
        final Mob mob = (Mob) registered(helper, path).create(level);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.readAdditionalSaveData(saved);
        level.addFreshEntity(mob);
        return mob;
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
                        .anyMatch(g -> g.getGoal().getClass() == MeleeAttackGoal.class
                                && g.getPriority() == 4),
                "the angry zombie has no melee-attack goal at goal slot 4 (the 1.7.10 "
                        + "EntityAIAttackOnCollide slot in the zombie parent)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == WaterAvoidingRandomStrollGoal.class
                                && g.getPriority() == 7),
                "the angry zombie has no wander goal at goal slot 7 (the 1.7.10 EntityAIWander slot)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == LookAtPlayerGoal.class
                                && g.getPriority() == 8),
                "the angry zombie has no watch-closest goal at goal slot 8 (1.7.10 EntityAIWatchClosest)");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal().getClass() == RandomLookAroundGoal.class
                                && g.getPriority() == 9),
                "the angry zombie has no look-idle goal at goal slot 9 (1.7.10 EntityAILookIdle)");
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
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        player.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        helper.runAtTickTime(helper.getTick() + 30L, () -> {
            // The target goal of the 1.7.10 zombie parent (EntityAINearestAttackable
            // Target for EntityPlayer) must be live: a zombie that never sees the
            // player is a zombie whose AI was stripped too far.
            helper.assertTrue(zombie.getTarget() == player,
                    "the angry zombie did not acquire the mock player as its target; the "
                            + "parent's nearest-player target goal is not installed (target="
                            + zombie.getTarget() + ")");
            player.discard();
            zombie.discard();
            helper.succeed();
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
        // :44-47 — every attackEntityFrom: anger = min(2.0, anger + 0.1)
        for (int i = 0; i < 20; i++) {
            giant.hurt(level.damageSources().generic(), 0.1F);
            if (giant.isDeadOrDying()) {
                giant.setHealth(60.0F);
            }
        }
        final CompoundTag hit = giant.saveWithoutId(new CompoundTag());
        helper.assertTrue(hit.contains("Anger"),
                "the furious zombie does not persist its anger value (:30-41)");
        final float angerAfter = hit.getFloat("Anger");
        helper.assertTrue(Math.abs(angerAfter - 2.0F) < 1.0E-4F,
                "after 20 hits the furious zombie's anger is not the original cap 2.0 (:45) but "
                        + angerAfter);
        // The scaled box at anger 2.0: 0.6 x 3.2 = 1.92, 1.8 x 3.2 = 5.76 (:24)
        final var dims = giant.getDimensions(giant.getPose());
        helper.assertTrue(Math.abs(dims.width() - 1.92F) < 1.0E-3F
                        && Math.abs(dims.height() - 5.76F) < 1.0E-3F,
                "the angry furious zombie is not at the 2.0-anger box 1.92 x 5.76 (:24) but "
                        + dims.width() + " x " + dims.height());
        // :27 — every living-update tick rewrites attack damage to
        // 7.0 + (anger - 1) * 5 = 12.0 at anger 2.0. The write lands on the
        // mob's next server AI tick, so the deferred block waits for it.
        helper.runAtTickTime(helper.getTick() + 5L, () -> {
            helper.assertTrue(Math.abs(giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) - 12.0) < 1.0E-3,
                    "the furious zombie's attack damage at anger 2.0 is not 7.0 + (2.0 - 1) * 5.0 "
                            + "(:27) but " + giant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
            giant.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void furiousZombieAngerDecaysAndTheBoxShrinksBack(final GameTestHelper helper) {
        // :30-33 — the anger data value (watcher index 20) starts at 1.0; a save
        // that carries a higher value decays at 0.002 per tick while it is
        // above 1.0 (:22-23), resizing the box as it goes (:24).
        final CompoundTag saved = new CompoundTag();
        saved.putFloat("Anger", 2.0F);
        final Mob giant = spawnLoaded(helper, "giantbrainyzombie", saved);
        final var start = giant.getDimensions(giant.getPose());
        helper.runAtTickTime(helper.getTick() + 60L, () -> {
            final CompoundTag now = giant.saveWithoutId(new CompoundTag());
            final float anger = now.getFloat("Anger");
            helper.assertTrue(anger < 2.0F - 0.1F && anger > 1.0F,
                    "after 60 ticks the anger did not decay toward 1.0 (:22-23): " + anger);
            helper.assertTrue(Math.abs(anger - (2.0F - 60 * 0.002F)) < 1.0E-2F,
                    "the anger decay is not 0.002 per tick (:23): " + anger);
            final var dims = giant.getDimensions(giant.getPose());
            helper.assertTrue(dims.width() < start.width() && dims.height() < start.height(),
                    "the shrinking anger did not shrink the box (:24): " + start.width() + " x "
                            + start.height() + " -> " + dims.width() + " x " + dims.height());
            giant.discard();
            helper.succeed();
        });
    }

    // ------------------------------------------------------------- drops

    /** Kill {@code kills} fresh mobs and count the items their deaths leave
     *  inside the pad. A mob built from the registry carries no equipment and
     *  has no entity loot table, so the only drops are the ones the original's
     *  {@code func_70628_a} body produces. */
    private static int countDeathDrops(final GameTestHelper helper, final String path,
            final Item expected, final int kills) {
        final ServerLevel level = helper.getLevel();
        final Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        killer.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        int items = 0;
        for (int i = 0; i < kills; i++) {
            final Mob mob = (Mob) registered(helper, path).create(level);
            mob.moveTo(at.getX() + 0.5, at.getY() + i * 0.0, at.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(mob);
            mob.hurt(level.damageSources().playerAttack(killer), 1000.0F);
        }
        killer.discard();
        for (final ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class,
                mobBox(helper))) {
            if (drop.getItem().is(expected)) {
                items += drop.getItem().getCount();
            }
        }
        return items;
    }

    private static net.minecraft.world.phys.AABB mobBox(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        return new net.minecraft.world.phys.AABB(at).inflate(12, 12, 12);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void angryZombieDeathDropsFollowTheOriginalRolls(final GameTestHelper helper) {
        // :44-46 — three rolls, each coin flip drops 1 rotten flesh
        // (Items.field_151078_bh = rotten_flesh, fields CSV), so 5 kills
        // average 5 * 3 * 0.5 = 7.5 flesh with sd 1.5: 7.5 +- 3 sd is [3, 12].
        // The brain branch (:50-51) names thaumcraft:itemzombiebrain, which this
        // port has not registered, so it must never appear (the gate).
        final int flesh = countDeathDrops(helper, "brainyzombie", Items.ROTTEN_FLESH, 5);
        helper.assertTrue(flesh >= 3 && flesh <= 12,
                "5 angry-zombie deaths left " + flesh + " rotten flesh; the original rolls 3 coin "
                        + "flips of 1 each per death (:44-46), so the rate must sit near 7.5");
        final ServerLevel level = helper.getLevel();
        for (final ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
            final Item item = drop.getItem().getItem();
            helper.assertTrue(item == Items.ROTTEN_FLESH,
                    "an angry zombie dropped " + BuiltInRegistries.ITEM.getKey(item)
                            + "; the original's func_70628_a body drops only rotten flesh and the "
                            + "(unregistered) zombie brain (:43-53)");
        }
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemzombiebrain")),
                "thaumcraft:itemzombiebrain is registered; the brain drop branch is c-card work, "
                        + "and the ledger row (content-mapping line 260) is still not_started");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void furiousZombieDeathDropsDoubleTheRollsAndTheStack(final GameTestHelper helper) {
        // :58-66 — TWO loops of SIX coin flips, each true drop leaving 2 rotten
        // flesh: 12 flips x 2 = 24 per death, so 5 deaths average 60 (sd 7.7):
        // 60 +- 3 sd is [37, 83]. The brain branch (:70-71) is velocity 2.0 and
        // the same unregistered item, so it is gated out exactly as for the
        // angry zombie.
        final int flesh = countDeathDrops(helper, "giantbrainyzombie", Items.ROTTEN_FLESH, 5);
        helper.assertTrue(flesh >= 37 && flesh <= 83,
                "5 furious-zombie deaths left " + flesh + " rotten flesh; the original rolls 2 loops "
                        + "of 6 coin flips dropping 2 each (:58-66), so the rate must sit near 60");
        final ServerLevel level = helper.getLevel();
        for (final ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, mobBox(helper))) {
            helper.assertTrue(drop.getItem().is(Items.ROTTEN_FLESH),
                    "a furious zombie dropped an item outside the original's dropFewItems body "
                            + "(:57-73): " + BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()));
        }
        helper.succeed();
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
