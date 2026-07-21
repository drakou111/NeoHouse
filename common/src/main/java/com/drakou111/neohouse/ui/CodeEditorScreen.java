package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;

import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compiler.CompilerSession;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CodeEditorScreen extends ScreenBase {
    private static final CompilerSession CHECKER = new CompilerSession();

    private static final int HEADER_H = 28;

    private final Path   filePath;
    private final Screen parent;

    private String  savedContent  = null;
    private String  pendingContent = null;

    private long    lastChangeMs   = 0;
    private boolean checkPending   = false;
    private static final long CHECK_DELAY_MS = 200;

    private SyntaxEditorWidget editor;
    private Button             saveBtn;

    public CodeEditorScreen(Path filePath, Screen parent) {
        super(Component.literal("Edit: " + filePath.getFileName()));
        this.filePath = filePath;
        this.parent   = parent;
    }

    @Override
    protected void init() {
        if (editor != null && pendingContent == null) pendingContent = editor.getValue();
        int rx = this.width - 6;
        int bh = 18;
        int by = (HEADER_H - bh) / 2;

        saveBtn = addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(rx - 186, by, 46, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Run"), btn -> saveAndRun())
                .bounds(rx - 136, by, 76, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(rx - 56, by, 56, bh).build());
        addRenderableWidget(Button.builder(Component.literal("< Back"), btn -> goBack())
                .bounds(6, by, 46, bh).build());
        addRenderableWidget(Button.builder(Component.literal("(i)").withStyle(ChatFormatting.AQUA), btn ->
                        Minecraft.getInstance().setScreen(new HelpScreen(this)))
                .bounds(56, by, 24, bh).build());

        editor = new SyntaxEditorWidget(4, HEADER_H + 2, this.width - 8, this.height - HEADER_H - 5);
        editor.setOnChange(() -> { scheduleCheck(); updateSaveBtn(); });

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
            savedContent = editor.getValue();
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

    private void scheduleCheck() {
        lastChangeMs = System.currentTimeMillis();
        checkPending = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (checkPending && System.currentTimeMillis() - lastChangeMs >= CHECK_DELAY_MS) {
            checkPending = false;
            checkErrors();
        }
    }

    private void checkErrors() {
        List<CompilingException> collected = new ArrayList<>();
        String src = editor.getValue();
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                CHECKER.compile(src, filePath.getParent());
                break;
            } catch (CompilingException e) {
                collected.add(e);
                if (e.errorToken == null) break;
                int offset = tokenOffset(src, e.errorToken);
                if (offset < 0 || offset + e.errorToken.text.length() > src.length()) break;
                src = src.substring(0, offset) + " ".repeat(e.errorToken.text.length()) + src.substring(offset + e.errorToken.text.length());
            } catch (Exception e) {
                editor.setError(-1, -1, 0, "Internal error: " + e);
                return;
            }
        }
        if (collected.isEmpty()) {
            showWarningsOrClear();
        } else {
            List<int[]> spans    = new ArrayList<>();
            List<String> messages = new ArrayList<>();
            for (CompilingException e : collected) {
                messages.add(e.getMessage());
                if (e.errorToken != null) {
                    int line0     = e.errorToken.line - 1;
                    int colStart0 = Math.max(0, e.errorToken.column - e.errorToken.text.length() - 1);
                    spans.add(new int[]{line0, colStart0, e.errorToken.text.length()});
                } else {
                    spans.add(new int[]{-1, -1, 0});
                }
            }
            editor.setErrors(spans, messages, collected.getFirst().getMessage());
        }
    }

    /** After a clean compile, show any compiler warnings (yellow) or clear the bar entirely. */
    private void showWarningsOrClear() {
        var warnings = CHECKER.getLastWarnings();
        if (warnings.isEmpty()) {
            editor.clearError();
            return;
        }
        List<int[]> spans     = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        for (var w : warnings) {
            messages.add(w.message());
            if (w.token() != null) {
                int line0     = w.token().line - 1;
                int colStart0 = Math.max(0, w.token().column - w.token().text.length() - 1);
                spans.add(new int[]{line0, colStart0, w.token().text.length()});
            } else {
                spans.add(new int[]{-1, -1, 0});
            }
        }
        editor.setWarnings(spans, messages, warnings.getFirst().message());
    }

    private static int tokenOffset(String source, Token t) {
        String[] lines = source.split("\n", -1);
        int lineIdx = t.line - 1;
        if (lineIdx < 0 || lineIdx >= lines.length) return -1;
        int offset = 0;
        for (int i = 0; i < lineIdx; i++) offset += lines[i].length() + 1;
        offset += Math.max(0, t.column - t.text.length() - 1);
        return offset;
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

    /** Return to the parent screen (e.g. the file browser), prompting if there are unsaved changes. */
    private void goBack() {
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

    /** Exit the editor straight back to the game, without popping to the parent screen. */
    private void closeToGame() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && editor != null && editor.isFindOpen()) {
            editor.closeFindBar();
            return true;
        }
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
                        closeToGame();
                    }
            ));
        } else {
            closeToGame();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fill(0, 0, this.width, this.height, t.bgTop(o.bgAlpha));
        g.fill(0, 0, this.width, HEADER_H,    t.panel(o.panelAlpha));
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, t.accent());

        RenderHelper.renderLogo(g, 4, (HEADER_H - 14) / 2, 14, t.accent());

        boolean dirty = isDirty();
        String indicator = dirty ? "● " : "";
        int indicatorColor = dirty ? 0xFFFF9944 : t.accent();
        g.drawString(this.font, indicator, 52, (HEADER_H - 8) / 2, indicatorColor, false);
        int nameX = 52 + (dirty ? font.width("● ") : 0);
        g.drawString(this.font, filePath.getFileName().toString(), nameX, (HEADER_H - 8) / 2,
                dirty ? 0xFFDDCCBB : 0xFFCCCCCC, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
