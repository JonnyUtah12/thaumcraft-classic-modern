package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/**
 * Stone device (content-mapping.csv {@code thaumcraft:blockstonedevice}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockStoneDevice} — one block
 * id, metadata 0-14 selecting the sub-kind. Physical properties are the
 * original's: rock material, hardness 3.0, resistance 25.0, stone step sound,
 * full cube. The meta-to-tile map is the binding one from
 * docs/catalog-triage.md; only meta 0 (basic alchemical furnace) is owned by
 * TC-M6-04c, the rest are supplied by their feature cards through
 * {@link DeviceTileFactory}.</p>
 *
 * <p>Deferred light: the original lit meta 0 to 12 while the alchemical
 * furnace was burning (dynamic, TE-driven — no block-state equivalent in
 * 1.21.1, noted in the TC-M6-04c handoff). Static light covers meta 2
 * (runic matrix, 10).</p>
 */
public class BlockStoneDevice extends BlockDeviceBase {

    public static final int MAX_VARIANT = 14;

    /** Meta 0: basic alchemical furnace (this card's TileAlchemyFurnace). */
    public static final int VARIANT_ALCHEMY_FURNACE = 0;
    public static final int VARIANT_RUNIC_MATRIX = 2;
    /** The original drops the cosmetic-solid block for the two plain pillars. */
    public static final int VARIANT_PILLAR_A = 3;
    public static final int VARIANT_PILLAR_B = 4;

    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    public BlockStoneDevice() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0F, 25.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(VARIANT) == VARIANT_RUNIC_MATRIX ? 10 : 0));
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * Original {@code getItemDropped}: metas 3 and 4 (infusion pillars) drop
     * the cosmetic-solid block instead of themselves. blockCosmeticSolid is
     * owned by TC-M6-04b; the lookup is by id so the drop starts working the
     * moment that block registers, with no compile dependency between cards.
     */
    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(final BlockState state,
            final net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        final int variant = variant(state);
        if (variant == VARIANT_PILLAR_A || variant == VARIANT_PILLAR_B) {
            final Item cosmetic = BuiltInRegistries.ITEM
                    .get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blockcosmeticsolid"));
            if (cosmetic != Items.AIR) {
                return java.util.List.of(new net.minecraft.world.item.ItemStack(cosmetic));
            }
        }
        return super.getDrops(state, builder);
    }
}
