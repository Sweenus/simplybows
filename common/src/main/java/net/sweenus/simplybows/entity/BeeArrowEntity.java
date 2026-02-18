package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class BeeArrowEntity extends ArrowEntity {

    private static final int POISON_DURATION_TICKS = 80;
    private static final int POISON_AMPLIFIER = 0;
    private static final double MIN_HORIZONTAL_SPEED_SQ_FOR_YAW = 1.0E-4;
    private static final float ROTATION_SMOOTHING = 0.35F;
    private boolean spawnSoundPlayed;

    public BeeArrowEntity(EntityType<? extends BeeArrowEntity> type, World world) {
        super(type, world);
    }

    public BeeArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.BEE_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
    }

    @Override
    public void tick() {
        super.tick();

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();

            float targetPitch = (float)(-(Math.atan2(velocity.y, velocity.horizontalLength()) * (180F / Math.PI)));
            float pitchDelta = MathHelper.wrapDegrees(targetPitch - this.getPitch());
            this.setPitch(this.getPitch() + pitchDelta * ROTATION_SMOOTHING);

            if (velocity.horizontalLengthSquared() > MIN_HORIZONTAL_SPEED_SQ_FOR_YAW) {
                float targetYaw = (float)(Math.atan2(velocity.z, velocity.x) * (180F / Math.PI)) - 90.0F;
                float yawDelta = MathHelper.wrapDegrees(targetYaw - this.getYaw());
                this.setYaw(this.getYaw() + yawDelta * ROTATION_SMOOTHING);
            }
            this.velocityDirty = true;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld && !this.spawnSoundPlayed) {
            this.spawnSoundPlayed = true;
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_BEE_HURT, SoundCategory.PLAYERS, 0.6F, 1.0F + this.random.nextFloat() * 0.2F);
        }

        if (this.inGround && this.getWorld() instanceof ServerWorld serverWorld) {
            spawnPoofAndDiscard(serverWorld);
            return;
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }

        if (!(entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER), this.getOwner());

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
            serverWorld.spawnParticles(ParticleTypes.POOF, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 10, 0.15, 0.12, 0.15, 0.02);
            serverWorld.spawnParticles(ParticleTypes.FALLING_HONEY, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
            serverWorld.spawnParticles(ParticleTypes.CRIT, livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ(), 6, 0.15, 0.15, 0.15, 0.02);
        }
    }

    private void spawnPoofAndDiscard(ServerWorld world) {
        world.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.15, 0.12, 0.15, 0.02);
        world.spawnParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.12, 0.08, 0.12, 0.0);
        this.discard();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.ARROW);
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
