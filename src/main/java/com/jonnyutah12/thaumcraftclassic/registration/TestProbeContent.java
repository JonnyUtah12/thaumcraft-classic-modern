package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only probe block + item for the registration/id-stability harness
 * (TC-M2-01). They prove the real {@link ModBlocks}/{@link ModItems} holders
 * produce registered objects and that those objects round-trip through save
 * data (block state NBT, item stack NBT).
 *
 * <p>TC-M8-01 adds probe accessory items for the accessory-layer harness:
 * a ring, an amulet, and a ring whose {@code canEquip} veto is always false.
 * The counters record lifecycle callbacks so gametests can assert the
 * exactly-once contract.</p>
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

    public static final DeferredItem<ProbeAccessory> PROBE_RING = ModItems.ITEMS.register("test_probe_ring",
            () -> new ProbeAccessory(Set.of(AccessorySlot.RING)));

    public static final DeferredItem<ProbeAccessory> PROBE_AMULET = ModItems.ITEMS.register("test_probe_amulet",
            () -> new ProbeAccessory(Set.of(AccessorySlot.AMULET)));

    /** Ring whose item-side canEquip veto is always false. */
    public static final DeferredItem<ProbeAccessory> PROBE_VETO_RING = ModItems.ITEMS.register("test_probe_veto_ring",
            () -> new ProbeAccessory(Set.of(AccessorySlot.RING)) {
                @Override
                public boolean canEquip(final ItemStack stack, final Player player) {
                    return false;
                }
            });

    /** Item class for accessory probes: slot set + lifecycle counters. */
    public static class ProbeAccessory extends Item implements AccessoryItem {

        public static final AtomicInteger WORN_TICKS = new AtomicInteger();
        public static final AtomicInteger EQUIPPED = new AtomicInteger();
        public static final AtomicInteger UNEQUIPPED = new AtomicInteger();

        public static void resetCounters() {
            WORN_TICKS.set(0);
            EQUIPPED.set(0);
            UNEQUIPPED.set(0);
        }

        private final Set<AccessorySlot> slots;

        public ProbeAccessory(final Set<AccessorySlot> slots) {
            super(new Item.Properties());
            this.slots = slots;
        }

        @Override
        public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
            return slots;
        }

        @Override
        public void onWornTick(final ItemStack stack, final Player wearer) {
            WORN_TICKS.incrementAndGet();
        }

        @Override
        public void onEquipped(final ItemStack stack, final Player player) {
            EQUIPPED.incrementAndGet();
        }

        @Override
        public void onUnequipped(final ItemStack stack, final Player player) {
            UNEQUIPPED.incrementAndGet();
        }
    }

    /**
     * TC-M4-04: test-only essentia provider. The runic matrix drains essentia
     * from any block entity implementing {@link aspect.IAspectSource}; the real
     * ones are the essentia jar (TC-M4-06) and its mirror feed (TC-M4-06), so
     * the altar's craft leg runs against this seeded probe instead.
     */
    public static final DeferredBlock<Block> PROBE_ASPECT_SOURCE_BLOCK = ModBlocks.BLOCKS.register(
            "test_aspect_source_block",
            () -> new ProbeAspectSourceBlock());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeAspectSource>> PROBE_ASPECT_SOURCE =
            ModBlockEntities.BLOCK_ENTITY_TYPES.register("test_aspect_source",
                    () -> BlockEntityType.Builder.of(ProbeAspectSource::new,
                            PROBE_ASPECT_SOURCE_BLOCK.get()).build(null));

    /** Host block for the aspect-source probe: the entity is what TC-M4-04 reads. */
    public static final class ProbeAspectSourceBlock extends Block implements EntityBlock {

        ProbeAspectSourceBlock() {
            super(BlockBehaviour.Properties.of().strength(1.0F));
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new ProbeAspectSource(pos, state);
        }
    }

    /** Aspect-source probe: a seeded pool plus a drain counter. */
    public static class ProbeAspectSource extends BlockEntity implements IAspectSource {

        private static int drainedTotal = 0;

        public static void resetDrains() {
            drainedTotal = 0;
        }

        /** Total units this probe's aspect pool has handed out (test evidence). */
        public static int drainedTotal() {
            return drainedTotal;
        }

        private final java.util.LinkedHashMap<Aspect, Integer> pool = new java.util.LinkedHashMap<>();

        public ProbeAspectSource(final BlockPos pos,
                                final BlockState state) {
            super(PROBE_ASPECT_SOURCE.get(), pos, state);
        }

        /** Seed one aspect's charge (the test's stand-in for a filled jar). */
        public void seed(final Aspect aspect, final int amount) {
            pool.put(aspect, Math.max(0, amount));
        }

        @Override
        public int getAspectPoolSize() {
            int total = 0;
            for (final Integer held : pool.values()) {
                total += held;
            }
            return total;
        }

        @Override
        public boolean doesContain(final Aspect aspect) {
            final Integer held = pool.get(aspect);
            return held != null && held > 0;
        }

        @Override
        public int useAspect(final Aspect aspect, final int amount) {
            final Integer held = pool.get(aspect);
            if (held == null || held <= 0) {
                return 0;
            }
            final int taken = Math.min(held, Math.max(1, amount));
            pool.put(aspect, held - taken);
            drainedTotal += taken;
            return taken;
        }
    }

    /** Forces class initialization (entry registration) from {@link ModRegistration}. */
    static void init() {
    }

    private TestProbeContent() {
    }
}
