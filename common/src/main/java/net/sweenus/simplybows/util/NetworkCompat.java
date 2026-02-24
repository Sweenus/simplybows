package net.sweenus.simplybows.util;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

public final class NetworkCompat {
    private NetworkCompat() {
    }

    public static void sendVelocityUpdate(ServerPlayerEntity player) {
        Object handler = player.networkHandler;
        Packet<?> packet = new EntityVelocityUpdateS2CPacket(player);

        if (invoke(handler, "sendPacket", packet)) {
            return;
        }
        invoke(handler, "send", packet);
    }

    private static boolean invoke(Object target, String methodName, Packet<?> packet) {
        try {
            Method method = target.getClass().getMethod(methodName, Packet.class);
            method.invoke(target, packet);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
