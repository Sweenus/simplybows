package net.sweenus.simplybows.neoforge;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;
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
import net.sweenus.simplybows.client.renderer.KoiFishVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.ShoulderBowEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicOrbitVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.CosmicStrikeVisualEntityRenderer;
import net.sweenus.simplybows.client.renderer.SimplyBowsArrowEntityRenderer;
import net.sweenus.simplybows.client.renderer.VineFlowerVisualEntityRenderer;
import net.sweenus.simplybows.registry.EntityRegistry;

public final class SimplyBowsNeoForgeClient {
    private SimplyBowsNeoForgeClient() {
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SimplyBows.Client.initializeClient();
            NeoForge.EVENT_BUS.addListener(SimplyBowsNeoForgeClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(SimplyBowsNeoForgeClient::onRenderLevelStage);
            SimplyBows.LOGGER.info("Registered NeoForge client setup (renderers + particles)");
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            CosmicArrowEntityRenderer.clientTick(client.world.getTime());
        } else {
            CosmicArrowEntityRenderer.clearTrails();
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        CosmicArrowEntityRenderer.renderOrphanTrails(
                client.world.getTime(),
                event.getCamera().getPos(),
                event.getPoseStack(),
                consumers
        );
        consumers.draw(RenderLayer.getLines());
        consumers.draw(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
    }

    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.HOMING_ARROW.get(), HomingArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HOMING_SPECTRAL_ARROW.get(), HomingSpectralArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/vine_bow/vine_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/bubble_bow/bubble_bow.png")));
        event.registerEntityRenderer(EntityRegistry.BUBBLE_PAIN_ARROW.get(), BubblePainArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_ARROW.get(), BeeArrowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BLOSSOM_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/blossom_bow/blossom_bow.png")));
        event.registerEntityRenderer(EntityRegistry.EARTH_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/earth_bow/earth_bow.png")));
        event.registerEntityRenderer(EntityRegistry.ECHO_ARROW.get(), context ->
                new SimplyBowsArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/echo_bow/echo_bow.png")));
        event.registerEntityRenderer(EntityRegistry.COSMIC_ARROW.get(), context ->
                new CosmicArrowEntityRenderer<>(context, Identifier.of(SimplyBows.MOD_ID, "textures/item/echo_bow/echo_bow.png")));
        event.registerEntityRenderer(EntityRegistry.COSMIC_ORBIT_VISUAL.get(), CosmicOrbitVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COSMIC_STRIKE_VISUAL.get(), CosmicStrikeVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SHOULDER_BOW.get(), ShoulderBowEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.EARTH_SPIKE_VISUAL.get(), EarthSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ICE_CHAOS_WALL_VISUAL.get(), IceChaosWallVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ECHO_CHAOS_BLACK_HOLE_VISUAL.get(), EchoChaosBlackHoleVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.VINE_FLOWER_VISUAL.get(), VineFlowerVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_HIVE_VISUAL.get(), BeeHiveVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BEE_GRACE_VISUAL.get(), BeeGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_BOUNTY_VISUAL.get(), BubbleBountyVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_GRACE_VISUAL.get(), BubbleGraceVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BUBBLE_CHAOS_WAVE_VISUAL.get(), BubbleChaosWaveVisualEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.KOI_FISH_VISUAL.get(), KoiFishVisualEntityRenderer::new);
    }
}
