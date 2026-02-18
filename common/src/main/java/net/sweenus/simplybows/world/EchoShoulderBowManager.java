package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.item.unique.EchoBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EchoShoulderBowManager {

    private static final Map<UUID, CompanionPair> ACTIVE_BOWS = new HashMap<>();
    private static final Map<UUID, UUID> FOCUSED_TARGETS = new HashMap<>();
    private static final double LOOK_TARGET_DISTANCE = 48.0;

    private EchoShoulderBowManager() {
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        boolean holdingEchoBow = player.getMainHandStack().getItem() instanceof EchoBowItem;
        if (!holdingEchoBow) {
            discardTracked(world, ownerId);
            ACTIVE_BOWS.remove(ownerId);
            FOCUSED_TARGETS.remove(ownerId);
            return;
        }

        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        ShoulderBowEntity left = resolveTracked(world, pair != null ? pair.leftId() : null, ownerId, -1);
        ShoulderBowEntity right = resolveTracked(world, pair != null ? pair.rightId() : null, ownerId, 1);

        if (left == null) {
            left = new ShoulderBowEntity(world, player, -1);
            world.spawnEntity(left);
        }
        if (right == null) {
            right = new ShoulderBowEntity(world, player, 1);
            world.spawnEntity(right);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(player.getMainHandStack());
        boolean mirrorOffhandBow = upgrades.runeEtching() == RuneEtching.BOUNTY
                && player.getOffHandStack().getItem() instanceof SimplyBowItem
                && !(player.getOffHandStack().getItem() instanceof EchoBowItem);
        UUID focusedTargetId = null;
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            focusedTargetId = FOCUSED_TARGETS.get(ownerId);
            validateFocusedTarget(player, focusedTargetId);
            focusedTargetId = FOCUSED_TARGETS.get(ownerId);
        } else {
            FOCUSED_TARGETS.remove(ownerId);
        }

        left.configureOffhandMirror(false, ItemStack.EMPTY);
        right.configureOffhandMirror(mirrorOffhandBow, mirrorOffhandBow ? player.getOffHandStack() : ItemStack.EMPTY);
        left.setForcedTargetUuid(focusedTargetId);
        right.setForcedTargetUuid(focusedTargetId);

        ACTIVE_BOWS.put(ownerId, new CompanionPair(left.getUuid(), right.getUuid()));

        if ((world.getTime() + player.getId()) % 40L == 0L) {
            cleanupOwnerOrphans(world, ownerId, left.getUuid(), right.getUuid());
        }
    }

    public static void onPlayerFired(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        tickPlayer(player);
        Vec3d lookDirection = player.getRotationVec(1.0F);
        UUID focusedTarget = resolveLookFocusedTarget(player);
        if (focusedTarget != null) {
            FOCUSED_TARGETS.put(ownerId, focusedTarget);
        } else {
            focusedTarget = FOCUSED_TARGETS.get(ownerId);
        }
        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        if (pair == null) {
            return;
        }

        ShoulderBowEntity left = resolveTracked(world, pair.leftId(), ownerId, -1);
        ShoulderBowEntity right = resolveTracked(world, pair.rightId(), ownerId, 1);
        if (left != null) {
            left.queueLookShot(lookDirection, focusedTarget);
        }
        if (right != null) {
            right.queueLookShot(lookDirection, focusedTarget);
        }
    }

    public static void setFocusedTarget(ServerPlayerEntity player, @Nullable LivingEntity target) {
        if (player == null) {
            return;
        }
        UUID ownerId = player.getUuid();
        if (target == null || !target.isAlive() || !CombatTargeting.checkFriendlyFire(target, player)) {
            FOCUSED_TARGETS.remove(ownerId);
            return;
        }
        FOCUSED_TARGETS.put(ownerId, target.getUuid());
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 20L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ShoulderBowEntity shoulderBow)) {
                continue;
            }
            UUID ownerId = shoulderBow.getOwnerUuid();
            if (ownerId == null) {
                shoulderBow.discard();
                continue;
            }
            ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerId);
            if (owner == null || !owner.isAlive() || !(owner.getMainHandStack().getItem() instanceof EchoBowItem)) {
                shoulderBow.discard();
                ACTIVE_BOWS.remove(ownerId);
                FOCUSED_TARGETS.remove(ownerId);
            }
        }
    }

    private static void validateFocusedTarget(ServerPlayerEntity player, @Nullable UUID focusedTargetId) {
        if (focusedTargetId == null) {
            return;
        }
        Entity focusedEntity = player.getServerWorld().getEntity(focusedTargetId);
        if (!(focusedEntity instanceof LivingEntity living) || !living.isAlive() || !CombatTargeting.checkFriendlyFire(living, player)) {
            FOCUSED_TARGETS.remove(player.getUuid());
        }
    }

    @Nullable
    private static UUID resolveLookFocusedTarget(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(LOOK_TARGET_DISTANCE));
        Box searchBox = player.getBoundingBox().stretch(look.multiply(LOOK_TARGET_DISTANCE)).expand(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                player.getWorld(),
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && CombatTargeting.checkFriendlyFire(living, player)
        );
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }

        HitResult blockHit = player.raycast(LOOK_TARGET_DISTANCE, 1.0F, false);
        double entityDistanceSq = start.squaredDistanceTo(entityHit.getPos());
        double blockDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : start.squaredDistanceTo(blockHit.getPos());
        if (entityDistanceSq > blockDistanceSq) {
            return null;
        }
        return living.getUuid();
    }

    private static ShoulderBowEntity resolveTracked(ServerWorld world, UUID bowId, UUID ownerId, int side) {
        if (bowId == null) {
            return null;
        }

        Entity entity = world.getEntity(bowId);
        if (!(entity instanceof ShoulderBowEntity bow)) {
            return null;
        }
        if (!ownerId.equals(bow.getOwnerUuid()) || bow.getSide() != side || !bow.isAlive()) {
            bow.discard();
            return null;
        }
        return bow;
    }

    private static void discardTracked(ServerWorld world, UUID ownerId) {
        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        if (pair == null) {
            return;
        }
        discardByUuid(world, pair.leftId());
        discardByUuid(world, pair.rightId());
    }

    private static void discardByUuid(ServerWorld world, UUID entityId) {
        if (entityId == null) {
            return;
        }
        Entity entity = world.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void cleanupOwnerOrphans(ServerWorld world, UUID ownerId, UUID leftId, UUID rightId) {
        Set<UUID> keep = new HashSet<>();
        keep.add(leftId);
        keep.add(rightId);
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ShoulderBowEntity bow)) {
                continue;
            }
            if (ownerId.equals(bow.getOwnerUuid()) && !keep.contains(bow.getUuid())) {
                bow.discard();
            }
        }
    }

    private record CompanionPair(UUID leftId, UUID rightId) {
    }
}
