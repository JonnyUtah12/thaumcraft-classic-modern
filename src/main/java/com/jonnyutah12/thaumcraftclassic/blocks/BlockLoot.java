package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.loot.LootRoller;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Loot container block (content-mapping.csv {@code thaumcraft:blocklooturn},
 * {@code thaumcraft:blocklootcrate}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockLoot}. One block id whose
 * metadata 0/1/2 selected the loot rarity (common/uncommon/rare); breaking it
 * drops {@code 1 + meta + rand(3)} rolls from the matching bag table via
 * {@code Utils.generateLoot(meta, rand)} — ported through
 * {@link LootRoller#generateLoot}. Silk touch keeps the block itself (the
 * original's canSilkHarvest default path), preserving the variant. Fortune is
 * ignored, as in the original.</p>
 *
 * <p>1.21.1 has no block metadata, so the rarity moves to a {@code variant}
 * property with the same 0-2 values (D-001). Physical properties are the
 * original's: hardness 0.15, resistance 0.0. The urn was clay material with a
 * custom "urnbreak" step sound that 1.7.10 resolved through the vanilla
 * unnamespaced lookup and never found — the surface was silent
 * (docs/sound-events.md) — so {@link SoundType#EMPTY} preserves the original
 * audible behavior; the crate used vanilla wood sounds verbatim.</p>
 *
 * <p>The original drew non-cube hitboxes through its custom block renderers:
 * urn from (2/16, 1/16, 2/16) to (14/16, 13/16, 14/16), crate from
 * (1/16, 0, 1/16) to (15/16, 14/16, 15/16) (BlockRenderer W-constants).
 * Those exact boxes are {@link #SHAPE_URN} / {@link #SHAPE_CRATE}. The
 * renderer/model shapes themselves are TC-M7-01's model-conversion scope.</p>
 */
public class BlockLoot extends Block {

    /** Original blockLoot metadata, verbatim 0-2: 0 common, 1 uncommon, 2 rare. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public static final int RARITY_COMMON = 0;
    public static final int RARITY_UNCOMMON = 1;
    public static final int RARITY_RARE = 2;

    /** Original BlockRenderer W-constants: W1=1/16, W2=2/16, W13=13/16, W14=14/16, W15=15/16. */
    public static final VoxelShape SHAPE_URN = box(2 / 16.0, 1 / 16.0, 2 / 16.0, 14 / 16.0, 13 / 16.0, 14 / 16.0);
    public static final VoxelShape SHAPE_CRATE = box(1 / 16.0, 0.0, 1 / 16.0, 15 / 16.0, 14 / 16.0, 15 / 16.0);

    /** Which original block this instance is (drives hitbox + sounds). */
    public enum Kind {
        URN(SHAPE_URN, SoundType.EMPTY),
        CRATE(SHAPE_CRATE, SoundType.WOOD);

        final VoxelShape shape;
        final SoundType sound;

        Kind(final VoxelShape shape, final SoundType sound) {
            this.shape = shape;
            this.sound = sound;
        }
    }

    private final Kind kind;

    public BlockLoot(final Kind kind) {
        super(BlockBehaviour.Properties.of()
                .mapColor(kind == Kind.URN ? MapColor.STONE : MapColor.WOOD)
                .strength(0.15F, 0.0F)
                .sound(kind.sound));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(VARIANT, RARITY_COMMON));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return kind.shape;
    }

    /**
     * Original getDrops: {@code q = 1 + meta + rand(3)} rolls of
     * {@code Utils.generateLoot(meta, rand)}; silk touch instead drops the
     * block itself with its variant preserved (the original's canSilkHarvest
     * path kept the metadata on the block item).
     */
    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final LootParams lootParams =
                params.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        final ItemStack tool = lootParams.hasParam(LootContextParams.TOOL)
                ? lootParams.getParamOrNull(LootContextParams.TOOL)
                : ItemStack.EMPTY;
        if (EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.core.Holder.direct(
                        lootParams.getLevel().registryAccess()
                                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)),
                tool) > 0) {
            final ItemStack self = new ItemStack(this);
            self.set(ModDataComponents.ITEM_META.get(), state.getValue(VARIANT));
            return List.of(self);
        }

        final int rarity = state.getValue(VARIANT);
        final RandomSource rand = lootParams.getLevel().getRandom();
        final List<ItemStack> drops = new ArrayList<>();
        final int count = 1 + rarity + rand.nextInt(3);
        for (int i = 0; i < count; i++) {
            final ItemStack roll = LootRoller.generateLoot(rarity, rand);
            if (!roll.isEmpty()) {
                drops.add(roll.copy());
            }
        }
        return drops;
    }

    /** Original pick-block kept the metadata; the variant rides on ITEM_META. */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final net.minecraft.world.phys.HitResult target,
                                       final net.minecraft.world.level.LevelReader level, final BlockPos pos,
                                       final net.minecraft.world.entity.player.Player player) {
        final ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        stack.set(ModDataComponents.ITEM_META.get(), state.getValue(VARIANT));
        return stack;
    }

    /** Convenience for creative tabs / tests: a stack for one rarity variant. */
    public static ItemStack withRarity(final Block block, final int rarity) {
        final ItemStack stack = new ItemStack(block);
        stack.set(ModDataComponents.ITEM_META.get(), rarity);
        return stack;
    }
}
