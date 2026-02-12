package net.sweenus.simplybows.item.unique;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.entity.HomingArrowEntity;
import net.sweenus.simplybows.entity.HomingSpectralArrowEntity;
import net.sweenus.simplybows.registry.ItemRegistry;
import net.sweenus.simplybows.util.HelperMethods;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class IceBowItem extends SimplyBowItem {

    public IceBowItem(Settings settings) {
        super(settings);
    }

    public static int quantity = 3;


    public static void passiveParticles(ServerPlayerEntity serverPlayer, PlayerEntity player,  ServerWorld world) {
        int random = (int) (Math.random() * 30);
        Item item = ItemRegistry.ICE_BOW.get();
        if (HelperMethods.isHoldingItem(item, serverPlayer) && serverPlayer.age % (5 + random) == 0) {
            HelperMethods.spawnParticlesAtItem(world, player, item, ParticleTypes.SNOWFLAKE, 1);
            HelperMethods.spawnParticlesAtItem(world, player, item, ParticleTypes.WHITE_ASH, 3);
        }
    }

    public void performStoppedUsing(ServerWorld serverWorld, PlayerEntity player, Hand hand, ItemStack stack, List<ItemStack> list, float f, float g, boolean bl, @Nullable LivingEntity livingEntity) {
        player.sendMessage(Text.literal("Using Ice Bow Ability"), true);
        this.shootAll(serverWorld, player, player.getActiveHand(), stack, list, f * 1.2F, 1.0F, f == 1.0F, null);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, player, ParticleTypes.SNOWFLAKE, 6);
        HelperMethods.spawnParticlesInFrontOfPlayer(serverWorld, player, ParticleTypes.WHITE_ASH, 8);

    }

    @Override
    protected void shootAll(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float speed, float divergence, boolean critical, @Nullable LivingEntity target) {
        shootFan(this, world, shooter, hand, stack, projectiles, speed, divergence, critical, target, quantity);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        Map<ItemStack, Integer> arrowStacks = HelperMethods.findArrowStacks(user);
        ItemStack itemStack = user.getStackInHand(hand);
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
        ProjectileEntity arrowEntity;
        if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
            HomingSpectralArrowEntity spectralArrow = new HomingSpectralArrowEntity(world, shooter, arrowStack, weaponStack);
            spectralArrow.setDamage(2.0);
            spectralArrow.setCritical(critical);
            arrowEntity = spectralArrow;
        } else {
            HomingArrowEntity homingArrow = new HomingArrowEntity(world, shooter, arrowStack, weaponStack);
            homingArrow.setDamage(2.0); // Set custom damage if needed
            homingArrow.setCritical(critical);
            arrowEntity = homingArrow;
        }
        return arrowEntity;
    }



}
