package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.CosmicTetherVisualEntity;

public class CosmicTetherVisualEntityRenderer extends EntityRenderer<CosmicTetherVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = Identifier.ofVanilla("block/white_concrete");
    private static final int POINTS = 9;
    private static final float JITTER_RADIUS = 0.30F;
    private static final float DRIFT_RADIUS = 0.08F;
    private static final float DRIFT_SPEED = 0.045F;

    public CosmicTetherVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicTetherVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicTetherVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        Vec3d end = entity.getEndPos().subtract(entity.getPos());
        Vec3d direction = end.lengthSquared() > 1.0E-6 ? end.normalize() : new Vec3d(0.0, 1.0, 0.0);
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 1.0E-6) {
            side = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        side = side.normalize();
        Vec3d up = direction.crossProduct(side).normalize();

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);

        float lastX = 0.0F;
        float lastY = 0.0F;
        float lastZ = 0.0F;
        float age = entity.age + tickDelta;
        for (int i = 1; i < POINTS; i++) {
            float t = i / (float) (POINTS - 1);
            float endpointFactor = i == POINTS - 1 ? 0.0F : 1.0F;
            float phase = entity.getId() * 0.73F + i * 1.91F;
            float driftA = (float) Math.sin(age * DRIFT_SPEED + phase) * DRIFT_RADIUS * endpointFactor;
            float driftB = (float) Math.cos(age * DRIFT_SPEED * 0.83F + phase * 1.37F) * DRIFT_RADIUS * endpointFactor;
            Vec3d drift = side.multiply(driftA).add(up.multiply(driftB));
            float x = (float) end.x * t + jitter(entity.getId(), i, 0) * JITTER_RADIUS * endpointFactor + (float) drift.x;
            float y = (float) end.y * t + jitter(entity.getId(), i, 1) * JITTER_RADIUS * endpointFactor + (float) drift.y;
            float z = (float) end.z * t + jitter(entity.getId(), i, 2) * JITTER_RADIUS * endpointFactor + (float) drift.z;
            CosmicOrbitVisualEntityRenderer.renderLine(matrices, lineConsumer, lastX, lastY, lastZ, x, y, z, 0.82F);
            CosmicOrbitVisualEntityRenderer.renderNodeDisc(matrices, nodeConsumer, nodeSprite, x, y, z, 0.034F, 0.95F);
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    private static float jitter(int seed, int index, int axis) {
        int mixed = seed * 31 + index * 131 + axis * 17;
        mixed ^= mixed >>> 13;
        mixed *= 0x5bd1e995;
        mixed ^= mixed >>> 15;
        return ((mixed & 0xFF) / 255.0F) - 0.5F;
    }
}
