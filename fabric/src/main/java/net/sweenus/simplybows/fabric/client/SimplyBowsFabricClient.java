package net.sweenus.simplybows.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.client.particle.WaveParticle;
import net.sweenus.simplybows.client.renderer.CosmicArrowEntityRenderer;
import net.sweenus.simplybows.registry.ParticleRegistry;

public final class SimplyBowsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplyBows.Client.initializeClient();

        // Direct Fabric registration to avoid relying solely on Architectury wrapper registration.
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.JAPANESE_WAVE.get(), WaveParticle.Factory::new);
        SimplyBows.LOGGER.info("Registered Fabric particle factory: simplybows:japanese_wave");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                CosmicArrowEntityRenderer.clientTick(client.world.getTime());
            } else {
                CosmicArrowEntityRenderer.clearTrails();
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.matrixStack() != null && context.consumers() != null && context.world() != null) {
                CosmicArrowEntityRenderer.renderOrphanTrails(
                        context.world().getTime(),
                        context.camera().getPos(),
                        context.matrixStack(),
                        context.consumers()
                );
            }
        });
    }
}
