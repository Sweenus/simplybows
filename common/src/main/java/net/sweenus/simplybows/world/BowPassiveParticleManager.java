package net.sweenus.simplybows.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.CosmicStrikeVisualEntity;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.util.HelperMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BowPassiveParticleManager {

    private static final int COSMIC_PASSIVE_TRAIL_LIFETIME_TICKS = 96;
    private static final Map<ServerWorld, Map<UUID, UUID>> ACTIVE_COSMIC_PASSIVE_TRAILS = new HashMap<>();

    private BowPassiveParticleManager() {
    }

    public static void tick(ServerPlayerEntity serverPlayer, PlayerEntity player, ServerWorld world) {
        if (serverPlayer == null || player == null || world == null) {
            return;
        }

        emit(serverPlayer, player, world, ItemRegistry.ICE_BOW.get(), 8, ParticleTypes.SNOWFLAKE, 1, ParticleTypes.WHITE_ASH, 2);
        emit(serverPlayer, player, world, ItemRegistry.VINE_BOW.get(), 9, ParticleTypes.COMPOSTER, 2, ParticleTypes.FALLING_SPORE_BLOSSOM, 1);
        emit(serverPlayer, player, world, ItemRegistry.BUBBLE_BOW.get(), 8, ParticleTypes.DRIPPING_WATER, 2, ParticleTypes.SPLASH, 1);
        emit(serverPlayer, player, world, ItemRegistry.BEE_BOW.get(), 9, ParticleTypes.FALLING_HONEY, 1, ParticleTypes.WAX_ON, 1);
        emit(serverPlayer, player, world, ItemRegistry.BLOSSOM_BOW.get(), 8, ParticleTypes.CHERRY_LEAVES, 1, ParticleTypes.SPORE_BLOSSOM_AIR, 1);
        emit(
                serverPlayer,
                player,
                world,
                ItemRegistry.EARTH_BOW.get(),
                9,
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()),
                1,
                ParticleTypes.DUST_PLUME,
                1
        );
        emit(serverPlayer, player, world, ItemRegistry.ECHO_BOW.get(), 8, ParticleTypes.CRIMSON_SPORE, 2, ParticleTypes.WITCH, 1);
        emitCosmicTrail(serverPlayer, player, world);
    }

    private static void emitCosmicTrail(ServerPlayerEntity serverPlayer, PlayerEntity player, ServerWorld world) {
        if (!HelperMethods.isHoldingItem(ItemRegistry.COSMIC_BOW.get(), serverPlayer)) {
            return;
        }
        if (hasActiveCosmicPassiveTrail(world, serverPlayer.getUuid())) {
            return;
        }

        int interval = 56 + serverPlayer.getRandom().nextInt(45);
        if (serverPlayer.age % interval != 0) {
            return;
        }

        Vec3d playerPos = serverPlayer.getBoundingBox().getCenter();

        CosmicStrikeVisualEntity visual = new CosmicStrikeVisualEntity(world, playerPos, playerPos, COSMIC_PASSIVE_TRAIL_LIFETIME_TICKS);
        visual.setPointCount(2 + serverPlayer.getRandom().nextInt(7));
        visual.setPassiveMode(true);
        visual.setPassiveOwnerId(serverPlayer.getId());
        if (world.spawnEntity(visual)) {
            ACTIVE_COSMIC_PASSIVE_TRAILS
                    .computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(serverPlayer.getUuid(), visual.getUuid());
        }
    }

    private static boolean hasActiveCosmicPassiveTrail(ServerWorld world, UUID playerId) {
        Map<UUID, UUID> trails = ACTIVE_COSMIC_PASSIVE_TRAILS.get(world);
        if (trails == null) {
            return false;
        }
        UUID trailId = trails.get(playerId);
        if (trailId == null) {
            return false;
        }
        Entity trail = world.getEntity(trailId);
        if (trail != null && !trail.isRemoved()) {
            return true;
        }
        trails.remove(playerId);
        if (trails.isEmpty()) {
            ACTIVE_COSMIC_PASSIVE_TRAILS.remove(world);
        }
        return false;
    }

    private static void emit(
            ServerPlayerEntity serverPlayer,
            PlayerEntity player,
            ServerWorld world,
            Item bowItem,
            int baseInterval,
            ParticleEffect primary,
            int primaryCount,
            ParticleEffect secondary,
            int secondaryCount
    ) {
        if (!HelperMethods.isHoldingItem(bowItem, serverPlayer)) {
            return;
        }
        int interval = baseInterval + serverPlayer.getRandom().nextInt(5);
        if (serverPlayer.age % interval != 0) {
            return;
        }
        HelperMethods.spawnParticlesAtItem(world, player, bowItem, primary, primaryCount);
        HelperMethods.spawnParticlesAtItem(world, player, bowItem, secondary, secondaryCount);
    }
}
