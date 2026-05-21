package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.SyntaxTheme;
import com.drakou111.neohouse.UIOpacity;
import com.drakou111.neohouse.UITheme;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compat.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfigScreen extends ScreenBase {

    private static final int PAD      = 10;
    private static final int HEADER_H = 28;
    private static final int ROW_H    = 24;
    private static final int LABEL_W  = 220;
    private static final int BOX_W    = 60;
    private static final int BOX_H    = 14;

    private static final String[] INT_KEYS    = { "clickDelay", "actionTimeoutTicks" };
    private static final String[] INT_LABELS  = { "Delay after click (ticks):", "Action timeout (ticks):" };
    private static final String[] INT_TIPS    = {
        "How long to wait after each UI click (in game ticks). Lower = faster but may miss clicks.",
        "How many ticks to wait for a screen or anvil to respond before timing out. Increase on laggy servers (default: 200 = 10s).",
    };

    private static final String[] BOOL_KEYS   = { "showArgumentHints", "showArgumentRecommendations", "autoIndent", "overrideExisting" };
    private static final String[] BOOL_LABELS = { "Show argument hints:", "Show argument recommendations:", "Auto-indent on Enter:", "Override existing on run:" };
    private static final String[] BOOL_TIPS   = {
        "Show the expected argument types when writing inside function calls.",
        "Show autocomplete suggestions for string argument values.",
        "Automatically match the indentation of the previous line on Enter.",
        "When running a script, recreate existing functions, events, commands and menus from scratch. Disable to skip items that already exist.",
    };

    private final EditBox[]  intBoxes = new EditBox[INT_KEYS.length];
    private final Checkbox[] boolBtns = new Checkbox[BOOL_KEYS.length];
    private SyntaxTheme activeTheme;
    private UITheme     activeUITheme;
    private UIOpacity   activeOpacity;

    private static final int DROP_NONE    = -1;
    private static final int DROP_SYNTAX  =  0;
    private static final int DROP_UITHEME =  1;
    private int openDropdown     = DROP_NONE;
    private int dropScrollSyntax  = 0;
    private int dropScrollUITheme = 0;

    private int widgetX;
    private static final int DROP_W        = 130;
    private static final int DROP_H        = BOX_H;
    private static final int DROP_ITEM_H   = 12;
    private static final int DROP_MAX_ROWS = 8;
    private int syntaxDropY;
    private int uiThemeDropY;

    private int      sliderX, sliderY, sliderW;
    private boolean  sliderDragging = false;
    private static final int SLIDER_H = 8;
    private static final int KNOB_W   = 8;
    private static final int KNOB_H   = SLIDER_H;

    private static final int PREVIEW_H = 80;
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("NeoHouse Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        widgetX = PAD + LABEL_W;
        int row = 0;

        for (int i = 0; i < INT_KEYS.length; i++, row++) {
            int boxY = HEADER_H + 6 + row * ROW_H + (ROW_H - BOX_H) / 2;
            EditBox box = new EditBox(font, widgetX, boxY, BOX_W, BOX_H, Component.empty());
            box.setMaxLength(5);
            box.setValue(switch (INT_KEYS[i]) {
                case "clickDelay"         -> String.valueOf(cfg.clickDelay);
                case "actionTimeoutTicks" -> String.valueOf(cfg.actionTimeoutTicks);
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

        activeTheme   = SyntaxTheme.byId(cfg.syntaxTheme);
        activeUITheme = UITheme.byId(cfg.uiTheme);
        activeOpacity = UIOpacity.byId(cfg.uiOpacity);

        syntaxDropY  = HEADER_H + 6 + row * ROW_H + (ROW_H - DROP_H) / 2; row++;
        uiThemeDropY = HEADER_H + 6 + row * ROW_H + (ROW_H - DROP_H) / 2; row++;

        sliderX = widgetX;
        sliderW = DROP_W;
        sliderY = HEADER_H + 6 + row * ROW_H + (ROW_H - SLIDER_H) / 2;

        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> closeAndSave())
                .bounds(PAD, 5, 52, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> closeAndSave())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        int x = (int) mx, y = (int) my;

        if (openDropdown != DROP_NONE) {
            if (openDropdown == DROP_SYNTAX  && clickInList(x, y, DROP_SYNTAX))  return true;
            if (openDropdown == DROP_UITHEME && clickInList(x, y, DROP_UITHEME)) return true;
            openDropdown = DROP_NONE;
        }

        if (hit(x, y, widgetX, syntaxDropY, DROP_W, DROP_H)) {
            openDropdown = DROP_SYNTAX;
            dropScrollSyntax = centeredScroll(activeTheme.ordinal(), SyntaxTheme.values().length);
            return true;
        }
        if (hit(x, y, widgetX, uiThemeDropY, DROP_W, DROP_H)) {
            openDropdown = DROP_UITHEME;
            dropScrollUITheme = centeredScroll(activeUITheme.ordinal(), UITheme.values().length);
            return true;
        }

        if (hit(x, y, sliderX, sliderY - 4, sliderW, SLIDER_H + 8)) {
            applySlider(x);
            sliderDragging = true;
            return true;
        }

        return superMouseClicked(mx, my, button);
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (sliderDragging) { applySlider((int) mx); return true; }
        return superMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int button) {
        sliderDragging = false;
        return superMouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int x = (int) mx, y = (int) my;
        if (openDropdown == DROP_SYNTAX) {
            int listY = syntaxDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, SyntaxTheme.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, SyntaxTheme.values().length - DROP_MAX_ROWS);
                dropScrollSyntax = Math.max(0, Math.min(max, dropScrollSyntax - (int) Math.signum(dy)));
                return true;
            }
        }
        if (openDropdown == DROP_UITHEME) {
            int listY = uiThemeDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, UITheme.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, UITheme.values().length - DROP_MAX_ROWS);
                dropScrollUITheme = Math.max(0, Math.min(max, dropScrollUITheme - (int) Math.signum(dy)));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private boolean clickInList(int x, int y, int dropId) {
        int listY    = (dropId == DROP_SYNTAX ? syntaxDropY : uiThemeDropY) + DROP_H;
        int total    = dropId == DROP_SYNTAX ? SyntaxTheme.values().length : UITheme.values().length;
        int visible  = Math.min(DROP_MAX_ROWS, total);
        int listH    = visible * DROP_ITEM_H + 4;
        if (!hit(x, y, widgetX, listY, DROP_W, listH)) return false;

        int scroll  = dropId == DROP_SYNTAX ? dropScrollSyntax : dropScrollUITheme;
        int itemIdx = (y - listY - 2) / DROP_ITEM_H + scroll;
        if (itemIdx >= 0 && itemIdx < total) {
            if (dropId == DROP_SYNTAX)  activeTheme   = SyntaxTheme.values()[itemIdx];
            else                        activeUITheme = UITheme.values()[itemIdx];
            openDropdown = DROP_NONE;
        }
        return true;
    }

    private void applySlider(int mx) {
        UIOpacity[] vals = UIOpacity.values();
        float t = Math.max(0f, Math.min(1f, (float)(mx - sliderX - KNOB_W / 2) / (sliderW - KNOB_W)));
        activeOpacity = vals[Math.round(t * (vals.length - 1))];
    }

    private int centeredScroll(int selectedIdx, int totalCount) {
        return Math.max(0, Math.min(selectedIdx - DROP_MAX_ROWS / 2, totalCount - DROP_MAX_ROWS));
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { closeAndSave(); }

    private void closeAndSave() { save(); minecraft.setScreen(parent); }

    private void save() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        for (int i = 0; i < INT_KEYS.length; i++) {
            try {
                int val = Integer.parseInt(intBoxes[i].getValue().trim());
                if (val < 0) val = 0;
                if (INT_KEYS[i].equals("clickDelay"))         cfg.clickDelay         = val;
                if (INT_KEYS[i].equals("actionTimeoutTicks")) cfg.actionTimeoutTicks = Math.max(20, val);
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
        RenderHelper.renderLogo(g, PAD + 56, 4, 18, t.accent());
        g.drawString(font, "NeoHouse — Config", PAD + 56 + 22, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, width, HEADER_H + 1, t.accent());

        int row = 0;
        String hoveredTip = null;

        for (int i = 0; i < INT_KEYS.length; i++, row++) {
            int labelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2;
            g.drawString(font, INT_LABELS[i], PAD, labelY, 0xFFCCCCCC);
            if (mouseX >= PAD && mouseX < PAD + LABEL_W && mouseY >= labelY - 2 && mouseY < labelY + 11)
                hoveredTip = INT_TIPS[i];
        }

        int divY = HEADER_H + 6 + row * ROW_H - 2;
        g.fill(PAD, divY, width - PAD, divY + 1, t.divider());

        for (int i = 0; i < BOOL_KEYS.length; i++, row++) {
            int labelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2;
            g.drawString(font, BOOL_LABELS[i], PAD, labelY, 0xFFCCCCCC);
            if (mouseX >= PAD && mouseX < PAD + LABEL_W && mouseY >= labelY - 2 && mouseY < labelY + 11)
                hoveredTip = BOOL_TIPS[i];
        }

        int divY2 = HEADER_H + 6 + row * ROW_H - 2;
        g.fill(PAD, divY2, width - PAD, divY2 + 1, t.divider());

        int syntaxLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "Syntax theme:", PAD, syntaxLabelY, 0xFFCCCCCC);
        drawDropBtn(g, DROP_SYNTAX, syntaxDropY, activeTheme.displayName, t, mouseX, mouseY);

        int uiThemeLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "UI theme:", PAD, uiThemeLabelY, 0xFFCCCCCC);
        drawDropBtn(g, DROP_UITHEME, uiThemeDropY, activeUITheme.displayName, t, mouseX, mouseY);

        int opacityLabelY = HEADER_H + 6 + row * ROW_H + (ROW_H - 9) / 2; row++;
        g.drawString(font, "UI opacity:", PAD, opacityLabelY, 0xFFCCCCCC);
        drawOpacitySlider(g, t, mouseX, mouseY);

        int previewY = HEADER_H + 6 + row * ROW_H + 4;
        int previewW = Math.min(320, width - 2 * PAD);
        renderPreview(g, PAD, previewY, previewW);

        super.render(g, mouseX, mouseY, partialTick);

        if (openDropdown == DROP_SYNTAX) {
            String[] names = new String[SyntaxTheme.values().length];
            for (int i = 0; i < names.length; i++) names[i] = SyntaxTheme.values()[i].displayName;
            drawDropList(g, widgetX, syntaxDropY + DROP_H, names, activeTheme.ordinal(), dropScrollSyntax, t, mouseX, mouseY);
        } else if (openDropdown == DROP_UITHEME) {
            String[] names = new String[UITheme.values().length];
            for (int i = 0; i < names.length; i++) names[i] = UITheme.values()[i].displayName;
            drawDropList(g, widgetX, uiThemeDropY + DROP_H, names, activeUITheme.ordinal(), dropScrollUITheme, t, mouseX, mouseY);
        }

        if (hoveredTip != null) {
            TooltipHelper.render(g, font, hoveredTip, mouseX, mouseY);
        }
    }

    private void drawDropBtn(GuiGraphics g, int dropId, int y, String label, UITheme t, int mx, int my) {
        boolean hovered = hit(mx, my, widgetX, y, DROP_W, DROP_H);
        boolean open    = openDropdown == dropId;
        g.fill(widgetX, y, widgetX + DROP_W, y + DROP_H,
               open ? t.highlight(0xFF) : hovered ? t.surface(0xFF) : 0xFF0F0F18);
        RenderHelper.renderOutline(g, widgetX, y, DROP_W, DROP_H, open ? t.accent() : 0xFF444466);
        int maxLabelW = DROP_W - 20;
        String shown = font.width(label) > maxLabelW ? font.plainSubstrByWidth(label, maxLabelW) + "…" : label;
        g.drawString(font, shown,            widgetX + 4,          y + (DROP_H - 8) / 2, 0xFFDDDDDD, false);
        g.drawString(font, open ? "▲" : "▼", widgetX + DROP_W - 11, y + (DROP_H - 8) / 2, 0xFF888899, false);
    }

    private void drawDropList(GuiGraphics g, int x, int y, String[] items, int selectedIdx,
                               int scroll, UITheme t, int mx, int my) {
        int visible = Math.min(DROP_MAX_ROWS, items.length);
        int listH   = visible * DROP_ITEM_H + 4;
        g.fill(x, y, x + DROP_W, y + listH, 0xFF0F0F1A);
        RenderHelper.renderOutline(g, x, y, DROP_W, listH, t.accent());

        for (int i = 0; i < visible; i++) {
            int idx = i + scroll;
            if (idx >= items.length) break;
            int iy      = y + 2 + i * DROP_ITEM_H;
            boolean hov = hit(mx, my, x, iy, DROP_W, DROP_ITEM_H);
            boolean sel = idx == selectedIdx;
            if (sel) g.fill(x, iy, x + DROP_W, iy + DROP_ITEM_H, t.highlight(0xCC));
            else if (hov) g.fill(x, iy, x + DROP_W, iy + DROP_ITEM_H, t.surface(0xCC));
            g.drawString(font, items[idx], x + 4, iy + (DROP_ITEM_H - 8) / 2, sel ? t.accent() : 0xFFDDDDDD, false);
        }

        if (scroll > 0)
            g.drawString(font, "▲", x + DROP_W - 10, y + 2, 0xFFAAAAAA, false);
        if (scroll + visible < items.length)
            g.drawString(font, "▼", x + DROP_W - 10, y + listH - 10, 0xFFAAAAAA, false);
    }

    private void drawOpacitySlider(GuiGraphics g, UITheme t, int mx, int my) {
        UIOpacity[] vals = UIOpacity.values();
        int trackMidY = sliderY + KNOB_H / 2;
        int knobX     = knobX();

        g.fill(sliderX, trackMidY - 1, sliderX + sliderW, trackMidY + 2, 0xFF333344);
        g.fill(sliderX, trackMidY - 1, knobX + KNOB_W / 2, trackMidY + 2, t.accentTint(0xAA));

        for (int i = 0; i < vals.length; i++) {
            int tx = sliderX + i * (sliderW - KNOB_W) / (vals.length - 1) + KNOB_W / 2;
            g.fill(tx, trackMidY - 2, tx + 1, trackMidY + 3, 0xFF666688);
        }

        boolean hov     = hit(mx, my, sliderX, sliderY - 4, sliderW, KNOB_H + 8);
        int     knobCol = (sliderDragging || hov) ? t.accent() : 0xFFCCCCDD;
        g.fill(knobX, sliderY, knobX + KNOB_W, sliderY + KNOB_H, knobCol);
        RenderHelper.renderOutline(g, knobX, sliderY, KNOB_W, KNOB_H, t.accent());

        g.drawString(font, activeOpacity.displayName, sliderX + sliderW + 6,
                     sliderY + (KNOB_H - 8) / 2, 0xFFCCCCCC, false);
    }

    private int knobX() {
        UIOpacity[] vals = UIOpacity.values();
        return sliderX + activeOpacity.ordinal() * (sliderW - KNOB_W) / (vals.length - 1);
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
            int    color = (int) parts[i];
            String text  = (String) parts[i + 1];
            g.drawString(font, text, cx, y, color);
            cx += font.width(text);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
