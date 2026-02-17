package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.entity.BubbleArrowEntity;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.entity.VineArrowEntity;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ENTITY_TYPE);

    // Register Homing Arrow
    public static final RegistrySupplier<EntityType<HomingArrowEntity>> HOMING_ARROW = ENTITY_TYPES.register("homing_arrow",
            () -> EntityType.Builder.<HomingArrowEntity>create(HomingArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F) // Arrow dimensions
                    .build(SimplyBows.MOD_ID + ":homing_arrow"));

    public static final RegistrySupplier<EntityType<HomingSpectralArrowEntity>> HOMING_SPECTRAL_ARROW = ENTITY_TYPES.register("homing_spectral_arrow",
            () -> EntityType.Builder.<HomingSpectralArrowEntity>create(HomingSpectralArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":homing_spectral_arrow"));

    public static final RegistrySupplier<EntityType<VineArrowEntity>> VINE_ARROW = ENTITY_TYPES.register("vine_arrow",
            () -> EntityType.Builder.<VineArrowEntity>create(VineArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":vine_arrow"));

    public static final RegistrySupplier<EntityType<BubbleArrowEntity>> BUBBLE_ARROW = ENTITY_TYPES.register("bubble_arrow",
            () -> EntityType.Builder.<BubbleArrowEntity>create(BubbleArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .build(SimplyBows.MOD_ID + ":bubble_arrow"));

    public static void registerEntities() {
        ENTITY_TYPES.register();
    }
}

