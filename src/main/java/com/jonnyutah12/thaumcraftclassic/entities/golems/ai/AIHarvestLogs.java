package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Ported {@code thaumcraft.common.entities.ai.interact.AIHarvestLogs} (core 7,
 * Chop) from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/interact/AIHarvestLogs.java}
 * (182 lines) — TC-M6-08-s-b2.
 *
 * <p>The original is installed by {@code EntityGolemBase.setupGolem()} at
 * <b>priority 2</b> (core-7 case, original {@code EntityGolemBase.java:217-296}).
 * Pinned constants, all from the original file: {@code setFlags(3)} →
 * {@code MOVE|LOOK} (AIHarvestLogs.java:39); scan {@code distance =
 * sqrt_float(range / 3)} — the {@code golem.getRange()} range gate (AIHarvestLogs.java:40);
 * activation gate {@code ticksElapsed % Config.golemDelay <= 0}
 * (AIHarvestLogs.java:47, {@link TcConfigCatalog#GOLEM_DELAY}); chop timer
 * {@code max(5, (20 - strength * 3) * blockHardness)} (AIHarvestLogs.java:76-78);
 * cadence {@code mod = delay / round(delay / 6)} (AIHarvestLogs.java:80);
 * reach squared distance {@code <= 4.0} (AIHarvestLogs.java:74); sound formula
 * {@code (volume + 0.7) / 8}, {@code pitch * 0.5} (AIHarvestLogs.java:92-93);
 * progress stage {@code 9 * (1 - delay / maxDelay)} (AIHarvestLogs.java:96);
 * walk count 200 (AIHarvestLogs.java:149); tree bounds 24/48/24 and scan radius
 * ±2 from the current farthest block (AIHarvestLogs.java:169-171 in
 * {@code BlockUtils.findBlocks}); the log-adjacency sweep is the 3×3×3 around
 * the target (AIHarvestLogs.java:116-118).</p>
 *
 * <h2>1.7.10 → 1.21.1</h2>
 * <p>The original's {@code Utils.isWoodLog} (canSustainLeaves ∪ the {@code
 * LOGS} ore dict, {@code Utils.java:187-195}) becomes {@code
 * state.is(BlockTags.LOGS)} per the card; the tree scan is the original's
 * wood-log walk (leaves, {@code BlockTags.LEAVES}, enter the scan only where
 * the original's block list did — the original's {@code findBlocks} walk is
 * logs-only, {@code BlockUtils.java:176}, so this port is logs-only too).
 * The fake-player harvest of {@code BlockUtils.breakFurthestBlock} becomes
 * {@code level.destroyBlock(pos, true, null)} — see the deviations at the
 * call site.</p>
 */
public class AIHarvestLogs extends GolemGoal {

    /** Target block coordinates — the original's {@code xx/yy/zz}. */
    private int xx;
    private int yy;
    private int zz;

    /** Original {@code distance = sqrt_float(range / 3)} (AIHarvestLogs.java:40). */
    private final float distance;

    /** Original {@code block}/{@code blockMd} as one 1.21.1 state. */
    private BlockState block = Blocks.AIR.defaultBlockState();

    /** Original chop countdown (AIHarvestLogs.java:30-32). */
    private int delay = -1;
    private int maxDelay = 1;
    private int mod = 1;

    /** Original {@code count} — the continueExecuting budget. */
    private int count = 0;

    public AIHarvestLogs(final EntityGolemBase golem) {
        super(golem);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.distance = Mth.sqrt(this.golem.getRange() / 3.0F);
    }

