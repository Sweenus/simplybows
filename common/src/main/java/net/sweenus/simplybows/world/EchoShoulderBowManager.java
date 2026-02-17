package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.item.unique.EchoBowItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EchoShoulderBowManager {

    private static final Map<UUID, CompanionPair> ACTIVE_BOWS = new HashMap<>();

    private EchoShoulderBowManager() {
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        boolean holdingEchoBow = player.getMainHandStack().getItem() instanceof EchoBowItem;
        if (!holdingEchoBow) {
            discardTracked(world, ownerId);
            ACTIVE_BOWS.remove(ownerId);
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
        CompanionPair pair = ACTIVE_BOWS.get(ownerId);
        if (pair == null) {
            return;
        }

        ShoulderBowEntity left = resolveTracked(world, pair.leftId(), ownerId, -1);
        ShoulderBowEntity right = resolveTracked(world, pair.rightId(), ownerId, 1);
        if (left != null) {
            left.queueLookShot(lookDirection);
        }
        if (right != null) {
            right.queueLookShot(lookDirection);
        }
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
            }
        }
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
