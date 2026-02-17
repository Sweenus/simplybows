package net.sweenus.simplybows.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public final class BubbleColumnFieldManager {

    private static final int COLUMN_DURATION_TICKS = 60;
    private static final double COLUMN_RADIUS = 1.2;
    private static final double COLUMN_HEIGHT = 2.6;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 12;
    private static final Map<ServerWorld, ActiveBubbleColumn> ACTIVE_COLUMNS = new HashMap<>();

    private BubbleColumnFieldManager() {
    }

    public static boolean createOrReplaceColumn(ServerWorld world, Vec3d center) {
        if (!isUnderwater(world, center)) {
            return false;
        }

        double y = findGroundTopY(world, center.x, center.z, center.y) + 0.05;
        Vec3d anchoredCenter = new Vec3d(center.x, y, center.z);
        if (!isUnderwater(world, anchoredCenter)) {
            return false;
        }

        long expiryTick = world.getTime() + COLUMN_DURATION_TICKS;
        ACTIVE_COLUMNS.put(world, new ActiveBubbleColumn(anchoredCenter, expiryTick));
        spawnBurstParticles(world, anchoredCenter);
        playSpawnSound(world, anchoredCenter);
        return true;
    }

    public static void tick(ServerWorld world) {
        ActiveBubbleColumn column = ACTIVE_COLUMNS.get(world);
        if (column == null) {
            return;
        }

        if (world.getTime() >= column.expiryTick()) {
            ACTIVE_COLUMNS.remove(world);
            return;
        }

        spawnAmbientParticles(world, column.center());

        if (world.getTime() % 5L == 0L) {
            refillPlayerAir(world, column.center());
        }
    }

    private static void refillPlayerAir(ServerWorld world, Vec3d center) {
        Box box = Box.of(center.add(0.0, COLUMN_HEIGHT * 0.5, 0.0), COLUMN_RADIUS * 2.0, COLUMN_HEIGHT, COLUMN_RADIUS * 2.0);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, box, PlayerEntity::isAlive)) {
            if (player.squaredDistanceTo(center.x, player.getY(), center.z) > COLUMN_RADIUS * COLUMN_RADIUS) {
                continue;
            }
            if (player.getAir() < player.getMaxAir()) {
                player.setAir(player.getMaxAir());
            }
        }
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 3; i++) {
            double y = center.y + 0.2 + world.getRandom().nextDouble() * (COLUMN_HEIGHT - 0.25);
            world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, y, center.z, 1, 0.18, 0.05, 0.18, 0.0);
        }
        world.spawnParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.15, center.z, 2, 0.22, 0.04, 0.22, 0.0);
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, center.y + 0.3, center.z, 16, 0.35, 0.25, 0.35, 0.0);
        world.spawnParticles(ParticleTypes.SPLASH, center.x, center.y + 0.15, center.z, 10, 0.3, 0.1, 0.3, 0.0);
    }

    private static void playSpawnSound(ServerWorld world, Vec3d center) {
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT,
                SoundCategory.BLOCKS,
                0.7F,
                0.95F + world.getRandom().nextFloat() * 0.15F
        );
    }

    private static boolean isUnderwater(ServerWorld world, Vec3d center) {
        BlockPos base = BlockPos.ofFloored(center);
        if (!world.getFluidState(base).isOf(Fluids.WATER)) {
            return false;
        }
        BlockPos up = base.up();
        return world.getFluidState(up).isOf(Fluids.WATER);
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }

        return centerY;
    }

    private record ActiveBubbleColumn(Vec3d center, long expiryTick) {
    }
}
