package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.JarStorageLogic;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The essentia phial (content-mapping.csv {@code thaumcraft:itemessence}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemEssence} — one item, two
 * original metas: 0 = empty phial, 1 = filled. The port keeps that pair on the
 * {@code thaumcraft:item_meta} component like the rest of the item catalog
 * (D-001), and carries the contents in the
 * {@code thaumcraft:essentia_container} data component
 * ({@link IEssentiaContainerItem}) instead of the original's raw
 * {@code Aspects} NBT subcompound.</p>
 *
 * <p>Behaviour ported verbatim from {@code onItemUseFirst}:</p>
 * <ul>
 *   <li><b>fill</b> — an empty phial on a warded jar holding at least
 *       {@value JarStorageLogic#PHIAL_TRANSFER} units drains exactly 8 of the
 *       jar's aspect into a new filled phial, consumes the empty phial and
 *       plays the swim sound;</li>
 *   <li><b>drain</b> — a filled phial with exactly one aspect on a warded jar
 *       whose fill leaves room for 8 and whose label accepts the aspect adds
 *       all 8 (only when nothing is left over), consumes the filled phial and
 *       returns an empty one;</li>
 *   <li>a jar with a label is skipped by the drain branch's accept check, and
 *       the original's void jar (its own metadata) is not a valid phial
 *       target — both preserved here.</li>
 * </ul>
 *
 * <p>Not ported: the original's alembic fill branch (the alembic tile is
 * blockMetalDevice metadata 1 and has no tile in this port yet — its card's
 * owner adds that branch, which is a copy of the jar fill against the same
 * container API), the knowledge-gated tooltip ("tc.aspect.unknown" for
 * undiscovered aspects — no player context in 1.21.1
 * {@code appendHoverText}, same deferral as {@link ItemResource}), the
 * two-pass essence overlay tint and the per-aspect creative-tab listing
 * (TC-M7-01).</p>
 */
public class ItemPhial extends Item implements IEssentiaContainerItem {

    /** Original damage value of an empty phial. */
    public static final int META_EMPTY = 0;
    /** Original damage value of a filled phial. */
    public static final int META_FILLED = 1;
    /**
     * The original's two damage values (0 = Glass Phial, 1 = Phial of
     * Essentia) as the contiguous meta domain, so the localization harness
     * demands both per-meta name keys and the shipped labels are the ones the
     * game asks for.
     */
    public static final int META_COUNT = 2;

    public ItemPhial(final Properties props) {
        super(props.stacksTo(64));
    }

    /** The original's {@code new ItemStack(this, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        final ItemStack stack = new ItemStack(CatalogEssentiaStorage.ITEM_ESSENCE.get());
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    /** A phial holding {@code amount} of {@code aspect} (original meta 1). */
    public static ItemStack filled(final Aspect aspect, final int amount) {
        final ItemStack stack = stack(META_FILLED);
        stack.set(ModDataComponents.ESSENTIA_CONTAINER.get(), EssentiaContainer.of(aspect, amount));
        return stack;
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), META_EMPTY);
    }

    public static boolean isFilled(final ItemStack stack) {
        return storedMeta(stack) == META_FILLED;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (final Aspect aspect : aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(aspect.getName() + " x" + aspects.getAmount(aspect)));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Original {@code onItemUseFirst}: fill from a jar / drain into a jar.
     *
     * <p>Returns PASS (not CONSUME) on the client so the client's own
     * interaction path stays in charge, matching the original's
     * {@code world.isRemote -> return false}.</p>
     */
    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getItemInHand();
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TileJarFillable jar)) {
            return InteractionResult.PASS;
        }
        // The original keys off blockJar metadata: the warded jar (md 0) and
        // the void jar (md 3) are both phial targets, and the port's variants
        // carry those same numbers, so any jar tile here is a valid target.
        if (storedMeta(stack) == META_EMPTY) {
            return fillFromJar(level, pos, player, stack, jar);
        }
        return drainIntoJar(level, pos, player, stack, jar);
    }

    /** Empty phial + jar with 8+ units: drain 8 of the jar's aspect into a new phial. */
    private InteractionResult fillFromJar(final Level level, final BlockPos pos, final Player player,
                                          final ItemStack stack, final TileJarFillable jar) {
        if (jar.amount < JarStorageLogic.PHIAL_TRANSFER || jar.aspect == null) {
            return InteractionResult.PASS;
        }
        final Aspect aspect = Aspect.getAspect(jar.aspect.getTag());
        if (!jar.takeFromContainer(aspect, JarStorageLogic.PHIAL_TRANSFER)) {
            return InteractionResult.PASS;
        }
        stack.shrink(1);
        give(level, player, filled(aspect, JarStorageLogic.PHIAL_TRANSFER), pos);
        swim(level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Filled phial + accepting jar with room for 8: put all 8 in, return an empty phial. */
    private InteractionResult drainIntoJar(final Level level, final BlockPos pos, final Player player,
                                           final ItemStack stack, final TileJarFillable jar) {
        final AspectList aspects = getAspects(stack);
        if (aspects == null || aspects.size() != 1) {
            return InteractionResult.PASS;
        }
        final Aspect aspect = aspects.getAspects().get(0);
        if (jar.amount > jar.maxAmount() - JarStorageLogic.PHIAL_TRANSFER
                || !jar.doesContainerAccept(aspect)
                || jar.addToContainer(aspect, JarStorageLogic.PHIAL_TRANSFER) != 0) {
            return InteractionResult.PASS;
        }
        stack.shrink(1);
        give(level, player, stack(META_EMPTY), pos);
        swim(level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(final Level level, final Player player, final ItemStack drop, final BlockPos pos) {
        if (!player.getInventory().add(drop)) {
            final ItemEntity entity = new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
            level.addFreshEntity(entity);
        }
    }

    /** Original {@code world.playSoundAtEntity(player, "game.neutral.swim", 0.25F, 1.0F)}. */
    private static void swim(final Level level, final BlockPos pos) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.0F);
    }
}
