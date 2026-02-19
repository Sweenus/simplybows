package net.sweenus.simplybows.neoforge;

import net.sweenus.simplybows.SimplyBows;
import net.neoforged.fml.common.Mod;
import net.sweenus.simplybows.neoforge.loot.SimplyBowsLootInjectorNeoForge;

@Mod(SimplyBows.MOD_ID)
public final class SimplyBowsNeoForge {
    public SimplyBowsNeoForge() {
        // Run our common setup.
        SimplyBows.init();
        SimplyBowsLootInjectorNeoForge.init();
    }
}
