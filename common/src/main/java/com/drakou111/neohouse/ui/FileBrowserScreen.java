package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;

import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileBrowserScreen extends ScreenBase {

    private static final int PAD      = 8;
    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 30;
    private static final int ROW_H    = 22;
    private static final int BTN_H    = 16;
    private static final int SB_W     = 4;
    private static final int SB_PAD   = 3;

    private static final String[] FILE_LABELS   = { "Edit", "Run", "Rename", "Move", "Delete" };
    private static final String[] FOLDER_LABELS = { "Open", "Rename", "Delete" };

    private final Path   rootPath;
    private final Screen parent;
    private Path currentPath;
    private final List<Path> entries = new ArrayList<>();

    private int selectedIdx  = -1;
    private int scrollOffset = 0;

    private boolean sbDragging      = false;
    private int     sbDragStartY;
    private int     sbDragStartOffset;

    private long lastClickMs   = 0;
    private Path lastClickPath = null;

    public FileBrowserScreen(Path rootPath) {
        this(rootPath, null);
    }

    public FileBrowserScreen(Path rootPath, Screen parent) {
        super(Component.literal("NeoHouse Files"));
        this.rootPath    = rootPath;
        this.parent      = parent;
        this.currentPath = rootPath;
    }

    @Override
    protected void init() {
        loadEntries();
        rebuildAll();
    }

    private void loadEntries() {
        entries.clear();
        try (Stream<Path> stream = Files.list(currentPath)) {
            stream.filter(p -> Files.isDirectory(p) || p.getFileName().toString().endsWith(".nh"))
                  .sorted(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                                    .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                  .forEach(entries::add);
        } catch (IOException ignored) {}
        scrollOffset = 0;
        selectedIdx  = -1;
    }

    private void rebuildAll() {
        clearWidgets();

        addRenderableWidget(Button.builder(
                Component.literal("X").withStyle(ChatFormatting.RED), b -> onClose())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());

        if (currentPath.equals(rootPath)) {
            addRenderableWidget(Button.builder(Component.literal("< Back"), b -> goBack())
                    .bounds(PAD, 28, 60, 16).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("< Back"), b -> navigateUp())
                    .bounds(PAD, 28, 60, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("+ File"), b -> newFile())
                .bounds(PAD + 64, 28, 60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Folder"), b -> newFolder())
                .bounds(PAD + 64 + 64, 28, 80, 16).build());

        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            Path    entry  = entries.get(selectedIdx);
            boolean isDir  = Files.isDirectory(entry);
            String[] labels = isDir ? FOLDER_LABELS : FILE_LABELS;
            int x    = this.width - PAD;
            int btnY = this.height - FOOTER_H + (FOOTER_H - BTN_H) / 2;
            for (int j = labels.length - 1; j >= 0; j--) {
                String lbl = labels[j];
                int w = Math.max(46, font.width(lbl) + 10);
                x -= (w + 4);
                int idx = j;
                addRenderableWidget(Button.builder(Component.literal(lbl), b -> {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    handleBtnClick(entry, isDir, idx);
                }).bounds(x, btnY, w, BTN_H).build());
            }
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));

        g.fill(0, 0, width, HEADER_H, t.panel(o.panelAlpha));
        g.fill(0, HEADER_H - 1, width, HEADER_H, t.accent());

        int logoSize = 18;
        int logoX = PAD;
        int textX = logoX + logoSize + 4;
        RenderHelper.renderLogo(g, logoX, (8 + 28 - logoSize) / 2, logoSize, t.accent());
        g.drawString(font, "NeoHouse - File Browser", textX, 8, 0xFFFFFFFF);
        String rel = rootPath.equals(currentPath)
                ? "/"
                : "/" + rootPath.relativize(currentPath).toString().replace('\\', '/') + "/";
        g.drawString(font, rel, textX, 20, t.muted());

        g.fill(0, height - FOOTER_H, width, height, t.panel(o.bgAlpha));
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, t.accent());
        renderFooterInfo(g, t);

        renderList(g, mouseX, mouseY);
        renderScrollbar(g, t);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFooterInfo(GuiGraphics g, UITheme t) {
        int textY = height - FOOTER_H + (FOOTER_H - 8) / 2;
        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            Path    entry = entries.get(selectedIdx);
            boolean isDir = Files.isDirectory(entry);
            int color = isDir ? 0xFFFFCC55 : 0xFFCCCCCC;
            String prefix = isDir ? "▶  " : "   ";
            g.drawString(font, prefix + entry.getFileName(), PAD, textY, color, false);
        } else {
            g.drawString(font, "Click a file or folder",
                    PAD, textY, t.divider(), false);
        }
    }

    private void renderScrollbar(GuiGraphics g, UITheme t) {
        int total   = entries.size();
        int maxRows = maxVisibleRows();
        if (total <= maxRows) return;

        int trackX = width - SB_W - SB_PAD;
        int trackY = HEADER_H;
        int trackH = height - HEADER_H - FOOTER_H - 2;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x22FFFFFF);

        int maxOff  = total - maxRows;
        int thumbH  = Math.max(16, trackH * maxRows / total);
        int thumbY  = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollOffset / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCDDDDDD : (0x77000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int listBottom = height - FOOTER_H - 2;
        int maxRows    = maxVisibleRows();
        int end        = Math.min(scrollOffset + maxRows, entries.size());

        for (int i = scrollOffset; i < end; i++) {
            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            if (rowY + ROW_H > listBottom) break;

            Path    entry    = entries.get(i);
            boolean isDir    = Files.isDirectory(entry);
            boolean hover    = mouseY >= rowY && mouseY < rowY + ROW_H && mouseX < width - SB_W - SB_PAD - 1;
            boolean selected = (i == selectedIdx);

            UITheme t   = NeoHouseConfig.get().uiTheme;
            int bg = selected ? t.accentTint(0x44) : hover ? 0x22FFFFFF : (i % 2 == 0 ? 0x0BFFFFFF : 0);
            g.fill(0, rowY, width - SB_W - SB_PAD - 1, rowY + ROW_H, bg);
            g.fill(0, rowY + ROW_H - 1, width - SB_W - SB_PAD - 1, rowY + ROW_H, t.accentTint(0x18));

            int textY  = rowY + (ROW_H - 8) / 2;
            int nameX  = PAD + 4;

            if (isDir) {
                g.drawString(font, "▶", nameX, textY, selected ? 0xFFFFBB44 : 0xFFAA7722, false);
                nameX += font.width("▶ ");
                g.drawString(font, entry.getFileName().toString(), nameX, textY,
                        selected ? 0xFFFFDD66 : 0xFFFFCC55, false);
            } else {
                nameX += font.width("  ");
                String name = entry.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    String base = name.substring(0, dot);
                    String ext  = name.substring(dot);
                    int nameColor = selected ? 0xFFEEEEEE : 0xFFCCCCCC;
                    g.drawString(font, base, nameX, textY, nameColor, false);
                    g.drawString(font, ext, nameX + font.width(base), textY,
                            selected ? t.muted() : t.accent(), false);
                } else {
                    g.drawString(font, name, nameX, textY, selected ? 0xFFEEEEEE : 0xFFCCCCCC, false);
                }
            }
        }
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int total   = entries.size();
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
                        sbDragging        = true;
                        sbDragStartY      = (int) my;
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
            int end     = Math.min(scrollOffset + maxRows, entries.size());

            for (int i = scrollOffset; i < end; i++) {
                int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
                if (my < rowY || my >= rowY + ROW_H) continue;

                Path    entry  = entries.get(i);
                boolean isDir  = Files.isDirectory(entry);
                long    now    = System.currentTimeMillis();
                boolean dbl    = entry.equals(lastClickPath) && (now - lastClickMs) < 400;
                lastClickMs   = now;
                lastClickPath = entry;

                if (dbl) {
                    if (isDir) navigateInto(entry);
                    else editFile(entry);
                } else {
                    selectedIdx = (selectedIdx == i) ? -1 : i;
                    rebuildAll();
                }
                return true;
            }

            if (selectedIdx != -1) {
                selectedIdx = -1;
                rebuildAll();
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (sbDragging && button == 0) {
            int total   = entries.size();
            int maxRows = maxVisibleRows();
            if (total > maxRows) {
                int trackH = height - HEADER_H - FOOTER_H - 2;
                int thumbH = Math.max(16, trackH * maxRows / total);
                int maxOff = total - maxRows;
                int slideH = trackH - thumbH;
                if (slideH > 0) {
                    int delta = (int) (my - sbDragStartY);
                    scrollOffset = Math.max(0, Math.min(maxOff,
                            sbDragStartOffset + delta * maxOff / slideH));
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
        int maxOff = Math.max(0, entries.size() - maxVisibleRows());
        if      (scrollY > 0 && scrollOffset > 0)     scrollOffset--;
        else if (scrollY < 0 && scrollOffset < maxOff) scrollOffset++;
        return true;
    }

    private void handleBtnClick(Path entry, boolean isDir, int idx) {
        if (isDir) {
            switch (idx) {
                case 0 -> navigateInto(entry);
                case 1 -> renameEntry(entry);
                case 2 -> deleteEntry(entry);
            }
        } else {
            switch (idx) {
                case 0 -> editFile(entry);
                case 1 -> runFile(entry);
                case 2 -> renameEntry(entry);
                case 3 -> moveFile(entry);
                case 4 -> deleteEntry(entry);
            }
        }
    }

    private void goBack() {
        if (parent != null) minecraft.setScreen(parent);
        else onClose();
    }

    private void navigateUp() {
        if (!currentPath.equals(rootPath)) {
            currentPath = currentPath.getParent();
            loadEntries();
            rebuildAll();
        }
    }

    private void navigateInto(Path dir) {
        currentPath = dir;
        loadEntries();
        rebuildAll();
    }

    private void editFile(Path file) {
        Minecraft.getInstance().setScreen(new CodeEditorScreen(file, this));
    }

    private void runFile(Path file) {
        onClose();
        try {
            ScriptRunner.INSTANCE.runScript(file);
            MessageUtils.debugToChat("Running: " + file.getFileName());
        } catch (Exception e) {
            MessageUtils.errorToChat(e);
        }
    }

    private void renameEntry(Path entry) {
        String current = entry.getFileName().toString();
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Rename \"" + current + "\" to:", current, this,
                newName -> {
                    if (!newName.isEmpty() && !newName.equals(current)) {
                        try { Files.move(entry, entry.getParent().resolve(newName)); }
                        catch (IOException e) { MessageUtils.errorToChat("Rename failed: " + e.getMessage()); }
                    }
                }
        ));
    }

    private void moveFile(Path file) {
        String relPath = rootPath.relativize(file).toString().replace('\\', '/');
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Move to:", relPath, this,
                newPath -> {
                    if (!newPath.isEmpty()) {
                        if (!newPath.endsWith(".nh")) newPath += ".nh";
                        Path target = rootPath.resolve(newPath);
                        try {
                            Files.createDirectories(target.getParent());
                            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) { MessageUtils.errorToChat("Move failed: " + e.getMessage()); }
                    }
                }
        ));
    }

    private void deleteEntry(Path entry) {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Delete \"" + entry.getFileName() + "\"?", this,
                () -> {
                    try { deleteRecursive(entry); }
                    catch (IOException e) { MessageUtils.errorToChat("Delete failed: " + e.getMessage()); }
                }
        ));
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> s = Files.list(path)) {
                for (Path child : s.toList()) deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }

    private void newFile() {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "New file name:", "new.nh", this,
                name -> {
                    if (!name.endsWith(".nh")) name += ".nh";
                    Path target = currentPath.resolve(name);
                    if (Files.exists(target)) { MessageUtils.errorToChat("\"" + name + "\" already exists."); return; }
                    try { Files.createFile(target); }
                    catch (IOException e) { MessageUtils.errorToChat("Create failed: " + e.getMessage()); }
                }
        ));
    }

    private void newFolder() {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "New folder name:", "folder", this,
                name -> {
                    Path target = currentPath.resolve(name);
                    if (Files.exists(target)) { MessageUtils.errorToChat("\"" + name + "\" already exists."); return; }
                    try { Files.createDirectories(target); }
                    catch (IOException e) { MessageUtils.errorToChat("Create failed: " + e.getMessage()); }
                }
        ));
    }

    private int maxVisibleRows() {
        return Math.max(1, (height - HEADER_H - FOOTER_H - 2) / ROW_H);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
