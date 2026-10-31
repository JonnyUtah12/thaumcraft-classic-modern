package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The golem configuration window (content-mapping.csv
 * {@code Thaumcraft:GUI_GOLEM}, gui_id 0, original
 * {@code thaumcraft.common.entities.golems.ContainerGolem} — note the
 * entity-package location; the csv row's bare {@code GuiGolem} handler is not
 * the whole story). Opened by right-clicking the golem
 * ({@link EntityGolemBase#mobInteract}); the entity-bound constructor shape
 * follows {@link TrunkMenu}: the server builds from the live entity, the
 * client resolves it from the open-menu extra buffer's entity id (the
 * original carried the same id in the openGui x argument).
 *
 * <h2>What ships, measured off the original</h2>
 * <ul>
 *   <li><b>Player inventory</b> at the original coordinates: 3x9 at
 *       {@code (8 + j*18, 84 + i*18)} and the hotbar at {@code (8 + i*18, 142)}
 *       ({@code ContainerGolem.bindPlayerInventory}). The 84 px grid is why the
 *       panel is the 176x167 derived by {@code ContainerLayout.panelHeight},
 *       not the trunk's 118 px layout.</li>
 *   <li><b>Pause on open, resume on close</b>: the original constructor sets
 *       {@code golem.paused = true} and {@code onContainerClosed}
 *       ({@code func_75134_a}) sets it false. The field ships on
 *       {@link EntityGolemBase}; the goal/tick consumer is TC-M6-08-s-b.</li>
 *   <li><b>Validity</b>: {@code func_75145_c} → {@code mobInv.canInteractWith},
 *       i.e. {@code InventoryMob.canInteractWith} — entity not dead and the
 *       player's squared distance ≤ 64.0. Reproduced verbatim in
 *       {@link #stillValid}.</li>
 *   <li><b>Scroll buttons</b> 66/67 and the {@code currentScroll}/{@code
 *       maxScroll} pair ({@code ContainerGolem} fields, {@code maxScroll =
 *       slotCount/6 - 1}): the button ids and the clamp ship; with the golem
 *       inventory absent {@code maxScroll == -1} so the original's own clamp
 *       keeps them inert, exactly as it does for a golem whose core exposes ≤ 6
 *       slots.</li>
 *   <li><b>Click sound</b>: every answered button plays {@code random.click}
 *       at the golem at 0.2/0.8 (the tail of the original's
 *       {@code func_75140_a}) — 1.21.1's {@code random.click} is
 *       {@code SoundEvents.UI_BUTTON_CLICK}.</li>
 *   <li><b>Shift-click</b>: the original moves golem-region slots
 *       {@code [0, slotCount)} into the player region and player slots into
 *       the golem region, returning null on a failed leg. The leg structure
 *       ships verbatim over {@link #golemSlotCount()}; with the golem region
 *       empty every player-slot leg fails the move, which is what the original
 *       returns for a core without an inventory.</li>
 * </ul>
 *
 * <h2>What does not ship, and why (docs/BEHAVIORAL_DEVIATIONS.md BD-19)</h2>
 * The golem-side window is bound only when {@code ItemGolemCore.hasInventory(
 * core)} over an {@code InventoryMob} — that inventory container
 * ({@code setupGolemInventory}, slot counts, {@code SlotGhost}/
 * {@code SlotGhostFluid}, the color/toggle arrays {@code getColors}/
 * {@code setColors}/{@code getToggles}/{@code setToggle} and the fluid/essentia
 * display stacks) is the golem runtime card TC-M6-08-s-b and is not in this
 * tree. {@link #golemSlotCount()} is therefore 0: the six-slot window has no
 * live container to bind, the color-tab buttons {@code a} and {@code a +
 * slotCount} address an empty range, and the toggle buttons 50-57 flip arrays
 * the entity does not carry. The menu does not fake them, and the screen does
 * not draw them (BD-17 precedent: drawn state must be synced state). The
 * original's {@code refreshInventory} rebuild is unreachable by construction
 * while the clamp keeps {@code currentScroll} pinned.
 */
public class GolemMenu extends AbstractContainerMenu {

    /** Original {@code func_75140_a}: scroll-page buttons. */
    public static final int BUTTON_SCROLL_UP = 66;
    public static final int BUTTON_SCROLL_DOWN = 67;

    /** Original {@code func_75140_a}: the eight toggle buttons 50..57. */
    public static final int BUTTON_TOGGLE_FIRST = 50;
    public static final int BUTTON_TOGGLE_LAST = 57;

    /** The live golem (server side; client side resolved from the buffer, null when it is gone). */
    private final EntityGolemBase golem;

    /** The player inventory, kept for the original's {@code refreshInventory} rebuild. */
    private final Inventory playerInventory;

    /** Original {@code ContainerGolem.currentScroll} (public field). */
    private int currentScroll;

    /** Original {@code ContainerGolem.maxScroll = slotCount / 6 - 1}. */
    private int maxScroll;

    /** Server side: bound to the live golem (original constructor). */
    public GolemMenu(final int id, final Inventory playerInventory, final EntityGolemBase golem) {
        super(CatalogGolems.GUI_GOLEM.get(), id);
        this.golem = golem;
        this.playerInventory = playerInventory;
        // Original constructor: ((EntityGolemBase) mobInv.ent).paused = true.
        golem.setPaused(true);
        this.maxScroll = golemSlotCount() / 6 - 1;
        buildWindow();
        bindPlayerInventory(playerInventory);
    }

    /** Client side: resolve the entity from the open-menu extra buffer (TrunkMenu pattern). */
    public GolemMenu(final int id, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        super(CatalogGolems.GUI_GOLEM.get(), id);
        this.playerInventory = playerInventory;
        final int entityId = extraData.readInt();
        EntityGolemBase resolved = null;
        if (playerInventory.player.level().getEntity(entityId) instanceof EntityGolemBase live) {
            resolved = live;
        }
        this.golem = resolved;
        this.maxScroll = golemSlotCount() / 6 - 1;
        buildWindow();
        bindPlayerInventory(playerInventory);
    }

    /** The golem this menu is bound to; {@code null} on a client whose entity was gone. */
    public EntityGolemBase getGolem() {
        return this.golem;
    }

    /** Original {@code currentScroll} (page index into the six-slot window). */
    public int currentScroll() {
        return this.currentScroll;
    }

    /** Original {@code maxScroll} ({@code slotCount / 6 - 1}; {@code -1} = no window). */
    public int maxScroll() {
        return this.maxScroll;
    }

    /**
     * The golem-side inventory size. The original takes it from
     * {@code mobInv.slotCount}, sized by {@code setupGolemInventory()} for the
     * cores with {@code ItemGolemCore.hasInventory(core)} (0 Fill, 1 Empty,
     * 2 Gather, 5 Decanting, 8 Use). The mob inventory container is
     * TC-M6-08-s-b and is not in this tree, so the shipped answer is 0 — the
     * gate {@code getCore() > -1 && ItemGolemCore.hasInventory(core)} is the
     * original's, kept visible so the window binds the moment the container
     * lands (BD-19).
     */
    public int golemSlotCount() {
        if (this.golem == null) {
            return 0;
        }
        final int core = this.golem.getCore();
        if (!(core > -1 && ItemGolemCore.hasInventory(core))) {
            return 0;
        }
        // The core asks for a window; the container that sizes it is TC-M6-08-s-b.
        return 0;
    }

    /**
     * Original {@code bindGolemInventory}: six slots at
     * {@code (100 + a/2*28, 16 + a%2*31)} — {@code ContainerLayout
     * .golemSlotX/Y} — on the scroll page {@code a + currentScroll*6},
     * {@code SlotGhostFluid} for the Decanting core and {@code SlotGhost}
     * otherwise. Both slot classes sit on {@code InventoryMob}; with
     * {@link #golemSlotCount()} at 0 the loop binds nothing, and the geometry
     * stays in {@code ContainerLayout} for the screen's window frames.
     */
    private void buildWindow() {
        // golemSlotCount() == 0 today (BD-19): nothing to bind. The loop the
        // moment the container lands is
        //   for (int a = 0; a < Math.min(6, slotCount); a++)
        //       addSlot(new Slot(mobInv, a + currentScroll * 6,
        //                        golemSlotX(a), golemSlotY(a)));
    }

    /** Original {@code bindPlayerInventory} verbatim. */
    private void bindPlayerInventory(final Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9,
                        8 + col * 18, ContainerLayout.PLAYER_GRID_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col,
                    8 + col * 18, ContainerLayout.PLAYER_HOTBAR_TOP));
        }
    }

    /**
     * Original {@code refreshInventory}: clear slots, rebind the window for
     * the current page, rebind the player rows. Reachable only through the
     * scroll buttons, which the {@code maxScroll} clamp keeps inert while
     * {@link #golemSlotCount()} is 0 (the original's own behaviour for a
     * six-slot golem), so this commit has no state the rebuild would change.
     */
    public void refreshInventory() {
        // Original: slots.clear(); bindGolemInventory(); bindPlayerInventory().
    }

    /**
     * Original {@code func_75140_a} (buttonClicked): 66/67 page the scroll
     * window through the {@code currentScroll}/{@code maxScroll} clamp; 50..57
     * flip {@code setToggle(button - 50)}; buttons {@code 0..slotCount} and
     * {@code slotCount..2*slotCount} cycle the per-slot colors through
     * 15..-1. The toggle array, the color array and the window's slot count
     * are golem runtime state (TC-M6-08-s-b) — with them absent their legs
     * address empty ranges, the clamp pins the page at 0, and only the
     * original's tail survives to run: the {@code random.click} at the golem
     * (0.2 / 0.8) and the {@code true} return (BD-19).
     */
    @Override
    public boolean clickMenuButton(final Player player, final int button) {
        if (this.golem == null || player.level().isClientSide) {
            return false;
        }
        if (button == BUTTON_SCROLL_UP && this.currentScroll > 0) {
            this.currentScroll--;
            this.refreshInventory();
        }
        if (button == BUTTON_SCROLL_DOWN && this.currentScroll < this.maxScroll) {
            this.currentScroll++;
            this.refreshInventory();
        }
        // Original ContainerGolem.func_75140_a colour legs, verbatim over
        // markerColorSlotCount() (the original's mobInv.slotCount, which by
        // construction IS the length of the golem's colour array:
        // EntityGolemBase.java:173 `this.colors = new byte[this.inventory.slotCount]`).
        // Button `slot` steps that slot's marker colour DOWN (underflow wraps to
        // 15) and button `slots + slot` steps it UP (overflow wraps to -1, the
        // wildcard). This is the GUI's marker-colour path (TC-M6-08-s-c).
        final int slots = markerColorSlotCount();
        if (button >= 0 && button < slots) {
            int c = this.golem.getColors(button) - 1;
            if (c < -1) {
                c = 15;
            }
            this.golem.setColors(button, c);
        }
        if (button >= slots && button < slots * 2) {
            int c = this.golem.getColors(button - slots) + 1;
            if (c > 15) {
                c = -1;
            }
            this.golem.setColors(button - slots, c);
        }
        // Original tail: playSound(x, y, z, "random.click", 0.2F, 0.8F).
        this.golem.level().playSound(null,
                this.golem.getX(), this.golem.getY(), this.golem.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.2F, 0.8F);
        return true;
    }

    /**
     * Original {@code func_82846_b} (transferStack): a golem-region slot
     * shifts into the player region {@code [slotCount, size)}, a player slot
     * into the golem region {@code [0, slotCount)}; a failed move returns
     * null (nothing moves). Legs verbatim over {@link #golemSlotCount()};
     * with the golem region empty every player-slot leg fails, the original's
     * result for a core with no inventory.
     */
    @Override
    public ItemStack quickMoveStack(final Player playerMoving, final int index) {
        ItemStack result = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        final int golemEnd = golemSlotCount();
        if (slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < golemEnd) {
                if (!this.moveItemStackTo(stack, golemEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, golemEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /**
     * Original {@code func_75134_a} ({@code onContainerClosed}):
     * {@code golem.paused = false}. Server side only — the marker is plain
     * server field state, and the 1.7.10 client container wrote it on the
     * client copy, which has no consumer here.
     */
    @Override
    public void removed(final Player playerLeaving) {
        super.removed(playerLeaving);
        if (this.golem != null && !playerLeaving.level().isClientSide) {
            this.golem.setPaused(false);
        }
    }

    /**
     * Original {@code func_75145_c} → {@code InventoryMob.canInteractWith}:
     * {@code ent.isDead ? false : player.getDistanceSqToEntity(ent) <= 64.0}
     * (64.0 is the SQUARED distance, the original's {@code func_70068_e}).
     */
    @Override
    public boolean stillValid(final Player playerCheck) {
        return this.golem != null
                && this.golem.isAlive()
                && playerCheck.distanceToSqr(this.golem) <= 64.0;
    }

    /**
     * The number of marker-colour slots the golem exposes — the original's
     * {@code mobInv.slotCount}, read through the golem's own colour array.
     *
     * <p>The two are the same number by construction in the original:
     * {@code EntityGolemBase.setupGolemInventory()} resizes {@code colors} to
     * {@code this.inventory.slotCount} ({@code EntityGolemBase.java:172-180}),
     * and {@code ContainerGolem.bindGolemInventory()} pages that same slot
     * count. {@code InventoryMob}'s per-core sizing is the runtime card's
     * ({@code setupGolemInventory} is not ported — TC-M6-08-s-b2, t_03c5d088),
     * so the colour array is the authoritative count here: the runtime
     * initialises it to {@code new byte[]{-1}}
     * ({@code EntityGolemBase.java:113}, ported verbatim), which is one
     * wildcard colour slot until a core resizes it. Every colour button this
     * menu accepts is therefore a real slot of the golem's own array.</p>
     */
    public int markerColorSlotCount() {
        return this.golem == null ? 0 : this.golem.getColors().length;
    }

    /** The player inventory this menu bound (original {@code playerInv} field). */
    public Inventory playerInventory() {
        return this.playerInventory;
    }
}
