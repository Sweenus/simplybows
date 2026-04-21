package net.sweenus.simplybows.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.forge.loot.SimplyBowsLootInjectorForge;

@Mod(SimplyBows.MOD_ID)
public final class SimplyBowsForge {
    public SimplyBowsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(SimplyBows.MOD_ID, modEventBus);
        SimplyBows.init();
        SimplyBowsLootInjectorForge.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(SimplyBowsForgeClient::onClientSetup);
            modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                    SimplyBowsForgeClient.onRegisterRenderers(event));
            modEventBus.addListener((RegisterParticleProvidersEvent event) ->
                    SimplyBowsForgeClient.onRegisterParticleProviders(event));
        }
    }
}
