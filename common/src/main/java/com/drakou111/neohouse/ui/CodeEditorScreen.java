package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;

import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.NeoHouseScreens;
import com.drakou111.neohouse.compiler.CompilerSession;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.compiler.ImportGraph;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.utils.MessageUtils;
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
    private static final int BACK_X = 6;
    private static final int BACK_W = 46;
    private static final int LOGO_SIZE = 14;
    private static final int HEADER_GAP = 4;
    // Save, Save & Run and Close, with their gaps.
    private static final int RIGHT_BUTTONS_W = 186;
    private static final String DIRTY_MARK = "* ";

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
                .bounds(rx - RIGHT_BUTTONS_W, by, 46, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Run"), btn -> saveAndRun())
                .bounds(rx - 136, by, 76, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(rx - 56, by, 56, bh).build());
        addRenderableWidget(Button.builder(Component.literal("< Back"), btn -> goBack())
                .bounds(BACK_X, by, BACK_W, bh).build());

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
        // Defer the first error-check to the debounce tick so a large file's editor appears
        // immediately instead of blocking the open on a full compile.
        scheduleCheck();
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
                CHECKER.compile(src, filePath);
                break;
            } catch (CompilingException e) {
                collected.add(e);
                // Blank out the offending token and recompile to find further errors, but only for
                // tokens in this file: an error inside an imported file can't be blanked out here.
                if (e.errorToken == null || !CHECKER.getLastImportGraph().isRoot(e.errorToken.file)) break;
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
                messages.add(editorMessage(e.errorToken, e.detail));
                spans.add(spanInThisFile(e.errorToken));
            }
            editor.setErrors(spans, messages, messages.getFirst());
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
            messages.add(editorMessage(w.token(), w.message()));
            spans.add(spanInThisFile(w.token()));
        }
        editor.setWarnings(spans, messages, messages.getFirst());
    }

    /**
     * The text shown in the error bar. The editor already underlines the token, so only diagnostics
     * from imported files (whose code isn't on screen) include the offending source line.
     */
    private static String editorMessage(Token token, String detail) {
        if (token == null) return detail;
        String message = CompilingException.location(token) + ": " + detail;
        if (CHECKER.getLastImportGraph().isRoot(token.file) || token.lineContent == null || token.lineContent.isBlank())
            return message;
        return message + "\n" + token.lineContent.replace("\r", "").strip();
    }

    /**
     * The {line, column, length} span (0-based) to underline for a diagnostic. A diagnostic inside an
     * imported file underlines the import statement in this file that pulled that file in.
     */
    private static int[] spanInThisFile(Token token) {
        if (token == null) return new int[]{-1, -1, 0};
        ImportGraph imports = CHECKER.getLastImportGraph();
        Token anchor = imports.isRoot(token.file) ? token : imports.getRootImportToken(token.file).orElse(null);
        if (anchor == null) return new int[]{-1, -1, 0};
        return new int[]{anchor.line - 1, Math.max(0, anchor.column - 1), anchor.text.length()};
    }

    private static int tokenOffset(String source, Token t) {
        String[] lines = source.split("\n", -1);
        int lineIdx = t.line - 1;
        if (lineIdx < 0 || lineIdx >= lines.length) return -1;
        int offset = 0;
        for (int i = 0; i < lineIdx; i++) offset += lines[i].length() + 1;
        offset += Math.max(0, t.column - 1);
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
            MessageUtils.errorToChat(e);
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
                        // Reopening NeoHouse shows the file browser, not this editor with the discarded changes.
                        pendingContent = null;
                        NeoHouseScreens.remember(parent);
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

        // Header, left to right: Back button, logo, file name; the name stops before the Save buttons.
        int logoX = BACK_X + BACK_W + HEADER_GAP;
        RenderHelper.renderLogo(g, logoX, (HEADER_H - LOGO_SIZE) / 2, LOGO_SIZE, t.accent());

        boolean dirty = isDirty();
        int textX = logoX + LOGO_SIZE + HEADER_GAP;
        int textY = (HEADER_H - 8) / 2;
        if (dirty) {
            g.drawString(this.font, DIRTY_MARK, textX, textY, 0xFFFF9944, false);
            textX += font.width(DIRTY_MARK);
        }
        int maxNameWidth = Math.max(0, this.width - 6 - RIGHT_BUTTONS_W - HEADER_GAP - textX);
        String name = filePath.getFileName().toString();
        if (font.width(name) > maxNameWidth)
            name = font.plainSubstrByWidth(name, Math.max(0, maxNameWidth - font.width("..."))) + "...";
        g.drawString(this.font, name, textX, textY, dirty ? 0xFFDDCCBB : 0xFFCCCCCC, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
