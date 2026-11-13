package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import com.jonnyutah12.thaumcraftclassic.entities.ai.ConvertGrassGoal;
import com.jonnyutah12.thaumcraftclassic.entities.ai.LookAtLivingGoal;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.Kind;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.Target;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The shared base of the six taint animals (TC-M6-06-s-b). The originals
 * all extend 1.7.10 {@code EntityMob} and build goal tables — NOT vanilla
 * animal subclasses — so the port does the same: a {@code Monster} whose
 * goal selectors are populated straight from {@link TaintedAnimalAiLogic}
 * (the tables, and every constant in them, are pinned by
 * {@code TaintedAnimalAiLogicTest}).
 *
 * <p>Deviations recorded with the goal factory below: 1.21.1 removed the
 * village system, so the villager's {@code MoveIndoors} row has no goal to
 * build (skipped at build time, documented) and {@code MoveThroughVillage}
 * maps onto 1.21.1's POI-based goal.</p>
 */
public abstract class EntityTaintedAnimalBase extends Monster implements ITaintedMob {

    protected EntityTaintedAnimalBase(final EntityType<? extends EntityTaintedAnimalBase> type,
            final Level level) {
        super(type, level);
    }

    protected abstract List<GoalSpec> goalTable();

    protected abstract List<GoalSpec> targetTable();

    @Override
    protected void registerGoals() {
        for (final GoalSpec spec : goalTable()) {
            final net.minecraft.world.entity.ai.goal.Goal goal = makeGoal(this, spec);
            if (goal != null) {
                this.goalSelector.addGoal(spec.priority(), goal);
            }
        }
        for (final GoalSpec spec : targetTable()) {
            this.targetSelector.addGoal(spec.priority(), makeTargetGoal(this, spec));
        }
    }

    /**
     * The single goal factory for every taint-animal table row. Each Kind
     * maps 1:1 onto the original's EntityAIBase class; rows with no 1.21.1
     * equivalent return {@code null} and are skipped (documented per case).
     */
    static net.minecraft.world.entity.ai.goal.Goal makeGoal(final PathfinderMob mob, final GoalSpec spec) {
        return switch (spec.kind()) {
            case SWIM_GOAL -> new FloatGoal(mob);
            case ATTACK -> new MeleeAttackGoal(mob, spec.speed(), spec.flag());
            case LEAP -> new LeapAtTargetGoal(mob, (float) spec.speed());
            case WANDER -> new WaterAvoidingRandomStrollGoal(mob, spec.speed());
            // EntityAIWatchClosest(this, class, range) — 1.21.1's
            // LookAtPlayerGoal takes the probability; 0.02 is the original's
            // default, WatchClosest2 rows pass 1.0 (flag true)
            case WATCH -> new LookAtPlayerGoal(mob, targetClass(spec.target()),
                    (float) spec.range(), spec.flag() ? 1.0F : 0.02F);
            case WATCH_LIVING -> new LookAtLivingGoal(mob, spec.range());
            case LOOK_IDLE -> new RandomLookAroundGoal(mob);
            case SWELL -> ((EntityTaintCreeper) mob).new SwellGoal();
            case CONVERT_GRASS -> new ConvertGrassGoal(mob);
            // EntityAIAvoidEntity(this, Ocelot.class, 6.0F, 1.0F, 1.2F)
            // (EntityTaintCreeper line 47)
            case AVOID -> new AvoidEntityGoal<>(mob, Ocelot.class,
                    (float) spec.range(), (float) spec.speed(), 1.2F);
            case OPEN_DOOR -> new OpenDoorGoal(mob, spec.flag());
            // EntityAIMoveIndoors: 1.21.1 removed the goal together with the
            // village system — no equivalent, skipped (documented)
            // EntityAIRestrictOpenDoor: 1.21.1 removed the goal (door
            // behaviour moved into GroundPathNavigation's canOpenDoors /
            // canPassDoors flags, which the villager sets in its ctor) —
            // skipped, documented
            case RESTRICT_OPEN_DOOR -> null;
            case MOVE_INDOORS -> null;
            case RESTRICT -> new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(
                    mob, spec.speed());
            // EntityAIMoveThroughVillage(this, 1.0, false): the boolean is the
            // decompiled onlyAtNight argument (false); the original's
            // door-passing behaviour (shouldPassDoors) is the supplier, and
            // the POI scan distance follows the villager's door net.
            case VILLAGE -> new net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal(
                    mob, spec.speed(), spec.flag(), 64, () -> true);
            case HURT_BY, TARGET -> null;
        };
    }

    static net.minecraft.world.entity.ai.goal.target.TargetGoal makeTargetGoal(
            final PathfinderMob mob, final GoalSpec spec) {
        return switch (spec.kind()) {
            // EntityAIHurtByTarget(this, false)
            case HURT_BY -> new HurtByTargetGoal(mob);
            // EntityAINearestAttackableTarget(this, class, 0, mustSee)
            case TARGET -> new NearestAttackableTargetGoal<>(mob, targetClass(spec.target()), spec.flag());
            default -> throw new IllegalArgumentException("not a target row: " + spec.kind());
        };
    }

    @SuppressWarnings("unchecked")
    static Class<? extends LivingEntity> targetClass(final Target target) {
        return switch (target) {
            case PLAYER -> (Class<? extends LivingEntity>) (Class<?>) Player.class;
            case VILLAGER -> (Class<? extends LivingEntity>) (Class<?>) Villager.class;
            case ANIMAL -> (Class<? extends LivingEntity>) (Class<?>) Animal.class;
            case OCELOT -> (Class<? extends LivingEntity>) (Class<?>) Ocelot.class;
            case LIVING, NONE -> LivingEntity.class;
        };
    }
}
