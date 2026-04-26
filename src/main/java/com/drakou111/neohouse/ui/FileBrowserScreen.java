package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileBrowserScreen extends Screen {
    private static final ScriptRunner RUNNER = new ScriptRunner(new NHCompiler());

    private static final int PAD = 8;
    private static final int HEADER_H = 54;
    private static final int ROW_H = 22;
    private static final int BTN_H = 16;
    private static final int SB_W = 4;
    private static final int SB_PAD = 3;

    private static final String[] FILE_LABELS   = {"Edit", "Run", "Rename", "Move", "Delete", "Reveal"};
    private static final String[] FOLDER_LABELS = {"Open", "Rename", "Delete"};

    private final Path rootPath;
    private Path currentPath;
    private final List<Path> entries = new ArrayList<>();

    private final List<Button> rowButtons = new ArrayList<>();

    private int scrollOffset = 0;

    private boolean sbDragging = false;
    private int sbDragStartY;
    private int sbDragStartOffset;

    private long lastClickMs = 0;
    private Path lastClickPath = null;

    public FileBrowserScreen(Path rootPath) {
        super(Component.literal("NeoHouse Files"));
        this.rootPath = rootPath;
        this.currentPath = rootPath;
    }

    @Override
    protected void init() {
        loadEntries();
        rebuildHeaderButtons();
        rebuildRowButtons();
    }

    private void loadEntries() {
        entries.clear();
        try (Stream<Path> stream = Files.list(currentPath)) {
            stream.sorted(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                                    .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                  .forEach(entries::add);
        } catch (IOException ignored) {}

        scrollOffset = 0;
    }

    private void rebuildHeaderButtons() {
        clearWidgets();

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                                  .bounds(this.width - PAD - 18, 5, 18, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Config"), b -> openConfig())
                                  .bounds(this.width - PAD - 18 - 4 - 54, 5, 54, 18).build());

        int leftX = PAD;

        if (!currentPath.equals(rootPath)) {
            addRenderableWidget(Button.builder(Component.literal("< Back"), b -> navigateUp())
                                      .bounds(leftX, 32, 60, 16).build());
            leftX += 64;
        }

        addRenderableWidget(Button.builder(Component.literal("+ File"), b -> newFile())
                                  .bounds(leftX, 32, 60, 16).build());

        addRenderableWidget(Button.builder(Component.literal("+ Folder"), b -> newFolder())
                                  .bounds(leftX + 64, 32, 80, 16).build());
    }

    private void rebuildRowButtons() {
        for (Button b : rowButtons) removeWidget(b);
        rowButtons.clear();

        int maxRows = maxVisibleRows();
        int end = Math.min(scrollOffset + maxRows, entries.size());

        for (int i = scrollOffset; i < end; i++) {
            Path entry = entries.get(i);
            boolean isDir = Files.isDirectory(entry);

            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            int btnY = rowY + (ROW_H - BTN_H) / 2;

            String[] labels = isDir ? FOLDER_LABELS : FILE_LABELS;

            int x = this.width - PAD;

            for (int j = labels.length - 1; j >= 0; j--) {
                String label = labels[j];
                int width = Math.max(50, font.width(label) + 10);
                x -= (width + 4);

                int idx = j;

                Button btn = Button.builder(Component.literal(label), b -> {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    handleBtnClick(entry, isDir, idx);
                }).bounds(x, btnY, width, BTN_H).build();

                rowButtons.add(btn);
                addRenderableWidget(btn);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC111111);
        g.fill(0, 0, width, HEADER_H, 0xCC1A1A2A);

        g.drawString(font, "NeoHouse File Browser", PAD, 7, 0xFFFFFFFF);

        String rel = rootPath.equals(currentPath)
                     ? "/"
                     : "/" + rootPath.relativize(currentPath).toString().replace('\\', '/') + "/";

        g.drawString(font, rel, PAD, 19, 0xFF9999BB);

        g.fill(0, HEADER_H - 1, width, HEADER_H, 0xFF444466);

        renderList(g, mouseX, mouseY);
        renderScrollbar(g);

        super.render(g, mouseX, mouseY, partialTick);

        if (entries.isEmpty()) {
            g.drawString(font, "(empty)", PAD + 4, HEADER_H + 6, 0xFF555577);
        }
    }

    private void renderScrollbar(GuiGraphics g) {
        int total = entries.size();
        int maxRows = maxVisibleRows();
        if (total <= maxRows) return;

        int trackX = width - SB_W - SB_PAD;
        int trackY = HEADER_H;
        int trackH = height - HEADER_H - 4;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x33FFFFFF);

        int maxOff  = total - maxRows;
        int thumbH  = Math.max(16, trackH * maxRows / total);
        int thumbY  = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollOffset / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCCCCCEE : 0x99AAAACC;
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int maxRows = maxVisibleRows();
        int end = Math.min(scrollOffset + maxRows, entries.size());

        for (int i = scrollOffset; i < end; i++) {
            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            Path entry = entries.get(i);

            boolean isDir = Files.isDirectory(entry);

            g.fill(0, rowY, width, rowY + ROW_H,
                    mouseY >= rowY && mouseY < rowY + ROW_H ? 0x22FFFFFF
                    : i % 2 == 0 ? 0x0AFFFFFF : 0x00000000);
            g.fill(0, rowY + ROW_H - 1, width - SB_W - SB_PAD - 1, rowY + ROW_H, 0x20444466);

            String name = isDir ? "[" + entry.getFileName() + "]" : entry.getFileName().toString();
            int color = isDir ? 0xFFFFCC55 : 0xFFCCCCFF;

            g.drawString(font, name, PAD + 4, rowY + (ROW_H - 8) / 2, color);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int total = entries.size();
            int maxRows = maxVisibleRows();
            if (total > maxRows) {
                int trackX = width - SB_W - SB_PAD;
                int trackY = HEADER_H;
                int trackH = height - HEADER_H - 4;
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
                        rebuildRowButtons();
                    }
                    return true;
                }
            }
        }
        if (super.mouseClicked(mx, my, button)) return true;

        int maxRows = maxVisibleRows();
        int end = Math.min(scrollOffset + maxRows, entries.size());

        for (int i = scrollOffset; i < end; i++) {
            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            if (my < rowY || my >= rowY + ROW_H) continue;

            Path entry = entries.get(i);
            boolean isDir = Files.isDirectory(entry);

            long now = System.currentTimeMillis();
            boolean isDouble = entry.equals(lastClickPath) && (now - lastClickMs) < 400;

            lastClickMs = now;
            lastClickPath = entry;

            if (isDouble) {
                if (isDir) navigateInto(entry);
                else editFile(entry);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (sbDragging && button == 0) {
            int total = entries.size();
            int maxRows = maxVisibleRows();
            if (total > maxRows) {
                int trackH = height - HEADER_H - 4;
                int thumbH = Math.max(16, trackH * maxRows / total);
                int maxOff = total - maxRows;
                int slideH = trackH - thumbH;
                if (slideH > 0) {
                    int delta = (int) (my - sbDragStartY);
                    scrollOffset = Math.max(0, Math.min(maxOff, sbDragStartOffset + delta * maxOff / slideH));
                    rebuildRowButtons();
                }
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (sbDragging && button == 0) { sbDragging = false; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxRows = maxVisibleRows();
        int maxOff = Math.max(0, entries.size() - maxRows);

        if (scrollY > 0 && scrollOffset > 0) scrollOffset--;
        else if (scrollY < 0 && scrollOffset < maxOff) scrollOffset++;

        rebuildRowButtons();
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
                case 5 -> openInExplorer(entry);
            }
        }
    }

    private void navigateUp() {
        if (!currentPath.equals(rootPath)) {
            currentPath = currentPath.getParent();
            loadEntries();
            rebuildHeaderButtons();
            rebuildRowButtons();
        }
    }

    private void navigateInto(Path dir) {
        currentPath = dir;
        loadEntries();
        rebuildHeaderButtons();
        rebuildRowButtons();
    }

    private void editFile(Path file) {
        Minecraft.getInstance().setScreen(new CodeEditorScreen(file, this));
    }

    private void runFile(Path file) {
        onClose();
        try {
            RUNNER.runScript(file);
            MessageUtils.debugToChat("Running: " + file.getFileName());
        } catch (Exception e) {
            MessageUtils.errorToChat(e.getMessage());
        }
    }

    private void renameEntry(Path entry) {
        String current = entry.getFileName().toString();
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Rename \"" + current + "\" to:",
                current,
                this,
                newName -> {
                    if (!newName.isEmpty() && !newName.equals(current)) {
                        try {
                            Files.move(entry, entry.getParent().resolve(newName));
                        } catch (IOException e) {
                            MessageUtils.errorToChat("Rename failed: " + e.getMessage());
                        }
                    }
                }
        ));
    }

    private void moveFile(Path file) {
        String relPath = rootPath.relativize(file).toString().replace('\\', '/');
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Move to:",
                relPath,
                this,
                newPath -> {
                    if (!newPath.isEmpty()) {
                        if (!newPath.endsWith(".nh")) newPath += ".nh";
                        Path target = rootPath.resolve(newPath);
                        try {
                            Files.createDirectories(target.getParent());
                            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            MessageUtils.errorToChat("Move failed: " + e.getMessage());
                        }
                    }
                }
        ));
    }

    private void deleteEntry(Path entry) {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "Delete \"" + entry.getFileName() + "\"?",
                this,
                () -> {
                    try {
                        deleteRecursive(entry);
                    } catch (IOException e) {
                        MessageUtils.errorToChat("Delete failed: " + e.getMessage());
                    }
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

    private void openInExplorer(Path file) {
        try {
            new ProcessBuilder("explorer", file.getParent().toString()).start();
        } catch (IOException e) {
            MessageUtils.errorToChat("Explorer failed: " + e.getMessage());
        }
    }

    private void newFile() {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "New file:",
                "new.nh",
                this,
                name -> {
                    try {
                        Files.createFile(currentPath.resolve(name));
                    } catch (IOException e) {
                        MessageUtils.errorToChat("Create failed: " + e.getMessage());
                    }
                }
        ));
    }

    private void openConfig() {
        Minecraft.getInstance().setScreen(new ConfigScreen());
    }

    private void newFolder() {
        Minecraft.getInstance().setScreen(new InputDialogScreen(
                "New folder:",
                "folder",
                this,
                name -> {
                    try {
                        Files.createDirectories(currentPath.resolve(name));
                    } catch (IOException e) {
                        MessageUtils.errorToChat("Create failed: " + e.getMessage());
                    }
                }
        ));
    }

    private int maxVisibleRows() {
        return Math.max(1, (height - HEADER_H - 4) / ROW_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}