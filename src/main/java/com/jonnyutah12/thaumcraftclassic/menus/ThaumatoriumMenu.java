package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.tiles.TileThaumatorium;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Thaumatorium menu (content-mapping.csv {@code thaumcraft:gui_thaumatorium},
 * original gui_id 3 / {@code ContainerThaumatorium}).
 *
 * <p><b>Slot order and anchors are the original's, verbatim</b>
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/container/ContainerThaumatorium.java}):
 * menu slot 0 is the tile's single catalyst slot at {@code (48,16)}, then the
 * player grid at {@code (8 + col*18, 84 + row*18)} and the hotbar at
 * {@code (8 + i*18, 142)} — the vanilla 176x166 panel the original's default
 * {@code GuiContainer} size drew. The only slot the original did not have is
 * menu slot 1, {@link #SLOT_PREVIEW}: the 1.7.10 window drew the current
 * recipe's output straight from the container's client-side recipe list
 * ({@code GuiThaumatorium.drawOutput} reads {@code recipes.get(index)
 * .getRecipeOutput()}), and 1.21.1 hands a screen only its menu — so the
 * output rides in a slot the screen never draws
 * ({@link Slot#isActive()} is false: no frame, no item, no click path) and
 * vanilla's own container sync carries the stack. The anchors come from
 * {@link ContainerLayout}, shared with the screen.</p>
 *
 * <p><b>The recipe list is the original's {@code updateRecipes}, on the
 * server.</b> The original container ran on both sides and both sides rebuilt
 * the list from the player's research and the tile's catalyst
 * ({@code isResearchComplete && catalystMatches}, else the queued-recipe
 * branch that keeps a queued recipe listed even with the catalyst gone);
 * 1.21.1 computes it server-side — where the tile, the research attachment
 * and the crucible {@code RecipeManager} live — and ships it to the screen as
 * 32 data slots: 0 visible count, 1 the page index (the original's client-side
 * {@code index}), 2 the aspect-window start (the original's
 * {@code startAspect}), 3 queue size, 4 the {@code maxRecipes} cap, 5 the
 * current recipe's aspect count, 6 whether the current recipe is queued, 7 the
 * queueability gate (the original's client hitbox test
 * {@code queueSize < maxRecipes || queued}), and 8..31 six
 * (colour, needed, stored, <b>aspect index</b>) columns for the tag row and
 * its essentia bars. The fourth field is what the tag's <i>sprite</i> needs:
 * {@code UtilsFX.drawTag} binds {@code aspect.getImage()}, and an RGB int
 * cannot name that file — the channel is the aspect's index in
 * {@link Aspect#values()} (the registry's declaration order, identical on both
 * sides), {@code -1} for an empty column. The screen draws these numbers,
 * never a guess; anything not synced is not drawn (BD-19).</p>
 *
 * <p><b>The queue click is the original's.</b> {@link #clickMenuButton}
 * buttons {@code 0..N-1} address the visible list by index exactly as
 * {@code handleWindowClick} did and toggle the tile's queue with the
 * original's no-cap server rule (the 1.7.10 client gated the hitbox; the
 * port's gate is in the screen, reading slot 7). Buttons 1000..1003 page
 * index/aspects — the original paged in the GUI's own fields, invisible to
 * the server; the port pages in menu state so the synced slots describe one
 * truth. Index clamping and {@code refreshIndex} (jump to the first queued
 * recipe, reset the aspect window) run server-side, where the original's
 * client ran them on open and on size change.</p>
 */
public class ThaumatoriumMenu extends AbstractContainerMenu {

    /** Menu slot of the tile's catalyst slot (the original's {@code Slot(tileEntity, 0, 48, 16)}). */
    public static final int SLOT_CATALYST = 0;
    /** The hidden output-preview slot; {@link Slot#isActive()} is false. */
    public static final int SLOT_PREVIEW = 1;
    /** Player grid starts at menu slot 2, the hotbar at 29. */
    public static final int PLAYER_FIRST = 2;
    public static final int HOTBAR_FIRST = PLAYER_FIRST + 27;
    public static final int SLOT_COUNT = PLAYER_FIRST + 36;

    /**
     * Menu-button protocol: {@code 0..N-1} toggle the recipe at that list
     * index (the original's raw {@code handleWindowClick(button)}); the pager
     * ids are the port's (the original's pager was pure client state).
     */
    public static final int BUTTON_PREV_RECIPE = 1000;
    public static final int BUTTON_NEXT_RECIPE = 1001;
    public static final int BUTTON_ASPECT_LEFT = 1002;
    public static final int BUTTON_ASPECT_RIGHT = 1003;

    /** Data-slot layout, as documented on the class. */
    public static final int DATA_VISIBLE_COUNT = 0;
    public static final int DATA_INDEX = 1;
    public static final int DATA_START_ASPECT = 2;
    public static final int DATA_QUEUE_SIZE = 3;
    public static final int DATA_MAX_RECIPES = 4;
    public static final int DATA_ASPECT_COUNT = 5;
    public static final int DATA_QUEUED_CURRENT = 6;
    public static final int DATA_QUEUEABLE = 7;
    public static final int DATA_DISPLAY = 8;
    /**
     * Fields per visible aspect column: colour, needed, stored, and the aspect
     * <b>identity</b> — its index in {@link Aspect#values()}, {@code -1} for an
     * empty column. The identity is what {@code UtilsFX.drawTag} needs and the
     * colour does not give it: the tag's sprite is {@code aspect.getImage()}
     * (aspect/Aspect.java:105-109 → {@code thaumcraft:textures/aspects/<tag>.png}), so
     * a screen holding only an RGB int can tint a box but cannot name the file
     * the original bound (TC-M7-04-m2e).
     */
    public static final int DATA_DISPLAY_STRIDE = 4;
    public static final int DATA_COUNT =
            DATA_DISPLAY + ContainerLayout.TAU_VISIBLE_ASPECTS * DATA_DISPLAY_STRIDE;

    /** One visible list entry: the live recipe and its data-file id. */
    public record VisibleRecipe(ResourceLocation id, CrucibleRecipe recipe) {
    }

    private final TileThaumatorium thaumatorium;
    private final Inventory playerInventory;
    private final int[] values = new int[DATA_COUNT];
    private final List<VisibleRecipe> recipes = new ArrayList<>();

    /** The menu-owned carrier for the current recipe's output (slot 1). */
    private final net.minecraft.world.SimpleContainer preview =
            new net.minecraft.world.SimpleContainer(1);
    private int lastSize = -1;
    private int index;
    private int startAspect;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int i) {
            return ThaumatoriumMenu.this.values[i];
        }

        @Override
        public void set(final int i, final int value) {
            ThaumatoriumMenu.this.values[i] = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ThaumatoriumMenu(final int id, final Inventory playerInventory,
                            final TileThaumatorium thaumatorium) {
        super(null, id);
        this.thaumatorium = thaumatorium;
        this.playerInventory = playerInventory;

        // Original slot order: catalyst, then the player grid at (8,84) and
        // the hotbar at (8,142). The preview slot is inserted at 1 (not in the
        // original — see the class note) and hidden from the screen.
        addSlot(new Slot(thaumatorium, TileThaumatorium.SLOT_INPUT,
                ContainerLayout.TAU_CATALYST_X, ContainerLayout.TAU_CATALYST_Y));
        addSlot(new PreviewSlot(preview, 0,
                ContainerLayout.TAU_OUTPUT_X, ContainerLayout.TAU_OUTPUT_Y));
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** The original's three loops at {@code (8 + col*18, 84 + row*18)} / {@code (8,142)}. */
    private void addPlayerInventory(final Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        ContainerLayout.TAU_PLAYER_X + col * ContainerLayout.SLOT_FRAME,
                        ContainerLayout.PLAYER_GRID_TOP + row * ContainerLayout.SLOT_FRAME));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    ContainerLayout.TAU_PLAYER_X + col * ContainerLayout.SLOT_FRAME,
                    ContainerLayout.PLAYER_HOTBAR_TOP));
        }
    }

    /** The tile this menu drives. */
    public TileThaumatorium thaumatorium() {
        return this.thaumatorium;
    }

    /** The server-side visible list (the original's {@code container.recipes}). */
    public List<VisibleRecipe> visibleRecipes() {
        return List.copyOf(this.recipes);
    }

    // ------------------------------------------------------ synced display

    public int getVisibleCount() {
        return this.values[DATA_VISIBLE_COUNT];
    }

    public int getIndex() {
        return this.values[DATA_INDEX];
    }

    public int getStartAspect() {
        return this.values[DATA_START_ASPECT];
    }

    public int getQueueSize() {
        return this.values[DATA_QUEUE_SIZE];
    }

    public int getMaxRecipes() {
        return this.values[DATA_MAX_RECIPES];
    }

    /** The current recipe's aspect count (0 with no current recipe). */
    public int getAspectCount() {
        return this.values[DATA_ASPECT_COUNT];
    }

    /** Whether the current recipe is queued (the bars' draw gate). */
    public boolean isCurrentQueued() {
        return this.values[DATA_QUEUED_CURRENT] != 0;
    }

    /** The original's client hitbox gate: queueable right now. */
    public boolean isQueueable() {
        return this.values[DATA_QUEUEABLE] != 0;
    }

    /** Colour of displayed aspect column {@code pos} (0 with nothing there). */
    public int getDisplayColour(final int pos) {
        return this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE];
    }

    /** Amount the current recipe needs of displayed aspect column {@code pos}. */
    public int getDisplayNeed(final int pos) {
        return this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 1];
    }

    /** Amount the tile's essentia buffer holds of that aspect. */
    public int getDisplayHave(final int pos) {
        return this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 2];
    }

    /**
     * The synced index of displayed aspect column {@code pos} in
     * {@link Aspect#values()}, or {@code -1} when nothing is displayed there.
     */
    public int getDisplayAspectIndex(final int pos) {
        return this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 3];
    }

    /**
     * The aspect of displayed column {@code pos} — the object
     * {@code UtilsFX.drawTag} is called with, for its sprite
     * ({@code aspect.getImage()}) as well as its colour. Null when the column is
     * empty <b>or</b> the synced index is not in this client's registry: an
     * out-of-range index degrades to "no tag drawn", never to an exception and
     * never to a drawn guess (the same shape
     * {@code DeconstructionTableMenu}'s reveal channel has).
     */
    @Nullable
    public Aspect getDisplayAspect(final int pos) {
        final int index = getDisplayAspectIndex(pos);
        if (index < 0) {
            return null;
        }
        final List<Aspect> all = Aspect.values();
        return index < all.size() ? all.get(index) : null;
    }

    /** The synced output preview (the hidden slot's stack). */
    public ItemStack getPreviewStack() {
        return this.slots.get(SLOT_PREVIEW).getItem();
    }

    // ------------------------------------------------------- the cycle

    /**
     * The server's per-tick job, run from {@link #broadcastChanges()} exactly
     * where {@code ArcaneWorkbenchMenu} runs its craft cycle: rebuild the
     * visible list (the original's {@code updateRecipes}, called from
     * {@code onCraftMatrixChanged} and the constructor there; the tile nudges
     * its event handler on inventory change), run {@code refreshIndex} on the
     * original's size-change trigger, clamp, then write every display slot.
     */
    public void serverTick() {
        updateRecipes();
        if (this.recipes.size() != this.lastSize) {
            this.lastSize = this.recipes.size();
            refreshIndex();
        }
        if (this.index >= this.recipes.size()) {
            this.index = this.recipes.size() - 1;
        }
        if (this.index < 0) {
            this.index = 0;
        }
        refreshDisplay();
    }

    /** The original's {@code updateRecipes} (ContainerThaumatorium.java:51-70). */
    private void updateRecipes() {
        final var level = this.thaumatorium.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        final Player player = this.playerInventory.player;
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final ItemStack catalyst = this.thaumatorium.getItem(TileThaumatorium.SLOT_INPUT);
        final boolean queueNonEmpty = !this.thaumatorium.queue().isEmpty();
        this.recipes.clear();
        // The original's guard was `inputStack != null || recipeHash != null`;
        // neither side can be null in 1.21.1 (EMPTY, empty list), so the walk
        // always runs — the original's live behaviour.
        for (final var holder : level.getRecipeManager()
                .getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get())) {
            final CrucibleRecipe recipe = holder.value();
            if (researchComplete(knowledge, recipe) && recipe.catalyst().test(catalyst)) {
                this.recipes.add(new VisibleRecipe(holder.id(), recipe));
            } else if (queueNonEmpty && this.thaumatorium.isQueued(holder.id())) {
                // The original's quirk: a queued recipe stays listed even when
                // research or the catalyst no longer qualifies.
                this.recipes.add(new VisibleRecipe(holder.id(), recipe));
            }
        }
    }

    /**
     * The original's {@code ResearchManager.isResearchComplete(username, key)}
     * gate — the same test {@code CrucibleRecipe.researchUnlocked} runs, taken
     * through the player's knowledge attachment so it works for any player,
     * exactly as the 1.7.10 container's {@code player.getCommandString()} did.
     */
    private static boolean researchComplete(final PlayerKnowledgeData knowledge,
                                            final CrucibleRecipe recipe) {
        return recipe.research().isEmpty()
                || ResearchManager.isResearchComplete(knowledge, recipe.research());
    }

    /** The original's {@code GuiThaumatorium.refreshIndex} (GuiThaumatorium.java:35-46). */
    private void refreshIndex() {
        if (!this.thaumatorium.queue().isEmpty() && !this.recipes.isEmpty()) {
            for (int a = 0; a < this.recipes.size(); a++) {
                if (this.thaumatorium.isQueued(this.recipes.get(a).id())) {
                    this.index = a;
                    break;
                }
            }
        }
        this.startAspect = 0;
    }

    /** Write every synced slot, including the hidden preview slot's stack. */
    private void refreshDisplay() {
        this.values[DATA_VISIBLE_COUNT] = this.recipes.size();
        this.values[DATA_INDEX] = this.index;
        this.values[DATA_START_ASPECT] = this.startAspect;
        this.values[DATA_QUEUE_SIZE] = this.thaumatorium.queue().size();
        this.values[DATA_MAX_RECIPES] = this.thaumatorium.maxRecipes();

        if (this.recipes.isEmpty()) {
            this.values[DATA_ASPECT_COUNT] = 0;
            this.values[DATA_QUEUED_CURRENT] = 0;
            this.values[DATA_QUEUEABLE] = 0;
            for (int pos = 0; pos < ContainerLayout.TAU_VISIBLE_ASPECTS; pos++) {
                this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE] = 0;
                this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 1] = 0;
                this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 2] = 0;
                this.values[DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE + 3] = -1;
            }
            this.preview.setItem(0, ItemStack.EMPTY);
            return;
        }

        final VisibleRecipe current = this.recipes.get(this.index);
        final AspectList aspects = current.recipe().aspects();
        final List<Aspect> sorted = aspects.getAspectsSorted();
        final boolean queued = this.thaumatorium.isQueued(current.id());
        this.values[DATA_ASPECT_COUNT] = sorted.size();
        this.values[DATA_QUEUED_CURRENT] = queued ? 1 : 0;
        // The original's drawOutput/mouseClicked gate:
        // recipeHash.size() < maxRecipes || recipeHash.contains(hash).
        this.values[DATA_QUEUEABLE] =
                this.thaumatorium.queue().size() < this.thaumatorium.maxRecipes() || queued ? 1 : 0;
        final List<Aspect> registry = Aspect.values();
        for (int pos = 0; pos < ContainerLayout.TAU_VISIBLE_ASPECTS; pos++) {
            final int at = this.startAspect + pos;
            final int base = DATA_DISPLAY + pos * DATA_DISPLAY_STRIDE;
            if (at < sorted.size()) {
                final Aspect aspect = sorted.get(at);
                this.values[base] = aspect.getColor();
                this.values[base + 1] = aspects.getAmount(aspect);
                this.values[base + 2] = this.thaumatorium.essentia().getAmount(aspect);
                // The identity the tag draw needs: Aspect.values() is the
                // registry's declaration order (aspect/Aspect.java:119-133 — it
                // walks AspectDefinitions.all() in order), so the same index
                // resolves to the same aspect object on the server and on every
                // client running this code.
                this.values[base + 3] = registry.indexOf(aspect);
            } else {
                this.values[base] = 0;
                this.values[base + 1] = 0;
                this.values[base + 2] = 0;
                this.values[base + 3] = -1;
            }
        }
        this.preview.setItem(0, current.recipe().getResultItem(
                this.thaumatorium.getLevel().registryAccess()));
    }

    @Override
    public void broadcastChanges() {
        final var level = this.thaumatorium.getLevel();
        if (level != null && !level.isClientSide) {
            serverTick();
        }
        super.broadcastChanges();
    }

    // ------------------------------------------------------- menu buttons

    /**
     * The original's {@code handleWindowClick} for buttons in the visible-list
     * range — including its server-side lack of a {@code maxRecipes} cap (the
     * client hitbox, reproduced in the screen, was the gate) — and the port's
     * pager buttons for the fields the original kept client-side
     * ({@code index}, {@code startAspect}) with the original's click gates.
     */
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId >= 0 && buttonId < this.recipes.size()) {
            final VisibleRecipe target = this.recipes.get(buttonId);
            return this.thaumatorium.toggleQueued(target.id(), target.recipe(), player);
        }
        switch (buttonId) {
            case BUTTON_PREV_RECIPE -> {
                // The original's gate: drawn and clickable only when index > 0.
                if (this.recipes.size() > 1 && this.index > 0) {
                    this.index--;
                    return true;
                }
                return false;
            }
            case BUTTON_NEXT_RECIPE -> {
                if (this.recipes.size() > 1 && this.index < this.recipes.size() - 1) {
                    this.index++;
                    return true;
                }
                return false;
            }
            case BUTTON_ASPECT_LEFT -> {
                final int count = currentAspectCount();
                if (count > ContainerLayout.TAU_VISIBLE_ASPECTS && this.startAspect > 0) {
                    this.startAspect--;
                    return true;
                }
                return false;
            }
            case BUTTON_ASPECT_RIGHT -> {
                final int count = currentAspectCount();
                if (count > ContainerLayout.TAU_VISIBLE_ASPECTS
                        && this.startAspect < count - 1) {
                    this.startAspect++;
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private int currentAspectCount() {
        if (this.recipes.isEmpty() || this.index < 0 || this.index >= this.recipes.size()) {
            return 0;
        }
        return this.recipes.get(this.index).recipe().aspects().size();
    }

    // ---------------------------------------------------- shift-click routing

    /**
     * The original's {@code transferStackInSlot} branches
     * ({@code ContainerThaumatorium.java:105-143}), kept branch for branch on
     * the port's slot indices (the preview slot is the original's absent
     * 38th slot and follows the machine-slot branch): the catalyst shifts to
     * the hotbar, player-grid stacks shift to the hotbar, hotbar stacks shift
     * to the grid and then to the whole player, and the preview slot shifts
     * to the player.
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int slotId) {
        final Slot slot = this.slots.get(slotId);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (slotId == SLOT_CATALYST || slotId == SLOT_PREVIEW) {
            // Original par2 == 0: moveItemStackTo(stack, 28, 37) — the hotbar.
            if (!moveItemStackTo(stack, HOTBAR_FIRST, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotId >= PLAYER_FIRST && slotId < HOTBAR_FIRST) {
            // Original 1..27 (player grid): move to 28..37 (the hotbar).
            if (!moveItemStackTo(stack, HOTBAR_FIRST, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotId >= HOTBAR_FIRST && slotId < SLOT_COUNT) {
            // Original 28..36 (hotbar): to the grid (1..28), then the whole
            // player range (1..37).
            if (!moveItemStackTo(stack, PLAYER_FIRST, HOTBAR_FIRST, false)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, PLAYER_FIRST, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.thaumatorium.stillValid(player);
    }

    /** The hidden preview slot: the screen draws nothing here (BD-19). */
    private static final class PreviewSlot extends Slot {
        PreviewSlot(final net.minecraft.world.Container container, final int index,
                    final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }
    }
}
