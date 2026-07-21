package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.constants.Sound;
import com.drakou111.neohouse.music.MusicPlayer;
import com.drakou111.neohouse.music.SoundEventResolver;
import com.drakou111.neohouse.music.SoundIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Searchable, scrollable picker listing every {@link Sound} by display name with
 * its item icon. Single-click selects and auditions; double-click (or Select)
 * confirms. When {@code melodicOnly} is set only note-block instruments are
 * shown.
 */
public class SoundPickerScreen extends ScreenBase {

    private static final int PAD = 8, HEADER_H = 50, FOOTER_H = 30, ROW_H = 18, SB_W = 4, SB_PAD = 3;

    private final Screen parent;
    private final boolean melodicOnly;
    private final Consumer<Sound> onPick;

    private final List<Sound> all = new ArrayList<>();
    private final List<Sound> filtered = new ArrayList<>();

    private EditBox searchBox;
    private Sound selected;
    private int scrollOffset = 0;

    private boolean sbDragging = false;
    private int sbDragStartY, sbDragStartOffset;
    private long lastClickMs = 0;
    private Sound lastClickSound = null;

    public SoundPickerScreen(Screen parent, boolean melodicOnly, Sound current, Consumer<Sound> onPick) {
        super(Component.literal("Pick Sound"));
        this.parent = parent;
        this.melodicOnly = melodicOnly;
        this.onPick = onPick;
        this.selected = current;
        for (Sound s : Sound.values()) {
            if (!melodicOnly || SoundEventResolver.isMelodic(s)) all.add(s);
        }
        all.sort(Comparator.comparing(s -> s.displayName.toLowerCase()));
    }

    @Override
    protected void init() {
        searchBox = new EditBox(this.font, PAD, 28, this.width - 2 * PAD - 4, 16, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search sounds…"));
        searchBox.setResponder(s -> applyFilter());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
        applyFilter();
        rebuildButtons();
    }

    private void rebuildButtons() {
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> cancel())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());

