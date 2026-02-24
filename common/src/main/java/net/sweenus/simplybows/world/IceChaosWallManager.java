package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;
import net.sweenus.simplybows.util.NetworkCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IceChaosWallManager {

    private static final Map<ServerWorld, List<ActiveWall>> ACTIVE_WALLS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> WALL_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();
    private static final String WALL_VISUAL_TAG = "simplybows_ice_chaos_wall_visual";
    private static final double WALL_THICKNESS = 0.9;
    private static final double SEGMENT_SPACING = 0.85;
    private static final int RISE_TICKS = 4;
    private static final int SINK_TICKS = 10;

    private IceChaosWallManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveWall> walls = ACTIVE_WALLS.get(world);
        return (walls != null && !walls.isEmpty()) || !getCooldowns(world).isEmpty() || (world.getTime() % 20L == 0L);
    }

    public static boolean isWallReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    public static void spawnAtImpact(ServerWorld world, Vec3d center, Vec3d travelDir, UUID ownerId, int stringLevel, int frameLevel) {
        if (world == null || center == null) {
            return;
        }

        Vec3d horizontal = new Vec3d(travelDir.x, 0.0, travelDir.z);
        if (horizontal.lengthSquared() <= 1.0E-6) {
            horizontal = new Vec3d(0.0, 0.0, 1.0);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3d right = new Vec3d(-horizontal.z, 0.0, horizontal.x).normalize();

        int durationTicks = Math.max(20,
                SimplyBowsConfig.INSTANCE.iceBow.chaosWallDurationTicks.get()
                        + Math.max(0, frameLevel) * SimplyBowsConfig.INSTANCE.iceBow.chaosWallDurationPerFrameTicks.get());
        int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.iceBow.chaosWallCooldownTicks.get());
        int widthBlocks = Math.max(1,
                SimplyBowsConfig.INSTANCE.iceBow.chaosWallWidth.get()
                        + Math.max(0, stringLevel) * SimplyBowsConfig.INSTANCE.iceBow.chaosWallWidthPerString.get());
        int heightBlocks = Math.max(1, SimplyBowsConfig.INSTANCE.iceBow.chaosWallHeight.get());
        double halfLength = widthBlocks * 0.5;

        ActiveWall wall = new ActiveWall(center, right, horizontal, ownerId, world.getTime(), world.getTime() + durationTicks, halfLength, heightBlocks + 0.05);
        ACTIVE_WALLS.computeIfAbsent(world, w -> new ArrayList<>()).add(wall);

        if (ownerId != null) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).put(ownerId, now + durationTicks + cooldownTicks);
        }

        spawnVisuals(world, wall);

        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.9F, 0.7F + world.random.nextFloat() * 0.08F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.65F, 1.35F + world.random.nextFloat() * 0.1F);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.PACKED_ICE.getDefaultState()), center.x, center.y + 1.0, center.z, 24, halfLength * 0.5, 0.8, halfLength * 0.3, 0.01);
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 18, halfLength * 0.5, 0.8, halfLength * 0.3, 0.0);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveWall> walls = ACTIVE_WALLS.get(world);
        if (walls == null || walls.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanWallVisuals(world);
            }
            return;
        }

        Iterator<ActiveWall> iterator = walls.iterator();
        while (iterator.hasNext()) {
            ActiveWall wall = iterator.next();
            if (world.getTime() >= wall.expiryTick) {
                discardWallVisuals(world, wall);
                iterator.remove();
                continue;
            }
            animateVisuals(world, wall);
            blockProjectiles(world, wall);
            blockEntities(world, wall);
        }

        if (walls.isEmpty()) {
            ACTIVE_WALLS.remove(world);
            purgeOrphanWallVisuals(world);
        }
    }

    private static void blockProjectiles(ServerWorld world, ActiveWall wall) {
        for (ProjectileEntity projectile : world.getEntitiesByClass(ProjectileEntity.class, wall.bounds, p -> p.isAlive() && !p.isRemoved())) {
            if (!isInsideWallPlane(projectile.getPos(), wall)) {
                continue;
            }
            Vec3d pos = projectile.getPos();
            world.spawnParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.0);
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.01);
            projectile.discard();
        }
    }

    private static void blockEntities(ServerWorld world, ActiveWall wall) {
        for (Entity entity : world.getEntitiesByClass(Entity.class, wall.bounds,
                e -> e.isAlive() && !e.isRemoved() && !(e instanceof ProjectileEntity) && !(e instanceof IceChaosWallVisualEntity))) {
            if (!isInsideWallPlane(entity, wall)) {
                continue;
            }

            double side = wall.normal.dotProduct(entity.getPos().subtract(wall.center));
            double pushDir = side >= 0.0 ? 1.0 : -1.0;
            Vec3d push = wall.normal.multiply(pushDir * 0.22);
            Vec3d vel = entity.getVelocity();
            Vec3d adjusted = new Vec3d(vel.x * 0.2 + push.x, vel.y, vel.z * 0.2 + push.z);
            entity.setVelocity(adjusted);
            entity.velocityDirty = true;
            if (entity instanceof ServerPlayerEntity player) {
                NetworkCompat.sendVelocityUpdate(player);
            }
        }
    }

    private static boolean isInsideWallPlane(Vec3d pos, ActiveWall wall) {
        Vec3d rel = pos.subtract(wall.center);
        double lateral = Math.abs(rel.dotProduct(wall.right));
        if (lateral > wall.halfLength) {
            return false;
        }
        if (pos.y < wall.center.y || pos.y > wall.center.y + wall.height) {
            return false;
        }
        double distToPlane = Math.abs(rel.dotProduct(wall.normal));
        return distToPlane <= WALL_THICKNESS;
    }

    private static boolean isInsideWallPlane(Entity entity, ActiveWall wall) {
        Vec3d pos = entity.getPos();
        Vec3d rel = pos.subtract(wall.center);

        Box box = entity.getBoundingBox();
        double halfX = (box.maxX - box.minX) * 0.5;
        double halfZ = (box.maxZ - box.minZ) * 0.5;
        double lateralPadding = Math.max(halfX, halfZ) + 0.1;

        double lateral = Math.abs(rel.dotProduct(wall.right));
        if (lateral > wall.halfLength + lateralPadding) {
            return false;
        }

        if (box.maxY < wall.center.y || box.minY > wall.center.y + wall.height) {
            return false;
        }

        double normalPadding = Math.max(halfX, halfZ) + 0.05;
        double distToPlane = Math.abs(rel.dotProduct(wall.normal));
        return distToPlane <= WALL_THICKNESS + normalPadding;
    }

    private static void spawnVisuals(ServerWorld world, ActiveWall wall) {
        int segments = Math.max(3, (int) Math.ceil((wall.halfLength * 2.0) / SEGMENT_SPACING));
        double start = -wall.halfLength;
        double step = (wall.halfLength * 2.0) / (segments - 1);

        for (int i = 0; i < segments; i++) {
            double offset = start + step * i;
            Vec3d pos = wall.center.add(wall.right.multiply(offset));
            IceChaosWallVisualEntity visual = new IceChaosWallVisualEntity(world, pos.x, wall.center.y, pos.z, (float) wall.height);
            float yaw = (float) (Math.atan2(wall.normal.x, wall.normal.z) * (180.0 / Math.PI));
            visual.setYaw(yaw);
            visual.prevYaw = yaw;
            visual.addCommandTag(WALL_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                wall.visuals.add(new VisualRef(visual.getUuid(), pos.x, wall.center.y, pos.z));
            }
        }
    }

    private static void animateVisuals(ServerWorld world, ActiveWall wall) {
        long age = world.getTime() - wall.spawnTick;
        long remaining = wall.expiryTick - world.getTime();
        float rise = MathHelper.clamp((float) age / RISE_TICKS, 0.0F, 1.0F);
        float sink = remaining <= SINK_TICKS ? MathHelper.clamp((float) remaining / SINK_TICKS, 0.0F, 1.0F) : 1.0F;
        float scale = Math.min(rise, sink);

        wall.visuals.removeIf(ref -> {
            Entity entity = world.getEntity(ref.id);
            if (!(entity instanceof IceChaosWallVisualEntity visual)) {
                return true;
            }
            visual.setPos(ref.x, ref.y, ref.z);
            visual.setHeightScale(scale);
            return false;
        });
    }

    private static void discardWallVisuals(ServerWorld world, ActiveWall wall) {
        for (VisualRef ref : wall.visuals) {
            Entity entity = world.getEntity(ref.id);
            if (entity != null) {
                entity.discard();
            }
        }
        wall.visuals.clear();
    }

    private static void purgeOrphanWallVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof IceChaosWallVisualEntity && entity.getCommandTags().contains(WALL_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(WALL_COOLDOWNS_BY_SERVER, world);
    }

    private static final class ActiveWall {
        private final Vec3d center;
        private final Vec3d right;
        private final Vec3d normal;
        private final UUID ownerId;
        private final long spawnTick;
        private final long expiryTick;
        private final double halfLength;
        private final double height;
        private final Box bounds;
        private final List<VisualRef> visuals = new ArrayList<>();

        private ActiveWall(Vec3d center, Vec3d right, Vec3d normal, UUID ownerId, long spawnTick, long expiryTick, double halfLength, double height) {
            this.center = center;
            this.right = right;
            this.normal = normal;
            this.ownerId = ownerId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.halfLength = halfLength;
            this.height = height;
            this.bounds = Box.of(center.add(0.0, height * 0.5, 0.0), halfLength * 2.0 + 1.0, height + 1.0, WALL_THICKNESS * 2.5);
        }
    }

    private record VisualRef(UUID id, double x, double y, double z) {
    }
}
