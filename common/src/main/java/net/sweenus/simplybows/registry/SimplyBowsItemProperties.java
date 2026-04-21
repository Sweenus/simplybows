package net.sweenus.simplybows.registry;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.config.SimplyBowsConfig;

@Environment(EnvType.CLIENT)
public class SimplyBowsItemProperties {
    public static void addSimplyBowsItemProperties() {
        register();
    }

    public static void register() {
        makeBows(ItemRegistry.VINE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedVine.get());
        makeBows(ItemRegistry.ICE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedIce.get());
        makeBows(ItemRegistry.BUBBLE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedBubble.get());
        makeBows(ItemRegistry.BEE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedBee.get());
        makeBows(ItemRegistry.BLOSSOM_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedBlossom.get());
        makeBows(ItemRegistry.EARTH_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedEarth.get());
        makeBows(ItemRegistry.ECHO_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedEcho.get());
    }

    public static void makeBows(Item item, float drawSpeed) {
        // Register "pull" predicate
        ItemPropertiesRegistry.register(item, new Identifier("pull") , (itemStack, clientWorld, livingEntity, seed) -> {
            if (livingEntity == null) {
                return 0.0F;
            } else {
                int useTicks = itemStack.getMaxUseTime() - livingEntity.getItemUseTimeLeft();
                return livingEntity.getActiveItem() != itemStack ? 0.0F : (float) useTicks / drawSpeed;
            }
        });

        // Register "pulling" predicate
        ItemPropertiesRegistry.register(item, new Identifier("pulling"), (itemStack, clientWorld, livingEntity, seed) ->
                livingEntity != null
                        && livingEntity.isUsingItem()
                        && livingEntity.getActiveItem() == itemStack ? 1.0F : 0.0F);
    }
}
