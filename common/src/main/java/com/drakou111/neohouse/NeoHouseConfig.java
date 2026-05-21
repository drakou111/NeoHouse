package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NeoHouseConfig {
    public static final NeoHouseConfig INSTANCE = new NeoHouseConfig();

    public boolean verboseActions              = false;
    public int     clickDelay                 = 2;
    public int     actionTimeoutTicks         = 200;
    public boolean showArgumentHints          = true;
    public boolean showArgumentRecommendations = true;
    public boolean autoIndent                  = true;
    public boolean overrideExisting            = true;
    public String  syntaxTheme                = SyntaxTheme.VSCODE_DARK.name();
    public String  uiTheme                   = UITheme.DARK_BLUE.name();
    public String  uiOpacity                 = UIOpacity.DEFAULT.name();

    private static boolean loaded = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private NeoHouseConfig() {}

    public static NeoHouseConfig get() {
        if (!loaded) { load(); loaded = true; }
        return INSTANCE;
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-config.json");
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) { INSTANCE.save(); return; }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("verboseActions"))             INSTANCE.verboseActions             = obj.get("verboseActions").getAsBoolean();
            if (obj.has("clickDelay"))               INSTANCE.clickDelay                 = obj.get("clickDelay").getAsInt();
            if (obj.has("actionTimeoutTicks"))        INSTANCE.actionTimeoutTicks         = obj.get("actionTimeoutTicks").getAsInt();
            if (obj.has("showArgumentHints"))          INSTANCE.showArgumentHints          = obj.get("showArgumentHints").getAsBoolean();
            if (obj.has("showArgumentRecommendations")) INSTANCE.showArgumentRecommendations = obj.get("showArgumentRecommendations").getAsBoolean();
            if (obj.has("autoIndent"))                 INSTANCE.autoIndent                  = obj.get("autoIndent").getAsBoolean();
            if (obj.has("overrideExisting"))           INSTANCE.overrideExisting            = obj.get("overrideExisting").getAsBoolean();
            if (obj.has("syntaxTheme"))                INSTANCE.syntaxTheme                 = obj.get("syntaxTheme").getAsString();
            if (obj.has("uiTheme"))                    INSTANCE.uiTheme                     = obj.get("uiTheme").getAsString();
            if (obj.has("uiOpacity"))                  INSTANCE.uiOpacity                   = obj.get("uiOpacity").getAsString();
        } catch (Exception e) {
            MessageUtils.warningToChat("Config load failed (" + e.getClass().getSimpleName() + "): " + e.getMessage() + ". Using defaults.");
        }
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("verboseActions",              verboseActions);
        obj.addProperty("clickDelay",                 clickDelay);
        obj.addProperty("actionTimeoutTicks",         actionTimeoutTicks);
        obj.addProperty("showArgumentHints",          showArgumentHints);
        obj.addProperty("showArgumentRecommendations", showArgumentRecommendations);
        obj.addProperty("autoIndent",                  autoIndent);
        obj.addProperty("overrideExisting",            overrideExisting);
        obj.addProperty("syntaxTheme",                 syntaxTheme);
        obj.addProperty("uiTheme",                    uiTheme);
        obj.addProperty("uiOpacity",                  uiOpacity);
        try {
            Files.writeString(configPath(), GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
