package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockManaPod;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Ported {@code thaumcraft.common.entities.ai.interact.AIHarvestCrops} (core 3,
 * Harvest) from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/interact/AIHarvestCrops.java}
 * (279 lines) — TC-M6-08-s-b2.
 *
 * <p>The original is installed by {@code EntityGolemBase.setupGolem()} at
 * <b>priority 2</b> (core-3 case, original {@code EntityGolemBase.java:217-296}).
 * Pinned constants, all from the original file: {@code setFlags(3)} →
 * {@code MOVE|LOOK} (AIHarvestCrops.java:55); scan {@code distance =
 * sqrt_float(range / 4)} (AIHarvestCrops.java:56); activation gate
 * {@code ticksElapsed % Config.golemDelay <= 0} (AIHarvestCrops.java:60, the
 * config lives in {@link TcConfigCatalog#GOLEM_DELAY}); chop timer
 * {@code max(10, (20 - strength * 2) * blockHardness)} (AIHarvestCrops.java:89-91);
 * animation cadence {@code mod = delay / round(delay / 6)} (AIHarvestCrops.java:93);
 * reach squared distance {@code <= 4.0} (AIHarvestCrops.java:87); sound volume
 * {@code (volume + 0.7) / 8}, pitch {@code * 0.5} (AIHarvestCrops.java:104-106);
 * walk count 200 (AIHarvestCrops.java:154-156, 185); neighbour sweep ±2 x/z,
 * ±1 y (AIHarvestCrops.java:122-124); home scan y ±3 (AIHarvestCrops.java:175).</p>
 *
 * <h2>The crop list (mirrors the original, cited)</h2>
 * <p>The original asks {@code CropUtils.isGrownCrop} — a dynamic registry. Its
 * hard-wired registrations are carrots, potatoes and the mana pod as standard
 * crops and cactus and sugar cane as stacked crops
 * ({@code Config.java:871-875}), and {@code isGrownCrop} itself accepts any
 * fully-grown {@code BlockCrops}, nether wart at meta ≥ 3, a fully-grown
 * pumpkin stem, and stacked crops only above their bottom block
 * ({@code CropUtils.java:72-100}); the {@code IGrowable} branch explicitly
 * <em>excludes</em> {@code BlockStem} ({@code CropUtils.java:91}). The port
 * reproduces that list over 1.21.1 state predicates (see
 * {@link #isGrownCrop(Level, BlockPos)}): {@code CropBlock.isMaxAge},
 * nether wart at max age, cactus/sugar-cane stacks above their base, and
 * melon/pumpkin <b>stems are not crops</b> (the card's instruction, and the
 * original's own {@code BlockStem} exclusion). Thaumic-crop IMC registrations
 * ({@code Thaumcraft.java:183-197}) have no senders in this tree — a seam.</p>
 *
 * <h2>1.7.10 → 1.21.1 harvest model</h2>
 * <p>DEVIATION: the original removes a non-clickable ripe crop with
 * {@code world.setBlockToAir} — no drops at all (AIHarvestCrops.java:192) —
 * and only replants when upgrade 4 pulls the drops in and seeds are used
 * (AIHarvestCrops.java:193-273). The port harvests canonically:
 * {@code level.destroyBlock(pos, true, null)} — the 1.21.1 drop route with no
 * tool and no fortune, the {@code Block.dropResources}/{@code ToolData} route
 * that needs tool context is deliberately avoided — then replants a
 * {@link CropBlock} at {@code AGE = 0} (the original's metadata 0, mirroring
 * its seed/pod replant at {@code AIHarvestCrops.java:231}).</p>
 */
public class AIHarvestCrops extends GolemGoal {

    /** Target block coordinates — the original's {@code xx/yy/zz}. */
    private int xx;
    private int yy;
    private int zz;

    /** Original {@code distance = sqrt_float(range / 4)} (AIHarvestCrops.java:56). */
    private final float distance;

    /** Original {@code block}/{@code blockMd} as one 1.21.1 state. */
    private BlockState block = Blocks.AIR.defaultBlockState();

    /** Original chop countdown (AIHarvestCrops.java:46-48). */
    private int delay = -1;
    private int maxDelay = 1;
    private int mod = 1;

    /** Original {@code count} — the continueExecuting budget. */
    private int count = 0;

    /** Original {@code checklist}: shuffled column grid around the home (AIHarvestCrops.java:50,161-169). */
    private final List<BlockPos> checklist = new ArrayList<>();

    public AIHarvestCrops(final EntityGolemBase golem) {
        super(golem);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.distance = Mth.sqrt(this.golem.getRange() / 4.0F);
    }

    @Override
    public boolean canUse() {
        // Per-core gate: the original installs this goal only in setupGolem()'s core-3 case.
        if (!this.coreIs(3)) {
            return false;
        }
        if (this.delay < 0
                && this.golem.tickCount % TcConfig.getInt(TcConfigCatalog.GOLEM_DELAY) <= 0
                && this.golem.getNavigation().isDone()) {
            final BlockPos found = this.findGrownCrop();
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
                && (this.delay > 0 || !this.golem.getNavigation().isDone());
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
                // DEVIATION: 1.7.10 `block.getBlockHardness(world,x,y,z)`
                // (AIHarvestCrops.java:90) is `BlockState.getDestroySpeed(level, pos)` in 1.21.1
                // (the same float hardness the break speed is derived from).
                this.delay = (int) Math.max(10.0F,
                        (20.0F - this.golem.getGolemStrength() * 2.0F) * this.block.getDestroySpeed(this.level(), pos));
                this.maxDelay = this.delay;
                this.mod = (int) (this.delay / Math.round(this.delay / 6.0F));
            }

            if (this.delay > 0) {
                if (--this.delay > 0 && this.delay % this.mod == 0 && this.golem.getNavigation().isDone()) {
                    this.golem.startActionTimer();
                    // DEVIATION: 1.7.10's raw sound-effect string of the block's material
                    // (AIHarvestCrops.java:100-107) becomes the state's SoundType step
                    // (dig) sound; the volume/pitch formula ((v + 0.7) / 8, p * 0.5) is kept.
                    final SoundType soundType = this.block.getSoundType();
                    this.level().playSound(null, this.xx + 0.5F, this.yy + 0.5F, this.zz + 0.5F,
                            soundType.getStepSound(), SoundSource.BLOCKS,
                            (soundType.getVolume() + 0.7F) / 8.0F, soundType.getPitch() * 0.5F);
                    // 1.7.10 destroyBlockPartially (S25PacketBlockBreakAnim) is
                    // ServerLevel#destroyBlockProgress in 1.21.1 — the same per-breaker
                    // crack overlay stage, and the -1 clear in stop().
                    this.level().destroyBlockProgress(this.golem.getId(), pos,
                            (int) (9.0F * (1.0F - (float) this.delay / this.maxDelay)));
                }

                if (this.delay == 0) {
                    this.harvest();
                    this.checkAdjacent();
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
     * The original's {@code harvest()} (AIHarvestCrops.java:184-278). See the
     * class javadoc for the drop/replant model deviation.
     */
    void harvest() {
        this.count = 200;
        final BlockPos pos = new BlockPos(this.xx, this.yy, this.zz);
        final BlockState target = this.level().getBlockState(pos);

        // SEAM: the original's clickable-crop branch (AIHarvestCrops.java:189-190) calls
        // `block.onBlockClicked` through a FakePlayer. The clickableCrops list is filled
        // only by IMC `harvestClickableCrop` messages (Thaumcraft.java:190) and 1.21.1's
        // block-click entry point is Player-typed; no registration exists in this tree,
        // so the branch has no target and the removal path below runs for every crop.
        final boolean clickable = false;
        if (clickable) {
            // No clickable crop is registered in this tree — see the SEAM above.
        } else {
            // DEVIATION: the original setBlockToAir'd the ripe crop (no drops,
            // AIHarvestCrops.java:192). The port's first attempt, destroyBlock
            // (pos, true, null), is a NO-OP on zero-explosion-resistance blocks
            // (ServerLevel.destroyBlock refuses them, and every crop has
            // resistance 0), so the port drops and removes explicitly:
            // Block.dropResources (loot table, no tool, no fortune) + an air
            // setBlock. Same observable result destroyBlock would give on a
            // resistance-bearing block.
            final BlockState harvestTarget = this.level().getBlockState(pos);
            final boolean broken = !harvestTarget.isAir();
            if (broken) {
                net.minecraft.world.level.block.Block.dropResources(harvestTarget, this.level(), pos,
                        this.level().getBlockEntity(pos));
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            // Replant: the original's metadata-0 reset (AIHarvestCrops.java:231, the pod
            // replant; seed items plant at age 0, AIHarvestCrops.java:240-258).
            if (broken && target.getBlock() instanceof CropBlock crop) {
                this.level().setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
            }
        }

        if (this.golem.getUpgradeAmount(4) > 0) {
            this.collectDropsAndReplant(pos, target.getBlock() instanceof BlockManaPod);
        }
        this.golem.startActionTimer();
    }

    /**
     * The original's upgrade-4 drop sweep (AIHarvestCrops.java:193-273): pull
     * every {@code EntityItem} within 6 of the golem, nudge fresh drops toward
     * it, then use the drop's stack: mana bean → mana pod, seeds → replant
     * around the harvest site.
     *
     * <p>DEVIATION (null player): the original runs each item use through a
     * FakePlayer; this tree's item-use plumbing exposes a {@code @Nullable
     * Player} (the vanilla dispenser shape), so the port drives
     * {@code useOn} with a null player. Vanilla seeds in this tree are plain
     * items whose planting is data-driven and {@code useOn}-inert, so vanilla
     * seed replanting is a no-op; the shipped {@code ItemManaBean#useOn}
     * handles the null player and plants.</p>
     *
     * @param harvestedPod whether the block just harvested was a mana pod
     *                     (the original's {@code func_149707_d} plantability check,
     *                     AIHarvestCrops.java:228, becomes the pod identity test)
     */
    private void collectDropsAndReplant(final BlockPos pos, final boolean harvestedPod) {
        final AABB area;
        final Vec3 p = this.golem.position();
        area = new AABB(p.x, p.y, p.z, p.x, p.y, p.z).inflate(6.0);
        final List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class, area);
        for (final ItemEntity item : drops) {
            ItemStack st = item.getItem();
            if (st.isEmpty()) {
                continue;
            }
            if (item.getAge() < 2) {
                // Original AIHarvestCrops.java:201-210: fresh drops get pulled toward
                // the golem — same velocity formula on 1.21.1's setDeltaMovement.
                final Vec3 v = item.position().subtract(this.golem.position()).normalize();
                item.setDeltaMovement(-v.x / 4.0, 0.075, -v.z / 4.0);
            }

            boolean done = false;
            if (st.is(CatalogResources.ITEM_MANA_BEAN.get())) {
                // Original AIHarvestCrops.java:227-235: the bean is used at (x, y+1, z)
                // face DOWN (the pod plants below the log); if the use fails, a pod at
                // meta 0 is set at the harvest site.
                if (harvestedPod) {
                    st.shrink(1);
                    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos.above()),
                            Direction.DOWN, pos.above(), false);
                    final InteractionResult used = st.getItem()
                            .useOn(new UseOnContext(this.level(), null, InteractionHand.MAIN_HAND, st, hit));
                    if (!used.consumesAction()) {
                        this.level().setBlock(pos, CatalogResources.BLOCK_MANA_POD.get().defaultBlockState(),
                                Block.UPDATE_ALL);
                    }
                }
                done = true;
            }
            // SEAM: the original's giant-mushroom-farm branch (AIHarvestCrops.java:216-226)
            // keys on the 1.7.10 red-mushroom-CAP block item (Items.field_151100_aR, meta 3)
            // landing on an upward-facing jungle log, then sets a pumpkin-stem-block at the
            // site. 1.21.1 has no cap block item, so the branch has no carrier in this tree.
            else {
                // Original AIHarvestCrops.java:237-258: use each plantable stack on the
                // nine columns one below the harvest site. DEVIATION: 1.7.10's
                // `instanceof IPlantable || ItemSeedFood` predicate has no interface in
                // this tree — the data-driven VILLAGER_PLANTABLE_SEEDS tag is the
                // plantable predicate, and planting runs through the item's own useOn
                // (vanilla seeds here plant via data, so their useOn is inert — SEAM).
                final int[] xm = {0, 0, 1, 1, -1, 0, -1, -1, 1};
                final int[] zm = {0, 1, 0, 1, 0, -1, -1, 1, -1};
                for (int i = 0; !st.isEmpty() && i < 9; i++) {
                    if (st.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                        final BlockPos below = pos.offset(xm[i], -1, zm[i]);
                        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below),
                                Direction.UP, below, false);
                        final InteractionResult used = st.getItem()
                                .useOn(new UseOnContext(this.level(), null, InteractionHand.MAIN_HAND, st, hit));
                        if (used.consumesAction()) {
                            st.shrink(1);
                        }
                    }
                }
            }

            if (st.isEmpty()) {
                item.discard();
            } else {
                item.setItem(st);
            }
            if (done) {
                break;
            }
        }
    }

    /**
     * The original's {@code checkAdjacent()} (AIHarvestCrops.java:121-147):
     * after a harvest, retarget the nearest grown crop within ±2 x/z and ±1 y,
     * still bounded by the golem's home distance.
     */
    private void checkAdjacent() {
        for (int x2 = -2; x2 <= 2; x2++) {
            for (int z2 = -2; z2 <= 2; z2++) {
                for (int y2 = -1; y2 <= 1; y2++) {
                    final int x = this.xx + x2;
                    final int y = this.yy + y2;
                    final int z = this.zz + z2;
                    final BlockPos home = this.golem.getRestrictCenter();
                    if (!(Math.abs(home.getX() - x) > this.distance)
                            && !(Math.abs(home.getY() - y) > this.distance)
                            && !(Math.abs(home.getZ() - z) > this.distance)
                            && isGrownCrop(this.level(), new BlockPos(x, y, z))) {
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
     * The original's {@code findGrownCrop()} (AIHarvestCrops.java:159-182): a
     * shuffled grid of columns {@code home.x ± d, home.z ± d} popped one cell
     * per attempt; each pop scans {@code home.y ± 3}.
     *
     * <p>DEVIATION: 1.21.1's {@code RandomSource} does not extend
     * {@code java.util.Random} as 1.7.10's {@code Random} did, so the shuffle
     * runs on a {@code java.util.Random} seeded from the golem's random
     * stream.</p>
     */
    private BlockPos findGrownCrop() {
        final RandomSource rand = this.golem.getRandom();
        if (this.checklist.isEmpty()) {
            final BlockPos home = this.golem.getRestrictCenter();
            for (int a = (int) -this.distance; a <= this.distance; a++) {
                for (int b = (int) -this.distance; b <= this.distance; b++) {
                    this.checklist.add(new BlockPos(home.getX() + a, 0, home.getZ() + b));
                }
            }
            Collections.shuffle(this.checklist, new Random(rand.nextLong()));
        }

        final BlockPos cell = this.checklist.get(0);
        this.checklist.remove(0);
        final BlockPos home = this.golem.getRestrictCenter();
        for (int y = home.getY() - 3; y <= home.getY() + 3; y++) {
            final BlockPos probe = new BlockPos(cell.getX(), y, cell.getZ());
            if (isGrownCrop(this.level(), probe)) {
                return probe;
            }
        }
        return null;
    }

    /**
     * The ported {@code CropUtils.isGrownCrop} over the original's hard-wired
     * list (class javadoc). Blocks the original excludes (melon/pumpkin stems,
     * {@code CropUtils.java:91}) stay excluded.
     */
    static boolean isGrownCrop(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        // The original's BlockStem exclusion (CropUtils.java:91): melon/pumpkin stems
        // are not crops. The original's separate fully-grown-stem acceptance
        // (CropUtils.java:93, the pumpkin-stem meta slice) is dropped per the card.
        if (state.is(Blocks.MELON_STEM) || state.is(Blocks.PUMPKIN_STEM)) {
            return false;
        }
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.is(Blocks.NETHER_WART)) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        // Stacked crops (cactus, sugar cane — Config.java:874-875): a stacked block
        // counts only above its bottom block (CropUtils.java:99, `biB == bi`).
        // DEVIATION: the 1.7.10 registration covers every meta of the block; 1.21.1
        // cactus/cane carry an age property, and the port accepts every non-base
        // block of the stack, exactly the registration's all-meta scope.
        return (state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE))
                && level.getBlockState(pos.below()).is(state.getBlock());
    }
}
