package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class BeeGraceVisualEntity extends Entity {

    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(BeeGraceVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BeeGraceVisualEntity(EntityType<? extends BeeGraceVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BeeGraceVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.BEE_GRACE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setHeightScale(1.0F);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(HEIGHT_SCALE, 1.0F);
    }

    public void setHeightScale(float heightScale) {
        this.dataTracker.set(HEIGHT_SCALE, heightScale);
    }

    public float getHeightScale() {
        return this.dataTracker.get(HEIGHT_SCALE);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("height_scale", this.getHeightScale());
    }
}
