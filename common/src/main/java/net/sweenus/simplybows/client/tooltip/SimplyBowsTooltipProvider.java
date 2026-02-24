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
 */
public final class SimplyBowsTooltipProvider implements TooltipProvider {

    private static final TooltipTheme DEFAULT_THEME = new TooltipTheme(
            0xFFE2A834, 0xFF8A6A1E, 0xF02E2210, 0xF0181208,
            0xFFFFF0CC, 0xFFEEEEEE, 0xFF141008, 0xFFFFD5A0,
            0xFFE6ECF5, 0xFF8A6A1E, 0xFFE2A834, 0xFF2A1E0A,
            0xFF8A6A1E, 0xFF9D62CA, 0xFF5E8ACF, 0xFFDB5E71,
            0xFFE2A834, 0xFF3D3020, 0xFFC7D2E2
    );

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SimplyBowItem;
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String bowKey = getBowKey(stack);
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        TooltipTheme theme = getTheme(bowKey);
        int borderStyle = getBorderStyle(bowKey);

        // Title
        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        // Parse ability lines from raw tooltip
        List<String> abilityLines = parseAbilityLines(rawLines);

        // Build upgrade section from NBT data
        int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxPerType = BowUpgradeData.getMaxLevelPerType();
        int usedSlots = upgrades.stringLevel() + upgrades.frameLevel();
        int maxString = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.frameLevel()));
        int maxFrame  = Math.max(0, Math.min(maxPerType, maxSlots - upgrades.stringLevel()));

        RuneEtching rune = upgrades.runeEtching();
        boolean isNone = rune == RuneEtching.NONE;
        String runeName = Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
        String runeEffectKey = isNone
                ? "tooltip.simplybows.rune.none_effect"
                : getRuneEffectKey(bowKey, rune);
        String runeEffectRaw = Text.translatable(runeEffectKey).getString();
        List<String> runeEffectLines = runeEffectRaw.isBlank() ? List.of() : List.of("  " + runeEffectRaw);

        UpgradeRune upgradeRune = new UpgradeRune(runeName, isNone, theme.runeColor(), runeEffectLines);

        List<UpgradeRow> rows = List.of(
                new UpgradeRow("◇", "String", theme.stringColor(), upgrades.stringLevel(), maxString,
                        Text.translatable(getStringEffectKey(bowKey)).getString()),
                new UpgradeRow("◇", "Frame", theme.frameColor(), upgrades.frameLevel(), maxFrame,
                        Text.translatable(getFrameEffectKey(bowKey)).getString())
        );

        UpgradeSection upgradeSection = new UpgradeSection(maxSlots, usedSlots, rows, upgradeRune);

        String animKeyExtra = "|s:" + upgrades.stringLevel()
                + "|f:" + upgrades.frameLevel()
                + "|r:" + rune.id();

        return new ModernTooltipModel(
                title,
                List.of("UNIQUE", "BOW"),
                borderStyle,
                abilityLines,
                List.of(),
                List.of(),
                theme,
                upgradeSection,
                animKeyExtra
        );
    }

    // --- Ability section parsing ---

    private static List<String> parseAbilityLines(List<Text> rawLines) {
        if (rawLines.size() < 2) return List.of();

        // BowTooltipHelper adds: blank → abilityHeader → ability lines → blank → upgradesHeader → ...
        // We look for the ability section using known translation keys.
        String abilityHeaderText = Text.translatable("tooltip.simplybows.section.ability").getString().trim();
        String upgradesHeaderText = Text.translatable("tooltip.simplybows.section.upgrades").getString().trim();

        List<String> result = new ArrayList<>();
        boolean inAbilitySection = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
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

    // --- Theme and border lookup ---

    private static TooltipTheme getTheme(String bowKey) {
        return switch (bowKey) {
            case "vine" -> withDefaultTextColors(new TooltipTheme(
                    0xFF7CCB88, 0xFF3A6A43, 0xF01D2F1F, 0xF0122016,
                    0xFFEAF9E9, 0xFFE8F3E8, 0xFF102114, 0xFFBDECBF,
                    0xFFD9EFDA, 0xFF4D8C57, 0xFF7CCB88, 0xFF173120,
                    0xFF4D8C57, 0xFF86D59A, 0xFF6FA9E6, 0xFFC67AB4,
                    0xFF8FD79B, 0xFF243629, 0xFFBBD8C0
            ));
            case "echo" -> withDefaultTextColors(new TooltipTheme(
                    0xFF8F7CFF, 0xFF4A3D8A, 0xF01D1738, 0xF0120F24,
                    0xFFF2EDFF, 0xFFECE9FF, 0xFF130F22, 0xFFC9C0FF,
                    0xFFE2DDF8, 0xFF6357B3, 0xFF8F7CFF, 0xFF201A3F,
                    0xFF6357B3, 0xFF9E8BFF, 0xFF76AAFF, 0xFFCE84E9,
                    0xFF9C90FF, 0xFF282246, 0xFFC8C2EC
            ));
            case "bee" -> withDefaultTextColors(new TooltipTheme(
                    0xFFF5C64E, 0xFF8A6B22, 0xF030280E, 0xF01C1808,
                    0xFFFFF2CF, 0xFFF7EECC, 0xFF181206, 0xFFFFE19A,
                    0xFFF2E9CF, 0xFFA7832F, 0xFFF5C64E, 0xFF2A210B,
                    0xFFA7832F, 0xFFE1B342, 0xFFC99A3A, 0xFFF08E53,
                    0xFFF5C64E, 0xFF2A2417, 0xFFD7CAA8
            ));
            case "blossom" -> withDefaultTextColors(new TooltipTheme(
                    0xFFF1A3C7, 0xFF8E4D66, 0xF0321B27, 0xF01E1118,
                    0xFFFFEEF6, 0xFFF8EAF1, 0xFF1D1117, 0xFFF7C7DB,
                    0xFFF2DEE7, 0xFFB06A8A, 0xFFF1A3C7, 0xFF351A26,
                    0xFFB06A8A, 0xFFD998C1, 0xFFB19AE8, 0xFFE37EA7,
                    0xFFEFA9CB, 0xFF33202A, 0xFFDCC2CE
            ));
            case "bubble" -> withDefaultTextColors(new TooltipTheme(
                    0xFF66D5E5, 0xFF2E7284, 0xF0132B33, 0xF00C1A20,
                    0xFFE8FAFF, 0xFFE7F4F8, 0xFF0D1A1E, 0xFFA7ECF7,
                    0xFFD8EDF2, 0xFF4697A8, 0xFF66D5E5, 0xFF13303A,
                    0xFF4697A8, 0xFF7AE1EA, 0xFF7DBBEB, 0xFF78CDE0,
                    0xFF78DCE8, 0xFF1B3138, 0xFFBCD7DD
            ));
            case "earth" -> withDefaultTextColors(new TooltipTheme(
                    0xFFD0AF7A, 0xFF7B6138, 0xF0302518, 0xF01B140C,
                    0xFFFFF1DD, 0xFFF5EDDE, 0xFF181208, 0xFFEACD9E,
                    0xFFEDDFC8, 0xFF9A7A49, 0xFFD0AF7A, 0xFF2A2114,
                    0xFF9A7A49, 0xFFC79C75, 0xFFA09CCC, 0xFFD49870,
                    0xFFD8B783, 0xFF2F261B, 0xFFD2C2AA
            ));
            case "ice" -> withDefaultTextColors(new TooltipTheme(
                    0xFF9BD9FF, 0xFF4D7894, 0xF0152733, 0xF00D1921,
                    0xFFF0FAFF, 0xFFEAF4FA, 0xFF0E181D, 0xFFC0E9FF,
                    0xFFDDEDF8, 0xFF6EA5C4, 0xFF9BD9FF, 0xFF1A2F3B,
                    0xFF6EA5C4, 0xFFA5DFFF, 0xFF8AB9F7, 0xFF9ECBEA,
                    0xFFA8E0FF, 0xFF22333E, 0xFFC8DCE9
            ));
            default -> DEFAULT_THEME;
        };
    }

    private static TooltipTheme withDefaultTextColors(TooltipTheme base) {
        return new TooltipTheme(
                base.border(),
                base.borderInner(),
                base.bgTop(),
                base.bgBottom(),
                DEFAULT_THEME.name(),
                base.badgeBg(),
                DEFAULT_THEME.badgeCutout(),
                DEFAULT_THEME.sectionHeader(),
                DEFAULT_THEME.body(),
                base.separator(),
                base.diamondFrame(),
                base.diamondFrameInner(),
                base.footerDot(),
                DEFAULT_THEME.stringColor(),
                DEFAULT_THEME.frameColor(),
                DEFAULT_THEME.runeColor(),
                base.slotFilled(),
                base.slotEmpty(),
                DEFAULT_THEME.hint()
        );
    }

    private static int getBorderStyle(String bowKey) {
        return switch (bowKey) {
            case "vine" -> TooltipBorderStyle.VINE;
            case "bee" -> TooltipBorderStyle.BEE;
            case "blossom" -> TooltipBorderStyle.BLOSSOM;
            case "bubble" -> TooltipBorderStyle.BUBBLE;
            case "earth" -> TooltipBorderStyle.EARTH;
            case "echo" -> TooltipBorderStyle.ECHO;
            case "ice" -> TooltipBorderStyle.ICE;
            default -> TooltipBorderStyle.DEFAULT;
        };
    }

    // --- Translation key helpers ---

    private static String getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine" -> "tooltip.simplybows.bow.vine.string_effect";
            case "earth" -> "tooltip.simplybows.bow.earth.string_effect";
            case "echo" -> "tooltip.simplybows.bow.echo.string_effect";
            case "ice" -> "tooltip.simplybows.bow.ice.string_effect";
            case "bee" -> "tooltip.simplybows.bow.bee.string_effect";
            case "bubble" -> "tooltip.simplybows.bow.bubble.string_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.string_effect";
            default -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    private static String getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine" -> "tooltip.simplybows.bow.vine.frame_effect";
            case "earth" -> "tooltip.simplybows.bow.earth.frame_effect";
            case "echo" -> "tooltip.simplybows.bow.echo.frame_effect";
            case "ice" -> "tooltip.simplybows.bow.ice.frame_effect";
            case "bee" -> "tooltip.simplybows.bow.bee.frame_effect";
            case "bubble" -> "tooltip.simplybows.bow.bubble.frame_effect";
            case "blossom" -> "tooltip.simplybows.bow.blossom.frame_effect";
            default -> "tooltip.simplybows.bow.generic.frame_effect";
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
