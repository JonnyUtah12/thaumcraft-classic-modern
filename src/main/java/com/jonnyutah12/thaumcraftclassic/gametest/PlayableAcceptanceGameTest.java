package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOre;
import com.jonnyutah12.thaumcraftclassic.registration.ModBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.ModCreativeTabs;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.registration.ModRegistration;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M9-01-s / t_8ea70fda — the PLAYABLE acceptance gate.
 *
 * <p>The intake (2026-10-30) asked for three observable behaviours: ores drop
 * items, items appear in JEI, items are searchable in creative. Each spine
 * shipped its own probe — {@code OreDropGameTest} (SPINE-1),
 * {@code CreativeTabGameTest} (SPINE-2), {@code ItemModelCoverageGameTest}
 * (SPINE-3) — and all three went green while the OVERALL ask was still
 * unsatisfied, because nothing composed them. This class is the composition:
 * one gate, in a fresh world, that asks the player-visible question end to end
 * and reports a single verdict. It is the standing gate, not a one-off.</p>
 *
 * <h2>The three legs, and how each is made machine-checkable</h2>
 * <ol>
 *   <li><b>Ores drop items.</b> For every one of the 8 {@code blockcustomore}
 *       variants, place the ore, break it through the shipped dispatch path,
 *       and pin the drop set against the original formula
 *       ({@code reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockCustomOre.java}
 *       getDrops 100-113): variant 0 drops the ore itself with its variant,
 *       variants 1-6 drop {@code thaumcraft:itemshard} with
 *       {@code thaumcraft:item_meta = variant-1}, count
 *       {@code 1 + rand(2 + fortune)} as ONE STACK PER UNIT, variant 7 drops
 *       {@code thaumcraft:itemresource} meta 6 (Amber) with
 *       {@code 1 + rand(fortune + 1)}. Run with no tool, with silk touch and
 *       with fortune &gt; 0.</li>
 *   <li><b>Items are searchable in creative.</b> The tab's contents are
 *       compared to the set DERIVED FROM THE LIVE ITEM REGISTRY at test time —
 *       never a hardcoded list, which is the trap this project keeps hitting
 *       (a hardcoded expected set can never fail when a catalog card
 *       registers a new item). The converse is asserted too: the
 *       {@code -Dthaumcraft.testprobe=true} fixtures ({@code test_probe},
 *       {@code test_aspect_source}) are absent from every public tab.</li>
 *   <li><b>Items appear in JEI / render as themselves.</b> JEI enumerates the
 *       registered items and needs no listing code (recipe categories are the
 *       deferred card), so the machine-checkable form of the JEI ask is that
 *       every production item resolves to a real display name —
 *       a {@code Component.translatable} whose key RESOLVES, i.e. resolves to
 *       something that is not its own key — and has a model JSON on the
 *       classpath. An item with neither is invisible in JEI's grid: a raw key
 *       and the missing-model placeholder are exactly what a player reports
 *       as "the items are not in JEI".</li>
 * </ol>
 *
 * <p>This probe does NOT depend on JEI being installed (the browser test
 * directory currently holds no JEI jar), on an asset overlay (D-004 ships the
 * original PNGs as a local overlay only), or on any modded listing API. It
 * asks only questions the shipped code answers on its own.</p>
 *
 * <h2>The dispatch path (why the tool legs differ from the no-tool leg)</h2>
 * <p>The no-tool legs drive {@code level.destroyBlock(pos, true, null)} — the
 * vanilla destruction entry point. Measured in the Mojang-mapped source
 * ({@code net/minecraft/world/level/Level.java:317},
 * {@code destroyBlock(BlockPos, boolean, Entity, int)}), that path hands
 * {@code Block.dropResources} a literal {@code ItemStack.EMPTY} as the tool,
 * so {@code match_tool} CANNOT see enchantments through it — no silk pool and
 * no fortune term can fire, in this port or in vanilla. The tool-carrying leg
 * of the same dispatch is the call {@code ServerPlayerGameMode.destroyBlock}
 * makes with the player's real main-hand copy
 * ({@code ServerPlayerGameMode.java:272-279}:
 * {@code block.playerDestroy(level, player, pos, state, blockEntity, itemstack1)}),
 * so the silk-touch and fortune arms drive exactly that call, with a mock
 * survival player holding an enchanted diamond pickaxe. Both are vanilla
 * dispatch hooks; no method of the mod under test is called directly.</p>
 *
 * <h2>Anti-tautology</h2>
 * <p>Every leg derives its expected set from the live registries and asserts
 * that set is non-empty before comparing: a leg whose expected set is empty
 * passes by construction, and that is the failure mode this card exists to
 * catch. The mutation proofs for this gate (hide
 * {@code data/thaumcraft/loot_table/blocks/blockcustomore.json}; empty the tab
 * builder's {@code displayItems}) are recorded in the card's handoff and in
 * {@code docs/testing.md}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayableAcceptanceGameTest {

    /** Breaks per variant in the no-tool leg: 1..2 must show both ends (2^-25 to miss). */
    private static final int SAMPLES_PLAIN = 25;
    /** Breaks per variant with fortune III: the wide ends must be reached. */
    private static final int SAMPLES_TOOL = 30;
    /** Breaks per variant in the composed verdict — cheap, the legs above own the statistics. */
    private static final int SAMPLES_VERDICT = 4;

    /**
     * Below this many breaks a sample cannot be asked to reach both ends of a
     * random draw (the infused-stone fortune-0 window is a coin flip; at 4
     * breaks a 1/16 flake is a coin flip). The spread assertions therefore run
     * only at or above this sample count — the bounds assertions always run.
     */
    private static final int SPREAD_MIN_SAMPLES = 16;

    /** The mod's asset tree on the runtime classpath (leg 3's model + lang reads). */
    private static final String ASSET_ROOT = "/assets/" + ThaumcraftClassic.MOD_ID + "/";
    private static final String MODELS_ITEM_DIR = ASSET_ROOT + "models/item/";

    private PlayableAcceptanceGameTest() {
    }

    // ================================================================== probes

    /**
     * Leg 1a: every variant drops the original's item through the vanilla
     * destruction entry point with no tool in hand — the shape a player gets
     * from an explosion, a piston, or a bare-handed world event.
     */
    @GameTest(template = "gametest_pad_32")
    public static void oresdropthroughthedestructionpath(final GameTestHelper helper) {
        report(helper, "ores drop on break (no tool)", oreLeg(helper, SAMPLES_PLAIN));
    }

    /**
     * Leg 1b: the two enchantment lanes of the shipped loot table. Silk touch
     * returns the block with its variant for all 8 variants (the {@code
     * match_tool} pool); fortune III widens variants 1-6 to 1..5 size-1 shard
     * stacks and variant 7 to 1..4 in one stack.
     */
    @GameTest(template = "gametest_pad_32")
    public static void silktouchandfortuneruntheloottablelanes(final GameTestHelper helper) {
        report(helper, "silk touch and fortune lanes", enchantmentLeg(helper, SAMPLES_TOOL));
    }

    /**
     * Leg 2: the creative tab's contents ARE the live registry's production
     * set, and the test fixtures are in no public tab.
     */
    @GameTest(template = "empty")
    public static void tabmembershipcomesfromtheliveregistry(final GameTestHelper helper) {
        report(helper, "creative tab membership", tabLeg(helper));
    }

    /**
     * Leg 3: every production item resolves a real display name and has a
     * model JSON on the classpath — the machine-checkable form of "it shows up
     * in JEI as itself".
     */
    @GameTest(template = "empty")
    public static void productionitemsresolvenameandmodel(final GameTestHelper helper) {
        report(helper, "name and model resolution", nameAndModelLeg());
    }

    /**
     * THE COMPOSED VERDICT. All three legs run in one fresh world and are
     * aggregated into ONE failure list, so the gate answers the intake's
     * question ("is the mod playable?") with one red/green, not three probes
     * that each pass in isolation. This is the assertion the three per-card
     * probes never made: that the ore drops, the tab membership and the
     * name/model resolution hold TOGETHER.
     */
    @GameTest(template = "gametest_pad_32")
    public static void theplayableintakecomposesintooneverdict(final GameTestHelper helper) {
        final List<String> verdict = new ArrayList<>();
        verdict.addAll(oreLeg(helper, SAMPLES_VERDICT));
        verdict.addAll(enchantmentLeg(helper, SAMPLES_VERDICT));
        verdict.addAll(tabLeg(helper));
        verdict.addAll(nameAndModelLeg());
        final int items = productionItemStacks().size();
        final int tabs = thaumcraftTabs().size();
        ThaumcraftClassic.LOGGER.info(
                "[M9-01s PLAYABLE] composed verdict: {} production items, {} thaumcraft tab(s), "
                        + "8 ore variants, {} leg failures",
                items, tabs, verdict.size());
        report(helper, "PLAYABLE intake (ores drop + tab membership + name/model)", verdict);
    }

    // ==================================================================== legs

    /**
     * Leg 1a body: place each of the 8 variants, break it with
     * {@code level.destroyBlock(pos, true, null)}, and pin the drop set.
     * Returns one string per broken behaviour; empty list means the leg holds.
     */
    private static List<String> oreLeg(final GameTestHelper helper, final int samples) {
        final List<String> failures = new ArrayList<>();
        for (int variant = 0; variant <= 7; variant++) {
            final int x = 1 + 3 * variant;
            final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
            int observedMin = Integer.MAX_VALUE;
            int observedMax = 0;
            for (int sample = 0; sample < samples; sample++) {
                helper.setBlock(x, 1, 1, oreState(variant));
                helper.getLevel().destroyBlock(pos, true, null);
                final List<ItemStack> drops = collectDrops(helper, pos);
                final int units = drops.stream().mapToInt(ItemStack::getCount).sum();
                observedMin = Math.min(observedMin, units);
                observedMax = Math.max(observedMax, units);
                failures.addAll(assertDropSet(variant, drops, 0));
            }
            if (variant >= BlockCustomOre.VARIANT_INFUSED_MIN && variant <= BlockCustomOre.VARIANT_INFUSED_MAX) {
                // The original's 1 + rand.nextInt(2 + 0) is a draw from {1,2}:
                // both ends must appear, and nothing outside the range may.
                if (samples >= SPREAD_MIN_SAMPLES && (observedMin != 1 || observedMax != 2)) {
                    failures.add("infused stone v" + variant + " (no tool): units observed "
                            + observedMin + ".." + observedMax + ", expected to cover exactly 1..2 over "
                            + samples + " breaks (original: 1 + rand.nextInt(2 + fortune))");
                }
            } else if (observedMin != observedMax) {
                failures.add("variant " + variant + " (no tool): unit count is not deterministic ("
                        + observedMin + ".." + observedMax + ") — cinnabar and amber draw exactly 1 at fortune 0");
            }
        }
        return failures;
    }

    /** Leg 1b body: the silk-touch pool and the fortune term. */
    private static List<String> enchantmentLeg(final GameTestHelper helper, final int samples) {
        final List<String> failures = new ArrayList<>();
        final Player breaker = helper.makeMockPlayer(GameType.SURVIVAL);
        breaker.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getX() + 0.5,
                helper.absolutePos(new BlockPos(1, 2, 1)).getY(),
                helper.absolutePos(new BlockPos(1, 2, 1)).getZ() + 0.5);
        try {
            final ItemStack silk = enchantedPickaxe(helper, Enchantments.SILK_TOUCH, 1);
            for (int variant = 0; variant <= 7; variant++) {
                final int x = 1 + 3 * variant;
                final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
                helper.setBlock(x, 1, 1, oreState(variant));
                toolBreak(helper, breaker, pos, silk);
                final List<ItemStack> drops = collectDrops(helper, pos);
                if (drops.size() != 1) {
                    failures.add("silk touch v" + variant + ": expected exactly 1 stack (the match_tool "
                            + "silk pool returns the block), got " + drops.size() + " " + drops);
                    continue;
                }
                final ItemStack drop = drops.get(0);
                if (!drop.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get())) {
                    failures.add("silk touch v" + variant + ": expected the ore block itself, got " + drop);
                    continue;
                }
                final BlockItemStateProperties blockState = drop.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
                if (blockState == null || !Objects.equals(blockState.get(BlockCustomOre.VARIANT), variant)) {
                    failures.add("silk touch v" + variant + ": the drop must carry the block_state variant "
                            + variant + " (damageDropped(meta)=meta in the original), got "
                            + drop.get(net.minecraft.core.component.DataComponents.BLOCK_STATE));
                }
            }

            final ItemStack fortune3 = enchantedPickaxe(helper, Enchantments.FORTUNE, 3);
            for (int variant = BlockCustomOre.VARIANT_INFUSED_MIN; variant <= BlockCustomOre.VARIANT_INFUSED_MAX;
                 variant++) {
                final int x = 1 + 3 * variant;
                final BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
                int observedMin = Integer.MAX_VALUE;
                int observedMax = 0;
                for (int sample = 0; sample < samples; sample++) {
                    helper.setBlock(x, 1, 1, oreState(variant));
                    toolBreak(helper, breaker, pos, fortune3);
                    final List<ItemStack> drops = collectDrops(helper, pos);
                    final int units = drops.stream().mapToInt(ItemStack::getCount).sum();
                    observedMin = Math.min(observedMin, units);
                    observedMax = Math.max(observedMax, units);
                    failures.addAll(assertDropSet(variant, drops, 3));
                }
                if (observedMin < 1 || observedMax > 5) {
                    failures.add("fortune III infused stone v" + variant + ": units observed " + observedMin
                            + ".." + observedMax + ", expected inside 1..5 (1 + rand.nextInt(2 + 3))");
                }
                if (observedMax < 2 && samples >= SPREAD_MIN_SAMPLES) {
                    failures.add("fortune III infused stone v" + variant + " never beat the fortune-0 ceiling"
                            + " over " + samples + " breaks (observed max " + observedMax
                            + ") — the fortuned_uniform roll term is not reaching the loot table");
                }
            }

            final int amberX = 22;
            final BlockPos amberPos = helper.absolutePos(new BlockPos(amberX, 1, 1));
            int amberMin = Integer.MAX_VALUE;
            int amberMax = 0;
            for (int sample = 0; sample < samples; sample++) {
                helper.setBlock(amberX, 1, 1, oreState(BlockCustomOre.VARIANT_AMBER));
                toolBreak(helper, breaker, amberPos, fortune3);
                final List<ItemStack> drops = collectDrops(helper, amberPos);
                if (drops.size() != 1) {
                    failures.add("fortune III amber: expected 1 stack (the original adds one item with a"
                            + " count, not q stacks), got " + drops.size() + " " + drops);
                    continue;
                }
                final int count = drops.get(0).getCount();
                amberMin = Math.min(amberMin, count);
                amberMax = Math.max(amberMax, count);
                if (count < 1 || count > 4) {
                    failures.add("fortune III amber: count " + count + " outside 1..4 (1 + rand.nextInt(3 + 1))");
                }
                if (!drops.get(0).is(CatalogResources.ITEM_RESOURCE.get()) || metaOf(drops.get(0)) != 6) {
                    failures.add("fortune III amber: must stay itemresource meta 6, got " + drops.get(0));
                }
            }
            if (amberMax < 2 && samples >= SPREAD_MIN_SAMPLES) {
                failures.add("fortune III amber never exceeded 1 over " + samples + " breaks (observed max "
                        + amberMax + ") — uniform_bonus_count is not applying the fortune level");
            }
        } finally {
            breaker.discard();
        }
        return failures;
    }

    /**
     * The exact drop-set contract for one break of one variant: identity,
     * carried meta, one stack per unit, and the count window for the tool in
     * hand (fortune level 0 = no tool, 3 = fortune III).
     */
    private static List<String> assertDropSet(final int variant, final List<ItemStack> drops, final int fortune) {
        final List<String> failures = new ArrayList<>();
        if (variant == BlockCustomOre.VARIANT_CINNABAR) {
            if (drops.size() != 1 || drops.get(0).getCount() != 1) {
                failures.add("cinnabar v0: expected exactly one stack of the ore, got " + drops);
                return failures;
            }
            final ItemStack drop = drops.get(0);
            if (!drop.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get())) {
                failures.add("cinnabar v0 drops the ore itself (it is furnace-smelted to quicksilver), got " + drop);
            }
            final BlockItemStateProperties blockState =
                    drop.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (blockState == null || !Objects.equals(blockState.get(BlockCustomOre.VARIANT), 0)) {
                failures.add("cinnabar v0: the drop keeps the variant (block_state component), got " + drop);
            }
            return failures;
        }
        if (variant == BlockCustomOre.VARIANT_AMBER) {
            if (drops.size() != 1) {
                failures.add("amber v7: expected one resource stack, got " + drops.size() + " " + drops);
                return failures;
            }
            final ItemStack drop = drops.get(0);
            if (!drop.is(CatalogResources.ITEM_RESOURCE.get()) || metaOf(drop) != 6) {
                failures.add("amber v7 drops itemresource meta 6 (Amber), got " + drop);
            }
            final int max = 1 + fortune;
            if (drop.getCount() < 1 || drop.getCount() > max) {
                failures.add("amber v7: count " + drop.getCount() + " outside 1.." + max
                        + " (1 + rand.nextInt(fortune + 1))");
            }
            return failures;
        }
        // Infused stone 1..6: q single-item shard stacks, meta = variant - 1.
        final int min = 1;
        final int max = 2 + fortune;
        int units = 0;
        for (final ItemStack drop : drops) {
            units += drop.getCount();
            if (!drop.is(CatalogResources.ITEM_SHARD.get())) {
                failures.add("infused stone v" + variant + " drops itemshard, got " + drop);
            } else if (metaOf(drop) != variant - 1) {
                failures.add("infused stone v" + variant + " drops shard meta " + (variant - 1)
                        + " (original getDrops: meta md-1), got " + metaOf(drop) + " on " + drop);
            }
            if (drop.getCount() != 1) {
                failures.add("infused stone v" + variant + ": the original loop adds q stacks of 1, not one"
                        + " stack of q, got a stack of " + drop.getCount() + " (" + drop + ")");
            }
        }
        if (units < min || units > max) {
            failures.add("infused stone v" + variant + ": " + units + " shard units outside " + min + ".." + max
                    + " (1 + rand.nextInt(2 + fortune), fortune " + fortune + ") in " + drops);
        }
        return failures;
    }

    /** Leg 2 body: the tab's contents against the live registry, plus the fixture converse. */
    private static List<String> tabLeg(final GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();

        final Set<Item> expected = new LinkedHashSet<>();
        for (final ItemStack stack : productionItemStacks()) {
            expected.add(stack.getItem());
        }
        if (expected.isEmpty()) {
            failures.add("the registry-derived expected item set is empty — this leg can never fail");
            return failures;
        }

        final List<CreativeModeTab> tabs = thaumcraftTabs();
        if (tabs.size() != 1) {
            failures.add("the original exposed exactly one tab (reference Thaumcraft.java:95 tabTC); "
                    + tabs.size() + " thaumcraft tabs are registered");
        }

        final Set<Item> shownInPublicTabs = new HashSet<>();
        final Set<Item> shownAnywhere = new HashSet<>();
        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            build(tab, helper);
            final Set<Item> contents = new HashSet<>();
            for (final ItemStack stack : tab.getDisplayItems()) {
                contents.add(stack.getItem());
            }
            shownAnywhere.addAll(contents);
            if (tab.getType() != CreativeModeTab.Type.SEARCH) {
                shownInPublicTabs.addAll(contents);
            }
        }

        final List<Item> missing = expected.stream().filter(item -> !shownInPublicTabs.contains(item)).toList();
        if (!missing.isEmpty()) {
            failures.add(missing.size() + " of " + expected.size() + " registered production items appear in no"
                    + " public creative tab, so creative search and JEI cannot find them: " + previewIds(missing));
        }
        if (!tabs.isEmpty()) {
            final Set<Item> tabItems = new HashSet<>();
            build(tabs.get(0), helper);
            for (final ItemStack stack : tabs.get(0).getDisplayItems()) {
                tabItems.add(stack.getItem());
            }
            if (!tabItems.equals(expected)) {
                final List<Item> absent = expected.stream().filter(i -> !tabItems.contains(i)).toList();
                final List<Item> foreign = tabItems.stream().filter(i -> !expected.contains(i)).toList();
                failures.add("the thaumcraft tab is the registered production item set (1.7.10 setTab(tabTC));"
                        + " " + absent.size() + " missing " + previewIds(absent) + ", " + foreign.size()
                        + " foreign/fixture " + previewIds(foreign));
            }
        }

        // The converse: the -Dthaumcraft.testprobe=true fixtures are test
        // scaffolding and must be in no public tab (search tab included).
        final List<Item> fixtures = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ThaumcraftClassic.MOD_ID.equals(id.getNamespace())
                    && ModCreativeTabs.isTestFixtureId(id.getPath())) {
                fixtures.add(item);
            }
        }
        final boolean propertyOn = Boolean.getBoolean(ModRegistration.TEST_PROBE_PROPERTY);
        if (propertyOn != !fixtures.isEmpty()) {
            failures.add("fixture items must be registered exactly when -D" + ModRegistration.TEST_PROBE_PROPERTY
                    + " is on; property=" + propertyOn + " fixtures=" + fixtures.size()
                    + " — the fixture converse leg is vacuous");
        }
        final List<Item> leaks = fixtures.stream().filter(shownAnywhere::contains).toList();
        if (!leaks.isEmpty()) {
            failures.add("test-fixture ids (" + previewIds(leaks) + ") are present in a public creative tab");
        }
        if (fixtures.isEmpty()) {
            failures.add("no test_probe/test_aspect_source fixture is registered, so the fixture-absence"
                    + " converse of this leg asserts nothing — the game-test run config must set -D"
                    + ModRegistration.TEST_PROBE_PROPERTY + "=true");
        }
        ThaumcraftClassic.LOGGER.info("[M9-01s] tab leg: {} production items, {} shown by public tabs, {} fixtures,"
                + " {} leaks, {} thaumcraft tabs", expected.size(), shownInPublicTabs.size(), fixtures.size(),
                leaks.size(), tabs.size());
        return failures;
    }

    /** Leg 3 body: display name resolves to non-key text, and a model JSON exists. */
    private static List<String> nameAndModelLeg() {
        final List<String> failures = new ArrayList<>();
        final JsonObject lang = loadJson(ASSET_ROOT + "lang/en_us.json").getAsJsonObject();
        final List<ItemStack> stacks = productionItemStacks();
        if (stacks.isEmpty()) {
            failures.add("the registry-derived production item set is empty — this leg can never fail");
            return failures;
        }
        final Set<String> modelFiles = committedModelPaths();
        if (modelFiles.isEmpty()) {
            failures.add("no item models are enumerable on the classpath under " + MODELS_ITEM_DIR
                    + " — this leg can never fail");
        }
        final List<String> nameFailures = new ArrayList<>();
        final List<String> modelFailures = new ArrayList<>();
        for (final ItemStack stack : stacks) {
            final Item item = stack.getItem();
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            final Component name = stack.getHoverName();
            // getHoverName is the item's own name component (Item.getName(stack));
            // ItemStack.getDisplayName() wraps it in Component.empty().append(...)
            // plus a rarity style, so its contents are never the translatable.
            if (!(name.getContents() instanceof TranslatableContents contents)) {
                nameFailures.add(id + " name is not translatable: " + name.getContents());
                continue;
            }
            final String key = contents.getKey();
            final String resolved = resolve(lang, key);
            if (resolved == null) {
                nameFailures.add(id + " name key " + key + " is not shipped in lang/en_us.json (the client"
                        + " shows the raw key)");
            } else if (resolved.isBlank()) {
                nameFailures.add(id + " name key " + key + " resolves to an empty string");
            } else if (resolved.equals(key)) {
                nameFailures.add(id + " name key " + key + " resolves to its own key");
            }
            final String model = id.getPath() + ".json";
            if (!modelFiles.contains(model)
                    || classResource(ASSET_ROOT + "models/item/" + model) == null) {
                modelFailures.add(id + " has no " + MODELS_ITEM_DIR + id.getPath() + ".json");
            }
        }
        ThaumcraftClassic.LOGGER.info("[M9-01s] name/model leg: {} production items, {} name failures, {} model"
                + " failures, {} model files enumerable", stacks.size(), nameFailures.size(),
                modelFailures.size(), modelFiles.size());
        failures.addAll(nameFailures.stream().map(s -> "display name: " + s).toList());
        failures.addAll(modelFailures.stream().map(s -> "model: " + s).toList());
        return failures;
    }

    // ================================================================= helpers

    private static void report(final GameTestHelper helper, final String leg, final List<String> failures) {
        helper.assertTrue(failures.isEmpty(),
                leg + " — " + failures.size() + " broken behaviour(s):\n  - "
                        + String.join("\n  - ", failures.subList(0, Math.min(failures.size(), 30))));
        helper.succeed();
    }

    private static BlockState oreState(final int variant) {
        return ModBlocks.BLOCK_CUSTOM_ORE.get().defaultBlockState().setValue(BlockCustomOre.VARIANT, variant);
    }

    /**
     * The stacks and orbs the break just spawned near pos, swept so the next
     * sample starts clean. Only the ids the shipped loot table can emit count
     * as this block's drop: game-test structures share one level and pack by
     * template size, so a neighbouring probe's cell can sit inside the sweep
     * box and drop its own stacks there (measured: a vanilla {@code
     * stone_button}). Asserting on identity, not on "whatever is in the box",
     * is what keeps this gate from moving every time the batch grows.
     */
    private static List<ItemStack> collectDrops(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        final List<ItemStack> all = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)
                .stream().map(ItemEntity::getItem).toList();
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
        helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).forEach(ExperienceOrb::discard);
        final List<ItemStack> foreign = all.stream().filter(stack -> !isOreDrop(stack)).toList();
        if (!foreign.isEmpty()) {
            ThaumcraftClassic.LOGGER.info("[M9-01s] {} foreign stack(s) in the drop box at {} — a neighbouring "
                    + "structure in the shared game-test level, not this block's drop: {}",
                    foreign.size(), pos, foreign);
        }
        return all.stream().filter(PlayableAcceptanceGameTest::isOreDrop).toList();
    }

    /** The three ids {@code blockcustomore}'s loot table is allowed to emit. */
    private static boolean isOreDrop(final ItemStack stack) {
        return stack.is(ModItems.BLOCK_CUSTOM_ORE_ITEM.get())
                || stack.is(CatalogResources.ITEM_SHARD.get())
                || stack.is(CatalogResources.ITEM_RESOURCE.get());
    }

    /**
     * The tool-carrying leg of vanilla's block-break dispatch: exactly the
     * call {@code ServerPlayerGameMode.destroyBlock} makes with the player's
     * main-hand copy ({@code ServerPlayerGameMode.java:272-279}).
     */
    private static void toolBreak(final GameTestHelper helper, final Player breaker, final BlockPos pos,
            final ItemStack tool) {
        breaker.setItemInHand(InteractionHand.MAIN_HAND, tool);
        final BlockState state = helper.getLevel().getBlockState(pos);
        state.getBlock().playerDestroy(helper.getLevel(), breaker, pos, state, null, tool);
    }

    private static ItemStack enchantedPickaxe(final GameTestHelper helper,
            final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
            final int level) {
        final ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(key), level);
        return pickaxe;
    }

    private static int metaOf(final ItemStack stack) {
        final Integer meta = stack.get(ModDataComponents.ITEM_META.get());
        return meta == null ? 0 : meta;
    }

    /**
     * The production item stacks, derived from the LIVE {@code
     * BuiltInRegistries.ITEM} at test time through the tab's own
     * namespace-plus-fixture filter, in registry-id order. These are the
     * stacks a player sees in the creative tab ({@code output.accept(item)}
     * builds exactly this), so legs 2 and 3 read what the player reads.
     */
    private static List<ItemStack> productionItemStacks() {
        final List<ItemStack> stacks = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (ModCreativeTabs.isShippedCatalogItem(item)) {
                stacks.add(new ItemStack(item));
            }
        }
        stacks.sort(java.util.Comparator.comparing(
                s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        return stacks;
    }

    private static List<CreativeModeTab> thaumcraftTabs() {
        final List<CreativeModeTab> tabs = new ArrayList<>();
        for (final CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            final ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (ThaumcraftClassic.MOD_ID.equals(id.getNamespace())) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    /**
     * Build a tab's contents the way the shipped game does. The game-test
     * server never reaches the production build sites, so drive the public
     * {@link CreativeModeTab#buildContents} with the production parameters —
     * vanilla feature set, permission true, the level's registry access —
     * which runs the shipped {@code displayItems} generator and NeoForge's
     * {@code BuildCreativeModeTabContentsEvent}.
     */
    private static void build(final CreativeModeTab tab, final GameTestHelper helper) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                net.minecraft.world.flag.FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
    }

    /** Resolve a lang key through the shipped en_us.json (the text a player reads). */
    private static String resolve(final JsonObject lang, final String key) {
        final JsonElement element = lang.get(key);
        if (element == null) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("translate")) {
            // A nested translatable: resolve its own key, one level.
            return resolve(lang, element.getAsJsonObject().get("translate").getAsString());
        }
        return element.toString();
    }

    /** Every {@code models/item/*.json} path enumerable on the classpath. */
    private static Set<String> committedModelPaths() {
        final java.net.URL dir = PlayableAcceptanceGameTest.class.getResource(MODELS_ITEM_DIR);
        if (dir == null) {
            return new TreeSet<>(); // the leg asserts a non-empty listing, so this cannot pass silently
        }
        try {
            final Path root = Path.of(dir.toURI());
            if (!Files.isDirectory(root)) {
                return new TreeSet<>(); // packaged: the per-id classpath read below still gates every item
            }
            try (Stream<Path> walk = Files.walk(root)) {
                final Set<String> paths = new TreeSet<>();
                walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> paths.add(root.relativize(p).toString().replace('\\', '/')));
                return paths;
            }
        } catch (java.net.URISyntaxException | IOException e) {
            throw new IllegalStateException("the shipped asset tree is not addressable on the classpath: "
                    + MODELS_ITEM_DIR, e);
        }
    }

    private static java.net.URL classResource(final String path) {
        return PlayableAcceptanceGameTest.class.getResource(path);
    }

    private static JsonElement loadJson(final String assetRelativePath) {
        try (InputStream in = PlayableAcceptanceGameTest.class.getResourceAsStream(assetRelativePath)) {
            if (in == null) {
                throw new IllegalStateException(assetRelativePath + " is not on the classpath");
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String previewIds(final List<Item> items) {
        final List<String> ids = items.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
        final int shown = Math.min(ids.size(), 25);
        return ids.subList(0, shown) + (ids.size() > shown ? " …(+" + (ids.size() - shown) + " more)" : "");
    }
}
