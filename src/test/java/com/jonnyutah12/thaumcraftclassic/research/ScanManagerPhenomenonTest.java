package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THAUM-1 remainder: the pure half of the scanType-3 phenomena/NODE branch —
 * the phenomenon key the original builds (ItemThaumometer L89 + TileNode
 * L75), the {@code generateNodeAspects} arithmetic (ScanManager L252-292) and
 * the {@code "@"}/{@code "#"} scan gate (L328-333, L376-381). The store,
 * attachment and pool wiring is the game-test probe
 * {@code ThaumometerScanGameTest}.
 */
class ScanManagerPhenomenonTest {

    // Unit tests run without the game's registry event, so Aspect.getAspect is
    // null here; build one instance per tag (Aspect has no equals).
    private static final Map<String, Aspect> ASPECTS = new java.util.HashMap<>();

    private static Aspect aspect(final String tag) {
        return ASPECTS.computeIfAbsent(tag, t -> new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(t))
                .findFirst()
                .orElseThrow()));
    }

    private static final Function<String, Aspect> RESOLVER = ScanManagerPhenomenonTest::aspect;

    private static final Aspect AER = aspect("aer");
    private static final Aspect IGNIS = aspect("ignis");

    // ------------------------------------------------------------------ key shape

    @Test
    void phenomenonKeyIsTheOriginalsFourPartNodeId() {
        // ItemThaumometer L89: "NODE" + ((INode) tile).getId(); TileNode L75
        // builds that id as dimension:x:y:z.
        final String key = ScanManager.nodePhenomenon(Level.OVERWORLD, new BlockPos(100, 64, -200));
        assertEquals("NODEminecraft:overworld:100:64:-200", key);
        assertTrue(key.startsWith(ScanManager.PHENOMENON_NODE));
        assertEquals("minecraft:overworld:100:64:-200",
                ScanManager.stripPhenomenonPrefix(key));
    }

    @Test
    void nodeIdReadsBackToItsDimensionAndPosition() {
        for (final var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            for (final BlockPos pos : List.of(new BlockPos(0, 0, 0), new BlockPos(-1, -64, 4096),
                    new BlockPos(12345, 7, -6789))) {
                final String key = ScanManager.nodePhenomenon(dimension, pos);
                final var parsed = ScanManager.parseNodeKey(ScanManager.stripPhenomenonPrefix(key));
                assertTrue(parsed.isPresent(), "must parse " + key);
                assertEquals(pos, parsed.get().pos(), "position of " + key);
                assertEquals(dimension.location(), parsed.get().dimension(), "dimension of " + key);
            }
        }
    }

    @Test
    void malformedNodeIdsReadAsAbsent() {
        // The pre-port id form (a bare numeric dimension) has no modern
        // dimension to match, so it resolves nowhere: no crash, no scan.
        assertTrue(ScanManager.parseNodeKey("0:100:64:-200").get().dimension()
                .equals(ResourceLocation.parse("minecraft:0")));
        assertTrue(ScanManager.parseNodeKey("").isEmpty());
        assertTrue(ScanManager.parseNodeKey(null).isEmpty());
        assertTrue(ScanManager.parseNodeKey("minecraft:overworld").isEmpty());
        assertTrue(ScanManager.parseNodeKey("minecraft:overworld:x:y:z").isEmpty());
        assertEquals("", ScanManager.stripPhenomenonPrefix(null));
    }

    // ----------------------------------------------------------- node aspects

    @Test
    void containerAspectsFloorAtFourAndTakeATenthAbove() {
        // ScanManager L265-267: merge(a, max(4, getAmount(a) / 10)).
        final AspectList tags = ScanManager.nodeAspects(
                new AspectList().add(AER, 100).add(IGNIS, 5), NodeType.NORMAL, RESOLVER);
        assertNotNull(tags);
        assertEquals(10, tags.getAmount(AER));
        assertEquals(4, tags.getAmount(IGNIS));
    }

    @Test
    void nodeTypeSignaturesMatchTheOriginalSwitch() {
        final AspectList empty = new AspectList();
        assertEquals(4, ScanManager.nodeAspects(empty, NodeType.UNSTABLE, RESOLVER)
                .getAmount(aspect("perditio")), "UNSTABLE merges ENTROPY 4 (L269-271)");
        assertEquals(4, ScanManager.nodeAspects(empty, NodeType.HUNGRY, RESOLVER)
                .getAmount(aspect("fames")), "HUNGRY merges HUNGER 4 (L273-274)");
        assertEquals(4, ScanManager.nodeAspects(empty, NodeType.TAINTED, RESOLVER)
                .getAmount(aspect("vitium")), "TAINTED merges TAINT 4 (L275-276)");
        final AspectList pure = ScanManager.nodeAspects(empty, NodeType.PURE, RESOLVER);
        assertEquals(2, pure.getAmount(aspect("sano")), "PURE merges HEAL 2 (L278-280)");
        assertEquals(2, pure.getAmount(aspect("ordo")), "PURE adds ORDER 2");
        final AspectList dark = ScanManager.nodeAspects(empty, NodeType.DARK, RESOLVER);
        assertEquals(2, dark.getAmount(aspect("mortuus")), "DARK merges DEATH 2 (L283-285)");
        assertEquals(2, dark.getAmount(aspect("tenebrae")), "DARK adds DARKNESS 2");
        assertNull(ScanManager.nodeAspects(empty, NodeType.NORMAL, RESOLVER),
                "an empty NORMAL node has no aspects, and L291 reads that as null");
    }

    @Test
    void mergeKeepsTheHigherAndAddAccumulatesExactlyAsTheOriginalWritesThem() {
        // AspectList.merge keeps the higher of old/new, add accumulates
        // (api/aspects/AspectList L?, cited by ScanManager L265-286: the
        // container loop merges, PURE merges HEAL then ADDS ORDER, DARK merges
        // DEATH then ADDS DARKNESS). So an UNSTABLE container that already
        // carries ENTROPY keeps the container value, while a DARK container
        // that already carries DARKNESS gains the +2.
        final Aspect vitium = aspect("vitium");
        final AspectList tainted = ScanManager.nodeAspects(new AspectList().add(vitium, 100),
                NodeType.TAINTED, RESOLVER);
        assertEquals(10, tainted.getAmount(vitium), "100/10 merges over the +4, keeping 10");

        final Aspect tenebrae = aspect("tenebrae");
        final AspectList dark = ScanManager.nodeAspects(new AspectList().add(tenebrae, 100),
                NodeType.DARK, RESOLVER);
        assertEquals(12, dark.getAmount(tenebrae), "add(tenebrae, 2) accumulates over the container's 10");
        assertEquals(2, dark.getAmount(aspect("mortuus")));
    }

    @Test
    void recordFormDerivesTheTypeFromTheStoredId() {
        final AspectList tags = ScanManager.nodeAspects(
                new WorldMagicData.Node(1, 2, 3, "dark", null, 5, Map.of("aer", 40)), RESOLVER);
        assertEquals(4, tags.getAmount(AER), "40 / 10 = 4");
        assertEquals(2, tags.getAmount(aspect("mortuus")), "the record's type string drives the signature");
        assertEquals(2, tags.getAmount(aspect("tenebrae")));
    }

    // ---------------------------------------------------------------- scan gate

    private static PlayerKnowledgeData knowledge() {
        return new PlayerKnowledgeData(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void theScanGateIsPerPrefixAndTheScannedCheckIsEitherPrefix() {
        final String phenomena = ScanManager.nodePhenomenon(Level.OVERWORLD, new BlockPos(1, 2, 3));
        final PlayerKnowledgeData fresh = knowledge();
        assertTrue(ScanManager.isValidScanTarget(fresh, ScanManager.PREFIX_THAUMOMETER, phenomena));
        assertFalse(ScanManager.hasBeenScanned(fresh, phenomena));

        // The original stores prefix + phenomena (L438) and the thaumometer
        // gate is the same prefix (L330); hasBeenScanned accepts either (L378).
        final PlayerKnowledgeData byThaumometer = fresh.withScannedPhenomena("@" + phenomena);
        assertFalse(ScanManager.isValidScanTarget(byThaumometer, ScanManager.PREFIX_THAUMOMETER, phenomena));
        assertTrue(ScanManager.hasBeenScanned(byThaumometer, phenomena));

        final PlayerKnowledgeData byRegistry = fresh.withScannedPhenomena("#" + phenomena);
        assertFalse(ScanManager.isValidScanTarget(byRegistry, ScanManager.PREFIX_REGISTRY, phenomena));
        assertTrue(ScanManager.hasBeenScanned(byRegistry, phenomena));

        // A registry entry does not consume the thaumometer gate — L329-330
        // looks up only the requested prefix, which is the original's
        // scannedByThaumometer rule at L390.
        assertTrue(ScanManager.isValidScanTarget(byRegistry, ScanManager.PREFIX_THAUMOMETER, phenomena));

        assertFalse(ScanManager.isValidScanTarget(null, ScanManager.PREFIX_THAUMOMETER, phenomena));
        assertFalse(ScanManager.hasBeenScanned(null, phenomena));
        assertFalse(ScanManager.hasBeenScanned(fresh, null));
    }

    @Test
    void scanTypesAreTheOriginalsNumbering() {
        assertEquals(3, ScanManager.SCAN_TYPE_PHENOMENA);
        assertFalse(ScanManager.PREFIX_THAUMOMETER.equals(ScanManager.PREFIX_REGISTRY));
    }
}
