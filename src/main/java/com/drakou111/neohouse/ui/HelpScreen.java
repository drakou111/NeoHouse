package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HelpScreen extends Screen {

    private static final int PAD       = 8;
    private static final int HEADER_H  = 28;
    private static final int TAB_BTN_H = 20;
    private static final int CONTENT_Y = HEADER_H + TAB_BTN_H + 2;
    private static final int LINE_H    = 11;
    private static final int ICON_SIZE = 8;
    private static final int ICON_PAD  = ICON_SIZE + 3;
    private static final int SB_W      = 4;
    private static final int SB_PAD    = 3;

    private static final int COL_H = 0xFFFFCC55; // header
    private static final int COL_C = 0xFF88CCFF; // signature / code
    private static final int COL_T = 0xFFCCCCCC; // normal text
    private static final int COL_D = 0xFF888888; // dim

    private enum Tab {
        OVERVIEW("Overview"),
        ACTIONS("Actions"),
        CONDITIONS("Conditions"),
        EVENTS("Events"),
        PLACEHOLDERS("Placeholders");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private record HelpLine(int color, String text, ResourceLocation icon) {
        HelpLine(int color, String text) { this(color, text, null); }
    }

    private record TabButton(Tab tab, int x, int w) {}

    // ── Static content (built once) ────────────────────────────────────────────

    private static final Map<Tab, List<HelpLine>> TAB_LINES;
    static {
        Map<Tab, List<HelpLine>> m = new EnumMap<>(Tab.class);
        m.put(Tab.OVERVIEW,     buildOverview());
        m.put(Tab.ACTIONS,      buildActions());
        m.put(Tab.CONDITIONS,   buildConditions());
        m.put(Tab.EVENTS,       buildEvents());
        m.put(Tab.PLACEHOLDERS, buildPlaceholders());
        TAB_LINES = Collections.unmodifiableMap(m);
    }

    private static List<HelpLine> buildOverview() {
        List<HelpLine> l = new ArrayList<>();
        l.add(new HelpLine(COL_H, "Overview"));
        l.add(new HelpLine(COL_T, "NeoHouse is a scripting language that compiles to Hypixel Housing actions."));
        l.add(new HelpLine(COL_T, "Write scripts in .nh files, then run them with the file browser or /neohouse run."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Commands"));
        l.add(new HelpLine(COL_C, "  /neohouse              — open home screen"));
        l.add(new HelpLine(COL_C, "  /neohouse files        — open file browser"));
        l.add(new HelpLine(COL_C, "  /neohouse run <file>   — compile & run a script"));
        l.add(new HelpLine(COL_C, "  /neohouse help         — open this help screen"));
        l.add(new HelpLine(COL_C, "  /neohouse config       — open config screen"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Variable Types"));
        l.add(new HelpLine(COL_C, "  long   — whole numbers"));
        l.add(new HelpLine(COL_C, "  double — decimal numbers"));
        l.add(new HelpLine(COL_C, "  string — text"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Language Basics"));
        l.add(new HelpLine(COL_T, "  Define variables:"));
        l.add(new HelpLine(COL_C, "    long counter;"));
        l.add(new HelpLine(COL_T, "  Conditions:"));
        l.add(new HelpLine(COL_C, "    if (playerHealth < 5.0) { kill(); }"));
        l.add(new HelpLine(COL_C, "    if (isPvpEnabled(true)) { ... } else { ... }"));
        l.add(new HelpLine(COL_T, "  Random blocks:"));
        l.add(new HelpLine(COL_C, "    random [ { kill(); }  { heal(); } ]"));
        l.add(new HelpLine(COL_T, "  Actions called directly:"));
        l.add(new HelpLine(COL_C, "    message(\"Hello\");"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Operators"));
        l.add(new HelpLine(COL_C, "  + - * /   arithmetic"));
        l.add(new HelpLine(COL_C, "  == != < <= > >=   comparison"));
        l.add(new HelpLine(COL_C, "  && ||   logical"));
        l.add(new HelpLine(COL_C, "  = += -= *= /=   assignment"));
        l.add(new HelpLine(COL_C, "  (long) (double) (string)   casts"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static String kindStr(ActionParamKind k) {
        return switch (k) {
            case STRING_LITERAL    -> "string";
            case BOOL_LITERAL      -> "bool";
            case LONG_LITERAL      -> "int";
            case DOUBLE_LITERAL    -> "float";
            case NORMAL_EXPRESSION -> "expr";
        };
    }

    private static String typeStr(VariableType t) {
        return switch (t) {
            case STRING -> "string";
            case LONG   -> "int";
            case DOUBLE -> "float";
            case BOOL   -> "bool";
            case VAR    -> "var";
        };
    }

    private static List<HelpLine> buildActions() {
        List<HelpLine> l = new ArrayList<>();
        for (AddAction a : AddAction.values()) {
            if (!a.canBeUsed) continue;
            StringBuilder sig = new StringBuilder(a.name).append("(");
            for (int i = 0; i < a.params.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(kindStr(a.params.get(i)));
            }
            sig.append(")");
            l.add(new HelpLine(COL_C, sig.toString(), a.texture));
            if (!a.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + a.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildConditions() {
        List<HelpLine> l = new ArrayList<>();
        for (AddCondition c : AddCondition.values()) {
            if (!c.canBeUsed) continue;
            StringBuilder sig = new StringBuilder(c.name).append("(");
            for (int i = 0; i < c.params.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(kindStr(c.params.get(i)));
            }
            sig.append(")");
            l.add(new HelpLine(COL_C, sig.toString(), c.texture));
            if (!c.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + c.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildEvents() {
        List<HelpLine> l = new ArrayList<>();
        for (Event e : Event.values()) {
            l.add(new HelpLine(COL_C, e.name, e.texture));
            if (!e.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + e.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildPlaceholders() {
        List<HelpLine> l = new ArrayList<>();
        for (Placeholder p : Placeholder.values()) {
            StringBuilder sig = new StringBuilder(p.name);
            if (!p.constTypes.isEmpty()) {
                sig.append("(");
                for (int i = 0; i < p.constTypes.size(); i++) {
                    if (i > 0) sig.append(", ");
                    sig.append(typeStr(p.constTypes.get(i)));
                }
                sig.append(")");
            }
            sig.append(" : ").append(typeStr(p.type));
            l.add(new HelpLine(COL_C, sig.toString()));
            if (!p.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + p.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    // ── Instance state ─────────────────────────────────────────────────────────

    private Tab activeTab = Tab.OVERVIEW;
    private final Map<Tab, Integer> scrollPos = new EnumMap<>(Tab.class);
    private List<TabButton> tabButtons = Collections.emptyList();

    private boolean sbDragging = false;
    private int sbDragStartY;
    private int sbDragStartScroll;

    public HelpScreen() {
        super(Component.literal("NeoHouse Help"));
        for (Tab t : Tab.values()) scrollPos.put(t, 0);
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());

        List<TabButton> tabs = new ArrayList<>();
        int x = PAD;
        for (Tab t : Tab.values()) {
            int w = font.width(t.label) + 14;
            tabs.add(new TabButton(t, x, w));
            x += w + 2;
        }
        tabButtons = Collections.unmodifiableList(tabs);
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Header
        g.fill(0, 0, this.width, this.height, 0xCC141414);
        g.fill(0, 0, this.width, HEADER_H, 0xCC1E1E2A);
        g.drawString(this.font, "NeoHouse — Help", PAD, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, 0xFF444466);

        // Tab buttons
        int tabY = HEADER_H + 1;
        for (TabButton tb : tabButtons) {
            boolean active = tb.tab() == activeTab;
            boolean hover  = mouseX >= tb.x() && mouseX < tb.x() + tb.w()
                          && mouseY >= tabY    && mouseY < tabY + TAB_BTN_H;
            int bg = active ? 0xFF2A2A3A : hover ? 0xFF1E1E28 : 0xFF161620;
            g.fill(tb.x(), tabY, tb.x() + tb.w(), tabY + TAB_BTN_H, bg);
            int labelX = tb.x() + (tb.w() - font.width(tb.tab().label)) / 2;
            int labelY = tabY + (TAB_BTN_H - 8) / 2;
            int textColor = active ? 0xFFFFFF88 : hover ? 0xFFCCCCCC : 0xFF888899;
            g.drawString(this.font, tb.tab().label, labelX, labelY, textColor, false);
            if (active)
                g.fill(tb.x(), tabY + TAB_BTN_H - 2, tb.x() + tb.w(), tabY + TAB_BTN_H, 0xFFFFFF44);
        }
        g.fill(0, tabY + TAB_BTN_H, this.width, tabY + TAB_BTN_H + 1, 0xFF444466);

        // Widgets (Close button)
        super.render(g, mouseX, mouseY, partialTick);

        // Content
        List<HelpLine> lines = TAB_LINES.get(activeTab);
        int scroll  = clampScroll(lines);
        int listH   = this.height - CONTENT_Y - 4;
        int maxRows = listH / LINE_H;
        int end     = Math.min(scroll + maxRows, lines.size());

        int contentRight = this.width - SB_W - SB_PAD - 2;
        g.enableScissor(0, CONTENT_Y, contentRight, this.height - 4);
        for (int i = scroll; i < end; i++) {
            HelpLine line = lines.get(i);
            int y     = CONTENT_Y + (i - scroll) * LINE_H;
            int textX = PAD;
            if (line.icon() != null && !line.text().isEmpty()) {
                renderIcon(g, line.icon(), PAD, y);
                textX = PAD + ICON_PAD;
            }
            g.drawString(this.font, line.text(), textX, y, line.color(), false);
        }
        g.disableScissor();

        renderScrollbar(g, lines, scroll, maxRows);
    }

    private void renderScrollbar(GuiGraphics g, List<HelpLine> lines, int scroll, int maxRows) {
        int total = lines.size();
        if (total <= maxRows) return;

        int trackX = this.width - SB_W - SB_PAD;
        int trackY = CONTENT_Y;
        int trackH = this.height - CONTENT_Y - 4;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x33FFFFFF);

        int maxOff = total - maxRows;
        int thumbH = Math.max(16, trackH * maxRows / total);
        int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scroll / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCCCCCEE : 0x99AAAACC;
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    private int clampScroll(List<HelpLine> lines) {
        int listH   = this.height - CONTENT_Y - 4;
        int maxRows = Math.max(1, listH / LINE_H);
        int max     = Math.max(0, lines.size() - maxRows);
        int cur     = scrollPos.get(activeTab);
        if (cur > max) { scrollPos.put(activeTab, max); return max; }
        if (cur < 0)   { scrollPos.put(activeTab, 0);   return 0; }
        return cur;
    }

    private void renderIcon(GuiGraphics g, ResourceLocation texture, int x, int y) {
        Item item = BuiltInRegistries.ITEM.getValue(texture);
        if (item == Items.AIR) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.renderItem(new ItemStack(item), 0, 0);
        g.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // ── Input ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int scroll  = scrollPos.get(activeTab);
            int listH   = this.height - CONTENT_Y - 4;
            int maxRows = Math.max(1, listH / LINE_H);
            if (lines.size() > maxRows) {
                int trackX = this.width - SB_W - SB_PAD;
                int trackY = CONTENT_Y;
                int trackH = this.height - CONTENT_Y - 4;
                if (mouseX >= trackX - 2 && mouseX <= trackX + SB_W + 2) {
                    int maxOff = lines.size() - maxRows;
                    int thumbH = Math.max(16, trackH * maxRows / lines.size());
                    int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scroll / maxOff : 0);
                    if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                        sbDragging = true;
                        sbDragStartY = (int) mouseY;
                        sbDragStartScroll = scroll;
                    } else if (mouseY >= trackY && mouseY < trackY + trackH) {
                        int newScroll = (int) ((mouseY - trackY - thumbH / 2.0) * maxOff / (trackH - thumbH));
                        scrollPos.put(activeTab, Math.max(0, Math.min(maxOff, newScroll)));
                    }
                    return true;
                }
            }
        }
        int tabY = HEADER_H + 1;
        if (mouseY >= tabY && mouseY < tabY + TAB_BTN_H) {
            for (TabButton tb : tabButtons) {
                if (mouseX >= tb.x() && mouseX < tb.x() + tb.w()) {
                    activeTab = tb.tab();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (sbDragging && button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int listH   = this.height - CONTENT_Y - 4;
            int maxRows = Math.max(1, listH / LINE_H);
            int trackH  = listH;
            int thumbH  = Math.max(16, trackH * maxRows / lines.size());
            int maxOff  = lines.size() - maxRows;
            int slideH  = trackH - thumbH;
            if (slideH > 0) {
                int delta = (int) (mouseY - sbDragStartY);
                scrollPos.put(activeTab, Math.max(0, Math.min(maxOff, sbDragStartScroll + delta * maxOff / slideH)));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sbDragging && button == 0) { sbDragging = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int cur = scrollPos.get(activeTab);
        if (scrollY > 0 && cur > 0) scrollPos.put(activeTab, cur - 1);
        else if (scrollY < 0)       scrollPos.put(activeTab, cur + 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int cur   = scrollPos.get(activeTab);
        List<HelpLine> lines = TAB_LINES.get(activeTab);
        switch (key) {
            case GLFW.GLFW_KEY_UP        -> { if (cur > 0) scrollPos.put(activeTab, cur - 1); return true; }
            case GLFW.GLFW_KEY_DOWN      -> { scrollPos.put(activeTab, cur + 1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP   -> { scrollPos.put(activeTab, Math.max(0, cur - 10)); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollPos.put(activeTab, cur + 10); return true; }
            case GLFW.GLFW_KEY_HOME      -> { scrollPos.put(activeTab, 0); return true; }
            case GLFW.GLFW_KEY_END       -> { scrollPos.put(activeTab, lines.size()); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }


    @Override
    public boolean isPauseScreen() { return false; }
}
