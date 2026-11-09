package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechItemEntityGoto;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechTradePlayer;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.MindSpiderAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityPechBlast;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.PechBlastAiLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Pech, its blast projectile and the Mind Spider
 * (TC-M6-06-s-c3, content-mapping {@code Thaumcraft:Pech} mod_index 12,
 * {@code Thaumcraft:PechBlast} mod_index 10 and {@code Thaumcraft:MindSpider}
 * mod_index 24).
 *
 * <p>Every figure reaches the mob through the <b>live registry</b> and the
 * vanilla {@code Mob} / {@code LivingEntity} / {@code Projectile} API, so the
 * class goes RED on behaviour rather than on a missing symbol. Each literal is
 * the original's own and quotes its decompiled line in
 * {@code thaumcraft.common.entities.monster.EntityPech} (830 lines),
 * {@code thaumcraft.common.entities.projectile.EntityPechBlast} (182 lines),
 * {@code thaumcraft.common.entities.monster.EntityMindSpider} (115 lines) and
 * the two {@code entities/ai/pech/} goals. The same figures live as named
 * constants on the port's Level-free rule classes, pinned world-free by
 * {@code PechAiLogicTest}, {@code PechBlastAiLogicTest} and
 * {@code MindSpiderAiLogicTest}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PechMobGameTest {

    private PechMobGameTest() {
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

    private static Mob spawn(final GameTestHelper helper, final String path) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, path).create(level);
        helper.assertTrue(spawned instanceof Mob,
                "thaumcraft:" + path + " did not create a Mob instance");
        final Mob mob = (Mob) spawned;
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        // Harness fixture, not ported behaviour: an "empty" pad has no floor, so
        // a WALKING mob falls out of the structure's ticking area (measured on
        // this pad by the c2 card: 12 mob ticks across 65 probe ticks) and the
        // pad sets it alight. Gravity off plus clearFire() keeps the mob ticking
        // inside the box.
        mob.setNoGravity(true);
        level.addFreshEntity(mob);
        return mob;
    }

    private static EntityPech pech(final GameTestHelper helper) {
        final Mob mob = spawn(helper, "pech");
        helper.assertTrue(mob instanceof EntityPech,
                "thaumcraft:pech did not create an EntityPech but " + mob.getClass().getName());
        return (EntityPech) mob;
    }

    private static BlockPos pad(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    // ------------------------------------------------------------ the pech

    @GameTest(template = "empty")
    public static void pechIsRegisteredWithTheOriginalBoxAndTracking(final GameTestHelper helper) {
        // ConfigEntities.java:103 — 64 / 3 / true; :86 func_70105_a(0.6F, 1.8F);
        // a MONSTER-category mob (the 1.7.10 registerModEntity has no category:
        // the family's rule is MobCategory.MONSTER for a hostile mob).
        final EntityType<?> type = registered(helper, "pech");
        helper.assertTrue(Math.abs(type.getDimensions().width() - 0.6F) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - 1.8F) < 1.0E-4F,
                "the pech's hitbox is not the original 0.6 x 1.8 (EntityPech.java:86) but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(type.getCategory() == MobCategory.MONSTER,
                "the pech is not registered as a MONSTER-category mob but " + type.getCategory());
        helper.assertTrue(type.clientTrackingRange() == 4 && type.updateInterval() == 3,
                "the pech's tracking triple is not 64 blocks / 3 ticks (ConfigEntities.java:103) but "
                        + type.clientTrackingRange() + " chunks / " + type.updateInterval() + " ticks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pechCarriesTheConstructorAttributesAndXp(final GameTestHelper helper) {
        final EntityPech pech = pech(helper);
        // :92-94 — maxHealth 30, attackDamage 6, moveSpeed 0.5
        helper.assertTrue(pech.getAttributeValue(Attributes.MAX_HEALTH) == 30.0,
                "the pech's health is not the original 30.0 (EntityPech.java:92) but "
                        + pech.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(pech.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 6.0,
                "the pech's attack is not the original 6.0 (:93) but "
                        + pech.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(pech.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - 0.5) < 1.0E-6,
                "the pech's move speed is not the original 0.5 (:94) but "
                        + pech.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        // :507-510 — func_70658_aO: super armour + 2, capped at 20. An unarmed
        // pech's parent value is 0, so the live value is exactly 2.
        helper.assertTrue(pech.getAttributeValue(Attributes.ARMOR) == 2.0,
                "the pech's armour must be the override's super + 2 (:507-510) but is "
                        + pech.getAttributeValue(Attributes.ARMOR));
        // :87 — experiencePoints = 5
        helper.assertTrue(pech.getExperienceReward(helper.getLevel(), null) == 5,
                "the pech's experience value is not the original 5 (:87) but "
                        + pech.getExperienceReward(helper.getLevel(), null));
        // :86 — the 0.66 eye-height factor on a 1.8 tall box
        helper.assertTrue(Math.abs(pech.getEyeHeight(pech.getPose()) - PechAiLogic.eyeHeight()) < 1.0E-3,
                "the pech's eye height is not height * 0.66 (:91) but "
                        + pech.getEyeHeight(pech.getPose()) + " (want " + PechAiLogic.eyeHeight() + ")");
        helper.succeed();
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

    @GameTest(template = "empty")
    public static void pechGoalSetIsTheOriginalSlots(final GameTestHelper helper) {
        final EntityPech pech = pech(helper);
        // :95-105 — the eleven goals the original registers, at its slots.
        final List<WrappedGoal> goals = goals(pech, false);
        helper.assertTrue(has(goals, FloatGoal.class, 0),
                "no swim goal at slot 0 (EntityPech.java:95 EntityAISwimming) — " + describe(goals));
        helper.assertTrue(has(goals, AIPechTradePlayer.class, 1),
                "no trade goal at slot 1 (:96 AIPechTradePlayer) — " + describe(goals));
        helper.assertTrue(has(goals, AIPechItemEntityGoto.class, 3),
                "no item-pickup goal at slot 3 (:97 AIPechItemEntityGoto) — " + describe(goals));
        helper.assertTrue(has(goals, OpenDoorGoal.class, 5),
                "no door goal at slot 5 (:98 EntityAIOpenDoor(this, true)) — " + describe(goals));
        helper.assertTrue(has(goals, MoveTowardsRestrictionGoal.class, 6),
                "no restriction goal at slot 6 (:99) — " + describe(goals));
        helper.assertTrue(has(goals, MoveThroughVillageGoal.class, 6),
                "no village goal at slot 6 (:100) — " + describe(goals));
        helper.assertTrue(has(goals, RandomStrollGoal.class, 9),
                "no wander goal at slot 9 (:101) — " + describe(goals));
        helper.assertTrue(count(goals, LookAtPlayerGoal.class) == 2,
                "the two watch-closest goals of :102-:103 are not both installed — " + describe(goals));
        helper.assertTrue(has(goals, RandomLookAroundGoal.class, 11),
                "no look-idle goal at slot 11 (:104) — " + describe(goals));
        // :105 — EntityAIHurtByTarget(this, false): 1.21.1's no-alert-others form
        final List<WrappedGoal> targets = goals(pech, true);
        helper.assertTrue(targets.stream().anyMatch(g -> g.getGoal().getClass() == HurtByTargetGoal.class
                        && g.getPriority() == 1),
                "the hurt-by-target goal is not at target slot 1 (:105) — " + describe(targets));
        // The pech registers NO nearest-target goal of its own (:95-105 lists
        // none, and 1.21.1's Monster parent installs nothing), so the target
        // selector is exactly one goal.
        helper.assertTrue(targets.size() == 1,
                "the pech's target selector must be the original's single hurt goal but has "
                        + targets.size() + " goals — " + describe(targets));
        helper.succeed();
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

    @GameTest(template = "empty")
    public static void pechCombatGoalSwapsWithItsType(final GameTestHelper helper) {
        // :444-462 — setCombatTask removes all three attack goals and installs
        // the one the current type wants at slot 2. The two ranged goals are
        // DISTINCT instances (:64 the 50-range arrow goal, :65 the 30-range
        // blast goal), so the probe checks the class at the slot AND that the
        // bow and wand modes do not share one instance.
        final EntityPech pech = pech(helper);
        pech.setPechType(PechAiLogic.TYPE_NORMAL);                                        // :174-182 the melee default
        pech.setCombatTask();
        helper.assertTrue(has(goals(pech, false), MeleeAttackGoal.class, 2),
                "a wild pech with no ranged weapon installs no melee goal at slot 2 (:449) — "
                        + describe(goals(pech, false)));
        helper.assertTrue(has(goals(pech, false), AvoidEntityGoal.class, 4),
                "an untamed pech must run from players at slot 4 (:460 EntityAIAvoidEntity) — "
                        + describe(goals(pech, false)));

        pech.setPechType(PechAiLogic.TYPE_BOW);
        pech.setCombatTask();
        final RangedAttackGoal arrow = rangedAt(pech, 2);
        helper.assertTrue(arrow != null,
                "a bow pech installs no ranged-attack goal at slot 2 (:452) — "
                        + describe(goals(pech, false)));
        helper.assertTrue(count(goals(pech, false), MeleeAttackGoal.class) == 0,
                "setCombatTask left the melee goal installed (:445 removes it) — "
                        + describe(goals(pech, false)));

        pech.setPechType(PechAiLogic.TYPE_WAND);
        pech.setCombatTask();
        final RangedAttackGoal blast = rangedAt(pech, 2);
        helper.assertTrue(blast != null,
                "a wand pech installs no blast goal at slot 2 (:453) — "
                        + describe(goals(pech, false)));
        helper.assertTrue(blast != arrow,
                "the bow (50-range, :64) and blast (30-range, :65) goals are the SAME instance");

        // :458-459 — a tamed pech drops the avoidance goal
        pech.setTamed(true);
        pech.setCombatTask();
        helper.assertTrue(count(goals(pech, false), AvoidEntityGoal.class) == 0,
                "a tamed pech must not flee players (:458-459) — "
                        + describe(goals(pech, false)));
        helper.succeed();
    }

    private static RangedAttackGoal rangedAt(final EntityPech pech, final int slot) {
        for (final WrappedGoal g : goals(pech, false)) {
            if (g.getGoal() instanceof RangedAttackGoal r && g.getPriority() == slot) {
                return r;
            }
        }
        return null;
    }

    @GameTest(template = "empty")
    public static void pechAngerIsTheFuseRollAndGatesItsTarget(final GameTestHelper helper) {
        // :227-235 — getTarget() reads null while the fuse is at 0
        final EntityPech pech = pech(helper);
        pech.setAnger(0);
        helper.assertTrue(pech.getTarget() == null,
                "a pech with anger 0 must report no target (:227-235)");
        // :494-505 — becomeAngryAt assigns the 400..799 roll (it does NOT add)
        // and untames the pech.
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        pech.setTamed(true);
        pech.becomeAngryAt(player);
        final int anger = pech.getAnger();
        helper.assertTrue(anger >= 400 && anger <= 799,
                "becomeAngryAt did not assign the original's 400 + nextInt(400) roll (:503); got " + anger);
        helper.assertTrue(!pech.isTamed(),
                "becomeAngryAt must clear tamed (:504)");
        // the fuse decays one per tick in the tick body (:546-549)
        pech.setAnger(500);
        pech.clearFire();
        pech.aiStep();
        helper.assertTrue(pech.getAnger() == 499,
                "the per-tick anger decay (:546-549) did not run: anger is " + pech.getAnger());
        pech.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pechHurtByAPlayerEnragesTheGangInTheBox(final GameTestHelper helper) {
        // :516-537 — a player attacker enrages every pech in the
        // inflate(32, 16, 32) box and this pech, and the AI-off gate runs first.
        final EntityPech a = pech(helper);
        final EntityPech b = pech(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        a.setAnger(0);
        b.setAnger(0);
        a.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(a.getAnger() >= 400, "the hurt pech did not get its anger fuse (:531)");
        helper.assertTrue(b.getAnger() >= 400,
                "the pech 0 blocks away was not enraged by the player hit — the 32x16x32 scan "
                        + "of :523-528 is not live (its anger is " + b.getAnger() + ")");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pechLootArrayAbsorbsItemsLikeTheOriginal(final GameTestHelper helper) {
        // :60 — the 9-slot array; :656-668 the empty-slot copy pass takes the
        // WHOLE stack; :612-630 canPickup needs a free slot for a non-valued item.
        final EntityPech pech = pech(helper);
        helper.assertTrue(pech.lootSlots().length == 9,
                "the pech's loot array is not the original's 9 slots (EntityPech.java:60)");
        // A DIRT stack is NOT in the valued table, so the pickup pass takes the
        // WHOLE stack. An EMERALD stack would take the valued/tame branch
        // (:612-619, one item per call plus the tame roll) — that leg is pinned
        // by pechTamesOnTheValueRollAgainstTen.
        final ItemStack dirt = new ItemStack(Items.DIRT, 3);
        final ItemStack left = pech.pickupItem(dirt);
        helper.assertTrue(left == null,
                "the pickup pass must return null when the whole stack is absorbed (:664); got " + left);
        int slots = 0;
        for (final ItemStack stack : pech.lootSlots()) {
            if (!stack.isEmpty()) {
                slots++;
                helper.assertTrue(stack.is(Items.DIRT) && stack.getCount() == 3,
                        "the loot slot holds " + stack + ", not the 3 dirt of :662-664");
            }
        }
        helper.assertTrue(slots == 1, "the absorb pass filled " + slots + " slots, not 1");
        helper.assertTrue(pech.canPickup(new ItemStack(Items.STONE)),
                "a pech with 8 free slots must accept a non-valued item (:620-622)");
        // :660-666 — a matching slot with room takes the WHOLE incoming stack
        // (mergeTransfer(held=3, max=64, incoming=1) = 1), so nothing is left over
        pech.pickupItem(new ItemStack(Items.DIRT, 1));
        helper.assertTrue(pech.lootCount(Items.DIRT) == 4,
                "the second dirt stack must merge into the held slot (:660-666); loot holds "
                        + pech.lootCount(Items.DIRT));
        // :666 — with that slot at max, mergeTransfer(64, 64, 1) = 0, so the merge
        // moves nothing and the incoming stack lands in the next free slot
        pech.setLootSlot(0, new ItemStack(Items.DIRT, 64));
        pech.pickupItem(new ItemStack(Items.DIRT, 1));
        helper.assertTrue(pech.lootCount(Items.DIRT) == 65,
                "a full slot must not merge (sz=0) and the stack must land in a free slot "
                        + "(:666-668); got " + pech.lootCount(Items.DIRT));
        // :707-745 — the valued table and the 32 cap
        helper.assertTrue(pech.getValue(new ItemStack(Items.EMERALD)) == 5,
                ":745 — emerald is the 5-value item, got " + pech.getValue(new ItemStack(Items.EMERALD)));
        helper.assertTrue(pech.getValue(new ItemStack(Items.DIRT)) == 0,
                "dirt is not in the valued table and carries no greed aspect, so getValue must be 0");
        helper.assertTrue(pech.isValued(new ItemStack(Items.GOLD_INGOT)),
                "gold_ingot is in the original's table (:741) — isValued must be true");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pechTamesOnTheValueRollAgainstTen(final GameTestHelper helper) {
        // :622-629 — while wild, a valued stack is absorbed one item per call and
        // the nextInt(10) < getValue roll tames the pech (and sends event 18,
        // :626 — the 5 happy particles). Emerald's value is 5, so each call is a
        // 50% draw: 20 calls fail to tame with probability 1e-6.
        final EntityPech pech = pech(helper);
        helper.assertTrue(!pech.isTamed(), "a fresh pech is wild (:223 DataParameter.Byte(0))");
        for (int attempt = 0; attempt < 20 && !pech.isTamed(); attempt++) {
            pech.pickupItem(new ItemStack(Items.EMERALD));
        }
        helper.assertTrue(pech.isTamed(),
                "20 emerald pickups never tamed the pech — the :623 nextInt(10) < getValue(5) "
                        + "roll is not live");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pechEntityEventsDispatchOnTheServerSide(final GameTestHelper helper) {
        // :352-397 — ids 16/17/18/19 are the client particle/sound handlers. The
        // world-side contract is that the receiver EXISTS and is inert on a
        // server: a gametest server is never field_72995_K, so each id must
        // dispatch without touching a client-only API. An unknown id goes to the
        // parent (:394-396).
        final EntityPech pech = pech(helper);
        pech.handleEntityEvent(PechAiLogic.EVENT_MUMBLE);
        pech.handleEntityEvent(PechAiLogic.EVENT_MUMBLE_TRADE);
        // The dispatch must not change the mob's own state. A mob spawned on an
        // `empty` pad can already be outside the structure's ticking area by
        // this tick (the c2 finding: 12 mob ticks across 65 probe ticks), so the
        // probe compares the state ACROSS the dispatches instead of claiming
        // fresh liveness.
        final boolean removedBefore = pech.isRemoved();
        pech.handleEntityEvent(PechAiLogic.EVENT_TAMED);
        pech.handleEntityEvent(PechAiLogic.EVENT_ANGRY);
        pech.handleEntityEvent((byte) 3);
        helper.assertTrue(pech.isRemoved() == removedBefore && pech.getHealth() > 0.0F,
                "the four event ids of :352-397 must not kill or remove the mob; removed "
                        + removedBefore + " -> " + pech.isRemoved() + ", health " + pech.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pechSpawnGateRejectsThePadBiome(final GameTestHelper helper) {
        // :776-806 — the pech spawns only in the overworld, only in a biome that
        // carries the isMagical flag, and only while 4 pechs are not already in
        // the 16-inflated box. The gametest pad sits in PLAINS (no isMagical
        // flag), so a natural spawn attempt must be REJECTED — the probe proves
        // the gate is live rather than vacuously true.
        final EntityPech pech = pech(helper);
        final int pechs = helper.getLevel().getEntitiesOfClass(EntityPech.class,
                pech.getBoundingBox().inflate(PechAiLogic.SPAWN_BOX_INFLATE)).size();
        helper.assertTrue(pechs == 1,
                "the probe expects exactly its own pech in the box, found " + pechs);
        helper.assertTrue(!pech.checkSpawnRules(helper.getLevel(), MobSpawnType.NATURAL),
                "a pech passed the natural spawn gate in the gametest pad biome — the "
                        + "isMagical tag gate (:804-806) is not live");
        helper.succeed();
    }

    // -------------------------------------------------------- the blast

    @GameTest(template = "empty")
    public static void pechBlastIsRegisteredWithTheThrowableBoxAndTwentyTicks(final GameTestHelper helper) {
        // ConfigEntities.java:86 — 64 / 20 / true (TEN times the mob's 3), and
        // EntityThrowable's own 0.5 x 0.5 box.
        final EntityType<?> type = registered(helper, "pechblast");
        helper.assertTrue(Math.abs(type.getDimensions().width() - 0.5F) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - 0.5F) < 1.0E-4F,
                "the blast's hitbox is not EntityThrowable's 0.5 x 0.5 but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(type.updateInterval() == 20 && type.clientTrackingRange() == 4,
                "the blast's tracking triple is not 64 blocks / 20 ticks (ConfigEntities.java:86) but "
                        + type.clientTrackingRange() + " chunks / " + type.updateInterval() + " ticks");
        final EntityPechBlast blast = new EntityPechBlast(
                (EntityType<EntityPechBlast>) type, helper.getLevel());
        helper.assertTrue(Math.abs(blast.getGravity() - PechBlastAiLogic.GRAVITY) < 1.0E-6,
                "the blast's gravity is not the original's 0.025 (EntityPechBlast.java:38-40) but "
                        + blast.getGravity());
        helper.assertTrue(Math.abs(blast.getPickRadius() - 0.1F) < 1.0E-6,
                "the blast's crosshair pick radius is not func_70053_R() = 0.1F (:179-181)");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pechBlastFliesAtItsOwnSpeedAndDropsAtItsOwnGravity(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityPechBlast> type = (EntityType<EntityPechBlast>) registered(helper, "pechblast");
        final EntityPech pech = pech(helper);
        final BlockPos at = pad(helper, 1, 3, 1);
        final EntityPechBlast blast = new EntityPechBlast(type, level, pech, 1, 0, false);
        blast.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        blast.setDeltaMovement(new Vec3(0.5, 0.0, 0.0));
        level.addFreshEntity(blast);
        helper.assertTrue(blast.strength() == 1 && blast.durationArg() == 0,
                "the pech's launch is new EntityPechBlast(w, this, 1, 0, roll) (EntityPech.java:481) "
                        + "— strength/duration read " + blast.strength() + "/" + blast.durationArg());
        helper.runAtTickTime(helper.getTick() + 6L, () -> {
            // :38-40 — the fall per tick is 0.025, HALF the vanilla arrow's.
            // ThrowableProjectile multiplies the motion by 0.99 every tick
            // (ThrowableProjectile.java:67), so after 6 ticks the y-motion is
            // -0.025 * (1 + 0.99 + ... + 0.99^5), not -6 * 0.025.
            double drag = 0.0;
            double term = 1.0;
            for (int i = 0; i < 6; i++) {
                drag += term;
                term *= 0.99;
            }
            helper.assertTrue(Math.abs(blast.getDeltaMovement().y + PechBlastAiLogic.GRAVITY * drag) < 1.0E-5,
                    "after 6 server ticks the blast's y-motion is not -0.025 * (1 + 0.99 + ... + 0.99^5) "
                            + "(:38-40 with ThrowableProjectile.java:67 drag) — expected "
                            + (-PechBlastAiLogic.GRAVITY * drag) + ", got " + blast.getDeltaMovement().y);
            helper.assertTrue(blast.getDeltaMovement().x > 0.4,
                    "the blast lost its launch motion: x-motion is " + blast.getDeltaMovement().x);
            helper.assertTrue(!blast.isRemoved(),
                    "the blast vanished inside 6 ticks — the 500-tick age-out (:79-81) is not the rule");
            blast.discard();
            pech.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pechBlastHitsLivingEntitiesAndVanishes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityPechBlast> type = (EntityType<EntityPechBlast>) registered(helper, "pechblast");
        final EntityPech pech = pech(helper);
        final BlockPos at = pad(helper, 1, 3, 1);
        // :142-146 — the 2x2x2 scan hits any EntityLivingBase that is not a pech.
        final Cow cow = new Cow(EntityType.COW, level);
        cow.moveTo(at.getX() + 2.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        cow.setNoGravity(true);
        level.addFreshEntity(cow);
        final float before = cow.getHealth();
        final EntityPechBlast blast = new EntityPechBlast(type, level, pech, 1, 0, true);
        blast.moveTo(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 0.0F, 0.0F);
        blast.setDeltaMovement(new Vec3(0.5, 0.0, 0.0));
        level.addFreshEntity(blast);
        helper.runAtTickTime(helper.getTick() + 12L, () -> {
            // :147 — the damage is strength + 2 = 3, and the blast discards itself
            helper.assertTrue(cow.getHealth() < before,
                    "the blast reached the cow and dealt no damage (:147, want strength+2 = 3; "
                            + "health " + before + " -> " + cow.getHealth() + ")");
            helper.assertTrue(before - cow.getHealth() <= 4.0F,
                    "the blast dealt " + (before - cow.getHealth()) + ", not the original's "
                            + PechBlastAiLogic.damageFor(1) + " (:147)");
            final boolean effected = cow.hasEffect(MobEffects.POISON)
                    || cow.hasEffect(MobEffects.WEAKNESS)
                    || cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(effected,
                    "a nightshade blast applied none of the :149-171 effects (poison / slowness / "
                            + "weakness) — effects: " + cow.getActiveEffects());
            helper.assertTrue(blast.isRemoved(),
                    "the blast must discard itself after the impact (:173-175 func_70106_y)");
            cow.discard();
            pech.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void pechBlastAgesOutAtFiveHundredTicks(final GameTestHelper helper) {
        // :79-81 — if (ticksExisted > 500) func_70106_y(). tickCount is
        // server-owned (ServerLevel#tickNonPassenger), so this probe runs on
        // real probe ticks and sits above the age: 520 ticks, timeout 700.
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityPechBlast> type = (EntityType<EntityPechBlast>) registered(helper, "pechblast");
        final EntityPechBlast blast = new EntityPechBlast(type, level);
        final BlockPos at = pad(helper, 1, 3, 1);
        blast.moveTo(at.getX() + 0.5, at.getY() + 6.0, at.getZ() + 0.5, 0.0F, 0.0F);
        blast.setNoGravity(true);                      // keep it in the box: the probe measures the age-out
        level.addFreshEntity(blast);
        helper.runAtTickTime(helper.getTick() + 400L, () -> helper.assertTrue(!blast.isRemoved(),
                "the blast was discarded before its 500-tick fuse (tickCount=" + blast.tickCount + ")"));
        helper.runAtTickTime(helper.getTick() + 520L, () -> {
            helper.assertTrue(blast.tickCount > PechBlastAiLogic.MAX_AGE,
                    "the probe never passed the fuse: tickCount=" + blast.tickCount);
            helper.assertTrue(blast.isRemoved(),
                    "the blast survived past 500 ticks (:79-81) at tickCount=" + blast.tickCount);
            helper.succeed();
        });
    }

    // ------------------------------------------------------ the mind spider

    @GameTest(template = "empty")
    public static void mindSpiderIsRegisteredAtZeroPointThreeAndRunsNoAi(final GameTestHelper helper) {
        final EntityType<?> type = registered(helper, "mindspider");
        // :18 — func_70105_a(0.3F, 0.3F)
        helper.assertTrue(Math.abs(type.getDimensions().width() - 0.3F) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - 0.3F) < 1.0E-4F,
                "the mind spider's hitbox is not the original 0.3 x 0.3 (EntityMindSpider.java:18) but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        // ConfigEntities.java:105-106 — 64 / 3 / true
        helper.assertTrue(type.updateInterval() == 3 && type.clientTrackingRange() == 4,
                "the mind spider's tracking triple is not 64 blocks / 3 ticks");
        final Mob mob = spawn(helper, "mindspider");
        helper.assertTrue(mob instanceof EntityMindSpider,
                "thaumcraft:mindspider did not create an EntityMindSpider");
        final EntityMindSpider spider = (EntityMindSpider) mob;
        // :26-30 — health 1.0, attack 1.0; :19 — xp 1
        helper.assertTrue(spider.getAttributeValue(Attributes.MAX_HEALTH) == 1.0,
                "the mind spider's health is not the original 1.0 (:26) but "
                        + spider.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(spider.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 1.0,
                "the mind spider's attack is not the original 1.0 (:27-30) but "
                        + spider.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(spider.getExperienceReward(helper.getLevel(), null) == 1,
                "the mind spider's xp is not the original 1 (:19)");
        // :90-92 — func_145773_az() = true: the whole goal engine is inert
        helper.assertTrue(spider.isNoAi(),
                "the mind spider must run with the AI-off flag (:90-92 func_145773_az)");
        helper.assertTrue(!spider.isEffectiveAi(),
                "isEffectiveAi() must be false for the AI-off mind spider (Mob.java:1421)");
        // :79-81 — the crosshair reach of a real spider is 0.1
        helper.assertTrue(Math.abs(spider.getPickRadius() - MindSpiderAiLogic.PICK_RADIUS_NORMAL) < 1.0E-6,
                "a non-harmless mind spider's pick radius must be 0.1 (:79-81)");
        // :14 — a normal spider never expires
        helper.assertTrue(spider.lifeSpan() == MindSpiderAiLogic.DEFAULT_LIFESPAN,
                "a non-harmless mind spider's lifespan must be Integer.MAX_VALUE (:14)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mindSpiderGoalSetIsTheSpiderParentsMinusTheArmadilloGoal(final GameTestHelper helper) {
        // The original adds NO goal (zero addTask lines), so the set is the
        // 1.7.10 EntitySpider parent's. 1.21.1's Spider.java:56 installs an
        // AvoidEntityGoal against the ARMADILLO (a 1.20.11 mob absent from
        // 1.7.10) — the port must strip it.
        final Mob spider = spawn(helper, "mindspider");
        final List<WrappedGoal> goals = goals(spider, false);
        helper.assertTrue(goals.stream().noneMatch(g -> g.getGoal() instanceof AvoidEntityGoal<?>),
                "the mind spider kept 1.21.1's armadillo AvoidEntityGoal (Spider.java:56) — 1.7.10 "
                        + "had no armadillo — " + describe(goals));
        helper.assertTrue(goals.stream().anyMatch(g -> g.getGoal() instanceof FloatGoal
                        && g.getPriority() == 1),
                "no FloatGoal at slot 1 (the 1.7.10 EntityAIFloat analogue) — " + describe(goals));
        helper.assertTrue(goals.stream().anyMatch(g ->
                        g.getGoal().getClass().getSimpleName().contains("Spider$SpiderAttackGoal")
                                && g.getPriority() == 4),
                "the spider's own melee goal is not at slot 4 — " + describe(goals));
        helper.assertTrue(goals.stream().anyMatch(g -> g.getGoal() instanceof RandomStrollGoal
                        && g.getPriority() == 5),
                "no wander goal at slot 5 (1.7.10 EntityAIWander(0.8)) — " + describe(goals));
        helper.assertTrue(goals.stream().anyMatch(g -> g.getGoal() instanceof LookAtPlayerGoal
                        && g.getPriority() == 6)
                        && goals.stream().anyMatch(g -> g.getGoal() instanceof RandomLookAroundGoal
                                && g.getPriority() == 6),
                "the look-at / look-around pair is not at slot 6 (1.7.10 slots 7/8) — " + describe(goals));
        final List<WrappedGoal> targets = goals(spider, true);
        helper.assertTrue(targets.size() == 3,
                "the target set must be the spider parent's three goals (hurt, player, golem) but has "
                        + targets.size() + " — " + describe(targets));
        helper.assertTrue(targets.stream().anyMatch(g ->
                        g.getGoal() instanceof NearestAttackableTargetGoal<?> && g.getPriority() == 2),
                "the spider's player-target goal is not at target slot 2 — " + describe(targets));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mindSpiderHarmlessModeArmsTheFuseAndCannotBite(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityMindSpider spider = (EntityMindSpider) spawn(helper, "mindspider");
        final Cow cow = new Cow(EntityType.COW, level);
        final BlockPos at = pad(helper, 2, 3, 1);
        cow.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        cow.setNoGravity(true);
        level.addFreshEntity(cow);
        // :52 — harmless mode arms the 1200-tick (60 s) fuse
        spider.setHarmless(true);
        helper.assertTrue(spider.isHarmless(), "setHarmless(true) did not stick");
        helper.assertTrue(spider.lifeSpan() == MindSpiderAiLogic.HARMLESS_LIFESPAN,
                "a harmless spider's fuse must be 1200 ticks (:52) but is " + spider.lifeSpan());
        // :79-81 — a harmless spider cannot be picked
        helper.assertTrue(spider.getPickRadius() == 0.0F,
                "a harmless spider's pick radius must be 0.0 (:79-81)");
        // :98-102 — the bite (the card's "attack/poison") runs only for a real spider
        final float before = cow.getHealth();
        helper.assertTrue(!spider.doHurtTarget(cow),
                "a harmless spider must not land the parent's bite (:98-102)");
        helper.assertTrue(cow.getHealth() == before,
                "the harmless bite damaged the cow: " + before + " -> " + cow.getHealth());
        // :22-24 — a harmless spider grants no xp
        helper.assertTrue(spider.getExperienceReward(helper.getLevel(), null) == 0,
                "a harmless spider must grant 0 xp (:22-24)");
        // a non-harmless one bites
        spider.setHarmless(false);
        helper.assertTrue(spider.doHurtTarget(cow),
                "a non-harmless mind spider must land the parent's bite (:98-102)");
        helper.assertTrue(cow.getHealth() < before,
                "the bite dealt no damage: " + before + " -> " + cow.getHealth());
        cow.discard();
        spider.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mindSpiderCarriesHarmlessAndViewerAcrossANbtRoundTrip(final GameTestHelper helper) {
        // :104-114 — the two keys the original writes: "harmless" and "viewer"
        final EntityMindSpider spider = (EntityMindSpider) spawn(helper, "mindspider");
        spider.setHarmless(true);
        spider.setViewer("Notch");
        final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        spider.addAdditionalSaveData(tag);
        helper.assertTrue(tag.getBoolean(MindSpiderAiLogic.NBT_HARMLESS),
                "addAdditionalSaveData did not write the harmless key (:104-109): " + tag);
        helper.assertTrue("Notch".equals(tag.getString(MindSpiderAiLogic.NBT_VIEWER)),
                "addAdditionalSaveData did not write the viewer key (:110-114): " + tag);
        spider.setHarmless(false);
        spider.setViewer("");
        spider.readAdditionalSaveData(tag);
        helper.assertTrue(spider.isHarmless() && "Notch".equals(spider.getViewer()),
                "the round trip lost the harmless/viewer state: harmless=" + spider.isHarmless()
                        + " viewer=" + spider.getViewer());
        helper.succeed();
    }
}
