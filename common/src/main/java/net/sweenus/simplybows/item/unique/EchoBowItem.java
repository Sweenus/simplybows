package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.EchoArrowEntity;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EchoBowItem extends SimplyBowItem {

    private static final float ECHO_ARROW_SPEED_MULTIPLIER = 0.9F;
    private static final float ECHO_ARROW_DIVERGENCE = 0.8F;

    public EchoBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "echo";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * ECHO_ARROW_SPEED_MULTIPLIER, ECHO_ARROW_DIVERGENCE, critical, target);
        if (shooter instanceof ServerPlayerEntity serverPlayer) {
            EchoShoulderBowManager.onPlayerFired(serverPlayer);
        }
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        EchoArrowEntity arrowEntity = new EchoArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(2.0);
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
