package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplybows.registry.EntityRegistry;

public class CosmicOrbitVisualEntity extends Entity {

    private static final TrackedData<Float> VISUAL_SCALE =
            DataTracker.registerData(CosmicOrbitVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(4.0F, 4.0F).withEyeHeight(2.0F);

    public CosmicOrbitVisualEntity(EntityType<? extends CosmicOrbitVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public CosmicOrbitVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.COSMIC_ORBIT_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setVisualScale(1.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(VISUAL_SCALE, 1.0F);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return DIMENSIONS;
    }

    public float getVisualScale() {
        return this.dataTracker.get(VISUAL_SCALE);
    }

    public void setVisualScale(float visualScale) {
        this.dataTracker.set(VISUAL_SCALE, visualScale);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("visual_scale")) {
            this.setVisualScale(nbt.getFloat("visual_scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_scale", this.getVisualScale());
    }
}
