package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.SyntaxTheme;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;

import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compat.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfigScreen extends ScreenBase {

    private static final int PAD = 10;
    private static final int HEADER_H = 28;
    private static final int TAB_H = 20;
    private static final int TAB_Y = HEADER_H + 4;
    private static final int ROW_H = 24;
    private static final int LABEL_W = 220;
    private static final int BOX_W = 60;
    private static final int BOX_H = 14;

    private static final String[] TAB_NAMES = { "Compiler", "IDE", "UI" };
    private static final int TAB_COMPILER = 0;
    private static final int TAB_IDE = 1;
    private static final int TAB_UI = 2;
    private int activeTab = TAB_COMPILER;

    private static final String[] INT_KEYS   = { "clickDelay", "actionTimeoutTicks", "tabWidth", "errorBarMaxLines" };
    private static final String[] INT_LABELS = { "Delay after click (ticks):", "Action timeout (ticks):", "Tab width (spaces):", "Max error lines:" };
    private static final String[] INT_TIPS   = {
        "How long to wait after each UI click (in game ticks). Lower = faster but might break.",
        "How many ticks to wait for a screen or anvil to respond before timing out. Increase if lagging.",
        "Number of spaces inserted per Tab and used for auto-indent in the code editor (1-16, default: 4).",
        "Maximum number of lines shown in the editor's error bar before it becomes scrollable (1-20, default: 5).",
    };
    private static final int[] INT_TABS = { TAB_COMPILER, TAB_COMPILER, TAB_IDE, TAB_IDE };

    private static final String[] BOOL_KEYS   = { "showArgumentHints", "showArgumentRecommendations", "autoIndent", "showScriptMob" };
    private static final String[] BOOL_LABELS = { "Show argument hints:", "Show argument recommendations:", "Auto-indent on Enter:", "Show mob while running:" };
    private static final String[] BOOL_TIPS   = {
        "Show the expected argument types when writing inside function calls.",
        "Show autocomplete suggestions for string argument values.",
        "Automatically match the indentation of the previous line on Enter.",
        "Display a mob above the progress overlay while a script is running as a companion!",
    };

    private final String[]  intValues  = new String[INT_KEYS.length];
    private final boolean[] boolValues = new boolean[BOOL_KEYS.length];
    private SyntaxTheme activeTheme;
    private UITheme activeUITheme;
    private UIOpacity activeOpacity;
    private OverrideMode activeOverrideMode;
    private MultiCallMode activeMultiCallMode;
    private RecursionMode activeRecursionMode;
    private GlobalCallMode activeGlobalCallMode;

    private final EditBox[]  intBoxes = new EditBox[INT_KEYS.length];
    private final Checkbox[] boolBtns = new Checkbox[BOOL_KEYS.length];

    private static final int DROP_NONE          = -1;
    private static final int DROP_SYNTAX        =  0;
    private static final int DROP_UITHEME       =  1;
    private static final int DROP_MOB           =  2;
    private static final int DROP_OVERRIDE      =  3;
    private static final int DROP_MULTICALL     =  4;
    private static final int DROP_RECURSION     =  5;
    private static final int DROP_GLOBALCALL    =  6;
    private int openDropdown         = DROP_NONE;
    private int dropScrollSyntax     = 0;
    private int dropScrollUITheme    = 0;
    private int dropScrollMob        = 0;
    private int dropScrollOverride   = 0;
    private int dropScrollMultiCall  = 0;
    private int dropScrollRecursion  = 0;
    private int dropScrollGlobalCall = 0;

    private String[] mobIds;
    private String[] mobNames;
    private int selectedMobIdx = 0;

    private int widgetX;
    private static final int DROP_W            = 130;
    private static final int DROP_H            = BOX_H;
    private static final int DROP_ITEM_H       = 12;
    private static final int DROP_MAX_ROWS     = 8;
    private static final int MOB_DROP_MAX_ROWS = 10;
    private int syntaxDropY;
    private int uiThemeDropY;
    private int overrideDropY;
    private int multiCallDropY;
    private int recursionDropY;
    private int globalCallDropY;
    private int mobDropY;

    private int sliderX, sliderY, sliderW;
    private boolean sliderDragging = false;
    private static final int SLIDER_H = 8;
    private static final int KNOB_W   = 8;
    private static final int KNOB_H   = SLIDER_H;

    private final int[] intLabelY  = new int[INT_KEYS.length];
    private final int[] boolLabelY = new int[BOOL_KEYS.length];
    private int overrideLabelY, multiCallLabelY, recursionLabelY, globalCallLabelY;
    private int syntaxLabelY, uiThemeLabelY, opacityLabelY, mobLabelY;
    private int previewY;

    private static final int PREVIEW_H = 80;
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("NeoHouse Config"));
        this.parent = parent;
        loadState();
    }

    private void loadState() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        for (int i = 0; i < INT_KEYS.length; i++) {
            intValues[i] = switch (INT_KEYS[i]) {
                case "clickDelay"         -> String.valueOf(cfg.clickDelay);
                case "actionTimeoutTicks" -> String.valueOf(cfg.actionTimeoutTicks);
                case "tabWidth"           -> String.valueOf(cfg.tabWidth);
                case "errorBarMaxLines"   -> String.valueOf(cfg.errorBarMaxLines);
                default -> "";
            };
        }
        for (int i = 0; i < BOOL_KEYS.length; i++) {
            boolValues[i] = switch (BOOL_KEYS[i]) {
                case "showArgumentHints"           -> cfg.showArgumentHints;
                case "showArgumentRecommendations" -> cfg.showArgumentRecommendations;
                case "autoIndent"                  -> cfg.autoIndent;
                case "showScriptMob"               -> cfg.showScriptMob;
                default -> true;
            };
        }
        activeTheme         = cfg.syntaxTheme;
        activeUITheme       = cfg.uiTheme;
        activeOpacity       = cfg.uiOpacity;
        activeOverrideMode   = cfg.overrideMode;
        activeMultiCallMode  = cfg.multiCallMode;
        activeRecursionMode  = cfg.recursionMode;
        activeGlobalCallMode = cfg.globalCallMode;

        List<EntityType<?>> entityTypes = new ArrayList<>();
        for (EntityType<?> et : BuiltInRegistries.ENTITY_TYPE) {
            if (et.getCategory() != MobCategory.MISC) entityTypes.add(et);
        }
        entityTypes.sort(Comparator.comparing(et -> et.getDescription().getString()));
        mobIds   = new String[entityTypes.size()];
        mobNames = new String[entityTypes.size()];
        String currentMobId = cfg.scriptMobId;
        selectedMobIdx = 0;
        for (int i = 0; i < entityTypes.size(); i++) {
            EntityType<?> et = entityTypes.get(i);
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(et);
            mobIds[i]   = key != null ? key.toString() : "minecraft:pig";
            mobNames[i] = et.getDescription().getString();
            if (mobIds[i].equals(currentMobId)) selectedMobIdx = i;
        }
    }

    @Override
    protected void init() {
        widgetX = PAD + LABEL_W;
        int y = HEADER_H + TAB_H + 10;

        if (activeTab == TAB_COMPILER) {
            overrideDropY   = y + (ROW_H - DROP_H) / 2; overrideLabelY   = y + (ROW_H - 9) / 2; y += ROW_H;
            multiCallDropY  = y + (ROW_H - DROP_H) / 2; multiCallLabelY  = y + (ROW_H - 9) / 2; y += ROW_H;
            recursionDropY  = y + (ROW_H - DROP_H) / 2; recursionLabelY  = y + (ROW_H - 9) / 2; y += ROW_H;
            globalCallDropY = y + (ROW_H - DROP_H) / 2; globalCallLabelY = y + (ROW_H - 9) / 2; y += ROW_H;
            y = addIntBoxesForTab(TAB_COMPILER, y);
        } else if (activeTab == TAB_IDE) {
            for (int i = 0; i < 3; i++) {
                boolLabelY[i] = y + (ROW_H - 9) / 2;
                boolBtns[i] = addRenderableWidget(Checkbox.builder(Component.empty(), font)
                        .pos(widgetX, y + (ROW_H - BOX_H) / 2)
                        .selected(boolValues[i])
                        .build());
                y += ROW_H;
            }
            syntaxDropY = y + (ROW_H - DROP_H) / 2; syntaxLabelY = y + (ROW_H - 9) / 2; y += ROW_H;
            y = addIntBoxesForTab(TAB_IDE, y);
            previewY = y + 4;
        } else { // TAB_UI
            uiThemeDropY = y + (ROW_H - DROP_H) / 2; uiThemeLabelY = y + (ROW_H - 9) / 2; y += ROW_H;
            sliderX = widgetX; sliderW = DROP_W;
            sliderY = y + (ROW_H - SLIDER_H) / 2; opacityLabelY = y + (ROW_H - 9) / 2; y += ROW_H;
            boolLabelY[3] = y + (ROW_H - 9) / 2;
            boolBtns[3] = addRenderableWidget(Checkbox.builder(Component.empty(), font)
                    .pos(widgetX, y + (ROW_H - BOX_H) / 2)
                    .selected(boolValues[3])
                    .build());
            y += ROW_H;
            mobDropY = y + (ROW_H - DROP_H) / 2; mobLabelY = y + (ROW_H - 9) / 2; y += ROW_H;
        }

        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> closeAndSave())
                .bounds(PAD, 5, 52, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> closeAndSave())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());
    }

    private int addIntBoxesForTab(int tab, int y) {
        for (int i = 0; i < INT_KEYS.length; i++) {
            if (INT_TABS[i] != tab) continue;
            intLabelY[i] = y + (ROW_H - 9) / 2;
            EditBox box = new EditBox(font, widgetX, y + (ROW_H - BOX_H) / 2, BOX_W, BOX_H, Component.empty());
            box.setMaxLength(5);
            box.setValue(intValues[i]);
            intBoxes[i] = box;
            addRenderableWidget(box);
            y += ROW_H;
        }
        return y;
    }

    private void captureActiveTab() {
        if (activeTab == TAB_COMPILER) {
            for (int i = 0; i < INT_KEYS.length; i++)
                if (INT_TABS[i] == TAB_COMPILER && intBoxes[i] != null) intValues[i] = intBoxes[i].getValue();
        } else if (activeTab == TAB_IDE) {
            for (int i = 0; i < INT_KEYS.length; i++)
                if (INT_TABS[i] == TAB_IDE && intBoxes[i] != null) intValues[i] = intBoxes[i].getValue();
            for (int i = 0; i < 3; i++)
                if (boolBtns[i] != null) boolValues[i] = boolBtns[i].selected();
        } else if (activeTab == TAB_UI) {
            if (boolBtns[3] != null) boolValues[3] = boolBtns[3].selected();
        }
    }

    private void switchTab(int tab) {
        if (tab == activeTab) return;
        captureActiveTab();
        activeTab    = tab;
        openDropdown = DROP_NONE;
        rebuildWidgets();
    }

    private int tabWidth() { return (width - 2 * PAD) / TAB_NAMES.length; }
    private int tabX(int i) { return PAD + i * tabWidth(); }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        int x = (int) mx, y = (int) my;

        if (openDropdown != DROP_NONE) {
            if (openDropdown == DROP_SYNTAX    && clickInList(x, y, DROP_SYNTAX))    return true;
            if (openDropdown == DROP_UITHEME   && clickInList(x, y, DROP_UITHEME))   return true;
            if (openDropdown == DROP_MOB       && clickInList(x, y, DROP_MOB))       return true;
            if (openDropdown == DROP_OVERRIDE  && clickInList(x, y, DROP_OVERRIDE))  return true;
            if (openDropdown == DROP_MULTICALL && clickInList(x, y, DROP_MULTICALL)) return true;
            if (openDropdown == DROP_RECURSION  && clickInList(x, y, DROP_RECURSION))  return true;
            if (openDropdown == DROP_GLOBALCALL && clickInList(x, y, DROP_GLOBALCALL)) return true;
            openDropdown = DROP_NONE;
        }

        if (y >= TAB_Y && y < TAB_Y + TAB_H) {
            for (int i = 0; i < TAB_NAMES.length; i++) {
                if (x >= tabX(i) && x < tabX(i) + tabWidth()) { switchTab(i); return true; }
            }
        }

        if (activeTab == TAB_COMPILER) {
            if (hit(x, y, widgetX, overrideDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_OVERRIDE;
                dropScrollOverride = centeredScroll(activeOverrideMode.ordinal(), OverrideMode.values().length);
                return true;
            }
            if (hit(x, y, widgetX, multiCallDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_MULTICALL;
                dropScrollMultiCall = centeredScroll(activeMultiCallMode.ordinal(), MultiCallMode.values().length);
                return true;
            }
            if (hit(x, y, widgetX, recursionDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_RECURSION;
                dropScrollRecursion = centeredScroll(activeRecursionMode.ordinal(), RecursionMode.values().length);
                return true;
            }
            if (hit(x, y, widgetX, globalCallDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_GLOBALCALL;
                dropScrollGlobalCall = centeredScroll(activeGlobalCallMode.ordinal(), GlobalCallMode.values().length);
                return true;
            }
        } else if (activeTab == TAB_IDE) {
            if (hit(x, y, widgetX, syntaxDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_SYNTAX;
                dropScrollSyntax = centeredScroll(activeTheme.ordinal(), SyntaxTheme.values().length);
                return true;
            }
        } else if (activeTab == TAB_UI) {
            if (hit(x, y, widgetX, uiThemeDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_UITHEME;
                dropScrollUITheme = centeredScroll(activeUITheme.ordinal(), UITheme.values().length);
                return true;
            }
            if (hit(x, y, widgetX, mobDropY, DROP_W, DROP_H)) {
                openDropdown = DROP_MOB;
                dropScrollMob = centeredScroll(selectedMobIdx, mobIds.length);
                return true;
            }
            if (hit(x, y, sliderX, sliderY - 4, sliderW, SLIDER_H + 8)) {
                applySlider(x);
                sliderDragging = true;
                return true;
            }
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
        if (openDropdown == DROP_OVERRIDE) {
            int listY = overrideDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, OverrideMode.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, OverrideMode.values().length - DROP_MAX_ROWS);
                dropScrollOverride = Math.max(0, Math.min(max, dropScrollOverride - (int) Math.signum(dy)));
                return true;
            }
        }
        if (openDropdown == DROP_MULTICALL) {
            int listY = multiCallDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, MultiCallMode.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, MultiCallMode.values().length - DROP_MAX_ROWS);
                dropScrollMultiCall = Math.max(0, Math.min(max, dropScrollMultiCall - (int) Math.signum(dy)));
                return true;
            }
        }
        if (openDropdown == DROP_RECURSION) {
            int listY = recursionDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, RecursionMode.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, RecursionMode.values().length - DROP_MAX_ROWS);
                dropScrollRecursion = Math.max(0, Math.min(max, dropScrollRecursion - (int) Math.signum(dy)));
                return true;
            }
        }
        if (openDropdown == DROP_GLOBALCALL) {
            int listY = globalCallDropY + DROP_H;
            int listH = Math.min(DROP_MAX_ROWS, GlobalCallMode.values().length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, GlobalCallMode.values().length - DROP_MAX_ROWS);
                dropScrollGlobalCall = Math.max(0, Math.min(max, dropScrollGlobalCall - (int) Math.signum(dy)));
                return true;
            }
        }
        if (openDropdown == DROP_MOB && mobIds != null) {
            int listY = mobDropY + DROP_H;
            int listH = Math.min(MOB_DROP_MAX_ROWS, mobIds.length) * DROP_ITEM_H + 4;
            if (hit(x, y, widgetX, listY, DROP_W, listH)) {
                int max = Math.max(0, mobIds.length - MOB_DROP_MAX_ROWS);
                dropScrollMob = Math.max(0, Math.min(max, dropScrollMob - (int) Math.signum(dy)));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private boolean clickInList(int x, int y, int dropId) {
        int listY, total, scroll, maxRows;
        if (dropId == DROP_SYNTAX) {
            listY = syntaxDropY + DROP_H; total = SyntaxTheme.values().length;
            scroll = dropScrollSyntax; maxRows = DROP_MAX_ROWS;
        } else if (dropId == DROP_UITHEME) {
            listY = uiThemeDropY + DROP_H; total = UITheme.values().length;
            scroll = dropScrollUITheme; maxRows = DROP_MAX_ROWS;
        } else if (dropId == DROP_OVERRIDE) {
            listY = overrideDropY + DROP_H; total = OverrideMode.values().length;
            scroll = dropScrollOverride; maxRows = DROP_MAX_ROWS;
        } else if (dropId == DROP_MULTICALL) {
            listY = multiCallDropY + DROP_H; total = MultiCallMode.values().length;
            scroll = dropScrollMultiCall; maxRows = DROP_MAX_ROWS;
        } else if (dropId == DROP_RECURSION) {
            listY = recursionDropY + DROP_H; total = RecursionMode.values().length;
            scroll = dropScrollRecursion; maxRows = DROP_MAX_ROWS;
        } else if (dropId == DROP_GLOBALCALL) {
            listY = globalCallDropY + DROP_H; total = GlobalCallMode.values().length;
            scroll = dropScrollGlobalCall; maxRows = DROP_MAX_ROWS;
        } else {
            listY = mobDropY + DROP_H; total = mobIds != null ? mobIds.length : 0;
            scroll = dropScrollMob; maxRows = MOB_DROP_MAX_ROWS;
        }
        int visible = Math.min(maxRows, total);
        int listH   = visible * DROP_ITEM_H + 4;
        if (!hit(x, y, widgetX, listY, DROP_W, listH)) return false;

        int itemIdx = (y - listY - 2) / DROP_ITEM_H + scroll;
        if (itemIdx >= 0 && itemIdx < total) {
            if      (dropId == DROP_SYNTAX)    activeTheme         = SyntaxTheme.values()[itemIdx];
            else if (dropId == DROP_UITHEME)   activeUITheme       = UITheme.values()[itemIdx];
            else if (dropId == DROP_OVERRIDE)  activeOverrideMode  = OverrideMode.values()[itemIdx];
            else if (dropId == DROP_MULTICALL) activeMultiCallMode = MultiCallMode.values()[itemIdx];
            else if (dropId == DROP_RECURSION)  activeRecursionMode  = RecursionMode.values()[itemIdx];
            else if (dropId == DROP_GLOBALCALL) activeGlobalCallMode = GlobalCallMode.values()[itemIdx];
            else                               selectedMobIdx      = itemIdx;
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
    public void onClose() { save(); minecraft.setScreen(null); }

    private void closeAndSave() { save(); minecraft.setScreen(parent); }

    private void save() {
        captureActiveTab();
        NeoHouseConfig cfg = NeoHouseConfig.get();
        for (int i = 0; i < INT_KEYS.length; i++) {
            try {
                int val = Integer.parseInt(intValues[i].trim());
                if (val < 0) val = 0;
                if (INT_KEYS[i].equals("clickDelay"))         cfg.clickDelay         = val;
                if (INT_KEYS[i].equals("actionTimeoutTicks")) cfg.actionTimeoutTicks = Math.max(20, val);
                if (INT_KEYS[i].equals("tabWidth"))           cfg.tabWidth           = Math.max(1, Math.min(16, val));
                if (INT_KEYS[i].equals("errorBarMaxLines"))   cfg.errorBarMaxLines   = Math.max(1, Math.min(20, val));
            } catch (NumberFormatException ignored) {}
        }
        for (int i = 0; i < BOOL_KEYS.length; i++) {
            switch (BOOL_KEYS[i]) {
                case "showArgumentHints"           -> cfg.showArgumentHints           = boolValues[i];
                case "showArgumentRecommendations" -> cfg.showArgumentRecommendations = boolValues[i];
                case "autoIndent"                  -> cfg.autoIndent                  = boolValues[i];
                case "showScriptMob"               -> cfg.showScriptMob               = boolValues[i];
            }
        }
        cfg.overrideMode    = activeOverrideMode;
        cfg.multiCallMode   = activeMultiCallMode;
        cfg.recursionMode   = activeRecursionMode;
        cfg.globalCallMode  = activeGlobalCallMode;
        if (mobIds != null && selectedMobIdx >= 0 && selectedMobIdx < mobIds.length)
            cfg.scriptMobId = mobIds[selectedMobIdx];
        cfg.syntaxTheme = activeTheme;
        cfg.uiTheme     = activeUITheme;
        cfg.uiOpacity   = activeOpacity;
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

        drawTabs(g, t, mouseX, mouseY);

        String hoveredTip = null;

        if (activeTab == TAB_COMPILER) {
            g.drawString(font, "Override on run:", PAD, overrideLabelY, 0xFFCCCCCC);
            if (hit(mouseX, mouseY, PAD, overrideLabelY - 2, LABEL_W, 13))
                hoveredTip = "Always Override: recreate all functions/events/commands/menus from scratch. Never Override: skip everything that already exists. Skip Const: only skip items marked with the 'const' keyword.";
            drawDropBtn(g, DROP_OVERRIDE, overrideDropY, activeOverrideMode.displayName, t, mouseX, mouseY);

            g.drawString(font, "Multi-call mode:", PAD, multiCallLabelY, 0xFFCCCCCC);
            if (hit(mouseX, mouseY, PAD, multiCallLabelY - 2, LABEL_W, 13))
                hoveredTip = "Off: no check. Warn: error if a function is called twice in one context. Fix: auto-create copies ($0, $1, ...) so each call uses a unique function name.";
            drawDropBtn(g, DROP_MULTICALL, multiCallDropY, activeMultiCallMode.displayName, t, mouseX, mouseY);

            g.drawString(font, "Recursion:", PAD, recursionLabelY, 0xFFCCCCCC);
            if (hit(mouseX, mouseY, PAD, recursionLabelY - 2, LABEL_W, 13))
                hoveredTip = "How to handle functions that call themselves directly or indirectly. Off: allowed silently. Warn: allowed but reported. Error: compilation fails. Hypixel Housing does not officially support recursive functions.";
            drawDropBtn(g, DROP_RECURSION, recursionDropY, activeRecursionMode.displayName, t, mouseX, mouseY);

            g.drawString(font, "Global call rules:", PAD, globalCallLabelY, 0xFFCCCCCC);
            if (hit(mouseX, mouseY, PAD, globalCallLabelY - 2, LABEL_W, 13))
                hoveredTip = "How to handle a globally-called function (or loop) that itself contains 'global ...' calls. Off: allowed silently. Warn: allowed but reported. Error: compilation fails.";
            drawDropBtn(g, DROP_GLOBALCALL, globalCallDropY, activeGlobalCallMode.displayName, t, mouseX, mouseY);

            for (int i = 0; i < INT_KEYS.length; i++) {
                if (INT_TABS[i] != TAB_COMPILER) continue;
                g.drawString(font, INT_LABELS[i], PAD, intLabelY[i], 0xFFCCCCCC);
                if (hit(mouseX, mouseY, PAD, intLabelY[i] - 2, LABEL_W, 13))
                    hoveredTip = INT_TIPS[i];
            }
        } else if (activeTab == TAB_IDE) {
            for (int i = 0; i < 3; i++) {
                g.drawString(font, BOOL_LABELS[i], PAD, boolLabelY[i], 0xFFCCCCCC);
                if (hit(mouseX, mouseY, PAD, boolLabelY[i] - 2, LABEL_W, 13))
                    hoveredTip = BOOL_TIPS[i];
            }
            g.drawString(font, "Syntax theme:", PAD, syntaxLabelY, 0xFFCCCCCC);
            drawDropBtn(g, DROP_SYNTAX, syntaxDropY, activeTheme.displayName, t, mouseX, mouseY);
            for (int i = 0; i < INT_KEYS.length; i++) {
                if (INT_TABS[i] != TAB_IDE) continue;
                g.drawString(font, INT_LABELS[i], PAD, intLabelY[i], 0xFFCCCCCC);
                if (hit(mouseX, mouseY, PAD, intLabelY[i] - 2, LABEL_W, 13))
                    hoveredTip = INT_TIPS[i];
            }
            int previewW = Math.min(320, width - 2 * PAD);
            renderPreview(g, PAD, previewY, previewW);
        } else if (activeTab == TAB_UI) {
            g.drawString(font, "UI theme:", PAD, uiThemeLabelY, 0xFFCCCCCC);
            drawDropBtn(g, DROP_UITHEME, uiThemeDropY, activeUITheme.displayName, t, mouseX, mouseY);

            g.drawString(font, "UI opacity:", PAD, opacityLabelY, 0xFFCCCCCC);
            drawOpacitySlider(g, t, mouseX, mouseY);

            g.drawString(font, BOOL_LABELS[3], PAD, boolLabelY[3], 0xFFCCCCCC);
            if (hit(mouseX, mouseY, PAD, boolLabelY[3] - 2, LABEL_W, 13))
                hoveredTip = BOOL_TIPS[3];

            g.drawString(font, "Script mob:", PAD, mobLabelY, 0xFFCCCCCC);
            String mobLabel = (mobNames != null && selectedMobIdx < mobNames.length) ? mobNames[selectedMobIdx] : "Fox";
            drawDropBtn(g, DROP_MOB, mobDropY, mobLabel, t, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (openDropdown == DROP_SYNTAX) {
            String[] names = new String[SyntaxTheme.values().length];
            for (int i = 0; i < names.length; i++) names[i] = SyntaxTheme.values()[i].displayName;
            drawDropList(g, widgetX, syntaxDropY + DROP_H, names, activeTheme.ordinal(), dropScrollSyntax, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_UITHEME) {
            String[] names = new String[UITheme.values().length];
            for (int i = 0; i < names.length; i++) names[i] = UITheme.values()[i].displayName;
            drawDropList(g, widgetX, uiThemeDropY + DROP_H, names, activeUITheme.ordinal(), dropScrollUITheme, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_OVERRIDE) {
            String[] names = new String[OverrideMode.values().length];
            for (int i = 0; i < names.length; i++) names[i] = OverrideMode.values()[i].displayName;
            drawDropList(g, widgetX, overrideDropY + DROP_H, names, activeOverrideMode.ordinal(), dropScrollOverride, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_MULTICALL) {
            String[] names = new String[MultiCallMode.values().length];
            for (int i = 0; i < names.length; i++) names[i] = MultiCallMode.values()[i].displayName;
            drawDropList(g, widgetX, multiCallDropY + DROP_H, names, activeMultiCallMode.ordinal(), dropScrollMultiCall, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_RECURSION) {
            String[] names = new String[RecursionMode.values().length];
            for (int i = 0; i < names.length; i++) names[i] = RecursionMode.values()[i].displayName;
            drawDropList(g, widgetX, recursionDropY + DROP_H, names, activeRecursionMode.ordinal(), dropScrollRecursion, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_GLOBALCALL) {
            String[] names = new String[GlobalCallMode.values().length];
            for (int i = 0; i < names.length; i++) names[i] = GlobalCallMode.values()[i].displayName;
            drawDropList(g, widgetX, globalCallDropY + DROP_H, names, activeGlobalCallMode.ordinal(), dropScrollGlobalCall, DROP_MAX_ROWS, t, mouseX, mouseY);
        } else if (openDropdown == DROP_MOB && mobNames != null) {
            drawDropList(g, widgetX, mobDropY + DROP_H, mobNames, selectedMobIdx, dropScrollMob, MOB_DROP_MAX_ROWS, t, mouseX, mouseY);
        }

        if (hoveredTip != null) {
            TooltipHelper.render(g, font, hoveredTip, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics g, UITheme t, int mx, int my) {
        int tw = tabWidth();
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int x = tabX(i);
            boolean active = i == activeTab;
            boolean hov    = hit(mx, my, x, TAB_Y, tw, TAB_H);
            int bg = active ? t.surface(0xFF) : hov ? t.highlight(0x66) : 0x33000000;
            g.fill(x, TAB_Y, x + tw - 2, TAB_Y + TAB_H, bg);
            RenderHelper.renderOutline(g, x, TAB_Y, tw - 2, TAB_H, active ? t.accent() : 0xFF444466);
            if (active) g.fill(x, TAB_Y + TAB_H - 2, x + tw - 2, TAB_Y + TAB_H, t.accent());
            g.drawCenteredString(font, TAB_NAMES[i], x + (tw - 2) / 2, TAB_Y + (TAB_H - 8) / 2,
                    active ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        g.fill(0, TAB_Y + TAB_H, width, TAB_Y + TAB_H + 1, t.divider());
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
                               int scroll, int maxRows, UITheme t, int mx, int my) {
        int visible = Math.min(maxRows, items.length);
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

        int lineY = y + 7;
        int spacing = 10;
        int left = x + 8;

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
