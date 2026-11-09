package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockJar;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.render.art.CrystalWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.JarWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.MirrorWorldArt;
import com.jonnyutah12.thaumcraftclassic.render.art.SpaWorldArt;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirror;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * BD-18 slice 4 (t_6563ce50): the world TER art is driven by <b>tile data</b>.
 *
 * <p>1.7.10 computed these five scenes per frame inside tile-entity
 * renderers, straight from tile fields — the jar's fluid height, tint and
 * label from {@code TileJarFillable.amount/aspect/aspectFilter/facing}
 * ({@code TileJarRenderer.java:47-82, 92-131}), the crystal's six shards from
 * a {@code Random} seeded with the tile's own coordinates
 * ({@code TileCrystalRenderer.java:80-101}), the mirror's pane swap and
 * portal from {@code TileMirror.linked} ({@code TileMirrorRenderer.java:575-657}),
 * the spa's tank from {@code TileSpa} ({@code TileSpa.java:39, 65-71}).
 * D-021 routes each scene; every probe below changes the tile data and
 * asserts the plan the renderer consumes changes with it — the appearance,
 * not a registry entry, is the subject.</p>
 *
 * <p>The tablecloth is the odd one out: the original drew it per-frame
 * ({@code TileTableRenderer.java:20-32}) from nothing but the block's
 * metadata, so D-021 routes it to a static model, and its probe reads the
 * shipped blockstate and model JSON (the art the renderer no longer needs).</p>
 *
 * <p>GameTestHelper positions are structure-relative: every read goes
 * through {@link #stateAt}/{@code helper.getBlockEntity} (the probe-bug rule
 * from t_065502ba).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldTERArtGameTest {

    private WorldTERArtGameTest() {
    }

    /**
     * The jar: fill height, tint, label and the sync that carries them all
     * follow the tile, per {@code TileJarRenderer.java:110, 109-116, 54-87}.
     */
    @GameTest(template = "empty")
    public static void jarFluidArtFollowsTheTile(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, CatalogEssentiaStorage.BLOCK_JAR.get().defaultBlockState()
                .setValue(BlockJar.VARIANT, BlockJar.VARIANT_WARDED));

        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof TileJarFillable
                        && blockEntity.getType() == CatalogEssentiaStorage.TILE_JAR.get(),
                "the warded jar must place the fillable tile the art reads, got " + blockEntity
                        + " (placed " + stateAt(helper, pos) + ")");
        final TileJarFillable tile = (TileJarFillable) blockEntity;

        // Empty jar: no fluid, no label — the renderer's own gates
        // (TileJarRenderer.java:48, 52).
        final JarWorldArt.Plan empty = JarWorldArt.plan(tile);
        helper.assertTrue(empty.fluidHeight() == 0.0F,
                "an empty jar draws no fluid (amount 0 -> height 0), got " + empty.fluidHeight());
        helper.assertTrue(empty.label() == null,
                "a jar with no aspectFilter draws no label (TileJarRenderer.java:52)");

        // Fill it: the height formula is the original's, amount/maxAmount * 0.625
        // (TileJarRenderer.java:110), the tint is aspect.getColor() (:109-111).
        final Aspect aqua = Aspect.getAspect("aqua");
        helper.assertTrue(aqua != null, "the aqua aspect must exist for the jar probe");
        tile.addToContainer(aqua, 150);
        helper.assertTrue(tile.amount == 150, "the jar took the essentia, amount is " + tile.amount);
        final JarWorldArt.Plan filled = JarWorldArt.plan(tile);
        helper.assertTrue(Math.abs(filled.fluidHeight() - 150.0F / tile.maxAmount() * 0.625F) < 1.0E-4F,
                "fluid height must be amount/maxAmount * 0.625 (TileJarRenderer.java:110), got "
                        + filled.fluidHeight() + " for amount " + tile.amount);
        helper.assertTrue(filled.fluidArgb() == aqua.getColor(),
                "the fluid tints to the aspect colour (TileJarRenderer.java:115), got "
                        + Integer.toHexString(filled.fluidArgb()) + " want "
                        + Integer.toHexString(aqua.getColor()));
        helper.assertTrue(filled.fluidLight() == 11,
                "the fluid draws at the original's 200/255 floor (TileJarRenderer.java:118-120), got "
                        + filled.fluidLight());

        // The label arrives with the filter and is seeded by the tile's identity
        // (hash of the filter tag, x-coord, facing — TileJarRenderer.java:65).
        tile.aspectFilter = aqua;
        tile.touch();
        final JarWorldArt.Plan labelled = JarWorldArt.plan(tile);
        helper.assertTrue(labelled.label() != null,
                "an aspectFilter must produce a label (TileJarRenderer.java:52)");
        helper.assertTrue(labelled.label().tiltDeg()
                        == JarWorldArt.tiltDeg("aqua", helper.absolutePos(pos).getX(), tile.facing),
                "the label tilt is (filterTag.hashCode() + x + facing) % 4 - 2 (TileJarRenderer.java:65)");
        helper.assertTrue(labelled.label().labelSprite().equals(JarWorldArt.LABEL_SPRITE),
                "the label sprite is the published models/label row (TileJarRenderer.java:72)");
        helper.assertTrue(labelled.label().aspectSprite().getPath().equals("aspects/aqua"),
                "the label icon is the published aspects/aqua texture, got "
                        + labelled.label().aspectSprite());

        // The sync contract: a 1.21.1 TER reads a client tile fed by the
        // update tag, so the tag must carry the art inputs (Amount is the
        // original's writeCustomNBT key, TileJarFillable.java:312).
        final CompoundTag sync = tile.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(sync.getShort("Amount") == 150,
                "the update tag must carry Amount for the client renderer, got " + sync);
        helper.assertTrue("aqua".equals(sync.getString("Aspect")),
                "the update tag must carry the aspect tint source, got " + sync);
        helper.assertTrue("aqua".equals(sync.getString("AspectFilter")),
                "the update tag must carry the label filter, got " + sync);

        // Facing rotates the whole label frame (TileJarRenderer.java:55-61).
        helper.assertTrue(JarWorldArt.facingRotation(3) == 180
                        && JarWorldArt.facingRotation(4) == 270
                        && JarWorldArt.facingRotation(5) == 90
                        && JarWorldArt.facingRotation(2) == 0,
                "label-frame yaw must be 3->180, 4->270, 5->90, else 0 (TileJarRenderer.java:55-61)");
        helper.succeed();
    }

    /**
     * The crystal: the shard geometry is a seeded draw — same tile identity
     * redraws the same cluster, a different cell draws a different one
     * ({@code TileCrystalRenderer.java:80-101}).
     */
    @GameTest(template = "empty")
    public static void crystalShardsAreSeededByThePlacing(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, CatalogDecorativeBlocks.BLOCK_CRYSTAL.get().defaultBlockState()
                .setValue(variantProperty(CatalogDecorativeBlocks.BLOCK_CRYSTAL.get()), 2));

        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof TileCrystal
                        && blockEntity.getType() == CatalogDecorativeBlocks.TILE_CRYSTAL.get(),
                "the crystal must place the orientation tile, got " + blockEntity);
        final TileCrystal tile = (TileCrystal) blockEntity;
        tile.setOrientation(net.minecraft.core.Direction.UP);

        final BlockPos worldPos = helper.absolutePos(pos);
        final var here = CrystalWorldArt.shards(2, worldPos);
        helper.assertTrue(here.size() == 6,
                "a cluster is six cubes: the main shard plus a = 1..5 (TileCrystalRenderer.java:81-101), got "
                        + here.size());
        helper.assertTrue(here.equals(CrystalWorldArt.shards(2, worldPos)),
                "the same tile identity redraws the same shards — the draw is deterministic (seed :78)");
        helper.assertTrue(!here.equals(CrystalWorldArt.shards(2, worldPos.east())),
                "the seed carries the position (metadata + x + y * z, TileCrystalRenderer.java:80): "
                        + "a cluster one cell over must draw different shards");
        helper.assertTrue(!here.equals(CrystalWorldArt.shards(3, worldPos)),
                "the seed carries the metadata: variant 3 must draw a different cluster");

        // The seed consumes in the original's order: shard a's yaw is
        // nextInt(36) + 72 * a, pitch 15 + nextInt(15) (TileCrystalRenderer.java:98, 99).
        for (int a = 1; a < 6; a++) {
            final CrystalWorldArt.Shard shard = here.get(a);
            helper.assertTrue(shard.yawDeg() >= 72 * a && shard.yawDeg() < 72 * a + 36,
                    "shard " + a + " yaw must sit in [72*a, 72*a+35] (TileCrystalRenderer.java:98), got "
                            + shard.yawDeg());
            helper.assertTrue(shard.pitchDeg() >= 15 && shard.pitchDeg() < 30,
                    "shard " + a + " pitch must sit in [15, 29] (TileCrystalRenderer.java:99), got "
                            + shard.pitchDeg());
            helper.assertTrue(shard.scaleY() >= 0.5F * CrystalWorldArt.SHARD_SIZE
                            && shard.scaleY() < 0.6F * CrystalWorldArt.SHARD_SIZE,
                    "shard " + a + " height scale (0.5 + r*0.1) * 0.8 (TileCrystalRenderer.java:56), got "
                            + shard.scaleY());
        }

        // Tints: colors[md + 1], the mixed cluster mains on colors[5] and
        // cycles the six primal tints (TileCrystalRenderer.java:74-76, 89-92).
        helper.assertTrue(CrystalWorldArt.tintFor(2) == 16727041,
                "the water cluster tints colors[3] = 16727041 (BlockCustomOreItem.colors, via :70-72), got "
                        + CrystalWorldArt.tintFor(2));
        helper.assertTrue(CrystalWorldArt.tintFor(6) == 15650047,
                "the mixed cluster mains on colors[5] = 15650047 (TileCrystalRenderer.java:74), got "
                        + CrystalWorldArt.tintFor(6));
        helper.assertTrue(CrystalWorldArt.mixedShardTint(5) == 5592439
                        && CrystalWorldArt.mixedShardTint(1) == 16777086,
                "the mixed cluster's shard tints cycle colors[a], colors[6] for a = 5 "
                        + "(TileCrystalRenderer.java:95)");
        helper.succeed();
    }

    /**
     * The mirror: the pane swaps and the portal appears while the tile's
     * link is live ({@code TileMirrorRenderer.java:577, 594-629}).
     */
    @GameTest(template = "empty")
    public static void mirrorPaneArtFollowsTheLink(final GameTestHelper helper) {
        final BlockPos posA = new BlockPos(1, 3, 1);
        final BlockPos posB = new BlockPos(4, 3, 1);
        final BlockState mirrorState = CatalogFurniture.BLOCK_MIRROR.get().defaultBlockState()
                .setValue(variantProperty(CatalogFurniture.BLOCK_MIRROR.get()), 2);
        helper.setBlock(posA, mirrorState);
        helper.setBlock(posB, mirrorState);

        final TileMirror a = (TileMirror) helper.getBlockEntity(posA);
        final TileMirror b = (TileMirror) helper.getBlockEntity(posB);

        // Unlinked: the opaque pane, no portal quads.
        final MirrorWorldArt.Plan unlinked = MirrorWorldArt.plan(a.isLinkValidSimple(), 0.0F);
        helper.assertTrue(unlinked.paneSprite().equals(MirrorWorldArt.PANE_SPRITE),
                "an unlinked mirror draws mirrorpane (TileMirrorRenderer.java:627-629), got "
                        + unlinked.paneSprite());

        // Link them both ways; the tile's own validation is the art's gate.
        final BlockPos worldA = helper.absolutePos(posA);
        final BlockPos worldB = helper.absolutePos(posB);
        a.linked = true;
        a.linkX = worldB.getX();
        a.linkY = worldB.getY();
        a.linkZ = worldB.getZ();
        a.linkDim = helper.getLevel().dimension();
        b.linked = true;
        b.linkX = worldA.getX();
        b.linkY = worldA.getY();
        b.linkZ = worldA.getZ();
        b.linkDim = helper.getLevel().dimension();

        helper.assertTrue(a.isLinkValidSimple(), "the paired link must validate");
        final MirrorWorldArt.Plan linked = MirrorWorldArt.plan(a.isLinkValidSimple(), 0.0F);
        helper.assertTrue(linked.paneSprite().equals(MirrorWorldArt.PANE_TRANSPARENT_SPRITE),
                "a linked mirror swaps to mirrorpanetrans (TileMirrorRenderer.java:619-621), got "
                        + linked.paneSprite());
        helper.assertTrue(linked.tunnelSprite().equals(MirrorWorldArt.TUNNEL_SPRITE)
                        && linked.fieldSprite().equals(MirrorWorldArt.FIELD_SPRITE),
                "the portal draws the published tunnel + particlefield layers "
                        + "(TileMirrorRenderer.java:27-28, 54-60)");
        helper.assertTrue(linked.paneOffset() == 0.02F + MirrorWorldArt.jitter(a.instability, 1.0F),
                "the pane standoff is 0.02 + random * instability / 10000 (TileMirrorRenderer.java:582, 620)");

        // Break the link at the far end: this tile's validation fails, the
        // art returns to the opaque pane.
        b.linked = false;
        b.setChanged();
        helper.assertTrue(!a.isLinkValidSimple(), "the far end dropping the link breaks validation");
        helper.assertTrue(MirrorWorldArt.plan(a.isLinkValidSimple(), 0.0F).paneSprite()
                        .equals(MirrorWorldArt.PANE_SPRITE),
                "a broken link redraws the opaque pane");

        // The sync contract: linked + coords ride the update tag
        // (TileMirror.java:246-253), so the client pane swap sees them.
        a.setChanged();
        final CompoundTag sync = a.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(sync.getBoolean("linked"),
                "the update tag carries the link flag the pane swap reads, got " + sync);
        helper.assertTrue(sync.getInt("linkX") == worldB.getX() && sync.getInt("linkY") == worldB.getY()
                        && sync.getInt("linkZ") == worldB.getZ(),
                "the update tag carries the link coordinates, got " + sync);
        helper.succeed();
    }

    /**
     * The spa: the water surface follows the tank (new art per BD-18 — the
     * original's spa cube draws no water, {@code BlockStoneDeviceRenderer.java:240-251}).
     */
    @GameTest(template = "empty")
    public static void spaWaterFollowsTheTank(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, CatalogDevices.BLOCK_STONE_DEVICE.get().defaultBlockState()
                .setValue(variantProperty(CatalogDevices.BLOCK_STONE_DEVICE.get()), 11));

        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof TileSpa
                        && blockEntity.getType() == CatalogFurniture.TILE_SPA.get(),
                "stone device variant 11 must place the spa tile (the port numbers the original's "
                        + "meta 12 as variant 11 — CatalogFurniture.wireDeviceTiles, "
                        + "BlockStoneDeviceRenderer.java:240), got " + blockEntity
                        + " (placed " + stateAt(helper, pos) + ")");
        final TileSpa tile = (TileSpa) helper.getBlockEntity(pos);

        helper.assertTrue(!SpaWorldArt.plan(tile.getTankAmount(), SpaWorldArt.TANK_CAPACITY).visible(),
                "an empty spa draws no water, tank is " + tile.getTankAmount());

        tile.fill(Fluids.WATER, 1500, true);
        helper.assertTrue(tile.getTankAmount() == 1500, "the spa took the water, tank is " + tile.getTankAmount());
        final SpaWorldArt.Plan plan = SpaWorldArt.plan(tile.getTankAmount(), SpaWorldArt.TANK_CAPACITY);
        helper.assertTrue(plan.visible(), "a filled spa draws water");
        helper.assertTrue(Math.abs(plan.uScale() - 1500.0F / 5000.0F) < 1.0E-4F,
                "the water covers fill/5000 of the face (TileSpa.java:39, 65-71), got " + plan.uScale());

        tile.drain(1500, true);
        helper.assertTrue(!SpaWorldArt.plan(tile.getTankAmount(), SpaWorldArt.TANK_CAPACITY).visible(),
                "draining the spa hides the water");

        // The sync contract: the tank rides saveAdditional -> update tag.
        tile.fill(Fluids.WATER, 1000, true);
        tile.setChanged();
        final CompoundTag sync = tile.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(sync.getInt("Amount") == 1000,
                "the update tag carries the tank amount the water reads (TileSpa.java:333-334), got " + sync);
        helper.assertTrue("minecraft:water".equals(sync.getString("FluidName")),
                "the update tag names the tank fluid, got " + sync);
        helper.succeed();
    }

    /**
     * The tablecloth: D-021 routes it to a static model (the original's
     * per-frame draw is pure metadata, {@code TileTableRenderer.java:20-32}),
     * so the probe reads the shipped JSON: every table variant has a model
     * key, the cloth geometry names the published {@code models/table}
     * sprite, and variant 1 carries the original's 90° rotation ({@code :27}).
     */
    @GameTest(template = "empty")
    public static void tableclothShipsAsAStaticModel(final GameTestHelper helper) {
        final String blockstate = readResource("/assets/thaumcraft/blockstates/blocktable.json", helper);
        helper.assertTrue(blockstate != null, "the table ships a blockstate");
        for (int variant = 0; variant <= 15; variant++) {
            helper.assertTrue(blockstate.contains("\"variant=" + variant + "\""),
                    "blocktable variant " + variant + " must have a blockstate key (the table carries "
                            + "metas 0-15); blockstates key set must cover the StateDefinition");
        }
        helper.assertTrue(blockstate.contains("\"y\": 90") || blockstate.contains("\"y\":90"),
                "the pair-placed table (variant 1) rotates the cloth 90° (TileTableRenderer.java:27)");

        final String cloth = readResource("/assets/thaumcraft/models/block/blocktable_cloth.json", helper);
        helper.assertTrue(cloth != null, "the tablecloth model ships");
        helper.assertTrue(cloth.contains("thaumcraft:models/table"),
                "the cloth texture is the published models/table row (TileTableRenderer.java:24)");
        int elements = 0;
        for (int i = 0; (i = cloth.indexOf("\"from\"", i)) >= 0; i++) {
            elements++;
        }
        helper.assertTrue(elements >= 4,
                "the cloth model carries the original's four boxes: top, two legs, crossbar "
                        + "(ModelTable.java:10-37), got " + elements + " elements");
        helper.succeed();
    }

    // ------------------------------------------------------------------ plumbing

    private static String readResource(final String path, final GameTestHelper helper) {
        try (final InputStream in = WorldTERArtGameTest.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            helper.fail("reading " + path + " failed: " + e);
            return null;
        }
    }

    private static BlockState stateAt(final GameTestHelper helper, final BlockPos pos) {
        return helper.getLevel().getBlockState(helper.absolutePos(pos));
    }

    private static IntegerProperty variantProperty(final net.minecraft.world.level.block.Block block) {
        final var property = block.getStateDefinition().getProperty("variant");
        if (property instanceof IntegerProperty integer) {
            return integer;
        }
        throw new IllegalStateException("the shipped block registers no integer 'variant' property: "
                + block.getStateDefinition().getProperties());
    }
}
