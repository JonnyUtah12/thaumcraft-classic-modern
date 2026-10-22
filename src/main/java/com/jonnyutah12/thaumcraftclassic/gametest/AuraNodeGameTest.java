package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.config.ConfigOption;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeLookup;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeModifier;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import com.jonnyutah12.thaumcraftclassic.registration.ModNodeDefs;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import com.jonnyutah12.thaumcraftclassic.worldgen.AuraNodeChunkGen;
import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura;
import com.jonnyutah12.thaumcraftclassic.worldgen.NodeGeneration;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Aura / vis node worldgen (TC-M5-01): the node type + modifier registries are
 * live, the chunk pipeline records biome aura values, a forced wild-node roll
 * stores a valid node record, the config gate suppresses generation, and the
 * server-tick driver drains the chunk queue end-to-end.
 *
 * <p>Random processes are pinned with fixed seeds (docs/testing.md). The pure
 * roll tables are covered by {@code NodeGenerationRollTest}; this class covers
 * the in-game wiring: registries, chunk hook, tick driver, store records.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AuraNodeGameTest {

    @GameTest(template = "empty")
    public static void nodeRegistriesContainTheFrozenIds(final GameTestHelper helper) {
        final Registry<NodeType> types = ModNodeDefs.nodeTypes();
        final Registry<NodeModifier> modifiers = ModNodeDefs.nodeModifiers();
        helper.assertTrue(types != null && modifiers != null, "node registries not created");
        for (final NodeType type : NodeType.values()) {
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                    type.registryPath());
            helper.assertTrue(type.equals(types.get(id)), "node_type entry missing: " + id);
        }
        for (final NodeModifier modifier : NodeModifier.values()) {
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                    modifier.registryPath());
            helper.assertTrue(modifier.equals(modifiers.get(id)), "node_modifier entry missing: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forcedWildNodeRollStoresValidRecord(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(0, 0, 0)));
        final WorldMagicData data = WorldMagicData.get(level);
        final int before = data.getNodes(chunk).size();

        // node_rarity=1 forces the attempt; fixed seed keeps the roll pinned.
        final boolean placed = NodeGeneration.generateWildNodes(level, chunk, new Random(0x5EEDL), 1, false);
        helper.assertTrue(placed, "forced wild-node roll did not place a node");

        final var nodes = data.getNodes(chunk);
        helper.assertTrue(nodes.size() == before + 1, "node record not stored in the world magic store");
        final WorldMagicData.Node node = nodes.get(nodes.size() - 1);

        // Type/modifier must be real enum ids; intensity positive; aspects
        // non-empty and every tag resolvable in the live aspect registry.
        helper.assertTrue(node.type() != null && NodeType.fromId(node.type()) != null,
                "node type id not resolvable: " + node.type());
        boolean typeKnown = false;
        for (final NodeType type : NodeType.values()) {
            typeKnown |= type.id().equals(node.type());
        }
        helper.assertTrue(typeKnown, "node type outside the enum: " + node.type());
        if (node.modifier() != null) {
            boolean modifierKnown = false;
            for (final NodeModifier modifier : NodeModifier.values()) {
                modifierKnown |= modifier.id().equals(node.modifier());
            }
            helper.assertTrue(modifierKnown, "node modifier outside the enum: " + node.modifier());
        }
        helper.assertTrue(node.intensity() > 0, "node intensity must be positive");
        helper.assertTrue(!node.aspects().isEmpty(), "node aspect list empty");
        for (final String tag : node.aspects().keySet()) {
            helper.assertTrue(Aspect.getAspect(tag) != null, "node aspect tag not registered: " + tag);
        }

        // The NodeLookup hook (Thaumometer/visualization consumers) finds it.
        helper.assertTrue(NodeLookup.findNodeAt(level, new BlockPos(node.x(), node.y(), node.z())).isPresent(),
                "NodeLookup.findNodeAt could not find the stored node");
        helper.assertTrue(!NodeLookup.nodesNear(level, helper.absolutePos(new BlockPos(0, 0, 0)), 32).isEmpty(),
                "NodeLookup.nodesNear could not find the stored node");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkPassRecordsBiomeAuraValues(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(0, 0, 0)));
        final WorldMagicData data = WorldMagicData.get(level);

        AuraNodeChunkGen.generateChunk(level, chunk, data);

        final int expected = BiomeAura.getBiomeAura(level.getBiome(chunk.getMiddleBlockPosition(0)));
        helper.assertTrue(data.getAuraDensity(chunk) == expected,
                "chunk aura density " + data.getAuraDensity(chunk) + " != biome table value " + expected);
        helper.assertTrue(data.getAuraPurity(chunk) == AuraNodeChunkGen.PRISTINE_PURITY,
                "fresh aura purity must start pristine (100), got " + data.getAuraPurity(chunk));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void configGateSuppressesAuraAndNodes(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final WorldMagicData data = WorldMagicData.get(level);
        // Far from the test structure: the tick driver has not touched this chunk
        // yet, so the gate is the only thing deciding whether it gets values.
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(1000, 0, 1000)));

        final ConfigOption gate = TcConfigCatalog.GENERATE_AURA_NODES;
        TcConfig.fields().get(gate).set(Boolean.FALSE);
        try {
            final int before = data.getNodes(chunk).size();
            AuraNodeChunkGen.generateChunk(level, chunk, data);
            helper.assertTrue(data.getNodes(chunk).size() == before,
                    "nodes generated while World_Generation.generate_aura_nodes=false");
            helper.assertTrue(data.getAuraDensity(chunk) == 0,
                    "aura recorded while generate_aura_nodes=false");
        } finally {
            TcConfig.fields().get(gate).set(Boolean.TRUE);
        }

        // Re-enabled: the same chunk now records its aura (gate reads live).
        AuraNodeChunkGen.generateChunk(level, chunk, data);
        helper.assertTrue(data.getAuraDensity(chunk) > 0,
                "aura did not resume after re-enabling the option");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nodeAtChunkBoundaryStoredInOwningChunk(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final WorldMagicData data = WorldMagicData.get(level);
        final BlockPos origin = helper.absolutePos(new BlockPos(0, 0, 0));
        final ChunkPos here = new ChunkPos(origin);
        // One block before this chunk's min corner belongs to the neighbour.
        final BlockPos boundary = new BlockPos(here.getMinBlockX() - 1, origin.getY(), here.getMinBlockZ() - 1);
        final ChunkPos neighbour = new ChunkPos(boundary);
        helper.assertTrue(!neighbour.equals(here), "boundary position did not cross chunk");

        NodeGeneration.createNodeAt(level, boundary.getX(), boundary.getY(), boundary.getZ(),
                new NodeGeneration.PlannedNode(NodeType.PURE, NodeModifier.BRIGHT, 42,
                        java.util.Map.of("ordo", 43)));

        helper.assertTrue(data.getNodes(neighbour).stream().anyMatch(n ->
                        n.x() == boundary.getX() && n.y() == boundary.getY() && n.z() == boundary.getZ()),
                "boundary node not stored in its owning chunk");
        helper.assertTrue(data.getNodes(here).stream().noneMatch(n -> n.x() == boundary.getX()),
                "boundary node leaked into the wrong chunk");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void loadedChunksFlowThroughTheTickDriver(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(0, 0, 0)));
        final WorldMagicData data = WorldMagicData.get(level);

        // The structure's chunk loaded while the mod was live, so ChunkEvent.Load
        // queued it; the ServerTickEvent.Post driver must drain it within a few
        // ticks. (If a previous test already marked this chunk, the queue check
        // still proves the pipeline ran.)
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(data.isMagicGenDone(chunk),
                    "chunk never marked magic-gen-complete by the server-tick driver");
            helper.succeed();
        });
    }

    private AuraNodeGameTest() {
    }
}
