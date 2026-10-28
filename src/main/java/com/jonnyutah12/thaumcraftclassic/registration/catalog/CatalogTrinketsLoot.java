package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLootItem;
import com.jonnyutah12.thaumcraftclassic.items.ItemCompassStone;
import com.jonnyutah12.thaumcraftclassic.items.ItemHandMirror;
import com.jonnyutah12.thaumcraftclassic.items.ItemLootBag;
import com.jonnyutah12.thaumcraftclassic.items.ItemResonator;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumometer;
import com.jonnyutah12.thaumcraftclassic.items.ItemTrunkSpawner;
import com.jonnyutah12.thaumcraftclassic.items.component.HandMirrorLink;
import com.jonnyutah12.thaumcraftclassic.menus.HandMirrorMenu;
import com.jonnyutah12.thaumcraftclassic.menus.TrunkMenu;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-04e catalog holder: trinkets, curios tools and loot containers.
 *
 * <p>Owns exactly the content-mapping.csv rows
 * {@code thaumcraft:blocklooturn}, {@code thaumcraft:blocklootcrate},
 * {@code thaumcraft:itemlootbag}, {@code thaumcraft:itemthaumometer},
 * {@code thaumcraft:itemresonator}, {@code thaumcraft:itemcompassstone},
 * {@code thaumcraft:handmirror}, {@code thaumcraft:trunkspawner} and the two
 * gui rows {@code gui_hand_mirror} / {@code gui_trunk} (the gui rows are
 * reference-only for the id harness; the menus registered here are their
 * 1.21.1 realizations). Per the catalog-triage rules this card registers
 * through its OWN holders; nothing here edits a sibling holder file.</p>
 */
public final class CatalogTrinketsLoot {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    // --- loot containers -----------------------------------------------------

    /**
     * Old Urn (original {@code blockLootUrn}): clay material, hardness 0.15,
     * resistance 0.0. The original's custom step sound "urnbreak" resolved
     * through 1.7.10's vanilla unnamespaced lookup and was never found, so the
     * surface was silent (docs/sound-events.md); {@code SoundType.EMPTY}
     * preserves that. Narrow urn hitbox via {@link BlockLoot#SHAPE_URN}.
     */
    public static final DeferredBlock<BlockLoot> BLOCK_LOOT_URN =
            BLOCKS.register("blocklooturn", () -> new BlockLoot(BlockLoot.Kind.URN));

    /**
     * Abandoned Crate (original {@code blockLootCrate}): wood material, same
     * hardness, vanilla wood sounds (the original used {@code soundTypeWood}
     * verbatim), crate hitbox via {@link BlockLoot#SHAPE_CRATE}.
     */
    public static final DeferredBlock<BlockLoot> BLOCK_LOOT_CRATE =
            BLOCKS.register("blocklootcrate", () -> new BlockLoot(BlockLoot.Kind.CRATE));

    public static final DeferredItem<BlockLootItem> BLOCK_LOOT_URN_ITEM =
            ITEMS.register("blocklooturn",
                    () -> new BlockLootItem(BLOCK_LOOT_URN.get(), new Item.Properties()));
    public static final DeferredItem<BlockLootItem> BLOCK_LOOT_CRATE_ITEM =
            ITEMS.register("blocklootcrate",
                    () -> new BlockLootItem(BLOCK_LOOT_CRATE.get(), new Item.Properties()));

    /**
     * Treasure bags (original {@code ItemLootBag}, stack 16): one id, the
     * original 0/1/2 metadata moved to the shared ITEM_META component
     * (D-001 pattern, same as ItemResource). Right-click rolls 8-12 items
     * from the original Config.initLoot bag tables (loot/LootRoller).
     */
    public static final DeferredItem<ItemLootBag> ITEM_LOOT_BAG =
            ITEMS.register("itemlootbag", () -> new ItemLootBag(new Item.Properties().stacksTo(16)));

    // --- trinket tools ---------------------------------------------------------

    /** Thaumometer (original {@code ItemThaumometer}): 25-tick scan hold.
     * 1.21.1 carries item rarity as registration properties (the original's
     * {@code getRarity} override has no per-stack hook anymore). */
    public static final DeferredItem<ItemThaumometer> ITEM_THAUMOMETER =
            ITEMS.register("itemthaumometer",
                    () -> new ItemThaumometer(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Essentia Resonator (original {@code ItemResonator}). */
    public static final DeferredItem<ItemResonator> ITEM_RESONATOR =
            ITEMS.register("itemresonator",
                    () -> new ItemResonator(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Sinister Lodestone (original {@code ItemCompassStone}). */
    public static final DeferredItem<ItemCompassStone> ITEM_COMPASS_STONE =
            ITEMS.register("itemcompassstone",
                    () -> new ItemCompassStone(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Magic Hand Mirror (original {@code ItemHandMirror}). */
    public static final DeferredItem<ItemHandMirror> HAND_MIRROR =
            ITEMS.register("handmirror",
                    () -> new ItemHandMirror(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /**
     * Traveling Trunk spawner item (original {@code ItemTrunkSpawner}). The
     * trunk entity itself is TC-M6-06; this item spawns it through the
     * {@code entities.TrunkHost} seam and carries the original's "upgrade" /
     * "inventory" NBT as the data components below.
     */
    public static final DeferredItem<ItemTrunkSpawner> TRUNK_SPAWNER =
            ITEMS.register("trunkspawner", () -> new ItemTrunkSpawner(new Item.Properties()));

    // --- data components -------------------------------------------------------

    /**
     * Hand-mirror link: the original linkX/linkY/linkZ/linkDim/dimname NBT,
     * verbatim fields (position + dimension id + the display-name string the
     * original stored for the tooltip).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HandMirrorLink>> MIRROR_LINK =
            DATA_COMPONENTS.registerComponentType("mirror_link",
                    builder -> builder.persistent(HandMirrorLink.CODEC));

    /** Trunk spawner "upgrade" byte (original NBT "upgrade", a byte). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> TRUNK_UPGRADE =
            DATA_COMPONENTS.registerComponentType("trunk_upgrade",
                    builder -> builder.persistent(Codec.BYTE));

    /** Trunk spawner "inventory" list (original NBTTagList "inventory"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TRUNK_INVENTORY =
            DATA_COMPONENTS.registerComponentType("trunk_inventory",
                    builder -> builder.persistent(ItemContainerContents.CODEC));

    // --- menus (gui rows) ------------------------------------------------------

    /** GUI_HAND_MIRROR (original gui_id 16, ContainerHandMirror). */
    public static final DeferredHolder<MenuType<?>, MenuType<HandMirrorMenu>> GUI_HAND_MIRROR =
            MENUS.register("gui_hand_mirror",
                    () -> new MenuType<>(HandMirrorMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    /** GUI_TRUNK (original gui_id 2, ContainerTravelingTrunk). Entity-bound:
     * the open-menu extra buffer carries the trunk entity id and row count
     * (see {@link TrunkMenu}). */
    public static final DeferredHolder<MenuType<?>, MenuType<TrunkMenu>> GUI_TRUNK =
            MENUS.register("gui_trunk",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                            (menuId, inv, extraData) -> new TrunkMenu(menuId, inv, extraData)));

    private CatalogTrinketsLoot() {
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void init(final IEventBus bus) {
        DATA_COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
    }
}
