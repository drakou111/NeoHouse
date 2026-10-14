package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class InputDialogScreen extends ScreenBase {
    private static final int LINE_H   = 10;
    private static final int PAD      = 8;
    private static final int BTN_H    = 20;
    private static final int INPUT_H  = 18;
    private static final int MIN_W    = 200;
    private static final int TITLE_H  = 18;

    private final String   prompt;
    private final String   defaultValue;
    private final Screen   parent;
    private final Consumer<String> onConfirm;
    private final boolean  confirmOnly;
    private EditBox inputBox;

    private int dialogW;
    private int dialogH;

    public InputDialogScreen(String prompt, String defaultValue, Screen parent, Consumer<String> onConfirm) {
        super(Component.literal("Input"));
        this.prompt       = prompt;
        this.defaultValue = defaultValue;
        this.parent       = parent;
        this.onConfirm    = onConfirm;
        this.confirmOnly  = false;
    }

    public InputDialogScreen(String prompt, Screen parent, Runnable onConfirm) {
        super(Component.literal("Confirm"));
        this.prompt       = prompt;
        this.defaultValue = "";
        this.parent       = parent;
        this.onConfirm    = ignored -> onConfirm.run();
        this.confirmOnly  = true;
    }

    private String[] promptLines() { return prompt.split("\n", -1); }

    @Override
    protected void init() {
        String[] lines  = promptLines();
        int promptH     = lines.length * LINE_H;

        dialogW = MIN_W;
        for (String l : lines) dialogW = Math.max(dialogW, this.font.width(l) + 2 * PAD + 4);
        dialogW = Math.min(dialogW, this.width - 40);

        if (confirmOnly) {
            dialogH = TITLE_H + PAD + promptH + PAD + BTN_H + PAD;
        } else {
            dialogH = TITLE_H + PAD + promptH + PAD + INPUT_H + PAD + BTN_H + PAD;
        }

        int x = (this.width  - dialogW) / 2;
        int y = (this.height - dialogH) / 2;

        int afterPrompt = y + TITLE_H + PAD + promptH;

        if (!confirmOnly) {
            inputBox = new EditBox(this.font, x + PAD, afterPrompt + PAD, dialogW - 2 * PAD, INPUT_H, Component.empty());
            inputBox.setMaxLength(256);
            inputBox.setValue(defaultValue);
            addRenderableWidget(inputBox);
            setInitialFocus(inputBox);
        }

        int btnY   = confirmOnly ? afterPrompt + PAD : afterPrompt + PAD + INPUT_H + PAD;
        int btnW   = (dialogW - 3 * PAD) / 2;
        addRenderableWidget(Button.builder(
                Component.literal("✓ OK").withStyle(ChatFormatting.GREEN), btn -> confirm())
                .bounds(x + PAD, btnY, btnW, BTN_H).build());
        addRenderableWidget(Button.builder(
                Component.literal("✗ Cancel").withStyle(ChatFormatting.RED), btn -> cancel())
                .bounds(x + PAD * 2 + btnW, btnY, btnW, BTN_H).build());
    }

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        int x  = (this.width  - dialogW) / 2;
        int y  = (this.height - dialogH) / 2;
        int cx = x + dialogW / 2;

        if (parent != null) g.render(parent, -1, -1, partialTick);
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fill(0, 0, this.width, this.height, o.dimAlpha << 24);

        g.fill(x, y, x + dialogW, y + dialogH, t.dialog(o.panelAlpha));
        RenderHelper.renderOutline(g, x, y, dialogW, dialogH, t.accent());

        String titleLabel = confirmOnly ? "Confirm" : "Input";
        g.fill(x, y, x + dialogW, y + TITLE_H, t.panel(0xFF));
        g.fill(x, y + TITLE_H - 1, x + dialogW, y + TITLE_H, t.accent());
        g.drawCenteredString(this.font, titleLabel, cx, y + (TITLE_H - 8) / 2, t.muted());

        String[] lines = promptLines();
        for (int i = 0; i < lines.length; i++) {
            g.drawCenteredString(this.font, lines[i], cx, y + TITLE_H + PAD + i * LINE_H, 0xFFCCCCCC);
        }

        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == Keys.ENTER || keyCode == Keys.KP_ENTER) { confirm(); return true; }
        if (keyCode == Keys.ESCAPE) { cancel(); return true; }
        return superKeyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        Screen before = ClientUi.screen(Minecraft.getInstance());
        onConfirm.accept(inputBox != null ? inputBox.getValue().trim() : "");
        if (ClientUi.screen(Minecraft.getInstance()) == before) {
            ClientUi.setScreen(Minecraft.getInstance(), parent);
        }
    }

    private void cancel() {
        ClientUi.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
