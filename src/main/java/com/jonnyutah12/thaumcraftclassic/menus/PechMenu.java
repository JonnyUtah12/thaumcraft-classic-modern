package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeChestPool;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeInventory;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechTradeRoll;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogPech;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Pech trade window (content-mapping.csv {@code thaumcraft:GUI_PECH}, original
 * gui_id 1).
 *
 * <p>Original: {@code common/entities/ContainerPech.java} (283 lines) over
 * {@code common/entities/InventoryPech.java} — a 5-slot window inventory
 * ({@code InventoryPech.java:11} {@code ItemStack[5]}, stack cap 64 at
 * {@code :69-71}) whose slot-validity rule is {@code par1 == 0} ({@code :77-79}):
 * only the offered stack accepts insertion. The offer sits at (36, 29)
 * ({@code ContainerPech.java:33}) and the 2x2 output grid at
 * {@code 106 + 18*j}, {@code 20 + 18*i} ({@code :37}) with
 * {@code common/container/SlotOutput.java} — {@code func_75214_a} returns false
 * ({@code SlotOutput.java:12-14}), reproduced as 1.21.1 {@code Slot#mayPlace}.
 * Then the standard 3x9 player grid at y = 84 and the hotbar at 142
 * ({@code :43,48}). The window inventory is per-container in the original (the
 * {@code InventoryPech} is built in the constructor, {@code :31}), so a
 * per-menu {@code SimpleContainer(5)} is the faithful 1.21.1 shape. Opening marks
 * the pech trading ({@code :32}); closing marks it not trading ({@code :204}) and
 * drops slots 0..4 to the world as items owned by the literal
 * {@code "PechDrop"} ({@code :205-212}, the owner stamp at {@code :211}) so the
 * pech's own pickup goal excludes them
 * ({@code entities/ai/pech/AIPechItemEntityGoto.java:76-78},
 * {@code PechAiLogic.PECH_DROP_OWNER}); the drop runs server-side on the
 * original's {@code !world.isRemote} gate ({@code :205}). Shift-clicks move 0..4
 * to the player region and 5..40 to the offer slot only ({@code :177-189} — the
 * output grid is never a shift destination). The window stays open while the pech
 * is tamed ({@code :164-166}, and the same tamed check on the inventory at
 * {@code InventoryPech.java:73-75}).</p>
 *
 * <p>The dice predicate is the original's overlay condition
 * ({@code client/gui/GuiPech.java:34-39}): slot 0 non-empty, slots 1..4 all
 * empty, and the pech's own {@code isValued} (the valued table / rich-aspects
 * check, {@code EntityPech.java:707-722}, ported at
 * {@code entities/monster/EntityPech.java:isValued}). The original evaluates it
 * in-process on the client, because a 1.7.10 GUI holds the entity and the slots
 * directly ({@code GuiPech.java:15,21}). The port's screen holds only its menu,
 * and 1.7.10's {@code Container} has <b>no</b> data-slot mechanism — the
 * original's crafting/stat plumbing here is two empty overrides
 * ({@code ContainerPech.java:56-62} {@code addCraftingToCrafters},
 * {@code :161} {@code updateDataSlot}), so nothing was synced and the client read
 * the live entity. The port therefore computes the predicate on the server and
 * mirrors it as a 1-int {@link ContainerData} slot: the client's copy is the
 * server's answer, the screen never touches the entity, and a dedicated-server
 * probe can measure the same bit (BD-73).</p>
 *
 * <p><b>The dice roll landed with TC-M7-04-m3-b-followup, its pool with
 * TC-M7-04-m3-b-c:</b>
 * {@link #clickMenuButton} carries button id 0 into {@link #generateContents()}
 * ({@code ContainerPech.java:83-158}) — the value, the scam roll, the jitter,
 * the payout loop, the loot-pack draw and the trade table — over the
 * registry-gated port of {@code EntityPech.tradeInventory}
 * ({@code entities/monster/PechTradeInventory.java}), the transcribed
 * {@code dungeonChest} pool ({@code entities/monster/PechTradeChestPool.java})
 * and the pure roll planner ({@code entities/monster/PechTradeRoll.java}). The
 * {@code ChestGenHooks} "dungeonChest" branch draws that pool under the
 * original's filter and 50-attempt bound (BD-80), the table search is bounded
 * (BD-74) and the {@code potionitem} metas ride as data (BD-81). The 25x25 dice
 * overlay ships as a framed vanilla-toned well — the original's
 * {@code gui_pech.png} (176,0) sprite is an asset-overlay-only sheet
 * (D-004 / BD-37).</p>
 */
public class PechMenu extends AbstractContainerMenu {

    /** The original's dice click: {@code GuiPech.java:62}
     *  {@code handleInventoryButtonClick(containerId, 0)} →
     *  {@code ContainerPech.java:64-70} {@code if (par2 == 0) generateContents()}.
     *  The id is 0 — the trunk/spa lanes' button 1 is their own id, not this one. */
    public static final int DICE_BUTTON = 0;

    /** The literal owner the original stamps on a dropped stack
     *  ({@code ContainerPech.java:211} {@code func_145799_b("PechDrop")}), read
     *  back by {@code AIPechItemEntityGoto} under the same {@code "Owner"} key. */
    private static final String DROP_OWNER = PechAiLogic.PECH_DROP_OWNER;

    private final SimpleContainer inventory = new SimpleContainer(5);
    private final EntityPech pech;

    /** The synced dice bit (the {@link ContainerData} slot's client copy). */
    private int diceValue;

    /** Server side: the pech the window is bound to. */
    public PechMenu(final int windowId, final Inventory playerInventory, final EntityPech pech) {
        super(CatalogPech.GUI_PECH.get(), windowId);
        this.pech = pech;
        build(playerInventory);
        if (this.pech != null) {
            this.pech.trading = true;                                                      // ContainerPech.java:32
        }
        bindDataSlot();
    }

    /** The dice bit as the data slot serves it (the probe seam for the sync). */
    public int diceDataValue() {
        return diceReady() ? 1 : 0;
    }

    /** Client side: the extra buffer carries the pech's entity id (the original's
     *  {@code openGui} passed it as the x argument, {@code EntityPech.java:697}). */
    public PechMenu(final int windowId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        super(CatalogPech.GUI_PECH.get(), windowId);
        final int entityId = extraData.readInt();
        this.pech = playerInventory.player.level().getEntity(entityId) instanceof EntityPech resolved
                ? resolved : null;
        build(playerInventory);
        bindDataSlot();
    }

    private void build(final Inventory playerInventory) {
        // ContainerPech.java:33 — the offered stack.
        this.addSlot(new Slot(this.inventory, 0, ContainerLayout.PECH_OFFER_X, ContainerLayout.PECH_OFFER_Y));
        // ContainerPech.java:37 — the 2x2 output grid; SlotOutput insertion rule.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                this.addSlot(new OutputSlot(this.inventory, 1 + row * 2 + col,
                        ContainerLayout.pechOutputX(col), ContainerLayout.pechOutputY(row)));
            }
        }
        // ContainerPech.java:43 — the player grid at 84.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, ContainerLayout.PECH_PLAYER_GRID_TOP + row * 18));
            }
        }
        // ContainerPech.java:48 — the hotbar at 142.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, ContainerLayout.PECH_HOTBAR_TOP));
        }
    }

    private void bindDataSlot() {
        this.addDataSlots(new ContainerData() {
            @Override
            public int get(final int index) {
                return PechMenu.this.diceReady() ? 1 : 0;
            }

            @Override
            public void set(final int index, final int value) {
                PechMenu.this.diceValue = value;
            }

            @Override
            public int getCount() {
                return 1;
            }
        });
    }

    /**
     * The overlay/click predicate — {@code GuiPech.java:38-39}: slot 0 non-empty,
     * slots 1..4 all empty, and the pech's {@code isValued} on the offer.
     */
    public boolean diceReady() {
        if (this.pech == null) {
            return false;
        }
        final ItemStack offer = this.inventory.getItem(0);
        if (offer.isEmpty()) {
            return false;
        }
        for (int a = 1; a < 5; a++) {
            if (!this.inventory.getItem(a).isEmpty()) {
                return false;
            }
        }
        return this.pech.isValued(offer);
    }

    /** The synced dice bit the screen reads (the data-slot client copy). */
    public boolean diceActive() {
        return this.diceValue != 0;
    }

    /**
     * The window's button: {@code GuiPech.java:62} sends
     * {@code window_click(container, 0, -1, 0)} from the dice rect and
     * {@code ContainerPech.java:64-70} runs {@code generateContents()} for
     * {@code id == 0} and returns true. Button 0 is accepted ONLY while the
     * original's roll gate stands ({@link #rollGateOk}, {@code :84-90}) —
     * {@code GuiPech.java:60} only draws the dice when its own predicate
     * ({@code :34-39}) holds, so the client cannot reach a roll it cannot show,
     * and the server re-checks it. The client screen plays the pech_dice cue on
     * the accepted answer ({@code GuiPech.java:63,67-80}); the roll's own
     * {@code thaumcraft:pech_trade} cue is server-side, on the pech
     * ({@code :95}).
     */
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId != 0) {
            return false;                                                                   // :64-66
        }
        if (!rollGateOk()) {
            return false;                                                                   // :84-90
        }
        generateContents();                                                                 // :67
        return true;                                                                        // :68
    }

    /**
     * The original's roll gate, {@code ContainerPech.java:84-90}: the server,
     * slot 0 non-null, slots 1..4 ALL null, and {@code pech.isValued(offer)}
     * (the valued table / rich-aspects check, {@code EntityPech.java:707-722}).
     * {@code world.isRemote} is the 1.7.10 client flag
     * ({@code world.field_72995_K}); the menu is built server-side (the client
     * copy's {@code pech} is null), so a null pech is the client arm.
     */
    private boolean rollGateOk() {
        if (this.pech == null || this.pech.level().isClientSide()) {
            return false;                                                                   // :84
        }
        return diceReady();                                 // :85-90 — the same predicate
    }

    /**
     * The roll: {@code ContainerPech.java:83-159}, the arm the original's button
     * 0 runs. The arithmetic is the pure planner
     * ({@link PechTradeRoll#plan}) so a unit test pins the exact
     * value→payout sequence over a seeded stream; the draws, the scam,
     * the loot-pack decrement and the trade-table lookup happen here.
     *
     * <p>The pech is the RNG: the original draws on {@code pech.getRandom()}
     * ({@code :92,98-102,107,109,118,126,132,147}) — the entity's
     * {@code RandomSource}, NOT the world's and NOT {@code RandomSource.create()}
     * (which is 1.21.1's thread-safe factory and does not exist in 1.7.10). The
     * offer loses exactly ONE item at the end ({@code :156}), whatever the roll
     * paid, and an unvalued offer never gets here at all (the gate,
     * {@link #rollGateOk}).</p>
     */
    private void generateContents() {
        final net.minecraft.world.level.Level level = this.pech.level();
        final ItemStack offer = this.inventory.getItem(0);                                   // :85
        int value = this.pech.getValue(offer);                                               // :91
        if (PechTradeRoll.scams(level.getRandom().nextInt(PechTradeRoll.SCAM_ROLL_BOUND), value)) {
            this.pech.setTamed(false);                                                       // :94
            this.pech.flagAiUpdate();                                                        // :95
            this.pech.playTradeCue();                                                        // :95
        }
        value = PechTradeRoll.jitteredValue(value, level.getRandom());                        // :98-102
        final int[] packSlots = PechTradeRoll.packSlotsOf(this.pech.lootSlots());             // :73-81
        final java.util.List<PechTradeInventory.TradeRow> liveRows =
                PechTradeInventory.liveRows(this.pech.getPechType(), level.registryAccess());  // :104
        final java.util.List<PechTradeChestPool.ChestRow> chestPool =
                PechTradeChestPool.liveRows(level.registryAccess());                           // :25, :127
        final PechTradeRoll.Plan plan =
                PechTradeRoll.plan(value, level.getRandom(), liveRows, chestPool, packSlots);  // :106-155
        for (final PechTradeRoll.Outcome outcome : plan.outcomes()) {
            if (outcome instanceof PechTradeRoll.FromPack fromPack) {                         // :109-125
                payFromLootPack(fromPack.packSlot());
            } else if (outcome instanceof PechTradeRoll.FromChest fromChest) {                // :136-139
                final ItemStack payout = PechTradeChestPool.resolve(fromChest.row(), level.registryAccess());
                if (!payout.isEmpty()) {
                    payToOutputRange(payout);                                                 // :139
                }
            } else if (outcome instanceof PechTradeRoll.FromTable fromTable) {                // :143-153
                final ItemStack payout = PechTradeInventory.resolve(fromTable.row(), level.registryAccess());
                if (!payout.isEmpty()) {
                    payToOutputRange(payout);                                                 // :150-152
                }
            }
        }
        this.inventory.removeItem(0, 1);                                                      // :156
    }

    /**
     * {@code ContainerPech.java:118-124} — copy ONE item out of the pech's loot
     * pack into the output grid, then decrement the pack slot. The reference cuts
     * the copy first: {@code ItemStack is = pech.loot[r].copy(); is.field_77994_a
     * = 1;} ({@code :118-119}) before {@code func_75135_a(is, 1, 5, false)}
     * ({@code :120}), so a 5-stack of gold pays a single gold, not the stack;
     * {@code :121-123} then decrements the slot and nulls it at zero
     * ({@code InventoryPech.java:57-67} {@code decStack}), which
     * {@code ItemStack#shrink} reaches at empty.
     */
    private void payFromLootPack(final int packSlot) {
        final ItemStack stack = this.pech.lootSlots()[packSlot];
        if (stack.isEmpty()) {
            return;
        }
        final ItemStack paid = stack.copy();
        paid.setCount(1);                                                                     // :118-119
        payToOutputRange(paid);                                                               // :120
        final ItemStack left = stack.copy();
        left.shrink(1);
        this.pech.setLootSlot(packSlot, left.isEmpty() ? ItemStack.EMPTY : left);             // :121-123
    }

    /**
     * The insertion the original does through {@code EntityPech.payToChest}
     * (vanilla 1.7.10 code, not in this repo's reference tree): a
     * {@code mergeItem}-style pass over the cells first (matching item AND
     * components, room left), then the first empty cell. It lands on the menu's
     * 2x2 grid — slots 1..4, NOT the player inventory, which is what the
     * name would suggest (BD-73 records the seam; the original's GUI reads the
     * container, so the cells are the only place the payout can show).
     *
     * <p>{@code Slot#mayPlace} is false on these cells
     * ({@code SlotOutput.java:12-14}) — that rule gates PLAYER insertion,
     * and the original's roll bypasses it because the container writes the slot
     * directly. This method is that direct write: it does NOT consult
     * {@code mayPlace}, and the probe pins both halves.</p>
     */
    private void payToOutputRange(final ItemStack payout) {
        final int max = payout.getMaxStackSize();                                             // :221
        for (int a = 1; a < 5; a++) {                                                         // :223-232
            final ItemStack current = this.slots.get(a).getItem();
            if (!current.isEmpty()
                    && ItemStack.isSameItemSameComponents(current, payout)
                    && current.getCount() < max) {
                final int moved = Math.min(payout.getCount(), max - current.getCount());     // :228-230
                current.grow(moved);
                payout.shrink(moved);
                this.slots.get(a).set(current);
                this.slots.get(a).setChanged();
                if (payout.isEmpty()) {
                    return;
                }
            }
        }
        for (int a = 1; a < 5; a++) {                                                         // :233-240
            if (this.slots.get(a).getItem().isEmpty()) {
                this.slots.get(a).set(payout.copy());
                this.slots.get(a).setChanged();
                return;
            }
        }
    }

    /** The roll's table draw space for this pech type — the live rows
     *  ({@link PechTradeInventory#liveRows}); the probe measures it. */
    public java.util.List<PechTradeInventory.TradeRow> tradeTable() {
        return PechTradeInventory.liveRows(this.pech.getPechType(), this.pech.level().registryAccess());
    }

    /** The roll's {@code dungeonChest} draw space — the registered rows of the
     *  transcribed pool ({@link PechTradeChestPool#liveRows}, the original's
     *  {@code :25}/{@code :127} pool handle with D-007's gate); the probe
     *  measures it. */
    public java.util.List<PechTradeChestPool.ChestRow> chestPool() {
        return PechTradeChestPool.liveRows(this.pech.level().registryAccess());
    }

    /** The pech this window is bound to ({@code ContainerPech.java:27-29}). */
    public EntityPech pech() {
        return this.pech;
    }

    /**
     * {@code ContainerPech.java:168-189} — slots 0..4 shift to the player region
     * (5..41); player slots 5..40 shift to the offer slot only (0..1), never the
     * output grid.
     */
    @Override
    public ItemStack quickMoveStack(final Player playerMoving, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < 5) {                                                                   // :177-186
            if (!this.moveItemStackTo(stack, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 41) {                                                           // :187-189
            if (!this.moveItemStackTo(stack, 0, 1, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * {@code ContainerPech.java:202-212} — closing marks the pech not trading
     * ({@code :204}) and drops slots 0..4 to the world stamped with the
     * {@code "PechDrop"} owner ({@code :211}), which the pech's pickup goal
     * excludes ({@code AIPechItemEntityGoto.java:76-78}). The drop runs on the
     * server, as the original's {@code !world.isRemote} gate ({@code :205}) does:
     * in singleplayer the original's one shared container ran it once.
     */
    @Override
    public void removed(final Player playerLeaving) {
        super.removed(playerLeaving);
        if (this.pech != null) {
            this.pech.trading = false;                                                     // :204
        }
        if (this.pech == null || playerLeaving.level().isClientSide) {
            return;                                                                        // :205
        }
        for (int a = 0; a < 5; a++) {                                                      // :206-212
            final ItemStack stack = this.inventory.removeItem(a, this.inventory.getItem(a).getCount());
            if (stack.isEmpty()) {
                continue;
            }
            final ItemEntity dropped = playerLeaving.drop(stack, false);
            if (dropped != null) {
                final CompoundTag owner = new CompoundTag();
                owner.putString("PechDropOwner", DROP_OWNER);                              // :211, BD-73 key
                dropped.getItem().set(DataComponents.ENTITY_DATA, CustomData.of(owner));
            }
        }
    }

    /**
     * {@code ContainerPech.java:164-166} — the window stays valid while the pech
     * is tamed (the window inventory carries the same check,
     * {@code InventoryPech.java:73-75}); 1.21.1 also closes it when the entity
     * leaves the level.
     */
    @Override
    public boolean stillValid(final Player playerChecking) {
        return this.pech != null && !this.pech.isRemoved() && this.pech.isTamed();
    }

    /** {@code common/container/SlotOutput.java:12-14} — an output cell that refuses
     *  insertion ({@code func_75214_a} returns false), 1.21.1 {@code Slot#mayPlace}.
     *  The original's window inventory states the same rule from the other side:
     *  {@code isItemValidForSlot} is {@code par1 == 0}
     *  ({@code InventoryPech.java:77-79}). */
    private static final class OutputSlot extends Slot {
        OutputSlot(final net.minecraft.world.Container container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }
    }
}
