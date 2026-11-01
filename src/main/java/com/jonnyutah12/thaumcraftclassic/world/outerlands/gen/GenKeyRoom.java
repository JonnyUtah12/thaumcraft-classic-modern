package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.phys.Vec3;

import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.placeBlock;
import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.setBlockAndUpdate;

/**
 * The TC maze key room — {@code MazeGenerator.FEATURE_KEY} (6), the room that
 * hands out the {@code blockEldritch} cap pedestal with the Runed Tablet on it
 * (content-mapping.csv row {@code thaumcraft:worldgen.outer_lands_procedural_maze}).
 *
 * <p>Original: {@code thaumcraft.common.lib.world.dim.GenKeyRoom.generateRoom}
 * (reference {@code GenKeyRoom.java:17-117}), the {@code GenMaze} case 6
 * (reference {@code MazeGenerator.java:242}). Every loop bound, placement code
 * and gate below is verbatim from that method: the shell of
 * {@code PAT_WALL}, the {@code PAT_VOID} lining, the {@code PAT_STAIRS_ELDRITCH}
 * runs, the {@code PAT_STAIRS_PURPLE} ring, the four connection gates, the cap
 * pedestal, the tablet drop and the difficulty-scaled Eldrian Guardian escort.</p>
 *
 * <p>Two legs run through the shipped seams / vanilla substitutes because
 * their entities are TC-M6-06: the tablet is dropped as a vanilla
 * {@link ItemEntity} that reproduces the original's
 * {@code EntityPermanentItem} behaviours (the random yaw and the never-despawn
 * rule — see the drop site), and the guardians are summoned through
 * {@link EldritchMobHost} (the original's
 * {@code ConfigEvents.mob_hosts.get("Guardian")}, reference L109). While the
 * host is unregistered the summons are no-ops and the room still builds.</p>
 */
public final class GenKeyRoom {

    private GenKeyRoom() {
    }

