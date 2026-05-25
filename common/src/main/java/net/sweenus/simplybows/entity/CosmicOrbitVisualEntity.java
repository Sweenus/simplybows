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
    private static final TrackedData<Boolean> PAIN_MODE =
            DataTracker.registerData(CosmicOrbitVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FIELD_MODE =
            DataTracker.registerData(CosmicOrbitVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LIFETIME_TICKS =
            DataTracker.registerData(CosmicOrbitVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

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
        builder.add(PAIN_MODE, false);
        builder.add(FIELD_MODE, false);
        builder.add(LIFETIME_TICKS, 0);
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

    public boolean isPainMode() {
        return this.dataTracker.get(PAIN_MODE);
    }

    public void setPainMode(boolean painMode) {
        this.dataTracker.set(PAIN_MODE, painMode);
    }

    public boolean isFieldMode() {
        return this.dataTracker.get(FIELD_MODE);
    }

    public void setFieldMode(boolean fieldMode) {
        this.dataTracker.set(FIELD_MODE, fieldMode);
    }

    public int getLifetimeTicks() {
        return this.dataTracker.get(LIFETIME_TICKS);
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.dataTracker.set(LIFETIME_TICKS, Math.max(0, lifetimeTicks));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        int lifetime = this.getLifetimeTicks();
        if (!this.getWorld().isClient && lifetime > 0 && this.age > lifetime) {
            this.discard();
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("visual_scale")) {
            this.setVisualScale(nbt.getFloat("visual_scale"));
        }
        this.setPainMode(nbt.getBoolean("pain_mode"));
        this.setFieldMode(nbt.getBoolean("field_mode"));
        this.setLifetimeTicks(nbt.getInt("lifetime_ticks"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_scale", this.getVisualScale());
        nbt.putBoolean("pain_mode", this.isPainMode());
        nbt.putBoolean("field_mode", this.isFieldMode());
        nbt.putInt("lifetime_ticks", this.getLifetimeTicks());
    }
}
