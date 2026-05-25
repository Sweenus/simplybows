package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.world.CosmicOrbitManager;

public class CosmicArrowEntity extends ArrowEntity {

    private final BowUpgradeData upgrades;

    public CosmicArrowEntity(EntityType<? extends CosmicArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public CosmicArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(EntityRegistry.COSMIC_ARROW.get(), world);
        this.setStack(sanitizeArrowStack(arrowStack));
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.prevX = owner.getX();
        this.prevY = owner.getEyeY() - 0.1;
        this.prevZ = owner.getZ();
        this.upgrades = BowUpgradeData.from(weaponStack);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = entityHitResult.getPos();
            serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 10, 0.15, 0.15, 0.15, 0.0);
            playImpactSound(serverWorld, pos);
            if (entityHitResult.getEntity() instanceof LivingEntity target) {
                CosmicOrbitManager.createOrRefresh(serverWorld, target, this.getOwner(), this.upgrades);
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = blockHitResult.getPos();
            serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 8, 0.12, 0.12, 0.12, 0.0);
            playImpactSound(serverWorld, pos);
        }
    }

    private void playImpactSound(ServerWorld world, Vec3d pos) {
        float chimePitch = 0.95F + this.random.nextFloat() * 0.18F;
        float anchorPitch = 1.65F + this.random.nextFloat() * 0.16F;
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.75F, chimePitch);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.28F, anchorPitch);
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
