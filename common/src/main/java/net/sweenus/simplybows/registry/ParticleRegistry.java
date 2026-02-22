package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;

public class ParticleRegistry {

    public static final DeferredRegister<net.minecraft.particle.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> JAPANESE_WAVE =
            PARTICLE_TYPES.register("japanese_wave", () -> new SimpleParticleType(false) {});

    public static void registerParticles() {
        PARTICLE_TYPES.register();
    }
}
