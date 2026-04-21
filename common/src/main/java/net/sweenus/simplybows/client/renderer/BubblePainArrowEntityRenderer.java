package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.AxolotlEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.BubblePainArrowEntity;

public class BubblePainArrowEntityRenderer extends EntityRenderer<BubblePainArrowEntity> {

    private static final Identifier AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_blue.png");
    private final AxolotlEntityRenderer axolotlRenderer;
    private AxolotlEntity renderAxolotl;

    public BubblePainArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.axolotlRenderer = new AxolotlEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(BubblePainArrowEntity entity) {
        return AXOLOTL_TEXTURE;
    }

    @Override
    public void render(BubblePainArrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        AxolotlEntity axolotl = getOrCreateAxolotl();
        if (axolotl == null) {
            return;
        }

        Vec3d velocity = entity.getVelocity();
        float renderYaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
        float renderPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        if (velocity.lengthSquared() > 1.0E-6) {
            renderYaw = (float) (Math.atan2(velocity.x, velocity.z) * (180F / Math.PI));
            renderPitch = (float) (-Math.atan2(velocity.y, velocity.horizontalLength()) * (180F / Math.PI));
        }

        axolotl.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), 0.0F, 0.0F);
        axolotl.setYaw(0.0F);
        axolotl.setPitch(0.0F);
        axolotl.prevYaw = 0.0F;
        axolotl.prevPitch = 0.0F;
        axolotl.bodyYaw = 0.0F;
        axolotl.prevBodyYaw = 0.0F;
        axolotl.setHeadYaw(0.0F);
        axolotl.prevHeadYaw = 0.0F;
        axolotl.age = entity.age;
        axolotl.setNoGravity(true);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderPitch));
        matrices.scale(0.6F, 0.6F, 0.6F);
        this.axolotlRenderer.render(axolotl, 0.0F, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    private AxolotlEntity getOrCreateAxolotl() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        if (this.renderAxolotl == null || this.renderAxolotl.getWorld() != client.world) {
            this.renderAxolotl = EntityType.AXOLOTL.create(client.world);
        }
        return this.renderAxolotl;
    }
}
