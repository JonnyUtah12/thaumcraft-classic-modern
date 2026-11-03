package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * <h2>The burning swap is a published state (BD-18 slice 2b / t_45b89dd2, D-020)</h2>
 *
 * <p>The original recomputed the alchemical furnace's look from its tile per
 * frame: {@code getConnectedIcon} ({@code reference/decompiled/thaumcraft/
 * thaumcraft/common/blocks/BlockStoneDevice.java:153-177}) swaps the whole
 * side ring from {@code iconFurnace[2] = al_furnace_front_off} to
 * {@code iconFurnace[3] = al_furnace_front_on} while
 * {@code TileAlchemyFurnace.isBurning()} ({@code :165-171}; icons registered
 * at {@code :76-77}, and the plain {@code getIcon} side answer is front_off
 * at {@code :101-103}), the same flag lights the block — {@code getLightValue}
 * returns 12 for meta 0 with a burning tile ({@code :216-220}) while the
 * runic matrix keeps its static 10 ({@code :221-223}) — and
 * {@code randomDisplayTick} ({@code :194-212}) spawns smoke + flame at four
 * rim positions around the top while burning.</p>
 *
 * <p>A static blockstate cannot recompute anything, so the port publishes the
 * flag as {@link #BURNING} and names a model per value:
 * {@code blockstonedevice_v0_lit} is the one that names front_on on the side
 * ring; every other stone device variant names the same model for both
 * values, exactly as the original swaps on meta 0 only ({@code :155}; metas
 * 1/5/8/12 delegate to the super answer at {@code :172-174}). The
 * {@code BlockStateCoverageGameTest.dynamicAppearanceShipsAsDistinctStateModels}
 * gate enforces the swap, {@link DeviceDynamicState} is the publish contract,
 * and {@code DynamicBlockAppearanceGameTest.burningFurnaceFacesFollowTheBurningState}
 * pins the light and the unlit default.</p>
 *
 * <p>Two deltas, recorded not faked:</p>
 * <ul>
 * <li>The furnace's tile — the publisher of this flag — is content-mapping row
 * {@code thaumcraft:tilealchemyfurnace} (not_started). Nothing writes
 * {@code burning=true} until that tile (or the machine slice, t_fd5265f3)
 * lands; the shipped contract is the state and its model, exactly as the
 * levitator's glow and the lamp's charge shipped theirs.</li>
 * <li>The top face does <em>not</em> swap: the original's top answer is a
 * separate vis-driven branch ({@code :157-163} answers
 * {@code al_furnace_top_filled} while {@code TileAlchemyFurnace.vis > 0}) —
 * the burning model keeps {@code al_furnace_top} ({@code :75}, {@code :98})
 * and the top_filled wiring ships with the tile.</li>
 * </ul>
 */
public class BlockStoneDevice extends BlockDeviceBase implements DeviceDynamicState {

    public static final int MAX_VARIANT = 14;

    /** Meta 0: basic alchemical furnace (this card's TileAlchemyFurnace). */
    public static final int VARIANT_ALCHEMY_FURNACE = 0;
    public static final int VARIANT_RUNIC_MATRIX = 2;
    /** The original drops the cosmetic-solid block for the two plain pillars. */
    public static final int VARIANT_PILLAR_A = 3;
    public static final int VARIANT_PILLAR_B = 4;

    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    /**
     * The alchemical furnace's fire — the published twin of the original's
     * {@code TileAlchemyFurnace.isBurning()} check at
     * {@code BlockStoneDevice.java:165-171} (side ring) and {@code :216-220}
     * (light 12). The publisher is the furnace tile, content-mapping row
     * {@code thaumcraft:tilealchemyfurnace} (not_started); the machine card
     * publishes through {@link DeviceDynamicState#sync}.
     */
    public static final BooleanProperty BURNING = BooleanProperty.create("burning");

    public BlockStoneDevice() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0F, 25.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> switch (state.getValue(VARIANT)) {
                    // meta 2 lit a static 10 (BlockStoneDevice.java:221-223);
                    // meta 0 lit 12 only while burning and 0 otherwise (:214-226).
                    case VARIANT_RUNIC_MATRIX -> 10;
                    case VARIANT_ALCHEMY_FURNACE -> state.getValue(BURNING) ? 12 : 0;
                    default -> 0;
                }));
        // burning=false is the shipped default — pinned explicitly: 1.21.1
        // orders a BooleanProperty true-first, and any() would light every
        // freshly placed furnace (the trap DeviceDynamicState's javadoc names).
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0).setValue(BURNING, false));
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, BURNING);
    }

    @Override
    public BooleanProperty dynamicAppearanceProperty() {
        return BURNING;
    }

    /**
     * The original's {@code randomDisplayTick} ({@code BlockStoneDevice.java:194-212}):
     * a burning furnace spawns smoke and flame at the four rim positions around
     * its top — {@code f1 = y + 0.2 + r*5/16}, {@code f3 = 0.52},
     * {@code f4 = r*0.5 - 0.25}. The tile the original read is not shipped
     * (content-mapping {@code thaumcraft:tilealchemyfurnace}, not_started), so
     * the published {@link #BURNING} state gates the box, per D-020.
     */
    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (variant(state) != VARIANT_ALCHEMY_FURNACE || !state.getValue(BURNING)) {
            return;
        }
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.2 + random.nextFloat() * 5.0 / 16.0;
        final double z = pos.getZ() + 0.5;
        final double side = 0.52;
        final double jitter = random.nextFloat() * 0.5 - 0.25;
        level.addParticle(ParticleTypes.SMOKE, x - side, y, z + jitter, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x - side, y, z + jitter, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x + side, y, z + jitter, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + side, y, z + jitter, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x + jitter, y, z - side, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + jitter, y, z - side, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x + jitter, y, z + side, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + jitter, y, z + side, 0.0, 0.0, 0.0);
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
