package com.jonnyutah12.thaumcraftclassic.potions;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Thaumarhia (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionThaumarhia.java}
 * (Thaumcraft:potion.thaumarhia, color 6702199, Config.java line 345).
 * Every 20 ticks, with 1/15 chance, the air block at the target's position
 * is replaced with flux goo ({@code ConfigBlocks.blockFluxGoo}). The goo
 * block is TC-M5-01 content; the lookup is registry-guarded so the effect is
 * inert until that card lands, mirroring the original's unconditional
 * reference to an always-present block.</p>
 */
public class PotionThaumarhia extends MobEffect {

    /** content-mapping.csv new_id of Thaumcraft:blockFluxGoo. */
    static final ResourceLocation FLUX_GOO_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockfluxgoo");

    public PotionThaumarhia() {
        super(MobEffectCategory.HARMFUL, 6702199);
    }

    /** Original tick gate: every 20 ticks. */
    public static boolean appliesThisTick(final int durationLeft, final int amplifier) {
        return durationLeft % 20 == 0;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int durationLeft, final int amplifier) {
        return appliesThisTick(durationLeft, amplifier);
    }

    @Override
    public boolean applyEffectTick(final LivingEntity target, final int amplifier) {
        if (target.level().random.nextInt(15) == 0) {
            final BlockPos pos = BlockPos.containing(target.getX(), target.getY(), target.getZ());
            if (target.level().isEmptyBlock(pos)) {
                final Block goo = BuiltInRegistries.BLOCK.get(FLUX_GOO_ID);
                if (goo != Blocks.AIR) {
                    target.level().setBlock(pos, goo.defaultBlockState(), 3);
                }
            }
        }
        return true;
    }
}
