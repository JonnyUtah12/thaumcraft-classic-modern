package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockManaPod;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original's {@code TileManaPod} (content-mapping.csv row
 * {@code thaumcraft:tilemanapod}, TC-M6-04a): stores the single aspect the
 * pod was seeded with (bean planting, TC-M6-04a) and runs the growth logic
 * the original's {@code checkGrowth} performed from the block's random tick
 * (see {@link BlockManaPod#randomTick}).
 *
 * <p>Original semantics ported 1:1:</p>
 * <ul>
 *   <li>age advance: the original incremented the block damage value (max 7);
 *       here the {@code age} block property (the 1.21.1 form of the same
 *       state, per the block-row ownership rule);</li>
 *   <li>aspect seeding at age 3: the pod reads its own aspect plus the four
 *       horizontal neighbours' aspects; with more than one distinct aspect it
 *       picks randomly from every aspect plus every pairwise combination
 *       (each combination weighted twice, original outlist shape); otherwise
 *       it takes the most common neighbour aspect;</li>
 *   <li>fallback seed for any grown pod (age &gt; 2) without an aspect: 1/8
 *       chance PLANT (herba), else a random primal;</li>
 *   <li>NBT: the original's {@code "aspect"}` string tag, same key.</li>
 * </ul>
 *
 * <p>The original's {@code IAspectContainer} view (getAspects non-null only
 * at age 7) is not implemented: no TC-M4 consumer exists yet, and the
 * interface arrives with its owner. The bean-drop path mirrors
 * {@link #getAspect()} onto the entity as the {@code thaumcraft:essentia_container}
 * data component, which the block loot table copies onto the dropped bean.</p>
 */
public class TileManaPod extends BlockEntity {

    /** Original public field {@code aspect}; null until seeded. */
    private Aspect aspect;

    public TileManaPod(final BlockPos pos, final BlockState state) {
        super(CatalogResources.MANA_POD.get(), pos, state);
    }

    public Aspect getAspect() {
        return aspect;
    }

    /** Original direct field write + {@code markDirty()}. */
    public void setAspect(final Aspect aspect) {
        this.aspect = aspect;
        syncAspectComponent();
        setChanged();
    }

    /**
     * Mirror the pod's aspect as the typed {@code thaumcraft:essentia_container}
     * data component on the entity: {@code minecraft:copy_components
     * source=block_entity} in the block loot table reads
     * {@link BlockEntity#collectComponents()} to seed the dropped bean — the
     * 1.21.1 replacement for the original's transient position→aspect map
     * read from {@code BlockManaPod.getDrops}. A pod without an aspect carries
     * no component, leaving the loot table's herba default in place.
     */
    private void syncAspectComponent() {
        if (this.aspect != null) {
            setComponents(DataComponentMap.builder()
                    .set(ModDataComponents.ESSENTIA_CONTAINER.get(),
                            EssentiaContainer.of(this.aspect, 1)).build());
        } else {
            setComponents(DataComponentMap.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (aspect != null) {
            tag.putString("aspect", aspect.getTag());
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Original readCustomNBT: Aspect.getAspect(name) is null for unknown tags.
        aspect = Aspect.getAspect(tag.getString("aspect"));
        syncAspectComponent();
    }

    /**
     * Original {@code checkGrowth}: advance the age, then seed the aspect
     * once the pod is mature enough (age &gt; 2), mixing neighbour aspects
     * exactly once at age 3.
     */
    public void checkGrowth() {
        if (level == null) {
            return;
        }
        final BlockState state = level.getBlockState(worldPosition);
        int age = state.getValue(BlockManaPod.AGE);
        if (age < 7) {
            age++;
            level.setBlock(worldPosition, state.setValue(BlockManaPod.AGE, age), 3);
        }
        if (age > 2) {
            if (age == 3) {
                final AspectList al = new AspectList();
                if (aspect != null) {
                    al.add(aspect, 1);
                }
                // Original ForgeDirection loop d=2..5: the four horizontal sides.
                for (final Direction dir : List.of(Direction.NORTH, Direction.SOUTH,
                        Direction.WEST, Direction.EAST)) {
                    if (level.getBlockEntity(worldPosition.relative(dir)) instanceof final TileManaPod neighbour
                            && neighbour.aspect != null) {
                        al.add(neighbour.aspect, 1);
                    }
                }
                if (al.size() > 1) {
                    final List<Aspect> aa = al.getAspects();
                    final List<Aspect> outlist = new ArrayList<>();
                    for (int i = 0; i < aa.size(); i++) {
                        outlist.add(aa.get(i));
                        for (int j = 0; j < aa.size(); j++) {
                            if (i != j) {
                                final Aspect combo = AspectCombinations.getCombinationResult(aa.get(i), aa.get(j));
                                if (combo != null) {
                                    outlist.add(combo);
                                    outlist.add(combo);
                                }
                            }
                        }
                    }
                    if (!outlist.isEmpty()) {
                        setAspect(outlist.get(level.getRandom().nextInt(outlist.size())));
                    }
                }
                if (al.size() >= 1 && aspect == null) {
                    setAspect(al.getAspectsSortedAmount().get(0));
                }
            }
            if (aspect == null) {
                if (level.getRandom().nextInt(8) == 0) {
                    setAspect(Aspect.getAspect("herba")); // original Aspect.PLANT
                } else {
                    final List<Aspect> primals = Aspect.getPrimalAspects();
                    setAspect(primals.get(level.getRandom().nextInt(primals.size())));
                }
            }
        }
    }
}
