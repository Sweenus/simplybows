package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.item.unique.BeeBowItem;
import net.sweenus.simplybows.item.unique.BlossomBowItem;
import net.sweenus.simplybows.item.unique.BubbleBowItem;
import net.sweenus.simplybows.item.unique.EarthBowItem;
import net.sweenus.simplybows.item.unique.EchoBowItem;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.item.unique.VineBowItem;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

import java.util.UUID;

public class ShoulderBowEntity extends Entity {

    private static final TrackedData<Integer> SIDE = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULL_STAGE = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> MIRROR_OFFHAND = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MIRROR_ITEM_RAW_ID = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int DRAW_TICKS = 9;
    private static final int AUTO_FIRE_COOLDOWN_TICKS = 40;
    private static final int TARGET_SCAN_TICKS = 10;
    private static final double TARGET_RADIUS = 18.0;
    private static final double STRING_TARGET_RADIUS_DELTA = 2.0;
    private static final double FRAME_TARGET_RADIUS_DELTA = 2.0;
    private static final int STRING_COOLDOWN_DELTA = 8;
    private static final int FRAME_COOLDOWN_DELTA = 8;
    private static final int MIN_TARGET_RADIUS = 6;
    private static final int MAX_TARGET_RADIUS = 40;
    private static final int MIN_COOLDOWN_TICKS = 10;
    private static final int MAX_COOLDOWN_TICKS = 80;
    public static final double SHOULDER_SIDE_OFFSET = 0.68;
    public static final double SHOULDER_BACK_OFFSET = -0.16;
    public static final double SHOULDER_HEIGHT_OFFSET = -0.15;
    private static final double FOLLOW_LERP = 0.24;
    private static final double FOLLOW_TELEPORT_DISTANCE_SQ = 9.0;
    private static final float IDLE_AIM_SMOOTHING = 0.25F;
    private static final double BOB_AMPLITUDE = 0.06;
    private static final double BOB_SPEED = 0.22;
    private static final float ARROW_SPEED = 2.7F;
    private static final float ARROW_DIVERGENCE = 0.85F;
    private UUID ownerUuid;
    private UUID trackedTargetUuid;
    private Vec3d queuedShotDirection;
    private Vec3d preparedShotDirection;
    private int drawTicksRemaining;
    private int cooldownTicks;
    private long lastFiredTick = -AUTO_FIRE_COOLDOWN_TICKS;

