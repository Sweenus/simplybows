package net.sweenus.simplybows.item.unique;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.world.World;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SimplyBowItem extends BowItem {

    public SimplyBowItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        int usedSlots = upgrades.stringLevel() + upgrades.frameLevel();
        int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxPerType = BowUpgradeData.getMaxLevelPerType();

        tooltip.add(Text.literal("Upgrade Slots: " + usedSlots + "/" + maxSlots).formatted(Formatting.GRAY));
        tooltip.add(Text.literal(" Per-type cap: " + maxPerType).formatted(Formatting.DARK_GRAY));

        if (upgrades.stringLevel() <= 0 && upgrades.frameLevel() <= 0 && upgrades.runeEtching() == RuneEtching.NONE) {
            tooltip.add(Text.literal("Upgrades: None").formatted(Formatting.DARK_GRAY));
            return;
        }

        tooltip.add(Text.literal("Bow Upgrades").formatted(Formatting.GOLD));
        tooltip.add(Text.literal(" Enchanted String: " + upgrades.stringLevel()).formatted(Formatting.GREEN));
        tooltip.add(Text.literal(" Reinforced Frame: " + upgrades.frameLevel()).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(" Rune Etching: " + formatRune(upgrades.runeEtching())).formatted(Formatting.LIGHT_PURPLE));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity playerEntity) {
            ItemStack itemStack = playerEntity.getProjectileType(stack);
            if (!itemStack.isEmpty()) {
                int i = this.getMaxUseTime(stack, user) - remainingUseTicks;
                float f = getPullProgress(i);
                if (!((double)f < 0.1)) {
                    List<ItemStack> list = load(stack, itemStack, playerEntity);
                    if (world instanceof ServerWorld serverWorld) {
                        if (!list.isEmpty()) {
                            //playerEntity.sendMessage(Text.literal("Checking for abilities"), false);
                            performStoppedUsing(stack, world, user, remainingUseTicks, f, playerEntity, serverWorld, list);
                        }
                    }

                    world.playSound((PlayerEntity)null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                    playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
                }
            }
        }
    }

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        projectile.setVelocity(shooter, shooter.getPitch(), shooter.getYaw() + yaw, 0.0F, speed, divergence);
    }


    private void performStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, float f, PlayerEntity playerEntity, ServerWorld serverWorld, List<ItemStack> list) {
        Item item = stack.getItem();
        //playerEntity.sendMessage(Text.literal("Object is: " + item.getClass().getName()), false);

        switch (item) {
            case IceBowItem iceBowItem -> {
                iceBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case VineBowItem vineBowItem -> {
                vineBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BubbleBowItem bubbleBowItem -> {
                bubbleBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BeeBowItem beeBowItem -> {
                beeBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BlossomBowItem blossomBowItem -> {
                blossomBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case EarthBowItem earthBowItem -> {
                earthBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case EchoBowItem echoBowItem -> {
                echoBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case CrossbowItem crossbowItem -> {
                playerEntity.sendMessage(Text.literal("You stopped using the Crossbow!"), false);
            }
            default -> {
                this.shootAll(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
        }

    }

    private void performShoot(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, float f, PlayerEntity playerEntity, ServerWorld serverWorld, List<ItemStack> list) {
        Item item = stack.getItem();
        playerEntity.sendMessage(Text.literal("Object is: " + item.getClass().getName()), false);

        switch (item) {
            case IceBowItem iceBowItem -> {
                iceBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case VineBowItem vineBowItem -> {
                vineBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BubbleBowItem bubbleBowItem -> {
                bubbleBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BeeBowItem beeBowItem -> {
                beeBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BlossomBowItem blossomBowItem -> {
                blossomBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case EarthBowItem earthBowItem -> {
                earthBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case EchoBowItem echoBowItem -> {
                echoBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case CrossbowItem crossbowItem -> {
                playerEntity.sendMessage(Text.literal("You stopped using the Crossbow!"), false);
            }
            default -> {
                this.shootAll(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
        }

    }


    protected void shootFan(SimplyBowItem bow, ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack,
                            List<ItemStack> projectiles, float speed, float divergence, boolean critical,
                            @Nullable LivingEntity target, int quantity) {
        float f = EnchantmentHelper.getProjectileSpread(world, stack, shooter, 0.0F);
        float g = projectiles.size() == 1 ? 0.0F : 2.0F * f / (float) (projectiles.size() - 1);
        float h = (float)((projectiles.size() - 1) % 2) * g / 2.0F;
        float i = 1.0F;

        if (shooter instanceof ServerPlayerEntity serverPlayerEntity) {
            // Get arrows from player inventory
            Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(serverPlayerEntity);

            int additionalArrowsNeeded = Math.max(0, quantity - 1) * projectiles.size();

            // Collect arrows from player inventory
            List<ItemStack> usableArrows = HelperMethods.collectArrows(arrowStacks, additionalArrowsNeeded);

            int arrowsConsumed = 0;

            // Iterate through the projectiles
            for (int j = 0; j < projectiles.size(); ++j) {
                for (int p = 0; p < quantity; ++p) {
                    ItemStack arrowForProjectile;
                    if (p == 0) {
                        // First shot for this projectile uses the arrow already consumed by Minecraft
                        arrowForProjectile = projectiles.get(j);
                    } else if (arrowsConsumed < additionalArrowsNeeded && !usableArrows.isEmpty()) {
                        arrowForProjectile = HelperMethods.consumeNextArrow(usableArrows);
                        if (arrowForProjectile == null || arrowForProjectile.isEmpty()) {
                            break;
                        }
                        arrowsConsumed++;
                    } else {
                        // insufficient arrows to continue
                        break;
                    }

                    // Calculate the spread
                    float k = h + i * (float) ((j + 1) / 2) * g;
                    i = -i;

                    // Create and shoot the projectile
                    ProjectileEntity projectileEntity = bow.createArrowEntity(world, shooter, stack, arrowForProjectile, critical);
                    bow.shoot(shooter, projectileEntity, j, speed, divergence, k + (p - ((float) quantity / 2)) * quantity, target);
                    world.spawnEntity(projectileEntity);

                    // Damage the bow after firing
                    stack.damage(bow.getWeaponStackDamage(arrowForProjectile), shooter, LivingEntity.getSlotForHand(hand));

                    // Stop processing if the bow breaks
                    if (stack.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    private static String formatRune(RuneEtching rune) {
        return switch (rune) {
            case PAIN -> "Pain";
            case GRACE -> "Grace";
            case BOUNTY -> "Bounty";
            default -> "None";
        };
    }


}
