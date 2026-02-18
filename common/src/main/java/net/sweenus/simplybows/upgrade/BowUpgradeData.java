package net.sweenus.simplybows.upgrade;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public record BowUpgradeData(int stringLevel, int frameLevel, RuneEtching runeEtching) {

    private static final String ROOT_KEY = "simplybows_upgrades";
    private static final String STRING_KEY = "enchanted_string";
    private static final String FRAME_KEY = "reinforced_frame";
    private static final String RUNE_KEY = "rune";
    private static final int MAX_LEVEL_PER_TYPE = 5;
    private static final int MAX_TOTAL_UPGRADE_SLOTS = 5;

    public static BowUpgradeData none() {
        return new BowUpgradeData(0, 0, RuneEtching.NONE);
    }

    public static BowUpgradeData from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return none();
        }
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return none();
        }
        NbtCompound root = customData.copyNbt();
        if (!root.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return none();
        }
        NbtCompound upgrades = root.getCompound(ROOT_KEY);
        int string = clampLevel(upgrades.getInt(STRING_KEY));
        int frame = clampLevel(upgrades.getInt(FRAME_KEY));
        RuneEtching rune = RuneEtching.fromId(upgrades.getString(RUNE_KEY));
        return new BowUpgradeData(string, frame, rune);
    }

    public void write(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NbtCompound root = getOrCreateCustomData(stack);
        NbtCompound upgrades = new NbtCompound();
        upgrades.putInt(STRING_KEY, clampLevel(this.stringLevel));
        upgrades.putInt(FRAME_KEY, clampLevel(this.frameLevel));
        upgrades.putString(RUNE_KEY, this.runeEtching.id());
        root.put(ROOT_KEY, upgrades);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
    }

    public BowUpgradeData withIncreasedString() {
        if (this.stringLevel >= MAX_LEVEL_PER_TYPE || this.stringLevel + this.frameLevel >= MAX_TOTAL_UPGRADE_SLOTS) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel + 1, this.frameLevel, this.runeEtching);
    }

    public BowUpgradeData withIncreasedFrame() {
        if (this.frameLevel >= MAX_LEVEL_PER_TYPE || this.stringLevel + this.frameLevel >= MAX_TOTAL_UPGRADE_SLOTS) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel, this.frameLevel + 1, this.runeEtching);
    }

    public BowUpgradeData withRune(RuneEtching rune) {
        return new BowUpgradeData(this.stringLevel, this.frameLevel, rune == null ? RuneEtching.NONE : rune);
    }

    public double sizeMultiplier() {
        return 1.0 + this.stringLevel * 0.2;
    }

    public double damageMultiplier() {
        return 1.0 + this.frameLevel * 0.55;
    }

    public int bonusKnockback() {
        return this.frameLevel;
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL_PER_TYPE, level));
    }

    private static NbtCompound getOrCreateCustomData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData == null ? new NbtCompound() : customData.copyNbt();
    }

    public static int getMaxLevelPerType() {
        return MAX_LEVEL_PER_TYPE;
    }

    public static int getMaxTotalUpgradeSlots() {
        return MAX_TOTAL_UPGRADE_SLOTS;
    }
}
