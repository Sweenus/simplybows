package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class BeeHiveVisualEntity extends Entity {

    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(BeeHiveVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BeeHiveVisualEntity(EntityType<? extends BeeHiveVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BeeHiveVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.BEE_HIVE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(HEIGHT_SCALE, 0.0F);
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
