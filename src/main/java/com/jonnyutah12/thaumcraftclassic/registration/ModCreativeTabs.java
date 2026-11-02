package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.items.ItemMetaAppearance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Creative tab registration (SPINE-2 / t_76ab9b65).
 *
 * <p>The original exposed one creative tab — {@code Thaumcraft.tabTC = new
 * CreativeTabThaumcraft(CreativeTabs.getNextID(), "thaumcraft")}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/
 * Thaumcraft.java:95}) — and 1.7.10 populated it through every item's
 * {@code setTab(tabTC)}, so its contents are exactly the registered item
 * set. This holder reproduces that: one tab, contents enumerated from the
 * live {@code thaumcraft}-namespace item registry. Enumerating the registry
 * (not a hardcoded list) means every catalog card's items are in the tab —
 * and therefore in the creative-mode search index and in JEI, which
 * enumerates registered items for free — from the moment they register.</p>
 *
 * <p>Tabs reference block/item holders, so they register after items
 * (NeoForge's own event order guarantees this; the constructor order in
 * {@link ModRegistration} mirrors it for readability). The
 * {@code displayItems} generator itself runs at tab-content build time —
 * after the registry freeze — so walking {@link BuiltInRegistries#ITEM}
 * inside it sees the complete item set, which is why the test-fixture items
 * (registered only under {@code -Dthaumcraft.testprobe=true}) must be
 * filtered here: the game-test server has them registered, and a tab built
 * from the raw registry would ship scaffolding to players. Enforced by
 * {@code CreativeTabGameTest}.</p>
 *
 * <p>The label is the 1.21.1 registry-entry convention
 * {@code itemGroup.<namespace>.<id>} — vanilla resolves the tab title from
 * the registered tab id, NOT the pre-1.20.5 {@code item_category.*} form.
 * The shipped string mirrors the original's {@code itemGroup.thaumcraft=
 * Thaumcraft} (original en_US.lang:670), which puts it under D-016.</p>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumcraftClassic.MOD_ID);

    /**
     * Registry ids that are test fixtures, not shipped content: registered
     * only when the run config sets {@code -Dthaumcraft.testprobe=true}
     * (see {@link TestProbeContent}), so they must never appear in a public
     * tab. Same rule as {@code LocalizationGameTest.TEST_FIXTURE_ID_PREFIXES}.
     */
    public static final List<String> TEST_FIXTURE_ID_PREFIXES = List.of("test_probe", "test_aspect_source");

    /**
     * The one thaumcraft tab, id {@code thaumcraft:thaumcraft} (the
     * original's tab label was "thaumcraft", Thaumcraft.java:95). Search bar
     * on: creative-mode search indexes tab contents, and the user report
     * this card closes is exactly "items are not searchable in creative".
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_THAUMCRAFT =
            CREATIVE_MODE_TABS.register("thaumcraft", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thaumcraft.thaumcraft"))
                    .icon(ModCreativeTabs::iconStack)
                    .displayItems((params, output) -> {
                        for (final Item item : BuiltInRegistries.ITEM) {
                            if (!isShippedCatalogItem(item)) {
                                continue;
                            }
                            // BD-17: the original's tab listed one entry per
                            // metadata (its CreativeTabs.getDisplayItemStacks
                            // walked Item.getSubItems, and every family item here
                            // registers hasSubtypes(true) / is a multi-variant
                            // block). Each entry carries thaumcraft:item_meta
                            // plus the 1.21.1 appearance mirror, so creative
                            // search shows — and gives — the variant it names.
                            final String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                            final int[] metas = ItemMetaAppearance.tabMetasOf(item);
                            if (metas.length == 0) {
                                output.accept(item);
                                continue;
                            }
                            for (final int meta : metas) {
                                final ItemStack entry = metaStack(item, meta);
                                if (entry != null) {
                                    output.accept(entry);
                                }
                            }
                        }
                    })
                    .withSearchBar()
                    .build());

    /** True for every registered {@code thaumcraft} item that is shipped content. */
    public static boolean isShippedCatalogItem(final Item item) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return ThaumcraftClassic.MOD_ID.equals(id.getNamespace())
                && !isTestFixtureId(id.getPath());
    }

    /** True for the {@code -Dthaumcraft.testprobe=true} fixture ids. */
    public static boolean isTestFixtureId(final String path) {
        return TEST_FIXTURE_ID_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * The tab icon. The original's was the casting wand
     * ({@code CreativeTabThaumcraft.func_78016_d} → {@code
     * ConfigItems.itemWandCasting}); the port has no wand item (the wand
     * stand-in is a vis-pool component on a pickaxe), so the icon falls back
     * to the shipped flagship resource: {@code itemresource} meta 0,
     * Alumentum — the label ItemResource.getName resolves for that meta.
     */
    private static ItemStack iconStack() {
        final ItemStack stack = new ItemStack(CatalogResources.ITEM_RESOURCE.get());
        ItemMetaAppearance.mirror(stack, 0); // BD-17: meta + appearance mirror
        return stack;
    }

    /**
     * The tab entry for one metadata of a shipped family. A block item's
     * variant must exist on the block (registry-derived: the {@code variant}
     * property's range is the authority), so a family table that ever names a
     * variant the block does not have drops the entry instead of shipping a
     * stack that cannot place.
     */
    private static ItemStack metaStack(final Item item, final int meta) {
        if (item instanceof BlockItem blockItem) {
            // The block's own int property (registry-derived) is the authority:
            // a meta it cannot hold gets no entry, never a stack that cannot place.
            return ItemMetaAppearance.variantStack(blockItem, meta);
        }
        final ItemStack stack = new ItemStack(item);
        ItemMetaAppearance.mirror(stack, meta);
        return stack;
    }

    private ModCreativeTabs() {
    }
}
