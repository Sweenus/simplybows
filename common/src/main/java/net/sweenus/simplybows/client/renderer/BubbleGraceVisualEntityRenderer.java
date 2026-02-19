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
import net.sweenus.simplybows.entity.BubbleGraceVisualEntity;

public class BubbleGraceVisualEntityRenderer extends EntityRenderer<BubbleGraceVisualEntity> {

    private static final Identifier AXOLOTL_TEXTURE = Identifier.ofVanilla("textures/entity/axolotl/axolotl_lucy.png");
    private final AxolotlEntityRenderer axolotlRenderer;
    private AxolotlEntity renderAxolotl;

    public BubbleGraceVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.axolotlRenderer = new AxolotlEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(BubbleGraceVisualEntity entity) {
        return AXOLOTL_TEXTURE;
    }

    @Override
    public void render(BubbleGraceVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        AxolotlEntity axolotl = getOrCreateAxolotl();
        if (axolotl == null) {
            return;
        }

        float fadeScale = MathHelper.clamp(entity.getHeightScale(), 0.0F, 1.0F);
        if (fadeScale <= 0.01F) {
            return;
        }

        float radius = Math.max(0.5F, entity.getRadius());
        float columnHeight = Math.max(1.0F, entity.getColumnHeight());
        float orbitRadius = radius * 0.68F;
        float orbitY = 0.25F + (columnHeight * 0.56F);
        float ageTime = entity.age + tickDelta;
        float orbitSpeed = 0.1F;
        float orbitT = ageTime * orbitSpeed;
        float x = MathHelper.cos(orbitT) * orbitRadius;
        float z = MathHelper.sin(orbitT) * orbitRadius;
        float y = orbitY + MathHelper.sin(orbitT * 1.5F) * 0.18F;

        float prevOrbitT = (ageTime - 0.5F) * orbitSpeed;
        float prevX = MathHelper.cos(prevOrbitT) * orbitRadius;
        float prevZ = MathHelper.sin(prevOrbitT) * orbitRadius;
        float motionX = x - prevX;
        float motionZ = z - prevZ;
        float renderYaw = (float) (Math.atan2(motionX, motionZ) * (180.0F / Math.PI));

        axolotl.setNoGravity(true);
        axolotl.setYaw(0.0F);
        axolotl.setPitch(0.0F);
        axolotl.prevYaw = 0.0F;
        axolotl.prevPitch = 0.0F;
        axolotl.bodyYaw = 0.0F;
        axolotl.prevBodyYaw = 0.0F;
        axolotl.setHeadYaw(0.0F);
        axolotl.prevHeadYaw = 0.0F;
        axolotl.age = entity.age;

        float scale = MathHelper.clamp(0.9F + radius * 0.16F, 0.9F, 1.35F) * fadeScale;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.scale(scale, scale, scale);
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
