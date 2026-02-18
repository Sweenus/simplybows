package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlossomStormManager {

    private static final int STORM_DURATION_TICKS = 200;
    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final int JUMP_INTERVAL_TICKS = 25;
    private static final float STORM_DAMAGE = 1.5F;
    private static final double JUMP_RANGE = 8.0;
    private static final int VORTEX_POINTS = 4;
    private static final Map<ServerWorld, List<ActiveStorm>> ACTIVE_STORMS = new HashMap<>();

    private BlossomStormManager() {
    }

    public static void createStorm(ServerWorld world, Vec3d startPos, LivingEntity directTarget, Entity owner) {
        List<ActiveStorm> existing = ACTIVE_STORMS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (directTarget != null && owner instanceof LivingEntity ownerLiving
                && !CombatTargeting.checkFriendlyFire(directTarget, ownerLiving)) {
            directTarget = null;
        }
        if (ownerId != null) {
            existing.removeIf(storm -> ownerId.equals(storm.ownerId));
        }

        long now = world.getTime();
        ActiveStorm storm = new ActiveStorm(
                now + STORM_DURATION_TICKS,
                now + DAMAGE_INTERVAL_TICKS,
                now + JUMP_INTERVAL_TICKS,
                startPos,
                directTarget != null ? directTarget.getUuid() : null,
                ownerId
        );
        existing.add(storm);
        world.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, SoundCategory.PLAYERS, 0.8F, 0.95F + world.random.nextFloat() * 0.2F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        long now = world.getTime();
        storms.removeIf(storm -> tickStorm(world, storm, now));
        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static boolean tickStorm(ServerWorld world, ActiveStorm storm, long now) {
        if (now >= storm.expiryTick) {
            return true;
        }

        LivingEntity currentTarget = storm.currentTargetId == null ? null : getLivingEntity(world, storm.currentTargetId);
        if (currentTarget != null && currentTarget.isAlive()) {
            storm.center = currentTarget.getPos().add(0.0, currentTarget.getHeight() * 0.5, 0.0);
        }

        spawnVortexParticles(world, storm.center, now);

        if (now >= storm.nextDamageTick) {
            if (currentTarget != null && currentTarget.isAlive()) {
                CombatTargeting.applyDamage(world, getLivingEntityNullable(world, storm.ownerId), currentTarget, STORM_DAMAGE, true);
            }
            storm.nextDamageTick = now + DAMAGE_INTERVAL_TICKS;
        }

        if (now >= storm.nextJumpTick) {
            LivingEntity next = findNextTarget(world, storm, currentTarget);
            if (next != null && (currentTarget == null || !next.getUuid().equals(currentTarget.getUuid()))) {
                Vec3d from = storm.center;
                Vec3d to = next.getPos().add(0.0, next.getHeight() * 0.5, 0.0);
                spawnJumpTrail(world, from, to);
                world.playSound(null, to.x, to.y, to.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.05F + world.random.nextFloat() * 0.2F);
                storm.currentTargetId = next.getUuid();
                storm.center = to;
            }
            storm.nextJumpTick = now + JUMP_INTERVAL_TICKS;
        }

        return false;
    }

    private static LivingEntity findNextTarget(ServerWorld world, ActiveStorm storm, LivingEntity currentTarget) {
        Box search = Box.of(storm.center, JUMP_RANGE * 2.0, 5.0, JUMP_RANGE * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                search,
                entity -> entity.isAlive() && (entity instanceof net.minecraft.entity.mob.HostileEntity || CombatTargeting.isTargetWhitelisted(entity))
        );
        LivingEntity owner = getLivingEntityNullable(world, storm.ownerId);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (storm.ownerId != null && candidate.getUuid().equals(storm.ownerId)) {
                continue;
            }
            if (currentTarget != null && candidate.getUuid().equals(currentTarget.getUuid())) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            double dist = candidate.squaredDistanceTo(storm.center);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static void spawnVortexParticles(ServerWorld world, Vec3d center, long now) {
        double time = now * 0.25;
        for (int i = 0; i < VORTEX_POINTS; i++) {
            double angle = time + (Math.PI * 2.0 / VORTEX_POINTS) * i;
            double radius = 0.85 + 0.2 * Math.sin(time + i);
            double y = center.y - 0.3 + ((i % 4) * 0.22);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 4 == 0) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y + 0.08, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
        }
    }

    private static void spawnJumpTrail(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        int steps = Math.max(8, (int) (delta.length() * 6.0));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d point = from.add(delta.multiply(t));
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity getLivingEntityNullable(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        return getLivingEntity(world, id);
    }

    private static final class ActiveStorm {
        private final long expiryTick;
        private long nextDamageTick;
        private long nextJumpTick;
        private Vec3d center;
        private UUID currentTargetId;
        private final UUID ownerId;

        private ActiveStorm(long expiryTick, long nextDamageTick, long nextJumpTick, Vec3d center, UUID currentTargetId, UUID ownerId) {
            this.expiryTick = expiryTick;
            this.nextDamageTick = nextDamageTick;
            this.nextJumpTick = nextJumpTick;
            this.center = center;
            this.currentTargetId = currentTargetId;
            this.ownerId = ownerId;
        }
    }
}
