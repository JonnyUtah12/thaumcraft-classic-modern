package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The golem's "get away from a fuse that is lit" goal — port of
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/combat/AIAvoidCreeperSwell.java},
 * Thaumcraft's creeper-only, swell-only copy of 1.7.10's {@code EntityAIAvoidEntity}.
 *
 * <p>Installed at melee priority {@code 0} for <b>every</b> core above {@code -1}
 * ({@code EntityGolemBase.java:221}: {@code if (this.getCore() > -1)
 * this.tasks.addTask(0, new AIAvoidCreeperSwell(this))}), so it outranks every other
 * goal a core installs. The original's constructor self-declares priority 1
 * ({@code AIAvoidCreeperSwell.java:28}), which the installation index 0 overwrote.</p>
 *
 * <h2>Why this is a {@link GolemGoal} and not an {@code AvoidEntityGoal} subclass</h2>
 * 1.21.1's {@code AvoidEntityGoal} is the same shape, but four behaviours the original
 * pins are not reachable through it: the flee speed pair is captured <em>lazily</em>
 * from the golem's live speed on the first attempt ({@code :32-35}), the goal starts at
 * the <em>near</em> speed instead of the walk speed ({@code :80}), the start applies the
 * original's hop impulse — {@code motionX/motionZ} nudged toward the flee point plus
 * {@code motionY = 0.3} ({@code :73-81}, 1.7.10's own {@code EntityAIAvoidEntity}
 * behaviour) — and the swell-state test is on the <em>first</em> creeper in the box, not
 * the nearest ({@code :40-44}). {@code AvoidEntityGoal}'s fields are private, so the
 * faithful route is a small {@code Goal} over {@code golem.getNavigation()}.</p>
 *
 * <h2>Pinned numbers (original lines)</h2>
 * <ul>
 *   <li>detection radius {@code distanceFromEntity = 5.0F} and the box inflated
 *       {@code (5.0, 3.0, 5.0)} ({@code :26, 37-39});</li>
 *   <li>the swell test: {@code func_70832_p() == 1} — the 1.7.10 creeper's
 *       {@code getCreeperState()} (the sibling {@code AICreeperSwell.java:16, 34} uses
 *       the deobf name for the same accessor) — is 1.21.1's
 *       {@code Creeper.getSwellDir() == 1}, the value 1.21.1's {@code SwellGoal} writes
 *       while swelling ({@code SwellGoal.java:37-42});</li>
 *   <li>the flee target: {@code RandomPositionGenerator.func_75461_b(golem, 16, 7, pos)}
 *       = {@code findPositionAway} ({@code :49-54}) → {@code DefaultRandomPos.getPosAway},
 *       and the "is it actually further from the creeper than I am" test
 *       ({@code :57-59});</li>
 *   <li>the path test: {@code PathEntity.findTargetPos(Vec3)} ({@code :63}) — in 1.7.10
 *       that compares the <b>last path node</b> with the floor of the wanted position,
 *       so {@link #pathReaches(Path, Vec3)} does exactly that (1.21.1's
 *       {@code AvoidEntityGoal} only checks for a non-null path);</li>
 *   <li>the speeds: {@code farSpeed = speed * 1.125F}, {@code nearSpeed = speed * 1.25F}
 *       ({@code :33-34}), switched at a squared distance of {@code 49.0}
 *       ({@code :88-92});</li>
 *   <li>the hop: the impulse toward the flee point scaled by {@code 0.8F} plus
 *       {@code 0.2F} of the current motion, and {@code motionY = 0.3} ({@code :74-79}).
 *       The {@code + 0.5} on the flee point in that arithmetic is 1.7.10 vanilla
 *       {@code EntityAIAvoidEntity}'s own quirk (the generated position already carries
 *       the half-block offset) and is kept, not "fixed".</li>
 * </ul>
 */
public final class AIAvoidCreeperSwell extends GolemGoal {

    /** The original's {@code distanceFromEntity} — the horizontal detection radius ({@code :26}). */
    private static final double DETECTION_RADIUS = 5.0;

    /** The original's vertical box inflation ({@code :39}). */
    private static final double VERTICAL_INFLATION = 3.0;

    /** The original's "near" squared distance for the speed switch ({@code :88}). */
    private static final double NEAR_SQUARED = 49.0;

    /** The original's {@code farSpeed} / {@code nearSpeed} ({@code :16-17}), captured lazily ({@code :32-35}). */
    private float farSpeed;
    private float nearSpeed;

    /** The creeper being fled from — the original's {@code closestLivingEntity} ({@code :18}). */
    private Entity closestLivingEntity;

    /** The original's {@code entityPathEntity} / {@code entityPathNavigate} / {@code targetBlock} ({@code :20-22}). */
    private Path entityPathEntity;
    private final PathNavigation entityPathNavigate;
    private Vec3 targetBlock;

