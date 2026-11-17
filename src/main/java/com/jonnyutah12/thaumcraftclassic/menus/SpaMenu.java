package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

/**
 * Arcane spa menu (content-mapping.csv {@code thaumcraft:gui_spa}, original
 * {@code ContainerSpa}).
 *
 * <p>One bath-salt slot; menu button 1 toggles the mix mode (original
 * {@code onContainerEvent(button==1) → toggleMix}).</p>
 *
 * <p><b>Tank and mix sync (TC-M7-04-m4).</b> The original {@code GuiSpa} read
 * {@code spa.tank} and {@code spa.getMix()} because the 1.7.10 GUI held the
 * tile; a 1.21.1 screen gets only the menu, so the tank now travels in four
 * container data slots — fluid registry id plus one (0 = empty), amount,
 * capacity, and the mix flag. The screen scales the fluid column
 * {@code amount * 48 / capacity} exactly as the original's
 * {@code bar = fluidAmount / capacity} blit did, and fills the 8x8 mix box on
 * the on-state (docs/BEHAVIORAL_DEVIATIONS.md BD-17 closure).</p>
 */
public class SpaMenu extends AbstractContainerMenu {

    /** Data slot carrying the tank fluid's {@code minecraft:fluid} registry id plus one (0 = empty). */
    public static final int DATA_FLUID_ID = 0;
    /** Data slot carrying the tank's current mB amount. */
    public static final int DATA_FLUID_AMOUNT = 1;
    /** Data slot carrying the tank's capacity in mB. */
    public static final int DATA_CAPACITY = 2;
    /** Data slot carrying the mix flag (1 = on, 0 = off). */
    public static final int DATA_MIX = 3;

    private final TileSpa spa;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            switch (index) {
                case DATA_FLUID_ID: {
                    final ResourceLocation key = SpaMenu.this.spa.getTankFluid();
                    final Fluid fluid = key == null ? null : BuiltInRegistries.FLUID.get(key);
                    return fluid == null ? 0 : BuiltInRegistries.FLUID.getId(fluid) + 1;
                }
                case DATA_FLUID_AMOUNT:
                    return SpaMenu.this.spa.getTankAmount();
                case DATA_CAPACITY:
                    return SpaMenu.this.spa.getTankCapacity();
                case DATA_MIX:
                    return SpaMenu.this.spa.getMix() ? 1 : 0;
                default:
                    return 0;
            }
        }

        @Override
        public void set(final int index, final int value) {
            // Server -> client landing point: the client's menu instance holds
            // the client tile, and the screen reads the state through the
            // getters below, so the sync writes into the tile (TC-M7-04-s's
            // DeconstructionTableMenu break-time pattern, generalized).
            switch (index) {
                case DATA_FLUID_ID:
                    SpaMenu.this.syncedFluid = value <= 0 ? null : BuiltInRegistries.FLUID.byId(value - 1);
                    SpaMenu.this.pushTank();
                    break;
                case DATA_FLUID_AMOUNT:
                    SpaMenu.this.syncedAmount = value;
                    SpaMenu.this.pushTank();
                    break;
                case DATA_MIX:
                    SpaMenu.this.spa.setMixFromSync(value != 0);
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private Fluid syncedFluid;
    private int syncedAmount;

    public SpaMenu(final int id, final Inventory playerInventory, final TileSpa spa) {
        super(null, id);
        this.spa = spa;
        this.addSlot(new Slot(spa, 0, 65, 31) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return SpaMenu.this.spa.canPlaceItem(0, stack);
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        addDataSlots(data);
    }

    private void pushTank() {
        this.spa.setTankFromSync(
                this.syncedFluid == null ? null : BuiltInRegistries.FLUID.getKey(this.syncedFluid),
                this.syncedAmount);
    }

    /**
     * The tank's fluid, or null when empty. Server side this is the tile's
     * live tank; client side it is what data slot {@link #DATA_FLUID_ID} last
     * synced in, so {@code SpaScreen} can draw the original's fluid column and
     * tooltip (BD-17 closure).
     */
    @Nullable
    public Fluid getTankFluid() {
        final ResourceLocation key = this.spa.getTankFluid();
        return key == null ? null : BuiltInRegistries.FLUID.get(key);
    }

    /** The tank's current mB amount, synced through {@link #DATA_FLUID_AMOUNT}. */
    public int getTankAmount() {
        return this.spa.getTankAmount();
    }

    /** The tank's capacity (the original's 5000 mB), synced through {@link #DATA_CAPACITY}. */
    public int getTankCapacity() {
        return this.spa.getTankCapacity();
    }

    /** The mix flag, synced through {@link #DATA_MIX}; the original drew its indicator from this. */
    public boolean getMix() {
        return this.spa.getMix();
    }

    /** The container data slots of this menu, for probes that prove the sync lands. */
    public ContainerData containerData() {
        return this.data;
    }

    // MC 1.21.1 declares clickMenuButton as boolean (true = the button did something).
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == 1) {
            this.spa.toggleMix();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        // MC 1.21.1 Slot has no onQuickMove hook; moveItemStackTo already
        // marks the container changed via Slot#set.
        return stack.copy();
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.spa.stillValid(player);
    }
}
