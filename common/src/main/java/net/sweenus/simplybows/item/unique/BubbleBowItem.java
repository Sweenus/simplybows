package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.BubbleArrowEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BubbleBowItem extends SimplyBowItem {

    private static final float BUBBLE_ARROW_SPEED_MULTIPLIER = 0.95F;
    private static final float BUBBLE_ARROW_DIVERGENCE = 0.8F;

    public BubbleBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "bubble";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * BUBBLE_ARROW_SPEED_MULTIPLIER, BUBBLE_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BubbleArrowEntity arrowEntity = new BubbleArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(1.75);
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