    public AIAvoidCreeperSwell(final EntityGolemBase golem) {
        super(golem);
        this.farSpeed = 0.0F;
        this.nearSpeed = 0.0F;
        this.entityPathNavigate = golem.getNavigation();
        // 1.7.10 set no mutex bits; MOVE is 1.21.1's AvoidEntityGoal flag set
        // (AvoidEntityGoal.java:57), which is what this goal does.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Original {@code func_75250_a()} ({@code :31-67}): capture the speeds once, find the
     * first creeper in the box, require it to be swelling and visible, then require a
     * reachable spot that is further from it than the golem.
     */
    @Override
    public boolean canUse() {
        if (this.farSpeed == 0.0F) {
            this.farSpeed = this.golem.getSpeed() * 1.125F;
            this.nearSpeed = this.golem.getSpeed() * 1.25F;
        }

        final AABB box = this.golem.getBoundingBox().inflate(DETECTION_RADIUS, VERTICAL_INFLATION, DETECTION_RADIUS);
        final List<Creeper> creepers = this.level().getEntitiesOfClass(Creeper.class, box);
        if (creepers.isEmpty()) {
            return false;
        }
        final Creeper creeper = creepers.get(0);
        if (creeper.getSwellDir() != 1) {
            return false;
        }
        this.closestLivingEntity = creeper;
        if (!this.golem.getSensing().hasLineOfSight(creeper)) {
            return false;
        }

        final Vec3 fleePos = DefaultRandomPos.getPosAway(this.golem, 16, 7, creeper.position());
        if (fleePos == null) {
            return false;
        }
        if (creeper.distanceToSqr(fleePos.x, fleePos.y, fleePos.z) < creeper.distanceToSqr(this.golem)) {
            return false;
        }

        this.entityPathEntity = this.entityPathNavigate.createPath(fleePos.x, fleePos.y, fleePos.z, 0);
        this.targetBlock = fleePos;
        return this.entityPathEntity == null ? false : pathReaches(this.entityPathEntity, fleePos);
    }

    /**
     * Original {@code func_75253_b()} ({@code :69-71}): live while the path is not finished.
     *
     * <p>Checked against the tree: 1.7.10's {@code PathNavigate.func_75500_f()} is "the navigate
     * is finished" (no path, path consumed, or empty), and 1.21.1's {@code PathNavigation.isDone()}
     * is {@code path == null || path.isDone()} ({@code PathNavigation.java:320-322}) — the same
     * clauses, so the call is the original's. Consequence of that contract, measured in
     * {@code combat_golemFleesOnlyACreeperWhoseFuseIsLit}: the goal holds its MOVE flag for as
     * long as the navigation has a node left, so if the golem parks one node short of a random
     * flee spot the return-home task stays starved. The probe therefore does not claim the
     * golem walks home afterwards.</p>
     */
    @Override
    public boolean canContinueToUse() {
        return !this.entityPathNavigate.isDone();
    }

    /** Original {@code func_75249_e()} ({@code :73-81}): the hop toward the flee point, then path at the near speed. */
    @Override
    public void start() {
        final double dx = this.targetBlock.x + 0.5 - this.golem.getX();
        final double dz = this.targetBlock.z + 0.5 - this.golem.getZ();
        final float distance = Mth.sqrt((float) (dx * dx + dz * dz));
        final Vec3 motion = this.golem.getDeltaMovement();
        this.golem.setDeltaMovement(
                motion.x + (dx / distance * 1.0 * 0.8F + motion.x * 0.2F),
                0.3,
                motion.z + (dz / distance * 1.0 * 0.8F + motion.z * 0.2F));
        this.entityPathNavigate.moveTo(this.entityPathEntity, this.nearSpeed);
    }

    /** Original {@code func_75251_c()} ({@code :83-85}): forget the creeper. */
    @Override
    public void stop() {
        this.closestLivingEntity = null;
    }

    /** 1.7.10 runs every live task every tick, so the speed switch is a per-tick test. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Original {@code func_75246_d()} ({@code :87-93}): near the creeper flee fast, far away flee slower. */
    @Override
    public void tick() {
        if (this.golem.distanceToSqr(this.closestLivingEntity) < NEAR_SQUARED) {
            this.golem.getNavigation().setSpeedModifier(this.nearSpeed);
        } else {
            this.golem.getNavigation().setSpeedModifier(this.farSpeed);
        }
    }

    /**
     * 1.7.10's {@code PathEntity.findTargetPos(Vec3)}: the last node must sit on the
     * floor of the wanted position. 1.21.1's {@code Path} exposes the same last node as
     * {@code getEndNode()}.
     */
    private static boolean pathReaches(final Path path, final Vec3 wanted) {
        final net.minecraft.world.level.pathfinder.Node end = path.getEndNode();
        if (end == null) {
            return false;
        }
        return end.x == Mth.floor(wanted.x) && end.y == Mth.floor(wanted.y) && end.z == Mth.floor(wanted.z);
    }
}
