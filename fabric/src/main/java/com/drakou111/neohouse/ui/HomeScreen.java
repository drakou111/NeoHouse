package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.*;
import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.platform.Platforms;
import com.drakou111.neohouse.ui.hint.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class HomeScreen extends ScreenBase {
    private final Supplier<Path> scriptsDir;
    private final Random random = new Random();
    private String hintText = "";

    private static final List<HintProvider> HINTS = List.of(
            new TipHint("g. variables are shared by everyone, p. variables are per-player, t[team]. is per-team."),
            new TipHint("By default, re-running a script only fills functions, commands and menus that are still empty."),
            new TipHint("random [ ... ] runs one block at random - weight an option with N*, like 3*{ ... }."),
            new TipHint("repeat(N) { ... } copies its body N times at compile time."),
            new TipHint("Split large scripts across files with: import \"lib/utils.nh\";"),
            new TipHint("Functions can return several values at once using a tuple, like (double, double)."),
            new TipHint("Build messages with +: \"Score: \" + p.score, works on any type."),
            new TipHint("Open Help to browse every action, condition, event and placeholder."),
            new TipHint("/nh rerun instantly recompiles and runs your last script."),
            new TipHint("Hold an item and run /nh copyitem to grab its exact data for giveItem()."),
            new TipHint("Change the editor theme and syntax colors in Config."),
            new TipHint("global foo(); triggers a void function for every player at once."),
            new TipHint("Define a value once with 'const long MAX = 100;' and reuse it everywhere."),
            new TipHint("Drop .nh files in your neohouse-scripts folder to edit them outside Minecraft."),

            new SeasonalHint(12, 24, 12, 26, "Merry Christmas!"),
            new SeasonalHint(12, 31, 1, 1, "Happy New Year!"),
            new SeasonalHint(10, 31, 10, 31, "Happy Halloween!"),
            new SeasonalHint(7, 15, 7, 15, "JULY 15TH 7PM EST THE AWARDENING"),

            new RareHint("GG red.", 1.0 / 4),
            new RareHint("GodTheHands.", 1.0 / 4),
            new RareHint("Coming to Terraria soon.", 1.0 / 4),
            new RareHint("//TODO: remove this", 1.0 / 4), // Keep this, this is a joke hint!
            new RareHint("You caught a shiny tip!", 1.0 / 8_192),
            new RareHint("Is this Minceraft?", 1.0 / 10_000),

            new RandomNumberHint("Here is a random number: $$$", "$$$"),

            new RecurseHint(1.0 / 8, HomeScreen::hintPool)
    );

    private static List<HintProvider> hintPool() {
        return HINTS;
    }


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
        int btnW = 84;
        int btnH = 22;
        int gap = 8;
        int totalW = 4 * btnW + 3 * gap;
        int startX = cx - totalW / 2;
        int btnY = midY + 48;
        addRenderableWidget(Button.builder(Component.literal("Files"), b -> openFiles()).bounds(startX, btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Music"), b -> openMusic()).bounds(startX + btnW + gap, btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Help"), b -> openHelp()).bounds(startX + 2 * (btnW + gap), btnY, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("Config"), b -> openConfig()).bounds(startX + 3 * (btnW + gap), btnY, btnW, btnH).build());
        hintText = getRandomHint().getText(Minecraft.getInstance());
    }

    private HintProvider getRandomHint() {
        Minecraft mc = Minecraft.getInstance();
        List<HintProvider> allowed = HINTS.stream().filter(h -> h.shouldShow(mc)).toList();
        return allowed.get(random.nextInt(allowed.size()));
    }

    @Override
    public void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, width, height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        int cx = width / 2;
        int midY = height / 2;
        int cardW = Math.min(width - 60, 390);
        int cardH = 160;
        int cardX = cx - cardW / 2;
        int cardY = midY - 88;
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x18FFFFFF);
        RenderHelper.renderOutline(g, cardX, cardY, cardW, cardH, t.accentTint(0x44));
        g.fill(cardX + 28, cardY, cardX + cardW - 28, cardY + 2, t.accent());
        int logoSize = 56;
        RenderHelper.renderLogo(g, cx - logoSize / 2, cardY + 10, logoSize, t.accent());
        int titleScale = 2;
        String title = "NeoHouse";
        int titleW = font.width(title) * titleScale;
        int titleY = cardY + 10 + logoSize + 8;
        g.pushPose();
        g.scale(titleScale, titleScale);
        g.drawString(font, title, (cx - titleW / 2) / titleScale, titleY / titleScale, 0xFFFFFFFF, false);
        g.popPose();
        int subtitleY = titleY + font.lineHeight * titleScale + 4;
        g.drawCenteredString(font, "Housing Script Compiler", cx, subtitleY, t.muted());
        int divW = cardW - 48;
        int divY = subtitleY + font.lineHeight + 8;
        g.fill(cx - divW / 2, divY, cx + divW / 2, divY + 1, t.divider());
        String meta = getVersion() + " · by Drakou111 · " + BuildConstants.BUILD_DATE;
        g.drawCenteredString(font, meta, cx, divY + 4, t.accent());
        renderWidgets(g, mouseX, mouseY, partialTick);
        String disclaimer = "Not affiliated with Hypixel, Mojang, or Microsoft.";
        g.drawCenteredString(font, disclaimer, cx, cardY + cardH + 6, t.divider());
        g.drawCenteredString(font, hintText, cx, height - 14, t.divider());
    }

    @Override
    public void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
    }

    private void openFiles() {
        ClientUi.setScreen(minecraft, new FileBrowserScreen(scriptsDir.get(), this));
    }

    private void openMusic() {
        ClientUi.setScreen(minecraft, new MusicBrowserScreen(CommandActions.getMusicDir(), this));
    }

    private void openHelp() {
        ClientUi.setScreen(minecraft, new HelpScreen(this));
    }

    private void openConfig() {
        ClientUi.setScreen(minecraft, new ConfigScreen(this));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
