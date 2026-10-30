package com.jonnyutah12.thaumcraftclassic.items.golems;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Golem accessory / decoration (content-mapping.csv
 * {@code thaumcraft:itemgolemdecoration} "Accessory") — the original's
 * {@code thaumcraft.common.entities.golems.ItemGolemDecoration}.
 *
 * <p>The original's {@code icon[8]} array is the domain: metas 0..7 are the top
 * hat, spectacles, bowtie, fez, dart launcher, visor, iron plating and mace
 * arm (the order of {@code item.ItemGolemDecoration.<meta>.name}). Its
 * {@code getDecoChar(int)} maps the metadata to the single character the golem
 * stores in its decoration string — {@code H}, {@code G}, {@code B}, {@code F},
 * {@code R}, {@code V}, {@code P}, {@code M} — and those characters are
 * functional: {@code EntityGolemBase.applyMaterialTraits} reads "H" for +5 max
 * health and "M" for +2 damage, {@code getRange()} reads "G", the move speed
 * reads "B" and "P", the climb rule reads "R" (a dart launcher). The mapping is
 * pure, so it is pinned by a unit test here; the <em>effects</em> and the
 * attachment interaction ({@code addDecoration}) are TC-M6-08-s-b.</p>
 *
 * <p>The original names a stack {@code "Accessory: " + <meta name>}; the port
 * follows its per-meta name convention for multi-meta items
 * ({@code ItemResource}), so the stack's name is the accessory's own name and
 * the family label is carried as a tooltip line.</p>
 */
public class ItemGolemDecoration extends Item {

    /** The original's eight damage values — {@code icon[8]}, one per accessory. */
    public static final int META_COUNT = 8;

    public ItemGolemDecoration(final Properties properties) {
        super(properties);
    }

    /** Original {@code func_77667_c} / {@code func_77653_i}: the per-meta name key. */
    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * Original {@code ItemGolemDecoration.getDecoChar(int md)}, verbatim:
     * 0→H (top hat), 1→G (spectacles), 2→B (bowtie), 3→F (fez),
     * 4→R (dart launcher), 5→V (visor), 6→P (iron plating), 7→M (mace arm),
     * and {@code ""} for anything else.
     */
    public static String getDecoChar(final int md) {
        return switch (md) {
            case 0 -> "H";
            case 1 -> "G";
            case 2 -> "B";
            case 3 -> "F";
            case 4 -> "R";
            case 5 -> "V";
            case 6 -> "P";
            case 7 -> "M";
            default -> "";
        };
    }

    /** The family label ({@code item.ItemGolemDecoration.name} → "Accessory"), shown in the tooltip. */
    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.itemgolemdecoration"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
