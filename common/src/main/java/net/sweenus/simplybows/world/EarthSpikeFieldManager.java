package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.EarthSpikeVisualEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EarthSpikeFieldManager {

    private static final int RAISE_TICKS = 9;
    private static final int HOLD_TICKS = 0;
    private static final int SINK_TICKS = 11;
    private static final int FIELD_DURATION_TICKS = RAISE_TICKS + HOLD_TICKS + SINK_TICKS;
    private static final double FIELD_RADIUS = 3.6;
    private static final double PATCH_VISUAL_RADIUS = 2.1;
    private static final int PATCH_VISUAL_POINTS = 20;
    private static final float SPIKE_DAMAGE = 2.0F;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 16;
    private static final double SPIKE_SEGMENT_HEIGHT = 0.34;
    private static final double START_DEPTH = 3.2;
    private static final double BASE_GROUND_OFFSET = -0.18;
    private static final Map<ServerWorld, List<ActiveSpikeField>> ACTIVE_FIELDS = new HashMap<>();

    private EarthSpikeFieldManager() {
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (ownerId != null) {
            fields.removeIf(field -> {
                if (ownerId.equals(field.ownerId())) {
                    removeField(world, field);
                    return true;
                }
                return false;
            });
        }

        long now = world.getTime();
        List<SpikePoint> points = buildPatchPoints(world, center);
        ActiveSpikeField field = new ActiveSpikeField(center, now, now + FIELD_DURATION_TICKS, ownerId);
        spawnSpikeVisuals(world, field, points);
        fields.add(field);

        applySpikeDamage(world, center);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.35F, 0.65F + world.random.nextFloat() * 0.1F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundCategory.PLAYERS, 1.0F, 0.75F + world.random.nextFloat() * 0.1F);
        spawnBurstParticles(world, center);
    }

    public static void tick(ServerWorld world) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return;
        }

        fields.removeIf(field -> {
            if (world.getTime() > field.expiryTick()) {
                removeField(world, field);
                return true;
            }
            return false;
        });
        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
            return;
        }

        for (ActiveSpikeField field : fields) {
            animateField(world, field);
        }
    }

    private static void applySpikeDamage(ServerWorld world, Vec3d center) {
        Box box = Box.of(center, FIELD_RADIUS * 2.0, 3.5, FIELD_RADIUS * 2.0);
        for (HostileEntity hostile : world.getEntitiesByClass(HostileEntity.class, box, LivingEntity::isAlive)) {
            if (hostile.squaredDistanceTo(center) > FIELD_RADIUS * FIELD_RADIUS) {
                continue;
            }
            hostile.damage(world.getDamageSources().magic(), SPIKE_DAMAGE);
        }
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), center.x, center.y + 0.12, center.z, 18, 1.1, 0.22, 1.1, 0.01);
        world.spawnParticles(ParticleTypes.POOF, center.x, center.y + 0.15, center.z, 8, 0.9, 0.12, 0.9, 0.01);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.05, center.z, 14, 1.0, 0.05, 1.0, 0.01);
    }

    private static List<SpikePoint> buildPatchPoints(ServerWorld world, Vec3d center) {
        List<SpikePoint> points = new ArrayList<>();
        for (int i = 0; i < PATCH_VISUAL_POINTS; i++) {
            double angle = ((Math.PI * 2.0) / PATCH_VISUAL_POINTS) * i;
            double ringScale = (0.45 + ((i * 13) % 10) * 0.06);
            double radius = PATCH_VISUAL_RADIUS * Math.min(1.0, ringScale);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + BASE_GROUND_OFFSET;
            int heightSegments = 1 + ((i * 11) % 8);
            points.add(new SpikePoint(x, y, z, heightSegments));
        }
        return points;
    }

    private static void spawnSpikeVisuals(ServerWorld world, ActiveSpikeField field, List<SpikePoint> points) {
        for (SpikePoint point : points) {
            float targetHeight = (float) (point.heightSegments() * SPIKE_SEGMENT_HEIGHT);
            EarthSpikeVisualEntity visual = new EarthSpikeVisualEntity(world, point.x(), point.y() - START_DEPTH, point.z(), targetHeight);
            visual.addCommandTag("simplybows_earth_spike_visual");
            world.spawnEntity(visual);
            field.visuals.add(new SpikeVisual(visual.getUuid(), point.x(), point.y(), point.z()));
        }
    }

    private static void animateField(ServerWorld world, ActiveSpikeField field) {
        long age = world.getTime() - field.spawnTick();
        float scale = getHeightScale(age);
        if (scale <= 0.01F) {
            return;
        }

        for (SpikeVisual visual : field.visuals) {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof EarthSpikeVisualEntity spikeVisual)) {
                continue;
            }
            spikeVisual.setHeightScale(scale);
            double yOffset = getVerticalOffset(age);
            spikeVisual.setPos(visual.baseX(), visual.baseY() + yOffset, visual.baseZ());
        }

        if (age % 2L == 0L) {
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), field.center().x, field.center().y + 0.08, field.center().z, 4, 1.4, 0.1, 1.4, 0.0);
        }
    }

    private static float getHeightScale(long age) {
        if (age < 0) {
            return 0.0F;
        }
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 1.0F;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return 1.0F - (t * t * t);
        }
        return 0.0F;
    }

    private static double getVerticalOffset(long age) {
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return -START_DEPTH + (START_DEPTH * easeOutBack(t));
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 0.0;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return -(START_DEPTH * t * t * t);
        }
        return -START_DEPTH;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
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

    private record SpikePoint(double x, double y, double z, int heightSegments) {
    }

    private static void removeField(ServerWorld world, ActiveSpikeField field) {
        for (SpikeVisual visual : field.visuals) {
            Entity entity = world.getEntity(visual.id());
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static final class ActiveSpikeField {
        private final Vec3d center;
        private final long spawnTick;
        private final long expiryTick;
        private final UUID ownerId;
        private final List<SpikeVisual> visuals = new ArrayList<>();

        private ActiveSpikeField(Vec3d center, long spawnTick, long expiryTick, UUID ownerId) {
            this.center = center;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.ownerId = ownerId;
        }

        private Vec3d center() {
            return this.center;
        }

        private long spawnTick() {
            return this.spawnTick;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private UUID ownerId() {
            return this.ownerId;
        }
    }

    private record SpikeVisual(UUID id, double baseX, double baseY, double baseZ) {
    }
}
