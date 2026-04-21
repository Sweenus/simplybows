package net.sweenus.simplybows.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;

public final class AbilityCooldownPayload {

    public static final Identifier CHANNEL_ID =
            new Identifier(SimplyBows.MOD_ID, "ability_cooldown_sync");

    public final long endMs;
    public final int totalTicks;
    public final String bowKey;

    public AbilityCooldownPayload(long endMs, int totalTicks, String bowKey) {
        this.endMs = endMs;
        this.totalTicks = totalTicks;
        this.bowKey = bowKey;
    }

    public static void encode(AbilityCooldownPayload payload, PacketByteBuf buf) {
        buf.writeLong(payload.endMs);
        buf.writeInt(payload.totalTicks);
        buf.writeString(payload.bowKey);
    }

    public static AbilityCooldownPayload decode(PacketByteBuf buf) {
        return new AbilityCooldownPayload(buf.readLong(), buf.readInt(), buf.readString());
    }
}
