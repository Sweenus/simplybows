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

public class EchoChaosBlackHoleVisualEntity extends Entity {

    private static final TrackedData<Float> VISUAL_SCALE =
            DataTracker.registerData(EchoChaosBlackHoleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> VISUAL_RADIUS =
            DataTracker.registerData(EchoChaosBlackHoleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // Large bounding box so the ring (up to 12-block radius) survives frustum culling
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(26.0F, 2.0F);

    public EchoChaosBlackHoleVisualEntity(EntityType<? extends EchoChaosBlackHoleVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public EchoChaosBlackHoleVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.ECHO_CHAOS_BLACK_HOLE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setVisualScale(1.0F);
        this.setVisualRadius(1.0F);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(VISUAL_SCALE, 1.0F);
        this.dataTracker.startTracking(VISUAL_RADIUS, 1.0F);
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

    public float getVisualRadius() {
        return this.dataTracker.get(VISUAL_RADIUS);
    }

    public void setVisualRadius(float radius) {
        this.dataTracker.set(VISUAL_RADIUS, radius);
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
        if (nbt.contains("visual_radius")) {
            this.setVisualRadius(nbt.getFloat("visual_radius"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_scale", this.getVisualScale());
        nbt.putFloat("visual_radius", this.getVisualRadius());
    }
}
