package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * BD-17 (t_f04c44fb) — the item-level colour table: which layer of which
 * stack gets tinted with what, transcribed from the original's
 * {@code Item.getColorFromItemStack(ItemStack, int pass)}.
 *
 * <p>1.7.10 called that method for the inventory/hand render of an item; the
 * 1.21.1 equivalent is a client item colour handler keyed by item, called per
 * render layer (measured: NeoForge 21.1.259 ships
 * {@code RegisterColorHandlersEvent.Item} and {@code ItemColorHandler}, and
 * the vanilla item render asks it once per {@code ItemLayerModel} layer).
 * {@link com.jonnyutah12.thaumcraftclassic.client.ItemMetaClient} wires this
 * table to that event; keeping the maths here lets the gates assert the
 * numbers without a client.</p>
 *
 * <h2>Only item-level colour is here</h2>
 * The original also tinted with <em>block-level</em> colour —
 * {@code BlockCandle.colorMultiplier(md) = Utils.colors[md]},
 * {@code BlockCrystal.colorMultiplier(md) = BlockCustomOreItem.colors[md + 1]},
 * the ore variants' {@code getRenderType(md)} — and those are the block
 * render's question, not the item render's, so they travel with the
 * blockstates/block-models card (t_4f36e7d7). Recording the split is the
 * point: the list below is the complete set of
 * {@code Item.getColorFromItemStack} implementations among the shipped items
 * (measured by grepping {@code func_82790_a} across the original's
 * {@code common/items/}).
 *
 * <h2>Rows</h2>
 * <ul>
 *   <li>{@code itemshard} — {@code ItemShard.getColorFromItemStack}:
 *       {@code md == 6 ? super.getColorFromItemStack(...) : BlockCustomOreItem.colors[md + 1]}.
 *       The original's array, read from the decimal literals at reference
 *       {@code common/blocks/BlockCustomOreItem.java:8}
 *       ({@code {16777215, 16777086, 16727041, 37119, 40960, 15650047, 5592439}}
 *       = {@code {0xFFFFFF, 0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF,
 *       0x555577}}), so metas 0-5 take indices 1-6 and meta 6 (the balanced
 *       shard) is untinted.</li>
 *   <li>{@code itembaubleblanks} — {@code ItemBaubleBlanks.getColorFromItemStack}
 *       (reference {@code common/items/baubles/ItemBaubleBlanks.java:88-92}):
 *       {@code md >= 3 && md <= 8 ? Aspect.getPrimalAspects().get(md - 3).getColor() : super}.
 *       Every pass, so both layers; the blanks at metas 0-2 fall through
 *       untinted. Read through {@code ItemBaubleBlanks.apprenticeAspect(md)} so
 *       the primal ordering comes from the aspect registry, not a copied list.</li>
 *   <li>{@code itemessence} — {@code ItemEssence.getColorFromItemStack} (the
 *       port's {@link ItemPhial}): {@code pass != 0 && md != 0
 *       ? (md == 1 && getAspects(stack) != null ? getAspects(stack).getAspects()[0].getColor() : white)
 *       : white}. The overlay layer carries the aspect; the phial glass stays white.</li>
 *   <li>{@code itemresource} — {@code ItemResource.getColorFromItemStack}:
 *       {@code pass == 1 && md == 13 && getAspects(stack) != null ? aspects[0].getColor() : white}
 *       — the jar label's aspect band, on the {@code label_over} overlay layer.</li>
 *   <li>{@code itemcrystalessence}, {@code itemwispessence},
 *       {@code itemmanabean} — one body in all three classes:
 *       {@code getAspects(stack) != null ? aspects[0].getColor()
 *       : displayAspects[(int)(System.currentTimeMillis() / 500L % displayAspects.length)].getColor()},
 *       with {@code displayAspects = Aspect.aspects.values().toArray(...)}
 *       ({@code ItemWispEssence.java:21}) — an unassigned stack cycles through
 *       every aspect's colour twice a second. 1.21.1 asks the colour handler
 *       per frame, so the cycle is expressible as-is.</li>
 * </ul>
 */
public final class ItemMetaTints {

    /**
     * The original's {@code BlockCustomOreItem.colors} array, verbatim. The
     * reference writes it in decimal ({@code common/blocks/BlockCustomOreItem.java:8}:
     * {@code {16777215, 16777086, 16727041, 37119, 40960, 15650047, 5592439}});
     * the hex here is the same seven numbers.
     */
    static final int[] ORE_COLORS = {
            0xFFFFFF, 0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};

    /** The original's untinted colour ({@code 16777215}). */
    static final int WHITE = 0xFFFFFF;

    /** The original's cycling period: {@code System.currentTimeMillis() / 500L}. */
    static final long CYCLE_MS = 500L;

    /** The balanced shard's meta — the original's {@code md == 6} test. */
    static final int META_SHARD_BALANCED = 6;

