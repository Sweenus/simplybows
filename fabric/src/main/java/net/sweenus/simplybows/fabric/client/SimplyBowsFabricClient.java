package net.sweenus.simplybows.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.client.particle.WaveParticle;
import net.sweenus.simplybows.registry.ParticleRegistry;

public final class SimplyBowsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplyBows.Client.initializeClient();

        // Direct Fabric registration to avoid relying solely on Architectury wrapper registration.
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.JAPANESE_WAVE.get(), WaveParticle.Factory::new);
        SimplyBows.LOGGER.info("Registered Fabric particle factory: simplybows:japanese_wave");
    }
}
