package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomingArrowEntity extends ArrowEntity {

    private static final double HOMING_RADIUS = 16.0; // Radius to detect mobs
    private static final double HOMING_ACCEL = 0.4; // Strength of homing adjustment
    private static final double MAX_SPEED = 0.7; // Cap homing arrow speed
    private LivingEntity target;
    public HomingArrowEntity(EntityType<? extends HomingArrowEntity> type, World world) {
        super(type, world);
    }

    public HomingArrowEntity(World world, LivingEntity owner) {
        super(EntityRegistry.HOMING_ARROW.get(), world);
        this.setOwner(owner);
    }

    public HomingArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(world, owner, sanitizeArrowStack(arrowStack), weaponStack);
        this.setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick(); // Ensure the arrow continues to behave like a normal arrow

        // Keep model rotation aligned with velocity on both sides.
        Vec3d vel = this.getVelocity();
        if (vel.lengthSquared() > 1.0E-6) {
            float yaw = (float)(-Math.atan2(vel.x, vel.z) * (180F / Math.PI));
            float pitch = (float)(-Math.atan2(vel.y, vel.horizontalLength()) * (180F / Math.PI));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
            this.setYaw(yaw);
            this.setPitch(pitch);
            this.velocityDirty = true;
        }

        if (!this.getWorld().isClient()) {
            // Locate a target if none exists
            if (target == null || !target.isAlive()) {
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
                        HomingArrowEntity.class, searchBox, arrow -> arrow != this && arrow.getOwner() == this.getOwner())) {
                    LivingEntity otherTarget = arrow.getTargetEntity();
                    if (otherTarget != null && otherTarget.isAlive()) {
                        avoided.add(otherTarget);
                    }
                }
                for (HomingSpectralArrowEntity arrow : getEntityWorld().getEntitiesByClass(
                        HomingSpectralArrowEntity.class, searchBox, arrow -> arrow.getOwner() == this.getOwner())) {
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
        if (newVelocity.lengthSquared() > (MAX_SPEED * MAX_SPEED)) {
            newVelocity = newVelocity.normalize().multiply(MAX_SPEED);
        }

        this.setVelocity(newVelocity);

        // Update the pitch and yaw for proper rendering
        float yaw = (float)(-Math.atan2(newVelocity.x, newVelocity.z) * (180F / Math.PI));
        float pitch = (float)(-Math.atan2(newVelocity.y, newVelocity.horizontalLength()) * (180F / Math.PI));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.velocityDirty = true;
    }

    @Override
    protected void onHit(LivingEntity target) {
        // Allow multiple fan arrows to damage in the same tick by clearing invulnerability frames.
        target.hurtTime = 0;
        target.timeUntilRegen = 0;
        super.onHit(target);
        // Handle any additional logic for when the arrow hits its target
    }

    LivingEntity getTargetEntity() {
        return this.target;
    }

    @Override
    protected void onCollision(net.minecraft.util.hit.HitResult hitResult) {
        super.onCollision(hitResult);
        // Handle any additional logic for collisions
    }

    private static ItemStack sanitizeArrowStack(ItemStack arrowStack) {
        if (arrowStack == null || arrowStack.isEmpty()) {
            return new ItemStack(Items.ARROW);
        }
        ItemStack copy = arrowStack.copy();
        copy.setCount(1);
        return copy;
    }
}
