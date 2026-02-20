package net.sweenus.simplybows.item.unique;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.EchoArrowEntity;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EchoBowItem extends SimplyBowItem {

    public EchoBowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected String getTooltipBowKey() {
        return "echo";
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            ItemStack offhandStack = user.getOffHandStack();
            if (isValidDrinkablePotion(offhandStack)) {
                user.setCurrentHand(Hand.OFF_HAND);
                return TypedActionResult.consume(user.getStackInHand(Hand.MAIN_HAND));
            }
        }
        return super.use(world, user, hand);
    }

    public void performStoppedUsing(ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float f, float g, boolean critical, @Nullable LivingEntity target) {
        this.shootAll(serverWorld, shooter, hand, stack, projectiles, f * SimplyBowsConfig.INSTANCE.echoBow.arrowSpeedMultiplier.get(), SimplyBowsConfig.INSTANCE.echoBow.arrowDivergence.get(), critical, target);
        if (shooter instanceof ServerPlayerEntity serverPlayer) {
            EchoShoulderBowManager.onPlayerFired(serverPlayer);
        }
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack arrowStack, boolean critical) {
        ItemStack firedArrowStack = arrowStack;
        if (firedArrowStack == null || firedArrowStack.isEmpty()) {
            firedArrowStack = new ItemStack(Items.ARROW);
        }

        EchoArrowEntity arrowEntity = new EchoArrowEntity(world, shooter, firedArrowStack, weaponStack);
        arrowEntity.setDamage(SimplyBowsConfig.INSTANCE.echoBow.baseDamage.get());
        arrowEntity.setCritical(critical);
        return arrowEntity;
    }

    private static boolean isValidDrinkablePotion(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) {
            return false;
        }
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            return false;
        }
        final boolean[] hasEffects = {false};
        potionContents.forEachEffect(effect -> hasEffects[0] = true);
        return hasEffects[0];
    }
}
