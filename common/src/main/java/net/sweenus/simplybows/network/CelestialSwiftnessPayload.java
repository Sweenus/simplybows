package net.sweenus.simplybows.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;

import java.util.UUID;

public record CelestialSwiftnessPayload(UUID playerId, int stacks, int durationTicks) implements CustomPayload {

    public static final CustomPayload.Id<CelestialSwiftnessPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SimplyBows.MOD_ID, "celestial_swiftness_sync"));

    public static final PacketCodec<PacketByteBuf, CelestialSwiftnessPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeUuid(payload.playerId());
                buf.writeInt(payload.stacks());
                buf.writeInt(payload.durationTicks());
            },
            buf -> new CelestialSwiftnessPayload(buf.readUuid(), buf.readInt(), buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
