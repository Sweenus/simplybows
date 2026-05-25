package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class CosmicOrbitVisualEntityRenderer extends EntityRenderer<CosmicOrbitVisualEntity> {

    private static final Identifier NODE_FILL_SPRITE = Identifier.ofVanilla("block/white_concrete");
    private static final float NODE_R = 0.48F;
    private static final float NODE_G = 0.88F;
    private static final float NODE_B = 1.00F;
    private static final float LINE_R = 0.20F;
    private static final float LINE_G = 0.68F;
    private static final float LINE_B = 1.00F;
    private static final int NODE_SEGMENTS = 12;
    private static final float ORBIT_RADIUS = 1.15F;
    private static final float ORBIT_SPEED = 0.22F;
    private static final float NODE_RADIUS = 0.038F;
    private static final int TRAIL_DURATION_TICKS = 9;
    private static final int TRAIL_LINE_DURATION_TICKS = 5;
    private static final float TRAIL_MAX_CONNECTION_DIST = 2.3F;
    private static final float TRAIL_CONNECTION_PROBABILITY = 0.35F;
    private static final int TRAIL_SAMPLE_INTERVAL = 1;

    private final Map<CosmicOrbitVisualEntity, ConstellationTrail> trails = new WeakHashMap<>();

    public CosmicOrbitVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CosmicOrbitVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CosmicOrbitVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float alpha = Math.max(0.0F, Math.min(1.0F, entity.getVisualScale()));
        if (alpha <= 0.02F) {
            return;
        }

        float age = entity.age + tickDelta;
        long worldTick = entity.getWorld().getTime();
        ConstellationTrail trail = updateTrail(entity, age, worldTick);

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);

        matrices.push();
        Vec3d renderPos = new Vec3d(
                lerp(tickDelta, entity.prevX, entity.getX()),
                lerp(tickDelta, entity.prevY, entity.getY()),
                lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        renderTrail(trail, worldTick, renderPos, lineConsumer, nodeConsumer, nodeSprite, matrices, alpha);
        matrices.pop();
    }

    private ConstellationTrail updateTrail(CosmicOrbitVisualEntity entity, float age, long worldTick) {
        ConstellationTrail trail = trails.computeIfAbsent(entity, ignored -> new ConstellationTrail(
                TRAIL_DURATION_TICKS,
                TRAIL_LINE_DURATION_TICKS,
                TRAIL_MAX_CONNECTION_DIST,
                TRAIL_CONNECTION_PROBABILITY,
                TRAIL_SAMPLE_INTERVAL
        ));
        if (entity.age % trail.getSampleInterval() == 0) {
            trail.recordPoint(entity.getPos().add(orbitPoint(age)), worldTick);
        }
        trail.prune(worldTick);
        return trail;
    }

    private static Vec3d orbitPoint(float age) {
        float angle = age * ORBIT_SPEED;
        return new Vec3d(
                Math.cos(angle) * ORBIT_RADIUS,
                0.75 + Math.sin(angle * 1.7F) * 0.28,
                Math.sin(angle) * ORBIT_RADIUS
        );
    }

    private static void renderTrail(ConstellationTrail trail, long worldTick, Vec3d renderPos,
                                    VertexConsumer lineConsumer, VertexConsumer nodeConsumer, Sprite nodeSprite,
                                    MatrixStack matrices, float visualAlpha) {
        var points = trail.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            float lineAlpha = Math.min(trail.getLineAlpha(i, worldTick), trail.getLineAlpha(i + 1, worldTick)) * visualAlpha;
            if (lineAlpha < 0.02F) {
                continue;
            }
            ConstellationTrail.TrailPoint a = points.get(i);
            ConstellationTrail.TrailPoint b = points.get(i + 1);
            renderLine(
                    matrices,
                    lineConsumer,
                    (float) (a.pos.x - renderPos.x), (float) (a.pos.y - renderPos.y), (float) (a.pos.z - renderPos.z),
                    (float) (b.pos.x - renderPos.x), (float) (b.pos.y - renderPos.y), (float) (b.pos.z - renderPos.z),
                    Math.min(1.0F, lineAlpha * 1.15F)
            );
        }

        for (int i = 0; i < points.size(); i++) {
            float nodeAlpha = trail.getAlpha(i, worldTick) * visualAlpha;
            if (nodeAlpha < 0.02F) {
                continue;
            }
            ConstellationTrail.TrailPoint point = points.get(i);
            float visibleAlpha = Math.min(1.0F, nodeAlpha * 1.3F);
            float radius = NODE_RADIUS * (0.25F + visibleAlpha * 0.75F);
            renderNodeDisc(
                    matrices,
                    nodeConsumer,
                    nodeSprite,
                    (float) (point.pos.x - renderPos.x),
                    (float) (point.pos.y - renderPos.y),
                    (float) (point.pos.z - renderPos.z),
                    radius,
                    visibleAlpha
            );
        }
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    static void renderNodeDisc(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
                               float x, float y, float z, float radius, float alpha) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -MinecraftClient.getInstance().gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                MinecraftClient.getInstance().gameRenderer.getCamera().getPitch()));

        MatrixStack.Entry entry = matrices.peek();
        float u = (sprite.getMinU() + sprite.getMaxU()) * 0.5F;
        float v = (sprite.getMinV() + sprite.getMaxV()) * 0.5F;
        for (int i = 0; i < NODE_SEGMENTS; i++) {
            double angleA = Math.PI * 2.0 * i / NODE_SEGMENTS;
            double angleB = Math.PI * 2.0 * (i + 1) / NODE_SEGMENTS;
            double angleMid = (angleA + angleB) * 0.5;
            emitNodeWedge(consumer, entry,
                    (float) Math.cos(angleA) * radius, (float) Math.sin(angleA) * radius,
                    (float) Math.cos(angleMid) * radius, (float) Math.sin(angleMid) * radius,
                    (float) Math.cos(angleB) * radius, (float) Math.sin(angleB) * radius,
                    u, v, alpha);
        }
        matrices.pop();
    }

    private static void emitNodeWedge(VertexConsumer consumer, MatrixStack.Entry entry,
                                      float x1, float y1, float xMid, float yMid, float x2, float y2,
                                      float u, float v, float alpha) {
        nodeVertex(consumer, entry, 0.0F, 0.0F, u, v, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, alpha);
        nodeVertex(consumer, entry, 0.0F, 0.0F, u, v, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, alpha);
    }

    private static void nodeVertex(VertexConsumer consumer, MatrixStack.Entry entry,
                                   float x, float y, float u, float v, float alpha) {
        consumer.vertex(entry, x, y, 0.0F)
                .color(NODE_R, NODE_G, NODE_B, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, 0.0F, 0.0F, 1.0F);
    }

    static void renderLine(MatrixStack matrices, VertexConsumer consumer,
                           float x1, float y1, float z1, float x2, float y2, float z2, float alpha) {
        MatrixStack.Entry entry = matrices.peek();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001F) return;
        consumer.vertex(entry, x1, y1, z1).color(LINE_R, LINE_G, LINE_B, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, dx / len, dy / len, dz / len);
        consumer.vertex(entry, x2, y2, z2).color(LINE_R, LINE_G, LINE_B, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, dx / len, dy / len, dz / len);
    }
}
