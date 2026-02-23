package net.sweenus.simplybows.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;

/**
 * S2C payload sent whenever a bow's ability cooldown bar needs to start or extend.
 * Using a CustomPayload (instead of writing to ItemStack NBT) prevents the server from
 * marking the inventory slot dirty, which avoids the equip/bob animation in first-person.
 */
public record AbilityCooldownPayload(long endMs, int totalTicks, String bowKey) implements CustomPayload {

    public static final CustomPayload.Id<AbilityCooldownPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SimplyBows.MOD_ID, "ability_cooldown_sync"));

    public static final PacketCodec<PacketByteBuf, AbilityCooldownPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeLong(payload.endMs());
                buf.writeInt(payload.totalTicks());
                buf.writeString(payload.bowKey());
            },
            buf -> new AbilityCooldownPayload(buf.readLong(), buf.readInt(), buf.readString())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
