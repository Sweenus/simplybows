package net.sweenus.simplybows;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.EarthSpikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingSpectralArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.ShoulderBowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
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
            EntityRendererRegistry.register(EntityRegistry.VINE_ARROW, ArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_ARROW, ArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_ARROW, BeeArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOSSOM_ARROW, ArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.EARTH_ARROW, ArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ECHO_ARROW, ArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SHOULDER_BOW, ShoulderBowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.EARTH_SPIKE_VISUAL, EarthSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VINE_FLOWER_VISUAL, VineFlowerVisualEntityRenderer::new);

        }
    }

}
