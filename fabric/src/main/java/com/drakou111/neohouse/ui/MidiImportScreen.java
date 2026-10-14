package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.music.MidiImporter;
import com.drakou111.neohouse.music.MusicIO;
import com.drakou111.neohouse.music.MusicProject;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dialog for importing a known {@code .mid} file into a new {@code .nhm} project.
 * The MIDI path is already known (the file was selected in the browser), so this
 * only asks what to name the resulting {@code .nhm} and handles overwrite.
 */
public class MidiImportScreen extends ScreenBase {

    private static final int PAD = 10, W = 300, TITLE_H = 18, LINE_H = 10, INPUT_H = 18, BTN_H = 20;

    private final Screen parent;
    private final Path midiFile;
    private final Path targetDir;

    private EditBox nameBox;
    private int dialogX, dialogY, dialogH;

    public MidiImportScreen(Screen parent, Path midiFile, Path targetDir) {
        super(Component.literal("Import MIDI"));
        this.parent = parent;
        this.midiFile = midiFile;
        this.targetDir = targetDir;
    }

    @Override
    protected void init() {
        dialogH = TITLE_H + PAD + 2 * LINE_H + PAD + INPUT_H + PAD + BTN_H + PAD;
        dialogX = (width - W) / 2;
        dialogY = (height - dialogH) / 2;

        int y = dialogY + TITLE_H + PAD + 2 * LINE_H + PAD;
        nameBox = new EditBox(font, dialogX + PAD, y, W - 2 * PAD, INPUT_H, Component.literal("Name"));
        nameBox.setMaxLength(128);
        nameBox.setValue(MusicIO.stripExtension(midiFile.getFileName().toString()));
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        int btnY = y + INPUT_H + PAD;
        int btnW = (W - 3 * PAD) / 2;
        addRenderableWidget(Button.builder(Component.literal("✓ Import").withStyle(ChatFormatting.GREEN), b -> doImport())
                .bounds(dialogX + PAD, btnY, btnW, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("✗ Cancel").withStyle(ChatFormatting.RED), b -> cancel())
                .bounds(dialogX + 2 * PAD + btnW, btnY, btnW, BTN_H).build());
    }

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) g.render(parent, -1, -1, partialTick);
        UITheme t = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fill(0, 0, width, height, o.dimAlpha << 24);

        g.fill(dialogX, dialogY, dialogX + W, dialogY + dialogH, t.dialog(o.panelAlpha));
        RenderHelper.renderOutline(g, dialogX, dialogY, W, dialogH, t.accent());
        g.fill(dialogX, dialogY, dialogX + W, dialogY + TITLE_H, t.panel(0xFF));
        g.fill(dialogX, dialogY + TITLE_H - 1, dialogX + W, dialogY + TITLE_H, t.accent());
        g.drawCenteredString(font, "Import MIDI -> Music", dialogX + W / 2, dialogY + (TITLE_H - 8) / 2, t.muted());

        int ty = dialogY + TITLE_H + PAD;
        g.drawString(font, "Importing: " + midiFile.getFileName(), dialogX + PAD, ty, t.muted(), false);
        g.drawString(font, "Save as (.nhm):", dialogX + PAD, ty + LINE_H, 0xFFCCCCCC, false);

        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        if (key == 256) { cancel(); return true; }
        if (key == 257 || key == 335) { doImport(); return true; }
        return superKeyPressed(key, scan, mods);
    }

    private void doImport() {
        String raw = nameBox.getValue().trim();
        if (raw.isEmpty()) { MessageUtils.errorToChat("Enter a name for the music file."); return; }
        String base = MusicIO.stripExtension(raw);
        Path target = targetDir.resolve(base + MusicIO.EXTENSION);
        if (Files.exists(target)) {
            ClientUi.setScreen(Minecraft.getInstance(), new InputDialogScreen(
                    "\"" + base + MusicIO.EXTENSION + "\" already exists. Overwrite?", parent,
                    () -> write(base, target)));
        } else {
            write(base, target);
        }
    }

    private void write(String base, Path target) {
        try {
            MusicProject project = MidiImporter.importMidi(midiFile, base);
            MusicIO.save(target, project);
            MessageUtils.debugToChat("Imported " + project.totalNotes() + " notes across "
                    + project.tracks.size() + " tracks into " + target.getFileName());
            ClientUi.setScreen(Minecraft.getInstance(), new MusicEditorScreen(target, parent));
        } catch (Exception e) {
            MessageUtils.errorToChat("MIDI import failed: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    private void cancel() {
        ClientUi.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
