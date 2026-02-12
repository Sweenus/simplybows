package net.sweenus.simplybows.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.sweenus.simplybows.SimplyBows;

public final class SimplyBowsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplyBows.Client.initializeClient();
    }
}
