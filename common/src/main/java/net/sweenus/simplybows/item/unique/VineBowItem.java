package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VineBowItem extends SimplyBowItem {

    private static final float VINE_ARROW_SPEED_MULTIPLIER = 0.55F;
    private static final float VINE_ARROW_DIVERGENCE = 1.15F;

    public VineBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "vine";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float speed = (float) (f * VINE_ARROW_SPEED_MULTIPLIER * (1.0 + upgrades.stringLevel() * 0.05));
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, VINE_ARROW_DIVERGENCE, critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        VineArrowEntity arrowEntity = new VineArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(1.5 * upgrades.damageMultiplier());
        //arrowEntity.setPunch(upgrades.bonusKnockback());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
