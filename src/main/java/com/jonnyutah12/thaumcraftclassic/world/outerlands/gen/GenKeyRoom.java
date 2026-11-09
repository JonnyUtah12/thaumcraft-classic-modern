package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * (reference {@code GenKeyRoom.java:17-117}), the builder switch's case 6
 * (reference {@code MazeHandler.java:152-153} — the switch that dispatches
 * features to builders is {@code MazeHandler.generateEldritch} at
 * {@code MazeHandler.java:142-169}, NOT in {@code MazeGenerator}, whose
 * 6..9 comment is the dead-end feature naming). Every loop bound, placement code
 * and gate below is verbatim from that method: the code-1 shell (13 deep),
 * the code-8 void lining on ring 2 with the four door-gap clauses (ref L35-38),
 * the ring-3 lining of {@code STONE_NOSPAWN} (code 19) in the core window — with
 * the single (a==8 && b==8, cxx==6) gap the original gates at ref L50 — and
 * {@code ROCK} (code 18) elsewhere, the floor/ceiling trio and the two stone
 * pyramids off {@code q = min(|8-a|,|8-b|)}, the four directional stair runs,
 * {@code GenCommon.generateConnections(..., 3, true)}, the cap pedestal, the
 * tablet drop and the difficulty-scaled Eldrian Guardian escort.</p>
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
     * @param random the maze random — the connection leg and the escort offset
     *               rolls draw from it (ref L94, L105-108), as the caller's
     *               {@code MazeHandler} random does
     * @param cx chunk X of the cell (ref L18: {@code cx * 16})
     * @param cz chunk Z of the cell (ref L19)
     * @param y room floor Y (the caller's 50, {@code OuterLands.ROOM_Y})
     * @param cell the maze cell; {@code north/south/east/west} gate the lining's
     *             door gaps (ref L35-38) and the connections, {@code feature}
     *             tags the placed blocks
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

        // Ref L21-29: the shell — code 1 (BEDROCK) on the boundary ring, the
        // full 13-deep column (c 0..12). The original's placeBlock case 1 places
        // only where the world is air (ref GenCommon.java:63-65), so a wall that
        // is already there survives; the port's case mirrors that.
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 1, cell); // ref L24-25
                    }
                }
            }
        }

        // Ref L31-43: the void lining — code 8 on ring 2 (a/b 2..14, cxx 1..11)
        // with the door gaps. The four gate clauses are verbatim (ref L35-38):
        // an open side leaves the b/a 4..11 span of that wall open below cxx 10,
        // so a door is a 8-wide, 9-high gap in the lining, not a carved hole in
        // the outer shell.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 2 || a == 14 || b == 2 || b == 14)
                            && (a != 2 || b <= 3 || b >= 12 || !cell.west || cxx >= 10)
                            && (a != 14 || b <= 3 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 2 || a <= 3 || a >= 12 || !cell.north || cxx >= 10)
                            && (b != 14 || a <= 3 || a >= 12 || !cell.south || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), 8, cell); // ref L39
                    }
                }
            }
        }

        // Ref L45-59: the inner lining on ring 3 (a/b 3..13, cxxx 2..10). The
        // core window — the a/b 8 cross for cxxx 4..8, the a/b 7..9 box for
        // cxxx 5..7 — is STONE_NOSPAWN (code 19) except the single gap the
        // original gates at ref L50 ((a==8 && b==8) && cxxx==6 places nothing);
        // every other ring cell is ROCK (code 18).
        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        if (cxxx > 3 && cxxx < 9 && (a == 8 || b == 8)
                                || cxxx > 4 && cxxx < 8 && (a == 7 || b == 7 || a == 9 || b == 9)) {
                            if (a != 8 && b != 8 || cxxx != 6) {
                                placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), 19, cell); // ref L51
                            }
                        } else {
                            placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), 18, cell); // ref L54
                        }
                    }
                }
            }
        }

        // Ref L61-85: the floor (code 1 at y-1, code 8 at y, code 2 at y+1) and
        // the ceiling (code 1 at y+13, code 8 at y+12, code 2 at y+11) across the
        // whole 2..14 span, then the two stone pyramids off the same
        // q = min(|8-a|, |8-b|): the floor rises q-1 steps (ref L69-75) and the
        // ceiling drops q steps (ref L77-83), so the (8,8) column stays open and
        // the cap under the tablet is not buried.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), 1, cell);  // ref L63
                placeBlock(world, new BlockPos(x + a, y, z + b), 8, cell);      // ref L64
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), 2, cell);  // ref L65
                placeBlock(world, new BlockPos(x + a, y + 13, z + b), 1, cell); // ref L66
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), 8, cell); // ref L67
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), 2, cell); // ref L68
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    final int q = Math.min(Math.abs(8 - a), Math.abs(8 - b)); // ref L70
                    for (int g = 0; g < q - 1; g++) {
                        placeBlock(world, new BlockPos(x + a, y + 1 + g, z + b), 2, cell); // ref L73
                    }
                }
                if (a > 3 && a < 13 && b > 3 && b < 13) {
                    final int q = Math.min(Math.abs(8 - a), Math.abs(8 - b)); // ref L78
                    for (int g = 0; g < q; g++) {
                        placeBlock(world, new BlockPos(x + a, y + 11 - g, z + b), 2, cell); // ref L81
                    }
                }
            }
        }

        // Ref L87-92: the four directional stair runs at y+2 (five blocks each,
        // the same shape the library room repeats at ref L69-77).
        for (int g = 0; g < 5; g++) {
            placeBlock(world, new BlockPos(x + 6 + g, y + 2, z + 4), 10, Direction.NORTH, cell);  // ref L88
            placeBlock(world, new BlockPos(x + 6 + g, y + 2, z + 12), 10, Direction.SOUTH, cell); // ref L89
            placeBlock(world, new BlockPos(x + 12, y + 2, z + 6 + g), 10, Direction.EAST, cell);  // ref L90
            placeBlock(world, new BlockPos(x + 4, y + 2, z + 6 + g), 10, Direction.WEST, cell);   // ref L91
        }

        // Ref L94: the connections the room itself opens — the shipped
        // generateConnections (depth 3, seal = true), the same call the nest
        // (ref GenNestRoom.java:120) and library (ref GenLibraryRoom.java:102)
        // rooms make. The key room has NO hand-rolled gate walls: the lining's
        // gate clauses (ref L35-38) are the door shape and this leg carves the
        // corridor stubs.
        GenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);

        // Ref L95: the pedestal — blockEldritch meta 3 (the cap) at the room
        // centre, y+2, on top of the code-2 floor at y+1. Direct set, no
        // decoration queue.

        // Ref L96-100: the Runed Tablet (itemEldritchObject meta 2) on the
        // pedestal, motion zeroed and never despawning. The original's
        // EntityPermanentItem is TC-M6-06 (it extends EntitySpecialItem, which
        // has no shipped entity row), so the port drops a vanilla ItemEntity
        // and reproduces the two behaviours that class adds: the ctor's random
        // yaw (EntityPermanentItem.java:17) and the never-despawn rule
        // (EntityPermanentItem.java:24-28 — `if (age + 5 >= lifespan) age = 0`,
        // whose 1.21.1 form is ItemEntity.setUnlimitedLifetime(), `age :=
        // -32768`). The ctor's motion (x/z ±0.1, y 0.2 — EntityPermanentItem
        // .java:18-20) is zeroed exactly as ref L97-99 zeroes all three.
        final ItemStack tablet = new ItemStack(CatalogEldritchRealm.ITEM_ELDRITCH_OBJECT.get());
        ItemEldritchObject.setStoredMeta(tablet, EldritchRealmValues.OBJECT_TABLET); // ref L96: ItemStack(..., 1, 2)
        final ItemEntity drop = new ItemEntity(world, x + 8.5, y + 3.5, z + 8.5, tablet); // ref L96
        drop.setDeltaMovement(Vec3.ZERO); // ref L97-99: motionX/motionY/motionZ := 0
        drop.setYRot(random.nextFloat() * 360.0F); // Entity.setRot is protected; the yaw field is the original's
        drop.setUnlimitedLifetime(); // EntityPermanentItem.java:24-28 is the rule this encodes
        if (!world.addFreshEntity(drop)) { // ref L100: world.spawnEntity
            // 1.21.1 measures the drop into nothing when no chunk section is
            // loaded at the position (ServerLevel.addEntity -> PersistentEntity
            // SectionManager.addEntityToStorage -> hasChunkLoaded), and the
            // tablet is the maze's one payload, so a refusal is an anomaly
            // worth a line. Not debug: the harness runs this logger at INFO and
            // the leg is invisible there (measured).
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f-m2] GenKeyRoom: the level refused the Runed Tablet at "
                    + "({}, {}, {}) — the maze cell's chunk is not loaded, the tablet is lost", x + 8.5, y + 3.5,
                    z + 8.5);
        }

        // Ref L101-116: the escort. The original counts `zz = 2 + (hard ? 2 :
        // normal ? 1 : 0)` off the world difficulty (peaceful/easy 2, normal 3,
        // hard 4) and summons that many Eldrian Guardians over the pedestal,
        // each at a spot the maze random draws in whole blocks (ref L105-108)
        // and leashed to the pedestal at range 16 (ref L110
        // `func_110171_b(x+8, y+2, z+8, 16)`). The champion flag (ref L111-113:
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
        for (int qq = 0; qq < escort; qq++) { // ref L103-116
            final BlockPos spot = escortSpot(random, x, y, z);
            EldritchMobHost.spawn(EldritchMobHost.GUARDIAN, world, spot);
            // ref L111-113: `if (qq == 0 && zz >= 4) EntityUtils.makeChampion(eg,
            // true)` — the champion flag is TC-M6-06's mob behaviour and the
            // EldritchMobHost seam carries no flag, so it is recorded, not faked.
        }
    }

    /**
     * The escort spawn point: ref L105 + L107-108 — {@code x + 8.5 +
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
