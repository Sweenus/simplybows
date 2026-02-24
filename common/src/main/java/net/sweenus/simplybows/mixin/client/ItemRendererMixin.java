package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique
    private static final ThreadLocal<Boolean> SIMPLYBOWS_SUBSTITUTING = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ModelIdentifier BOW_INVENTORY_MODEL_ID =
            ModelIdentifier.ofInventoryVariant(Identifier.of("simplybows", "vine_bow/vine_bow"));

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simplybows$swapInventoryModel(
            ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, int overlay, BakedModel model, CallbackInfo ci) {

        if (SIMPLYBOWS_SUBSTITUTING.get()) return;
        if (!(stack.getItem() instanceof SimplyBowItem)) return;
        if (renderMode != ModelTransformationMode.GUI
                && renderMode != ModelTransformationMode.GROUND
                && renderMode != ModelTransformationMode.FIXED) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getBakedModelManager() == null) return;

        // Fetch the base vine_bow model, then re-apply its overrides using the original
        // bow stack. The 'pulling' predicate checks entity.getActiveItem() == stack, so
        // passing the real in-use stack gives us the correct pulling variant automatically.
        BakedModel vineBaseModel = client.getBakedModelManager().getModel(BOW_INVENTORY_MODEL_ID);
        if (vineBaseModel == null) return;

        ClientWorld world = client.world;
        LivingEntity entity = client.player;
        BakedModel resolvedModel = vineBaseModel.getOverrides().apply(vineBaseModel, stack, world, entity, 0);
        if (resolvedModel == null) resolvedModel = vineBaseModel;

        // Skip if the vine model resolved to the same object already in use (vine_bow itself).
        if (resolvedModel == model) return;

        SIMPLYBOWS_SUBSTITUTING.set(true);
        try {
            ((ItemRenderer) (Object) this).renderItem(
                    stack, renderMode, leftHanded, matrices, vertexConsumers, light, overlay, resolvedModel);
        } finally {
            SIMPLYBOWS_SUBSTITUTING.set(false);
        }
        ci.cancel();
    }
}
