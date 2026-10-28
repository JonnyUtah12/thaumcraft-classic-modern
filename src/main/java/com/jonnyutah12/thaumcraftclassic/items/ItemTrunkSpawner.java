package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.TrunkHost;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Traveling Trunk spawner item (content-mapping.csv {@code thaumcraft:trunkspawner}).
 *
 * <p>Original: {@code thaumcraft.common.entities.golems.ItemTrunkSpawner} —
 * right-click a block: spawn an {@code EntityTravelingTrunk} on the clicked
 * face (offset by the face normal; +0.5 y when standing on a lava block's top
 * face), random yaw, owner set to the player, custom name copied from the
 * item, the stored golem-upgrade byte applied (which resizes the trunk
 * inventory) and the stored inventory restored, then the item is consumed
 * outside creative. The tooltip shows the stored upgrade's name (blue) and,
 * when an inventory is stored, "This trunk holds items".</p>
 *
 * <p>1.21.1 realization: the trunk entity row is TC-M6-06's; this item looks
 * the entity up by its contract id ({@code thaumcraft:travelingtrunk}) and
 * drives it through the {@link TrunkHost} seam. While the entity is not
 * registered the item does nothing — the same observable behaviour as the
 * original with the entity absent. The upgrade/inventory NBT move to the
 * {@code trunk_upgrade}/{@code trunk_inventory} data components. The original
 * played the trunk's living sound on spawn; that call is protected per-entity
 * and belongs to TC-M6-06's entity, so it is left to the entity itself.</p>
 */
public class ItemTrunkSpawner extends Item {

    /** Contract id of the trunk entity (CSV row thaumcraft:travelingtrunk, TC-M6-06). */
    public static final ResourceLocation TRUNK_ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "travelingtrunk");

    public ItemTrunkSpawner(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(TRUNK_ENTITY_ID);
        if (type == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(TRUNK_ENTITY_ID)) {
            // TC-M6-06 has not landed the trunk entity yet: nothing to spawn.
            return InteractionResult.PASS;
        }

        final ItemStack stack = context.getItemInHand();
        final BlockPos clicked = context.getClickedPos();
        final Direction face = context.getClickedFace();
        final BlockState clickedState = level.getBlockState(clicked);
        final BlockPos target = clicked.relative(face);

        // Original: standing on a lava block's top face lifts the trunk by 0.5.
        double yLift = 0.0;
        if (face == Direction.UP && !clickedState.isAir() && clickedState.is(Blocks.LAVA)) {
            yLift = 0.5;
        }

        final Entity trunk = type.create(level);
        if (!(trunk instanceof TrunkHost host)) {
            return InteractionResult.PASS;
        }
        trunk.setPos(target.getX(), target.getY() + yLift, target.getZ());
        trunk.setYRot(level.getRandom().nextFloat() * 360.0F);
        trunk.setXRot(0.0F);

        host.setTrunkOwner(player.getGameProfile().getName());
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            trunk.setCustomName(stack.getHoverName());
        }
        final Byte upgrade = stack.get(CatalogTrinketsLoot.TRUNK_UPGRADE.get());
        if (upgrade != null) {
            host.setTrunkUpgrade(upgrade);
        }
        final ItemContainerContents contents = stack.get(CatalogTrinketsLoot.TRUNK_INVENTORY.get());
        if (contents != null) {
            host.setTrunkStoredInventory(contents);
        }
        if (trunk instanceof Mob mob) {
            mob.finalizeSpawn((net.minecraft.server.level.ServerLevel) level,
                    level.getCurrentDifficultyAt(target), MobSpawnType.MOB_SUMMONED, null);
        }
        level.addFreshEntity(trunk);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Original addInformation: blue upgrade name line (the original printed a
     * bare blue line when the stored byte was negative), plus the stored-
     * inventory hint. The upgrade name keys are the golem-upgrade item's
     * per-meta name keys (that item is TC-M6-06; the key shape follows the
     * port's per-meta naming, same as ItemResource).
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final Byte upgrade = stack.get(CatalogTrinketsLoot.TRUNK_UPGRADE.get());
        if (upgrade != null) {
            if (upgrade > -1) {
                tooltip.add(Component.translatable("item.thaumcraft.itemgolemupgrade." + upgrade)
                        .withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.literal("").withStyle(ChatFormatting.BLUE));
            }
        }
        if (stack.has(CatalogTrinketsLoot.TRUNK_INVENTORY.get())) {
            tooltip.add(Component.translatable("item.thaumcraft.trunkspawner.tooltip"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
