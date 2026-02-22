package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LargeTropicalFishEntityModel;
import net.minecraft.client.render.entity.model.SmallTropicalFishEntityModel;
import net.minecraft.client.render.entity.model.TintableCompositeModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplybows.entity.KoiFishVisualEntity;

public class KoiFishVisualEntityRenderer extends EntityRenderer<KoiFishVisualEntity> {

    private static final Identifier SMALL_BASE_TEXTURE = Identifier.ofVanilla("textures/entity/fish/tropical_a.png");
    private static final Identifier LARGE_BASE_TEXTURE = Identifier.ofVanilla("textures/entity/fish/tropical_b.png");

    private static final Identifier[] SMALL_PATTERN_TEXTURES = new Identifier[]{
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_1.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_2.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_3.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_4.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_5.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_a_pattern_6.png")
    };

    private static final Identifier[] LARGE_PATTERN_TEXTURES = new Identifier[]{
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_1.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_2.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_3.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_4.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_5.png"),
            Identifier.ofVanilla("textures/entity/fish/tropical_b_pattern_6.png")
    };

    private final TintableCompositeModel<KoiFishVisualEntity> smallModel;
    private final TintableCompositeModel<KoiFishVisualEntity> largeModel;
    private final TintableCompositeModel<KoiFishVisualEntity> smallPatternModel;
    private final TintableCompositeModel<KoiFishVisualEntity> largePatternModel;

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
        TintableCompositeModel<KoiFishVisualEntity> baseModel = large ? this.largeModel : this.smallModel;
        TintableCompositeModel<KoiFishVisualEntity> patternModel = large ? this.largePatternModel : this.smallPatternModel;

        float age = entity.age + tickDelta;
        int renderLight = light;
        int baseColor = argb(alpha, entity.getBaseColorRgb() & 0xFFFFFF);
        int patternColor = argb(alpha, 0xFFFFFF);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getSwimAngle() + 180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(4.3F * MathHelper.sin(0.6F * age)));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.45F, 0.0F);
        matrices.scale(2.0F, 2.0F, 2.0F);

        baseModel.setAngles(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        VertexConsumer baseConsumer = vertexConsumers.getBuffer(baseModel.getLayer(getTexture(entity)));
        baseModel.render(matrices, baseConsumer, renderLight, OverlayTexture.DEFAULT_UV, baseColor);

        patternModel.setAngles(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        VertexConsumer patternConsumer = vertexConsumers.getBuffer(patternModel.getLayer(getPatternTexture(large, entity.getPatternIndex())));
        patternModel.render(matrices, patternConsumer, renderLight, OverlayTexture.DEFAULT_UV, patternColor);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static Identifier getPatternTexture(boolean large, int patternIndex) {
        Identifier[] table = large ? LARGE_PATTERN_TEXTURES : SMALL_PATTERN_TEXTURES;
        int safeIndex = Math.floorMod(patternIndex, table.length);
        return table[safeIndex];
    }

    private static int argb(float alpha, int rgb) {
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
