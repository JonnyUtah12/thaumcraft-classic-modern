package com.jonnyutah12.thaumcraftclassic.nodes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Node visibility gating (TC-M5-02): the original only renders a node when the
 * viewer is "revealing" it — reference
 * {@code thaumcraft/client/renderers/tile/TileNodeRenderer.java} L187-197:
 * a helmet-slot {@code IRevealer} whose {@code showNodes} agrees (goggles, void
 * and fortress helmets), or a held {@code ItemThaumometer}.
 *
 * <p>The port has no node renderer yet (no node block), so this is the reveal
 * predicate plus the consumer hook: whoever renders or highlights nodes
 * (M6 visuals, the wand HUD) asks {@link #canSeeNodes(Entity)}. Items opt in by
 * registering here — the same role the original's {@code IRevealer} interface
 * played, kept as a registry so item classes need no marker interface.</p>
 */
public final class NodeReveal {

    /** Original {@code IRevealer.showNodes(ItemStack, EntityLivingBase)}. */
    public interface Revealer {
        boolean showNodes(ItemStack stack, LivingEntity wearer);
    }

    private static final Map<Item, Revealer> REVEALERS = new ConcurrentHashMap<>();
    private static final Set<Item> THAUMOMETERS = ConcurrentHashMap.newKeySet();

    private NodeReveal() {
    }

    /** Registers a helmet item as a revealer (original {@code IRevealer} armor). */
    public static void registerRevealer(final Item item, final Revealer revealer) {
        REVEALERS.put(item, revealer);
    }

    /** Registers a held item that reveals nodes (original {@code ItemThaumometer} branch). */
    public static void registerThaumometer(final Item item) {
        THAUMOMETERS.add(item);
    }

    public static boolean isRevealer(final ItemStack stack) {
        return !stack.isEmpty() && REVEALERS.containsKey(stack.getItem());
    }

    public static boolean isThaumometer(final ItemStack stack) {
        return !stack.isEmpty() && THAUMOMETERS.contains(stack.getItem());
    }

    /**
     * The renderer's rule, item-level: helmet revealer whose {@code showNodes}
     * agrees, else a held thaumometer. Mirrors the original's else-if order —
     * a revealing helmet wins without the line-of-sight branch.
     */
    public static boolean canSeeNodes(final ItemStack held, final ItemStack helmet, final LivingEntity viewer) {
        if (!helmet.isEmpty()) {
            final Revealer revealer = REVEALERS.get(helmet.getItem());
            if (revealer != null && revealer.showNodes(helmet, viewer)) {
                return true;
            }
        }
        return isThaumometer(held);
    }

    /** Consumer hook: main-hand item + head slot of a living viewer. */
    public static boolean canSeeNodes(final Entity viewer) {
        if (!(viewer instanceof LivingEntity living)) {
            return false;
        }
        final ItemStack held = living.getItemInHand(InteractionHand.MAIN_HAND);
        final ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
        return canSeeNodes(held, helmet, living);
    }

    /**
     * Player convenience matching the original's viewer type (the renderer only
     * checked {@code EntityPlayer} viewers).
     */
    public static boolean canSeeNodes(final Player player) {
        return canSeeNodes((Entity) player);
    }

    /** Drop all registrations (server stop / test isolation). */
    public static void clear() {
        REVEALERS.clear();
        THAUMOMETERS.clear();
    }
}
