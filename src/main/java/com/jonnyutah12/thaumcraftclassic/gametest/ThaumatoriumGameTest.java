package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.menus.ThaumatoriumMenu;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogThaumatorium;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.tiles.TileThaumatorium;
import com.jonnyutah12.thaumcraftclassic.tiles.TileThaumatoriumTop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M7-04-m2c acceptance: the thaumatorium window — the device metas' tiles,
 * the menu's original slot order and anchors, the queue click and its
 * no-cap server rule, the original's queued-recipe listing quirk,
 * {@code refreshIndex}, the synced aspect display (colours, prices, stored
 * essentia, the uncapped bar formula), the {@code maxRecipes} gate the
 * original's client hitbox enforced, the tiles' persistence, and — since
 * TC-M7-04-m2e — the aspect identity the display channel carries for the tag
 * sprite ({@code theAspectDisplayCarriesTheAspectItself}).
 *
 * <p>The probes run against live crucible recipe data: {@code recipe.7.crucible}
 * (research {@code ALCHEMICALDUPLICATION}, catalyst gunpowder, ignis+perditio
 * 4+4) and {@code recipe.9.crucible} (same research, catalyst clay ball) —
 * one research, two catalysts, so the catalyst gate, the queued-recipe branch
 * and the two-page-free queue are all reachable. The visible list's order is
 * the {@code RecipeManager}'s own iteration order (measured in the harness:
 * {@code recipe.9.crucible} before {@code recipe.7.crucible}), so the probes
 * derive list positions from the live menu rather than assuming a sort.</p>
 *
 * <p>As with every GUI row, rendered pixels are not proved here — the harness
 * is a dedicated server and never fires {@code RegisterMenuScreensEvent}
 * (docs/BEHAVIORAL_DEVIATIONS.md BD-19).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumatoriumGameTest {

    private ThaumatoriumGameTest() {
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private static final ResourceLocation RECIPE_7 = id("recipe.7.crucible");
    private static final ResourceLocation RECIPE_9 = id("recipe.9.crucible");
    private static final String RESEARCH = "ALCHEMICALDUPLICATION";

    // --- helpers ---------------------------------------------------------------

    private static Block metalDevice() {
        final Block block = BuiltInRegistries.BLOCK.get(id("blockmetaldevice"));
        helperCheck(block != Blocks.AIR, "blockmetaldevice is not registered");
        return block;
    }

    private static void helperCheck(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /** Place the thaumatorium meta (10) at (1,1,1) and hand back its entity. */
    private static TileThaumatorium thaumatoriumAt(final GameTestHelper helper) {
        final BlockState state = ((BlockDeviceBase) metalDevice()).withVariant(10);
        helper.setBlock(1, 1, 1, state);
        final BlockEntity entity = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(entity instanceof TileThaumatorium,
                "blockmetaldevice meta 10 must create its TileThaumatorium entity (BlockMetalDevice.java:366)");
        return (TileThaumatorium) entity;
    }

    /** A mock player whose TC-M3-02 knowledge has the listed research done. */
    private static Player playerWithResearch(final GameTestHelper helper,
                                             final List<String> keys) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final String key : keys) {
            knowledge = knowledge.withResearchCompleted(key);
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        return player;
    }

    private static ThaumatoriumMenu menuFor(final Player player, final TileThaumatorium tile) {
        return new ThaumatoriumMenu(1, player.getInventory(), tile);
    }

    private static void assertAnchor(final GameTestHelper helper, final ThaumatoriumMenu menu,
                                     final int index, final int x, final int y, final String what) {
        final Slot slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                what + " slot anchor: the menu says (" + slot.x + "," + slot.y + "), the original's "
                        + "ContainerThaumatorium and client/ContainerLayout both say (" + x + "," + y + ")");
    }

    /** Position of a recipe id in the menu's live list, or -1. */
    private static int positionOf(final List<ThaumatoriumMenu.VisibleRecipe> visible,
                                  final ResourceLocation id) {
        for (int a = 0; a < visible.size(); a++) {
            if (visible.get(a).id().equals(id)) {
                return a;
            }
        }
        return -1;
    }

    /** The list's ids, for failure messages. */
    private static String describe(final List<ThaumatoriumMenu.VisibleRecipe> visible) {
        final StringBuilder sb = new StringBuilder("[");
        for (final var recipe : visible) {
            sb.append(recipe.id().getPath()).append(' ');
        }
        return sb.append(']').toString();
    }

    // --- probes ----------------------------------------------------------------

    /**
     * The device metas 10/11 create their tiles (the original's
     * {@code createTileEntity}), the base tile is the menu provider the block's
     * generic device seam opens, its title is the gui row's lang key, and the
     * top tile finds the base below it — the original's
     * {@code onBlockActivated(x, y - 1, z)} test. The open-screen packet needs
     * a real {@code ServerPlayer}; like every GUI row, the wire half is not
     * exercised by the mock player.
     */
    @GameTest(template = "empty")
    public static void deviceTilesAndMenuProviderAreWired(final GameTestHelper helper) {
        final TileThaumatorium base = thaumatoriumAt(helper);
        final BlockState topState = ((BlockDeviceBase) metalDevice()).withVariant(11);
        helper.setBlock(1, 2, 1, topState);
        final BlockEntity top = helper.getBlockEntity(new BlockPos(1, 2, 1));
        helper.assertTrue(top instanceof TileThaumatoriumTop,
                "meta 11 must create TileThaumatoriumTop (BlockMetalDevice.java:370)");
        helper.assertTrue(top instanceof BlockDeviceBase.DeviceUseTile,
                "the top tile carries the block's use seam that opens the base's window");
        helper.assertTrue(TileThaumatoriumTop.baseThaumatorium(helper.getLevel(),
                        helper.absolutePos(new BlockPos(1, 2, 1))) == base,
                "the top must resolve the thaumatorium one block below, as the original did");

        helper.assertTrue(base instanceof net.minecraft.world.MenuProvider,
                "the base tile is the MenuProvider the device block's generic seam opens");
        final var title = base.getDisplayName().getContents();
        helper.assertTrue(title instanceof TranslatableContents contents
                        && "container.thaumcraft.gui_thaumatorium".equals(contents.getKey()),
                "the menu title must be the translatable key the gui row ships, got " + title);
        final Player player = playerWithResearch(helper, List.of());
        helper.assertTrue(base.createMenu(7, player.getInventory(), player)
                        instanceof ThaumatoriumMenu,
                "the provider must build the menu the registered MenuType names");
        helper.assertTrue(CatalogThaumatorium.MENU_THAUMATORIUM.getId().getPath()
                        .equals("gui_thaumatorium"),
                "the menu type is registered under the content-mapping.csv gui row id");
        helper.succeed();
    }

    /**
     * The original container's slot order — catalyst at (48,16), player grid
     * at (8,84), hotbar at (8,142) on the 176x166 default panel — with the
     * port's one addition: the hidden output-preview slot at the original's
     * draw box, framed by nothing ({@code isActive()} false).
     */
    @GameTest(template = "empty")
    public static void menuSlotsAreTheOriginalsOrderAndAnchors(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of());
        final ThaumatoriumMenu menu = menuFor(player, tile);

        helper.assertTrue(menu.slots.size() == ThaumatoriumMenu.SLOT_COUNT,
                "the menu carries 38 slots (1 catalyst + 1 preview + 27 + 9), got "
                        + menu.slots.size());
        assertAnchor(helper, menu, ThaumatoriumMenu.SLOT_CATALYST, 48, 16, "catalyst");
        assertAnchor(helper, menu, ThaumatoriumMenu.PLAYER_FIRST, 8, 84, "player grid first");
        assertAnchor(helper, menu, ThaumatoriumMenu.PLAYER_FIRST + 8, 8 + 8 * 18, 84,
                "player grid row 0 last");
        assertAnchor(helper, menu, ThaumatoriumMenu.PLAYER_FIRST + 26, 8 + 8 * 18, 84 + 2 * 18,
                "player grid last");
        assertAnchor(helper, menu, ThaumatoriumMenu.HOTBAR_FIRST, 8, 142, "hotbar first");
        assertAnchor(helper, menu, ThaumatoriumMenu.HOTBAR_FIRST + 8, 8 + 8 * 18, 142, "hotbar last");

        final Slot preview = menu.slots.get(ThaumatoriumMenu.SLOT_PREVIEW);
        assertAnchor(helper, menu, ThaumatoriumMenu.SLOT_PREVIEW, 112, 16, "output preview");
        helper.assertTrue(!preview.isActive(),
                "the preview slot must draw and click as nothing — the original had no slot there");
        helper.assertTrue(menu.slots.get(ThaumatoriumMenu.SLOT_CATALYST).container == tile,
                "the catalyst slot is the tile's own (the original's Slot(tileEntity, 0, 48, 16))");

        // Empty tile, unlettered player: nothing visible, and the preview is empty.
        menu.broadcastChanges();
        helper.assertTrue(menu.getVisibleCount() == 0 && menu.getPreviewStack().isEmpty(),
                "no catalyst and no research means no list and no output preview");
        helper.succeed();
    }

    /**
     * The list is the original's {@code updateRecipes}: research AND catalyst
     * gate it; the click is the original's {@code handleWindowClick}: it
     * appends the recipe's aspect copy, the queuer's name, resets
     * {@code currentCraft}, and the server has <b>no</b> {@code maxRecipes}
     * cap (the 1.7.10 client hitbox — the port's screen — was the gate). The
     * display syncs colours, prices and the queue state the screen draws.
     */
    @GameTest(template = "empty")
    public static void theQueueClickIsTheOriginalsNoCapToggle(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of(RESEARCH));
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));

        final ThaumatoriumMenu menu = menuFor(player, tile);
        menu.broadcastChanges();
        helper.assertTrue(menu.getVisibleCount() == 1
                        && menu.visibleRecipes().get(0).id().equals(RECIPE_7),
                "researched and catalysed, recipe.7 must be the one visible recipe");
        helper.assertTrue(menu.getPreviewStack().is(Items.GUNPOWDER),
                "the synced preview is recipe.7's result — the original drew it from the container's list");
        helper.assertTrue(menu.getAspectCount() == 2, "recipe.7 has ignis + perditio");
        helper.assertTrue(menu.getMaxRecipes() == 1, "the base cap is the original's 1");
        helper.assertTrue(menu.getQueueSize() == 0 && menu.isQueueable(),
                "an empty queue is queueable — the 1.7.10 hitbox gate (size < max)");

        // The click: buttons index the visible list, as the raw window button did.
        menu.clickMenuButton(player, 0);
        menu.broadcastChanges();
        helper.assertTrue(tile.queue().size() == 1
                        && tile.queue().get(0).recipeId().equals(RECIPE_7)
                        && tile.currentCraft() == -1,
                "the queue append carries the recipe and resets currentCraft (ContainerThaumatorium.java:88-96)");
        helper.assertTrue(tile.queue().get(0).player().equals(player.getGameProfile().getName()),
                "the queuer is the clicking player's name (the original's recipePlayer)");
        helper.assertTrue(tile.queue().get(0).aspects().getAmount(Aspect.getAspect("ignis")) == 4,
                "the queued cost is a COPY of the recipe's aspects (aspects.copy())");
        helper.assertTrue(menu.getQueueSize() == 1 && menu.isCurrentQueued(),
                "the queue size and the queued flag sync to the screen");
        helper.assertTrue(menu.isQueueable(),
                "a queued recipe stays queueable so its click can toggle it back off");
        // The bars: stored 0 before the machine card fills the buffer; the
        // screen's fill math is the original's, uncapped.
        helper.assertTrue(menu.getDisplayNeed(0) == 4 && menu.getDisplayHave(0) == 0,
                "the price syncs as 4 and the tile's empty buffer as 0");
        helper.assertTrue(menu.getDisplayColour(0) == Aspect.getAspect("ignis").getColor(),
                "the tag colour is the aspect's registered colour, synced by the menu");

        // The toggle-off: same click, entry gone, currentCraft reset.
        menu.clickMenuButton(player, 0);
        menu.broadcastChanges();
        helper.assertTrue(tile.queue().isEmpty() && menu.getQueueSize() == 0,
                "clicking a queued recipe removes it (the original removed all three parallel entries)");

        // The server's no-cap rule: a full queue (1) accepts a second entry
        // because the original server never checked maxRecipes; only the
        // client-side gate (the port's screen, reading DATA_QUEUEABLE) refused.
        tile.essentia().add(Aspect.getAspect("ignis"), 5);
        menu.broadcastChanges();
        helper.assertTrue(menu.getDisplayHave(0) == 5
                        && ContainerLayout.thaumatoriumBarFill(5, 4) == 15,
                "the stored amount syncs, and the bar fill is the original's uncapped (int)(have/need*12) — 15 runs past the 14 px background");
        menu.clickMenuButton(player, 0);
        menu.broadcastChanges();
        helper.assertTrue(tile.queue().size() == 1, "queue refilled for the cap probes");
        helper.succeed();
    }

    /**
     * TC-M7-04-m2e: the display channel carries the <b>aspect itself</b>, which
     * is what {@code UtilsFX.drawTag} needs and what a colour int cannot give it.
     * The tag's sprite is {@code aspect.getImage()}
     * ({@code aspect/Aspect.java:105-109} →
     * {@code thaumcraft:textures/aspects/<tag>.png}), so the screen cannot bind
     * the original's glyph from an RGB value; the menu syncs the aspect's index
     * in {@link Aspect#values()} (the registry's declaration order) and the
     * screen resolves it back. This probe pins that seam at the only level the
     * harness can reach: the identity that crosses the wire, the sprite path the
     * renderer will ask the resource provider for, the empty column's
     * {@code -1}, and the out-of-range index degrading to "no tag" instead of
     * throwing (BD-19: the pixels stay unproved — the harness is a dedicated
     * server — so this is the arithmetic half of the glyph swap).
     */
    @GameTest(template = "empty")
    public static void theAspectDisplayCarriesTheAspectItself(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of(RESEARCH));
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));

        final ThaumatoriumMenu menu = menuFor(player, tile);
        menu.broadcastChanges();
        helper.assertTrue(menu.getVisibleCount() == 1 && menu.getAspectCount() == 2,
                "recipe.7 (ignis + perditio) is the visible recipe with two aspects");
        helper.assertTrue(ThaumatoriumMenu.DATA_COUNT
                        == ThaumatoriumMenu.DATA_DISPLAY
                                + ContainerLayout.TAU_VISIBLE_ASPECTS
                                        * ThaumatoriumMenu.DATA_DISPLAY_STRIDE,
                "the data channel is 8 + 6 columns of (colour, needed, stored, aspect index) — "
                        + "the fourth field is the aspect's identity, added for the tag sprite");
        // The stride is the layout, not a comment: the LAST data slot is column
        // 5's index field, so writing it and reading it back proves the channel
        // is laid out as the screen reads it (no off-by-one, no truncation).
        menu.setData(ThaumatoriumMenu.DATA_COUNT - 1, 5);
        helper.assertTrue(menu.getDisplayAspectIndex(5) == 5
                        && menu.getDisplayAspect(5) == Aspect.values().get(5),
                "data slot " + (ThaumatoriumMenu.DATA_COUNT - 1) + " is column 5's aspect index");
        menu.broadcastChanges();

        // The identity: the resolved aspect is the registry's own object, not a
        // copy, so the renderer's getImage()/getColor() calls see what the server
        // saw. Aspect.values() is declaration-ordered on both sides, so the
        // index is the same number on the server and on any client of this code.
        final Aspect ignis = Aspect.getAspect("ignis");
        final Aspect resolved = menu.getDisplayAspect(0);
        helper.assertTrue(resolved != null && resolved == ignis,
                "column 0 resolves to the registry's ignis object, not a stand-in: " + resolved);
        helper.assertTrue(Aspect.values().get(menu.getDisplayAspectIndex(0)) == resolved,
                "the synced index round-trips through Aspect.values()");
        helper.assertTrue(resolved.getImage().equals(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                                "textures/aspects/ignis.png")),
                "the sprite the renderer will ask for is the mapped overlay path "
                        + "(docs/asset-mapping.csv maps textures/aspects/* to themselves, and "
                        + "OriginalTextureOverlayTest.KEPT_DIRS keeps 'aspects' unrenamed)");
        helper.assertTrue(menu.getDisplayAspect(1) == Aspect.getAspect("perditio"),
                "column 1 is the recipe's second sorted aspect");
        helper.assertTrue(menu.getDisplayAspect(0).getColor() == menu.getDisplayColour(0),
                "the colour the bar tints with is the same aspect's colour — one source, no drift");

        // The empty columns: -1 and null, never an exception and never a drawn
        // guess (the original's `if (aspect != null)` guard, UtilsFX.java:815).
        for (int pos = 2; pos < ContainerLayout.TAU_VISIBLE_ASPECTS; pos++) {
            helper.assertTrue(menu.getDisplayAspectIndex(pos) == -1 && menu.getDisplayAspect(pos) == null,
                    "an empty column syncs -1 and resolves to null at pos " + pos);
        }
        // An index this client's registry does not know (an older client, a
        // trimmed pack) degrades to "no tag drawn".
        menu.setData(ThaumatoriumMenu.DATA_DISPLAY
                + 3 * ThaumatoriumMenu.DATA_DISPLAY_STRIDE + 3, 9999);
        helper.assertTrue(menu.getDisplayAspect(3) == null,
                "an out-of-range aspect index resolves to null, not to an exception");
        menu.setData(ThaumatoriumMenu.DATA_DISPLAY
                + 3 * ThaumatoriumMenu.DATA_DISPLAY_STRIDE + 3, -5);
        helper.assertTrue(menu.getDisplayAspect(3) == null, "a negative index resolves to null too");

        // Every registered aspect travels this channel: the thaumatorium can
        // display any of them, so a hole in the channel is a tag that silently
        // stops drawing.
        final List<Aspect> all = Aspect.values();
        helper.assertTrue(all.size() == 48, "the registry has the original's 48 aspects");
        for (final Aspect aspect : all) {
            final int index = all.indexOf(aspect);
            helper.assertTrue(index >= 0 && all.get(index) == aspect,
                    "aspect " + aspect.getName() + " must round-trip through its index");
            helper.assertTrue(aspect.getImage().getPath().startsWith("textures/aspects/"),
                    "every tag's sprite is in the directory the overlay keeps unrenamed");
        }
        helper.succeed();
    }

    /**
     * The original's listing quirk and {@code refreshIndex}: a queued recipe
     * stays listed when its catalyst is gone (the {@code updateRecipes}
     * {@code recipeHash != null} branch), the list's size change runs the
     * index jump to the queued entry, and the counter state the screen draws
     * is {@code queueSize}/{@code maxRecipes} with the queueability gate shut
     * at the cap.
     */
    @GameTest(template = "empty")
    public static void queuedRecipesStayListedAndTheIndexJumpsToThem(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of(RESEARCH));

        // Clay catalyst: recipe.9 (catalyst clay ball) is the visible one.
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.CLAY_BALL));
        final ThaumatoriumMenu menu = menuFor(player, tile);
        menu.broadcastChanges();
        helper.assertTrue(menu.getVisibleCount() == 1
                        && menu.visibleRecipes().get(0).id().equals(RECIPE_9),
                "with a clay-ball catalyst only recipe.9 passes the catalyst gate");
        menu.clickMenuButton(player, 0);

        // Swap the catalyst to gunpowder: recipe.7 joins by gate, recipe.9
        // stays ONLY through the queued branch. The size change (1 -> 2) fires
        // refreshIndex, which jumps the index to the queued entry.
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        menu.broadcastChanges();
        helper.assertTrue(menu.getVisibleCount() == 2,
                "7 matches the catalyst and 9 stays listed as queued");
        // refreshIndex jumps to the QUEUED recipe — the position it holds in the
        // live list. That position is the RecipeManager's own iteration order
        // (measured in the harness: recipe.9.crucible before recipe.7.crucible),
        // not an id sort, so the probe derives it instead of assuming it. The
        // parity contract is the JUMP and the aspect-window reset.
        final int queuedPos = positionOf(menu.visibleRecipes(), RECIPE_9);
        helper.assertTrue(queuedPos >= 0,
                "the queued recipe stays listed after its catalyst is swapped away: "
                        + describe(menu.visibleRecipes()));
        helper.assertTrue(menu.getIndex() == queuedPos && menu.getStartAspect() == 0,
                "refreshIndex jumped to the queued recipe (index " + menu.getIndex() + " for pos "
                        + queuedPos + ", list " + describe(menu.visibleRecipes())
                        + ") and reset the aspect window (GuiThaumatorium.java:35-46)");
        helper.assertTrue(menu.isCurrentQueued() && menu.isQueueable(),
                "the current recipe is queued, so it is toggleable (the original's gate)");
        helper.assertTrue(menu.getQueueSize() == 1 && menu.getMaxRecipes() == 1,
                "the queue counter state is 1/1");

        // The toggle is a toggle: clicking the queued entry's row again clears
        // it (the original's found-branch removes the parallel-list entry and
        // resets currentCraft). The position used is the derived one, never a
        // hard-coded row.
        menu.clickMenuButton(player, queuedPos);
        menu.broadcastChanges();
        helper.assertTrue(tile.queue().isEmpty(), "the queued entry cleared");
        helper.assertTrue(menu.getVisibleCount() == 1
                        && menu.visibleRecipes().get(0).id().equals(RECIPE_7)
                        && menu.getIndex() == 0 && !menu.isCurrentQueued(),
                "the list drops to the gunpowder match alone and the clamp keeps the index on it: "
                        + describe(menu.visibleRecipes()));
        helper.assertTrue(menu.isQueueable(),
                "an empty queue leaves the row queueable (queueSize 0 < maxRecipes 1)");

        menu.clickMenuButton(player, 0); // queue the gunpowder recipe
        menu.broadcastChanges();
        helper.assertTrue(menu.getIndex() == 0 && menu.isCurrentQueued()
                        && menu.getQueueSize() == 1 && menu.getMaxRecipes() == 1,
                "the queue is 1/1 at the base cap, and the index stays on the row it queued");

        // The cap gate the original's client hitbox enforced, proved on the row
        // it bites: swap the catalyst so the unqueued recipe is listed, page the
        // index onto it, and the synced gate must close (queue full at the base
        // cap, that row not queued). The server itself never caps (the previous
        // probe proved the no-cap rule).
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.CLAY_BALL));
        menu.broadcastChanges();
        final int sevenPos = positionOf(menu.visibleRecipes(), RECIPE_7);
        helper.assertTrue(menu.getVisibleCount() == 2 && sevenPos >= 0
                        && positionOf(menu.visibleRecipes(), RECIPE_9) >= 0,
                "the queued row stays listed with its catalyst gone: "
                        + describe(menu.visibleRecipes()));
        helper.assertTrue(menu.getIndex() == sevenPos && menu.isCurrentQueued() && menu.isQueueable(),
                "refreshIndex jumps onto the queued row, which the cap never blocks");
        final int otherPos = sevenPos == 0 ? 1 : 0;
        menu.clickMenuButton(player, otherPos > menu.getIndex()
                ? ThaumatoriumMenu.BUTTON_NEXT_RECIPE : ThaumatoriumMenu.BUTTON_PREV_RECIPE);
        menu.broadcastChanges();
        helper.assertTrue(menu.getIndex() == otherPos && !menu.isCurrentQueued() && !menu.isQueueable(),
                "at the cap an unqueued recipe is not queueable (index " + menu.getIndex()
                        + " of " + describe(menu.visibleRecipes()) + ")");
        helper.succeed();
    }

    /**
     * The {@code getUpgrades} scan is the original's shape on the port's
     * neighbour seam: base cap 1 (the Mnemonic Matrix clause can never count —
     * TileBrainbox is not ported, BD-19), and the queue trim fires only when
     * the cap CHANGES (the original's {@code if (mr != maxRecipes)}), never by
     * the scan's own hand.
     */
    @GameTest(template = "empty")
    public static void getUpgradesKeepsTheBaseCapUntilMatricesLand(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of(RESEARCH));
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        final ThaumatoriumMenu menu = menuFor(player, tile);
        menu.broadcastChanges();
        menu.clickMenuButton(player, 0);
        helper.assertTrue(tile.queue().size() == 1, "queued for the cap probe");

        // Vanilla's fill rule (1.21.1's AbstractContainerMenu.getRedstoneSignalFromContainer,
        // the live form of the original's Container.getRedstoneSignalValue): one
        // item in a 64-limit slot is 1, a full stack is 15, empty is 0.
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        helper.assertTrue(tile.getComparatorSignal() == 1,
                "one item in the 64-limit catalyst slot reads 1 through the vanilla fill rule");
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER, 64));
        helper.assertTrue(tile.getComparatorSignal() == 15,
                "a full stack reads 15 (the original's Container.getRedstoneSignalValue)");
        tile.setItem(TileThaumatorium.SLOT_INPUT, ItemStack.EMPTY);
        helper.assertTrue(tile.getComparatorSignal() == 0, "an empty container reads 0");

        tile.deviceNeighborChanged(); // the seam BlockDeviceBase fires on neighbour change
        menu.broadcastChanges();
        helper.assertTrue(tile.maxRecipes() == 1,
                "no brainbox tile exists, so the scan stays at the base 1");
        helper.assertTrue(tile.queue().size() == 1,
                "the queue trims only when the cap CHANGES (the original's if (mr != maxRecipes))");
        helper.succeed();
    }

    /**
     * The tile round-trips the original's key set: {@code maxrec}, the
     * essentia list under {@code Aspects}, the catalyst under {@code Items}
     * slot 0, and the queue under {@code recipes} + {@code OutputPlayer} (plus
     * the index-aligned cost copies under {@code QueueAspects}) — the port's
     * id-string form; BD-19 replaces the original's int-hash arrays. The load
     * is exercised on 1.21.1's REAL path, where the block entity has no level
     * yet, and on the drops the original's registry lookup produced.
     */
    @GameTest(template = "empty")
    public static void theTilesSaveAndLoadTheOriginalKeys(final GameTestHelper helper) {
        final TileThaumatorium tile = thaumatoriumAt(helper);
        final Player player = playerWithResearch(helper, List.of(RESEARCH));
        tile.setItem(TileThaumatorium.SLOT_INPUT, new ItemStack(Items.GUNPOWDER, 3));
        final ThaumatoriumMenu menu = menuFor(player, tile);
        menu.broadcastChanges();
        menu.clickMenuButton(player, 0);
        tile.essentia().add(Aspect.getAspect("perditio"), 6);

        final var tag = tile.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tag.contains("maxrec") && tag.contains("recipes")
                        && tag.contains("OutputPlayer") && tag.contains("Items"),
                "the saved tag carries the original's keys (facing/maxrec/Aspects/Items/recipes/OutputPlayer)");
        helper.assertTrue(tag.getList("recipes", net.minecraft.nbt.Tag.TAG_STRING).size() == 1,
                "the queue round-trips (id-string form, BD-19)");

        tile.clearContent();
        tile.queue().clear();
        helper.assertTrue(tile.getItem(0).isEmpty() && tile.queue().isEmpty(), "state cleared for the load");
        tile.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(tile.getItem(0).is(Items.GUNPOWDER) && tile.getItem(0).getCount() == 3,
                "the catalyst returns from the Items/Slot 0 form");
        helper.assertTrue(tile.queue().size() == 1
                        && tile.queue().get(0).recipeId().equals(RECIPE_7)
                        && tile.queue().get(0).player().equals(player.getGameProfile().getName()),
                "the queue and its queuer name return");
        helper.assertTrue(tile.essentia().getAmount(Aspect.getAspect("perditio")) == 6,
                "the essentia buffer returns from the Aspects label");

        // The real load path: 1.21.1 builds a block entity through
        // BlockEntity.loadStatic, which runs loadWithComponents BEFORE the
        // level is set, so a queue rebuilt from the RecipeManager at load time
        // — the original's rule — would empty on every world load. The port
        // keeps the cost as data (BD-19), so a level-less load keeps the queue.
        final TileThaumatorium reloaded = new TileThaumatorium(BlockPos.ZERO,
                helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(1, 1, 1))));
        reloaded.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(!reloaded.hasLevel(), "the real load path has no level yet");
        helper.assertTrue(reloaded.queue().size() == 1
                        && reloaded.queue().get(0).recipeId().equals(RECIPE_7)
                        && reloaded.queue().get(0).aspects().getAmount(Aspect.getAspect("ignis")) == 4
                        && reloaded.queue().get(0).player().equals(player.getGameProfile().getName()),
                "a level-less load keeps the queue, its cost copy and its queuer");
        helper.assertTrue(reloaded.getItem(0).is(Items.GUNPOWDER)
                        && reloaded.getItem(0).getCount() == 3,
                "the catalyst returns on that same level-less path");

        // The drops the original had, in id form: an unparsable id vanishes.
        final net.minecraft.nbt.CompoundTag junk = tile.saveWithFullMetadata(
                helper.getLevel().registryAccess());
        junk.getList("recipes", net.minecraft.nbt.Tag.TAG_STRING)
                .add(net.minecraft.nbt.StringTag.valueOf("not a resource location"));
        tile.queue().clear();
        tile.loadWithComponents(junk, helper.getLevel().registryAccess());
        helper.assertTrue(tile.queue().size() == 1
                        && tile.queue().get(0).recipeId().equals(RECIPE_7),
                "the unparsable id is dropped, as the original's null-hash entry was");

        // A save without the stored costs (the pre-QueueAspects form) keeps the
        // original's lookup-and-drop through a live level: recipe.7 resolves,
        // a stale id does not, and that entry vanishes.
        final net.minecraft.nbt.CompoundTag legacy = tile.saveWithFullMetadata(
                helper.getLevel().registryAccess());
        legacy.remove("QueueAspects");
        legacy.getList("recipes", net.minecraft.nbt.Tag.TAG_STRING)
                .add(net.minecraft.nbt.StringTag.valueOf("thaumcraft:recipe.999.crucible"));
        tile.queue().clear();
        tile.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(tile.queue().size() == 1
                        && tile.queue().get(0).recipeId().equals(RECIPE_7)
                        && tile.queue().get(0).aspects().getAmount(Aspect.getAspect("ignis")) == 4,
                "the cost-less form re-resolves recipe.7 from the recipe and drops the unknown id");
        helper.succeed();
    }
}
