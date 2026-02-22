package net.sweenus.simplybows.mixin;

import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplybows.world.BlossomStormManager;
import net.sweenus.simplybows.world.BeeChaosHoneyStormManager;
import net.sweenus.simplybows.world.BlossomChaosKoiManager;
import net.sweenus.simplybows.world.BeeGraceShieldManager;
import net.sweenus.simplybows.world.BeeHiveSwarmManager;
import net.sweenus.simplybows.world.BubbleColumnFieldManager;
import net.sweenus.simplybows.world.BubbleChaosWaveManager;
import net.sweenus.simplybows.world.EarthChaosSunderManager;
import net.sweenus.simplybows.world.EarthSpikeFieldManager;
import net.sweenus.simplybows.world.EchoChaosBlackHoleManager;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import net.sweenus.simplybows.world.IceChaosWallManager;
import net.sweenus.simplybows.world.VineFlowerFieldManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void simplybows$tickVineFields(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;
        VineFlowerFieldManager.tick(world);
        BubbleColumnFieldManager.tick(world);
        BubbleChaosWaveManager.tick(world);
        IceChaosWallManager.tick(world);
        BlossomStormManager.tick(world);
        EarthChaosSunderManager.tick(world);
        EarthSpikeFieldManager.tick(world);
        EchoChaosBlackHoleManager.tick(world);
        BeeChaosHoneyStormManager.tick(world);
        BlossomChaosKoiManager.tick(world);
        BeeHiveSwarmManager.tick(world);
        BeeGraceShieldManager.tick(world);
        EchoShoulderBowManager.tickWorld(world);
    }
}
