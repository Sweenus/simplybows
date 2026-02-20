package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BeeBowItem extends SimplyBowItem {

    public BeeBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "bee";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        float speed = f * SimplyBowsConfig.INSTANCE.beeBow.arrowSpeedMultiplier.get();
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            int quantity = Math.max(1, upgrades.stringLevel() + 1);
            this.shootFan(this, serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.beeBow.arrowDivergence.get(), critical, target, quantity);
            return;
        }
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.beeBow.arrowDivergence.get(), critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        BeeArrowEntity arrowEntity = new BeeArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.beeBow.baseDamage.get() * upgrades.damageMultiplier());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
