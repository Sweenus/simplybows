package net.sweenus.simplybows.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    public abstract ServerWorld getServerWorld();


    @Inject(at = @At("HEAD"), method = "tick")
    public void simplybows$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {


            IceBowItem.passiveParticles(serverPlayer, player, getServerWorld());
            EchoShoulderBowManager.tickPlayer(serverPlayer);

        }
    }




}
