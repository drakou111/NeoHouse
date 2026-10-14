package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.music.MusicFunctionExporter;
import com.drakou111.neohouse.music.MusicIO;
import com.drakou111.neohouse.music.MusicProject;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Browses the NeoHouse folder: scripts, music projects, and MIDI / Note Block Studio files to import,
 * each shown with its own icon and actions.
 */
public class FileBrowserScreen extends ScreenBase {

    private static final int PAD      = 8;
    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 30;
    private static final int ROW_H    = 22;
    private static final int BTN_H    = 16;
    private static final int SB_W     = 4;
    private static final int SB_PAD   = 3;
    private static final int ICON_SIZE = 16;
    private static final int DOUBLE_CLICK_MS = 400;
    private static final String SCRIPT_EXTENSION = ".nh";

    /** What an entry is, decided by its extension; each kind has an icon and its actions (the first is the double-click one). */
    private enum EntryKind {
        FOLDER(() -> Items.CHEST, 0xFFFFCC55, "Open", "Rename", "Delete"),
        SCRIPT(() -> Items.WRITABLE_BOOK, 0xFFCCCCCC, "Edit", "Run", "Rename", "Move", "Delete"),
        MUSIC(() -> Items.NOTE_BLOCK, 0xFF99DDFF, "Open", "Export", "Rename", "Move", "Delete"),
        MIDI(() -> Items.JUKEBOX, 0xFFCCAAFF, "Import", "Rename", "Move", "Delete"),
        NOTE_BLOCK_SONG(() -> Items.MUSIC_DISC_CAT, 0xFFAAFFAA, "Import", "Rename", "Move", "Delete");

        private final Supplier<Item> icon;
        final int nameColor;
        final List<String> actions;

        EntryKind(Supplier<Item> icon, int nameColor, String... actions) {
            this.icon = icon;
            this.nameColor = nameColor;
            this.actions = List.of(actions);
        }

        ItemStack icon() {
            return new ItemStack(this.icon.get());
        }

