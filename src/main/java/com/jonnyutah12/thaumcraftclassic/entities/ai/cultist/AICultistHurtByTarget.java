package com.jonnyutah12.thaumcraftclassic.entities.ai.cultist;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;

import java.util.List;

/**
 * The cultist gang call — {@code thaumcraft.common.entities.ai.combat.AICultistHurtByTarget},
 * target slot 1 of both registered cultists ({@code EntityCultistKnight:34},
 * {@code EntityCultistCleric:44}) and of the Leader (c5).
 *
 * <p>Original: {@code extends EntityAITarget} with {@code shouldCheckSight =
 * false} and {@code setPriority(1)}. {@code shouldExecute} ({@code :23-25}) is
 * "the hurt timestamp moved and the attacker is a suitable target";
 * {@code start} ({@code :27-56}) sets the owner's target to the attacker, records
 * the timestamp, and — because both cultists pass {@code callsForHelp = true} —
 * scans the help box: the unit cube at the owner's feet inflated by the follow
 * range on x/z and 10.0 on y ({@code :35-43}), over {@code EntityCultist.class}
 * (the whole family, not the mob's own class), and hands the attacker to every
 * cultist that has no target and cannot attack the attacker
 * ({@code :45-47}). A cleric mid-ritual is left alone unless
 * {@code world.rand.nextInt(3) == 0}, which drops the ritual and joins
 * ({@code :48-53}).</p>
 *
 * <p>1.21.1's home for this goal is {@link HurtByTargetGoal}, whose own
 * {@code alertOthers} is the same box arithmetic ({@code AABB.unitCubeFromLowerCorner}
 * inflated by the follow range and 10 — 1.7.10's Thaumcraft copied vanilla's, and
 * 1.21.1 kept it). The port subclasses it and keeps the original's scan, because
 * the two differences are the family's: the box is over the {@code EntityCultist}
 * base, and the cleric's ritual rule lives inside it. The parent's
 * {@code setAlertOthers} is never enabled, so the vanilla reinforcement path
 * stays off.</p>
 *
 * <p>The sight gate: 1.7.10's {@code EntityAITarget(owner, false)} means "no
 * line of sight required". 1.21.1's {@code HurtByTargetGoal} passes
 * {@code checkSight = true} to {@code TargetGoal} but its {@code canUse} tests
 * the attacker with {@code HURT_BY_TARGETING = forCombat().ignoreLineOfSight()},
 * so the effective gate is the original's. Recorded in the class docs, not
 * worked around.</p>
 */
public class AICultistHurtByTarget extends HurtByTargetGoal {

    /** {@code :14} — {@code boolean entityCallsForHelp}. */
    private final boolean callsForHelp;
    /** {@code :15} — {@code private int field_142052_b}: the last seen timestamp. */
    private int timestamp;

    public AICultistHurtByTarget(final PathfinderMob owner, final boolean callsForHelp) {
        // :16-19 — super(owner, false); setPriority(1). TargetGoal's checkSight
        // is inert in canUse (see the class javadoc); the target-list slot is 1
        // (TARGET_SLOT_HURT), which is what 1.21.1's priority is.
        super(owner);
        this.callsForHelp = callsForHelp;
    }

    /** {@code :23-25} — the timestamp moved and the attacker passes the
     *  suitability test. The parent's {@code canUse} is exactly that
     *  ({@code getLastHurtByMobTimestamp()} against the cached timestamp, then
     *  {@code canAttack(attacker, HURT_BY_TARGETING)}). */
    @Override
    public boolean canUse() {
        return super.canUse();
    }

    /**
     * {@code :27-56} — {@code start()}: target the attacker, remember the
     * timestamp, run the gang call, then arm the goal. The order of the
     * original's own body is kept (its {@code super.start()} runs last); the
     * parent's {@code start()} is the 1.21.1 form of "set the target, remember
     * the timestamp, remember the unseen-memory budget", and its reinforcement
     * pass is off.
     */
    @Override
    public void start() {
        super.start();                                                                  // :29-32
        if (this.callsForHelp) {
            callTheGang();                                                              // :33-54
        }
    }

    /** {@code :35-54} — the help box, verbatim. */
    private void callTheGang() {
        final LivingEntity attacker = this.mob.getLastHurtByMob();
        if (attacker == null) {
            return;
        }
        final double range = getFollowDistance();                                       // :35
        final AABB box = AABB.unitCubeFromLowerCorner(this.mob.position())
                .inflate(range, CultistAiLogic.HELP_BOX_Y_EXPAND, range);               // :36-43
        final Level level = this.mob.level();
        final List<EntityCultist> gang = level.getEntitiesOfClass(EntityCultist.class, box,
                net.minecraft.world.entity.EntitySelector.NO_SPECTATORS);
        for (final EntityCultist cultist : gang) {
            if (!CultistAiLogic.helpsTheHurt(cultist == this.mob, cultist.getTarget() != null)) {
                continue;                                                               // :45-46
            }
            if (cultist.canAttack(attacker)) {
                continue;                                                               // :47
            }
            if (!(cultist instanceof EntityCultistCleric cleric) || !cleric.getIsRitualist()) {
                cultist.setTarget(attacker);                                            // :49-50
            } else if (level.getRandom().nextInt(CultistAiLogic.RITUALIST_BREAK_ROLL) == 0) {
                cleric.setIsRitualist(false);                                           // :52
                cultist.setTarget(attacker);                                            // :53
            }
        }
    }
}
