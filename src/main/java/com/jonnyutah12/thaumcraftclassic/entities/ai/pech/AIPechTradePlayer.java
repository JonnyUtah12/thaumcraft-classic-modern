package com.jonnyutah12.thaumcraftclassic.entities.ai.pech;

import java.util.EnumSet;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The Pech's stand-still trade goal — a port of
 * {@code thaumcraft.common.entities.ai.pech.AIPechTradePlayer} (35 lines),
 * installed at goal slot 1 ({@code EntityPech.java:96}). It is the vanilla
 * villager-trade goal copied into Thaumcraft (the field is even named
 * {@code villager}), and it is all the original ships: it holds MOVE and JUMP so
 * a tamed pech that is trading does not walk away, clears the path once, and
 * clears the flag when it stops.
 *
 * <p>Gate by gate ({@code :14-26}): not dead, not in water, tamed, on ground,
 * {@code velocityChanged} clear, and {@code trading}. The 1.21.1 seams:
 * {@code func_70089_S() = isDead} → {@code isRemoved()},
 * {@code func_70090_H() = isInWater}, {@code field_70122_E = onGround},
 * {@code field_70133_I = velocityChanged} → 1.21.1's {@code Entity#wasDirty}
 * (the renamed field), {@code func_75248_a(5)} = MOVE | JUMP,
 * {@code func_75499_g()} = {@code PathNavigation#recomputePath()}
 * ({@code :29}), {@code trading = false} on stop ({@code :33}).</p>
 *
 * <p>The flag it keys off is public on the pech ({@code EntityPech.java:69}) and
 * is written by the trade GUI, i.e. by the client/container lane this card leaves
 * out of scope (BD-67: the menu and its screen need content this slice does not own) — so on a headless server the goal can never start. It ships so
 * the goal slot, the mutex bits and the gates are the original's, and a probe
 * drives the flag directly to prove the gate table. Recorded in the row's
 * {@code known_differences}.</p>
 */
public class AIPechTradePlayer extends Goal {

    private final EntityPech pech;

    public AIPechTradePlayer(final EntityPech pech) {
        this.pech = pech;
        // :11 — func_75248_a(5): 1.7.10 mutex bit 1 = MOVE, bit 4 = JUMP
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.pech.isRemoved()) {
            return false;                                                       // :15
        } else if (this.pech.isInWater()) {
            return false;                                                       // :17
        } else if (!this.pech.isTamed()) {
            return false;                                                       // :19
        } else if (!this.pech.onGround()) {
            return false;                                                       // :21
        } else {
            // :24 — the original's ternary: `field_70133_I ? false : trading`.
            // field_70133_I = velocityChanged (fields_1.7.10_mcp_stable_11.csv);
            // 21.1.259 keeps that flag private, so EntityPech#isStandingStill
            // supplies the observable state and the rule stays on the logic class.
            return PechAiLogic.canStartTrade(this.pech.isStandingStill(), this.pech.trading);
        }
    }

    @Override
    public void start() {
        // :29 — func_70661_as().func_75499_g(): the navigation's "stop walking
        // the current path" call. The methods CSV is not vendored, so the shape
        // is read from the CALLER (a trade starts by standing still): 1.21.1's
        // PathNavigation#stop() clears the path; recomputePath() would re-run
        // the path it already has, which is not a goal-start behaviour.
        this.pech.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.pech.trading = false;                                              // :33
    }
}
