package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class VineFlowerVisualEntity extends Entity {

    private static final TrackedData<Integer> FLOWER_TYPE = DataTracker.registerData(VineFlowerVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(VineFlowerVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public VineFlowerVisualEntity(EntityType<? extends VineFlowerVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public VineFlowerVisualEntity(World world, double x, double y, double z, int flowerType) {
        this(EntityRegistry.VINE_FLOWER_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setFlowerType(flowerType);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(FLOWER_TYPE, 0);
        builder.add(HEIGHT_SCALE, 0.0F);
    }

    public void setFlowerType(int flowerType) {
        this.dataTracker.set(FLOWER_TYPE, flowerType);
    }

    public int getFlowerType() {
        return this.dataTracker.get(FLOWER_TYPE);
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
        if (nbt.contains("flower_type")) {
            this.setFlowerType(nbt.getInt("flower_type"));
        }
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("flower_type", this.getFlowerType());
        nbt.putFloat("height_scale", this.getHeightScale());
    }
}
