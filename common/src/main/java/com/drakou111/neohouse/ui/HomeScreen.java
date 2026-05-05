package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.*;
import com.drakou111.neohouse.compat.PoseHelper;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.platform.Platforms;
import com.drakou111.neohouse.ui.hint.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class HomeScreen extends Screen {
    private final Supplier<Path> scriptsDir;
    private final Random random = new Random();
    private HintProvider currentHint;

    private static final List<HintProvider> HINTS = List.of(
            new AlwaysShowHint("GG red."),
            new AlwaysShowHint("GodTheHands."),
            new AlwaysShowHint("There are no bugs. Only hidden features."),
            new AlwaysShowHint("Yaaay!"),
            new AlwaysShowHint("...!"),
            new AlwaysShowHint("Subscribe to Drakou111"),
            new AlwaysShowHint("NP is not in P."),
            new AlwaysShowHint("Semicolons matter."),
            new AlwaysShowHint("99 little bugs in the code..."),
            new AlwaysShowHint("Off by one error."),
            new AlwaysShowHint("Works on my machine."),
            new AlwaysShowHint("Skill issue."),
            new AlwaysShowHint("404: Hint not found."),
            new AlwaysShowHint("git push"),
            new AlwaysShowHint("Hello, World!"),
            new AlwaysShowHint("Have you tried turning it off and on again?"),
            new AlwaysShowHint("TODO: fix this later."),
            new AlwaysShowHint("The answer is 42."),
            new AlwaysShowHint("Don't dig straight down."),
            new AlwaysShowHint("The cake is a lie."),
            new AlwaysShowHint("Drink water."),
            new AlwaysShowHint("This is fine."),
            new AlwaysShowHint("To be continued...")
    );


    public HomeScreen(Supplier<Path> scriptsDir) {
        super(Component.literal("NeoHouse"));
        this.scriptsDir = scriptsDir;
    }

    private static String getVersion() {
        return Platforms.get().getModVersion(NHConstants.MOD_ID);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int midY = this.height / 2;
        int btnW = 110;
        int btnH = 22;
        int gap = 10;
        int totalW = 3 * btnW + 2 * gap;
        int startX = cx - totalW / 2;
        int btnY = midY + 18;
        addRenderableWidget(Button.builder(Component.literal("Files"), b -> openFiles()).bounds(startX, btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Help"), b -> openHelp()).bounds(startX + btnW + gap, btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Config"), b -> openConfig()).bounds(startX + 2 * (btnW + gap), btnY, btnW, btnH).build());
        currentHint = getRandomHint();
    }

    private HintProvider getRandomHint() {
        Minecraft mc = Minecraft.getInstance();
        List<HintProvider> allowed = HINTS.stream().filter(h -> h.shouldShow(mc)).toList();
        return allowed.get(random.nextInt(allowed.size()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = UITheme.byId(NeoHouseConfig.get().uiTheme);
        UIOpacity o = UIOpacity.byId(NeoHouseConfig.get().uiOpacity);
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        int cx = width / 2;
        int midY = height / 2;
        int cardW = Math.min(width - 60, 390);
        int cardH = 130;
        int cardX = cx - cardW / 2;
        int cardY = midY - 62;
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x18FFFFFF);
        RenderHelper.renderOutline(g, cardX, cardY, cardW, cardH, t.accentTint(0x44));
        g.fill(cardX + 28, cardY, cardX + cardW - 28, cardY + 2, t.accent());
        int titleScale = 2;
        String title = "NeoHouse";
        int titleW = font.width(title) * titleScale;
        PoseHelper.push(g);
        PoseHelper.scale(g, titleScale, titleScale);
        g.drawString(font, title, (cx - titleW / 2) / titleScale, (midY - 46) / titleScale, 0xFFFFFFFF, false);
        PoseHelper.pop(g);
        g.drawCenteredString(font, "Housing Script Compiler", cx, midY - 24, t.muted());
        int divW = cardW - 48;
        g.fill(cx - divW / 2, midY - 10, cx + divW / 2, midY - 9, t.divider());
        String meta = getVersion() + " · by Drakou111 · " + BuildConstants.BUILD_DATE;
        g.drawCenteredString(font, meta, cx, midY - 4, t.accent());
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, currentHint.getText(minecraft), cx, height - 14, t.divider());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    private void openFiles() {
        minecraft.setScreen(new FileBrowserScreen(scriptsDir.get(), this));
    }

    private void openHelp() {
        minecraft.setScreen(new HelpScreen(this));
    }

    private void openConfig() {
        minecraft.setScreen(new ConfigScreen(this));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
