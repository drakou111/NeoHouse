package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.SyntaxTheme;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * The NeoHouse settings. Every setting is declared once in {@link #createRows()} with its tab,
 * label, tooltip and control; layout, scrolling, clicks, dropdowns and saving all follow from that
 * list. Values are edited on the rows and written to the config on Back or Escape.
 */
public class ConfigScreen extends ScreenBase {

    private static final int PAD = 10;
    private static final int HEADER_H = 28;
    private static final int TAB_H = 20;
    private static final int TAB_Y = HEADER_H + 4;
    private static final int CONTENT_TOP = TAB_Y + TAB_H + 6;
    private static final int ROW_H = 24;
    private static final int LABEL_W = 220;
    private static final int BOX_W = 60;
    private static final int BOX_H = 14;
    private static final int DROP_W = 130;
    private static final int DROP_H = BOX_H;
    private static final int DROP_ITEM_H = 12;
    private static final int DROP_MAX_ROWS = 8;
    private static final int MOB_DROP_MAX_ROWS = 10;
    private static final int SLIDER_H = 8;
    private static final int KNOB_W = 8;
    private static final int PREVIEW_H = 80;
    private static final int PREVIEW_MAX_W = 320;
    private static final int SB_W = 4;
    private static final int SCROLL_STEP = ROW_H;
    private static final int LABEL_COLOR = 0xFFCCCCCC;

    private enum Tab {
        COMPILER("Compiler"), EDITOR("IDE"), UI("UI");

        final String label;

        Tab(String label) { this.label = label; }
    }

    private final Screen parent;
    private final List<Row> rows;
    private final DropdownRow syntaxThemeRow;
    private final DropdownRow uiThemeRow;
    private final SliderRow opacityRow;

    private Tab activeTab = Tab.COMPILER;
    private DropdownRow openDropdown = null;
    private SliderRow draggingSlider = null;
    private int scroll = 0;

    public ConfigScreen(Screen parent) {
        super(Component.literal("NeoHouse Config"));
        this.parent = parent;
        NeoHouseConfig cfg = NeoHouseConfig.get();
        this.syntaxThemeRow = enumRow(Tab.EDITOR, "Syntax theme:", null, SyntaxTheme.values(), t -> t.displayName,
                cfg.syntaxTheme, t -> cfg.syntaxTheme = t);
        this.uiThemeRow = enumRow(Tab.UI, "UI theme:", null, UITheme.values(), t -> t.displayName,
                cfg.uiTheme, t -> cfg.uiTheme = t);
        this.opacityRow = new SliderRow(Tab.UI, "UI opacity:", null, UIOpacity.values(), cfg.uiOpacity, o -> cfg.uiOpacity = o);
        this.rows = createRows();
    }

    /** Every setting, in display order. Add a setting here and it gets a row, a tooltip and saving. */
    private List<Row> createRows() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        List<Row> list = new ArrayList<>();

        list.add(enumRow(Tab.COMPILER, "Override on run:",
                "What a run does with functions, events, commands and menus that already exist.\n\n"
                        + "Always Override: recreate them from scratch.\n"
                        + "Never Override: skip them.\n"
                        + "Override If Empty: fill them only when they have no content yet.\n"
                        + "Append: add the script's actions after theirs, skipped if a Housing limit would be exceeded. "
                        + "Menus get only their empty slots filled.",
                OverrideMode.values(), m -> m.displayName, cfg.overrideMode, m -> cfg.overrideMode = m));
        list.add(enumRow(Tab.COMPILER, "Multi-call mode:",
                "Housing won't run a function twice in one execution.\n\n"
                        + "Off: no check.\n"
                        + "Warn: allowed but reported.\n"
                        + "Error: compilation fails.\n"
                        + "Fix: auto-create copies ($0, $1, ...) so each call uses its own function.",
                MultiCallMode.values(), m -> m.displayName, cfg.multiCallMode, m -> cfg.multiCallMode = m));
        list.add(enumRow(Tab.COMPILER, "Recursion:",
                "How to handle functions that call themselves directly or indirectly. Hypixel Housing does not "
                        + "officially support recursive functions.\n\n"
                        + "Off: allowed silently.\n"
                        + "Warn: allowed but reported.\n"
                        + "Error: compilation fails.",
                RecursionMode.values(), m -> m.displayName, cfg.recursionMode, m -> cfg.recursionMode = m));
        list.add(enumRow(Tab.COMPILER, "Global call rules:",
                "How to handle a globally-called function (or loop) that itself contains 'global ...' calls.\n\n"
                        + "Off: allowed silently.\n"
                        + "Warn: allowed but reported.\n"
                        + "Error: compilation fails.",
                GlobalCallMode.values(), m -> m.displayName, cfg.globalCallMode, m -> cfg.globalCallMode = m));
        list.add(enumRow(Tab.COMPILER, "Undeclared variables:",
                "A variable used without a declaration becomes an untyped var, which hides typos like p.cions.\n\n"
                        + "Allow: no check.\n"
                        + "Warn: reported.\n"
                        + "Error: compilation fails.",
                UndeclaredVariableMode.values(), m -> m.displayName, cfg.undeclaredVariableMode, m -> cfg.undeclaredVariableMode = m));
        list.add(new BoolRow(Tab.COMPILER, "Ban the var type:",
                "Reject the var type everywhere: in declarations, parameters and return types.\n\n"
                        + "Undeclared variables are vars, so they become errors too.",
                cfg.forbidVarType, v -> cfg.forbidVarType = v));
        list.add(new IntRow(Tab.COMPILER, "Delay after click (ticks):",
                "How long to wait after each UI click, in game ticks.\n\nLower is faster but might break.",
                cfg.clickDelay, 0, Integer.MAX_VALUE, v -> cfg.clickDelay = v));
        list.add(new IntRow(Tab.COMPILER, "Action timeout (ticks):",
                "How many ticks to wait for a screen or anvil to respond before timing out.\n\nIncrease it if the server lags.",
                cfg.actionTimeoutTicks, 20, Integer.MAX_VALUE, v -> cfg.actionTimeoutTicks = v));

        list.add(new BoolRow(Tab.EDITOR, "Show argument hints:",
                "Show the expected argument names when writing inside function calls.",
                cfg.showArgumentHints, v -> cfg.showArgumentHints = v));
        list.add(new BoolRow(Tab.EDITOR, "Show argument recommendations:",
                "Show autocomplete suggestions for events, placeholders, locations and string argument values.",
                cfg.showArgumentRecommendations, v -> cfg.showArgumentRecommendations = v));
        list.add(new BoolRow(Tab.EDITOR, "Auto-indent on Enter:",
                "Automatically match the indentation of the previous line on Enter.",
                cfg.autoIndent, v -> cfg.autoIndent = v));
        list.add(this.syntaxThemeRow);
        list.add(new IntRow(Tab.EDITOR, "Tab width (spaces):",
                "Number of spaces inserted per Tab and used for auto-indent in the code editor.\n\n1 to 16, default 4.",
                cfg.tabWidth, 1, 16, v -> cfg.tabWidth = v));
        list.add(new IntRow(Tab.EDITOR, "Max error lines:",
                "Maximum number of lines shown in the editor's error bar before it becomes scrollable.\n\n1 to 20, default 5.",
                cfg.errorBarMaxLines, 1, 20, v -> cfg.errorBarMaxLines = v));
        list.add(new PreviewRow(Tab.EDITOR));

        list.add(this.uiThemeRow);
        list.add(this.opacityRow);
        list.add(new BoolRow(Tab.UI, "Show mob while running:",
                "Display a mob above the progress overlay while a script is running as a companion!",
                cfg.showScriptMob, v -> cfg.showScriptMob = v));
        list.add(mobRow(cfg));
        return list;
    }

    private static <E extends Enum<E>> DropdownRow enumRow(Tab tab, String label, String tip, E[] values,
                                                           Function<E, String> display, E current, Consumer<E> save) {
        List<String> names = new ArrayList<>();
        for (E value : values) names.add(display.apply(value));
        return new DropdownRow(tab, label, tip, names, current.ordinal(), DROP_MAX_ROWS, i -> save.accept(values[i]));
    }

    private static DropdownRow mobRow(NeoHouseConfig cfg) {
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) if (type.getCategory() != MobCategory.MISC) types.add(type);
        types.sort(Comparator.comparing(type -> type.getDescription().getString()));
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (EntityType<?> type : types) {
            // The key's class was renamed in 1.21.11, so only its text is used.
            String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            if (id.equals(cfg.scriptMobId)) selected = ids.size();
            ids.add(id);
            names.add(type.getDescription().getString());
        }
        return new DropdownRow(Tab.UI, "Script mob:", null, names, selected, MOB_DROP_MAX_ROWS, i -> cfg.scriptMobId = ids.get(i));
    }

    // ---- setup and layout ----

    @Override
    protected void init() {
        // Also runs when the window is resized: keep what was typed into the old widgets.
        captureWidgets();
        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> closeAndSave()).bounds(PAD, 5, 52, 18).build());
        for (Row row : tabRows()) row.createWidget(this);
        layoutRows();
    }

    private void switchTab(Tab tab) {
        if (tab == this.activeTab) return;
        captureWidgets();
        this.activeTab = tab;
        this.openDropdown = null;
        this.scroll = 0;
        rebuildWidgets();
    }

    private List<Row> tabRows() {
        List<Row> list = new ArrayList<>();
        for (Row row : this.rows) if (row.tab == this.activeTab) list.add(row);
        return list;
    }

    private int widgetX() {
        return PAD + Math.min(LABEL_W, this.width / 2);
    }

    private int contentBottom() {
        return this.height - PAD;
    }

    private int maxScroll() {
        int total = 0;
        for (Row row : tabRows()) total += row.height();
        return Math.max(0, total - (contentBottom() - CONTENT_TOP));
    }

    /** Places every row (and its widget) for the current scroll; widgets cut off by the view are hidden. */
    private void layoutRows() {
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll()));
        int y = CONTENT_TOP - this.scroll;
        for (Row row : tabRows()) {
            row.y = y;
            row.placeWidget(widgetX(), y, y >= CONTENT_TOP && y + row.height() <= contentBottom());
            y += row.height();
        }
    }

    private void captureWidgets() {
        for (Row row : this.rows) row.capture();
    }

    // ---- input ----

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        int x = (int) mx;
        int y = (int) my;
        if (this.openDropdown != null) {
            DropdownRow dropdown = this.openDropdown;
            this.openDropdown = null;
            if (dropdown.clickList(x, y, widgetX(), listY(dropdown))) return true;
            if (hit(x, y, widgetX(), dropdown.controlY(), DROP_W, DROP_H)) return true;
        }

        if (y >= TAB_Y && y < TAB_Y + TAB_H) {
            for (Tab tab : Tab.values()) {
                if (x >= tabX(tab) && x < tabX(tab) + tabWidth()) {
                    switchTab(tab);
                    return true;
                }
            }
        }

        if (y >= CONTENT_TOP && y < contentBottom()) {
            for (Row row : tabRows()) {
                if (row instanceof DropdownRow dropdown && hit(x, y, widgetX(), dropdown.controlY(), DROP_W, DROP_H)) {
                    this.openDropdown = dropdown;
                    dropdown.centerOnSelection();
                    return true;
                }
                if (row instanceof SliderRow slider && slider.hitTrack(x, y, widgetX())) {
                    this.draggingSlider = slider;
                    slider.dragTo(x, widgetX());
                    return true;
                }
            }
        }
        return superMouseClicked(mx, my, button);
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.draggingSlider != null) {
            this.draggingSlider.dragTo((int) mx, widgetX());
            return true;
        }
        return superMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int button) {
        this.draggingSlider = null;
        return superMouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (this.openDropdown != null && this.openDropdown.scrollList((int) mx, (int) my, widgetX(), listY(this.openDropdown), dy))
            return true;
        this.openDropdown = null;
        this.scroll -= (int) Math.signum(dy) * SCROLL_STEP;
        layoutRows();
        return true;
    }

    @Override
    public void onClose() {
        save();
        ClientUi.setScreen(this.minecraft, null);
    }

    private void closeAndSave() {
        save();
        ClientUi.setScreen(this.minecraft, this.parent);
    }

    private void save() {
        captureWidgets();
        for (Row row : this.rows) row.save();
        NeoHouseConfig.get().save();
    }

    // ---- drawing ----

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        layoutRows();
        UITheme t = UITheme.values()[this.uiThemeRow.selected];
        UIOpacity o = UIOpacity.values()[this.opacityRow.selected];
        g.fillGradient(0, 0, this.width, this.height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        g.fill(0, 0, this.width, HEADER_H, t.panel(o.panelAlpha));
        RenderHelper.renderLogo(g, PAD + 56, 4, 18, t.accent());
        g.drawString(this.font, "NeoHouse - Config", PAD + 56 + 22, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, t.accent());
        drawTabs(g, t, mouseX, mouseY);

        String tip = null;
        g.enableScissor(0, CONTENT_TOP, this.width, contentBottom());
        for (Row row : tabRows()) {
            if (row.y + row.height() < CONTENT_TOP || row.y > contentBottom()) continue;
            if (row.label != null) g.drawString(this.font, row.label, PAD, row.y + (ROW_H - 9) / 2, LABEL_COLOR);
            row.draw(g, this, t, widgetX(), mouseX, mouseY);
            if (row.tip != null && hit(mouseX, mouseY, PAD, row.y + (ROW_H - 9) / 2 - 2, Math.min(LABEL_W, this.width / 2), 13)
                    && mouseY >= CONTENT_TOP && mouseY < contentBottom()) tip = row.tip;
        }
        g.disableScissor();
        drawScrollbar(g, t);

        renderWidgets(g, mouseX, mouseY, partialTick);
        if (this.openDropdown != null) this.openDropdown.drawList(g, this, t, widgetX(), listY(this.openDropdown), mouseX, mouseY);
        if (tip != null) g.tooltip(this.font, tip, mouseX, mouseY);
    }

    /** Where a dropdown's list goes: below its button, or above when it wouldn't fit below. */
    private int listY(DropdownRow dropdown) {
        int below = dropdown.controlY() + DROP_H;
        if (below + dropdown.listHeight() <= this.height) return below;
        return Math.max(CONTENT_TOP, dropdown.controlY() - dropdown.listHeight());
    }

    private void drawScrollbar(Gfx g, UITheme t) {
        int max = maxScroll();
        if (max <= 0) return;
        int view = contentBottom() - CONTENT_TOP;
        int total = view + max;
        int trackX = this.width - SB_W - 2;
        g.fill(trackX, CONTENT_TOP, trackX + SB_W, contentBottom(), 0x33FFFFFF);
        int thumbH = Math.max(16, view * view / total);
        int thumbY = CONTENT_TOP + (view - thumbH) * this.scroll / max;
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, 0x99000000 | (t.muted() & 0xFFFFFF));
    }

    private int tabWidth() { return (this.width - 2 * PAD) / Tab.values().length; }

    private int tabX(Tab tab) { return PAD + tab.ordinal() * tabWidth(); }

    private void drawTabs(Gfx g, UITheme t, int mx, int my) {
        int tw = tabWidth();
        for (Tab tab : Tab.values()) {
            int x = tabX(tab);
            boolean active = tab == this.activeTab;
            boolean hover = hit(mx, my, x, TAB_Y, tw, TAB_H);
            g.fill(x, TAB_Y, x + tw - 2, TAB_Y + TAB_H, active ? t.surface(0xFF) : hover ? t.highlight(0x66) : 0x33000000);
            RenderHelper.renderOutline(g, x, TAB_Y, tw - 2, TAB_H, active ? t.accent() : 0xFF444466);
            if (active) g.fill(x, TAB_Y + TAB_H - 2, x + tw - 2, TAB_Y + TAB_H, t.accent());
            g.drawCenteredString(this.font, tab.label, x + (tw - 2) / 2, TAB_Y + (TAB_H - 8) / 2, active ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        g.fill(0, TAB_Y + TAB_H, this.width, TAB_Y + TAB_H + 1, t.divider());
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---- rows ----

    /** One setting: a label on the left, a control at the widget column, and how it is saved. */
    private abstract static class Row {
        final Tab tab;
        final String label;
        final String tip;
        int y;

        Row(Tab tab, String label, String tip) {
            this.tab = tab;
            this.label = label;
            this.tip = tip;
        }

        int height() { return ROW_H; }

        /** Adds this row's vanilla widget to the screen, if it has one. */
        void createWidget(ConfigScreen screen) {}

        void placeWidget(int x, int y, boolean visible) {}

        /** Copies the widget's value into the row (before the widgets are rebuilt or the row is saved). */
        void capture() {}

        void draw(Gfx g, ConfigScreen screen, UITheme t, int x, int mouseX, int mouseY) {}

        abstract void save();
    }

    private static final class DropdownRow extends Row {
        private final List<String> names;
        private final int maxRows;
        private final IntConsumer onSave;
        int selected;
        private int listScroll = 0;

        DropdownRow(Tab tab, String label, String tip, List<String> names, int selected, int maxRows, IntConsumer onSave) {
            super(tab, label, tip);
            this.names = names;
            this.selected = selected;
            this.maxRows = maxRows;
            this.onSave = onSave;
        }

        int controlY() { return this.y + (ROW_H - DROP_H) / 2; }

        int listHeight() { return Math.min(this.maxRows, this.names.size()) * DROP_ITEM_H + 4; }

        void centerOnSelection() {
            this.listScroll = Math.max(0, Math.min(this.selected - this.maxRows / 2, this.names.size() - this.maxRows));
        }

        boolean clickList(int mx, int my, int x, int listY) {
            if (!hit(mx, my, x, listY, DROP_W, listHeight())) return false;
            int index = (my - listY - 2) / DROP_ITEM_H + this.listScroll;
            if (index >= 0 && index < this.names.size()) this.selected = index;
            return true;
        }

        boolean scrollList(int mx, int my, int x, int listY, double dy) {
            if (!hit(mx, my, x, listY, DROP_W, listHeight())) return false;
            int max = Math.max(0, this.names.size() - this.maxRows);
            this.listScroll = Math.max(0, Math.min(max, this.listScroll - (int) Math.signum(dy)));
            return true;
        }

        @Override
        void draw(Gfx g, ConfigScreen screen, UITheme t, int x, int mouseX, int mouseY) {
            Font font = screen.font;
            int y = controlY();
            boolean open = screen.openDropdown == this;
            boolean hovered = hit(mouseX, mouseY, x, y, DROP_W, DROP_H);
            g.fill(x, y, x + DROP_W, y + DROP_H, open ? t.highlight(0xFF) : hovered ? t.surface(0xFF) : 0xFF0F0F18);
            RenderHelper.renderOutline(g, x, y, DROP_W, DROP_H, open ? t.accent() : 0xFF444466);
            String label = this.names.get(this.selected);
            int maxLabelW = DROP_W - 20;
            String shown = font.width(label) > maxLabelW ? font.plainSubstrByWidth(label, maxLabelW) + "..." : label;
            g.drawString(font, shown, x + 4, y + (DROP_H - 8) / 2, 0xFFDDDDDD, false);
            g.drawString(font, open ? "▲" : "▼", x + DROP_W - 11, y + (DROP_H - 8) / 2, 0xFF888899, false);
        }

        void drawList(Gfx g, ConfigScreen screen, UITheme t, int x, int listY, int mouseX, int mouseY) {
            Font font = screen.font;
            int visible = Math.min(this.maxRows, this.names.size());
            int listH = listHeight();
            g.fill(x, listY, x + DROP_W, listY + listH, 0xFF0F0F1A);
            RenderHelper.renderOutline(g, x, listY, DROP_W, listH, t.accent());
            for (int i = 0; i < visible; i++) {
                int index = i + this.listScroll;
                if (index >= this.names.size()) break;
                int iy = listY + 2 + i * DROP_ITEM_H;
                boolean isSelected = index == this.selected;
                if (isSelected) g.fill(x, iy, x + DROP_W, iy + DROP_ITEM_H, t.highlight(0xCC));
                else if (hit(mouseX, mouseY, x, iy, DROP_W, DROP_ITEM_H)) g.fill(x, iy, x + DROP_W, iy + DROP_ITEM_H, t.surface(0xCC));
                g.drawString(font, this.names.get(index), x + 4, iy + (DROP_ITEM_H - 8) / 2, isSelected ? t.accent() : 0xFFDDDDDD, false);
            }
            if (this.listScroll > 0) g.drawString(font, "▲", x + DROP_W - 10, listY + 2, 0xFFAAAAAA, false);
            if (this.listScroll + visible < this.names.size()) g.drawString(font, "▼", x + DROP_W - 10, listY + listH - 10, 0xFFAAAAAA, false);
        }

        @Override
        void save() { this.onSave.accept(this.selected); }
    }

    private static final class IntRow extends Row {
        private final int min;
        private final int max;
        private final IntConsumer onSave;
        private String value;
        private EditBox box;

        IntRow(Tab tab, String label, String tip, int value, int min, int max, IntConsumer onSave) {
            super(tab, label, tip);
            this.value = String.valueOf(value);
            this.min = min;
            this.max = max;
            this.onSave = onSave;
        }

        @Override
        void createWidget(ConfigScreen screen) {
            this.box = new EditBox(screen.font, 0, 0, BOX_W, BOX_H, Component.empty());
            this.box.setMaxLength(5);
            this.box.setValue(this.value);
            screen.addRenderableWidget(this.box);
        }

        @Override
        void placeWidget(int x, int y, boolean visible) {
            if (this.box == null) return;
            this.box.setX(x);
            this.box.setY(y + (ROW_H - BOX_H) / 2);
            this.box.visible = visible;
        }

        @Override
        void capture() {
            if (this.box != null) this.value = this.box.getValue();
            this.box = null;
        }

        /** An unreadable number keeps the saved value. */
        @Override
        void save() {
            try {
                this.onSave.accept(Math.max(this.min, Math.min(this.max, Integer.parseInt(this.value.trim()))));
            } catch (NumberFormatException ignored) {
                // not a number: leave the setting as it was
            }
        }
    }

    private static final class BoolRow extends Row {
        private final Consumer<Boolean> onSave;
        private boolean value;
        private Checkbox box;

        BoolRow(Tab tab, String label, String tip, boolean value, Consumer<Boolean> onSave) {
            super(tab, label, tip);
            this.value = value;
            this.onSave = onSave;
        }

        @Override
        void createWidget(ConfigScreen screen) {
            this.box = screen.addRenderableWidget(Checkbox.builder(Component.empty(), screen.font).pos(0, 0).selected(this.value).build());
        }

        @Override
        void placeWidget(int x, int y, boolean visible) {
            if (this.box == null) return;
            this.box.setX(x);
            this.box.setY(y + (ROW_H - BOX_H) / 2);
            this.box.visible = visible;
        }

        @Override
        void capture() {
            if (this.box != null) this.value = this.box.selected();
            this.box = null;
        }

        @Override
        void save() { this.onSave.accept(this.value); }
    }

    private static final class SliderRow extends Row {
        private final UIOpacity[] values;
        private final Consumer<UIOpacity> onSave;
        int selected;

        SliderRow(Tab tab, String label, String tip, UIOpacity[] values, UIOpacity current, Consumer<UIOpacity> onSave) {
            super(tab, label, tip);
            this.values = values;
            this.selected = current.ordinal();
            this.onSave = onSave;
        }

        private int trackY() { return this.y + (ROW_H - SLIDER_H) / 2; }

        boolean hitTrack(int mx, int my, int x) {
            return hit(mx, my, x, trackY() - 4, DROP_W, SLIDER_H + 8);
        }

        void dragTo(int mx, int x) {
            float f = Math.max(0f, Math.min(1f, (float) (mx - x - KNOB_W / 2) / (DROP_W - KNOB_W)));
            this.selected = Math.round(f * (this.values.length - 1));
        }

        @Override
        void draw(Gfx g, ConfigScreen screen, UITheme t, int x, int mouseX, int mouseY) {
            int trackY = trackY();
            int mid = trackY + SLIDER_H / 2;
            int knobX = x + this.selected * (DROP_W - KNOB_W) / (this.values.length - 1);
            g.fill(x, mid - 1, x + DROP_W, mid + 2, 0xFF333344);
            g.fill(x, mid - 1, knobX + KNOB_W / 2, mid + 2, t.accentTint(0xAA));
            for (int i = 0; i < this.values.length; i++) {
                int tx = x + i * (DROP_W - KNOB_W) / (this.values.length - 1) + KNOB_W / 2;
                g.fill(tx, mid - 2, tx + 1, mid + 3, 0xFF666688);
            }
            boolean active = screen.draggingSlider == this || hitTrack(mouseX, mouseY, x);
            g.fill(knobX, trackY, knobX + KNOB_W, trackY + SLIDER_H, active ? t.accent() : 0xFFCCCCDD);
            RenderHelper.renderOutline(g, knobX, trackY, KNOB_W, SLIDER_H, t.accent());
            g.drawString(screen.font, this.values[this.selected].displayName, x + DROP_W + 6, trackY, LABEL_COLOR, false);
        }

        @Override
        void save() { this.onSave.accept(this.values[this.selected]); }
    }

    /** A sample of code in the syntax theme being chosen. */
    private static final class PreviewRow extends Row {
        PreviewRow(Tab tab) {
            super(tab, null, null);
        }

        @Override
        int height() { return PREVIEW_H + 8; }

        @Override
        void draw(Gfx g, ConfigScreen screen, UITheme t, int x, int mouseX, int mouseY) {
            SyntaxTheme s = SyntaxTheme.values()[screen.syntaxThemeRow.selected];
            int left = PAD;
            int top = this.y + 4;
            int w = Math.min(PREVIEW_MAX_W, screen.width - 2 * PAD);
            g.fill(left, top, left + w, top + PREVIEW_H, 0xFF1A1A1A);
            RenderHelper.renderOutline(g, left, top, w, PREVIEW_H, t.accent());
            int cx = left + 8;
            int ly = top + 7;
            int step = 10;
            code(g, screen.font, cx, ly, s.comment, "// Comment");
            code(g, screen.font, cx, ly += step, s.keyword, "function ", s.plain, "greet() {");
            code(g, screen.font, cx + 8, ly += step, s.keyword, "if ", s.plain, "(", s.playerVar, "p", s.plain, ".", s.playerVar, "hp ",
                    s.plain, "<= ", s.number, "10", s.plain, ") {");
            code(g, screen.font, cx + 16, ly += step, s.action, "message", s.plain, "(", s.string, "\"Low HP!\"", s.plain, ");");
            code(g, screen.font, cx + 8, ly += step, s.plain, "}");
            code(g, screen.font, cx + 8, ly += step, s.globalVar, "g", s.plain, ".", s.globalVar, "score", s.plain, " += ",
                    s.number, "1", s.plain, ";");
            code(g, screen.font, cx, ly + step, s.plain, "}");
        }

        /** Draws alternating colour / text pairs on one line. */
        private static void code(Gfx g, Font font, int x, int y, Object... parts) {
            int cx = x;
            for (int i = 0; i < parts.length; i += 2) {
                String text = (String) parts[i + 1];
                g.drawString(font, text, cx, y, (int) parts[i]);
                cx += font.width(text);
            }
        }

        @Override
        void save() {}
    }
}
