package net.sweenus.simplybows.item.unique;
import dev.architectury.networking.NetworkManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.network.AbilityCooldownPayload;
import net.sweenus.simplybows.util.BowTooltipHelper;
import net.sweenus.simplybows.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SimplyBowItem extends BowItem {
    private static final ThreadLocal<Boolean> FORCE_VANILLA_ARROW = ThreadLocal.withInitial(() -> false);
    private static final int ABILITY_COOLDOWN_BAR_COLOR = 0x24C4FF;
    public static Function<String, long[]> CLIENT_COOLDOWN_READER = null;

    public SimplyBowItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(384));
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().setStyle(BowTooltipHelper.STYLE_UNIQUE_NAME);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        // Keep vanilla item bar behavior (durability only). Ability cooldown is rendered
        // via a separate top overlay in DrawContextMixin.
        return super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        // Keep vanilla item bar behavior (durability only).
        return super.getItemBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        // Keep vanilla item bar behavior (durability only).
        return super.getItemBarColor(stack);
    }

    public boolean simplybows$hasAbilityCooldown() {
        Function<String, long[]> reader = CLIENT_COOLDOWN_READER;
        if (reader == null) {
            return false;
        }
        long[] data = reader.apply(getTooltipBowKey());
        return data != null && System.currentTimeMillis() < data[0];
    }

    public int simplybows$getAbilityCooldownBarStep() {
        Function<String, long[]> reader = CLIENT_COOLDOWN_READER;
        if (reader == null) {
            return 0;
        }
        long[] data = reader.apply(getTooltipBowKey());
        if (data == null || System.currentTimeMillis() >= data[0]) {
            return 0;
        }

        long remainingMs = Math.max(0L, data[0] - System.currentTimeMillis());
        int remaining = (int) Math.ceil(remainingMs / 50.0);
        int total = (int) data[1];
        if (remaining <= 0 || total <= 0) {
            return 0;
        }

        return Math.max(1, Math.min(13, Math.round(13.0F * ((float) remaining / (float) total))));
    }

    public int simplybows$getAbilityCooldownBarColor() {
        return ABILITY_COOLDOWN_BAR_COLOR;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        BowTooltipHelper.appendBowTooltip(getTooltipBowKey(), stack, tooltip);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity playerEntity) {
            ItemStack projectileStack = playerEntity.getProjectileType(stack);
            if (projectileStack.isEmpty() && playerEntity.getAbilities().creativeMode) {
                projectileStack = new ItemStack(Items.ARROW);
            }
            if (projectileStack.isEmpty()) {
                return;
            }
            boolean hasInfiniteAmmo = simplybows$hasInfiniteAmmo(playerEntity, stack, projectileStack);

            int i = this.getMaxUseTime(stack, user) - remainingUseTicks;
            float f = getPullProgress(i);
            if (!((double)f < 0.1)) {
                List<ItemStack> list;
                if (hasInfiniteAmmo) {
                    ItemStack virtualProjectile = projectileStack.copy();
                    virtualProjectile.setCount(1);
                    list = List.of(virtualProjectile);
                } else {
                    list = load(stack, projectileStack, playerEntity);
                }
                if (world instanceof ServerWorld serverWorld) {
                    if (!list.isEmpty()) {
                        performStoppedUsing(stack, world, user, remainingUseTicks, f, playerEntity, serverWorld, list);
                    }
                }

                world.playSound((PlayerEntity)null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
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
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, false, null);
                } else {
                    iceBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, true, null);
                }
            }
            case VineBowItem vineBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, false, null);
                } else {
                    vineBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, true, null);
                }
            }
            case BubbleBowItem bubbleBowItem -> {
                bubbleBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BeeBowItem beeBowItem -> {
                beeBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null);
            }
            case BlossomBowItem blossomBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, false, null);
                } else {
                    blossomBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, true, null);
                }
            }
            case EarthBowItem earthBowItem -> {
                if (f < 1.0F) {
                    simplybows$shootVanillaArrow(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, false, null);
                } else {
                    earthBowItem.performStoppedUsing(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, f * 3.0F, 1.0F, true, null);
                }
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
            boolean hasInfiniteAmmo = simplybows$hasInfiniteAmmo(serverPlayerEntity, stack);
            // Get arrows from player inventory
            Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(serverPlayerEntity);

            int additionalArrowsNeeded = Math.max(0, quantity - 1) * projectiles.size();

            // Collect arrows from player inventory
            List<ItemStack> usableArrows = hasInfiniteAmmo ? List.of() : HelperMethods.collectArrows(arrowStacks, additionalArrowsNeeded);

            int arrowsConsumed = 0;

            // Iterate through the projectiles
            for (int j = 0; j < projectiles.size(); ++j) {
                for (int p = 0; p < quantity; ++p) {
                    ItemStack arrowForProjectile;
                    if (p == 0) {
                        // First shot for this projectile uses the arrow already consumed by Minecraft
                        arrowForProjectile = projectiles.get(j);
                    } else if (hasInfiniteAmmo) {
                        arrowForProjectile = projectiles.get(j).copy();
                        arrowForProjectile.setCount(1);
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

    protected static boolean simplybows$isForcingVanillaArrow() {
        return FORCE_VANILLA_ARROW.get();
    }

    /**
     * Sends a cooldown sync packet to {@code player} so the client-side bar appears
     * without writing anything to the ItemStack (which would cause the equip/bob animation).
     */
    protected void simplybows$startAbilityItemCooldown(ServerPlayerEntity player, int cooldownTicks) {
        if (player == null || cooldownTicks <= 0) {
            return;
        }
        long endMs = System.currentTimeMillis() + Math.max(1, cooldownTicks) * 50L;
        simplybows$sendCooldownPacket(player, getTooltipBowKey(), endMs, cooldownTicks);
    }

    /**
     * Sends a raw cooldown packet to the given player.
     * Call this from managers (e.g. VineFlowerFieldManager) that need to extend the bar
     * beyond its initial value without going through an ItemStack.
     */
    public static void simplybows$sendCooldownPacket(ServerPlayerEntity player, String bowKey, long endMs, int totalTicks) {
        if (player == null || bowKey == null || endMs <= 0 || totalTicks <= 0) {
            return;
        }
        NetworkManager.sendToPlayer(player, new AbilityCooldownPayload(endMs, totalTicks, bowKey));
    }

    protected boolean simplybows$hasInfiniteAmmo(PlayerEntity player, ItemStack bowStack) {
        return simplybows$hasInfiniteAmmo(player, bowStack, player.getProjectileType(bowStack));
    }

    protected boolean simplybows$hasInfiniteAmmo(PlayerEntity player, ItemStack bowStack, ItemStack projectileStack) {
        if (player.getAbilities().creativeMode) {
            return true;
        }
        if (projectileStack == null || projectileStack.isEmpty() || !projectileStack.isOf(Items.ARROW)) {
            return false;
        }
        Registry<net.minecraft.enchantment.Enchantment> enchantmentRegistry =
                player.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        return enchantmentRegistry
                .getEntry(Enchantments.INFINITY)
                .map(infinity -> EnchantmentHelper.getLevel(infinity, bowStack) > 0)
                .orElse(false);
    }

    private void simplybows$shootVanillaArrow(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack,
                                              List<ItemStack> projectiles, float speed, float divergence, boolean critical,
                                              @Nullable LivingEntity target) {
        FORCE_VANILLA_ARROW.set(true);
        try {
            this.shootAll(world, shooter, hand, stack, projectiles, speed, divergence, critical, target);
        } finally {
            FORCE_VANILLA_ARROW.set(false);
        }
    }

    protected String getTooltipBowKey() {
        return "generic";
    }


}
