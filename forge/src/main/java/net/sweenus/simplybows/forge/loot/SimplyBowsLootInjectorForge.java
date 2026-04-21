package net.sweenus.simplybows.forge.loot;

import net.minecraft.loot.LootPool;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.sweenus.simplybows.loot.SimplyBowsChestLootRules;

public final class SimplyBowsLootInjectorForge {

    private SimplyBowsLootInjectorForge() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SimplyBowsLootInjectorForge::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        String namespace = event.getName().getNamespace();
        String path = event.getName().getPath();
        for (LootPool.Builder pool : SimplyBowsChestLootRules.createPoolsForChestTable(namespace, path)) {
            event.getTable().addPool(pool.build());
        }
    }
}
