package net.sweenus.simplybows.neoforge;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.sweenus.simplybows.SimplyBows;

public final class SimplyBowsNeoForgeClient {
    private SimplyBowsNeoForgeClient() {
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimplyBows.Client.initializeClient();
            SimplyBows.LOGGER.info("Registered NeoForge client setup (renderers + particles)");
        });
    }
}