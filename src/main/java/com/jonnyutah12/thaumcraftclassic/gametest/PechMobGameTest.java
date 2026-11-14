package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechItemEntityGoto;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechTradePlayer;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Pech and its blast projectile (TC-M6-06-s-c3-a,
 * content-mapping {@code Thaumcraft:Pech} (row 290) mod_index 23 and
 * {@code Thaumcraft:PechBlast} mod_index 10). The Mind Spider's probes are the
 * sibling card TC-M6-06-s-c3-b's and are not in this class.
 *
 * <p>Every figure reaches the mob through the <b>live registry</b> and the
 * vanilla {@code Mob} / {@code LivingEntity} / {@code Projectile} API, so the
 * class goes RED on behaviour rather than on a missing symbol. Each literal is
 * the original's own and quotes its decompiled line in
 * {@code thaumcraft.common.entities.monster.EntityPech} (830 lines),
 * {@code thaumcraft.common.entities.projectile.EntityPechBlast} (182 lines) and
 * the two {@code entities/ai/pech/} goals. The same figures live as named
 * constants on the port's Level-free rule classes, pinned world-free by
 * {@code PechAiLogicTest} and {@code PechBlastAiLogicTest}.</p>
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
                "the pech's hitbox is not the original 0.6 x 1.8 (EntityPech.java:92) but "
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

    /**
     * {@code :73-87} — {@code func_70005_c_} (getCommandSenderName): the pech's
     * name is its TYPE's lang key, not its entity type's. The probe asserts the
     * KEY (a dedicated server resolves no client lang, so {@code getString()}
     * would return the key itself and prove nothing about the mapping), and
     * asserts the original's custom-name branch ({@code :74-75}) wins over the
     * type switch.
     */
    @GameTest(template = "empty")
    public static void pechNamesItselfByItsType(final GameTestHelper helper) {
        final EntityPech pech = pech(helper);
        pech.setPechType(PechAiLogic.TYPE_NORMAL);
        helper.assertTrue("entity.thaumcraft.pech".equals(nameKey(pech)),
                "a type-0 pech must read the registry key (the original's entity.Thaumcraft.Pech.name,"
                        + " :79) but its name key is " + nameKey(pech));
        pech.setPechType(PechAiLogic.TYPE_WAND);
        helper.assertTrue("entity.thaumcraft.pech.1".equals(nameKey(pech)),
                "the wand pech must be the original's Pech Mage (entity.Thaumcraft.Pech.1.name, :81)"
                        + " but its name key is " + nameKey(pech));
        pech.setPechType(PechAiLogic.TYPE_BOW);
        helper.assertTrue("entity.thaumcraft.pech.2".equals(nameKey(pech)),
                "the bow pech must be the original's Pech Stalker (entity.Thaumcraft.Pech.2.name, :83)"
                        + " but its name key is " + nameKey(pech));
        // :74-75 — func_94056_bM() (hasCustomName) returns func_94057_bL() (the
        // custom tag), so the type switch never runs.
        pech.setCustomName(net.minecraft.network.chat.Component.literal("Notch"));
        helper.assertTrue(!(pech.getDisplayName().getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents),
                "a named pech must keep its custom name (:74-75) but reads " + nameKey(pech));
        pech.discard();
        helper.succeed();
    }

    private static String nameKey(final EntityPech pech) {
        return pech.getDisplayName().getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        ? tc.getKey() : "<not translatable>";
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
        // dispatch without touching a client-only API and without touching the
        // mob's own state. An id the pech does not know goes to the parent
        // (:394-396) — id 33 there, which 1.21.1 answers with its
        // "Unidentified entity event" branch, so the delegation runs and nothing
        // else happens.
        //
        // Id 3 is deliberately NOT asserted inert: in 1.7.10 the parent answered
        // it with playDeathSound() (hence the original's inertness), while
        // 1.21.1 defines id 3 as EntityEvent.DEATH and LivingEntity answers it by
        // killing the mob. That is the same notification with a 1.21.1 body, so
        // the probe asserts the delegation, not the 1.7.10 parent's side effect.
        final EntityPech pech = pech(helper);
        // The `empty` pad sets mobs alight (the c2 finding: a DamageSource
        // (onFire), 60 -> 58 hp), which is not this contract, so the probe pins a
        // known state first and asserts the DELTA across the dispatches.
        pech.clearFire();
        pech.setHealth(20.0F);
        pech.handleEntityEvent(PechAiLogic.EVENT_MUMBLE);
        pech.handleEntityEvent(PechAiLogic.EVENT_MUMBLE_TRADE);
        final float before = pech.getHealth();
        final boolean removedBefore = pech.isRemoved();
        final StringBuilder trail = new StringBuilder();
        for (final byte id : new byte[] {PechAiLogic.EVENT_TAMED, PechAiLogic.EVENT_ANGRY,
                PechAiLogic.EVENT_MUMBLE, 33}) {
            pech.handleEntityEvent(id);
            trail.append(" id ").append(id).append("->hp ").append(pech.getHealth());
        }
        helper.assertTrue(pech.isRemoved() == removedBefore && pech.getHealth() == before,
                "the four event ids of :352-397 and the parent's unknown-id branch must not kill,"
                        + " remove or damage the mob; hp " + before + trail + ", removed "
                        + removedBefore + " -> " + pech.isRemoved());
        // The mumble field is the only state the four ids may touch (:355, :358).
        helper.assertTrue(pech.getMumble() > 0.0F,
                "the mumble ids of :355 / :358 must set the field the renderer reads, got "
                        + pech.getMumble());
        pech.discard();
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

    // ------------------------------------------------------------------
    // The blast's flight, stated from the ORIGINAL's literals. The probe below never
    // reads blast.getGravity(): a gravity edit has to redden the probe, not take the
    // probe along with it. 1.7.10 multiplies the motion by 0.99F every tick
    // (Entity.java:333 motionTo) and 1.21.1 multiplies by the same float in
    // ThrowableProjectile.java:67-70; Vec3#scale(double) and applyGravity's
    // add(0.0, -getGravity(), 0.0) widen that float to double, so the recurrence
    // y(n+1) = y(n) * 0.99F − 0.025F evaluated in double arithmetic is BIT-identical
    // to the platform. Measured after 6 ticks: y-motion -0.14629963215951283 and the
    // recurrence -0.14629963215951283; x-motion 0.4707401019085442 and 0.5 * 0.99F^6 =
    // 0.4707401019085442. (What this probe carried before was a double-0.99 geometric
    // sum built from PechBlastAiLogic.GRAVITY — the product's own getter — off by
    // 3.5e-9, which a 1e-5 tolerance hid.)
    private static final float BLAST_GRAVITY_1710 = 0.025F;      // EntityPechBlast.java:38-40
    private static final float BLAST_DRAG_1710 = 0.99F;          // ThrowableProjectile.java:67
    private static final double BLAST_LAUNCH_X_MOTION = 0.5;     // this probe's hand launch
    private static final int BLAST_SAMPLE_TICKS = 6;
    private static final double BLAST_MOTION_TOLERANCE = 1.0E-9;
    // The cells the blast's hit scan can touch while the verdict is still pending, and
    // therefore the cells this probe claims. On each of its ticks the projectile scans
    // from position() to position() + deltaMovement (ThrowableProjectile.java:46-50 →
    // ProjectileUtil.java:24-29, 48-56), so x = 1.5 + 50*(1 − 0.99^n) reaches 12.2 at
    // n = 24 and y = 3.5 − 0.025*n²/2 reaches 1.4 at n = 16, while the verdict needs
    // n ≥ 6. The z motion is 0, so the ray never leaves cell z = 1.
    private static final int BLAST_CORRIDOR_FIRST_X = 1;
    private static final int BLAST_CORRIDOR_LAST_X = 12;
    private static final int BLAST_CORRIDOR_FIRST_Y = 1;
    private static final int BLAST_CORRIDOR_LAST_Y = 4;
    private static final int BLAST_CORRIDOR_Z = 1;
    private static final int PAD_SIDE = 24;                      // "empty_24"
    private static final List<BlockPos> BLAST_CORRIDOR = blastCorridorCells();

    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void pechBlastFliesAtItsOwnSpeedAndDropsAtItsOwnGravity(final GameTestHelper helper) {
        // empty_24, not empty: thaumcraft:empty is a 1x1x1 template (its NBT
        // size list is [1,0,0]), so the cell this probe launches the blast in
        // sits outside the structure's ticking area and the projectile never
        // ticks — the first run of this probe read y-motion 0.0 after 6 server
        // ticks. empty_24 (24x24x24) is this repo's template for probes that
        // measure a real tick cadence.
        //
        // WHY THIS PROBE IS A CLAIM, NOT A SLEEP. CI run 43623936502 (curios arm,
        // tip 3c804129) read y-motion -0.02500000037252903 where the local arm read
        // -0.14629963215951283. That value is the ORIGINAL's gravity applied EXACTLY
        // once, so the blast had been ticked once and then never again: not a sample
        // taken too early — a projectile killed on its first tick. The kill runs at
        // the START of that tick's work: ThrowableProjectile.java:46-50 is
        // super.tick() (tickCount++) and then the hit scan at the CURRENT position
        // with the CURRENT motion, and the entity channel is open for a foreign mob
        // (Projectile.java:257-264 canHitEntity, Entity.java:1663-1665
        // canBeHitByProjectile, LivingEntity.java:3066-3068 isPickable; only the
        // owner pech is filtered, as its own root vehicle — Entity.java:3202-3204).
        // The hit goes to EntityPechBlast#onHit, which scans ±2, damages what is not
        // a pech and discards the blast (EntityPechBlast.java:95-123). The discard
        // does not abort the tick, so that tick's drag and gravity complete: y-motion
        // -0.025F, tickCount 1, isRemoved() true, frozen from then on.
        //
        // Reproduced on the shipped probe by planting ONE Cow centred at relative
        // (2.4, 3, 1.5) — the cell the launch ray (1.5 → 2.0, y 3.5, z 1.5) crosses:
        // red with the CI line byte-for-byte, the sampler showing blastTick=1,
        // removed=true, y=-0.02500000037252903 from probe tick 1. Two measured edges
        // of the same channel, pinned here because they set the claim's shape:
        //   • a mob standing in the launch cell itself is NOT hit — the entity scan
        //     this path uses (ProjectileUtil.java:116-135) inflates the target box by
        //     0.3 and has no start-inside fallback (unlike the shooter variant at
        //     :68-102, which tests aabb.contains(startVec) at :77-82), and AABB#clip
        //     returns nothing for a ray with a zero component inside a box. Planted
        //     at the launch cell, the Cow left the blast flying all 6 ticks.
        //   • a BLOCK in a path cell is the same kill one tick later: a stone in
        //     (2,3,1) reddened the probe with y frozen at -0.04975000097975135 (the
        //     clip is ClipContext.Block.COLLIDER, ProjectileUtil.java:49).
        //
        // The pad area is shared, which is what makes the intruder a batch property
        // and not a product property: the harness reuses one grid area across batches
        // and clearSpaceForStructure clears only BLOCK space in the current
        // structure's box (GameTestStructureInfo.java:269-275; measured in
        // t_b51c488c — a structure block and two mobs from a probe 36 grid cells
        // away, at the same Y, standing inside this pad). A 1x1x1 probe's stride is
        // 6 cells, so sixteen of them pack inside this 24-wide footprint, and their
        // mobs spawn in cells of THIS pad. The curios arm runs 502 probes; that is
        // the arm dependence the card measured.
        //
        // So the probe CLAIMS the cells its flight can touch — swept of foreign
        // entities, cleared of blocks, watched every tick — and reads the blast's OWN
        // tickCount for the physics, so no probe-tick budget is part of the arc. The
        // harness's tick runs after the entity pass (measured: probe tick 1 sees
        // blast tick 1), so the sweep that matters is the one that runs at setup.
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityPechBlast> type = (EntityType<EntityPechBlast>) registered(helper, "pechblast");
        final BlockPos at = pad(helper, BLAST_CORRIDOR_FIRST_X, 3, BLAST_CORRIDOR_Z);
        helper.assertTrue(level.isLoaded(at), "the 24³ pad is not loaded at " + show(at));
        // The layout is claimed, not asserted on: log what the batch handed over, then take
        // the cells. A block in a cell the hit scan touches ends the flight on that tick
        // (ThrowableProjectile.java:47-50 → EntityPechBlast#onHit → discard,
        // EntityPechBlast.java:95-123), and the same is true of a mob (the reproduction
        // above), so the probe cannot afford to sample a pad it did not clear.
        final List<String> handedOver = blastCorridorBlocks(helper);
        if (!handedOver.isEmpty()) {
            ThaumcraftClassic.LOGGER.info("[t_89eee704] the pad was handed over with blocks in the"
                    + " pech-blast flight corridor: {}", handedOver);
        }
        final int sweptAtSetup = sweepBlastCorridor(helper, List.of());
        if (sweptAtSetup > 0) {
            ThaumcraftClassic.LOGGER.info("[t_89eee704] the pech-blast pad was handed over with {}"
                    + " foreign entities in the flight corridor; they are discarded", sweptAtSetup);
        }
        final EntityPech pech = pech(helper);
        final EntityPechBlast blast = new EntityPechBlast(type, level, pech, 1, 0, false);
        blast.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        blast.setDeltaMovement(new Vec3(0.5, 0.0, 0.0));
        level.addFreshEntity(blast);
        helper.assertTrue(blast.strength() == 1 && blast.durationArg() == 0,
                "the pech's launch is new EntityPechBlast(w, this, 1, 0, roll) (EntityPech.java:481) "
                        + "— strength/duration read " + blast.strength() + "/" + blast.durationArg());
        for (final BlockPos cell : BLAST_CORRIDOR) {
            helper.setBlock(cell, Blocks.AIR);
        }
        // Precondition, named before any physics: after the claim the cells have to be air.
        helper.assertTrue(blastCorridorBlocks(helper).isEmpty(),
                "the pech blast's flight corridor is not free air after the claim: "
                        + blastCorridorBlocks(helper));
        // The standing fixture: a foreign mob in the pad, planted after the sweep — so
        // the sweep cannot be what removed it — and 3 blocks off the ray, which the
        // probe measures is outside the scan's reach (see the two edges above). The
        // verdict passes with this mob alive in the pad, and the census assert below
        // fails if it is not really there, so a pad that was never contaminated cannot
        // be what makes this probe green.
        final Cow fixture = new Cow(EntityType.COW, level);
        fixture.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 3.5, 0.0F, 0.0F);
        fixture.setNoGravity(true);
        level.addFreshEntity(fixture);
        helper.onEachTick(() -> {
            // The batch is live in the same area for the whole flight, so an intruder
            // can arrive after the setup sweep. Re-claim and name it at the tick it shows.
            final int swept = sweepBlastCorridor(helper, List.of(pech, blast, fixture));
            if (swept > 0) {
                ThaumcraftClassic.LOGGER.info("[t_89eee704] re-claimed the pech-blast flight corridor:"
                        + " {} foreign entities discarded on tick {}", swept, helper.getTick());
            }
            final List<String> intruders = blastCorridorBlocks(helper);
            helper.assertTrue(intruders.isEmpty(),
                    "a block appeared in the pech blast's claimed flight corridor: " + intruders
                            + " — the hit scan that touches a block ends the flight on that tick"
                            + " (ThrowableProjectile.java:47-50 → EntityPechBlast#onHit → discard,"
                            + " EntityPechBlast.java:95-123)");
            // The sample is read on the blast's OWN tick count, and it is read from a
            // scheduled task: the harness fails the probe on the spot when a task throws
            // (GameTestInfo.java:127-136), so a physics red names the physics. Kept in the
            // success sequence, the same assert is only read at the timeout (
            // :142-149), where a real gravity defect reports the layout arm that happened
            // to be throwing last — measured on the gravity mutation, which reddened with
            // "the blast died on tick 18" instead of the y-motion line.
            if (blast.tickCount >= BLAST_SAMPLE_TICKS) {
                assertFlightOnSampleTick(helper, level, blast, fixture);
            }
        });
        // The gate, not the measurement: the pad is ticking the blast, the pad is the pad
        // this probe planted into, the blast is still flying, and it has ticked the sample
        // count. No probe-tick budget is part of the arc — the blast's own tickCount is.
        helper.succeedWhen(() -> {
            helper.assertTrue(level.isPositionEntityTicking(blast.blockPosition()),
                    "the blast's cell " + show(blast.blockPosition()) + " is not entity-ticking, so no"
                            + " probe-tick budget can buy the flight — the pad is not vouched at "
                            + show(helper.absolutePos(new BlockPos(0, 0, 0))));
            helper.assertTrue(fixture.isAlive() && !fixture.isRemoved(),
                    "the planted fixture mob is gone from " + show(fixture.blockPosition())
                            + " — the pad this probe asserted on is not the pad it planted into; census "
                            + padEntities(helper));
            helper.assertTrue(!blast.isRemoved(),
                    "the blast died on tick " + blast.tickCount + " at " + show(blast.blockPosition())
                            + " — on 1.7.10's table a blast launched into an empty pad hits nothing"
                            + " (EntityPechBlast.java:95-118 skips EntityPech); the claimed corridor holds "
                            + blastCorridorBlocks(helper) + " and the pad census is " + padEntities(helper));
            helper.assertTrue(blast.tickCount >= BLAST_SAMPLE_TICKS,
                    "the blast has ticked " + blast.tickCount + " of the " + BLAST_SAMPLE_TICKS
                            + " ticks this sample needs; it is added on the probe's own tick, so its first"
                            + " tick lands on the next level tick — the pad is not ticking it at "
                            + show(blast.blockPosition()));
        });
    }

    /**
     * The flight's physics, read on the blast's own tick. Preconditions first and each
     * one naming what it measured, so a layout failure reads as a layout failure and a
     * physics failure reads as a physics failure.
     */
    private static void assertFlightOnSampleTick(final GameTestHelper helper, final ServerLevel level,
            final EntityPechBlast blast, final Cow fixture) {
        final int ticks = blast.tickCount;
        helper.assertTrue(level.isPositionEntityTicking(blast.blockPosition()),
                "the blast's cell " + show(blast.blockPosition()) + " is not entity-ticking on its tick "
                        + ticks + " — the pad is not vouched at " + show(helper.absolutePos(new BlockPos(0, 0, 0))));
        helper.assertTrue(fixture.isAlive() && !fixture.isRemoved(),
                "the planted fixture mob is gone from " + show(fixture.blockPosition())
                        + " — the pad this probe asserted on is not the pad it planted into; census "
                        + padEntities(helper));
        helper.assertTrue(!blast.isRemoved(),
                "the blast died on tick " + ticks + " at " + show(blast.blockPosition())
                        + " — on 1.7.10's table a blast launched into an empty pad hits nothing"
                        + " (EntityPechBlast.java:95-118 skips EntityPech); the claimed corridor holds "
                        + blastCorridorBlocks(helper) + " and the pad census is " + padEntities(helper));
        final double expectedY = blastFallMotionAfter(ticks);
        helper.assertTrue(Math.abs(blast.getDeltaMovement().y - expectedY) <= BLAST_MOTION_TOLERANCE,
                "after " + ticks + " of the blast's own ticks its y-motion is not the original's"
                        + " recurrence y = y*0.99F − 0.025F (EntityPechBlast.java:38-40 with"
                        + " ThrowableProjectile.java:67 drag) — expected " + expectedY + ", got "
                        + blast.getDeltaMovement().y);
        final double expectedX = BLAST_LAUNCH_X_MOTION * Math.pow(BLAST_DRAG_1710, ticks);
        helper.assertTrue(Math.abs(blast.getDeltaMovement().x - expectedX) <= BLAST_MOTION_TOLERANCE,
                "after " + ticks + " of the blast's own ticks its x-motion is not 0.5*0.99F^n"
                        + " (ThrowableProjectile.java:67) — expected " + expectedX + ", got "
                        + blast.getDeltaMovement().x);
    }

    /** A position in failure text, absolute, so a red line can be pasted into a world. */
    private static String show(final BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /** The original's fall: y(0) = 0, y(n+1) = y(n) * 0.99F − 0.025F, in double arithmetic. */
    private static double blastFallMotionAfter(final int ticks) {
        double motion = 0.0;
        for (int tick = 0; tick < ticks; tick++) {
            motion = motion * BLAST_DRAG_1710 - BLAST_GRAVITY_1710;
        }
        return motion;
    }

    private static List<BlockPos> blastCorridorCells() {
        final List<BlockPos> cells = new ArrayList<>();
        for (int x = BLAST_CORRIDOR_FIRST_X; x <= BLAST_CORRIDOR_LAST_X; x++) {
            for (int y = BLAST_CORRIDOR_FIRST_Y; y <= BLAST_CORRIDOR_LAST_Y; y++) {
                cells.add(new BlockPos(x, y, BLAST_CORRIDOR_Z));
            }
        }
        return List.copyOf(cells);
    }

    private static AABB blastCorridorBox(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(new BlockPos(0, 0, 0));
        return new AABB(origin.getX() + BLAST_CORRIDOR_FIRST_X, origin.getY() + BLAST_CORRIDOR_FIRST_Y,
                origin.getZ() + BLAST_CORRIDOR_Z,
                origin.getX() + BLAST_CORRIDOR_LAST_X + 1, origin.getY() + BLAST_CORRIDOR_LAST_Y + 1,
                origin.getZ() + BLAST_CORRIDOR_Z + 1);
    }

    /**
     * Discards everything in the claimed corridor that is not one of this probe's own
     * entities — exactly what the harness's own clearSpaceForStructure does to BLOCK
     * space for the current structure's box (GameTestStructureInfo.java:269-275,
     * measured in t_b51c488c). Players stay: a registered player is a shared fixture
     * and removing one floods the network system with remove packets.
     */
    private static int sweepBlastCorridor(final GameTestHelper helper, final List<Entity> keep) {
        final List<Entity> strays = helper.getLevel().getEntities((Entity) null, blastCorridorBox(helper),
                scanned -> !(scanned instanceof Player) && !keep.contains(scanned));
        for (final Entity stray : strays) {
            ThaumcraftClassic.LOGGER.info("[t_89eee704] discarding {} at {} from the pech-blast flight corridor",
                    BuiltInRegistries.ENTITY_TYPE.getKey(stray.getType()).getPath(),
                    show(stray.blockPosition()));
            stray.discard();
        }
        return strays.size();
    }

    private static List<String> blastCorridorBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<String> found = new ArrayList<>();
        for (final BlockPos cell : BLAST_CORRIDOR) {
            final BlockPos absolute = helper.absolutePos(cell);
            final BlockState state = level.getBlockState(absolute);
            if (!state.isAir()) {
                found.add(show(absolute) + "=" + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            }
        }
        return found;
    }

    private static List<String> padEntities(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(new BlockPos(0, 0, 0));
        final List<String> census = new ArrayList<>();
        for (final Entity entity : helper.getLevel().getEntities((Entity) null,
                new AABB(origin.getX(), origin.getY(), origin.getZ(),
                        origin.getX() + PAD_SIDE, origin.getY() + PAD_SIDE, origin.getZ() + PAD_SIDE),
                scanned -> true)) {
            census.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath()
                    + "@" + show(entity.blockPosition()));
        }
        return census;
    }

    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void pechBlastHitsLivingEntitiesAndVanishes(final GameTestHelper helper) {
        // empty_24 for the same reason as pechBlastFliesAtItsOwnSpeed...: the
        // impact needs the blast to fly 2 blocks on real server ticks.
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

    @GameTest(template = "empty_24", timeoutTicks = 700)
    public static void pechBlastAgesOutAtFiveHundredTicks(final GameTestHelper helper) {
        // :79-81 — if (ticksExisted > 500) func_70106_y(). tickCount is
        // server-owned (ServerLevel#tickNonPassenger), so this probe runs on
        // real probe ticks and sits above the age: 520 ticks, timeout 700.
        // The template must be empty_24: on the 1x1x1 `empty` template the
        // blast is outside the ticking area, tickCount stayed 0 and the probe
        // "passed" its own fuse check vacuously.
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

    // ---- TC-M6-06-s-c3-a (t_3cb64300): the card's own probes ----

    /**
     * THE SPAWN QUESTION, MEASURED. :189-209 is the gate: overworld only, the
     * biome must carry NeoForge's is_magical flag (:189-191), the three named
     * exclusions must not match (:188-192 / :201-205), at most MAX_PECHS_IN_BOX
     * pechs in the 16-inflated box (:193-200, :207), and the weight-100 spawn
     * placement roll (:206). The probe measures the parts that are live in this
     * tree AND answers the question the card asks: it counts the shipped biomes
     * that carry the flag. A pech spawns only where a biome is flagged magical —
     * the shipped tree flags NONE (the biome rows 961-963 and the family's shared
     * mob_spawn row 347 are `not_started`, owned by the biome cards and by
     * TC-M6-06-s-c6), so in a shipped world today NO pech spawns, while the gate
     * itself is live code. Both halves are asserted, so the day a biome carries
     * the flag this probe says so.
     */
    @GameTest(template = "empty")
    public static void theNaturalSpawnGateIsLiveAndIsMeasuredAgainstTheShippedBiomes(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityPech pech = pech(helper);
        // :189-191 — the pad biome carries no magical flag, so the gate rejects.
        helper.assertTrue(!pech.checkSpawnRules(level, MobSpawnType.NATURAL),
                "the spawn gate accepted the gametest pad biome, which carries no "
                        + "neoforge:is_magical flag (:189-191) — the gate is not live");
        // :193-200, :207, :202-206 — the rule constants ride the gate.
        helper.assertTrue(PechAiLogic.MAX_PECHS_IN_BOX == 4
                        && Math.abs(PechAiLogic.SPAWN_BOX_INFLATE - 16.0) < 1.0E-9
                        && PechAiLogic.OVERWORLD_ONLY,
                "the gate's constants are not the original's (4 pechs / expand(16.0) / overworld"
                        + " only): " + PechAiLogic.MAX_PECHS_IN_BOX + " / "
                        + PechAiLogic.SPAWN_BOX_INFLATE + " / " + PechAiLogic.OVERWORLD_ONLY);
        // :201-205 — the exclusions.
        helper.assertTrue(PechAiLogic.isExcludedBiome(true, false, false)
                        && PechAiLogic.isExcludedBiome(false, true, false)
                        && PechAiLogic.isExcludedBiome(false, false, true)
                        && !PechAiLogic.isExcludedBiome(false, false, false),
                ":201-205 excludes taint, the magical forest and the eerie wood — the shipped rule"
                        + " is inverted or incomplete");
        // The question: how many shipped biomes carry the flag?
        final net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes =
                level.registryAccess().registryOrThrow(
                        net.minecraft.core.registries.Registries.BIOME);
        int magical = 0;
        final StringBuilder named = new StringBuilder();
        for (final ResourceLocation id : biomes.keySet()) {
            // 21.1.259 Registry has no wrapAsKey: getHolder(ResourceLocation) is
            // the accessor that answers the tag question (Holder#is(TagKey)).
            if (biomes.getHolder(id)
                    .map(h -> h.is(net.neoforged.neoforge.common.Tags.Biomes.IS_MAGICAL))
                    .orElse(false)) {
                magical++;
                named.append(' ').append(id);
            }
        }
        helper.assertTrue(magical == 0,
                "the shipped data now flags " + magical + " biomes magical (" + named
                        + ") — this probe's plain statement (no pech spawns in a shipped world"
                        + " today) is stale and must be re-measured and re-written");
        pech.discard();
        helper.succeed();
    }

    /**
     * Does the blast attack fire? The firing seam is {@code performRangedAttack}
     * (:465-489): type 2 looses an arrow at motion 1.6 (:466-476) and the wand
     * pech fires an {@code EntityPechBlast} with the call-site arguments
     * {@code (1, 0, nextFloat() < 0.1F)} (:481) at motion 1.5 / spread 4.0
     * (:482-485). The probe drives the seam in the world and reads the blast
     * back — its owner and its launch speed — and asserts the branch is
     * exclusive: a bow pech never throws a blast. The blast's 2.0 damage and 0.25
     * knockback of :50-53 are pinned at the rule constants
     * ({@code PechBlastAiLogicTest}) and read by the impact probe; 1.21.1's
     * Projectile exposes no damage/knockback getters.
     */
    @GameTest(template = "empty")
    public static void theRangedAttackSeamLaunchesTheBlastWithTheOriginalConstants(
            final GameTestHelper helper) {
        final EntityPech pech = pech(helper);
        final net.minecraft.world.entity.monster.Zombie victim = helper.spawn(
                net.minecraft.world.entity.EntityType.ZOMBIE, pad(helper, 6, 3, 1));
        pech.setPechType(PechAiLogic.TYPE_WAND);
        pech.performRangedAttack(victim, 1.0F);                                                // :481
        final java.util.List<EntityPechBlast> blasts = helper.getLevel().getEntitiesOfClass(
                EntityPechBlast.class, pech.getBoundingBox().inflate(24.0));
        helper.assertTrue(blasts.size() == 1,
                "the :481 firing path put " + blasts.size() + " blasts in the world, expected 1");
        final EntityPechBlast blast = blasts.get(0);
        helper.assertTrue(blast.getOwner() == pech,
                "the blast's owner is not the firing pech (EntityPechBlast.java:50)");
        final double speed = blast.getDeltaMovement().length();
        helper.assertTrue(Math.abs(speed - PechAiLogic.BLAST_MOTION) < 0.75F,
                "the blast's launch speed is not the :483 motion 1.5 (with the :484-485 arc"
                        + " compensation and the :485 spread of 4.0) but " + speed);
        // :465-466 — the bow leg. 1.21.1's Arrow ctor
        // (level, owner, pickupStack, firedFromWeapon) throws
        // IllegalStateException("Invalid weapon firing an arrow") unless the
        // weapon is a bow/crossbow, and the original's type-2 pech is derived
        // FROM a bow in the mainhand (:172-178), so the probe equips one
        // (finalizeSpawn's roll does the same in a world).
        pech.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        pech.setPechType(PechAiLogic.TYPE_BOW);                                                 // :465
        pech.performRangedAttack(victim, 1.0F);                                                 // :466
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityPechBlast.class,
                        pech.getBoundingBox().inflate(24.0)).size() == 1,
                "a bow pech (type 2) must loose an arrow (:466-476), never a blast");
        final java.util.List<net.minecraft.world.entity.projectile.Arrow> arrows =
                helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,
                        pech.getBoundingBox().inflate(24.0));
        helper.assertTrue(!arrows.isEmpty(), "the :466 arrow is missing - the bow branch never fired");
        // The regression this leg carries. AbstractArrow.addAdditionalSaveData (AbstractArrow.java:544)
        // serializes the pickup stack UNCONDITIONALLY, and ItemStack.save throws
        // IllegalStateException("Cannot encode empty ItemStack") (ItemStack.java:409) on an empty one, so
        // an arrow built with an EMPTY pickup stack crashes the CHUNK SAVE
        // (ServerLevel.save -> EntityStorage.storeEntities -> AbstractArrow.addAdditionalSaveData) -
        // measured in CI on this card's first game leg. Encode the shipped arrow here, then discard it so
        // the probe leaves nothing alive for the shutdown save to serialize.
        for (net.minecraft.world.entity.projectile.Arrow arrow : arrows) {
            final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            try {
                arrow.saveWithoutId(tag);
            } catch (RuntimeException e) {
                helper.fail("the :466 arrow cannot be serialized - the chunk save that reaches it throws "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            helper.assertTrue(tag.contains("item"), "the arrow's NBT carries no `item` pickup entry");
            arrow.discard();
        }
        pech.discard();
        victim.discard();
        helper.succeed();
    }

    /**
     * The blast never fires at nothing: the firing path of :466-496 runs only
     * while a target is in sight, and the goal that drives it is the ranged
     * attack goal of :64 (combat slot 2). The probe asserts the mage pech carries
     * that goal and that it is NOT armed while the pech has no target.
     */
    @GameTest(template = "empty")
    public static void pechblastFiresOnlyOnATargetInSight(final GameTestHelper helper) {
        final EntityPech mage = pech(helper);
        mage.setPechType(PechAiLogic.TYPE_WAND);
        // setPechType only writes the data value (:238-240 — the watcher byte);
        // the original reinstalls the combat goal on the NEXT TICK through
        // updateAINextTick (:600-603), so the probe drives that seam directly.
        mage.setCombatTask();
        mage.setTarget(null);
        final java.util.List<WrappedGoal> list = goals(mage, false);
        final java.util.List<WrappedGoal> ranged = list.stream()
                .filter(g -> g.getGoal() instanceof RangedAttackGoal).toList();
        helper.assertTrue(!ranged.isEmpty(),
                "a mage pech must carry the ranged attack goal of :64 (combat slot 2 at :453);"
                        + " goals: " + list);
        helper.assertTrue(ranged.stream().allMatch(g -> !g.canUse()),
                "the ranged goal is armed with no target — the firing path of :466-496 runs only on"
                        + " a target in sight (:64-65)");
        mage.discard();
        helper.succeed();
    }

    /**
     * :170-178 — the pech's type is read BACK from the mainhand item id. The
     * probe drives {@code finalizeSpawn} (the pass that equips, :168, then reads
     * the mainhand back, :170-178) over draws and asserts the derived type: the
     * bow rolls land type 2, and nothing lands type 1, because the wand branch is
     * gated on the unregistered {@code thaumcraft:wandcasting} (BD-68).
     */
    @GameTest(template = "empty")
    public static void pechTypeTracksTheMainhandItemIdThroughTheSpawnPass(
            final GameTestHelper helper) {
        int bows = 0;
        // The spawn pass arms the mainhand from `random.nextInt(20)` and the bow
        // occupies 5 of the 20 rolls (PechAiLogic.equipmentFor cases 2/4/10/11/13,
        // :150), so one pass equips a bow with p = 0.25. At 12 passes the
        // "at least one bow" gate is a 0.75^12 = 3.2 % cliff on the level RNG, and
        // the RNG draw order moves every time a probe is added to the batch - the
        // 12-pass version went red on the c3-b merge purely because Mind Spider's
        // 11 probes shifted the batch (t_f2184302). 60 passes put the cliff at
        // 0.75^60 = 3e-8 while keeping the gate: if the bow ever leaves the roll
        // table the probe fails loudly instead of measuring nothing.
        for (int i = 0; i < 60; i++) {
            final EntityPech pech = pech(helper);
            final BlockPos at = pad(helper, 1, 3, 1);
            // :167-178 — 21.1.259's DifficultyInstance is (Difficulty, dayTime,
            // specialWorldArg, difficulty); a 0 specialWorldArg keeps
            // getSpecialMultiplier() at 0, so the probe measures the equip/type
            // derivation without the boss-scaling term (BD-69).
            pech.finalizeSpawn(helper.getLevel(), new net.minecraft.world.DifficultyInstance(
                            helper.getLevel().getDifficulty(), helper.getLevel().getDayTime(), 0L, 0.0F),
                    MobSpawnType.NATURAL, null);                                               // :167-178
            final ItemStack mainhand = pech.getItemBySlot(
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            final ResourceLocation id = mainhand.isEmpty() ? null
                    : BuiltInRegistries.ITEM.getKey(mainhand.getItem());
            final int type = pech.getPechType();
            final boolean bow = PechAiLogic.BOW_ITEM.equals(id);
            helper.assertTrue((bow ? type == PechAiLogic.TYPE_BOW : type == PechAiLogic.TYPE_NORMAL)
                            && type != PechAiLogic.TYPE_WAND,
                    "mainhand " + id + " derived pechType " + type + " — :172-178 derives 2 from the"
                            + " bow, 1 only from a REGISTERED thaumcraft:wandcasting (BD-68), 0"
                            + " otherwise");
            if (bow) {
                bows++;
            }
            pech.discard();
        }
        helper.assertTrue(bows >= 1,
                "none of the 60 spawn passes equipped a bow (the bow is 5 of the 20 rolls of "
                        + ":122-146), so the mainhand-to-type link is unmeasured — 0.75^60 "
                        + "= 3e-8, so this is a real roll-table change, not an RNG draw");
        helper.succeed();
    }
}
