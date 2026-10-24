package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
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
     * TC-M6-05: damageable probe implementing the Repair enchantment's item
     * contracts (IRepairable marker + IVisContainerItem vis payment). The
     * gametest supplies the object-tag cost directly, since the shipped
     * essentia tables contain no damageable items until TC-M6-01.
     */
    public static final DeferredItem<ProbeRepairableVis> PROBE_REPAIRABLE =
            ModItems.ITEMS.register("test_probe_repairable",
                    () -> new ProbeRepairableVis(new Item.Properties().stacksTo(1).durability(32)));

    public static class ProbeRepairableVis extends Item
            implements com.jonnyutah12.thaumcraftclassic.api.IRepairable,
                       com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem {

        public ProbeRepairableVis(final Properties properties) {
            super(properties);
        }

        @Override
        public com.jonnyutah12.thaumcraftclassic.aspect.AspectList getAspectsWithRoom(final ItemStack stack) {
            return new com.jonnyutah12.thaumcraftclassic.aspect.AspectList();
        }

        @Override
        public void addVis(final ItemStack stack,
                           final com.jonnyutah12.thaumcraftclassic.aspect.Aspect aspect,
                           final int amount, final boolean doUpdate) {
        }

        @Override
        public String getCapTag(final ItemStack stack) {
            return "probe";
        }

        @Override
        public String getRodTag(final ItemStack stack) {
            return "probe";
        }
    }

    /** Forces class initialization (entry registration) from {@link ModRegistration}. */
    static void init() {
    }

    private TestProbeContent() {
    }
}
