package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockManaPod;
import com.jonnyutah12.thaumcraftclassic.items.ItemNugget;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.items.ItemShard;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.tiles.TileManaPod;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-04a probes: the multi-meta resource families round-trip through the
 * save path with their {@code item_meta} component, the oredict item tags
 * named by docs/content-mapping.csv resolve and contain their owner items,
 * and the mana pod's growth + bean-drop semantics (original
 * {@code TileManaPod.checkGrowth} / {@code BlockManaPod.getDrops}) hold
 * against the real loot-table path.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogResourcesGameTest {

    private CatalogResourcesGameTest() {
    }

    private static List<ContentMappingCsv.Row> readContract(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        try {
            return ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GameTest(template = "empty")
    public static void metaFamilyStacksRoundTrip(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        for (final int meta : List.of(ItemResource.META_ALUMENTUM, ItemResource.META_THAUMIUM_INGOT,
                ItemResource.META_QUICKSILVER, ItemResource.META_TALLOW, ItemResource.META_AMBER,
                ItemResource.META_KNOWLEDGE_FRAGMENT, ItemResource.META_JAR_LABEL,
                ItemResource.META_SALIS_MUNDUS, ItemResource.META_PRIMAL_CHARM,
                ItemResource.META_VOID_INGOT, ItemResource.META_GOLD_COIN)) {
            final ItemStack stack = ItemResource.stack(meta);
            final CompoundTag nbt = (CompoundTag) stack.save(registries);
            final ItemStack back = ItemStack.parseOptional(registries, nbt);
            helper.assertTrue(!back.isEmpty() && back.is(CatalogResources.ITEM_RESOURCE.get())
                            && ItemResource.storedMeta(back) == meta,
                    "ItemResource meta " + meta + " did not survive the save round-trip");
            helper.assertTrue(nameKey(back).equals("item.thaumcraft.itemresource." + meta),
                    "ItemResource meta " + meta + " does not resolve its per-meta name key");
        }
        // Original getItemStackLimit: primal charm never stacks.
        helper.assertTrue(ItemResource.stack(ItemResource.META_PRIMAL_CHARM).getMaxStackSize() == 1,
                "primal charm must have a max stack size of 1");

        for (int meta = 0; meta < ItemShard.META_COUNT; meta++) {
            final ItemStack stack = ItemShard.stack(meta);
            final CompoundTag nbt = (CompoundTag) stack.save(registries);
            final ItemStack back = ItemStack.parseOptional(registries, nbt);
            helper.assertTrue(!back.isEmpty() && back.is(CatalogResources.ITEM_SHARD.get())
                            && back.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == meta,
                    "ItemShard meta " + meta + " did not survive the save round-trip");
        }

        for (final int meta : ItemNugget.VALID_METAS) {
            final ItemStack stack = ItemNugget.stack(meta);
            final CompoundTag nbt = (CompoundTag) stack.save(registries);
            final ItemStack back = ItemStack.parseOptional(registries, nbt);
            helper.assertTrue(!back.isEmpty() && back.is(CatalogResources.ITEM_NUGGET.get())
                            && back.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == meta,
                    "ItemNugget meta " + meta + " did not survive the save round-trip");
        }

        helper.succeed();
    }

    private static String nameKey(final ItemStack stack) {
        final var contents = stack.getItem().getName(stack).getContents();
        return contents instanceof TranslatableContents translatable
                ? translatable.getKey() : String.valueOf(contents);
    }

    @GameTest(template = "empty")
    public static void oredictTagsResolveFromContract(final GameTestHelper helper) {
        final Pattern stackClass = Pattern.compile("stack=(\\w+)");
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final var itemLookup = registries.lookupOrThrow(Registries.ITEM);

        int checked = 0;
        for (final ContentMappingCsv.Row row : readContract(helper)) {
            if (!"oredict".equals(row.kind()) || !"done".equals(row.implementationStatus())) {
                continue;
            }
            final Matcher matcher = stackClass.matcher(row.notes());
            if (!matcher.find()) {
                continue;
            }
            final String ownerClass = matcher.group(1);
            if (!List.of("ItemResource", "ItemShard", "ItemNugget").contains(ownerClass)) {
                continue; // owned by another catalog card (blockCustomOre, etc.)
            }
            final TagKey<Item> tag =
                    TagKey.create(Registries.ITEM, ResourceLocation.parse(row.newId()));
            final Optional<HolderSet.Named<Item>> holderSet = itemLookup.get(tag);
            helper.assertTrue(holderSet.isPresent() && !holderSet.get().isEmpty(),
                    "oredict tag " + row.newId() + " is marked done but does not resolve to a non-empty tag");

            final ResourceLocation ownerId =
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, ownerClass.toLowerCase());
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ownerId),
                    "oredict tag " + row.newId() + " owner item " + ownerId + " is not registered");
            final Item owner = BuiltInRegistries.ITEM.get(ownerId);
            helper.assertTrue(new ItemStack(owner).is(tag),
                    "owner item " + ownerId + " does not match its oredict tag " + row.newId());
            checked++;
        }
        helper.assertTrue(checked == 26,
                "expected 26 TC-M6-04a oredict rows to be done and resolvable, got " + checked);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void manaPodGrowsAndDropsAspectBeans(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockState base = CatalogResources.BLOCK_MANA_POD.get().defaultBlockState();

        helper.setBlock(1, 1, 1, base);
        final TileManaPod pod = helper.getBlockEntity(pos);

        // Original TileManaPod.checkGrowth: age advances, and the aspect
        // seeded by planting (here setAspect directly) survives growth.
        final Aspect aer = Aspect.getAspect("aer");
        pod.setAspect(aer);
        pod.checkGrowth();
        helper.assertBlockProperty(pos, BlockManaPod.AGE, 1);
        pod.checkGrowth();
        pod.checkGrowth();
        helper.assertBlockProperty(pos, BlockManaPod.AGE, 3);
        helper.assertTrue(pod.getAspect() == aer,
                "the pod's seeded aspect must survive growth (original checkGrowth keeps it)");

        // NBT: the original's "aspect" string tag.
        final CompoundTag nbt = pod.saveWithoutMetadata();
        helper.assertTrue("aer".equals(nbt.getString("aspect")),
                "the pod's aspect must save under the original key, got: " + nbt.getString("aspect"));

        // Original BlockManaPod.getDrops: nothing below age 2; one bean at
        // ages 2-6; at age 7 a second bean with 67% chance; every bean
        // carries the pod's aspect (default herba when the pod has none).
        final BlockState age7 = base.setValue(BlockManaPod.AGE, 7);
        int sawSecondBean = 0;
        for (int i = 0; i < 30; i++) {
            final List<ItemStack> drops = Block.getDrops(age7, level, pos, pod);
            helper.assertTrue(!drops.isEmpty() && drops.size() <= 2,
                    "age-7 pod must drop 1-2 beans, got " + drops.size());
            if (drops.size() == 2) {
                sawSecondBean++;
            }
            for (final ItemStack bean : drops) {
                helper.assertTrue(bean.is(CatalogResources.ITEM_MANA_BEAN.get()),
                        "pod must drop mana beans, got " + bean);
                final EssentiaContainer essentia = bean.get(ModDataComponents.ESSENTIA_CONTAINER.get());
                helper.assertTrue(essentia != null && essentia.amounts().getOrDefault("aer", 0) == 1,
                        "dropped bean must carry the pod's aspect, got " + essentia);
            }
        }
        helper.assertTrue(sawSecondBean > 0,
                "the age-7 second-bean branch (67% chance) never fired across 30 rolls");

        for (final int age : List.of(0, 1)) {
            final List<ItemStack> none =
                    Block.getDrops(base.setValue(BlockManaPod.AGE, age), level, pos, pod);
            helper.assertTrue(none.isEmpty(), "pods below age 2 must not drop beans (age " + age + ")");
        }

        // A pod without an aspect drops herba-seeded beans (original default).
        // Clear the block first so the seeded tile entity is gone.
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(1, 1, 1, base);
        final TileManaPod bare = helper.getBlockEntity(pos);
        helper.assertTrue(bare.getAspect() == null, "freshly placed pod must have no aspect");
        for (final ItemStack bean : Block.getDrops(age7, level, pos, bare)) {
            final EssentiaContainer essentia = bean.get(ModDataComponents.ESSENTIA_CONTAINER.get());
            helper.assertTrue(essentia != null && essentia.amounts().getOrDefault("herba", 0) == 1,
                    "aspect-less pod beans must default to herba, got " + essentia);
        }

        helper.succeed();
    }
}
