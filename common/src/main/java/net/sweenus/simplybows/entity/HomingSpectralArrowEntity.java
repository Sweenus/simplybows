package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HomingSpectralArrowEntity extends SpectralArrowEntity {

    private static final double HOMING_RADIUS = 16.0; // Radius to detect mobs
    private static final double HOMING_ACCEL = 0.3; // Strength of homing adjustment
    private static final int HOMING_START_TICKS = 15; // Delay before arrows begin homing
    private static final float INITIAL_SPREAD_YAW_RADIANS = 0.90F; // ~20.1 degrees
    private static final float INITIAL_SPREAD_PITCH_RADIANS = 0.14F; // ~8.0 degrees
    private static final double START_SPEED = 0.2; // Initial speed cap at spawn
    private static final double MAX_SPEED = 0.7; // Maximum speed cap over lifetime
    private static final int SPEED_RAMP_TICKS = 40; // Ticks to ramp from START_SPEED to MAX_SPEED
    private LivingEntity target;
    private boolean initialSpreadApplied;
    private boolean lockSingleTarget;
    private boolean stackingSlowness;
    private UUID lockedTargetUuid;

    public HomingSpectralArrowEntity(EntityType<? extends HomingSpectralArrowEntity> type, World world) {
        super(type, world);
    }

    public HomingSpectralArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(world, owner, sanitizeArrowStack(arrowStack), weaponStack);
        this.setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick(); // Ensure the arrow continues to behave like a normal arrow

        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            spawnTrailParticles(serverWorld);
        }

        updateNoGravityState();

        if (!this.getWorld().isClient() && !this.initialSpreadApplied) {
            applyInitialSpread();
        }

        limitVelocityToCurrentMaxSpeed();

        // Keep model rotation aligned with velocity on both sides.
        Vec3d vel = this.getVelocity();
        if (vel.lengthSquared() > 1.0E-6) {
            float yaw = (float)(Math.atan2(vel.x, vel.z) * (180F / Math.PI));
            float pitch = (float)(Math.atan2(vel.y, vel.horizontalLength()) * (180F / Math.PI));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
            this.setYaw(yaw);
            this.setPitch(pitch);
            this.velocityDirty = true;
        }

        if (!this.getWorld().isClient()) {
            if (this.lockSingleTarget && this.lockedTargetUuid != null && this.target == null && this.getWorld() instanceof ServerWorld serverWorld) {
                net.minecraft.entity.Entity e = serverWorld.getEntity(this.lockedTargetUuid);
                if (e instanceof LivingEntity living && living.isAlive()) {
                    this.target = living;
                }
            }
            if (this.inGround) {
                this.target = null;
                return;
            }

            if (this.age < HOMING_START_TICKS) {
                return;
            }

            // Locate a target if none exists
            if (target == null || !target.isAlive()) {
                if (this.lockSingleTarget) {
                    this.target = null;
                    return;
                }
                target = findNearestHostileMob();
            }

            // Adjust arrow trajectory toward the target
            if (target != null) {
                adjustTrajectoryTowardTarget();
            }
        }
    }

    /**
     * Finds the nearest hostile mob to the arrow within the HOMING_RADIUS.
     */
    private LivingEntity findNearestHostileMob() {
        Box searchBox = new Box(this.getX() - HOMING_RADIUS, this.getY() - HOMING_RADIUS, this.getZ() - HOMING_RADIUS,
                this.getX() + HOMING_RADIUS, this.getY() + HOMING_RADIUS, this.getZ() + HOMING_RADIUS);

        List<LivingEntity> entities = getEntityWorld().getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity instanceof HostileEntity && entity.isAlive());

        if (!entities.isEmpty()) {
            Set<LivingEntity> avoided = new HashSet<>();
            if (this.getOwner() != null) {
                for (HomingArrowEntity arrow : getEntityWorld().getEntitiesByClass(
                        HomingArrowEntity.class, searchBox, arrow -> arrow.getOwner() == this.getOwner())) {
                    LivingEntity otherTarget = arrow.getTargetEntity();
                    if (otherTarget != null && otherTarget.isAlive()) {
                        avoided.add(otherTarget);
                    }
                }
                for (HomingSpectralArrowEntity arrow : getEntityWorld().getEntitiesByClass(
                        HomingSpectralArrowEntity.class, searchBox, arrow -> arrow != this && arrow.getOwner() == this.getOwner())) {
                    LivingEntity otherTarget = arrow.getTargetEntity();
                    if (otherTarget != null && otherTarget.isAlive()) {
                        avoided.add(otherTarget);
                    }
                }
            }

            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity entity : entities) {
                if (avoided.contains(entity)) {
                    continue;
                }
                double dist = this.squaredDistanceTo(entity);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entity;
                }
            }
            if (best != null) {
                return best;
            }

            // Fallback: allow duplicates if not enough unique targets
            return entities.getFirst();
        }

        return null;
    }

    /**
     * Adjusts the arrow's velocity to steer toward the target.
     */
    private void adjustTrajectoryTowardTarget() {
        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getStandingEyeHeight(), target.getZ());
        Vec3d arrowPos = this.getPos();

        // Calculate the direction vector toward the target
        Vec3d direction = targetPos.subtract(arrowPos).normalize();

        // Smoothly adjust the velocity toward the target
        Vec3d newVelocity = this.getVelocity().add(direction.multiply(HOMING_ACCEL));
        double speedCap = getCurrentMaxSpeed();
        if (newVelocity.lengthSquared() > (speedCap * speedCap)) {
            newVelocity = newVelocity.normalize().multiply(speedCap);
        }

        this.setVelocity(newVelocity);

        // Update the pitch and yaw for proper rendering
        float yaw = (float)(Math.atan2(newVelocity.x, newVelocity.z) * (180F / Math.PI));
        float pitch = (float)(Math.atan2(newVelocity.y, newVelocity.horizontalLength()) * (180F / Math.PI));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.velocityDirty = true;
    }

    private double getCurrentMaxSpeed() {
        double ramp = Math.min(1.0, (double) this.age / SPEED_RAMP_TICKS);
        return START_SPEED + (MAX_SPEED - START_SPEED) * ramp;
    }

    private void applyInitialSpread() {
        this.initialSpreadApplied = true;
        if (this.age > 1) {
            return;
        }

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-6) {
            return;
        }

        float yawJitter = (this.random.nextFloat() * 2.0F - 1.0F) * INITIAL_SPREAD_YAW_RADIANS;
        float pitchJitter = (this.random.nextFloat() * 2.0F - 1.0F) * INITIAL_SPREAD_PITCH_RADIANS;
        Vec3d spreadVelocity = velocity.rotateY(yawJitter).rotateX(pitchJitter);
        this.setVelocity(spreadVelocity);
        this.velocityDirty = true;
    }

    private void updateNoGravityState() {
        this.setNoGravity(!this.inGround && this.age < HOMING_START_TICKS);
    }

    private void spawnTrailParticles(ServerWorld world) {
        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-6) {
            return;
        }

        world.spawnParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.03, 0.03, 0.03, 0.005);
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.04, 0.04, 0.04, 0.002);
        }
    }

    private void spawnImpactParticles(ServerWorld world, LivingEntity hitTarget) {
        double x = hitTarget.getX();
        double y = hitTarget.getBodyY(0.5);
        double z = hitTarget.getZ();
        world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 18, 0.25, 0.25, 0.25, 0.06);
        world.spawnParticles(ParticleTypes.WHITE_ASH, x, y, z, 10, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.15, 0.15, 0.15, 0.01);
    }

    private void limitVelocityToCurrentMaxSpeed() {
        Vec3d velocity = this.getVelocity();
        double speedSq = velocity.lengthSquared();
        if (speedSq <= 1.0E-6) {
            return;
        }

        double speedCap = getCurrentMaxSpeed();
        if (speedSq > speedCap * speedCap) {
            this.setVelocity(velocity.normalize().multiply(speedCap));
            this.velocityDirty = true;
        }
    }

    @Override
    protected void onHit(LivingEntity target) {
        // Allow multiple fan arrows to damage in the same tick by clearing invulnerability frames.
        target.hurtTime = 0;
        target.timeUntilRegen = 0;
        applyStackingSlow(target);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnImpactParticles(serverWorld, target);
        }
        super.onHit(target);
    }

    LivingEntity getTargetEntity() {
        return this.target;
    }

    public void setLockSingleTarget(boolean lockSingleTarget) {
        this.lockSingleTarget = lockSingleTarget;
    }

    public void setLockedTargetUuid(UUID lockedTargetUuid) {
        this.lockedTargetUuid = lockedTargetUuid;
    }

    public void setStackingSlowness(boolean stackingSlowness) {
        this.stackingSlowness = stackingSlowness;
    }

    private void applyStackingSlow(LivingEntity target) {
        if (!this.stackingSlowness) {
            return;
        }
        int amplifier = 0;
        StatusEffectInstance existing = target.getStatusEffect(StatusEffects.SLOWNESS);
        if (existing != null) {
            amplifier = Math.min(4, existing.getAmplifier() + 1);
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, amplifier), this.getOwner());
    }

    private static ItemStack sanitizeArrowStack(ItemStack arrowStack) {
        if (arrowStack == null || arrowStack.isEmpty()) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }
        ItemStack copy = arrowStack.copy();
        copy.setCount(1);
        return copy;
    }
}
