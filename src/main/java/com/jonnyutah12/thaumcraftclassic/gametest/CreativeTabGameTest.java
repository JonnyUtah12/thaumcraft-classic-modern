package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonObject;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModCreativeTabs;
import com.jonnyutah12.thaumcraftclassic.registration.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SPINE-2 / t_76ab9b65: the creative tab contract — "items are searchable in
 * creative mode" in machine-checkable form.
 *
 * <p>The user report: Thaumcraft items never appeared in the creative-mode
 * search. Root cause: {@code ModCreativeTabs} registered zero tabs, so no
 * thaumcraft item was in any tab's {@code displayItems} and the vanilla
 * creative search (which indexes tab contents, not the registry) found
 * nothing. JEI needs no code: it enumerates the item registry, so the same
 * tab registration is what puts the items in JEI as well.</p>
 *
 * <p>The original (Thaumcraft 4.2.3.5) exposed exactly ONE tab:
 * {@code Thaumcraft.tabTC = new CreativeTabThaumcraft(CreativeTabs.getNextID(),
 * "thaumcraft")} ({@code reference/decompiled/thaumcraft/thaumcraft/common/
 * Thaumcraft.java:95}), and 1.7.10 tabs are populated by every item's
 * {@code setTab(tabTC)} — i.e. the tab's contents are the registered item
 * set. The port reproduces that: one tab enumerating the live
 * {@code thaumcraft}-namespace item registry.</p>
 *
 * <p><b>The probes derive their expected sets from the live item registry at
 * test time.</b> A test that hardcoded a list of items to check can never
 * fail when a catalog card registers new items without the test knowing;
 * deriving from the registry makes every future catalog item automatically
 * accountable to this gate. The fixture set is derived the same way, so the
 * converse probe cannot be satisfied by omission.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTest {

    /**
     * Ids registered only under {@code -Dthaumcraft.testprobe=true} (the
     * game-test server run config) — test scaffolding, never shipped content,
     * and therefore never tab content. Single source of truth: the tab's own
     * enumeration filter in {@code ModCreativeTabs} applies the same list.
     */
    private static final List<String> TEST_FIXTURE_ID_PREFIXES = ModCreativeTabs.TEST_FIXTURE_ID_PREFIXES;

    private CreativeTabGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * Every production item in the live registry appears in the
     * {@code displayItems} of at least one public tab. The expected set is
     * enumerated from {@link BuiltInRegistries#ITEM} at test time, filtered
     * to this mod's namespace minus test fixtures — never hardcoded.
     *
     * <p>The vanilla search tab ({@code Type.SEARCH}) is excluded: its
     * contents are a derived union of the other tabs, so counting it would
     * let a mod with no tab pass this probe the moment any other source
     * (e.g. a future BuildCreativeModeTabContentsEvent handler on a vanilla
     * tab) touches any one item.</p>
     */
    @GameTest(template = "empty")
    public static void everyProductionItemIsInAPublicTab(final GameTestHelper helper) {
        final Set<Item> expected = productionItems();
        helper.assertTrue(!expected.isEmpty(),
                "the registry-derived expected item set is empty — this probe can never fail");

        final Set<Item> shown = new HashSet<>();
        int publicTabs = 0;
        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (tab.getType() == CreativeModeTab.Type.SEARCH) {
                continue;
            }
            publicTabs++;
            for (final ItemStack stack : displayItemsOf(helper, tab)) {
                shown.add(stack.getItem());
            }
        }

        final List<Item> missing = expected.stream().filter(item -> !shown.contains(item)).toList();
        ThaumcraftClassic.LOGGER.info("[SPINE-2] creative coverage: {} production items, {} items shown by {} "
                + "non-search tabs, {} missing", expected.size(), shown.size(), publicTabs, missing.size());
        helper.assertTrue(missing.isEmpty(),
                "creative-mode search is impossible for " + missing.size() + " of " + expected.size()
                        + " registered thaumcraft items — they appear in no tab's displayItems: "
                        + previewIds(missing));
        helper.succeed();
    }

    /**
     * The converse: the test-fixture items — registered only under the
     * game-test property, per the TestProbeContent contract — must not
     * appear in any public tab, search tab included. A tab that enumerates
     * the registry without filtering fixtures ships scaffolding to players.
     */
    @GameTest(template = "empty")
    public static void testProbeItemsAreNotInAnyPublicTab(final GameTestHelper helper) {
        final List<Item> fixtures = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ThaumcraftClassic.MOD_ID.equals(id.getNamespace()) && isFixture(id.getPath())) {
                fixtures.add(item);
            }
        }
        final boolean propertyOn = Boolean.getBoolean(ModRegistration.TEST_PROBE_PROPERTY);
        helper.assertTrue(propertyOn == !fixtures.isEmpty(),
                "fixture items must be registered exactly when -D" + ModRegistration.TEST_PROBE_PROPERTY
                        + " is on; property=" + propertyOn + " fixtures=" + fixtures.size());

        final Set<Item> shownAnywhere = new HashSet<>();
        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            for (final ItemStack stack : displayItemsOf(helper, tab)) {
                shownAnywhere.add(stack.getItem());
            }
        }
        final List<Item> leaks = fixtures.stream().filter(shownAnywhere::contains).toList();
        ThaumcraftClassic.LOGGER.info("[SPINE-2] fixture leak check: {} fixture items, {} leaks",
                fixtures.size(), leaks.size());
        helper.assertTrue(leaks.isEmpty(),
                "test-fixture items leaked into a public creative tab: " + previewIds(leaks));
        helper.succeed();
    }

    /**
     * The tab mirrors the original: exactly one thaumcraft tab, its title is
     * the 1.21.1 {@code itemGroup.<ns>.<id>} key present in the shipped lang
     * file, it carries a search bar, and its contents are exactly the
     * production item set — nothing missing (that is probe 1, asserted from
     * the tab side too), no foreign items, and no fixtures.
     */
    @GameTest(template = "empty")
    public static void thaumcraftTabMirrorsTheOriginalOneTab(final GameTestHelper helper) {
        final List<CreativeModeTab> ours = new ArrayList<>();
        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            final ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (ThaumcraftClassic.MOD_ID.equals(id.getNamespace())) {
                ours.add(tab);
            }
        }
        helper.assertTrue(ours.size() == 1,
                "the original exposed exactly one tab (reference Thaumcraft.java:95 tabTC); "
                        + ours.size() + " thaumcraft tabs are registered");

        final CreativeModeTab tab = ours.get(0);
        final ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        helper.assertTrue(tab.hasSearchBar(),
                "thaumcraft tab " + tabId + " has no search bar — creative-mode search is impossible");

        final String labelKey = "itemGroup." + tabId.getNamespace() + "." + tabId.getPath();
        helper.assertTrue(tab.getDisplayName().getContents() instanceof TranslatableContents contents
                        && labelKey.equals(contents.getKey()),
                "the tab title is not the 1.21.1 itemGroup key " + labelKey + " (1.21.1 convention; NOT "
                        + "item_category.*): " + tab.getDisplayName().getContents());
        final JsonObject lang = loadLang();
        helper.assertTrue(lang.has(labelKey) && lang.get(labelKey).getAsString().length() > 0,
                "the tab label key " + labelKey + " is not shipped in assets/thaumcraft/lang/en_us.json");

        final Set<Item> production = productionItems();
        final Set<Item> tabItems = new HashSet<>();
        for (final ItemStack stack : displayItemsOf(helper, tab)) {
            tabItems.add(stack.getItem());
        }
        final List<Item> missing = production.stream().filter(item -> !tabItems.contains(item)).toList();
        helper.assertTrue(missing.isEmpty(),
                "the thaumcraft tab is the registered item set (1.7.10 setTab(tabTC)); "
                        + missing.size() + " registered items are absent: " + previewIds(missing));
        final List<Item> foreign = tabItems.stream().filter(item -> !production.contains(item)).toList();
        helper.assertTrue(foreign.isEmpty(),
                "the thaumcraft tab shows items that are not shipped production content "
                        + "(foreign ids or test fixtures): " + previewIds(foreign));
        ThaumcraftClassic.LOGGER.info("[SPINE-2] tab {} carries {} display items (label key {})",
                tabId, tabItems.size(), labelKey);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The tab's built display contents. The game-test server never reaches
     * the production build sites ({@code MinecraftServer#syncResourcePacks}
     * / client resource reload), so the probe drives the public
     * {@link CreativeModeTab#buildContents} API with the production
     * parameters — the vanilla feature set, permission true, the level's
     * registry access — which runs the shipped {@code displayItems}
     * generator and NeoForge's {@code BuildCreativeModeTabContentsEvent}
     * exactly as the shipped game will.
     */
    private static java.util.Collection<ItemStack> displayItemsOf(final GameTestHelper helper,
            final CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                net.minecraft.world.flag.FeatureFlags.VANILLA_SET, true,
                helper.getLevel().registryAccess()));
        return tab.getDisplayItems();
    }

    /** Every registered item in the mod's namespace, minus the test fixtures. */
    private static Set<Item> productionItems() {
        final Set<Item> items = new LinkedHashSet<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ThaumcraftClassic.MOD_ID.equals(id.getNamespace()) && !isFixture(id.getPath())) {
                items.add(item);
            }
        }
        return items;
    }

    private static boolean isFixture(final String path) {
        return TEST_FIXTURE_ID_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static String previewIds(final List<Item> items) {
        final List<String> ids = items.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
        final int shown = Math.min(ids.size(), 25);
        return ids.subList(0, shown) + (ids.size() > shown ? " …(+" + (ids.size() - shown) + " more)" : "");
    }

    private static JsonObject loadLang() {
        try (InputStream in = CreativeTabGameTest.class.getResourceAsStream(
                "/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            if (in == null) {
                throw new IllegalStateException("assets/thaumcraft/lang/en_us.json is not on the classpath");
            }
            return com.google.gson.JsonParser.parseString(
                    new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
