package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.CommandActions;
import com.drakou111.neohouse.compat.PoseHelper;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.constants.Sound;
import com.drakou111.neohouse.music.MusicFunctionExporter;
import com.drakou111.neohouse.music.MusicIO;
import com.drakou111.neohouse.music.MusicNote;
import com.drakou111.neohouse.music.MusicPlayer;
import com.drakou111.neohouse.music.MusicProject;
import com.drakou111.neohouse.music.MusicTrack;
import com.drakou111.neohouse.music.SoundEventResolver;
import com.drakou111.neohouse.music.SoundIcon;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Piano-roll editor for a {@code .nhm} music project.
 *
 * <p>Melodic (note-block) tracks are edited on a 25-row semitone grid; other
 * tracks are edited as one-shot lanes with free volume/pitch. Left-click places
 * or selects a note, drag moves it, right-click deletes it. Space toggles
 * playback; Ctrl+C/Ctrl+V copy/paste the selected note at the playhead; Delete
 * removes it; Ctrl+S saves. Each track shows its sound's item icon plus compact
 * mute/sound/rename/delete buttons in the left panel.</p>
 */
public class MusicEditorScreen extends ScreenBase {

    private static final int PAD = 6;
    private static final int HEADER_H = 40;
    private static final int LEFT_W = 122;
    private static final int FOOTER_H = 24;
    private static final int HSB_H = 9;          // horizontal scrollbar strip height
    private static final int MIN_TICK_W = 3;
    private static final int MAX_TICK_W = 28;
    private static final int SEMITONES = 25;
    private static final int TRACK_ROW_H = 30;

    private final Path file;
    private final Screen parent;
    private final MusicProject project;
    private final MusicPlayer player = new MusicPlayer();

    private int activeTrack = 0;
    private int tickOffset = 0;
    private int tickW = 14;                        // horizontal zoom (pixels per tick)

    private boolean hsbDragging = false;
    private int hsbDragStartX, hsbDragStartOffset;

    private double currentVolume = 0.7; // default volume for newly-placed notes (0.7 = playSound default, skipped on export)

    private MusicNote selected;
    private boolean dragging = false;
    private boolean dirty = false;
    private boolean wasPlaying = false;

    private MusicNote clipboard;

    public MusicEditorScreen(Path file, Screen parent) {
        super(Component.literal("Music Editor"));
        this.file = file;
        this.parent = parent;
        MusicProject loaded;
        try {
            loaded = MusicIO.load(file);
        } catch (IOException e) {
            MessageUtils.errorToChat("Could not read music file: " + e.getMessage());
            loaded = MusicProject.fresh(MusicIO.stripExtension(file.getFileName().toString()));
        }
        if (loaded.tracks.isEmpty()) loaded.tracks.add(new MusicTrack("Harp", Sound.BLOCK_NOTE_BLOCK_HARP));
        this.project = loaded;
        this.player.setProject(project);
    }

    private MusicTrack active() {
        activeTrack = Math.max(0, Math.min(project.tracks.size() - 1, activeTrack));
        return project.tracks.get(activeTrack);
    }

    private static Sound soundOf(MusicTrack tr) {
        try { return Sound.valueOf(tr.sound); }
        catch (IllegalArgumentException e) { return Sound.BLOCK_NOTE_BLOCK_HARP; }
    }

    // ---- layout helpers ----
    private int gridX0() { return LEFT_W; }
    private int gridY0() { return HEADER_H; }
    private int gridW()  { return width - LEFT_W - PAD; }
    private int gridH()  { return height - HEADER_H - FOOTER_H - HSB_H; }
    private int rowH()   { return Math.max(6, gridH() / SEMITONES); }
    private int visibleTicks() { return Math.max(1, gridW() / tickW); }

    private int cellX(int tick) { return gridX0() + (tick - tickOffset) * tickW; }
    private int tickAt(double mx) { return tickOffset + (int) Math.floor((mx - gridX0()) / tickW); }
    private int rowTop(int semitone) { return gridY0() + (24 - MusicNote.clampSemitone(semitone)) * rowH(); }
    private int semitoneAt(double my) { return MusicNote.clampSemitone(24 - (int) Math.floor((my - gridY0()) / rowH())); }
    private int trackRowY(int i) { return HEADER_H + 4 + i * TRACK_ROW_H; }

