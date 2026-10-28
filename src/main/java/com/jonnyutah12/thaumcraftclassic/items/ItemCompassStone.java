package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sinister Lodestone (content-mapping.csv {@code thaumcraft:itemcompassstone}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemCompassStone}. A dark vis
 * node announces itself to nearby lodestones: the original's
 * {@code TileNode.onDataUpdate} client branch wrote its coordinates into the
 * static {@code ItemCompassStone.sinisterNodes} map (timestamped, every 50th
 * tick) while the node was DARK. While the lodestone sat in the player's
 * inventory, its per-tick update pruned entries older than 10 s and, if any
 * node in the current dimension lay inside the player's view cone
 * ({@code EntityUtils.isVisibleTo}: 0.66 rad aperture, 256 m range — a cone
 * test, not a raycast), swapped the item to its "active" icon. The original's
 * private {@code directionToPoint} had no callers in 4.2.3.5 (dead code); the
 * port omits it. The active/inactive icon pair is a model concern (TC-M7-01);
 * the port exposes the state through {@link #isActive()}.</p>
 *
 * <p>TC-M5-02 hook: the node side of the seam is
 * {@link #noteSinisterNode(BlockPos, ResourceKey)} — TC-M5-02's TileNode
 * calls it from its client tick exactly where the original wrote to the map.
 * Until that lands the map stays empty and the lodestone stays idle, matching
 * the original in a world without dark nodes.</p>
 */
public class ItemCompassStone extends Item {

    /** One announced dark-node position, keyed by block pos + dimension. */
    public record SinisterLocation(BlockPos pos, ResourceKey<Level> dimension) {
    }

    /** Original {@code ItemCompassStone.sinisterNodes}: position → last-seen millis. */
    private static final Map<SinisterLocation, Long> SINISTER_NODES = new ConcurrentHashMap<>();

    /** Original staleness bound: entries older than 10 s are pruned. */
    private static final long STALE_MS = 10_000L;

    /** Original EntityUtils.isVisibleTo parameters. */
    private static final float VIEW_APERTURE = 0.66F;
    private static final double VIEW_RANGE = 256.0F;

    /**
     * Original stored the chosen icon on the item instance (shared across all
     * stacks); the port keeps the same instance-level state so the renderer
     * and tests can read it.
     */
    private boolean active;

    public ItemCompassStone(final Properties properties) {
        super(properties);
    }

    /** TC-M5-02 seam: the node-side half of the original map write. */
    public static void noteSinisterNode(final BlockPos pos, final ResourceKey<Level> dimension) {
        SINISTER_NODES.put(new SinisterLocation(pos.immutable(), dimension), System.currentTimeMillis());
    }

    /** Whether a dark node is currently in view (drives the active icon). */
    public boolean isActive() {
        return active;
    }

    /** Test seam: the live announcement map. */
    public static Map<SinisterLocation, Long> sinisterNodes() {
        return SINISTER_NODES;
    }

    /**
     * Original {@code func_77663_a} body, extracted from {@link #inventoryTick}
     * so the original client-only call site and a headless game-test server can
     * both reach the identical code. Prune announcements older than the original
     * 10 s bound, then activate if any same-dimension node lies in the
     * original's view cone ({@code EntityUtils.isVisibleTo}: 0.66 rad aperture,
     * 256 m range — a cone test, not a raycast).
     */
    public void updateAnnouncements(final Level level, final Player player) {
        final long now = System.currentTimeMillis();
        active = false;
        SINISTER_NODES.entrySet().removeIf(entry -> entry.getValue() < now - STALE_MS);
        for (final Map.Entry<SinisterLocation, Long> entry : SINISTER_NODES.entrySet()) {
            if (!entry.getKey().dimension().equals(level.dimension())) {
                continue;
            }
            final Vec3 target = Vec3.atBottomCenterOf(entry.getKey().pos());
            if (lyingInCone(target, player.getEyePosition(),
                    player.getEyePosition().add(player.getLookAngle().scale(VIEW_RANGE)), VIEW_APERTURE)) {
                active = true;
                return;
            }
        }
    }

    /**
     * Original onUpdate (func_77663_a): the whole body is gated on
     * {@code world.field_72995_K} (client side) and on the ticking entity being
     * the player, exactly as in 1.7.10.
     */
    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity,
                              final int slotIndex, final boolean isSelected) {
        if (!level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        updateAnnouncements(level, player);
    }

    /**
     * Original {@code Utils.isLyingInCone(x, t, b, aperture)}: the target
     * point x lies inside the cone with apex t (the eye), axis t→b
     * (eye + look·range), half-aperture aperture/2, and closer than the axis
     * length. Ported with the same dot-product/cosine formulation.
     */
    private static boolean lyingInCone(final Vec3 x, final Vec3 t, final Vec3 b, final double aperture) {
        final double halfAperture = aperture / 2.0;
        final Vec3 apexToX = t.subtract(x);
        final Vec3 axis = t.subtract(b);
        final double axisLen = axis.length();
        final double apexLen = apexToX.length();
        if (apexLen == 0.0 || axisLen == 0.0) {
            return false;
        }
        final boolean inInfiniteCone = apexToX.dot(axis) / apexLen / axisLen > Math.cos(halfAperture);
        return inInfiniteCone && apexToX.dot(axis) / axisLen < axisLen;
    }
}
