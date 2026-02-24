package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stripped DrawContextMixin — only the inventory cooldown bar overlay remains.
 * All tooltip rendering has been moved to Simply Tooltips' DrawContextMixin,
 * delegated via {@link net.sweenus.simplybows.client.tooltip.SimplyBowsTooltipProvider}.
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"), require = 0)
    private void simplybows$drawTopCooldownOverlayWithCount(
            TextRenderer textRenderer, ItemStack stack, int x, int y,
            String countOverride, CallbackInfo ci) {
        simplybows$drawTopCooldownOverlayInternal(stack, x, y);
    }

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("TAIL"), require = 0)
    private void simplybows$drawTopCooldownOverlaySimple(
            TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        simplybows$drawTopCooldownOverlayInternal(stack, x, y);
    }

    @Unique
    private void simplybows$drawTopCooldownOverlayInternal(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SimplyBowItem bowItem)) return;
        if (!bowItem.simplybows$hasAbilityCooldown()) return;
        int step = bowItem.simplybows$getAbilityCooldownBarStep();
        if (step <= 0) return;
        int color = bowItem.simplybows$getAbilityCooldownBarColor() | 0xFF000000;
        DrawContext context = (DrawContext) (Object) this;
        int barX = x + 2, barTop = y + 11, barBottom = barTop + 1;
        context.fill(RenderLayer.getGuiOverlay(), barX, barTop, barX + 13, barBottom, 0xFF000000);
        context.fill(RenderLayer.getGuiOverlay(), barX, barTop, barX + step, barBottom, color);
    }
}
