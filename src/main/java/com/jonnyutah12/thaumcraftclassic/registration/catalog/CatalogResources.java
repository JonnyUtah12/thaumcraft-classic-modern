package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockManaPod;
import com.jonnyutah12.thaumcraftclassic.items.ItemCrystalEssence;
import com.jonnyutah12.thaumcraftclassic.items.ItemManaBean;
import com.jonnyutah12.thaumcraftclassic.items.ItemNugget;
import com.jonnyutah12.thaumcraftclassic.items.ItemNuggetEdible;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.items.ItemShard;
import com.jonnyutah12.thaumcraftclassic.items.ItemTripleMeatTreat;
import com.jonnyutah12.thaumcraftclassic.items.ItemWispEssence;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.tiles.TileManaPod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-04a catalog holder: resource items (ItemResource / ItemShard /
 * ItemNugget), metal clusters, food (meat nuggets, triple meat treat, mana
 * bean), wisp/crystal essences, and the mana pod block + tile entity.
 *
 * <p>Per the catalog-triage ownership rules this card registers its content
 * in its own holder (this file) and adds exactly one wiring line in
 * {@code ModRegistration.init}; ids are the {@code new_id} column of
 * docs/content-mapping.csv (id contract, docs/architecture/registration.md).
 * The multi-meta families carry their original damage value in the
 * {@code thaumcraft:item_meta} component ({@link ModDataComponents}); the
 * original's per-meta names are per-meta lang keys resolved by TC-M7-01.</p>
 *
 * <p>Ore-dictionary rows are item tags under the same ids as their CSV
 * {@code new_id} (data/thaumcraft/tags/item/oredict.*.json); meta-level
 * precision at recipe use sites is expressed with
 * {@code neoforge:data_component} ingredients keyed on
 * {@code thaumcraft:item_meta}.</p>
 */
public final class CatalogResources {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);

    /** Attach every TC-M6-04a holder to the mod bus (single wiring line). */
    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    // ------------------------------------------------------------------
    // Multi-meta resource families (original damage values -> item_meta)
    // ------------------------------------------------------------------

    /** thaumcraft:itemresource — 19 metas, see ItemResource. */
    public static final DeferredItem<ItemResource> ITEM_RESOURCE =
            ITEMS.register("itemresource", () -> new ItemResource(metaCarryingProps()));

    /** thaumcraft:itemshard — 7 metas (6 primal shards + balanced). */
    public static final DeferredItem<ItemShard> ITEM_SHARD =
            ITEMS.register("itemshard", () -> new ItemShard(metaCarryingProps()));

    /** thaumcraft:itemnugget — nuggets, quicksilver drops, native clusters. */
    public static final DeferredItem<ItemNugget> ITEM_NUGGET =
            ITEMS.register("itemnugget", () -> new ItemNugget(metaCarryingProps()));

    /** thaumcraft:itemwispessence — ethereal essence, aspect via essentia component. */
    public static final DeferredItem<ItemWispEssence> ITEM_WISP_ESSENCE =
            ITEMS.register("itemwispessence", () -> new ItemWispEssence(new Item.Properties()));

    /** thaumcraft:itemcrystalessence — crystallized essence, self-assigning. */
    public static final DeferredItem<ItemCrystalEssence> ITEM_CRYSTAL_ESSENCE =
            ITEMS.register("itemcrystalessence", () -> new ItemCrystalEssence(new Item.Properties()));

    /**
     * The original's meta-carrying items were plain stack-64 items whose
     * damage value selected the variant; the default {@code item_meta} of 0
     * reproduces the original's damageless {@code new ItemStack(item)} form.
     */
    private static Item.Properties metaCarryingProps() {
        return new Item.Properties().component(ModDataComponents.ITEM_META.get(), 0);
    }

    // ------------------------------------------------------------------
    // Food (original ItemFood constructor values)
    // ------------------------------------------------------------------

    /** Original ItemNuggetEdible: super(1, 0.3F, false); 10-tick eat in the item class. */
    private static final FoodProperties MEAT_NUGGET_FOOD = new FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.3F).build();

    public static final DeferredItem<ItemNuggetEdible> ITEM_NUGGET_CHICKEN =
            ITEMS.register("itemnuggetchicken",
                    () -> new ItemNuggetEdible(new Item.Properties().food(MEAT_NUGGET_FOOD)));
    public static final DeferredItem<ItemNuggetEdible> ITEM_NUGGET_BEEF =
            ITEMS.register("itemnuggetbeef",
                    () -> new ItemNuggetEdible(new Item.Properties().food(MEAT_NUGGET_FOOD)));
    public static final DeferredItem<ItemNuggetEdible> ITEM_NUGGET_PORK =
            ITEMS.register("itemnuggetpork",
                    () -> new ItemNuggetEdible(new Item.Properties().food(MEAT_NUGGET_FOOD)));
    public static final DeferredItem<ItemNuggetEdible> ITEM_NUGGET_FISH =
            ITEMS.register("itemnuggetfish",
                    () -> new ItemNuggetEdible(new Item.Properties().food(MEAT_NUGGET_FOOD)));

    /**
     * Original ItemTripleMeatTreat: super(6, 0.8F, true) + setAlwaysEdible +
     * setPotionEffect(regeneration, 5s, amp 0, 66% chance).
     */
    private static final FoodProperties TRIPLE_MEAT_TREAT_FOOD = new FoodProperties.Builder()
            .nutrition(6).saturationModifier(0.8F).alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66F)
            .build();

    public static final DeferredItem<ItemTripleMeatTreat> TRIPLE_MEAT_TREAT =
            ITEMS.register("triplemeattreat",
                    () -> new ItemTripleMeatTreat(new Item.Properties().food(TRIPLE_MEAT_TREAT_FOOD)));

    /**
     * Original ItemManaBean: super(1, 0.5F, true) + setAlwaysEdible; the
     * random-effect-on-eat and aspect logic live in ItemManaBean.
     */
    private static final FoodProperties MANA_BEAN_FOOD = new FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.5F).alwaysEdible().build();

    public static final DeferredItem<ItemManaBean> ITEM_MANA_BEAN =
            ITEMS.register("itemmanabean",
                    () -> new ItemManaBean(new Item.Properties().food(MANA_BEAN_FOOD)));

    // ------------------------------------------------------------------
    // Mana pod block + tile entity (block row owns all ages; tile row is
    // keyed by the block id, docs/architecture/registration.md)
    // ------------------------------------------------------------------

    /**
     * Original BlockManaPod: Material.plants (GRASS step sound), hardness
     * 0.5F (divided by 8-age in the block class), light = age, non-opaque.
     * The original's func_149675_a(true) is the 1.7-era "has tile entity"
     * flag, expressed here by EntityBlock/hasBlockEntity.
     */
    public static final DeferredBlock<BlockManaPod> BLOCK_MANA_POD =
            BLOCKS.register("blockmanapod", () -> new BlockManaPod(BlockBehaviour.Properties.of()
                    .destroyTime(0.5F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()));

    /** Block item under the block id, as the original registered it. */
    public static final DeferredItem<BlockItem> BLOCK_MANA_POD_ITEM =
            ITEMS.register("blockmanapod",
                    () -> new BlockItem(BLOCK_MANA_POD.get(), new Item.Properties()));

    /** Tile entity for the pod; id contract = owning block id. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileManaPod>> MANA_POD =
            BLOCK_ENTITY_TYPES.register("blockmanapod",
                    () -> BlockEntityType.Builder.of(TileManaPod::new, BLOCK_MANA_POD.get()).build(null));

    private CatalogResources() {
    }
}
