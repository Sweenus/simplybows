package net.sweenus.simplybows.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.sweenus.simplybows.SimplyBows;
import net.neoforged.fml.common.Mod;
import net.sweenus.simplybows.neoforge.loot.SimplyBowsLootInjectorNeoForge;

@Mod(SimplyBows.MOD_ID)
public final class SimplyBowsNeoForge {
    public SimplyBowsNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        SimplyBows.init();
        SimplyBowsLootInjectorNeoForge.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(SimplyBowsNeoForgeClient::onClientSetup);
        }
    }
}
