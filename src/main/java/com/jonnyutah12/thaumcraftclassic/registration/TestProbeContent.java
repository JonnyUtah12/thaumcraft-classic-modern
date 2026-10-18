package com.jonnyutah12.thaumcraftclassic.registration;

import net.minecraft.world.item.Item;
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

    /** Forces class initialization (entry registration) from {@link ModRegistration}. */
    static void init() {
    }

    private TestProbeContent() {
    }
}