    /**
     * Build the key room in one 16x16 maze cell.
     *
     * @param level the maze level writer (original {@code World}, ref L17)
     * @param random the maze random — the escort offset rolls and the champion
     *               roll draw from it (ref L107-115), as the caller's
     *               {@code MazeHandler} random does
     * @param cx chunk X of the cell (ref L18: {@code cx * 16})
     * @param cz chunk Z of the cell (ref L19)
     * @param y room floor Y (the caller's 50, {@code OuterLands.ROOM_Y})
     * @param cell the maze cell; {@code north/south/east/west} pick the
     *             connection gates, {@code feature} tags the placed blocks
     */
    public static void generateRoom(final LevelWriter level, final Random random, final int cx, final int cz,
                                    final int y, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f-m2] GenKeyRoom needs a ServerLevel (block-entity reads, the "
                    + "summon seam and the tablet drop); the maze is a server-side path, key room at chunk ("
                    + cx + ", " + cz + ") is left unbuilt");
            return;
        }

        final int x = cx * 16; // ref L18
        final int z = cz * 16; // ref L19

        // Ref L20-36: the outer shell, the void lining, the stair runs, the floor.
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 1; c <= 10; c++) {
                    if (a == 1 || b == 1 || a == 15 || b == 15) {
                        if (c < 10) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 1, cell); // ref L24
                        }
                    } else if ((a == 2 || b == 2 || a == 14 || b == 14) && c < 9) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 8, cell); // ref L25
                    } else if ((a == 3 || b == 3 || a == 13 || b == 13) && c < 9) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 3, cell); // ref L26: stairs
                    } else if (c < 9) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 2, cell); // ref L27: floor
                    }
                }
            }
        }

        // Ref L37-64: the inner box, the two stair runs, the purple ring.
        for (int a = 4; a <= 12; a++) {
            for (int b = 4; b <= 12; b++) {
                for (int c = 1; c <= 9; c++) {
                    if (a == 4 || b == 4 || a == 12 || b == 12) {
                        if (c < 9 && c > 1) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 8, cell); // ref L41
                        }
                    } else if (a == 5 || b == 5 || a == 11 || b == 11) {
                        if (c < 5 && c > 1) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 10, cell); // ref L44
                        } else if (c >= 5 && c < 9) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 11, cell); // ref L45
                        }
                    } else if (a == 6 || b == 6 || a == 10 || b == 10) {
                        if (c > 3) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 8, cell); // ref L50
                        }
                    } else if (a == 7 || b == 7 || a == 9 || b == 9) {
                        if (c == 5) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 10, cell); // ref L55
                        } else if (c > 5 && c < 8) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 8, cell); // ref L56
                        } else if (c == 8) {
                            placeBlock(world, new BlockPos(x + a, y + c, z + b), 4, cell); // ref L57
                        }
                    }
                }
            }
        }

        // Ref L65-70: the void core over the pedestal.
        for (int a = 7; a <= 9; a++) {
            for (int b = 7; b <= 9; b++) {
                for (int c = 5; c <= 7; c++) {
                    placeBlock(world, new BlockPos(x + a, y + c, z + b), 8, cell); // ref L69
                }
            }
        }

        // Ref L71-94: the connection gates. A cell open on exactly one axis
        // seals the two side walls of that door with PAT_WALL; a cell open on
        // BOTH axes on one plane seals nothing (ref L71).
        if (!(cell.east && cell.west) && !(cell.south && cell.north)) {
            if (cell.east) {
                for (int c = 0; c <= 8; c++) {
                    for (int b = 7; b <= 9; b++) {
                        placeBlock(world, new BlockPos(x + 14, y + c, z + b), 1, cell); // ref L75-76
                    }
                }
            }
            if (cell.west) {
                for (int c = 0; c <= 8; c++) {
                    for (int b = 7; b <= 9; b++) {
                        placeBlock(world, new BlockPos(x + 2, y + c, z + b), 1, cell); // ref L80-81
                    }
                }
            }
            if (cell.south) {
                for (int c = 0; c <= 8; c++) {
                    for (int b = 7; b <= 9; b++) {
                        placeBlock(world, new BlockPos(b + x, y + c, z + 14), 1, cell); // ref L85-86
                    }
                }
            }
            if (cell.north) {
                for (int c = 0; c <= 8; c++) {
                    for (int b = 7; b <= 9; b++) {
                        placeBlock(world, new BlockPos(b + x, y + c, z + 2), 1, cell); // ref L90-91
                    }
                }
            }
        }

        // Ref L95: the pedestal — blockEldritch meta 3 (the cap) at the room
        // centre, y+2. Direct set, no decoration queue.
        setBlockAndUpdate(new BlockPos(x + 8, y + 2, z + 8), CatalogEldritchRealm.BLOCK_ELDRITCH.get(), world,
                EldritchRealmValues.ELDRITCH_CAP);

        // Ref L96-100: the Runed Tablet (itemEldritchObject meta 2) on the
        // pedestal, motion zeroed and never despawning. The original's
        // EntityPermanentItem is TC-M6-06 (it extends EntitySpecialItem, which
        // has no shipped entity row), so the port drops a vanilla ItemEntity
        // and reproduces the two behaviours that class adds: the ctor's random
        // yaw (EntityPermanentItem.java:16) and the never-despawn rule
        // (EntityPermanentItem.java:23-25 — `if (age + 5 >= lifespan) age = 0`,
        // whose 1.21.1 form is ItemEntity.setUnlimitedLifetime(), `age :=
        // -32768`). The ctor's motion (x/z ±0.1, y 0.2 — EntityPermanentItem
        // .java:17-19) is zeroed exactly as ref L99 zeroes it.
        final ItemStack tablet = new ItemStack(CatalogEldritchRealm.ITEM_ELDRITCH_OBJECT.get());
        ItemEldritchObject.setStoredMeta(tablet, EldritchRealmValues.OBJECT_TABLET); // ref L98: meta 2
        final ItemEntity drop = new ItemEntity(world, x + 8.5, y + 3.5, z + 8.5, tablet); // ref L97
        drop.setDeltaMovement(Vec3.ZERO); // ref L99: motionX/motionY/motionZ := 0
        drop.setYRot(random.nextFloat() * 360.0F); // Entity.setRot is protected; the yaw field is the original's
        drop.setUnlimitedLifetime(); // ref L100: field_145822_b := true
        world.addFreshEntity(drop); // ref L100

        // Ref L101-116: the escort. The original counts `zz = 2 + (hard ? 2 :
        // normal ? 1 : 0)` off the world difficulty (peaceful/easy 2, normal 3,
        // hard 4) and summons that many Eldrian Guardians over the pedestal,
        // each at a spot the maze random draws in whole blocks (ref L107-108)
        // and leashed to the pedestal at range 16 (ref L112
        // `func_110171_b(x+8, y+2, z+8, 16)`). The champion flag (ref L113-115:
        // the first guardian on hard, EntityUtils.makeChampion) and the leash
        // are the entity's own behaviour and live with the mob slice
        // (TC-M6-06); the seam takes a position only, so the port draws the
        // same spots in the same order and leaves those two legs to it.
        final Difficulty difficulty = world.getDifficulty();
        int escort = 2; // ref L101 (zz)
        if (difficulty == Difficulty.HARD) {
            escort = 4;
        } else if (difficulty == Difficulty.NORMAL) {
            escort = 3;
        }
        for (int qq = 0; qq < escort; qq++) { // ref L102-116
            final BlockPos spot = escortSpot(random, x, y, z);
            EldritchMobHost.spawn(EldritchMobHost.GUARDIAN, world, spot);
            // ref L113-115: `if (qq == 0 && zz >= 4) EntityUtils.makeChampion(eg,
            // true)` — the champion flag is TC-M6-06's mob behaviour and the
            // EldritchMobHost seam carries no flag, so it is recorded, not faked.
        }
    }

    /**
     * The escort spawn point: ref L107-108 — {@code x + 8.5 +
     * MathHelper.getRandomInRange(v, 1, 3) * MathHelper.getRandomInRange(v, -1,
     * 1)} on X, the same roll on Z, {@code y + 2}. {@code getRandomInRange} is
     * {@code min + nextInt(max - min + 1)} in 1.7.10, so both factors are
     * integers (1..3 and -1..1): the offsets are whole blocks in -3..3 and the
     * y is exactly {@code y + 2}. The port truncates to the block position the
     * seam takes; each guardian draws four ints from the maze random, as the
     * original's four {@code getRandomInRange} calls do.
     */
    private static BlockPos escortSpot(final Random random, final int x, final int y, final int z) {
        final int spreadX = (1 + random.nextInt(3)) * (-1 + random.nextInt(3));
        final int spreadZ = (1 + random.nextInt(3)) * (-1 + random.nextInt(3));
        return new BlockPos(net.minecraft.util.Mth.floor(x + 8.5 + spreadX),
                net.minecraft.util.Mth.floor(y + 2.0), net.minecraft.util.Mth.floor(z + 8.5 + spreadZ));
    }

    /**
     * The {@code LevelWriter} resolved to a {@code ServerLevel}, else null —
     * the maze call site ({@code MazeHandler.generateEldritch}) always
     * resolves (see {@code GenPortal.serverLevel}).
     */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel server ? server : null;
    }
}
