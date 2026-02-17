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
        List<UUID> displayIds = spawnDisplayPatch(world, center);
        ACTIVE_FIELDS.put(world, new ActiveFlowerField(center, expiryTick, displayIds));
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

    private static List<UUID> spawnDisplayPatch(ServerWorld world, Vec3d center) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < PATCH_VISUAL_POINTS; i++) {
            double angle = ((Math.PI * 2.0) / PATCH_VISUAL_POINTS) * i;
            double ringScale = (0.45 + ((i * 17) % 10) * 0.06);
            double radius = PATCH_VISUAL_RADIUS * Math.min(1.0, ringScale);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + 0.03;

            if (i % 4 == 0) {
                spawnBlockDisplay(world, ids, x, y, z, Blocks.DANDELION.getDefaultState());
            } else if (i % 5 == 0) {
                spawnBlockDisplay(world, ids, x, y, z, Blocks.POPPY.getDefaultState());
            } else if (i % 3 == 0) {
                spawnBlockDisplay(world, ids, x, y, z, Blocks.FERN.getDefaultState());
            } else {
                spawnBlockDisplay(world, ids, x, y, z, Blocks.SHORT_GRASS.getDefaultState());
            }
        }
        return ids;
    }

    private static void spawnBlockDisplay(ServerWorld world, List<UUID> ids, double x, double y, double z, net.minecraft.block.BlockState state) {
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        FallingBlockEntity display = FallingBlockEntity.spawnFromBlock(world, pos, state);
        if (display == null) {
            return;
        }
        display.setFallingBlockPos(display.getBlockPos());
        display.setDestroyedOnLanding();
        display.setHurtEntities(0.0F, 0);
        display.setNoGravity(true);
        ((EntityAccessor) display).simplybows$setNoClip(true);
        display.setVelocity(0.0, 0.0, 0.0);
        display.setPos(x, y, z);
        display.setYaw(world.random.nextFloat() * 360.0F);
        display.setPitch(0.0F);
        display.addCommandTag(FIELD_VISUAL_TAG);
        ids.add(display.getUuid());
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

        for (UUID id : field.displayIds()) {
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

    private record ActiveFlowerField(Vec3d center, long expiryTick, List<UUID> displayIds) {
    }
}
