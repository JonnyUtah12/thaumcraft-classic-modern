package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.ArcaneFurnaceStructure;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * TC-M4-06-m-i (t_42864caf): the alchemical furnace's <b>registration and
 * data</b> half — the nine shell metas, the {@code variant} blockstate, the
 * lang keys and the block loot table. The burning/face-machine behaviour and
 * its state machine are the split-out half (D-012), proven by
 * {@code ArcaneFurnaceFaceMachineGameTest} and by
 * {@code BlockStateCoverageGameTest#arcanefurnaceshellfacesshiptheoriginalgrid},
 * which this probe deliberately does not duplicate.
 *
 * <p>The meta domain is <b>derived from the block's own state container</b>
 * ({@code getStateDefinition().getProperty("variant")}), never a list written
 * into this file: widening, narrowing or renaming the property changes what the
 * probe enumerates, so every assertion here can fail. A hardcoded
 * {@code List.of(1,2,…,9)} would keep passing against a block that had lost a
 * state, which is what the card forbids.</p>
 *
 * <p>Original citations for the shipped facts, from
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockArcaneFurnace.java}:</p>
 * <ul>
 *   <li>{@code :355-362} {@code createTileEntity} — the nozzle tile for
 *       metadata 10, 2, 4, 5, 6, 8; <b>no tile at all</b> for the corner metas
 *       1, 3, 7, 9; the furnace tile for the plate (0).</li>
 *   <li>{@code :179-182} {@code getLightValue} — 13 for metadata 0 and 10,
 *       {@code super} (0) for the shell.</li>
 *   <li>{@code WandManager.replaceArcaneFurnace} {@code :457-480} — the grid,
 *       {@code dx + 3*dz + 5} over the plate's 3×3×3, the same index
 *       {@code calculateTexture} reads back at {@code :78-84}.</li>
 *   <li>{@code reference/assets/thaumcraft/lang/en_US.lang:282}
 *       {@code tile.blockArcaneFurnace.name=Infernal Furnace} — the family's
 *       <b>one</b> name. The original ships no {@code tile.<id>.<meta>.name}
 *       key for this block, so the nine shell metas keep the family label: the
 *       same rule {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockVariantNames}
 *       applies to the metas the original left unnamed.</li>
 * </ul>
 *
 * <p>1.21.1 has no implicit block self-drop, so the original's "the block drops
 * itself, with its metadata" is data: one {@code block_state_property}-gated
 * pool per variant in
 * {@code data/thaumcraft/loot_table/blocks/blockarcanefurnace.json}, each
 * carrying the {@code minecraft:block_state} component so the drop keeps its
 * variant. The third probe reads that in the world for every state the
 * container answers.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneFurnaceShellRegistrationGameTest {

    private ArcaneFurnaceShellRegistrationGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * The nine shell metas are <em>states of the registered block</em>: the
     * state container answers eleven variants, nine of them shell, they map
     * bijectively onto the original's metadata 0..10, and each state carries
     * the original's per-metadata tile map and light.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theshellmetasarestatesoftheregisteredblock(final GameTestHelper helper) {
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();

        final Property<?> property =
                block.getStateDefinition().getProperty(VariantStateItems.VARIANT_PROPERTY);
        helper.assertTrue(property instanceof IntegerProperty,
                "the furnace's " + VariantStateItems.VARIANT_PROPERTY + " property is not an IntegerProperty,"
                        + " so the original-metadata domain is not readable from the state container —"
                        + " registered properties " + block.getStateDefinition().getProperties());
        final List<Integer> variants =
                ((IntegerProperty) property).getPossibleValues().stream().sorted().toList();

        final List<Integer> shell = variants.stream()
                .filter(v -> BlockArcaneFurnace.isShell(stateOf(block, v)))
                .toList();
        helper.assertTrue(shell.size() == 9,
                "the state container answers nine shell metas — the original's metadata 1-9, the 3x3 shell ring"
                        + " of the 3x3x3 (WandManager.replaceArcaneFurnace :457-480) — the block's own "
                        + VariantStateItems.VARIANT_PROPERTY + " property answers " + variants
                        + ", of which shell " + shell);

        // variant -> original metadata is a bijection over the whole container.
        final Set<Integer> originalMetas = new TreeSet<>();
        for (final int variant : variants) {
            originalMetas.add(BlockArcaneFurnace.originalMetaOf(stateOf(block, variant)));
        }
        helper.assertTrue(originalMetas.size() == variants.size(),
                "every variant answers a distinct original metadata (D-001: the original was one block id with"
                        + " eleven metadata — variant 0 = meta 0, variant 1 = meta 10, variants 2..10 = metas"
                        + " 1..9) — " + variants + " map to " + originalMetas);
        helper.assertTrue(originalMetas.equals(new TreeSet<>(IntStream.rangeClosed(0, 10).boxed().toList())),
                "the metadata domain the shipped states cover is the original's 0..10, got " + originalMetas);

        // Each of the nine grid positions is carried by exactly one shell state.
        final TreeSet<Integer> grids = shell.stream()
                .map(v -> BlockArcaneFurnace.shellGridOf(stateOf(block, v)))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(grids.size() == 9
                        && grids.first() == ArcaneFurnaceStructure.GRID_FIRST
                        && grids.last() == ArcaneFurnaceStructure.GRID_LAST,
                "the nine shell states cover grid positions " + ArcaneFurnaceStructure.GRID_FIRST + ".."
                        + ArcaneFurnaceStructure.GRID_LAST + " once each (the atlas index dx + 3*dz + 5 that"
                        + " calculateTexture reads back at :78-84), got " + grids);

        for (final int variant : variants) {
            final BlockState state = stateOf(block, variant);
            final int meta = BlockArcaneFurnace.originalMetaOf(state);
            // :179-182 — the light is the lava plate's and the nozzle's only.
            helper.assertTrue(state.getLightEmission() == (meta == 0 || meta == 10 ? 13 : 0),
                    "metadata " + meta + " answers light " + state.getLightEmission() + " — getLightValue"
                            + " (:179-182) returns 13 for metadata 0 and 10 and super (0) for the shell");
            // :355-362 — the tile map, at registration level (no world needed).
            final BlockArcaneFurnace.TileKind expected = meta == 0 ? BlockArcaneFurnace.TileKind.PLATE
                    : meta == 10 ? BlockArcaneFurnace.TileKind.NOZZLE
                    : (meta == 2 || meta == 4 || meta == 5 || meta == 6 || meta == 8)
                            ? BlockArcaneFurnace.TileKind.NOZZLE
                            : BlockArcaneFurnace.TileKind.NONE;
            helper.assertTrue(BlockArcaneFurnace.tileFor(state) == expected,
                    "metadata " + meta + " must take the " + expected + " tile — createTileEntity (:355-362)"
                            + " names the nozzle tile for 10, 2, 4, 5, 6, 8, NOTHING at all for the corner metas"
                            + " 1, 3, 7, 9 and the furnace tile for 0; the port answers "
                            + BlockArcaneFurnace.tileFor(state) + " for " + state);
            final BlockEntity tile = block.newBlockEntity(BlockPos.ZERO, state);
            switch (expected) {
                case NONE -> helper.assertTrue(tile == null,
                        "metadata " + meta + " is a shell corner and the original gives it no tile at all"
                                + " (createTileEntity :355-362 answers null for 1, 3, 7, 9), got " + tile);
                case NOZZLE -> helper.assertTrue(tile instanceof TileArcaneFurnaceNozzle,
                        "metadata " + meta + " carries the nozzle tile (createTileEntity :355-362), got " + tile);
                case PLATE -> helper.assertTrue(tile instanceof TileArcaneFurnace,
                        "metadata 0 is the base plate and carries the furnace tile (createTileEntity :355-362),"
                                + " got " + tile);
            }
        }

        // The published face-machine properties keep their cardinality, so the
        // blockstate's key space is the whole container (metas x run level x side).
        final int levels = block.getStateDefinition()
                .getProperty(BlockArcaneFurnace.STACK.getName()).getPossibleValues().size();
        final int sides = block.getStateDefinition()
                .getProperty(BlockArcaneFurnace.ATTACHED.getName()).getPossibleValues().size();
        helper.assertTrue(block.getStateDefinition().getPossibleStates().size()
                        == variants.size() * levels * sides,
                "the state container is the full cross product of the registered properties, got "
                        + block.getStateDefinition().getPossibleStates().size() + " states for "
                        + variants.size() + " variants x " + levels + " run levels x " + sides + " sides");
        helper.succeed();
    }

    /**
     * Every state the container answers names a lang key the shipped file
     * carries, so a placed stack of any variant renders a real label and never
     * a raw key. The original names this family <b>once</b>
     * ({@code en_US.lang:282} {@code tile.blockArcaneFurnace.name=Infernal
     * Furnace}) and ships no {@code tile.<id>.<meta>.name} key for it, so all
     * eleven variants resolve the family key and no per-meta key is invented.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyvariantresolvesashippedlabel(final GameTestHelper helper) {
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        final JsonObject lang = loadLang(helper);
        final List<Integer> variants = ((IntegerProperty) block.getStateDefinition()
                .getProperty(VariantStateItems.VARIANT_PROPERTY)).getPossibleValues().stream().sorted().toList();

        final List<String> problems = new ArrayList<>();
        final Set<String> keys = new TreeSet<>();
        for (final int variant : variants) {
            final ItemStack stack = new ItemStack(CatalogEssentiaStorage.ITEM_ARCANE_FURNACE.get());
            VariantStateItems.withVariant(stack, stateOf(block, variant));
            final Component label = stack.getHoverName();
            final String key = label.getContents() instanceof TranslatableContents translatable
                    ? translatable.getKey() : null;
            if (key == null) {
                problems.add("the stack for variant " + variant + " carries no translatable label at all: " + label);
                continue;
            }
            keys.add(key);
            if (!lang.has(key)) {
                problems.add("variant " + variant + " (metadata "
                        + BlockArcaneFurnace.originalMetaOf(stateOf(block, variant)) + ") renders the key "
                        + key + ", which assets/thaumcraft/lang/en_us.json does not carry");
            } else if (lang.get(key).getAsString().isBlank()) {
                problems.add("the shipped label for " + key + " is blank");
            }
        }
        helper.assertTrue(!keys.isEmpty(),
                "no variant of the furnace resolves a translatable label — " + problems);
        helper.assertTrue(keys.equals(Set.of(block.getDescriptionId())),
                "the " + variants.size() + " variants all resolve the family key " + block.getDescriptionId()
                        + " — the original names this block once (en_US.lang:282) and ships no"
                        + " tile.<id>.<meta>.name key for it, so a per-meta key here would be a key the"
                        + " reference does not have; resolved keys " + keys);
        helper.assertTrue(lang.has(block.getDescriptionId())
                        && "Infernal Furnace".equals(lang.get(block.getDescriptionId()).getAsString()),
                "the family label is the original's own wording: en_US.lang:282"
                        + " tile.blockArcaneFurnace.name=Infernal Furnace, got "
                        + (lang.has(block.getDescriptionId()) ? lang.get(block.getDescriptionId()) : "<missing>"));
        helper.assertTrue(problems.isEmpty(), "the furnace's variant labels: " + problems);
        helper.succeed();
    }

    /**
     * Every state the container answers drops itself <b>with its variant</b> —
     * the original's self-drop for all eleven metadata, which in 1.21.1 exists
     * only as a pool of the block loot table. The check runs in the world, one
     * fresh cell per state: a shell cell that drops a base plate loses its grid
     * position, and a nozzle that drops a plate loses the eject direction the
     * whole furnace depends on.
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void everyvariantdropsitselfwithitsvariant(final GameTestHelper helper) {
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        final List<Integer> variants = ((IntegerProperty) block.getStateDefinition()
                .getProperty(VariantStateItems.VARIANT_PROPERTY)).getPossibleValues().stream().sorted().toList();
        helper.assertTrue(variants.size() >= 11,
                "the state container answers at least the original's eleven metadata, got " + variants);

        final List<String> problems = new ArrayList<>();
        int cell = 0;
        for (final int variant : variants) {
            // Fresh cell per state, 6 cells apart: setBlock on the state that is
            // already there keeps the old block entity, and item entities drift
            // into a neighbour's scan box at a tighter pitch.
            final BlockPos local = new BlockPos(2 + 6 * (cell % 4), 1, 2 + 6 * (cell / 4));
            cell++;
            final BlockPos pos = helper.absolutePos(local);
            helper.setBlock(local, stateOf(block, variant));
            helper.getLevel().destroyBlock(pos, true, null);
            final List<ItemStack> drops = dropsAround(helper, pos);
            if (drops.size() != 1) {
                problems.add("variant " + variant + " (metadata "
                        + BlockArcaneFurnace.originalMetaOf(stateOf(block, variant)) + ") broke into "
                        + drops.size() + " items " + drops + " — the loot table needs exactly one"
                        + " block_state_property-gated pool for this variant (an unconditional pool fires for"
                        + " every state and duplicates the drop, and no pool at all drops nothing)");
                continue;
            }
            final ItemStack drop = drops.get(0);
            if (!drop.is(CatalogEssentiaStorage.ITEM_ARCANE_FURNACE.get())) {
                problems.add("variant " + variant + " drops " + drop + ", not the furnace block item");
                continue;
            }
            final Integer carried = VariantStateItems.variantOf(drop);
            if (carried == null || carried.intValue() != variant) {
                problems.add("variant " + variant + " drops itself without its variant (the stack carries "
                        + carried + ") — the pool must set the minecraft:block_state component, which is what"
                        + " keeps a broken shell cell in its grid position");
            }
        }
        helper.assertTrue(problems.isEmpty(), "the furnace's self-drops: " + problems);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** A state of the furnace carrying one value of its {@code variant} property. */
    private static BlockState stateOf(final BlockArcaneFurnace block, final int variant) {
        return block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, variant);
    }

    /** The item entities a block break left in this cell. */
    private static List<ItemStack> dropsAround(final GameTestHelper helper, final BlockPos pos) {
        final AABB box = new AABB(pos).inflate(2.0D);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
                .map(ItemEntity::getItem)
                .toList();
    }

    /** The shipped label file, read off the classpath (the game-test server's own resource). */
    private static JsonObject loadLang(final GameTestHelper helper) {
        try (InputStream in = ArcaneFurnaceShellRegistrationGameTest.class.getResourceAsStream(
                "/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            helper.assertTrue(in != null, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            final String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            final var parsed = JsonParser.parseString(text);
            helper.assertTrue(parsed.isJsonObject(), "en_us.json must be a JSON object");
            return parsed.getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
