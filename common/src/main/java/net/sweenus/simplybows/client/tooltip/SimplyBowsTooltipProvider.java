package net.sweenus.simplybows.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;
import net.sweenus.simplytooltips.api.UpgradeRow;
import net.sweenus.simplytooltips.api.UpgradeRune;
import net.sweenus.simplytooltips.api.UpgradeSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge provider that gives Simply Bows items a rich modern tooltip
 * rendered by the Simply Tooltips engine.
 *
 * <p>This class uses only the ST API types and reads bow data directly
 * from {@link BowUpgradeData} rather than parsing the vanilla tooltip lines.
 *
 * <p>Theme colors are resolved by Simply Tooltips' {@code ThemeRegistry} using
 * {@code themeKey = bowKey} (e.g. {@code "vine"}, {@code "bee"}).  Pip colors
 * (String / Frame / Rune) use the ST default theme's fixed accent colors so
 * they remain legible across all bow themes.
 */
public final class SimplyBowsTooltipProvider implements TooltipProvider {

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SimplyBowItem;
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String bowKey = getBowKey(stack);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);

        // Title
        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        // Parse ability lines from raw tooltip
        List<String> abilityLines = parseAbilityLines(rawLines);

        // Fixed accent colors — same as ST default theme, consistent across all bow themes
        TooltipTheme defaults = TooltipTheme.defaultTheme();

        // Build upgrade section from NBT data
        int maxSlots  = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxPerType = BowUpgradeData.getMaxLevelPerType();
        int usedSlots  = upgrades.stringLevel() + upgrades.frameLevel();
        int maxString  = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.frameLevel()));
        int maxFrame   = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.stringLevel()));

        RuneEtching rune = upgrades.runeEtching();
        boolean isNone = rune == RuneEtching.NONE;
        String runeName = Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
        String runeEffectKey = isNone
                ? "tooltip.simplybows.rune.none_effect"
                : getRuneEffectKey(bowKey, rune);
        String runeEffectRaw = Text.translatable(runeEffectKey).getString();
        List<String> runeEffectLines = runeEffectRaw.isBlank() ? List.of() : List.of("  " + runeEffectRaw);

        UpgradeRune upgradeRune = new UpgradeRune(runeName, isNone, defaults.runeColor(), runeEffectLines);

        List<UpgradeRow> rows = List.of(
                new UpgradeRow("◇", "String", defaults.stringColor(), upgrades.stringLevel(), maxString,
                        Text.translatable(getStringEffectKey(bowKey)).getString()),
                new UpgradeRow("◇", "Frame", defaults.frameColor(), upgrades.frameLevel(), maxFrame,
                        Text.translatable(getFrameEffectKey(bowKey)).getString())
        );

        UpgradeSection upgradeSection = new UpgradeSection(maxSlots, usedSlots, rows, upgradeRune);

        String animKeyExtra = "|s:" + upgrades.stringLevel()
                + "|f:" + upgrades.frameLevel()
                + "|r:" + rune.id();

        // themeKey = bowKey: TooltipRenderer looks up the full ThemeDefinition (colors + motif)
        // in ThemeRegistry, so vine bows get the vine theme, bee bows get bee, etc.
        return new ModernTooltipModel(
                title,
                List.of("UNIQUE", "BOW"),
                TooltipBorderStyle.DEFAULT,
                abilityLines,
                List.of(),
                List.of(),
                TooltipTheme.defaultTheme(),
                upgradeSection,
                animKeyExtra,
                bowKey,
                null  // hint=null
        );
    }

    // --- Ability section parsing ---

    private static List<String> parseAbilityLines(List<Text> rawLines) {
        if (rawLines.size() < 2) return List.of();

        // BowTooltipHelper adds: blank → abilityHeader → ability lines → blank → upgradesHeader → ...
        // We look for the ability section using known translation keys.
        String abilityHeaderText  = Text.translatable("tooltip.simplybows.section.ability").getString().trim();
        String upgradesHeaderText = Text.translatable("tooltip.simplybows.section.upgrades").getString().trim();

        List<String> result = new ArrayList<>();
        boolean inAbilitySection = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s       = rawLines.get(i).getString();
            String trimmed = s.trim();

            if (trimmed.equals(abilityHeaderText)) {
                inAbilitySection = true;
                continue;
            }
            if (trimmed.equals(upgradesHeaderText)) {
                break; // stop at upgrades section
            }
            if (inAbilitySection && !trimmed.isEmpty()) {
                result.add(s);
            }
        }

        return result;
    }

    // --- Bow key from registry ID ---

    private static String getBowKey(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null || !"simplybows".equals(id.getNamespace())) return "generic";
        // Items are registered as e.g. "vine_bow/vine_bow" — take the last path segment
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith("_bow") ? name.substring(0, name.length() - 4) : "generic";
    }

    // --- Translation key helpers ---

    private static String getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.string_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.string_effect";
            case "echo"    -> "tooltip.simplybows.bow.echo.string_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.string_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.string_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.string_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.string_effect";
            default        -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    private static String getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine"    -> "tooltip.simplybows.bow.vine.frame_effect";
            case "earth"   -> "tooltip.simplybows.bow.earth.frame_effect";
            case "echo"    -> "tooltip.simplybows.bow.echo.frame_effect";
            case "ice"     -> "tooltip.simplybows.bow.ice.frame_effect";
            case "bee"     -> "tooltip.simplybows.bow.bee.frame_effect";
            case "bubble"  -> "tooltip.simplybows.bow.bubble.frame_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.frame_effect";
            default        -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    private static String getRuneEffectKey(String bowKey, RuneEtching rune) {
        return switch (bowKey) {
            case "bee", "vine", "earth", "ice", "echo", "bubble", "blossom" ->
                "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
            default -> "tooltip.simplybows.bow.generic.rune." + rune.id();
        };
    }
}
