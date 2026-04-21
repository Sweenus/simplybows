package net.sweenus.simplybows.upgrade;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.sweenus.simplybows.config.SimplyBowsConfig;

public record BowUpgradeData(int stringLevel, int frameLevel, RuneEtching runeEtching) {

    private static final String ROOT_KEY = "simplybows_upgrades";
    private static final String STRING_KEY = "enchanted_string";
    private static final String FRAME_KEY = "reinforced_frame";
    private static final String RUNE_KEY = "rune";
    private static int maxLevelPerType() { return SimplyBowsConfig.INSTANCE.upgrades.maxLevelPerType.get(); }
    private static int maxTotalUpgradeSlots() { return SimplyBowsConfig.INSTANCE.upgrades.maxTotalSlots.get(); }
    private static double sizeMultiplierPerString() { return SimplyBowsConfig.INSTANCE.upgrades.sizeMultiplierPerString.get(); }
    private static double damageMultiplierPerFrame() { return SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get(); }

    public static BowUpgradeData none() {
        return new BowUpgradeData(0, 0, RuneEtching.NONE);
    }

    public static BowUpgradeData from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return none();
        }
        NbtCompound root = stack.getNbt();
        if (root == null) {
            return none();
        }
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
        NbtCompound upgrades = new NbtCompound();
        upgrades.putInt(STRING_KEY, clampLevel(this.stringLevel));
        upgrades.putInt(FRAME_KEY, clampLevel(this.frameLevel));
        upgrades.putString(RUNE_KEY, this.runeEtching.id());
        stack.getOrCreateNbt().put(ROOT_KEY, upgrades);
    }

    public BowUpgradeData withIncreasedString() {
        if (this.stringLevel >= maxLevelPerType() || this.stringLevel + this.frameLevel >= maxTotalUpgradeSlots()) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel + 1, this.frameLevel, this.runeEtching);
    }

    public BowUpgradeData withIncreasedFrame() {
        if (this.frameLevel >= maxLevelPerType() || this.stringLevel + this.frameLevel >= maxTotalUpgradeSlots()) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel, this.frameLevel + 1, this.runeEtching);
    }

    public BowUpgradeData withRune(RuneEtching rune) {
        return new BowUpgradeData(this.stringLevel, this.frameLevel, rune == null ? RuneEtching.NONE : rune);
    }

    public double sizeMultiplier() {
        return 1.0 + this.stringLevel * sizeMultiplierPerString();
    }

    public double damageMultiplier() {
        return 1.0 + this.frameLevel * damageMultiplierPerFrame();
    }

    public int bonusKnockback() {
        return this.frameLevel;
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(maxLevelPerType(), level));
    }

    public static int getMaxLevelPerType() {
        return maxLevelPerType();
    }

    public static int getMaxTotalUpgradeSlots() {
        return maxTotalUpgradeSlots();
    }
}
