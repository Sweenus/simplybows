package net.sweenus.simplybows.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void simplybows$captureEchoGracePotion(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof PotionItem)) {
            return;
        }

        List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(stack);
        if (effects.isEmpty()) {
            return;
        }
        EchoShoulderBowManager.registerGracePotionCharge(player, effects);
    }
}
