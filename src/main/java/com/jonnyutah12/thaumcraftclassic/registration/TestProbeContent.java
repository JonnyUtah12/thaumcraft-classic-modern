package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Test-only probe block + item for the registration/id-stability harness
 * (TC-M2-01). They prove the real {@link ModBlocks}/{@link ModItems} holders
 * produce registered objects and that those objects round-trip through save
 * data (block state NBT, item stack NBT).
 *
 * <p>Registered only when {@code -Dthaumcraft.testprobe=true} is set, which
 * the {@code gameTestServer} run configuration does (build.gradle). The
 * shipped mod therefore contains exactly the ids in
 * docs/content-mapping.csv — test scaffolding never enters the frozen id
 * contract.</p>
 */
public final class TestProbeContent {

    public static final DeferredBlock<Block> PROBE_BLOCK = ModBlocks.BLOCKS.register("test_probe_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 3.0F)));

    public static final DeferredItem<Item> PROBE_ITEM = ModItems.ITEMS.register("test_probe_item",
            () -> new Item(new Item.Properties()));

    /** Extra distinct probe items for the TC-M4-04 altar gametest (center /
     * component / output must be distinguishable stacks). */
    public static final DeferredItem<Item> PROBE_COMPONENT = ModItems.ITEMS.register("test_probe_component",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PROBE_OUTPUT = ModItems.ITEMS.register("test_probe_output",
            () -> new Item(new Item.Properties()));

    /** Essentia source for the TC-M4-04 altar gametest: a block entity that
     * implements {@link IAspectSource} so {@code EssentiaHandler} can find and
     * drain it exactly like an IAspectSource tile (jar/tile). */
    public static final DeferredBlock<ProbeEssentiaSourceBlock> PROBE_ESSENTIA_SOURCE =
            ModBlocks.BLOCKS.register("test_probe_essentia_source", ProbeEssentiaSourceBlock::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeEssentiaSource>> PROBE_ESSENTIA_SOURCE_BE =
            ModBlockEntities.BLOCK_ENTITY_TYPES.register("test_probe_essentia_source",
                    () -> BlockEntityType.Builder.of(ProbeEssentiaSource::new, PROBE_ESSENTIA_SOURCE.get()).build(null));

    /** Forces class initialization (entry registration) from {@link ModRegistration}. */
    static void init() {
    }

    private TestProbeContent() {
    }

    public static class ProbeEssentiaSourceBlock extends Block implements EntityBlock {
        public ProbeEssentiaSourceBlock() {
            super(BlockBehaviour.Properties.of().strength(3.0F, 3.0F));
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new ProbeEssentiaSource(pos, state);
        }
    }

    public static class ProbeEssentiaSource extends BlockEntity implements IAspectSource {
        private final Map<Aspect, Integer> pool = new HashMap<>();

        public ProbeEssentiaSource(final BlockPos pos, final BlockState state) {
            super(PROBE_ESSENTIA_SOURCE_BE.get(), pos, state);
        }

        public void addAspect(final Aspect aspect, final int amount) {
            pool.merge(aspect, amount, Integer::sum);
            setChanged();
        }

        @Override
        public int getAspectPoolSize() {
            return pool.values().stream().mapToInt(Integer::intValue).sum();
        }

        @Override
        public boolean doesContain(final Aspect aspect) {
            return pool.getOrDefault(aspect, 0) > 0;
        }

        @Override
        public int useAspect(final Aspect aspect, final int amount) {
            final int have = pool.getOrDefault(aspect, 0);
            final int taken = Math.min(have, amount);
            if (taken > 0) {
                pool.put(aspect, have - taken);
                setChanged();
            }
            return taken;
        }
    }
}
