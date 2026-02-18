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

    // Colors — gold/amber theme derived from BowTooltipHelper.STYLE_UNIQUE_NAME (#E2A834)
    @Unique private static final int COLOR_BORDER = 0xFFE2A834;
    @Unique private static final int COLOR_BORDER_INNER = 0xFF8A6A1E;
    @Unique private static final int COLOR_BG_TOP = 0xF02E2210;
    @Unique private static final int COLOR_BG_BOTTOM = 0xF0181208;
    @Unique private static final int COLOR_NAME = 0xFFFFF0CC;
    @Unique private static final int COLOR_BADGE_BG = 0xFFEEEEEE;
    @Unique private static final int COLOR_BADGE_CUTOUT = 0xFF141008;
    @Unique private static final int COLOR_SECTION_HEADER = 0xFFFFD5A0;
    @Unique private static final int COLOR_BODY = 0xFFE6ECF5;
    @Unique private static final int COLOR_SEPARATOR = 0xFF8A6A1E;
    @Unique private static final int COLOR_DIAMOND_FRAME = 0xFFE2A834;
    @Unique private static final int COLOR_DIAMOND_FRAME_INNER = 0xFF2A1E0A;
    @Unique private static final int COLOR_FOOTER_DOT = 0xFF8A6A1E;

    // Upgrade icon colors
    @Unique private static final int COLOR_STRING = 0xFF9D62CA;
    @Unique private static final int COLOR_FRAME = 0xFF5E8ACF;
    @Unique private static final int COLOR_RUNE = 0xFFDB5E71;
    @Unique private static final int COLOR_SLOT_FILLED = 0xFFE2A834;
    @Unique private static final int COLOR_SLOT_EMPTY = 0xFF3D3020;
    @Unique private static final int COLOR_HINT = 0xFFC7D2E2;

    // Cache the real ItemStack (with NBT data) so drawTooltip fallback paths can access it
    @Unique
    private static ItemStack simplybows$lastRealStack = ItemStack.EMPTY;

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void simplybows$drawModernTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!SimplyBows.modernTooltipsEnabled || stack == null || stack.isEmpty() || !simplybows$isSimplyBowsItem(stack)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Cache the real stack before anything else — drawTooltip may fire from within getTooltipFromItem
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
                // Access focusedSlot via reflection — it's a protected field
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

        // 4. Fallback: scan registry (no NBT data — upgrades will show as 0)
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
        // section: 0=before ◆, 1=ability, 2=upgrades, 3=after upgrades
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
                    continue; // still upgrade content — skip
                }
                // Not an upgrade line — section ended
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

        // Get bow key for effect lookups — use the item's own method
        String bowKey = "generic";
        if (stack.getItem() instanceof SimplyBowItem simplyBow) {
            try {
                java.lang.reflect.Method m = SimplyBowItem.class.getDeclaredMethod("getTooltipBowKey");
                m.setAccessible(true);
                bowKey = (String) m.invoke(simplyBow);
            } catch (Throwable ignored) {}
        }
        final String effectBowKey = bowKey;

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

        // Pre-wrap rune effect description (always shown below rune row)
        String rawRuneEffect = hasUpgrades ? (rune != RuneEtching.NONE
                ? "  " + Text.translatable(simplybows$getRuneEffectKey(effectBowKey, rune)).getString().trim()
                : "  No rune etched.") : "";
        List<String> wrappedRuneEffect = simplybows$wrapStrings(List.of(rawRuneEffect), tr, MAX_TEXT_WIDTH);

        // Compute width — account for inline effect text when alt is held
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
            bodyH += upgradeRowH; // string row (pips or effect text — same line either way)
            bodyH += upgradeRowH; // frame row (pips or effect text — same line either way)
            if (!altDown) bodyH += lineHeight; // "Hold Alt" hint
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight; // rune always shows effect
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
        simplybows$drawGradientBackground(context, panelX, panelY, panelW, panelH);
        simplybows$drawDecorativeBorder(context, panelX, panelY, panelW, panelH);

        int cursorY = panelY + PADDING;

        // --- Header ---
        int iconFrameX = panelX + PADDING + 2;
        int iconFrameY = cursorY + 2;
        simplybows$drawDiamondFrame(context, iconFrameX, iconFrameY, 24);

        context.getMatrices().push();
        context.drawItem(stack, iconFrameX + 4, iconFrameY + 4);
        context.getMatrices().pop();

        int nameX = panelX + PADDING + iconAreaW;
        context.drawText(tr, Text.literal(itemName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFF0CC))), nameX, cursorY + 4, COLOR_NAME, true);

        int badgeY = cursorY + 4 + tr.fontHeight + 3;
        int badgeX = nameX;
        if (isBow) {
            badgeX = simplybows$drawBadge(context, tr, "UNIQUE", badgeX, badgeY);
            badgeX += 4;
            simplybows$drawBadge(context, tr, "BOW", badgeX, badgeY);
        } else {
            simplybows$drawBadge(context, tr, "UNIQUE", badgeX, badgeY);
        }

        cursorY = panelY + headerH;

        // --- Separator after header ---
        if (hasAbility || hasUpgrades || hasOther) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2);
            cursorY += separatorH;
        }

        // --- Ability section ---
        if (hasAbility) {
            context.drawText(tr, Text.literal("\u25C6 Ability").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD5A0))), panelX + PADDING, cursorY, COLOR_SECTION_HEADER, true);
            cursorY += lineHeight + sectionGap;
            for (String line : wrappedAbility) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE6ECF5))), panelX + PADDING, cursorY, COLOR_BODY, true);
                cursorY += lineHeight;
            }
        }

        // --- Separator between ability and upgrades ---
        if (hasAbility && hasUpgrades) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2);
            cursorY += separatorH;
        }

        // --- Upgrades section (icon-based, rendered from data) ---
        if (hasUpgrades) {
            context.drawText(tr, Text.literal("\u25C6 Upgrades").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD5A0))), panelX + PADDING, cursorY, COLOR_SECTION_HEADER, true);
            cursorY += lineHeight + sectionGap;

            int leftX = panelX + PADDING;

            // Slot pips: ◇ icon — filled/empty squares
            context.drawText(tr, Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE2A834))), leftX, cursorY, COLOR_SLOT_FILLED, false);
            int pipX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr, Text.literal("Slots").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD3C8B0))), pipX, cursorY, 0xFFD3C8B0, false);
            pipX += tr.getWidth("Slots ") + 2;
            for (int i = 0; i < maxSlots; i++) {
                int color = i < usedSlots ? COLOR_SLOT_FILLED : COLOR_SLOT_EMPTY;
                context.fill(pipX, cursorY + 1, pipX + 5, cursorY + 6, color);
                context.fill(pipX, cursorY + 1, pipX + 5, cursorY + 2, i < usedSlots ? 0xFFF5D060 : 0xFF4A4030);
                pipX += 7;
            }
            cursorY += upgradeRowH;

            // Enchanted String: ⁘ icon + label + (pips OR effect text inline)
            context.drawText(tr, Text.literal("\u2058").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9D62CA))), leftX, cursorY, COLOR_STRING, false);
            int labelX = leftX + tr.getWidth("\u2058 ");
            context.drawText(tr, Text.literal("String").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xBB8ADE))), labelX, cursorY, 0xFFBB8ADE, false);
            int afterStringLabel = labelX + tr.getWidth("String ") + 2;
            if (altDown) {
                // Effect text inline after label
                String stringEffectTrimmed = Text.translatable(simplybows$getStringEffectKey(effectBowKey)).getString().trim();
                context.drawText(tr, Text.literal(stringEffectTrimmed).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9D8ABB))), afterStringLabel, cursorY, 0xFF9D8ABB, false);
            } else {
                // Level pips inline after label
                int stringPipX = afterStringLabel;
                for (int i = 0; i < maxString; i++) {
                    int color = i < upgrades.stringLevel() ? COLOR_STRING : COLOR_SLOT_EMPTY;
                    simplybows$drawPip(context, stringPipX, cursorY + 1, color, i < upgrades.stringLevel());
                    stringPipX += 7;
                }
            }
            cursorY += upgradeRowH;

            // Reinforced Frame: ◈ icon + label + (pips OR effect text inline)
            context.drawText(tr, Text.literal("\u25C8").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5E8ACF))), leftX, cursorY, COLOR_FRAME, false);
            labelX = leftX + tr.getWidth("\u25C8 ");
            context.drawText(tr, Text.literal("Frame").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x7EA8E0))), labelX, cursorY, 0xFF7EA8E0, false);
            int afterFrameLabel = labelX + tr.getWidth("Frame ") + 2;
            if (altDown) {
                // Effect text inline after label
                String frameEffectTrimmed = Text.translatable(simplybows$getFrameEffectKey(effectBowKey)).getString().trim();
                context.drawText(tr, Text.literal(frameEffectTrimmed).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x7090B8))), afterFrameLabel, cursorY, 0xFF7090B8, false);
            } else {
                // Level pips inline after label
                int framePipX = afterFrameLabel;
                for (int i = 0; i < maxFrame; i++) {
                    int color = i < upgrades.frameLevel() ? COLOR_FRAME : COLOR_SLOT_EMPTY;
                    simplybows$drawPip(context, framePipX, cursorY + 1, color, i < upgrades.frameLevel());
                    framePipX += 7;
                }
            }
            cursorY += upgradeRowH;
            cursorY += lineHeight;


            if (!altDown) {
                // Hint to hold Alt
                // context.drawText(tr, Text.literal("\u25E6 Hold Alt for details").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC7D2E2))), leftX + 2, cursorY, COLOR_HINT, false);
                //cursorY += lineHeight;
            }


            // Rune Etching: ◎ icon + rune name (always visible)
            context.drawText(tr, Text.literal("\u25CE").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xDB5E71))), leftX, cursorY, COLOR_RUNE, false);
            labelX = leftX + tr.getWidth("\u25CE ");
            String runeName = rune == RuneEtching.NONE ? "None" : Text.translatable("tooltip.simplybows.rune." + rune.id()).getString();
            int runeTextColor = rune == RuneEtching.NONE ? 0xFF6A6060 : 0xFFDB5E71;
            context.drawText(tr, Text.literal("Rune: ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xDBA0AB))), labelX, cursorY, 0xFFDBA0AB, false);
            int runeValX = labelX + tr.getWidth("Rune: ");
            context.drawText(tr, Text.literal(runeName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeTextColor & 0x00FFFFFF))), runeValX, cursorY, runeTextColor, false);
            cursorY += upgradeRowH;
            // Rune effect description (always shown)
            int runeDescColor = rune != RuneEtching.NONE ? 0xFFC07080 : 0xFF6A6060;
            for (String eLine : wrappedRuneEffect) {
                context.drawText(tr, Text.literal(eLine).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeDescColor & 0x00FFFFFF))), leftX, cursorY, runeDescColor, false);
                cursorY += lineHeight;
            }
        }

        // --- Other lines (for non-bow items) ---
        if (hasOther) {
            for (String line : wrappedOther) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE6ECF5))), panelX + PADDING, cursorY, COLOR_BODY, true);
                cursorY += lineHeight;
            }
        }

        // --- Extra lines (enchantments, mod-added tooltip lines, durability, etc.) ---
        if (hasExtra) {
            simplybows$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2);
            cursorY += separatorH;
            for (String line : wrappedExtra) {
                context.drawText(tr, Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xC0C8D8))), panelX + PADDING, cursorY, 0xFFC0C8D8, true);
                cursorY += lineHeight;
            }
        }

        // --- Footer dots ---
        simplybows$drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8);

        context.getMatrices().pop();
    }

    // --- Helper: effect translation keys (mirrors BowTooltipHelper logic) ---

    @Unique
    private static String simplybows$getStringEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine", "earth", "echo", "ice" -> "tooltip.simplybows.bow." + bowKey + ".string_effect";
            default -> "tooltip.simplybows.bow.generic.string_effect";
        };
    }

    @Unique
    private static String simplybows$getFrameEffectKey(String bowKey) {
        return switch (bowKey) {
            case "vine", "earth", "echo", "ice" -> "tooltip.simplybows.bow." + bowKey + ".frame_effect";
            default -> "tooltip.simplybows.bow.generic.frame_effect";
        };
    }

    @Unique
    private static String simplybows$getRuneEffectKey(String bowKey, RuneEtching rune) {
        return switch (bowKey) {
            case "vine", "earth", "ice", "echo" -> "tooltip.simplybows.bow." + bowKey + ".rune." + rune.id();
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
    private static void simplybows$drawGradientBackground(DrawContext context, int x, int y, int w, int h) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0.0F : (float) i / (float) (h - 1);
            int row = simplybows$lerpColor(COLOR_BG_TOP, COLOR_BG_BOTTOM, t);
            context.fill(x, y + i, x + w, y + i + 1, row);
        }
    }

    @Unique
    private static void simplybows$drawDecorativeBorder(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + 1, COLOR_BORDER);
        context.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        context.fill(x, y, x + 1, y + h, COLOR_BORDER);
        context.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);

        context.fill(x + 1, y + 1, x + w - 1, y + 2, COLOR_BORDER_INNER);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, COLOR_BORDER_INNER);
        context.fill(x + 1, y + 1, x + 2, y + h - 1, COLOR_BORDER_INNER);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, COLOR_BORDER_INNER);

        simplybows$drawSmallDiamond(context, x + 6, y, COLOR_BORDER);
        simplybows$drawSmallDiamond(context, x + w - 7, y, COLOR_BORDER);
        simplybows$drawSmallDiamond(context, x + 6, y + h - 1, COLOR_BORDER);
        simplybows$drawSmallDiamond(context, x + w - 7, y + h - 1, COLOR_BORDER);
    }

    @Unique
    private static void simplybows$drawSmallDiamond(DrawContext context, int cx, int cy, int color) {
        context.fill(cx, cy - 1, cx + 1, cy, color);
        context.fill(cx - 1, cy, cx + 2, cy + 1, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    @Unique
    private static void simplybows$drawSeparator(DrawContext context, int x, int y, int width) {
        int lineY = y + 4;
        int midX = x + width / 2;

        context.fill(x + 4, lineY, midX - 5, lineY + 1, COLOR_SEPARATOR);
        context.fill(midX + 5, lineY, x + width - 4, lineY + 1, COLOR_SEPARATOR);
        simplybows$drawSmallDiamond(context, midX, lineY, COLOR_BORDER);
    }

    @Unique
    private static void simplybows$drawDiamondFrame(DrawContext context, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 2;

        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, COLOR_DIAMOND_FRAME_INNER);
        }

        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx - span + 1, cy + dy + 1, COLOR_DIAMOND_FRAME);
            context.fill(cx + span, cy + dy, cx + span + 1, cy + dy + 1, COLOR_DIAMOND_FRAME);
        }
        context.fill(cx, cy - half, cx + 1, cy - half + 1, COLOR_DIAMOND_FRAME);
        context.fill(cx, cy + half, cx + 1, cy + half + 1, COLOR_DIAMOND_FRAME);
    }

    @Unique
    private static int simplybows$drawBadge(DrawContext context, TextRenderer tr, String label, int x, int y) {
        int textW = tr.getWidth(label);
        int badgePadH = 3;
        int badgeH = tr.fontHeight + 1;

        int badgeW = textW + badgePadH * 2;

        // White badge background
        context.fill(x, y, x + badgeW, y + badgeH, COLOR_BADGE_BG);

        // Dark cutout text — vertically centered (1px top offset for MC font baseline)
        int textY = y + 1;
        context.drawText(tr, Text.literal(label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_BADGE_CUTOUT & 0x00FFFFFF))), x + badgePadH, textY, COLOR_BADGE_CUTOUT, false);

        return x + badgeW;
    }

    @Unique
    private static void simplybows$drawFooterDots(DrawContext context, int cx, int y) {
        simplybows$drawSmallDiamond(context, cx - 8, y, COLOR_FOOTER_DOT);
        simplybows$drawSmallDiamond(context, cx, y, COLOR_FOOTER_DOT);
        simplybows$drawSmallDiamond(context, cx + 8, y, COLOR_FOOTER_DOT);
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
