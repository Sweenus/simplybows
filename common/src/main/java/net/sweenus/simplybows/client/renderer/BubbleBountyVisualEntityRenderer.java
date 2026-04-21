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
import net.sweenus.simplybows.entity.BubbleBountyVisualEntity;

public class BubbleBountyVisualEntityRenderer extends EntityRenderer<BubbleBountyVisualEntity> {

    private static final Identifier AXOLOTL_TEXTURE = new Identifier("minecraft", "textures/entity/axolotl/axolotl_blue.png");
    private static final int SWARM_COUNT = 6;
    private final AxolotlEntityRenderer axolotlRenderer;
    private AxolotlEntity renderAxolotl;

    public BubbleBountyVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.axolotlRenderer = new AxolotlEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(BubbleBountyVisualEntity entity) {
        return AXOLOTL_TEXTURE;
    }

    @Override
    public void render(BubbleBountyVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
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
        float baseScale = MathHelper.clamp(0.35F + radius * 0.08F, 0.35F, 0.75F) * fadeScale;
        float ageTime = entity.age + tickDelta;

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

        for (int i = 0; i < SWARM_COUNT; i++) {
            float orbitSpeed = 0.07F + i * 0.01F;
            float orbitT = ageTime * orbitSpeed + (float) ((Math.PI * 2.0 / SWARM_COUNT) * i);
            float orbitRadius = radius * (0.55F + (i % 3) * 0.16F);
            float x = MathHelper.cos(orbitT) * orbitRadius;
            float z = MathHelper.sin(orbitT) * orbitRadius;
            float y = 0.3F + (columnHeight * ((i + 1.0F) / (SWARM_COUNT + 1.0F))) + MathHelper.sin(orbitT * 1.8F) * 0.18F;

            float prevOrbitT = (ageTime - 0.5F) * orbitSpeed + (float) ((Math.PI * 2.0 / SWARM_COUNT) * i);
            float prevX = MathHelper.cos(prevOrbitT) * orbitRadius;
            float prevZ = MathHelper.sin(prevOrbitT) * orbitRadius;
            float motionX = x - prevX;
            float motionZ = z - prevZ;
            float renderYaw = (float) (Math.atan2(motionX, motionZ) * (180.0F / Math.PI));

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
            matrices.scale(baseScale, baseScale, baseScale);
            this.axolotlRenderer.render(axolotl, 0.0F, tickDelta, matrices, vertexConsumers, light);
            matrices.pop();
        }
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
