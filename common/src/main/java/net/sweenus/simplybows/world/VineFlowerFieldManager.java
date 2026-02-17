package net.sweenus.simplybows.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.mixin.EntityAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VineFlowerFieldManager {

    private static final int FIELD_DURATION_TICKS = 200;
    private static final double FIELD_RADIUS = 5.0;
    private static final double ATTRACTION_RADIUS = 14.0;
    private static final double PATCH_VISUAL_RADIUS = 2.35;
    private static final int PATCH_VISUAL_POINTS = 26;
    private static final int GROWTH_POINTS_PER_TICK = 6;
    private static final int SPRING_ANIM_TICKS = 8;
    private static final double SPRING_START_OFFSET_Y = -0.62;
    private static final float SPRING_MAX_TILT_DEGREES = 18.0F;
    private static final float FRIENDLY_HEAL = 2.0F;
    private static final float UNDEAD_DAMAGE = 3.0F;
    private static final int GROUND_SCAN_UP = 5;
    private static final int GROUND_SCAN_DOWN = 18;
    private static final String FIELD_VISUAL_TAG = "simplybows_vine_field_visual";
    private static final Map<ServerWorld, ActiveFlowerField> ACTIVE_FIELDS = new HashMap<>();

    private VineFlowerFieldManager() {
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center) {
        removeField(world, ACTIVE_FIELDS.remove(world));

        long expiryTick = world.getTime() + FIELD_DURATION_TICKS;
        List<FlowerPoint> pendingPoints = buildPatchPoints(world, center);
        ACTIVE_FIELDS.put(world, new ActiveFlowerField(center, expiryTick, pendingPoints));
        playFieldCreationSound(world, center);
        spawnBurstParticles(world, center);
    }

    public static void tick(ServerWorld world) {
        ActiveFlowerField field = ACTIVE_FIELDS.get(world);
        if (field == null) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanFieldVisuals(world);
            }
            return;
        }

        if (world.getTime() >= field.expiryTick()) {
            removeField(world, field);
            ACTIVE_FIELDS.remove(world);
            return;
        }

        growFieldVisuals(world, field);
        animateFieldVisuals(world, field);
        spawnAmbientParticles(world, field.center());

        if (world.getTime() % 10L == 0L) {
            attractPassiveMobs(world, field.center());
        }

        if (world.getTime() % 20L == 0L) {
            applyAuraEffects(world, field.center());
        }
    }

    private static void attractPassiveMobs(ServerWorld world, Vec3d center) {
        Box box = Box.of(center, ATTRACTION_RADIUS * 2.0, 6.0, ATTRACTION_RADIUS * 2.0);
        for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class, box, LivingEntity::isAlive)) {
            if (animal.squaredDistanceTo(center) > ATTRACTION_RADIUS * ATTRACTION_RADIUS) {
                continue;
            }
            animal.getNavigation().startMovingTo(center.x, center.y, center.z, 1.2);
        }
    }

    private static void applyAuraEffects(ServerWorld world, Vec3d center) {
        Box box = Box.of(center, FIELD_RADIUS * 2.0, 4.0, FIELD_RADIUS * 2.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.squaredDistanceTo(center) > FIELD_RADIUS * FIELD_RADIUS) {
                continue;
            }

            if (entity.getType().isIn(EntityTypeTags.UNDEAD)) {
                entity.damage(world.getDamageSources().magic(), UNDEAD_DAMAGE);
                continue;
            }

            if (entity instanceof HostileEntity) {
                continue;
            }

            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(FRIENDLY_HEAL);
            }
        }
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.35, center.z, 2, 1.6, 0.2, 1.6, 0.0);
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 2, 1.3, 0.1, 1.3, 0.0);
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 10, 0.9, 0.15, 0.9, 0.0);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.getDefaultState()), center.x, center.y + 0.1, center.z, 10, 0.9, 0.1, 0.9, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DANDELION.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.POPPY.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
    }

    private static void playFieldCreationSound(ServerWorld world, Vec3d center) {
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_GRASS_PLACE,
                SoundCategory.BLOCKS,
                0.9F,
                0.95F + world.getRandom().nextFloat() * 0.2F
        );
    }

    private static List<FlowerPoint> buildPatchPoints(ServerWorld world, Vec3d center) {
        List<FlowerPoint> points = new ArrayList<>();
        for (int i = 0; i < PATCH_VISUAL_POINTS; i++) {
            double angle = ((Math.PI * 2.0) / PATCH_VISUAL_POINTS) * i;
            double ringScale = (0.45 + ((i * 17) % 10) * 0.06);
            double radius = PATCH_VISUAL_RADIUS * Math.min(1.0, ringScale);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + 0.03;
            net.minecraft.block.BlockState state;

            if (i % 4 == 0) {
                state = Blocks.DANDELION.getDefaultState();
            } else if (i % 5 == 0) {
                state = Blocks.POPPY.getDefaultState();
            } else if (i % 3 == 0) {
                state = Blocks.FERN.getDefaultState();
            } else {
                state = Blocks.SHORT_GRASS.getDefaultState();
            }
            points.add(new FlowerPoint(x, y, z, state));
        }
        return points;
    }

    private static void growFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.spawnCursor >= field.pendingPoints.size()) {
            return;
        }

        int spawnCount = Math.min(GROWTH_POINTS_PER_TICK, field.pendingPoints.size() - field.spawnCursor);
        for (int i = 0; i < spawnCount; i++) {
            FlowerPoint point = field.pendingPoints.get(field.spawnCursor++);
            UUID id = spawnBlockDisplay(world, field.displayIds, point.x, point.y, point.z, point.state);
            if (id != null) {
                float tilt = (world.random.nextFloat() * 2.0F - 1.0F) * SPRING_MAX_TILT_DEGREES;
                field.springVisuals.add(new SpringVisual(id, point.x, point.y, point.z, world.getTime(), tilt));
            }
            // Brief upward burst so the patch feels like it's emerging from the ground.
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, point.state), point.x, point.y - 0.08, point.z, 3, 0.08, 0.02, 0.08, 0.03);
            world.spawnParticles(ParticleTypes.COMPOSTER, point.x, point.y + 0.04, point.z, 2, 0.06, 0.01, 0.06, 0.0);
        }
    }

    private static void animateFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.springVisuals.isEmpty()) {
            return;
        }

        field.springVisuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof FallingBlockEntity display)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= SPRING_ANIM_TICKS) {
                display.setPos(visual.targetX(), visual.targetY(), visual.targetZ());
                display.setPitch(0.0F);
                return true;
            }

            float t = (float) age / (float) SPRING_ANIM_TICKS;
            double riseOffset = SPRING_START_OFFSET_Y * (1.0 - t) * (1.0 - t);
            double bounceOffset = Math.sin(t * Math.PI * 2.3) * 0.10 * (1.0 - t);
            double y = visual.targetY() + riseOffset + bounceOffset;
            display.setPos(visual.targetX(), y, visual.targetZ());
            display.setPitch(visual.initialTilt() * (1.0F - t));
            return false;
        });
    }

    private static UUID spawnBlockDisplay(ServerWorld world, List<UUID> ids, double x, double y, double z, net.minecraft.block.BlockState state) {
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        FallingBlockEntity display = FallingBlockEntity.spawnFromBlock(world, pos, state);
        if (display == null) {
            return null;
        }
        display.setFallingBlockPos(display.getBlockPos());
        display.setDestroyedOnLanding();
        display.setHurtEntities(0.0F, 0);
        display.setNoGravity(true);
        ((EntityAccessor) display).simplybows$setNoClip(true);
        display.setVelocity(0.0, 0.0, 0.0);
        display.setPos(x, y + SPRING_START_OFFSET_Y, z);
        display.setYaw(world.random.nextFloat() * 360.0F);
        display.setPitch(0.0F);
        display.addCommandTag(FIELD_VISUAL_TAG);
        ids.add(display.getUuid());
        return display.getUuid();
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

    private static void removeField(ServerWorld world, ActiveFlowerField field) {
        if (field == null) {
            return;
        }

        for (UUID id : field.displayIds) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanFieldVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof FallingBlockEntity && entity.getCommandTags().contains(FIELD_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveFlowerField {
        private final Vec3d center;
        private final long expiryTick;
        private final List<FlowerPoint> pendingPoints;
        private final List<UUID> displayIds = new ArrayList<>();
        private final List<SpringVisual> springVisuals = new ArrayList<>();
        private int spawnCursor;

        private ActiveFlowerField(Vec3d center, long expiryTick, List<FlowerPoint> pendingPoints) {
            this.center = center;
            this.expiryTick = expiryTick;
            this.pendingPoints = pendingPoints;
        }

        private Vec3d center() {
            return this.center;
        }

        private long expiryTick() {
            return this.expiryTick;
        }
    }

    private record FlowerPoint(double x, double y, double z, net.minecraft.block.BlockState state) {
    }

    private record SpringVisual(UUID id, double targetX, double targetY, double targetZ, long spawnTick, float initialTilt) {
    }
}
