package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LargeTropicalFishEntityModel;
import net.minecraft.client.render.entity.model.SmallTropicalFishEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.KoiFishVisualEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
public class KoiFishVisualEntityRenderer extends EntityRenderer<KoiFishVisualEntity> {

    private static final Identifier SMALL_BASE_TEXTURE = new Identifier("minecraft", "textures/entity/fish/tropical_a.png");
    private static final Identifier LARGE_BASE_TEXTURE = new Identifier("minecraft", "textures/entity/fish/tropical_b.png");

    private static final Identifier[] SMALL_PATTERN_TEXTURES = new Identifier[]{
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_1.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_2.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_3.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_4.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_5.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_a_pattern_6.png")
    };

    private static final Identifier[] LARGE_PATTERN_TEXTURES = new Identifier[]{
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_1.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_2.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_3.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_4.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_5.png"),
            new Identifier("minecraft", "textures/entity/fish/tropical_b_pattern_6.png")
    };

    private final EntityModel smallModel;
    private final EntityModel largeModel;
    private final EntityModel smallPatternModel;
    private final EntityModel largePatternModel;

    public KoiFishVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.smallModel = new SmallTropicalFishEntityModel<>(context.getPart(EntityModelLayers.TROPICAL_FISH_SMALL));
        this.largeModel = new LargeTropicalFishEntityModel<>(context.getPart(EntityModelLayers.TROPICAL_FISH_LARGE));
        this.smallPatternModel = new SmallTropicalFishEntityModel<>(context.getPart(EntityModelLayers.TROPICAL_FISH_SMALL_PATTERN));
        this.largePatternModel = new LargeTropicalFishEntityModel<>(context.getPart(EntityModelLayers.TROPICAL_FISH_LARGE_PATTERN));
    }

    @Override
    public Identifier getTexture(KoiFishVisualEntity entity) {
        return entity.isLargeVariant() ? LARGE_BASE_TEXTURE : SMALL_BASE_TEXTURE;
    }

    @Override
    public void render(KoiFishVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float alpha = MathHelper.clamp(entity.getVisualScale(), 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }

        boolean large = entity.isLargeVariant();
        EntityModel baseModel = large ? this.largeModel : this.smallModel;
        EntityModel patternModel = large ? this.largePatternModel : this.smallPatternModel;

        float age = entity.age + tickDelta;

        int baseRgb = entity.getBaseColorRgb() & 0xFFFFFF;
        float baseR = ((baseRgb >> 16) & 0xFF) / 255.0F;
        float baseG = ((baseRgb >> 8) & 0xFF) / 255.0F;
        float baseB = (baseRgb & 0xFF) / 255.0F;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getSwimAngle() + 180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(4.3F * MathHelper.sin(0.6F * age)));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.45F, 0.0F);
        matrices.scale(2.0F, 2.0F, 2.0F);

        baseModel.setAngles(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        VertexConsumer baseConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(entity)));
        baseModel.render(matrices, baseConsumer, light, OverlayTexture.DEFAULT_UV, baseR, baseG, baseB, alpha);

        patternModel.setAngles(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        VertexConsumer patternConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getPatternTexture(large, entity.getPatternIndex())));
        patternModel.render(matrices, patternConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, alpha);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static Identifier getPatternTexture(boolean large, int patternIndex) {
        Identifier[] table = large ? LARGE_PATTERN_TEXTURES : SMALL_PATTERN_TEXTURES;
        int safeIndex = Math.floorMod(patternIndex, table.length);
        return table[safeIndex];
    }
}
