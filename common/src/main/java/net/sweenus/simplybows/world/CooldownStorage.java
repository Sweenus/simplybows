package net.sweenus.simplybows.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class CooldownStorage {

    private CooldownStorage() {
    }

    public static Map<MinecraftServer, Map<UUID, Long>> newServerScopedStore() {
        return new WeakHashMap<>();
    }

    public static Map<UUID, Long> forWorld(Map<MinecraftServer, Map<UUID, Long>> cooldownsByServer, ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return new HashMap<>();
        }
        return cooldownsByServer.computeIfAbsent(server, ignored -> new HashMap<>());
    }

    public static long currentTick(ServerWorld world) {
        return world.getTime();
    }
}