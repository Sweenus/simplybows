package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.VineArrowEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VineBowItem extends SimplyBowItem {

    private static final float VINE_ARROW_SPEED_MULTIPLIER = 0.55F;
    private static final float VINE_ARROW_DIVERGENCE = 1.15F;

    public VineBowItem(Settings settings) {
        super(settings);
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * VINE_ARROW_SPEED_MULTIPLIER, VINE_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        VineArrowEntity arrowEntity = new VineArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(1.5);
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
