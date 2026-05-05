package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.SyntaxTheme;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.TooltipHelper;
import com.drakou111.neohouse.UIOpacity;
import com.drakou111.neohouse.UITheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    private static final int PAD     = 10;
    private static final int HEADER_H = 28;
    private static final int ROW_H    = 24;
    private static final int LABEL_W  = 220;
    private static final int BOX_W    = 60;
    private static final int BOX_H    = 14;

    private static final String[] INT_KEYS    = { "clickDelay" };
    private static final String[] INT_LABELS  = { "Delay after click (ticks):" };
    private static final String[] INT_TIPS    = { "How long to wait after each UI click (in game ticks). Lower = faster but may miss clicks." };

    private static final String[] BOOL_KEYS   = { "showArgumentHints", "showArgumentRecommendations", "autoIndent", "overrideExisting" };
    private static final String[] BOOL_LABELS = { "Show argument hints:", "Show argument recommendations:", "Auto-indent on Enter:", "Override existing on run:" };
    private static final String[] BOOL_TIPS   = {
        "Show the expected argument types when writing inside function calls.",
        "Show autocomplete suggestions for string argument values.",
        "Automatically match the indentation of the previous line on Enter.",
        "When running a script, recreate existing functions, events, commands and menus from scratch. Disable to skip items that already exist.",
    };

    private final EditBox[]  intBoxes  = new EditBox[INT_KEYS.length];
    private final Checkbox[] boolBtns  = new Checkbox[BOOL_KEYS.length];
    private SyntaxTheme activeTheme;
    private UITheme     activeUITheme;
    private UIOpacity   activeOpacity;
    private Button      themePrev, themeNext;
    private Button      uiThemePrev, uiThemeNext;
    private Button      opacityPrev, opacityNext;

    private static final int PREVIEW_H = 80;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("NeoHouse Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        int widgetX = PAD + LABEL_W;
        int row = 0;

        for (int i = 0; i < INT_KEYS.length; i++, row++) {
            int boxY = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2;
            EditBox box = new EditBox(font, widgetX, boxY, BOX_W, BOX_H, Component.empty());
            box.setMaxLength(5);
            box.setValue(switch (INT_KEYS[i]) {
                case "clickDelay" -> String.valueOf(cfg.clickDelay);
                default -> "";
            });
            intBoxes[i] = box;
            addRenderableWidget(box);
        }

        for (int i = 0; i < BOOL_KEYS.length; i++, row++) {
            boolean initial = switch (BOOL_KEYS[i]) {
                case "showArgumentHints"           -> cfg.showArgumentHints;
                case "showArgumentRecommendations" -> cfg.showArgumentRecommendations;
                case "autoIndent"                  -> cfg.autoIndent;
                case "overrideExisting"            -> cfg.overrideExisting;
                default -> true;
            };
            int cbY = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2;
            boolBtns[i] = addRenderableWidget(Checkbox.builder(Component.empty(), font)
                    .pos(widgetX, cbY)
                    .selected(initial)
                    .build());
        }

        // Appearance section
        activeTheme   = SyntaxTheme.byId(cfg.syntaxTheme);
        activeUITheme = UITheme.byId(cfg.uiTheme);
        activeOpacity = UIOpacity.byId(cfg.uiOpacity);

        int arrowR = widgetX + 16 + 80 + 4;

        int themeRowY   = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2; row++;
        int uiThemeRowY = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2; row++;
        int opacityRowY = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2;

        themePrev   = addRenderableWidget(Button.builder(Component.literal("<"), b -> activeTheme   = activeTheme.prev()  ).bounds(widgetX, themeRowY,   16, BOX_H).build());
        themeNext   = addRenderableWidget(Button.builder(Component.literal(">"), b -> activeTheme   = activeTheme.next()  ).bounds(arrowR,  themeRowY,   16, BOX_H).build());
        uiThemePrev = addRenderableWidget(Button.builder(Component.literal("<"), b -> activeUITheme = activeUITheme.prev()).bounds(widgetX, uiThemeRowY, 16, BOX_H).build());
        uiThemeNext = addRenderableWidget(Button.builder(Component.literal(">"), b -> activeUITheme = activeUITheme.next()).bounds(arrowR,  uiThemeRowY, 16, BOX_H).build());
        opacityPrev = addRenderableWidget(Button.builder(Component.literal("<"), b -> activeOpacity = activeOpacity.prev()).bounds(widgetX, opacityRowY, 16, BOX_H).build());
        opacityNext = addRenderableWidget(Button.builder(Component.literal(">"), b -> activeOpacity = activeOpacity.next()).bounds(arrowR,  opacityRowY, 16, BOX_H).build());

        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> closeAndSave())
                .bounds(PAD, 5, 52, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> closeAndSave())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());
    }

    @Override
    public void onClose() { closeAndSave(); }

    private void closeAndSave() {
        save();
        minecraft.setScreen(parent);
    }

    private void save() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        for (int i = 0; i < INT_KEYS.length; i++) {
            try {
                int val = Integer.parseInt(intBoxes[i].getValue().trim());
                if (val < 0) val = 0;
                if (INT_KEYS[i].equals("clickDelay")) cfg.clickDelay = val;
            } catch (NumberFormatException ignored) {}
        }
        for (int i = 0; i < BOOL_KEYS.length; i++) {
            switch (BOOL_KEYS[i]) {
                case "showArgumentHints"           -> cfg.showArgumentHints          = boolBtns[i].selected();
                case "showArgumentRecommendations" -> cfg.showArgumentRecommendations = boolBtns[i].selected();
                case "autoIndent"                  -> cfg.autoIndent                  = boolBtns[i].selected();
                case "overrideExisting"            -> cfg.overrideExisting            = boolBtns[i].selected();
            }
        }
        cfg.syntaxTheme = activeTheme.name();
        cfg.uiTheme     = activeUITheme.name();
        cfg.uiOpacity   = activeOpacity.name();
        cfg.save();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t = activeUITheme;
        UIOpacity o = activeOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        g.fill(0, 0, width, HEADER_H, t.panel(o.panelAlpha));
        g.drawString(font, "NeoHouse — Config", PAD + 56, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, width, HEADER_H + 1, t.accent());

        int row = 0;
        String hoveredTip = null;

        for (int i = 0; i < INT_KEYS.length; i++, row++) {
            int labelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2;
            g.drawString(font, INT_LABELS[i], PAD, labelY, 0xFFCCCCCC);
            if (mouseX >= PAD && mouseX < PAD + LABEL_W && mouseY >= labelY - 2 && mouseY < labelY + 11)
                hoveredTip = INT_TIPS[i];
        }

        // Section divider
        int divY = HEADER_H + 6 + row * ROW_H - 2;
        g.fill(PAD, divY, width - PAD, divY + 1, t.divider());

        for (int i = 0; i < BOOL_KEYS.length; i++, row++) {
            int labelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2;
            g.drawString(font, BOOL_LABELS[i], PAD, labelY, 0xFFCCCCCC);
            if (mouseX >= PAD && mouseX < PAD + LABEL_W && mouseY >= labelY - 2 && mouseY < labelY + 11)
                hoveredTip = BOOL_TIPS[i];
        }

        // Section divider before appearance
        int divY2 = HEADER_H + 6 + row * ROW_H - 2;
        g.fill(PAD, divY2, width - PAD, divY2 + 1, t.divider());

        int nameX = (PAD + LABEL_W) + 16 + 4;

        int syntaxThemeLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "Syntax theme:", PAD, syntaxThemeLabelY, 0xFFCCCCCC);
        g.drawCenteredString(font, activeTheme.displayName, nameX + 36, syntaxThemeLabelY, 0xFFFFFFFF);

        int uiThemeLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "UI theme:", PAD, uiThemeLabelY, 0xFFCCCCCC);
        g.drawCenteredString(font, activeUITheme.displayName, nameX + 36, uiThemeLabelY, 0xFFFFFFFF);

        int opacityLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "UI opacity:", PAD, opacityLabelY, 0xFFCCCCCC);
        g.drawCenteredString(font, activeOpacity.displayName, nameX + 36, opacityLabelY, 0xFFFFFFFF);

        // Syntax theme preview
        int previewY = HEADER_H + 6 + row * ROW_H + 4;
        int previewW = Math.min(320, width - 2 * PAD);
        renderPreview(g, PAD, previewY, previewW);

        super.render(g, mouseX, mouseY, partialTick);

        if (hoveredTip != null) {
            TooltipHelper.render(g, font, hoveredTip, mouseX, mouseY);
        }
    }

    private void renderPreview(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + PREVIEW_H, 0xFF1A1A1A);
        RenderHelper.renderOutline(g, x, y, w, PREVIEW_H, activeUITheme.accent());

        int lineY   = y + 7;
        int spacing = 10;
        int left    = x + 8;

        drawCodeLine(g, left, lineY, activeTheme.comment, "// Comment");
        lineY += spacing;
        drawCodeLine(g, left, lineY, activeTheme.keyword, "function ", activeTheme.plain, "greet() {");
        lineY += spacing;
        drawCodeLine(g, left + 8, lineY, activeTheme.keyword, "if ", activeTheme.plain, "(", activeTheme.playerVar, "p", activeTheme.plain, ".", activeTheme.playerVar, "hp ", activeTheme.plain, "<= ", activeTheme.number, "10", activeTheme.plain, ") {");
        lineY += spacing;
        drawCodeLine(g, left + 16, lineY, activeTheme.action, "message", activeTheme.plain, "(", activeTheme.string, "\"Low HP!\"", activeTheme.plain, ");");
        lineY += spacing;
        drawCodeLine(g, left + 8, lineY, activeTheme.plain, "}");
        lineY += spacing;
        drawCodeLine(g, left + 8, lineY, activeTheme.globalVar, "g", activeTheme.plain, ".", activeTheme.globalVar, "score", activeTheme.plain, " += ", activeTheme.number, "1", activeTheme.plain, ";");
        lineY += spacing;
        drawCodeLine(g, left, lineY, activeTheme.plain, "}");
    }

    private void drawCodeLine(GuiGraphics g, int x, int y, Object... parts) {
        int cx = x;
        for (int i = 0; i < parts.length; i += 2) {
            int color = (int) parts[i];
            String text = (String) parts[i + 1];
            g.drawString(font, text, cx, y, color);
            cx += font.width(text);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
