package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.EarthArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EarthBowItem extends SimplyBowItem {

    private static final float EARTH_ARROW_SPEED_MULTIPLIER = 0.72F;
    private static final float EARTH_ARROW_DIVERGENCE = 0.9F;

    public EarthBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "earth";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float speed = (float) (f * EARTH_ARROW_SPEED_MULTIPLIER * (1.0 + upgrades.stringLevel() * 0.05));
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, EARTH_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        EarthArrowEntity arrowEntity = new EarthArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(2.0 * upgrades.damageMultiplier());
        //arrowEntity.setPunch(upgrades.bonusKnockback());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
