package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

/**
 * Base for the ported per-core golem goals (TC-M6-08-s-b2).
 *
 * <p>The original's goal classes all extend {@code EntityAIBase} and take the
 * golem in their constructor (every file under
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/**});
 * 1.21.1's {@code Goal} is the same shape — {@code canUse}/{@code start}/
 * {@code tick}/{@code stop} — so this base adds nothing but the golem
 * reference, the {@code -1} core guard, and the golem's own tick-delay
 * reduction, which the originals reach through {@code EntityAIBase}.</p>
 *
 * <p>The goals that the original inherits from <em>vanilla</em> AI
 * ({@code EntityAIAttackOnCollide}, {@code EntityAIOpenDoor},
 * {@code EntityAITarget}, {@code EntityAIWatchClosest},
 * {@code EntityAILookIdle}) extend the matching 1.21.1 vanilla goal instead of
 * this class — {@code MeleeAttackGoal}, {@code OpenDoorGoal},
 * {@code target.*}, {@code LookAtPlayerGoal}, {@code RandomLookAroundGoal} —
 * because those vanilla goals are the same code the original copied.</p>
 */
public abstract class GolemGoal extends Goal {

    /** The golem this goal runs for — the original's {@code field_7526x} style field. */
    protected final EntityGolemBase golem;

    /** The golem's level, captured once (an entity never moves levels). */
    protected final Level level;

    protected GolemGoal(final EntityGolemBase golem) {
        this.golem = golem;
        this.level = golem.level();
    }

    /** The golem's level ({@code worldObj} in the original). */
    protected final Level level() {
        return this.level;
    }

    /**
     * The original's per-core gate: the goals are installed by
     * {@code setupGolem()}'s {@code switch (this.getCore())}, so a goal only
     * ever runs for its own core. {@code -1} is "no core" —
     * {@code GolemCoreTask.NO_CORE}.
     */
    protected final boolean coreIs(final int id) {
        return this.golem.getCore() == id;
    }

    /**
     * The original's {@code EntityAIBase.func_75400_b(int)} — the "reduced tick
     * delay" helper the vanilla goals use to halve a cooldown. 1.21.1's
     * {@code Goal.reducedTickDelay(int)} is the same helper, so the ported
     * goals call this and read like the originals.
     */
    protected static int reducedTickDelay(final int ticks) {
        return Goal.reducedTickDelay(ticks);
    }

    /** The simple class name, for {@code GolemCoreTask.goalNames()} parity checks. */
    public final String goalName() {
        return this.getClass().getSimpleName();
    }
}