        int btnW = 80, btnH = 16;
        int btnY = this.height - FOOTER_H + (FOOTER_H - btnH) / 2;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancel())
                .bounds(this.width - PAD - btnW, btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("✓ Select").withStyle(ChatFormatting.GREEN), b -> confirm())
                .bounds(this.width - PAD - btnW - 4 - btnW, btnY, btnW, btnH).build());
    }

    private void applyFilter() {
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
        filtered.clear();
        for (Sound s : all) {
            if (q.isEmpty() || s.displayName.toLowerCase().contains(q) || s.name().toLowerCase().contains(q)) {
                filtered.add(s);
            }
        }
        scrollOffset = 0;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));

        g.fill(0, 0, width, HEADER_H, t.panel(o.panelAlpha));
        g.fill(0, HEADER_H - 1, width, HEADER_H, t.accent());
        int logoSize = 18;
        RenderHelper.renderLogo(g, PAD, 4, logoSize, t.accent());
        g.drawString(font, melodicOnly ? "Pick Sound — note blocks only" : "Pick Sound", PAD + logoSize + 4, 9, 0xFFFFFFFF);

        g.fill(0, height - FOOTER_H, width, height, t.panel(o.bgAlpha));
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, t.accent());
        int textY = height - FOOTER_H + (FOOTER_H - 8) / 2;
        String info = selected != null ? "Selected: " + selected.displayName : "Click a sound to preview it";
        g.drawString(font, info, PAD, textY, selected != null ? 0xFFCCCCCC : t.divider(), false);

        renderList(g, mouseX, mouseY, t);
        renderScrollbar(g, t);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY, UITheme t) {
        int listBottom = height - FOOTER_H - 2;
        int maxRows = maxVisibleRows();
        int end = Math.min(scrollOffset + maxRows, filtered.size());
        for (int i = scrollOffset; i < end; i++) {
            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            if (rowY + ROW_H > listBottom) break;
            Sound s = filtered.get(i);
            boolean hover = mouseY >= rowY && mouseY < rowY + ROW_H && mouseX < width - SB_W - SB_PAD - 1;
            boolean sel = s == selected;
            int bg = sel ? t.accentTint(0x44) : hover ? 0x22FFFFFF : (i % 2 == 0 ? 0x0BFFFFFF : 0);
            g.fill(0, rowY, width - SB_W - SB_PAD - 1, rowY + ROW_H, bg);

            g.renderItem(SoundIcon.iconFor(s), PAD, rowY + 1);
            int textY = rowY + (ROW_H - 8) / 2;
            g.drawString(font, s.displayName, PAD + 20, textY, sel ? 0xFFFFFFFF : 0xFFCCCCCC, false);
            String en = s.name();
            g.drawString(font, en, width - SB_W - SB_PAD - 6 - font.width(en), textY, t.muted(), false);
        }
        if (filtered.isEmpty()) {
            g.drawCenteredString(font, "No sounds match your search.", width / 2, HEADER_H + 20, t.muted());
        }
    }

    private void renderScrollbar(GuiGraphics g, UITheme t) {
        int total = filtered.size();
        int maxRows = maxVisibleRows();
        if (total <= maxRows) return;
        int trackX = width - SB_W - SB_PAD;
        int trackY = HEADER_H;
        int trackH = height - HEADER_H - FOOTER_H - 2;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x22FFFFFF);
        int maxOff = total - maxRows;
        int thumbH = Math.max(16, trackH * maxRows / total);
        int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollOffset / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCDDDDDD : (0x77000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int total = filtered.size();
            int maxRows = maxVisibleRows();
            if (total > maxRows) {
                int trackX = width - SB_W - SB_PAD;
                int trackY = HEADER_H;
                int trackH = height - HEADER_H - FOOTER_H - 2;
                if (mx >= trackX - 2 && mx <= trackX + SB_W + 2) {
                    int maxOff = total - maxRows;
                    int thumbH = Math.max(16, trackH * maxRows / total);
                    int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollOffset / maxOff : 0);
                    if (my >= thumbY && my < thumbY + thumbH) {
                        sbDragging = true;
                        sbDragStartY = (int) my;
                        sbDragStartOffset = scrollOffset;
                    } else if (my >= trackY && my < trackY + trackH) {
                        int newOff = (int) ((my - trackY - thumbH / 2.0) * maxOff / (trackH - thumbH));
                        scrollOffset = Math.max(0, Math.min(maxOff, newOff));
                    }
                    return true;
                }
            }
        }

        if (superMouseClicked(mx, my, button)) return true;

        if (my >= HEADER_H && my < height - FOOTER_H) {
            int maxRows = maxVisibleRows();
            int end = Math.min(scrollOffset + maxRows, filtered.size());
            for (int i = scrollOffset; i < end; i++) {
                int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
                if (my < rowY || my >= rowY + ROW_H) continue;
                Sound s = filtered.get(i);
                long now = System.currentTimeMillis();
                boolean dbl = s == lastClickSound && (now - lastClickMs) < 400;
                lastClickMs = now;
                lastClickSound = s;
                selected = s;
                if (dbl) {
                    confirm();
                } else {
                    float pitch = SoundEventResolver.isMelodic(s) ? 1.0f : 1.0f;
                    MusicPlayer.playNote(s, 1.0f, pitch);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (sbDragging && button == 0) {
            int total = filtered.size();
            int maxRows = maxVisibleRows();
            if (total > maxRows) {
                int trackH = height - HEADER_H - FOOTER_H - 2;
                int thumbH = Math.max(16, trackH * maxRows / total);
                int maxOff = total - maxRows;
                int slideH = trackH - thumbH;
                if (slideH > 0) {
                    int delta = (int) (my - sbDragStartY);
                    scrollOffset = Math.max(0, Math.min(maxOff, sbDragStartOffset + delta * maxOff / slideH));
                }
            }
            return true;
        }
        return superMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int button) {
        if (sbDragging && button == 0) { sbDragging = false; return true; }
        return superMouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOff = Math.max(0, filtered.size() - maxVisibleRows());
        if (scrollY > 0 && scrollOffset > 0) scrollOffset--;
        else if (scrollY < 0 && scrollOffset < maxOff) scrollOffset++;
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        if (key == 256) { cancel(); return true; }                 // esc
        if ((key == 257 || key == 335) && selected != null) { confirm(); return true; } // enter
        return superKeyPressed(key, scan, mods);
    }

    private void confirm() {
        if (selected != null) onPick.accept(selected);
        Minecraft.getInstance().setScreen(parent);
    }

    private void cancel() {
        Minecraft.getInstance().setScreen(parent);
    }

    private int maxVisibleRows() {
        return Math.max(1, (height - HEADER_H - FOOTER_H - 2) / ROW_H);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
