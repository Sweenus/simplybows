package net.sweenus.simplybows.fabric.loot;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.loot.SimplyBowsChestLootRules;

public final class SimplyBowsLootInjector {

    private SimplyBowsLootInjector() {
    }

    public static void init() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            for (LootPool.Builder pool : SimplyBowsChestLootRules.createPoolsForChestTable(id.getNamespace(), id.getPath())) {
                tableBuilder.pool(pool);
            }
        });
    }

}
