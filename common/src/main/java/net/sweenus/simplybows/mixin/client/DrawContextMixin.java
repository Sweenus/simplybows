package net.sweenus.simplybows.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.client.tooltip.TooltipTheme;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Unique
    private static final int PADDING = 10;
    @Unique
    private static final int LINE_SPACING = 1;
    @Unique
    private static final int MAX_TEXT_WIDTH = 200;
    @Unique
    private static final int BORDER_STYLE_DEFAULT = 0;
    @Unique
    private static final int BORDER_STYLE_VINE = 1;
    @Unique
    private static final int BORDER_STYLE_BEE = 2;
    @Unique
    private static final int BORDER_STYLE_BLOSSOM = 3;
    @Unique
    private static final int BORDER_STYLE_BUBBLE = 4;
    @Unique
    private static final int BORDER_STYLE_EARTH = 5;
    @Unique
    private static final int BORDER_STYLE_ECHO = 6;
    @Unique
    private static final int BORDER_STYLE_ICE = 7;
    @Unique
    private static final TooltipTheme DEFAULT_THEME = new TooltipTheme(
            0xFFE2A834, 0xFF8A6A1E, 0xF02E2210, 0xF0181208,
            0xFFFFF0CC, 0xFFEEEEEE, 0xFF141008, 0xFFFFD5A0,
            0xFFE6ECF5, 0xFF8A6A1E, 0xFFE2A834, 0xFF2A1E0A,
            0xFF8A6A1E, 0xFF9D62CA, 0xFF5E8ACF, 0xFFDB5E71,
            0xFFE2A834, 0xFF3D3020, 0xFFC7D2E2
    );

    // Cache the real ItemStack (with NBT data) so drawTooltip fallback paths can access it
    @Unique
    private static ItemStack simplybows$lastRealStack = ItemStack.EMPTY;

    @Unique
    private static TooltipTheme simplybows$getTheme(String bowKey) {
        return switch (bowKey) {
            case "vine" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFF7CCB88, 0xFF3A6A43, 0xF01D2F1F, 0xF0122016,
                    0xFFEAF9E9, 0xFFE8F3E8, 0xFF102114, 0xFFBDECBF,
                    0xFFD9EFDA, 0xFF4D8C57, 0xFF7CCB88, 0xFF173120,
                    0xFF4D8C57, 0xFF86D59A, 0xFF6FA9E6, 0xFFC67AB4,
                    0xFF8FD79B, 0xFF243629, 0xFFBBD8C0
            ));
            case "echo" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFF8F7CFF, 0xFF4A3D8A, 0xF01D1738, 0xF0120F24,
                    0xFFF2EDFF, 0xFFECE9FF, 0xFF130F22, 0xFFC9C0FF,
                    0xFFE2DDF8, 0xFF6357B3, 0xFF8F7CFF, 0xFF201A3F,
                    0xFF6357B3, 0xFF9E8BFF, 0xFF76AAFF, 0xFFCE84E9,
                    0xFF9C90FF, 0xFF282246, 0xFFC8C2EC
            ));
            case "bee" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFFF5C64E, 0xFF8A6B22, 0xF030280E, 0xF01C1808,
                    0xFFFFF2CF, 0xFFF7EECC, 0xFF181206, 0xFFFFE19A,
                    0xFFF2E9CF, 0xFFA7832F, 0xFFF5C64E, 0xFF2A210B,
                    0xFFA7832F, 0xFFE1B342, 0xFFC99A3A, 0xFFF08E53,
                    0xFFF5C64E, 0xFF2A2417, 0xFFD7CAA8
            ));
            case "blossom" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFFF1A3C7, 0xFF8E4D66, 0xF0321B27, 0xF01E1118,
                    0xFFFFEEF6, 0xFFF8EAF1, 0xFF1D1117, 0xFFF7C7DB,
                    0xFFF2DEE7, 0xFFB06A8A, 0xFFF1A3C7, 0xFF351A26,
                    0xFFB06A8A, 0xFFD998C1, 0xFFB19AE8, 0xFFE37EA7,
                    0xFFEFA9CB, 0xFF33202A, 0xFFDCC2CE
            ));
            case "bubble" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFF66D5E5, 0xFF2E7284, 0xF0132B33, 0xF00C1A20,
                    0xFFE8FAFF, 0xFFE7F4F8, 0xFF0D1A1E, 0xFFA7ECF7,
                    0xFFD8EDF2, 0xFF4697A8, 0xFF66D5E5, 0xFF13303A,
                    0xFF4697A8, 0xFF7AE1EA, 0xFF7DBBEB, 0xFF78CDE0,
                    0xFF78DCE8, 0xFF1B3138, 0xFFBCD7DD
            ));
            case "earth" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFFD0AF7A, 0xFF7B6138, 0xF0302518, 0xF01B140C,
                    0xFFFFF1DD, 0xFFF5EDDE, 0xFF181208, 0xFFEACD9E,
                    0xFFEDDFC8, 0xFF9A7A49, 0xFFD0AF7A, 0xFF2A2114,
                    0xFF9A7A49, 0xFFC79C75, 0xFFA09CCC, 0xFFD49870,
                    0xFFD8B783, 0xFF2F261B, 0xFFD2C2AA
            ));
            case "ice" -> simplybows$withDefaultTextColors(new TooltipTheme(
                    0xFF9BD9FF, 0xFF4D7894, 0xF0152733, 0xF00D1921,
                    0xFFF0FAFF, 0xFFEAF4FA, 0xFF0E181D, 0xFFC0E9FF,
                    0xFFDDEDF8, 0xFF6EA5C4, 0xFF9BD9FF, 0xFF1A2F3B,
                    0xFF6EA5C4, 0xFFA5DFFF, 0xFF8AB9F7, 0xFF9ECBEA,
                    0xFFA8E0FF, 0xFF22333E, 0xFFC8DCE9
            ));
            default -> DEFAULT_THEME;
        };
    }

    @Unique
    private static int simplybows$getBorderStyle(String bowKey) {
        return switch (bowKey) {
            case "vine" -> BORDER_STYLE_VINE;
            case "bee" -> BORDER_STYLE_BEE;
            case "blossom" -> BORDER_STYLE_BLOSSOM;
            case "bubble" -> BORDER_STYLE_BUBBLE;
            case "earth" -> BORDER_STYLE_EARTH;
            case "echo" -> BORDER_STYLE_ECHO;
            case "ice" -> BORDER_STYLE_ICE;
            default -> BORDER_STYLE_DEFAULT;
        };
    }

    @Unique
    private static TooltipTheme simplybows$withDefaultTextColors(TooltipTheme base) {
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

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void simplybows$drawModernTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!SimplyBows.modernTooltipsEnabled || stack == null || stack.isEmpty() || !simplybows$isSimplyBowsItem(stack)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Cache the real stack before anything else
        simplybows$lastRealStack = stack;

        List<Text> raw = Screen.getTooltipFromItem(client, stack);
        if (raw == null || raw.isEmpty()) {
            return;
        }

        simplybows$renderModernTooltip((DrawContext) (Object) this, textRenderer, stack, raw, x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void simplybows$drawModernTooltipFromLines(TextRenderer textRenderer, List<Text> text, java.util.Optional<?> data, int x, int y, CallbackInfo ci) {
        simplybows$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void simplybows$drawModernTooltipFromSimpleLines(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
        simplybows$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    @Unique
    private void simplybows$tryRenderFromLines(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
        if (!SimplyBows.modernTooltipsEnabled || text == null || text.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Try multiple sources for the real ItemStack (with NBT upgrade data)
        String title = text.get(0).getString();
        ItemStack resolved = simplybows$findRealStack(client, title);
        if (resolved.isEmpty()) {
            return;
        }

        simplybows$renderModernTooltip((DrawContext) (Object) this, textRenderer, resolved, text, x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Unique
    private static boolean simplybows$isSimplyBowsItem(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null && SimplyBows.MOD_ID.equals(id.getNamespace());
    }

    @Unique
    private static ItemStack simplybows$findRealStack(MinecraftClient client, String title) {
        if (title == null || title.isBlank()) {
            return ItemStack.EMPTY;
        }

        // 1. Check cached stack from drawItemTooltip
        if (!simplybows$lastRealStack.isEmpty()
                && simplybows$isSimplyBowsItem(simplybows$lastRealStack)
                && simplybows$lastRealStack.getName().getString().equals(title)) {
            return simplybows$lastRealStack;
        }

        // 2. Try to get the focused slot stack from HandledScreen (inventory, creative, etc.)
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            try {
                // Access focusedSlot via reflection
                java.lang.reflect.Field focusedSlotField = null;
                for (java.lang.reflect.Field f : HandledScreen.class.getDeclaredFields()) {
                    if (f.getType() == Slot.class) {
                        focusedSlotField = f;
                        break;
                    }
                }
                if (focusedSlotField != null) {
                    focusedSlotField.setAccessible(true);
                    Slot slot = (Slot) focusedSlotField.get(handledScreen);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (simplybows$isSimplyBowsItem(slotStack)
                                && slotStack.getName().getString().equals(title)) {
                            return slotStack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Check cursor stack (item held by mouse)
        if (client.player != null) {
            ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty()
                    && simplybows$isSimplyBowsItem(cursorStack)
                    && cursorStack.getName().getString().equals(title)) {
                return cursorStack;
            }
        }

        // 4. Fallback: scan registry (no NBT data - upgrades will show as 0)
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id == null || !SimplyBows.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            ItemStack candidate = new ItemStack(item);
            if (candidate.getName().getString().equals(title)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    // --- Main tooltip renderer ---

    @Unique
    private static void simplybows$renderModernTooltip(DrawContext context, TextRenderer tr, ItemStack stack, List<Text> rawLines, int x, int y, int screenW, int screenH) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, 400.0f);

        String itemName = rawLines.isEmpty() ? "" : rawLines.get(0).getString();
        boolean isBow = stack.getItem() instanceof SimplyBowItem;

        // --- Parse tooltip lines into: ability, (upgrades skipped), other (before), extra (after) ---
        List<String> abilityLines = new ArrayList<>();
        List<String> otherLines = new ArrayList<>();  // lines before our sections
        List<Text> extraLines = new ArrayList<>();    // lines after our sections (enchantments, mod lines)
        // section: 0=before, 1=ability, 2=upgrades, 3=after upgrades
        int section = 0;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString().trim();

            // Detect section headers
            if (s.startsWith("\u25C6") || s.startsWith("\u25c6")) {
                String lower = s.toLowerCase();
                if (lower.contains("ability")) { section = 1; continue; }
                if (lower.contains("upgrade")) { section = 2; continue; }
            }

            // In upgrades section, detect when upgrade content ends
            if (section == 2) {
                if (s.isEmpty() || s.startsWith("\u2022") || s.startsWith("\u25E6")
                        || s.startsWith("\u2726") || s.toLowerCase().contains("hold alt")) {
                    continue;
                }
                section = 3;
            }

            if (s.isEmpty()) continue;

            switch (section) {
                case 0 -> otherLines.add(rawLines.get(i).getString());
                case 1 -> abilityLines.add(rawLines.get(i).getString());
                case 3 -> extraLines.add(rawLines.get(i));
            }
        }

        // Read upgrade data directly from the item stack
        BowUpgradeData upgrades = BowUpgradeData.from(stack);
        int usedSlots = upgrades.stringLevel() + upgrades.frameLevel();
        int maxSlots = BowUpgradeData.getMaxTotalUpgradeSlots();
        int maxString = Math.max(0, Math.min(BowUpgradeData.getMaxLevelPerType(), maxSlots - upgrades.frameLevel()));
        int maxFrame = Math.max(0, Math.min(BowUpgradeData.getMaxLevelPerType(), maxSlots - upgrades.stringLevel()));
        RuneEtching rune = upgrades.runeEtching();
        boolean hasUpgrades = isBow;

        // Get bow key for effect lookups
        String bowKey = "generic";
        if (stack.getItem() instanceof SimplyBowItem simplyBow) {
            try {
                java.lang.reflect.Method m = SimplyBowItem.class.getDeclaredMethod("getTooltipBowKey");
                m.setAccessible(true);
                bowKey = (String) m.invoke(simplyBow);
            } catch (Throwable ignored) {}
        }
        final String effectBowKey = bowKey;
        TooltipTheme theme = simplybows$getTheme(bowKey);
        int borderStyle = simplybows$getBorderStyle(bowKey);

        // --- Layout calculations ---
        int lineHeight = tr.fontHeight + LINE_SPACING;
        int sectionGap = 4; // padding below section headers
        int iconAreaW = 36;
        int headerH = PADDING + 16 + 6 + 12 + PADDING;
        int separatorH = 10;
        int upgradeRowH = lineHeight + 3;

        List<String> wrappedAbility = simplybows$wrapStrings(abilityLines, tr, MAX_TEXT_WIDTH);
        List<String> wrappedOther = simplybows$wrapStrings(otherLines, tr, MAX_TEXT_WIDTH);
        List<String> wrappedExtra = new ArrayList<>();
        for (Text t : extraLines) {
            wrappedExtra.addAll(simplybows$wrapStrings(List.of(t.getString()), tr, MAX_TEXT_WIDTH));
        }

        // Alt key toggles string/frame between pips and effect description
        boolean altDown = false;
        try { altDown = Screen.hasAltDown(); } catch (Throwable ignored) {}

        // Pre-wrap rune effect description
        String rawRuneEffect = hasUpgrades ? (rune != RuneEtching.NONE
                ? "  " + Text.translatable(simplybows$getRuneEffectKey(effectBowKey, rune)).getString().trim()
                : "  No rune etched.") : "";
        List<String> wrappedRuneEffect = simplybows$wrapStrings(List.of(rawRuneEffect), tr, MAX_TEXT_WIDTH);

        // Compute width
        int stringLabelW = tr.getWidth("\u2058 String ") + 2;
        int frameLabelW = tr.getWidth("\u25C8 Frame ") + 2;
        int stringEffectInlineW = hasUpgrades ? stringLabelW + tr.getWidth(Text.translatable(simplybows$getStringEffectKey(effectBowKey)).getString().trim()) : 0;
        int frameEffectInlineW = hasUpgrades ? frameLabelW + tr.getWidth(Text.translatable(simplybows$getFrameEffectKey(effectBowKey)).getString().trim()) : 0;

        int textContentW = 0;
        textContentW = Math.max(textContentW, tr.getWidth(itemName) + iconAreaW + 4);
        for (String s : wrappedAbility) textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedOther) textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedExtra) textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedRuneEffect) textContentW = Math.max(textContentW, tr.getWidth(s));
        if (altDown) {
            textContentW = Math.max(textContentW, stringEffectInlineW);
            textContentW = Math.max(textContentW, frameEffectInlineW);
        }
        textContentW = Math.max(textContentW, 150);

        int panelW = PADDING + textContentW + PADDING;

        boolean hasAbility = !wrappedAbility.isEmpty();
        boolean hasOther = !wrappedOther.isEmpty();
        boolean hasExtra = !wrappedExtra.isEmpty();

        int bodyH = 0;
        if (hasAbility) {
            bodyH += lineHeight + sectionGap + wrappedAbility.size() * lineHeight;
        }
        if (hasAbility && hasUpgrades) bodyH += separatorH;
        if (hasUpgrades) {
            bodyH += lineHeight + sectionGap; // header + gap
            bodyH += upgradeRowH; // slots row
            bodyH += upgradeRowH; // string row
            bodyH += upgradeRowH; // frame row
            bodyH += lineHeight; // spacer before rune row
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight; // rune
        }
        if (hasOther) bodyH += wrappedOther.size() * lineHeight;
        if (hasExtra) {
            bodyH += separatorH; // separator before extras
            bodyH += wrappedExtra.size() * lineHeight;
        }

        int footerH = 14;
        int panelH = headerH + (hasAbility || hasUpgrades || hasOther ? separatorH : 0) + bodyH + footerH;

        // Position tooltip
        int panelX = x + 12;
        int panelY = y - 12;
        if (panelX + panelW > screenW - 6) panelX = x - panelW - 12;
        if (panelX < 6) panelX = 6;
        if (panelY + panelH > screenH - 6) panelY = screenH - panelH - 6;
        if (panelY < 6) panelY = 6;

        // Draw background & border
        simplybows$drawGradientBackground(context, panelX, panelY, panelW, panelH, theme);
        simplybows$drawAmbientBackgroundMotif(context, panelX, panelY, panelW, panelH, borderStyle);
        simplybows$drawDecorativeBorder(context, panelX, panelY, panelW, panelH, theme, borderStyle);

        int cursorY = panelY + PADDING;

        // --- Header ---
        int iconFrameX = panelX + PADDING + 2;
        int iconFrameY = cursorY + 2;
        simplybows$drawDiamondFrame(context, iconFrameX, iconFrameY, 24, theme);

        context.getMatrices().push();
        context.drawItem(stack, iconFrameX + 4, iconFrameY + 4);
        context.getMatrices().pop();

        int nameX = panelX + PADDING + iconAreaW;
        context.drawText(tr, Text.literal(itemName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.name() & 0x00FFFFFF))), nameX, cursorY + 4, theme.name(), true);

        int badgeY = cursorY + 4 + tr.fontHeight + 3;
        int badgeX = nameX;
        if (isBow) {
            badgeX = simplybows$drawBadge(context, tr, "UNIQUE", badgeX, badgeY, theme);
            badgeX += 4;
            simplybows$drawBadge(context, tr, "BOW", badgeX, badgeY, theme);
        } else {
            simplybows$drawBadge(context, tr, "UNIQUE", badgeX, badgeY, theme);
        }

        cursorY = panelY + headerH;

        // --- Separator after header ---
        if (hasAbility || hasUpgrades || hasOther) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // --- Ability section ---
        if (hasAbility) {
            context.drawText(tr, Text.literal("\u25C6 Ability").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))), panelX + PADDING, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;
            for (String line : wrappedAbility) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.body() & 0x00FFFFFF))), panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // --- Separator between ability and upgrades ---
        if (hasAbility && hasUpgrades) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // --- Upgrades section (icon-based, rendered from data) ---
        if (hasUpgrades) {
            context.drawText(tr, Text.literal("\u25C6 Upgrades").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))), panelX + PADDING, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;

            int leftX = panelX + PADDING;

            // Slot pips:
            int slotLabelColor = simplybows$lerpColor(theme.body(), 0xFFFFFFFF, 0.10f);
            int stringLabelColor = simplybows$lerpColor(theme.stringColor(), 0xFFFFFFFF, 0.20f);
            int stringDescColor = simplybows$lerpColor(theme.stringColor(), theme.body(), 0.50f);
            int frameLabelColor = simplybows$lerpColor(theme.frameColor(), 0xFFFFFFFF, 0.18f);
            int frameDescColor = simplybows$lerpColor(theme.frameColor(), theme.body(), 0.50f);
            int runeLabelColor = simplybows$lerpColor(theme.runeColor(), 0xFFFFFFFF, 0.22f);
            context.drawText(tr, Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.slotFilled() & 0x00FFFFFF))), leftX, cursorY, theme.slotFilled(), false);
            int pipX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr, Text.literal("Slots").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(slotLabelColor & 0x00FFFFFF))), pipX, cursorY, slotLabelColor, false);
            pipX += tr.getWidth("Slots  ") + 2;
            for (int i = 0; i < maxSlots; i++) {
                int color = i < usedSlots ? theme.slotFilled() : theme.slotEmpty();
                context.fill(pipX, cursorY + 1, pipX + 5, cursorY + 6, color);
                context.fill(pipX, cursorY + 1, pipX + 5, cursorY + 2, i < usedSlots ? 0xFFF5D060 : 0xFF4A4030);
                pipX += 7;
            }
            cursorY += upgradeRowH;

            // Enchanted String:
            context.drawText(tr, Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.stringColor() & 0x00FFFFFF))), leftX, cursorY, theme.stringColor(), false);
            int labelX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr, Text.literal("String").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(stringLabelColor & 0x00FFFFFF))), labelX, cursorY, stringLabelColor, false);
            int afterStringLabel = labelX + tr.getWidth("String ") + 2;
            if (altDown) {
                // Effect text inline after label
                String stringEffectTrimmed = Text.translatable(simplybows$getStringEffectKey(effectBowKey)).getString().trim();
                context.drawText(tr, Text.literal(stringEffectTrimmed).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(stringDescColor & 0x00FFFFFF))), afterStringLabel, cursorY, stringDescColor, false);
            } else {
                // Level pips inline after label
                int stringPipX = afterStringLabel;
                for (int i = 0; i < maxString; i++) {
                    int color = i < upgrades.stringLevel() ? theme.stringColor() : theme.slotEmpty();
                    simplybows$drawPip(context, stringPipX, cursorY + 1, color, i < upgrades.stringLevel());
                    stringPipX += 7;
                }
            }
            cursorY += upgradeRowH;

            // Reinforced Frame:
            context.drawText(tr, Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.frameColor() & 0x00FFFFFF))), leftX, cursorY, theme.frameColor(), false);
            labelX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr, Text.literal("Frame").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(frameLabelColor & 0x00FFFFFF))), labelX, cursorY, frameLabelColor, false);
            int afterFrameLabel = labelX + tr.getWidth("Frame ") + 2;
            if (altDown) {
                // Effect text inline after label
                String frameEffectTrimmed = Text.translatable(simplybows$getFrameEffectKey(effectBowKey)).getString().trim();
                context.drawText(tr, Text.literal(frameEffectTrimmed).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(frameDescColor & 0x00FFFFFF))), afterFrameLabel, cursorY, frameDescColor, false);
            } else {
                // Level pips inline after label
                int framePipX = afterFrameLabel;
                for (int i = 0; i < maxFrame; i++) {
                    int color = i < upgrades.frameLevel() ? theme.frameColor() : theme.slotEmpty();
                    simplybows$drawPip(context, framePipX, cursorY + 1, color, i < upgrades.frameLevel());
                    framePipX += 7;
                }
            }
            cursorY += upgradeRowH;
            cursorY += lineHeight;


            if (!altDown) {
                // Do nothing
            }


            // Rune Etching: icon + rune name (always visible)
            context.drawText(tr, Text.literal("\u25CE").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.runeColor() & 0x00FFFFFF))), leftX, cursorY, theme.runeColor(), false);
            labelX = leftX + tr.getWidth("\u25CE ");
            String runeName = rune == RuneEtching.NONE ? "None" : Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
            int runeTextColor = rune == RuneEtching.NONE ? 0xFF6A6060 : theme.runeColor();
            context.drawText(tr, Text.literal("Rune: ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeLabelColor & 0x00FFFFFF))), labelX, cursorY, runeLabelColor, false);
            int runeValX = labelX + tr.getWidth("Rune: ");
            context.drawText(tr, Text.literal(runeName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeTextColor & 0x00FFFFFF))), runeValX, cursorY, runeTextColor, false);
            cursorY += upgradeRowH;
            // Rune effect description (always shown)
            int runeDescColor = rune != RuneEtching.NONE ? simplybows$lerpColor(theme.runeColor(), theme.body(), 0.45f) : 0xFF6A6060;
            for (String eLine : wrappedRuneEffect) {
                context.drawText(tr, Text.literal(eLine).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeDescColor & 0x00FFFFFF))), leftX, cursorY, runeDescColor, false);
                cursorY += lineHeight;
            }
        }

        // --- Other lines (for non-bow items) ---
        if (hasOther) {
            for (String line : wrappedOther) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.body() & 0x00FFFFFF))), panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // --- Extra lines (enchantments, mod-added tooltip lines, durability, etc.) ---
        if (hasExtra) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
            int extraColor = simplybows$lerpColor(theme.body(), 0xFFB8C2CF, 0.30f);
            for (String line : wrappedExtra) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(extraColor & 0x00FFFFFF))), panelX + PADDING, cursorY, extraColor, true);
                cursorY += lineHeight;
            }
        }

        // --- Footer dots ---
        simplybows$drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8, theme);

        context.getMatrices().pop();
    }

    // --- Helper: effect translation keys (mirrors BowTooltipHelper logic) ---

    @Unique
    private static String simplybows$getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine", "earth", "echo", "ice", "bee", "bubble", "blossom" -> "tooltip.simplybows.bow." + bowKey + ".string_effect";
            default -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    @Unique
    private static String simplybows$getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine", "earth", "echo", "ice", "bee", "bubble", "blossom" -> "tooltip.simplybows.bow." + bowKey + ".frame_effect";
            default -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    @Unique
    private static String simplybows$getRuneEffectKey(String bowKey, RuneEtching rune) {
        return switch (bowKey) {
            case "bee", "vine", "earth", "ice", "echo", "bubble", "blossom" -> "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
            default -> "tooltip.simplybows.bow.generic.rune." + rune.id();
        };
    }

    // --- Drawing helpers ---

    @Unique
    private static void simplybows$drawPip(DrawContext context, int x, int y, int color, boolean filled) {
        // Small 5x5 square pip with top highlight
        context.fill(x, y, x + 5, y + 5, color);
        if (filled) {
            int highlight = simplybows$lerpColor(color, 0xFFFFFFFF, 0.35f);
            context.fill(x, y, x + 5, y + 1, highlight);
        } else {
            context.fill(x, y, x + 5, y + 1, 0xFF4A4030);
        }
    }

    @Unique
    private static void simplybows$drawGradientBackground(DrawContext context, int x, int y, int w, int h, TooltipTheme theme) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0.0F : (float) i / (float) (h - 1);
            int row = simplybows$lerpColor(theme.bgTop(), theme.bgBottom(), t);
            context.fill(x, y + i, x + w, y + i + 1, row);
        }
    }

    @Unique
    private static void simplybows$drawDecorativeBorder(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, int borderStyle) {
        context.fill(x, y, x + w, y + 1, theme.border());
        context.fill(x, y + h - 1, x + w, y + h, theme.border());
        context.fill(x, y, x + 1, y + h, theme.border());
        context.fill(x + w - 1, y, x + w, y + h, theme.border());

        context.fill(x + 1, y + 1, x + w - 1, y + 2, theme.borderInner());
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, theme.borderInner());
        context.fill(x + 1, y + 1, x + 2, y + h - 1, theme.borderInner());
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, theme.borderInner());

        simplybows$drawSmallDiamond(context, x + 6, y, theme.border());
        simplybows$drawSmallDiamond(context, x + w - 7, y, theme.border());
        simplybows$drawSmallDiamond(context, x + 6, y + h - 1, theme.border());
        simplybows$drawSmallDiamond(context, x + w - 7, y + h - 1, theme.border());

        simplybows$drawBorderPattern(context, x, y, w, h, theme, borderStyle);
    }

    @Unique
    private static void simplybows$drawAmbientBackgroundMotif(DrawContext context, int x, int y, int w, int h, int borderStyle) {
        if (w < 40 || h < 40) {
            return;
        }

        switch (borderStyle) {
            case BORDER_STYLE_BLOSSOM -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);

                for (int i = 0; i < 7; i++) {
                    double fallSpeed = 0.010 + i * 0.0012;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 19;
                    int py = minY + (int) (fallProgress % travel);

                    int lane = (i * 37 + i * i * 11) % spawnRange;
                    double swayA = Math.sin((time * 0.0009) + i * 2.1) * (w * 0.22);
                    double swayB = Math.sin((time * 0.0017) + i * 0.9) * 8.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 5, baseX));

                    int coarseSpin = (int) (fallProgress / 26.0);
                    int rotation = Math.floorMod(i * 3 + coarseSpin, 4);

                    simplybows$drawRotatedBlossomPetal(context, px, py, rotation);
                }
            }
            case BORDER_STYLE_ICE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);

                for (int i = 0; i < 8; i++) {
                    double fallSpeed = 0.010 + i * 0.0010;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 23;
                    int py = minY + (int) (fallProgress % travel);

                    int lane = (i * 41 + i * i * 7) % spawnRange;
                    double swayA = Math.sin((time * 0.0010) + i * 1.6) * (w * 0.08);
                    double swayB = Math.sin((time * 0.0021) + i * 0.8) * 2.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 4, baseX));
                    int rotation = Math.floorMod(i + (int) (fallProgress / 36.0), 4);
                    int sizeVariant = ((i * 17) + 5) % 3;

                    simplybows$drawRotatedSnowflake(context, px, py, rotation, sizeVariant);
                }
            }
            case BORDER_STYLE_VINE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);

                for (int i = 0; i < 6; i++) {
                    double fallSpeed = 0.009 + i * 0.0011;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 27;
                    int py = minY + (int) (fallProgress % travel);

                    int lane = (i * 43 + i * i * 13) % spawnRange;
                    double swayA = Math.sin((time * 0.0011) + i * 2.0) * (w * 0.20);
                    double swayB = Math.sin((time * 0.0024) + i * 1.3) * 7.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 5, baseX));
                    int rotation = Math.floorMod(i * 2 + (int) (fallProgress / 30.0), 4);

                    simplybows$drawRotatedVineLeaf(context, px, py, rotation);
                }
            }
            case BORDER_STYLE_EARTH -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spawnRange = Math.max(10, maxX - minX - 1);

                for (int i = 0; i < 16; i++) {
                    int speedBand = (i * 13 + 5) % 6;
                    double fallSpeed = 0.009 + speedBand * 0.0011 + i * 0.00015;
                    int travel = Math.max(8, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 11;
                    int py = minY + (int) (fallProgress % travel);

                    int lane = (i * 29 + i * i * 5) % spawnRange;
                    double drift = Math.sin((time * 0.0016) + i * 1.1) * 2.0;

                    int sizeRoll = (i * 7 + 3) % 10;
                    int size = sizeRoll < 6 ? 1 : 2;
                    if (sizeRoll == 9) {
                        size = 3;
                    }
                    int px = Math.max(minX, Math.min(maxX - size, minX + lane + (int) drift));
                    int cA = ((i + ((int) fallProgress / 24)) & 1) == 0 ? 0x1CB08C66 : 0x168A6A4B;
                    int cB = 0x126E583F;

                    if (size == 1) {
                        context.fill(px, py, px + 1, py + 1, cA);
                    } else if (size == 2) {
                        context.fill(px, py, px + 2, py + 2, cA);
                        context.fill(px + 1, py, px + 2, py + 1, cB);
                    } else {
                        context.fill(px, py, px + 3, py + 2, cA);
                        context.fill(px + 1, py + 1, px + 3, py + 2, cB);
                    }
                }
            }
            case BORDER_STYLE_BUBBLE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 5);

                for (int i = 0; i < 8; i++) {
                    // Original baseline speed with tiny fixed per-particle variance.
                    int speedSeed = ((i * 17) + 3) % 5; // 0..4
                    double speedOffset = (speedSeed - 2) * 0.00008; // -0.00016 .. +0.00016
                    double riseSpeed = (0.008 + i * 0.0009) + speedOffset;
                    int travel = Math.max(10, maxY - minY);
                    double riseProgress = (time * riseSpeed) + i * 21;
                    int py = maxY - (int) (riseProgress % travel);

                    int lane = (i * 31 + i * i * 9) % spawnRange;
                    double wobble = Math.sin((time * 0.0015) + i * 1.4) * 6.0;
                    int px = Math.max(minX, Math.min(maxX - 4, minX + lane + (int) wobble));

                    context.fill(px, py, px + 2, py + 2, 0x148EE7F8);
                    context.fill(px + 1, py + 1, px + 4, py + 4, 0x0EC8F7FF);
                    context.fill(px + 1, py, px + 2, py + 1, 0x1AF2FDFF);
                }
            }
            case BORDER_STYLE_BEE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spanX = Math.max(10, maxX - minX - 2);
                int spanY = Math.max(10, maxY - minY - 2);

                for (int i = 0; i < 7; i++) {
                    int baseX = minX + ((i * 47 + i * i * 7) % spanX);
                    int baseY = minY + ((i * 29 + i * i * 5) % spanY);
                    int px = baseX + (int) (Math.sin((time * 0.0030) + i * 1.8) * 5.0);
                    int py = baseY + (int) (Math.sin((time * 0.0024) + i * 1.2) * 3.0);
                    px = Math.max(minX, Math.min(maxX - 2, px));
                    py = Math.max(minY, Math.min(maxY - 2, py));

                    context.fill(px, py, px + 1, py + 1, 0x20F2C74A);
                    context.fill(px + 1, py, px + 2, py + 1, 0x167A5A22);
                }
            }
            case BORDER_STYLE_ECHO -> {
                long time = System.currentTimeMillis();
                int minX = x + 6;
                int maxX = x + w - 6;
                int minY = y + 6;
                int maxY = y + h - 6;
                int spanX = Math.max(10, maxX - minX - 3);
                int spanY = Math.max(10, maxY - minY - 3);

                for (int i = 0; i < 7; i++) {
                    int laneX = minX + ((i * 37 + i * i * 9) % spanX);
                    int laneY = minY + ((i * 23 + i * i * 11) % spanY);
                    int px = laneX + (int) (Math.sin((time * 0.0019) + i * 1.5) * 7.0);
                    int py = laneY + (int) (Math.sin((time * 0.0022) + i * 1.1) * 4.0);
                    px = Math.max(minX, Math.min(maxX - 3, px));
                    py = Math.max(minY, Math.min(maxY - 3, py));

                    int pulse = (((int) (time / 240L) + i) & 1);
                    int runeA = pulse == 0 ? 0x18B59AFF : 0x12B59AFF;
                    int runeB = pulse == 0 ? 0x167C67D9 : 0x107C67D9;

                    context.fill(px, py + 1, px + 3, py + 2, runeA);
                    context.fill(px + 1, py, px + 2, py + 3, runeB);
                }
            }
            default -> {
            }
        }
    }

    @Unique
    private static void simplybows$drawRotatedBlossomPetal(DrawContext context, int x, int y, int rotation) {
        int petalA = 0x14F3B1D2;
        int petalB = 0x10E38BB8;
        int petalC = 0x0CF6C6DE;
        int core = 0x16FFF4BE;

        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, petalA);
                context.fill(x, y + 1, x + 2, y + 5, petalB);
                context.fill(x + 2, y, x + 4, y + 3, petalC);
                context.fill(x + 1, y + 2, x + 2, y + 3, core);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, petalA);
                context.fill(x, y + 1, x + 4, y + 3, petalB);
                context.fill(x + 2, y + 2, x + 5, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, petalA);
                context.fill(x + 1, y + 1, x + 3, y + 5, petalB);
                context.fill(x, y + 1, x + 3, y + 3, petalC);
                context.fill(x + 2, y + 2, x + 3, y + 3, core);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, petalA);
                context.fill(x + 1, y + 1, x + 5, y + 3, petalB);
                context.fill(x, y + 2, x + 3, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
        }
    }

    @Unique
    private static void simplybows$drawRotatedSnowflake(DrawContext context, int x, int y, int rotation, int sizeVariant) {
        int outer = 0x12D8F2FF;
        int inner = 0x18E7F8FF;
        int core = 0x20FFFFFF;

        if ((sizeVariant & 1) == 0) {
            // 3x3 soft round mote
            context.fill(x + 1, y, x + 2, y + 1, outer);
            context.fill(x, y + 1, x + 3, y + 2, inner);
            context.fill(x + 1, y + 2, x + 2, y + 3, outer);
            if ((rotation & 1) == 0) {
                context.fill(x + 1, y + 1, x + 2, y + 2, core);
            } else {
                context.fill(x + 1, y + 1, x + 2, y + 2, 0x18FFFFFF);
            }
        } else {
            // 4x4 soft round mote
            context.fill(x + 1, y, x + 3, y + 1, outer);
            context.fill(x, y + 1, x + 4, y + 3, inner);
            context.fill(x + 1, y + 3, x + 3, y + 4, outer);
            if ((rotation & 1) == 0) {
                context.fill(x + 1, y + 1, x + 3, y + 3, core);
            } else {
                context.fill(x + 1, y + 1, x + 3, y + 3, 0x18FFFFFF);
            }
        }
    }

    @Unique
    private static void simplybows$drawRotatedVineLeaf(DrawContext context, int x, int y, int rotation) {
        int leafA = 0x1A84C47E;
        int leafB = 0x1470B16A;
        int vein = 0x1A3F7E45;

        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, leafA);
                context.fill(x, y + 1, x + 2, y + 5, leafB);
                context.fill(x + 1, y + 1, x + 2, y + 4, vein);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, leafA);
                context.fill(x, y + 1, x + 4, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, leafA);
                context.fill(x + 1, y + 1, x + 3, y + 5, leafB);
                context.fill(x + 2, y + 1, x + 3, y + 4, vein);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, leafA);
                context.fill(x + 1, y + 1, x + 5, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
        }
    }

    @Unique
    private static void simplybows$drawBorderPattern(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, int borderStyle) {
        switch (borderStyle) {
            case BORDER_STYLE_VINE -> {
                int leafA = 0xFF79BE77;
                int leafB = 0xFF5EA661;
                int stem = 0xFF3E7A44;
                for (int px = x + 9, i = 0; px < x + w - 10; px += 11, i++) {
                    boolean flip = (i & 1) == 0;
                    int c = flip ? leafA : leafB;
                    // Leaves
                    context.fill(px, y + 1, px + 2, y + 2, c);
                    if (flip) {
                        context.fill(px + 1, y + 2, px + 3, y + 3, c);
                    } else {
                        context.fill(px - 1, y + 2, px + 1, y + 3, c);
                    }
                    // Mirrored
                    context.fill(px, y + h - 3, px + 2, y + h - 2, c);
                    if (flip) {
                        context.fill(px - 1, y + h - 2, px + 1, y + h - 1, c);
                    } else {
                        context.fill(px + 1, y + h - 2, px + 3, y + h - 1, c);
                    }
                    // Tendril
                    if (i % 3 == 0) {
                        context.fill(px, y + 3, px + 1, y + 5, stem);
                        context.fill(px, y + h - 5, px + 1, y + h - 3, stem);
                    }
                }
            }
            case BORDER_STYLE_BEE -> {
                int honey = 0xFFE8B847;
                int wax = 0xFFF4D77B;
                int outline = 0xFF6A4A1C;
                // Honeycomb cells
                for (int px = x + 8; px < x + w - 10; px += 12) {
                    context.fill(px, y + 1, px + 3, y + 3, honey);
                    context.fill(px + 1, y, px + 2, y + 1, wax);
                    context.fill(px, y + 1, px + 1, y + 2, outline);
                    context.fill(px + 2, y + 2, px + 3, y + 3, outline);

                    context.fill(px, y + h - 3, px + 3, y + h - 1, honey);
                    context.fill(px + 1, y + h - 1, px + 2, y + h, wax);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, outline);
                    context.fill(px + 2, y + h - 3, px + 3, y + h - 2, outline);
                }
                // Wing accents
                context.fill(x + 3, y + 4, x + 5, y + 5, 0x99DDF7FF);
                context.fill(x + w - 5, y + 4, x + w - 3, y + 5, 0x99DDF7FF);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 4, 0x99DDF7FF);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 4, 0x99DDF7FF);
            }
            case BORDER_STYLE_BLOSSOM -> {
                int petal = 0xFFF3B1D2;
                int core = 0xFFFFF4BE;
                for (int px = x + 12; px < x + w - 12; px += 16) {
                    context.fill(px, y + 1, px + 1, y + 4, petal);
                    context.fill(px - 1, y + 2, px + 2, y + 3, petal);
                    context.fill(px, y + 2, px + 1, y + 3, core);
                    context.fill(px, y + h - 4, px + 1, y + h - 1, petal);
                    context.fill(px - 1, y + h - 3, px + 2, y + h - 2, petal);
                    context.fill(px, y + h - 3, px + 1, y + h - 2, core);
                }
            }
            case BORDER_STYLE_BUBBLE -> {
                int crest = 0xFF8EE7F8;
                int foam = 0xFFC8F7FF;
                // Wave
                for (int px = x + 6, step = 0; px < x + w - 6; px += 8, step++) {
                    int dy = (step % 2 == 0) ? 0 : 1;
                    context.fill(px, y + 1 + dy, px + 5, y + 2 + dy, crest);
                    context.fill(px, y + h - 2 - dy, px + 5, y + h - 1 - dy, crest);
                }
                // Sparse bubbles
                for (int px = x + 12; px < x + w - 12; px += 18) {
                    context.fill(px, y + 2, px + 2, y + 4, foam);
                    context.fill(px + 1, y + 3, px + 3, y + 5, crest);
                    context.fill(px, y + h - 5, px + 2, y + h - 3, foam);
                    context.fill(px + 1, y + h - 4, px + 3, y + h - 2, crest);
                }
            }
            case BORDER_STYLE_EARTH -> {
                int rockDark = simplybows$lerpColor(theme.border(), 0xFF3A2E22, 0.55f);
                int rockMid = simplybows$lerpColor(theme.border(), 0xFF6E5A40, 0.32f);
                int dust = simplybows$lerpColor(theme.borderInner(), 0xFFCAB28D, 0.22f);
                // Uneven stone chunks
                for (int px = x + 8, i = 0; px < x + w - 9; px += 10, i++) {
                    int wChunk = (i % 3 == 0) ? 3 : 2;
                    context.fill(px, y + 1, px + wChunk, y + 2, rockMid);
                    context.fill(px + 1, y + 2, px + wChunk + 1, y + 3, rockDark);

                    context.fill(px, y + h - 3, px + wChunk, y + h - 2, rockMid);
                    context.fill(px - 1, y + h - 2, px + wChunk - 1, y + h - 1, rockDark);
                }
                // Hairline cracks
                for (int px = x + 14; px < x + w - 14; px += 18) {
                    context.fill(px, y + 2, px + 1, y + 4, dust);
                    context.fill(px + 1, y + 3, px + 2, y + 4, rockDark);
                    context.fill(px, y + h - 4, px + 1, y + h - 2, dust);
                    context.fill(px - 1, y + h - 3, px, y + h - 2, rockDark);
                }
                // Small corner pebbles
                context.fill(x + 3, y + 3, x + 5, y + 5, rockMid);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 5, rockMid);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 3, rockMid);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, rockMid);
            }
            case BORDER_STYLE_ECHO -> {
                int runeA = 0xFFB59AFF;
                int runeB = 0xFF7C67D9;
                // Alternating rune sigils
                for (int px = x + 10, i = 0; px < x + w - 10; px += 16, i++) {
                    if ((i & 1) == 0) {
                        // crescent
                        context.fill(px, y + 1, px + 1, y + 4, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 2, runeB);
                        context.fill(px + 1, y + 3, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 3, runeB);
                        context.fill(px + 1, y + h - 2, px + 2, y + h - 1, runeB);
                    } else {
                        // spark
                        context.fill(px, y + 2, px + 3, y + 3, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
                    }
                }
            }
            case BORDER_STYLE_ICE -> {
                int ice = 0xFFBFE9FF;
                for (int px = x + 10; px < x + w - 10; px += 12) {
                    context.fill(px, y, px + 1, y + 3, ice);
                    context.fill(px - 1, y + 1, px + 2, y + 2, ice);
                    context.fill(px, y + h - 3, px + 1, y + h, ice);
                    context.fill(px - 1, y + h - 2, px + 2, y + h - 1, ice);
                }
            }
            default -> {
            }
        }
    }

    @Unique
    private static void simplybows$drawSmallDiamond(DrawContext context, int cx, int cy, int color) {
        context.fill(cx, cy - 1, cx + 1, cy, color);
        context.fill(cx - 1, cy, cx + 2, cy + 1, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    @Unique
    private static void simplybows$drawSeparator(DrawContext context, int x, int y, int width, TooltipTheme theme) {
        int lineY = y + 4;
        int midX = x + width / 2;

        context.fill(x + 4, lineY, midX - 5, lineY + 1, theme.separator());
        context.fill(midX + 5, lineY, x + width - 4, lineY + 1, theme.separator());
        simplybows$drawSmallDiamond(context, midX, lineY, theme.border());
    }

    @Unique
    private static void simplybows$drawDiamondFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 2;

        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrameInner());
        }

        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx - span + 1, cy + dy + 1, theme.diamondFrame());
            context.fill(cx + span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrame());
        }
        context.fill(cx, cy - half, cx + 1, cy - half + 1, theme.diamondFrame());
        context.fill(cx, cy + half, cx + 1, cy + half + 1, theme.diamondFrame());
    }

    @Unique
    private static int simplybows$drawBadge(DrawContext context, TextRenderer tr, String label, int x, int y, TooltipTheme theme) {
        int textW = tr.getWidth(label);
        int badgePadH = 3;
        int badgeH = tr.fontHeight;

        int badgeW = textW + badgePadH * 2;

        // White badge background
        context.fill(x, y, x + badgeW, y + badgeH, theme.badgeBg());

        // Dark cutout text vertically centered
        int textY = y + 1;
        context.drawText(tr, Text.literal(label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.badgeCutout() & 0x00FFFFFF))), x + badgePadH, textY, theme.badgeCutout(), false);

        return x + badgeW;
    }

    @Unique
    private static void simplybows$drawFooterDots(DrawContext context, int cx, int y, TooltipTheme theme) {
        simplybows$drawSmallDiamond(context, cx - 8, y, theme.footerDot());
        simplybows$drawSmallDiamond(context, cx, y, theme.footerDot());
        simplybows$drawSmallDiamond(context, cx + 8, y, theme.footerDot());
    }

    @Unique
    private static List<String> simplybows$wrapStrings(List<String> lines, TextRenderer tr, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) {
                wrapped.add(" ");
                continue;
            }
            String[] words = raw.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (tr.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                    wrapped.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                wrapped.add(current.toString());
            }
        }
        return wrapped;
    }

    @Unique
    private static int simplybows$lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        return ((int)(aA + (bA - aA) * t) << 24) | ((int)(aR + (bR - aR) * t) << 16) |
               ((int)(aG + (bG - aG) * t) << 8)  | (int)(aB + (bB - aB) * t);
    }
}
