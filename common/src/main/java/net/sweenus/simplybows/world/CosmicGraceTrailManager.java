package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicGraceTrailManager {

    private static final int BUFF_REFRESH_TICKS = 10;
    private static final int VISUAL_DURATION_PADDING_TICKS = 10;
    private static final Map<ServerWorld, List<ActiveGraceField>> ACTIVE_FIELDS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> FIELD_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private CosmicGraceTrailManager() {
    }

    private static int fieldDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldDurationTicks.get(); }
    private static int fieldCooldownTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldCooldownTicks.get(); }
    private static double fieldRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldRadius.get(); }
    private static int buffDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldBuffDurationTicks.get(); }
    private static int buffAmplifier() { return SimplyBowsConfig.INSTANCE.cosmicBow.graceFieldBuffAmplifier.get(); }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveGraceField> fields = ACTIVE_FIELDS.get(world);
        return (fields != null && !fields.isEmpty()) || !getCooldowns(world).isEmpty() || (world.getTime() % 20L == 0L);
    }

    public static void createField(ServerWorld world, Entity owner, Vec3d pos) {
        if (world == null || owner == null || pos == null || owner.isRemoved()) {
            return;
        }
        UUID ownerId = owner.getUuid();
        if (!isFieldReady(world, ownerId)) {
            return;
        }

        int duration = Math.max(1, fieldDurationTicks());
        ActiveGraceField field = new ActiveGraceField(pos, ownerId, world.getTime() + duration);
        ACTIVE_FIELDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(field);

        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, pos.x, pos.y, pos.z);
        visual.setFieldMode(true);
        visual.setLifetimeTicks(duration + VISUAL_DURATION_PADDING_TICKS);
        world.spawnEntity(visual);

        startFieldCooldown(world, ownerId);
        if (owner instanceof ServerPlayerEntity player) {
            int cooldownTicks = Math.max(20, fieldCooldownTicks());
            SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + cooldownTicks * 50L, cooldownTicks);
        }
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveGraceField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveGraceField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            ActiveGraceField field = iterator.next();
            if (field.expiresAt <= now) {
                iterator.remove();
                continue;
            }
            if (now >= field.nextBuffTick) {
                field.nextBuffTick = now + BUFF_REFRESH_TICKS;
                applyFieldBuffs(world, field);
            }
        }

        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
        }
    }

    private static void applyFieldBuffs(ServerWorld world, ActiveGraceField field) {
        Entity owner = world.getEntity(field.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        if (ownerLiving == null || !ownerLiving.isAlive()) {
            return;
        }

        double radius = Math.max(0.1, fieldRadius());
        Box searchBox = Box.of(field.pos, radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && entity.squaredDistanceTo(field.pos) <= radius * radius
                        && CombatTargeting.isFriendlyTo(entity, ownerLiving));

        int duration = Math.max(BUFF_REFRESH_TICKS + 5, buffDurationTicks());
        int amplifier = Math.max(0, buffAmplifier());
        for (LivingEntity living : entities) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, amplifier, true, true, true), owner);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, true, true, true), owner);
        }
    }

    private static boolean isFieldReady(ServerWorld world, UUID ownerId) {
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startFieldCooldown(ServerWorld world, UUID ownerId) {
        getCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + Math.max(20, fieldCooldownTicks()));
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(FIELD_COOLDOWNS_BY_SERVER, world);
    }

    private static class ActiveGraceField {
        private final Vec3d pos;
        private final UUID ownerId;
        private final long expiresAt;
        private long nextBuffTick;

        private ActiveGraceField(Vec3d pos, UUID ownerId, long expiresAt) {
            this.pos = pos;
            this.ownerId = ownerId;
            this.expiresAt = expiresAt;
            this.nextBuffTick = 0;
        }
    }
}
