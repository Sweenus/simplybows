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

public class KoiFishVisualEntity extends Entity {

    private static final TrackedData<Float> VISUAL_SCALE =
            DataTracker.registerData(KoiFishVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SWIM_ANGLE =
            DataTracker.registerData(KoiFishVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> LARGE_VARIANT =
            DataTracker.registerData(KoiFishVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BASE_COLOR_RGB =
            DataTracker.registerData(KoiFishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PATTERN_INDEX =
            DataTracker.registerData(KoiFishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public KoiFishVisualEntity(EntityType<? extends KoiFishVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public KoiFishVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.KOI_FISH_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setVisualScale(0.0F);
        this.setSwimAngle(0.0F);
        this.setLargeVariant(false);
        this.setBaseColorRgb(0xF28B2D);
        this.setPatternIndex(0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(VISUAL_SCALE, 0.0F);
        builder.add(SWIM_ANGLE, 0.0F);
        builder.add(LARGE_VARIANT, false);
        builder.add(BASE_COLOR_RGB, 0xF28B2D);
        builder.add(PATTERN_INDEX, 0);
    }

    public void setVisualScale(float scale) {
        this.dataTracker.set(VISUAL_SCALE, scale);
    }

    public float getVisualScale() {
        return this.dataTracker.get(VISUAL_SCALE);
    }

    public void setSwimAngle(float angle) {
        this.dataTracker.set(SWIM_ANGLE, angle);
    }

    public float getSwimAngle() {
        return this.dataTracker.get(SWIM_ANGLE);
    }

    public void setLargeVariant(boolean largeVariant) {
        this.dataTracker.set(LARGE_VARIANT, largeVariant);
    }

    public boolean isLargeVariant() {
        return this.dataTracker.get(LARGE_VARIANT);
    }

    public void setBaseColorRgb(int rgb) {
        this.dataTracker.set(BASE_COLOR_RGB, rgb);
    }

    public int getBaseColorRgb() {
        return this.dataTracker.get(BASE_COLOR_RGB);
    }

    public void setPatternIndex(int patternIndex) {
        this.dataTracker.set(PATTERN_INDEX, Math.max(0, patternIndex));
    }

    public int getPatternIndex() {
        return this.dataTracker.get(PATTERN_INDEX);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return EntityDimensions.fixed(1.2F, 0.9F);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("visual_scale")) {
            this.setVisualScale(nbt.getFloat("visual_scale"));
        }
        if (nbt.contains("swim_angle")) {
            this.setSwimAngle(nbt.getFloat("swim_angle"));
        }
        if (nbt.contains("large_variant")) {
            this.setLargeVariant(nbt.getBoolean("large_variant"));
        }
        if (nbt.contains("base_color_rgb")) {
            this.setBaseColorRgb(nbt.getInt("base_color_rgb"));
        }
        if (nbt.contains("pattern_index")) {
            this.setPatternIndex(nbt.getInt("pattern_index"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_scale", this.getVisualScale());
        nbt.putFloat("swim_angle", this.getSwimAngle());
        nbt.putBoolean("large_variant", this.isLargeVariant());
        nbt.putInt("base_color_rgb", this.getBaseColorRgb());
        nbt.putInt("pattern_index", this.getPatternIndex());
    }
}
