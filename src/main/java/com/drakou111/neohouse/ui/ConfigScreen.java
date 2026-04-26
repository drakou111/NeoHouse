package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    private static final int PAD     = 10;
    private static final int HEADER_H = 28;
    private static final int ROW_H   = 26;
    private static final int LABEL_W = 180;
    private static final int BOX_W   = 60;
    private static final int BOX_H   = 14;

    private static final String[][] ROWS = {
        { "Delay after click (ticks):", "clickDelay" },
        { "Retry amount:",              "retryAmount" },
        { "Retry delay (ticks):",       "retryDelay"  },
    };

    private final EditBox[] boxes = new EditBox[ROWS.length];

    public ConfigScreen() {
        super(Component.literal("NeoHouse Config"));
    }

    @Override
    protected void init() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        int boxX = PAD + LABEL_W;

        for (int i = 0; i < ROWS.length; i++) {
            int rowY  = HEADER_H + 10 + i * ROW_H;
            int boxY  = rowY + (ROW_H - BOX_H) / 2;

            EditBox box = new EditBox(font, boxX, boxY, BOX_W, BOX_H, Component.literal(""));
            box.setMaxLength(5);
            box.setValue(switch (ROWS[i][1]) {
                case "clickDelay"  -> String.valueOf(cfg.clickDelay);
                case "retryAmount" -> String.valueOf(cfg.retryAmount);
                case "retryDelay"  -> String.valueOf(cfg.retryDelay);
                default -> "";
            });
            boxes[i] = box;
            addRenderableWidget(box);
        }

        int btnsY = HEADER_H + 10 + ROWS.length * ROW_H + 8;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> { save(); onClose(); })
                .bounds(PAD, btnsY, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(PAD + 64, btnsY, 60, 18).build());

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());
    }

    private void save() {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        for (int i = 0; i < ROWS.length; i++) {
            String key = ROWS[i][1];
            try {
                int val = Integer.parseInt(boxes[i].getValue().trim());
                if (val < 0) val = 0;
                switch (key) {
                    case "clickDelay"  -> cfg.clickDelay  = val;
                    case "retryAmount" -> cfg.retryAmount = val;
                    case "retryDelay"  -> cfg.retryDelay  = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        cfg.save();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC141414);
        g.fill(0, 0, width, HEADER_H, 0xCC1E1E2A);
        g.drawString(font, "NeoHouse — Config", PAD, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, width, HEADER_H + 1, 0xFF444466);

        for (int i = 0; i < ROWS.length; i++) {
            int labelY = HEADER_H + 10 + i * ROW_H + (ROW_H - 9) / 2;
            g.drawString(font, ROWS[i][0], PAD, labelY, 0xFFCCCCCC);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}


    @Override
    public boolean isPauseScreen() { return false; }
}
