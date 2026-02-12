package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SpectralArrowEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;

public class HomingSpectralArrowEntityRenderer extends SpectralArrowEntityRenderer {
    public HomingSpectralArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(SpectralArrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Force rotation to follow velocity each frame for accurate visuals.
        Vec3d vel = entity.getVelocity();
        if (vel.lengthSquared() > 1.0E-6) {
            float velYaw = (float)(-Math.atan2(vel.x, vel.z) * (180F / Math.PI));
            float velPitch = (float)(-Math.atan2(vel.y, vel.horizontalLength()) * (180F / Math.PI));
            entity.prevYaw = entity.getYaw();
            entity.prevPitch = entity.getPitch();
            entity.setYaw(velYaw);
            entity.setPitch(velPitch);
        }
        super.render(entity, entity.getYaw(), tickDelta, matrices, vertexConsumers, light);
    }
}
