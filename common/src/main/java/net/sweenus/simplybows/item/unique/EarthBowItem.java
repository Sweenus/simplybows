package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.EarthArrowEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EarthBowItem extends SimplyBowItem {

    private static final float EARTH_ARROW_SPEED_MULTIPLIER = 0.72F;
    private static final float EARTH_ARROW_DIVERGENCE = 0.9F;

    public EarthBowItem(Settings settings) {
        super(settings);
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * EARTH_ARROW_SPEED_MULTIPLIER, EARTH_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        EarthArrowEntity arrowEntity = new EarthArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(2.0);
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
