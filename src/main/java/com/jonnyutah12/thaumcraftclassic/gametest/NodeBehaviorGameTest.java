package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeReveal;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeRuntime;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeWandTap;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.world.NodeTickDriver;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Node behaviour engine end-to-end (TC-M5-02; design record:
 * docs/architecture/node-behavior.md). Covers what the pure unit tests cannot:
 * the driver's record→runtime→record loop against the live store and aspect
 * registry, the production/degradation/death cycle observed over simulated
 * ticks, the wand tap persisting through the store, and the reveal gate against
 * a registered item. Random comes from the level's pinned game-test seed; the
 * probabilistic branches are pinned in {@code NodeBehaviorMathTest}.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NodeBehaviorGameTest {

    private static int sum(final Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static WorldMagicData.Node findNode(final WorldMagicData data, final ChunkPos chunk,
            final BlockPos pos) {
        for (final WorldMagicData.Node node : data.getNodes(chunk)) {
            if (node.x() == pos.getX() && node.y() == pos.getY() && node.z() == pos.getZ()) {
                return node;
            }
        }
        return null;
    }

    @GameTest(template = "empty")
    public static void driverMaterialisesRuntimeAndTapPersistsToStore(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final ChunkPos chunk = new ChunkPos(pos);
        final WorldMagicData data = WorldMagicData.get(level);

        data.addNode(chunk, new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                Map.of("aer", 10, "ignis", 10)));

        // Pass 1: the driver reconciles the record into a live runtime.
        NodeTickDriver.runNodePass(level);
        final NodeRuntime runtime = NodeTickDriver.runtime(level, pos);
        helper.assertTrue(runtime != null, "driver did not materialise a runtime for the stored record");
        helper.assertTrue(runtime.getAspects().visSize() == 20,
                "runtime container does not match the record");

        // Three taps of 1 vis each (TileNode L403-427 via NodeWandTap).
        VisPool pool = VisPool.EMPTY;
        final Random random = new Random(0x5EEDL);
        for (int i = 0; i < 3; i++) {
            pool = NodeWandTap.tap(runtime, pool, 5000, 1, false, random).pool();
        }
        helper.assertTrue(sum(pool.centiVis()) == 300,
                "three taps must store 300 centi-vis, got " + sum(pool.centiVis()));
        helper.assertTrue(runtime.getAspects().visSize() == 17,
                "node must have lost exactly 3 vis, got " + runtime.getAspects().visSize());

        // Pass 2: the dirty runtime writes back through replaceNode.
        NodeTickDriver.runNodePass(level);
        final WorldMagicData.Node stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null, "node record vanished from the store");
        helper.assertTrue(sum(stored.aspects()) == 17,
                "store did not receive the drained container: " + stored.aspects());
        helper.assertTrue(stored.aspectsBase() != null && sum(stored.aspectsBase()) == 20,
                "vis base must persist alongside the container: " + stored.aspectsBase());

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void productionAndDeathSpiralCycleObservedOverSimulatedTicks(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final ChunkPos chunk = new ChunkPos(pos);
        final WorldMagicData data = WorldMagicData.get(level);

        // A fully drained node with a vis base of 5+5: production, then the
        // death spiral, then death and record removal — the full TileNode
        // lifecycle driven purely through the store.
        data.addNode(chunk, new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                Map.of("aer", 0, "ignis", 0), Map.of("aer", 5, "ignis", 5), 0L));

        NodeTickDriver.runNodePass(level); // creates the runtime (count -> 1)
        helper.assertTrue(NodeTickDriver.runtime(level, pos) != null, "runtime not materialised");

        // Production: the first unit appears exactly at count 600 (TileNode L763).
        for (int tick = 2; tick <= 600; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        WorldMagicData.Node stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null && sum(stored.aspects()) == 1,
                "expected exactly one produced vis at tick 600, got "
                        + (stored == null ? "dead" : stored.aspects()));

        // Death spiral at count 1200 (TileNode L707-748): exactly one aspect is
        // empty (the other holds the unit produced at 600), so the vis base
        // drops by exactly one — whether the 1/20 removal roll also fires or not.
        for (int tick = 601; tick <= 1200; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null, "node died too early");
        final int baseSum = sum(stored.aspectsBase());
        helper.assertTrue(baseSum == 9,
                "death spiral must degrade the empty aspect's base by exactly one, got " + baseSum);
        // Production at 1200 refills one unit unless the removal roll also
        // downgraded the modifier to PALE (level RNG): either 1 or 2 held.
        final int held = sum(stored.aspects());
        helper.assertTrue(held == 1 || held == 2,
                "unexpected held vis after the 1200 spiral: " + stored.aspects());

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyNodeDiesAndIsRemovedFromStore(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        final ChunkPos chunk = new ChunkPos(pos);
        final WorldMagicData data = WorldMagicData.get(level);

        // A node whose every aspect sits at base 0 with an empty container: the
        // 1200-tick spiral removes one zero-base aspect per pass (TileNode
        // L713-726, forced by the base <= 0 branch), and the emptied node dies
        // and loses its record (L749-758).
        data.addNode(chunk, new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                Map.of("aer", 0, "ignis", 0), Map.of("aer", 0, "ignis", 0), 0L));

        for (int tick = 1; tick <= 1200; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        WorldMagicData.Node stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null && stored.aspects().size() == 1,
                "first zero-base aspect must be removed at the 1200 spiral: "
                        + (stored == null ? "already dead" : stored.aspects()));

        for (int tick = 1201; tick <= 2400; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        helper.assertTrue(findNode(data, chunk, pos) == null,
                "emptied node must be removed from the store at the 2400 spiral");
        helper.assertTrue(NodeTickDriver.runtime(level, pos) == null,
                "dead node runtime must be dropped from the driver");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void revealGateFollowsTheOriginalSlots(final GameTestHelper helper) {
        try {
            final ItemStack probe = new ItemStack(TestProbeContent.PROBE_ITEM.get());
            final ItemStack empty = ItemStack.EMPTY;

            // Nothing registered: no reveal (TileNodeRenderer L187-197 default).
            helper.assertTrue(!NodeReveal.canSeeNodes(probe, empty, null),
                    "unregistered items must not reveal nodes");

            // Held thaumometer branch (TileNodeRenderer L191-196).
            NodeReveal.registerThaumometer(probe.getItem());
            helper.assertTrue(NodeReveal.canSeeNodes(probe, empty, null),
                    "held thaumometer must reveal nodes");
            helper.assertTrue(!NodeReveal.canSeeNodes(empty, probe, null),
                    "a thaumometer in the helmet slot must not reveal nodes");

            // Helmet IRevealer branch, honouring showNodes (TileNodeRenderer L187-190).
            NodeReveal.registerRevealer(probe.getItem(), (stack, wearer) -> true);
            helper.assertTrue(NodeReveal.canSeeNodes(empty, probe, null),
                    "revealing helmet must reveal nodes");
            NodeReveal.registerRevealer(probe.getItem(), (stack, wearer) -> false);
            helper.assertTrue(!NodeReveal.canSeeNodes(empty, probe, null),
                    "showNodes=false must not reveal nodes");
        } finally {
            NodeReveal.clear();
        }
        helper.succeed();
    }
}
