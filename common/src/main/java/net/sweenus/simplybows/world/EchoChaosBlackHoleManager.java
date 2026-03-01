package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector3f;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.EchoChaosBlackHoleVisualEntity;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EchoChaosBlackHoleManager {

    private static final Map<ServerWorld, List<ActiveBlackHole>> ACTIVE_BLACK_HOLES = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> BLACK_HOLE_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();
    private static final String BLACK_HOLE_VISUAL_TAG = "simplybows_echo_chaos_black_hole_visual";

    private EchoChaosBlackHoleManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveBlackHole> holes = ACTIVE_BLACK_HOLES.get(world);
        return (holes != null && !holes.isEmpty()) || !getCooldowns(world).isEmpty() || (world.getTime() % 20L == 0L);
    }

    public static boolean isBlackHoleReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    public static void spawnAtImpact(ServerWorld world, Vec3d center, UUID ownerId, int stringLevel, int frameLevel) {
        if (world == null || center == null) {
            return;
        }

        List<ActiveBlackHole> holes = ACTIVE_BLACK_HOLES.computeIfAbsent(world, w -> new ArrayList<>());
        if (ownerId != null) {
            holes.removeIf(hole -> {
                if (ownerId.equals(hole.ownerId)) {
                    discardVisual(world, hole);
                    return true;
                }
                return false;
            });
        }

        int durationTicks = Math.max(20,
                SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDurationTicks.get()
                        + Math.max(0, stringLevel) * SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDurationPerStringTicks.get());
        int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleCooldownTicks.get());
        double radius = Math.max(1.0, SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleRadius.get());
        double pullStrength = Math.max(0.0,
                SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHolePullStrength.get()
                        + Math.max(0, frameLevel) * SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHolePullStrengthPerFrame.get());
        Vec3d floatCenter = new Vec3d(center.x, center.y + 1.0, center.z);
        ActiveBlackHole hole = new ActiveBlackHole(floatCenter, ownerId, world.getTime(), world.getTime() + durationTicks, radius, pullStrength);
        spawnVisual(world, hole);
        holes.add(hole);

        if (ownerId != null) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).put(ownerId, now + durationTicks + cooldownTicks);
        }

        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.8F, 0.55F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.PLAYERS, 0.35F, 0.5F + world.random.nextFloat() * 0.05F);
        world.spawnParticles(ParticleTypes.SMOKE, center.x, center.y + 0.1, center.z, 18, 0.4, 0.25, 0.4, 0.01);
        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y + 0.2, center.z, 24, 0.55, 0.3, 0.55, 0.15);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y + 0.25, center.z, 14, 0.4, 0.2, 0.4, 0.01);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveBlackHole> holes = ACTIVE_BLACK_HOLES.get(world);
        if (holes == null || holes.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Iterator<ActiveBlackHole> iterator = holes.iterator();
        while (iterator.hasNext()) {
            ActiveBlackHole hole = iterator.next();
            if (world.getTime() >= hole.expiryTick) {
                discardVisual(world, hole);
                iterator.remove();
                continue;
            }

            animateVisual(world, hole);
            applyPullAndDamage(world, hole);
            spawnInfallParticles(world, hole);
        }

        if (holes.isEmpty()) {
            ACTIVE_BLACK_HOLES.remove(world);
            purgeOrphanVisuals(world);
        }
    }

    private static void applyPullAndDamage(ServerWorld world, ActiveBlackHole hole) {
        double pullStrength = hole.pullStrength;
        double itemPullStrength = pullStrength * 0.35;
        double orbPullStrength = pullStrength * 0.40;
        int damageInterval = Math.max(1, SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDamageIntervalTicks.get());
        float damage = SimplyBowsConfig.INSTANCE.echoBow.chaosBlackHoleDamagePerTick.get();
        LivingEntity owner = getOwnerEntity(world, hole.ownerId);

        Box effectBounds = Box.of(hole.center, hole.radius * 2.0, Math.max(3.0, hole.radius * 1.4), hole.radius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                effectBounds,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            Vec3d centerTarget = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.4, 0.0);
            Vec3d toCenter = hole.center.subtract(centerTarget);
            double dist = toCenter.length();
            if (dist > hole.radius || dist <= 1.0E-6) {
                continue;
            }

            Vec3d pullDir = toCenter.normalize();
            double falloff = 1.0 - MathHelper.clamp(dist / hole.radius, 0.0, 1.0);
            double sizeMultiplier = getSizePullMultiplier(target);
            if (sizeMultiplier <= 0.0) {
                continue;
            }
            Vec3d velocity = target.getVelocity().multiply(0.7).add(pullDir.multiply(pullStrength * sizeMultiplier * (0.45 + falloff)));
            target.setVelocity(velocity.x, Math.max(velocity.y, -0.05), velocity.z);
            target.velocityDirty = true;

            if (world.getTime() % damageInterval == 0L) {
                boolean wasAlive = target.isAlive();
                boolean damaged = CombatTargeting.applyDamage(world, owner, target, damage, true, false);
                if (damaged && wasAlive && !target.isAlive()) {
                    hole.radius = Math.min(hole.baseRadius * 2.0, hole.radius + 1.0);
                }
            }
        }

        for (ItemEntity itemEntity : world.getEntitiesByClass(
                ItemEntity.class,
                effectBounds,
                Entity::isAlive
        )) {
            Vec3d toCenter = hole.center.subtract(itemEntity.getPos());
            double dist = toCenter.length();
            if (dist > hole.radius || dist <= 1.0E-6) {
                continue;
            }

            Vec3d pullDir = toCenter.normalize();
            double falloff = 1.0 - MathHelper.clamp(dist / hole.radius, 0.0, 1.0);
            double sizeMultiplier = getSizePullMultiplier(itemEntity);
            if (sizeMultiplier <= 0.0) {
                continue;
            }
            Vec3d velocity = itemEntity.getVelocity().multiply(0.82).add(pullDir.multiply(itemPullStrength * sizeMultiplier * (0.45 + falloff)));
            itemEntity.setVelocity(velocity.x, Math.max(velocity.y, -0.08), velocity.z);
            itemEntity.velocityDirty = true;
        }

        for (ExperienceOrbEntity orbEntity : world.getEntitiesByClass(
                ExperienceOrbEntity.class,
                effectBounds,
                Entity::isAlive
        )) {
            Vec3d toCenter = hole.center.subtract(orbEntity.getPos());
            double dist = toCenter.length();
            if (dist > hole.radius || dist <= 1.0E-6) {
                continue;
            }

            Vec3d pullDir = toCenter.normalize();
            double falloff = 1.0 - MathHelper.clamp(dist / hole.radius, 0.0, 1.0);
            double sizeMultiplier = getSizePullMultiplier(orbEntity);
            if (sizeMultiplier <= 0.0) {
                continue;
            }
            Vec3d velocity = orbEntity.getVelocity().multiply(0.82).add(pullDir.multiply(orbPullStrength * sizeMultiplier * (0.45 + falloff)));
            orbEntity.setVelocity(velocity.x, Math.max(velocity.y, -0.08), velocity.z);
            orbEntity.velocityDirty = true;
        }
    }

    private static double getSizePullMultiplier(Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        double size = Math.max(entity.getWidth(), entity.getHeight());
        final double fullPullAtOrBelow = 0.9;
        final double noPullAtOrAbove = 3.0;
        if (size <= fullPullAtOrBelow) {
            return 1.0;
        }
        if (size >= noPullAtOrAbove) {
            return 0.0;
        }
        double t = (size - fullPullAtOrBelow) / (noPullAtOrAbove - fullPullAtOrBelow);
        return 1.0 - MathHelper.clamp(t, 0.0, 1.0);
    }

    private static final Vector3f INFILL_COLOR = new Vector3f(1.0F, 0.42F, 0.0F);

    private static void spawnInfallParticles(ServerWorld world, ActiveBlackHole hole) {
        DustParticleEffect dust = new DustParticleEffect(INFILL_COLOR, 0.7F);
        int count = Math.min(8, 3 + (int) (hole.radius * 0.35));

        for (int i = 0; i < count; i++) {
            double angle = world.getRandom().nextDouble() * 2.0 * Math.PI;
            double r = hole.radius * (0.95 + world.getRandom().nextDouble() * 0.12);
            double sx = hole.center.x + Math.cos(angle) * r;
            double sz = hole.center.z + Math.sin(angle) * r;
            double sy = hole.center.y + 0.35 + (world.getRandom().nextDouble() - 0.5) * 0.35;

            double dx = hole.center.x - sx;
            double dz = hole.center.z - sz;
            double len = Math.sqrt(dx * dx + dz * dz);
            double speed = 0.09 + world.getRandom().nextDouble() * 0.07;

            // count=0 → directed-velocity mode: particle spawns at (sx,sy,sz) with exact velocity
            world.spawnParticles(dust, sx, sy, sz, 0,
                    dx / len * speed, -0.008, dz / len * speed, 1.0);
        }

        // Faint singularity core
        if (world.getTime() % 3L == 0L) {
            int core = Math.max(1, (int) hole.radius / 3);
            world.spawnParticles(ParticleTypes.PORTAL,
                    hole.center.x, hole.center.y + 0.3, hole.center.z,
                    core, 0.05, 0.05, 0.05, 0.04);
        }
    }

    private static void spawnVisual(ServerWorld world, ActiveBlackHole hole) {
        EchoChaosBlackHoleVisualEntity visual = new EchoChaosBlackHoleVisualEntity(world, hole.center.x, hole.center.y + 0.05, hole.center.z);
        visual.addCommandTag(BLACK_HOLE_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            hole.visualId = visual.getUuid();
        }
    }

    private static void animateVisual(ServerWorld world, ActiveBlackHole hole) {
        if (hole.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(hole.visualId);
        if (!(entity instanceof EchoChaosBlackHoleVisualEntity visual)) {
            hole.visualId = null;
            return;
        }

        long age = world.getTime() - hole.spawnTick;
        long remaining = hole.expiryTick - world.getTime();
        float fadeIn = MathHelper.clamp(age / 10.0F, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp(remaining / 10.0F, 0.0F, 1.0F);
        float pulse = 0.92F + (float) Math.sin((world.getTime() + visual.getId()) * 0.18F) * 0.08F;
        float rawRatio = (float) (hole.radius / hole.baseRadius);
        float cubeGrowth = Math.max(0.90F, Math.min(1.20F, 0.90F + (rawRatio - 1.0F) * 0.12F));
        float finalScale = Math.max(0.05F, Math.min(fadeIn, fadeOut) * pulse * cubeGrowth);
        visual.setVisualScale(finalScale);
        visual.setVisualRadius((float) hole.radius);
        double lift = 0.05 + Math.max(0.0, cubeGrowth - 1.0F) * 0.2;
        visual.setPos(hole.center.x, hole.center.y + lift, hole.center.z);
    }

    private static void discardVisual(ServerWorld world, ActiveBlackHole hole) {
        if (hole.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(hole.visualId);
        if (entity != null) {
            entity.discard();
        }
        hole.visualId = null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof EchoChaosBlackHoleVisualEntity && entity.getCommandTags().contains(BLACK_HOLE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(BLACK_HOLE_COOLDOWNS_BY_SERVER, world);
    }

    private static final class ActiveBlackHole {
        private final Vec3d center;
        private final UUID ownerId;
        private final long spawnTick;
        private final long expiryTick;
        private final double baseRadius;
        private final double pullStrength;
        private double radius;
        private UUID visualId;

        private ActiveBlackHole(Vec3d center, UUID ownerId, long spawnTick, long expiryTick, double radius, double pullStrength) {
            this.center = center;
            this.ownerId = ownerId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.baseRadius = radius;
            this.pullStrength = pullStrength;
            this.radius = radius;
        }
    }
}
