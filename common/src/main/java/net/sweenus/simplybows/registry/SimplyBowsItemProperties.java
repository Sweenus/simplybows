package net.sweenus.simplybows.registry;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SimplyBowsItemProperties {
    public static void addSimplyBowsItemProperties() {
        ClientLifecycleEvent.CLIENT_SETUP.register(instance -> register());
    }

    public static void register() {
        makeBows(ItemRegistry.VINE_BOW.get(), 20f);
        makeBows(ItemRegistry.ICE_BOW.get(), 40f);
        makeBows(ItemRegistry.BUBBLE_BOW.get(), 20f);
        makeBows(ItemRegistry.BEE_BOW.get(), 20f);
        makeBows(ItemRegistry.BLOSSOM_BOW.get(), 20f);
    }

    public static void makeBows(Item item, float drawSpeed) {
        // Register "pull" predicate
        ItemPropertiesRegistry.register(item, Identifier.of("pull") , (itemStack, clientWorld, livingEntity, seed) -> {
            if (livingEntity == null) {
                return 0.0F;
            } else {
                int useTicks = itemStack.getMaxUseTime(livingEntity) - livingEntity.getItemUseTimeLeft();
                return livingEntity.getActiveItem() != itemStack ? 0.0F : (float) useTicks / drawSpeed;
            }
        });

        // Register "pulling" predicate
        ItemPropertiesRegistry.register(item, Identifier.of("pulling"), (itemStack, clientWorld, livingEntity, seed) ->
                livingEntity != null
                        && livingEntity.isUsingItem()
                        && livingEntity.getActiveItem() == itemStack ? 1.0F : 0.0F);
    }
}
