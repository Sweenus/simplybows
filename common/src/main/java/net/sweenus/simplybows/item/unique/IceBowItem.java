package net.sweenus.simplybows.item.unique;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IceBowItem extends SimplyBowItem {
    private static final int BASE_QUANTITY = 3;
    private static final double PAIN_TARGET_HORIZONTAL_RANGE = 48.0;
    private static final double PAIN_TARGET_VERTICAL_RANGE = 16.0;
    private static final String NBT_DAMAGE_MULTIPLIER = "simplybows_ice_damage_multiplier";
    private static final String NBT_LOCK_TARGET = "simplybows_ice_lock_target";
    private static final String NBT_SLOW_STACK = "simplybows_ice_stacking_slow";
    private static final String NBT_TARGET_UUID = "simplybows_ice_target_uuid";

    public IceBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "ice";
    }


    public static void passiveParticles(ServerPlayerEntity serverPlayer, PlayerEntity player,  ServerWorld world) {
        int random = (int) (Math.random() * 30);
        Item item = ItemRegistry.ICE_BOW.get();
        if (HelperMethods.isHoldingItem(item, serverPlayer) && serverPlayer.age % (5 + random) == 0) {
            HelperMethods.spawnParticlesAtItem(world, player, item, ParticleTypes.SNOWFLAKE, 1);
            HelperMethods.spawnParticlesAtItem(world, player, item, ParticleTypes.WHITE_ASH, 3);
        }
    }

    public void performStoppedUsing(ServerWorld serverWorld, PlayerEntity player, Hand hand, ItemStack stack, List<ItemStack> list, float f, float g, boolean bl, @Nullable LivingEntity livingEntity) {
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        int quantity = getArrowQuantity(upgrades);
        double damageMultiplier = upgrades.damageMultiplier();
        RuneEtching rune = upgrades.runeEtching();
        if (rune == RuneEtching.PAIN) {
            damageMultiplier *= 1.5;
        } else if (rune == RuneEtching.BOUNTY) {
            damageMultiplier *= 0.75;
        }

        LivingEntity painTarget = null;
        if (rune == RuneEtching.PAIN) {
            painTarget = findNearestHostile(serverWorld, player);
        }

        NbtCompound customData = getOrCreateCustomData(stack);
        customData.putDouble(NBT_DAMAGE_MULTIPLIER, damageMultiplier);
        // Only hard-lock when pain mode found a concrete target.
        // If no target is found, keep normal homing fallback behavior.
        customData.putBoolean(NBT_LOCK_TARGET, rune == RuneEtching.PAIN && painTarget != null);
        customData.putBoolean(NBT_SLOW_STACK, rune == RuneEtching.GRACE);
        if (painTarget != null) {
            customData.putUuid(NBT_TARGET_UUID, painTarget.getUuid());
        } else {
            customData.remove(NBT_TARGET_UUID);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        this.shootFan(this, serverWorld, player, player.getActiveHand(), stack, list, f * 1.2F, 1.0F, f == 1.0F, null, quantity);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, player, ParticleTypes.SNOWFLAKE, 6);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, player, ParticleTypes.WHITE_ASH, 8);

    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (simplybows$hasInfiniteAmmo(user, itemStack, user.getProjectileType(itemStack))) {
            //return super.use(world, user, hand);
        }

        int quantity = getArrowQuantity(BowUpgradeData.from(itemStack));
        Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(user);
        for (Map.Entry<ItemStack, Integer> entry : arrowStacks.entrySet()) {
            ItemStack arrowStack = entry.getKey();
            int arrowCount = entry.getValue();
            if (arrowCount > quantity - 1)
                return super.use(world, user, hand);
        }
        return TypedActionResult.fail(itemStack);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        if (simplybows$isForcingVanillaArrow()) {
            return super.createArrowEntity(world, shooter, weaponStack, arrowStack, critical);
        }

        double damageMultiplier = 1.0;
        boolean lockTarget = false;
        boolean stackSlow = false;
        UUID targetUuid = null;
        NbtComponent customData = weaponStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            damageMultiplier = nbt.getDouble(NBT_DAMAGE_MULTIPLIER);
            if (damageMultiplier <= 0.0) {
                damageMultiplier = 1.0;
            }
            lockTarget = nbt.getBoolean(NBT_LOCK_TARGET);
            stackSlow = nbt.getBoolean(NBT_SLOW_STACK);
            if (nbt.containsUuid(NBT_TARGET_UUID)) {
                targetUuid = nbt.getUuid(NBT_TARGET_UUID);
            }
        }

        ProjectileEntity arrowEntity;
        if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
            HomingSpectralArrowEntity spectralArrow = new HomingSpectralArrowEntity(world, shooter, arrowStack, weaponStack);
            spectralArrow.setDamage(2.0 * damageMultiplier);
            //spectralArrow.setPunch((int) Math.floor((damageMultiplier - 1.0) * 2.0));
            spectralArrow.setLockSingleTarget(lockTarget);
            spectralArrow.setStackingSlowness(stackSlow);
            if (targetUuid != null) {
                spectralArrow.setLockedTargetUuid(targetUuid);
            }
            spectralArrow.setCritical(critical);
            arrowEntity = spectralArrow;
        } else {
            HomingArrowEntity homingArrow = new HomingArrowEntity(world, shooter, arrowStack, weaponStack);
            homingArrow.setDamage(2.0 * damageMultiplier);
            //homingArrow.setPunch((int) Math.floor((damageMultiplier - 1.0) * 2.0));
            homingArrow.setLockSingleTarget(lockTarget);
            homingArrow.setStackingSlowness(stackSlow);
            if (targetUuid != null) {
                homingArrow.setLockedTargetUuid(targetUuid);
            }
            homingArrow.setCritical(critical);
            arrowEntity = homingArrow;
        }
        return arrowEntity;
    }

    private int getArrowQuantity(BowUpgradeData upgrades) {
        int quantity = BASE_QUANTITY + upgrades.stringLevel();
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            quantity *= 2;
        }
        return quantity;
    }

    private LivingEntity findNearestHostile(ServerWorld world, PlayerEntity player) {
        List<LivingEntity> hostiles = world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(PAIN_TARGET_HORIZONTAL_RANGE, PAIN_TARGET_VERTICAL_RANGE, PAIN_TARGET_HORIZONTAL_RANGE),
                entity -> entity.isAlive()
                        && (entity instanceof net.minecraft.entity.mob.HostileEntity || CombatTargeting.isTargetWhitelisted(entity))
        );
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity hostile : hostiles) {
            if (!CombatTargeting.checkFriendlyFire(hostile, player)) {
                continue;
            }
            double dist = hostile.squaredDistanceTo(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = hostile;
            }
        }
        return best;
    }

    private static NbtCompound getOrCreateCustomData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData == null ? new NbtCompound() : customData.copyNbt();
    }

}
