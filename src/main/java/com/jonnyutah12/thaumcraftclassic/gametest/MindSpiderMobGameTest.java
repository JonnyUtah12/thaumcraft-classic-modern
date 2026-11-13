package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.MindSpiderAiLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Mind Spider (TC-M6-06-s-c3-b, content-mapping
 * {@code Thaumcraft:MindSpider} — line 288 of docs/content-mapping.csv,
 * mod_index 24). The Pech / PechBlast probes are the sibling card
 * TC-M6-06-s-c3-a's and live in {@code PechMobGameTest}.
 *
 * <p>Every figure reaches the mob through the <b>live registry</b> and the vanilla
 * {@code Entity} / {@code Mob} / {@code LivingEntity} API, so the class goes RED on
 * behaviour rather than on a missing symbol. Each literal is the original's own and
 * quotes its decompiled line in {@code thaumcraft.common.entities.monster.
 * EntityMindSpider} (115 lines, read whole). The same figures live as named
 * constants on the Level-free {@link MindSpiderAiLogic}, pinned world-free by
 * {@code MindSpiderAiLogicTest}.</p>
 *
 * <p>What the probes can and cannot show: the mob's AI is switched off
 * unconditionally ({@code :90-92}), so it has no behaviour of its own to chase —
 * these probes measure the switch, the goal set behind it, the harmless-mode gates,
 * the 1200-tick fuse, the drop shutdown and the one spawn seam the shipped tree
 * owns. The card's "does it spawn / behave?" answer is stated in
 * {@link #thePassageSpawnerSeamResolvesAndNoShippedBiomeSpawnsTheMindSpider} and in
 * {@link #theAiSwitchIsForcedOnSoEveryGoalStaysInert}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MindSpiderMobGameTest {

    private MindSpiderMobGameTest() {
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

    /** The column a placed mob stands in: the structure block's OWN (x, z). The
     *  harness force-loads and vouches only the chunks the template box
     *  intersects, and {@code GameTestInfo#tick} starts a probe once those chunks
     *  are entity-ticking — a cell off the origin can land in a vouch-less
     *  neighbour as the grid origin moves with the batch (the c2 finding,
     *  {@code ZombieMobGameTest.MOB_CELL}). */
    private static final BlockPos MOB_CELL = new BlockPos(0, 3, 0);

    private static EntityMindSpider spider(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityMindSpider> type = (EntityType<EntityMindSpider>) registered(helper, "mindspider");
        final EntityMindSpider spider = type.create(level);
        helper.assertTrue(spider != null, "thaumcraft:mindspider created no entity");
        final BlockPos at = helper.absolutePos(MOB_CELL);
        spider.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        // Harness fixture, not ported behaviour: an all-air pad has no floor, so a
        // walking mob falls out of the vouched column, and the pad sets mobs alight
        // (both measured on this family's pads — a burning 1.0-health spider is
        // dead in ~2 s, far inside the 1200-tick fuse this card measures). Gravity
        // off keeps it in the column and clearFire() keeps it alive.
        spider.setNoGravity(true);
        spider.clearFire();
        level.addFreshEntity(spider);
        return spider;
    }

    private static Spider vanillaSpider(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Spider spider = new Spider(EntityType.SPIDER, level);
        final BlockPos at = helper.absolutePos(MOB_CELL);
        spider.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        spider.setNoGravity(true);
        spider.clearFire();
        level.addFreshEntity(spider);
        return spider;
    }

    private static AABB mobBox(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(MOB_CELL);
        return new AABB(at.getX() - 6, at.getY() - 4, at.getZ() - 6, at.getX() + 7, at.getY() + 5, at.getZ() + 7);
    }

    /** The tick-list precondition, stated where it can be read (the c2 pattern): a
     *  harness/layout change reports the pad here, not as a stale counter. */
    private static void assertMobCellIsTicking(final GameTestHelper helper, final Mob mob, final String who) {
        final ServerLevel level = helper.getLevel();
        helper.assertTrue(level.isPositionEntityTicking(mob.blockPosition()),
                who + " stands at " + mob.blockPosition().toShortString() + ", which the harness is not "
                        + "entity-ticking (structure origin "
                        + helper.absolutePos(BlockPos.ZERO).toShortString() + ") — the mob is outside the "
                        + "vouched footprint, so a level-tick measurement is meaningless");
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

    private static boolean has(final Iterable<WrappedGoal> goals, final String goalClass, final int slot) {
        for (final WrappedGoal wrapped : goals) {
            if (wrapped.getGoal().getClass().getSimpleName().equals(goalClass) && wrapped.getPriority() == slot) {
                return true;
            }
        }
        return false;
    }

    private static String describe(final Iterable<WrappedGoal> goals) {
        final StringBuilder sb = new StringBuilder("goals:");
        for (final WrappedGoal g : goals) {
            sb.append(' ').append(g.getPriority()).append('=').append(g.getGoal().getClass().getSimpleName());
            if (g.isRunning()) {
                sb.append("(running)");
            }
        }
        return sb.length() == 6 ? "goals: <none>" : sb.toString();
    }


    // ------------------------------------------------------- registration

    @GameTest(template = "empty")
    public static void registeredWithTheOriginalBoxAndTrackingTriple(final GameTestHelper helper) {
        // ConfigEntities.java:105-106 — registerModEntity(..., 64, 3, true). The 64
        // is BLOCKS; 1.21.1's builder measures the tracking range in CHUNKS, so the
        // shipped builder has to hand it 4 (64/16), not 64.
        final EntityType<?> type = registered(helper, "mindspider");
        helper.assertTrue(Math.abs(type.getDimensions().width() - 0.3F) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - 0.3F) < 1.0E-4F,
                "the mind spider's hitbox is not the original 0.3 x 0.3 (EntityMindSpider.java:18) but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(type.getCategory() == MobCategory.MONSTER,
                "the mind spider is not registered as a MONSTER-category mob but " + type.getCategory());
        helper.assertTrue(type.clientTrackingRange() == MindSpiderAiLogic.TRACK_RANGE_CHUNKS
                        && type.updateInterval() == MindSpiderAiLogic.UPDATE_INTERVAL,
                "the mind spider's tracking triple is not 64 blocks (4 chunks) / 3 ticks "
                        + "(ConfigEntities.java:105-106) but " + type.clientTrackingRange()
                        + " chunks / " + type.updateInterval() + " ticks");
        helper.assertTrue(type.trackDeltas(),
                "the velocity-updates flag of registerModEntity(..., true) (:106) is not set");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attributesAreBothOneAndTheXpValueIsOne(final GameTestHelper helper) {
        final EntityMindSpider spider = spider(helper);
        // :26-30 — func_110147_ax: super's map, then maxHealth and attackDamage
        // written to 1.0. Movement speed is NOT touched, so it stays the parent's —
        // asserted against the vanilla spider itself, not a literal.
        helper.assertTrue(spider.getAttributeValue(Attributes.MAX_HEALTH) == MindSpiderAiLogic.MAX_HEALTH,
                "the mind spider's health is not the original 1.0 (EntityMindSpider.java:28) but "
                        + spider.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(spider.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == MindSpiderAiLogic.ATTACK_DAMAGE,
                "the mind spider's attack is not the original 1.0 (:29) but "
                        + spider.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(spider.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                        - vanillaSpider(helper).getAttributeBaseValue(Attributes.MOVEMENT_SPEED)) < 1.0E-9,
                "the mind spider must keep the parent spider's move speed (:26-30 writes only health "
                        + "and attack); got " + spider.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        // :19 — field_70728_aV = 1 (death xp); :22-24 passes a non-harmless spider.
        helper.assertTrue(spider.getExperienceReward(helper.getLevel(), null) == MindSpiderAiLogic.XP,
                "a non-harmless mind spider's death xp is not the original 1 (EntityMindSpider.java:19) but "
                        + spider.getExperienceReward(helper.getLevel(), null));
        // :94-96 — func_70041_e_() = false; 1.21.1's seam is isPushable(). The
        // vanilla spider, the same parent, is pushable, so the assert is a delta.
        helper.assertTrue(!spider.isPushable(),
                "the mind spider must not be pushable (:94-96 func_70041_e_() = false)");
        helper.assertTrue(vanillaSpider(helper).isPushable(),
                "PROBE BROKEN: a vanilla spider is not pushable, so the isPushable assert above proves "
                        + "nothing (1.21.1's Entity#isPushable is not the seam the original overrides)");
        spider.discard();
        helper.succeed();
    }

    // ------------------------------------------------------- the goal set

    @GameTest(template = "empty")
    public static void goalSetIsTheSpiderParentsMinusTheArmadilloGoal(final GameTestHelper helper) {
        // The original adds NO goal of its own (zero addTask lines in its 115
        // lines), so the set is the EntitySpider parent's. The 1.7.10 vanilla
        // EntitySpider decompile is not vendored, so the slots are the 1.21.1
        // Spider.java:54-65 transcription minus the one provably post-1.7.10 goal.
        final EntityMindSpider spider = spider(helper);
        final List<WrappedGoal> goals = goals(spider, false);
        helper.assertTrue(has(goals, "FloatGoal", 1),
                "no swim goal at slot 1 (Spider.java:54 EntityAIFloat) — " + describe(goals));
        helper.assertTrue(has(goals, "SpiderAttackGoal", 4),
                "no melee goal at slot 4 (Spider.java:57, the card's \"attack\", whose hit the port "
                        + "gates at EntityMindSpider.java:98-102) — " + describe(goals));
        helper.assertTrue(has(goals, "WaterAvoidingRandomStrollGoal", 5),
                "no wander goal at slot 5 (Spider.java:58 EntityAIWander(0.8)) — " + describe(goals));
        helper.assertTrue(has(goals, "LookAtPlayerGoal", 6) && has(goals, "RandomLookAroundGoal", 6),
                "the two look goals of Spider.java:59-60 are not both installed at slot 6 — " + describe(goals));
        helper.assertTrue(goals.stream().anyMatch(g -> g.getGoal() instanceof LeapAtTargetGoal),
                "the leap goal of Spider.java:56 (the card's \"jump\") is missing — " + describe(goals));
        // Spider.java:55 is the ARMADILLO avoidance goal — a 1.20.11 mob, so the
        // 1.7.10 EntitySpider cannot own it, and the constructor strips it.
        helper.assertTrue(goals.stream().noneMatch(g -> g.getGoal() instanceof AvoidEntityGoal<?>),
                "the 1.21.1 armadillo AvoidEntityGoal (Spider.java:55, post-1.7.10) survived the "
                        + "reconciliation — " + describe(goals));
        // Spider.java:61-63 — hurt 1, player 2, iron golem 3 (1.7.10 ordered the
        // hurt goal last and had no villager target: recorded drift).
        final List<WrappedGoal> targets = goals(spider, true);
        helper.assertTrue(has(targets, "HurtByTargetGoal", 1),
                "the hurt-by-target goal is not at target slot 1 (Spider.java:61) — " + describe(targets));
        helper.assertTrue(has(targets, "SpiderTargetGoal", 2) && has(targets, "SpiderTargetGoal", 3),
                "the player (2) and iron-golem (3) target goals of Spider.java:62-63 are not both "
                        + "installed — " + describe(targets));
        helper.assertTrue(targets.size() == 3,
                "the target selector must be the parent's three goals but carries " + targets.size()
                        + " — " + describe(targets));
        spider.discard();
        helper.succeed();
    }

    /**
     * THE CARD'S "DOES IT BEHAVE?" ANSWER, MEASURED. {@code :90-92} forces the AI
     * switch ON, so nothing the spider inherits ever runs. 1.21.1 gates the whole
     * goal engine the same way: {@code Mob#isEffectiveAi() = super && !isNoAi()}
     * (Mob.java:1421-1422) and {@code LivingEntity#aiStep()} calls
     * {@code serverAiStep()} only inside {@code isEffectiveAi()}
     * (LivingEntity.java:2765-2767, reached from {@code tick()} at :2492-2494).
     *
     * <p>The vanilla {@code Spider} in the same structure is the control, and it
     * has to be <b>level-ticked</b>: {@code GoalSelector#tick()} starts a goal only
     * from {@code serverAiStep}, and {@code MeleeAttackGoal#canUse()} throttles
     * itself on {@code level().getGameTime()} ({@code MeleeAttackGoal.java:canUse}
     * — one check per 20 WORLD ticks), so driving {@code mob.tick()} in a loop
     * inside one probe tick exercises the goal engine once at most. Measured that
     * way the control is inert too, which is why this probe polls the level's own
     * cadence (the c2 {@code ZombieMobGameTest} pattern) and latches the outcome.
     * The goal that can start on an all-air pad is {@code RandomLookAroundGoal}
     * (1.21.1 {@code Spider.java:60}, one roll in fifteen per AI tick, no ground
     * needed); the stroll and melee goals need a path and an air pad has no ground,
     * so the assert is "the control ran SOMETHING", not "it attacked".</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void theAiSwitchIsForcedOnSoEveryGoalStaysInert(final GameTestHelper helper) {
        final EntityMindSpider spider = spider(helper);
        final Spider vanilla = vanillaSpider(helper);

        helper.assertTrue(spider.isNoAi(), "the port must force the AI switch (EntityMindSpider.java:90-92)");
        helper.assertTrue(!spider.isEffectiveAi(),
                "isEffectiveAi() must be false while the AI is switched off (Mob.java:1421-1422)");
        helper.assertTrue(vanilla.isEffectiveAi(),
                "PROBE BROKEN: the vanilla control is not on an effective-AI path, so a goal that never "
                        + "starts there is a harness problem, not evidence");

        // ran[0] = the mind spider ever ran a goal (must stay false)
        // ran[1] = the vanilla control ever ran a goal (the success condition)
        final boolean[] ran = {false, false};
        final var start = spider.position();
        helper.succeedWhen(() -> {
            spider.clearFire();                     // pad burn is not the contract
            vanilla.clearFire();
            assertMobCellIsTicking(helper, spider, "the mind spider");
            assertMobCellIsTicking(helper, vanilla, "the vanilla spider control");
            if (goals(spider, false).stream().anyMatch(WrappedGoal::isRunning)) {
                ran[0] = true;
            }
            if (goals(vanilla, false).stream().anyMatch(WrappedGoal::isRunning)) {
                ran[1] = true;
            }
            helper.assertTrue(!ran[0],
                    "a goal RAN on the mind spider while the AI switch is on (:90-92 forces "
                            + "func_145773_az() = true; Mob.java:1421-1422 + LivingEntity.java:2765-2767 "
                            + "gate the engine on it) — " + describe(goals(spider, false))
                            + ", probeTick=" + helper.getTick());
            helper.assertTrue(spider.position().distanceTo(start) < 1.0E-6,
                    "the mind spider moved " + spider.position().distanceTo(start) + " while its AI is off");
            helper.assertTrue(spider.getNavigation().getPath() == null,
                    "the mind spider's navigation was handed a path while its AI is off");
            helper.assertTrue(ran[1],
                    "the vanilla spider control has run no goal by probe tick " + helper.getTick()
                            + " (it needs the LEVEL's AI cadence — see the javadoc; goals of the "
                            + "control: " + describe(goals(vanilla, false)) + ", tickCount="
                            + vanilla.tickCount + ", mind spider tickCount=" + spider.tickCount + ")");
        });

        spider.discard();
        vanilla.discard();
        helper.succeed();
    }

    // ------------------------------------------------------- harmless mode

    @GameTest(template = "empty")
    public static void harmlessModeArmsTheFuseAndShutsOffPickRadiusAttackAndXp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityMindSpider spider = spider(helper);
        final Player victim = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(MOB_CELL);
        victim.setPos(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);

        // :14 — a normal spider never expires; :79-81 — and is crosshair-picked.
        helper.assertTrue(spider.lifeSpan() == MindSpiderAiLogic.DEFAULT_LIFESPAN,
                "a fresh mind spider's lifespan is not Integer.MAX_VALUE (:14) but " + spider.lifeSpan());
        helper.assertTrue(!spider.isHarmless(), "a fresh mind spider is not harmless (:34 Byte(0))");
        helper.assertTrue(Math.abs(spider.getPickRadius() - MindSpiderAiLogic.PICK_RADIUS_NORMAL) < 1.0E-6,
                "a non-harmless mind spider's crosshair reach is not 0.1F (:79-81) but "
                        + spider.getPickRadius());

        // :50-56 — arming harmless mode writes the flag AND arms the 1200-tick fuse.
        spider.setHarmless(true);
        helper.assertTrue(spider.isHarmless(), "setHarmless(true) did not raise the flag (:55)");
        helper.assertTrue(spider.lifeSpan() == MindSpiderAiLogic.HARMLESS_LIFESPAN,
                "setHarmless(true) did not arm the 1200-tick fuse (:51-53); lifeSpan=" + spider.lifeSpan());
        helper.assertTrue(spider.getPickRadius() == MindSpiderAiLogic.PICK_RADIUS_HARMLESS,
                "a harmless spider must not be crosshair-picked (0.0F, :79-81) but reads "
                        + spider.getPickRadius());
        helper.assertTrue(spider.getExperienceReward(level, null) == 0,
                "a harmless spider grants no experience (:22-24) but rewards "
                        + spider.getExperienceReward(level, null));

        // :98-102 — attackEntityAsMob runs only for a non-harmless spider, and that
        // seam is where the parent's bite (the card's "poison") lives.
        final float healthBefore = victim.getHealth();
        helper.assertTrue(!spider.doHurtTarget(victim),
                "a harmless spider landed a melee hit (:98-102 gates attackEntityAsMob)");
        helper.assertTrue(victim.getHealth() == healthBefore,
                "the harmless gate let damage through: health " + healthBefore + " -> " + victim.getHealth());

        // :50-56 — the original's asymmetry: clearing harmless mode does NOT take
        // the fuse back off. Preserved, and recorded.
        spider.setHarmless(false);
        helper.assertTrue(!spider.isHarmless(), "setHarmless(false) must clear the flag (:55)");
        helper.assertTrue(spider.lifeSpan() == MindSpiderAiLogic.HARMLESS_LIFESPAN,
                "clearing harmless mode must leave the fuse armed (the original's :51-53 asymmetry); "
                        + "lifeSpan=" + spider.lifeSpan());
        helper.assertTrue(Math.abs(spider.getPickRadius() - MindSpiderAiLogic.PICK_RADIUS_NORMAL) < 1.0E-6
                        && spider.getExperienceReward(level, null) == MindSpiderAiLogic.XP,
                "a cleared spider must be pickable (0.1F) and reward xp 1 again (:79-81, :22-24)");
        helper.assertTrue(spider.doHurtTarget(victim),
                "a non-harmless mind spider must land the melee hit (:98-102)");
        helper.assertTrue(victim.getHealth() < healthBefore,
                "the non-harmless hit dealt no damage (:100 super.attackEntityAsMob)");

        victim.discard();
        spider.discard();
        helper.succeed();
    }

    /**
     * THE FUSE, ON REAL TICKS. {@code :72-77} — {@code super.tick()} plus
     * {@code !isRemote && ticksExisted > lifeSpan → func_70106_y()}: a harmless
     * spider is discarded once it passes 1200 ticks (60 s); a normal one carries
     * {@code lifeSpan = Integer.MAX_VALUE} (:14) and never expires.
     *
     * <p>The probe sits above the fuse and self-checks that it crossed it
     * ({@code tickCount > 1200}), so it cannot pass vacuously on a pad that is not
     * ticking. The control spider in the same structure proves the discard is the
     * fuse and not the pad, the burn or a shutdown. Health is asserted at every
     * checkpoint: the mind spider has 1.0 max health, so one burn tick would end
     * this probe for the wrong reason.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 1400)
    public static void theHarmlessFuseDiscardsPast1200TicksAndANormalSpiderNeverExpires(
            final GameTestHelper helper) {
        final EntityMindSpider armed = spider(helper);
        final EntityMindSpider control = spider(helper);
        armed.setHarmless(true);
        helper.assertTrue(armed.lifeSpan() == MindSpiderAiLogic.HARMLESS_LIFESPAN,
                "the probe never armed the fuse (:51-53)");
        helper.assertTrue(control.lifeSpan() == MindSpiderAiLogic.DEFAULT_LIFESPAN,
                "the control spider must carry the unbounded lifespan (:14)");
        assertMobCellIsTicking(helper, armed, "the armed mind spider");

        for (int tick = 20; tick <= 1100; tick += 20) {
            final int checkpoint = tick;              // lambda capture (the loop variable is not final)
            helper.runAtTickTime(helper.getTick() + tick, () -> {
                armed.clearFire();
                control.clearFire();
                helper.assertTrue(armed.getHealth() == MindSpiderAiLogic.MAX_HEALTH,
                        "the armed spider lost health at checkpoint " + checkpoint + " (pad burn/fall) — "
                                + "health " + armed.getHealth() + ", tickCount=" + armed.tickCount);
                helper.assertTrue(!armed.isRemoved(),
                        "the armed spider vanished at checkpoint " + checkpoint + ", long inside its "
                                + "1200-tick fuse (tickCount=" + armed.tickCount + ")");
                helper.assertTrue(!control.isRemoved(),
                        "the control spider vanished at checkpoint " + checkpoint + " — the pad is "
                                + "killing mobs (tickCount=" + control.tickCount + ")");
            });
        }
        helper.runAtTickTime(helper.getTick() + 1150, () -> {
            helper.assertTrue(armed.tickCount < MindSpiderAiLogic.HARMLESS_LIFESPAN,
                    "the pre-fuse checkpoint sat past the fuse: tickCount=" + armed.tickCount);
            helper.assertTrue(!armed.isRemoved(),
                    "the armed spider died before its fuse (tickCount=" + armed.tickCount + ")");
        });
        helper.runAtTickTime(helper.getTick() + 1300, () -> {
            armed.clearFire();
            control.clearFire();
            assertMobCellIsTicking(helper, control, "the control mind spider");
            helper.assertTrue(armed.tickCount > MindSpiderAiLogic.HARMLESS_LIFESPAN,
                    "PROBE BROKEN: the run never passed the 1200-tick fuse, tickCount=" + armed.tickCount);
            helper.assertTrue(armed.isRemoved(),
                    "the armed harmless spider survived past its fuse (:74-76 discards once ticksExisted "
                            + "> lifeSpan); tickCount=" + armed.tickCount + ", lifeSpan=" + armed.lifeSpan());
            helper.assertTrue(!control.isRemoved(),
                    "the control spider (lifeSpan Integer.MAX_VALUE, :14) expired with tickCount="
                            + control.tickCount + " — the shutdown is not the fuse");
            control.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void viewerAndHarmlessRoundTripThroughNbtWithoutReArmingTheFuse(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        final EntityType<EntityMindSpider> type = (EntityType<EntityMindSpider>) registered(helper, "mindspider");
        final EntityMindSpider saved = spider(helper);
        saved.setHarmless(true);
        saved.setViewer("Azanor");                                          // :42-44
        final CompoundTag tag = new CompoundTag();
        saved.addAdditionalSaveData(tag);                                   // :110-114
        helper.assertTrue(tag.getByte(MindSpiderAiLogic.NBT_HARMLESS) == 1,
                "the save writes \"harmless\" as the watcher byte (:112); tag reads "
                        + tag.getByte(MindSpiderAiLogic.NBT_HARMLESS));
        helper.assertTrue("Azanor".equals(tag.getString(MindSpiderAiLogic.NBT_VIEWER)),
                "the save writes \"viewer\" as a string (:113); tag reads "
                        + tag.getString(MindSpiderAiLogic.NBT_VIEWER));

        final EntityMindSpider loaded = type.create(level);
        loaded.readAdditionalSaveData(tag);                                 // :104-108
        helper.assertTrue(loaded.isHarmless(), "the loaded spider lost the harmless flag (:106)");
        helper.assertTrue("Azanor".equals(loaded.getViewer()),
                "the loaded spider lost the viewer name (:107); reads \"" + loaded.getViewer() + "\"");
        // The original writes the WATCHER directly on load, never setHarmless, so a
        // loaded harmless spider keeps lifeSpan = Integer.MAX_VALUE and outlives
        // the 1200-tick fuse that arming would set. Preserved, and recorded.
        helper.assertTrue(loaded.lifeSpan() == MindSpiderAiLogic.DEFAULT_LIFESPAN,
                "the load path must NOT arm the fuse (the original's :106 writes the watcher, not "
                        + "setHarmless); loaded lifeSpan=" + loaded.lifeSpan());
        saved.discard();
        helper.succeed();
    }

    // ------------------------------------------------------- drop shutdown

    /**
     * THE CARD'S "EVERY DROP OFF" CLAIM, MEASURED. {@code :83-88} —
     * {@code getDropItemId() = Item.getItemById(0)} and an empty
     * {@code func_70628_a}: the mind spider ships nothing on death. The port keeps
     * that as an empty {@code dropCustomDeathLoot} on a registry-built type with no
     * entity loot table, so the death adds nothing to the world.
     *
     * <p>The scan's own liveness is the control, and it is deterministic: a
     * vanilla spider's loot is RANDOM (string 0-2, an eye only when a player lands
     * the kill), so asserting "the parent drops string" is a 1-in-3 flaky probe —
     * measured on the first run of this class. The probe therefore spawns a stack
     * through the mob's own drop seam and asserts the box scan sees it, and reports
     * the vanilla spider's drop count as evidence in the message.</p>
     */
    @GameTest(template = "empty")
    public static void killingItDropsNothingWhileTheScanSeesADrop(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(MOB_CELL);
        killer.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        final AABB box = mobBox(helper);
        // Ambient baseline: the gametest grid packs neighbours, so an unrelated
        // slice's ejected stack can land in this pad (the OreDropGameTest
        // contamination class). Everything is a DELTA over this scan, and the
        // foreign stacks are named in the failure.
        final int ambient = level.getEntitiesOfClass(ItemEntity.class, box).size();

        final EntityMindSpider spider = spider(helper);
        spider.hurt(level.damageSources().playerAttack(killer), 100.0F);
        // A dead LIVING entity is not removed on the kill tick: LivingEntity#tick
        // discards it after 20 death ticks, and a NoAi mob's death animation is not
        // driven here. isDeadOrDying() is the kill fact; the loot seam has already
        // run inside die(), which is what this probe reads.
        helper.assertTrue(spider.isDeadOrDying(),
                "100 damage on 1.0 health did not kill the mind spider (health=" + spider.getHealth() + ")");
        final List<ItemEntity> afterSpider = level.getEntitiesOfClass(ItemEntity.class, box);
        helper.assertTrue(afterSpider.size() == ambient,
                "the mind spider's death produced " + (afterSpider.size() - ambient) + " item entities; "
                        + ":83-88 (getDropItemId = Item.getItemById(0), empty dropFewItems — the port's "
                        + "empty dropCustomDeathLoot, and a registry-built type with no entity loot "
                        + "table) ships nothing. In the box: " + describeItems(afterSpider));

        // Scan liveness, deterministic: the mob's own drop seam puts a stack in the
        // world, and the same scan must count it.
        final Spider control = vanillaSpider(helper);
        final ItemEntity dropped = control.spawnAtLocation(new net.minecraft.world.item.ItemStack(Items.STRING),
                0.0F);
        helper.assertTrue(dropped != null,
                "PROBE BROKEN: spawnAtLocation returned no ItemEntity, so the box scan is not the "
                        + "measurement this probe needs");
        final List<ItemEntity> afterDrop = level.getEntitiesOfClass(ItemEntity.class, box);
        helper.assertTrue(afterDrop.size() == ambient + 1
                        && afterDrop.stream().anyMatch(e -> e.getItem().is(Items.STRING)),
                "PROBE BROKEN: a stack spawned through the drop seam is invisible to the box scan "
                        + "(ambient=" + ambient + ", now " + afterDrop.size() + "): "
                        + describeItems(afterDrop));

        // The parent's own death behaviour, reported and not asserted: vanilla's
        // spider loot table is random (string 0-2, an eye only on a player kill), so
        // asserting a count here is a flaky probe. Kill one and name the number.
        final Spider parent = vanillaSpider(helper);
        parent.hurt(level.damageSources().playerAttack(killer), 100.0F);
        final int parentDrops = level.getEntitiesOfClass(ItemEntity.class, box).size() - afterDrop.size();
        ThaumcraftClassic.LOGGER.info("[mindspider] drop shutdown measured: the mind spider's death added "
                + "0 item entities over an ambient baseline of {}; the scan sees a spawned stack "
                + "(+1); the vanilla spider control (isDeadOrDying={}) added {} item entities on its own "
                + "death — vanilla's spider loot is random, so that number is evidence, not an assert",
                ambient, parent.isDeadOrDying(), parentDrops);

        control.discard();
        parent.discard();
        killer.discard();
        helper.succeed();
    }

    private static String describeItems(final List<ItemEntity> items) {
        final StringBuilder sb = new StringBuilder();
        for (final ItemEntity item : items) {
            sb.append(item.getItem().getCount()).append('x')
                    .append(BuiltInRegistries.ITEM.getKey(item.getItem().getItem())).append(' ');
        }
        return sb.length() == 0 ? "<empty>" : sb.toString();
    }

    // ------------------------------------------------------- the seams it feeds

    /**
     * THE SPAWN QUESTION, MEASURED. The mind spider has no spawn placement of its
     * own: the original has no biome spawn-list entry and no
     * {@code func_70601_bi}/{@code checkSpawnRules} override. It appears from the
     * warp event ({@code WarpEvents.java:281-315}, not ported — the port has no
     * warp-events class at all) and from the Outer Lands passage's mob spawner
     * ({@code GenPassage.java:271}: {@code setEntityId("Thaumcraft.MindSpider")}).
     *
     * <p>This probe measures the shipped half of that: the id the port's passage
     * seam resolves through the registry, proved on a real spawner block with the
     * same {@code setEntityId} call, and the count of shipped biome definitions
     * that spawn the mind spider — zero, so in a shipped world today no mind
     * spider appears, and the maze feature that would place the spawner is not
     * routed into {@code MazeHandler} (its own class javadoc records that gap).</p>
     */
    @GameTest(template = "empty")
    public static void thePassageSpawnerSeamResolvesAndNoShippedBiomeSpawnsTheMindSpider(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityType<?> type = registered(helper, "mindspider");

        // The shipped seam (GenPassage feature 14) makes exactly this call. The
        // cell is TEMPLATE-RELATIVE (1,1,1) — the house machine cell: setBlock and
        // getBlockEntity take relative positions and ignore cells outside the
        // structure box, so an absolutePos argument lands the block outside the
        // template (measured — the first two runs of this probe died on "Missing
        // block entity" with the doubled coordinates in the harness message).
        final BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Blocks.SPAWNER);
        final BlockPos worldAt = helper.absolutePos(at);
        final SpawnerBlockEntity spawner = helper.getBlockEntity(at);
        helper.assertTrue(spawner != null, "no mob spawner block entity at the placed cell " + worldAt);
        spawner.setEntityId(type, level.getRandom());
        final CompoundTag tag = spawner.saveWithId(level.registryAccess());
        final String carried = tag.getCompound("SpawnData").getCompound("entity").getString("id");
        helper.assertTrue("thaumcraft:mindspider".equals(carried),
                "the passage spawner seam (GenPassage's setEntityId of the registry-resolved id) left "
                        + "SpawnData id \"" + carried + "\" — the shipped id resolves to nothing");

        // And the honest state: no shipped biome spawns it.
        final Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int entriesNamingIt = 0;
        int shippedBiomes = 0;
        final StringBuilder scanned = new StringBuilder();
        for (final ResourceLocation id : biomes.keySet()) {
            if (!ThaumcraftClassic.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            shippedBiomes++;
            scanned.append(' ').append(id);
            final Biome biome = biomes.get(id);
            for (final MobCategory category : MobCategory.values()) {
                if (category == MobCategory.MISC || category == MobCategory.CREATURE
                        || category == MobCategory.AMBIENT) {
                    continue;
                }
                for (final MobSpawnSettings.SpawnerData entry : biome.getMobSettings().getMobs(category).unwrap()) {
                    if (BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).equals(type)) {
                        entriesNamingIt++;
                    }
                }
            }
        }
        helper.assertTrue(entriesNamingIt == 0,
                "a shipped thaumcraft biome now spawns the mind spider (" + entriesNamingIt
                        + " spawn entries) — the original has no spawn-list entry, so this probe's plain"
                        + " statement (it does not spawn naturally) is stale and must be re-measured");
        ThaumcraftClassic.LOGGER.info("[mindspider] shipped thaumcraft biomes scanned: {} ({}), natural"
                + " spawn entries naming thaumcraft:mindspider: 0; the only shipped spawn seam is the "
                + "Outer Lands passage spawner, whose maze feature is not routed into MazeHandler",
                shippedBiomes, scanned);
        helper.succeed();
    }

    /**
     * The original's essentia identity: {@code ConfigAspects.java:125} registers
     * {@code Thaumcraft.MindSpider} as vitium 2 + ignis 2 (1.7.10 {@code
     * Aspect.TAINT} 2 / {@code Aspect.FIRE} 2), and the port ships that pair in
     * {@code data/thaumcraft/essentia/tags.json}. Proved through the live loader
     * and the live entity, so the kill route reads the mind spider by its
     * REGISTERED id, not by the original's name.
     */
    @GameTest(template = "empty")
    public static void theShippedEssentiaEntryNamesTheMindSpiderByItsRegisteredId(final GameTestHelper helper) {
        final EntityMindSpider spider = spider(helper);
        final AspectList aspects = EssentiaTags.getEntityAspects(spider);
        final Aspect taint = Aspect.getAspect("vitium");
        final Aspect fire = Aspect.getAspect("ignis");
        helper.assertTrue(taint != null && fire != null,
                "the aspect registry must carry vitium and ignis for this probe to read");
        helper.assertTrue(aspects.getAmount(taint) == 2 && aspects.getAmount(fire) == 2,
                "the mind spider's essentia is not the original's vitium 2 + ignis 2 "
                        + "(ConfigAspects.java:125) but " + describeAspects(aspects));
        spider.discard();
        helper.succeed();
    }

    private static String describeAspects(final AspectList aspects) {
        final StringBuilder sb = new StringBuilder();
        for (final Aspect aspect : aspects.getAspects()) {
            sb.append(aspect.getName()).append('=').append(aspects.getAmount(aspect)).append(' ');
        }
        return sb.length() == 0 ? "<empty>" : sb.toString();
    }
}