    /** The ids this table colours (the gates' expected set). */
    private static final Set<String> TINTED = Set.of(
            "itemshard", "itemessence", "itemresource", "itembaubleblanks",
            "itemcrystalessence", "itemwispessence", "itemmanabean");

    /**
     * The subset whose original coloured ONE pass and not the other —
     * {@code ItemEssence} (pass 0 = the phial glass stays white) and
     * {@code ItemResource} (pass 1 = the {@code label_over} band). Those two
     * need a model with a separate overlay layer; an id that tints every pass
     * ({@code ItemBaubleBlanks}, the shards, the wisps) is fine on one layer.
     * Probe 8 reads this split.
     */
    private static final Set<String> OVERLAY_LAYER_TINTS = Set.of("itemessence", "itemresource");

    public static boolean tintsOneLayerOnly(final String idPath) {
        return OVERLAY_LAYER_TINTS.contains(idPath);
    }

    public static Set<String> overlayLayerTintIds() {
        return OVERLAY_LAYER_TINTS;
    }

    private ItemMetaTints() {
    }

    /** Does the original colour this item at all? */
    public static boolean hasTint(final String idPath) {
        return TINTED.contains(idPath);
    }

    public static Set<String> tintedIds() {
        return TINTED;
    }

    /** The tint of layer {@code layer} of {@code stack}, by registry id path. */
    public static int color(final String idPath, final ItemStack stack, final int layer) {
        return color(idPath, stack, layer, System.currentTimeMillis());
    }

    /** The tint of a live stack, id resolved from the item registry. */
    public static int color(final Item item, final ItemStack stack, final int layer) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return color(id.getPath(), stack, layer, System.currentTimeMillis());
    }

    /** The pure form (explicit clock), so the unit test can drive the cycle. */
    public static int color(final String idPath, final ItemStack stack,
                            final int layer, final long nowMillis) {
        return color(idPath, storedMeta(stack), firstAspect(stack), layer, nowMillis);
    }

    /**
     * The tint maths, pure: meta and first aspect in, colour out. The
     * server-free unit test drives this directly (no registry, no stack); the
     * client handler reaches it through the stack form.
     */
    public static int color(final String idPath, final int storedMeta, final Aspect firstAspect,
                            final int layer, final long nowMillis) {
        switch (idPath) {
            case "itemshard":
                // ItemShard.getColorFromItemStack: md == 6 ? super : colors[md + 1]
                return storedMeta == META_SHARD_BALANCED ? WHITE : ORE_COLORS[storedMeta + 1];
            case "itemessence": {
                // ItemEssence.getColorFromItemStack: pass 0 (the phial glass) is never tinted
                if (layer == 0 || storedMeta != ItemPhial.META_FILLED) {
                    return WHITE;
                }
                return firstAspect == null ? WHITE : firstAspect.getColor();
            }
            case "itemresource": {
                // ItemResource.getColorFromItemStack: pass 1 of md 13 (the label_over overlay)
                if (layer != 1 || storedMeta != ItemResource.META_JAR_LABEL) {
                    return WHITE;
                }
                return firstAspect == null ? WHITE : firstAspect.getColor();
            }
            case "itembaubleblanks": {
                // ItemBaubleBlanks.getColorFromItemStack (reference :88-92): the
                // apprentice rings (md 3-8) wear their primal's colour on every
                // pass; metas 0-2 (the amulet/ring/belt blanks) fall through
                // untinted. apprenticeAspect answers null outside 3-8, so the
                // primal order comes from the aspect registry, not a copied list.
                final Aspect primal = com.jonnyutah12.thaumcraftclassic.items.baubles.ItemBaubleBlanks
                        .apprenticeAspect(storedMeta);
                return primal == null ? WHITE : primal.getColor();
            }
            case "itemcrystalessence":
            case "itemwispessence":
            case "itemmanabean": {
                if (firstAspect != null) {
                    return firstAspect.getColor();
                }
                // displayAspects: every aspect the original knows, cycling every 500 ms
                final List<Aspect> all = Aspect.values();
                if (all.isEmpty()) {
                    return WHITE;
                }
                return all.get((int) (nowMillis / CYCLE_MS % all.size())).getColor();
            }
            default:
                return WHITE;
        }
    }

    /** The stack's {@code thaumcraft:item_meta}, the same read the labels use. */
    static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * The first aspect of a container stack — the original's
     * {@code getAspects(stack).getAspects()[0]} on
     * {@code IEssentiaContainerItem}, read through the same component so no
     * item instance (and no registry) is needed.
     */
    static Aspect firstAspect(final ItemStack stack) {
        final EssentiaContainer container = stack.get(ModDataComponents.ESSENTIA_CONTAINER.get());
        if (container == null || container.isEmpty()) {
            return null;
        }
        final AspectList aspects = container.toAspectList();
        if (aspects == null || aspects.getAspects() == null || aspects.getAspects().isEmpty()) {
            return null;
        }
        return aspects.getAspects().get(0);
    }
}
