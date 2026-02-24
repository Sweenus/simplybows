package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.EchoChaosBlackHoleVisualEntity;

public class EchoChaosBlackHoleVisualEntityRenderer extends EntityRenderer<EchoChaosBlackHoleVisualEntity> {

    private static final Identifier BLACK_TEXTURE =
            Identifier.of("simplybows", "textures/entity/echo_chaos_black_hole.png");
    private static final Identifier OUTLINE_TEXTURE =
            Identifier.of("simplybows", "textures/entity/echo_chaos_black_hole_outline.png");
    private static final Identifier DISK_TEXTURE =
            Identifier.of("simplybows", "textures/entity/echo_chaos_disk.png");

    private static final float DISK_R = 1.00f;
    private static final float DISK_G = 0.42f;
    private static final float DISK_B = 0.00f;   // #FF6B00 amber-orange
    private static final int DISK_SEGMENTS = 24;

    public EchoChaosBlackHoleVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(EchoChaosBlackHoleVisualEntity entity) {
        return BLACK_TEXTURE;
    }

    @Override
    public void render(EchoChaosBlackHoleVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = Math.max(0.0F, entity.getVisualScale());
        if (scale <= 0.01F) {
            return;
        }

        float age = entity.age + tickDelta;
        float spinA = age * 7.5F;
        float spinB = -age * 11.0F;

        matrices.push();
        matrices.translate(0.0, 0.35, 0.0);

        // Accretion disk — rendered first so cubes composite on top
        renderDisk(matrices, vertexConsumers, scale, entity.getVisualRadius(), age);

        // Outer cube
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spinA));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spinA * 0.55F));
        matrices.scale(0.72F * scale, 0.72F * scale, 0.72F * scale);
        matrices.translate(-0.5, -0.5, -0.5);
        renderBlackCube(matrices, vertexConsumers, light);
        renderOutlineCube(matrices, vertexConsumers);
        matrices.pop();

        // Inner cube (same size as outer, different rotation axes/speed)
        matrices.push();
        matrices.translate(0.0, 0.1, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spinB));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinB * 0.65F));
        matrices.scale(0.72F * scale, 0.72F * scale, 0.72F * scale);
        matrices.translate(-0.5, -0.5, -0.5);
        renderBlackCube(matrices, vertexConsumers, light);
        renderOutlineCube(matrices, vertexConsumers);
        matrices.pop();

        matrices.pop();
    }

    // --- Cube rendering ---

    private static void renderBlackCube(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BLACK_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        emitCube(vc, entry, 0, 0, 0, 1, 1, 1, light);
    }

    private static void renderOutlineCube(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(OUTLINE_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        emitCube(vc, entry, 0, 0, 0, 1, 1, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private static void emitCube(VertexConsumer vc, MatrixStack.Entry entry,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  int light) {
        // Top (+Y)
        quadDoubleSided(vc, entry, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 0, 1, 1, light, 1, 1, 1, 1, 0, 1, 0);
        // Bottom (-Y)
        quadDoubleSided(vc, entry, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, 0, 1, 1, light, 1, 1, 1, 1, 0, -1, 0);
        // North (-Z)
        quadDoubleSided(vc, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, 1, 1, light, 1, 1, 1, 1, 0, 0, -1);
        // South (+Z)
        quadDoubleSided(vc, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, 1, light, 1, 1, 1, 1, 0, 0, 1);
        // West (-X)
        quadDoubleSided(vc, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, 0, 0, 1, 1, light, 1, 1, 1, 1, -1, 0, 0);
        // East (+X)
        quadDoubleSided(vc, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 0, 0, 1, 1, light, 1, 1, 1, 1, 1, 0, 0);
    }

    // --- Accretion disk rendering ---

    private static void renderDisk(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   float alpha, float diskRadius, float age) {
        float innerEdge  = diskRadius - 0.25F;   // alpha fades in from here
        float peakRadius = diskRadius;            // peak brightness at actual pull radius
        float outerEdge  = diskRadius + 0.25F;   // alpha fades out to here

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(DISK_TEXTURE));

        matrices.push();
        float nutation = 5.0F + (float) (Math.sin(age * 0.07F) * 4.0F); // oscillates 1–9°
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 1.2F));  // slow precession
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(nutation));    // wobbling tilt
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 22.0F)); // fast self-spin
        MatrixStack.Entry entry = matrices.peek();

        float peakAlpha = 0.90F * alpha;   // fade with visualScale (encodes fade-in/out)
        float twoPi = (float) (2.0 * Math.PI);
        for (int i = 0; i < DISK_SEGMENTS; i++) {
            float a0 = i       * twoPi / DISK_SEGMENTS;
            float a1 = (i + 1) * twoPi / DISK_SEGMENTS;

            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            // Inner half of ring: innerEdge → peakRadius  (alpha 0.0 → peakAlpha)
            diskQuad(vc, entry,
                    cos0 * innerEdge,  sin0 * innerEdge,
                    cos1 * innerEdge,  sin1 * innerEdge,
                    cos1 * peakRadius, sin1 * peakRadius,
                    cos0 * peakRadius, sin0 * peakRadius,
                    0.00F, peakAlpha);

            // Outer half of ring: peakRadius → outerEdge  (alpha peakAlpha → 0.0)
            diskQuad(vc, entry,
                    cos0 * peakRadius, sin0 * peakRadius,
                    cos1 * peakRadius, sin1 * peakRadius,
                    cos1 * outerEdge,  sin1 * outerEdge,
                    cos0 * outerEdge,  sin0 * outerEdge,
                    peakAlpha, 0.00F);
        }

        matrices.pop();
    }

    private static void diskQuad(VertexConsumer vc, MatrixStack.Entry entry,
                                  float x0, float z0, float x1, float z1,
                                  float x2, float z2, float x3, float z3,
                                  float aInner, float aOuter) {
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        // Front face (+Y normal)
        vertex(vc, entry, x0, 0, z0, 0, 1, light, DISK_R, DISK_G, DISK_B, aInner, 0, 1, 0);
        vertex(vc, entry, x1, 0, z1, 1, 1, light, DISK_R, DISK_G, DISK_B, aInner, 0, 1, 0);
        vertex(vc, entry, x2, 0, z2, 1, 0, light, DISK_R, DISK_G, DISK_B, aOuter, 0, 1, 0);
        vertex(vc, entry, x3, 0, z3, 0, 0, light, DISK_R, DISK_G, DISK_B, aOuter, 0, 1, 0);
        // Back face (-Y normal), reversed winding
        vertex(vc, entry, x3, 0, z3, 0, 0, light, DISK_R, DISK_G, DISK_B, aOuter, 0, -1, 0);
        vertex(vc, entry, x2, 0, z2, 1, 0, light, DISK_R, DISK_G, DISK_B, aOuter, 0, -1, 0);
        vertex(vc, entry, x1, 0, z1, 1, 1, light, DISK_R, DISK_G, DISK_B, aInner, 0, -1, 0);
        vertex(vc, entry, x0, 0, z0, 0, 1, light, DISK_R, DISK_G, DISK_B, aInner, 0, -1, 0);
    }

    // --- Shared helpers (from BubbleChaosWaveVisualEntityRenderer pattern) ---

    private static void quadDoubleSided(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, -nx, -ny, -nz);
    }

    private static void vertex(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x, float y, float z,
            float u, float v,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vc.vertex(entry, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}
