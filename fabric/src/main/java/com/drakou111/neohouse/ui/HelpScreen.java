package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.compat.RegistryHelper;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.SyntaxTheme;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.ui.HelpContent.Block;
import com.drakou111.neohouse.ui.HelpContent.Code;
import com.drakou111.neohouse.ui.HelpContent.Entry;
import com.drakou111.neohouse.ui.HelpContent.Heading;
import com.drakou111.neohouse.ui.HelpContent.Preformatted;
import com.drakou111.neohouse.ui.HelpContent.Text;
import com.drakou111.neohouse.ui.HelpContent.Topic;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The help: a sidebar of guide and reference topics, a search box that searches all of them, and
 * the selected topic (or the search results) wrapped to the window, with a Copy button on every
 * code example. The content itself lives in {@link HelpContent}.
 */
public class HelpScreen extends ScreenBase {

    private static final int PAD = 8;
    private static final int HEADER_H = 28;
    private static final int SIDEBAR_MIN_W = 100;
    private static final int SIDEBAR_MAX_W = 150;
    private static final int SIDEBAR_ROW_H = 13;
    private static final int SECTION_ROW_H = 16;
    private static final int SEARCH_W = 150;
    private static final int TITLE_H = 22;
    private static final int HEADING_H = 18;
    private static final int TEXT_H = 11;
    private static final int CODE_H = 10;
    private static final int ENTRY_H = 18;
    private static final int GAP_H = 6;
    private static final int ICON_SIZE = 16;
    private static final int SB_W = 4;
    private static final int SCROLL_STEP = 24;
    private static final int COPY_W = 30;
    private static final int COPY_H = 10;
    private static final long COPIED_SHOWN_MS = 1200;

    private static final int COL_TITLE = 0xFFFFFFFF;
    private static final int COL_HEADING = 0xFFFFCC55;
    private static final int COL_TEXT = 0xFFCCCCCC;
    private static final int COL_DIM = 0xFF999999;

    private enum Kind { TITLE, HEADING, TEXT, CODE, PRE, SIGNATURE, DETAIL, GAP }

    /**
     * One laid-out line of the page. {@code copy} is set on the first line of a code example and
     * holds what its Copy button copies; {@code icon} is an item id drawn before a signature.
     */
    private record Row(Kind kind, String text, int height, String icon, String copy) {
        static Row of(Kind kind, String text, int height) { return new Row(kind, text, height, null, null); }
    }

    private final Screen parent;
    private final List<Topic> topics = HelpContent.topics();
    private final SyntaxHighlighter highlighter = new SyntaxHighlighter();

    private Topic selected;
    private EditBox searchBox;
    private List<Row> rows = List.of();
    private int rowsWidth = -1;
    private String rowsQuery = null;
    private Topic rowsTopic = null;
    private int scroll = 0;
    private int sidebarScroll = 0;
    private boolean scrollbarDragging = false;
    private int dragStartY;
    private int dragStartScroll;
    private String copiedText = null;
    private long copiedAtMs = 0;

