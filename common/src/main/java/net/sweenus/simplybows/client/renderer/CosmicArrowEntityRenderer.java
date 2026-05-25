package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicArrowEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CosmicArrowEntityRenderer<T extends ArrowEntity> extends SimplyBowsArrowEntityRenderer<T> {

    private static final float NODE_RADIUS = 0.045F;
    private static final int NODE_SEGMENTS = 14;
    private static final Identifier NODE_FILL_SPRITE = Identifier.ofVanilla("block/white_concrete");

    private static final float NODE_R = 0.48F;
    private static final float NODE_G = 0.88F;
    private static final float NODE_B = 1.00F;

    private static final float LINE_R = 0.20F;
    private static final float LINE_G = 0.68F;
    private static final float LINE_B = 1.00F;

    private static final Map<ArrowEntity, ConstellationTrail> ACTIVE_TRAILS = new IdentityHashMap<>();
    private static final List<ConstellationTrail> ORPHAN_TRAILS = new ArrayList<>();

    public CosmicArrowEntityRenderer(EntityRendererFactory.Context context, Identifier customTexture) {
        super(context, customTexture);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity instanceof CosmicArrowEntity cosmicArrow) {
            updateTrail(cosmicArrow);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        if (entity instanceof CosmicArrowEntity cosmicArrow) {
            renderConstellationTrail(cosmicArrow, tickDelta, matrices, vertexConsumers);
        }
    }

    private void updateTrail(CosmicArrowEntity arrow) {
        ConstellationTrail trail = ACTIVE_TRAILS.get(arrow);
        long worldTick = arrow.getWorld().getTime();

        if (arrow.isOnGround()) {
            if (trail != null) {
                trail.prune(worldTick);
                if (trail.getPoints().isEmpty()) {
                    ACTIVE_TRAILS.remove(arrow);
                }
            }
            return;
        }

        if (trail == null) {
            SimplyBowsConfig.CosmicBowSection cfg = SimplyBowsConfig.INSTANCE.cosmicBow;
            trail = new ConstellationTrail(
                    cfg.trailDurationTicks.get(),
                    cfg.trailMaxConnectionDist.get(),
                    cfg.trailConnectionProbability.get(),
                    2
            );
            ACTIVE_TRAILS.put(arrow, trail);
        }

        if (arrow.age % trail.getSampleInterval() == 0) {
            trail.recordPoint(arrow.getPos(), worldTick);
        }
        trail.prune(worldTick);
    }

    private void renderConstellationTrail(CosmicArrowEntity arrow, float tickDelta,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        ConstellationTrail trail = ACTIVE_TRAILS.get(arrow);
        if (trail == null) return;

        long currentTick = arrow.getWorld().getTime();
        Vec3d renderPos = new Vec3d(
                lerp(tickDelta, arrow.prevX, arrow.getX()),
                lerp(tickDelta, arrow.prevY, arrow.getY()),
                lerp(tickDelta, arrow.prevZ, arrow.getZ())
        );

        renderConstellationTrail(trail, currentTick, renderPos, matrices, vertexConsumers);
    }

    public static void clientTick(long worldTick) {
        Iterator<Map.Entry<ArrowEntity, ConstellationTrail>> activeIterator = ACTIVE_TRAILS.entrySet().iterator();
        while (activeIterator.hasNext()) {
            Map.Entry<ArrowEntity, ConstellationTrail> entry = activeIterator.next();
            ArrowEntity arrow = entry.getKey();
            ConstellationTrail trail = entry.getValue();
            trail.prune(worldTick);

            if (trail.getPoints().isEmpty()) {
                activeIterator.remove();
            } else if (arrow.isRemoved()) {
                ORPHAN_TRAILS.add(trail);
                activeIterator.remove();
            }
        }

        ORPHAN_TRAILS.removeIf(trail -> {
            trail.prune(worldTick);
            return trail.getPoints().isEmpty();
        });
    }

    public static void renderOrphanTrails(long worldTick, Vec3d cameraPos,
                                          MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (ORPHAN_TRAILS.isEmpty()) {
            return;
        }

        for (ConstellationTrail trail : ORPHAN_TRAILS) {
            renderConstellationTrail(trail, worldTick, cameraPos, matrices, vertexConsumers);
        }
    }

    public static void clearTrails() {
        ACTIVE_TRAILS.clear();
        ORPHAN_TRAILS.clear();
    }

    private static void renderConstellationTrail(ConstellationTrail trail, long currentTick, Vec3d renderPos,
                                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        List<ConstellationTrail.TrailPoint> points = trail.getPoints();
        if (points.size() < 2) return;

        matrices.push();

        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (int i = 0; i < points.size() - 1; i++) {
            renderTrailConnection(trail, points, currentTick, renderPos, matrices, lineConsumer, i, i + 1);
        }

        VertexConsumer nodeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        Sprite nodeSprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(NODE_FILL_SPRITE);

        for (int i = 0; i < points.size(); i++) {
            float alpha = trail.getAlpha(i, currentTick);
            if (alpha < 0.02F) continue;

            ConstellationTrail.TrailPoint point = points.get(i);
            float x = (float) (point.pos.x - renderPos.x);
            float y = (float) (point.pos.y - renderPos.y);
            float z = (float) (point.pos.z - renderPos.z);

            float visibleAlpha = Math.min(1.0F, alpha * 1.3F);
            float radius = NODE_RADIUS * (0.85F + visibleAlpha * 0.15F);
            renderNodeDisc(matrices, nodeConsumer, nodeSprite, x, y, z, radius, visibleAlpha);
        }

        matrices.pop();
    }

    private static void renderTrailConnection(ConstellationTrail trail,
                                              List<ConstellationTrail.TrailPoint> points,
                                              long currentTick,
                                              Vec3d renderPos,
                                              MatrixStack matrices,
                                              VertexConsumer lineConsumer,
                                              int indexA,
                                              int indexB) {
        float alphaA = trail.getAlpha(indexA, currentTick);
        float alphaB = trail.getAlpha(indexB, currentTick);
        float alpha = Math.min(alphaA, alphaB);
        if (alpha < 0.02F) return;

        ConstellationTrail.TrailPoint a = points.get(indexA);
        ConstellationTrail.TrailPoint b = points.get(indexB);

        renderLine(
                matrices, lineConsumer,
                (float) (a.pos.x - renderPos.x), (float) (a.pos.y - renderPos.y), (float) (a.pos.z - renderPos.z),
                (float) (b.pos.x - renderPos.x), (float) (b.pos.y - renderPos.y), (float) (b.pos.z - renderPos.z),
                LINE_R, LINE_G, LINE_B, Math.min(1.0F, alpha * 1.15F)
        );
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }

    private static void renderNodeDisc(MatrixStack matrices, VertexConsumer consumer, Sprite sprite,
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
            emitNodeWedge(
                    consumer,
                    entry,
                    0.0F,
                    0.0F,
                    (float) Math.cos(angleA) * radius,
                    (float) Math.sin(angleA) * radius,
                    (float) Math.cos(angleMid) * radius,
                    (float) Math.sin(angleMid) * radius,
                    (float) Math.cos(angleB) * radius,
                    (float) Math.sin(angleB) * radius,
                    u,
                    v,
                    alpha
            );
        }

        matrices.pop();
    }

    private static void emitNodeWedge(VertexConsumer consumer, MatrixStack.Entry entry,
                                      float centerX, float centerY,
                                      float x1, float y1,
                                      float xMid, float yMid,
                                      float x2, float y2,
                                      float u, float v,
                                      float alpha) {
        nodeVertex(consumer, entry, centerX, centerY, u, v, alpha);
        nodeVertex(consumer, entry, x1, y1, u, v, alpha);
        nodeVertex(consumer, entry, xMid, yMid, u, v, alpha);
        nodeVertex(consumer, entry, x2, y2, u, v, alpha);
        nodeVertex(consumer, entry, centerX, centerY, u, v, alpha);
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

    private static void renderLine(MatrixStack matrices, VertexConsumer consumer,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float r, float g, float b, float alpha) {
        MatrixStack.Entry entry = matrices.peek();

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001F) return;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        consumer.vertex(entry, x1, y1, z1).color(r, g, b, alpha).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, nx, ny, nz);
        consumer.vertex(entry, x2, y2, z2).color(r, g, b, alpha).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(entry, nx, ny, nz);
    }
}
