package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.UIOpacity;
import com.drakou111.neohouse.UITheme;
import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.drakou111.neohouse.compat.ScreenBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeEditorScreen extends ScreenBase {
    private static final NHCompiler CHECKER = new NHCompiler();

    private static final int HEADER_H = 28;

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
        int by = (HEADER_H - bh) / 2;

        saveBtn = addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(rx - 186, by, 46, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Run"), btn -> saveAndRun())
                .bounds(rx - 136, by, 76, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(rx - 56, by, 56, bh).build());
        addRenderableWidget(Button.builder(Component.literal("(i)").withStyle(ChatFormatting.AQUA), btn ->
                        Minecraft.getInstance().setScreen(new HelpScreen(this)))
                .bounds(6, by, 24, bh).build());

        editor = new SyntaxEditorWidget(4, HEADER_H + 2, this.width - 8, this.height - HEADER_H - 5);
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
            CHECKER.compile(editor.getValue(), filePath.getParent());
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
            editor.setError(-1, -1, 0, "Internal error: " + e);
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
            ScriptRunner.INSTANCE.runScript(filePath);
            MessageUtils.debugToChat("Running: " + filePath.getFileName());
        } catch (Exception e) {
            MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void discard() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && editor != null && editor.hasSuggestions()) {
            editor.dismissSuggestions();
            return true;
        }
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        return superKeyPressed(key, scan, mods);
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
        UITheme t   = UITheme.byId(NeoHouseConfig.get().uiTheme);
        UIOpacity o = UIOpacity.byId(NeoHouseConfig.get().uiOpacity);
        g.fill(0, 0, this.width, this.height, t.bgTop(o.bgAlpha));
        g.fill(0, 0, this.width, HEADER_H,    t.panel(o.panelAlpha));
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, t.accent());

        boolean dirty = isDirty();
        String indicator = dirty ? "● " : "";
        int indicatorColor = dirty ? 0xFFFF9944 : t.accent();
        g.drawString(this.font, indicator, 36, (HEADER_H - 8) / 2, indicatorColor, false);
        int nameX = 36 + (dirty ? font.width("● ") : 0);
        g.drawString(this.font, filePath.getFileName().toString(), nameX, (HEADER_H - 8) / 2,
                dirty ? 0xFFDDCCBB : 0xFFCCCCCC, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
