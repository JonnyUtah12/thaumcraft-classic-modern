package com.jonnyutah12.thaumcraftclassic.entities.ai.cultist;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;

/**
 * The altar watch — {@code thaumcraft.common.entities.ai.misc.AIAltarFocus},
 * goal slot 1 of the Crimson Cleric ({@code EntityCultistCleric:35}).
 *
 * <p>Original: {@code extends EntityAIBase} with {@code setPriority(7)} (the
 * slot is what 1.21.1 uses). {@code shouldExecute} and {@code start}
 * ({@code :22-27}) are "the cleric is a ritualist and has a home";
 * {@code start}/{@code startReading} bodies are empty — the goal moves nothing,
 * it owns the ritual. {@code shouldContinueExecute} ({@code :31-33}) is the same
 * pair. {@code updateTask} ({@code :35-45}) runs on the cleric's
 * {@code ticksExisted % 40} cadence and drops the ritual when either arm fires:
 * the squared distance from the home to the cleric's <b>block-truncated</b>
 * position exceeds 16.0F (so 4 blocks, compared squared), or the home block is
 * no longer {@code ConfigBlocks.blockEldritch}.</p>
 *
 * <p>1.21.1 keeps the shape one for one. The block test reads the live
 * {@code thaumcraft:blockeldritch} ({@code CatalogEldritchRealm.BLOCK_ELDRITCH},
 * a registered block in this port), and the distance test keeps the original's
 * {@code (int)} truncation of the cleric's position — the pair is pinned on
 * {@link CultistAiLogic#ALTAR_MAX_DIST_SQ} and
 * {@link CultistAiLogic#ALTAR_HOME_BLOCK}.</p>
 *
 * <p>No flags: the original's {@code EntityAIBase} declares none, so the goal
 * runs beside the movement goals, and 1.21.1's {@code Goal} default
 * ({@code EnumSet.noneOf}) is the same. The goal is the only writer that clears
 * the ritual from the world side; the damage clear is
 * {@code EntityCultistCleric#hurt}.</p>
 */
public class AIAltarFocus extends Goal {

    private final EntityCultistCleric cleric;
    private final Level level;

    public AIAltarFocus(final EntityCultistCleric cleric) {
        this.cleric = cleric;
        this.level = cleric.level();
    }

    /** {@code :22-24} — ritualist, and a home exists. */
    @Override
    public boolean canUse() {
        return this.cleric.getIsRitualist() && this.cleric.getHomePosition() != null;
    }

    /** {@code :25} — {@code start()} is empty in the original. */
    @Override
    public void start() {
    }

    /** {@code :26} — {@code startReading()}/{@code stop()} is empty. */
    @Override
    public void stop() {
    }

    /** {@code :31-33} — the same pair as {@code canUse}. */
    @Override
    public boolean canContinueToUse() {
        return this.cleric.getIsRitualist() && this.cleric.getHomePosition() != null;
    }

    /**
     * {@code :35-45} — {@code updateTask()}: every 40th tick of the cleric's age,
     * drop the ritual when the squared distance to the home (measured to the
     * cleric's integer position, as the original's {@code (int)} casts do) is
     * over 16.0F, or when the home block is not eldritch.
     */
    @Override
    public void tick() {
        final BlockPos home = this.cleric.getHomePosition();
        if (home == null) {
            return;
        }
        if (this.cleric.tickCount % CultistAiLogic.ALTAR_CADENCE_TICKS != 0) {          // :36
            return;
        }
        final Vec3 feet = new Vec3((int) this.cleric.getX(), (int) this.cleric.getY(),
                (int) this.cleric.getZ());                                              // :37
        if (new Vec3(home.getX(), home.getY(), home.getZ()).distSqr(feet) > CultistAiLogic.ALTAR_MAX_DIST_SQ                       // :37
                || !this.level.getBlockState(home).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) { // :38-43
            this.cleric.setIsRitualist(false);                                          // :44
        }
    }
}
