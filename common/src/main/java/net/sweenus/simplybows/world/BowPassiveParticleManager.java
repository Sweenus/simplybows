package net.sweenus.simplybows.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.util.HelperMethods;

public final class BowPassiveParticleManager {

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
                ParticleTypes.POOF,
                1
        );
        emit(serverPlayer, player, world, ItemRegistry.ECHO_BOW.get(), 8, ParticleTypes.CRIMSON_SPORE, 2, ParticleTypes.WITCH, 1);
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

