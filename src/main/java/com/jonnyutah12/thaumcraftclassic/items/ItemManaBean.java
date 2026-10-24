package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockManaPod;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.tiles.TileManaPod;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original's {@code ItemManaBean} (content-mapping.csv row
 * {@code thaumcraft:itemmanabean}, TC-M6-04a). Food values are the
 * original's {@code super(1, 0.5F, true)} + always-edible + 10-tick eat
 * (values attached at registration).
 *
 * <p>Original behaviors ported here:</p>
 * <ul>
 *   <li>{@code onEaten}: a random potion effect — beneficial effects apply
 *       at amplifier 2 for 3 seconds, harmful ones at amplifier 0 for
 *       8-12 seconds (original {@code Potion.applyPotionEffect(p, 2, 3.0)}
 *       vs {@code new PotionEffect(p, 160 + rand(80), 0)}); then a 25%
 *       chance to add the bean's first aspect to the player's aspect pool
 *       (TC-M2-02 knowledge + TC-M2-03 sync).</li>
 *   <li>{@code onUpdate}/{@code onCraftedBy}: an unassigned bean gets a
 *       random aspect the moment it enters an inventory or is crafted.</li>
 *   <li>{@code onItemUse}: right-clicking the underside of a log in a
 *       magical biome plants a mana pod (age 0) below it, seeded with the
 *       bean's aspect. The original's BiomeDictionary MAGICAL check becomes
 *       the {@code thaumcraft:is_magical} biome tag (TC-M5-05 fills it
 *       with the magical forest); the log check uses the vanilla
 *       {@code minecraft:logs} tag, which the port's magical log will carry.</li>
 * </ul>
 */
public class ItemManaBean extends Item implements IEssentiaContainerItem {

    private static final int EAT_DURATION_TICKS = 10;

    public ItemManaBean(final Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(final ItemStack stack, final Level level, final LivingEntity livingEntity) {
        final ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (level.isClientSide) {
            return result;
        }
        final List<MobEffect> effects = BuiltInRegistries.MOB_EFFECT.getList();
        final MobEffect effect = effects.get(level.getRandom().nextInt(effects.size()));
        if (effect.isBeneficial()) {
            livingEntity.addEffect(new MobEffectInstance(effect, 60, 2));
        } else {
            livingEntity.addEffect(new MobEffectInstance(effect, 160 + level.getRandom().nextInt(80), 0));
        }
        if (level.getRandom().nextFloat() < 0.25F && livingEntity instanceof final ServerPlayer player) {
            final AspectList aspects = getAspects(stack);
            if (aspects != null && aspects.size() > 0) {
                AspectKnowledge.checkAndSyncAspectKnowledge(player, aspects.getAspects().get(0), 1);
            }
        }
        return result;
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity,
                              final int slotId, final boolean isSelected) {
        if (!level.isClientSide && getAspects(stack) == null) {
            assignRandomAspect(stack, level);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void onCraftedBy(final ItemStack stack, final Level level, final Player player) {
        if (getAspects(stack) == null) {
            assignRandomAspect(stack, level);
        }
    }

    private void assignRandomAspect(final ItemStack stack, final Level level) {
        final List<Aspect> all = Aspect.values();
        setAspects(stack, new AspectList().add(all.get(level.getRandom().nextInt(all.size())), 1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        // Original onItemUse: only the bottom face of a log, only in a
        // magical biome, pod planted in the air block below.
        if (context.getClickedFace() != Direction.DOWN) {
            return InteractionResult.PASS;
        }
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();
        final BlockPos target = clicked.below();
        final BlockState support = level.getBlockState(clicked);
        if (!support.is(BlockTags.LOGS) || !level.getBiome(target).is(BlockManaPod.MAGICAL_BIOMES)
                || !level.isEmptyBlock(target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(target, CatalogResources.BLOCK_MANA_POD.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(target) instanceof final TileManaPod pod) {
            final AspectList aspects = getAspects(context.getItemInUse());
            if (aspects != null && aspects.size() > 0) {
                pod.setAspect(aspects.getAspects().get(0));
            }
        }
        final Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabreak) {
            context.getItemInUse().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (final Aspect aspect : aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(aspect.getName() + " x" + aspects.getAmount(aspect)));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
