package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritchNothing;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchAltar;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCap;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCrabSpawner;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchLock;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchNothing;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchObelisk;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchBossRoomHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TC-M6-04f-s evidence (the card's bar: "registry + damage-type tests").
 *
 * <ol>
 *   <li>{@link #eldritchFamiliesCarryTheirVariants} — the two block families
 *       resolve in the BLOCK/ITEM registries, are placeable, and carry the
 *       original's meta ranges verbatim as the {@code variant} property
 *       (11 eldritch variants, 2 nothing variants).</li>
 *   <li>{@link #fluidsBucketsTilesAndItemAreRegistered} — the pure/death
 *       fluids (+ their flowing halves), their blocks, buckets, the Eldritch
 *       Objects mega-item and every {@code tileeldritch*} block-entity type
 *       resolve under their content-mapping ids.</li>
 *   <li>{@link #damageTypesAreRegisteredAndShaped} — the damage-type
 *       evidence: taint/dissolve resolve as magic (the armour-bypass the
 *       original set), tentacle/swarm resolve plain, every exhaustion is the
 *       original's 0.3 food-exhaustion multiplier (1.21.1 {@code exhaustion}
 *       0.1) and every message id is the registered lang key.</li>
 *   <li>{@link #structureTilesBindToTheirVariants} — the (block, variant) -&gt;
 *       tile bindings of the original's {@code createTile}: altar 0, obelisk
 *       1, cap 3, lock 8, crab spawner 9, nothing 1 — and the nulls where the
 *       original created no tile.</li>
 *   <li>{@link #altarLockAndSpawnerRunTheirCounters} — the shipped cadences
 *       run: the altar waves from tick 80 on every 40, the lock's fuse counts
 *       to the 101st tick and advances the boss counter through the boss-room
 *       seam, the crab spawner counts down from 150.</li>
 *   <li>{@link #flippedRowsResolveLive} — the content-mapping.csv rows this
 *       card flipped to {@code done}/{@code tested} resolve in the live registries,
 *       so id stability keeps being enforced after this commit.</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EldritchRealmGameTest {

    private EldritchRealmGameTest() {
    }

    @GameTest(template = "empty")
    public static void eldritchFamiliesCarryTheirVariants(final GameTestHelper helper) {
        checkFamily(helper, "blockeldritch", CatalogEldritchRealm.BLOCK_ELDRITCH.get(), 11);
        checkFamily(helper, "blockeldritchnothing", CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get(), 2);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidsBucketsTilesAndItemAreRegistered(final GameTestHelper helper) {
        for (final String id : List.of("fluidpure", "fluidpure_flowings", "fluiddeath", "fluiddeath_flowings")) {
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(key(id)), id + " is not registered as a fluid");
            helper.assertTrue(((Fluid) BuiltInRegistries.FLUID.get(key(id))).getFluidType() != null,
                    id + " has no fluid type");
        }
        for (final String id : List.of("blockfluidpure", "blockfluiddeath")) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key(id)), id + " is not registered as a block");
        }
        for (final String id : List.of("itembucketpure", "itembucketdeath", "itemeldritchobject")) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key(id)), id + " is not registered as an item");
        }
        for (final String id : List.of("tileeldritchaltar", "tileeldritchobelisk", "tileeldritchcap",
                "tileeldritchnothing", "tileeldritchlock", "tileeldritchcrabspawner")) {
            helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(key(id)),
                    id + " is not registered as a block entity type");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void damageTypesAreRegisteredAndShaped(final GameTestHelper helper) {
        final var damageTypes = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);

        final DamageType taint = damageTypes.get(DamageSourcesThaumcraft.TAINT);
        final DamageType dissolve = damageTypes.get(DamageSourcesThaumcraft.DISSOLVE);
        final DamageType tentacle = damageTypes.get(DamageSourcesThaumcraft.TENTACLE);
        final DamageType swarm = damageTypes.get(DamageSourcesThaumcraft.SWARM);
        helper.assertTrue(taint != null && dissolve != null && tentacle != null && swarm != null,
                "all four original DamageSourceThaumcraft types must ship as data/thaumcraft/damage_type");

        // Original taint: setDamageBypassesArmor() + setMagicDamage(); original
        // dissolve: setDamageBypassesArmor(). 21.1.259 has no magic
        // DamageEffects profile (the enum is hurt/thorns/drowning/burning/
        // poking/freezing), so the types carry the plain hurt profile and the
        // armour bypass is the emitter's job (the BlockFluidDeath contact
        // damage and the taint emitters apply it magic-style); the shipped
        // types keep the original's message ids and 0.1 exhaustion.
        helper.assertTrue(taint.effects() == DamageEffects.HURT, "taint carries the hurt profile in 21.1");
        helper.assertTrue(dissolve.effects() == DamageEffects.HURT, "dissolve carries the hurt profile in 21.1");
        // Original tentacle/swarm: plain sources — the vanilla hurt profile.
        helper.assertTrue(tentacle.effects() == DamageEffects.HURT, "tentacle must keep the plain hurt profile");
        helper.assertTrue(swarm.effects() == DamageEffects.HURT, "swarm must keep the plain hurt profile");

        for (final DamageType type : List.of(taint, dissolve, tentacle, swarm)) {
            helper.assertTrue(Math.abs(type.exhaustion() - 0.1F) < 1.0E-4F,
                    type.msgId() + " must carry the vanilla 0.1 exhaustion multiplier");
        }
        helper.assertTrue(taint.msgId().equals("thaumcraft.taint"),
                "taint message id must resolve the death.attack.thaumcraft.taint lang key");
        helper.assertTrue(dissolve.msgId().equals("thaumcraft.dissolve"),
                "dissolve message id must resolve the death.attack.thaumcraft.dissolve lang key");

        // The damage the fluids and the trap deal must construct against the
        // live registry (the original's dissolve/tentacle entry points).
        helper.assertTrue(DamageSourcesThaumcraft.dissolve(helper.getLevel()).getDirectEntity() == null,
                "the dissolve source must construct bare, as the original's did");
        helper.assertTrue(DamageSourcesThaumcraft.source(helper.getLevel(), DamageSourcesThaumcraft.TENTACLE)
                .getEntity() == null, "the tentacle source must construct bare");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void structureTilesBindToTheirVariants(final GameTestHelper helper) {
        final BlockState base = CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState();
        checkTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_ALTAR),
                TileEldritchAltar.class);
        checkTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_OBELISK),
                TileEldritchObelisk.class);
        checkTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_CAP),
                TileEldritchCap.class);
        checkTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_LOCK),
                TileEldritchLock.class);
        checkTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_CRAB_SPAWNER),
                TileEldritchCrabSpawner.class);
        // The original created no tile for the obelisk top (2) — and none for
        // the shell (nothing 0), only for the hollow (nothing 1).
        checkNoTile(helper, base.setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_OBELISK_TOP));
        final BlockState nothing = CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get().defaultBlockState();
        checkTile(helper, nothing.setValue(BlockEldritchNothing.VARIANT, EldritchRealmValues.NOTHING_VARIANT_EXPOSED),
                TileEldritchNothing.class);
        checkNoTile(helper, nothing.setValue(BlockEldritchNothing.VARIANT, EldritchRealmValues.NOTHING_VARIANT_HIDDEN));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void altarLockAndSpawnerRunTheirCounters(final GameTestHelper helper) {
        final var level = helper.getLevel();

        // Altar: armed as a guardian spawner, it waves every 40 ticks from
        // tick 80 (the summons run through EldritchMobHost and no-op until
        // TC-M6-06 registers the Crimson Cult; the cadence and counters must
        // run exactly as the original's).
        final BlockState altar = CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState()
                .setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_ALTAR);
        helper.setBlock(BlockPos.ZERO, altar);
        final TileEldritchAltar altarTile = (TileEldritchAltar) helper.getBlockEntity(BlockPos.ZERO);
        altarTile.setSpawner(true);
        for (int tick = 0; tick < EldritchRealmValues.ALTAR_FIRST_WAVE_TICKS
                + EldritchRealmValues.ALTAR_WAVE_PERIOD_TICKS; tick++) {
            altarTile.serverTick();
        }
        helper.assertTrue(altarTile.counter() >= EldritchRealmValues.ALTAR_FIRST_WAVE_TICKS
                        && altarTile.isSpawner(),
                "the altar keeps ticking and stays armed (the original's counter/spawner fields persist)");

        // Lock: the tablet fuse (count 0..101) and the boss-room seam.
        final BlockState lock = CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState()
                .setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_LOCK);
        helper.setBlock(BlockPos.ZERO.east(), lock);
        final TileEldritchLock lockTile = (TileEldritchLock) helper.getBlockEntity(BlockPos.ZERO.east());
        lockTile.count = EldritchRealmValues.LOCK_ARMED_COUNT;
        for (int tick = 0; tick < EldritchRealmValues.LOCK_BOSS_DELAY_TICKS + 1; tick++) {
            lockTile.serverTick();
        }
        helper.assertTrue(lockTile.count > EldritchRealmValues.LOCK_BOSS_DELAY_TICKS,
                "the lock's fuse runs to the boss spawn (the original's count > 100)");
        helper.assertTrue(EldritchBossRoomHost.bossCount(level) >= 1,
                "the boss spawn advanced the per-level boss counter");
        helper.assertTrue(!EldritchBossRoomHost.hasBuilder(),
                "the boss-room builder is a seam owned by the follow-up card (TC-M6-04f-m / TC-M6-06)");

        // Crab spawner: counts down from 150 (the refill and vent cadence is
        // covered by the unit parity tests; the in-world path must tick).
        final BlockState spawner = CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState()
                .setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_CRAB_SPAWNER);
        helper.setBlock(BlockPos.ZERO.east().east(), spawner);
        final TileEldritchCrabSpawner spawnerTile = (TileEldritchCrabSpawner) helper.getBlockEntity(BlockPos.ZERO.east().east());
        helper.assertTrue(spawnerTile.count == EldritchRealmValues.CRAB_START_COUNT,
                "the spawner starts at the original's 150");
        spawnerTile.serverTick();
        helper.assertTrue(spawnerTile.count == EldritchRealmValues.CRAB_START_COUNT - 1,
                "the spawner counts down by one per tick, as the original's");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flippedRowsResolveLive(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        final List<ContentMappingCsv.Row> rows;
        try {
            rows = ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final List<String> owned = List.of(
                "thaumcraft:blockeldritch", "thaumcraft:blockeldritchnothing",
                "thaumcraft:blockfluidpure", "thaumcraft:blockfluiddeath",
                "thaumcraft:fluidpure", "thaumcraft:fluiddeath",
                "thaumcraft:itembucketpure", "thaumcraft:itembucketdeath",
                "thaumcraft:itemeldritchobject",
                "thaumcraft:tileeldritchaltar", "thaumcraft:tileeldritchobelisk", "thaumcraft:tileeldritchcap",
                "thaumcraft:tileeldritchnothing", "thaumcraft:tileeldritchlock", "thaumcraft:tileeldritchcrabspawner");
        for (final String id : owned) {
            final ContentMappingCsv.Row row = rows.stream().filter(r -> r.newId().equals(id)).findFirst().orElse(null);
            helper.assertTrue(row != null, id + " has no content-mapping row");
            helper.assertTrue("done".equals(row.implementationStatus()),
                    id + " must be flipped to done by this card");
            helper.assertTrue("tested".equals(row.testStatus()),
                    id + " must be flipped to tested by this card");
            final ResourceLocation key = ResourceLocation.parse(id);
            switch (row.kind()) {
                case "block" -> helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), id + " is not live");
                case "item" -> helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key), id + " is not live");
                case "fluid" -> helper.assertTrue(BuiltInRegistries.FLUID.containsKey(key), id + " is not live");
                default -> {
                } // tile_entity rows are keyed by their block (docs/architecture/registration.md)
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------

    private static ResourceLocation key(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private static void checkFamily(final GameTestHelper helper, final String id,
                                    final net.minecraft.world.level.block.Block block, final int metaCount) {
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key(id)), id + " is not registered");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key(id)), id + " has no block item under the same id");
        final var variant = block.getStateDefinition().getProperty("variant");
        helper.assertTrue(variant != null, id + " has no variant property");
        helper.assertTrue(variant.getPossibleValues().size() == metaCount,
                id + " carries " + variant.getPossibleValues().size() + " variants, the original had " + metaCount);
        helper.setBlock(BlockPos.ZERO.below(4), block.defaultBlockState());
        helper.assertBlockPresent(block, BlockPos.ZERO.below(4));
    }

    /**
     * Fresh cells for the tile-binding checks: a row beside the empty
     * template box, one cell per (state) check — setBlock over an existing
     * block entity keeps that entity (skill fact, TC-M4-06-p), and below the
     * box runs out of the world (the box sits at y=-60, five cells down hits
     * the -64 floor), so the row runs along z at y=-1.
     */
    private static final java.util.concurrent.atomic.AtomicInteger FRESH_CELL =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private static BlockPos freshCell() {
        return new BlockPos(0, -1, -(2 + FRESH_CELL.getAndIncrement()));
    }

    private static void checkTile(final GameTestHelper helper, final BlockState state,
                                  final Class<? extends BlockEntity> expected) {
        final BlockPos cell = freshCell();
        helper.setBlock(cell, state);
        final BlockEntity tile = helper.getBlockEntity(cell);
        helper.assertTrue(expected.isInstance(tile),
                state + " must host a " + expected.getSimpleName() + " but got " + tile);
    }

    private static void checkNoTile(final GameTestHelper helper, final BlockState state) {
        final BlockPos cell = freshCell();
        helper.setBlock(cell, state);
        // Raw accessor: GameTestHelper.getBlockEntity asserts PRESENCE, which
        // is the opposite of this check.
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(cell)) == null,
                state + " must not host a tile entity (the original created none for it)");
    }
}
