package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;

public class ItemRegistry {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ITEM);

    public static final RegistrySupplier<Item> VINE_BOW = ITEM.register("vine_bow/vine_bow", () -> new SimplyBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW = ITEM.register("ice_bow/ice_bow", () -> new IceBowItem(
            new Item.Settings()
    ));


    public static void registerItems() {
        ITEM.register();
    }

}
