package net.sweenus.simplybows.loot;

import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.sweenus.simplybows.registry.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

public final class SimplyBowsChestLootRules {

    private static final float BASE_STRING_CHANCE = 0.02F;
    private static final float BASE_FRAME_CHANCE = 0.02F;
    private static final float BASE_RUNE_CHANCE = 0.003F;
    private static final float BASE_UNIQUE_BOW_CHANCE = 0.002F;

    private static final float BOOSTED_BOW_CHANCE = 0.015F;
    private static final float BOOSTED_RUNE_CHANCE_ANCIENT_CITY = 0.02F;

    private SimplyBowsChestLootRules() {
    }

    public static List<LootPool.Builder> createPoolsForChestTable(String namespace, String path) {
        if (!"minecraft".equals(namespace) || path == null || !path.startsWith("chests/")) {
            return List.of();
        }

        List<LootPool.Builder> pools = new ArrayList<>();
        addGlobalPools(pools);
        addBiomeSpecificBoosts(path, pools);
        return pools;
    }

    private static void addGlobalPools(List<LootPool.Builder> pools) {
        pools.add(singleItemChancePool(ItemRegistry.ENCHANTED_BOW_STRING.get(), BASE_STRING_CHANCE));
        pools.add(singleItemChancePool(ItemRegistry.REINFORCED_BOW_FRAME.get(), BASE_FRAME_CHANCE));
        pools.add(oneOfChancePool(
                BASE_RUNE_CHANCE,
                ItemRegistry.RUNE_ETCHING_PAIN.get(),
                ItemRegistry.RUNE_ETCHING_GRACE.get(),
                ItemRegistry.RUNE_ETCHING_BOUNTY.get()
        ));
        pools.add(oneOfChancePool(
                BASE_UNIQUE_BOW_CHANCE,
                ItemRegistry.VINE_BOW.get(),
                ItemRegistry.ICE_BOW.get(),
                ItemRegistry.BUBBLE_BOW.get(),
                ItemRegistry.BEE_BOW.get(),
                ItemRegistry.BLOSSOM_BOW.get(),
                ItemRegistry.EARTH_BOW.get(),
                ItemRegistry.ECHO_BOW.get()
        ));
    }

    private static void addBiomeSpecificBoosts(String path, List<LootPool.Builder> pools) {
        if (matchesAny(path, "chests/ocean_monument", "chests/underwater_ruin_big", "chests/underwater_ruin_small", "chests/ocean_ruin_cold", "chests/ocean_ruin_warm", "chests/shipwreck_supply", "chests/shipwreck_map", "chests/shipwreck_treasure")) {
            pools.add(singleItemChancePool(ItemRegistry.BUBBLE_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/swamp_hut", "chests/woodland_mansion")) {
            pools.add(singleItemChancePool(ItemRegistry.ECHO_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/jungle_temple")) {
            pools.add(singleItemChancePool(ItemRegistry.VINE_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (path.startsWith("chests/village/")) {
            pools.add(singleItemChancePool(ItemRegistry.BEE_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/abandoned_mineshaft")) {
            pools.add(singleItemChancePool(ItemRegistry.EARTH_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/buried_treasure")) {
            pools.add(singleItemChancePool(ItemRegistry.BLOSSOM_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/igloo_chest")) {
            pools.add(singleItemChancePool(ItemRegistry.ICE_BOW.get(), BOOSTED_BOW_CHANCE));
        }
        if (matchesAny(path, "chests/ancient_city")) {
            pools.add(oneOfChancePool(
                    BOOSTED_RUNE_CHANCE_ANCIENT_CITY,
                    ItemRegistry.RUNE_ETCHING_PAIN.get(),
                    ItemRegistry.RUNE_ETCHING_GRACE.get(),
                    ItemRegistry.RUNE_ETCHING_BOUNTY.get()
            ));
        }
    }

    private static LootPool.Builder singleItemChancePool(Item item, float chance) {
        return LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance))
                .with(ItemEntry.builder(item));
    }

    private static LootPool.Builder oneOfChancePool(float chance, Item... items) {
        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance));
        for (Item item : items) {
            pool.with(ItemEntry.builder(item));
        }
        return pool;
    }

    private static boolean matchesAny(String path, String... expectedPaths) {
        for (String expected : expectedPaths) {
            if (expected.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
