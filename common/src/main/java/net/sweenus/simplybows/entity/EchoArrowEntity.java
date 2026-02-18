package net.sweenus.simplybows.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.world.EchoShoulderBowManager;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class EchoArrowEntity extends ArrowEntity {

    private static final float PAIN_EXPLOSION_BASE_MAX_HEALTH_DAMAGE = 0.16F;
    private static final float PAIN_EXPLOSION_FRAME_MAX_HEALTH_DAMAGE = 0.06F;
    private static final float PAIN_EXPLOSION_MAX_DAMAGE_RATIO = 0.55F;
    private static final float PAIN_EXPLOSION_MIN_DAMAGE = 2.0F;
    private static final double PAIN_EXPLOSION_BASE_RADIUS = 2.35;
    private static final double PAIN_EXPLOSION_STRING_RADIUS_BONUS = 0.55;
    private final BowUpgradeData upgrades;

    public EchoArrowEntity(EntityType<? extends EchoArrowEntity> type, World world) {
        super(type, world);
        this.upgrades = BowUpgradeData.none();
    }

    public EchoArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, ItemStack weaponStack) {
        super(world, owner, sanitizeArrowStack(arrowStack), weaponStack);
        this.upgrades = BowUpgradeData.from(weaponStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld && !this.inGround) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        LivingEntity hitLiving = entityHitResult.getEntity() instanceof LivingEntity living ? living : null;
        boolean wasAliveBeforeHit = hitLiving != null && hitLiving.isAlive();

        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.timeUntilRegen = 0;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld && hitLiving != null) {
            if (this.getOwner() instanceof ServerPlayerEntity player && this.upgrades.runeEtching() == RuneEtching.PAIN) {
                EchoShoulderBowManager.setFocusedTarget(player, hitLiving);
            }
            if (this.upgrades.runeEtching() == RuneEtching.PAIN && wasAliveBeforeHit && !hitLiving.isAlive()) {
                triggerPainArcaneChain(serverWorld, hitLiving);
            }
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, entityHitResult.getPos().x, entityHitResult.getPos().y + 0.1, entityHitResult.getPos().z, 10, 0.15, 0.15, 0.15, 0.0);
            serverWorld.playSound(null, entityHitResult.getPos().x, entityHitResult.getPos().y, entityHitResult.getPos().z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 1.25F);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, blockHitResult.getPos().x, blockHitResult.getPos().y + 0.1, blockHitResult.getPos().z, 8, 0.12, 0.12, 0.12, 0.0);
        }
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.ARROW);
    }

    private void triggerPainArcaneChain(ServerWorld world, LivingEntity initialVictim) {
        if (initialVictim == null) {
            return;
        }

        ArrayDeque<LivingEntity> queue = new ArrayDeque<>();
        Set<java.util.UUID> explodedVictims = new HashSet<>();
        queue.add(initialVictim);

        while (!queue.isEmpty()) {
            LivingEntity sourceVictim = queue.removeFirst();
            if (sourceVictim == null || !explodedVictims.add(sourceVictim.getUuid())) {
                continue;
            }

            double radius = PAIN_EXPLOSION_BASE_RADIUS + this.upgrades.stringLevel() * PAIN_EXPLOSION_STRING_RADIUS_BONUS;
            float healthRatio = PAIN_EXPLOSION_BASE_MAX_HEALTH_DAMAGE + this.upgrades.frameLevel() * PAIN_EXPLOSION_FRAME_MAX_HEALTH_DAMAGE;
            float clampedRatio = Math.min(PAIN_EXPLOSION_MAX_DAMAGE_RATIO, healthRatio);
            float damage = Math.max(PAIN_EXPLOSION_MIN_DAMAGE, sourceVictim.getMaxHealth() * clampedRatio);
            Vec3d center = sourceVictim.getPos().add(0.0, sourceVictim.getStandingEyeHeight() * 0.5, 0.0);

            spawnArcaneExplosionEffects(world, center, radius);

            Box damageBox = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
            for (LivingEntity nearby : world.getEntitiesByClass(LivingEntity.class, damageBox, entity ->
                    entity.isAlive() && entity != sourceVictim && sourceVictim.squaredDistanceTo(entity) <= radius * radius)) {
                boolean wasAlive = nearby.isAlive();
                if (!CombatTargeting.applyDamage(world, this.getOwner(), nearby, damage, true)) {
                    continue;
                }
                if (wasAlive && !nearby.isAlive()) {
                    queue.addLast(nearby);
                }
            }
        }
    }

    private static void spawnArcaneExplosionEffects(ServerWorld world, Vec3d center, double radius) {
        int enchantCount = (int) Math.max(18, 26 + radius * 7.0);
        int witchCount = (int) Math.max(10, 12 + radius * 4.0);
        int dragonCount = (int) Math.max(12, 14 + radius * 5.0);
        spawnArcaneDomeShell(world, center, radius);
        spawnArcaneGroundRing(world, center, radius);

        world.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, enchantCount, radius * 0.45, radius * 0.28, radius * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.WITCH, center.x, center.y, center.z, witchCount, radius * 0.38, radius * 0.24, radius * 0.38, 0.02);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, dragonCount, radius * 0.35, radius * 0.2, radius * 0.35, 0.01);
        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 22, radius * 0.4, radius * 0.3, radius * 0.4, 0.2);

        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.7F, 0.65F + world.random.nextFloat() * 0.1F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.45F, 1.2F + world.random.nextFloat() * 0.15F);
    }

    private static void spawnArcaneDomeShell(ServerWorld world, Vec3d center, double radius) {
        int layers = 8;
        int pointsPerRing = 24;
        for (int layer = 0; layer <= layers; layer++) {
            double t = (double) layer / (double) layers;
            double theta = t * (Math.PI * 0.5); // hemisphere
            double y = center.y + Math.sin(theta) * radius;
            double ringRadius = Math.cos(theta) * radius;
            for (int i = 0; i < pointsPerRing; i++) {
                double angle = (Math.PI * 2.0 * i) / pointsPerRing;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
                if ((i & 1) == 0) {
                    world.spawnParticles(ParticleTypes.WITCH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void spawnArcaneGroundRing(ServerWorld world, Vec3d center, double radius) {
        int points = 36;
        double y = center.y - radius * 0.15;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.0, 0.0, 0.0, 0.02);
            if ((i % 3) == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.02, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
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
