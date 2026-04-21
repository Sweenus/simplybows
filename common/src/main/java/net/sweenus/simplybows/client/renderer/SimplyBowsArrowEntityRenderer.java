package net.sweenus.simplybows.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Identifier;

public class SimplyBowsArrowEntityRenderer<T extends ArrowEntity> extends ProjectileEntityRenderer<T> {

    private static final Identifier DEFAULT_ARROW_TEXTURE = new Identifier("minecraft", "textures/entity/projectiles/arrow.png");
    private final Identifier customTexture;

    public SimplyBowsArrowEntityRenderer(EntityRendererFactory.Context context, Identifier customTexture) {
        super(context);
        this.customTexture = customTexture;
    }

    @Override
    public Identifier getTexture(T entity) {
        if (this.customTexture == null) {
            return DEFAULT_ARROW_TEXTURE;
        }
        return MinecraftClient.getInstance().getResourceManager().getResource(this.customTexture).isPresent()
                ? this.customTexture
                : DEFAULT_ARROW_TEXTURE;
    }
}
