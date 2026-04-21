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
import net.sweenus.simplybows.entity.BeeGraceVisualEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BeeGraceVisualEntityRenderer extends EntityRenderer<BeeGraceVisualEntity> {

    private static final Identifier BEE_TEXTURE = new Identifier("minecraft", "textures/entity/bee/bee.png");
    private static final double RENDER_POSITION_SMOOTHING = 0.35;
    private static final float RENDER_YAW_SMOOTHING = 0.35F;
    private static final double MIN_MOTION_FOR_HEADING_SQ = 1.0E-5;
    private final BeeEntityRenderer beeRenderer;
    private BeeEntity renderBee;
    private final Map<Integer, Vec3d> smoothedPositions = new HashMap<>();
    private final Map<Integer, Float> smoothedYaws = new HashMap<>();

    public BeeGraceVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.beeRenderer = new BeeEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(BeeGraceVisualEntity entity) {
        return BEE_TEXTURE;
    }

    @Override
    public void render(BeeGraceVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = MathHelper.clamp(entity.getHeightScale(), 0.0F, 1.0F);
        if (scale <= 0.01F) {
            return;
        }

        BeeEntity bee = getOrCreateBee();
        if (bee == null) {
            return;
        }

        if (entity.age % 40 == 0) {
            pruneDeadEntries();
        }

        int entityId = entity.getId();
        Vec3d targetPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        Vec3d previousSmoothedPos = this.smoothedPositions.get(entityId);
        Vec3d smoothedPos = previousSmoothedPos;
        if (smoothedPos == null) {
            smoothedPos = targetPos;
        } else {
            smoothedPos = smoothedPos.lerp(targetPos, RENDER_POSITION_SMOOTHING);
        }
        this.smoothedPositions.put(entityId, smoothedPos);

        float targetYaw = entity.getYaw();
        if (previousSmoothedPos != null) {
            Vec3d movement = smoothedPos.subtract(previousSmoothedPos);
            if (movement.lengthSquared() > MIN_MOTION_FOR_HEADING_SQ) {
                targetYaw = (float) (Math.atan2(movement.x, movement.z) * (180.0F / Math.PI));
            }
        }
        float previousSmoothedYaw = this.smoothedYaws.getOrDefault(entityId, targetYaw);
        float renderYaw = MathHelper.lerpAngleDegrees(RENDER_YAW_SMOOTHING, previousSmoothedYaw, targetYaw);
        this.smoothedYaws.put(entityId, renderYaw);

        bee.refreshPositionAndAngles(smoothedPos.x, smoothedPos.y, smoothedPos.z, 0.0F, 0.0F);
        bee.prevYaw = 0.0F;
        bee.setYaw(0.0F);
        bee.prevPitch = 0.0F;
        bee.setPitch(0.0F);
        bee.prevBodyYaw = 0.0F;
        bee.bodyYaw = 0.0F;
        bee.prevHeadYaw = 0.0F;
        bee.setHeadYaw(0.0F);
        bee.setNoGravity(true);
        bee.age = entity.age;

        matrices.push();
        matrices.translate(0.0, -0.15, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.scale(0.7F * scale, 0.7F * scale, 0.7F * scale);
        this.beeRenderer.render(bee, 0.0F, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    private void pruneDeadEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.smoothedPositions.clear();
            this.smoothedYaws.clear();
            return;
        }

        Iterator<Integer> posIt = this.smoothedPositions.keySet().iterator();
        while (posIt.hasNext()) {
            int id = posIt.next();
            if (client.world.getEntityById(id) == null) {
                posIt.remove();
                this.smoothedYaws.remove(id);
            }
        }
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
