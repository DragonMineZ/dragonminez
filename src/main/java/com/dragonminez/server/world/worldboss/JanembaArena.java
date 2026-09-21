package com.dragonminez.server.world.worldboss;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class JanembaArena {

    public static final int FLOOR_RADIUS = 16;
    public static final int RIM_RADIUS = 22;
    public static final int HEADROOM = 22;
    public static final int FOUNDATION_DEPTH = 4;

    private static final int RIM_HEIGHT = 7;
    private static final int RING_SPIKES = 18;
    private static final int INNER_SPIKES = 9;

    private JanembaArena() {}

    public static boolean isAreaLoaded(ServerLevel level, BlockPos center) {
        for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx += 16) {
            for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz += 16) {
                if (!level.isLoaded(center.offset(dx, 0, dz))) return false;
            }
        }
        return level.isLoaded(center);
    }

    public static void build(ServerLevel level, BlockPos center) {
        RandomSource random = RandomSource.create(center.asLong());

        BlockState floor = MainBlocks.HELL_STONE.get().defaultBlockState();
        BlockState ground = MainBlocks.HELL_GROUND.get().defaultBlockState();
        BlockState deep = MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseY = center.getY();

        for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx++) {
            for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RIM_RADIUS) continue;

                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                if (dist <= FLOOR_RADIUS) {
                    BlockState surface = random.nextInt(4) == 0 ? ground : floor;
                    cursor.set(x, baseY, z);
                    level.setBlock(cursor, surface, 2);

                    for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
                        cursor.set(x, baseY - depth, z);
                        if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, deep, 2);
                    }

                    for (int up = 1; up <= HEADROOM; up++) {
                        cursor.set(x, baseY + up, z);
                        if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                } else {
                    double ramp = (dist - FLOOR_RADIUS) / (double) (RIM_RADIUS - FLOOR_RADIUS);
                    int wallTop = baseY + Mth.ceil(ramp * RIM_HEIGHT);

                    for (int y = baseY - FOUNDATION_DEPTH; y <= wallTop; y++) {
                        cursor.set(x, y, z);
                        BlockState existing = level.getBlockState(cursor);
                        if (existing.isAir() || y >= baseY) {
                            level.setBlock(cursor, y >= wallTop - 1 ? floor : deep, 2);
                        }
                    }

                    for (int up = wallTop + 1; up <= baseY + HEADROOM; up++) {
                        cursor.set(x, up, z);
                        if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        for (int i = 0; i < RING_SPIKES; i++) {
            double angle = (Math.PI * 2.0D / RING_SPIKES) * i + random.nextDouble() * 0.15D;
            int radius = RIM_RADIUS - 1 - random.nextInt(2);
            int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
            int top = surfaceAt(level, x, z, baseY + RIM_HEIGHT + 2, baseY - FOUNDATION_DEPTH);
            if (top != Integer.MIN_VALUE) spike(level, x, top + 1, z, 5 + random.nextInt(6), random);
        }

        for (int i = 0; i < INNER_SPIKES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = 4 + random.nextInt(FLOOR_RADIUS - 5);
            int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
            if (Math.abs(x - center.getX()) < 4 && Math.abs(z - center.getZ()) < 4) continue;
            spike(level, x, baseY + 1, z, 3 + random.nextInt(4), random);
        }
    }

    private static int surfaceAt(ServerLevel level, int x, int z, int from, int to) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = from; y >= to; y--) {
            cursor.set(x, y, z);
            if (!level.getBlockState(cursor).isAir()) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static void spike(ServerLevel level, int x, int baseY, int z, int height, RandomSource random) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

        for (int i = 0; i < height; i++) {
            int width = i < height / 3 ? 1 : 0;
            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    if (width == 1 && Math.abs(dx) + Math.abs(dz) == 2) continue;
                    cursor.set(x + dx, baseY + i, z + dz);
                    if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, bone, 2);
                }
            }
            if (i > height / 2 && random.nextInt(4) == 0) break;
        }
    }
}
