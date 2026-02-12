package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.HomingArrowEntity;

public class HomingArrowEntityRenderer extends ProjectileEntityRenderer<HomingArrowEntity> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/projectiles/arrow.png");
    private static final Identifier TIPPED_TEXTURE = Identifier.ofVanilla("textures/entity/projectiles/tipped_arrow.png");
    private static final Identifier SPECTRAL_TEXTURE = Identifier.ofVanilla("textures/entity/projectiles/spectral_arrow.png");
    private int currentColor = -1;

    public HomingArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(HomingArrowEntity entity) {
        ItemStack stack = entity.getItemStack();
        if (stack == null || stack.isEmpty()) {
            stack = entity.getItemStack();
        }
        if (stack != null && stack.isOf(Items.SPECTRAL_ARROW)) {
            return SPECTRAL_TEXTURE;
        }
        if (entity.getColor() > 0) {
            return TIPPED_TEXTURE;
        }
        return TEXTURE;
    }

    @Override
    public void render(HomingArrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
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
        this.currentColor = getArrowColor(entity);
        super.render(entity, entity.getYaw(), tickDelta, matrices, vertexConsumers, light);
        this.currentColor = -1;
    }

    @Override
    public void vertex(MatrixStack.Entry entry, VertexConsumer consumer, int x, int y, int z, float u, float v,
                       int normalX, int normalZ, int normalY, int light) {
        consumer.vertex(entry, (float) x, (float) y, (float) z)
                .color(this.currentColor)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, (float) normalX, (float) normalY, (float) normalZ);
    }

    private int getArrowColor(HomingArrowEntity entity) {
        int color = entity.getColor();
        if (color > 0) {
            return 0xFF000000 | color;
        }
        return -1;
    }
}
