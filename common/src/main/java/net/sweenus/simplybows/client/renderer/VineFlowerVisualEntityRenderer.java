package net.sweenus.simplybows.client.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;

public class VineFlowerVisualEntityRenderer extends EntityRenderer<VineFlowerVisualEntity> {

    public VineFlowerVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(VineFlowerVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(VineFlowerVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = MathHelper.clamp(entity.getHeightScale(), 0.0F, 1.0F);
        if (heightScale <= 0.01F) {
            return;
        }

        BlockState state = switch (entity.getFlowerType()) {
            case 2 -> Blocks.DANDELION.getDefaultState();
            case 3 -> Blocks.POPPY.getDefaultState();
            case 1 -> Blocks.FERN.getDefaultState();
            default -> Blocks.SHORT_GRASS.getDefaultState();
        };

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.translate(-0.5, 0.0, -0.5);
        matrices.scale(0.92F, Math.max(0.05F, heightScale), 0.92F);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                state,
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }
}
