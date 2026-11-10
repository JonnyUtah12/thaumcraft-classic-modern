package com.jonnyutah12.thaumcraftclassic.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * The world lookups the focus bodies did themselves (TC-M6-03-s), ported from
 * {@code common/lib/utils/BlockUtils.getTargetBlock} and
 * {@code common/lib/utils/EntityUtils.getPointedEntity}:
 *
 * <ul>
 *   <li>{@code EntityUtils.getPointedEntity(world, player, 0.0, 20.0, 1.1F)} —
 *       the shock focus's bolt target
 *       ({@code ItemFocusShock.java:147}), the hellbat's summon target
 *       ({@code ItemFocusHellbat.java:87})</li>
 *   <li>{@code ItemFocusTrade.getMovingObjectPositionFromPlayer} and
 *       {@code BlockUtils.getTargetBlock} — the block the architecture foci act
 *       on ({@code ItemFocusTrade.java:70-93},
 *       {@code ItemFocusPortableHole.java:85}, {@code ItemFocusWarding.java:89})</li>
 *   <li>{@code ItemFocusPortableHole}'s forward scan (lines 102-131)</li>
 * </ul>
 *
 * <p>The 1.7.10 method names are not ported as such: the ray is rebuilt here
 * from {@link ClipContext}, {@link Level#clip} and the shipped
 * {@code world/} drivers' conventions, and the entity sweep is the original's
 * own algorithm — a box sweep along the view vector, each candidate inflated by
 * {@code max(0.8F, entity.getPickRadius())}, the closest visible one winning.
 * Every 1.7.10 entity's default pick radius is {@code 0.0F}, so the inflate the
 * original performed is the constant {@code 0.8F} here; that is the only
 * difference, and it is invisible for the entities these foci aim at.</p>
 */
public final class CastTargets {

    /** The original's default inflate for a candidate's box ({@code ItemFocusShock.java:147} path). */
    public static final float ENTITY_PADDING = 0.8F;

    /** How far the block-reaching bodies look ({@code ItemFocusTrade.java:86}: {@code double d3 = 5.0;}). */
    public static final double BLOCK_RANGE = 5.0D;

    /**
     * The living entity the player's view vector hits within {@code range},
     * or null — {@code EntityUtils.getPointedEntity(world, player, 0.0, range,
     * padding)} with the original's own sweep.
     */
    public static Entity pointedEntity(final Level level, final Player player,
                                       final double range, final float padding) {
        final Vec3 eye = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        final Vec3 look = player.getViewVector(1.0F);
        final Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
        final AABB sweep = player.getBoundingBox()
                .expandTowards(look.x * range, look.y * range, look.z * range)
                .inflate(padding, padding, padding);

        Entity best = null;
        double bestDistance = 0.0D;
        final List<Entity> candidates = level.getEntities(player, sweep, target -> target.isAlive());
        for (final Entity candidate : candidates) {
            final AABB box = candidate.getBoundingBox().inflate(ENTITY_PADDING);
            final Vec3 hit = box.clip(eye, end).orElse(null);
            final boolean contains = box.contains(eye);
            if (!contains && hit == null) {
                continue;
            }
            // The original also required an unobstructed line to the candidate.
            final Vec3 candidateEye = new Vec3(candidate.getX(),
                    candidate.getY() + candidate.getEyeHeight(), candidate.getZ());
            final HitResult obstruction = level.clip(new ClipContext(eye, candidateEye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (obstruction.getType() != HitResult.Type.MISS) {
                continue;
            }
            final double distance = contains ? 0.0D : eye.distanceTo(hit);
            if (best == null || distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * The block the player's view vector hits within the original's 5.0 block
     * reach, or null — {@code BlockUtils.getTargetBlock}/{@code
     * ItemFocusTrade.getMovingObjectPositionFromPlayer}. The 1.7.10
     * {@code MovingObjectPosition} became the port's {@link BlockHitResult};
     * its face is {@link BlockHitResult#getDirection()} and its hit position
     * {@code getLocation()}, which is what the original's
     * {@code mop.field_72310_e}/{@code mop.field_72307_f} were.
     */
    public static BlockHitResult pointedBlock(final Level level, final Player player) {
        final Vec3 eye = player.getEyePosition(1.0F);
        final Vec3 end = eye.add(player.getViewVector(1.0F).scale(BLOCK_RANGE));
        final BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    /**
     * The original's block-reach distance: {@code 5.0} for anything but a
     * {@code EntityPlayerMP}, which reported {@code getBlockReachDistance()}
     * ({@code ItemFocusTrade.java:86-89}). 1.21.1 exposes the same number as the
     * {@code minecraft:block_interaction_range} attribute.
     */
    public static double blockReach(final Player player) {
        return player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    /**
     * {@code ItemFocusPortableHole.onFocusRightClick}'s forward scan
     * (lines 102-131): walk from {@code origin} along {@code side} while the
     * block there is something the hole can pass through, and report how many
     * blocks were walked ({@code distance}). The original's
     * {@code ThaumcraftApi.portableHoleBlackList}/{@code Blocks.bedrock}/
     * {@code ConfigBlocks.blockHole}/air/{-1 hardness} guards become
     * "air, no collision, or an unbreakable block", because the blacklist and
     * {@code blockHole} are the hole machinery's own content (not ported).
     */
    public static int holeDistance(final Level level, final BlockPos origin, final Direction side,
                                   final int maxDistance) {
        BlockPos pos = origin;
        int distance = 0;
        for (; distance < maxDistance; distance++) {
            if (!passable(level, pos)) {
                break;
            }
            pos = pos.relative(side);
        }
        return distance;
    }

    /** Whether the portable hole's scan may pass through the block at {@code pos}. */
    private static boolean passable(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) == -1.0F) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /** Whether a block is solid enough for the warding focus to ward it ({@code ItemFocusWarding.java:97}). */
    public static boolean wardeable(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.blocksMotion() && level.getBlockEntity(pos) == null;
    }

    private CastTargets() {
    }
}
