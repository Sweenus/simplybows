package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BeeBowItem extends SimplyBowItem {

    private static final float BEE_ARROW_SPEED_MULTIPLIER = 0.48F;
    private static final float BEE_ARROW_DIVERGENCE = 0.7F;

    public BeeBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "bee";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * BEE_ARROW_SPEED_MULTIPLIER, BEE_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        BeeArrowEntity arrowEntity = new BeeArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(2.0 * upgrades.damageMultiplier());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
