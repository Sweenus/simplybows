package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BeeEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.BeeArrowEntity;

public class BeeArrowEntityRenderer extends EntityRenderer<BeeArrowEntity> {

    private static final Identifier BEE_TEXTURE = Identifier.ofVanilla("textures/entity/bee/bee.png");
    private final BeeEntityRenderer beeRenderer;
    private BeeEntity renderBee;

    public BeeArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.beeRenderer = new BeeEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(BeeArrowEntity entity) {
        return BEE_TEXTURE;
    }

    @Override
    public void render(BeeArrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        BeeEntity bee = getOrCreateBee();
        if (bee == null) {
            return;
        }

        Vec3d velocity = entity.getVelocity();
        float renderYaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
        float renderPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        if (velocity.lengthSquared() > 1.0E-6) {
            renderYaw = (float)(Math.atan2(velocity.x, velocity.z) * (180F / Math.PI));
            renderPitch = (float)(-Math.atan2(velocity.y, velocity.horizontalLength()) * (180F / Math.PI));
        }

        bee.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), 0.0F, 0.0F);
        bee.setYaw(0.0F);
        bee.setPitch(0.0F);
        bee.prevYaw = 0.0F;
        bee.prevPitch = 0.0F;
        bee.bodyYaw = 0.0F;
        bee.prevBodyYaw = 0.0F;
        bee.setHeadYaw(0.0F);
        bee.prevHeadYaw = 0.0F;
        bee.age = entity.age;
        bee.setNoGravity(true);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderPitch));
        this.beeRenderer.render(bee, 0.0F, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    private BeeEntity getOrCreateBee() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        if (this.renderBee == null || this.renderBee.getWorld() != client.world) {
            this.renderBee = EntityType.BEE.create(client.world);
        }
        return this.renderBee;
    }
}