    @Override
    protected void init() {
        rebuildAll();
    }

    private void rebuildAll() {
        clearWidgets();

        // top-right close
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> onClose())
                .bounds(width - PAD - 16, 4, 16, 16).build());

        // top toolbar
        int x = PAD;
        x = addTop("< Back", 42, x, b -> onClose());
        x = addTop("Save", 40, x, b -> save());
        x = addTop("Export", 48, x, b -> exportFunction());
        x = addTop(player.isPlaying() ? "■ Stop" : "▶ Play", 48, x, b -> { player.toggle(0); rebuildAll(); });
        x = addTop(player.isLooping() ? "Loop On" : "Loop Off", 54, x, b -> { player.setLoop(!player.isLooping()); rebuildAll(); });
        x = addTop("+ Track", 48, x, b -> addTrack());
        x = addTop("Z -", 24, x, b -> zoom(-2));
        x = addTop("Z +", 24, x, b -> zoom(2));

        // per-track buttons in the left panel
        for (int i = 0; i < project.tracks.size(); i++) {
            MusicTrack tr = project.tracks.get(i);
            int by = trackRowY(i) + 17;
            if (by + 11 > height - FOOTER_H) break; // panel full — remaining tracks not shown
            addPanelBtn(tr.muted ? "Unm" : "Mut", 4,  by, b -> { activeTrack(tr); tr.muted = !tr.muted; markDirty(); rebuildAll(); });
            addPanelBtn("Snd", 32, by, b -> { activeTrack(tr); pickSoundFor(tr); });
            addPanelBtn("Ren", 60, by, b -> { activeTrack(tr); renameTrack(tr); });
            addPanelBtn("Del", 88, by, b -> { activeTrack(tr); deleteTrack(tr); });
        }
    }

    private int addTop(String label, int w, int x, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(Component.literal(label), onPress).bounds(x, 4, w, 16).build());
        return x + w + 4;
    }
    private void addPanelBtn(String label, int x, int y, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(Component.literal(label), onPress).bounds(x, y, 26, 11).build());
    }

    private void activeTrack(MusicTrack tr) {
        int idx = project.tracks.indexOf(tr);
        if (idx >= 0) { activeTrack = idx; selected = null; }
    }

    // ---- rendering ----
    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        player.update();
        if (player.isPlaying() != wasPlaying) { wasPlaying = player.isPlaying(); rebuildAll(); }

        UITheme t = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));

        renderGrid(g, t);
        renderHScroll(g, t);
        renderTrackPanel(g, t);

        // header + footer bars
        g.fill(0, 0, width, HEADER_H, t.panel(o.panelAlpha));
        g.fill(0, HEADER_H - 1, width, HEADER_H, t.accent());
        g.fill(0, height - FOOTER_H, width, height, t.panel(o.panelAlpha));
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, t.accent());

        String title = project.name + (dirty ? " *" : "");
        g.drawString(font, title, width - PAD - 20 - font.width(title), 26, 0xFFFFFFFF, false);

        // compact footer: selected-note readout (left) + controls hint (right)
        int fY = height - FOOTER_H + 8;
        if (selected != null) {
            MusicTrack tr = active();
            String sel = String.format("Vol %.1f   ·   Pitch %.2f", tr.volumeFor(selected), tr.pitchFor(selected));
            g.drawString(font, sel, PAD, fY, 0xFFCCCCCC, false);
        }
        String hint = String.format("Len %.1fs   ·   LMB add   ·   RMB delete   ·   scroll note = volume",
                project.lengthTicks() / (double) MusicProject.TICKS_PER_SECOND);
        g.drawString(font, hint, width - PAD - font.width(hint), fY, t.muted(), false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphics g, UITheme t) {
        int gx0 = gridX0(), gy0 = gridY0(), gw = gridW(), gh = gridH();
        g.fill(gx0, gy0, gx0 + gw, gy0 + gh, 0x66000000);

        MusicTrack tr = active();
        int rh = rowH();

        // pitch rows (row 12 = pitch 1.0) — drawn for every track since Y = pitch
        for (int s = 0; s < SEMITONES; s++) {
            int y = gy0 + (24 - s) * rh;
            boolean base = (s == 12);
            int shade = base ? t.accentTint(0x22) : (s % 2 == 0 ? 0x0CFFFFFF : 0x06FFFFFF);
            g.fill(gx0, y, gx0 + gw, y + rh, shade);
        }

        // when zoomed out the grid gets crowded — drop the per-tick minor lines,
        // and drop beat lines too once they'd be closer than a few pixels
        int beatEvery = Math.max(1, project.gridDivision);
        boolean drawMinor = tickW >= 9;
        boolean drawBeat = beatEvery * tickW >= 4;
        int vt = visibleTicks();
        for (int i = 0; i <= vt; i++) {
            int tick = tickOffset + i;
            int xx = cellX(tick);
            if (xx < gx0 || xx > gx0 + gw) continue;
            if (tick % beatEvery == 0) {
                if (drawBeat) g.fill(xx, gy0, xx + 1, gy0 + gh, t.accentTint(0x33));
            } else if (drawMinor) {
                g.fill(xx, gy0, xx + 1, gy0 + gh, 0x14FFFFFF);
            }
        }

        for (int ti = 0; ti < project.tracks.size(); ti++) {
            if (ti == activeTrack) continue;
            drawTrackNotes(g, project.tracks.get(ti), gx0, gy0, gw, gh, rh, true);
        }
        drawTrackNotes(g, tr, gx0, gy0, gw, gh, rh, false);

        double head = player.currentTick();
        int hx = gx0 + (int) Math.round((head - tickOffset) * tickW);
        if (hx >= gx0 && hx <= gx0 + gw) g.fill(hx, gy0, hx + 1, gy0 + gh, 0xFFFF5555);
    }

    private void drawTrackNotes(GuiGraphics g, MusicTrack tr, int gx0, int gy0, int gw, int gh, int rh, boolean dim) {
        ItemStack icon = SoundIcon.iconFor(soundOf(tr));
        int rgb = trackColor(tr) & 0xFFFFFF;
        for (MusicNote n : tr.notes) {
            int nx = cellX(n.tick);
            if (nx + tickW < gx0 || nx > gx0 + gw) continue;
            int ny = rowTop(n.semitone);
            int nh = rh;
            int x2 = Math.min(gx0 + gw, nx + tickW - 1);

            // volume (0..2) → background opacity: louder notes are brighter
            int alpha = 0x30 + (int) Math.round(Math.min(1.0, tr.volumeFor(n) / 2.0) * 0xC0);
            if (dim) alpha /= 2;
            g.fill(nx + 1, ny + 1, x2, ny + nh - 1, (alpha << 24) | rgb);

            // icon always shows (every track, selected or not) when the cell is big enough
            int size = Math.min(tickW - 2, nh - 2);
            if (size >= 6) {
                renderIcon(g, icon, nx + (tickW - size) / 2, ny + (nh - size) / 2, size);
            }
            if (n == selected) {
                g.fill(nx, ny, x2, ny + 1, 0xFFFFFFFF);
                g.fill(nx, ny + nh - 1, x2, ny + nh, 0xFFFFFFFF);
                g.fill(nx, ny, nx + 1, ny + nh, 0xFFFFFFFF);
                g.fill(x2, ny, x2 + 1, ny + nh, 0xFFFFFFFF);
            }
        }
    }

    private void renderIcon(GuiGraphics g, ItemStack stack, int x, int y, int size) {
        float s = size / 16f;
        PoseHelper.push(g);
        PoseHelper.translate(g, x, y);
        PoseHelper.scale(g, s, s);
        g.renderItem(stack, 0, 0);
        PoseHelper.pop(g);
    }

    private int trackColor(MusicTrack tr) {
        int idx = project.tracks.indexOf(tr);
        int[] palette = { 0xFF5AA9E6, 0xFF7FC97F, 0xFFF28E2B, 0xFFE15759, 0xFFB07AA1, 0xFF76B7B2, 0xFFEDC948 };
        int base = palette[Math.floorMod(idx, palette.length)];
        return tr.muted ? (base & 0x55FFFFFF) : base;
    }

    private void renderTrackPanel(GuiGraphics g, UITheme t) {
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fill(0, HEADER_H, LEFT_W, height - FOOTER_H, t.panel(o.panelAlpha));
        g.fill(LEFT_W - 1, HEADER_H, LEFT_W, height - FOOTER_H, t.accent());
        for (int i = 0; i < project.tracks.size(); i++) {
            int rowY = trackRowY(i);
            if (rowY + TRACK_ROW_H - 4 > height - FOOTER_H) break;
            MusicTrack tr = project.tracks.get(i);
            boolean act = i == activeTrack;
            if (act) g.fill(0, rowY - 2, LEFT_W - 1, rowY + TRACK_ROW_H - 4, t.accentTint(0x33));
            g.fill(3, rowY - 2, 4, rowY + TRACK_ROW_H - 4, trackColor(tr));

            renderIcon(g, SoundIcon.iconFor(soundOf(tr)), 5, rowY, 12);

            int col = tr.muted ? t.muted() : (act ? 0xFFFFFFFF : 0xFFCCCCCC);
            g.drawString(font, trimTo(tr.name, 92), 21, rowY + 1, col, false);
            g.drawString(font, tr.notes.size() + (tr.muted ? " · muted" : " notes"), 21, rowY + 10, t.muted(), false);
        }
    }

    private String trimTo(String s, int px) {
        if (font.width(s) <= px) return s;
        while (s.length() > 1 && font.width(s + "…") > px) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private int hsbTotal() { return Math.max(project.lengthTicks() + 8, visibleTicks()); }
    private static int clampOff(int v, int max) { return Math.max(0, Math.min(max, v)); }
    private boolean inHScroll(double mx, double my) {
        return my >= height - FOOTER_H - HSB_H && my < height - FOOTER_H
                && mx >= gridX0() && mx <= gridX0() + gridW();
    }

    private void renderHScroll(GuiGraphics g, UITheme t) {
        int y0 = height - FOOTER_H - HSB_H, y1 = height - FOOTER_H;
        int gx0 = gridX0(), gw = gridW();
        g.fill(gx0, y0, gx0 + gw, y1, 0x22FFFFFF);
        g.fill(gx0, y0, gx0 + gw, y0 + 1, t.accentTint(0x22));
        int total = hsbTotal(), vis = visibleTicks();
        int thumbW = Math.min(gw, Math.max(14, (int) ((long) gw * vis / total)));
        int maxOff = Math.max(0, total - vis);
        int thumbX = gx0 + (maxOff > 0 ? (gw - thumbW) * tickOffset / maxOff : 0);
        g.fill(thumbX, y0 + 1, thumbX + thumbW, y1 - 1, hsbDragging ? 0xCCDDDDDD : (0x88000000 | (t.muted() & 0xFFFFFF)));
    }

    private void zoom(int d) {
        tickW = Math.max(MIN_TICK_W, Math.min(MAX_TICK_W, tickW + d));
        tickOffset = clampOff(tickOffset, Math.max(0, hsbTotal() - visibleTicks()));
    }

    /** Zoom while keeping the tick under the cursor roughly in place. */
    private void zoomAt(int d, double mouseX) {
        boolean inX = mouseX >= gridX0() && mouseX <= gridX0() + gridW();
        int pivotTick = inX ? tickAt(mouseX) : tickOffset + visibleTicks() / 2;
        int px = inX ? (int) (mouseX - gridX0()) : gridW() / 2;
        tickW = Math.max(MIN_TICK_W, Math.min(MAX_TICK_W, tickW + d));
        tickOffset = clampOff(pivotTick - px / tickW, Math.max(0, hsbTotal() - visibleTicks()));
    }

    // ---- input ----
    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        // let all widgets (toolbar, per-track buttons, footer) fire first
        if (superMouseClicked(mx, my, button)) return true;

        // horizontal scrollbar
        if (button == 0 && inHScroll(mx, my)) {
            int total = hsbTotal(), vis = visibleTicks(), gw = gridW(), gx0 = gridX0();
            int thumbW = Math.min(gw, Math.max(14, (int) ((long) gw * vis / total)));
            int maxOff = Math.max(0, total - vis);
            int thumbX = gx0 + (maxOff > 0 ? (gw - thumbW) * tickOffset / maxOff : 0);
            if (mx >= thumbX && mx < thumbX + thumbW) {
                hsbDragging = true;
                hsbDragStartX = (int) mx;
                hsbDragStartOffset = tickOffset;
            } else {
                int slide = gw - thumbW;
                if (slide > 0) tickOffset = clampOff((int) Math.round((mx - gx0 - thumbW / 2.0) / slide * maxOff), maxOff);
            }
            return true;
        }

        // track panel row selection (icon/name area)
        if (mx < LEFT_W && my >= HEADER_H && my < height - FOOTER_H) {
            int idx = (int) ((my - (HEADER_H + 4)) / TRACK_ROW_H);
            if (idx >= 0 && idx < project.tracks.size()) {
                activeTrack = idx;
                selected = null;
                rebuildAll();
            }
            return true;
        }

        if (inGrid(mx, my)) {
            int tick = tickAt(mx);
            if (tick < 0) return true;
            MusicTrack tr = active();
            MusicNote hit = noteAt(tr, tick, my);
            if (button == 1) {
                if (hit != null) { tr.notes.remove(hit); if (hit == selected) selected = null; markDirty(); }
                return true;
            }
            if (hit != null) {
                selected = hit;
                dragging = true;
                currentVolume = hit.volume;
            } else {
                MusicNote n = new MusicNote(tick);
                n.volume = currentVolume;
                n.semitone = semitoneAt(my);
                tr.notes.add(n);
                selected = n;
                dragging = true;
                markDirty();
                MusicPlayer.playNote(tr.soundFor(n), tr.volumeFor(n), (float) tr.pitchFor(n));
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (hsbDragging && button == 0) {
            int total = hsbTotal(), vis = visibleTicks(), gw = gridW();
            int thumbW = Math.min(gw, Math.max(14, (int) ((long) gw * vis / total)));
            int maxOff = Math.max(0, total - vis);
            int slide = gw - thumbW;
            if (slide > 0) tickOffset = clampOff(hsbDragStartOffset + (int) Math.round((mx - hsbDragStartX) * (double) maxOff / slide), maxOff);
            return true;
        }
        if (dragging && selected != null && button == 0) {
            selected.tick = Math.max(0, tickAt(mx));
            selected.semitone = semitoneAt(my);
            markDirty();
            return true;
        }
        return superMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int button) {
        if (hsbDragging && button == 0) { hsbDragging = false; return true; }
        if (dragging && button == 0) { dragging = false; return true; }
        return superMouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // ctrl+scroll = zoom in/out (centred on the cursor)
        if (RenderHelper.hasControlDown()) {
            zoomAt(scrollY > 0 ? 2 : -2, mouseX);
            return true;
        }
        // scrolling over a note adjusts that note's volume; elsewhere scrolls the timeline
        if (inGrid(mouseX, mouseY)) {
            MusicTrack tr = active();
            MusicNote n = noteAt(tr, tickAt(mouseX), mouseY);
            if (n != null) {
                n.volume = clamp(n.volume + (scrollY > 0 ? 0.1 : -0.1), 0.0, 2.0);
                currentVolume = n.volume;
                selected = n;
                markDirty();
                MusicPlayer.playNote(tr.soundFor(n), tr.volumeFor(n), (float) tr.pitchFor(n));
                return true;
            }
        }
        int step = RenderHelper.hasShiftDown() ? 4 : 1;
        int maxOff = Math.max(0, hsbTotal() - visibleTicks());
        if (scrollY > 0) tickOffset = Math.max(0, tickOffset - step);
        else if (scrollY < 0) tickOffset = Math.min(maxOff, tickOffset + step);
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 0x2) != 0; // GLFW_MOD_CONTROL
        if (key == 32) { player.toggle(0); rebuildAll(); return true; }
        if (key == 261 && selected != null) {
            active().notes.remove(selected); selected = null; markDirty(); return true;
        }
        if (ctrl && key == 67 && selected != null) { clipboard = selected.copy(); return true; }
        if (ctrl && key == 86 && clipboard != null) {
            MusicNote n = clipboard.copy();
            n.tick = (int) Math.floor(player.currentTick());
            active().notes.add(n);
            selected = n;
            markDirty();
            return true;
        }
        if (ctrl && key == 83) { save(); return true; }
        return superKeyPressed(key, scan, mods);
    }

    private boolean inGrid(double mx, double my) {
        return mx >= gridX0() && mx <= gridX0() + gridW() && my >= gridY0() && my <= gridY0() + gridH();
    }

    private MusicNote noteAt(MusicTrack tr, int tick, double my) {
        int sem = semitoneAt(my);
        for (MusicNote n : tr.notes) {
            if (n.tick == tick && n.semitone == sem) return n;
        }
        return null;
    }

    // ---- actions ----
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private void addTrack() {
        MusicTrack tr = new MusicTrack("Track " + (project.tracks.size() + 1), Sound.BLOCK_NOTE_BLOCK_BASS);
        project.tracks.add(tr);
        activeTrack = project.tracks.size() - 1;
        markDirty();
        rebuildAll();
    }

    private void deleteTrack(MusicTrack tr) {
        if (project.tracks.size() <= 1) { MessageUtils.errorToChat("A project needs at least one track."); return; }
        project.tracks.remove(tr);
        activeTrack = Math.max(0, activeTrack - 1);
        selected = null;
        markDirty();
        rebuildAll();
    }

    private void renameTrack(MusicTrack tr) {
        minecraft.setScreen(new InputDialogScreen("Rename track:", tr.name, this, name -> {
            if (!name.isBlank()) { tr.name = name; markDirty(); }
        }));
    }

    private void pickSoundFor(MusicTrack tr) {
        minecraft.setScreen(new SoundPickerScreen(this, false, soundOf(tr), s -> {
            tr.sound = s.name();
            tr.melodic = SoundEventResolver.isMelodic(s);
            markDirty();
        }));
    }

    private void save() {
        try {
            MusicIO.save(file, project);
            dirty = false;
            MessageUtils.debugToChat("Saved " + file.getFileName());
        } catch (IOException e) {
            MessageUtils.errorToChat("Save failed: " + e.getMessage());
        }
    }

    private void exportFunction() {
        String suggested = MusicFunctionExporter.sanitizeFunctionName(project.name);
        minecraft.setScreen(new InputDialogScreen(
                "Export as function — file name:", suggested, this,
                raw -> {
                    String fnName = MusicFunctionExporter.sanitizeFunctionName(raw);
                    Path target = CommandActions.getScriptsDir().resolve(fnName + ".nh");
                    if (Files.exists(target)) {
                        minecraft.setScreen(new InputDialogScreen(
                                "\"" + fnName + ".nh\" already exists. Overwrite?", this,
                                () -> writeExport(fnName, target)));
                    } else {
                        writeExport(fnName, target);
                    }
                }));
    }

    private void writeExport(String fnName, Path target) {
        try {
            Files.writeString(target, MusicFunctionExporter.toScript(project, fnName));
            MessageUtils.debugToChat("Exported to neohouse-scripts/" + fnName + ".nh (" + project.totalNotes() + " notes).");
        } catch (IOException e) {
            MessageUtils.errorToChat("Export failed: " + e.getMessage());
        }
    }

    private void markDirty() { dirty = true; }

    @Override
    public void onClose() {
        player.stop();
        if (dirty) {
            minecraft.setScreen(new InputDialogScreen(
                    "Unsaved changes. Discard?", this,
                    this::gotoParent));
        } else {
            gotoParent();
        }
    }

    private void gotoParent() {
        if (parent != null) minecraft.setScreen(parent);
        else minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
