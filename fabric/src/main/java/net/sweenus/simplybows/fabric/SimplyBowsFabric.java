package net.sweenus.simplybows.fabric;

import net.sweenus.simplybows.SimplyBows;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.sweenus.simplybows.fabric.command.SimplyBowsLootTestCommand;
import net.sweenus.simplybows.fabric.loot.SimplyBowsLootInjector;

public final class SimplyBowsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        SimplyBows.init();
        SimplyBowsLootInjector.init();
        CommandRegistrationCallback.EVENT.register(SimplyBowsLootTestCommand::register);
    }
}
