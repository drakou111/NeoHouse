package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.BuildConstants;
import com.drakou111.neohouse.NeoHouse;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.nio.file.Path;
import java.util.function.Supplier;

public class HomeScreen extends Screen {

    private final Supplier<Path> scriptsDir;

    public HomeScreen(Supplier<Path> scriptsDir) {
        super(Component.literal("NeoHouse"));
        this.scriptsDir = scriptsDir;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int midY    = this.height / 2;

        int btnW = 100;
        int btnH = 20;
        int gap  = 8;
        int totalW = 3 * btnW + 2 * gap;
        int startX = centerX - totalW / 2;
        int btnY   = midY + 22;

        addRenderableWidget(Button.builder(Component.literal("Files"), b -> openFiles())
                .bounds(startX, btnY, btnW, btnH).build());

        addRenderableWidget(Button.builder(Component.literal("Help"), b -> openHelp())
                .bounds(startX + btnW + gap, btnY, btnW, btnH).build());

        addRenderableWidget(Button.builder(Component.literal("Config"), b -> openConfig())
                .bounds(startX + 2 * (btnW + gap), btnY, btnW, btnH).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC141414);

        int cx = width / 2;
        int midY = height / 2;

        // Title
        int titleScale = 2;
        String title = "NeoHouse";
        int titleW = font.width(title) * titleScale;
        g.pose().pushPose();
        g.pose().scale(titleScale, titleScale, 1f);
        g.drawString(font, title, (cx - titleW / 2) / titleScale, (midY - 34) / titleScale, 0xFFFFFFFF, false);
        g.pose().popPose();

        // Subtitle
        String sub = "Housing Script Compiler";
        g.drawCenteredString(font, sub, cx, midY - 14, 0xFF9999CC);

        // Meta line
        String version = getVersion();
        String meta = version + "  ·  by Drakou111  ·  Updated " + BuildConstants.BUILD_DATE;
        g.drawCenteredString(font, meta, cx, midY + 2, 0xFF555577);

        // Divider
        int divW = Math.min(width - 40, 340);
        g.fill(cx - divW / 2, midY + 14, cx + divW / 2, midY + 15, 0xFF333355);

        super.render(g, mouseX, mouseY, partialTick);

        // Footer hint
        String hint = "Open Files to browse, edit, and run your .nh scripts";
        g.drawCenteredString(font, hint, cx, height - 18, 0xFF444466);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private void openFiles() {
        minecraft.setScreen(new FileBrowserScreen(scriptsDir.get()));
    }

    private void openHelp() {
        minecraft.setScreen(new HelpScreen());
    }

    private void openConfig() {
        minecraft.setScreen(new ConfigScreen());
    }

    private static String getVersion() {
        return ModList.get().getModContainerById(NeoHouse.MOD_ID)
                .map(c -> "v" + c.getModInfo().getVersion())
                .orElse("v?");
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
