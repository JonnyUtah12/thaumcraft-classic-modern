package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Ported {@code thaumcraft.common.entities.ai.interact.AIUseItem} (core 8, Use)
 * from {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/interact/AIUseItem.java}
 * (203 lines) — TC-M6-08-s-b2.
 *
 * <p>The original is installed by {@code EntityGolemBase.setupGolem()} at
 * <b>priority 0</b>, alongside {@code AIHomeReplace} (original
 * {@code EntityGolemBase.java:217-296}, core-8 case). The goal finds a marked
 * block whose marker colour matches the golem's carried item
 * ({@link EntityGolemBase#getColorsMatching(ItemStack)}, AIUseItem.java:182),
 * walks to it, faces it, and right-clicks it with the carried item — in the
 * original through a {@code FakePlayer} sitting at the golem's position
 * (AIUseItem.java:116-170). Cooldown is the original's
 * {@code nextTick} ladder: gap {@code d = 5 - ticksElapsed} clamped to ≥ 1,
 * next attempt at {@code ticksElapsed + d * 3} (AIUseItem.java:66-72), the
 * {@code nextTick = ticksElapsed + rand(6)} jitter at construction
 * (AIUseItem.java:49).</p>
 *
 * <p>Pinned constants, all from the original file: {@code setFlags(3)} →
 * {@code MOVE|LOOK} (AIUseItem.java:42); {@code distance = sqrt(range/3)}
 * (AIUseItem.java:43, the original keeps the field; it is not read);
 * activation squared range {@code <= 4.0} (AIUseItem.java:89); look speeds
 * 30°/30° (AIUseItem.java:87); walk count budget 200 (AIUseItem.java:100);
 * continue count 200 (AIUseItem.java:82, consumed by {@code count--}).</p>
 *
 * <h2>1.7.10 → 1.21.1</h2>
 * <p>The original drives the click through {@code ItemInWorldManager}
 * (the 1.7.10 server play manager) on a {@code FakePlayer}. 1.21.1 has no
 * manager and every public {@code UseOnContext} entry point in this tree
 * exposes a {@code @Nullable Player} — the exact shape vanilla dispensers
 * already use ({@code DispenseItemBehavior} passes a null player). The port
 * therefore clicks with {@code stack.getItem().useOn(new UseOnContext(level,
 * null, MAIN_HAND, carried, hit))}; no {@code Player} is faked or cast.
 * The sites where that is not equivalent are declared below.</p>
 */
public class AIUseItem extends GolemGoal {

    /** Target block coordinates — the original's {@code xx/yy/zz}. */
    private int xx;
    private int yy;
    private int zz;

    /** Original {@code distance = sqrt_float(range / 3)} — kept, unused (AIUseItem.java:43). */
    private final float distance;

    /** Original {@code block}/{@code blockMd} as one 1.21.1 state. */
    private BlockState block = Blocks.AIR.defaultBlockState();

    /** The colour of the marker the current target was found under. */
    private int color = -1;

    /** Original {@code count} — the continueExecuting budget. */
    private int count = 0;

    /** Original {@code nextTick} — the goal's activation cooldown stamp. */
    private int nextTick = 0;

    public AIUseItem(final EntityGolemBase golem) {
        super(golem);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.distance = Mth.sqrt(this.golem.getRange() / 3.0F);
        // Original AIUseItem.java:49 — `ticksElapsed + world.rand.nextInt(6)`, in a try/catch
        // that only existed because the original ran the line in the constructor before the
        // entity was fully constructed; 1.21.1 has no such hazard, so no catch.
        this.nextTick = this.golem.tickCount + this.level().random.nextInt(6);
    }

    @Override
    public boolean canUse() {
        // Per-core gate: the original installs this goal only in setupGolem()'s core-8 case.
        if (!this.coreIs(8)) {
            return false;
        }
        final boolean ignoreItem = !this.homeContainerProbe().isContainer();

        final int d = Math.max(1, 5 - this.golem.tickCount);
        if ((this.golem.getCarried() != null || ignoreItem)
                && this.golem.tickCount >= this.nextTick
                && this.golem.getNavigation().isDone()) {
            this.nextTick = this.golem.tickCount + d * 3;
            return this.findSomething();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.level().getBlockState(new BlockPos(this.xx, this.yy, this.zz)).equals(this.block)
                && this.count-- > 0
                && !this.golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.count = 200;
        this.golem.getNavigation().moveTo(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5, this.golem.getSpeed());
    }

    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5, 30.0F, 30.0F);
        final double dist = this.golem.distanceToSqr(this.xx + 0.5, this.yy + 0.5, this.zz + 0.5);
        if (dist <= 4.0) {
            this.click();
        }
    }

    @Override
    public void stop() {
        this.count = 0;
        this.golem.getNavigation().stop();
    }

    /**
     * The original's {@code click()} (AIUseItem.java:104-179): fake-player
     * right-click on the target block's marked face with the carried item.
     *
     * <p>DEVIATION (null player): the click runs with a {@code null} player,
     * the shape vanilla dispensers already use. Item/block paths written
     * null-safe (block placement, bucket emptying, buttons, levers, doors)
     * behave; paths that need a real player (menus, inventory UIs, in-air
     * {@code Item#use}, which is Player-typed in this tree) do nothing for
     * the golem. The original also swept the fake player's inventory (slots
     * 1..n) into the carried stack after the click (AIUseItem.java:160-170)
     * to capture swap results (bucket → water bucket); with no player
     * inventory there is nothing to sweep — null-player use paths shrink the
     * passed stack in place, which the golem sees directly.</p>
     */
    void click() {
        final BlockEntityProbe homeProbe = this.homeContainerProbe();
        final boolean ignoreItem = !homeProbe.isContainer();
        final ItemStack carried = this.golem.getCarried();

        final BlockPos target = new BlockPos(this.xx, this.yy, this.zz);
        final List<Integer> sides =
                GolemHelper.getMarkedSides(this.golem, this.xx, this.yy, this.zz, dimensionOf(this.level()), (byte) this.color);
        if (sides.isEmpty()) {
            return;
        }
        final int side = sides.get(0);
        int x = 0;
        int y = 0;
        int z = 0;
        final BlockState markerState = this.level().getBlockState(target);
        // DEVIATION: 1.7.10 `world.isBlockSolid(x,y,z)` (AIUseItem.java:131) becomes
        // `BlockState.isSolid()` — the closest 1.21.1 predicate for "this block is a
        // full cube the golem reaches through".
        if (markerState.isSolid()) {
            final Direction opp = GolemHelper.directionOf(side).getOpposite();
            x = opp.getStepX();
            y = opp.getStepY();
            z = opp.getStepZ();
        }

        if (carried == null && !ignoreItem) {
            this.stop();
            return;
        }

        final BlockPos clicked = target.offset(x, y, z);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked), GolemHelper.directionOf(side), clicked, false);
        try {
            if (this.golem.getToggles()[1]) {
                // Original AIUseItem.java:145-147: toggles[1] → left-arm bare block
                // activation (no item). 1.21.1's item interaction entry point
                // `BlockBehaviour$BlockStateBase#useItemOn` carries a @Nullable Player, the
                // dispenser shape; blocks that need a real player (menus) pass.
                // SEAM: 1.7.10's `ItemInWorldManager.onBlockActivated` had no item and no
                // container-menu concept; blocks that only act through a ServerPlayer menu
                // (chests opening, crafting tables) are inert for the golem in 1.21.1.
                this.golem.startLeftArmTimer();
                markerState.useItemOn(ItemStack.EMPTY, this.level(), null, InteractionHand.MAIN_HAND, hit);
            } else if (carried != null) {
                final InteractionResult result = carried.getItem()
                        .useOn(new UseOnContext(this.level(), null, InteractionHand.MAIN_HAND, carried, hit));
                if (result.consumesAction()) {
                    this.golem.startRightArmTimer();
                }
                // SEAM: the original's in-air fallback `useItemRightClick`
                // (ItemInWorldManager.activateBlockOrUseItem, AIUseItem.java:148-153) is
                // `Item#use(Level, Player, InteractionHand)` in this tree — Player-typed,
                // no non-player user exists, so the golem never starts a use duration.
            }
            // Original AIUseItem.java:155-158: carried = the player's held stack, nulled
            // when emptied. The ported click mutates the carried stack in place; the
            // getCarried()/setCarried pair performs the same emptiness→null normalisation.
            this.golem.setCarried(this.golem.getCarried());
            // SEAM: the original's `golem.updateCarried()` (AIUseItem.java:172) refreshes the
            // carried display stack; the display stack is owned by TC-M6-08-s-c in this tree.
            this.stop();
        } catch (final Exception e) {
            // Original AIUseItem.java:174-176: any click exception aborts the walk.
            this.stop();
        }
    }

    /**
     * The original's {@code findSomething()} (AIUseItem.java:181-201): for each
     * slot colour matching the carried stack, for each marker with that colour
     * (the {@code -1} colour is the wildcard), the marker block must be solid
     * iff toggle 0 is set (the original's XOR, AIUseItem.java:185-186 — with
     * toggle 0 off no marker ever qualifies), and the block behind the marker
     * face must be solid.
     */
    boolean findSomething() {
        for (final byte col : this.golem.getColorsMatching(this.golem.getCarried())) {
            for (final Marker marker : this.golem.getMarkers()) {
                final boolean solid = this.level().getBlockState(new BlockPos(marker.x, marker.y, marker.z)).isSolid();
                final boolean[] toggles = this.golem.getToggles();
                if ((marker.color == col || col == -1)
                        && (!toggles[0] || solid)
                        && (toggles[0] || !solid)) {
                    final Direction opp = GolemHelper.directionOf(marker.side).getOpposite();
                    final BlockState behind = this.level().getBlockState(
                            new BlockPos(marker.x + opp.getStepX(), marker.y + opp.getStepY(), marker.z + opp.getStepZ()));
                    if (behind.isSolid()) {
                        this.color = col;
                        this.xx = marker.x;
                        this.yy = marker.y;
                        this.zz = marker.z;
                        this.block = this.level().getBlockState(new BlockPos(this.xx, this.yy, this.zz));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The home-container check the original repeats in canUse/click
     * (AIUseItem.java:56-64, 105-114): the block behind the home facing.
     */
    private BlockEntityProbe homeContainerProbe() {
        final BlockPos pos = GolemHelper.homeContainerPos(this.golem);
        return new BlockEntityProbe(this.level().getBlockEntity(pos) instanceof Container);
    }

    /** Tiny record so the tile probe reads like the original's two-step check. */
    private record BlockEntityProbe(boolean container) {
        boolean isContainer() {
            return this.container;
        }
    }

    /**
     * Dimension identity for {@link GolemHelper#getMarkedSides} — the same
     * formula {@code GolemHelper.dimensionOf} uses (package-private there).
     */
    private static int dimensionOf(final Level level) {
        return level.dimension().location().hashCode();
    }
}
