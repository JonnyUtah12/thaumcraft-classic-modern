package com.jonnyutah12.thaumcraftclassic.entities.ai;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Port of {@code thaumcraft.common.entities.ai.misc.AIConvertGrass}
 * (TC-M6-06-s-b, {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * entities/ai/misc/AIConvertGrass.java}). The sheep's goal: with a 1-in-250
 * roll (line 25) and grass underfoot (fern at the feet with the original's
 * {@code tallgrass} meta 1 gate, lines 31-33, mapping to 1.21.1's {@code
 * Blocks.FERN}; a grass block one below, line 33), it counts {@code 40}
 * ticks (line 38) and converts on tick {@code 4} (line 57): the foot cell
 * becomes fibrous taint meta 0 with setBlock flags 3 (lines 64/69), the
 * biome becomes Taint Land (lines 65/70) and the sheep heals to full
 * (lines 66/71, 1.7.10 {@code healFully()}).
 *
 * <p>Deviations noted: the original's entity status broadcast
 * {@code func_72960_a(entity, (byte)10)} (start, line 39 — the client-side
 * "infection" animation trigger) and the paired ghost-record event
 * {@code func_72926_e(2001, ...)} (lines 62/68, an invalid record id that
 * played nothing audible) have no 1.21.1 counterpart wired here; the
 * animation ships with TC-M7-03-s renderers.</p>
 */
public class ConvertGrassGoal extends Goal {

    private final Mob entity;
    private int counter;

    public ConvertGrassGoal(final Mob entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP)); // func_75248_a(7)
    }

    @Override
    public boolean canUse() {
        if (this.entity.getRandom().nextInt(250) != 0) { // line 25
            return false;
        }
        final int x = Mth.floor(this.entity.getX());
        final int y = Mth.floor(this.entity.getY());
        final int z = Mth.floor(this.entity.getZ());
        final BlockState feet = this.entity.level().getBlockState(new BlockPos(x, y, z));
        // 1.7.10: Blocks.tallgrass with metadata == 1 (the fern variant)
        if (isFernAtFeet(feet)) {
            return true;
        }
        return this.entity.level().getBlockState(new BlockPos(x, y - 1, z)).is(Blocks.GRASS_BLOCK);
    }

    static boolean isFernAtFeet(final BlockState state) {
        return state.is(Blocks.FERN);
    }

    @Override
    public void start() {
        this.counter = 40; // line 38
        final PathNavigation navigation = this.entity.getNavigation();
        if (navigation != null) {
            navigation.stop(); // func_75499_g
        }
    }

    @Override
    public void stop() {
        this.counter = 0; // line 44
    }

    @Override
    public boolean canContinueToUse() {
        return this.counter > 0; // line 48
    }

    public int ticksRemaining() {
        return this.counter;
    }

    @Override
    public void tick() {
        this.counter = Math.max(0, this.counter - 1); // line 56
        if (this.counter != 4) { // line 57 — conversion fires at 4
            return;
        }
        final int x = Mth.floor(this.entity.getX());
        final int y = Mth.floor(this.entity.getY());
        final int z = Mth.floor(this.entity.getZ());
        final BlockPos feet = new BlockPos(x, y, z);
        final BlockState feetState = this.entity.level().getBlockState(feet);
        final BlockState below = this.entity.level().getBlockState(feet.below());
        // line 61 tests the block ONLY (any tallgrass meta — grass, fern,
        // shrub), unlike canUse's meta-1 gate: map to fern | short grass
        final boolean atTallgrass = feetState.is(Blocks.FERN) || feetState.is(Blocks.SHORT_GRASS);
        final boolean grassBelow = below.is(Blocks.GRASS_BLOCK);
        if (!atTallgrass && !grassBelow) {
            return;
        }
        if (!(this.entity.level() instanceof ServerLevel level)) {
            return;
        }
        // fern branch (lines 63-66) / grass-below branch (lines 68-71): foot
        // cell -> fibres meta 0 (flags 3), biome -> taint, entity heals to
        // full; both branches write at the FEET cell (lines 64 and 69)
        level.setBlock(feet, CatalogTaint.BLOCK_TAINT_FIBRES.get().defaultBlockState()
                .setValue(BlockTaintFibres.VARIANT, 0), 3);
        TaintBiomes.setBiomeAtAndSync(level, x, z, TaintBiomes.taintBiomeHolder(level));
        this.entity.setHealth(this.entity.getMaxHealth());
    }
}
