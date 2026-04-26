package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeEditorScreen extends Screen {
    private static final ScriptRunner RUNNER  = new ScriptRunner(new NHCompiler());
    private static final NHCompiler   CHECKER = new NHCompiler();

    private static final int HEADER_H = 26;

    private final Path   filePath;
    private final Screen parent;

    private String  savedContent  = null; // null = not yet loaded
    private String  pendingContent = null; // set before showing confirm dialog

    private SyntaxEditorWidget editor;
    private Button             saveBtn;

    public CodeEditorScreen(Path filePath, Screen parent) {
        super(Component.literal("Edit: " + filePath.getFileName()));
        this.filePath = filePath;
        this.parent   = parent;
    }

    @Override
    protected void init() {
        int rx = this.width - 6;
        int bh = 18;

        saveBtn = addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(rx - 186, 4, 46, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Run"), btn -> saveAndRun())
                .bounds(rx - 136, 4, 76, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Discard"), btn -> discard())
                .bounds(rx - 56, 4, 56, bh).build());
        addRenderableWidget(Button.builder(Component.literal("(i)"), btn ->
                        Minecraft.getInstance().setScreen(new HelpScreen()))
                .bounds(6, 4, 24, bh).build());

        editor = new SyntaxEditorWidget(4, HEADER_H + 2, this.width - 8, this.height - HEADER_H - 6);
        editor.setOnChange(() -> { checkErrors(); updateSaveBtn(); });

        String content;
        if (pendingContent != null) {
            content        = pendingContent;
            pendingContent = null;
        } else {
            try {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                content = "";
                MessageUtils.errorToChat("Could not load file: " + e.getMessage());
            }
        }
        editor.setValue(content);

        if (savedContent == null) {
            savedContent = editor.getValue(); // first load
        }

        addRenderableWidget(editor);
        setInitialFocus(editor);
        checkErrors();
        updateSaveBtn();
    }

    private boolean isDirty() {
        return editor != null && !editor.getValue().equals(savedContent);
    }

    private void updateSaveBtn() {
        if (saveBtn != null) saveBtn.active = isDirty();
    }

    private void checkErrors() {
        try {
            CHECKER.compile(editor.getValue());
            editor.clearError();
        } catch (CompilingException e) {
            if (e.errorToken != null) {
                int line0     = e.errorToken.line - 1;
                int colStart0 = e.errorToken.column - e.errorToken.text.length() - 1;
                editor.setError(line0, Math.max(0, colStart0), e.errorToken.text.length(), e.getMessage());
            } else {
                editor.setError(-1, -1, 0, e.getMessage());
            }
        } catch (Exception e) {
            editor.setError(-1, -1, 0, "Internal error: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.writeString(filePath, editor.getValue(), StandardCharsets.UTF_8);
            savedContent = editor.getValue();
            updateSaveBtn();
        } catch (IOException e) {
            MessageUtils.errorToChat("Save failed: " + e.getMessage());
        }
    }

    private void saveAndRun() {
        save();
        discard();
        try {
            RUNNER.runScript(filePath);
            MessageUtils.debugToChat("Running: " + filePath.getFileName());
        } catch (Exception e) {
            MessageUtils.errorToChat(e.getMessage());
        }
    }

    private void discard() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (isDirty()) {
            pendingContent = editor.getValue();
            Minecraft.getInstance().setScreen(new InputDialogScreen(
                    "Unsaved changes. Discard?",
                    this,
                    () -> {
                        pendingContent = null;
                        discard();
                    }
            ));
        } else {
            discard();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xCC141414);
        g.fill(0, 0, this.width, HEADER_H,    0xCC1E1E2A);
        g.drawString(this.font, "Editing: " + filePath.getFileName(), 36, 8, 0xFFCCCCCC);
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, 0xFF444466);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
