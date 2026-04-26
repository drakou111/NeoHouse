package com.drakou111.neohouse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class InputDialogScreen extends Screen {
    private static final int DIALOG_W    = 260;
    private static final int DIALOG_H    = 96;
    private static final int CONFIRM_H   = 70;

    private final String prompt;
    private final String defaultValue;
    private final Screen parent;
    private final Consumer<String> onConfirm;
    private final boolean confirmOnly;
    private EditBox inputBox;

    /** Input dialog with a text field. */
    public InputDialogScreen(String prompt, String defaultValue, Screen parent, Consumer<String> onConfirm) {
        super(Component.literal("Input"));
        this.prompt      = prompt;
        this.defaultValue = defaultValue;
        this.parent      = parent;
        this.onConfirm   = onConfirm;
        this.confirmOnly = false;
    }

    /** Confirm-only dialog (no text field). */
    public InputDialogScreen(String prompt, Screen parent, Runnable onConfirm) {
        super(Component.literal("Confirm"));
        this.prompt      = prompt;
        this.defaultValue = "";
        this.parent      = parent;
        this.onConfirm   = ignored -> onConfirm.run();
        this.confirmOnly = true;
    }

    @Override
    protected void init() {
        int h = confirmOnly ? CONFIRM_H : DIALOG_H;
        int x = (this.width  - DIALOG_W) / 2;
        int y = (this.height - h)        / 2;

        if (!confirmOnly) {
            inputBox = new EditBox(this.font, x + 6, y + 34, DIALOG_W - 12, 18, Component.empty());
            inputBox.setMaxLength(256);
            inputBox.setValue(defaultValue);
            addRenderableWidget(inputBox);
            setInitialFocus(inputBox);
        }

        int btnY = confirmOnly ? y + 36 : y + 60;
        addRenderableWidget(Button.builder(Component.literal("OK"), btn -> confirm())
                .bounds(x + 6, btnY, (DIALOG_W - 18) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> cancel())
                .bounds(x + 12 + (DIALOG_W - 18) / 2, btnY, (DIALOG_W - 18) / 2, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int h = confirmOnly ? CONFIRM_H : DIALOG_H;
        int x = (this.width  - DIALOG_W) / 2;
        int y = (this.height - h)        / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);

        g.fill(x,              y,      x + DIALOG_W, y + h,    0xDD2B2B2B);
        g.fill(x,              y,      x + DIALOG_W, y + 1,    0xFF666688);
        g.fill(x,              y + h - 1, x + DIALOG_W, y + h, 0xFF222244);
        g.fill(x,              y,      x + 1,         y + h,   0xFF666688);
        g.fill(x + DIALOG_W - 1, y,   x + DIALOG_W,  y + h,   0xFF222244);

        g.drawString(this.font, prompt, x + 6, y + 10, 0xFFDDDDDD);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { confirm(); return true; }
        if (keyCode == 256) { cancel(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        Screen before = Minecraft.getInstance().screen;
        onConfirm.accept(inputBox != null ? inputBox.getValue().trim() : "");
        // Only navigate to parent if the lambda didn't already navigate away
        if (Minecraft.getInstance().screen == before) {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private void cancel() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
