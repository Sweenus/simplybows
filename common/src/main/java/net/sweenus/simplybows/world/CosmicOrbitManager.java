package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicOrbitManager {

    private static final String ORBIT_VISUAL_TAG = "simplybows_cosmic_orbit_visual";
    private static final int FADE_TICKS = 12;
    private static final int SCALE_IN_TICKS = 8;

    private static final Map<ServerWorld, List<ActiveOrbit>> ACTIVE_ORBITS = new HashMap<>();

    private CosmicOrbitManager() {
    }

    private static int orbitDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitDurationTicks.get(); }
    private static double attackRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackRadius.get(); }
    private static float attackDamage() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackDamage.get(); }
    private static int minAttackIntervalTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackIntervalMinTicks.get(); }
    private static int maxAttackIntervalTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackIntervalMaxTicks.get(); }
    private static int maxJumps() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitMaxJumps.get(); }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveOrbit> orbits = ACTIVE_ORBITS.get(world);
        return (orbits != null && !orbits.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void createOrRefresh(ServerWorld world, LivingEntity target, Entity owner) {
        if (world == null || target == null || !target.isAlive()) {
            return;
        }

        List<ActiveOrbit> orbits = ACTIVE_ORBITS.computeIfAbsent(world, ignored -> new ArrayList<>());
        ActiveOrbit existing = null;
        for (ActiveOrbit orbit : orbits) {
            if (target.getUuid().equals(orbit.targetId)) {
                existing = orbit;
                break;
            }
        }

        long now = world.getTime();
        if (existing != null) {
            existing.ownerId = owner == null ? null : owner.getUuid();
            existing.targetId = target.getUuid();
            existing.visitedTargets.clear();
            existing.visitedTargets.add(target.getUuid());
            existing.jumpsUsed = 0;
            existing.spawnTick = now;
            existing.expiryTick = now + orbitDurationTicks();
            existing.nextLeapTick = now + randomLeapInterval(world);
            ensureVisual(world, target, existing);
            return;
        }

        Vec3d center = getOrbitCenter(target);
        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, center.x, center.y, center.z);
        visual.addCommandTag(ORBIT_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }

        ActiveOrbit orbit = new ActiveOrbit(
                target.getUuid(),
                owner == null ? null : owner.getUuid(),
                visual.getUuid(),
                now,
                now + orbitDurationTicks(),
                now + randomLeapInterval(world)
        );
        orbit.visitedTargets.add(target.getUuid());
        orbits.add(orbit);

        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.45F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 8, 0.24, 0.24, 0.24, 0.01);
    }

    public static void tick(ServerWorld world) {
        List<ActiveOrbit> orbits = ACTIVE_ORBITS.get(world);
        if (orbits == null || orbits.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        long now = world.getTime();
        orbits.removeIf(orbit -> {
            LivingEntity target = getLivingEntity(world, orbit.targetId);
            if (target == null || !target.isAlive() || now > orbit.expiryTick) {
                discardVisual(world, orbit.visualId);
                return true;
            }
            Entity visualEntity = world.getEntity(orbit.visualId);
            if (!(visualEntity instanceof CosmicOrbitVisualEntity)) {
                return true;
            }
            return false;
        });

        if (orbits.isEmpty()) {
            ACTIVE_ORBITS.remove(world);
            purgeOrphanVisuals(world);
            return;
        }

        for (ActiveOrbit orbit : orbits) {
            LivingEntity target = getLivingEntity(world, orbit.targetId);
            if (target == null) {
                continue;
            }
            updateVisual(world, orbit, target);
            tryLeap(world, orbit, target);
        }
    }

    private static void updateVisual(ServerWorld world, ActiveOrbit orbit, LivingEntity target) {
        Entity visualEntity = world.getEntity(orbit.visualId);
        if (!(visualEntity instanceof CosmicOrbitVisualEntity visual)) {
            return;
        }

        Vec3d center = getOrbitCenter(target);
        visual.setPos(center.x, center.y, center.z);

        long now = world.getTime();
        float scaleIn = MathHelper.clamp((float) (now - orbit.spawnTick) / SCALE_IN_TICKS, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((float) (orbit.expiryTick - now) / FADE_TICKS, 0.0F, 1.0F);
        visual.setVisualScale(Math.min(scaleIn, fadeOut));
    }

    private static void tryLeap(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        long now = world.getTime();
        if (now < orbit.nextLeapTick || orbit.jumpsUsed >= maxJumps()) {
            return;
        }

        List<LivingEntity> targets = findNearbyTargets(world, orbit, anchor);
        if (targets.isEmpty()) {
            orbit.nextLeapTick = now + randomLeapInterval(world);
            return;
        }

        LivingEntity target = targets.get(world.random.nextInt(targets.size()));
        leapToTarget(world, orbit, anchor, target);
    }

    private static List<LivingEntity> findNearbyTargets(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        double radius = attackRadius();
        Vec3d center = getOrbitCenter(anchor);
        Entity owner = orbit.ownerId == null ? null : world.getEntity(orbit.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        double maxDistanceSq = radius * radius;

        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity != anchor
                        && entity != owner
                        && entity.squaredDistanceTo(center) <= maxDistanceSq
                        && !orbit.visitedTargets.contains(entity.getUuid())
                        && CombatTargeting.isOffensiveTargetCandidate(entity, ownerLiving)
                        && anchor.canSee(entity));
        candidates.sort((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)));
        return candidates;
    }

    private static void leapToTarget(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        Entity owner = orbit.ownerId == null ? null : world.getEntity(orbit.ownerId);
        Vec3d end = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.55, 0.0);

        CombatTargeting.applyDamage(world, owner, target, attackDamage(), true, false);
        orbit.targetId = target.getUuid();
        orbit.visitedTargets.add(target.getUuid());
        orbit.jumpsUsed++;
        orbit.nextLeapTick = world.getTime() + randomLeapInterval(world);

        if (orbit.jumpsUsed >= maxJumps()) {
            orbit.expiryTick = Math.min(orbit.expiryTick, world.getTime() + FADE_TICKS);
        }

        playImpactSound(world, end);
        world.spawnParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.0);
    }

    private static void playImpactSound(ServerWorld world, Vec3d pos) {
        float chimePitch = 0.95F + world.random.nextFloat() * 0.18F;
        float anchorPitch = 1.65F + world.random.nextFloat() * 0.16F;
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.75F, chimePitch);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.28F, anchorPitch);
    }

    private static void ensureVisual(ServerWorld world, LivingEntity target, ActiveOrbit orbit) {
        Entity entity = world.getEntity(orbit.visualId);
        if (entity instanceof CosmicOrbitVisualEntity) {
            return;
        }

        Vec3d center = getOrbitCenter(target);
        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, center.x, center.y, center.z);
        visual.addCommandTag(ORBIT_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            orbit.visualId = visual.getUuid();
        }
    }

    private static int randomLeapInterval(ServerWorld world) {
        int min = Math.max(1, minAttackIntervalTicks());
        int max = Math.max(min, maxAttackIntervalTicks());
        return min + world.random.nextInt(max - min + 1);
    }

    private static Vec3d getOrbitCenter(LivingEntity target) {
        return target.getPos().add(0.0, target.getStandingEyeHeight() * 0.62, 0.0);
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof CosmicOrbitVisualEntity && entity.getCommandTags().contains(ORBIT_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static class ActiveOrbit {
        private UUID targetId;
        private UUID ownerId;
        private UUID visualId;
        private long spawnTick;
        private long expiryTick;
        private long nextLeapTick;
        private int jumpsUsed;
        private final List<UUID> visitedTargets = new ArrayList<>();

        private ActiveOrbit(UUID targetId, UUID ownerId, UUID visualId, long spawnTick, long expiryTick, long nextLeapTick) {
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.nextLeapTick = nextLeapTick;
        }
    }
}
