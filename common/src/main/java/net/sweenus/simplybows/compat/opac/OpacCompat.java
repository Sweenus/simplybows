package net.sweenus.simplybows.compat.opac;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class OpacCompat {

    private OpacCompat() {
    }

    public static boolean checkOpacFriendlyFire(LivingEntity livingEntity, PlayerEntity playerEntity) {
        if (playerEntity.getWorld().isClient() || !(livingEntity instanceof PlayerEntity)) {
            return true;
        }

        MinecraftServer server = playerEntity.getServer();
        if (server == null) {
            return true;
        }

        try {
            Object api = invokeStatic("xaero.pac.common.server.api.OpenPACServerAPI", "get", new Class<?>[]{MinecraftServer.class}, server);
            Object partyManager = invoke(api, "getPartyManager", new Class<?>[0]);

            UUID playerUUID = playerEntity.getUuid();
            UUID targetUUID = livingEntity.getUuid();

            Object playerParty = invoke(partyManager, "getPartyByMember", new Class<?>[]{UUID.class}, playerUUID);
            Object targetParty = invoke(partyManager, "getPartyByMember", new Class<?>[]{UUID.class}, targetUUID);

            if (playerParty == null) {
                return true;
            }

            if (targetParty != null) {
                UUID playerPartyId = (UUID) invoke(playerParty, "getId", new Class<?>[0]);
                UUID targetPartyId = (UUID) invoke(targetParty, "getId", new Class<?>[0]);
                if (playerPartyId != null && targetPartyId != null && !playerPartyId.equals(targetPartyId)) {
                    Boolean isAlly = (Boolean) invoke(playerParty, "isAlly", new Class<?>[]{UUID.class}, targetPartyId);
                    if (Boolean.TRUE.equals(isAlly)) {
                        return false;
                    }
                }
            }

            Object stream = invoke(playerParty, "getOnlineMemberStream", new Class<?>[0]);
            @SuppressWarnings("unchecked")
            List<ServerPlayerEntity> members = (List<ServerPlayerEntity>) invoke(stream, "toList", new Class<?>[0]);
            if (livingEntity instanceof ServerPlayerEntity serverTarget && members != null && !members.isEmpty()) {
                return !members.contains(serverTarget);
            }

            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object invoke(Object instance, String method, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method m = instance.getClass().getMethod(method, parameterTypes);
        return m.invoke(instance, args);
    }

    private static Object invokeStatic(String className, String method, Class<?>[] parameterTypes, Object... args) throws Exception {
        Class<?> clazz = Class.forName(className);
        Method m = clazz.getMethod(method, parameterTypes);
        return m.invoke(null, args);
    }
}