    public HelpScreen(Screen parent) {
        super(Component.literal("NeoHouse Help"));
        this.parent = parent;
        this.selected = this.topics.getFirst();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> ClientUi.setScreen(this.minecraft, this.parent))
                .bounds(PAD, 5, 52, 18).build());
        String query = this.searchBox == null ? "" : this.searchBox.getValue();
        int searchW = Math.min(SEARCH_W, Math.max(60, this.width - 220));
        this.searchBox = new EditBox(this.font, this.width - PAD - searchW, 6, searchW, 16, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Component.literal("Search (Ctrl+F)"));
        this.searchBox.setValue(query);
        this.searchBox.setResponder(s -> this.scroll = 0);
        addRenderableWidget(this.searchBox);
    }

    // ---- layout ----

    private int sidebarWidth() {
        return Math.max(SIDEBAR_MIN_W, Math.min(SIDEBAR_MAX_W, this.width / 5));
    }

    private int contentLeft() {
        return sidebarWidth() + 1;
    }

    private int contentRight() {
        return this.width - SB_W - 4;
    }

    private int textWidth() {
        return Math.max(60, contentRight() - contentLeft() - PAD * 2);
    }

    private int viewHeight() {
        return this.height - HEADER_H - 1;
    }

    private String query() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim();
    }

    /** The page's rows, laid out again only when the topic, the search or the width changes. */
    private List<Row> rows() {
        String query = query();
        if (this.rowsWidth == textWidth() && query.equals(this.rowsQuery) && this.rowsTopic == this.selected) return this.rows;
        List<Row> out = new ArrayList<>();
        if (query.isEmpty()) {
            layoutTopic(out, this.selected.title(), this.selected.blocks());
        } else {
            for (Topic topic : this.topics) {
                List<Block> found = HelpContent.search(topic, query);
                if (!found.isEmpty()) layoutTopic(out, topic.title(), found);
            }
            if (out.isEmpty()) out.add(Row.of(Kind.TEXT, "Nothing found for \"" + query + "\".", TEXT_H));
        }
        this.rows = out;
        this.rowsWidth = textWidth();
        this.rowsQuery = query;
        this.rowsTopic = this.selected;
        return out;
    }

    private void layoutTopic(List<Row> out, String title, List<Block> blocks) {
        out.add(Row.of(Kind.GAP, "", GAP_H));
        out.add(Row.of(Kind.TITLE, title, TITLE_H));
        for (Block block : blocks) {
            switch (block) {
                case Heading h -> {
                    out.add(Row.of(Kind.GAP, "", GAP_H));
                    out.add(Row.of(Kind.HEADING, h.text(), HEADING_H));
                }
                case Text t -> {
                    for (String line : wrap(t.text(), textWidth())) out.add(Row.of(Kind.TEXT, line, TEXT_H));
                    out.add(Row.of(Kind.GAP, "", GAP_H));
                }
                case Code c -> {
                    layoutCode(out, c.code(), Kind.CODE);
                    out.add(Row.of(Kind.GAP, "", GAP_H));
                }
                case Preformatted p -> {
                    for (String line : p.text().split("\n", -1)) out.add(Row.of(Kind.PRE, line, CODE_H));
                    out.add(Row.of(Kind.GAP, "", GAP_H));
                }
                case Entry e -> layoutEntry(out, e);
            }
        }
        out.add(Row.of(Kind.GAP, "", GAP_H * 2));
    }

    private void layoutEntry(List<Row> out, Entry e) {
        out.add(new Row(Kind.SIGNATURE, e.signature(), ENTRY_H, e.icon(), null));
        int indent = textWidth() - ICON_SIZE - 4;
        if (!e.description().isEmpty()) {
            for (String line : wrap(e.description(), indent)) out.add(Row.of(Kind.DETAIL, line, TEXT_H));
            out.add(Row.of(Kind.GAP, "", GAP_H));
        }
        for (String detail : e.details())
            for (String line : wrap(detail, indent)) out.add(Row.of(Kind.DETAIL, line, TEXT_H));
        if (e.example() != null) {
            if (!e.details().isEmpty()) out.add(Row.of(Kind.GAP, "", GAP_H));
            layoutCode(out, e.example(), Kind.CODE);
        }
        out.add(Row.of(Kind.GAP, "", GAP_H * 2));
    }

    private static void layoutCode(List<Row> out, String code, Kind kind) {
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length; i++)
            out.add(new Row(kind, lines[i], CODE_H, null, i == 0 ? code : null));
    }

    /** {@code text} split into lines no wider than {@code maxWidth}, breaking at spaces. */
    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && this.font.width(line + " " + word) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private int totalHeight(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += r.height();
        return h;
    }

    private int maxScroll() {
        return Math.max(0, totalHeight(rows()) - viewHeight());
    }

    // ---- drawing ----

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        UITheme t = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, this.width, this.height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));

        g.fill(0, 0, this.width, HEADER_H, t.panel(o.panelAlpha));
        RenderHelper.renderLogo(g, PAD + 58, 5, 18, t.accent());
        g.drawString(this.font, "NeoHouse - Help", PAD + 58 + 22, 10, COL_TITLE);
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, t.accent());

        renderSidebar(g, mouseX, mouseY, t, o);
        renderContent(g, mouseX, mouseY, t);
        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    private void renderSidebar(Gfx g, int mouseX, int mouseY, UITheme t, UIOpacity o) {
        int w = sidebarWidth();
        int top = HEADER_H + 1;
        g.fill(0, top, w, this.height, t.panel(o.panelAlpha));
        g.fill(w, top, w + 1, this.height, t.accentTint(0x88));

        boolean searching = !query().isEmpty();
        g.enableScissor(0, top, w, this.height);
        int y = top + 4 - this.sidebarScroll;
        String section = null;
        for (Topic topic : this.topics) {
            if (!topic.section().equals(section)) {
                section = topic.section();
                g.drawString(this.font, section.toUpperCase(), PAD, y + 5, t.muted(), false);
                y += SECTION_ROW_H;
            }
            boolean active = !searching && topic == this.selected;
            boolean hover = mouseX < w && mouseY >= y && mouseY < y + SIDEBAR_ROW_H && mouseY > top;
            if (active) g.fill(0, y, w, y + SIDEBAR_ROW_H, t.highlight(0xFF));
            else if (hover) g.fill(0, y, w, y + SIDEBAR_ROW_H, t.surface(0xFF));
            if (active) g.fill(0, y, 2, y + SIDEBAR_ROW_H, t.accent());
            String label = this.font.plainSubstrByWidth(topic.title(), w - PAD - 4);
            g.drawString(this.font, label, PAD, y + 3, active ? 0xFFFFFF88 : hover ? COL_TEXT : 0xFFAAAAAA, false);
            y += SIDEBAR_ROW_H;
        }
        g.disableScissor();
    }

    private void renderContent(Gfx g, int mouseX, int mouseY, UITheme t) {
        List<Row> rows = rows();
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll()));
        int left = contentLeft();
        int right = contentRight();
        int top = HEADER_H + 1;
        int x = left + PAD;

        g.enableScissor(left, top, right, this.height);
        int y = top - this.scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int h = row.height();
            if (y + h > top && y < this.height) drawRow(g, rows, i, x, y, left, right, mouseX, mouseY, t);
            y += h;
        }
        g.disableScissor();
        renderScrollbar(g, rows, t);
    }

    private void drawRow(Gfx g, List<Row> rows, int i, int x, int y, int left, int right, int mouseX, int mouseY, UITheme t) {
        Row row = rows.get(i);
        switch (row.kind()) {
            case TITLE -> {
                g.drawString(this.font, row.text(), x, y + 6, COL_TITLE);
                g.fill(x, y + row.height() - 3, right - PAD, y + row.height() - 2, t.accentTint(0x88));
            }
            case HEADING -> {
                g.fill(x - 4, y + 4, x - 2, y + row.height() - 3, t.accent());
                g.drawString(this.font, row.text(), x + 2, y + 6, COL_HEADING, false);
            }
            case TEXT -> g.drawString(this.font, row.text(), x, y + 1, COL_TEXT, false);
            case DETAIL -> g.drawString(this.font, row.text(), x + ICON_SIZE + 4, y + 1, COL_DIM, false);
            case CODE, PRE -> {
                boolean first = i == 0 || rows.get(i - 1).kind() != row.kind();
                boolean last = i == rows.size() - 1 || rows.get(i + 1).kind() != row.kind();
                g.fill(x - 4, y - (first ? 3 : 0), right - PAD, y + row.height() + (last ? 3 : 0), t.surface(0xAA));
                g.fill(x - 4, y - (first ? 3 : 0), x - 3, y + row.height() + (last ? 3 : 0), t.accentTint(0xCC));
                if (row.kind() == Kind.CODE) drawCode(g, row.text(), x, y + 1);
                else g.drawString(this.font, row.text(), x, y + 1, COL_TEXT, false);
                if (row.copy() != null) drawCopyButton(g, row.copy(), right - PAD - COPY_W - 2, y - 2, mouseX, mouseY, t);
            }
            case SIGNATURE -> {
                if (row.icon() != null) drawIcon(g, row.icon(), x, y + 1);
                drawCode(g, row.text(), x + ICON_SIZE + 4, y + 5);
            }
            case GAP -> { }
        }
    }

    private void drawCopyButton(Gfx g, String text, int bx, int by, int mouseX, int mouseY, UITheme t) {
        boolean hover = mouseX >= bx && mouseX < bx + COPY_W && mouseY >= by && mouseY < by + COPY_H + 2;
        boolean justCopied = text.equals(this.copiedText) && System.currentTimeMillis() - this.copiedAtMs < COPIED_SHOWN_MS;
        g.fill(bx, by, bx + COPY_W, by + COPY_H + 2, hover ? t.highlight(0xFF) : t.panel(0xFF));
        String label = justCopied ? "Copied" : "Copy";
        g.drawString(this.font, label, bx + (COPY_W - this.font.width(label)) / 2, by + 2, justCopied ? 0xFF88CC88 : COL_TEXT, false);
    }

    private void renderScrollbar(Gfx g, List<Row> rows, UITheme t) {
        int total = totalHeight(rows);
        int view = viewHeight();
        if (total <= view) return;
        int trackX = this.width - SB_W - 2;
        int trackY = HEADER_H + 1;
        g.fill(trackX, trackY, trackX + SB_W, trackY + view, 0x33FFFFFF);
        int thumbH = Math.max(16, view * view / total);
        int thumbY = trackY + (view - thumbH) * this.scroll / Math.max(1, total - view);
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, this.scrollbarDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF)));
    }

    private void drawIcon(Gfx g, String id, int x, int y) {
        Item item = RegistryHelper.lookupItem(id).orElse(Items.AIR);
        if (item == Items.AIR) return;
        g.renderItem(new ItemStack(item), x, y);
    }

    private void drawCode(Gfx g, String text, int x, int y) {
        SyntaxTheme st = NeoHouseConfig.get().syntaxTheme;
        int curX = x;
        int drawn = 0;
        for (SyntaxHighlighter.Span sp : this.highlighter.buildSpans(text)) {
            int lo = Math.max(drawn, sp.start());
            int hi = Math.min(text.length(), sp.end());
            if (lo >= hi) continue;
            if (lo > drawn) {
                String gap = text.substring(drawn, lo);
                g.drawString(this.font, gap, curX, y, st.plain, false);
                curX += this.font.width(gap);
            }
            String segment = text.substring(lo, hi);
            g.drawString(this.font, segment, curX, y, sp.color() == 0 ? st.plain : sp.color(), false);
            curX += this.font.width(segment);
            drawn = hi;
        }
        if (drawn < text.length()) g.drawString(this.font, text.substring(drawn), curX, y, st.plain, false);
    }

    // ---- input ----

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        if (superMouseClicked(mx, my, btn)) return true;
        if (btn != 0 || my <= HEADER_H) return false;
        if (mx < sidebarWidth()) return clickSidebar(my);

        int trackX = this.width - SB_W - 2;
        if (mx >= trackX - 2 && maxScroll() > 0) {
            this.scrollbarDragging = true;
            this.dragStartY = (int) my;
            this.dragStartScroll = this.scroll;
            return true;
        }
        return clickCopyButton(mx, my);
    }

    private boolean clickSidebar(double my) {
        int y = HEADER_H + 1 + 4 - this.sidebarScroll;
        String section = null;
        for (Topic topic : this.topics) {
            if (!topic.section().equals(section)) {
                section = topic.section();
                y += SECTION_ROW_H;
            }
            if (my >= y && my < y + SIDEBAR_ROW_H) {
                this.selected = topic;
                this.searchBox.setValue("");
                this.scroll = 0;
                return true;
            }
            y += SIDEBAR_ROW_H;
        }
        return false;
    }

    private boolean clickCopyButton(double mx, double my) {
        int bx = contentRight() - PAD - COPY_W - 2;
        if (mx < bx || mx >= bx + COPY_W) return false;
        int y = HEADER_H + 1 - this.scroll;
        for (Row row : rows()) {
            if (row.copy() != null && my >= y - 2 && my < y - 2 + COPY_H + 2) {
                this.minecraft.keyboardHandler.setClipboard(row.copy());
                this.copiedText = row.copy();
                this.copiedAtMs = System.currentTimeMillis();
                return true;
            }
            y += row.height();
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!this.scrollbarDragging) return superMouseDragged(mx, my, btn, dx, dy);
        int total = totalHeight(rows());
        int view = viewHeight();
        int thumbH = Math.max(16, view * view / Math.max(1, total));
        int slide = Math.max(1, view - thumbH);
        this.scroll = Math.max(0, Math.min(maxScroll(), this.dragStartScroll + (int) ((my - this.dragStartY) * (total - view) / slide)));
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int btn) {
        this.scrollbarDragging = false;
        return superMouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY) * SCROLL_STEP;
        if (mouseX < sidebarWidth()) {
            int sidebarHeight = 8 + 2 * SECTION_ROW_H + this.topics.size() * SIDEBAR_ROW_H;
            this.sidebarScroll = Math.max(0, Math.min(Math.max(0, sidebarHeight - viewHeight()), this.sidebarScroll - step));
        } else {
            this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - step));
        }
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & Keys.MOD_CONTROL) != 0;
        if (ctrl && key == Keys.F) {
            setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            return true;
        }
        if (key == Keys.ESCAPE && !this.searchBox.getValue().isEmpty()) {
            this.searchBox.setValue("");
            return true;
        }
        if (!this.searchBox.isFocused()) {
            switch (key) {
                case Keys.PAGE_DOWN -> { this.scroll = Math.min(maxScroll(), this.scroll + viewHeight() - SCROLL_STEP); return true; }
                case Keys.PAGE_UP -> { this.scroll = Math.max(0, this.scroll - viewHeight() + SCROLL_STEP); return true; }
                case Keys.HOME -> { this.scroll = 0; return true; }
                case Keys.END -> { this.scroll = maxScroll(); return true; }
                default -> { }
            }
        }
        return superKeyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        ClientUi.setScreen(this.minecraft, this.parent);
    }
}
