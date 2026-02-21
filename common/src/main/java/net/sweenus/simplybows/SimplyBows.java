package net.sweenus.simplybows;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.client.renderer.BeeArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BeeHiveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleBountyVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleChaosWaveVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubbleGraceVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.BubblePainArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.EarthSpikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.EchoChaosBlackHoleVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.HomingSpectralArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.IceChaosWallVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.ShoulderBowEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.registry.SimplyBowsCreativeTabRegistry;
import net.sweenus.simplybows.registry.SimplyBowsItemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimplyBows {
    public static final String MOD_ID = "simplybows";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean debugMode() {
        return SimplyBowsConfig.INSTANCE.general.debugMode.get();
    }

    public static boolean modernTooltipsEnabled() {
        return SimplyBowsConfig.INSTANCE.general.modernTooltipsEnabled.get();
    }

    public static void init() {
        // Trigger config registration and loading
        LOGGER.info("Simply Bows config loaded: {}", SimplyBowsConfig.INSTANCE.getId());

        ItemRegistry.ITEM.register();
        SimplyBowsCreativeTabRegistry.register();
        EntityRegistry.registerEntities();
        SimplyBowsItemProperties.addSimplyBowsItemProperties();

    }

    @Environment(EnvType.CLIENT)
    public static class Client {

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            EntityRendererRegistry.register(EntityRegistry.HOMING_ARROW, HomingArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.HOMING_SPECTRAL_ARROW, HomingSpectralArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VINE_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/vine_bow/vine_bow.png")));
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/bubble_bow/bubble_bow.png")));
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_PAIN_ARROW, BubblePainArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_ARROW, BeeArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOSSOM_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/blossom_bow/blossom_bow.png")));
            EntityRendererRegistry.register(EntityRegistry.EARTH_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/earth_bow/earth_bow.png")));
            EntityRendererRegistry.register(EntityRegistry.ECHO_ARROW, context ->
                    new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/echo_bow/echo_bow.png")));
            EntityRendererRegistry.register(EntityRegistry.SHOULDER_BOW, ShoulderBowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.EARTH_SPIKE_VISUAL, EarthSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ICE_CHAOS_WALL_VISUAL, IceChaosWallVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ECHO_CHAOS_BLACK_HOLE_VISUAL, EchoChaosBlackHoleVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VINE_FLOWER_VISUAL, VineFlowerVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_HIVE_VISUAL, BeeHiveVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BEE_GRACE_VISUAL, BeeGraceVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_BOUNTY_VISUAL, BubbleBountyVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_GRACE_VISUAL, BubbleGraceVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BUBBLE_CHAOS_WAVE_VISUAL, BubbleChaosWaveVisualEntityRenderer::new);

        }
    }

}
