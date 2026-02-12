package net.sweenus.simplybows;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sweenus.simplybows.client.renderer.HomingArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingSpectralArrowEntityRenderer;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.registry.SimplyBowsItemProperties;

public final class SimplyBows {
    public static final String MOD_ID = "simplybows";

    public static void init() {

        ItemRegistry.ITEM.register();
        EntityRegistry.registerEntities();
        SimplyBowsItemProperties.addSimplyBowsItemProperties();

    }

    @Environment(EnvType.CLIENT)
    public static class Client {

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            EntityRendererRegistry.register(EntityRegistry.HOMING_ARROW, HomingArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.HOMING_SPECTRAL_ARROW, HomingSpectralArrowEntityRenderer::new);

        }
    }

}
