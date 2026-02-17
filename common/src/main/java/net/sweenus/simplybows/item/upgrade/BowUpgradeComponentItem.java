package net.sweenus.simplybows.item.upgrade;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;

public class BowUpgradeComponentItem extends Item {

    private final UpgradeKind upgradeKind;
    private final RuneEtching runeEtching;

    public BowUpgradeComponentItem(Settings settings, UpgradeKind upgradeKind, RuneEtching runeEtching) {
        super(settings);
        this.upgradeKind = upgradeKind;
        this.runeEtching = runeEtching == null ? RuneEtching.NONE : runeEtching;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack componentStack = user.getStackInHand(hand);
        ItemStack bowStack = resolveBowTarget(user, hand);
        if (bowStack.isEmpty()) {
            return TypedActionResult.pass(componentStack);
        }

        BowUpgradeData current = BowUpgradeData.from(bowStack);
        BowUpgradeData updated = this.applyTo(current);

        if (current.equals(updated)) {
            if (!world.isClient()) {
                user.sendMessage(Text.literal("Bow is already at max for that upgrade."), true);
            }
            return TypedActionResult.fail(componentStack);
        }

        updated.write(bowStack);
        if (!user.getAbilities().creativeMode) {
            componentStack.decrement(1);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.0F);
        if (!world.isClient()) {
            user.sendMessage(Text.literal("Applied bow upgrade: " + this.upgradeKind.displayName(this.runeEtching)), true);
        }
        return TypedActionResult.success(componentStack, world.isClient());
    }

    public BowUpgradeData applyTo(BowUpgradeData current) {
        return switch (this.upgradeKind) {
            case ENCHANTED_STRING -> current.withIncreasedString();
            case REINFORCED_FRAME -> current.withIncreasedFrame();
            case RUNE_ETCHING -> current.withRune(this.runeEtching);
        };
    }

    public int getAnvilCost(BowUpgradeData before, BowUpgradeData after) {
        if (before.equals(after)) {
            return 0;
        }
        return switch (this.upgradeKind) {
            case ENCHANTED_STRING -> 2 + after.stringLevel();
            case REINFORCED_FRAME -> 2 + after.frameLevel();
            case RUNE_ETCHING -> 5;
        };
    }

    private static ItemStack resolveBowTarget(PlayerEntity user, Hand usedHand) {
        ItemStack main = user.getMainHandStack();
        ItemStack off = user.getOffHandStack();
        if (usedHand == Hand.MAIN_HAND) {
            if (off.getItem() instanceof SimplyBowItem) {
                return off;
            }
            if (main.getItem() instanceof SimplyBowItem) {
                return main;
            }
            return ItemStack.EMPTY;
        }

        if (main.getItem() instanceof SimplyBowItem) {
            return main;
        }
        if (off.getItem() instanceof SimplyBowItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    public enum UpgradeKind {
        ENCHANTED_STRING,
        REINFORCED_FRAME,
        RUNE_ETCHING;

        String displayName(RuneEtching runeEtching) {
            return switch (this) {
                case ENCHANTED_STRING -> "Enchanted Bow-String";
                case REINFORCED_FRAME -> "Reinforced Bow-Frame";
                case RUNE_ETCHING -> "Rune Etching (" + runeEtching.id() + ")";
            };
        }
    }
}
