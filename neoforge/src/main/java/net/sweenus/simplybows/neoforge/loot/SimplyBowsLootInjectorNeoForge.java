package net.sweenus.simplybows.neoforge.loot;

import net.minecraft.loot.LootPool;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.sweenus.simplybows.loot.SimplyBowsChestLootRules;

public final class SimplyBowsLootInjectorNeoForge {

    private SimplyBowsLootInjectorNeoForge() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SimplyBowsLootInjectorNeoForge::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        String namespace = event.getName().getNamespace();
        String path = event.getName().getPath();
        for (LootPool.Builder pool : SimplyBowsChestLootRules.createPoolsForChestTable(namespace, path)) {
            event.getTable().addPool(pool.build());
        }
    }
}