    public ShoulderBowEntity(EntityType<? extends ShoulderBowEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public ShoulderBowEntity(World world, ServerPlayerEntity owner, int side) {
        this(EntityRegistry.SHOULDER_BOW.get(), world);
        this.ownerUuid = owner.getUuid();
        this.setSide(side);
        this.setPullStage(0);
        this.setOwnerEntityId(owner.getId());
        this.refreshShoulderTransform(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SIDE, 1);
        builder.add(PULL_STAGE, 0);
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(MIRROR_OFFHAND, false);
        builder.add(MIRROR_ITEM_RAW_ID, Item.getRawId(Items.AIR));
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public int getSide() {
        return this.dataTracker.get(SIDE);
    }

    public void setSide(int side) {
        this.dataTracker.set(SIDE, side < 0 ? -1 : 1);
    }

    public int getPullStage() {
        return this.dataTracker.get(PULL_STAGE);
    }

    public void setPullStage(int stage) {
        this.dataTracker.set(PULL_STAGE, MathHelper.clamp(stage, 0, 3));
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setOwnerEntityId(int entityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, entityId);
    }

    public boolean isMirroringOffhand() {
        return this.dataTracker.get(MIRROR_OFFHAND);
    }

    public int getMirroredItemRawId() {
        return this.dataTracker.get(MIRROR_ITEM_RAW_ID);
    }

    public void configureOffhandMirror(boolean enabled, ItemStack mirroredStack) {
        this.dataTracker.set(MIRROR_OFFHAND, enabled);
        Item item = mirroredStack == null || mirroredStack.isEmpty() ? Items.AIR : mirroredStack.getItem();
        this.dataTracker.set(MIRROR_ITEM_RAW_ID, Item.getRawId(item));
    }

    public void queueLookShot(Vec3d direction) {
        if (direction.lengthSquared() > 1.0E-6) {
            this.queuedShotDirection = direction.normalize();
        }
    }

    @Override
    public void tick() {
        this.noClip = true;
        this.setNoGravity(true);
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || !(owner.getMainHandStack().getItem() instanceof EchoBowItem)) {
            this.discard();
            return;
        }

        this.setOwnerEntityId(owner.getId());
        refreshShoulderTransform(owner);
        spawnIdleParticles();
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }

        if (this.drawTicksRemaining > 0) {
            this.drawTicksRemaining--;
            this.setPullStage(stageForRemainingDrawTicks(this.drawTicksRemaining));
            updateAimDuringDraw(owner);
            if (this.drawTicksRemaining == 0) {
                firePreparedArrow(owner);
            }
            return;
        }

        this.setPullStage(0);

        if (this.queuedShotDirection != null && this.cooldownTicks <= 0) {
            beginDrawing(this.queuedShotDirection, null);
            this.queuedShotDirection = null;
            return;
        }

        int scanTicks = getDynamicScanTicks(owner);
        if (this.cooldownTicks > 0 || this.age % scanTicks != 0) {
            return;
        }

        LivingEntity target = findNearestHostile(owner, getDynamicTargetRadius(owner));
        if (target != null) {
            Vec3d aim = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.6, 0.0).subtract(this.getPos());
            beginDrawing(aim, target.getUuid());
        }
    }

    private ServerPlayerEntity getOwnerPlayer() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.ownerUuid == null) {
            return null;
        }
        return serverWorld.getServer().getPlayerManager().getPlayer(this.ownerUuid);
    }

    private void refreshShoulderTransform(PlayerEntity owner) {
        Vec3d forward = Vec3d.fromPolar(0.0F, owner.getYaw()).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double sideOffset = SHOULDER_SIDE_OFFSET * (double) this.getSide();
        double bob = Math.sin((this.age + (this.getSide() > 0 ? 6.0 : 0.0)) * BOB_SPEED) * BOB_AMPLITUDE;
        Vec3d shoulder = owner.getPos()
                .add(0.0, owner.getStandingEyeHeight() + SHOULDER_HEIGHT_OFFSET + bob, 0.0)
                .add(right.multiply(sideOffset))
                .add(forward.multiply(SHOULDER_BACK_OFFSET));
        if (this.age < 2 || this.squaredDistanceTo(shoulder.x, shoulder.y, shoulder.z) > FOLLOW_TELEPORT_DISTANCE_SQ) {
            this.setPos(shoulder.x, shoulder.y, shoulder.z);
        } else {
            Vec3d smoothed = this.getPos().lerp(shoulder, FOLLOW_LERP);
            this.setPos(smoothed.x, smoothed.y, smoothed.z);
        }

        if (this.preparedShotDirection == null || this.preparedShotDirection.lengthSquared() <= 1.0E-6 || this.trackedTargetUuid == null) {
            setLookToDirectionSmooth(owner.getRotationVec(1.0F), IDLE_AIM_SMOOTHING);
        }
    }

    private LivingEntity findNearestHostile(ServerPlayerEntity owner, double targetRadius) {
        LivingEntity nearest = null;
        double nearestDist = targetRadius * targetRadius;
        for (HostileEntity hostile : this.getWorld().getEntitiesByClass(HostileEntity.class, owner.getBoundingBox().expand(targetRadius, 8.0, targetRadius), Entity::isAlive)) {
            double dist = hostile.squaredDistanceTo(this.getX(), this.getY(), this.getZ());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = hostile;
            }
        }
        return nearest;
    }

    private void beginDrawing(Vec3d direction, UUID targetUuid) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        this.preparedShotDirection = direction.normalize();
        this.trackedTargetUuid = targetUuid;
        this.drawTicksRemaining = DRAW_TICKS;
        this.setPullStage(1);
    }

    private void updateAimDuringDraw(ServerPlayerEntity owner) {
        if (this.trackedTargetUuid != null && this.getWorld() instanceof ServerWorld serverWorld) {
            Entity tracked = serverWorld.getEntity(this.trackedTargetUuid);
            if (tracked instanceof LivingEntity living && living.isAlive()) {
                this.preparedShotDirection = living.getPos().add(0.0, living.getStandingEyeHeight() * 0.6, 0.0).subtract(this.getPos()).normalize();
            } else {
                this.trackedTargetUuid = null;
            }
        }

        if (this.preparedShotDirection == null || this.preparedShotDirection.lengthSquared() <= 1.0E-6) {
            this.preparedShotDirection = owner.getRotationVec(1.0F).normalize();
        }

        setLookToDirection(this.preparedShotDirection);
    }

    private void firePreparedArrow(ServerPlayerEntity owner) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        int dynamicCooldown = getDynamicCooldown(owner);
        long now = serverWorld.getTime();
        if (this.lastFiredTick >= 0L && now - this.lastFiredTick < dynamicCooldown) {
            this.cooldownTicks = Math.max(this.cooldownTicks, (int) (dynamicCooldown - (now - this.lastFiredTick)));
            this.preparedShotDirection = null;
            this.trackedTargetUuid = null;
            this.setPullStage(0);
            return;
        }

        Vec3d direction = this.preparedShotDirection;
        if (direction == null || direction.lengthSquared() <= 1.0E-6) {
            direction = owner.getRotationVec(1.0F);
        }

        ProjectileEntity arrow = createCompanionArrow(serverWorld, owner, direction);
        if (arrow == null) {
            this.preparedShotDirection = null;
            this.trackedTargetUuid = null;
            this.setPullStage(0);
            return;
        }
        serverWorld.spawnEntity(arrow);

        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.45F, 1.35F + serverWorld.random.nextFloat() * 0.1F);
        this.lastFiredTick = now;
        this.cooldownTicks = dynamicCooldown + (this.getSide() > 0 ? 4 : 0);
        this.preparedShotDirection = null;
        this.trackedTargetUuid = null;
        this.setPullStage(0);
    }

    private double getDynamicTargetRadius(ServerPlayerEntity owner) {
        BowUpgradeData upgrades = BowUpgradeData.from(owner.getMainHandStack());
        double radius = TARGET_RADIUS
                - upgrades.stringLevel() * STRING_TARGET_RADIUS_DELTA
                + upgrades.frameLevel() * FRAME_TARGET_RADIUS_DELTA;
        return MathHelper.clamp(radius, MIN_TARGET_RADIUS, MAX_TARGET_RADIUS);
    }

    private int getDynamicCooldown(ServerPlayerEntity owner) {
        BowUpgradeData upgrades = BowUpgradeData.from(owner.getMainHandStack());
        int cooldown = AUTO_FIRE_COOLDOWN_TICKS
                - upgrades.stringLevel() * STRING_COOLDOWN_DELTA
                + upgrades.frameLevel() * FRAME_COOLDOWN_DELTA;
        return MathHelper.clamp(cooldown, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
    }

    private int getDynamicScanTicks(ServerPlayerEntity owner) {
        int cooldown = getDynamicCooldown(owner);
        return MathHelper.clamp(cooldown / 4, 2, TARGET_SCAN_TICKS);
    }

    private ProjectileEntity createCompanionArrow(ServerWorld world, ServerPlayerEntity owner, Vec3d direction) {
        ItemStack mainHand = owner.getMainHandStack();
        ItemStack offHand = owner.getOffHandStack();
        ItemStack arrowStack = owner.getProjectileType(this.isMirroringOffhand() ? offHand : mainHand);
        if (arrowStack == null || arrowStack.isEmpty()) {
            arrowStack = new ItemStack(Items.ARROW);
        }

        float speed = ARROW_SPEED;
        float divergence = ARROW_DIVERGENCE;
        ProjectileEntity projectile;

        if (this.isMirroringOffhand() && offHand.getItem() instanceof SimplyBowItem offhandBow) {
            BowUpgradeData upgrades = BowUpgradeData.from(offHand);
            if (offhandBow instanceof VineBowItem) {
                speed = 1.65F * (float) (1.0 + upgrades.stringLevel() * 0.05);
                divergence = 1.15F;
                VineArrowEntity arrow = new VineArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(1.5 * upgrades.damageMultiplier());
                projectile = arrow;
            } else if (offhandBow instanceof EarthBowItem) {
                speed = 2.16F * (float) (1.0 + upgrades.stringLevel() * 0.05);
                divergence = 0.9F;
                EarthArrowEntity arrow = new EarthArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(2.0 * upgrades.damageMultiplier());
                projectile = arrow;
            } else if (offhandBow instanceof BubbleBowItem) {
                speed = 2.85F;
                divergence = 0.8F;
                BubbleArrowEntity arrow = new BubbleArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(1.75);
                projectile = arrow;
            } else if (offhandBow instanceof BeeBowItem) {
                speed = 1.44F;
                divergence = 0.7F;
                BeeArrowEntity arrow = new BeeArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(2.0);
                projectile = arrow;
            } else if (offhandBow instanceof BlossomBowItem) {
                speed = 2.34F;
                divergence = 0.85F;
                BlossomArrowEntity arrow = new BlossomArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(1.5);
                projectile = arrow;
            } else if (offhandBow instanceof IceBowItem) {
                speed = 3.6F;
                divergence = 1.0F;
                double damageMultiplier = upgrades.damageMultiplier();
                RuneEtching rune = upgrades.runeEtching();
                if (rune == RuneEtching.PAIN) {
                    damageMultiplier *= 1.5;
                } else if (rune == RuneEtching.BOUNTY) {
                    damageMultiplier *= 0.75;
                }

                if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
                    HomingSpectralArrowEntity arrow = new HomingSpectralArrowEntity(world, owner, arrowStack, offHand);
                    arrow.setDamage(2.0 * damageMultiplier);
                    arrow.setLockSingleTarget(rune == RuneEtching.PAIN);
                    arrow.setStackingSlowness(rune == RuneEtching.GRACE);
                    if (rune == RuneEtching.PAIN && this.trackedTargetUuid != null) {
                        arrow.setLockedTargetUuid(this.trackedTargetUuid);
                    }
                    projectile = arrow;
                } else {
                    HomingArrowEntity arrow = new HomingArrowEntity(world, owner, arrowStack, offHand);
                    arrow.setDamage(2.0 * damageMultiplier);
                    arrow.setLockSingleTarget(rune == RuneEtching.PAIN);
                    arrow.setStackingSlowness(rune == RuneEtching.GRACE);
                    if (rune == RuneEtching.PAIN && this.trackedTargetUuid != null) {
                        arrow.setLockedTargetUuid(this.trackedTargetUuid);
                    }
                    projectile = arrow;
                }
            } else {
                EchoArrowEntity arrow = new EchoArrowEntity(world, owner, arrowStack, mainHand);
                arrow.setDamage(2.0);
                projectile = arrow;
                speed = ARROW_SPEED;
                divergence = ARROW_DIVERGENCE;
            }
        } else {
            EchoArrowEntity arrow = new EchoArrowEntity(world, owner, arrowStack, mainHand);
            arrow.setDamage(2.0);
            projectile = arrow;
            speed = ARROW_SPEED;
            divergence = ARROW_DIVERGENCE;
        }

        if (projectile instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent) {
            persistent.setCritical(this.getPullStage() >= 3);
        }
        projectile.setPosition(this.getX(), this.getY() + 0.02, this.getZ());
        projectile.setVelocity(direction.x, direction.y, direction.z, speed, divergence);
        return projectile;
    }

    private static int stageForRemainingDrawTicks(int remainingTicks) {
        if (remainingTicks >= 6) {
            return 1;
        }
        if (remainingTicks >= 3) {
            return 2;
        }
        return 3;
    }

    private void setLookToDirection(Vec3d direction) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI));
        float pitch = (float) (-(Math.atan2(direction.y, direction.horizontalLength()) * (180.0F / Math.PI)));
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    private void setLookToDirectionSmooth(Vec3d direction, float smoothing) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        float targetYaw = (float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI));
        float targetPitch = (float) (-(Math.atan2(direction.y, direction.horizontalLength()) * (180.0F / Math.PI)));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - this.getYaw());
        float pitchDelta = targetPitch - this.getPitch();
        this.setYaw(this.getYaw() + yawDelta * smoothing);
        this.setPitch(this.getPitch() + pitchDelta * smoothing);
    }

    private void spawnIdleParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.age % 3 != 0) {
            return;
        }
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.08, this.getZ(), 2, 0.08, 0.04, 0.08, 0.0);
    }


    @Override
    public boolean isAttackable() {
        return false;
    }


    @Override
    public boolean isCollidable() {
        return false;
    }


    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
        if (nbt.contains("side")) {
            this.setSide(nbt.getInt("side"));
        }
        if (nbt.contains("pull_stage")) {
            this.setPullStage(nbt.getInt("pull_stage"));
        }
        if (nbt.contains("mirror_offhand")) {
            this.dataTracker.set(MIRROR_OFFHAND, nbt.getBoolean("mirror_offhand"));
        }
        if (nbt.contains("mirror_item_id")) {
            this.dataTracker.set(MIRROR_ITEM_RAW_ID, nbt.getInt("mirror_item_id"));
        }
        this.drawTicksRemaining = nbt.getInt("draw_ticks");
        this.cooldownTicks = nbt.getInt("cooldown_ticks");
        if (nbt.contains("last_fired_tick")) {
            this.lastFiredTick = nbt.getLong("last_fired_tick");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putInt("side", this.getSide());
        nbt.putInt("pull_stage", this.getPullStage());
        nbt.putBoolean("mirror_offhand", this.dataTracker.get(MIRROR_OFFHAND));
        nbt.putInt("mirror_item_id", this.dataTracker.get(MIRROR_ITEM_RAW_ID));
        nbt.putInt("draw_ticks", this.drawTicksRemaining);
        nbt.putInt("cooldown_ticks", this.cooldownTicks);
        nbt.putLong("last_fired_tick", this.lastFiredTick);
    }
}
