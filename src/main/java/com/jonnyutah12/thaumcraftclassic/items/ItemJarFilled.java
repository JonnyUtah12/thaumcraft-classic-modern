package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * A jar lifted out of the world with its essentia still in it
 * (content-mapping.csv {@code thaumcraft:itemjarfilleditem}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.ItemJarFilled} — stack of one,
 * metadata 0 (warded) / 3 (void), contents in the {@code Aspects} NBT
 * subcompound and the label in an {@code AspectFilter} string. A warded jar
 * that is empty and unlabelled drops its own block item instead of this
 * ({@code BlockJar.getDrops}), so this item only ever exists with essentia
 * and/or a label on it.</p>
 *
 * <p>Ported: the 1-item stack limit, the drop with the jar's aspect, amount
 * and label attached (see {@code BlockJar.getDrops}), placement that puts the
 * essentia and the label straight into the jar tile, and the tooltip
 * (contents, then the magenta label line). The contents ride on the
 * {@code thaumcraft:essentia_container} component and the label on
 * {@code thaumcraft:jar_filter}; the original damage value rides on
 * {@code thaumcraft:item_meta} (D-001), keeping 0 and 3 as the original's
 * metas so packs and saves that mention them keep working.</p>
 *
 * <p>Not ported: the knowledge-gated tooltip (undiscovered aspects show as
 * {@code tc.aspect.unknown}) and the blank-sprite renderer that draws the
 * liquid inside the item — both with the client knowledge mirror and
 * TC-M7-01. The original's special-case "click the side of a water source"
 * placement branch is 1.7.10 water-logging folklore and rides with the fluid
 * work (TC-M4-07).</p>
 */
public class ItemJarFilled extends Item implements IEssentiaContainerItem {

    /** Original metadata of a filled warded jar. */
    public static final int META_WARDED = 0;
    /** Original metadata of a filled void jar. */
    public static final int META_VOID = 3;
    /**
     * The original's sparse metadata domain (0 = Jar of Essentia, 3 = Void Jar
     * of Essentia), declared so the localization harness demands both name
     * keys the item's {@code getName} can ask for.
     */
    public static final Set<Integer> VALID_METAS = Set.of(META_WARDED, META_VOID);

    public ItemJarFilled(final Properties props) {
        super(props.stacksTo(1));
    }

    /** The original's {@code new ItemStack(ConfigItems.itemJarFilled, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        final ItemStack stack = new ItemStack(CatalogEssentiaStorage.ITEM_JAR_FILLED.get());
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), META_WARDED);
    }

    public static boolean isVoid(final ItemStack stack) {
        return storedMeta(stack) == META_VOID;
    }

    /** The label carried on the item (original {@code getFilter}). */
    @Nullable
    public static Aspect filterOf(final ItemStack stack) {
        final String tag = stack.get(ModDataComponents.JAR_FILTER.get());
        return tag == null ? null : Aspect.getAspect(tag);
    }

    /** Attach a label to the item (the drop path and the label recipe). */
    public static void setFilter(final ItemStack stack, @Nullable final Aspect filter) {
        if (filter == null) {
            stack.remove(ModDataComponents.JAR_FILTER.get());
        } else {
            stack.set(ModDataComponents.JAR_FILTER.get(), filter.getTag());
        }
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath()
                + (isVoid(stack) ? ".void" : ""));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        // Original addInformation: the aspect lines, then the label in magenta.
        final AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() > 0) {
            for (final Aspect aspect : aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(aspect.getName() + " x " + aspects.getAmount(aspect)));
            }
        }
        final Aspect filter = filterOf(stack);
        if (filter != null) {
            tooltip.add(Component.literal("§5" + filter.getName()));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Original {@code onItemUse}: place a jar (warded or void, per the item's
     * metadata) and pour the carried essentia and label into the tile.
     */
    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final ItemStack stack = context.getItemInHand();
        final Player player = context.getPlayer();
        final BlockPos clicked = context.getClickedPos();
        final Direction face = context.getClickedFace();
        final BlockState clickedState = level.getBlockState(clicked);

        // Original: a replaceable target (or water/lava) takes the jar in
        // place; anything else places on the clicked face.
        final BlockPos target = clickedState.canBeReplaced() ? clicked : clicked.relative(face);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isEmptyBlock(target) && !level.getBlockState(target).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.setBlock(target, CatalogEssentiaStorage.jarState(isVoid(stack)), 11)) {
            return InteractionResult.PASS;
        }
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        applyContents(level, target, stack);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(false);
    }

    /** Original {@code placeBlockAt} tail: aspect + amount + label into the tile. */
    private void applyContents(final Level level, final BlockPos target, final ItemStack stack) {
        final BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof TileJarFillable jar)) {
            return;
        }
        final AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() == 1) {
            final Aspect aspect = aspects.getAspects().get(0);
            jar.aspect = aspect;
            jar.amount = aspects.getAmount(aspect);
        }
        final Aspect filter = filterOf(stack);
        if (filter != null) {
            jar.aspectFilter = filter;
        }
        jar.touch();
    }

    /** A drop for a jar tile: the original's {@code BlockJar.getDrops} shape. */
    public static ItemStack dropFor(@Nullable final TileJarFillable jar) {
        if (jar == null) {
            return ItemStack.EMPTY;
        }
        final boolean voidJar = jar.voidJar();
        final ItemStack drop = stack(voidJar ? META_VOID : META_WARDED);
        if (jar.amount > 0 && jar.aspect != null) {
            drop.set(ModDataComponents.ESSENTIA_CONTAINER.get(),
                    EssentiaContainer.of(jar.aspect, jar.amount));
        }
        if (jar.aspectFilter != null) {
            setFilter(drop, jar.aspectFilter);
        }
        return drop;
    }
}
