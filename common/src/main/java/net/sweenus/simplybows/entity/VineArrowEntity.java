package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.world.VineFlowerFieldManager;

public class VineArrowEntity extends ArrowEntity {

    private static final double EXTRA_DRAG_XZ = 0.94;
    private static final double EXTRA_DRAG_Y = 0.90;
    private static final String FIELD_VISUAL_TAG = "simplybows_vine_field_visual";
    private boolean spawnedFlowerField;

    public VineArrowEntity(EntityType<? extends VineArrowEntity> type, World world) {
        super(type, world);
    }

    public VineArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(world, owner, sanitizeArrowStack(arrowStack), weaponStack);
        this.setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.inGround) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.x * EXTRA_DRAG_XZ, velocity.y * EXTRA_DRAG_Y, velocity.z * EXTRA_DRAG_XZ);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        trySpawnFlowerField(blockHitResult.getPos());
        super.onBlockHit(blockHitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        trySpawnFlowerField(entityHitResult.getPos());
        super.onEntityHit(entityHitResult);
    }

    @Override
    public boolean canHit(Entity entity) {
        if (entity.getCommandTags().contains(FIELD_VISUAL_TAG)) {
            return false;
        }
        return super.canHit(entity);
    }

    private void trySpawnFlowerField(Vec3d hitPos) {
        if (this.spawnedFlowerField) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            VineFlowerFieldManager.createOrReplaceField(serverWorld, hitPos);
            this.spawnedFlowerField = true;
        }
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
