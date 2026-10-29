package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * The seam the Outer Lands structures use to summon their mobs (TC-M6-04f-s).
 *
 * <p>The original's eldritch structures placed and spawned Crimson Cult mobs
 * directly: the altar spawned {@code EntityEldritchGuardian} /
 * {@code EntityCultist} / {@code EntityBobo}, the lock spawned the guardian,
 * the crab spawner spawned {@code EntityEldritchCrab}, the trap hit
 * {@code EntityFireMoth} behaviour. None of those entity types exist in the
 * port yet (TC-M6-06 owns them, and the Crimson Cult card is open), and an
 * unregistered {@code EntityType} cannot be spawned in 1.21.1.</p>
 *
 * <p>So the structures run their full original state machines — eyes, fuses,
 * counts, sounds, the {@code IEldritchMob} census — and hand the summon itself
 * to this seam. A card that ships one of those mobs registers a spawner for
 * its id and the structures start spawning it with no further change; while
 * nothing is registered the summon is a no-op, exactly as the {@code
 * entities/TrunkHost} seam does for the traveling trunk. Everything the
 * structures do *around* the summon is shipped and observable.</p>
 */
public final class EldritchMobHost {

    /** Spawns one mob of the id at the structure cell; true on success. */
    public interface Spawner {
        boolean spawn(ServerLevel level, BlockPos pos);
    }

    /** Census predicate: counts the mobs of this family in a box (TC-M6-06). */
    private static final Map<String, Spawner> SPAWNERS = new HashMap<>();
    private static final Map<String, BooleanSupplier> AVAILABLE = new HashMap<>();

    /** Ids the original's eldritch structures summon, in registration order. */
    public static final String GUARDIAN = "eldritchguardian";
    public static final String CULTIST = "cultist";
    public static final String CLERIC = "cultistcleric";
    public static final String KNIGHT = "cultistknight";
    public static final String BOBO = "bobo";
    public static final String CRAB = "eldritchcrab";
    public static final String FIRE_MOTH = "firemoth";

    /** Registers a mob's spawner; {@code available} gates the structure census. */
    public static void register(final String id, final Spawner spawner, final BooleanSupplier available) {
        SPAWNERS.put(id.toLowerCase(Locale.ROOT), spawner);
        AVAILABLE.put(id.toLowerCase(Locale.ROOT), available);
    }

    /** True when a mob of this id can be spawned (a card has claimed it). */
    public static boolean isAvailable(final String id) {
        final BooleanSupplier available = AVAILABLE.get(id.toLowerCase(Locale.ROOT));
        return available != null && available.getAsBoolean();
    }

    /** The original's summon; a no-op while no card has claimed the mob. */
    public static boolean spawn(final String id, final ServerLevel level, final BlockPos pos) {
        final Spawner spawner = SPAWNERS.get(id.toLowerCase(Locale.ROOT));
        if (spawner == null) {
            return false;
        }
        return spawner.spawn(level, pos);
    }

    /**
     * The {@code world.getEntitiesWithinAABB(...IEldritchMob.class)} census of
     * the original's structure tiles: the number of mobs of one family in the
     * box. Always 0 while the family is unregistered, which is what lets the
     * structures run their stock checks today.
     */
    public static int countFamily(final String id, final ServerLevel level, final BlockPos center,
                                  final int radiusXZ, final int radiusY) {
        if (!isAvailable(id)) {
            return 0;
        }
        int count = 0;
        for (final Entity entity : level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(center.getX() - radiusXZ, center.getY() - radiusY, center.getZ() - radiusXZ,
                        center.getX() + radiusXZ + 1, center.getY() + radiusY + 1, center.getZ() + radiusXZ + 1))) {
            if (matches(id, entity)) {
                count++;
            }
        }
        return count;
    }

    private static boolean matches(final String id, final Entity entity) {
        final net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return ThaumcraftClassic.MOD_ID.equals(key.getNamespace()) && id.equals(key.getPath());
    }

    private EldritchMobHost() {
    }
}