    @Override
    public boolean canUse() {
        // Per-core gate: the original installs this goal only in setupGolem()'s core-7 case.
        if (!this.coreIs(7)) {
            return false;
        }
        if (this.delay < 0
                && this.golem.tickCount % TcConfig.getInt(TcConfigCatalog.GOLEM_DELAY) <= 0
                && this.golem.getNavigation().isDone()) {
            final BlockPos found = this.findLog();
            if (found == null) {
                return false;
            }
            this.xx = found.getX();
            this.yy = found.getY();
            this.zz = found.getZ();
            this.block = this.level().getBlockState(found);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.level().getBlockState(new BlockPos(this.xx, this.yy, this.zz)).equals(this.block)
                && this.count-- > 0
                && (this.delay > 0
                        || isLog(this.level().getBlockState(new BlockPos(this.xx, this.yy, this.zz)))
                        || !this.golem.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.count = 200;
        this.golem.getNavigation().moveTo(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5, this.golem.getSpeed());
    }

    @Override
    public void tick() {
        final BlockPos pos = new BlockPos(this.xx, this.yy, this.zz);
        final double dist = this.golem.distanceToSqr(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5);
        this.golem.getLookControl().setLookAt(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5, 30.0F, 30.0F);
        if (dist <= 4.0) {
            if (this.delay < 0) {
                // DEVIATION: 1.7.10 `block.getBlockHardness(world,x,y,z)` (AIHarvestLogs.java:77)
                // is `BlockState.getDestroySpeed(level, pos)` in 1.21.1.
                this.delay = (int) Math.max(5.0F,
                        (20.0F - this.golem.getGolemStrength() * 3.0F) * this.block.getDestroySpeed(this.level(), pos));
                this.maxDelay = this.delay;
                this.mod = (int) (this.delay / Math.round(this.delay / 6.0F));
            }

            if (this.delay > 0) {
                if (--this.delay > 0 && this.delay % this.mod == 0 && this.golem.getNavigation().isDone()) {
                    this.golem.startActionTimer();
                    // DEVIATION: 1.7.10's raw material sound string (AIHarvestLogs.java:86-94)
                    // becomes the state's SoundType step (dig) sound; the volume/pitch
                    // formula ((v + 0.7) / 8, p * 0.5) is kept.
                    final SoundType soundType = this.block.getSoundType();
                    this.level().playSound(null, this.xx + 0.5F, this.yy + 0.5F, this.zz + 0.5F,
                            soundType.getStepSound(), SoundSource.BLOCKS,
                            (soundType.getVolume() + 0.7F) / 8.0F, soundType.getPitch() * 0.5F);
                    // 1.7.10 destroyBlockPartially is ServerLevel#destroyBlockProgress.
                    this.level().destroyBlockProgress(this.golem.getId(), pos,
                            (int) (9.0F * (1.0F - (float) this.delay / this.maxDelay)));
                }

                if (this.delay == 0) {
                    this.harvest();
                    if (isLog(this.level().getBlockState(pos))) {
                        this.delay = -1;
                        this.block = this.level().getBlockState(pos);
                        this.start();
                    } else {
                        this.checkAdjacent();
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        this.level().destroyBlockProgress(this.golem.getId(), new BlockPos(this.xx, this.yy, this.zz), -1);
        this.delay = -1;
    }

    /**
     * The original's {@code harvest()} (AIHarvestLogs.java:153-158): play the
     * break event, then {@code BlockUtils.breakFurthestBlock} — fell the
     * <em>farthest</em> log of the connected tree so a tall trunk is chopped
     * from the top down — and start the action animation.
     */
    void harvest() {
        this.count = 200;
        final BlockPos pos = new BlockPos(this.xx, this.yy, this.zz);
        // DEVIATION: the original plays sound event 2001 keyed on
        // `blockId + (meta << 12)` (AIHarvestLogs.java:155) — the 1.7.10 block-break
        // note; 1.21.1 plays the state's break sound directly.
        this.level().playSound(null, pos, this.block.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

        final BlockPos furthest = this.findFurthestConnectedLog(pos, this.block);
        // DEVIATION: the original breaks through a FakePlayer
        // (BlockUtils.breakFurthestBlock → harvestBlock: canHarvestBlock + drop with
        // the player, BlockUtils.java:202-229, 39-70). destroyBlock(pos, true, null)
        // is the 1.21.1 equivalent: loot-table drops with no tool, no fortune.
        this.level().destroyBlock(furthest, true, null);
        this.golem.startActionTimer();
        // SEAM: the original's client display work around the felled block —
        // 3×3×3 randomDisplayTick scheduling and the two markBlockRangeForRenderUpdate
        // calls (BlockUtils.java:213-226, the @SideOnly particle path) — is dropped;
        // destroyBlock's UPDATE_ALL flags carry the block updates.
    }

    /**
     * The original's {@code checkAdjacent()} (AIHarvestLogs.java:115-141): the
     * 3×3×3 around the target, bounded by the home distance, retargets the
     * next log of the same trunk.
     */
    private void checkAdjacent() {
        for (int x2 = -1; x2 <= 1; x2++) {
            for (int z2 = -1; z2 <= 1; z2++) {
                for (int y2 = -1; y2 <= 1; y2++) {
                    final int x = this.xx + x2;
                    final int y = this.yy + y2;
                    final int z = this.zz + z2;
                    final BlockPos home = this.golem.getRestrictCenter();
                    if (!(Math.abs(home.getX() - x) > this.distance)
                            && !(Math.abs(home.getY() - y) > this.distance)
                            && !(Math.abs(home.getZ() - z) > this.distance)
                            && isLog(this.level().getBlockState(new BlockPos(x, y, z)))) {
                        this.xx = x;
                        this.yy = y;
                        this.zz = z;
                        this.block = this.level().getBlockState(new BlockPos(x, y, z));
                        this.delay = -1;
                        this.start();
                        return;
                    }
                }
            }
        }
    }

    /**
     * The original's {@code findLog()} (AIHarvestLogs.java:160-181): random
     * sampling in a box of {@code 2·distance × 3 × 2·distance} around the
     * home, {@code distance * 4} attempts, then a walk DOWN to the lowest
     * connected log that is closer to the golem (chop from the base).
     */
    private BlockPos findLog() {
        final RandomSource rand = this.golem.getRandom();
        final BlockPos home = this.golem.getRestrictCenter();

        for (int var2 = 0; var2 < this.distance * 4.0F; var2++) {
            final int x = (int) (home.getX() + rand.nextInt((int) (1.0F + this.distance * 2.0F)) - this.distance);
            final int y = (int) (home.getY() + rand.nextInt((int) (1.0F + this.distance)) - this.distance / 2.0F);
            final int z = (int) (home.getZ() + rand.nextInt((int) (1.0F + this.distance * 2.0F)) - this.distance);
            if (isLog(this.level().getBlockState(new BlockPos(x, y, z)))) {
                BlockPos v = new BlockPos(x, y, z);
                double dist = this.golem.distanceToSqr(x + 0.5, y + 0.5, z + 0.5);

                for (int yy = 1;
                        isLog(this.level().getBlockState(new BlockPos(x, y - yy, z)))
                                && this.golem.distanceToSqr(x + 0.5, y - yy + 0.5, z + 0.5) < dist; yy++) {
                    v = new BlockPos(x, y - yy, z);
                    dist = this.golem.distanceToSqr(x + 0.5, y - yy + 0.5, z + 0.5);
                }
                return v;
            }
        }
        return null;
    }

    /**
     * The ported {@code BlockUtils.findBlocks} walk (BlockUtils.java:163-190,
     * as driven by {@code breakFurthestBlock}): from the current farthest
     * point, scan the 5×5×5 (top-down y, so the canopy wins) for a same-block
     * wood log with hardness ≥ 0 that is strictly farther from the origin;
     * restart from it; abort on the 24/48/24 bounds. The 1.7.10 recursion is
     * an iterative restart — same fixed point.
     */
    private BlockPos findFurthestConnectedLog(final BlockPos origin, final BlockState base) {
        int lx = origin.getX();
        int ly = origin.getY();
        int lz = origin.getZ();
        double lastDistance = 0.0;
        final Vec3i originVec = new Vec3i(origin.getX(), origin.getY(), origin.getZ());

        boolean moved = true;
        while (moved) {
            moved = false;
            for (int xx = -2; xx <= 2 && !moved; xx++) {
                for (int yy = 2; yy >= -2 && !moved; yy--) {
                    for (int zz = -2; zz <= 2 && !moved; zz++) {
                        // Original bounds (BlockUtils.java:169-173): abort the walk,
                        // keeping the current farthest point.
                        if (Math.abs(lx + xx - origin.getX()) > 24
                                || Math.abs(ly + yy - origin.getY()) > 48
                                || Math.abs(lz + zz - origin.getZ()) > 24) {
                            return new BlockPos(lx, ly, lz);
                        }
                        final BlockPos test = new BlockPos(lx + xx, ly + yy, lz + zz);
                        final BlockState state = this.level().getBlockState(test);
                        if (state.is(base.getBlock()) && isLog(state)
                                && state.getDestroySpeed(this.level(), test) >= 0.0F) {
                            final double d = test.distSqr(originVec);
                            if (d > lastDistance) {
                                lastDistance = d;
                                lx += xx;
                                ly += yy;
                                lz += zz;
                                moved = true;
                            }
                        }
                    }
                }
            }
        }
        return new BlockPos(lx, ly, lz);
    }

    /**
     * {@code Utils.isWoodLog} (Utils.java:187-195) as the 1.21.1
     * {@code minecraft:logs} block tag (the card's mapping of the 1.7.10
     * ore-dict {@code listTree} set).
     */
    private static boolean isLog(final BlockState state) {
        return state.is(BlockTags.LOGS);
    }
}
