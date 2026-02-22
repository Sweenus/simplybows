package net.sweenus.simplybows.item.unique;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.VineArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.VineFlowerFieldManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class VineBowItem extends SimplyBowItem {

    public VineBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "vine";
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        UUID ownerId = shooter != null ? shooter.getUuid() : null;
        boolean chaosFieldReady = upgrades.runeEtching() == RuneEtching.CHAOS
                && ownerId != null
                && VineFlowerFieldManager.isChaosFieldReady(serverWorld, ownerId);

        if (chaosFieldReady) {
            int durationTicks = Math.max(20,
                    SimplyBowsConfig.INSTANCE.vineBow.chaosBaseDurationTicks.get()
                            + Math.max(0, upgrades.frameLevel()) * SimplyBowsConfig.INSTANCE.vineBow.chaosDurationPerFrameTicks.get());
            int cooldownTicks = Math.max(20, SimplyBowsConfig.INSTANCE.vineBow.chaosCooldownTicks.get());
            simplybows$startAbilityItemCooldown(stack, serverWorld, durationTicks + cooldownTicks);
        }

        float speed = (float) (f * SimplyBowsConfig.INSTANCE.vineBow.arrowSpeedMultiplier.get() * (1.0 + upgrades.stringLevel() * 0.05));
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, speed, SimplyBowsConfig.INSTANCE.vineBow.arrowDivergence.get(), critical, target);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        if (simplybows$isForcingVanillaArrow()) {
            return super.createArrowEntity(world, shooter, weaponStack, arrowStack, critical);
        }

        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        BowUpgradeData upgrades = BowUpgradeData.from(weaponStack);
        VineArrowEntity arrowEntity = new VineArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.vineBow.baseDamage.get() * upgrades.damageMultiplier());
        //arrowEntity.setPunch(upgrades.bonusKnockback());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }
}