        static Optional<EntryKind> of(Path path) {
            if (Files.isDirectory(path)) return Optional.of(FOLDER);
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(SCRIPT_EXTENSION)) return Optional.of(SCRIPT);
            if (name.endsWith(MusicIO.EXTENSION)) return Optional.of(MUSIC);
            if (name.endsWith(".mid") || name.endsWith(".midi")) return Optional.of(MIDI);
            if (name.endsWith(".nbs")) return Optional.of(NOTE_BLOCK_SONG);
            return Optional.empty();
        }
    }

    private record Entry(Path path, EntryKind kind) {}

    private final Path   rootPath;
    private final Screen parent;
    private Path currentPath;
    private final List<Entry> entries = new ArrayList<>();

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
            stream.sorted(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                                    .thenComparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                  .forEach(p -> EntryKind.of(p).ifPresent(kind -> entries.add(new Entry(p, kind))));
        } catch (IOException e) {
            MessageUtils.errorToChat("Could not list " + currentPath.getFileName() + ": " + e.getMessage());
        }
        scrollOffset = 0;
        selectedIdx  = -1;
    }

    private void rebuildAll() {
        clearWidgets();

        addRenderableWidget(Button.builder(
                Component.literal("X").withStyle(ChatFormatting.RED), b -> onClose())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());

        int x = PAD;
        x = addHeaderButton("< Back", 60, x, b -> { if (currentPath.equals(rootPath)) goBack(); else navigateUp(); });
        x = addHeaderButton("+ Script", 60, x, b -> newScript());
        x = addHeaderButton("+ Music", 60, x, b -> newMusic());
        addHeaderButton("+ Folder", 60, x, b -> newFolder());

        if (selectedIdx >= 0 && selectedIdx < entries.size()) {
            Entry entry = entries.get(selectedIdx);
            List<String> labels = entry.kind().actions;
            int right = this.width - PAD;
            int btnY = this.height - FOOTER_H + (FOOTER_H - BTN_H) / 2;
            for (int j = labels.size() - 1; j >= 0; j--) {
                String label = labels.get(j);
                int w = Math.max(46, font.width(label) + 10);
                right -= (w + 4);
                addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    runAction(entry, label);
                }).bounds(right, btnY, w, BTN_H).build());
            }
        }
    }

    private int addHeaderButton(String label, int w, int x, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(Component.literal(label), onPress).bounds(x, 28, w, 16).build());
        return x + w + 4;
    }

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));

        g.fill(0, 0, width, HEADER_H, t.panel(o.panelAlpha));
        g.fill(0, HEADER_H - 1, width, HEADER_H, t.accent());

        int logoSize = 18;
        int textX = PAD + logoSize + 4;
        RenderHelper.renderLogo(g, PAD, (8 + 28 - logoSize) / 2, logoSize, t.accent());
        g.drawString(font, "NeoHouse - Files", textX, 8, 0xFFFFFFFF);
        String rel = rootPath.equals(currentPath)
                ? "/"
                : "/" + rootPath.relativize(currentPath).toString().replace('\\', '/') + "/";
        g.drawString(font, rel, textX, 20, t.muted());

        g.fill(0, height - FOOTER_H, width, height, t.panel(o.bgAlpha));
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, t.accent());
        renderFooterInfo(g, t);

        renderList(g, mouseX, mouseY);
        renderScrollbar(g, t);

        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    private void renderFooterInfo(Gfx g, UITheme t) {
        if (selectedIdx >= 0 && selectedIdx < entries.size()) return; // the action buttons take the footer
        int textY = height - FOOTER_H + (FOOTER_H - 8) / 2;
        g.drawString(font, "Double-click to open. Scripts .nh, music .nhm, imports .mid .nbs",
                PAD, textY, t.divider(), false);
    }

    private void renderScrollbar(Gfx g, UITheme t) {
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

    private void renderList(Gfx g, int mouseX, int mouseY) {
        int listBottom = height - FOOTER_H - 2;
        int end        = Math.min(scrollOffset + maxVisibleRows(), entries.size());
        UITheme t      = NeoHouseConfig.get().uiTheme;

        if (entries.isEmpty()) {
            g.drawString(font, "This folder is empty. Use + Script or + Music, or drop .mid / .nbs files here.",
                    PAD + 4, HEADER_H + 8, t.divider(), false);
            return;
        }

        for (int i = scrollOffset; i < end; i++) {
            int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
            if (rowY + ROW_H > listBottom) break;

            Entry   entry    = entries.get(i);
            boolean hover    = mouseY >= rowY && mouseY < rowY + ROW_H && mouseX < width - SB_W - SB_PAD - 1;
            boolean selected = (i == selectedIdx);

            int bg = selected ? t.accentTint(0x44) : hover ? 0x22FFFFFF : (i % 2 == 0 ? 0x0BFFFFFF : 0);
            g.fill(0, rowY, width - SB_W - SB_PAD - 1, rowY + ROW_H, bg);
            g.fill(0, rowY + ROW_H - 1, width - SB_W - SB_PAD - 1, rowY + ROW_H, t.accentTint(0x18));

            int iconX = PAD + 2;
            g.renderItem(entry.kind().icon(), iconX, rowY + (ROW_H - ICON_SIZE) / 2);

            int textY = rowY + (ROW_H - 8) / 2;
            int nameX = iconX + ICON_SIZE + 6;
            String name = entry.path().getFileName().toString();
            int dot = entry.kind() == EntryKind.FOLDER ? -1 : name.lastIndexOf('.');
            int nameColor = selected ? 0xFFFFFFFF : entry.kind().nameColor;
            if (dot > 0) {
                String base = name.substring(0, dot);
                g.drawString(font, base, nameX, textY, nameColor, false);
                g.drawString(font, name.substring(dot), nameX + font.width(base), textY, t.muted(), false);
            } else {
                g.drawString(font, name, nameX, textY, nameColor, false);
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
            int end = Math.min(scrollOffset + maxVisibleRows(), entries.size());
            for (int i = scrollOffset; i < end; i++) {
                int rowY = HEADER_H + (i - scrollOffset) * ROW_H;
                if (my < rowY || my >= rowY + ROW_H) continue;

                Entry   entry = entries.get(i);
                long    now   = System.currentTimeMillis();
                boolean dbl   = entry.path().equals(lastClickPath) && (now - lastClickMs) < DOUBLE_CLICK_MS;
                lastClickMs   = now;
                lastClickPath = entry.path();

                if (dbl) {
                    runAction(entry, entry.kind().actions.getFirst());
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
        int maxOff = Math.max(0, entries.size() - maxVisibleRows());
        if      (scrollY > 0 && scrollOffset > 0)     scrollOffset--;
        else if (scrollY < 0 && scrollOffset < maxOff) scrollOffset++;
        return true;
    }

    private void runAction(Entry entry, String action) {
        Path path = entry.path();
        switch (action) {
            case "Open" -> {
                if (entry.kind() == EntryKind.FOLDER) navigateInto(path);
                else open(new MusicEditorScreen(path, this));
            }
            case "Edit" -> open(new CodeEditorScreen(path, this));
            case "Run" -> runScript(path);
            case "Export" -> exportMusic(path);
            case "Import" -> open(new MusicImportScreen(this, path, currentPath));
            case "Rename" -> renameEntry(path);
            case "Move" -> moveEntry(path);
            case "Delete" -> deleteEntry(path);
            default -> throw new IllegalStateException("Unknown file action: " + action);
        }
    }

    private static void open(Screen screen) {
        ClientUi.setScreen(Minecraft.getInstance(), screen);
    }

    private void goBack() {
        if (parent != null) ClientUi.setScreen(minecraft, parent);
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

    private void runScript(Path file) {
        onClose();
        try {
            ScriptRunner.INSTANCE.runScript(file);
            MessageUtils.debugToChat("Running: " + file.getFileName());
        } catch (Exception e) {
            MessageUtils.errorToChat(e);
        }
    }

    /** Writes a music project as a script function next to it. */
    private void exportMusic(Path file) {
        final MusicProject project;
        try {
            project = MusicIO.load(file);
        } catch (IOException e) {
            MessageUtils.errorToChat("Export failed: " + e.getMessage());
            return;
        }
        String suggested = MusicFunctionExporter.sanitizeFunctionName(project.name);
        Path folder = file.getParent();
        open(new InputDialogScreen("Export as function - file name:", suggested, this, raw -> {
            String fnName = MusicFunctionExporter.sanitizeFunctionName(raw);
            Path target = folder.resolve(fnName + SCRIPT_EXTENSION);
            if (Files.exists(target)) {
                open(new InputDialogScreen("\"" + target.getFileName() + "\" already exists. Overwrite?", this,
                        () -> writeExport(project, fnName, target)));
            } else {
                writeExport(project, fnName, target);
            }
        }));
    }

    private void writeExport(MusicProject project, String fnName, Path target) {
        try {
            Files.writeString(target, MusicFunctionExporter.toScript(project, fnName));
            MessageUtils.debugToChat("Exported " + target.getFileName() + " - run it like any other script.");
        } catch (IOException e) {
            MessageUtils.errorToChat("Export failed: " + e.getMessage());
        }
    }

    private void renameEntry(Path entry) {
        String current = entry.getFileName().toString();
        open(new InputDialogScreen("Rename \"" + current + "\" to:", current, this, newName -> {
            if (newName.isEmpty() || newName.equals(current)) return;
            Path target = entry.resolveSibling(newName);
            if (Files.exists(target)) { MessageUtils.errorToChat("\"" + newName + "\" already exists."); return; }
            try { Files.move(entry, target); }
            catch (IOException e) { MessageUtils.errorToChat("Rename failed: " + e.getMessage()); }
        }));
    }

    /** Moves a file to another path inside the NeoHouse folder, keeping its extension and never overwriting. */
    private void moveEntry(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot > 0 ? name.substring(dot) : "";
        String relPath = rootPath.relativize(file).toString().replace('\\', '/');
        open(new InputDialogScreen("Move to:", relPath, this, newPath -> {
            if (newPath.isEmpty()) return;
            if (!newPath.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT))) newPath += extension;
            Path target = rootPath.resolve(newPath).normalize();
            if (!target.startsWith(rootPath)) { MessageUtils.errorToChat("Files can only be moved inside the NeoHouse folder."); return; }
            if (Files.exists(target)) { MessageUtils.errorToChat("\"" + newPath + "\" already exists."); return; }
            try {
                Files.createDirectories(target.getParent());
                Files.move(file, target);
            } catch (IOException e) { MessageUtils.errorToChat("Move failed: " + e.getMessage()); }
        }));
    }

    private void deleteEntry(Path entry) {
        open(new InputDialogScreen("Delete \"" + entry.getFileName() + "\"?", this, () -> {
            try { deleteRecursive(entry); }
            catch (IOException e) { MessageUtils.errorToChat("Delete failed: " + e.getMessage()); }
        }));
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> s = Files.list(path)) {
                for (Path child : s.toList()) deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }

    private void newScript() {
        open(new InputDialogScreen("New script name:", "new" + SCRIPT_EXTENSION, this, name -> {
            if (!name.endsWith(SCRIPT_EXTENSION)) name += SCRIPT_EXTENSION;
            Path target = currentPath.resolve(name);
            if (Files.exists(target)) { MessageUtils.errorToChat("\"" + name + "\" already exists."); return; }
            try { Files.createFile(target); }
            catch (IOException e) { MessageUtils.errorToChat("Create failed: " + e.getMessage()); }
        }));
    }

    private void newMusic() {
        open(new InputDialogScreen("New music name:", "new" + MusicIO.EXTENSION, this, name -> {
            if (!name.endsWith(MusicIO.EXTENSION)) name += MusicIO.EXTENSION;
            Path target = currentPath.resolve(name);
            if (Files.exists(target)) { MessageUtils.errorToChat("\"" + name + "\" already exists."); return; }
            try { MusicIO.save(target, MusicProject.fresh(MusicIO.stripExtension(name))); }
            catch (IOException e) { MessageUtils.errorToChat("Create failed: " + e.getMessage()); }
        }));
    }

    private void newFolder() {
        open(new InputDialogScreen("New folder name:", "folder", this, name -> {
            Path target = currentPath.resolve(name);
            if (Files.exists(target)) { MessageUtils.errorToChat("\"" + name + "\" already exists."); return; }
            try { Files.createDirectories(target); }
            catch (IOException e) { MessageUtils.errorToChat("Create failed: " + e.getMessage()); }
        }));
    }

    private int maxVisibleRows() {
        return Math.max(1, (height - HEADER_H - FOOTER_H - 2) / ROW_H);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
