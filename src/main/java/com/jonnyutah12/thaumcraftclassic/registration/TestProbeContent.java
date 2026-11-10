package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

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

    /**
     * Minimal vis container for the TC-M4-03 arcane-workbench gametest: the
     * wand slot needs an {@link IVisContainerItem} stack carrying a
     * {@code thaumcraft:vis_pool} component, and no shipped item is one yet
     * (wand items are TC-M6-01). Test-only, same gate as the probe block.
     */
    public static final DeferredItem<Item> PROBE_VIS_CONTAINER =
            ModItems.ITEMS.register("test_probe_vis_container", ProbeVisContainer::new);

    /** Probe vis container item (see {@link #PROBE_VIS_CONTAINER}). */
    public static final class ProbeVisContainer extends Item implements IVisContainerItem {

        /** Rod capacity 50 vis → derived max via the TC-M4-02 rule. */
        public static final int MAX_VIS = WandVis.maxVis(50, false);

        public ProbeVisContainer() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public AspectList getAspectsWithRoom(final ItemStack stack) {
            return WandVis.getAspectsWithRoom(WandVis.poolOf(stack), MAX_VIS);
        }

        @Override
        public boolean addVis(final ItemStack stack, final Aspect aspect, final int amount,
                             final boolean doUpdate) {
            final WandVis.AddResult result =
                    WandVis.addVis(WandVis.poolOf(stack), aspect, amount, MAX_VIS);
            if (doUpdate) {
                WandVis.setPool(stack, result.pool());
            }
            return result.added() > 0;
        }

        @Override
        public String getCapTag(final ItemStack stack) {
            return "test";
        }

        @Override
        public String getRodTag(final ItemStack stack) {
            return "test";
        }
    }

    /** Forces class initialization (entry registration) from {@link ModRegistration}. */
    static void init() {
    }

    private TestProbeContent() {
    }
}
