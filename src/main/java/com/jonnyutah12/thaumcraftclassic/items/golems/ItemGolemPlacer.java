package com.jonnyutah12.thaumcraftclassic.items.golems;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Golem placer (content-mapping.csv {@code thaumcraft:itemgolemplacer}) —
 * the original's {@code thaumcraft.common.entities.golems.ItemGolemPlacer}.
 *
 * <p>One damage value per material: the original's {@code iconGolem[8]} array
 * is indexed by the stack's metadata and {@code func_77667_c} names the stack
 * {@code <item>.<damageValue>}, so meta 0..7 is the {@link EnumGolemType}
 * ordinal. The port keeps the metadata on the shared
 * {@code thaumcraft:item_meta} component (D-001; the shipped crucible recipe
 * rows already write it, e.g. {@code recipe.32.crucible} for meta 0 =
 * STRAW).</p>
 *
 * <p><b>Construction path</b> — original {@code onItemUseFirst} +
 * {@code spawnCreature}, transcribed:</p>
 * <ol>
 *   <li>server side only, and only when the player is <em>not</em> sneaking
 *       ({@code !world.isRemote && !player.isSneaking()});</li>
 *   <li>the spawn position is the clicked block plus the clicked face's normal,
 *       plus {@code 0.5} when the clicked block is lava
 *       ({@code if (side == 1 && var11 == Blocks.lava || var11 ==
 *       Blocks.flowing_lava) var12 = 0.5} — the {@code ||} makes the face check
 *       redundant, and 1.21.1 has a single lava block, mapped the same way by
 *       {@code ItemTrunkSpawner}, TC-M6-04e);</li>
 *   <li>{@code new EntityGolemBase(world, EnumGolemType.getType(meta), adv)}
 *       where {@code adv} is the stack's {@code advanced} tag — the infusion
 *       recipe {@code recipe.67.infusion} writes it through the altar's
 *       {@code output_tag}, which lands in 1.21.1's
 *       {@code minecraft:custom_data} component;</li>
 *   <li>random yaw, {@code func_110171_b(x, y, z, 32)} home area
 *       ({@code Mob.restrictTo}), the stored core / upgrades / decoration
 *       transferred, {@code setup(side)} (the attribute half is
 *       {@link EntityGolemBase#applyMaterialTraits()}; goals and inventory are
 *       TC-M6-08-s-b), spawn, owner, custom name, and the stack consumed
 *       outside creative.</li>
 * </ol>
 *
 * <p><b>Deferred, with their cards:</b> the marker list transfer
 * ({@code golem.setMarkers(ItemGolemBell.getMarkers(stack))}) and the stored
 * inventory ({@code golem.inventory.readFromNBT}) both need the
 * {@code Marker} / {@code InventoryMob} infrastructure and stay on
 * TC-M6-08-s-b; the "core"/"adv"/"upgrades"/"marked locations"/decoration
 * tooltip block is the original's {@code addInformation} and is deferred with
 * the same card (the port ships the per-meta names and the upgrade tooltip
 * keys it needs).</p>
 */
public class ItemGolemPlacer extends Item {

    /** The original's eight damage values — {@code iconGolem[8]}, one per {@link EnumGolemType}. */
    public static final int META_COUNT = 8;

    /**
     * The original's wildcard metadata: the ADVANCEDGOLEM research entry's icon
     * is {@code new ItemStack(itemGolemPlacer, 1, 32767)} ({@code
     * ConfigResearch.java}) and {@code ResearchManager.META_ANY} is the same
     * figure. Not a placeable variant — {@link EnumGolemType#getType} returns
     * null for it, exactly as the original does.
     */
    public static final int META_ANY = 32767;

    public ItemGolemPlacer(final Properties properties) {
        super(properties);
    }

    /** Original {@code func_77667_c(ItemStack)}: {@code <item>.<meta>} — one name per material. */
    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    /** The stack's original metadata (the port's {@code item_meta} component; default 0). */
    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * The material a stack places: original
     * {@code EnumGolemType.getType(stack.getMetadata())} — {@code null} for a
     * metadata outside 0..7 (including the 32767 wildcard), which is why the
     * original's {@code spawnCreature} could build nothing.
     */
    public static EnumGolemType materialOf(final ItemStack stack) {
        return EnumGolemType.getType(storedMeta(stack));
    }

    /**
     * The original's {@code stack.getTagCompound().hasKey("advanced")} test.
     * In 1.21.1 the infusion altar stores that tag pair in the
     * {@code minecraft:custom_data} component
     * ({@code TileInfusionMatrix.finishCraft}).
     */
    public static boolean isAdvanced(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains("advanced");
    }

    /** The stack's custom-data tag, or null when the stack carries none. */
    public static CustomData customData(final ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            // Original: the placement block runs only when the player is not
            // sneaking; otherwise the click belongs to the block.
            return InteractionResult.PASS;
        }
        final Level level = context.getLevel();
        if (level.isClientSide) {
            // Original gate: !world.isRemote. The server owns the spawn.
            return InteractionResult.SUCCESS;
        }
        final ItemStack stack = context.getItemInHand();
        if (materialOf(stack) == null) {
            // Original: EnumGolemType.getType(meta) is null, so no entity is built.
            return InteractionResult.PASS;
        }
        final BlockState clickedState = level.getBlockState(context.getClickedPos());
        final BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        // Original: `if (side == 1 && block == lava || block == flowing_lava) y += 0.5`.
        final double yLift = clickedState.is(Blocks.LAVA) ? 0.5 : 0.0;

        if (spawnGolem(level, target.getX() + 0.5, target.getY() + yLift, target.getZ() + 0.5,
                context.getClickedFace().get3DDataValue(), stack, player)
                && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Original {@code spawnCreature(World, double, double, double, int, ItemStack,
     * EntityPlayer)}. Returns false when the metadata names no material.
     */
    public static boolean spawnGolem(final Level level, final double x, final double y, final double z,
                                     final int side, final ItemStack stack, final Player player) {
        final EnumGolemType material = materialOf(stack);
        if (material == null) {
            return false;
        }
        final EntityGolemBase golem = new EntityGolemBase(CatalogGolems.GOLEM.get(), level);
        golem.setMaterial(material);
        golem.setAdvanced(isAdvanced(stack));
        // Original func_70012_b(x, y, z, rand * 360.0F, 0.0F).
        golem.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        // Original func_110171_b(x, y, z, 32) — setHomeArea(x, y, z, 32).
        golem.restrictTo(BlockPos.containing(x, y, z), EntityGolemBase.HOME_RANGE);
        golem.setHomeFacing(side);

        final CustomData data = customData(stack);
        if (data != null) {
            if (data.contains("core")) {
                golem.setCore(data.copyTag().getByte("core"));
            }
            if (data.contains("upgrades")) {
                golem.setUpgrades(resizeUpgrades(golem.upgradeSlotCount(), data.copyTag().getByteArray("upgrades")));
            }
            if (data.contains("deco")) {
                golem.setDecoration(data.copyTag().getString("deco"));
            }
        }
        // Original golem.setup(side) -> setupGolem() + setupGolemInventory():
        // the attribute half is this card's, the goal/inventory halves are
        // TC-M6-08-s-b.
        golem.applyMaterialTraits();
        level.addFreshEntity(golem);
        golem.setOwnerName(player.getGameProfile().getName());
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            golem.setCustomName(stack.getHoverName());
            golem.setPersistenceRequired();
        }
        return true;
    }

    /**
     * The original's upgrade-array normalisation, verbatim: keep the golem's own
     * slot count ({@code golemType.upgrades + (advanced ? 1 : 0)}), pad with -1
     * and truncate the stack's array to it.
     */
    public static byte[] resizeUpgrades(final int slots, final byte[] fromStack) {
        if (fromStack.length == slots) {
            return fromStack;
        }
        final byte[] resized = new byte[slots];
        Arrays.fill(resized, (byte) -1);
        for (int a = 0; a < fromStack.length && a < slots; a++) {
            resized[a] = fromStack[a];
        }
        return resized;
    }
}
