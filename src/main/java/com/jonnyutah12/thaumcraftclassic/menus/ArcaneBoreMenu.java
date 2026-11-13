package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the arcane bore screen (the original {@code gui_id} 15,
 * {@code ContainerArcaneBore}).
 *
 * <p>Geometry and slot rules cite the original container 1:1: the
 * excavation-focus slot at {@code (26,18)} accepts exactly the original's
 * focus class (the id contract behind
 * {@link TileArcaneBore#isExcavationFocus}, the same contract the tile's
 * run gate uses), the pickaxe slot at {@code (74,18)} accepts picks
 * (the original's {@code ItemPickaxe} instanceof — the port matches the
 * tile's own {@link net.minecraft.tags.ItemTags#PICKAXES} gate in
 * {@code refreshTools}), and the player grid sits at {@code 59} with the
 * hotbar at {@code 117} — the original's short 176x141 panel, not the
 * default 166.</p>
 *
 * <p>The screen's readout state (area, max radius, speed, fortune) travels
 * in four data slots (0..3), the same {@link ContainerData} shape the decon
 * and spa menus ship, so the screen draws from synced state only.
 * {@link #quickMoveStack} reproduces {@code ContainerArcaneBore.func_82846_b}
 * branch for branch, and {@link #stillValid} rides the tile seam the bore
 * tile already ships.</p>
 */
public final class ArcaneBoreMenu extends AbstractContainerMenu {

    /** The player grid starts at slot index 2, vanilla's convention. */
    private static final int PLAYER_INVENTORY_START = 2;
    /** The original's {@code par2 >= 2 && par2 < 29} main-grid bound. */
    private static final int PLAYER_MAIN_END = PLAYER_INVENTORY_START + 27;
    /** The original's {@code par2 >= 29 && par2 < 38} hotbar bound. */
    private static final int PLAYER_END = PLAYER_INVENTORY_START + 36;

    /** Data slot 0: the tile's installed focus {@code area}. */
    public static final int DATA_AREA = 0;
    /** Data slot 1: the tile's {@code maxRadius}. */
    public static final int DATA_MAX_RADIUS = 1;
    /** Data slot 2: the tile's {@code speed}. */
    public static final int DATA_SPEED = 2;
    /** Data slot 3: the tile's {@code fortune} (from the pickaxe enchantment). */
    public static final int DATA_FORTUNE = 3;

    private final TileArcaneBore bore;
    private final ContainerData data;

    public ArcaneBoreMenu(final int windowId, final Inventory playerInventory,
                          final TileArcaneBore bore) {
        super(null, windowId);
        this.bore = bore;
        this.checkContainerSize(bore, TileArcaneBore.SLOTS);
        // ContainerArcaneBore: focus slot (26,18), pickaxe slot (74,18).
        addSlot(new Slot(bore, 0,
                ContainerLayout.BORE_SLOT_FOCUS_X, ContainerLayout.BORE_SLOT_FOCUS_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return TileArcaneBore.isExcavationFocus(stack);
            }
        });
        addSlot(new Slot(bore, 1,
                ContainerLayout.BORE_SLOT_PICKAXE_X, ContainerLayout.BORE_SLOT_PICKAXE_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                // The original's ItemPickaxe instanceof, matched to the tile's
                // own refreshTools gate (ItemTags.PICKAXES).
                return !stack.isEmpty() && stack.is(ItemTags.PICKAXES);
            }
        });
        // The original's short panel: player grid at 59, hotbar at 117.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, ContainerLayout.BORE_PLAYER_GRID_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    8 + column * 18, ContainerLayout.BORE_PLAYER_HOTBAR_TOP));
        }
        this.data = new ContainerData() {
            @Override
            public int get(final int index) {
                switch (index) {
                    case DATA_AREA:
                        return bore.area;
                    case DATA_MAX_RADIUS:
                        return bore.maxRadius;
                    case DATA_SPEED:
                        return bore.speed;
                    case DATA_FORTUNE:
                        return bore.fortune;
                    default:
                        return 0;
                }
            }

            @Override
            public void set(final int index, final int value) {
                switch (index) {
                    case DATA_AREA:
                        bore.area = value;
                        break;
                    case DATA_MAX_RADIUS:
                        bore.maxRadius = value;
                        break;
                    case DATA_SPEED:
                        bore.speed = value;
                        break;
                    case DATA_FORTUNE:
                        bore.fortune = value;
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
        addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(final Player player) {
        return bore.stillValid(player);
    }

    /** Synced excavation area (data slot 0). */
    public int getArea() {
        return data.get(DATA_AREA);
    }

    /** Synced max radius (data slot 1); drawn width is {@link ContainerLayout#boreWidth}. */
    public int getMaxRadius() {
        return data.get(DATA_MAX_RADIUS);
    }

    /** Synced speed bonus (data slot 2). */
    public int getSpeed() {
        return data.get(DATA_SPEED);
    }

    /** Synced fortune (data slot 3), from the tile's pickaxe refresh. */
    public int getFortune() {
        return data.get(DATA_FORTUNE);
    }

    /** The pickaxe slot's stack, for the screen's near-break icon and property rows. */
    public ItemStack getPickaxeStack() {
        return slots.get(1).getItem();
    }

    /**
     * The original's "Native Clusters" row gate: {@code GuiArcaneBore} shows
     * it when {@code instanceof ItemElementalPickaxe} — that item is
     * TC-M6-01-era and not registered, so match its contract id, the same
     * id-contract convention as {@link TileArcaneBore#isExcavationFocus}
     * (cites {@code BlockWoodenDevice:346}).
     */
    public static boolean isElementalPickaxe(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("thaumcraft") && id.getPath().equals("itemelementalpickaxe");
    }

    /**
     * The original's silk row: {@code GuiArcaneBore:60} shows "Silk Touch"
     * when {@code EnchantmentHelper.getSilkTouch(pickaxe) > 0} — the port's
     * equivalent is the vanilla {@link Enchantments#SILK_TOUCH} item level.
     * The pickaxe slot's stack is synced, so the client sees the real value.
     */
    public boolean hasSilkTouch(final ItemStack pickaxe) {
        // 1.21.1 enchantments are a datapack registry: the Enchantments
        // constants are ResourceKeys and EnchantmentHelper wants a Holder,
        // so resolve through the bore's level exactly as the tile's private
        // enchantmentHolder does (there is no BuiltInRegistries entry).
        if (pickaxe.isEmpty() || bore.getLevel() == null) {
            return false;
        }
        return net.minecraft.world.item.enchantment.EnchantmentHelper
                .getItemEnchantmentLevel(
                        bore.getLevel().registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .getHolderOrThrow(
                                        net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
                        pickaxe) > 0;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack stack = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return stack;
        }
        final ItemStack slotStack = slot.getItem();
        stack = slotStack.copy();
        if (index <= 1) {
            // Machine slots shift-click out to the whole player inventory.
            if (!slotStack.isEmpty()
                    && !moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (TileArcaneBore.isExcavationFocus(slotStack)) {
            if (!moveItemStackTo(slotStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotStack.is(ItemTags.PICKAXES)) {
            if (!moveItemStackTo(slotStack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_MAIN_END) {
            if (!moveItemStackTo(slotStack, PLAYER_MAIN_END, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_MAIN_END && index < PLAYER_END) {
            if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_MAIN_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack;
    }
}
