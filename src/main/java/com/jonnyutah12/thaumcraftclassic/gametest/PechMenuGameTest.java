package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechItemEntityGoto;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import com.jonnyutah12.thaumcraftclassic.entities.monster.PechAiLogic;
import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogPech;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M7-04-m3-b probes: the pech trade window (gui_id 1 /
 * {@code thaumcraft:gui_pech}). The menu is a live menu type with the original's
 * 5-slot layout at the original's coordinates, the {@code SlotOutput} insertion
 * rule, the dice predicate from {@code GuiPech.java:38-39} synced over a
 * data slot, the original's shift-click ranges, the close-drop stamped with the
 * {@code "PechDrop"} owner, the tamed-only open gate and validity, and — for the
 * unported loot-table trade roll — silence: no invented contents (BD-73).
 *
 * <p>What these probes do <b>not</b> claim: rendered pixels (the game-test
 * server is a dedicated server and never fires {@code RegisterMenuScreensEvent}
 * — the 175x232 window, the (67,24) 25x25 dice rect and the geometry are pinned
 * by {@code PechLayoutTest} + {@code PechGuiParityTest}), and no trade roll
 * (the {@code ChestGenHooks} engine is D-007 / BD-73).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PechMenuGameTest {

    private PechMenuGameTest() {
    }

    private static EntityPech tamedPech(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final EntityPech pech = CatalogPech.PECH.get().create(level);
        final BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        pech.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(pech);                                                          // the mob lane's idiom
        pech.setTamed(true);
        return pech;
    }

    private static Player fakePlayer(final GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.CREATIVE);                                       // the golem lane's idiom
    }

    /** The menu type is {@code thaumcraft:gui_pech} (the content-mapping gui path, D-001). */
    @GameTest(template = "empty")
    public static void pechMenuTypeIsRegistered(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "gui_pech");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(id),
                "thaumcraft:gui_pech must be registered (content-mapping.csv Thaumcraft:GUI_PECH)");
        helper.assertTrue(BuiltInRegistries.MENU.get(id) == CatalogPech.GUI_PECH.get(),
                "thaumcraft:gui_pech resolves to the catalog holder");
        try {
            PechMenu.class.getConstructor(int.class, Inventory.class, RegistryFriendlyByteBuf.class);
        } catch (final NoSuchMethodException e) {
            helper.fail("the client (int, Inventory, RegistryFriendlyByteBuf) constructor is required: " + e);
        }
        helper.succeed();
    }

    /** {@code ContainerPech.java:33,37,43,48} — 41 slots at the original coordinates. */
    @GameTest(template = "empty")
    public static void pechSlotsSitAtTheOriginalCoordinates(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(menu.slots.size() == 41, "5 pech slots + 27 + 9 = 41, got " + menu.slots.size());
        assertSlotAt(helper, menu.slots.get(0), ContainerLayout.PECH_OFFER_X, ContainerLayout.PECH_OFFER_Y);      // :33
        assertSlotAt(helper, menu.slots.get(1), 106, 20);                                                          // :37
        assertSlotAt(helper, menu.slots.get(2), 124, 20);
        assertSlotAt(helper, menu.slots.get(3), 106, 38);
        assertSlotAt(helper, menu.slots.get(4), 124, 38);
        assertSlotAt(helper, menu.slots.get(5), 8, 84);                                                            // :43
        assertSlotAt(helper, menu.slots.get(31), 152, 120);
        assertSlotAt(helper, menu.slots.get(32), 8, 142);                                                          // :48
        assertSlotAt(helper, menu.slots.get(40), 152, 142);
        helper.succeed();
    }

    /** {@code SlotOutput.java} — the 2x2 grid refuses insertion; the offer accepts it. */
    @GameTest(template = "empty")
    public static void pechOutputGridRefusesInsertion(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.DIAMOND)),
                "the offer slot accepts items (ContainerPech.java:33)");
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(!menu.slots.get(a).mayPlace(new ItemStack(Items.DIAMOND)),
                    "output slot " + a + " refuses insertion (SlotOutput insertion rule)");
        }
        helper.succeed();
    }

    /** {@code GuiPech.java:38-39} — the dice predicate, and its data-slot sync. */
    @GameTest(template = "empty")
    public static void pechDicePredicateFollowsTheOriginal(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, fakePlayer(helper).getInventory(), pech);
        helper.assertTrue(!menu.diceReady(), "an empty window shows no dice");
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND));
        helper.assertTrue(menu.diceReady(), "a valued offer with an empty grid lights the dice (GuiPech.java:38-39)");
        helper.assertTrue(menu.diceDataValue() == 1, "the data slot carries the bit");
        menu.slots.get(1).set(new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(!menu.diceReady(), "a full output cell hides the dice");
        helper.assertTrue(menu.diceDataValue() == 0, "the data slot follows");
        menu.slots.get(1).set(ItemStack.EMPTY);
        menu.slots.get(0).set(new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(!menu.diceReady(), "an unvalued offer shows no dice (EntityPech.java:707-722)");
        helper.succeed();
    }

    /** {@code ContainerPech.java:54-64} — 0..4 to the player region, 5..40 to the offer only. */
    @GameTest(template = "empty")
    public static void pechShiftClickUsesTheOriginalRanges(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, 3));
        menu.quickMoveStack(player, 0);
        helper.assertTrue(menu.getItems().get(0).isEmpty(), "the offer shifts out of the pech slots (:54-58)");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.DIAMOND, 3)),
                "the offer lands in the player region");
        player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 2));
        menu.quickMoveStack(player, 5);                                                  // menu slot 5 = inv slot 9
        helper.assertTrue(ItemStack.isSameItem(menu.getItems().get(0), new ItemStack(Items.EMERALD, 2)),
                "a player slot shifts into the offer slot (:59-64)");
        for (int a = 1; a < 5; a++) {
            helper.assertTrue(menu.getItems().get(a).isEmpty(), "the output grid is never a shift destination");
        }
        helper.succeed();
    }

    /** {@code ContainerPech.java:199-205} — close drops slots 0..4 owned by {@code "PechDrop"}. */
    @GameTest(template = "empty")
    public static void pechCloseDropsStampedWithPechDrop(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        pech.trading = true;
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        for (int a = 0; a < 5; a++) {
            menu.slots.get(a).set(new ItemStack(Items.DIAMOND, a + 1));
        }
        menu.removed(player);
        helper.assertTrue(!pech.trading, "closing marks the pech not trading (:204)");
        // The drop lands at the DROPPING player - a mock player is not parked in the test
        // cell (take-2 measured 0 hits for a test-cell box), so search around the player.
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(8.0));
        int stamped = 0;
        for (final ItemEntity drop : drops) {
            // Read the marker through the goal's own reader, not a hand-rolled key: this
            // probe is the write side's contract and the AI is its only consumer
            // (AIPechItemEntityGoto.java:76-78).
            if (PechAiLogic.PECH_DROP_OWNER.equals(AIPechItemEntityGoto.pechDropOwnerOf(drop))) {
                stamped++;
            }
        }
        helper.assertTrue(stamped == 5, "all five slots drop with the PechDrop owner (:201), got " + stamped);
        helper.succeed();
    }

    /**
     * The pech drop on the <b>world-save path</b> (t_04233450). The stamp's first home
     * was the vanilla {@code minecraft:entity_data} component, whose persistent codec is
     * {@code CustomData.CODEC_WITH_ID} ({@code DataComponents.java:172};
     * {@code CustomData.java:33-35} = {@code contains("id", 8) ? success :
     * error("Missing id for entity in: " + tag)}), so every pech trade drop threw out of
     * {@code ItemStack#save} on the save path: {@code ServerLevel#save} logged
     * {@code Error saving [15 minecraft:diamond]} and the stack never reached the chunk
     * data — the pech's drop was silently lost on save/reload (186 occurrences in 53/53
     * legs at t_8b63e037's census, both accessory arms, three trees).
     *
     * <p>The arms below run the SAME frames that log line names, in order: save
     * ({@code Entity#saveWithoutId} → {@code ItemEntity#addAdditionalSaveData:333} →
     * {@code ItemStack#save}, {@code ItemStack.java:419-425} — the frame in the census
     * stack) and reload ({@code Entity#load} →
     * {@code ItemEntity#readAdditionalSaveData:341-359} → {@code ItemStack#parse}). The
     * marker is read back through {@link AIPechItemEntityGoto#pechDropOwnerOf}, the
     * goal's own reader, so a marker that survives the bytes on a key the AI never reads
     * cannot pass green (docs/testing.md#component-codec-save-path). The drops are
     * attributed by the UUID delta of the producing call
     * (docs/testing.md#attribute-not-filter): the box is the mock player's, shared
     * with the neighbours, and a foreign stack is named in the message, never counted.</p>
     */
    @GameTest(template = "empty")
    public static void pechDropSurvivesAWorldSave(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        pech.trading = true;
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        for (int a = 0; a < 5; a++) {
            menu.slots.get(a).set(new ItemStack(Items.DIAMOND, a + 1));
        }
        final AABB box = player.getBoundingBox().inflate(8.0);
        final Set<UUID> before = itemIdsIn(helper, box);
        menu.removed(player);                                                          // :205-212
        final List<ItemEntity> owned = dropsAddedSince(helper, box, before);
        helper.assertTrue(owned.size() == 5,
                "the close must produce 5 owned drops, got " + owned.size() + " [" + describe(owned)
                        + "]; stacks already in the box before the close: [" + stacksIn(helper, box, before) + "]");
        final StringBuilder broken = new StringBuilder();
        for (final ItemEntity drop : owned) {
            final ItemStack stack = drop.getItem();
            final int count = stack.getCount();
            if (!stack.is(Items.DIAMOND)) {
                broken.append("not a diamond stack: ").append(stack).append("; ");
            }
            if (!PechAiLogic.PECH_DROP_OWNER.equals(AIPechItemEntityGoto.pechDropOwnerOf(drop))) {
                broken.append("no PechDrop owner marker on ").append(stack).append("; ");
            }
            if (stack.has(DataComponents.ENTITY_DATA)) {
                broken.append(stack).append(" rides vanilla entity_data, whose codec demands an entity id; ");
            }
            final CompoundTag saved = new CompoundTag();
            try {
                drop.saveWithoutId(saved);                                             // the census's crashing frame
            } catch (final RuntimeException e) {
                helper.fail("the world-save path cannot encode the pech drop " + stack + ": " + e);
            }
            final ItemEntity back = EntityType.ITEM.create(helper.getLevel());
            back.load(saved);                                                          // the reload frame
            final ItemStack restored = back.getItem();
            if (!restored.is(Items.DIAMOND) || restored.getCount() != count) {
                broken.append("the reload lost the stack: saved ").append(stack)
                        .append(", reloaded ").append(restored).append("; ");
            }
            if (!PechAiLogic.PECH_DROP_OWNER.equals(AIPechItemEntityGoto.pechDropOwnerOf(back))) {
                broken.append("the reload lost the owner marker: ").append(restored).append("; ");
            }
            back.discard();
        }
        helper.assertTrue(broken.length() == 0, "the pech drops fail the save/reload round-trip: " + broken);
        for (final ItemEntity drop : owned) {
            drop.discard();                                                            // the level is shared
        }
        helper.succeed();
    }

    /**
     * The target-platform fact the fix rests on, pinned so a platform change that
     * removes it reports by name. {@code minecraft:entity_data}'s persistent codec
     * rejects a marker compound with no entity {@code id} (the census's signature);
     * the SAME compound plus an {@code id} encodes; and the port's own
     * {@code thaumcraft:pech_drop_owner} encodes with no id at all and round-trips the
     * literal. That is why the marker is a component of our own, and why BD-73's first
     * attempt — renaming the key away from the reserved {@code Owner} — never cured the
     * loss: the gate is on the absence of {@code id}, not on the key name.
     */
    @GameTest(template = "empty")
    public static void vanillaEntityDataNeedsAnEntityIdAndOursDoesNot(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag marker = new CompoundTag();
        marker.putString("PechDropOwner", PechAiLogic.PECH_DROP_OWNER);
        // The codec, NOT a stack save: NeoForge wraps every stack-level encode in
        // DataComponentUtil.wrapEncodingExceptions, which LOGS the failure through
        // Util#logDataComponentSaveError before rethrowing. A deliberate stack-level
        // negative arm therefore prints the exact signature this lane is scored on
        // (grep -c "Missing id for entity in" is the census gate), so the negative arm reads
        // the codec that registers the gate (CustomData.CODEC_WITH_ID,
        // DataComponents.java:172) directly. docs/testing.md#component-codec-save-path.
        final DataResult<Tag> idless =
                CustomData.CODEC_WITH_ID.encodeStart(NbtOps.INSTANCE, CustomData.of(marker));
        helper.assertTrue(idless.result().isEmpty(),
                "the census's signature must still fire: minecraft:entity_data's own codec "
                        + "ENCODED {PechDropOwner:\"PechDrop\"} with no entity id, so the gate this fix "
                        + "rests on is gone and the marker's component shape is open to review (BD-73)");
        final String cause = idless.error().map(error -> error.message()).orElse("");
        helper.assertTrue(cause.contains("Missing id for entity in"),
                "the codec must reject the marker for the missing entity id - the census's "
                        + "signature - got: " + cause);
        final CompoundTag withId = marker.copy();
        withId.putString("id", "minecraft:cow");
        final DataResult<Tag> idful =
                CustomData.CODEC_WITH_ID.encodeStart(NbtOps.INSTANCE, CustomData.of(withId));
        helper.assertTrue(idful.result().isPresent(),
                "the same codec must take the marker compound once it carries an entity id - "
                        + "the gate is the id, not the marker - got: " + idful);
        final ItemStack ours = new ItemStack(Items.DIAMOND, 15);
        ours.set(ModDataComponents.PECH_DROP_OWNER.get(), PechAiLogic.PECH_DROP_OWNER);
        ours.set(DataComponents.ENTITY_DATA, CustomData.of(withId));
        final CompoundTag oursSaved = (CompoundTag) ours.save(registries);   // a legal save: no log line
        final ItemStack oursBack = ItemStack.parseOptional(registries, oursSaved);
        helper.assertTrue(PechAiLogic.PECH_DROP_OWNER.equals(oursBack.get(ModDataComponents.PECH_DROP_OWNER.get())),
                "our own component must encode and round-trip the owner literal beside a legal "
                        + "minecraft:entity_data, got " + oursSaved);
        helper.succeed();
    }

    /** The UUIDs of every item entity in the box, captured immediately around the action. */
    private static Set<UUID> itemIdsIn(final GameTestHelper helper, final AABB box) {
        final Set<UUID> ids = new HashSet<>();
        for (final ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            ids.add(drop.getUUID());
        }
        return ids;
    }

    /** The item entities in the box that the captured set does not contain: this action's output. */
    private static List<ItemEntity> dropsAddedSince(final GameTestHelper helper, final AABB box,
            final Set<UUID> before) {
        final List<ItemEntity> owned = new ArrayList<>();
        for (final ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (!before.contains(drop.getUUID())) {
                owned.add(drop);
            }
        }
        return owned;
    }

    private static String describe(final List<ItemEntity> drops) {
        final StringBuilder out = new StringBuilder();
        for (final ItemEntity drop : drops) {
            out.append(drop.getItem()).append(" ");
        }
        return out.toString().trim();
    }

    /** The stacks that were already in the box — the diagnostic, never a counted stack. */
    private static String stacksIn(final GameTestHelper helper, final AABB box, final Set<UUID> tracked) {
        final StringBuilder out = new StringBuilder();
        for (final ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (!tracked.contains(drop.getUUID())) {
                out.append(drop.getItem()).append(" ");
            }
        }
        return out.toString().trim();
    }

    /** {@code EntityPech.java:695-698} — the open gate: server-side, tamed, not sneaking, no name tag. */
    @GameTest(template = "empty")
    public static void pechOpensOnlyUnderTheOriginalGate(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final EntityPech wild = tamedPech(helper);
        wild.setTamed(false);
        helper.assertTrue(!wild.openTradeGui(player), "an untamed pech opens no window (:696)");
        final EntityPech pech = tamedPech(helper);
        player.setShiftKeyDown(true);
        helper.assertTrue(!pech.openTradeGui(player), "a sneaking player opens no window (:695)");
        player.setShiftKeyDown(false);
        player.getInventory().setItem(0, new ItemStack(Items.NAME_TAG));
        helper.assertTrue(!pech.openTradeGui(player), "the name-tag stand-in opens no window (:695, BD-73)");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(pech.openTradeGui(player), "a tamed pech opens the window (:697)");
        // NOT asserted: the opened container on the mock player. The golem lane measured this
        // harness wall (harness note at the bottom of GolemMenuGameTest.java): the patched
        // ServerPlayer#openMenu payload path against the mock player's unnegotiated
        // EmbeddedChannel is not a provable leg in both arms; the gate booleans above are.
        helper.succeed();
    }

    /** {@code ContainerPech.java:245-247} — the window lives while the pech is tamed. */
    @GameTest(template = "empty")
    public static void pechMenuValidatesTamedOnly(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final EntityPech pech = tamedPech(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        helper.assertTrue(menu.stillValid(player), "a tamed pech keeps the window (:245-247)");
        pech.setTamed(false);
        helper.assertTrue(!menu.stillValid(player), "an untamed pech closes it");
        helper.succeed();
    }

    /**
     * The dice click. This probe pinned the SHIPPED DEFECT ("no button is
     * accepted") while the roll sat behind the unported loot engine; the roll
     * landed with TC-M7-04-m3-b-followup, so the accepted arm is what the
     * contract now pins: the window's gate is the original's
     * ({@code ContainerPech.java:84-90}) and the offer loses exactly one item
     * ({@code :158}). BD-73 keeps the parts that are still true — the
     * {@code ChestGenHooks} "dungeonChest" pool is not ported, so branch (b)
     * takes its failure arm (BD-75) — and the payout itself is measured probe by
     * probe in {@code PechTradeRollGameTest} (the grid contents, the scam arm,
     * the loot pack, the bounded search). The number of items in the grid is
     * deliberately not asserted here: the default level seed draws the roll's own
     * sequence, and the gated melee table can exhaust it.
     */
    @GameTest(template = "empty")
    public static void pechDiceClickRunsTheRoll(final GameTestHelper helper) {
        final EntityPech pech = tamedPech(helper);
        final Player player = fakePlayer(helper);
        final PechMenu menu = new PechMenu(0, player.getInventory(), pech);
        menu.slots.get(0).set(new ItemStack(Items.DIAMOND, 4));
        helper.assertTrue(menu.clickMenuButton(player, PechMenu.DICE_BUTTON),
                "button 0 is accepted while the roll gate stands (GuiPech.java:62 -> ContainerPech.java:64-70)");
        helper.assertTrue(menu.getItems().get(0).getCount() == 3,
                "the roll consumes exactly one offer item (:158), got " + menu.getItems().get(0).getCount());
        helper.assertTrue(!menu.clickMenuButton(player, 1), "no other button exists (:64-66)");
        helper.succeed();
    }

    private static void assertSlotAt(final GameTestHelper helper, final Slot slot, final int x, final int y) {
        helper.assertTrue(slot.x == x && slot.y == y,
                "slot at (" + slot.x + "," + slot.y + ") expected (" + x + "," + y + ")");
    }
}
